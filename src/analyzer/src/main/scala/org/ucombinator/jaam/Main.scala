package org.ucombinator.jaam

/*
  Warning:

  1. Don't cross the streams.  It would be bad.

  2. Don't order addresses.  Dogs and cats, living together.  Mass hysteria.
*/

// TODO: need to track exceptions that derefing a Null could cause
// TODO: invoke main method instead of jumping to first instr so static init is done correctly
// TODO: invoke main method so we can initialize the parameters to main

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.{Map, TreeSet}
import scala.language.postfixOps
import scala.reflect.ClassTag
import xml.Utility

import org.ucombinator.jaam.Stmt.unitToStmt

// We expect every Unit we use to be a soot.jimple.Stmt, but the APIs
// are built around using Unit so we stick with that.  (We may want to
// fix this when we build the Scala wrapper for Soot.)
import soot._
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, Type => SootType}

import soot.jimple._
import soot.jimple.{Stmt => SootStmt}
import soot.jimple.internal.JStaticInvokeExpr
import soot.tagkit._
import soot.jimple.toolkits.invoke.AccessManager


// JGraphX
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager

import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.view.mxGraph
import com.mxgraph.layout.mxCompactTreeLayout

// JSON4s
import org.json4s._

import java.io.FileOutputStream
import java.io.FileInputStream
import org.ucombinator.jaam.messaging
import org.ucombinator.jaam.messaging.{State => MState, AbstractState => MAbstractState, ErrorState => MErrorState, Edge => MEdge, Id, Message, Done}

// Possibly thrown during transition between states.
case class UninitializedClassException(sootClass : SootClass) extends RuntimeException
case class StringConstantException(string : String) extends RuntimeException
case class UndefinedAddrsException[A <: Addr](addrs : Set[A]) extends RuntimeException

// A continuation store paired with a continuation
case class KontStack(k : Kont) {
  var store : KontStore = KontStore(Map())
  def setStore(store : KontStore) : scala.Unit = {
    this.store = store
  }
  def getStore() = store
  def copyKontStack(store: KontStore = this.store): KontStack = {
    val ks = this.copy()
    ks.setStore(store)
    ks
  }

  // TODO/generalize: Add widening in push and pop (to support PDCFA)
  // TODO/generalize: We need the control pointer of the target state to HANDLE PDCFA.
  def kalloca(frame : Frame) = OneCFAKontAddr(frame.fp)

  // TODO/generalize: Handle PDCFA
  def push(frame : Frame) : KontStack = {
    val kAddr = kalloca(frame)
    val newKontStore = store.update(kAddr, KontD(Set(k))).asInstanceOf[KontStore]
    val newKontStack = KontStack(RetKont(frame, kAddr))
    newKontStack.setStore(newKontStore)
    newKontStack
  }

  // Pop returns all possible frames beneath the current one.
  def pop() : Set[(Frame, KontStack)] = {
    k match {
      case RetKont(frame, kontAddr) => {
        for (topk <- store(kontAddr).values) yield {
          val ks = KontStack(topk)
          ks.setStore(store)
          (frame, ks)
        }
      }
      case HaltKont => Set()
    }
  }

  def handleException(exception : Value,
                      stmt : Stmt,
                      fp : FramePointer,
                      store : Store,
                      initializedClasses : Set[SootClass]) : Set[AbstractState] = {
    if (!exception.isInstanceOf[ObjectValue])
      throw new Exception("Impossible throw: stmt = " + stmt + "; value = " + exception)

    var visited = Set[(Stmt, FramePointer, KontStack)]()

    // TODO/performance: Make iterative.
    def stackWalk(stmt : Stmt, fp : FramePointer, kontStack : KontStack) : Set[AbstractState] = {
      if (visited.contains((stmt, fp, kontStack))) return Set()

      visited = visited + ((stmt, fp, kontStack)) // TODO: do we really need all of these in here?

      for (trap <- TrapManager.getTrapsAt(stmt.sootStmt, Soot.getBody(stmt.sootMethod))) {
        val caughtType = trap.getException()
        // The handler will expect the exception to be waiting at CaughtExceptionFrameAddr(fp).
        // It'll be referenced through CaughtExceptionRef.

        store.update(CaughtExceptionFrameAddr(fp), D(Set(exception)))

        // TODO/soundness or performance?: use Hierarchy or FastHierarchy?
        if (Soot.isSubclass(exception.asInstanceOf[ObjectValue].sootClass, caughtType)) {
          val newState = State(stmt.copy(sootStmt = trap.getHandlerUnit()), fp, this.copyKontStack())
          newState.setInitializedClasses(initializedClasses)
          return Set(newState)
        }
      }

      // TODO/interface we should log this sort of thing
      val nextFrames = kontStack.pop()
      if (nextFrames isEmpty) {
        return Set(ErrorState)
      }
      (for ((frame, kontStack) <- nextFrames) yield { stackWalk(frame.stmt, frame.fp, kontStack) }).flatten
    }

    stackWalk(stmt, fp, this)
    // TODO/soundness: deal with unhandled exceptions
  }
}

case class Frame( val stmt : Stmt,
                  val fp : FramePointer,
                  val destAddr : Option[Set[Addr]]) {
  def acceptsReturnValue() : Boolean = !destAddr.isEmpty
}

abstract class Kont

case class RetKont(
                    val frame : Frame,
                    val k : KontAddr
                  ) extends Kont

object HaltKont extends Kont

//----------- ABSTRACT VALUES -----------------

// TODO/precision D needs to have an interface that allows eval to use it


abstract class Value

abstract class AtomicValue extends Value

case object AnyAtomicValue extends AtomicValue

case class ObjectValue(val sootClass : SootClass, val bp : BasePointer) extends Value

case class ArrayValue(val sootType : SootType, val bp : BasePointer) extends Value

//case class SnowflakeInterfaceValue(val sootClass : SootClass, val bp : BasePointer) extends Value

//----------------- POINTERS ------------------

// FramePointers, when paired with variable names, yield the addresses of variables.
abstract class FramePointer
case object InvariantFramePointer extends FramePointer // TODO: delete this?  Is seems unused
case class ZeroCFAFramePointer(val method : SootMethod) extends FramePointer
case class OneCFAFramePointer(val method : SootMethod, val nextStmt : Stmt) extends FramePointer
case object InitialFramePointer extends FramePointer

abstract class From
case object FromNative extends From
case object FromJava extends From

// BasePointers, when paired with field names, yield the addresses of fields.
abstract class BasePointer
case class OneCFABasePointer(stmt : Stmt, fp : FramePointer, from : From) extends BasePointer
case object InitialBasePointer extends BasePointer
// Note that due to interning, strings and classes may share base pointers with each other
// Oh, and class loaders are a headache(!)
case class StringBasePointer(val string : String) extends BasePointer
// we remvoe the argument of ClassBasePointer, to make all ClassBasePointer points to the same
case class ClassBasePointer(val name : String) extends BasePointer
//case object ClassBasePointer extends BasePointer

//----------------- ADDRESSES ------------------

// Addresses of continuations on the stack
abstract class KontAddr extends Addr
case class OneCFAKontAddr(val fp : FramePointer) extends KontAddr

abstract class Addr
abstract class FrameAddr extends Addr

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

abstract class AbstractDomain[T](val values: Set[T]) {
  def join[M <: AbstractDomain[T] : ClassTag](d: M): M = {
    implicitly[ClassTag[M]].runtimeClass.getConstructors.head.newInstance(values++d.values).asInstanceOf[M]
  }
}

case class KontD(override val values: Set[Kont]) extends AbstractDomain[Kont](values)
object KontD {
  val serializer = new CustomSerializer[KontD](implicit format => (
    { case s => ??? },
    { case KontD(values) => Extraction.decompose(values) }
    ))
}

case class D(override val values: Set[Value]) extends AbstractDomain[Value](values) {
  def maybeZero() : Boolean = values.exists(_.isInstanceOf[AtomicValue])
}
object D {
  val atomicTop = D(Set(AnyAtomicValue))
  val serializer = new CustomSerializer[D](implicit format => (
    { case s => ??? },
    { case D(values) => Extraction.decompose(values) }
    ))
}

// Due to a bug in json4s we have to use "Int" instead of "AbstractState.Id" for the keys of this map
// Once the following pull request is accepted we can use "AbstractState.Id":
//   https://github.com/json4s/json4s/pull/324
case class UpdatePacket(states : Map[Int,AbstractState], edges : Set[(AbstractState.Id, AbstractState.Id)]) {
  def nonEmpty = states.nonEmpty || edges.nonEmpty
}
object UpdatePacket {
  val serializer = new CustomSerializer[UpdatePacket](implicit format => (
    { case s => ??? },
    { case (i : AbstractState.Id, j : AbstractState.Id) => Extraction.decompose(List(i,j)) }
    ))
}

abstract sealed class AbstractState {
  def next() : Set[AbstractState]
  def setStore(store: Store) : scala.Unit
  def getStore() : Store

  def setKontStore(store : KontStore) : scala.Unit
  def getKontStore() : KontStore

  def getReadAddrs: Set[Addr]
  def setReadAddrs(s: Set[Addr]): scala.Unit

  def getKReadAddrs: Set[KontAddr]
  def setKReadAddrs(s: Set[KontAddr]): scala.Unit

  def getWriteAddrs: Set[Addr]
  def setWriteAddrs(s: Set[Addr]): scala.Unit

  def getKWriteAddrs: Set[KontAddr]
  def setKWriteAddrs(s: Set[KontAddr]): scala.Unit

  def setInitializedClasses(classes: Set[SootClass]) : scala.Unit
  def getInitializedClasses() : Set[SootClass]

  def toMessage() : MAbstractState

  val id = AbstractState.idMap.getOrElseUpdate(this, AbstractState.nextId)
}

object AbstractState {
  type Id = Int
  val idMap = scala.collection.mutable.Map[AbstractState, Id]()
  var nextId_ = 0
  def nextId() : Id = { nextId_ += 1; nextId_ }
}

case object ErrorState extends AbstractState {
  override def next() : Set[AbstractState] = Set.empty
  override def setStore(store : Store) = scala.Unit
  override def getStore() = Store(Map())
  override def setKontStore(store : KontStore) = scala.Unit
  override def getKontStore() = KontStore(Map())
  override def getReadAddrs = Set()
  override def setReadAddrs(s: Set[Addr]) = scala.Unit
  override def getKReadAddrs: Set[KontAddr] = Set()
  override def setKReadAddrs(s: Set[KontAddr]) = scala.Unit
  override def getWriteAddrs = Set()
  override def setWriteAddrs(s: Set[Addr]) = scala.Unit
  override def getKWriteAddrs: Set[KontAddr] = Set()
  override def setKWriteAddrs(s: Set[KontAddr]) = scala.Unit
  override def setInitializedClasses(classes: Set[SootClass]) = scala.Unit
  override def getInitializedClasses() : Set[SootClass] = Set()
  override def toMessage() = messaging.ErrorState(Id[messaging.AbstractState](id))
}

// State abstracts a collection of concrete states of execution.
case class State(val stmt : Stmt,
                 val fp : FramePointer,
                 //val store : Store,
                 val kontStack : KontStack
                 /*val initializedClasses : Set[SootClass]*/) extends AbstractState {
  // Needed because different "stores" should lead to different objects
  //  override def equals(that: Any): Boolean =
  //    that match {
  //      case that: State => this.stmt == that.stmt && this.fp == that.fp && this.kontStack == that.kontStack && this.store == that.store
  //      case _ => false
  //   }

  var store: Store = Store(Map())

  override def setStore(store : Store) : scala.Unit = this.store = store
  override def getStore(): Store = store
  override def getKontStore = kontStack.getStore()
  override def setKontStore(store : KontStore) = kontStack.setStore(store)

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

  var initializedClasses = Set[SootClass]()
  override def setInitializedClasses(classes: Set[SootClass]) = {
    initializedClasses = classes
  }
  override def getInitializedClasses() : Set[SootClass] = initializedClasses

  override def toMessage() = messaging.State(Id[messaging.AbstractState](id), stmt.toMessage, fp.toString, kontStack.toString)

  def copyState(stmt: Stmt = stmt,
                fp: FramePointer = fp,
                kontStack: KontStack = kontStack,
                store: Store = this.store,
                kontStore: KontStore = this.kontStack.getStore(),
                initializedClasses: Set[SootClass] = initializedClasses
               ): State = {
    val newState = this.copy(stmt, fp, kontStack)
    newState.setStore(store)
    newState.setKontStore(kontStore)
    newState.setInitializedClasses(initializedClasses)
    newState
  }

  // When you call next, you may realize you are going to end up throwing some exceptions
  // TODO/refactor: Consider turning this into a list of exceptions.
  var exceptions = D(Set())

  // Allocates a new frame pointer (currently uses 1CFA)
  def alloca(expr : InvokeExpr, nextStmt : Stmt) : FramePointer = ZeroCFAFramePointer(expr.getMethod)
  // Allocates objects
  def malloc() : BasePointer = OneCFABasePointer(stmt, fp, FromJava)
  def mallocFromNative() : BasePointer = OneCFABasePointer(stmt, fp, FromNative)

  // Any time you reference a class, you have to run this to make sure it's initialized.
  // If it isn't, the exception should be caught so the class can be initialized.
  def checkInitializedClasses(c : SootClass) {
    if (!initializedClasses.contains(c)) {
      throw new UninitializedClassException(c)
    }
  }
  def checkInitializedClasses(t : SootType) {
    t match {
      case at : ArrayType => checkInitializedClasses(at.baseType)
      case pt : PrimType => {}
      case rt : RefType => checkInitializedClasses(rt.getSootClass)
    }
  }

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
      val d : D = eval(b)
        // TODO/optimize
        // filter out incorrect class types
        // TODO/bug
        // Suppose a number flows to x in x.y = 3;
        for (ObjectValue(_, bp) <- d.values)
          yield InstanceFieldAddr(bp, lhs.getField)
      case lhs : StaticFieldRef =>
        val f : SootField = lhs.getField
        val c : SootClass = f.getDeclaringClass
        checkInitializedClasses(c)
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
        for (ArrayValue(_, bp) <- b.values) yield
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
      //TODO missing: CmplExpr, CmpgExpr, MethodHandle
      //TODO/precision actually do the calculations
      case (_ : Local) | (_ : Ref) => store(addrsOf(v))
      case _ : NullConstant => D.atomicTop
      case _ : NumericConstant => D.atomicTop
      // TODO: Class and String objects are objects and so need their fields initialized
      // TODO/clarity: Add an example of Java code that can trigger this.
      case v : ClassConstant => D(Set(ObjectValue(Soot.classes.Class, ClassBasePointer(v.value.replace('/', '.')))))
      //D(Set(ObjectValue(stmt.classmap("java.lang.Class"), StringBasePointer(v.value))))
      case v : StringConstant =>
        val bp = StringBasePointer(v.value)
        if (store.contains(InstanceFieldAddr(bp, Soot.classes.String.getFieldByName("value")))) {
          D(Set(ObjectValue(Soot.classes.String, bp)))
        } else {
          throw StringConstantException(v.value)
        }
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
          ArrayValue(_, bp) <- eval(v.getOp).values
        } yield ArrayLengthAddr(bp)
        store(addrs)

      // TODO/precision: implement the actual check
      case v : InstanceOfExpr => D.atomicTop

      case v : CastExpr =>
        // TODO: cast from a SnowflakeObject to another SnowflakeObject
        val castedExpr : SootValue = v.getOp
        val castedType : SootType = v.getType
        checkInitializedClasses(castedType)
        val d = eval(castedExpr)
        //        println("CastExpr "+v+" "+d)
        // TODO: filter out elements of "d" that are not of the
        // expression's type (should be done in general inside "eval"
        // (and maybe already is))
        def isCastableTo(v : Value, t : SootType) : Boolean = {
          v match {
            case _ : AtomicValue => t.isInstanceOf[PrimType]
            case ObjectValue(sootClass, _) => Soot.isSubType(sootClass.getType, t)
            case ArrayValue(sootType, _) => Soot.isSubType(sootType, t)
            //            case SnowflakeInterfaceValue(sootClass, _) => Soot.isSubType(sootClass.getType, t)
          }
        }

        var d2 = D(Set())
        for (v <- d.values) {
          //          println("v:"+v+" castedType:"+castedType)
          if (isCastableTo(v, castedType)) {
            // Up casts are always legal
            d2 = d2.join(D(Set((v))))
          } else if (v.isInstanceOf[ObjectValue] && v.asInstanceOf[ObjectValue].bp.isInstanceOf[SnowflakeBasePointer] &&
            Soot.isSubType(castedType, v.asInstanceOf[ObjectValue].sootClass.getType)
          ) {
            //          } else if (v.isInstanceOf[SnowflakeInterfaceValue]
            //            && Soot.isSubType(castedType, v.asInstanceOf[SnowflakeInterfaceValue].sootClass.getType)) {
            // Snowflakes can be down cast, but they might throw an exception
            val classCastException = D(Set(ObjectValue(Soot.classes.ClassCastException, malloc())))
            exceptions = exceptions.join(classCastException)
            val name = castedType match {
              case r : RefType => r.getClassName
              case a : ArrayType => ???
            }
            // println("snowflake cast "+castedType+" v "+v)
            d2 = d2.join(Snowflakes.createObjectOrThrow(name))
          } else {
            // Anything else would cause an exception
            val classCastException = D(Set(ObjectValue(Soot.classes.ClassCastException, malloc())))
            exceptions = exceptions.join(classCastException)
          }
        }

        d2

      /*
      println("castExpr!!!!:", v)
              // TODO: cast from a SnowflakeObject to another SnowflakeObject
              val castedExpr : SootValue = v.getOp
              val castedType : SootType = v.getType
              checkInitializedClasses(castedType)
              val d = eval(castedExpr)
              // TODO: filter out elements of "d" that are not of the
              // expression's type (should be done in general inside "eval"
              // (and maybe already is))

              var vs = Set()

              for (v <- d.values) {
                if (Soot.isSubType(objectType.getType, castedType)) {
                  // Up casts are always legal
                  vs += v
                } else if (v.instanceOf[SnowflakeBasePointer]
                  && Soot.isSubType(castedType, objectType.getType)) {
                  // Snowflakes can be down cast, but they might throw an exception
                  val classCastException = D(Set(ObjectValue(Soot.classes.ClassCastaException, malloc())))
                  exceptions = exceptions.join(classCastException)
                  vs += Snowflake.createObjectOrThrow()
                } else {
                  // Anything else would cause an exception
                  val classCastException = D(Set(ObjectValue(Soot.classes.ClassCastaException, malloc())))
                  exceptions = exceptions.join(classCastException)
                }


      /*
                // Up cast or cast to self
                if (Soot.isSubType(objectType.getType, castedType)) {
                  vs += v
                }
                // Down cast
                else if (Soot.isSubType(castedType, objectType.getType) {
                  if
                  exceptions += ...
                  if
                }
                if (downcast) {
                  exception
                  convertsnowflake
                }
                v match {
                  case _ : AnyAtomicValue =>
                    if (castedType.isInstanceOf[PrimType]) {
                      d = d join D.atomicTop
                    }
                  case ObjectValue
                }
              }

              d2

              if (castedType.isInstanceOf[PrimType]) {
                return D.atomicTop
              }
      println(castedExpr)
      println(castedType)

              // TODO/soundness: Throw an exception if necessary.
              for (vl <- d.values) {
                vl match {
                  case ObjectValue(objectType, _) =>
      println("objectType=", objectType)
                    if (!Soot.isSubType(objectType.getType, castedType)) {
      println("isSubtype")
                      val classCastException = D(Set(ObjectValue(Soot.classes.ClassCastException, malloc())))
                      exceptions = exceptions.join(classCastException)
                    }
                  case ArrayValue(arrayType, basePointer) =>
                    if (!Soot.isSubType(arrayType, castedType)) {
                      val classCastException = D(Set(ObjectValue(Soot.classes.ClassCastException, malloc())))
                      exceptions = exceptions.join(classCastException)
                    }
                  case AnyAtomicValue => {}
                  case _ => throw new Exception ("Unknown value type " + vl)
                }
              }

              for (vl <- d.values) {
                vl match {
                  case ObjectValue(
                }
              }
              castedType match {
                case castedType : RefType =>
                  if (castedType.isJavaLibraryClass) {
                  }
                case _ => ???
              }
              if (castedType..isJavaLibraryClass
              if (javalib) { new java snowflake}
              else {        d }
            }
      */*/

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
    // o.f(3); // In this case, c is the type of o. m is f. receivers is the result of eval(o).
    // TODO/dragons. Here they be.
    def dispatch(bp : BasePointer, c : SootClass, m : SootMethod, receivers : Set[Value]) : Set[AbstractState] = {
      // This function finds all methods that could override root_m.
      // These methods are returned with the root-most at the end of
      // the list and the leaf-most at the head.  Thus the caller
      // should use the head of the returned list.  The reason a list
      // is returned is so this function can recursively compute the
      // transitivity rule in Java's method override definition.
      //
      // Note that Hierarchy.resolveConcreteDispath should be able to do this, but seems to be implemented wrong
      def overrides(curr : SootClass, root_m : SootMethod) : List[SootMethod] = {
        val curr_m = curr.getMethodUnsafe(root_m.getName, root_m.getParameterTypes, root_m.getReturnType)
        if (curr_m == null) {
          overrides(curr.getSuperclass(), root_m)
        }
        else if (root_m.getDeclaringClass.isInterface || AccessManager.isAccessLegal(curr_m, root_m)) { List(curr_m) }
        else {
          val o = overrides(curr.getSuperclass(), root_m)
          (if (o.exists(m => AccessManager.isAccessLegal(curr_m, m))) List(curr_m) else List()) ++ o
        }
      }

      val meth = if (c == null) m else overrides(c, m).head
      // TODO: put a better message when there is no getActiveBody due to it being a native method
      val newFP = alloca(expr, nextStmt)
      var newStore = store // TODO: currently update the store directly, not using newStore
      for (i <- 0 until expr.getArgCount())
        store.update(ParameterFrameAddr(newFP, i), eval(expr.getArg(i)))
      val newKontStack = kontStack.push(Frame(nextStmt, fp, destAddr))
      // TODO/optimize: filter out incorrect class types
      val th = ThisFrameAddr(newFP)
      for (r <- receivers) store.update(th, D(Set(r)))

      Snowflakes.get(meth) match {
        case Some(h) => h(this, nextStmt, newFP, store, newKontStack)
        case None =>
          if (meth.getDeclaringClass.isJavaLibraryClass ||
            bp.isInstanceOf[SnowflakeBasePointer]) {
            Snowflakes.warn(this.id, stmt, meth)
            val rtType = meth.getReturnType
            rtType match {
              case t: VoidType => NoOpSnowflake(this, nextStmt, newFP, store, newKontStack)
              case pt: PrimType => ReturnSnowflake(D.atomicTop)(this, nextStmt, newFP, store, newKontStack)
              case at: ArrayType =>
                ReturnArraySnowflake(at.baseType.toString, at.numDimensions)
                  .apply(this, nextStmt, newFP, store, newKontStack)
              case rt: RefType =>
                ReturnObjectSnowflake(rt.getClassName)(this, nextStmt, newFP, store, newKontStack)
            }
          }
          else if (meth.isNative) {
            println(Console.YELLOW + "!!!!! WARNING (in state "+this.id+"): Native method without a snowflake. May be unsound. !!!!!")
            println("!!!!! stmt = " + stmt + " !!!!!")
            println("!!!!! method = " + meth + " !!!!!" + Console.RESET)
            meth.getReturnType match {
              case _ : VoidType => Set(this.copyState(stmt = nextStmt))
              case _ : PrimType => {
                store.update(destAddr, D.atomicTop)
                Set(this.copyState(stmt = nextStmt))
              }
              case _ =>
                println(Console.RED + "!!!!! Native method returns an object.  Aborting. !!!!!" + Console.RESET)
                Set()
            }
          } else {
            val newState = State(Stmt.methodEntry(meth), newFP, newKontStack)
            newState.setInitializedClasses(initializedClasses)
            Set(newState)
          }
      }
    }

    expr match {
      case expr : DynamicInvokeExpr => ??? // TODO: Could only come from non-Java sources
      case expr : StaticInvokeExpr =>
        checkInitializedClasses(expr.getMethod().getDeclaringClass())
        dispatch(null, null, expr.getMethod(), Set())
      case expr : InstanceInvokeExpr =>
        val d = eval(expr.getBase())
        val vs = d.values filter {
          case ObjectValue(c,_) => Soot.isSubclass(c, expr.getMethod.getDeclaringClass)
          /*
                    case ObjectValue(c, SnowflakeBasePointer(_)) => Soot.isSubclass(c, expr.getMethod.getDeclaringClass)
                    case ObjectValue(_, HashMapSnowflakes.EntrySetOfHashMap(_)) => true
                    case ObjectValue(_, HashMapSnowflakes.IteratorOfEntrySetOfHashMap(_)) => true
                    case ObjectValue(_, HashMapSnowflakes.EntryOfIteratorOfEntrySetOfHashMap(_)) => true
                    case ObjectValue(_, HashMapSnowflakes.EntrySetOfHashMap(_)) => true
                    case ObjectValue(_, ArrayListSnowflakes.IteratorOfArrayList(_)) => true

                    case ObjectValue(sootClass, _) => {
          //            println("sootClass "+sootClass+" "+expr.getMethod().getDeclaringClass)
                      if (expr.getMethod().getDeclaringClass.isInterface) {
                        val imps = Scene.v().getActiveHierarchy.getImplementersOf(expr.getMethod().getDeclaringClass)
                        imps.contains(sootClass)
                        //true
                      }
                      else {
                        Soot.isSubclass(sootClass, expr.getMethod().getDeclaringClass)
                      }
                    }
           */
          case ArrayValue(sootType, _) => {
            //TODO Check Type
            true
          }
          case _ => false
        }
        ((for (v <- vs) yield {
          v match {
            case ObjectValue(sootClass, bp) => {
              val objectClass = if (expr.isInstanceOf[SpecialInvokeExpr]) null else sootClass
              dispatch(bp, objectClass, expr.getMethod(), vs)
            }
            case ArrayValue(sootType, _) => {
              dispatch(null, null, expr.getMethod, vs)
            }
          }
        }) :\ Set[AbstractState]())(_ ++ _) // TODO: better way to do this?
    }
  }

  // If you reference an unititialized field, what should it be?
  def defaultInitialValue(t : SootType) : D = {
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
        case t : StringConstantValueTag => return D(Set(ObjectValue(Soot.classes.String, StringBasePointer(t.getStringValue()))))
        case _ => ()
      }
    return defaultInitialValue(f.getType)
  }

  // Returns a Store containing the possibly nested arrays described
  // by the SootType t with dimension sizes 'sizes'
  def createArray(t : SootType, sizes : List[D], addrs : Set[Addr]) : Store = sizes match {
    // Should only happen on recursive calls. (createArray should never be called by a user with an empty list of sizes).
    case Nil => store.update(addrs, defaultInitialValue(t)).asInstanceOf[Store]
    case (s :: ss) => {
      val bp : BasePointer = malloc()
      // TODO/soundness: exception for a negative length
      // TODO/precision: stop allocating if a zero length
      // TODO/precision: separately allocate each array element
      createArray(t.asInstanceOf[ArrayType].getElementType(), ss, Set(ArrayRefAddr(bp)))
        .update(addrs, D(Set(ArrayValue(t, bp))))
        .update(ArrayLengthAddr(bp), s).asInstanceOf[Store]
    }
  }

  // Returns the set of successor states to this state.
  override def next() : Set[AbstractState] = {
    try {
      val nexts = true_next()
      val exceptionStates = (exceptions.values map {
        kontStack.handleException(_, stmt, fp, store, initializedClasses)
      }).flatten
      nexts ++ exceptionStates
    } catch {
      case UninitializedClassException(sootClass) =>
        println("Exception: Static initializing "+sootClass)
        // TODO/soundness: needs to also initialize parent classes
        exceptions = D(Set())
        val meth = sootClass.getMethodByNameUnsafe(SootMethod.staticInitializerName)

        if (meth != null) {
          if (meth.getDeclaringClass.isJavaLibraryClass) {
            val newState = this.copyState(initializedClasses=initializedClasses+sootClass)
            newState.handleInvoke(new JStaticInvokeExpr(meth.makeRef(),
              java.util.Collections.emptyList()), None, stmt)
          }
          else {
            // Initialize all static fields per JVM 5.4.2 and 5.5
            val staticUpdates = for {
              f <- sootClass.getFields(); if f.isStatic
            } yield (StaticFieldAddr(f) -> staticInitialValue(f))
            //var newStore = store.update(staticUpdates.toMap)
            store.update(Map(staticUpdates.toMap.toSeq: _*))
            val newState = this.copyState(initializedClasses = initializedClasses+sootClass)
            newState.handleInvoke(new JStaticInvokeExpr(meth.makeRef(),
              java.util.Collections.emptyList()), None, stmt)
          }
        } else {
          // TODO: Do we need to do newStore for static fields?
          Set(this.copyState(initializedClasses = initializedClasses + sootClass))
        }

      case UninitializedSnowflakeObjectException(className) =>
        println("Exception: Initializing snowflake "+className)
        //val newStore = store.join(Snowflakes.createObject(className, List()))
        store.join(Snowflakes.createObject(className, List()))
        //Set(this.copyState(store = newStore)) //store=>store.join(Snowflakes.createObject(className, List()))))
        Set(this.copyState())

      case StringConstantException(string) =>
        println("Exception: Initializing string constant: \""+string+"\"")
        val value = Soot.classes.String.getFieldByName("value")
        val hash = Soot.classes.String.getFieldByName("hash")
        val hash32 = Soot.classes.String.getFieldByName("hash32")
        val bp = StringBasePointer(string)
        //val newStore =
        createArray(ArrayType.v(CharType.v,1), List(D.atomicTop/*string.length*/), Set(InstanceFieldAddr(bp, value)))
          .update(InstanceFieldAddr(bp, hash), D.atomicTop)
          .update(InstanceFieldAddr(bp, hash32), D.atomicTop)
        Set(this.copyState(/*store = newStore*/))

      case UndefinedAddrsException(addrs) =>
        println(Console.RED + "!!!!! ERROR (in state "+this.id+"): Undefined Addrs !!!!!")
        println("!!!!! stmt = " + stmt + " !!!!!")
        println("!!!!! addrs = " + addrs + " !!!!!" + Console.RESET)
        Set()
    }
  }

  def newExpr(lhsAddr : Set[Addr], sootClass : SootClass, store : Store) : Store = {
    val md = MethodDescription(sootClass.getName, SootMethod.constructorName, List(), "void")
    if (sootClass.isJavaLibraryClass || Snowflakes.contains(md)) {
      val obj = ObjectValue(sootClass, SnowflakeBasePointer(sootClass.getName))
      val d = D(Set(obj))
      var newStore = store.update(lhsAddr, d)
      newStore = newStore.join(Snowflakes.createObject(sootClass.getName, List()))
      newStore.asInstanceOf[Store]
    }
    else {
      val bp : BasePointer = malloc()
      val obj : Value = ObjectValue(sootClass, bp)
      val d = D(Set(obj))
      var newStore = store.update(lhsAddr, d)
      checkInitializedClasses(sootClass)
      // initialize instance fields to default values for their type
      def initInstanceFields(c : SootClass) {
        for (f <- c.getFields) {
          newStore = newStore.update(InstanceFieldAddr(bp, f), defaultInitialValue(f.getType))
        }
        // TODO: swap these lines?
        if(c.hasSuperclass) initInstanceFields(c.getSuperclass)
      }
      initInstanceFields(sootClass)
      newStore.asInstanceOf[Store]
    }
  }

  def true_next() : Set[AbstractState] = {
    stmt.sootStmt match {
      case sootStmt : InvokeStmt => handleInvoke(sootStmt.getInvokeExpr, None)

      case sootStmt : DefinitionStmt => {
        val lhsAddr = addrsOf(sootStmt.getLeftOp())
        sootStmt.getRightOp() match {
          case rhs : InvokeExpr => handleInvoke(rhs, Some(lhsAddr))
          case rhs : NewExpr => {
            val baseType : RefType = rhs.getBaseType()
            val sootClass = baseType.getSootClass()
            this.newExpr(lhsAddr, sootClass, store)
            Set(this.copyState(stmt = stmt.nextSyntactic/*store = this.newExpr(lhsAddr, sootClass, store)*/))
          }
          case rhs : NewArrayExpr => {
            //TODO, if base type is Java library class, call Snowflake.createArray
            // Value of lhsAddr will be set to a pointer to the array. (as opposed to the array itself)
            val newStore = createArray(rhs.getType(), List(eval(rhs.getSize())), lhsAddr)
            Set(this.copyState(stmt = stmt.nextSyntactic/*store = newStore*/))
          }
          case rhs : NewMultiArrayExpr => {
            //TODO, if base type is Java library class, call Snowflake.createArray
            //see comment above about lhs addr
            val newStore = createArray(rhs.getType(), rhs.getSizes().toList map eval, lhsAddr)
            Set(this.copyState(stmt = stmt.nextSyntactic/*store = newStore*/))
          }
          case rhs => {
            val newStore = store.update(lhsAddr, eval(rhs))
            Set(this.copyState(stmt = stmt.nextSyntactic/*store = newStore*/))
          }
        }
      }

      case sootStmt : IfStmt => {
        eval(sootStmt.getCondition()) //in case of side effects //TODO/precision evaluate the condition
        val trueState = this.copyState(stmt = stmt.copy(sootStmt = sootStmt.getTarget()))
        val falseState = this.copyState(stmt = stmt.nextSyntactic)
        Set(trueState, falseState)
      }

      case sootStmt : SwitchStmt =>
        //TODO/prrecision dont take all the switches
        sootStmt.getTargets().map(t => this.copyState(stmt = stmt.copy(sootStmt = t))).toSet

      case sootStmt : ReturnStmt => {
        val evaled = eval(sootStmt.getOp())
        for ((frame, newStack) <- kontStack.pop) yield {
          if (frame.acceptsReturnValue()) {
            store.update(frame.destAddr.get, evaled)
          }

          State(frame.stmt, frame.fp, newStack)
        }
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

      case sootStmt : GotoStmt => Set(this.copyState(stmt = stmt.copy(sootStmt = sootStmt.getTarget())))

      // For now we don't model monitor statements, so we just skip over them
      // TODO/soundness: In the event of multi-threaded code with precise interleaving, this is not sound.
      case sootStmt : EnterMonitorStmt => Set(this.copyState(stmt = stmt.nextSyntactic))
      case sootStmt : ExitMonitorStmt => Set(this.copyState(stmt = stmt.nextSyntactic))

      // TODO: needs testing
      case sootStmt : ThrowStmt => { exceptions = exceptions.join(eval(sootStmt.getOp())); Set() }

      // TODO: We're missing BreakPointStmt and RetStmt (but these might not be used)
      case _ => {
        throw new Exception("No match for " + stmt.sootStmt.getClass + " : " + stmt.sootStmt)
      }
    }
  }
}

// TODO/refactor: Maybe allow user-specified arguments.
object State {
  val initialFramePointer = InitialFramePointer
  val initialBasePointer = InitialBasePointer
  val stringClass : SootClass = Soot.classes.String
  val initial_map : Map[Addr, D] = Map(
    ParameterFrameAddr(initialFramePointer, 0) ->
      D(Set(ArrayValue(stringClass.getType(), initialBasePointer))),
    ArrayRefAddr(initialBasePointer) -> D(Set(ObjectValue(stringClass, initialBasePointer))),
    ArrayLengthAddr(initialBasePointer) -> D.atomicTop)

  def inject(stmt : Stmt) : State = {
    val ks = KontStack(HaltKont)
    ks.setStore(KontStore(Map()))
    State(stmt, initialFramePointer, ks)
  }
}

object System {
  val store: Store = Store(State.initial_map)
  val kstore: KontStore = KontStore(Map[KontAddr, KontD]())
  val readTable: mutable.Map[Addr, Set[AbstractState]] = Map[Addr, Set[AbstractState]]()
  val readKTable: mutable.Map[KontAddr, Set[AbstractState]] = Map[KontAddr, Set[AbstractState]]()

  private def addToMultiMap[K, V](table: mutable.Map[K, Set[V]])(key: K, value: V) = {
    table.get(key) match {
      case Some(vals) => table += (key -> (vals+value))
      case None => table += (key -> Set(value))
    }
  }
  def addToReadTable = addToMultiMap(readTable)(_, _)
  def addToReadKTable = addToMultiMap(readKTable)(_, _)

  def next(state: AbstractState,
           initializedClasses: Set[SootClass]): (Set[AbstractState], Set[SootClass]) = {
    state.setStore(store)
    state.setKontStore(kstore)
    state.setInitializedClasses(initializedClasses)

    store.resetReadAddrsAndWriteAddrs
    kstore.resetReadAddrsAndWriteAddrs

    store.on = true
    kstore.on = true
    val nexts = state.next()
    store.on = false
    kstore.on = false

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

    val newInitClasses = nexts.par.map(_.getInitializedClasses()).foldLeft(Set[SootClass]())(_.++(_))

    (nexts, newInitClasses)
  }
}

// Command option config
case class Config(
                   rtJar: String = null,
                   sootClassPath: String = null,
                   className: String = null,
                   methodName: String = null)

object Main {
  def main(args : Array[String]) {
    val parser = new scopt.OptionParser[Config]("jaam") {
      head("jaam")

      opt[String]("classpath") action {
        (x, c) => c.copy(sootClassPath = x)
      } text("the TODO class directory")

      opt[String]('J', "rt_jar") action {
        (x, c) => c.copy(rtJar = x)
      } text("the rt.jar file")

      opt[String]('c', "class") action {
        (x, c) => c.copy(className = x)
      } text("the main class")

      opt[String]('m', "method") action {
        (x, c) => c.copy(methodName = x)
      } text("the main method")

      help("help") text("prints this usage text")

      override def showUsageOnError = true
    }

    parser.parse(args, Config()) match {
      case None =>
        println("Wrong arguments")

      case Some(config) =>
        Soot.initialize(config)
        defaultMode(config)
    }
  }

  def defaultMode(config : Config) {
    val outMessaging = Message.openOutput(new FileOutputStream("test.dat"))
    val mainMainMethod : SootMethod = Soot.getSootClass(config.className).getMethodByName(config.methodName)

    // Setting up the GUI
    //val window = new Window
    val initialState = State.inject(Stmt.methodEntry(mainMainMethod))
    //window.addState(initialState)

    var todo: List[AbstractState] = List(initialState)
    var done: Set[AbstractState] = Set()
    var globalInitClasses: Set[SootClass] = Set()
    var doneEdges: Map[Pair[Int, Int], Int] = Map()

    Message.write(outMessaging, initialState.toMessage)
    implicit val formats = Soot.formats +
      UpdatePacket.serializer +
      D.serializer +
      Store.serializer +
      KontStore.serializer
    //println(Serialization.writePretty(UpdatePacket(Map(1 -> initialState), Set.empty)))

    while (todo nonEmpty) {
      //TODO refactor store widening code
      val current = todo.head
      println()
      println("Processing state " + current.id+": "+(current match { case s : State => s.stmt.toString; case s => s.toString}))
      val (nexts, initClasses) = System.next(current, globalInitClasses)
      //for (next <- nexts) {
      //  window.addNext(current, next)
      //}

      val newTodo = nexts.toList.filter(!done.contains(_))

      for (n <- newTodo) {
        println("Writing state "+n.id)
        Message.write(outMessaging, n.toMessage)
      }
      for (n <- nexts) {
        if (!doneEdges.contains((current.id, n.id))) {
          val id = doneEdges.size
          println("Writing edge "+id+": "+current.id+" -> "+n.id)
          doneEdges += (current.id, n.id) -> id
          Message.write(outMessaging, messaging.Edge(Id[MEdge](id), Id[messaging.AbstractState](current.id), Id[messaging.AbstractState](n.id)))
        } else {
          println("Skipping edge "+current.id+" -> "+n.id)
        }
      }

      for (w <- current.getWriteAddrs; s <- System.readTable(w)) {
        done -= s
        todo :+= s
      }
      for (w <- current.getKWriteAddrs; s <- System.readKTable(w)) {
        done -= s
        todo :+= s
      }

      if ((globalInitClasses++initClasses).size != globalInitClasses.size) {
        todo = newTodo ++ List(current) ++ todo.tail
      }
      else {
        done += current
        todo = newTodo ++ todo.tail
      }

      globalInitClasses ++= initClasses
      println("Done processing state "+current.id+": "+(current match { case s : State => s.stmt.toString; case s => s.toString}))
    }

    outMessaging.close()
    println("Done!")
  }
}

case class CallGraph(val map : Map[Stmt, CallGraphValue])
case class CallGraphValue(val targets : List[Stmt], val successors : List[Stmt])

class Window extends JFrame ("Shimple Analyzer") {
  // TODO: make exiting window go back to repl
  // TODO: save graph files to review later
  val graph = new mxGraph() {
    override def getToolTipForCell(cell : Object) : String = {
      vertexToState.get(cell) match {
        case None => super.getToolTipForCell(cell)
        case Some(state : State) =>
          var tip = "<html>"
          tip += "FP: " + Utility.escape(state.fp.toString) + "<br><br>"
          tip += "Kont: " + Utility.escape(state.kontStack.k.toString) + "<br><br>"
          tip += "Store:<br>" + state.store.prettyString.foldLeft("")(_ + "&nbsp;&nbsp;" + Utility.escape(_) + "<br>") + "<br>"
          tip += "KontStore:<br>" + state.kontStack.store.prettyString.foldLeft("")(_ + "&nbsp;&nbsp;" + Utility.escape(_) + "<br>")
          //tip += "InitializedClasses: " + state.initializedClasses.toString + "<br>"
          tip += "</html>"
          tip
        case Some(ErrorState) => "ERROR"
      }
    }
  }


  private val layoutX = new mxCompactTreeLayout(graph, false)
  private val parentX = graph.getDefaultParent()
  private val graphComponent = new mxGraphComponent(graph)
  private var stateToVertex = Map[AbstractState,Object]()
  private var vertexToState = Map[Object,AbstractState]()
  private var edgeSet = Set[(AbstractState, AbstractState)]()

  graphComponent.setEnabled(false)
  graphComponent.setToolTips(true)
  getContentPane().add(graphComponent)
  ToolTipManager.sharedInstance().setInitialDelay(0)
  ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE)
  setSize(400, 320)
  setExtendedState(java.awt.Frame.MAXIMIZED_BOTH)
  setVisible(true)

  private def stateString(state : AbstractState) : String = state match {
    // TODO/interface more information
    case ErrorState => "ErrorState"
    case state: State =>
      state.id + "\n" + state.stmt.sootMethod.toString() + "\n" + state.stmt.sootStmt.toString()
  }

  def addState(state : State) {
    val vertex = graph.insertVertex(parentX, null, stateString(state), 100, 100, 20, 20, "ROUNDED")
    graph.updateCellSize(vertex)
    stateToVertex += (state -> vertex)
    vertexToState += (vertex -> state)
  }

  def addNext(start : AbstractState, end : AbstractState) {
    graph.getModel().beginUpdate()
    try {
      stateToVertex.get(end) match {
        case None =>
          val tag = stateString(end)
          val v = graph.insertVertex(parentX, null, tag, 240, 150, 80, 30, "ROUNDED")
          graph.updateCellSize(v)
          graph.insertEdge(parentX, null, null, stateToVertex(start), v)
          stateToVertex += (end -> v)
          vertexToState += (v -> end)
          edgeSet += ((start, end))
        case Some(v) =>
          if (!edgeSet.contains((start, end))) {
            graph.insertEdge(parentX, null, null, stateToVertex(start), v)
            edgeSet += ((start, end))
          }
      }
      // TODO: layout basic blocks together
      layoutX.execute(parentX)
    }
    finally
    {
      graph.getModel().endUpdate()
    }
  }
}

//future rendering of control flow graphs
/*
class CFG extends JFrame ("Control Flow Graph") {

  val graph = new mxGraph()

  private val layoutX = new mxCompactTreeLayout(graph, false)
  private val parentX = graph.getDefaultParent()
  private val graphComponent = new mxGraphComponent(graph)
  private var stateToVertex = Map[Block,Object]()
  private var vertexToState = Map[Object,Block]()


  graphComponent.setEnabled(false)
  graphComponent.setToolTips(true)
  getContentPane().add(graphComponent)
  ToolTipManager.sharedInstance().setInitialDelay(0)
  ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE)
  setSize(400, 320)
  setExtendedState(java.awt.Frame.MAXIMIZED_BOTH)
  setVisible(true)


   def addState(inst : Block) {
    val vertex = graph.insertVertex(parentX, null, inst, 100, 100, 20, 20, "ROUNDED")
    graph.updateCellSize(vertex)
    stateToVertex += (inst -> vertex)
    vertexToState += (vertex -> inst)
  }

   def addNext(start : Block, end : Block) {
    graph.getModel().beginUpdate()
    try {
      stateToVertex.get(end) match {
        case None =>
          //val tag = stateString(end)
          val v = graph.insertVertex(parentX, null, end, 240, 150, 80, 30, "ROUNDED")
          graph.updateCellSize(v)
          graph.insertEdge(parentX, null, null, stateToVertex(start), v)
          stateToVertex += (end -> v)
          vertexToState += (v -> end)
        case Some(v) =>
          graph.insertEdge(parentX, null, null, stateToVertex(start), v)
      }
      layoutX.execute(parentX)
    }
    finally
    {
      graph.getModel().endUpdate()
    }
  }

}
*/
