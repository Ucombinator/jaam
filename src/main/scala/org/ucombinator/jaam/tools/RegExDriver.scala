package org.ucombinator.jaam.tools.regex_driver

import org.ucombinator.jaam.serializer.Serializer
import org.ucombinator.jaam.tools.app.{App, Origin}
import org.ucombinator.jaam.util.{Soot, Stmt, Loop}
import soot.{PackManager, Scene}
import soot.options.Options
import soot.toolkits.graph.LoopNestTree
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}

import scala.collection.JavaConverters._

/**
  * TODO: write documentation on exception loops
  */

object Main {
  def printStmts(stmts: List[Stmt]) = {
    for (stmt <- stmts) {
      println(stmt.index + ": " + stmt.sootStmt)
    }
  }

  def main(input: List[String], className: String, methodName: Option[String], showStmts: Boolean): Unit = {
    prepFromInput(input)

    val classNames = Soot.loadedClasses.keys
    val classes = classNames.map(Soot.getSootClass).filter(_.getName == className)
    for (c <- classes) {
      if (Soot.loadedClasses(c.getName).origin == Origin.APP) {
        // Search through only concrete methods.
        for (method <- c.getMethods.asScala.filter(m => m.isConcrete && (methodName match { case None => true; case Some(mn) => m.getName == mn}))) {
          println("Looking in method " + c.getName + "." + method.getName + ":")
          val lnt = new LoopNestTree(Soot.getBody(method))
          for (loop <- lnt.asScala.toSet[SootLoop]) {
            val info = Loop.identifyLoop(method, loop)
            println(info)
          }
        }
      }
    }
  }

//  def testFunctions(stmts: List[Stmt]) = {
//
//    val wildcard = Fun(StmtPatternToRegEx(LabeledStmtPattern(AnyLabelPattern, AnyStmtPattern)), _ => List())
//    val wildcardRule = Rep(wildcard)
//
//    val labelGrabber = Fun(StmtPatternToRegEx(LabeledStmtPattern(NamedLabelPattern("label"), AnyStmtPattern)), _ => List())
//    val labelRule = Cat(List(wildcardRule, labelGrabber, wildcardRule))
//
//    val nameGrabber = Fun(StmtPatternToRegEx(LabeledStmtPattern(AnyLabelPattern, AssignStmtPattern(VariableExpPattern("identifier"), AnyExpPattern))), _ => List())
//    val nameRule = Cat(List(wildcardRule, nameGrabber, wildcardRule, nameGrabber, wildcardRule))
//
//    val increment = Fun(StmtPatternToRegEx(LabeledStmtPattern(NamedLabelPattern("incr"), AssignStmtPattern(VariableExpPattern("dst"), AddExpPattern(VariableExpPattern("dst"), IntegralConstantExpPattern(1))))), _ => List())
//    val incrementRule = Cat(List(wildcardRule, increment, wildcardRule))
//
//    val addMethod = Soot.getSootClass("java.util.ArrayList").getMethod("add", List(Soot.getSootType("java.lang.Object")).asJava)
//    val virtualInvokeGrabber = Fun(StmtPatternToRegEx(LabeledStmtPattern(NamedLabelPattern("virtualInvoke"),
//      AssignStmtPattern(UnusedAssignDestExpPattern,
//        InstanceInvokeExpPattern(VariableExpPattern("base"),
//          ConstantMethodPattern(addMethod),
//          List(AnyExpPattern))))), _ => List())
//    val virtualInvokeRule = Cat(List(wildcardRule, virtualInvokeGrabber, wildcardRule))
//
//    val valueOfMethod = Soot.getSootClass("java.lang.Integer").getMethod("valueOf", List(Soot.getSootType("int")).asJava)
//    val staticInvokeGrabber = Fun(StmtPatternToRegEx(LabeledStmtPattern(NamedLabelPattern("staticInvoke"),
//      AssignStmtPattern(VariableExpPattern("dest"),
//        StaticInvokeExpPattern(ConstantMethodPattern(valueOfMethod), List(AnyExpPattern))))), _ => List())
//    val staticInvokeRule = Cat(List(wildcardRule, staticInvokeGrabber, wildcardRule))
//
//    val rules = List(wildcardRule, labelRule, nameRule, incrementRule, virtualInvokeRule, staticInvokeRule)
//
//    for (rule <- rules) {
//      println("RULE: " + rule)
//      for (inputs <- List(List(), List(stmts.head), stmts)) {
//        val states = deriveAll(rule, State(Map(), Map()), inputs)
//        println()
//        println("STATES: " + states)
//        println()
//      }
//      println("--------------------")
//    }
//  }

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
