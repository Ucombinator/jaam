package org.ucombinator.jaam.visualizer.layout;

import java.util.Iterator;

import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.StacFrame;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

public class AnimationHandler implements javafx.event.EventHandler<javafx.scene.input.MouseEvent>
{
    public static int transitionTime = 300; // Milliseconds per transition

    @Override
    public void handle(MouseEvent event) {
        EventType<MouseEvent> type = (EventType<MouseEvent>) event.getEventType();
        if(type.equals(MouseEvent.MOUSE_CLICKED))
        {
            if(event.getButton().equals(MouseButton.PRIMARY))
            {
                switch (event.getClickCount())
                {
                    case 1:
                        handlePrimarySingleClick(event);
                        break;
                    case 2:
                        handlePrimaryDoubleClick(event);
                        break;
                    default:
                        break;
                }
            }
            else if(event.getButton().equals(MouseButton.SECONDARY)) {}
            else if(event.getButton().equals(MouseButton.MIDDLE)) {}
        }
        else
        {
            System.out.println("This line should never be printed since we add the handler by setOnMouseClicked");
        }
    }
    
    private void collapsing(AbstractLayoutVertex v)
    {
        //System.out.println("\nCollapsing node: " + v.getId() + ", " + v.getGraphics().toString());
        Iterator<Node> it = v.getGraphics().getChildren().iterator();

        // Fade edges out?
        /*while(it.hasNext())
        {
            final Node n = it.next();
            if(!n.getClass().equals(Rectangle.class))
            {
                FadeTransition ft = new FadeTransition(Duration.millis(transitionTime), n);
                ft.setToValue(0.0);
                
                ft.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {        
                        n.setVisible(false);
                    }
                });
                
                ft.play();
            }
        }*/

        v.setExpanded(false);
        VizPanel panel = Main.getSelectedMainPanel();
        final AbstractLayoutVertex panelRoot = panel.getPanelRoot();
        panel.resetContent();
        LayoutAlgorithm.layout(panelRoot);
        panel.drawGraph();

        /*ParallelTransition pt = new ParallelTransition();
        animateRecursive(panelRoot, pt, panel);
        pt.play();

        pt.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                LayoutEdge.redrawEdges(panelRoot, true);
            }
        });*/
    }

    private void expanding(AbstractLayoutVertex v)
    {
        //System.out.println("\nExpanding node: " + v.getId() + ", " + v.getGraphics().toString());
        Iterator<Node> it = v.getGraphics().getChildren().iterator();

        // Fade edges in?
        /*while(it.hasNext())
        {
            final Node n = it.next();
            if(!n.getClass().equals(Rectangle.class))
            {
                FadeTransition ft = new FadeTransition(Duration.millis(transitionTime), n);
                ft.setToValue(1.0);
                
                ft.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {        
                        n.setVisible(true);
                    }
                });
                
                ft.play();
            }
        }*/

        v.setExpanded(true);
        VizPanel panel = Main.getSelectedMainPanel();
        final AbstractLayoutVertex panelRoot = panel.getPanelRoot();
        panel.resetContent();
        LayoutAlgorithm.layout(panelRoot);
        panel.drawGraph();

        //ParallelTransition pt = new ParallelTransition();
        //animateRecursive(panelRoot, pt, panel);
        //pt.play();

        //pt.setOnFinished(new EventHandler<ActionEvent>() {
        //    @Override
        //    public void handle(ActionEvent event) {
                //LayoutEdge.redrawEdges(panelRoot, true);
        //    }
        //});
    }
    
    private void handlePrimaryDoubleClick(MouseEvent event)
    {
        AbstractLayoutVertex v = (((GUINode)(event.getSource())).getVertex());

        // Collapsing the root vertex leaves us with a blank screen.
        if(!(v instanceof LayoutRootVertex)) {
            if (v.isExpanded())
                collapsing(v);
            else
                expanding(v);
        }
                
        event.consume();
    }

    private void animateRecursive(final AbstractLayoutVertex v, ParallelTransition pt, VizPanel mainPanel)
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

        Iterator<AbstractLayoutVertex> it = v.getInnerGraph().getVertices().values().iterator();
        while(it.hasNext()){
            AbstractLayoutVertex next = it.next();
            if(v.isExpanded()) {
                animateRecursive(next, pt, mainPanel);
            }
        }
    }

    private void handlePrimarySingleClick(MouseEvent event)
    {
        event.consume();
        AbstractLayoutVertex v = ((GUINode)(event.getSource())).getVertex();

        StacFrame currentFrame = Main.getSelectedStacTabController();
        currentFrame.getMainPanel().resetHighlighted(v);
        currentFrame.getBytecodeArea().setDescription();
        currentFrame.setRightText();
    }
}
