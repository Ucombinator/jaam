package org.ucombinator.jaam.visualizer.gui;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import org.ucombinator.jaam.visualizer.taint.TaintMethodVertex;
import org.ucombinator.jaam.visualizer.taint.TaintVertex;

import java.util.ArrayList;

public class MethodGuiNode extends GUINode{

    VBox pane;
    HBox inputPane, outPane;

    Rectangle tempRect;

    private double height;

    public MethodGuiNode(GUINode parent, TaintMethodVertex v) {
        super(parent, v); // The rect will be used for collapsing

        // But we don't need it for now
        this.getChildren().clear();

        pane = new VBox(8);
        pane.getStyleClass().add("taint-method-vbox");

        inputPane = new HBox();
        inputPane.getStyleClass().add("taint-method-hbox");
        outPane = new HBox();
        outPane.getStyleClass().add("taint-method-hbox");
        fillTopAndBottom();

        pane.getChildren().addAll(inputPane, outPane);

        this.getChildren().add(pane);

        tempRect = new Rectangle();
        tempRect.setFill(Color.RED);
        this.rect.setFill(Color.BLACK);

        //this.getChildren().add(tempRect);
        inputPane.getChildren().add(tempRect);
        outPane.getChildren().add(tempRect);


    }

    private void fillTopAndBottom() {
        for (TaintVertex v : getV().getInputs()) {

            GUINode guiNode = v.setGraphics(this);
            guiNode.setTranslateLocation(0, 0, 4, 4);

            inputPane.getChildren().add(guiNode);
            v.getGraphics().rect.setFill(Color.OLIVE);
        }
        for (TaintVertex v : getV().getOutputs()) {
            GUINode guiNode = v.setGraphics(this);
            guiNode.setTranslateLocation(0, 0, 4, 4);

            outPane.getChildren().add(guiNode);
            v.getGraphics().rect.setFill(Color.RED);
        }
    }

    public void setTranslateLocation(double x, double y, double width, double height) {
        /*
        System.out.println("Translating MethodGuiNode (" + inputPane.getChildren().size() + "," + outPane.getChildren().size() + ") "
                + this.getTranslateX() + "," + this.getTranslateY()
                + " --> Local " + pane.getBoundsInLocal()
                + " --> Parent " + pane.getBoundsInParent()
                + " to " + x + "," + y + ":" + width + "," + height);
        */

        //super.setTranslateLocation(x,y,width,height);

        this.setTranslateX(x);
        this.setTranslateY(y);

        //pane.setTranslateX(x);
        //pane.setTranslateY(y);

        System.out.println("Translating MethodGuiNode "
                + this.getTranslateX() + "," + this.getTranslateY()
                + " --> Local " + pane.getBoundsInLocal()
                + " --> Parent " + pane.getBoundsInParent()
                + " to " + x + "," + y + ":" + width + "," + height);
    }

    private TaintMethodVertex getV() {
        return (TaintMethodVertex) vertex;
    }

    public double getHeight() {
        return pane.getHeight();
    }

    @Override
    public double getWidth() {
        return pane.getWidth();
    }
}
