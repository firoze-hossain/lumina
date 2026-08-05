// SettingsVCSPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsVCSPage extends VBox {

    public SettingsVCSPage() {
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

        // Editor Gutter section
        Label gutterLabel = new Label("Editor Gutter");
        gutterLabel.getStyleClass().add("settings-section");

        VBox gutterBox = new VBox(4);
        gutterBox.setPadding(new Insets(4, 0, 12, 0));
        String[] gutterItems = {
            "Added ignored lines border", "Added lines", "Border",
            "Changed lines popup", "Deleted ignored lines border",
            "Deleted lines", "Modified ignored lines border",
            "Modified lines", "Whitespace-modified lines"
        };
        for (String item : gutterItems) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label colorPreview = new Label("■");
            colorPreview.setStyle("-fx-text-fill: #3A3122; -fx-font-size: 16px;");
            Button chooseBtn = new Button("Choose...");
            chooseBtn.getStyleClass().add("dialog-secondary");
            chooseBtn.setPrefWidth(80);
            Label itemLabel = new Label(item);
            itemLabel.getStyleClass().add("settings-label");
            row.getChildren().addAll(colorPreview, chooseBtn, itemLabel);
            gutterBox.getChildren().add(row);
        }

        // VCS Annotations section
        Label annotationsLabel = new Label("VCS Annotations");
        annotationsLabel.getStyleClass().add("settings-section");

        HBox bgRow = new HBox(8);
        bgRow.setPadding(new Insets(4, 0, 8, 20));
        bgRow.setAlignment(Pos.CENTER_LEFT);
        Label bgLabel = new Label("Background color #1");
        bgLabel.getStyleClass().add("settings-label");
        Label bgColor = new Label("■");
        bgColor.setStyle("-fx-text-fill: #2A4A2A; -fx-font-size: 16px;");
        Button bgChoose = new Button("Choose...");
        bgChoose.getStyleClass().add("dialog-secondary");
        bgRow.getChildren().addAll(bgLabel, bgColor, bgChoose);

        // Annotation items
        VBox annoBox = new VBox(4);
        annoBox.setPadding(new Insets(4, 0, 12, 20));
        String[] annoItems = {
            "Deleted line below", "Modified line", "Added line",
            "Line with modified whitespaces", "Added line",
            "Line with modified whitespaces and deletion after"
        };
        for (int i = 0; i < annoItems.length; i++) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label num = new Label((i + 1) + ".");
            num.getStyleClass().add("settings-label");
            Label colorPreview = new Label("■");
            colorPreview.setStyle("-fx-text-fill: #6AAB73; -fx-font-size: 14px;");
            Button chooseBtn = new Button("Choose...");
            chooseBtn.getStyleClass().add("dialog-secondary");
            chooseBtn.setPrefWidth(70);
            Label itemLabel = new Label(annoItems[i]);
            itemLabel.getStyleClass().add("settings-label");
            row.getChildren().addAll(num, colorPreview, chooseBtn, itemLabel);
            annoBox.getChildren().add(row);
        }

        // Settings section
        Label settingsLabel = new Label("Settings");
        settingsLabel.getStyleClass().add("settings-section");

        HBox settingsRow = new HBox(16);
        settingsRow.setPadding(new Insets(4, 0, 8, 20));
        settingsRow.setAlignment(Pos.CENTER_LEFT);
        String[] settings = {"Bold", "Italic", "Foreground", "Background", "Error stripe mark", "Effects", "Underscored"};
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

        getChildren().addAll(schemeRow, gutterLabel, gutterBox, annotationsLabel, bgRow, annoBox, settingsLabel, settingsRow);
    }
}