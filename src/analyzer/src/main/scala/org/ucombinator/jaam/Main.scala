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
import scala.collection.mutable.TreeSet
import scala.language.postfixOps

import java.io.FileOutputStream

import com.esotericsoftware.minlog.Log

// We expect every Unit we use to be a soot.jimple.Stmt, but the APIs
// are built around using Unit so we stick with that.  (We may want to
// fix this when we build the Scala wrapper for Soot.)
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, Type => SootType, _}

import soot.jimple._
import soot.jimple.{Stmt => SootStmt}
import soot.jimple.internal.JStaticInvokeExpr
import soot.jimple.internal.JArrayRef

import soot.tagkit._

import soot.util.Chain
import soot.options.Options
import soot.jimple.toolkits.invoke.AccessManager

import org.ucombinator.jaam.messaging._

import org.ucombinator.jaam.Stmt.unitToStmt // Automatically convert soot.Unit to soot.Stmt

// Possibly thrown during transition between states.
case class UninitializedClassException(sootClass : SootClass) extends RuntimeException
case class StringConstantException(string : String) extends RuntimeException
case class UndefinedAddrsException(addrs : Set[Addr]) extends RuntimeException

// A continuation store paired with a continuation
case class KontStack(/*store : KontStore, */k : Kont) {
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
    val newKontStore = store.update(kAddr, Set(k))
    val newKontStack = KontStack(RetKont(frame, kAddr))
    newKontStack.setStore(newKontStore)
    newKontStack
  }

  // Pop returns all possible frames beneath the current one.
  def pop() : Set[(Frame, KontStack)] = {
    k match {
      case RetKont(frame, kontAddr) => {
        for (topk <- store(kontAddr)) yield {
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
        val newStore = store.update(CaughtExceptionFrameAddr(fp), D(Set(exception)))

        // TODO/soundness or performance?: use Hierarchy or FastHierarchy?
        if (Soot.isSubclass(exception.asInstanceOf[ObjectValue].sootClass, caughtType)) {
          val newState = State(stmt.copy(sootStmt = trap.getHandlerUnit()), fp, this.copyKontStack())
          newState.setStore(newStore)
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

object KontStore {
  var readAddrs = Set[KontAddr]()
  var writeAddrs = Set[KontAddr]()
}

// Probably don't refactor. Might only want to widen on KontStore.
case class KontStore(private val map : Map[KontAddr, Set[Kont]]) {

  def resetReadAddrsAndWriteAddrs: scala.Unit = {
    KontStore.readAddrs = Set[KontAddr]()
    KontStore.writeAddrs = Set[KontAddr]()
  }

  def join(other : KontStore) : KontStore = {
    var newStore = this.copy()
    other.map.foreach { case (k, v) => {
      newStore = newStore.update(k, v)
    }}
    //newStore.readAddrs = other.readAddrs ++ readAddrs
    //newStore.writeAddrs = other.writeAddrs ++ writeAddrs
    newStore
  }

  def update(addr : KontAddr, konts : Set[Kont]) : KontStore = {
    val oldd = map.get(addr) match {
      case Some(oldd) => oldd
      case None => Set()
    }
    val newd = oldd ++ konts
    if (oldd.size != newd.size)
      KontStore.writeAddrs += addr
    KontStore(map + (addr -> newd))
  }

  def apply(addr : KontAddr) : Set[Kont] = {
    KontStore.readAddrs += addr
    map(addr)
  }

  def get(addr : KontAddr) : Option[Set[Kont]] = map.get(addr)

  def prettyString() : List[String] =
    (for ((a, d) <- map) yield { a + " -> " + d }).toList
}


case class Frame(
                  val stmt : Stmt,
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
case class D(val values: Set[Value]) {
  def join(otherd : D) : D = {
    D(values ++ otherd.values)
  }
  def maybeZero() : Boolean = values.exists(_.isInstanceOf[AtomicValue])
}

object D {
  val atomicTop = D(Set(AnyAtomicValue))
}

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
abstract class KontAddr
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

object Store {
  var on = false
  var readAddrs = Set[Addr]()
  var writeAddrs = Set[Addr]()
  var map : Map[Addr, D] = Map()
}

case class Store() { //private val map : Map[Addr, D]) {
  def contains(addr : Addr) = Store.map.contains(addr)

//  var readAddrs = Set[Addr]()
//  var writeAddrs = Set[Addr]()

  def resetReadAddrsAndWriteAddrs: scala.Unit = {
    Store.readAddrs = Set[Addr]()
    Store.writeAddrs = Set[Addr]()
  }

  def join(store : Store): Store = {
//    var newStore = this.copy()
//    store.map.foreach { case (k, v) => {
//      newStore = newStore.update(k, v)
//    }}
//    //newStore.readAddrs = store.readAddrs ++ readAddrs
//    //newStore.writeAddrs = store.writeAddrs ++ writeAddrs
//    newStore
    Store()
  }

  def get(addr: Addr): D = {
    Store.map.get(addr) match {
      case Some(oldd) => oldd
      case None => D(Set())
    }
  }

  def update(addr : Addr, d : D) : Store = {
    val oldd: D = get(addr)
    val newd: D = oldd.join(d)
    if (Store.on && oldd.values.size != newd.values.size) {
      Store.writeAddrs += addr
    }
    Store.map = Store.map + (addr -> newd)
    Store()
//    Store(map + (addr -> newd))
  }

  def update(addrs : Set[Addr], d : D) : Store = {
    var newStore = this
    for (a <- addrs) {
      newStore = newStore.update(a, d)
    }
    newStore
  }

  def update(addrs : Option[Set[Addr]], d : D) : Store = {
    addrs match {
      case None => this
      case Some(a) => update(a, d)
    }
  }

  def update(m : Map[Addr, D]) : Store = {
    var newStore = this
    for ((a, d) <- m) {
      newStore = newStore.update(a, d)
    }
    newStore
  }

  def apply(addrs : Set[Addr]) : D = {
    Store.readAddrs ++= addrs
    val ds = for (a <- addrs; if Store.map.contains(a)) yield {
      a match {
        case InstanceFieldAddr(OneCFABasePointer(_, _, FromNative), _) =>
          Log.error("Access to field of object created from native method. addr = "+a)
        case _ =>
      }
      Store.map(a)
    }
    val res = ds.fold (D(Set()))(_ join _)
    if (res == D(Set())) throw UndefinedAddrsException(addrs)
    res
  }

  def prettyString() : List[String] =
    (for ((a, d) <- Store.map) yield { a + " -> " + d }).toList
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

  def toMessage() : messaging.AbstractState

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
  override def getStore() = Store(/*Map()*/)
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
  override def toMessage() = messaging.ErrorState(messaging.Id[messaging.AbstractState](id))
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

  var store: Store = Store(/*Map()*/)

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

  override def toMessage() = messaging.State(messaging.Id[messaging.AbstractState](id), stmt.toMessage, fp.toString, kontStack.toString)

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
            d2 = d2.join(Snowflakes.createObjectOrThrow(name))
          } else {
            // Anything else would cause an exception
            val classCastException = D(Set(ObjectValue(Soot.classes.ClassCastException, malloc())))
            exceptions = exceptions.join(classCastException)
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
      var newStore = store
      for (i <- 0 until expr.getArgCount())
        newStore = newStore.update(ParameterFrameAddr(newFP, i), eval(expr.getArg(i)))
      val newKontStack = kontStack.push(Frame(nextStmt, fp, destAddr))
      // TODO/optimize: filter out incorrect class types
      val th = ThisFrameAddr(newFP)
      for (r <- receivers)
        newStore = newStore.update(th, D(Set(r)))

      Snowflakes.get(meth) match {
        case Some(h) => h(this, nextStmt, newFP, newStore, newKontStack)
        case None =>
          if (meth.getDeclaringClass.isJavaLibraryClass ||
              bp.isInstanceOf[SnowflakeBasePointer]) {
            Snowflakes.warn(this.id, stmt, meth)
            val rtType = meth.getReturnType
            rtType match {
              case t: VoidType => NoOpSnowflake(this, nextStmt, newFP, newStore, newKontStack)
              case pt: PrimType => ReturnSnowflake(D.atomicTop)(this, nextStmt, newFP, newStore, newKontStack)
              case at: ArrayType =>
                ReturnArraySnowflake(at.baseType.toString, at.numDimensions)
                  .apply(this, nextStmt, newFP, newStore, newKontStack)
              case rt: RefType =>
                ReturnObjectSnowflake(rt.getClassName)(this, nextStmt, newFP, newStore, newKontStack)
            }
          }
          else if (meth.isNative) {
            Log.warn("Native method without a snowflake in state "+this.id+". May be unsound. stmt = " + stmt)
            meth.getReturnType match {
              case _ : VoidType => Set(this.copyState(stmt = nextStmt))
              case _ : PrimType => {
                val newStore = store.update(destAddr, D.atomicTop)
                Set(this.copyState(stmt = nextStmt, store = newStore))
              }
              case _ =>
                Log.error("Native method returns an object. Aborting.")
                Set()
            }
          } else {
            val newState = State(Stmt.methodEntry(meth), newFP, newKontStack)
            newState.setStore(newStore)
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
    case Nil => store.update(addrs, defaultInitialValue(t))
    case (s :: ss) => {
      val bp : BasePointer = malloc()
      // TODO/soundness: exception for a negative length
      // TODO/precision: stop allocating if a zero length
      // TODO/precision: separately allocate each array element
      createArray(t.asInstanceOf[ArrayType].getElementType(), ss, Set(ArrayRefAddr(bp)))
        .update(addrs, D(Set(ArrayValue(t, bp))))
        .update(ArrayLengthAddr(bp), s)
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
        Log.info("Static initializing "+sootClass)
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
            var newStore = store.update(staticUpdates.toMap)
            val newState = this.copyState(store = newStore, initializedClasses = initializedClasses+sootClass)
            newState.handleInvoke(new JStaticInvokeExpr(meth.makeRef(),
              java.util.Collections.emptyList()), None, stmt)
          }
        } else {
          // TODO: Do we need to do newStore for static fields?
          Set(this.copyState(initializedClasses = initializedClasses + sootClass))
        }

      case UninitializedSnowflakeObjectException(className) =>
        Log.info("Initializing snowflake "+className)
        val newStore = store.join(Snowflakes.createObject(className, List()))
        Set(this.copyState(store = newStore)) //store=>store.join(Snowflakes.createObject(className, List()))))

      case StringConstantException(string) =>
        Log.info("Initializing string constant: \""+string+"\"")
        val value = Soot.classes.String.getFieldByName("value")
        val hash = Soot.classes.String.getFieldByName("hash")
        val hash32 = Soot.classes.String.getFieldByName("hash32")
        val bp = StringBasePointer(string)
        val newStore = createArray(ArrayType.v(CharType.v,1), List(D.atomicTop/*string.length*/), Set(InstanceFieldAddr(bp, value)))
          .update(InstanceFieldAddr(bp, hash), D.atomicTop)
          .update(InstanceFieldAddr(bp, hash32), D.atomicTop)
        Set(this.copyState(store = newStore))

      case UndefinedAddrsException(addrs) =>
        Log.error("Undefined Addrs in state "+this.id+" stmt = "+stmt+" addrs = "+addrs)
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
      newStore
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
      newStore
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

            Set(this.copyState(stmt = stmt.nextSyntactic, store = this.newExpr(lhsAddr, sootClass, store)))
          }
          case rhs : NewArrayExpr => {
            //TODO, if base type is Java library class, call Snowflake.createArray
            // Value of lhsAddr will be set to a pointer to the array. (as opposed to the array itself)
            val newStore = createArray(rhs.getType(), List(eval(rhs.getSize())), lhsAddr)
            Set(this.copyState(stmt = stmt.nextSyntactic, store = newStore))
          }
          case rhs : NewMultiArrayExpr => {
            //TODO, if base type is Java library class, call Snowflake.createArray
            //see comment above about lhs addr
            val newStore = createArray(rhs.getType(), rhs.getSizes().toList map eval, lhsAddr)
            Set(this.copyState(stmt = stmt.nextSyntactic, store = newStore))
          }
          case rhs => {
            val newStore = store.update(lhsAddr, eval(rhs))
            Set(this.copyState(stmt = stmt.nextSyntactic, store = newStore))
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
          val newStore = if (frame.acceptsReturnValue()) {
            store.update(frame.destAddr.get, evaled)
          } else {
            store
          }

          val state = State(frame.stmt, frame.fp, newStack)//, initializedClasses)
          state.setStore(newStore)
          state
        }
      }

      case sootStmt : ReturnVoidStmt =>
        for ((frame, newStack) <- kontStack.pop() if !frame.acceptsReturnValue()) yield {
          val s = State(frame.stmt, frame.fp, newStack)//, initializedClasses)
          s.setStore(store)
          s
          //s.asInstanceOf[AbstractState]
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
  def inject(stmt : Stmt) : State = {
    val stringClass : SootClass = Soot.classes.String
    val initial_map : Map[Addr, D] = Map(
      ParameterFrameAddr(initialFramePointer, 0) ->
        D(Set(ArrayValue(stringClass.getType(), initialBasePointer))),
      ArrayRefAddr(initialBasePointer) -> D(Set(ObjectValue(stringClass, initialBasePointer))),
      ArrayLengthAddr(initialBasePointer) -> D.atomicTop)
    val store = Store(/*initial_map*/).update(initial_map)

    val ks = KontStack(HaltKont)
    ks.setStore(KontStore(Map()))
    val state = State(stmt, initialFramePointer, ks)
    state.setStore(store)
    state
  }
}

object System {
  def next(state: AbstractState,
                  store: Store,
                  kstore: KontStore,
                  initializedClasses: Set[SootClass]): (Set[AbstractState], Store, KontStore, Set[SootClass]) = {
    state.setStore(store)
    state.setKontStore(kstore)
    state.setInitializedClasses(initializedClasses)

  store.resetReadAddrsAndWriteAddrs
  kstore.resetReadAddrsAndWriteAddrs
    Store.on = true

    val nexts = state.next()


    Store.on = false

    state.setReadAddrs(Store.readAddrs)
    state.setKReadAddrs(KontStore.readAddrs)
    state.setWriteAddrs(Store.writeAddrs)
    state.setKWriteAddrs(KontStore.writeAddrs)

    val newStore = Store() //nexts.par.map(_.getStore()).foldLeft(Store(Map()))(_.join(_))
    val newKStore = nexts.par.map(_.getKontStore()).foldLeft(KontStore(Map()))(_.join(_))
    val newInitClasses = nexts.par.map(_.getInitializedClasses()).foldLeft(Set[SootClass]())(_.++(_))

    (nexts, newStore, newKStore, newInitClasses)
  }
}

// Command option config
case class Config(
                   rtJar: String = null,
                   sootClassPath: String = null,
                   className: String = null,
                   methodName: String = null,
                   outputFile: String = null)

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

      opt[String]('o', "outfile") action {
        (x, c) => c.copy(outputFile = x)
      } text("the output file for the serialized data")

      help("help") text("prints this usage text")

      override def showUsageOnError = true
    }

    parser.parse(args, Config()) match {
      case None =>
        println("Wrong arguments")

      case Some(config) =>
        Log.setLogger(new JaamLogger)
        Soot.initialize(config)
        defaultMode(config)
    }
  }

  def defaultMode(config : Config) {
    val outputFileOpt = Option(config.outputFile)
    var outputFile: String = null
    outputFileOpt match {
      case None => outputFile = config.className + ".jaam"
      case Some(x) => outputFile = x
    }
    val outMessaging = new MessageOutput(new FileOutputStream(outputFile))
    val mainMainMethod : SootMethod = Soot.getSootClass(config.className).getMethodByName(config.methodName)
    val initialState = State.inject(Stmt.methodEntry(mainMainMethod))

    var todo: List[AbstractState] = List(initialState)
    var done: Set[AbstractState] = Set()
    var globalStore: Store = initialState.getStore
    var globalKontStore: KontStore = initialState.getKontStore
    var globalInitClasses: Set[SootClass] = Set()
    var doneEdges: Map[Int,Pair[Int, Int]] = Map()

    outMessaging.write(initialState.toMessage)

    while (todo nonEmpty) {
      //TODO refactor store widening code
      val current = todo.head
      Log.info("Processing state " + current.id+": "+(current match { case s : State => s.stmt.toString; case s => s.toString}))
      val (nexts, newStore, newKStore, initClasses) = System.next(current, globalStore, globalKontStore, globalInitClasses)

      val newTodo = nexts.toList.filter(!done.contains(_))

      for (n <- newTodo) {
        Log.info("Writing state "+n.id)
        outMessaging.write(n.toMessage)
      }

      for (n <- nexts) {
        if (!doneEdges.contains(current, n)) {
          val id = doneEdges.size
          Log.info("Writing edge "+id+": "+current.id+" -> "+n.id)
          doneEdges += id -> Pair(current.id, n.id)
          outMessaging.write(messaging.Edge(messaging.Id[messaging.Edge](id), messaging.Id[messaging.AbstractState](current.id), messaging.Id[messaging.AbstractState](n.id)))
        } else {
          Log.info("Skipping edge "+current.id+" -> "+n.id)
        }
      }

      for (d <- done) {
        if (d.getReadAddrs.intersect(current.getWriteAddrs).nonEmpty
          || d.getKReadAddrs.intersect(current.getKWriteAddrs).nonEmpty) {
          todo = todo ++ List(d)
          done = done - d
        }
      }

      if ((globalInitClasses++initClasses).size != globalInitClasses.size) {
        todo = newTodo ++ List(current) ++ todo.tail
      }
      else {
        done += current
        todo = newTodo ++ todo.tail
      }

      globalStore = globalStore.join(newStore)
      globalKontStore = globalKontStore.join(newKStore)
      globalInitClasses ++= initClasses
      Log.info("Done processing state "+current.id+": "+(current match { case s : State => s.stmt.toString; case s => s.toString}))
    }

    outMessaging.close()

    Log.info("Done!")
  }

  class JaamLogger extends Log.Logger {
    private var level = 0

    override def log(level : Int, category : String, message : String, ex : Throwable) {
      this.level = level
      super.log(level, category, message, ex)
    }

    override def print(s : String) = {
      this.level match {
        case Log.LEVEL_ERROR => super.print(Console.RED + s + Console.RESET)
        case Log.LEVEL_WARN => super.print(Console.YELLOW + s + Console.RESET)
        case _ => super.print(s)
      }
    }
  }
}
