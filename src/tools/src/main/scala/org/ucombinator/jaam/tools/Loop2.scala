package org.ucombinator.jaam.tools

import java.io.FileOutputStream
import java.io.PrintStream

import scala.collection.JavaConversions._

import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt}
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}
import soot.jimple.toolkits.callgraph.{CallGraph, CHATransformer, Edge}
import soot.options.Options
import soot.tagkit.GenericAttribute
import soot.toolkits.graph.LoopNestTree

class LoopAnalyzer extends Main("loop2") {
  banner("Analyze the depth of each loop in the application code")
  footer("")

  // val graph = opt[Boolean](descr = "Print loops to GraphViz dot file")
  // val rec = opt[Boolean](descr = "Run recursion detection")
  // TODO name this
  val all = opt[Boolean](descr =
      "Print loops even if no methods are called within them")

  val mainClass = trailArg[String](descr = "The name of the main class")
  val mainMethod = trailArg[String](descr = "The name of the main method")
  val classpath = trailArg[String](descr =
      "Colon-separated list of JAR files and directories")
  val output = opt[String](descr = "An output file for the dot output")

  def run(conf: Conf): Unit = {
    val outStream: PrintStream = output.toOption match {
      case None => System.out
      case Some(f) => new PrintStream(new FileOutputStream(f))
    }
    LoopAnalyzer.main(mainClass(), mainMethod(), classpath(), outStream,
        all())
  }
}

object LoopAnalyzer {
  def getTargets(u: SootUnit, cg: CallGraph): Set[SootMethod] = {
    var targets = Set.empty[SootMethod]
    val iterator = cg.edgesOutOf(u)
    while(iterator.hasNext) {
      val e: Edge = iterator.next
      e.getTgt match {
        case m: SootMethod => targets = targets + m
        case _ => {}
      }
    }
    targets
  }
  private def stmtCount(loop: SootLoop): Int = {
    loop.getLoopStatements.size
  }
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

  private var loopsContaining = Map.empty[Stmt, Set[SootLoop]]
  def getLoopsContaining(stmt: Stmt): Set[SootLoop] = {
    loopsContaining.get(stmt) match {
      case Some(loops) => loops
      case None =>
        val loops = getLoops(stmt.m)
        val result = loops filter { (loop: SootLoop) =>
          loop.getLoopStatements.contains(stmt.stmt)
        }
        loopsContaining = loopsContaining + (stmt -> result)
        result
    }
  }

  private var loopDepths = Map.empty[Stmt, Int]
  def getLoopDepth(stmt: Stmt): Int = {
    loopDepths.get(stmt) match {
      case Some(i) => i
      case None =>
        val result = getLoopsContaining(stmt).size
        loopDepths = loopDepths + (stmt -> result)
        result
    }
  }

  private var emitted = Set.empty[String]
  private def emit(str: String): Unit = {
    if (!emitted.contains(str)) {
      emitted = emitted + str
      println(str)
    }
  }

  /*
  def emitEdge(from: String, to: String, isLoop: Boolean = false,
      label: Option[String] = None): Unit = {
    val end = label match {
      case None => ""
      case Some(l) => " [" + l + "]"
    }
    val edge = "  " + string(from) + " -> " + string(to) + end + ";"
    emit(edge)
    if (isLoop) {
      emitNodeAnnotation(to, "shape=diamond")
    }
  }
  def emitNodeAnnotation(node: String, annotation: String): Unit = {
    val str = "  " + string(node) + " [" + annotation + "];"
    emit(str)
  }
  private def emitLineage(src: String, trees: Set[Node], stmt: SootStmt,
      dest: String): Unit = {
    val containing = trees filter { (tree: LoopNode) => tree.contains(stmt) }
    if (containing.isEmpty) {
      emitEdge(src, dest)
    } else {
      assert(containing.size == 1, "bad tree structure")
      val next = containing.head
      val nodeLabel = loopString(next.loop)
      emitEdge(src, nodeLabel, true)
      emitLineage(nodeLabel, next.children, stmt, dest)
    }
  }
  private def justEmitForest(src: String, forest: Set[Node]): Unit = {
    for {
      tree <- forest
    } {
      val text = loopString(tree.loop)
      emitEdge(src, text, true)
      justEmitForest(text, tree.children)
    }
  }
  private def emitLoopForest(src: String, forest: Set[LoopNode], stmt: SootStmt,
      dest: String): Unit = {
    var containing = 0
    for {
      tree <- forest
    } {
      val text = loopString(tree.loop)
      emitEdge(src, text, true)
      if (tree.contains(stmt)) {
        containing = containing + 1
        emitLoopForest(text, tree.children, stmt, dest)
      } else {
        justEmitForest(text, tree.children)
      }
    }
    assert(containing <= 1, "bad tree structure")
    if (containing == 0) {
      emitEdge(src, dest)
    }
  }
  */

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

  abstract class Node {
    val tag: String
    val annotation = ""
    override def toString: String = ""
  }
  case class LoopNode(val loop: SootLoop) extends Node {
    override val tag = loop.getHead.toString()
    override def toString = "  " + quote(tag) + " [shape=diamond];\n"
  }
  // TODO we might have uniqueness problems with SootMethod objects.
  // For now, SootMethod.getSignature will do.
  case class MethodNode(override val tag: String) extends Node {
  }

  case class LoopGraph(private val g: Map[Node, Set[Node]]) {
    def apply(n: Node): Set[Node] = g.getOrElse(n, Set.empty)
    def keySet: Set[String] = g.keySet flatMap {
      case MethodNode(sig) => Some(sig)
      case _ => None
    }
    def +(binding: (Node, Set[Node])): LoopGraph = {
      val (k, v) = binding
      LoopGraph(g + (k -> (this(k) ++ v)))
    }
    // TODO if things get slow, this should be easy to optimize
    def ++(binding: (Node, Set[LoopTree])): LoopGraph = {
      val (node, forest) = binding
      forest.foldLeft(this)({ (g: LoopGraph, tree: LoopTree) =>
        val treeNode = LoopNode(tree.loop)
        (g + (node -> Set(treeNode))) ++ (treeNode -> tree.children)
      })
    }
    // TODO remove method leaves
    def prune: LoopGraph = ???
    // TODO remove loopless method calls, replacing them with downstream loops
    def condense: LoopGraph = ???
    def printGraph(from: Node, seen: Set[Node] = Set.empty): Unit = {
      if (!seen.contains(from)) {
        // don't print a newline; toString includes a newline if we want one
        print(from)
        for {
          to <- this(from)
        } {
          println("  " + quote(from.tag) + " -> " + quote(to.tag) + ";")
        }
        // enforce a BFS order
        for {
          to <- this(from)
        } {
          printGraph(to, seen + from)
        }
      }
    }

    def build(m: SootMethod, cg: CallGraph): LoopGraph = {
      val mNode = MethodNode(m.getSignature)
      if (g isDefinedAt mNode) {
        this
      } else {
        val iterator = cg.edgesOutOf(m)
        val forest = getLoopForest(m)
        var newGraph = this ++ (mNode -> forest)
        while (iterator.hasNext) {
          val edge = iterator.next
          val sootStmt = edge.srcStmt
          val stmt = Stmt(sootStmt, m)
          val dest = Coverage2.freshenMethod(edge.tgt)
          val destNode = MethodNode(dest.getSignature)
          val parents = forest filter { _ contains sootStmt }
          if (parents.isEmpty) {
            newGraph = newGraph + (mNode -> Set[Node](destNode))
          } else {
            assert(parents.size == 1, "multiple parents")
            val parent = LoopNode(parents.head.parent(sootStmt).loop)
            newGraph = newGraph + (parent -> Set[Node](destNode))
          }
          newGraph = newGraph.build(dest, cg)
        }
        newGraph
      }
    }
  }
  object LoopGraph {
    def empty: LoopGraph = new LoopGraph(Map.empty)
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

  def main(mainClass: String, mainMethod: String, classpath: String,
      graphStream: PrintStream, allLoops: Boolean): Unit = {
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

    val graph = LoopGraph.empty.build(m, cg)

    Console.withOut(graphStream) {
      println("digraph loops {")
      println("ranksep=\"10\";");
      graph.printGraph(MethodNode(m.getSignature))
      println("}")
    }
  }
}

case class Stmt(val stmt: SootStmt, val m: SootMethod) {
  assert(stmt != null, "trying to create a Stmt with a null object")
  val index = if (stmt.hasTag(Stmt.indexTag)) {
    BigInt(stmt.getTag(Stmt.indexTag).getValue).intValue
  } else {
    // label everything in m so the amortized work is linear
    for ((u, i) <- Soot.getBody(m).getUnits().toList.zipWithIndex) {
      u.addTag(new GenericAttribute(Stmt.indexTag, BigInt(i).toByteArray))
    }

    assert(stmt.hasTag(Stmt.indexTag),
        "SootStmt "+stmt+" not found in SootMethod " + m)
    BigInt(stmt.getTag(Stmt.indexTag).getValue).intValue
  }
}

object Stmt {
  val indexTag = "org.ucombinator.jaam.Stmt.indexTag"
}
