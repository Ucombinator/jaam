package org.ucombinator.jaam.visualizer.gui;

import javafx.event.*;
import org.ucombinator.jaam.interpreter.State;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.state.StateVertex;
import org.ucombinator.jaam.visualizer.taint.TaintVertex;

public class SelectEvent<T extends AbstractLayoutVertex> extends javafx.event.Event {

    private static final long serialVersionUID = 3351716759550629777L;
    private T vertex;
    public enum VERTEX_TYPE {STATE, TAINT};


    /** The only valid EventType for the CustomEvent. */
    public static final EventType<SelectEvent<StateVertex>> STATE_VERTEX_SELECTED =
        new EventType<>(Event.ANY, "STATE_VERTEX_SELECTED");

    public static final EventType<SelectEvent<TaintVertex>> TAINT_VERTEX_SELECTED =
            new EventType<>(Event.ANY, "TAINT_VERTEX_SELECTED");

    public SelectEvent(Object source, EventTarget target, T vertex, VERTEX_TYPE type) {
        super(source, target, type == VERTEX_TYPE.STATE ? STATE_VERTEX_SELECTED : TAINT_VERTEX_SELECTED);
        this.vertex = vertex;
    }

    /*
    public SelectEvent(Object source, EventTarget target, TaintVertex vertex) {
        super(source, target, TAINT_VERTEX_SELECTED);
        this.vertex = vertex;
    }
    */

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
