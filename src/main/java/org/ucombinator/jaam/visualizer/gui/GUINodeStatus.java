package org.ucombinator.jaam.visualizer.gui;

import com.sun.javafx.geom.Edge;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.ucombinator.jaam.visualizer.graph.AbstractVertex;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.GraphEntity;
import org.ucombinator.jaam.visualizer.layout.GraphicsStatus;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by giordano on 8/28/17.
 */
public class GUINodeStatus implements GraphicsStatus {


    private double x;
    private double y;
    private double width;
    private double height;
    private double totalScaleX;
    private double totalScaleY;
    private double opacity;

    public GUINodeStatus(AbstractLayoutVertex v){
        GUINode g = v.getGraphics();

        this.x = g.getLayoutX() + g.getTranslateX();
        this.y = g.getLayoutY() + g.getTranslateY();

        this.width = g.getWidth();
        this.height = g.getHeight();

        this.totalScaleX = this.width/g.getRect().getWidth();
        this.totalScaleY = this.height/g.getRect().getHeight();

        this.opacity = g.getOpacity();
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        this.opacity = opacity;
    }

    public double getX() {

        return this.x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }


    public double getTotalScaleX() {
        return totalScaleX;
    }

    public void setTotalScaleX(double totalScaleX) {
        this.totalScaleX = totalScaleX;
    }

    public double getTotalScaleY() {
        return totalScaleY;
    }

    public void setTotalScaleY(double totalScaleY) {
        this.totalScaleY = totalScaleY;
    }



}
