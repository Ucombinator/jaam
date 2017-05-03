package org.ucombinator.jaam.visualizer.layout;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import org.ucombinator.jaam.visualizer.graph.AbstractVertex;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.VizPanel;
import org.ucombinator.jaam.visualizer.main.Parameters;

public class LayoutEdge implements Comparable<org.ucombinator.jaam.visualizer.layout.LayoutEdge>
{
    private Group graphics;
    private Line line;
    private Polygon arrowhead;
    private Rectangle marker1, marker2;
    private static final boolean markCenters = false;

    public enum EDGE_TYPE {EDGE_REGULAR, EDGE_DUMMY};
    private EDGE_TYPE type;

    private int source, dest;
    private AbstractLayoutVertex sourceVertex, destVertex;
    private GUINode node;
    private String strId;

    private static double defaultStrokeWidth = 1;
    private static double arrowheadAngleDiff = 0.15 * Math.PI;
    private static double arrowLengthRatio = 0.5;

    private LayoutEdge(int source, int dest)
    {
        this.strId = LayoutEdge.createID(source, dest);
        this.source = source;
        this.dest = dest;
    }

    public LayoutEdge(AbstractLayoutVertex sourceVertex, AbstractLayoutVertex destVertex, EDGE_TYPE edgeType)
    {
        this(sourceVertex.getId(), destVertex.getId());
        this.type = edgeType;
        this.sourceVertex = sourceVertex;
        this.destVertex = destVertex;
        this.sourceVertex.addOutgoingNeighbor(this.destVertex);
        this.destVertex.addIncomingNeighbor(this.sourceVertex);

        graphics = new Group();
        line = new Line();
        arrowhead = new Polygon();
        //System.out.println("Created new layout edge: " + this.getID());
    }

    public EDGE_TYPE getType() {
        return type;
    }

    public static String createID(int source, int dest) {
        return "edge:" + source + "-->" + dest;
    }

    public String getID() {
        return strId;
    }

    public int compareTo(org.ucombinator.jaam.visualizer.layout.LayoutEdge otherEdge) {
        return ((Integer) source).compareTo(otherEdge.source);
    }

    public AbstractLayoutVertex getSourceVertex() {
        return sourceVertex;
    }

    public AbstractLayoutVertex getDestVertex() {
        return destVertex;
    }

    public void draw(GUINode node)
    {
        //System.out.println("Drawing edges for " + node.getVertex().toString());
        if (this.source == this.dest)
        {
			System.out.println("Error in Edge.drawGraph(): The source and destination vertices are the same.");
			System.out.println(this.source +"---"+ this.dest);
			System.out.println(this.sourceVertex.getLabel() +"---"+ this.destVertex.getLabel());
			System.out.println(this.getType());
            return;
        }
        else if (sourceVertex.getX() == destVertex.getX() && sourceVertex.getY() == destVertex.getY())
        {
			System.out.println("Error in Edge.drawGraph(): The two vertices are at the same location.");
			System.out.println(this.source + " --- " + this.dest);
			System.out.println(this.sourceVertex.getLabel() + " --- " + this.destVertex.getLabel());
			System.out.println(this.getType());
            return;
        }
        else if(!sourceVertex.getDrawEdges() || !destVertex.getDrawEdges()) {
            System.out.println("Draw source: " + sourceVertex.getDrawEdges() + "\nDraw dest: " + destVertex.getDrawEdges());
            return;
        }
        else if((sourceVertex.getGraphics() == null) || (destVertex.getGraphics() == null))
            return;
        else if(node == null) {
            System.out.println("Error! The node for this edge does not exist.");
            return;
        }

        //System.out.println("Passed checks for drawing edge: " + this.getID());
        this.node = node;
        GUINode sourceNode = sourceVertex.getGraphics();
        GUINode destNode = destVertex.getGraphics();
        this.line = GUINode.getLine(sourceNode, destNode);
        if (this.getType() == EDGE_TYPE.EDGE_DUMMY)
        {
            line.getStrokeDashArray().addAll(5d, 4d);
        }
        line.setStrokeWidth(destVertex.getGraphics().getRect().getStrokeWidth());

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
        arrowhead.setFill(Color.BLACK);

        this.graphics.getChildren().removeAll(this.graphics.getChildren());
        this.graphics.getChildren().add(line);
        this.graphics.getChildren().add(arrowhead);

        node.getChildren().add(graphics);
        graphics.setVisible(node.getVertex().isExpanded() && this.sourceVertex.isEdgeVisible()
                && this.destVertex.isEdgeVisible() && this.isVisible());
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
            for (LayoutEdge e : v.getSelfGraph().getEdges().values())
            {
                if (v.getId() == e.source || v.getId() == e.dest)
                {
                    // Clear current graphics...
                    if(markCenters) {
                        e.graphics.getChildren().remove(e.marker1);
                        e.graphics.getChildren().remove(e.marker2);
                    }

                    e.graphics.getChildren().remove(e.line);
                    e.graphics.getChildren().remove(e.arrowhead);

                    if(e.node != null)
                        e.node.getChildren().remove(e.graphics);

                    // ...And draw new ones
                    e.draw(e.node);
                }
            }
        }

        if(recurse)
        {
            for (AbstractLayoutVertex w : v.getInnerGraph().getVertices().values())
                redrawEdges(w, recurse);
        }
    }

    public Line getLine()
    {
        return this.line;
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
