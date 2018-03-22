package org.ucombinator.jaam.util

import org.ucombinator.jaam.util.stmtPattern._
import org.ucombinator.jaam.util.stmtPattern.regEx._

case class StmtPatternToRegEx(pattern: LabeledStmtPattern) extends ((State, Stmt) => (List[State], List[(RegExp, State)])) {
  override def apply(state: State, stmt: Stmt): (List[State], List[(RegExp, State)]) = {
    (List(), pattern(state, stmt).map((Cat(List()), _)))
  }
}
