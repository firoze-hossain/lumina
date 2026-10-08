package dev.lumina.ui;

import dev.lumina.build.GradleSettings;
import dev.lumina.build.GradleSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;

/**
 * Settings subpage for Build, Execution, Deployment > Build Tools > Gradle.
 * Matches 1:1 with reference screenshot media_1791428045029.png:
 *  - General Settings header
 *  - Gradle user home with dynamic ~/.gradle fallback and folder chooser
 *  - Generate *.iml files for modules imported from Gradle (with Lumina IDE brand isolation)
 *  - Enable parallel Gradle model fetching for Gradle 7.4+ with warning indicator
 */
public class SettingsGradlePage extends VBox {

    private final GradleSettingsManager manager = GradleSettingsManager.getInstance();

    private final TextField gradleUserHomeField = new TextField();
    private final Button browseGradleHomeBtn = new Button("📁");
    private final CheckBox generateImlFilesCheck = new CheckBox("Generate *.iml files for modules imported from Gradle");
    private final CheckBox parallelModelFetchingCheck = new CheckBox("Enable parallel Gradle model fetching for Gradle 7.4+ ⚠");

    private GradleSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsGradlePage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // Section: General Settings
        Label generalSettingsHeader = new Label("General Settings");
        generalSettingsHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 14px;");

        // Gradle user home field with dynamic default prompt text
        Label gradleHomeLabel = new Label("Gradle user home:");
        gradleHomeLabel.setMinWidth(140);
        gradleHomeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        gradleUserHomeField.setPromptText(GradleSettings.getDefaultGradleUserHome());
        gradleUserHomeField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
        HBox.setHgrow(gradleUserHomeField, Priority.ALWAYS);

        browseGradleHomeBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10; -fx-cursor: hand; -fx-font-size: 12px;");
        browseGradleHomeBtn.setOnAction(e -> browseGradleUserHome());

        HBox homeRow = new HBox(10, gradleHomeLabel, gradleUserHomeField, browseGradleHomeBtn);
        homeRow.setAlignment(Pos.CENTER_LEFT);

        Label homeSubtext = new Label("Override the default location where Gradle stores downloaded files, e.g. to tune anti-virus software on Windows");
        homeSubtext.setWrapText(true);
        homeSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 150;");

        VBox homeBox = new VBox(4, homeRow, homeSubtext);

        // Checkbox 1: Generate *.iml files
        generateImlFilesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        Label imlSubtext = new Label("Enable if you have a mixed project with Lumina IDE modules and Gradle modules so that it could be shared via VCS");
        imlSubtext.setWrapText(true);
        imlSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 24;");

        VBox imlBox = new VBox(4, generateImlFilesCheck, imlSubtext);

        // Checkbox 2: Parallel model fetching
        parallelModelFetchingCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        Label parallelSubtext = new Label("Allow collecting Gradle models in parallel during a project reload.");
        parallelSubtext.setWrapText(true);
        parallelSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 0 24;");

        VBox parallelBox = new VBox(4, parallelModelFetchingCheck, parallelSubtext);

        getChildren().addAll(generalSettingsHeader, homeBox, imlBox, parallelBox);

        gradleUserHomeField.textProperty().addListener((obs, o, n) -> notifyModified());
        generateImlFilesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        parallelModelFetchingCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
    }

    private void browseGradleUserHome() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Gradle User Home Directory");
        String cur = gradleUserHomeField.getText();
        if (cur != null && !cur.isBlank()) {
            File curDir = new File(cur.trim());
            if (curDir.exists() && curDir.isDirectory()) {
                chooser.setInitialDirectory(curDir);
            }
        } else {
            File def = new File(GradleSettings.getDefaultGradleUserHome());
            if (def.exists() && def.isDirectory()) {
                chooser.setInitialDirectory(def);
            }
        }
        File selected = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
        if (selected != null) {
            gradleUserHomeField.setText(selected.getAbsolutePath());
        }
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        gradleUserHomeField.setText(initialSettings.getGradleUserHome());
        generateImlFilesCheck.setSelected(initialSettings.isGenerateImlFiles());
        parallelModelFetchingCheck.setSelected(initialSettings.isParallelModelFetching());

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        GradleSettings cur = getCurrentSettings();
        return !initialSettings.equals(cur);
    }

    public void apply() {
        GradleSettings cur = getCurrentSettings();
        manager.setSettings(cur);
        initialSettings = cur.clone();
    }

    public void reset() {
        loadData();
    }

    public GradleSettings getCurrentSettings() {
        GradleSettings s = new GradleSettings();
        s.setGradleUserHome(gradleUserHomeField.getText());
        s.setGenerateImlFiles(generateImlFilesCheck.isSelected());
        s.setParallelModelFetching(parallelModelFetchingCheck.isSelected());
        return s;
    }

    public TextField getGradleUserHomeField() {
        return gradleUserHomeField;
    }

    public Button getBrowseGradleHomeBtn() {
        return browseGradleHomeBtn;
    }

    public CheckBox getGenerateImlFilesCheck() {
        return generateImlFilesCheck;
    }

    public CheckBox getParallelModelFetchingCheck() {
        return parallelModelFetchingCheck;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
