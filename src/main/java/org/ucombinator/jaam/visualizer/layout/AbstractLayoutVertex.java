package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.control.TreeItem;

import java.util.HashSet;
import java.util.LinkedHashSet;

import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.graph.*;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.GUINodeStatus;

public abstract class AbstractLayoutVertex extends AbstractVertex
        implements Comparable<AbstractLayoutVertex>, GraphEntity
{
    // Types of layout vertices
    public enum VertexType {
        INSTRUCTION, LOOP, METHOD, CHAIN, ROOT, SHRINK, SCC
    }

    // Because layout vertices are also HierarchicalGraphs they have two associated graphs:
    private HierarchicalGraph selfGraph = null; // The graph to which this vertex belongs
    private HierarchicalGraph innerGraph = new HierarchicalGraph(); // The graph contained inside this vertex
    private VertexType vertexType;

    // Graphic related fields
    private GUINode graphics;
    private GUINodeStatus nodeStatus = new GUINodeStatus();

    public static final double DEFAULT_WIDTH = 10.0;
    public static final double DEFAULT_HEIGHT = 10.0;

    protected Color color;
    public static final Color highlightColor = Color.ORANGE;
    public static final DropShadow highlightShadow = new DropShadow(10, Color.BLUE);
    public static final DropShadow classHighlightShadow = new DropShadow(BlurType.ONE_PASS_BOX, Color.GREEN, 20,
            0.5, 0, 0);

    private boolean isExpanded = true;
    private boolean isHidden = false;
    private boolean isLabelVisible = false;
    private boolean isEdgeVisible = true;
    private boolean drawEdges;
    private boolean isSelected = false;

    // TODO we should probably replace this with only a id version
    public AbstractLayoutVertex(String label, VertexType type, boolean drawEdges) {
        super(label);
        this.drawEdges = drawEdges;

        this.vertexType = type;
        this.setVisible(false);
    }

    public AbstractLayoutVertex(int id, String label, VertexType type){
    	super(id, label);
        this.drawEdges = true;

        this.vertexType = type;
        this.setVisible(false);
    }

    public void setX(double x) {
        this.nodeStatus.x = x;
    }
    public void setY(double y) {
        this.nodeStatus.y = y;
    }
    public void setWidth(double width) {
        this.nodeStatus.width = width;
    }
    public void setHeight(double height) {
        this.nodeStatus.height = height;
    }
    public void setBboxWidth(double width) {
        this.nodeStatus.bboxWidth = width;
    }
    public void setBboxHeight(double height) {
        this.nodeStatus.bboxHeight = height;
    }
    public void setOpacity(double opacity) {
        this.nodeStatus.opacity = opacity;
    }

    public double getX() {
        return this.nodeStatus.x;
    }
    public double getY() {
        return this.nodeStatus.y;
    }
    public double getWidth() {
        return this.nodeStatus.width;
    }
    public double getHeight() {
        return this.nodeStatus.height;
    }
    public double getBboxWidth() {
        return this.nodeStatus.bboxWidth;
    }
    public double getBboxHeight() {
        return this.nodeStatus.bboxHeight;
    }
    public double getOpacity() {
        return this.nodeStatus.opacity;
    }
    public GUINodeStatus getNodeStatus() {
        return this.nodeStatus;
    }

    public GUINode getGraphics()
    {
        return graphics;
    }
    public void setGraphics(GUINode graphics)
    {
        this.graphics = graphics;
    }

    public int compareTo(AbstractLayoutVertex v) {
        return ((Integer)(this.getMinInstructionLine())).compareTo(v.getMinInstructionLine());
    }

    // Subclasses must override these so that we have descriptions for each of them,
    // and so that our generic collapsing can work for all of them
    public abstract String getRightPanelContent();

    // These searches may be different for different subclasses, so we implement them there.
    public abstract boolean searchByMethod(String query, MainTabController mainTab);

    // This is needed so that we can show the code for the methods that correspond to selected vertices
    public abstract HashSet<LayoutMethodVertex> getMethodVertices();

    public void setColor(Color c) {
        this.color = c;
    }

    public Paint getFill() {
        return this.color;
    }

    // Override in base case, LayoutInstructionVertex
    public int getMinInstructionLine() {
        int minIndex = Integer.MAX_VALUE;
        for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices()) {
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

    public void setHidden() {
        this.isHidden = true;
        this.setVisible(false);
        this.getSelfGraph().setHidden(this);
    }

    // Warning: Setting a single vertex in a graph to be unhidden must change the entire graph to be unhidden.
    // This should be more clearly structured so that it is automatically enforced.
    public void setUnhidden() {
        this.isHidden = false;
        this.setVisible(true);
        this.getSelfGraph().setUnhidden();
    }

    public boolean isHidden() {
        return this.isHidden;
    }

    public boolean isVisible() {
        return this.getGraphics().isVisible();
    }

    public void setVisible(boolean isVisible)
    {
        if(this.getGraphics() != null)
            this.getGraphics().setVisible(isVisible);
    }

    public boolean addTreeNodes(TreeItem parentNode, MainTabController mainTab) {
        boolean addedNodes = false;
        TreeItem newNode = new TreeItem(this);
        for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices())
            addedNodes |= v.addTreeNodes(newNode, mainTab);

        if(mainTab.getHighlighted().contains(this) || addedNodes) {
            parentNode.getChildren().add(newNode);
            return true;
        }
        else
            return false;
    }

    public double distTo(double x, double y)
    {
        double xDiff = x - this.nodeStatus.x;
        double yDiff = y - this.nodeStatus.y;
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    public void resetGraphics() {
        for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices()) {
            v.resetGraphics();
        }

        for(LayoutEdge e : this.getInnerGraph().getVisibleEdges()) {
            e.resetGraphics();
        }

        this.graphics = null;
    }

    public void searchByID(int id, MainTabController mainTab)
    {
        this.searchByIDRange(id, id, mainTab);
    }

    public void searchByIDRange(int id1, int id2, MainTabController mainTab)
    {
        if(this.getId() >= id1 && this.getId() <= id2) {
            this.setHighlighted(true);
            mainTab.getHighlighted().add(this);
            System.out.println("Search successful: " + this.getId());
        }

        for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices())
            v.searchByIDRange(id1, id2, mainTab);
    }

    public void searchByInstruction(String query, MainTabController mainTab)
    {
        if(this instanceof LayoutInstructionVertex) {
            String instStr = ((LayoutInstructionVertex) this).getInstruction().getText();
            if(instStr.contains(query)) {
                this.setHighlighted(true);
                mainTab.getHighlighted().add(this);
            }
        }

        for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices())
            v.searchByInstruction(query, mainTab);
    }

    public void setHighlighted(boolean isHighlighted)
    {
        this.isSelected = isHighlighted;

        if (isHighlighted) {
            this.getGraphics().setFill(highlightColor);
        } else {
            this.getGraphics().setFill(this.color);
        }

        if (isHighlighted) {
            this.getGraphics().getRect().setEffect(highlightShadow);
        } else {
            this.getGraphics().getRect().setEffect(null);
        }
    }

    public void setSelfGraph(HierarchicalGraph abstractGraph) {
        this.selfGraph = abstractGraph;
    }

    public boolean isExpanded() {
        return this.isExpanded;
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
        for(LayoutEdge e : this.innerGraph.getVisibleEdges())
            e.setVisible(isEdgeVisible);

        for(AbstractLayoutVertex v : this.innerGraph.getVisibleVertices())
            v.setEdgeVisibility(isEdgeVisible);
    }

    public void setLabelVisibility(boolean isLabelVisible)
    {
        this.setLabelVisible(isLabelVisible);
        this.getGraphics().setLabelVisible(isLabelVisible);

        for(AbstractLayoutVertex v : this.innerGraph.getVisibleVertices())
            v.setLabelVisibility(isLabelVisible);
    }

    public void resetStrokeWidth(double factor) {
        this.getGraphics().setStrokeWidth(factor);
        for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices())
            v.resetStrokeWidth(factor);
    }
    
    public void printCoordinates()
    {
        System.out.println("Vertex " + this.getId() + this.nodeStatus.toString());
    }

    public VertexType getType() {
        return this.vertexType;
    }

    public void toggleNodesOfType(VertexType type, boolean isExpanded) {
        if(this.getType() == type){
            this.setExpanded(isExpanded);
        }

        for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices()) {
            v.toggleNodesOfType(type, isExpanded);
        }
    }

    public void toggleEdges(boolean isEdgeVisible) {
        for(LayoutEdge e : this.getInnerGraph().getVisibleEdges()) {
            if(e.getGraphics() != null) {
                e.getGraphics().setVisible(!e.getGraphics().isVisible() && isEdgeVisible);
            }
        }

        for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices()) {
            v.toggleEdges(isEdgeVisible);
        }
    }

    public HashSet<Instruction> getInstructions() {
        return this.getInstructions(new LinkedHashSet<Instruction>());
    }

    private HashSet<Instruction> getInstructions(HashSet<Instruction> instructions) {
        if(this.getType().equals(VertexType.ROOT) || this.getType().equals(VertexType.METHOD)
                || this.getType().equals(VertexType.CHAIN)) {
            for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices()) {
                v.getInstructions(instructions);
            }
        } else if(this.getType().equals(VertexType.INSTRUCTION)){
            instructions.add(((LayoutInstructionVertex) this).getInstruction());
        } else {
            System.out.println("Unrecognized type in method getInstructions: " + this.getType());
        }

        return instructions;
    }

    public HashSet<AbstractLayoutVertex> getVerticesWithInstructionID(int id, String method_name) {
        return getVerticesWithInstructionID(id, method_name, new LinkedHashSet<AbstractLayoutVertex>());
    }

    private HashSet<AbstractLayoutVertex> getVerticesWithInstructionID(int id, String method_name,
                                                                       HashSet<AbstractLayoutVertex> set)  {
        if(this.getType().equals(VertexType.ROOT) || this.getType().equals(VertexType.METHOD)
                || this.getType().equals(VertexType.CHAIN)){
            for(AbstractLayoutVertex v : this.getInnerGraph().getVisibleVertices()) {
                v.getVerticesWithInstructionID(id, method_name, set);
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

    public boolean isInnerGraphEmpty()
    {
        return innerGraph.isEmpty();
    }

    public void setClassHighlight(boolean isHiglighted)
    {
        if(!isSelected)
        {
            this.getGraphics().getRect().setEffect(isHiglighted ? classHighlightShadow : null);
        }
    }
}
