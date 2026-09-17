package dev.lumina.ui;

import dev.lumina.project.ExpressMetadata;
import dev.lumina.project.ReactMetadata;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.function.Consumer;

/**
 * Modal dialog replicating IntelliJ IDEA's "Specify Version" / "Select Package Version" dialog
 * when clicking 'Select...' in the CLI dropdown or the browse button.
 */
public class SelectCliVersionDialog {

    private final Stage stage;
    private final Consumer<String> onSelected;
    private final ComboBox<String> versionBox = new ComboBox<>();
    private final Button okBtn = new Button("OK");
    private final Button cancelBtn = new Button("Cancel");

    public SelectCliVersionDialog(Stage owner, String projectType, String currentVersion, Consumer<String> onSelected) {
        this.stage = new Stage();
        this.onSelected = onSelected;

        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);

        String pkgName = "Express".equalsIgnoreCase(projectType)
                ? ExpressMetadata.PACKAGE_NAME
                : ReactMetadata.getCliPackage(projectType);
        stage.setTitle("Specify " + pkgName + " Version");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "download-node-dialog");

        VBox content = new VBox(12);
        content.setPadding(new Insets(18, 22, 12, 22));

        Label headerLabel = new Label("Specify version for " + pkgName + ":");
        headerLabel.getStyleClass().add("form-label");
        headerLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #E6E9F2;");

        versionBox.setEditable(true);
        versionBox.getStyleClass().add("choice-box");
        versionBox.setMaxWidth(Double.MAX_VALUE);

        // Load versions
        List<String> versions = "Express".equalsIgnoreCase(projectType)
                ? ExpressMetadata.fetchAllVersions(false)
                : ReactMetadata.fetchAllVersions(projectType, false);
        versionBox.getItems().setAll(versions);

        if (currentVersion != null && !currentVersion.isBlank()) {
            versionBox.setValue(currentVersion);
        } else if (!versions.isEmpty()) {
            versionBox.setValue(versions.getFirst());
        }

        // Asynchronously refresh versions from npm registry in background
        Thread.ofVirtual().start(() -> {
            List<String> remote = "Express".equalsIgnoreCase(projectType)
                    ? ExpressMetadata.fetchAllVersions(true)
                    : ReactMetadata.fetchAllVersions(projectType, true);
            if (remote != null && !remote.isEmpty()) {
                Platform.runLater(() -> {
                    String cur = versionBox.getValue();
                    versionBox.getItems().setAll(remote);
                    if (cur != null && !cur.isBlank()) {
                        versionBox.setValue(cur);
                    } else {
                        versionBox.setValue(remote.getFirst());
                    }
                });
            }
        });

        content.getChildren().addAll(headerLabel, versionBox);

        // Bottom buttons
        cancelBtn.getStyleClass().add("dialog-secondary");
        cancelBtn.setOnAction(e -> stage.close());

        okBtn.getStyleClass().add("dialog-primary");
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(e -> {
            String val = versionBox.getValue();
            if (val != null && !val.isBlank()) {
                if (this.onSelected != null) {
                    this.onSelected.accept(val.trim());
                }
                stage.close();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBar = new HBox(10, spacer, cancelBtn, okBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 20, 14, 20));
        buttonBar.getStyleClass().add("dialog-footer");

        root.setCenter(content);
        root.setBottom(buttonBar);

        Scene scene = new Scene(root, 440, 170);
        try {
            var css = getClass().getResource("/css/lumina-dark.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.setMinWidth(400);
        stage.setMinHeight(160);
    }

    public void show() {
        stage.showAndWait();
    }

    public ComboBox<String> getVersionBox() {
        return versionBox;
    }

    public Button getOkButton() {
        return okBtn;
    }

    public Button getCancelButton() {
        return cancelBtn;
    }
}
