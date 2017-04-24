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
            println("Unable to retrieve body for " + m.getName)
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
  def emitEdge(from: String, to: String, isLoop: Boolean = false,
      label: Option[String] = None): Unit = {
    val end = label match {
      case None => ""
      case Some(l) => " [" + l + "]"
    }
    val edge = "  \"" + from + "\" -> \"" + to + "\"" + end + ";"
    emit(edge)
    if (isLoop) {
      emitNodeAnnotation(to, "shape=diamond")
    }
  }
  def emitNodeAnnotation(node: String, annotation: String): Unit = {
    val str = "  \"" + node + "\" [" + annotation + "];"
    emit(str)
  }
  private def emitLineage(src: String, trees: Set[LoopTree], stmt: SootStmt,
      dest: String): Unit = {
    val containing = trees filter { (tree: LoopTree) => tree.contains(stmt) }
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
  private def justEmitForest(src: String, forest: Set[LoopTree]): Unit = {
    for {
      tree <- forest
    } {
      val text = loopString(tree.loop)
      emitEdge(src, text, true)
      justEmitForest(text, tree.children)
    }
  }
  private def emitLoopForest(src: String, forest: Set[LoopTree], stmt: SootStmt,
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

  private def isParent(mParent: SootLoop, mChild: SootLoop): Boolean = {
    mChild.getLoopStatements.toSet.subsetOf(mParent.getLoopStatements.toSet)
  }
  case class LoopTree(val loop: SootLoop, val children: Set[LoopTree]) {
    def contains(stmt: SootStmt): Boolean = {
      loop.getLoopStatements.contains(stmt)
    }
    def insert(child: SootLoop): LoopTree = {
      val grandchildren = children filter { (ln: LoopTree) =>
        isParent(child, ln.loop)
      }
      val parents = children filter { (ln: LoopTree) =>
        isParent(ln.loop, child)
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
        val node = LoopTree(child, grandchildren)
        LoopTree(loop, (children -- grandchildren) + node)
      }
    }
    // assumes that its loop contains the stmt in question
    def parent(stmt: SootStmt): LoopTree = {
      val parents = children filter { (parent: LoopTree) =>
        parent.loop.getLoopStatements.contains(stmt)
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

  // TODO how do we want these to render?
  def loopString(loop: SootLoop): String = {
    loop.getHead.toString
  }

  private def printTree(tree: LoopTree): Unit = {
    for {
      child <- tree.children
    } {
      emitEdge(loopString(tree.loop), loopString(child.loop))
    }
    for {
      child <- tree.children
    } printTree(child)
  }

  private var loopForests = Map.empty[SootMethod, Set[LoopTree]]
  private def getLoopForest(m: SootMethod): Set[LoopTree] = {
    loopForests.get(m) match {
      case None =>
        val loops = getLoops(m)
        var forest = Set(LoopTree(loops.head))
        for {
          loop <- loops.tail
        } {
          val parents = forest.filter { (tree: LoopTree) =>
            isParent(tree.loop, loop)
          }
          if (parents.isEmpty) {
            val children = forest.filter { (tree: LoopTree) =>
              isParent(loop, tree.loop)
            }
            // This is correct even if children is empty
            val tree = LoopTree(loop, children)
            forest = (forest -- children) + tree
          } else {
            assert(parents.size <= 1, "multiple parents")
            val parent = parents.head
            forest = (forest - parent) + parent.insert(loop)
          }
        }
        loopForests = loopForests + (m -> forest)
        forest
      case Some(forest) => forest
    }
  }

  private var graphed = Set.empty[String]
  def graph(m: SootMethod, src: SootMethod, cg: CallGraph, allLoops: Boolean):
      Unit = {
    if (!graphed.contains(m.getSubSignature)) {
      graphed = graphed + m.getSubSignature
      val iterator = cg.edgesOutOf(m)
      while (iterator.hasNext) {
        val edge = iterator.next
        val sootStmt = edge.srcStmt
        val stmt = Stmt(sootStmt, m)
        val dest = Coverage2.freshenMethod(edge.tgt)
        val depth = getLoopDepth(stmt)
        if (depth > 0) {
          val forest = getLoopForest(m)
          if (allLoops) {
            emitLoopForest(Taint.fqn(src), forest, sootStmt, Taint.fqn(dest))
          } else {
            emitLineage(Taint.fqn(src), forest, sootStmt, Taint.fqn(dest))
          }
          graph(dest, dest, cg, allLoops)
        } else {
          graph(dest, src, cg, allLoops)
        }
      }
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

    Console.withOut(graphStream) {
      println("digraph loops {")
      println("ranksep=\"10\";");
      graph(m, m, cg, allLoops)
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
