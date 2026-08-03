// SettingsSmartKeysMarkdownPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Smart Keys > Markdown settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsSmartKeysMarkdownPage extends VBox {

    public SettingsSmartKeysMarkdownPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Tables section
        // ============================================================
        Label tablesLabel = new Label("Tables");
        tablesLabel.getStyleClass().add("settings-section");

        VBox tablesBox = new VBox(4);
        tablesBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox reformatTable = new CheckBox("Reformat table when typing");
        reformatTable.setSelected(true);
        reformatTable.getStyleClass().add("settings-check");

        CheckBox insertLineBreak = new CheckBox("Insert HTML line break ('<br/>') instead of new line inside table cells");
        insertLineBreak.setSelected(true);
        insertLineBreak.getStyleClass().add("settings-check");

        CheckBox shiftEnterRow = new CheckBox("Use Shift+Enter to insert new table row");
        shiftEnterRow.setSelected(true);
        shiftEnterRow.getStyleClass().add("settings-check");

        CheckBox tabNavigate = new CheckBox("Use Tab/Shift+Tab to navigate table cells");
        tabNavigate.setSelected(true);
        tabNavigate.getStyleClass().add("settings-check");

        tablesBox.getChildren().addAll(
            reformatTable,
            insertLineBreak,
            shiftEnterRow,
            tabNavigate
        );

        // ============================================================
        // Lists section
        // ============================================================
        Label listsLabel = new Label("Lists");
        listsLabel.getStyleClass().add("settings-section");

        VBox listsBox = new VBox(4);
        listsBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox adjustIndentation = new CheckBox("Adjust indentation on type");
        adjustIndentation.setSelected(true);
        adjustIndentation.getStyleClass().add("settings-check");

        CheckBox smartEnterBackspace = new CheckBox("Use smart Enter and Backspace");
        smartEnterBackspace.setSelected(true);
        smartEnterBackspace.getStyleClass().add("settings-check");

        CheckBox renumberList = new CheckBox("Renumber list when typing");
        renumberList.setSelected(false);
        renumberList.getStyleClass().add("settings-check");

        listsBox.getChildren().addAll(
            adjustIndentation,
            smartEnterBackspace,
            renumberList
        );

        // List numerating dropdown
        HBox numeratingRow = new HBox(8);
        numeratingRow.setPadding(new Insets(4, 0, 8, 20));
        numeratingRow.setAlignment(Pos.CENTER_LEFT);

        Label numeratingLabel = new Label("List numerating:");
        numeratingLabel.getStyleClass().add("settings-label");
        ComboBox<String> numeratingCombo = new ComboBox<>();
        numeratingCombo.getItems().addAll("Sequentially", "Strictly", "As in markdown");
        numeratingCombo.getSelectionModel().selectFirst();
        numeratingCombo.getStyleClass().add("settings-combo");
        numeratingCombo.setPrefWidth(150);
        numeratingRow.getChildren().addAll(numeratingLabel, numeratingCombo);

        // ============================================================
        // Other section
        // ============================================================
        Label otherLabel = new Label("Other");
        otherLabel.getStyleClass().add("settings-section");

        VBox otherBox = new VBox(4);
        otherBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox insertLinks = new CheckBox("Insert links to images or files on drag-and-drop");
        insertLinks.setSelected(true);
        insertLinks.getStyleClass().add("settings-check");

        otherBox.getChildren().add(insertLinks);

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            tablesLabel,
            tablesBox,
            listsLabel,
            listsBox,
            numeratingRow,
            otherLabel,
            otherBox
        );
    }
}