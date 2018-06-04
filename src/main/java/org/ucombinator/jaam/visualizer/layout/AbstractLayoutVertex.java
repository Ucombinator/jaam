package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import org.ucombinator.jaam.visualizer.graph.*;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.GUINodeStatus;

import java.util.Set;

public abstract class AbstractLayoutVertex<T extends AbstractLayoutVertex<T>>
        implements Vertex
{
    // Types of layout vertices
    public enum VertexType {
        INSTRUCTION, LOOP, METHOD, CLASS, ROOT, SHRINK, SCC, TAINT_ADDRESS, TAINT_STMT, PROFILER
    }

    enum VertexStatus
    {
        WHITE,
        GRAY,
        BLACK
    }

    private static int idCounter = 0;
    private String label;
    private int id;

    private VertexType vertexType;
    private VertexStatus vertexStatus;

    // Graphic related fields
    private GUINode<T> graphics;
    private GUINodeStatus nodeStatus = new GUINodeStatus();

    public static final double DEFAULT_WIDTH = 10.0;
    public static final double DEFAULT_HEIGHT = 10.0;

    protected Color color;
    private static final Color highlightColor = Color.ORANGE;
    private static final DropShadow highlightShadow = new DropShadow(10, Color.BLUE);
    private static final DropShadow classHighlightShadow = new DropShadow(BlurType.ONE_PASS_BOX, Color.GREEN, 20,
            0.5, 0, 0);

    private boolean isExpanded = true;
    private boolean isHidden = false;
    private boolean isLabelVisible = false;
    private boolean isEdgeVisible = true;
    private boolean drawEdges;
    private boolean isSelected = false;

    // Used for building taint graph
    public AbstractLayoutVertex(String label, VertexType type, boolean drawEdges) {
        this.id = idCounter++;
        this.label = label;
        this.drawEdges = drawEdges;
        this.vertexType = type;
        this.setVisible(false);
    }

    // Used for building loop graph
    public AbstractLayoutVertex(int id, String label, VertexType type){
    	this.id = id;
    	this.label = label;
        this.drawEdges = true;
        this.vertexType = type;
        this.setVisible(false);
    }

    public int getId() {
        return this.id;
    }

    public String getLabel() {
        return this.label;
    }

    public VertexStatus getVertexStatus() {
        return vertexStatus;
    }

    public void setVertexStatus(VertexStatus status) {
        this.vertexStatus = status;
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

    public void setColor(Color c) {
        this.color = c;
    }

    public Paint getFill() {
        return this.color;
    }

    public boolean getDrawEdges() {
        return this.drawEdges;
    }

    public void setHidden() {
        this.isHidden = true;
        this.setVisible(false);
    }

    public void setUnhidden() {
        this.isHidden = false;
        this.setVisible(true);
    }

    public boolean isHidden() {
        return this.isHidden;
    }

    public boolean isVisible() {
        return this.getGraphics().isVisible();
    }

    public void setVisible(boolean isVisible) {
        if(this.getGraphics() != null) {
            this.getGraphics().setVisible(isVisible);
        }
    }

    public void setHighlighted(boolean isHighlighted) {
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

    public boolean isExpanded() {
        return this.isExpanded;
    }

    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
    }

    public boolean isLabelVisible() {
        return isLabelVisible;
    }

    public boolean isEdgeVisible() {
        return isEdgeVisible;
    }

    public void setEdgeVisible(boolean isEdgeVisible) {
        this.isEdgeVisible = isEdgeVisible;
    }

    public void setLabelVisible(boolean isLabelVisible)
    {
        this.isLabelVisible = isLabelVisible;
        this.getGraphics().setLabelVisible(isLabelVisible);
    }

    public void resetStrokeWidth(double factor) {
        this.getGraphics().setStrokeWidth(factor);
    }

    public VertexType getType() {
        return this.vertexType;
    }

    public void setClassHighlight(boolean isHighlighted)
    {
        if(!isSelected) {
            this.getGraphics().getRect().setEffect(isHighlighted ? classHighlightShadow : null);
        }
    }

    public void onMouseEnter() {
        this.getIncidentEdges().forEach(LayoutEdge::highlightEdgePath);
    }

    public void onMouseExit() {
        this.getIncidentEdges().forEach(LayoutEdge::resetEdgePath);
    }

    public abstract Set<? extends LayoutEdge<T>> getIncidentEdges();
    public abstract void onMouseClick(MouseEvent event);
}
