package org.ucombinator.jaam.visualizer.main;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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

	public void start(Stage stage) {
		URL url = getClass().getResource("/app.fxml");
		System.out.println("Loading url: " + url);
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(url);
			fxmlLoader.setController(new SimpleController());

			AnchorPane anchorPane = fxmlLoader.load(url);
			OuterFrame outerFrame = new OuterFrame(anchorPane);

			Scene scene = new Scene(anchorPane);
			stage.setScene(scene);
			stage.show();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public static OuterFrame getOuterFrame() {
		return outerFrame;
	}
}
