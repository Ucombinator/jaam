package org.ucombinator.jaam.visualizer.gui;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.ucombinator.jaam.visualizer.taint.TaintMethodVertex;
import org.ucombinator.jaam.visualizer.taint.TaintVertex;

import java.util.ArrayList;

public class MethodGuiNode extends GUINode{

    BorderPane pane;
    HBox inputPane, outPane;
    Label centerLabel;

    public MethodGuiNode(GUINode parent, TaintMethodVertex v) {
        super(parent, v); // The rect will be used for collapsing

        // But we don't need it for now
        this.getChildren().clear();

        pane = new BorderPane();
        inputPane = new HBox();
        outPane = new HBox();
        fillTopAndBottom();
        centerLabel = new Label(v.getMethodName());

        pane.setTop(inputPane);
        pane.setCenter(centerLabel);
        pane.setBottom(outPane);

        this.getChildren().add(pane);

        Rectangle r = new Rectangle(10.0, 10.0);
        r.setFill(Color.BLACK);
        pane.setLeft(r);
    }

    private void fillTopAndBottom() {
        for (TaintVertex v : getV().getInputs()) {
            inputPane.getChildren().add(v.setGraphics(this));
            v.getGraphics().setTranslateLocation(0,0, 10, 10);
            v.getGraphics().rect.setFill(Color.OLIVE);
        }
        for (TaintVertex v : getV().getOutputs()) {
            outPane.getChildren().add(v.setGraphics(this));
            v.getGraphics().rect.setFill(Color.RED);
        }
    }

    public void setTranslateLocation(double x, double y, double width, double height) {
        System.out.println("Translating MethodGuiNode (" + inputPane.getChildren().size() + "," + outPane.getChildren().size() + ") "
                + this.getTranslateX() + "," + this.getTranslateY()
                + " --> Local " + pane.getBoundsInLocal()
                + " --> Parent " + pane.getBoundsInParent()
                + " to " + x + "," + y + ":" + width + "," + height);


        //super.setTranslateLocation(x,y,width,height);

        pane.setTranslateX(x);
        pane.setTranslateY(y);

        System.out.println("Translating MethodGuiNode "
                + this.getTranslateX() + "," + this.getTranslateY()
                + " --> Local " + pane.getBoundsInLocal()
                + " --> Parent " + pane.getBoundsInParent()
                + " to " + x + "," + y + ":" + width + "," + height);
    }

    private TaintMethodVertex getV() {
        return (TaintMethodVertex) vertex;
    }
}
