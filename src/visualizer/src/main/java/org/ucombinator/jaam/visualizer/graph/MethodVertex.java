package org.ucombinator.jaam.visualizer.graph;

import org.ucombinator.jaam.visualizer.main.Parameters;

import java.util.ArrayList;

public class MethodVertex extends AbstractVertex
{
	private Method method;
	public ArrayList<Vertex> mergeChildren; //mergeChildren stores all of the lines that were merged to make this vertex

	public MethodVertex(Method method, boolean isVisible)
	{
		this.setDefaults();
		vertexType = VertexType.METHOD;

		this.method = method;
		this.name = method.getFullName();
		this.setVisible(isVisible);
		this.numChildrenHighlighted = 0;
		this.drawEdges = true;

		this.incoming = new ArrayList<AbstractVertex>();
		this.children = new ArrayList<AbstractVertex>();
		this.mergeChildren = new ArrayList<Vertex>();
		this.innerGraph = new AbstractGraph();
	}

	public MethodVertex(int id, int index, Method method, boolean isVisible)
	{
		this(method, isVisible);
		this.id = id;
		this.index = index;
	}

	public String getName()
	{
		return this.name;
	}
	
	public String getMethodName()
	{
		return this.method.getFullName();
	}
    
	public Method getMethod()
	{
		return this.method;
	}
	
	public String getRightPanelContent()
	{
		String str = "Method Vertex (loop height = " + loopHeight + ")\n"
				+ "id: " + this.id + "\n"
				+ "method: " + this.getMethodName() + "\n"
				+ "This contains " + this.mergeChildren.size() + " regular vertices\n";
		return str;
	}
	
    public String getShortDescription()
    {
//        String str = this.id+": Method Vertex\n        method: " + this.getMethodName() + "\n";
        String str = "<html>"+this.id+": Method Vertex<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                    + "method: " + Parameters.getHTMLVerbatim(this.getMethodName()) + "</html>";
        return str;
    }
	
	public ArrayList<Vertex> getMergeChildren()
	{
		return this.mergeChildren;
	}
	
	public ArrayList<Instruction> getInstructionList()
	{
		return this.method.getInstructionList();
	}
	
    
    public void collectAllTagsFromChildren()
    {
        for(Vertex ver : this.mergeChildren)
        {

            for(Integer t : ver.tags)
            {
                this.addTag(t);
            }
        }
    }
}
