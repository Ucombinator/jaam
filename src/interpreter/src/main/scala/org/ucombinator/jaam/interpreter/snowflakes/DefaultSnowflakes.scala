package org.ucombinator.jaam.interpreter.snowflakes

import scala.collection.JavaConversions._
import scala.collection.mutable
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}

import org.ucombinator.jaam.interpreter._

object DefaultReturnSnowflake {
  // A list contains statically initialized classes
  var initializedClasses = List[SootClass]()
  // A list contains instantiated classes
  var instantiatedClasses = List[SootClass]()

  def createArray(t: soot.Type, sizes: List[D], addrs: Set[Addr]) {
    val bp = t match {
      case at: ArrayType => Snowflakes.malloc(at)
      case _ => Snowflakes.malloc(t.toString) //TODO
    }
    createArray(t, sizes, addrs, bp)
  }

  def createArray(at: soot.Type, sizes: List[D], addrs: Set[Addr], bp: BasePointer) {
    sizes match {
      case Nil =>
        at match {
          case pt: PrimType => System.store.update(addrs, D.atomicTop)
          case rt: RefType => DefaultReturnSnowflake.createObject(Some(addrs), Soot.getSootClass(rt.getClassName))
        }
      case (s :: ss) =>
        createArray(at.asInstanceOf[ArrayType].getElementType, ss, Set(ArrayRefAddr(bp)))
        System.store.update(addrs, D(Set(ArrayValue(at, bp))))
        System.store.update(ArrayLengthAddr(bp), s)
    }
  }

/*
  TODO: createObjectOrThrow vs createObject
  def createObjectOrThrow(sootClass: SootClass) : D = {
    if (!initializedClasses.contains(sootClass)) {
      throw UninitializedSnowflakeObjectException(sootClass)
    }
    D(Set(ObjectValue(sootClass, Snowflakes.malloc(sootClass))))
  }
 */

  def initField(addrs: Set[Addr], field: SootField) {
    field.getType match {
      case pt: PrimType => System.store.update(addrs, D.atomicTop)
      case at: ArrayType => createArray(at, List.fill(at.numDimensions)(D.atomicTop), addrs)
      case rt: RefType => createObject(Some(addrs), Soot.getSootClass(rt.getClassName))
      case t => Log.error("Unknown field type " + t)
    }
  }

  def initStaticFields(sootClass: SootClass) {
    if (initializedClasses.contains(sootClass)) return
    if (sootClass.hasSuperclass) initStaticFields(sootClass.getSuperclass)
    for (field <- sootClass.getFields; if field.isStatic) {
      initField(Set(StaticFieldAddr(field)), field)
    }
    initializedClasses = sootClass::initializedClasses
  }

  def initInstanceFields(sootClass: SootClass, bp: BasePointer) {
    val className = sootClass.getName
    for (field <- sootClass.getFields; if !field.isStatic) {
      val addrs: Set[Addr] = Set(InstanceFieldAddr(bp, field))
      initField(addrs, field)
    }
  }

  /* DefaultReturnSnowflake.createObject should only used for instantiating Java library or
     application library classes.
     */
  def createObject(destAddr: Option[Set[Addr]], sootClass: SootClass) {
    def allSuperClasses(sootClass: SootClass, supers: Set[SootClass]): Set[SootClass] = {
      if (sootClass.hasSuperclass) allSuperClasses(sootClass.getSuperclass, supers + sootClass.getSuperclass)
      else supers
    }
    def allInterfaces(sootClass: SootClass): Set[SootClass] = {
      val ifs = sootClass.getInterfaces().toSet
      ifs.foldLeft(ifs)((acc, interfaceClass) => allInterfaces(interfaceClass)++acc)
    }
    def allImplementers(sootClass: SootClass): Set[SootClass] = {
      Scene.v.getOrMakeFastHierarchy.getAllImplementersOfInterface(sootClass).toSet
    }
    def allSubclasses(sootClass: SootClass): Set[SootClass] = {
      val sub = Scene.v.getOrMakeFastHierarchy.getSubclassesOf(sootClass).toSet
      sub.foldLeft(sub)((acc, subclass) => allSubclasses(subclass)++acc)
    }

    if (!System.isLibraryClass(sootClass)) {
      //throw new RuntimeException("Trying to use Snowflake to instantiate a non-library class: " + sootClass.getName + ", abort.")
      Log.error("Trying to use Snowflake to instantiate a non-library class: " + sootClass.getName)
      //return
    }

    val objectBP = Snowflakes.malloc(sootClass)
    destAddr match {
      case Some(addr) => System.store.update(destAddr, D(Set(ObjectValue(sootClass, objectBP))))
      case None => {}
    }
    if (instantiatedClasses.contains(sootClass)) return
    instantiatedClasses = sootClass::instantiatedClasses

    if (sootClass.isInterface) {
      //Log.error("Can not instantiate interface " + sootClass.getName + ".")
      val impls = allImplementers(sootClass)
      for (impl <- impls) { createObject(destAddr, impl) }
      if (impls.isEmpty) {
        //Log.error("interface " + sootClass.getName + " has no implementers, continue.")
        for (iface <- allInterfaces(sootClass)) {
          initStaticFields(iface)
        }
        initStaticFields(sootClass)
      }
      return
    }
    if (!sootClass.isInterface && sootClass.isAbstract) {
      val subs = allSubclasses(sootClass)
      for (subclass <- subs) { createObject(destAddr, subclass) }
      if (subs.nonEmpty) return
      //Log.error("abstract class " + sootClass.getName + " has no subclass, continue.")
    }

    for (superClass <- allSuperClasses(sootClass, Set())) {
      initInstanceFields(superClass, objectBP)
      initStaticFields(superClass)
    }
    for (iface <- allInterfaces(sootClass)) {
      initStaticFields(iface)
    }
    initInstanceFields(sootClass, objectBP)
    initStaticFields(sootClass)
  }
}

case class ReturnObjectSnowflake(name : String) extends SnowflakeHandler {
  override def apply(state : State, nextStmt : Stmt, self : Option[Value], args : List[D]) : Set[AbstractState] = {
    val addrs: Option[Set[Addr]] = state.stmt.sootStmt match {
      case stmt: DefinitionStmt => Some(state.addrsOf(stmt.getLeftOp))
      case stmt: InvokeStmt => None
    }
    DefaultReturnSnowflake.createObject(addrs, Soot.getSootClass(name))
    Set[AbstractState](state.copy(stmt = nextStmt))
  }
}

case class ReturnArraySnowflake(baseType: String, dim: Int) extends SnowflakeHandler {
  override def apply(state : State, nextStmt : Stmt, self : Option[Value], args : List[D]) : Set[AbstractState] = {
    val sizes = List.fill(dim)(D.atomicTop)
    val sootBaseType = Soot.getSootType(baseType)
    val at = soot.ArrayType.v(sootBaseType, dim)
    state.stmt.sootStmt match {
      case stmt : DefinitionStmt => DefaultReturnSnowflake.createArray(at, sizes, state.addrsOf(stmt.getLeftOp))
      case stmt : InvokeStmt =>
    }
    Set(state.copy(stmt=nextStmt))
  }
}

case class DefaultReturnSnowflake(meth : SootMethod) extends SnowflakeHandler {
  import DefaultReturnSnowflake._

  def typesToDs(types: List[Type]): List[D] = {
    def typeToD(ty: Type): D = {
      ty match {
        case _ : PrimType => D.atomicTop
        case at : ArrayType => D(Set(ArrayValue(at, Snowflakes.malloc(at))))
        case rt : RefType =>
          D(Set(ObjectValue(Soot.getSootClass(rt.getClassName), Snowflakes.malloc(rt.getSootClass))))
      }
    }
    types map typeToD
  }

  override def apply(state : State, nextStmt : Stmt, self : Option[Value], args : List[D]) : Set[AbstractState] = {
    // TODO: options for controlling which parts flow into the global address
    for (arg <- args) {
      val d = GlobalD.update(arg.getValues)
      System.store.strongUpdate(GlobalSnowflakeAddr, d, GlobalD.modified)
    }

    self match {
      case Some(target) =>
        val d = GlobalD.update(Set[Value](target))
        System.store.strongUpdate(GlobalSnowflakeAddr, d, GlobalD.modified) // TODO: unneeded?
      case None => {}
    }

    val exceptions = for (exception <- meth.getExceptions) yield {
      ObjectValue(exception, Snowflakes.malloc(exception))
    }
    val exceptionStates = (exceptions map {
      state.kontStack.handleException(_, state.stmt, state.fp)
    }).flatten

    val normalStates = meth.getReturnType match {
      case _ : VoidType => NoOpSnowflake(state, nextStmt, self, args)
      case _ : PrimType =>
        // NOTE: if we eventually do something other than D.atomicTop, we need
        // to track where in the store our return value comes from
        ReturnSnowflake(D.atomicTop)(state, nextStmt, self, args)
      case at : ArrayType =>
        val states = ReturnArraySnowflake(at.baseType.toString, at.numDimensions)(state, nextStmt, self, args)
        val values = System.store.getOrElseBot(GlobalSnowflakeAddr).getValues

        val bp = Snowflakes.malloc(at)
        state.stmt.sootStmt match {
          case stmt : DefinitionStmt =>
            stmt.getLeftOp.getType match {
              case leftAt : ArrayType =>
                val newValues = values.filter(_ match {
                  case ArrayValue(at, bp) => Soot.canStoreType(at, leftAt)
                  case _ => false
                })
                System.store.update(Set[Addr](ArrayRefAddr(bp)), D(newValues))
              case _ => Log.warn("Can not assign an ArrayType value to non-ArrayType. stmt: " + stmt + " meth: " + meth)
            }
          case _ =>
            System.store.update(Set[Addr](ArrayRefAddr(bp)), D(values))
        }
        states
      case rt : RefType =>
        val states = ReturnObjectSnowflake(rt.getClassName)(state, nextStmt, self, args)
        state.stmt.sootStmt match {
          case stmt : DefinitionStmt =>
            val parentClass = stmt.getLeftOp.getType match {
              case rt : RefType => rt.getSootClass
              case _ => throw new RuntimeException("Can not assign a RefType value to non-RefType. stmt: " + stmt + " meth: " + meth)
            }

            val values: Set[Value] = System.store.getOrElseBot(GlobalSnowflakeAddr).getValues
            val newValues = values.filter(_ match {
              case ObjectValue(sootClass, bp) => Soot.canStoreClass(sootClass, parentClass)
              case _ => false
            })
            System.store.update(state.addrsOf(stmt.getLeftOp), D(newValues))
          case _ =>
        }
        states
    }

    // If the argument type is an interface or abstract class, then we try to call
    // each method from the definition of interface/abstract class.
    // TODO: options to control saturation
    // TODO: log what objects are being saturated
    val methodsOfArgs = (for {
      (arg, ty) <- args zip meth.getParameterTypes if ty.isInstanceOf[RefType];
      sootClass = ty.asInstanceOf[RefType].getSootClass;
      if (sootClass.isInterface || sootClass.isAbstract) && System.isLibraryClass(sootClass)
    } yield {
      val newValues = arg.getValues.filter(_ match {
        case ObjectValue(objClass, bp) =>
          !System.isLibraryClass(objClass) && Soot.canStoreClass(objClass, sootClass)
        case _ => false
      })

      (D(newValues), sootClass.getMethods) //TODO: maybe not include <init>?
    })
    //println("methodsOfArgs: " + methodsOfArgs)

    Log.warn("Saturating due to: "+meth)
    val methStates = (for {
      (base, meths) <- methodsOfArgs
      if base.getValues.nonEmpty
      meth <- meths
    } yield {
      Log.warn("Saturating: "+base+" meth: "+meth)
      val params = typesToDs(meth.getParameterTypes.toList)
      state.handleInvoke2(Some((base, false)), meth, params, ZeroCFAFramePointer(meth), None, nextStmt)
    }).flatten
    ///////////////////////////////

    normalStates ++ exceptionStates ++ methStates
  }
}
