package org.ucombinator.jaam.visualizer.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.StacFrame;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainPane implements Initializable {
    public MainPane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/MainPane.fxml"));
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // Using instead of Platform.exit because we want a non-zero exit code
        }
    }

    @FXML
    private AnchorPane root;
    public AnchorPane getRoot() { return this.root; }

    @FXML // Values injected by FXMLLoader
    private TabPane tabPane;
    public TabPane getTabPane() { return this.tabPane; }

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        System.out.println("Initializing...");
    }

    public void loadLoopGraph(ActionEvent event) {
        Graph graph;
        TakeInput ti = new TakeInput();
        String filename = "";

        System.out.println("Load graph: start...");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load graph file");
        File file = fileChooser.showOpenDialog(getTabPane().getScene().getWindow());

        if (file == null) {
            System.out.println("Error! Invalid file.");
            return;
        }

        String path = file.getAbsolutePath();
        System.out.println("File path: " + path);
        graph = ti.parseLoopGraph(file.getAbsolutePath());
        filename = file.getName();

        System.out.println("--> Create visualization: start...");
        StacFrame newTab = new StacFrame(graph);
        System.out.println("<-- Create visualization: Done!");

        if (filename.equals(""))
            newTab.setText("Sample");
        else
            newTab.setText(filename);
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);
    }

    public void searchByID(ActionEvent event) {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if(currentTab instanceof StacFrame) {
            StacFrame currentFrame = (StacFrame) currentTab;
            currentFrame.initSearch(StacFrame.searchType.ID);
        }
        else {
            System.out.println("Error! Current tab is not a StacFrame.");
        }
    }

    public void searchByStatement(ActionEvent event) {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if(currentTab instanceof StacFrame) {
            StacFrame currentFrame = (StacFrame) currentTab;
            currentFrame.initSearch(StacFrame.searchType.INSTRUCTION);
        }
        else {
            System.out.println("Error! Current tab is not a StacFrame.");
        }
    }

    public void searchByMethod(ActionEvent event) {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if(currentTab instanceof StacFrame) {
            StacFrame currentFrame = (StacFrame) currentTab;
            currentFrame.initSearch(StacFrame.searchType.METHOD);
        }
        else {
            System.out.println("Error! Current tab is not a StacFrame.");
        }
    }
}
