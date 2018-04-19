package org.ucombinator.jaam.visualizer.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import org.ucombinator.jaam.visualizer.search.SearchTreeNode;

import java.io.IOException;

public class SearchResultsController {
    @FXML protected final BorderPane root = null; // Initialized by Controllers.loadFXML()
    @FXML protected TreeView<String> searchTree = null; // Initialized by Controllers.loadFXML()
    @FXML private TextField searchText = null; // Initialized by Controllers.loadFXML()
    private SearchTreeNode searchRoot;

    public SearchResultsController() throws IOException {
        Controllers.loadFXML("/SearchResults.fxml", this);
        this.searchRoot = new SearchTreeNode("Search Results");
        this.searchTree.setRoot(this.searchRoot);
    }

    // Called when the user clicks enter inside the search field.
    @FXML public void performSearch(ActionEvent event) {
        String query = this.searchText.getText();
        System.out.println("Search to perform: " + query);
    }
}
