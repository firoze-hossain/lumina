// SettingsConsoleColorsPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > Color Scheme > Console Colors settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsConsoleColorsPage extends VBox {

    public SettingsConsoleColorsPage() {
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
        // Color grid with checkboxes and color pickers
        // ============================================================
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(6);
        grid.setPadding(new Insets(12, 0, 12, 0));

        // Headers
        Label colorLabel = new Label("Color");
        colorLabel.getStyleClass().add("settings-label");
        colorLabel.setStyle("-fx-font-weight: bold;");
        Label boldLabel = new Label("Bold");
        boldLabel.getStyleClass().add("settings-label");
        boldLabel.setStyle("-fx-font-weight: bold;");
        Label italicLabel = new Label("Italic");
        italicLabel.getStyleClass().add("settings-label");
        italicLabel.setStyle("-fx-font-weight: bold;");
        Label foregroundLabel = new Label("Foreground");
        foregroundLabel.getStyleClass().add("settings-label");
        foregroundLabel.setStyle("-fx-font-weight: bold;");
        Label backgroundLabel = new Label("Background");
        backgroundLabel.getStyleClass().add("settings-label");
        backgroundLabel.setStyle("-fx-font-weight: bold;");
        Label errorLabel = new Label("Error stripe mark");
        errorLabel.getStyleClass().add("settings-label");
        errorLabel.setStyle("-fx-font-weight: bold;");
        Label effectsLabel = new Label("Effects");
        effectsLabel.getStyleClass().add("settings-label");
        effectsLabel.setStyle("-fx-font-weight: bold;");

        grid.add(colorLabel, 0, 0);
        grid.add(boldLabel, 1, 0);
        grid.add(italicLabel, 2, 0);
        grid.add(foregroundLabel, 3, 0);
        grid.add(backgroundLabel, 4, 0);
        grid.add(errorLabel, 5, 0);
        grid.add(effectsLabel, 6, 0);

        // Color rows
        String[] colors = {
            "Blue", "Bright Black", "Bright Blue", "Bright Cyan", "Bright Green",
            "Bright Magenta", "Bright Red", "Bright White", "Bright Yellow",
            "Cyan", "Green", "Magenta", "Red", "White (Gray)", "Yellow"
        };

        for (int i = 0; i < colors.length; i++) {
            Label colorName = new Label(colors[i]);
            colorName.getStyleClass().add("settings-label");

            CheckBox bold = new CheckBox();
            bold.setSelected(i % 3 == 0);
            bold.getStyleClass().add("settings-check");

            CheckBox italic = new CheckBox();
            italic.setSelected(i % 2 == 0);
            italic.getStyleClass().add("settings-check");

            // Foreground color picker
            Label fgColor = new Label("■");
            fgColor.setStyle("-fx-text-fill: #1FB0FF; -fx-font-size: 16px;");
            Button fgPicker = new Button("1FB0FF");
            fgPicker.getStyleClass().add("dialog-secondary");
            fgPicker.setPrefWidth(70);

            // Background color picker
            Label bgColor = new Label("■");
            bgColor.setStyle("-fx-text-fill: #1778BD; -fx-font-size: 16px;");
            Button bgPicker = new Button("1778BD");
            bgPicker.getStyleClass().add("dialog-secondary");
            bgPicker.setPrefWidth(70);

            // Error stripe mark
            Label stripeColor = new Label("■");
            stripeColor.setStyle("-fx-text-fill: #E5534B; -fx-font-size: 16px;");
            Button stripePicker = new Button("Choose...");
            stripePicker.getStyleClass().add("dialog-secondary");
            stripePicker.setPrefWidth(70);

            // Effects
            CheckBox bordered = new CheckBox("Bordered");
            bordered.setSelected(i % 2 == 0);
            bordered.getStyleClass().add("settings-check");

            grid.add(colorName, 0, i + 1);
            grid.add(bold, 1, i + 1);
            grid.add(italic, 2, i + 1);
            grid.add(fgPicker, 3, i + 1);
            grid.add(bgPicker, 4, i + 1);
            grid.add(stripePicker, 5, i + 1);
            grid.add(bordered, 6, i + 1);
        }

        // ============================================================
        // Inherit values from section
        // ============================================================
        HBox inheritRow = new HBox(8);
        inheritRow.setPadding(new Insets(8, 0, 8, 0));
        inheritRow.setAlignment(Pos.CENTER_LEFT);

        Label inheritLabel = new Label("Inherit values from:");
        inheritLabel.getStyleClass().add("settings-label");

        ComboBox<String> inheritCombo = new ComboBox<>();
        inheritCombo.getItems().addAll("ANSI colors → Blue", "ANSI colors → Default", "ANSI colors → Green");
        inheritCombo.getSelectionModel().selectFirst();
        inheritCombo.getStyleClass().add("settings-combo");
        inheritCombo.setPrefWidth(200);

        Label ansiLabel = new Label("(Console Colors)");
        ansiLabel.getStyleClass().add("settings-hint");

        inheritRow.getChildren().addAll(inheritLabel, inheritCombo, ansiLabel);

        // ============================================================
        // ANSI color preview rows
        // ============================================================
        VBox ansiBox = new VBox(4);
        ansiBox.setPadding(new Insets(8, 0, 8, 0));

        Label ansiHeader = new Label("ANSI colors:");
        ansiHeader.getStyleClass().add("settings-label");

        String[] ansiColors = {"ANSI:bright blue", "ANSI:bright magenta", "ANSI:bright cyan"};
        for (String ansi : ansiColors) {
            Label ansiItem = new Label(ansi);
            ansiItem.getStyleClass().add("settings-label");
            ansiItem.setStyle("-fx-text-fill: #56A8F5;");
            ansiBox.getChildren().add(ansiItem);
        }

        // ============================================================
        // Preview area
        // ============================================================
        VBox previewBox = new VBox(4);
        previewBox.setPadding(new Insets(8, 12, 8, 12));
        previewBox.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label previewLine1 = new Label("git log");
        previewLine1.setStyle("-fx-text-fill: #8FCE8F;");
        Label previewLine2 = new Label("Process finished with exit code 1");
        previewLine2.setStyle("-fx-text-fill: #E5534B;");

        previewBox.getChildren().addAll(previewLine1, previewLine2);

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            schemeRow,
            grid,
            inheritRow,
            ansiHeader,
            ansiBox,
            previewBox
        );
    }
}