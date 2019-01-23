package org.ucombinator.jaam.visualizer.controllers;

import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.ucombinator.jaam.visualizer.classTree.ClassTreeNode;
import org.ucombinator.jaam.visualizer.classTree.ClassTreeUtils;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.state.*;
import org.ucombinator.jaam.visualizer.taint.*;
import soot.SootClass;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MainTabController {
    public final Tab tab;
    public final VizPanelController vizPanelController;
    public final TaintPanelController taintPanelController;
    public final CodeViewController codeViewController;
    public final SearchResultsController searchResultsController;

    // Left Side Components
    @FXML private final VBox leftPane = null; // Initialized by Controllers.loadFXML()

    // Center Components
    @FXML private final Node root = null; // Initialized by Controllers.loadFXML()
    @FXML private final BorderPane vizPane = null; // Initialized by Controllers.loadFXML()
    @FXML private final BorderPane taintPane = null; // Initialized by Controllers.loadFXML()

    // Right Side Components
    @FXML private final TextArea vizDescriptionArea = null; // Initialized by Controllers.loadFXML()
    @FXML private final TextArea taintDescriptionArea = null; // Initialized by Controllers.loadFXML()
    @FXML private TreeView<ClassTreeNode> classTree = null; // Initialized by Controllers.loadFXML()
    @FXML private final BorderPane searchPane = null; // Initialized by Controllers.loadFXML()

    private HashSet<StateVertex> stateHighlighted; // Visible nodes
    private HashSet<TaintVertex> taintHighlighted; // Visible nodes
    private SetHistoryProperty<StateVertex> immutableStateHidden; // Immutable nodes

    public MainTabController(File file, Graph<StateVertex, StateEdge> graph, List<CompilationUnit> compilationUnits,
                             Graph<TaintVertex, TaintEdge> taintGraph, Set<SootClass> sootClasses) throws IOException {
        Controllers.loadFXML("/MainTabContent.fxml", this);

        // Initialize sets
        this.stateHighlighted = new LinkedHashSet<>();
        this.taintHighlighted = new LinkedHashSet<>();
        this.immutableStateHidden = new SetHistoryProperty<>(new SimpleSetProperty<>(FXCollections.observableSet()));

        // Initialize controllers
        this.codeViewController = new CodeViewController(compilationUnits, sootClasses);
        this.leftPane.getChildren().add(this.codeViewController.codeTabs);

        this.vizPanelController = new VizPanelController(graph, this);
        this.vizPane.setCenter(this.vizPanelController.root);

        this.taintPanelController = new TaintPanelController(taintGraph, this.codeViewController, this);
        this.taintPane.setCenter(this.taintPanelController.root);

        this.searchResultsController = new SearchResultsController();
        this.searchPane.setCenter(this.searchResultsController.root);

        this.tab = new Tab(file.getName(), this.root);
        this.tab.tooltipProperty().set(new Tooltip(file.getAbsolutePath()));
        Controllers.put(this.tab, this);

        this.codeViewController.addSelectHandler(vizPane);
        this.taintPanelController.addSelectHandler(vizPane);

        ClassTreeUtils.buildClassTree(this.classTree, this.codeViewController, this.vizPanelController.getImmutableRoot());

        // Add listeners
        this.immutableStateHidden.addListener(this.vizPanelController);
        //this.immutableTaintShown.addListener(this.taintPanelController);
    }

    public TreeView<ClassTreeNode> getClassTree() {
        return this.classTree;
    }

    public void setVizRightText(StateVertex v) {
        this.vizDescriptionArea.setText(v.getLongText());
    }
    public void setTaintRightText(TaintVertex v) {
        this.taintDescriptionArea.setText(v.getLongText());
    }

    public SetHistoryProperty<StateVertex> getImmutableStateHidden() {
        return this.immutableStateHidden;
    }

    public void hideSelectedStateNodes() {
        if (this.stateHighlighted.isEmpty()) { return; }

        this.immutableStateHidden.addAll(this.vizPanelController.getImmutable(this.stateHighlighted));
        this.stateHighlighted.clear();
        this.vizPanelController.redrawGraph();
    }

    public void hideSelectedTaintNodes() {
        if (this.taintHighlighted.isEmpty()) { return; }

        taintPanelController.hideVisibleVertices(this.taintHighlighted);
        this.taintHighlighted.clear();
    }

    public void showAllStateNodes() {
        this.immutableStateHidden.clear();
        this.vizPanelController.redrawGraph();
    }

    public HashSet<StateVertex> getStateHighlighted() {
        return this.stateHighlighted;
    }

    public HashSet<TaintVertex> getTaintHighlighted() {
        return this.taintHighlighted;
    }

    public void addToHighlighted(StateVertex v) {
        if(v != null) {
            stateHighlighted.add(v);
            v.setHighlighted(true);
        }
    }

    public void resetVizHighlighted() {
        for(StateVertex currHighlighted : stateHighlighted) {
            currHighlighted.setHighlighted(false);
        }
        stateHighlighted.clear();
    }

    public void resetHighlighted(StateVertex newHighlighted) {
        resetVizHighlighted();
        addToHighlighted(newHighlighted);
    }

    public void addToHighlighted(TaintVertex v) {
        if(v != null) {
            taintHighlighted.add(v);
            v.setHighlighted(true);
        }
    }

    public void resetTaintHighlighted() {
        for(TaintVertex currHighlighted : taintHighlighted) {
            currHighlighted.setHighlighted(false);
        }
        taintHighlighted.clear();
    }

    public void resetHighlighted(TaintVertex newHighlighted) {
        resetTaintHighlighted();
        addToHighlighted(newHighlighted);
    }

    public void hideUnrelatedToHighlightedState() {
        if (this.stateHighlighted.isEmpty()) {
            return;
        }

        HashSet<StateVertex> toHideVis = vizPanelController.getUnrelatedVisible(this.stateHighlighted);
        this.immutableStateHidden.addAll(this.vizPanelController.getImmutable(toHideVis));
        this.stateHighlighted.clear();
        this.vizPanelController.redrawGraph();
    }

    public void hideUnrelatedToHighlightedTaint() {
        if (this.taintHighlighted.isEmpty()) {
            return;
        }

        HashSet<TaintVertex> toHideVis = taintPanelController.getUnrelatedVisible(this.taintHighlighted);
        this.taintHighlighted.clear();
        this.taintPanelController.hideVisibleVertices(toHideVis);
    }

    // TODO: This should be done using event and event handling using FieldSelectEvent
    public void selectFieldInTaintGraph(String fullClassName, String fieldName) {
        taintPanelController.showFieldTaintGraph(fullClassName, fieldName);
    }

    public void setClassHighlight(HashSet<StateVertex> vertices, boolean value) {
        vizPanelController.setClassHighlight(vertices, value);
    }
}
