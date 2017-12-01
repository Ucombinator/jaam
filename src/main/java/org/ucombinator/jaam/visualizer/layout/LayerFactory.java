package org.ucombinator.jaam.visualizer.layout;

import java.util.*;

import org.ucombinator.jaam.visualizer.graph.AbstractVertex;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;

public class LayerFactory
{
    private static final boolean createChains = true;
    private static final boolean chainsExpanded = true;
    private static final int CHAIN_LENGTH = 3 ; // This value should ALWAYS be LARGER THAN OR EQUAL 3 (otherwise it will break)

    public static LayoutRootVertex getLayeredGraph(Graph graph){
        return getStronglyConnectedComponentsGraph(graph);
        //return getLoopLayout(graph);
    }
    private static LayoutRootVertex getStronglyConnectedComponentsGraph(Graph graph)
    {
        ArrayList< ArrayList<Integer>> sccs = GraphUtils.StronglyConnectedComponents(graph);

    	LayoutRootVertex root = new LayoutRootVertex();

        HierarchicalGraph sccGraph = new HierarchicalGraph();
        root.setInnerGraph(sccGraph);

        // Need these two maps for the second pass to avoid having to look around for everything
        HashMap<AbstractVertex      , AbstractLayoutVertex> inputToInner = new HashMap<>();
        HashMap<AbstractLayoutVertex, AbstractLayoutVertex> innerToSCC   = new HashMap<>();
        // First pass create SCC vertex and populate with layout vertices
        for (ArrayList<Integer> scc : sccs)
        {
            if(scc.size() > 1) {
                int sccId = sccGraph.getVisibleVertices().size();
                LayoutSccVertex sccVertex = new LayoutSccVertex(sccId, "SCC-" + sccId);
                sccGraph.addVertex(sccVertex);

                HierarchicalGraph sccInner = new HierarchicalGraph();
                sccVertex.setInnerGraph(sccInner);

                for (Integer id : scc) {
                    AbstractVertex v = graph.containsInputVertex(id);

                    AbstractLayoutVertex innerVertex = upgradeVertex(v);

                    sccInner.addVertex(innerVertex);

                    // Add to hash tables for next pass
                    inputToInner.put(v, innerVertex);
                    innerToSCC.put(innerVertex, sccVertex);
                }
            }
            else {
                int id = scc.get(0);
                AbstractVertex v = graph.containsInputVertex(id);

                AbstractLayoutVertex newVertex = upgradeVertex(v);

                sccGraph.addVertex(newVertex);

                // Add to hash tables for next pass
                inputToInner.put(v, newVertex);
                innerToSCC.put(newVertex, newVertex);
            }
        }

        // Second pass add edges between SCC vertices and add edges inside SCC vertices
        for(AbstractVertex inputV: graph.getVertices())
        {
            AbstractLayoutVertex v = inputToInner.get(inputV);
            AbstractLayoutVertex vSCC = innerToSCC.get(v);

            // TODO probably should have a better way
            if(vSCC.getInnerGraph().getVisibleVertices().size() > 0) // Am a SCC node
            {
                for(AbstractVertex inputN: graph.getOutNeighbors(inputV))
                {
                    AbstractLayoutVertex n = inputToInner.get(inputN);
                    AbstractLayoutVertex nSCC = innerToSCC.get(n);

                    if(vSCC == nSCC)
                    {
                        HierarchicalGraph inner = vSCC.getInnerGraph();
                        inner.addEdge(new LayoutEdge(v,n, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
                    }
                    else
                    {
                        sccGraph.addEdge(new LayoutEdge(vSCC, nSCC, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
                    }
                }
            }
            else // Am some other type node not part of an SCC
            {
                for(AbstractVertex inputN: graph.getOutNeighbors(inputV)) {
                    AbstractLayoutVertex n = inputToInner.get(inputN);

                    AbstractLayoutVertex nSCC = innerToSCC.get(n);

                    sccGraph.addEdge(new LayoutEdge(vSCC, nSCC, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
                }
            }
        }

        return root;
    }

    // Takes an input vertex (a AbstractVertex which is actually a Loop or Method Vertex) and returns a new
    // vertex of the correct type
    private static AbstractLayoutVertex upgradeVertex(AbstractVertex v)
    {
        AbstractLayoutVertex newVertex;

        if(v instanceof LayoutLoopVertex)
        {
            LayoutLoopVertex l = (LayoutLoopVertex) v;
            newVertex = new LayoutLoopVertex(l.getId(), l.getLabel(), l.getStatementIndex(),
                    l.getCompilationUnit());
            //newVertex = new LayoutLoopVertex(v.getId(), v.getLabel(), 0);
        }
        else if(v instanceof  LayoutMethodVertex)
        {
            LayoutMethodVertex l = (LayoutMethodVertex) v;
            newVertex = new LayoutMethodVertex(l.getId(), l.getLabel(), l.getCompilationUnit());
        }
        else
        {
            newVertex = null;
        }

        return newVertex;
    }
}
