package org.ucombinator.jaam.visualizer.gui;

import javafx.scene.control.Tab;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.graph.Graph;

import java.io.IOException;

public class MainTab extends Tab {
    public final MainTabController controller;

    public MainTab(Graph graph) throws IOException {
        // TODO: super(text, content);
        controller = new MainTabController(graph);
        this.setContent(controller.getRoot());
    }
}
