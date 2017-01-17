
import java.io.*;
import java.util.ArrayList;
import java.util.Stack;
import org.ucombinator.jaam.serializer.*;

import javafx.application.Platform;

public class TakeInput extends Thread
{
	BufferedReader parseInput;
	PacketInput packetInput;

	public void run(String file, boolean fromPackets)
	{
		Main.graph = new Graph();
		this.parsePackets(file);

		Main.graph.finalizeParentsForRootChildren();
		Main.graph.mergeAllByMethod();
		Main.graph.computeInstLists();
		Main.graph.collectAllTags();
		Main.graph.identifyLoops();
		Main.graph.calcLoopHeights();

		// Run these panels on JavaFX thread instead of Swing thread
		Platform.runLater(new Runnable() {
			@Override
			public void run()
			{
				Parameters.stFrame.mainPanel.initFX(null);
			}
		});

		Parameters.mouseLastTime = System.currentTimeMillis();

		System.out.println("number of vertices = " + Main.graph.vertices.size());
		System.out.println("number of method vertices = " + Main.graph.methodVertices.size());
		System.out.println("number of classes = " + Main.graph.classes.size());
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

	public void parsePackets(String file)
	{
		if(file.equals(""))
			packetInput = new PacketInput(System.in);
		try
		{
			packetInput = new PacketInput(new FileInputStream(file));
			Packet packet = packetInput.read();

			while(!(packet instanceof EOF))
			{
				//Name collision with our own Edge class
				if(packet instanceof org.ucombinator.jaam.serializer.Edge)
				{
					org.ucombinator.jaam.serializer.Edge edgePacket = (org.ucombinator.jaam.serializer.Edge) packet;
					int edgeId = edgePacket.id().id();
					int srcId = edgePacket.src().id();
					int destId = edgePacket.dst().id();
					Main.graph.addEdge(srcId, destId);
				}
				else if(packet instanceof ErrorState)
				{
					int id = ((ErrorState) packet).id().id();
					Main.graph.addErrorState(id);
				}
				//Name collision with java.lang.Thread.State
				else if(packet instanceof org.ucombinator.jaam.serializer.State)
				{
					org.ucombinator.jaam.serializer.State statePacket = (org.ucombinator.jaam.serializer.State) packet;
					int id = statePacket.id().id();
					String methodName = statePacket.stmt().method().toString();
					String instruction = statePacket.stmt().stmt().toString();
					int jimpleIndex = statePacket.stmt().index();
					Main.graph.addVertex(id, methodName, instruction, "", jimpleIndex, true);
				}
                
                else if(packet instanceof org.ucombinator.jaam.serializer.NodeTag)
                {
                    org.ucombinator.jaam.serializer.NodeTag tag = (org.ucombinator.jaam.serializer.NodeTag) packet;
                    
                    int tagId = tag.id().id();
                    int nodeId = tag.node().id();
                    String tagStr = ((org.ucombinator.jaam.serializer.Tag)tag.tag()).toString();
                    Main.graph.addTag(nodeId,tagStr);
                }

                packet = packetInput.read();
			}
		}
		catch(FileNotFoundException e)
		{
			System.out.println(e);
		}
	}

	public static void loadDecompiledCode()
	{
		if(Main.graph != null)
		{
			File file = Parameters.openFile(true);
			if(file.isDirectory())
			{
				ArrayList<File> javaFiles = getJavaFilesRec(file);
				Main.graph.matchClassesToCode(file.getAbsolutePath() + "/", javaFiles);
			}
			else if(file.getAbsolutePath().endsWith(".java"))
			{
				//For now, we assume that there is only one class, because otherwise the user
				//would load a directory.
				if(Main.graph.classes.size() == 1)
				{
					Class ourClass = Main.graph.classes.entrySet().iterator().next().getValue();
					ourClass.parseJavaFile(file.getAbsolutePath());
				}
				else
					System.out.println("Cannot load single class. Number of classes: " + Main.graph.classes.size());
			}
		}
		else
		{
			System.out.println("Cannot load source code until we have a graph...");
		}
	}

	public static ArrayList<File> getJavaFilesRec(File file)
	{
		ArrayList<File> javaFiles = new ArrayList<File>();
		Stack<File> toSearch = new Stack<File>();
		toSearch.add(file);

		while (!toSearch.isEmpty())
		{
			File nextFilepath = toSearch.pop();
			if (nextFilepath.isFile() && nextFilepath.toString().endsWith(".java"))
			{
				//Add this .java file
				javaFiles.add(nextFilepath);
			}
			else if (nextFilepath.isDirectory())
			{
				//Search directory for more .java files
				File[] newFilepaths = nextFilepath.listFiles();

				//Assume we actually have a tree of directories, with no extra links
				for (File f : newFilepaths)
					toSearch.add(f);
			}
		}

		return javaFiles;
	}
}
