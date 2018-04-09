package org.ucombinator.jaam.patterns

case class RegEx[State, AtomType]() {
  type Identifier = String
  type Index = Int

  sealed trait RegExp

  abstract case class Cat(es: List[RegExp]) extends RegExp
  case class Alt(es: List[RegExp]) extends RegExp
  case class Rep(e: RegExp) extends RegExp
  case class Fun(derive: (State, AtomType) => (List[State], List[(RegExp, State)]), parseNull: (State) => (List[State])) extends RegExp

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
      case a::as =>
        val (bs, cs) = fun(a)
        val (moreBs, moreCs) = flatMap2(as, fun)
        (bs ++ moreBs, cs ++ moreCs)
    }
  }

  def derive(exp: RegExp, state: State, atom: AtomType): (List[State], List[(RegExp, State)]) = {
    /*
     * List[State]:
     *   atom is not consumed
     *
     * List[(Exp, State)]:
     *   atom is consumed
     */
    exp match {
      case Cat(List()) => (List(state), List())
      case Cat(x::xs) =>
        val (bs, cs) = derive(x, state, atom)
        val derivedBs = flatMap2(bs, derive(Cat(xs), _: State, atom))
        val derivedCs = cs.map({ case (e, s) => (Cat(e::xs), s) })
        (derivedBs._1, derivedBs._2 ++ derivedCs)
      case Alt(List()) => (List(), List())
      case Alt(xs) => flatMap2(xs, derive(_: RegExp, state, atom))
      case Rep(x) =>
        val (_, cs) = derive(x, state, atom)
        (List(state), cs.map({ case (e, s) => (Cat(List(e, exp)), s)} ))
      case Fun(fd, _) => fd(state, atom)
    }
  }

  def parseNull(exp: RegExp, state: State): List[State] = {
    println("parseNull: (" + exp + ", " + state + ")")
    exp match {
      case Cat(es) => es.foldLeft(List(state))((s, e) => s.flatMap(parseNull(e, _)))
      case Alt(es) => es.flatMap(parseNull(_, state))
      case Rep(e) => List(state)
      case Fun(_, fpn) => fpn(state)
    }
  }

  def deriveAll(exp: RegExp, state: State, atoms: Seq[AtomType]): List[State] = {
    println("deriveAll:")
    println("  exp:   " + exp)
    println("  state: " + state)
    println("  atoms: " + atoms)
    def step(oldTup: (List[State], List[(RegExp, State)]), atom: AtomType): (List[State], List[(RegExp, State)]) = {
      println("  step: oldTup: " + oldTup)
      println("        atom:   " + atom)
      flatMap2[(RegExp, State), State, (RegExp, State)](oldTup._2, { case (e, s) => derive(e, s, atom) })
    }

    val (_, result1) = atoms.foldLeft((List[State](), List((exp, state))))(step)
    val result2 = result1.flatMap({ case (e, s) => parseNull(e, s) })
    println("  result1: " + result1)
    println("  result2: " + result2)
    result2
  }
}
