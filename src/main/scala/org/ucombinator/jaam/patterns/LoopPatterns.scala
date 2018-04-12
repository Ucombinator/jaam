package org.ucombinator.jaam.patterns

import org.ucombinator.jaam.patterns.stmt._
import org.ucombinator.jaam.util.{Soot, Stmt}
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}
import soot.{Type => SootType}

import scala.collection.JavaConverters._

object LoopPatterns {
  def findLoops(stmts: List[Stmt]): Unit = {
    runRule(iteratorInvokeMatch, stmts)
  }

  def findEndingLabels(stmts: List[Stmt]): Unit = {
    runRule(wildLabel, stmts)
  }

  def findAddInvokes(stmts: List[Stmt]): Unit = {
    runRule(addInvokes, stmts)
  }

  private def runRule(rule: RegExp, stmts: List[Stmt]): Unit = {
    val states = deriveAll(rule, State(Map(), Map()), stmts)
    println()
    println("STATES: " + states)
    println()
  }

  abstract case class LoopPattern() extends (SootLoop => LoopPattern)

  private val wildcard = mkPatRegExp(AnyLabelPattern, AnyStmtPattern)
  private val wildcardRep = Rep(wildcard)

  private val arrayListIteratorMethod = getMethod("java.lang.Iterable", "iterator")
  private val arrayListAddMethod = getMethod("java.util.ArrayList", "add", Some(List(Soot.getSootType("java.lang.Object"))))

  // TODO: This isn't detecting the iterator() virtualinvoke for some reason.
  private val iteratorInvoke = mkPatRegExp(
    NamedLabelPattern("iteratorInvoke"),
    AssignStmtPattern(
      VariableExpPattern("assignee"),
      InstanceInvokeExpPattern(
        VariableExpPattern("base"),
        OverriddenMethodPattern(arrayListIteratorMethod),
        ListArgPattern(List())
      )
    )
  )

  private val addInvoke = mkPatRegExp(
    NamedLabelPattern("addInvoke"),
    AssignStmtPattern(
      UnusedAssignDestExpPattern,
      InstanceInvokeExpPattern(
        VariableExpPattern("base"),
        ConstantMethodPattern(arrayListAddMethod),
        ListArgPattern(List(AnyExpPattern))
      )
    )
  )

  private val getLabel = mkPatRegExp(
    NamedLabelPattern("getLabel"),
    AnyStmtPattern
  )

  private val iteratorInvokeMatch = Cat(List(wildcardRep, iteratorInvoke, wildcardRep))
  //  private val iteratorLoop = Cat(List(wildcardRep, iteratorInvoke, wildcardRep))
  private val addInvokes = Cat(List(wildcardRep, addInvoke, wildcardRep))
  private val wildLabel = Cat(List(wildcardRep, getLabel))

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
