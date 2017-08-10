package org.ucombinator.jaam.visualizer.gui;

import javafx.scene.control.Tab;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.graph.Graph;

public class MainTab extends Tab {
    public final MainTabController controller;

    public MainTab(Graph graph) {
        // TODO: super(text, content);
        controller = new MainTabController(graph);
        this.setContent(controller.getRoot());
    }
}
