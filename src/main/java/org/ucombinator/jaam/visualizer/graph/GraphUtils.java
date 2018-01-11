package org.ucombinator.jaam.visualizer.graph;

import org.ucombinator.jaam.interpreter.State;
import org.ucombinator.jaam.visualizer.layout.*;

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

    private static <T extends AbstractLayoutVertex<T>>
    void visit(Graph<T> g, T v, HashMap<Integer, SCCVertex> visitedVertices, Stack<Integer> stack,
               ArrayList<ArrayList<Integer>> components )
    {

        SCCVertex vSCC = new SCCVertex(v.getId(), visitedVertices.size());
        visitedVertices.put(v.getId(), vSCC);
        stack.push(v.getId());
        vSCC.lowlink = vSCC.index;

        //System.out.println("TERE Visiting " + v.getId() + " == " + vSCC);

        HashSet<T> neighbors = g.getOutNeighbors(v);
        for (T n : neighbors) {
            if (n.getId() == v.getId()) { // No self loops
                continue;
            }
            //System.out.print("\tTERE Neighbor " + n.getId());
            if (!visitedVertices.containsKey(n.getId())) {
                //System.out.println(" Hadn't been visited");
                visit(g, n, visitedVertices, stack, components);
                vSCC.lowlink = Math.min(vSCC.lowlink, visitedVertices.get(n.getId()).lowlink);
            } else if (stack.contains(n.getId())) { // Should be fast because the stack is small
                //System.out.println(" Still On Stack" + stack );

                vSCC.lowlink = Math.min(vSCC.lowlink, visitedVertices.get(n.getId()).index);
            } else {
                //System.out.println(" Already processed and popped " + stack + visitedVertices);
            }
        }

        //System.out.println("TERE Finished Visiting " + v.getId() + " == " + vSCC);

        if (vSCC.lowlink == vSCC.index) {
            //System.out.println("\t\t\tTERE Found a leader " + vSCC);
            ArrayList<Integer> newComponent = new ArrayList<>();
            while (true) {
                int w = stack.pop();
                // System.out.println("\t\t\t\tTERE Popped " + w);
                newComponent.add(w);
                if(w == v.getId())
                    break;
            }
            components.add(newComponent);
        }
    }

    public static <T extends AbstractLayoutVertex<T>> ArrayList<ArrayList<Integer>> StronglyConnectedComponents(final Graph<T> g)
    {
        ArrayList<ArrayList<Integer>> components = new ArrayList<>();

        Stack<Integer> stack = new Stack<>();
        HashMap<Integer, SCCVertex> visitedVertices = new HashMap<>();

        ArrayList<T> vertices = g.getVertices();
        System.out.println("Vertices: " + vertices.size());

        for(T v : vertices) {
            if(stack.size() > 0) {
                System.out.println("JUAN FOUND A NON EMPTY STACK!");
            }

            if(!visitedVertices.containsKey(v.getId())) {
                visit(g, v, visitedVertices, stack, components);
            }
        }

        return components;
    }

    public static HashMap<String, ArrayList<StateVertex>> groupByClass(final Graph<StateVertex> graph) {
        HashMap<String, ArrayList<StateVertex>> visitedVertices = new HashMap<>();

        Stack<StateVertex> stack = new Stack<StateVertex>();
        for(StateVertex v : graph.getVertices()) {
            stack.add(v);
        }

        while(stack.size() > 0) {
            StateVertex v = stack.pop();
            if (v instanceof LayoutMethodVertex) {
                String className = ((LayoutMethodVertex) v).getClassName();
                addVertexToClassGroup(visitedVertices, className, v);
            } else if (v instanceof LayoutLoopVertex) {
                String className = ((LayoutLoopVertex) v).getClassName();
                addVertexToClassGroup(visitedVertices, className, v);
            } else {
                System.out.println("Error! Unhandled vertex type in GraphUtils.groupByClass.");
            }
        }

        return visitedVertices;
    }

    public static void addVertexToClassGroup(HashMap<String, ArrayList<StateVertex>> visitedVertices,
                                             String className, StateVertex vertex) {

        if(visitedVertices.containsKey(className)) {
            visitedVertices.get(className).add(vertex);
        }
        else {
            ArrayList<StateVertex> list = new ArrayList<>();
            list.add(vertex);
            visitedVertices.put(className, list);
        }
    }
}
