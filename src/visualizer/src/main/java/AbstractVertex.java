
import java.util.ArrayList;
import javax.swing.JOptionPane;

//The base class for the various kinds of vertices.
//An abstract vertex is a box on the screen. The subclasses vary in
//what kind of data the vertex represents.

//The current subclasses are lines of code, methods, and paths of methods.
//We may also add other kinds of collapsing later, which will require
//having new kinds of vertices.

public abstract class AbstractVertex
{
	public enum VertexType
	{
		LINE, METHOD, METHOD_PATH
	};
	
	protected int id;
	protected VertexType vertexType;
	protected String name;
	protected int index, parentIndex;
	
	//neighbors stores all of the vertices that are on the same line as this vertex?
	//children stores all of the vertices to which we have edges from this vertex
	//incoming stores all of the vertices that have edges into this vertex
	protected ArrayList<AbstractVertex> neighbors, children, incoming;
	
	protected double x, y, left, right, top, bottom, width, height;
	
	//Since merging occurs as a tree, we only have one merge parent and one merge root
	//The parent is the vertex on the line above us?
	protected AbstractVertex parent, mergeRoot;
	protected boolean isVisible, mergeable;
	protected boolean isHighlighted, isIncomingHighlighted, isOutgoingHighlighted; //Highlight this vertex, incoming edges, or outgoing edges
	protected boolean drawEdges;
	protected int numChildrenHighlighted;
	protected int loopHeight;
	
	//Subclasses must override these so that our generic collapsing can work for all of them
	abstract String getRightPanelContent();
	abstract AbstractVertex getMergeParent();
	abstract ArrayList<? extends AbstractVertex> getMergeChildren();
	
	public void setDefaults()
	{
		this.neighbors = new ArrayList<AbstractVertex>();
		this.incoming = new ArrayList<AbstractVertex>();
		this.children = new ArrayList<AbstractVertex>();
		
		this.width = 1;
		this.height = 1;
		this.left = 0;
		this.right = 1;
		this.top = -1;
		this.bottom = 0;
		this.x = 0.5;
		this.y = -0.5;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public boolean isAtCoordinate(double x, double y)
	{
		return this.isVisible && Math.abs(x - this.x) < 0.25 && Math.abs(y - this.y) < 0.25;
	}
	
	public double distTo(double x, double y)
	{
		return Math.sqrt((x - this.x)*(x - this.x) + (y - this.y)*(y - this.y));
	}
	
	public void replaceChild(AbstractVertex ver)
	{
		int p = ver.parentIndex;
		double inc = ver.width - this.children.get(p).width;
		
		this.children.remove(p);
		this.children.add(p, ver);
		this.increaseWidth(ver, inc);
		
		this.calculateHeight();
	}
	
	public void calculateHeight()
	{
		double h = 0;
		for(int i = 0; i < this.children.size(); i++)
		{
			if(this.children.get(i).height > h)
				h = this.children.get(i).height;
		}
		this.height = h + 1;
		this.bottom = this.top + this.height;
		
		if(this.parent != null)
			this.parent.calculateHeight();
	}
	
	public void collapse()
	{
		if(this.mergeRoot != null && this.mergeRoot.isVisible)
		{
			this.children = new ArrayList<AbstractVertex>();
	
			this.left = this.mergeRoot.left;
			this.top = this.mergeRoot.top;
			Main.graph.setMaxHeight(0);
			
			double w = this.mergeRoot.disappear(this.left, this.top+1, this);
			if(w > 1)
				this.width = w;
			else
				this.width = 1;
			
			this.right = this.left + this.width;
			this.x = (this.left + this.right)/2;
			
			this.height = Main.graph.getMaxHeight() + 1;
			this.bottom = this.top + this.height;
			this.y = this.top + 0.5;
	
			this.parent = this.mergeRoot.parent;
			this.parentIndex = this.mergeRoot.parentIndex;
			this.mergeRoot.parent.replaceChild(this);
	
			this.isVisible = true;
		}
	}
	
	public double disappear(double left, double top, AbstractVertex mP)
	{
		double w = 0;
		AbstractVertex v;
		
		for(int i = 0; i < this.children.size(); i++)
		{
			v = this.children.get(i);
			if(v.getMergeParent() == mP)
				w = w + v.disappear(left + w, top, mP);
			else
			{
				while(!v.isVisible && v.getMergeParent() != null)
					v = v.getMergeParent();
				while(!v.isVisible)
					v = v.mergeRoot;

				v.shiftSubtree(left + w - v.left);
				v.shiftSubtreeY(top - v.top);
				if(v.height > Main.graph.getMaxHeight())
					Main.graph.setMaxHeight(v.height);
				v.parent = mP;
				v.parentIndex = mP.children.size();
				mP.children.add(v);
				w += v.width;
			}
		}
		this.isVisible = false;
		return w;
	}
	
	public void deCollapse()
	{
		if(this.mergeRoot == null)
			return;
		
		if(!this.isVisible)
			return;
		
		this.mergeRoot.left = this.left;
		this.mergeRoot.top = this.top;
		
		this.mergeRoot.appear(this.left, this.top, this);
		

		this.mergeRoot.parent = this.parent;
		this.mergeRoot.parentIndex = this.parentIndex;
		this.parent.replaceChild(this.mergeRoot);

		this.isVisible = false;
	}
	
	public void appear(double left, double top, AbstractVertex mP)
	{
		double w = 0;
		AbstractVertex v;
		
		this.left = left;
		this.top = top;
		this.y = this.top + 0.5;
		
		for(int i=0; i<this.children.size(); i++)
		{
			v = this.children.get(i);
			if(v.getMergeParent() == mP)
			{
				v.appear(left + w, top + 1, mP);
				w += v.width;
			}
			else
			{
				while(!v.isVisible && v.getMergeParent() != null)
					v = v.getMergeParent();
				while(!v.isVisible)
					v = v.mergeRoot;
				if(v != this.children.get(i))
				{
					this.children.remove(i);
					this.children.add(i, v);
				}
				v.shiftSubtree(left + w - v.left);
				v.shiftSubtreeY(top + 1 - v.top);
				v.parent = this;
				v.parentIndex = i;
				w += v.width;
			}
		}
		
		if(w > 1)
			this.width = w;
		else
			this.width = 1;

		this.right = this.left + this.width;
		this.x = (this.left + this.right)/2;
		
		double h = 0;
		for(int i = 0; i < this.children.size(); i++)
		{
			if(this.children.get(i).height > h)
				h = this.children.get(i).height;
		}
		this.height = h + 1;
		this.bottom = this.top + this.height;
		
		this.isVisible = true;
	}
	
	public void increaseWidth(AbstractVertex child, double inc)
	{
		AbstractVertex ver = this;
		AbstractVertex ch = child;
		while(ver != null)
		{
			ver.width += inc;
			ver.right += inc;
			ver.x = (ver.left + ver.right)/2;
			
			for(int i=ch.parentIndex+1; i<ver.children.size(); i++)
			{
				ver.children.get(i).shiftSubtree(inc);
			}
			ch = ver;
			ver = ver.parent;
		}
	}
	
	public void increaseWidthRecursive(AbstractVertex child, double inc)
	{
		this.width += inc;
		this.right += inc;
		this.x = (this.left + this.right)/2;
		
		for(int i=child.parentIndex+1; i<this.children.size(); i++)
		{
			this.children.get(i).shiftSubtree(inc);
		}
		
		if(this.parent!=null)
			this.parent.increaseWidth(this, inc);
	}
	
	public void shiftSubtree(double inc)
	{
		AbstractVertex ver = this;
		while(true)
		{
			ver.left += inc;
			ver.right += inc;
			ver.x += inc;
			if(ver.children.size()>0)
				ver = ver.children.get(0);
			else
			{
				while(ver!=this && ver.parent.children.size()==ver.parentIndex+1)
				{
					ver = ver.parent;
				}
				if(ver==this)
					break;
				else
					ver = ver.parent.children.get(ver.parentIndex+1);
			}
		}
	}

	public void shiftSubtreeNonRecursive(double inc)
	{
		this.left += inc;
		this.right += inc;
		this.x += inc;
		
		if(Parameters.debug)
			System.out.println("shifting subtree for "+this.getName());
		for(int i=0; i<this.children.size(); i++)
			this.children.get(i).shiftSubtree(inc);
	}
	
	public void shiftSubtreeY(double inc)
	{
		this.top += inc;
		this.bottom += inc;
		this.y += inc;
		
		for(int i=0; i<this.children.size(); i++)
			this.children.get(i).shiftSubtreeY(inc);
	}
	
	public void increaseHeight(double inc)
	{
		this.height += inc;
		
		if(this.parent==null)
			return;
		if(this.height+1 > this.parent.height)
			this.parent.increaseHeight(this.height-this.parent.height+1);
	}
	
	public void addChild(AbstractVertex child)
	{
		child.parentIndex = this.children.size();
		this.children.add(child);
		child.parent = this;


		if(this.children.size()==1)
		{
			child.shiftSubtree(this.left - child.left);
			if(child.width > 1)
				this.increaseWidth(child, child.width - 1);
		}
		else
		{
			child.shiftSubtree(this.right - child.left);
			this.increaseWidth(child, child.width);
		}

		child.shiftSubtreeY(this.bottom - child.top);
		
		if(child.height + 1 > this.height)
			this.increaseHeight(child.height - this.height+1);
	}
	
	public boolean checkPotentialParent(AbstractVertex potential)
	{
		AbstractVertex ver = this;
		while(ver != Main.graph.root)
		{
			if(ver == this)
			{
				return false;
			}
			ver = ver.parent;
		}
		return true;
	}
	
	public void addNeighborOld(AbstractVertex dest)
	{
		if(this.id==Parameters.debug1 && dest.id==Parameters.debug2)
			Parameters.debug = true;
		if(this.id==Parameters.debug2 && dest.id==Parameters.debug1)
			Parameters.debug = true;

		if(Parameters.debug)
			System.out.println("adding edge from " + this.id + " to " + dest.id);
		
		if(Parameters.debug)
		{
			System.out.println("before-------------------------");
			AbstractVertex ver = this;
			while(ver.parent!=null)
			{
				ver.printCoordinates();
				Main.graph.printParent(ver);
				ver = ver.parent;
			}
			ver.printCoordinates();
			System.out.println("-------------------------");
			ver = dest;
			while(ver.parent!=null)
			{
				ver.printCoordinates();
				Main.graph.printParent(ver);
				ver = ver.parent;
			}
			ver.printCoordinates();
			System.out.println("-------------------------");
		}
//*/			
		this.neighbors.add(dest);
		dest.incoming.add(this);
		
		boolean flag = false;

		if(dest.parent == Main.graph.root)
		{
			if(Parameters.debug)
				System.out.println("checkpoint 1");
			AbstractVertex ver = this;
			while(ver != Main.graph.root)
			{
				if(Parameters.debug)
				{
					System.out.println("checkpoint 2:" + ver.getName());
				}
				if(ver == dest)
				{
					flag = true;
					break;
				}
				ver = ver.parent;
			}
			
			if(flag)
			{
				if(Parameters.debug)
					System.out.println("checkpoint 3");
				boolean cont = true;
				AbstractVertex parVer = null, chVer = null, potential;
				ver = this;
				while(ver != dest && cont)
				{
					if(Parameters.debug)
						System.out.println("checkpoint 4");
					for(int i = 0; i < ver.incoming.size(); i++)
					{
						if(Parameters.debug)
							System.out.println("checkpoint 5");
						potential = ver.incoming.get(i);
						if(potential==ver.parent)
							continue;
						if(ver.checkPotentialParent(potential))
						{
							cont = false;
							parVer = potential;
							chVer = ver;
							break;
						}
					}
					ver = ver.parent;
				}

				if(parVer == null || chVer == null)
					return;

				chVer.parent.increaseWidth(chVer, -1*chVer.width);
				for(int i=chVer.parentIndex+1; i<chVer.parent.children.size(); i++)
				{
					chVer.parent.children.get(i).parentIndex --;
				}
				chVer.parent.children.remove(chVer.parentIndex);
				ver = chVer;
				while(ver.parent!=null)
				{
					double height = 0;
					for(int i=0; i< ver.parent.children.size(); i++)
					{
						if(ver.parent.children.get(i).height>height)
							height = ver.parent.children.get(i).height;
					}
					ver.parent.height = height + 1;
					ver = ver.parent;
				}
				
				parVer.addChild(chVer);

			}

			if(Parameters.debug)
				System.out.println("checkpoint 6");
			if(Parameters.debug)
				JOptionPane.showMessageDialog(null, "hello");
			if(Parameters.debug)
			{
				System.out.println("middle-------------------------");
				ver = this;
				while(ver.parent!=null)
				{
					ver.printCoordinates();
					Main.graph.printParent(ver);
					ver = ver.parent;
				}
				ver.printCoordinates();
				System.out.println("-------------------------");
				ver = dest;
				while(ver.parent!=null)
				{
					ver.printCoordinates();
					Main.graph.printParent(ver);
					ver = ver.parent;
				}
				ver.printCoordinates();
				System.out.println("-------------------------");
			}
			
			Main.graph.root.increaseWidth(dest, -1*dest.width);
			for(int i=dest.parentIndex+1; i<Main.graph.root.children.size(); i++)
			{
				Main.graph.root.children.get(i).parentIndex --;
			}
			Main.graph.root.children.remove(dest.parentIndex);
			double height = 0;
			for(int i=0; i< Main.graph.root.children.size(); i++)
			{
				if(Main.graph.root.children.get(i).height>height)
					height = Main.graph.root.children.get(i).height;
			}
			Main.graph.root.height = height + 1;
			
			this.addChild(dest);

			if(Parameters.debug)
			{
				System.out.println("after-------------------------");
				ver = this;
				while(ver.parent!=null)
				{
					ver.printCoordinates();
					Main.graph.printParent(ver);
					ver = ver.parent;
				}
				ver.printCoordinates();
				System.out.println("-------------------------");
				ver = dest;
				while(ver.parent!=null)
				{
					ver.printCoordinates();
					Main.graph.printParent(ver);
					ver = ver.parent;
				}
				ver.printCoordinates();
				System.out.println("-------------------------");
			}
		}
	}
	
	public void setLoopHeight()
	{
		if(this.mergeRoot==null)
			return;
		
		this.loopHeight = 0;
		for(AbstractVertex v : this.getMergeChildren())
		{
			if(v.loopHeight > this.loopHeight)
				this.loopHeight = v.loopHeight;
		}
	}
	
	public void addHighlight(boolean vertex, boolean to, boolean from)
	{
		//System.out.println("Highlighting vertex: " + this.getName());
		if(vertex)
			this.isHighlighted = true;
		if(to)
			this.isIncomingHighlighted = true;
		if(from)
			this.isOutgoingHighlighted = true;

		AbstractVertex v = this.getMergeParent();
		while(v != null)
		{
			if(vertex)
			{
				v.numChildrenHighlighted++;
				//System.out.println("Adding child highlight to " + v.getName());
			}
			if(to)
				v.isIncomingHighlighted = true;
			if(from)
				v.isOutgoingHighlighted = true;
			
			v = v.getMergeParent();
		}
	}
	
	public boolean isCycleHighlighted()
	{
		return this.isOutgoingHighlighted && this.isIncomingHighlighted;
	}
	
	public boolean isHighlighted()
	{
		return this.isHighlighted;
	}
	
	public boolean isChildHighlighted()
	{
		//System.out.println("Highlighted children for vertex: " + this.getName() + " = " + this.numChildrenHighlighted);
		return this.numChildrenHighlighted > 0;
	}
	
	public void clearAllHighlights()
	{
		if(this.isHighlighted)
		{
			AbstractVertex v = this.getMergeParent();
			while(v != null)
			{
				v.numChildrenHighlighted--;
				v = v.getMergeParent();
			}
		}

		this.isHighlighted = false;
		this.isIncomingHighlighted = false;
		this.isOutgoingHighlighted = false;
	}
	
	public void clearCycleHighlights()
	{
		this.isIncomingHighlighted = false;
		this.isOutgoingHighlighted = false;
	}
	
	public void changeParent(AbstractVertex newParent)
	{
		this.parent.increaseWidth(this, -1*this.width);
		for(int i=this.parentIndex+1; i<this.parent.children.size(); i++)
		{
			this.parent.children.get(i).parentIndex --;
		}
		this.parent.children.remove(this.parentIndex);
		AbstractVertex ver = this;
		
		while(ver.parent != null)
		{
			double height = 0;
			for(int i=0; i< ver.parent.children.size(); i++)
			{
				if(ver.parent.children.get(i).height>height)
					height = ver.parent.children.get(i).height;
			}
			ver.parent.height = height + 1;
			ver = ver.parent;
		}
		
		newParent.addChild(this);
	}
	
	public int getOutDegree()
	{
		return this.neighbors.size();
	}
	
	public void printCoordinates()
	{
		System.out.println(this.getName() + ": " + this.x + ", " + this.y + ", left = " + this.left + ", right = " + this.right +
				", width = " + this.width + ", top = " + this.top + ", bottom = " + this.bottom + ", height = " + this.height);
	}
}
