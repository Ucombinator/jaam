package org.ucombinator.jaam.util

import org.ucombinator.jaam.patterns.stmt._
import org.ucombinator.jaam.patterns.{LoopPatterns, State}
import soot.{Local, SootMethod, Value}
import soot.jimple.{Stmt => SootStmt, _}
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}
import soot.toolkits.graph.LoopNestTree

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

import org.ucombinator.jaam.util.Loop.BranchIdentifiedLoop

import scala.collection.mutable.ListBuffer

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
    val endIndex = Stmt.getIndex(loop.getBackJumpStmt, method)

    val initialState = State(Map("head" -> headIndex), Map())

    def runPattern(loopPattern: RegExp): Option[State] = {
      deriveAll(loopPattern, initialState, stmts, endAnywhere = false) match {
        case List(s) => Some(s)
        case List() => None
        case _ => println("multiple matches"); None
      }
    }

    runPattern(LoopPatterns.iteratorLoop).foreach(s => return IteratorLoop(s.values("arr")))
    runPattern(LoopPatterns.arrayForEachLoop).foreach(s => return ArrayForEachLoop(s.values("arr")))
    runPattern(LoopPatterns.arrayForLoop).foreach(s => return ArrayForLoop(s.values("arr")))
    runPattern(LoopPatterns.simpleCountUpForLoop).foreach(s => return SimpleCountUpForLoop(s.values("lowerBound"), s.values("upperBound"), s.values("incr")))
    runPattern(LoopPatterns.simpleCountDownForLoop).foreach(s => return SimpleCountDownForLoop(s.values("upperBound"), s.values("lowerBound"), s.values("incr")))
    runPattern(LoopPatterns.countUpForLoopWithSizedBound).foreach(s => return CountUpForLoopWithSizedBound(s.values("lowerBound"), s.values("upperBound"), s.values("incr")))
    runPattern(LoopPatterns.countUpForLoopWithSizedBoundAndOffset).foreach(s => return CountUpForLoopWithSizedBoundAndOffset(s.values("lowerBound"), s.values("upperBound"), s.values("incr"), s.values("offset")))
    runPattern(LoopPatterns.characterForLoop).foreach(s => return CharacterForLoop(s.values("lowerBound"), s.values("upperBound"), s.values("offset")))
    runPattern(LoopPatterns.simpleWhileLoop).foreach(s => return SimpleWhileLoop(s.values("iter")))
    runPattern(LoopPatterns.countUpForLoopWithVarBounds).foreach(s => return CountUpForLoopWithVarBounds(s.values("lowerBound"), s.values("upperBound"), s.values("incr")))
    runPattern(LoopPatterns.countDownForLoopWithVarBounds).foreach(s => return CountDownForLoopWithVarBounds(s.values("upperBound"), s.values("lowerBound"), s.values("incr")))
    runPattern(LoopPatterns.randomizedWhileLoop).foreach(s => return RandomizedWhileLoop(s.values("iter")))
    runPattern(LoopPatterns.doWhileLoop).foreach(s => return DoWhileLoop(s.values("iter")))

    identifyLoopBranches(method, loop, stmts, headIndex, endIndex) match {
      case Some(branches) => {
        val sootValues: ListBuffer[Value] = ListBuffer[Value]()
        val indexTags: ListBuffer[String] = ListBuffer[String]()
        val sortedBranches = branches.sortWith(getIndexTag(_) < getIndexTag(_))

        for (branch <- sortedBranches) {
          sootValues += branch.getCondition
          indexTags += getIndexTag(branch)
        }
        return BranchIdentifiedLoop(sootValues.toList.asJava, indexTags.toList)
      }
      case _ => None
    }
    UnidentifiedLoop()
  }

  def identifyLoopBranches(method: SootMethod, loop: SootLoop, stmts: List[Stmt], headIndex: Int, endIndex: Int): Option[List[IfStmt]] = {
    val graph: ControlFlowGraph = createControlFlowGraph(method, loop, stmts, headIndex, endIndex)
    graph.createBooleanExprFromGraph()
//    graph.renderGraphToFile("./controlFlow_" + method.getName + "_" + headIndex + ".gv")
    val vitalBranches: List[IfStmt] = graph.findVitalBranches()
    if (vitalBranches.nonEmpty) {
      Some(vitalBranches)
    } else {
      None
    }
  }

  def getIndexTag(stmt: soot.Unit): String = {
    BigInt(stmt.getTag(Stmt.indexTag).getValue).toString()
  }

  def createControlFlowGraph(method: SootMethod, loop: SootLoop, stmts: List[Stmt], headIndex: Int, endIndex: Int): ControlFlowGraph = {
    val graph: ControlFlowGraph = new ControlFlowGraph()
    var prevIndexTag: String = null
    val loopStmts = stmts.slice(headIndex, endIndex + 1)
    val backJumpStmtIndex = getIndexTag(loop.getBackJumpStmt)

    for (loopStmt: Stmt <- loopStmts) {

      val indexTag: String = getIndexTag(loopStmt.sootStmt)
      var isBackJmpStmt: Boolean = indexTag == backJumpStmtIndex

      loopStmt.sootStmt match {

        case stmt: IfStmt => {
          //add node to graph

          //if there are no exit vertices in the graph and this is the last stmt of the loop, mark this as exit
          if (isBackJmpStmt && graph.getExitVertex() == null) {
            graph.addVertex(indexTag, "", stmt, true, true)
          } else {
            graph.addVertex(indexTag, "", stmt, true)
          }

          var targetIndex: String = getIndexTag(stmt.getTarget)
          //this is a back jump stmt if it is tagged as backJmp or if this has a backwards branch
          isBackJmpStmt = indexTag == backJumpStmtIndex || targetIndex.toInt < indexTag.toInt

          val isTargetExitStmt: Boolean = Integer.valueOf(targetIndex) == endIndex + 1
          targetIndex = if (isTargetExitStmt) graph.exitLabel else targetIndex


          //add two branching edges to graph
          if (prevIndexTag != null) graph.addEdge(prevIndexTag, indexTag)
          //if back jmp statement dont add the back edge
          if (!isBackJmpStmt) {
            //add target vertex before adding target edge
            stmt.getTarget match {
              case branchStmt: IfStmt => graph.addVertex(targetIndex, "", branchStmt, true, isTargetExitStmt)
              case gotoStmt: GotoStmt => if (isTargetExitStmt) graph.addVertex(targetIndex, targetIndex, gotoStmt, false, isTargetExitStmt)
              case targetStmt: SootStmt => graph.addVertex(targetIndex, targetStmt.toString, targetStmt, false, isTargetExitStmt)
            }
            if (!stmt.getTarget.isInstanceOf[GotoStmt] || isTargetExitStmt) {
              graph.addEdge(indexTag, targetIndex, true)
            }
          }
          prevIndexTag = indexTag
        }
        case stmt: GotoStmt => {
          //goto stmt links prev statement to target
          //goto not considered as node
          var targetIndex: String = getIndexTag(stmt.getTarget)
          //this is a back jump stmt if it is tagged as backJmp or if this has a backwards branch
          isBackJmpStmt = indexTag == backJumpStmtIndex || targetIndex.toInt < indexTag.toInt

          val isTargetExitStmt: Boolean = Integer.valueOf(targetIndex) == endIndex + 1
          targetIndex = if (isTargetExitStmt) graph.exitLabel else targetIndex

          if (!isBackJmpStmt) {
            //add target vertex before adding target edge
            stmt.getTarget match {
              case branchStmt: IfStmt => graph.addVertex(targetIndex, "", branchStmt, true, isTargetExitStmt)
              case gotoStmt: GotoStmt => if (isTargetExitStmt) graph.addVertex(targetIndex, targetIndex, gotoStmt, false, isTargetExitStmt)
              case targetStmt: SootStmt => graph.addVertex(targetIndex, targetStmt.toString, targetStmt, false, isTargetExitStmt)
            }
            if (prevIndexTag != null && (!stmt.getTarget.isInstanceOf[GotoStmt] || isTargetExitStmt)) graph.addEdge(prevIndexTag, targetIndex)
          } else {
            if (graph.getExitVertex() == null) {
              graph.addVertex(indexTag, graph.exitLabel, stmt, false, true)
              if (prevIndexTag != null) graph.addEdge(prevIndexTag, indexTag)
            }
          }
          prevIndexTag = null
        }
        case stmt => {
          if (isBackJmpStmt && graph.getExitVertex() == null) {
            graph.addVertex(indexTag, stmt.toString(), stmt, false, true)
          } else {
            graph.addVertex(indexTag, stmt.toString(), stmt)
          }
          if (prevIndexTag != null) graph.addEdge(prevIndexTag, indexTag)
          prevIndexTag = indexTag
        }
      }
    }
    graph
  }

  sealed trait LoopInfo

  case class UnidentifiedLoop() extends LoopInfo

  case class IteratorLoop(iterable: Value) extends LoopInfo

  case class ArrayForEachLoop(iterable: Value) extends LoopInfo

  case class ArrayForLoop(iterable: Value) extends LoopInfo

  case class SimpleCountUpForLoop(lowerBound: Value, upperBound: Value, increment: Value) extends LoopInfo

  case class SimpleCountDownForLoop(upperBound: Value, lowerBound: Value, increment: Value) extends LoopInfo

  case class CountUpForLoopWithSizedBound(lowerBound: Value, upperBound: Value, increment: Value) extends LoopInfo

  case class CountUpForLoopWithSizedBoundAndOffset(lowerBound: Value, upperBound: Value, increment: Value, offset: Value) extends LoopInfo

  case class CharacterForLoop(lowerBound: Value, upperBound: Value, increment: Value) extends LoopInfo

  case class SimpleWhileLoop(iterable: Value) extends LoopInfo

  case class IteratorWhileLoop(iterable: Value) extends LoopInfo

  case class CountUpForLoopWithVarBounds(lowerBound: Value, upperBound: Value, increment: Value) extends LoopInfo

  case class CountDownForLoopWithVarBounds(upperBound: Value, lowerBound: Value, increment: Value) extends LoopInfo

  case class RandomizedWhileLoop(iterable: Value) extends LoopInfo

  case class DoWhileLoop(iterable: Value) extends LoopInfo

  case class BranchIdentifiedLoop(vitalBranches: java.util.List[Value], indexTags: List[String]) extends LoopInfo {
    override def toString: String = {
      var resultString: String = "BranchIdentifiedLoop\n Vital Branches:\n"
      for (i <- 0 until vitalBranches.length) {
        resultString += indexTags(i) + " : " + vitalBranches(i) + "\n"
      }
      resultString
    }
  }

}
