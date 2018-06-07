package org.ucombinator.jaam.patterns

import org.ucombinator.jaam.patterns.stmt._
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
    * TODO: From 2018-04-23
    *
    * LoopInfo patterns to build:
    *   - for-each over array
    *   - k = CONST; k < TOP; ++k
    *     - start with (k = 0; k < VAR; k++)
    *     - enumerate comparison operator
    *     - enumerate modification statement (++k/--k/k += 2/etc)
    *   - inventory Engagement-5 apps (produce simple statistics)
    *   - make LoopPatterns into objects/classes with more info (relevant arg names, for example)
    */

  abstract case class LoopPattern() extends (SootLoop => LoopPattern)

  private val wildcard = mkPatRegExp(None, AnyStmtPattern)
  private val wildcardRep = Rep(wildcard)

  private val arrayListIteratorMethod = getMethod("java.lang.Iterable", "iterator")
  private val iteratorHasNextMethod = getMethod("java.util.Iterator", "hasNext")
  private val iteratorNextMethod = getMethod("java.util.Iterator", "next")

  private val arrayListAddMethod = getMethod("java.util.ArrayList", "add", Some(List(Soot.getSootType("java.lang.Object"))))

  private def iteratorInvoke(dest: String, base: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
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

  private def iteratorHasNext(dest: String, base: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
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

  private def ifZeroGoto(lhs: String, destLabel: Option[String], label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      IfStmtPattern(
        EqExpPattern(
          VariableExpPattern(lhs), IntegralConstantExpPattern(0)
        ),
        mkLabel(destLabel)
      )
    )
  }

  private def ifGeGoto(lhs: String, rhs: ExpPattern, destLabel: Option[String], label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      IfStmtPattern(
        GeExpPattern(
          VariableExpPattern(lhs), rhs
        ),
        mkLabel(destLabel)
      )
    )
  }

  private def ifLeGoto(lhs: String, rhs: ExpPattern, destLabel: Option[String], label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      IfStmtPattern(
        LeExpPattern(
          VariableExpPattern(lhs), rhs
        ),
        mkLabel(destLabel)
      )
    )
  }

  private def iteratorNext(dest: String, base: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
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

  private def goto(destLabel: Option[String], label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      GotoStmtPattern(mkLabel(destLabel))
    )
  }

  private def matchLabel(label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AnyStmtPattern
    )
  }

  private def addInvoke(base: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
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

  private def assignSomeConst(varName: String, valName: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(varName),
        NamedExpPattern(valName, AnyIntegralConstantExpPattern)
      )
    )
  }

  private def assignConst(varName: String, const: Long, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(varName),
        IntegralConstantExpPattern(const)
      )
    )
  }

  private def assignZero(varName: String, label: Option[String] = None): RegExp = assignConst(varName, 0, label)

  private def addToVar(varName: String, amount: Long, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(varName),
        AddExpPattern(
          VariableExpPattern(varName),
          IntegralConstantExpPattern(amount)
        )
      )
    )
  }

  private def incrVar(varName: String, label: Option[String] = None): RegExp = addToVar(varName, 1, label)

  private def decrVar(varName: String, label: Option[String] = None): RegExp = addToVar(varName, -1, label)

  private def lengthOf(varName: String, arrName: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(varName),
        LengthExpPattern(VariableExpPattern(arrName))
      )
    )
  }

  private def getArrayElem(varName: String, arrName: String, element: String, label: Option[String] = None) : RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(varName),
        ArrayRefExpPattern(
          VariableExpPattern(arrName),
          VariableExpPattern(element)
        )
      )
    )
  }

  /*
  TODO: From 2018-05-29

  - rewrite mkPatRegExp to support optional labels (use AnyLabelPattern)
  - supply "head", "end", and "exit" labels for matching
  - enforce some sort of naming convention for labels (variables start lowercase, labels are uppercase/underscores)
    - throw error if invalid labels
  - add warning/handling for multiple matches
   */

  private def Label = Some
  val head = Label("head")
  val end = Label("end")
  val exit = Label("exit")

  // TODO: Realistically, the `wildcardRep` should be able to exclude certain variables.
  // This would allow us to filter out loops where induction variables are manipulated.
  val iteratorLoop = Cat(List(
    wildcardRep,
    iteratorInvoke("iter", "arr"),
    iteratorHasNext("hasNext", "iter", label = head),
    ifZeroGoto("hasNext", destLabel = end),
    iteratorNext("next", "iter"),
    wildcardRep,
    goto(head),
    matchLabel(end),
    wildcardRep
  ))
  val arrayLoop = Cat(List(
    wildcardRep,
    lengthOf("length", "arr"),
    assignZero("iter"),
    ifGeGoto("iter", VariableExpPattern("length"), destLabel = end, label = head),  // TODO: Is there a better way?
    getArrayElem("elem", "arr", "iter"),
    wildcardRep,
    incrVar("iter"),
    goto(head),
    matchLabel(end),
    wildcardRep
  ))
  val simpleCountUpForLoop = Cat(List(
    wildcardRep,
    assignZero("iter"),
    ifGeGoto("iter", NamedExpPattern("bound", AnyIntegralConstantExpPattern), destLabel = end, label = head),  // TODO: How to save the value?
    wildcardRep,
    incrVar("iter"),
    goto(head),
    matchLabel(end),
    wildcardRep
  ))
  val simpleCountDownForLoop = Cat(List(
    wildcardRep,
    assignSomeConst("iter", "bound"),
    ifLeGoto("iter", IntegralConstantExpPattern(0), destLabel = end, label = head),  // TODO: How to save the value?
    wildcardRep,
    decrVar("iter"),
    goto(head),
    matchLabel(end),
    wildcardRep
  ))
  private val addInvokes = Cat(List(wildcardRep, addInvoke("arr"), wildcardRep))

  private def mkLabel(label: Option[String] = None): LabelPattern = {
    label match {
      case None => AnyLabelPattern
      case Some(l) => NamedLabelPattern(l)
    }
  }

  private def mkPatRegExp(label: Option[String], stmtPattern: StmtPattern): RegExp = {
    Fun(StmtPatternToRegEx(LabeledStmtPattern(mkLabel(label), stmtPattern)), _ => List())
  }

  private def getMethod(className: String, methodName: String, arguments: Option[List[SootType]] = None) = {
    arguments match {
      case Some(args) => Soot.getSootClass(className).getMethod(methodName, args.asJava)
      case None => Soot.getSootClass(className).getMethodByName(methodName)
    }
  }
}
