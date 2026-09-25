package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Editor > General > Smart Keys > Markdown settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysMarkdownPage extends VBox {

    // Tables
    private final CheckBox reformatTableCheck = new CheckBox("Reformat table when typing");
    private final CheckBox insertLineBreakCheck = new CheckBox("Insert HTML line break ('<br/>') instead of new line inside table cells");
    private final CheckBox shiftEnterRowCheck = new CheckBox("Use Shift+Enter to insert new table row");
    private final CheckBox tabNavigateCheck = new CheckBox("Use Tab/Shift+Tab to navigate table cells");

    // Lists
    private final CheckBox adjustIndentationCheck = new CheckBox("Adjust indentation on type");
    private final CheckBox smartEnterBackspaceCheck = new CheckBox("Use smart Enter and Backspace");
    private final CheckBox renumberListCheck = new CheckBox("Renumber list when typing");
    private final ComboBox<String> numeratingCombo = new ComboBox<>();

    // Other
    private final CheckBox insertLinksCheck = new CheckBox("Insert links to images or files on drag-and-drop");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysMarkdownPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        buildUi();
        setupListeners();
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

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 0, 4, 0));

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(lbl, line);
        return header;
    }

    private void buildUi() {
        styleCheckBox(reformatTableCheck);
        styleCheckBox(insertLineBreakCheck);
        styleCheckBox(shiftEnterRowCheck);
        styleCheckBox(tabNavigateCheck);
        styleCheckBox(adjustIndentationCheck);
        styleCheckBox(smartEnterBackspaceCheck);
        styleCheckBox(renumberListCheck);
        styleCheckBox(insertLinksCheck);

        HBox tablesHeader = createSectionHeader("Tables");
        VBox tablesGroup = new VBox(8,
                reformatTableCheck,
                insertLineBreakCheck,
                shiftEnterRowCheck,
                tabNavigateCheck
        );

        HBox listsHeader = createSectionHeader("Lists");

        numeratingCombo.getItems().addAll("Sequentially", "Strictly", "As in markdown");
        numeratingCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        numeratingCombo.setPrefWidth(160);

        Label numeratingLabel = new Label("List numerating:");
        numeratingLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox numeratingRow = new HBox(10, numeratingLabel, numeratingCombo);
        numeratingRow.setAlignment(Pos.CENTER_LEFT);
        numeratingRow.setPadding(new Insets(4, 0, 0, 0));

        VBox listsGroup = new VBox(8,
                adjustIndentationCheck,
                smartEnterBackspaceCheck,
                renumberListCheck,
                numeratingRow
        );

        HBox otherHeader = createSectionHeader("Other");
        VBox otherGroup = new VBox(8,
                insertLinksCheck
        );

        getChildren().addAll(
                tablesHeader, tablesGroup,
                listsHeader, listsGroup,
                otherHeader, otherGroup
        );
    }

    private void setupListeners() {
        reformatTableCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        insertLineBreakCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        shiftEnterRowCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        tabNavigateCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        adjustIndentationCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        smartEnterBackspaceCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        renumberListCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        numeratingCombo.valueProperty().addListener((obs, old, val) -> notifyModified());
        insertLinksCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
    }

    public void loadFromSettings(SmartKeysSettings settings) {
        suppressEvents = true;
        try {
            reformatTableCheck.setSelected(settings.isMarkdownReformatTable());
            insertLineBreakCheck.setSelected(settings.isMarkdownInsertHtmlBreakInsideTableCells());
            shiftEnterRowCheck.setSelected(settings.isMarkdownUseShiftEnterForNewTableRow());
            tabNavigateCheck.setSelected(settings.isMarkdownUseTabShiftTabToNavigateCells());
            adjustIndentationCheck.setSelected(settings.isMarkdownAdjustIndentationOnType());
            smartEnterBackspaceCheck.setSelected(settings.isMarkdownSmartEnterAndBackspace());
            renumberListCheck.setSelected(settings.isMarkdownRenumberListWhenTyping());
            numeratingCombo.setValue(settings.getMarkdownListNumerating());
            insertLinksCheck.setSelected(settings.isMarkdownInsertLinksOnDrop());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return reformatTableCheck.isSelected() != s.isMarkdownReformatTable()
                || insertLineBreakCheck.isSelected() != s.isMarkdownInsertHtmlBreakInsideTableCells()
                || shiftEnterRowCheck.isSelected() != s.isMarkdownUseShiftEnterForNewTableRow()
                || tabNavigateCheck.isSelected() != s.isMarkdownUseTabShiftTabToNavigateCells()
                || adjustIndentationCheck.isSelected() != s.isMarkdownAdjustIndentationOnType()
                || smartEnterBackspaceCheck.isSelected() != s.isMarkdownSmartEnterAndBackspace()
                || renumberListCheck.isSelected() != s.isMarkdownRenumberListWhenTyping()
                || !numeratingCombo.getValue().equals(s.getMarkdownListNumerating())
                || insertLinksCheck.isSelected() != s.isMarkdownInsertLinksOnDrop();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setMarkdownReformatTable(reformatTableCheck.isSelected());
        s.setMarkdownInsertHtmlBreakInsideTableCells(insertLineBreakCheck.isSelected());
        s.setMarkdownUseShiftEnterForNewTableRow(shiftEnterRowCheck.isSelected());
        s.setMarkdownUseTabShiftTabToNavigateCells(tabNavigateCheck.isSelected());
        s.setMarkdownAdjustIndentationOnType(adjustIndentationCheck.isSelected());
        s.setMarkdownSmartEnterAndBackspace(smartEnterBackspaceCheck.isSelected());
        s.setMarkdownRenumberListWhenTyping(renumberListCheck.isSelected());
        s.setMarkdownListNumerating(numeratingCombo.getValue());
        s.setMarkdownInsertLinksOnDrop(insertLinksCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    // --- Component Getters ---
    public CheckBox getReformatTableCheck() { return reformatTableCheck; }
    public CheckBox getInsertLineBreakCheck() { return insertLineBreakCheck; }
    public CheckBox getShiftEnterRowCheck() { return shiftEnterRowCheck; }
    public CheckBox getTabNavigateCheck() { return tabNavigateCheck; }
    public CheckBox getAdjustIndentationCheck() { return adjustIndentationCheck; }
    public CheckBox getSmartEnterBackspaceCheck() { return smartEnterBackspaceCheck; }
    public CheckBox getRenumberListCheck() { return renumberListCheck; }
    public ComboBox<String> getNumeratingCombo() { return numeratingCombo; }
    public CheckBox getInsertLinksCheck() { return insertLinksCheck; }
}