package org.ucombinator.jaam.visualizer.main;

import java.awt.Toolkit;

import org.ucombinator.jaam.visualizer.graph.Graph;


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.ucombinator.jaam.visualizer.gui.OuterFrame;

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

		/*for (int i = 0; i < args.length; i++)
		{
			if (args[i].equalsIgnoreCase("--refresh") || args[i].equalsIgnoreCase("-r"))
			{
				Parameters.interval = Long.parseLong(args[i].trim());
			}
			else if (args[i].equalsIgnoreCase("--limit") || args[i].equalsIgnoreCase("-l"))
			{
				Parameters.limitV = Long.parseLong(args[i].trim());
			}
			else if (args[i].equalsIgnoreCase("--file") || args[i].equalsIgnoreCase("-f"))
			{
				i++;

				if (args[i].startsWith("./") || args[i].startsWith("/"))
				{
					file = args[i];
				}
				else
				{
					file = "./" + args[i];
				}

				Parameters.currDirectory = Parameters.folderFromPath(file);
			}
			// TODO: What should happen here?
			else if (args[i].equalsIgnoreCase("--pipe") || args[i].equalsIgnoreCase("-p")) {}
			else if (args[i].equalsIgnoreCase("--debug") || args[i].equalsIgnoreCase("-d"))
			{
				i++;
				Parameters.debug1 = Integer.parseInt(args[i].trim());
				i++;
				Parameters.debug2 = Integer.parseInt(args[i].trim());
			}
			else
			{
				System.out.println("ignoring invalid input option:\"" + args[i] + "\"");
			}
		}*/
	}

	public static OuterFrame getOuterFrame() {
		return outerFrame;
	}
}
