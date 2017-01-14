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

/* TODO Failure apps

blogger
Failed to apply jb to <stac.example.Next: fi.iki.elonen.JavaPluginResponse render(fi.iki.elonen.NanoHTTPD$IHTTPSession)>
./bin/jaam-tools loop fi.iki.elonen.JavaWebServer main /Users/kraks/study/utah/STAC/Engagement1/Challenge_Programs/blogger/challenge_program/nanohttpd-javawebserver-2.2.0-SNAPSHOT-jar-with-dependencies.jar

graph_analyzer, not terminates
./bin/jaam-tools loop user.commands.CommandProcessor main /Users/kraks/study/utah/STAC/Engagement1/Challenge_Programs/graph_analyzer/challenge_program/GraphDisplay.jar

subspace
Failed to apply jb to <org.apache.commons.math3.util.BigReal: org.apache.commons.math3.util.BigReal divide(org.apache.commons.math3.util.BigReal)>
./bin/jaam-tools loop com.example.subspace.Main main /Users/kraks/study/utah/STAC/Engagement1/Challenge_Programs/subspace/challenge_program/lib/Subspace-1.0.jar
*/

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

  def nextSyntactic(stmt: SootStmt, method: SootMethod): Option[SootStmt] = {
    val next = getBody(method).getUnits.getSuccOf(stmt) 
    if (next == null) None else Some(next)
  }

  def methodFullName(m: SootMethod): String = m.getDeclaringClass.getName + "." + m.getName
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

  case class Loop(method: SootMethod, start: SootStmt, end: SootStmt, depth: Int, parent: Option[Loop], offset: Int = 0) {
    def isEndStmt(stmt: SootStmt): Boolean = end == stmt
    //TODO refactor offset
    def getOffsetOfFutureEndStmt(stmt: SootStmt): Option[Int] = {
      def find(stmt: SootStmt, n: Int, loop: Option[Loop]): Option[Int] = {
        loop match {
          case None => None
          case Some(l) => if (l.end == stmt) Some(1+n) else find(stmt, 1+n, l.parent)
        }
      }
      find(stmt, 0, parent)
    }
    def setOffset(n: Int): Loop = Loop(method, start, end, depth, parent, n)
    def getParent(n: Int = 0): Option[Loop] = {
      if (n == 0) parent else parent.get.getParent(n-1)
    }
  }

  abstract class Stack {
    def exists(m: SootMethod): Option[Stack]
  }
  case object Halt extends Stack {
    def exists(m: SootMethod): Option[Stack] = None
  }
  case class CallStack(currentMethod: SootMethod, allLoops: List[SootLoop], stack: Stack, nLoop: Int = 0) extends Stack {
    def incLoop: CallStack = CallStack(currentMethod, allLoops, stack, nLoop+1)
    def decLoop(n: Int): CallStack = CallStack(currentMethod, allLoops, stack, nLoop-n)
    def exists(m: SootMethod): Option[Stack] = {
      if ((m == currentMethod) || (stack.exists(m).isInstanceOf[Some[Stack]])) Some(stack)
      else None
    }
    override def toString(): String =  {
      def methodName(m: SootMethod, nLoop: Int) = {
        val name = Soot.methodFullName(m)
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
    
    def toStringAndHighlightMethod(recMethod: SootMethod): String = {
      def methodName(m: SootMethod, nLoop: Int) = {
        val name = Soot.methodFullName(m)
        val nameLoop = "  " + (if (nLoop > 0) s"$name($nLoop)" else name)
        if (m == recMethod) s"$GREEN$nameLoop$RESET"  else nameLoop
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
      stack.exists(m) match {
        case Some(s) => 
          println(s"Found recursive calls on ${Soot.methodFullName(m)}, skip.") 
          println(CallStack(m, List()/*just need a list here*/, stack).toStringAndHighlightMethod(m))
        case None =>
          if (m.isPhantom) { /*println(s"Warning: phantom method ${m}, will not analyze")*/ }
          else if (m.isAbstract) { /*println(s"Warning: abstract method ${m}, will not analyze")*/ }
          else if (m.isNative) { /*println(s"Warning: native method ${m}, will not analyze")*/ }
          else { 
            val allLoops = (new LoopNestTree(Soot.getBody(m))).toList
            findLoopsInMethod(Some(Soot.getMethodEntry(m)), currentLoop, CallStack(m, allLoops, stack)) 
          }
      }
    }
  }

  def findLoopsInMethod(method: SootMethod) {
    val entry = Soot.getMethodEntry(method)
    val allLoops = (new LoopNestTree(Soot.getBody(method))).toList
    findLoopsInMethod(Some(entry), None, CallStack(method, allLoops, Halt))
  }

  def findLoopsInMethod(stmt: Option[SootStmt], currentLoop: Option[Loop], stack: CallStack) {
    if (stmt.nonEmpty) {
      val realStmt = stmt.get
      val nextStmt = Soot.nextSyntactic(realStmt, stack.currentMethod)
      //println(s"${stack.currentMethod.getName}[${realStmt.getJavaSourceStartLineNumber}]\t${realStmt}")
      
      realStmt match {
        case invokeStmt: InvokeStmt =>
          handleInvoke(invokeStmt, invokeStmt.getInvokeExpr, currentLoop, stack)
        case defStmt: DefinitionStmt =>
          defStmt.getRightOp match {
            case invokeExpr: InvokeExpr => handleInvoke(defStmt, invokeExpr, currentLoop, stack)
            case _ => //Do nothing
          }
        case _ => //Do nothing
      }

      stack.allLoops.find((l: SootLoop) => l.getHead == realStmt) match {
        case Some(l) =>
          val depth = if (currentLoop.nonEmpty) currentLoop.get.depth+1 else 1
          val offset = if (currentLoop.nonEmpty && currentLoop.get.isEndStmt(realStmt)) {
            //TODO need more test on this very corner case
            //println("realStmt is an end of loop and a start of another loop")
            currentLoop.get.offset+1
          } else { 0 }
          val newLoop = Loop(stack.currentMethod, realStmt, l.getBackJumpStmt, depth, currentLoop, offset)
          val newStack = stack.incLoop
          println(s"Found a loop in ${stack.currentMethod.getDeclaringClass.getName}.${stack.currentMethod.getName}, " + 
                  s"starts from line [${realStmt.getJavaSourceStartLineNumber}], " + 
                  s"depth: ${depth}")
          println(newStack)
          findLoopsInMethod(nextStmt, Some(newLoop), newStack)

        case None if (currentLoop.nonEmpty && currentLoop.get.isEndStmt(realStmt)) =>
          val n = currentLoop.get.offset
          findLoopsInMethod(nextStmt, currentLoop.get.getParent(n), stack.decLoop(n+1))

        case None if (currentLoop.nonEmpty) =>
          currentLoop.get.getOffsetOfFutureEndStmt(realStmt) match {
            case Some(n) =>
              //println(s"Find a future loop end, offset: ${n}")
              findLoopsInMethod(nextStmt, Some(currentLoop.get.setOffset(n)), stack) 
            case None => findLoopsInMethod(nextStmt, currentLoop, stack)
          }

        case _ =>
          findLoopsInMethod(nextStmt, currentLoop, stack)
      }
    }
  }
}