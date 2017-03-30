package org.ucombinator.jaam.tools

import java.util.zip.ZipInputStream
import java.io.{BufferedOutputStream, FileInputStream, FileOutputStream}

import scala.language.implicitConversions
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import soot.options.Options
import soot.tagkit.{GenericAttribute, SourceFileTag}
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}
import soot.jimple.toolkits.callgraph._
import soot.jimple.Jimple
import soot.jimple.toolkits.annotation.logic.{Loop => SootLoop}
import soot.toolkits.graph.Block
import soot.toolkits.graph.BlockGraph
import soot.toolkits.graph.ExceptionalBlockGraph
import soot.toolkits.graph.LoopNestTree

import Console._
import collection.mutable.ListBuffer

class LoopDepthCounter extends Main("loop") {
  banner("Analyze the number of depth of each loop in the application code")
  footer("")

  val loop = opt[Boolean](descr = "Run loop detection")
  val rec = opt[Boolean](descr = "Run recursion detection")
  val alloc = opt[Boolean](descr = "Run allocation detection")
  val nocolor = opt[Boolean](descr = "No coloring option if you want to redirect the output to some file or text editor",
                             default = Some(false))
  var remove_duplicates = opt[Boolean](name = "remove-duplicates", descr = "Only output deepest loop, may lose suspicious loops", default = Some(false))

  val mainClass = trailArg[String](descr = "The name of the main class")
  val mainMethod = trailArg[String](descr = "The name of entrance method")
  val jars = trailArg[String](descr = "Colon separated list of application's JAR files, not includes library")

  def run(conf: Conf) {
    val all = !(loop() || rec() || alloc())
    var color = !nocolor()
    LoopDepthCounter.main(mainClass(), mainMethod(), jars().split(":"), 
                          PrintOption(all, loop(), rec(), alloc(), color, remove_duplicates()))
  }
}


// TODO duplicate

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

case class PrintOption(all: Boolean, loop: Boolean, rec: Boolean, alloc: Boolean, color: Boolean, rmDup: Boolean, graph: Boolean)

object LoopDepthCounter {
  var opt: PrintOption = null

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
    def exists(m: SootMethod): Boolean
    def >= (that: Stack): Boolean
  }
  case object Halt extends Stack {
    def exists(m: SootMethod): Boolean = false
    def >= (that: Stack): Boolean = {
      that match {
        case Halt => true
        case _ => false
      }
    }
  }
  case class CallStack(expr: Expr, currentMethod: SootMethod, allLoops: List[SootLoop], stack: Stack, nLoop: Int = 0) extends Stack {
    def incLoop: CallStack = CallStack(expr, currentMethod, allLoops, stack, nLoop+1)
    def decLoop(n: Int): CallStack = CallStack(expr, currentMethod, allLoops, stack, nLoop-n)
    def exists(m: SootMethod): Boolean = ((m == currentMethod) || stack.exists(m))
    def >= (that: Stack): Boolean = {
      def len(s: Stack): Int = {
        s match {
          case Halt => 0
          case CallStack(_, _, _, stack, _) => 1 + len(stack)
        }
      }
      that match {
        case Halt => true
        case that: CallStack =>
          val thisLen = len(this)
          val thatLen = len(that)
          if (thisLen < thatLen) return false
          if (thisLen == thatLen) return stackEqual(this, that)
          this.stack >= that
      }
    }

    /**
      *
      * @param m  method
      * @param nLoop  loop depth
      * @param printing  indicates whether called from toString or toList (for coloring)
      * @return
      */
    def methodName(m: SootMethod, nLoop: Int, printing: Boolean): String = {
      val mName = Soot.methodFullName(m)
      "  " + (if (nLoop > 0 && opt.color && printing) s"$RED$mName($nLoop)$RESET"
      else if (nLoop > 0) s"$mName($nLoop)"
      else mName)
    }

    def aux(stack: Stack, printing: Boolean): List[String] = {
      stack match {
        case Halt => List()
        case CallStack(e, m, al, s, loop) =>  methodName(m, loop, printing) :: aux(s, printing)
      }
    }

    def toList(): List[String] = {
      methodName(currentMethod, nLoop, false)::aux(stack, false).reverse
    }

    override def toString(): String = {
      ((methodName(currentMethod, nLoop, true)::aux(stack, true)).reverse).mkString("\n")+"\n"
    }
    
    def toStringAndHighlightMethod(recMethod: SootMethod): String = {
      def methodName(m: SootMethod, nLoop: Int): String = {
        val mName = Soot.methodFullName(m)
        val nameLoop = if (nLoop > 0) s"$mName($nLoop)" else mName
        val nameColor = if (opt.color) s"$GREEN$nameLoop$RESET" else nameLoop
        "  " + (if (m == recMethod) nameColor else nameLoop)
      }
      def aux(stack: Stack, metEndOfRec: Boolean): List[String] = {
        stack match {
          case Halt => List()
          case CallStack(e, m, al, s, loop) => 
            if (m == recMethod && !metEndOfRec) (methodName(m, loop) + " ⇐ recursion ends")::aux(s, true)
            else if (m == recMethod) (methodName(m, loop) + " ⇐ recursion begins")::aux(s, metEndOfRec)
            else methodName(m, loop)::(aux(s, metEndOfRec))
        }
      }
      aux(this, false).reverse.mkString("\n")+"\n"
    }
  }
  def stackEqual(s1: Stack, s2: Stack): Boolean = {
    (s1, s2) match {
      case (Halt, Halt) => true
      case (s1: CallStack, s2: CallStack) =>
        if (s1.expr == s2.expr && s1.currentMethod == s2.currentMethod)
          stackEqual(s1.stack, s2.stack)
        else false
      case (_, _) => false
    }
  }

  abstract class Result
  case class LoopResult(method: SootMethod, depth: Int, loc: Int, stack: CallStack) extends Result {
    override def toString(): String = {
      s"Found a loop in ${method.getDeclaringClass.getName}.${method.getName}, " + 
      s"starts from line [$loc], " +
      s"depth: ${depth} \n" + 
      stack
    }
  }
  case class RecResult(method: SootMethod, stack: CallStack) extends Result {
    override def toString(): String = {
      s"Found recursive calls on ${Soot.methodFullName(method)}, skip.\n" +
      stack.toStringAndHighlightMethod(method)
    }
  }
  case class AllocResult(method: SootMethod, expr: SootValue, depth: Int, stack: CallStack) extends Result {
    override def toString(): String = {
      val e = if (opt.color) s"$CYAN$expr$RESET" else s"$expr"
      s"Found object allocation in ${method.getDeclaringClass.getName}.${method.getName}, " + 
      s"depth ${depth}: $e \n" + 
      stack
    }
  }

  def removeDuplicates(s: List[Result], res: List[Result]): List[Result] = {
    def notKeep(x: Result, xs: List[Result]): Boolean = {
      xs.exists((y: Result) => {
        // TODO refactor
        if (y.isInstanceOf[LoopResult]) {
          val y1 = y.asInstanceOf[LoopResult]
          val x1 = x.asInstanceOf[LoopResult]
          (stackEqual(y1.stack, x1.stack) && y1.depth > x1.depth) || (y1.stack >= x1.stack)
        }
        else if (y.isInstanceOf[AllocResult]) {
          val y1 = y.asInstanceOf[AllocResult]
          val x1 = x.asInstanceOf[AllocResult]
          (stackEqual(y1.stack, x1.stack) && y1.depth > x1.depth) || (y1.stack >= x1.stack)
        }
        else false
      })
    }
    s match {
      case x::xs => if (notKeep(x, res++xs)) { removeDuplicates(xs, res) }
                    else { removeDuplicates(xs, x::res) }
      case Nil => res
    }
  }

  // Using set to remove equivalent items
  var loopResults: ListBuffer[LoopResult] = ListBuffer()
  val recResults: ListBuffer[RecResult] = ListBuffer()
  val allocResults: ListBuffer[AllocResult] = ListBuffer()

  def main(mainClassName: String, mainMethodName: String, classPaths: Seq[String], runOpt: PrintOption) {
    opt = runOpt
    val sootClassPath = classPaths.toList.mkString(":")
    println(s"main class: ${mainClassName}")
    println(s"main method: ${mainMethodName}")
    println(s"jar file names: ${sootClassPath}")

    Soot.initSoot(mainClassName, sootClassPath)
    val mainMethod = Soot.getSootClass(mainClassName).getMethodByName(mainMethodName)
    findLoopsInMethod(mainMethod)
    
    if (opt.all || opt.loop) {
      if (opt.rmDup) {
        removeDups(loopResults.toList)
      }
      loopResults = loopResults.sortBy(_.depth)
      loopResults.foreach(println)
      if (opt.graph) {
        writeToGraph(loopResults.toList)
      }
    }
    if (opt.all || opt.rec)   { 
      recResults.foreach(println) 
    }
    if (opt.all || opt.alloc) { 
      if (opt.rmDup) {
        val reduced = removeDuplicates(allocResults.toList, List())
        allocResults.clear
        reduced.foreach(allocResults += _.asInstanceOf[AllocResult]) // TODO use removeDups
      }
      allocResults.sortBy(_.depth).foreach(println)
    }
    
    println("Summary:")
    if (opt.all || opt.loop)  { println(s"  number of loops: ${loopResults.size}") }
    if (opt.all || opt.rec)   { println(s"  number of recursions: ${recResults.size}") }
    if (opt.all || opt.alloc) { println(s"  number of object allocations: ${allocResults.size}") }
  }

  /***
    * Produce list of loops with duplicates removed
    * @param loops
    */
  def removeDups(loops: List[LoopDepthCounter.Result]) = {
    val reduced = removeDuplicates(loops, List())
    loopResults.clear
    for (x <- reduced) { loopResults += x.asInstanceOf[LoopResult] }
    loops
  }

  /***
    * Write a GraphViz file from the loops.
    * @param loops
    */
  def writeToGraph(loops: List[LoopResult]) = {
    val out = new FileOutputStream("out.gv")
    out.write("strict digraph G {\n".getBytes())
    var counter = 1
      for (loop <- loops) {
        val subgraphbegin = s"subgraph $counter {" + "\n"
        out.write(subgraphbegin.getBytes())

        val list = loop.stack.toList()
        val loophead = list.head
        val loopheadNode = "\"" + s"$loophead$counter" + "\"" + "[label=\"" + s"$loophead" + "\"" + "];\n"
        out.write(loopheadNode.getBytes())

        // write the labels for nodes
        for (item <- list.drop(1)) {
          val nodeString = "\"" + s"$item$counter" + "\"[label = \"" + s"$item" + "\"];\n"
          out.write(nodeString.getBytes());
        }

        // write node relations
        for (item <- list.drop(1)) {
          val relationString = "\"" + s"$item$counter" + "\" -> \n"
          out.write(relationString.getBytes());
        }
        val count = "\"" + s"$loophead$counter" + "\"\n"
        out.write(count.getBytes())

        out.write("}\n".getBytes()) // end subgraph

        counter += 1
      }
    out.write("}\n".getBytes()) // end outer graph
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
        case true => 
          recResults += RecResult(m, CallStack(null/*?*/, m, List()/*just need a list here*/, stack))
        case false =>
          if (m.isPhantom) { /*println(s"Warning: phantom method ${m}, will not analyze")*/ }
          else if (m.isAbstract) { /*println(s"Warning: abstract method ${m}, will not analyze")*/ }
          else if (m.isNative) { /*println(s"Warning: native method ${m}, will not analyze")*/ }
          else { 
            val allLoops = (new LoopNestTree(Soot.getBody(m))).toList
            findLoopsInMethod(Some(Soot.getMethodEntry(m)), currentLoop, CallStack(invokeExpr, m, allLoops, stack)) 
          }
      }
    }
  }

  def findLoopsInMethod(method: SootMethod) {
    val entry = Soot.getMethodEntry(method)
    val allLoops = (new LoopNestTree(Soot.getBody(method))).toList
    findLoopsInMethod(Some(entry), None, CallStack(Jimple.v.newStaticInvokeExpr(method.makeRef), method, allLoops, Halt))
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
            case newExpr @ (_:NewExpr | _:NewArrayExpr | _:NewMultiArrayExpr) if (currentLoop.nonEmpty) =>
              allocResults += AllocResult(stack.currentMethod, newExpr, currentLoop.get.depth, stack)
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

          loopResults += LoopResult(stack.currentMethod, depth, realStmt.getJavaSourceStartLineNumber, newStack)
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
