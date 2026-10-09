package dev.lumina.ui;

import dev.lumina.protobuf.ProtobufSettings;
import dev.lumina.protobuf.ProtobufSettingsManager;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Languages & Frameworks > Protocol Buffers > Text Format settings page in Lumina IDE.
 * Matches Image 5 with schema association warnings checkbox for .textproto / .pbtxt files.
 */
public class SettingsLanguagesProtobufTextFormatPage extends VBox {

    private final ProtobufSettingsManager manager = ProtobufSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private CheckBox warnMissingSchemaCheckBox;
    private ProtobufSettings initialSettings;

    public SettingsLanguagesProtobufTextFormatPage() {
        setSpacing(16);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ProtobufSettings current = manager.getSettings();

        warnMissingSchemaCheckBox = new CheckBox("Warn about missing schema associations");
        warnMissingSchemaCheckBox.setSelected(current.isWarnAboutMissingSchemaAssociations());
        warnMissingSchemaCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        warnMissingSchemaCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        getChildren().add(warnMissingSchemaCheckBox);
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

    public void revertChanges() {
        reset();
    }

    public void takeSnapshot() {
        this.initialSettings = manager.getSettings();
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return initialSettings.isWarnAboutMissingSchemaAssociations() != warnMissingSchemaCheckBox.isSelected();
    }

    public void apply() {
        ProtobufSettings s = manager.getSettings();
        s.setWarnAboutMissingSchemaAssociations(warnMissingSchemaCheckBox.isSelected());
        manager.setSettings(s);
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        ProtobufSettings current = manager.getSettings();
        warnMissingSchemaCheckBox.setSelected(current.isWarnAboutMissingSchemaAssociations());
        takeSnapshot();
        fireModified();
    }

    public CheckBox getWarnMissingSchemaCheckBox() {
        return warnMissingSchemaCheckBox;
    }
}
