package com.signalvfx.editor;

import com.signalvfx.model.Skill;
import com.signalvfx.model.io.SkillIO;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The SignalVFX skill editor application: menu-driven load/save of skill JSON
 * documents, with tabs for identity/cast, visual, and damage editing.
 */
public class EditorApp extends Application {

    private Stage stage;
    private Skill skill = new Skill();
    private Path currentPath;
    private boolean dirty;

    private MetaCastPane metaPane;
    private VisualPane visualPane;
    private DamagePane damagePane;
    private Preview3DPane previewPane;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        Runnable onChange = this::onEdit;
        previewPane = new Preview3DPane(skill);
        metaPane = new MetaCastPane(onChange);
        visualPane = new VisualPane(onChange, geometry -> previewPane.setGeometry(geometry));
        damagePane = new DamagePane(onChange);

        // Keep damage-point selection in sync between the Damage tab and the 3D preview.
        damagePane.setOnSelectionChanged(dp -> previewPane.setSelected(dp));
        previewPane.setOnPointPicked(dp -> damagePane.select(dp));
        // Timeline delay-marker drags mark the document dirty and refresh overlays.
        previewPane.setOnEdit(this::onEdit);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("Skill & Cast", metaPane),
                new Tab("Visual (VFX)", visualPane),
                new Tab("Damage", damagePane),
                new Tab("3D Preview", previewPane));

        BorderPane rootPane = new BorderPane();
        rootPane.setTop(buildMenuBar());
        rootPane.setCenter(tabs);

        bindAll();

        Scene scene = new Scene(rootPane, 1000, 680);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            if (!confirmDiscardIfDirty()) {
                e.consume();
            }
        });
        updateTitle();
        stage.show();
    }

    private MenuBar buildMenuBar() {
        Menu file = new Menu("File");
        MenuItem newItem = item("New", "Shortcut+N", this::newSkill);
        MenuItem open = item("Open…", "Shortcut+O", this::openSkill);
        MenuItem save = item("Save", "Shortcut+S", this::save);
        MenuItem saveAs = item("Save As…", "Shortcut+Shift+S", this::saveAs);
        MenuItem quit = item("Quit", "Shortcut+Q", () -> {
            if (confirmDiscardIfDirty()) {
                stage.close();
            }
        });
        file.getItems().addAll(newItem, open, save, saveAs, new javafx.scene.control.SeparatorMenuItem(), quit);

        Menu help = new Menu("Help");
        help.getItems().add(item("About", null, this::about));

        return new MenuBar(file, help);
    }

    private MenuItem item(String text, String accel, Runnable action) {
        MenuItem mi = new MenuItem(text);
        if (accel != null) {
            mi.setAccelerator(KeyCombination.keyCombination(accel));
        }
        mi.setOnAction(e -> action.run());
        return mi;
    }

    private void bindAll() {
        metaPane.bind(skill);
        visualPane.bind(skill);
        damagePane.bind(skill);
        previewPane.bind(skill);
    }

    /** Any edit: mark the document dirty and keep the 3D overlay in sync. */
    private void onEdit() {
        markDirty();
        previewPane.refreshDamage();
    }

    // ---- actions -------------------------------------------------------

    private void newSkill() {
        if (!confirmDiscardIfDirty()) {
            return;
        }
        skill = new Skill();
        currentPath = null;
        dirty = false;
        bindAll();
        updateTitle();
    }

    private void openSkill() {
        if (!confirmDiscardIfDirty()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Skill");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Skill JSON", "*.json"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            skill = SkillIO.read(file.toPath());
            currentPath = file.toPath();
            dirty = false;
            bindAll();
            updateTitle();
        } catch (Exception ex) {
            error("Failed to open", ex.getMessage());
        }
    }

    private void save() {
        if (currentPath == null) {
            saveAs();
            return;
        }
        writeTo(currentPath);
    }

    private void saveAs() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Skill");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Skill JSON", "*.json"));
        chooser.setInitialFileName((skill.getId() == null || skill.getId().isBlank() ? "skill" : skill.getId()) + ".json");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        writeTo(file.toPath());
    }

    private void writeTo(Path path) {
        try {
            SkillIO.write(path, skill);
            currentPath = path;
            dirty = false;
            updateTitle();
        } catch (Exception ex) {
            error("Failed to save", ex.getMessage());
        }
    }

    private void about() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("About SignalVFX");
        a.setHeaderText("SignalVFX Skill Editor");
        a.setContentText("Author MagicSpell-style skills for Paper 1.21.\n"
                + "Choose a resource-pack or display-entity visual, and place\n"
                + "damage points with ranges. Files are plain skill JSON.");
        a.showAndWait();
    }

    // ---- state helpers -------------------------------------------------

    private void markDirty() {
        if (!dirty) {
            dirty = true;
            updateTitle();
        }
    }

    private void updateTitle() {
        String name = currentPath == null ? "untitled.json" : currentPath.getFileName().toString();
        stage.setTitle("SignalVFX Editor — " + name + (dirty ? " *" : ""));
    }

    private boolean confirmDiscardIfDirty() {
        if (!dirty) {
            return true;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "You have unsaved changes. Discard them?",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Unsaved changes");
        Optional<ButtonType> result = a.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void error(String header, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(message);
        a.showAndWait();
    }
}
