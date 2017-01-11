
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;

abstract class AbstractVertex implements Comparable<AbstractVertex>
{
	public static final double DEFAULT_WIDTH = 1.0;
	public static final double DEFAULT_HEIGHT = 1.0;
	static int idCounter = 0; // Used to assign unique id numbers to each vertex

	// Used to sort lines of code in a method
	public int compareTo(AbstractVertex o)
	{
		if(this.getMinInstructionLine() == o.getMinInstructionLine())
		{
			return 0;
		}
		else if(this.getMinInstructionLine() < o.getMinInstructionLine())
		{
			return -1;
		} 

		return 1;
	}

	private int minInstructionLine = -1; //start with a negative value to be properly initialized later

	public int getMinInstructionLine() {
		return minInstructionLine;
	}
	public void setMinInstructionLine(int smallestInstructionLine) {
		this.minInstructionLine = smallestInstructionLine;
	}
	
	protected AbstractGraph selfGraph = null;
	protected AbstractGraph innerGraph = null;

	protected GUINode graphics = null;
	
	public GUINode getGraphics()
	{
		return graphics;
	}
	public void setGraphics(GUINode graphics)
	{
		this.graphics = graphics;
	}

	private String label;

	private boolean isExpanded = true;


	public ArrayList<Integer> tags;

    public enum VertexType
	{
		LINE, METHOD, METHOD_PATH, CHAIN, INSTRUCTION, ROOT
	}

	protected int id;
	protected String strId;
	protected VertexType vertexType;
	protected String name;
	protected int index, parentIndex;
	protected ArrayList<Vertex> neighbors;
	protected ArrayList<AbstractVertex> abstractNeighbors;

	// A location stores coordinates for a subtree.
	protected Location location = new Location();
	boolean updateLocation = false;

	//children stores all of the vertices to which we have edges from this vertex
	//in the current display
	//incoming stores all of the vertices that have edges into this vertex in
	//the current display
	//The base graph is stored in the neighbors in the Vertex class.
	protected ArrayList<AbstractVertex> children, incoming;
	

	public void setWidth(double width) {
		this.location.width = width;
	}
	public void setHeight(double height) {
		this.location.height = height;
	}
	
	public void setX(double x) {
		this.location.x = x;
	}
	public void setY(double y) {
		this.location.y = y;
	}
	
	public double getX() {
		return this.location.x;
	}
	public double getY() {
		return this.location.y;
	}
	public double getWidth() {
		return this.location.width;
	}
	public double getHeight() {
		return this.location.height;
	}

	protected AbstractVertex parent, mergeRoot, mergeParent;
	protected boolean isVisible;
	protected boolean isSelected, isHighlighted, isIncomingHighlighted, isOutgoingHighlighted; //Select or Highlight this vertex, incoming edges, or outgoing edges
	protected boolean drawEdges;
	protected int numChildrenHighlighted, numChildrenSelected;
	protected int loopHeight;

	// TODO: Can we just use white for unvisited and black for visited?
	public enum VertexStatus
	{
		WHITE,
		GRAY,
		BLACK,
		VISITED,
		UNVISITED
	}
	protected VertexStatus vertexStatus = VertexStatus.WHITE;

	protected double[] subtreeBoundBox = {this.location.width, this.location.height};

	
	//Subclasses must override these so that we have descriptions for each of them,
	//and so that our generic collapsing can work for all of them
	abstract String getRightPanelContent();
	abstract String getShortDescription();
	abstract ArrayList<? extends AbstractVertex> getMergeChildren();
	abstract String getName();
	abstract Method getMethod();
	
	public AbstractVertex()
	{
		this.abstractNeighbors = new ArrayList<AbstractVertex>();
		this.id = idCounter++;
		this.strId = "vertex:"+this.id;
	
	}
	

	public AbstractVertex(String label, VertexType type){
		this();
		this.label = label;
		this.innerGraph = new AbstractGraph();
		this.vertexType = type;
	}
	
    public String getLabel() {
		return this.label;
	}
	
	public AbstractGraph getInnerGraph() {
		return innerGraph;
	}
	
	public AbstractGraph getSelfGraph() {
		return selfGraph;
	}
	
	public void setInnerGraph(AbstractGraph innerGraph) {
		this.innerGraph = innerGraph;
	}

	public AbstractVertex getMergeParent()
	{
		return this.mergeParent;
	}

	public void setMergeParent(AbstractVertex mergeParent)
	{
		this.mergeParent = mergeParent;
	}
	
	public String getStrID()
	{
		return strId;
	}
	
	public void setDefaults()
	{
		this.setVisible(false);
		this.incoming = new ArrayList<AbstractVertex>();
		this.children = new ArrayList<AbstractVertex>();
        this.tags = new ArrayList<Integer>();
	}

	public void setVisible(boolean isVisible)
	{
		this.isVisible = isVisible;
		if(this.getGraphics() != null)
			this.getGraphics().setVisible(isVisible);
	}
    
    public DefaultMutableTreeNode toDefaultMutableTreeNode()
    {
        return new DefaultMutableTreeNode(this);
    }
    
    public void addTag(int t)
    {
        for(Integer i : this.tags)
        {
            if(i.intValue()==t)
                return;
        }
        this.tags.add(new Integer(t));
    }
    
    public boolean hasTag(int t)
    {
        for(Integer i : this.tags)
        {
            if(i.intValue()==t)
                return true;
        }
        return false;
    }
    
	public double distTo(double x, double y)
	{
		double xDiff = x - this.location.x;
		double yDiff = y - this.location.y;
		return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
	}
	
	public void replaceChild(AbstractVertex ver)
	{
		int p = ver.parentIndex;
		double inc = ver.location.width - this.children.get(p).location.width;
		
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
			if(this.children.get(i).location.height > h)
				h = this.children.get(i).location.height;
		}
		this.location.height = h + 1;
		this.location.bottom = this.location.top + this.location.height;
		
		if(this.parent != null)
			this.parent.calculateHeight();
	}

	//Collapse a merge parent's vertices.
	public void collapse()
	{
		//To collapse a vertex, first we check that it contains visible merge children.
		if(this.mergeRoot != null && this.mergeRoot.isVisible)
		{
			this.updateLocation = true;
			this.children = new ArrayList<AbstractVertex>();

			//Set the location for our merge parent to be the same as its first child.
			this.location.left = this.mergeRoot.location.left;
			this.location.top = this.mergeRoot.location.top;
			Main.graph.setMaxHeight(0);

			//Remove the children of our merge parent and set them invisible.
			double w = this.mergeRoot.disappear(this.location.left, this.location.top + 1, this);
			if(w > 1)
				this.location.width = w;
			else
				this.location.width = 1;
			
			this.location.right = this.location.left + this.location.width;
			this.location.x = (this.location.left + this.location.right)/2;
			
			this.location.height = Main.graph.getMaxHeight() + 1;
			this.location.bottom = this.location.top + this.location.height;
			this.location.y = this.location.top + 0.5;
	
			this.parent = this.mergeRoot.parent;
			this.parentIndex = this.mergeRoot.parentIndex;
			this.mergeRoot.parent.replaceChild(this);
	
			this.setVisible(true);
		}
	}

	//Collapse a vertex into its merge parent
	private double disappear(double left, double top, AbstractVertex mP)
	{
		double w = 0;
		AbstractVertex v;
		
		for(int i = 0; i < this.children.size(); i++)
		{
			v = this.children.get(i);
			v.updateLocation = true;

			// If our current vertex has the same merge parent, then it also should be collapsed, and we
			// recurse to its children.
			if(v.getMergeParent() == mP)
			{
				w = w + v.disappear(left + w, top, mP);
			}
			// Otherwise, we need to shift v.
			else
			{
				while(!v.isVisible && v.getMergeParent() != null)
					v = v.getMergeParent();

				while(!v.isVisible)
					v = v.mergeRoot;

				v.shiftSubtree(left + w - v.location.left);
				v.shiftSubtreeY(top - v.location.top);

				if(v.location.height > Main.graph.getMaxHeight())
					Main.graph.setMaxHeight(v.location.height);

				v.parent = mP;
				v.parentIndex = mP.children.size();
				mP.children.add(v);
				w += v.location.width;
			}
		}
		this.setVisible(false);
		return w;
	}

	//Expand a vertex out of its merge parent.
	public void deCollapse()
	{
		//First check that our vertex is expandable.
		//It must be visible and have a valid merge root.
		if(this.isVisible || this.mergeRoot != null)
		{
			//If so, we set the merge start vertex to take its parent's location...
			this.mergeRoot.location.left = this.location.left;
			this.mergeRoot.location.top = this.location.top;

			//Show it and its children in our graph...
			this.mergeRoot.appear(this.location.left, this.location.top, this);

			//Connect its edges...
			this.mergeRoot.parent = this.parent;
			this.mergeRoot.parentIndex = this.parentIndex;
			this.parent.replaceChild(this.mergeRoot);

			//And lastly, we set our current vertex to be invisible.
			this.setVisible(false);
		}
	}

	//Beginning with our starting merge vertex, display all children of the
	//expanding merge parent.
	private void appear(double left, double top, AbstractVertex mP)
	{
		//System.out.println("Vertex appearing: " + this.getName());
		double w = 0;
		AbstractVertex v;

		this.updateLocation = true;
		this.location.left = left;
		this.location.top = top;
		this.location.y = this.location.top + 0.5;
		
		for(int i = 0; i < this.children.size(); i++)
		{
			//Check each of our children. All merge siblings should appear.
			v = this.children.get(i);
			if(v.getMergeParent() == mP)
			{
				v.appear(left + w, top + 1, mP);
				w += v.location.width;
			}
			//Vertices that do not have the same merge parent do not need to be changed,
			//but we must recompute our edges to them.
			else
			{
				//We walk up our merge tree until we cannot go higher, or we reach a
				//vertex that is isExpanded.
				while(!v.isVisible && v.getMergeParent() != null)
					v = v.getMergeParent();

				//Then we walk back down the chain of merge roots until we find one that
				//is visible. This should be the child to which we have an edge.
				while(!v.isVisible)
					v = v.mergeRoot;

				//If our current child is not correct, we replace it. This can happen when
				//the current child is either collapsed or isExpanded to a different level
				//than it was before.
				if(v != this.children.get(i))
				{
					this.children.remove(i);
					this.children.add(i, v);
				}

				//We must shift our subtrees, since our children have changed.
				v.shiftSubtree(left + w - v.location.left);
				v.shiftSubtreeY(top + 1 - v.location.top);
				v.parent = this;
				v.parentIndex = i;
				w += v.location.width;
			}
		}
		
		if(w > 1)
			this.location.width = w;
		else
			this.location.width = 1;

		this.location.right = this.location.left + this.location.width;
		this.location.x = (this.location.left + this.location.right)/2;
		
		double h = 0;
		for(int i = 0; i < this.children.size(); i++)
		{
			if(this.children.get(i).location.height > h)
				h = this.children.get(i).location.height;
		}
		this.location.height = h + 1;
		this.location.bottom = this.location.top + this.location.height;

		this.setVisible(true);
		//System.out.println("Vertex has appeared!");
	}


    public void centerizeXCoordinate()
    {
        int num = this.children.size();
        
        for(int i=0; i<num; i++)
            this.children.get(i).centerizeXCoordinate();
        
        if(this.children.size()>0)
            this.location.x = (this.children.get(0).location.x + this.children.get(num-1).location.x)/2;
    }
    
    
    public void rearrangeByLoopHeight()
    {
        int num = this.children.size(), pos, max;
        AbstractVertex rearranged[] = new AbstractVertex[num];
        boolean taken[] = new boolean[num];
        int sorted[] = new int[num];
        
        
        // MJA todo: the next sorting is currently done by selection sort, we should convert it to counting sort
        
        for(int i=0; i< num; i++)
        {
            taken[i]=false;
        }

        for(int i=0; i<num; i++)
        {
            pos = -1;
            max = -1;
            
            for(int j=0; j<num; j++)
            {
                if(taken[j])
                    continue;
                if(this.children.get(j).loopHeight>=max)
                {
                    max = this.children.get(j).loopHeight;
                    pos = j;
                }
            }
            
            if(pos>=0)
            {
                taken[pos] = true;
                sorted[num-i-1] = pos;
            }
        }
        
        
        // now rearrange
        
        pos = 0;
        for(int j=num-2; j>=0; pos++)
        {
            rearranged[pos] = this.children.get(sorted[j]);
            j=j-2;
        }

        int l = 0;
        if(num%2==0)
            l = 1;
        while(pos<num)
        {
            rearranged[pos] = this.children.get(sorted[l]);
            l = l + 2;
            pos++;
        }
        
        double left = this.location.left;
        
        for(int i=0; i<num; i++)
        {
            rearranged[i].shiftSubtree(left-rearranged[i].location.left);
            left += rearranged[i].location.width;
        }

            
        this.children = new ArrayList<AbstractVertex>();
        
        for(int i=0; i<num; i++)
        {
            rearranged[i].parentIndex = i;
            this.children.add(rearranged[i]);
        }
        
        for(int i=0; i< num; i++)
            this.children.get(i).rearrangeByLoopHeight();
        
    }
    

    public void rearrangeByWidth()
    {
        int num = this.children.size(), pos;
        double max;
        AbstractVertex rearranged[] = new AbstractVertex[num];
        boolean taken[] = new boolean[num];
        int sorted[] = new int[num];
        
        
        // MJA todo: the next sorting is currently done by selection sort, we should convert it to counting sort
        
        for(int i=0; i< num; i++)
        {
            taken[i]=false;
        }
        
        for(int i=0; i<num; i++)
        {
            pos = -1;
            max = -1;
            
            for(int j=0; j<num; j++)
            {
                if(taken[j])
                    continue;
                if(this.children.get(j).location.width > max)
                {
                    max = this.children.get(j).location.width;
                    pos = j;
                }
            }
            
            if(pos >= 0)
            {
                taken[pos] = true;
                sorted[num-i-1] = pos;
            }
        }
        
        
        // now rearrange
        
        pos = 0;
        for(int j=num-2; j>=0; pos++)
        {
            rearranged[pos] = this.children.get(sorted[j]);
            j=j-2;
        }
        
        int l = 0;
        if(num%2==0)
            l = 1;
        while(pos<num)
        {
            rearranged[pos] = this.children.get(sorted[l]);
            l = l + 2;
            pos++;
        }
        
        double left = this.location.left;
        
        for(int i=0; i<num; i++)
        {
            rearranged[i].shiftSubtree(left-rearranged[i].location.left);
            left += rearranged[i].location.width;
        }
        
        
        this.children = new ArrayList<AbstractVertex>();
        
        for(int i=0; i<num; i++)
        {
            rearranged[i].parentIndex = i;
            this.children.add(rearranged[i]);
        }
        
        for(int i=0; i< num; i++)
            this.children.get(i).rearrangeByWidth();
    }
    
	public void increaseWidth(AbstractVertex child, double inc)
	{
		AbstractVertex ver = this;
		AbstractVertex ch = child;
		while(ver != null)
		{
			ver.updateLocation = true;
			ver.location.width += inc;
			ver.location.right += inc;
			ver.location.x = (ver.location.left + ver.location.right)/2;
			
			for(int i = ch.parentIndex + 1; i < ver.children.size(); i++)
			{
				ver.children.get(i).shiftSubtree(inc);
			}
			ch = ver;
			ver = ver.parent;
		}
	}

	//Shift a tree by the given increment by DFS
	public void shiftSubtree(double inc)
	{
		AbstractVertex ver = this;
		while(true)
		{
			ver.updateLocation = true;
			ver.location.left += inc;
			ver.location.right += inc;
			ver.location.x += inc;

			if(ver.children.size() > 0)
				ver = ver.children.get(0);
			else
			{
				while(ver != this && ver.parent.children.size() == ver.parentIndex + 1)
				{
					ver = ver.parent;
				}
				if(ver == this)
					break;
				else
					ver = ver.parent.children.get(ver.parentIndex + 1);
			}
		}
	}

	//TODO: Why not use DFS here?
	public void shiftSubtreeY(double inc)
	{
		this.updateLocation = true;
		this.location.top += inc;
		this.location.bottom += inc;
		this.location.y += inc;
		
		for(int i = 0; i < this.children.size(); i++)
			this.children.get(i).shiftSubtreeY(inc);
	}
	
	public void increaseHeight(double inc)
	{
		this.location.height += inc;
		
		if(this.parent == null)
			return;
		if(this.location.height + 1 > this.parent.location.height)
			this.parent.increaseHeight(this.location.height - this.parent.location.height + 1);
	}
	
	public void addChild(AbstractVertex child)
	{
		child.parentIndex = this.children.size();
		this.children.add(child);
		child.parent = this;


		if(this.children.size() == 1)
		{
			child.shiftSubtree(this.location.left - child.location.left);
			if(child.location.width > 1)
				this.increaseWidth(child, child.location.width - 1);
		}
		else
		{
			child.shiftSubtree(this.location.right - child.location.left);
			this.increaseWidth(child, child.location.width);
		}

		child.shiftSubtreeY(this.location.bottom - child.location.top);
		
		if(child.location.height + 1 > this.location.height)
			this.increaseHeight(child.location.height - this.location.height + 1);
	}
	
	public boolean checkPotentialParent()
	{
		return this != Main.graph.root;
	}
	
	public void setLoopHeight()
	{
		if(this.mergeRoot == null)
			return;
		
		this.loopHeight = 0;
		for(AbstractVertex v : this.getMergeChildren())
		{
			if(v.loopHeight > this.loopHeight)
				this.loopHeight = v.loopHeight;
		}
	}

	public void toggleSelected()
	{
		this.isSelected = !this.isSelected;
		this.isHighlighted = !this.isHighlighted;
		if(this.parent != null)
			this.parent.isHighlighted = this.isHighlighted;
	}
	
	public void addHighlight(boolean select, boolean vertex, boolean to, boolean from)
	{
		//System.out.println("Highlighting vertex: " + this.getFullName());
		if(select)
			this.isSelected = true;
		if(vertex)
			this.isHighlighted = true;
		if(to)
			this.isIncomingHighlighted = true;
		if(from)
			this.isOutgoingHighlighted = true;

		AbstractVertex v = this.getMergeParent();
		while(v != null)
		{
			if(select)
			{
				v.numChildrenSelected++;
			}
			if(vertex)
			{
				v.numChildrenHighlighted++;
				//System.out.println("Adding child highlight to " + v.getFullName());
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
		//System.out.println("Highlighted children for vertex: " + this.getFullName() + " = " + this.numChildrenHighlighted);
		return this.numChildrenHighlighted > 0;
	}
	
    public boolean isParentHighlighted()
    {
        AbstractVertex ver = this;
        
        while(ver !=null)
        {
            if(ver.isHighlighted)
                return true;
            ver = ver.getMergeParent();
        }
        return false;
    }
    
    public boolean isBranchHighlighted()
    {
        return this.isHighlighted() | this.isParentHighlighted() | this.isChildHighlighted();
    }
    
	public boolean isSelected()
	{
		return this.isSelected;
	}
	
	public boolean isChildSelected()
	{
		return this.numChildrenSelected > 0;
	}
    
    public boolean isParentSelected()
    {
        AbstractVertex ver = this;
        
        while(ver !=null)
        {
            if(ver.isSelected)
                return true;
            ver = ver.getMergeParent();
        }
        return false;
    }
    
    public boolean isBranchSelected()
    {
        return this.isSelected() | this.isParentSelected() | this.isChildSelected();
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
	
	public void clearAllSelect()
	{
		if(this.isSelected)
		{
			AbstractVertex v = this.getMergeParent();
			while(v != null)
			{
				v.numChildrenSelected--;
				v = v.getMergeParent();
			}
		}

		this.isSelected = false;
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
		this.parent.increaseWidth(this, -1*this.location.width);
		for(int i = this.parentIndex + 1; i < this.parent.children.size(); i++)
		{
			this.parent.children.get(i).parentIndex--;
		}
		this.parent.children.remove(this.parentIndex);
		AbstractVertex ver = this;
		
		while(ver.parent != null)
		{
			double height = 0;
			for(int i=0; i< ver.parent.children.size(); i++)
			{
				if(ver.parent.children.get(i).location.height>height)
					height = ver.parent.children.get(i).location.height;
			}
			ver.parent.location.height = height + 1;
			ver = ver.parent;
		}
		
		newParent.addChild(this);
	}

	public void addAbstractNeighbor(AbstractVertex neighbor)
	{
		this.abstractNeighbors.add(neighbor);
	}
	
	public ArrayList<AbstractVertex> getAbstractNeighbors()
	{
		return this.abstractNeighbors;
	}

	public void setSelfGraph(AbstractGraph abstractGraph) {
		this.selfGraph = abstractGraph;
	}
	
	public boolean isExpanded() {
		return isExpanded;
	}

	public void setExpanded(boolean expanded) {
		this.isExpanded = expanded;
	}

	public void setEdgeVisibility(boolean isEdgeVisible)
	{
		for(Edge e : this.innerGraph.getEdges().values())
			e.setVisible(isEdgeVisible);
	}
	
	public void printCoordinates()
	{
		System.out.println("Vertex " + this.id + this.location.toString());
	}

	public void removeAbstractNeighbor(AbstractVertex destVertex) {
		this.abstractNeighbors.remove(destVertex);		
	}


	public VertexType getType() {
		return this.vertexType;
	}




}

