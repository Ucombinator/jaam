package org.ucombinator.jaam.analyzer

import org.ucombinator.jaam.serializer._
import java.io.{FileOutputStream}

import org.ucombinator.jaam.serializer

import scala.collection.mutable

object Chain {

  def MakeChain(graph : AnalysisGraph, file : String): AnalysisGraph = {

    def lookupOrCreateNode(node : Id[AnalysisNode], graph : AnalysisGraph) : Id[AnalysisNode] = {

      var newNode : Id[AnalysisNode] = null

      graph.abstractMap get node match{
        case Some(_) =>
          newNode = graph.abstractMap(node)
        case None =>
          newNode = graph.addBlankNode()
          graph.abstractMap(node) = newNode
      }

      newNode
    }

    def linkNode(node : Id[AnalysisNode]) : Boolean = {
      if ((graph.graph(node).inDegree == 1) && (graph.graph(node).outDegree() == 1)) {
        return true
      } else {
        return false
      }
    }

    val result = new AnalysisGraph()
    val seen = mutable.Map[Id[AnalysisNode], Boolean]()
    DFS(graph.rootId, null)

    def addEdges(from : Id[AnalysisNode],  to : Id[AnalysisNode]) {
      result.graph(from).outNodes += to
      result.graph(to).inNodes += from
    }

    def DFS(current : Id[AnalysisNode], chainRoot : Id[AnalysisNode]): Unit = {

      //println (current + " " + chainRoot)
      seen get current match {
        case Some(_) => {
          if (chainRoot != null) {
            //todo
            val id = result.abstractMap(current)
            val parentId = result.abstractMap(chainRoot)
            addEdges(id, parentId)
            //chainRoot = null
          }
          return
        }

        case None =>
          seen (current) = true
      }

      //add vertex to the new graph
      if(chainRoot == null) {

        var id : Id[AnalysisNode] = lookupOrCreateNode(current, result)

        result.graph(id).addAbstractNodeID(current)

        //check if its start node, then sets the root
        if(linkNode(current)) {
          DFS(graph.graph(current).outNodes(0), current)
          return
        }

        //add all edges to graph
        //go to all children
        for(outNode <- graph.graph(current).outNodes) {
          if (current == outNode) {
            addEdges(id, id)
          }
          else {
            var childId : Id[AnalysisNode] = lookupOrCreateNode(outNode, result)

            addEdges(id, childId)
            DFS(outNode, null)
          }

        }
      }

      //ChainRoot != null
      else {
        val parentId = result.abstractMap(chainRoot)

        //merge it into the chainRoot
        if(linkNode(current)) {
          result.graph(parentId).addAbstractNodeID(current)
          result.abstractMap(current) = parentId
          DFS(graph.graph(current).outNodes(0), chainRoot)
          return
        }

        //else: end of the chain
        //one more step is adding the back edge (we missed adding this edge in our process)

        var id: Id[AnalysisNode] = lookupOrCreateNode(current, result)

        result.graph(id).addAbstractNodeID(current)
        addEdges(parentId, id)
        for(outNode <- graph.graph(current).outNodes) {
          if(current == outNode) {
              addEdges(id, id)
          }
          else {
            var childId : Id[AnalysisNode] = lookupOrCreateNode(outNode, result)

            addEdges(id, childId)
            DFS(outNode, null)
          }
        }

      }
    }
    writeOut(file)

    /*
      Write the output of Chain to a .jaam file
     */
    def writeOut(file : String) {

      val outSerializer = new serializer.PacketOutput(new FileOutputStream(file))

      for((k,v) <- result.graph) {
        outSerializer.write(v.toPacket())
      }
      outSerializer.write(EOF())
      outSerializer.close()
    }

    result
  }

}

