
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Collections;

public class Graph
{
	public int totalVertices;
	public int baseEdges;
	public ArrayList<Vertex> vertices;
	public ArrayList<MethodVertex> methodVertices;
	public ArrayList<Edge> edges;
    public ArrayList<String> tagsList;
    public ArrayList<Boolean> highlightedTags;
	public HashMap<String, Method> methods;
	public HashMap<String, Class> classes;
	protected AbstractVertex root;

	private double maxHeight; // required for collapse method
	public int maxIndex;

	public Graph()
	{
		this.totalVertices = 0;
		this.baseEdges = 0;
		this.vertices = new ArrayList<Vertex>();
		this.methodVertices = new ArrayList<MethodVertex>();
		this.edges = new ArrayList<Edge>();

		this.methods = new HashMap<String, Method>();
		this.classes = new HashMap<String, Class>();
		root = new Vertex(-1,-1);
		this.maxIndex = -1;

        this.tagsList = new ArrayList<String>();
        this.highlightedTags = new ArrayList<Boolean>();
	}
	
	public void setMaxHeight(double height)
	{
		this.maxHeight = height;
	}
	
	public double getMaxHeight()
	{
		return maxHeight;
	}

	public void addErrorState(int id)
	{
		this.addVertex(id, "ErrorState", "", "ErrorState", -1, false);
	}
	
	public void addVertex(int v, String methodName, String inst, String desc, int ind, boolean drawEdges)
	{
		Vertex ver = this.containsVertex(v);
		
		if(ver == null)
		{
			ver = new Vertex(v, totalVertices);
			this.vertices.add(ver);
			totalVertices++;
		}
		
		this.matchVertexToMethod(ver, methodName);
		ver.setDescription(desc);
		ver.setInstruction(inst);
		ver.setNameToInstruction();
		ver.jimpleIndex = ind;
		ver.drawEdges = drawEdges;
		
		if(ind > this.maxIndex)
			this.maxIndex = ind;
	}

	public void matchVertexToMethod(Vertex v, String methodName)
	{
		Method currMethod;
		if(this.methods.containsKey(methodName))
		{
			currMethod = this.methods.get(methodName);
		}
		else
		{
			currMethod = new Method(methodName);
			this.methods.put(methodName, currMethod);
		}

		v.setMethod(currMethod);
		currMethod.addVertex(v);
	}

	public void addEdge(int src, int dest)
	{
		Vertex vSrc, vDest;

		if(src != dest)
		{
			vSrc = this.containsVertex(src);
			if (vSrc == null)
			{
				totalVertices++;
				vSrc = new Vertex(src, this.vertices.size());
				this.vertices.add(vSrc);
			}

			vDest = this.containsVertex(dest);
			if (vDest == null)
			{
				totalVertices++;
				vDest = new Vertex(dest, this.vertices.size());
				this.vertices.add(vDest);
			}

			baseEdges++;
			vSrc.addNeighbor(vDest);
		}
	}

	public void matchClassesToCode(String basePath, ArrayList<File> javaFiles)
	{
		//Search through all the files, removing basePath from the beginning and .java from the end
		for(File file : javaFiles)
		{
			String path = file.getAbsolutePath();
			String className = path.substring(basePath.length(), path.length() - 5).replace('/', '.');

			if(this.classes.containsKey(className))
				this.classes.get(className).parseJavaFile(file.getAbsolutePath());
			else
				System.out.println("Cannot find class: " + className);
		}
	}

    public void addTag(int nodeId, String tag)
    {
        Vertex ver = this.containsVertex(nodeId);
        if(ver==null)
            return;
        
        int t = this.tagPresent(tag);
        
        if(t<0)
        {
            t = this.tagsList.size();
            this.tagsList.add(tag);
            this.highlightedTags.add(new Boolean(true));
        }
        ver.addTag(t);
    }

    public int tagPresent(String tag)
    {
        int i = 0;
        for(String t : this.tagsList)
        {
            if(t.equalsIgnoreCase(tag))
                return i;
            i++;
        }
        return -1;
    }

	public void clearHighlights()
	{
		for(Vertex v : this.vertices)
			v.clearAllHighlights();
		
		for(MethodVertex v : this.methodVertices)
			v.clearAllHighlights();
	}
	
	public void clearSelects()
	{
		for(Vertex v : this.vertices)
			v.clearAllSelect();
		
		for(MethodVertex v : this.methodVertices)
			v.clearAllSelect();
	}
	
	public HashSet<Method> collectHighlightedMethods()
	{
		HashSet<Method> highlightedMethods = new HashSet<Method>();
		
		for(Vertex v : this.vertices)
		{
			System.out.println("Vertex: " + v.id);
			System.out.println("Selected: " + v.isSelected());
            if(v.isHighlighted || v.isSelected)
            {
				highlightedMethods.add(v.getMethod());
				System.out.println("Adding method: " + v.getMethodName());
			}
		}
		
		for(MethodVertex v : this.methodVertices)
		{
			System.out.println("Method vertex: " + v.id);
			System.out.println("Selected: " + v.isSelected());
            if(v.isHighlighted || v.isSelected)
            {
				highlightedMethods.add(v.getMethod());
				System.out.println("Adding method: " + v.getMethodName());
			}
		}
		
		return highlightedMethods;
	}
	
	//After a vertex has been unhighlighted, we may have to remove some highlights.
	//But that's complicated, so it's simpler (albeit slightly inefficient) to erase
	//and redo everything.
	//TODO: Make this more efficient. Right now it could be O(n^2).
	public void redoCycleHighlights()
	{
		//First we remove all cycle highlights
		for(Vertex v : Main.graph.vertices)
			v.clearCycleHighlights();
		
		//Then we rehighlight the correct cycles
		for(Vertex v : Main.graph.vertices)
		{
			if(v.isHighlighted())
			{
				//System.out.println("Highlighting cycle for vertex " + v.getFullName());
				v.highlightCycles();
			}
		}
	}

    public boolean[] processQuery(String query)
    {
        int total = Main.graph.vertices.size() + Main.graph.methodVertices.size();
        boolean selected[] = new boolean[total];
        return selected;
    }
	
	public void searchNodes(StacFrame.searchType search, String searchStr)
	{
		this.clearHighlights();
		Parameters.bytecodeArea.clear();
		
		if(search == StacFrame.searchType.ID || search == StacFrame.searchType.OUT_OPEN || search == StacFrame.searchType.OUT_CLOSED
				|| search == StacFrame.searchType.IN_OPEN || search == StacFrame.searchType.IN_CLOSED || search == StacFrame.searchType.ROOT_PATH)
		{
			StringTokenizer token = new StringTokenizer(searchStr,", ");
			
			int id1, id2;
			String tok;
			
			while(token.hasMoreTokens())
			{
				tok = token.nextToken();
				if(tok.trim().equalsIgnoreCase(""))
					continue;
				if(tok.indexOf('-')==-1)
				{
					id1 = Integer.parseInt(tok.trim());
					if(search == StacFrame.searchType.ID)
						this.searchByID(id1);
					else if(search == StacFrame.searchType.OUT_OPEN || search == StacFrame.searchType.OUT_CLOSED)
						this.searchNeighbors(id1);
					else if(search == StacFrame.searchType.IN_OPEN || search == StacFrame.searchType.IN_CLOSED)
						this.searchIncomings(id1);
					else if(search == StacFrame.searchType.ROOT_PATH)
						this.searchPathToRoot(id1);
				}
				else
				{
					id1 = Integer.parseInt(tok.substring(0,tok.indexOf('-')).trim());
					id2 = Integer.parseInt(tok.substring(tok.lastIndexOf('-')+1).trim());
					if(search == StacFrame.searchType.ID)
						this.searchByID(id1, id2);
					else if(search == StacFrame.searchType.OUT_OPEN || search == StacFrame.searchType.OUT_CLOSED)
						this.searchNeighbors(id1, id2);
					else if(search == StacFrame.searchType.IN_OPEN || search == StacFrame.searchType.IN_CLOSED)
						this.searchIncomings(id1, id2);
					else if(search == StacFrame.searchType.ROOT_PATH)
						this.searchPathToRoot(id1, id2);
				}
			}
		}
        else if(search == StacFrame.searchType.TAG)
        {
            this.searchByTag(0);
        }
		else if(search == StacFrame.searchType.INSTRUCTION)
		{
			this.searchByInst(searchStr);
		}
		else if(search == StacFrame.searchType.METHOD)
		{
			this.searchByMethod(searchStr);
		}
		else if(search == StacFrame.searchType.ALL_LEAVES)
		{
			//System.out.println("Searching for leaves");
			searchLeaves();
		}
		else if(search == StacFrame.searchType.ALL_SOURCES)
		{
			//System.out.println("Searching for sources");
			searchSources();
		}
		
		//this.printHighlightTo();
		//this.printHighlightFrom();
	}
	
	public void printHighlightIncoming()
	{
		System.out.print("HighlightTo Nodes: ");
		for(int i=0; i<this.vertices.size(); i++)
			if(this.vertices.get(i).isIncomingHighlighted)
				System.out.print(this.vertices.get(i).getName()+", ");
		System.out.println("");
	}
	
	public void printHighlightOutgoing()
	{
		System.out.print("HighlightFrom Nodes: ");
		for(int i=0; i<this.vertices.size(); i++)
			if(this.vertices.get(i).isOutgoingHighlighted)
				System.out.print(this.vertices.get(i).getName()+", ");
		System.out.println("");
	}
	
	//Highlight vertices with no outgoing edges
	public void searchLeaves()
	{
		for(Vertex v : this.vertices)
		{
			//System.out.println(v.neighbors.size());
			if(v.neighbors.size() == 0)
				v.addHighlight(false, true, false, false);
		}
	}

	//Highlight vertices with no incoming edges
	public void searchSources()
	{
		for(Vertex v : this.vertices)
		{
			//System.out.println(v.incoming.size());
			if(v.incoming.size() == 0)
				v.addHighlight(false, true, false, false);
		}
	}
	
	//Highlight all neighbors of the vertex with the given id
	public void searchNeighbors(int id)
	{
		for(Vertex v : this.vertices)
		{
			if(v.id == id)
			{
				v.addHighlight(false, true, false, true);
				for(AbstractVertex w : v.neighbors)
					w.addHighlight(false, true, true, false);
			}
		}
	}
	
	//Highlight all neighbors of vertices with an id in the given range
	public void searchNeighbors(int startID, int endID)
	{
		for(Vertex v : this.vertices)
		{
			if(v.id >= startID && v.id <= endID)
			{
				v.addHighlight(false, true, false, true);
				for(AbstractVertex w : v.neighbors)
					w.addHighlight(false, true, true, false);
			}
		}
	}
	
	//Highlight all incoming vertices to the vertex with the given id
	public void searchIncomings(int id)
	{
		for(Vertex v : this.vertices)
		{
			if(v.id == id)
			{
				v.addHighlight(false, true, false, true);
				for(AbstractVertex w : v.incoming)
					w.addHighlight(false, true, true, false);
			}
		}
	}
	
	//Highlight all incoming vertices to a vertex with an id in the given range
	public void searchIncomings(int startID, int endID)
	{
		for(Vertex v : this.vertices)
		{
			if(v.id >= startID && v.id <= endID)
			{
				v.addHighlight(false, true, false, true);
				for(AbstractVertex w : v.incoming)
					w.addHighlight(false, true, true, false);
			}
		}
	}
	
	//Highlight the path from the vertex with the given id to the root of the tree
	public void searchPathToRoot(int id)
	{
		for(Vertex v : this.vertices)
		{
			if(v.id == id)
			{
				AbstractVertex ver = v;
				while(ver != this.root)
				{
					ver.addHighlight(false, true, true, true);
					ver = ver.parent;
				}
			}
		}
	}
	
	//Highlight the paths from vertices with an id in the given range to the root of the true
	public void searchPathToRoot(int startID, int endID)
	{
		for(Vertex v : this.vertices)
		{
			if(v.id >= startID && v.id <= endID)
			{
				AbstractVertex ver = v;
				while(ver != this.root)
				{
					ver.addHighlight(false, true, true, true);
					ver = ver.parent;
				}
			}
		}
	}
	
	//Highlight vertex with the given id
	public void searchByID(int id)
	{
		for(Vertex v : this.vertices)
		{
			if(v.id == id)
				v.addHighlight(false, true, true, true);
		}
	}
	
	//Highlight vertices with an id in the given range
	public void searchByID(int startID, int endID)
	{
		for(Vertex v : this.vertices)
		{
			if(v.id >= startID && v.id <= endID)
				v.addHighlight(false, true, true, true);
		}
	}
	
    //Highlight vertices with a given tag
    public void searchByTag(int t)
    {
        for(Vertex v : this.vertices)
        {
            if(v.hasTag(t))
                v.addHighlight(false, true, true, true);
        }
    }
    
	//Highlight vertices whose instruction contains the given search string
	public void searchByInst(String match)
	{
		for(Vertex v : this.vertices)
		{
			if(v.getInstruction().contains(match))
				v.addHighlight(false, true, true, true);
		}
	}
	
	//Highlight vertices that are in the given method
	public void searchByMethod(String match)
	{
		for(Vertex v : this.vertices)
		{
			if(v.getMethodName().contains(match))
				v.addHighlight(false, true, true, true);
		}
	}
	
	public Vertex containsVertex(int id)
	{
		for(Vertex v : this.vertices)
		{
			if(v.id == id)
				return v;
		}

		return null;
	}	
	
	//Computes the instruction lists for each method
	public void computeInstLists()
	{
		for(Entry<String, Method> entry : this.methods.entrySet())
		{
			Method method = entry.getValue();
			method.collectAndSortInstructions();
		}
	}
	
	public void mergeAllByMethod()
	{
		for(AbstractVertex v : this.root.children)
		{
			if(v.vertexType == AbstractVertex.VertexType.LINE)
			{
				Vertex w = (Vertex) v;
				w.mergeByMethod(null);
			}
		}
	}
    
    public void collectAllTags()
    {
        for(MethodVertex v : this.methodVertices)
            v.collectAllTagsFromChildren();
    }

	public void finalizeParentsForRootChildren()
	{
		AbstractVertex dest, src;
		boolean flag;
		for(int i = 0; i < this.root.children.size(); i++)
		{
			dest = this.root.children.get(i);
			for(int j = 0; j < dest.incoming.size(); j++)
			{
				src = dest.incoming.get(j);
				
				flag = false;
				AbstractVertex ver = src;
				while(ver != Main.graph.root)
				{
					if(ver == dest)
					{
						flag = true;
						break;
					}
					ver = ver.parent;
				}
				
				if(!flag) // if there is a loop, then try to find potential point to break the loop
				{
					dest.changeParent(src);
					i--;
					break;
				}
			}
		}
	}

	// Collapse all visible method vertices once.
	public void collapseOnce()
	{
		for(MethodVertex v : this.methodVertices)
		{
			if(v.mergeRoot.isVisible)
				v.collapse();
		}
	}

	//Expand all visible method vertices once.
	public void deCollapseOnce()
	{
		for(MethodVertex v : this.methodVertices)
		{
			if(v.isVisible)
				v.deCollapse();
		}
	}
	
	public void printAdjacencyList()
	{
		for(int i = 0; i < this.vertices.size(); i++)
		{
			Vertex v = this.vertices.get(i);
			System.out.print(v.id + ": ");

			for(AbstractVertex w : v.neighbors)
				System.out.print(w.id + " ");
			System.out.println("");
		}
	}
	
	public void printAllChildren()
	{
		for(int i = 0; i < this.vertices.size(); i++)
		{
			Vertex v = this.vertices.get(i);
			System.out.print(v.getName() + ": ");
			
			for(AbstractVertex w : v.children)
				System.out.print(w.getName() + " ");
			System.out.println("");
		}
	}
	
	public void printAllParents()
	{
		for(Vertex v: this.vertices)
			System.out.println(v.getName() + ": " + v.parent.getName());
	}
	
	public void printParent(AbstractVertex v)
	{
		System.out.println(v.getName() + ": " + v.parent.getName());
	}
	
	public void printCoordinates()
	{
		for(AbstractVertex w : this.vertices)
			System.out.println(w.getName() + ": " + w.location);
	}

	//Return the width of our graph in box units
	public double getWidth()
	{
		return root.location.width;
	}

	//Return the height of our graph in box units
	public double getHeight()
	{
		return root.location.height - 1;
	}
	
	public ArrayList<Edge> computeDummyEdges()
	{
		ArrayList<Edge> dummies = new ArrayList<Edge>();
		HashMap<String, Vertex> hash = new HashMap<String, Vertex>();
		Iterator<Vertex> it = this.vertices.iterator();
		while(it.hasNext())
		{
			it.next().vertexStatus = AbstractVertex.VertexStatus.UNVISITED;
		}

		// Visit first vertex of root method
		Collections.sort(Main.graph.vertices);
		System.out.println("Num of vertices: " + Main.graph.vertices.size());
		visit(Main.graph.vertices.get(0), hash, dummies);
		return dummies;
	}
	
	private void visit(Vertex root, HashMap<String, Vertex> hash, ArrayList<Edge> dummies)
	{
		//System.out.println("Root: " + root);
		Iterator<Vertex> it = root.neighbors.iterator();
		root.vertexStatus = AbstractVertex.VertexStatus.VISITED;
		//System.out.println("Vertex: " + root.getStrID() + " has been visited!");
		
		while(it.hasNext())
		{
			Vertex v  = it.next();
			if(v.vertexStatus == AbstractVertex.VertexStatus.UNVISITED){
				if(!v.getMethodName().equals(root.getMethodName()))
				{
					if(hash.containsKey(v.getMethodName())){
						dummies.add(new Edge(hash.get(v.getMethodName()), v, Edge.EDGE_TYPE.EDGE_DUMMY));
					}
				}

				hash.put(v.getMethodName(), v);
				visit(v,hash,dummies);
			}
		}
	}
}
