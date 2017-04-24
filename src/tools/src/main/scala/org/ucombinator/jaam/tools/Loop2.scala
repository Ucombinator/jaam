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
  val nocolor = opt[Boolean](descr = "Turn color off")

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
    LoopAnalyzer.main(!nocolor(), mainClass(), mainMethod(), classpath(),
        outStream)
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

  private var graphed = Set.empty[SootMethod]
  def graph(m: SootMethod, src: SootMethod, cg: CallGraph): Unit = {
    if (!graphed.contains(m)) {
      graphed = graphed + m
      val iterator = cg.edgesOutOf(m)
      while (iterator.hasNext) {
        val edge = iterator.next
        val stmt = Stmt(edge.srcStmt, m)
        val dest = Coverage2.freshenMethod(edge.tgt)
        val depth = getLoopDepth(stmt)
        if (depth > 0) {
          println("  \"" + Taint.fqn(src) + "\" -> \"" + Taint.fqn(dest) +
            "\" [label=" + depth + "]")
          graph(dest, dest, cg)
        } else {
          graph(dest, src, cg)
        }
      }
    }
  }

  def main(color: Boolean, mainClass: String, mainMethod: String,
      classpath: String, graphStream: PrintStream): Unit = {
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
      graph(m, m, cg)
      println("}")
    }
  }
}

case class Stmt(val stmt: SootStmt, val m: SootMethod) {
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
