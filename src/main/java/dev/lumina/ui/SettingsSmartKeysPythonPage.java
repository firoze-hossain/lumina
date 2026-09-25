package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * Editor > General > Smart Keys > Python settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysPythonPage extends VBox {

    private final CheckBox smartIndentPastedLinesCheck = new CheckBox("Smart indent pasted lines");
    private final CheckBox useParenthesesCheck = new CheckBox("Use parentheses instead of backslashes for breaking lines");
    private final CheckBox insertSelfCheck = new CheckBox("Insert 'self' when defining a method");
    private final CheckBox insertTypePlaceholdersCheck = new CheckBox("Insert type placeholders in the documentation comment stub");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysPythonPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        styleCheckBox(smartIndentPastedLinesCheck);
        styleCheckBox(useParenthesesCheck);
        styleCheckBox(insertSelfCheck);
        styleCheckBox(insertTypePlaceholdersCheck);

        setupListeners();

        getChildren().addAll(
                smartIndentPastedLinesCheck,
                useParenthesesCheck,
                insertSelfCheck,
                insertTypePlaceholdersCheck
        );

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

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void setupListeners() {
        smartIndentPastedLinesCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        useParenthesesCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        insertSelfCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        insertTypePlaceholdersCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
    }

    public void loadFromSettings(SmartKeysSettings settings) {
        suppressEvents = true;
        try {
            smartIndentPastedLinesCheck.setSelected(settings.isPythonSmartIndentPastedLines());
            useParenthesesCheck.setSelected(settings.isPythonUseParenthesesInsteadOfBackslashes());
            insertSelfCheck.setSelected(settings.isPythonInsertSelfWhenDefiningMethod());
            insertTypePlaceholdersCheck.setSelected(settings.isPythonInsertTypePlaceholdersInDocCommentStub());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return smartIndentPastedLinesCheck.isSelected() != s.isPythonSmartIndentPastedLines()
                || useParenthesesCheck.isSelected() != s.isPythonUseParenthesesInsteadOfBackslashes()
                || insertSelfCheck.isSelected() != s.isPythonInsertSelfWhenDefiningMethod()
                || insertTypePlaceholdersCheck.isSelected() != s.isPythonInsertTypePlaceholdersInDocCommentStub();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setPythonSmartIndentPastedLines(smartIndentPastedLinesCheck.isSelected());
        s.setPythonUseParenthesesInsteadOfBackslashes(useParenthesesCheck.isSelected());
        s.setPythonInsertSelfWhenDefiningMethod(insertSelfCheck.isSelected());
        s.setPythonInsertTypePlaceholdersInDocCommentStub(insertTypePlaceholdersCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    // --- Component Getters ---
    public CheckBox getSmartIndentPastedLinesCheck() { return smartIndentPastedLinesCheck; }
    public CheckBox getUseParenthesesCheck() { return useParenthesesCheck; }
    public CheckBox getInsertSelfCheck() { return insertSelfCheck; }
    public CheckBox getInsertTypePlaceholdersCheck() { return insertTypePlaceholdersCheck; }
}
