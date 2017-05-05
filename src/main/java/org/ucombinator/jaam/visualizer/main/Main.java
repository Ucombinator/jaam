package org.ucombinator.jaam.visualizer.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
		this.outerFrame = new OuterFrame();
		Scene scene = new Scene(outerFrame, org.ucombinator.jaam.visualizer.main.Parameters.width,
				org.ucombinator.jaam.visualizer.main.Parameters.height);
		stage.setTitle("JAAM Visualizer");
		stage.setWidth(Toolkit.getDefaultToolkit().getScreenSize().getWidth());
		stage.setHeight(Toolkit.getDefaultToolkit().getScreenSize().getHeight());
		stage.setScene(scene);
		stage.show();

		// Read dummy graph
		if(org.ucombinator.jaam.visualizer.main.Parameters.loadSampleGraph) {
			outerFrame.loadGraph(false, false);
		}

		// TODO: Build GUI in SceneBuilder
		// Example code for loading scene from SceneBuilder
		/*URL url = getClass().getResource("/testgui.fxml");
		try {
			Pane pane = FXMLLoader.load(url);
			Scene scene = new Scene(pane);
			stage.setScene(scene);
			stage.show();
		}
		catch(Exception e) {
			System.out.println(e);
		}*/
	}

	public static OuterFrame getOuterFrame() {
		return outerFrame;
	}
}
