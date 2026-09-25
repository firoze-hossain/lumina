package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * Editor > General > Smart Keys > Scala settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysScalaPage extends VBox {

    private final CheckBox insertClosingBraceCheck = new CheckBox("Insert closing brace on Enter");
    private final CheckBox autoIndentCheck = new CheckBox("Smart indent on typing");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysScalaPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        insertClosingBraceCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        autoIndentCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        insertClosingBraceCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoIndentCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        getChildren().addAll(insertClosingBraceCheck, autoIndentCheck);
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
            insertClosingBraceCheck.setSelected(settings.isScalaInsertClosingBrace());
            autoIndentCheck.setSelected(settings.isScalaAutoIndent());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return insertClosingBraceCheck.isSelected() != s.isScalaInsertClosingBrace()
                || autoIndentCheck.isSelected() != s.isScalaAutoIndent();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setScalaInsertClosingBrace(insertClosingBraceCheck.isSelected());
        s.setScalaAutoIndent(autoIndentCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    public CheckBox getInsertClosingBraceCheck() { return insertClosingBraceCheck; }
    public CheckBox getAutoIndentCheck() { return autoIndentCheck; }
}
