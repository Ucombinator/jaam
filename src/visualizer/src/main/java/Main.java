
public class Main
{
	public static Graph graph;
	
	public static void main(String[] args)
	{
		Parameters.stFrame = new StacFrame();
		boolean load = false;
		TakeInput ti = new TakeInput();
		String file = "";

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equalsIgnoreCase("--refresh") || args[i].equalsIgnoreCase("-r")) {
				Parameters.interval = Long.parseLong(args[i].trim());
			} else if (args[i].equalsIgnoreCase("--limit") || args[i].equalsIgnoreCase("-l")) {
				Parameters.limitV = Long.parseLong(args[i].trim());
			} else if (args[i].equalsIgnoreCase("--file") || args[i].equalsIgnoreCase("-f")) {
				i++;

				if (args[i].startsWith("./") || args[i].startsWith("/")) {
					file = args[i];
				} else {
					file = "./" + args[i];
				}

				Parameters.pwd = Parameters.folderFromPath(file);

				load = true;
			} else if (args[i].equalsIgnoreCase("--pipe") || args[i].equalsIgnoreCase("-p")) {
				load = true;
			} else if (args[i].equalsIgnoreCase("--debug") || args[i].equalsIgnoreCase("-d")) {
				i++;
				Parameters.debug1 = Integer.parseInt(args[i].trim());
				i++;
				Parameters.debug2 = Integer.parseInt(args[i].trim());

			} else {
				System.out.println("ignoring invalid input option:\"" + args[i] + "\"");
			}
		}

		if (load) {
			//From the command line, we can only load message file
			ti.run(file, true);
			Parameters.repaintAll();
		}
	}
}
