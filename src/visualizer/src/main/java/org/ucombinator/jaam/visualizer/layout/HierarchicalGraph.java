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

	public HierarchicalGraph()
	{
		this(new HashMap<String, AbstractLayoutVertex>(), new HashMap<String, LayoutEdge>());
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

	public void addVertex(AbstractLayoutVertex vertex)
	{
		this.vertices.put(vertex.getStrID(), vertex);
		vertex.setSelfGraph(this);
	}

	public void deleteVertex(AbstractLayoutVertex vertex)
	{
		this.vertices.remove(vertex.getStrID());
		vertex.setSelfGraph(null);
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

	public static ArrayList<LayoutEdge> computeDummyEdges(AbstractLayoutVertex root)
	{
		ArrayList<LayoutEdge> dummies = new ArrayList<LayoutEdge>();

		// Visit first vertex of root method
		root.cleanAll();
		visit(root, new HashMap<String, AbstractLayoutVertex>(), dummies);
		return dummies;
	}

	private static void visit(AbstractLayoutVertex root, HashMap<String, AbstractLayoutVertex> hash, ArrayList<LayoutEdge> dummies)
	{
		//System.out.println("Root: " + root);
		Iterator<AbstractLayoutVertex> it = root.getOutgoingNeighbors().iterator();
		root.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);
		String rootMethod;

		if (root instanceof LayoutInstructionVertex)
			rootMethod = ((LayoutInstructionVertex) root).getInstruction().getMethodName();
		else // root instanceof LayoutMethodVertex
			rootMethod = ((LayoutMethodVertex) root).getMethodName();

		while(it.hasNext())
		{
			AbstractLayoutVertex absVertex = it.next();
			String nextVertexMethod;

			if (absVertex instanceof LayoutInstructionVertex)
				nextVertexMethod = ((LayoutInstructionVertex) absVertex).getInstruction().getMethodName();
			else
				nextVertexMethod = ((LayoutMethodVertex) absVertex).getMethodName();

			if(absVertex.getVertexStatus() == AbstractLayoutVertex.VertexStatus.WHITE){
				if(!nextVertexMethod.equals(rootMethod))
				{
					if(hash.containsKey(nextVertexMethod)){
						dummies.add(new LayoutEdge(hash.get(nextVertexMethod), absVertex, LayoutEdge.EDGE_TYPE.EDGE_DUMMY));
					}
				}

				hash.put(nextVertexMethod, absVertex);
				visit(absVertex, hash, dummies);
			}
		}
	}
}
