
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class LayerFactory
{
	static HashMap<String, Vertex> id_to_vertex = new HashMap<String, Vertex>();
	static HashMap<String, AbstractVertex> id_to_abs_vertex = new HashMap<String, AbstractVertex>();
	
	static AbstractVertex get2layer(Graph graph)
	{
		AbstractGraph methodGraph = new AbstractGraph();
		
		/* We partion the vertex set of Main.graph into buckets corresponding to the methods*/
		HashMap<String, HashSet<Vertex>> buckets = new HashMap<String, HashSet<Vertex>>();
		for(int i = 0; i < graph.vertices.size(); i++)
		{
			Vertex vertex = graph.vertices.get(i);
			String method = vertex.getMethodName();
			if(!buckets.containsKey(method))
			{
				buckets.put(method, new HashSet<Vertex>());
			}
			buckets.get(method).add(vertex);
		}
		
		// Add a vertex for each method to the methodGraph.
		HashMap<String, AbstractVertex> methodVertices = new HashMap<>();
		Iterator<String> iter = buckets.keySet().iterator();
		while(iter.hasNext())
		{
			String method = iter.next();
			AbstractVertex vertex = new Vertex(method);
			methodVertices.put(method, vertex);
			methodGraph.addVertex(vertex);
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
				// This cast should be changed in the future once we get rid of the abstract class AbstractVertex
				Vertex neighbor = it.next();
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
		Iterator<AbstractVertex> itAbstract = methodGraph.getVertices().values().iterator();
		while(itAbstract.hasNext())
		{
			//Create inner-vertices of the inner-methods graph.
			AbstractVertex methodVertex = itAbstract.next();
			HashSet<Vertex> innerVertexSet = buckets.get(methodVertex.getLabel());
			AbstractGraph innerGraph = methodVertex.getInnerGraph();
			
			// Add vertices of the inner graph.
			Iterator<Vertex> it = innerVertexSet.iterator();
			HashMap<String,String> idMapping = new HashMap<>(); // first id is the Main.graph vertex id and the second id the New vertex id
			while(it.hasNext())
			{
				Vertex oldV = it.next();
				Vertex newV = new Vertex("instruction:" + oldV.getStrID());
				newV.setMinInstructionLine(oldV.id);

				id_to_vertex.put(oldV.getStrID(), oldV);
				id_to_abs_vertex.put(oldV.getStrID(), newV);
				idMapping.put(oldV.getStrID(), newV.getStrID());
				innerGraph.addVertex(newV);
			}
			
			// Add the edges of the inner graph.
			it = innerVertexSet.iterator();
			while(it.hasNext()){
				Vertex v = it.next();
				ArrayList<Vertex> neighbors = v.neighbors;
				Iterator<Vertex> itNeighbors = neighbors.iterator();
				while(itNeighbors.hasNext()){
					Vertex neighbor = itNeighbors.next();
					if(v.getMethodName().equals(neighbor.getMethodName()))
					{
						innerGraph.addEdge(
								new Edge(
										innerGraph.getVertices().get(
												idMapping.get(
												v.getStrID()))
										, innerGraph.getVertices().get(
								idMapping.get(
								neighbor.getStrID())),
										Edge.EDGE_TYPE.EDGE_REGULAR)
								);
					}
				}
			}
		}
		
		// Setting the Smallest_instruction_line of the method vertices
		itAbstract = methodGraph.getVertices().values().iterator();
		while(itAbstract.hasNext())
		{
			AbstractVertex methodVertex = itAbstract.next();
			Iterator<AbstractVertex> itInner = methodVertex.getInnerGraph().getVertices().values().iterator();
			while(itInner.hasNext())
			{
				if(methodVertex.getMinInstructionLine() == -1)
				{
					methodVertex.setMinInstructionLine(itInner.next().getMinInstructionLine());
				}
				else
				{
					methodVertex.setMinInstructionLine(Math.min(methodVertex.getMinInstructionLine(),
							itInner.next().getMinInstructionLine()));
				}
			}
		}
		
		ArrayList<Edge> dummies = graph.computeDummyEdges();
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
		
		AbstractVertex root = new Vertex("root");
		root.setInnerGraph(methodGraph);
		return root;
	}
}
