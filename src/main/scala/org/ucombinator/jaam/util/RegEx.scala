package org.ucombinator.jaam.util

class RegEx[State, AtomType] {
  sealed trait Exp

  case class Cat(x: List[Exp]) extends Exp
  case class Alt(x: List[Exp]) extends Exp
  case class Rep(x: Exp) extends Exp
  case class Fun(x: (State, AtomType) => List[(Option[Exp], State)]) extends Exp

  def split[A, B, C](list: List[A], fun: (A => Either[B, C])): (List[B], List[C]) = {
    list match {
      case List() => (List(), List())
      case x::xs =>
        val (bs, cs) = split(xs, fun)
        fun(x) match {
          case Left(b) => (b :: bs, cs)
          case Right(c) => (bs, c :: cs)
        }
    }
  }

  def extractOption(tup: (Option[Exp], State)): Either[State, (Exp, State)] = {
    tup match {
      case (None, s) => Left(s)
      case (Some(e), s) => Right((e, s))
    }
  }

  def derive(exp: Exp, state: State, atom: AtomType): List[(Option[Exp], State)] = {
    exp match {
      case Cat(List()) => List()
      case Cat(x::xs) =>
        val (bs, cs) = split(derive(x, state, atom), extractOption)
        val derivedBs = bs.flatMap(derive(Cat(xs), _, atom))
        val derivedCs = cs.map({ case (e, s) => (Some(Cat(e::xs)), s) })
        derivedBs ++ derivedCs
      case Alt(List()) => List()
      case Alt(x) => x.flatMap(derive(_, state, atom))
      case Rep(x) =>
        val (_, cs) = split(derive(x, state, atom), extractOption)
        (None, state) :: cs.map({ case (e, s) => (Some(e), s) })
      case Fun(x) => x(state, atom)
    }
  }

  def deriveAll(exp: Exp, state: State, atoms: Seq[AtomType]): List[State] = {
    def step(oldTup: (List[State], List[(Exp, State)]), atom: AtomType): (List[State], List[(Exp, State)]) = {
      split(oldTup._2.flatMap({ case (e, s) => derive(e, s, atom) }), extractOption)
    }

    atoms.foldLeft((List[State](), List((exp, state))))(step)._1
  }
}
