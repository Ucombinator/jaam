
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

	public void run(boolean fromCFG)
	{
		StacViz.graph = new Graph();
		
		if(fromCFG)
		{
			this.parseCFGInput();
		}
		else
		{
			this.parseDefault();
		}
		
		//StacViz.graph.addLabels();
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
		
		System.out.println("number of vertices = "+StacViz.graph.vertices.size());
		System.out.println("number of method vertices = "+StacViz.graph.methodVertices.size());
	}
	
	public void setSystemInput()
	{
		this.input = new BufferedReader(new InputStreamReader(System.in));
	}
	
	public void setFileInput(String file)
	{
		try
		{
//			System.out.println(file);
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
		int id=-1, start=0, end=0;
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
		//System.out.println("Adding new vertex: ");
		//System.out.println(description);
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
	
/*	
	public void defaultAddVertextest(int id, String descrip)
	{
//		System.out.println(descrip);
		StringTokenizer token = new StringTokenizer(descrip,"\n");
		String desc = "", temp1="", temp2="";
		token.nextToken();
		token.nextToken();
		token.nextToken();
		token.nextToken();
		temp1 = token.nextToken();
		while(token.hasMoreTokens())
		{
			temp2 = token.nextToken();
			if(temp2.trim().equalsIgnoreCase("\"fp\":{"))
			{
				System.out.println(temp1+","+temp2);
				break;
			}
			desc = desc+temp1;
			temp1 = temp2;
		}
		desc = desc.trim()+" ";
		System.out.println(desc);
		Pattern stmtPattern = Pattern.compile(
				"\"sootStmt\":\"(.*)\",\\s*"
				+ "\"sootMethod\":\\{\\s*"
				+ "\"\\$type\":\"soot.SootMethod\",\\s*"
				+ "\"declaringClass\":\"(.*)\",\\s*"
				+ "\"name\":\"(.*)\",\\s*"
				+ "\"parameterTypes\":\\[\\s*(.*)\\s*\\],\\s*"
				+ "\"returnType\":\"(.*)\",\\s*"
				+ "\"modifiers\":(\\d+),\\s*"
				+ "\"exceptions\":\\[\\s*(.*)\\s*\\]\\s*\\},\\s*"
				+ "\"index\":(-?\\d+),\\s*"
				+ "\"line\":(-?\\d+),\\s*"
				+ "\"column\":(-?\\d+),\\s*"
				+ "\"sourceFile\":\"(.*)\"\\s*");
		
		Matcher stmtMatcher = stmtPattern.matcher(desc);
		if(stmtMatcher.find())
		{
//			System.out.println(stmtMatcher.group(0));
			String inst = this.stmtMatcherToInst(stmtMatcher);
			String method = stmtMatcherToMethod(stmtMatcher);

			StacViz.graph.addVertex(id, method, inst, desc);
		}
		else
		{
			System.out.println("something is wrong");
			JOptionPane.showMessageDialog(null, desc);
		}

//		System.out.println(description);

//		JOptionPane.showMessageDialog(null, desc);

	}
	
	
//*/
	
	public void letItSleep()
	{
		if(Parameters.willSleep)
		{
			try
			{
				Thread.sleep(Parameters.ThreadSleep);
			}
			catch (InterruptedException ie)
			{
				System.out.println(ie);
			}
		}
	}
	
	public void parseCFGInput()
	{
		String line;
		Pattern startPattern = Pattern.compile("\"\\$type\":\"org.ucombinator.jaam.Stmt\",");
		Pattern targetFinishedPattern = Pattern.compile("\\s+],$");
		Pattern edgesFinishedPattern = Pattern.compile("\\s+]$");
		
		try
		{
			//Key is the method name, value is the number of statements in the given method.
			//We will use this after we finish parsing to assign absolute indices to every statement.
			//We use a linked hash map so that we know in which order we examined each method.
			LinkedHashMap<String, Integer> methodLengths = new LinkedHashMap<String, Integer>();
			HashMap<String, Integer> absoluteMethodIndex = new HashMap<String, Integer>();

			//Store the edges as a list of pairs of (function name + integer), since we won't know the actual index of each endpoint until we're done.
			ArrayList<String> verDesc = new ArrayList<String>();
			ArrayList<String> verInst = new ArrayList<String>();
			ArrayList<String> verFunction = new ArrayList<String>();
			ArrayList<Integer> verIndex = new ArrayList<Integer>();

			ArrayList<String> edgeFunction1 = new ArrayList<String>();
			ArrayList<Integer> edgeIndex1 = new ArrayList<Integer>();
			ArrayList<String> edgeFunction2 = new ArrayList<String>();
			ArrayList<Integer> edgeIndex2 = new ArrayList<Integer>();
			
			String method1 = "", method2 = "";
			String desc = "", inst = "";
			int index1 = -1, index2 = -1;
			
			line = input.readLine();
			while(line != null)
			{
				//Look for the beginning "$type":"org.ucombinator.jamm.Stmt"
				Matcher startMatcher = startPattern.matcher(line);
				if(startMatcher.find())
				{
					//The next several lines will contain a statement, the method that contains it, and its relative position inside that method.
					Matcher stmtMatcher1 = findStatement();
					desc = stmtMatcher1.group(0);
					inst = this.stmtMatcherToInst(stmtMatcher1);
					method1 = stmtMatcherToMethod(stmtMatcher1);
					index1 = stmtMatcherToIndex(stmtMatcher1);
					
					verFunction.add(method1);
					verDesc.add(desc);
					verInst.add(inst);
					verIndex.add(index1);
					
					System.out.println("processing node ... "+inst);

					
					//Update the length of the method
					if(!methodLengths.containsKey(method1) || index1 >= methodLengths.get(method1))
					{
						methodLengths.put(method1, index1 + 1); //Indices are 0-indexed, so we add 1 to the maximum index to get the length
					}
					
					//Next we have a series of targets, and a series of successors.
					//The end of both sets of edges will be marked by a single ], followed by whitespace.
					while(!line.trim().equalsIgnoreCase("\"targets\":["))
						line = input.readLine();
					line = input.readLine();
					Matcher edgeFinishedMatcher = targetFinishedPattern.matcher(line);
					if(!line.trim().equalsIgnoreCase(""))
					{
						while(!edgeFinishedMatcher.find())
						{
							Matcher startMatcher2 = startPattern.matcher(line);
							if(startMatcher2.find())
							{
								Matcher stmtMatcher2 = findStatement();
								method2 = stmtMatcherToMethod(stmtMatcher2);
								index2 = stmtMatcherToIndex(stmtMatcher2);
								
//								System.out.println(""+stmtMatcher2.group(0));
//								System.out.println("storing edges from "+inst);
//								System.out.println("                to "+this.stmtMatcherToInst(stmtMatcher2));
								
								//Add edge to list of edges
								edgeFunction1.add(method1);
								edgeIndex1.add(index1);
								edgeFunction2.add(method2);
								edgeIndex2.add(index2);
							}
							
							line = input.readLine();
							edgeFinishedMatcher = targetFinishedPattern.matcher(line);
						}
					}
					else
					{
						line = input.readLine();
					}
					//System.out.println("instr="+inst);
					//System.out.println(line);
					while(!line.trim().equalsIgnoreCase("\"successors\":["))
						line = input.readLine();
					edgeFinishedMatcher = edgesFinishedPattern.matcher(line);
					if(!line.trim().equalsIgnoreCase(""))
					{
						while(!edgeFinishedMatcher.find())
						{
							Matcher startMatcher2 = startPattern.matcher(line);
							if(startMatcher2.find())
							{
								Matcher stmtMatcher2 = findStatement();
								method2 = stmtMatcherToMethod(stmtMatcher2);
								index2 = stmtMatcherToIndex(stmtMatcher2);
								
								//Add edge to list of edges
								edgeFunction1.add(method1);
								edgeIndex1.add(index1);
								edgeFunction2.add(method2);
								edgeIndex2.add(index2);
							}
							
							line = input.readLine();
							edgeFinishedMatcher = edgesFinishedPattern.matcher(line);
						}
					}
				}
				
				line = input.readLine();
			}
			
			//Iterate through the methods in order and assign indices
			int currIndex = 0;
			for(String methodName : methodLengths.keySet())
			{
				absoluteMethodIndex.put(methodName, currIndex);
				currIndex += methodLengths.get(methodName);
			}
			
			//Now the absolute index for each vertex is its method index plus its index in that method
			for(int i=0; i<verDesc.size(); i++)
			{
				desc = verDesc.get(i);
				inst = verInst.get(i);
				method1 = verFunction.get(i);
				index1 = absoluteMethodIndex.get(method1) + verIndex.get(i);
				StacViz.graph.addVertex(index1, method1, inst, desc);

			}
			
			//Instead of adding the edges in any order, we sort them by their source vertex first.
			ArrayList<Edge> edgesToAdd = new ArrayList<Edge>();
			for(int i = 0; i < edgeFunction1.size(); i++)
			{
				method1 = edgeFunction1.get(i);
				index1 = edgeIndex1.get(i);
				method2 = edgeFunction2.get(i);
				index2 = edgeIndex2.get(i);
				
				if(absoluteMethodIndex.containsKey(method1) && absoluteMethodIndex.containsKey(method2))
				{
					int absIndex1 = absoluteMethodIndex.get(method1) + index1;
					int absIndex2 = absoluteMethodIndex.get(method2) + index2;
					edgesToAdd.add(new Edge(absIndex1, absIndex2));
				}
				else
				{
					System.out.println("Pair of methods not found: " + method1 + ", " + method2);
				}
			}
			
			Collections.sort(edgesToAdd);
			for(Edge e: edgesToAdd)
			{
				if(Parameters.debug)
					System.out.println("Adding edge: " + e.source + ", " + e.dest);
				//System.out.println("desc pairs: " + desc1 + "\n            " + desc2);
				StacViz.graph.addEdge(e.source + " , " + e.dest);
				Parameters.repaintAll();
			}
		}
		catch(IOException e)
		{
			System.out.println(e);
		}
	}
	
	public Matcher findStatement() throws IOException
	{
		Pattern stmtPattern = Pattern.compile(
				"\\s*\"sootStmt\":\"(.*)\",\n\\s*"
				+ "\"sootMethod\":\\{\n\\s*"
				+ "\"\\$type\":\"soot.SootMethod\",\n\\s*"
				+ "\"declaringClass\":\"(.*)\",\n\\s*"
				+ "\"name\":\"(.*)\",\n\\s*"
				+ "\"parameterTypes\":\\[\\s*([[^,]*,\\s*)]*.*)\\s*\\],\n\\s*"
				+ "\"returnType\":\"(.*)\",\n\\s*"
				+ "\"modifiers\":(\\d+),\n\\s*"
				+ "\"exceptions\":\\[\\s*(.*)\\s*\\]\\s*\\},\\s*"
				+ "\"index\":(-?\\d+),\n\\s*"
				+ "\"line\":(-?\\d+),\n\\s*"
				+ "\"column\":(-?\\d+),\n\\s*"
				+ "\"sourceFile\":\"(.*)\"");			
		
		String line = input.readLine();
		Matcher stmtMatcher = stmtPattern.matcher(line);
		while(!stmtMatcher.find())
		{
			line += "\n"+input.readLine();
			stmtMatcher = stmtPattern.matcher(line);
		}
		
		return stmtMatcher;
	}
	
	public String stmtMatcherToDesc(Matcher stmtMatcher)
	{
		//The function description is stored as: return_type class.name(parameters)
		//See regex pattern in findStatement()
		String str = "\"Stmt\":"+this.stmtMatcherToInst(stmtMatcher);
		str = str+"\n\"Method\":"+this.stmtMatcherToMethod(stmtMatcher);
		str = str+"\n\"index\":"+this.stmtMatcherToIndex(stmtMatcher);
		str = str+"\n\"line\":"+Integer.parseInt(stmtMatcher.group(9));
		str = str+"\n\"column\":"+Integer.parseInt(stmtMatcher.group(10));
		str = str+"\n\"sourceFile\":"+stmtMatcher.group(11);
		return str;
	}
	
	public String stmtMatcherToInst(Matcher stmtMatcher)
	{
		return stmtMatcher.group(1);
	}

	public String stmtMatcherToMethod(Matcher stmtMatcher)
	{
		//The function description is stored as: return_type class.name(parameters)
		//See regex pattern in findStatement()
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
	
	public int stmtMatcherToIndex(Matcher stmtMatcher)
	{
		//See regex pattern in findStatement()
		return Integer.parseInt(stmtMatcher.group(8));
	}
}