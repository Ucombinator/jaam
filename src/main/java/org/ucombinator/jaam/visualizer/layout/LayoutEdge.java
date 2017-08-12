package org.ucombinator.jaam.visualizer.layout;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import org.ucombinator.jaam.visualizer.gui.GUINode;

public class LayoutEdge implements Comparable<org.ucombinator.jaam.visualizer.layout.LayoutEdge>
{
    private Group graphics;
    private Shape edgePath; // This will be a either a line for most edges, or a path for self-edges
    private Polygon arrowhead;
    private Rectangle marker1, marker2;
    private static final boolean markCenters = false;

    public enum EDGE_TYPE {EDGE_REGULAR, EDGE_DUMMY};
    private final EDGE_TYPE type;

    private final AbstractLayoutVertex source, dest;
    private GUINode node;

    private static final double defaultStrokeWidth = 1;
    private static final double arrowheadAngleDiff = 0.15 * Math.PI;
    private static final double arrowLengthRatio = 0.5;

    private static final Color downwardColor = Color.BLACK;
    private static final Color upwardColor = Color.VIOLET;
    private Color color = Color.RED;
    private boolean colorIsSet = false;

    public LayoutEdge(AbstractLayoutVertex source, AbstractLayoutVertex dest, EDGE_TYPE edgeType)
    {
        this.type = edgeType;
        this.source = source;
        this.dest = dest;

        System.out.println("SEE: --" + dest.getY() + "-- --" + source.getY());

        graphics = new Group();
        arrowhead = new Polygon();
        //System.out.println("Created new layout edge: " + this.getID());
    }

    public EDGE_TYPE getType() {
        return type;
    }

    public int compareTo(LayoutEdge that) {
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

    public AbstractLayoutVertex getSource() {
        return source;
    }

    public AbstractLayoutVertex getDest() {
        return dest;
    }

    public void draw(GUINode node)
    {
        if(!source.getDrawEdges() || !dest.getDrawEdges()) {
            System.out.println("Draw source: " + source.getDrawEdges() + "\nDraw dest: " + dest.getDrawEdges());
            return;
        }
        else if((source.getGraphics() == null) || (dest.getGraphics() == null))
            return;
        else if(node == null) {
            System.out.println("Error! The node for this edge does not exist.");
            return;
        }
        else if (this.source == this.dest)
        {
            /*System.out.println("Error in Edge.drawGraph(): The source and destination vertices are the same.");
            System.out.println(this.source +"---"+ this.dest);
            System.out.println(this.source.getLabel() +"---"+ this.dest.getLabel());
            System.out.println(this.getType());
            return;*/
            this.node = node;
            drawLoop(node);
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
        this.node = node;
        GUINode sourceNode = source.getGraphics();
        GUINode destNode = dest.getGraphics();
        Line line = GUINode.getLine(sourceNode, destNode);
        if (this.getType() == EDGE_TYPE.EDGE_DUMMY)
        {
            line.getStrokeDashArray().addAll(5d, 4d);
        }

        line.setStrokeWidth(dest.getGraphics().getRect().getStrokeWidth());
        if(!colorIsSet) {
            colorIsSet = true;
            if (dest.getY() >= source.getY()) {
                color = downwardColor;
            } else {
                color = upwardColor;
            }
        }
        line.setStroke(color);


        // Compute arrowhead
        double angle = Math.PI + Math.atan2(line.getEndY() - line.getStartY(), line.getEndX() - line.getStartX());
        // TODO: Adjust arrowLength by scale
        double arrowLength = Math.min(10, arrowLengthRatio * dest.getGraphics().getRect().getWidth());

        double destEnterX = line.getEndX();
        double destEnterY = line.getEndY();
        double x1 = destEnterX;
        double y1 = destEnterY;
        double x2 = destEnterX + arrowLength * Math.cos(angle + arrowheadAngleDiff);
        double y2 = destEnterY + arrowLength * Math.sin(angle + arrowheadAngleDiff);
        double x3 = destEnterX + arrowLength * Math.cos(angle - arrowheadAngleDiff);
        double y3 = destEnterY + arrowLength * Math.sin(angle - arrowheadAngleDiff);

        arrowhead = new Polygon();
        arrowhead.getPoints().addAll(
                x1, y1,
                x2, y2,
                x3, y3);
        arrowhead.setFill(color);

        this.edgePath = line;
        this.graphics.getChildren().removeAll(this.graphics.getChildren());
        this.graphics.getChildren().add(edgePath);
        this.graphics.getChildren().add(arrowhead);

        node.getChildren().add(graphics);
        graphics.setVisible(node.getVertex().isExpanded() && this.source.isEdgeVisible()
                && this.dest.isEdgeVisible() && this.isVisible());
    }

    public void highlightEdgePath()
    {
        edgePath.setStroke(Color.ORANGERED);
        edgePath.setStrokeWidth(edgePath.getStrokeWidth() * 4.0);
    }

    public void resetEdgePath()
    {
        edgePath.setStroke(color);
        edgePath.setStrokeWidth(edgePath.getStrokeWidth() / 4.0);
    }

    public void drawLoop(GUINode node) {
        System.out.println("Drawing loop for vertex: " + this.source.getId());
        System.out.println("Rect bounds in node: " + source.getGraphics().getRect().getBoundsInParent());
        Path path = new Path();
        Bounds bounds = source.getGraphics().getRectBoundsInParent();
        System.out.println("Rect bounds in parent: " + bounds);
        double padding = 10; // TODO: Adjust based on scale

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

        //System.out.println("Adding edge for vertices: " + this.source.getId() + ", " + this.dest.getId());
        node.getChildren().add(graphics);
        graphics.setVisible(node.getVertex().isExpanded() && this.source.isEdgeVisible() && this.isVisible());
    }

    public Rectangle getMarker(double x, double y)
    {
        Rectangle marker = new Rectangle();
        marker.setTranslateX(x);
        marker.setTranslateY(y);
        marker.setWidth(10);
        marker.setHeight(10);
        marker.setFill(Color.RED);

        return marker;
    }

    public static void redrawEdges(AbstractLayoutVertex v, boolean recurse)
    {
        if(v.getSelfGraph() != null)
        {
            for (LayoutEdge e : v.getSelfGraph().getEdges())
            {
                if (v.getId() == e.source.getId() || v.getId() == e.dest.getId())
                {
                    // Clear current graphics...
                    if (markCenters) {
                        e.graphics.getChildren().remove(e.marker1);
                        e.graphics.getChildren().remove(e.marker2);
                    }

                    e.graphics.getChildren().remove(e.edgePath);
                    e.graphics.getChildren().remove(e.arrowhead);

                    if (e.node != null)
                        e.node.getChildren().remove(e.graphics);

                    // ...And draw new ones
                    e.draw(e.node);
                }
            }
        }

        if(recurse)
        {
            for (AbstractLayoutVertex w : v.getInnerGraph().getVertices())
                redrawEdges(w, recurse);
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

    public boolean isVisible() {
        return this.graphics.isVisible();
    }
}
