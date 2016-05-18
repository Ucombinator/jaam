
public class StacViz
{

	public static Graph graph;
	
	public static void main(String[] args)
	{
		Parameters.stFrame = new StacFrame();
		boolean fromJson = false, load=false;
		TakeInput ti = new TakeInput();
		for(int i = 0; i < args.length; i++)
		{
			if(args[i].equalsIgnoreCase("cfg"))
				fromJson = true;
			else if(args[i].equalsIgnoreCase("default"))
				fromJson = false;
			else if(args[i].equalsIgnoreCase("--refresh")|| args[i].equalsIgnoreCase("-r"))
			{
				i++;
				Parameters.interval = Long.parseLong(args[i].trim());
			}
			else if(args[i].equalsIgnoreCase("--limit")|| args[i].equalsIgnoreCase("-l"))
			{
				i++;
				Parameters.limitV = Long.parseLong(args[i].trim());
			}
			else if(args[i].equalsIgnoreCase("--file")||args[i].equalsIgnoreCase("-f"))
			{
				i++;
				
				String file ="";
				if(args[i].startsWith("./")||args[i].startsWith("/"))
				{
					file=args[i];
				}
				else
				{
					file="./"+args[i];
				}
				
				Parameters.pwd = Parameters.folderFromPath(file);

				ti.setFileInput(file);
				load = true;
			}
			else if(args[i].equalsIgnoreCase("--pipe")||args[i].equalsIgnoreCase("-p"))
			{
				ti.setSystemInput();
				load = true;
			}
			else if(args[i].equalsIgnoreCase("--debug")||args[i].equalsIgnoreCase("-d"))
			{
				i++;
				Parameters.debug1 = Integer.parseInt(args[i].trim());
				i++;
				Parameters.debug2 = Integer.parseInt(args[i].trim());

			}
			else
			{
				System.out.println("ignoring invalid input option:\""+args[i]+"\"");
			}
		}
		
		if(load)
		{
			ti.run(fromJson);
			Parameters.refreshBoth();
		}
	}
}
