package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import dev.lumina.settings.SmartKeysSettings.ReformatOnPaste;
import dev.lumina.settings.SmartKeysSettings.UnindentOnBackspace;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

/**
 * Editor > General > Smart Keys settings page.
 * Full dynamic configuration matching the modern IDE design.
 */
public class SettingsSmartKeysPage extends VBox {

    // --- General Smart Keys ---
    private final CheckBox homeMovesCaretCheck = new CheckBox("Home moves caret to first non-whitespace character");
    private final CheckBox endOnBlankLineCheck = new CheckBox("End on blank line moves caret to indent position");
    private final CheckBox insertPairedBracketsCheck = new CheckBox("Insert paired brackets (), [], {}, <>");
    private final CheckBox insertPairQuoteCheck = new CheckBox("Insert pair quote");
    private final CheckBox reformatBlockCheck = new CheckBox("Reformat block on typing '}'");
    private final CheckBox useCamelHumpsCheck = new CheckBox("Use \"CamelHumps\" words");
    private final CheckBox honorCamelHumpsCheck = new CheckBox("Honor \"CamelHumps\" words settings when selecting on double click");
    private final CheckBox surroundSelectionCheck = new CheckBox("Surround selection on typing quote or brace");
    private final CheckBox multipleCaretsCheck = new CheckBox("Add multiple carets on double Ctrl with arrow keys");
    private final CheckBox jumpOutsideBracketCheck = new CheckBox("Jump outside closing bracket/quote with Tab when typing");

    // --- Enter ---
    private final CheckBox smartIndentCheck = new CheckBox("Smart Indent");
    private final CheckBox insertPairRBraceCheck = new CheckBox("Insert pair '}'");
    private final CheckBox closeBlockCommentCheck = new CheckBox("Close block comment");
    private final CheckBox insertDocCommentCheck = new CheckBox("Insert documentation comment stub");

    // --- Backspace & Paste ---
    private final ComboBox<UnindentOnBackspace> unindentOnBackspaceCombo = new ComboBox<>();
    private final ComboBox<ReformatOnPaste> reformatOnPasteCombo = new ComboBox<>();
    private final CheckBox reformatRemoveBreaksCheck = new CheckBox("Reformat again to remove custom line breaks");

    // --- JavaDoc ---
    private final CheckBox autoInsertClosingTagCheck = new CheckBox("Automatically insert closing tag in JavaDoc");

    // --- JSP ---
    private final CheckBox insertPairPercentCheck = new CheckBox("Insert pair %> on Enter in JSP");

    // --- Kotlin ---
    private final CheckBox convertPastedJavaCheck = new CheckBox("Convert pasted Java code to Kotlin");
    private final CheckBox dontShowConversionDialogCheck = new CheckBox("Don't show Java to Kotlin conversion dialog on paste");
    private final CheckBox autoAddValCheck = new CheckBox("Auto add 'val' keyword to data/value class constructor parameters");

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsSmartKeysPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 28, 24));
        setSpacing(10);

        buildUi();
        setupListeners();
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 0, 4, 0));

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(lbl, line);
        return header;
    }

    private void buildUi() {
        // Style all checkboxes
        styleCheckBox(homeMovesCaretCheck);
        styleCheckBox(endOnBlankLineCheck);
        styleCheckBox(insertPairedBracketsCheck);
        styleCheckBox(insertPairQuoteCheck);
        styleCheckBox(reformatBlockCheck);
        styleCheckBox(useCamelHumpsCheck);
        styleCheckBox(honorCamelHumpsCheck);
        styleCheckBox(surroundSelectionCheck);
        styleCheckBox(multipleCaretsCheck);
        styleCheckBox(jumpOutsideBracketCheck);

        styleCheckBox(smartIndentCheck);
        styleCheckBox(insertPairRBraceCheck);
        styleCheckBox(closeBlockCommentCheck);
        styleCheckBox(insertDocCommentCheck);

        styleCheckBox(reformatRemoveBreaksCheck);
        styleCheckBox(autoInsertClosingTagCheck);
        styleCheckBox(insertPairPercentCheck);

        styleCheckBox(convertPastedJavaCheck);
        styleCheckBox(dontShowConversionDialogCheck);
        dontShowConversionDialogCheck.setPadding(new Insets(0, 0, 0, 20));
        dontShowConversionDialogCheck.disableProperty().bind(convertPastedJavaCheck.selectedProperty().not());
        styleCheckBox(autoAddValCheck);

        // General Smart Keys list
        VBox generalGroup = new VBox(8,
                homeMovesCaretCheck,
                endOnBlankLineCheck,
                insertPairedBracketsCheck,
                insertPairQuoteCheck,
                reformatBlockCheck,
                useCamelHumpsCheck,
                honorCamelHumpsCheck,
                surroundSelectionCheck,
                multipleCaretsCheck,
                jumpOutsideBracketCheck
        );

        // Enter Section
        HBox enterHeader = createSectionHeader("Enter");
        VBox enterGroup = new VBox(8,
                smartIndentCheck,
                insertPairRBraceCheck,
                closeBlockCommentCheck,
                insertDocCommentCheck
        );

        // Backspace & Paste controls
        unindentOnBackspaceCombo.getItems().setAll(UnindentOnBackspace.values());
        unindentOnBackspaceCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(UnindentOnBackspace object) {
                return object != null ? object.getLabel() : "";
            }

            @Override
            public UnindentOnBackspace fromString(String string) {
                return UnindentOnBackspace.fromLabel(string);
            }
        });
        unindentOnBackspaceCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        unindentOnBackspaceCombo.setPrefWidth(240);

        reformatOnPasteCombo.getItems().setAll(ReformatOnPaste.values());
        reformatOnPasteCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ReformatOnPaste object) {
                return object != null ? object.getLabel() : "";
            }

            @Override
            public ReformatOnPaste fromString(String string) {
                return ReformatOnPaste.fromLabel(string);
            }
        });
        reformatOnPasteCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        reformatOnPasteCombo.setPrefWidth(240);

        GridPane dropdownGrid = new GridPane();
        dropdownGrid.setHgap(10);
        dropdownGrid.setVgap(8);
        dropdownGrid.setPadding(new Insets(4, 0, 4, 0));

        Label unindentLabel = new Label("Unindent on Backspace:");
        unindentLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        unindentLabel.setPrefWidth(160);

        Label reformatLabel = new Label("Reformat on paste:");
        reformatLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        reformatLabel.setPrefWidth(160);

        dropdownGrid.add(unindentLabel, 0, 0);
        dropdownGrid.add(unindentOnBackspaceCombo, 1, 0);
        dropdownGrid.add(reformatLabel, 0, 1);
        dropdownGrid.add(reformatOnPasteCombo, 1, 1);

        VBox pasteGroup = new VBox(8,
                dropdownGrid,
                reformatRemoveBreaksCheck
        );

        // JavaDoc Section
        HBox javadocHeader = createSectionHeader("JavaDoc");
        VBox javadocGroup = new VBox(8,
                autoInsertClosingTagCheck
        );

        // JSP Section
        VBox jspGroup = new VBox(8,
                insertPairPercentCheck
        );
        jspGroup.setPadding(new Insets(4, 0, 0, 0));

        // Kotlin Section
        HBox kotlinHeader = createSectionHeader("Kotlin");
        VBox kotlinGroup = new VBox(8,
                convertPastedJavaCheck,
                dontShowConversionDialogCheck,
                autoAddValCheck
        );

        getChildren().addAll(
                generalGroup,
                enterHeader,
                enterGroup,
                pasteGroup,
                javadocHeader,
                javadocGroup,
                jspGroup,
                kotlinHeader,
                kotlinGroup
        );
    }

    private void setupListeners() {
        homeMovesCaretCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        endOnBlankLineCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        insertPairedBracketsCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        insertPairQuoteCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        reformatBlockCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        useCamelHumpsCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        honorCamelHumpsCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        surroundSelectionCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        multipleCaretsCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        jumpOutsideBracketCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        smartIndentCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        insertPairRBraceCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        closeBlockCommentCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        insertDocCommentCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        unindentOnBackspaceCombo.valueProperty().addListener((obs, old, val) -> notifyModified());
        reformatOnPasteCombo.valueProperty().addListener((obs, old, val) -> notifyModified());
        reformatRemoveBreaksCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        autoInsertClosingTagCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        insertPairPercentCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());

        convertPastedJavaCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        dontShowConversionDialogCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
        autoAddValCheck.selectedProperty().addListener((obs, old, val) -> notifyModified());
    }

    public void loadFromSettings(SmartKeysSettings settings) {
        suppressEvents = true;
        try {
            homeMovesCaretCheck.setSelected(settings.isHomeMovesCaretToFirstNonWhitespace());
            endOnBlankLineCheck.setSelected(settings.isEndOnBlankLineMovesCaretToIndent());
            insertPairedBracketsCheck.setSelected(settings.isInsertPairedBrackets());
            insertPairQuoteCheck.setSelected(settings.isInsertPairQuote());
            reformatBlockCheck.setSelected(settings.isReformatBlockOnTypingRBrace());
            useCamelHumpsCheck.setSelected(settings.isUseCamelHumpsWords());
            honorCamelHumpsCheck.setSelected(settings.isHonorCamelHumpsOnDoubleClick());
            surroundSelectionCheck.setSelected(settings.isSurroundSelectionOnQuoteOrBrace());
            multipleCaretsCheck.setSelected(settings.isAddMultipleCaretsOnDoubleCtrlArrow());
            jumpOutsideBracketCheck.setSelected(settings.isJumpOutsideClosingBracketOrQuoteWithTab());

            smartIndentCheck.setSelected(settings.isSmartIndent());
            insertPairRBraceCheck.setSelected(settings.isInsertPairRBrace());
            closeBlockCommentCheck.setSelected(settings.isCloseBlockComment());
            insertDocCommentCheck.setSelected(settings.isInsertDocCommentStub());

            unindentOnBackspaceCombo.setValue(settings.getUnindentOnBackspace());
            reformatOnPasteCombo.setValue(settings.getReformatOnPaste());
            reformatRemoveBreaksCheck.setSelected(settings.isReformatAgainToRemoveCustomLineBreaks());

            autoInsertClosingTagCheck.setSelected(settings.isAutoInsertClosingTagInJavaDoc());
            insertPairPercentCheck.setSelected(settings.isInsertPairPercentOnEnterInJsp());

            convertPastedJavaCheck.setSelected(settings.isConvertPastedJavaToKotlin());
            dontShowConversionDialogCheck.setSelected(settings.isDontShowJavaToKotlinDialogOnPaste());
            autoAddValCheck.setSelected(settings.isAutoAddValKeywordToConstructorParams());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        return homeMovesCaretCheck.isSelected() != s.isHomeMovesCaretToFirstNonWhitespace()
                || endOnBlankLineCheck.isSelected() != s.isEndOnBlankLineMovesCaretToIndent()
                || insertPairedBracketsCheck.isSelected() != s.isInsertPairedBrackets()
                || insertPairQuoteCheck.isSelected() != s.isInsertPairQuote()
                || reformatBlockCheck.isSelected() != s.isReformatBlockOnTypingRBrace()
                || useCamelHumpsCheck.isSelected() != s.isUseCamelHumpsWords()
                || honorCamelHumpsCheck.isSelected() != s.isHonorCamelHumpsOnDoubleClick()
                || surroundSelectionCheck.isSelected() != s.isSurroundSelectionOnQuoteOrBrace()
                || multipleCaretsCheck.isSelected() != s.isAddMultipleCaretsOnDoubleCtrlArrow()
                || jumpOutsideBracketCheck.isSelected() != s.isJumpOutsideClosingBracketOrQuoteWithTab()
                || smartIndentCheck.isSelected() != s.isSmartIndent()
                || insertPairRBraceCheck.isSelected() != s.isInsertPairRBrace()
                || closeBlockCommentCheck.isSelected() != s.isCloseBlockComment()
                || insertDocCommentCheck.isSelected() != s.isInsertDocCommentStub()
                || unindentOnBackspaceCombo.getValue() != s.getUnindentOnBackspace()
                || reformatOnPasteCombo.getValue() != s.getReformatOnPaste()
                || reformatRemoveBreaksCheck.isSelected() != s.isReformatAgainToRemoveCustomLineBreaks()
                || autoInsertClosingTagCheck.isSelected() != s.isAutoInsertClosingTagInJavaDoc()
                || insertPairPercentCheck.isSelected() != s.isInsertPairPercentOnEnterInJsp()
                || convertPastedJavaCheck.isSelected() != s.isConvertPastedJavaToKotlin()
                || dontShowConversionDialogCheck.isSelected() != s.isDontShowJavaToKotlinDialogOnPaste()
                || autoAddValCheck.isSelected() != s.isAutoAddValKeywordToConstructorParams();
    }

    public void apply() {
        SmartKeysSettings s = SmartKeysSettings.getInstance();
        s.setHomeMovesCaretToFirstNonWhitespace(homeMovesCaretCheck.isSelected());
        s.setEndOnBlankLineMovesCaretToIndent(endOnBlankLineCheck.isSelected());
        s.setInsertPairedBrackets(insertPairedBracketsCheck.isSelected());
        s.setInsertPairQuote(insertPairQuoteCheck.isSelected());
        s.setReformatBlockOnTypingRBrace(reformatBlockCheck.isSelected());
        s.setUseCamelHumpsWords(useCamelHumpsCheck.isSelected());
        s.setHonorCamelHumpsOnDoubleClick(honorCamelHumpsCheck.isSelected());
        s.setSurroundSelectionOnQuoteOrBrace(surroundSelectionCheck.isSelected());
        s.setAddMultipleCaretsOnDoubleCtrlArrow(multipleCaretsCheck.isSelected());
        s.setJumpOutsideClosingBracketOrQuoteWithTab(jumpOutsideBracketCheck.isSelected());

        s.setSmartIndent(smartIndentCheck.isSelected());
        s.setInsertPairRBrace(insertPairRBraceCheck.isSelected());
        s.setCloseBlockComment(closeBlockCommentCheck.isSelected());
        s.setInsertDocCommentStub(insertDocCommentCheck.isSelected());

        s.setUnindentOnBackspace(unindentOnBackspaceCombo.getValue());
        s.setReformatOnPaste(reformatOnPasteCombo.getValue());
        s.setReformatAgainToRemoveCustomLineBreaks(reformatRemoveBreaksCheck.isSelected());

        s.setAutoInsertClosingTagInJavaDoc(autoInsertClosingTagCheck.isSelected());
        s.setInsertPairPercentOnEnterInJsp(insertPairPercentCheck.isSelected());

        s.setConvertPastedJavaToKotlin(convertPastedJavaCheck.isSelected());
        s.setDontShowJavaToKotlinDialogOnPaste(dontShowConversionDialogCheck.isSelected());
        s.setAutoAddValKeywordToConstructorParams(autoAddValCheck.isSelected());

        s.save();
    }

    public void reset() {
        loadFromSettings(SmartKeysSettings.getInstance());
    }

    // --- Component Getters for Tests ---

    public CheckBox getHomeMovesCaretCheck() { return homeMovesCaretCheck; }
    public CheckBox getEndOnBlankLineCheck() { return endOnBlankLineCheck; }
    public CheckBox getInsertPairedBracketsCheck() { return insertPairedBracketsCheck; }
    public CheckBox getInsertPairQuoteCheck() { return insertPairQuoteCheck; }
    public CheckBox getReformatBlockCheck() { return reformatBlockCheck; }
    public CheckBox getUseCamelHumpsCheck() { return useCamelHumpsCheck; }
    public CheckBox getHonorCamelHumpsCheck() { return honorCamelHumpsCheck; }
    public CheckBox getSurroundSelectionCheck() { return surroundSelectionCheck; }
    public CheckBox getMultipleCaretsCheck() { return multipleCaretsCheck; }
    public CheckBox getJumpOutsideBracketCheck() { return jumpOutsideBracketCheck; }

    public CheckBox getSmartIndentCheck() { return smartIndentCheck; }
    public CheckBox getInsertPairRBraceCheck() { return insertPairRBraceCheck; }
    public CheckBox getCloseBlockCommentCheck() { return closeBlockCommentCheck; }
    public CheckBox getInsertDocCommentCheck() { return insertDocCommentCheck; }

    public ComboBox<UnindentOnBackspace> getUnindentOnBackspaceCombo() { return unindentOnBackspaceCombo; }
    public ComboBox<ReformatOnPaste> getReformatOnPasteCombo() { return reformatOnPasteCombo; }
    public CheckBox getReformatRemoveBreaksCheck() { return reformatRemoveBreaksCheck; }

    public CheckBox getAutoInsertClosingTagCheck() { return autoInsertClosingTagCheck; }
    public CheckBox getInsertPairPercentCheck() { return insertPairPercentCheck; }

    public CheckBox getConvertPastedJavaCheck() { return convertPastedJavaCheck; }
    public CheckBox getDontShowConversionDialogCheck() { return dontShowConversionDialogCheck; }
    public CheckBox getAutoAddValCheck() { return autoAddValCheck; }
}