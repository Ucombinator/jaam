package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.GraphicsStatus;


public class TransitionFactory {

    public Transition build(Node n, GraphicsStatus gs){
        return createTransition(n, gs);
    }

    private ParallelTransition createTransition(Node n, GraphicsStatus gs){
        ParallelTransition pt;
        Duration time = Duration.millis(1000);

        FadeTransition ft = new FadeTransition(time);
            ft.setToValue(gs.getOpacity());

        if(gs instanceof GUINodeStatus) {
            GUINodeStatus guiStatus = (GUINodeStatus)gs;

            TranslateTransition tt = new TranslateTransition(time);
            tt.setToX(guiStatus.getX());
            tt.setToY(guiStatus.getY());

            ScaleTransition st = new ScaleTransition(time);

            st.setToX(guiStatus.getTotalScaleX());
            st.setToY(guiStatus.getTotalScaleY());

            pt = new ParallelTransition(n,ft,tt,st);
        }else{
            pt = new ParallelTransition(n,ft);
        }

        return pt;
    }

}
