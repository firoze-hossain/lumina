// SettingsDebuggerPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > Color Scheme > Debugger settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsDebuggerPage extends VBox {

    public SettingsDebuggerPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(14);

        // ============================================================
        // Scheme row
        // ============================================================
        HBox schemeRow = new HBox(12);
        schemeRow.setAlignment(Pos.CENTER_LEFT);

        Label schemeLabel = new Label("Scheme:");
        schemeLabel.getStyleClass().add("settings-label");

        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.getItems().addAll("Islands Dark Theme default", "Darcula", "IntelliJ Light");
        schemeCombo.getSelectionModel().selectFirst();
        schemeCombo.getStyleClass().add("settings-combo");
        schemeCombo.setPrefWidth(260);

        Hyperlink themeLink = new Hyperlink("Change IDE Theme...");
        themeLink.getStyleClass().add("settings-link");

        schemeRow.getChildren().addAll(schemeLabel, schemeCombo, themeLink);

        // ============================================================
        // Debugger color items
        // ============================================================
        VBox itemsBox = new VBox(4);
        itemsBox.setPadding(new Insets(12, 0, 12, 0));

        String[] debuggerItems = {
            "Breakpoint line",
            "Evaluated expression text",
            "Evaluated expression text for execution line",
            "Execution point",
            "Inline stack frames",
            "Inlined modified values",
            "Inlined values",
            "Inlined values for execution line",
            "Not top frame",
            "Smart step into selection",
            "Smart step into target"
        };

        for (String item : debuggerItems) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 0, 2, 0));

            Label colorPreview = new Label("■");
            colorPreview.setStyle("-fx-text-fill: #3A3122; -fx-font-size: 16px;");
            Button chooseBtn = new Button("Choose...");
            chooseBtn.getStyleClass().add("dialog-secondary");
            chooseBtn.setPrefWidth(80);

            Label itemLabel = new Label(item);
            itemLabel.getStyleClass().add("settings-label");

            row.getChildren().addAll(colorPreview, chooseBtn, itemLabel);
            itemsBox.getChildren().add(row);
        }

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(schemeRow, itemsBox);
    }
}