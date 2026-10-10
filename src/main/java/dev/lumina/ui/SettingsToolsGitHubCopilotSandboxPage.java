package dev.lumina.ui;

import dev.lumina.tools.GitHubCopilotSandboxSettings;
import dev.lumina.tools.GitHubCopilotSandboxSettingsManager;
import dev.lumina.tools.SandboxHostAccess;
import dev.lumina.tools.SandboxPathPermission;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tools > GitHub Copilot > Sandbox settings page in Lumina IDE matching 1:1 design of reference IDE.
 */
public class SettingsToolsGitHubCopilotSandboxPage extends VBox {

    private final GitHubCopilotSandboxSettingsManager manager;
    private GitHubCopilotSandboxSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    // Header Checkbox
    private CheckBox enableLocalSandboxCheck;

    // Filesystem
    private CheckBox fsIncludeWorkingDirCheck;
    private CheckBox fsClearPolicyOnExitCheck;
    private ObservableList<SandboxPathPermission> fsPermissionsData = FXCollections.observableArrayList();
    private TableView<SandboxPathPermission> fsPermissionsTable;
    private Button addFsPermBtn;
    private Button removeFsPermBtn;

    // Network
    private CheckBox netAllowOutboundCheck;
    private CheckBox netAllowLocalCheck;
    private ObservableList<SandboxHostAccess> netHostAccessData = FXCollections.observableArrayList();
    private TableView<SandboxHostAccess> netHostAccessTable;
    private Button addNetHostBtn;
    private Button removeNetHostBtn;

    public SettingsToolsGitHubCopilotSandboxPage() {
        this.manager = GitHubCopilotSandboxSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(20);
        setStyle("-fx-background-color: #1E1F22;");

        // Top Sandbox Enable Checkbox
        enableLocalSandboxCheck = createCheckBox("Enable Local Sandbox for Copilot Agent (Preview)");
        Label sandboxDesc = createHintLabel(
                "Local sandboxing lets the Copilot Agent run in a sandboxed environment directly on your machine, " +
                "with restricted access to your filesystem, network connectivity, and system capabilities. " +
                "Applies to all chats and takes effect on your next message."
        );
        sandboxDesc.setPadding(new Insets(0, 0, 0, 22));

        // --- Filesystem Section ---
        VBox fsSection = new VBox(12);
        Label fsHeader = createSectionHeader("Filesystem");

        fsIncludeWorkingDirCheck = createCheckBox("Include working directory");
        Label fsWorkingDirHint = createHintLabel("Auto-add the current working directory to read/write paths.");
        fsWorkingDirHint.setPadding(new Insets(0, 0, 0, 22));

        fsClearPolicyOnExitCheck = createCheckBox("Clear policy on exit");
        Label fsClearPolicyHint = createHintLabel("Reset filesystem permissions when the sandbox exits.");
        fsClearPolicyHint.setPadding(new Insets(0, 0, 0, 22));

        Label fsPermsLabel = createFieldLabel("Filesystem permissions");
        Label fsPermsHint = createHintLabel("Configure additional filesystem permissions for sandboxed commands. Denied paths aren't enforced on Windows yet.");

        // Table toolbar (+, -)
        addFsPermBtn = createIconButton("+", () -> {
            SandboxPathPermission item = new SandboxPathPermission("", "Read");
            fsPermissionsData.add(item);
            fsPermissionsTable.getSelectionModel().select(item);
            notifyModified();
        });
        removeFsPermBtn = createIconButton("—", () -> {
            SandboxPathPermission sel = fsPermissionsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                fsPermissionsData.remove(sel);
                notifyModified();
            }
        });
        HBox fsToolbar = new HBox(6, addFsPermBtn, removeFsPermBtn);
        fsToolbar.setAlignment(Pos.CENTER_LEFT);

        fsPermissionsTable = buildFsPermissionsTable();

        fsSection.getChildren().addAll(
                fsHeader,
                fsIncludeWorkingDirCheck, fsWorkingDirHint,
                fsClearPolicyOnExitCheck, fsClearPolicyHint,
                fsPermsLabel, fsPermsHint,
                fsToolbar, fsPermissionsTable
        );

        // --- Network Section ---
        VBox netSection = new VBox(12);
        Label netHeader = createSectionHeader("Network");

        netAllowOutboundCheck = createCheckBox("Allow outbound connections");
        Label netOutboundHint = createHintLabel("Allow the sandboxed process to reach the Internet.");
        netOutboundHint.setPadding(new Insets(0, 0, 0, 22));

        netAllowLocalCheck = createCheckBox("Allow local network");
        Label netLocalHint = createHintLabel("Allow the sandboxed process to reach hosts on the local network.");
        netLocalHint.setPadding(new Insets(0, 0, 0, 22));

        Label hostAccessLabel = createFieldLabel("Host access");
        Label hostAccessHint = createHintLabel(
                "Configure host allow/block entries for sandboxed network access. " +
                "Per-host rules aren't reliable for network isolation and vary by platform: on macOS, allowed hosts fall back " +
                "to unrestricted outbound access and blocked hosts have no effect; on Linux, they can't reliably limit access " +
                "to selected hosts when outbound is off; on Windows, host filtering isn't supported yet."
        );

        // Network toolbar (+, -)
        addNetHostBtn = createIconButton("+", () -> {
            SandboxHostAccess item = new SandboxHostAccess("", "Allow");
            netHostAccessData.add(item);
            netHostAccessTable.getSelectionModel().select(item);
            notifyModified();
        });
        removeNetHostBtn = createIconButton("—", () -> {
            SandboxHostAccess sel = netHostAccessTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                netHostAccessData.remove(sel);
                notifyModified();
            }
        });
        HBox netToolbar = new HBox(6, addNetHostBtn, removeNetHostBtn);
        netToolbar.setAlignment(Pos.CENTER_LEFT);

        netHostAccessTable = buildNetHostAccessTable();

        netSection.getChildren().addAll(
                netHeader,
                netAllowOutboundCheck, netOutboundHint,
                netAllowLocalCheck, netLocalHint,
                hostAccessLabel, hostAccessHint,
                netToolbar, netHostAccessTable
        );

        getChildren().addAll(
                enableLocalSandboxCheck,
                sandboxDesc,
                fsSection,
                netSection
        );
    }

    private TableView<SandboxPathPermission> buildFsPermissionsTable() {
        TableView<SandboxPathPermission> table = new TableView<>(fsPermissionsData);
        table.setPrefHeight(130);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label placeholder = new Label("Nothing to show");
        placeholder.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        table.setPlaceholder(placeholder);

        TableColumn<SandboxPathPermission, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(data -> data.getValue().pathProperty());
        pathCol.setCellFactory(TextFieldTableCell.forTableColumn());
        pathCol.setOnEditCommit(event -> {
            event.getRowValue().setPath(event.getNewValue());
            notifyModified();
        });
        pathCol.setPrefWidth(500);

        TableColumn<SandboxPathPermission, String> permCol = new TableColumn<>("Permission");
        permCol.setCellValueFactory(data -> data.getValue().permissionProperty());
        permCol.setCellFactory(ComboBoxTableCell.forTableColumn("Read", "Read/Write", "Deny"));
        permCol.setOnEditCommit(event -> {
            event.getRowValue().setPermission(event.getNewValue());
            notifyModified();
        });
        permCol.setPrefWidth(160);

        table.getColumns().addAll(pathCol, permCol);
        return table;
    }

    private TableView<SandboxHostAccess> buildNetHostAccessTable() {
        TableView<SandboxHostAccess> table = new TableView<>(netHostAccessData);
        table.setPrefHeight(130);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label placeholder = new Label("Nothing to show");
        placeholder.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");
        table.setPlaceholder(placeholder);

        TableColumn<SandboxHostAccess, String> hostCol = new TableColumn<>("Host");
        hostCol.setCellValueFactory(data -> data.getValue().hostProperty());
        hostCol.setCellFactory(TextFieldTableCell.forTableColumn());
        hostCol.setOnEditCommit(event -> {
            event.getRowValue().setHost(event.getNewValue());
            notifyModified();
        });
        hostCol.setPrefWidth(500);

        TableColumn<SandboxHostAccess, String> accessCol = new TableColumn<>("Access");
        accessCol.setCellValueFactory(data -> data.getValue().accessProperty());
        accessCol.setCellFactory(ComboBoxTableCell.forTableColumn("Allow", "Block"));
        accessCol.setOnEditCommit(event -> {
            event.getRowValue().setAccess(event.getNewValue());
            notifyModified();
        });
        accessCol.setPrefWidth(160);

        table.getColumns().addAll(hostCol, accessCol);
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

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();

            enableLocalSandboxCheck.setSelected(initialSettings.isEnableLocalSandbox());
            fsIncludeWorkingDirCheck.setSelected(initialSettings.isFilesystemIncludeWorkingDirectory());
            fsClearPolicyOnExitCheck.setSelected(initialSettings.isFilesystemClearPolicyOnExit());

            fsPermissionsData.clear();
            for (SandboxPathPermission p : initialSettings.getFilesystemPermissions()) {
                fsPermissionsData.add(p.clone());
            }

            netAllowOutboundCheck.setSelected(initialSettings.isNetworkAllowOutbound());
            netAllowLocalCheck.setSelected(initialSettings.isNetworkAllowLocalNetwork());

            netHostAccessData.clear();
            for (SandboxHostAccess h : initialSettings.getNetworkHostAccess()) {
                netHostAccessData.add(h.clone());
            }
        } finally {
            updating = false;
        }
    }

    public void apply() {
        GitHubCopilotSandboxSettings s = collectCurrentSettings();
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
        GitHubCopilotSandboxSettings current = collectCurrentSettings();
        return !Objects.equals(initialSettings, current);
    }

    private GitHubCopilotSandboxSettings collectCurrentSettings() {
        GitHubCopilotSandboxSettings s = new GitHubCopilotSandboxSettings();
        s.setEnableLocalSandbox(enableLocalSandboxCheck.isSelected());
        s.setFilesystemIncludeWorkingDirectory(fsIncludeWorkingDirCheck.isSelected());
        s.setFilesystemClearPolicyOnExit(fsClearPolicyOnExitCheck.isSelected());

        List<SandboxPathPermission> perms = new ArrayList<>();
        for (SandboxPathPermission p : fsPermissionsData) {
            perms.add(p.clone());
        }
        s.setFilesystemPermissions(perms);

        s.setNetworkAllowOutbound(netAllowOutboundCheck.isSelected());
        s.setNetworkAllowLocalNetwork(netAllowLocalCheck.isSelected());

        List<SandboxHostAccess> hosts = new ArrayList<>();
        for (SandboxHostAccess h : netHostAccessData) {
            hosts.add(h.clone());
        }
        s.setNetworkHostAccess(hosts);

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
    public CheckBox getEnableLocalSandboxCheck() { return enableLocalSandboxCheck; }
    public CheckBox getFsIncludeWorkingDirCheck() { return fsIncludeWorkingDirCheck; }
    public CheckBox getFsClearPolicyOnExitCheck() { return fsClearPolicyOnExitCheck; }
    public ObservableList<SandboxPathPermission> getFsPermissionsData() { return fsPermissionsData; }
    public TableView<SandboxPathPermission> getFsPermissionsTable() { return fsPermissionsTable; }
    public Button getAddFsPermBtn() { return addFsPermBtn; }
    public Button getRemoveFsPermBtn() { return removeFsPermBtn; }

    public CheckBox getNetAllowOutboundCheck() { return netAllowOutboundCheck; }
    public CheckBox getNetAllowLocalCheck() { return netAllowLocalCheck; }
    public ObservableList<SandboxHostAccess> getNetHostAccessData() { return netHostAccessData; }
    public TableView<SandboxHostAccess> getNetHostAccessTable() { return netHostAccessTable; }
    public Button getAddNetHostBtn() { return addNetHostBtn; }
    public Button getRemoveNetHostBtn() { return removeNetHostBtn; }
}
