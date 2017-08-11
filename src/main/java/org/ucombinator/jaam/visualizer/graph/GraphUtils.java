package org.ucombinator.jaam.visualizer.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

public class GraphUtils {

    private static class SCCVertex
    {
        SCCVertex(Integer id, Integer myIndex)
        {
            vertexId = id;
            index = myIndex;
            lowlink = -1;
        }

        @Override
        public String toString()
        {
                return "{"+ vertexId + ":" + index + ":" + lowlink + "}";
        }

        public final int vertexId;
        public final int index; // Order in which the vertices were visited by the DFS
        public int lowlink; // minIndex of a vertex reachable from my subtree that is not already part of a SCC
    }

    private static void visit(Graph g, AbstractVertex v, HashMap<Integer, SCCVertex> visitedVertices, Stack<Integer> stack,
                       ArrayList<ArrayList<Integer>> components )
    {

        SCCVertex vSCC = new SCCVertex(v.getId(), visitedVertices.size());
        visitedVertices.put(v.getId(), vSCC);
        stack.push(v.getId());
        vSCC.lowlink = vSCC.index;

        //System.out.println("TERE Visiting " + v.getId() + " == " + vSCC);

        HashSet<AbstractVertex> neighbors = g.getOutNeighbors(v);
        for(AbstractVertex n : neighbors)
        {
            if(n.getId() == v.getId()) // No self loops
                continue;
            //System.out.print("\tTERE Neighbor " + n.getId());
            if(!visitedVertices.containsKey(n.getId())) {
                //System.out.println(" Hadn't been visited");
                visit(g, n, visitedVertices, stack, components);
                vSCC.lowlink = Math.min(vSCC.lowlink, visitedVertices.get(n.getId()).lowlink);
            }
            else if(stack.contains(n.getId())) // Should be fast because the stack is small
            {
                //System.out.println(" Still On Stack" + stack );

                vSCC.lowlink = Math.min(vSCC.lowlink, visitedVertices.get(n.getId()).index);
            }
            else {
                //System.out.println(" Already processed and popped " + stack + visitedVertices);
            }
        }

        //System.out.println("TERE Finished Visiting " + v.getId() + " == " + vSCC);

        if(vSCC.lowlink == vSCC.index)
        {
            //System.out.println("\t\t\tTERE Found a leader " + vSCC);
            ArrayList<Integer> newComponent = new ArrayList<Integer>();
            while(true)
            {
                int w = stack.pop();
                //System.out.println("\t\t\t\tTERE Popped " + w);
                newComponent.add(w);
                if(w == v.getId())
                    break;
            }
            components.add(newComponent);
        }
        else
        {
            //System.out.println("\t\t\t TERE Didn't find a leader! " + vSCC);
        }

    }

    public static ArrayList<ArrayList<Integer>> StronglyConnectedComponents(final Graph g)
    {
        ArrayList<ArrayList<Integer>> components = new ArrayList<ArrayList<Integer>>();

        Stack<Integer> stack = new Stack<Integer>();
        HashMap<Integer, SCCVertex> visitedVertices = new HashMap<Integer, SCCVertex>();

        ArrayList<AbstractVertex<AbstractVertex>> vertices = g.getVertices();

        for(AbstractVertex<AbstractVertex> v : vertices)
        {
            if(stack.size() > 0)
            {
                System.out.println("JUAN FOUND A NON EMPTY STACK!");
            }
            if(!visitedVertices.containsKey(v.getId())) {
                visit(g, v, visitedVertices, stack, components);
            }
        }

        return components;
    }

    public static void firstTest(Graph g)
    {
        g.addEdge(26,25);
        g.addEdge(25,8);
        g.addEdge(8,27);
    }

}
