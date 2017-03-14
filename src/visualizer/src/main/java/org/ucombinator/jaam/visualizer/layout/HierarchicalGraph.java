package org.ucombinator.jaam.visualizer.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class HierarchicalGraph
{
	private HashMap<String, AbstractLayoutVertex> vertices;
	private HashMap<String, LayoutEdge> edges;
	
	public HierarchicalGraph(HashMap<String, AbstractLayoutVertex> vertices, HashMap<String, LayoutEdge> edges)
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

	public HashMap<String, LayoutEdge> getEdges() {
		return this.edges;
	}

	public void setEdges(HashMap<String, LayoutEdge> edges) {
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
	
	public void addEdge(LayoutEdge edge)
	{
		edge.getSourceVertex().addOutgoingNeighbor(edge.getDestVertex());
		this.edges.put(edge.getID(), edge);
	}
	
	public void deleteEdge(LayoutEdge edge)
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
		
		Iterator<LayoutEdge> edgeIter = this.getEdges().values().iterator();
		output.append("Edges: ");
		while(edgeIter.hasNext()){
			LayoutEdge e = edgeIter.next();
			output.append("( " + e.getSourceVertex().getLabel() + "->" + e.getDestVertex().getLabel() + " ), ");
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
		this.edges.remove(LayoutEdge.createID(previous.getId(), next.getId()));
	}

	public boolean hasEdge(AbstractLayoutVertex first, AbstractLayoutVertex second) {
		return this.edges.containsKey(LayoutEdge.createID(first.getId(), second.getId()));
	}

	public static ArrayList<LayoutEdge> computeDummyEdges(LayoutInstructionVertex root)
	{
		ArrayList<LayoutEdge> dummies = new ArrayList<LayoutEdge>();

		// Visit first vertex of root method
		root.cleanAll();
		visit(root, new HashMap<String, AbstractLayoutVertex>(), dummies);
		return dummies;
	}

	private static void visit(LayoutInstructionVertex root, HashMap<String, AbstractLayoutVertex> hash, ArrayList<LayoutEdge> dummies)
	{
		//System.out.println("Root: " + root);
		Iterator<AbstractLayoutVertex> it = root.getOutgoingNeighbors().iterator();
		root.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);
		//System.out.println("Vertex: " + root.getStrID() + " has been visited!");

		while(it.hasNext())
		{
			// TODO: Check if this is actually valid.
			LayoutInstructionVertex v  = (LayoutInstructionVertex) it.next();
			String vMethod = v.getInstruction().getMethodName();
			String rootMethod = root.getInstruction().getMethodName();
			if(v.getVertexStatus() == AbstractLayoutVertex.VertexStatus.WHITE){
				if(!vMethod.equals(rootMethod))
				{
					if(hash.containsKey(vMethod)){
						dummies.add(new LayoutEdge(hash.get(vMethod), v, LayoutEdge.EDGE_TYPE.EDGE_DUMMY));
					}
				}

				hash.put(vMethod, v);
				visit(v,hash,dummies);
			}
		}
	}
}
