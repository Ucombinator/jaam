package org.ucombinator.jaam.visualizer.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.graph.AbstractVertex;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.Vertex;
import org.ucombinator.jaam.visualizer.main.Parameters;

public class LayerFactory
{
	private static final boolean create_chains = true;
	private static final boolean chains_expanded = true;
	private static final boolean methods_expanded = true;
	private static final int CHAIN_LENGTH = 3 ; // This value should ALWAYS be LARGER THAN OR EQUAL 3 (otherwise it will break)

	public static LayoutRootVertex getLayeredGraph(Graph graph){
		return get2layer(graph);
	}
	
	private static LayoutRootVertex get2layer(Graph graph)
	{
		HashMap<String, Vertex> id_to_vertex = new HashMap<String, Vertex>();
		HashMap<String, AbstractVertex> id_to_abs_vertex = new HashMap<String, AbstractVertex>();
		HierarchicalGraph methodGraph = new HierarchicalGraph();
		
		// We partition the vertex set of Main.graph into buckets corresponding to the methods.
		HashMap<String, HashSet<Vertex>> methodBuckets = new HashMap<String, HashSet<Vertex>>();
		for(Vertex vertex: graph.getVertices()) {
			//System.out.println("Reading vertex: " + vertex.getInstructionText());
			String method = vertex.getMethodName();
			if(!methodBuckets.containsKey(method)){
				methodBuckets.put(method, new HashSet<Vertex>());
				//System.out.println("Creating bucket for method: " + method);
			}
			methodBuckets.get(method).add(vertex);
		}
		
		// Add a vertex for each method to the methodGraph.
		HashMap<String, LayoutMethodVertex> methodVertices = new HashMap<>();
		for(String method: methodBuckets.keySet()) {
			//System.out.println("Creating method node for method: " + method);
			LayoutMethodVertex vertex = new LayoutMethodVertex(method, true);
			vertex.setExpanded(methods_expanded);
			methodVertices.put(method, vertex);
			methodGraph.addVertex(vertex);
		}

		// Add edges to the methodGraph.
		HashMap<String, LayoutEdge> edges = new HashMap<String, LayoutEdge>();
		for(Vertex vertex: graph.getVertices()){
			// Not sure why we need an Object instead of a Vertex here
			for(Object neighborObj: vertex.getOutgoingNeighbors()) {
				Vertex neighbor = (Vertex) neighborObj;
				String tempID = vertex.getMethodName() + "--" + neighbor.getMethodName();
				if(!edges.containsKey(tempID))
				{
					AbstractLayoutVertex absVertex = methodVertices.get(vertex.getMethodName());
					AbstractLayoutVertex absNeigh = methodVertices.get(neighbor.getMethodName());
					
					if(absVertex != absNeigh) {	// We are not distinguishing recursive calls
						LayoutEdge e = new LayoutEdge(absVertex, absNeigh, LayoutEdge.EDGE_TYPE.EDGE_REGULAR);
						edges.put(tempID, e);
						methodGraph.addEdge(e);
					}
				}
			}
		}
		
		// Create inner graph for each method vertex.
		for(AbstractLayoutVertex methodVertexAbs: methodGraph.getVertices().values()) {
			// Add vertices of the inner graph.
			LayoutMethodVertex methodVertex = (LayoutMethodVertex) methodVertexAbs;
			HashMap<String,String> idMapping = new HashMap<>(); // first id is the graph vertex id and the second id the New vertex id
			for(Vertex oldV: methodBuckets.get(methodVertex.getMethodName())) {
				LayoutInstructionVertex newV = new LayoutInstructionVertex(oldV.getInstruction(), methodVertex, true);

				id_to_vertex.put(oldV.getStrID(), oldV);
				id_to_abs_vertex.put(oldV.getStrID(), newV);
				idMapping.put(oldV.getStrID(), newV.getStrID());
				methodVertex.getInnerGraph().addVertex(newV);
			}
			
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
		}
		
		LayoutRootVertex root = new LayoutRootVertex();
		root.setInnerGraph(methodGraph);
		ArrayList<LayoutEdge> dummies = HierarchicalGraph.computeDummyEdges(root);
		
		Iterator<LayoutEdge> itEdge = dummies.iterator();
		while(itEdge.hasNext())
		{
			LayoutEdge e = itEdge.next();
			AbstractLayoutVertex start = e.getSourceVertex();
			AbstractLayoutVertex end = e.getDestVertex();

			start.getSelfGraph().addEdge(new LayoutEdge(start, end, LayoutEdge.EDGE_TYPE.EDGE_DUMMY));
		}

		for(LayoutMethodVertex m: methodVertices.values())
			m.identifyLoops();
		root.computeHues();

		createChainVertices(root, CHAIN_LENGTH);
		createLoopVertices(root);
		return root;
	}

	static void createChainVertices(AbstractLayoutVertex parent, int k){
		Iterator<AbstractLayoutVertex> it = parent.getInnerGraph().getVertices().values().iterator();
		while(it.hasNext()) {
			AbstractLayoutVertex absVertex = it.next();
			absVertex.setVertexStatus(AbstractVertex.VertexStatus.WHITE);
			createChainVertices(absVertex, k);
		}
		
		if(create_chains){
			createChainVerticesFromVertex(parent.getInnerGraph().getRoot(), k);
		}
	}

	private static void createChainVerticesFromVertex(AbstractLayoutVertex root, int k) {
		if (root == null) {
			return;
		}

		//System.out.println("collapseFromVertex");
		int i = 0;
		AbstractLayoutVertex currentVertex = root;
		ArrayList<AbstractLayoutVertex> chain = new ArrayList<AbstractLayoutVertex>();
		while (true) {
			currentVertex.setVertexStatus(AbstractVertex.VertexStatus.GRAY);
			Iterator<AbstractLayoutVertex> itChildren = currentVertex.getOutgoingNeighbors().iterator();
			ArrayList<AbstractLayoutVertex> grayChildren = new ArrayList<AbstractLayoutVertex>();
			while (itChildren.hasNext()) {
				AbstractLayoutVertex child = itChildren.next();
				if (child.getVertexStatus() == AbstractVertex.VertexStatus.WHITE) {
					child.setVertexStatus(AbstractVertex.VertexStatus.GRAY);
					grayChildren.add(child);
				}
			}


			ArrayList<AbstractLayoutVertex> copyOfIncoming = new ArrayList<AbstractLayoutVertex>(currentVertex.getIncomingNeighbors());
			copyOfIncoming.removeAll(grayChildren);

			ArrayList<AbstractLayoutVertex> copyOfOutgoing = new ArrayList<AbstractLayoutVertex>(currentVertex.getOutgoingNeighbors());
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
					createChainVerticesFromVertex(itChildren.next(), k);
				}
				break;
			}

			//System.out.println("i:" + i);
			i++;
		}
	}

	private static void createLoopVertices(AbstractLayoutVertex root) {
		// TODO: Run loop tool to get all loops
		// Create vertices for each one, and break method graph.
	}
}

