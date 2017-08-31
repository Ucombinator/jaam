package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;


public class TransitionFactory {

    public Transition build(Node n, GUINodeStatus gs){
        return createTransition(n, gs);
    }

    private ParallelTransition createTransition(Node n, GUINodeStatus gs){
        FadeTransition ft = new FadeTransition(Duration.millis(200));
        ft.setToValue(gs.getOpacity());

        //TranslateTransition tt = new TranslateTransition(Duration.millis(1000));
        //    tt.setFromX(n.getLayoutX()); tt.setToX(gs.getX());
        //    tt.setFromY(n.getLayoutY()); tt.setToX(gs.getY());

        return new ParallelTransition(n,ft);
    }

}
