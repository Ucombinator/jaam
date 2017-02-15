package org.ucombinator.jaam.visualizer.main;

import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.gui.StacFrame;

public class Main
{
	public static Graph graph;

	public static void main(String[] args)
	{
		Parameters.stFrame = new StacFrame();
		TakeInput ti = new TakeInput();
		String file = "";

		for (int i = 0; i < args.length; i++)
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
		}
		
		ti.run(file, true);
	}
}
