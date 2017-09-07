package org.ucombinator.jaam.visualizer.layout;

import java.util.*;

public class LayoutAlgorithm
{
    // This works on a graph whose vertices have been assigned a bounding box
    private static final double MARGIN_PADDING = 10;//.25;
    private static final double NODES_PADDING = 10;//.5;
    private static final double ROOT_V_OFFSET = 10;//2;
    private static HashMap<Integer, Double> bboxWidthTable;
    private static HashMap<Integer, Double> bboxHeightTable;

    public static void layout(AbstractLayoutVertex parentVertex) {
        bboxWidthTable = new LinkedHashMap<>();
        bboxHeightTable = new LinkedHashMap<>();
        initializeSizes(parentVertex);
        bfsLayout(parentVertex);
        //defaultLayout(parentVertex);
        parentVertex.setY(parentVertex.getY()+ROOT_V_OFFSET);
    }

    private static void initializeSizes(AbstractLayoutVertex parentVertex) {
        parentVertex.setWidth(AbstractLayoutVertex.DEFAULT_WIDTH);
        parentVertex.setHeight(AbstractLayoutVertex.DEFAULT_HEIGHT);
        for(AbstractLayoutVertex v:parentVertex.getInnerGraph().getVertices()){
            initializeSizes(v);
        }
    }

    private static void expandSubGraphs(AbstractLayoutVertex parentVertex)
    {
        for(AbstractLayoutVertex v: parentVertex.getInnerGraph().getVertices()){
            HierarchicalGraph inner_graph = v.getInnerGraph();
            if (inner_graph.getVertices().size() != 0)
            {
                //Layout the inner graphs of each node and assign width W and height H to each node
                //X and Y coordinates are RELATIVE to the parent
                if(v.isExpanded()){
                    defaultLayout(v);
                }
            }
        }
    }

    private static void defaultLayout(AbstractLayoutVertex parentVertex) {

        HierarchicalGraph graph = parentVertex.getInnerGraph();

        expandSubGraphs(parentVertex);

        /*******************************************************************************************/
        /*********************** ACTUAL LAYOUT FOR THE CURRENT LEVEL/GRAPH *************************/
        /*******************************************************************************************/
        for (AbstractLayoutVertex v : graph.getVertices()) {
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        }

        HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap = new HashMap<>();

        for (AbstractLayoutVertex v : graph.getVertices()) {
            childrenMap.put(v, new ArrayList<>());
            childrenMap.get(v).addAll(graph.getOutNeighbors(v));
        }

        doLayout(parentVertex, childrenMap);

    }

    private static void bfsLayout(AbstractLayoutVertex parentVertex)
    {
        HierarchicalGraph graph = parentVertex.getInnerGraph();

        // Interior graphs use the DFS Layout
        expandSubGraphs(parentVertex);

        /*******************************************************************************************/
        /*********************** ACTUAL LAYOUT FOR THE CURRENT LEVEL/GRAPH *************************/
        /*******************************************************************************************/
        // Initialize all the nodes to be WHITE
        for(AbstractLayoutVertex v: graph.getVertices()){
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        }

        AbstractLayoutVertex root = graph.getRoot();

        // Do the BFS Pass
        HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap = bfsChildren(graph, root);

        // Reset all the nodes to be WHITE
        for(AbstractLayoutVertex v: graph.getVertices()){
            if(v.getVertexStatus() != AbstractLayoutVertex.VertexStatus.BLACK)
            {
                System.out.println("ERROR in Max Depth Drawings. Does your graph ahve a cycle?");
                System.out.println("BFS ERROR Didn't process " + v.getId() + " in BFS Children Pass " + v.getVertexStatus());
            }
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        }

        doLayout(parentVertex, childrenMap);
    }

    private static void doLayout(AbstractLayoutVertex parentVertex,
                                 HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap)
    {
        HierarchicalGraph graph = parentVertex.getInnerGraph();

        AbstractLayoutVertex root = graph.getRoot();

        if(root != null) {
            storeBBoxWidthAndHeight(graph, root, childrenMap);
        }

        for(AbstractLayoutVertex v: graph.getVertices()){
            v.setVertexStatus(AbstractLayoutVertex.VertexStatus.WHITE);
        }

        if(root != null) {
            assignXandYtoInnerNodesAndGiveParentBBox(root, MARGIN_PADDING, MARGIN_PADDING, childrenMap);
            if(root.getInnerGraph().getVertices().size() > 1)
            {
                parentVertex.setWidth(bboxWidthTable.get(root.getId()) + 1000 * MARGIN_PADDING);
                parentVertex.setHeight(bboxHeightTable.get(root.getId()) + 2 * MARGIN_PADDING);
            }
            else
            {
                parentVertex.setWidth(bboxWidthTable.get(root.getId()) + 2 * MARGIN_PADDING);
                parentVertex.setHeight(bboxHeightTable.get(root.getId()) + 2 * MARGIN_PADDING);
            }
        } else {
            parentVertex.setWidth(AbstractLayoutVertex.DEFAULT_WIDTH);
            parentVertex.setHeight(AbstractLayoutVertex.DEFAULT_HEIGHT);
        }
    }
    

    /**
     * Preconditions: Height and Width of the inner nodes of the graph is (resursively known)
     * input: graph and left/top offset
     * Changes of Status: assigns X and Y to the inner vertices of the graph
     * Output: returns the W and H to be assign to the parent node
     * */
    private static HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> bfsChildren(HierarchicalGraph graph, AbstractLayoutVertex root)
    {
//        System.out.println("TERE: I think this should be called ONCE!");
        HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap = new HashMap<>();

        HashMap<AbstractLayoutVertex, Integer> vertexCounters = new HashMap<>();
        Queue<AbstractLayoutVertex> vertexQueue = new ArrayDeque<>();

        HashSet<AbstractLayoutVertex> seen = new HashSet<>();

        vertexQueue.add(root);
        seen.add(root);

        while(!vertexQueue.isEmpty())
        {
           AbstractLayoutVertex v = vertexQueue.remove();

           childrenMap.put(v, new ArrayList<>());

//           System.out.print("TERE \tVisiting " + v + " --> " + v.getId() + " is " + v.getVertexStatus() + " Method: ");
           /*
           for(LayoutMethodVertex m : v.getMethodVertices())
           {
               System.out.print(m.getMethodName() + " ");
           }
           System.out.println();
           */

           for(AbstractLayoutVertex child : graph.getOutNeighbors(v))
           {
//               System.out.println("\tTERE SEEING CHILD " + child + " --> " + child.getId() + " is " + child.getVertexStatus());

               if(child == v)
                   continue;
               if(!seen.contains(child))
               {
                   seen.add(child);
                   child.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
                   int numberOfIncomingEdges = graph.getInNeighbors(child).size();

//                   System.out.print("\t\tTERE: Child " + child + " has " + numberOfIncomingEdges);

                   numberOfIncomingEdges = numberOfIncomingEdges - 1; // v's incoming edge
                   if(graph.getInNeighbors(child).contains(child))   // Self loop
                       numberOfIncomingEdges = numberOfIncomingEdges - 1;

                   if(numberOfIncomingEdges > 0)
                   {
                       vertexCounters.put(child, numberOfIncomingEdges); // Discounting the current
                   }
                   else if(numberOfIncomingEdges == 0)
                   {
                        childrenMap.get(v).add(child);
                        vertexQueue.add(child);
                   }
                   else
                   {
                       // Should be an assert..
                       System.out.println("ERROR in BFS CHILDREN. Invalid graph, child has no incoming edges " + child + " -> " + child.getId() + " Status" + child.getVertexStatus() + " Incoming Neigh " + graph.getInNeighbors(child));
                   }
               }
               else if(child.getVertexStatus() == AbstractLayoutVertex.VertexStatus.GRAY)
               {
                   Integer numberOfIncomingEdges = vertexCounters.get(child);

                   if(numberOfIncomingEdges == null)
                   {
//                       System.out.println("TERE: Found a NULL " + child + " -> " + child.getId() + " Status" + child.getVertexStatus() + " Incoming Neigh " + graph.getInNeighbors(child));
                       System.out.println("Map\n\t " + vertexCounters);
                   }

//                   System.out.print("\t\tTERE: GRAY Child " + child + " has " + numberOfIncomingEdges);

                   numberOfIncomingEdges -= 1;

                   vertexCounters.put(child, numberOfIncomingEdges);

                   //System.out.println("TERE After Fixing " + numberOfIncomingEdges);

                   if(numberOfIncomingEdges == 0)
                   {
//                       System.out.print("\t\t\tTERE: Adding Child to queue " + child + " has " + numberOfIncomingEdges);
                       childrenMap.get(v).add(child);
                       vertexQueue.add(child);
                       vertexCounters.remove(child);
                   }
                   else if(numberOfIncomingEdges < 0)
                   {
                       System.out.println("ERROR in BFS CHILDREN. Seeing a grey child with <0 incoming edges " + child + " -> " + child.getId() + " Status" + child.getVertexStatus() + " Incoming Neigh " + graph.getInNeighbors(child));
                   }
               }
               else
               {
                   System.out.println("ERROR in BFS children. Seeing a Black Child " + child + " --> " + child.getId() + " " + graph.getInNeighbors(child).size());
               }
           }

//           System.out.println("TERE Finished " + v + " --> " + v.getId());
//           System.out.println("\t\tTERE queue " + vertexQueue);
//            System.out.println("\t\tTERE counters " + vertexCounters);

           v.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);
        }

        if(vertexCounters.size() > 0) {
            System.out.println("BFS uncounted vertices, what happened to the incoming?!!! " + vertexCounters);
            for (Map.Entry<AbstractLayoutVertex, Integer> entry : vertexCounters.entrySet()) {

                System.out.println("\t\t" + entry + " --> " + entry.getKey().getId() + " " + entry.getKey().getVertexStatus() + " " +  entry.getKey().getMethodVertices());
                for(AbstractLayoutVertex n : graph.getInNeighbors(entry.getKey()))
                {
                    System.out.println("\t\t\t" + n + " --> " + n.getId() + " " + n.getVertexStatus());
                }
            }
        }

        return childrenMap;
    }


    /**
     * Preconditions: Height and Width of the inner nodes of the graph is (resursively known)
     * input: graph and left/top offset
     * Changes of Status: assigns X and Y to the inner vertices of the graph
     * Output: returns the W and H to be assign to the parent node
     * */
    private static double[] storeBBoxWidthAndHeight(
            HierarchicalGraph graph, AbstractLayoutVertex root,
            HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex> > childrenMap)
    {
        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
        ArrayList<AbstractLayoutVertex> grayChildren = new ArrayList<AbstractLayoutVertex>();
        for(AbstractLayoutVertex child: childrenMap.get(root))
        {
            if (child.getVertexStatus() == AbstractLayoutVertex.VertexStatus.WHITE)
            {
                child.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
                grayChildren.add(child);
            }
        }

        double currentWidth = 0;
        double currentHeight = 0;
        for(AbstractLayoutVertex curVer: grayChildren)
        {
            double[] boundBox = storeBBoxWidthAndHeight(graph, curVer, childrenMap);
            currentWidth += boundBox[0] + NODES_PADDING;
            currentHeight = Math.max(currentHeight, boundBox[1]);
        }

        double currBboxWidth, currBboxHeight;
        currBboxWidth = Math.max(root.getWidth(), currentWidth - NODES_PADDING);
        if(grayChildren.size() == 0)
        {
            currBboxHeight = root.getHeight();
        }
        else
        {
            currBboxHeight = NODES_PADDING + root.getHeight() + currentHeight;
        }

        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);

        bboxWidthTable.put(root.getId(), currBboxWidth);
        bboxHeightTable.put(root.getId(), currBboxHeight);

        double[] result = {currBboxWidth, currBboxHeight};
        return result;
    }

    /**
     * Preconditions: Height and Width of the inner nodes of the graph is known (recursively)
     * Input: graph and left/top offset
     * Changes of Status: assigns X and Y to the inner vertices of the graph
     * Output: returns the W and H to be assigned to the parent node
     * */
    private static void assignXandYtoInnerNodesAndGiveParentBBox(
            AbstractLayoutVertex root, double left, double top,
            HashMap<AbstractLayoutVertex, ArrayList<AbstractLayoutVertex>> childrenMap)
    {
        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
        ArrayList<AbstractLayoutVertex> grayChildren = new ArrayList<>();
        for(AbstractLayoutVertex child: childrenMap.get(root))
        {
            if (child.getVertexStatus() == AbstractLayoutVertex.VertexStatus.WHITE)
            {
                child.setVertexStatus(AbstractLayoutVertex.VertexStatus.GRAY);
                grayChildren.add(child);
            }
        }

        double currentWidth = 0;
        double currentHeight = 0;

        for(AbstractLayoutVertex curVer: grayChildren)
        {
            currentWidth += bboxWidthTable.get(curVer.getId()) + NODES_PADDING;
        }

        // TODO: What is AX?
        double AX;
        if(root.getWidth() >= currentWidth - NODES_PADDING) {
            AX = (root.getWidth() - (currentWidth - NODES_PADDING))/2;
        } else {
            AX = 0;
        }

        currentWidth = 0;
        for(AbstractLayoutVertex curVer: grayChildren)
        {
            assignXandYtoInnerNodesAndGiveParentBBox(curVer,currentWidth + left + AX,NODES_PADDING + top + root.getHeight(), childrenMap);
            currentWidth += bboxWidthTable.get(curVer.getId()) + NODES_PADDING;
            currentHeight = Math.max(currentHeight, bboxHeightTable.get(curVer.getId()));
        }

        root.setX(left + ((bboxWidthTable.get(root.getId()) - root.getWidth()) / 2.0));  //left-most corner x
        root.setY(top);                                                    //top-most corner y
        root.setVertexStatus(AbstractLayoutVertex.VertexStatus.BLACK);
    }

}


