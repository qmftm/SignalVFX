package com.signalvfx.editor;

import com.signalvfx.editor.bbmodel.BbGeometry;
import com.signalvfx.editor.bbmodel.BbModel;
import com.signalvfx.model.Skill;
import com.signalvfx.model.Vec3;
import com.signalvfx.model.visual.Attach;
import com.signalvfx.model.visual.BetterModelVisual;
import com.signalvfx.model.visual.Billboard;
import com.signalvfx.model.visual.DisplayEntityVisual;
import com.signalvfx.model.visual.DisplayKind;
import com.signalvfx.model.visual.ResourcePackVisual;
import com.signalvfx.model.visual.Visual;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Editor panel for the skill's visual. The top toggle chooses between a
 * BetterModel visual (recommended; can import a .bbmodel to fill in the model
 * id and animation list), a resource-pack visual, and a display-entity visual
 * (pack-free, billboard-as-custom-particle). Switching preserves the shared
 * fields (anchor, offset, duration, sound).
 */
final class VisualPane extends ScrollPane {

    private final Runnable onChange;
    private final Consumer<BbGeometry> onGeometryImported;
    private final VBox root = new VBox(8);
    private Skill skill;

    /** Animation names discovered from the last imported .bbmodel, for the dropdown. */
    private List<String> importedAnimations = new ArrayList<>();

    VisualPane(Runnable onChange, Consumer<BbGeometry> onGeometryImported) {
        this.onChange = onChange;
        this.onGeometryImported = onGeometryImported;
        setFitToWidth(true);
        root.setPadding(new Insets(10));
        setContent(root);
    }

    void bind(Skill skill) {
        this.skill = skill;
        importedAnimations = new ArrayList<>();
        rebuild();
    }

    private void rebuild() {
        root.getChildren().clear();

        Visual visual = skill.getVisual();

        ToggleGroup group = new ToggleGroup();
        RadioButton bm = new RadioButton("BetterModel");
        RadioButton rp = new RadioButton("Resource Pack");
        RadioButton de = new RadioButton("Display Entity");
        bm.setToggleGroup(group);
        rp.setToggleGroup(group);
        de.setToggleGroup(group);
        bm.setSelected(visual instanceof BetterModelVisual);
        rp.setSelected(visual instanceof ResourcePackVisual);
        de.setSelected(visual instanceof DisplayEntityVisual);

        bm.setOnAction(e -> switchTo(VisualKind.BETTER_MODEL));
        rp.setOnAction(e -> switchTo(VisualKind.RESOURCE_PACK));
        de.setOnAction(e -> switchTo(VisualKind.DISPLAY_ENTITY));

        Label kind = new Label("Visual kind:");
        kind.setStyle("-fx-font-weight: bold;");
        HBox toggle = new HBox(12, kind, bm, rp, de);
        toggle.setPadding(new Insets(0, 0, 6, 0));
        root.getChildren().add(toggle);

        root.getChildren().add(commonForm(visual));

        if (visual instanceof BetterModelVisual v) {
            root.getChildren().add(betterModelForm(v));
        } else if (visual instanceof ResourcePackVisual v) {
            root.getChildren().add(resourcePackForm(v));
        } else if (visual instanceof DisplayEntityVisual v) {
            root.getChildren().add(displayForm(v));
        }
    }

    private enum VisualKind {
        BETTER_MODEL, RESOURCE_PACK, DISPLAY_ENTITY
    }

    private void switchTo(VisualKind target) {
        Visual current = skill.getVisual();
        VisualKind currentKind = current instanceof BetterModelVisual ? VisualKind.BETTER_MODEL
                : current instanceof ResourcePackVisual ? VisualKind.RESOURCE_PACK
                : VisualKind.DISPLAY_ENTITY;
        if (target == currentKind) {
            return;
        }
        Visual next = switch (target) {
            case BETTER_MODEL -> new BetterModelVisual();
            case RESOURCE_PACK -> new ResourcePackVisual();
            case DISPLAY_ENTITY -> new DisplayEntityVisual();
        };
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

    private GridPane betterModelForm(BetterModelVisual v) {
        GridPane grid = Fx.form();
        int r = 0;
        grid.add(header("BetterModel (server-side BlockBench model + animation)"), 0, r++, 2, 1);

        Button importBtn = new Button("Import .bbmodel…");
        importBtn.setOnAction(e -> importBbModel(v));
        Fx.row(grid, r++, "BlockBench file", importBtn);

        Fx.row(grid, r++, "Model id", Fx.text(v.getModelId(), val -> {
            v.setModelId(val);
            onChange.run();
        }));

        // Editable combo: pick a discovered animation, or type one by hand.
        ComboBox<String> anim = new ComboBox<>();
        anim.setEditable(true);
        anim.getItems().addAll(importedAnimations);
        anim.setValue(v.getAnimation());
        anim.setMaxWidth(Double.MAX_VALUE);
        anim.valueProperty().addListener((o, a, b) -> {
            v.setAnimation(b == null ? "" : b);
            onChange.run();
        });
        anim.getEditor().textProperty().addListener((o, a, b) -> {
            v.setAnimation(b == null ? "" : b);
            onChange.run();
        });
        Fx.row(grid, r++, "Animation", anim);

        if (!importedAnimations.isEmpty()) {
            Label found = new Label("Found " + importedAnimations.size() + " animation(s): "
                    + String.join(", ", importedAnimations));
            found.setStyle("-fx-text-fill: #6a8; -fx-padding: 0 0 4 0;");
            found.setWrapText(true);
            grid.add(found, 0, r++, 2, 1);
        }

        Fx.row(grid, r++, "Animation speed", Fx.doubleField(v.getAnimationSpeed(), val -> {
            v.setAnimationSpeed(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Loop", Fx.check("", v.isLoop(), val -> {
            v.setLoop(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Scale", Fx.doubleField(v.getScale(), val -> {
            v.setScale(val);
            onChange.run();
        }));
        Fx.row(grid, r++, "Use model hit-box", Fx.check("", v.isUseModelHitbox(), val -> {
            v.setUseModelHitbox(val);
            onChange.run();
        }));
        Label note = new Label(
                "Requires the BetterModel plugin (soft-depend). Model & animation are\n"
                        + "authored in BlockBench; SignalVFX only references them by name.");
        note.setStyle("-fx-text-fill: #888; -fx-padding: 6 0 0 0;");
        grid.add(note, 0, r, 2, 1);
        return grid;
    }

    private void importBbModel(BetterModelVisual v) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import BlockBench model");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("BlockBench model", "*.bbmodel"));
        File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            BbModel model = BbModel.read(file.toPath());
            v.setModelId(model.getModelId());
            importedAnimations = model.getAnimations();
            if (v.getAnimation().isBlank() && !importedAnimations.isEmpty()) {
                v.setAnimation(importedAnimations.get(0));
            }
            // Feed the geometry to the 3D preview so points can be placed on the model.
            onGeometryImported.accept(BbGeometry.read(file.toPath()));
            onChange.run();
            rebuild();
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Import failed");
            a.setHeaderText("Could not read .bbmodel");
            a.setContentText(ex.getMessage());
            a.showAndWait();
        }
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
