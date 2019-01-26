package org.ucombinator.jaam.util

import scala.collection.mutable.ListBuffer

class CFBooleanExpr() {
  private var terms: ListBuffer[Term] = _

  {
    terms = new ListBuffer[Term]()
  }

  def this(term: Term) {
    this()
    this.terms += term
  }

  def this(terms: ListBuffer[Term]) {
    this()
    this.terms = terms
  }

  def isIdentityExpr: Boolean = terms.isEmpty

  def getTerms: ListBuffer[Term] = terms

  def addTerm(term: Term): Unit = {
    terms += term
    simplify(term)
  }

  def addExpr(expr: CFBooleanExpr): Unit = {
    for (term <- expr.terms) {
      addTerm(term)
    }
  }

  def and(factor: Factor): CFBooleanExpr = {
    for (term <- terms) {
      term.addFactor(factor)
    }

    new CFBooleanExpr(terms)
  }

  def simplify(newTerm: Term): Unit = {
    for (term <- terms) {
      if (term != newTerm) {
        val simplifiedExpr: Option[Term] = simplifyTerms(term, newTerm)
        simplifiedExpr match {
          case Some(simplifiedTerm) => {
            terms = terms -- List(term, newTerm)
            addTerm(simplifiedTerm)
            return
          }
          case None => None
        }
      }
    }
  }

  def diff(factors1: ListBuffer[Factor], factors2: ListBuffer[Factor]): ListBuffer[Factor] = {
    factors1 filterNot (x => factors2.contains(x))
  }

  def simplifyTerms(term1: Term, term2: Term): Option[Term] = {
    val factors1: ListBuffer[Factor] = term1.getFactors()
    val factors2: ListBuffer[Factor] = term2.getFactors()
    val diff1: ListBuffer[Factor] = diff(factors1, factors2).sorted
    val diff2: ListBuffer[Factor] = diff(factors2, factors1).sorted

    //if terms are same then return one of them
    if (diff1.isEmpty && diff2.isEmpty) {
      return Some(term1)
    }

    //check if same term exists with opp polarity in diff lists
    if (diff1.isEmpty || diff2.isEmpty || diff1.size != diff2.size || diff1.size > 1) {
      return None
    } else {
      for (i <- diff1.indices) {
        if (diff1(i).getValue() != diff2(i).getValue()) {
          return None
        }
      }
    }

    //if we reach here, same terms with opp polarities are present in diff lists
    //so return the common elements in them
    val simplifiedTerm = new Term(factors1 -- diff1)
    Some(simplifiedTerm)
  }

  def getConstants(): List[String] = {
    var constants: Set[String] = Set()
    for (term <- terms) {
      for (factor <- term.getFactors()) {
        constants = constants + factor.getValue()
      }
    }
    constants.toList
  }

  override def toString(): String = {
    var expr: String = ""
    for (term <- terms) {
      if (expr != "") {
        expr = expr + "+" + "(" + term.toString() + ")"
      } else {
        expr = "(" + term.toString() + ")"
      }
    }
    expr
  }

  override def equals(that: Any): Boolean =
    that match {
      case that: CFBooleanExpr => that.isInstanceOf[CFBooleanExpr] && this.hashCode == that.hashCode
      case _ => false
    }

  override def clone(): CFBooleanExpr = {
    var clonedTerms = new ListBuffer[Term]()
    for (term <- terms) {
      clonedTerms += term.clone()
    }

    new CFBooleanExpr(clonedTerms)
  }
}

class Term {
  private var factors: ListBuffer[Factor] = _

  {
    factors = new ListBuffer[Factor]()
  }

  def this(factor: Factor) {
    this()
    factors += factor
  }

  def this(factors: ListBuffer[Factor]) {
    this()
    this.factors = factors
  }

  def addFactor(factor: Factor): Unit = {
    factors += factor
    simplify(factor)
  }

  def simplify(newFactor: Factor): Unit = {
    for (factor <- factors) {
      if(factor != newFactor){
        if (factor.getValue() == newFactor.getValue()) {
          if (factor.polarity() == newFactor.polarity()) {
            factors = factors - newFactor
          } else {
            factors = factors -- List(newFactor, factor)
          }
        }
      }
    }
  }

  def getFactors(): ListBuffer[Factor] = factors

  override def toString(): String = {
    var term: String = ""
    for (factor <- factors) {
      if (term != "") {
        term = term + "*" + factor.toString()
      } else {
        term = factor.toString()
      }
    }
    term
  }

  override def clone(): Term = {
    var clonedFactors = new ListBuffer[Factor]()
    for (factor <- factors) {
      clonedFactors += factor.clone()
    }

    new Term(clonedFactors)
  }

}

class Factor(value: String, isPositive: Boolean = true) extends Ordered[Factor] {
  override def toString(): String = {
    if (!isPositive) {
      "~" + value
    } else {
      value
    }
  }

  def getValue(): String = value

  def polarity(): Boolean = isPositive

  override def clone(): Factor = new Factor(value, isPositive)

  override def equals(that: Any): Boolean =
    that match {
      case that: Factor => that.isInstanceOf[Factor] && this.value == that.getValue() && this.isPositive == that.polarity()
      case _ => false
    }

  def compare(that: Factor) = this.value.compare(that.getValue())

}