package org.ucombinator.jaam.visualizer.layout;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import org.ucombinator.jaam.visualizer.gui.GUINode;

public abstract class LayoutEdge<T extends AbstractLayoutVertex> implements Comparable<LayoutEdge<T>>
{
    private static final Color highlightColor = Color.ORANGERED;
    private static final Color downwardColor = Color.BLACK;
    private static final Color upwardColor = Color.DARKRED;

    private static final double defaultStrokeWidth = 0.5;
    private static final double highlightStrokeWidthMultiplier = 4;
    private static final double arrowheadAngleWidth = 0.15 * Math.PI;
    private static final double arrowLengthRatio = 0.5;

    private T src, dest;
    private Group graphics;
    private Shape edgePath; // A path for self-edges and spanning tree edges, and a straight line for all the rest.
    private Polygon arrowhead;

    public enum EDGE_TYPE {EDGE_REGULAR, EDGE_DUMMY};
    private EDGE_TYPE type;
    private Color color;
    private double opacity;
    private boolean isSpanningEdge;

    public LayoutEdge(T src, T dest, EDGE_TYPE edgeType) {
        this.src = src;
        this.dest = dest;
        this.type = edgeType;
        this.isSpanningEdge = false;
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
        if (this.src.getId() < that.src.getId()) {
            return -1;
        } else if (this.src.getId() > that.src.getId()) {
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

    public T getSrc() {
        return src;
    }

    public T getDest() {
        return dest;
    }

    public void setSpanningEdge(boolean isSpanningEdge) {
        this.isSpanningEdge = isSpanningEdge;
    }

    public void draw()
    {
        if(!src.getDrawEdges() || !dest.getDrawEdges()) {
            System.out.println("Draw source: " + src.getDrawEdges() + "\nDraw dest: " + dest.getDrawEdges());
            return;
        }
        else if(this.getSrcParent() == null) {
            System.out.println("Error! The parent for this edge does not exist.");
            return;
        }
        else if (this.src == this.dest)
        {
            drawLoop();
            return;
        }
        else if (src.getX() == dest.getX() && src.getY() == dest.getY())
        {
            System.out.println("Error in Edge.drawGraph(): The two vertices are at the same location.");
            /*
            System.out.println(this.src.getId() + " --- " + this.dest.getId());
            System.out.println(this.src.getLabel() + " --- " + this.dest.getLabel());
            System.out.println(this.getType());
            */
            return;
        }
        else {
            double destCenter = dest.getX() + (dest.getWidth() / 2);
            double srcLeft = src.getX();
            double srcRight = src.getX() + src.getWidth();
            if (this.isSpanningEdge && (destCenter > srcLeft) && (destCenter < srcRight)) {
                drawOrthogonalEdge();
            } else {
                drawStraightEdge();
            }

            this.graphics.getChildren().removeAll(this.graphics.getChildren());
            this.graphics.getChildren().add(edgePath);
            this.graphics.getChildren().add(arrowhead);

            this.getSrcParent().getChildren().add(graphics);
            graphics.setVisible(this.isDisplayed());
            graphics.setMouseTransparent(true);
        }
    }

    public void drawOrthogonalEdge() {
        // System.out.println("Drawing orthogonal edge: " + src + " --> " + dest);
        double destCenter = dest.getX() + (dest.getWidth() / 2);
        Path path = new Path();
        MoveTo moveTo = new MoveTo();
        if (destCenter < src.getX()) {
            moveTo.setX(src.getX());
        }
        else if (destCenter < src.getX() + src.getWidth()) {
            moveTo.setX(destCenter);
        }
        else {
            moveTo.setX(src.getX() + src.getWidth());
        }
        moveTo.setY(src.getY() + (src.getHeight() / 2));

        HLineTo hLine = new HLineTo();
        hLine.setX(destCenter);

        VLineTo vLine = new VLineTo();
        vLine.setY(dest.getY());
        path.getElements().addAll(moveTo, hLine, vLine);

        this.setEdgePath(path);

        double arrowLength = Math.min(10, arrowLengthRatio * dest.getGraphics().getWidth());
        this.arrowhead = GUINode.computeArrowhead(destCenter, dest.getY(), arrowLength, 3 * Math.PI / 2, arrowheadAngleWidth);
    }

    public void drawStraightEdge() {
        // System.out.println("Drawing straight edge: " + src + " --> " + dest);
        GUINode srcNode = src.getGraphics();
        GUINode destNode   = dest.getGraphics();

        if (dest.getY() >= src.getY()) {
            this.color = downwardColor;
            Line line = GUINode.getLine(srcNode, destNode);
            line.setStroke(this.color);
            line.setStrokeWidth(defaultStrokeWidth);
            this.setEdgePath(line);

            if (this.getType() == EDGE_TYPE.EDGE_DUMMY) {
                line.getStrokeDashArray().addAll(5D, 4D);
            }

            // Compute arrowhead
            double orientAngle = Math.PI + Math.atan2(line.getEndY() - line.getStartY(), line.getEndX() - line.getStartX());
            // TODO: Adjust arrowLength by scale
            double arrowLength = Math.min(10, arrowLengthRatio * dest.getGraphics().getWidth());
            this.arrowhead = GUINode.computeArrowhead(line.getEndX(), line.getEndY(), arrowLength, orientAngle, arrowheadAngleWidth);
            this.arrowhead.setFill(this.color);
        }
        else {
            this.color = upwardColor;
            QuadCurve curve = GUINode.getCurve(srcNode, destNode);
            curve.setStroke(this.color);
            curve.setFill(Color.TRANSPARENT);
            curve.setStrokeWidth(defaultStrokeWidth);
            this.setEdgePath(curve);

            if (this.getType() == EDGE_TYPE.EDGE_DUMMY) {
                curve.getStrokeDashArray().addAll(5D, 4D);
            }

            double orientAngle = Math.PI + Math.atan2(curve.getEndY() - curve.getControlY(), curve.getEndX() - curve.getControlX());
            double arrowLength = Math.min(10, arrowLengthRatio * dest.getGraphics().getWidth());
            this.arrowhead = GUINode.computeArrowhead(curve.getEndX(), curve.getEndY(), arrowLength, orientAngle, arrowheadAngleWidth);
            this.arrowhead.setFill(this.color);
        }
    }

    public void highlightEdgePath()
    {
        if(edgePath != null) {
            edgePath.setStroke(highlightColor);
            edgePath.setStrokeWidth(defaultStrokeWidth * highlightStrokeWidthMultiplier);
        }
        else {
            System.out.println("Error! Edge path is null.");
        }
    }

    public void resetEdgePath()
    {
        if(edgePath != null) {
            edgePath.setStroke(color);
            edgePath.setStrokeWidth(defaultStrokeWidth);
        }
        else {
            System.out.println("Error! Edge path is null.");
        }
    }

    private void drawLoop() {
        Path path = new Path();
        Bounds bounds = src.getGraphics().getRectBoundsInParent();
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
        this.setEdgePath(path);
        this.graphics.getChildren().removeAll(this.graphics.getChildren());
        this.graphics.getChildren().add(edgePath);

        this.getSrcParent().getChildren().add(graphics);
        this.graphics.setVisible(this.isDisplayed());
    }

    // Checks that an edge is currently showing on the screen.
    public boolean isDisplayed()
    {
        return this.getSrcParent().getVertex().isExpanded() && this.src.isEdgeVisible()
                && this.dest.isEdgeVisible() && this.isVisible();
    }

    public void redrawEdge()
    {
        // Remove current graphics and redraw.
        this.graphics.getChildren().remove(this.edgePath);
        this.graphics.getChildren().remove(this.arrowhead);
        if (this.getSrcParent() != null) {
            this.getSrcParent().getChildren().remove(this.graphics);
        }
        this.draw();
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

    public void setVisible(boolean isVisible) {
        this.graphics.setVisible(isVisible);
    }

    public void redrawAndSetVisible(boolean isVisible)
    {
        this.redrawEdge();
        this.graphics.setVisible(isVisible);
    }

    // Checks that an edge is set to visible. The edge may still not show on the screen if the nodes it connects
    // are hidden.
    public boolean isVisible() {
        return this.graphics.isVisible();
    }

    private GUINode getSrcParent()
    {
        if (this.getSrc().getGraphics() != null)
            return this.getSrc().getGraphics().getParentNode();
        return null;
    }

    private void setEdgePath(Shape newShape) {
        this.edgePath = newShape;
        this.edgePath.setMouseTransparent(false);
    }
}
