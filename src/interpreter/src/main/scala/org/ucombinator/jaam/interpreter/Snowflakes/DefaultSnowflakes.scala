package org.ucombinator.jaam.interpreter.snowflakes

import scala.collection.JavaConversions._
import scala.collection.mutable
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}

import org.ucombinator.jaam.interpreter._

case class DefaultReturnSnowflake(meth : SootMethod) extends SnowflakeHandler {
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
