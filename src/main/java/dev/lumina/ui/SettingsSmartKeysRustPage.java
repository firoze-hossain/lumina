package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Editor > General > Smart Keys > Rust settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsSmartKeysRustPage extends VBox {

    private final CheckBox pairedHashCheck = new CheckBox("Insert paired hash '#' signs for raw strings");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysRustPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);

        pairedHashCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        pairedHashCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        HBox rustHeader = createSectionHeader("Rust");
        VBox rustGroup = new VBox(8, pairedHashCheck);

        getChildren().addAll(rustHeader, rustGroup);
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 0, 4, 0));

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(lbl, line);
        return header;
    }

    public void loadFromSettings(SmartKeysSettings settings) {
        suppressEvents = true;
        try {
            pairedHashCheck.setSelected(settings.isRustInsertPairedHashForRawStrings());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        return pairedHashCheck.isSelected() != SmartKeysSettings.getInstance().isRustInsertPairedHashForRawStrings();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setRustInsertPairedHashForRawStrings(pairedHashCheck.isSelected());
        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    public CheckBox getPairedHashCheck() {
        return pairedHashCheck;
    }
}