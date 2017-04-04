package org.ucombinator.jaam.visualizer.main;

import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.StacFrame;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application
{
	public static Graph graph;

	public void start(Stage stage)
	{
		org.ucombinator.jaam.visualizer.main.Parameters.stFrame = new StacFrame();
		Scene scene = new Scene(org.ucombinator.jaam.visualizer.main.Parameters.stFrame, org.ucombinator.jaam.visualizer.main.Parameters.width,
				org.ucombinator.jaam.visualizer.main.Parameters.height);
		stage.setTitle("JAAM Visualizer");
		stage.setScene(scene);
		stage.show();

		TakeInput ti = new TakeInput();
		String file = "";
		ti.run(file, true);

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
}
