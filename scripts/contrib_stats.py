#!/usr/bin/env python3
"""Per-person contribution statistics: commits, lines added/removed, and surviving lines.

Run from anywhere inside the repo:
    python scripts/contrib_stats.py                 # current HEAD
    python scripts/contrib_stats.py --ref origin/main
    python scripts/contrib_stats.py --csv stats.csv

The per-area tables count Kotlin sources (.kt) only. The closing summaries show
each person's share of surviving lines per area, once for Kotlin sources and
once for every text file in the repo.

Surviving = lines still attributed to a person by `git blame` at --ref.
Share = that person's surviving lines as a percentage of all surviving lines
in the same area and scope (so every Share column adds up to 100%).
Set NO_COLOR=1 to disable colours.
"""
import argparse
import csv
import itertools
import os
import re
import subprocess
import sys
import threading
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, astuple

# Canonical team names. An author is matched if the lowercase first word of the
# canonical name appears in their git name or email.
TEAM = ["Vlad Marciu", "Ansh", "Deniz", "Skerdi", "Atharva", "Ved"]

# Extra aliases: lowercase substring of git name/email -> canonical name.
ALIASES = {}

# Authors to leave out entirely (lowercase substring of git name/email).
IGNORED = ["jenkins", "aktaş", "abelt", "abdelsalam"]

# Area name -> path prefix.
AREAS = {"main": "src/main/", "test": "src/test/", "systemtest": "src/systemtest/", "whole repo": ""}

# Scope name -> which files it counts.
SCOPES = {"Kotlin": lambda path: path.endswith(".kt"), "Everything": lambda path: True}

EMPTY_TREE = "4b825dc642cb6eb9a060e54bf8d69288fbee4904"


# ── Styling ─────────────────────────────────────────────────────────────────

COLOR = sys.stdout.isatty() and not os.environ.get("NO_COLOR")
BOLD, DIM, GREEN, RED = "1", "2", "32", "31"
ANSI = re.compile(r"\033\[[\d;]*m")

# Kotlin brand colours.
PURPLE, CORAL = "38;2;127;82;255", "38;2;228;72;87"


def style(text, *codes):
    return f"\033[{';'.join(codes)}m{text}\033[0m" if COLOR and codes else str(text)


def width(text):
    return len(ANSI.sub("", text))


def pad(text, w, right=False):
    gap = " " * (w - width(text))
    return gap + text if right else text + gap


def bar(fraction, cells=16):
    """Horizontal bar; pass fraction relative to the largest value so the leader fills it."""
    full, part = divmod(round(fraction * cells * 8), 8)
    return pad(style("█" * full + ("", *"▏▎▍▌▋▊▉")[part], PURPLE), cells)


def percent(fraction):
    return f"{100 * fraction:.1f}%" if fraction else style("—", DIM)


def signed(n, positive=GREEN, negative=RED):
    return style(f"{n:+,}".replace("-", "−"), positive if n >= 0 else negative) if n else style("0", DIM)


def table(header, rows, footer=None):
    """Render rows of styled cells; the first column is left-aligned, the rest right."""
    header = [style(h, DIM) for h in header]
    everything = [header, *rows, *([footer] if footer else [])]
    widths = [max(width(r[i]) for r in everything) for i in range(len(header))]
    fmt = lambda row: "  ".join(pad(c, w, right=i > 0) for i, (c, w) in enumerate(zip(row, widths)))
    rule = style("─" * sum(widths + [2 * (len(widths) - 1)]), DIM)
    return [fmt(header), rule, *map(fmt, rows), *([rule, fmt(footer)] if footer else [])]


def box(title, lines):
    """Frame lines in a rounded box with the title set into the top border."""
    inner = max([width(title) + 2, *map(width, lines)])
    edge = lambda s: style(s, DIM)
    print()
    print(edge("╭─ ") + style(title, BOLD, PURPLE) + edge(" " + "─" * (inner - width(title) - 1) + "╮"))
    for line in lines:
        print(edge("│ ") + pad(line, inner) + edge(" │"))
    print(edge("╰" + "─" * (inner + 2) + "╯"))


class Spinner:
    """Animated progress line on stderr (terminals only): a turning triangle
    beside the current message. Set `.message` to update."""
    FRAMES = "◢◣◤◥"

    def __init__(self):
        self.message = ""
        self.done = threading.Event()
        self.thread = threading.Thread(target=self.run, daemon=True)

    def __enter__(self):
        if COLOR and sys.stderr.isatty():
            self.thread.start()
        return self

    def __exit__(self, *_):
        if self.thread.is_alive():
            self.done.set()
            self.thread.join()
            print("\r\033[2K", end="", file=sys.stderr, flush=True)

    def run(self):
        for tick in itertools.count():
            glyph = style(self.FRAMES[tick % 4], BOLD, PURPLE)
            print(f"\r\033[2K{glyph} {style(self.message, DIM)}",
                  end="", file=sys.stderr, flush=True)
            if self.done.wait(0.12):
                return


progress = Spinner()


# ── Collection ──────────────────────────────────────────────────────────────

@dataclass
class Stats:
    commits: int = 0
    added: int = 0
    removed: int = 0
    surviving: int = 0


def git(*args):
    out = subprocess.run(
        ["git", "-c", "core.quotepath=off", *args],
        capture_output=True, encoding="utf-8", errors="replace",
    )
    if out.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)} failed: {out.stderr.strip()}")
    return out.stdout


def buckets_of(path):
    """Every (scope, area) a path counts towards."""
    return [(scope, area) for scope, keep in SCOPES.items() if keep(path)
            for area, prefix in AREAS.items() if path.startswith(prefix)]


def canonical(name, email):
    """Map a git identity to a team member, None if ignored."""
    hay = f"{name} {email}".lower()
    if any(i in hay for i in IGNORED):
        return None
    for key, canon in ALIASES.items():
        if key in hay:
            return canon
    for canon in TEAM:
        if canon.split()[0].lower() in hay:
            return canon
    return f"(unmatched) {name}"


def parse_numstat(line):
    """Return (added, removed, path) for a text-file numstat line, else None."""
    parts = line.split("\t")
    if len(parts) != 3 or parts[0] == "-":  # malformed or binary
        return None
    return int(parts[0]), int(parts[1]), parts[2]


def collect_history(ref, stats):
    progress.message = "Reading history…"
    log = git("log", ref, "--no-merges", "--no-renames", "--numstat",
              "--format=%x00%aN\t%aE")
    for commit in log.split("\0")[1:]:
        header, *files = commit.splitlines()
        author = canonical(*header.split("\t", 1))
        if not author:
            continue
        touched = set()
        for added, removed, path in filter(None, map(parse_numstat, files)):
            for bucket in buckets_of(path):
                touched.add(bucket)
                stats[bucket][author].added += added
                stats[bucket][author].removed += removed
        for bucket in touched:
            stats[bucket][author].commits += 1


def blame_file(ref, path):
    """Count surviving lines per author in one file."""
    try:
        out = git("blame", "--line-porcelain", "-w", ref, "--", path)
    except RuntimeError:
        return {}
    counts = defaultdict(int)
    name = None
    for line in out.splitlines():
        if line.startswith("author "):
            name = line.removeprefix("author ")
        elif line.startswith("author-mail "):
            author = canonical(name, line.removeprefix("author-mail ").strip("<>"))
            if author:
                counts[author] += 1
    return counts


def collect_survival(ref, stats):
    # Diffing against the empty tree lists every text file at ref (binaries are skipped).
    listing = git("diff", "--numstat", "--no-renames", EMPTY_TREE, ref)
    paths = [entry[2] for entry in map(parse_numstat, listing.splitlines()) if entry]
    with ThreadPoolExecutor(max_workers=8) as pool:
        results = pool.map(lambda p: blame_file(ref, p), paths)
        for done, (path, counts) in enumerate(zip(paths, results), 1):
            progress.message = f"Blaming files {done}/{len(paths)}…"
            for bucket in buckets_of(path):
                for author, n in counts.items():
                    stats[bucket][author].surviving += n


# ── Report ──────────────────────────────────────────────────────────────────

def ranked(bucket_stats):
    """Authors with any activity, most surviving lines first, plus the total surviving."""
    authors = sorted(((a, s) for a, s in bucket_stats.items() if s != Stats()),
                     key=lambda item: -item[1].surviving)
    return authors, sum(s.surviving for _, s in authors)


def area_table(area, bucket_stats):
    authors, surviving = ranked(bucket_stats)
    if not authors:
        return box(f"{area} · Kotlin", [style("no data", DIM)])
    top = authors[0][1].surviving
    rows = [[author, f"{s.commits:,}", signed(s.added), signed(-s.removed),
             signed(s.added - s.removed), f"{s.surviving:,}",
             percent(s.surviving / (surviving or 1)), bar(s.surviving / (top or 1))]
            for author, s in authors]
    added, removed = sum(s.added for _, s in authors), sum(s.removed for _, s in authors)
    footer = [style("Total", BOLD), "", signed(added), signed(-removed),
              signed(added - removed), style(f"{surviving:,}", BOLD), "", ""]
    header = ["Author", "Commits", "Added", "Removed", "Net", "Surviving", "Share", ""]
    box(f"{area} · Kotlin", table(header, rows, footer))


def summary_table(scope, stats):
    shares = defaultdict(dict)
    for area in AREAS:
        authors, surviving = ranked(stats[scope, area])
        for author, s in authors:
            shares[author][area] = s.surviving / surviving if surviving else 0
    order = sorted(shares, key=lambda a: -shares[a].get("whole repo", 0))
    top = shares[order[0]].get("whole repo", 0) or 1
    rows = [[author, *(percent(shares[author].get(area, 0)) for area in AREAS),
             bar(shares[author].get("whole repo", 0) / top)] for author in order]
    box(f"Share of surviving lines · {scope}", table(["Author", *AREAS, ""], rows))


def write_csv(path, stats):
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["Scope", "Area", "Author", "Commits", "Added", "Removed", "Surviving", "Share %"])
        for (scope, area), bucket_stats in stats.items():
            authors, surviving = ranked(bucket_stats)
            for author, s in authors:
                share = round(100 * s.surviving / surviving, 2) if surviving else 0
                writer.writerow([scope, area, author, *astuple(s), share])


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--ref", default="HEAD", help="branch/commit to analyse (default HEAD)")
    ap.add_argument("--csv", help="also write every table to this CSV file")
    args = ap.parse_args()
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    stats = defaultdict(lambda: defaultdict(Stats))
    with progress:
        collect_history(args.ref, stats)
        collect_survival(args.ref, stats)

    sha = git("rev-parse", "--short", args.ref).strip()
    print(f"\n{style('◆', PURPLE)} {style('Contribution stats', BOLD)}"
          f"{style(f' · {args.ref} @ {sha}', DIM)}")

    for area in AREAS:
        area_table(area, stats["Kotlin", area])
    for scope in SCOPES:
        summary_table(scope, stats)

    unmatched = sorted({a for bucket in stats.values() for a in bucket if a.startswith("(unmatched)")})
    if unmatched:
        print(f"\n{style('⚠', CORAL)} Unmatched authors "
              f"{style('(add them to TEAM/ALIASES/IGNORED in the script)', DIM)}")
        print("\n".join(f"  {style('⎿', DIM)} {a}" for a in unmatched))

    if args.csv:
        write_csv(args.csv, stats)
        print(f"\n{style('✓', PURPLE)} Wrote {args.csv}")
    print()


if __name__ == "__main__":
    try:
        main()
    except RuntimeError as e:
        sys.exit(str(e))
