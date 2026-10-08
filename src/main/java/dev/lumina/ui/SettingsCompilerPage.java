package dev.lumina.ui;

import dev.lumina.build.CompilerSettings;
import dev.lumina.build.CompilerSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Settings page for Build, Execution, Deployment > Compiler.
 * Matches 1:1 with reference screenshot media_1791429963570.png:
 *  - Resource patterns: (with expand and help)
 *  - Clear output directory on rebuild (with warning subtext)
 *  - Add runtime assertions for notnull-annotated methods and parameters (with Configure annotations link)
 *  - Automatically show first error in editor
 *  - Display notification on build completion
 *  - Build project automatically (with subtext)
 *  - Rebuild module on dependency change
 *  - Compile independent modules in parallel: Automatic
 *  - Build Process:
 *    - Shared heap size / Shared VM options
 *    - User-local heap size / User-local VM options
 */
public class SettingsCompilerPage extends VBox {

    private final CompilerSettingsManager manager = CompilerSettingsManager.getInstance();

    private final TextField resourcePatternsField = new TextField();
    private final Button expandResourcePatternsBtn = new Button("⤢");
    private final CheckBox clearOutputDirectoryCheck = new CheckBox("Clear output directory on rebuild");
    private final CheckBox addRuntimeAssertionsCheck = new CheckBox("Add runtime assertions for notnull-annotated methods and parameters");
    private final Hyperlink configureAnnotationsLink = new Hyperlink("Configure annotations...");
    private final CheckBox autoShowFirstErrorCheck = new CheckBox("Automatically show first error in editor");
    private final CheckBox displayNotificationCheck = new CheckBox("Display notification on build completion");
    private final CheckBox buildProjectAutomaticallyCheck = new CheckBox("Build project automatically");
    private final CheckBox rebuildModuleOnDependencyChangeCheck = new CheckBox("Rebuild module on dependency change");
    private final ComboBox<String> parallelCompilationCombo = new ComboBox<>();

    // Build Process
    private final TextField sharedHeapSizeField = new TextField();
    private final TextField sharedVmOptionsField = new TextField();
    private final Button expandSharedVmBtn = new Button("⤢");
    private final TextField userLocalHeapSizeField = new TextField();
    private final TextField userLocalVmOptionsField = new TextField();
    private final Button expandUserLocalVmBtn = new Button("⤢");

    private CompilerSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsCompilerPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(10);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // 1. Resource patterns
        Label resourcePatternsLabel = new Label("Resource patterns:");
        resourcePatternsLabel.setMinWidth(140);
        resourcePatternsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        resourcePatternsField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #707278; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
        HBox.setHgrow(resourcePatternsField, Priority.ALWAYS);

        expandResourcePatternsBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
        expandResourcePatternsBtn.setOnAction(e -> openExpandDialog("Resource patterns", resourcePatternsField));

        Label helpPatternsBtn = new Label("?");
        helpPatternsBtn.setStyle("-fx-text-fill: #8C8C8C; -fx-cursor: hand; -fx-padding: 0 4; -fx-font-size: 13px;");
        Tooltip.install(helpPatternsBtn, new Tooltip("Patterns of resource files to be copied to the output directory during compilation"));

        HBox patternsRow = new HBox(8, resourcePatternsLabel, resourcePatternsField, expandResourcePatternsBtn, helpPatternsBtn);
        patternsRow.setAlignment(Pos.CENTER_LEFT);

        Label patternsSubtext = new Label("Use ; to separate patterns and ! to negate a pattern.");
        patternsSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 4 148;");

        VBox patternsBox = new VBox(2, patternsRow, patternsSubtext);

        // 2. Checkboxes
        clearOutputDirectoryCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        Label clearOutputSubtext = new Label("Warning: If enabled, the entire contents of directories where generated sources are stored will be cleared on rebuild.");
        clearOutputSubtext.setWrapText(true);
        clearOutputSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 2 24;");
        VBox clearOutputBox = new VBox(2, clearOutputDirectoryCheck, clearOutputSubtext);

        addRuntimeAssertionsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        configureAnnotationsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0;");
        configureAnnotationsLink.setOnAction(e -> showAnnotationsDialog());
        HBox assertionsBox = new HBox(10, addRuntimeAssertionsCheck, configureAnnotationsLink);
        assertionsBox.setAlignment(Pos.CENTER_LEFT);

        autoShowFirstErrorCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        displayNotificationCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        buildProjectAutomaticallyCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        Label autoBuildSubtext = new Label("Only works while not running / debugging");
        autoBuildSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px; -fx-padding: 0 0 2 24;");
        VBox autoBuildBox = new VBox(2, buildProjectAutomaticallyCheck, autoBuildSubtext);

        rebuildModuleOnDependencyChangeCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Compile in parallel row
        Label parallelLabel = new Label("Compile independent modules in parallel:");
        parallelLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        parallelCompilationCombo.getItems().addAll("Automatic", "Always", "Never");
        parallelCompilationCombo.setValue("Automatic");
        parallelCompilationCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");

        Label helpParallelBtn = new Label("?");
        helpParallelBtn.setStyle("-fx-text-fill: #8C8C8C; -fx-cursor: hand; -fx-padding: 0 4; -fx-font-size: 13px;");
        Tooltip.install(helpParallelBtn, new Tooltip("Enable parallel compilation of independent modules to speed up build"));

        HBox parallelRow = new HBox(10, parallelLabel, parallelCompilationCombo, helpParallelBtn);
        parallelRow.setAlignment(Pos.CENTER_LEFT);
        parallelRow.setPadding(new Insets(4, 0, 8, 0));

        // 3. Section: Build Process
        Label buildProcessHeader = new Label("Build Process");
        buildProcessHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 0 0 0;");

        GridPane buildGrid = new GridPane();
        buildGrid.setHgap(12);
        buildGrid.setVgap(8);

        // Shared heap size
        Label sharedHeapLabel = new Label("Shared heap size:");
        sharedHeapLabel.setMinWidth(150);
        sharedHeapLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        sharedHeapSizeField.setPrefWidth(90);
        sharedHeapSizeField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");

        Label sharedMbytesLabel = new Label("Mbytes");
        sharedMbytesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox sharedHeapBox = new HBox(8, sharedHeapSizeField, sharedMbytesLabel);
        sharedHeapBox.setAlignment(Pos.CENTER_LEFT);

        // Shared VM options
        Label sharedVmLabel = new Label("Shared VM options:");
        sharedVmLabel.setMinWidth(150);
        sharedVmLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        sharedVmOptionsField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
        HBox.setHgrow(sharedVmOptionsField, Priority.ALWAYS);

        expandSharedVmBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
        expandSharedVmBtn.setOnAction(e -> openExpandDialog("Shared VM options", sharedVmOptionsField));

        HBox sharedVmBox = new HBox(8, sharedVmOptionsField, expandSharedVmBtn);
        sharedVmBox.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(sharedVmBox, Priority.ALWAYS);

        // User-local heap size
        Label userHeapLabel = new Label("User-local heap size:");
        userHeapLabel.setMinWidth(150);
        userHeapLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        userLocalHeapSizeField.setPrefWidth(90);
        userLocalHeapSizeField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");

        Label userMbytesLabel = new Label("Mbytes");
        userMbytesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox userHeapBox = new HBox(8, userLocalHeapSizeField, userMbytesLabel);
        userHeapBox.setAlignment(Pos.CENTER_LEFT);

        Label userHeapSubtext = new Label("Overrides Shared size");
        userHeapSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");

        VBox userHeapVBox = new VBox(2, userHeapBox, userHeapSubtext);

        // User-local VM options
        Label userVmLabel = new Label("User-local VM options:");
        userVmLabel.setMinWidth(150);
        userVmLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        userLocalVmOptionsField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
        HBox.setHgrow(userLocalVmOptionsField, Priority.ALWAYS);

        expandUserLocalVmBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #8C8C8C; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-font-size: 12px;");
        expandUserLocalVmBtn.setOnAction(e -> openExpandDialog("User-local VM options", userLocalVmOptionsField));

        HBox userVmBox = new HBox(8, userLocalVmOptionsField, expandUserLocalVmBtn);
        userVmBox.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(userVmBox, Priority.ALWAYS);

        Label userVmSubtext = new Label("Overrides Shared options");
        userVmSubtext.setStyle("-fx-text-fill: #8C8C8C; -fx-font-size: 11px;");

        VBox userVmVBox = new VBox(2, userVmBox, userVmSubtext);

        buildGrid.add(sharedHeapLabel, 0, 0);
        buildGrid.add(sharedHeapBox, 1, 0);

        buildGrid.add(sharedVmLabel, 0, 1);
        buildGrid.add(sharedVmBox, 1, 1);

        buildGrid.add(userHeapLabel, 0, 2);
        buildGrid.add(userHeapVBox, 1, 2);

        buildGrid.add(userVmLabel, 0, 3);
        buildGrid.add(userVmVBox, 1, 3);

        getChildren().addAll(
                patternsBox,
                clearOutputBox,
                assertionsBox,
                autoShowFirstErrorCheck,
                displayNotificationCheck,
                autoBuildBox,
                rebuildModuleOnDependencyChangeCheck,
                parallelRow,
                buildProcessHeader,
                buildGrid
        );

        // Listeners
        resourcePatternsField.textProperty().addListener((obs, o, n) -> notifyModified());
        clearOutputDirectoryCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        addRuntimeAssertionsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        autoShowFirstErrorCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        displayNotificationCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        buildProjectAutomaticallyCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        rebuildModuleOnDependencyChangeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        parallelCompilationCombo.valueProperty().addListener((obs, o, n) -> notifyModified());

        sharedHeapSizeField.textProperty().addListener((obs, o, n) -> notifyModified());
        sharedVmOptionsField.textProperty().addListener((obs, o, n) -> notifyModified());
        userLocalHeapSizeField.textProperty().addListener((obs, o, n) -> notifyModified());
        userLocalVmOptionsField.textProperty().addListener((obs, o, n) -> notifyModified());
    }

    private void openExpandDialog(String title, TextField targetField) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        TextArea textArea = new TextArea(targetField.getText());
        textArea.setWrapText(true);
        textArea.setPrefSize(440, 200);
        textArea.setStyle("-fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5;");

        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? textArea.getText() : null);

        dialog.showAndWait().ifPresent(targetField::setText);
    }

    private void showAnnotationsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Configure Annotations");
        alert.setHeaderText("NotNull Annotations");
        alert.setContentText("Configured runtime assertion annotations:\n- javax.annotation.Nonnull\n- jakarta.annotation.Nonnull\n- edu.umd.cs.findbugs.annotations.NonNull\n- org.jspecify.annotations.NonNull");
        alert.showAndWait();
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        resourcePatternsField.setText(initialSettings.getResourcePatterns());
        clearOutputDirectoryCheck.setSelected(initialSettings.isClearOutputDirectoryOnRebuild());
        addRuntimeAssertionsCheck.setSelected(initialSettings.isAddRuntimeAssertionsNotNull());
        autoShowFirstErrorCheck.setSelected(initialSettings.isAutoShowFirstErrorInEditor());
        displayNotificationCheck.setSelected(initialSettings.isDisplayNotificationOnBuildCompletion());
        buildProjectAutomaticallyCheck.setSelected(initialSettings.isBuildProjectAutomatically());
        rebuildModuleOnDependencyChangeCheck.setSelected(initialSettings.isRebuildModuleOnDependencyChange());
        parallelCompilationCombo.setValue(initialSettings.getCompileModulesInParallel());

        sharedHeapSizeField.setText(initialSettings.getSharedHeapSizeMb());
        sharedVmOptionsField.setText(initialSettings.getSharedVmOptions());
        userLocalHeapSizeField.setText(initialSettings.getUserLocalHeapSizeMb());
        userLocalVmOptionsField.setText(initialSettings.getUserLocalVmOptions());

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        CompilerSettings cur = getCurrentSettings();
        return !initialSettings.equals(cur);
    }

    public void apply() {
        CompilerSettings cur = getCurrentSettings();
        manager.setSettings(cur);
        initialSettings = cur.clone();
    }

    public void reset() {
        loadData();
    }

    public CompilerSettings getCurrentSettings() {
        CompilerSettings s = new CompilerSettings();
        s.setResourcePatterns(resourcePatternsField.getText());
        s.setClearOutputDirectoryOnRebuild(clearOutputDirectoryCheck.isSelected());
        s.setAddRuntimeAssertionsNotNull(addRuntimeAssertionsCheck.isSelected());
        s.setAutoShowFirstErrorInEditor(autoShowFirstErrorCheck.isSelected());
        s.setDisplayNotificationOnBuildCompletion(displayNotificationCheck.isSelected());
        s.setBuildProjectAutomatically(buildProjectAutomaticallyCheck.isSelected());
        s.setRebuildModuleOnDependencyChange(rebuildModuleOnDependencyChangeCheck.isSelected());
        s.setCompileModulesInParallel(parallelCompilationCombo.getValue());

        s.setSharedHeapSizeMb(sharedHeapSizeField.getText());
        s.setSharedVmOptions(sharedVmOptionsField.getText());
        s.setUserLocalHeapSizeMb(userLocalHeapSizeField.getText());
        s.setUserLocalVmOptions(userLocalVmOptionsField.getText());

        return s;
    }

    public TextField getResourcePatternsField() {
        return resourcePatternsField;
    }

    public CheckBox getClearOutputDirectoryCheck() {
        return clearOutputDirectoryCheck;
    }

    public CheckBox getAddRuntimeAssertionsCheck() {
        return addRuntimeAssertionsCheck;
    }

    public CheckBox getAutoShowFirstErrorCheck() {
        return autoShowFirstErrorCheck;
    }

    public CheckBox getDisplayNotificationCheck() {
        return displayNotificationCheck;
    }

    public CheckBox getBuildProjectAutomaticallyCheck() {
        return buildProjectAutomaticallyCheck;
    }

    public CheckBox getRebuildModuleOnDependencyChangeCheck() {
        return rebuildModuleOnDependencyChangeCheck;
    }

    public ComboBox<String> getParallelCompilationCombo() {
        return parallelCompilationCombo;
    }

    public TextField getSharedHeapSizeField() {
        return sharedHeapSizeField;
    }

    public TextField getSharedVmOptionsField() {
        return sharedVmOptionsField;
    }

    public TextField getUserLocalHeapSizeField() {
        return userLocalHeapSizeField;
    }

    public TextField getUserLocalVmOptionsField() {
        return userLocalVmOptionsField;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
