package org.ucombinator.jaam.visualizer.gui;

import java.util.ArrayList;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;
import org.ucombinator.jaam.visualizer.layout.AnimationHandler;
import org.ucombinator.jaam.visualizer.layout.LayoutRootVertex;
import org.ucombinator.jaam.visualizer.main.Main;

public class GUINode extends Pane
{
    protected static boolean showId = true;
    protected static final double TEXT_VERTICAL_PADDING = 15;
    protected static final double TEXT_HORIZONTAL_PADDING = 15;
	private double dragStartX, dragStartY;
    private Rectangle rect, highlightingRect;
    private Text rectLabel;
    private AbstractLayoutVertex vertex;
	private GUINode parent;

	private ArrayList<LayoutEdge> edges = new ArrayList<LayoutEdge>();

    boolean isDragging;

    private double totalScaleX;
    private double totalScaleY;

    
    public GUINode(GUINode parent, AbstractLayoutVertex v)
    {
        super();
        this.parent = parent;
        this.vertex = v;
        this.vertex.setGraphics(this);
        
        this.rect = new Rectangle();
        this.rectLabel = new Text(v.getId() + ", " + v.getLoopHeight());
        this.rectLabel.setVisible(v.isLabelVisible());

        
        this.highlightingRect = new Rectangle();
        this.highlightingRect.setVisible(false);
        this.highlightingRect.setStroke(javafx.scene.paint.Color.BLUE);
        this.highlightingRect.setFill(javafx.scene.paint.Color.WHITE);
        this.highlightingRect.setStrokeWidth(10);
        
        
        if(v instanceof LayoutRootVertex) {
            this.getChildren().add(this.rect);
        } else {
            this.getChildren().addAll(this.highlightingRect, this.rect, this.rectLabel);
        }

        this.rectLabel.setTranslateX(TEXT_HORIZONTAL_PADDING);
        this.rectLabel.setTranslateY(TEXT_VERTICAL_PADDING);

        this.isDragging = false;
        this.totalScaleX = 1;
        this.totalScaleY = 1;

        this.addMouseEvents();
        this.setVisible(true);
    }
    
    public AbstractLayoutVertex getVertex() {
		return vertex;
	}

	public void setVertex(AbstractLayoutVertex vertex) {
		this.vertex = vertex;
	}

    public String toString()
    {
        return rectLabel.getText().toString();
    }

    public void setLabel(String text)
    {
        this.rectLabel.setText(text);
    }

    // Next several methods: Pass on calls to underlying rectangle
    public void setFill(Color c)
    {
    	this.rect.setFill(c);
    	if(vertex.getType() == AbstractLayoutVertex.VertexType.CHAIN){
        	Stop[] stops = new Stop[]{new Stop(0.6,c), new Stop(0.4,Color.WHITE)};
            this.rect.setFill(new LinearGradient(0, 0, 8, 8, false, CycleMethod.REPEAT, stops));
        } else if(vertex.getType() == AbstractLayoutVertex.VertexType.ROOT){
        	this.rect.setFill(javafx.scene.paint.Color.WHITE);
        }
    }

    public void setStroke(Color c)
    {
        this.rect.setStroke(c);
    }

    public void setStrokeWidth(double strokeWidth)
    {
        this.rect.setStrokeWidth(strokeWidth);
    }

    public void setArcHeight(double height)
    {
        this.rect.setArcHeight(height);
        this.highlightingRect.setArcHeight(height);
    }

    public void setArcWidth(double width)
    {
        this.rect.setArcWidth(width);
        this.highlightingRect.setArcWidth(width);
    }

    public void setTranslateLocation(double x, double y) {
        this.setTranslateX(x);
        this.setTranslateY(y);
        this.rectLabel.setTranslateX(TEXT_HORIZONTAL_PADDING);
        this.rectLabel.setTranslateY(TEXT_VERTICAL_PADDING);
    }

    public void setTranslateLocation(double x, double y, double width, double height)
    {
        this.setTranslateX(x);
        this.setTranslateY(y);
        this.setMaxWidth(width);
        this.setMaxHeight(height);

        this.rect.setWidth(width);
        this.rect.setHeight(height);
        
        this.highlightingRect.setWidth(width);
        this.highlightingRect.setHeight(height);

        this.rectLabel.setTranslateX(TEXT_HORIZONTAL_PADDING);
        this.rectLabel.setTranslateY(TEXT_VERTICAL_PADDING);
    }

    // Returns the bounding box for just the rectangle in the coordinate system for the parent of our node.
    public Bounds getRectBoundsInParent() {
        Bounds nodeBounds = this.getBoundsInParent();
        Bounds rectBounds = this.rect.getBoundsInParent();
        BoundingBox totalBounds = new BoundingBox(nodeBounds.getMinX() + rectBounds.getMinX(),
                nodeBounds.getMinY() + rectBounds.getMinY(), rectBounds.getWidth(), rectBounds.getHeight());
        return totalBounds;
    }

    public void printLocation() {
        Bounds bounds = this.getBoundsInParent();
        System.out.println("Node x = " + bounds.getMinX() + ", " + bounds.getMaxX());
        System.out.println("Node y = " + bounds.getMinY() + ", " + bounds.getMaxY());
    }

    // Halve the distance from the current opacity to 1.
    public void increaseOpacity()
    {
        this.rect.setOpacity((1 + this.rect.getOpacity()) / 2.0);	
    }

    // Halve the current opacity.
    public void decreaseOpacity()
    {
        this.rect.setOpacity((this.rect.getOpacity()) / 2.0);
    }

    public void addMouseEvents()
    {
        this.setOnMousePressed(onMousePressedEventHandler);
        this.setOnMouseDragged(onMouseDraggedEventHandler);
        this.setOnMouseReleased(onMouseReleasedEventHandler);
        this.setOnMouseEntered(onMouseEnteredEventHandler);
        this.setOnMouseExited(onMouseExitedEventHandler);
        this.setOnMouseClicked(new AnimationHandler());
    }

    // The next two functions compute the shift that must be applied to keep the
    // top left corner stationary when the node is scaled about its center.
    public double getXShift()
    {
        double currentWidth = this.getScaleX() * this.vertex.getWidth();
        double oldWidth = this.vertex.getWidth();
        return (oldWidth - currentWidth) / 2;
        //return 0;
    }

    public double getYShift()
    {
        double currentHeight = this.getScaleY() * this.vertex.getHeight();
        double oldHeight = this.vertex.getHeight();
        return (oldHeight - currentHeight) / 2;
    }

    EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            event.consume();
            GUINode node = (GUINode) event.getSource();

            double scaleFactorX = Main.getOuterFrame().getCurrentFrame().getMainPanel().getPanelRoot().getGraphics().getScaleX();
            double scaleFactorY = Main.getOuterFrame().getCurrentFrame().getMainPanel().getPanelRoot().getGraphics().getScaleY();

            dragStartX = event.getScreenX() / scaleFactorX - node.getBoundsInParent().getMinX();
            dragStartY = event.getScreenY() / scaleFactorY - node.getBoundsInParent().getMinY();
        }
    };

    EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            event.consume();
            GUINode node = (GUINode) event.getSource();

            node.isDragging = true;
            double scaleFactorX = Main.getOuterFrame().getCurrentFrame().getMainPanel().getPanelRoot().getGraphics().getScaleX();
            double scaleFactorY = Main.getOuterFrame().getCurrentFrame().getMainPanel().getPanelRoot().getGraphics().getScaleY();
            double offsetX = event.getScreenX() / scaleFactorX - dragStartX;
            double offsetY = event.getScreenY() / scaleFactorY - dragStartY;
            if(GUINode.this.getParentNode() != null) {
                Bounds thisBounds = GUINode.this.rect.getBoundsInLocal();
                double thisWidth = thisBounds.getWidth();
                double thisHeight = thisBounds.getHeight();

                Bounds parentBounds = GUINode.this.getParentNode().rect.getBoundsInLocal();
                double maxOffsetX = parentBounds.getWidth() - thisWidth;
                double maxOffsetY = parentBounds.getHeight() - thisHeight;

                // This truncation of the offset confines our box to its parent.
                if (offsetX < 0)
                    offsetX = 0;
                else if (offsetX > maxOffsetX)
                    offsetX = maxOffsetX;

                if (offsetY < 0)
                    offsetY = 0;
                else if (offsetY > maxOffsetY)
                    offsetY = maxOffsetY;
            }

            double totalTranslateX = offsetX - node.getXShift();
            double totalTranslateY = offsetY - node.getYShift();
            node.setTranslateLocation(totalTranslateX, totalTranslateY);

            AbstractLayoutVertex v = GUINode.this.vertex;
            VizPanel mainPanel = Main.getOuterFrame().getCurrentFrame().getMainPanel();
            v.setX(mainPanel.invScaleX(offsetX));
            v.setY(mainPanel.invScaleY(offsetY));
            LayoutEdge.redrawEdges(v, false);
        }
    };

    EventHandler<MouseEvent> onMouseReleasedEventHandler = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            event.consume();
            GUINode node = (GUINode) event.getSource();

            if (node.isDragging)
            {
                node.isDragging = false;
            }
        }
    };

    EventHandler onMouseEnteredEventHandler = new javafx.event.EventHandler()
    {
        @Override
        public void handle(Event event)
        {
            event.consume();
        	if (vertex.getSelfGraph() != null)
        	{
	        	for(LayoutEdge e : vertex.getSelfGraph().getEdges().values())
                {
	        		if(e.getSourceVertex() == vertex || e.getDestVertex() == vertex)
	        		{
	        		    Line line = e.getLine();
	        		    line.setStroke(Color.ORANGERED);
	        		    line.setStrokeWidth(line.getStrokeWidth() * 4.0);
	        		}
	        	}
        	}
        }
    };

	EventHandler onMouseExitedEventHandler = new javafx.event.EventHandler()
    {
        @Override
        public void handle(Event event)
        {
            event.consume();
            //getChildren().remove(rectLabel);
            
        	if(vertex.getSelfGraph() != null)
        	{
	        	for(LayoutEdge e : vertex.getSelfGraph().getEdges().values())
                {
	        		if (e.getSourceVertex() == vertex || e.getDestVertex() == vertex)
	        		{
	        		    Line line = e.getLine();
	        			line.setStroke(Color.BLACK);
                        line.setStrokeWidth(line.getStrokeWidth() / 4.0);
	        		}
	        	}
        	}
        }
    };

	public GUINode getParentNode() {
	    return this.parent;
    }

    public double getTotalParentScaleX() {
	    if (this.parent != null)
	        return this.parent.totalScaleX;
	    else return 1;
    }

    public double getTotalParentScaleY() {
	    if (this.parent != null)
	        return this.parent.totalScaleY;
	    else return 1;
    }

	public void setTotalScaleX(double scale) {
	    this.totalScaleX = scale;
    }

	public double getTotalScaleX() {
	    return this.totalScaleX;
    }

    public void setTotalScaleY(double scale) {
	    this.totalScaleY = scale;
    }

    public double getTotalScaleY() {
	    return this.totalScaleY;
    }

	public void setLabelVisible(boolean isLabelVisible) {
		vertex.setLabelVisible(isLabelVisible);
		this.rectLabel.setVisible(isLabelVisible);
	}

	public Rectangle getHighlightingRect() {
		return this.highlightingRect;
	}
	
    public Rectangle getRect(){
    	return this.rect;
    }
}
