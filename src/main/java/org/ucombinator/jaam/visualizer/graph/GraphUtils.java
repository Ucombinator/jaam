package org.ucombinator.jaam.visualizer.graph;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

/**
 * Created by jjbesavi on 6/28/17.
 */
public class GraphUtils {

    private class SCCVertex
    {
        SCCVertex(Integer id, Integer myIndex)
        {
            vertexId = id;
            index = myIndex;
            lowlink = -1;
        }

        public final Integer vertexId;
        public final Integer index; // Order in which the vertices were visited by the DFS
        public Integer lowlink; // minIndex of a vertex reachable from my subtree that is not already part of a SCC
    }

    private void visit(AbstractVertex v, HashMap<Integer, SCCVertex> visitedVertices, Stack<Integer> stack,
                       ArrayList< ArrayList<Integer> > components )
    {

        SCCVertex vSCC = new SCCVertex(v.getId(), visitedVertices.size());
        visitedVertices.put(vSCC.vertexId, vSCC);
        stack.push(vSCC.vertexId);
        vSCC.lowlink = vSCC.index;

        HashSet<AbstractVertex> neighbors = v.getOutgoingNeighbors();
        for(AbstractVertex n : neighbors)
        {
            if(!visitedVertices.containsKey(n.getId())) {
                visit(n, visitedVertices, stack, components);
                vSCC.lowlink = Math.min(vSCC.lowlink, visitedVertices.get(n.getId()).lowlink);
            }
            else if(stack.contains(n.getId())) // Should be fast because the stack is small
            {
                vSCC.lowlink = Math.min(vSCC.lowlink, visitedVertices.get(n.getId()).index);
            }
        }

        if(vSCC.lowlink < vSCC.index)
        {
            ArrayList<Integer> newComponent = new ArrayList<Integer>();
            while(true)
            {
                Integer w = stack.pop();
                newComponent.add(w);
                if(w == vSCC.vertexId)
                    break;
            }
            components.add(newComponent);
        }

    }

    public ArrayList< ArrayList<Integer> > StronglyConnectedComponents(final Graph g)
    {
        ArrayList< ArrayList<Integer> > components = new ArrayList<ArrayList<Integer>>();

        Stack<Integer> stack = new Stack<Integer>();
        HashMap<Integer, SCCVertex> visitedVertices = new HashMap<Integer, SCCVertex>();

        ArrayList<Vertex> vertices = g.getVertices();

        for(Vertex v : vertices)
        {
            if(!visitedVertices.containsKey(v.getId())) {
                visit(v, visitedVertices, stack, components);
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
