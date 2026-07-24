package com.signalvfx.editor;

/**
 * Plain (non-{@link javafx.application.Application}) entry point. Launching the
 * app through this class avoids the "JavaFX runtime components are missing"
 * error that occurs when a shaded fat jar's main class extends
 * {@code Application}.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        EditorApp.main(args);
    }
}
