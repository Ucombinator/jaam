package org.ucombinator.jaam.visualizer.controllers;

import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.ucombinator.jaam.visualizer.classTree.ClassTreeNode;
import org.ucombinator.jaam.visualizer.classTree.PackageNode;
import org.ucombinator.jaam.visualizer.graph.Edge;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.*;
import org.ucombinator.jaam.visualizer.layout.*;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
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

    // Left Side Components
    @FXML private final VBox leftPane = null; // Initialized by Controllers.loadFXML()

    // Center Components
    @FXML private final Node root = null; // Initialized by Controllers.loadFXML()
    @FXML private final BorderPane vizPane = null; // Initialized by Controllers.loadFXML()
    @FXML private final BorderPane taintPane = null; // Initialized by Controllers.loadFXML()

    //Right Side Components
    @FXML private final TextArea vizDescriptionArea = null; // Initialized by Controllers.loadFXML()
    @FXML private final TextArea taintDescriptionArea = null; // Initialized by Controllers.loadFXML()
    @FXML private final TreeView<ClassTreeNode> classTree = null; // Initialized by Controllers.loadFXML()
    @FXML private final SearchResults searchResults = null; // Initialized by Controllers.loadFXML()

    private HashSet<StateVertex> vizHighlighted; // TODO: Make this an observable set
    private HashSet<TaintVertex> taintHighlighted;
    private SetProperty<StateVertex> hidden;

    public enum SearchType {
        ID, TAG, INSTRUCTION, METHOD, ALL_LEAVES, ALL_SOURCES, OUT_OPEN, OUT_CLOSED, IN_OPEN, IN_CLOSED, ROOT_PATH
    }

    public MainTabController(File file, Graph<StateVertex> graph, List<CompilationUnit> compilationUnits, TaintGraph taintGraph, Set<SootClass> sootClasses) throws IOException {
        Controllers.loadFXML("/MainTabContent.fxml", this);

        this.tab = new Tab(file.getName(), this.root);
        this.tab.tooltipProperty().set(new Tooltip(file.getAbsolutePath()));
        Controllers.put(this.tab, this);

        this.vizPanelController = new VizPanelController(graph);
        this.vizPane.setCenter(this.vizPanelController.root);

        this.taintPanelController = new TaintPanelController(taintGraph);
        this.taintPane.setCenter(this.taintPanelController.root);

        this.codeViewController = new CodeViewController(compilationUnits, sootClasses);
        this.leftPane.getChildren().add(this.codeViewController.codeTabs);

        this.codeViewController.addSelectHandler(vizPane);
        this.taintPanelController.addSelectHandler(vizPane);

        buildClassTree(this.codeViewController.getClassNames(), this.vizPanelController.getPanelRoot());

        this.vizHighlighted = new LinkedHashSet<>();
        this.taintHighlighted = new LinkedHashSet<>();
        this.hidden = new SimpleSetProperty<>(FXCollections.observableSet());

        this.hidden.addListener(this.vizPanelController);
    }

    private void buildClassTree(HashSet<String> classNames, StateVertex panelRoot)
    {
        this.classTree.setCellFactory(CheckBoxTreeCell.forTreeView());

        PackageNode root = new PackageNode("root", null);
        ArrayList<PackageNode> topLevel = new ArrayList<>();

        for(String c : classNames)
        {
            String[] split = c.split("\\.");

            PackageNode current = root;
            for (int i = 0; i < split.length-1; ++i) {
                current = current.addPackageIfAbsent(split[i]);
            }

            String className = split[split.length-1];
            current.addClassIfAbsent(className);
        }

        topLevel.addAll(root.subPackages);

        // Compression Step
        topLevel.stream().forEach(f -> f.compress());

        // Fix top level names. If a node is on the top level and a leaf due to compression
        // it's fullname is missing package information, this fixes it.
        /*
        for (ClassTreeNode f : topLevel) {
            if (f.isLeaf()) {
                f.name = f.shortName;// shortName is correct due to compressions step
            }
        }
        */

        // Add the vertices
        addVerticesToClassTree(topLevel, panelRoot);

        // Build the Tree
        CheckBoxTreeItem<ClassTreeNode> treeRoot = new CheckBoxTreeItem<>();
        treeRoot.setSelected(true);
        treeRoot.setValue(new ClassTreeNode("root", null));
        treeRoot.setExpanded(true);

        topLevel.stream().forEach(f -> f.build(treeRoot));

        classTree.setRoot(treeRoot);

        classTree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<ClassTreeNode>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<ClassTreeNode>> observableValue,
                                TreeItem<ClassTreeNode> oldValue, TreeItem<ClassTreeNode> newValue) {

                if (oldValue != null) {
                    setClassHighlight(oldValue.getValue().getChildVertices(), false);
                }

                setClassHighlight(newValue.getValue().getChildVertices(), true);
            }
        });

        classTree.setOnMouseClicked(m -> {
            if (m.getClickCount() == 2) {
                final TreeItem<ClassTreeNode> item = classTree.getSelectionModel().getSelectedItem();

                item.getValue().handleDoubleClick(codeViewController);
            }
        });

    }

    private void addVerticesToClassTree(ArrayList<PackageNode> topLevel, StateVertex root) {

        if(root instanceof CodeEntity) {
            PackageNode topLevelNode = getTopLevel(topLevel, ((CodeEntity) root).getClassName());
            boolean success = false;
            if (topLevelNode != null) {
                success = topLevelNode.addVertex(root);
            }

            if (!success) {
                System.out.println("Warning didn't find package for: " + ((CodeEntity) root).getClassName());
            }
        }

       ImmutableHierarchicalGraph<StateVertex> innerGraph = root.getImmutableInnerGraph(); // TODO: Is this the right one?
       for (StateVertex v : innerGraph.getVertices()) {
           addVerticesToClassTree(topLevel, v);
       }
    }

    private PackageNode getTopLevel(ArrayList<PackageNode> topLevel, String className) {
        for (PackageNode n : topLevel) {
            if (className.startsWith(n.shortName))
                return n;
        }

        return null;
    }

    public void repaintAll() {
        System.out.println("Repainting all...");
        //bytecodeArea.setDescription();
        setVizRightText();
        searchResults.writeText(this);
    }

    public void setVizRightText() {
        StringBuilder text = new StringBuilder();
        for (StateVertex v : this.getVizHighlighted()) {
            text.append(v.getRightPanelContent() + "\n");
        }

        this.vizDescriptionArea.setText(text.toString());

        for(TaintVertex v : this.getTaintHighlighted()) {

        }
    }

    public void setRightText(LayoutLoopVertex v)
    {
        this.vizDescriptionArea.setText("Loop:\n  Class: "
                + v.getClassDeclaration() + "\n  Method: "
                + v.getMethodName()       + "\n  Index: "
                + v.getStatementIndex()   + "\n  Signature: " + v.getLabel());
    }


    public void setRightText(LayoutMethodVertex v)
    {
        this.vizDescriptionArea.setText("Method:\n  Class: "
                + v.getClassDeclaration() + "\n  Method: "
                + v.getMethodName()       + "\n  Signature: " + v.getLabel());
    }

    public void setRightText(LayoutSccVertex v)
    {
        StringBuilder text = new StringBuilder("SCC contains:\n");
        int k = 0;
        VisibleHierarchicalGraph<StateVertex> innerGraph = v.getVisibleInnerGraph();
        for (StateVertex i : innerGraph.getVertices()) {
            text.append(k++ + "  " + i.getLabel() + "\n");
        }
        this.vizDescriptionArea.setText(text.toString());
    }

    public void setRightText(TaintAddress v) {
        this.taintDescriptionArea.setText("Taint address:\n" + v.toString());
    }

    public void setRightText(TaintSccVertex v) {
        StringBuilder text = new StringBuilder("SCC contains:\n");
        int k = 0;
        VisibleHierarchicalGraph<TaintVertex> innerGraph = v.getVisibleInnerGraph();
        for(AbstractLayoutVertex i : innerGraph.getVertices()) {
            text.append(k++ + "  " + i.getLabel() + "\n");
        }
        this.taintDescriptionArea.setText(text.toString());
    }

    public void setRightText(TaintStmtVertex v) {
        StringBuilder text = new StringBuilder("Statement: " + v.getStmt());
        text.append("\nAddresses: " + v.getAddresses().size());
        this.taintDescriptionArea.setText(text.toString());
    }

    public void setVizRightText(String text)
    {
        this.vizDescriptionArea.setText(text);
    }

    public void setTaintRightText(String text) {
        this.taintDescriptionArea.setText(text);
    }

    public void initSearch(SearchType search) {
        // Clean up info from previous searches
        this.resetVizHighlighted();
        String query = getSearchInput(search);

        if (search == SearchType.ID) {
            searchByID(query); // TODO: Fix inconsistency with panel root
        } else if (search == SearchType.METHOD) {
            this.vizPanelController.getPanelRoot().searchByMethod(query.toLowerCase(), this);
        }

        this.repaintAll();
    }

    public ObservableSet<StateVertex> getHidden() {
        return hidden.get();
    }

    public void hideSelectedNodes() {
        this.hidden.addAll(this.getVizHighlighted());
        this.vizHighlighted.clear();
    }

    public void pruneVisibleGraph() {
        HashSet<StateVertex> prunedVertices = this.vizPanelController.pruneVisibleGraph();
        this.hidden.addAll(prunedVertices);
        this.vizHighlighted.removeAll(prunedVertices);
    }

    public void showAllNodes() {
        System.out.println("Showing all hidden nodes...");
        vizPanelController.startBatchMode();
        this.hidden.clear();
        vizPanelController.endBatchMode();
    }

    public String getSearchInput(SearchType search) {
        String title = "";
        System.out.println("Search type: " + search);
        if (search == SearchType.ID || search == SearchType.ROOT_PATH) {
            title = "Enter node ID(s)";
        } else if (search == SearchType.INSTRUCTION) {
            title = "Instruction contains ...";
        } else if (search == SearchType.METHOD) {
            title = "Method name contains ...";
        } else if (search == SearchType.OUT_OPEN || search == SearchType.OUT_CLOSED || search == SearchType.IN_OPEN
                || search == SearchType.IN_CLOSED) {
            title = "Enter node ID";
        }

        String input = "";
        if (search != SearchType.ALL_LEAVES && search != SearchType.ALL_SOURCES && search != SearchType.TAG) {
            System.out.println("Showing dialog...");
            TextInputDialog dialog = new TextInputDialog();
            dialog.setHeaderText(title);
            dialog.showAndWait();
            input = dialog.getResult();
            if (input == null) {
                return "";
            } else {
                input = input.trim();
            }

            if (input.equals("")) {
                return "";
            }
        }

        return input;
    }

    public void searchByID(String input) {
        for (String token : input.split(", ")) {
            if (token.trim().equalsIgnoreCase("")) {
                /* Do nothing */
            } else if (token.indexOf('-') == -1) {
                int id1 = Integer.parseInt(token.trim());
                this.vizPanelController.getPanelRoot().searchByID(id1, this);
            } else {
                int id1 = Integer.parseInt(token.substring(0, token.indexOf('-')).trim());
                int id2 = Integer.parseInt(token.substring(token.lastIndexOf('-') + 1).trim());
                this.vizPanelController.getPanelRoot().searchByIDRange(id1, id2, this);
            }
        }
    }

    public HashSet<StateVertex> getVizHighlighted() {
        return this.vizHighlighted;
    }

    public HashSet<TaintVertex> getTaintHighlighted() {
        return this.taintHighlighted;
    }

    public void addToHighlighted(StateVertex v)
    {
        if(v != null) {
            vizHighlighted.add(v);
            v.setHighlighted(true);
        }
    }

    public void resetVizHighlighted() {
        for(StateVertex currHighlighted : vizHighlighted) {
            currHighlighted.setHighlighted(false);
        }
        vizHighlighted.clear();
    }

    public void resetHighlighted(StateVertex newHighlighted)
    {
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

    public void hideUnrelatedToHighlighted()
    {
        HashSet<StateVertex> keep = new HashSet<>();

        this.vizHighlighted.stream().forEach(v -> keep.addAll(v.getAncestors()) );
        this.vizHighlighted.stream().forEach(v -> keep.addAll(v.getDescendants()) );

        HashSet<StateVertex> toHide = new HashSet<>();
        this.vizPanelController.getPanelRoot().getVisibleInnerGraph().getVertices().stream().forEach(v -> {
            if (!keep.contains(v)) {
                toHide.add(v);
            }
        });

        this.vizPanelController.startBatchMode();
        this.hidden.addAll(toHide);
        this.vizPanelController.endBatchMode();
        this.vizHighlighted.clear();
        this.vizPanelController.resetAndRedraw();
    }

    // TODO: This should be done using event and event handling using FieldSelectEvent
    public void selectFieldInTaintGraph(String fullClassName, String fieldName) {
        taintPanelController.showFieldTaintGraph(fullClassName, fieldName);
    }

    private void setClassHighlight(HashSet<StateVertex> vertices, boolean value) {

        for (StateVertex v : vertices) {
            if (!v.isHidden()) {
                v.setClassHighlight(value);
            }
        }
    }

    private void setClassHighlight(StateVertex v, String prevPrefix, String currPrefix)
    {
        if(!v.isHidden()) {
            if(v instanceof CodeEntity) {
                CodeEntity cv = (CodeEntity) v;
                if (cv.getClassName().startsWith(currPrefix)) {
                    //System.out.println("Highlight " + cv.getClassName() + " --> " + cv.getMethodName() + " --> " + v.getId());
                    v.setClassHighlight(true);
                } else if (prevPrefix != null && cv.getClassName().startsWith(prevPrefix)) {
                    v.setClassHighlight(false);
                }
            }

            if (!v.isVisibleInnerGraphEmpty()) {
                HierarchicalGraph<StateVertex> innerGraph = v.getVisibleInnerGraph();
                for (StateVertex i : innerGraph.getVertices()) {
                    setClassHighlight(i, prevPrefix, currPrefix);
                }
            }
        }
    }
}
