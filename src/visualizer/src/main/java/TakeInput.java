
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.regex.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;


public class TakeInput extends Thread//implements Runnable
{
	BufferedReader input;

	public void run()
	{
		StacViz.graph = new Graph();
		this.parseDefault();
		
		StacViz.graph.finalizeParentsForRootChildren();
		StacViz.graph.identifyLoops();
		StacViz.graph.calcLoopHeights();
		StacViz.graph.mergeAllByMethod();
		StacViz.graph.mergePaths();
		StacViz.graph.computeInstLists();
		StacViz.graph.setAllMethodHeight();
		StacViz.graph.collapseAll();
		Parameters.mouseLastTime = System.currentTimeMillis();
		Parameters.repaintAll();
		
		System.out.println("number of vertices = " + StacViz.graph.vertices.size());
		System.out.println("number of method vertices = " + StacViz.graph.methodVertices.size());
	}
	
	public void setSystemInput()
	{
		this.input = new BufferedReader(new InputStreamReader(System.in));
	}
	
	public void setFileInput(String file)
	{
		try
		{
			this.input = new BufferedReader(new FileReader(file));
			
			if(this.input == null)
				System.out.println("null file");
			
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void parseDefault()
	{
		String line;
		int id = -1, start = 0, end = 0;
		String desc = "", tempStr = "";
		Pattern vertexPattern = Pattern.compile("(\"(\\d+)\":)");
		
		Parameters.cut_off = false;
		Parameters.startTime = System.currentTimeMillis();
		Parameters.lastInterval = -1;
		
		try
		{
			line = input.readLine();
			while(!Parameters.cut_off)
			{
				if(StacViz.graph.vertices.size()>=Parameters.limitV)
				{
					Parameters.cut_off = true;
					break;
				}
				if(line==null||line.length()==0)
				{
					line = input.readLine();
					continue;
				}
				if(line.equals("Done!"))
				{
					Parameters.console("Done!!");
					Parameters.cut_off = true;
					break;
				}
				
				
				if(line.contains("\"states\":{"))
				{
					tempStr = "";
					
					while(true)
					{
						line = input.readLine();						
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
							desc = tempStr.substring(start, end-2);
							desc = desc.substring(0, desc.lastIndexOf("\n")-1);
							this.defaultAddVertex(id, desc);
							id = Integer.parseInt(vertexMatcher.group(2));
							start = vertexMatcher.end(1);
						}
						tempStr = tempStr.substring(0, tempStr.length()-1);
						tempStr = tempStr.substring(0, tempStr.lastIndexOf("\n"));
						desc = tempStr.substring(start);
						this.defaultAddVertex(id, desc);
						if((System.currentTimeMillis()-Parameters.startTime)/Parameters.interval>Parameters.lastInterval)
						{
							System.out.println("number of vertices up to now = "+StacViz.graph.vertices.size());
							Parameters.lastInterval = (System.currentTimeMillis()-Parameters.startTime)/Parameters.interval;
						}
					}
					
					
					while(true)
					{
						line = input.readLine();
						if(line.trim().equalsIgnoreCase(""))
							break;
						
						line = input.readLine();
						
						tempStr = line.trim();
						tempStr = tempStr.substring(0, tempStr.length()-1);
						tempStr = tempStr + " -> ";
						
						line = input.readLine();
						
						tempStr = tempStr + line.trim();
						StacViz.graph.addEdge(tempStr);
						if((System.currentTimeMillis()-Parameters.startTime)/Parameters.interval>Parameters.lastInterval)
						{
							System.out.println("number of vertices up to now = "+StacViz.graph.vertices.size());
							Parameters.repaintAll();
							Parameters.lastInterval = (System.currentTimeMillis()-Parameters.startTime)/Parameters.interval;
						}
						
						line = input.readLine();
						
						if(!line.contains(","))
							break;
					}
				}

				line = input.readLine();
			}
			
			this.input.close();
		}
		catch(IOException e)
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
				int ln = Integer.parseInt(stmtMatcher.group(9));

				StacViz.graph.addVertex(id, method, inst, description, ind, ln, true);
			}
			else
			{
				System.out.println("Cannot parse vertex");
				System.out.println(description);
			}
		}
		else if(desc.indexOf("org.ucombinator.jaam.ErrorState$") >= 0)
		{
			StacViz.graph.addVertex(id, "ErrorState", "", "ErrorState", -1, -1, false);
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