package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.GraphEntity;
import org.ucombinator.jaam.visualizer.layout.GraphicsStatus;
import org.ucombinator.jaam.visualizer.layout.LayoutEdge;


import java.util.HashMap;
import java.util.HashSet;

public class DDD {

    private HashMap<GraphEntity,GraphicsStatus> db;

    private HashMap<GraphEntity,GraphicsStatus> enter;
    private HashMap<GraphEntity,GraphicsStatus> update;
    private HashMap<GraphEntity,GraphicsStatus> exit;

    public HashMap<GraphEntity,GraphicsStatus> update(AbstractLayoutVertex v){
        return this.updateWithDB(v, this.retrieveAllGraphicsStatus(v));
    }

    private HashMap<GraphEntity,GraphicsStatus> updateWithDB(AbstractLayoutVertex v, HashMap<GraphEntity,GraphicsStatus> db) {
        GUINodeStatus gs = ((GUINodeStatus)db.get(v));
        gs.setX(v.getX());
        gs.setY(v.getY());

        gs.setWidth(v.getWidth());
        gs.setHeight(v.getHeight());


        for (AbstractLayoutVertex vChild: v.getInnerGraph().getVertices()) {
            this.updateWithDB(vChild,db);
        }

        return db;
    }


    public DDD(AbstractLayoutVertex root){
        this.db = retrieveAllGraphicsStatus(root);
    }


    public DDD bind(HashMap<GraphEntity,GraphicsStatus> newDB){
        this.enter = new HashMap<>();
        this.update = new HashMap<>();
        this.exit = new HashMap<>();

        HashSet<GraphEntity> allKeys = new HashSet<>(db.keySet()); allKeys.addAll(newDB.keySet());
        HashSet<GraphEntity> enterKeys = new HashSet<>(newDB.keySet()); enterKeys.removeAll(db.keySet());
        HashSet<GraphEntity> exitKeys = new HashSet<>(db.keySet()); exitKeys.removeAll(newDB.keySet());
        HashSet<GraphEntity> updateKeys = new HashSet<>(allKeys); updateKeys.removeAll(enterKeys); updateKeys.removeAll(exitKeys);


        for(GraphEntity key: enterKeys){
            enter.put(key,newDB.get(key));
        }

        for(GraphEntity key: exitKeys){
            exit.put(key,db.get(key));
        }

        for(GraphEntity key: updateKeys){
            update.put(key,newDB.get(key));
        }

        this.db = newDB;
        return this;
    }

    public void run(Runnable runnable){
        System.out.println("Objected to be updated: " + this.update.size());
        System.out.println("Objected to be removed: " + this.exit.size());
        System.out.println("Objected to be inserted: " + this.enter.size());

        ParallelTransition pt = new ParallelTransition(
                runTransition(this.update, new TransitionFactory()),
                runTransition(this.enter, new TransitionFactoryFadeIn()),
                runTransition(this.exit, new TransitionFactoryFadeOut())
        );

        if (runnable!=null){
            pt.setOnFinished(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    Platform.runLater(runnable);
                }
            });
        }
        pt.play();
    }

    public void run(){
        this.run(null);
    }


/*
    public DDD enter(TransitionFactory tf){
        runTransition(enter,tf);
        return this;
    }

    public DDD exit(TransitionFactory tf){
        runTransition(exit,tf);
        return this;
    }

    public DDD update(TransitionFactory tf){
        runTransition(update,tf);
        return this;
    }
*/
    private ParallelTransition runTransition(HashMap<GraphEntity,GraphicsStatus> set, TransitionFactory tf){
        ParallelTransition pt = new ParallelTransition();
        for(GraphEntity g: set.keySet()){
            pt.getChildren().add(
                tf.build(g.getGraphics(),set.get(g))
            );
        }
        return pt;
    }


    private HashMap<GraphEntity,GraphicsStatus> retrieveAllGraphicsStatus(AbstractLayoutVertex root){
        HashMap<GraphEntity,GraphicsStatus> db = new HashMap();
        retrieveAllGraphicsStatus(root, db);
        return db;
    }

    private void retrieveAllGraphicsStatus(AbstractLayoutVertex root, HashMap<GraphEntity,GraphicsStatus> db){
        db.put(root, new GUINodeStatus(root));
        for(LayoutEdge e: root.getInnerGraph().getEdges()){
            db.put(e, new EdgeStatus(e));
        }
        //System.out.print(root.getId());
        for(AbstractLayoutVertex v: root.getInnerGraph().getVertices()){
            retrieveAllGraphicsStatus(v, db);
        }
    }
    /*
    private ParallelTransition createTransition(Node n, GraphicsStatus gs){
        FadeTransition ft = new FadeTransition(Duration.millis(200));
        ft.setFromValue(n.getOpacity());
        ft.setToValue(gs.getOpacity());

        TranslateTransition tt = new TranslateTransition(Duration.millis(200));
        tt.setFromX(n.getLayoutX()); tt.setToX(gs.getX());
        tt.setFromY(n.getLayoutY()); tt.setToX(gs.getY());

        return new ParallelTransition(n,ft,tt);
    }
*/



}
