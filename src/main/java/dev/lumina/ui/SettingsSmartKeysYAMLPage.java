package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * Editor > General > Smart Keys > YAML settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysYAMLPage extends VBox {

    private final CheckBox autoExpandCheck = new CheckBox("Auto expand key sequences upon paste");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysYAMLPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        autoExpandCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        autoExpandCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        getChildren().add(autoExpandCheck);
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void loadFromSettings(SmartKeysSettings settings) {
        suppressEvents = true;
        try {
            autoExpandCheck.setSelected(settings.isYamlAutoExpandKeySequencesOnPaste());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        return autoExpandCheck.isSelected() != SmartKeysSettings.getInstance().isYamlAutoExpandKeySequencesOnPaste();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setYamlAutoExpandKeySequencesOnPaste(autoExpandCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    public CheckBox getAutoExpandCheck() {
        return autoExpandCheck;
    }
}