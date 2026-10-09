package dev.lumina.ui;

import dev.lumina.php.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

/**
 * Settings page for Languages & Frameworks > PHP in Lumina IDE.
 * Dynamically managed and configured without hardcoding.
 * Faithfully matches reference IDE design across all 4 tabs:
 * 1. Include Path (Image 1)
 * 2. PHP Runtime (Image 2)
 * 3. Analysis (Image 3)
 * 4. Composer Files (Image 4)
 * Along with dynamic language level dropdown (Image 5) and Select Path dialog (Image 4).
 */
public class SettingsLanguagesPHPPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();
    private Runnable onModifiedListener;

    // Top Controls
    private ComboBox<String> languageLevelCombo;
    private Button helpLevelBtn;
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
    private boolean advancedExpanded = true;
    private final Map<String, Boolean> currentExtensionStates = new HashMap<>();

    // Tab 3: Analysis
    private VBox analysisPane;
    private CheckBox exceptionAnalysisCheck;
    private ComboBox<String> callTreeDepthCombo;
    private CheckBox skipCallsWithConstantParamsCheck;
    private ListView<String> uncheckedExceptionsListView;
    private List<String> currentUncheckedExceptions = new ArrayList<>();
    private boolean customFormatExpanded = true;
    private VBox customFormatBox;
    private Label customFormatToggleLabel;
    private TextField documentRootField;

    // Tab 4: Composer Files
    private VBox composerPane;
    private ListView<PhpComposerFileConfig> composerListView;
    private Label composerEmptyLabel;
    private List<PhpComposerFileConfig> currentComposerFiles = new ArrayList<>();

    // Snapshots for dirty tracking
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
        setPadding(new Insets(14, 20, 20, 20));
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

        // Custom Cell Factory for Language Level ComboBox (matching Image 5)
        languageLevelCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    int pIdx = item.indexOf('(');
                    String ver = pIdx > 0 ? item.substring(0, pIdx).trim() : item;
                    String feat = pIdx > 0 ? item.substring(pIdx) : "";

                    Label verLbl = new Label(ver);
                    verLbl.setStyle("-fx-text-fill: " + (isSelected() ? "#FFFFFF" : "#DFE1E5") + "; -fx-font-size: 13px;");

                    Label featLbl = new Label(" " + feat);
                    featLbl.setStyle("-fx-text-fill: " + (isSelected() ? "#C0C8D8" : "#8C9099") + "; -fx-font-size: 13px;");

                    HBox row = new HBox(verLbl, featLbl);
                    row.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(row);
                    setText(null);

                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-padding: 3 8;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-padding: 3 8;");
                    }
                }
            }
        });

        languageLevelCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        helpLevelBtn = new Button("?");
        helpLevelBtn.setTooltip(new Tooltip("PHP Language Level Documentation"));
        helpLevelBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #6F737A; -fx-border-radius: 10; " +
                "-fx-background-radius: 10; -fx-text-fill: #8C9099; -fx-font-size: 11px; -fx-cursor: hand; " +
                "-fx-min-width: 20px; -fx-min-height: 20px; -fx-max-width: 20px; -fx-max-height: 20px; -fx-padding: 0;");

        HBox levelBox = new HBox(8, languageLevelCombo, helpLevelBtn);
        levelBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(languageLevelCombo, Priority.ALWAYS);

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
        grid.add(levelBox, 1, 0);

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
        HBox tabBar = new HBox(4);
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
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9DA0A8; -fx-font-size: 13px; -fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
        btn.setOnAction(e -> selectTab(index));
        return btn;
    }

    public void selectTab(int index) {
        Button[] buttons = {tabIncludePathBtn, tabRuntimeBtn, tabAnalysisBtn, tabComposerBtn};
        for (int i = 0; i < buttons.length; i++) {
            if (i == index) {
                buttons[i].setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold; -fx-cursor: hand;");
            } else {
                buttons[i].setStyle("-fx-background-color: transparent; -fx-text-fill: #9DA0A8; -fx-font-size: 13px; -fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
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
    // Tab 1: Include Path (Image 1)
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
            SelectPathDialog spd = new SelectPathDialog(getScene() != null ? getScene().getWindow() : null, System.getProperty("user.dir", "."));
            String selectedPath = spd.showAndWait();
            if (selectedPath != null && !selectedPath.isBlank()) {
                if (!currentIncludePaths.contains(selectedPath)) {
                    currentIncludePaths.add(selectedPath);
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
        includePathListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(includePathListView, Priority.ALWAYS);

        includePathListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText("📁 " + item);
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-padding: 3 6; -fx-font-size: 13px;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-padding: 3 6; -fx-font-size: 13px;");
                    }
                }
            }
        });

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
    // Tab 2: PHP Runtime (Image 2)
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

        // TreeView with CheckBoxes matching Image 2
        TreeItem<RuntimeTreeNode> rootItem = new TreeItem<>(new RuntimeTreeNode("PHP Runtime", false, true));
        rootItem.setExpanded(true);

        Map<String, TreeItem<RuntimeTreeNode>> categoryNodes = new LinkedHashMap<>();
        for (String cat : List.of("Core", "Bundled", "External", "PECL", "Others")) {
            TreeItem<RuntimeTreeNode> catItem = new TreeItem<>(new RuntimeTreeNode(cat, true, false));
            catItem.setExpanded("Core".equals(cat)); // Core expanded by default in Image 2
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
        runtimeTreeView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
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
                    setStyle("-fx-background-color: transparent;");
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

                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-padding: 2 4;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-padding: 2 4;");
                    }

                    setGraphic(cellBox);
                    setText(null);
                }
            }
        });

        // Sync Extensions button
        syncExtensionsBtn = new Button("Sync Extensions with Interpreter");
        syncExtensionsBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 5 12; -fx-cursor: hand;");
        syncStatusLabel = new Label("");
        syncStatusLabel.setStyle("-fx-text-fill: #59A869; -fx-font-size: 12px;");

        syncExtensionsBtn.setOnAction(e -> {
            PhpInterpreter selectedInterp = interpreterCombo.getValue();
            int synced = manager.syncExtensionsWithInterpreter(selectedInterp);
            for (PhpRuntimeExtension ext : manager.getRuntimeExtensions()) {
                currentExtensionStates.put(ext.getName(), ext.isEnabled());
            }
            runtimeTreeView.refresh();
            syncStatusLabel.setText("✓ " + synced + " extensions synchronized with " +
                    (selectedInterp != null ? selectedInterp.getDisplayLabel() : "interpreter"));
            fireModified();
        });

        HBox syncBox = new HBox(12, syncExtensionsBtn, syncStatusLabel);
        syncBox.setAlignment(Pos.CENTER_LEFT);

        // Advanced settings collapsible (expanded by default in Image 2)
        VBox advancedContainer = new VBox(6);
        advancedToggleLabel = new Label("⌄ Advanced settings");
        advancedToggleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-font-weight: bold;");

        advancedStubsBox = new VBox(8);
        advancedStubsBox.setVisible(true);
        advancedStubsBox.setManaged(true);
        advancedStubsBox.setPadding(new Insets(6, 0, 6, 12));

        Label stubsLabel = new Label("Default stubs path:");
        stubsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        customStubsField = new TextField(manager.getCustomStubsPath());
        styleTextField(customStubsField);
        HBox.setHgrow(customStubsField, Priority.ALWAYS);
        customStubsField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        Button browseStubsBtn = new Button("📁");
        styleIconButton(browseStubsBtn);
        browseStubsBtn.setTooltip(new Tooltip("Select PHP Runtime Stubs Path"));
        browseStubsBtn.setOnAction(e -> {
            SelectPathDialog spd = new SelectPathDialog(getScene() != null ? getScene().getWindow() : null, customStubsField.getText());
            String path = spd.showAndWait();
            if (path != null && !path.isBlank()) {
                customStubsField.setText(path);
            }
        });

        HBox stubsFieldBox = new HBox(6, customStubsField, browseStubsBtn);
        stubsFieldBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(customStubsField, Priority.ALWAYS);

        HBox stubsRow = new HBox(12, stubsLabel, stubsFieldBox);
        stubsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(stubsFieldBox, Priority.ALWAYS);

        advancedStubsBox.getChildren().add(stubsRow);

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
    // Tab 3: Analysis (Image 3)
    // ------------------------------------------------------------

    private void buildAnalysisTab() {
        analysisPane = new VBox(14);
        VBox.setVgrow(analysisPane, Priority.ALWAYS);
        analysisPane.setPadding(new Insets(8, 0, 0, 0));

        PhpAnalysisSettings initial = manager.getAnalysisSettings();

        // 1. Exception Analysis Section with CheckBox in header (matching Image 3)
        VBox exceptionSection = new VBox(8);

        exceptionAnalysisCheck = new CheckBox("Exception Analysis");
        exceptionAnalysisCheck.setSelected(true);
        exceptionAnalysisCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; " +
                "-fx-border-color: #3574F0; -fx-border-radius: 3; -fx-padding: 3 6;");

        VBox exceptionContent = new VBox(10);
        exceptionContent.setPadding(new Insets(4, 0, 4, 12));

        // Depth and constant params row
        HBox depthRow = new HBox(12);
        depthRow.setAlignment(Pos.CENTER_LEFT);

        Label depthLabel = new Label("Call tree analysis depth:");
        depthLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        callTreeDepthCombo = new ComboBox<>();
        callTreeDepthCombo.getItems().addAll("1", "2", "3", "4", "5", "Unlimited");
        callTreeDepthCombo.setValue(initial.getCallTreeAnalysisDepth());
        styleComboBox(callTreeDepthCombo);
        callTreeDepthCombo.setPrefWidth(90);
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
            if (getClass().getResource("/css/lumina-dark.css") != null) {
                dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
            }
            dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A;");
            dialog.getEditor().setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4;");
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
        uncheckedExceptionsListView.setPrefHeight(120);
        uncheckedExceptionsListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        uncheckedExceptionsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-padding: 3 6; -fx-font-size: 13px;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-padding: 3 6; -fx-font-size: 13px;");
                    }
                }
            }
        });

        exceptionContent.getChildren().addAll(depthRow, uncheckedLabel, exceptionsToolbar, uncheckedExceptionsListView);
        exceptionSection.getChildren().addAll(exceptionAnalysisCheck, exceptionContent);

        exceptionAnalysisCheck.selectedProperty().addListener((obs, oldV, enabled) -> {
            exceptionContent.setDisable(!enabled);
            fireModified();
        });

        // 2. Custom Format Functions (collapsible, matching Image 3)
        VBox customFormatSection = new VBox(6);
        customFormatToggleLabel = new Label("⌄ Custom Format Functions");
        customFormatToggleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-font-weight: bold;");

        customFormatBox = new VBox(6);
        customFormatBox.setVisible(true);
        customFormatBox.setManaged(true);
        customFormatBox.setPadding(new Insets(4, 0, 4, 12));

        HBox customFormatToolbar = new HBox(4);
        Button addFormatBtn = new Button("+");
        styleToolbarButton(addFormatBtn);
        Button removeFormatBtn = new Button("—");
        styleToolbarButton(removeFormatBtn);
        Button editFormatBtn = new Button("✏");
        styleToolbarButton(editFormatBtn);
        customFormatToolbar.getChildren().addAll(addFormatBtn, removeFormatBtn, editFormatBtn);

        StackPane formatArea = new StackPane();
        formatArea.setPrefHeight(90);
        formatArea.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        Label formatEmpty = new Label("Nothing to show");
        formatEmpty.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");
        formatArea.getChildren().add(formatEmpty);

        customFormatBox.getChildren().addAll(customFormatToolbar, formatArea);

        customFormatToggleLabel.setOnMouseClicked(e -> {
            customFormatExpanded = !customFormatExpanded;
            customFormatToggleLabel.setText(customFormatExpanded ? "⌄ Custom Format Functions" : "› Custom Format Functions");
            customFormatBox.setVisible(customFormatExpanded);
            customFormatBox.setManaged(customFormatExpanded);
        });

        customFormatSection.getChildren().addAll(customFormatToggleLabel, customFormatBox);

        // 3. Include Analysis with horizontal rule line (matching Image 3)
        VBox includeAnalysisSection = new VBox(8);

        HBox incHeaderBox = new HBox(8);
        incHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label includeAnalysisHeader = new Label("Include Analysis");
        includeAnalysisHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        Separator incSep = new Separator();
        HBox.setHgrow(incSep, Priority.ALWAYS);
        incSep.setStyle("-fx-background-color: #393B40;");
        incHeaderBox.getChildren().addAll(includeAnalysisHeader, incSep);

        HBox docRootRow = new HBox(8);
        docRootRow.setAlignment(Pos.CENTER_LEFT);

        Label docRootLabel = new Label("$_SERVER['DOCUMENT_ROOT']");
        docRootLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        docRootLabel.setPrefWidth(200);

        documentRootField = new TextField(initial.getDocumentRoot());
        styleTextField(documentRootField);
        HBox.setHgrow(documentRootField, Priority.ALWAYS);
        documentRootField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        Button browseDocRootBtn = new Button("📁");
        styleIconButton(browseDocRootBtn);
        browseDocRootBtn.setTooltip(new Tooltip("Select Document Root directory"));
        browseDocRootBtn.setOnAction(e -> {
            SelectPathDialog spd = new SelectPathDialog(getScene() != null ? getScene().getWindow() : null, documentRootField.getText());
            String path = spd.showAndWait();
            if (path != null && !path.isBlank()) {
                documentRootField.setText(path);
            }
        });

        docRootRow.getChildren().addAll(docRootLabel, documentRootField, browseDocRootBtn);
        includeAnalysisSection.getChildren().addAll(incHeaderBox, docRootRow);

        analysisPane.getChildren().addAll(exceptionSection, customFormatSection, includeAnalysisSection);
    }

    // ------------------------------------------------------------
    // Tab 4: Composer Files (Image 4)
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
            SelectPathDialog spd = new SelectPathDialog(getScene() != null ? getScene().getWindow() : null, System.getProperty("user.dir", "."));
            String selectedPath = spd.showAndWait();
            if (selectedPath != null && !selectedPath.isBlank()) {
                PhpComposerFileConfig config = new PhpComposerFileConfig(UUID.randomUUID().toString(), selectedPath);
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
                SelectPathDialog spd = new SelectPathDialog(getScene() != null ? getScene().getWindow() : null, selected.getPath());
                String path = spd.showAndWait();
                if (path != null && !path.isBlank()) {
                    selected.setPath(path);
                    updateComposerList();
                    fireModified();
                }
            }
        });

        toolbar.getChildren().addAll(addBtn, removeBtn, editBtn);

        // List & Placeholder
        currentComposerFiles = new ArrayList<>();
        for (PhpComposerFileConfig f : manager.getComposerFiles()) {
            currentComposerFiles.add(f.copy());
        }

        composerListView = new ListView<>();
        composerListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(composerListView, Priority.ALWAYS);

        composerListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PhpComposerFileConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText("📄 " + item.getPath());
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-padding: 3 6; -fx-font-size: 13px;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-padding: 3 6; -fx-font-size: 13px;");
                    }
                }
            }
        });

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

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: #1E1F22;");

        // Top Header
        Label header = new Label("PHP CLI Interpreters");
        header.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");
        root.setTop(header);

        // Center Split: Left list, Right details
        HBox centerBox = new HBox(14);
        VBox.setVgrow(centerBox, Priority.ALWAYS);

        // Left: List and toolbar
        VBox leftBox = new VBox(6);
        leftBox.setPrefWidth(220);

        HBox toolbar = new HBox(4);
        Button addBtn = new Button("+");
        styleToolbarButton(addBtn);
        Button removeBtn = new Button("—");
        styleToolbarButton(removeBtn);
        Button refreshBtn = new Button("🔄");
        styleToolbarButton(refreshBtn);
        toolbar.getChildren().addAll(addBtn, removeBtn, refreshBtn);

        ListView<PhpInterpreter> list = new ListView<>();
        list.getItems().setAll(manager.getInterpreters());
        list.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(list, Priority.ALWAYS);

        leftBox.getChildren().addAll(toolbar, list);

        // Right: Inspector details
        VBox rightBox = new VBox(10);
        rightBox.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 12;");
        HBox.setHgrow(rightBox, Priority.ALWAYS);

        Label detailsTitle = new Label("Interpreter Details");
        detailsTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(10);
        detailsGrid.setVgap(8);

        Label pathLbl = new Label("PHP executable:");
        pathLbl.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");
        Label pathVal = new Label();
        pathVal.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-family: monospace;");

        Label verLbl = new Label("PHP version:");
        verLbl.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");
        Label verVal = new Label();
        verVal.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        Label debugLbl = new Label("Debugger:");
        debugLbl.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");
        Label debugVal = new Label();
        debugVal.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        Label iniLbl = new Label("Configuration file:");
        iniLbl.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");
        Label iniVal = new Label();
        iniVal.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-family: monospace;");

        detailsGrid.add(pathLbl, 0, 0);
        detailsGrid.add(pathVal, 1, 0);
        detailsGrid.add(verLbl, 0, 1);
        detailsGrid.add(verVal, 1, 1);
        detailsGrid.add(debugLbl, 0, 2);
        detailsGrid.add(debugVal, 1, 2);
        detailsGrid.add(iniLbl, 0, 3);
        detailsGrid.add(iniVal, 1, 3);

        rightBox.getChildren().addAll(detailsTitle, detailsGrid);

        Runnable updateSelection = () -> {
            PhpInterpreter sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) {
                pathVal.setText(sel.getPath());
                verVal.setText(sel.getPhpVersion());
                debugVal.setText(sel.getDebugger());
                iniVal.setText(sel.getPhpIniPath());
            } else {
                pathVal.setText("-");
                verVal.setText("-");
                debugVal.setText("-");
                iniVal.setText("-");
            }
        };

        list.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updateSelection.run());
        if (!list.getItems().isEmpty()) {
            list.getSelectionModel().select(0);
        }

        // Toolbar actions
        addBtn.setOnAction(e -> {
            SelectPathDialog spd = new SelectPathDialog(stage, "/usr/bin");
            String path = spd.showAndWait();
            if (path != null && !path.isBlank()) {
                String ver = manager.probePhpCliVersion(path);
                String ini = manager.probePhpIniPath(path);
                String debug = manager.probePhpDebugger(path);
                String name = path + " (" + ver + ")";
                PhpInterpreter newInterp = new PhpInterpreter(UUID.randomUUID().toString(), name, path, ver, debug, ini);
                manager.addOrUpdateInterpreter(newInterp);
                list.getItems().setAll(manager.getInterpreters());
                list.getSelectionModel().select(newInterp);
                refreshInterpretersList();
                fireModified();
            }
        });

        removeBtn.setOnAction(e -> {
            PhpInterpreter selected = list.getSelectionModel().getSelectedItem();
            if (selected != null && list.getItems().size() > 1) {
                List<PhpInterpreter> current = manager.getInterpreters();
                current.removeIf(i -> i.getId().equals(selected.getId()));
                manager.setInterpreters(current);
                list.getItems().setAll(manager.getInterpreters());
                if (!list.getItems().isEmpty()) {
                    list.getSelectionModel().select(0);
                }
                refreshInterpretersList();
                fireModified();
            }
        });

        refreshBtn.setOnAction(e -> {
            String sysBin = PhpSettingsManager.findSystemPhpBinary();
            if (sysBin != null) {
                String ver = manager.probePhpCliVersion(sysBin);
                String ini = manager.probePhpIniPath(sysBin);
                String debug = manager.probePhpDebugger(sysBin);
                String name = sysBin + " (" + ver + ")";
                PhpInterpreter detected = new PhpInterpreter("system-php", name, sysBin, ver, debug, ini);
                manager.addOrUpdateInterpreter(detected);
                list.getItems().setAll(manager.getInterpreters());
                list.getSelectionModel().select(detected);
                refreshInterpretersList();
                fireModified();
            }
        });

        centerBox.getChildren().addAll(leftBox, rightBox);
        root.setCenter(centerBox);

        // Bottom Bar
        HBox bottomBar = new HBox(8);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(12, 0, 0, 0));

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-padding: 5 16; -fx-background-radius: 4; -fx-cursor: hand;");
        okBtn.setOnAction(e -> {
            PhpInterpreter sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) {
                interpreterCombo.setValue(sel);
            }
            stage.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 5 14; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        bottomBar.getChildren().addAll(okBtn, cancelBtn);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 580, 390);
        if (getClass().getResource("/css/lumina-dark.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        }
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
        field.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 5 8;");
    }

    private void styleToolbarButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 12px; -fx-padding: 2 8; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-border-color: #589DF6; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 12px; -fx-padding: 2 8; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 12px; -fx-padding: 2 8; -fx-cursor: hand;"));
    }

    private void styleIconButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 4 10; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-border-color: #589DF6; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 4 10; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 4 10; -fx-cursor: hand;"));
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
