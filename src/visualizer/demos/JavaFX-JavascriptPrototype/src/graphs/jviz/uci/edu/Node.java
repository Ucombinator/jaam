package graphs.jviz.uci.edu;

import java.util.HashMap;
import java.util.Map;

import jviz.uci.edu.UCIJSObject;

public class Node implements Graphics{
	private String id;
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	private String label;
	private Map<String,Node> edges;
	private Map<String,Node> neighbors;
	private UCIJSObject obj;

	private Point center;
	
	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public UCIJSObject getGraphics() {
		return obj;
	}

	@Override
	public UCIJSObject setGraphics(UCIJSObject obj) {
		return this.obj = obj;
	}
	
	public Point getCenter() {
		return center;
	}

	public void setCenter(int x, int y) {
		this.center = new Point(x,y);
	}

	public Node(String id, String label) {
		super();
		this.id = id;
		this.label = label;
		this.edges = new HashMap<String,Node>();
		this.neighbors = new HashMap<String,Node>();
	}

	

}
