package org.ucombinator.jaam.visualizer.gui;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.main.Main;

public class GUINode extends Pane
{
    public enum ShapeType {RECTANGLE, CIRCLE, DIAMOND};
    private ShapeType shape;

    private static final double TEXT_VERTICAL_PADDING = 15;
    private static final double TEXT_HORIZONTAL_PADDING = 15;
    private double dragStartX, dragStartY;
    private Rectangle rect, highlightingRect;
    private Text rectLabel;
    private AbstractLayoutVertex vertex;
    private GUINode parent;

    private boolean isDragging;
    private double totalScaleX;
    private double totalScaleY;

    
    public GUINode(GUINode parent, AbstractLayoutVertex v)
    {
        super();
        this.parent = parent;
        this.vertex = v;
        this.vertex.setGraphics(this);
        shape = v.getShape();
        
        this.rect = new Rectangle();
        this.rectLabel = new Text(v.getId() + ", " + v.getLoopHeight());
        this.rectLabel.setVisible(v.isLabelVisible());

        this.highlightingRect = new Rectangle();
        this.highlightingRect.setVisible(false);
        this.highlightingRect.setStroke(javafx.scene.paint.Color.BLUE);
        this.highlightingRect.setFill(javafx.scene.paint.Color.WHITE);
        this.highlightingRect.setStrokeWidth(10);

        if(v instanceof LayoutRootVertex) {
            this.getChildren().add(this.rect);
        } else {
            this.getChildren().addAll(this.highlightingRect, this.rect, this.rectLabel);
        }

        this.rectLabel.setTranslateX(TEXT_HORIZONTAL_PADDING);
        this.rectLabel.setTranslateY(TEXT_VERTICAL_PADDING);

        this.isDragging = false;
        this.totalScaleX = 1;
        this.totalScaleY = 1;

        this.setOnMousePressed(event -> {
            event.consume();

            double scaleFactorX1 = Main.getSelectedVizPanel().getPanelRoot().getGraphics().getScaleX();
            double scaleFactorY1 = Main.getSelectedVizPanel().getPanelRoot().getGraphics().getScaleY();

            if (this.getVertex() instanceof LayoutRootVertex) {
                dragStartX = event.getScreenX() - this.getTranslateX();
                dragStartY = event.getScreenY() - this.getTranslateY();
            } else {
                dragStartX = event.getScreenX() / scaleFactorX1 - this.getTranslateX();
                dragStartY = event.getScreenY() / scaleFactorY1 - this.getTranslateY();
            }
        });

        this.setOnMouseDragged(event -> {
            event.consume();

            this.isDragging = true;
            double scaleFactorX = Main.getSelectedVizPanel().getPanelRoot().getGraphics().getScaleX();
            double scaleFactorY = Main.getSelectedVizPanel().getPanelRoot().getGraphics().getScaleY();

            double offsetX, offsetY;
            if(this.getParentNode() != null) {
                offsetX = event.getScreenX() / scaleFactorX - dragStartX;
                offsetY = event.getScreenY() / scaleFactorY - dragStartY;
                Bounds thisBounds = this.rect.getBoundsInLocal();
                double thisWidth = thisBounds.getWidth();
                double thisHeight = thisBounds.getHeight();

                Bounds parentBounds = this.getParentNode().rect.getBoundsInLocal();
                double maxOffsetX = parentBounds.getWidth() - thisWidth;
                double maxOffsetY = parentBounds.getHeight() - thisHeight;

                // This truncation of the offset confines our box to its parent.
                if (offsetX < 0)
                    offsetX = 0;
                else if (offsetX > maxOffsetX)
                    offsetX = maxOffsetX;

                if (offsetY < 0)
                    offsetY = 0;
                else if (offsetY > maxOffsetY)
                    offsetY = maxOffsetY;
            }
            else {
                offsetX = event.getScreenX() - dragStartX;
                offsetY = event.getScreenY() - dragStartY;
            }

            this.setTranslateLocation(offsetX, offsetY);

            AbstractLayoutVertex v1 = this.vertex;
            VizPanel mainPanel = Main.getSelectedVizPanel();
            v1.setX(mainPanel.invScaleX(offsetX));
            v1.setY(mainPanel.invScaleY(offsetY));
            LayoutEdge.redrawEdges(v1, false);
        });

        this.setOnMouseReleased(event -> {
            event.consume();
            this.isDragging = false;
        });

        this.setOnMouseEntered(event -> {
            event.consume();
            if (vertex.getSelfGraph() != null)
            {
                for(LayoutEdge e : vertex.getSelfGraph().getEdges())
                {
                    if(e.getSource() == vertex || e.getDest() == vertex)
                    {
                        e.highlightEdgePath();
                    }
                }
            }
        });

        this.setOnMouseExited(event -> {
            event.consume();
            //getChildren().remove(rectLabel);

            if(vertex.getSelfGraph() != null)
            {
                for(LayoutEdge e : vertex.getSelfGraph().getEdges())
                {
                    if (e.getSource() == vertex || e.getDest() == vertex)
                    {
                        e.resetEdgePath();
                    }
                }
            }
        });

        if(!(this.vertex instanceof LayoutRootVertex)) {
            this.setOnMouseClicked(new AnimationHandler());
        }

        this.setVisible(true);
    }

    public AbstractLayoutVertex getVertex() {
        return vertex;
    }

    public void setVertex(AbstractLayoutVertex vertex) {
        this.vertex = vertex;
    }

    public String toString()
    {
        return rectLabel.getText().toString();
    }

    public void setLabel(String text)
    {
        this.rectLabel.setText(text);
    }

    public void setFill(Paint c) {
        this.rect.setFill(c);
    }

    public void setStroke(Color c)
    {
        this.rect.setStroke(c);
    }

    public void setStrokeWidth(double strokeWidth)
    {
        this.rect.setStrokeWidth(strokeWidth);
    }

    public void setArcHeight(double height)
    {
        this.rect.setArcHeight(height);
        this.highlightingRect.setArcHeight(height);
    }

    public void setArcWidth(double width)
    {
        this.rect.setArcWidth(width);
        this.highlightingRect.setArcWidth(width);
    }

    public void setTranslateLocation(double x, double y) {
        this.setTranslateX(x);
        this.setTranslateY(y);
        this.rectLabel.setTranslateX(TEXT_HORIZONTAL_PADDING);
        this.rectLabel.setTranslateY(TEXT_VERTICAL_PADDING);
    }

    public void setTranslateLocation(double x, double y, double width, double height)
    {
        this.setTranslateX(x);
        this.setTranslateY(y);
        this.setMaxWidth(width);
        this.setMaxHeight(height);

        this.rect.setWidth(width);
        this.rect.setHeight(height);
        
        this.highlightingRect.setWidth(width);
        this.highlightingRect.setHeight(height);

        this.rectLabel.setTranslateX(TEXT_HORIZONTAL_PADDING);
        this.rectLabel.setTranslateY(TEXT_VERTICAL_PADDING);
    }

    // Returns the bounding box for just the rectangle in the coordinate system for the parent of our node.
    public Bounds getRectBoundsInParent() {
        Bounds nodeBounds = this.getBoundsInParent();
        Bounds nodeBoundsLocal = this.getBoundsInLocal();
        Bounds rectBounds = this.rect.getBoundsInParent();
        return new BoundingBox(nodeBounds.getMinX() + rectBounds.getMinX() - nodeBoundsLocal.getMinX(),
                nodeBounds.getMinY() + rectBounds.getMinY() - nodeBoundsLocal.getMinY(), rectBounds.getWidth(), rectBounds.getHeight());
    }

    public void printLocation() {
        Bounds bounds = this.getBoundsInParent();
        System.out.println("Node x = " + bounds.getMinX() + ", " + bounds.getMaxX());
        System.out.println("Node y = " + bounds.getMinY() + ", " + bounds.getMaxY());
    }

    // Halve the distance from the current opacity to 1.
    public void increaseOpacity()
    {
        this.rect.setOpacity((1 + this.rect.getOpacity()) / 2.0);    
    }

    // Halve the current opacity.
    public void decreaseOpacity()
    {
        this.rect.setOpacity((this.rect.getOpacity()) / 2.0);
    }

    // The next two functions compute the shift that must be applied to keep the
    // top left corner stationary when the node is scaled about its center.
    public double getXShift()
    {
        double currentWidth = this.getScaleX() * this.vertex.getWidth();
        double oldWidth = this.vertex.getWidth();
        return (oldWidth - currentWidth) / 2;
        //return 0;
    }

    public double getYShift()
    {
        double currentHeight = this.getScaleY() * this.vertex.getHeight();
        double oldHeight = this.vertex.getHeight();
        return (oldHeight - currentHeight) / 2;
    }

    public static Line getLine(GUINode sourceNode, GUINode destNode) {
        if(sourceNode == null || destNode == null) {
            System.out.println("This should never happen!");
            return new Line(0, 0, 0, 0);
        }
        else {
            Point2D sourceIntersect = sourceNode.getLineIntersection(destNode);
            Point2D destIntersect = destNode.getLineIntersection(sourceNode);

            return new Line(sourceIntersect.getX(), sourceIntersect.getY(),
                    destIntersect.getX(), destIntersect.getY());
        }
    }

    private Point2D getLineIntersection(GUINode otherNode) {
        if(this.shape == ShapeType.RECTANGLE)
            return this.getRectangleLineIntersection(otherNode);
        else if(this.shape == ShapeType.DIAMOND)
            return this.getDiamondLineIntersection(otherNode);
        else // this.shape == ShapeType.CIRCLE
            return this.getCircleLineIntersection(otherNode);
    }

    // TODO: Fill in these two functions
    private Point2D getCircleLineIntersection(GUINode otherNode) {
        return new Point2D(0, 0);
    }

    private Point2D getDiamondLineIntersection(GUINode otherNode) {
        return new Point2D(0, 0);
    }

    private Point2D getRectangleLineIntersection(GUINode otherNode) {
        Bounds sourceBounds = this.getRectBoundsInParent();
        Bounds destBounds = otherNode.getRectBoundsInParent();

        double sourceCenterX = (sourceBounds.getMinX() + sourceBounds.getMaxX()) / 2.0;
        double sourceCenterY = (sourceBounds.getMinY() + sourceBounds.getMaxY()) / 2.0;
        double destCenterX = (destBounds.getMinX() + destBounds.getMaxX()) / 2.0;
        double destCenterY = (destBounds.getMinY() + destBounds.getMaxY()) / 2.0;
        double sourceExitX, sourceExitY;

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

        return new Point2D(sourceExitX, sourceExitY);
    }

    // This doesn't work, and we don't know why.
    /*private Point2D getLineIntersection2(GUINode otherNode) {
        Bounds sourceBounds = this.getRectBoundsInParent();
        Bounds destBounds = otherNode.getRectBoundsInParent();
        System.out.println("\nSource bounds: " + sourceBounds.toString());
        System.out.println("Dest bounds: " + destBounds.toString());

        double sourceCenterX = (sourceBounds.getMinX() + sourceBounds.getMaxX()) / 2.0;
        double sourceCenterY = (sourceBounds.getMinY() + sourceBounds.getMaxY()) / 2.0;
        double destCenterX = (destBounds.getMinX() + destBounds.getMaxX()) / 2.0;
        double destCenterY = (destBounds.getMinY() + destBounds.getMaxY()) / 2.0;
        Line line = new Line(sourceCenterX, sourceCenterY, destCenterX, destCenterY);
        line.setStrokeWidth(5);
        System.out.println("Line: " + line.toString());

        // Get all points on intersection  between node and line
        Rectangle rect = new Rectangle(sourceBounds.getMinX(), sourceBounds.getMinY(),
                sourceBounds.getMaxX(), sourceBounds.getMaxY());
        Path intersection = (Path) Shape.intersect(line, rect);
        System.out.println(intersection.toString());
        ArrayList<Point2D> points = new ArrayList<Point2D>();
        for(PathElement elem : intersection.getElements()) {
            if(elem instanceof MoveTo) {
                MoveTo moveElem = (MoveTo) elem;
                points.add(new Point2D(moveElem.getX(), moveElem.getY()));
            }
            else if(elem instanceof LineTo) {
                LineTo lineElem = (LineTo) elem;
                points.add(new Point2D(lineElem.getX(), lineElem.getY()));
            }
        }

        // Sort points by distance from center, and take the last one
        Point2D center = new Point2D(sourceCenterX, sourceCenterY);
        Collections.sort(points, new Comparator<Point2D>() {
            @Override
            public int compare(Point2D p1, Point2D p2) {
                double distance1 = p1.distance(center);
                double distance2 = p2.distance(center);

                return ((Double) distance1).compareTo(distance2);
            }
        });

        //System.out.println("Points found: " + points.size());
        if(points.size() == 0) {
            System.out.println("Error: No points on path for edge from vertex " + this.vertex.getId() + " to " + otherNode.vertex.getId());
            return new Point2D(0, 0);
        }
        else {
            Point2D result = points.get(points.size() - 1);
            System.out.println("Returning point: " + result.toString());
            return result;
        }
    }*/

    public GUINode getParentNode() {
        return this.parent;
    }

    public double getTotalParentScaleX() {
        if (this.parent != null)
            return this.parent.totalScaleX;
        else return 1;
    }

    public double getTotalParentScaleY() {
        if (this.parent != null)
            return this.parent.totalScaleY;
        else return 1;
    }

    public void setTotalScaleX(double scale) {
        this.totalScaleX = scale;
    }

    public double getTotalScaleX() {
        return this.totalScaleX;
    }

    public void setTotalScaleY(double scale) {
        this.totalScaleY = scale;
    }

    public double getTotalScaleY() {
        return this.totalScaleY;
    }

    public void setLabelVisible(boolean isLabelVisible) {
        vertex.setLabelVisible(isLabelVisible);
        this.rectLabel.setVisible(isLabelVisible);
    }

    public Rectangle getHighlightingRect() {
        return this.highlightingRect;
    }
    
    public Rectangle getRect(){
        return this.rect;
    }
}
