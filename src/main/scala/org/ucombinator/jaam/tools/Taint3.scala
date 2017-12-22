package org.ucombinator.jaam.tools.taint3

import java.io._
import scala.collection.JavaConverters._
import org.ucombinator.jaam.util.Soot
import org.ucombinator.jaam.util.Soot.unitToStmt
import org.ucombinator.jaam.util.Stmt
import org.ucombinator.jaam.tools.app.{App, Origin}
import org.ucombinator.jaam.serializer
import soot.options.Options
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}
import org.jgrapht._
import org.jgrapht.graph._
import org.jgrapht.io.{DOTExporter, StringComponentNameProvider}


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
}


abstract sealed class Relationship
object Relationship {
  class  Stmt extends Relationship
  object  Stmt extends Relationship { def apply = new Stmt() }

  class  Return extends Relationship
  object  Return extends Relationship { def apply = new Return() }

  class  Throws extends Relationship
  object  Throws extends Relationship { def apply = new Throws() }

  class  UnOp extends Relationship
  object  UnOp extends Relationship { def apply = new UnOp() }

  class  BinOp1 extends Relationship
  object  BinOp1 extends Relationship { def apply = new BinOp1() }

  class  BinOp2 extends Relationship
  object  BinOp2 extends Relationship { def apply = new BinOp2() }

  class  Cast extends Relationship
  object  Cast extends Relationship { def apply = new Cast() }

  class  InstanceOf extends Relationship
  object  InstanceOf extends Relationship { def apply = new InstanceOf }

  class  Ref extends Relationship
  object Ref extends  Relationship { def apply = new Ref }

  class  InstanceFieldBase extends Relationship
  object  InstanceFieldBase extends Relationship { def apply = new InstanceFieldBase }

  class  InstanceFieldValue extends Relationship
  object  InstanceFieldValue extends Relationship { def apply = new InstanceFieldValue }

  class  ArrayBase extends Relationship
  object  ArrayBase extends Relationship { def apply = new ArrayBase }

  class  ArrayIndex extends Relationship
  object  ArrayIndex extends Relationship { def apply = new ArrayIndex }

  class  ArrayValue extends Relationship
  object  ArrayValue extends Relationship { def apply = new ArrayValue }

  class  InvokeBase extends Relationship
  object  InvokeBase extends Relationship { def apply = new InvokeBase }

  class  Argument(val index: Int) extends Relationship
  object  Argument extends Relationship { def apply(i: Int) = new Argument(i)}

  class  Result extends Relationship
  object  Result extends Relationship { def apply = new Result }

  class  Lhs extends Relationship
  object  Lhs extends Relationship { def apply = new Lhs }

  class  New extends Relationship
  object  New extends Relationship { def apply = new New }

  class  NewArray extends Relationship
  object NewArray extends Relationship { def apply = new NewArray }

  class  NewArraySize extends Relationship
  object  NewArraySize extends Relationship { def apply = new NewArraySize }

  class  NewMultiArray extends Relationship
  object  NewMultiArray extends Relationship { def apply = new NewMultiArray}

  class  NewMultiArraySize(val index: Int) extends Relationship
  object  NewMultiArraySize extends Relationship { def apply(i: Int) = new NewMultiArraySize(i) }

  class  Definition extends Relationship
  object  Definition extends Relationship { def apply = new Definition }

  class  ParameterRef extends Relationship
  object  ParameterRef extends Relationship { def apply = new ParameterRef }

  class  StaticFieldRef extends Relationship
  object  StaticFieldRef extends Relationship { def apply = new StaticFieldRef }

  class  ThisRef extends Relationship
  object ThisRef extends  Relationship { def apply = new ThisRef }

  class  InstanceFieldRef extends Relationship
  object InstanceFieldRef extends  Relationship { def apply = new InstanceFieldRef }

  class  ArrayRef extends Relationship
  object  ArrayRef extends Relationship { def apply = new ArrayRef }

  // Return flow when methods are overridden
  class  MethodOverridden extends Relationship
  object MethodOverridden extends Relationship { def apply = new MethodOverridden }

  // Parameter flow when methods are overridden
  class  ParameterSubtypePolymorphism extends Relationship
  object ParameterSubtypePolymorphism extends Relationship { def apply = new ParameterSubtypePolymorphism }

  // Return value of call depends on parameters of call
  class  ParametersDependency extends Relationship
  object ParametersDependency extends Relationship { def apply = new ParametersDependency }
}

final class Edge(val source: Address, val target: Address, val relation: Relationship) extends serializer.Packet {
  // TODO: does this code for `toString` work?
  override def toString: String = f"Edge($source, $target, $relation)"
  //override def toString: String = relation.getClass.getSimpleName.stripSuffix("$")
}

object Taint3 {
  val graph = new DirectedPseudograph[Address, Edge](classOf[Edge])

  def main(input: List[String], output: String): Unit = {
    val allClasses = loadInput(input)

    def buildInheritanceConnections(m: SootMethod, superMethod: SootMethod, source: Address): Unit = {
      val target = Address.Return(superMethod)
      addEdge(source, target, Relationship.MethodOverridden)

      for (i <- 0 until m.getParameterCount) {
        addEdge(Address.Parameter(superMethod, i),
          Address.Parameter(m, i),
          Relationship.ParameterSubtypePolymorphism)
      }
    }

    // for each class (if in APP)
    for (c <- allClasses) {
      // Fields
      for (f <- c.getFields.asScala) {
        graph.addVertex(Address.Field(f))
      }

      // Methods
      for (m <- c.getMethods.asScala) {
        val source = Address.Return(m)
        val declaringClass = m.getDeclaringClass

        if (!m.isConstructor && !m.isStaticInitializer && !m.isStatic) {
          // NOTE: This counts as overriding methods that
          // <https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-5.html#jvms-5.4.5>
          // says should not override.  But it is an over approximation so it is okay.
          def go(t: SootClass): Unit = {
            t.getMethodUnsafe(m.getName, m.getParameterTypes, m.getReturnType) match {
              case null =>
                if (t != Soot.classes.Object) {
                  (t.getInterfaces.asScala ++ List(t.getSuperclass)).foreach(go)
                }
              case m2 =>
                addEdge(source, Address.Return(m2), Relationship.MethodOverridden)
                for (i <- 0 until m.getParameterCount) {
                  addEdge(Address.Parameter(m2, i),
                    Address.Parameter(m, i),
                    Relationship.ParameterSubtypePolymorphism)
                }
            }
          }

          (declaringClass.getInterfaces.asScala ++
            List(declaringClass.getSuperclass)).foreach(go)
        }

        // TODO: keep track of data-flow due to exceptions
        // TODO: currently not connected to callers
        graph.addVertex(Address.Throws(m))

        for (p <- 0 until m.getParameterCount) {
          graph.addVertex(Address.Parameter(m, p))
        }

        if (m.isConcrete) {
          for (u <- Soot.getBody(m).getUnits.asScala) {
            sootStmt(Stmt(u, m))
          }
        }
      }
    }

    val outSerializer = new serializer.PacketOutput(new FileOutputStream(output))
    output2JaamFile(outSerializer)
    outSerializer.close

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

    val inputPackets = input.flatMap(serializer.Serializer.readAll(_).asScala)

    val apps = inputPackets.filter(_.isInstanceOf[App]).map(_.asInstanceOf[App])
    for (a <- apps) { Soot.addClasses(a) }

    Scene.v.loadBasicClasses()
    PackManager.v.runPacks()

    // TODO: add all super classes and super interfaces (recursively)
    var classes = Set[SootClass]()
    def go(c: SootClass): Unit = {
      if (classes(c)) { /* do nothing */ }
      else {
        classes += c
        go(c.getSuperclass)
        c.getInterfaces.asScala.foreach(go)
      }
    }

    for(name <- Soot.loadedClasses.keys.toSet
        if Soot.loadedClasses(name).origin == Origin.APP) { // TODO: try non-APP
        go(Soot.getSootClass(name))
    }

    return classes
  }

  def addEdge(a1: Address, a2: Address, r: Relationship): Unit = {
    var rc: Boolean = false  // For test
    rc = graph.addVertex(a1)
    rc = graph.addVertex(a2)
    rc = graph.addEdge(a1, a2, new Edge(a1, a2, r))
  }

  // TODO: edges between method declarations and implementations
  // TODO:             Scene.v.getActiveHierarchy.getSubclassesOfIncluding(c).asScala.toSet
  def handleInvoke(a0: Address, sootMethod: SootMethod, rhs: InvokeExpr): Unit = {
    // Base (if non-static)
    rhs match {
      case rhs: InstanceInvokeExpr => // TODO
        // NEW
        // NEW
        val aBase = eval(sootMethod, rhs.getBase)
        addEdge(aBase, a0, Relationship.InvokeBase)
      case _ => /* Do nothing */
    }

    // Parameters
    val aArgs = rhs.getArgs.asScala.map(eval(sootMethod, _))
    val aParams = for (i <- 0 until rhs.getMethod.getParameterCount)
      yield Address.Parameter(rhs.getMethod, i)

    for ((param, arg) <- aParams zip aArgs) {
      addEdge(arg, param, Relationship.Argument(param.index))
    }

    // Return
    val aReturn = Address.Return(rhs.getMethod)
    addEdge(aReturn, a0, Relationship.Result)
  }

  def sootStmt(stmt: Stmt): Unit = {
    val thisMethod = stmt.sootMethod
    val a0 = Address.Stmt(stmt)
    stmt.sootStmt match {
      case sootStmt : InvokeStmt =>
       handleInvoke(a0, thisMethod, sootStmt.getInvokeExpr)

      case sootStmt : DefinitionStmt =>
        val leftOp = sootStmt.getLeftOp
        val aLhs = Address.Value(leftOp)
        lhs(thisMethod, leftOp, aLhs)
        addEdge(a0, aLhs, Relationship.Lhs)

        sootStmt.getRightOp match {
          case rhs: InvokeExpr => handleInvoke(a0, thisMethod, rhs)
          case _: NewExpr =>
            val a1 = Address.New(stmt)
            addEdge(a1, a0, Relationship.New)

          case rhs : NewArrayExpr =>
            val a1 = Address.NewArray(stmt)
            val a2 = eval(thisMethod, rhs.getSize)
            addEdge(a2, a1, Relationship.NewArraySize)
            addEdge(a1, a0, Relationship.NewArray)

          case rhs : NewMultiArrayExpr =>
            val a1 = Address.NewArray(stmt)
            val a2 = rhs.getSizes.asScala.map(eval(thisMethod, _))
            for ((b, i) <- a2.zipWithIndex) {
              addEdge(b, a1, Relationship.NewMultiArraySize(i))
            }
            addEdge(a1, a0, Relationship.NewMultiArray)

          case rhs =>
            val a1 = eval(thisMethod, rhs)
//            println(s"$a1 --->  $a0")
            addEdge(a1, a0, Relationship.Definition)
        }

      case sootStmt : IfStmt =>
        val a1 = eval(thisMethod, sootStmt.getCondition)
        // TODO: branch target
        addEdge(a1, a0, Relationship.Stmt)

      case sootStmt : SwitchStmt =>
        val a1 = eval(thisMethod, sootStmt.getKey)
        // TODO: branch target
        addEdge(a1, a0, Relationship.Stmt)

      case sootStmt : ReturnStmt =>
        val a1 = eval(thisMethod, sootStmt.getOp)
        addEdge(a1, a0, Relationship.Stmt)

        val returnAddress = Address.Return(thisMethod)
        addEdge(a0, returnAddress, Relationship.Return)
        for (i <- 0 until thisMethod.getParameterCount) {
          addEdge(Address.Parameter(thisMethod, i),
                  returnAddress,
                  Relationship.ParametersDependency)
        }

      case _ : ReturnVoidStmt =>
        val returnAddress = Address.Return(thisMethod)
        addEdge(a0, returnAddress, Relationship.Return)
        for (i <- 0 until thisMethod.getParameterCount) {
          addEdge(Address.Parameter(thisMethod, i),
                  returnAddress,
                  Relationship.ParametersDependency)
        }

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

      case _ : GotoStmt => {}

      // For now we don't model monitor statements, so we just skip over them
      // TODO/soundness: In the event of multi-threaded code with precise interleaving, this is not sound.
      case sootStmt : EnterMonitorStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp)
        addEdge(a1, Address.Stmt(stmt), Relationship.Stmt)
      case sootStmt : ExitMonitorStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp)
        addEdge(a1, Address.Stmt(stmt),Relationship.Stmt)

      // TODO: needs testing
      case sootStmt : ThrowStmt =>
        val a1 = eval(stmt.sootMethod, sootStmt.getOp)
        addEdge(a1, a0, Relationship.Stmt)
        addEdge(a0, Address.Throws(stmt.sootMethod),Relationship.Throws)
      // TODO: parameters as ReturnStmt and ReturnVoidStmt ???

      // TODO: We're missing BreakPointStmt and RetStmt (but these might not be used)
      case _ =>
        throw new Exception("No match for " + stmt.sootStmt.getClass + " : " + stmt.sootStmt)
    }
  }

  def lhs(m: SootMethod, v: SootValue, a0: Address.Value): Unit =
    v match {
      case v: Local => // TODO: Set(LocalFrameAddr(fp, lhs))
      case v: ParameterRef =>
        val a1 = Address.Parameter(m, v.getIndex)
        addEdge(a0, a1, Relationship.ParameterRef)
      case v: StaticFieldRef =>
        val a1 = Address.StaticField(v.getField)
        addEdge(a0, a1, Relationship.StaticFieldRef)
      case v: ThisRef =>
        val a1 = Address.This(v.getType)
        addEdge(a0, a1, Relationship.ThisRef)

      case v: InstanceFieldRef =>
        // TODO: avoid duplication with `eval` by having an `addr` function
        val a1 = eval(m, v.getBase)
        val a2 = Address.InstanceField(v.getField)
        addEdge(a1, a0, Relationship.InstanceFieldBase)
        addEdge(a0, a2, Relationship.InstanceFieldRef)
      case v: ArrayRef =>
        val a1 = eval(m, v.getBase)
        val a2 = eval(m, v.getIndex)
        val a3 = Address.ArrayRef(v.getType)
        addEdge(a1, a0, Relationship.ArrayBase)
        addEdge(a2, a0, Relationship.ArrayIndex)
        addEdge(a0, a3, Relationship.ArrayRef)
      case _: CaughtExceptionRef => {} // TODO
      case _ =>  throw new Exception("No match for " + v.getClass + " : " + v)
    }

  def eval(m: SootMethod, v: SootValue): Address = {
    val a0 = Address.Value(v)

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
      case _ : CaughtExceptionRef => {}
        // TODO
      case _ : Constant => {}
      case v : UnopExpr =>
        val a1 = eval(m, v.getOp)
        addEdge(a1, a0, Relationship.UnOp)
      case v : BinopExpr =>
        val a1 = eval(m, v.getOp1)
        val a2 = eval(m, v.getOp2)
        addEdge(a1, a0, Relationship.BinOp1)
        addEdge(a2, a0, Relationship.BinOp2)
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

    dotExporter.exportGraph(graph, new File(output.replace("jaam", "gv")))
  }

  def output2JaamFile(pktOut: serializer.PacketOutput): Unit = {
    graph.vertexSet.asScala.foreach(pktOut.write)
    graph.edgeSet.asScala.foreach(pktOut.write)
  }
}