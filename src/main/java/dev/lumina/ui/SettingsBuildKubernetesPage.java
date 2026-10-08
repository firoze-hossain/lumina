package dev.lumina.ui;

import dev.lumina.kubernetes.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * Full settings page for Build, Execution, Deployment > Kubernetes (Screenshots 3, 4, 5).
 * Replicates all 9 sections dynamically matching IntelliJ IDEA 1:1.
 */
public class SettingsBuildKubernetesPage extends ScrollPane {

    private final KubernetesSettingsManager manager = KubernetesSettingsManager.getInstance();

    // 1. Tool locations
    private final TextField kubectlPathField = new TextField("kubectl");
    private final Button browseKubectlButton = new Button("📁");
    private final Button testKubectlButton = new Button("Test");
    private final Label kubectlTestResultLabel = new Label();

    private final TextField helmPathField = new TextField("helm");
    private final Button browseHelmButton = new Button("📁");
    private final Button testHelmButton = new Button("Test");
    private final Label helmTestResultLabel = new Label();

    // 2. Configuration
    private final ObservableList<KubernetesConfigFile> configFilesList = FXCollections.observableArrayList();
    private final TableView<KubernetesConfigFile> configFilesTable = new TableView<>(configFilesList);
    private final Button addConfigButton = new Button("+");
    private final Button removeConfigButton = new Button("—");
    private final CheckBox reloadConfigAutoCheck = new CheckBox("Reload configuration automatically");
    private final CheckBox refreshClusterResourcesCheck = new CheckBox("Refresh cluster resources after some action was performed");

    // 3. Pod Shell
    private final ComboBox<String> shellCommandCombo = new ComboBox<>();
    private final CheckBox shellCommandGlobalCheck = new CheckBox("Set this shell command globally for all projects");

    // 4. Appearance
    private final ComboBox<String> floatingToolbarCombo = new ComboBox<>();

    // 5. Logs (global settings)
    private final ToggleGroup downloadLogsGroup = new ToggleGroup();
    private final RadioButton pathDownloadLogsRadio = new RadioButton("Path to download logs to:");
    private final TextField customDownloadPathField = new TextField("Download to Scratches");
    private final Button browseDownloadPathButton = new Button("📁");
    private final RadioButton askDownloadLogsRadio = new RadioButton("Ask where to save log before downloading");
    private final CheckBox appendTimestampCheck = new CheckBox("Append timestamp to log file name");
    private final CheckBox dropAnsiSymbolsCheck = new CheckBox("Drop ANSI symbols in logs (can affect filtering)");

    private final CheckBox displayTimestampCheck = new CheckBox("Timestamp");
    private final CheckBox displaySourceCheck = new CheckBox("Source");
    private final CheckBox displayMessageCheck = new CheckBox("Message");

    private final ComboBox<String> clusterEventsModeCombo = new ComboBox<>();
    private final TextField logCacheSizeField = new TextField("300");
    private final TextField logUpdateDelayField = new TextField("250");

    // 6. Filters
    private final ObservableList<KubernetesLogFilter> logFiltersList = FXCollections.observableArrayList();
    private final TableView<KubernetesLogFilter> logFiltersTable = new TableView<>(logFiltersList);
    private final Button addFilterButton = new Button("+");
    private final Button removeFilterButton = new Button("—");
    private final Button resetFiltersButton = new Button("↶");

    // 7. Namespaces
    private final ObservableList<KubernetesNamespaceItem> namespacesList = FXCollections.observableArrayList();
    private final TableView<KubernetesNamespaceItem> namespacesTable = new TableView<>(namespacesList);
    private final Button addNamespaceButton = new Button("+");
    private final Button removeNamespaceButton = new Button("—");

    // 8. Kubectl Custom Arguments
    private final CheckBox appendServerPathFlagCheck = new CheckBox("If the context server path is present, add the --append-server-path kubectl flag in this project");
    private final ObservableList<KubernetesCustomArg> customArgsList = FXCollections.observableArrayList();
    private final TableView<KubernetesCustomArg> customArgsTable = new TableView<>(customArgsList);
    private final Button addCustomArgButton = new Button("+");
    private final Button removeCustomArgButton = new Button("—");

    // 9. Ephemeral Debug Containers
    private final ObservableList<KubernetesEphemeralContainer> ephemeralContainersList = FXCollections.observableArrayList();
    private final TableView<KubernetesEphemeralContainer> ephemeralContainersTable = new TableView<>(ephemeralContainersList);
    private final Button addEphemeralButton = new Button("+");
    private final Button removeEphemeralButton = new Button("—");
    private final Button editEphemeralButton = new Button("✏");

    private KubernetesSettings initialSettings;
    private boolean suppressEvents = false;
    private Runnable onModifiedListener;

    public SettingsBuildKubernetesPage() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: #1E1F22; -fx-border-color: transparent;");

        VBox contentBox = new VBox(20);
        contentBox.setStyle("-fx-background-color: #1E1F22;");
        contentBox.setPadding(new Insets(16, 24, 30, 24));

        buildSections(contentBox);
        setContent(contentBox);

        initEventHandlers();
        loadData();
    }

    private void buildSections(VBox root) {
        // --- 1. Tool locations (global settings) ---
        VBox toolLocSection = new VBox(10);
        toolLocSection.getChildren().add(createSectionHeader("Tool locations (global settings)"));

        GridPane toolGrid = new GridPane();
        toolGrid.setHgap(12);
        toolGrid.setVgap(8);
        toolGrid.getColumnConstraints().addAll(
                new ColumnConstraints(180),
                new ColumnConstraints(400, 500, Double.MAX_VALUE, Priority.ALWAYS, javafx.geometry.HPos.LEFT, true)
        );

        Label kubectlLabel = new Label("Path to kubectl executable:");
        styleLabel(kubectlLabel);
        styleTextField(kubectlPathField);
        styleBrowseButton(browseKubectlButton);
        HBox kubectlBox = new HBox(6, kubectlPathField, browseKubectlButton);
        HBox.setHgrow(kubectlPathField, Priority.ALWAYS);

        styleSmallButton(testKubectlButton);
        kubectlTestResultLabel.setStyle("-fx-font-size: 11px;");
        HBox testKubectlBox = new HBox(10, testKubectlButton, kubectlTestResultLabel);
        testKubectlBox.setAlignment(Pos.CENTER_LEFT);

        toolGrid.add(kubectlLabel, 0, 0);
        toolGrid.add(kubectlBox, 1, 0);
        toolGrid.add(testKubectlBox, 1, 1);

        Label helmLabel = new Label("Path to helm executable:");
        styleLabel(helmLabel);
        styleTextField(helmPathField);
        styleBrowseButton(browseHelmButton);
        HBox helmBox = new HBox(6, helmPathField, browseHelmButton);
        HBox.setHgrow(helmPathField, Priority.ALWAYS);

        styleSmallButton(testHelmButton);
        helmTestResultLabel.setStyle("-fx-font-size: 11px;");
        HBox testHelmBox = new HBox(10, testHelmButton, helmTestResultLabel);
        testHelmBox.setAlignment(Pos.CENTER_LEFT);

        toolGrid.add(helmLabel, 0, 2);
        toolGrid.add(helmBox, 1, 2);
        toolGrid.add(testHelmBox, 1, 3);

        toolLocSection.getChildren().add(toolGrid);
        root.getChildren().add(toolLocSection);

        // --- 2. Configuration ---
        VBox configSection = new VBox(10);
        configSection.getChildren().add(createSectionHeaderWithSeparator("Configuration"));

        HBox configToolbar = new HBox(4, addConfigButton, removeConfigButton);
        configToolbar.setAlignment(Pos.CENTER_LEFT);
        styleToolbarButton(addConfigButton, "Add Configuration File");
        styleToolbarButton(removeConfigButton, "Remove Selected Configuration");

        setupConfigFilesTable();
        configFilesTable.setPrefHeight(130);

        styleCheckBox(reloadConfigAutoCheck);
        styleCheckBox(refreshClusterResourcesCheck);

        configSection.getChildren().addAll(configToolbar, configFilesTable, reloadConfigAutoCheck, refreshClusterResourcesCheck);
        root.getChildren().add(configSection);

        // --- 3. Pod Shell ---
        VBox shellSection = new VBox(10);
        shellSection.getChildren().add(createSectionHeaderWithSeparator("Pod Shell"));

        shellCommandCombo.setEditable(true);
        shellCommandCombo.getItems().addAll("/bin/sh", "/bin/bash", "sh", "bash");
        styleComboBox(shellCommandCombo);
        shellCommandCombo.setPrefWidth(400);

        HBox shellBox = new HBox(12, new Label("Command to run shell inside container:") {{ styleLabel(this); }}, shellCommandCombo);
        shellBox.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(shellCommandGlobalCheck);
        VBox.setMargin(shellCommandGlobalCheck, new Insets(0, 0, 0, 245));

        shellSection.getChildren().addAll(shellBox, shellCommandGlobalCheck);
        root.getChildren().add(shellSection);

        // --- 4. Appearance ---
        VBox appSection = new VBox(10);
        appSection.getChildren().add(createSectionHeaderWithSeparator("Appearance"));

        floatingToolbarCombo.getItems().addAll("Always Show", "Never Show", "Auto");
        styleComboBox(floatingToolbarCombo);
        floatingToolbarCombo.setPrefWidth(200);

        HBox appBox = new HBox(12, new Label("Show floating toolbar in editor:") {{ styleLabel(this); }}, floatingToolbarCombo);
        appBox.setAlignment(Pos.CENTER_LEFT);

        appSection.getChildren().add(appBox);
        root.getChildren().add(appSection);

        // --- 5. Logs (global settings) ---
        VBox logsSection = new VBox(10);
        logsSection.getChildren().add(createSectionHeaderWithSeparator("Logs (global settings)"));

        pathDownloadLogsRadio.setToggleGroup(downloadLogsGroup);
        askDownloadLogsRadio.setToggleGroup(downloadLogsGroup);
        styleRadioButton(pathDownloadLogsRadio);
        styleRadioButton(askDownloadLogsRadio);

        styleTextField(customDownloadPathField);
        styleBrowseButton(browseDownloadPathButton);
        HBox dlPathBox = new HBox(6, customDownloadPathField, browseDownloadPathButton);
        HBox.setHgrow(customDownloadPathField, Priority.ALWAYS);

        HBox radioPathBox = new HBox(10, pathDownloadLogsRadio, dlPathBox);
        radioPathBox.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(appendTimestampCheck);
        styleCheckBox(dropAnsiSymbolsCheck);

        Label fieldsLbl = new Label("Fields displayed in logs by default");
        fieldsLbl.setStyle("-fx-text-fill: #9DA0A8; -fx-font-size: 12px;");

        styleCheckBox(displayTimestampCheck);
        styleCheckBox(displaySourceCheck);
        styleCheckBox(displayMessageCheck);

        VBox fieldsBox = new VBox(6, fieldsLbl, displayTimestampCheck, displaySourceCheck, displayMessageCheck);
        fieldsBox.setPadding(new Insets(4, 0, 4, 10));

        clusterEventsModeCombo.getItems().addAll("In editor", "In tool window");
        styleComboBox(clusterEventsModeCombo);
        clusterEventsModeCombo.setPrefWidth(250);
        HBox clusterBox = new HBox(12, new Label("Cluster events presentation mode:") {{ styleLabel(this); }}, clusterEventsModeCombo);
        clusterBox.setAlignment(Pos.CENTER_LEFT);

        styleTextField(logCacheSizeField);
        logCacheSizeField.setPrefWidth(120);
        HBox cacheBox = new HBox(12, new Label("Log cache size (in MB) at which a warning is shown:") {{ styleLabel(this); }}, logCacheSizeField);
        cacheBox.setAlignment(Pos.CENTER_LEFT);

        styleTextField(logUpdateDelayField);
        logUpdateDelayField.setPrefWidth(120);
        HBox delayBox = new HBox(12, new Label("Log editor update delay in milliseconds:") {{ styleLabel(this); }}, logUpdateDelayField);
        delayBox.setAlignment(Pos.CENTER_LEFT);

        logsSection.getChildren().addAll(
                radioPathBox,
                askDownloadLogsRadio,
                appendTimestampCheck,
                dropAnsiSymbolsCheck,
                fieldsBox,
                clusterBox,
                cacheBox,
                delayBox
        );
        root.getChildren().add(logsSection);

        // --- 6. Filters (under Logs) ---
        VBox filterSection = new VBox(8);
        filterSection.getChildren().add(createSubSectionHeader("Filters"));

        HBox filterToolbar = new HBox(4, addFilterButton, removeFilterButton, resetFiltersButton);
        filterToolbar.setAlignment(Pos.CENTER_LEFT);
        styleToolbarButton(addFilterButton, "Add Filter");
        styleToolbarButton(removeFilterButton, "Remove Selected Filter");
        styleToolbarButton(resetFiltersButton, "Reset to Default Filters");

        setupLogFiltersTable();
        logFiltersTable.setPrefHeight(150);

        filterSection.getChildren().addAll(filterToolbar, logFiltersTable);
        root.getChildren().add(filterSection);

        // --- 7. Namespaces ---
        VBox nsSection = new VBox(8);
        nsSection.getChildren().add(createSectionHeaderWithSeparator("Namespaces (applied when they cannot be loaded from the cluster)"));

        HBox nsToolbar = new HBox(4, addNamespaceButton, removeNamespaceButton);
        nsToolbar.setAlignment(Pos.CENTER_LEFT);
        styleToolbarButton(addNamespaceButton, "Add Namespace");
        styleToolbarButton(removeNamespaceButton, "Remove Selected Namespace");

        setupNamespacesTable();
        namespacesTable.setPrefHeight(130);

        nsSection.getChildren().addAll(nsToolbar, namespacesTable);
        root.getChildren().add(nsSection);

        // --- 8. Kubectl Custom Arguments ---
        VBox customArgsSection = new VBox(8);
        customArgsSection.getChildren().add(createSectionHeaderWithSeparator("Kubectl Custom Arguments"));

        styleCheckBox(appendServerPathFlagCheck);

        HBox argsToolbar = new HBox(4, addCustomArgButton, removeCustomArgButton);
        argsToolbar.setAlignment(Pos.CENTER_LEFT);
        styleToolbarButton(addCustomArgButton, "Add Custom Argument");
        styleToolbarButton(removeCustomArgButton, "Remove Selected Argument");

        setupCustomArgsTable();
        customArgsTable.setPrefHeight(130);

        customArgsSection.getChildren().addAll(appendServerPathFlagCheck, argsToolbar, customArgsTable);
        root.getChildren().add(customArgsSection);

        // --- 9. Ephemeral Debug Containers ---
        VBox ephemSection = new VBox(8);
        ephemSection.getChildren().add(createSectionHeaderWithSeparator("Ephemeral Debug Containers"));

        HBox ephemToolbar = new HBox(4, addEphemeralButton, removeEphemeralButton, editEphemeralButton);
        ephemToolbar.setAlignment(Pos.CENTER_LEFT);
        styleToolbarButton(addEphemeralButton, "Add Ephemeral Container");
        styleToolbarButton(removeEphemeralButton, "Remove Selected Container");
        styleToolbarButton(editEphemeralButton, "Edit Selected Container");

        setupEphemeralTable();
        ephemeralContainersTable.setPrefHeight(120);

        ephemSection.getChildren().addAll(ephemToolbar, ephemeralContainersTable);
        root.getChildren().add(ephemSection);
    }

    // --- Table Setups ---

    private void setupConfigFilesTable() {
        configFilesTable.setEditable(true);
        configFilesTable.setStyle("-fx-background-color: #1E1F22; -fx-base: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40;");
        configFilesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label emptyLabel = new Label("Determined by kubectl");
        emptyLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        configFilesTable.setPlaceholder(emptyLabel);

        TableColumn<KubernetesConfigFile, Boolean> validCol = new TableColumn<>("Valid");
        validCol.setCellValueFactory(d -> new SimpleBooleanProperty(d.getValue().isValid()));
        validCol.setCellFactory(CheckBoxTableCell.forTableColumn(validCol));
        validCol.setMaxWidth(70);

        TableColumn<KubernetesConfigFile, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPath()));
        pathCol.setCellFactory(TextFieldTableCell.forTableColumn());
        pathCol.setOnEditCommit(e -> {
            e.getRowValue().setPath(e.getNewValue());
            notifyModified();
        });

        TableColumn<KubernetesConfigFile, String> scopeCol = new TableColumn<>("Scope");
        scopeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getScope()));
        scopeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        scopeCol.setOnEditCommit(e -> {
            e.getRowValue().setScope(e.getNewValue());
            notifyModified();
        });
        scopeCol.setMaxWidth(140);

        configFilesTable.getColumns().addAll(validCol, pathCol, scopeCol);
    }

    private void setupLogFiltersTable() {
        logFiltersTable.setEditable(true);
        logFiltersTable.setStyle("-fx-background-color: #1E1F22; -fx-base: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40;");
        logFiltersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<KubernetesLogFilter, Boolean> enabledCol = new TableColumn<>("Enabled");
        enabledCol.setCellValueFactory(d -> {
            SimpleBooleanProperty p = new SimpleBooleanProperty(d.getValue().isEnabled());
            p.addListener((o, ov, nv) -> {
                d.getValue().setEnabled(nv);
                notifyModified();
            });
            return p;
        });
        enabledCol.setCellFactory(CheckBoxTableCell.forTableColumn(enabledCol));
        enabledCol.setMaxWidth(70);

        TableColumn<KubernetesLogFilter, String> patternCol = new TableColumn<>("Regex Pattern");
        patternCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPattern()));
        patternCol.setCellFactory(TextFieldTableCell.forTableColumn());
        patternCol.setOnEditCommit(e -> {
            e.getRowValue().setPattern(e.getNewValue());
            logFiltersTable.refresh();
            notifyModified();
        });

        TableColumn<KubernetesLogFilter, Boolean> boldCol = new TableColumn<>("Bold");
        boldCol.setCellValueFactory(d -> {
            SimpleBooleanProperty p = new SimpleBooleanProperty(d.getValue().isBold());
            p.addListener((o, ov, nv) -> {
                d.getValue().setBold(nv);
                logFiltersTable.refresh();
                notifyModified();
            });
            return p;
        });
        boldCol.setCellFactory(CheckBoxTableCell.forTableColumn(boldCol));
        boldCol.setMaxWidth(70);

        TableColumn<KubernetesLogFilter, Boolean> italicCol = new TableColumn<>("Italic");
        italicCol.setCellValueFactory(d -> {
            SimpleBooleanProperty p = new SimpleBooleanProperty(d.getValue().isItalic());
            p.addListener((o, ov, nv) -> {
                d.getValue().setItalic(nv);
                logFiltersTable.refresh();
                notifyModified();
            });
            return p;
        });
        italicCol.setCellFactory(CheckBoxTableCell.forTableColumn(italicCol));
        italicCol.setMaxWidth(70);

        TableColumn<KubernetesLogFilter, String> colorCol = new TableColumn<>("Color");
        colorCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPattern()));
        colorCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    KubernetesLogFilter f = getTableRow().getItem();
                    setText(f.getPattern());
                    String style = String.format("-fx-text-fill: %s; %s %s",
                            f.getColorHex(),
                            f.isBold() ? "-fx-font-weight: bold;" : "",
                            f.isItalic() ? "-fx-font-style: italic;" : "");
                    setStyle(style);
                }
            }
        });

        logFiltersTable.getColumns().addAll(enabledCol, patternCol, boldCol, italicCol, colorCol);
    }

    private void setupNamespacesTable() {
        namespacesTable.setEditable(true);
        namespacesTable.setStyle("-fx-background-color: #1E1F22; -fx-base: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40;");
        namespacesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label emptyLabel = new Label("Add namespaces\nto use when they cannot\nbe loaded from the cluster");
        emptyLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        emptyLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        namespacesTable.setPlaceholder(emptyLabel);

        TableColumn<KubernetesNamespaceItem, String> nsCol = new TableColumn<>("Namespace");
        nsCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNamespace()));
        nsCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nsCol.setOnEditCommit(e -> {
            e.getRowValue().setNamespace(e.getNewValue());
            notifyModified();
        });

        TableColumn<KubernetesNamespaceItem, String> scopeCol = new TableColumn<>("Scope");
        scopeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getScope()));
        scopeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        scopeCol.setOnEditCommit(e -> {
            e.getRowValue().setScope(e.getNewValue());
            notifyModified();
        });
        scopeCol.setMaxWidth(140);

        namespacesTable.getColumns().addAll(nsCol, scopeCol);
    }

    private void setupCustomArgsTable() {
        customArgsTable.setEditable(true);
        customArgsTable.setStyle("-fx-background-color: #1E1F22; -fx-base: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40;");
        customArgsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label emptyLabel = new Label("Add custom cluster proxy configuration\nFor example, for `myCluster` parameter `--append-server-path`");
        emptyLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        emptyLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        customArgsTable.setPlaceholder(emptyLabel);

        TableColumn<KubernetesCustomArg, String> clusCol = new TableColumn<>("Cluster");
        clusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCluster()));
        clusCol.setCellFactory(TextFieldTableCell.forTableColumn());
        clusCol.setOnEditCommit(e -> {
            e.getRowValue().setCluster(e.getNewValue());
            notifyModified();
        });

        TableColumn<KubernetesCustomArg, String> paramCol = new TableColumn<>("Parameter");
        paramCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getParameter()));
        paramCol.setCellFactory(TextFieldTableCell.forTableColumn());
        paramCol.setOnEditCommit(e -> {
            e.getRowValue().setParameter(e.getNewValue());
            notifyModified();
        });

        customArgsTable.getColumns().addAll(clusCol, paramCol);
    }

    private void setupEphemeralTable() {
        ephemeralContainersTable.setEditable(true);
        ephemeralContainersTable.setStyle("-fx-background-color: #1E1F22; -fx-base: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40;");
        ephemeralContainersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label emptyLabel = new Label("Nothing to show");
        emptyLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        ephemeralContainersTable.setPlaceholder(emptyLabel);

        TableColumn<KubernetesEphemeralContainer, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> {
            e.getRowValue().setName(e.getNewValue());
            notifyModified();
        });
        nameCol.setMaxWidth(180);

        TableColumn<KubernetesEphemeralContainer, String> paramCol = new TableColumn<>("Parameters");
        paramCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getParameters()));
        paramCol.setCellFactory(TextFieldTableCell.forTableColumn());
        paramCol.setOnEditCommit(e -> {
            e.getRowValue().setParameters(e.getNewValue());
            notifyModified();
        });

        ephemeralContainersTable.getColumns().addAll(nameCol, paramCol);
    }

    // --- Event Handlers ---

    private void initEventHandlers() {
        browseKubectlButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select kubectl Executable");
            File f = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                kubectlPathField.setText(f.getAbsolutePath());
                notifyModified();
            }
        });

        testKubectlButton.setOnAction(e -> {
            kubectlTestResultLabel.setText("Testing...");
            kubectlTestResultLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 11px;");
            new Thread(() -> {
                KubernetesSettingsManager.ToolTestResult res = manager.testTool(kubectlPathField.getText(), "kubectl");
                javafx.application.Platform.runLater(() -> {
                    if (res.success()) {
                        kubectlTestResultLabel.setText("✓ " + res.message());
                        kubectlTestResultLabel.setStyle("-fx-text-fill: #59A869; -fx-font-size: 11px;");
                    } else {
                        kubectlTestResultLabel.setText("✗ " + res.message());
                        kubectlTestResultLabel.setStyle("-fx-text-fill: #E05555; -fx-font-size: 11px;");
                    }
                });
            }).start();
        });

        browseHelmButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select helm Executable");
            File f = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                helmPathField.setText(f.getAbsolutePath());
                notifyModified();
            }
        });

        testHelmButton.setOnAction(e -> {
            helmTestResultLabel.setText("Testing...");
            helmTestResultLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 11px;");
            new Thread(() -> {
                KubernetesSettingsManager.ToolTestResult res = manager.testTool(helmPathField.getText(), "helm");
                javafx.application.Platform.runLater(() -> {
                    if (res.success()) {
                        helmTestResultLabel.setText("✓ " + res.message());
                        helmTestResultLabel.setStyle("-fx-text-fill: #59A869; -fx-font-size: 11px;");
                    } else {
                        helmTestResultLabel.setText("✗ " + res.message());
                        helmTestResultLabel.setStyle("-fx-text-fill: #E05555; -fx-font-size: 11px;");
                    }
                });
            }).start();
        });

        browseDownloadPathButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Logs Download Directory");
            File dir = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (dir != null) {
                customDownloadPathField.setText(dir.getAbsolutePath());
                notifyModified();
            }
        });

        // Add / Remove buttons for tables
        addConfigButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Kubeconfig File");
            File f = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            String p = f != null ? f.getAbsolutePath() : "~/.kube/config";
            configFilesList.add(new KubernetesConfigFile(true, p, "Project"));
            notifyModified();
        });
        removeConfigButton.setOnAction(e -> {
            int idx = configFilesTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                configFilesList.remove(idx);
                notifyModified();
            }
        });

        addFilterButton.setOnAction(e -> {
            KubernetesLogFilter f = new KubernetesLogFilter(true, "(?i)\\b(debug)\\b", false, false, "#3574F0");
            logFiltersList.add(f);
            logFiltersTable.getSelectionModel().select(f);
            notifyModified();
        });
        removeFilterButton.setOnAction(e -> {
            int idx = logFiltersTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                logFiltersList.remove(idx);
                notifyModified();
            }
        });
        resetFiltersButton.setOnAction(e -> {
            logFiltersList.clear();
            KubernetesSettings defaults = new KubernetesSettings();
            for (KubernetesLogFilter f : defaults.getLogFilters()) {
                logFiltersList.add(f.clone());
            }
            notifyModified();
        });

        addNamespaceButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("default");
            dialog.setTitle("Add Namespace");
            dialog.setHeaderText("Enter fallback namespace:");
            dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22;");
            dialog.showAndWait().ifPresent(ns -> {
                if (!ns.isBlank()) {
                    namespacesList.add(new KubernetesNamespaceItem(ns.trim(), "Project"));
                    notifyModified();
                }
            });
        });
        removeNamespaceButton.setOnAction(e -> {
            int idx = namespacesTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                namespacesList.remove(idx);
                notifyModified();
            }
        });

        addCustomArgButton.setOnAction(e -> {
            customArgsList.add(new KubernetesCustomArg("myCluster", "--append-server-path"));
            notifyModified();
        });
        removeCustomArgButton.setOnAction(e -> {
            int idx = customArgsTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                customArgsList.remove(idx);
                notifyModified();
            }
        });

        addEphemeralButton.setOnAction(e -> {
            ephemeralContainersList.add(new KubernetesEphemeralContainer("debug-container", "-i -t --image=busybox"));
            notifyModified();
        });
        removeEphemeralButton.setOnAction(e -> {
            int idx = ephemeralContainersTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                ephemeralContainersList.remove(idx);
                notifyModified();
            }
        });
        editEphemeralButton.setOnAction(e -> {
            KubernetesEphemeralContainer item = ephemeralContainersTable.getSelectionModel().getSelectedItem();
            if (item != null) {
                TextInputDialog dialog = new TextInputDialog(item.getParameters());
                dialog.setTitle("Edit Ephemeral Container");
                dialog.setHeaderText("Edit parameters for " + item.getName() + ":");
                dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22;");
                dialog.showAndWait().ifPresent(params -> {
                    item.setParameters(params);
                    ephemeralContainersTable.refresh();
                    notifyModified();
                });
            }
        });

        // Field change listeners
        kubectlPathField.textProperty().addListener((o, ov, nv) -> onFieldChanged());
        helmPathField.textProperty().addListener((o, ov, nv) -> onFieldChanged());
        reloadConfigAutoCheck.selectedProperty().addListener((o, ov, nv) -> onFieldChanged());
        refreshClusterResourcesCheck.selectedProperty().addListener((o, ov, nv) -> onFieldChanged());
        shellCommandCombo.valueProperty().addListener((o, ov, nv) -> onFieldChanged());
        shellCommandGlobalCheck.selectedProperty().addListener((o, ov, nv) -> onFieldChanged());
        floatingToolbarCombo.valueProperty().addListener((o, ov, nv) -> onFieldChanged());

        pathDownloadLogsRadio.selectedProperty().addListener((o, ov, nv) -> onFieldChanged());
        askDownloadLogsRadio.selectedProperty().addListener((o, ov, nv) -> onFieldChanged());
        customDownloadPathField.textProperty().addListener((o, ov, nv) -> onFieldChanged());
        appendTimestampCheck.selectedProperty().addListener((o, ov, nv) -> onFieldChanged());
        dropAnsiSymbolsCheck.selectedProperty().addListener((o, ov, nv) -> onFieldChanged());
        displayTimestampCheck.selectedProperty().addListener((o, ov, nv) -> onFieldChanged());
        displaySourceCheck.selectedProperty().addListener((o, ov, nv) -> onFieldChanged());
        displayMessageCheck.selectedProperty().addListener((o, ov, nv) -> onFieldChanged());

        clusterEventsModeCombo.valueProperty().addListener((o, ov, nv) -> onFieldChanged());
        logCacheSizeField.textProperty().addListener((o, ov, nv) -> onFieldChanged());
        logUpdateDelayField.textProperty().addListener((o, ov, nv) -> onFieldChanged());
        appendServerPathFlagCheck.selectedProperty().addListener((o, ov, nv) -> onFieldChanged());
    }

    private void onFieldChanged() {
        if (!suppressEvents) {
            notifyModified();
        }
    }

    // --- State Persistence & Lifecycle ---

    public void loadData() {
        suppressEvents = true;
        try {
            initialSettings = manager.getSettings();

            kubectlPathField.setText(initialSettings.getKubectlPath());
            helmPathField.setText(initialSettings.getHelmPath());

            configFilesList.clear();
            for (KubernetesConfigFile f : initialSettings.getConfigFiles()) {
                configFilesList.add(f.clone());
            }
            reloadConfigAutoCheck.setSelected(initialSettings.isReloadConfigAutomatically());
            refreshClusterResourcesCheck.setSelected(initialSettings.isRefreshClusterResources());

            shellCommandCombo.setValue(initialSettings.getShellCommand());
            shellCommandGlobalCheck.setSelected(initialSettings.isShellCommandGlobal());

            floatingToolbarCombo.setValue(initialSettings.getFloatingToolbarMode());

            if ("ASK".equalsIgnoreCase(initialSettings.getDownloadLogsMode())) {
                askDownloadLogsRadio.setSelected(true);
            } else {
                pathDownloadLogsRadio.setSelected(true);
            }
            customDownloadPathField.setText(initialSettings.getCustomDownloadPath());
            appendTimestampCheck.setSelected(initialSettings.isAppendTimestampToLogFileName());
            dropAnsiSymbolsCheck.setSelected(initialSettings.isDropAnsiSymbols());

            displayTimestampCheck.setSelected(initialSettings.isDisplayTimestamp());
            displaySourceCheck.setSelected(initialSettings.isDisplaySource());
            displayMessageCheck.setSelected(initialSettings.isDisplayMessage());

            clusterEventsModeCombo.setValue(initialSettings.getClusterEventsPresentationMode());
            logCacheSizeField.setText(String.valueOf(initialSettings.getLogCacheSizeMb()));
            logUpdateDelayField.setText(String.valueOf(initialSettings.getLogEditorUpdateDelayMs()));

            logFiltersList.clear();
            for (KubernetesLogFilter f : initialSettings.getLogFilters()) {
                logFiltersList.add(f.clone());
            }

            namespacesList.clear();
            for (KubernetesNamespaceItem ns : initialSettings.getNamespaces()) {
                namespacesList.add(ns.clone());
            }

            appendServerPathFlagCheck.setSelected(initialSettings.isAppendServerPathFlag());

            customArgsList.clear();
            for (KubernetesCustomArg a : initialSettings.getCustomArgs()) {
                customArgsList.add(a.clone());
            }

            ephemeralContainersList.clear();
            for (KubernetesEphemeralContainer c : initialSettings.getEphemeralContainers()) {
                ephemeralContainersList.add(c.clone());
            }

            kubectlTestResultLabel.setText("");
            helmTestResultLabel.setText("");
        } finally {
            suppressEvents = false;
        }
    }

    public KubernetesSettings collectCurrentSettings() {
        KubernetesSettings s = new KubernetesSettings();
        s.setKubectlPath(kubectlPathField.getText().trim());
        s.setHelmPath(helmPathField.getText().trim());

        s.setConfigFiles(new ArrayList<>(configFilesList));
        s.setReloadConfigAutomatically(reloadConfigAutoCheck.isSelected());
        s.setRefreshClusterResources(refreshClusterResourcesCheck.isSelected());

        s.setShellCommand(shellCommandCombo.getValue() != null ? shellCommandCombo.getValue() : "/bin/sh");
        s.setShellCommandGlobal(shellCommandGlobalCheck.isSelected());

        s.setFloatingToolbarMode(floatingToolbarCombo.getValue() != null ? floatingToolbarCombo.getValue() : "Always Show");

        s.setDownloadLogsMode(askDownloadLogsRadio.isSelected() ? "ASK" : "SCRATCHES");
        s.setCustomDownloadPath(customDownloadPathField.getText().trim());
        s.setAppendTimestampToLogFileName(appendTimestampCheck.isSelected());
        s.setDropAnsiSymbols(dropAnsiSymbolsCheck.isSelected());

        s.setDisplayTimestamp(displayTimestampCheck.isSelected());
        s.setDisplaySource(displaySourceCheck.isSelected());
        s.setDisplayMessage(displayMessageCheck.isSelected());

        s.setClusterEventsPresentationMode(clusterEventsModeCombo.getValue() != null ? clusterEventsModeCombo.getValue() : "In editor");

        try {
            s.setLogCacheSizeMb(Integer.parseInt(logCacheSizeField.getText().trim()));
        } catch (NumberFormatException e) {
            s.setLogCacheSizeMb(300);
        }

        try {
            s.setLogEditorUpdateDelayMs(Integer.parseInt(logUpdateDelayField.getText().trim()));
        } catch (NumberFormatException e) {
            s.setLogEditorUpdateDelayMs(250);
        }

        s.setLogFilters(new ArrayList<>(logFiltersList));
        s.setNamespaces(new ArrayList<>(namespacesList));
        s.setAppendServerPathFlag(appendServerPathFlagCheck.isSelected());
        s.setCustomArgs(new ArrayList<>(customArgsList));
        s.setEphemeralContainers(new ArrayList<>(ephemeralContainersList));

        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        KubernetesSettings curr = collectCurrentSettings();
        return !Objects.equals(initialSettings, curr);
    }

    public void apply() {
        KubernetesSettings curr = collectCurrentSettings();
        manager.setSettings(curr);
        initialSettings = curr.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // --- UI Helpers & Styling ---

    private Label createSectionHeader(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        return lbl;
    }

    private Node createSectionHeaderWithSeparator(String title) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1; -fx-pref-height: 1; -fx-max-height: 1;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(lbl, line);
        return header;
    }

    private Label createSubSectionHeader(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");
        return lbl;
    }

    private void styleLabel(Label lbl) {
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-font-size: 13px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
    }

    private void styleBrowseButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand;");
    }

    private void styleSmallButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 12; -fx-cursor: hand;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleComboBox(ComboBox<?> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void styleToolbarButton(Button btn, String tooltipText) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 7;");
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373C; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 7; -fx-background-radius: 4;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 7;"));
    }

    // Getters for testing
    public TextField getKubectlPathField() { return kubectlPathField; }
    public TextField getHelmPathField() { return helmPathField; }
    public Button getTestKubectlButton() { return testKubectlButton; }
    public Button getTestHelmButton() { return testHelmButton; }
    public Label getKubectlTestResultLabel() { return kubectlTestResultLabel; }
    public Label getHelmTestResultLabel() { return helmTestResultLabel; }
    public TableView<KubernetesConfigFile> getConfigFilesTable() { return configFilesTable; }
    public ObservableList<KubernetesConfigFile> getConfigFilesList() { return configFilesList; }
    public CheckBox getReloadConfigAutoCheck() { return reloadConfigAutoCheck; }
    public CheckBox getRefreshClusterResourcesCheck() { return refreshClusterResourcesCheck; }
    public ComboBox<String> getShellCommandCombo() { return shellCommandCombo; }
    public CheckBox getShellCommandGlobalCheck() { return shellCommandGlobalCheck; }
    public ComboBox<String> getFloatingToolbarCombo() { return floatingToolbarCombo; }
    public RadioButton getPathDownloadLogsRadio() { return pathDownloadLogsRadio; }
    public RadioButton getAskDownloadLogsRadio() { return askDownloadLogsRadio; }
    public TextField getCustomDownloadPathField() { return customDownloadPathField; }
    public CheckBox getAppendTimestampCheck() { return appendTimestampCheck; }
    public CheckBox getDropAnsiSymbolsCheck() { return dropAnsiSymbolsCheck; }
    public CheckBox getDisplayTimestampCheck() { return displayTimestampCheck; }
    public CheckBox getDisplaySourceCheck() { return displaySourceCheck; }
    public CheckBox getDisplayMessageCheck() { return displayMessageCheck; }
    public ComboBox<String> getClusterEventsModeCombo() { return clusterEventsModeCombo; }
    public TextField getLogCacheSizeField() { return logCacheSizeField; }
    public TextField getLogUpdateDelayField() { return logUpdateDelayField; }
    public TableView<KubernetesLogFilter> getLogFiltersTable() { return logFiltersTable; }
    public ObservableList<KubernetesLogFilter> getLogFiltersList() { return logFiltersList; }
    public TableView<KubernetesNamespaceItem> getNamespacesTable() { return namespacesTable; }
    public ObservableList<KubernetesNamespaceItem> getNamespacesList() { return namespacesList; }
    public CheckBox getAppendServerPathFlagCheck() { return appendServerPathFlagCheck; }
    public TableView<KubernetesCustomArg> getCustomArgsTable() { return customArgsTable; }
    public ObservableList<KubernetesCustomArg> getCustomArgsList() { return customArgsList; }
    public TableView<KubernetesEphemeralContainer> getEphemeralContainersTable() { return ephemeralContainersTable; }
    public ObservableList<KubernetesEphemeralContainer> getEphemeralContainersList() { return ephemeralContainersList; }
}
