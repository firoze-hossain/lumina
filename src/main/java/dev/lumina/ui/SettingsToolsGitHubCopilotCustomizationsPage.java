package dev.lumina.ui;

import dev.lumina.tools.CustomizationLocationEntry;
import dev.lumina.tools.GitHubCopilotCustomizationsSettings;
import dev.lumina.tools.GitHubCopilotCustomizationsSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tools > GitHub Copilot > Customizations settings page in Lumina IDE matching 1:1 design of reference IDE.
 */
public class SettingsToolsGitHubCopilotCustomizationsPage extends VBox {

    public static class PluginEntry {
        private final SimpleStringProperty url = new SimpleStringProperty("");

        public PluginEntry(String url) {
            this.url.set(url != null ? url : "");
        }

        public String getUrl() {
            return url.get();
        }

        public void setUrl(String url) {
            this.url.set(url != null ? url : "");
        }

        public SimpleStringProperty urlProperty() {
            return url;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PluginEntry that = (PluginEntry) o;
            return Objects.equals(getUrl(), that.getUrl());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getUrl());
        }
    }

    private final GitHubCopilotCustomizationsSettingsManager manager;
    private GitHubCopilotCustomizationsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // 1. Instructions
    private VBox instructionsContentBox;
    private ObservableList<CustomizationLocationEntry> instructionLocationsData = FXCollections.observableArrayList();
    private TableView<CustomizationLocationEntry> instructionLocationsTable;
    private Button addInstrBtn;
    private Button removeInstrBtn;
    private Button resetInstrBtn;

    private CheckBox useAgentsMdCheck;
    private CheckBox useNestedAgentsMdCheck;
    private CheckBox useClaudeMdCheck;
    private CheckBox useNestedClaudeMdCheck;

    // 2. Prompts
    private VBox promptsContentBox;
    private ObservableList<CustomizationLocationEntry> promptLocationsData = FXCollections.observableArrayList();
    private TableView<CustomizationLocationEntry> promptLocationsTable;
    private Button addPromptBtn;
    private Button removePromptBtn;
    private Button resetPromptBtn;

    // 3. Agents
    private VBox agentsContentBox;
    private ObservableList<CustomizationLocationEntry> agentLocationsData = FXCollections.observableArrayList();
    private TableView<CustomizationLocationEntry> agentLocationsTable;
    private Button addAgentBtn;
    private Button removeAgentBtn;
    private Button resetAgentBtn;

    // 4. Plugins
    private VBox pluginsContentBox;
    private ObservableList<PluginEntry> pluginMarketplacesData = FXCollections.observableArrayList();
    private TableView<PluginEntry> pluginMarketplacesTable;
    private Button addPluginBtn;
    private Button removePluginBtn;

    public SettingsToolsGitHubCopilotCustomizationsPage() {
        this.manager = GitHubCopilotCustomizationsSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(20);
        setStyle("-fx-background-color: #1E1F22;");

        // --- 1. Instructions Section ---
        VBox instructionsSection = new VBox(10);
        Button instrToggleBtn = createCollapsibleHeader("Instructions");
        instructionsContentBox = new VBox(12);
        instructionsContentBox.setPadding(new Insets(4, 0, 4, 16));

        instrToggleBtn.setOnAction(e -> {
            boolean visible = !instructionsContentBox.isVisible();
            instructionsContentBox.setVisible(visible);
            instructionsContentBox.setManaged(visible);
            instrToggleBtn.setText(visible ? "▾  Instructions" : "▸  Instructions");
        });

        Label instrHeader = createFieldLabel("Instruction File Locations");
        Label instrHint1 = createHintLabel("Specify location(s) of instruction files (*.instructions.md) that can be attached in chat sessions.");
        Label instrHint2 = createHintLabel("Relative paths are resolved from the root folder(s) of your workspace.");

        addInstrBtn = createIconButton("+", () -> {
            CustomizationLocationEntry entry = new CustomizationLocationEntry("", true);
            entry.enabledProperty().addListener((obs, o, n) -> { if (!updating) notifyModified(); });
            instructionLocationsData.add(entry);
            instructionLocationsTable.getSelectionModel().select(entry);
            notifyModified();
        });
        removeInstrBtn = createIconButton("—", () -> {
            CustomizationLocationEntry sel = instructionLocationsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                instructionLocationsData.remove(sel);
                notifyModified();
            }
        });
        resetInstrBtn = createIconButton("↺", this::resetInstructionsToDefault);
        HBox instrToolbar = new HBox(6, addInstrBtn, removeInstrBtn, resetInstrBtn);
        instrToolbar.setAlignment(Pos.CENTER_LEFT);

        instructionLocationsTable = buildLocationsTable(instructionLocationsData);

        // AGENTS.md subheader
        Label agentsMdHeader = createFieldLabel("AGENTS.md");
        useAgentsMdCheck = createCheckBox("Use AGENTS.md file");
        Label agentsMdHint = createHintLabel("Controls whether instructions from 'AGENTS.md' are attached to all chat requests.");
        agentsMdHint.setPadding(new Insets(0, 0, 0, 22));

        useNestedAgentsMdCheck = createCheckBox("Use nested AGENTS.md files (Experimental)");
        useNestedAgentsMdCheck.setPadding(new Insets(0, 0, 0, 22));
        Label nestedAgentsMdHint = createHintLabel("Controls whether instructions from nested 'AGENTS.md' files found in the project are attached to all chat requests.");
        nestedAgentsMdHint.setPadding(new Insets(0, 0, 0, 44));

        // CLAUDE.md subheader
        Label claudeMdHeader = createFieldLabel("CLAUDE.md");
        useClaudeMdCheck = createCheckBox("Use CLAUDE.md file");
        Label claudeMdHint = createHintLabel("Controls whether instructions from 'CLAUDE.md' and 'CLAUDE.local.md' are attached to all chat requests.");
        claudeMdHint.setPadding(new Insets(0, 0, 0, 22));

        useNestedClaudeMdCheck = createCheckBox("Use nested CLAUDE.md files (Experimental)");
        useNestedClaudeMdCheck.setPadding(new Insets(0, 0, 0, 22));
        Label nestedClaudeMdHint = createHintLabel("Controls whether instructions from nested 'CLAUDE.md' files found in the project are attached to all chat requests.");
        nestedClaudeMdHint.setPadding(new Insets(0, 0, 0, 44));

        instructionsContentBox.getChildren().addAll(
                instrHeader,
                instrHint1,
                instrHint2,
                instrToolbar,
                instructionLocationsTable,
                agentsMdHeader,
                useAgentsMdCheck, agentsMdHint,
                useNestedAgentsMdCheck, nestedAgentsMdHint,
                claudeMdHeader,
                useClaudeMdCheck, claudeMdHint,
                useNestedClaudeMdCheck, nestedClaudeMdHint
        );
        instructionsSection.getChildren().addAll(instrToggleBtn, instructionsContentBox);

        // --- 2. Prompts Section ---
        VBox promptsSection = new VBox(10);
        Button promptToggleBtn = createCollapsibleHeader("Prompts");
        promptsContentBox = new VBox(12);
        promptsContentBox.setPadding(new Insets(4, 0, 4, 16));

        promptToggleBtn.setOnAction(e -> {
            boolean visible = !promptsContentBox.isVisible();
            promptsContentBox.setVisible(visible);
            promptsContentBox.setManaged(visible);
            promptToggleBtn.setText(visible ? "▾  Prompts" : "▸  Prompts");
        });

        Label promptHeader = createFieldLabel("Prompt File Locations");
        Label promptHint1 = createHintLabel("Specify location(s) of reusable prompt files (*.prompt.md) that can be run in chat sessions.");
        Label promptHint2 = createHintLabel("Relative paths are resolved from the root folder(s) of your workspace.");

        addPromptBtn = createIconButton("+", () -> {
            CustomizationLocationEntry entry = new CustomizationLocationEntry("", true);
            entry.enabledProperty().addListener((obs, o, n) -> { if (!updating) notifyModified(); });
            promptLocationsData.add(entry);
            promptLocationsTable.getSelectionModel().select(entry);
            notifyModified();
        });
        removePromptBtn = createIconButton("—", () -> {
            CustomizationLocationEntry sel = promptLocationsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                promptLocationsData.remove(sel);
                notifyModified();
            }
        });
        resetPromptBtn = createIconButton("↺", this::resetPromptsToDefault);
        HBox promptToolbar = new HBox(6, addPromptBtn, removePromptBtn, resetPromptBtn);
        promptToolbar.setAlignment(Pos.CENTER_LEFT);

        promptLocationsTable = buildLocationsTable(promptLocationsData);

        promptsContentBox.getChildren().addAll(
                promptHeader,
                promptHint1,
                promptHint2,
                promptToolbar,
                promptLocationsTable
        );
        promptsSection.getChildren().addAll(promptToggleBtn, promptsContentBox);

        // --- 3. Agents Section ---
        VBox agentsSection = new VBox(10);
        Button agentToggleBtn = createCollapsibleHeader("Agents");
        agentsContentBox = new VBox(12);
        agentsContentBox.setPadding(new Insets(4, 0, 4, 16));

        agentToggleBtn.setOnAction(e -> {
            boolean visible = !agentsContentBox.isVisible();
            agentsContentBox.setVisible(visible);
            agentsContentBox.setManaged(visible);
            agentToggleBtn.setText(visible ? "▾  Agents" : "▸  Agents");
        });

        Label agentHeader = createFieldLabel("Agent File Locations");
        Label agentHint1 = createHintLabel("Specify location(s) of custom agent files (*.agent.md).");
        Label agentHint2 = createHintLabel("Relative paths are resolved from the root folder(s) of your workspace.");

        addAgentBtn = createIconButton("+", () -> {
            CustomizationLocationEntry entry = new CustomizationLocationEntry("", true);
            entry.enabledProperty().addListener((obs, o, n) -> { if (!updating) notifyModified(); });
            agentLocationsData.add(entry);
            agentLocationsTable.getSelectionModel().select(entry);
            notifyModified();
        });
        removeAgentBtn = createIconButton("—", () -> {
            CustomizationLocationEntry sel = agentLocationsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                agentLocationsData.remove(sel);
                notifyModified();
            }
        });
        resetAgentBtn = createIconButton("↺", this::resetAgentsToDefault);
        HBox agentToolbar = new HBox(6, addAgentBtn, removeAgentBtn, resetAgentBtn);
        agentToolbar.setAlignment(Pos.CENTER_LEFT);

        agentLocationsTable = buildLocationsTable(agentLocationsData);

        agentsContentBox.getChildren().addAll(
                agentHeader,
                agentHint1,
                agentHint2,
                agentToolbar,
                agentLocationsTable
        );
        agentsSection.getChildren().addAll(agentToggleBtn, agentsContentBox);

        // --- 4. Plugins Section ---
        VBox pluginsSection = new VBox(10);
        Button pluginToggleBtn = createCollapsibleHeader("Plugins");
        pluginsContentBox = new VBox(12);
        pluginsContentBox.setPadding(new Insets(4, 0, 4, 16));

        pluginToggleBtn.setOnAction(e -> {
            boolean visible = !pluginsContentBox.isVisible();
            pluginsContentBox.setVisible(visible);
            pluginsContentBox.setManaged(visible);
            pluginToggleBtn.setText(visible ? "▾  Plugins" : "▸  Plugins");
        });

        Label pluginHeader = createFieldLabel("Plugin Marketplaces");
        Label pluginHint = createHintLabel(
                "Plugin marketplaces to query. Entries may be GitHub shorthand (owner/repo or owner/repo@tag), " +
                "direct Git repository URLs (https://, git, ssh://, or git@) optionally suffixed with #ref), or local directory URLs."
        );

        addPluginBtn = createIconButton("+", () -> {
            PluginEntry entry = new PluginEntry("");
            pluginMarketplacesData.add(entry);
            pluginMarketplacesTable.getSelectionModel().select(entry);
            notifyModified();
        });
        removePluginBtn = createIconButton("—", () -> {
            PluginEntry sel = pluginMarketplacesTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                pluginMarketplacesData.remove(sel);
                notifyModified();
            }
        });
        HBox pluginToolbar = new HBox(6, addPluginBtn, removePluginBtn);
        pluginToolbar.setAlignment(Pos.CENTER_LEFT);

        pluginMarketplacesTable = buildPluginsTable();

        pluginsContentBox.getChildren().addAll(
                pluginHeader,
                pluginHint,
                pluginToolbar,
                pluginMarketplacesTable
        );
        pluginsSection.getChildren().addAll(pluginToggleBtn, pluginsContentBox);

        getChildren().addAll(
                instructionsSection,
                promptsSection,
                agentsSection,
                pluginsSection
        );
    }

    private TableView<CustomizationLocationEntry> buildLocationsTable(ObservableList<CustomizationLocationEntry> data) {
        TableView<CustomizationLocationEntry> table = new TableView<>(data);
        table.setPrefHeight(120);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label placeholder = new Label("Nothing to show");
        placeholder.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        table.setPlaceholder(placeholder);

        TableColumn<CustomizationLocationEntry, Boolean> checkCol = new TableColumn<>("");
        checkCol.setCellValueFactory(d -> d.getValue().enabledProperty());
        checkCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkCol));
        checkCol.setPrefWidth(44);
        checkCol.setMaxWidth(44);
        checkCol.setResizable(false);
        checkCol.setEditable(true);

        TableColumn<CustomizationLocationEntry, String> pathCol = new TableColumn<>("Location");
        pathCol.setCellValueFactory(d -> d.getValue().pathProperty());
        pathCol.setCellFactory(TextFieldTableCell.forTableColumn());
        pathCol.setOnEditCommit(event -> {
            event.getRowValue().setPath(event.getNewValue());
            notifyModified();
        });
        pathCol.setPrefWidth(550);

        table.getColumns().addAll(checkCol, pathCol);
        return table;
    }

    private TableView<PluginEntry> buildPluginsTable() {
        TableView<PluginEntry> table = new TableView<>(pluginMarketplacesData);
        table.setPrefHeight(120);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label placeholder = new Label("Nothing to show");
        placeholder.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        table.setPlaceholder(placeholder);

        TableColumn<PluginEntry, String> urlCol = new TableColumn<>("Marketplace URL / Shorthand");
        urlCol.setCellValueFactory(d -> d.getValue().urlProperty());
        urlCol.setCellFactory(TextFieldTableCell.forTableColumn());
        urlCol.setOnEditCommit(event -> {
            event.getRowValue().setUrl(event.getNewValue());
            notifyModified();
        });
        urlCol.setPrefWidth(600);

        table.getColumns().add(urlCol);
        return table;
    }

    private Button createCollapsibleHeader(String title) {
        Button b = new Button("▾  " + title);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0;");
        return b;
    }

    private Label createFieldLabel(String title) {
        Label l = new Label(title);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");
        return l;
    }

    private Label createHintLabel(String hint) {
        Label l = new Label(hint);
        l.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");
        l.setWrapText(true);
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

    private Button createIconButton(String symbol, Runnable action) {
        Button b = new Button(symbol);
        b.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 2 10 2 10; -fx-font-size: 12px;");
        b.setOnAction(e -> action.run());
        return b;
    }

    public void resetInstructionsToDefault() {
        instructionLocationsData.clear();
        addInstructionEntry(".github/instructions", true);
        addInstructionEntry("~/.copilot/instructions", false);
        notifyModified();
    }

    public void resetPromptsToDefault() {
        promptLocationsData.clear();
        addPromptEntry(".github/prompts", true);
        addPromptEntry("~/.copilot/prompts", false);
        notifyModified();
    }

    public void resetAgentsToDefault() {
        agentLocationsData.clear();
        addAgentEntry(".claude/agents", true);
        addAgentEntry(".github/agents", true);
        addAgentEntry("~/.copilot/agents", true);
        notifyModified();
    }

    private void addInstructionEntry(String path, boolean enabled) {
        CustomizationLocationEntry e = new CustomizationLocationEntry(path, enabled);
        e.enabledProperty().addListener((obs, o, n) -> { if (!updating) notifyModified(); });
        instructionLocationsData.add(e);
    }

    private void addPromptEntry(String path, boolean enabled) {
        CustomizationLocationEntry e = new CustomizationLocationEntry(path, enabled);
        e.enabledProperty().addListener((obs, o, n) -> { if (!updating) notifyModified(); });
        promptLocationsData.add(e);
    }

    private void addAgentEntry(String path, boolean enabled) {
        CustomizationLocationEntry e = new CustomizationLocationEntry(path, enabled);
        e.enabledProperty().addListener((obs, o, n) -> { if (!updating) notifyModified(); });
        agentLocationsData.add(e);
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();

            instructionLocationsData.clear();
            for (CustomizationLocationEntry e : initialSettings.getInstructionLocations()) {
                addInstructionEntry(e.getPath(), e.isEnabled());
            }

            useAgentsMdCheck.setSelected(initialSettings.isUseAgentsMd());
            useNestedAgentsMdCheck.setSelected(initialSettings.isUseNestedAgentsMd());
            useClaudeMdCheck.setSelected(initialSettings.isUseClaudeMd());
            useNestedClaudeMdCheck.setSelected(initialSettings.isUseNestedClaudeMd());

            promptLocationsData.clear();
            for (CustomizationLocationEntry e : initialSettings.getPromptLocations()) {
                addPromptEntry(e.getPath(), e.isEnabled());
            }

            agentLocationsData.clear();
            for (CustomizationLocationEntry e : initialSettings.getAgentLocations()) {
                addAgentEntry(e.getPath(), e.isEnabled());
            }

            pluginMarketplacesData.clear();
            for (String url : initialSettings.getPluginMarketplaces()) {
                pluginMarketplacesData.add(new PluginEntry(url));
            }
        } finally {
            updating = false;
        }
    }

    public void apply() {
        GitHubCopilotCustomizationsSettings s = collectCurrentSettings();
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
        GitHubCopilotCustomizationsSettings current = collectCurrentSettings();
        return !Objects.equals(initialSettings, current);
    }

    private GitHubCopilotCustomizationsSettings collectCurrentSettings() {
        GitHubCopilotCustomizationsSettings s = new GitHubCopilotCustomizationsSettings();

        List<CustomizationLocationEntry> instrs = new ArrayList<>();
        for (CustomizationLocationEntry e : instructionLocationsData) {
            instrs.add(e.clone());
        }
        s.setInstructionLocations(instrs);

        s.setUseAgentsMd(useAgentsMdCheck.isSelected());
        s.setUseNestedAgentsMd(useNestedAgentsMdCheck.isSelected());
        s.setUseClaudeMd(useClaudeMdCheck.isSelected());
        s.setUseNestedClaudeMd(useNestedClaudeMdCheck.isSelected());

        List<CustomizationLocationEntry> prompts = new ArrayList<>();
        for (CustomizationLocationEntry e : promptLocationsData) {
            prompts.add(e.clone());
        }
        s.setPromptLocations(prompts);

        List<CustomizationLocationEntry> agents = new ArrayList<>();
        for (CustomizationLocationEntry e : agentLocationsData) {
            agents.add(e.clone());
        }
        s.setAgentLocations(agents);

        List<String> plugins = new ArrayList<>();
        for (PluginEntry pe : pluginMarketplacesData) {
            plugins.add(pe.getUrl());
        }
        s.setPluginMarketplaces(plugins);

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

    // Getters for tests and IDE access
    public ObservableList<CustomizationLocationEntry> getInstructionLocationsData() { return instructionLocationsData; }
    public TableView<CustomizationLocationEntry> getInstructionLocationsTable() { return instructionLocationsTable; }
    public Button getAddInstrBtn() { return addInstrBtn; }
    public Button getRemoveInstrBtn() { return removeInstrBtn; }
    public Button getResetInstrBtn() { return resetInstrBtn; }

    public CheckBox getUseAgentsMdCheck() { return useAgentsMdCheck; }
    public CheckBox getUseNestedAgentsMdCheck() { return useNestedAgentsMdCheck; }
    public CheckBox getUseClaudeMdCheck() { return useClaudeMdCheck; }
    public CheckBox getUseNestedClaudeMdCheck() { return useNestedClaudeMdCheck; }

    public ObservableList<CustomizationLocationEntry> getPromptLocationsData() { return promptLocationsData; }
    public TableView<CustomizationLocationEntry> getPromptLocationsTable() { return promptLocationsTable; }
    public Button getAddPromptBtn() { return addPromptBtn; }
    public Button getRemovePromptBtn() { return removePromptBtn; }
    public Button getResetPromptBtn() { return resetPromptBtn; }

    public ObservableList<CustomizationLocationEntry> getAgentLocationsData() { return agentLocationsData; }
    public TableView<CustomizationLocationEntry> getAgentLocationsTable() { return agentLocationsTable; }
    public Button getAddAgentBtn() { return addAgentBtn; }
    public Button getRemoveAgentBtn() { return removeAgentBtn; }
    public Button getResetAgentBtn() { return resetAgentBtn; }

    public ObservableList<PluginEntry> getPluginMarketplacesData() { return pluginMarketplacesData; }
    public TableView<PluginEntry> getPluginMarketplacesTable() { return pluginMarketplacesTable; }
    public Button getAddPluginBtn() { return addPluginBtn; }
    public Button getRemovePluginBtn() { return removePluginBtn; }
}
