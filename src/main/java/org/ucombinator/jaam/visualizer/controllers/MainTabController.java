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
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.*;
import org.ucombinator.jaam.visualizer.layout.*;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import org.ucombinator.jaam.visualizer.main.Main;
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

        this.vizPanelController = new VizPanelController(graph);
        this.vizPane.setCenter(this.vizPanelController.root);
        this.vizPanelController.initFX();

        this.taintPanelController = new TaintPanelController(taintGraph);
        this.taintPane.setCenter(this.taintPanelController.root);

        this.tab = new Tab(file.getName(), this.root);
        this.tab.tooltipProperty().set(new Tooltip(file.getAbsolutePath()));
        Controllers.put(this.tab, this);

        this.codeViewController = new CodeViewController(compilationUnits, sootClasses);
        this.leftPane.getChildren().add(this.codeViewController.codeTabs);

        this.codeViewController.addSelectHandler(vizPane);
        this.taintPanelController.addSelectHandler(vizPane);

        buildClassTree(this.codeViewController.getClassNames(), this.vizPanelController.getPanelRoot());

        this.vizHighlighted = new LinkedHashSet<>();
        this.taintHighlighted = new LinkedHashSet<>();
        this.hidden = new SimpleSetProperty<StateVertex>(FXCollections.observableSet());

        this.hidden.addListener(this.vizPanelController);
    }

    private void buildClassTree(HashSet<String> classNames, LayoutRootVertex panelRoot)
    {
        this.classTree.setCellFactory(CheckBoxTreeCell.forTreeView());

        ClassTreeNode root = new ClassTreeNode("root", null);
        ArrayList<ClassTreeNode> topLevel = new ArrayList<>();

        for(String c : classNames)
        {
            String[] split = c.split("\\.");

            ClassTreeNode current = root;
            for(String s : split)
            {
                current = current.addIfAbsent(s);
            }
        }

        topLevel.addAll(root.subDirs);

        // Compression Step
        topLevel.stream().forEach(f -> f.compress());

        // Fix top level names. If a node is on the top level and a leaf due to compression
        // it's fullname is missing package information, this fixes it.
        for (ClassTreeNode f : topLevel) {
            if (f.isLeaf()) {
                f.fullName = f.name;// name is correct due to compressions step
            }
        }

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

                setClassHighlight(vizPanelController.getPanelRoot(),
                        oldValue != null ? oldValue.getValue().fullName : null,
                        newValue.getValue().fullName);
            }
        });

        classTree.setOnMouseClicked(m -> {
            if (m.getClickCount() == 2) {
                final TreeItem<ClassTreeNode> item = classTree.getSelectionModel().getSelectedItem();

                if (item.isLeaf()) {
                    codeViewController.displayCodeTab(item.getValue().fullName, null);
                }
            }
        });

    }

    private void addVerticesToClassTree(ArrayList<ClassTreeNode> topLevel, StateVertex root) {

        if(root instanceof CodeEntity) {
            ClassTreeNode topLevelNode = getTopLevel(topLevel, ((CodeEntity) root).getClassName());
            boolean success = false;
            if (topLevelNode != null) {
                success = addVertex(topLevelNode, root);
            }

            if (!success) {
                System.out.println("Warning didn't find package for: " + ((CodeEntity) root).getClassName());
            }
        }

       HierarchicalGraph<StateVertex> innerGraph = root.getInnerGraph();
       for (StateVertex v : innerGraph.getVertices()) {
           addVerticesToClassTree(topLevel, v);
       }
    }

    private ClassTreeNode getTopLevel(ArrayList<ClassTreeNode> topLevel, String className) {

        for (ClassTreeNode n : topLevel) {
            if (className.startsWith(n.name))
                return n;
        }

        return null;
    }

    private boolean addVertex(ClassTreeNode node, StateVertex vertex) {

        return node.addVertex(vertex);
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
        HierarchicalGraph<StateVertex> innerGraph = v.getInnerGraph();
        for (StateVertex i : innerGraph.getVisibleVertices()) {
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
        HierarchicalGraph<TaintVertex> innerGraph = v.getInnerGraph();
        for(AbstractLayoutVertex i : innerGraph.getVisibleVertices()) {
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
        for(StateVertex v : this.getVizHighlighted()) {
            this.hidden.add(v);
        }

        this.vizHighlighted.clear();
        this.vizPanelController.resetAndRedraw();
    }

    public void showAllHiddenNodes() {
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

        HashSet<StateVertex> hide = new HashSet<>();

        this.vizPanelController.getPanelRoot().getInnerGraph().getVertices().stream().forEach(v -> {
            if (!keep.contains(v)) {
                hide.add(v);
            }
        });

        this.vizPanelController.startBatchMode();
        this.getHidden().addAll(hide);
        this.vizPanelController.endBatchMode();
        this.vizHighlighted.clear();
        this.vizPanelController.resetAndRedraw();
    }

    // TODO: This should be done using event and event handling using FieldSelectEvent
    public void selectFieldInTaintGraph(String fullClassName, String fieldName)
    {
        System.out.println("Find taint graph of " + fullClassName  + ":" + fieldName);
        taintPanelController.showFieldTaintGraph(fullClassName, fieldName);
    }

    // ClassTree Code -------------------------------------
    // Has a double function, either a folder (inner node) in which case it has no vertex;
    // Or a leaf node in which case it is associated to a one or more vertices
    class ClassTreeNode
    {
        public String name;
        public String fullName;
        public HashSet<ClassTreeNode> subDirs;
        public HashSet<StateVertex> vertices; // Leaf nodes store their associated vertices

        public ClassTreeNode(String name, String prefix)
        {
            this.name = name;
            this.subDirs = new HashSet<>();
            if(prefix == null)
                fullName = new String("");
            else if(prefix.compareTo("") == 0)
                fullName = name;
            else
                fullName = prefix + "." + name;
        }

        public ClassTreeNode addIfAbsent(String name)
        {
            ClassTreeNode subDir = null;
            for(ClassTreeNode f : subDirs)
            {
                if(f.name.compareTo(name) == 0)
                {
                    subDir = f;
                    break;
                }
            }
            if(subDir == null)
            {
                subDir = new ClassTreeNode(name, this.fullName);
                subDirs.add(subDir);
            }

            return subDir;
        }

        public void compress()
        {
            while(subDirs.size() == 1)
            {
                ClassTreeNode onlyElement = subDirs.iterator().next();
                name = name.concat("." + onlyElement.name);
                subDirs = onlyElement.subDirs;
            }

            for(ClassTreeNode f : subDirs)
                f.compress();

            if(subDirs.isEmpty())
                vertices = new HashSet<>();
        }

        @Override
        public String toString() {
            return name;
        }

        public String getFullName() { return  fullName; }

        public String toString(int depth) {
            StringBuilder subTree = new StringBuilder(depth + "-" + name + "\n");

            for (int i = 0; i < depth; i++) {
                subTree.insert(0, '\t');
            }

            for (ClassTreeNode f : subDirs) {
                subTree.append(f.toString(depth+1));
            }

            return subTree.toString();
        }

        private boolean isLeaf()
        {
            return subDirs.isEmpty();
        }

        private HashSet<StateVertex> getChildVertices()
        {
            if (this.isLeaf()) {
                return this.vertices;
            }
            else {
                HashSet<StateVertex> all = new HashSet<>();
                for (ClassTreeNode f : subDirs) {
                    f.getChildVertices(all);
                }
                return all;
            }
        }

        private void getChildVertices(HashSet<StateVertex> all)
        {
            if (this.isLeaf()) {
                all.addAll(this.vertices);
            } else {
                for (ClassTreeNode f : subDirs) {
                    f.getChildVertices(all);
                }
            }
        }

        public void build(TreeItem<ClassTreeNode> parent) {
            CheckBoxTreeItem<ClassTreeNode> item = new CheckBoxTreeItem<>();
            item.setSelected(true);
            item.setValue(this);
            item.setIndependent(false);
            item.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observableValue, Boolean prevVal, Boolean currVal) {

                    System.out.println("JUAN: Firing off " + item.getValue());
                    HashSet<StateVertex> childVertices = item.getValue().getChildVertices();
                    System.out.println("\t\tJUAN children is: " + childVertices);
                    System.out.println("Current value: " + currVal);
                    System.out.println("Previous value: " + prevVal);

                    vizPanelController.startBatchMode();
                    if(currVal) {
                        System.out.println("Showing nodes...");
                        Main.getSelectedMainTabController().getHidden().removeAll(childVertices);
                    } else {
                        System.out.println("Hiding nodes...");
                        Main.getSelectedMainTabController().getHidden().addAll(childVertices);
                    }
                    vizPanelController.endBatchMode();
                    vizPanelController.resetAndRedraw();
                }
            });
            parent.getChildren().add(item);

            for (ClassTreeNode f : subDirs) {
                f.build(item);
            }

            item.getChildren().sort(Comparator.comparing(t->t.getValue().name));
        }

        public boolean addVertex(StateVertex vertex) {

            if (vertex instanceof CodeEntity) {
                if (!this.subDirs.isEmpty()) {
                    for (ClassTreeNode n : this.subDirs) {
                        if (((CodeEntity) vertex).getClassName().startsWith(n.fullName)) {
                            return n.addVertex(vertex);
                        }
                    }
                    return false;
                }
            }

            this.vertices.add(vertex);
            return true;
        }
    } // End of class TreeNode

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

            if (!v.isInnerGraphEmpty()) {
                HierarchicalGraph<StateVertex> innerGraph = v.getInnerGraph();
                for (StateVertex i : innerGraph.getVertices()) {
                    setClassHighlight(i, prevPrefix, currPrefix);
                }
            }
        }
    }
    // End of ClassTree Code ------------------------------
}
