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
    private static SimpleController controller; // TODO: add 'root' (anchorPane)
    public static SimpleController getController() { return controller; }

    private static AnchorPane anchorPane;
    public static AnchorPane getAnchorPane() { return anchorPane; }

    public static StacFrame getSelectedStacFrame() {
        return (StacFrame)controller.getTabPane().getSelectionModel().getSelectedItem();
    }

    public static VizPanel getSelectedMainPanel() {
        return getSelectedStacFrame().getMainPanel();
    }

    @Override
    public void start(Stage stage) {
        controller = new SimpleController();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/app.fxml"));
        fxmlLoader.setController(controller);

        try {
            anchorPane = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scene scene = new Scene(anchorPane);
        stage.setScene(scene);
        stage.show();
    }

}
