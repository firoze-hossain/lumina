package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * Editor > General > Smart Keys > SQL settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysSQLPage extends VBox {

    private final CheckBox insertStringConcatCheck = new CheckBox("Insert string concatenation on Enter");
    private final CheckBox closeCodeBlocksCheck = new CheckBox("Close code blocks on Enter");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysSQLPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        insertStringConcatCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        closeCodeBlocksCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        insertStringConcatCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        closeCodeBlocksCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        getChildren().addAll(insertStringConcatCheck, closeCodeBlocksCheck);
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
            insertStringConcatCheck.setSelected(settings.isSqlInsertStringConcatOnEnter());
            closeCodeBlocksCheck.setSelected(settings.isSqlCloseCodeBlocksOnEnter());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return insertStringConcatCheck.isSelected() != s.isSqlInsertStringConcatOnEnter()
                || closeCodeBlocksCheck.isSelected() != s.isSqlCloseCodeBlocksOnEnter();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setSqlInsertStringConcatOnEnter(insertStringConcatCheck.isSelected());
        s.setSqlCloseCodeBlocksOnEnter(closeCodeBlocksCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    public CheckBox getInsertStringConcatCheck() { return insertStringConcatCheck; }
    public CheckBox getCloseCodeBlocksCheck() { return closeCodeBlocksCheck; }
}