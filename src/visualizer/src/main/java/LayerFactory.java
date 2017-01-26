
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


import com.sun.media.jfxmedia.events.NewFrameEvent;

import javafx.scene.paint.Color;


public class LayerFactory
{
	private static final boolean create_chains = true;
	private static final boolean chains_expanded = true;
	private static final boolean methods_expanded = true;
	
	private static final int CHAIN_LENGTH = 3 ; // This value should ALWAYS be LARGER THAN OR EQUAL 3 (otherwise it will break)
	
	static HashMap<String, Vertex> id_to_vertex = new HashMap<String, Vertex>();
	static HashMap<String, AbstractVertex> id_to_abs_vertex = new HashMap<String, AbstractVertex>();
	
	
	static AbstractVertex getLayeredGraph(Graph graph){
		return get2layer(graph);
	}
	
	static AbstractVertex get2layer(Graph graph)
	{
		AbstractGraph methodGraph = new AbstractGraph();
		
		/* We partition the vertex set of Main.graph into buckets corresponding to the methods*/
		HashMap<String, HashSet<Vertex>> buckets = new HashMap<String, HashSet<Vertex>>();
		for(Vertex vertex: graph.vertices){
			String method = vertex.getMethodName();
			if(!buckets.containsKey(method)){
				buckets.put(method, new HashSet<Vertex>());
			}
			buckets.get(method).add(vertex);
		}
		
		
		// Add a vertex for each method to the methodGraph.
		HashMap<String, AbstractVertex> methodVertices = new HashMap<>();
		for(String method: buckets.keySet()){
				AbstractVertex vertex = new Vertex(method, AbstractVertex.VertexType.METHOD);
				vertex.getMetaData().put(AbstractVertex.METADATA_METHOD_NAME, method);
				vertex.getMetaData().put(AbstractVertex.METADATA_MERGE_PARENT, buckets.get(method).iterator().next().mergeParent);
				vertex.setExpanded(methods_expanded);
				methodVertices.put(method, vertex);
				methodGraph.addVertex(vertex);
		}

		// Add edges to the methodGraph.
		HashMap<String, Edge> edges = new HashMap<String,Edge>(); 
		for(Vertex vertex: graph.vertices){
			for(Vertex neighbor: vertex.neighbors){
				String tempID = vertex.getMethodName() + "--" + neighbor.getMethodName();
				if(!edges.containsKey(tempID))
				{
					AbstractVertex absVertex = methodVertices.get(vertex.getMethodName()); 
					AbstractVertex absNeigh = methodVertices.get(neighbor.getMethodName());
					
					if(absVertex!=absNeigh){	// We are not distinguishing recursive calls
						Edge e = new Edge(absVertex, absNeigh, Edge.EDGE_TYPE.EDGE_REGULAR);
						edges.put(tempID, e);
						methodGraph.addEdge(e);
					}
				}
			}
		}
		
		// Create inner graph for each method vertex.
		for(AbstractVertex methodVertex: methodGraph.getVertices().values()){
			//Create inner-vertices of the inner-methods graph.

			// Add vertices of the inner graph.
			HashMap<String,String> idMapping = new HashMap<>(); // first id is the Main.graph vertex id and the second id the New vertex id
			for(Vertex oldV: buckets.get(methodVertex.getLabel())){
				String inst = "";
				if(oldV.getRealInstruction() != null)
					inst = oldV.getRealInstruction().str;
				Vertex newV = new Vertex(inst, AbstractVertex.VertexType.INSTRUCTION);
				newV.getMetaData().put(AbstractVertex.METADATA_METHOD_NAME, methodVertex.getMetaData().get(AbstractVertex.METADATA_METHOD_NAME));
				newV.getMetaData().put(AbstractVertex.METADATA_INSTRUCTION, oldV.getRealInstruction());
				newV.setColor(convertToFXColor(VizPanel.hues[oldV.loopHeight]));
				newV.setMinInstructionLine(oldV.id);

				id_to_vertex.put(oldV.getStrID(), oldV);
				id_to_abs_vertex.put(oldV.getStrID(), newV);
				idMapping.put(oldV.getStrID(), newV.getStrID());

				methodVertex.getInnerGraph().addVertex(newV);
			}
			
			// Add the edges of the inner graph.
			for(Vertex v: buckets.get(methodVertex.getLabel())){
				for(Vertex neighbor: v.neighbors){
					if(v.getMethodName().equals(neighbor.getMethodName())){
						methodVertex.getInnerGraph().addEdge(
								new Edge(
										methodVertex.getInnerGraph().getVertices().get(idMapping.get(v.getStrID()))
										, 
										methodVertex.getInnerGraph().getVertices().get(idMapping.get(neighbor.getStrID()))
										,
										Edge.EDGE_TYPE.EDGE_REGULAR
										)
								);
					}
				}
			}
		}
		
		AbstractVertex root = new Vertex("root", AbstractVertex.VertexType.ROOT);
		root.setInnerGraph(methodGraph);
		root.bringUpInstructionValues();
//		
//		// Setting the Smallest_instruction_line of the method vertices
//		
//		for(AbstractVertex methodVertex: methodGraph.getVertices().values()){
//			Iterator<AbstractVertex> itInner = methodVertex.getInnerGraph().getVertices().values().iterator();
//			while(itInner.hasNext())
//			{
//				if(methodVertex.getMinInstructionLine() == -1)
//				{
//					methodVertex.setMinInstructionLine(itInner.next().getMinInstructionLine());
//				}
//				else
//				{
//					methodVertex.setMinInstructionLine(Math.min(methodVertex.getMinInstructionLine(),
//							itInner.next().getMinInstructionLine()));
//				}
//			}
//		}
		

		Collections.sort(graph.vertices);
		ArrayList<Edge> dummies = Graph.computeDummyEdges(graph.vertices.get(0));
		
		Iterator<Edge> itEdge = dummies.iterator();
		while(itEdge.hasNext())
		{
			Edge e = itEdge.next();
			AbstractVertex startOringal = e.getSourceVertex();
			AbstractVertex endOriginal = e.getDestVertex();
			
			AbstractVertex start = id_to_abs_vertex.get(startOringal.getStrID());
			AbstractVertex end = id_to_abs_vertex.get(endOriginal.getStrID());
			
			start.getSelfGraph().addEdge(new Edge(start,end,Edge.EDGE_TYPE.EDGE_DUMMY));
		}
		
		
		
		createChainVertices(root, CHAIN_LENGTH);
		
		/*System.out.println("Statistics:");
		System.out.println(JAAMUtils.RED("Number of edges: ") +JAAMUtils.YELLOW(""+root.getTotalEdgeCount()));
		System.out.println(JAAMUtils.RED("Number of vertices: ") + JAAMUtils.YELLOW(""+root.getTotalVertexCount()));*/

		return root;
	}

	static AbstractVertex get1layer(Graph graph)
	{
		AbstractGraph abstractGraph = new AbstractGraph();
		
		/* We partition the vertex set of Main.graph into buckets corresponding to the methods*/
		
		for(int i = 0; i < graph.vertices.size(); i++)
		{
			Vertex vertex = graph.vertices.get(i);
			String method = vertex.getMethodName();
			if(id_to_abs_vertex.containsKey(vertex.getStrID())){
				System.out.println("WARNING: there exists two vertices with the same StrID: " + vertex.getStrID());
			}else{
				Vertex newV = new Vertex("instruction:" + vertex.getStrID(), AbstractVertex.VertexType.INSTRUCTION);
				newV.setMinInstructionLine(vertex.getMinInstructionLine());
				newV.setExpanded(methods_expanded);
				newV.getMetaData().put(AbstractVertex.METADATA_METHOD_NAME, method);
				abstractGraph.addVertex(newV);
				id_to_abs_vertex.put(vertex.getStrID(), newV);
			}
		}
		// Add edges to the methodGraph.
		HashMap<String, Edge> edges = new HashMap<String,Edge>(); 
		for(int i = 0; i < graph.vertices.size(); i++)
		{
			Vertex vertex = graph.vertices.get(i);
			ArrayList<Vertex> neighbors = vertex.neighbors;
			Iterator<Vertex> it = neighbors.iterator();
			while(it.hasNext())
			{
				Vertex neighbor = it.next();
				String tempID = vertex.getStrID() + "--" + neighbor.getStrID();
				if(edges.containsKey(tempID))
				{
					System.out.println("WARNING: there exists two vertices with the same StrID: " + tempID);
				}
				else{
					AbstractVertex absVertex = id_to_abs_vertex.get(vertex.getStrID()); 
					AbstractVertex absNeigh = id_to_abs_vertex.get(neighbor.getStrID());
					if(absVertex!=absNeigh){	// We are not distinguishing recursive calls
						Edge e = new Edge(absVertex, absNeigh, Edge.EDGE_TYPE.EDGE_REGULAR);
						abstractGraph.addEdge(e);
						edges.put(tempID, e);
					}else{
						System.out.println("Warning: loop exists for vertex: " +  absVertex.getStrID());
					}
				}
			}
		}
		
		Collections.sort(graph.vertices);
		ArrayList<Edge> dummies = Graph.computeDummyEdges(graph.vertices.get(0));

		Iterator<Edge> itEdge = dummies.iterator();
		while(itEdge.hasNext())
		{
			Edge e = itEdge.next();
			AbstractVertex startOringal = e.getSourceVertex();
			AbstractVertex endOriginal = e.getDestVertex();
			
			AbstractVertex start = id_to_abs_vertex.get(startOringal.getStrID());
			AbstractVertex end = id_to_abs_vertex.get(endOriginal.getStrID());
			
			start.getSelfGraph().addEdge(new Edge(start,end,Edge.EDGE_TYPE.EDGE_DUMMY));
		}
		
		AbstractVertex root = new Vertex("root", AbstractVertex.VertexType.ROOT);
		root.setInnerGraph(abstractGraph);	
		//createChainVertices(root, CHAIN_LENGTH);
		return root;
	}

	static void createChainVertices(AbstractVertex parent, int k){
		Iterator<AbstractVertex> it = parent.getInnerGraph().getVertices().values().iterator();
		while(it.hasNext()){
			AbstractVertex absVertex = it.next();
			absVertex.vertexStatus = AbstractVertex.VertexStatus.WHITE;
			createChainVertices(absVertex, k);
		}
		
		if(create_chains){
			createChainVerticesFromVertex(parent.getInnerGraph().getRoot(), k);
		}
	
	}

	private static void createChainVerticesFromVertex(AbstractVertex root, int k) {
		if(root==null){return;}
		
		//System.out.println("collapseFromVertex");
		int i = 0;
		AbstractVertex currentVertex = root;
		ArrayList<AbstractVertex> chain = new ArrayList<AbstractVertex>();
		while(true){
			currentVertex.vertexStatus = AbstractVertex.VertexStatus.GRAY;
			Iterator<AbstractVertex> itChildren = currentVertex.getOutgoingAbstractNeighbors().iterator();
			ArrayList<AbstractVertex> grayChildren = new ArrayList<AbstractVertex>();
			while(itChildren.hasNext())
			{
				AbstractVertex child = itChildren.next();
				if (child.vertexStatus == AbstractVertex.VertexStatus.WHITE)
				{
					child.vertexStatus = AbstractVertex.VertexStatus.GRAY;
					grayChildren.add(child);
				}
			}
			
			
			ArrayList<AbstractVertex> copyOfIncoming = new ArrayList<AbstractVertex>(currentVertex.getIncomingAbstractNeighbors());
			copyOfIncoming.removeAll(grayChildren);
			
			
			ArrayList<AbstractVertex> copyOfOutgoing = new ArrayList<AbstractVertex>(currentVertex.getOutgoingAbstractNeighbors());
			copyOfOutgoing.removeAll(copyOfIncoming);
			
			
			/*System.out.println("Condition for vertex: " + currentVertex.getStrID());
			System.out.println("grayChildren: " + grayChildren.size());
			System.out.println("copyOfIncoming: " + copyOfIncoming.size());
			System.out.println("copyOfOutgoing" + copyOfOutgoing.size());*/
			Iterator<AbstractVertex> itVVV = copyOfOutgoing.iterator();
			/*while(itVVV.hasNext()){
				System.out.println("n: " + itVVV.next().getStrID());
			}*/
			
			
			
			//if(grayChildren.size()==1 && copyOfIncoming.size()==1){
//			if(currentVertex.getOutgoingAbstractNeighbors().size()==1 && copyOfIncoming.size()==1){
			if(grayChildren.size()==1 && copyOfIncoming.size()<=1 && copyOfOutgoing.size()==1){
			//System.out.println("Condition true for vertex: " + currentVertex.getStrID());
			//System.out.println("getOutgoingAbstractNeighbors: "+ currentVertex.getOutgoingAbstractNeighbors().size());
//			if(currentVertex.getOutgoingAbstractNeighbors().size()==1){
				AbstractVertex child =  grayChildren.get(0);
				chain.add(currentVertex);
				currentVertex = child;
			}else{
				// We also add the last vertex to the chain (so that we can reconstruct all edges)
//				if(grayChildren.size()==0){
					chain.add(currentVertex);
//			}
				
				/********************************************************************************/
				if(i>=k){
					System.out.println("CREATING CHAIN!!");
					AbstractVertex first = chain.get(0);
					AbstractVertex last = chain.get(chain.size()-1);
					
				
					
				
					System.out.println("CHAIN starts at: " + chain.get(0).getStrID());
					
					
					//Create the new vertex
					AbstractVertex chainVertex = new Vertex("Chain:" + chain.get(0).getStrID(), AbstractVertex.VertexType.CHAIN);
					chainVertex.setExpanded(chains_expanded);
					chainVertex.setMinInstructionLine(Integer.MAX_VALUE); // to be sure it won't be the root
					
					first.getSelfGraph().addVertex(chainVertex);
					first.getSelfGraph().addEdge(new Edge(first, chainVertex, Edge.EDGE_TYPE.EDGE_REGULAR));
					if(first.getSelfGraph().hasEdge(chain.get(1),first)){
						chainVertex.getSelfGraph().addEdge(new Edge(chainVertex,first, Edge.EDGE_TYPE.EDGE_REGULAR));
					}
					first.getSelfGraph().addEdge(new Edge(chainVertex, last, Edge.EDGE_TYPE.EDGE_REGULAR));
					if(last.getSelfGraph().hasEdge(last,chain.get(chain.size()-2))){
						chainVertex.getSelfGraph().addEdge(new Edge(last,chainVertex,  Edge.EDGE_TYPE.EDGE_REGULAR));
					}
				
					
					
					chain.remove(chain.size()-1);
					Iterator<AbstractVertex> chainIt = chain.iterator();
					chainIt.next(); // to start from the second node of the chain
					AbstractVertex previous = chainIt.next();
					chainVertex.getInnerGraph().addVertex(previous);
					while(chainIt.hasNext()){
						AbstractVertex next = chainIt.next();
						chainVertex.getInnerGraph().addVertex(next);
						chainVertex.getInnerGraph().addEdge(new Edge(previous, next, Edge.EDGE_TYPE.EDGE_REGULAR));
						if(first.getSelfGraph().hasEdge(next, previous)){
							chainVertex.getInnerGraph().addEdge(new Edge(next, previous, Edge.EDGE_TYPE.EDGE_REGULAR));	
						}
						previous = next;
					}
					
					
					previous = first;
					chainIt = chain.iterator();
					chainIt.next();
					while(chainIt.hasNext()){
						AbstractVertex next = chainIt.next();
						first.getSelfGraph().deleteEdge(previous,next);
						first.getSelfGraph().deleteEdge(next,previous);
						first.getSelfGraph().deleteVertex(next);
						previous = next;
					}
					last.getSelfGraph().deleteEdge(chain.get(chain.size()-1),last);
					last.getSelfGraph().deleteEdge(last,chain.get(chain.size()-1));
					
					
				}
				/********************************************************************************/
				itChildren = grayChildren.iterator();
				while(itChildren.hasNext()){
					createChainVerticesFromVertex(itChildren.next(), k);
				}
				break;
			}
		
			//System.out.println("i:" + i);
			i++;
		}
	

		}

		public static Color convertToFXColor(float redToGreenHue)
		{
			float sat = 1f;
			float brightness = 1f;
			java.awt.Color awtColor = getHSBColorT(redToGreenHue, sat, brightness);

			int r = awtColor.getRed();
			int g = awtColor.getGreen();
			int b = awtColor.getBlue();
			int a = awtColor.getAlpha();
			double opacity = a / 255.0 ;
			return javafx.scene.paint.Color.rgb(r, g, b, opacity);
		}

	public static java.awt.Color getHSBColorT(float H, float S, float B)
	{
		int rgb = java.awt.Color.HSBtoRGB(H, S, B);
		int red = (rgb >> 16) & 0xFF;
		int green = (rgb >> 8) & 0xFF;
		int blue = rgb & 0xFF;
		return new java.awt.Color(red, green, blue, Parameters.transparency);
	}
}

