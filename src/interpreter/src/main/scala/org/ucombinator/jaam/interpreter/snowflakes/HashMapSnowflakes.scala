package org.ucombinator.jaam.interpreter.snowflakes

import scala.collection.JavaConversions._
import scala.collection.mutable
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}

import org.ucombinator.jaam.interpreter._

// Note: currently disabled
object HashMapSnowflakes {
  lazy val HashMap = Soot.getSootClass("java.util.HashMap")
  lazy val EntrySet = Soot.getSootClass("java.util.Set")
  lazy val Iterator = Soot.getSootClass("java.util.Iterator")
  lazy val Entry = Soot.getSootClass("java.util.Map$" +"Entry") // Spilt up "$Entry to keep linter happy

  Snowflakes.table.put(MethodDescription("java.util.HashMap", SootMethod.constructorName, List(), "void"), HashMapSnowflakes.init())
  Snowflakes.table.put(MethodDescription("java.util.HashMap", "put", List("java.lang.Object", "java.lang.Object"), "java.lang.Object"), HashMapSnowflakes.put())
  Snowflakes.table.put(MethodDescription("java.util.HashMap", "get", List("java.lang.Object"), "java.lang.Object"), HashMapSnowflakes.get())
  Snowflakes.table.put(MethodDescription("java.util.Iterator", "hasNext", List(), "boolean"), hasNext())
  Snowflakes.table.put(MethodDescription("java.util.Iterator", "next", List(), "java.lang.Object"), next())

  case class KeysAddr(val bp : BasePointer) extends Addr
  case class ValuesAddr(val bp : BasePointer) extends Addr

  case class init() extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      // void HashMap.<init>():
      //  this.keys = {}
      //  this.values = {}

      var extraStates = Set[AbstractState]()
      self match {
        case ObjectValue(sootClass, bp) if Soot.canStoreClass(sootClass, HashMap) =>
          System.store.update(KeysAddr(bp), D(Set()))
          System.store.update(ValuesAddr(bp), D(Set()))
        case _ =>
          Snowflakes.warn(state.id, None, null, null)
          extraStates ++= NoOpSnowflake(state, nextStmt, Some(self), args)
      }

      extraStates + state.copy(stmt = nextStmt)
    }
  }

  case class put() extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      // Object HashMap.put(Object o1, Object o2):
      //   this.keys += o1
      //   this.values += o2

      // TODO: avoid duplication with get()
      var extraStates = Set[AbstractState]()
      state.stmt.sootStmt match {
        case stmt : InvokeStmt => {}
        case stmt : DefinitionStmt =>
          self match {
            case ObjectValue(sootClass, bp) if Soot.canStoreClass(sootClass, HashMap) =>
              // D.atomicTop is for the null returned when the key had no previous assignment
              System.store.update(state.addrsOf(stmt.getLeftOp),
                System.store(Set[Addr](ValuesAddr(bp))).join(D.atomicTop))
            case _ =>
              Snowflakes.warn(state.id, None, null, null)
              extraStates ++= ReturnObjectSnowflake("java.lang.Object")(state, nextStmt, Some(self), args)
          }
      }

      val key = args(0)
      val value = args(1)
      self match {
        case ObjectValue(sootClass, bp) if Soot.canStoreClass(sootClass, HashMap) =>
          System.store.update(KeysAddr(bp), key)
          System.store.update(ValuesAddr(bp), value)
        case _ => {} // Already taken care of in the first half of this function
      }

      extraStates + state.copy(stmt = nextStmt)
    }
  }

  case class get() extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      // Object HashMap.get(Object):
      //   return this.values

      var extraStates = Set[AbstractState]()
      state.stmt.sootStmt match {
        case stmt : InvokeStmt => {}
        case stmt : DefinitionStmt =>
          self match {
            case ObjectValue(sootClass, bp) if Soot.canStoreClass(sootClass, HashMap) =>
              // D.atomicTop is for the null returned when the key is not found
              System.store.update(state.addrsOf(stmt.getLeftOp),
                  System.store(Set[Addr](ValuesAddr(bp))).join(D.atomicTop))
            case _ =>
              Snowflakes.warn(state.id, None, null, null)
              extraStates ++= ReturnObjectSnowflake("java.lang.Object")(state, nextStmt, Some(self), args)
          }
      }

      extraStates + state.copy(stmt = nextStmt)
    }
  }

  // java.util.Set HashMap.entrySet():
  //   return this
  case class EntrySetOfHashMap(val bp : BasePointer) extends BasePointer
  case class entrySet() extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      var extraStates = Set[AbstractState]()
      state.stmt.sootStmt match {
        case stmt : InvokeStmt => {}
        case stmt : DefinitionStmt =>
          self match {
            case ObjectValue(sootClass, bp) if Soot.canStoreClass(sootClass, HashMap) =>
              // D.atomicTop is for the null returned when the key is not found
              System.store.update(state.addrsOf(stmt.getLeftOp),
                D(Set(ObjectValue(EntrySet, EntrySetOfHashMap(bp)))))
            case _ =>
              Snowflakes.warn(state.id, None, null, null)
              extraStates ++= ReturnObjectSnowflake("java.util.Set")(state, nextStmt, Some(self), args)
          }
      }

      extraStates + state.copy(stmt = nextStmt)
    }
  }

  // java.util.Iterator EntrySet.iterator():
  //   return this
  case class IteratorOfEntrySetOfHashMap(val bp : BasePointer) extends BasePointer
  case class iterator() extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      var extraStates = Set[AbstractState]()
      state.stmt.sootStmt match {
        case stmt : InvokeStmt => {}
        case stmt : DefinitionStmt =>
          self match {
            case ObjectValue(sootClass, EntrySetOfHashMap(bp)) if Soot.canStoreClass(sootClass, EntrySet) =>
              // D.atomicTop is for the null returned when the key is not found
              System.store.update(state.addrsOf(stmt.getLeftOp),
                D(Set(ObjectValue(Iterator, IteratorOfEntrySetOfHashMap(bp)))))
            case x =>
              Snowflakes.warn(state.id, None, null, null)
              extraStates ++= ReturnObjectSnowflake("java.util.Set")(state, nextStmt, Some(self), args)
          }
      }

      extraStates + state.copy(stmt = nextStmt)
    }
  }

  // boolean Iterator.hasNext():
  //   return atomicTop
  case class hasNext() extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      var extraStates = Set[AbstractState]()
      state.stmt.sootStmt match {
        case stmt : InvokeStmt => {}
        case stmt : DefinitionStmt =>
          self match {
            case ObjectValue(sootClass, IteratorOfEntrySetOfHashMap(bp)) if Soot.canStoreClass(sootClass, Iterator) =>
              // D.atomicTop is for the null returned when the key is not found
              System.store.update(state.addrsOf(stmt.getLeftOp), D.atomicTop)
            case ObjectValue(sootClass, ArrayListSnowflakes.IteratorOfArrayList(bp)) if Soot.canStoreClass(sootClass, Iterator) =>
              // D.atomicTop is for the null returned when the key is not found
              System.store.update(state.addrsOf(stmt.getLeftOp), D.atomicTop)
            case _ =>
              Snowflakes.warn(state.id, None, null, null)
              extraStates ++= ReturnSnowflake(D.atomicTop)(state, nextStmt, Some(self), args)
          }
      }

      extraStates + state.copy(stmt = nextStmt)
    }
  }

  // java.lang.Object Iterator.next()
  //    return (Entry) this
  case class EntryOfIteratorOfEntrySetOfHashMap(val bp : BasePointer) extends BasePointer
  case class next() extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      var extraStates = Set[AbstractState]()
      state.stmt.sootStmt match {
        case stmt : InvokeStmt => {}
        case stmt : DefinitionStmt =>
          self match {
            case ObjectValue(sootClass, IteratorOfEntrySetOfHashMap(bp)) if Soot.canStoreClass(sootClass, Iterator) =>
              // D.atomicTop is for the null returned when the key is not found
              System.store.update(state.addrsOf(stmt.getLeftOp),
                D(Set(ObjectValue(Entry, EntryOfIteratorOfEntrySetOfHashMap(bp)))))
            case ObjectValue(sootClass, ArrayListSnowflakes.IteratorOfArrayList(bp)) if Soot.canStoreClass(sootClass, Iterator) =>
              // D.atomicTop is for the null returned when the key is not found
              // TODO: throw exception
              System.store.update(state.addrsOf(stmt.getLeftOp),
                System.store(Set[Addr](ArrayRefAddr(bp))))
            case _ =>
              Snowflakes.warn(state.id, None, null, null)
              extraStates ++= ReturnObjectSnowflake("java.lang.Object")(state, nextStmt, Some(self), args)
          }
      }

      extraStates + state.copy(stmt = nextStmt)
    }
  }

  // java.lang.Object Entry.getKey()
  case class getKey() extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      var extraStates = Set[AbstractState]()
      state.stmt.sootStmt match {
        case stmt : InvokeStmt => {}
        case stmt : DefinitionStmt =>
          self match {
            case ObjectValue(sootClass, EntryOfIteratorOfEntrySetOfHashMap(bp)) if Soot.canStoreClass(sootClass, Entry) =>
              // D.atomicTop is for the null returned when the key is not found
              System.store.update(state.addrsOf(stmt.getLeftOp),
                System.store(Set[Addr](KeysAddr(bp))).join(D.atomicTop))
            case _ =>
              Snowflakes.warn(state.id, None, null, null)
              extraStates ++= ReturnObjectSnowflake("java.lang.Object")(state, nextStmt, Some(self), args)
          }
      }

      extraStates + state.copy(stmt = nextStmt)
    }
  }

  // java.lang.Object getValue()
  case class getValue() extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      var extraStates = Set[AbstractState]()
      state.stmt.sootStmt match {
        case stmt : InvokeStmt => {}
        case stmt : DefinitionStmt =>
          self match {
            case ObjectValue(sootClass, EntryOfIteratorOfEntrySetOfHashMap(bp)) if Soot.canStoreClass(sootClass, Entry) =>
              // D.atomicTop is for the null returned when the key is not found
              System.store.update(state.addrsOf(stmt.getLeftOp),
                System.store(Set[Addr](ValuesAddr(bp))).join(D.atomicTop))
            case _ =>
              Snowflakes.warn(state.id, None, null, null)
              extraStates ++= ReturnObjectSnowflake("java.lang.Object")(state, nextStmt, Some(self), args)
          }
      }

      extraStates + state.copy(stmt = nextStmt)
    }
  }
}
