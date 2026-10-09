package dev.lumina.ui;

import dev.lumina.micronaut.MicronautSettings;
import dev.lumina.micronaut.MicronautSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Languages & Frameworks > Micronaut settings page in Lumina IDE.
 */
public class SettingsLanguagesMicronautPage extends VBox {

    private final MicronautSettingsManager manager = MicronautSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private CheckBox createRunConfigCheckBox;
    private MicronautSettings initialSettings;

    public SettingsLanguagesMicronautPage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        MicronautSettings current = manager.getSettings();

        createRunConfigCheckBox = new CheckBox("Create run configuration automatically");
        createRunConfigCheckBox.setSelected(current.isCreateRunConfigurationAutomatically());
        createRunConfigCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        createRunConfigCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        getChildren().add(createRunConfigCheckBox);
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void takeSnapshot() {
        this.initialSettings = getFormSettings();
    }

    private MicronautSettings getFormSettings() {
        return new MicronautSettings(createRunConfigCheckBox.isSelected());
    }

    public boolean isModified() {
        return !Objects.equals(getFormSettings(), initialSettings);
    }

    public void apply() {
        manager.setSettings(getFormSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        if (initialSettings != null) {
            createRunConfigCheckBox.setSelected(initialSettings.isCreateRunConfigurationAutomatically());
        }
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public CheckBox getCreateRunConfigCheckBox() {
        return createRunConfigCheckBox;
    }
}
