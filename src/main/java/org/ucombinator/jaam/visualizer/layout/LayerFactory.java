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
        HashMap<Integer, Integer> vertexToComponentIndex = getVertexToComponentMap(sccs);
        System.out.println("Strongly connected components in loop graph: " + sccs.size());

        StateRootVertex graphRoot = new StateRootVertex();
        graphRoot.setChildGraph(graph);

        return (StateRootVertex) GraphUtils.constructCompressedGraph(graphRoot,
                v -> Integer.toString(vertexToComponentIndex.get(v.getId())),
                new Function<List<StateVertex>, StateVertex>() {
                    @Override
                    public StateVertex apply(List<StateVertex> stateVertices) {
                        StateSccVertex sccVertex = new StateSccVertex("SCC");
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
        HashMap<Integer, Integer> vertexToComponentIndex = getVertexToComponentMap(sccs);
        System.out.println("Strongly connected components in taint graph: " + sccs.size());

        TaintRootVertex graphRoot = new TaintRootVertex();
        graphRoot.setChildGraph(graph);

        return (TaintRootVertex) GraphUtils.constructCompressedGraph(graphRoot,
                v -> Integer.toString(vertexToComponentIndex.get(v.getId())),
                new Function<List<TaintVertex>, TaintVertex>() {
                    @Override
                    public TaintVertex apply(List<TaintVertex> stateVertices) {
                        TaintSccVertex sccVertex = new TaintSccVertex("SCC");
                        stateVertices.forEach(v -> sccVertex.getChildGraph().addVertex(v));
                        return sccVertex;
                    }
                },
                TaintEdge::new);
    }

    private static HashMap<Integer, Integer> getVertexToComponentMap(List<List<Integer>> components) {
        HashMap<Integer, Integer> vertexToComponentIndex = new HashMap<>();
        for (int componentIndex = 0; componentIndex < components.size(); componentIndex++) {
            for (int vertexId : components.get(componentIndex)) {
                vertexToComponentIndex.put(vertexId, componentIndex);
            }
        }
        return vertexToComponentIndex;
    }
}
