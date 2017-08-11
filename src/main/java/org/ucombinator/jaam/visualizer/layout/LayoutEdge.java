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
    private EDGE_TYPE type;

    private final AbstractLayoutVertex sourceVertex, destVertex;
    private GUINode node;

    private static final double defaultStrokeWidth = 1;
    private static final double arrowheadAngleDiff = 0.15 * Math.PI;
    private static final double arrowLengthRatio = 0.5;

    private Color downwardColor = Color.BLACK;
    private Color upwardColor = Color.VIOLET;
    private Color color = Color.RED;
    private boolean colorIsSet = false;

    public LayoutEdge(AbstractLayoutVertex sourceVertex, AbstractLayoutVertex destVertex, EDGE_TYPE edgeType)
    {
        this.type = edgeType;
        this.sourceVertex = sourceVertex;
        this.destVertex = destVertex;
        this.sourceVertex.addOutgoingNeighbor(this.destVertex);
        this.destVertex.addIncomingNeighbor(this.sourceVertex);

        System.out.println("SEE: --" + destVertex.getY() + "-- --" + sourceVertex.getY());

        graphics = new Group();
        arrowhead = new Polygon();
        //System.out.println("Created new layout edge: " + this.getID());
    }

    public EDGE_TYPE getType() {
        return type;
    }

    public int compareTo(LayoutEdge that) {
        if (this.sourceVertex.getId() < that.sourceVertex.getId()) {
            return -1;
        } else if (this.sourceVertex.getId() > that.sourceVertex.getId()) {
            return 1;
        } else {
            if (this.destVertex.getId() < that.destVertex.getId()) {
                return -1;
            } else if (this.destVertex.getId() > that.destVertex.getId()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public AbstractLayoutVertex getSourceVertex() {
        return sourceVertex;
    }

    public AbstractLayoutVertex getDestVertex() {
        return destVertex;
    }

    public void draw(GUINode node)
    {
        if(!sourceVertex.getDrawEdges() || !destVertex.getDrawEdges()) {
            System.out.println("Draw source: " + sourceVertex.getDrawEdges() + "\nDraw dest: " + destVertex.getDrawEdges());
            return;
        }
        else if((sourceVertex.getGraphics() == null) || (destVertex.getGraphics() == null))
            return;
        else if(node == null) {
            System.out.println("Error! The node for this edge does not exist.");
            return;
        }
        else if (this.sourceVertex == this.destVertex)
        {
            /*System.out.println("Error in Edge.drawGraph(): The source and destination vertices are the same.");
            System.out.println(this.source +"---"+ this.dest);
            System.out.println(this.sourceVertex.getLabel() +"---"+ this.destVertex.getLabel());
            System.out.println(this.getType());
            return;*/
            this.node = node;
            drawLoop(node);
            return;
        }
        else if (sourceVertex.getX() == destVertex.getX() && sourceVertex.getY() == destVertex.getY())
        {
            System.out.println("Error in Edge.drawGraph(): The two vertices are at the same location.");
            System.out.println(this.sourceVertex.getId() + " --- " + this.destVertex.getId());
            System.out.println(this.sourceVertex.getLabel() + " --- " + this.destVertex.getLabel());
            System.out.println(this.getType());
            return;
        }

        //System.out.println("Passed checks for drawing edge: " + this.getID());
        this.node = node;
        GUINode sourceNode = sourceVertex.getGraphics();
        GUINode destNode = destVertex.getGraphics();
        Line line = GUINode.getLine(sourceNode, destNode);
        if (this.getType() == EDGE_TYPE.EDGE_DUMMY)
        {
            line.getStrokeDashArray().addAll(5d, 4d);
        }

        line.setStrokeWidth(destVertex.getGraphics().getRect().getStrokeWidth());
        if(!colorIsSet) {
            colorIsSet = true;
            if (destVertex.getY() >= sourceVertex.getY()) {
                color = downwardColor;
            } else {
                color = upwardColor;
            }
        }
        line.setStroke(color);


        // Compute arrowhead
        double angle = Math.PI + Math.atan2(line.getEndY() - line.getStartY(), line.getEndX() - line.getStartX());
        // TODO: Adjust arrowLength by scale
        double arrowLength = Math.min(10, arrowLengthRatio * destVertex.getGraphics().getRect().getWidth());

        double destEnterX = line.getEndX();
        double destEnterY = line.getEndY();
        double x1 = destEnterX;
        double y1 = destEnterY;
        double x2 = destEnterX + arrowLength * Math.cos(angle + arrowheadAngleDiff);
        double y2 = destEnterY + arrowLength * Math.sin(angle + arrowheadAngleDiff);
        double x3 = destEnterX + arrowLength * Math.cos(angle - arrowheadAngleDiff);
        double y3 = destEnterY + arrowLength * Math.sin(angle - arrowheadAngleDiff);

        arrowhead = new Polygon();
        arrowhead.getPoints().addAll(new Double[]{
                x1, y1,
                x2, y2,
                x3, y3 });
        arrowhead.setFill(color);

        this.edgePath = line;
        this.graphics.getChildren().removeAll(this.graphics.getChildren());
        this.graphics.getChildren().add(edgePath);
        this.graphics.getChildren().add(arrowhead);

        node.getChildren().add(graphics);
        graphics.setVisible(node.getVertex().isExpanded() && this.sourceVertex.isEdgeVisible()
                && this.destVertex.isEdgeVisible() && this.isVisible());
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
        System.out.println("Drawing loop for vertex: " + this.sourceVertex.getId());
        System.out.println("Rect bounds in node: " + sourceVertex.getGraphics().getRect().getBoundsInParent());
        Path path = new Path();
        Bounds bounds = sourceVertex.getGraphics().getRectBoundsInParent();
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

        //System.out.println("Adding edge for vertices: " + this.sourceVertex.getId() + ", " + this.destVertex.getId());
        node.getChildren().add(graphics);
        graphics.setVisible(node.getVertex().isExpanded() && this.sourceVertex.isEdgeVisible() && this.isVisible());
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
                if (v.getId() == e.sourceVertex.getId() || v.getId() == e.destVertex.getId())
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
