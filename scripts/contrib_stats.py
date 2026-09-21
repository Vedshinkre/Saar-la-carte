#!/usr/bin/env python3
"""Per-person contribution statistics: commits, lines added/removed, and survival rate.

Run from anywhere inside the repo:
    python scripts/contrib_stats.py                 # current HEAD
    python scripts/contrib_stats.py --ref origin/main
    python scripts/contrib_stats.py --csv stats.csv

Survival rate = lines still attributed to a person by `git blame` at --ref
divided by all lines that person ever added (in the same category).
"""
import argparse
import csv
import subprocess
import sys
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor

# Canonical team names. An author is matched if the lowercase first word of the
# canonical name appears in their git name or email.
TEAM = ["Vlad Marciu", "Ansh", "Deniz", "Skerdi", "Atharva", "Ved"]

# Extra aliases: lowercase substring of git name/email -> canonical name.
ALIASES = {

}

# Authors to leave out entirely (lowercase substring of git name/email).
IGNORED = ["jenkins", "aktaş", "abelt", "abdelsalam"]

EMPTY_TREE = "4b825dc642cb6eb9a060e54bf8d69288fbee4904"

CATEGORIES = [
    ("main", lambda p: p.startswith("src/main/")),
    ("test", lambda p: p.startswith("src/test/")),
    ("systemtest", lambda p: p.startswith("src/systemtest/")),
    ("systemtest (non-json)",
     lambda p: p.startswith("src/systemtest/") and not p.lower().endswith(".json")),
    ("total (whole repo)", lambda p: True),
]


def git(*args):
    out = subprocess.run(
        ["git", "-c", "core.quotepath=off", *args],
        capture_output=True, encoding="utf-8", errors="replace",
    )
    if out.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)} failed: {out.stderr.strip()}")
    return out.stdout


def canonical(name, email):
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


def new_stats():
    return {"commits": 0, "added": 0, "removed": 0, "surviving": 0}


def collect_history(ref, stats):
    log = git("log", ref, "--no-merges", "--no-renames", "--numstat",
              "--format=@@%H\t%aN\t%aE")
    author = None
    cats_seen = set()

    def flush():
        if author:
            for c in cats_seen:
                stats[c][author]["commits"] += 1

    for line in log.splitlines():
        if line.startswith("@@"):
            flush()
            _, name, email = line.split("\t", 2)
            author = canonical(name, email)
            cats_seen = set()
        elif line.strip() and author:
            parts = line.split("\t")
            if len(parts) != 3 or parts[0] == "-":  # binary file
                continue
            added, removed, path = int(parts[0]), int(parts[1]), parts[2]
            for cname, pred in CATEGORIES:
                if pred(path):
                    cats_seen.add(cname)
                    stats[cname][author]["added"] += added
                    stats[cname][author]["removed"] += removed
    flush()


def blame_file(ref, path):
    try:
        out = git("blame", "--line-porcelain", "-w", ref, "--", path)
    except RuntimeError:
        return path, {}
    counts = defaultdict(int)
    name = None
    for line in out.splitlines():
        if line.startswith("author "):
            name = line[7:]
        elif line.startswith("author-mail "):
            author = canonical(name, line[12:].strip("<>"))
            if author:
                counts[author] += 1
    return path, counts


def collect_survival(ref, stats):
    # Text files only: binary files show "-" in numstat against the empty tree.
    listing = git("diff", "--numstat", "--no-renames", EMPTY_TREE, ref)
    paths = [l.split("\t", 2)[2] for l in listing.splitlines()
             if l.count("\t") == 2 and not l.startswith("-\t")]
    with ThreadPoolExecutor(max_workers=8) as pool:
        for path, counts in pool.map(lambda p: blame_file(ref, p), paths):
            for cname, pred in CATEGORIES:
                if pred(path):
                    for author, n in counts.items():
                        stats[cname][author]["surviving"] += n


def build_rows(cat_stats):
    rows = []
    for author, s in cat_stats.items():
        if not any(s.values()):
            continue
        surv = f"{100 * s['surviving'] / s['added']:.1f}%" if s["added"] else "-"
        rows.append([author, s["commits"], s["added"], s["removed"],
                     s["added"] - s["removed"], s["surviving"], surv])
    rows.sort(key=lambda r: -r[2])
    return rows


HEADER = ["Author", "Commits", "Added", "Removed", "Net", "Surviving", "Survival"]


def print_table(title, rows):
    print(f"\n=== {title} ===")
    if not rows:
        print("(no data)")
        return
    tot_added = sum(r[2] for r in rows)
    total = ["TOTAL", "", tot_added, sum(r[3] for r in rows), sum(r[4] for r in rows),
             sum(r[5] for r in rows),
             f"{100 * sum(r[5] for r in rows) / tot_added:.1f}%" if tot_added else "-"]
    table = [HEADER] + rows + [total]
    widths = [max(len(str(r[i])) for r in table) for i in range(len(HEADER))]
    for i, r in enumerate(table):
        print("  ".join(str(c).ljust(widths[j]) if j == 0 else str(c).rjust(widths[j])
                        for j, c in enumerate(r)))
        if i == 0 or i == len(table) - 2:
            print("  ".join("-" * w for w in widths))


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--ref", default="HEAD", help="branch/commit to analyse (default HEAD)")
    ap.add_argument("--csv", help="also write all tables to this CSV file")
    args = ap.parse_args()
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    stats = {c: defaultdict(new_stats) for c, _ in CATEGORIES}
    collect_history(args.ref, stats)
    collect_survival(args.ref, stats)

    csv_rows = []
    for cname, _ in CATEGORIES:
        rows = build_rows(stats[cname])
        print_table(cname, rows)
        csv_rows += [[cname, *r] for r in rows]

    unmatched = sorted({a for c in stats.values() for a in c if a.startswith("(unmatched)")})
    if unmatched:
        print("\nUnmatched authors (add them to TEAM/ALIASES/IGNORED in the script):")
        for a in unmatched:
            print(f"  {a}")

    if args.csv:
        with open(args.csv, "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["Category", *HEADER])
            w.writerows(csv_rows)
        print(f"\nWrote {args.csv}")


if __name__ == "__main__":
    try:
        main()
    except RuntimeError as e:
        sys.exit(str(e))
