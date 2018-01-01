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

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MainTabController {
    public final Tab tab;
    public final VizPanelController vizPanelController;
    public final CodeViewController codeViewController;

    // Left Side Components
    @FXML public final VBox leftPane = null; // Initialized by Controllers.loadFXML()

    // Center Components
    @FXML private final Node root = null; // Initialized by Controllers.loadFXML()
    @FXML private final BorderPane centerPane = null; // Initialized by Controllers.loadFXML()

    //Right Side Components
    @FXML private final TextArea descriptionArea = null; // Initialized by Controllers.loadFXML()
    @FXML private final TreeView<ClassTreeNode> classTree = null; // Initialized by Controllers.loadFXML()
    @FXML private final SearchResults searchResults = null; // Initialized by Controllers.loadFXML()

    private HashSet<StateVertex> highlighted; // TODO: Make this an observable set
    private SetProperty<StateVertex> hidden;

    public enum SearchType {
        ID, TAG, INSTRUCTION, METHOD, ALL_LEAVES, ALL_SOURCES, OUT_OPEN, OUT_CLOSED, IN_OPEN, IN_CLOSED, ROOT_PATH
    }

    public MainTabController(File file, Graph graph, List<CompilationUnit> compilationUnits) throws IOException {
        Controllers.loadFXML("/MainTabContent.fxml", this);


        this.vizPanelController = new VizPanelController();
        this.centerPane.setCenter(this.vizPanelController.root);
        this.vizPanelController.initFX(graph);
        this.tab = new Tab(file.getName(), this.root);
        this.tab.tooltipProperty().set(new Tooltip(file.getAbsolutePath()));
        Controllers.put(this.tab, this);

        this.codeViewController = new CodeViewController(compilationUnits);
        this.leftPane.getChildren().add(this.codeViewController.codeTabs);

        this.codeViewController.addSelectHandler(centerPane);

        buildClassTree(this.codeViewController.getClassNames(), this.vizPanelController.getPanelRoot());

        this.highlighted = new LinkedHashSet<>();
        this.hidden = new SimpleSetProperty<StateVertex>(FXCollections.observableSet());

        this.hidden.addListener(this.vizPanelController);
    }

    private void buildClassTree(HashSet<String> classNames, LayoutRootVertex panelRoot)
    {
        this.classTree.setCellFactory(CheckBoxTreeCell.<ClassTreeNode>forTreeView());

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
        for(ClassTreeNode f : topLevel)
        {
            f.compress();
        }

        // Add the vertices
        addVerticesToClassTree(topLevel, panelRoot);


        // Build the Tree
        CheckBoxTreeItem<ClassTreeNode> treeRoot = new CheckBoxTreeItem<>();
        treeRoot.setSelected(true);
        treeRoot.setValue(new ClassTreeNode("root", null));
        treeRoot.setExpanded(true);

        for(ClassTreeNode f : topLevel)
        {
            f.build(treeRoot);
        }

        classTree.setRoot(treeRoot);

        classTree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<ClassTreeNode>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<ClassTreeNode>> observableValue,
                                TreeItem<ClassTreeNode> oldValue, TreeItem<ClassTreeNode> newValue) {

                setClassHighlight(vizPanelController.getPanelRoot(),
                        oldValue != null? oldValue.getValue().fullName : null,
                        newValue.getValue().fullName);
            }
        });

    }

    private void addVerticesToClassTree(ArrayList<ClassTreeNode> topLevel, AbstractLayoutVertex root) {

       if(root instanceof CodeEntity)
       {
           ClassTreeNode topLevelNode = getTopLevel(topLevel, ((CodeEntity) root).getClassName());
           boolean success = false;
           if(topLevelNode != null)
               success = addVertex(topLevelNode, root);

           if(!success)
               System.out.println("Warning didn't find package for: " + ((CodeEntity) root).getClassName());
       }

       HierarchicalGraph<AbstractLayoutVertex> innerGraph = root.getInnerGraph();
       for(AbstractLayoutVertex v : innerGraph.getVertices())
       {
           addVerticesToClassTree(topLevel, v);
       }
    }

    private ClassTreeNode getTopLevel(ArrayList<ClassTreeNode> topLevel, String className) {

        for(ClassTreeNode n : topLevel)
        {
            if(className.startsWith(n.name))
                return n;
        }

        return null;
    }

    private boolean addVertex(ClassTreeNode node, AbstractLayoutVertex vertex) {

        return node.addVertex(vertex);
    }

    public void repaintAll() {
        System.out.println("Repainting all...");
        //bytecodeArea.setDescription();
        setRightText();
        searchResults.writeText(this);
    }

    public void setRightText() {
        StringBuilder text = new StringBuilder();
        for (StateVertex v : this.getHighlighted()) {
            text.append(v.getRightPanelContent() + "\n");
        }

        this.descriptionArea.setText(text.toString());
    }

    public void setRightText(LayoutLoopVertex v)
    {
        this.descriptionArea.setText("Loop:\n  Class: "
                + v.getClassName().substring(v.getClassName().lastIndexOf(".")+1) + "\n  Method: "
                + v.getMethodName()     + "\n  Index: " +
                + v.getStatementIndex() + "\n  Signature: " + v.getLabel());
    }


    public void setRightText(LayoutMethodVertex v)
    {
        this.descriptionArea.setText("Method:\n  Class: "
                + v.getClassName().substring(v.getClassName().lastIndexOf(".")+1) + "\n  Method: "
                + v.getMethodName() + "\n  Signature: " + v.getLabel());
    }

    public void setRightText(LayoutSccVertex v)
    {
        StringBuilder text = new StringBuilder("SCC contains:\n");
        int k = 0;
        HierarchicalGraph<StateVertex> innerGraph = v.getInnerGraph();
        for(AbstractLayoutVertex i : innerGraph.getVisibleVertices()) {
            text.append(k++ + "  " + i.getLabel() + "\n");
        }
        this.descriptionArea.setText(text.toString());
    }

    public void setRightText(String text)
    {
        this.descriptionArea.setText(text);
    }

    // Clean up info from previous searches
    public void initSearch(SearchType search) {
        this.resetHighlighted(null);
        String query = getSearchInput(search);

        if (search == SearchType.ID) {
            searchByID(query); // TODO: Fix inconsistency with panel root
        } else if (search == SearchType.INSTRUCTION) {
            this.vizPanelController.getPanelRoot().searchByInstruction(query, this);
        } else if (search == SearchType.METHOD) {
            this.vizPanelController.getPanelRoot().searchByMethod(query.toLowerCase(), this);
        }

        this.repaintAll();
        /*Parameters.bytecodeArea.clear();
        Parameters.rightArea.setText("");*/
    }

    public ObservableSet<StateVertex> getHidden() {
        return hidden.get();
    }

    public void hideSelectedNodes() {
        for(StateVertex v : this.getHighlighted()) {
            this.hidden.add(v);
        }

        this.highlighted.clear();
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

    public HashSet<StateVertex> getHighlighted() {
        return this.highlighted;
    }

    /*
    //Called when the user clicks on a line in the left area.
    //Updates the vertex highlights to those that correspond to the instruction clicked.
    public void searchByJimpleIndex(String method, int index, boolean removeCurrent, boolean addChosen)
    {
        if(removeCurrent) {
            // Unhighlight currently highlighted vertices
            for (AbstractLayoutVertex v : this.highlighted) {
                v.setHighlighted(false);
            }
            highlighted.clear();
        }

        if(addChosen) {
            //Next we add the highlighted vertices
            HashSet<AbstractLayoutVertex> toAddHighlights = this.vizPanelController.getPanelRoot().getVerticesWithInstructionID(index, method);
            for (AbstractLayoutVertex v : toAddHighlights) {
                highlighted.add(v);
                v.setHighlighted(true);
            }
        } else {
            HashSet<AbstractLayoutVertex> toRemoveHighlights = this.vizPanelController.getPanelRoot().getVerticesWithInstructionID(index, method);
            for(AbstractLayoutVertex v : toRemoveHighlights) {
                v.setHighlighted(false);
            }
            highlighted.removeAll(toRemoveHighlights);
        }
    }
    */

    public void resetHighlighted(StateVertex newHighlighted)
    {
        System.out.println("Resetting highlighted: " + newHighlighted);
        for(StateVertex currHighlighted : highlighted) {
            currHighlighted.setHighlighted(false);
        }
        highlighted.clear();

        if(newHighlighted != null) {
            highlighted.add(newHighlighted);
            newHighlighted.setHighlighted(true);
        }
    }

    // ClassTree Code -------------------------------------

    // Has a double function, either a folder(inner node) in which case it has no vertex
    // Or a leaf node in which case it is associated to a one or more vertices
    class ClassTreeNode<T>
    {
        public String name;
        public String fullName;
        public HashSet<ClassTreeNode<T>> subDirs;
        public HashSet<T> vertices; // Leaf nodes store their associated vertices

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

            for(int i = 0; i < depth; ++i)
                subTree.insert(0, '\t');

            for(ClassTreeNode f : subDirs)
            {
                subTree.append(f.toString(depth+1));
            }

            return subTree.toString();
        }

        private boolean isLeaf()
        {
            return subDirs.isEmpty();
        }

        private HashSet<T> getChildrenVertices()
        {
            if(this.isLeaf())
                return this.vertices;

            HashSet<T> all = new HashSet<>();

            for(ClassTreeNode f : subDirs)
            {
                f.getChildrenVertices(all);
            }
            return all;
        }

        private void getChildrenVertices(HashSet<T> all)
        {
            if(this.isLeaf())
            {
                all.addAll(this.vertices);
            }
            else
            {
                for(ClassTreeNode f : subDirs)
                {
                    f.getChildrenVertices(all);
                }
            }
        }

        public void build(TreeItem<ClassTreeNode> parent) {
            CheckBoxTreeItem<ClassTreeNode> item = new CheckBoxTreeItem<>();
            item.setSelected(true);
            item.setValue(this);
            item.setIndependent(true);
            item.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observableValue, Boolean prevVal, Boolean currVal) {

                    System.out.println("JUAN: Firing off " + item.getValue());

                    HashSet<StateVertex> childVertices = item.getValue().getChildrenVertices();

                    System.out.println("\t\tJUAN children is: " + childVertices);

                    if(currVal)
                    {
                        Main.getSelectedMainTabController().getHidden().removeAll(childVertices);
                    }
                    else
                    {
                        Main.getSelectedMainTabController().getHidden().addAll(childVertices);
                    }
                }
            });
            parent.getChildren().add(item);

            for (ClassTreeNode f : subDirs)
                f.build(item);
        }

        public boolean addVertex(T vertex) {

            if(!this.subDirs.isEmpty())
            {
                for(ClassTreeNode<T> n : this.subDirs)
                {
                    if(((CodeEntity)vertex).getClassName().startsWith(n.fullName))
                    {
                        return n.addVertex(vertex);
                    }
                }
                return false;
            }

            this.vertices.add(vertex);
            return true;
        }

    } // End of class TreeNode

    private void setClassHighlight(AbstractLayoutVertex v, String prevPrefix, String currPrefix)
    {
        if(!v.isHidden()) {

            if (v instanceof CodeEntity) {
                if (((CodeEntity) v).getClassName().startsWith(currPrefix)) {
                    //System.out.println("Highlight " + ((CodeEntity) v).getClassName() + " --> " + ((CodeEntity) v).getMethodName() + " --> " + v.getId());
                    v.setClassHighlight(true);
                }
                else if(prevPrefix != null && ((CodeEntity) v).getClassName().startsWith(prevPrefix)) {
                    v.setClassHighlight(false);
                }
            }

            if(!v.isInnerGraphEmpty())
            {
                HierarchicalGraph<AbstractLayoutVertex> innerGraph = v.getInnerGraph();
                for(AbstractLayoutVertex i : innerGraph.getVertices())
                {
                    setClassHighlight(i, prevPrefix, currPrefix);
                }
            }
        }
    }

    // End of ClassTree Code ------------------------------


}
