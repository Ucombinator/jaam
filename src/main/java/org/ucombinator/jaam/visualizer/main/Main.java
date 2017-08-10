package org.ucombinator.jaam.visualizer.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.ucombinator.jaam.visualizer.gui.StacFrame;
import org.ucombinator.jaam.visualizer.gui.VizPanel;
import java.io.IOException;

public class Main extends Application
{
    private static SimpleController controller; // TODO: add 'root' (anchorPane)
    public static SimpleController getController() { return controller; }

    public static StacFrame getSelectedStacFrame() {
        return (StacFrame)getController().getTabPane().getSelectionModel().getSelectedItem();
    }

    public static VizPanel getSelectedMainPanel() {
        return getSelectedStacFrame().getMainPanel();
    }

    @Override
    public void start(Stage stage) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/app.fxml"));

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        controller = fxmlLoader.getController();

        Scene scene = new Scene(getController().getRoot());
        stage.setScene(scene);
        stage.show();
    }
}
