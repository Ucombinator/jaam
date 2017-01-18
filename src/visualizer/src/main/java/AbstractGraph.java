
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


public class AbstractGraph
{
	private HashMap<String,AbstractVertex> vertices;
	private HashMap<String,Edge> edges;
	
	public AbstractGraph(HashMap<String, AbstractVertex> vertices, HashMap<String, Edge> edges)
	{
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
	
	public AbstractGraph()
	{
		vertices = new HashMap<>();
		edges = new HashMap<>();
	}

	public void addVertex(AbstractVertex vertex)
	{
		this.vertices.put(vertex.getStrID(), vertex);
		vertex.setSelfGraph(this);
	}
	
	
	public void deleteVertex(AbstractVertex vertex)
	{
		this.vertices.remove(vertex.getStrID());
		vertex.setSelfGraph(null);
	}
	
	public void addVertices(HashSet<AbstractVertex> vertices)
	{
		Iterator<AbstractVertex> it =  vertices.iterator();
		while(it.hasNext())
			this.addVertex(it.next());
	}
	
	public void addEdge(Edge edge)
	{
		edge.getSourceVertex().addOutgoingAbstractNeighbor(edge.getDestVertex());
		this.edges.put(edge.getID(), edge);
	}
	
	public void deleteEdge(Edge edge)
	{
		edge.getSourceVertex().removeOutgoingAbstractNeighbor(edge.getDestVertex());
		this.edges.remove(edge.getID());
	}
	
	public String toString()
	{
		StringBuilder output = new StringBuilder();
		if(this.vertices.size() == 0)
			return "";
		
		Iterator<AbstractVertex> abstractVertexIter = this.vertices.values().iterator();
		output.append("Vertices: ");
		while(abstractVertexIter.hasNext())
		{
			AbstractVertex v = abstractVertexIter.next();
			output.append(v.getLabel() + ", ");
			output.append("\n");
			output.append("Inner graph: \n");
			output.append(v.getInnerGraph().toString());
			output.append("\n");
		}
		output.append("\n");
		
		Iterator<Edge> edgeIter = this.getEdges().values().iterator();
		output.append("Edges: ");
		while(edgeIter.hasNext()){
			Edge e = edgeIter.next();
			output.append("( "+e.getSourceVertex().getLabel()+"->" +e.getDestVertex().getLabel()+ " ), ");
		}
		output.append("\n");
		return output.toString();
	}
	
	public void printCoordinates(){
		Iterator<AbstractVertex> it = this.getVertices().values().iterator();
		while(it.hasNext())
		{
			AbstractVertex v = it.next();
			System.out.println(v.getStrID() + ", x=" + v.getX() + ", y=" + v.getY());
		}
	}
	
	public AbstractVertex getRoot(){
		if(this.vertices.values().size()==0){
			System.out.println("getRoot on empty graph");
			return null;
		}
		ArrayList<AbstractVertex> arrayList = new ArrayList<AbstractVertex>(this.vertices.values());
		Collections.sort(arrayList);
		return arrayList.get(0);
	}

	public void deleteEdge(AbstractVertex previous, AbstractVertex next) {
		this.edges.remove(Edge.createID(previous.id, next.id));
	}

	public boolean hasEdge(AbstractVertex first, AbstractVertex second) {
		return this.edges.containsKey(Edge.createID(first.id, second.id));
	}


	

}
