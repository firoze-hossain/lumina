package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * Editor > General > Smart Keys > Ruby settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysRubyPage extends VBox {

    private final CheckBox autoInsertEndCheck = new CheckBox("Insert 'end' on Enter");
    private final CheckBox smartIndentCheck = new CheckBox("Smart indent on typing");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysRubyPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        autoInsertEndCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        smartIndentCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        autoInsertEndCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        smartIndentCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        getChildren().addAll(autoInsertEndCheck, smartIndentCheck);
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
            autoInsertEndCheck.setSelected(settings.isRubyAutoInsertEnd());
            smartIndentCheck.setSelected(settings.isRubySmartIndent());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return autoInsertEndCheck.isSelected() != s.isRubyAutoInsertEnd()
                || smartIndentCheck.isSelected() != s.isRubySmartIndent();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setRubyAutoInsertEnd(autoInsertEndCheck.isSelected());
        s.setRubySmartIndent(smartIndentCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    public CheckBox getAutoInsertEndCheck() { return autoInsertEndCheck; }
    public CheckBox getSmartIndentCheck() { return smartIndentCheck; }
}
