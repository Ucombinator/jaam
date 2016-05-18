
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Graph
{
	public ArrayList<Vertex> vertices;
	public ArrayList<MethodVertex> methodVertices;
	public ArrayList<MethodPathVertex> methodPathVertices;
	public HashMap<String, Method> methods;
	public AbstractVertex root;
	public View currWindow;
	static int numHotkeys = 10;
	public View[] hotkeyedViews;
	private double maxH; // required for collapse method
	public int maxIndex;
	
	
	public Graph()
	{
		this.vertices = new ArrayList<Vertex>();
		this.methodVertices = new ArrayList<MethodVertex>();
		this.methodPathVertices = new ArrayList<MethodPathVertex>();
		this.methods = new HashMap<String, Method>();
		root = new Vertex(-1,-1);
		this.maxIndex = -1;
		
		currWindow = new View(true);
		hotkeyedViews = new View[numHotkeys];
		for(int i = 0; i < numHotkeys; i++)
			hotkeyedViews[i] = new View(false);
		
		this.resetZoom();
	}
	
	public void setMaxHeight(double height)
	{
		this.maxH = height;
	}
	
	public double getMaxHeight()
	{
		return maxH;
	}
	
	public void increaseZoom(double factor)
	{
		double centerX = (this.currWindow.left+this.currWindow.right)/2;
		double centerY = (this.currWindow.top+this.currWindow.bottom)/2;
		this.zoomNPan(centerX, centerY, factor);
	}
	
	public void resetZoom()
	{
		View newWindow = new View(true);
		currWindow.setNext(newWindow);
		newWindow.setPrev(currWindow);
		currWindow = newWindow;	
		this.computeShowViz();
	}
	
	public void loadPreviousView()
	{
		currWindow = currWindow.getPrev();
		this.computeShowViz();
	}	
	
	public void restoreNewView()
	{
		currWindow = currWindow.getNext();
		this.computeShowViz();
	}	
	
	public void shiftView(int hor, int ver)
	{
		View newWindow = new View(currWindow);
		newWindow.shiftView(hor, ver);
		newWindow.setPrev(currWindow);
		currWindow.setNext(newWindow);
		currWindow = newWindow;
		this.computeShowViz();
	}
	
	public void addHotkeyedView(int digit)
	{
		hotkeyedViews[digit] = new View(currWindow);
	}

	public void loadHotkeyedView(int digit)
	{
		if(hotkeyedViews[digit].initialized)
			zoomNPan(hotkeyedViews[digit]);
	}
	
	public void zoomNPan(double centerX, double centerY, double factor)
	{
		View newWindow = new View(currWindow);
		newWindow.setPrev(currWindow);
		currWindow.setNext(newWindow);
		newWindow.zoomNPan(centerX, centerY, factor);
		currWindow = newWindow;
		this.computeShowViz();
	}
	
	public void zoomNPan(double left, double right, double top, double bottom)
	{
		View newWindow = new View(left, right, top, bottom);
		newWindow.setPrev(currWindow);
		currWindow.setNext(newWindow);
		currWindow = newWindow;
		this.computeShowViz();
	}
	
	public void zoomNPan(View v)
	{
		View newWindow = new View(v);
		newWindow.setPrev(currWindow);
		currWindow.setNext(newWindow);
		currWindow = newWindow;
		this.computeShowViz();
	}
	
	//TODO: Figure out why some vertices aren't being selected...
	public void selectVertices(double x1, double x2, double y1, double y2)
	{
		System.out.println("Selecting vertices: " + x1 + ", " + y1 + ", " + x2 + ", " + y2);
		for(Vertex v : this.vertices)
		{
			System.out.println(v.x + ", " + v.y);
			v.clearAllHighlights();
			if(v.isVisible && x1 < v.x && v.x < x2 && y1 < v.y && v.y < y2)
			{
				System.out.println("Selecting vertex: " + v.getName());
				v.addHighlight(true, true, true);
			}
		}
		
		for(MethodVertex v : this.methodVertices)
		{
			System.out.println(v.x + ", " + v.y);
			v.clearAllHighlights();
			if(v.isVisible && x1 < v.x && v.x < x2 && y1 < v.y && v.y < y2)
			{
				System.out.println("Selecting vertex: " + v.getName());
				v.addHighlight(true, true, true);
			}
		}
		
		for(MethodPathVertex v: this.methodPathVertices)
		{
			System.out.println(v.x + ", " + v.y);
			v.clearAllHighlights();
			if(v.isVisible && x1 < v.x && v.x < x2 && y1 < v.y && v.y < y2)
			{
				System.out.println("Selecting vertex: " + v.getName());
				v.addHighlight(true, true, true);
			}
		}
	}
	
	public void computeShowViz()
	{
		for(Vertex v : this.vertices)
			v.showViz = false;

		for(MethodVertex v : this.methodVertices)
			v.showViz = false;
		
		for(MethodPathVertex v : this.methodPathVertices)
			v.showViz = false;
		
		AbstractVertex ver, v, nbr;
		
		for(int i = 0; i < this.vertices.size(); i++)
		{
			ver = this.vertices.get(i);
			
			v = ver;
			while(!v.isVisible)
				v = v.getMergeParent();
			
			for(int j=0; j< ver.neighbors.size(); j++)
			{
				nbr = ver.neighbors.get(j);
				
				while(!nbr.isVisible)
					nbr = nbr.getMergeParent();
				
				if(v == nbr)
					continue;

								
				if(v.x >= this.currWindow.left*this.getWidth() && v.x <= this.currWindow.right*this.getWidth()
						&& v.y >= this.currWindow.top*this.getHeight() && v.y <= this.currWindow.bottom*this.getHeight())
				{
					v.showViz = true;
					nbr.showViz = true;
				}
				else if(nbr.x >= this.currWindow.left*this.getWidth() && nbr.x <= this.currWindow.right*this.getWidth()
						&& nbr.y >= this.currWindow.top*this.getHeight() && nbr.y <= this.currWindow.bottom*this.getHeight())
				{
					v.showViz = true;
					nbr.showViz = true;
				}
			}
		}
	}

/*
	
	public void extractGraph(String file)
	{
		String line;
		StringTokenizer token;
		int src, dest;
		Vertex vSrc, vDest;
		
		try
		{
//			System.out.println(file);
			BufferedReader r = new BufferedReader(new FileReader(file));
			
//			if(r == null)
//				System.out.println("null file");
			
			while(true)
			{
				line = r.readLine();
				if(line==null)
					break;
				
				if(line.contains(" -> "))
				{
					token = new StringTokenizer(line);
					if(token.countTokens()!=3)
						continue;
					src = Integer.parseInt(token.nextToken());
					token.nextToken();
					dest = Integer.parseInt(token.nextToken());
					vSrc = this.containsVertex(src);
					if(vSrc==null)
					{
						vSrc=new Vertex(src, this.vertices.size());
						this.vertices.add(vSrc);
					}
					vDest = this.containsVertex(dest);
					if(vDest==null)
					{
						vDest=new Vertex(dest, this.vertices.size());
						this.vertices.add(vDest);
					}
					vSrc.addNeighbor(vDest);
				}
			}
			r.close();
			
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}

	}
	
//*/	
	
	public AbstractVertex getVertexAtCoordinate(double x, double y)
	{
		for(Vertex v : vertices)
		{
			if(v.isAtCoordinate(x, y))
				return v;
		}
		
		for(MethodVertex v : this.methodVertices)
		{
			if(v.isAtCoordinate(x, y))
				return v;
		}
		
		for(MethodPathVertex v : this.methodPathVertices)
		{
			if(v.isAtCoordinate(x, y))
				return v;
		}
		
		return null;
	}
	
 	public void addVertex(String des)
	{
		String name = des.substring(0, des.indexOf(':'));
		int id = Integer.parseInt(name);
		Vertex ver = this.containsVertex(id);		
		
		if(ver==null)
		{
			ver = new Vertex(id, this.vertices.size());
			this.vertices.add(ver);
		}
		
		String desc = des.substring(name.length()+1);
		ver.setDescription(desc);
		
		Pattern methodPattern = Pattern.compile("(\"stmt\":\\{.*?\"sootMethod\":\\{(.*?)\\}.*?\\})", Pattern.DOTALL);
		Matcher methodMatcher = methodPattern.matcher(desc);
		if(methodMatcher.find())
		{
			String methodName = methodMatcher.group(2);
			this.matchVertexToMethod(ver, methodName);
		}
	}
	
	public void addVertex(int v, String methodName, String inst)
	{
		Vertex ver = this.containsVertex(v);
		this.matchVertexToMethod(ver, methodName);
		
		if(ver==null)
		{
			ver = new Vertex(v,this.vertices.size());
			this.vertices.add(ver);
		}
		ver.setDescription("\"method\":" + methodName + "\n\"instruction\":" + inst);
		ver.setInstruction(inst);
	}
	
	public void addVertex(int v, String methodName, String inst, String desc)
	{
		Vertex ver = this.containsVertex(v);
		this.matchVertexToMethod(ver, methodName);
		
		if(ver==null)
		{
			ver = new Vertex(v,this.vertices.size());
			this.vertices.add(ver);
		}
		ver.setDescription(desc);
		ver.setInstruction(inst);
		ver.setNameToInstruction();
	}
	
	public void addVertex(int v, String methodName, String inst, String desc, int ind, int line)
	{
		Vertex ver = this.containsVertex(v);
		
		if(ver == null)
		{
			ver = new Vertex(v, this.vertices.size());
			this.vertices.add(ver);
		}
		
		this.matchVertexToMethod(ver, methodName);
		ver.setDescription(desc);
		ver.setInstruction(inst);
		ver.setNameToInstruction();
		ver.jimpleIndex = ind;
		ver.jimpleLine = line;
		
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
	
	public void addEdge(String line)
	{
		int src, dest;
		Vertex vSrc, vDest;
		
		StringTokenizer token = new StringTokenizer(line);
		
		if(token.countTokens() != 3)
			return;
		
		src = Integer.parseInt(token.nextToken());
		token.nextToken();
		dest = Integer.parseInt(token.nextToken());
		if(src == dest)
		{
			System.out.println("ERROR! Cannot add self-loop.");
			return;
		}
		
		vSrc = this.containsVertex(src);
		if(vSrc==null)
		{
			vSrc=new Vertex(src, this.vertices.size());
			this.vertices.add(vSrc);
		}
		vDest = this.containsVertex(dest);
		if(vDest==null)
		{
			vDest=new Vertex(dest, this.vertices.size());
			this.vertices.add(vDest);
		}
		vSrc.addNeighbor(vDest);

	}
	
	public void clearHighlights()
	{
		for(Vertex v : this.vertices)
			v.clearAllHighlights();
		
		for(MethodVertex v : this.methodVertices)
			v.clearAllHighlights();

		for(MethodPathVertex v : this.methodPathVertices)
			v.clearAllHighlights();
	}
	
	public HashSet<Method> collectHighlightedMethods()
	{
		HashSet<Method> highlightedMethods = new HashSet<Method>();
		
		for(Vertex v : this.vertices)
		{
			if(v.isHighlighted)
				highlightedMethods.add(v.getMethod());
		}
		
		for(MethodVertex v : this.methodVertices)
		{
			if(v.isHighlighted)
				highlightedMethods.add(v.getMethod());
		}
		
		for(MethodPathVertex v : this.methodPathVertices)
		{
			if(v.isHighlighted)
			{
				for(MethodVertex w : v.getMergeChildren())
					highlightedMethods.add(w.getMethod());
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
		for(Vertex v : StacViz.graph.vertices)
			v.clearCycleHighlights();
		
		//Then we rehighlight the correct cycles
		for(Vertex v : StacViz.graph.vertices)
		{
			if(v.isHighlighted())
			{
				//System.out.println("Highlighting cycle for vertex " + v.getName());
				v.highlightCycles();
			}
		}
	}
	
	public void searchNodes(StacFrame.searchType search, String searchStr)
	{
		this.clearHighlights();
		Parameters.leftArea.clear();
		
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
				v.addHighlight(true, false, false);
		}
	}

	//Highlight vertices with no incoming edges
	public void searchSources()
	{
		for(Vertex v : this.vertices)
		{
			//System.out.println(v.incoming.size());
			if(v.incoming.size() == 0)
				v.addHighlight(true, false, false);
		}
	}
	
	//Highlight all neighbors of the vertex with the given id
	public void searchNeighbors(int id)
	{
		for(Vertex v : this.vertices)
		{
			if(v.id == id)
			{
				v.addHighlight(true, false, true);
				for(AbstractVertex w : v.neighbors)
					w.addHighlight(true, true, false);
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
				v.addHighlight(true, false, true);
				for(AbstractVertex w : v.neighbors)
					w.addHighlight(true, true, false);
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
				v.addHighlight(true, false, true);
				for(AbstractVertex w : v.incoming)
					w.addHighlight(true, true, false);
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
				v.addHighlight(true, false, true);
				for(AbstractVertex w : v.incoming)
					w.addHighlight(true, true, false);
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
					ver.addHighlight(true, true, true);
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
					ver.addHighlight(true, true, true);
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
				v.addHighlight(true, true, true);
		}
	}
	
	//Highlight vertices with an id in the given range
	public void searchByID(int startID, int endID)
	{
		for(Vertex v : this.vertices)
		{
			if(v.id >= startID && v.id <= endID)
				v.addHighlight(true, true, true);
		}
	}
	
	//Highlight vertices whose instruction contains the given search string
	public void searchByInst(String match)
	{
		for(Vertex v : this.vertices)
		{
			if(v.getInstruction().contains(match))
				v.addHighlight(true, true, true);
		}
	}
	
	//Highlight vertices that are in the given method
	public void searchByMethod(String match)
	{
		for(Vertex v : this.vertices)
		{
			if(v.getMethodName().contains(match))
				v.addHighlight(true, true, true);
		}
	}

	public int numVertices()
	{
		return this.vertices.size();
	}
	
	public Vertex containsVertex(int id)
	{
		for(int i=0; i<this.vertices.size(); i++)
		{
			if(this.vertices.get(i).id==id)
				return this.vertices.get(i);
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
	
	public void mergeMoreMethods()
	{
		for(int i = 0; i < this.methodVertices.size(); i++)
		{
			MethodVertex method1 = this.methodVertices.get(i);
			for(int j = i + 1; j < this.methodVertices.size(); j++)
			{
				MethodVertex method2 = this.methodVertices.get(j);
				if(method2.name.equals(method1.name))
				{
					MethodPathVertex root1 = (MethodPathVertex) method1.mergeRoot;
					MethodPathVertex root2 = (MethodPathVertex) method2.mergeRoot;
					
					AbstractVertex childVer = null, parentVer = null;
					
					for(int k = 0; k < root2.incoming.size(); k++)
					{
						if(root2.incoming.get(k).getMergeParent() == method1)
						{
							System.out.println("potential merging: " + root2.getName() + " to " + root2.incoming.get(k).getName());
							AbstractVertex ver = root2;
							while(ver != this.root && ver.getMergeParent() != method1)
							{
								ver = ver.parent;
							}
							
							if(ver != this.root)
							{
								childVer = root2;
								parentVer = root2.incoming.get(k);
								break;
							}
						}
					}

					if(parentVer != null) // root2 can be merged
					{
						System.out.println("going to merge " + j + " to " + i);
						childVer.changeParent(parentVer);
						System.out.println("change parent done");
						for(int k = 0; k < method2.mergeChildren.size(); k++)
						{
							method2.mergeChildren.get(k).mergeParent = method1;
							method1.mergeChildren.add(method2.mergeChildren.get(k));
						}
						System.out.println("merging done");
						this.methodVertices.remove(j);
						j--;
						continue;
					}
					for(int k=0; k<root1.incoming.size(); k++)
					{
						if(root1.incoming.get(k).getMergeParent() == method2)
						{
							System.out.println("potential merging: "+root1.getName()+" to "+root1.incoming.get(k).getName());
							AbstractVertex ver = root1;
							while(ver!=this.root && ver.getMergeParent() != method2)
							{
								ver = ver.parent;
							}
							if(ver==this.root)
								continue;
							childVer = root1;
							parentVer = root1.incoming.get(k);
							break;
						}
					}
					if(parentVer==null)
						continue;
					System.out.println("\ngoing to merge "+i+" to "+j);
					childVer.changeParent(parentVer);
					System.out.println("change parent done");
					for(int k=0; k<method1.mergeChildren.size(); k++)
					{
						method1.mergeChildren.get(k).mergeParent = method2;
						method2.mergeChildren.add(method1.mergeChildren.get(k));
					}
					System.out.println("merging done");
					this.methodVertices.remove(i);
					i--;
					break;
				}
			}
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
	
	public void mergePaths()
	{
		for(MethodVertex v : this.methodVertices)
			v.mergePath();
		
		for(MethodPathVertex v : this.methodPathVertices)
			v.collectMethodsAndInstructions();
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
				while(ver != StacViz.graph.root)
				{
					if(ver == dest)
					{
						flag = true;
						break;
					}
					ver = ver.parent;
				}
				
				if(!flag) // if there is loop then, try to find potential point to break the loop 
				{
					dest.changeParent(src);
					i--;
					break;
				}
			}
		}
	}

	public void collapseOnce()
	{
		for(MethodPathVertex v : this.methodPathVertices)
		{
			if(v.mergeRoot.isVisible)
				v.collapse();
		}
		
		for(MethodVertex v : this.methodVertices)
		{
			if(v.mergeRoot.isVisible)
				v.collapse();
		}
	}
	
	public void deCollapseOnce()
	{
		for(MethodVertex v : this.methodVertices)
		{
			if(v.isVisible)
			{
				v.deCollapse();
			}
		}
		
		for(MethodPathVertex v : this.methodPathVertices)
		{
			if(v.isVisible)
			{
				v.deCollapse();
			}
		}
	}
	
	public void collapseAll()
	{
		boolean flag = true;
		while(flag)
		{
			flag = false;
			for(MethodVertex v : this.methodVertices)
			{
				if(v.mergeRoot.isVisible)
				{
					flag = true;
					v.collapse();
				}
			}
			
			for(MethodPathVertex v : this.methodPathVertices)
			{
				if(v.mergeRoot.isVisible)
				{
					flag = true;
					v.collapse();
				}
			}
		}
	}
	
	public void deCollapseAll()
	{
		boolean flag = true;
		while(flag)
		{
			flag = false;
			for(MethodVertex v : this.methodVertices)
			{
				if(v.isVisible)
				{
					flag = true;
					v.deCollapse();
				}
			}
			
			for(MethodPathVertex v : this.methodPathVertices)
			{
				if(v.isVisible)
				{
					flag = true;
					v.deCollapse();
				}
			}
		}
	}
	
	public void setAllMethodHeight()
	{
		for(MethodVertex v : this.methodVertices)
			v.setLoopHeight();
		
		for(MethodPathVertex v : this.methodPathVertices)
			v.setLoopHeight();
	}
	
	public void printAdjacencyList()
	{
		for(int i=0; i<this.vertices.size(); i++)
		{
			Vertex v = this.vertices.get(i);
			
			System.out.print(v.id+": ");
			int deg = v.getOutDegree();
			
			for(int j=0; j<deg; j++)
			{
				System.out.print(v.neighbors.get(j).id+" ");
			}
			System.out.println("");
		}
	}
	
	public void printAllChildren()
	{
		for(int i = 0; i < this.vertices.size(); i++)
		{
			Vertex v = this.vertices.get(i);
			
			System.out.print(v.getName()+": ");
			
			for(int j=0; j<v.children.size(); j++)
			{
				System.out.print(v.children.get(j).getName()+" ");
			}
			System.out.println("");
		}
	}
	
	public void printChildren(AbstractVertex v)
	{
		System.out.print(v.getName() + ": ");
		
		for(int j=0; j<v.children.size(); j++)
		{
			System.out.print(v.children.get(j).getName() + " ");
		}
		System.out.println("");
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
		AbstractVertex v = this.root;
		System.out.println("root: " + v.x + ", " + v.y + ", left = " + v.left + ", right = " + v.right + ", width = " + v.width + ", top = " + v.top + ", bottom = " + v.bottom + ", height = " + v.height);
		for(int i=0; i<this.vertices.size(); i++)
		{
			v = this.vertices.get(i);
			
			System.out.println(v.getName() + ": " + v.x + ", " + v.y + ", left = " + v.left + ", right = " + v.right + ", width = " + v.width + ", top = " + v.top + ", bottom = " + v.bottom + ", height = " + v.height);
		}
	}
	
	public double getWidth()
	{
		return root.width;
	}
	
	public double getHeight()
	{
		return root.height - 1;
	}
	
	//Next three methods modified from "A New Algorithm for Identifying Loops in Decompilation"
	public void identifyLoops()
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
		}*/
	}
	
	public Vertex travLoopsDFS(Vertex v0, int dfsPathPos)
	{
		//System.out.println("Expanding vertex: " + Integer.toString(v0.id));
		v0.traversed = true;
		v0.dfsPathPos = dfsPathPos;
		for(AbstractVertex ver : v0.neighbors)
		{
			Vertex v = (Vertex) ver;
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
		//be a loop in following header pointers.
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
	}

}
