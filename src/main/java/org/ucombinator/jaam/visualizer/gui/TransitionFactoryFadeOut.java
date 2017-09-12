package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.scene.Node;
import org.ucombinator.jaam.visualizer.layout.GraphicsStatus;

public class TransitionFactoryFadeOut extends TransitionFactory {
    @Override
    public ParallelTransition build(Node n, GraphicsStatus gs) {
        gs.setOpacity(0.0);
        return super.build(n,gs);
    }
}

