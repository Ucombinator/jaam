package org.ucombinator.jaam.tools.loopConditions

import java.io.{FileOutputStream, PrintStream}

import org.jgrapht.Graphs
import org.jgrapht.alg.ConnectivityInspector
import org.jgrapht.alg.shortestpath.AllDirectedPaths
import org.jgrapht.graph.{AsSubgraph, DefaultEdge, EdgeReversedGraph}
import org.jgrapht.traverse.TopologicalOrderIterator
import org.ucombinator.jaam.serializer.Serializer
import org.ucombinator.jaam.tools
import org.ucombinator.jaam.tools.app.{App, Origin}
import org.ucombinator.jaam.util.{JGraphT, Soot, Stmt}
import soot.jimple.{Stmt => SootStmt, _}
import soot.options.Options
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}

import scala.collection.JavaConverters._
import scala.collection.immutable

object Main {
  def main(input: List[String], `class`: Option[String]) {
    Options.v().set_verbose(false)
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_keep_line_number(true)
    Options.v().set_allow_phantom_refs(true)
    //Options.v().set_soot_classpath(classpath.mkString(":"))
    Options.v().set_include_all(true)
    Options.v().set_prepend_classpath(false)
    Options.v().set_src_prec(Options.src_prec_only_class)
    //Options.v().set_whole_program(true)-
    //Options.v().set_app(true)-
    soot.Main.v().autoSetOptions()
    //Options.v().setPhaseOption("cg", "verbose:true")
    //Options.v().setPhaseOption("cg.cha", "enabled:true")
    //Options.v.set_main_class(mainClass)
    //Scene.v.setMainClass(clazz)
    //Scene.v.addBasicClass(className, SootClass.HIERARCHY)
    //Scene.v.setSootClassPath(classpath)
    //Scene.v.loadNecessaryClasses

    Soot.useJaamClassProvider()

    val inputPackets = input.flatMap(Serializer.readAll(_).asScala)

    for (a <- inputPackets) { Soot.addClasses(a.asInstanceOf[App]) }

/*
    val mainClasses = for (a <- inputPackets) yield { a.asInstanceOf[App].main.className }
    val mainMethods = for (a <- inputPackets) yield { a.asInstanceOf[App].main.methodName }
    val mainClass = mainClasses.head.get // TODO: fix
    val mainMethod = mainMethods.head.get // TODO: fix
    */

    Scene.v.loadBasicClasses()
    PackManager.v.runPacks()

    // Get all classes loaded so Soot doesn't keep recomputing the Hierarchy
    for (name <- Soot.loadedClasses.keys) {
      Soot.getSootClass(name)
    }

    `class` match {
      case Some(c) => doClass(c)
      case None =>
        for (name <- Soot.loadedClasses.keys) {
          if (Soot.loadedClasses(name).origin == Origin.APP) {
            doClass(name)
          }
        }
    }
  }

  def doClass(name: String): Unit = {
    for (m <- Soot.getSootClass(name).getMethods.asScala) {
      if (!m.isNative && !m.isAbstract) {
        //println(f"\n\n*******\n*******\nMethod: ${m.getDeclaringClass.getName}.${m.getName}\n\n")
        //    .getMethodByName(mainMethod) //Coverage2.freshenMethod(Soot.getSootClass(mainClass).getMethodByName(mainMethod))
        myLoops(m)
      }
    }
  }

  // TEST CMD: (cd ../..; sbt assembly) && jaam loop4 --input DoWhileLoop.app.jaam --output /dev/null
  def myLoops(m: SootMethod): Unit = {
    //TODO: replace set with ordered set?
    val (start, graph) = Soot.getBodyGraph(m)

    val dom = JGraphT.dominators(graph, start, true)

    // Maps header nodes to sets of backjump nodes
    val headers = JGraphT.loopHeads(graph, start)

    val loops = JGraphT.loopNodes(graph, start)
    if (loops.size > 0) {
      println(f"\n\n*******\n*******\nMethod: ${m.getDeclaringClass.getName}.${m.getName}\n\n")
      println("Loops:")
    }

    // Print Loop Graph
    for ((k, vs) <- loops.toList.sortBy(_._1.index)) {
      try {
        println(f"-------\n\nHeader:\n$k\nVertices:")
        for (v <- vs.toList.sortBy(_.index)) {
          println(f"    ${v.index}: $v")
          for (p <- Graphs.successorListOf(graph, v).asScala.toList.sortBy(_.index)) {
            println(f"    Edge to ${p.index}: $p")
          }
        }
        println()

        // Get Reachable Set from "k" (header)
        // Detect try-catch {}
        var reach = Set[Stmt]()
        var search = Set[Stmt](k)
        while (search.nonEmpty) {
          val v = search.head
          search = search.tail
          if(!reach(v)) {
            val succ = Graphs.successorListOf(graph, v).asScala.filter(vs)
            search ++= succ
          }
          reach += v
        }

        // Create Loop Graph
        // Removes try-catch {}
        val loopGraph = new AsSubgraph[Stmt, DefaultEdge](graph, reach.asJava)
        //println(f"loop at $k")

        // ends = all exit nodes
        // sources = start nodes
        // ts = all nodes that jump to header (t and contiunes)
        // c = start of condition
        // s = start of body
        // t = end of body
        // e = first statement after loop
        // ps = states that jump to e
        val ends = vs.flatMap(v => Graphs.successorListOf(graph, v).asScala.filter(s => !vs.contains(s)))
        val sources = loopGraph.vertexSet().asScala.filter(v => Graphs.predecessorListOf(graph, v).isEmpty)
        val backEdges = headers(k)
        /*val singlebreak = loopGraph.vertexSet().asScala.filter(v => Graphs.successorListOf(graph, v).asScala.exists(!vs(_)) && Graphs.successorListOf(graph, v).size == 1)
        for (v <- singlebreak) {
          println(f"Detectable Break or Return: $v")
        }*/

        // Types of loops: exception, infinite, pre-condition, post-condition
        // Infinite: No jumps out of loop
        // Pre-condition: Jumps out of loop + back jump states (t and continues) don't go to exits
        // Post-condition: Jumps out of loop + "there are" back jump states go to exits
        if (ends.size > 1) {
          println("Loop has multiple exits")
        } else if (k match {
          case k: IdentityStmt => k.getRightOp.isInstanceOf[CaughtExceptionRef]
          case _ => false
        }) {
          println("Loop type: exception (by identity)")
        } else if (k match {
          case k: DefinitionStmt => k.getRightOp.isInstanceOf[CaughtExceptionRef]
          case _ => false
        }) {
          println("Loop type: exception (by definition)")
        } else if (vs.forall(v => v.nextSemantic.forall(vs))) {
          // infinite = no jumps out of loop
          println("Loop type: infinite")
        } else if (backEdges.forall(b => b.nextSemantic.forall(vs))) {
          // pre-condition = jumps out of loop - back jump statements go only into the loop
          println("Loop type: pre-condition")

          val c: Stmt = k

          // Detects "t" and continues
          val ts = Graphs.predecessorListOf(loopGraph, c).asScala
          // Remove edges going to c
          Graphs.predecessorListOf(loopGraph, c).asScala.foreach(loopGraph.removeEdge(_, c))

          // Note, there may be multiple `ends` due to "returns" or "breaks" inside the loop.
          // However, the "real" e is the one that has a set of predecessors that all have paths to the predecessors of all the other members of `ends`.
          // Thus we can partially order the predecessors of the members of `ends` and take the first one.
          // This may not be totally ordered but the set of initial statements in the partial order all go to the same e.

          // Create a topological traversal
          val i = new TopologicalOrderIterator[Stmt, DefaultEdge](loopGraph)
          // Filter i to contain only those that have some successor that is not in the loop
          val filtered = i.asScala.filter(v => Graphs.successorListOf(graph, v).asScala.exists(!vs(_)))

          // Take the first one.  This jumps to the "real" `e`
          val p = filtered.next

          // The member of `ends` that is preceded by `p` is the "real" `e`
          val es = Graphs.successorListOf(graph, p).asScala.filter(!vs(_))
          //assert(es.size == 1) // There should be only one
          if(es.size > 1) {
            println("Error: \nLoop type: infinite (switch with breaks or returns inside...)")
          }
          val e = es.toList.head
          assert(ends(e))

          // ps: all states that jump to e
          var ps = Graphs.predecessorListOf(graph, e).asScala.filter(reach).toSet

          // Condition = nodes between c and s
          // (1a) Condition = everything reachable backwards from e but not through edge t->c
          // (1b) Condition = every node on every path between c and e but not through t
          // Note: 1a is the same as 1b
          // (2) s = first choke point after e
          // (3) s = statement after last edge to e
          // (4a) s = first node such that t dominates e (relative to s)
          // (4b) s = first node that is post-dominated by t (relative to e)
          // Note: 1a is the same as 1b
          // Note: (1) is the same as (4)

          // Compute 1a
          // Everything reachable backwards from e but not through edge t->c
          val cs1 = {
            var cond = ps
            var bs = Set[Stmt]()
            var breaks = Set[Stmt]()
            var todo = ps.toList

            // Finds Condition
            // Find everything reachable
            while (todo.nonEmpty) {
              val v = todo.head
              todo = todo.tail
              if (v != c) {
                val predecessor = Graphs.predecessorListOf(loopGraph, v).asScala.filterNot(cond)
                todo ++= predecessor
                cond ++= predecessor
              }
            }

            // If case 1 returns multiple starts: There are breaks (or maybe returns?)
            // Since all real condition ps have path to all break ps:
            // Each time remove last p in topological ordering

            // # Start Nodes = # Nodes in body that have edges from condition
            bs = loopGraph.vertexSet().asScala.filter(v => Graphs.predecessorListOf(loopGraph, v).asScala.exists(cond) && !cond.contains(v)).toSet
            while (bs.size > 1) {

              /*println(f"  (1) starts = ")
              for (cb <- bs.toList.sortBy(_.index)) {
                println(f"    $cb")
              }*/
              // Remove last ps from its topological sort
              val i = new TopologicalOrderIterator[Stmt, DefaultEdge](loopGraph)
              val filtered = i.asScala.filter(ps)
              // Add it to breaks
              var cand = ps.head
              while (filtered.hasNext) {
                cand = filtered.next
              }
              ps -= cand
              breaks += cand
              loopGraph.removeEdge(cand, e)

              println(f"    Loop has break at:\n    ---break: $cand")
              /*println("  *** ps = ")
              for (cb <- ps.toList.sortBy(_.index)) {
                println(f"    $cb")
              }*/

              // Finds Condition Again
              cond = ps
              todo = ps.toList
              // Find everything reachable
              while (todo.nonEmpty) {
                val v = todo.head
                todo = todo.tail
                if (v != c) {
                  val predecessor = Graphs.predecessorListOf(loopGraph, v).asScala.filterNot(cond)
                  todo ++= predecessor
                  cond ++= predecessor
                }
              }

              bs = loopGraph.vertexSet().asScala.filter(v => Graphs.predecessorListOf(loopGraph, v).asScala.exists(cond) && !cond.contains(v)).toSet

            }

            println("  (1) condition = ")
            for (cb <- cond.toList.sortBy(_.index)) {
              println(f"    $cb")
            }
            cond
          }

          // Compute 2
          // First choke point after e
          val cs2 = {
            // Create a topological traversal
            val i = new TopologicalOrderIterator[Stmt, DefaultEdge](loopGraph).asScala

            // Finds first choke point
            var seen = Set[Stmt]() // Stmts that we have seen jumps to
            var visited = Set[Stmt]() // Stmts that the topological traversal has visited
            var v: Stmt = i.next
            visited += v
            while (!ps.subsetOf(visited) // not after all jumps to e
              || !seen.subsetOf(visited)) {
              // not at choke point

              seen ++= Graphs.successorListOf(loopGraph, v).asScala
              v = i.next
              visited += v
            }

            // Might include s
            if (!v.nextSemantic.contains(e)) {
              visited -= v
            }

            println("  (2) condition = ")
            for (cb <- visited.toList.sortBy(_.index)) {
              println(f"    $cb")
            }
            visited
          }

          // Compute 3
          // Statement after last edge to e
          val cs3 = {
            val i = new TopologicalOrderIterator[Stmt, DefaultEdge](loopGraph)
            // Filter i to contain only those that have some successor that is not in the loop
            val filtered = i.asScala.filter(Graphs.successorListOf(graph, _).contains(e)).filter(ps)

            // Take the last condition.  This jumps to the "real" `s`
            while (ps.size > 1) {
              ps -= filtered.next
            }
            var lc = c
            if (ps.size > 0) {
              lc = ps.head
            }

            // Finds 's'
            val ss = lc.nextSemantic.filter(_ != e)
            if(ss.size > 1) {
              println("Error: \nLoop type: infinite (switch with breaks and/or returns inside)")
            }
            //assert(ss.size == 1)
            val s = ss.head

            // Traverse from c to s:
            var cond = Set(c)
            var todo = List(c)
            while (todo.nonEmpty) {
              val v = todo.head
              todo = todo.tail
              if (v != s) {
                val successor = Graphs.successorListOf(graph, v).asScala.filterNot(cond).filter(vs)
                todo ++= successor
                cond ++= successor
              }
            }
            cond -= s

            println("  (3) condition = ")
            for (cb <- cond.toList.sortBy(_.index)) {
              println(f"    $cb")
            }
            cond
          }

          if (ps.size == 0) {
            println("Infinite Loop\n")
          } else {
            if (cs1 != cs2) {
              println("!!!!!\n!!!!!\n!!!!!\nCase 1 not equal Case 2")
            }
            if (cs1 != cs3) {
              println("!!!!!\n!!!!!\n!!!!!\nCase 1 not equal Case 3")
            }
            if (cs1 == cs2 && cs1 == cs3) {
              println("Correct")
            }
          }

        } else {
          // post-condition = some back jump statements go out of loop
          println("Loop type: post-condition")

          val s: Stmt = k

          // Note, there may be multiple `ends` due to a "return" inside the loop.
          // However, the "real" e is the one that has a set of predecessors that predecessors of all other members of 'ends' have paths to them.
          // Thus we can partially order the predecessors of the members of `ends` and take the last one.
          // This may not be totally ordered but the set of final statements in the partial order all go to the same e.

          // Detects "t" and continues
          val ts = Graphs.predecessorListOf(loopGraph, s).asScala
          // Remove edges going to s
          Graphs.predecessorListOf(loopGraph, s).asScala.foreach(loopGraph.removeEdge(_, s))

          //TODO...
          // What happens here if we have breaks and multiple start nodes?
          // Like pre-condition case

          val i = new TopologicalOrderIterator[Stmt, DefaultEdge](loopGraph)
          // Filter i to contain only those that have some successor that is not in the loop
          val filtered = i.asScala.filter(v => Graphs.successorListOf(graph, v).asScala.exists(!vs(_)))

          // Set of nodes that jump to ends
          var all_ps = loopGraph.vertexSet().asScala.filter(_.nextSemantic.exists(ends(_)))
          // Take the last node.  This jumps to the "real" `e`
          while (all_ps.size > 1) {
            all_ps -= filtered.next
          }
          val p = all_ps.head

          // The member of `ends` that is preceded by `p` is the "real" `e`
          val es = Graphs.successorListOf(graph, p).asScala.filter(!vs(_))
          assert(es.size == 1) // There should be only one
          val e = es.toList.head
          assert(ends(e))

          // Condition = nodes between c and s
          // (1) c = last node that dominates e and s (relative to s)
          // (2) c = first choke-point after e and s (in reverse)
          // (3) c = first common ancestor of first nodes (partial order) that go to e or s
          // Note: (1) is the same as (2)

          // Compute 2
          // First choke-point after e and s (in reverse)
          val cs2 = {
            // Create a reversed topological traversal
            val reverseGraph = new EdgeReversedGraph(loopGraph)
            val i = new TopologicalOrderIterator[Stmt, DefaultEdge](reverseGraph)

            var seen = Set[Stmt]() // Stmts that we have seen jumps to
            var visited = Set[Stmt]() // Stmts that the topological traversal has visited
            // TODO...
            // Do we need to do this in pre-condition case 2? (pps instead of ps)

            // All states that jump to e or s
            val pps = loopGraph.vertexSet().asScala.filter(v => v.nextSemantic.contains(e) || v.nextSemantic.contains(s))

            var v: Stmt = i.next
            visited += v
            while (!pps.subsetOf(visited) // Not after all jumps to e
              || !seen.subsetOf(visited)) {
              // Not at choke point
              seen ++= Graphs.predecessorListOf(loopGraph, v).asScala

              v = i.next
              visited += v
            }

            /*val ci = new ConnectivityInspector(loopGraph)
            val reach = visited.filter(p => ci.pathExists(s, p))
            for (v <- vs) {
              val path = new AllDirectedPaths(loopGraph).getAllPaths(s, v, true, null).asScala.head.getVertexList.asScala.toSet
              println("\n\nHere...:\n")
              val test = ci.pathExists(s, v)
              println(f"Exist: $test")
              for (p <- path)
                println(f"path: $p")
            }*/

            val cond = visited

            println("  (2) condition = ")
            for (cb <- cond.toList.sortBy(_.index)) {
              println(f"    $cb")
            }
            cond
          }

          // Compute 3
          // First common ancestor of first nodes (partial order) that go to e or s
          val cs3 = {
            val i = new TopologicalOrderIterator[Stmt, DefaultEdge](loopGraph)
            // TODO...
            // Again do we need to do this in pre-condition case 3? (pps instead of ps)

            // Iterator on pps
            // Filter i to contain only those that have some successor that is not in the loop
            val filtered = i.asScala.filter(vs).filter(v => v.nextSemantic.contains(e) || v.nextSemantic.contains(s))

            //Finds the common ancestor
            var common = vs
            while (filtered.hasNext) {
              // Backtrack and mark every node passed
              val p = filtered.next()
              val path = new AllDirectedPaths(loopGraph).getAllPaths(s, p, true, null).asScala.head.getVertexList.asScala.toSet
              /*var path = Set(p)
              var pred = Graphs.predecessorListOf(loopGraph, p).asScala
              while (pred.size > 0 && pred.head != s) {
                path ++= pred
                p = pred.head
                pred = Graphs.predecessorListOf(loopGraph, p).asScala
              }*/
              common = common.intersect(path)
            }

            // Any path backward, leads to common ancestor
            val i2 = new TopologicalOrderIterator[Stmt, DefaultEdge](loopGraph)
            // Iterator on ps
            // Filter i to contain only those that have some successor that is not in the loop
            val filtered2 = i2.asScala.filter(vs).filter(v => v.nextSemantic.contains(e) || v.nextSemantic.contains(s))
            var last = filtered2.next()
            var pred = Graphs.predecessorListOf(loopGraph, last).asScala
            while (!common.contains(last)) {
              last = pred.head
              pred = Graphs.predecessorListOf(loopGraph, last).asScala
            }
            val c = last

            // Traverse from c to s:
            var cond = Set(c)
            var todo = List(c)
            while (todo.nonEmpty) {
              val v = todo.head
              todo = todo.tail
              if (v != s) {
                val successor = Graphs.successorListOf(loopGraph, v).asScala.filterNot(cond).filter(vs)
                todo ++= successor
                cond ++= successor
              }
            }
            cond -= s

            println("  (3) condition = ")
            for (cb <- cond.toList.sortBy(_.index)) {
              println(f"    $cb")
            }
            cond
          }

          if (cs2 != cs3) {
            println("!!!!!\n!!!!!\n!!!!!\nCase 2 not equal Case 3")
          }
          if (cs2 == cs3) {
            println("Correct")
          }
        }
      }
      catch {
        case e: IllegalArgumentException => println("Graph not a dag")
        case e: Throwable => e.printStackTrace()
      }
    }
  }
}
// Definition of Loop: Piece of code that can be run more than once
// Decompilers: Procyon - FernFlower (Intelij)
// False: Conditions going to both exit and start == Last nodes in partial ordering
// False: Can detect those returns with only one straight edge to them (conditions has 2)

// For detecting break if they have statements before them:
// They can reach the actual exit node
// Examples of why we are considering some cases as the same (like the while + return example of michael)
// Figure out "Not DAG" cases
// Find any breaks or returns that can't be in condition:
// For example anything after a "loop" or after something 1. Detectable 2. that can't exist in condition
// We cant have any infinite loops!!!
// TODO...
// Ignore multiple exits for testing purposes
//TODO...
// Change nextSemantics

//TODO...
// Replaced some graphs with loopGraph

class PseudoStmt(stmt: Stmt) extends Stmt(stmt.sootStmt, stmt.sootMethod) {
  override def toString: String = "PseudoStmt" + super.toString
}

// ./bin/jaam-tools loop4 --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/airplan_1.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-cli-1.3.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-codec-1.9.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-fileupload-1.3.1.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-io-2.2.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-lang3-3.4.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-logging-1.2.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/httpclient-4.5.1.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/httpcore-4.4.3.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/jline-2.8.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/log4j-1.2.17.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/mapdb-2.0-beta8.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/netty-all-4.0.34.Final.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/protobuf-java-3.0.0-beta-2.jar --classpath resources/rt.jar


// --app airplan_1.jar
// --rt ../../../../../../../../jaam/jaam.git/resources/rt.jar
// --lib commons-cli-1.3.jar
// --lib commons-codec-1.9.jar
// --lib commons-fileupload-1.3.1.jar
// --lib commons-io-2.2.jar
// --lib commons-lang3-3.4.jar
// --lib commons-logging-1.2.jar
// --lib httpclient-4.5.1.jar
// --lib httpcore-4.4.3.jar
// --lib jline-2.8.jar
// --lib log4j-1.2.17.jar
// --lib mapdb-2.0-beta8.jar
// --lib netty-all-4.0.34.Final.jar
// --lib protobuf-java-3.0.0-beta-2.jar
// 
// --app airplan_1.jar --rt ../../../../../../../../jaam/jaam.git/resources/rt.jar --lib commons-cli-1.3.jar --lib commons-codec-1.9.jar --lib commons-fileupload-1.3.1.jar --lib commons-io-2.2.jar --lib commons-lang3-3.4.jar --lib commons-logging-1.2.jar --lib httpclient-4.5.1.jar --lib httpcore-4.4.3.jar --lib jline-2.8.jar --lib log4j-1.2.17.jar --lib mapdb-2.0-beta8.jar --lib netty-all-4.0.34.Final.jar --lib protobuf-java-3.0.0-beta-2.jar
