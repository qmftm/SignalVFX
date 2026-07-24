package com.signalvfx.editor;

import com.signalvfx.editor.bbmodel.BbGeometry;
import com.signalvfx.model.DamagePoint;
import com.signalvfx.model.Shape;
import com.signalvfx.model.Skill;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * A rough 3D viewport that shows an imported BlockBench model together with the
 * skill's damage-point volumes, so points can be placed relative to the model.
 * Orbit with left-drag, zoom with the scroll wheel. Rendering is approximate
 * (cube geometry, no textures/animation); the authoritative look is BetterModel
 * in-game.
 *
 * <p>All content lives in Minecraft block space: the model is scaled from
 * BlockBench units (16 units per block) and the world Y axis is flipped so up
 * is up on screen. Damage volumes are placed directly at their block offsets.
 */
final class Preview3DPane extends BorderPane {

    private static final double UNITS_PER_BLOCK = 16.0;

    private final Pane viewportHolder = new Pane();
    private final Label status = new Label("No model loaded. Import a .bbmodel to preview.");

    private final Group modelGroup = new Group();   // BlockBench units
    private final Group damageGroup = new Group();   // block units
    private final Group world = new Group(modelGroup, damageGroup);
    private final Group pivot = new Group(world);

    private final Rotate rotateX = new Rotate(-20, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-30, Rotate.Y_AXIS);
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    private SubScene subScene;
    private Skill skill;
    private BbGeometry geometry;

    private double distance = 30;
    private double anchorX;
    private double anchorY;
    private double anchorRx;
    private double anchorRy;

    Preview3DPane(Skill skill) {
        this.skill = skill;
        setPadding(new Insets(8));

        Button load = new Button("Load .bbmodel for preview…");
        load.setOnAction(e -> chooseModel());
        Button fit = new Button("Reset view");
        fit.setOnAction(e -> resetView());
        HBox toolbar = new HBox(8, load, fit, status);
        toolbar.setPadding(new Insets(0, 0, 8, 0));
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        setTop(toolbar);

        // Y-up world (flip JavaFX's Y-down); model additionally scaled to blocks.
        world.getTransforms().add(new Scale(1, -1, 1));
        modelGroup.getTransforms().add(new Scale(1 / UNITS_PER_BLOCK, 1 / UNITS_PER_BLOCK, 1 / UNITS_PER_BLOCK));
        pivot.getTransforms().addAll(rotateY, rotateX);

        Group sceneRoot = new Group(pivot, buildLights(), buildAxes());
        subScene = new SubScene(sceneRoot, 600, 480, true, SceneAntialiasing.BALANCED);
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

    void bind(Skill skill) {
        this.skill = skill;
        refreshDamage();
    }

    /** Called when a .bbmodel is imported elsewhere (e.g. the Visual tab). */
    void setGeometry(BbGeometry geometry) {
        this.geometry = geometry;
        rebuildModel();
        resetView();
    }

    /** Rebuilds only the damage overlay; cheap enough to call on every edit. */
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
        if (geometry == null) {
            status.setText("No model loaded.");
            return;
        }
        modelGroup.getChildren().add(buildBone(geometry.getRoot()));
        status.setText("Model: " + geometry.getCubeCount() + " cube(s). Left-drag: orbit · scroll: zoom.");
    }

    private Group buildBone(BbGeometry.Bone bone) {
        Group g = new Group();
        // Rotate the bone's contents about its pivot (Z, then Y, then X).
        if (bone.rotation[0] != 0 || bone.rotation[1] != 0 || bone.rotation[2] != 0) {
            g.getTransforms().addAll(
                    new Rotate(bone.rotation[2], bone.origin[0], bone.origin[1], bone.origin[2], Rotate.Z_AXIS),
                    new Rotate(bone.rotation[1], bone.origin[0], bone.origin[1], bone.origin[2], Rotate.Y_AXIS),
                    new Rotate(bone.rotation[0], bone.origin[0], bone.origin[1], bone.origin[2], Rotate.X_AXIS));
        }
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
        // Box is centred on its local origin; move it to the cube's centre.
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
        // Camera sits back along -Z, looking toward the origin.
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

    /** Offsets the world so the combined model+damage bounds are centred on the pivot. */
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

    /** Small X(red)/Y(green)/Z(blue) axis rods at the origin for orientation. */
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
