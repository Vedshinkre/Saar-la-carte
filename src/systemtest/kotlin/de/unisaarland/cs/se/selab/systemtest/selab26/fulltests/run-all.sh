#!/usr/bin/env bash
# Re-runs every fulltests scenario and checks it against its expected log.
# Usage: bash src/systemtest/kotlin/de/unisaarland/cs/se/selab/systemtest/selab26/fulltests/run-all.sh
#
# The check mirrors FullSystemTests.kt: every expected line must appear, in order, and a line
# of `...` in the expected log skips ahead to the next expected line instead of asserting it
# Expected result: OK for every scenario
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../../../../../../../.." && pwd)"
cd "$ROOT" || exit 1
FT="src/systemtest/resources/fulltests"
OUT="${TMPDIR:-/tmp}/fulltests-out"
mkdir -p "$OUT"

run() { # <name> <maxTicks> <logLevel> <suffix>
  local name="$1" ticks="$2" level="$3" suffix="$4"
  local s="$FT/scenarios/$name"
  local got="$OUT/${name}${suffix}.log"
  local want="$FT/logs/${name}${suffix}.log"
  java -jar libs/selab.jar \
    --food "$s/food.json" --restaurants "$s/restaurants.json" --scenario "$s/scenario.json" \
    --maxTicks "$ticks" --logLevel "$level" --out "$got" 2>/dev/null
  local verdict
  verdict=$(python3 - "$want" "$got" <<'PY'
import sys
want = open(sys.argv[1]).read().splitlines()
got = open(sys.argv[2]).read().splitlines()
i, skipping = 0, False
for lineno, exp in enumerate(want, 1):
    if exp == "...":
        skipping = True
        continue
    if skipping:
        while i < len(got) and not got[i].startswith(exp):
            i += 1
        if i == len(got):
            print(f"MISSING expected line {lineno}: {exp}"); sys.exit()
        i += 1
        skipping = False
    else:
        if i >= len(got):
            print(f"LOG ENDED, expected line {lineno}: {exp}"); sys.exit()
        if got[i] != exp:
            print(f"MISMATCH at expected line {lineno}\n  want: {exp}\n  got:  {got[i]}"); sys.exit()
        i += 1
if not skipping and i != len(got):
    print(f"TRAILING OUTPUT: {len(got) - i} extra line(s), first: {got[i]}"); sys.exit()
print("OK")
PY
)
  printf '%-44s maxTicks=%-4s %-9s %s\n' "$name$suffix" "$ticks" "$level" "$verdict"
}

run staff-and-tables       24 DEBUG     ""
run staff-and-tables       24 INFO      ".info"
run staff-and-tables       24 IMPORTANT ".important"
run staff-and-tables       12 DEBUG     ".maxticks12"
run kitchen-and-serving    24 DEBUG     ""
run delivery-handoff       48 DEBUG     ""
run event-merge-incidents  96 DEBUG     ""
run staff-incidents        96 DEBUG     ""
