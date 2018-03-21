package org.ucombinator.jaam.visualizer.layout;

import java.util.*;
import java.util.function.Function;

import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.state.*;
import org.ucombinator.jaam.visualizer.taint.*;

public class LayerFactory
{
    // TODO: Template these functions instead of copying them.
    public static StateRootVertex getLayeredLoopGraph(Graph<StateVertex, StateEdge> graph) {
        return LayerFactory.getStronglyConnectedComponentsLoopGraph(graph);
    }

    private static StateRootVertex getStronglyConnectedComponentsLoopGraph(Graph<StateVertex, StateEdge> graph)
    {
        List<List<Integer>> sccs = GraphUtils.StronglyConnectedComponents(graph);
        System.out.println("Strongly connected components in loop graph: " + sccs.size());

        HashMap<Integer, Integer> vertexToComponentIndex = new HashMap<>();
        for (int componentIndex = 0; componentIndex < sccs.size(); componentIndex++) {
            for (int vertexId : sccs.get(componentIndex)) {
                vertexToComponentIndex.put(vertexId, componentIndex);
            }
        }

        StateRootVertex graphRoot = new StateRootVertex();
        graphRoot.setChildGraph(graph);

        return (StateRootVertex) GraphUtils.constructCompressedGraph(graphRoot,
                v -> Integer.toString(vertexToComponentIndex.get(v.getId())),
                new Function<List<StateVertex>, StateVertex>() {
                    @Override
                    public StateVertex apply(List<StateVertex> stateVertices) {
                        StateSccVertex sccVertex = new StateSccVertex(0, "SCC-0"); // TODO: This ID isn't unique...
                        stateVertices.forEach(v -> sccVertex.getChildGraph().addVertex(v));
                        return sccVertex;
                    }
                },
                StateEdge::new);
    }

    public static TaintRootVertex getLayeredTaintGraph(Graph<TaintVertex, TaintEdge> graph) {
        return getStronglyConnectedComponentsTaintGraph(graph);
    }

    private static TaintRootVertex getStronglyConnectedComponentsTaintGraph(Graph<TaintVertex, TaintEdge> graph)
    {
        List<List<Integer>> sccs = GraphUtils.StronglyConnectedComponents(graph);
        System.out.println("Strongly connected components in taint graph: " + sccs.size());

        HashMap<Integer, Integer> vertexToComponentIndex = new HashMap<>();
        for (int componentIndex = 0; componentIndex < sccs.size(); componentIndex++) {
            for (int vertexId : sccs.get(componentIndex)) {
                vertexToComponentIndex.put(vertexId, componentIndex);
            }
        }

        TaintRootVertex graphRoot = new TaintRootVertex();
        graphRoot.setChildGraph(graph);

        for(TaintVertex v : graphRoot.getChildGraph().getVertices()) {
            System.out.println(v.getId() + ", " + vertexToComponentIndex.get(v.getId()));
        }

        return (TaintRootVertex) GraphUtils.constructCompressedGraph(graphRoot,
                v -> Integer.toString(vertexToComponentIndex.get(v.getId())),
                new Function<List<TaintVertex>, TaintVertex>() {
                    @Override
                    public TaintVertex apply(List<TaintVertex> stateVertices) {
                        TaintSccVertex sccVertex = new TaintSccVertex(0, "SCC-0"); // TODO: This ID isn't unique...
                        stateVertices.forEach(v -> sccVertex.getChildGraph().addVertex(v));
                        return sccVertex;
                    }
                },
                TaintEdge::new);
    }
}
