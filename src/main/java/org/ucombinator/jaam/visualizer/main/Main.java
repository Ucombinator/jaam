package org.ucombinator.jaam.visualizer.main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.ucombinator.jaam.visualizer.controllers.MainPane;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.gui.MainTab;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

public class Main extends Application
{
    private static MainPane mainPane;
    public static MainPane getMainPane() { return mainPane; }

    public static MainTab getSelectedStacTab() {
        return (MainTab) getMainPane().getTabPane().getSelectionModel().getSelectedItem();
    }

    public static MainTabController getSelectedStacTabController() {
        return getSelectedStacTab().controller;
    }

    public static VizPanel getSelectedMainPanel() {
        return getSelectedStacTabController().getMainPanel();
    }

    @Override
    public void start(Stage stage) {
        mainPane = new MainPane();
        Scene scene = new Scene(getMainPane().getRoot());
        stage.setScene(scene);
        stage.show();
    }
}
