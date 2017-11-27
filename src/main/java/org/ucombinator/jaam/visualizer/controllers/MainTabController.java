package org.ucombinator.jaam.visualizer.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.*;
import org.ucombinator.jaam.visualizer.layout.*;
import com.strobel.decompiler.languages.java.ast.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MainTabController {
    public final Tab tab;
    public final VizPanelController vizPanelController;

    private HashSet<AbstractLayoutVertex> highlighted; // TODO: Make this an observable set
    private HashSet<AbstractLayoutVertex> hidden;

    public final CodeViewController codeViewController;

    // TODO: rename some of these
    @FXML public final VBox leftPane = null; // Initialized by Controllers.loadFXML()
    @FXML private final Node root = null; // Initialized by Controllers.loadFXML()
    @FXML private final BorderPane centerPane = null; // Initialized by Controllers.loadFXML()

    //Right Side Components
    @FXML private final TextArea descriptionArea = null; // Initialized by Controllers.loadFXML()

    @FXML private final TreeView<ClassTreeNode> classTree = null; // Initialized by Controllers.loadFXML()

    @FXML private final SearchResults searchResults = null; // Initialized by Controllers.loadFXML()

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

        this.highlighted = new LinkedHashSet<>();
        this.hidden = new LinkedHashSet<>();
        this.codeViewController = new CodeViewController(compilationUnits);
        this.leftPane.getChildren().add(this.codeViewController.codeTabs);

        this.codeViewController.addSelectHandler(centerPane);

        buildClassTree(this.codeViewController.getClassNames());
    }

    private void buildClassTree(HashSet<String> classNames)
    {
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

        for(ClassTreeNode f : root.subDirs)
        {
            topLevel.add(f);
        }

        // Compression Step
        for(ClassTreeNode f : topLevel)
        {
            f.compress();
        }

        // Build the Tree
        TreeItem<ClassTreeNode> treeRoot = new TreeItem<>();
        treeRoot.setValue(new ClassTreeNode("root", null));
        treeRoot.setExpanded(true);

        for(ClassTreeNode f : topLevel)
        {
            f.build(treeRoot);
        }

        classTree.setRoot(treeRoot);

        classTree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observableValue, Object oldValue, Object newValue) {

                TreeItem<ClassTreeNode> prev = (TreeItem<ClassTreeNode>)oldValue;
                TreeItem<ClassTreeNode> curr = (TreeItem<ClassTreeNode>)newValue;

                setClassHighlight(vizPanelController.getPanelRoot(),
                        prev != null? prev.getValue().fullName : null,
                        curr.getValue().fullName);
            }
        });

    }

    public void repaintAll() {
        System.out.println("Repainting all...");
        //bytecodeArea.setDescription();
        setRightText();
        searchResults.writeText(this);
    }

    public void setRightText() {
        StringBuilder text = new StringBuilder();
        for (AbstractLayoutVertex v : this.getHighlighted()) {
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
        for(AbstractLayoutVertex i : v.getInnerGraph().getVisibleVertices())
        {
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
            this.vizPanelController.getPanelRoot().searchByMethod(query, this);
        }

        this.repaintAll();
        /*Parameters.bytecodeArea.clear();
        Parameters.rightArea.setText("");*/
    }

    public void hideSelectedNodes() {
        System.out.println("Hiding selected nodes...");
        for(AbstractLayoutVertex v : this.getHighlighted()) {
            v.setHighlighted(false);
            v.setHidden();
            this.hidden.add(v);
        }

        this.highlighted = new LinkedHashSet<>();
        this.vizPanelController.resetAndRedraw();
    }

    public void showAllHiddenNodes() {
        System.out.println("Showing all hidden nodes...");
        for(AbstractLayoutVertex v : this.hidden) {
            v.setUnhidden();
        }

        this.hidden = new LinkedHashSet<>();
        this.vizPanelController.resetAndRedraw();
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

    public HashSet<AbstractLayoutVertex> getHighlighted() {
        return this.highlighted;
    }

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

    public void resetHighlighted(AbstractLayoutVertex newHighlighted)
    {
        System.out.println("Resetting highlighted: " + newHighlighted);
        for(AbstractLayoutVertex currHighlighted : highlighted) {
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
    class ClassTreeNode
    {
        public String name;
        public String fullName;
        public HashSet<ClassTreeNode> subDirs;

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


        public void build(TreeItem<ClassTreeNode> parent) {
            TreeItem<ClassTreeNode> item = new TreeItem<ClassTreeNode>();
            item.setValue(this);
            parent.getChildren().add(item);

            for(ClassTreeNode f : subDirs)
                f.build(item);
        }
    }

    private void setClassHighlight(AbstractLayoutVertex v, String prevName, String currName)
    {
        if(!v.isHidden()) {

            if (v instanceof CodeEntity) {
                if (((CodeEntity) v).getClassName().compareTo(currName) == 0) {
                    //System.out.println("Highlight " + ((CodeEntity) v).getClassName() + " --> " + ((CodeEntity) v).getMethodName() + " --> " + v.getId());
                    v.setClassHighlight(true);
                }
                else if(prevName != null && ((CodeEntity) v).getClassName().compareTo(prevName) == 0) {
                    v.setClassHighlight(false);
                }
            }

            if(!v.isInnerGraphEmpty())
            {
                for(AbstractLayoutVertex i : v.getInnerGraph().getVertices())
                {
                    setClassHighlight(i, prevName, currName);
                }
            }
        }
    }

    // End of ClassTree Code ------------------------------


}
