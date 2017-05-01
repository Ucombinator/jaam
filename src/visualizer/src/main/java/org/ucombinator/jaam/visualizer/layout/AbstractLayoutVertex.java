package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import org.ucombinator.jaam.visualizer.graph.*;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.Location;
import org.ucombinator.jaam.visualizer.gui.StacFrame;
import org.ucombinator.jaam.visualizer.gui.VizPanel;
import org.w3c.dom.css.CSSPrimitiveValue;


import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.sg.prism.NGNode;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
//import javax.swing.tree.DefaultMutableTreeNode;
import javafx.scene.control.TreeItem;

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

    private HierarchicalGraph selfGraph = null;
    private HierarchicalGraph innerGraph = null;

    private GUINode graphics;
    private boolean isExpanded;
    private boolean isLabelVisible;
    private boolean isEdgeVisible;
    

	// A location stores coordinates for a subtree.
    private Location location = new Location();
    private boolean updateLocation = false;

    // Used for shading vertices based on the number of nested loops they contain
    private int loopHeight;
    private AbstractLayoutVertex loopHeader;
    private ArrayList<AbstractLayoutVertex> loopChildren;
    private int dfsPathPos;

    public enum VertexType {
        INSTRUCTION, METHOD, CHAIN, ROOT
    }
    private VertexType vertexType;

    public Color getColor() {
        return this.color;
    }
    public void setColor(Color color) {
        this.color = color;
    }
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

    public void setDFSPosition(int pos) {
        this.dfsPathPos = pos;
    }

    public int getDFSPosition() {
        return this.dfsPathPos;
    }

    public int getLoopHeight() {
        return this.loopHeight;
    }

    public void setLoopHeight(int loopHeight) {
        this.loopHeight = loopHeight;
    }

    public void setLoopHeader(AbstractLayoutVertex v)
    {
        this.loopHeader = v;
    }

    public AbstractLayoutVertex getLoopHeader()
    {
        return this.loopHeader;
    }

    public void addLoopChild(AbstractLayoutVertex v)
    {
        if(this != v)
            loopChildren.add(v);
        else
            System.out.println("Error! Cannot add self as header in loop decomposition.");
    }

    public ArrayList<AbstractLayoutVertex> getLoopChildren()
    {
        return loopChildren;
    }

    public void setColor(int maxLoopHeight) {
        if(this instanceof LayoutInstructionVertex) {
            double hue = (0.4 - 0.4 * (((double) this.loopHeight) / maxLoopHeight)) * 360;
            this.color = Color.hsb(hue, 0.9, 0.9);
        }
        else {
            for (AbstractLayoutVertex v : this.getInnerGraph().getVertices().values())
                v.setColor(maxLoopHeight);
        }
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
    public abstract boolean searchByMethod(String query, VizPanel mainPanel);

    // This is needed so that we can show the code for the methods that correspond to selected vertices
    public abstract HashSet<LayoutMethodVertex> getMethodVertices();

    static int colorIndex = 0;

    public AbstractLayoutVertex(String label, VertexType type, boolean drawEdges) {
        super(label);
        this.graphics = null;
        this.isExpanded = true;
        this.isLabelVisible = true;
        this.isEdgeVisible = true;

        this.innerGraph = new HierarchicalGraph();
        this.vertexType = type;
        this.drawEdges = drawEdges;

        this.loopChildren = new ArrayList<AbstractLayoutVertex>();
        this.loopHeight = -1;
        this.dfsPathPos = -1;

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

    public int calcMaxLoopHeight() {
        int maxLoopHeight = 0;
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values())
            maxLoopHeight = Math.max(maxLoopHeight, v.getLoopHeight());

        this.setLoopHeight(maxLoopHeight);
        return maxLoopHeight;
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

    public void recomputeGraphicsSize(VizPanel mainPanel)
    {
        double pixelWidth = mainPanel.scaleX(this.location.width);
        double pixelHeight = mainPanel.scaleY(this.location.height);
        
        this.getGraphics().getRect().setWidth(pixelWidth);
        this.getGraphics().getRect().setHeight(pixelHeight);
        
        this.getGraphics().getHighlightingRect().setWidth(pixelWidth);
        this.getGraphics().getHighlightingRect().setHeight(pixelHeight);
    }

    public void setVisible(boolean isVisible)
    {
        if(this.getGraphics() != null)
            this.getGraphics().setVisible(isVisible);
    }

    public boolean addTreeNodes(TreeItem parentNode, VizPanel mainPanel) {
        boolean addedNodes = false;
        TreeItem newNode = new TreeItem(this);
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values())
            addedNodes |= v.addTreeNodes(newNode, mainPanel);

        if(mainPanel.getHighlighted().contains(this) || addedNodes) {
            parentNode.getChildren().add(newNode);
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
            e.resetGraphics();
        }

        this.graphics = null;
    }

    public void searchByID(int id, VizPanel mainPanel)
    {
        this.searchByIDRange(id, id, mainPanel);
    }

    public void searchByIDRange(int id1, int id2, VizPanel mainPanel)
    {
        if(this.getId() >= id1 && this.getId() <= id2) {
            this.setHighlighted(true, mainPanel);
            mainPanel.getHighlighted().add(this);
            System.out.println("Search successful: " + this.getId());
        }

        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values())
            v.searchByIDRange(id1, id2, mainPanel);
    }

    public void searchByInstruction(String query, VizPanel mainPanel)
    {
        if(this instanceof LayoutInstructionVertex) {
            String instStr = ((LayoutInstructionVertex) this).getInstruction().getText();
            if(instStr.contains(query)) {
                this.setHighlighted(true, mainPanel);
                mainPanel.getHighlighted().add(this);
            }
        }

        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices().values())
            v.searchByInstruction(query, mainPanel);
    }

    public void setHighlighted(boolean isHighlighted, VizPanel mainPanel)
    {
        if(isHighlighted) {
            this.getGraphics().setFill(highlightColor);
            mainPanel.getHighlighted().add(this);
            this.setGraphicsHighlighted(true);
        }
        else {
            this.getGraphics().setFill(this.getColor());
            mainPanel.getHighlighted().remove(this);
            this.setGraphicsHighlighted(false);
        }
    }

    private void setGraphicsHighlighted(boolean visible) {
    	Rectangle r = this.getGraphics().getHighlightingRect();
		FadeTransition ft = new FadeTransition(Duration.millis(300),r);
		if(visible){
			this.getGraphics().getHighlightingRect().setVisible(true);
			ft.setFromValue(0f);
			ft.setToValue(1f);
        }else{
        	ft.setFromValue(1f);
			ft.setToValue(0f);
        }
//		AbstractLayoutVertex vertex = this;
//		ft.setOnFinished(new EventHandler<ActionEvent>() {
//			
//			@Override
//			public void handle(ActionEvent event) {
//				//vertex.getGraphics().getHighlightingRect().setVisible(visible);
//			}
//		});
		ft.play();
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

    public boolean isLabelVisible() {
		return isLabelVisible;
	}
	public void setLabelVisible(boolean isLabelVisible) {
		this.isLabelVisible = isLabelVisible;
	}

	public boolean isEdgeVisible() {
        return isEdgeVisible;
    }

    public void setEdgeVisible(boolean isEdgeVisible) {
        this.isEdgeVisible = isEdgeVisible;
    }

    public void setEdgeVisibility(boolean isEdgeVisible)
    {
        this.setEdgeVisible(isEdgeVisible);
        for(LayoutEdge e : this.innerGraph.getEdges().values())
            e.setVisible(isEdgeVisible);

        for(AbstractLayoutVertex v : this.innerGraph.getVertices().values())
            v.setEdgeVisibility(isEdgeVisible);
    }

    public void setLabelVisibility(boolean isLabelVisible)
    {
    	this.setLabelVisible(isLabelVisible);
    	this.getGraphics().setLabelVisible(isLabelVisible);

        for(AbstractLayoutVertex v : this.innerGraph.getVertices().values())
            v.setLabelVisibility(isLabelVisible);
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

    public void toggleEdges(boolean isEdgeVisible) {
        Iterator<LayoutEdge> itEdges = this.getInnerGraph().getEdges().values().iterator();
        while(itEdges.hasNext()) {
            LayoutEdge e = itEdges.next();
            if(e.getGraphics() != null) {
                e.getGraphics().setVisible(!e.getGraphics().isVisible() && isEdgeVisible);
            }
        }

        Iterator<AbstractLayoutVertex> itNodes = this.getInnerGraph().getVertices().values().iterator();
        while(itNodes.hasNext()){
            itNodes.next().toggleEdges(isEdgeVisible);
        }
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
