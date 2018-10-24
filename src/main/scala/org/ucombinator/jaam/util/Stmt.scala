package org.ucombinator.jaam.util

import scala.collection.JavaConverters._

import org.ucombinator.jaam.serializer
import soot.{SootMethod, Unit => SootUnit}
import soot.jimple.{Stmt => SootStmt, _}
import soot.tagkit.{GenericAttribute, SourceFileTag}

import org.ucombinator.jaam.util.Soot.unitToStmt


object Stmt {
  val indexTag = "org.ucombinator.jaam.Stmt.indexTag"

  def getIndex(sootStmt: SootStmt, sootMethod: SootMethod) : Int = {
    if (!sootStmt.hasTag(Stmt.indexTag)) {
      // label everything in the sootMethod so the amortized work is linear
      for ((u, i) <- Soot.getBody(sootMethod).getUnits.asScala.toList.zipWithIndex) {
        u.addTag(new GenericAttribute(Stmt.indexTag, BigInt(i).toByteArray))
      }
      assert(sootStmt.hasTag(Stmt.indexTag), "SootStmt "+sootStmt+" not found in SootMethod " + sootMethod)
    }

    BigInt(sootStmt.getTag(Stmt.indexTag).getValue).intValue
  }

  def methodEntry(sootMethod: SootMethod) = Stmt(Soot.getBody(sootMethod).getUnits.getFirst,sootMethod)
}


case class Stmt(sootStmt : SootStmt, sootMethod : SootMethod) extends CachedHashCode {
  val index: Int = Stmt.getIndex(sootStmt, sootMethod)
  val line: Int = sootStmt.getJavaSourceStartLineNumber
  val column: Int = sootStmt.getJavaSourceStartColumnNumber

  val sourceFile: String =
    sootMethod.getDeclaringClass.getTag("SourceFileTag") match {
      case null => "<unknown>"
      case tag =>
        val sourceTag = tag.asInstanceOf[SourceFileTag]
        val absolutePath =
          sourceTag.getAbsolutePath match {
            case null => ""
            case absPath => absPath + "/"
          }
        absolutePath + sourceTag.getSourceFile
    }

  def toPacket: serializer.Stmt = serializer.Stmt(sootMethod, index, sootStmt)
  def prevSyntactic: Stmt = this.copy(sootStmt = Soot.getBody(sootMethod).getUnits.getPredOf(sootStmt))
  def nextSyntactic: Stmt = this.copy(sootStmt = Soot.getBody(sootMethod).getUnits.getSuccOf(sootStmt))
  def nextSemantic: List[Stmt] =
    sootStmt match {
      case _ : ReturnStmt => List.empty
      case _ : ReturnVoidStmt => List.empty
      case _ : ThrowStmt => List.empty
      case sootStmt : GotoStmt => List(this.copy(sootStmt = sootStmt.getTarget))
      case sootStmt : SwitchStmt => (sootStmt.getDefaultTarget :: sootStmt.getTargets.asScala.toList).map(u => this.copy(sootStmt = u))
      case sootStmt : IfStmt => List(this.nextSyntactic, this.copy(sootStmt = sootStmt.getTarget))
      case _ => List(this.nextSyntactic)
    }

  // TODO: print statement offset of target for "goto" and "if"
  override def toString : String = sootMethod + ":" + index + ":" + sootStmt
}
