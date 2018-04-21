package org.ucombinator.jaam.patterns

import org.ucombinator.jaam.patterns.stmt._
import org.ucombinator.jaam.util.Loop.LoopInfo
import org.ucombinator.jaam.util.{Soot, Stmt}
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}
import soot.{SootMethod, Type => SootType}

import scala.collection.JavaConverters._

object LoopPatterns {
  def findLoops(stmts: List[Stmt]): Unit = {
    runRule(iteratorLoop, stmts)
  }

  def findAddInvokes(stmts: List[Stmt]): Unit = {
    runRule(addInvokes, stmts)
  }

  private def runRule(rule: RegExp, stmts: List[Stmt]): Unit = {
    val states = deriveAll(rule, State(Map(), Map()), stmts)
    println("  " + rule)
    println("  STATES: " + states)
    println()
  }

  /**
    * TODO: From 2018-04-12
    *
    * Write method:
    *   - takes SootMethod and SootLoop
    *   - grab statements from method
    *   - identify loop header/exit with labels
    *   - attempt to identify the loop contained within the method matching those labels
    *   - produce LoopInfo object
    *
    * Produce more loop-matching patterns
    */


  /**
    * TODO: From 2018-04-20
    *
    * Modify every creation of LoopNode in Loop3 to take a LoopInfo.
    * Modify serializer.LoopLoopNode to take a LoopInfo.
    * Pass LoopInfo to LoopLoopNode creation in Loop3.toJaam.
    * Serialization is automagic at this point.
    *
    * -> Don't use any Collection classes in LoopInfo object because serialization problems.
    */

  def makeLoopInfo(method: SootMethod, loop: SootLoop): Unit = {
    val units = Soot.getBody(method).getUnits.asScala.toList
    val stmts = units.map(u => Stmt(Soot.unitToStmt(u), method))
    for (loopPattern <- List(iteratorLoop)) {
      val headIndex = Stmt.getIndex(loop.getHead, method)
      val initialState = State(Map("iteratorHasNext" -> headIndex), Map())
      val states = deriveAll(loopPattern, initialState, stmts)
      states match {
        case List(s) =>
          val x = s.locals.get("arr")
        case _ => ()
      }
      println("  STATES: " + states)
    }
  }

  abstract case class LoopPattern() extends (SootLoop => LoopPattern)

  private val wildcard = mkPatRegExp(AnyLabelPattern, AnyStmtPattern)
  private val wildcardRep = Rep(wildcard)

  private val arrayListIteratorMethod = getMethod("java.lang.Iterable", "iterator")
  private val iteratorHasNextMethod = getMethod("java.util.Iterator", "hasNext")
  private val iteratorNextMethod = getMethod("java.util.Iterator", "next")

  private val arrayListAddMethod = getMethod("java.util.ArrayList", "add", Some(List(Soot.getSootType("java.lang.Object"))))

  private def iteratorInvoke(label: String, dest: String, base: String): RegExp = {
    mkPatRegExp(
      NamedLabelPattern(label),
      AssignStmtPattern(
        VariableExpPattern(dest),
        InstanceInvokeExpPattern(
          VariableExpPattern(base),
          OverriddenMethodPattern(arrayListIteratorMethod),
          ListArgPattern(List())
        )
      )
    )
  }

  private def iteratorHasNext(label: String, dest: String, base: String): RegExp = {
    mkPatRegExp(
      NamedLabelPattern(label),
      AssignStmtPattern(
        VariableExpPattern(dest),
        InstanceInvokeExpPattern(
          VariableExpPattern(base),
          OverriddenMethodPattern(iteratorHasNextMethod),
          ListArgPattern(List())
        )
      )
    )
  }

  private def ifZeroGoto(label: String, lhs: String, destLabel: String): RegExp = {
    mkPatRegExp(
      NamedLabelPattern(label),
      IfStmtPattern(
        EqualsExpPattern(
          VariableExpPattern(lhs), IntegralConstantExpPattern(0)
        ),
        NamedLabelPattern(destLabel)
      )
    )
  }

  private def iteratorNext(label: String, dest: String, base: String): RegExp = {
    mkPatRegExp(
      NamedLabelPattern(label),
      AssignStmtPattern(
        VariableExpPattern(dest),
        InstanceInvokeExpPattern(
          VariableExpPattern(base),
          OverriddenMethodPattern(iteratorNextMethod),
          ListArgPattern(List())
        )
      )
    )
  }

  private def goto(label: String, destLabel: String): RegExp = {
    mkPatRegExp(
      NamedLabelPattern(label),
      GotoStmtPattern(NamedLabelPattern(destLabel))
    )
  }

  private def matchLabel(label: String): RegExp = {
    mkPatRegExp(
      NamedLabelPattern(label),
      AnyStmtPattern
    )
  }

  private def addInvoke(label: String, base: String): RegExp = {
    mkPatRegExp(
      NamedLabelPattern(label),
      AssignStmtPattern(
        UnusedAssignDestExpPattern,
        InstanceInvokeExpPattern(
          VariableExpPattern(base),
          ConstantMethodPattern(arrayListAddMethod),
          ListArgPattern(List(AnyExpPattern))
        )
      )
    )
  }

  // TODO: Realistically, the `wildcardRep` should be able to exclude certain variables.
  // This would allow us to filter out loops where induction variables are manipulated.
  val iteratorLoop = Cat(List(
    wildcardRep,
    iteratorInvoke("iteratorInvoke", "iter", "arr"),
    iteratorHasNext("iteratorHasNext", "hasNext", "iter"),
    ifZeroGoto("test", "hasNext", "loopEnd"),
    iteratorNext("iteratorNext", "next", "iter"),
    wildcardRep,
    goto("goto", "iteratorHasNext"),
    matchLabel("loopEnd"),
    wildcardRep
  ))
  private val addInvokes = Cat(List(wildcardRep, addInvoke("addInvoke", "arr"), wildcardRep))

  private def mkPatRegExp(labelPattern: LabelPattern, stmtPattern: StmtPattern): RegExp = {
    Fun(StmtPatternToRegEx(LabeledStmtPattern(labelPattern, stmtPattern)), _ => List())
  }

  private def getMethod(className: String, methodName: String, arguments: Option[List[SootType]] = None) = {
    arguments match {
      case Some(args) => Soot.getSootClass(className).getMethod(methodName, args.asJava)
      case None => Soot.getSootClass(className).getMethodByName(methodName)
    }
  }
}
