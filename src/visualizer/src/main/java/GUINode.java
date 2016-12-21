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
    protected Rectangle rect;
    protected Text rectLabel;
    boolean isDragging;
    AbstractVertex vertex;
    GUINode parent;

    // A node in the main visualization will keep track of its mirror in the context view, and vice versa.
    // This allows us to update the location of both when either one of them is dragged.
    protected GUINode mirror;

    public GUINode(GUINode mirror)
    {
        super();
        this.rect = new Rectangle();
        this.rectLabel = new Text();
        this.getChildren().addAll(this.rect, this.rectLabel);

        this.setLayoutX(0);
        this.setLayoutY(0);
        this.makeDraggable();
        this.isDragging = false;
    }

    public GUINode(GUINode parent, AbstractVertex v)
    {
        super();
        this.parent = parent;
        this.vertex = v;
        this.rect = new Rectangle();
        this.rectLabel = new Text();
        this.getChildren().addAll(this.rect, this.rectLabel);

        this.rect.setOpacity(0.3);
        this.makeDraggable();
        this.isDragging = false;
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
    }

    public void setArcWidth(double height)
    {
        this.rect.setArcWidth(height);
    }

    public void setLocation(double x, double y, double width, double height)
    {
        /*System.out.println("Adding rectangle (x, y, width, height)");
        System.out.println(x + ", " + y + ", " + width + ", " + height);*/
        this.setLayoutX(x);
        this.setLayoutY(y);
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
            GUINode obj = (GUINode) (event.getSource());
            obj.rect.setOpacity(0.3);
            if(obj.parent != null)
                obj.parent.rect.setOpacity(1);
            //System.out.println("Setting low opacity: " + obj.vertex.id);
        }
    };
}
