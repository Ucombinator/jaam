package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;
import org.ucombinator.jaam.visualizer.layout.GraphEntity;
import org.ucombinator.jaam.visualizer.layout.GraphicsStatus;


import java.util.HashMap;
import java.util.HashSet;

public class DDD {

    private HashMap<GraphEntity,GraphicsStatus> db;

    private HashMap<GraphEntity,GraphicsStatus> enter;
    private HashMap<GraphEntity,GraphicsStatus> update;
    private HashMap<GraphEntity,GraphicsStatus> exit;


    public DDD(HashMap<GraphEntity,GraphicsStatus> ddd){
        this.db = ddd;
    }

    public DDD(){
        this.db = new HashMap<>();
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

        return this;
    }

    public void run(){
        System.out.println("Objected to be updated: " + this.update.size());
        System.out.println("Objected to be removed: " + this.exit.size());
        System.out.println("Objected to be inserted: " + this.enter.size());
        runTransition(this.update, new TransitionFactory());
        runTransition(this.enter, new TransitionFactoryFadeIn());
        runTransition(this.exit, new TransitionFactoryFadeOut());
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
    private void runTransition(HashMap<GraphEntity,GraphicsStatus> set, TransitionFactory tf){
        ParallelTransition pt = new ParallelTransition();
        for(GraphEntity g: set.keySet()){
            pt.getChildren().add(
                tf.build(g.getGraphics(),set.get(g))
            );
        }
    pt.play();
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
