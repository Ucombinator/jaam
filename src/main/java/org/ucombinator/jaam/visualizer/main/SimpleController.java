package org.ucombinator.jaam.visualizer.main;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by timothyjohnson on 5/5/17.
 */
public class SimpleController implements Initializable {
    @FXML // fx:id="myButton"
    private Button myButton; // Value injected by FXMLLoader

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        assert myButton != null : "fx:id=\"myButton\" was not injected: check your FXML file";
    }

    public void doSomething(ActionEvent event) {
        System.out.println("Hello world!");
        System.out.println(myButton.getText());
    }
}
