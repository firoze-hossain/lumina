package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

/**
 * Settings page for Languages & Frameworks > PHP > Debug > Templates.
 * Matches Image 3.
 */
public class SettingsLanguagesPhpDebugTemplatesPage extends VBox {

    public SettingsLanguagesPhpDebugTemplatesPage() {
        setSpacing(12);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
    }

    public boolean isModified() {
        return false;
    }

    public void apply() {
    }

    public void reset() {
    }

    public void revertChanges() {
    }
}
