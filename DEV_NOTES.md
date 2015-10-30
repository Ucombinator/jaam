To dump Shimple output:

sbt 'runMain soot.Main -cp to-analyze -pp -f S Factorial'

produces:

[info] Set current project to analyzer (in build file:/Users/webyrd/github/shimple-analyzer/)
[info] Running soot.Main -cp to-analyze -pp -f S Factorial
Soot started on Fri Oct 30 10:53:35 MDT 2015
Transforming Factorial...
Writing to sootOutput/Factorial.shimple
Soot finished on Fri Oct 30 10:53:36 MDT 2015
Soot has run for 0 min. 0 sec.
[success] Total time: 1 s, completed Oct 30, 2015 10:53:36 AM



To dump a CFG (intraprocedural) as dot files:

For all phases:

sbt 'runMain soot.Main -cp to-analyze -pp -dump-cfg ALL -f S Factorial'

Or for specific phases:

sbt 'runMain soot.Main -cp to-analyze -pp -dump-cfg jb -f S Factorial'

(cg doesn't seem to dump dot files)

sbt 'runMain soot.Main -cp to-analyze -pp -dump-cfg cg -f S Factorial'

