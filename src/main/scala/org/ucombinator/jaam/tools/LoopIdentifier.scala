package org.ucombinator.jaam.tools.loopidentifier

import org.ucombinator.jaam.serializer.Serializer
import org.ucombinator.jaam.tools.app.Origin
import org.ucombinator.jaam.tools.app.App
import org.ucombinator.jaam.util.{Soot, Loop}

import soot.{PackManager, Scene}
import soot.options.Options

import scala.collection.JavaConverters._
import scala.collection.mutable

/*
  Goal: identify different types of loops being used. Specifically:
    - constant literal loops
    - determined loops (constants via variables)
    - variable loops
  This must be done over both regular for loops as well as for-each loops.

  TODO:
    [x] - search all methods (not just given specific "main" method)
    [ ] - identify further loop patterns in Jimple
 */

object Main {
  def main(input: List[String], printBodies: Boolean, printStatements: Boolean, skipExceptionLoops: Boolean) {
    // Set everything up for analysis.
    prepFromInput(input)

    // Count up all loops found.
    val loops = mutable.Map[Class[_], mutable.Set[Loop.LoopInfo]]()

    val class_names = Soot.loadedClasses.keys
    val classes = class_names.map(Soot.getSootClass)
    for (c <- classes) {
      if (Soot.loadedClasses(c.getName).origin == Origin.APP) {
        // Search through only concrete methods.
        for (method <- c.getMethods.asScala.filter(_.isConcrete)) {
          print(Soot.getBody(method))
//          val infoSet = Loop.getLoopInfoSet(method, skipExceptionLoops)
//          if (infoSet.nonEmpty && printBodies) {
//            println("Statements in method: " + method)
//            method.getActiveBody.getUnits.asScala.foreach(u => println("  " + u))
//          }
//          for (info <- infoSet) {
//            val s = loops.getOrElse(info.getClass, mutable.Set[Loop.LoopInfo]())
//            s.add(info)
//            loops(info.getClass) = s
//          }
        }
      }
    }

//    for ((cls, idents) <- loops) {
//      println(cls.getSimpleName + ": " + idents.size)
//      idents.foreach(ident => {
//        println("  " + ident.head.sourceFile + ", line " + ident.head.line + " in " + ident.method.getName)
//        println("    prehead: " + ident.prehead.sootStmt)
//        println("    head:    " + ident.head.sootStmt)
//        if (printStatements) {
//          println("    statements:")
//          ident.loop.getLoopStatements.asScala.foreach(s => println("      " + s))
//        }
//      })
//      println()
//    }
  }

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

    inputPackets.foreach(a => Soot.addClasses(a.asInstanceOf[App]))

    Scene.v.loadBasicClasses()
    PackManager.v.runPacks()
  }
}
