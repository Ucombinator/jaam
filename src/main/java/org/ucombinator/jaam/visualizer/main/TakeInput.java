package org.ucombinator.jaam.visualizer.main;

import java.io.*;

import org.ucombinator.jaam.serializer.*;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.Instruction;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;

public class TakeInput extends Thread
{
	public Graph parseStateGraph(String file)
	{
		Graph graph = new Graph();
		if(file.equals("")) {
			//readSmallDummyGraph(graph);
			readLargeDummyGraph(graph);
		}
		else try
		{
			PacketInput packetInput = new PacketInput(new FileInputStream(file));
			Packet packet = packetInput.read();
			int loop_counter = 0;

			while(!(packet instanceof EOF))
			{
				//Name collision with our own Edge class
				if(packet instanceof org.ucombinator.jaam.serializer.Edge)
				{
					org.ucombinator.jaam.serializer.Edge edgePacket = (org.ucombinator.jaam.serializer.Edge) packet;
					int edgeId = edgePacket.id().id();
					int srcId = edgePacket.src().id();
					int destId = edgePacket.dst().id();
					graph.addEdge(srcId, destId);
				}
				else if(packet instanceof ErrorState)
				{
					// TODO: Add description for ErrorState on initialization
					int id = ((ErrorState) packet).id().id();
					graph.addErrorState(id);
				}
				//Name collision with java.lang.Thread.State
				else if(packet instanceof org.ucombinator.jaam.serializer.State)
				{
					org.ucombinator.jaam.serializer.State statePacket = (org.ucombinator.jaam.serializer.State) packet;
					int id = statePacket.id().id();
					String methodName = statePacket.stmt().method().toString();
					String instruction = statePacket.stmt().stmt().toString();
					int jimpleIndex = statePacket.stmt().index();
					Instruction inst = new Instruction(instruction, methodName, jimpleIndex, true);
					graph.addVertex(id, inst, true);
				}
                
                else if(packet instanceof org.ucombinator.jaam.serializer.NodeTag)
                {
                    org.ucombinator.jaam.serializer.NodeTag tag = (org.ucombinator.jaam.serializer.NodeTag) packet;
                    
                    int tagId = tag.id().id();
                    int nodeId = tag.node().id();
                    String tagStr = ((org.ucombinator.jaam.serializer.Tag)tag.tag()).toString();
                    graph.addTag(nodeId,tagStr);
                }

                packet = packetInput.read();
			}
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}

		return graph;
	}

	public Graph parseLoopGraph(String file) {
		Graph graph = new Graph();

		if(file.equals("")) {
			//readSmallDummyGraph(graph);
			//readLargeDummyGraph(graph);
		}
		else try
		{
			PacketInput packetInput = new PacketInput(new FileInputStream(file));
			Packet packet = packetInput.read();
			int loop_counter = 0;

			while(!(packet instanceof EOF))
			{
				if(packet instanceof LoopLoopNode) {
					LoopLoopNode node = (LoopLoopNode) packet;
					int id = node.id().id();
					String label = node.method().getSignature() + "\ninstruction #" + node.statementIndex();
					graph.addVertex(id, new Instruction(label, "Loop:"+node.method().getSignature() + ":" +
							loop_counter++, id, false), true);
				}
				else if(packet instanceof LoopMethodNode) {
					LoopMethodNode node = (LoopMethodNode) packet;
					int id = node.id().id();
					String label = node.method().getSignature();
					graph.addVertex(id, new Instruction(label, label, id, false), true);
				}
				else if(packet instanceof LoopEdge) {
					LoopEdge edge = (LoopEdge) packet;
					int src = edge.src().id();
					int dest = edge.dst().id();
					graph.addEdge(src, dest);
				}
				else if (packet instanceof org.ucombinator.jaam.tools.decompile.DecompiledClass)
				{
					CompilationUnit unit = ((org.ucombinator.jaam.tools.decompile.DecompiledClass) packet).compilationUnit();
					String className = getClassName(unit);
					graph.addClass(className, unit.getText());
				}

                packet = packetInput.read();
			}
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}

		return graph;
	}

	private static String getClassName(CompilationUnit unit) {
		return "";
	}

	public static void readSmallDummyGraph(Graph graph) {
		int dummyInstructions = 6;
		for (int i = 0; i < dummyInstructions; i++) {
			if (i < 3) {
				Instruction inst = new Instruction("i" + Integer.toString(i) + " = " + Integer.toString(i),
						"<Main: main>", i, true);
				graph.addVertex(i, inst, true);
			} else {
				Instruction inst = new Instruction("i" + Integer.toString(i) + " = " + Integer.toString(i),
						"<Main: func>", i, true);
				graph.addVertex(i, inst, true);
			}
		}

		graph.addEdge(0, 1);
		graph.addEdge(1, 2);
		graph.addEdge(2, 3);
		graph.addEdge(3, 4);
		graph.addEdge(4, 5);
	}

	public static void readLargeDummyGraph(Graph graph) {
		int dummyInstructions = 16;
		for(int i = 0; i < dummyInstructions; i++) {
			if(i < 5) {
				Instruction inst = new Instruction("i" + Integer.toString(i) + " = " + Integer.toString(i),
						"<Main: main>", i, true);
				graph.addVertex(i, inst, true);
			}
			else {
				Instruction inst = new Instruction("i" + Integer.toString(i) + " = " + Integer.toString(i),
						"<Main: func>", i, true);
				graph.addVertex(i, inst, true);
			}
		}

		// Main.main
		graph.addEdge(0, 1);
		graph.addEdge(1, 2);
		graph.addEdge(1, 3);
		graph.addEdge(2, 4);
		graph.addEdge(3, 4);

		// Main.func
		graph.addEdge(4, 5);
		graph.addEdge(5, 6);
		graph.addEdge(6, 7);
		graph.addEdge(6, 8);
		graph.addEdge(7, 9);
		graph.addEdge(8, 9);
		graph.addEdge(9, 10);
		graph.addEdge(10, 11);
		graph.addEdge(11, 12);
		graph.addEdge(12,13);
		graph.addEdge(13,14);
		graph.addEdge(14,15);
	}
}
