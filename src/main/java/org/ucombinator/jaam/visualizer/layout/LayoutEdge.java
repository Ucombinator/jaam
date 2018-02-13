package org.ucombinator.jaam.visualizer.layout;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import org.ucombinator.jaam.visualizer.gui.GUINode;

public class LayoutEdge<T extends AbstractLayoutVertex<T>> implements Comparable<LayoutEdge<T>>, GraphEntity
{
    private static final Color highlightColor = Color.ORANGERED;

    private Group graphics;
    private Shape edgePath; // This will be either a line for most edges, or a path for self-edges
    private Polygon arrowhead;

    public enum EDGE_TYPE {EDGE_REGULAR, EDGE_DUMMY};
    private final EDGE_TYPE type;

    private final T source, dest;

    private static final double defaultStrokeWidth = 0.5;
    private static final double highlightStrokeWidthMultiplier = 4;
    private static final double arrowheadAngleWidth = 0.15 * Math.PI;
    private static final double arrowLengthRatio = 0.5;

    private static final Color downwardColor = Color.BLACK;
    private static final Color upwardColor = Color.DARKRED;
    private Color color;
    private double opacity;

    public LayoutEdge(T source, T dest, EDGE_TYPE edgeType)
    {
        this.type = edgeType;
        this.source = source;
        this.dest = dest;

        graphics = new Group();
        arrowhead = new Polygon();
        this.opacity = 1.0;
    }

    public void setOpacity(double opacity) {
        this.opacity = opacity;
    }

    public double getOpacity() {
        return this.opacity;
    }

    public EDGE_TYPE getType() {
        return type;
    }

    public int compareTo(LayoutEdge<T> that) {
        if (this.source.getId() < that.source.getId()) {
            return -1;
        } else if (this.source.getId() > that.source.getId()) {
            return 1;
        } else {
            if (this.dest.getId() < that.dest.getId()) {
                return -1;
            } else if (this.dest.getId() > that.dest.getId()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public T getSource() {
        return source;
    }

    public T getDest() {
        return dest;
    }

    public void draw()
    {
        // System.out.println("Drawing edge: " + this.source.getId() + " --- " + this.dest.getId());
        if(!source.getDrawEdges() || !dest.getDrawEdges()) {
            System.out.println("Draw source: " + source.getDrawEdges() + "\nDraw dest: " + dest.getDrawEdges());
            return;
        }
        else if(this.getSourceParent() == null) {
            System.out.println("Error! The parent for this edge does not exist.");
            return;
        }
        else if (this.source == this.dest)
        {
            drawLoop();
            return;
        }
        else if (source.getX() == dest.getX() && source.getY() == dest.getY())
        {
            System.out.println("Error in Edge.drawGraph(): The two vertices are at the same location.");
            System.out.println(this.source.getId() + " --- " + this.dest.getId());
            System.out.println(this.source.getLabel() + " --- " + this.dest.getLabel());
            System.out.println(this.getType());
            return;
        }

        //System.out.println("Passed checks for drawing edge: " + this.getID());
        GUINode<T> sourceNode = source.getGraphics();
        GUINode<T> destNode   = dest.getGraphics();

        if (dest.getY() >= source.getY()) {
            this.color = downwardColor;
            Line line = GUINode.getLine(sourceNode, destNode);
            line.setStroke(this.color);
            line.setStrokeWidth(defaultStrokeWidth);
            this.edgePath = line;

            if (this.getType() == EDGE_TYPE.EDGE_DUMMY) {
                line.getStrokeDashArray().addAll(5D, 4D);
            }

            // Compute arrowhead
            double orientAngle = Math.PI + Math.atan2(line.getEndY() - line.getStartY(), line.getEndX() - line.getStartX());
            // TODO: Adjust arrowLength by scale
            double arrowLength = Math.min(10, arrowLengthRatio * dest.getGraphics().getRect().getWidth());
            this.arrowhead = GUINode.computeArrowhead(line.getEndX(), line.getEndY(), arrowLength, orientAngle, arrowheadAngleWidth);
            this.arrowhead.setFill(this.color);
        }
        else {
            this.color = upwardColor;
            QuadCurve curve = GUINode.getCurve(sourceNode, destNode);
            curve.setStroke(this.color);
            curve.setFill(Color.TRANSPARENT);
            curve.setStrokeWidth(defaultStrokeWidth);
            this.edgePath = curve;

            if (this.getType() == EDGE_TYPE.EDGE_DUMMY) {
                curve.getStrokeDashArray().addAll(5D, 4D);
            }

            double orientAngle = Math.PI + Math.atan2(curve.getEndY() - curve.getControlY(), curve.getEndX() - curve.getControlX());
            double arrowLength = Math.min(10, arrowLengthRatio * dest.getGraphics().getRect().getWidth());
            this.arrowhead = GUINode.computeArrowhead(curve.getEndX(), curve.getEndY(), arrowLength, orientAngle, arrowheadAngleWidth);
            this.arrowhead.setFill(this.color);
        }

        this.graphics.getChildren().removeAll(this.graphics.getChildren());
        this.graphics.getChildren().add(edgePath);
        this.graphics.getChildren().add(arrowhead);

        this.getSourceParent().getChildren().add(graphics);
        graphics.setVisible(this.isDisplayed());
    }

    public void highlightEdgePath()
    {
        edgePath.setStroke(highlightColor);
        edgePath.setStrokeWidth(defaultStrokeWidth * highlightStrokeWidthMultiplier);
    }

    public void resetEdgePath()
    {
        edgePath.setStroke(color);
        edgePath.setStrokeWidth(defaultStrokeWidth);
    }

    private void drawLoop() {
        Path path = new Path();
        Bounds bounds = source.getGraphics().getRectBoundsInParent();
        double padding = 2; // TODO: Adjust based on scale

        MoveTo moveTo = new MoveTo();
        moveTo.setX(bounds.getMinX() + bounds.getWidth() / 2.0);
        moveTo.setY(bounds.getMaxY());

        VLineTo vLine1 = new VLineTo();
        vLine1.setY(bounds.getMaxY() + padding);

        HLineTo hLine1 = new HLineTo();
        hLine1.setX(bounds.getMaxX() + padding);

        VLineTo vLine2 = new VLineTo();
        vLine2.setY(bounds.getMinY() - padding);

        HLineTo hLine2 = new HLineTo();
        hLine2.setX(bounds.getMinX() + bounds.getWidth() / 2.0);

        VLineTo vLine3 = new VLineTo();
        vLine3.setY(bounds.getMinY());

        path.getElements().addAll(moveTo, vLine1, hLine1, vLine2, hLine2, vLine3);
        this.edgePath = path;
        this.graphics.getChildren().removeAll(this.graphics.getChildren());
        this.graphics.getChildren().add(edgePath);

        this.getSourceParent().getChildren().add(graphics);
        this.graphics.setVisible(this.isDisplayed());
    }

    // Checks that an edge is currently showing on the screen.
    public boolean isDisplayed()
    {
        return this.getSourceParent().getVertex().isExpanded() && this.source.isEdgeVisible()
                && this.dest.isEdgeVisible() && this.isVisible();
    }

    public static <T extends AbstractLayoutVertex<T>> void redrawEdges(T v, boolean recurse)
    {
        if(v.getSelfGraph() != null)
        {
            HierarchicalGraph<T> selfGraph = v.getSelfGraph();
            for (LayoutEdge<T> e : selfGraph.getVisibleEdges())
            {
                if (v.getId() == e.source.getId() || v.getId() == e.dest.getId())
                {
                    // Remove current graphics and redraw.
                    e.graphics.getChildren().remove(e.edgePath);
                    e.graphics.getChildren().remove(e.arrowhead);
                    e.getSourceParent().getChildren().remove(e.graphics);
                    e.draw();
                }
            }
        }

        if (recurse) {
            HierarchicalGraph<T> innerGraph = v.getInnerGraph();
            for (T w : innerGraph.getVisibleVertices()) {
                redrawEdges(w, recurse);
            }
        }
    }

    public Shape getEdgePath()
    {
        return this.edgePath;
    }

    public Polygon getArrowhead()
    {
        return this.arrowhead;
    }

    public Group getGraphics() {
        return graphics;
    }

    public void setGraphics(Group graphics)
    {
        this.graphics = graphics;
    }

    public void resetGraphics() {
        this.setGraphics(null);
    }

    public void setVisible(boolean isVisible)
    {
        this.graphics.setVisible(isVisible);
    }

    // Checks that an edge is set to visible. The edge may still not show on the screen if the nodes it connects
    // are hidden.
    public boolean isVisible() {
        return this.graphics.isVisible();
    }

    private GUINode<T> getSourceParent()
    {
        return this.getSource().getGraphics().getParentNode();
    }
}
