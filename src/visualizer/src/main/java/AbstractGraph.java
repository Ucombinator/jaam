import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class AbstractGraph {
	
	private HashMap<String,AbstractVertex> vertices;
	private HashMap<String,Edge> edges;
	
	public AbstractGraph(HashMap<String, AbstractVertex> vertices, HashMap<String, Edge> edges) {
		super();
		this.vertices = vertices;
		this.edges = edges;
	}

	
	public HashMap<String, AbstractVertex> getVertices() {
		return vertices;
	}

	public void setVertices(HashMap<String, AbstractVertex> vertices) {
		this.vertices = vertices;
	}

	public HashMap<String, Edge> getEdges() {
		return this.edges;
	}

	public void setEdges(HashMap<String, Edge> edges) {
		this.edges = edges;
	}	
	
	public AbstractGraph() {
		vertices = new HashMap<>();
		edges = new HashMap<>();
	}

	public void addVertex(AbstractVertex vertex) {
		this.vertices.put(vertex.getID(), vertex);
		vertex.setSelfGraph(this);
	}
	
	public void addVertices(HashSet<AbstractVertex> vertices) {
		Iterator<AbstractVertex> it =  vertices.iterator();
		while(it.hasNext()){
			this.addVertex(it.next());
		}
	}
	
	
	
	public void addEdge(Edge edge) {
		edge.getSourceVertex().addAbstractNeighbor(edge.getDestVertex());
		this.edges.put(edge.getID(), edge);
	}
	
	public String toString(){
		String output = "";
		if(this.getVertices().values().size()==0){
			return "";
		}
		
		Iterator<AbstractVertex> absIt = this.getVertices().values().iterator();
		output = "Vertices: ";
		while(absIt.hasNext()){
			AbstractVertex v = absIt.next();
			output += v.getLabel() + ", ";
			output += "\n";
			output += "Inner graph: \n";
			output += v.getInnerGraph().toString();
			output += "\n";
		}
		output+="\n";
		
		Iterator<Edge> absEd = this.getEdges().values().iterator();
		output+= "Edges: ";
		while(absEd.hasNext()){
			Edge e = absEd.next();
			output += "( "+e.getSourceVertex().getLabel()+"->" +e.getDestVertex().getLabel()+ " ), ";
		}
		output+="\n";
		return output;
	}
}
