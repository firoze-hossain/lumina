package dev.lumina.ui;

import dev.lumina.go.GoSettings;
import dev.lumina.go.GoSettingsManager;
import dev.lumina.project.GoMetadata;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > Go > GOROOT.
 * Faithfully matches Image 3.
 */
public class SettingsLanguagesGoGoRootPage extends VBox {

    private final GoSettingsManager manager = GoSettingsManager.getInstance();

    private final ObservableList<GoMetadata.GoSdk> sdkList = FXCollections.observableArrayList();
    private final ToggleGroup sdkToggleGroup = new ToggleGroup();
    private final VBox sdkListView = new VBox(6);

    private String currentGoRootPath = "";
    private String currentGoRootVersion = "";
    private String initialGoRootPath = "";
    private String initialGoRootVersion = "";

    private Runnable onModified;

    public SettingsLanguagesGoGoRootPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadFromManager();
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        // "Add SDK..." Button
        Button addSdkBtn = new Button("Add SDK…");
        addSdkBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; "
                + "-fx-border-color: #4E5157; -fx-border-radius: 4px; "
                + "-fx-padding: 4 14; -fx-font-size: 13px; -fx-cursor: hand;");

        ContextMenu addSdkMenu = new ContextMenu();
        addSdkMenu.getStyleClass().add("catalog-menu");

        MenuItem localItem = new MenuItem("Local…");
        localItem.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        localItem.setOnAction(e -> chooseLocalGoSdk());

        MenuItem downloadItem = new MenuItem("Download…");
        downloadItem.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        downloadItem.setOnAction(e -> downloadGoSdk());

        addSdkMenu.getItems().addAll(localItem, downloadItem);

        addSdkBtn.setOnAction(e -> {
            if (!addSdkMenu.isShowing()) {
                addSdkMenu.show(addSdkBtn, Side.BOTTOM, 0, 4);
            } else {
                addSdkMenu.hide();
            }
        });

        HBox topBar = new HBox(addSdkBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // SDK list area
        sdkListView.setStyle("-fx-background-color: transparent;");

        getChildren().addAll(topBar, sdkListView);
    }

    private void refreshSdkListView() {
        sdkListView.getChildren().clear();

        if (sdkList.isEmpty()) {
            // Empty state matching clean IDE view in Image 3
            return;
        }

        Label title = new Label("Configured and detected SDKs:");
        title.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px; -fx-padding: 6 0 2 0;");
        sdkListView.getChildren().add(title);

        for (GoMetadata.GoSdk sdk : sdkList) {
            RadioButton radio = new RadioButton();
            radio.setToggleGroup(sdkToggleGroup);
            radio.setSelected(sdk.path().equals(currentGoRootPath));

            Label nameLabel = new Label(sdk.name());
            nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

            Label pathLabel = new Label(sdk.path());
            pathLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

            VBox infoBox = new VBox(2, nameLabel, pathLabel);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Button removeBtn = new Button("—");
            removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C919D; -fx-cursor: hand; -fx-font-size: 12px;");
            removeBtn.setOnAction(e -> {
                sdkList.remove(sdk);
                if (sdk.path().equals(currentGoRootPath)) {
                    currentGoRootPath = "";
                    currentGoRootVersion = "";
                }
                refreshSdkListView();
                notifyModified();
            });

            radio.selectedProperty().addListener((obs, ov, nv) -> {
                if (Boolean.TRUE.equals(nv)) {
                    currentGoRootPath = sdk.path();
                    currentGoRootVersion = sdk.version();
                    notifyModified();
                }
            });

            HBox card = new HBox(10, radio, infoBox, removeBtn);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(8, 12, 8, 12));
            card.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4px; -fx-background-radius: 4px;");

            sdkListView.getChildren().add(card);
        }
    }

    private Stage getOwnerStage() {
        if (getScene() != null && getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    private void chooseLocalGoSdk() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select GOROOT Directory");
        File chosen = chooser.showDialog(getOwnerStage());
        if (chosen != null) {
            if (!GoMetadata.isValidGoRoot(chosen)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                if (getOwnerStage() != null) alert.initOwner(getOwnerStage());
                alert.setTitle("Invalid GOROOT");
                alert.setHeaderText("Invalid GOROOT Location");
                alert.setContentText("The chosen directory does not appear to contain a valid Go SDK (missing bin/go or VERSION).");
                alert.showAndWait();
                return;
            }

            String ver = GoMetadata.detectGoVersion(chosen.getAbsolutePath());
            String name = "Go " + (ver.startsWith("go") ? ver.substring(2) : ver);
            GoMetadata.GoSdk newSdk = new GoMetadata.GoSdk(name, ver, chosen.getAbsolutePath(), true);

            // Avoid duplicate path
            sdkList.removeIf(s -> s.path().equalsIgnoreCase(newSdk.path()));
            sdkList.add(0, newSdk);

            currentGoRootPath = newSdk.path();
            currentGoRootVersion = newSdk.version();
            refreshSdkListView();
            notifyModified();
        }
    }

    private void downloadGoSdk() {
        DownloadGoSdkDialog dialog = new DownloadGoSdkDialog(getOwnerStage(), sdk -> {
            if (sdk != null) {
                sdkList.removeIf(s -> s.path().equalsIgnoreCase(sdk.path()));
                sdkList.add(0, sdk);
                currentGoRootPath = sdk.path();
                currentGoRootVersion = sdk.version();
                refreshSdkListView();
                notifyModified();
            }
        });
        dialog.show();
    }

    public void loadFromManager() {
        GoSettings s = manager.getSettings();
        currentGoRootPath = s.getGoRootPath();
        currentGoRootVersion = s.getGoRootVersion();
        initialGoRootPath = currentGoRootPath;
        initialGoRootVersion = currentGoRootVersion;

        // Discover Go SDKs dynamically if list is empty
        sdkList.clear();
        List<GoMetadata.GoSdk> discovered = GoMetadata.discoverGoRoots();
        sdkList.addAll(discovered);

        // Ensure current configured SDK is in the list
        if (!currentGoRootPath.isBlank()) {
            boolean found = sdkList.stream().anyMatch(sdk -> sdk.path().equalsIgnoreCase(currentGoRootPath));
            if (!found) {
                String ver = !currentGoRootVersion.isBlank() ? currentGoRootVersion : GoMetadata.detectGoVersion(currentGoRootPath);
                String name = "Go " + (ver.startsWith("go") ? ver.substring(2) : ver);
                sdkList.add(0, new GoMetadata.GoSdk(name, ver, currentGoRootPath, true));
            }
        }

        refreshSdkListView();
    }

    public boolean isModified() {
        return !Objects.equals(currentGoRootPath, initialGoRootPath) ||
                !Objects.equals(currentGoRootVersion, initialGoRootVersion);
    }

    public void apply() {
        GoSettings s = manager.getSettings();
        s.setGoRootPath(currentGoRootPath);
        s.setGoRootVersion(currentGoRootVersion);
        manager.setSettings(s);

        initialGoRootPath = currentGoRootPath;
        initialGoRootVersion = currentGoRootVersion;
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }

    public String getCurrentGoRootPath() {
        return currentGoRootPath;
    }

    public String getCurrentGoRootVersion() {
        return currentGoRootVersion;
    }
}
