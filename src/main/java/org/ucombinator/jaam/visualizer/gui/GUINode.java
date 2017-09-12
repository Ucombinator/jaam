package org.ucombinator.jaam.visualizer.gui;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import org.ucombinator.jaam.visualizer.layout.*;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.HashMap;

public class GUINode extends Group
{
    private static final double TEXT_VERTICAL_PADDING = 15;
    private static final double TEXT_HORIZONTAL_PADDING = 15;

    private final Rectangle rect;
    private final Rectangle highlightingRect;
    private final Text rectLabel;
    private final AbstractLayoutVertex vertex;
    private final GUINode parent;

    private Point2D dragStart;

    public GUINode(GUINode parent, AbstractLayoutVertex v)
    {
        super();
        this.parent = parent;
        this.vertex = v;
        this.vertex.setGraphics(this);

        this.rect = new Rectangle();
        this.rectLabel = new Text(v.getId() + ", " + v.getLoopHeight());
        this.rectLabel.setVisible(v.isLabelVisible());

        this.highlightingRect = new Rectangle();
        this.highlightingRect.setVisible(false);
        this.highlightingRect.setStroke(javafx.scene.paint.Color.BLUE);
        this.highlightingRect.setFill(javafx.scene.paint.Color.WHITE);
        this.highlightingRect.setStrokeWidth(10);
        this.highlightingRect.widthProperty().bind(this.rect.widthProperty());
        this.highlightingRect.heightProperty().bind(this.rect.heightProperty());

        if (v instanceof LayoutRootVertex) {
            this.getChildren().add(this.rect);
        } else {
            this.getChildren().addAll(this.highlightingRect, this.rect, this.rectLabel);
        }

        this.rectLabel.setTranslateX(TEXT_HORIZONTAL_PADDING);
        this.rectLabel.setTranslateY(TEXT_VERTICAL_PADDING);

        this.rect.setArcWidth(5);
        this.rect.setArcHeight(5);
        this.highlightingRect.setArcWidth(5);
        this.highlightingRect.setArcHeight(5);

        this.setFill(v.getFill());
        this.rect.setStroke(Color.BLACK);
        this.setStrokeWidth(0.5);
        this.setOpacity(1);

        this.setOnMousePressed(this::handleOnMousePressed);
        this.setOnMouseDragged(this::handleOnMouseDragged);
        this.setOnMouseEntered(this::handleOnMouseEntered);
        this.setOnMouseExited(this::handleOnMouseExited);
        this.setOnMouseClicked(this::handleOnMouseClicked);
        this.setVisible(true);
    }

    private void handleOnMousePressed(MouseEvent event) {
        event.consume();
        this.dragStart = new Point2D(event.getX(), event.getY());
    }

    private void handleOnMouseDragged(MouseEvent event) {
        event.consume();

        double newX = this.getTranslateX() + event.getX() - this.dragStart.getX();
        double newY = this.getTranslateY() + event.getY() - this.dragStart.getY();

        // Clamp the offset to confine our box to its parent.
        if (this.getParentNode() != null) {
            Bounds thisBounds = this.rect.getBoundsInLocal();
            Bounds parentBounds = this.getParentNode().rect.getBoundsInLocal();

            newX = Math.min(Math.max(newX, 0), parentBounds.getWidth() - thisBounds.getWidth());
            newY = Math.min(Math.max(newY, 0), parentBounds.getHeight() - thisBounds.getHeight());
        }

        this.setTranslateX(newX);
        this.setTranslateY(newY);

        this.vertex.setX(newX);
        this.vertex.setY(newY);

        LayoutEdge.redrawEdges(this.vertex, false);
    }

    private void handleOnMouseEntered(MouseEvent event) {
        event.consume();
        if (vertex.getSelfGraph() != null) {
            for(LayoutEdge e : vertex.getSelfGraph().getEdges()) {
                if(e.getSource() == vertex || e.getDest() == vertex) {
                    e.highlightEdgePath();
                }
            }
        }
    }

    private void handleOnMouseExited(MouseEvent event) {
        event.consume();
        //getChildren().remove(rectLabel);

        if (vertex.getSelfGraph() != null) {
            for(LayoutEdge e : vertex.getSelfGraph().getEdges()) {
                if (e.getSource() == vertex || e.getDest() == vertex) {
                    e.resetEdgePath();
                }
            }
        }
    }

    private void handleOnMouseClicked(MouseEvent event) {
        event.consume();

        if(event.getClickCount()>1){
            handleDoubleCkick(event);
        }

        /*
        if(!(this.vertex instanceof LayoutRootVertex)) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                switch (event.getClickCount()) {
                    case 1:
                        event.consume();

                        MainTabController currentFrame = Main.getSelectedMainTabController();
                        currentFrame.vizPanelController.resetHighlighted(this.getVertex());
                        currentFrame.bytecodeArea.setDescription();
                        currentFrame.setRightText();
                        break;
                    case 2:
                        // Collapsing the root vertex leaves us with a blank screen.
                        if (!(this.getVertex() instanceof LayoutRootVertex)) {
                            if (this.getVertex().isExpanded()) {
                                collapse();
                            } else {
                                expand();
                            }
                        }

                        event.consume();
                        break;
                    default: break;
                }
            }
        }
        */
    }

    private void handleDoubleCkick(MouseEvent event){
        if(this.vertex instanceof LayoutRootVertex){
            return;
        }

        if(this.vertex.isExpanded()) {

            AbstractLayoutVertex root = Main.getSelectedVizPanelController().getPanelRoot();
            DDD d3 = Main.getSelectedVizPanelController().getDDD();
            HashMap<GraphEntity, GraphicsStatus> dbNew = d3.retrieveAllGraphicsStatus(root);


            System.out.println("Node clicked: " + this.vertex.getId());

            //Fist, we want the content of the clicked node to disappear
            for(AbstractLayoutVertex v: this.vertex.getInnerGraph().getVertices()){
                GUINodeStatus gs = (GUINodeStatus)dbNew.get(v);
                gs.setOpacity(0.0);
            }

            for(LayoutEdge e: this.vertex.getInnerGraph().getEdges()){
                dbNew.get(e).setOpacity(0.0);
            }


            d3.bind(dbNew).run(
                    new Runnable() {
                        @Override
                        public void run() {
                            //Then, we want the vertices to move to their final positions and the clicked vertex to change its size

                            GUINode.this.vertex.setCollapsed();
                            LayoutAlgorithm.layout(root);
                            System.out.println("New Layout Computed");

                            //At the end of the animation we also set the content of the subgraph of the clicked node to be invisible
                            for(AbstractLayoutVertex v: GUINode.this.vertex.getInnerGraph().getVertices()){
                                v.setVisible(false);
                            }
                            for(LayoutEdge e: GUINode.this.vertex.getInnerGraph().getEdges()){
                                e.setVisible(false);
                            }


                            HashMap<GraphEntity,GraphicsStatus> db = d3.update(root);
                            d3.bind(db).run(new Runnable() {
                                @Override
                                public void run() {
                                    LayoutEdge.redrawEdges(root, true);
                                }
                            });
                        }
                    }
            );

        }else{

        }
        if(this.vertex.isExpanded()){
            this.vertex.setCollapsed();
        }else{
            this.vertex.setExpanded();
        }
    }

//    private void collapse()
//    {
//        //System.out.println("\nCollapsing node: " + v.getId() + ", " + v.getGraphics().toString());
//        Iterator<Node> it = this.getVertex().getGraphics().getChildren().iterator();
//
//        // Fade edges out?
//        /*while(it.hasNext())
//        {
//            final Node n = it.next();
//            if(!n.getClass().equals(Rectangle.class))
//            {
//                FadeTransition ft = new FadeTransition(Duration.millis(transitionTime), n);
//                ft.setToValue(0.0);
//
//                ft.setOnFinished(new EventHandler<ActionEvent>() {
//                    @Override
//                    public void handle(ActionEvent event) {
//                        n.setVisible(false);
//                    }
//                });
//
//                ft.play();
//            }
//        }*/
//
//        this.getVertex().setCollapsed();
//        VizPanelController panel = Main.getSelectedVizPanelController();
//        final AbstractLayoutVertex panelRoot = panel.getPanelRoot();
//        panel.resetContent();
//        LayoutAlgorithm.layout(panelRoot);
//        panel.drawGraph();
//
//        /*ParallelTransition pt = new ParallelTransition();
//        animateRecursive(panelRoot, pt, panel);
//        pt.play();
//
//        pt.setOnFinished(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                LayoutEdge.redrawEdges(panelRoot, true);
//            }
//        });*/
//    }
//
//    private void expand()
//    {
//        //System.out.println("\nExpanding node: " + v.getId() + ", " + v.getGraphics().toString());
//        Iterator<Node> it = this.getVertex().getGraphics().getChildren().iterator();
//
//        // Fade edges in?
//        /*while(it.hasNext())
//        {
//            final Node n = it.next();
//            if(!n.getClass().equals(Rectangle.class))
//            {
//                FadeTransition ft = new FadeTransition(Duration.millis(transitionTime), n);
//                ft.setToValue(1.0);
//
//                ft.setOnFinished(new EventHandler<ActionEvent>() {
//                    @Override
//                    public void handle(ActionEvent event) {
//                        n.setVisible(true);
//                    }
//                });
//
//                ft.play();
//            }
//        }*/
//
//        this.getVertex().setExpanded();
//        VizPanelController panel = Main.getSelectedVizPanelController();
//        final AbstractLayoutVertex panelRoot = panel.getPanelRoot();
//        panel.resetContent();
//        LayoutAlgorithm.layout(panelRoot);
//        panel.drawGraph();
//
//        //ParallelTransition pt = new ParallelTransition();
//        //animateRecursive(panelRoot, pt, panel);
//        //pt.play();
//
//        //pt.setOnFinished(new EventHandler<ActionEvent>() {
//        //    @Override
//        //    public void handle(ActionEvent event) {
//                //LayoutEdge.redrawEdges(panelRoot, true);
//        //    }
//        //});
//    }

/*
    private static void animateRecursive(final AbstractLayoutVertex v, ParallelTransition pt, VizPanelController mainPanel)
    {
        // TODO: Move arrows as well as nodes.
        if(!(v instanceof LayoutRootVertex)) {
            boolean toPrint = (v instanceof LayoutMethodVertex);
            System.out.println("Size of node " + v.getId() + ": " + v.getWidth() + ", " + v.getHeight());
            System.out.println("Location: " + v.getX() + ", " + v.getY());
            System.out.println("Node: " + v.getGraphics());
            GUINode node = v.getGraphics();
            double newWidth = mainPanel.scaleX(v.getWidth());
            double newHeight = mainPanel.scaleY(v.getHeight());
            double currWidth = node.getWidth() * node.getTotalParentScaleX();
            double currHeight = node.getHeight() * node.getTotalParentScaleY();

            double toScaleX = newWidth / currWidth;
            double toScaleY = newHeight / currHeight;
            node.setTotalScaleX(toScaleX * node.getTotalParentScaleX());
            node.setTotalScaleY(toScaleY * node.getTotalParentScaleY());
            System.out.println(String.format("Scale X: %.3f", toScaleX));
            System.out.println(String.format("Scale Y: %.3f", toScaleY));

            // Shift to keep upper left corner in the same place after scaling
            System.out.println("Compare widths: " + currWidth + ", " + newWidth);
            System.out.println("Compare heights: " + currHeight + ", " + newHeight);
            double xShift = node.getXShift();
            double yShift = node.getYShift();
            //double xShift = 0;
            //double yShift = 0;
            System.out.println("Shift: " + xShift + ", " + yShift);
            double toX = mainPanel.scaleX(v.getX() + xShift);
            double toY = mainPanel.scaleY(v.getY() + yShift);
            System.out.println(String.format("Translate X: %.3f", toX));
            System.out.println(String.format("Translate Y: %.3f", toY));

            if (toScaleX != node.getScaleX() || toScaleY != node.getScaleY()) {
                ScaleTransition st = new ScaleTransition(Duration.millis(transitionTime), node);
                st.setToX(toScaleX);
                st.setToY(toScaleY);
                pt.getChildren().addAll(st);
            }

            if (toX != node.getTranslateX() || toY != node.getTranslateY()) {
                TranslateTransition tt = new TranslateTransition(Duration.millis(transitionTime), node);
                tt.setToX(toX);
                tt.setToY(toY);
                pt.getChildren().addAll(tt);
            }
        }

        for (AbstractLayoutVertex next : v.getInnerGraph().getVertices()) {
            if (v.isExpanded()) {
                animateRecursive(next, pt, mainPanel);
            }
        }
    }
    */

    public AbstractLayoutVertex getVertex() {
        return vertex;
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

    public void setStrokeWidth(double strokeWidth)
    {
        this.rect.setStrokeWidth(strokeWidth);
    }

    public void setTranslateLocation(double x, double y, double width, double height)
    {
        this.setTranslateX(x);
        this.setTranslateY(y);

        this.rect.setWidth(width);
        this.rect.setHeight(height);

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

    public GUINode getParentNode() {
        return this.parent;
    }

    public void setLabelVisible(boolean isLabelVisible) {
        vertex.setLabelVisible(isLabelVisible);
        this.rectLabel.setVisible(isLabelVisible);
    }

    public Rectangle getHighlightingRect() {
        return this.highlightingRect;
    }
    
    public Rectangle getRect() {
        return this.rect;
    }

}
