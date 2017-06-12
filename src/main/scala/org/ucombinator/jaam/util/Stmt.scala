package org.ucombinator.jaam.util

import scala.collection.JavaConverters._

import org.ucombinator.jaam.serializer
import soot.{SootMethod, Unit => SootUnit}
import soot.jimple.{Stmt => SootStmt, _}
import soot.tagkit.{GenericAttribute, SourceFileTag}

import org.ucombinator.jaam.util.Soot.{unitToStmt, valueToExpr}

object Stmt {
  val indexTag = "org.ucombinator.jaam.Stmt.indexTag"

  def getIndex(sootStmt : SootStmt, sootMethod : SootMethod) : Int = {
    if (sootStmt.hasTag(Stmt.indexTag)) {
      BigInt(sootStmt.getTag(Stmt.indexTag).getValue).intValue
    } else {
      // label everything in the sootMethod so the amortized work is linear
      for ((u, i) <- Soot.getBody(sootMethod).getUnits().asScala.toList.zipWithIndex) {
        u.addTag(new GenericAttribute(Stmt.indexTag, BigInt(i).toByteArray))
      }

      assert(sootStmt.hasTag(Stmt.indexTag), "SootStmt "+sootStmt+" not found in SootMethod " + sootMethod)
      BigInt(sootStmt.getTag(Stmt.indexTag).getValue).intValue
    }
  }

  def methodEntry(sootMethod : SootMethod) = Stmt(Soot.getBody(sootMethod).getUnits.getFirst, sootMethod)
}

case class Stmt(val sootStmt : SootStmt, val sootMethod : SootMethod) extends CachedHashCode {
  val index = Stmt.getIndex(sootStmt, sootMethod)
  val line = sootStmt.getJavaSourceStartLineNumber
  val column = sootStmt.getJavaSourceStartColumnNumber
  val sourceFile = {
    val tag = sootMethod.getDeclaringClass.getTag("SourceFileTag")
    if (tag != null) {
      val sourceTag = tag.asInstanceOf[SourceFileTag]
      if (sourceTag.getAbsolutePath != null) { sourceTag.getAbsolutePath + "/" } else { "" } + sourceTag.getSourceFile
    } else { "<unknown>" }
  }

  def toPacket() : serializer.Stmt = serializer.Stmt(sootMethod, index, sootStmt)
  def nextSyntactic : Stmt = this.copy(sootStmt = Soot.getBody(sootMethod).getUnits().getSuccOf(sootStmt))
  def nextSemantic : List[Stmt] =
    sootStmt match {
      case sootStmt : ReturnStmt => List()
      case sootStmt : ReturnVoidStmt => List ()
      case sootStmt : ThrowStmt => List ()
      case sootStmt : GotoStmt => List(this.copy(sootStmt = sootStmt.getTarget))
      case sootStmt : SwitchStmt => sootStmt.getTargets.asScala.toList.map(u => this.copy(sootStmt = u))
      case sootStmt : IfStmt => List(this.nextSyntactic, this.copy(sootStmt = sootStmt.getTarget))
      case sootStmt => List(this.nextSyntactic)
    }

  // TODO: print statement offset of target for "goto" and "if"
  override def toString : String = sootMethod + ":" + index + ":" + sootStmt
}
