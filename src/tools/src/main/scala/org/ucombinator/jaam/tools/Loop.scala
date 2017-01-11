package org.ucombinator.jaam.tools

import java.util.zip.ZipInputStream
import java.io.FileInputStream

import scala.language.implicitConversions
import scala.collection.JavaConversions._

import soot.options.Options
import soot.tagkit.{GenericAttribute, SourceFileTag}
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}
import soot.jimple.toolkits.callgraph._

import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.toolkits.graph.LoopNestTree;

import Console._

// TODO: merge with jaam.interpreter.Soot
object Soot {
  def initSoot(mainClassName: String, sootClassPath: String) {
    Options.v().set_verbose(false)
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_keep_line_number(true)
    Options.v().set_allow_phantom_refs(true)
    Options.v().set_soot_classpath(sootClassPath)
    Options.v().set_include_all(true)
    Options.v().set_prepend_classpath(false)
    Options.v().set_src_prec(Options.src_prec_only_class)
    Options.v().set_whole_program(true)
    Options.v().set_app(true)
    soot.Main.v().autoSetOptions()
    
    val classesInJar = getAllClassNames(sootClassPath)
    if (classesInJar.nonEmpty) {
      for (className <- classesInJar) {
        Scene.v().addBasicClass(className, SootClass.HIERARCHY)
      }
    } else {
      Options.v().set_main_class(mainClassName)
      val mainClass = Scene.v().loadClassAndSupport(mainClassName)
      Scene.v().setMainClass(mainClass)
    }

    Scene.v().setSootClassPath(sootClassPath)
    Scene.v().loadNecessaryClasses()

    PackManager.v().runPacks()

    CHATransformer.v().transform()
    val cg = Scene.v().getCallGraph()
  }
  
  // Only works on jar files
  def getAllClassNames(classPath: String): List[String] = {
    def getClassNames(jarFile: String): List[String] = {
      //println(s"get all class names from ${jarFile}")
      val zip = new ZipInputStream(new FileInputStream(jarFile))
      var ans: List[String] = List()
      var entry = zip.getNextEntry
      while (entry != null) {
        if (!entry.isDirectory && entry.getName().endsWith(".class")) {
          val className = entry.getName().replace('/', '.')
          ans = (className.substring(0, className.length() - ".class".length()))::ans
        }
        entry = zip.getNextEntry
      }
      ans
    }
    val classPaths = classPath.split(":").toList
    (for (path <- classPaths if path.endsWith("jar")) yield getClassNames(path)).flatten
  }

  def getSootClass(s: String) = Scene.v().loadClass(s, SootClass.SIGNATURES)

  def getBody(m: SootMethod): Body = {
    if (m.isNative || m.isAbstract) 
      throw new Exception("method body not exists")

    if (!m.hasActiveBody) {
      SootResolver.v().resolveClass(m.getDeclaringClass.getName, SootClass.BODIES)
      m.retrieveActiveBody
    }
    m.getActiveBody
  }

  implicit def unitToStmt(unit : SootUnit) : SootStmt = {
    assert(unit ne null, "unit is null")
    assert(unit.isInstanceOf[SootStmt], "unit not instance of Stmt. Unit is of class: " + unit.getClass)
    unit.asInstanceOf[SootStmt]
  }

  def getMethodEntry(m: SootMethod): SootStmt = getBody(m).getUnits.getFirst

  def prevSyntactic(stmt: SootStmt, method: SootMethod): Option[SootStmt] = {
    val prev = getBody(method).getUnits.getPredOf(stmt)
    if (prev == null) None else Some(prev)
  }
  
  def nextSyntactic(stmt: SootStmt, method: SootMethod): Option[SootStmt] = {
    val next = getBody(method).getUnits.getSuccOf(stmt) 
    if (next == null) None else Some(next)
  }

  def nextSemantic(stmt: SootStmt, method: SootMethod): List[SootStmt] = {
    stmt match {
      case stmt: ReturnStmt => List()
      case stmt: ReturnVoidStmt => List()
      case stmt: ThrowStmt => List()
      case stmt: GotoStmt => List(stmt.getTarget)
      case stmt: SwitchStmt => stmt.getTargets.toList.map(u => u.asInstanceOf[SootStmt])
      case stmt: IfStmt => 
        nextSyntactic(stmt, method) match {
          case Some(next) => List(stmt.getTarget, next)
          case None => List(stmt.getTarget)
        }
      case _ => 
        nextSyntactic(stmt, method) match {
          case Some(next) => List(next)
          case None => List()
        }
    }
  }
  
  val indexTag = "org.ucombinator.jaam.Stmt.indexTag"
  def getIndex(sootStmt: SootStmt, sootMethod: SootMethod): Int = {
    if (sootStmt.hasTag(indexTag)) {
      BigInt(sootStmt.getTag(indexTag).getValue).intValue
    } else {
      // label everything in the sootMethod so the amortized work is linear
      for ((u, i) <- Soot.getBody(sootMethod).getUnits().toList.zipWithIndex) {
        u.addTag(new GenericAttribute(indexTag, BigInt(i).toByteArray))
      }

      assert(sootStmt.hasTag(indexTag), "SootStmt "+sootStmt+" not found in SootMethod " + sootMethod)
      BigInt(sootStmt.getTag(indexTag).getValue).intValue
    }
  }
}

object LoopDepthCounter {
  def main(mainClassName: String, mainMethodName: String, classPaths: Seq[String]) {
    val sootClassPath = classPaths.toList.mkString(":")
    println(s"main class: ${mainClassName}")
    println(s"main method: ${mainMethodName}")
    println(s"jar file names: ${sootClassPath}")

    Soot.initSoot(mainClassName, sootClassPath)
    val mainMethod = Soot.getSootClass(mainClassName).getMethodByName(mainMethodName)
    findLoopsInMethod(mainMethod)
  }

  case class Loop(method: SootMethod, start: SootStmt, end: SootStmt, depth: Int, parent: Option[Loop])

  abstract class Stack {
    def exists(m: SootMethod): Boolean
  }
  case object Halt extends Stack {
    def exists(m: SootMethod) = false
  }
  case class CallStack(currentMethod: SootMethod, allLoops: List[SootLoop], stack: Stack, nLoop: Int = 0) extends Stack {
    def incLoop: CallStack = CallStack(currentMethod, allLoops, stack, nLoop+1)
    def decLoop: CallStack = CallStack(currentMethod, allLoops, stack, nLoop-1)
    def exists(m: SootMethod): Boolean = (m == currentMethod) || (stack.exists(m))
    override def toString(): String =  {
      def methodName(m: SootMethod, nLoop: Int) = {
        val name = m.getDeclaringClass.getName + "." + m.getName
        "  " + (if (nLoop > 0) s"$RED$name($nLoop)$RESET" else name)
      }
      def aux(stack: Stack): List[String] = {
        stack match {
          case Halt => List()
          case CallStack(m, al, s, loop) =>  methodName(m, loop) :: aux(s)
        }
      }
      ((methodName(currentMethod, nLoop)::aux(stack)).reverse).mkString("\n")
    }
  }

  def getDispatchedMethods(invokeExpr: InvokeExpr): List[SootMethod] = {
    val hierarchy = Scene.v().getOrMakeFastHierarchy()
    val targetMethod = invokeExpr.getMethod
    val realTargetMethods: List[SootMethod] = invokeExpr match {
      case dynInvoke: DynamicInvokeExpr => throw new Exception(s"Unexpected DynamicInvokeExpr: $dynInvoke")
      case staticInvoke: StaticInvokeExpr => List(targetMethod)
      case specInvoke: SpecialInvokeExpr => List(hierarchy.resolveSpecialDispatch(specInvoke, targetMethod))
      case insInvoke: InstanceInvokeExpr => 
        assert(insInvoke.getBase.getType.isInstanceOf[RefLikeType], "Base is not a RefLikeType")
        insInvoke.getBase.getType match {
          case rt: RefType =>
            val baseClass = insInvoke.getBase.getType.asInstanceOf[RefType].getSootClass
            //println(s"baseClass: ${baseClass}")
            hierarchy.resolveAbstractDispatch(baseClass, targetMethod).toList
          case _ => List() // invocation on array/primitive type
        }
    }
    //println(s"dispatched methods for ${targetMethod.getName}: ${realTargetMethods}")
    realTargetMethods
  }

  def getDispatchedMethodsCHA(stmt: SootStmt): List[SootMethod] = {
    val cg = Scene.v().getCallGraph()
    val edges = cg.edgesOutOf(stmt).toList
    val methods = edges.map((e: Edge) => e.tgt)
    //println(s"dispatched methods according to CHA: ${methods}")
    methods
  }

  def handleInvoke(stmt: SootStmt, invokeExpr: InvokeExpr, currentLoop: Option[Loop], stack: CallStack) {
    val realTargetMethods = getDispatchedMethods(invokeExpr)
    val realTargetMethodsCHA = getDispatchedMethodsCHA(stmt)
    
    for (m <- realTargetMethods) {
      if (m.isPhantom) { /*println(s"Warning: phantom method ${m}, will not analyze")*/ }
      else if (m.isAbstract) { /*println(s"Warning: abstract method ${m}, will not analyze")*/ }
      else if (m.isNative) { /*println(s"Warning: native method ${m}, will not analyze")*/ }
      else if (stack.exists(m)) { println(s"Recursive call on ${m}") }
      else { 
        val allLoops = (new LoopNestTree(Soot.getBody(m))).toList
        findLoopsInMethod(Some(Soot.getMethodEntry(m)), m, currentLoop, CallStack(m, allLoops, stack)) 
      }
    }
  }

  def findLoopsInMethod(method: SootMethod) {
    val entry = Soot.getMethodEntry(method)
    val allLoops = (new LoopNestTree(Soot.getBody(method))).toList
    findLoopsInMethod(Some(entry), method, None, CallStack(method, allLoops, Halt))
  }

  def findLoopsInMethod(stmt: Option[SootStmt], method: SootMethod, currentLoop: Option[Loop], stack: CallStack) {
    def isBefore(s1: SootStmt, s2: SootStmt): Boolean = {
      Soot.getIndex(s1, method) < Soot.getIndex(s2, method)     
    }

    if (stmt.nonEmpty) {
      val realStmt = stmt.get
      println(s"${method.getName}[${realStmt.getJavaSourceStartLineNumber}]\t${realStmt}")

      realStmt match {
        case ifStmt: IfStmt =>
          stack.allLoops.find((l: SootLoop) => l.getHead == ifStmt) match {
            case Some(l) => 
              val depth = if (currentLoop.nonEmpty) currentLoop.get.depth+1 else 1
              val newLoop = Loop(method, ifStmt, l.getBackJumpStmt, depth, currentLoop)
              val newStack = stack.incLoop
              println(s"Found a loop in ${method.getDeclaringClass.getName}.${method.getName} " + 
                      s"[${ifStmt.getJavaSourceStartLineNumber}, ${l.getBackJumpStmt.getJavaSourceStartLineNumber}], " + 
                      s"depth: ${depth}")
              println(newStack)
              findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, Some(newLoop), newStack)
            case None => findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, currentLoop, stack)
          }

        case gotoStmt: GotoStmt if (currentLoop.nonEmpty && currentLoop.get.end == gotoStmt) =>
          findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, currentLoop.get.parent, stack.decLoop)
        
        case invokeStmt: InvokeStmt =>
          handleInvoke(invokeStmt, invokeStmt.getInvokeExpr, currentLoop, stack)
          findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, currentLoop, stack)

        case defStmt: DefinitionStmt =>
          defStmt.getRightOp match {
            case invokeExpr: InvokeExpr => 
              handleInvoke(defStmt, invokeExpr, currentLoop, stack)
              findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, currentLoop, stack)
            case _ => findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, currentLoop, stack)
          }

        case _ => findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, currentLoop, stack)
      }
    }
  }
}
