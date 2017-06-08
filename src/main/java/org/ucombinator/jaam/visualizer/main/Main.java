package org.ucombinator.jaam.visualizer.main;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import org.ucombinator.jaam.visualizer.gui.OuterFrame;

import java.awt.Toolkit;
import java.net.URL;

public class Main extends Application
{
	private static OuterFrame outerFrame;
	private static boolean useFXML = false;
	private static SimpleController controller;

	public void start(Stage stage) {
		URL url = getClass().getResource("/app.fxml");
		System.out.println("Loading url: " + url);
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(url);
			controller = new SimpleController();
			fxmlLoader.setController(controller);

			AnchorPane anchorPane = fxmlLoader.load();
			outerFrame = new OuterFrame(anchorPane);

			Scene scene = new Scene(anchorPane);
			stage.setScene(scene);
			stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static OuterFrame getOuterFrame() {
		return outerFrame;
	}

	public static TabPane getTabPane() {
		return controller.getTabPane();
	}
}
