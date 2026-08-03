// SettingsBreadcrumbsPage.java - NEW FILE
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Breadcrumbs settings page.
 * Exactly matching the screenshot.
 */
public class SettingsBreadcrumbsPage extends VBox {

    public SettingsBreadcrumbsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(12);

        // ============================================================
        // Show breadcrumbs
        // ============================================================
        CheckBox showBreadcrumbs = new CheckBox("Show breadcrumbs");
        showBreadcrumbs.setSelected(true);
        showBreadcrumbs.getStyleClass().add("settings-check");

        // Placement - Top/Bottom radio buttons
        HBox placementRow = new HBox(16);
        placementRow.setPadding(new Insets(4, 0, 8, 20));
        placementRow.setAlignment(Pos.CENTER_LEFT);

        Label placementLabel = new Label("Placement:");
        placementLabel.getStyleClass().add("settings-label");

        RadioButton top = new RadioButton("Top");
        top.setSelected(true);
        RadioButton bottom = new RadioButton("Bottom");

        ToggleGroup placementGroup = new ToggleGroup();
        top.setToggleGroup(placementGroup);
        bottom.setToggleGroup(placementGroup);

        placementRow.getChildren().addAll(placementLabel, top, bottom);

        // ============================================================
        // Languages section
        // ============================================================
        Label languagesLabel = new Label("Languages:");
        languagesLabel.getStyleClass().add("settings-section");

        // Language checkboxes in a grid - 4 columns
        GridPane languageGrid = new GridPane();
        languageGrid.setHgap(16);
        languageGrid.setVgap(4);
        languageGrid.setPadding(new Insets(8, 0, 8, 20));

        String[] languages = {
                "CSS", "FreeMarker", "Groovy", "HTML",
                "Java", "JavaScript", "JSON", "JSP",
                "JSTL", "JavaScript", "Protobuf", "Rust",
                "Sass"
        };

        // All languages selected by default
        for (int i = 0; i < languages.length; i++) {
            CheckBox cb = new CheckBox(languages[i]);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            languageGrid.add(cb, i % 4, i / 4);
        }

        // ============================================================
        // Manage colors button
        // ============================================================
        Button manageColors = new Button("Manage colors");
        manageColors.getStyleClass().add("dialog-secondary");
        manageColors.setOnAction(e -> {
            // Show color management dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Manage Colors");
            alert.setHeaderText("Manage Colors");
            alert.setContentText("Color management for breadcrumbs will be available in a future update.");
            alert.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/lumina-dark.css").toExternalForm());
            alert.showAndWait();
        });

        // ============================================================
        // Assemble
        // ============================================================
        getChildren().addAll(
                showBreadcrumbs,
                placementRow,
                languagesLabel,
                languageGrid,
                manageColors
        );
    }
}