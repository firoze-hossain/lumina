// SettingsInlineCompletionPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Inline Completion settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsInlineCompletionPage extends VBox {

    public SettingsInlineCompletionPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Go to Code Completion settings page - link
        // ============================================================
        Hyperlink codeCompletionLink = new Hyperlink("Go to Code Completion settings page to adjust lookup completion settings");
        codeCompletionLink.getStyleClass().add("settings-link");
        codeCompletionLink.setOnAction(e -> {
            // Navigate to Code Completion page
            // This would need to be handled by the parent SettingsDialog
            // For now, just show a message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Navigate");
            alert.setHeaderText("Code Completion Settings");
            alert.setContentText("This would navigate to the Code Completion settings page.");
            alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
            alert.showAndWait();
        });

        // ============================================================
        // Enable local Full Line completion suggestions
        // ============================================================
        CheckBox localCompletion = new CheckBox("Enable local Full Line completion suggestions");
        localCompletion.setSelected(true);
        localCompletion.getStyleClass().add("settings-check");

        Label localHint = new Label("Runs entirely on your local device without sending anything over the internet");
        localHint.getStyleClass().add("settings-hint");
        localHint.setWrapText(true);
        localHint.setPadding(new Insets(0, 0, 8, 20));

        // ============================================================
        // Language checkboxes with download sizes
        // ============================================================
        Label languagesLabel = new Label("Languages");
        languagesLabel.getStyleClass().add("settings-section");

        VBox languageBox = new VBox(4);
        languageBox.setPadding(new Insets(4, 0, 8, 20));

        // Kotlin
        CheckBox kotlin = new CheckBox("Kotlin");
        kotlin.setSelected(true);
        kotlin.getStyleClass().add("settings-check");

        // Java
        CheckBox java = new CheckBox("Java");
        java.setSelected(true);
        java.getStyleClass().add("settings-check");

        // CSS-like
        HBox cssRow = new HBox(8);
        cssRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox css = new CheckBox("CSS-like");
        css.setSelected(true);
        css.getStyleClass().add("settings-check");
        Label cssSize = new Label("Download (100 MB)");
        cssSize.getStyleClass().add("settings-hint");
        cssRow.getChildren().addAll(css, cssSize);

        // HTML
        HBox htmlRow = new HBox(8);
        htmlRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox html = new CheckBox("HTML");
        html.setSelected(true);
        html.getStyleClass().add("settings-check");
        Label htmlSize = new Label("Download (100 MB)");
        htmlSize.getStyleClass().add("settings-hint");
        htmlRow.getChildren().addAll(html, htmlSize);

        // JavaScript / TypeScript
        HBox jsRow = new HBox(8);
        jsRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox js = new CheckBox("JavaScript / TypeScript");
        js.setSelected(true);
        js.getStyleClass().add("settings-check");
        Label jsSize = new Label("Download (100 MB)");
        jsSize.getStyleClass().add("settings-hint");
        jsRow.getChildren().addAll(js, jsSize);

        // Rust
        HBox rustRow = new HBox(8);
        rustRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox rust = new CheckBox("Rust");
        rust.setSelected(true);
        rust.getStyleClass().add("settings-check");
        Label rustSize = new Label("Download (100 MB)");
        rustSize.getStyleClass().add("settings-hint");
        rustRow.getChildren().addAll(rust, rustSize);

        languageBox.getChildren().addAll(
            kotlin, java, cssRow, htmlRow, jsRow, rustRow
        );

        // ============================================================
        // Download models - Ask before downloading
        // ============================================================
        CheckBox askBeforeDownload = new CheckBox("Ask before downloading");
        askBeforeDownload.getStyleClass().add("settings-check");

        VBox downloadBox = new VBox(4);
        downloadBox.setPadding(new Insets(4, 0, 8, 20));
        downloadBox.getChildren().add(askBeforeDownload);

        // ============================================================
        // Enable cloud completion suggestions
        // ============================================================
        HBox cloudRow = new HBox(8);
        cloudRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox cloudCompletion = new CheckBox("Enable cloud completion suggestions");
        cloudCompletion.getStyleClass().add("settings-check");
        Hyperlink activationLink = new Hyperlink("Go to Activation...");
        activationLink.getStyleClass().add("settings-link");
        activationLink.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Activation");
            alert.setHeaderText("Cloud Completion Activation");
            alert.setContentText("Cloud completion activation is not available yet.");
            alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
            alert.showAndWait();
        });
        cloudRow.getChildren().addAll(cloudCompletion, activationLink);

        VBox cloudBox = new VBox(4);
        cloudBox.setPadding(new Insets(4, 0, 8, 20));
        cloudBox.getChildren().add(cloudRow);

        // ============================================================
        // Enable automatic completion on typing
        // ============================================================
        CheckBox autoCompletion = new CheckBox("Enable automatic completion on typing");
        autoCompletion.setSelected(true);
        autoCompletion.getStyleClass().add("settings-check");

        Label autoHint = new Label("If disabled, completion suggestions can still be invoked via Alt+Shift+\u2193 shortcut");
        autoHint.getStyleClass().add("settings-hint");
        autoHint.setWrapText(true);
        autoHint.setPadding(new Insets(0, 0, 8, 20));

        VBox autoBox = new VBox(4);
        autoBox.setPadding(new Insets(4, 0, 8, 20));
        autoBox.getChildren().addAll(autoCompletion, autoHint);

        // ============================================================
        // Enable multi-line suggestions
        // ============================================================
        CheckBox multiLine = new CheckBox("Enable multi-line suggestions");
        multiLine.setSelected(true);
        multiLine.getStyleClass().add("settings-check");

        Label multiHint = new Label("If disabled, only single-line suggestions will be shown");
        multiHint.getStyleClass().add("settings-hint");
        multiHint.setWrapText(true);
        multiHint.setPadding(new Insets(0, 0, 8, 20));

        VBox multiBox = new VBox(4);
        multiBox.setPadding(new Insets(4, 0, 8, 20));
        multiBox.getChildren().addAll(multiLine, multiHint);

        // ============================================================
        // Synchronize inline and popup completions
        // ============================================================
        CheckBox syncCompletion = new CheckBox("Synchronize inline and popup completions");
        syncCompletion.setSelected(true);
        syncCompletion.getStyleClass().add("settings-check");

        Label syncHint = new Label("When enabled, inline completions will be shown in the popup completion list to avoid shortcut conflicts");
        syncHint.getStyleClass().add("settings-hint");
        syncHint.setWrapText(true);
        syncHint.setPadding(new Insets(0, 0, 8, 20));

        VBox syncBox = new VBox(4);
        syncBox.setPadding(new Insets(4, 0, 8, 20));
        syncBox.getChildren().addAll(syncCompletion, syncHint);

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            codeCompletionLink,
            localCompletion,
            localHint,
            languagesLabel,
            languageBox,
            askBeforeDownload,
            cloudBox,
            autoBox,
            multiBox,
            syncBox
        );
    }
}