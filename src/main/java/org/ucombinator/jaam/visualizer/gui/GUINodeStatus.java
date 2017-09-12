package org.ucombinator.jaam.visualizer.gui;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.GraphicsStatus;

public class GUINodeStatus implements GraphicsStatus {


    private double x;
    private double y;
    private double width;
    private double height;
    private double opacity;

    public GUINodeStatus(AbstractLayoutVertex v){
        GUINode g = v.getGraphics();

        this.x = g.getLayoutX() + g.getTranslateX();
        this.y = g.getLayoutY() + g.getTranslateY();

        this.width = g.getRect().getWidth();
        this.height = g.getRect().getHeight();

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

}
