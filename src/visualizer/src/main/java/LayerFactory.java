
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class LayerFactory
{
	static HashMap<String, Vertex> idToVertex = new HashMap<String, Vertex>();
	static HashMap<String, AbstractVertex> idToAbsVertex = new HashMap<String, AbstractVertex>();
	
	static AbstractVertex get2layer(Graph graph)
	{
		AbstractGraph methodGraph = new AbstractGraph();
		
		// We partition the vertex set of Main.graph into buckets corresponding to the methods.
		HashMap<String, HashSet<Vertex>> buckets = new HashMap<String, HashSet<Vertex>>();
		HashMap<String, Method> methods = new HashMap<String, Method>();
		for(int i = 0; i < graph.vertices.size(); i++)
		{
			Vertex vertex = graph.vertices.get(i);
			String methodName = vertex.getMethodName();
			if(!buckets.containsKey(methodName))
			{
				buckets.put(methodName, new HashSet<Vertex>());
			}
			buckets.get(methodName).add(vertex);

			if(!methods.containsKey(methodName))
				methods.put(methodName, vertex.getMethod());
		}
		
		// Add a vertex to the method graph for each method.
		HashMap<String, AbstractVertex> methodVertices = new HashMap<>();
		for(String methodName: buckets.keySet())
		{
			// Create vertex for method
			AbstractVertex methodVertex = new MethodVertex(methods.get(methodName), true);
			methodVertices.put(methodName, methodVertex);
			methodGraph.addVertex(methodVertex);
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
					
					if(absVertex != absNeigh)
					{
						// We are not distinguishing recursive calls
						Edge e = new Edge(absVertex, absNeigh, Edge.EDGE_TYPE.EDGE_REGULAR);
						edges.put(tempID, e);
						methodGraph.addEdge(e);
					}
				}
			}
		}
		
		// Create inner graph for each method vertex.
		for(AbstractVertex methodVertex: methodGraph.getVertices().values())
		{
			//Create inner-vertices of the inner-methods graph.
			HashSet<Vertex> innerVertexSet = buckets.get(methodVertex.getName());
			System.out.println("Number of vertices: " + innerVertexSet.size());
			AbstractGraph innerGraph = methodVertex.getInnerGraph();
			
			// Add vertices of the inner graph.
			HashMap<String, String> idMapping = new HashMap<>(); // first id is the Main.graph vertex id and the second id the New vertex id
			for(Vertex oldV : innerVertexSet)
			{
				Vertex newV = new Vertex("instruction:" + oldV.getStrID());
				newV.setMinInstructionLine(oldV.id);

				idToVertex.put(oldV.getStrID(), oldV);
				idToAbsVertex.put(oldV.getStrID(), newV);
				idMapping.put(oldV.getStrID(), newV.getStrID());
				innerGraph.addVertex(newV);

				newV.setMethod(oldV.getMethod());
			}
			
			// Add the edges of the inner graph.
			for(Vertex v : innerVertexSet)
			{
				for(Vertex neighbor: v.neighbors)
				{
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
		
		// Setting the minimum instruction line of the method vertices
		for(AbstractVertex methodVertex: methodGraph.getVertices().values())
		{
			for(AbstractVertex vertex : methodVertex.getInnerGraph().getVertices().values())
			{
				if(methodVertex.getMinInstructionLine() == -1)
				{
					methodVertex.setMinInstructionLine(vertex.getMinInstructionLine());
				}
				else
				{
					methodVertex.setMinInstructionLine(Math.min(methodVertex.getMinInstructionLine(),
							vertex.getMinInstructionLine()));
				}
			}
		}

		for(Edge e : graph.computeDummyEdges())
		{
			AbstractVertex startOriginal = e.getSourceVertex();
			AbstractVertex endOriginal = e.getDestVertex();
			
			AbstractVertex start = idToAbsVertex.get(startOriginal.getStrID());
			AbstractVertex end = idToAbsVertex.get(endOriginal.getStrID());
			
			start.getSelfGraph().addEdge(new Edge(start,end,Edge.EDGE_TYPE.EDGE_DUMMY));
		}
		
		AbstractVertex root = new Vertex("root");
		root.setInnerGraph(methodGraph);
		return root;
	}
}
