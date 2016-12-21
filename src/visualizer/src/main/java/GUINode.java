
import java.util.Iterator;

import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

// TODO: Stack vertex labels on top of vertices.
public class GUINode extends Pane
{
    double dragX, dragY;
    protected Rectangle back_rect,rect;
    protected Text rectLabel;
    boolean isDragging;
    private AbstractVertex vertex;
	GUINode parent;

    // A node in the main visualization will keep track of its mirror in the context view, and vice versa.
    // This allows us to update the location of both when either one of them is dragged.
    protected GUINode mirror;

    public GUINode(GUINode parent, AbstractVertex v)
    {
        super();
        this.parent = parent;
        this.vertex = v;
        this.vertex.setGraphics(this);
        this.back_rect = new Rectangle();
        this.rect = new Rectangle();
        this.rectLabel = new Text();
        this.getChildren().addAll(this.back_rect, this.rect, this.rectLabel);

        this.rect.setOpacity(0.2);
        this.makeDraggable();
        this.isDragging = false;
        
        this.setOnMouseClicked(new AnimationHandler());
    }
    
    public AbstractVertex getVertex() {
		return vertex;
	}

	public void setVertex(AbstractVertex vertex) {
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
    	this.back_rect.setFill(Color.WHITE);
        this.rect.setFill(c);
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
    	this.back_rect.setArcHeight(height);
        this.rect.setArcHeight(height);
    }

    public void setArcWidth(double height)
    {
    	this.back_rect.setArcWidth(height);
        this.rect.setArcWidth(height);
    }

    public void setLocation(double x, double y, double width, double height)
    {
        this.setLayoutX(x);
        this.setLayoutY(y);
        this.back_rect.setWidth(width);
        this.back_rect.setHeight(height);
        this.rect.setWidth(width);
        this.rect.setHeight(height);
    }

    public void makeDraggable()
    {
        this.setOnMousePressed(onMousePressedEventHandler);
        this.setOnMouseDragged(onMouseDraggedEventHandler);
        this.setOnMouseReleased(onMouseReleasedEventHandler);
        this.setOnMouseEntered(onMouseEnteredEventHandler);
        this.setOnMouseExited(onMouseExitedEventHandler);
    }

    EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            event.consume();
            GUINode node = (GUINode) event.getSource();
            System.out.println("GUI node pressed: " + node.vertex.id);

            dragX = node.getBoundsInParent().getMinX() - event.getScreenX();
            dragY = node.getBoundsInParent().getMinY() - event.getScreenY();
        }
    };

    EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            event.consume();
            GUINode node = (GUINode) event.getSource();
            System.out.println("GUI node dragged: " + node.vertex.id);

            node.isDragging = true;
            double offsetX = event.getScreenX() + dragX;
            double offsetY = event.getScreenY() + dragY;
            node.relocate(offsetX, offsetY);
            // TODO: Also adjust mirror node
        }
    };

    EventHandler<MouseEvent> onMouseReleasedEventHandler = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            event.consume();
            GUINode node = (GUINode) event.getSource();
            System.out.println("GUI node released: " + node.vertex.id);

            if(node.isDragging)
            {
                node.isDragging = false;
            }
            else
            {
                //TODO: Select node on click?
            }
        }
    };

    EventHandler onMouseEnteredEventHandler = new javafx.event.EventHandler()
    {
        @Override
        public void handle(Event event)
        {
        	if(vertex.getSelfGraph()!=null){
	        	Iterator<Edge> it = vertex.getSelfGraph().getEdges().values().iterator();
	        	while(it.hasNext()){
	        		Edge e = it.next();
	        		if(e.getSourceVertex() == vertex || e.getDestVertex() == vertex)
	        		{
	        		    e.getLine().setStroke(Color.GREENYELLOW);
	        		    e.getLine().setStrokeWidth(2);
	        		}
	        	}
        	}
        	
            GUINode obj = (GUINode) (event.getSource());
            obj.rect.setOpacity(1);
            if(obj.parent != null)
                obj.parent.rect.setOpacity(0.3);
            //System.out.println("Setting high opacity: " + obj.vertex.id);
        }
    };

	EventHandler onMouseExitedEventHandler = new javafx.event.EventHandler()
    {
        @Override
        public void handle(Event event)
        {
        	if(vertex.getSelfGraph()!=null)
        	{
	        	Iterator<Edge> it = vertex.getSelfGraph().getEdges().values().iterator();
	        	while(it.hasNext())
                {
	        		Edge e = it.next();
	        		if(e.getSourceVertex() == vertex || e.getDestVertex() == vertex)
	        		{
	        			e.getLine().setStroke(Color.BLACK);
	        			e.getLine().setStrokeWidth(1);
	        		}
	        	}
        	}
        	
            GUINode obj = (GUINode) (event.getSource());
            obj.rect.setOpacity(0.3);
            if(obj.parent != null)
                obj.parent.rect.setOpacity(1);
            //System.out.println("Setting low opacity: " + obj.vertex.id);
        }
    };
}