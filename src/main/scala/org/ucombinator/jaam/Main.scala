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
import scala.language.postfixOps
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
import soot.jimple.internal.JArrayRef

import soot.tagkit._

import soot.util.Chain
import soot.options.Options
import soot.jimple.toolkits.invoke.AccessManager

//ICFG
import soot.toolkits.graph._
import soot.jimple.toolkits.ide.icfg._

// JGraphX

import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager

import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.view.mxGraph
import com.mxgraph.layout.mxCompactTreeLayout

// JSON4s
import org.json4s._
import org.json4s.native._

// Possibly thrown during transition between states.
case class UninitializedClassException(sootClass : SootClass) extends RuntimeException
case class StringConstantException(string : String) extends RuntimeException
case class UndefinedAddrsException(addrs : Set[Addr]) extends RuntimeException

// A continuation store paired with a continuation
case class KontStack(store : KontStore, k : Kont) {
  // TODO/generalize: Add widening in push and pop (to support PDCFA)

  // TODO/generalize: We need the control pointer of the target state to HANDLE PDCFA.
  def kalloca(frame : Frame) = OneCFAKontAddr(frame.fp)

  // TODO/generalize: Handle PDCFA
  def push(frame : Frame) : KontStack = {
    val kAddr = kalloca(frame)
    val newKontStore = store.update(kAddr, Set(k))
    KontStack(newKontStore, RetKont(frame, kAddr))
  }

  // Pop returns all possible frames beneath the current one.
  def pop() : Set[(Frame, KontStack)] = {
    k match {
      case RetKont(frame, kontAddr) => {
        for (topk <- store(kontAddr)) yield (frame, KontStack(store, topk))
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
        if (Soot.isSubclass(exception.asInstanceOf[ObjectValue].sootClass, caughtType))
          return Set(State(stmt.copy(sootStmt = trap.getHandlerUnit()), fp, newStore, this, initializedClasses))
      }

      // TODO/interface we should log this sort of thing
      val nextFrames = this.pop()
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
  val serializer = Soot.mapSerializer[KontStore, KontAddr, Set[Kont]]
}

// Probably don't refactor. Might only want to widen on KontStore.
case class KontStore(private val map : Map[KontAddr, Set[Kont]]) {
  def update(addr : KontAddr, konts : Set[Kont]) : KontStore = {
    map.get(addr) match {
      case Some(oldd) => KontStore(map + (addr -> (oldd ++ konts)))
      case None => KontStore(map + (addr -> konts))
    }
  }

  def apply(addr : KontAddr) : Set[Kont] = map(addr)
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
  val serializer = new CustomSerializer[D](implicit format => (
    { case s => ??? },
    { case D(values) => Extraction.decompose(values) }
  ))
}

abstract class Value

abstract class AtomicValue extends Value

case object AnyAtomicValue extends AtomicValue

case class ObjectValue(val sootClass : SootClass,  val bp : BasePointer) extends Value

case class ArrayValue(val sootType : SootType, val bp : BasePointer) extends Value

//----------------- POINTERS ------------------

// FramePointers, when paired with variable names, yield the addresses of variables.
abstract class FramePointer
case object InvariantFramePointer extends FramePointer // TODO: delete this?  Is seems unused
case class OneCFAFramePointer(val method : SootMethod, val nextStmt : Stmt) extends FramePointer
case object InitialFramePointer extends FramePointer

// BasePointers, when paired with field names, yield the addresses of fields.
abstract class BasePointer
case class OneCFABasePointer(stmt : Stmt, fp : FramePointer) extends BasePointer
case object InitialBasePointer extends BasePointer
// Note that due to interning, strings and classes may share base pointers with each other
// Oh, and class loaders are a headache(!)
case class StringBasePointer(val string : String) extends BasePointer
case class ClassBasePointer(val name : String) extends BasePointer

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
  val serializer = Soot.mapSerializer[Store, Addr, D]
}

case class Store(private val map : Map[Addr, D]) {
  def contains(addr : Addr) = map.contains(addr)

  def update(addr : Addr, d : D) : Store = {
    map.get(addr) match {
      case Some(oldd) => Store(map + (addr -> oldd.join(d)))
      case None => Store(map + (addr -> d))
    }
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
    val ds = for (a <- addrs; if map.contains(a)) yield map(a)
    val res = ds.fold (D(Set()))(_ join _)
    if (res == D(Set())) throw UndefinedAddrsException(addrs)
    res
  }

  def prettyString() : List[String] =
    (for ((a, d) <- map) yield { a + " -> " + d }).toList
}

// Due to a bug in json4s we have to use "Int" instead of "AbstractState.Id" for the keys of this map
// Once the following pull request is accepted we can use "AbstractState.Id":
//   https://github.com/json4s/json4s/pull/324
case class UpdatePacket(states : Map[Int,AbstractState], edges : Set[(AbstractState.Id, AbstractState.Id)])
object UpdatePacket {
  val serializer = new CustomSerializer[UpdatePacket](implicit format => (
    { case s => ??? },
    { case (i : AbstractState.Id, j : AbstractState.Id) => Extraction.decompose(List(i,j)) }
  ))
}

abstract sealed class AbstractState {
  def next() : Set[AbstractState]
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
}
// State abstracts a collection of concrete states of execution.
case class State(val stmt : Stmt,
                 val fp : FramePointer,
                 val store : Store,
                 val kontStack : KontStack,
                 val initializedClasses : Set[SootClass]) extends AbstractState {

  // When you call next, you may realize you are going to end up throwing some exceptions
  // TODO/refactor: Consider turning this into a list of exceptions.
  var exceptions = D(Set())

  // Allocates a new frame pointer (currently uses 1CFA)
  def alloca(expr : InvokeExpr, nextStmt : Stmt) : FramePointer = OneCFAFramePointer(expr.getMethod, nextStmt)
  // Allocates objects
  def malloc() : BasePointer = OneCFABasePointer(stmt, fp)

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

    v match {
      //TODO missing: CmplExpr, CmpgExpr, MethodHandle
      //TODO/precision actually do the calculations
      case (_ : Local) | (_ : Ref) => store(addrsOf(v))
      case _ : NullConstant => D.atomicTop
      case _ : NumericConstant => D.atomicTop
      // TODO: Class and String objects are objects and so need their fields initialized
      // TODO/clarity: Add an example of Java code that can trigger this.
      case v : ClassConstant => D(Set(ObjectValue(Soot.classes.Class, ClassBasePointer(v.value))))
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
          case (_ : EqExpr) | (_ : NeExpr) | (_ : GeExpr) | (_ : GtExpr) | (_ : LeExpr) | (_ : LtExpr) =>
            eval(v.getOp1)
            eval(v.getOp2)
            D.atomicTop
          case (_ : ShrExpr) | (_ : ShlExpr) | (_ : UshrExpr) | (_ : RemExpr) =>
            assertIntegral(v.getOp1)
            assertIntegral(v.getOp2)
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

      case v : CastExpr => {
        val castedExpr : SootValue = v.getOp
        val castedType : SootType = v.getType
        checkInitializedClasses(castedType)
        val d = eval(castedExpr)
        if (castedType.isInstanceOf[PrimType]) {
          return D.atomicTop
        }
        // TODO/soundness: Throw an exception if necessary.
        for (vl <- d.values) {
          vl match {
            case ObjectValue(objectType, _) =>
              if (!Soot.isSubType(objectType.getType, castedType)) {
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
        d
      }

      case _ =>  throw new Exception("No match for " + v.getClass + " : " + v)
    }
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
    def dispatch(c : SootClass, m : SootMethod, receivers : Set[Value]) : Set[AbstractState] = {
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
        if (curr_m == null) { overrides(curr.getSuperclass(), root_m) }
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
          if (meth.isNative) {
            println()
            println(Console.YELLOW + "!!!!! WARNING: Native method without a snowflake. May be unsound. !!!!!")
            println("!!!!! stmt = " + stmt + " !!!!!")
            println("!!!!! method = " + meth + " !!!!!" + Console.RESET)
            meth.getReturnType match {
              case _ : VoidType => Set(this.copy(stmt = nextStmt))
              case _ : PrimType => Set(this.copy(stmt = nextStmt, store = store.update(destAddr, D.atomicTop)))
              case _ =>
                println(Console.RED + "!!!!! Native method returns an object.  Aborting. !!!!!" + Console.RESET)
                Set()
            }
          } else {
            Set(State(Stmt.methodEntry(meth), newFP, newStore, newKontStack, initializedClasses))
          }
      }
    }

    expr match {
      case expr : DynamicInvokeExpr => ??? // TODO: Could only come from non-Java sources
      case expr : StaticInvokeExpr =>
        checkInitializedClasses(expr.getMethod().getDeclaringClass())
        dispatch(null, expr.getMethod(), Set())
      case expr : InstanceInvokeExpr =>
        val d = eval(expr.getBase())
        val vs = d.values filter { case ObjectValue(sootClass, _) => Soot.isSubclass(sootClass, expr.getMethod().getDeclaringClass); case _ => false }
        ((for (ObjectValue(sootClass, _) <- vs) yield {
          val objectClass = if (expr.isInstanceOf[SpecialInvokeExpr]) null else sootClass
          dispatch(objectClass, expr.getMethod(), vs)
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
      val exceptionStates = (exceptions.values map { kontStack.handleException(_, stmt, fp, store, initializedClasses) }).flatten
      nexts ++ exceptionStates
    } catch {
      case UninitializedClassException(sootClass) =>
        // TODO/soundness: needs to also initialize parent classes
        exceptions = D(Set())
        val meth = sootClass.getMethodByNameUnsafe(SootMethod.staticInitializerName)
        if (meth != null) {
          // Initialize all static fields per JVM 5.4.2 and 5.5
          val staticUpdates = for {
            f <- sootClass.getFields(); if f.isStatic
          } yield (StaticFieldAddr(f) -> staticInitialValue(f))
          var newStore = store.update(staticUpdates.toMap)
          this.copy(store = newStore,
                    initializedClasses = initializedClasses + sootClass)
            .handleInvoke(new JStaticInvokeExpr(meth.makeRef(),
                          java.util.Collections.emptyList()), None, stmt)
        } else {
          // TODO: Do we need to do newStore for static fields?
          Set(this.copy(initializedClasses = initializedClasses + sootClass))
        }

      case StringConstantException(string) =>
        val value = Soot.classes.String.getFieldByName("value")
        val hash = Soot.classes.String.getFieldByName("hash")
        val hash32 = Soot.classes.String.getFieldByName("hash32")
        val bp = StringBasePointer(string)
        Set(this.copy(store =
          createArray(ArrayType.v(CharType.v,1), List(D.atomicTop/*string.length*/), Set(InstanceFieldAddr(bp, value)))
            .update(InstanceFieldAddr(bp, hash), D.atomicTop)
            .update(InstanceFieldAddr(bp, hash32), D.atomicTop)))

      case UndefinedAddrsException(addrs) =>
        println()
        println(Console.RED + "!!!!! ERROR: Undefined Addrs !!!!!")
        println("!!!!! stmt = " + stmt + " !!!!!")
        println("!!!!! addrs = " + addrs + " !!!!!" + Console.RESET)
        Set()
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
            Set(this.copy(stmt = stmt.nextSyntactic, store = newStore))
          }
          case rhs : NewArrayExpr => {
            // Value of lhsAddr will be set to a pointer to the array. (as opposed to the array itself)
            val newStore = createArray(rhs.getType(), List(eval(rhs.getSize())), lhsAddr)
            Set(this.copy(stmt = stmt.nextSyntactic, store = newStore))
          }
          case rhs : NewMultiArrayExpr => {
            //see comment above about lhs addr
            val newStore = createArray(rhs.getType(), rhs.getSizes().toList map eval, lhsAddr)
            Set(this.copy(stmt = stmt.nextSyntactic, store = newStore))
          }
          case rhs => {
            val newStore = store.update(lhsAddr, eval(rhs))
            Set(this.copy(stmt = stmt.nextSyntactic, store = newStore))
          }
        }
      }

      case sootStmt : IfStmt => {
            eval(sootStmt.getCondition()) //in case of side effects //TODO/precision evaluate the condition
            val trueState = this.copy(stmt = stmt.copy(sootStmt = sootStmt.getTarget()))
            val falseState = this.copy(stmt = stmt.nextSyntactic)
        Set(trueState, falseState)
      }

      case sootStmt : SwitchStmt =>
        //TODO/prrecision dont take all the switches
        sootStmt.getTargets().map(t => this.copy(stmt = stmt.copy(sootStmt = t))).toSet

      case sootStmt : ReturnStmt => {
        val evaled = eval(sootStmt.getOp())
        for ((frame, newStack) <- kontStack.pop) yield {
          val newStore = if (frame.acceptsReturnValue()) {
            store.update(frame.destAddr.get, evaled)
          } else {
            store
          }

          State(frame.stmt, frame.fp, newStore, newStack, initializedClasses)
        }
      }

      case sootStmt : ReturnVoidStmt =>
        for ((frame, newStack) <- kontStack.pop() if !frame.acceptsReturnValue()) yield
          State(frame.stmt, frame.fp, store, newStack, initializedClasses).asInstanceOf[AbstractState]

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
    State(stmt, initialFramePointer, Store(initial_map), KontStack(KontStore(Map()), HaltKont), Set())
  }
}

// Command option config
case class Config(
  cfg: Option[String] = None,
  sootClassPath: String = null,
  className: String = null,
  methodName: String = null)

object Main {
  def main(args : Array[String]) {
    val parser = new scopt.OptionParser[Config]("shimple-analyzer") {
      head("shimple-analyzer")
      opt[String]("cfg") action {
        (x, c) => c.copy(cfg = Some(x))
      } text("output TODO control flow graph")

      opt[String]("classpath") action {
        (x, c) => c.copy(sootClassPath = x)
      } text("the TODO class directory")

      opt[String]('c', "class") action {
        (x, c) => c.copy(className = x)
      } text("the main class")

      opt[String]('m', "method") action {
        (x, c) => c.copy(methodName = x)
      } text("the main method")

      override def showUsageOnError = true
    }

    parser.parse(args, Config()) match {
      case None =>
        println("Wrong arguments")

      case Some(config) =>
        Soot.initialize(config)

        config.cfg match {
          case None => defaultMode(config)
          case Some(dir) => cfgMode(config)
        }
    }
  }

  def defaultMode(config : Config) {
    val mainMainMethod : SootMethod = Soot.getSootClass(config.className).getMethodByName(config.methodName)

    // Setting up the GUI
    val window = new Window
    val initialState = State.inject(Stmt.methodEntry(mainMainMethod))
    window.addState(initialState)

    var todo : List[AbstractState] = List(initialState)
    var seen : Set[AbstractState] = Set()

    // Explore the state graph
    while (todo nonEmpty) {
      val current = todo.head
      println("!!! Processing " + current.id + " !!!")
      val nexts : Set[AbstractState] = current.next()
      for (n <- nexts) {
        window.addNext(current, n)
      }
      val newTodo = nexts.toList.filter(!seen.contains(_))
      val packet = UpdatePacket(
        (for (n <- newTodo) yield { (n.id -> n) }).toMap,
        for (n <- nexts) yield { (current.id -> n.id) })

      implicit val formats = Soot.formats +
        UpdatePacket.serializer +
        D.serializer +
        Store.serializer +
        KontStore.serializer
      println(Serialization.writePretty(packet))

      println()

      // TODO: Fix optimization bug here
      todo = newTodo ++ todo.tail
      seen = seen ++ nexts

    }
    println("Done!")
  }

  def cfgMode(config: Config) {
    val cg = Scene.v().getCallGraph()

    // TODO: target for throw
    def getTargets(stmt : Stmt) : List[Stmt] = {
      def getCallTarget() = {
        // Skip over things like native methods
        // TODO: note this edge somehow
        for (e <- cg.edgesOutOf(stmt.sootMethod); if !e.getTgt.method().hasActiveBody) {
          println("Warning: no active body of " + e.getTgt.method().getSignature)
        }
        cg.edgesOutOf(stmt.sootMethod).filter(_.getTgt.method().hasActiveBody).map(e => Stmt.methodEntry(e.getTgt.method)).toList
      }
      stmt.sootStmt match {
        case (_ : InvokeStmt) => getCallTarget()

        case sootStmt: DefinitionStmt => sootStmt.getRightOp match {
          case rhs: InvokeExpr => getCallTarget()
          case rhs => List()
        }

        case (_: ReturnStmt | _ : ReturnVoidStmt) =>
          cg.edgesInto(stmt.sootMethod).map(e => Stmt(e.srcUnit(), e.getSrc.method())).toList

        case _ => List()
      }
    }

    val tgtMap = scala.collection.mutable.Map[String, List[String]]()
    val callGraph = CallGraph(
      (for (cls <- Scene.v().getApplicationClasses();
        method <- cls.getMethods;
        if method.isConcrete;
        unit <- Soot.getBody(method).getUnits) yield {
          val stmt = Stmt(unit, method)
          val targets = getTargets(stmt)

          tgtMap.get(cls.toString) match {
            case None => tgtMap += (cls.toString -> targets.map(_.sootMethod.getDeclaringClass.toString))
            case Some(tgts) => tgtMap += (cls.toString -> (targets.map(_.sootMethod.getDeclaringClass.toString)++tgts).distinct)
          }

          (stmt -> CallGraphValue(targets, stmt.nextSemantic)) //obj)
      }).toMap)

    def getDenpendencyList(className: String, acc: List[String]): List[String] = {
      var existed = List(className) ++ acc
      tgtMap.get(className) match {
        case None => existed
        case Some(deps) => {
          deps.filter(!existed.contains(_)).foreach((c: String) => {
            existed ++= getDenpendencyList(c, existed)
          })
          existed
        }
      }
    }
    val depList = getDenpendencyList(config.className, List()).distinct
    val filteredCallGraph = CallGraph(callGraph.map.filterKeys(stmt => {
      depList.contains(stmt.sootMethod.getDeclaringClass.toString)
    }))

    implicit val formats = Soot.formats + Soot.mapSerializer[CallGraph, Stmt, CallGraphValue]
    println(Serialization.writePretty(filteredCallGraph))
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
        case Some(v) =>
          graph.insertEdge(parentX, null, null, stateToVertex(start), v)
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
