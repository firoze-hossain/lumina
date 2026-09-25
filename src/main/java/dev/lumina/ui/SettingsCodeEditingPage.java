package dev.lumina.ui;

import dev.lumina.settings.CodeEditingSettings;
import dev.lumina.settings.CodeEditingSettings.RefactoringOption;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Editor > Code Editing settings page.
 * Dynamic persistent implementation matching the modern IDE design.
 */
public class SettingsCodeEditingPage extends VBox {

    // Highlight on Caret Movement
    private final CheckBox matchedBraceCheck = new CheckBox("Matched brace");
    private final CheckBox currentScopeCheck = new CheckBox("Current scope");
    private final CheckBox usagesAtCaretCheck = new CheckBox("Usages of element at caret");

    // Quick Documentation
    private final CheckBox showQuickDocHoverCheck = new CheckBox("Show quick documentation on hover");

    // Refactorings
    private final ToggleGroup refactoringGroup = new ToggleGroup();
    private final RadioButton inEditorRadio = new RadioButton("In the editor");
    private final RadioButton inModalRadio = new RadioButton("In modal dialogs");
    private final CheckBox preselectRenameCheck = new CheckBox("Preselect current symbol name for Rename refactoring");
    private final CheckBox showInlineDialogCheck = new CheckBox("Show inline dialog for local variables");

    // Error Highlighting
    private final TextField errorStripeMinHeightField = new TextField("2");
    private final TextField autoreparseDelayField = new TextField("300");
    private final ComboBox<String> nextErrorCombo = new ComboBox<>();
    private final CheckBox suppressWarningsCheck = new CheckBox("Suppress with @SuppressWarnings");

    // Editor Tooltips
    private final TextField tooltipDelayField = new TextField("500");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsCodeEditingPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);

        buildUi();
        setupListeners();
        loadFromSettings(CodeEditingSettings.getInstance());
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleNumberField(TextField tf, int width) {
        tf.setPrefWidth(width);
        tf.setMaxWidth(width);
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6 3 6; -fx-font-size: 12px;");
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 0, 4, 0));

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(lbl, line);
        return header;
    }

    private void buildUi() {
        // Highlight on Caret Movement
        styleCheckBox(matchedBraceCheck);
        styleCheckBox(currentScopeCheck);
        styleCheckBox(usagesAtCaretCheck);

        HBox highlightHeader = createSectionHeader("Highlight on Caret Movement");
        VBox highlightGroup = new VBox(8, matchedBraceCheck, currentScopeCheck, usagesAtCaretCheck);

        // Quick Documentation
        styleCheckBox(showQuickDocHoverCheck);
        HBox quickDocHeader = createSectionHeader("Quick Documentation");
        VBox quickDocGroup = new VBox(8, showQuickDocHoverCheck);

        // Refactorings
        HBox refactorHeader = createSectionHeader("Refactorings");
        Label specifyOptionsLabel = new Label("Specify refactoring options:");
        specifyOptionsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        inEditorRadio.setToggleGroup(refactoringGroup);
        inModalRadio.setToggleGroup(refactoringGroup);
        styleRadioButton(inEditorRadio);
        styleRadioButton(inModalRadio);

        VBox radioBox = new VBox(6, inEditorRadio, inModalRadio);
        radioBox.setPadding(new Insets(2, 0, 2, 20));

        styleCheckBox(preselectRenameCheck);
        styleCheckBox(showInlineDialogCheck);

        VBox refactorGroup = new VBox(8,
                specifyOptionsLabel,
                radioBox,
                preselectRenameCheck,
                showInlineDialogCheck
        );

        // Error Highlighting
        HBox errorHighlightHeader = createSectionHeader("Error Highlighting");

        styleNumberField(errorStripeMinHeightField, 45);
        Label minHeightLabel = new Label("Error stripe mark min height:");
        minHeightLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        Label pixelsLabel = new Label("pixels");
        pixelsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        HBox minHeightRow = new HBox(8, minHeightLabel, errorStripeMinHeightField, pixelsLabel);
        minHeightRow.setAlignment(Pos.CENTER_LEFT);

        styleNumberField(autoreparseDelayField, 55);
        Label autoreparseLabel = new Label("Autoreparse delay:");
        autoreparseLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        Label msLabel = new Label("milliseconds");
        msLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        HBox autoreparseRow = new HBox(8, autoreparseLabel, autoreparseDelayField, msLabel);
        autoreparseRow.setAlignment(Pos.CENTER_LEFT);

        nextErrorCombo.getItems().addAll(
                "The problems with the highest priority",
                "All problems"
        );
        nextErrorCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        Label nextErrorLabel = new Label("The 'Next Error' action goes through:");
        nextErrorLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        HBox nextErrorRow = new HBox(8, nextErrorLabel, nextErrorCombo);
        nextErrorRow.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(suppressWarningsCheck);

        VBox errorHighlightGroup = new VBox(8,
                minHeightRow,
                autoreparseRow,
                nextErrorRow,
                suppressWarningsCheck
        );

        // Editor Tooltips
        HBox tooltipsHeader = createSectionHeader("Editor Tooltips");

        styleNumberField(tooltipDelayField, 55);
        Label tooltipLabel = new Label("Tooltip delay:");
        tooltipLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        Label tooltipMsLabel = new Label("milliseconds");
        tooltipMsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        HBox tooltipsRow = new HBox(8, tooltipLabel, tooltipDelayField, tooltipMsLabel);
        tooltipsRow.setAlignment(Pos.CENTER_LEFT);

        VBox tooltipsGroup = new VBox(8, tooltipsRow);

        getChildren().addAll(
                highlightHeader, highlightGroup,
                quickDocHeader, quickDocGroup,
                refactorHeader, refactorGroup,
                errorHighlightHeader, errorHighlightGroup,
                tooltipsHeader, tooltipsGroup
        );
    }

    private void setupListeners() {
        matchedBraceCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        currentScopeCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        usagesAtCaretCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        showQuickDocHoverCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        inEditorRadio.selectedProperty().addListener((obs, old, val) -> notifyModified());
        inModalRadio.selectedProperty().addListener((obs, old, val) -> notifyModified());
        preselectRenameCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        showInlineDialogCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        errorStripeMinHeightField.textProperty().addListener((obs, old, val) -> notifyModified());
        autoreparseDelayField.textProperty().addListener((obs, old, val) -> notifyModified());
        nextErrorCombo.valueProperty().addListener((obs, old, val) -> notifyModified());
        suppressWarningsCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        tooltipDelayField.textProperty().addListener((obs, old, val) -> notifyModified());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void loadFromSettings(CodeEditingSettings s) {
        suppressEvents = true;
        try {
            matchedBraceCheck.setSelected(s.isMatchedBrace());
            currentScopeCheck.setSelected(s.isCurrentScope());
            usagesAtCaretCheck.setSelected(s.isUsagesOfElementAtCaret());

            showQuickDocHoverCheck.setSelected(s.isShowQuickDocOnHover());

            if (s.getRefactoringOption() == RefactoringOption.IN_MODAL_DIALOGS) {
                inModalRadio.setSelected(true);
            } else {
                inEditorRadio.setSelected(true);
            }

            preselectRenameCheck.setSelected(s.isPreselectCurrentSymbolForRename());
            showInlineDialogCheck.setSelected(s.isShowInlineDialogForLocalVariables());

            errorStripeMinHeightField.setText(String.valueOf(s.getErrorStripeMarkMinHeight()));
            autoreparseDelayField.setText(String.valueOf(s.getAutoreparseDelay()));
            nextErrorCombo.setValue(s.getNextErrorActionGoesThrough());
            suppressWarningsCheck.setSelected(s.isSuppressWithSuppressWarnings());

            tooltipDelayField.setText(String.valueOf(s.getTooltipDelay()));
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        CodeEditingSettings s = CodeEditingSettings.getInstance();
        boolean refactModified = (inEditorRadio.isSelected() && s.getRefactoringOption() != RefactoringOption.IN_EDITOR)
                || (inModalRadio.isSelected() && s.getRefactoringOption() != RefactoringOption.IN_MODAL_DIALOGS);

        int minHeight = parseSafeInt(errorStripeMinHeightField.getText(), s.getErrorStripeMarkMinHeight());
        int autoreparse = parseSafeInt(autoreparseDelayField.getText(), s.getAutoreparseDelay());
        int tooltipDelay = parseSafeInt(tooltipDelayField.getText(), s.getTooltipDelay());

        return matchedBraceCheck.isSelected() != s.isMatchedBrace()
                || currentScopeCheck.isSelected() != s.isCurrentScope()
                || usagesAtCaretCheck.isSelected() != s.isUsagesOfElementAtCaret()
                || showQuickDocHoverCheck.isSelected() != s.isShowQuickDocOnHover()
                || refactModified
                || preselectRenameCheck.isSelected() != s.isPreselectCurrentSymbolForRename()
                || showInlineDialogCheck.isSelected() != s.isShowInlineDialogForLocalVariables()
                || minHeight != s.getErrorStripeMarkMinHeight()
                || autoreparse != s.getAutoreparseDelay()
                || !java.util.Objects.equals(nextErrorCombo.getValue(), s.getNextErrorActionGoesThrough())
                || suppressWarningsCheck.isSelected() != s.isSuppressWithSuppressWarnings()
                || tooltipDelay != s.getTooltipDelay();
    }

    public void apply() {
        CodeEditingSettings s = CodeEditingSettings.getInstance();
        s.setMatchedBrace(matchedBraceCheck.isSelected());
        s.setCurrentScope(currentScopeCheck.isSelected());
        s.setUsagesOfElementAtCaret(usagesAtCaretCheck.isSelected());

        s.setShowQuickDocOnHover(showQuickDocHoverCheck.isSelected());

        s.setRefactoringOption(inModalRadio.isSelected() ? RefactoringOption.IN_MODAL_DIALOGS : RefactoringOption.IN_EDITOR);
        s.setPreselectCurrentSymbolForRename(preselectRenameCheck.isSelected());
        s.setShowInlineDialogForLocalVariables(showInlineDialogCheck.isSelected());

        s.setErrorStripeMarkMinHeight(parseSafeInt(errorStripeMinHeightField.getText(), s.getErrorStripeMarkMinHeight()));
        s.setAutoreparseDelay(parseSafeInt(autoreparseDelayField.getText(), s.getAutoreparseDelay()));
        if (nextErrorCombo.getValue() != null) {
            s.setNextErrorActionGoesThrough(nextErrorCombo.getValue());
        }
        s.setSuppressWithSuppressWarnings(suppressWarningsCheck.isSelected());
        s.setTooltipDelay(parseSafeInt(tooltipDelayField.getText(), s.getTooltipDelay()));

        s.save();
    }

    public void reset() {
        loadFromSettings(CodeEditingSettings.getInstance());
    }

    private int parseSafeInt(String text, int defaultVal) {
        if (text == null || text.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    // Component Getters for testing and programmatic access
    public CheckBox getMatchedBraceCheck() { return matchedBraceCheck; }
    public CheckBox getCurrentScopeCheck() { return currentScopeCheck; }
    public CheckBox getUsagesAtCaretCheck() { return usagesAtCaretCheck; }
    public CheckBox getShowQuickDocHoverCheck() { return showQuickDocHoverCheck; }
    public RadioButton getInEditorRadio() { return inEditorRadio; }
    public RadioButton getInModalRadio() { return inModalRadio; }
    public CheckBox getPreselectRenameCheck() { return preselectRenameCheck; }
    public CheckBox getShowInlineDialogCheck() { return showInlineDialogCheck; }
    public TextField getErrorStripeMinHeightField() { return errorStripeMinHeightField; }
    public TextField getAutoreparseDelayField() { return autoreparseDelayField; }
    public ComboBox<String> getNextErrorCombo() { return nextErrorCombo; }
    public CheckBox getSuppressWarningsCheck() { return suppressWarningsCheck; }
    public TextField getTooltipDelayField() { return tooltipDelayField; }
}