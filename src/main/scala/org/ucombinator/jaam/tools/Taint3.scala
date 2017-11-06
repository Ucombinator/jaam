package org.ucombinator.jaam.tools.taint3

import java.io.{BufferedWriter, File, FileWriter, Writer}

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


abstract sealed class Relationship extends DefaultEdge
object Relationship {
  case object StmtEdge extends Relationship
  case object ReturnEdge extends Relationship
  case object ThrowsEdge extends Relationship
  case object UnOpEdge extends Relationship
  case object BinOp1Edge extends Relationship
  case object BinOp2Edge extends Relationship
  case object CastEdge extends Relationship
  case object InstanceOfEdge extends Relationship
  case object RefEdge extends Relationship
  case object InstanceFieldBaseEdge extends Relationship
  case object InstanceFieldValueEdge extends Relationship
  case object ArrayBaseEdge extends Relationship
  case object ArrayIndexEdge extends Relationship
  case object ArrayValueEdge extends Relationship
}

object Taint3 {
  val graph = new DefaultDirectedGraph[Address, Relationship](classOf[Relationship])

  def main(input: List[String], output: String): Unit = {
    println("In Taint3")

    // for each class (if in APP)
    for (c <- loadInput(input)) {
      // Fields
      for (f <- c.getFields.asScala) {
        graph.addVertex(Address.Field(f))
      }

      // Methods
      for (m <- c.getMethods.asScala) {
        graph.addVertex(Address.Return(m))
        graph.addVertex(Address.Throws(m))

        for (p <- 0 until m.getParameterCount) {
          graph.addVertex(Address.Parameter(m, p))
        }

        // TODO: exceptions
        for (u <- Soot.getBody(m).getUnits.asScala) {
          sootStmt(Stmt(u, m))
        }
      }
    }

    // Output to a .gv file, which can be used by Graphviz to do visualization
    printToGraphvizFile(output, graph)
  }

  def loadInput(input: List[String]): Set[SootClass] = {
    Options.v().set_verbose(false)
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_keep_line_number(true)
    Options.v().set_allow_phantom_refs(true)
    Options.v().set_include_all(true)
    Options.v().set_prepend_classpath(false)
    Options.v().set_src_prec(Options.src_prec_only_class)

    soot.Main.v().autoSetOptions()

    Soot.useJaamClassProvider()

    val inputPackets = input.flatMap(Serializer.readAll(_).asScala)

    for (a <- inputPackets) { Soot.addClasses(a.asInstanceOf[App]) }

    val mainClasses = for (a <- inputPackets) yield { a.asInstanceOf[App].main.className }
    val mainMethods = for (a <- inputPackets) yield { a.asInstanceOf[App].main.methodName }
    val mainClass = mainClasses.head.get // TODO: fix
    val mainMethod = mainMethods.head.get // TODO: fix

    Scene.v.loadBasicClasses()   // ???
    PackManager.v.runPacks()    // ???

    for(name <- Soot.loadedClasses.keys.toSet
        if Soot.loadedClasses(name).origin == Origin.APP)  // TODO: try non-APP
      yield Soot.getSootClass(name)
  }


  // TODO: edges between method declarations and implementations
  // TODO:             Scene.v.getActiveHierarchy.getSubclassesOfIncluding(c).asScala.toSet
  def handleInvoke(a0: Address, sootMethod: SootMethod, rhs: InvokeExpr): Unit = {
    // Base (if non-static)
    rhs match {
      case rhs: InstanceInvokeExpr => // TODO
        val aBase = eval(sootMethod, rhs.getBase)
        graph.addEdge(aBase, a0, ???)
    }

    // Parameters
    val aArgs = rhs.getArgs.asScala.map(eval(sootMethod, _))
    val aParams = for (i <- 0 until rhs.getMethod.getParameterCount)
      yield { Address.Parameter(rhs.getMethod, i) }

    for ((param, arg) <- aParams zip aArgs) {
      graph.addEdge(arg, param, ???)
    }

    // Return
    val aReturn = Address.Return(rhs.getMethod)
    graph.addEdge(aReturn, a0,???)
  }

  def sootStmt(stmt: Stmt): Unit = {
    val a0 = Address.Stmt(stmt)
    stmt.sootStmt match {
      case sootStmt : InvokeStmt =>
       handleInvoke(a0, stmt.sootMethod, sootStmt.getInvokeExpr)

      case sootStmt : DefinitionStmt =>
        val aLhs = lhs(stmt.sootMethod, sootStmt.getLeftOp)
        graph.addEdge(a0, aLhs,???)

        sootStmt.getRightOp match {
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
            graph.addEdge(a1, a0, ???)
        }

      case sootStmt : IfStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getCondition)
        // TODO: branch target
        graph.addEdge(a1, a0, Relationship.StmtEdge)

      case sootStmt : SwitchStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getKey)
        // TODO: branch target
        graph.addEdge(a1, a0, Relationship.StmtEdge)

      case sootStmt : ReturnStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp)
        graph.addEdge(a1, a0, Relationship.StmtEdge)
        graph.addEdge(a0, Address.Return(stmt.sootMethod), Relationship.ReturnEdge)

      case sootStmt : ReturnVoidStmt =>
        graph.addEdge(a0, Address.Return(stmt.sootMethod), Relationship.ReturnEdge)

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
        val a1 = eval(stmt.sootMethod, sootStmt.getOp)
        graph.addEdge(a1, Address.Stmt(stmt), Relationship.StmtEdge)
      case sootStmt : ExitMonitorStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp)
        graph.addEdge(a1, Address.Stmt(stmt),Relationship.StmtEdge)

      // TODO: needs testing
      case sootStmt : ThrowStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp)
        graph.addEdge(a1, a0, Relationship.StmtEdge)
        graph.addEdge(a0, Address.Throws(stmt.sootMethod),Relationship.ThrowsEdge)

      // TODO: We're missing BreakPointStmt and RetStmt (but these might not be used)
      case _ =>
        throw new Exception("No match for " + stmt.sootStmt.getClass + " : " + stmt.sootStmt)
    }
  }

  def lhs(m: SootMethod, v: SootValue): Address = {
    val a0 = Address.Value(v)
    graph.addVertex(a0)

    v match {
      case v: Local => // TODO: Set(LocalFrameAddr(fp, lhs))
      case v: ParameterRef =>
        val a1 = Address.Parameter(m, v.getIndex)
        graph.addEdge(a0, a1, ???)
      case v: StaticFieldRef =>
        val a1 = Address.StaticField(v.getField)
        graph.addEdge(a0, a1, ???)
      case v: ThisRef =>
        val a1 = Address.This(v.getType)
        graph.addEdge(a0, a1, ???)

      case v: InstanceFieldRef =>
        // TODO: avoid duplication with `eval` by having an `addr` function
        val a1 = eval(m, v.getBase)
        val a2 = Address.InstanceField(v.getField)
        graph.addEdge(a1, a0,Relationship.InstanceFieldBaseEdge)
        graph.addEdge(a0, a2, ???)
      case v: ArrayRef =>
        val a1 = eval(m, v.getBase)
        val a2 = eval(m, v.getIndex)
        val a3 = Address.ArrayRef(v.getType)
        graph.addEdge(a1, a0, Relationship.ArrayBaseEdge)
        graph.addEdge(a2, a0, Relationship.ArrayIndexEdge)
        graph.addEdge(a0, a3, ???)
      case v: CaughtExceptionRef => {} // TODO
      case _ =>  throw new Exception("No match for " + v.getClass + " : " + v)
    }

    return a0
  }

  def eval(m: SootMethod, v: SootValue): Address = {
    val a0 = Address.Value(v)
    graph.addVertex(a0)

    v match {
      // Base cases
      case v : Local =>
        val a1 = Address.Local(v.getName)
        graph.addEdge(a1, a0, Relationship.RefEdge)
      case v : ParameterRef =>
        val a1 = Address.Parameter(m, v.getIndex)
        graph.addEdge(a1, a0, Relationship.RefEdge)
      case v : StaticFieldRef =>
        val a1 = Address.StaticField(v.getField)
        graph.addEdge(a1, a0, Relationship.RefEdge)
      case v : ThisRef =>
        val a1 = Address.This(v.getType)
        graph.addEdge(a1, a0, Relationship.RefEdge)

      // Recursive
      case v : InstanceFieldRef =>
        val a1 = eval(m, v.getBase)
        val a2 = Address.InstanceField(v.getField)
        graph.addEdge(a1, a0, Relationship.InstanceFieldBaseEdge)
        graph.addEdge(a2, a0, Relationship.InstanceFieldValueEdge)
      case v : ArrayRef =>
        val a1 = eval(m, v.getBase)
        val a2 = eval(m, v.getIndex)
        val a3 = Address.ArrayRef(v.getType)
        graph.addEdge(a1, a0, Relationship.ArrayBaseEdge)
        graph.addEdge(a2, a0, Relationship.ArrayIndexEdge)
        graph.addEdge(a3, a0, Relationship.ArrayValueEdge)
      case v : CaughtExceptionRef => {}
        // TODO
      case _ : Constant => {}
      case v : UnopExpr =>
        val a1 = eval(m, v.getOp)
        graph.addEdge(a1, a0, Relationship.UnOpEdge)
      case v : BinopExpr =>
        val a1 = eval(m, v.getOp1)
        val a2 = eval(m, v.getOp2)
        graph.addEdge(a1, a0, Relationship.BinOp1Edge)
        graph.addEdge(a2, a0, Relationship.BinOp2Edge)
      case v : InstanceOfExpr =>
        val a1 = eval(m, v.getOp)
        graph.addEdge(a1, a0, Relationship.InstanceOfEdge)
      case v : CastExpr =>
        val a1 = eval(m, v.getOp)
        graph.addEdge(a1, a0, Relationship.CastEdge)
      case _ =>  throw new Exception("No match for " + v.getClass + " : " + v)
    }

    return a0
  }

  def printToGraphvizFile[V, E](output: String, graph: Graph[V, E]): Unit = {
    val dotExporter = new DOTExporter[V, E](
      new StringComponentNameProvider[V], null,
      new StringComponentNameProvider[E]
    )

    dotExporter.exportGraph(graph, new File(output))
  }
}