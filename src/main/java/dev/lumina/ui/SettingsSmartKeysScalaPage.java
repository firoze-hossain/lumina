package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Editor > General > Smart Keys > Scala settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysScalaPage extends VBox {

    private final CheckBox indentPastedLinesCheck = new CheckBox("Indent pasted lines at caret");
    private final CheckBox insertPairQuotesCheck = new CheckBox("Insert pair quotes for multiline string");
    private final CheckBox upgradeSimpleStringCheck = new CheckBox("Upgrade simple string into interpolated after typing '${'");
    private final CheckBox wrapSingleExpressionCheck = new CheckBox("Wrap single expression body with closing brace after typing '{'");
    private final CheckBox deleteClosingBraceCheck = new CheckBox("Delete closing brace after deleting '{'");

    private final Label controlBracesLabel = new Label("Control curly braces based on indentation:");
    private final CheckBox addBracesAutomaticallyCheck = new CheckBox("Add braces automatically");
    private final CheckBox removeBracesAutomaticallyCheck = new CheckBox("Remove braces automatically");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysScalaPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        buildUi();
        setupListeners();
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private Label createHelpIcon(String tooltipText) {
        Label help = new Label("?");
        help.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 10px; -fx-font-weight: bold; "
                + "-fx-border-color: #5A5D63; -fx-border-radius: 10; -fx-background-radius: 10; "
                + "-fx-min-width: 14px; -fx-min-height: 14px; -fx-max-width: 14px; -fx-max-height: 14px; "
                + "-fx-alignment: center; -fx-cursor: hand;");
        Tooltip tt = new Tooltip(tooltipText);
        tt.setStyle("-fx-font-size: 12px;");
        Tooltip.install(help, tt);
        return help;
    }

    private void buildUi() {
        styleCheckBox(indentPastedLinesCheck);
        styleCheckBox(insertPairQuotesCheck);
        styleCheckBox(upgradeSimpleStringCheck);
        styleCheckBox(wrapSingleExpressionCheck);
        styleCheckBox(deleteClosingBraceCheck);
        styleCheckBox(addBracesAutomaticallyCheck);
        styleCheckBox(removeBracesAutomaticallyCheck);

        controlBracesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 6 0 2 0;");

        HBox addRow = new HBox(6, addBracesAutomaticallyCheck, createHelpIcon("Automatically add curly braces when block indentation increases"));
        addRow.setAlignment(Pos.CENTER_LEFT);

        HBox removeRow = new HBox(6, removeBracesAutomaticallyCheck, createHelpIcon("Automatically remove curly braces when block indentation decreases"));
        removeRow.setAlignment(Pos.CENTER_LEFT);

        VBox indentedBracesGroup = new VBox(8, addRow, removeRow);
        indentedBracesGroup.setPadding(new Insets(0, 0, 0, 20));

        getChildren().addAll(
                indentPastedLinesCheck,
                insertPairQuotesCheck,
                upgradeSimpleStringCheck,
                wrapSingleExpressionCheck,
                deleteClosingBraceCheck,
                controlBracesLabel,
                indentedBracesGroup
        );
    }

    private void setupListeners() {
        indentPastedLinesCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        insertPairQuotesCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        upgradeSimpleStringCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        wrapSingleExpressionCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        deleteClosingBraceCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        addBracesAutomaticallyCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        removeBracesAutomaticallyCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
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
            indentPastedLinesCheck.setSelected(settings.isScalaIndentPastedLinesAtCaret());
            insertPairQuotesCheck.setSelected(settings.isScalaInsertPairQuotesForMultilineString());
            upgradeSimpleStringCheck.setSelected(settings.isScalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace());
            wrapSingleExpressionCheck.setSelected(settings.isScalaWrapSingleExpressionBodyWithClosingBraceAfterBrace());
            deleteClosingBraceCheck.setSelected(settings.isScalaDeleteClosingBraceAfterDeletingBrace());
            addBracesAutomaticallyCheck.setSelected(settings.isScalaAddBracesAutomaticallyBasedOnIndentation());
            removeBracesAutomaticallyCheck.setSelected(settings.isScalaRemoveBracesAutomaticallyBasedOnIndentation());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return indentPastedLinesCheck.isSelected() != s.isScalaIndentPastedLinesAtCaret()
                || insertPairQuotesCheck.isSelected() != s.isScalaInsertPairQuotesForMultilineString()
                || upgradeSimpleStringCheck.isSelected() != s.isScalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace()
                || wrapSingleExpressionCheck.isSelected() != s.isScalaWrapSingleExpressionBodyWithClosingBraceAfterBrace()
                || deleteClosingBraceCheck.isSelected() != s.isScalaDeleteClosingBraceAfterDeletingBrace()
                || addBracesAutomaticallyCheck.isSelected() != s.isScalaAddBracesAutomaticallyBasedOnIndentation()
                || removeBracesAutomaticallyCheck.isSelected() != s.isScalaRemoveBracesAutomaticallyBasedOnIndentation();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setScalaIndentPastedLinesAtCaret(indentPastedLinesCheck.isSelected());
        s.setScalaInsertPairQuotesForMultilineString(insertPairQuotesCheck.isSelected());
        s.setScalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace(upgradeSimpleStringCheck.isSelected());
        s.setScalaWrapSingleExpressionBodyWithClosingBraceAfterBrace(wrapSingleExpressionCheck.isSelected());
        s.setScalaDeleteClosingBraceAfterDeletingBrace(deleteClosingBraceCheck.isSelected());
        s.setScalaAddBracesAutomaticallyBasedOnIndentation(addBracesAutomaticallyCheck.isSelected());
        s.setScalaRemoveBracesAutomaticallyBasedOnIndentation(removeBracesAutomaticallyCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    // --- Component Getters ---
    public CheckBox getIndentPastedLinesCheck() { return indentPastedLinesCheck; }
    public CheckBox getInsertPairQuotesCheck() { return insertPairQuotesCheck; }
    public CheckBox getUpgradeSimpleStringCheck() { return upgradeSimpleStringCheck; }
    public CheckBox getWrapSingleExpressionCheck() { return wrapSingleExpressionCheck; }
    public CheckBox getDeleteClosingBraceCheck() { return deleteClosingBraceCheck; }
    public CheckBox getAddBracesAutomaticallyCheck() { return addBracesAutomaticallyCheck; }
    public CheckBox getRemoveBracesAutomaticallyCheck() { return removeBracesAutomaticallyCheck; }
}
