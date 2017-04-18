package org.ucombinator.jaam.visualizer.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.HashMap;

public class Graph
{
	private ArrayList<Vertex> vertices;
    private ArrayList<String> tagsList;
    private ArrayList<Boolean> highlightedTags;
	private HashMap<String, Method> methods;
	private HashMap<String, Class> classes;

	private double maxHeight; // required for collapse method
	private int maxIndex;
	
	public Graph()
	{
		this.vertices = new ArrayList<Vertex>();
		this.methods = new HashMap<String, Method>();
		this.classes = new HashMap<String, Class>();
		this.maxIndex = -1;

        this.tagsList = new ArrayList<String>();
        this.highlightedTags = new ArrayList<Boolean>();
	}

	public ArrayList<Vertex> getVertices() {
		return this.vertices;
	}

	public HashMap<String, Class> getClasses() {
		return this.classes;
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
		Instruction instruction = new Instruction("ErrorState", "", -1, false);
		this.addVertex(id, instruction, false);
	}
	
	public void addVertex(int vIndex, Instruction instruction, boolean drawEdges)
	{
		System.out.println("Adding vertex: " + instruction.getText());
		Vertex ver = this.containsVertex(vIndex);
		
		if(ver == null)
		{
			ver = new Vertex(instruction, vIndex, true);
			this.vertices.add(ver);
		}
		
		this.matchVertexToMethod(ver, instruction.getMethodName());
		ver.setDrawEdges(drawEdges);
		
		if(instruction.getJimpleIndex() > this.maxIndex)
			this.maxIndex = instruction.getJimpleIndex();
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
			currMethod = new Method(this, methodName);
			this.methods.put(methodName, currMethod);
		}

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
				vSrc = new Vertex(src);
				this.vertices.add(vSrc);
			}

			vDest = this.containsVertex(dest);
			if (vDest == null)
			{
				vDest = new Vertex(dest);
				this.vertices.add(vDest);
			}

			vSrc.addOutgoingNeighbor(vDest);
			vDest.addIncomingNeighbor(vSrc);
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

        ver.addTag(Integer.toString(t));
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
	
	public Vertex containsVertex(int id)
	{
		for(Vertex v : this.vertices)
		{
			if(v.getId() == id)
				return v;
		}

		return null;
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

		for(Vertex v : vertices)
		{
			Vertex header = v.getLoopHeader();
			//if(header != null)
			//	System.out.println("identifyLoops:" + v.id + " --> " + v.getLoopHeader().id);
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
		System.out.println("Calculating loop heights");

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
}
