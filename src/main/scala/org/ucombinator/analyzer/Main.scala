package org.ucombinator.analyzer

import org.ucombinator.SootWrapper
import scala.collection.JavaConversions._
import scala.language.postfixOps
import soot.util.Chain
import soot.SootClass
import soot.SootMethod

// We expect every Unit we use to be a soot.jimple.Stmt, but the APIs
// are built around using Unit so we stick with that.
import soot.{Unit => SootUnit}


import soot.Local
import soot.{Value => SootValue}
import soot.IntType
import soot.jimple._

abstract class FramePointer

class ConcreteFramePointer() extends FramePointer

object InvariantFramePointer extends FramePointer

case class KontStack(store : KontStore, k : Kont) {
  def push(frame : Frame) : KontStack = {
    // TODO replace InvariantKontAddr with call to parameterized function
    val kAddr = InvariantKontAddr
    val newKontStore = store.update(kAddr, Set(k))
    KontStack(newKontStore, RetKont(frame, kAddr))
  }

  def pop() : Set[(Frame, KontStack)] = {
    k match {
      case RetKont(frame, kontAddr) => {
        for (topk <- store(kontAddr)) yield (frame, KontStack(store, topk))
        }
      case HaltKont => Set()
    }
  }
}

abstract class KontAddr

case object InvariantKontAddr extends KontAddr

// TODO Michael B: refactor KontStore and Store, since they only
// differ in their types
case class KontStore(private val map : Map[KontAddr, Set[Kont]]) {
  def update(addr : KontAddr, konts : Set[Kont]) : KontStore = {
    map.get(addr) match {
      case Some(oldd) => KontStore(map + (addr -> (oldd ++ konts)))
      case None => KontStore(map + (addr -> konts))
    }
  }

  def apply(addr : KontAddr) : Set[Kont] = map(addr)
  def get(addr : KontAddr) : Option[Set[Kont]] = map.get(addr)
}


case class Frame(
  val stmt : Stmt,
  val fp : FramePointer,
  val destAddr : Option[Addr]) {

  def acceptsReturnValue() : Boolean = !(destAddr.isEmpty)
}

abstract class Kont

case class RetKont(
  val frame : Frame,
  val k : KontAddr
) extends Kont

object HaltKont extends Kont

case class D(val values: Set[Value]) {
  def join(otherd : D) : D = {
    D(values ++ otherd.values)
  }
}

object D {
  val atomicTop = D(Set(AnyAtomicValue))
}

abstract class Value

abstract class AtomicValue extends Value

case object AnyAtomicValue extends AtomicValue

abstract class Addr

abstract class FrameAddr extends Addr

case class Store(private val map : Map[Addr, D]) {
  def update(addr : Addr, d : D) : Store= {
    map.get(addr) match {
      case Some(oldd) => Store(map + (addr -> oldd.join(d)))
      case None => Store(map + (addr -> d))
    }
  }

  def apply(addr : Addr) : D = map(addr)
  def get(addr : Addr) : Option[D] = map.get(addr)
}

case class LocalFrameAddr(val fp : FramePointer, val register : Local) extends FrameAddr

case class ParameterFrameAddr(val fp : FramePointer, val parameter : Int) extends FrameAddr

case class Stmt(val unit : SootUnit, val method : SootMethod, val program : Map[String, SootClass]) {
  def next_syntactic() : Stmt = {
    Stmt(method.getActiveBody().getUnits().getSuccOf(unit), method, program)
  }
}

case class State(stmt : Stmt, fp : FramePointer, store : Store, kontStack : KontStack) {
  def alloca() : FramePointer = InvariantFramePointer

  def eval(v: SootValue, fp : FramePointer, store : Store) : D = {
    v match {
      case n : NumericConstant => D.atomicTop
      case subexpr : SubExpr => {
        assert(subexpr.getOp1().getType().isInstanceOf[IntType])
        assert(subexpr.getOp2().getType().isInstanceOf[IntType])

        D.atomicTop
      }

      case subexpr : MulExpr => {
        assert(subexpr.getOp1().getType().isInstanceOf[IntType])
        assert(subexpr.getOp2().getType().isInstanceOf[IntType])

        D.atomicTop
      }

      case local : Local => store(LocalFrameAddr(fp, local))

      case _ =>  throw new Exception("No match for " + v.getClass + " : " + v)
    }
  }

  def handleInvoke(expr : InvokeExpr, destAddr : Option[Addr]) : Set[State] = {
    expr match {
      case inv : StaticInvokeExpr => {
        val args = inv.getArgs
        val methRef = inv.getMethodRef
        val cls = methRef.declaringClass
        val meth = cls.getMethod(methRef.name, methRef.parameterTypes, methRef.returnType)
        val statements = meth.getActiveBody().getUnits()
        val newStmt = Stmt(statements.getFirst, meth, stmt.program)
        val newFP = alloca()
        var i = 0
        var newStore = store
        for (a <- args) {
          val addr = ParameterFrameAddr(newFP, i)
          val d = eval(a, fp, store)
          newStore = newStore.update(addr, d)
        }
        val newKontStack = kontStack.push(Frame(stmt.next_syntactic, newFP, destAddr))
        Set(State(newStmt, newFP, newStore, newKontStack))
      }
    }
  }

  def next() : Set[State] = {
    stmt.unit match {
      case unit : IdentityStmt => {
        val lhs_addr = LocalFrameAddr(fp, unit.getLeftOp().asInstanceOf[Local])
        val rhs_addr = ParameterFrameAddr(fp, unit.getRightOp().asInstanceOf[ParameterRef].getIndex())
        val new_store = store.update(lhs_addr, store(rhs_addr))
        Set(State(stmt.next_syntactic(), fp, new_store, kontStack))
      }
      case unit : InvokeStmt => {
        handleInvoke(unit.getInvokeExpr, None)
      }

      case unit : IfStmt => {
        val trueState = State(Stmt(unit.getTarget(), stmt.method, stmt.program),
                              fp, store, kontStack)
        val falseState = State(stmt.next_syntactic(), fp, store, kontStack)
        Set(trueState, falseState)
      }

      case unit : AssignStmt => {
        val lhs_addr = LocalFrameAddr(fp, unit.getLeftOp().asInstanceOf[Local])

        unit.getRightOp() match {
          case expr : InvokeExpr => handleInvoke(expr, Some(lhs_addr))
          case _ => {
            val evaled_rhs = eval(unit.getRightOp(), fp, store)
            val newStore = store.update(lhs_addr, evaled_rhs)
            Set(State(stmt.next_syntactic(), fp, newStore, kontStack))
          }
        }
      }

      case unit : ReturnStmt => {
        val evaled = eval(unit.getOp(), fp, store)
        for ((frame, newStack) <- kontStack.pop) yield {
          val newStore = if (frame.acceptsReturnValue()) {
            store.update(frame.destAddr.get, evaled)
          } else {
            store
          }

          State(frame.stmt, frame.fp, newStore, newStack)
        }
      }

      case unit : ReturnVoidStmt => {
        for ((frame, newStack) <- kontStack.pop if !(frame.acceptsReturnValue()))
            yield State(frame.stmt, frame.fp, store, newStack)
      }

      // Since Soot's NopEliminator run before us, no "nop" should be
      // left in the code and this case isn't needed (and also is
      // untested).  The one place a "nop" could occur is as the last
      // instruction of a method that is also the instruction after
      // the end of a "try" clause. (See NopEliminator for the exact
      // conditions.) However, that would not be an executable
      // instruction, so we still wouldn't need this case.
      case unit : NopStmt => Set(State(stmt.next_syntactic, fp, store, kontStack))

      case unit : GotoStmt => Set(State(Stmt(unit.getTarget(), stmt.method, stmt.program), fp, store, kontStack))

      // We're missing DefinitionStmt and SwitchStmt.

      case _ => {
        throw new Exception("No match for " + stmt.unit.getClass + " : " + stmt.unit)
      }
    }
  }
}

object State {
  def inject(stmt : Stmt) : State = {
    val initial_map : Map[Addr, D] = Map((ParameterFrameAddr(InvariantFramePointer, 0) -> D.atomicTop))
    State(stmt, InvariantFramePointer, Store(initial_map), KontStack(KontStore(Map()), HaltKont))
  }
}

object Main {
  def main(args : Array[String]) {
    // TODO: proper option parsing
    if (args.length != 3) println("Expected arguments: [classDirectory] [className] [methodName]")
    val classDirectory = args(0)
    val className = args(1)
    val methodName = args(2)

    val source = SootWrapper.fromClasses(classDirectory, "")
    val classes = getClassMap(source.getShimple())

    val mainMainMethod = classes(className).getMethodByName(methodName);
    val units = mainMainMethod.getActiveBody().getUnits();

    val first = units.getFirst()

    val initialState = State.inject(Stmt(first, mainMainMethod, classes))
    var todo : List [State] = List(initialState)
    var seen : Set [State] = Set()
    while (todo nonEmpty) {
      val current = todo.head
      println(current);
      val nexts = current.next
      // TODO: Fix optimization bug here
      todo = nexts.toList.filter(!seen.contains(_)) ++ todo.tail
      seen = seen ++ nexts
    }
  }

  def getClassMap(classes : Chain[SootClass]) : Map[String, SootClass]= {
    var map : Map[String, SootClass] = Map()
    for (c <- classes) {
      map = map + ((c.getName(), c))
    }

    map
  }
}
