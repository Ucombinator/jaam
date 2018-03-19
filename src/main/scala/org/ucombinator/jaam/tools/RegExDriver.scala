package org.ucombinator.jaam.tools.regex_driver

import org.ucombinator.jaam.serializer.Serializer
import org.ucombinator.jaam.tools.app.{App, Origin}
import org.ucombinator.jaam.util.stmtPattern._
import org.ucombinator.jaam.util.{Soot, Stmt, StmtPatternToRegEx}
import org.ucombinator.jaam.util.stmtPattern.regEx._
import soot.{PackManager, Scene}
import soot.options.Options

import scala.collection.JavaConverters._

/**
  * TODO: write documentation on exception loops
  */

object Main {
  def main(input: List[String], className: String, methodName: String): Unit = {
    prepFromInput(input)

    val classNames = Soot.loadedClasses.keys
    val classes = classNames.map(Soot.getSootClass).filter(_.getName == className)
    for (c <- classes) {
      if (Soot.loadedClasses(c.getName).origin == Origin.APP) {
        // Search through only concrete methods.
        for (method <- c.getMethods.asScala.filter(m => m.isConcrete && m.getName == methodName)) {
          print(Soot.getBody(method))

          val units = Soot.getBody(method).getUnits.asScala.toList
          val stmts = units.map(u => Stmt(Soot.unitToStmt(u), method))

          val wildcard = Fun(StmtPatternToRegEx(LabeledStmtPattern(AnyLabelPattern, AnyStmtPattern)), _ => List())
          val baseRule = Rep(wildcard)

          val labelGrabber = Fun(StmtPatternToRegEx(LabeledStmtPattern(NamedLabelPattern("weDontCare"), AnyStmtPattern)), _ => List())
          val labelRule = Cat(List(baseRule, labelGrabber, baseRule))

          val rules = List(baseRule, labelRule)

          for (rule <- rules) {
            println("RULE: " + rule)
            for (inputs <- List(List(), List(stmts.head), stmts)) {
              val states = deriveAll(rule, State(Map(), Map()), inputs)
              println()
              println("STATES: " + states)
              println()
            }
            println("--------------------")
          }
        }
      }
    }
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
