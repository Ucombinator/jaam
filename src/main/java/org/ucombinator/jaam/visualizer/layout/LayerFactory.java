package org.ucombinator.jaam.visualizer.layout;

import java.util.*;
import java.util.function.BiFunction;

import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.state.*;
import org.ucombinator.jaam.visualizer.taint.*;

public class LayerFactory
{
    // TODO: Template these functions instead of copying them.
    public static StateRootVertex getLayeredGraph(Graph<StateVertex, StateEdge> graph) {
        return getStronglyConnectedComponentsGraph(graph);
    }

    private static StateRootVertex getStronglyConnectedComponentsGraph(Graph<StateVertex, StateEdge> graph)
    {
        List<List<Integer>> sccs = GraphUtils.StronglyConnectedComponents(graph);
        System.out.println("Strongly connected components: " + sccs.size());

        HashMap<Integer, Integer> vertexToComponentIndex = new HashMap<>();
        for (int componentIndex = 0; componentIndex < sccs.size(); componentIndex++) {
            for (int vertexId : sccs.get(componentIndex)) {
                vertexToComponentIndex.put(vertexId, componentIndex);
            }
        }

        StateRootVertex graphRoot = new StateRootVertex();
        graphRoot.setChildGraph(graph);

        return (StateRootVertex) graphRoot.constructCompressedGraph(
                (StateVertex v) -> "Id-" + Integer.toString(vertexToComponentIndex.get(v.getId())),
                new BiFunction<String, Set<StateVertex>, StateVertex>() {
                    @Override
                    public StateVertex apply(String s, Set<StateVertex> stateVertices) {
                        StateSccVertex sccVertex = new StateSccVertex(0, "SCC-0"); // TODO: This ID isn't unique...
                        for (StateVertex v : stateVertices) {
                            sccVertex.getChildGraph().addVertex(v);
                        }
                        return sccVertex;
                    }
                },
                StateEdge::new);
    }

    public static void getLayeredGraph(Graph<TaintVertex, TaintEdge> graph, TaintRootVertex root) {
        getStronglyConnectedComponentsGraph(graph, root);
    }

    private static void getStronglyConnectedComponentsGraph(Graph<TaintVertex, TaintEdge> graph, TaintRootVertex root)
    {
        List<List<Integer>> sccs = GraphUtils.StronglyConnectedComponents(graph);
        System.out.println("Strongly connected components in taint graph: " + sccs.size());

        Graph<TaintVertex, TaintEdge> sccGraph = root.getChildGraph();

        // Need these two maps for the second pass to avoid having to look around for everything
        HashMap<TaintVertex, TaintVertex> inputToInner = new HashMap<>();
        HashMap<TaintVertex, TaintVertex> innerToSCC   = new HashMap<>();

        // First pass create SCC vertex and populate with layout vertices
        for (List<Integer> scc : sccs)
        {
            if(scc.size() > 1) {
                int sccId = sccGraph.getVertices().size();
                TaintSccVertex sccVertex = new TaintSccVertex(sccId, "SCC-" + sccId);
                sccGraph.addVertex(sccVertex);
                sccVertex.setParentGraph(sccGraph);

                Graph<TaintVertex, TaintEdge> sccInner = sccVertex.getChildGraph();

                for (Integer id : scc) {
                    TaintVertex v = graph.getVertexById(id);
                    TaintVertex innerVertex = upgradeTaintVertex(v);
                    sccInner.addVertex(innerVertex);
                    innerVertex.setParentGraph(sccInner);

                    // Add to hash tables for next pass
                    inputToInner.put(v, innerVertex);
                    innerToSCC.put(innerVertex, sccVertex);
                }
            }
            else {
                TaintVertex v = graph.getVertexById(scc.get(0));
                TaintVertex newVertex = upgradeTaintVertex(v);
                sccGraph.addVertex(newVertex);
                newVertex.setParentGraph(sccGraph);

                // Add to hash tables for next pass
                inputToInner.put(v, newVertex);
                innerToSCC.put(newVertex, newVertex);
            }
        }

        // Second pass add edges between SCC vertices and add edges inside SCC vertices
        for (TaintVertex inputV: graph.getVertices())
        {
            TaintVertex v = inputToInner.get(inputV);
            TaintVertex vSCC = innerToSCC.get(v);

            // TODO probably should have a better way
            if (vSCC.getChildGraph().getVertices().size() > 0) // Am a SCC node
            {
                for (TaintVertex inputN: graph.getOutNeighbors(inputV)) {
                    TaintVertex n = inputToInner.get(inputN);
                    TaintVertex nSCC = innerToSCC.get(n);

                    if (vSCC == nSCC) {
                        Graph<TaintVertex, TaintEdge> inner = vSCC.getChildGraph();
                        inner.addEdge(new TaintEdge(v, n));
                    } else {
                        sccGraph.addEdge(new TaintEdge(vSCC, nSCC));
                    }
                }
            }
            else // Am some other type node not part of an SCC
            {
                for(TaintVertex inputN: graph.getOutNeighbors(inputV)) {
                    TaintVertex n = inputToInner.get(inputN);

                    TaintVertex nSCC = innerToSCC.get(n);

                    sccGraph.addEdge(new TaintEdge(vSCC, nSCC));
                }
            }
        }
    }

    // Takes an input vertex (a Vertex which is actually a Loop or Method Vertex) and returns a new
    // vertex of the correct type
    // TODO: Can we rewrite our algorithm to make this upgrading unnecessary?
    private static StateVertex upgradeStateVertex(StateVertex v)
    {
        StateVertex newVertex;

        if(v instanceof StateLoopVertex) {
            StateLoopVertex l = (StateLoopVertex) v;
            newVertex = new StateLoopVertex(l.getId(), l.getLabel(), l.getStatementIndex(),
                    l.getCompilationUnit());
            //newVertex = new StateLoopVertex(v.getId(), v.getLabel(), 0);
        }
        else if(v instanceof StateMethodVertex) {
            StateMethodVertex l = (StateMethodVertex) v;
            newVertex = new StateMethodVertex(l.getId(), l.getLabel(), l.getCompilationUnit());
        }
        else {
            newVertex = null;
        }

        return newVertex;
    }

    // Takes an input vertex and upgrades it to a TaintAddress
    private static TaintVertex upgradeTaintVertex(TaintVertex v)
    {
        TaintVertex newVertex;

        if(v instanceof TaintStmtVertex) {
            TaintStmtVertex l = (TaintStmtVertex) v;
            newVertex = new TaintStmtVertex(l.getAddresses());
        } else if(v instanceof TaintAddress) {
            TaintAddress l = (TaintAddress) v;
            newVertex = new TaintAddress(l.getAddress());
        }
        else {
            newVertex = null;
        }

        return newVertex;
    }
}
