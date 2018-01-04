package org.ucombinator.jaam.visualizer.gui;

import javafx.event.*;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.StateVertex;

public class SelectEvent<T extends AbstractLayoutVertex<T>> extends javafx.event.Event {

    private T vertex;

    /** The only valid EventType for the CustomEvent. */
    public static final EventType<SelectEvent<StateVertex>> VERTEX_SELECTED =
        new EventType<>(Event.ANY, "VERTEX_SELECTED");

    public SelectEvent() {
        super(VERTEX_SELECTED);
        this.vertex = null;
    }

    public SelectEvent(Object source, GUINode<T> target) {
        super(source, target, VERTEX_SELECTED);

        this.vertex = target.getVertex();
    }

    public T getVertex() { return this.vertex; }

    @Override
    public SelectEvent<T> copyFor(Object newSource, EventTarget newTarget) {
        return (SelectEvent<T>) super.copyFor(newSource, newTarget);
    }

    @Override
    public EventType<? extends SelectEvent<T>> getEventType() {
        return (EventType<? extends SelectEvent<T>>) super.getEventType();
    }
}
