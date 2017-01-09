package org.ucombinator.jaam.tools

import scala.language.implicitConversions
import scala.collection.JavaConversions._

import soot.options.Options
import soot.tagkit.{GenericAttribute, SourceFileTag}
import soot.{Main => SootMain, Unit => SootUnit, Value => SootValue, _}
import soot.jimple.{Stmt => SootStmt, _}
import soot.jimple.toolkits.callgraph._

// TODO: merge with jaam.interpreter.Soot
object Soot {
  def initSoot(mainClassName: String, sootClassPath: String) {
    Options.v().set_verbose(false)
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_include_all(false)
    Options.v().set_keep_line_number(true)
    Options.v().set_allow_phantom_refs(true)
    Options.v().set_soot_classpath(sootClassPath)
    Options.v().set_prepend_classpath(false)
    Options.v().set_src_prec(Options.src_prec_only_class)
    Options.v().set_app(true)
    Options.v().set_whole_program(true)
    soot.Main.v().autoSetOptions()
    val mainClass = Scene.v().loadClassAndSupport(mainClassName)
    Scene.v().setMainClass(mainClass)
    Scene.v().loadNecessaryClasses()
    CHATransformer.v().transform()
    val cg = Scene.v().getCallGraph()
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

  def handleInvoke(invokeExpr: InvokeExpr, currentLoop: Option[Loop]) {
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
            hierarchy.resolveAbstractDispatch(baseClass, targetMethod).toList
          case _ => List() // invocation on array/primitive type
        }
    }
    println(s"dispatched methods for ${targetMethod.getName}: ${realTargetMethods}")

    for (m <- realTargetMethods) {
      if (m.isPhantom) { println(s"Warning: phantom method ${m}, will not analyze") }
      else if (m.isAbstract) { println(s"Warning: abstract method ${m}, will not analyze") }
      else if (m.isNative) { println(s"Warning: native method ${m}, will not analyze") }
      else { findLoopsInMethod(Some(Soot.getMethodEntry(m)), m, currentLoop) }
    }
  }

  def findLoopsInMethod(method: SootMethod) {
    val entry = Soot.getMethodEntry(method)
    findLoopsInMethod(Some(entry), method, None)
  }

  def findLoopsInMethod(stmt: Option[SootStmt], method: SootMethod, currentLoop: Option[Loop]) {
    if (stmt.nonEmpty) {
      val realStmt = stmt.get
      //println(s"${method.getName}[${realStmt.getJavaSourceStartLineNumber}]\t${realStmt}")

      realStmt match {
        case ifStmt: IfStmt =>
          val target = ifStmt.getTarget
          val predOfTarget = Soot.prevSyntactic(target, method)
          predOfTarget match {
            case Some(gotoStmt: GotoStmt) if gotoStmt.getTarget == ifStmt =>
              val depth = if (currentLoop.nonEmpty) currentLoop.get.depth+1 else 1
              println(s"loop in ${method.getDeclaringClass.getName}.${method.getName} " + 
                      s"[${ifStmt.getJavaSourceStartLineNumber}, ${gotoStmt.getJavaSourceStartLineNumber}], " + 
                      s"depth: ${depth}")
              findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, 
                Some(Loop(method, ifStmt, gotoStmt, depth, currentLoop)))
            case _ => findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, currentLoop)
          }

        case gotoStmt: GotoStmt if (currentLoop.nonEmpty && currentLoop.get.end == gotoStmt) =>
          findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, currentLoop.get.parent)

        case invokeStmt: InvokeStmt =>
          //TODO handle recursive invokeExprs
          handleInvoke(invokeStmt.getInvokeExpr, currentLoop)
          findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, currentLoop)

        case defStmt: DefinitionStmt =>
          defStmt.getRightOp match {
            case invokeExpr: InvokeExpr => 
              handleInvoke(invokeExpr, currentLoop)
              findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, currentLoop)
            case _ => findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, currentLoop)
          }

        case _ => findLoopsInMethod(Soot.nextSyntactic(realStmt, method), method, currentLoop)
      }
    }
  }
}
