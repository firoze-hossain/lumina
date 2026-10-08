package dev.lumina.ui;

import dev.lumina.php.*;
import dev.lumina.util.Settings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

/**
 * 1:1 dynamic replica of IntelliJ IDEA / PhpStorm PHP settings page
 * (Languages & Frameworks > PHP) matching all 4 tabs:
 * 1. Include Path
 * 2. PHP Runtime
 * 3. Analysis
 * 4. Composer Files
 */
public class SettingsLanguagesPHPPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();
    private Runnable onModifiedListener;

    // Top Controls
    private ComboBox<String> languageLevelCombo;
    private ComboBox<PhpInterpreter> interpreterCombo;
    private Button configureInterpreterButton;

    // Tab buttons
    private Button tabIncludePathBtn;
    private Button tabRuntimeBtn;
    private Button tabAnalysisBtn;
    private Button tabComposerBtn;
    private StackPane tabContentContainer;

    // Tab 1: Include Path
    private VBox includePathPane;
    private ListView<String> includePathListView;
    private Label includePathEmptyLabel;
    private List<String> currentIncludePaths = new ArrayList<>();

    // Tab 2: PHP Runtime
    private VBox runtimePane;
    private TreeView<RuntimeTreeNode> runtimeTreeView;
    private Button syncExtensionsBtn;
    private Label syncStatusLabel;
    private VBox advancedStubsBox;
    private Label advancedToggleLabel;
    private TextField customStubsField;
    private boolean advancedExpanded = false;
    private final Map<String, Boolean> currentExtensionStates = new HashMap<>();

    // Tab 3: Analysis
    private VBox analysisPane;
    private ComboBox<String> callTreeDepthCombo;
    private CheckBox skipCallsWithConstantParamsCheck;
    private ListView<String> uncheckedExceptionsListView;
    private List<String> currentUncheckedExceptions = new ArrayList<>();
    private boolean customFormatExpanded = false;
    private VBox customFormatBox;
    private Label customFormatToggleLabel;
    private TextField documentRootField;

    // Tab 4: Composer Files
    private VBox composerPane;
    private ListView<PhpComposerFileConfig> composerListView;
    private Label composerEmptyLabel;
    private List<PhpComposerFileConfig> currentComposerFiles = new ArrayList<>();

    // Snapshot for dirty checking
    private PhpLanguageLevel initialLevel;
    private String initialInterpreterId;
    private List<String> initialIncludePaths;
    private Map<String, Boolean> initialExtensionStates;
    private String initialCallTreeDepth;
    private boolean initialSkipConstantParams;
    private List<String> initialUncheckedExceptions;
    private String initialDocumentRoot;
    private String initialCustomStubs;
    private List<PhpComposerFileConfig> initialComposerFiles;

    public SettingsLanguagesPHPPage() {
        setSpacing(12);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildTopControls();
        buildTabBar();
        buildTabContents();

        selectTab(0);
        takeSnapshot();
    }

    // ============================================================
    // Top Controls
    // ============================================================

    private void buildTopControls() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER_LEFT);

        // 1. PHP language level
        Label levelLabel = new Label("PHP language level:");
        levelLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        languageLevelCombo = new ComboBox<>();
        languageLevelCombo.getItems().addAll(PhpLanguageLevel.getAllDisplayNames());
        languageLevelCombo.setValue(manager.getLanguageLevel().getDisplayName());
        languageLevelCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(languageLevelCombo, Priority.ALWAYS);
        styleComboBox(languageLevelCombo);
        languageLevelCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        // 2. CLI Interpreter
        Label interpreterLabel = new Label("CLI Interpreter:");
        interpreterLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        interpreterCombo = new ComboBox<>();
        refreshInterpretersList();
        interpreterCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(interpreterCombo, Priority.ALWAYS);
        styleComboBox(interpreterCombo);
        interpreterCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        configureInterpreterButton = new Button("...");
        styleIconButton(configureInterpreterButton);
        configureInterpreterButton.setTooltip(new Tooltip("Configure PHP CLI Interpreters"));
        configureInterpreterButton.setOnAction(e -> showConfigureInterpretersDialog());

        HBox interpreterBox = new HBox(6, interpreterCombo, configureInterpreterButton);
        interpreterBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(interpreterCombo, Priority.ALWAYS);

        grid.add(levelLabel, 0, 0);
        grid.add(languageLevelCombo, 1, 0);

        grid.add(interpreterLabel, 0, 1);
        grid.add(interpreterBox, 1, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(140);
        col1.setPrefWidth(150);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        getChildren().add(grid);
    }

    private void refreshInterpretersList() {
        List<PhpInterpreter> list = manager.getInterpreters();
        interpreterCombo.getItems().setAll(list);
        PhpInterpreter active = manager.getActiveInterpreter();
        if (active != null) {
            for (PhpInterpreter item : list) {
                if (item.getId().equals(active.getId())) {
                    interpreterCombo.setValue(item);
                    return;
                }
            }
        }
        if (!list.isEmpty()) {
            interpreterCombo.setValue(list.get(0));
        }
    }

    // ============================================================
    // Tab Bar
    // ============================================================

    private void buildTabBar() {
        HBox tabBar = new HBox(2);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setStyle("-fx-border-color: #393B40; -fx-border-width: 0 0 1 0; -fx-padding: 6 0 6 0;");

        tabIncludePathBtn = createTabButton("Include Path", 0);
        tabRuntimeBtn = createTabButton("PHP Runtime", 1);
        tabAnalysisBtn = createTabButton("Analysis", 2);
        tabComposerBtn = createTabButton("Composer Files", 3);

        tabBar.getChildren().addAll(tabIncludePathBtn, tabRuntimeBtn, tabAnalysisBtn, tabComposerBtn);
        getChildren().add(tabBar);
    }

    private Button createTabButton(String text, int index) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9DA0A8; -fx-font-size: 13px; -fx-padding: 4 12 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
        btn.setOnAction(e -> selectTab(index));
        return btn;
    }

    private void selectTab(int index) {
        Button[] buttons = {tabIncludePathBtn, tabRuntimeBtn, tabAnalysisBtn, tabComposerBtn};
        for (int i = 0; i < buttons.length; i++) {
            if (i == index) {
                buttons[i].setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-padding: 4 12 4 12; -fx-background-radius: 4; -fx-font-weight: bold; -fx-cursor: hand;");
            } else {
                buttons[i].setStyle("-fx-background-color: transparent; -fx-text-fill: #9DA0A8; -fx-font-size: 13px; -fx-padding: 4 12 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
            }
        }

        tabContentContainer.getChildren().clear();
        switch (index) {
            case 0 -> tabContentContainer.getChildren().add(includePathPane);
            case 1 -> tabContentContainer.getChildren().add(runtimePane);
            case 2 -> tabContentContainer.getChildren().add(analysisPane);
            case 3 -> tabContentContainer.getChildren().add(composerPane);
        }
    }

    // ============================================================
    // Tab Contents
    // ============================================================

    private void buildTabContents() {
        tabContentContainer = new StackPane();
        VBox.setVgrow(tabContentContainer, Priority.ALWAYS);

        buildIncludePathTab();
        buildRuntimeTab();
        buildAnalysisTab();
        buildComposerTab();

        getChildren().add(tabContentContainer);
    }

    // ------------------------------------------------------------
    // Tab 1: Include Path
    // ------------------------------------------------------------

    private void buildIncludePathTab() {
        includePathPane = new VBox(6);
        VBox.setVgrow(includePathPane, Priority.ALWAYS);
        includePathPane.setPadding(new Insets(8, 0, 0, 0));

        // Toolbar
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("+");
        styleToolbarButton(addBtn);
        addBtn.setTooltip(new Tooltip("Add Include Path"));
        addBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select PHP Include Path");
            File dir = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (dir != null) {
                String path = dir.getAbsolutePath();
                if (!currentIncludePaths.contains(path)) {
                    currentIncludePaths.add(path);
                    updateIncludePathList();
                    fireModified();
                }
            }
        });

        Button removeBtn = new Button("—");
        styleToolbarButton(removeBtn);
        removeBtn.setTooltip(new Tooltip("Remove Include Path"));
        removeBtn.setOnAction(e -> {
            String selected = includePathListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                currentIncludePaths.remove(selected);
                updateIncludePathList();
                fireModified();
            }
        });

        Button addFolderBtn = new Button();
        SVGPath folderIcon = new SVGPath();
        folderIcon.setContent("M 2 3 L 6 3 L 7 5 L 14 5 L 14 12 L 2 12 Z");
        folderIcon.setFill(Color.web("#848BA3"));
        folderIcon.setScaleX(0.85);
        folderIcon.setScaleY(0.85);
        addFolderBtn.setGraphic(folderIcon);
        styleToolbarButton(addFolderBtn);
        addFolderBtn.setTooltip(new Tooltip("Configure Library or Source Root"));
        addFolderBtn.setOnAction(e -> addBtn.fire());

        toolbar.getChildren().addAll(addBtn, removeBtn, addFolderBtn);

        // List & Placeholder
        currentIncludePaths = new ArrayList<>(manager.getIncludePaths());
        includePathListView = new ListView<>();
        includePathListView.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(includePathListView, Priority.ALWAYS);

        includePathEmptyLabel = new Label("Nothing to show");
        includePathEmptyLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");

        StackPane listContainer = new StackPane(includePathListView, includePathEmptyLabel);
        VBox.setVgrow(listContainer, Priority.ALWAYS);

        updateIncludePathList();

        includePathPane.getChildren().addAll(toolbar, listContainer);
    }

    private void updateIncludePathList() {
        includePathListView.getItems().setAll(currentIncludePaths);
        includePathEmptyLabel.setVisible(currentIncludePaths.isEmpty());
    }

    // ------------------------------------------------------------
    // Tab 2: PHP Runtime
    // ------------------------------------------------------------

    public static class RuntimeTreeNode {
        private final String name;
        private final boolean isCategory;
        private final boolean isRoot;

        public RuntimeTreeNode(String name, boolean isCategory, boolean isRoot) {
            this.name = name;
            this.isCategory = isCategory;
            this.isRoot = isRoot;
        }

        public String getName() {
            return name;
        }

        public boolean isCategory() {
            return isCategory;
        }

        public boolean isRoot() {
            return isRoot;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private void buildRuntimeTab() {
        runtimePane = new VBox(10);
        VBox.setVgrow(runtimePane, Priority.ALWAYS);
        runtimePane.setPadding(new Insets(8, 0, 0, 0));

        // Load extension states
        for (PhpRuntimeExtension ext : manager.getRuntimeExtensions()) {
            currentExtensionStates.put(ext.getName(), ext.isEnabled());
        }

        // TreeView with CheckBoxes
        TreeItem<RuntimeTreeNode> rootItem = new TreeItem<>(new RuntimeTreeNode("PHP Runtime", false, true));
        rootItem.setExpanded(true);

        Map<String, TreeItem<RuntimeTreeNode>> categoryNodes = new LinkedHashMap<>();
        for (String cat : List.of("Core", "Bundled", "External", "PECL", "Others")) {
            TreeItem<RuntimeTreeNode> catItem = new TreeItem<>(new RuntimeTreeNode(cat, true, false));
            catItem.setExpanded(false);
            categoryNodes.put(cat, catItem);
            rootItem.getChildren().add(catItem);
        }

        for (PhpRuntimeExtension ext : manager.getRuntimeExtensions()) {
            TreeItem<RuntimeTreeNode> catItem = categoryNodes.get(ext.getCategory());
            if (catItem == null) {
                catItem = categoryNodes.get("Others");
            }
            TreeItem<RuntimeTreeNode> extItem = new TreeItem<>(new RuntimeTreeNode(ext.getName(), false, false));
            catItem.getChildren().add(extItem);
        }

        runtimeTreeView = new TreeView<>(rootItem);
        runtimeTreeView.setShowRoot(true);
        runtimeTreeView.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(runtimeTreeView, Priority.ALWAYS);

        runtimeTreeView.setCellFactory(tv -> new TreeCell<>() {
            private final CheckBox checkBox = new CheckBox();
            private final Label label = new Label();
            private final HBox cellBox = new HBox(6, checkBox, label);

            {
                cellBox.setAlignment(Pos.CENTER_LEFT);
                checkBox.setStyle("-fx-cursor: hand;");
                checkBox.setOnAction(e -> {
                    TreeItem<RuntimeTreeNode> item = getTreeItem();
                    if (item == null) return;
                    RuntimeTreeNode node = item.getValue();
                    boolean checked = checkBox.isSelected();

                    if (node.isRoot()) {
                        for (TreeItem<RuntimeTreeNode> cat : item.getChildren()) {
                            for (TreeItem<RuntimeTreeNode> child : cat.getChildren()) {
                                currentExtensionStates.put(child.getValue().getName(), checked);
                            }
                        }
                    } else if (node.isCategory()) {
                        for (TreeItem<RuntimeTreeNode> child : item.getChildren()) {
                            currentExtensionStates.put(child.getValue().getName(), checked);
                        }
                    } else {
                        currentExtensionStates.put(node.getName(), checked);
                    }
                    tv.refresh();
                    fireModified();
                });
            }

            @Override
            protected void updateItem(RuntimeTreeNode node, boolean empty) {
                super.updateItem(node, empty);
                if (empty || node == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    label.setText(node.getName());
                    label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

                    if (node.isRoot()) {
                        int total = 0;
                        int enabled = 0;
                        for (Map.Entry<String, Boolean> entry : currentExtensionStates.entrySet()) {
                            total++;
                            if (Boolean.TRUE.equals(entry.getValue())) enabled++;
                        }
                        checkBox.setAllowIndeterminate(true);
                        if (enabled == 0) {
                            checkBox.setIndeterminate(false);
                            checkBox.setSelected(false);
                        } else if (enabled == total) {
                            checkBox.setIndeterminate(false);
                            checkBox.setSelected(true);
                        } else {
                            checkBox.setIndeterminate(true);
                        }
                    } else if (node.isCategory()) {
                        TreeItem<RuntimeTreeNode> treeItem = getTreeItem();
                        int catTotal = treeItem.getChildren().size();
                        int catEnabled = 0;
                        for (TreeItem<RuntimeTreeNode> ch : treeItem.getChildren()) {
                            if (Boolean.TRUE.equals(currentExtensionStates.get(ch.getValue().getName()))) {
                                catEnabled++;
                            }
                        }
                        checkBox.setAllowIndeterminate(true);
                        if (catEnabled == 0) {
                            checkBox.setIndeterminate(false);
                            checkBox.setSelected(false);
                        } else if (catEnabled == catTotal) {
                            checkBox.setIndeterminate(false);
                            checkBox.setSelected(true);
                        } else {
                            checkBox.setIndeterminate(true);
                        }
                    } else {
                        checkBox.setAllowIndeterminate(false);
                        checkBox.setSelected(Boolean.TRUE.equals(currentExtensionStates.get(node.getName())));
                    }

                    setGraphic(cellBox);
                    setText(null);
                }
            }
        });

        // Sync Extensions button
        syncExtensionsBtn = new Button("Sync Extensions with Interpreter");
        syncExtensionsBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 5 12 5 12; -fx-cursor: hand;");
        syncStatusLabel = new Label("");
        syncStatusLabel.setStyle("-fx-text-fill: #59A869; -fx-font-size: 12px;");

        syncExtensionsBtn.setOnAction(e -> {
            PhpInterpreter selectedInterp = interpreterCombo.getValue();
            int synced = manager.syncExtensionsWithInterpreter(selectedInterp);
            for (PhpRuntimeExtension ext : manager.getRuntimeExtensions()) {
                currentExtensionStates.put(ext.getName(), ext.isEnabled());
            }
            runtimeTreeView.refresh();
            syncStatusLabel.setText("✓ Extensions synchronized with " + (selectedInterp != null ? selectedInterp.getDisplayLabel() : "interpreter"));
            fireModified();
        });

        HBox syncBox = new HBox(12, syncExtensionsBtn, syncStatusLabel);
        syncBox.setAlignment(Pos.CENTER_LEFT);

        // Advanced settings collapsible
        VBox advancedContainer = new VBox(6);
        advancedToggleLabel = new Label("› Advanced settings");
        advancedToggleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        advancedStubsBox = new VBox(8);
        advancedStubsBox.setVisible(false);
        advancedStubsBox.setManaged(false);
        advancedStubsBox.setPadding(new Insets(6, 0, 6, 12));

        Label stubsLabel = new Label("Custom PHP runtime stubs path:");
        stubsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        customStubsField = new TextField(manager.getCustomStubsPath());
        styleTextField(customStubsField);
        HBox.setHgrow(customStubsField, Priority.ALWAYS);
        customStubsField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        Button browseStubsBtn = new Button("...");
        styleIconButton(browseStubsBtn);
        browseStubsBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select PHP Runtime Stubs Directory");
            File dir = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (dir != null) {
                customStubsField.setText(dir.getAbsolutePath());
            }
        });

        HBox stubsFieldBox = new HBox(6, customStubsField, browseStubsBtn);
        stubsFieldBox.setAlignment(Pos.CENTER_LEFT);
        advancedStubsBox.getChildren().addAll(stubsLabel, stubsFieldBox);

        advancedToggleLabel.setOnMouseClicked(e -> {
            advancedExpanded = !advancedExpanded;
            advancedToggleLabel.setText(advancedExpanded ? "⌄ Advanced settings" : "› Advanced settings");
            advancedStubsBox.setVisible(advancedExpanded);
            advancedStubsBox.setManaged(advancedExpanded);
        });

        advancedContainer.getChildren().addAll(advancedToggleLabel, advancedStubsBox);

        runtimePane.getChildren().addAll(runtimeTreeView, syncBox, advancedContainer);
    }

    // ------------------------------------------------------------
    // Tab 3: Analysis
    // ------------------------------------------------------------

    private void buildAnalysisTab() {
        analysisPane = new VBox(14);
        VBox.setVgrow(analysisPane, Priority.ALWAYS);
        analysisPane.setPadding(new Insets(8, 0, 0, 0));

        PhpAnalysisSettings initial = manager.getAnalysisSettings();

        // 1. Exception Analysis (collapsible, open by default)
        VBox exceptionSection = new VBox(8);
        Label exceptionHeader = new Label("⌄ Exception Analysis");
        exceptionHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        VBox exceptionContent = new VBox(10);
        exceptionContent.setPadding(new Insets(4, 0, 4, 12));

        // Depth and constant params
        HBox depthRow = new HBox(12);
        depthRow.setAlignment(Pos.CENTER_LEFT);

        Label depthLabel = new Label("Call tree analysis depth:");
        depthLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        callTreeDepthCombo = new ComboBox<>();
        callTreeDepthCombo.getItems().addAll("1", "2", "3", "4", "5", "Unlimited");
        callTreeDepthCombo.setValue(initial.getCallTreeAnalysisDepth());
        styleComboBox(callTreeDepthCombo);
        callTreeDepthCombo.setPrefWidth(120);
        callTreeDepthCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        Region depthSpacer = new Region();
        HBox.setHgrow(depthSpacer, Priority.ALWAYS);

        skipCallsWithConstantParamsCheck = new CheckBox("Skip calls with constant params");
        skipCallsWithConstantParamsCheck.setSelected(initial.isSkipCallsWithConstantParams());
        skipCallsWithConstantParamsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        skipCallsWithConstantParamsCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        depthRow.getChildren().addAll(depthLabel, callTreeDepthCombo, depthSpacer, skipCallsWithConstantParamsCheck);

        // Unchecked Exceptions list
        Label uncheckedLabel = new Label("Unchecked Exceptions:");
        uncheckedLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox exceptionsToolbar = new HBox(4);
        exceptionsToolbar.setAlignment(Pos.CENTER_LEFT);

        Button addExcBtn = new Button("+");
        styleToolbarButton(addExcBtn);
        addExcBtn.setTooltip(new Tooltip("Add Unchecked Exception"));
        addExcBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("\\CustomException");
            dialog.setTitle("Add Unchecked Exception");
            dialog.setHeaderText("Specify fully qualified PHP exception class:");
            dialog.setContentText("Class:");
            dialog.showAndWait().ifPresent(cls -> {
                String trimmed = cls.trim();
                if (!trimmed.isBlank() && !currentUncheckedExceptions.contains(trimmed)) {
                    currentUncheckedExceptions.add(trimmed);
                    uncheckedExceptionsListView.getItems().setAll(currentUncheckedExceptions);
                    fireModified();
                }
            });
        });

        Button removeExcBtn = new Button("—");
        styleToolbarButton(removeExcBtn);
        removeExcBtn.setTooltip(new Tooltip("Remove Selected Exception"));
        removeExcBtn.setOnAction(e -> {
            String selected = uncheckedExceptionsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                currentUncheckedExceptions.remove(selected);
                uncheckedExceptionsListView.getItems().setAll(currentUncheckedExceptions);
                fireModified();
            }
        });

        exceptionsToolbar.getChildren().addAll(addExcBtn, removeExcBtn);

        currentUncheckedExceptions = new ArrayList<>(initial.getUncheckedExceptions());
        uncheckedExceptionsListView = new ListView<>();
        uncheckedExceptionsListView.getItems().setAll(currentUncheckedExceptions);
        uncheckedExceptionsListView.setPrefHeight(130);
        uncheckedExceptionsListView.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        exceptionContent.getChildren().addAll(depthRow, uncheckedLabel, exceptionsToolbar, uncheckedExceptionsListView);
        exceptionSection.getChildren().addAll(exceptionHeader, exceptionContent);

        // 2. Custom Format Functions (collapsible, collapsed by default)
        VBox customFormatSection = new VBox(6);
        customFormatToggleLabel = new Label("› Custom Format Functions");
        customFormatToggleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        customFormatBox = new VBox(8);
        customFormatBox.setVisible(false);
        customFormatBox.setManaged(false);
        customFormatBox.setPadding(new Insets(4, 0, 4, 12));

        Label customFormatDesc = new Label("Configure user-defined functions that accept printf-style formatting.");
        customFormatDesc.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");
        customFormatBox.getChildren().add(customFormatDesc);

        customFormatToggleLabel.setOnMouseClicked(e -> {
            customFormatExpanded = !customFormatExpanded;
            customFormatToggleLabel.setText(customFormatExpanded ? "⌄ Custom Format Functions" : "› Custom Format Functions");
            customFormatBox.setVisible(customFormatExpanded);
            customFormatBox.setManaged(customFormatExpanded);
        });

        customFormatSection.getChildren().addAll(customFormatToggleLabel, customFormatBox);

        // 3. Include Analysis
        VBox includeAnalysisSection = new VBox(8);
        Label includeAnalysisHeader = new Label("Include Analysis");
        includeAnalysisHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox docRootRow = new HBox(8);
        docRootRow.setAlignment(Pos.CENTER_LEFT);

        Label docRootLabel = new Label("$_SERVER['DOCUMENT_ROOT']");
        docRootLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        docRootLabel.setPrefWidth(220);

        documentRootField = new TextField(initial.getDocumentRoot());
        styleTextField(documentRootField);
        HBox.setHgrow(documentRootField, Priority.ALWAYS);
        documentRootField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        Button browseDocRootBtn = new Button();
        SVGPath folderSvg = new SVGPath();
        folderSvg.setContent("M 2 3 L 6 3 L 7 5 L 14 5 L 14 12 L 2 12 Z");
        folderSvg.setFill(Color.web("#848BA3"));
        folderSvg.setScaleX(0.85);
        folderSvg.setScaleY(0.85);
        browseDocRootBtn.setGraphic(folderSvg);
        styleToolbarButton(browseDocRootBtn);
        browseDocRootBtn.setTooltip(new Tooltip("Select Document Root directory"));
        browseDocRootBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Document Root Directory");
            File dir = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (dir != null) {
                documentRootField.setText(dir.getAbsolutePath());
            }
        });

        docRootRow.getChildren().addAll(docRootLabel, documentRootField, browseDocRootBtn);
        includeAnalysisSection.getChildren().addAll(includeAnalysisHeader, docRootRow);

        analysisPane.getChildren().addAll(exceptionSection, customFormatSection, includeAnalysisSection);
    }

    // ------------------------------------------------------------
    // Tab 4: Composer Files
    // ------------------------------------------------------------

    private void buildComposerTab() {
        composerPane = new VBox(6);
        VBox.setVgrow(composerPane, Priority.ALWAYS);
        composerPane.setPadding(new Insets(8, 0, 0, 0));

        // Toolbar
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("+");
        styleToolbarButton(addBtn);
        addBtn.setTooltip(new Tooltip("Add Composer File"));
        addBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select composer.json");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Composer JSON", "*.json"));
            File file = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                PhpComposerFileConfig config = new PhpComposerFileConfig(UUID.randomUUID().toString(), file.getAbsolutePath());
                currentComposerFiles.add(config);
                updateComposerList();
                fireModified();
            }
        });

        Button removeBtn = new Button("—");
        styleToolbarButton(removeBtn);
        removeBtn.setTooltip(new Tooltip("Remove Selected Composer File"));
        removeBtn.setOnAction(e -> {
            PhpComposerFileConfig selected = composerListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                currentComposerFiles.remove(selected);
                updateComposerList();
                fireModified();
            }
        });

        Button editBtn = new Button("✏");
        styleToolbarButton(editBtn);
        editBtn.setTooltip(new Tooltip("Edit Composer Configuration"));
        editBtn.setOnAction(e -> {
            PhpComposerFileConfig selected = composerListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                TextInputDialog tid = new TextInputDialog(selected.getPath());
                tid.setTitle("Edit Composer File");
                tid.setHeaderText("Path to composer.json:");
                tid.setContentText("Path:");
                tid.showAndWait().ifPresent(p -> {
                    selected.setPath(p.trim());
                    updateComposerList();
                    fireModified();
                });
            }
        });

        toolbar.getChildren().addAll(addBtn, removeBtn, editBtn);

        // List & Placeholder
        currentComposerFiles = new ArrayList<>();
        for (PhpComposerFileConfig f : manager.getComposerFiles()) {
            currentComposerFiles.add(f.copy());
        }

        composerListView = new ListView<>();
        composerListView.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(composerListView, Priority.ALWAYS);

        composerEmptyLabel = new Label("Nothing to show");
        composerEmptyLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");

        StackPane listContainer = new StackPane(composerListView, composerEmptyLabel);
        VBox.setVgrow(listContainer, Priority.ALWAYS);

        updateComposerList();

        composerPane.getChildren().addAll(toolbar, listContainer);
    }

    private void updateComposerList() {
        composerListView.getItems().setAll(currentComposerFiles);
        composerEmptyLabel.setVisible(currentComposerFiles.isEmpty());
    }

    // ============================================================
    // Configure Interpreters Dialog
    // ============================================================

    private void showConfigureInterpretersDialog() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (getScene() != null && getScene().getWindow() != null) {
            stage.initOwner(getScene().getWindow());
        }
        stage.setTitle("PHP CLI Interpreters");

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #1E1F22;");

        Label header = new Label("Configured PHP Interpreters:");
        header.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        ListView<PhpInterpreter> list = new ListView<>();
        list.getItems().setAll(manager.getInterpreters());
        list.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        list.setPrefHeight(150);

        HBox toolbar = new HBox(4);
        Button addBtn = new Button("+");
        styleToolbarButton(addBtn);
        addBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select PHP Binary");
            File file = fc.showOpenDialog(stage);
            if (file != null) {
                String path = file.getAbsolutePath();
                String name = path + " (PHP)";
                PhpInterpreter newInterp = new PhpInterpreter(UUID.randomUUID().toString(), name, path, "Detected");
                manager.addOrUpdateInterpreter(newInterp);
                list.getItems().setAll(manager.getInterpreters());
                refreshInterpretersList();
                fireModified();
            }
        });

        Button removeBtn = new Button("—");
        styleToolbarButton(removeBtn);
        removeBtn.setOnAction(e -> {
            PhpInterpreter selected = list.getSelectionModel().getSelectedItem();
            if (selected != null && list.getItems().size() > 1) {
                List<PhpInterpreter> current = manager.getInterpreters();
                current.removeIf(i -> i.getId().equals(selected.getId()));
                manager.setInterpreters(current);
                list.getItems().setAll(manager.getInterpreters());
                refreshInterpretersList();
                fireModified();
            }
        });

        toolbar.getChildren().addAll(addBtn, removeBtn);

        HBox buttonBar = new HBox(8);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-padding: 5 16 5 16; -fx-background-radius: 4; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> stage.close());
        buttonBar.getChildren().add(closeBtn);

        root.getChildren().addAll(header, toolbar, list, buttonBar);

        Scene scene = new Scene(root, 460, 320);
        stage.setScene(scene);
        stage.showAndWait();
    }

    // ============================================================
    // Styling Helpers
    // ============================================================

    private void styleComboBox(ComboBox<?> combo) {
        combo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
    }

    private void styleTextField(TextField field) {
        field.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 5 8 5 8;");
    }

    private void styleToolbarButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 12px; -fx-padding: 2 8 2 8; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-border-color: #589DF6; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 12px; -fx-padding: 2 8 2 8; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 12px; -fx-padding: 2 8 2 8; -fx-cursor: hand;"));
    }

    private void styleIconButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-border-color: #589DF6; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));
    }

    // ============================================================
    // Dirty Checking & State Lifecycle
    // ============================================================

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void takeSnapshot() {
        this.initialLevel = PhpLanguageLevel.fromDisplayName(languageLevelCombo.getValue());
        PhpInterpreter interp = interpreterCombo.getValue();
        this.initialInterpreterId = interp != null ? interp.getId() : "";
        this.initialIncludePaths = new ArrayList<>(currentIncludePaths);
        this.initialExtensionStates = new HashMap<>(currentExtensionStates);
        this.initialCallTreeDepth = callTreeDepthCombo.getValue();
        this.initialSkipConstantParams = skipCallsWithConstantParamsCheck.isSelected();
        this.initialUncheckedExceptions = new ArrayList<>(currentUncheckedExceptions);
        this.initialDocumentRoot = documentRootField.getText();
        this.initialCustomStubs = customStubsField.getText();
        this.initialComposerFiles = new ArrayList<>();
        for (PhpComposerFileConfig f : currentComposerFiles) {
            initialComposerFiles.add(f.copy());
        }
    }

    public boolean isModified() {
        PhpLanguageLevel currentLevel = PhpLanguageLevel.fromDisplayName(languageLevelCombo.getValue());
        if (currentLevel != initialLevel) return true;

        PhpInterpreter interp = interpreterCombo.getValue();
        String currentInterpId = interp != null ? interp.getId() : "";
        if (!Objects.equals(currentInterpId, initialInterpreterId)) return true;

        if (!Objects.equals(currentIncludePaths, initialIncludePaths)) return true;
        if (!Objects.equals(currentExtensionStates, initialExtensionStates)) return true;
        if (!Objects.equals(callTreeDepthCombo.getValue(), initialCallTreeDepth)) return true;
        if (skipCallsWithConstantParamsCheck.isSelected() != initialSkipConstantParams) return true;
        if (!Objects.equals(currentUncheckedExceptions, initialUncheckedExceptions)) return true;
        if (!Objects.equals(documentRootField.getText(), initialDocumentRoot)) return true;
        if (!Objects.equals(customStubsField.getText(), initialCustomStubs)) return true;
        if (!Objects.equals(currentComposerFiles, initialComposerFiles)) return true;

        return false;
    }

    public void apply() {
        PhpLanguageLevel lvl = PhpLanguageLevel.fromDisplayName(languageLevelCombo.getValue());
        manager.setLanguageLevel(lvl);

        PhpInterpreter interp = interpreterCombo.getValue();
        if (interp != null) {
            manager.setActiveInterpreterId(interp.getId());
        }

        manager.setIncludePaths(currentIncludePaths);

        for (Map.Entry<String, Boolean> entry : currentExtensionStates.entrySet()) {
            manager.setExtensionEnabled(entry.getKey(), Boolean.TRUE.equals(entry.getValue()));
        }

        PhpAnalysisSettings analysis = new PhpAnalysisSettings(
                callTreeDepthCombo.getValue(),
                skipCallsWithConstantParamsCheck.isSelected(),
                currentUncheckedExceptions,
                documentRootField.getText()
        );
        manager.setAnalysisSettings(analysis);

        manager.setCustomStubsPath(customStubsField.getText());
        manager.setComposerFiles(currentComposerFiles);
        manager.saveSettings();

        takeSnapshot();
        fireModified();
    }

    public void reset() {
        languageLevelCombo.setValue(initialLevel != null ? initialLevel.getDisplayName() : manager.getLanguageLevel().getDisplayName());

        if (initialInterpreterId != null) {
            for (PhpInterpreter interp : interpreterCombo.getItems()) {
                if (interp.getId().equals(initialInterpreterId)) {
                    interpreterCombo.setValue(interp);
                    break;
                }
            }
        }

        currentIncludePaths = new ArrayList<>(initialIncludePaths != null ? initialIncludePaths : List.of());
        updateIncludePathList();

        currentExtensionStates.clear();
        if (initialExtensionStates != null) {
            currentExtensionStates.putAll(initialExtensionStates);
        }
        if (runtimeTreeView != null) {
            runtimeTreeView.refresh();
        }

        callTreeDepthCombo.setValue(initialCallTreeDepth != null ? initialCallTreeDepth : "1");
        skipCallsWithConstantParamsCheck.setSelected(initialSkipConstantParams);

        currentUncheckedExceptions = new ArrayList<>(initialUncheckedExceptions != null ? initialUncheckedExceptions : List.of());
        uncheckedExceptionsListView.getItems().setAll(currentUncheckedExceptions);

        documentRootField.setText(initialDocumentRoot != null ? initialDocumentRoot : "$_SERVER['DOCUMENT_ROOT']");
        customStubsField.setText(initialCustomStubs != null ? initialCustomStubs : "");

        currentComposerFiles = new ArrayList<>();
        if (initialComposerFiles != null) {
            for (PhpComposerFileConfig f : initialComposerFiles) {
                currentComposerFiles.add(f.copy());
            }
        }
        updateComposerList();

        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    // ============================================================
    // Accessors for Verification & Testing
    // ============================================================

    public ComboBox<String> getLanguageLevelCombo() {
        return languageLevelCombo;
    }

    public ComboBox<PhpInterpreter> getInterpreterCombo() {
        return interpreterCombo;
    }

    public List<String> getCurrentIncludePaths() {
        return currentIncludePaths;
    }

    public Map<String, Boolean> getCurrentExtensionStates() {
        return currentExtensionStates;
    }

    public Button getSyncExtensionsBtn() {
        return syncExtensionsBtn;
    }

    public ComboBox<String> getCallTreeDepthCombo() {
        return callTreeDepthCombo;
    }

    public CheckBox getSkipCallsWithConstantParamsCheck() {
        return skipCallsWithConstantParamsCheck;
    }

    public List<String> getCurrentUncheckedExceptions() {
        return currentUncheckedExceptions;
    }

    public TextField getDocumentRootField() {
        return documentRootField;
    }

    public List<PhpComposerFileConfig> getCurrentComposerFiles() {
        return currentComposerFiles;
    }

    public void selectTabForTest(int index) {
        selectTab(index);
    }
}
