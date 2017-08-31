package org.ucombinator.jaam.visualizer.gui;

import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.ucombinator.jaam.visualizer.graph.AbstractVertex;
import org.ucombinator.jaam.visualizer.graph.Graph;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by giordano on 8/28/17.
 */
public class GUINodeStatus {


    private double x, y;
    private double totalScaleX;
    private double totalScaleY;
    private double opacity;

    public GUINodeStatus(GUINode g){
        this.x = g.getLayoutX();
        this.y = g.getLayoutY();
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
        return x;
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

    public static HashMap<GUINode,GUINodeStatus> retrieveAllGUINodeStatus(AbstractLayoutVertex root){
        HashMap<GUINode,GUINodeStatus> db = new HashMap<GUINode,GUINodeStatus>();
        retrieveAllGUINodeStatus(root, db);
        return db;
    }

    private static void retrieveAllGUINodeStatus(AbstractLayoutVertex root, HashMap<GUINode,GUINodeStatus> db){
        db.put(root.getGraphics(), new GUINodeStatus(root.getGraphics()));
        //System.out.print(root.getId());
        for(AbstractLayoutVertex v: root.getInnerGraph().getVertices()){
            retrieveAllGUINodeStatus(v, db);
        }
    }
}
