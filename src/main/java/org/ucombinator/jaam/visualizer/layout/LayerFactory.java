package org.ucombinator.jaam.visualizer.layout;

import java.util.*;

import org.ucombinator.jaam.visualizer.graph.AbstractVertex;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.GraphUtils;

public class LayerFactory
{
    private static final boolean create_chains = true;
    private static final boolean chains_expanded = true;
    //private static final int CHAIN_LENGTH = 3 ; // This value should ALWAYS be LARGER THAN OR EQUAL 3 (otherwise it will break)

    public static LayoutRootVertex getLayeredGraph(Graph graph){
        //return get2layer(graph);
        return getStronglyConnectedComponentsGraph(graph);
        //return cullGraph(graph);
    }
//
//    public static LayoutRootVertex cullGraph(Graph graph)
//    {
//        System.out.println("JUAN Culling graph of " + graph.getVertices().size());
//
//        ArrayList< ArrayList<Integer>> sccs =  (new GraphUtils()).StronglyConnectedComponents(graph);
//        System.out.print(sccs);
//
//        sccs.sort( (a, b) -> b.size() - a.size() );
//
//        System.out.println("JUAN TEST " + sccs.get(0));
//        ArrayList<Integer> scc = sccs.get(0);
//
//        ArrayList<AbstractVertex<AbstractVertex>> vertices = graph.getVertices();
//        //Remove edges
//        for(AbstractVertex<AbstractVertex> v : vertices)
//        {
//            if(scc.contains(v.getId()))
//            {
//                HashSet outgoingNeighbors = v.getOutgoingNeighbors();
//                Iterator iterator = outgoingNeighbors.iterator();
//
//                for(Iterator it = outgoingNeighbors.iterator(); it.hasNext();)
//                {
//                    Object no = it.next();
//                    AbstractVertex<AbstractVertex> n  = (AbstractVertex<AbstractVertex>)no;
//                    System.out.print("JUAN Checking " + v.getId() + " -> " + n.getId());
//                    if( !scc.contains(n.getId()))
//                    {
//                        System.out.print(" Removed");
//                        it.remove();
//                    }
//                    System.out.print("\n");
//                }
//
//
//                //outgoingNeighbors.removeIf((Object n) -> !scc.contains(((AbstractVertex) v).getId()));
//            }
//            else
//            {
//                HashSet outgoingNeighbors = v.getOutgoingNeighbors();
//                Iterator iterator = outgoingNeighbors.iterator();
//
//                for(Iterator it = outgoingNeighbors.iterator(); it.hasNext();)
//                {
//                    Object no = it.next();
//                    AbstractVertex<AbstractVertex> n  = (AbstractVertex<AbstractVertex>)no;
//                    System.out.print("\t\t\tJUAN Checking " + v.getId() + " -> " + n.getId());
//                    System.out.print("\n");
//                }
//
//            }
//        }
//
//        vertices.removeIf((AbstractVertex<AbstractVertex> v) -> !scc.contains(v.getId()));
//
//        for(AbstractVertex<AbstractVertex> v : vertices)
//        {
//            if(scc.contains(v.getId()))
//            {
//                HashSet outgoingNeighbors = v.getOutgoingNeighbors();
//                for(Object on : outgoingNeighbors)
//                {
//                    AbstractVertex<AbstractVertex> n = (AbstractVertex<AbstractVertex>)on;
//                    if(!scc.contains(n.getId()))
//                    {
//                        System.out.println("JUAN ERROR: Improper edge!! " + v.getId() + " -> " + n.getId() );
//                    }
//                }
//            }
//            else
//            {
//                System.out.println("JUAN ERROR! How did you get out? " + v.getId());
//            }
//        }
//
//
//        System.out.println("JUAN GRAPH CULLED " + graph.getVertices().size());
//
//        return get2layer(graph);
//    }
//    
    private static LayoutRootVertex getStronglyConnectedComponentsGraph(Graph graph)
    {
    	LayoutRootVertex root = new LayoutRootVertex();

        ArrayList< ArrayList<Integer>> sccs = GraphUtils.StronglyConnectedComponents(graph);

        HashMap<String, AbstractVertex<AbstractVertex>> id_to_abs_vertex = new LinkedHashMap<String, AbstractVertex<AbstractVertex>>();
        for(AbstractVertex<AbstractVertex> v: graph.getVertices()){
        	System.out.println("Vertex: +'"+v.getId()+"'");
        	id_to_abs_vertex.put(""+v.getId(), v);
        }

        HashMap<String, LayoutMethodVertex> vertex_to_scc = new LinkedHashMap<String, LayoutMethodVertex>();
        HashMap<Integer, AbstractVertex<AbstractVertex>> methodVertex_to_vertex = new HashMap<>();
        HashMap<Integer, LayoutMethodVertex > vertex_to_methodVertex = new HashMap<>();
        HierarchicalGraph rootGraph = new HierarchicalGraph();
        // TODO This should be cleaner
        int i = graph.getVertices().size()+1;
        for (ArrayList<Integer> scc: sccs){
        	System.out.println("Scc: "+i);
        	//Method m = new Method(null, ""+i++);
        	LayoutMethodVertex sccVertex = new LayoutMethodVertex(i,"scc"+i);
        	++i;

        	System.out.println(sccVertex.getStrID());
            rootGraph.addVertex(sccVertex);

            HierarchicalGraph gSCC = new HierarchicalGraph();
            for (Integer id: scc){

                //Method mSCC = graph.getMethod(id_to_abs_vertex.get(id).getMethodName());
                String idValue = ""+id.intValue();
                vertex_to_scc.put(idValue, sccVertex);
                //child_to_scc.put(""+id, sccVertex);
                //String methodName = id_to_abs_vertex.get(idValue).getMethodName();
                //System.out.println("Method name: " + methodName);
                LayoutMethodVertex mVertex = new LayoutMethodVertex(id, graph.containsInputVertex(id).getLabel());
                //LayoutInstructionVertex v = new LayoutInstructionVertex(id_to_abs_vertex.get(id).getInstruction(), sccVertex, true);

                methodVertex_to_vertex.put(mVertex.getId(), id_to_abs_vertex.get(""+id));
                vertex_to_methodVertex.put(id             , mVertex);

                gSCC.addVertex(mVertex);
            }
            sccVertex.setInnerGraph(gSCC);
        }

        // Add edges between SCC Vertices
        HashMap<String, LayoutEdge> edges = new LinkedHashMap<>();
        for(AbstractVertex<AbstractVertex> vertex : graph.getVertices()){
            // Not sure why we need an Object instead of a Vertex here
            for(AbstractVertex<AbstractVertex> neighbor : graph.getOutNeighbors(vertex)) {
                String tempID = vertex.getId() + "--" + neighbor.getId();
                if(!edges.containsKey(tempID))
                {
                    AbstractLayoutVertex from = vertex_to_scc.get(""+vertex.getId());
                    AbstractLayoutVertex to = vertex_to_scc.get(""+neighbor.getId());

                    System.out.println("From:" +from);
                    System.out.println("To:" +to);
                    if(from != to) {    // We are not distinguishing recursive calls
                        LayoutEdge e = new LayoutEdge(from, to, LayoutEdge.EDGE_TYPE.EDGE_REGULAR);
                        System.out.println("Edge: " + e.getSource().getId() + " ----> " + e.getDest().getId());
                        edges.put(tempID, e);
                        rootGraph.addEdge(e);
                    }else{
                    	System.out.println("SelfLoop");
                    }
                }
            }
        }

        // Create inner graph for each method vertex.
        for(AbstractLayoutVertex sccVertex: rootGraph.getVertices()) {

            System.out.println("JUAN: sccVertex");
            HashSet<AbstractLayoutVertex> innerVertices = sccVertex.getInnerGraph().getVertices();
            if(innerVertices.size() <= 1)
                continue;
            System.out.println("JUAN size " + innerVertices.size());
            for(AbstractLayoutVertex mv: innerVertices)
            {
                System.out.println("\tJUAN: Inner Vertex " + mv.getId());
                if(!methodVertex_to_vertex.containsKey(mv.getId()))
                {
                    System.out.println("JUAN Found a NULL Boy " + mv.getId());
                    continue;
                }
                AbstractVertex<AbstractVertex> v = methodVertex_to_vertex.get(mv.getId());
                for(AbstractVertex<AbstractVertex> graphNeighbor: graph.getOutNeighbors(v))
                {
                   LayoutMethodVertex mn = vertex_to_methodVertex.get(graphNeighbor.getId());

                   //if(innerVertices.containsKey(mn.getStrID()))
                   {
                       System.out.println("\t\t\tJUAN: ADDING EDGE");
                       sccVertex.getInnerGraph().addEdge(new LayoutEdge(mv, mn, LayoutEdge.EDGE_TYPE.EDGE_REGULAR));
                   }

                }
            }
            /*
            // Add the edges of the inner graph.
            for(Vertex v: methodBuckets.get(methodVertex.getMethodName())){
                for(Object neighborObj: v.getOutgoingNeighbors()){
                    Vertex neighbor = (Vertex) neighborObj;
                    if(v.getMethodName().equals(neighbor.getMethodName())){
                        methodVertex.getInnerGraph().addEdge(
                                new LayoutEdge(
                                        methodVertex.getInnerGraph().getVertices().get(idMapping.get(v.getStrID())),
                                        methodVertex.getInnerGraph().getVertices().get(idMapping.get(neighbor.getStrID())),
                                        LayoutEdge.EDGE_TYPE.EDGE_REGULAR
                                )
                        );
                    }
                }
            }
            */
        }

        graph.matchMethodsToClasses();

        root.setInnerGraph(rootGraph);
       
        return root;
    }

//    
//    private static LayoutRootVertex get2layer(Graph graph)
//    {
//        System.out.println("JUAN Graph coming has " + graph.getVertices().size());
//
//        HashMap<String, AbstractVertex<AbstractVertex>> id_to_vertex =  new LinkedHashMap<String, AbstractVertex<AbstractVertex>>();
//        HashMap<String, AbstractVertex> id_to_abs_vertex = new LinkedHashMap<String, AbstractVertex>();
//        HierarchicalGraph methodGraph = new HierarchicalGraph();
//        
//        // We partition the vertex set of Main.graph into buckets corresponding to the methods.
//        HashMap<String, HashSet<AbstractVertex<AbstractVertex>>> methodBuckets = new LinkedHashMap<String, HashSet<AbstractVertex<AbstractVertex>>>();
//        for(AbstractVertex<AbstractVertex> vertex: graph.getVertices()) {
//            //System.out.println("Reading vertex: " + vertex.getInstructionText());
//            String method = vertex.getMethodName();
//            if(!methodBuckets.containsKey(method)){
//                methodBuckets.put(method, new LinkedHashSet<AbstractVertex<AbstractVertex>>());
//                //System.out.println("Creating bucket for method: " + method);
//            }
//            methodBuckets.get(method).add(vertex);
//        }
//        
//        // Add a vertex for each method to the methodGraph.
//        HashMap<String, LayoutMethodVertex> methodVertices = new LinkedHashMap<>();
//        for(String method: methodBuckets.keySet()) {
//            //System.out.println("Creating method node for method: " + method);
//            LayoutMethodVertex vertex = new LayoutMethodVertex(graph.getMethod(method), true);
//            vertex.setExpanded(methods_expanded);
//            methodVertices.put(method, vertex);
//            methodGraph.addVertex(vertex);
//        }
//
//        // Add edges to the methodGraph.
//        HashMap<String, LayoutEdge> edges = new LinkedHashMap<String, LayoutEdge>();
//        for(AbstractVertex<AbstractVertex> vertex: graph.getVertices()){
//            // Not sure why we need an Object instead of a Vertex here
//            for(Object neighborObj: vertex.getOutgoingNeighbors()) {
//                AbstractVertex<AbstractVertex> neighbor = (AbstractVertex<AbstractVertex>) neighborObj;
//                String tempID = vertex.getMethodName() + "--" + neighbor.getMethodName();
//                if(!edges.containsKey(tempID))
//                {
//                    AbstractLayoutVertex absVertex = methodVertices.get(vertex.getMethodName());
//                    AbstractLayoutVertex absNeigh = methodVertices.get(neighbor.getMethodName());
//
//                    if(absVertex != absNeigh) {    // We are not distinguishing recursive calls
//                        LayoutEdge e = new LayoutEdge(absVertex, absNeigh, LayoutEdge.EDGE_TYPE.EDGE_REGULAR);
//                        edges.put(tempID, e);
//                        methodGraph.addEdge(e);
//                    }
//                }
//            }
//        }
//        
//        // Create inner graph for each method vertex.
//        for(AbstractLayoutVertex methodVertexAbs: methodGraph.getVertices().values()) {
//            // Add vertices of the inner graph.
//            LayoutMethodVertex methodVertex = (LayoutMethodVertex) methodVertexAbs;
//            HashMap<String,String> idMapping = new LinkedHashMap<>(); // first id is the graph vertex id and the second id the New vertex id
//            for(AbstractVertex<AbstractVertex> oldV: methodBuckets.get(methodVertex.getMethodName())) {
//                LayoutInstructionVertex newV = new LayoutInstructionVertex(oldV.getInstruction(), methodVertex, true);
//
//                id_to_vertex.put(oldV.getStrID(), oldV);
//                id_to_abs_vertex.put(oldV.getStrID(), newV);
//                idMapping.put(oldV.getStrID(), newV.getStrID());
//                methodVertex.getInnerGraph().addVertex(newV);
//                methodVertex.setDefaultColor();
//            }
//            
//            // Add the edges of the inner graph.
//            for(AbstractVertex<AbstractVertex> v: methodBuckets.get(methodVertex.getMethodName())){
//                for(Object neighborObj: v.getOutgoingNeighbors()){
//                	AbstractVertex<AbstractVertex> neighbor = (AbstractVertex<AbstractVertex>) neighborObj;
//                    if(v.getMethodName().equals(neighbor.getMethodName())){
//                        methodVertex.getInnerGraph().addEdge(
//                                new LayoutEdge(
//                                        methodVertex.getInnerGraph().getVertices().get(idMapping.get(v.getStrID())),
//                                        methodVertex.getInnerGraph().getVertices().get(idMapping.get(neighbor.getStrID())),
//                                        LayoutEdge.EDGE_TYPE.EDGE_REGULAR
//                                        )
//                                );
//                    }
//                }
//            }
//        }
//        
//        LayoutRootVertex root = new LayoutRootVertex();
//        root.setInnerGraph(methodGraph);
//        ArrayList<LayoutEdge> dummies = HierarchicalGraph.computeDummyEdges(root);
//        
//        Iterator<LayoutEdge> itEdge = dummies.iterator();
//        while(itEdge.hasNext())
//        {
//            LayoutEdge e = itEdge.next();
//            AbstractLayoutVertex start = e.getSource();
//            AbstractLayoutVertex end = e.getDest();
//
//            start.getSelfGraph().addEdge(new LayoutEdge(start, end, LayoutEdge.EDGE_TYPE.EDGE_DUMMY));
//        }
//
//        System.out.println("JUAN Graph coming out has " + graph.getVertices().size() + " drawn graph has " + root.getMethodVertices().size());
//
//        graph.matchMethodsToClasses();
//
//        for(LayoutMethodVertex m: methodVertices.values())
//            m.identifyLoops();
//        root.computeHues();
//
//        createChainVertices(root, CHAIN_LENGTH);
//        createLoopVertices(root);
//        return root;
//    }

    
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
