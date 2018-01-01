package org.ucombinator.jaam.visualizer.controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import org.ucombinator.jaam.visualizer.taint.TaintGraph;

import java.io.File;
import java.io.IOException;

// TODO: Make base TabController class or interface?
public class TaintTabController {

    public final Tab tab;
    public final TaintPanelController taintPanelController;

    @FXML private final Node root = null; // Initialized by Controllers.loadFXML()
    @FXML private final BorderPane centerPane = null; // Initialized by Controllers.loadFXML()

    public TaintTabController(File file, TaintGraph graph) throws IOException {
        Controllers.loadFXML("/TaintTabContent.fxml", this);

        this.tab = new Tab(file.getName(), this.root);
        this.tab.tooltipProperty().set(new Tooltip(file.getAbsolutePath()));
        Controllers.put(this.tab, this);

        this.taintPanelController = new TaintPanelController(graph);
        this.centerPane.setCenter(this.taintPanelController.root);
    }
}
