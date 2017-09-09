package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.FadeTransition;
import javafx.animation.Transition;
import javafx.scene.Node;
import javafx.util.Duration;
import org.ucombinator.jaam.visualizer.layout.GraphicsStatus;

public class TransitionFactoryFadeIn extends TransitionFactory {
    @Override
    public Transition build(Node n, GraphicsStatus gs) {
        gs.setOpacity(1.0);
        return super.build(n,gs);
    }
}
