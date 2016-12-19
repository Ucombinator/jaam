
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class LayerFactory
{
	static AbstractVertex get2layer(Graph graph)
	{
		AbstractGraph methodGraph = new AbstractGraph();

		// Partition the vertex set of Main.graph into buckets corresponding to the methods.
		HashMap<String, HashSet<Vertex>> buckets = new HashMap<String, HashSet<Vertex>>();
		for(int i = 0; i < graph.vertices.size(); i++){
			Vertex vertex = graph.vertices.get(i);
			String method = vertex.getMethodName();
			if(!buckets.containsKey(method)){
				buckets.put(method, new HashSet<Vertex>());
			}
			buckets.get(method).add(vertex);
		}
		
		// Add a vertex for each method to the methodGraph.
		HashMap<String, AbstractVertex> methodVertices = new HashMap<>();
		Iterator<String> iter = buckets.keySet().iterator();
		while(iter.hasNext()){
			String method = iter.next();
			AbstractVertex vertex = new Vertex(method);
			methodVertices.put(method, vertex);
			methodGraph.addVertex(vertex);
		}

		// Add edges to the methodGraph.
		HashMap<String, Edge> edges = new HashMap<String,Edge>(); 
		for(int i = 0; i < graph.vertices.size(); i++){
			Vertex vertex = graph.vertices.get(i);
			ArrayList<Vertex> neighbors = vertex.neighbors;
			Iterator<Vertex> it = neighbors.iterator();
			while(it.hasNext()){
				// This cast should be changed in the future once we get ride of the abstract class AbstractVertex
				Vertex neigh = it.next();
				String tempID = vertex.getMethodName() +"--"+neigh.getMethodName();
				if(!edges.containsKey(tempID)){
					AbstractVertex absVertex = methodVertices.get(vertex.getMethodName()); 
					AbstractVertex absNeigh = methodVertices.get(neigh.getMethodName());
					Edge e = new Edge(absVertex, absNeigh, Edge.EDGE_TYPE.EDGE_REGULAR);
					edges.put(tempID, e);
					methodGraph.addEdge(e);
				}
			}
		}
		
		/* We are creating an inner graph for each method vertex*/
		Iterator<AbstractVertex> absIt = methodGraph.getVertices().values().iterator();
		// Creation of the inner-method graph
		while(absIt.hasNext()){
			//Create inner-vertices of the inner-methods graph
			AbstractVertex methodVertex = absIt.next();
			HashSet<Vertex> innerVertexSet = buckets.get(methodVertex.getLabel());
			AbstractGraph innerGraph = methodVertex.getInnerGraph();
			
			/* We are adding the vertices of the inner graph*/
			Iterator<Vertex> it = innerVertexSet.iterator();
			HashMap<String,String> idMapping = new HashMap<>(); // first id is the Main.graph vertex id and the second id the New vertex id
			while(it.hasNext()){
			Vertex oldV = it.next();
			Vertex newV = new Vertex("instruction:"+oldV.getID());
			newV.setMinInstructionLine(oldV.id);
			idMapping.put(oldV.getID(), newV.getID());
				innerGraph.addVertex(newV);
			}
			
			/* We are adding the edges of the inner graph*/
			it = innerVertexSet.iterator();
			while(it.hasNext()){
				Vertex v = it.next();
				ArrayList<Vertex> neighbors = v.neighbors;
				Iterator<Vertex> itNeigh = neighbors.iterator(); 
				while(itNeigh.hasNext()){
					Vertex neigh = itNeigh.next();
					if(v.getMethodName().equals(neigh.getMethodName())){						
						innerGraph.addEdge(
								new Edge(
										innerGraph.getVertices().get(
												idMapping.get(
												v.getID()))
										, innerGraph.getVertices().get(
								idMapping.get(
								neigh.getID())),
										Edge.EDGE_TYPE.EDGE_REGULAR)
								);
					}
				}
			}
			
		}
		
		// Setting the Smallest_instruction_line of the method vertices
		absIt = methodGraph.getVertices().values().iterator();
		while(absIt.hasNext()){
			AbstractVertex methodVertex = absIt.next();
			Iterator<AbstractVertex> innerIt = methodVertex.getInnerGraph().getVertices().values().iterator();
			while(innerIt.hasNext()){
				if(methodVertex.getMinInstructionLine()==-1){
					methodVertex.setMinInstructionLine(innerIt.next().getMinInstructionLine());
				}else{
					methodVertex.setMinInstructionLine(Math.min(methodVertex.getMinInstructionLine(),innerIt.next().getMinInstructionLine()));
				}
			}
		}
		
		AbstractVertex root = new Vertex("root");
		root.setInnerGraph(methodGraph);
		return root;
	}

}
