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

(cg.cha doesn't seem to dump dot files)

sbt 'runMain soot.Main -cp to-analyze -pp -dump-cfg cg.cha -f S Factorial'


Potentially more useful:

sootsurvivalguide, section 7 (pages 30--32)

```
When performing an interprocedural analysis, the call graph of the applica- tion is an essential entity. When a call graph is available (only in whole- program mode), it can be accessed through the environment class (Scene) with the method getCallGraph. The CallGraph class and other associated constructs are located in the soot.jimple.toolkits.callgraph package. The simplest call graph is obtained through Class Hierarchy Analysis (CHA), for which no setup is necessary. CHA is simple in the fact that it assumes that all reference vari- ables can point to any object of the correct type. The following is an example of getting access to the call graph using CHA.

 CHATransformer.v().transform();
 SootClass a = Scene.v().getSootClass("testers.A");
 SootMethod src = Scene.v().getMainClass().getMethodByName("doStuff"); CallGraph cg = Scene.v().getCallGraph();

Refer to Section 8 for points-to analyses that will produce more interesting call graphs.
```

```
The call graph has methods to query for the edges coming into a method, edges coming out of method and edges coming from a particular statement (edgesInto(method), edgesOutOf(method) and edgesOutOf(statement), respec- tively). Each of these methods return an Iterator over Edge constructs. Soot provides three so-called adapters for iterating over specific parts of an edge.

Sources iterates over source methods of edges.
Units iterates over source statements of edges.
Targets iterates over target methods of edges.

So, in order to iterate over all possible calling methods of a particular method, we could use the code:

public void printPossibleCallers(SootMethod target) { CallGraph cg = Scene.v().getCallGraph();
  Iterator sources = new Sources(cg.edgesInto(target)); while (sources.hasNext()) {
  SootMethod src = (SootMethod)sources.next();
  System.out.println(target + " might be called by " + src); }
}
```
