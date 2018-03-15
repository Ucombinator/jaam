package org.ucombinator.jaam.visualizer.gui;

import javafx.event.*;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.state.StateVertex;
import org.ucombinator.jaam.visualizer.taint.TaintVertex;

public class SelectEvent<T extends AbstractLayoutVertex<T>> extends javafx.event.Event {

    private T vertex;

    /** The only valid EventType for the CustomEvent. */
    public static final EventType<SelectEvent<StateVertex>> STATE_VERTEX_SELECTED =
        new EventType<>(Event.ANY, "STATE_VERTEX_SELECTED");

    public static final EventType<SelectEvent<TaintVertex>> TAINT_VERTEX_SELECTED =
            new EventType<>(Event.ANY, "TAINT_VERTEX_SELECTED");

    // TODO: Modify constructors to use the type parameter
    public SelectEvent(Object source, EventTarget target, StateVertex vertex) {
        super(source, target, STATE_VERTEX_SELECTED);
        this.vertex = ((GUINode<T>) target).getVertex(); // Actually equals vertex parameter
    }

    public SelectEvent(Object source, EventTarget target, TaintVertex vertex) {
        super(source, target, TAINT_VERTEX_SELECTED);
        this.vertex = ((GUINode<T>) target).getVertex(); // Actually equals vertex parameter
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
