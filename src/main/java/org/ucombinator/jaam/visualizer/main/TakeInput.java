package org.ucombinator.jaam.visualizer.main;

import java.io.*;
import java.util.ArrayList;

import org.ucombinator.jaam.serializer.*;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.graph.Instruction;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;

public class TakeInput extends Thread
{

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
            ArrayList<int[]> edges =  new ArrayList<int[]>();   
            
            while(!(packet instanceof EOF))
            {
                if(packet instanceof LoopLoopNode) {
                    LoopLoopNode node = (LoopLoopNode) packet;
                    graph.addVertex(new LayoutLoopNode(node.id().id(),
                    								   node.method().getSignature(),
                    								   node.statementIndex()
                    				));
                }
                else if(packet instanceof LoopMethodNode) {
                    LoopMethodNode node = (LoopMethodNode) packet;
                    graph.addVertex(new LayoutMethodNode(node.id().id(),
                    									node.method().getSignature()
                    									));
                }
                else if(packet instanceof LoopEdge) {
                    LoopEdge edge = (LoopEdge) packet;
                    int src = edge.src().id();
                    int dest = edge.dst().id();
                    //We store the pairs here
                    edges.add(new int[] {src,dest});
                }
//                else if (packet instanceof org.ucombinator.jaam.tools.decompile.DecompiledClass)
//                {
//                    CompilationUnit unit = ((org.ucombinator.jaam.tools.decompile.DecompiledClass) packet).compilationUnit();
//                    String className = getClassName(unit);
//                    graph.addClass(className, unit.getText());
//                }

                System.out.println("Reading new packet...");
                packet = packetInput.read();
                System.out.println("Packet read!");
            }

            packetInput.close();
            
            
            // We actually create the edges here
            for (int i=0; i<edges.size(); i++)
            {
            	graph.addEdge(edges.get(i)[0],edges.get(i)[1]);
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
