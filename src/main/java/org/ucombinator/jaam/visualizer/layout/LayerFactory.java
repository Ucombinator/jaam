package org.ucombinator.jaam.visualizer.layout;

import java.util.*;
import java.util.function.Function;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.graph.GraphTransform;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.state.*;
import org.ucombinator.jaam.visualizer.taint.*;

public class LayerFactory
{
    // TODO: Template these functions instead of copying them.
    public static GraphTransform<StateRootVertex, StateVertex> getLayeredLoopGraph(StateRootVertex root) {
        return LayerFactory.getStronglyConnectedComponentsLoopGraph(root);
    }

    private static GraphTransform<StateRootVertex, StateVertex> getStronglyConnectedComponentsLoopGraph(StateRootVertex root)
    {
        List<List<Integer>> sccs = GraphUtils.StronglyConnectedComponents(root.getInnerGraph());
        HashMap<Integer, Integer> vertexToComponentIndex = getVertexToComponentMap(sccs);
        System.out.println("Strongly connected components in loop graph: " + sccs.size());

        return GraphUtils.copyAndCompressGraph(root,
                v -> Integer.toString(vertexToComponentIndex.get(v.getId())),
                new Function<List<StateVertex>, StateVertex>() {
                    @Override
                    public StateVertex apply(List<StateVertex> stateVertices) {
                        StateSccVertex sccVertex = new StateSccVertex("SCC");
                        stateVertices.forEach(v -> {
                            sccVertex.getInnerGraph().addVertex(v);
                            v.setOuterGraph(sccVertex.getInnerGraph());
                        });
                        return sccVertex;
                    }
                },
                StateEdge::new);
    }

    public static GraphTransform<TaintRootVertex, TaintVertex> getLayeredTaintGraph(TaintRootVertex root) {
        return getMethodGroupingGraph(root);
    }

    public static GraphTransform<TaintRootVertex, TaintVertex> getTaintClassGrouping(TaintRootVertex root) {
        return getClassGroupingGraph(root);
    }

    private static GraphTransform<TaintRootVertex, TaintVertex> getStronglyConnectedComponentsTaintGraph(TaintRootVertex root) {
        List<List<Integer>> sccs = GraphUtils.StronglyConnectedComponents(root.getInnerGraph());
        HashMap<Integer, Integer> vertexToComponentIndex = getVertexToComponentMap(sccs);
        System.out.println("Strongly connected components in taint graph: " + sccs.size());

        return GraphUtils.copyAndCompressGraph(root,
                v -> Integer.toString(vertexToComponentIndex.get(v.getId())),
                new Function<List<TaintVertex>, TaintVertex>() {
                    @Override
                    public TaintVertex apply(List<TaintVertex> stateVertices) {
                        TaintSccVertex sccVertex = new TaintSccVertex("SCC");
                        stateVertices.forEach(v -> {
                            sccVertex.getInnerGraph().addVertex(v);
                            v.setOuterGraph(sccVertex.getInnerGraph());
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



    /*
    private static TaintRootVertex getClassClusteredTaintGraph(Graph<TaintVertex, TaintEdge> graph) {

        TaintRootVertex classGraphRoot = getClassGroupingGraph(graph);

        classGraphRoot.getInnerGraph().getVertices().forEach(classVertex -> {
            if (classVertex.getInnerGraph().getVertices().size() > 1) {
                TaintRootVertex methodRootVertex = getMethodGroupingGraph(classVertex.getInnerGraph());
                Graph<TaintVertex, TaintEdge> groupedMethodGraph = methodRootVertex.getInnerGraph();
                classVertex.setInnerGraph(groupedMethodGraph);
            }
        });
        return classGraphRoot;
    }
    */

    private static int calcSize(Graph<TaintVertex, TaintEdge> graph) {
        int total = 0;

        for(TaintVertex v : graph.getVertices())
        {
            if (v.getInnerGraph().getVertices().size() > 1) total += calcSize(v.getInnerGraph());
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

    private static GraphTransform<TaintRootVertex, TaintVertex> getClassGroupingGraph(TaintRootVertex root)
    {
        return GraphUtils.copyAndCompressGraph(root,
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
                            classVertex.getInnerGraph().addVertex(v);
                            v.setOuterGraph(classVertex.getInnerGraph());
                        });

                        return classVertex;
                    }
                },
                TaintEdge::new);
    }

    private static GraphTransform<TaintRootVertex, TaintVertex> getMethodGroupingGraph(TaintRootVertex root) {

        return GraphUtils.copyAndCompressGraph(root,
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

                        TaintVertex representative = stateVertices.stream().findFirst().get();
                        String className  = representative.getClassName();
                        String methodName = representative.getMethodName();
                        assert methodName != null;

                        TaintMethodVertex methodVertex = new TaintMethodVertex(className, methodName, LayoutAlgorithm.LAYOUT_ALGORITHM.DFS);
                        stateVertices.forEach(v -> {
                            methodVertex.getInnerGraph().addVertex(v);
                            v.setOuterGraph(methodVertex.getInnerGraph());
                        });
                        return methodVertex;
                    }
                },
                TaintEdge::new);
    }

}
