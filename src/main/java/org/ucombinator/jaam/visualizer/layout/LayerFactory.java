package org.ucombinator.jaam.visualizer.layout;

import java.util.*;
import java.util.function.Function;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.graph.GraphTransform;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.main.Main;
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
                TaintEdge::new, true);
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

                    //System.out.println("JUAN: Called hash function on class" + className );

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
                    else {
                        // We want to group only library nodes now...
                        if (Main.getSelectedMainTabController().codeViewController.haveCode(v.getClassName())) {
                           methodName = v.getLongText();
                        }
                    }
                    return longHash(methodName);
                },
                new Function<List<TaintVertex>, TaintVertex>() {
                    @Override
                    public TaintVertex apply(List<TaintVertex> taintVertices) {

                        TaintVertex representative = taintVertices.stream().findFirst().get();

                        if (representative.getMethodName() == null) {
                            return representative;
                        }

                        if (taintVertices.size() == 1) {
                            if (Main.getSelectedMainTabController().codeViewController.haveCode(representative.getClassName())) {
                                return representative;
                            }
                        }

                        String className  = representative.getClassName();
                        String methodName = representative.getMethodName();
                        assert methodName != null;

                        ArrayList<TaintVertex> inputs = new ArrayList<>(), inner = new ArrayList<>(), outputs = new ArrayList<>();

                        //System.out.println("Processing " + className + "." + methodName);
                        for (TaintVertex v : taintVertices) {

                            if (!(v instanceof TaintAddress)) {
                                inner.add(v);
                                System.out.println("\t" + v + " is not an address");
                                continue;
                            }

                            TaintAddress a = (TaintAddress)v;
                            //System.out.println("\t" + a.toString() + " is a " + a.type);

                            if (a.type == TaintAddress.Type.Parameter) {
                                inputs.add(v);
                            }
                            else if (a.type == TaintAddress.Type.Return) {
                                outputs.add(v);
                            }
                            else {
                                inner.add(v);
                            }
                        }

                        TaintMethodVertex methodVertex = new TaintMethodVertex(className, methodName, LayoutAlgorithm.LAYOUT_ALGORITHM.DFS,
                                inputs, inner, outputs);
                        return methodVertex;
                    }
                },
                TaintEdge::new,
                true);
    }

}
