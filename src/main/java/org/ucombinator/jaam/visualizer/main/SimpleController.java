package org.ucombinator.jaam.visualizer.main;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

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
        try {
            Tab tab = new Tab();
            tab.setContent(FXMLLoader.load(getClass().getResource("/tab.fxml")));
            tabPane.getTabs().add(tab);
        }
        catch(Exception e) {
            System.out.println(e);
        }
    }

    public void loadLoopGraph(ActionEvent event) {
        System.out.println("Loading loop graph...");

    }

    public void loadImage(ActionEvent event) {
        System.out.println("Loading image...");
    }

    public void searchByID(ActionEvent event) {
        System.out.println("Searching by ID...");
    }

    public void searchByStatement(ActionEvent event) {
        System.out.println("Searching by statement...");
    }

    public void searchByMethod(ActionEvent event) {
        System.out.println("Searching by method...");
    }
}
