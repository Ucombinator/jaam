package org.ucombinator.jaam.visualizer.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.main.TakeInput;

import java.io.File;
import java.io.IOException;

public class MainPaneController {
    @FXML private AnchorPane root;
    public AnchorPane getRoot() { return this.root; }

    @FXML private TabPane tabPane;
    public TabPane getTabPane() { return this.tabPane; }

    public MainPaneController() throws IOException {
        Controllers.loadFXML("/MainPane.fxml", this);
    }

    public void loadLoopGraph(ActionEvent event) throws IOException {
        Graph graph;
        TakeInput ti = new TakeInput();
        String filename = "";

        System.out.println("Load graph: start...");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load graph file");
        File file = fileChooser.showOpenDialog(getTabPane().getScene().getWindow());

        if (file == null) {
            System.out.println("Error! Invalid file.");
            return;
        }

        String path = file.getAbsolutePath();
        System.out.println("File path: " + path);
        graph = ti.parseLoopGraph(file.getAbsolutePath());
        filename = file.getName();

        System.out.println("--> Create visualization: start...");
        MainTabController tabController = new MainTabController(file.getName(), graph);
        System.out.println("<-- Create visualization: Done!");

        if (filename.equals("")) {
            tabController.tab.setText("Sample");
        } else {
            tabController.tab.setText(filename);
        }
        tabPane.getTabs().add(tabController.tab);
        tabPane.getSelectionModel().select(tabController.tab);
    }

    @FXML private void quit(ActionEvent event) {
        Platform.exit();
    }

    @FXML private void searchByID(ActionEvent event) {
        Main.getSelectedMainTabController().initSearch(MainTabController.SearchType.ID);
    }

    @FXML private void searchByStatement(ActionEvent event) {
        Main.getSelectedMainTabController().initSearch(MainTabController.SearchType.INSTRUCTION);
    }

    @FXML private void searchByMethod(ActionEvent event) {
        Main.getSelectedMainTabController().initSearch(MainTabController.SearchType.METHOD);
    }
}