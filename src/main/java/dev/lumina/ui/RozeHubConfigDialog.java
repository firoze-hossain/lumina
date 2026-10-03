package dev.lumina.ui;

import dev.lumina.plugin.PluginManager;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Dialog for configuring the central RozeHub Server connection (Marketplace & IDE Updates).
 */
public class RozeHubConfigDialog extends Stage {

    public RozeHubConfigDialog(Window owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("RozeHub Central Server Settings");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");

        Label heading = new Label("RozeHub Central Platform");
        heading.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label description = new Label("Configure the RozeHub backend URL for marketplace plugin discovery and IDE updates (e.g. http://localhost:8000).");
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");

        Label urlLabel = new Label("RozeHub Server URL:");
        urlLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");

        TextField urlField = new TextField(RozeHubClient.getInstance().getBaseUrl());
        urlField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #FFFFFF; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 8 10; -fx-font-size: 13px;");
        HBox.setHgrow(urlField, Priority.ALWAYS);

        Label statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 12px;");

        Button testBtn = new Button("Test Connection");
        testBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 6 14; -fx-cursor: hand;");

        testBtn.setOnAction(e -> {
            String target = urlField.getText().trim();
            statusLabel.setStyle("-fx-text-fill: #E5C07B; -fx-font-size: 12px;");
            statusLabel.setText("Connecting to " + target + "...");
            testBtn.setDisable(true);

            Thread.ofVirtual().start(() -> {
                try {
                    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(target.replaceAll("/+$", "") + "/api/v1/marketplace/lumina"))
                            .timeout(Duration.ofSeconds(5))
                            .header("Accept", "application/json")
                            .GET()
                            .build();

                    HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                    Platform.runLater(() -> {
                        testBtn.setDisable(false);
                        if (res.statusCode() == 200) {
                            statusLabel.setStyle("-fx-text-fill: #50FA7B; -fx-font-size: 12px; -fx-font-weight: bold;");
                            statusLabel.setText("✓ Connected successfully! RozeHub Lumina Marketplace is online.");
                        } else {
                            statusLabel.setStyle("-fx-text-fill: #FF5555; -fx-font-size: 12px;");
                            statusLabel.setText("✗ Server returned HTTP " + res.statusCode() + ".");
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        testBtn.setDisable(false);
                        statusLabel.setStyle("-fx-text-fill: #FF5555; -fx-font-size: 12px;");
                        statusLabel.setText("✗ Connection failed: " + ex.getMessage());
                    });
                }
            });
        });

        HBox testRow = new HBox(10, testBtn, statusLabel);
        testRow.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button defaultBtn = new Button("Reset Default");
        defaultBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-cursor: hand; -fx-padding: 6 10;");
        defaultBtn.setOnAction(e -> {
            urlField.setText(RozeHubClient.DEFAULT_ROZEHUB_URL);
            statusLabel.setText("");
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 6 16; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        Button saveBtn = new Button("Save & Sync");
        saveBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 18; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            String newUrl = urlField.getText().trim();
            RozeHubClient.getInstance().setBaseUrl(newUrl);
            PluginManager.getInstance().refreshMarketplaceFromRozeHubAsync();
            close();
        });

        HBox btnBar = new HBox(10, defaultBtn, new Region(), cancelBtn, saveBtn);
        HBox.setHgrow(btnBar.getChildren().get(1), Priority.ALWAYS);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(heading, description, urlLabel, urlField, testRow, spacer, btnBar);

        Scene scene = new Scene(root, 520, 300);
        setScene(scene);
    }
}
