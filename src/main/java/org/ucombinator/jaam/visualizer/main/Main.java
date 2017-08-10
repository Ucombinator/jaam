package org.ucombinator.jaam.visualizer.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.ucombinator.jaam.visualizer.gui.StacFrame;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

import java.io.IOException;
import java.net.URL;

public class Main extends Application
{
    private static SimpleController controller;
    private static AnchorPane anchorPane;

    @Override
    public void start(Stage stage) {
        URL url = getClass().getResource("/app.fxml");
        System.out.println("Loading url: " + url);
        FXMLLoader fxmlLoader = new FXMLLoader(url);
        controller = new SimpleController();
        fxmlLoader.setController(controller);

        try {
            Main.anchorPane = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scene scene = new Scene(anchorPane);
        stage.setScene(scene);
        stage.show();
    }

    public static StacFrame getSelectedStacFrame() {
        return (StacFrame)controller.getTabPane().getSelectionModel().getSelectedItem();
    }

    public static VizPanel getSelectedMainPanel() {
        return getSelectedStacFrame().getMainPanel();
    }

    public static AnchorPane getAnchorPane() {
        return anchorPane;
    }
}
