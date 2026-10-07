package dev.lumina.ui;

import dev.lumina.injections.LanguageInjectionAdvancedSettings;
import dev.lumina.injections.LanguageInjectionAdvancedSettings.PerformanceMode;
import dev.lumina.injections.LanguageInjectionAdvancedSettings.RuntimePatternValidation;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Settings page for Editor > Language Injections > Advanced.
 * Strictly reproduces the design, controls, and layout shown in reference screenshot:
 * - Annotation Classes (Language, Pattern, Substitution) with textfields and browse '...' buttons
 * - Runtime Pattern Validation radio group
 * - Performance mode radio group
 * - Concatenation & annotation checkboxes
 */
public class SettingsLanguageInjectionsAdvancedPage extends VBox {

    private final LanguageInjectionAdvancedSettings settings = LanguageInjectionAdvancedSettings.getInstance();

    // Annotation text fields
    private final TextField languageAnnotationField = new TextField();
    private final TextField patternAnnotationField = new TextField();
    private final TextField substitutionAnnotationField = new TextField();

    // Runtime pattern validation radio buttons
    private final ToggleGroup validationGroup = new ToggleGroup();
    private final RadioButton noRuntimeRadio = new RadioButton("No runtime instrumentation");
    private final RadioButton assertionsRadio = new RadioButton("Instrument with assertions");
    private final RadioButton illegalArgumentRadio = new RadioButton("Instrument with IllegalArgumentException");

    // Performance radio buttons
    private final ToggleGroup performanceGroup = new ToggleGroup();
    private final RadioButton doNotAnalyzeRadio = new RadioButton("Do not analyze anything (fast)");
    private final RadioButton analyzeReferencesRadio = new RadioButton("Analyze references");
    private final RadioButton lookForAssignmentsRadio = new RadioButton("Look for variable assignments");
    private final RadioButton dataflowRadio = new RadioButton("Use dataflow analysis (slow)");

    // Checkboxes
    private final CheckBox convertUndefinedOperandsCheck = new CheckBox("Convert undefined operands to text in concatenations");
    private final CheckBox addLanguageAnnotationCheck = new CheckBox("Add @Language annotation or comment if needed");

    private Runnable onModifiedListener;
    private final BooleanProperty modifiedProperty = new SimpleBooleanProperty(false);
    private boolean updatingUI = false;

    public SettingsLanguageInjectionsAdvancedPage() {
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 18, 24));
        setSpacing(14);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadSettings();

        settings.setOnModifiedListener(() -> {
            modifiedProperty.set(settings.isModified());
            if (onModifiedListener != null) {
                onModifiedListener.run();
            }
        });
    }

    private void buildUI() {
        getChildren().clear();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #1E1F22; -fx-background: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox content = new VBox(14);
        content.setStyle("-fx-background-color: #1E1F22;");
        content.setPadding(new Insets(2, 4, 10, 4));

        // 1. Annotation Classes Section
        Node annotationSection = buildAnnotationClassesSection();

        // 2. Runtime Pattern Validation Section
        Node validationSection = buildRuntimeValidationSection();

        // 3. Performance Section
        Node performanceSection = buildPerformanceSection();

        // 4. Additional Checkboxes Section
        Node checkboxesSection = buildCheckboxesSection();

        content.getChildren().addAll(annotationSection, validationSection, performanceSection, checkboxesSection);
        scrollPane.setContent(content);

        getChildren().add(scrollPane);
    }

    // =========================================================================
    // Section 1: Annotation Classes
    // =========================================================================

    private VBox buildAnnotationClassesSection() {
        VBox box = new VBox(8);

        box.getChildren().add(createSectionHeader("Annotation Classes"));

        VBox fieldsBox = new VBox(10);
        fieldsBox.setPadding(new Insets(4, 0, 4, 12));

        fieldsBox.getChildren().addAll(
                createClassInputField("Language annotation class", languageAnnotationField),
                createClassInputField("Pattern annotation class", patternAnnotationField),
                createClassInputField("Substitution annotation class", substitutionAnnotationField)
        );

        box.getChildren().add(fieldsBox);
        return box;
    }

    private VBox createClassInputField(String labelText, TextField field) {
        VBox item = new VBox(4);

        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        field.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #3E4147; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-font-size: 12px;");
        HBox.setHgrow(field, Priority.ALWAYS);
        field.setMaxWidth(Double.MAX_VALUE);

        field.textProperty().addListener((obs, oldV, newV) -> {
            if (!updatingUI) {
                if (field == languageAnnotationField) settings.setLanguageAnnotation(newV);
                else if (field == patternAnnotationField) settings.setPatternAnnotation(newV);
                else if (field == substitutionAnnotationField) settings.setSubstitutionAnnotation(newV);
                notifyModified();
            }
        });

        Button browseBtn = new Button("...");
        browseBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #868A91; -fx-border-color: #3E4147; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 10; -fx-cursor: hand; -fx-font-size: 11px;");
        browseBtn.setOnMouseEntered(e -> browseBtn.setStyle("-fx-background-color: #35373C; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5158; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 10; -fx-cursor: hand; -fx-font-size: 11px;"));
        browseBtn.setOnMouseExited(e -> browseBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #868A91; -fx-border-color: #3E4147; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 10; -fx-cursor: hand; -fx-font-size: 11px;"));
        browseBtn.setOnAction(e -> openClassChooserDialog(labelText, field));

        HBox inputRow = new HBox(6, field, browseBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.setMaxWidth(720);

        item.getChildren().addAll(label, inputRow);
        return item;
    }

    private void openClassChooserDialog(String title, TextField field) {
        TextInputDialog dialog = new TextInputDialog(field.getText());
        dialog.setTitle("Choose Annotation Class");
        dialog.setHeaderText("Specify class name for " + title + ":");
        dialog.getDialogPane().setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");
        dialog.showAndWait().ifPresent(chosen -> {
            if (!chosen.isBlank()) {
                field.setText(chosen.trim());
            }
        });
    }

    // =========================================================================
    // Section 2: Runtime Pattern Validation
    // =========================================================================

    private VBox buildRuntimeValidationSection() {
        VBox box = new VBox(8);

        box.getChildren().add(createSectionHeader("Runtime Pattern Validation"));

        VBox radioBox = new VBox(6);
        radioBox.setPadding(new Insets(4, 0, 4, 12));

        noRuntimeRadio.setToggleGroup(validationGroup);
        assertionsRadio.setToggleGroup(validationGroup);
        illegalArgumentRadio.setToggleGroup(validationGroup);

        styleRadioButton(noRuntimeRadio);
        styleRadioButton(assertionsRadio);
        styleRadioButton(illegalArgumentRadio);

        validationGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (!updatingUI) {
                if (newV == noRuntimeRadio) {
                    settings.setRuntimePatternValidation(RuntimePatternValidation.NO_RUNTIME);
                } else if (newV == illegalArgumentRadio) {
                    settings.setRuntimePatternValidation(RuntimePatternValidation.ILLEGAL_ARGUMENT_EXCEPTION);
                } else {
                    settings.setRuntimePatternValidation(RuntimePatternValidation.ASSERTIONS);
                }
                notifyModified();
            }
        });

        radioBox.getChildren().addAll(noRuntimeRadio, assertionsRadio, illegalArgumentRadio);
        box.getChildren().add(radioBox);
        return box;
    }

    // =========================================================================
    // Section 3: Performance
    // =========================================================================

    private VBox buildPerformanceSection() {
        VBox box = new VBox(8);

        box.getChildren().add(createSectionHeader("Performance"));

        VBox radioBox = new VBox(6);
        radioBox.setPadding(new Insets(4, 0, 4, 12));

        doNotAnalyzeRadio.setToggleGroup(performanceGroup);
        analyzeReferencesRadio.setToggleGroup(performanceGroup);
        lookForAssignmentsRadio.setToggleGroup(performanceGroup);
        dataflowRadio.setToggleGroup(performanceGroup);

        styleRadioButton(doNotAnalyzeRadio);
        styleRadioButton(analyzeReferencesRadio);
        styleRadioButton(lookForAssignmentsRadio);
        styleRadioButton(dataflowRadio);

        performanceGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (!updatingUI) {
                if (newV == doNotAnalyzeRadio) {
                    settings.setPerformanceMode(PerformanceMode.DO_NOT_ANALYZE);
                } else if (newV == lookForAssignmentsRadio) {
                    settings.setPerformanceMode(PerformanceMode.LOOK_FOR_ASSIGNMENTS);
                } else if (newV == dataflowRadio) {
                    settings.setPerformanceMode(PerformanceMode.DATAFLOW_ANALYSIS);
                } else {
                    settings.setPerformanceMode(PerformanceMode.ANALYZE_REFERENCES);
                }
                notifyModified();
            }
        });

        radioBox.getChildren().addAll(doNotAnalyzeRadio, analyzeReferencesRadio, lookForAssignmentsRadio, dataflowRadio);
        box.getChildren().add(radioBox);
        return box;
    }

    // =========================================================================
    // Section 4: Checkboxes
    // =========================================================================

    private VBox buildCheckboxesSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(6, 0, 4, 12));

        convertUndefinedOperandsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        addLanguageAnnotationCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        convertUndefinedOperandsCheck.setOnAction(e -> {
            if (!updatingUI) {
                settings.setConvertUndefinedOperandsToText(convertUndefinedOperandsCheck.isSelected());
                notifyModified();
            }
        });

        addLanguageAnnotationCheck.setOnAction(e -> {
            if (!updatingUI) {
                settings.setAddLanguageAnnotationOrComment(addLanguageAnnotationCheck.isSelected());
                notifyModified();
            }
        });

        box.getChildren().addAll(convertUndefinedOperandsCheck, addLanguageAnnotationCheck);
        return box;
    }

    // =========================================================================
    // Helpers & Styling
    // =========================================================================

    private Node createSectionHeader(String title) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        header.getChildren().addAll(label, sep);
        return header;
    }

    private void styleRadioButton(RadioButton radio) {
        radio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    private void notifyModified() {
        modifiedProperty.set(settings.isModified());
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void loadSettings() {
        updatingUI = true;
        try {
            languageAnnotationField.setText(settings.getLanguageAnnotation());
            patternAnnotationField.setText(settings.getPatternAnnotation());
            substitutionAnnotationField.setText(settings.getSubstitutionAnnotation());

            switch (settings.getRuntimePatternValidation()) {
                case NO_RUNTIME -> validationGroup.selectToggle(noRuntimeRadio);
                case ILLEGAL_ARGUMENT_EXCEPTION -> validationGroup.selectToggle(illegalArgumentRadio);
                default -> validationGroup.selectToggle(assertionsRadio);
            }

            switch (settings.getPerformanceMode()) {
                case DO_NOT_ANALYZE -> performanceGroup.selectToggle(doNotAnalyzeRadio);
                case LOOK_FOR_ASSIGNMENTS -> performanceGroup.selectToggle(lookForAssignmentsRadio);
                case DATAFLOW_ANALYSIS -> performanceGroup.selectToggle(dataflowRadio);
                default -> performanceGroup.selectToggle(analyzeReferencesRadio);
            }

            convertUndefinedOperandsCheck.setSelected(settings.isConvertUndefinedOperandsToText());
            addLanguageAnnotationCheck.setSelected(settings.isAddLanguageAnnotationOrComment());
        } finally {
            updatingUI = false;
        }
    }

    // =========================================================================
    // Lifecycle (Apply / Reset / isModified)
    // =========================================================================

    public boolean isModified() {
        return settings.isModified();
    }

    public void apply() {
        settings.apply();
        modifiedProperty.set(false);
    }

    public void reset() {
        settings.reset();
        modifiedProperty.set(false);
        loadSettings();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public BooleanProperty modifiedProperty() {
        return modifiedProperty;
    }

    // Accessors for testing
    public TextField getLanguageAnnotationField() {
        return languageAnnotationField;
    }

    public TextField getPatternAnnotationField() {
        return patternAnnotationField;
    }

    public TextField getSubstitutionAnnotationField() {
        return substitutionAnnotationField;
    }

    public ToggleGroup getValidationGroup() {
        return validationGroup;
    }

    public ToggleGroup getPerformanceGroup() {
        return performanceGroup;
    }

    public CheckBox getConvertUndefinedOperandsCheck() {
        return convertUndefinedOperandsCheck;
    }

    public CheckBox getAddLanguageAnnotationCheck() {
        return addLanguageAnnotationCheck;
    }
}
