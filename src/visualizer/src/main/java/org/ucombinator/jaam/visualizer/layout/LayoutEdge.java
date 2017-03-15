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
    Group graphics;
    Line line;
    Polygon arrowhead;
    Rectangle marker1, marker2;
    static final boolean markCenters = false;

    public enum EDGE_TYPE {EDGE_REGULAR, EDGE_DUMMY};
    private EDGE_TYPE type;

    int source, dest;
    private AbstractLayoutVertex sourceVertex, destVertex;
    private GUINode node;
    protected String strId;

    static double defaultStrokeWidth = 1;
    static double arrowheadAngleDiff = 0.15 * Math.PI;
    public static double arrowLength = 10;

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

    public void draw(VizPanel panel, GUINode node)
    {
        if (this.source == this.dest)
        {
			/*System.out.println("Error in Edge.draw(): The source and destination vertices are the same.");
			System.out.println(this.source +"---"+ this.dest);
			System.out.println(this.sourceVertex.getLabel() +"---"+ this.destVertex.getLabel());
			System.out.println(this.getType());*/
            return;
        }
        else if (sourceVertex.getX() == destVertex.getX() && sourceVertex.getY() == destVertex.getY())
        {
			/*System.out.println("Error in Edge.draw(): The two vertices are at the same location.");
			System.out.println(this.source + " --- " + this.dest);
			System.out.println(this.sourceVertex.getLabel() + " --- " + this.destVertex.getLabel());
			System.out.println(this.getType());*/
            return;
        }
        else if(!sourceVertex.getDrawEdges() || !destVertex.getDrawEdges()) {
            return;
        }
        else if((sourceVertex.getGraphics() == null) || (destVertex.getGraphics() == null))
            return;

        this.node = node;
        GUINode sourceNode = sourceVertex.getGraphics();
        GUINode destNode = destVertex.getGraphics();
        Bounds sourceBounds = sourceNode.getBoundsInParent();
        Bounds destBounds = destNode.getBoundsInParent();
        double sourceCenterX = (sourceBounds.getMinX() + sourceBounds.getMaxX()) / 2.0;
        double sourceCenterY = (sourceBounds.getMinY() + sourceBounds.getMaxY()) / 2.0;
        double destCenterX = (destBounds.getMinX() + destBounds.getMaxX()) / 2.0;
        double destCenterY = (destBounds.getMinY() + destBounds.getMaxY()) / 2.0;
        double sourceExitX, sourceExitY, destEnterX, destEnterY;

        // To find which side a line exits from, we compute both diagonals of the rectangle and determine whether
        // the other end lies above or below each diagonal. The positive diagonal uses the positive slope, and the
        // negative diagonal uses the negative slope.
        // Keep in mind that the increasing y direction is downward.
        double startDiagSlope = sourceBounds.getHeight() / sourceBounds.getWidth();
        double startInterceptPos = sourceCenterY - sourceCenterX * startDiagSlope;
        double startInterceptNeg = sourceCenterY + sourceCenterX * startDiagSlope;
        boolean aboveStartPosDiag = (destCenterX * startDiagSlope + startInterceptPos > destCenterY);
        boolean aboveStartNegDiag = (-destCenterX * startDiagSlope + startInterceptNeg > destCenterY);

        if (aboveStartPosDiag && aboveStartNegDiag)
        {
            // Top
            double invSlope = (destCenterX - sourceCenterX) / (destCenterY - sourceCenterY);
            sourceExitY = sourceBounds.getMinY();
            sourceExitX = sourceCenterX + invSlope * (sourceExitY - sourceCenterY);
        }
        else if (!aboveStartPosDiag && aboveStartNegDiag)
        {
            // Left
            double slope = (destCenterY - sourceCenterY) / (destCenterX - sourceCenterX);
            sourceExitX = sourceBounds.getMinX();
            sourceExitY = sourceCenterY + slope * (sourceExitX - sourceCenterX);
        }
        else if (aboveStartPosDiag && !aboveStartNegDiag)
        {
            // Right
            double slope = (destCenterY - sourceCenterY) / (destCenterX - sourceCenterX);
            sourceExitX = sourceBounds.getMaxX();
            sourceExitY = sourceCenterY + slope * (sourceExitX - sourceCenterX);
        }
        else
        {
            // Bottom
            double invSlope = (destCenterX - sourceCenterX) / (destCenterY - sourceCenterY);
            sourceExitY = sourceBounds.getMaxY();
            sourceExitX = sourceCenterX + invSlope * (sourceExitY - sourceCenterY);
        }

        double destDiagSlope = destBounds.getHeight() / destBounds.getWidth();
        double destInterceptPos = destCenterY - destCenterX * destDiagSlope;
        double destInterceptNeg = destCenterY + destCenterX * destDiagSlope;
        boolean aboveDestPosDiag = (sourceCenterX * destDiagSlope + destInterceptPos > sourceCenterY);
        boolean aboveDestNegDiag = (-sourceCenterX * destDiagSlope + destInterceptNeg > sourceCenterY);

        if (aboveDestPosDiag && aboveDestNegDiag)
        {
            // Top
            double invSlope = (sourceCenterX - destCenterX) / (sourceCenterY - destCenterY);
            destEnterY = destBounds.getMinY();
            destEnterX = destCenterX + invSlope * (destEnterY - destCenterY);
        }
        else if(!aboveDestPosDiag && aboveDestNegDiag)
        {
            // Left
            double slope = (sourceCenterY - destCenterY) / (sourceCenterX - destCenterX);
            destEnterX = destBounds.getMinX();
            destEnterY = destCenterY + slope * (destEnterX - destCenterX);
        }
        else if (aboveDestPosDiag && !aboveDestNegDiag)
        {
            // Right
            double slope = (sourceCenterY - destCenterY) / (sourceCenterX - destCenterX);
            destEnterX = destBounds.getMaxX();
            destEnterY = destCenterY + slope * (destEnterX - destCenterX);
        }
        else
        {
            // Bottom
            double invSlope = (sourceCenterX - destCenterX) / (sourceCenterY - destCenterY);
            destEnterY = destBounds.getMaxY();
            destEnterX = destCenterX + invSlope * (destEnterY - destCenterY);
        }

        this.line = new Line(sourceExitX, sourceExitY, destEnterX, destEnterY);
        if (this.getType() == EDGE_TYPE.EDGE_DUMMY)
        {
            line.getStrokeDashArray().addAll(5d, 4d);
        }
        line.setStrokeWidth(defaultStrokeWidth);

        // Compute arrowhead
        double angle = Math.PI + Math.atan2(destEnterY - sourceExitY, destEnterX - sourceExitX);

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
        //System.out.println("Arrowhead points: " + arrowhead.toString());


        this.graphics.getChildren().removeAll(this.graphics.getChildren());

        // Mark the center of each node for testing
        if(markCenters)
        {
            marker1 = getMarker(sourceCenterX, sourceCenterY);
            marker2 = getMarker(destCenterX, destCenterY);
            this.graphics.getChildren().add(marker1);
            this.graphics.getChildren().add(marker2);
        }

        this.graphics.getChildren().add(line);
        this.graphics.getChildren().add(arrowhead);

        if(node != null)
            node.getChildren().add(graphics);
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
                    e.draw(Parameters.stFrame.mainPanel, e.node);
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

    public void setVisible(boolean isVisible)
    {
        this.graphics.setVisible(isVisible);
    }

    public void setScale()
    {
        if(this.node != null) {
            // Make the line for our edge thinner
            double zoomLevel = Parameters.stFrame.mainPanel.getZoomLevel();
            line.setStrokeWidth(defaultStrokeWidth / zoomLevel);

            // TODO: The arrowhead will scale around the center, not the tip.
            // It could be shifted, but that might not make much difference in the end.
            this.arrowhead.setScaleX(1.0 / zoomLevel);
            this.arrowhead.setScaleY(1.0 / zoomLevel);
        }
    }
}

