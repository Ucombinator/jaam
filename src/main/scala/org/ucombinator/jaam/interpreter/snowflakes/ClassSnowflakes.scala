package org.ucombinator.jaam.interpreter.snowflakes

import scala.collection.JavaConversions._
import scala.collection.mutable
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}

import org.ucombinator.jaam.main.Log
import org.ucombinator.jaam.interpreter._
import org.ucombinator.jaam.util.{Soot, Stmt}

// Note: currently enabled
object ClassSnowflakes {
  Snowflakes.put(MethodDescription("java.lang.Class", "newInstance", List(), "java.lang.Object"), newInstance)
  case object newInstance extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      val local = state.stmt.sootStmt match {
        case stmt : DefinitionStmt => stmt.getLeftOp().asInstanceOf[Local]
      }
      val lhsAddr = state.addrsOf(local)

      val exceptionClass = Soot.getSootClass("java.lang.InstantiationException")
      val instatiationException = ObjectValue(exceptionClass, Snowflakes.malloc(exceptionClass))
      self match {
        case ObjectValue(_, ClassBasePointer(className)) =>
          if (className.startsWith("[")) {
            state.kontStack.handleException(instatiationException, state.stmt, state.fp)
          }
          else {
            val sootClass = Soot.getSootClass(className)
            if (sootClass.isInterface || sootClass.isAbstract) {
              state.kontStack.handleException(instatiationException, state.stmt, state.fp)
            }
            else {
              //val state2 = state.copy(store = state.newExpr(lhsAddr, sootClass, System.store))
              state.newExpr(lhsAddr, sootClass)

              try { // TODO: this is a bit of a hack
                val expr = new soot.jimple.internal.JSpecialInvokeExpr(
                  local, //new soot.jimple.internal.JimpleLocal("newInstanceSnowflake", sootClass.getType()),
                  sootClass.getMethod(SootMethod.constructorName, List(), VoidType.v()).makeRef(),
                  List[soot.Value]())
                //state2.handleInvoke(expr, None)
                state.handleInvoke(expr, None)
              } catch {
                // If it doesn't have a no argument, then we must be on a spurious flow
                case _: Exception => Set()
              }
            }
          }
        case _ =>
          Log.error("Unimplemented: newInstance on "+self)
          Set()
      }
    }
  }

  Snowflakes.put(MethodDescription("java.lang.Class", "getName", List(), "java.lang.String"), getName)
  case object getName extends NonstaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
      val local = state.stmt.sootStmt match {
        case stmt : DefinitionStmt => stmt.getLeftOp().asInstanceOf[Local]
      }
      val lhsAddr = state.addrsOf(local)

      Log.info("getName self: "+self)
      self match {
        case ObjectValue(_, ClassBasePointer(className)) =>
          System.store.update(lhsAddr, D(Set(ObjectValue(Soot.classes.Class, StringBasePointer(className, state)))))
          Set(state.copy(stmt = nextStmt))
        case _ =>
          Log.error("Unimplemented: getName on "+self)
          Set()
      }
    }
  }

  //private static native java.lang.Class<?> forName0(java.lang.String, boolean, java.lang.ClassLoader, java.lang.Class<?>) throws java.lang.ClassNotFoundException;
  Snowflakes.put(MethodDescription("java.lang.Class", "forName0", List("java.lang.String", "boolean", "java.lang.ClassLoader", "java.lang.Class"), "java.lang.Class"), forName0)
  case object forName0 extends StaticSnowflakeHandler {
    override def apply(state: State, nextStmt: Stmt, args: List[D]): Set[AbstractState] = {
//      Log.error(s"forName0\n  state: $state\n  nextStmt: $nextStmt\n  args: $args")
      val className = args(0)
      var classes = D(Set())
      for (v <- className.getValues) {
        v match {
          case ObjectValue(_, LiteralStringBasePointer(s)) =>
            if (Soot.isClass(s)) {
/*
            Log.error("allow phantom: "+Scene.v().getPhantomRefs())
            Log.error("forName0 ok: "+s+".")
            Log.error("forName0 containsClass: "+SourceLocator.v().getClassSource("com.stac.Main"))
            Log.error("forName0 containsClass: "+SourceLocator.v().getClassSource(s))
            Log.error("forName0 containsClass: "+SourceLocator.v().getClassSource("foobar.poiuwer"))
            //Log.error("forName0 containsClass: "+soot.Scene.v().containsClass(s))
            //val sc = soot.Scene.v().getSootClassUnsafe(s)
            // TryloadClass, NO loadClassAndSupport, loadClass, NO getSootClassUnsafe
            //Log.error("forName0 getSootClassUnsafe: "+sc)
            //Log.error("forName0 getSootClassUnsafe.isPhantom: "+sc.isPhantom)
            //Log.error("forName0 foobar2: "+soot.Scene.v().loadClass("foobar", SootClass.BODIES))
 */
              classes = classes.join(D(Set(ObjectValue(Soot.classes.Class, ClassBasePointer(s.replace('/', '.')))))) // TODO: replace might be unneeded; put a check in the ClassBasePointer constructor
            }
          case ObjectValue(_, StringBasePointerTop) =>
            for (StringLiteralValue(s) <- System.store.getOrElseBot(StringLiteralAddr).getValues) {
              if (Soot.isClass(s)) {
                classes = classes.join(D(Set(ObjectValue(Soot.classes.Class, ClassBasePointer(s.replace('/', '.')))))) // TODO: replace might be unneeded; put a check in the ClassBasePointer constructor
              }
            }
          case ObjectValue(c,_) if c == Soot.classes.String =>
            Log.error("java.lang.Class.forName0 ignoring non-literal String: "+v)
          case _ => {}
        }
      }
      Log.error(f"forName0: $className $classes")
      // TODO: factor this with ReturnSnowflake
      state.stmt.sootStmt match {
        case sootStmt : DefinitionStmt => System.store.update(state.addrsOf(sootStmt.getLeftOp()), classes)
        case sootStmt : InvokeStmt => {}
      }
      Set(state.copy(stmt = nextStmt))
    }
  }
}
