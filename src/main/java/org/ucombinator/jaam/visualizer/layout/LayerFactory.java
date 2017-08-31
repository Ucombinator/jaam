package org.ucombinator.jaam.visualizer.layout;

import java.util.*;

import org.ucombinator.jaam.visualizer.graph.AbstractVertex;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;
import sun.security.provider.certpath.Vertex;

public class LayerFactory
{
    private static final boolean create_chains = true;
    private static final boolean chains_expanded = true;
    private static final int CHAIN_LENGTH = 3 ; // This value should ALWAYS be LARGER THAN OR EQUAL 3 (otherwise it will break)

    public static LayoutRootVertex getLayeredGraph(Graph graph){
        //return get2layer(graph);
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
            int sccId = sccGraph.getVertices().size();
            LayoutMethodVertex sccVertex = new LayoutMethodVertex(sccId, "SCC-"+sccId);
            sccGraph.addVertex(sccVertex);

            HierarchicalGraph sccInner = new HierarchicalGraph();
            sccVertex.setInnerGraph(sccInner);

            for(Integer id: scc)
            {
                AbstractVertex v = graph.containsInputVertex(id);

                LayoutMethodVertex innerVertex = new LayoutMethodVertex(v.getId(), v.getLabel());
                sccInner.addVertex(innerVertex);

                // Add to hash tables for next pass
                inputToInner.put(v, innerVertex);
                innerToSCC.put(innerVertex, sccVertex);
            }
        }

        // Second pass add edges between SCC vertices and add edges inside SCC vertices
        for(AbstractVertex inputV: graph.getVertices())
        {
            AbstractLayoutVertex v = inputToInner.get(inputV);
            for(AbstractVertex inputN: graph.getOutNeighbors(inputV))
            {
                if(inputN == inputV)
                    continue;
                AbstractLayoutVertex n = inputToInner.get(inputN);

                AbstractLayoutVertex vSCC = innerToSCC.get(v);
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

        return root;
    }

    private static LayoutRootVertex getLoopLayout(Graph graph)
    {
        // Upper level of the Layout Graph. Graph will be it's child
        LayoutRootVertex root = new LayoutRootVertex();

        HierarchicalGraph loopGraph = new HierarchicalGraph();

        // We will do it in two passes, first create vertices then add the edges
        HashMap<AbstractVertex, AbstractLayoutVertex> vertexToLayoutVertex = new HashMap<>();

        for(AbstractVertex v: graph.getVertices())
        {
            LayoutMethodVertex layoutV = new LayoutMethodVertex(v.getId(), v.getLabel());
            loopGraph.addVertex(layoutV);

            vertexToLayoutVertex.put(v, layoutV);
        }

        int totalEdges = 0;
        // Now the edges
        for(AbstractVertex v: graph.getVertices())
        {
           for(AbstractVertex n: graph.getOutNeighbors(v))
           {
               //if(n == v)
               //    continue;
               AbstractLayoutVertex from = vertexToLayoutVertex.get(v);
               AbstractLayoutVertex to   = vertexToLayoutVertex.get(n);

               totalEdges += 1;
               System.out.println("JUAN: New Edge: " + from.getId() + " --> " + to.getId() + " ::: " + totalEdges);

               loopGraph.addEdge(new LayoutEdge( from, to, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
           }
        }

        System.out.println("JUAN: TOTAL ADDED EDGES: " + totalEdges);
        System.out.println("JUAN: Loop Graph has " + loopGraph.getEdges().size() + " edges");
        System.out.println("JUAN Root is " + loopGraph.getRoot().getLabel());


        root.setInnerGraph(loopGraph);

        return root;
    }


    //private static LayoutRootVertex get2layer(Graph graph)
    //{
    //    System.out.println("JUAN Graph coming has " + graph.getVertices().size());

    //    HashMap<String, AbstractVertex> id_to_vertex =  new LinkedHashMap<String, AbstractVertex>();
    //    HashMap<String, AbstractVertex> id_to_abs_vertex = new LinkedHashMap<String, AbstractVertex>();
    //    HierarchicalGraph methodGraph = new HierarchicalGraph();

    //    // We partition the vertex set of Main.graph into buckets corresponding to the methods.
    //    HashMap<String, HashSet<AbstractVertex>> methodBuckets = new LinkedHashMap<String, HashSet<AbstractVertex>>();
    //    for(AbstractVertex vertex: graph.getVertices()) {
    //        //System.out.println("Reading vertex: " + vertex.getInstructionText());
    //        String method = vertex.getMethodName();
    //        if(!methodBuckets.containsKey(method)){
    //            methodBuckets.put(method, new LinkedHashSet<AbstractVertex>());
    //            //System.out.println("Creating bucket for method: " + method);
    //        }
    //        methodBuckets.get(method).add(vertex);
    //    }

    //    // Add a vertex for each method to the methodGraph.
    //    HashMap<String, LayoutMethodVertex> methodVertices = new LinkedHashMap<>();
    //    for(String method: methodBuckets.keySet()) {
    //        //System.out.println("Creating method node for method: " + method);
    //        LayoutMethodVertex vertex = new LayoutMethodVertex(graph.getMethod(method), true);
    //        vertex.setExpanded(methods_expanded);
    //        methodVertices.put(method, vertex);
    //        methodGraph.addVertex(vertex);
    //    }

    //    // Add edges to the methodGraph.
    //    HashMap<String, LayoutEdge> edges = new LinkedHashMap<String, LayoutEdge>();
    //    for(AbstractVertex vertex: graph.getVertices()){
    //        // Not sure why we need an Object instead of a Vertex here
    //        for(Object neighborObj: vertex.getOutgoingNeighbors()) {
    //            AbstractVertex neighbor = (AbstractVertex) neighborObj;
    //            String tempID = vertex.getMethodName() + "--" + neighbor.getMethodName();
    //            if(!edges.containsKey(tempID))
    //            {
    //                AbstractLayoutVertex absVertex = methodVertices.get(vertex.getMethodName());
    //                AbstractLayoutVertex absNeigh = methodVertices.get(neighbor.getMethodName());

    //                if(absVertex != absNeigh) {    // We are not distinguishing recursive calls
    //                    LayoutEdge e = new LayoutEdge(absVertex, absNeigh, LayoutEdge.EDGE_TYPE.EDGE_REGULAR);
    //                    edges.put(tempID, e);
    //                    methodGraph.addEdge(e);
    //                }
    //            }
    //        }
    //    }

    //    // Create inner graph for each method vertex.
    //    for(AbstractLayoutVertex methodVertexAbs: methodGraph.getVertices().values()) {
    //        // Add vertices of the inner graph.
    //        LayoutMethodVertex methodVertex = (LayoutMethodVertex) methodVertexAbs;
    //        HashMap<String,String> idMapping = new LinkedHashMap<>(); // first id is the graph vertex id and the second id the New vertex id
    //        for(AbstractVertex oldV: methodBuckets.get(methodVertex.getMethodName())) {
    //            LayoutInstructionVertex newV = new LayoutInstructionVertex(oldV.getInstruction(), methodVertex, true);

    //            id_to_vertex.put(oldV.getStrID(), oldV);
    //            id_to_abs_vertex.put(oldV.getStrID(), newV);
    //            idMapping.put(oldV.getStrID(), newV.getStrID());
    //            methodVertex.getInnerGraph().addVertex(newV);
    //            methodVertex.setDefaultColor();
    //        }

    //        // Add the edges of the inner graph.
    //        for(AbstractVertex v: methodBuckets.get(methodVertex.getMethodName())){
    //            for(Object neighborObj: v.getOutgoingNeighbors()){
    //            	AbstractVertex neighbor = (AbstractVertex) neighborObj;
    //                if(v.getMethodName().equals(neighbor.getMethodName())){
    //                    methodVertex.getInnerGraph().addEdge(
    //                            new LayoutEdge(
    //                                    methodVertex.getInnerGraph().getVertices().get(idMapping.get(v.getStrID())),
    //                                    methodVertex.getInnerGraph().getVertices().get(idMapping.get(neighbor.getStrID())),
    //                                    LayoutEdge.EDGE_TYPE.EDGE_REGULAR
    //                                    )
    //                            );
    //                }
    //            }
    //        }
    //    }

    //    LayoutRootVertex root = new LayoutRootVertex();
    //    root.setInnerGraph(methodGraph);
    //    //ArrayList<LayoutEdge> dummies = HierarchicalGraph.computeDummyEdges(root);

    //   // Iterator<LayoutEdge> itEdge = dummies.iterator();
    //   // while(itEdge.hasNext())
    //   // {
    //   //     LayoutEdge e = itEdge.next();
    //   //     AbstractLayoutVertex start = e.getSource();
    //   //     AbstractLayoutVertex end = e.getDest();

       //     start.getSelfGraph().addEdge(new LayoutEdge(start, end, LayoutEdge.EDGE_TYPE.EDGE_DUMMY));
       // }

    //    System.out.println("JUAN Graph coming out has " + graph.getVertices().size() + " drawn graph has " + root.getMethodVertices().size());

    //    graph.matchMethodsToClasses();

    //    for(LayoutMethodVertex m: methodVertices.values())
    //        m.identifyLoops();
    //    root.computeHues();

    //    createChainVertices(root, CHAIN_LENGTH);
        //createLoopVertices(root);
        //return root;
    //}

    
    static void createChainVertices(AbstractLayoutVertex parent, int k){
        Iterator<AbstractLayoutVertex> it = parent.getInnerGraph().getVertices().iterator();
        while(it.hasNext()) {
            AbstractLayoutVertex absVertex = it.next();
            absVertex.setVertexStatus(AbstractVertex.VertexStatus.WHITE);
            createChainVertices(absVertex, k);
        }
        
        if(create_chains) {
            createChainVerticesFromVertex(parent.getInnerGraph(), parent.getInnerGraph().getRoot(), k);
        }
    }

    private static void createChainVerticesFromVertex(HierarchicalGraph graph, AbstractLayoutVertex root, int k) {
        if (root == null) {
            return;
        }

        //System.out.println("collapseFromVertex");
        int i = 0;
        AbstractLayoutVertex currentVertex = root;
        ArrayList<AbstractLayoutVertex> chain = new ArrayList<AbstractLayoutVertex>();
        while (true) {
            currentVertex.setVertexStatus(AbstractVertex.VertexStatus.GRAY);
            Iterator<AbstractLayoutVertex> itChildren = graph.getOutNeighbors(currentVertex).iterator();
            ArrayList<AbstractLayoutVertex> grayChildren = new ArrayList<AbstractLayoutVertex>();
            while (itChildren.hasNext()) {
                AbstractLayoutVertex child = itChildren.next();
                if (child.getVertexStatus() == AbstractVertex.VertexStatus.WHITE) {
                    child.setVertexStatus(AbstractVertex.VertexStatus.GRAY);
                    grayChildren.add(child);
                }
            }


            ArrayList<AbstractLayoutVertex> copyOfIncoming = new ArrayList<>(graph.getInNeighbors(currentVertex));
            copyOfIncoming.removeAll(grayChildren);

            ArrayList<AbstractLayoutVertex> copyOfOutgoing = new ArrayList<>(graph.getOutNeighbors(currentVertex));
            copyOfOutgoing.removeAll(copyOfIncoming);

            if (grayChildren.size() == 1 && copyOfIncoming.size() <= 1 && copyOfOutgoing.size() == 1) {
                AbstractLayoutVertex child = grayChildren.get(0);
                chain.add(currentVertex);
                currentVertex = child;
            } else {
                chain.add(currentVertex);

                if (i >= k) {
                    AbstractLayoutVertex first = chain.get(0);
                    AbstractLayoutVertex last = chain.get(chain.size() - 1);

                    //Create the new vertex
                    LayoutChainVertex chainVertex = new LayoutChainVertex(true);
                    chainVertex.setExpanded(chains_expanded);

                    first.getSelfGraph().addVertex(chainVertex);
                    first.getSelfGraph().addEdge(new LayoutEdge(first, chainVertex, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
                    if (first.getSelfGraph().hasEdge(chain.get(1), first)) {
                        chainVertex.getSelfGraph().addEdge(new LayoutEdge(chainVertex, first, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
                    }
                    first.getSelfGraph().addEdge(new LayoutEdge(chainVertex, last, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
                    if (last.getSelfGraph().hasEdge(last, chain.get(chain.size() - 2))) {
                        chainVertex.getSelfGraph().addEdge(new LayoutEdge(last, chainVertex, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
                    }


                    chain.remove(chain.size() - 1);
                    Iterator<AbstractLayoutVertex> chainIt = chain.iterator();
                    chainIt.next(); // to start from the second node of the chain
                    AbstractLayoutVertex previous = chainIt.next();
                    chainVertex.getInnerGraph().addVertex(previous);
                    while (chainIt.hasNext()) {
                        AbstractLayoutVertex next = chainIt.next();
                        chainVertex.getInnerGraph().addVertex(next);
                        chainVertex.getInnerGraph().addEdge(new LayoutEdge(previous, next, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
                        if (first.getSelfGraph().hasEdge(next, previous)) {
                            chainVertex.getInnerGraph().addEdge(new LayoutEdge(next, previous, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
                        }
                        previous = next;
                    }


                    previous = first;
                    chainIt = chain.iterator();
                    chainIt.next();
                    while (chainIt.hasNext()) {
                        AbstractLayoutVertex next = chainIt.next();
                        first.getSelfGraph().deleteEdge(previous, next);
                        first.getSelfGraph().deleteEdge(next, previous);
                        first.getSelfGraph().deleteVertex(next);
                        previous = next;
                    }
                    last.getSelfGraph().deleteEdge(chain.get(chain.size() - 1), last);
                    last.getSelfGraph().deleteEdge(last, chain.get(chain.size() - 1));

                    chainVertex.calcMaxLoopHeight();
                    //System.out.println("Loop height for chain: " + chainVertex.getLoopHeight());
                }
                /********************************************************************************/
                itChildren = grayChildren.iterator();
                while (itChildren.hasNext()) {
                    createChainVerticesFromVertex(graph, itChildren.next(), k);
                }
                break;
            }

            //System.out.println("i:" + i);
            i++;
        }
    }

}
