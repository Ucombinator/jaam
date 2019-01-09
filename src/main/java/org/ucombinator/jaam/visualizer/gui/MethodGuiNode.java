package org.ucombinator.jaam.visualizer.gui;

import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import org.ucombinator.jaam.visualizer.taint.TaintMethodVertex;
import org.ucombinator.jaam.visualizer.taint.TaintVertex;

import java.util.ArrayList;
import java.util.Optional;

public class MethodGuiNode extends GUINode{

    public final double ELEMENT_WIDTH  = 5;
    public final double ELEMENT_HEIGHT = 5;

    VBox pane;
    HBox inputPane, outPane;

    Rectangle tempRect;



    public MethodGuiNode(GUINode parent, TaintMethodVertex v) {
        super(parent, v); // The rect will be used for collapsing

        // But we don't need it for now
        this.getChildren().clear();



        pane = new VBox();
        pane.getStyleClass().add("taint-method-vbox");
        setBackgroundFill(Color.DEEPSKYBLUE);
        //this.pane.setBackground(new Background(new BackgroundFill(Color.DEEPSKYBLUE, null, null)));

        inputPane = new HBox();
        inputPane.getStyleClass().add("taint-method-hbox");
        outPane = new HBox();
        outPane.getStyleClass().add("taint-method-hbox");


        /*
        System.out.println("START " + v
                + "\nT\t---> " + this.getBoundsInParent()
                + "\nI\t---> " + inputPane.getBoundsInParent()
                + "\nO\t---> " + outPane.getBoundsInParent()
                + "\nP\t---> " + pane.getBoundsInParent()
        );
        */


        fillTopAndBottom();

        pane.getChildren().addAll(inputPane, outPane);

        this.getChildren().add(pane);


        /*
        tempRect = new Rectangle();
        tempRect.setFill(Color.RED);
        this.rect.setFill(Color.BLACK);

        //this.getChildren().add(tempRect);
        inputPane.getChildren().add(tempRect);
        outPane.getChildren().add(tempRect);
        */
        /*
        System.out.println("END " + v
                + "\nT\t--> " + this.getBoundsInParent()
                + "\nI\t---> " + inputPane.getBoundsInParent()
                + "\nO\t---> " + outPane.getBoundsInParent()
                + "\nP\t---> " + pane.getBoundsInParent()
        );
        */
    }

    private void fillTopAndBottom() {

        for (TaintVertex v : getV().getInputs()) {

            GUINode guiNode = v.setGraphics(this);
            guiNode.setTranslateLocation(0, 0, ELEMENT_WIDTH, ELEMENT_HEIGHT);
            guiNode.rect.setFill(Color.DARKGREEN);
            guiNode.rect.setStroke(null);

            inputPane.getChildren().add(guiNode);
            //System.out.println("\t\tI\t " + this.getRectBoundsInParent() + " ?? " + (guiNode == v.getGraphics()));
        }


        for (TaintVertex v : getV().getOutputs()) {
            GUINode guiNode = v.setGraphics(this);
            guiNode.setTranslateLocation(0, 0, ELEMENT_WIDTH, ELEMENT_HEIGHT);
            guiNode.rect.setFill(Color.RED);
            guiNode.rect.setStroke(null);

            outPane.getChildren().add(guiNode);
            //System.out.println("\t\tO\t " +this.getRectBoundsInParent() + " ?? " + (guiNode == v.getGraphics()));
        }

        // Calculate the size

        int numElements = Math.max(getV().getInputs().size(), getV().getInputs().size());

        if (numElements == 0) numElements = 1; // We will use 1 as minimal size of a method node

        /*
        System.out.println("Width Elements:\n\t#\t" + numElements
                + "\n\th\n\t\tPad\t" + inputPane.getPadding() + "\n\t\tSpa\t" + inputPane.getSpacing()
                + "\n\tv\n\t\tPad\t" + pane.getPadding() + "\n\t\tSpa\t" + pane.getSpacing()
        );
        */

        double width = inputPane.getPadding().getLeft() // Left Padding
                + numElements * ELEMENT_WIDTH +  (numElements - 1) * inputPane.getSpacing() // Elements
                + inputPane.getPadding().getRight() // Right Padding
                + 1.0  // Border width
                ;
        double height = inputPane.getPadding().getTop() + inputPane.getPadding().getBottom()
                + 2 * ELEMENT_HEIGHT + pane.getSpacing()
                + outPane.getPadding().getTop() + outPane.getPadding().getBottom()
                + 1.0
                + 2.0 // Not sure where this comes from...
                ;

        width = Math.max(width, height); // Always at least a square

        //width = 0; height = 0;

        pane.setMinSize(width, height);
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
    }

    private TaintMethodVertex getV() {
        return (TaintMethodVertex) vertex;
    }

    public double getHeight() {
        return pane.getHeight();
    }

    public void setBackgroundFill(Color c) {
        this.pane.setBackground(new Background(new BackgroundFill(c, null, null)));
    }

    @Override
    public double getWidth() {
        return pane.getWidth();
    }
}
