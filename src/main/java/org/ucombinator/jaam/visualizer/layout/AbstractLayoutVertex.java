package org.ucombinator.jaam.visualizer.layout;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import javafx.animation.FadeTransition;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.ucombinator.jaam.visualizer.graph.*;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.gui.Location;
import org.ucombinator.jaam.visualizer.gui.VizPanel;

public abstract class AbstractLayoutVertex extends AbstractVertex implements Comparable<AbstractLayoutVertex>
{
    public static final Color highlightColor = Color.ORANGE;
    protected Color color = Color.RED;

    private Color[] colors = {
        Color.LIGHTCORAL,
        Color.LIGHTBLUE,
        Color.LIGHTCYAN,
        Color.LIGHTSEAGREEN,
        Color.LIGHTSALMON,
        Color.LIGHTSKYBLUE,
        Color.LIGHTGOLDENRODYELLOW,
        Color.LIGHTGREY};

    public static final double DEFAULT_WIDTH = 1.0;
    public static final double DEFAULT_HEIGHT = 1.0;

    private HierarchicalGraph selfGraph = null;
    private HierarchicalGraph innerGraph = null;

    private GUINode graphics;
    private boolean isExpanded;
    private boolean isLabelVisible;
    private boolean isEdgeVisible;
    private boolean drawEdges;

    // A location stores coordinates for a subtree.
    private Location location = new Location();
    private boolean updateLocation = false;

    // Used for shading vertices based on the number of nested loops they contain
    private int loopHeight;
    private AbstractLayoutVertex loopHeader;
    private ArrayList<AbstractLayoutVertex> loopChildren;
    private int dfsPathPos;

    public enum VertexType {
        INSTRUCTION, LOOP, METHOD, CHAIN, ROOT, SHRINK, SCC
    }
    private VertexType vertexType;

    public void setWidth(double width) {
        this.location.width = width;
    }
    public void setHeight(double height) {
        this.location.height = height;
    }

    public void setX(double x) {
        this.location.x = x;
    }
    public void setY(double y) {
        this.location.y = y;
    }

    public double getX() {
        return this.location.x;
    }
    public double getY() {
        return this.location.y;
    }
    public double getWidth() {
        return this.location.width;
    }
    public double getHeight() {
        return this.location.height;
    }

    public GUINode getGraphics()
    {
        return graphics;
    }
    public void setGraphics(GUINode graphics)
    {
        this.graphics = graphics;
    }

    public void setDFSPosition(int pos) {
        this.dfsPathPos = pos;
    }

    public int getDFSPosition() {
        return this.dfsPathPos;
    }

    public int getLoopHeight() {
        return this.loopHeight;
    }

    public void setLoopHeight(int loopHeight) {
        this.loopHeight = loopHeight;
    }

    public void setLoopHeader(AbstractLayoutVertex v)
    {
        this.loopHeader = v;
    }

    public AbstractLayoutVertex getLoopHeader()
    {
        return this.loopHeader;
    }

    public void addLoopChild(AbstractLayoutVertex v)
    {
        if(this != v)
            loopChildren.add(v);
        else
            System.out.println("Error! Cannot add self as header in loop decomposition.");
    }

    public ArrayList<AbstractLayoutVertex> getLoopChildren()
    {
        return loopChildren;
    }

    public void setColor(int maxLoopHeight) {
        if(this instanceof LayoutInstructionVertex) {
            double hue = (0.4 - 0.4 * (((double) this.loopHeight) / maxLoopHeight)) * 360;
            this.color = Color.hsb(hue, 0.9, 0.9);
        }
        else {
            for (AbstractLayoutVertex v : this.getInnerGraph().getVertices())
                v.setColor(maxLoopHeight);
        }
    }

    public int compareTo(AbstractLayoutVertex v) {
        return ((Integer)(this.getMinInstructionLine())).compareTo(v.getMinInstructionLine());
    }

    // Subclasses must override these so that we have descriptions for each of them,
    // and so that our generic collapsing can work for all of them
    public abstract String getRightPanelContent();
    public abstract String getShortDescription();

    // These searches may be different for different subclasses, so we implement them there.
    public abstract boolean searchByMethod(String query, VizPanel mainPanel);

    // This is needed so that we can show the code for the methods that correspond to selected vertices
    public abstract HashSet<LayoutMethodVertex> getMethodVertices();

    static int colorIndex = 0;

    public AbstractLayoutVertex(String label, VertexType type, boolean drawEdges) {
        super(label);
        this.graphics = null;
        this.isExpanded = true;
        this.isLabelVisible = false; // If you change this, also change the initialization for MainTabController
        this.isEdgeVisible = true;
        this.drawEdges = drawEdges;

        this.innerGraph = new HierarchicalGraph();
        this.vertexType = type;

        this.loopChildren = new ArrayList<AbstractLayoutVertex>();
        this.loopHeight = -1;
        this.dfsPathPos = -1;

        if (this.vertexType == VertexType.METHOD)
                this.setColor(colors[colorIndex++ % colors.length]);

/*        if (this.vertexType == VertexType.METHOD){
            System.out.println("**************************METHOD*********************+");
            if(!this.getInnerGraph().getVertices().isEmpty()) {
                AbstractLayoutVertex v = this.getInnerGraph().getVertices().values().iterator().next();
                if (v instanceof LayoutLoopVertex) {
                    this.setColor(loopColor);
                } else if (v instanceof LayoutMethodVertex) {
                    this.setColor(methodColor);
                }
            }
        }
*/

        this.setVisible(false);
    }
    
    
    public AbstractLayoutVertex(int id, String label){
    	super(id, label);
    	this.graphics = null;
        this.isExpanded = true;
        this.isLabelVisible = false; // If you change this, also change the initialization for MainTabController
        this.isEdgeVisible = true;
        this.drawEdges = true;

        this.innerGraph = new HierarchicalGraph();
        

        this.loopChildren = new ArrayList<AbstractLayoutVertex>();
        this.loopHeight = -1;
        this.dfsPathPos = -1;

        
        this.setColor(colors[colorIndex++ % colors.length]);

        this.setVisible(false);
    }

    public void setColor(Color c) {
        this.color = c;
    }

    public Paint getFill() {
        return this.color;
    }

    // Override in base case, LayoutInstructionVertex
    public int getMinInstructionLine() {
        int minIndex = Integer.MAX_VALUE;
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices()) {
            minIndex = Math.min(minIndex, v.getMinInstructionLine());
        }

        return minIndex;
    }

    public int calcMaxLoopHeight() {
        int maxLoopHeight = 0;
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices())
            maxLoopHeight = Math.max(maxLoopHeight, v.getLoopHeight());

        this.setLoopHeight(maxLoopHeight);
        return maxLoopHeight;
    }

    public boolean getDrawEdges() {
        return this.drawEdges;
    }

    public HierarchicalGraph getInnerGraph() {
        return innerGraph;
    }

    public HierarchicalGraph getSelfGraph() {
        return selfGraph;
    }

    public void setInnerGraph(HierarchicalGraph innerGraph) {
        this.innerGraph = innerGraph;
    }

    public void recomputeGraphicsSize(VizPanel mainPanel)
    {
        double pixelWidth = mainPanel.scaleX(this.location.width);
        double pixelHeight = mainPanel.scaleY(this.location.height);
        
        this.getGraphics().getRect().setWidth(pixelWidth);
        this.getGraphics().getRect().setHeight(pixelHeight);
        
        this.getGraphics().getHighlightingRect().setWidth(pixelWidth);
        this.getGraphics().getHighlightingRect().setHeight(pixelHeight);
    }

    public void setVisible(boolean isVisible)
    {
        if(this.getGraphics() != null)
            this.getGraphics().setVisible(isVisible);
    }

    public boolean addTreeNodes(TreeItem parentNode, VizPanel mainPanel) {
        boolean addedNodes = false;
        TreeItem newNode = new TreeItem(this);
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices())
            addedNodes |= v.addTreeNodes(newNode, mainPanel);

        if(mainPanel.getHighlighted().contains(this) || addedNodes) {
            parentNode.getChildren().add(newNode);
            return true;
        }
        else
            return false;
    }

    public double distTo(double x, double y)
    {
        double xDiff = x - this.location.x;
        double yDiff = y - this.location.y;
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    public void resetGraphics() {
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices()) {
            v.resetGraphics();
        }

        for(LayoutEdge e : this.getInnerGraph().getEdges()) {
            e.resetGraphics();
        }

        this.graphics = null;
    }

    public void searchByID(int id, VizPanel mainPanel)
    {
        this.searchByIDRange(id, id, mainPanel);
    }

    public void searchByIDRange(int id1, int id2, VizPanel mainPanel)
    {
        if(this.getId() >= id1 && this.getId() <= id2) {
            this.setHighlighted(true);
            mainPanel.getHighlighted().add(this);
            System.out.println("Search successful: " + this.getId());
        }

        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices())
            v.searchByIDRange(id1, id2, mainPanel);
    }

    public void searchByInstruction(String query, VizPanel mainPanel)
    {
        if(this instanceof LayoutInstructionVertex) {
            String instStr = ((LayoutInstructionVertex) this).getInstruction().getText();
            if(instStr.contains(query)) {
                this.setHighlighted(true);
                mainPanel.getHighlighted().add(this);
            }
        }

        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices())
            v.searchByInstruction(query, mainPanel);
    }

    public void setHighlighted(boolean isHighlighted)
    {
        if(isHighlighted) {
            this.getGraphics().setFill(highlightColor);
            this.setGraphicsHighlighted(true);
        }
        else {
            this.getGraphics().setFill(this.color);
            this.setGraphicsHighlighted(false);
        }
    }

    private void setGraphicsHighlighted(boolean visible) {
        Rectangle r = this.getGraphics().getHighlightingRect();
        FadeTransition ft = new FadeTransition(Duration.millis(300), r);
        if(visible) {
            this.getGraphics().getHighlightingRect().setVisible(true);
            ft.setFromValue(0f);
            ft.setToValue(1f);
        } else {
            ft.setFromValue(1f);
            ft.setToValue(0f);
        }
//        AbstractLayoutVertex vertex = this;
//        ft.setOnFinished(new EventHandler<ActionEvent>() {
//            
//            @Override
//            public void handle(ActionEvent event) {
//                //vertex.getGraphics().getHighlightingRect().setVisible(visible);
//            }
//        });
        ft.play();
    }

    public void setSelfGraph(HierarchicalGraph abstractGraph) {
        this.selfGraph = abstractGraph;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;

    }

    public boolean isLabelVisible() {
        return isLabelVisible;
    }
    public void setLabelVisible(boolean isLabelVisible) {
        this.isLabelVisible = isLabelVisible;
    }

    public boolean isEdgeVisible() {
        return isEdgeVisible;
    }

    public void setEdgeVisible(boolean isEdgeVisible) {
        this.isEdgeVisible = isEdgeVisible;
    }

    public void setEdgeVisibility(boolean isEdgeVisible)
    {
        this.setEdgeVisible(isEdgeVisible);
        for(LayoutEdge e : this.innerGraph.getEdges())
            e.setVisible(isEdgeVisible);

        for(AbstractLayoutVertex v : this.innerGraph.getVertices())
            v.setEdgeVisibility(isEdgeVisible);
    }

    public void setLabelVisibility(boolean isLabelVisible)
    {
        this.setLabelVisible(isLabelVisible);
        this.getGraphics().setLabelVisible(isLabelVisible);

        for(AbstractLayoutVertex v : this.innerGraph.getVertices())
            v.setLabelVisibility(isLabelVisible);
    }

    public void resetStrokeWidth(double factor) {
        this.getGraphics().setStrokeWidth(factor);
        for(AbstractLayoutVertex v : this.getInnerGraph().getVertices())
            v.resetStrokeWidth(factor);
    }
    
    public void printCoordinates()
    {
        System.out.println("Vertex " + this.getId() + this.location.toString());
    }

    public VertexType getType() {
        return this.vertexType;
    }

    public void toggleNodesOfType(VertexType type, boolean isExpanded) {
        if(this.getType() == type){
            this.setExpanded(isExpanded);
        }

        Iterator<AbstractLayoutVertex> it = this.getInnerGraph().getVertices().iterator();
        while(it.hasNext()){
            it.next().toggleNodesOfType(type, isExpanded);
        }
    }

    public void toggleEdges(boolean isEdgeVisible) {
        Iterator<LayoutEdge> itEdges = this.getInnerGraph().getEdges().iterator();
        while(itEdges.hasNext()) {
            LayoutEdge e = itEdges.next();
            if(e.getGraphics() != null) {
                e.getGraphics().setVisible(!e.getGraphics().isVisible() && isEdgeVisible);
            }
        }

        Iterator<AbstractLayoutVertex> itNodes = this.getInnerGraph().getVertices().iterator();
        while(itNodes.hasNext()){
            itNodes.next().toggleEdges(isEdgeVisible);
        }
    }

    public HashSet<Instruction> getInstructions() {
        return this.getInstructions(new LinkedHashSet<Instruction>());
    }

    private HashSet<Instruction> getInstructions(HashSet<Instruction> instructions) {
        if(this.getType().equals(VertexType.ROOT) || this.getType().equals(VertexType.METHOD) || this.getType().equals(VertexType.CHAIN)) {
            Iterator<AbstractLayoutVertex> it = this.getInnerGraph().getVertices().iterator();
            while(it.hasNext()){
                it.next().getInstructions(instructions);
            }
        } else if(this.getType().equals(VertexType.INSTRUCTION)){
            instructions.add(((LayoutInstructionVertex) this).getInstruction());
        } else {
            System.out.println("Unrecognized type in method getInstructions: " + this.getType());
        }

        return instructions;
    }

    public HashSet<AbstractLayoutVertex> getVerticesWithInstructionID(int id, String method_name){
        return getVerticesWithInstructionID(id, method_name, new LinkedHashSet<AbstractLayoutVertex>());
    }

    private HashSet<AbstractLayoutVertex> getVerticesWithInstructionID(int id, String method_name, HashSet<AbstractLayoutVertex> set){
        if(this.getType().equals(VertexType.ROOT) || this.getType().equals(VertexType.METHOD) || this.getType().equals(VertexType.CHAIN)){
            Iterator<AbstractLayoutVertex> it = this.getInnerGraph().getVertices().iterator();
            while(it.hasNext()){
                it.next().getVerticesWithInstructionID(id, method_name, set);
            }
        } else if(this.getType().equals(VertexType.INSTRUCTION)) {
            Instruction inst = ((LayoutInstructionVertex) this).getInstruction();
            if(inst.getMethodName() == method_name && inst.getJimpleIndex() == id) {
                set.add(this);
            }
        } else {
            System.out.println("Unrecognized type in method getInstructions: " + this.getType());
        }

        return set;
    }

    public void cleanAll(){
        this.setVertexStatus(VertexStatus.WHITE);
        Iterator<AbstractLayoutVertex> it = this.getInnerGraph().getVertices().iterator();
        while(it.hasNext()) {
            AbstractLayoutVertex v = it.next();
            v.cleanAll();
        }
    }

    public int getTotalEdgeCount(){
        int result = this.getInnerGraph().getEdges().size();
        for(AbstractLayoutVertex v: this.getInnerGraph().getVertices()){
            result += v.getTotalEdgeCount();
        }
        return result;
    }

    public int getTotalVertexCount(){
        int result = this.getInnerGraph().getVertices().size();
        for(AbstractLayoutVertex v: this.getInnerGraph().getVertices()) {
            result += v.getTotalVertexCount();
        }
        return result;
    }

    public void setDefaultColor(){
    }
}
