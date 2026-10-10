package dev.lumina.ui;

import dev.lumina.tools.SharedIndexesSettings;
import dev.lumina.tools.SharedIndexesSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Settings UI page for Tools > Shared Indexes in Lumina IDE.
 */
public class SettingsToolsSharedIndexesPage extends VBox {

    private final Hyperlink manualConfigLink;
    private Runnable onModified;
    private boolean isLocallyModified = false;

    private String currentMode = SharedIndexesSettings.MODE_MANUAL;
    private String customServerUrl = "";
    private String localCacheDirectory = "";
    private boolean downloadJdkIndexes = false;
    private boolean downloadMavenIndexes = false;

    public SettingsToolsSharedIndexesPage() {
        setSpacing(16);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

        // Section header with line
        HBox headerBox = createSectionHeader("Project Shared Indexes");

        // Description text with embedded hyperlink
        Label prefix = new Label("The project pre-built shared indexes download is not configured, it should be done ");
        prefix.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        manualConfigLink = new Hyperlink("manually");
        manualConfigLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;");
        manualConfigLink.setOnMouseEntered(e -> manualConfigLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 13px; -fx-underline: true; -fx-border-color: transparent; -fx-padding: 0;"));
        manualConfigLink.setOnMouseExited(e -> manualConfigLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-underline: false; -fx-border-color: transparent; -fx-padding: 0;"));
        manualConfigLink.setOnAction(e -> showManualConfigurationDialog());

        Label suffix = new Label(".");
        suffix.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox descRow = new HBox(prefix, manualConfigLink, suffix);
        descRow.setAlignment(Pos.CENTER_LEFT);
        descRow.setPadding(new Insets(4, 0, 0, 16));

        getChildren().addAll(headerBox, descRow);
        loadSettings();
    }

    private HBox createSectionHeader(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1; -fx-max-height: 1;");
        HBox.setHgrow(line, Priority.ALWAYS);

        HBox box = new HBox(12, titleLabel, line);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void showManualConfigurationDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Shared Indexes Configuration");
        dialog.setHeaderText("Configure Pre-Built Shared Indexes");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1E1F22;");

        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton manualRadio = new RadioButton("Manual (don't download automatically)");
        manualRadio.setToggleGroup(modeGroup);
        manualRadio.setSelected(SharedIndexesSettings.MODE_MANUAL.equals(currentMode));
        manualRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        RadioButton autoRadio = new RadioButton("Auto-download for project SDK & standard dependencies");
        autoRadio.setToggleGroup(modeGroup);
        autoRadio.setSelected(SharedIndexesSettings.MODE_AUTO_DOWNLOAD.equals(currentMode));
        autoRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        RadioButton customRadio = new RadioButton("Custom index server");
        customRadio.setToggleGroup(modeGroup);
        customRadio.setSelected(SharedIndexesSettings.MODE_CUSTOM_SERVER.equals(currentMode));
        customRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        TextField serverUrlField = new TextField(customServerUrl);
        serverUrlField.setPromptText("https://indexes.internal.domain/shared-indexes");
        serverUrlField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-text-fill: #DFE1E5;");
        serverUrlField.disableProperty().bind(customRadio.selectedProperty().not());

        CheckBox jdkCheck = new CheckBox("Download JDK shared indexes");
        jdkCheck.setSelected(downloadJdkIndexes);
        jdkCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        CheckBox mavenCheck = new CheckBox("Download Maven repository shared indexes");
        mavenCheck.setSelected(downloadMavenIndexes);
        mavenCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        content.getChildren().addAll(
                new Label("Download Strategy:") {{ setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold;"); }},
                manualRadio,
                autoRadio,
                customRadio,
                new HBox(8, new Label("Server URL:") {{ setStyle("-fx-text-fill: #848BA3;"); }}, serverUrlField),
                new Separator(),
                jdkCheck,
                mavenCheck
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                if (autoRadio.isSelected()) currentMode = SharedIndexesSettings.MODE_AUTO_DOWNLOAD;
                else if (customRadio.isSelected()) currentMode = SharedIndexesSettings.MODE_CUSTOM_SERVER;
                else currentMode = SharedIndexesSettings.MODE_MANUAL;

                customServerUrl = serverUrlField.getText().trim();
                downloadJdkIndexes = jdkCheck.isSelected();
                downloadMavenIndexes = mavenCheck.isSelected();
                isLocallyModified = true;
                notifyModified();
                return true;
            }
            return false;
        });

        dialog.showAndWait();
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (onModified != null) {
            onModified.run();
        }
    }

    public void loadSettings() {
        SharedIndexesSettings s = SharedIndexesSettingsManager.getInstance().getSettings();
        this.currentMode = s.getDownloadMode();
        this.customServerUrl = s.getCustomServerUrl();
        this.localCacheDirectory = s.getLocalCacheDirectory();
        this.downloadJdkIndexes = s.isDownloadJdkIndexes();
        this.downloadMavenIndexes = s.isDownloadMavenIndexes();
        this.isLocallyModified = false;
    }

    public boolean isModified() {
        if (isLocallyModified) return true;
        SharedIndexesSettings current = SharedIndexesSettingsManager.getInstance().getSettings();
        return !Objects.equals(currentMode, current.getDownloadMode()) ||
                !Objects.equals(customServerUrl, current.getCustomServerUrl()) ||
                !Objects.equals(localCacheDirectory, current.getLocalCacheDirectory()) ||
                downloadJdkIndexes != current.isDownloadJdkIndexes() ||
                downloadMavenIndexes != current.isDownloadMavenIndexes();
    }

    public void apply() {
        SharedIndexesSettings s = new SharedIndexesSettings();
        s.setDownloadMode(currentMode);
        s.setCustomServerUrl(customServerUrl);
        s.setLocalCacheDirectory(localCacheDirectory);
        s.setDownloadJdkIndexes(downloadJdkIndexes);
        s.setDownloadMavenIndexes(downloadMavenIndexes);
        SharedIndexesSettingsManager.getInstance().setSettings(s);
        isLocallyModified = false;
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    public Hyperlink getManualConfigLink() {
        return manualConfigLink;
    }

    public String getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(String mode) {
        this.currentMode = mode;
        this.isLocallyModified = true;
        notifyModified();
    }
}
