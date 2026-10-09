package dev.lumina.ui;

import dev.lumina.lombok.LombokSettings;
import dev.lumina.lombok.LombokSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Languages & Frameworks > Lombok settings page in Lumina IDE.
 */
public class SettingsLanguagesLombokPage extends VBox {

    private final LombokSettingsManager manager = LombokSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private CheckBox autoAddOptionCheckBox;
    private LombokSettings initialSettings;

    public SettingsLanguagesLombokPage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        LombokSettings current = manager.getSettings();

        autoAddOptionCheckBox = new CheckBox("Automatically add '-Djps.track.ap.dependencies=false' compile option for old (<1.18.16) lombok version");
        autoAddOptionCheckBox.setSelected(current.isAutoAddTrackApDependencies());
        autoAddOptionCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        autoAddOptionCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        getChildren().add(autoAddOptionCheckBox);
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

    private LombokSettings getFormSettings() {
        return new LombokSettings(autoAddOptionCheckBox.isSelected());
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
            autoAddOptionCheckBox.setSelected(initialSettings.isAutoAddTrackApDependencies());
        }
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public CheckBox getAutoAddOptionCheckBox() {
        return autoAddOptionCheckBox;
    }
}
