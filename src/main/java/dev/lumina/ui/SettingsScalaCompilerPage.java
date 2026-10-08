package dev.lumina.ui;

import dev.lumina.build.ScalaCompilerProfile;
import dev.lumina.build.ScalaCompilerSettings;
import dev.lumina.build.ScalaCompilerSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Settings page for Build, Execution, Deployment > Compiler > Scala Compiler.
 * Matches 1:1 with reference screenshot media_1791449910603_f5140118.png:
 *  - Incrementality type: Zinc / Standard
 *  - Master-detail view with profile/module tree and toolbar (+, -, ->)
 *  - Compile order: Mixed / Java then Scala / Scala then Java
 *  - Features (Dynamics, Postfix operator notation, Reflective calls, Implicit conversions,
 *              Higher-kinded types, Existential types, Macros, Experimental Features)
 *  - Options (Enable warnings, Deprecation warnings, Unchecked warnings, Feature warnings,
 *             Optimise bytecode, Explain type errors, Enable specialization, Enable continuations)
 *  - Debugging info level
 *  - Additional compiler options (with expand button)
 *  - Compiler plugins list with toolbar and "Nothing to show" placeholder
 */
public class SettingsScalaCompilerPage extends VBox {

    private final ScalaCompilerSettingsManager manager = ScalaCompilerSettingsManager.getInstance();

    private final ComboBox<String> incrementalityTypeCombo = new ComboBox<>();

    // Master-Detail
    private final TreeView<String> profileTree = new TreeView<>();
    private final Button addProfileBtn = new Button("+");
    private final Button removeProfileBtn = new Button("−");
    private final Button moveModuleBtn = new Button("→");

    // Profile detail controls
    private final ComboBox<String> compileOrderCombo = new ComboBox<>();

    // Features checkboxes
    private final CheckBox dynamicsCheck = new CheckBox("Dynamics");
    private final CheckBox postfixOperatorCheck = new CheckBox("Postfix operator notation");
    private final CheckBox reflectiveCallsCheck = new CheckBox("Reflective calls");
    private final CheckBox implicitConversionsCheck = new CheckBox("Implicit conversions");
    private final CheckBox higherKindedTypesCheck = new CheckBox("Higher-kinded types");
    private final CheckBox existentialTypesCheck = new CheckBox("Existential types");
    private final CheckBox macrosCheck = new CheckBox("Macros");
    private final CheckBox experimentalFeaturesCheck = new CheckBox("Experimental Features");

    // Options checkboxes
    private final CheckBox enableWarningsCheck = new CheckBox("Enable warnings");
    private final CheckBox deprecationWarningsCheck = new CheckBox("Deprecation warnings");
    private final CheckBox uncheckedWarningsCheck = new CheckBox("Unchecked warnings");
    private final CheckBox featureWarningsCheck = new CheckBox("Feature warnings");
    private final CheckBox optimiseBytecodeCheck = new CheckBox("Optimise bytecode (use with care*)");
    private final CheckBox explainTypeErrorsCheck = new CheckBox("Explain type errors");
    private final CheckBox enableSpecializationCheck = new CheckBox("Enable specialization");
    private final CheckBox enableContinuationsCheck = new CheckBox("Enable continuations");

    private final ComboBox<String> debugInfoLevelCombo = new ComboBox<>();
    private final TextField additionalCompilerOptionsField = new TextField();
    private final Button expandOptionsBtn = new Button("⤢");

    // Compiler plugins
    private final ObservableList<String> pluginsList = FXCollections.observableArrayList();
    private final ListView<String> pluginsListView = new ListView<>(pluginsList);
    private final Button addPluginBtn = new Button("+");
    private final Button removePluginBtn = new Button("−");

    private List<ScalaCompilerProfile> workingProfiles = new ArrayList<>();
    private ScalaCompilerProfile selectedProfile;

    private ScalaCompilerSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsScalaCompilerPage() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // 1. Top row: Incrementality type
        Label incLabel = new Label("Incrementality type:");
        incLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        incrementalityTypeCombo.getItems().addAll("Zinc", "Standard");
        incrementalityTypeCombo.setValue("Zinc");
        styleComboBox(incrementalityTypeCombo);
        incrementalityTypeCombo.setOnAction(e -> fireModified());

        HBox topRow = new HBox(12, incLabel, incrementalityTypeCombo);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Master Detail SplitPane
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent; -fx-box-border: transparent;");

        // Left Panel: Toolbar + Profile Tree
        VBox leftPane = new VBox(0);
        leftPane.setMinWidth(220);
        leftPane.setPrefWidth(240);
        leftPane.setMaxWidth(350);

        HBox treeToolbar = createMiniToolbar(addProfileBtn, removeProfileBtn, moveModuleBtn);
        addProfileBtn.setOnAction(e -> showAddProfileDialog());
        removeProfileBtn.setOnAction(e -> removeSelectedTreeItem());
        moveModuleBtn.setOnAction(e -> showMoveModuleDialog());

        profileTree.setShowRoot(false);
        profileTree.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A; -fx-border-radius: 0 0 4 4; -fx-background-radius: 0 0 4 4;");
        profileTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> onTreeSelectionChanged(newVal));
        VBox.setVgrow(profileTree, Priority.ALWAYS);

        leftPane.getChildren().addAll(treeToolbar, profileTree);

        // Right Panel: Configuration Form
        VBox rightPane = new VBox(10);
        rightPane.setPadding(new Insets(0, 0, 0, 16));

        // Compile order
        Label compileOrderLabel = new Label("Compile order:");
        compileOrderLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        compileOrderCombo.getItems().addAll("Mixed", "Java then Scala", "Scala then Java");
        compileOrderCombo.setValue("Mixed");
        styleComboBox(compileOrderCombo);
        compileOrderCombo.setOnAction(e -> {
            if (selectedProfile != null && !updating) {
                selectedProfile.setCompileOrder(compileOrderCombo.getValue());
                fireModified();
            }
        });

        HBox orderRow = new HBox(12, compileOrderLabel, compileOrderCombo);
        orderRow.setAlignment(Pos.CENTER_LEFT);

        // Features section
        Label featuresHeader = createSectionHeader("Features");
        GridPane featuresGrid = new GridPane();
        featuresGrid.setHgap(28);
        featuresGrid.setVgap(6);

        styleCheckBox(dynamicsCheck);
        styleCheckBox(postfixOperatorCheck);
        styleCheckBox(reflectiveCallsCheck);
        styleCheckBox(implicitConversionsCheck);
        styleCheckBox(higherKindedTypesCheck);
        styleCheckBox(existentialTypesCheck);
        styleCheckBox(macrosCheck);
        styleCheckBox(experimentalFeaturesCheck);

        wireProfileCheckBox(dynamicsCheck, (p, b) -> p.setDynamics(b));
        wireProfileCheckBox(postfixOperatorCheck, (p, b) -> p.setPostfixOperatorNotation(b));
        wireProfileCheckBox(reflectiveCallsCheck, (p, b) -> p.setReflectiveCalls(b));
        wireProfileCheckBox(implicitConversionsCheck, (p, b) -> p.setImplicitConversions(b));
        wireProfileCheckBox(higherKindedTypesCheck, (p, b) -> p.setHigherKindedTypes(b));
        wireProfileCheckBox(existentialTypesCheck, (p, b) -> p.setExistentialTypes(b));
        wireProfileCheckBox(macrosCheck, (p, b) -> p.setMacros(b));
        wireProfileCheckBox(experimentalFeaturesCheck, (p, b) -> p.setExperimentalFeatures(b));

        featuresGrid.add(dynamicsCheck, 0, 0);
        featuresGrid.add(postfixOperatorCheck, 0, 1);
        featuresGrid.add(reflectiveCallsCheck, 0, 2);
        featuresGrid.add(implicitConversionsCheck, 0, 3);

        featuresGrid.add(higherKindedTypesCheck, 1, 0);
        featuresGrid.add(existentialTypesCheck, 1, 1);
        featuresGrid.add(macrosCheck, 1, 2);
        featuresGrid.add(experimentalFeaturesCheck, 1, 3);

        // Options section
        Label optionsHeader = createSectionHeader("Options");
        GridPane optionsGrid = new GridPane();
        optionsGrid.setHgap(28);
        optionsGrid.setVgap(6);

        styleCheckBox(enableWarningsCheck);
        styleCheckBox(deprecationWarningsCheck);
        styleCheckBox(uncheckedWarningsCheck);
        styleCheckBox(featureWarningsCheck);
        styleCheckBox(optimiseBytecodeCheck);
        styleCheckBox(explainTypeErrorsCheck);
        styleCheckBox(enableSpecializationCheck);
        styleCheckBox(enableContinuationsCheck);

        wireProfileCheckBox(enableWarningsCheck, (p, b) -> p.setEnableWarnings(b));
        wireProfileCheckBox(deprecationWarningsCheck, (p, b) -> p.setDeprecationWarnings(b));
        wireProfileCheckBox(uncheckedWarningsCheck, (p, b) -> p.setUncheckedWarnings(b));
        wireProfileCheckBox(featureWarningsCheck, (p, b) -> p.setFeatureWarnings(b));
        wireProfileCheckBox(optimiseBytecodeCheck, (p, b) -> p.setOptimiseBytecode(b));
        wireProfileCheckBox(explainTypeErrorsCheck, (p, b) -> p.setExplainTypeErrors(b));
        wireProfileCheckBox(enableSpecializationCheck, (p, b) -> p.setEnableSpecialization(b));
        wireProfileCheckBox(enableContinuationsCheck, (p, b) -> p.setEnableContinuations(b));

        optionsGrid.add(enableWarningsCheck, 0, 0);
        optionsGrid.add(deprecationWarningsCheck, 0, 1);
        optionsGrid.add(uncheckedWarningsCheck, 0, 2);
        optionsGrid.add(featureWarningsCheck, 0, 3);

        optionsGrid.add(optimiseBytecodeCheck, 1, 0);
        optionsGrid.add(explainTypeErrorsCheck, 1, 1);
        optionsGrid.add(enableSpecializationCheck, 1, 2);
        optionsGrid.add(enableContinuationsCheck, 1, 3);

        // Debugging info level
        Label debugLabel = new Label("Debugging info level:");
        debugLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        debugInfoLevelCombo.getItems().addAll(
                "Source, line number and local variable information",
                "Source and line number information",
                "None"
        );
        debugInfoLevelCombo.setValue("Source, line number and local variable information");
        styleComboBox(debugInfoLevelCombo);
        debugInfoLevelCombo.setOnAction(e -> {
            if (selectedProfile != null && !updating) {
                selectedProfile.setDebuggingInfoLevel(debugInfoLevelCombo.getValue());
                fireModified();
            }
        });

        HBox debugRow = new HBox(12, debugLabel, debugInfoLevelCombo);
        debugRow.setAlignment(Pos.CENTER_LEFT);

        // Additional compiler options
        Label addOptsLabel = new Label("Additional compiler options:");
        addOptsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        styleTextField(additionalCompilerOptionsField);
        HBox.setHgrow(additionalCompilerOptionsField, Priority.ALWAYS);
        additionalCompilerOptionsField.textProperty().addListener((obs, o, n) -> {
            if (selectedProfile != null && !updating) {
                selectedProfile.setAdditionalCompilerOptions(n);
                fireModified();
            }
        });

        styleMiniButton(expandOptionsBtn);
        expandOptionsBtn.setOnAction(e -> openExpandDialog("Additional compiler options", additionalCompilerOptionsField));

        HBox optsRow = new HBox(6, additionalCompilerOptionsField, expandOptionsBtn);
        optsRow.setAlignment(Pos.CENTER_LEFT);
        HBox addOptsBox = new HBox(12, addOptsLabel, optsRow);
        addOptsBox.setAlignment(Pos.CENTER_LEFT);

        // Compiler plugins
        Label pluginsHeader = createSectionHeader("Compiler plugins");
        HBox pluginsToolbar = createMiniToolbar(addPluginBtn, removePluginBtn);
        addPluginBtn.setOnAction(e -> showAddPluginDialog());
        removePluginBtn.setOnAction(e -> {
            String sel = pluginsListView.getSelectionModel().getSelectedItem();
            if (sel != null && selectedProfile != null) {
                pluginsList.remove(sel);
                selectedProfile.getCompilerPlugins().remove(sel);
                fireModified();
            }
        });

        pluginsListView.setPrefHeight(100);
        pluginsListView.setMaxHeight(120);
        pluginsListView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A; -fx-border-radius: 0 0 4 4; -fx-background-radius: 0 0 4 4;");
        Label placeholder = new Label("Nothing to show");
        placeholder.setStyle("-fx-text-fill: #707278; -fx-font-size: 12px;");
        pluginsListView.setPlaceholder(placeholder);

        VBox pluginsBox = new VBox(0, pluginsHeader, pluginsToolbar, pluginsListView);

        ScrollPane rightScroll = new ScrollPane();
        rightScroll.setFitToWidth(true);
        rightScroll.setStyle("-fx-background: #1E1F22; -fx-background-color: transparent; -fx-padding: 0;");

        VBox rightContent = new VBox(10,
                orderRow,
                featuresHeader,
                featuresGrid,
                optionsHeader,
                optionsGrid,
                debugRow,
                addOptsBox,
                pluginsBox
        );
        rightScroll.setContent(rightContent);

        splitPane.getItems().addAll(leftPane, rightScroll);
        splitPane.setDividerPositions(0.32);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        getChildren().addAll(topRow, splitPane);
    }

    private void wireProfileCheckBox(CheckBox cb, java.util.function.BiConsumer<ScalaCompilerProfile, Boolean> setter) {
        cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedProfile != null && !updating) {
                setter.accept(selectedProfile, newVal);
                fireModified();
            }
        });
    }

    private void onTreeSelectionChanged(TreeItem<String> item) {
        if (item == null) return;
        String val = item.getValue();
        ScalaCompilerProfile target = null;
        for (ScalaCompilerProfile p : workingProfiles) {
            if (p.getName().equals(val)) {
                target = p;
                break;
            }
            if (p.getModules().contains(val)) {
                target = p;
                break;
            }
        }
        if (target != null) {
            selectedProfile = target;
            updateRightPane();
        }
    }

    private void updateRightPane() {
        if (selectedProfile == null) return;
        updating = true;

        compileOrderCombo.setValue(selectedProfile.getCompileOrder());

        dynamicsCheck.setSelected(selectedProfile.isDynamics());
        postfixOperatorCheck.setSelected(selectedProfile.isPostfixOperatorNotation());
        reflectiveCallsCheck.setSelected(selectedProfile.isReflectiveCalls());
        implicitConversionsCheck.setSelected(selectedProfile.isImplicitConversions());
        higherKindedTypesCheck.setSelected(selectedProfile.isHigherKindedTypes());
        existentialTypesCheck.setSelected(selectedProfile.isExistentialTypes());
        macrosCheck.setSelected(selectedProfile.isMacros());
        experimentalFeaturesCheck.setSelected(selectedProfile.isExperimentalFeatures());

        enableWarningsCheck.setSelected(selectedProfile.isEnableWarnings());
        deprecationWarningsCheck.setSelected(selectedProfile.isDeprecationWarnings());
        uncheckedWarningsCheck.setSelected(selectedProfile.isUncheckedWarnings());
        featureWarningsCheck.setSelected(selectedProfile.isFeatureWarnings());
        optimiseBytecodeCheck.setSelected(selectedProfile.isOptimiseBytecode());
        explainTypeErrorsCheck.setSelected(selectedProfile.isExplainTypeErrors());
        enableSpecializationCheck.setSelected(selectedProfile.isEnableSpecialization());
        enableContinuationsCheck.setSelected(selectedProfile.isEnableContinuations());

        debugInfoLevelCombo.setValue(selectedProfile.getDebuggingInfoLevel());
        additionalCompilerOptionsField.setText(selectedProfile.getAdditionalCompilerOptions());

        pluginsList.setAll(selectedProfile.getCompilerPlugins());

        updating = false;
    }

    private void rebuildTree() {
        TreeItem<String> root = new TreeItem<>("Root");
        for (ScalaCompilerProfile p : workingProfiles) {
            TreeItem<String> pItem = new TreeItem<>(p.getName());
            Label icon = new Label("⚙");
            icon.setStyle("-fx-font-size: 11px;");
            pItem.setGraphic(icon);
            pItem.setExpanded(true);

            for (String mod : p.getModules()) {
                TreeItem<String> mItem = new TreeItem<>(mod);
                Label modIcon = new Label("📁");
                modIcon.setStyle("-fx-font-size: 11px;");
                mItem.setGraphic(modIcon);
                pItem.getChildren().add(mItem);
            }
            root.getChildren().add(pItem);
        }
        profileTree.setRoot(root);
        if (!root.getChildren().isEmpty()) {
            profileTree.getSelectionModel().select(root.getChildren().getFirst());
        }
    }

    private void showAddProfileDialog() {
        TextInputDialog dialog = new TextInputDialog("Profile " + (workingProfiles.size() + 1));
        dialog.setTitle("New Scala Compiler Profile");
        dialog.setHeaderText("Enter profile name:");
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A;");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                ScalaCompilerProfile p = new ScalaCompilerProfile(name.trim());
                workingProfiles.add(p);
                rebuildTree();
                fireModified();
            }
        });
    }

    private void removeSelectedTreeItem() {
        TreeItem<String> sel = profileTree.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String val = sel.getValue();
        if (sel.getParent() == profileTree.getRoot()) {
            // It's a profile
            if (workingProfiles.size() > 1) {
                workingProfiles.removeIf(p -> p.getName().equals(val));
                rebuildTree();
                fireModified();
            }
        } else {
            // It's a module
            if (selectedProfile != null) {
                selectedProfile.getModules().remove(val);
                rebuildTree();
                fireModified();
            }
        }
    }

    private void showMoveModuleDialog() {
        TreeItem<String> sel = profileTree.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getParent() == profileTree.getRoot()) return;
        String modName = sel.getValue();

        ChoiceDialog<String> dialog = new ChoiceDialog<>();
        dialog.setTitle("Move Module");
        dialog.setHeaderText("Move " + modName + " to profile:");
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A;");
        for (ScalaCompilerProfile p : workingProfiles) {
            dialog.getItems().add(p.getName());
        }
        if (!dialog.getItems().isEmpty()) {
            dialog.setSelectedItem(dialog.getItems().getFirst());
        }
        dialog.showAndWait().ifPresent(targetProfileName -> {
            for (ScalaCompilerProfile p : workingProfiles) {
                p.getModules().remove(modName);
                if (p.getName().equals(targetProfileName)) {
                    p.getModules().add(modName);
                }
            }
            rebuildTree();
            fireModified();
        });
    }

    private void showAddPluginDialog() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Add Compiler Plugin");
        dialog.setHeaderText("Enter path to compiler plugin JAR:");
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A;");
        dialog.showAndWait().ifPresent(path -> {
            if (!path.isBlank() && selectedProfile != null) {
                pluginsList.add(path.trim());
                selectedProfile.getCompilerPlugins().add(path.trim());
                fireModified();
            }
        });
    }

    private void openExpandDialog(String title, TextField targetField) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-border-color: #43454A;");

        TextArea area = new TextArea(targetField.getText());
        area.setWrapText(true);
        area.setPrefSize(420, 200);
        area.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-family: monospace; -fx-border-color: #43454A;");

        VBox box = new VBox(8, area);
        box.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(box);

        ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == okType) return area.getText();
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            targetField.setText(res);
            fireModified();
        });
    }

    private Label createSectionHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 0 2 0;");
        return l;
    }

    private void styleTextField(TextField f) {
        f.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
    }

    private void styleComboBox(ComboBox<?> c) {
        c.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleMiniButton(Button b) {
        b.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
    }

    private HBox createMiniToolbar(Button... buttons) {
        HBox bar = new HBox(2);
        for (Button b : buttons) {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 8;");
            bar.getChildren().add(b);
        }
        bar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-width: 1 1 0 1; -fx-padding: 2 4;");
        return bar;
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        incrementalityTypeCombo.setValue(initialSettings.getIncrementalityType());

        workingProfiles = new ArrayList<>();
        for (ScalaCompilerProfile p : initialSettings.getProfiles()) {
            workingProfiles.add(p.clone());
        }

        rebuildTree();
        updating = false;
    }

    private void syncRightPaneToProfile() {
        if (selectedProfile != null && !updating) {
            selectedProfile.setCompileOrder(compileOrderCombo.getValue());
            selectedProfile.setDynamics(dynamicsCheck.isSelected());
            selectedProfile.setPostfixOperatorNotation(postfixOperatorCheck.isSelected());
            selectedProfile.setReflectiveCalls(reflectiveCallsCheck.isSelected());
            selectedProfile.setImplicitConversions(implicitConversionsCheck.isSelected());
            selectedProfile.setHigherKindedTypes(higherKindedTypesCheck.isSelected());
            selectedProfile.setExistentialTypes(existentialTypesCheck.isSelected());
            selectedProfile.setMacros(macrosCheck.isSelected());
            selectedProfile.setExperimentalFeatures(experimentalFeaturesCheck.isSelected());
            selectedProfile.setEnableWarnings(enableWarningsCheck.isSelected());
            selectedProfile.setDeprecationWarnings(deprecationWarningsCheck.isSelected());
            selectedProfile.setUncheckedWarnings(uncheckedWarningsCheck.isSelected());
            selectedProfile.setFeatureWarnings(featureWarningsCheck.isSelected());
            selectedProfile.setOptimiseBytecode(optimiseBytecodeCheck.isSelected());
            selectedProfile.setExplainTypeErrors(explainTypeErrorsCheck.isSelected());
            selectedProfile.setEnableSpecialization(enableSpecializationCheck.isSelected());
            selectedProfile.setEnableContinuations(enableContinuationsCheck.isSelected());
            selectedProfile.setDebuggingInfoLevel(debugInfoLevelCombo.getValue());
            selectedProfile.setAdditionalCompilerOptions(additionalCompilerOptionsField.getText());
        }
    }

    public ScalaCompilerSettings getCurrentSettings() {
        syncRightPaneToProfile();
        ScalaCompilerSettings s = new ScalaCompilerSettings();
        s.setIncrementalityType(incrementalityTypeCombo.getValue());
        List<ScalaCompilerProfile> pList = new ArrayList<>();
        for (ScalaCompilerProfile p : workingProfiles) {
            pList.add(p.clone());
        }
        s.setProfiles(pList);
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        ScalaCompilerSettings current = getCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.clone();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void reset() {
        loadData();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public ComboBox<String> getIncrementalityTypeCombo() { return incrementalityTypeCombo; }
    public TreeView<String> getProfileTree() { return profileTree; }
    public ComboBox<String> getCompileOrderCombo() { return compileOrderCombo; }
    public CheckBox getDynamicsCheck() { return dynamicsCheck; }
    public CheckBox getEnableWarningsCheck() { return enableWarningsCheck; }
    public CheckBox getEnableSpecializationCheck() { return enableSpecializationCheck; }
    public ComboBox<String> getDebugInfoLevelCombo() { return debugInfoLevelCombo; }
    public TextField getAdditionalCompilerOptionsField() { return additionalCompilerOptionsField; }
    public ObservableList<String> getPluginsList() { return pluginsList; }
}
