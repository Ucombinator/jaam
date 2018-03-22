package org.ucombinator.jaam.util

import java.io._
import java.lang.invoke.LambdaMetafactory

import scala.collection.JavaConverters._
import org.jgrapht._
import org.jgrapht.graph._
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.ucombinator.jaam.serializer
import org.ucombinator.jaam.tools.app.{App, Origin, PathElement}
import soot.coffi.Util
import soot.jimple.toolkits.invoke.AccessManager
import soot.{Unit => SootUnit, _}
import soot.jimple.{ClassConstant, DynamicInvokeExpr, Expr, IntConstant, Stmt => SootStmt}
import soot.options.Options

// Helpers for working with Soot.
//
// Note that these methods relate to Soot only and do not include any
// classes or logic for the analyzer


object Soot {
  import scala.language.implicitConversions

  implicit def unitToStmt(unit : SootUnit) : SootStmt = {
    require(unit ne null, "unit is null")
    require(unit.isInstanceOf[SootStmt], "unit not instance of Stmt. Unit is of class: " + unit.getClass)
    unit.asInstanceOf[SootStmt]
  }

  def useJaamClassProvider(): Unit = {
    SourceLocator.v.setClassProviders(List[ClassProvider](new JaamClassProvider).asJava)
  }

  case class ClassData(source: String, origin: Origin, data: Array[Byte])

  var loadedClasses: Map[String, ClassData] = Map.empty

  private def load(p: PathElement) {
    //println(f"p ${p.path} and ${p.root}")
    for (d <- p.classData()) {
      //println(f"d ${d.length}")
      val cr = new ClassReader(new ByteArrayInputStream(d))
      val cn = new ClassNode
      cr.accept(cn, 0)

      // TODO: temporary hack
      val appPackages = List("com/ainfosec/", "com/bbn/", "com/stac/", "com/cyberpointllc/")
      val isAppPackage = appPackages.exists(prefix => cn.name.startsWith(prefix))
      val newOrigin = p.origin match {
        case Origin.APP => if (isAppPackage) Origin.APP else Origin.LIB
        case otherwise => otherwise
      }
      //println(f"cn.name: ${cn.name} ${p.origin} $newOrigin")
      loadedClasses += cn.name.replace('/', '.') -> ClassData("TODO:JaamClassProvider", newOrigin, d)
    }
  }

  def addClasses(app: App): Unit = { app.classpath.foreach(load) }

  // TODO: optional flags to load only some parts?
  def addJaamClasses(file: String): Unit = {
    val stream = new FileInputStream(file)
    val pi = new serializer.PacketInput(stream)

    var packet: serializer.Packet = null
    while ({packet = pi.read(); !packet.isInstanceOf[serializer.EOF]}) {
      // TODO: for (packet <- pi) {
      packet match { case packet: App => addClasses(packet) }
    }
  }

  def getSootClass(s : String): SootClass = Scene.v().loadClass(s, SootClass.SIGNATURES)

  def getBody(m : SootMethod): Body = {
    require(!m.isNative, "Attempt to Soot.getBody on native method: " + m)
    require(!m.isAbstract, "Attempt to Soot.getBody on abstract method: " + m)
    // TODO: do we need to test for phantom here?
    if (!m.hasActiveBody) {
      SootResolver.v().resolveClass(m.getDeclaringClass.getName, SootClass.BODIES)
      m.retrieveActiveBody()
    }
    m.getActiveBody
  }

  def getBodyUnsafe(m: SootMethod): Body = {
    try { getBody(m) }
    catch { case _: Throwable => null }
  }

  def getBodyGraph(m: SootMethod): (Stmt, Graph[Stmt, DefaultEdge]) = {
    val graph = new DirectedPseudograph[Stmt, DefaultEdge](classOf[DefaultEdge])
    var start: Stmt = null

    for (s <- getBody(m).getUnits.asScala) {
      val stmt = Stmt(s, m)
      graph.addVertex(stmt)
      if (stmt.index == 0) { start = stmt }
    }

    for (s <- graph.vertexSet.asScala) {
      Graphs.addOutgoingEdges(graph, s, s.nextSemantic.asJava)
    }

    return (start, graph)
  }

  def initialize(rtJar: String, classpath: String, customizations: => Unit = {}) {
    // We choose to not use Soot in verbose mode.
    Options.v().set_verbose(false)
    // Put class bodies in Jimple format
    Options.v().set_output_format(Options.output_format_jimple)
    // Process all packages and do not exclude packages like java.*
    Options.v().set_include_all(true)
    // we need to link instructions to source line for display
    Options.v().set_keep_line_number(true)
    // Called methods without jar files or source are considered phantom
    Options.v().set_allow_phantom_refs(true)
    // Use the class path from the command line
    Options.v().set_soot_classpath(rtJar + ":" + classpath)
    // Use only the class path from the command line
    Options.v().set_prepend_classpath(false)
    // Take definitions only from class files
    Options.v().set_src_prec(Options.src_prec_only_class)

    // TODO: when should we have this?
    //Options.v.set_whole_program(true)

    customizations // Let the caller specify modifications to settings

    // Compute dependent options
    soot.Main.v().autoSetOptions()

    // Run transformations and analyses according to the configured options.
    // Transformation could include jimple, shimple, and CFG generation
    Scene.v().loadBasicClasses()
    PackManager.v().runPacks()
  }

  def isClass(s: String): Boolean = SourceLocator.v().getClassSource(s) != null

  def getSootType(t : String): Type = t match {
    case "int"    => soot.IntType.v()
    case "bool"   => soot.BooleanType.v()
    case "double" => soot.DoubleType.v()
    case "float"  => soot.FloatType.v()
    case "long"   => soot.LongType.v()
    case "byte"   => soot.ByteType.v()
    case "short"  => soot.ShortType.v()
    case "char"   => soot.CharType.v()
    case _        => soot.RefType.v(t)
  }

  def isPrimitive(t : Type) : Boolean = !t.isInstanceOf[RefLikeType]

  def canStoreType(child: Type, parent: Type): Boolean = {
    Scene.v().getOrMakeFastHierarchy().canStoreType(child, parent)
  }

  def canStoreClass(child: SootClass, parent: SootClass): Boolean = {
    Scene.v().getOrMakeFastHierarchy().canStoreType(child.getType, parent.getType)
  }

  object classes {
    lazy val Object: SootClass              = getSootClass("java.lang.Object")
    lazy val Class: SootClass               = getSootClass("java.lang.Class")
    lazy val String: SootClass              = getSootClass("java.lang.String")
    lazy val Cloneable: SootClass           = getSootClass("java.lang.Cloneable")
    lazy val ClassCastException: SootClass  = getSootClass("java.lang.ClassCastException")
    lazy val ArithmeticException: SootClass = getSootClass("java.lang.ArithmeticException")
    lazy val Serializable: SootClass        = getSootClass("java.io.Serializable")
    lazy val Iterator: SootClass            = getSootClass("java.util.Iterator")
    lazy val LambdaMetafactory: SootClass   = getSootClass("java.lang.invoke.LambdaMetafactory")
  }

  // is a of type b?
  def isSubType(a : Type, b : Type) : Boolean = {
    if (a equals b) true
    else if (isPrimitive(a) || isPrimitive(b)) false
    else
      (a, b) match {
        case (at : ArrayType, bt : ArrayType) =>
          (at.numDimensions == bt.numDimensions) &&
            isSubType(at.baseType, bt.baseType)
        case (ot : Type, _ : ArrayType) =>
          ot.equals(classes.Object.getType) ||
            ot.equals(classes.Cloneable.getType) ||
            ot.equals(classes.Serializable.getType)
        case (_ : ArrayType, ot : Type) =>
          Log.warn(f"Checking if a non-array type $ot is an array")
          false // maybe
        case _ =>
          a.merge(b, Scene.v) match {
            case null => false
            case lub => !lub.equals(a)
          }
      }
  }

  // This function finds all methods that could override root_m.
  // These methods are returned with the root-most at the end of
  // the list and the leaf-most at the head.  Thus the caller
  // should use the head of the returned list.  The reason a list
  // is returned is so this function can recursively compute the
  // transitivity rule in Java's method override definition.
  //
  // Note that Hierarchy.resolveConcreteDispath should be able to do this, but seems to be implemented wrong
  def overrides(curr : SootClass, root_m : SootMethod) : List[SootMethod] = {
    Log.debug("curr: " + curr.toString)
    val curr_m = curr.getMethodUnsafe(root_m.getName, root_m.getParameterTypes, root_m.getReturnType)
    if (curr_m == null) {
      Log.debug("root_m: " + root_m.toString)
      if (curr == Soot.classes.Object) {
        List()
      } else {
        overrides(curr.getSuperclass, root_m)
      }
    }
    else if (root_m.getDeclaringClass.isInterface || AccessManager.isAccessLegal(curr_m, root_m)) { List(curr_m) }
    else {
      val o = overrides(curr.getSuperclass, root_m)
      (if (o.exists(m => AccessManager.isAccessLegal(curr_m, m))) List(curr_m) else List.empty) ++ o
    }
  }

  private lazy val metafactory = classes.LambdaMetafactory.getMethodByName("metafactory")
  private lazy val altMetafactory = classes.LambdaMetafactory.getMethodByName("altMetafactory")

  case class DecodedLambda(interface: SootClass, interfaceMethod: SootMethod, implementationMethod: SootMethod, captures: java.util.List[soot.Value])

  def decodeLambda(e: DynamicInvokeExpr): DecodedLambda = {

    def decodeSimple(): DecodedLambda = {
      // Step 1: Find `interface`, which is the type of the closure returned by the lambda.
      assert(e.getType.isInstanceOf[RefType])
      val interface = e.getType.asInstanceOf[RefType].getSootClass

      // Step 2: Find the method in `interface` that the lambda corresponds to.
      // Unfortunately, this is not already computed so we find it manually.
      // This is complicated by the fact that it may be in a super-class or
      // super-interface of `interface`.
      assert(e.getBootstrapArg(0).isInstanceOf[ClassConstant])
      val bytecodeSignature = e.getBootstrapArg(0).asInstanceOf[ClassConstant].getValue
      val types = Util.v().jimpleTypesOfFieldOrMethodDescriptor(bytecodeSignature)
      val paramTypes = java.util.Arrays.asList[Type](types:_*).subList(0, types.length - 1)
      val returnType = types(types.length - 1)

      def findMethod(klass: SootClass): SootMethod = {
        val m = klass.getMethodUnsafe(e.getMethod.getName, paramTypes, returnType)
        if (m != null) { return m }

        if (klass.hasSuperclass) {
          val m = findMethod(klass.getSuperclass)
          if (m != null) { return m }
        }

        for (i <- klass.getInterfaces.asScala) {
          val m = findMethod(i)
          if (m != null) { return m }
        }

        return null
      }

      val interfaceMethod = findMethod(interface)

      // Step 3: Find `implementationMethod`, which is the method implementing the lambda
      //   Because calling e.getMethodRef.resolve may throw missmatched `static` errors,
      //   we look for the method manually.
      assert(e.getBootstrapArg(1).isInstanceOf[soot.jimple.MethodHandle])
      val implementationMethodRef = e.getBootstrapArg(1).asInstanceOf[soot.jimple.MethodHandle].getMethodRef
      val implementationMethod = implementationMethodRef.declaringClass().getMethod(implementationMethodRef.getSubSignature)

      // Step 4: Find the `captures` which are values that should be saved and passed
      //   to `implementationMethod` before any other arguments
      val captures = e.getArgs

      return DecodedLambda(interface, interfaceMethod, implementationMethod, captures)
    }

    def bootstrapArgIsInt(index: Int, i: Int): Boolean = {
      e.getBootstrapArg(index) match {
        case arg: IntConstant => arg.value == i
        case _ => false
      }
    }

    // Check that this dynamic invoke uses LambdaMetafactory
    val bootstrapMethod = e.getBootstrapMethodRef.resolve
    if (bootstrapMethod == metafactory) {
      assert(e.getBootstrapArgCount == 3)
      return decodeSimple()
    } else if (bootstrapMethod == altMetafactory) {
      e.getBootstrapArg(3) match {
        case flags: IntConstant =>
          val bridges = (flags.value & LambdaMetafactory.FLAG_BRIDGES) != 0
          val markers = (flags.value & LambdaMetafactory.FLAG_MARKERS) != 0
          val isSimple =
            (bridges && !markers && bootstrapArgIsInt(4, 0)) ||
            (!bridges && markers && bootstrapArgIsInt(4, 0)) ||
            (bridges && markers && bootstrapArgIsInt(4, 0) && bootstrapArgIsInt(5, 0))
          if (isSimple) { decodeSimple() }
          else { throw new Exception("Unimplemented altMetafactory: e = " + e) }
        case _ => throw new Exception("Non-int flags passed to altMetafactory: e = " + e)
      }
    } else {
      throw new Exception("Soot.decodeLambda could not decode: e = " + e)
    }
  }
}
