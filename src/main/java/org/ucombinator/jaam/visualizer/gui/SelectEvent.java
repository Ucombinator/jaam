package org.ucombinator.jaam.visualizer.gui;

import javafx.event.*;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;

public class SelectEvent extends javafx.event.Event{

    private AbstractLayoutVertex vertex;

    /** The only valid EventType for the CustomEvent. */
    public static final EventType<SelectEvent> VERTEX_SELECTED =
        new EventType<>(Event.ANY, "VERTEX_SELECTED");

    public SelectEvent() {
        super(VERTEX_SELECTED);
        vertex = null;
    }

    public SelectEvent(Object source, EventTarget target) {
        super(source, target, VERTEX_SELECTED);

        vertex = ((GUINode)target).getVertex();
    }

    public AbstractLayoutVertex getVertex() { return vertex; }

    @Override
    public SelectEvent copyFor(Object newSource, EventTarget newTarget) {
        return (SelectEvent) super.copyFor(newSource, newTarget);
    }

    @Override
    public EventType<? extends SelectEvent> getEventType() {
        return (EventType<? extends SelectEvent>) super.getEventType();
    }
}
