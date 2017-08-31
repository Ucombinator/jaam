package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.Transition;
import javafx.scene.Node;

public class TransitionFactoryFadeOut extends TransitionFactory {
    @Override
    public Transition build(Node n, GUINodeStatus gs) {
        gs.setOpacity(0.0);
        return super.build(n,gs);
    }
}

