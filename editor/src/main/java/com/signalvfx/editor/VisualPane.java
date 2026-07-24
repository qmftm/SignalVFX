package com.signalvfx.editor;

import com.signalvfx.model.Skill;
import com.signalvfx.model.Vec3;
import com.signalvfx.model.visual.Attach;
import com.signalvfx.model.visual.Billboard;
import com.signalvfx.model.visual.DisplayEntityVisual;
import com.signalvfx.model.visual.DisplayKind;
import com.signalvfx.model.visual.ResourcePackVisual;
import com.signalvfx.model.visual.Visual;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Editor panel for the skill's visual. The top toggle chooses between a
 * resource-pack visual (primary path) and a display-entity visual (pack-free,
 * billboard-as-custom-particle). Switching preserves the shared fields
 * (anchor, offset, duration, sound).
 */
final class VisualPane extends ScrollPane {

    private final Runnable onChange;
    private final VBox root = new VBox(8);
    private Skill skill;

    VisualPane(Runnable onChange) {
        this.onChange = onChange;
        setFitToWidth(true);
        root.setPadding(new Insets(10));
        setContent(root);
    }

    void bind(Skill skill) {
        this.skill = skill;
        rebuild();
    }

    private void rebuild() {
        root.getChildren().clear();

        Visual visual = skill.getVisual();

        ToggleGroup group = new ToggleGroup();
        RadioButton rp = new RadioButton("Resource Pack");
        RadioButton de = new RadioButton("Display Entity");
        rp.setToggleGroup(group);
        de.setToggleGroup(group);
        boolean isRp = visual instanceof ResourcePackVisual;
        rp.setSelected(isRp);
        de.setSelected(!isRp);

        rp.setOnAction(e -> switchTo(true));
        de.setOnAction(e -> switchTo(false));

        Label kind = new Label("Visual kind:");
        kind.setStyle("-fx-font-weight: bold;");
        HBox toggle = new HBox(12, kind, rp, de);
        toggle.setPadding(new Insets(0, 0, 6, 0));
        root.getChildren().add(toggle);

        root.getChildren().add(commonForm(visual));

        if (visual instanceof ResourcePackVisual v) {
            root.getChildren().add(resourcePackForm(v));
        } else if (visual instanceof DisplayEntityVisual v) {
            root.getChildren().add(displayForm(v));
        }
    }

    private void switchTo(boolean toResourcePack) {
        Visual current = skill.getVisual();
        boolean currentIsRp = current instanceof ResourcePackVisual;
        if (toResourcePack == currentIsRp) {
            return;
        }
        Visual next = toResourcePack ? new ResourcePackVisual() : new DisplayEntityVisual();
        // Carry over the shared fields so switching kinds is non-destructive.
        next.setAttach(current.getAttach());
        next.setOffset(current.getOffset());
        next.setDurationTicks(current.getDurationTicks());
        next.setSoundKey(current.getSoundKey());
        next.setSoundVolume(current.getSoundVolume());
        next.setSoundPitch(current.getSoundPitch());
        skill.setVisual(next);
        onChange.run();
        rebuild();
    }

    private GridPane commonForm(Visual v) {
        GridPane grid = Fx.form();
        int r = 0;
        grid.add(header("Placement & timing"), 0, r++, 2, 1);
        Fx.row(grid, r++, "Anchor", Fx.enumCombo(Attach.class, v.getAttach(), val -> {
            v.setAttach(val);
            onChange.run();
        }));
        r = vec3Rows(grid, r, "Offset", v.getOffset(), val -> {
            v.setOffset(val);
            onChange.run();
        });
        Fx.row(grid, r++, "Duration (ticks)", Fx.intField(v.getDurationTicks(), val -> {
            v.setDurationTicks(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Sound key", Fx.text(v.getSoundKey(), val -> {
            v.setSoundKey(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Sound volume", Fx.doubleField(v.getSoundVolume(), val -> {
            v.setSoundVolume((float) val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Sound pitch", Fx.doubleField(v.getSoundPitch(), val -> {
            v.setSoundPitch((float) val);
            onChange.run();
        }));
        return grid;
    }

    private GridPane resourcePackForm(ResourcePackVisual v) {
        GridPane grid = Fx.form();
        int r = 0;
        grid.add(header("Resource Pack model (hosted by the plugin)"), 0, r++, 2, 1);
        Fx.row(grid, r++, "Base item", Fx.text(v.getMaterial(), val -> {
            v.setMaterial(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Item model key", Fx.text(v.getItemModel(), val -> {
            v.setItemModel(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Custom model data", Fx.intField(v.getCustomModelData(), val -> {
            v.setCustomModelData(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Animation state", Fx.text(v.getAnimation(), val -> {
            v.setAnimation(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Scale", Fx.doubleField(v.getScale(), val -> {
            v.setScale(val);
            onChange.run();
        }));
        return grid;
    }

    private GridPane displayForm(DisplayEntityVisual v) {
        GridPane grid = Fx.form();
        int r = 0;
        grid.add(header("Display entity (billboard = custom-particle look)"), 0, r++, 2, 1);
        Fx.row(grid, r++, "Display kind", Fx.enumCombo(DisplayKind.class, v.getDisplayKind(), val -> {
            v.setDisplayKind(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Item", Fx.text(v.getItem(), val -> {
            v.setItem(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Block", Fx.text(v.getBlock(), val -> {
            v.setBlock(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Text", Fx.text(v.getText(), val -> {
            v.setText(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Custom model data", Fx.intField(v.getCustomModelData(), val -> {
            v.setCustomModelData(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Billboard", Fx.enumCombo(Billboard.class, v.getBillboard(), val -> {
            v.setBillboard(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Block light (-1 off)", Fx.intField(v.getBlockLight(), val -> {
            v.setBlockLight(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Sky light (-1 off)", Fx.intField(v.getSkyLight(), val -> {
            v.setSkyLight(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Glowing", Fx.check("", v.isGlowing(), val -> {
            v.setGlowing(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Glow color (#RRGGBB)", Fx.text(v.getGlowColor(), val -> {
            v.setGlowColor(val);
            onChange.run();
        }));

        grid.add(header("Base transform"), 0, r++, 2, 1);
        r = vec3Rows(grid, r, "Translation", v.getBaseTransform().getTranslation(),
                val -> {
                    v.getBaseTransform().setTranslation(val);
                    onChange.run();
                });
        r = vec3Rows(grid, r, "Scale", v.getBaseTransform().getScale(),
                val -> {
                    v.getBaseTransform().setScale(val);
                    onChange.run();
                });
        r = vec3Rows(grid, r, "Left rot (deg)", v.getBaseTransform().getLeftRotation(),
                val -> {
                    v.getBaseTransform().setLeftRotation(val);
                    onChange.run();
                });
        r = vec3Rows(grid, r, "Right rot (deg)", v.getBaseTransform().getRightRotation(),
                val -> {
                    v.getBaseTransform().setRightRotation(val);
                    onChange.run();
                });

        Label note = new Label(
                "Keyframe animation: " + v.getKeyframes().size()
                        + " keyframe(s). (Timeline editing planned; edit JSON for now.)");
        note.setStyle("-fx-text-fill: #888; -fx-padding: 6 0 0 0;");
        grid.add(note, 0, r, 2, 1);
        return grid;
    }

    /** Emits three columns of x/y/z fields sharing one label; returns the next row. */
    private int vec3Rows(GridPane grid, int r, String label, Vec3 value,
                         java.util.function.Consumer<Vec3> setter) {
        final Vec3[] holder = {value};
        HBox box = new HBox(6);
        box.getChildren().addAll(
                Fx.doubleField(value.getX(), x -> {
                    holder[0] = holder[0].withX(x);
                    setter.accept(holder[0]);
                }),
                Fx.doubleField(value.getY(), y -> {
                    holder[0] = holder[0].withY(y);
                    setter.accept(holder[0]);
                }),
                Fx.doubleField(value.getZ(), z -> {
                    holder[0] = holder[0].withZ(z);
                    setter.accept(holder[0]);
                }));
        HBox.setHgrow(box.getChildren().get(0), javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(box.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(box.getChildren().get(2), javafx.scene.layout.Priority.ALWAYS);
        Fx.row(grid, r, label + " (x/y/z)", box);
        return r + 1;
    }

    private Label header(String title) {
        Label l = new Label(title);
        l.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 2 0; -fx-font-size: 13px;");
        return l;
    }
}
