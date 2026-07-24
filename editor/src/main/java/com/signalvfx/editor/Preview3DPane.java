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
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
    private final Canvas timeline = new Canvas(400, 40);
    private final Pane timelineHolder = new Pane(timeline);
    private final Label timeLabel = new Label("tick 0 / 0");

    private final Map<String, BoneHandle> boneHandles = new HashMap<>();

    private SubScene subScene;
    private Skill skill;
    private BbGeometry geometry;
    private DamagePoint selectedPoint;
    private Consumer<DamagePoint> onPointPicked;
    private Runnable onEdit;
    private BbGeometry.Animation currentAnim;
    private double currentTime;          // seconds
    private double timelineLength = 1;   // seconds mapped across the timeline
    private long lastNanos;
    private DamagePoint draggingMarker;
    private boolean scrubbing;

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
        Label pickHint = new Label("· click a sphere to select it");
        pickHint.setStyle("-fx-text-fill: #888;");
        HBox row1 = new HBox(8, load, fit, status, pickHint);
        row1.setAlignment(Pos.CENTER_LEFT);

        animSelect.setPrefWidth(180);
        animSelect.valueProperty().addListener((o, a, b) -> selectAnimation(b));
        playButton.setOnAction(e -> togglePlay(playButton.isSelected()));

        timeline.setHeight(40);
        timeline.widthProperty().bind(timelineHolder.widthProperty());
        timeline.widthProperty().addListener((o, a, b) -> redrawTimeline());
        timelineHolder.setMinWidth(140);
        timelineHolder.setPrefHeight(40);
        timeline.setOnMousePressed(this::onTimelinePress);
        timeline.setOnMouseDragged(this::onTimelineDrag);
        timeline.setOnMouseReleased(e -> {
            draggingMarker = null;
            scrubbing = false;
        });
        HBox.setHgrow(timelineHolder, Priority.ALWAYS);

        HBox row2 = new HBox(8, new Label("Animation:"), animSelect, playButton, timelineHolder, timeLabel);
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

    /** Notified when a damage volume is clicked in the 3D view. */
    void setOnPointPicked(Consumer<DamagePoint> callback) {
        this.onPointPicked = callback;
    }

    /** Notified when an edit happens here (e.g. dragging a delay marker) so the doc goes dirty. */
    void setOnEdit(Runnable callback) {
        this.onEdit = callback;
    }

    /** Highlights the given point (called when selection changes in the Damage tab). */
    void setSelected(DamagePoint dp) {
        if (dp != selectedPoint) {
            selectedPoint = dp;
            refreshDamage();
        }
    }

    void refreshDamage() {
        damageGroup.getChildren().clear();
        if (skill == null) {
            return;
        }
        for (DamagePoint dp : skill.getDamagePoints()) {
            Group holder = new Group();
            holder.setTranslateX(dp.getOffset().getX());
            holder.setTranslateY(dp.getOffset().getY());
            holder.setTranslateZ(dp.getOffset().getZ());
            addVolume(holder, dp, dp == selectedPoint);
            holder.setOnMouseClicked(e -> {
                pick(dp);
                e.consume();
            });
            damageGroup.getChildren().add(holder);
        }
        redrawTimeline();
    }

    /** Builds a translucent volume + wireframe outline + centre marker for a damage point. */
    private void addVolume(Group holder, DamagePoint dp, boolean selected) {
        Color fillColor = selected ? Color.web("#ffd23f", 0.34) : Color.web("#ff5a5a", 0.18);
        Color wireColor = selected ? Color.web("#ffe680") : Color.web("#ff8a8a");
        PhongMaterial fill = new PhongMaterial(fillColor);
        PhongMaterial wire = new PhongMaterial(wireColor);

        Shape shape = dp.getShape();
        switch (shape.getType()) {
            case SPHERE -> {
                double r = Math.max(shape.getRadius(), 0.05);
                holder.getChildren().addAll(fillSphere(r, fill), wireSphere(r, wire));
            }
            case BOX -> {
                double sx = Math.max(shape.getHalfExtents().getX() * 2, 0.05);
                double sy = Math.max(shape.getHalfExtents().getY() * 2, 0.05);
                double sz = Math.max(shape.getHalfExtents().getZ() * 2, 0.05);
                holder.getChildren().addAll(fillBox(sx, sy, sz, fill), wireBox(sx, sy, sz, wire));
            }
            case CONE -> {
                double r = Math.max(shape.getLength(), 0.05);
                holder.getChildren().addAll(fillSphere(r, fill), wireSphere(r, wire));
            }
        }
        // Bright centre dot for precise position reference.
        Sphere marker = new Sphere(0.12);
        marker.setMaterial(new PhongMaterial(selected ? Color.web("#fff2b0") : Color.web("#ff5a5a")));
        holder.getChildren().add(marker);
    }

    private Sphere fillSphere(double r, PhongMaterial mat) {
        Sphere s = new Sphere(r);
        s.setMaterial(mat);
        return s;
    }

    private Sphere wireSphere(double r, PhongMaterial mat) {
        Sphere s = new Sphere(r * 1.001);
        s.setMaterial(mat);
        s.setDrawMode(DrawMode.LINE);
        s.setMouseTransparent(true); // don't block picking of the fill
        return s;
    }

    private Box fillBox(double sx, double sy, double sz, PhongMaterial mat) {
        Box b = new Box(sx, sy, sz);
        b.setMaterial(mat);
        return b;
    }

    private Box wireBox(double sx, double sy, double sz, PhongMaterial mat) {
        Box b = new Box(sx, sy, sz);
        b.setMaterial(mat);
        b.setDrawMode(DrawMode.LINE);
        b.setMouseTransparent(true);
        return b;
    }

    private void pick(DamagePoint dp) {
        selectedPoint = dp;
        refreshDamage();
        if (onPointPicked != null) {
            onPointPicked.accept(dp);
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
        timelineLength = currentAnim == null ? 1 : Math.max(currentAnim.lengthSeconds, 0.0001);
        timeline.setDisable(false); // still usable for delay markers even in rest pose
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
        int tick = (int) Math.round(currentTime * TICKS_PER_SECOND);
        int total = currentAnim == null ? 0 : (int) Math.round(currentAnim.lengthSeconds * TICKS_PER_SECOND);
        timeLabel.setText("tick " + tick + " / " + total);
        redrawTimeline();
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
    }

    // ---- timeline (damage delay markers) ------------------------------

    private static final double TL_PAD = 10;

    /** Draws the playback playhead and a draggable marker per damage point at its delayTicks. */
    private void redrawTimeline() {
        double w = timeline.getWidth();
        double h = timeline.getHeight();
        if (w <= 0) {
            return;
        }
        GraphicsContext g = timeline.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        double uw = Math.max(w - 2 * TL_PAD, 1);
        double trackTop = 24;
        double trackBot = 34;

        g.setFill(Color.web("#2a2d33"));
        g.fillRoundRect(TL_PAD, trackTop, uw, trackBot - trackTop, 6, 6);

        double totalTicks = Math.max(timelineLength * TICKS_PER_SECOND, 1);
        g.setFont(Font.font(9));
        for (int t = 0; t <= totalTicks + 0.001; t += 5) {
            double x = TL_PAD + (t / totalTicks) * uw;
            g.setStroke(Color.web("#3a3e45"));
            g.setLineWidth(1);
            g.strokeLine(x, trackTop, x, trackBot);
            if (t % 10 == 0) {
                g.setFill(Color.web("#777"));
                g.fillText(Integer.toString(t), x - 4, trackBot + 10);
            }
        }

        // Playhead (current animation time).
        double px = TL_PAD + (clamp(currentTime, 0, timelineLength) / timelineLength) * uw;
        g.setStroke(Color.web("#7fd4ff"));
        g.setLineWidth(2);
        g.strokeLine(px, 2, px, trackBot);

        // Damage delay markers.
        if (skill != null) {
            for (DamagePoint dp : skill.getDamagePoints()) {
                double sec = dp.getDelayTicks() / TICKS_PER_SECOND;
                boolean beyond = sec > timelineLength;
                double mx = TL_PAD + (clamp(sec, 0, timelineLength) / timelineLength) * uw;
                boolean sel = dp == selectedPoint;
                g.setFill(sel ? Color.web("#ffd23f") : Color.web("#ff6b6b"));
                g.fillPolygon(new double[]{mx - 5, mx + 5, mx}, new double[]{4, 4, 16}, 3);
                if (sel) {
                    g.fillText(dp.getDelayTicks() + "t" + (beyond ? " »" : ""), mx + 7, 13);
                } else if (beyond) {
                    g.fillText("»", mx + 7, 13);
                }
            }
        }
    }

    private double xToSeconds(double x) {
        double uw = Math.max(timeline.getWidth() - 2 * TL_PAD, 1);
        return clamp((x - TL_PAD) / uw, 0, 1) * timelineLength;
    }

    private void onTimelinePress(MouseEvent e) {
        draggingMarker = null;
        scrubbing = false;
        if (skill != null && e.getY() < 20) {
            double uw = Math.max(timeline.getWidth() - 2 * TL_PAD, 1);
            double best = 8;
            DamagePoint hit = null;
            for (DamagePoint dp : skill.getDamagePoints()) {
                double sec = clamp(dp.getDelayTicks() / TICKS_PER_SECOND, 0, timelineLength);
                double mx = TL_PAD + (sec / timelineLength) * uw;
                double d = Math.abs(mx - e.getX());
                if (d < best) {
                    best = d;
                    hit = dp;
                }
            }
            if (hit != null) {
                draggingMarker = hit;
                pick(hit);
                return;
            }
        }
        scrubbing = true;
        scrubTo(xToSeconds(e.getX()));
    }

    private void onTimelineDrag(MouseEvent e) {
        if (draggingMarker != null) {
            int ticks = (int) Math.round(xToSeconds(e.getX()) * TICKS_PER_SECOND);
            draggingMarker.setDelayTicks(Math.max(ticks, 0));
            if (onEdit != null) {
                onEdit.run();
            }
            redrawTimeline();
        } else if (scrubbing) {
            scrubTo(xToSeconds(e.getX()));
        }
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
