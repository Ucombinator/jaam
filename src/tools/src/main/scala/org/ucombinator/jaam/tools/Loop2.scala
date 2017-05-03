package org.ucombinator.jaam.tools

// TODO: nodes for loops (allows us to group methods that are in the same loop)
// TODO: vertical separation
// TODO: how confident are we in the coverage of rsa_commander?
// TODO: headlabel
// TODO: Compound graphs so loops live inside methods?
// TODO: test coverage

import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.PrintStream
import java.util.jar.JarEntry
import java.util.jar.JarInputStream

import scala.collection.JavaConversions._

import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt}
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}
import soot.jimple.toolkits.callgraph.{CallGraph, CHATransformer, Edge}
import soot.options.Options
import soot.tagkit.GenericAttribute
import soot.toolkits.graph.LoopNestTree

import org.ucombinator.jaam.serializer

class LoopAnalyzer extends Main("loop2") {
  banner("Analyze the depth of each loop in the application code")
  footer("")

  // val graph = opt[Boolean](descr = "Print loops to GraphViz dot file")
  // val rec = opt[Boolean](descr = "Run recursion detection")
  // TODO name this
  val prune = toggle(
      descrYes = "Remove methods without outgoing edges from graph",
      descrNo = "Do not remove methods without outgoing edges from graph",
      default = Some(true))
  val shrink = toggle(descrYes = "Skip methods without loops",
      descrNo = "Include methods without loops", default = Some(true))

  val mainClass = trailArg[String](descr = "The name of the main class")
  val mainMethod = trailArg[String](descr = "The name of the main method")
  val classpath = trailArg[String](descr =
      "Colon-separated list of JAR files and directories")
  val output = opt[String](descr = "An output file for the dot output")
  val coverage = opt[String](descr = "An output file for the coverage output")
  val jaam = opt[String](short = 'h', descr = "the output file for the serialized data")

  def run(conf: Conf): Unit = {
    val outStream: PrintStream = output.toOption match {
      case None => System.out
      case Some(f) => new PrintStream(new FileOutputStream(f))
    }
    val coverageStream: PrintStream = coverage.toOption match {
      case None => System.out
      case Some(f) => new PrintStream(new FileOutputStream(f))
    }
    LoopAnalyzer.main(mainClass(), mainMethod(), classpath(), outStream, coverageStream, jaam.toOption,
        prune(), shrink())
  }
}

object LoopAnalyzer {
  private var loops = Map.empty[SootMethod, Set[SootLoop]]
  def getLoops(m: SootMethod): Set[SootLoop] = {
    loops.get(m) match {
      case Some(l) => l
      case None =>
        var body: Body = null
        try {
          body = m.retrieveActiveBody
        } catch {
          case _: RuntimeException =>
            // println("Unable to retrieve body for " + m.getName)
        }
        val result = if (body != null) {
          new LoopNestTree(body).toSet
        } else {
          Set.empty[SootLoop]
        }
        loops = loops + (m -> result)
        result
    }
  }

  case class LoopTree(val loop: SootLoop, val children: Set[LoopTree]) {
    def contains(stmt: SootStmt): Boolean = {
      loop.getLoopStatements.contains(stmt)
    }
    def isParent(other: LoopTree): Boolean = {
      other.loop.getLoopStatements.toSet.subsetOf(loop.getLoopStatements.toSet)
    }
    def insert(child: LoopTree): LoopTree = {
      val grandchildren: Set[LoopTree] = children flatMap {
        case ln: LoopTree if child.isParent(ln) => Some(ln)
        case _ => None
      }
      val parents: Set[LoopTree] = children flatMap {
        case ln: LoopTree if ln.isParent(child) => Some(ln)
        case _ => None
      }
      if (parents.nonEmpty) {
        assert(parents.size <= 1,
            "two disparate loops contain the same child")
        assert(grandchildren.isEmpty, "malformed tree")
        val parent = parents.head
        val newParent = parent.insert(child)
        LoopTree(loop, (children - parent) + newParent)
      } else {
        // child becomes a direct descendant containing 0 or more children
        val node = LoopTree(child.loop, child.children ++ grandchildren)
        LoopTree(loop, (children -- grandchildren) + node)
      }
    }
    // assumes that its loop contains the stmt in question
    def parent(stmt: SootStmt): LoopTree = {
      val parents = children flatMap {
        case ln: LoopTree if ln.loop.getLoopStatements.contains(stmt) =>
          Some(ln)
        case _ => None
      }
      if (parents.isEmpty) {
        this
      } else {
        assert(parents.size <= 1,
            "two disparate loops contain the same child")
        parents.head.parent(stmt)
      }
    }
  }
  object LoopTree {
    def apply(loop: SootLoop): LoopTree = new LoopTree(loop, Set.empty)
  }

  def encode(s: String): String = s.replace("\"", "\\\"")
  def quote(s: String): String = "\"" + encode(s) + "\""

  abstract sealed class Node {
    val tag: String
    val annotation = ""
    override def toString: String = ""
  }
  case class LoopNode(val m: SootMethod, val loop: SootLoop) extends Node {
    override val tag = {
      val stmt = Statement(loop.getHead, m)
      m.getSignature + "\ninstruction #" + stmt.index
    }
    override def toString = "  " + quote(tag) + " [shape=diamond];\n"
  }
  // TODO we might have uniqueness problems with SootMethod objects.
  // For now, SootMethod.getSignature will do.
  case class MethodNode(override val tag: String) extends Node

  case class LoopGraph(val m: SootMethod, private val g: Map[Node, Set[Node]],
      private val recurEdges: Set[(Node, Node)]) {
    private val mNode = MethodNode(m.getSignature)
    def apply(n: Node): Set[Node] = g.getOrElse(n, Set.empty)
    def keySet: Set[String] = g.keySet flatMap {
      case MethodNode(sig) => Some(sig)
      case _ => None
    }
    def +(binding: (Node, Set[Node])): LoopGraph = {
      val (k, v) = binding
      LoopGraph(m, g + (k -> (this(k) ++ v)), recurEdges)
    }
    // remove method leaves
    def prune: LoopGraph = {
      var keepMap = Map.empty[Node, Boolean]
      var parentMap = Map.empty[Node, Set[Node]]
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
              val parents = (parentMap.getOrElse(succ, Set.empty) + n)
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
                val newChildren = ((newGraph(n) - child) ++ descMap(child))
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

    def toJaam(s: serializer.PacketOutput) {
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
          val isMethod = from.isInstanceOf[MethodNode]
          s.write(serializer.LoopNode(id, isMethod, from.toString))
          for {
            to <- this(from)
          } {
            s.write(serializer.LoopEdge(
              name(from), name(to), recurEdges.contains((from, to))))
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
    }
  }
  object LoopGraph {
      private def add(g: Map[Node, Set[Node]], from: Node, to: Node):
          Map[Node, Set[Node]] = {
        g + (from -> (g.getOrElse(from, Set.empty) + to))
      }
      private def addForest(g: Map[Node, Set[Node]], node: Node,
          forest: Set[LoopTree], m: SootMethod): Map[Node, Set[Node]] = {
        forest.foldLeft(g)({ (g: Map[Node, Set[Node]], tree: LoopTree) =>
          val treeNode = LoopNode(m, tree.loop)
          addForest(add(g, node, treeNode), treeNode, tree.children, m)
        })
      }
    def apply(m: SootMethod, cg: CallGraph): LoopGraph = {
      // TODO if things get slow, this should be easy to optimize
      def build(m: SootMethod, g: Map[Node, Set[Node]]):
          Map[Node, Set[Node]] = {
        val mNode = MethodNode(m.getSignature)
        if (g isDefinedAt mNode) {
          g
        } else {
          val iterator = cg.edgesOutOf(m)
          val forest = getLoopForest(m)
          // g keeps track of the methods we've seen, so adding the empty set
          // to it prevents an infinite loop.
          var newGraph: Map[Node, Set[Node]] = g + (mNode -> Set.empty)
          newGraph = addForest(g, mNode, forest, m)
          while (iterator.hasNext) {
            val edge = iterator.next
            val sootStmt = edge.srcStmt
            val dest = Coverage2.freshenMethod(edge.tgt)

            // class initializers can't recur but Soot thinks they do
            if (m.getSignature != dest.getSignature || m.getName != "<clinit>"){
              val destNode = MethodNode(dest.getSignature)
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
          newGraph
        }
      }
      LoopGraph(m, build(m, Map.empty), Set.empty)
    }
  }

  private var loopForests = Map.empty[SootMethod, Set[LoopTree]]
  private def getLoopForest(m: SootMethod): Set[LoopTree] = {
    loopForests.get(m) match {
      case None =>
        val loops = getLoops(m)
        var forest = Set.empty[LoopTree]
        if (loops.nonEmpty) {
          forest = Set(LoopTree(loops.head))
          for {
            loop <- loops.tail
          } {
            val leaf = LoopTree(loop)
            val parents = forest.filter { (tree: LoopTree) =>
              tree.isParent(leaf)
            }
            if (parents.isEmpty) {
              val children = forest.filter { (tree: LoopTree) =>
                leaf.isParent(tree)
              }
              // This is correct even if children is empty
              val tree = LoopTree(loop, children)
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

  def computeCoverage(classPath: String, graph: LoopGraph): Unit = {
    def add(map0: Map[String, Int], string: String): Map[String, Int] = {
      var map = map0
      for (ss <- string.split('.').inits) {
        val path = ss.mkString(".")
        map += (path -> (map.getOrElse(path, 0) + 1))
      }
      return map
    }
    var expected = Map[String, Int]()
    var actual = Map[String, Int]()
    var missing = Map[String, Set[String]]()
    for (s <- Taint.getAllClasses(classPath)) {
      val c = Scene.v().loadClass(s, SootClass.SIGNATURES)
      for (m <- c.getMethods()) {
        val name = c.getPackageName + "." + c.getName
        expected = add(expected, name)
        if (!graph.keySet.contains(m.getSignature)) {
          missing += (name -> (missing.getOrElse(name, Set()) + m.getSubSignature))
        } else {
          actual = add(actual, name)
        }
      }
    }

    for (k <- expected.keys.toList.sorted) {
      println("expected=" + expected.getOrElse(k, 0) + " found=" + actual.getOrElse(k, 0) + " name=" + k)
      for (s <- missing.getOrElse(k, Set()).toList.sorted) {
        println("  missing=" + s)
      }
    }
  }

  def main(mainClass: String, mainMethod: String, classpath: String,
      graphStream: PrintStream, coverageStream: PrintStream, jaam: Option[String], prune: Boolean, shrink: Boolean): Unit = {
    Options.v().set_verbose(false)
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_keep_line_number(true)
    Options.v().set_allow_phantom_refs(true)
    Options.v().set_soot_classpath(classpath)
    Options.v().set_include_all(true)
    Options.v().set_prepend_classpath(false)
    Options.v().set_src_prec(Options.src_prec_only_class)
    Options.v().set_whole_program(true)
    Options.v().set_app(true)
    soot.Main.v().autoSetOptions()

    Options.v().setPhaseOption("cg", "verbose:true")
    Options.v().setPhaseOption("cg.cha", "enabled:true")

    Options.v.set_main_class(mainClass)
    val clazz = Scene.v.forceResolve(mainClass, SootClass.BODIES)
    Scene.v.setMainClass(clazz)
    val m = Coverage2.freshenMethod(clazz.getMethodByName(mainMethod))

    for {
      className <- Taint.getAllClasses(classpath)
    } {
      Scene.v.addBasicClass(className, SootClass.HIERARCHY)
    }

    Scene.v.setSootClassPath(classpath)
    Scene.v.loadNecessaryClasses

    PackManager.v.runPacks
    CHATransformer.v.transform
    val cg = Scene.v.getCallGraph

    val graph = LoopGraph(m, cg)
    val pruned = if (prune) {
      graph.prune
    } else {
      graph
    }
    val shrunk = if (shrink) {
      pruned.shrink
    } else {
      pruned
    }

    // TODO: print unpruned size
    jaam match {
      case None =>
      case Some(jaamFile) =>
        val outSerializer = new serializer.PacketOutput(new FileOutputStream(jaamFile))
        shrunk.toJaam(outSerializer)
        outSerializer.close()
    }

    Console.withOut(graphStream) {
      println("digraph loops {")
      println("ranksep=\"10\";");
      print(shrunk)
      println("}")
    }

    Console.withOut(coverageStream) {
      computeCoverage(classpath, graph)
    }
  }
}

case class Statement(val stmt: SootStmt, val m: SootMethod) {
  assert(stmt != null, "trying to create a Statement with a null object")
  val index = if (stmt.hasTag(Statement.indexTag)) {
    BigInt(stmt.getTag(Statement.indexTag).getValue).intValue
  } else {
    // label everything in m so the amortized work is linear
    for ((u, i) <- Soot.getBody(m).getUnits().toList.zipWithIndex) {
      u.addTag(new GenericAttribute(Statement.indexTag, BigInt(i).toByteArray))
    }

    assert(stmt.hasTag(Statement.indexTag),
        "SootStmt "+stmt+" not found in SootMethod " + m)
    BigInt(stmt.getTag(Statement.indexTag).getValue).intValue
  }
}

object Statement {
  val indexTag = "org.ucombinator.jaam.Statement.indexTag"
}
