package org.ucombinator.jaam.util

import org.jgrapht.{Graph, Graphs}
import org.jgrapht.graph.{DefaultDirectedGraph, DefaultEdge}
import java.io.{BufferedWriter, File, FileWriter, IOException}
import scala.collection.JavaConverters._
import scala.collection.mutable.{ListBuffer, Map}
import com.microsoft.z3._
import soot.jimple.{Stmt => SootStmt, _}


class LabelledEdge extends DefaultEdge {
  private var label: CFBooleanExpr = _
  private var isTrueEdge: Boolean = false

  def this(label: CFBooleanExpr, isTrueEdge: Boolean) {
    this()
    this.label = label
    this.isTrueEdge = isTrueEdge
  }

  def getLabel: CFBooleanExpr = label

  def setLabel(label: CFBooleanExpr): Unit = {
    this.label = label
  }

  def isTrueBranch: Boolean = isTrueEdge

  override def toString: String = "(" + getSource + " : " + getTarget + " : " + label + ")"
}

class VertexNode(val id: String, val label: String = "", val stmt: SootStmt = null, val isBranch: Boolean = false, val isExitVertex: Boolean = false) {
  override def toString: String = id

  override def hashCode: Int = toString.hashCode

  override def equals(o: Any): Boolean = o.isInstanceOf[VertexNode] && (toString == o.toString)
}

class ControlFlowGraph {
  private var graph: DefaultDirectedGraph[VertexNode, LabelledEdge] = _
  private var alphabeticLabel: String = _
  private var vertices: Map[String, VertexNode] = _
  private var root: VertexNode = _
  val exitLabel: String = "Exit"
  private var branchVertices: Map[String, VertexNode] = _

  //init block
  {
    graph = new DefaultDirectedGraph[VertexNode, LabelledEdge](classOf[LabelledEdge])
    alphabeticLabel = "A"
    vertices = Map[String, VertexNode]()
    branchVertices = Map[String, VertexNode]()
  }

  def generateAlphabeticLabel(alphabeticLabel: String): String = {
    var resultString: String = ""
    var lastChar: Char = alphabeticLabel.charAt(alphabeticLabel.length - 1)
    if (lastChar == 'Z') {
      //if it starts with z and last char is Z, all chars in between are Z. so start with all A's
      if (alphabeticLabel.startsWith("Z")) {
        resultString = "A" * (alphabeticLabel.length + 1)
        return resultString
      } else {
        //find first occurence of Z, need to increment char before that
        val indexZ = alphabeticLabel.indexOf("Z")
        resultString = alphabeticLabel.take(indexZ - 1) + (alphabeticLabel.charAt(indexZ - 1).toInt + 1).toChar + ("A" * (alphabeticLabel.length - indexZ))
        return resultString
      }
    } else {
      lastChar = (lastChar.toInt + 1).toChar
      resultString = alphabeticLabel.take(alphabeticLabel.length - 1) + lastChar
      return resultString
    }
  }

  def addVertex(id: String, label: String = "", stmt: SootStmt = null, isBranch: Boolean = false, isExitVertex: Boolean = false): Unit = {
    var vertexNode: VertexNode = null

    if (vertices.contains(id)) return

    //if conditional stmt, label it with A-Z
    if (isBranch) {
      vertexNode = new VertexNode(id, alphabeticLabel, stmt, isBranch, isExitVertex)
      this.branchVertices += (alphabeticLabel -> vertexNode)
      alphabeticLabel = generateAlphabeticLabel(alphabeticLabel)
    } else {
      vertexNode = new VertexNode(id, label, stmt, isBranch, isExitVertex)
    }

    if (root == null) {
      root = vertexNode
    }

    graph.addVertex(vertexNode)
    this.vertices += (id -> vertexNode)
  }

  def addEdge(src: String, dest: String, isTrueEdge: Boolean = false): Unit = {
    val edge: LabelledEdge = new LabelledEdge(new CFBooleanExpr(), isTrueEdge)
    val srcVertex: VertexNode = vertices(src)
    val destVertex: VertexNode = vertices(dest)
    graph.addEdge(srcVertex, destVertex, edge)
  }

  def getGraph(): Graph[VertexNode, LabelledEdge] = graph

  def getInExpr(vertex: VertexNode): CFBooleanExpr = {

    if (vertex == root) return new CFBooleanExpr()

    val expr: CFBooleanExpr = new CFBooleanExpr()
    val incomingEdges: Set[LabelledEdge] = graph.incomingEdgesOf(vertex).asScala.toSet

    for (edge: LabelledEdge <- incomingEdges) {
      expr.addExpr(edge.getLabel)
    }
    expr
  }

  def getOutExpr(vertex: VertexNode, edge: LabelledEdge, inExpr: CFBooleanExpr): CFBooleanExpr = {
    var outExpr: CFBooleanExpr = inExpr.clone()
    var vertexLabel: Factor = new Factor(vertex.label)

    if (vertex.isBranch) {
      if (!edge.isTrueBranch) {
        vertexLabel = new Factor(vertex.label, false)
      }

      //if the current vertex is a branch statement, set expr as such for trueBranch
      if (inExpr.isIdentityExpr) {
        val term: Term = new Term(vertexLabel)
        outExpr = new CFBooleanExpr(term)
      } else {
        outExpr = outExpr.and(vertexLabel)
      }
    }
    outExpr
  }

  def setOutExpr(vertex: VertexNode, inExpr: CFBooleanExpr): Unit = {
    val outgoingEdges: Set[LabelledEdge] = graph.outgoingEdgesOf(vertex).asScala.toSet
    for (edge: LabelledEdge <- outgoingEdges) {
      val outExpr: CFBooleanExpr = getOutExpr(vertex, edge, inExpr)
      edge.setLabel(outExpr)
    }
  }

  def createBooleanExprFromGraph(): Unit = {
    var workList: ListBuffer[VertexNode] = new ListBuffer()
    val processedVertices: Map[String, CFBooleanExpr] = Map[String, CFBooleanExpr]()
    workList += root
    while (workList.nonEmpty) {
      val vertex: VertexNode = workList.head
      val successors: List[VertexNode] = Graphs.successorListOf(graph, vertex).asScala.toList

      val inExpr: CFBooleanExpr = getInExpr(vertex)
      setOutExpr(vertex, inExpr)

      for (successor: VertexNode <- successors) {
        processedVertices.get(successor.id) match {
          case Some(expr) => {
            //if successor already processed, add to worklist only if expr has changed
            if (expr != graph.getEdge(vertex, successor).getLabel) {
              //remove if its already there and add again
              workList -= successor
              workList += successor
            }
          }
          case None => {
            workList -= successor
            workList += successor
          }
        }
      }
      processedVertices += (vertex.id -> inExpr)
      workList -= vertex
    }
  }

  def renderGraphToFile(fileName: String): Unit = {
    var writer: BufferedWriter = null
    try {
      val file = new File(fileName)
      if (!file.exists) file.createNewFile
      writer = new BufferedWriter(new FileWriter(file))
      writer.write("digraph {\n")

      //add nodes to file
      for ((id, vertex) <- vertices) {
        if (vertex.isBranch) {
          writer.write(id + "[label= \"" + vertex.label + "\", style=\"filled\", color=\"azure3\"]\n")
        } else {
          writer.write(id + "[label= \"" + vertex.label + "\"]\n")
        }
      }

      //add edges
      for (edge: LabelledEdge <- graph.edgeSet().asScala.toSet) {
        val src = graph.getEdgeSource(edge).id
        val dest = graph.getEdgeTarget(edge).id

        if (edge.isTrueBranch) {
          writer.write(src + "->" + dest + "[label=\"" + edge.getLabel + "\", color=\"green3\"]\n")
        } else {
          writer.write(src + "->" + dest + "[label=\"" + edge.getLabel + "\"]\n")
        }
      }
      writer.write("}")
    }
    catch {
      case io: IOException => io.printStackTrace()
      case e: Exception => e.printStackTrace()
    }
    finally {
      if (writer != null) {
        writer.close()
      }
    }
  }

  def getExitVertex(): VertexNode = {
    var exitVertex: VertexNode = null

    for ((_, vertex) <- vertices) {
      if (vertex.isExitVertex) {
        exitVertex = vertex
      }
    }
    exitVertex
  }

  def makeFormula(context: Context, finalExpr: CFBooleanExpr): (BoolExpr, Map[String, BoolExpr]) = {
    val boolConstants: Map[String, BoolExpr] = Map[String, BoolExpr]()
    val constants: List[String] = finalExpr.getConstants()

    for (constant <- constants) {
      boolConstants += (constant -> context.mkBoolConst(constant))
    }

    var formula: BoolExpr = null

    for (term <- finalExpr.getTerms) {
      val factors = term.getFactors()
      var termExpr: BoolExpr = null

      for (factor <- factors) {
        var factorExpr: BoolExpr = boolConstants.getOrElseUpdate(factor.getValue(), context.mkBoolConst(factor.getValue()))
        if (!factor.polarity()) {
          factorExpr = context.mkNot(factorExpr)
        }

        if (termExpr == null) {
          termExpr = factorExpr
        } else {
          termExpr = context.mkAnd(termExpr, factorExpr)
        }
      }

      if (formula == null) {
        formula = termExpr
      } else {
        formula = context.mkOr(formula, termExpr)
      }
    }
    (formula, boolConstants)
  }

  def runSmtSolver(finalExpr: CFBooleanExpr): List[String] = {
    val context: Context = new Context(new java.util.HashMap[String, String])
    val resultTuple = makeFormula(context, finalExpr)
    val formula: BoolExpr = resultTuple._1
    val boolConstants: Map[String, BoolExpr] = resultTuple._2
    val solver: com.microsoft.z3.Solver = context.mkSolver()
    val constExprs: ListBuffer[BoolExpr] = boolConstants.values.toList.to[ListBuffer]
    var vitalBranches: ListBuffer[String] = ListBuffer[String]()

    //if formula contains only one branch variable, return that
    if (boolConstants.size == 1) {
      return boolConstants.keySet.toList
    }

    for ((constant, expr) <- boolConstants) {
      var predicateExprLeft: BoolExpr = null
      var predicateExprRight: BoolExpr = null
      var predicate: BoolExpr = null

      predicateExprLeft = context.mkAnd(context.mkImplies(context.mkNot(expr), formula), context.mkImplies(expr, context.mkNot(formula)))
      predicateExprLeft = context.mkForall(List(expr).toArray, predicateExprLeft, 0, null, null, null, null)
      predicateExprRight = context.mkAnd(context.mkImplies(expr, formula), context.mkImplies(context.mkNot(expr), context.mkNot(formula)))
      predicateExprRight = context.mkForall(List(expr).toArray, predicateExprRight, 0, null, null, null, null)
      predicate = context.mkOr(predicateExprLeft, predicateExprRight)
      predicate = context.mkExists((constExprs - expr).toArray, predicate, 0, null, null, null, null)
      solver.add(predicate)
      if (solver.check == Status.SATISFIABLE) {
        vitalBranches += constant
      }
    }
    vitalBranches.toList
  }

  def findVitalBranches(): List[IfStmt] = {
    val exitVertex: VertexNode = getExitVertex()
    if (exitVertex == null) {
      return List()
    }

    val finalExpr: CFBooleanExpr = getInExpr(exitVertex)
    val vitalBranchLabels: List[String] = runSmtSolver(finalExpr)
    val vitalBranches: ListBuffer[IfStmt] = ListBuffer[IfStmt]()

    for (label <- vitalBranchLabels) {
      val vertex: VertexNode = branchVertices.getOrElse(label, null)
      if (vertex != null) {
        vitalBranches += vertex.stmt.asInstanceOf[IfStmt]
      }
    }
    vitalBranches.toList
  }
}
