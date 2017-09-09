package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.GraphicsStatus;


public class TransitionFactory {

    public Transition build(Node n, GraphicsStatus gs){
        return createTransition(n, gs);
    }

    private ParallelTransition createTransition(Node n, GraphicsStatus gs){
        FadeTransition ft = new FadeTransition(Duration.millis(200));
        ft.setToValue(gs.getOpacity());


        ParallelTransition pt;
        if(gs instanceof GUINodeStatus) {
            TranslateTransition tt = new TranslateTransition(Duration.millis(1000));
            GUINodeStatus guiStatus = (GUINodeStatus)gs;
            tt.setToX(guiStatus.getX());
            tt.setToY(guiStatus.getY());
            pt = new ParallelTransition(n, ft,tt);
        }else{
            pt = new ParallelTransition(n, ft);
        }

        return pt;
    }

}
