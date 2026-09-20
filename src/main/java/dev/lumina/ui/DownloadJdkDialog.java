package dev.lumina.ui;

import dev.lumina.project.JdkMetadata;
import dev.lumina.project.JdkMetadata.JdkInstallation;
import dev.lumina.project.JdkMetadata.JdkPackage;
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
 * Modal dialog replicating IntelliJ IDEA's "Download JDK" window.
 * Allows selecting major Java version, vendor distribution (Corretto, Zulu, Liberica, OpenJDK, Temurin, etc.),
 * and installation location (~/.jdks/<vendor>-<version>), with live downloading, extraction, and registration.
 */
public class DownloadJdkDialog {

    private final Stage stage;
    private final Consumer<JdkInstallation> onInstalled;

    private final ComboBox<Integer> versionBox = new ComboBox<>();
    private final ComboBox<JdkPackage> vendorBox = new ComboBox<>();
    private final TextField locationField = new TextField();
    private final Label archiveSizeLabel = new Label("Archive size: 209.1 MB");
    private final Button browseBtn = new Button("📁");
    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label();
    private final Button cancelBtn = new Button("Cancel");
    private final Button downloadBtn = new Button("Download");

    private boolean locationManuallyEdited = false;

    public DownloadJdkDialog(Stage owner, int initialMajorVersion, Consumer<JdkInstallation> onInstalled) {
        this.stage = new Stage();
        this.onInstalled = onInstalled;

        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Download JDK");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "download-node-dialog");

        VBox content = new VBox(10);
        content.setPadding(new Insets(18, 22, 10, 22));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(75);
        col1.setPrefWidth(75);
        col1.setHgrow(Priority.NEVER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        // Row 0: Version
        Label versionLabel = new Label("Version:");
        versionLabel.getStyleClass().add("form-label");
        versionBox.getStyleClass().add("choice-box");
        versionBox.setMaxWidth(Double.MAX_VALUE);

        versionBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                }
            }
        });
        versionBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                }
            }
        });

        grid.add(versionLabel, 0, 0);
        grid.add(versionBox, 1, 0);

        // Row 1: Vendor
        Label vendorLabel = new Label("Vendor:");
        vendorLabel.getStyleClass().add("form-label");
        vendorBox.getStyleClass().add("choice-box");
        vendorBox.setMaxWidth(Double.MAX_VALUE);

        vendorBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JdkPackage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label nameLbl = new Label(item.vendorDisplay());
                    nameLbl.setStyle("-fx-text-fill: #EDEFF6; -fx-font-size: 12px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label infoLbl = new Label(item.javaVersion() + "  " + item.architecture());
                    infoLbl.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

                    row.getChildren().addAll(nameLbl, spacer, infoLbl);
                    setGraphic(row);
                }
            }
        });
        vendorBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(JdkPackage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label nameLbl = new Label(item.vendorDisplay());
                    nameLbl.setStyle("-fx-text-fill: #EDEFF6; -fx-font-size: 12px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label infoLbl = new Label(item.javaVersion() + "  " + item.architecture());
                    infoLbl.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

                    row.getChildren().addAll(nameLbl, spacer, infoLbl);
                    setGraphic(row);
                }
            }
        });

        grid.add(vendorLabel, 0, 1);
        grid.add(vendorBox, 1, 1);

        // Row 2: Location
        Label locationLabel = new Label("Location:");
        locationLabel.getStyleClass().add("form-label");
        locationField.getStyleClass().add("text-field");
        HBox.setHgrow(locationField, Priority.ALWAYS);

        browseBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 3 7 3 7;");
        browseBtn.setOnAction(e -> pickDirectory());

        HBox locationRow = new HBox(8, locationField, browseBtn);
        locationRow.setAlignment(Pos.CENTER_LEFT);

        grid.add(locationLabel, 0, 2);
        grid.add(locationRow, 1, 2);

        locationField.textProperty().addListener((obs, old, val) -> {
            if (locationField.isFocused()) {
                locationManuallyEdited = true;
            }
        });

        // Row 3: Archive size (matching media_1789873735586.png directly under Location)
        archiveSizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8C92A4;");
        grid.add(archiveSizeLabel, 1, 3);

        // Progress & Status
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setProgress(0.0);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9DA5B4;");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        VBox progressBox = new VBox(6, statusLabel, progressBar);

        content.getChildren().addAll(grid, progressBox);

        // Footer buttons
        cancelBtn.getStyleClass().add("dialog-secondary");
        cancelBtn.setOnAction(e -> stage.close());

        downloadBtn.getStyleClass().add("dialog-primary");
        downloadBtn.setOnAction(e -> startDownload());

        HBox footer = new HBox(10, cancelBtn, downloadBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 22, 16, 22));

        root.setCenter(content);
        root.setBottom(footer);

        Scene scene = new Scene(root, 540, 250);
        var css = getClass().getResource("/css/lumina-dark.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setScene(scene);
        stage.setMinWidth(480);
        stage.setMinHeight(220);

        // Setup listeners
        versionBox.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                loadVendorsForVersion(val);
            }
        });

        vendorBox.valueProperty().addListener((obs, old, val) -> {
            if (val != null && !locationManuallyEdited) {
                updateDefaultLocation(val);
            }
        });

        // Initialize versions
        initVersions(initialMajorVersion > 0 ? initialMajorVersion : 25);
    }

    private void initVersions(int defaultVersion) {
        List<Integer> versions = JdkMetadata.fetchMajorVersions(false);
        versionBox.getItems().setAll(versions);

        if (versions.contains(defaultVersion)) {
            versionBox.setValue(defaultVersion);
        } else if (!versions.isEmpty()) {
            versionBox.setValue(versions.getFirst());
        }

        // Fetch remote versions in background
        Thread.ofVirtual().start(() -> {
            List<Integer> remote = JdkMetadata.fetchMajorVersions(true);
            if (remote != null && !remote.isEmpty()) {
                Platform.runLater(() -> {
                    Integer current = versionBox.getValue();
                    versionBox.getItems().setAll(remote);
                    if (current != null && remote.contains(current)) {
                        versionBox.setValue(current);
                    } else {
                        versionBox.setValue(remote.getFirst());
                    }
                });
            }
        });
    }

    private void loadVendorsForVersion(int version) {
        List<JdkPackage> pkgs = JdkMetadata.fetchPackages(version, false);
        vendorBox.getItems().setAll(pkgs);
        if (!pkgs.isEmpty()) {
            JdkPackage defaultMatch = pkgs.stream()
                    .filter(p -> p.vendorDisplay().contains("Oracle OpenJDK"))
                    .findFirst()
                    .orElse(pkgs.getFirst());
            vendorBox.setValue(defaultMatch);
            updateDefaultLocation(defaultMatch);
        }

        Thread.ofVirtual().start(() -> {
            List<JdkPackage> remote = JdkMetadata.fetchPackages(version, true);
            if (remote != null && !remote.isEmpty()) {
                Platform.runLater(() -> {
                    if (versionBox.getValue() != null && versionBox.getValue() == version) {
                        JdkPackage cur = vendorBox.getValue();
                        vendorBox.getItems().setAll(remote);
                        if (cur != null) {
                            var match = remote.stream()
                                    .filter(p -> p.vendorDisplay().equals(cur.vendorDisplay()))
                                    .findFirst()
                                    .orElse(remote.getFirst());
                            vendorBox.setValue(match);
                        } else {
                            JdkPackage defaultMatch = remote.stream()
                                    .filter(p -> p.vendorDisplay().contains("Oracle OpenJDK"))
                                    .findFirst()
                                    .orElse(remote.getFirst());
                            vendorBox.setValue(defaultMatch);
                        }
                    }
                });
            }
        });
    }

    private void updateDefaultLocation(JdkPackage pkg) {
        Path defaultDir = JdkMetadata.getDefaultInstallDir(pkg.vendorDisplay(), pkg.majorVersion(), pkg.javaVersion());
        String pathStr = defaultDir.toAbsolutePath().toString();
        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank() && pathStr.startsWith(userHome)) {
            pathStr = "~" + pathStr.substring(userHome.length());
        }
        locationField.setText(pathStr);
        archiveSizeLabel.setText("Archive size: " + pkg.formatArchiveSize());
    }

    private void pickDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select JDK Installation Directory");
        String current = locationField.getText().trim();
        if (current.startsWith("~")) {
            current = System.getProperty("user.home") + current.substring(1);
        }
        if (!current.isBlank()) {
            File curFile = new File(current);
            File parent = curFile.getParentFile();
            if (parent != null && parent.exists()) {
                chooser.setInitialDirectory(parent);
            }
        }
        File selected = chooser.showDialog(stage);
        if (selected != null) {
            locationManuallyEdited = true;
            String pathStr = selected.getAbsolutePath();
            String userHome = System.getProperty("user.home", "");
            if (!userHome.isBlank() && pathStr.startsWith(userHome)) {
                pathStr = "~" + pathStr.substring(userHome.length());
            }
            locationField.setText(pathStr);
        }
    }

    private void startDownload() {
        JdkPackage selectedPkg = vendorBox.getValue();
        if (selectedPkg == null) return;

        String locStr = locationField.getText().trim();
        if (locStr.isBlank()) {
            statusLabel.setText("Please specify an installation location.");
            statusLabel.setStyle("-fx-text-fill: #E06C75;");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
            return;
        }

        if (locStr.startsWith("~")) {
            locStr = System.getProperty("user.home") + locStr.substring(1);
        }
        Path targetDir = Path.of(locStr);

        // Disable controls
        downloadBtn.setDisable(true);
        cancelBtn.setDisable(true);
        versionBox.setDisable(true);
        vendorBox.setDisable(true);
        locationField.setDisable(true);
        browseBtn.setDisable(true);

        progressBar.setProgress(0.0);
        progressBar.setVisible(true);
        progressBar.setManaged(true);

        statusLabel.setText("Preparing download \u2026");
        statusLabel.setStyle("-fx-text-fill: #9DA5B4;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);

        Thread downloadThread = new Thread(() -> {
            try {
                JdkInstallation installed = JdkMetadata.downloadAndExtract(
                        selectedPkg,
                        targetDir,
                        progress -> Platform.runLater(() -> {
                            if (progress < 0) {
                                progressBar.setProgress(-1.0); // Indeterminate
                            } else {
                                progressBar.setProgress(progress);
                            }
                        }),
                        status -> Platform.runLater(() -> statusLabel.setText(status))
                );

                Platform.runLater(() -> {
                    stage.close();
                    if (onInstalled != null) {
                        onInstalled.accept(installed);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Download failed: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #E06C75;");
                    downloadBtn.setDisable(false);
                    cancelBtn.setDisable(false);
                    versionBox.setDisable(false);
                    vendorBox.setDisable(false);
                    locationField.setDisable(false);
                    browseBtn.setDisable(false);
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                });
            }
        }, "lumina-jdk-downloader");

        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    public void show() {
        stage.show();
    }

    public void showAndWait() {
        stage.showAndWait();
    }
}
