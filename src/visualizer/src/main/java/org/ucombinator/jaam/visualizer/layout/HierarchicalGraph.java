package org.ucombinator.jaam.visualizer.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.ucombinator.jaam.visualizer.graph.Edge;

public class HierarchicalGraph
{
	private HashMap<String, AbstractLayoutVertex> vertices;
	private HashMap<String, Edge> edges;
	
	public HierarchicalGraph(HashMap<String, AbstractLayoutVertex> vertices, HashMap<String, Edge> edges)
	{
		super();
		this.vertices = vertices;
		this.edges = edges;
	}
	
	public HashMap<String, AbstractLayoutVertex> getVertices() {
		return vertices;
	}

	public void setVertices(HashMap<String, AbstractLayoutVertex> vertices) {
		this.vertices = vertices;
	}

	public HashMap<String, Edge> getEdges() {
		return this.edges;
	}

	public void setEdges(HashMap<String, Edge> edges) {
		this.edges = edges;
	}	
	
	public HierarchicalGraph()
	{
		vertices = new HashMap<>();
		edges = new HashMap<>();
	}

	public void addVertex(AbstractLayoutVertex vertex)
	{
		this.vertices.put(vertex.getStrID(), vertex);
		vertex.setSelfGraph(this);
	}
	
	public void addEdge(Edge edge)
	{
		edge.getSourceVertex().addOutgoingNeighbor(edge.getDestVertex());
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
		
		Iterator<AbstractLayoutVertex> abstractVertexIter = this.vertices.values().iterator();
		output.append("Vertices: ");
		while(abstractVertexIter.hasNext())
		{
			AbstractLayoutVertex v = abstractVertexIter.next();
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
		Iterator<AbstractLayoutVertex> it = this.getVertices().values().iterator();
		while(it.hasNext())
		{
			AbstractLayoutVertex v = it.next();
			System.out.println(v.getStrID() + ", x=" + v.getX() + ", y=" + v.getY());
		}
	}
	
	public AbstractLayoutVertex getRoot(){
		if(this.vertices.values().size()==0){
			//System.out.println("getRoot on empty graph");
			return null;
		}

		ArrayList<AbstractLayoutVertex> arrayList = new ArrayList<AbstractLayoutVertex>(this.vertices.values());
		Collections.sort(arrayList);
		return arrayList.get(0);
	}

	public void deleteEdge(AbstractLayoutVertex previous, AbstractLayoutVertex next) {
		this.edges.remove(Edge.createID(previous.getId(), next.getId()));
	}

	public boolean hasEdge(AbstractLayoutVertex first, AbstractLayoutVertex second) {
		return this.edges.containsKey(Edge.createID(first.getId(), second.getId()));
	}
}
