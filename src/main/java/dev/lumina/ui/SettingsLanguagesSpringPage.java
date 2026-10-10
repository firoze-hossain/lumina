package dev.lumina.ui;

import dev.lumina.spring.SpringSettings;
import dev.lumina.spring.SpringSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Settings page for Languages & Frameworks > Spring.
 * Matches reference screenshot media_1791600365242_13139971.png:
 *  - Configuration Files Editor
 *    - [x] Show Profiles panel
 *    - [x] Show Multiple Contexts panel
 *  - Completion
 *    - [x] Smart beans completion
 *  - Spring Initializr
 *    - [x] Reformat code when creating a new project
 *  - Autowiring Code Style
 *    - Default bean injector strategy: (•) Constructor  ( ) Field  ( ) Setter
 *    - [x] Use method parameter injection for @Bean methods
 *  - Spring Boot
 *    - [x] Refresh health in the Actuator tab every [ 15 ] sec.
 *  - Project Import
 *    - [x] Create run configuration automatically
 */
public class SettingsLanguagesSpringPage extends VBox {

    private final SpringSettingsManager manager = SpringSettingsManager.getInstance();

    // Configuration Files Editor
    private final CheckBox showProfilesCheck = new CheckBox("Show Profiles panel");
    private final CheckBox showContextsCheck = new CheckBox("Show Multiple Contexts panel");

    // Completion
    private final CheckBox smartBeansCheck = new CheckBox("Smart beans completion");

    // Spring Initializr
    private final CheckBox reformatCodeCheck = new CheckBox("Reformat code when creating a new project");

    // Autowiring Code Style
    private final ToggleGroup injectorStrategyGroup = new ToggleGroup();
    private final RadioButton constructorRadio = new RadioButton("Constructor");
    private final RadioButton fieldRadio = new RadioButton("Field");
    private final RadioButton setterRadio = new RadioButton("Setter");
    private final CheckBox methodParamInjectionCheck = new CheckBox("Use method parameter injection for @Bean methods");

    // Spring Boot
    private final CheckBox refreshHealthActuatorCheck = new CheckBox("Refresh health in the Actuator tab every");
    private final Spinner<Integer> actuatorRefreshIntervalSpinner = new Spinner<>(1, 3600, 15);
    private final Label secLabel = new Label("sec.");

    // Project Import
    private final CheckBox createRunConfigCheck = new CheckBox("Create run configuration automatically");

    private SpringSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsLanguagesSpringPage() {
        setSpacing(16);
        setPadding(new Insets(16, 24, 24, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- 1. Configuration Files Editor ---
        Label configEditorHeader = createSectionHeaderLabel("Configuration Files Editor");
        styleCheckBox(showProfilesCheck);
        styleCheckBox(showContextsCheck);
        showProfilesCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());
        showContextsCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        VBox configEditorBox = new VBox(8, configEditorHeader, showProfilesCheck, showContextsCheck);

        // --- 2. Completion ---
        Label completionHeader = createSectionHeaderLabel("Completion");
        styleCheckBox(smartBeansCheck);
        smartBeansCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        VBox completionBox = new VBox(8, completionHeader, smartBeansCheck);

        // --- 3. Spring Initializr ---
        Label initializrHeader = createSectionHeaderLabel("Spring Initializr");
        styleCheckBox(reformatCodeCheck);
        reformatCodeCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        VBox initializrBox = new VBox(8, initializrHeader, reformatCodeCheck);

        // --- 4. Autowiring Code Style ---
        VBox autowiringBox = new VBox(8);
        autowiringBox.getChildren().add(createDividerLine());

        Label autowiringHeader = createSectionHeaderLabel("Autowiring Code Style");
        autowiringBox.getChildren().add(autowiringHeader);

        Label strategyLabel = new Label("Default bean injector strategy:");
        strategyLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        constructorRadio.setToggleGroup(injectorStrategyGroup);
        fieldRadio.setToggleGroup(injectorStrategyGroup);
        setterRadio.setToggleGroup(injectorStrategyGroup);
        styleRadioButton(constructorRadio);
        styleRadioButton(fieldRadio);
        styleRadioButton(setterRadio);

        injectorStrategyGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> fireModified());

        HBox strategyRow = new HBox(14, strategyLabel, constructorRadio, fieldRadio, setterRadio);
        strategyRow.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(methodParamInjectionCheck);
        methodParamInjectionCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        autowiringBox.getChildren().addAll(strategyRow, methodParamInjectionCheck);

        // --- 5. Spring Boot ---
        VBox springBootBox = new VBox(8);
        springBootBox.getChildren().add(createDividerLine());

        Label springBootHeader = createSectionHeaderLabel("Spring Boot");
        springBootBox.getChildren().add(springBootHeader);

        styleCheckBox(refreshHealthActuatorCheck);
        refreshHealthActuatorCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            actuatorRefreshIntervalSpinner.setDisable(!newV);
            fireModified();
        });

        actuatorRefreshIntervalSpinner.setPrefWidth(65);
        actuatorRefreshIntervalSpinner.setEditable(true);
        actuatorRefreshIntervalSpinner.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-font-size: 13px; -fx-text-fill: #DFE1E5;");
        actuatorRefreshIntervalSpinner.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        secLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox actuatorRow = new HBox(8, refreshHealthActuatorCheck, actuatorRefreshIntervalSpinner, secLabel);
        actuatorRow.setAlignment(Pos.CENTER_LEFT);

        springBootBox.getChildren().add(actuatorRow);

        // --- 6. Project Import ---
        Label projectImportHeader = createSectionHeaderLabel("Project Import");
        styleCheckBox(createRunConfigCheck);
        createRunConfigCheck.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        VBox projectImportBox = new VBox(8, projectImportHeader, createRunConfigCheck);

        getChildren().addAll(
                configEditorBox,
                completionBox,
                initializrBox,
                autowiringBox,
                springBootBox,
                projectImportBox
        );
    }

    private Label createSectionHeaderLabel(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: normal;");
        return lbl;
    }

    private Region createDividerLine() {
        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px;");
        VBox.setMargin(line, new Insets(4, 0, 4, 0));
        return line;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        showProfilesCheck.setSelected(initialSettings.isShowProfilesPanel());
        showContextsCheck.setSelected(initialSettings.isShowMultipleContextsPanel());
        smartBeansCheck.setSelected(initialSettings.isSmartBeansCompletion());
        reformatCodeCheck.setSelected(initialSettings.isReformatCodeWhenCreatingProject());

        switch (initialSettings.getDefaultBeanInjectorStrategy()) {
            case CONSTRUCTOR -> constructorRadio.setSelected(true);
            case FIELD -> fieldRadio.setSelected(true);
            case SETTER -> setterRadio.setSelected(true);
        }

        methodParamInjectionCheck.setSelected(initialSettings.isUseMethodParameterInjectionForBeanMethods());
        refreshHealthActuatorCheck.setSelected(initialSettings.isRefreshHealthInActuatorTab());
        actuatorRefreshIntervalSpinner.getValueFactory().setValue(initialSettings.getActuatorRefreshIntervalSeconds());
        actuatorRefreshIntervalSpinner.setDisable(!refreshHealthActuatorCheck.isSelected());

        createRunConfigCheck.setSelected(initialSettings.isCreateRunConfigurationAutomatically());

        updating = false;
    }

    public SpringSettings getCurrentSettings() {
        SpringSettings s = new SpringSettings();
        s.setShowProfilesPanel(showProfilesCheck.isSelected());
        s.setShowMultipleContextsPanel(showContextsCheck.isSelected());
        s.setSmartBeansCompletion(smartBeansCheck.isSelected());
        s.setReformatCodeWhenCreatingProject(reformatCodeCheck.isSelected());

        if (fieldRadio.isSelected()) {
            s.setDefaultBeanInjectorStrategy(SpringSettings.BeanInjectorStrategy.FIELD);
        } else if (setterRadio.isSelected()) {
            s.setDefaultBeanInjectorStrategy(SpringSettings.BeanInjectorStrategy.SETTER);
        } else {
            s.setDefaultBeanInjectorStrategy(SpringSettings.BeanInjectorStrategy.CONSTRUCTOR);
        }

        s.setUseMethodParameterInjectionForBeanMethods(methodParamInjectionCheck.isSelected());
        s.setRefreshHealthInActuatorTab(refreshHealthActuatorCheck.isSelected());
        Integer val = actuatorRefreshIntervalSpinner.getValue();
        s.setActuatorRefreshIntervalSeconds(val != null ? val : 15);
        s.setCreateRunConfigurationAutomatically(createRunConfigCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        SpringSettings current = getCurrentSettings();
        manager.setSettings(current);
        initialSettings = current.clone();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void reset() {
        loadData();
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Getters for UI elements (testing & programmatic interaction)
    public CheckBox getShowProfilesCheck() {
        return showProfilesCheck;
    }

    public CheckBox getShowContextsCheck() {
        return showContextsCheck;
    }

    public CheckBox getSmartBeansCheck() {
        return smartBeansCheck;
    }

    public CheckBox getReformatCodeCheck() {
        return reformatCodeCheck;
    }

    public RadioButton getConstructorRadio() {
        return constructorRadio;
    }

    public RadioButton getFieldRadio() {
        return fieldRadio;
    }

    public RadioButton getSetterRadio() {
        return setterRadio;
    }

    public CheckBox getMethodParamInjectionCheck() {
        return methodParamInjectionCheck;
    }

    public CheckBox getRefreshHealthActuatorCheck() {
        return refreshHealthActuatorCheck;
    }

    public Spinner<Integer> getActuatorRefreshIntervalSpinner() {
        return actuatorRefreshIntervalSpinner;
    }

    public CheckBox getCreateRunConfigCheck() {
        return createRunConfigCheck;
    }
}
