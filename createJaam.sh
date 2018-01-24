#!/bin/bash
set -e

: ${JAAM_dir:="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"}
: ${JAAM_exec:="$JAAM_dir/bin/jaam"}

file="`basename "$1" ".jar"`"

"$JAAM_exec" app --input "$1"  --output "$file".app.jaam
"$JAAM_exec" decompile --input "$file".app.jaam --output "$file".decompile.jaam
"$JAAM_exec" loop3  --input "$file".app.jaam --output "$file".loop3.jaam
"$JAAM_exec" taint3 --input "$file".app.jaam --output "$file".taint3.jaam
"$JAAM_exec" cat \
  --input "$file".app.jaam \
  --input "$file".decompile.jaam \
  --input "$file".loop3.jaam \
  --input "$file".taint3.jaam \
  --output "$file".all.jaam
rm "$file".app.jaam
rm "$file".loop3.jaam
rm "$file".decompile.jaam
rm "$file".taint3.jaam
#sh buildjaam.sh airplan_1
