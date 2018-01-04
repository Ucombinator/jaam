package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.control.TreeItem;

import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.graph.*;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.GUINodeStatus;

public abstract class AbstractLayoutVertex<T extends AbstractLayoutVertex<T>> extends AbstractVertex
        implements Comparable<AbstractLayoutVertex<T>>, GraphEntity
{
    // Types of layout vertices
    public enum VertexType {
        INSTRUCTION, LOOP, METHOD, CHAIN, ROOT, SHRINK, SCC, TAINT_ADDRESS
    }

    // Because layout vertices are also HierarchicalGraphs they have two associated graphs:
    private HierarchicalGraph<T> selfGraph = null; // The graph to which this vertex belongs
    private HierarchicalGraph<T> innerGraph = new HierarchicalGraph<>(); // The graph contained inside this vertex
    private VertexType vertexType;

    // Graphic related fields
    private GUINode<T> graphics;
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

    public GUINode<T> getGraphics()
    {
        return this.graphics;
    }
    public void setGraphics(GUINode<T> graphics)
    {
        this.graphics = graphics;
    }

    public int compareTo(AbstractLayoutVertex<T> v) {
        return Integer.compare(this.getMinInstructionLine(), v.getMinInstructionLine());
    }

    public void setColor(Color c) {
        this.color = c;
    }

    public Paint getFill() {
        return this.color;
    }

    // Override in base case, LayoutInstructionVertex
    public int getMinInstructionLine() {
        int minIndex = Integer.MAX_VALUE;
        for(AbstractLayoutVertex<T> v : this.getInnerGraph().getVisibleVertices()) {
            minIndex = Math.min(minIndex, v.getMinInstructionLine());
        }

        return minIndex;
    }

    public boolean getDrawEdges() {
        return this.drawEdges;
    }

    public HierarchicalGraph<T> getInnerGraph() {
        return innerGraph;
    }

    public HierarchicalGraph<T> getSelfGraph() {
        return selfGraph;
    }

    public void setInnerGraph(HierarchicalGraph<T> innerGraph) {
        this.innerGraph = innerGraph;
    }

    public void setHidden() {
        this.isHidden = true;
        this.setVisible(false);
        this.getSelfGraph().setHidden((T)this); // TODO: Can we avoid the need for this type cast?
    }

    // Warning: Setting a single vertex in a graph to be unhidden must change the entire graph to be unhidden.
    // TODO: This should be more clearly structured so that it is automatically enforced.
    public void setUnhidden() {
        this.isHidden = false;
        this.setVisible(true);
        if(this.getSelfGraph() != null) {
            this.getSelfGraph().setUnhidden(false);
        }
    }

    public boolean isHidden() {
        return this.isHidden;
    }

    public boolean isVisible() {
        return this.getGraphics().isVisible();
    }

    public void setVisible(boolean isVisible)
    {
        if(this.getGraphics() != null) {
            this.getGraphics().setVisible(isVisible);
        }
    }

    public boolean addTreeNodes(TreeItem<T> parentNode, MainTabController mainTab) {
        boolean addedNodes = false;
        TreeItem<T> newNode = new TreeItem<>((T)this);
        for (T v : this.getInnerGraph().getVisibleVertices()) {
            addedNodes |= v.addTreeNodes(newNode, mainTab);
        }

        if(mainTab.getVizHighlighted().contains(this) || addedNodes) {
            parentNode.getChildren().add(newNode);
            return true;
        } else {
            return false;
        }
    }

    public void resetGraphics() {
        for(T v : this.getInnerGraph().getVisibleVertices()) {
            v.resetGraphics();
        }

        for(LayoutEdge<T> e : this.getInnerGraph().getVisibleEdges()) {
            e.resetGraphics();
        }

        this.graphics = null;
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

    public void setSelfGraph(HierarchicalGraph<T> graph) {
        this.selfGraph = graph;
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
        for (LayoutEdge<T> e : this.innerGraph.getVisibleEdges()) {
            e.setVisible(isEdgeVisible);
        }

        for (T v : this.innerGraph.getVisibleVertices()) {
            v.setEdgeVisibility(isEdgeVisible);
        }
    }

    public void setLabelVisibility(boolean isLabelVisible)
    {
        this.setLabelVisible(isLabelVisible);
        this.getGraphics().setLabelVisible(isLabelVisible);

        for (T v : this.innerGraph.getVisibleVertices()) {
            v.setLabelVisibility(isLabelVisible);
        }
    }

    public void resetStrokeWidth(double factor) {
        this.getGraphics().setStrokeWidth(factor);
        for (T v : this.getInnerGraph().getVisibleVertices()) {
            v.resetStrokeWidth(factor);
        }
    }
    
    public void printCoordinates() {
        System.out.println("Vertex " + this.getId() + this.nodeStatus.toString());
    }

    public VertexType getType() {
        return this.vertexType;
    }

    public void toggleNodesOfType(VertexType type, boolean isExpanded) {
        if (this.getType() == type) {
            this.setExpanded(isExpanded);
        }

        for (T v : this.getInnerGraph().getVisibleVertices()) {
            v.toggleNodesOfType(type, isExpanded);
        }
    }


    public void toggleEdges(boolean isEdgeVisible) {
        for (LayoutEdge<T> e : this.getInnerGraph().getVisibleEdges()) {
            if(e.getGraphics() != null) {
                e.getGraphics().setVisible(!e.getGraphics().isVisible() && isEdgeVisible);
            }
        }

        for (T v : this.getInnerGraph().getVisibleVertices()) {
            v.toggleEdges(isEdgeVisible);
        }
    }

    public boolean isInnerGraphEmpty() {
        return innerGraph.isEmpty();
    }

    public void setClassHighlight(boolean isHighlighted)
    {
        if(!isSelected)
        {
            this.getGraphics().getRect().setEffect(isHighlighted ? classHighlightShadow : null);
        }
    }
}
