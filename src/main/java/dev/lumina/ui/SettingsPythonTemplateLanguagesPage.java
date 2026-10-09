package dev.lumina.ui;

import com.google.gson.Gson;
import dev.lumina.settings.PythonTemplateLanguagesSettings;
import dev.lumina.util.Settings;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Settings page for Languages & Frameworks > Python Template Languages.
 * Faithfully matches Image 1.
 */
public class SettingsPythonTemplateLanguagesPage extends VBox {

    public static final String KEY_PYTHON_TEMPLATE_LANGUAGES = "python.template.languages.settings";
    private static final Gson GSON = new Gson();

    private ComboBox<String> templateLanguageCombo;
    private final ObservableList<String> fileTypesList = FXCollections.observableArrayList();
    private ListView<String> fileTypesListView;

    private PythonTemplateLanguagesSettings initialSettings = new PythonTemplateLanguagesSettings();
    private Runnable onModified;

    public SettingsPythonTemplateLanguagesPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadSettings();
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        // Row 1: Template language
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label langLabel = new Label("Template language:");
        langLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        langLabel.setPrefWidth(140);

        templateLanguageCombo = new ComboBox<>(FXCollections.observableArrayList(
                "None", "Jinja2", "Django", "Mako", "Web2Py"
        ));
        templateLanguageCombo.setValue("None");
        templateLanguageCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 13px;");
        templateLanguageCombo.setPrefWidth(120);
        templateLanguageCombo.valueProperty().addListener((obs, ov, nv) -> notifyModified());

        topRow.getChildren().addAll(langLabel, templateLanguageCombo);

        // Section: Template File Types
        HBox sectionHeader = createSectionHeader("Template File Types");

        // Toolbar: + / —
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 0, 4, 0));

        Button addBtn = new Button("+");
        addBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-padding: 2 8; -fx-font-size: 12px; -fx-cursor: hand;");
        addBtn.setOnAction(e -> handleAddFileType());

        Button removeBtn = new Button("—");
        removeBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-padding: 2 8; -fx-font-size: 12px; -fx-cursor: hand;");
        removeBtn.setOnAction(e -> handleRemoveFileType());

        toolbar.getChildren().addAll(addBtn, removeBtn);

        // List View
        fileTypesListView = new ListView<>(fileTypesList);
        fileTypesListView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4;");
        fileTypesListView.setPrefHeight(220);
        VBox.setVgrow(fileTypesListView, Priority.ALWAYS);

        fileTypesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label(item.equalsIgnoreCase("XML") ? "</>" : "<>");
                    icon.setStyle("-fx-text-fill: #589DF6; -fx-font-weight: bold; -fx-font-size: 11px;");

                    Label text = new Label(item);
                    text.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

                    box.getChildren().addAll(icon, text);
                    setGraphic(box);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 4 8;");
                }
            }
        });

        getChildren().addAll(topRow, sectionHeader, toolbar, fileTypesListView);
    }

    private void handleAddFileType() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Add Template File Type");
        dlg.setHeaderText("Enter file type name (e.g. SVG, JSON):");
        dlg.showAndWait().ifPresent(val -> {
            String trimmed = val.trim();
            if (!trimmed.isEmpty() && !fileTypesList.contains(trimmed)) {
                fileTypesList.add(trimmed);
                notifyModified();
            }
        });
    }

    private void handleRemoveFileType() {
        String selected = fileTypesListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            fileTypesList.remove(selected);
            notifyModified();
        }
    }

    private HBox createSectionHeader(String titleText) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(titleText);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(label, line);
        return box;
    }

    public void loadSettings() {
        String json = Settings.get(KEY_PYTHON_TEMPLATE_LANGUAGES);
        PythonTemplateLanguagesSettings loaded = null;
        if (json != null && !json.isBlank()) {
            try {
                loaded = GSON.fromJson(json, PythonTemplateLanguagesSettings.class);
            } catch (Exception ignored) {}
        }
        if (loaded == null) {
            loaded = new PythonTemplateLanguagesSettings();
        }

        templateLanguageCombo.setValue(loaded.getTemplateLanguage());
        fileTypesList.setAll(loaded.getTemplateFileTypes());

        initialSettings = loaded.copy();
    }

    private PythonTemplateLanguagesSettings buildCurrentSettings() {
        PythonTemplateLanguagesSettings current = new PythonTemplateLanguagesSettings();
        current.setTemplateLanguage(templateLanguageCombo.getValue());
        current.setTemplateFileTypes(fileTypesList);
        return current;
    }

    public boolean isModified() {
        return !buildCurrentSettings().equals(initialSettings);
    }

    public void apply() {
        PythonTemplateLanguagesSettings current = buildCurrentSettings();
        Settings.put(KEY_PYTHON_TEMPLATE_LANGUAGES, GSON.toJson(current));
        initialSettings = current.copy();
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    public String getSelectedLanguage() {
        return templateLanguageCombo.getValue();
    }

    public void setSelectedLanguage(String lang) {
        templateLanguageCombo.setValue(lang);
    }

    public List<String> getFileTypes() {
        return new ArrayList<>(fileTypesList);
    }

    public void addFileType(String type) {
        if (type != null && !type.isBlank() && !fileTypesList.contains(type)) {
            fileTypesList.add(type);
            notifyModified();
        }
    }

    public void removeFileType(String type) {
        if (fileTypesList.remove(type)) {
            notifyModified();
        }
    }
}
