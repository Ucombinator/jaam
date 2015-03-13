package org.ucombinator.analyzer

/*
  Warning:

  1. Don't cross the streams.  It would be bad.

  2. Don't order addresses.  Dogs and cats, living together.  Mass hysteria.
*/

import org.ucombinator.SootWrapper
import scala.collection.JavaConversions._
import scala.language.postfixOps
import soot.util.Chain
import soot.SootClass
import soot.SootField
import soot.SootMethod    

// We expect every Unit we use to be a soot.jimple.Stmt, but the APIs
// are built around using Unit so we stick with that.  (We may want to
// fix this when we build the Scala wrapper for Soot.)
import soot.{Unit => SootUnit}
import soot.jimple.{Stmt => SootStmt}

import soot.Local
import soot.{Value => SootValue}
import soot.IntType
import soot.jimple._

abstract class FramePointer

abstract class BasePointer

class ConcreteFramePointer() extends FramePointer

class ConcreteBasePointer() extends BasePointer

object InvariantFramePointer extends FramePointer

object InvariantBasePointer extends BasePointer

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
  val destAddr : Option[Set[Addr]]) {

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

case class ObjectValue(val sootClass : SootClass,  val bp : BasePointer) extends Value

abstract class Addr

abstract class FrameAddr extends Addr

case class Store(private val map : Map[Addr, D]) {
  def update(addr : Addr, d : D) : Store= {
    map.get(addr) match {
      case Some(oldd) => Store(map + (addr -> oldd.join(d)))
      case None => Store(map + (addr -> d))
    }
  }
  def update(addrs : Set[Addr], d : D) : Store= {
     var newStore = this
     for (a <- addrs) {
       newStore = newStore.update(a, d)
     }
     newStore
  }

  def apply(addr : Addr) : D = map(addr)
  def apply(addrs : Set[Addr]) : D = {
    val ds = for (a <- addrs)
               yield map(a)
    ds.fold (D(Set()))(_ join _)
  }
  def get(addr : Addr) : Option[D] = map.get(addr)
}

case class LocalFrameAddr(val fp : FramePointer, val register : Local) extends FrameAddr

case class ParameterFrameAddr(val fp : FramePointer, val parameter : Int) extends FrameAddr

case class ThisFrameAddr(val fp : FramePointer) extends FrameAddr

case class FieldAddr(val bp : BasePointer, val sf : SootField) extends Addr

case class Stmt(val unit : SootUnit, val method : SootMethod, val program : Map[String, SootClass]) {
  assert(unit.isInstanceOf[SootStmt])
  def nextTarget(unit : SootUnit) : Stmt = Stmt(unit, method, program)
  def nextSyntactic() : Stmt = nextTarget(method.getActiveBody().getUnits().getSuccOf(unit))
  override def toString() : String = unit.toString()
}

case class State(stmt : Stmt, fp : FramePointer, store : Store, kontStack : KontStack) {
  def alloca() : FramePointer = InvariantFramePointer

  def addrOf(v : SootValue, fp : FramePointer, store : Store) : Addr = {
    v match {
      case local : Local => LocalFrameAddr(fp, local)
      case v : ParameterRef => ParameterFrameAddr(fp, v.getIndex())
      case v : ThisRef => ThisFrameAddr(fp)
    }
  }

  def addrsOf(sv : SootValue, fp : FramePointer, store : Store) : Set[Addr] = {
    sv match {       
      case sv : InstanceFieldRef => {
	val b = sv.getBase()
	val d = eval(b, fp, store)
	// TODO/optimize
	// filter out incorrect class types
	for (v <- d.values if v.isInstanceOf[ObjectValue])
	 yield FieldAddr(v.asInstanceOf[ObjectValue].bp, sv.getField())      
      }
      case _ => Set(addrOf(sv, fp, store))
    }    
  }

  def eval(v: SootValue, fp : FramePointer, store : Store) : D = {
    v match {
      case i : InstanceFieldRef => store(addrsOf(v, fp, store))
      case (_ : Local) | (_ : Ref) => store(addrOf(v, fp, store))

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

      case _ =>  throw new Exception("No match for " + v.getClass + " : " + v)
    }
  }

  def handleInvoke(expr : InvokeExpr, destAddr : Option[Set[Addr]]) : Set[State] = {
    expr match {
      case inv : SpecialInvokeExpr => {
	val methRef = inv.getMethodRef
	val cls = methRef.declaringClass
        val meth = cls.getMethod(methRef.name, methRef.parameterTypes, methRef.returnType)
        val statements = meth.getActiveBody().getUnits()
        val newStmt = Stmt(statements.getFirst, meth, stmt.program)
        val newFP = alloca()
        var newStore = store
        for (i <- 0 until inv.getArgCount())
          newStore = newStore.update(ParameterFrameAddr(newFP, i), eval(inv.getArg(i), fp, store))
	val th = ThisFrameAddr(newFP)
	val sootValue = inv.getBase()
	val d = eval(sootValue, fp, store)
	// TODO/optimize
	// filter out incorrect class types	      
	for (v <- d.values)
	  v match {
            case ObjectValue(sootClass, _) => newStore = newStore.update(th, D(Set(v)))
            case _ => {}
	  }
        val newKontStack = kontStack.push(Frame(stmt.nextSyntactic(), newFP, destAddr))
        Set(State(newStmt, newFP, newStore, newKontStack))
      }
      case inv : StaticInvokeExpr => {
        val methRef = inv.getMethodRef
        val cls = methRef.declaringClass
        val meth = cls.getMethod(methRef.name, methRef.parameterTypes, methRef.returnType)
        val statements = meth.getActiveBody().getUnits()
        val newStmt = Stmt(statements.getFirst, meth, stmt.program)
        val newFP = alloca()
        var newStore = store
        for (i <- 0 until inv.getArgCount())
          newStore = newStore.update(ParameterFrameAddr(newFP, i), eval(inv.getArg(i), fp, store))
        val newKontStack = kontStack.push(Frame(stmt.nextSyntactic(), newFP, destAddr))
        Set(State(newStmt, newFP, newStore, newKontStack))
      }
    }
  }

  def next() : Set[State] = {
    stmt.unit match {
      case unit : InvokeStmt => handleInvoke(unit.getInvokeExpr, None)

      case unit : DefinitionStmt => {
        val lhsAddr = addrsOf(unit.getLeftOp(), fp, store)

        unit.getRightOp() match {
          case rhs : InvokeExpr => handleInvoke(rhs, Some(lhsAddr))	  
	  case rhs : NewExpr => {
	    val baseType = rhs.getBaseType()
	    val sootClass = baseType.getSootClass()
	    val bp = InvariantBasePointer // TODO turn this into malloc
	    val obj = ObjectValue(sootClass, bp)
	    val d = D(Set(obj))
            val newStore = store.update(lhsAddr, d)
	    Set(State(stmt.nextSyntactic(), fp, newStore, kontStack))
	  }
          case rhs => {
            val evaledRhs = eval(rhs, fp, store)
            val newStore = store.update(lhsAddr, evaledRhs)
            Set(State(stmt.nextSyntactic(), fp, newStore, kontStack))
          }
        }
      }

      case unit : IfStmt => {
        val trueState = State(stmt.nextTarget(unit.getTarget()), fp, store, kontStack)
        val falseState = State(stmt.nextSyntactic(), fp, store, kontStack)
        Set(trueState, falseState)
      }

      // TODO: needs testing
      case unit : SwitchStmt =>
        unit.getTargets().map(t => State(stmt.nextTarget(t), fp, store, kontStack)).toSet

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
      //
      // If we ever need the code for this, it would probably be:
      //   Set(State(stmt.nextSyntactic(), fp, store, kontStack))
      case unit : NopStmt => throw new Exception("Impossible statement: " + unit)

      case unit : GotoStmt => Set(State(stmt.nextTarget(unit.getTarget()), fp, store, kontStack))

      // We're missing BreakPointStmt, MonitorStmt, RetStmt, and ThrowStmt.

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

  def getClassMap(classes : Chain[SootClass]) : Map[String, SootClass] = 
    (for (c <- classes) yield c.getName() -> c).toMap
}
