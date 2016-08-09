package org.ucombinator.jaam.analyzer

import org.ucombinator.jaam.serializer.Id

import scala.collection.mutable

import scala.util.control.Breaks._

object Chain {

  def MakeChain(graph : AnalysisGraph): AnalysisGraph = {
    val result = new AnalysisGraph()
    val seen = mutable.Map[Id[AnalysisNode], Boolean]()

    def DFS(current : Id[AnalysisNode], chainRoot : Id[AnalysisNode]): Unit = {

      seen get current match{
        case Some(_) =>
          if(chainRoot != null) {
            println("Error: set root to null")
            val id = result.abstractMap(current)
            result.graph(id).addAbstractNodeID(current)
            //chainRoot = null
          }
          return

        case None =>
          seen (current) = true
      }

      //add vertex to the new graph
      if(chainRoot == null) {

        var id : Id[AnalysisNode] = null
        result.abstractMap get current match{
          case Some(_) =>
            id = result.abstractMap(current)
          case None =>
            id = result.addBlankNode()
            result.abstractMap(current) = id
        }

        result.graph(id).addAbstractNodeID(current)

        //check if its start node, then sets the root
        if((graph.graph(current).inDegree == 1) && (graph.graph(current).outDegree() == 1)) {
          DFS(graph.graph(current).outNodes(0), current)
          return
        }
        //add all edges to graph
        //go to all children
        for(outNode <- graph.graph(current).outNodes) {
          breakable {
            if (current == outNode) {
              result.graph(id).outNodes += id
              result.graph(id).inNodes += id
              break
            }
          }

          var childId : Id[AnalysisNode] = null
          result.abstractMap get current match{
            case Some(_) =>
              childId = result.abstractMap(outNode)
            case None =>
              childId = result.addBlankNode()
              result.abstractMap(outNode) = childId
          }
          result.graph(id).outNodes += childId
          result.graph(childId).inNodes += id
          DFS(outNode, null)
        }
      }

      else {
        val parentId = result.abstractMap(chainRoot)

        //merge it into the chainRoot
        if((graph.graph(current).inDegree == 1) && (graph.graph(current).outDegree() == 1)) {
          result.graph(parentId).addAbstractNodeID(current)
          result.abstractMap(current) = parentId
          DFS(graph.graph(current).outNodes(0), chainRoot)
          return
        }

        //else: end of the chain
        //one more step is adding the back edge (we missed adding this edge in our process)

        var id: Id[AnalysisNode] = null
        result.abstractMap get current match{
          case Some(_) =>
            id = result.abstractMap(current)
          case None =>
            id = result.addBlankNode()
            result.abstractMap(current) = id
        }

        result.graph(id).addAbstractNodeID(current)
        result.graph(id).inNodes += parentId
        result.graph(parentId).outNodes += id
        //chainRoot = null
        for(outNode <- graph.graph(current).outNodes) {
          breakable {
            if(current == outNode) {
              result.graph(id).outNodes += id
              result.graph(id).inNodes += id
              break
            }
          }

          var childId : Id[AnalysisNode] = null
          result.abstractMap get current match{
            case Some(_) =>
              childId = result.abstractMap(outNode)
            case None =>
              childId = result.addBlankNode()
              result.abstractMap(outNode) = childId
          }
          result.graph(id).inNodes += childId
          result.graph(childId).outNodes += id
          DFS(outNode, null)
        }

      }
    }

    result
  }

}