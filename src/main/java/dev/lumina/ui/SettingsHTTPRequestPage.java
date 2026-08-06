// SettingsHTTPRequestPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsHTTPRequestPage extends VBox {

    public SettingsHTTPRequestPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(14);

        // Scheme row
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

        // Color items
        VBox itemsBox = new VBox(4);
        itemsBox.setPadding(new Insets(12, 0, 12, 0));

        String[] items = {
            "Comment", "Dynamic Variable", "Number arguments", "String arguments",
            "Environment Variable", "Escaped Value", "Header Field Key",
            "Header Field Value", "HTTP protocol", "Import", "In-Place Variable",
            "Input File Path", "Input File Sign", "Method Type"
        };
        for (String item : items) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label colorPreview = new Label("■");
            colorPreview.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 16px;");
            Button chooseBtn = new Button("Choose...");
            chooseBtn.getStyleClass().add("dialog-secondary");
            chooseBtn.setPrefWidth(80);
            Label itemLabel = new Label(item);
            itemLabel.getStyleClass().add("settings-label");
            row.getChildren().addAll(colorPreview, chooseBtn, itemLabel);
            itemsBox.getChildren().add(row);
        }

        // Settings section
        Label settingsLabel = new Label("Settings");
        settingsLabel.getStyleClass().add("settings-section");

        HBox settingsRow = new HBox(16);
        settingsRow.setPadding(new Insets(4, 0, 8, 20));
        settingsRow.setAlignment(Pos.CENTER_LEFT);
        String[] settings = {"Bold", "Italic", "Foreground", "Background", "Error stripe mark", "Effects", "Bordered"};
        for (String s : settings) {
            if (s.equals("Foreground") || s.equals("Background") || s.equals("Error stripe mark")) {
                HBox box = new HBox(4);
                box.setAlignment(Pos.CENTER_LEFT);
                Label color = new Label("■");
                color.setStyle("-fx-text-fill: #1FB0FF; -fx-font-size: 14px;");
                Button btn = new Button("Choose...");
                btn.getStyleClass().add("dialog-secondary");
                btn.setPrefWidth(70);
                box.getChildren().addAll(color, btn);
                settingsRow.getChildren().add(box);
            } else {
                CheckBox cb = new CheckBox(s);
                cb.setSelected(true);
                cb.getStyleClass().add("settings-check");
                settingsRow.getChildren().add(cb);
            }
        }

        // Preview area
        VBox previewBox = new VBox(4);
        previewBox.setPadding(new Insets(12));
        previewBox.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");
        Label previewLine1 = new Label("### First separator's content is treated as request name");
        previewLine1.setStyle("-fx-text-fill: #7A7E85; -fx-font-style: italic;");
        Label previewLine2 = new Label("### Other request separators can contain comments");
        previewLine2.setStyle("-fx-text-fill: #7A7E85; -fx-font-style: italic;");
        Label previewLine3 = new Label("POST http://{{host}}:8080/update-value?id={{item-id}} HTTP/2");
        previewLine3.setStyle("-fx-text-fill: #CF8E6D;");
        Label previewLine4 = new Label("Content-Type: application/json");
        previewLine4.setStyle("-fx-text-fill: #56A8F5;");
        Label previewLine5 = new Label("X-Request-ID: {{$random.integer(10, 1000)}}");
        previewLine5.setStyle("-fx-text-fill: #6AAB73;");
        Label previewLine6 = new Label("</input-file.json");
        previewLine6.setStyle("-fx-text-fill: #6AAB73;");
        previewBox.getChildren().addAll(previewLine1, previewLine2, previewLine3, previewLine4, previewLine5, previewLine6);

        getChildren().addAll(schemeRow, itemsBox, settingsLabel, settingsRow, previewBox);
    }
}