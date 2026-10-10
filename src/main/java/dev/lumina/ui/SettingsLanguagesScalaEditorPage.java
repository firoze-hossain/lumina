package dev.lumina.ui;

import dev.lumina.scala.ScalaEditorSettings;
import dev.lumina.scala.ScalaLanguageSettingsManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Languages & Frameworks > Scala > Editor settings page in Lumina IDE.
 * Matches Image 4 with Built-in Highlighting, export aliases, and collapsible Advanced settings.
 */
public class SettingsLanguagesScalaEditorPage extends VBox {

    private final ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
    private Runnable onModifiedListener;

    // Built-in Highlighting
    private CheckBox showHintsOnTypeMismatchCheckBox;
    private CheckBox showHintsIfNoImplicitCheckBox;
    private CheckBox showHintsIfAmbiguousImplicitCheckBox;
    private ComboBox<String> exportAliasesComboBox;

    // Advanced Section
    private Button advancedToggleBtn;
    private VBox advancedContentBox;
    private boolean advancedExpanded = true;

    private CheckBox highlightImplicitConversionsCheckBox;
    private CheckBox highlightArgumentsToByNameCheckBox;
    private CheckBox includeBlockExpressionsCheckBox;
    private CheckBox includeLiteralsCheckBox;
    private CheckBox customScalaTestKeywordsCheckBox;
    private ComboBox<String> collectionTypeHighlightingComboBox;
    private CheckBox aheadOfTimeCompletionCheckBox;
    private CheckBox useScalaClassesPriorityCheckBox;
    private CheckBox convertJavaCodeToScalaCheckBox;
    private CheckBox dontShowPasteDialogCheckBox;
    private CheckBox addOverrideKeywordCheckBox;

    private ScalaEditorSettings initialSettings;

    public SettingsLanguagesScalaEditorPage() {
        setSpacing(12);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ScalaEditorSettings current = manager.getEditorSettings();

        // 1. Built-in Highlighting Header
        HBox highlightingHeader = createSectionHeader("Built-in Highlighting");

        showHintsOnTypeMismatchCheckBox = new CheckBox("Show hints on type mismatch");
        showHintsOnTypeMismatchCheckBox.setSelected(current.isShowHintsOnTypeMismatch());
        styleCheckBox(showHintsOnTypeMismatchCheckBox);
        showHintsOnTypeMismatchCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        showHintsIfNoImplicitCheckBox = new CheckBox("Show hints if no implicit arguments found");
        showHintsIfNoImplicitCheckBox.setSelected(current.isShowHintsIfNoImplicitArgumentsFound());
        styleCheckBox(showHintsIfNoImplicitCheckBox);
        showHintsIfNoImplicitCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        showHintsIfAmbiguousImplicitCheckBox = new CheckBox("Show hints if ambiguous implicit arguments found");
        showHintsIfAmbiguousImplicitCheckBox.setSelected(current.isShowHintsIfAmbiguousImplicitArgumentsFound());
        styleCheckBox(showHintsIfAmbiguousImplicitCheckBox);
        showHintsIfAmbiguousImplicitCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        // Export aliases row
        HBox exportAliasesRow = new HBox(12);
        exportAliasesRow.setAlignment(Pos.CENTER_LEFT);

        Label exportLabel = new Label("Export aliases are:");
        exportLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        exportAliasesComboBox = new ComboBox<>(FXCollections.observableArrayList("Exports", "Aliases", "None"));
        exportAliasesComboBox.setValue(current.getExportAliases());
        exportAliasesComboBox.setPrefWidth(140);
        styleComboBox(exportAliasesComboBox);
        exportAliasesComboBox.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        Label helpIcon = new Label("(?)");
        helpIcon.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-cursor: hand;");
        helpIcon.setTooltip(new Tooltip("Specifies whether export aliases in Scala 3 are treated as exports or general aliases"));

        exportAliasesRow.getChildren().addAll(exportLabel, exportAliasesComboBox, helpIcon);

        VBox builtInBox = new VBox(8,
                highlightingHeader,
                showHintsOnTypeMismatchCheckBox,
                showHintsIfNoImplicitCheckBox,
                showHintsIfAmbiguousImplicitCheckBox,
                exportAliasesRow
        );

        // 2. Advanced Section (Collapsible)
        HBox advancedHeader = new HBox(6);
        advancedHeader.setAlignment(Pos.CENTER_LEFT);
        advancedHeader.setPadding(new Insets(8, 0, 4, 0));

        advancedToggleBtn = new Button("\u25BE Advanced");
        advancedToggleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0; -fx-cursor: hand;");
        advancedToggleBtn.setOnAction(e -> toggleAdvanced());

        Separator advancedLine = new Separator();
        advancedLine.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(advancedLine, Priority.ALWAYS);

        advancedHeader.getChildren().addAll(advancedToggleBtn, advancedLine);

        advancedContentBox = new VBox(8);
        advancedContentBox.setPadding(new Insets(4, 0, 0, 16));

        highlightImplicitConversionsCheckBox = new CheckBox("Highlight implicit conversions");
        highlightImplicitConversionsCheckBox.setSelected(current.isHighlightImplicitConversions());
        styleCheckBox(highlightImplicitConversionsCheckBox);
        highlightImplicitConversionsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        highlightArgumentsToByNameCheckBox = new CheckBox("Highlight arguments to by-name parameters");
        highlightArgumentsToByNameCheckBox.setSelected(current.isHighlightArgumentsToByNameParameters());
        styleCheckBox(highlightArgumentsToByNameCheckBox);
        highlightArgumentsToByNameCheckBox.selectedProperty().addListener((obs, oldV, newV) -> {
            updateByNameSubItemsState();
            fireModified();
        });

        includeBlockExpressionsCheckBox = new CheckBox("Include block expressions");
        includeBlockExpressionsCheckBox.setSelected(current.isIncludeBlockExpressions());
        styleCheckBox(includeBlockExpressionsCheckBox);
        includeBlockExpressionsCheckBox.setPadding(new Insets(0, 0, 0, 22));
        includeBlockExpressionsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        includeLiteralsCheckBox = new CheckBox("Include literals");
        includeLiteralsCheckBox.setSelected(current.isIncludeLiterals());
        styleCheckBox(includeLiteralsCheckBox);
        includeLiteralsCheckBox.setPadding(new Insets(0, 0, 0, 22));
        includeLiteralsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        updateByNameSubItemsState();

        customScalaTestKeywordsCheckBox = new CheckBox("Custom scalaTest keywords highlighting");
        customScalaTestKeywordsCheckBox.setSelected(current.isCustomScalaTestKeywordsHighlighting());
        styleCheckBox(customScalaTestKeywordsCheckBox);
        customScalaTestKeywordsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        // Collection type highlighting row
        HBox collectionRow = new HBox(12);
        collectionRow.setAlignment(Pos.CENTER_LEFT);

        Label collectionLabel = new Label("Collection type highlighting:");
        collectionLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        collectionTypeHighlightingComboBox = new ComboBox<>(FXCollections.observableArrayList("None", "Standard collections", "All collections"));
        collectionTypeHighlightingComboBox.setValue(current.getCollectionTypeHighlighting());
        collectionTypeHighlightingComboBox.setPrefWidth(160);
        styleComboBox(collectionTypeHighlightingComboBox);
        collectionTypeHighlightingComboBox.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        collectionRow.getChildren().addAll(collectionLabel, collectionTypeHighlightingComboBox);

        aheadOfTimeCompletionCheckBox = new CheckBox("Ahead-of-time completion (parameter and variable names)");
        aheadOfTimeCompletionCheckBox.setSelected(current.isAheadOfTimeCompletion());
        styleCheckBox(aheadOfTimeCompletionCheckBox);
        aheadOfTimeCompletionCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        useScalaClassesPriorityCheckBox = new CheckBox("Use Scala classes priority over Java classes");
        useScalaClassesPriorityCheckBox.setSelected(current.isUseScalaClassesPriorityOverJavaClasses());
        styleCheckBox(useScalaClassesPriorityCheckBox);
        useScalaClassesPriorityCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        convertJavaCodeToScalaCheckBox = new CheckBox("Convert Java code to Scala on copy-paste");
        convertJavaCodeToScalaCheckBox.setSelected(current.isConvertJavaCodeToScalaOnCopyPaste());
        styleCheckBox(convertJavaCodeToScalaCheckBox);
        convertJavaCodeToScalaCheckBox.selectedProperty().addListener((obs, oldV, newV) -> {
            updateCopyPasteSubItemsState();
            fireModified();
        });

        dontShowPasteDialogCheckBox = new CheckBox("Don't show dialog on paste and automatically convert to Scala code");
        dontShowPasteDialogCheckBox.setSelected(current.isDontShowDialogOnPasteAndAutomaticallyConvert());
        styleCheckBox(dontShowPasteDialogCheckBox);
        dontShowPasteDialogCheckBox.setPadding(new Insets(0, 0, 0, 22));
        dontShowPasteDialogCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        updateCopyPasteSubItemsState();

        addOverrideKeywordCheckBox = new CheckBox("Add override keyword to method implementation");
        addOverrideKeywordCheckBox.setSelected(current.isAddOverrideKeywordToMethodImplementation());
        styleCheckBox(addOverrideKeywordCheckBox);
        addOverrideKeywordCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        advancedContentBox.getChildren().addAll(
                highlightImplicitConversionsCheckBox,
                highlightArgumentsToByNameCheckBox,
                includeBlockExpressionsCheckBox,
                includeLiteralsCheckBox,
                customScalaTestKeywordsCheckBox,
                collectionRow,
                aheadOfTimeCompletionCheckBox,
                useScalaClassesPriorityCheckBox,
                convertJavaCodeToScalaCheckBox,
                dontShowPasteDialogCheckBox,
                addOverrideKeywordCheckBox
        );

        getChildren().addAll(builtInBox, advancedHeader, advancedContentBox);
    }

    private void toggleAdvanced() {
        advancedExpanded = !advancedExpanded;
        advancedContentBox.setVisible(advancedExpanded);
        advancedContentBox.setManaged(advancedExpanded);
        advancedToggleBtn.setText(advancedExpanded ? "\u25BE Advanced" : "\u25B8 Advanced");
    }

    private void updateByNameSubItemsState() {
        boolean selected = highlightArgumentsToByNameCheckBox.isSelected();
        includeBlockExpressionsCheckBox.setDisable(!selected);
        includeLiteralsCheckBox.setDisable(!selected);
    }

    private void updateCopyPasteSubItemsState() {
        boolean selected = convertJavaCodeToScalaCheckBox.isSelected();
        dontShowPasteDialogCheckBox.setDisable(!selected);
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 4, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator line = new Separator();
        line.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(label, line);
        return header;
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

    public ScalaEditorSettings buildSettingsFromUi() {
        ScalaEditorSettings s = new ScalaEditorSettings();
        s.setShowHintsOnTypeMismatch(showHintsOnTypeMismatchCheckBox.isSelected());
        s.setShowHintsIfNoImplicitArgumentsFound(showHintsIfNoImplicitCheckBox.isSelected());
        s.setShowHintsIfAmbiguousImplicitArgumentsFound(showHintsIfAmbiguousImplicitCheckBox.isSelected());
        s.setExportAliases(exportAliasesComboBox.getValue());

        s.setHighlightImplicitConversions(highlightImplicitConversionsCheckBox.isSelected());
        s.setHighlightArgumentsToByNameParameters(highlightArgumentsToByNameCheckBox.isSelected());
        s.setIncludeBlockExpressions(includeBlockExpressionsCheckBox.isSelected());
        s.setIncludeLiterals(includeLiteralsCheckBox.isSelected());
        s.setCustomScalaTestKeywordsHighlighting(customScalaTestKeywordsCheckBox.isSelected());
        s.setCollectionTypeHighlighting(collectionTypeHighlightingComboBox.getValue());
        s.setAheadOfTimeCompletion(aheadOfTimeCompletionCheckBox.isSelected());
        s.setUseScalaClassesPriorityOverJavaClasses(useScalaClassesPriorityCheckBox.isSelected());
        s.setConvertJavaCodeToScalaOnCopyPaste(convertJavaCodeToScalaCheckBox.isSelected());
        s.setDontShowDialogOnPasteAndAutomaticallyConvert(dontShowPasteDialogCheckBox.isSelected());
        s.setAddOverrideKeywordToMethodImplementation(addOverrideKeywordCheckBox.isSelected());
        return s;
    }

    public void apply() {
        ScalaEditorSettings s = buildSettingsFromUi();
        manager.setEditorSettings(s);
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        ScalaEditorSettings current = manager.getEditorSettings();

        showHintsOnTypeMismatchCheckBox.setSelected(current.isShowHintsOnTypeMismatch());
        showHintsIfNoImplicitCheckBox.setSelected(current.isShowHintsIfNoImplicitArgumentsFound());
        showHintsIfAmbiguousImplicitCheckBox.setSelected(current.isShowHintsIfAmbiguousImplicitArgumentsFound());
        exportAliasesComboBox.setValue(current.getExportAliases());

        highlightImplicitConversionsCheckBox.setSelected(current.isHighlightImplicitConversions());
        highlightArgumentsToByNameCheckBox.setSelected(current.isHighlightArgumentsToByNameParameters());
        includeBlockExpressionsCheckBox.setSelected(current.isIncludeBlockExpressions());
        includeLiteralsCheckBox.setSelected(current.isIncludeLiterals());
        updateByNameSubItemsState();

        customScalaTestKeywordsCheckBox.setSelected(current.isCustomScalaTestKeywordsHighlighting());
        collectionTypeHighlightingComboBox.setValue(current.getCollectionTypeHighlighting());
        aheadOfTimeCompletionCheckBox.setSelected(current.isAheadOfTimeCompletion());
        useScalaClassesPriorityCheckBox.setSelected(current.isUseScalaClassesPriorityOverJavaClasses());
        convertJavaCodeToScalaCheckBox.setSelected(current.isConvertJavaCodeToScalaOnCopyPaste());
        dontShowPasteDialogCheckBox.setSelected(current.isDontShowDialogOnPasteAndAutomaticallyConvert());
        updateCopyPasteSubItemsState();
        addOverrideKeywordCheckBox.setSelected(current.isAddOverrideKeywordToMethodImplementation());

        takeSnapshot();
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    // Public getters for testing
    public CheckBox getShowHintsOnTypeMismatchCheckBox() { return showHintsOnTypeMismatchCheckBox; }
    public CheckBox getShowHintsIfNoImplicitCheckBox() { return showHintsIfNoImplicitCheckBox; }
    public CheckBox getShowHintsIfAmbiguousImplicitCheckBox() { return showHintsIfAmbiguousImplicitCheckBox; }
    public ComboBox<String> getExportAliasesComboBox() { return exportAliasesComboBox; }
    public CheckBox getHighlightImplicitConversionsCheckBox() { return highlightImplicitConversionsCheckBox; }
    public CheckBox getHighlightArgumentsToByNameCheckBox() { return highlightArgumentsToByNameCheckBox; }
    public CheckBox getIncludeBlockExpressionsCheckBox() { return includeBlockExpressionsCheckBox; }
    public CheckBox getIncludeLiteralsCheckBox() { return includeLiteralsCheckBox; }
    public CheckBox getCustomScalaTestKeywordsCheckBox() { return customScalaTestKeywordsCheckBox; }
    public ComboBox<String> getCollectionTypeHighlightingComboBox() { return collectionTypeHighlightingComboBox; }
    public CheckBox getAheadOfTimeCompletionCheckBox() { return aheadOfTimeCompletionCheckBox; }
    public CheckBox getUseScalaClassesPriorityCheckBox() { return useScalaClassesPriorityCheckBox; }
    public CheckBox getConvertJavaCodeToScalaCheckBox() { return convertJavaCodeToScalaCheckBox; }
    public CheckBox getDontShowPasteDialogCheckBox() { return dontShowPasteDialogCheckBox; }
    public CheckBox getAddOverrideKeywordCheckBox() { return addOverrideKeywordCheckBox; }
    public Button getAdvancedToggleBtn() { return advancedToggleBtn; }
}
