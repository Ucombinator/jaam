package org.ucombinator.jaam.util

case class RegEx[State, AtomType]() {
  type Identifier = String
  type Index = Int

  sealed trait Exp

  case class Cat(es: List[Exp]) extends Exp
  case class Alt(es: List[Exp]) extends Exp
  case class Rep(e: Exp) extends Exp
  case class Fun(derive: (State, AtomType) => (List[State], List[(Exp, State)]), parseNull: (State) => (List[State])) extends Exp

  def flatMap2[A, B, C](aList: List[A], fun: A => (List[B], List[C])): (List[B], List[C]) = {
    aList match {
      case List() => (List(), List())
      case a::as =>
        val (bs, cs) = fun(a)
        val (moreBs, moreCs) = flatMap2(as, fun)
        (bs ++ moreBs, cs ++ moreCs)
    }
  }

  def derive(exp: Exp, state: State, atom: AtomType): (List[State], List[(Exp, State)]) = {
    exp match {
      case Cat(List()) => (List(), List())
      case Cat(x::xs) =>
        val (bs, cs) = derive(x, state, atom)
        val derivedBs = flatMap2(bs, derive(Cat(xs), _: State, atom))
        val derivedCs = cs.map({ case (e, s) => (Cat(e::xs), s) })
        (derivedBs._1, derivedBs._2 ++ derivedCs)
      case Alt(List()) => (List(), List())
      case Alt(xs) => flatMap2(xs, derive(_: Exp, state, atom))
      case Rep(x) =>
        val (bs, cs) = derive(x, state, atom)
        (state :: bs, cs)
      case Fun(fd, _) => fd(state, atom)
    }
  }

  def parseNull(exp: Exp, state: State): List[State] = {
    exp match {
      case Cat(es) => es.foldLeft(List(state))((s, e) => s.flatMap(parseNull(e, _)))
      case Alt(es) => es.flatMap(parseNull(_, state))
      case Rep(e) => List(state)
      case Fun(_, fpn) => fpn(state)
    }
  }

  def deriveAll(exp: Exp, state: State, atoms: Seq[AtomType]): List[State] = {
    def step(oldTup: (List[State], List[(Exp, State)]), atom: AtomType): (List[State], List[(Exp, State)]) = {
      println("deriveAll: step: " + oldTup)
      flatMap2[(Exp, State), State, (Exp, State)](oldTup._2, { case (e, s) => derive(e, s, atom) })
    }

    val result = atoms.foldLeft((List[State](), List((exp, state))))(step)
    result._1 ++ result._2.flatMap(es => { parseNull(es._1, es._2) })
  }
}
