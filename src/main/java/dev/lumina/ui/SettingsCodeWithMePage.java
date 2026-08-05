// SettingsCodeWithMePage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > Color Scheme > Code With Me settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsCodeWithMePage extends VBox {

    public SettingsCodeWithMePage() {
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
        // User cursor/selection color grid
        // ============================================================
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);
        grid.setPadding(new Insets(12, 0, 12, 0));

        // Headers
        Label userLabel = new Label("User");
        userLabel.getStyleClass().add("settings-label");
        userLabel.setStyle("-fx-font-weight: bold;");
        Label cursorLabel = new Label("cursor");
        cursorLabel.getStyleClass().add("settings-label");
        cursorLabel.setStyle("-fx-font-weight: bold;");
        Label selectionLabel = new Label("selection");
        selectionLabel.getStyleClass().add("settings-label");
        selectionLabel.setStyle("-fx-font-weight: bold;");

        grid.add(userLabel, 0, 0);
        grid.add(cursorLabel, 1, 0);
        grid.add(selectionLabel, 2, 0);

        // User rows with color preview and combo boxes
        String[] users = {"User1", "User2", "User3", "User4", "User5"};
        String[] colors = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7"};

        for (int i = 0; i < users.length; i++) {
            Label user = new Label(users[i]);
            user.getStyleClass().add("settings-label");

            // Cursor color - color preview with dropdown
            HBox cursorBox = new HBox(6);
            cursorBox.setAlignment(Pos.CENTER_LEFT);
            Label cursorColor = new Label("■");
            cursorColor.setStyle("-fx-text-fill: " + colors[i] + "; -fx-font-size: 18px;");
            ComboBox<String> cursorCombo = new ComboBox<>();
            cursorCombo.getItems().addAll("Default", "Red", "Blue", "Green", "Yellow", "Cyan", "Magenta");
            cursorCombo.getSelectionModel().selectFirst();
            cursorCombo.getStyleClass().add("settings-combo");
            cursorCombo.setPrefWidth(120);
            cursorBox.getChildren().addAll(cursorColor, cursorCombo);

            // Selection color - color preview with dropdown
            HBox selectionBox = new HBox(6);
            selectionBox.setAlignment(Pos.CENTER_LEFT);
            Label selectionColor = new Label("■");
            selectionColor.setStyle("-fx-text-fill: " + colors[i] + "88; -fx-font-size: 18px;");
            ComboBox<String> selectionCombo = new ComboBox<>();
            selectionCombo.getItems().addAll("Default", "Red", "Blue", "Green", "Yellow", "Cyan", "Magenta");
            selectionCombo.getSelectionModel().selectFirst();
            selectionCombo.getStyleClass().add("settings-combo");
            selectionCombo.setPrefWidth(120);
            selectionBox.getChildren().addAll(selectionColor, selectionCombo);

            grid.add(user, 0, i + 1);
            grid.add(cursorBox, 1, i + 1);
            grid.add(selectionBox, 2, i + 1);
        }

        // ============================================================
        // Preview area
        // ============================================================
        VBox previewBox = new VBox(6);
        previewBox.setPadding(new Insets(8, 12, 8, 12));
        previewBox.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label previewTitle = new Label("// JSON File as an example");
        previewTitle.getStyleClass().add("sx-comment");

        Label previewLine1 = new Label("/* Some block comment */");
        previewLine1.getStyleClass().add("sx-comment");

        Label previewLine2 = new Label("\"keywords\": [");
        previewLine2.getStyleClass().add("sx-string");

        Label previewLine3 = new Label("  true,");
        previewLine3.getStyleClass().add("sx-keyword");

        Label previewLine4 = new Label("  false,");
        previewLine4.getStyleClass().add("sx-keyword");

        Label previewLine5 = new Label("  null");
        previewLine5.getStyleClass().add("sx-keyword");

        Label previewLine6 = new Label("],");
        previewLine6.getStyleClass().add("sx-string");

        Label previewLine7 = new Label("\"strings\": {");
        previewLine7.getStyleClass().add("sx-string");

        Label previewLine8 = new Label("  \"no escapes\": \"pseudopolynomiality\",");
        previewLine8.getStyleClass().add("sx-string");

        Label previewLine9 = new Label("  \"escapes\": \"C-style\\nandunicode\\u0021\"");
        previewLine9.getStyleClass().add("sx-string");

        Label previewLine10 = new Label("},");
        previewLine10.getStyleClass().add("sx-string");

        Label previewLine11 = new Label("\"Some numbers\": [");
        previewLine11.getStyleClass().add("sx-string");

        Label previewLine12 = new Label("  42,");
        previewLine12.getStyleClass().add("sx-number");

        Label previewLine13 = new Label("  -0.0e-0,");
        previewLine13.getStyleClass().add("sx-number");

        Label previewLine14 = new Label("  6.626e-34");
        previewLine14.getStyleClass().add("sx-number");

        Label previewLine15 = new Label("]");
        previewLine15.getStyleClass().add("sx-string");

        previewBox.getChildren().addAll(
            previewTitle, previewLine1, previewLine2, previewLine3, previewLine4,
            previewLine5, previewLine6, previewLine7, previewLine8, previewLine9,
            previewLine10, previewLine11, previewLine12, previewLine13, previewLine14,
            previewLine15
        );

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(schemeRow, grid, previewBox);
    }
}