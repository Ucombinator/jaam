package org.ucombinator.jaam.visualizer.gui;

import javafx.scene.control.Tab;
import org.ucombinator.jaam.visualizer.graph.Graph;

public class StacTab extends Tab {
    public final StacFrame controller;

    public StacTab(Graph graph) {
        // TODO: super(text, content);
        controller = new StacFrame(graph);
        this.setContent(controller.getRoot());
    }
}
