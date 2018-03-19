package org.ucombinator.jaam.util.stmtPattern

import org.ucombinator.jaam.util.stmtPattern.regEx._
import org.ucombinator.jaam.util.Stmt
import org.ucombinator.jaam.util.Soot
import soot.{Local, SootMethod, Type, UnitPrinter, UnknownType, Value, ValueBox}
import soot.jimple._
import soot.util.Switch

import scala.collection.JavaConverters._

object UnusedInvokeResult extends Value {
  override def getUseBoxes: java.util.List[ValueBox] = new java.util.ArrayList[ValueBox]()
  override def getType: Type = UnknownType.v()
  override def toString(unitPrinter: UnitPrinter): Unit = ???
  override def equivHashCode(): Int = ???
  override def equivTo(o: scala.Any): Boolean = ???
  override def apply(aSwitch: Switch): Unit = ???
}

case class State(indexes: Map[String, Index], identifiers: Map[String, Identifier])

case class LabeledStmtPattern(label: LabelPattern, stmtPattern: StmtPattern) extends ((State, Stmt) => List[State]) {
  override def apply(state: State, stmt: Stmt): List[State] = {
    val states = label(state, stmt.index)
    states.flatMap(stmtPattern(_, stmt))
  }
}

/*
 * STATEMENT PATTERNS
 */

sealed trait StmtPattern extends ((State, Stmt) => List[State]) {
  override def apply(state: State, stmt: Stmt): List[State]
}

case object AnyStmtPattern extends StmtPattern {
  override def apply(state: State, stmt: Stmt): List[State] = {
    List(state)
  }
}
case class IfStmtPattern(cond: ExpPattern, target: LabelPattern) extends StmtPattern {
  override def apply(state: State, stmt: Stmt): List[State] = {
    stmt.sootStmt match {
      case sootStmt: IfStmt =>
        val states = cond(state, sootStmt.getCondition)
        states.flatMap(target(_: State, Stmt.getIndex(sootStmt.getTarget, stmt.sootMethod)))
      case _ => List()
    }
  }
}
case class GotoStmtPattern(target: LabelPattern) extends StmtPattern {
  override def apply(state: State, stmt: Stmt): List[State] = {
    stmt.sootStmt match {
      case sootStmt: GotoStmt =>
        target(state, Stmt.getIndex(Soot.unitToStmt(sootStmt.getTarget), stmt.sootMethod))
      case _ => List()
    }
  }
}
case class AssignStmtPattern(lhs: ExpPattern, rhs: ExpPattern) extends StmtPattern {
  override def apply(state: State, stmt: Stmt): List[State] = {
    stmt.sootStmt match {
      case sootStmt: DefinitionStmt =>
        val states = lhs(state, sootStmt.getLeftOp)
        states.flatMap(rhs(_, sootStmt.getRightOp))
      case sootStmt: InvokeStmt =>
        val states = lhs(state, UnusedInvokeResult)
        states.flatMap(rhs(_, sootStmt.getInvokeExpr))
      case _ => List()
    }
  }
}

/*
 * EXPRESSION PATTERNS
 */

sealed trait ExpPattern extends ((State, Value) => List[State]) {
  override def apply(state: State, value: Value): List[State]
}

case object AnyExpPattern extends ExpPattern {
  override def apply(state: State, value: Value): List[State] = {
    List(state)
  }
}
case class InstanceInvokeExpPattern(base: ExpPattern, method: MethodPattern, args: List[ExpPattern]) extends ExpPattern {
  override def apply(state: State, value: Value): List[State] = {
    value match {
      case value: InstanceInvokeExpr =>
        val states = base(state, value.getBase)
        val states2 = states.flatMap(method(_, value.getMethod))
        val exprArgs = value.getArgs.asScala
        if (args.lengthCompare(exprArgs.length) != 0) {
          List()
        } else {
          args.zip(exprArgs).foldLeft(states2)({ case (prevStates, (e, v)) => prevStates.flatMap(e(_, v)) })
        }
      case _ => List()

       /*
        do
          base(expr.getBase)
          method(expr.getMethod)
          zipWithM_ ($) args expr.getArgs
          return ()
    */
    }
  }
}
case class StaticInvokeExpPattern(method: MethodPattern, args: List[ExpPattern]) extends ExpPattern {
  override def apply(state: State, value: Value): List[State] = {
    value match {
      case value: StaticInvokeExpr =>
        val states = method(state, value.getMethod)
        val exprArgs = value.getArgs.asScala
        if (args.lengthCompare(exprArgs.length) != 0) {
          List()
        } else {
          args.zip(exprArgs).foldLeft(states)({ case (prevStates, (e, v)) => prevStates.flatMap(e(_, v)) })
        }
      case _ => List()
    }
  }
}
case class EqualsExpPattern(lhs: ExpPattern, rhs: ExpPattern) extends ExpPattern {
  override def apply(state: State, value: Value): List[State] = {
    value match {
      case value: EqExpr =>
        val states = lhs(state, value.getOp1)
        states.flatMap(rhs(_, value.getOp2))
      case _ => List()
    }
  }
}
case class IntegralConstantExpPattern(integral: Long) extends ExpPattern {
  override def apply(state: State, value: Value): List[State] = {
    value match {
      case value: IntConstant =>
        if (value.value == integral) {
          List(state)
        } else {
          List()
        }
      case value: LongConstant =>
        if (value.value == integral) {
          List(state)
        } else {
          List()
        }
      case _ => List()
    }
  }
}
case class VariableExpPattern(name: Identifier) extends ExpPattern {
  override def apply(state: State, value: Value): List[State] = {
    value match {
      case value: Local =>
        state.identifiers.get(name) match {
          case Some(id) =>
            if (id == value.getName) {
              List(state)
            } else {
              List()
            }
          case None =>
            List(state.copy(identifiers = state.identifiers + (name -> value.getName)))
        }
      case _ => List()
    }
  }
}
case class CastExpPattern(castType: Type, operand: ExpPattern) extends ExpPattern {
  override def apply(state: State, value: Value): List[State] = {
    value match {
      case value: CastExpr =>
        if (value.getCastType == castType) {
          operand(state, value.getOp)
        } else {
          List()
        }
      case _ => List()
    }
  }
}

/*
 * LABEL PATTERNS
 */

sealed trait LabelPattern extends ((State, Index) => List[State]) {
  override def apply(state: State, index: Index): List[State]
}

case object AnyLabelPattern extends LabelPattern {
  override def apply(state: State, index: Index): List[State] = {
    List(state)
  }
}
case class NamedLabelPattern(name: Identifier) extends LabelPattern {
  override def apply(state: State, index: Index): List[State] = {
    state.indexes.get(name) match {
      case Some(idx) =>
        if (idx == index) {
          List(state)
        } else {
          List()
        }
      case None =>
        List(state.copy(indexes = state.indexes + (name -> index)))
    }
  }
}

/*
 * METHOD PATTERNS
 */

sealed trait MethodPattern extends ((State, SootMethod) => List[State]) {
  override def apply(state: State, sootMethod: SootMethod): List[State]
}

case object AnyMethodPattern extends MethodPattern {
  override def apply(state: State, sootMethod: SootMethod): List[State] = {
    List(state)
  }
}
case class ConstantMethodPattern(method: SootMethod) extends MethodPattern {
  override def apply(state: State, sootMethod: SootMethod): List[State] = {
    if (sootMethod == method) {
      List(state)
    } else {
      List()
    }
  }
}

/*

simpleFor:
        int i1;
        i1 = 0;
     label1:
        if i1 >= 100 goto label2;
        ... (body)
        i1 = i1 + 1;
        goto label1;
     label2:
        ...

forWithBreak:
        int i1;
        i1 = 10;
     label1:
        if i1 >= 135 goto label3;
        _ = _ + i1; (body)
        if _ <= 400 goto label2;
        goto label3;
     label2:
        i1 = i1 + 1;
        goto label1;
     label3:
        return i0;

iterableLoop:
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
