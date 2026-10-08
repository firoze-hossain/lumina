package dev.lumina.ui;

import dev.lumina.build.MavenSettings;
import dev.lumina.build.MavenSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.*;

/**
 * Settings subpage for Build, Execution, Deployment > Build Tools > Maven > Ignored Files.
 * Matches 1:1 with reference specification and dynamically discovers project pom.xml files.
 */
public class SettingsMavenIgnoredFilesPage extends VBox {

    private final MavenSettingsManager manager = MavenSettingsManager.getInstance();

    private final TextField pathPatternsField = new TextField();
    private final VBox filesContainer = new VBox(8);
    private final Map<String, CheckBox> pomCheckBoxes = new LinkedHashMap<>();

    private String initialPatterns = "";
    private Set<String> initialIgnoredFiles = new LinkedHashSet<>();
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsMavenIgnoredFilesPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // Label for path patterns
        Label patternsLabel = new Label("Path patterns (comma-separated, '*' and '?' wildcards allowed):");
        patternsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Patterns textfield
        pathPatternsField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10; -fx-font-size: 13px;");
        pathPatternsField.textProperty().addListener((obs, o, n) -> notifyModified());

        VBox patternsBox = new VBox(6, patternsLabel, pathPatternsField);
        getChildren().add(patternsBox);

        // Header for Ignored Files list
        Label ignoredFilesLabel = new Label("Ignored Files");
        ignoredFilesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        getChildren().add(ignoredFilesLabel);

        // Bordered scrollable container for discovered pom.xml files
        filesContainer.setPadding(new Insets(10, 12, 10, 12));
        filesContainer.setStyle("-fx-background-color: #1E1F22;");

        ScrollPane scroll = new ScrollPane(filesContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; " +
                "-fx-border-radius: 4; -fx-background-radius: 4;");
        scroll.setPrefHeight(340);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().add(scroll);
    }

    public void loadData() {
        updating = true;
        MavenSettings s = manager.getSettings();
        initialPatterns = s.getIgnoredPathPatterns();
        initialIgnoredFiles = s.getIgnoredFiles();

        pathPatternsField.setText(initialPatterns);

        // Dynamically discover all pom.xml files in the active project workspace
        List<String> discoveredPoms = MavenSettings.discoverProjectPomFiles();
        filesContainer.getChildren().clear();
        pomCheckBoxes.clear();

        if (discoveredPoms.isEmpty()) {
            // If running outside a discovered project directory, fallback to current pom if available
            String currentPom = java.nio.file.Path.of("pom.xml").toAbsolutePath().normalize().toString();
            if (java.nio.file.Files.exists(java.nio.file.Path.of(currentPom))) {
                discoveredPoms = List.of(currentPom);
            }
        }

        for (String pomPath : discoveredPoms) {
            CheckBox cb = new CheckBox(pomPath);
            cb.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 13px;");
            boolean isIgnored = initialIgnoredFiles.contains(pomPath);
            cb.setSelected(isIgnored);
            if (isIgnored) {
                cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
            }
            cb.selectedProperty().addListener((obs, o, n) -> {
                cb.setStyle("-fx-text-fill: " + (n ? "#DFE1E5" : "#8C919D") + "; -fx-font-size: 13px;");
                notifyModified();
            });
            pomCheckBoxes.put(pomPath, cb);
            filesContainer.getChildren().add(cb);
        }

        updating = false;
    }

    public boolean isModified() {
        String currentPatterns = pathPatternsField.getText() != null ? pathPatternsField.getText() : "";
        if (!initialPatterns.equals(currentPatterns)) return true;
        Set<String> currentIgnored = getCurrentIgnoredFiles();
        return !initialIgnoredFiles.equals(currentIgnored);
    }

    public void apply() {
        MavenSettings s = manager.getSettings();
        s.setIgnoredPathPatterns(pathPatternsField.getText());
        Set<String> currentIgnored = getCurrentIgnoredFiles();
        s.setIgnoredFiles(currentIgnored);
        manager.setSettings(s);

        initialPatterns = pathPatternsField.getText() != null ? pathPatternsField.getText() : "";
        initialIgnoredFiles = new LinkedHashSet<>(currentIgnored);
    }

    public void reset() {
        loadData();
    }

    public Set<String> getCurrentIgnoredFiles() {
        Set<String> set = new LinkedHashSet<>();
        for (Map.Entry<String, CheckBox> e : pomCheckBoxes.entrySet()) {
            if (e.getValue().isSelected()) {
                set.add(e.getKey());
            }
        }
        return set;
    }

    public TextField getPathPatternsField() {
        return pathPatternsField;
    }

    public Map<String, CheckBox> getPomCheckBoxes() {
        return pomCheckBoxes;
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
