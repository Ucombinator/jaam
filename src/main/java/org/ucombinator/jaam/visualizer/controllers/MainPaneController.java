package org.ucombinator.jaam.visualizer.controllers;

import com.strobel.decompiler.languages.java.ast.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import org.ucombinator.jaam.serializer.*;
import org.ucombinator.jaam.visualizer.codeView.CodeAreaGenerator;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.LayoutLoopVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutMethodVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MainPaneController {
    @FXML public final Parent root = null; // Initialized by Controllers.loadFXML()
    @FXML public final TabPane tabPane = null; // Initialized by Controllers.loadFXML()

    public MainPaneController() throws IOException {
        Controllers.loadFXML("/MainPane.fxml", this);
    }

    @FXML private void quit(ActionEvent event) { Platform.exit(); }

    @FXML private void zoomInAction(ActionEvent event) { Main.getSelectedVizPanelController().zoomSpinner.increment(); }
    @FXML private void zoomOutAction(ActionEvent event) { Main.getSelectedVizPanelController().zoomSpinner.decrement(); }

    @FXML private void searchByID(ActionEvent event) {
        Main.getSelectedMainTabController().initSearch(MainTabController.SearchType.ID);
    }

    @FXML private void searchByStatement(ActionEvent event) {
        Main.getSelectedMainTabController().initSearch(MainTabController.SearchType.INSTRUCTION);
    }

    @FXML private void searchByMethod(ActionEvent event) {
        Main.getSelectedMainTabController().initSearch(MainTabController.SearchType.METHOD);
    }

    private final FileChooser fileChooser = new FileChooser();
    @FXML public void loadLoopGraph(ActionEvent event) throws IOException {
        System.out.println("Load graph: start...");

        fileChooser.setTitle("Load graph file");
        List<File> files = fileChooser.showOpenMultipleDialog(this.tabPane.getScene().getWindow());

        if (files != null) {
            for (File file : files) {
                loadLoopGraphFile(file);
            }
        }
    }

    public void loadLoopGraphFile(File file) throws IOException {
        // Make "Open" dialog remember where we last loaded a file
        fileChooser.setInitialDirectory(file.getParentFile());

        CodeAreaGenerator codeAreaGenerator = new CodeAreaGenerator();
        Graph graph = parseLoopGraph(file, codeAreaGenerator);

        System.out.println("--> Create visualization: start...");
        MainTabController tabController = new MainTabController(file, graph, codeAreaGenerator);
        System.out.println("<-- Create visualization: Done!");

        tabPane.getTabs().add(tabController.tab);
        tabPane.getSelectionModel().select(tabController.tab);
    }

    private static Graph parseLoopGraph(File file, CodeAreaGenerator codeAreaGenerator) {
        Graph graph = new Graph();

        ArrayList<LoopEdge> edges = new ArrayList<>();
        for (Packet packet : Serializer.readAll(file.getAbsolutePath())) {
            if (packet instanceof LoopLoopNode) {
                LoopLoopNode node = (LoopLoopNode) packet;

                graph.addVertex(new LayoutLoopVertex(node.id().id(),
                        node.method().getSignature(),
                        node.statementIndex(), node));

            } else if (packet instanceof LoopMethodNode) {
                LoopMethodNode node = (LoopMethodNode) packet;
                graph.addVertex(new LayoutMethodVertex(node.id().id(),
                        node.method().getSignature(), node));
            } else if (packet instanceof LoopEdge) {
                edges.add((LoopEdge) packet);
            }
                else if (packet instanceof org.ucombinator.jaam.tools.decompile.DecompiledClass)
                {
                    CompilationUnit unit = ((org.ucombinator.jaam.tools.decompile.DecompiledClass) packet).compilationUnit();

                    codeAreaGenerator.addClass(unit);
                }
        }

        // We actually create the edges here
        for (LoopEdge edge : edges) {
            graph.addEdge(edge.src().id(), edge.dst().id());
        }

        return graph;
    }

    private static class CodeIdentifier
    {
        String signature;
        String className;
        String methodName;

        CodeIdentifier(String signature)
        {
            this.signature = signature;
            String[] fields = signature.split(" ");

            className  = fields[0].substring(1, fields[0].indexOf(":"));
            methodName = fields[2].substring(0, fields[2].indexOf("("));
        }

        public String toString(){ return className + ":" + methodName;}
    }

}
