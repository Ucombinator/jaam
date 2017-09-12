package org.ucombinator.jaam.visualizer.gui;

import org.ucombinator.jaam.visualizer.layout.GraphicsStatus;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;

public class EdgeStatus implements GraphicsStatus {

    private double opacity=1;
    private LayoutEdge edge;

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        this.opacity = opacity;
    }

    public EdgeStatus(LayoutEdge e){
        this.edge = e;
    }

    public LayoutEdge getEdge() {
        return edge;
    }
}
