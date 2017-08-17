package org.ucombinator.jaam.visualizer.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import org.ucombinator.jaam.serializer.*;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.LayoutLoopVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutMethodVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainPaneController {
    @FXML private AnchorPane root;
    public AnchorPane getRoot() { return this.root; }

    @FXML private TabPane tabPane;
    public TabPane getTabPane() { return this.tabPane; }

    public MainPaneController() throws IOException {
        Controllers.loadFXML("/MainPane.fxml", this);
    }

    public void loadLoopGraph(ActionEvent event) throws IOException {
        System.out.println("Load graph: start...");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load graph file");
        File file = fileChooser.showOpenDialog(getTabPane().getScene().getWindow());

        if (file == null) {
            System.out.println("Error! Invalid file.");
            return;
        }

        loadLoopGraphFile(file);
    }

    public void loadLoopGraphFile(File file) throws IOException {
        Graph graph = parseLoopGraph(file);

        System.out.println("--> Create visualization: start...");
        MainTabController tabController = new MainTabController(file, graph);
        System.out.println("<-- Create visualization: Done!");

        tabPane.getTabs().add(tabController.tab);
        tabPane.getSelectionModel().select(tabController.tab);
    }

    private static Graph parseLoopGraph(File file) {
        Graph graph = new Graph();

        ArrayList<LoopEdge> edges = new ArrayList<>();
        for (Packet packet : Serializer.readAll(file.getAbsolutePath())) {
            if (packet instanceof LoopLoopNode) {
                LoopLoopNode node = (LoopLoopNode) packet;
                graph.addVertex(new LayoutLoopVertex(node.id().id(),
                        node.method().getSignature(),
                        node.statementIndex()));
            } else if (packet instanceof LoopMethodNode) {
                LoopMethodNode node = (LoopMethodNode) packet;
                graph.addVertex(new LayoutMethodVertex(node.id().id(),
                        node.method().getSignature()));
            } else if (packet instanceof LoopEdge) {
                edges.add((LoopEdge) packet);
            }
//                else if (packet instanceof org.ucombinator.jaam.tools.decompile.DecompiledClass)
//                {
//                    CompilationUnit unit = ((org.ucombinator.jaam.tools.decompile.DecompiledClass) packet).compilationUnit();
//                    String className = getClassName(unit);
//                    graph.addClass(className, unit.getText());
//                }

//                System.out.println("Reading new packet...");
//                packet = packetInput.read();
//                System.out.println("Packet read!");
        }

        // We actually create the edges here
        for (LoopEdge edge : edges) {
            graph.addEdge(edge.src().id(), edge.dst().id());
        }

        return graph;
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
