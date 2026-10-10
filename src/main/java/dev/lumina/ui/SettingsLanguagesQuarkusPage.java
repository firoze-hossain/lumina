package dev.lumina.ui;

import dev.lumina.quarkus.QuarkusSettings;
import dev.lumina.quarkus.QuarkusSettingsManager;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Languages & Frameworks > Quarkus settings page in Lumina IDE.
 * Matches Image 1 with automatic run configuration creation checkbox.
 */
public class SettingsLanguagesQuarkusPage extends VBox {

    private final QuarkusSettingsManager manager = QuarkusSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private CheckBox createRunConfigCheckBox;
    private QuarkusSettings initialSettings;

    public SettingsLanguagesQuarkusPage() {
        setSpacing(16);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        QuarkusSettings current = manager.getSettings();

        createRunConfigCheckBox = new CheckBox("Create run configuration automatically");
        createRunConfigCheckBox.setSelected(current.isCreateRunConfigurationAutomatically());
        createRunConfigCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        createRunConfigCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        getChildren().add(createRunConfigCheckBox);
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModified(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void takeSnapshot() {
        this.initialSettings = manager.getSettings();
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return initialSettings.isCreateRunConfigurationAutomatically() != createRunConfigCheckBox.isSelected();
    }

    public void apply() {
        QuarkusSettings s = new QuarkusSettings(createRunConfigCheckBox.isSelected());
        manager.setSettings(s);
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        QuarkusSettings current = manager.getSettings();
        createRunConfigCheckBox.setSelected(current.isCreateRunConfigurationAutomatically());
        takeSnapshot();
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public CheckBox getCreateRunConfigCheckBox() {
        return createRunConfigCheckBox;
    }
}
