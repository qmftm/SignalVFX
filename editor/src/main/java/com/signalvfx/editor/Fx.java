package com.signalvfx.editor;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Small helpers for building the hand-wired forms used throughout the editor.
 * Every builder takes an initial value and a setter callback that writes back
 * into the underlying model object as the user types.
 */
final class Fx {

    private Fx() {
    }

    static GridPane form() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.setPadding(new Insets(10));
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(140);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setFillWidth(true);
        grid.getColumnConstraints().addAll(labels, fields);
        return grid;
    }

    static void row(GridPane grid, int r, String label, Region control) {
        Label l = new Label(label);
        grid.add(l, 0, r);
        grid.add(control, 1, r);
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
    }

    static TextField text(String value, Consumer<String> setter) {
        TextField tf = new TextField(value == null ? "" : value);
        tf.textProperty().addListener((obs, old, val) -> setter.accept(val));
        return tf;
    }

    static TextField intField(int value, IntConsumer setter) {
        TextField tf = new TextField(Integer.toString(value));
        tf.textProperty().addListener((obs, old, val) -> {
            try {
                setter.accept(val.isBlank() ? 0 : Integer.parseInt(val.trim()));
                tf.setStyle("");
            } catch (NumberFormatException ex) {
                tf.setStyle("-fx-border-color: #cc4444;");
            }
        });
        return tf;
    }

    static TextField doubleField(double value, DoubleConsumer setter) {
        TextField tf = new TextField(trim(value));
        tf.textProperty().addListener((obs, old, val) -> {
            try {
                setter.accept(val.isBlank() ? 0 : Double.parseDouble(val.trim()));
                tf.setStyle("");
            } catch (NumberFormatException ex) {
                tf.setStyle("-fx-border-color: #cc4444;");
            }
        });
        return tf;
    }

    static <E extends Enum<E>> ComboBox<E> enumCombo(Class<E> type, E value, Consumer<E> setter) {
        ComboBox<E> combo = new ComboBox<>();
        combo.getItems().addAll(type.getEnumConstants());
        combo.setValue(value);
        combo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                setter.accept(val);
            }
        });
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    static CheckBox check(String label, boolean value, Consumer<Boolean> setter) {
        CheckBox cb = new CheckBox(label);
        cb.setSelected(value);
        cb.selectedProperty().addListener((obs, old, val) -> setter.accept(val));
        return cb;
    }

    /** Formats a double without a trailing ".0" for whole numbers. */
    static String trim(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
