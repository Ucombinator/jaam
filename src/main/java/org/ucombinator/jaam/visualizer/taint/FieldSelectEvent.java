package org.ucombinator.jaam.visualizer.taint;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import org.ucombinator.jaam.visualizer.gui.GUINode;
import org.ucombinator.jaam.visualizer.layout.AbstractLayoutVertex;
import org.ucombinator.jaam.visualizer.layout.StateVertex;

public class FieldSelectEvent extends Event {

    private String className;
    private String fieldName;

    public static final EventType<FieldSelectEvent> FIELD_SELECTED =
            new EventType<>(Event.ANY, "FIELD_SELECTED");

    // TODO: Modify constructors to use the type parameter
    public FieldSelectEvent(Object source, EventTarget target, String fullName) {
        super(source, target, FIELD_SELECTED);
        this.className = fullName.substring(0, fullName.indexOf(':'));
        this.fieldName = fullName.substring(fullName.indexOf(':') +1);
    }

    public String getClassName() { return className; }

    public String getFieldName() { return fieldName; }

    @Override
    public FieldSelectEvent copyFor(Object newSource, EventTarget newTarget) {
        return (FieldSelectEvent) super.copyFor(newSource, newTarget);
    }

    @Override
    public EventType<? extends FieldSelectEvent> getEventType() {
        return (EventType<? extends FieldSelectEvent>) super.getEventType();
    }
}
