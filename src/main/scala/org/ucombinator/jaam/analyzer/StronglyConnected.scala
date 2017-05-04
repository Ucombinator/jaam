package org.ucombinator.jaam.analyzer

class StronglyConnected {
//    def StrongConnect(graph : AnalysisGraph): AnalysisGraph = {
//      Vector  queue = null
//      var index = 0
//      var color = 0
//      for(all v in graph.nodes()) {
//        if(graph(v).index == null) {
//          Trajan(v)
//        }
//      }
//
//      def Trajan(node : AnalysisNode) = {
//        graph(node).index = index
//        graph(node).lowlink = index
//        index += 1
//        S.push(node)
//        v.onStack = true
//        for(all w in graph.out_edges(v)){
//          if(w.index == null){
//            Trajan(w)
//            node.lowlink = min(node.lowlink, w.lowlink)
//          }
//          else{
//            if(w.onStack == true){
//              node.lowlink = min(node.lowlink, w.index)
//            }
//          }
//        }
//
//        if(node.index == node.lowlink){
//          color += 1
//          w = S.pop()
//          w.onStack = false
//          graph(w).scc = color
//          while(w != node){
//            w = S.pop()
//            w.onStack = false
//            graph(w).scc = color
//          }
//        }
//      }
//    }
}
