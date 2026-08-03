// SettingsConsolePage.java
package dev.lumina.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Console settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsConsolePage extends VBox {

    private final ObservableList<String> foldPatterns = FXCollections.observableArrayList(
        "+",
        "-",
        "at com.intellij.jpa.",
        "at com.intellij.junit3.",
        "at com.intellij.junit4.",
        "at com.intellij.junit5.",
        "at com.intellij.junit6.",
        "at com.intellij.runtime.execution.",
        "at com.intellij.runtime.",
        "at com.jgoodies.binding.beans.ExtendedPropertyChangeSupport.firePropertyChange0(",
        "at com.sun.nroxy.$Proxy"
    );

    public SettingsConsolePage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Editor section
        // ============================================================
        Label editorLabel = new Label("Editor");
        editorLabel.getStyleClass().add("settings-section");

        CheckBox softWraps = new CheckBox("Use soft wraps in console");
        softWraps.setSelected(true);
        softWraps.getStyleClass().add("settings-check");

        VBox editorBox = new VBox(4);
        editorBox.setPadding(new Insets(4, 0, 8, 20));

        HBox historyRow = new HBox(8);
        historyRow.setAlignment(Pos.CENTER_LEFT);
        Label historyLabel = new Label("Console commands history size:");
        historyLabel.getStyleClass().add("settings-label");
        Spinner<Integer> historySpinner = new Spinner<>(10, 1000, 300, 10);
        historySpinner.setPrefWidth(80);
        historySpinner.getStyleClass().add("settings-spinner");
        historyRow.getChildren().addAll(historyLabel, historySpinner);

        HBox bufferRow = new HBox(8);
        bufferRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox overrideBuffer = new CheckBox("Override console cycle buffer size");
        overrideBuffer.setSelected(true);
        overrideBuffer.getStyleClass().add("settings-check");
        Spinner<Integer> bufferSpinner = new Spinner<>(128, 4096, 1024, 128);
        bufferSpinner.setPrefWidth(80);
        bufferSpinner.getStyleClass().add("settings-spinner");
        Label kbLabel = new Label("KB");
        kbLabel.getStyleClass().add("settings-label");
        bufferRow.getChildren().addAll(overrideBuffer, bufferSpinner, kbLabel);

        editorBox.getChildren().addAll(softWraps, historyRow, bufferRow);

        // ============================================================
        // Default Encoding section
        // ============================================================
        Label encodingLabel = new Label("Default Encoding");
        encodingLabel.getStyleClass().add("settings-section");

        HBox encodingRow = new HBox(8);
        encodingRow.setPadding(new Insets(4, 0, 8, 20));
        encodingRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> encodingCombo = new ComboBox<>();
        encodingCombo.getItems().addAll(
            "<System Default: UTF-8>",
            "UTF-8",
            "UTF-16",
            "ISO-8859-1",
            "Windows-1252",
            "US-ASCII"
        );
        encodingCombo.getSelectionModel().selectFirst();
        encodingCombo.getStyleClass().add("settings-combo");
        encodingCombo.setPrefWidth(200);
        encodingRow.getChildren().add(encodingCombo);

        // ============================================================
        // Fold console lines that contain section
        // ============================================================
        Label foldLabel = new Label("Fold console lines that contain:");
        foldLabel.getStyleClass().add("settings-section");

        VBox foldBox = new VBox(6);
        foldBox.setPadding(new Insets(4, 0, 8, 20));

        // Pattern list with toolbar
        ListView<String> patternList = new ListView<>(foldPatterns);
        patternList.getStyleClass().add("settings-list");
        patternList.setPrefHeight(180);

        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("property-button");
        addBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add Pattern");
            dialog.setHeaderText("Enter pattern to fold");
            dialog.setContentText("Pattern:");
            dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
            dialog.showAndWait().ifPresent(pattern -> {
                String trimmed = pattern.trim();
                if (!trimmed.isEmpty() && !foldPatterns.contains(trimmed)) {
                    foldPatterns.add(trimmed);
                }
            });
        });

        Button removeBtn = new Button("-");
        removeBtn.getStyleClass().add("property-button");
        removeBtn.setOnAction(e -> {
            String selected = patternList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                foldPatterns.remove(selected);
            }
        });

        toolbar.getChildren().addAll(addBtn, removeBtn);

        foldBox.getChildren().addAll(patternList, toolbar);

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            editorLabel,
            editorBox,
            encodingLabel,
            encodingRow,
            foldLabel,
            foldBox
        );
    }
}