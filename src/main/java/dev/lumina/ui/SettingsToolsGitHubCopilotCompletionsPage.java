package dev.lumina.ui;

import dev.lumina.tools.GitHubCopilotCompletionsSettings;
import dev.lumina.tools.GitHubCopilotCompletionsSettingsManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.*;

/**
 * Tools > GitHub Copilot > Completions settings page in Lumina IDE matching 1:1 design of reference IDE.
 */
public class SettingsToolsGitHubCopilotCompletionsPage extends VBox {

    public static class LanguageEntry {
        private final String name;
        private final BooleanProperty enabled = new SimpleBooleanProperty(true);

        public LanguageEntry(String name, boolean enabled) {
            this.name = name;
            this.enabled.set(enabled);
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return enabled.get();
        }

        public void setEnabled(boolean enabled) {
            this.enabled.set(enabled);
        }

        public BooleanProperty enabledProperty() {
            return enabled;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LanguageEntry that = (LanguageEntry) o;
            return isEnabled() == that.isEnabled() && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, isEnabled());
        }
    }

    private final GitHubCopilotCompletionsSettingsManager manager;
    private GitHubCopilotCompletionsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // Checkboxes
    private CheckBox enableCopilotCompletionsCheck;
    private CheckBox enableNextEditSuggestionsCheck;
    private CheckBox showIdeCompletionsSideBySideCheck;
    private CheckBox showMultipleSuggestionsCheck;

    // Color
    private CheckBox colorForCompletionsCheck;
    private Rectangle colorSwatch;
    private ColorPicker colorPicker;
    private Hyperlink showColorSettingLink;

    // Model
    private ComboBox<String> modelCombo;
    private Hyperlink learnMoreLink;

    // Languages
    private ObservableList<LanguageEntry> languagesData = FXCollections.observableArrayList();
    private TableView<LanguageEntry> languagesTable;

    public SettingsToolsGitHubCopilotCompletionsPage() {
        this.manager = GitHubCopilotCompletionsSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        // Top checkboxes
        enableCopilotCompletionsCheck = createCheckBox("Enable Copilot Completions");
        enableNextEditSuggestionsCheck = createCheckBox("Enable Next Edit Suggestions(NES)");
        showIdeCompletionsSideBySideCheck = createCheckBox("Show IDE completions side-by-side");
        showMultipleSuggestionsCheck = createCheckBox("Show multiple code suggestions in Tool Window");

        // Color for completions row
        colorForCompletionsCheck = createCheckBox("Color for completions");

        colorSwatch = new Rectangle(36, 18);
        colorSwatch.setArcWidth(4);
        colorSwatch.setArcHeight(4);
        colorSwatch.setStroke(Color.web("#393B40"));
        colorSwatch.setFill(Color.web("#6C707E"));

        colorPicker = new ColorPicker(Color.web("#6C707E"));
        colorPicker.setStyle("-fx-background-color: #2B2D30; -fx-color-label-visible: false;");
        colorPicker.setVisible(false);
        colorPicker.setManaged(false);
        colorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String hex = toHexString(newVal);
                colorSwatch.setFill(newVal);
                if (!updating) notifyModified();
            }
        });

        colorSwatch.setOnMouseClicked(e -> {
            colorPicker.show();
        });

        showColorSettingLink = new Hyperlink("Show color setting used by GitHub Copilot");
        styleHyperlink(showColorSettingLink);
        showColorSettingLink.setOnAction(e -> {
            // Focus color picker or hint
            colorPicker.show();
        });

        HBox colorRow = new HBox(12, colorForCompletionsCheck, colorSwatch, colorPicker, showColorSettingLink);
        colorRow.setAlignment(Pos.CENTER_LEFT);

        // Model for completions row
        Label modelLabel = createFieldLabel("Model for completions");
        modelLabel.setPrefWidth(160);

        modelCombo = new ComboBox<>();
        modelCombo.getItems().addAll(GitHubCopilotCompletionsSettings.AVAILABLE_MODELS);
        modelCombo.setValue("GPT-4.1 Copilot");
        modelCombo.setPrefWidth(180);
        styleComboBox(modelCombo);
        modelCombo.valueProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });

        learnMoreLink = new Hyperlink("Learn more");
        styleHyperlink(learnMoreLink);

        HBox modelRow = new HBox(12, modelLabel, modelCombo, learnMoreLink);
        modelRow.setAlignment(Pos.CENTER_LEFT);

        // --- Languages Section ---
        VBox langSection = new VBox(8);
        Label langHeader = createSectionHeader("Languages");
        Label langSubHeader = createFieldLabel("Enabled languages for completions");

        languagesTable = buildLanguagesTable();

        langSection.getChildren().addAll(langHeader, langSubHeader, languagesTable);

        getChildren().addAll(
                enableCopilotCompletionsCheck,
                enableNextEditSuggestionsCheck,
                showIdeCompletionsSideBySideCheck,
                showMultipleSuggestionsCheck,
                colorRow,
                modelRow,
                langSection
        );
    }

    private TableView<LanguageEntry> buildLanguagesTable() {
        TableView<LanguageEntry> table = new TableView<>(languagesData);
        table.setPrefHeight(320);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        TableColumn<LanguageEntry, Boolean> checkCol = new TableColumn<>("");
        checkCol.setCellValueFactory(data -> data.getValue().enabledProperty());
        checkCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkCol));
        checkCol.setPrefWidth(44);
        checkCol.setMaxWidth(44);
        checkCol.setResizable(false);
        checkCol.setEditable(true);

        TableColumn<LanguageEntry, String> nameCol = new TableColumn<>("Language");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(550);
        nameCol.setEditable(false);

        // When user toggles checkbox
        table.setOnMouseClicked(event -> {
            // Catch selection changes
        });

        table.getColumns().addAll(checkCol, nameCol);
        return table;
    }

    private Label createSectionHeader(String title) {
        Label l = new Label(title);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        return l;
    }

    private Label createFieldLabel(String title) {
        Label l = new Label(title);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        return l;
    }

    private CheckBox createCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        cb.selectedProperty().addListener((obs, o, n) -> {
            if (!updating) notifyModified();
        });
        return cb;
    }

    private void styleHyperlink(Hyperlink link) {
        link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;");
        link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0;"));
        link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;"));
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();

            enableCopilotCompletionsCheck.setSelected(initialSettings.isEnableCopilotCompletions());
            enableNextEditSuggestionsCheck.setSelected(initialSettings.isEnableNextEditSuggestions());
            showIdeCompletionsSideBySideCheck.setSelected(initialSettings.isShowIdeCompletionsSideBySide());
            showMultipleSuggestionsCheck.setSelected(initialSettings.isShowMultipleSuggestionsInToolWindow());

            colorForCompletionsCheck.setSelected(initialSettings.isColorForCompletions());
            String colorHex = initialSettings.getCompletionColorRgb();
            try {
                Color c = Color.web(colorHex);
                colorSwatch.setFill(c);
                colorPicker.setValue(c);
            } catch (Exception ignored) {}

            modelCombo.setValue(initialSettings.getModelForCompletions());

            languagesData.clear();
            Map<String, Boolean> langs = initialSettings.getEnabledLanguages();
            for (Map.Entry<String, Boolean> entry : langs.entrySet()) {
                LanguageEntry le = new LanguageEntry(entry.getKey(), entry.getValue());
                le.enabledProperty().addListener((obs, o, n) -> {
                    if (!updating) notifyModified();
                });
                languagesData.add(le);
            }
        } finally {
            updating = false;
        }
    }

    public void apply() {
        GitHubCopilotCompletionsSettings s = collectCurrentSettings();
        manager.save(s);
        initialSettings = s.clone();
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void reset() {
        loadData();
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void revertChanges() {
        reset();
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        GitHubCopilotCompletionsSettings current = collectCurrentSettings();
        return !Objects.equals(initialSettings, current);
    }

    private GitHubCopilotCompletionsSettings collectCurrentSettings() {
        GitHubCopilotCompletionsSettings s = new GitHubCopilotCompletionsSettings();
        s.setEnableCopilotCompletions(enableCopilotCompletionsCheck.isSelected());
        s.setEnableNextEditSuggestions(enableNextEditSuggestionsCheck.isSelected());
        s.setShowIdeCompletionsSideBySide(showIdeCompletionsSideBySideCheck.isSelected());
        s.setShowMultipleSuggestionsInToolWindow(showMultipleSuggestionsCheck.isSelected());
        s.setColorForCompletions(colorForCompletionsCheck.isSelected());
        s.setCompletionColorRgb(toHexString((Color) colorSwatch.getFill()));
        s.setModelForCompletions(modelCombo.getValue());

        Map<String, Boolean> map = new LinkedHashMap<>();
        for (LanguageEntry le : languagesData) {
            map.put(le.getName(), le.isEnabled());
        }
        s.setEnabledLanguages(map);

        return s;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    // Getters for test verification and IDE access
    public CheckBox getEnableCopilotCompletionsCheck() { return enableCopilotCompletionsCheck; }
    public CheckBox getEnableNextEditSuggestionsCheck() { return enableNextEditSuggestionsCheck; }
    public CheckBox getShowIdeCompletionsSideBySideCheck() { return showIdeCompletionsSideBySideCheck; }
    public CheckBox getShowMultipleSuggestionsCheck() { return showMultipleSuggestionsCheck; }
    public CheckBox getColorForCompletionsCheck() { return colorForCompletionsCheck; }
    public Rectangle getColorSwatch() { return colorSwatch; }
    public ColorPicker getColorPicker() { return colorPicker; }
    public Hyperlink getShowColorSettingLink() { return showColorSettingLink; }
    public ComboBox<String> getModelCombo() { return modelCombo; }
    public Hyperlink getLearnMoreLink() { return learnMoreLink; }
    public ObservableList<LanguageEntry> getLanguagesData() { return languagesData; }
    public TableView<LanguageEntry> getLanguagesTable() { return languagesTable; }
}
