package dev.lumina.ui;

import dev.lumina.plugin.RequiredPlugin;
import dev.lumina.plugin.RequiredPluginsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Modal dialog for adding or editing a Required Plugin.
 * Strictly matches media_1790045975642.png.
 */
public class RequiredPluginDialog {

    private final Stage stage;
    private final ComboBox<String> pluginCombo = new ComboBox<>();
    private final TextField minVersionField = new TextField();
    private final TextField maxVersionField = new TextField();
    private RequiredPlugin result = null;

    public RequiredPluginDialog(Window owner, RequiredPlugin existing) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Required Plugin");
        stage.setResizable(false);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");
        root.setPadding(new Insets(16, 20, 16, 20));

        // Form Grid
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER_LEFT);

        Label pluginLbl = new Label("Plugin:");
        pluginLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        pluginCombo.setEditable(true);
        pluginCombo.getItems().setAll(RequiredPluginsManager.getInstance().getAvailablePluginNames());
        pluginCombo.setPrefWidth(260);
        pluginCombo.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        Label minLbl = new Label("Minimum version:");
        minLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        minVersionField.setPrefWidth(260);
        minVersionField.setPromptText("");
        minVersionField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8 5 8; -fx-font-size: 12px;");

        Label maxLbl = new Label("Maximum version:");
        maxLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        maxVersionField.setPrefWidth(260);
        maxVersionField.setPromptText("");
        maxVersionField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8 5 8; -fx-font-size: 12px;");

        // Prepopulate if editing
        if (existing != null) {
            pluginCombo.setValue(existing.getPluginName());
            minVersionField.setText(existing.getMinVersion());
            maxVersionField.setText(existing.getMaxVersion());
        } else if (!pluginCombo.getItems().isEmpty()) {
            pluginCombo.getSelectionModel().selectFirst();
        }

        grid.add(pluginLbl, 0, 0);
        grid.add(pluginCombo, 1, 0);
        grid.add(minLbl, 0, 1);
        grid.add(minVersionField, 1, 1);
        grid.add(maxLbl, 0, 2);
        grid.add(maxVersionField, 1, 2);

        root.setCenter(grid);

        // Bottom Button Bar
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(16, 0, 0, 0));

        Button helpBtn = new Button("?");
        helpBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #4E5157; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px; -fx-max-width: 24px; -fx-max-height: 24px; -fx-padding: 0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        cancelBtn.setOnAction(e -> stage.close());

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        okBtn.setOnAction(e -> {
            String name = pluginCombo.getValue();
            if (name != null && !name.trim().isEmpty()) {
                String cleanName = name.trim();
                String id = cleanName.toLowerCase().replaceAll("[^a-z0-9.]", "");
                result = new RequiredPlugin(id, cleanName, minVersionField.getText().trim(), maxVersionField.getText().trim());
            }
            stage.close();
        });

        buttonBar.getChildren().addAll(helpBtn, spacer, cancelBtn, okBtn);
        root.setBottom(buttonBar);

        Scene scene = new Scene(root, 400, 200);
        stage.setScene(scene);
    }

    public RequiredPlugin showAndWait() {
        stage.showAndWait();
        return result;
    }
}
