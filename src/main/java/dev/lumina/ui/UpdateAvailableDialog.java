package dev.lumina.ui;

import dev.lumina.plugin.RozeHubClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Modern IntelliJ-style update dialog when a new version of Lumina IDE is detected on RozeHub.
 */
public class UpdateAvailableDialog extends Stage {

    public UpdateAvailableDialog(Window owner, RozeHubClient.UpdateInfo updateInfo) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Lumina IDE Update");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");

        // Header
        Label title = new Label("A new version of Lumina IDE is available");
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16px; -fx-font-weight: bold;");

        String versionText = String.format("Current version: %s   ➔   Latest version: %s",
                updateInfo.currentVersion(), updateInfo.latestVersion());
        Label versionLabel = new Label(versionText);
        versionLabel.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-font-weight: bold;");

        // Release notes box
        Label notesHeader = new Label("Release Notes:");
        notesHeader.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-font-weight: bold;");

        TextArea notesArea = new TextArea(updateInfo.notes() != null && !updateInfo.notes().isBlank()
                ? updateInfo.notes() : "General improvements and bug fixes.");
        notesArea.setEditable(false);
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(7);
        notesArea.setStyle("-fx-control-inner-background: #2B2D30; -fx-background-color: #2B2D30; " +
                "-fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-font-family: monospace; -fx-font-size: 12px;");
        VBox.setVgrow(notesArea, Priority.ALWAYS);

        // Package metadata
        String packageInfo = "";
        if (updateInfo.fileName() != null) {
            double mb = updateInfo.fileSize() / (1024.0 * 1024.0);
            packageInfo = String.format(Locale.US, "Package: %s (%.1f MB)", updateInfo.fileName(), mb);
        }
        Label packageLabel = new Label(packageInfo);
        packageLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setVisible(false);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #3574F0;");

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");

        // Action Buttons
        Button laterBtn = new Button("Remind Me Later");
        laterBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 6 16; -fx-cursor: hand;");
        laterBtn.setOnAction(e -> close());

        Button downloadBtn = new Button("Download & Install");
        downloadBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 18; -fx-cursor: hand;");

        if (updateInfo.downloadUrl() == null || updateInfo.downloadUrl().isBlank()) {
            downloadBtn.setDisable(true);
            statusLabel.setText("No download package attached to this release.");
        }

        downloadBtn.setOnAction(e -> {
            downloadBtn.setDisable(true);
            laterBtn.setDisable(true);
            progressBar.setVisible(true);
            statusLabel.setText("Downloading installer from RozeHub...");

            Thread.ofVirtual().start(() -> {
                try {
                    HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(updateInfo.downloadUrl()))
                            .header("User-Agent", "Lumina-IDE/Updater")
                            .GET()
                            .build();

                    HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                    if (res.statusCode() != 200) {
                        throw new Exception("HTTP " + res.statusCode());
                    }

                    long totalBytes = res.headers().firstValueAsLong("Content-Length").orElse(-1L);
                    Path tempDownload = Files.createTempFile("lumina-update-", updateInfo.fileName() != null ? updateInfo.fileName() : "installer.exe");

                    try (InputStream in = res.body();
                         OutputStream out = Files.newOutputStream(tempDownload)) {
                        byte[] buf = new byte[16384];
                        long downloaded = 0;
                        int read;
                        while ((read = in.read(buf)) != -1) {
                            out.write(buf, 0, read);
                            downloaded += read;
                            if (totalBytes > 0) {
                                double p = (double) downloaded / (double) totalBytes;
                                Platform.runLater(() -> progressBar.setProgress(p));
                            }
                        }
                    }

                    Platform.runLater(() -> {
                        progressBar.setProgress(1.0);
                        statusLabel.setStyle("-fx-text-fill: #50FA7B; -fx-font-weight: bold;");
                        statusLabel.setText("Downloaded successfully to: " + tempDownload.toAbsolutePath());
                        laterBtn.setText("Close");
                        laterBtn.setDisable(false);

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Update Downloaded");
                        alert.setHeaderText("Lumina " + updateInfo.latestVersion() + " is ready to install");
                        alert.setContentText("Installer saved to:\n" + tempDownload.toAbsolutePath() +
                                "\n\nYou can launch the installer now or complete it on exit.");
                        alert.showAndWait();
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: #FF5555;");
                        statusLabel.setText("Download failed: " + ex.getMessage());
                        downloadBtn.setDisable(false);
                        laterBtn.setDisable(false);
                    });
                }
            });
        });

        HBox btnBar = new HBox(10, laterBtn, downloadBtn);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(
                title,
                versionLabel,
                notesHeader,
                notesArea,
                packageLabel,
                progressBar,
                statusLabel,
                btnBar
        );

        Scene scene = new Scene(root, 560, 420);
        setScene(scene);
    }
}
