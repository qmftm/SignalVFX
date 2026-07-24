package com.signalvfx.editor;

import com.signalvfx.editor.bbmodel.BbGeometry;
import com.signalvfx.model.DamagePoint;
import com.signalvfx.model.Shape;
import com.signalvfx.model.Skill;
import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A rough 3D viewport that shows an imported BlockBench model together with the
 * skill's damage-point volumes, so points can be placed relative to the model —
 * and, when the model has animations, played back so damage timing can be lined
 * up with a moment in the animation. Orbit with left-drag, zoom with scroll.
 *
 * <p>Rendering and animation are approximate (cube geometry, numeric keyframes,
 * no textures/Molang/IK); the authoritative look is BetterModel in-game. All
 * content lives in Minecraft block space (16 BlockBench units per block, Y up).
 */
final class Preview3DPane extends BorderPane {

    private static final double UNITS_PER_BLOCK = 16.0;
    private static final double TICKS_PER_SECOND = 20.0;

    /** Mutable per-bone transforms updated by the animation sampler. */
    private static final class BoneHandle {
        final Translate translate = new Translate();
        final Rotate rotX;
        final Rotate rotY;
        final Rotate rotZ;
        final Scale scale;

        BoneHandle(double[] pivot) {
            rotX = new Rotate(0, pivot[0], pivot[1], pivot[2], Rotate.X_AXIS);
            rotY = new Rotate(0, pivot[0], pivot[1], pivot[2], Rotate.Y_AXIS);
            rotZ = new Rotate(0, pivot[0], pivot[1], pivot[2], Rotate.Z_AXIS);
            scale = new Scale(1, 1, 1, pivot[0], pivot[1], pivot[2]);
        }

        void reset() {
            translate.setX(0);
            translate.setY(0);
            translate.setZ(0);
            rotX.setAngle(0);
            rotY.setAngle(0);
            rotZ.setAngle(0);
            scale.setX(1);
            scale.setY(1);
            scale.setZ(1);
        }
    }

    private final Pane viewportHolder = new Pane();
    private final Label status = new Label("No model loaded. Import a .bbmodel to preview.");

    private final Group modelGroup = new Group();
    private final Group damageGroup = new Group();
    private final Group world = new Group(modelGroup, damageGroup);
    private final Group pivot = new Group(world);

    private final Rotate rotateX = new Rotate(-20, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-30, Rotate.Y_AXIS);
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    // Animation playback controls.
    private final ComboBox<String> animSelect = new ComboBox<>();
    private final ToggleButton playButton = new ToggleButton("▶ Play");
    private final Slider timeSlider = new Slider(0, 1, 0);
    private final Label timeLabel = new Label("tick 0 / 0");

    private final Map<String, BoneHandle> boneHandles = new HashMap<>();

    private SubScene subScene;
    private Skill skill;
    private BbGeometry geometry;
    private BbGeometry.Animation currentAnim;
    private double currentTime;          // seconds
    private long lastNanos;
    private boolean sliderDriven;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastNanos == 0) {
                lastNanos = now;
                return;
            }
            double dt = (now - lastNanos) / 1_000_000_000.0;
            lastNanos = now;
            advance(dt);
        }
    };

    private double distance = 30;
    private double anchorX;
    private double anchorY;
    private double anchorRx;
    private double anchorRy;

    Preview3DPane(Skill skill) {
        this.skill = skill;
        setPadding(new Insets(8));

        setTop(buildToolbar());

        world.getTransforms().add(new Scale(1, -1, 1)); // Y-up
        modelGroup.getTransforms().add(new Scale(1 / UNITS_PER_BLOCK, 1 / UNITS_PER_BLOCK, 1 / UNITS_PER_BLOCK));
        pivot.getTransforms().addAll(rotateY, rotateX);

        Group sceneRoot = new Group(pivot, buildLights(), buildAxes());
        subScene = new SubScene(sceneRoot, 600, 460, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#15171b"));
        camera.setNearClip(0.05);
        camera.setFarClip(2000);
        camera.setFieldOfView(45);
        subScene.setCamera(camera);
        updateCamera();

        subScene.widthProperty().bind(viewportHolder.widthProperty());
        subScene.heightProperty().bind(viewportHolder.heightProperty());
        viewportHolder.getChildren().add(subScene);
        setCenter(viewportHolder);

        installControls();
    }

    private VBox buildToolbar() {
        Button load = new Button("Load .bbmodel for preview…");
        load.setOnAction(e -> chooseModel());
        Button fit = new Button("Reset view");
        fit.setOnAction(e -> resetView());
        HBox row1 = new HBox(8, load, fit, status);
        row1.setAlignment(Pos.CENTER_LEFT);

        animSelect.setPrefWidth(180);
        animSelect.valueProperty().addListener((o, a, b) -> selectAnimation(b));
        playButton.setOnAction(e -> togglePlay(playButton.isSelected()));
        timeSlider.valueProperty().addListener((o, a, b) -> {
            if (!sliderDriven) {
                scrubTo(b.doubleValue());
            }
        });
        HBox.setHgrow(timeSlider, Priority.ALWAYS);
        timeSlider.setMaxWidth(Double.MAX_VALUE);
        HBox row2 = new HBox(8, new Label("Animation:"), animSelect, playButton, timeSlider, timeLabel);
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.setPadding(new Insets(6, 0, 6, 0));
        setAnimControlsEnabled(false);

        VBox top = new VBox(4, row1, new Separator(), row2);
        top.setPadding(new Insets(0, 0, 8, 0));
        return top;
    }

    void bind(Skill skill) {
        this.skill = skill;
        refreshDamage();
    }

    void setGeometry(BbGeometry geometry) {
        this.geometry = geometry;
        rebuildModel();
        populateAnimations();
        resetView();
    }

    void refreshDamage() {
        damageGroup.getChildren().clear();
        if (skill == null) {
            return;
        }
        PhongMaterial mat = new PhongMaterial(Color.web("#ff5a5a", 0.28));
        for (DamagePoint dp : skill.getDamagePoints()) {
            javafx.scene.Node node = damageNode(dp, mat);
            if (node != null) {
                node.setTranslateX(dp.getOffset().getX());
                node.setTranslateY(dp.getOffset().getY());
                node.setTranslateZ(dp.getOffset().getZ());
                damageGroup.getChildren().add(node);
            }
        }
    }

    // ---- model ---------------------------------------------------------

    private void rebuildModel() {
        modelGroup.getChildren().clear();
        boneHandles.clear();
        if (geometry == null) {
            status.setText("No model loaded.");
            return;
        }
        modelGroup.getChildren().add(buildBone(geometry.getRoot()));
        status.setText("Model: " + geometry.getCubeCount() + " cube(s), "
                + geometry.getAnimations().size() + " animation(s). Left-drag: orbit · scroll: zoom.");
    }

    private Group buildBone(BbGeometry.Bone bone) {
        Group g = new Group();
        BoneHandle handle = new BoneHandle(bone.origin);
        if (!bone.uuid.isEmpty()) {
            boneHandles.put(bone.uuid, handle);
        }
        // Order (outermost first): anim translate, rest rotation, anim rotation, anim scale.
        g.getTransforms().add(handle.translate);
        if (bone.rotation[0] != 0 || bone.rotation[1] != 0 || bone.rotation[2] != 0) {
            g.getTransforms().addAll(
                    new Rotate(bone.rotation[2], bone.origin[0], bone.origin[1], bone.origin[2], Rotate.Z_AXIS),
                    new Rotate(bone.rotation[1], bone.origin[0], bone.origin[1], bone.origin[2], Rotate.Y_AXIS),
                    new Rotate(bone.rotation[0], bone.origin[0], bone.origin[1], bone.origin[2], Rotate.X_AXIS));
        }
        g.getTransforms().addAll(handle.rotZ, handle.rotY, handle.rotX, handle.scale);

        PhongMaterial mat = new PhongMaterial(Color.web("#b9c0cc"));
        mat.setSpecularColor(Color.web("#2c3038"));
        for (BbGeometry.Cube c : bone.cubes) {
            g.getChildren().add(buildCube(c, mat));
        }
        for (BbGeometry.Bone child : bone.children) {
            g.getChildren().add(buildBone(child));
        }
        return g;
    }

    private Group buildCube(BbGeometry.Cube c, PhongMaterial mat) {
        double sx = Math.abs(c.to[0] - c.from[0]) + 2 * c.inflate;
        double sy = Math.abs(c.to[1] - c.from[1]) + 2 * c.inflate;
        double sz = Math.abs(c.to[2] - c.from[2]) + 2 * c.inflate;
        Box box = new Box(Math.max(sx, 0.001), Math.max(sy, 0.001), Math.max(sz, 0.001));
        box.setMaterial(mat);
        box.setTranslateX((c.from[0] + c.to[0]) / 2.0);
        box.setTranslateY((c.from[1] + c.to[1]) / 2.0);
        box.setTranslateZ((c.from[2] + c.to[2]) / 2.0);
        Group holder = new Group(box);
        if (c.rotation[0] != 0 || c.rotation[1] != 0 || c.rotation[2] != 0) {
            holder.getTransforms().addAll(
                    new Rotate(c.rotation[2], c.origin[0], c.origin[1], c.origin[2], Rotate.Z_AXIS),
                    new Rotate(c.rotation[1], c.origin[0], c.origin[1], c.origin[2], Rotate.Y_AXIS),
                    new Rotate(c.rotation[0], c.origin[0], c.origin[1], c.origin[2], Rotate.X_AXIS));
        }
        return holder;
    }

    private javafx.scene.Node damageNode(DamagePoint dp, PhongMaterial mat) {
        Shape shape = dp.getShape();
        return switch (shape.getType()) {
            case SPHERE -> {
                Sphere s = new Sphere(Math.max(shape.getRadius(), 0.05));
                s.setMaterial(mat);
                yield s;
            }
            case BOX -> {
                Box b = new Box(
                        Math.max(shape.getHalfExtents().getX() * 2, 0.05),
                        Math.max(shape.getHalfExtents().getY() * 2, 0.05),
                        Math.max(shape.getHalfExtents().getZ() * 2, 0.05));
                b.setMaterial(mat);
                yield b;
            }
            case CONE -> {
                Sphere s = new Sphere(Math.max(shape.getLength(), 0.05));
                s.setMaterial(mat);
                s.setDrawMode(DrawMode.LINE);
                yield s;
            }
        };
    }

    // ---- animation -----------------------------------------------------

    private void populateAnimations() {
        stopPlayback();
        animSelect.getItems().clear();
        animSelect.getItems().add("(rest pose)");
        if (geometry != null) {
            for (BbGeometry.Animation a : geometry.getAnimations()) {
                animSelect.getItems().add(a.name);
            }
        }
        animSelect.getSelectionModel().selectFirst();
        boolean hasAnims = geometry != null && !geometry.getAnimations().isEmpty();
        setAnimControlsEnabled(hasAnims);
    }

    private void selectAnimation(String name) {
        stopPlayback();
        currentAnim = null;
        currentTime = 0;
        if (geometry != null && name != null) {
            for (BbGeometry.Animation a : geometry.getAnimations()) {
                if (a.name.equals(name)) {
                    currentAnim = a;
                    break;
                }
            }
        }
        double length = currentAnim == null ? 1 : Math.max(currentAnim.lengthSeconds, 0.0001);
        timeSlider.setMax(length);
        timeSlider.setDisable(currentAnim == null);
        playButton.setDisable(currentAnim == null);
        applyPose();
    }

    private void togglePlay(boolean play) {
        if (currentAnim == null) {
            playButton.setSelected(false);
            return;
        }
        if (play) {
            playButton.setText("❚❚ Pause");
            lastNanos = 0;
            timer.start();
        } else {
            stopPlayback();
        }
    }

    private void stopPlayback() {
        timer.stop();
        playButton.setSelected(false);
        playButton.setText("▶ Play");
    }

    private void advance(double dt) {
        if (currentAnim == null) {
            return;
        }
        double length = Math.max(currentAnim.lengthSeconds, 0.0001);
        currentTime += dt;
        if (currentTime > length) {
            if (currentAnim.loop) {
                currentTime %= length;
            } else {
                currentTime = length;
                stopPlayback();
            }
        }
        applyPose();
    }

    private void scrubTo(double seconds) {
        if (currentAnim == null) {
            return;
        }
        currentTime = seconds;
        applyPose();
    }

    /** Samples the current animation at {@link #currentTime} and updates the bones + UI. */
    private void applyPose() {
        for (BoneHandle h : boneHandles.values()) {
            h.reset();
        }
        if (currentAnim != null) {
            for (Map.Entry<String, BbGeometry.BoneTrack> e : currentAnim.tracks.entrySet()) {
                BoneHandle h = boneHandles.get(e.getKey());
                if (h == null) {
                    continue;
                }
                BbGeometry.BoneTrack track = e.getValue();
                double[] pos = sample(track.position, currentTime, 0);
                double[] rot = sample(track.rotation, currentTime, 0);
                double[] scl = sample(track.scale, currentTime, 1);
                h.translate.setX(pos[0]);
                h.translate.setY(pos[1]);
                h.translate.setZ(pos[2]);
                h.rotX.setAngle(rot[0]);
                h.rotY.setAngle(rot[1]);
                h.rotZ.setAngle(rot[2]);
                h.scale.setX(scl[0]);
                h.scale.setY(scl[1]);
                h.scale.setZ(scl[2]);
            }
        }
        // Reflect time in the slider/label without re-triggering the listener.
        sliderDriven = true;
        timeSlider.setValue(currentTime);
        sliderDriven = false;
        int tick = (int) Math.round(currentTime * TICKS_PER_SECOND);
        int total = currentAnim == null ? 0 : (int) Math.round(currentAnim.lengthSeconds * TICKS_PER_SECOND);
        timeLabel.setText("tick " + tick + " / " + total);
    }

    private static double[] sample(List<BbGeometry.Keyframe> kfs, double t, double def) {
        if (kfs.isEmpty()) {
            return new double[]{def, def, def};
        }
        if (t <= kfs.get(0).time) {
            return kfs.get(0).value.clone();
        }
        BbGeometry.Keyframe last = kfs.get(kfs.size() - 1);
        if (t >= last.time) {
            return last.value.clone();
        }
        for (int i = 0; i < kfs.size() - 1; i++) {
            BbGeometry.Keyframe a = kfs.get(i);
            BbGeometry.Keyframe b = kfs.get(i + 1);
            if (t >= a.time && t <= b.time) {
                if (a.step || b.time == a.time) {
                    return a.value.clone();
                }
                double f = (t - a.time) / (b.time - a.time);
                return new double[]{
                        a.value[0] + (b.value[0] - a.value[0]) * f,
                        a.value[1] + (b.value[1] - a.value[1]) * f,
                        a.value[2] + (b.value[2] - a.value[2]) * f};
            }
        }
        return last.value.clone();
    }

    private void setAnimControlsEnabled(boolean enabled) {
        animSelect.setDisable(!enabled);
        playButton.setDisable(!enabled);
        timeSlider.setDisable(!enabled);
    }

    // ---- camera & controls --------------------------------------------

    private void installControls() {
        subScene.setOnMousePressed(e -> {
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
            anchorRx = rotateX.getAngle();
            anchorRy = rotateY.getAngle();
        });
        subScene.setOnMouseDragged(e -> {
            rotateY.setAngle(anchorRy + (e.getSceneX() - anchorX) * 0.4);
            rotateX.setAngle(clamp(anchorRx - (e.getSceneY() - anchorY) * 0.4, -89, 89));
        });
        subScene.setOnScroll(e -> {
            distance = clamp(distance - e.getDeltaY() * 0.05, 2, 400);
            updateCamera();
        });
    }

    private void updateCamera() {
        camera.setTranslateX(0);
        camera.setTranslateY(0);
        camera.setTranslateZ(-distance);
    }

    private void resetView() {
        centreContent();
        rotateX.setAngle(-20);
        rotateY.setAngle(-30);
        double span = contentSpan();
        distance = Math.max(span * 1.8, 8);
        updateCamera();
    }

    private void centreContent() {
        world.setTranslateX(0);
        world.setTranslateY(0);
        world.setTranslateZ(0);
        Bounds b = world.getBoundsInParent();
        if (b.getWidth() == 0 && b.getHeight() == 0 && b.getDepth() == 0) {
            return;
        }
        world.setTranslateX(-(b.getMinX() + b.getWidth() / 2));
        world.setTranslateY(-(b.getMinY() + b.getHeight() / 2));
        world.setTranslateZ(-(b.getMinZ() + b.getDepth() / 2));
    }

    private double contentSpan() {
        Bounds b = world.getBoundsInParent();
        return Math.max(b.getWidth(), Math.max(b.getHeight(), b.getDepth()));
    }

    private Group buildLights() {
        javafx.scene.AmbientLight ambient = new javafx.scene.AmbientLight(Color.web("#6a6f78"));
        javafx.scene.PointLight key = new javafx.scene.PointLight(Color.web("#ffffff"));
        key.setTranslateX(-60);
        key.setTranslateY(-120);
        key.setTranslateZ(-90);
        return new Group(ambient, key);
    }

    private Group buildAxes() {
        Group axes = new Group();
        axes.getChildren().addAll(
                axisRod(6, 0.06, 0.06, "#c0392b", new Point3D(3, 0, 0)),
                axisRod(0.06, 6, 0.06, "#27ae60", new Point3D(0, -3, 0)),
                axisRod(0.06, 0.06, 6, "#2980b9", new Point3D(0, 0, 3)));
        return axes;
    }

    private Box axisRod(double sx, double sy, double sz, String color, Point3D at) {
        Box rod = new Box(sx, sy, sz);
        rod.setMaterial(new PhongMaterial(Color.web(color)));
        rod.setTranslateX(at.getX());
        rod.setTranslateY(at.getY());
        rod.setTranslateZ(at.getZ());
        return rod;
    }

    private void chooseModel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load BlockBench model for preview");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BlockBench model", "*.bbmodel"));
        File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            setGeometry(BbGeometry.read(file.toPath()));
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Load failed");
            a.setHeaderText("Could not read .bbmodel geometry");
            a.setContentText(ex.getMessage());
            a.showAndWait();
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
