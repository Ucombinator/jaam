package org.ucombinator.jaam.visualizer.taint;

import org.ucombinator.jaam.util.Stmt;
import org.ucombinator.jaam.visualizer.graph.Graph;

import java.util.ArrayList;
import java.util.HashMap;

public class TaintGraph extends Graph<TaintVertex> {

    public TaintGraph() {
        super();
    }

    // TODO: Rewrite
    public TaintGraph groupByStatement() {
        TaintGraph newTaintGraph = new TaintGraph();

        int nullCounter = 0;
        HashMap<TaintAddress, String> indexStrings = new HashMap<>();
        HashMap<String, ArrayList<TaintAddress>> stmtGroups = new HashMap<>();

        for(TaintVertex v : this.getVertices()) {
            if (v instanceof TaintAddress) {
                TaintAddress vAddress = (TaintAddress) v;
                Stmt stmt = vAddress.getAddress().stmt();

                // Separate vertices whose statement is null by adding a unique counter
                String stmtString;
                if (stmt == null) {
                    stmtString = "null" + Integer.toString(nullCounter);
                    nullCounter++;
                } else {
                    stmtString = stmt.toString();
                }
                indexStrings.put(vAddress, stmtString);

                if (stmtGroups.containsKey(stmtString)) {
                    stmtGroups.get(stmtString).add(vAddress);
                } else {
                    ArrayList<TaintAddress> newGroup = new ArrayList<>();
                    newGroup.add(vAddress);
                    stmtGroups.put(stmtString, newGroup);
                }
            }
        }

        HashMap<String, TaintVertex> newVertexIndex = new HashMap<>();
        for(String stmtString : stmtGroups.keySet()) {
            ArrayList<TaintAddress> stmtGroup = stmtGroups.get(stmtString);
            if(stmtGroup.size() > 1) {
                TaintStmtVertex stmtVertex = new TaintStmtVertex(stmtGroup);
                newTaintGraph.addVertex(stmtVertex);
                newVertexIndex.put(stmtString, stmtVertex);
            }
            else if (stmtGroup.size() == 1) {
                newTaintGraph.addVertex(stmtGroup.get(0));
                newVertexIndex.put(stmtString, stmtGroup.get(0));
            }
        }

        for(String stmtString : stmtGroups.keySet()) {
            TaintVertex currVertex = newVertexIndex.get(stmtString);
            ArrayList<TaintAddress> stmtGroup = stmtGroups.get(stmtString);

            for(TaintAddress v : stmtGroup) {
                for(TaintVertex w : this.getOutNeighbors(v)) {
                    if(w instanceof TaintAddress) {
                        TaintAddress wAddress = (TaintAddress) w;
                        String wString = indexStrings.get(wAddress);
                        TaintVertex nextVertex = newVertexIndex.get(wString);
                        if(currVertex != nextVertex) {
                            newTaintGraph.addEdge(currVertex, nextVertex);
                        }
                    }
                }
            }
        }

        return newTaintGraph;
    }
}
