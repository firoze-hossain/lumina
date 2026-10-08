package dev.lumina.ui;

import dev.lumina.build.AnnotationProcessingProfile;
import dev.lumina.build.AnnotationProcessingSettings;
import dev.lumina.build.AnnotationProcessingSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;

/**
 * Settings subpage for Build, Execution, Deployment > Compiler > Annotation Processors.
 * Matches 1:1 with reference screenshot media_1791429993536.png:
 *  - Master-detail split layout
 *  - Left: Tree of profiles (Default, Maven default profile with modules) + toolbar (+, -, ->) + "Create new profile" dialog
 *  - Right:
 *    - Enable annotation processing
 *    - Obtain processors from project classpath vs Processor path
 *    - Use --processor-module-path compiler option (for Java 9 and later)
 *    - Store generated sources relative to: Module output directory vs Module content root
 *    - Production sources directory / Test sources directory
 *    - Run processors in a separate step before compiling java (-proc:only mode)
 *    - Annotation processors list (will run all automatically discovered processors)
 *    - Annotation processor options table (Option Name, Value)
 */
public class SettingsAnnotationProcessorsPage extends VBox {

    private final AnnotationProcessingSettingsManager manager = AnnotationProcessingSettingsManager.getInstance();

    private final TreeView<String> profileTreeView = new TreeView<>();
    private final Button addProfileBtn = new Button("+");
    private final Button removeProfileBtn = new Button("-");
    private final Button moveModuleBtn = new Button("→");

    // Detail controls
    private final CheckBox enableAnnotationProcessingCheck = new CheckBox("Enable annotation processing");
    private final ToggleGroup processorPathGroup = new ToggleGroup();
    private final RadioButton obtainFromClasspathRadio = new RadioButton("Obtain processors from project classpath");
    private final RadioButton processorPathRadio = new RadioButton("Processor path:");
    private final TextField processorPathField = new TextField();
    private final Button browseProcessorPathBtn = new Button("📁");

    private final CheckBox useProcessorModulePathCheck = new CheckBox("Use --processor-module-path compiler option (for Java 9 and later)");
    private final ToggleGroup sourcesRelativeGroup = new ToggleGroup();
    private final RadioButton moduleOutputDirRadio = new RadioButton("Module output directory");
    private final RadioButton moduleContentRootRadio = new RadioButton("Module content root");

    private final TextField productionSourcesField = new TextField("target/generated-sources/annotations");
    private final TextField testSourcesField = new TextField("target/generated-test-sources/test-annotations");
    private final CheckBox runSeparateStepCheck = new CheckBox("Run processors in a separate step before compiling java (-proc:only mode)");

    private final TableView<ProcessorOptionItem> optionsTable = new TableView<>();

    private AnnotationProcessingSettings initialSettings;
    private AnnotationProcessingSettings currentWorkingSettings;
    private AnnotationProcessingProfile selectedProfile;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsAnnotationProcessorsPage() {
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(10);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // --- Left Panel ---
        VBox leftPane = new VBox(6);
        leftPane.setMinWidth(220);
        leftPane.setPrefWidth(260);

        HBox leftToolbar = new HBox(4);
        leftToolbar.setAlignment(Pos.CENTER_LEFT);
        styleToolbarButton(addProfileBtn);
        styleToolbarButton(removeProfileBtn);
        styleToolbarButton(moveModuleBtn);

        addProfileBtn.setOnAction(e -> showCreateProfileDialog());
        removeProfileBtn.setOnAction(e -> removeSelectedProfile());

        leftToolbar.getChildren().addAll(addProfileBtn, removeProfileBtn, moveModuleBtn);

        profileTreeView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4;");
        profileTreeView.setShowRoot(false);
        VBox.setVgrow(profileTreeView, Priority.ALWAYS);

        profileTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String profileName = getRootProfileName(newVal);
                selectProfile(profileName);
            }
        });

        leftPane.getChildren().addAll(leftToolbar, profileTreeView);

        // --- Right Panel ---
        VBox rightPane = new VBox(12);
        rightPane.setPadding(new Insets(0, 0, 0, 16));
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        enableAnnotationProcessingCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Processor Path Group
        obtainFromClasspathRadio.setToggleGroup(processorPathGroup);
        obtainFromClasspathRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        processorPathRadio.setToggleGroup(processorPathGroup);
        processorPathRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        processorPathField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        HBox.setHgrow(processorPathField, Priority.ALWAYS);

        browseProcessorPathBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-padding: 4 8; -fx-cursor: hand;");
        browseProcessorPathBtn.setOnAction(e -> browseProcessorPath());

        HBox procPathRow = new HBox(8, processorPathRadio, processorPathField, browseProcessorPathBtn);
        procPathRow.setAlignment(Pos.CENTER_LEFT);

        // Help icon for module path
        useProcessorModulePathCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Sources Relative To
        Label storeRelativeToLabel = new Label("Store generated sources relative to:");
        storeRelativeToLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        moduleOutputDirRadio.setToggleGroup(sourcesRelativeGroup);
        moduleOutputDirRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        moduleContentRootRadio.setToggleGroup(sourcesRelativeGroup);
        moduleContentRootRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox relativeRow = new HBox(14, storeRelativeToLabel, moduleOutputDirRadio, moduleContentRootRadio);
        relativeRow.setAlignment(Pos.CENTER_LEFT);

        // Sources Directories
        GridPane dirGrid = new GridPane();
        dirGrid.setHgap(12);
        dirGrid.setVgap(8);

        Label prodLabel = new Label("Production sources directory:");
        prodLabel.setMinWidth(190);
        prodLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        productionSourcesField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        GridPane.setHgrow(productionSourcesField, Priority.ALWAYS);

        Label testLabel = new Label("Test sources directory:");
        testLabel.setMinWidth(190);
        testLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        testSourcesField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-font-size: 13px;");
        GridPane.setHgrow(testSourcesField, Priority.ALWAYS);

        dirGrid.add(prodLabel, 0, 0);
        dirGrid.add(productionSourcesField, 1, 0);
        dirGrid.add(testLabel, 0, 1);
        dirGrid.add(testSourcesField, 1, 1);

        runSeparateStepCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Section: Annotation processors
        Label processorsLabel = new Label("Annotation processors:");
        processorsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label autoDiscoveredLabel = new Label("will run all automatically discovered processors");
        autoDiscoveredLabel.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 12px; -fx-padding: 4 0 4 8;");

        // Section: Annotation processor options
        Label optionsHeader = new Label("Annotation processor options:");
        optionsHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");

        TableColumn<ProcessorOptionItem, String> nameCol = new TableColumn<>("Option Name");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(220);

        TableColumn<ProcessorOptionItem, String> valCol = new TableColumn<>("Value");
        valCol.setCellValueFactory(data -> data.getValue().valueProperty());
        valCol.setPrefWidth(220);

        optionsTable.getColumns().setAll(nameCol, valCol);
        optionsTable.setPlaceholder(new Label("No processor-specific options configured"));
        optionsTable.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4;");
        optionsTable.setPrefHeight(120);

        rightPane.getChildren().addAll(
                enableAnnotationProcessingCheck,
                obtainFromClasspathRadio,
                procPathRow,
                useProcessorModulePathCheck,
                relativeRow,
                dirGrid,
                runSeparateStepCheck,
                processorsLabel,
                autoDiscoveredLabel,
                optionsHeader,
                optionsTable
        );

        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.32);

        getChildren().add(splitPane);

        // Listeners
        enableAnnotationProcessingCheck.selectedProperty().addListener((obs, o, n) -> saveCurrentToWorking());
        processorPathGroup.selectedToggleProperty().addListener((obs, o, n) -> saveCurrentToWorking());
        processorPathField.textProperty().addListener((obs, o, n) -> saveCurrentToWorking());
        useProcessorModulePathCheck.selectedProperty().addListener((obs, o, n) -> saveCurrentToWorking());
        sourcesRelativeGroup.selectedToggleProperty().addListener((obs, o, n) -> saveCurrentToWorking());
        productionSourcesField.textProperty().addListener((obs, o, n) -> saveCurrentToWorking());
        testSourcesField.textProperty().addListener((obs, o, n) -> saveCurrentToWorking());
        runSeparateStepCheck.selectedProperty().addListener((obs, o, n) -> saveCurrentToWorking());
    }

    private void styleToolbarButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 8; -fx-cursor: hand; -fx-font-weight: bold;");
    }

    private String getRootProfileName(TreeItem<String> item) {
        TreeItem<String> cur = item;
        while (cur.getParent() != null && cur.getParent().getValue() != null && !cur.getParent().getValue().isEmpty()) {
            cur = cur.getParent();
        }
        return cur.getValue();
    }

    public void showCreateProfileDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Create new profile");
        dialog.setHeaderText(null);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16, 20, 16, 20));

        Label label = new Label("Profile name:");
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField nameField = new TextField();
        nameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4;");
        nameField.setPrefWidth(260);

        content.getChildren().addAll(label, nameField);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22;");

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? nameField.getText() : null);

        dialog.showAndWait().ifPresent(name -> {
            if (name != null && !name.isBlank()) {
                AnnotationProcessingProfile newProfile = new AnnotationProcessingProfile(name.trim());
                currentWorkingSettings.getProfiles().add(newProfile);
                rebuildTree();
                selectProfile(newProfile.getName());
                notifyModified();
            }
        });
    }

    private void removeSelectedProfile() {
        TreeItem<String> sel = profileTreeView.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String profileName = getRootProfileName(sel);
        if ("Default".equals(profileName) || "Maven default annotation processors profile".equals(profileName)) {
            return; // Protected system profiles
        }
        AnnotationProcessingProfile p = currentWorkingSettings.getProfileByName(profileName);
        if (p != null) {
            currentWorkingSettings.getProfiles().remove(p);
            rebuildTree();
            selectProfile("Default");
            notifyModified();
        }
    }

    private void browseProcessorPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Processor Path");
        File dir = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
        if (dir != null) {
            processorPathField.setText(dir.getAbsolutePath());
            processorPathRadio.setSelected(true);
        }
    }

    private void rebuildTree() {
        TreeItem<String> root = new TreeItem<>("");
        for (AnnotationProcessingProfile p : currentWorkingSettings.getProfiles()) {
            TreeItem<String> pItem = new TreeItem<>(p.getName());
            pItem.setExpanded(true);
            for (String mod : p.getModules()) {
                TreeItem<String> modItem = new TreeItem<>("📁 " + mod);
                pItem.getChildren().add(modItem);
            }
            root.getChildren().add(pItem);
        }
        profileTreeView.setRoot(root);
    }

    private void selectProfile(String profileName) {
        if (currentWorkingSettings == null) return;
        selectedProfile = currentWorkingSettings.getProfileByName(profileName);
        if (selectedProfile == null && !currentWorkingSettings.getProfiles().isEmpty()) {
            selectedProfile = currentWorkingSettings.getProfiles().getFirst();
        }
        if (selectedProfile == null) return;

        updating = true;

        enableAnnotationProcessingCheck.setSelected(selectedProfile.isEnabled());
        if (selectedProfile.isObtainFromClasspath()) {
            obtainFromClasspathRadio.setSelected(true);
        } else {
            processorPathRadio.setSelected(true);
        }
        processorPathField.setText(selectedProfile.getProcessorPath());
        useProcessorModulePathCheck.setSelected(selectedProfile.isUseProcessorModulePath());

        if (selectedProfile.isStoreRelativeToContentRoot()) {
            moduleContentRootRadio.setSelected(true);
        } else {
            moduleOutputDirRadio.setSelected(true);
        }
        productionSourcesField.setText(selectedProfile.getProductionSourcesDirectory());
        testSourcesField.setText(selectedProfile.getTestSourcesDirectory());
        runSeparateStepCheck.setSelected(selectedProfile.isRunInSeparateStep());

        updating = false;
    }

    private void saveCurrentToWorking() {
        if (updating || selectedProfile == null) return;

        selectedProfile.setEnabled(enableAnnotationProcessingCheck.isSelected());
        selectedProfile.setObtainFromClasspath(obtainFromClasspathRadio.isSelected());
        selectedProfile.setProcessorPath(processorPathField.getText());
        selectedProfile.setUseProcessorModulePath(useProcessorModulePathCheck.isSelected());
        selectedProfile.setStoreRelativeToContentRoot(moduleContentRootRadio.isSelected());
        selectedProfile.setProductionSourcesDirectory(productionSourcesField.getText());
        selectedProfile.setTestSourcesDirectory(testSourcesField.getText());
        selectedProfile.setRunInSeparateStep(runSeparateStepCheck.isSelected());

        notifyModified();
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();
        currentWorkingSettings = initialSettings.clone();

        rebuildTree();
        selectProfile("Default");

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null || currentWorkingSettings == null) return false;
        return !initialSettings.equals(currentWorkingSettings);
    }

    public void apply() {
        manager.setSettings(currentWorkingSettings);
        initialSettings = currentWorkingSettings.clone();
    }

    public void reset() {
        loadData();
    }

    public AnnotationProcessingSettings getCurrentSettings() {
        return currentWorkingSettings != null ? currentWorkingSettings.clone() : new AnnotationProcessingSettings();
    }

    public TreeView<String> getProfileTreeView() {
        return profileTreeView;
    }

    public CheckBox getEnableAnnotationProcessingCheck() {
        return enableAnnotationProcessingCheck;
    }

    public RadioButton getObtainFromClasspathRadio() {
        return obtainFromClasspathRadio;
    }

    public RadioButton getProcessorPathRadio() {
        return processorPathRadio;
    }

    public TextField getProductionSourcesField() {
        return productionSourcesField;
    }

    public TextField getTestSourcesField() {
        return testSourcesField;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public static class ProcessorOptionItem {
        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleStringProperty value = new SimpleStringProperty();

        public ProcessorOptionItem(String name, String value) {
            this.name.set(name);
            this.value.set(value);
        }

        public SimpleStringProperty nameProperty() { return name; }
        public SimpleStringProperty valueProperty() { return value; }
    }
}
