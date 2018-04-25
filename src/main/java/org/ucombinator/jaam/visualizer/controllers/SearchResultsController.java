package org.ucombinator.jaam.visualizer.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import org.ucombinator.jaam.visualizer.classTree.ClassTreeNode;
import org.ucombinator.jaam.visualizer.classTree.ClassTreeUtils;
import org.ucombinator.jaam.visualizer.classTree.PackageNode;
import org.ucombinator.jaam.visualizer.main.Main;

import java.io.IOException;

public class SearchResultsController {
    @FXML protected final BorderPane root = null; // Initialized by Controllers.loadFXML()
    @FXML protected TreeView<ClassTreeNode> searchTree = null; // Initialized by Controllers.loadFXML()
    @FXML private TextField searchText = null; // Initialized by Controllers.loadFXML()

    public SearchResultsController() throws IOException {
        Controllers.loadFXML("/SearchResults.fxml", this);
        TreeItem<ClassTreeNode> rootItem = new TreeItem<>(new PackageNode("Search results:", ""));
        this.searchTree.setRoot(rootItem);
        this.searchTree.setVisible(true);
        ClassTreeUtils.setEventHandlers(this.searchTree);
    }

    // Called when the user clicks enter inside the search field.
    @FXML public void performSearch(ActionEvent event) {
        String query = this.searchText.getText();
        System.out.println("Search to perform: " + query);

        // Make root for loop graph results, since we'll eventually search other areas as well.
        this.searchTree.getRoot().getChildren().clear();
        TreeItem<ClassTreeNode> loopGraphRoot = new TreeItem<>(new PackageNode("Loop graph results:", ""));
        this.searchTree.getRoot().getChildren().add(loopGraphRoot);

        // Add all loop graph results.
        TreeItem<ClassTreeNode> classRootItem = Main.getSelectedMainTabController().getClassTree().getRoot();
        for (TreeItem<ClassTreeNode> topLevelItem: classRootItem.getChildren()) {
            TreeItem<ClassTreeNode> newSearchRootItem = ClassTreeUtils.buildFilteredTree(topLevelItem, query);
            if (newSearchRootItem != null) {
                loopGraphRoot.getChildren().add(newSearchRootItem);
            }
        }

        // Expand results.
        this.searchTree.getRoot().setExpanded(true);
        loopGraphRoot.setExpanded(true);
    }
}
