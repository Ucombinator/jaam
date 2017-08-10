package org.ucombinator.jaam.visualizer.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.ucombinator.jaam.visualizer.gui.StacFrame;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

import java.net.URL;

public class Main extends Application
{
    private static SimpleController controller;
    private static AnchorPane anchorPane;

    @Override
    public void start(Stage stage) {
        URL url = getClass().getResource("/app.fxml");
        System.out.println("Loading url: " + url);
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(url);
            controller = new SimpleController();
            fxmlLoader.setController(controller);

            AnchorPane anchorPane = fxmlLoader.load();
            Main.anchorPane = anchorPane;

            Scene scene = new Scene(anchorPane);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static StacFrame getSelectedStacFrame() {
        return (StacFrame)getTabPane().getSelectionModel().getSelectedItem();
    }

    public static VizPanel getSelectedMainPanel() {
        return getSelectedStacFrame().getMainPanel();
    }

    public static AnchorPane getAnchorPane() {
        return anchorPane;
    }

    public static TabPane getTabPane() {
        return controller.getTabPane();
    }
}
