package dev.lumina.ui;

import dev.lumina.git.PerforceMcpSettingsManager;
import dev.lumina.git.PerforceMcpSettingsManager.McpEnvironmentMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Version Control > Perforce > Perforce MCP settings page matching IntelliJ IDEA Image 2.
 */
public class SettingsVcsPerforceMcpPage extends VBox {

    private final PerforceMcpSettingsManager manager = PerforceMcpSettingsManager.getInstance();

    // MCP Executable
    private final TextField mcpPathField = new TextField();
    private final Button mcpBrowseBtn = new Button();
    private final Label warningLabel = new Label("Executable must be set to run MCP server.");

    // Settings Section
    private final CheckBox readOnlyCheck = new CheckBox("Use read-only mode (disable write operations)");
    private final CheckBox anonStatsCheck = new CheckBox("Allow anonymous usage statistics");

    // Environment Section
    private final RadioButton doNotUseRadio = new RadioButton("Do not use");
    private final RadioButton useProjectSettingsRadio = new RadioButton("Use Perforce project settings");
    private final RadioButton overrideCustomRadio = new RadioButton("Override with custom values:");
    private final TextField serverPortField = new TextField();
    private final TextField userField = new TextField();
    private final TextField clientWorkspaceField = new TextField();

    // Toolset Section
    private final CheckBox toolsetAllCheck = new CheckBox("All");
    private final CheckBox toolsetFilesCheck = new CheckBox("Files");
    private final CheckBox toolsetChangelistsCheck = new CheckBox("Changelists");
    private final CheckBox toolsetShelvesCheck = new CheckBox("Shelves");
    private final CheckBox toolsetWorkspacesCheck = new CheckBox("Workspaces");
    private final CheckBox toolsetJobsCheck = new CheckBox("Jobs support");

    public SettingsVcsPerforceMcpPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 24, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Executable Section
        VBox execSection = buildExecutableSection();

        // 2. Settings Section
        VBox settingsSection = buildSettingsSection();

        // 3. Environment Section
        VBox envSection = buildEnvironmentSection();

        // 4. Toolset Section
        VBox toolsetSection = buildToolsetSection();

        getChildren().addAll(execSection, settingsSection, envSection, toolsetSection);

        manager.addListener(this::syncFromManager);
        syncFromManager();
    }

    private VBox buildExecutableSection() {
        VBox box = new VBox(6);

        Label label = new Label("Path to MCP server executable:");
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        mcpPathField.setText(manager.getMcpExecutablePath());
        styleTextField(mcpPathField, 350);
        HBox.setHgrow(mcpPathField, Priority.ALWAYS);
        mcpPathField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                manager.setMcpExecutablePath(newV);
                updateWarningVisibility(newV);
            }
        });

        styleBrowseBtn(mcpBrowseBtn, "Select MCP Server Executable", f -> {
            mcpPathField.setText(f.getAbsolutePath());
            manager.setMcpExecutablePath(f.getAbsolutePath());
        });

        HBox row = new HBox(8, label, mcpPathField, mcpBrowseBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        // Warning Icon & Label
        SVGPath warnIcon = new SVGPath();
        warnIcon.setContent("M 6 1 L 11 10 L 1 10 Z M 6 4 L 6 7 M 6 8.5 L 6 9");
        warnIcon.setFill(Color.web("#E5983A"));
        warnIcon.setStroke(Color.web("#E5983A"));
        warnIcon.setStrokeWidth(0.8);

        warningLabel.setGraphic(warnIcon);
        warningLabel.setStyle("-fx-text-fill: #E5983A; -fx-font-size: 11px;");
        updateWarningVisibility(manager.getMcpExecutablePath());

        box.getChildren().addAll(row, warningLabel);
        return box;
    }

    private void updateWarningVisibility(String path) {
        boolean isEmpty = path == null || path.isBlank();
        warningLabel.setVisible(isEmpty);
        warningLabel.setManaged(isEmpty);
    }

    private VBox buildSettingsSection() {
        VBox box = new VBox(8);
        box.getChildren().add(createSectionHeader("Settings"));

        initCheckBox(readOnlyCheck, manager.isReadOnlyMode(), e -> manager.setReadOnlyMode(readOnlyCheck.isSelected()));
        initCheckBox(anonStatsCheck, manager.isAllowAnonymousUsageStats(), e -> manager.setAllowAnonymousUsageStats(anonStatsCheck.isSelected()));

        box.getChildren().addAll(readOnlyCheck, anonStatsCheck);
        return box;
    }

    private VBox buildEnvironmentSection() {
        VBox box = new VBox(8);
        box.getChildren().add(createSectionHeader("Environment"));

        ToggleGroup envGroup = new ToggleGroup();
        doNotUseRadio.setToggleGroup(envGroup);
        useProjectSettingsRadio.setToggleGroup(envGroup);
        overrideCustomRadio.setToggleGroup(envGroup);

        initRadio(doNotUseRadio, manager.getEnvironmentMode() == McpEnvironmentMode.DO_NOT_USE);
        initRadio(useProjectSettingsRadio, manager.getEnvironmentMode() == McpEnvironmentMode.USE_PROJECT_SETTINGS);
        initRadio(overrideCustomRadio, manager.getEnvironmentMode() == McpEnvironmentMode.OVERRIDE_CUSTOM);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(4, 0, 4, 24));

        Label serverLabel = new Label("Server (Port):");
        serverLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        serverPortField.setText(manager.getCustomServerPort());
        styleTextField(serverPortField, 260);
        serverPortField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setCustomServerPort(newV);
        });

        Label userLabel = new Label("User:");
        userLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        userField.setText(manager.getCustomUser());
        styleTextField(userField, 260);
        userField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setCustomUser(newV);
        });

        Label workspaceLabel = new Label("Workspace (Client):");
        workspaceLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        clientWorkspaceField.setText(manager.getCustomClientWorkspace());
        styleTextField(clientWorkspaceField, 260);
        clientWorkspaceField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) manager.setCustomClientWorkspace(newV);
        });

        grid.add(serverLabel, 0, 0);
        grid.add(serverPortField, 1, 0);
        grid.add(userLabel, 0, 1);
        grid.add(userField, 1, 1);
        grid.add(workspaceLabel, 0, 2);
        grid.add(clientWorkspaceField, 1, 2);

        serverPortField.disableProperty().bind(overrideCustomRadio.selectedProperty().not());
        userField.disableProperty().bind(overrideCustomRadio.selectedProperty().not());
        clientWorkspaceField.disableProperty().bind(overrideCustomRadio.selectedProperty().not());

        envGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == doNotUseRadio) {
                manager.setEnvironmentMode(McpEnvironmentMode.DO_NOT_USE);
            } else if (newV == overrideCustomRadio) {
                manager.setEnvironmentMode(McpEnvironmentMode.OVERRIDE_CUSTOM);
            } else {
                manager.setEnvironmentMode(McpEnvironmentMode.USE_PROJECT_SETTINGS);
            }
        });

        box.getChildren().addAll(doNotUseRadio, useProjectSettingsRadio, overrideCustomRadio, grid);
        return box;
    }

    private VBox buildToolsetSection() {
        VBox box = new VBox(8);
        box.getChildren().add(createSectionHeader("Toolset"));

        initCheckBox(toolsetAllCheck, manager.isAllToolsetsEnabled(), e -> {
            boolean sel = toolsetAllCheck.isSelected();
            manager.setAllToolsets(sel);
            toolsetFilesCheck.setSelected(sel);
            toolsetChangelistsCheck.setSelected(sel);
            toolsetShelvesCheck.setSelected(sel);
            toolsetWorkspacesCheck.setSelected(sel);
            toolsetJobsCheck.setSelected(sel);
        });

        VBox indented = new VBox(6);
        indented.setPadding(new Insets(0, 0, 0, 20));

        initCheckBox(toolsetFilesCheck, manager.isToolsetFiles(), e -> {
            manager.setToolsetFiles(toolsetFilesCheck.isSelected());
            updateAllCheckState();
        });
        initCheckBox(toolsetChangelistsCheck, manager.isToolsetChangelists(), e -> {
            manager.setToolsetChangelists(toolsetChangelistsCheck.isSelected());
            updateAllCheckState();
        });
        initCheckBox(toolsetShelvesCheck, manager.isToolsetShelves(), e -> {
            manager.setToolsetShelves(toolsetShelvesCheck.isSelected());
            updateAllCheckState();
        });
        initCheckBox(toolsetWorkspacesCheck, manager.isToolsetWorkspaces(), e -> {
            manager.setToolsetWorkspaces(toolsetWorkspacesCheck.isSelected());
            updateAllCheckState();
        });
        initCheckBox(toolsetJobsCheck, manager.isToolsetJobs(), e -> {
            manager.setToolsetJobs(toolsetJobsCheck.isSelected());
            updateAllCheckState();
        });

        indented.getChildren().addAll(toolsetFilesCheck, toolsetChangelistsCheck, toolsetShelvesCheck, toolsetWorkspacesCheck, toolsetJobsCheck);
        box.getChildren().addAll(toolsetAllCheck, indented);
        return box;
    }

    private void updateAllCheckState() {
        boolean all = manager.isAllToolsetsEnabled();
        toolsetAllCheck.setSelected(all);
    }

    private HBox createSectionHeader(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-border-color: #393B40;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        HBox bar = new HBox(8, lbl, sep);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 0, 2, 0));
        return bar;
    }

    private void initCheckBox(CheckBox cb, boolean initial, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        cb.setSelected(initial);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        cb.setOnAction(onAction);
    }

    private void initRadio(RadioButton rb, boolean initial) {
        rb.setSelected(initial);
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    private void styleTextField(TextField tf, double width) {
        tf.setPrefWidth(width);
        tf.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
    }

    private void styleBrowseBtn(Button btn, String title, java.util.function.Consumer<File> onFileChosen) {
        SVGPath folderIcon = new SVGPath();
        folderIcon.setContent("M 2 3 L 5 3 L 6.5 5 L 12 5 L 12 11 L 2 11 Z");
        folderIcon.setFill(Color.TRANSPARENT);
        folderIcon.setStroke(Color.web("#848BA3"));
        folderIcon.setStrokeWidth(1.2);
        btn.setGraphic(folderIcon);
        btn.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle(title);
            File f = fc.showOpenDialog(getScene().getWindow());
            if (f != null) {
                onFileChosen.accept(f);
            }
        });
    }

    private void syncFromManager() {
        mcpPathField.setText(manager.getMcpExecutablePath());
        updateWarningVisibility(manager.getMcpExecutablePath());
        readOnlyCheck.setSelected(manager.isReadOnlyMode());
        anonStatsCheck.setSelected(manager.isAllowAnonymousUsageStats());

        switch (manager.getEnvironmentMode()) {
            case DO_NOT_USE -> doNotUseRadio.setSelected(true);
            case OVERRIDE_CUSTOM -> overrideCustomRadio.setSelected(true);
            default -> useProjectSettingsRadio.setSelected(true);
        }
        serverPortField.setText(manager.getCustomServerPort());
        userField.setText(manager.getCustomUser());
        clientWorkspaceField.setText(manager.getCustomClientWorkspace());

        toolsetFilesCheck.setSelected(manager.isToolsetFiles());
        toolsetChangelistsCheck.setSelected(manager.isToolsetChangelists());
        toolsetShelvesCheck.setSelected(manager.isToolsetShelves());
        toolsetWorkspacesCheck.setSelected(manager.isToolsetWorkspaces());
        toolsetJobsCheck.setSelected(manager.isToolsetJobs());
        toolsetAllCheck.setSelected(manager.isAllToolsetsEnabled());
    }
}
