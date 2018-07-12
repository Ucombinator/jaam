package org.ucombinator.jaam.tools.regex_driver

import org.ucombinator.jaam.serializer.Serializer
import org.ucombinator.jaam.tools.app.{App, Origin}
import org.ucombinator.jaam.util.Loop.{LoopInfo, UnidentifiedLoop}
import org.ucombinator.jaam.util.{Loop, Soot, Stmt}
import soot.{PackManager, Scene, SootMethod}
import soot.options.Options
import soot.toolkits.graph.LoopNestTree
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * TODO: write documentation on exception loops
  */

object Main {

  def main(input: List[String], className: Option[String], methodName: Option[String], showStmts: Boolean, showUnidentified: Boolean): Unit = {
    prepFromInput(input)

    val loopTypeToLoop = mutable.Map[Class[_ <: LoopInfo], Set[IdentifiedLoop]]()  // Maps types of LoopInfo to identified loops
    val sootLoopToLoopInfo = mutable.LinkedHashMap[SootLoop, LoopInfo]()           // LinkedHashMap preserves insertion order

    val sootClasses = Soot.loadedClasses.keys.map(Soot.getSootClass)
    val classes = className match {
      case Some(cn) => sootClasses.filter(_.getName == className)
      case None => sootClasses
    }

    for (c <- classes) {
      if (Soot.loadedClasses(c.getName).origin == Origin.APP) {
        // Search through only concrete methods.
        for (method <- c.getMethods.asScala.filter(m => m.isConcrete && (methodName match { case None => true; case Some(mn) => m.getName == mn }))) {
          val lnt = new LoopNestTree(Soot.getBody(method))
          for (loop <- lnt.asScala.toSet[SootLoop]) {
            val info = Loop.identifyLoop(method, loop, showStmts)
            val identifiedLoop = new IdentifiedLoop(method, loop, info)

            // Update map of LoopInfo types to identified loops.
            val identifiedLoops = loopTypeToLoop.getOrElse(info.getClass, Set())
            loopTypeToLoop(info.getClass) = identifiedLoops + identifiedLoop

            // Update map of SootLoops to LoopInfos.
            sootLoopToLoopInfo(loop) = info
          }
        }
      }
    }

    val totalNumberOfLoops = loopTypeToLoop.foldLeft(0)({ case (a, (_, v)) => a + v.size })
    println("LOOP STATISTICS")
    println("Total number of loops: " + totalNumberOfLoops)
    for ((loopType, loops) <- loopTypeToLoop) {
      val percentage = loops.size.toFloat / totalNumberOfLoops.toFloat * 100.0
      println("  " + loopType.getSimpleName + ": " + loops.size + " / " + totalNumberOfLoops + " = " + f"$percentage%1.2f" + "%")
    }

    if (showUnidentified) {
      println()
      println("UNIDENTIFIED LOOPS")
      for (loop <- loopTypeToLoop(classOf[UnidentifiedLoop])) {
        println("  " + loop.sootMethod)
        if (Loop.isExceptionLoop(loop.sootLoop, loop.sootMethod)) {
          println("  EXCEPTION LOOP")
        }
        for (stmt <- loop.sootLoop.getLoopStatements.asScala.map(Stmt(_, loop.sootMethod))) {
          println("    " + stmt)
        }
      }
    }
  }

  sealed case class IdentifiedLoop(sootMethod: SootMethod, sootLoop: SootLoop, loopInfo: LoopInfo)

  def prepFromInput(input: List[String]): Unit = {
    Options.v().set_verbose(false)
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_keep_line_number(true)
    Options.v().set_allow_phantom_refs(true)
    Options.v().set_include_all(true)
    Options.v().set_prepend_classpath(false)
    Options.v().set_src_prec(Options.src_prec_only_class)

    soot.Main.v().autoSetOptions()

    Soot.useJaamClassProvider()

    val inputPackets = input.flatMap(Serializer.readAll(_).asScala)

    for (packet <- inputPackets) {
      packet match {
        case p: App => Soot.addClasses(p)
        case _ => ()
      }
    }

    Scene.v.loadBasicClasses()
    PackManager.v.runPacks()
  }
}
