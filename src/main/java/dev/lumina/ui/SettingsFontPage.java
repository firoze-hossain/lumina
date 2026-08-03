// SettingsFontPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > Font settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsFontPage extends VBox {

    public SettingsFontPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Font section
        // ============================================================
        HBox fontRow = new HBox(8);
        fontRow.setPadding(new Insets(4, 0, 8, 20));
        fontRow.setAlignment(Pos.CENTER_LEFT);

        Label fontLabel = new Label("Font:");
        fontLabel.getStyleClass().add("settings-label");
        ComboBox<String> fontCombo = new ComboBox<>();
        fontCombo.getItems().addAll(
            "JetBrains Mono",
            "Consolas",
            "Menlo",
            "Monaco",
            "Courier New",
            "Segoe UI",
            "SF Mono",
            "Fira Code",
            "Source Code Pro"
        );
        fontCombo.getSelectionModel().selectFirst();
        fontCombo.getStyleClass().add("settings-combo");
        fontCombo.setPrefWidth(200);
        fontRow.getChildren().addAll(fontLabel, fontCombo);

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
        // Checkboxes and button
        // ============================================================
        VBox optionsBox = new VBox(4);
        optionsBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox enableLigatures = new CheckBox("Enable ligatures");
        enableLigatures.setSelected(false);
        enableLigatures.getStyleClass().add("settings-check");

        CheckBox readerMode = new CheckBox("See line height and ligatures also in Reader mode");
        readerMode.setSelected(true);
        readerMode.getStyleClass().add("settings-check");

        Button typographySettings = new Button("Typography Settings");
        typographySettings.getStyleClass().add("dialog-secondary");
        typographySettings.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Typography Settings");
            alert.setHeaderText("Typography Settings");
            alert.setContentText("Typography settings will be available in a future update.");
            alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
            alert.showAndWait();
        });

        optionsBox.getChildren().addAll(enableLigatures, readerMode, typographySettings);

        // ============================================================
        // Preview section - Default
        // ============================================================
        Label defaultLabel = new Label("Default:");
        defaultLabel.getStyleClass().add("settings-label");

        Label defaultPreview = new Label("abcdefghijklmnopqrstuvwxyz\nABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789 ( ) { } [ ]\n+ - * / = . , ; ! ? # & $ % @ ^");
        defaultPreview.getStyleClass().add("font-preview");
        defaultPreview.setWrapText(true);
        defaultPreview.setPadding(new Insets(8, 12, 8, 12));
        defaultPreview.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");

        VBox defaultBox = new VBox(4);
        defaultBox.setPadding(new Insets(4, 0, 8, 20));
        defaultBox.getChildren().addAll(defaultLabel, defaultPreview);

        // ============================================================
        // Preview section - Bold
        // ============================================================
        Label boldLabel = new Label("Bold:");
        boldLabel.getStyleClass().add("settings-label");

        Label boldPreview = new Label("abcdefghijklmnopqrstuvwxyz\nABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789 ( ) { } [ ]\n+ - * / = . , ; ! ? # & $ % @ ^");
        boldPreview.getStyleClass().add("font-preview-bold");
        boldPreview.setWrapText(true);
        boldPreview.setPadding(new Insets(8, 12, 8, 12));
        boldPreview.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: bold;");

        VBox boldBox = new VBox(4);
        boldBox.setPadding(new Insets(4, 0, 8, 20));
        boldBox.getChildren().addAll(boldLabel, boldPreview);

        // ============================================================
        // Preview text field
        // ============================================================
        HBox previewRow = new HBox(8);
        previewRow.setPadding(new Insets(4, 0, 8, 20));
        previewRow.setAlignment(Pos.CENTER_LEFT);

        Label previewLabel = new Label("Enter any text to preview");
        previewLabel.getStyleClass().add("settings-label");
        TextField previewField = new TextField();
        previewField.setPromptText("Type here to preview the font");
        previewField.getStyleClass().add("text-field");
        previewField.setPrefWidth(300);
        previewRow.getChildren().addAll(previewLabel, previewField);

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            fontRow,
            sizeRow,
            optionsBox,
            defaultBox,
            boldBox,
            previewRow
        );
    }
}