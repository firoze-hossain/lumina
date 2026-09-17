package dev.lumina.ui;

import dev.lumina.project.GoMetadata;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modal dialog for downloading official Go SDK distributions, matching IntelliJ IDEA's "Download Go SDK" dialog.
 */
public class DownloadGoSdkDialog {

    private final Stage stage;
    private final Consumer<GoMetadata.GoSdk> onInstalled;

    private final ComboBox<GoMetadata.GoRelease> versionCombo = new ComboBox<>();
    private final CheckBox showAllCheck = new CheckBox("Show all");
    private final TextField locationField = new TextField();
    private final Button browseBtn = new Button();
    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label();
    private final Button okBtn = new Button("OK");
    private final Button cancelBtn = new Button("Cancel");
    private final Button helpBtn = new Button("?");

    private boolean userEditedLocation = false;

    public DownloadGoSdkDialog(Stage owner, Consumer<GoMetadata.GoSdk> onInstalled) {
        this.stage = new Stage();
        this.onInstalled = onInstalled;

        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Download Go SDK");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1E1F22;");

        // ---- Form Content ----
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 24, 12, 24));

        ColumnConstraints col0 = new ColumnConstraints(70);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        // Row 0: Version
        Label versionLabel = new Label("Version:");
        versionLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        versionCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(versionCombo, Priority.ALWAYS);
        versionCombo.setStyle("-fx-font-size: 12px;");

        Label showAllHelp = new Label("?");
        showAllHelp.setStyle("-fx-text-fill: #707890; -fx-font-size: 10px; -fx-cursor: hand; "
                + "-fx-border-color: #707890; -fx-border-radius: 8; -fx-min-width: 14px; "
                + "-fx-alignment: center; -fx-padding: 0 2 0 2;");
        Tooltip.install(showAllHelp, new Tooltip("Show all available Go versions including older releases"));

        showAllCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox showAllBox = new HBox(4, showAllCheck, showAllHelp);
        showAllBox.setAlignment(Pos.CENTER_LEFT);

        HBox versionRow = new HBox(12, versionCombo, showAllBox);
        versionRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(versionRow, Priority.ALWAYS);

        grid.add(versionLabel, 0, 0);
        grid.add(versionRow, 1, 0);

        // Row 1: Location
        Label locationLabel = new Label("Location:");
        locationLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        locationField.setStyle("-fx-font-size: 12px;");
        HBox.setHgrow(locationField, Priority.ALWAYS);
        locationField.textProperty().addListener((obs, oldV, newV) -> {
            if (locationField.isFocused()) {
                userEditedLocation = true;
            }
        });

        browseBtn.setGraphic(createBrowseFolderIcon());
        browseBtn.getStyleClass().add("console-button");
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Go SDK Location");
            String current = locationField.getText().trim();
            if (!current.isBlank()) {
                File dir = new File(current);
                if (dir.exists()) {
                    chooser.setInitialDirectory(dir);
                } else if (dir.getParentFile() != null && dir.getParentFile().exists()) {
                    chooser.setInitialDirectory(dir.getParentFile());
                }
            }
            File chosen = chooser.showDialog(stage);
            if (chosen != null) {
                userEditedLocation = true;
                locationField.setText(chosen.getAbsolutePath());
            }
        });

        HBox locationRow = new HBox(6, locationField, browseBtn);
        locationRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(locationRow, Priority.ALWAYS);

        grid.add(locationLabel, 0, 1);
        grid.add(locationRow, 1, 1);

        // Row 2: Status and Progress
        statusLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11.5px;");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        VBox progressBox = new VBox(6, statusLabel, progressBar);
        progressBox.setPadding(new Insets(0, 0, 4, 0));
        grid.add(progressBox, 1, 2);

        root.setCenter(grid);

        // ---- Footer Buttons ----
        helpBtn.setStyle("-fx-text-fill: #707890; -fx-font-size: 11px; -fx-cursor: hand; "
                + "-fx-border-color: #707890; -fx-border-radius: 9; -fx-min-width: 18px; -fx-min-height: 18px; "
                + "-fx-alignment: center; -fx-padding: 0 4 0 4; -fx-background-color: transparent;");
        Tooltip.install(helpBtn, new Tooltip("Official Go distributions will be downloaded from go.dev"));

        okBtn.getStyleClass().add("dialog-primary");
        okBtn.setDefaultButton(true);

        cancelBtn.getStyleClass().add("dialog-secondary");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(10, helpBtn, spacer, okBtn, cancelBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(12, 24, 18, 24));
        root.setBottom(footer);

        // ---- Event Handlers & Data Loading ----
        Runnable loadReleases = () -> {
            boolean showAll = showAllCheck.isSelected();
            List<GoMetadata.GoRelease> releases = GoMetadata.fetchAvailableReleases(showAll);
            versionCombo.getItems().setAll(releases);
            if (!releases.isEmpty()) {
                versionCombo.getSelectionModel().select(0);
            }
        };

        showAllCheck.selectedProperty().addListener((obs, oldV, newV) -> loadReleases.run());

        versionCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !userEditedLocation) {
                locationField.setText(GoMetadata.defaultDownloadLocation(newV.version()));
            }
        });

        okBtn.setOnAction(e -> {
            GoMetadata.GoRelease selected = versionCombo.getValue();
            String loc = locationField.getText().trim();
            if (selected == null || loc.isBlank()) return;

            okBtn.setDisable(true);
            showAllCheck.setDisable(true);
            versionCombo.setDisable(true);
            locationField.setDisable(true);
            browseBtn.setDisable(true);

            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(-1);

            new Thread(() -> {
                try {
                    Path target = Path.of(loc);
                    GoMetadata.downloadAndExtract(selected.version(), target, msg -> {
                        Platform.runLater(() -> statusLabel.setText(msg));
                    });
                    String detectedVer = GoMetadata.detectGoVersion(target.toString());
                    GoMetadata.GoSdk sdk = new GoMetadata.GoSdk(
                            "Go " + (detectedVer.startsWith("go") ? detectedVer.substring(2) : detectedVer),
                            detectedVer,
                            target.toAbsolutePath().toString(),
                            true
                    );
                    Platform.runLater(() -> {
                        if (onInstalled != null) {
                            onInstalled.accept(sdk);
                        }
                        stage.close();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Failed: " + ex.getMessage());
                        okBtn.setDisable(false);
                        versionCombo.setDisable(false);
                        locationField.setDisable(false);
                        browseBtn.setDisable(false);
                        progressBar.setVisible(false);
                    });
                }
            }, "go-sdk-downloader").start();
        });

        loadReleases.run();

        Scene scene = new Scene(root, 520, 210);
        scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);
    }

    public void show() {
        stage.showAndWait();
    }

    private static Node createBrowseFolderIcon() {
        SVGPath folder = new SVGPath();
        folder.setContent("M 1.5,3 C 1.5,2.4 1.9,2 2.5,2 L 5.8,2 C 6.2,2 6.6,2.2 6.8,2.6 L 8,4.2 L 13.5,4.2 C 14.1,4.2 14.5,4.6 14.5,5.2 L 14.5,12 C 14.5,12.6 14.1,13 13.5,13 L 2.5,13 C 1.9,13 1.5,12.6 1.5,12 Z");
        folder.setFill(Color.TRANSPARENT);
        folder.setStroke(Color.web("#8C919D"));
        folder.setStrokeWidth(1.2);
        return folder;
    }
}
