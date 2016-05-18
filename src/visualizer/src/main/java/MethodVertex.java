import java.util.ArrayList;

public class MethodVertex extends AbstractVertex
{
	private Method method;
	public ArrayList<Vertex> mergeChildren; //mergeChildren stores all of the lines that were merged to make this vertex
	public MethodPathVertex mergeParent;
		
	public MethodVertex(int id, int index, Method method, boolean isVisible)
	{
		this.setDefaults();
		vertexType = VertexType.METHOD;
		
		this.id = id;
		this.index = index;
		this.method = method;
		this.name = method.getName();
		this.isVisible = isVisible;
		this.numChildrenHighlighted = 0;
		
		this.neighbors = new ArrayList<AbstractVertex>();
		this.incoming = new ArrayList<AbstractVertex>();
		this.children = new ArrayList<AbstractVertex>();
		this.mergeChildren = new ArrayList<Vertex>();
	}
	
	public String getMethodName()
	{
		return this.method.getName();
	}
	
	public Method getMethod()
	{
		return this.method;
	}
	
	public String getRightPanelContent()
	{
		String str = "Method Vertex\n\n"
				+ "method: " + this.getMethodName() + "\n\n"
				+ "This contains " + this.mergeChildren.size() + " regular vertices\n";
		return str;
	}
	
	public MethodPathVertex getMergeParent()
	{
		return this.mergeParent;
	}
	
	public ArrayList<Vertex> getMergeChildren()
	{
		return this.mergeChildren;
	}
	
	public ArrayList<Instruction> getInstructionList()
	{
		return this.method.getInstructionList();
	}
	
	public void mergePath()
	{
		AbstractVertex in = null, out = null;
		
		for(Vertex temp : this.getMergeChildren())
		{
			for(AbstractVertex v : temp.incoming)
			{
				if(v.getMergeParent() != this)
				{				
					if(in == null)
						in = v.getMergeParent();
					
					else if(v.getMergeParent() != in)
					{
						this.initializeMethodPathVertex();
						return;
					}
				}
			}
		}
		
		for(Vertex temp : this.getMergeChildren())
		{
			for(AbstractVertex v : temp.neighbors)
			{
				if(v.getMergeParent() != this)
				{
					if(out == null)
						out = v.getMergeParent();
					
					else if(v.getMergeParent() != out)
					{
						this.initializeMethodPathVertex();
						return;
					}
				}
			}
		}

		if(in != null)
		{
			if(in.getMergeParent() != null)
			{
				if(in.getMergeParent().mergeable)
				{
					//This may cause a ClassCast exception, but the logic seems to make it work. I don't know why.
					((MethodVertex) in).getMergeParent().addMergeChild(this);
					this.mergeParent = ((MethodVertex) in).getMergeParent();
					return;
				}
			}
		}
		
		if(out != null)
		{
			if(out.getMergeParent() != null)
			{
				//This may cause a ClassCast exception, but the logic seems to make it work. I don't know why.
				((MethodVertex) out).getMergeParent().getMergeChildren().add(this);
				out.getMergeParent().mergeRoot = this;
				this.mergeParent = ((MethodVertex) out).getMergeParent();
				return;
			}
		}
		
		this.initializeMethodPathVertex();
		return;
	}
	
	public void initializeMethodPathVertex()
	{
		//System.out.println("Adding new method path vertex for method " + this.getMethodName());
		MethodPathVertex ver = new MethodPathVertex(this.id, StacViz.graph.methodPathVertices.size());
		ver.mergeRoot = this;
		StacViz.graph.methodPathVertices.add(ver);
		this.mergeParent = ver;
		ver.addMergeChild(this);
		ver.mergeable = true;
	}
}
