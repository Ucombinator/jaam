package org.ucombinator.jaam.interpreter.snowflakes

import scala.collection.JavaConversions._
import scala.collection.mutable
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}

import org.ucombinator.jaam.interpreter._
import org.ucombinator.jaam.util.{Stmt, Soot}

// Note: currently disabled
object ArrayListSnowflakes {
  lazy val ArrayList = Soot.getSootClass("java.util.ArrayList")
  lazy val Iterator = Soot.getSootClass("java.util.Iterator")

  Snowflakes.put(MethodDescription("java.util.ArrayList", SootMethod.constructorName, List(), "void"), ArrayListSnowflakes.init())
  Snowflakes.put(MethodDescription("java.util.ArrayList", "add", List("java.lang.Object"), "boolean"), ArrayListSnowflakes.add())
  Snowflakes.put(MethodDescription("java.util.ArrayList", "iterator", List(), "java.util.Iterator"), ArrayListSnowflakes.iterator())

  case class init() extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      // void ArrayList.<init>():
      //  this.length = top
      //  this.refs = {}
      var extraStates = Set[AbstractState]()
      self match {
        case ObjectValue(sootClass, bp) if Soot.canStoreClass(sootClass, ArrayList) =>
          System.store.update(ArrayLengthAddr(bp), D.atomicTop)
          System.store.update(ArrayRefAddr(bp), D.atomicTop) // D.atomicTop is so we don't get undefiend addrs exception
        case _ =>
          Snowflakes.warn(state.id, None, null, null)
          extraStates ++= NoOpSnowflake(state, nextStmt, Some(self), args)
      }

      extraStates + state.copy(stmt = nextStmt)
    }
  }

  case class add() extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      // boolean ArrayList.add(Object o)
      //   this.length += top
      //   this.refs += o
      // TODO: avoid duplication
      var extraStates = Set[AbstractState]()
      state.stmt.sootStmt match {
        case stmt : InvokeStmt => {}
        case stmt : DefinitionStmt =>
          self match {
            case ObjectValue(sootClass, bp) if Soot.canStoreClass(sootClass, ArrayList) =>
              System.store.update(state.addrsOf(stmt.getLeftOp), D.atomicTop)
            case _ =>
              Snowflakes.warn(state.id, None, null, null)
              extraStates ++= ReturnAtomicSnowflake(state, nextStmt, Some(self), args)
          }
      }

      val value = args(0)
      self match {
        case ObjectValue(sootClass, bp) if Soot.canStoreClass(sootClass, ArrayList) =>
          System.store.update(ArrayRefAddr(bp), value)
        case _ => {} // already handled by the code in the first half of this function
      }

      extraStates + state.copy(stmt = nextStmt)
    }
  }

  // java.util.Iterator ArrayList.iterator():
  //   return this
  case class IteratorOfArrayList(val bp : BasePointer) extends BasePointer
  case class iterator() extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      var extraStates = Set[AbstractState]()
      state.stmt.sootStmt match {
        case stmt : InvokeStmt => {}
        case stmt : DefinitionStmt =>
          self match {
            case ObjectValue(sootClass, bp) if Soot.canStoreClass(sootClass, ArrayList) =>
              // D.atomicTop is for the null returned when the key is not found
              System.store.update(state.addrsOf(stmt.getLeftOp),
                D(Set(ObjectValue(Iterator, IteratorOfArrayList(bp)))))
            case _ =>
              Snowflakes.warn(state.id, None, null, null)
              extraStates ++= ReturnObjectSnowflake("java.util.Iterator")(state, nextStmt, Some(self), args)
          }
      }

      extraStates + state.copy(stmt = nextStmt)
    }
  }
}
