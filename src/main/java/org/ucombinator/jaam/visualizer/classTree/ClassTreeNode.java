package org.ucombinator.jaam.visualizer.classTree;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.controllers.VizPanelController;
import org.ucombinator.jaam.visualizer.layout.CodeEntity;
import org.ucombinator.jaam.visualizer.layout.StateVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.Comparator;
import java.util.HashSet;

// ClassTree Code -------------------------------------
// Has a double function, either a folder (inner node) in which case it has no vertex;
// Or a leaf node in which case it is associated to a one or more vertices
public class ClassTreeNode
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

    public boolean isLeaf()
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

                VizPanelController vizPanelController = Main.getSelectedVizPanelController();

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