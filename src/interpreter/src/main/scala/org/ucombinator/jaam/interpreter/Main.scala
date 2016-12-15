package org.ucombinator.jaam.interpreter

/*
  Warning:

  1. Don't cross the streams.  It would be bad.

  2. Don't order addresses.  Dogs and cats, living together.  Mass hysteria.
*/

// TODO: need to track exceptions that derefing a Null could cause
// TODO: invoke main method instead of jumping to first instr so static init is done correctly
// TODO: invoke main method so we can initialize the parameters to main

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.io.Source

import java.io.FileOutputStream

// We expect every Unit we use to be a soot.jimple.Stmt, but the APIs
// are built around using Unit so we stick with that.  (We may want to
// fix this when we build the Scala wrapper for Soot.)
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}

import soot.jimple.internal.JStaticInvokeExpr
import soot.jimple.toolkits.invoke.AccessManager
import soot.tagkit._

import org.ucombinator.jaam.serializer
import org.ucombinator.jaam.interpreter.Stmt.unitToStmt // Automatically convert soot.Unit to soot.Stmt

// TODO: some sets could just be lists until we sort them out at the end
// TODO: `union` might be more efficient than `_++_`
/*
TODO: use FastSet to enforce use of CachedHashCode
class FastSet[T <: CachedHashCode] extends Set[T]
 */
trait CachedHashCode extends Product {
  override lazy val hashCode = scala.runtime.ScalaRunTime._hashCode(this)
}

// Possibly thrown during transition between states.
case class UninitializedClassException(sootClass : SootClass) extends RuntimeException
case class StringConstantException(string : String) extends RuntimeException
case class UndefinedAddrsException[A <: Addr](addrs : Set[A]) extends RuntimeException

// A continuation store paired with a continuation
case class KontStack(k : Kont) extends CachedHashCode {
  // TODO/generalize: Add widening in push and pop (to support PDCFA)
  // TODO/generalize: We need the control pointer of the target state to HANDLE PDCFA.
  def kalloca(frame : Frame) = OneCFAKontAddr(frame.fp)

  // TODO/generalize: Handle PDCFA
  def push(frame : Frame) : KontStack = {
    val kAddr = kalloca(frame)
    System.kstore.update(kAddr, KontD(Set(k)))
    KontStack(RetKont(frame, kAddr))
  }

  // Pop returns all possible frames beneath the current one.
  def pop() : Set[(Frame, KontStack)] = {
    k match {
      case RetKont(frame, kontAddr) =>
        for (topk <- System.kstore(kontAddr).values) yield {
          val ks = KontStack(topk)
          (frame, ks)
        }
      case HaltKont => Set()
    }
  }

  def handleException(exception : Value,
                      stmt : Stmt,
                      fp : FramePointer
                    ) : Set[AbstractState] = {
    //TODO: if the JVM implementation does not enforce the rules on structured locking described in §2.11.10,
    //then if the method of the current frame is a synchronized method and the current thread is not the owner
    //of the monitor entered or reentered on invocation of the method, athrow throws an IllegalMonitorStateException
    //instead of the object previously being thrown.
    if (!exception.isInstanceOf[ObjectValue]) {
      Log.warn("Impossible throw: stmt = " + stmt + "; value = " + exception + ". May be unsound.")
      if (exception == AnyAtomicValue) {
        // if throws something might be a null pointer, then throw a NullPointerException
        val nullPointerException = Soot.getSootClass("java.lang.NullPointerException");
        return handleException(ObjectValue(nullPointerException, OneCFABasePointer(stmt, fp)), stmt, fp)
      }
      return Set()
    }

    /*
    val currentFrame = Frame(stmt, fp, None)
    var states: Set[AbstractState] = Set()
    var nextFrames: Set[(Frame, KontStack)] = Set((currentFrame, this))

    while (nextFrames.nonEmpty) {
      val (frame, kontStack) = nextFrames.last
      val (stmt, fp) = (frame.stmt, frame.fp)
      for (trap <- TrapManager.getTrapsAt(stmt.sootStmt, Soot.getBody(stmt.sootMethod))) {
        val caughtType = trap.getException
        if (Soot.canStoreClass(exception.asInstanceOf[ObjectValue].sootClass, caughtType)) {
          System.store.update(CaughtExceptionFrameAddr(fp), D(Set(exception)))
          states += State(stmt.copy(sootStmt = trap.getHandlerUnit), fp, kontStack)
        }
        else {
          val fs = kontStack.pop
          if (fs.isEmpty) states += ErrorState
          else nextFrames ++= fs
        }
      }
      nextFrames -= ((frame, kontStack))
    }
    Log.debug("exception: " + exception + " states.size: " + states.size)
    states
    */

    var visited = Set[(Stmt, FramePointer, KontStack)]()
    // TODO/performance: Make iterative.
    def stackWalk(stmt : Stmt, fp : FramePointer, kontStack : KontStack) : Set[AbstractState] = {
      if (visited.contains((stmt, fp, kontStack))) return Set()
      visited = visited + ((stmt, fp, kontStack)) // TODO: do we really need all of these in here?
      for (trap <- TrapManager.getTrapsAt(stmt.sootStmt, Soot.getBody(stmt.sootMethod))) {
        val caughtType = trap.getException()
        // The handler will expect the exception to be waiting at CaughtExceptionFrameAddr(fp).
        // It'll be referenced through CaughtExceptionRef.

        System.store.update(CaughtExceptionFrameAddr(fp), D(Set(exception)))
        // TODO/soundness or performance?: use Hierarchy or FastHierarchy?
        if (Soot.canStoreClass(exception.asInstanceOf[ObjectValue].sootClass, caughtType)) {
          return Set(State(stmt.copy(sootStmt = trap.getHandlerUnit()), fp, kontStack.copy()))
        }
      }

      // TODO/interface we should log this sort of thing
      val nextFrames = kontStack.pop()
      if (nextFrames.isEmpty)
        return Set(ErrorState)

      (for ((frame, kontStack) <- nextFrames) yield { stackWalk(frame.stmt, frame.fp, kontStack) }).flatten
    }

    stackWalk(stmt, fp, this)
    // TODO/soundness: deal with unhandled exceptions
  }
}

case class Frame( val stmt : Stmt,
                  val fp : FramePointer,
                  val destAddr : Option[Set[Addr]]) extends CachedHashCode {
  def acceptsReturnValue() : Boolean = !destAddr.isEmpty
}

abstract class Kont extends CachedHashCode

case class RetKont( val frame : Frame,
                    val k : KontAddr
                  ) extends Kont

case object HaltKont extends Kont

//----------- ABSTRACT VALUES -----------------

// TODO/precision D needs to have an interface that allows eval to use it

abstract class Value extends CachedHashCode
object Value {
  def canStoreType(child : Value, parent : Type): Boolean = {
    child match {
      case AnyAtomicValue => true /* AnyAtomicValue includes null which can go in any object or array type */
      case ObjectValue(sootClass, _) => Soot.canStoreType(sootClass.getType, parent)
      case ArrayValue(sootType, _) => Soot.canStoreType(sootType, parent)
    }
  }
}

abstract class AtomicValue extends Value

case object AnyAtomicValue extends AtomicValue

case class ObjectValue(val sootClass : SootClass, val bp : BasePointer) extends Value

// The sootType is the type with array wrapper
case class ArrayValue(val sootType : Type, val bp : BasePointer) extends Value

//case class SnowflakeInterfaceValue(val sootClass : SootClass, val bp : BasePointer) extends Value

//----------------- POINTERS ------------------

// FramePointers, when paired with variable names, yield the addresses of variables.
abstract class FramePointer extends CachedHashCode
case object InvariantFramePointer extends FramePointer // TODO: delete this?  Is seems unused
case class ZeroCFAFramePointer(val method : SootMethod) extends FramePointer
case class OneCFAFramePointer(val method : SootMethod, val nextStmt : Stmt) extends FramePointer
case object InitialFramePointer extends FramePointer

abstract class From
case object FromNative extends From
case object FromJava extends From

// BasePointers, when paired with field names, yield the addresses of fields.
abstract class BasePointer
case class OneCFABasePointer(stmt : Stmt, fp : FramePointer) extends BasePointer
case object InitialBasePointer extends BasePointer
// Note that due to interning, strings and classes may share base pointers with each other
// Oh, and class loaders are a headache(!)
case class StringBasePointer(val string : String) extends BasePointer {
  // Use escape codes (e.g., `\n`) in the string.  We do this by getting a
  // representation of a string constant and then printing that.
//TODO:  override def toString = "X"
  override lazy val toString = {
    import scala.reflect.runtime.universe._
    "StringBasePointer(" + Literal(Constant(string)).toString + ")"
  }
}

case object StringBasePointerTop extends BasePointer

// we remvoe the argument of ClassBasePointer, to make all ClassBasePointer points to the same
case class ClassBasePointer(val name : String) extends BasePointer
//case object ClassBasePointer extends BasePointer

//----------------- ADDRESSES ------------------

// Addresses of continuations on the stack
abstract class KontAddr extends Addr
case class OneCFAKontAddr(val fp : FramePointer) extends KontAddr

abstract class Addr extends CachedHashCode
abstract class FrameAddr extends Addr

case object GlobalSnowflakeAddr extends Addr

// For local variables
case class LocalFrameAddr(val fp : FramePointer, val register : Local) extends FrameAddr

case class ParameterFrameAddr(val fp : FramePointer, val parameter : Int) extends FrameAddr

case class ThisFrameAddr(val fp : FramePointer) extends FrameAddr

// Holds the most recently caught exception in this frame
case class CaughtExceptionFrameAddr(val fp : FramePointer) extends FrameAddr

case class InstanceFieldAddr(val bp : BasePointer, val field : SootField) extends Addr

case class ArrayRefAddr(val bp : BasePointer) extends Addr // TODO: add index (it is all AtomicValue anyway)

case class ArrayLengthAddr(val bp : BasePointer) extends Addr

case class StaticFieldAddr(val field : SootField) extends Addr

// TODO: replace KontD and D with AbstractDomain[...]
// TODO: cache hashcode for Kont and Value

case class D(private val values: Set[Value]) {
  def getValues: Set[Value] = values
  def join(that : D) = D(this.getValues ++ that.getValues)
  def maybeZero() : Boolean = getValues.exists(_.isInstanceOf[AtomicValue])
}
object D {
  val atomicTop = D(Set(AnyAtomicValue))
}

object GlobalD extends D(Set[Value]()) {
  val globalValues: mutable.Set[Value] = mutable.Set[Value]()
  val map = mutable.Map[SootClass, mutable.Set[Value]]()
  var modified: Boolean = false

  var size: Int = 0
  var cachedValues: Set[Value] = Set[Value]()

  override def getValues: Set[Value] = {
    if (size != globalValues.size) {
      cachedValues = globalValues.toSet
      size = cachedValues.size
    }
    cachedValues
  }

  def update(vs: Set[Value]): D = {
    val oldSize = globalValues.size
    globalValues ++= vs
    modified = (oldSize != globalValues.size)
    this
  }

  // Not being used
  def get(baseClass: SootClass): Set[Value] = {
    val newValues = map.get(baseClass) match {
      case None =>
        val valuesOfSootClass = globalValues.filter(_ match {
          case ObjectValue(sootClass, bp) => Soot.canStoreClass(sootClass, baseClass)
          case _ => false
        })
        map += (baseClass -> valuesOfSootClass)
        valuesOfSootClass
      case Some(v) => v
    }
    newValues.toSet
  }
}

case class KontD(val values: Set[Kont]) {
  def join(that : KontD) = KontD(this.values ++ that.values)
}

abstract sealed class AbstractState {
  def next() : Set[AbstractState]

  def getReadAddrs: Set[Addr] = Set()
  def setReadAddrs(s: Set[Addr]) = {}
  def getKReadAddrs: Set[KontAddr] = Set()
  def setKReadAddrs(s: Set[KontAddr]) = {}
  def getWriteAddrs: Set[Addr] = Set()
  def setWriteAddrs(s: Set[Addr]) = {}
  def getKWriteAddrs: Set[KontAddr] = Set()
  def setKWriteAddrs(s: Set[KontAddr]) = {}

  def toPacket() : serializer.AbstractState

  val id = AbstractState.idMap.getOrElseUpdate(this, AbstractState.nextId)
}

object AbstractState {
  type Id = Int
  val idMap = mutable.Map[AbstractState, Id]()
  var nextId_ = 0
  def nextId() : Id = { nextId_ += 1; nextId_ }
}

case object ErrorState extends AbstractState {
  override def next() : Set[AbstractState] = Set.empty
  override def toPacket() = serializer.ErrorState(serializer.Id[serializer.Node](id))
}

// State abstracts a collection of concrete states of execution.
case class State(val stmt : Stmt,
                 val fp : FramePointer,
                 val kontStack : KontStack) extends AbstractState with CachedHashCode {
  // Needed because different "stores" should lead to different objects
  //  override def equals(that: Any): Boolean =
  //    that match {
  //      case that: State => this.stmt == that.stmt && this.fp == that.fp && this.kontStack == that.kontStack && this.store == that.store
  //      case _ => false
  //   }

  var readAddrs = Set[Addr]()
  override def getReadAddrs = readAddrs
  override def setReadAddrs(s: Set[Addr]) = readAddrs = s

  var kReadAddrs = Set[KontAddr]()
  override def getKReadAddrs = kReadAddrs
  override def setKReadAddrs(s: Set[KontAddr]) = kReadAddrs = s

  var writeAddrs = Set[Addr]()
  override def getWriteAddrs = writeAddrs
  override def setWriteAddrs(s: Set[Addr]) = writeAddrs = s

  var kWriteAddrs = Set[KontAddr]()
  override def getKWriteAddrs = kWriteAddrs
  override def setKWriteAddrs(s: Set[KontAddr]) = kWriteAddrs = s

  override def toPacket() = serializer.State(serializer.Id[serializer.Node](id), stmt.toPacket, fp.toString, kontStack.toString)

  // When you call next, you may realize you are going to end up throwing some exceptions
  // TODO/refactor: Consider turning this into a list of exceptions.
  var exceptions = D(Set())

  // Allocates a new frame pointer (currently uses 0CFA)
  def alloca(expr : InvokeExpr, nextStmt : Stmt) : FramePointer = ZeroCFAFramePointer(expr.getMethod)
  // Allocates objects
  def malloc() : BasePointer = OneCFABasePointer(stmt, fp)
  //def mallocFromNative() : BasePointer = OneCFABasePointer(stmt, fp)

  // Returns all possible addresses of an assignable expression.
  // x = 3; // Should only return 1 address.
  // x[2] = 3; // May return more than one address because x might be multiple arrays.
  // x.y = 3; // May return multiple addresses
  // x[y] = 3; // May return multiple addresses even if x is a single value. (not currently relevant)
  // Right now, we model arrays as having only a single storage location, so the last example produces multiple addresses only based on there being more than one location for x.
  def addrsOf(lhs : SootValue) : Set[Addr] = {
    lhs match {
      case lhs : Local => Set(LocalFrameAddr(fp, lhs))
      case lhs : InstanceFieldRef =>
        val b : SootValue = lhs.getBase // the x in x.y
        val baseSootClass = b.getType match {
          case rt : RefType => rt.getSootClass
          case _ => throw new RuntimeException("non-ref type can not have InstanceFieldRef")
        }
        val d : D = eval(b)
        // TODO/bug
        // Suppose a number flows to x in x.y = 3;
        for (ObjectValue(sc, bp) <- d.getValues if Soot.canStoreClass(sc, baseSootClass))
          yield InstanceFieldAddr(bp, lhs.getField)
      case lhs : StaticFieldRef =>
        val f : SootField = lhs.getField
        val c : SootClass = f.getDeclaringClass
        System.checkInitializedClasses(c)
        Set(StaticFieldAddr(f))
      case lhs : ParameterRef => Set(ParameterFrameAddr(fp, lhs.getIndex))
      case lhs : ThisRef => Set(ThisFrameAddr(fp))
      // TODO/precision: have multiple addresses per "catch" clause?
      // Perhaps mix left-hand side into address?
      case lhs : CaughtExceptionRef => Set(CaughtExceptionFrameAddr(fp))
      case lhs : ArrayRef =>
        // TODO/precision: Use more than a single address for each Array.
        val b = eval(lhs.getBase)
        // TODO/soundness: array ref out of bounds exception
        val i = eval(lhs.getIndex) // Unused but evaled in case we trigger a <clinit> or exception
        // TODO: filter out incorrect types
        for (ArrayValue(_, bp) <- b.getValues) yield
          ArrayRefAddr(bp)
    }
  }

  // Abstractly evaluate a Soot expression.
  // x.f
  // 3
  // a + b
  // We cheat slightly here, by using side-effects to install exceptions that need to be thrown.
  // This does not evaluate complex expressions like method-calls.
  // TODO/soundness: properly initialize exceptions
  def eval(v: SootValue) : D = {
    def assertNumeric(op : SootValue) {
      assert(op.getType.isInstanceOf[PrimType] && !op.getType.isInstanceOf[BooleanType])
    }
    def assertIntegral(op : SootValue) {
      assert(op.getType.isInstanceOf[PrimType] &&
        !op.getType.isInstanceOf[BooleanType] &&
        !op.getType.isInstanceOf[FloatType] &&
        !op.getType.isInstanceOf[DoubleType])
    }
    def assertLogical(op : SootValue) {
      assert(op.getType.isInstanceOf[PrimType] &&
        !op.getType.isInstanceOf[FloatType] &&
        !op.getType.isInstanceOf[DoubleType])
    }
    val result = v match {
      //TODO missing: MethodHandle
      //TODO/precision actually do the calculations
      case (_ : Local) | (_ : Ref) =>
        D(System.store(addrsOf(v)).getValues.filter(Value.canStoreType(_, v.getType())))
      case _ : NullConstant =>
        D.atomicTop
      case _ : NumericConstant => D.atomicTop
      // TODO: Class and String objects are objects and so need their fields initialized
      // TODO/clarity: Add an example of Java code that can trigger this.
      case v : ClassConstant => D(Set(ObjectValue(Soot.classes.Class, ClassBasePointer(v.value.replace('/', '.')))))
      //D(Set(ObjectValue(stmt.classmap("java.lang.Class"), StringBasePointer(v.value))))
      case v : StringConstant =>
        D(Set(ObjectValue(Soot.classes.String, StringBasePointerTop)))
        /*
        if (System.store.contains(InstanceFieldAddr(bp, Soot.classes.String.getFieldByName("value")))) {
          D(Set(ObjectValue(Soot.classes.String, bp)))
        } else {
          throw StringConstantException(v.value)
        }
        */
      case v : NegExpr => D.atomicTop
      case v : BinopExpr =>
        v match {
          case (_ : EqExpr) | (_ : NeExpr) | (_ : GeExpr) | (_ : GtExpr) | (_ : LeExpr) | (_ : LtExpr) |
               (_ : CmpExpr) | (_ : CmpgExpr) | (_ : CmplExpr) =>
            eval(v.getOp1)
            eval(v.getOp2)
            D.atomicTop
          case (_ : ShrExpr) | (_ : ShlExpr) | (_ : UshrExpr) =>
            assertIntegral(v.getOp1)
            assertIntegral(v.getOp2)
            eval(v.getOp1)
            eval(v.getOp2)
            D.atomicTop
          case (_ : RemExpr) =>
            assertNumeric(v.getOp1) // floats are allowed
            assertNumeric(v.getOp2)
            eval(v.getOp1)
            eval(v.getOp2)
            D.atomicTop
          case (_ : XorExpr) | (_ : OrExpr) | (_ : AndExpr) =>
            assertLogical(v.getOp1)
            assertLogical(v.getOp2)
            eval(v.getOp1)
            eval(v.getOp2)
            D.atomicTop
          case (_ : AddExpr) | (_ : SubExpr) | (_ : MulExpr) | (_ : DivExpr) =>
            assertNumeric(v.getOp1)
            assertNumeric(v.getOp2)
            eval(v.getOp1)
            val zCheck = eval(v.getOp2)
            if(v.isInstanceOf[DivExpr] && zCheck.maybeZero()){
              // TODO/soundness: No malloc!
              exceptions = exceptions.join(D(Set(ObjectValue(Soot.classes.ArithmeticException, malloc()))))
            }
            D.atomicTop
        }

      // Every array has a distinguished field for its address.
      case v : LengthExpr =>
        val addrs : Set[Addr] = for {
          ArrayValue(_, bp) <- eval(v.getOp).getValues
        } yield ArrayLengthAddr(bp)
        D(System.store(addrs).getValues.filter(Value.canStoreType(_, v.getType())))

      // TODO/precision: implement the actual check
      case v : InstanceOfExpr => D.atomicTop

      case v : CastExpr =>
        // TODO: cast from a SnowflakeObject to another SnowflakeObject
        val castedExpr: SootValue = v.getOp
        //val fromType: Type = castedExpr.getType
        val castedType: Type = v.getCastType
        System.checkInitializedClasses(castedType)
        val d = eval(castedExpr)

        // TODO: filter out elements of "d" that are not of the
        // expression's type (should be done in general inside "eval"
        // (and maybe already is))
        def isCastableTo(v : Value, t : Type) : Boolean = {
          v match {
            case _ : AtomicValue => t.isInstanceOf[PrimType]
            case ObjectValue(sootClass, _) =>
              t match {
                case rt : RefType => Soot.canStoreClass(sootClass, rt.getSootClass)
                case _ => false
              }
            case ArrayValue(sootType, _) => Soot.canStoreType(sootType, t)
          }
        }
        var d2 = D(Set())
        for (v <- d.getValues) {
          if (isCastableTo(v, castedType) || castedType.isInstanceOf[ArrayType]) {
            // Up casts are always legal
            d2 = d2.join(D(Set((v))))
          }
          else if (Snowflakes.isSnowflakeObject(v)) {
            // && Soot.canStoreType(castedType, v.asInstanceOf[ObjectValue].sootClass.getType)) {

            val classCastException = D(Set(ObjectValue(Soot.classes.ClassCastException, malloc())))
            exceptions = exceptions.join(classCastException)
            d2 = d2.join(D(Set(v)))

            /*
            // Snowflakes can be down cast, but they might throw an exception
            val classCastException = D(Set(ObjectValue(Soot.classes.ClassCastException, malloc())))
            exceptions = exceptions.join(classCastException)
            val castedClass = castedType match {
              case r : RefType => r.getSootClass
              case a : ArrayType => ???
            }

            if (System.isLibraryClass(castedClass)) {
              // Snowflake object cast to Snowflake object
              //Log.error("casting snowflake object " + v.asInstanceOf[ObjectValue].castedClass.getName + " to " + sootClass.getName)
              val bp = Snowflakes.malloc(castedClass)
              d2 = d2.join(D(Set(ObjectValue(castedClass, bp))))
              Snowflakes.createObject(None, castedClass)
            }
            else {
              // Snowflake object cast to non-Snowflake object
              // TODO: if castedType is not from Java library, then we need to instantiated one
              // TODO: call <init>, interface/abstract class
              d2 = d2.join(Snowflakes.createObjectOrThrow(castedClass.getName))
            }
          }
          else {
            // Anything else would cause an exception
            val classCastException = D(Set(ObjectValue(Soot.classes.ClassCastException, malloc())))
            exceptions = exceptions.join(classCastException)
          }
          */
          }
        }
        d2
      case _ =>  throw new Exception("No match for " + v.getClass + " : " + v)
    }
    result
  }

  // The last parameter of handleInvoke allows us to override what
  // Stmt to execute after returning from this call.  We need this for
  // static class initialization because in that case we want to
  // return to the current statement instead of the next statement.
  def handleInvoke(expr : InvokeExpr,
                   destAddr : Option[Set[Addr]],
                   nextStmt : Stmt = stmt.nextSyntactic) : Set[AbstractState] = {
    val base = expr match {
      // TODO: Could only come from non-Java sources
      case expr : DynamicInvokeExpr => ???
      case expr : StaticInvokeExpr => None
      case expr : InstanceInvokeExpr =>
      // SpecialInvokeExpr, <init>, private methods, and methods of superclass
      // InterfaceInvokeExpr
      // VirtualInvokeExpr
        val d = eval(expr.getBase)
        Some((d, expr.isInstanceOf[SpecialInvokeExpr]))
    }
    val method = expr.getMethod
    val args = expr.getArgs.toList map eval
    val newFP = alloca(expr, nextStmt)

    handleInvoke2(base, method, args, newFP, destAddr, nextStmt)
  }

  // The last parameter of handleInvoke allows us to override what
  // Stmt to execute after returning from this call.  We need this for
  // static class initialization because in that case we want to
  // return to the current statement instead of the next statement.
  def handleInvoke2(base : Option[(D, Boolean)],
                   method : SootMethod,
                   args : List[D],
                   newFP : FramePointer,
                   destAddr : Option[Set[Addr]],
    // TODO: use `Option` for nextStmt
                   nextStmt : Stmt = stmt.nextSyntactic) : Set[AbstractState] = {
    // This function finds all methods that could override root_m.
    // These methods are returned with the root-most at the end of
    // the list and the leaf-most at the head.  Thus the caller
    // should use the head of the returned list.  The reason a list
    // is returned is so this function can recursively compute the
    // transitivity rule in Java's method override definition.
    //
    // Note that Hierarchy.resolveConcreteDispath should be able to do this, but seems to be implemented wrong
    def overrides(curr : SootClass, root_m : SootMethod) : List[SootMethod] = {
      Log.debug("curr: " + curr.toString)
      val curr_m = curr.getMethodUnsafe(root_m.getName, root_m.getParameterTypes, root_m.getReturnType)
      if (curr_m == null) {
        Log.debug("root_m: " + root_m.toString)
        overrides(curr.getSuperclass(), root_m)
      }
      else if (root_m.getDeclaringClass.isInterface || AccessManager.isAccessLegal(curr_m, root_m)) { List(curr_m) }
      else {
        val o = overrides(curr.getSuperclass(), root_m)
        (if (o.exists(m => AccessManager.isAccessLegal(curr_m, m))) List(curr_m) else List()) ++ o
      }
    }

    // o.f(3);
    // In this case, c is the type of o. m is f. receivers is the result of eval(o).
    // TODO/dragons. Here they be.
    def dispatch(self : Option[Value], meth : SootMethod) : Set[AbstractState] = {
      // TODO: group multiple "self" together
      //Log.info("dispatch: " + self + ":" + meth)
      Snowflakes.get(meth) match {
        case Some(h) => h(this, nextStmt, self, args)
        case None =>
          /* (!meth.getDeclaringClass.getPackageName.startsWith("com.sun.net.httpserver") || meth.isAbstract()) || */
          /*
          if (System.isLibraryClass(meth.getDeclaringClass) ||
              self.isDefined && Snowflakes.isSnowflakeObject(self.get)) {
            Snowflakes.warn(this.id, self, stmt, meth)
            //if (meth.getDeclaringClass.getPackageName.startsWith("com.sun.net.httpserver"))
            //  Log.warn("Snowflake due to Abstract: "+meth)
            DefaultReturnSnowflake(meth)(this, nextStmt, self, args)
          }
          */
          def aux(): Set[AbstractState] = {
            if (meth.isNative) {
              DefaultReturnSnowflake(meth)(this, nextStmt, self, args)
              /*
              Log.warn("Native method without a snowflake in state "+this.id+". May be unsound. stmt = " + stmt)
              meth.getReturnType match {
                case _ : VoidType => Set(this.copy(stmt = nextStmt))
                case _ : PrimType =>
                  System.store.update(destAddr, D.atomicTop)
                  Set(this.copy(stmt = nextStmt))
                case _ =>
                  Log.error("Native method returns an object. Aborting.")
                  Set()
              }
              */
            }
            else {
              // TODO/optimize: filter out incorrect class types
              val newKontStack = kontStack.push(Frame(nextStmt, fp, destAddr))
              self match {
                case Some(s) => System.store.update(ThisFrameAddr(newFP), D(Set(s)))
                case None => {} // TODO: throw exception here?
              }
              for (i <- 0 until args.length) {
                System.store.update(ParameterFrameAddr(newFP, i), args(i))
              }
              Set(State(Stmt.methodEntry(meth), newFP, newKontStack))
            }
          }
          self match {
            case Some(v) =>
              if (Snowflakes.isSnowflakeObject(v))
                DefaultReturnSnowflake(meth)(this, nextStmt, self, args)
              else
                aux()
            case None => aux()
          }
      }
    }

    base match {
      case None =>
        System.checkInitializedClasses(method.getDeclaringClass())
        dispatch(None, method)
      case Some((b, isSpecial)) =>
        ((for (v <- b.getValues) yield {
          v match {
            case ObjectValue(_, bp) if bp.isInstanceOf[AbstractSnowflakeBasePointer] => dispatch(Some(v), method)
            case ObjectValue(sootClass, bp) if Soot.canStoreClass(sootClass, method.getDeclaringClass) =>
              val meth = if (isSpecial) method else overrides(sootClass, method).head
              dispatch(Some(v), meth)
            case ArrayValue(sootType, bp) =>
              dispatch(Some(v), method)
            case _ => Set()
          }
        }) :\ Set[AbstractState]())(_ ++ _) // TODO: better way to do this?
    }
  }

  // If you reference an unititialized field, what should it be?
  def defaultInitialValue(t : Type) : D = {
    t match {
      case t : RefLikeType => eval(NullConstant.v())
      case t : PrimType => D.atomicTop // TODO/precision: should eval based on specific type
    }
  }

  // If you reference an unititialized static field, what should it be?
  def staticInitialValue(f : SootField) : D = {
    for (t <- f.getTags)
      t match {
        case t : DoubleConstantValueTag => return D.atomicTop
        case t : FloatConstantValueTag => return D.atomicTop
        case t : IntegerConstantValueTag => return D.atomicTop
        case t : LongConstantValueTag => return D.atomicTop
        case t : StringConstantValueTag => return D(Set(ObjectValue(Soot.classes.String, StringBasePointerTop)))
        case _ => ()
      }
    return defaultInitialValue(f.getType)
  }

  // Update the store to contain the possibly nested arrays described
  // by the Type t with dimension sizes 'sizes'
  def createArray(t : Type, sizes : List[D], addrs : Set[Addr]/*, store: Store*/) : Store = sizes match {
    // Should only happen on recursive calls. (createArray should never be called by a user with an empty list of sizes).
    //case Nil => Store(mutable.Map()).update(addrs, defaultInitialValue(t)).asInstanceOf[Store]
    case Nil => System.store.update(addrs, defaultInitialValue(t)).asInstanceOf[Store]
    case (s :: ss) =>
      val bp : BasePointer = malloc()
      // TODO/soundness: exception for a negative length
      // TODO/precision: stop allocating if a zero length
      // TODO/precision: separately allocate each array element
      createArray(t.asInstanceOf[ArrayType].getElementType(), ss, Set(ArrayRefAddr(bp)))
        .update(addrs, D(Set(ArrayValue(t, bp))))
        .update(ArrayLengthAddr(bp), s).asInstanceOf[Store]
  }

  // Returns the set of successor states to this state.
  override def next() : Set[AbstractState] = {
    try {
      val nexts = true_next()
/*      val exceptionStates = (exceptions.getValues map {
        kontStack.handleException(_, stmt, fp)//, store)
      }).flatten
      nexts ++ exceptionStates
 */
      nexts
    } catch {
      case UninitializedClassException(sootClass) =>
        Log.info("Static initializing "+sootClass)

        def initializeClass(sootClass: SootClass) {
          if (sootClass.hasSuperclass) {
            initializeClass(sootClass.getSuperclass)
          }
          // Initialize all static fields per JVM 5.4.2 and 5.5
          for (f <- sootClass.getFields if f.isStatic) {
            System.store.update(StaticFieldAddr(f), staticInitialValue(f))
          }
          System.addInitializedClass(sootClass)
        }

        exceptions = D(Set())
        val meth = sootClass.getMethodByNameUnsafe(SootMethod.staticInitializerName)
        if (meth != null) {
          initializeClass(sootClass)
          // TODO: Do we need to use the same JStaticInvokeExpr for repeated calls?
          this.copy().handleInvoke(new JStaticInvokeExpr(meth.makeRef(),
            java.util.Collections.emptyList()), None, stmt)
        } else {
          // TODO: Do we need to do newStore for static fields?
          System.addInitializedClass(sootClass)
          Set(this.copy())
        }

      case UninitializedSnowflakeObjectException(sootClass) =>
        Log.info("Initializing snowflake class "+sootClass.getName)
        Snowflakes.initStaticFields(sootClass)
        System.addInitializedClass(sootClass)
        Set(this.copy())
      /*
      case StringConstantException(string) =>
        Log.info("Initializing string constant: \""+string+"\"")
        val value = Soot.classes.String.getFieldByName("value")
        val hash = Soot.classes.String.getFieldByName("hash")
        val hash32 = Soot.classes.String.getFieldByName("hash32")
        val bp = StringBasePointerTop
        createArray(ArrayType.v(CharType.v,1), List(D.atomicTop/*string.length*/),
                    Set(InstanceFieldAddr(bp, value)))
          .update(InstanceFieldAddr(bp, hash), D.atomicTop)
          .update(InstanceFieldAddr(bp, hash32), D.atomicTop)
        Set(this.copy())
      */
      case UndefinedAddrsException(addrs) =>
        //An empty set of addrs may due to the over approximation of ifStmt.
        System.undefined += 1

        for (addr <- addrs) {
          addr match {
            case InstanceFieldAddr(bp, field) =>
              Snowflakes.initField(Set(addr), field)
            case StaticFieldAddr(field) =>
              Snowflakes.initField(Set(addr), field)
            case _ =>
              Log.error("Undefined Addrs in state "+this.id+"; stmt = "+stmt+"; addrs = "+addrs)
          }
        }
        Set(this.copy())
    }
  }

  def newExpr(lhsAddr : Set[Addr], sootClass : SootClass) = {
    val md = MethodDescription(sootClass.getName, SootMethod.constructorName, List(), "void")
    //if (System.isLibraryClass(sootClass) && !sootClass.getPackageName.startsWith("com.sun.net.httpserver")
    /*
    if (System.isLibraryClass(sootClass) || Snowflakes.contains(md)) {
      Snowflakes.createObject(Some(lhsAddr), sootClass)
    }
    else {
    */
      val bp : BasePointer = malloc()
      val obj : Value = ObjectValue(sootClass, bp)
      val d = D(Set(obj))
      System.store.update(lhsAddr, d)
      System.checkInitializedClasses(sootClass)
      // initialize instance fields to default values for their type
      def initInstanceFields(c : SootClass) {
        for (f <- c.getFields) {
          System.store.update(InstanceFieldAddr(bp, f), defaultInitialValue(f.getType))
        }
        // TODO: swap these lines?
        if(c.hasSuperclass) initInstanceFields(c.getSuperclass)
      }
      initInstanceFields(sootClass)
    //}
  }

  def true_next() : Set[AbstractState] = {
    stmt.sootStmt match {
      case sootStmt : InvokeStmt => handleInvoke(sootStmt.getInvokeExpr, None)

      case sootStmt : DefinitionStmt =>
        val lhsAddr = addrsOf(sootStmt.getLeftOp())

        sootStmt.getRightOp() match {
          case rhs : InvokeExpr =>
            handleInvoke(rhs, Some(lhsAddr))
          case rhs : NewExpr =>
            val baseType : RefType = rhs.getBaseType()
            val sootClass = baseType.getSootClass()
            this.newExpr(lhsAddr, sootClass)
            Set(this.copy(stmt = stmt.nextSyntactic))
          case rhs : NewArrayExpr =>
            // Value of lhsAddr will be set to a pointer to the array. (as opposed to the array itself)
            /*
            rhs.getType match {
              case rt: RefType if (System.isLibraryClass(Soot.getSootClass(rt.getClassName))) =>
                  Snowflakes.createArray(rt, List(eval(rhs.getSize)), lhsAddr)
              case t => createArray(t, List(eval(rhs.getSize)), lhsAddr)
            }
            */
            createArray(rhs.getType, List(eval(rhs.getSize)), lhsAddr)
            Set(this.copy(stmt = stmt.nextSyntactic))
          case rhs : NewMultiArrayExpr =>
            //TODO, if base type is Java library class, call Snowflake.createArray
            //see comment above about lhs addr
            createArray(rhs.getType, rhs.getSizes.toList map eval, lhsAddr)
            Set(this.copy(stmt = stmt.nextSyntactic))
          case rhs =>
            System.store.update(lhsAddr, eval(rhs))
            Set(this.copy(stmt = stmt.nextSyntactic))
        }

      case sootStmt : IfStmt =>
        eval(sootStmt.getCondition()) //in case of side effects //TODO/precision evaluate the condition
        val trueState = this.copy(stmt = stmt.copy(sootStmt = sootStmt.getTarget()))
        val falseState = this.copy(stmt = stmt.nextSyntactic)
        Set(trueState, falseState)

      case sootStmt : SwitchStmt =>
        //TODO/precision dont take all the switches
        (sootStmt.getDefaultTarget() :: sootStmt.getTargets().toList)
          .map(t => this.copy(stmt = stmt.copy(sootStmt = t))).toSet

      case sootStmt : ReturnStmt =>
        val evaled = eval(sootStmt.getOp())
        for ((frame, newStack) <- kontStack.pop) yield {
          if (frame.acceptsReturnValue()) {
            System.store.update(frame.destAddr.get, evaled)
          }
          State(frame.stmt, frame.fp, newStack)
        }

      case sootStmt : ReturnVoidStmt =>
        for ((frame, newStack) <- kontStack.pop() if !frame.acceptsReturnValue()) yield {
          State(frame.stmt, frame.fp, newStack)
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

      case sootStmt : GotoStmt => Set(this.copy(stmt = stmt.copy(sootStmt = sootStmt.getTarget())))

      // For now we don't model monitor statements, so we just skip over them
      // TODO/soundness: In the event of multi-threaded code with precise interleaving, this is not sound.
      case sootStmt : EnterMonitorStmt => Set(this.copy(stmt = stmt.nextSyntactic))
      case sootStmt : ExitMonitorStmt => Set(this.copy(stmt = stmt.nextSyntactic))

      // TODO: needs testing
      case sootStmt : ThrowStmt =>
        val v = eval(sootStmt.getOp())
        exceptions = exceptions.join(v)
        Set()

      // TODO: We're missing BreakPointStmt and RetStmt (but these might not be used)
      case _ =>
        throw new Exception("No match for " + stmt.sootStmt.getClass + " : " + stmt.sootStmt)
    }
  }
}

// TODO/refactor: Maybe allow user-specified arguments.
object State {
  val stringClass : SootClass = Soot.classes.String
  val value = stringClass.getFieldByName("value")
  val hash = stringClass.getFieldByName("hash")
  val hash32 = stringClass.getFieldByName("hash32")

  val initialFramePointer = InitialFramePointer
  val initialBasePointer = InitialBasePointer
  val initial_map : mutable.Map[Addr, D] = mutable.Map(
    ParameterFrameAddr(initialFramePointer, 0) ->
      D(Set(ArrayValue(ArrayType.v(stringClass.getType, 1), initialBasePointer))),
    ArrayRefAddr(initialBasePointer) -> D(Set(ObjectValue(stringClass, initialBasePointer))),
    ArrayLengthAddr(initialBasePointer) -> D.atomicTop,

    InstanceFieldAddr(StringBasePointerTop, value) ->
      D(Set(ArrayValue(ArrayType.v(CharType.v, 1), StringBasePointerTop))),
    ArrayRefAddr(StringBasePointerTop) -> D.atomicTop,
    ArrayLengthAddr(StringBasePointerTop) ->  D.atomicTop,
    InstanceFieldAddr(StringBasePointerTop, hash) -> D.atomicTop,
    InstanceFieldAddr(StringBasePointerTop, hash32) -> D.atomicTop,

    InstanceFieldAddr(InitialBasePointer, value) ->
      D(Set(ArrayValue(ArrayType.v(CharType.v, 1), StringBasePointerTop))),
    InstanceFieldAddr(InitialBasePointer, hash) -> D.atomicTop,
    InstanceFieldAddr(InitialBasePointer, hash32) -> D.atomicTop
  )

  def inject(stmt : Stmt) : State = {
    val ks = KontStack(HaltKont)
    State(stmt, initialFramePointer, ks)
  }
}

object System {
  var undefined: Int = 0
  var noSucc: Int = 0
  val store: Store = Store(State.initial_map)
  val kstore: KontStore = KontStore(mutable.Map[KontAddr, KontD]())

  var isInitializedClassesChanged: Boolean = false
  val initializedClasses: mutable.Set[SootClass] = mutable.Set[SootClass]()

  val readTable: mutable.Map[Addr, Set[AbstractState]] = mutable.Map[Addr, Set[AbstractState]]()
  val readKTable: mutable.Map[KontAddr, Set[AbstractState]] = mutable.Map[KontAddr, Set[AbstractState]]()

  // Any time you reference a class, you have to run this to make sure it's initialized.
  // If it isn't, the exception should be caught so the class can be initialized.
  def checkInitializedClasses(c : SootClass) {
    if (!initializedClasses.contains(c)) {
      //if (System.isLibraryClass(c))
      //  throw new UninitializedSnowflakeObjectException(c)
      throw new UninitializedClassException(c)
    }
  }
  def checkInitializedClasses(t : Type) {
    t match {
      case at : ArrayType => checkInitializedClasses(at.baseType)
      case pt : PrimType => {}
      case rt : RefType => checkInitializedClasses(rt.getSootClass)
    }
  }

  def isLibraryClass(c: SootClass): Boolean = {
    isAppLibraryClass(c) || Soot.isJavaLibraryClass(c)
  }

  // Application Library package names is in resources/libclasses.txt
  // We put a dot at the end in case the package name is an exact match
  // We end these with "." so we don't hit similarly named libraries
  var appLibClasses: List[String] = List()
  def setAppLibraryClasses(libClassesFile: String) {
    if (libClassesFile != null && libClassesFile.size > 0)
      appLibClasses = Source.fromFile(libClassesFile).getLines.toList.foldLeft(List[String]()) { (acc, l) =>
        val line = l.trim
        if (line(0) == '#') acc else line::acc
      }
  }
  def isAppLibraryClass(c : SootClass) : Boolean =
    System.appLibClasses.exists((c.getPackageName()+".").startsWith(_))

  private def addToMultiMap[K, V](table: mutable.Map[K, Set[V]])(key: K, value: V) {
    table.get(key) match {
      case Some(vals) => table += (key -> (vals+value))
      case None => table += (key -> Set(value))
    }
  }
  private def trunOnRecording() {
    store.resetReadAddrsAndWriteAddrs
    kstore.resetReadAddrsAndWriteAddrs
    store.on = true
    kstore.on = true
    isInitializedClassesChanged = false
  }
  private def turnOffRecording() {
    store.on = false
    kstore.on = false
  }
  def addInitializedClass(sootClass: SootClass) {
    initializedClasses += sootClass
    isInitializedClassesChanged = true
  }
  def addToReadTable = addToMultiMap(readTable)(_, _)
  def addToReadKTable = addToMultiMap(readKTable)(_, _)

  def next(state: AbstractState): Set[AbstractState] = {
    trunOnRecording()
    val nexts = state.next()
    if (nexts.size == 0) {
      state match {
        case ErrorState => {}
        case state : State =>
          if (state.kontStack.k != HaltKont) {
            Log.error("state[" + state.id + "] has no successors: " + state)
            System.noSucc += 1
          }
      }
    }
    turnOffRecording()

    Log.debug("readAddr: " + store.readAddrs)
    for (addr <- store.readAddrs) {
      if (store.contains(addr))
        Log.debug("read content: " + addr + " " + store(addr))
      else
        Log.debug("no content for " + addr)
    }
    Log.debug("writeAddr: " + store.writeAddrs)
    for (addr <- store.writeAddrs) {
      if (store.contains(addr))
        Log.debug("write content: " + addr + " " + store(addr))
      else
        Log.debug("no content for " + addr)
    }

    for (addr <- store.readAddrs) {
      addToReadTable(addr, state)
    }
    for (kaddr <- kstore.readAddrs) {
      addToReadKTable(kaddr, state)
    }

    state.setReadAddrs(store.readAddrs)
    state.setWriteAddrs(store.writeAddrs)
    state.setKReadAddrs(kstore.readAddrs)
    state.setKWriteAddrs(kstore.writeAddrs)

    nexts
  }
}

// Command option config
case class Config(rtJar: String = null,
                  sootClassPath: String = null,
                  className: String = null,
                  methodName: String = null,
                  outputFile: String = null,
                  libClassesFile: String = null,
                  logLevel: String = "info")

object Main {
  def main(args : Array[String]) {
    val parser = new scopt.OptionParser[Config]("jaam-interpreter") {
      opt[String]("classpath") action {
        (x, c) => c.copy(sootClassPath = x)
      } text("the TODO class directory")

      opt[String]('J', "rt_jar") action {
        (x, c) => c.copy(rtJar = x)
      } text("the rt.jar file")

      opt[String]('L', "lib_classes") action {
        (x, c) => c.copy(libClassesFile = x)
      } text("app's library classes")

      opt[String]('c', "class") action {
        (x, c) => c.copy(className = x)
      } text("the main class")

      opt[String]('m', "method") action {
        (x, c) => c.copy(methodName = x)
      } text("the main method")

      opt[String]('o', "outfile") action {
        (x, c) => c.copy(outputFile = x)
      } text("the output file for the serialized data")

      opt[String]('l', "log") action {
        (x, c) => c.copy(logLevel = x)
      } validate {
        x =>
          if (List("none", "error", "warn", "info", "debug", "trace").contains(x.toLowerCase))
            success
          else
            failure("Logging level must be one of 'none', 'error', 'warn', 'info', 'debug', or 'trace'")
      } text ("the level of logging verbosity; one of 'none', 'error', 'warn', 'info', 'debug', 'trace'; default: 'info'")

      help("help") text("prints this usage text")

      override def showUsageOnError = true
    }

    parser.parse(args, Config()) match {
      case None =>
        println("Wrong arguments")

      case Some(config) =>
        Soot.initialize(config)
        Log.setLogging(config.logLevel)
        defaultMode(config)
    }
  }

  def defaultMode(config : Config) {
    System.setAppLibraryClasses(config.libClassesFile)
    val outputFileOpt = Option(config.outputFile)
    var outputFile: String = null
    outputFileOpt match {
      case None => outputFile = config.className + ".jaam"
      case Some(x) => outputFile = x
    }
    val outSerializer = new serializer.PacketOutput(new FileOutputStream(outputFile))
    val mainMainMethod : SootMethod = Soot.getSootClass(config.className).getMethodByName(config.methodName)
    val initialState = State.inject(Stmt.methodEntry(mainMainMethod))

    var todo: List[AbstractState] = List(initialState)
    var done: Set[AbstractState] = Set()
    var doneEdges: Map[(Int, Int), Int] = Map()

    outSerializer.write(initialState.toPacket)

    while (todo.nonEmpty) {
      val current = todo.head
      todo = todo.tail
      Log.info("Processing state " + current.id+": "+(current match { case s : State => s.stmt.toString; case s => s.toString}))
      val nexts = System.next(current)
      val newTodo = nexts.toList.filter(!done.contains(_))

      for (n <- newTodo) {
        Log.info("Writing state "+n.id)
        outSerializer.write(n.toPacket)
      }

      for (n <- nexts) {
        if (!doneEdges.contains((current.id, n.id))) {
          val id = doneEdges.size // TODO: Should create an creating object so these are consistent about 0 vs 1 based
          Log.info("Writing edge "+id+": "+current.id+" -> "+n.id)
          doneEdges += (current.id, n.id) -> id
          outSerializer.write(serializer.Edge(serializer.Id[serializer.Edge](id), serializer.Id[serializer.Node](current.id), serializer.Id[serializer.Node](n.id)))
        } else {
          Log.info("Skipping edge "+current.id+" -> "+n.id)
        }
      }

      for (w <- current.getWriteAddrs; s <- System.readTable(w)) {
        done -= s
        todo +:= s
      }
      for (w <- current.getKWriteAddrs; s <- System.readKTable(w)) {
        done -= s
        todo +:= s
      }

      if (System.isInitializedClassesChanged) {
        todo = newTodo ++ List(current) ++ todo
      }
      else {
        done += current
        todo = newTodo ++ todo
      }

      Log.info("Done processing state "+current.id)
    }

    outSerializer.close()
    /*

    // Store summary, print out the number of values in a single address,
    // and how many address have that number of values.
    val summary = System.store.map.foldLeft(Map[Int, Int]()) {
      case (acc, (k, v)) =>
        val size = v.values.size
        if (acc.contains(size)) acc + (size -> (acc(size)+1))
        else acc + (size -> 1)
    }
    val sorted = summary.toList.sortWith(_._1 > _._1)
    sorted.foreach { case (size, n) => println(size + " \t " + n) }
    */
    Log.info("Done!")
    if (System.undefined != 0) {
      Log.error("Undefined address number: " + System.undefined)
    }
    if (System.noSucc != 0) {
      Log.error("No successor state number: " + System.noSucc)
    }
  }
}
