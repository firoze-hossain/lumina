package dev.lumina.ui;

import dev.lumina.docker.DockerConsoleSettings;
import dev.lumina.docker.DockerSettingsManager;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Docker > Console (Screenshot 4).
 * Accurately replicates the UI and behavior shown in IntelliJ IDEA.
 */
public class SettingsDockerConsolePage extends VBox {

    private final DockerSettingsManager manager = DockerSettingsManager.getInstance();

    private final CheckBox foldPreviousSessionsCheck = new CheckBox("Fold previous sessions in the Log console");

    private DockerConsoleSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDockerConsolePage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(12);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        foldPreviousSessionsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        foldPreviousSessionsCheck.selectedProperty().addListener((obs, o, n) -> checkModified());
        getChildren().add(foldPreviousSessionsCheck);
    }

    public void loadData() {
        suppressEvents = true;
        initialSettings = manager.getConsoleSettings();
        foldPreviousSessionsCheck.setSelected(initialSettings.isFoldPreviousSessionsInLogConsole());
        suppressEvents = false;
        checkModified();
    }

    public DockerConsoleSettings getCurrentSettings() {
        return new DockerConsoleSettings(foldPreviousSessionsCheck.isSelected());
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        if (isModified()) {
            initialSettings = getCurrentSettings();
            manager.setConsoleSettings(initialSettings);
            checkModified();
        }
    }

    public void reset() {
        loadData();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void checkModified() {
        if (suppressEvents) return;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
