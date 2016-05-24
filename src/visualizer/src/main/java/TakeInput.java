
import java.io.*;
import java.util.regex.*;
import java.util.StringTokenizer;
import org.ucombinator.jaam.messaging.*;
import org.ucombinator.jaam.messaging.Message.Input;

public class TakeInput extends Thread
{
	BufferedReader parseInput;
	Input messageInput;

	//If file is empty, we read from System.in
	public void run(String file, boolean fromMessages)
	{
		Main.graph = new Graph();

		if(!fromMessages)
			this.parseDefault(file);
		else
			this.parseMessages(file);

		Main.graph.finalizeParentsForRootChildren();
		Main.graph.identifyLoops();
		Main.graph.calcLoopHeights();
		Main.graph.mergeAllByMethod();
		Main.graph.mergePaths();
		Main.graph.computeInstLists();
		Main.graph.setAllMethodHeight();
		Main.graph.collapseAll();
		Parameters.mouseLastTime = System.currentTimeMillis();
		Parameters.repaintAll();

		System.out.println("number of vertices = " + Main.graph.vertices.size());
		System.out.println("number of method vertices = " + Main.graph.methodVertices.size());
	}
	
	private void setFileInput(String file)
	{
		try
		{
			this.parseInput = new BufferedReader(new FileReader(file));
			
			if(this.parseInput == null)
				System.out.println("null file");
			else
				Parameters.stFrame.setTitle("STAC Visualizer: " + file);			
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	private void parseDefault(String file)
	{
		if(file.equals(""))
			this.parseInput = new BufferedReader(new InputStreamReader(System.in));
		else
			this.setFileInput(file);

		String line;
		int id = -1, start = 0, end = 0;
		String desc = "", tempStr = "";
		Pattern vertexPattern = Pattern.compile("(\"(\\d+)\":)");

		Parameters.cut_off = false;
		Parameters.startTime = System.currentTimeMillis();
		Parameters.lastInterval = -1;

		try
		{
			line = parseInput.readLine();
			while(!Parameters.cut_off)
			{
				if(Main.graph.vertices.size()>=Parameters.limitV)
				{
					Parameters.cut_off = true;
					break;
				}

				else if(line == null || line.length() == 0)
				{
					line = parseInput.readLine();
					continue;
				}

				else if(line.equals("Done!"))
				{
					System.out.println("Done!");
					Parameters.cut_off = true;
					break;
				}

				else if(line.contains("\"states\":{"))
				{
					tempStr = "";

					while(true)
					{
						line = parseInput.readLine();
						if(line.trim().equalsIgnoreCase("\"edges\":["))
							break;

						tempStr = tempStr + line + "\n";

					}

					Matcher vertexMatcher = vertexPattern.matcher(tempStr);
					if(vertexMatcher.find())
					{
						id = Integer.parseInt(vertexMatcher.group(2));
						start = vertexMatcher.end(1);
						while(vertexMatcher.find())
						{
							end = vertexMatcher.start(1);
							desc = tempStr.substring(start, end - 2);
							desc = desc.substring(0, desc.lastIndexOf("\n")-1);
							this.defaultAddVertex(id, desc);
							id = Integer.parseInt(vertexMatcher.group(2));
							start = vertexMatcher.end(1);
						}

						tempStr = tempStr.substring(0, tempStr.length() - 1);
						tempStr = tempStr.substring(0, tempStr.lastIndexOf("\n"));
						desc = tempStr.substring(start);
						this.defaultAddVertex(id, desc);
						if((System.currentTimeMillis()-Parameters.startTime)/Parameters.interval>Parameters.lastInterval)
						{
							System.out.println("number of vertices up to now = "+Main.graph.vertices.size());
							Parameters.lastInterval = (System.currentTimeMillis()-Parameters.startTime)/Parameters.interval;
						}
					}

					while(true)
					{
						line = parseInput.readLine();
						if(line.trim().equalsIgnoreCase(""))
							break;

						line = parseInput.readLine();

						tempStr = line.trim();
						tempStr = tempStr.substring(0, tempStr.length()-1);
						tempStr = tempStr + " -> ";

						line = parseInput.readLine();

						tempStr = tempStr + line.trim();
						Main.graph.addEdge(tempStr);
						if((System.currentTimeMillis()-Parameters.startTime)/Parameters.interval>Parameters.lastInterval)
						{
							System.out.println("number of vertices up to now = "+Main.graph.vertices.size());
							Parameters.repaintAll();
							Parameters.lastInterval = (System.currentTimeMillis()-Parameters.startTime)/Parameters.interval;
						}

						line = parseInput.readLine();

						if(!line.contains(","))
							break;
					}
				}

				line = parseInput.readLine();
			}

			this.parseInput.close();
		}
		catch(IOException e)
		{
			System.out.println(e);
		}
	}

	public void parseMessages(String file)
	{
		if(file.equals(""))
			messageInput = new Input(System.in);
		try
		{
			messageInput = new Input(new FileInputStream(file));
			Message message = Message.read(messageInput);

			while(!(message instanceof Done))
			{
				//Name collision with our own Edge class
				if(message instanceof org.ucombinator.jaam.messaging.Edge)
				{
					org.ucombinator.jaam.messaging.Edge edgeMessage = (org.ucombinator.jaam.messaging.Edge) message;
					int edgeId = edgeMessage.id().id();
					int srcId = edgeMessage.src().id();
					int destId = edgeMessage.dst().id();
					Main.graph.addEdge(srcId, destId);
				}
				else if(message instanceof ErrorState)
				{
					int id = ((ErrorState) message).id().id();
					Main.graph.addErrorState(id);
				}
				//Name collision with java.lang.Thread.State
				else if(message instanceof org.ucombinator.jaam.messaging.State)
				{
					org.ucombinator.jaam.messaging.State stateMessage = (org.ucombinator.jaam.messaging.State) message;
					int id = stateMessage.id().id();
					String methodName = stateMessage.stmt().method().toString();
					String instruction = stateMessage.stmt().stmt().toString();
					int jimpleIndex = stateMessage.stmt().index();
					Main.graph.addVertex(id, methodName, instruction, "", jimpleIndex, true);
				}

				message = Message.read(messageInput);
			}
		}
		catch(FileNotFoundException e)
		{
			System.out.println(e);
		}
	}
	
	public void defaultAddVertex(int id, String description)
	{
		String desc = ""+description;

		int start = desc.indexOf("\"$type\":\"org.ucombinator.jaam.Stmt\"")+2;
		int end = desc.indexOf("\"fp\":{");
		if(start >= 0 && end >= 0)
		{
			desc = desc.substring(start, end - 3);
			start = desc.indexOf("\n");
			end = desc.lastIndexOf("\n");
			desc = desc.substring(start + 1, end);
	
			Pattern stmtPattern = Pattern.compile(
					"\\s*\"sootStmt\":\"(.*)\",\n\\s*"
					+ "\"sootMethod\":\\{\n\\s*"
					+ "\"\\$type\":\"soot.SootMethod\",\n\\s*"
					+ "\"declaringClass\":\"(.*)\",\n\\s*"
					+ "\"name\":\"(.*)\",\n\\s*"
					+ "\"parameterTypes\":\\[\\s*([[^,]*,\\s*]*.*)\\s*\\],\n\\s*"
					+ "\"returnType\":\"(.*)\",\n\\s*"
					+ "\"modifiers\":(\\d+),\n\\s*"
					+ "\"exceptions\":\\[\\s*(.*)\\s*\\]\\s*\\},\\s*"
					+ "\"index\":(-?\\d+),\n\\s*"
					+ "\"line\":(-?\\d+),\n\\s*"
					+ "\"column\":(-?\\d+),\n\\s*"
					+ "\"sourceFile\":\"(.*)\"");
			
			Matcher stmtMatcher = stmtPattern.matcher(desc);
			if(stmtMatcher.find())
			{
				desc = stmtMatcher.group(0);
				String inst = this.stmtMatcherToInst(stmtMatcher);
				String method = stmtMatcherToMethod(stmtMatcher);
				int ind = Integer.parseInt(stmtMatcher.group(8));

				Main.graph.addVertex(id, method, inst, description, ind, true);
			}
			else
			{
				System.out.println("Cannot parse vertex");
				System.out.println(description);
			}
		}
		else if(desc.indexOf("org.ucombinator.jaam.ErrorState$") >= 0)
		{
			Main.graph.addErrorState(id);
		}
		else
		{
			System.out.println("ERROR! The input contains a vertex type that has not been implemented in the parser.");
		}
	}
	
	public String stmtMatcherToInst(Matcher stmtMatcher)
	{
		return stmtMatcher.group(1);
	}

	public String stmtMatcherToMethod(Matcher stmtMatcher)
	{
		return stmtMatcher.group(5) + " " + stmtMatcher.group(2) + "::" + stmtMatcher.group(3) + "(" + this.getParameters(stmtMatcher.group(4)) + ")";
	}
	
	public String getParameters(String str)
	{
		if(str.trim().equalsIgnoreCase(""))
			return "";
		StringTokenizer token = new StringTokenizer(str.trim());
		String toReturn = token.nextToken();
		
		while(token.hasMoreTokens())
			toReturn += " "+token.nextToken();
		return toReturn;
	}
}