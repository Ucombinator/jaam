package org.ucombinator.jaam.visualizer.graph;

import java.io.File;

import java.util.ArrayList;
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
		//System.out.println("Adding vertex " + vIndex + ": " + instruction.getText());
		Vertex ver = this.containsInputVertex(vIndex);
		
		if(ver == null)
		{
			ver = new Vertex(instruction, vIndex, true);
			this.vertices.add(ver);
		}
		else {
			ver.setRealInstruction(instruction);
			//System.out.println("Setting method for dummy vertex: " + ver.getMethodName());
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
		//System.out.println("Adding input edge: " + src + ", " + dest);
		Vertex vSrc, vDest;

		vSrc = this.containsInputVertex(src);
		if (vSrc == null)
		{
			//System.out.println("Creating new source vertex: " + src);
			vSrc = new Vertex(src);
			this.vertices.add(vSrc);
		}

		vDest = this.containsInputVertex(dest);
		if (vDest == null)
		{
			//System.out.println("Creating new dest vertex: " + dest);
			vDest = new Vertex(dest);
			this.vertices.add(vDest);
		}

		vSrc.addOutgoingNeighbor(vDest);
		vDest.addIncomingNeighbor(vSrc);
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
        Vertex ver = this.containsInputVertex(nodeId);
        if(ver==null)
            return;
        
        int t = this.tagPresent(tag);
        
        if(t < 0)
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
	
	public Vertex containsInputVertex(int id)
	{
		for(Vertex v : this.vertices)
		{
			if(v.getInputId() == id)
				return v;
		}

		return null;
	}

	/*public void save(String filename) {
		try {
			PacketOutput output = new PacketOutput(new FileOutputStream(filename));
			for (Vertex v : this.vertices)
				v.save(output);
		}
		catch(Exception e) {
			System.out.println(e);
		}
	}*/
}
