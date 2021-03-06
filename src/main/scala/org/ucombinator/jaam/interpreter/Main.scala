package org.ucombinator.jaam.interpreter

/*
  Warning:

  1. Don't cross the streams.  It would be bad.

  2. Don't order addresses.  Dogs and cats, living together.  Mass hysteria.
*/

// TODO: put info about configuration in jaam file
// TODO: put enough information in jaam file to continue interrupted processes
// TODO: need to track exceptions that derefing a Null could cause
// TODO: invoke main method instead of jumping to first instr so static init is done correctly
// TODO: invoke main method so we can initialize the parameters to main
// TODO: option for saturation or not
// TODO: if(?) { C.f } else { C.f }
//       Then C.<clinit> may be called on one branch but not the other
// TODO: logging system that allows control of different sub-systems
// TODO: must use return type with getMethod
// TODO/research: increase context sensitivity when constructors go up inheritance hierarchy
// TODO: way to interrupt process but with normal termination

import scala.collection.JavaConversions._
import scala.collection.immutable
import scala.collection.immutable.::
import scala.collection.mutable
import scala.io.Source
import scala.reflect.ClassTag
import java.io.{BufferedReader, File, FileOutputStream, FileReader}

import org.rogach.scallop._
import org.ucombinator.jaam.util.CachedHashCode

// We expect every Unit we use to be a soot.jimple.Stmt, but the APIs
// are built around using Unit so we stick with that.  (We may want to
// fix this when we build the Scala wrapper for Soot.)
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}
import soot.options.Options

import soot.jimple.internal.JStaticInvokeExpr
import soot.jimple.toolkits.callgraph._
import soot.jimple.toolkits.invoke.AccessManager
import soot.tagkit._

import org.ucombinator.jaam.util.Log
import org.ucombinator.jaam.serializer
import org.ucombinator.jaam.util.Stmt
import org.ucombinator.jaam.util.Soot
import org.ucombinator.jaam.util.Soot.unitToStmt // Automatically convert soot.Unit to soot.Stmt
import org.ucombinator.jaam.interpreter.snowflakes._

// TODO: some sets could just be lists until we sort them out at the end
// TODO: `union` might be more efficient than `_++_`
/*
TODO: use FastSet to enforce use of CachedHashCode
class FastSet[T <: CachedHashCode] extends Set[T]
 */

// Possibly thrown during transition between states.
case class UninitializedClassException(sootClass : SootClass) extends RuntimeException
case class StringConstantException(string : String) extends RuntimeException
case class UndefinedAddrsException[A <: Addr](addrs : Set[A]) extends RuntimeException

// A continuation store paired with a continuation
case class KontStack(k : KontAddr) extends CachedHashCode {
  // TODO/generalize: Add widening in push and pop (to support PDCFA)
  // TODO/generalize: We need the control pointer of the target state to HANDLE PDCFA.
  def kalloca(meth : SootMethod, newFP : FramePointer, frame : Frame) = ZeroCFAKontAddr(newFP)

  // TODO/generalize: Handle PDCFA
  def push(meth : SootMethod, newFP : FramePointer, frame : Frame) : KontStack = {
    val kAddr = kalloca(meth, newFP, frame)
    System.kStore.update(kAddr, KontD(Set(RetKont(frame, k))))
    KontStack(kAddr)
    //KontStack(RetKont(frame, kAddr))
  }

  // Pop returns all possible frames beneath the current one.
  def pop() : Set[(Frame, KontStack)] = {
    for (topk <- System.kStore(k).values) yield {
      topk match {
        case RetKont(frame, kontAddr) => (frame, KontStack(kontAddr))
      }
    }
  }

  def handleException(exception : Value,
                      stmt : Stmt,
                      fp : FramePointer
                    ) : Set[AbstractState] = {

    if (!Main.conf.exceptions()) {
      Log.info("Ignoring thrown exception: "+exception)
      return Set.empty
    }

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
      return Set.empty
    }

    /* TODO: can this iterative version of stack walking be deleted?
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
        val caughtType = trap.getException
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

case class Frame(stmt : Stmt,
                 fp : FramePointer,
                 destAddr : Option[Set[Addr]]) extends CachedHashCode {
  def acceptsReturnValue : Boolean = destAddr.nonEmpty
}

abstract class Kont extends CachedHashCode

case class RetKont(frame : Frame,
                   k : KontAddr
                  ) extends Kont

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
// TODO: separate 'null' from AnyAtomicValue

case class ObjectValue(sootClass : SootClass, bp : BasePointer) extends Value {
  if (!sootClass.isConcrete && !Snowflakes.isSnowflakeObject(bp)) {
    throw new Exception("Created ObjectValue for non-concrete type: " + sootClass + " ; base pointer: " + bp)
  }
}

// The sootType is the type with array wrapper
case class ArrayValue(sootType : Type, bp : BasePointer) extends Value

//----------------- POINTERS ------------------

// FramePointers, when paired with variable names, yield the addresses of variables.
abstract class FramePointer extends CachedHashCode
case object InvariantFramePointer extends FramePointer // TODO: delete this?  Is seems unused
case class ZeroCFAFramePointer(method : SootMethod) extends FramePointer
case class OneCFAFramePointer(method : SootMethod, nextStmt : Stmt) extends FramePointer
case object InitialFramePointer extends FramePointer

abstract class From
case object FromNative extends From
case object FromJava extends From

// BasePointers, when paired with field names, yield the addresses of fields.
abstract class BasePointer
case class OneCFABasePointer(stmt : Stmt, fp : FramePointer) extends BasePointer
case object InitialBasePointer extends BasePointer
case class PreloadedBasePointer(id: Int) extends BasePointer

// Note that due to interning, strings and classes may share base pointers with each other
// Oh, and class loaders are a headache(!)
abstract class StringBasePointer extends BasePointer

case object StringLiteralAddr extends Addr {} // HACK: so string literals can be smuggled to Class.forName0
case class StringLiteralValue(s: String) extends Value {} // HACK: so string literals can be smuggled to Class.forName0

// TODO/soundness: how to mix literal string base pointer and string base pointer top?
object StringBasePointer {
  var constants= Map.empty[String, StringBasePointer]
  val value: SootField  = Soot.classes.String.getFieldByName("value")
  val hash: SootField   = Soot.classes.String.getFieldByName("hash")
  val hash32: SootField = Soot.classes.String.getFieldByName("hash32")

  // Fields of StringBasePointerTop
  System.store.update(
    InstanceFieldAddr(StringBasePointerTop, value),
    D(Set(ArrayValue(ArrayType.v(CharType.v, 1), StringArrayBasePointerTop))))
  System.store.update(
    InstanceFieldAddr(StringBasePointerTop, hash), D.atomicTop)
  System.store.update(
    InstanceFieldAddr(StringBasePointerTop, hash32), D.atomicTop)
  System.store.update(
    ArrayRefAddr(StringArrayBasePointerTop), D.atomicTop)
  System.store.update(
    ArrayLengthAddr(StringArrayBasePointerTop), D.atomicTop)

  def apply(string: String, state: State): StringBasePointer = {
    if (Main.conf.stringTop()) {
      System.store.update(StringLiteralAddr, D(Set(StringLiteralValue(string))))
      StringBasePointerTop
    }
    else {
      constants.get(string) match {
        case Some(bp) => bp
        case None =>
          // TODO: should this go here or should we throw an exception for top level handling?
          // TODO: we can reuse base pointers when strings are the same because all the fields are immutable. Research: can we generalize this idea?
          Log.info("Initializing string constant: \""+string+"\"")
          val bp = LiteralStringBasePointer(string)

          state.createArray( // TODO: use 'current state' instead of state parameter
            ArrayType.v(CharType.v,1),
            List(D.atomicTop/*string.length*/),
            Set(InstanceFieldAddr(bp, value)))
          System.store.update(InstanceFieldAddr(bp, hash), D.atomicTop)
          System.store.update(InstanceFieldAddr(bp, hash32), D.atomicTop)
          constants += string -> bp
          bp
      }
    }
  }
}

case object StringBasePointerTop extends StringBasePointer
case object StringArrayBasePointerTop extends BasePointer
case object InitialStringBasePointer extends StringBasePointer
case object InitialStringArrayBasePointer extends StringBasePointer
case class LiteralStringBasePointer(string: String) extends StringBasePointer {
  // Use escape codes (e.g., `\n`) in the string.  We do this by getting a
  // representation of a string constant and then printing that.
  override lazy val toString = {
    import scala.reflect.runtime.universe._
    "LiteralStringBasePointer(" + Literal(Constant(string)).toString + ")"
  }
}

// we remvoe the argument of ClassBasePointer, to make all ClassBasePointer points to the same
case class ClassBasePointer(name : String) extends BasePointer
//case object ClassBasePointer extends BasePointer

//----------------- ADDRESSES ------------------

// TODO: implement p4f
// Addresses of continuations on the stack
abstract class KontAddr extends Addr
//case class OneCFAKontAddr(val fp : FramePointer) extends KontAddr
case class ZeroCFAKontAddr(fp : FramePointer) extends KontAddr
case object HaltKontAddr extends KontAddr // Should never be assigned a continuation

abstract class Addr extends CachedHashCode
abstract class FrameAddr extends Addr

case object GlobalSnowflakeAddr extends Addr

// For local variables
case class LocalFrameAddr(fp : FramePointer, register : Local) extends FrameAddr

case class ParameterFrameAddr(fp : FramePointer, parameter : Int) extends FrameAddr

case class ThisFrameAddr(fp : FramePointer) extends FrameAddr

// Holds the most recently caught exception in this frame
case class CaughtExceptionFrameAddr(fp : FramePointer) extends FrameAddr

case class InstanceFieldAddr(bp : BasePointer, field : SootField) extends Addr

case class ArrayRefAddr(bp : BasePointer) extends Addr // TODO: add index (it is all AtomicValue anyway)

case class ArrayLengthAddr(bp : BasePointer) extends Addr

case class StaticFieldAddr(field : SootField) extends Addr

// TODO: replace KontD and D with AbstractDomain[...]
// TODO: cache hashcode for Kont and Value

// TODO: why is values private?
case class D(private val values: Set[Value]) {
  def getValues: Set[Value] = values
  def join(that : D): D = D(this.getValues.union(that.getValues))
  def maybeZero() : Boolean = getValues.exists(_.isInstanceOf[AtomicValue])
}
object D {
  val atomicTop = D(Set(AnyAtomicValue))
}

object GlobalD extends D(Set.empty[Value]) {
  val globalValues: mutable.Set[Value] = mutable.Set.empty
  val map: mutable.Map[SootClass, mutable.Set[Value]] = mutable.Map.empty
  var modified: Boolean = false

  var size: Int = 0
  var cachedValues: Set[Value] = Set.empty

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
    modified = oldSize != globalValues.size
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

case class KontD(values: Set[Kont]) {
  def join(that : KontD) = KontD(this.values ++ that.values)
}

abstract sealed class AbstractState {
  def next() : Set[AbstractState]

  // All the getter here are implemented, though they all return a Unit value directly
  def getReadAddrs: Set[Addr] = Set.empty
  def setReadAddrs(s: Set[Addr]): Unit = {}
  def getKReadAddrs: Set[KontAddr] = Set.empty
  def setKReadAddrs(s: Set[KontAddr]): Unit = {}
  def getWriteAddrs: Set[Addr] = Set.empty
  def setWriteAddrs(s: Set[Addr]): Unit = {}
  def getKWriteAddrs: Set[KontAddr] = Set.empty
  def setKWriteAddrs(s: Set[KontAddr]): Unit = {}

  def toPacket : serializer.AbstractState

  val id: AbstractState.Id = AbstractState.idMap.getOrElseUpdate(this, AbstractState.nextId())

// TODO: can't have because AbstractState.idMap would use it
//  override def equals(that: Any): Boolean = {
//    that.isInstanceOf[AbstractState] &&
//    that.asInstanceOf[AbstractState].id == this.id
//  }
}

object AbstractState {
  type Id = Int
  val idMap: mutable.Map[AbstractState, Id] = mutable.Map.empty
  var nextId_ = 0
  def nextId() : Id = { nextId_ += 1; nextId_ }
}

case object ErrorState extends AbstractState {
  override def next() : Set[AbstractState] = Set.empty
  override def toPacket = serializer.ErrorState(serializer.Id[serializer.Node](id))
}

// State abstracts a collection of concrete states of execution.
case class State(stmt : Stmt,
                 fp : FramePointer,
                 kontStack : KontStack) extends AbstractState with CachedHashCode {
  // Needed because different "stores" should lead to different objects
  //  override def equals(that: Any): Boolean =
  //    that match {
  //      case that: State => this.stmt == that.stmt && this.fp == that.fp && this.kontStack == that.kontStack && this.store == that.store
  //      case _ => false
  //   }

  var readAddrs = Set.empty[Addr]
  override def getReadAddrs = readAddrs
  override def setReadAddrs(s: Set[Addr]) { readAddrs = s }

  var kReadAddrs = Set.empty[KontAddr]
  override def getKReadAddrs = kReadAddrs
  override def setKReadAddrs(s: Set[KontAddr]) { kReadAddrs = s }

  var writeAddrs = Set.empty[Addr]
  override def getWriteAddrs = writeAddrs
  override def setWriteAddrs(s: Set[Addr]) { writeAddrs = s }

  var kWriteAddrs = Set.empty[KontAddr]
  override def getKWriteAddrs = kWriteAddrs
  override def setKWriteAddrs(s: Set[KontAddr]) { kWriteAddrs = s }

  override def toPacket = serializer.State(serializer.Id[serializer.Node](id), stmt.toPacket, fp.toString, kontStack.toString)

  // When you call next, you may realize you are going to end up throwing some exceptions
  // TODO/refactor: Consider turning this into a list of exceptions.
  var exceptions = D(Set.empty)

  // Allocates a new frame pointer (currently uses 0CFA)
  def alloca(expr : InvokeExpr, nextStmt : Stmt) : FramePointer = ZeroCFAFramePointer(expr.getMethod)
  // Allocates objects
  def malloc() : BasePointer = OneCFABasePointer(stmt, fp)

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
      case _ : ThisRef => Set(ThisFrameAddr(fp))
      // TODO/precision: have multiple addresses per "catch" clause?
      // Perhaps mix left-hand side into address?
      case _ : CaughtExceptionRef => Set(CaughtExceptionFrameAddr(fp))
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
        D(System.store(addrsOf(v)).getValues.filter(Value.canStoreType(_, v.getType)))
      case _ : NullConstant =>
        D.atomicTop
      case _ : NumericConstant => D.atomicTop
      // TODO: Class and String objects are objects and so need their fields initialized
      // TODO/clarity: Add an example of Java code that can trigger this.
      case v : ClassConstant => D(Set(ObjectValue(Soot.classes.Class, ClassBasePointer(v.value.replace('/', '.')))))
      //D(Set(ObjectValue(stmt.classmap("java.lang.Class"), StringBasePointer(v.value)))) // TODO: remove?
      case v : StringConstant =>
        D(Set(ObjectValue(Soot.classes.String, StringBasePointer(v.value, this))))
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
              // TODO: populate fields of object (use createObject?)
              exceptions = exceptions.join(D(Set(ObjectValue(Soot.classes.ArithmeticException, malloc()))))
            }
            D.atomicTop
        }

      // Every array has a distinguished field for its address.
      case v : LengthExpr =>
        val addrs : Set[Addr] = for {
          ArrayValue(_, bp) <- eval(v.getOp).getValues
        } yield ArrayLengthAddr(bp)
        D(System.store(addrs).getValues.filter(Value.canStoreType(_, v.getType)))

      // TODO/precision: implement the actual check
      case v : InstanceOfExpr => D.atomicTop

      case v : CastExpr =>
        // TODO: cast from a SnowflakeObject to another SnowflakeObject
        val castedExpr: SootValue = v.getOp
        val castedType: Type = v.getCastType
        System.checkInitializedClasses(castedType)
        val d = eval(castedExpr)

        // TODO: filter out elements of "d" that are not of the
        // expression's type (should be done in general inside "eval"
        // (and maybe already is))
        def isCastableTo(v : Value, t : Type) : Boolean = {
          v match {
            case _ : AtomicValue => true // null is always castable
            case ObjectValue(sootClass, _) if t.isInstanceOf[RefType] =>
              Soot.canStoreClass(sootClass, t.asInstanceOf[RefType].getSootClass)
            case ObjectValue(_, _) => false
            case ArrayValue(sootType, _) => Soot.canStoreType(sootType, t)
          }
        }
        var d2 = D(Set.empty)
        for (v <- d.getValues) {
          // TODO/Guannan: Can we remove rhs of this `||`?
          if (v == AnyAtomicValue || isCastableTo(v, castedType) || castedType.isInstanceOf[ArrayType]) {
            // Up casts are always legal
            d2 = d2.join(D(Set(v)))
          }
          else if (Snowflakes.isSnowflakeObject(v)) {
            // && Soot.canStoreType(castedType, v.asInstanceOf[ObjectValue].sootClass.getType)) {

            val classCastException = D(Set(ObjectValue(Soot.classes.ClassCastException, malloc())))
            exceptions = exceptions.join(classCastException)
            d2 = d2.join(D(Set(v)))

            // TODO: re-enable this exception code
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
              d2 = d2.join(Snowflakes.createObjectOrThrow(castedClass))
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
      case _ : DynamicInvokeExpr =>
        // Could only come from non-Java sources
        throw new Exception(s"Unexpected DynamicInvokeExpr: $expr")
      case _ : StaticInvokeExpr => None
      case expr : InstanceInvokeExpr =>
        // Possibilities:
        //  - SpecialInvokeExpr: <init>, private methods, and methods of superclass
        //  - InterfaceInvokeExpr
        //  - VirtualInvokeExpr
        Some((eval(expr.getBase), expr.isInstanceOf[SpecialInvokeExpr]))
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
    // o.f(3);
    // In this case, c is the type of o. m is f. receivers is the result of eval(o).
    // TODO/dragons. Here they be.
    def dispatch(self : Option[Value], meth : SootMethod) : Set[AbstractState] = {
      // TODO: group multiple "self" together
      Snowflakes.dispatch(meth) match {
        case Some(h) =>
          // TODO: kepp self.exists(Snowflakes.isSnowflakeObject(_) ?
          // TODO: Also, allow option to abort instead of snowflake (i.e., return empty set of states with log message)
          if (meth.isNative) { Log.warn("Native snowflake: "+meth+" self: "+self) }
          Snowflakes.warn(this.id, self, stmt, meth)
          // TODO/optimize: do we need to filter out incorrect class types?
          h(this, nextStmt, self, args)
        case None =>
          // TODO/optimize: filter out incorrect class types
          val newKontStack = kontStack.push(meth, newFP, Frame(nextStmt, fp, destAddr))
          self match {
            case Some(s) => System.store.update(ThisFrameAddr(newFP), D(Set(s)))
            case None => {} // TODO: throw exception here?
          }
          for ((arg, i)<- args.zipWithIndex) {
            System.store.update(ParameterFrameAddr(newFP, i), arg)
          }
          Set(State(Stmt.methodEntry(meth), newFP, newKontStack))
      }
    }

    base match {
      case None =>
        System.checkInitializedClasses(method.getDeclaringClass)
        dispatch(None, method)
      case Some((b, isSpecial)) =>
        var r = Set.empty[AbstractState]
        for (v <- b.getValues) {
          r ++= (v match {
            case ObjectValue(_, bp) if bp.isInstanceOf[AbstractSnowflakeBasePointer] =>
              dispatch(Some(v), method)
            case ObjectValue(sootClass, _) if Soot.canStoreClass(sootClass, method.getDeclaringClass) =>
              val meth = if (isSpecial) method else Soot.overrides(sootClass, method).head
              dispatch(Some(v), meth)
            case ArrayValue(_, _) =>
              dispatch(Some(v), method)
            case _ => Set.empty
          })
        }
        r
    }
  }

  // If you reference an uninitialized field, what should it be?
  def defaultInitialValue(t : Type) : D = {
    t match {
      case _ : RefLikeType => eval(NullConstant.v)
      case _ : PrimType => D.atomicTop // TODO/precision: should eval based on specific type
    }
  }

  // If you reference an uninitialized static field, what should it be?
  def staticInitialValue(f : SootField) : D = {
    for (t <- f.getTags)
      t match {
        case _ : DoubleConstantValueTag  => return D.atomicTop
        case _ : FloatConstantValueTag   => return D.atomicTop
        case _ : IntegerConstantValueTag => return D.atomicTop
        case _ : LongConstantValueTag    => return D.atomicTop
        case t : StringConstantValueTag  => return D(Set(ObjectValue(Soot.classes.String, StringBasePointer(t.getStringValue, this))))
        case _ => ()
      }

    defaultInitialValue(f.getType)
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
      createArray(t.asInstanceOf[ArrayType].getElementType, ss, Set(ArrayRefAddr(bp)))
        .update(addrs, D(Set(ArrayValue(t, bp))))
        .update(ArrayLengthAddr(bp), s).asInstanceOf[Store]
  }

  // Returns the set of successor states to this state.
  override def next() : Set[AbstractState] = {
    try {
      val nexts = true_next()
      if (!Main.conf.exceptions()) {
        if (exceptions.getValues.nonEmpty) {
          // TODO: put entry in jaam file
          Log.info("Ignoring thrown exceptions: "+exceptions)
        }
        nexts
      } else {
        val exceptionStates = exceptions.getValues flatMap {
          kontStack.handleException(_, stmt, fp)//, store)
        }
        // `exceptionStates` is usually empty so it is slightly faster to put that first
        exceptionStates ++ nexts
      }
    } catch {
      case UninitializedClassException(sootClass) =>
        Log.info("Static initializing "+sootClass)

        // TODO: run this even if there is no clinit?
        // TODO: do we need to call clinit of super classes?
        def initializeClass(sootClass: SootClass) {
          // TODO: stop if reach initialized class
          // TODO: does `Object` hasSuperclass?
          // TODO: handle initialize snowflake object
          // TODO: call super class even when self has no clinit method?
          if (sootClass.hasSuperclass) {
            initializeClass(sootClass.getSuperclass)
          }
          // Initialize all static fields per JVM 5.4.2 and 5.5
          for (f <- sootClass.getFields if f.isStatic) {
            System.store.update(StaticFieldAddr(f), staticInitialValue(f))
          }
          System.addInitializedClass(sootClass)
        }

/*
 TODO: This exception is often due to missing a Jar file that should be in the path.  Could we compute a jar file signature that we could check?

Exception in thread "main" java.lang.RuntimeException: No field value in class jdk.nashorn.internal.codegen.ClassEmitter$Flag
        at soot.SootClass.getFieldByName(SootClass.java:283)
        at org.ucombinator.jaam.interpreter.State$$anonfun$org$ucombinator$jaam$interpreter$State$$loadObject$1$2.apply(Main.scala:869)
        at org.ucombinator.jaam.interpreter.State$$anonfun$org$ucombinator$jaam$interpreter$State$$loadObject$1$2.apply(Main.scala:867)
*/
        def loadObject(o: String): D = {
          if (o == "<null>") {
            D.atomicTop
          } else {
            val id = o.toInt
            val bp = PreloadedBasePointer(o.toInt)
            val (t, members) = System.objects(id)
            val typ = Utilities.parseType(t)
            typ match {
              case typ: RefType =>
                val c = Soot.getSootClass(typ.getClassName)
                if (!System.loadedObjects(id)) {
                  System.loadedObjects += id
                  for (System.Field(d, f, _, v) <- members) {
                    val d2 = Soot.getSootClass(Utilities.parseType(d).asInstanceOf[RefType].getClassName)
                    System.store.update(InstanceFieldAddr(bp, d2.getFieldByName(f)), loadObject(v))
                  }
                }
                D(Set(ObjectValue(c, bp)))
              case typ: ArrayType =>
                if (!System.loadedObjects(id)) {
                  System.loadedObjects += id
                  System.store.update(ArrayLengthAddr(bp), D.atomicTop)
                  val (_, elements) = System.objects(id)
                  for (System.Element(_, e) <- elements) {
                    System.store.update(ArrayRefAddr(bp), loadObject(e))
                  }
                }
                D(Set(ArrayValue(typ, bp)))
              case _ => throw new RuntimeException("Unexpected type in load object: "+o)
            }
          }
        }

        exceptions = D(Set.empty)

        // NOTE: we dont initialize the super class until one of its fields is
        // mentioned.  I think we get away with delaying it, but that might
        // not technically be the right thing to do.
        val fields = System.staticFields.getOrElse(sootClass.getName, null)
        if (fields != null) {
          for (System.Field(_, name, _, value) <- fields) {
            Log.info(f"Static field $sootClass $name is $value")
            val sootField = sootClass.getFieldByName(name)
            System.store.update(StaticFieldAddr(sootField), loadObject(value))
          }
          System.addInitializedClass(sootClass)
          return Set(this)
        }

        val meth = sootClass.getMethodByNameUnsafe(SootMethod.staticInitializerName)
        if (meth != null) {
          initializeClass(sootClass) // TODO: factor by moving before `if (meth != null)`
          // TODO: Do we need to use the same JStaticInvokeExpr for repeated calls?
          this.handleInvoke(new JStaticInvokeExpr(meth.makeRef(),
            java.util.Collections.emptyList()), None, stmt)
        } else {
          // TODO: Do we need to do newStore for static fields?
          //initializeClass(sootClass) // TODO: factor by moving before `if (meth != null)`
          // TODO: above omitted b/c absense of <clinit> means there are no static fields?
          System.addInitializedClass(sootClass)
          Set(this)
        }

/* TODO: remove (note, might be thrown by checkInitializedClasses?)
// TODO: subsumed (sort of) by UndefinedAddrException
      case UninitializedSnowflakeObjectException(sootClass) =>
        Log.info("Initializing snowflake class "+sootClass.getName)
        Snowflakes.initStaticFields(sootClass)
        System.addInitializedClass(sootClass)
        Set(this)
 */
      case UndefinedAddrsException(addrs) =>
        //An empty set of addrs may due to the over approximation of ifStmt.

        // TODO: is this right? (filter to do only on snowflake objects) (might have trouble determining if snowflake b/c we don't have the target)
        val states = for (addr <- addrs) yield {
          addr match {
            case InstanceFieldAddr(_, field) =>
              DefaultReturnSnowflake.initField(Set(addr), field)
              Set(this)
            case StaticFieldAddr(field) =>
              DefaultReturnSnowflake.initField(Set(addr), field)
              Set(this)
            case _ =>
              System.undefined += 1
              Log.error("Undefined Addrs in state "+this.id+"; stmt = "+stmt+"; addrs = "+addrs)
              Set.empty
          }
        }
        states.foldLeft(Set.empty[AbstractState])(_++_)
    }
  }

  def newExpr(lhsAddr : Set[Addr], sootClass : SootClass): Unit = {
    //if (System.isLibraryClass(sootClass) && !sootClass.getPackageName.startsWith("com.sun.net.httpserver")
    /*
    val md = MethodDescription(sootClass.getName, SootMethod.constructorName, List(), "void")
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
      def initInstanceFields(c : SootClass): Unit = {
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
        val lhsAddr = addrsOf(sootStmt.getLeftOp)

        sootStmt.getRightOp match {
          case rhs : InvokeExpr =>
            handleInvoke(rhs, Some(lhsAddr))
          case rhs : NewExpr =>
            val baseType : RefType = rhs.getBaseType
            val sootClass = baseType.getSootClass
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
        eval(sootStmt.getCondition) //in case of side effects //TODO/precision evaluate the condition
        val trueState = this.copy(stmt = stmt.copy(sootStmt = sootStmt.getTarget))
        val falseState = this.copy(stmt = stmt.nextSyntactic)
        Set(trueState, falseState)

      case sootStmt : SwitchStmt =>
        //TODO/precision dont take all the switches
        (sootStmt.getDefaultTarget :: sootStmt.getTargets.toList)
          .map(t => this.copy(stmt = stmt.copy(sootStmt = t))).toSet

      case sootStmt : ReturnStmt =>
        val evaluated = eval(sootStmt.getOp)
        for ((frame, newStack) <- kontStack.pop()) yield {
          if (frame.acceptsReturnValue) {
            System.store.update(frame.destAddr.get, evaluated)
          }
          State(frame.stmt, frame.fp, newStack)
        }

      case sootStmt : ReturnVoidStmt =>
        for ((frame, newStack) <- kontStack.pop() if !frame.acceptsReturnValue) yield {
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

      case sootStmt : GotoStmt => Set(this.copy(stmt = stmt.copy(sootStmt = sootStmt.getTarget)))

      // For now we don't model monitor statements, so we just skip over them
      // TODO/soundness: In the event of multi-threaded code with precise interleaving, this is not sound.
      case sootStmt : EnterMonitorStmt => Set(this.copy(stmt = stmt.nextSyntactic))
      case sootStmt : ExitMonitorStmt => Set(this.copy(stmt = stmt.nextSyntactic))

      // TODO: needs testing
      case sootStmt : ThrowStmt =>
        val v = eval(sootStmt.getOp)
        exceptions = exceptions.join(v)
        Set.empty

      // TODO: We're missing BreakPointStmt and RetStmt (but these might not be used)
      case _ =>
        throw new Exception("No match for " + stmt.sootStmt.getClass + " : " + stmt.sootStmt)
    }
  }
}

// TODO: research idea: lazy but with known forced times (i.e. some code can always access the forced value)

// TODO/refactor: Maybe allow user-specified arguments.
object State {
  val initialFramePointer = InitialFramePointer
  val initialArrayBasePointer = InitialBasePointer
  val initialStringBasePointer = InitialStringBasePointer
  val initialStringArrayBasePointer = InitialStringArrayBasePointer

  // TODO: factor these into helpers
  // `String[]` argument of `main(String[])`
  System.store.update(
    ParameterFrameAddr(initialFramePointer, 0),
    D(Set(ArrayValue(ArrayType.v(Soot.classes.String.getType, 1), initialArrayBasePointer))))
  System.store.update(
    ArrayRefAddr(initialArrayBasePointer), D(Set(ObjectValue(Soot.classes.String, initialStringBasePointer))))
  System.store.update(
    ArrayLengthAddr(initialArrayBasePointer), D.atomicTop)

  // `String` objects in `String[]` argument of `main(String[])`
  System.store.update(
    InstanceFieldAddr(initialStringBasePointer, StringBasePointer.value),
    D(Set(ArrayValue(ArrayType.v(CharType.v, 1), initialStringArrayBasePointer))))
  System.store.update(
    InstanceFieldAddr(initialStringBasePointer, StringBasePointer.hash), D.atomicTop)
  System.store.update(
    InstanceFieldAddr(initialStringBasePointer, StringBasePointer.hash32), D.atomicTop)
  System.store.update(
    ArrayRefAddr(initialStringArrayBasePointer), D.atomicTop)
  System.store.update(
    ArrayLengthAddr(initialStringArrayBasePointer), D.atomicTop)

  def inject(stmt : Stmt) : State = {
    val ks = KontStack(HaltKontAddr)
    State(stmt, initialFramePointer, ks)
  }
}

object System {
  // TODO: record the state numbers not just how many undefined (needed to avoid double counting)
  // TODO: more statistics on what kind of statement and why.  Also, remove from list if it later gets a successor?
  // TODO: should these go in Main?
  var undefined: Int = 0
  var noSucc = mutable.Set.empty[AbstractState]
  val store: Store = Store(mutable.Map.empty[Addr, D])
  val kStore: KontStore = KontStore(mutable.Map[KontAddr, KontD](HaltKontAddr -> KontD(Set.empty)))

  // TODO: possible bug if we attempt to eval a state that is waiting for a class to be initialized
  // Possible solutions:
  //   Keep a set of classing being initialized
  //   A more general dependency
  var isInitializedClassesChanged: Boolean = false
  val initializedClasses: mutable.Set[SootClass] = mutable.Set.empty

  val readTable: mutable.Map[Addr, Set[AbstractState]] = mutable.Map.empty
  val readKTable: mutable.Map[KontAddr, Set[AbstractState]] = mutable.Map.empty

  // Any time you reference a class, you have to run this to make sure it's initialized.
  // If it isn't, the exception should be caught so the class can be initialized.
  def checkInitializedClasses(c : SootClass) {
    if (!initializedClasses.contains(c)) {
      //if (System.isLibraryClass(c)) // TOOD: remove
      //  throw new UninitializedSnowflakeObjectException(c)
      throw new UninitializedClassException(c)
    }
  }
  def checkInitializedClasses(t : Type) {
    t match {
      case at : ArrayType => checkInitializedClasses(at.baseType)
      case _ : PrimType => {}
      case rt : RefType => checkInitializedClasses(rt.getSootClass)
    }
  }

  def isLibraryClass(c: SootClass): Boolean = isAppLibraryClass(c) || System.isJavaLibraryClass(c)

  // Application Library package names is in resources/libclasses.txt
  // We put a dot at the end in case the package name is an exact match
  // We end these with "." so we don't hit similarly named libraries
  var appLibClasses: List[String] = List.empty
  def setAppLibraryClasses(libClassesFile: String) {
    if (libClassesFile != null && libClassesFile.nonEmpty)
      appLibClasses = Source.fromFile(libClassesFile).getLines.toList.foldLeft(List.empty[String]) {
        (acc, l) =>
          val line = l.trim
          // TODO: post-pend "." if not already present
          // TODO: trim after '#' anywhere
          if (line.head == '#') acc else line::acc
      }
  }

  def isAppLibraryClass(c : SootClass) : Boolean =
    System.appLibClasses.exists((c.getPackageName+".").startsWith(_))

  def isJavaLibraryClass(sootClass: SootClass): Boolean = {
    sootClass.isJavaLibraryClass || sootClass.getName.startsWith("org.w3c") || sootClass.getName.startsWith("jdk.")
  }

  private def addToMultiMap[K, V](table: mutable.Map[K, Set[V]])(key: K, value: V) {
    table.get(key) match {
      case Some(vals) => table += (key -> (vals+value))
      case None => table += (key -> Set(value))
    }
  }
  private def trunOnRecording() {
    store.resetReadAddrsAndWriteAddrs()
    kStore.resetReadAddrsAndWriteAddrs()
    store.on = true
    kStore.on = true
    isInitializedClassesChanged = false
  }
  private def turnOffRecording() {
    store.on = false
    kStore.on = false
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
    if (nexts.nonEmpty ||
      state == ErrorState ||
      state.isInstanceOf[State] && state.asInstanceOf[State].kontStack.k == HaltKontAddr) {
      noSucc -= state
    } else {
      noSucc += state
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
    for (kaddr <- kStore.readAddrs) {
      addToReadKTable(kaddr, state)
    }

    state.setReadAddrs(store.readAddrs)
    state.setWriteAddrs(store.writeAddrs)
    state.setKReadAddrs(kStore.readAddrs)
    state.setKWriteAddrs(kStore.writeAddrs)

    nexts
  }

  abstract class Member
  case class Field(declaringClass: String, name: String, typ: String, value: String) extends Member
  case class Element(index: Int, element: String) extends Member

  val loadedStatics = mutable.Set.empty[String]
  val loadedObjects = mutable.Set.empty[Int]
  val staticFields = mutable.Map.empty[String/*class name*/, List[Field]]
  val objects = mutable.Map.empty[Int/*addr*/, (String/*type*/,List[Member])]

  def loadStore(file: File) {
    val f = new BufferedReader(new FileReader(file))
    var line = f.readLine()
    while (line != null) {
      line.split(": ", 2).toList match {
        //case List() => {}
        case List(_) => {}
        case List(tag,body) =>
      val parts = body.split("\t").toList.map(_.split("=",2).toList)
      if (tag == "static fields") {
        parts match {
          case List(List("class",c)) =>
            staticFields(c) = List.empty
        }
      } else if (tag == "static field") {
        parts match {
          case List(List("class", c), List("field", f), List("type", t), List("value", v)) =>
            staticFields(c) = Field(c, f, t, v) :: staticFields(c) // TODO: flip order the normal way around
        }
      } else if (tag == "instance fields") {
        parts match { // TODO: [] in pattern match?
          case List(List("object", o), List("type", c)) =>
            if (objects.getOrElse(o.toInt, null) == null) { // TODO: clean up this code
              objects(o.toInt) = (c, List.empty)
            } else {
              objects(o.toInt) = (c, objects(o.toInt)._2)
            }
        }
      } else if (tag == "instance field") {
        parts match {
          case List(List("class", c), List("object",o), List("field", f), List("type", t), List("value", v)) =>
            objects(o.toInt) = (c, Field(c, f, t, v) :: objects(o.toInt)._2) //TODO: agent should do only one instancefields (or we just work around it)
        }
      } else if (tag == "array elements") {
        parts match {
          case List(List("array", a), List("type", t), List("length", _)) =>
            objects(a.toInt) = (t, List.empty)
        }
      } else if (tag == "array element") {
        parts match {
          case List(List("array", a), List("type", t), List("index", i), List("element", e)) =>
            objects(a.toInt) = (t, Element(i.toInt, e) :: objects(a.toInt)._2)
        }
      }
      }

      line = f.readLine()
    }
  }

}

object Main {
  var conf = org.ucombinator.jaam.main.Interpreter;  // TODO: remove this

  def cha() {
    //val allAppClassess = conf.classpath().toString.split(":").map(getAllClassNames(_))
    Soot.initialize(conf.rtJar().toString,  conf.classpath().toString, {
      Options.v().set_include_all(false)
      Options.v.set_whole_program(true)
      Options.v().set_app(true)
      Options.v().set_soot_classpath(conf.rtJar().toString + ":" + conf.classpath().toString)

      for (className <- Utilities.getAllClasses(conf.classpath().toString)) {
        val c = Scene.v().loadClassAndSupport(className)
        c.setApplicationClass()
      }

      //Options.v().setPhaseOption("cg", "verbose:true")
      Options.v().setPhaseOption("cg.cha", "enabled:true")

      // Spark looks not as good as CHA according to the number of reachable methods
      // but point-to analysis can be used to refine call graph, see example
      // http://www.programcreek.com/java-api-examples/index.php?api=soot.jimple.toolkits.callgraph.CallGraph
      //Options.v().setPhaseOption("cg.spark", "enabled:true,on-fly-cg:true")

      Options.v().set_main_class(conf.mainClass())
      //Scene.v().loadNecessaryClasses()
      Scene.v().loadBasicClasses()
      Scene.v().loadDynamicClasses()
    })

    CHATransformer.v().transform()
    val cg = Scene.v().getCallGraph
    Log.info("Total number of reachable methods (including Java library): " + Scene.v().getReachableMethods.size)
    for (m <- Scene.v().getReachableMethods.listener.toList) {
      Log.debug("  " + m)
    }

    var seen = Set.empty[State] // includes everything including todo elements
    var todo = Set.empty[State]
    var seenMethods = Set.empty[SootMethod]
    var stateCount = 0
    var edgeCount = 0

    val outSerializer = new serializer.PacketOutput(new FileOutputStream(conf.outfile()))

    def addState(s: State) {
      if (!seen(s)) {
        stateCount += 1
        Log.debug("addState:"+s)
        seen += s
        todo += s
        outSerializer.write(s.toPacket)

        seenMethods += s.stmt.sootMethod
      }
    }

    def addEdge(s1: State, s2: State) {
      Log.debug("addEdge:"+s1.id+" -> "+s2.id)
      edgeCount += 1
      val edge = serializer.Edge(
        serializer.Id[serializer.Edge](edgeCount),
        serializer.Id[serializer.Node](s1.id),
        serializer.Id[serializer.Node](s2.id))
      outSerializer.write(edge)
    }

    try {
      val mainMethod = Soot.getSootClass(conf.mainClass()).getMethodByName(conf.method())
      val initialState = State.inject(Stmt.methodEntry(mainMethod))
      val javaMethodsToAppMethods = mutable.Map.empty[SootMethod, Set[SootMethod]]
      def getReachableAppMethods(m: SootMethod, level: Option[Int]): Set[SootMethod] = {
        assert(System.isJavaLibraryClass(m.getDeclaringClass))
        def search(todo: Set[SootMethod], seen: Set[SootMethod],
                   result: Set[SootMethod], level: Option[Int]): Set[SootMethod] = {
          if (todo.isEmpty) return result
          level match {
            case None => result
            case Some(n) =>
              val outMethods = todo flatMap { (m: SootMethod) =>
                cg.edgesOutOf(m).toSet.map((e: Edge) => e.tgt.method)
              }
              val (jMethods, appMethods) = outMethods.partition((m: SootMethod) => System.isJavaLibraryClass(m.getDeclaringClass))
              val (_, notSeen) = jMethods.partition((m: SootMethod) => seen.contains(m))
              search(notSeen, seen++todo, appMethods++result, if (n==0) None else Some(n-1))
          }
        }

        if (javaMethodsToAppMethods.contains(m)) { return javaMethodsToAppMethods(m) }
        val appMethods = search(Set(m), Set.empty[SootMethod], Set.empty[SootMethod], level)
        javaMethodsToAppMethods += ((m, appMethods))
        appMethods
      }

      addState(initialState)
      while (todo.nonEmpty) {
        val state = todo.head
        todo = todo.tail
        Log.info("Processing:"+state)

        for (edge <- cg.edgesOutOf(state.stmt.sootStmt)) {
          val m = edge.tgt.method
          Log.debug("  -->" + m)
          if (m.isPhantom || m.isAbstract || m.isNative) {  }
          else if (System.isJavaLibraryClass(m.getDeclaringClass)) {
            // Here we construct spurious edges between `m` and reachable application methods
            // by stepping over searching the reachable methods with some level
            val rms = getReachableAppMethods(m, Some(1))
            for (rm <- rms) {
              Log.info(s" inside of $m calls $rm")
              val entryState = State.inject(Stmt.methodEntry(rm))
              addState(entryState)
              addEdge(state, entryState)
            }
          }
          else {
            val entryState = State.inject(Stmt.methodEntry(m))
            addState(entryState)
            addEdge(state, entryState)
          }
        }

        if (state.stmt.sootStmt.isInstanceOf[ReturnStmt] ||
            state.stmt.sootStmt.isInstanceOf[ReturnVoidStmt]) {
          for (edge <- cg.edgesInto(state.stmt.sootMethod)) {
            // TODO: do something for non-explicit edges (they have a null stmt and unit) (probably are due to throwing exceptions)
            val clazz = edge.src.method.getDeclaringClass
            if (edge.isExplicit && !System.isJavaLibraryClass(clazz)) {
              Log.info(s"  goes back to ${edge.src.method}")
              val returnState = State.inject(Stmt(edge.srcStmt, edge.src).nextSyntactic)
              addState(returnState)
              addEdge(state, returnState)
            }
          }
        } else {
          for (stmt <- state.stmt.nextSemantic) {
            val newState = State.inject(stmt)
            addState(newState)
            addEdge(state, newState)
          }
        }
      }
    } finally {
      outSerializer.close()

      val allAppMethods = Utilities.getAllClasses(conf.classpath().toString)
                          .map(Scene.v().loadClassAndSupport(_))
                          .flatMap(_.getMethods)
      val seenAndInJar = seenMethods.intersect(allAppMethods)
      val seenButNotInJar = seenMethods.diff(allAppMethods)
      val inJarButNotSeen = allAppMethods.diff(seenMethods)
      Log.info(s"number of states: ${stateCount}")
      Log.info(s"number of edges: ${edgeCount}")
      Log.info(s"number of covered methods: ${seenMethods.size}")
      Log.info(s"number of methods in app's jar file: ${allAppMethods.size}")
      Log.info(s"number of methods covered and in jar file: ${seenAndInJar.size}")
      Log.info(s"number of methods covered but not in jar file: ${seenButNotInJar.size}")
      Log.info(s"number of methods not covered but in jar file: ${inJarButNotSeen.size}")

      // Many of the methods not covered but appeared in jar file are maybe never been called,
      // we can safely omit these methods if the call graph is correct.
      val (called, notCalled) = inJarButNotSeen.partition(cg.edgesInto(_).nonEmpty)
      Log.info(s"number of methods not covered but in jar file and have been called: ${called.size}")
      Log.info(s"real method coverage: ${seenAndInJar.size.toDouble/(allAppMethods.size-notCalled.size)}")

      // Print methods not covered but may have been called
      for (m <- called) {
        val into = cg.edgesInto(m).toList
        Log.info(s"  number of edges into ${m}: ${into.size}")
        for (edge <- into) {
          val caller = edge.src.method
          Log.debug(s"    may called by: ${caller}")
        }
      }
    }
  }

  def aam() {
    val startInstant = java.time.Instant.now

    Main.conf.initialStore.toOption match {
      case Some(f) => System.loadStore(f)
      case None => {}
    }

    Soot.initialize(conf.rtJar().toString, conf.classpath().toString)

    // TODO: libClasses option?
    System.setAppLibraryClasses(conf.libClasses())

    val outSerializer = new serializer.PacketOutput(new FileOutputStream(conf.outfile()))
    val mainMainMethod : SootMethod = Soot.getSootClass(conf.mainClass()).getMethodByName(conf.method())
    val initialState = State.inject(Stmt.methodEntry(mainMainMethod)) // TODO: we should initialize the main class
    // TODO: check if we are initializing superclasses
    // TODO: check if we initialize on java.lang.Class

    var visitCounts: Map[AbstractState.Id, Int] = Map.empty

    var done: Set[AbstractState.Id] = Set.empty
    var doneEdges: Map[(Int, Int), Int] = Map.empty

    val todo: mutable.PriorityQueue[AbstractState] = mutable.PriorityQueue()(
      conf.stateOrdering()(conf.globalSnowflakeAddrLast()))
    todo.enqueue(initialState)
    outSerializer.write(initialState.toPacket)

    var steps = 0
    var realSteps = 0
    try {
      while (todo.nonEmpty && !conf.maxSteps.toOption.exists(steps >= _)) {
        steps += 1
        val current = todo.dequeue

        // Remove any duplicates
        while (todo.headOption match {
          case Some(s) => s.id == current.id
          case None => false}) {
          todo.dequeue
        }

        if (!done.contains(current.id)) {
          realSteps += 1
          val currentCount = visitCounts.getOrElse(current.id, 0) + 1
          visitCounts += current.id -> currentCount
          Log.info(s"Processing step $steps and real step $realSteps and state ${current.id} for ${currentCount}th time with ${todo.size} states remaining: " +
            (current match { case s : State => s.stmt.toString; case s => s.toString}))
          val nexts = System.next(current)
          val newTodo = nexts.filter({x => !done.contains(x.id)})

          for (n <- newTodo) {
            Log.info("Writing state "+n.id)
            outSerializer.write(n.toPacket)
          }

          for (n <- nexts) {
            if (!doneEdges.contains((current.id, n.id))) {
              val id = doneEdges.size // TODO: Should create an creating object so these are consistent about 0 vs 1 based
              Log.info(s"Writing edge $id: ${current.id} -> ${n.id}")
              doneEdges += (current.id, n.id) -> id
              outSerializer.write(serializer.Edge(serializer.Id[serializer.Edge](id), serializer.Id[serializer.Node](current.id), serializer.Id[serializer.Node](n.id)))
            } else {
              Log.info(s"Skipping edge ${current.id} -> ${n.id}")
            }
          }

          for (w <- current.getWriteAddrs; s <- System.readTable.getOrElse(w, Set())) {
            done -= s.id
            todo += s
          }
          for (w <- current.getKWriteAddrs; s <- System.readKTable.getOrElse(w, Set())) {
            done -= s.id
            todo += s
          }

          if (System.isInitializedClassesChanged) {
            todo += current
            todo ++= newTodo
          }
          else {
            done += current.id
            todo ++= newTodo
          }

          Log.info(s"Done processing step $steps and state ${current.id}")
        }
      }
    }
    finally {
      outSerializer.close()
    }

    Log.error(s"Done!")
    // TODO: this is wrong if the process is suspended.  Maybe use com.sun.management.OperatingSystemMXBean
    val duration = java.time.Duration.between(startInstant, java.time.Instant.now())
    Log.error(f"Steps: $steps")
    // TODO: number of states
    // TODO: number of edges
    Log.error(f"Time: ${duration.toHours}%d:${duration.toMinutes % 60}%02d:${duration.getSeconds%60}%02d.${duration.toMillis%1000}%03d")
    Log.error("Total memory: " + Runtime.getRuntime.totalMemory/1024/1024 + " MB")
    Log.error("Unused memory: " + Runtime.getRuntime.freeMemory/1024/1024 + " MB")
    Log.error("Used memory: " + (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory)/1024/1024 + " MB")
/* TODO: if we need more precise info (e.g. peak memory)
    for (mp <- java.lang.management.ManagementFactory.getMemoryPoolMXBeans()) {
      Log.error("")
      Log.error(f"Name: " + mp.getName)
      Log.error(f"Memory: " + mp.getUsage) //${mp.getInit} ${mp.getUsed} ${mp.getCommitted} ${mp.getMax}")
      Log.error(f"Peak: " + mp.getPeakUsage) //${mp.getInit} ${mp.getUsed} ${mp.getCommitted} ${mp.getMax}")
      Log.error(f"Collection: " + mp.getCollectionUsage) //${mp.getInit} ${mp.getUsed} ${mp.getCommitted} ${mp.getMax}")
    }
 */

    if (conf.maxSteps.toOption.exists(steps >= _)) {
      Log.error(s"Exceeded maximum state limit (${conf.maxSteps()})")
    }

    Log.error(s"Initialized classes: ${System.initializedClasses.size}")
    for (c <- System.initializedClasses.map(_.getName).toSeq.sorted) {
      Log.error("Initialized class: " + c)
    }

    if (System.undefined != 0) {
      Log.error(s"Undefined address number: ${System.undefined}")
    }

    Log.error(s"States with no successor: ${System.noSucc.size}")
    for (state <- System.noSucc) {
      Log.error("State " + state.id + " has no successors: " + state)
    }
  }
}

// TODO: test load all of the preloaded store

// TODO: undefined addrs in state 12
// 00:16  INFO: Processing step 147 and state 126 with 7 states remaining: <java.util.Hashtable: java.lang.Object get(java.lang.Object)>:5:$i2 = lengthof r2
// ESC[31m00:16 ERROR: Undefined Addrs in state 126; stmt = <java.util.Hashtable: java.lang.Object get(java.lang.Object)>:5:$i2 = lengthof r2; addrs = Set(LocalFrameAddr(ZeroCFAFramePointer(<java.util.Hashtable: java.lang.Object get(java.lang.Object)>),r2))ESC[0m
// 00:16  INFO: Done processing step 147 and state 126
