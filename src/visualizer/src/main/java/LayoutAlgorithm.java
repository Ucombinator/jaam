
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class LayoutAlgorithm
{
	// This works on a graph whose vertices have been assigned a bounding box
	final static double MARGIN_PADDING = .25;
	final static double NODES_PADDING = .5;
	
	private static AbstractVertex getVertexWithID(String id){
		Iterator<AbstractVertex> it = Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().getVertices().values().iterator();
		while(it.hasNext()){
			AbstractVertex v = it.next();
			if(v.getStrID().equals(id)){
				System.out.println(":-D '"+v.getStrID()+"'");
				return v;
			}
		}
		return null;
	}
	
	public static void layout(AbstractVertex parenteVertex){
	
//		getVertexWithID("vertex:20").setExpanded(false);
//		getVertexWithID("vertex:21").setExpanded(false);
//		getVertexWithID("vertex:22").setExpanded(false);
		
		initializeSizes(parenteVertex);
		defaultLayout(parenteVertex,parenteVertex.getInnerGraph());
		
		System.out.println("AHAHAH");
		Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().printCoordinates();
	}
	
	private static void initializeSizes(AbstractVertex parenteVertex){
		parenteVertex.setWidth(AbstractVertex.DEFAULT_WIDTH);
		parenteVertex.setHeight(AbstractVertex.DEFAULT_HEIGHT);
		Iterator<AbstractVertex> it = parenteVertex.getInnerGraph().getVertices().values().iterator();
		while(it.hasNext()){
			initializeSizes(it.next());
		}
	}
	
	public static void defaultLayout(AbstractVertex parentVertex, AbstractGraph graph){
		
		Iterator<AbstractVertex> it = graph.getVertices().values().iterator();
		while(it.hasNext())
		{
			AbstractVertex v = it.next();
			AbstractGraph inner_graph = v.getInnerGraph();
			if (inner_graph.getVertices().size() != 0)
			{
				//Layout the inner graphs of each node and assign width and height to each node
				//coordinates are RELATIVE to the parent
				if(v.isExpanded()){
					defaultLayout(v, v.getInnerGraph());
				}
			}
		}

		//Layout the current graph
		defaultNonRecursiveLayout(parentVertex, graph);
	}
	
	// We assign x,y to each vertex of graph
	// We assign width and height to the parentVertex
	public static void defaultNonRecursiveLayout(AbstractVertex parentVertex, AbstractGraph graph)
	{
	    Iterator<AbstractVertex> it = graph.getVertices().values().iterator();
		// Initialize all the nodes to be white
		while(it.hasNext())
		{
			it.next().vertexStatus = AbstractVertex.VertexStatus.WHITE;
		}
		
		ArrayList<AbstractVertex> arrayList = new ArrayList<AbstractVertex>();
		arrayList.addAll(graph.getVertices().values());
		Collections.sort(arrayList);
		AbstractVertex root = arrayList.get(0);
		

		double[] xyPair = visit(root, MARGIN_PADDING, MARGIN_PADDING);
		parentVertex.setWidth(xyPair[0] + 2 * MARGIN_PADDING);
		parentVertex.setHeight(xyPair[1] + 2 * MARGIN_PADDING);

	}
	
	public static double[] visit(AbstractVertex root, double left, double top)
	{
		root.vertexStatus = AbstractVertex.VertexStatus.GRAY;
		Iterator<AbstractVertex> it = root.getAbstractNeighbors().iterator();
		ArrayList<AbstractVertex> grayChildren = new ArrayList<AbstractVertex>(); 
		while(it.hasNext())
		{
			AbstractVertex child = it.next();
			if (child.vertexStatus == AbstractVertex.VertexStatus.WHITE)
			{
				child.vertexStatus = AbstractVertex.VertexStatus.GRAY;
				grayChildren.add(child);
			}
		}
		
		double currentWidth = 0;
		double currentHeight = 0; 
		Iterator<AbstractVertex> itGray = grayChildren.iterator();
		while(itGray.hasNext())
		{
			AbstractVertex curVer = itGray.next();
			double[] boundBox = visit(curVer,currentWidth + left,NODES_PADDING + top + root.getHeight());
			currentWidth += boundBox[0] + NODES_PADDING;
			currentHeight = Math.max(currentHeight, boundBox[1]);
		}
		
		root.subtreeBoundBox[0] = Math.max(root.getWidth(), currentWidth - NODES_PADDING);
		if(grayChildren.size() == 0)
		{
			root.subtreeBoundBox[1] = root.getHeight();
		}
		else
		{
			root.subtreeBoundBox[1] = NODES_PADDING + root.getHeight() + currentHeight;
		}
		
		root.setX(left + ((root.subtreeBoundBox[0] - root.getWidth()) / 2));  //left-most corner x
		root.setY(top);							    					//top-most corner y
		root.vertexStatus = AbstractVertex.VertexStatus.BLACK;
		return root.subtreeBoundBox;
	}
}
