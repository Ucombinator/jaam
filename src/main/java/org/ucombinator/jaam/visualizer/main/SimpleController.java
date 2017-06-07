package org.ucombinator.jaam.visualizer.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.GUIUtils;
import org.ucombinator.jaam.visualizer.gui.OuterFrame;
import org.ucombinator.jaam.visualizer.gui.StacFrame;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by timothyjohnson on 5/5/17.
 */
public class SimpleController implements Initializable {
    @FXML // Values injected by FXMLLoader
    private TabPane tabPane;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        System.out.println("Initializing...");
    }

    public void loadGraph(ActionEvent event) {
        loadAnyGraph(false);
    }

    public void loadLoopGraph(ActionEvent event) {
        loadAnyGraph(true);
    }

    private void loadAnyGraph(boolean isLoopGraph) {
        Graph graph;
        TakeInput ti = new TakeInput();
        String filename = "";

        System.out.println("Load graph: start...");

        File file = GUIUtils.openFile(tabPane, "Load graph file");
        if (file == null) {
            System.out.println("Error! Invalid file.");
            return;
        }

        if(isLoopGraph)
            graph = ti.parseLoopGraph(file.getAbsolutePath());
        else
            graph = ti.parsePackets(file.getAbsolutePath());
        filename = file.getName();

        System.out.println("--> Create visualization: start...");
        StacFrame newTab = new StacFrame(graph);
        newTab.loadFXML();
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

    public TabPane getTabPane() {
        return this.tabPane;
    }
}
