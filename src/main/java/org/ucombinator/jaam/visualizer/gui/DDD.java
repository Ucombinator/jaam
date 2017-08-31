package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.HashSet;

public class DDD {

    private HashMap<GUINode,GUINodeStatus> db;

    private HashMap<GUINode,GUINodeStatus> enter;
    private HashMap<GUINode,GUINodeStatus> update;
    private HashMap<GUINode,GUINodeStatus> exit;


    public DDD(HashMap<GUINode,GUINodeStatus> ddd){
        this.db = ddd;
    }

    public DDD(){
        this.db = new HashMap<GUINode,GUINodeStatus>();
    }

    public DDD bind(HashMap<GUINode,GUINodeStatus> newDB){
        this.enter = new HashMap<GUINode,GUINodeStatus>();
        this.update = new HashMap<GUINode,GUINodeStatus>();
        this.exit = new HashMap<GUINode,GUINodeStatus>();

        HashSet<GUINode> allKeys = new HashSet<GUINode>(db.keySet()); allKeys.addAll(newDB.keySet());
        HashSet<GUINode> enterKeys = new HashSet<GUINode>(newDB.keySet()); enterKeys.removeAll(db.keySet());
        HashSet<GUINode> exitKeys = new HashSet<GUINode>(db.keySet()); exitKeys.removeAll(newDB.keySet());
        HashSet<GUINode> updateKeys = new HashSet<GUINode>(allKeys); updateKeys.removeAll(enterKeys); updateKeys.removeAll(exitKeys);


        for(GUINode key: enterKeys){
            enter.put(key,newDB.get(key));
        }

        for(GUINode key: exitKeys){
            exit.put(key,db.get(key));
        }

        for(GUINode key: updateKeys){
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
    private void runTransition(HashMap<GUINode,GUINodeStatus> set, TransitionFactory tf){
        for(GUINode g: set.keySet()){
            tf.build(g,set.get(g)).play();
        }
    }


    private ParallelTransition createTransition(Node n, GUINodeStatus gs){
        FadeTransition ft = new FadeTransition(Duration.millis(200));
        ft.setFromValue(n.getOpacity());
        ft.setToValue(gs.getOpacity());

        TranslateTransition tt = new TranslateTransition(Duration.millis(200));
        tt.setFromX(n.getLayoutX()); tt.setToX(gs.getX());
        tt.setFromY(n.getLayoutY()); tt.setToX(gs.getY());

        return new ParallelTransition(n,ft,tt);
    }




}
