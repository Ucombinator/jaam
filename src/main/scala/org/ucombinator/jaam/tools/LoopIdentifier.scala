package org.ucombinator.jaam.tools.loopidentifier

import org.jgrapht.Graphs
import org.ucombinator.jaam.serializer.Serializer
import org.ucombinator.jaam.tools.app.App
import org.ucombinator.jaam.util.{Soot, Stmt}
import soot.{PackManager, Scene, SootClass, SootMethod}
import org.ucombinator.jaam.tools.app.{Origin, App}
import soot.jimple.{Stmt => SootStmt, _}
import soot.jimple.toolkits.annotation.logic.Loop
import soot.Value
import soot.options.Options
import soot.toolkits.graph.LoopNestTree

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
  def main(input: List[String], printBodies: Boolean, printStatements: Boolean) {
    val (c, m) = prepFromInput(input)

    // Count up all loops found.
    val loops = mutable.Map[Class[_], mutable.Set[LoopIdentity]]()

    val class_names = Soot.loadedClasses.keys
    val classes = class_names.map(Soot.getSootClass)
    for (c <- classes) {
      if (Soot.loadedClasses(c.getName).origin == Origin.APP) {
        // Search through only concrete methods.
        for (method <- c.getMethods.asScala.filter(_.isConcrete)) {
          val lnt = new LoopNestTree(Soot.getBody(method))
          if (lnt.size > 0) {
            // Print the method body if (a) it was requested and (b) this method contains at least one loop.
            if (printBodies) {
              println("Statements in method: " + method)
              method.getActiveBody.getUnits.asScala.foreach(u => println("  " + u))
            }
            // Identify all the loops in the method.
            lnt.asScala.foreach(loop => {
              val ident = identifyLoop(loop, method)
              val s = loops.getOrElse(ident.getClass, mutable.Set[LoopIdentity]())
              s.add(ident)
              loops(ident.getClass) = s
            })
          }
        }
      }
    }

    for ((cls, idents) <- loops) {
      println(cls.getSimpleName + ": " + idents.size)
      idents.foreach(ident => {
        println("  " + ident.head.sourceFile + ", line " + ident.head.line + " in " + ident.method.getName)
        println("    prehead: " + ident.prehead.sootStmt)
        println("    head:    " + ident.head.sootStmt)
        if (printStatements) {
          println("    statements:")
          ident.loop.getLoopStatements.asScala.foreach(s => println("      " + s))
        }
      })
      println()
    }
  }

  def getAssignees(statements: java.util.List[SootStmt]): Set[Value] = {
    // Get list of all values assigned to in a set of statements.
    statements.asScala.toSet.filter(s => s.isInstanceOf[AssignStmt]).map(s => s.asInstanceOf[AssignStmt].getLeftOp)
  }

  abstract class LoopIdentity(val loop: Loop, val method: SootMethod) {
    val head: Stmt = Stmt(loop.getHead, method)
    val prehead: Stmt = head.prevSyntactic
    val exits: Int = loop.getLoopExits.size()
    val assignees: Set[Value] = getAssignees(loop.getLoopStatements)
  }
  case class UnidentifiedLoop(override val loop: Loop, override val method: SootMethod) extends LoopIdentity(loop, method)
  case class SimpleInfiniteLoop(override val loop: Loop, override val method: SootMethod) extends LoopIdentity(loop, method)
  case class ExitlessLoop(override val loop: Loop, override val method: SootMethod) extends LoopIdentity(loop, method)
  case class RegularLoop(override val loop: Loop, override val method: SootMethod) extends LoopIdentity(loop, method) {
    val cond: ConditionExpr = loop.getHead.asInstanceOf[IfStmt].getCondition.asInstanceOf[ConditionExpr]
    val op1: Value = cond.getOp1
    val op2: Value = cond.getOp2
  }
  case class IteratorLoop(override val loop: Loop, override val method: SootMethod) extends LoopIdentity(loop, method)
  case class UnclassifiedAssignmentLoop(override val loop: Loop, override val method: SootMethod) extends LoopIdentity(loop, method)
  case class InterfaceInvokeLoop(override val loop: Loop, override val method: SootMethod) extends LoopIdentity(loop, method)
  case class ExceptionLoop(override val loop: Loop, override val method: SootMethod) extends LoopIdentity(loop, method)

  def identifyLoop(loop: Loop, method: SootMethod): LoopIdentity = {
    // TODO: Identify nested loops.
    if (loop.loopsForever()) {
      return SimpleInfiniteLoop(loop, method)
    }
    loop.getHead match {
      case _: IfStmt =>
        if (loop.getLoopExits.size() == 0) {
          ExitlessLoop(loop, method)
        }
        RegularLoop(loop, method)
      case s: DefinitionStmt =>
        s.getRightOp match {
          case rhs: InterfaceInvokeExpr =>
            /**
              * Assumption: if the rhs of an assignment in the first statement of a loop is an InterfaceInvokeExpr, and
              * if the class being invoked from is "java.util.Iterator", then this loop is something like a for-each
              * loop and should be handled as such.
              */
            if (rhs.getMethod.getDeclaringClass.equals(Soot.classes.Iterator)) {
              IteratorLoop(loop, method)
            } else {
              InterfaceInvokeLoop(loop, method)
            }
          case rhs: CaughtExceptionRef =>
            /**
              * Assumption: these are the not-really-a-loop loops generated by having enough variables in a block with
              * a try/catch/finally.
              */
            ExceptionLoop(loop, method)
          case _ =>
            UnclassifiedAssignmentLoop(loop, method)
        }
      case _: InterfaceInvokeExpr =>
        IteratorLoop(loop, method)
      case _ =>
        UnidentifiedLoop(loop, method)
    }
  }

  def prepFromInput(input: List[String]): (SootClass, SootMethod) = {
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

    for (a <- inputPackets) { Soot.addClasses(a.asInstanceOf[App]) }

    val mainClasses = for (a <- inputPackets) yield { a.asInstanceOf[App].main.className }
    val mainMethods = for (a <- inputPackets) yield { a.asInstanceOf[App].main.methodName }
    val mainClass = mainClasses.head.get // TODO: fix
    val mainMethod = mainMethods.head.get // TODO: fix

    Scene.v.loadBasicClasses()
    PackManager.v.runPacks()

    val c = Soot.getSootClass(mainClass)
    (c, c.getMethodByName(mainMethod))
  }
}
