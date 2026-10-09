package dev.lumina.ui;

import dev.lumina.ktor.KtorSettings;
import dev.lumina.ktor.KtorSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Languages & Frameworks > Ktor settings page in Lumina IDE.
 */
public class SettingsLanguagesKtorPage extends VBox {

    private final KtorSettingsManager manager = KtorSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private CheckBox createRunConfigurationCheckBox;
    private KtorSettings initialSettings;

    public SettingsLanguagesKtorPage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        KtorSettings current = manager.getSettings();

        createRunConfigurationCheckBox = new CheckBox("Create run configuration automatically");
        createRunConfigurationCheckBox.setSelected(current.isCreateRunConfigurationAutomatically());
        createRunConfigurationCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        createRunConfigurationCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        getChildren().add(createRunConfigurationCheckBox);
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

    private KtorSettings getFormSettings() {
        return new KtorSettings(createRunConfigurationCheckBox.isSelected());
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
            createRunConfigurationCheckBox.setSelected(initialSettings.isCreateRunConfigurationAutomatically());
        }
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public CheckBox getCreateRunConfigurationCheckBox() {
        return createRunConfigurationCheckBox;
    }
}
