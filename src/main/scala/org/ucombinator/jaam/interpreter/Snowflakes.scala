package org.ucombinator.jaam.interpreter

import scala.collection.JavaConversions._
import scala.collection.mutable

import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}

import org.ucombinator.jaam.interpreter.snowflakes._

// Snowflakes are special Java procedures whose behavior we know and special-case.
// For example, native methods (that would be difficult to analyze) are snowflakes.

// TODO: make SnowflakeHandler record its method description
// TODO: this and params method
// TODO: returns method
// TODO: make SnowflakeBasePointer take a Class object instead of a String

// TODO: why do we have different kinds of SnowflakeBasePointer? is there a better way?
abstract class AbstractSnowflakeBasePointer extends BasePointer
case class SnowflakeBasePointer(name: String) extends AbstractSnowflakeBasePointer
case class SnowflakeArrayBasePointer(at: ArrayType) extends AbstractSnowflakeBasePointer
case class SnowflakeInterfaceBasePointer(name: String) extends AbstractSnowflakeBasePointer
case class SnowflakeAbstractClassBasePointer(name: String) extends AbstractSnowflakeBasePointer

// Uniquely identifies a particular method somewhere in the program.
case class MethodDescription(className: String, methodName: String,
                             parameterTypes: List[String], returnType: String) extends CachedHashCode {
  override def toString() = {
    val typesStr = parameterTypes.mkString(",")
    s"${className}::${returnType} ${methodName}(${typesStr})"
  }

  def getSootClass() = { Soot.getSootClass(className) }

  def getSootMethod() = {
    val clazz = getSootClass
    val paramTypes = parameterTypes.map(Utilities.stringToType)
    val retType = Utilities.stringToType(returnType)
    clazz.getMethod(methodName, paramTypes, retType)
  }
}

// Snowflakes are special-cased methods
abstract class SnowflakeHandler {
  // self is None if this is a static call and Some otherwise
  def apply(state : State, nextStmt : Stmt, self : Option[Value], args : List[D]) : Set[AbstractState]
}

trait NativeSnowflakeHandler extends SnowflakeHandler
trait JavaLibrarySnowflakeHandler extends SnowflakeHandler
trait AppLibrarySnowflakeHandler extends SnowflakeHandler

abstract class StaticSnowflakeHandler extends SnowflakeHandler {
  def apply(state : State, nextStmt : Stmt, args : List[D]) : Set[AbstractState]

  override def apply(state : State, nextStmt : Stmt, self : Option[Value], args : List[D]) : Set[AbstractState] =
    self match {
      case None => this.apply(state, nextStmt, args)
      case Some(_) => throw new Exception("Static Snowflake used on non-static call. snowflake = "+this+" state = "+state)
    }
}

abstract class NonstaticSnowflakeHandler extends SnowflakeHandler {
  def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState]

  override def apply(state : State, nextStmt : Stmt, self : Option[Value], args : List[D]) : Set[AbstractState] =
    self match {
      case None => throw new Exception("Non-static Snowflake used on static call. snowflake = "+this+" state = "+state)
      case Some(s) => this.apply(state, nextStmt, s, args)
    }
}

// TODO/soundness: Add JohnSnowflake for black-holes (i.e., you know nothing). Not everything becomes top, but an awful lot will.

case class PutStaticSnowflake(clas : String, field : String) extends StaticSnowflakeHandler {
  override def apply(state : State, nextStmt : Stmt, args : List[D]) : Set[AbstractState] = {
    val sootField = Jimple.v.newStaticFieldRef(Soot.getSootClass(clas).getFieldByName(field).makeRef())
    val value = args(0)
    System.store.update(state.addrsOf(sootField), value)
    Set(state.copy(stmt = nextStmt))
  }
}

object Snowflakes {
  /* All natie method handlers (in both Java library and application library)
     should put into nativeTable. */
  private val nativeTable = mutable.Map.empty[MethodDescription, SnowflakeHandler]
  private val javaLibTable = mutable.Map.empty[MethodDescription, SnowflakeHandler]
  private val appTable = mutable.Map.empty[MethodDescription, SnowflakeHandler]

  // enable Snowflakes for registered native methods
  var nativeSF = true
  // enable Generic Snowflake for other native methods
  var nativeGenericSF = true
  // enable Snowflakes for registered Java library methdos
  var librarySF = true
  // enable Generic Snowflake for other Java library methods
  var libraryGenericSF = true
  // enable Snowflakes for registered application library methods
  var appLibrarySF = true
  // enable Generic Snowflake for other application library methods
  var appLibraryGenericSF = true

  // TODO: take state as argument? (so id is clearly the state id)
  // TODO: take reason as argument (e.g., native, java library, app library, etc.)
  def warn(id : Int, self: Option[Value], stmt : Stmt, meth : SootMethod) {
    Log.warn("Using generic snowflake in state "+id+". May be unsound." +
      " self = " + self +
      " stmt = " + stmt +
      " method = " + meth)
  }

  def put(md: MethodDescription, sf: SnowflakeHandler) {
    println(s"Putting ${md}")
    val clazz = md.getSootClass
    val meth = md.getSootMethod
    if (meth.isNative) {
      nativeTable.put(md, sf)
    }
    else if (System.isJavaLibraryClass(clazz)) {
      javaLibTable.put(md, sf)
    }
    else {
      appTable.put(md, sf) //should be a application class
    }
  }

  def get(md: MethodDescription, meth: SootMethod): Option[SnowflakeHandler] = {
    def lookupHelper(m: mutable.Map[MethodDescription, SnowflakeHandler],
                     cond: Boolean,
                     default: SnowflakeHandler): Option[SnowflakeHandler] = {
      m.get(md) match {
        case None => if (cond) Some(default) else None
        case Some(h) => Some(h)
      }
    }

    if (meth.isNative && nativeSF) {
      lookupHelper(nativeTable, nativeGenericSF, DefaultReturnSnowflake(meth))
    }
    else if (System.isJavaLibraryClass(meth.getDeclaringClass) && librarySF) {
      lookupHelper(javaLibTable, libraryGenericSF, DefaultReturnSnowflake(meth))
    }
    else if (System.isAppLibraryClass(meth.getDeclaringClass) && appLibrarySF) {
      lookupHelper(appTable, appLibraryGenericSF, DefaultReturnSnowflake(meth))
    }
    else { None }
  }

  def dispatch(meth : SootMethod) : Option[SnowflakeHandler] = {
    val md = MethodDescription(
      meth.getDeclaringClass.getName,
      meth.getName,
      meth.getParameterTypes.toList.map(_.toString()),
      meth.getReturnType.toString())
    get(md, meth)
  }

  def contains(meth : MethodDescription) : Boolean =
    nativeTable.contains(meth) || javaLibTable.contains(meth) || appTable.contains(meth)

  def malloc(sootClass: SootClass): AbstractSnowflakeBasePointer = {
    if (sootClass.isInterface) SnowflakeInterfaceBasePointer(sootClass.getName)
    else if (sootClass.isAbstract) SnowflakeAbstractClassBasePointer(sootClass.getName)
    else SnowflakeBasePointer(sootClass.getName)
  }
  def malloc(at: ArrayType): AbstractSnowflakeBasePointer = { SnowflakeArrayBasePointer(at) }
  def malloc(name: String): AbstractSnowflakeBasePointer = { SnowflakeBasePointer(name) }

  def isSnowflakeObject(v: Value): Boolean = v.isInstanceOf[ObjectValue] && isSnowflakeObject(v.asInstanceOf[ObjectValue])
  def isSnowflakeObject(v: ObjectValue): Boolean = isSnowflakeObject(v.bp)
  def isSnowflakeObject(v: BasePointer): Boolean = v.isInstanceOf[AbstractSnowflakeBasePointer]

  private def updateStore(oldStore : Store, clas : String, field : String, typ : String) =
    oldStore.update(StaticFieldAddr(Soot.getSootClass(clas).getFieldByName(field)),
      D(Set(ObjectValue(Soot.getSootClass(typ),
        Snowflakes.malloc(clas + "." + field))))).asInstanceOf[Store]

  ClassSnowflakes

  //java.lang.System :: static void arraycopy(java.lang.Object, int, java.lang.Object, int, int)
  put(MethodDescription("java.lang.System", "arraycopy",
    List("java.lang.Object", "int", "java.lang.Object", "int", "int"), "void"), new StaticSnowflakeHandler {
    override def apply(state : State, nextStmt : Stmt, args : List[D]) : Set[AbstractState] = {
      assert(state.stmt.sootStmt.getInvokeExpr.getArgCount == 5)
      val expr = state.stmt.sootStmt.getInvokeExpr
      System.store.update(state.addrsOf(expr.getArg(2)), state.eval(expr.getArg(0)))
      Set(state.copy(stmt = nextStmt))
    }
  })


  // java.lang.System :: static void <clinit>()
  put(MethodDescription("java.lang.System", SootMethod.staticInitializerName, List(), "void"),
    new StaticSnowflakeHandler {
      override def apply(state : State, nextStmt : Stmt, args : List[D]) : Set[AbstractState] = {
        var newNewStore = System.store
        newNewStore = updateStore(newNewStore, "java.lang.System", "in", "java.io.InputStream")
        newNewStore = updateStore(newNewStore, "java.lang.System", "out", "java.io.PrintStream")
        newNewStore = updateStore(newNewStore, "java.lang.System", "err", "java.io.PrintStream")
        newNewStore = updateStore(newNewStore, "java.lang.System", "security", "java.lang.SecurityManager")
        newNewStore = updateStore(newNewStore, "java.lang.System", "cons", "java.io.Console")
        newNewStore = updateStore(newNewStore, "java.lang.System", "props", "java.util.Properties")
        newNewStore = updateStore(newNewStore, "java.lang.System", "lineSeparator", "java.lang.String")
        Set(state.copy(stmt = nextStmt))
      }
    })

  // By skipping the code for `java.lang.Object.<init>()` we avoid a state convergence of every constructor call
  // java.lang.Object :: void <init>()
  put(MethodDescription("java.lang.Object", SootMethod.constructorName, List(), "void"), NoOpSnowflake)

  // java.lang.Object :: boolean equals(java.lang.Object)
  put(MethodDescription("java.lang.Object", "equals", List("java.lang.Object"), "boolean"), ReturnAtomicSnowflake)

  // java.lang.Object :: java.lang.Object clone()
  put(MethodDescription("java.lang.Object", "clone", List(), "java.lang.Object"),
    new NonstaticSnowflakeHandler {
      override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
        val newNewStore = state.stmt.sootStmt match {
          case stmt : DefinitionStmt => System.store.update(state.addrsOf(stmt.getLeftOp), D(Set(self)))
          case stmt : InvokeStmt => System.store
        }
        Set(state.copy(stmt = nextStmt))
      }
    })

  // java.securiy.AccessController :: static java.lang.Object doPrivileged(java.security.PrivilegedAction)
  put(MethodDescription("java.security.AccessController", "doPrivileged", List("java.security.PrivilegedAction"), "java.lang.Object"),
    new StaticSnowflakeHandler {
      lazy val method = Soot.getSootClass("java.security.PrivilegedAction").getMethodByName("run")
      override def apply(state : State, nextStmt : Stmt, args : List[D]) : Set[AbstractState] = {
        // TODO: expr as argument to apply?
        // TODO: dest as argument to apply
        val expr = state.stmt.sootStmt match {
          case sootStmt : InvokeStmt => sootStmt.getInvokeExpr
          case sootStmt : DefinitionStmt =>
            sootStmt.getRightOp().asInstanceOf[InvokeExpr]
        }

        val dest = state.stmt.sootStmt match {
          case sootStmt : DefinitionStmt => Some(state.addrsOf(sootStmt.getLeftOp()))
          case sootStmt : InvokeStmt => None
        }

        state.handleInvoke2(Some((args(0), false)), method, List(), state.alloca(expr, nextStmt), dest, nextStmt /*TODO: this is not a real nextStmt; we just need to put something here*/)
      }
    })

  // java.lang.Thread :: void <init>(java.lang.Runnable)
  put(MethodDescription("java.lang.Thread", SootMethod.constructorName, List("java.lang.Runnable"), "void"),
    new NonstaticSnowflakeHandler {
      lazy val method = Soot.getSootClass("java.lang.Runnable").getMethodByName("run")
      override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
        // TODO: expr as argument to apply?
        // TODO: dest as argument to apply
        val expr = state.stmt.sootStmt match {
          case sootStmt : InvokeStmt => sootStmt.getInvokeExpr
          case sootStmt : DefinitionStmt =>
            sootStmt.getRightOp().asInstanceOf[InvokeExpr]
        }

        val dest = state.stmt.sootStmt match {
          case sootStmt : DefinitionStmt => Some(state.addrsOf(sootStmt.getLeftOp()))
          case sootStmt : InvokeStmt => None
        }

        state.handleInvoke2(Some((args(0), false)), method, List(), state.alloca(expr, nextStmt), dest, nextStmt /*TODO: this is not a real nextStmt; we just need to put something here*/) + state.copy(stmt = nextStmt)
      }
    })

  //Path start, FileVisitor<? super Path> visitor)
  //java.nio.file.Files :: static java.nio.file.Path walkFileTree(java.nio.file.Path, java.nio.file.FileVisitor)
  put(MethodDescription("java.nio.file.Files", "walkFileTree", List("java.nio.file.Path", "java.nio.file.FileVisitor"), "java.nio.file.Path"),
    new StaticSnowflakeHandler {
      lazy val cls = Soot.getSootClass("java.nio.file.FileVisitor")

      lazy val postVisitDirectory = cls.getMethodByName("postVisitDirectory")
      lazy val preVisitDirectory = cls.getMethodByName("preVisitDirectory")
      lazy val visitFile = cls.getMethodByName("visitFile")
      lazy val visitFileFailed = cls.getMethodByName("visitFileFailed")

      lazy val pathType = Soot.getSootClass("java.nio.file.Path")
      lazy val attributesType = Soot.getSootClass("java.nio.file.attribute.BasicFileAttributes")
      lazy val exceptionType = Soot.getSootClass("java.io.IOException")
      lazy val pathParam = D(Set(ObjectValue(pathType, Snowflakes.malloc("java.nio.file.Files.walkFileTree.Path"))))
      lazy val attributesParam = D(Set(ObjectValue(attributesType, Snowflakes.malloc("java.nio.file.Files.walkFileTree.BasicFileAttributes"))))
      lazy val exceptionParam = D(Set(ObjectValue(exceptionType, Snowflakes.malloc("java.nio.file.Files.walkFileTree.IOException"))))

      override def apply(state : State, nextStmt : Stmt, args : List[D]) : Set[AbstractState] = {
        // TODO: expr as argument to apply?
        // TODO: dest as argument to apply
        val expr = state.stmt.sootStmt match {
          case sootStmt : InvokeStmt => sootStmt.getInvokeExpr
          case sootStmt : DefinitionStmt =>
            sootStmt.getRightOp().asInstanceOf[InvokeExpr]
        }

        val fp = state.alloca(expr, nextStmt)

        state.handleInvoke2(Some((args(1), false)), postVisitDirectory, List(pathParam, exceptionParam), fp, None, nextStmt /*TODO: this is not a real nextStmt; we just need to put something here*/) ++
        state.handleInvoke2(Some((args(1), false)), preVisitDirectory, List(pathParam, attributesParam), fp, None, nextStmt /*TODO: this is not a real nextStmt; we just need to put something here*/) ++
        state.handleInvoke2(Some((args(1), false)), visitFile, List(pathParam, attributesParam), fp, None, nextStmt /*TODO: this is not a real nextStmt; we just need to put something here*/) ++
        state.handleInvoke2(Some((args(1), false)), visitFileFailed, List(pathParam, exceptionParam), fp, None, nextStmt /*TODO: this is not a real nextStmt; we just need to put something here*/)
      }
    })

  println("Snowflake Initializing")

//    table.put(MethodDescription("com.cyberpointllc.stac.hashmap.Node", "hash", List("java.lang.Object", "int"), "int"), ReturnAtomicSnowflake)
    /*
    table.put(MethodDescription("com.sun.net.httpserver.HttpServer", "createContext",
      List("java.lang.String", "com.sun.net.httpserver.HttpHandler"), "com.sun.net.httpserver.HttpContext"),
      new SnowflakeHandler {
        val httpsExchange = "com.sun.net.httpserver.HttpsExchangeImpl"
        val absHttpHandler = Soot.getSootClass("com.cyberpointllc.stac.webserver.handler.AbstractHttpHandler")
        override def apply(state: State, nextStmt: Stmt, self: Option[Value], args: List[D]): Set[AbstractState] = {
          val handlers = args.get(1).values
          val newStore = Snowflakes.createObject(httpsExchange, List())
          System.store.join(newStore)

          val meth = absHttpHandler.getMethodByName("handle")
          val newFP = ZeroCFAFramePointer(meth)
          val handlerStates: Set[AbstractState] =
            for (ObjectValue(sootClass, bp) <- handlers
              //if (Soot.canStoreClass(sootClass, absHttpHandler) && sootClass.isConcrete)) yield {
              if Soot.canStoreClass(sootClass, absHttpHandler)) yield {
              System.store.update(ThisFrameAddr(newFP), D(Set(ObjectValue(sootClass, bp))))
              System.store.update(ParameterFrameAddr(newFP, 0),
                D(Set(ObjectValue(Soot.getSootClass(httpsExchange), SnowflakeBasePointer(httpsExchange)))))
              State(Stmt.methodEntry(meth), newFP, state.kontStack)
          }

          val retStates = ReturnObjectSnowflake("sun.net.httpserver.HttpContextImpl").apply(state, nextStmt, self, args)
          retStates ++ handlerStates
        }
      })
      */

  // For running Image Processor
  //table.put(MethodDescription("java.lang.System", "getProperty", List("java.lang.String"), "java.lang.String"),
  //  ReturnAtomicSnowflake(D(Set(ObjectValue(Soot.classes.String, StringBasePointer("returns from getProperty")))))) // TODO: StringBasePointerTop
  //
  //table.put(MethodDescription("java.nio.file.Paths", "get", List("java.lang.String", "java.lang.String[]"), "java.nio.file.Path"), ReturnObjectSnowflake("java.nio.file.Path"))
  //table.put(MethodDescription("java.util.HashMap", SootMethod.constructorName, List(), "void"),
  //  ReturnObjectSnowflake("java.util.HashMap"))

  // For running gabfeed_1
//    118 Snowflake due to Abstract: <com.sun.net.httpserver.HttpContext: java.util.List getFilters()>
//    656 Snowflake due to Abstract: <com.sun.net.httpserver.HttpServer: com.sun.net.httpserver.HttpContext createContext(java.lang.String,com.sun.net.httpserver.HttpHandler)>
//    202 Snowflake due to Abstract: <com.sun.net.httpserver.HttpServer: void setExecutor(java.util.concurrent.Executor)>

  /*
  table.put(MethodDescription("com.sun.net.httpserver.HttpServer", "createContext", List("java.lang.String", "com.sun.net.httpserver.HttpHandler"), "com.sun.net.httpserver.HttpContext"),
    new NonstaticSnowflakeHandler {
      lazy val method = Soot.getSootClass("com.sun.net.httpserver.HttpHandler").getMethodByName("handle")
      lazy val ReturnAtomicSnowflake = ReturnObjectSnowflake("com.sun.net.httpserver.HttpContext")
      override def apply(state : State, nextStmt : Stmt, self : Value, args : List[D]) : Set[AbstractState] = {
        val expr = state.stmt.sootStmt match {
          case sootStmt : InvokeStmt => sootStmt.getInvokeExpr
          case sootStmt : DefinitionStmt =>
            sootStmt.getRightOp().asInstanceOf[InvokeExpr]
        }

        val exchange = Snowflakes.createObjectOrThrow("com.sun.net.httpserver.HttpExchang") /// TODO: Put with global snowflakes?
        val s1 = state.handleInvoke2(Some((args(1), false)), method, List(exchange), state.alloca(expr, nextStmt), None, nextStmt /*TODO: this is not a real nextStmt; we just need to put something here*/)
        val s2 = ReturnAtomicSnowflake(state, nextStmt, Some(self), args)
        s1 ++ s2
      }
    })
  */
  // java.io.PrintStream
  //table.put(MethodDescription("java.io.PrintStream", "println", List("int"), "void"), NoOpSnowflake)
  //table.put(MethodDescription("java.io.PrintStream", "println", List("java.lang.String"), "void"), NoOpSnowflake)

  //HashMapSnowflakes // this triggers HashMapSnowflakes to add snowflake entries
  //ArrayListSnowflakes

  // java.lang.Class
  //private static native void registerNatives();
  //private static native java.lang.Class<?> forName0(java.lang.String, boolean, java.lang.ClassLoader, java.lang.Class<?>) throws java.lang.ClassNotFoundException;
  //public native boolean isInstance(java.lang.Object);
  //public native boolean isAssignableFrom(java.lang.Class<?>);
  //public native boolean isInterface();
  //public native boolean isArray();
  //public native boolean isPrimitive();
  //private native java.lang.String getName0();
  //public native java.lang.Class<? super T> getSuperclass();
  //public native java.lang.Class<?>[] getInterfaces();
  //public native java.lang.Class<?> getComponentType();
  //public native int getModifiers();
  //public native java.lang.Object[] getSigners();
  //native void setSigners(java.lang.Object[]);
  //private native java.lang.Object[] getEnclosingMethod0();
  //private native java.lang.Class<?> getDeclaringClass0();
  //private native java.security.ProtectionDomain getProtectionDomain0();
  //native void setProtectionDomain0(java.security.ProtectionDomain);
  //static native java.lang.Class getPrimitiveClass(java.lang.String);
  //private static native java.lang.reflect.Method getCheckMemberAccessMethod(java.lang.Class<? extends java.lang.SecurityManager>) throws java.lang.NoSuchMethodError;
  //private native java.lang.String getGenericSignature();
  //native byte[] getRawAnnotations();
  //native sun.reflect.ConstantPool getConstantPool();
  //private native java.lang.reflect.Field[] getDeclaredFields0(boolean);
  //private native java.lang.reflect.Method[] getDeclaredMethods0(boolean);
  //private native java.lang.reflect.Constructor<T>[] getDeclaredConstructors0(boolean);
  //private native java.lang.Class<?>[] getDeclaredClasses0();
  //private static native boolean desiredAssertionStatus0(java.lang.Class<?>);

  //table.put(MethodDescription("java.lang.Class", "desiredAssertionStatus", List(), "boolean"), ReturnAtomicSnowflake)

  /*
  table.put(MethodDescription("java.security.AccessController", "checkPermission", List("java.security.Permission"), "void"), NoOpSnowflake)

  table.put(MethodDescription("java.security.AccessController", "getStackAccessControlContext",
    List(), "java.security.AccessControlContext"), NoOpSnowflake)
  table.put(MethodDescription("java.lang.Class", "registerNatives", List(), "void"), NoOpSnowflake)
  table.put(MethodDescription("sun.misc.Unsafe", "registerNatives", List(), "void"), NoOpSnowflake)

  table.put(MethodDescription("java.lang.Double", "doubleToRawLongBits", List("double"), "long"), ReturnAtomicSnowflake)
  table.put(MethodDescription("java.lang.Float", "floatToRawIntBits", List("float"), "int"), ReturnAtomicSnowflake)
  table.put(MethodDescription("java.lang.Class", "isArray", List(), "boolean"), ReturnAtomicSnowflake)
  table.put(MethodDescription("java.lang.Class", "isPrimitive", List(), "boolean"), ReturnAtomicSnowflake)

  table.put(MethodDescription("java.lang.Class", "getPrimitiveClass", List("java.lang.String"), "java.lang.Class"),
    ReturnAtomicSnowflake(D(Set(ObjectValue(Soot.classes.Class, ClassBasePointer("TODO:unknown"))))))
  table.put(MethodDescription("java.lang.Class", "getComponentType", List(), "java.lang.Class"),
    ReturnAtomicSnowflake(D(Set(ObjectValue(Soot.classes.Class, ClassBasePointer("TODO:unknown"))))))
  table.put(MethodDescription("java.security.AccessController", "doPrivileged", List("java.security.PrivilegedAction"), "java.lang.Object"), ReturnObjectSnowflake("java.lang.Object"))
  */

/*
Not needed b/c the only comparator is over String
  table.put(MethodDescription("java.util.Collections", "sort", List("java.util.List", "java.util.Comparator"), "void"),
    new StaticSnowflakeHandler {
      override def apply(state : State, nextStmt : Stmt, args : List[D]) : Set[AbstractState] = {
        () <- System.store(args(0)).values
        val elems =
//Collections.sort(stuffList, this.comparator);

        var states = Set(state.copyState(stmt = nextStmt))

        for (elem1 <- elems) {
          for (elem2 <- elems) {
            states += state.handleInvoke2(Some((args(1), false)), method, List(elem1, elem2), state.alloca(expr, nextStmt), None, nextStmt /*TODO: this is not a real nextStmt; we just need to put something here*/)
          }
        }

        return states
      }
    })
 */

  //private static native void registerNatives();
  /*
  table.put(MethodDescription("java.lang.System", "setIn0", List("java.io.InputStream"), "void"),
    PutStaticSnowflake("java.lang.System", "in"))
  table.put(MethodDescription("java.lang.System", "setOut0", List("java.io.PrintStream"), "void"),
    PutStaticSnowflake("java.lang.System", "out"))
  table.put(MethodDescription("java.lang.System", "setErr0", List("java.io.PrintStream"), "void"),
    PutStaticSnowflake("java.lang.System", "err"))
  table.put(MethodDescription("java.lang.System", "currentTimeMillis", List(), "long"), ReturnAtomicSnowflake)
  table.put(MethodDescription("java.lang.System", "nanoTime", List(), "long"), ReturnAtomicSnowflake)
  //public static native void arraycopy(java.lang.Object, int, java.lang.Object, int, int);
  table.put(MethodDescription("java.lang.System", "identityHashCode", List("java.lang.Object"), "int"), ReturnAtomicSnowflake)
  //private static native java.util.Properties initProperties(java.util.Properties);
  //public static native java.lang.String mapLibraryName(java.lang.String);
  // java.lang.Throwable
  table.put(MethodDescription("java.lang.Throwable", SootMethod.constructorName, List(), "void"), NoOpSnowflake)
  //table.put(MethodDescription("java.lang.Throwable", SootMethod.staticInitializerName, List(), "void"), NoOpSnowflake)
  //private native java.lang.Throwable fillInStackTrace(int);
  table.put(MethodDescription("java.lang.Throwable", "getStackTraceDepth", List(), "int"), ReturnAtomicSnowflake)
  table.put(MethodDescription("java.lang.Throwable", "fillInStackTrace", List("int"), "java.lang.Throwable"), ReturnObjectSnowflake("java.lang.Throwable"))

  //native java.lang.StackTraceElement getStackTraceElement(int);

  // java.util.ArrayList
  //table.put(MethodDescription("java.util.ArrayList", SootMethod.constructorName, List("int"), "void"), NoOpSnowflake)
  */
}
