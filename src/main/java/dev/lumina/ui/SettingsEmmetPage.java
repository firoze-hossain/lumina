package dev.lumina.ui;

import dev.lumina.settings.EmmetSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Settings page for Editor > Emmet.
 * Matches IntelliJ IDEA layout with the "Expand abbreviation with" combo box.
 */
public class SettingsEmmetPage extends VBox {

    private final EmmetSettings workingSettings;
    private Runnable onModifiedListener;

    private final ComboBox<String> expandCombo = new ComboBox<>();

    public SettingsEmmetPage() {
        this.workingSettings = EmmetSettings.getInstance().copy();
        getStyleClass().add("settings-page");
        setPadding(new Insets(24, 24, 24, 24));
        setSpacing(16);
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
        Label expandLabel = new Label("Expand abbreviation with");
        expandLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        expandCombo.getItems().addAll("Space", "Tab", "Enter", "Custom...");
        expandCombo.setValue(workingSettings.getExpandAbbreviationWith());
        expandCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-font-size: 13px;");
        expandCombo.setOnAction(e -> {
            workingSettings.setExpandAbbreviationWith(expandCombo.getValue());
            fireModified();
        });

        HBox row = new HBox(12, expandLabel, expandCombo);
        row.setAlignment(Pos.CENTER_LEFT);

        getChildren().add(row);
    }

    public void apply() {
        EmmetSettings.getInstance().applyFrom(workingSettings);
        EmmetSettings.getInstance().save();
    }

    public void reset() {
        workingSettings.applyFrom(EmmetSettings.getInstance());
        expandCombo.setValue(workingSettings.getExpandAbbreviationWith());
    }

    public boolean isModified() {
        return workingSettings.isModified(EmmetSettings.getInstance());
    }
}
