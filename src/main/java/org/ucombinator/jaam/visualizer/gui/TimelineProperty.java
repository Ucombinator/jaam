package org.ucombinator.jaam.visualizer.gui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.util.Duration;

public class TimelineProperty {
    public static <Out, In extends Out> void bind(WritableValue<Out> out, ObservableValue<In> in, double millis) {
      in.addListener((o, oldValue, newValue) ->
        new Timeline(new KeyFrame(new Duration(millis), new KeyValue(out, newValue))).play());
    }
}
