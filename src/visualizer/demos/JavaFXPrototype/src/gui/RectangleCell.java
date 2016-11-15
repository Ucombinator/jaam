package gui;

/**
 * Created by timothyjohnson on 11/14/16.
 */
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import gui.Cell;

public class RectangleCell extends Cell {
    public static int dim = 5;

    public RectangleCell( String id) {
        super( id);

        Rectangle view = new Rectangle(dim, dim);

        view.setStroke(Color.DODGERBLUE);
        view.setFill(Color.DODGERBLUE);

        setView( view);

    }

}
