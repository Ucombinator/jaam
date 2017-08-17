package org.ucombinator.jaam.visualizer.gui;

import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

public class ZoomSpinnerValueFactory extends SpinnerValueFactory<Double> {
    private final double factor;
    public ZoomSpinnerValueFactory(double value, double factor) {
        this.setConverter(CONVERTER);
        // Prevent going below zero
        this.valueProperty().addListener((o, oldValue, newValue) -> { if (newValue < 0) { this.setValue(0.0); }});
        this.setValue(value);
        this.factor = factor;
    }

    @Override public void decrement(int steps) { this.setValue(this.getValue() / Math.pow(factor, steps)); }
    @Override public void increment(int steps) { this.setValue(this.getValue() * Math.pow(factor, steps)); }

    private static final Converter CONVERTER = new Converter();
    private static class Converter extends StringConverter<Double> {
        @Override public String toString(Double value) {
            if (value == null) { return ""; }
            else { return String.format("%2.0f%%", 100.0 * value); }
        }

        @Override public Double fromString(String value) {
            if (value == null) { return null; }

            value = value
                .replaceFirst("(%| )+$", "")
                .replaceFirst("^( )*", "");

            if (value.isEmpty()) { return null; }

            return Double.parseDouble(value) / 100.0;
        }
    }
}
