package org.ucombinator.jaam.util

import org.ucombinator.jaam.patterns.stmt._
import org.ucombinator.jaam.patterns.{LoopPatterns, State}
import soot.{Local, SootMethod, Value}
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

  def printStmts(stmts: List[Stmt]): Unit = {
    for (stmt <- stmts) {
      println(stmt.index + ": " + stmt.sootStmt)
    }
  }

  def identifyLoop(method: SootMethod, loop: SootLoop, showStmts: Boolean = false): LoopInfo = {
    val units = Soot.getBody(method).getUnits.asScala.toList
    val stmts = units.map(u => Stmt(Soot.unitToStmt(u), method))

    if (showStmts) {
      printStmts(stmts)
    }

    val headIndex = Stmt.getIndex(loop.getHead, method)
    val initialState = State(Map("head" -> headIndex), Map())

    def runPattern(loopPattern: RegExp): Option[State] = {
      deriveAll(loopPattern, initialState, stmts) match {
        case List(s) => Some(s)
        case List() => None
        case _ => println("multiple matches"); None
      }
    }

    runPattern(LoopPatterns.iteratorLoop).foreach(s => return IteratorLoop(s.locals("arr")))
    runPattern(LoopPatterns.arrayLoop).foreach(s => return ArrayLoop(s.locals("arr")))
    runPattern(LoopPatterns.simpleCountUpForLoop).foreach(s => return SimpleCountUpForLoop())
    runPattern(LoopPatterns.simpleCountDownForLoop).foreach(s => return SimpleCountDownForLoop())

    UnidentifiedLoop()
  }

  sealed trait LoopInfo
  case class UnidentifiedLoop() extends LoopInfo
  case class IteratorLoop(iterable: Local) extends LoopInfo
  case class ArrayLoop(iterable: Local) extends LoopInfo
  case class SimpleCountUpForLoop() extends LoopInfo
  case class SimpleCountDownForLoop() extends LoopInfo
}
