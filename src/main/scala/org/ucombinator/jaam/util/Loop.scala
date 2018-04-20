package org.ucombinator.jaam.util

import soot.{SootMethod, Value}
import soot.jimple.{Stmt => SootStmt, _}
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}
import soot.toolkits.graph.LoopNestTree

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

object Loop {
  def getLoopSet(method: SootMethod, skipExceptionLoops: Boolean = true): Set[SootLoop] = {
    val lnt = new LoopNestTree(Soot.getBody(method))
    lnt.toSet[SootLoop].filterNot(skipExceptionLoops && isExceptionLoop(_, method))
  }

  def isExceptionLoop(loop: SootLoop, method: SootMethod): Boolean = {
    loop.getHead match {
      case s: DefinitionStmt => s.getRightOp.isInstanceOf[CaughtExceptionRef]
      case _ => false
    }
  }

  def getAssignees(statements: java.util.List[SootStmt]): Set[Value] = {
    // Get list of all values assigned to in a set of statements.
    statements.asScala.toSet.filter(s => s.isInstanceOf[AssignStmt]).map(s => s.asInstanceOf[AssignStmt].getLeftOp)
  }

  abstract class LoopInfo(val loop: SootLoop, val method: SootMethod) {
    val assignees: Set[Value] = Loop.getAssignees(loop.getLoopStatements)
  }
  case class UnidentifiedLoop(override val loop: SootLoop, override val method: SootMethod) extends LoopInfo(loop, method)
  case class SimpleInfiniteLoop(override val loop: SootLoop, override val method: SootMethod) extends LoopInfo(loop, method)
  case class ExitlessLoop(override val loop: SootLoop, override val method: SootMethod) extends LoopInfo(loop, method)
  case class RegularLoop(override val loop: SootLoop, override val method: SootMethod) extends LoopInfo(loop, method) {
    val cond: ConditionExpr = loop.getHead.asInstanceOf[IfStmt].getCondition.asInstanceOf[ConditionExpr]
    val op1: Value = cond.getOp1
    val op2: Value = cond.getOp2
  }
  case class IteratorLoop(override val loop: SootLoop, override val method: SootMethod) extends LoopInfo(loop, method)
  case class UnclassifiedAssignmentLoop(override val loop: SootLoop, override val method: SootMethod) extends LoopInfo(loop, method)
  case class InterfaceInvokeLoop(override val loop: SootLoop, override val method: SootMethod) extends LoopInfo(loop, method)
  case class ExceptionLoop(override val loop: SootLoop, override val method: SootMethod) extends LoopInfo(loop, method)

}
