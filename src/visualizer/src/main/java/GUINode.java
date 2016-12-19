import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import javafx.scene.paint.Color;

/**
 * Created by timothyjohnson on 12/12/16.
 */
public class GUINode extends StackPane
{
    double dragX, dragY;
    protected Rectangle rect;
    protected Text rectLabel;
    boolean isDragging;

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

    public String toString()
    {
        return rectLabel.getText().toString();
    }

    public void setFill(Color c)
    {
        this.rect.setFill(c);
    }

    public void setStroke(Color c)
    {
        this.rect.setStroke(c);
    }

    public void setLocation(double x1, double y1, double x2, double y2)
    {
        this.setLayoutX(x1);
        this.setLayoutY(y1);
        this.rect.setWidth(x2 - x1);
        this.rect.setHeight(y2 - y1);
    }

    public void makeDraggable()
    {
        this.setOnMousePressed(onMousePressedEventHandler);
        this.setOnMouseDragged(onMouseDraggedEventHandler);
        this.setOnMouseReleased(onMouseReleasedEventHandler);
    }

    EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            System.out.println("GUI node pressed");
            event.consume();

            GUINode node = (GUINode) event.getSource();
            dragX = node.getBoundsInParent().getMinX() - event.getScreenX();
            dragY = node.getBoundsInParent().getMinY() - event.getScreenY();
        }
    };

    EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            System.out.println("GUI node dragged");
            event.consume();

            GUINode node = (GUINode) event.getSource();
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
            System.out.println("GUI node released");
            event.consume();

            GUINode node = (GUINode) event.getSource();
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
}
