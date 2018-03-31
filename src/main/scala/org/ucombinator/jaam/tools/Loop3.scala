package org.ucombinator.jaam.tools.loop3

import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.PrintStream
import java.util.jar.JarEntry
import java.util.jar.JarInputStream

import org.jgrapht.{Graph, Graphs}
import org.jgrapht.graph.{DefaultDirectedGraph, DefaultEdge}
import org.ucombinator.jaam.{serializer, tools}
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.options.Options
import soot.jimple.{Stmt => SootStmt, _}
import org.ucombinator.jaam.util.{CachedHashCode, Loop, Soot, Stmt}
import org.ucombinator.jaam.tools.coverage2.Coverage2
import org.ucombinator.jaam.serializer._
//import org.ucombinator.jaam.tools.LoopAnalyzer.{LoopNode, LoopTree, Node}

import org.jgrapht.graph.{DefaultDirectedGraph, DefaultEdge}
import org.jgrapht.{Graph, Graphs}
import org.ucombinator.jaam.serializer
import org.ucombinator.jaam.util.{CachedHashCode, Loop, Stmt}
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}
import soot.jimple.{IfStmt, Stmt => SootStmt}
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}

import scala.collection.JavaConversions._
import scala.collection.{immutable, mutable}

import scala.collection.{immutable, mutable}
import scala.collection.JavaConverters._
import org.ucombinator.jaam.tools._
import org.ucombinator.jaam.tools.app._

object Main {
  def main(input: List[String], jaam: String, prune: Boolean, shrink: Boolean, prettyPrint: Boolean) {
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

    val mainClasses = for (a <- inputPackets) yield { a.asInstanceOf[App].main.className }
    val mainMethods = for (a <- inputPackets) yield { a.asInstanceOf[App].main.methodName }
    val mainClass = mainClasses.head.get // TODO: fix
    val mainMethod = mainMethods.head.get // TODO: fix

    Scene.v.loadBasicClasses()
    PackManager.v.runPacks()

    //println("dc" + Scene.v.dynamicClasses().asScala)
    println("ap" + Scene.v.getApplicationClasses.asScala)
    println("al" + Scene.v.getClasses.asScala)
    println("dan" + Scene.v.getClasses(SootClass.DANGLING).asScala)
    println("lib" + Scene.v.getLibraryClasses.asScala)
    println("phan" + Scene.v.getPhantomClasses.asScala)

    val c = Soot.getSootClass("java.lang.Object")
    println("hier " + Scene.v.getActiveHierarchy())
    println("hier sub " + Scene.v.getActiveHierarchy().getSubclassesOf(c))
    println("fast hier " + Scene.v.getOrMakeFastHierarchy())
    println("hier sub " + Scene.v.getFastHierarchy.getSubclassesOf(c))
    println("entry " + Scene.v.getEntryPoints().asScala)
    //println("main " + Scene.v.getMainClass())
    println("pkg " + Scene.v.getPkgList)

    var class_count = 0
    var method_count = 0
    var stmt_count = 0
    var target_count = 0

    var edges = immutable.Map[Stmt, Set[SootMethod]]()

    def invokeExprTargets(expr: InvokeExpr): Set[SootMethod] = {
      val m = expr.getMethod
      val c = m.getDeclaringClass
      val cs: Set[SootClass] = expr match {
        case e : DynamicInvokeExpr =>
          val Soot.DecodedLambda(_, _, implementationMethod, _) = Soot.decodeLambda(e)
          // A bit of a cheat, since the method isn't called from here, but it seems as good a place as any to put it
          // Note that we use `return` to bypass the rest of `invokeExprTargets`
          return Set(implementationMethod)
        case _ : StaticInvokeExpr => Set(c)
        // SpecialInvokeExpr is also a subclasses of InstanceInvokeExpr but we need to treat it special
        case _: SpecialInvokeExpr => Set(c)
        case _: InstanceInvokeExpr =>
          if (c.isInterface) {
            // TODO: Main performance cost, but can't cache because new new hierarchy when scene changes (due to getSootClass?)
            Scene.v.getActiveHierarchy.getImplementersOf(c).asScala.toSet
          } else {
            Scene.v.getActiveHierarchy.getSubclassesOfIncluding(c).asScala.toSet
          }
      }

      (for (c2 <- cs if !c2.isInterface) yield
        c2.getMethodUnsafe(m.getNumberedSubSignature) match {
          case null => None
          case m2   => Some(m2)
        }).flatten
    }

    def stmtTargets(stmt: Stmt): Set[SootMethod] = stmt.sootStmt match {
      case s: InvokeStmt => invokeExprTargets(s.getInvokeExpr)
      case s: DefinitionStmt =>
        s.getRightOp match {
          case s: InvokeExpr => invokeExprTargets(s)
          case _ => Set()
        }
      case _ => Set()
    }

    // Get all classes loaded so Soot doesn't keep recomputing the Hierarchy
    for (name <- Soot.loadedClasses.keys) {
      Soot.getSootClass(name)
      println(f"class name: $name")
    }

    for (name <- Soot.loadedClasses.keys) {
      class_count += 1
      //val name = entry.getName.replace("/", ".").replaceAll("\\.class$", "")
      println(f"class origin ${Soot.loadedClasses(name).origin} $class_count: $name")

      if (Soot.loadedClasses(name).origin == Origin.APP) {
        val c = Soot.getSootClass(name)
        // The .toList prevents a concurrent access exception
        for (m <- c.getMethods.asScala.toList) {
          method_count += 1
          //println(f"method $method_count: $m")
          if (m.isNative) { println("skipping body because native") }
          else if (m.isAbstract) { println("skipping body because abstract") }
          else {
            for (sootStmt <- Soot.getBody(m).getUnits.asScala) {
              stmt_count += 1
              //println(f"stmt $stmt_count: $sootStmt")
              val s = Stmt(Soot.unitToStmt(sootStmt), m)
              val ts = stmtTargets(s)
              target_count += ts.size
              if (edges.contains(s)) {
                println(f"already have edge from $s")
              }
              edges += s -> ts
              //edges += s -> (ts ++ edges.get(s).getOrElse(Set()))
              // TODO: cache ts
              if (ts.nonEmpty) {
                //println(f"$target_count.$c.$m.${s.index}: $ts")
              }
            }
          }
          //println(f"end method $c $m")
        }
        println(f"end class $c")
      }
    }

    var edge_count = 0
    var app_out_count = 0
    var app_in_count = 0
    var app_edge_count = 0
    var app_edge_both_count = 0
    for ((s: Stmt, ds: Set[SootMethod]) <- edges) {
      for (d <- ds) {
        edge_count += 1
        var isAppOut = false
        var isAppIn = false
        Soot.loadedClasses.get(s.sootMethod.getDeclaringClass.getName) match {
          case None => println(f"couldn't find src: " + s.sootMethod.getDeclaringClass.getName + "::" + s)
          case Some(r) =>
            //println(f"found src $r $s")
            if (r.origin == Origin.APP) {
              app_out_count += 1
              isAppOut = true
            }
        }

        Soot.loadedClasses.get(d.getDeclaringClass.getName) match {
          case None => println(f"couldn't find dst: " + d.getDeclaringClass.getName + "::" + d)
          case Some(r) =>
            //println(f"found dst $r $d")
            if (r.origin == Origin.APP) {
              app_in_count += 1
              isAppIn = true
            }
        }

        if (isAppOut || isAppIn) {
          app_edge_count += 1
        }

        if (isAppOut && isAppIn) {
          app_edge_both_count += 1
        }
      }
    }

    println(f"counts: $edge_count $app_out_count $app_in_count $app_edge_count $app_edge_both_count")
    // TODO: computer coverage
    // TODO: process only app methods in the first place

    println(f"END classes=$class_count methods=$method_count stmts=$stmt_count targets=$target_count")

    val outStream = new PrintStream(new FileOutputStream("loop3.out.out")) // or System.out
    val coverageStream = new PrintStream(new FileOutputStream("loop3.coverage.out"))

    // TODO: I don't like the implementation of `appEdges` -- with my first glance I can't get an intuitive sence of it!
    val appEdges =
      for ((s, ds) <- edges;
           Some(c) = Soot.loadedClasses.get(s.sootMethod.getDeclaringClass.getName);
           if c.origin == Origin.APP;
           new_ds = for (d <- ds;
                         c2 = Soot.loadedClasses.get(d.getDeclaringClass.getName) match {
                           case None =>
                             throw new Exception("no class for: " + d.getDeclaringClass.getName + " s: " + s.sootMethod.getDeclaringClass.getName)
                           case Some(v) =>
                             v
                         };
                         if c2.origin == Origin.APP) yield {d};
           if new_ds.nonEmpty)
        yield {
          s -> new_ds
        }

    var appEdges2 = Map[SootMethod, Map[Stmt, Set[SootMethod]]]()
    for ((s, ds) <- appEdges) {
      val old = appEdges2.getOrElse(s.sootMethod, Map[Stmt, Set[SootMethod]]())
      appEdges2 += s.sootMethod -> (old + (s -> ds))
    }
    val targets = (for ((_, s) <- appEdges2; (_, ms) <- s; m <- ms) yield m).toSet
    val roots = appEdges2.keys.filter(!targets.contains(_)).toSet

    for (root <- roots) {
      println(f"root: $root")
    }

    println(f"appEdges: ${appEdges.size}")

    def encode(s: String): String = s.replace("\"", "\\\"")
    def quote(s: String): String = "\"" + encode(s) + "\""

    //    println("digraph loops {")
    //    println("ranksep=\"10\";");
    //    for ((s, ds) <- appEdges) {
    //      for (d <- ds) {
    //        println(f"  ${quote(s.sootMethod.toString)} -> ${quote(d.toString)};")
    //      }
    //    }
    //    println("}")

    val m = Soot.getSootClass(mainClass).getMethodByName(mainMethod) //Coverage2.freshenMethod(Soot.getSootClass(mainClass).getMethodByName(mainMethod))
    // The next three lines add edges from the main method to any method that has no incoming edges.
    val s = Stmt(Soot.getBody(m).getUnits.asScala.toList.head.asInstanceOf[SootStmt], m)
    val fromMain = appEdges2.getOrElse(m, Map())
    appEdges2 += m -> (fromMain + (s -> (fromMain.getOrElse(s, Set()) ++ roots)))
    println(f"Printing appEdges2(m): ${appEdges2(m)}")
    println(f"roots: $roots");

    computeLoopGraph(mainClass, mainMethod, /*classpath: String,*/
      outStream, coverageStream, jaam, prune, shrink, prettyPrint, m, appEdges2, roots)

  }

  def computeLoopGraph(mainClass: String,
                       mainMethod: String, /*classpath: String,*/
                       graphStream: PrintStream,
                       coverageStream: PrintStream,
                       jaam: String,
                       prune: Boolean,
                       shrink: Boolean,
                       prettyPrint: Boolean,
                       m: SootMethod,
                       cg: Map[SootMethod, Map[Stmt, Set[SootMethod]]],
                       roots: Set[SootMethod]): Unit = {
    // import org.ucombinator.jaam.tools.LoopAnalyzer
    import org.ucombinator.jaam.serializer

    val graph = makeLoopGraph(m, cg, prettyPrint)
    val pruned = if (prune) {
      graph.prune
    } else {
      graph
    }
    val shrunk = if (shrink) {
      pruned // Was: pruned.shrink // TODO: shrink currently goes in an infinite loop 
    } else {
      pruned
    }

    // TODO: print unpruned size
    val outSerializer = new serializer.PacketOutput(new FileOutputStream(jaam))
    shrunk.toJaam(outSerializer, roots)
    outSerializer.close()
    println(f"Prune = $prune, shrink = $shrink")
    println("number of roots: " + roots.size)
    println("set of roots: " + roots)

/*
    Console.withOut(graphStream) {
      println("digraph loops {")
      println("ranksep=\"10\";")
      print(shrunk)
      println("}")
    }
    */

    //    Console.withOut(coverageStream) {
    //      tools.LoopAnalyzer.computeCoverage(classpath, graph)
    //    }
  }

  def add(g: Map[Node, Set[Node]], from: Node, to: Node):
      Map[Node, Set[Node]] = {
    g + (from -> (g.getOrElse(from, Set.empty) + to))
  }
  def addForest(g: Map[Node, Set[Node]], node: Node,
      forest: Set[LoopTree], m: SootMethod): Map[Node, Set[Node]] = {
    forest.foldLeft(g)({ (g: Map[Node, Set[Node]], tree: LoopTree) =>
      val treeNode = LoopNode(m, tree.loop)
      addForest(add(g, node, treeNode), treeNode, tree.children, m)
    })
  }

  private var loopForests = Map.empty[SootMethod, Set[LoopTree]]
  def getLoopForest(m: SootMethod): Set[LoopTree] = {
    loopForests.get(m) match {
      case None =>
        val loops =
          if (m.isConcrete) { Loop.getLoopInfoSet(m).map(_.loop) }
          else { Set() }
        var forest = Set.empty[LoopTree]
        if (loops.nonEmpty) {
          forest = Set(LoopTree(loops.head, m))
          for {
            loop <- loops.tail
          } {
            val leaf = LoopTree(loop, m)
            val parents = forest.filter { (tree: LoopTree) =>
              tree.isParent(leaf)
            }
            if (parents.isEmpty) {
              val children = forest.filter { (tree: LoopTree) =>
                leaf.isParent(tree)
              }
              // This is correct even if children is empty
              val tree = LoopTree(loop, m, children)
              forest = (forest -- children) + tree
            } else {
              assert(parents.size <= 1, "multiple parents")
              val parent = parents.head
              forest = (forest - parent) + parent.insert(leaf)
            }
          }
          loopForests = loopForests + (m -> forest)
        }
        forest
      case Some(forest) => forest
    }
  }

  def makeLoopGraph(m: SootMethod,
                    cg: Map[SootMethod,Map[Stmt, Set[SootMethod]]],
                    prettyPrint: Boolean): LoopGraph = {
    // TODO if things get slow, this should be easy to optimize
    def build(m: SootMethod, g: Map[Node, Set[Node]]):
    Map[Node, Set[Node]] = {

      val mNode = MethodNode(m)
      if (g isDefinedAt mNode) {
        g
      } else {
        //val iterator = ??? //cg.edgesOutOf(m)
        val forest = getLoopForest(m)
        if (prettyPrint) {
          for (tree <- forest) {
            println("Tree:")
            tree.prettyPrint()
          }
        }
        // g keeps track of the methods we've seen, so adding the empty set
        // to it prevents an infinite loop.
        var newGraph: Map[Node, Set[Node]] = g + (mNode -> Set.empty)
        newGraph = addForest(g, mNode, forest, m)
        for ((stmt, methods) <- cg.getOrElse(m, Map())) {
          for (tgt <- methods) {
            println(f"src $stmt tgt $tgt")
            val sootStmt = stmt.sootStmt
            val dest = tgt

            // class initializers can't recur but Soot thinks they do
            if (m.getSignature != dest.getSignature || m.getName != "<clinit>"){
              val destNode = MethodNode(dest)
              val parents = forest filter { _ contains sootStmt }
              if (parents.isEmpty) {
                newGraph = add(newGraph, mNode, destNode)
              } else {
                assert(parents.size == 1, "multiple parents")
                val parent = LoopNode(m, parents.head.parent(sootStmt).loop)
                newGraph = add(newGraph, parent, destNode)
              }
              newGraph = build(dest, newGraph)
            }
          }
        }
        newGraph
      }
    }
    LoopGraph(m, build(m, Map.empty), Set.empty[(Node,Node)])
  }
}

case class LoopTree(loop: SootLoop, method: SootMethod, children: Set[LoopTree]) {
  def contains(stmt: SootStmt): Boolean = {
    loop.getLoopStatements.contains(stmt)
  }
  def isParent(other: LoopTree): Boolean = {
    other.loop.getLoopStatements.toSet.subsetOf(loop.getLoopStatements.toSet)
  }
  def insert(child: LoopTree): LoopTree = {
    val grandchildren: Set[LoopTree] = children filter child.isParent
    val parents: Set[LoopTree] = children filter { _.isParent(child) }

    if (parents.nonEmpty) {
      assert(parents.size <= 1,
          "two disparate loops contain the same child")
      assert(grandchildren.isEmpty, "malformed tree")
      val parent = parents.head
      val newParent = parent.insert(child)
      LoopTree(loop, method, (children - parent) + newParent)
    } else {
      // child becomes a direct descendant containing 0 or more children
      val node = LoopTree(child.loop, method, child.children ++ grandchildren)
      LoopTree(loop, method, (children -- grandchildren) + node)
    }
  }
  // assumes that its loop contains the stmt in question
  def parent(stmt: SootStmt): LoopTree = {
    val parents = children filter  { _.loop.getLoopStatements.contains(stmt) }
    if (parents.isEmpty) {
      this
    } else {
      assert(parents.size <= 1,
          "two disparate loops contain the same child")
      parents.head.parent(stmt)
    }
  }
  def prettyPrint(indent: Int = 0): Unit = {
    println(f"Method: $method")
    println("Head:")
    println(loop.getHead)
    for (stmt <- loop.getLoopStatements) {
      val next = Stmt(stmt, method).nextSemantic.map(_.sootStmt).map({x => Stmt.getIndex(x, method)})
      println(f"Stmt: ${Stmt.getIndex(stmt, method)} $next")
      println(stmt)
    }
    println()
    println("Children:")
    for (child <- children) {
      child.prettyPrint(indent+1)
      println()
    }
    println("End Children:")

    val graph = new DefaultDirectedGraph[SootStmt, DefaultEdge](classOf[DefaultEdge])

    // TODO: all nextSemantic that are not in the loop are exit jumps
    // Include them in graph along with a synthetic final statement
    for (node <- loop.getLoopStatements) {
      graph.addVertex(node)
    }

    for (node <- loop.getLoopStatements) {
      for (target <- Stmt(node, method).nextSemantic) {
        if (graph.containsVertex(target.sootStmt)) {
          graph.addEdge(node, target.sootStmt)
        }
      }
    }

    println("START_GRAPH")
    println(graph)
    println("END_GRAPH")

    val imm = dominatorTree(loop.getHead, graph)

    //new DOTExporter().exportGraph(imm, System.out)
    println("START_IMM")
    for (i <- imm.keys) {
      println(f"${Stmt.getIndex(i, method)}:$i -> ${Stmt.getIndex(imm(i), method)}:${imm(i)}")
    }
    println("END_IMM")
  }

  def dominatorTree[V,E](root: V, graph: Graph[V,E]): immutable.Map[V, V] = {
    val dom = new mutable.HashMap[V, mutable.Set[V]] with mutable.MultiMap[V, V]

    dom.addBinding(root, root)
    for (i <- graph.vertexSet if i != root) {
      dom(i) = graph.vertexSet
    }

    println("START_DOM")
    for (i <- dom.keys) {
      println(f"key: $i")
      println("val:"+dom(i))
    }
    println("END_DOM")

    var done = false
    while (!done) {
      done = true
      for (i <- graph.vertexSet if i != root) {
        var newDom = dom(i).clone()
        for (j <- Graphs.predecessorListOf(graph, i)) {
          newDom = (newDom & dom(j)) + i
        }
        if (newDom != dom(i)) {
          dom(i) = newDom
          done = false
        }
      }
    }

    println("START_DOM2")
    for (i <- dom.keys) {
      println(f"key: $i")
      println("val:"+dom(i))
    }
    println("END_DOM2")

    var imm: immutable.Map[V, V] = Map.empty

    for (i <- graph.vertexSet if i != root) {
      for (j <- dom(i)) {
        println(f"imm: ${dom(i).size} ${dom(j).size} $i $j")
        if (dom(j).size == dom(i).size - 1) {
          imm += (i -> j)
        }
      }
    }

    return imm
  }
}
object LoopTree {
  def apply(loop: SootLoop, method: SootMethod): LoopTree = new LoopTree(loop, method, Set.empty)
}

abstract sealed class Node extends CachedHashCode {
  val tag: String
}
case class LoopNode(m: SootMethod, loop: SootLoop) extends Node {
  override val tag: String = m.getSignature + "\ninstruction #" + Stmt(loop.getHead, m).index
  val index: Int = Stmt(loop.getHead, m).index
}
// TODO we might have uniqueness problems with SootMethod objects.
// For now, SootMethod.getSignature will do.
case class MethodNode(method: SootMethod) extends Node {
  override val tag: String = method.getSignature
}

case class LoopGraph(m: SootMethod, private val g: Map[Node, Set[Node]],
    private val recurEdges: Set[(Node, Node)]) {
  private val mNode = MethodNode(m)

  def apply(n: Node): Set[Node] = g.getOrElse(n, Set.empty)

  def keySet: Set[String] = g.keySet.
    withFilter(_.isInstanceOf[MethodNode]).
    map(_.tag)

  def +(binding: (Node, Set[Node])): LoopGraph = {
    val (k, v) = binding
    LoopGraph(m, g + (k -> (this(k) ++ v)), recurEdges)
  }

  // remove method leaves
  def prune: LoopGraph = {
    var keepMap: Map[Node, Boolean] = Map.empty[Node, Boolean]
    var parentMap: Map[Node, Set[Node]] = Map.empty[Node, Set[Node]]
    var recursionEdges = recurEdges

    def analyze(n: Node, path: List[Node]): Unit = {
      if (!keepMap.isDefinedAt(n)) {
        if (path.contains(n)) {
          val loopNodes = n :: path.takeWhile(_ != n)
          val rotated = loopNodes.tail :+ n
          recursionEdges = recursionEdges ++ loopNodes.zip(rotated)
          keepMap = keepMap + (n -> true)
        } else {
          val succs = this(n)
          for {
            succ <- succs
          } {
            val parents = parentMap.getOrElse(succ, Set.empty) + n
            parentMap = parentMap + (succ -> parents)
            analyze(succ, n :: path)
          }
          val keep = succs.foldLeft(n.isInstanceOf[LoopNode] ||
                                    keepMap.getOrElse(n, false))({
              (keep: Boolean, succ: Node) =>
            keepMap.get(succ) match {
              case Some(keepSucc) => keep || keepSucc
              case None =>
                println("WARNING: " + succ +
                    " should already have been analyzed")
                keep
            }
          })
          keepMap = keepMap + (n -> keep)
        }
      }
    }
    analyze(mNode, List())
    val newGraph = keepMap.foldLeft(g)({
        (g: Map[Node, Set[Node]], pair: (Node, Boolean)) =>
      val (n, keep) = pair
      if (keep) {
        g
      } else {
        val parents = parentMap.getOrElse(n, Set.empty)
        parents.foldLeft(g - n)({
            (g: Map[Node, Set[Node]], parent: Node) =>
          g + (parent -> (g.getOrElse(parent, Set.empty) - n))
        })
      }
    })
    LoopGraph(m, newGraph, recursionEdges)
  }

  // remove loopless method calls, replacing them with downstream loops
  def shrink: LoopGraph = {
    // keepers is a set of MethodNode objects that should remain. All LoopNode
    // objects are kept, so there's no need to add them to a set.
    var keepers = Set(mNode)
    // descMap keeps track of the descendants to be kept from a node. Nodes
    // that should be kept return a set containing just themselves; nodes that
    // are to be discarded return the merged results from their children.
    var descMap = Map.empty[Node, Set[Node]]
    var newGraph = g
    var recursionEdges = recurEdges
    def shouldKeep(n: Node): Boolean = {
      keepers.contains(n) || n.isInstanceOf[LoopNode]
    }
    def analyze(n: Node, path: List[Node]): Set[Node] = {
      //println(f"analyzer $n $path")
      n match {
        // if there is a loop,
        case m: MethodNode if path.contains(n) =>
          // get the method nodes in the loop and mark them
          val loopNodes = n :: path.takeWhile(_ != n)
          val rotated = loopNodes.tail :+ n
          recursionEdges = recursionEdges ++ loopNodes.zip(rotated)
          val toKeep = loopNodes flatMap {
            case m: MethodNode => Some(m)
            case _ => None
          }
          keepers = keepers ++ toKeep
          Set(n)
        case _ =>
          // recur and store the resulting sets of descendants
          for {
            child <- this(n)
          } {
            if (!descMap.isDefinedAt(child)) {
              descMap = descMap + (child -> analyze(child, n :: path))
            }
          }
          // in the case that n should be kept,
          if (shouldKeep(n)) {
            // replace each child with the set returned by its call to analyze
            for {
              child <- this(n) filter { !shouldKeep(_) }
            } {
              val newChildren = (newGraph(n) - child) ++ descMap(child)
              newGraph = newGraph + (n -> newChildren)
            }
            // and keep n
            Set(n)
          } else {
            // otherwise, roll all of the children's sets together
            // crucially, the set returned does not include n
            this(n).foldLeft(Set.empty[Node])({
              (descendants: Set[Node], child: Node) =>
                descendants ++ descMap(child)
            })
          }
      }
    }
    analyze(mNode, List.empty)
    LoopGraph(m, newGraph, recursionEdges)
  }

/*
  override def toString: String = {
    val builder = new StringBuilder
    var seen = Set.empty[Node]
    def inner(from: Node): Unit = {
      if (!seen.contains(from)) {
        seen = seen + from
        builder ++= from.toString
        for {
          to <- this(from)
        } {
          val maybeColored = if (recurEdges.contains((from, to))) {
            " [penwidth=10, color=\"blue\"]"
          } else " [penwidth=10]"
          builder ++= "  " + quote(from.tag) + " -> " + quote(to.tag) +
            maybeColored + ";\n"
        }
        // enforce a BFS order
        for {
          to <- this(from)
        } {
          inner(to)
        }
      }
    }
    inner(mNode)
    builder.toString
  }
  */

  def getByIndex(sootMethod : SootMethod, index: Int) : SootStmt = {
    assert(index >= 0, "index must be nonnegative")
    val units = sootMethod.retrieveActiveBody.getUnits.toList
    assert(index < units.length, "index must not overflow the list of units")
    val unit = units(index)
    assert(unit.isInstanceOf[SootStmt], "the index specifies a Soot Unit that is not a Stmt. It is a " + unit.getClass)
    unit.asInstanceOf[SootStmt]
  }

  // TODO petey/michael: is InvokeExpr the only expr with side effects?
  def addrsOf(expr: SootValue, m: SootMethod): Set[TaintAddress] = {
    expr match {
      case l : Local => Set(LocalTaintAddress(m, l))
      // TODO this could throw an exception
      case pr: ParameterRef => Set(ParameterTaintAddress(m, pr.getIndex))
      case t: ThisRef => Set(ThisRefTaintAddress(m))
      case r : Ref => Set(RefTaintAddress(m, r))
      // case _ : Constant => Set(ConstantTaintAddress(m))
      case c : Constant => Set(ConstantTaintAddress(m, c))
      case unop : UnopExpr => addrsOf(unop.getOp, m)
      case binop : BinopExpr =>
        // TODO in the case of division, this could throw an exception
        addrsOf(binop.getOp1, m) ++
          addrsOf(binop.getOp2, m)
      case io : InstanceOfExpr => addrsOf(io.getOp, m)
        // TODO this could throw an exception
      case cast : CastExpr => addrsOf(cast.getOp, m)
      case invoke : InvokeExpr => Set(InvokeTaintAddress(m, invoke))
      case na : NewArrayExpr =>
        addrsOf(na.getSize, m)
      case _ : NewExpr => Set.empty
      case nma : NewMultiArrayExpr =>
        nma.getSizes.toSet flatMap { (exp: SootValue) => addrsOf(exp, m) }
      case _ =>
        println(expr)
        ???
    }
  }


  def toJaam(s: serializer.PacketOutput,
             roots: Set[SootMethod] = Set()) {
    var seen = Set.empty[Node]
    var names = Map.empty[Node, serializer.Id[serializer.LoopNode]]
    def name(node: Node): serializer.Id[serializer.LoopNode] = {
      names.get(node) match {
        case Some(id) => id
        case None =>
          val id = serializer.Id[serializer.LoopNode](names.size)
          names += (node -> id)
          id
      }
    }
    def inner(from: Node): Unit = {
      if (!seen.contains(from)) {
        seen = seen + from
        val id = name(from)
        val packet = from match {
          case MethodNode(m) =>
            println(f"Serializing method: $m")
            serializer.LoopMethodNode(id, m)
          case n@LoopNode(m, _) =>
            val stmt = getByIndex(m, n.index+1) // add one because the loop node is apparently the instruction before...?
            val addrs = stmt match {
              case sootStmt: IfStmt => addrsOf(sootStmt.getCondition, m)
              case _ =>
                println("TODO: investigate why the loop guard is not an IfStmt (" + stmt + ")")
                Set.empty[serializer.TaintAddress]
            }
            serializer.LoopLoopNode(id, m, addrs, n.index)
        }

        // println("Writing: " + packet)
        s.write(packet)
        for {
          to: Node <- this(from)
        } {
          // TODO: Instead of ignoring the roots this way, modify the BFS, both here and in makeLoopGraph in Loop3.
          if (!to.isInstanceOf[MethodNode] || !roots.contains(to.asInstanceOf[MethodNode].method)) {
            // println("Edge: " + name(from) + "->" + name(to))
            s.write(serializer.LoopEdge(
              name(from), name(to), recurEdges.contains((from, to))))
          }
          else {
            // println("Skipping edge: " + name(from) + "->" + name(to))
          }
        }

        // enforce a BFS order
        for (to <- this(from)) inner(to)
      }
    }
    inner(mNode)
  }
}

// ./bin/jaam-tools loop3 --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/airplan_1.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-cli-1.3.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-codec-1.9.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-fileupload-1.3.1.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-io-2.2.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-lang3-3.4.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/commons-logging-1.2.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/httpclient-4.5.1.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/httpcore-4.4.3.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/jline-2.8.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/log4j-1.2.17.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/mapdb-2.0-beta8.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/netty-all-4.0.34.Final.jar --classpath ../../engagements/en4/article/stac_engagement_4_release_v1.1/challenge_programs/airplan_1/challenge_program/lib/protobuf-java-3.0.0-beta-2.jar --classpath resources/rt.jar


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
