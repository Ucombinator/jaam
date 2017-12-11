package org.ucombinator.jaam.tools.taint4

import java.io._

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

import scala.collection.JavaConverters._

import scala.collection.mutable

import org.ucombinator.jaam.{serializer, tools}
import org.ucombinator.jaam.util._


abstract sealed class Address extends serializer.Packet
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
  case class New(stmt: org.ucombinator.jaam.util.Stmt) extends Address
  case class NewArray(stmt: org.ucombinator.jaam.util.Stmt) extends Address
  case class NewMultiArray(stmt: org.ucombinator.jaam.util.Stmt) extends Address

  def addressToString(address: Address): String = {
    def quote(s: String): String = "\"" + s + "\""

    def escapeIllegalChars(a: Address): String =
      a.
        toString.
        replace("\\", "\\\\").
        replace("\"", "\\\"")

    quote(escapeIllegalChars(address))
  }
}

abstract sealed class Relationship
object Relationship {
  class  StmtEdge extends Relationship
  object  StmtEdge extends Relationship { def apply = new StmtEdge }

  class  ReturnEdge extends Relationship
  object  ReturnEdge extends Relationship { def apply = new ReturnEdge }

  class  ThrowsEdge extends Relationship
  object  ThrowsEdge extends Relationship { def apply = new ThrowsEdge }

  class  UnOpEdge extends Relationship
  object  UnOpEdge extends Relationship { def apply = new UnOpEdge }

  class  BinOp1Edge extends Relationship
  object  BinOp1Edge extends Relationship { def apply = new BinOp1Edge }

  class  BinOp2Edge extends Relationship
  object  BinOp2Edge extends Relationship { def apply = new BinOp2Edge }

  class  CastEdge extends Relationship
  object  CastEdge extends Relationship { def apply = new CastEdge }

  class  InstanceOfEdge extends Relationship
  object  InstanceOfEdge extends Relationship { def apply = new InstanceOfEdge }

  class  RefEdge extends Relationship
  object RefEdge extends  Relationship { def apply = new RefEdge }

  class  InstanceFieldBaseEdge extends Relationship
  object  InstanceFieldBaseEdge extends Relationship { def apply = new InstanceFieldBaseEdge }

  class  InstanceFieldValueEdge extends Relationship
  object  InstanceFieldValueEdge extends Relationship { def apply = new InstanceFieldValueEdge }

  class  ArrayBaseEdge extends Relationship
  object  ArrayBaseEdge extends Relationship { def apply = new ArrayBaseEdge }

  class  ArrayIndexEdge extends Relationship
  object  ArrayIndexEdge extends Relationship { def apply = new ArrayIndexEdge }

  class  ArrayValueEdge extends Relationship
  object  ArrayValueEdge extends Relationship { def apply = new ArrayValueEdge }

  class  InvokeBaseEdge extends Relationship
  object  InvokeBaseEdge extends Relationship { def apply = new InvokeBaseEdge }

  class  ArgumentEdge(val index: Int) extends Relationship
  object  ArgumentEdge extends Relationship { def apply(i: Int) = new ArgumentEdge(i)}

  class  ResultEdge extends Relationship
  object  ResultEdge extends Relationship { def apply = new ResultEdge }

  class  LhsEdge extends Relationship
  object  LhsEdge extends Relationship { def apply = new LhsEdge }

  class  NewEdge extends Relationship
  object  NewEdge extends Relationship { def apply = new NewEdge }

  class  NewArrayEdge extends Relationship
  object NewArrayEdge extends Relationship { def apply = new NewArrayEdge }

  class  NewArraySizeEdge extends Relationship
  object  NewArraySizeEdge extends Relationship { def apply = new NewArraySizeEdge }

  class  NewMultiArrayEdge extends Relationship
  object  NewMultiArrayEdge extends Relationship { def apply = new NewMultiArrayEdge}

  class  NewMultiArraySizeEdge(val index: Int) extends Relationship
  object  NewMultiArraySizeEdge extends Relationship { def apply(i: Int) = new NewMultiArraySizeEdge(i) }

  class  DefinitionEdge extends Relationship
  object  DefinitionEdge extends Relationship { def apply = new DefinitionEdge }

  class  ParameterRefEdge extends Relationship
  object  ParameterRefEdge extends Relationship { def apply = new ParameterRefEdge }

  class  StaticFieldRefEdge extends Relationship
  object  StaticFieldRefEdge extends Relationship { def apply = new StaticFieldRefEdge }

  class  ThisRefEdge extends Relationship
  object ThisRefEdge extends  Relationship { def apply = new ThisRefEdge }

  class  InstanceFieldRefEdge extends Relationship
  object InstanceFieldRefEdge extends  Relationship { def apply = new InstanceFieldRefEdge }

  class  ArrayRefEdge extends Relationship
  object  ArrayRefEdge extends Relationship { def apply = new ArrayRefEdge }
}

final case class Edge(source: Address, target: Address, relation: Relationship) extends serializer.Packet


object Taint4 {
  val nodes: mutable.LinkedHashSet[Address] = mutable.LinkedHashSet.empty
  val graph: mutable.Map[Address, mutable.Set[Address]] = mutable.Map.empty
  var edges: mutable.Map[(Address, Address), mutable.Set[Relationship]] = mutable.Map.empty

  def main(input: List[String], output: String): Unit = {
    println("In Taint4")

    // for each class (if in APP)
    for (c <- loadInput(input)) {
      // Fields
      for (f <- c.getFields.asScala) {
        val node = Address.Field(f)
        nodes.add(node)
        graph.put(node, mutable.Set.empty)
      }

      // Methods
      for (m <- c.getMethods.asScala) {
        val node1 = Address.Return(m)
        val node2 = Address.Throws(m)
        nodes.add(node1)
        nodes.add(node2)
        graph.put(node1, mutable.Set.empty)
        graph.put(node2, mutable.Set.empty)

        for (p <- 0 until m.getParameterCount;
             nd = Address.Parameter(m, p)) {
          nodes.add(nd)
          graph.put(nd, mutable.Set.empty)
        }

        // TODO: keep track of data-flow due to exceptions
        for (u <- Soot.getBody(m).getUnits.asScala) {
          sootStmt(Stmt(u, m))
        }
      }
    }

    val outSerializer = new serializer.PacketOutput(new FileOutputStream(output))
    output2JaamFile(outSerializer)
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

    val apps = inputPackets.filter(_.isInstanceOf[App])
    for (a <- apps) { Soot.addClasses(a.asInstanceOf[App]) }

    val mainClasses = for (a <- apps) yield { a.asInstanceOf[App].main.className }
    val mainMethods = for (a <- apps) yield { a.asInstanceOf[App].main.methodName }
    val mainClass = mainClasses.head.get // TODO: fix
    val mainMethod = mainMethods.head.get // TODO: fix

    Scene.v.loadBasicClasses()   // ???
    PackManager.v.runPacks()    // ???

    for(name <- Soot.loadedClasses.keys.toSet
        if Soot.loadedClasses(name).origin == Origin.APP)  // TODO: try non-APP
      yield Soot.getSootClass(name)
  }

  def addEdge(s: Address, t: Address, r: Relationship): Unit = {
    nodes.add(s)
    nodes.add(t)

    if (graph.contains(s)) graph(s).add(t)
    else                   graph.put(s, mutable.Set(t))

    if (!graph.contains(t)) graph.put(t, mutable.Set.empty)

    val key = (s, t)
    if (edges.contains(key)) edges(key).add(r)
    else                     edges.put(key, mutable.Set.empty)
  }

  // TODO: edges between method declarations and implementations
  // TODO:             Scene.v.getActiveHierarchy.getSubclassesOfIncluding(c).asScala.toSet
  def handleInvoke(a0: Address, sootMethod: SootMethod, rhs: InvokeExpr): Unit = {
    // Base (if non-static)
    rhs match {
      case rhs: InstanceInvokeExpr => // TODO
        val aBase = eval(sootMethod, rhs.getBase)
        addEdge(aBase, a0, Relationship.InvokeBaseEdge)
      case _ => /* Do nothing */
    }

    // Parameters
    val aArgs = rhs.getArgs.asScala.map(eval(sootMethod, _))
    val aParams = for (i <- 0 until rhs.getMethod.getParameterCount)
      yield { Address.Parameter(rhs.getMethod, i) }

    for ((param, arg) <- aParams zip aArgs) {
      addEdge(arg, param, Relationship.ArgumentEdge(param.index))
    }

    // Return
    val aReturn = Address.Return(rhs.getMethod)
    addEdge(aReturn, a0, Relationship.ResultEdge)
  }

  def sootStmt(stmt: Stmt): Unit = {
    val a0 = Address.Stmt(stmt)
    stmt.sootStmt match {
      case sootStmt : InvokeStmt =>
        handleInvoke(a0, stmt.sootMethod, sootStmt.getInvokeExpr)

      case sootStmt : DefinitionStmt =>
        val aLhs = lhs(stmt.sootMethod, sootStmt.getLeftOp)
        addEdge(a0, aLhs, Relationship.LhsEdge)

        sootStmt.getRightOp match {
          case rhs: InvokeExpr => handleInvoke(a0, stmt.sootMethod, rhs)
          case _ : NewExpr =>
            val a1 = Address.New(stmt)
            addEdge(a1, a0, Relationship.NewEdge)

          case rhs : NewArrayExpr =>
            val a1 = Address.NewArray(stmt)
            val a2 = eval(stmt.sootMethod, rhs.getSize)
            addEdge(a2, a1, Relationship.NewArraySizeEdge)
            addEdge(a1, a0, Relationship.NewArrayEdge)

          case rhs : NewMultiArrayExpr =>
            val a1 = Address.NewArray(stmt)
            val a2 = rhs.getSizes.asScala.map(eval(stmt.sootMethod, _))
            for ((b, i) <- a2.zipWithIndex) {
              addEdge(b, a1, Relationship.NewMultiArraySizeEdge(i))
            }
            addEdge(a1, a0, Relationship.NewMultiArrayEdge)

          case rhs =>
            val a1 = eval(stmt.sootMethod, rhs)
            addEdge(a1, a0, Relationship.DefinitionEdge)
        }

      case sootStmt : IfStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getCondition)
        // TODO: branch target
        addEdge(a1, a0, Relationship.StmtEdge)

      case sootStmt : SwitchStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getKey)
        // TODO: branch target
        addEdge(a1, a0, Relationship.StmtEdge)

      case sootStmt : ReturnStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp)
        addEdge(a1, a0, Relationship.StmtEdge)
        addEdge(a0, Address.Return(stmt.sootMethod), Relationship.ReturnEdge)

      case sootStmt : ReturnVoidStmt =>
        addEdge(a0, Address.Return(stmt.sootMethod), Relationship.ReturnEdge)

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
        addEdge(a1, Address.Stmt(stmt), Relationship.StmtEdge)
      case sootStmt : ExitMonitorStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp)
        addEdge(a1, Address.Stmt(stmt),Relationship.StmtEdge)

      // TODO: needs testing
      case sootStmt : ThrowStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp)
        addEdge(a1, a0, Relationship.StmtEdge)
        addEdge(a0, Address.Throws(stmt.sootMethod),Relationship.ThrowsEdge)

      // TODO: We're missing BreakPointStmt and RetStmt (but these might not be used)
      case _ =>
        throw new Exception("No match for " + stmt.sootStmt.getClass + " : " + stmt.sootStmt)
    }
  }

  def lhs(m: SootMethod, v: SootValue): Address = {
    val a0 = Address.Value(v)

    v match {
      case _: Local => // TODO: Set(LocalFrameAddr(fp, lhs))
      case v: ParameterRef =>
        val a1 = Address.Parameter(m, v.getIndex)
        addEdge(a0, a1, Relationship.ParameterRefEdge)
      case v: StaticFieldRef =>
        val a1 = Address.StaticField(v.getField)
        addEdge(a0, a1, Relationship.StaticFieldRefEdge)
      case v: ThisRef =>
        val a1 = Address.This(v.getType)
        addEdge(a0, a1, Relationship.ThisRefEdge)

      case v: InstanceFieldRef =>
        // TODO: avoid duplication with `eval` by having an `addr` function
        val a1 = eval(m, v.getBase)
        val a2 = Address.InstanceField(v.getField)
        addEdge(a1, a0, Relationship.InstanceFieldBaseEdge)
        addEdge(a0, a2, Relationship.InstanceFieldRefEdge)
      case v: ArrayRef =>
        val a1 = eval(m, v.getBase)
        val a2 = eval(m, v.getIndex)
        val a3 = Address.ArrayRef(v.getType)
        addEdge(a1, a0, Relationship.ArrayBaseEdge)
        addEdge(a2, a0, Relationship.ArrayIndexEdge)
        addEdge(a0, a3, Relationship.ArrayRefEdge)
      case v: CaughtExceptionRef => {} // TODO
      case _ =>  throw new Exception("No match for " + v.getClass + " : " + v)
    }

    a0
  }

  def eval(m: SootMethod, v: SootValue): Address = {
    val a0 = Address.Value(v)
    // graph.addVertex(a0)

    v match {
      // Base cases
      case v : Local =>
        val a1 = Address.Local(v.getName)
        addEdge(a1, a0, Relationship.RefEdge)
      case v : ParameterRef =>
        val a1 = Address.Parameter(m, v.getIndex)
        addEdge(a1, a0, Relationship.RefEdge)
      case v : StaticFieldRef =>
        val a1 = Address.StaticField(v.getField)
        addEdge(a1, a0, Relationship.RefEdge)
      case v : ThisRef =>
        val a1 = Address.This(v.getType)
        addEdge(a1, a0, Relationship.RefEdge)

      // Recursive
      case v : InstanceFieldRef =>
        val a1 = eval(m, v.getBase)
        val a2 = Address.InstanceField(v.getField)
        addEdge(a1, a0, Relationship.InstanceFieldBaseEdge)
        addEdge(a2, a0, Relationship.InstanceFieldValueEdge)
      case v : ArrayRef =>
        val a1 = eval(m, v.getBase)
        val a2 = eval(m, v.getIndex)
        val a3 = Address.ArrayRef(v.getType)
        addEdge(a1, a0, Relationship.ArrayBaseEdge)
        addEdge(a2, a0, Relationship.ArrayIndexEdge)
        addEdge(a3, a0, Relationship.ArrayValueEdge)
      case v : CaughtExceptionRef => {}
      // TODO
      case _ : Constant => {}
      case v : UnopExpr =>
        val a1 = eval(m, v.getOp)
        addEdge(a1, a0, Relationship.UnOpEdge)
      case v : BinopExpr =>
        val a1 = eval(m, v.getOp1)
        val a2 = eval(m, v.getOp2)
        addEdge(a1, a0, Relationship.BinOp1Edge)
        addEdge(a2, a0, Relationship.BinOp2Edge)
      case v : InstanceOfExpr =>
        val a1 = eval(m, v.getOp)
        addEdge(a1, a0, Relationship.InstanceOfEdge)
      case v : CastExpr =>
        val a1 = eval(m, v.getOp)
        addEdge(a1, a0, Relationship.CastEdge)
      case _ =>  throw new Exception("No match for " + v.getClass + " : " + v)
    }

    a0
  }

  class EscapedStringComponentNameProvider[T](quotes: Boolean) extends StringComponentNameProvider[T] {
    override def getName(component: T): String = {
      val s = component.toString
        .replaceAll("\\\\", "\\\\\\\\")
        .replaceAll("\"", "\\\\\"")
      if (quotes) { "\"" + s + "\""}
      else { s }
    }
  }

  def printToGraphvizFile[V, E](output: String, graph: Graph[V, E]): Unit = {
    val dotExporter = new DOTExporter[V, E](
      new EscapedStringComponentNameProvider[V](true), null,
      new EscapedStringComponentNameProvider[E](false)
    )

    dotExporter.exportGraph(graph, new File(output))
  }

  def output2JaamFile(pktOut: serializer.PacketOutput): Unit = {
    for (s <- nodes) {
      pktOut.write(s)

      for (t <- graph(s)) {
        pktOut.write(t)

        val key = (s, t)
        for (e <- edges(key))
          pktOut.write(Edge(s, t, e))
      }
    }
  }
}

