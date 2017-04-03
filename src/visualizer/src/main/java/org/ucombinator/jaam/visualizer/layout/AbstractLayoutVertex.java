package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;

import org.ucombinator.jaam.visualizer.graph.*;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.Location;
import org.ucombinator.jaam.visualizer.main.Parameters;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public abstract class AbstractLayoutVertex extends AbstractVertex<AbstractLayoutVertex>
        implements Comparable<AbstractLayoutVertex>
{
    public static Color highlightColor = Color.ORANGE;
    private Color color = Color.RED;
    private Color[] colors = {Color.LIGHTCORAL,
            Color.LIGHTBLUE, Color.LIGHTCYAN,
            Color.LIGHTSEAGREEN, Color.LIGHTSALMON,
            Color.LIGHTSKYBLUE, Color.LIGHTGOLDENRODYELLOW,
            Color.LIGHTGREY};

    public static final double DEFAULT_WIDTH = 1.0;
    public static final double DEFAULT_HEIGHT = 1.0;

    public Color getColor() {
        return this.color;
    }
    public void setColor(Color color) {
        this.color = color;
    }

    private HierarchicalGraph selfGraph = null;
    private HierarchicalGraph innerGraph = null;

    private GUINode graphics;

    private boolean isExpanded;
    private String strId;
    private String name;
    private int index, parentIndex;
    private HashSet<AbstractLayoutVertex> outgoingLayoutNeighbors;
    private HashSet<AbstractLayoutVertex> incomingLayoutNeighbors;

    // A location stores coordinates for a subtree.
    private Location location = new Location();
    private boolean updateLocation = false;

    //Used for shading vertices based on the number of nested loops they contain
    //loopHeight is stored for all vertices
    private int loopHeight;
    private Vertex loopHeader;
    private ArrayList<Vertex> loopChildren;
    private int dfsPathPos;
    private boolean traversed;

    public enum VertexType {
        INSTRUCTION, METHOD, CHAIN, ROOT
    }
    private VertexType vertexType;

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

    public GUINode getGraphics()
    {
        return graphics;
    }
    public void setGraphics(GUINode graphics)
    {
        this.graphics = graphics;
    }

    public int getLoopHeight() {
        return this.loopHeight;
    }

    public void setLoopHeight(int loopHeight) {
        this.loopHeight = loopHeight;
    }

    public int compareTo(AbstractLayoutVertex v) {
        return ((Integer)(this.getMinInstructionLine())).compareTo(v.getMinInstructionLine());
    }

    public boolean isHighlighted; //Select or Highlight this vertex, incoming edges, or outgoing edges
    private boolean drawEdges;

    //Subclasses must override these so that we have descriptions for each of them,
    //and so that our generic collapsing can work for all of them
    public abstract String getRightPanelContent();
    public abstract String getShortDescription();

    // These searches may be different for different subclasses, so we implement them there.
    public abstract boolean searchByMethod(String query);

    // This is needed so that we can show the code for the methods that correspond to selected vertices
    public abstract HashSet<LayoutMethodVertex> getMethodVertices();

    static int colorIndex = 0;
    public AbstractLayoutVertex(String label, VertexType type, boolean drawEdges) {
        super(label);
        this.graphics = null;
        this.isExpanded = true;
        this.outgoingLayoutNeighbors = new HashSet<AbstractLayoutVertex>();
        this.incomingLayoutNeighbors = new HashSet<AbstractLayoutVertex>();

        this.innerGraph = new HierarchicalGraph();
        this.vertexType = type;
        this.drawEdges = drawEdges;

        if(this.vertexType == VertexType.ROOT){
            this.setColor(Color.WHITE);
        } else if (this.vertexType == VertexType.METHOD){
            this.setColor(colors[colorIndex++ % colors.length]);
        } else if (this.vertexType == VertexType.CHAIN){
            this.setColor(Color.GREEN);
        } else if (this.vertexType == VertexType.INSTRUCTION){
            this.setColor(Color.YELLOW);
        }

        this.setVisible(false);
    }

    // Override in base case, LayoutInstructionVertex
    public int getMinInstructionLine() {
        int minIndex = Integer.MAX_VALUE;
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values()) {
            minIndex = Math.min(minIndex, v.getMinInstructionLine());
        }

        return minIndex;
    }

    public boolean getDrawEdges() {
        return this.drawEdges;
    }

    public HierarchicalGraph getInnerGraph() {
        return innerGraph;
    }

    public HierarchicalGraph getSelfGraph() {
        return selfGraph;
    }

    public void setInnerGraph(HierarchicalGraph innerGraph) {
        this.innerGraph = innerGraph;
    }

    public void recomputeGraphicsSize()
    {
        double pixelWidth = Parameters.stFrame.mainPanel.scaleX(this.location.width);
        double pixelHeight = Parameters.stFrame.mainPanel.scaleY(this.location.height);
        this.getGraphics().rect.setWidth(pixelWidth);
        this.getGraphics().rect.setHeight(pixelHeight);
    }

    public void setVisible(boolean isVisible)
    {
        if(this.getGraphics() != null)
            this.getGraphics().setVisible(isVisible);
    }

    public boolean addTreeNodes(DefaultMutableTreeNode parentNode) {
        boolean addedNodes = false;
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(this);
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values())
            addedNodes |= v.addTreeNodes(newNode);

        if(Parameters.stFrame.mainPanel.highlighted.contains(this) || addedNodes) {
            parentNode.add(newNode);
            return true;
        }
        else
            return false;
    }

    public double distTo(double x, double y)
    {
        double xDiff = x - this.location.x;
        double yDiff = y - this.location.y;
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    public void resetGraphics() {
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values()) {
            v.resetGraphics();
        }

        for(LayoutEdge e : this.getInnerGraph().getEdges().values()) {
            e.graphics = null;
        }

        this.graphics = null;
    }

    /*
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
    */

    //Collapse a merge parent's vertices.
    /*
    public void collapse()
    {
        //To collapse a vertex, first we check that it contains visible merge children.
        if(this.mergeRoot != null && this.mergeRoot.isVisible)
        {
            this.updateLocation = true;
            this.children = new ArrayList<org.ucombinator.jaam.visualizer.graph.AbstractVertex>();

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
    */

    //Collapse a vertex into its merge parent
    /*
    private double disappear(double left, double top, org.ucombinator.jaam.visualizer.graph.AbstractVertex mP)
    {
        double w = 0;
        org.ucombinator.jaam.visualizer.graph.AbstractVertex v;

        for(int i = 0; i < this.children.size(); i++)
        {
            v = this.children.get(i);
            v.updateLocation = true;

            // If our current vertex has the same merge parent, then it also should be collapsed, and we
            // recurse to its children.
            if(v.getParentCluster() == mP)
            {
                w = w + v.disappear(left + w, top, mP);
            }
            // Otherwise, we need to shift v.
            else
            {
                while(!v.isVisible && v.getParentCluster() != null)
                    v = v.getParentCluster();

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
    */

    //Expand a vertex out of its merge parent.
    /*
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
    */

    //Beginning with our starting merge vertex, display all children of the
    //expanding merge parent.
    /*
    private void appear(double left, double top, org.ucombinator.jaam.visualizer.graph.AbstractVertex mP)
    {
        //System.out.println("Vertex appearing: " + this.getName());
        double w = 0;
        org.ucombinator.jaam.visualizer.graph.AbstractVertex v;

        this.updateLocation = true;
        this.location.left = left;
        this.location.top = top;
        this.location.y = this.location.top + 0.5;

        for(int i = 0; i < this.children.size(); i++)
        {
            //Check each of our children. All merge siblings should appear.
            v = this.children.get(i);
            if(v.getParentCluster() == mP)
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
                while(!v.isVisible && v.getParentCluster() != null)
                    v = v.getParentCluster();

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
    */

    /*
    public void centerizeXCoordinate()
    {
        int num = this.children.size();

        for(int i = 0; i < num; i++)
            this.children.get(i).centerizeXCoordinate();

        if(this.children.size() > 0)
            this.location.x = (this.children.get(0).location.x + this.children.get(num-1).location.x)/2;
    }
    */


    /*
    public void rearrangeByLoopHeight()
    {
        int num = this.outgoingLayoutNeighbors.size(), pos, max;
        AbstractLayoutVertex rearranged[] = new AbstractLayoutVertex[num];
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


        this.children = new ArrayList<org.ucombinator.jaam.visualizer.graph.AbstractVertex>();

        for(int i=0; i<num; i++)
        {
            rearranged[i].parentIndex = i;
            this.children.add(rearranged[i]);
        }

        for(int i=0; i< num; i++)
            this.children.get(i).rearrangeByLoopHeight();
    }
    */

    /*
    public void rearrangeByWidth()
    {
        int num = this.children.size(), pos;
        double max;
        org.ucombinator.jaam.visualizer.graph.AbstractVertex rearranged[] = new org.ucombinator.jaam.visualizer.graph.AbstractVertex[num];
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


        this.children = new ArrayList<org.ucombinator.jaam.visualizer.graph.AbstractVertex>();

        for(int i=0; i<num; i++)
        {
            rearranged[i].parentIndex = i;
            this.children.add(rearranged[i]);
        }

        for(int i=0; i< num; i++)
            this.children.get(i).rearrangeByWidth();
    }
    */

    /*
    public void increaseWidth(org.ucombinator.jaam.visualizer.graph.AbstractVertex child, double inc)
    {
        org.ucombinator.jaam.visualizer.graph.AbstractVertex ver = this;
        org.ucombinator.jaam.visualizer.graph.AbstractVertex ch = child;
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
    */

    /*
    //Shift a tree by the given increment by DFS
    public void shiftSubtree(double inc)
    {
        org.ucombinator.jaam.visualizer.graph.AbstractVertex ver = this;
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
    */

    /*
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
    */

    /*
    public void increaseHeight(double inc)
    {
        this.location.height += inc;

        if(this.parent == null)
            return;
        if(this.location.height + 1 > this.parent.location.height)
            this.parent.increaseHeight(this.location.height - this.parent.location.height + 1);
    }
    */

    /*
    public void addChild(org.ucombinator.jaam.visualizer.graph.AbstractVertex child)
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
    */

    public void searchByID(int id)
    {
        this.searchByIDRange(id, id);
    }

    public void searchByIDRange(int id1, int id2)
    {
        if(this.getId() >= id1 && this.getId() <= id2) {
            this.setHighlighted(true);
            Parameters.stFrame.mainPanel.highlighted.add(this);
            System.out.println("Search successful: " + this.getId());
        }

        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values())
            v.searchByIDRange(id1, id2);
    }

    public void searchByInstruction(String query)
    {
        if(this instanceof LayoutInstructionVertex) {
            String instStr = ((LayoutInstructionVertex) this).getInstruction().getText();
            if(instStr.contains(query)) {
                this.setHighlighted(true);
                Parameters.stFrame.mainPanel.highlighted.add(this);
            }
        }

        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values())
            v.searchByInstruction(query);
    }

    public void setHighlighted(boolean isHighlighted)
    {
        if(isHighlighted) {
            this.getGraphics().setFill(highlightColor);
            Parameters.stFrame.mainPanel.highlighted.add(this);
        }
        else {
            this.getGraphics().setFill(this.getColor());
            Parameters.stFrame.mainPanel.highlighted.remove(this);
        }
    }

    public boolean isHighlighted()
    {
        return this.isHighlighted;
    }

    public void setSelfGraph(HierarchicalGraph abstractGraph) {
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
        for(LayoutEdge e : this.innerGraph.getEdges().values())
            e.setVisible(isEdgeVisible);
    }

    public void printCoordinates()
    {
        System.out.println("Vertex " + this.getId() + this.location.toString());
    }

    public VertexType getType() {
        return this.vertexType;
    }

    public void toggleNodesOfType(VertexType type, boolean isExpanded) {
        if(this.getType() == type){
            this.setExpanded(isExpanded);
        }

        Iterator<AbstractLayoutVertex> it = this.getInnerGraph().getVertices().values().iterator();
        while(it.hasNext()){
            it.next().toggleNodesOfType(type, isExpanded);
        }
    }

    public void toggleEdges() {
        Iterator<LayoutEdge> itEdges = this.getInnerGraph().getEdges().values().iterator();
        while(itEdges.hasNext()) {
            LayoutEdge e = itEdges.next();
            if(e.getGraphics() != null) {
                e.getGraphics().setVisible(!e.getGraphics().isVisible() && Parameters.edgeVisible);
            }
        }

        Iterator<AbstractLayoutVertex> itNodes = this.getInnerGraph().getVertices().values().iterator();
        while(itNodes.hasNext()){
            itNodes.next().toggleEdges();
        }
    }

    public void reset() {
        /*this.setGraphics(null);
        Iterator<AbstractLayoutVertex> it = this.getInnerGraph().getVertices().values().iterator();
        while(it.hasNext()){
            it.next().reset();
        }*/
    }

    public HashSet<Instruction> getInstructions() {
        return this.getInstructions(new HashSet<Instruction>());
    }

    private HashSet<Instruction> getInstructions(HashSet<Instruction> instructions) {
        if(this.getType().equals(VertexType.ROOT) || this.getType().equals(VertexType.METHOD) || this.getType().equals(VertexType.CHAIN)) {
            Iterator<AbstractLayoutVertex> it = this.getInnerGraph().getVertices().values().iterator();
            while(it.hasNext()){
                it.next().getInstructions(instructions);
            }
        } else if(this.getType().equals(VertexType.INSTRUCTION)){
            instructions.add(((LayoutInstructionVertex) this).getInstruction());
        } else {
            System.out.println("Unrecognized type in method getInstructions: " + this.getType());
        }

        return instructions;
    }

    public HashSet<AbstractLayoutVertex> getVerticesWithInstructionID(int id, String method_name){
        return getVerticesWithInstructionID(id, method_name, new HashSet<AbstractLayoutVertex>());
    }

    private HashSet<AbstractLayoutVertex> getVerticesWithInstructionID(int id, String method_name, HashSet<AbstractLayoutVertex> set){
        if(this.getType().equals(VertexType.ROOT) || this.getType().equals(VertexType.METHOD) || this.getType().equals(VertexType.CHAIN)){
            Iterator<AbstractLayoutVertex> it = this.getInnerGraph().getVertices().values().iterator();
            while(it.hasNext()){
                it.next().getVerticesWithInstructionID(id, method_name, set);
            }
        } else if(this.getType().equals(VertexType.INSTRUCTION)) {
            Instruction inst = ((LayoutInstructionVertex) this).getInstruction();
            if(inst.getMethodName() == method_name && inst.getJimpleIndex() == id) {
                set.add(this);
            }
        } else {
            System.out.println("Unrecognized type in method getInstructions: " + this.getType());
        }

        return set;
    }

    public void cleanAll(){
        this.setVertexStatus(VertexStatus.WHITE);
        Iterator<AbstractLayoutVertex> it = this.getInnerGraph().getVertices().values().iterator();
        while(it.hasNext()) {
            AbstractLayoutVertex v = it.next();
            v.cleanAll();
        }
    }

    public int getTotalEdgeCount(){
        int result = this.getInnerGraph().getEdges().size();
        for(AbstractLayoutVertex v: this.getInnerGraph().getVertices().values()){
            result += v.getTotalEdgeCount();
        }
        return result;
    }

    public int getTotalVertexCount(){
        int result = this.getInnerGraph().getVertices().size();
        for(AbstractLayoutVertex v: this.getInnerGraph().getVertices().values()) {
            result += v.getTotalVertexCount();
        }
        return result;
    }
}
