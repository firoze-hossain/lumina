// SettingsStickyLinesPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Sticky Lines settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsStickyLinesPage extends VBox {

    public SettingsStickyLinesPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Show sticky lines while scrolling
        // ============================================================
        CheckBox showStickyLines = new CheckBox("Show sticky lines while scrolling");
        showStickyLines.setSelected(true);
        showStickyLines.getStyleClass().add("settings-check");

        // ============================================================
        // Maximum number of lines
        // ============================================================
        HBox maxRow = new HBox(8);
        maxRow.setPadding(new Insets(4, 0, 8, 20));
        maxRow.setAlignment(Pos.CENTER_LEFT);

        Label maxLabel = new Label("Maximum number of lines:");
        maxLabel.getStyleClass().add("settings-label");
        Spinner<Integer> maxSpinner = new Spinner<>(1, 20, 5, 1);
        maxSpinner.setPrefWidth(70);
        maxSpinner.getStyleClass().add("settings-spinner");
        maxRow.getChildren().addAll(maxLabel, maxSpinner);

        // ============================================================
        // Languages section
        // ============================================================
        Label languagesLabel = new Label("Languages:");
        languagesLabel.getStyleClass().add("settings-section");

        // Language checkboxes in a grid - 6 columns
        GridPane languageGrid = new GridPane();
        languageGrid.setHgap(16);
        languageGrid.setVgap(4);
        languageGrid.setPadding(new Insets(8, 0, 8, 20));

        String[] languages = {
            "CSS", "FreeMarker", "Groovy", "HTML", "Java", "JavaScript",
            "JSON", "JSP", "JSPX", "Jupyter", "Kotlin", "Less",
            "Markdown", "protobuf", "Rust", "Sass", "SCSS", "SQL",
            "TypeScript", "VTL", "XHTML", "XML", "YAML"
        };

        // All languages selected by default (matching screenshot)
        for (int i = 0; i < languages.length; i++) {
            CheckBox cb = new CheckBox(languages[i]);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            languageGrid.add(cb, i % 6, i / 6);
        }

        // ============================================================
        // Manage colors button
        // ============================================================
        Button manageColors = new Button("Manage colors");
        manageColors.getStyleClass().add("dialog-secondary");
        manageColors.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Manage Colors");
            alert.setHeaderText("Manage Colors");
            alert.setContentText("Color management for sticky lines will be available in a future update.");
            alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
            alert.showAndWait();
        });

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            showStickyLines,
            maxRow,
            languagesLabel,
            languageGrid,
            manageColors
        );
    }
}