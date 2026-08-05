// SettingsColorSchemeFontPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > Color Scheme > Color Scheme Font settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsColorSchemeFontPage extends VBox {

    public SettingsColorSchemeFontPage() {
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
        // Use color scheme font checkbox
        // ============================================================
        CheckBox useColorSchemeFont = new CheckBox("Use color scheme font instead of the default (JetBrains Mono, 13)");
        useColorSchemeFont.setSelected(true);
        useColorSchemeFont.getStyleClass().add("settings-check");

        // ============================================================
        // Font row
        // ============================================================
        HBox fontRow = new HBox(8);
        fontRow.setPadding(new Insets(4, 0, 4, 20));
        fontRow.setAlignment(Pos.CENTER_LEFT);

        Label fontLabel = new Label("Font:");
        fontLabel.getStyleClass().add("settings-label");
        ComboBox<String> fontCombo = new ComboBox<>();
        fontCombo.getItems().addAll("JetBrains Mono", "Consolas", "Menlo", "Monaco", "Courier New", "Fira Code");
        fontCombo.getSelectionModel().selectFirst();
        fontCombo.getStyleClass().add("settings-combo");
        fontCombo.setPrefWidth(180);
        fontRow.getChildren().addAll(fontLabel, fontCombo);

        // ============================================================
        // Fallback font row
        // ============================================================
        HBox fallbackRow = new HBox(8);
        fallbackRow.setPadding(new Insets(4, 0, 4, 20));
        fallbackRow.setAlignment(Pos.CENTER_LEFT);

        Label fallbackLabel = new Label("Fallback font:");
        fallbackLabel.getStyleClass().add("settings-label");
        ComboBox<String> fallbackCombo = new ComboBox<>();
        fallbackCombo.getItems().addAll("<None> Non-latin", "Noto Sans", "Segoe UI", "Arial Unicode MS");
        fallbackCombo.getSelectionModel().selectFirst();
        fallbackCombo.getStyleClass().add("settings-combo");
        fallbackCombo.setPrefWidth(180);
        fallbackRow.getChildren().addAll(fallbackLabel, fallbackCombo);

        // ============================================================
        // Show only monospaced fonts
        // ============================================================
        CheckBox showMonospaced = new CheckBox("Show only monospaced fonts");
        showMonospaced.setSelected(true);
        showMonospaced.getStyleClass().add("settings-check");

        Label nonLatinHint = new Label("Used for symbols not supported by the main font");
        nonLatinHint.getStyleClass().add("settings-hint");
        nonLatinHint.setPadding(new Insets(0, 0, 8, 20));

        // ============================================================
        // Size and Line height
        // ============================================================
        HBox sizeRow = new HBox(8);
        sizeRow.setPadding(new Insets(4, 0, 8, 20));
        sizeRow.setAlignment(Pos.CENTER_LEFT);

        Label sizeLabel = new Label("Size:");
        sizeLabel.getStyleClass().add("settings-label");
        Spinner<Double> sizeSpinner = new Spinner<>(8.0, 30.0, 13.0, 0.5);
        sizeSpinner.setPrefWidth(70);
        sizeSpinner.getStyleClass().add("settings-spinner");

        Label lineHeightLabel = new Label("Line height:");
        lineHeightLabel.getStyleClass().add("settings-label");
        Spinner<Double> lineHeightSpinner = new Spinner<>(1.0, 3.0, 1.2, 0.1);
        lineHeightSpinner.setPrefWidth(70);
        lineHeightSpinner.getStyleClass().add("settings-spinner");

        sizeRow.getChildren().addAll(sizeLabel, sizeSpinner, lineHeightLabel, lineHeightSpinner);

        // ============================================================
        // Enable ligatures and Reader mode
        // ============================================================
        CheckBox enableLigatures = new CheckBox("Enable ligatures");
        enableLigatures.setSelected(false);
        enableLigatures.getStyleClass().add("settings-check");

        CheckBox readerMode = new CheckBox("See line height and ligatures also in Reader mode");
        readerMode.setSelected(true);
        readerMode.getStyleClass().add("settings-check");

        // ============================================================
        // Preview text area
        // ============================================================
        TextArea previewText = new TextArea(
                "IntelliJ IDEA is an Integrated\n" +
                        "Development Environment (IDE) designed\n" +
                        "to maximize productivity. It provides\n" +
                        "clever code completion, static code\n" +
                        "analysis, and refactorings, and lets\n" +
                        "you focus on the bright side of\n" +
                        "software development making\n" +
                        "it an enjoyable experience."
        );
        previewText.setEditable(false);
        previewText.setWrapText(true);
        previewText.getStyleClass().add("settings-preview-text");
        previewText.setPrefHeight(150);
        previewText.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #D8DBE6; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 13px;");

        VBox previewBox = new VBox(4);
        previewBox.setPadding(new Insets(4, 0, 8, 0));
        previewBox.getChildren().add(previewText);

        // ============================================================
        // Default preview
        // ============================================================
        Label defaultLabel = new Label("Default:");
        defaultLabel.getStyleClass().add("settings-label");

        Label defaultPreview = new Label("abcdefghijklmnopqrstuvwxyz\nABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789 ( ) { } [ ]\n+ - * / = . , ; ! ? # & $ % @ ^");
        defaultPreview.getStyleClass().add("font-preview");
        defaultPreview.setWrapText(true);
        defaultPreview.setPadding(new Insets(8, 12, 8, 12));
        defaultPreview.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 13px;");

        VBox defaultBox = new VBox(4);
        defaultBox.setPadding(new Insets(4, 0, 8, 20));
        defaultBox.getChildren().addAll(defaultLabel, defaultPreview);

        // ============================================================
        // Enter any text to preview
        // ============================================================
        HBox previewRow = new HBox(8);
        previewRow.setPadding(new Insets(4, 0, 8, 20));
        previewRow.setAlignment(Pos.CENTER_LEFT);

        Label previewInputLabel = new Label("Enter any text to preview");
        previewInputLabel.getStyleClass().add("settings-label");
        TextField previewInput = new TextField();
        previewInput.setPromptText("Type here to preview the font");
        previewInput.getStyleClass().add("text-field");
        previewInput.setPrefWidth(300);
        previewRow.getChildren().addAll(previewInputLabel, previewInput);

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
                schemeRow,
                useColorSchemeFont,
                fontRow,
                fallbackRow,
                showMonospaced,
                nonLatinHint,
                sizeRow,
                enableLigatures,
                readerMode,
                previewBox,
                defaultBox,
                previewRow
        );
    }
}