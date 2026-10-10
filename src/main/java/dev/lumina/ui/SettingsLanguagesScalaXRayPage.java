package dev.lumina.ui;

import dev.lumina.scala.ScalaLanguageSettingsManager;
import dev.lumina.scala.ScalaXRaySettings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Languages & Frameworks > Scala > X-Ray Mode settings page in Lumina IDE.
 * Matches Image 5 with activation triggers, hint categories, and status widget options.
 */
public class SettingsLanguagesScalaXRayPage extends VBox {

    private final ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
    private Runnable onModifiedListener;

    // Activate on
    private CheckBox doublePressAndHoldCtrlCheckBox;
    private CheckBox pressAndHoldCtrlCheckBox;

    // Show
    private CheckBox parameterNameHintsCheckBox;
    private CheckBox forAllParametersCheckBox;
    private CheckBox byNameArgumentHintsCheckBox;
    private CheckBox applyMethodHintsCheckBox;
    private CheckBox typeHintsCheckBox;
    private CheckBox memberVariablesCheckBox;
    private CheckBox localVariablesCheckBox;
    private CheckBox methodResultsCheckBox;
    private CheckBox lambdaParametersCheckBox;
    private CheckBox lambdaPlaceholdersCheckBox;
    private CheckBox variablePatternsCheckBox;
    private CheckBox methodChainHintsCheckBox;
    private CheckBox typeArgumentsCheckBox;
    private CheckBox implicitHintsCheckBox;
    private CheckBox indentGuidesCheckBox;
    private CheckBox methodSeparatorsCheckBox;

    // Widget
    private ComboBox<String> widgetDisplayComboBox;

    private ScalaXRaySettings initialSettings;

    public SettingsLanguagesScalaXRayPage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ScalaXRaySettings current = manager.getXRaySettings();

        // 1. Activate on Section
        Label activateTitle = createSectionTitle("Activate on:");

        doublePressAndHoldCtrlCheckBox = new CheckBox("Double-press and hold Ctrl");
        doublePressAndHoldCtrlCheckBox.setSelected(current.isDoublePressAndHoldCtrl());
        styleCheckBox(doublePressAndHoldCtrlCheckBox);
        doublePressAndHoldCtrlCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        pressAndHoldCtrlCheckBox = new CheckBox("Press and hold Ctrl");
        pressAndHoldCtrlCheckBox.setSelected(current.isPressAndHoldCtrl());
        styleCheckBox(pressAndHoldCtrlCheckBox);
        pressAndHoldCtrlCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        VBox activateBox = new VBox(6, activateTitle, doublePressAndHoldCtrlCheckBox, pressAndHoldCtrlCheckBox);

        // 2. Show Section
        Label showTitle = createSectionTitle("Show:");

        parameterNameHintsCheckBox = new CheckBox("Parameter name hints");
        parameterNameHintsCheckBox.setSelected(current.isParameterNameHints());
        styleCheckBox(parameterNameHintsCheckBox);
        parameterNameHintsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> {
            updateParameterNameSubState();
            fireModified();
        });

        forAllParametersCheckBox = new CheckBox("For all parameters");
        forAllParametersCheckBox.setSelected(current.isForAllParameters());
        styleCheckBox(forAllParametersCheckBox);
        forAllParametersCheckBox.setPadding(new Insets(0, 0, 0, 22));
        forAllParametersCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        updateParameterNameSubState();

        byNameArgumentHintsCheckBox = new CheckBox("By-name argument hints");
        byNameArgumentHintsCheckBox.setSelected(current.isByNameArgumentHints());
        styleCheckBox(byNameArgumentHintsCheckBox);
        byNameArgumentHintsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        applyMethodHintsCheckBox = new CheckBox("Apply method hints");
        applyMethodHintsCheckBox.setSelected(current.isApplyMethodHints());
        styleCheckBox(applyMethodHintsCheckBox);
        applyMethodHintsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        typeHintsCheckBox = new CheckBox("Type hints for:");
        typeHintsCheckBox.setSelected(current.isTypeHints());
        styleCheckBox(typeHintsCheckBox);
        typeHintsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> {
            updateTypeHintsSubState();
            fireModified();
        });

        memberVariablesCheckBox = createSubCheckBox("Member variables", current.isMemberVariables());
        localVariablesCheckBox = createSubCheckBox("Local variables", current.isLocalVariables());
        methodResultsCheckBox = createSubCheckBox("Method results", current.isMethodResults());
        lambdaParametersCheckBox = createSubCheckBox("Lambda parameters", current.isLambdaParameters());
        lambdaPlaceholdersCheckBox = createSubCheckBox("Lambda placeholders", current.isLambdaPlaceholders());
        variablePatternsCheckBox = createSubCheckBox("Variable patterns", current.isVariablePatterns());

        updateTypeHintsSubState();

        methodChainHintsCheckBox = new CheckBox("Method chain hints");
        methodChainHintsCheckBox.setSelected(current.isMethodChainHints());
        styleCheckBox(methodChainHintsCheckBox);
        methodChainHintsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        typeArgumentsCheckBox = new CheckBox("Type arguments");
        typeArgumentsCheckBox.setSelected(current.isTypeArguments());
        styleCheckBox(typeArgumentsCheckBox);
        typeArgumentsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        implicitHintsCheckBox = new CheckBox("Implicit hints");
        implicitHintsCheckBox.setSelected(current.isImplicitHints());
        styleCheckBox(implicitHintsCheckBox);
        implicitHintsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        indentGuidesCheckBox = new CheckBox("Indent guides");
        indentGuidesCheckBox.setSelected(current.isIndentGuides());
        styleCheckBox(indentGuidesCheckBox);
        indentGuidesCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        methodSeparatorsCheckBox = new CheckBox("Method separators");
        methodSeparatorsCheckBox.setSelected(current.isMethodSeparators());
        styleCheckBox(methodSeparatorsCheckBox);
        methodSeparatorsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        VBox showBox = new VBox(6,
                showTitle,
                parameterNameHintsCheckBox,
                forAllParametersCheckBox,
                byNameArgumentHintsCheckBox,
                applyMethodHintsCheckBox,
                typeHintsCheckBox,
                memberVariablesCheckBox,
                localVariablesCheckBox,
                methodResultsCheckBox,
                lambdaParametersCheckBox,
                lambdaPlaceholdersCheckBox,
                variablePatternsCheckBox,
                methodChainHintsCheckBox,
                typeArgumentsCheckBox,
                implicitHintsCheckBox,
                indentGuidesCheckBox,
                methodSeparatorsCheckBox
        );

        // 3. Widget Section
        Label widgetTitle = createSectionTitle("Widget:");

        HBox displayRow = new HBox(12);
        displayRow.setAlignment(Pos.CENTER_LEFT);
        displayRow.setPadding(new Insets(2, 0, 0, 16));

        Label displayLabel = new Label("Display:");
        displayLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        widgetDisplayComboBox = new ComboBox<>(FXCollections.observableArrayList("Always", "When active", "Never"));
        widgetDisplayComboBox.setValue(current.getWidgetDisplay());
        widgetDisplayComboBox.setPrefWidth(160);
        styleComboBox(widgetDisplayComboBox);
        widgetDisplayComboBox.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        displayRow.getChildren().addAll(displayLabel, widgetDisplayComboBox);

        VBox widgetBox = new VBox(6, widgetTitle, displayRow);

        getChildren().addAll(activateBox, showBox, widgetBox);
    }

    private CheckBox createSubCheckBox(String text, boolean initial) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(initial);
        styleCheckBox(cb);
        cb.setPadding(new Insets(0, 0, 0, 22));
        cb.selectedProperty().addListener((obs, oldV, newV) -> fireModified());
        return cb;
    }

    private void updateParameterNameSubState() {
        boolean selected = parameterNameHintsCheckBox.isSelected();
        forAllParametersCheckBox.setDisable(!selected);
    }

    private void updateTypeHintsSubState() {
        boolean selected = typeHintsCheckBox.isSelected();
        memberVariablesCheckBox.setDisable(!selected);
        localVariablesCheckBox.setDisable(!selected);
        methodResultsCheckBox.setDisable(!selected);
        lambdaParametersCheckBox.setDisable(!selected);
        lambdaPlaceholdersCheckBox.setDisable(!selected);
        variablePatternsCheckBox.setDisable(!selected);
    }

    private Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        return label;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-font-size: 13px;");
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModified(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void takeSnapshot() {
        this.initialSettings = buildSettingsFromUi();
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, buildSettingsFromUi());
    }

    public ScalaXRaySettings buildSettingsFromUi() {
        ScalaXRaySettings s = new ScalaXRaySettings();
        s.setDoublePressAndHoldCtrl(doublePressAndHoldCtrlCheckBox.isSelected());
        s.setPressAndHoldCtrl(pressAndHoldCtrlCheckBox.isSelected());

        s.setParameterNameHints(parameterNameHintsCheckBox.isSelected());
        s.setForAllParameters(forAllParametersCheckBox.isSelected());
        s.setByNameArgumentHints(byNameArgumentHintsCheckBox.isSelected());
        s.setApplyMethodHints(applyMethodHintsCheckBox.isSelected());

        s.setTypeHints(typeHintsCheckBox.isSelected());
        s.setMemberVariables(memberVariablesCheckBox.isSelected());
        s.setLocalVariables(localVariablesCheckBox.isSelected());
        s.setMethodResults(methodResultsCheckBox.isSelected());
        s.setLambdaParameters(lambdaParametersCheckBox.isSelected());
        s.setLambdaPlaceholders(lambdaPlaceholdersCheckBox.isSelected());
        s.setVariablePatterns(variablePatternsCheckBox.isSelected());

        s.setMethodChainHints(methodChainHintsCheckBox.isSelected());
        s.setTypeArguments(typeArgumentsCheckBox.isSelected());
        s.setImplicitHints(implicitHintsCheckBox.isSelected());
        s.setIndentGuides(indentGuidesCheckBox.isSelected());
        s.setMethodSeparators(methodSeparatorsCheckBox.isSelected());

        s.setWidgetDisplay(widgetDisplayComboBox.getValue());
        return s;
    }

    public void apply() {
        ScalaXRaySettings s = buildSettingsFromUi();
        manager.setXRaySettings(s);
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        ScalaXRaySettings current = manager.getXRaySettings();

        doublePressAndHoldCtrlCheckBox.setSelected(current.isDoublePressAndHoldCtrl());
        pressAndHoldCtrlCheckBox.setSelected(current.isPressAndHoldCtrl());

        parameterNameHintsCheckBox.setSelected(current.isParameterNameHints());
        forAllParametersCheckBox.setSelected(current.isForAllParameters());
        updateParameterNameSubState();

        byNameArgumentHintsCheckBox.setSelected(current.isByNameArgumentHints());
        applyMethodHintsCheckBox.setSelected(current.isApplyMethodHints());

        typeHintsCheckBox.setSelected(current.isTypeHints());
        memberVariablesCheckBox.setSelected(current.isMemberVariables());
        localVariablesCheckBox.setSelected(current.isLocalVariables());
        methodResultsCheckBox.setSelected(current.isMethodResults());
        lambdaParametersCheckBox.setSelected(current.isLambdaParameters());
        lambdaPlaceholdersCheckBox.setSelected(current.isLambdaPlaceholders());
        variablePatternsCheckBox.setSelected(current.isVariablePatterns());
        updateTypeHintsSubState();

        methodChainHintsCheckBox.setSelected(current.isMethodChainHints());
        typeArgumentsCheckBox.setSelected(current.isTypeArguments());
        implicitHintsCheckBox.setSelected(current.isImplicitHints());
        indentGuidesCheckBox.setSelected(current.isIndentGuides());
        methodSeparatorsCheckBox.setSelected(current.isMethodSeparators());

        widgetDisplayComboBox.setValue(current.getWidgetDisplay());

        takeSnapshot();
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    // Public getters for testing
    public CheckBox getDoublePressAndHoldCtrlCheckBox() { return doublePressAndHoldCtrlCheckBox; }
    public CheckBox getPressAndHoldCtrlCheckBox() { return pressAndHoldCtrlCheckBox; }
    public CheckBox getParameterNameHintsCheckBox() { return parameterNameHintsCheckBox; }
    public CheckBox getForAllParametersCheckBox() { return forAllParametersCheckBox; }
    public CheckBox getByNameArgumentHintsCheckBox() { return byNameArgumentHintsCheckBox; }
    public CheckBox getApplyMethodHintsCheckBox() { return applyMethodHintsCheckBox; }
    public CheckBox getTypeHintsCheckBox() { return typeHintsCheckBox; }
    public CheckBox getMemberVariablesCheckBox() { return memberVariablesCheckBox; }
    public CheckBox getLocalVariablesCheckBox() { return localVariablesCheckBox; }
    public CheckBox getMethodResultsCheckBox() { return methodResultsCheckBox; }
    public CheckBox getLambdaParametersCheckBox() { return lambdaParametersCheckBox; }
    public CheckBox getLambdaPlaceholdersCheckBox() { return lambdaPlaceholdersCheckBox; }
    public CheckBox getVariablePatternsCheckBox() { return variablePatternsCheckBox; }
    public CheckBox getMethodChainHintsCheckBox() { return methodChainHintsCheckBox; }
    public CheckBox getTypeArgumentsCheckBox() { return typeArgumentsCheckBox; }
    public CheckBox getImplicitHintsCheckBox() { return implicitHintsCheckBox; }
    public CheckBox getIndentGuidesCheckBox() { return indentGuidesCheckBox; }
    public CheckBox getMethodSeparatorsCheckBox() { return methodSeparatorsCheckBox; }
    public ComboBox<String> getWidgetDisplayComboBox() { return widgetDisplayComboBox; }
}
