package org.ucombinator.jaam.patterns

case class RegEx[State, AtomType]() {
  type Identifier = String
  type Index = Int

  sealed trait RegExp

  abstract case class Cat(es: List[RegExp]) extends RegExp
  case class Alt(es: List[RegExp]) extends RegExp
  case class Rep(e: RegExp) extends RegExp
  case class Fun(derive: (State, AtomType) => (List[State], List[(RegExp, State)]), parseNull: (State) => (List[State])) extends RegExp
  case class Not(e: RegExp) extends RegExp

  object Cat {
    def apply(es: List[RegExp]): RegExp = {
      es match {
        case List(e) => e
        case _ => new Cat(es) {}
      }
    }
  }

  def flatMap2[A, B, C](aList: List[A], fun: A => (List[B], List[C])): (List[B], List[C]) = {
    aList match {
      case List() => (List(), List())
      case a :: as =>
        val (bs, cs) = fun(a)
        val (moreBs, moreCs) = flatMap2(as, fun)
        (bs ++ moreBs, cs ++ moreCs)
    }
  }

  def derive(exp: RegExp, state: State, atom: AtomType, remaining: List[AtomType]): (List[State], List[(RegExp, State)]) = {
    /*
     * List[State]:
     *   atom is not consumed
     *
     * List[(Exp, State)]:
     *   atom is consumed
     */
    exp match {
      case Cat(List()) => (List(state), List())
      case Cat(x :: xs) =>
        val (bs, cs) = derive(x, state, atom, remaining)
        val derivedBs = flatMap2(bs, derive(Cat(xs), _: State, atom, remaining))
        val derivedCs = cs.map({ case (e, s) => (Cat(e :: xs), s) })
        (derivedBs._1, derivedBs._2 ++ derivedCs)
      case Alt(List()) => (List(), List())
      case Alt(xs) => flatMap2(xs, derive(_: RegExp, state, atom, remaining))
      case Rep(x) =>
        val (_, cs) = derive(x, state, atom, remaining)
        (List(state), cs.map({ case (e, s) => (Cat(List(e, exp)), s) }))
      case Fun(fd, _) => fd(state, atom)
      case Not(x) =>
        if (notStepAll(x, state, atom :: remaining).isEmpty) {
          (List(state), List())
        } else {
          (List(), List())
        }
    }
  }

  def parseNull(exp: RegExp, state: State): List[State] = {
    exp match {
      case Cat(es) => es.foldLeft(List(state))((s, e) => s.flatMap(parseNull(e, _)))
      case Alt(es) => es.flatMap(parseNull(_, state))
      case Rep(e) => List(state)
      case Fun(_, fpn) => fpn(state)
      case Not(e) =>
        if (parseNull(e, state).isEmpty) {
          List(state)
        } else {
          List()
        }
    }
  }

  def notStepAll(exp: RegExp, state: State, atoms: List[AtomType]): List[State] = {
    def step(oldTup: (List[State], List[(RegExp, State)]), remaining: List[AtomType]): List[State] = {
      oldTup._1 ++ (
        remaining match {
          case List() =>
            oldTup._2.flatMap({ case (e, s) => parseNull(e, s) })
          case a :: as =>
            val newTup = flatMap2[(RegExp, State), State, (RegExp, State)](oldTup._2, { case (e, s) => derive(e, s, a, as) })
            step(newTup, as)
        })
    }

    step((List[State](), List((exp, state))), atoms)
  }

  def stepAll(exp: RegExp, state: State, atoms: List[AtomType]): List[(RegExp, State)] = {
    def step(oldTup: (List[State], List[(RegExp, State)]), remaining: List[AtomType]): (List[State], List[(RegExp, State)]) = {
      remaining match {
        case List() => oldTup
        case a :: as =>
          val newTup = flatMap2[(RegExp, State), State, (RegExp, State)](oldTup._2, { case (e, s) => derive(e, s, a, as) })
          step(newTup, as)
      }
    }

    step((List[State](), List((exp, state))), atoms)._2
  }

  def deriveAll(exp: RegExp, state: State, atoms: List[AtomType]): List[State] = {
    stepAll(exp, state, atoms).flatMap({ case (e, s) => parseNull(e, s) })
  }
}
