package org.ucombinator.jaam.analyzer

import org.ucombinator.jaam.serializer.Id

import scala.collection.mutable

object Chain {

  def MakeChain(graph : AnalysisGraph): AnalysisGraph = {
    val result = new AnalysisGraph()
    val seen = mutable.Map[Id[AnalysisNode], Boolean]()

    def DFS(current : Id[AnalysisNode], chainRoot : Id[AnalysisNode]): Unit = {

      seen get current match{
        case Some(_) =>

          return

        case None =>
          seen (current) = true
      }

      //add vertex to the new graph
      if(chainRoot == null) {
        val id = result.addBlankNode()
        result.abstractMap(current) = id
        result.graph(id).addAbstractNodeID(current)

        //check if its start node, then sets the root
        if((graph.graph(current).inDegree == 1) && (graph.graph(current).outDegree() == 1)) {
          DFS(graph.graph(current).outNodes(0), current)
          return
        }
        //add all edges to graph
        //go to all children
        for(outNode <- graph.graph(current).outNodes) {
          if(current == outNode) {
            result.graph(id).outNodes +=
            continue
          }
        }
      }
      else {

      }

    }

    result
  }

}