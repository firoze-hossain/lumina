package dev.lumina.ui;

import dev.lumina.git.GitService;
import dev.lumina.notification.Notification;
import dev.lumina.notification.NotificationService;
import dev.lumina.notification.NotificationType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * IntelliJ IDEA-styled "Create Patch" dialog matching Git > Patch > Create Patch from Local Changes...
 */
public class CreatePatchDialog extends Stage {

    private final Path projectRoot;
    private final Consumer<String> log;
    private final Runnable onCreated;

    private final TextField pathField = new TextField();
    private final Button browseBtn = new Button("Browse\u2026");
    private final CheckBox reversePatchCheck = new CheckBox("Reverse patch");
    private final Button createPatchBtn = new Button("Create Patch");
    private final Button cancelBtn = new Button("Cancel");
    private final Button helpBtn = new Button("?");

    public CreatePatchDialog(Window owner, Path projectRoot, Consumer<String> log, Runnable onCreated) {
        this.projectRoot = projectRoot;
        this.log = log;
        this.onCreated = onCreated;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Create Patch");
        setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(18, 20, 16, 20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        root.setPrefWidth(520);

        // 1. Destination file row
        Label fileLabel = new Label("Save to file:");
        fileLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        String defaultName = (projectRoot != null ? projectRoot.getFileName().toString() : "changes") + ".patch";
        Path defaultTarget = projectRoot != null ? projectRoot.resolve(defaultName) : Path.of(defaultName);
        pathField.setText(defaultTarget.toAbsolutePath().normalize().toString());
        pathField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px; -fx-padding: 5 8 5 8;");
        HBox.setHgrow(pathField, Priority.ALWAYS);

        browseBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5; -fx-padding: 5 12 5 12; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Patch Destination");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Patch Files (*.patch, *.diff)", "*.patch", "*.diff"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            if (projectRoot != null) {
                fc.setInitialDirectory(projectRoot.toFile());
            }
            fc.setInitialFileName(defaultName);
            File f = fc.showSaveDialog(this);
            if (f != null) {
                pathField.setText(f.getAbsolutePath());
            }
        });

        HBox pathRow = new HBox(8, pathField, browseBtn);
        pathRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Options
        reversePatchCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");

        // 3. Bottom Action Bar
        helpBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-weight: bold; -fx-min-width: 24; -fx-min-height: 24; -fx-cursor: hand;");
        helpBtn.setTooltip(new Tooltip("Create standard Unified Diff patch file from local changes"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        createPatchBtn.setDefaultButton(true);
        createPatchBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 5 18 5 18; -fx-background-radius: 4; -fx-cursor: hand;");
        createPatchBtn.setOnAction(e -> doCreatePatch());

        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5; -fx-padding: 5 14 5 14; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        HBox bottomBar = new HBox(8, helpBtn, spacer, createPatchBtn, cancelBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(fileLabel, pathRow, reversePatchCheck, bottomBar);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        } catch (Exception ignored) {}
        setScene(scene);
    }

    private void doCreatePatch() {
        String pathStr = pathField.getText().trim();
        if (pathStr.isBlank()) {
            Alert err = new Alert(Alert.AlertType.WARNING);
            err.setTitle("Invalid Path");
            err.setHeaderText("Please specify a valid patch file path.");
            err.showAndWait();
            return;
        }

        Path patchFile = Path.of(pathStr);
        boolean reverse = reversePatchCheck.isSelected();

        createPatchBtn.setDisable(true);
        cancelBtn.setDisable(true);

        new Thread(() -> {
            GitService.Result r = GitService.createPatch(projectRoot, patchFile, reverse);
            Platform.runLater(() -> {
                if (r.ok()) {
                    if (log != null) log.accept("\u2713 " + r.output());
                    Notification notif = new Notification(
                            "Git",
                            "Patch created",
                            "Saved to " + patchFile.getFileName().toString(),
                            NotificationType.INFORMATION
                    );
                    NotificationService.getInstance().notify(notif);
                    if (onCreated != null) onCreated.run();
                    close();
                } else {
                    createPatchBtn.setDisable(false);
                    cancelBtn.setDisable(false);
                    if (log != null) log.accept("Patch creation failed: " + r.output());

                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Patch Creation Failed");
                    err.setHeaderText("Could not create patch");
                    err.setContentText(r.output().trim());
                    err.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
                    err.showAndWait();
                }
            });
        }, "lumina-git-create-patch").start();
    }

    // Accessors for testing
    public TextField getPathField() { return pathField; }
    public CheckBox getReversePatchCheck() { return reversePatchCheck; }
    public Button getCreatePatchBtn() { return createPatchBtn; }
    public Button getCancelBtn() { return cancelBtn; }
}
