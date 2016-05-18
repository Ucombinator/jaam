
import java.util.ArrayList;

public class Method
{
	private final String methodName;
	ArrayList<Vertex> vertices;
	ArrayList<Instruction> instructionList;
	
	public Method(String methodName)
	{
		//System.out.println("Creating method: " + methodName);
		this.methodName = methodName;
		vertices = new ArrayList<Vertex>();
		instructionList = new ArrayList<Instruction>();
	}
	
	public String getName()
	{
		return this.methodName;
	}
	
	public void addVertex(Vertex v)
	{
		this.vertices.add(v);
	}
	
	public ArrayList<Instruction> getInstructionList()
	{
		return this.instructionList;
	}

	//Since we override the equals method, we must also override the hashCode method.
	public int hashCode()
	{
		return this.methodName.hashCode();
	}
	
	public boolean equals(Object other)
	{
		if(other instanceof Method)
			return this.methodName.equals(((Method) other).methodName);
		else
			return false;
	}
	
	public void print()
	{
		System.out.println(this.methodName);
	}
	
	//This has to be here instead of in the MethodVertex class, because a method vertex
	//might only have part of the code for the method.
	public void collectAndSortInstructions()
	{
		this.instructionList = new ArrayList<Instruction>();
	
		for(Vertex v : this.vertices)
		{
			if(v.jimpleIndex >= 0)
			{
				while(instructionList.size() <= v.jimpleIndex)
					instructionList.add(new Instruction(this.methodName));
				
				String newInst = v.jimpleIndex + ":  " + v.getInstruction() + "\n";
				this.instructionList.set(v.jimpleIndex, new Instruction(newInst, this.methodName, true, v.jimpleIndex));
			}
		}
		
		//Remove empty instructions
		/*for(int i = 0; i < instructionList.size(); i++)
		{
			if(instructionList.get(i).str == "")
			{
				this.instructionList.remove(i);
				i--;
			}
		}*/
	}
	
	public void highlightInstructions()
	{
		//System.out.println("Recalculating highlights for instructions in method " + this.methodName);
		for(Instruction inst : this.instructionList)
		{
			inst.isHighlighted = false;
			inst.isCycleHighlighted = false;
		}
		
		for(Vertex v : this.vertices)
		{
			if(v.jimpleIndex >= 0)
			{
				if(v.isHighlighted())
				{
					this.instructionList.get(v.jimpleIndex).isHighlighted = true;
					//System.out.println("Highlighting " + v.jimpleIndex + ", " + v.getMethodName());
				}
				
				if(v.isCycleHighlighted())
				{
					this.instructionList.get(v.jimpleIndex).isCycleHighlighted = true;
					//System.out.println("Cycle highlighting " + v.jimpleIndex + ", " + v.getMethodName());
				}
			}
		}
	}
}
