package org.ucombinator.jaam.visualizer.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.stage.Stage;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import org.ucombinator.jaam.visualizer.controllers.*;

import java.io.File;
import java.io.IOException;

public class Main extends Application {
    private static MainPaneController mainPane;

    public static Tab getSelectedMainTab() {
        return Main.mainPane.tabPane.getSelectionModel().getSelectedItem();
    }

    public static MainTabController getSelectedMainTabController() {
        return Controllers.get(getSelectedMainTab());
    }

    public static VizPanelController getSelectedVizPanelController() {
        return getSelectedMainTabController().vizPanelController;
    }

    public static TaintPanelController getSelectedTaintPanelController() {
        return getSelectedMainTabController().taintPanelController;
    }

    public static GlyphFont getIconFont() {
        GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

        return fontAwesome;
    }

    private static void uncaughtExceptionHandler(Thread t, Throwable e) {
        if (Platform.isFxApplicationThread()) {
            ExceptionDialog dialog = new ExceptionDialog(e);
            dialog.setTitle("Exception");
            dialog.setHeaderText("Exception in " + t);
            dialog.setResizable(true);
            Platform.runLater(dialog::showAndWait); // `showAndWait` is not allowed during animation and processing
        } else {
            System.err.println("***** Exception in " + t + "*****");
            e.printStackTrace();
            System.exit(1); // Using instead of Platform.exit because we want a non-zero exit code
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        Thread.setDefaultUncaughtExceptionHandler(Main::uncaughtExceptionHandler);

        // Prevent the window from being so small that the user cannot grab it
        stage.setMinHeight(100);
        stage.setMinWidth(100);

        Main.mainPane = new MainPaneController();
        Scene scene = new Scene(Main.mainPane.root);
        stage.setScene(scene);
        stage.show();

        for (String arg : this.getParameters().getRaw()) {
            Main.mainPane.loadLoopGraphFile(new File(arg));
        }
    }
}
