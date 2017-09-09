package org.ucombinator.jaam.visualizer.gui;

import com.sun.javafx.geom.Edge;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.ucombinator.jaam.visualizer.graph.AbstractVertex;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.GraphEntity;
import org.ucombinator.jaam.visualizer.layout.GraphicsStatus;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by giordano on 8/28/17.
 */
public class GUINodeStatus implements GraphicsStatus {


    private double x, y;
    private double totalScaleX;
    private double totalScaleY;
    private double opacity;
    private AbstractLayoutVertex vertex;

    public GUINodeStatus(AbstractLayoutVertex v){
        this.vertex = v;
        GUINode g = v.getGraphics();
        this.x = g.localToScene(g.getBoundsInLocal()).getMinX();
        this.y = g.localToScene(g.getBoundsInLocal()).getMinY();
        this.totalScaleX = g.getScaleX();
        this.totalScaleY = g.getScaleY();
        this.opacity = g.getOpacity();
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        this.opacity = opacity;
    }

    public double getX() {

        return this.x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }


    public double getTotalScaleX() {
        return totalScaleX;
    }

    public void setTotalScaleX(double totalScaleX) {
        this.totalScaleX = totalScaleX;
    }

    public double getTotalScaleY() {
        return totalScaleY;
    }

    public void setTotalScaleY(double totalScaleY) {
        this.totalScaleY = totalScaleY;
    }

    public static HashMap<GraphEntity,GraphicsStatus> retrieveAllGraphicsStatus(AbstractLayoutVertex root){
        HashMap<GraphEntity,GraphicsStatus> db = new HashMap();
        retrieveAllGraphicsStatus(root, db);
        return db;
    }

    private static void retrieveAllGraphicsStatus(AbstractLayoutVertex root, HashMap<GraphEntity,GraphicsStatus> db){
        db.put(root, new GUINodeStatus(root));
        for(LayoutEdge e: root.getInnerGraph().getEdges()){
            db.put(e, new EdgeStatus(e));
        }
        //System.out.print(root.getId());
        for(AbstractLayoutVertex v: root.getInnerGraph().getVertices()){
            retrieveAllGraphicsStatus(v, db);
        }
    }
}
