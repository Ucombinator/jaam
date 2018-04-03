package org.ucombinator.jaam.patterns

import org.ucombinator.jaam.patterns.stmt._
import org.ucombinator.jaam.util.Stmt

case class StmtPatternToRegEx(pattern: LabeledStmtPattern) extends ((State, Stmt) => (List[State], List[(RegExp, State)])) {
  override def apply(state: State, stmt: Stmt): (List[State], List[(RegExp, State)]) = {
    (List(), pattern(state, stmt).map((Cat(List()), _)))
  }
}
