package dev.lumina.ui;

import dev.lumina.util.Settings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * IntelliJ IDEA-style Auto-Sync Settings dialog for Maven (Image 3).
 * Allows configuring automatic reload, dependency resolution, and source generation.
 */
public class AutoSyncSettingsDialog {

    public static final String KEY_AUTO_SYNC_POM = "maven.autoSync.pomChanges";
    public static final String KEY_AUTO_DOWNLOAD = "maven.autoSync.downloadDocs";
    public static final String KEY_AUTO_GEN_SOURCES = "maven.autoSync.generateSources";

    private final Stage stage = new Stage();
    private final CheckBox syncOnPomChanges = new CheckBox("Reload project after any changes in pom.xml");
    private final CheckBox autoDownload = new CheckBox("Automatically download sources and documentation");
    private final CheckBox autoGenSources = new CheckBox("Generate sources automatically on project build");

    public AutoSyncSettingsDialog(Stage owner) {
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Auto-Sync Settings");

        Label title = new Label("Auto-Sync Settings");
        title.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label subtitle = new Label("Configure automatic synchronization for Maven projects:");
        subtitle.setStyle("-fx-text-fill: #9DA0A8; -fx-font-size: 12px;");

        // Load saved values
        syncOnPomChanges.setSelected(!"false".equalsIgnoreCase(Settings.get(KEY_AUTO_SYNC_POM)));
        autoDownload.setSelected("true".equalsIgnoreCase(Settings.get(KEY_AUTO_DOWNLOAD)));
        autoGenSources.setSelected(!"false".equalsIgnoreCase(Settings.get(KEY_AUTO_GEN_SOURCES)));

        styleCheckBox(syncOnPomChanges);
        styleCheckBox(autoDownload);
        styleCheckBox(autoGenSources);

        VBox optionsBox = new VBox(12, syncOnPomChanges, autoDownload, autoGenSources);
        optionsBox.setPadding(new Insets(10, 0, 10, 0));

        Separator separator = new Separator();

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(e -> {
            save();
            stage.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBar = new HBox(8, spacer, cancelBtn, okBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(8, 0, 0, 0));

        VBox root = new VBox(12, title, subtitle, optionsBox, separator, buttonBar);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1;");

        Scene scene = new Scene(root, 480, 240);
        scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });

        stage.setScene(scene);
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void save() {
        Settings.put(KEY_AUTO_SYNC_POM, String.valueOf(syncOnPomChanges.isSelected()));
        Settings.put(KEY_AUTO_DOWNLOAD, String.valueOf(autoDownload.isSelected()));
        Settings.put(KEY_AUTO_GEN_SOURCES, String.valueOf(autoGenSources.isSelected()));
    }

    public void show() {
        stage.centerOnScreen();
        stage.showAndWait();
    }
}
