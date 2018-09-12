package org.ucombinator.jaam.visualizer.gui;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.taint.TaintMethodVertex;

public class GUITaintMethodNode<T extends AbstractLayoutVertex> extends GUINode {

    private BorderPane borderPane;
    private HBox inputs, outputs;
    private Rectangle center;
    private Label label;


    public GUITaintMethodNode(GUINode parent, TaintMethodVertex v) {
        super(parent, (T)v);

        //this.rect.setVisible(false);

        inputs = new HBox();
        outputs = new HBox();
        center = new Rectangle();
        borderPane = new BorderPane();

        borderPane.setTop(inputs);
        borderPane.setBottom(outputs);
        borderPane.setCenter(center);
    }
}
