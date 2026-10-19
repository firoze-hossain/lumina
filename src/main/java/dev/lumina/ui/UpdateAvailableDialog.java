package dev.lumina.ui;

import dev.lumina.update.AppUpdate;
import dev.lumina.update.AppUpdateService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Modern IntelliJ-style update dialog for Lumina IDE.
 * Shows version comparisons, release notes, progress bar, and handles
 * one-click download, installation, and IDE restart.
 */
public class UpdateAvailableDialog extends Stage {

    private final AppUpdate update;
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label();
    private final Button actionBtn = new Button("Download & Install");
    private final Button closeBtn = new Button("Remind Me Later");
    private Path downloadedFile;

    public static void show(Window owner, AppUpdate update, boolean autoStartDownload) {
        if (update == null || !update.available || update.release == null) return;
        Platform.runLater(() -> {
            UpdateAvailableDialog dialog = new UpdateAvailableDialog(owner, update);
            dialog.show();
            if (autoStartDownload) {
                dialog.startDownload();
            }
        });
    }

    public UpdateAvailableDialog(Window owner, AppUpdate update) {
        this.update = update;
        if (owner != null) {
            initOwner(owner);
        }
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Lumina IDE Update");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setPrefWidth(540);
        root.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");

        // Header Title
        Label title = new Label("A new version of Lumina IDE is available");
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Version comparison & source
        String sourceLabel = "github".equalsIgnoreCase(update.source) ? "GitHub Releases" : "RozeHub";
        String versionText = String.format("Current: %s  ➔  Latest: %s  •  Source: %s",
                update.currentVersion, update.latestVersion, sourceLabel);
        Label versionLabel = new Label(versionText);
        versionLabel.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-font-weight: bold;");

        // Release notes box
        Label notesHeader = new Label("Release Notes:");
        notesHeader.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-font-weight: bold;");

        TextArea notesArea = new TextArea(update.release.displayNotes());
        notesArea.setEditable(false);
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(7);
        notesArea.setStyle("""
            -fx-control-inner-background: #2B2D30;
            -fx-background-color: #2B2D30;
            -fx-text-fill: #DFE1E5;
            -fx-border-color: #393B40;
            -fx-border-radius: 4;
            -fx-font-family: monospace;
            -fx-font-size: 12px;
        """);
        VBox.setVgrow(notesArea, Priority.ALWAYS);

        // Package metadata
        String packageInfo = "";
        String fileName = update.release.effectiveFileName();
        long fileSize = update.release.effectiveFileSize();
        if (fileName != null && !fileName.isBlank()) {
            if (fileSize > 0) {
                double mb = fileSize / (1024.0 * 1024.0);
                packageInfo = String.format(Locale.US, "Package: %s (%.1f MB)", fileName, mb);
            } else {
                packageInfo = String.format("Package: %s", fileName);
            }
        }
        Label packageLabel = new Label(packageInfo);
        packageLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        progressBar.setVisible(false);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #3574F0;");

        statusLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");

        // Action Buttons
        closeBtn.setStyle("""
            -fx-background-color: #2B2D30;
            -fx-text-fill: #DFE1E5;
            -fx-border-color: #393B40;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-padding: 6 16;
            -fx-cursor: hand;
        """);
        closeBtn.setOnAction(e -> close());

        actionBtn.setStyle("""
            -fx-background-color: #3574F0;
            -fx-text-fill: #FFFFFF;
            -fx-font-weight: bold;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-padding: 6 18;
            -fx-cursor: hand;
        """);

        String downloadUrl = update.release.effectiveDownloadUrl();
        if (downloadUrl == null || downloadUrl.isBlank()) {
            actionBtn.setDisable(true);
            statusLabel.setText("No download package attached to this release.");
        }

        actionBtn.setOnAction(e -> {
            if (downloadedFile != null) {
                // Restart and install
                try {
                    AppUpdateService.installAndRestart(downloadedFile);
                } catch (Exception ex) {
                    statusLabel.setStyle("-fx-text-fill: #FF5555;");
                    statusLabel.setText("Failed to start installer: " + ex.getMessage());
                }
            } else {
                startDownload();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox btnBox = new HBox(10, closeBtn, spacer, actionBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, versionLabel, notesHeader, notesArea, packageLabel, progressBar, statusLabel, btnBox);

        Scene scene = new Scene(root);
        setScene(scene);
    }

    private void startDownload() {
        actionBtn.setDisable(true);
        closeBtn.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        statusLabel.setStyle("-fx-text-fill: #589DF6;");
        statusLabel.setText("Downloading installer package...");

        AppUpdateService.download(update.release, p -> Platform.runLater(() -> {
            progressBar.setProgress(p);
            statusLabel.setText(String.format(Locale.US, "Downloading: %.0f%%", p * 100));
        })).whenComplete((file, err) -> Platform.runLater(() -> {
            closeBtn.setDisable(false);
            if (err != null) {
                statusLabel.setStyle("-fx-text-fill: #FF5555;");
                statusLabel.setText("Download failed: " + err.getMessage());
                actionBtn.setText("Retry Download");
                actionBtn.setDisable(false);
            } else {
                downloadedFile = file;
                progressBar.setProgress(1.0);
                statusLabel.setStyle("-fx-text-fill: #50FA7B; -fx-font-weight: bold;");
                statusLabel.setText("Downloaded successfully to: " + file.getFileName());

                actionBtn.setText("Restart & Install Now");
                actionBtn.setDisable(false);
                actionBtn.setStyle("""
                    -fx-background-color: #5FB865;
                    -fx-text-fill: #FFFFFF;
                    -fx-font-weight: bold;
                    -fx-border-radius: 4;
                    -fx-background-radius: 4;
                    -fx-padding: 6 18;
                    -fx-cursor: hand;
                """);
                closeBtn.setText("Install on Exit");
            }
        }));
    }
}
