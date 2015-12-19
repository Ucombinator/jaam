package org.ucombinator.analyzer

import scala.collection.JavaConversions._

import soot._
import soot.jimple._

// Snowflakes are special Java procedures whose behavior we know and special-case.
// For example, native methods (that would be difficult to analyze) are snowflakes.

case class SnowflakeBasePointer(val clas : String) extends BasePointer

// Uniquely identifies a particular method somewhere in the program.
case class MethodDescription(val className : String,
                             val methodName : String,
                             val parameterTypes : List[String],
                             val returnType : String)

// Snowflakes are special-cased methods
abstract class SnowflakeHandler {
  def apply(state : State,
            nextStmt : Stmt,
            newFP : FramePointer,
            newStore : Store,
            newKontStack : KontStack) : Set[AbstractState]
}

object Snowflakes {
  val table = scala.collection.mutable.Map.empty[MethodDescription, SnowflakeHandler]

  Snowflakes.put(MethodDescription("java.lang.System", SootMethod.staticInitializerName, List(), "void"),
    new SnowflakeHandler {
      override def apply(state : State,
        nextStmt : Stmt,
        newFP : FramePointer,
        newStore : Store,
        newKontStack : KontStack) = {
        def updateStore(oldStore : Store, clas : String, field : String, typ : String) =
          oldStore.update(StaticFieldAddr(Soot.getSootClass(clas).getFieldByName(field)),
            D(Set(ObjectValue(Soot.getSootClass(typ),
              SnowflakeBasePointer(clas + "." + field)))))
        var newNewStore = newStore
        newNewStore = updateStore(newNewStore, "java.lang.System", "in", "java.io.InputStream")
        newNewStore = updateStore(newNewStore, "java.lang.System", "out", "java.io.PrintStream")
        newNewStore = updateStore(newNewStore, "java.lang.System", "err", "java.io.PrintStream")
        newNewStore = updateStore(newNewStore, "java.lang.System", "security", "java.lang.SecurityManager")
        newNewStore = updateStore(newNewStore, "java.lang.System", "cons", "java.io.Console")

        Set(state.copy(stmt = nextStmt, store = newNewStore))
      }
    })
  Snowflakes.put(MethodDescription("java.io.PrintStream", "println", List("int"), "void"), NoOpSnowflake)
  Snowflakes.put(MethodDescription("java.io.PrintStream", "println", List("java.lang.String"), "void"), NoOpSnowflake)
  Snowflakes.put(MethodDescription("java.lang.Class", "desiredAssertionStatus", List(), "boolean"), ConstSnowflake(D.atomicTop))
  Snowflakes.put(MethodDescription("java.lang.System", "nanoTime", List(), "long"), ConstSnowflake(D.atomicTop))
  Snowflakes.put(MethodDescription("java.lang.System", "currentTimeMillis", List(), "long"), ConstSnowflake(D.atomicTop))
  Snowflakes.put(MethodDescription("java.lang.System", "identityHashCode", List("java.lang.Object"), "int"), ConstSnowflake(D.atomicTop))
  Snowflakes.put(MethodDescription("java.lang.Throwable", SootMethod.constructorName, List(), "void"), NoOpSnowflake)
  Snowflakes.put(MethodDescription("java.lang.Throwable", SootMethod.staticInitializerName, List(), "void"), NoOpSnowflake)
  Snowflakes.put(MethodDescription("java.util.ArrayList", SootMethod.constructorName, List("int"), "void"), NoOpSnowflake)

  def get(meth : SootMethod) : Option[SnowflakeHandler] =
    table.get(MethodDescription(
      meth.getDeclaringClass.getName,
      meth.getName,
      meth.getParameterTypes.toList.map(_.toString()),
      meth.getReturnType.toString()))
  def put(md : MethodDescription, handler : SnowflakeHandler) { table.put(md, handler) }
}

object NoOpSnowflake extends SnowflakeHandler {
  override def apply(state : State,
                     nextStmt : Stmt,
                     newFP : FramePointer,
                     newStore : Store,
                     newKontStack : KontStack) : Set[AbstractState] =
    Set(state.copy(stmt = nextStmt))
}

// TODO/soundness: Add JohnSnowflake for black-holes. Not everything becomes top, but an awful lot will.

case class ConstSnowflake(value : D) extends SnowflakeHandler {
  override def apply(state : State, nextStmt : Stmt, newFP : FramePointer, newStore : Store, newKontStack : KontStack) = {
    val newNewStore = state.stmt.inst match {
      case inst : DefinitionStmt => state.store.update(state.addrsOf(inst.getLeftOp()), value)
      case inst : InvokeStmt => state.store
    }
    Set(state.copy(stmt = nextStmt, store = newNewStore))
  }
}
