package org.ucombinator.jaam.visualizer.controllers;

import com.strobel.decompiler.languages.java.ast.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.tuple.Pair;
import org.ucombinator.jaam.serializer.*;
import org.ucombinator.jaam.tools.taint3.Address;
import org.ucombinator.jaam.tools.taint3.Edge;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.LayoutLoopVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutMethodVertex;
import org.ucombinator.jaam.visualizer.layout.StateVertex;
import org.ucombinator.jaam.visualizer.main.Main;
import org.ucombinator.jaam.visualizer.taint.TaintAddress;
import org.ucombinator.jaam.visualizer.taint.TaintGraph;
import soot.SootClass;

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

    @FXML private void hideSelectedNodes(ActionEvent event) {
        Main.getSelectedMainTabController().hideSelectedNodes();
    }

    @FXML private void hideUnrelatedNodes(ActionEvent event) {
        Main.getSelectedMainTabController().hideUnrelatedToHighlighted();
    }

    @FXML private void pruneVisibleGraph(ActionEvent event) {
        Main.getSelectedMainTabController().pruneVisibleGraph();
    }

    @FXML private void showAllNodes(ActionEvent event) {
        Main.getSelectedMainTabController().showAllNodes();
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

        List<CompilationUnit> compilationUnits = new ArrayList<>();
        Set<SootClass> sootClasses = new HashSet<>();
        Pair<Graph<StateVertex>,TaintGraph> s = parseLoopGraph(file, compilationUnits, sootClasses);

        System.out.println("--> Create visualization: start...");
        MainTabController tabController = new MainTabController(file, s.getLeft(),  compilationUnits, s.getRight(), sootClasses);
        System.out.println("<-- Create visualization: Done!");

        tabPane.getTabs().add(tabController.tab);
        tabPane.getSelectionModel().select(tabController.tab);
    }

    private static Pair<Graph<StateVertex>,TaintGraph> parseLoopGraph(File file, List<CompilationUnit> compilationUnits, Set<SootClass> sootClasses) {
        Graph<StateVertex> graph = new Graph<>();
        int loopPackets = 0;
        int methodPackets = 0;
        int loopEdgePackets = 0;
        ArrayList<LoopEdge> loopEdges = new ArrayList<>();

        TaintGraph taintGraph = new TaintGraph();
        HashMap<Address, TaintAddress> addressIndex = new HashMap<>();
        int addressPackets = 0;
        int taintEdgePackets = 0;
        ArrayList<Edge> taintEdges = new ArrayList<>();

        for (Packet packet : Serializer.readAll(file.getAbsolutePath())) {
            if (packet instanceof LoopLoopNode) {
                loopPackets++;
                LoopLoopNode node = (LoopLoopNode) packet;

                graph.addVertex(new LayoutLoopVertex(node.id().id(),
                        node.method().getSignature(),
                        node.statementIndex(), node));

                sootClasses.add(node.method().getDeclaringClass());

            } else if (packet instanceof LoopMethodNode) {
                methodPackets++;
                LoopMethodNode node = (LoopMethodNode) packet;
                graph.addVertex(new LayoutMethodVertex(node.id().id(),
                        node.method().getSignature(), node));

                sootClasses.add(node.method().getDeclaringClass());

                System.out.println("Reading method packet: " + node.method().getSignature());
            } else if (packet instanceof LoopEdge) {
                loopEdgePackets++;
                loopEdges.add((LoopEdge) packet);
            } else if (packet instanceof org.ucombinator.jaam.tools.decompile.DecompiledClass) {
                CompilationUnit unit = ((org.ucombinator.jaam.tools.decompile.DecompiledClass) packet).compilationUnit();
                compilationUnits.add(unit);
            } else if(packet instanceof Address) {
                Address address = (Address) packet;
                TaintAddress vertex = new TaintAddress(address);
                taintGraph.addVertex(vertex);
                addressIndex.put(address, vertex);
                addressPackets++;

                if (address.sootMethod() != null) {
                    sootClasses.add(address.sootMethod().getDeclaringClass());
                }

                //System.out.println("Read address: " + address);
            } else if(packet instanceof Edge) {
                Edge edge = (Edge) packet;
                taintEdges.add(edge);
                taintEdgePackets++;
                //System.out.println("Read edge: " + edge);
            }
        }

        // We actually create the edges here
        for (LoopEdge edge : loopEdges) {
            graph.addEdge(edge.src().id(), edge.dst().id());
        }

        // We actually create the edges here
        int ignoredEdges = 0;
        for (Edge edge : taintEdges) {
            if(addressIndex.containsKey(edge.source()) && addressIndex.containsKey(edge.target())) {
                taintGraph.addEdge(addressIndex.get(edge.source()), addressIndex.get(edge.target()));
            }
            else {
                System.out.println("Ignoring edge: " + edge.source() + " --> " + edge.target());
                ignoredEdges++;
            }
        }

        System.out.println("Loop packets: " + loopPackets);
        System.out.println("Method packets: " + methodPackets);
        System.out.println("Loop edge packets: " + loopEdgePackets);
        System.out.println("Address packets: " + addressPackets);
        System.out.println("Taint edge packets: " + taintEdgePackets);
        System.out.println("Ignored edges: " + ignoredEdges);

        TaintGraph stmtTaintGraph = taintGraph.groupByStatement();
        return Pair.of(graph, stmtTaintGraph);
    }
}
