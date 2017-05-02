package org.ucombinator.jaam.visualizer.main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.ucombinator.jaam.visualizer.gui.OuterFrame;

import java.awt.Toolkit;

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
			outerFrame.loadGraph(false);
		}
	}

	public static OuterFrame getOuterFrame() {
		return outerFrame;
	}
}
