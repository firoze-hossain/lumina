package dev.lumina.ui;

import dev.lumina.tools.KotlinNotebookNewNotebooksSettings;
import dev.lumina.tools.KotlinNotebookNewNotebooksSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Tools > Kotlin Notebook > Settings for New Notebooks page in Lumina IDE.
 * Matches 1:1 with screenshot 4.
 */
public class SettingsToolsKotlinNotebookNewNotebooksPage extends VBox {

    private final KotlinNotebookNewNotebooksSettingsManager manager;
    private KotlinNotebookNewNotebooksSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private CheckBox addProjectLibrariesCheck;

    public SettingsToolsKotlinNotebookNewNotebooksPage() {
        this.manager = KotlinNotebookNewNotebooksSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        Label descLabel = new Label("Configure the default settings used for newly created Kotlin notebooks");
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox jvmHeader = createSectionHeader("JVM and Build");

        addProjectLibrariesCheck = new CheckBox("Add project libraries to the notebook classpath");
        addProjectLibrariesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        getChildren().addAll(descLabel, jvmHeader, addProjectLibrariesCheck);

        setupListeners();
    }

    private void setupListeners() {
        addProjectLibrariesCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 0, 4, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        header.getChildren().addAll(label, sep);
        return header;
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(KotlinNotebookNewNotebooksSettings s) {
        if (s == null) return;
        addProjectLibrariesCheck.setSelected(s.isAddProjectLibrariesToClasspath());
    }

    private KotlinNotebookNewNotebooksSettings getCurrentSettingsFromUI() {
        KotlinNotebookNewNotebooksSettings s = new KotlinNotebookNewNotebooksSettings();
        s.setAddProjectLibrariesToClasspath(addProjectLibrariesCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        KotlinNotebookNewNotebooksSettings current = getCurrentSettingsFromUI();
        return !Objects.equals(initialSettings, current);
    }

    public void apply() {
        KotlinNotebookNewNotebooksSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            updating = true;
            try {
                applySettingsToUI(initialSettings);
            } finally {
                updating = false;
            }
            notifyModified();
        }
    }

    public void reset() {
        revertChanges();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public CheckBox getAddProjectLibrariesCheck() {
        return addProjectLibrariesCheck;
    }
}
