package dev.lumina;

/**
 * Standard launcher entry point for Lumina IDE.
 * Allows launching from standalone executable JAR and jpackage runtime
 * without requiring JavaFX modules on the bootstrap layer.
 */
public class Launcher {
    public static void main(String[] args) {
        LuminaApp.main(args);
    }
}
