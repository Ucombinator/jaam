package org.ucombinator.jaam.visualizer.layout;

import java.util.*;
import java.util.function.Function;

import javafx.scene.paint.Color;
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
                        stateVertices.forEach(v -> {
                            sccVertex.getChildGraph().addVertex(v);
                            v.setParentGraph(sccVertex.getChildGraph());
                        });
                        return sccVertex;
                    }
                },
                StateEdge::new);
    }

    public static TaintRootVertex getLayeredTaintGraph(Graph<TaintVertex, TaintEdge> graph) {
        System.out.println("JUAN: Found " + graph.getVertices().size() + " vertices");
        return getSuperTaintGraph(graph);
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
                        stateVertices.forEach(v -> {
                            sccVertex.getChildGraph().addVertex(v);
                            v.setParentGraph(sccVertex.getChildGraph());
                        });
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

    private static TaintRootVertex getSuperTaintGraph(Graph<TaintVertex, TaintEdge> graph) {

        System.out.println("JUAN: Start there are " + calcSize(graph));
        TaintRootVertex classGraphRoot = getClassGroupingGraph(graph);

        System.out.println("JUAN After " + calcSize(classGraphRoot.getChildGraph()));

        classGraphRoot.getChildGraph().getVertices().forEach(classVertex -> {
            if (classVertex.getChildGraph().getVertices().size() > 1) {
                TaintRootVertex methodRootVertex = getMethodGroupingGraph(classVertex.getChildGraph());
                Graph<TaintVertex, TaintEdge> groupedMethodGraph = methodRootVertex.getChildGraph();
                classVertex.setChildGraph(groupedMethodGraph);
            }
        });
        return classGraphRoot;
    }

    private static int calcSize(Graph<TaintVertex, TaintEdge> graph) {
        int total = 0;

        for(TaintVertex v : graph.getVertices())
        {
            if (v.getChildGraph().getVertices().size() > 1) total += calcSize(v.getChildGraph());
            else total++;
        }

        return total;
    }

    public static long longHash(String string) {
        long h = 98764321261L;
        int l = string.length();
        char[] chars = string.toCharArray();

        for (int i = 0; i < l; i++) {
            h = 31*h + chars[i];
        }
        return h;
    }

    private static TaintRootVertex getClassGroupingGraph(Graph<TaintVertex, TaintEdge> graph)
    {
        TaintRootVertex graphRoot = new TaintRootVertex();
        graphRoot.setChildGraph(graph);

        return (TaintRootVertex) GraphUtils.constructCompressedGraph(graphRoot,
                v -> {
                    String className = v.getClassName();

                    if (className == null) { className = v.toString(); }

                    System.out.println("JUAN: Called hash function on class" + className );

                    return longHash((className));
                },
                new Function<List<TaintVertex>, TaintVertex>() {
                    @Override
                    public TaintVertex apply(List<TaintVertex> stateVertices) {

                        String className = stateVertices.stream().findFirst().get().getClassName();

                        assert  className != null;

                        TaintSccVertex classVertex = new TaintSccVertex(className, LayoutAlgorithm.LAYOUT_ALGORITHM.DFS);
                        classVertex.setColor(Color.ORANGE);
                        stateVertices.forEach(v -> {
                            classVertex.getChildGraph().addVertex(v);
                            v.setParentGraph(classVertex.getChildGraph());
                        });

                        System.out.println("Creating class vertex " + className);


                        return classVertex;
                    }
                },
                TaintEdge::new);
    }

    private static TaintRootVertex getMethodGroupingGraph(Graph<TaintVertex, TaintEdge> graph)
    {
        TaintRootVertex graphRoot = new TaintRootVertex();
        graphRoot.setChildGraph(graph);

        return (TaintRootVertex) GraphUtils.constructCompressedGraph(graphRoot,
                v -> {
                    String methodName = v.getMethodName();
                    if (methodName == null) {
                        methodName = v.toString() + v.getLabel(); // Should be unique
                    }
                    return longHash(methodName);
                },
                new Function<List<TaintVertex>, TaintVertex>() {
                    @Override
                    public TaintVertex apply(List<TaintVertex> stateVertices) {

                        String methodName = stateVertices.stream().findFirst().get().getMethodName();
                        assert methodName != null;

                        TaintSccVertex methodVertex = new TaintSccVertex(methodName, LayoutAlgorithm.LAYOUT_ALGORITHM.DFS);
                        methodVertex.setColor(Color.HOTPINK);
                        stateVertices.forEach(v -> {
                            methodVertex.getChildGraph().addVertex(v);
                            v.setParentGraph(methodVertex.getChildGraph());
                        });
                        return methodVertex;
                    }
                },
                TaintEdge::new);
    }

}
