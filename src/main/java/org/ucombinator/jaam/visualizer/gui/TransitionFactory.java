package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.ucombinator.jaam.visualizer.layout.GraphicsStatus;



public class TransitionFactory {



    public ParallelTransition build(Node n, GraphicsStatus gs){


        ParallelTransition pt;
        Duration time = Duration.millis(1000);

        FadeTransition ft = new FadeTransition(time);
        ft.setToValue(gs.getOpacity());

        if (gs instanceof GUINodeStatus) {
            GUINodeStatus guiStatus = (GUINodeStatus)gs;
            Rectangle rect = ((GUINode) n).getRect();

            TranslateTransition tt = new TranslateTransition(time);
            tt.setToX(guiStatus.getX());
            tt.setToY(guiStatus.getY());

            Timeline widthTimeline = new Timeline(new KeyFrame(time, new KeyValue(rect.widthProperty(), guiStatus.getWidth())));
            Timeline heightTimeline = new Timeline(new KeyFrame(time, new KeyValue(rect.heightProperty(), guiStatus.getHeight())));

            pt = new ParallelTransition(n,ft,tt,widthTimeline,heightTimeline);
        } else {
            pt = new ParallelTransition(n,ft);
        }

        return pt;
    }

}
