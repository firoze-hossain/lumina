package dev.lumina.ui;

import dev.lumina.build.BuildToolsManager;
import dev.lumina.build.BuildToolsSettings;
import dev.lumina.build.BuildToolsSettings.SyncTrigger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Settings page for Build, Execution, Deployment > Build Tools.
 * Replicates the project build script sync options from reference specification.
 */
public class SettingsBuildToolsPage extends VBox {

    private final BuildToolsManager manager = BuildToolsManager.getInstance();

    private final CheckBox syncCheck = new CheckBox("Sync project after changes in the build scripts:");
    private final ToggleGroup triggerGroup = new ToggleGroup();
    private final RadioButton anyChangesRadio = new RadioButton("Any changes");
    private final RadioButton externalChangesRadio = new RadioButton("External changes");
    private final Label externalChangesSubtitle = new Label("Changes outside the IDE or from version control.");

    private BuildToolsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsBuildToolsPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        syncCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        anyChangesRadio.setToggleGroup(triggerGroup);
        externalChangesRadio.setToggleGroup(triggerGroup);

        styleRadioButton(anyChangesRadio);
        styleRadioButton(externalChangesRadio);

        externalChangesSubtitle.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        VBox radioBox = new VBox(10);
        radioBox.setPadding(new Insets(2, 0, 0, 20));

        VBox externalBox = new VBox(4, externalChangesRadio, externalChangesSubtitle);
        externalBox.setAlignment(Pos.CENTER_LEFT);

        radioBox.getChildren().addAll(anyChangesRadio, externalBox);

        // Bind disable state of radio buttons and subtitle to syncCheck
        radioBox.disableProperty().bind(syncCheck.selectedProperty().not());

        getChildren().addAll(syncCheck, radioBox);

        // Dirty tracking listeners
        syncCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        triggerGroup.selectedToggleProperty().addListener((obs, o, n) -> notifyModified());
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        syncCheck.setSelected(initialSettings.isSyncOnBuildScriptChanges());
        if (initialSettings.getSyncTrigger() == SyncTrigger.ANY_CHANGES) {
            anyChangesRadio.setSelected(true);
        } else {
            externalChangesRadio.setSelected(true);
        }
        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        BuildToolsSettings current = getCurrentSettings();
        return !initialSettings.equals(current);
    }

    public void apply() {
        BuildToolsSettings current = getCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.clone();
    }

    public void reset() {
        loadData();
    }

    public BuildToolsSettings getCurrentSettings() {
        BuildToolsSettings s = new BuildToolsSettings();
        s.setSyncOnBuildScriptChanges(syncCheck.isSelected());
        s.setSyncTrigger(anyChangesRadio.isSelected() ? SyncTrigger.ANY_CHANGES : SyncTrigger.EXTERNAL_CHANGES);
        return s;
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
