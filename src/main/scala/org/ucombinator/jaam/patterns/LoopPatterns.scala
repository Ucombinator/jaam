package org.ucombinator.jaam.patterns

import org.ucombinator.jaam.patterns.LoopPatterns.ifGeGoto
import org.ucombinator.jaam.patterns.stmt._
import org.ucombinator.jaam.util.{Soot, Stmt}
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}
import soot.{SootMethod, Type => SootType}

import scala.collection.JavaConverters._

object LoopPatterns {

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

  private def wildcardRepExclude(exclude: RegExp): RegExp = Rep(Cat(List(Not(exclude), wildcard)))

  private val arrayListIteratorMethod = getMethod("java.lang.Iterable", "iterator")
  private val iteratorHasNextMethod = getMethod("java.util.Iterator", "hasNext")
  private val iteratorNextMethod = getMethod("java.util.Iterator", "next")

  private val arrayListAddMethod = getMethod("java.util.ArrayList", "add", Some(List(Soot.getSootType("java.lang.Object"))))
  private val vectorSizeMethod = getMethod("java.util.Vector", "size")
  private val vectorElementAtMethod = getMethod("java.util.Vector", "elementAt", Some(List(Soot.getSootType("int"))))
  private val stringLengthMethod = getMethod(className = "java.lang.String", "length")
  private val arrayListSizeMethod = getMethod("java.util.ArrayList", "size")
  private val listSizeMethod = getMethod("java.util.List", "size")
  private val bigIntBitLengthMethod = getMethod("java.math.BigInteger", "bitLength", Some(List()))
  private val randomMethod = getMethod("java.lang.Math", "random")

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

  private def ifNotZeroGoto(lhs: String, destLabel: Option[String], label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      IfStmtPattern(
        NotEqExpPattern(
          VariableExpPattern(lhs), IntegralConstantExpPattern(0)
        ),
        mkLabel(destLabel)
      )
    )
  }

  private def ifEqValueGoto(lhs: String, rhs: ExpPattern, destLabel: Option[String], label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      IfStmtPattern(
        EqExpPattern(
          VariableExpPattern(lhs), rhs
        ),
        mkLabel(destLabel)
      )
    )
  }

  private def ifNotEqValueGoto(lhs: String, rhs: ExpPattern, destLabel: Option[String], label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      IfStmtPattern(
        NotEqExpPattern(
          VariableExpPattern(lhs), rhs
        ),
        mkLabel(destLabel)
      )
    )
  }

  private def ifGtGoto(lhs: String, rhs: ExpPattern, destLabel: Option[String], label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      IfStmtPattern(
        GtExpPattern(
          VariableExpPattern(lhs), rhs
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

  private def ifGtOrGeGoto(lhs: String, rhs: ExpPattern, destLabel: Option[String], label: Option[String] = None): RegExp = {
    Alt(List(
      ifGtGoto(lhs, rhs, destLabel, label),
      ifGeGoto(lhs, rhs, destLabel, label)
    ))
  }

  private def ifLtGoto(lhs: String, rhs: ExpPattern, destLabel: Option[String], label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      IfStmtPattern(
        LtExpPattern(
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

  private def ifLtOrLeGoto(lhs: String, rhs: ExpPattern, destLabel: Option[String], label: Option[String] = None): RegExp = {
    Alt(List(
      ifLtGoto(lhs, rhs, destLabel, label),
      ifLeGoto(lhs, rhs, destLabel, label)
    ))
  }

  private def anyIf(destLabel: Option[String], label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      IfStmtPattern(
        AnyExpPattern,
        mkLabel(destLabel)
      )
    )
  }

  private def cmpg(dest: String, lhs: ExpPattern, rhs: ExpPattern, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(dest),
        CmpgExpPattern(lhs, rhs)
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

  private def assignSomeValue(varName: String, valName: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(varName),
        NamedExpPattern(valName, AnyExpPattern)
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

  private def anyAddToVar(varName: String, amountName: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(varName),
        AddExpPattern(
          VariableExpPattern(varName),
          NamedExpPattern(amountName, AnyExpPattern)
        )
      )
    )
  }

  private def lengthOf(varName: String, arrName: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(varName),
        LengthExpPattern(VariableExpPattern(arrName))
      )
    )
  }

  private def getArrayElem(varName: String, arrName: String, element: String, label: Option[String] = None): RegExp = {
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

  private def vectorSize(dest: String, base: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(dest),
        InstanceInvokeExpPattern(
          VariableExpPattern(base),
          ConstantMethodPattern(vectorSizeMethod),
          ListArgPattern(List())
        )
      )
    )
  }

  private def vectorElementAt(varName: String, arrName: String, element: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(varName),
        InstanceInvokeExpPattern(
          VariableExpPattern(arrName),
          ConstantMethodPattern(vectorElementAtMethod),
          ListArgPattern(List(AnyExpPattern))
        )
      )
    )
  }

  private def stringLength(dest: String, base: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(dest),
        InstanceInvokeExpPattern(
          VariableExpPattern(base),
          ConstantMethodPattern(stringLengthMethod),
          ListArgPattern(List())
        )
      )
    )
  }

  private def arrayListSize(dest: String, base: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(dest),
        InstanceInvokeExpPattern(
          VariableExpPattern(base),
          ConstantMethodPattern(arrayListSizeMethod),
          ListArgPattern(List())
        )
      )
    )
  }

  private def listSize(dest: String, base: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(dest),
        InstanceInvokeExpPattern(
          VariableExpPattern(base),
          ConstantMethodPattern(listSizeMethod),
          ListArgPattern(List())
        )
      )
    )
  }

  private def random(dest: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(dest),
        StaticInvokeExpPattern(
          ConstantMethodPattern(randomMethod),
          ListArgPattern(List())
        )
      )
    )
  }

  private def bigIntBitLength(dest: String, base: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(dest),
        InstanceInvokeExpPattern(
          VariableExpPattern(base),
          OverriddenMethodPattern(bigIntBitLengthMethod),
          ListArgPattern(List())
        )
      )
    )
  }

  private def getAnyDynamicUpperBound(dest: String, base: String, label: Option[String] = None): RegExp = {
    Alt(List(
      vectorSize(dest, base, label),
      stringLength(dest, base, label),
      arrayListSize(dest, base, label),
      listSize(dest, base, label),
      bigIntBitLength(dest, base, label)
    ))
  }

  private def addOffsetAndAssignToNewVar(newVarName: String, varName: String, offsetName: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(newVarName),
        AddExpPattern(
          VariableExpPattern(varName),
          NamedExpPattern(offsetName, AnyExpPattern)
        )
      )
    )
  }

  private def subOffsetAndAssignToNewVar(newVarName: String, varName: String, offsetName: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(newVarName),
        SubExpPattern(
          VariableExpPattern(varName),
          NamedExpPattern(offsetName, AnyExpPattern)
        )
      )
    )
  }

  private def offsetAndAssignToNewVar(newVarName: String, varName: String, offsetName: String, label: Option[String] = None): RegExp = {
    Alt(List(
      addOffsetAndAssignToNewVar(newVarName, varName, offsetName, label),
      subOffsetAndAssignToNewVar(newVarName, varName, offsetName, label)
    ))
  }

  private def castAndAssignToVar(varName: String, valName: String, castType: SootType, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(varName),
        CastExpPattern(
          castType,
          VariableExpPattern(valName)
        )
      )
    )
  }

  private def invokeAny(dest: String, base: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(dest),
        InstanceInvokeExpPattern(
          VariableExpPattern(base),
          AnyMethodPattern,
          ListArgPattern(List())
        )
      )
    )
  }

  private def negate(varName: String, valName: String, label: Option[String] = None): RegExp = {
    mkPatRegExp(
      label,
      AssignStmtPattern(
        VariableExpPattern(varName),
        NegExpPattern(VariableExpPattern(valName))
      )
    )
  }

  private def anyRelationalIf(lhs: String, rhs: ExpPattern, destLabel: Option[String], label: Option[String] = None): RegExp = {
    Alt(
      List(
        ifLtOrLeGoto(lhs, rhs, destLabel, label),
        ifGtOrGeGoto(lhs, rhs, destLabel, label),
        //        ifEqValueGoto(lhs,rhs,destLabel,label),
        //        ifNotEqValueGoto(lhs,rhs,destLabel,label)
        ifZeroGoto(lhs, destLabel),
        ifNotZeroGoto(lhs, destLabel)
      ))
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
  val arrayForEachLoop = Cat(List(
    wildcardRep,
    Alt(List(lengthOf("length", "arr"), assignZero("iter"))),
    Alt(List(assignZero("iter"), lengthOf("length", "arr"))),
    ifGeGoto("iter", VariableExpPattern("length"), destLabel = end, label = head),
    getArrayElem("elem", "arr", "iter"),
    wildcardRep,
    incrVar("iter"),
    goto(head),
    matchLabel(end),
    wildcardRep
  ))
  val arrayForLoop = Cat(List(
    wildcardRep,
    assignZero("iter"),
    lengthOf("length", "arr", label = head),
    ifGeGoto("iter", VariableExpPattern("length"), destLabel = end),
    wildcardRep,
    getArrayElem("elem", "arr", "iter"),
    wildcardRep,
    incrVar("iter"),
    goto(head),
    matchLabel(end),
    wildcardRep
  ))
  val simpleCountUpForLoop = Cat(List(
    wildcardRep,
    assignSomeValue("iter", "lowerBound"),
    ifGtOrGeGoto("iter", NamedExpPattern("upperBound", AnyExpPattern), destLabel = end, label = head),
    wildcardRepExclude(Alt(List(anyIf(end), goto(end)))),
    anyAddToVar("iter", "incr"),
    goto(head),
    matchLabel(end),
    wildcardRep
  ))
  val simpleCountDownForLoop = Cat(List(
    wildcardRep,
    assignSomeValue("iter", "upperBound"),
    ifLtOrLeGoto("iter", NamedExpPattern("lowerBound", AnyExpPattern), destLabel = end, label = head),
    wildcardRepExclude(Alt(List(anyIf(end), goto(end)))),
    anyAddToVar("iter", "incr"),
    goto(head),
    matchLabel(end),
    wildcardRep
  ))

  val countUpForLoopWithSizedBound = Cat(List(
    wildcardRep,
    assignSomeValue("iter", "lowerBound"),
    getAnyDynamicUpperBound("upperBound", "dataStruct", label = head),
    ifGtOrGeGoto("iter", VariableExpPattern("upperBound"), destLabel = end),
    wildcardRep,
    anyAddToVar("iter", "incr"),
    goto(head),
    matchLabel(end),
    wildcardRep
  ))

  val countUpForLoopWithSizedBoundAndOffset = Cat(List(
    wildcardRep,
    assignSomeValue("iter", "lowerBound"),
    getAnyDynamicUpperBound("initalUpperBound", "dataStruct", label = head),
    offsetAndAssignToNewVar("upperBound", "initalUpperBound", "offset"),
    ifGtOrGeGoto("iter", VariableExpPattern("upperBound"), destLabel = end),
    wildcardRep,
    anyAddToVar("iter", "incr"),
    goto(head),
    matchLabel(end),
    wildcardRep
  ))

  val characterForLoop = Cat(List(
    wildcardRep,
    assignSomeValue("iter", "lowerBound"),
    ifGtOrGeGoto("iter", NamedExpPattern("upperBound", AnyExpPattern), destLabel = end, label = head),
    wildcardRepExclude(Alt(List(anyIf(end), goto(end)))),
    addOffsetAndAssignToNewVar("tempVar", "iter", "offset"),
    castAndAssignToVar("iter", "tempVar", Soot.getSootType("char")),
    goto(head),
    matchLabel(end),
    wildcardRep
  ))

  val countUpForLoopWithVarBounds = Cat(List(
    wildcardRep,
    assignSomeValue("iter", "lowerBound"),
    assignSomeValue("limit", "upperBound", label = head),
    ifGtOrGeGoto("iter", VariableExpPattern("limit"), destLabel = end),
    wildcardRepExclude(Alt(List(anyIf(end), goto(end)))),
    anyAddToVar("iter", "incr"),
    goto(head),
    matchLabel(end),
    wildcardRep
  ))

  val countDownForLoopWithVarBounds = Cat(List(
    wildcardRep,
    assignSomeValue("iter", "lowerBound"),
    assignSomeValue("limit", "upperBound", label = head),
    ifLtOrLeGoto("iter", VariableExpPattern("limit"), destLabel = end),
    wildcardRepExclude(Alt(List(anyIf(end), goto(end)))),
    anyAddToVar("iter", "incr"),
    goto(head),
    matchLabel(end),
    wildcardRep
  ))

  val simpleWhileLoop = Cat(List(
    wildcardRep,
    assignSomeValue("iter", "initValue"),
    wildcardRep,
    anyRelationalIf("iter", NamedExpPattern("bound", AnyExpPattern), destLabel = end, label = head),
    wildcardRep,
    anyAddToVar("iter", "incr"),
    wildcardRep,
    goto(head),
    matchLabel(end),
    wildcardRep
  ))

  val randomizedWhileLoop = Cat(List(
    wildcardRep,
    assignSomeValue("limit", "upperBound"),
    anyRelationalIf("iter", VariableExpPattern("limit"), destLabel = head),
    random("randomValue"),
    cmpg("randomCond", VariableExpPattern("randomValue"), NamedExpPattern("bound", AnyExpPattern)),
    ifGtOrGeGoto("randomCond", IntegralConstantExpPattern(0), destLabel = head),
    matchLabel(end),
    wildcardRep
  ))

  val doWhileLoop = Cat(List(
    wildcardRep,
    assignSomeValue("iter", "lowerBound"),
    wildcardRep,
    matchLabel(head),
    wildcardRep,
    anyAddToVar("iter", "incr"),
    wildcardRep,
    anyRelationalIf("iter", NamedExpPattern("limit", AnyExpPattern), destLabel = head),
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
    def derive(state: State, remaining: List[Stmt]): (List[State], List[(RegExp, State)]) = {
      remaining match {
        case List() => (List(), List())
        case a :: _ => StmtPatternToRegEx(LabeledStmtPattern(mkLabel(label), stmtPattern))(state, a)
      }
    }

    Fun(derive)
  }

  private def getMethod(className: String, methodName: String, arguments: Option[List[SootType]] = None) = {
    arguments match {
      case Some(args) => Soot.getSootClass(className).getMethod(methodName, args.asJava)
      case None => Soot.getSootClass(className).getMethodByName(methodName)
    }
  }
}
