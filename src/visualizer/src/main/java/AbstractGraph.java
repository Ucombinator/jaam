
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
		edge.getSourceVertex().addAbstractNeighbor(edge.getDestVertex());
		this.edges.put(edge.getID(), edge);
	}
	
	public void deleteEdge(Edge edge)
	{
		edge.getSourceVertex().removeAbstractNeighbor(edge.getDestVertex());
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

	// Next three methods modified from "A New Algorithm for Identifying Loops in Decompilation"
	// TODO: Run this on each method graph separately
	/*public void identifyLoops()
	{
		//Each vertex is already initialized
		for(Vertex v : vertices)
		{
			if(!v.traversed)
				travLoopsDFS(v, 1);
		}

		/*for(Vertex v : vertices)
		{
			Vertex header = v.getLoopHeader();
			if(header != null)
				System.out.println(v.id + " --> " + v.getLoopHeader().id);
		}
	}

	public Vertex travLoopsDFS(Vertex v0, int dfsPathPos)
	{
		//System.out.println("Expanding vertex: " + Integer.toString(v0.id));
		v0.traversed = true;
		v0.dfsPathPos = dfsPathPos;
		for(Vertex ver : v0.neighbors)
		{
			Vertex v = ver;
			//System.out.println("New child: " + Integer.toString(v.id));
			if(!v.traversed)
			{
				//Case A: v is not yet traversed
				Vertex header = travLoopsDFS(v, dfsPathPos + 1);
				tagLoopHeader(v0, header);
			}
			else
			{
				if(v.dfsPathPos > 0)
				{
					//Case B: Mark b as a loop header
					tagLoopHeader(v0, v);
				}
				else if(v.getLoopHeader() == null)
				{
					//Case C: Do nothing
				}
				else
				{
					Vertex header = v.getLoopHeader();
					if(header.dfsPathPos > 0)
					{
						//Case D
						tagLoopHeader(v0, header);
					}
					else
					{
						//Case E: Re-entry
						while(header.getLoopHeader() != null)
						{
							header = header.getLoopHeader();
							if(header.dfsPathPos > 0)
							{
								tagLoopHeader(v0, header);
								break;
							}
						}
					}
				}
			}
		}

		v0.dfsPathPos = 0;
		return v0.getLoopHeader();
	}

	public void tagLoopHeader(Vertex v, Vertex header)
	{
		if(v == header || header == null)
			return;

		Vertex cur1 = v;
		Vertex cur2 = header;
		while(cur1.getLoopHeader() != null)
		{
			Vertex newHeader = cur1.getLoopHeader();
			if(newHeader == cur2)
				return;

			if(newHeader.dfsPathPos < cur2.dfsPathPos)
			{
				cur1.setLoopHeader(cur2);
				cur1 = cur2;
				cur2 = newHeader;
			}
			else
				cur1 = newHeader;
		}
		cur1.setLoopHeader(cur2);
	}

	public void calcLoopHeights()
	{
		//The loop height is -1 if it has not yet been calculated.
		//We do a breadth-first search of the graph, since the vertices might not be in order in our list.

		//We begin our search from the vertices that do not have a loop header.
		ArrayList<Vertex> toSearch = new ArrayList<Vertex>();
		ArrayList<Vertex> newSearch = new ArrayList<Vertex>();
		for(Vertex v: vertices)
		{
			Vertex header = v.getLoopHeader();
			if(header == null)
			{
				v.loopHeight = 0;
				toSearch.add(v);
			}
			else
			{
				header.addLoopChild(v);
			}
		}

		//This loop should terminate because every vertex has exactly one loop header, and there should not
		//be a loop in following header pointers. Each pass sets the height for the vertices at the next
		//level.
		int currLoopHeight = 1;
		while(toSearch.size() > 0)
		{
			for(Vertex v : toSearch)
			{
				ArrayList<Vertex> loopChildren = v.getLoopChildren();
				if(loopChildren.size() > 0)
				{
					v.loopHeight = currLoopHeight;
					for(Vertex w : loopChildren)
						newSearch.add(w);
				}
				else
					v.loopHeight = currLoopHeight - 1;
			}

			toSearch = newSearch;
			newSearch = new ArrayList<Vertex>();
			currLoopHeight++;
		}

		System.out.println("Loop heights found!");
		VizPanel.computeHues();
	}*/
	
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
