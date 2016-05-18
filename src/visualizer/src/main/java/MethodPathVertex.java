
import java.util.ArrayList;
import java.util.HashSet;

public class MethodPathVertex extends AbstractVertex
{
	ArrayList<MethodVertex> mergeChildren;
	ArrayList<Instruction> instructionList;
	HashSet<Method> methods;
	
	public MethodPathVertex(int d, int i)
	{
		this.setDefaults();
		vertexType = VertexType.METHOD_PATH;
		
		this.id = d;
		this.index = i;
		this.name = "";
		this.numChildrenHighlighted = 0;
		
		this.mergeChildren = new ArrayList<MethodVertex>();
		this.instructionList = new ArrayList<Instruction>();
		this.methods = new HashSet<Method>();
	}
	
	public String getRightPanelContent()
	{
		StringBuilder s = new StringBuilder("Path Vertex\n\n" + "This contains " + this.mergeChildren.size() + " method vertices:\n");
		for(MethodVertex v : this.mergeChildren)
			s.append(v.getName() + "\n");
		
		return s.toString();
	}
	
	public AbstractVertex getMergeParent()
	{
		return null;
	}
	
	public ArrayList<MethodVertex> getMergeChildren()
	{
		return mergeChildren;
	}
	
	public void addMergeChild(MethodVertex v)
	{
		//System.out.println("Adding merge child: " + v.getName());
		this.mergeChildren.add(v);
		this.writeName();
	}
	
	public void collectMethodsAndInstructions()
	{	
		for(MethodVertex v : this.mergeChildren)
			methods.add(v.getMethod());
			
		for(Method m : methods)
			instructionList.addAll(m.getInstructionList());	
	}
	
	public ArrayList<Instruction> getInstructionList()
	{	
		return instructionList;
	}
	
	public void writeName()
	{
		StringBuilder s = new StringBuilder();
		for(int i = 0; i < mergeChildren.size() && i < Parameters.boxLines; i++)
		{
			s.append(mergeChildren.get(i).getName() + "\n");
		}
		
		if(mergeChildren.size() > Parameters.boxLines)
		{
			int extraMethods = mergeChildren.size() - Parameters.boxLines;
			s.append("( " + extraMethods + " other methods");
		}
		
		this.name = s.toString();
	}
}
