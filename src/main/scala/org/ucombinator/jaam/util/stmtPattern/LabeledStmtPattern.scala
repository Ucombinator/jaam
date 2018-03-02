package org.ucombinator.jaam.util.stmtPattern

import org.ucombinator.jaam.util.stmtPattern.regEx._
import org.ucombinator.jaam.util.Stmt
import soot.SootMethod
import soot.jimple._

// TODO: State will have two maps: Strings -> Indexes (Ints), Strings -> Identifier (local var; the thing that a VariableExpPattern matches)


case class State()

case class LabeledStmtPattern(label: LabelPattern, stmtPattern: StmtPattern)

sealed trait StmtPattern extends ((State, Stmt) => (List[State], List[(Exp, State)])) {
  override def apply(state: State, stmt: Stmt): (List[State], List[(Exp, State)])
}

case class AnyStmtPattern() extends StmtPattern {
  override def apply(state: State, stmt: Stmt): (List[State], List[(Exp, State)]) = {
    (List(), List((Cat(List()), state)))
  }
}
case class IfStmtPattern(cond: ExpPattern, target: LabelPattern) extends StmtPattern {
  override def apply(state: State, stmt: Stmt): (List[State], List[(regEx.Exp, State)]) = {
    stmt.sootStmt match {
      case sootStmt: IfStmt =>
        val (fails, successes) = cond(state, sootStmt.getCondition)
        val (fails2, successes2) = flatMap2(successes, target(_: State, Stmt.getIndex(sootStmt.getTarget, stmt.sootMethod)))
        (fails ++ fails2, successes2.map((Cat(List()), _)))
      case _ => (List(), List())
    }
  }
}
//case class GotoStmtPattern(target: LabelPattern) extends StmtPattern
//case class AssignStmtPattern(lhs: ExpPattern, rhs: ExpPattern) extends StmtPattern


sealed trait ExpPattern extends ((State, soot.Value) => (List[State], List[State]))

//case class AnyExpPattern() extends ExpPattern
//case class InstanceInvokeExpPattern(base: ExpPattern, method: MethodPattern, args: List[ExpPattern]) extends ExpPattern
//case class StaticInvokeExpPattern(method: MethodPattern, args: List[ExpPattern]) extends ExpPattern
//case class EqualsExpPattern(lhs: ExpPattern, rhs: ExpPattern) extends ExpPattern
//case class IntegralConstantExpPattern(value: Long) extends ExpPattern
//case class VariableExpPattern(name: String) extends ExpPattern
//case class CastExpPattern(castType: Type, value: ExpPattern) extends ExpPattern


sealed trait LabelPattern extends ((State, Int) => (List[State], List[State]))

//case class AnyLabelPattern() extends LabelPattern
//case class NamedLabelPattern(name: String) extends LabelPattern


sealed trait MethodPattern

case class AnyMethodPattern() extends MethodPattern
case class ConstantMethodPattern(method: SootMethod) extends MethodPattern

/*
        r1 = virtualinvoke r0.<java.util.ArrayList: java.util.Iterator iterator()>();
     label1:
        $z10 = interfaceinvoke r1.<java.util.Iterator: boolean hasNext()>();
        if $z10 == 0 goto label2;
        $r14 = interfaceinvoke r1.<java.util.Iterator: java.lang.Object next()>();
        _ = (java.lang.Integer) $r14;
        ... (body)
        goto label1;
     label2:
        ...
 */

private object Blah {
//  val iteratorLoopPattern: RegEx[State, Stmt] = r.Cat([
//      r.Fun()
//    ])
}
