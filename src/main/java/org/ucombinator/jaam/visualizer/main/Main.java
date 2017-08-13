package org.ucombinator.jaam.visualizer.main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.controlsfx.dialog.ExceptionDialog;
import org.ucombinator.jaam.visualizer.controllers.MainPaneController;
import org.ucombinator.jaam.visualizer.controllers.MainTabController;
import org.ucombinator.jaam.visualizer.gui.MainTab;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

public class Main extends Application {
    private static MainPaneController mainPane;

    public static MainPaneController getMainPane() {
        return mainPane;
    }

    public static MainTab getSelectedMainTab() {
        return (MainTab) getMainPane().getTabPane().getSelectionModel().getSelectedItem();
    }

    public static MainTabController getSelectedMainTabController() {
        return getSelectedMainTab().controller;
    }

    public static VizPanel getSelectedVizPanel() {
        return getSelectedMainTabController().getMainPanel();
    }

    private static void uncaughtExceptionHandler(Thread t, Throwable e) {
        ExceptionDialog dialog = new ExceptionDialog(e);
        dialog.setTitle("Exception");
        dialog.setHeaderText("Exception in " + t);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler(Main::uncaughtExceptionHandler);

        mainPane = new MainPaneController();
        Scene scene = new Scene(getMainPane().getRoot());
        stage.setScene(scene);
        stage.show();
    }
}
