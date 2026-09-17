package dev.lumina.ui;

import dev.lumina.project.NodeDistMetadata;
import dev.lumina.project.NodeDistMetadata.NodeRelease;
import dev.lumina.project.NodeDistMetadata.NodeVersionEntry;
import dev.lumina.project.NodeMetadata;
import dev.lumina.project.NodeMetadata.NodeInterpreter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modal dialog replicating IntelliJ IDEA's "Download Node.js" window.
 * Allows choosing major release (e.g. Node.js 24 LTS: Krypton), patch version,
 * installation directory with dynamic trailing version path updating,
 * and downloading/extracting the distribution directly into Lumina.
 */
public class DownloadNodeDialog {

    private final Stage stage;
    private final Consumer<NodeInterpreter> onInstalled;

    private final ComboBox<NodeRelease> releaseBox = new ComboBox<>();
    private final ComboBox<NodeVersionEntry> versionBox = new ComboBox<>();
    private final TextField locationField = new TextField();
    private final Button browseBtn = new Button("…");
    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label();
    private final Button cancelBtn = new Button("Cancel");
    private final Button downloadBtn = new Button("Download");

    public DownloadNodeDialog(Stage owner, Consumer<NodeInterpreter> onInstalled) {
        this.stage = new Stage();
        this.onInstalled = onInstalled;

        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Download Node.js");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "download-node-dialog");

        // ---- Content Layout ----
        VBox content = new VBox(12);
        content.setPadding(new Insets(18, 22, 10, 22));

        // Grid for form controls
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(75);
        col1.setPrefWidth(75);
        col1.setHgrow(Priority.NEVER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        // Row 0: Release
        Label releaseLabel = new Label("Release:");
        releaseLabel.getStyleClass().add("form-label");
        releaseBox.getStyleClass().add("choice-box");
        releaseBox.setMaxWidth(Double.MAX_VALUE);

        releaseBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(NodeRelease item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.formatDisplay());
                }
            }
        });
        releaseBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(NodeRelease item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.formatDisplay());
                }
            }
        });

        grid.add(releaseLabel, 0, 0);
        grid.add(releaseBox, 1, 0);

        // Row 1: Version
        Label versionLabel = new Label("Version:");
        versionLabel.getStyleClass().add("form-label");
        versionBox.getStyleClass().add("choice-box");
        versionBox.setMaxWidth(Double.MAX_VALUE);

        versionBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(NodeVersionEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.formatDisplay());
                }
            }
        });
        versionBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(NodeVersionEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.formatDisplay());
                }
            }
        });

        grid.add(versionLabel, 0, 1);
        grid.add(versionBox, 1, 1);

        // Row 2: Location
        Label locationLabel = new Label("Location:");
        locationLabel.getStyleClass().add("form-label");

        locationField.getStyleClass().add("text-field");
        HBox.setHgrow(locationField, Priority.ALWAYS);

        browseBtn.getStyleClass().addAll("console-button", "react-browse-btn");
        Tooltip.install(browseBtn, new Tooltip("Choose installation directory"));
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Node.js Installation Directory");
            String current = locationField.getText().trim();
            if (!current.isEmpty()) {
                File dir = new File(current);
                if (dir.exists()) {
                    dc.setInitialDirectory(dir);
                } else if (dir.getParentFile() != null && dir.getParentFile().exists()) {
                    dc.setInitialDirectory(dir.getParentFile());
                }
            }
            File chosen = dc.showDialog(stage);
            if (chosen != null) {
                locationField.setText(chosen.getAbsolutePath());
            }
        });

        HBox locationBox = new HBox(8, locationField, browseBtn);
        locationBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(locationLabel, 0, 2);
        grid.add(locationBox, 1, 2);

        // Progress bar & Status
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("node-download-progress");
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        statusLabel.getStyleClass().add("download-node-status");
        statusLabel.setWrapText(true);
        statusLabel.setText("");

        content.getChildren().addAll(grid, progressBar, statusLabel);

        // ---- Bottom Buttons ----
        HBox buttonBar = buildButtonBar();

        root.setCenter(content);
        root.setBottom(buttonBar);

        Scene scene = new Scene(root, 580, 250);
        try {
            var css = getClass().getResource("/css/lumina-dark.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.setMinWidth(480);
        stage.setMinHeight(230);

        // Populate initial releases and versions
        initData();
    }

    private void initData() {
        List<NodeRelease> releases = NodeDistMetadata.getReleases(false);
        releaseBox.getItems().setAll(releases);

        // Listener for Release selection
        releaseBox.valueProperty().addListener((obs, oldRel, newRel) -> {
            if (newRel == null) return;
            versionBox.getItems().setAll(newRel.versions());
            if (!newRel.versions().isEmpty()) {
                versionBox.getSelectionModel().selectFirst();
            }
        });

        // Listener for Version selection -> updates Location path
        versionBox.valueProperty().addListener((obs, oldVer, newVer) -> {
            if (newVer == null) return;
            updateLocationForVersion(oldVer, newVer);
        });

        // Default selection: select active LTS (e.g. Node.js 24) or first available
        NodeRelease defaultRel = releases.stream()
                .filter(NodeRelease::isLts)
                .findFirst()
                .orElse(releases.isEmpty() ? null : releases.get(0));

        if (defaultRel != null) {
            releaseBox.getSelectionModel().select(defaultRel);
        } else if (!releases.isEmpty()) {
            releaseBox.getSelectionModel().selectFirst();
        }
    }

    private void updateLocationForVersion(NodeVersionEntry oldVer, NodeVersionEntry newVer) {
        String current = locationField.getText().trim();
        String newVersionStr = newVer.version();

        if (current.isEmpty()) {
            locationField.setText(NodeDistMetadata.getDefaultInstallLocation(newVersionStr));
            return;
        }

        if (oldVer != null && current.endsWith(oldVer.version())) {
            // Replace trailing old version with new version
            String base = current.substring(0, current.length() - oldVer.version().length());
            locationField.setText(base + newVersionStr);
        } else {
            // Check if current matches default directory pattern
            Path p = Path.of(current);
            Path parent = p.getParent();
            if (parent != null) {
                locationField.setText(parent.resolve(newVersionStr).toString());
            } else {
                locationField.setText(NodeDistMetadata.getDefaultInstallLocation(newVersionStr));
            }
        }
    }

    private HBox buildButtonBar() {
        cancelBtn.getStyleClass().add("dialog-secondary");
        cancelBtn.setOnAction(e -> stage.close());

        downloadBtn.getStyleClass().add("dialog-primary");
        downloadBtn.setDefaultButton(true);
        downloadBtn.setOnAction(e -> startDownload());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, spacer, cancelBtn, downloadBtn);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(10, 20, 14, 20));
        bar.getStyleClass().add("dialog-footer");
        return bar;
    }

    private void startDownload() {
        NodeVersionEntry versionEntry = versionBox.getValue();
        if (versionEntry == null) return;

        String loc = locationField.getText().trim();
        if (loc.isEmpty()) {
            statusLabel.setText("Please specify an installation location.");
            return;
        }

        // Lock UI controls during download
        downloadBtn.setDisable(true);
        cancelBtn.setDisable(true);
        releaseBox.setDisable(true);
        versionBox.setDisable(true);
        locationField.setDisable(true);
        browseBtn.setDisable(true);

        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        statusLabel.setText("Connecting to nodejs.org…");

        Thread.ofVirtual().start(() -> {
            try {
                Path targetDir = Path.of(loc);
                Path nodeBin = NodeDistMetadata.downloadAndExtract(
                        versionEntry.version(),
                        targetDir,
                        msg -> Platform.runLater(() -> statusLabel.setText(msg))
                );

                NodeMetadata.clearCache();
                String probed = NodeMetadata.probeVersion(nodeBin.toAbsolutePath().toString());
                if (probed == null || probed.isBlank()) {
                    probed = versionEntry.version();
                }

                NodeInterpreter interpreter = new NodeInterpreter(
                        "node",
                        nodeBin.toAbsolutePath().toString(),
                        probed,
                        false
                );

                Platform.runLater(() -> {
                    if (onInstalled != null) {
                        onInstalled.accept(interpreter);
                    }
                    stage.close();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + ex.getMessage());
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                    downloadBtn.setDisable(false);
                    cancelBtn.setDisable(false);
                    releaseBox.setDisable(false);
                    versionBox.setDisable(false);
                    locationField.setDisable(false);
                    browseBtn.setDisable(false);
                });
            }
        });
    }

    public void show() {
        stage.showAndWait();
    }

    public Stage getStage() {
        return stage;
    }

    public ComboBox<NodeRelease> getReleaseBox() {
        return releaseBox;
    }

    public ComboBox<NodeVersionEntry> getVersionBox() {
        return versionBox;
    }

    public TextField getLocationField() {
        return locationField;
    }

    public Button getDownloadButton() {
        return downloadBtn;
    }

    public Button getCancelButton() {
        return cancelBtn;
    }
}
