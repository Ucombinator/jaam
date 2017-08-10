package org.ucombinator.jaam.visualizer.main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.ucombinator.jaam.visualizer.gui.StacFrame;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

public class Main extends Application
{
    private static MainPane mainPane;
    public static MainPane getMainPane() { return mainPane; }

    public static StacFrame getSelectedStacFrame() {
        return (StacFrame) getMainPane().getTabPane().getSelectionModel().getSelectedItem();
    }

    public static VizPanel getSelectedMainPanel() {
        return getSelectedStacFrame().getMainPanel();
    }

    @Override
    public void start(Stage stage) {
        mainPane = new MainPane();
        Scene scene = new Scene(getMainPane().getRoot());
        stage.setScene(scene);
        stage.show();
    }
}
