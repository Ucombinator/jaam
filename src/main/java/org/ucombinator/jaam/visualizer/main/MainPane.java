package org.ucombinator.jaam.visualizer.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.StacFrame;
import org.ucombinator.jaam.visualizer.gui.StacTab;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainPane implements Initializable {
    @FXML private AnchorPane root;
    public AnchorPane getRoot() { return this.root; }

    @FXML private TabPane tabPane;
    public TabPane getTabPane() { return this.tabPane; }

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

    @Override public void initialize(URL fxmlFileLocation, ResourceBundle resources) {}

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
        StacTab newTab = new StacTab(graph);
        System.out.println("<-- Create visualization: Done!");

        if (filename.equals(""))
            newTab.setText("Sample");
        else
            newTab.setText(filename);
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);
    }

    public void searchByID(ActionEvent event) {
        Main.getSelectedStacTabController().initSearch(StacFrame.SearchType.ID);
    }

    public void searchByStatement(ActionEvent event) {
        Main.getSelectedStacTabController().initSearch(StacFrame.SearchType.INSTRUCTION);
    }

    public void searchByMethod(ActionEvent event) {
        Main.getSelectedStacTabController().initSearch(StacFrame.SearchType.METHOD);
    }
}
