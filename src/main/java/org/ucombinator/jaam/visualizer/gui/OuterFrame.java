package org.ucombinator.jaam.visualizer.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.io.File;

import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.main.TakeInput;

/**
 * Created by timothyjohnson on 4/17/17.
 */
public class OuterFrame {
    private AnchorPane anchorPane;
    private TabPane tabPane;

    public OuterFrame(AnchorPane anchorPane) {
        this.anchorPane = anchorPane;

        for(Node child : this.anchorPane.getChildren())
            if(child instanceof TabPane)
                this.tabPane = (TabPane) child;
    }

    public void loadGraph(boolean chooseFile, boolean isLoopGraph)
    {
        Graph graph;
        TakeInput ti = new TakeInput();
        String filename = "";

    	System.out.println("Load graph: start...");

    	if(chooseFile) {
            File file = GUIUtils.openFile(this.tabPane, "Load graph file");
            if (file == null) {
                System.out.println("Error! Invalid file.");
                return;
            }
            /*if(isLoopGraph)
                graph = ti.parseLoopGraph(file.getAbsolutePath());
            else*/
            graph = ti.parseStateGraph(file.getAbsolutePath());
            filename = file.getName();
        }
        else {
            graph = ti.parseStateGraph("");
        }
        
        System.out.println("--> Create visualization: start...");
        StacFrame newFrame = new StacFrame(graph);
        System.out.println("<-- Create visualization: Done!");

        this.tabPane.getTabs().add(newFrame);
        tabPane.getSelectionModel().select(newFrame);
        System.out.println("Load graph: done!");
    }

    public StacFrame getCurrentFrame() {
        Tab currentTab = this.getCurrentTab();
        if(currentTab instanceof StacFrame)
            return (StacFrame) currentTab;
        else
            return null;
    }
    
    public Tab getCurrentTab() {
        return this.tabPane.getSelectionModel().getSelectedItem();
    }

    public AnchorPane getAnchorPane() {
        return this.anchorPane;
    }
}
