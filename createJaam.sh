#!/bin/bash
set -e

file="`basename "$1" ".jar"`"
./bin/jaam app --input "$1"  --output "$file".app.jaam
./bin/jaam decompile --input "$file".app.jaam --output "$file".decompile.jaam
./bin/jaam loop3  --input "$file".app.jaam --output "$file".loop3.jaam
./bin/jaam taint3 --input "$file".app.jaam --output "$file".taint3.jaam
./bin/jaam cat \
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
