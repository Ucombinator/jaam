package org.ucombinator.jaam.tools.taint3

import java.io.{BufferedWriter, FileWriter}

import org.ucombinator.jaam.util.Soot
// import org.ucombinator.jaam.serializer._
import org.ucombinator.jaam.serializer.Serializer
import org.ucombinator.jaam.tools.app.{App, Origin}
import org.ucombinator.jaam.util.Stmt
import org.ucombinator.jaam.util.Soot.unitToStmt
import org.jgrapht._
import org.jgrapht.graph._
import org.jgrapht.ext.{DOTExporter, StringComponentNameProvider}
import soot.options.Options
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}

import scala.collection.immutable
import scala.collection.JavaConverters._

abstract sealed class Address
object Address {
  case class Field(sootField: SootField) extends Address
  case class Return(sootMethod: SootMethod) extends Address
  case class Parameter(sootMethod: SootMethod, index: Int) extends Address
  case class Throws(sootMethod: SootMethod) extends Address
  case class Stmt(stmt: org.ucombinator.jaam.util.Stmt) extends Address
  case class Value(sootValue: SootValue) extends Address
  case class Local(name: String) extends Address
  case class This(typ: Type) extends Address
  case class StaticField(sootField: SootField) extends Address
  case class InstanceField(sootField: SootField) extends Address
  case class ArrayRef(typ: Type) extends Address
}

abstract sealed class Relationship
object Relationship {
  case object Stmt extends Relationship
  case object Return extends Relationship
  case object Throws extends Relationship
  case object Unop extends Relationship
  case object Binop1 extends Relationship
  case object Binop2 extends Relationship
  case object Cast extends Relationship
  case object InstanceOf extends Relationship
  case object Ref extends Relationship
  case object InstanceFieldBase extends Relationship
  case object InstanceFieldValue extends Relationship
  case object ArrayBase extends Relationship
  case object ArrayIndex extends Relationship
  case object ArrayValue extends Relationship
}

object Taint3 {
// TODO: use JGraphT
  val store = Map[Address, Map[Address, Set[Relationship]]]()

  def addNode(a: Address): Unit = {
    ???
  }

  def addEdge(src: Address, dst: Address, rel: Relationship): Unit = {
    ???
  }

  def main(input: List[String], output: String): Unit = {
    println("In Taint3")

    // TODO: load input

    // for each class (if in APP) // TODO: try non-APP
    for (c <- (???): List[SootClass]) {
      // Fields
      for (f <- c.getFields.asScala) {
        addNode(Address.Field(f))
      }
      // Methods
      for (m <- c.getMethods.asScala) {
        addNode(Address.Return(m))
        addNode(Address.Throws(m))
        for (p <- 0 until m.getParameterCount) {
          addNode(Address.Parameter(m, p))
        }
        // TODO: exceptions
        for (u <- Soot.getBody(m).getUnits.asScala) {
          sootStmt(Stmt(u, m))
        }
      }
    }

    // TODO: output the store as a graph (use JGraphT GraphViz to start)
  }

  // TODO: edges between method declarations and implementations
  // TODO:             Scene.v.getActiveHierarchy.getSubclassesOfIncluding(c).asScala.toSet

  def handleInvoke(a0: Address, sootMethod: SootMethod, rhs: InvokeExpr): Unit = {
    // Base (if non-static)
    rhs match {
      case rhs: InstanceInvokeExpr => // TODO
        val aBase = eval(sootMethod, rhs.getBase)
        addEdge(aBase, a0, ???)
    }

    // Parameters
    val aArgs = rhs.getArgs.asScala.map(eval(sootMethod, _))
    val aParams =
      for (i <- 0 until rhs.getMethod.getParameterCount)
      yield { Address.Parameter(rhs.getMethod, i) }
    for ((param, arg) <- aParams zip aArgs) {
      addEdge(arg, param, ???)
    }

    // Return
    val aReturn = Address.Return(rhs.getMethod)
    addEdge(aReturn, a0, ???)
  }

  def sootStmt(stmt: Stmt): Unit = {
    val a0 = Address.Stmt(stmt)
    stmt.sootStmt match {
      case sootStmt : InvokeStmt =>
       handleInvoke(a0, stmt.sootMethod, sootStmt.getInvokeExpr)

      case sootStmt : DefinitionStmt =>
        val aLhs = lhs(stmt.sootMethod, sootStmt.getLeftOp())
        addEdge(a0, aLhs, ???)

        sootStmt.getRightOp() match {
          case rhs: InvokeExpr => handleInvoke(a0, stmt.sootMethod, rhs)
//          case rhs : NewExpr =>
//            val baseType : RefType = rhs.getBaseType()
//            val sootClass = baseType.getSootClass()
//            this.newExpr(lhsAddr, sootClass)
//            Set(this.copy(stmt = stmt.nextSyntactic))
//          case rhs : NewArrayExpr =>
//            // Value of lhsAddr will be set to a pointer to the array. (as opposed to the array itself)
//            /*
//            rhs.getType match {
//              case rt: RefType if (System.isLibraryClass(Soot.getSootClass(rt.getClassName))) =>
//                  Snowflakes.createArray(rt, List(eval(rhs.getSize)), lhsAddr)
//              case t => createArray(t, List(eval(rhs.getSize)), lhsAddr)
//            }
//            */
//            createArray(rhs.getType, List(eval(rhs.getSize)), lhsAddr)
//            Set(this.copy(stmt = stmt.nextSyntactic))
//          case rhs : NewMultiArrayExpr =>
//            //TODO, if base type is Java library class, call Snowflake.createArray
//            //see comment above about lhs addr
//            createArray(rhs.getType, rhs.getSizes.toList map eval, lhsAddr)
//            Set(this.copy(stmt = stmt.nextSyntactic))
          case rhs =>
            val a1 = eval(stmt.sootMethod, rhs)
            val a2 = lhs(stmt.sootMethod, sootStmt.getLeftOp)
            addEdge(a1, a0, ???)
            addEdge(a0, a2, ???)
        }

      case sootStmt : IfStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getCondition)
        // TODO: branch target
        addEdge(a1, a0, Relationship.Stmt)

      case sootStmt : SwitchStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getKey)
        // TODO: branch target
        addEdge(a1, a0, Relationship.Stmt)

      case sootStmt : ReturnStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp)
        addEdge(a1, a0, Relationship.Stmt)
        addEdge(a0, Address.Return(stmt.sootMethod), Relationship.Return)

      case sootStmt : ReturnVoidStmt =>
        addEdge(a0, Address.Return(stmt.sootMethod), Relationship.Return)

      // Since Soot's NopEliminator run before us, no "nop" should be
      // left in the code and this case isn't needed (and also is
      // untested).  The one place a "nop" could occur is as the last
      // instruction of a method that is also the instruction after
      // the end of a "try" clause. (See NopEliminator for the exact
      // conditions.) However, that would not be an executable
      // instruction, so we still wouldn't need this case.
      //
      // If we ever need the code for this, it would probably be:
      //   Set(State(stmt.nextSyntactic, fp, store, kontStack, initializedClasses))
      case sootStmt : NopStmt => throw new Exception("Impossible statement: " + sootStmt)

      case sootStmt : GotoStmt => {}

      // For now we don't model monitor statements, so we just skip over them
      // TODO/soundness: In the event of multi-threaded code with precise interleaving, this is not sound.
      case sootStmt : EnterMonitorStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp())
        addEdge(a1, Address.Stmt(stmt), Relationship.Stmt)
      case sootStmt : ExitMonitorStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp())
        addEdge(a1, Address.Stmt(stmt), Relationship.Stmt)

      // TODO: needs testing
      case sootStmt : ThrowStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp())
        addEdge(a1, a0, Relationship.Stmt)
        addEdge(a0, Address.Throws(stmt.sootMethod), Relationship.Throws)

      // TODO: We're missing BreakPointStmt and RetStmt (but these might not be used)
      case _ =>
        throw new Exception("No match for " + stmt.sootStmt.getClass + " : " + stmt.sootStmt)
    }
  }

  def lhs(m: SootMethod, v: SootValue): Address = {
    val a0 = Address.Value(v)
    addNode(a0)

    v match {
      case v: Local => // TODO: Set(LocalFrameAddr(fp, lhs))
      case v: ParameterRef =>
        val a1 = Address.Parameter(m, v.getIndex)
        addEdge(a0, a1, ???)
      case v: StaticFieldRef =>
        val a1 = Address.StaticField(v.getField)
        addEdge(a0, a1, ???)
      case v: ThisRef =>
        val a1 = Address.This(v.getType)
        addEdge(a0, a1, ???)

      case v: InstanceFieldRef =>
        // TODO: avoid duplication with `eval` by having an `addr` function
        val a1 = eval(m, v.getBase)
        val a2 = Address.InstanceField(v.getField)
        addEdge(a1, a0, Relationship.InstanceFieldBase)
        addEdge(a0, a2, ???)
      case v: ArrayRef =>
        val a1 = eval(m, v.getBase)
        val a2 = eval(m, v.getIndex)
        val a3 = Address.ArrayRef(v.getType)
        addEdge(a1, a0, Relationship.ArrayBase)
        addEdge(a2, a0, Relationship.ArrayIndex)
        addEdge(a0, a3, ???)
      case v: CaughtExceptionRef => {} // TODO
      case _ =>  throw new Exception("No match for " + v.getClass + " : " + v)
    }

    return a0
  }

  def eval(m: SootMethod, v: SootValue): Address = {
    val a0 = Address.Value(v)
    addNode(a0)

    v match {
      // Base cases
      case v : Local =>
        val a1 = Address.Local(v.getName)
        addEdge(a1, a0, Relationship.Ref)
      case v : ParameterRef =>
        val a1 = Address.Parameter(m, v.getIndex)
        addEdge(a1, a0, Relationship.Ref)
      case v : StaticFieldRef =>
        val a1 = Address.StaticField(v.getField)
        addEdge(a1, a0, Relationship.Ref)
      case v : ThisRef =>
        val a1 = Address.This(v.getType)
        addEdge(a1, a0, Relationship.Ref)

      // Recursive
      case v : InstanceFieldRef =>
        val a1 = eval(m, v.getBase)
        val a2 = Address.InstanceField(v.getField)
        addEdge(a1, a0, Relationship.InstanceFieldBase)
        addEdge(a2, a0, Relationship.InstanceFieldValue)
      case v : ArrayRef =>
        val a1 = eval(m, v.getBase)
        val a2 = eval(m, v.getIndex)
        val a3 = Address.ArrayRef(v.getType)
        addEdge(a1, a0, Relationship.ArrayBase)
        addEdge(a2, a0, Relationship.ArrayIndex)
        addEdge(a3, a0, Relationship.ArrayValue)
      case v : CaughtExceptionRef => {}
        // TODO
      case _ : Constant => {}
      case v : UnopExpr =>
        val a1 = eval(m, v.getOp)
        addEdge(a1, a0, Relationship.Unop)
      case v : BinopExpr =>
        val a1 = eval(m, v.getOp1)
        val a2 = eval(m, v.getOp2)
        addEdge(a1, a0, Relationship.Binop1)
        addEdge(a2, a0, Relationship.Binop2)
      case v : InstanceOfExpr =>
        val a1 = eval(m, v.getOp)
        addEdge(a1, a0, Relationship.InstanceOf)
      case v : CastExpr =>
        val a1 = eval(m, v.getOp)
        addEdge(a1, a0, Relationship.Cast)
      case _ =>  throw new Exception("No match for " + v.getClass + " : " + v)
    }

    return a0
  }
}