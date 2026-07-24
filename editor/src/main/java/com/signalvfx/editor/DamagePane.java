package com.signalvfx.editor;

import com.signalvfx.model.DamagePoint;
import com.signalvfx.model.Origin;
import com.signalvfx.model.Shape;
import com.signalvfx.model.ShapeType;
import com.signalvfx.model.Skill;
import com.signalvfx.model.TargetFilter;
import com.signalvfx.model.Vec3;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Editor panel for damage points. A top-down (X/Z) canvas shows the caster at
 * the centre and each damage point as a circle whose radius is its planar
 * range; points can be dragged to reposition. The right side lists points and
 * exposes a property form for the selected one.
 */
final class DamagePane extends HBox {

    private static final double PIXELS_PER_BLOCK = 12.0;

    private final Runnable onChange;
    private final Canvas canvas = new Canvas(360, 360);
    private final ListView<DamagePoint> list = new ListView<>();
    private final VBox propertyBox = new VBox(6);

    private Skill skill;
    private DamagePoint selected;
    private DamagePoint dragging;
    private java.util.function.Consumer<DamagePoint> onSelectionChanged;

    DamagePane(Runnable onChange) {
        this.onChange = onChange;
        setSpacing(10);
        setPadding(new Insets(10));

        list.setPrefWidth(180);
        list.setCellFactory(v -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(DamagePoint item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : item.getLabel() + "  (" + Fx.trim(item.getDamage()) + " dmg)");
            }
        });
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            selected = b;
            rebuildProperties();
            redraw();
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(b);
            }
        });

        Button add = new Button("Add");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(e -> {
            DamagePoint dp = new DamagePoint();
            skill.getDamagePoints().add(dp);
            list.getItems().add(dp);
            list.getSelectionModel().select(dp);
            onChange.run();
            redraw();
        });
        Button remove = new Button("Remove");
        remove.setMaxWidth(Double.MAX_VALUE);
        remove.setOnAction(e -> {
            DamagePoint sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) {
                skill.getDamagePoints().remove(sel);
                list.getItems().remove(sel);
                onChange.run();
                redraw();
            }
        });
        HBox buttons = new HBox(6, add, remove);
        HBox.setHgrow(add, Priority.ALWAYS);
        HBox.setHgrow(remove, Priority.ALWAYS);

        VBox leftCol = new VBox(6, new Label("Damage points"), list, buttons);
        VBox.setVgrow(list, Priority.ALWAYS);

        Label hint = new Label("Top-down view — caster at centre, forward = down (+Z). Drag points to move.");
        hint.setStyle("-fx-text-fill: #888;");
        canvas.setOnMousePressed(this::onPress);
        canvas.setOnMouseDragged(this::onDrag);
        canvas.setOnMouseReleased(e -> dragging = null);
        VBox centerCol = new VBox(6, hint, canvas);

        ScrollPane propScroll = new ScrollPane(propertyBox);
        propScroll.setFitToWidth(true);
        propScroll.setPrefWidth(300);
        propertyBox.setPadding(new Insets(4));

        getChildren().addAll(leftCol, centerCol, propScroll);
        HBox.setHgrow(propScroll, Priority.ALWAYS);
    }

    void bind(Skill skill) {
        this.skill = skill;
        list.getItems().setAll(skill.getDamagePoints());
        selected = skill.getDamagePoints().isEmpty() ? null : skill.getDamagePoints().get(0);
        if (selected != null) {
            list.getSelectionModel().select(selected);
        }
        rebuildProperties();
        redraw();
    }

    /** Notified whenever the selected damage point changes (may be null). */
    void setOnSelectionChanged(java.util.function.Consumer<DamagePoint> callback) {
        this.onSelectionChanged = callback;
    }

    /** Selects a point from outside (e.g. clicked in the 3D preview). */
    void select(DamagePoint dp) {
        if (dp != null && dp != selected) {
            list.getSelectionModel().select(dp);
        }
    }

    // ---- property form -------------------------------------------------

    private void rebuildProperties() {
        propertyBox.getChildren().clear();
        if (selected == null) {
            propertyBox.getChildren().add(new Label("Select or add a damage point."));
            return;
        }
        DamagePoint dp = selected;
        Shape shape = dp.getShape();

        javafx.scene.layout.GridPane grid = Fx.form();
        int r = 0;
        Fx.row(grid, r++, "Label", Fx.text(dp.getLabel(), v -> {
            dp.setLabel(v);
            list.refresh();
            edited();
        }));
        Fx.row(grid, r++, "Origin", Fx.enumCombo(Origin.class, dp.getOrigin(), v -> {
            dp.setOrigin(v);
            edited();
        }));
        Fx.row(grid, r++, "Offset X", Fx.doubleField(dp.getOffset().getX(), v -> {
            dp.setOffset(dp.getOffset().withX(v));
            edited();
        }));
        Fx.row(grid, r++, "Offset Y", Fx.doubleField(dp.getOffset().getY(), v -> {
            dp.setOffset(dp.getOffset().withY(v));
            edited();
        }));
        Fx.row(grid, r++, "Offset Z", Fx.doubleField(dp.getOffset().getZ(), v -> {
            dp.setOffset(dp.getOffset().withZ(v));
            edited();
        }));
        Fx.row(grid, r++, "Damage", Fx.doubleField(dp.getDamage(), v -> {
            dp.setDamage(v);
            list.refresh();
            edited();
        }));
        Fx.row(grid, r++, "Delay (ticks)", Fx.intField(dp.getDelayTicks(), v -> {
            dp.setDelayTicks(v);
            edited();
        }));
        Fx.row(grid, r++, "Repeat count", Fx.intField(dp.getRepeatCount(), v -> {
            dp.setRepeatCount(v);
            edited();
        }));
        Fx.row(grid, r++, "Repeat interval", Fx.intField(dp.getRepeatIntervalTicks(), v -> {
            dp.setRepeatIntervalTicks(v);
            edited();
        }));
        Fx.row(grid, r++, "Targets", Fx.enumCombo(TargetFilter.class, dp.getTargetFilter(), v -> {
            dp.setTargetFilter(v);
            edited();
        }));
        Fx.row(grid, r++, "Knockback", Fx.doubleField(dp.getKnockback(), v -> {
            dp.setKnockback(v);
            edited();
        }));

        Label shapeHeader = new Label("Range shape");
        shapeHeader.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 2 0;");
        grid.add(shapeHeader, 0, r++, 2, 1);
        Fx.row(grid, r++, "Shape", Fx.enumCombo(ShapeType.class, shape.getType(), v -> {
            shape.setType(v);
            edited();
        }));
        Fx.row(grid, r++, "Radius (sphere)", Fx.doubleField(shape.getRadius(), v -> {
            shape.setRadius(v);
            edited();
        }));
        Fx.row(grid, r++, "Box half-X", Fx.doubleField(shape.getHalfExtents().getX(), v -> {
            shape.setHalfExtents(shape.getHalfExtents().withX(v));
            edited();
        }));
        Fx.row(grid, r++, "Box half-Y", Fx.doubleField(shape.getHalfExtents().getY(), v -> {
            shape.setHalfExtents(shape.getHalfExtents().withY(v));
            edited();
        }));
        Fx.row(grid, r++, "Box half-Z", Fx.doubleField(shape.getHalfExtents().getZ(), v -> {
            shape.setHalfExtents(shape.getHalfExtents().withZ(v));
            edited();
        }));
        Fx.row(grid, r++, "Cone angle (deg)", Fx.doubleField(shape.getAngle(), v -> {
            shape.setAngle(v);
            edited();
        }));
        Fx.row(grid, r++, "Cone length", Fx.doubleField(shape.getLength(), v -> {
            shape.setLength(v);
            edited();
        }));

        propertyBox.getChildren().add(grid);
    }

    private void edited() {
        onChange.run();
        redraw();
    }

    // ---- canvas --------------------------------------------------------

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cx = w / 2;
        double cy = h / 2;

        g.setFill(Color.web("#1e1f22"));
        g.fillRect(0, 0, w, h);

        // grid lines every block
        g.setStroke(Color.web("#2c2e33"));
        g.setLineWidth(1);
        for (double x = cx % PIXELS_PER_BLOCK; x < w; x += PIXELS_PER_BLOCK) {
            g.strokeLine(x, 0, x, h);
        }
        for (double y = cy % PIXELS_PER_BLOCK; y < h; y += PIXELS_PER_BLOCK) {
            g.strokeLine(0, y, w, y);
        }
        // axes
        g.setStroke(Color.web("#45474d"));
        g.strokeLine(cx, 0, cx, h);
        g.strokeLine(0, cy, w, cy);

        // caster + forward arrow (+Z is downward on screen)
        g.setFill(Color.web("#5aa0ff"));
        g.fillOval(cx - 4, cy - 4, 8, 8);
        g.setStroke(Color.web("#5aa0ff"));
        g.strokeLine(cx, cy, cx, cy + PIXELS_PER_BLOCK * 2);

        if (skill != null) {
            for (DamagePoint dp : skill.getDamagePoints()) {
                double px = cx + dp.getOffset().getX() * PIXELS_PER_BLOCK;
                double py = cy + dp.getOffset().getZ() * PIXELS_PER_BLOCK;
                double rr = Math.max(2, dp.getShape().approximatePlanarRadius() * PIXELS_PER_BLOCK);
                boolean sel = dp == selected;
                g.setStroke(sel ? Color.web("#ff6b6b") : Color.web("#c07a3a"));
                g.setLineWidth(sel ? 2 : 1);
                g.strokeOval(px - rr, py - rr, rr * 2, rr * 2);
                g.setFill(sel ? Color.web("#ff6b6b", 0.22) : Color.web("#c07a3a", 0.14));
                g.fillOval(px - rr, py - rr, rr * 2, rr * 2);
                g.setFill(sel ? Color.web("#ff6b6b") : Color.web("#e0a060"));
                g.fillOval(px - 3, py - 3, 6, 6);
            }
        }
    }

    private void onPress(javafx.scene.input.MouseEvent e) {
        if (skill == null) {
            return;
        }
        double cx = canvas.getWidth() / 2;
        double cy = canvas.getHeight() / 2;
        // pick the nearest point within a small pixel radius
        DamagePoint hit = null;
        double best = 12;
        for (DamagePoint dp : skill.getDamagePoints()) {
            double px = cx + dp.getOffset().getX() * PIXELS_PER_BLOCK;
            double py = cy + dp.getOffset().getZ() * PIXELS_PER_BLOCK;
            double d = Math.hypot(px - e.getX(), py - e.getY());
            if (d < best) {
                best = d;
                hit = dp;
            }
        }
        if (hit != null) {
            dragging = hit;
            list.getSelectionModel().select(hit);
        }
    }

    private void onDrag(javafx.scene.input.MouseEvent e) {
        if (dragging == null) {
            return;
        }
        double cx = canvas.getWidth() / 2;
        double cy = canvas.getHeight() / 2;
        double nx = round((e.getX() - cx) / PIXELS_PER_BLOCK);
        double nz = round((e.getY() - cy) / PIXELS_PER_BLOCK);
        dragging.setOffset(new Vec3(nx, dragging.getOffset().getY(), nz));
        rebuildProperties();
        edited();
    }

    private double round(double v) {
        return Math.round(v * 4.0) / 4.0; // snap to quarter-block
    }
}
