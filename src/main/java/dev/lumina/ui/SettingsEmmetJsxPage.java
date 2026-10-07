package dev.lumina.ui;

import dev.lumina.settings.EmmetSettings;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

/**
 * Settings page for Editor > Emmet > JSX.
 * Matches IntelliJ IDEA layout with single "Enable JSX Emmet" option.
 */
public class SettingsEmmetJsxPage extends VBox {

    private final EmmetSettings workingSettings;
    private Runnable onModifiedListener;

    private final CheckBox enableJsxEmmetCheck = new CheckBox("Enable JSX Emmet");

    public SettingsEmmetJsxPage() {
        this.workingSettings = EmmetSettings.getInstance().copy();
        getStyleClass().add("settings-page");
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void buildUI() {
        enableJsxEmmetCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        enableJsxEmmetCheck.setSelected(workingSettings.isEnableJsxEmmet());

        enableJsxEmmetCheck.setOnAction(e -> {
            workingSettings.setEnableJsxEmmet(enableJsxEmmetCheck.isSelected());
            fireModified();
        });

        getChildren().add(enableJsxEmmetCheck);
    }

    public void apply() {
        EmmetSettings.getInstance().applyFrom(workingSettings);
        EmmetSettings.getInstance().save();
    }

    public void reset() {
        workingSettings.applyFrom(EmmetSettings.getInstance());
        enableJsxEmmetCheck.setSelected(workingSettings.isEnableJsxEmmet());
    }

    public boolean isModified() {
        return workingSettings.isModified(EmmetSettings.getInstance());
    }
}
