package dev.lumina.ui;

import dev.lumina.settings.CodeCompletionSettings;
import dev.lumina.settings.CodeCompletionSettings.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;

import java.util.*;

/**
 * Pixel-perfect IntelliJ IDEA-style Editor > General > Code Completion settings page.
 * Completely dynamic, backed by CodeCompletionSettings, with live real-time synchronization,
 * section separators, dropdown scopes, editable table aliases, and dirty tracking.
 */
public class SettingsCodeCompletionPage extends VBox {

    // 1. Match case & basic options
    private final CheckBox matchCaseCheck = new CheckBox("Match case:");
    private final RadioButton firstLetterRadio = new RadioButton("First letter only");
    private final RadioButton allLettersRadio = new RadioButton("All letters");
    private final ToggleGroup matchCaseGroup = new ToggleGroup();

    private final CheckBox autoInsertBasicCheck = new CheckBox("Basic Completion");
    private final CheckBox autoInsertTypeMatchingCheck = new CheckBox("Type-Matching Completion");
    private final CheckBox sortAlphabeticallyCheck = new CheckBox("Sort suggestions alphabetically");
    private final CheckBox showSuggestionsAsYouTypeCheck = new CheckBox("Show suggestions as you type");
    private final CheckBox insertBySpaceOrDotCheck = new CheckBox("Insert selected suggestion by pressing space, dot, or other context-dependent keys");
    private final CheckBox showDocPopupCheck = new CheckBox("Show the documentation popup in");
    private final TextField docPopupField = new TextField("500");
    private final CheckBox insertParenthesesCheck = new CheckBox("Insert parentheses automatically when applicable");
    private final Hyperlink configureExcludedClassesLink = new Hyperlink("Configure classes excluded from completion");

    // 2. Command completion
    private final CheckBox commandCompletionCheck = new CheckBox("Enable command completion");
    private final CheckBox commandSeparateGroupCheck = new CheckBox("Show command completion as a separate group");
    private final CheckBox commandReadOnlyCheck = new CheckBox("Enable command completion for read-only files");

    // 3. Machine learning-assisted completion
    private final Hyperlink inlineCompletionLink = new Hyperlink("Inline Completion settings page");
    private final CheckBox mlSortSuggestionsCheck = new CheckBox("Sort completion suggestions based on machine learning");
    private final Map<String, CheckBox> mlLanguageChecks = new LinkedHashMap<>();
    private final CheckBox markPositionChangesCheck = new CheckBox("Mark position changes in the completion popup");
    private final CheckBox markRelevantItemCheck = new CheckBox("Mark the most relevant item in the completion popup");

    // 4. HTML
    private final CheckBox htmlAutoPopupTagNameCheck = new CheckBox("Enable auto-popup of tag name code completion when typing in HTML text");

    // 5. Python
    private final CheckBox pythonSuggestImportableCheck = new CheckBox("Suggest importable classes, functions and variables in basic completion");

    // 6. JavaScript
    private final CheckBox jsOnlyTypeBasedCheck = new CheckBox("Only type-based completion");
    private final CheckBox jsSuggestOptionalChainingCheck = new CheckBox("Suggest items with optional chaining for nullable types");
    private final CheckBox jsExpandMethodBodiesCheck = new CheckBox("Expand method bodies in completion for overrides");
    private final CheckBox jsSuggestVariableParameterNamesCheck = new CheckBox("Suggest variable and parameter names");
    private final CheckBox jsSuggestClassFieldsCheck = new CheckBox("Suggest names for class fields");
    private final CheckBox jsAddTypeAnnotationsCheck = new CheckBox("Add type annotations for suggested parameter names");

    // 7. Parameter info
    private final CheckBox paramShowHintsCheck = new CheckBox("Show parameter name hints on completion");
    private final CheckBox paramShowPopupCheck = new CheckBox("Show the parameter info popup in");
    private final TextField paramPopupField = new TextField("1000");
    private final CheckBox paramShowFullSignaturesCheck = new CheckBox("Show full method signatures");

    // 8. Rust
    private final CheckBox rustSuggestOutOfScopeCheck = new CheckBox("Suggest out-of-scope items");
    private final CheckBox rustHighlightMoveErrorsCheck = new CheckBox("Highlight move errors in completion list");

    // 9. Ruby
    private final CheckBox rubyMatchAcrossNamespacesCheck = new CheckBox("Match suggestions across namespaces");
    private final CheckBox rubySuggestMethodsAfterColonColonCheck = new CheckBox("Suggest methods after '::'");
    private final CheckBox rubyPreselectFirstInEditorsCheck = new CheckBox("Editors");
    private final CheckBox rubyPreselectFirstInConsolesCheck = new CheckBox("Consoles");

    // 10. SQL
    private final RadioButton sqlScopeSearchPathRadio = new RadioButton("The current search path only");
    private final RadioButton sqlScopeCurrentScopeRadio = new RadioButton("The current scope");
    private final RadioButton sqlScopeAllSchemasRadio = new RadioButton("All available schemas");
    private final ToggleGroup sqlScopeGroup = new ToggleGroup();

    private final ComboBox<SqlQualifyOption> sqlQualifyDatabaseCombo = new ComboBox<>();
    private final ComboBox<SqlQualifyOption> sqlQualifySchemaCombo = new ComboBox<>();
    private final ComboBox<SqlQualifyOption> sqlQualifyTableViewCombo = new ComboBox<>();
    private final ComboBox<SqlQualifyOption> sqlQualifyTableViewAliasCombo = new ComboBox<>();

    private final ComboBox<SqlQualifyOption> sqlQualifyInBasicCombo = new ComboBox<>();
    private final ComboBox<SqlQualifyOption> sqlQualifyInJoinCombo = new ComboBox<>();
    private final ComboBox<SqlQualifyOption> sqlQualifyInRefactoringCombo = new ComboBox<>();
    private final ComboBox<SqlQualifyOption> sqlQualifyInLiveTemplatesCombo = new ComboBox<>();
    private final ComboBox<SqlQualifyOption> sqlQualifyInDragDropCombo = new ComboBox<>();

    private final CheckBox sqlJoinUseAliasesCheck = new CheckBox("Use aliases in completion for JOIN");
    private final CheckBox sqlJoinInvertOperandsCheck = new CheckBox("Invert order of operands in auto-generated ON clause");
    private final CheckBox sqlJoinSuggestNonStrictFkCheck = new CheckBox("Suggest non-strict foreign keys based on the name matching");

    private final CheckBox sqlTableAliasAutoAddCheck = new CheckBox("Automatically add aliases when completing table names");
    private final CheckBox sqlTableAliasSuggestCheck = new CheckBox("Suggest alias names in completion after table names");

    // Table alias table
    public static class TableAliasRow {
        private final SimpleStringProperty tableName;
        private final SimpleStringProperty customAlias;

        public TableAliasRow(String table, String alias) {
            this.tableName = new SimpleStringProperty(table != null ? table : "");
            this.customAlias = new SimpleStringProperty(alias != null ? alias : "");
        }

        public String getTableName() { return tableName.get(); }
        public void setTableName(String val) { tableName.set(val != null ? val : ""); }
        public SimpleStringProperty tableNameProperty() { return tableName; }

        public String getCustomAlias() { return customAlias.get(); }
        public void setCustomAlias(String val) { customAlias.set(val != null ? val : ""); }
        public SimpleStringProperty customAliasProperty() { return customAlias; }
    }

    private final ObservableList<TableAliasRow> tableAliasData = FXCollections.observableArrayList();
    private final TableView<TableAliasRow> tableAliasView = new TableView<>(tableAliasData);
    private final TextField additionalCharsField = new TextField();

    // Event & navigation callbacks
    private Runnable onModifiedListener;
    private Runnable onNavigateInlineCompletion;
    private Runnable onConfigureExcludedClasses;
    private boolean suppressEvents = false;

    public SettingsCodeCompletionPage() {
        this(null, null);
    }

    public SettingsCodeCompletionPage(Runnable onNavigateInlineCompletion, Runnable onConfigureExcludedClasses) {
        this.onNavigateInlineCompletion = onNavigateInlineCompletion;
        this.onConfigureExcludedClasses = onConfigureExcludedClasses;

        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 28, 24));
        setSpacing(6);

        buildUi();
        setupListeners();
        loadFromSettings(CodeCompletionSettings.getInstance());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnNavigateInlineCompletion(Runnable callback) {
        this.onNavigateInlineCompletion = callback;
    }

    public void setOnConfigureExcludedClasses(Runnable callback) {
        this.onConfigureExcludedClasses = callback;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void styleRadio(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private Node buildSectionSeparator(String title) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(16, 0, 4, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1px; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(label, line);
        return box;
    }

    private Node buildHelpIcon(String tooltipText) {
        Label icon = new Label("?");
        icon.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 10px; -fx-border-color: #55575E; -fx-border-radius: 8; -fx-background-radius: 8; -fx-min-width: 14px; -fx-min-height: 14px; -fx-max-width: 14px; -fx-max-height: 14px; -fx-alignment: center; -fx-cursor: hand;");
        if (tooltipText != null) {
            Tooltip.install(icon, new Tooltip(tooltipText));
        }
        return icon;
    }

    private void styleComboBox(ComboBox<SqlQualifyOption> combo) {
        combo.getItems().setAll(SqlQualifyOption.values());
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(SqlQualifyOption o) { return o != null ? o.getLabel() : ""; }
            @Override public SqlQualifyOption fromString(String s) { return SqlQualifyOption.fromLabel(s); }
        });
        combo.setPrefWidth(125);
        combo.setPrefHeight(25);
        combo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void buildUi() {
        // 1. Match case row
        styleCheckBox(matchCaseCheck);
        styleRadio(firstLetterRadio);
        styleRadio(allLettersRadio);
        firstLetterRadio.setToggleGroup(matchCaseGroup);
        allLettersRadio.setToggleGroup(matchCaseGroup);

        HBox matchCaseRow = new HBox(14, matchCaseCheck, firstLetterRadio, allLettersRadio);
        matchCaseRow.setAlignment(Pos.CENTER_LEFT);

        matchCaseCheck.selectedProperty().addListener((obs, o, n) -> {
            firstLetterRadio.setDisable(!n);
            allLettersRadio.setDisable(!n);
        });

        // 2. Automatically insert single suggestions
        Label autoInsertLabel = new Label("Automatically insert single suggestions for:");
        autoInsertLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        VBox.setMargin(autoInsertLabel, new Insets(6, 0, 0, 0));

        styleCheckBox(autoInsertBasicCheck);
        Label basicKeyLabel = new Label("Ctrl+Space");
        basicKeyLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");
        HBox basicBox = new HBox(8, autoInsertBasicCheck, basicKeyLabel);
        basicBox.setAlignment(Pos.CENTER_LEFT);
        basicBox.setPadding(new Insets(0, 0, 0, 18));

        styleCheckBox(autoInsertTypeMatchingCheck);
        Label typeKeyLabel = new Label("Ctrl+Shift+Space");
        typeKeyLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");
        HBox typeBox = new HBox(8, autoInsertTypeMatchingCheck, typeKeyLabel);
        typeBox.setAlignment(Pos.CENTER_LEFT);
        typeBox.setPadding(new Insets(0, 0, 0, 18));

        styleCheckBox(sortAlphabeticallyCheck);
        styleCheckBox(showSuggestionsAsYouTypeCheck);

        styleCheckBox(insertBySpaceOrDotCheck);
        HBox insertBySpaceBox = new HBox(insertBySpaceOrDotCheck);
        insertBySpaceBox.setPadding(new Insets(0, 0, 0, 18));
        showSuggestionsAsYouTypeCheck.selectedProperty().addListener((obs, o, n) -> insertBySpaceOrDotCheck.setDisable(!n));

        // Documentation popup row
        styleCheckBox(showDocPopupCheck);
        docPopupField.setPrefWidth(55);
        docPopupField.setMaxWidth(65);
        docPopupField.setPrefHeight(24);
        docPopupField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 2 6 2 6;");

        Label msLabel = new Label("ms");
        msLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox docRow = new HBox(6, showDocPopupCheck, docPopupField, msLabel);
        docRow.setAlignment(Pos.CENTER_LEFT);
        showDocPopupCheck.selectedProperty().addListener((obs, o, n) -> docPopupField.setDisable(!n));

        styleCheckBox(insertParenthesesCheck);

        configureExcludedClassesLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 2 0 4 0; -fx-border-color: transparent; -fx-underline: false;");
        configureExcludedClassesLink.setOnMouseEntered(e -> configureExcludedClassesLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-padding: 2 0 4 0; -fx-border-color: transparent; -fx-underline: true;"));
        configureExcludedClassesLink.setOnMouseExited(e -> configureExcludedClassesLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 2 0 4 0; -fx-border-color: transparent; -fx-underline: false;"));
        configureExcludedClassesLink.setOnAction(e -> {
            if (onConfigureExcludedClasses != null) onConfigureExcludedClasses.run();
        });

        // 3. Command Completion
        Node commandSep = buildSectionSeparator("Command Completion");
        styleCheckBox(commandCompletionCheck);
        Node cmdHelp = buildHelpIcon("Complete commands, flags, and options in supported contexts");
        HBox cmdHeader = new HBox(6, commandCompletionCheck, cmdHelp);
        cmdHeader.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(commandSeparateGroupCheck);
        HBox cmdSepBox = new HBox(commandSeparateGroupCheck);
        cmdSepBox.setPadding(new Insets(0, 0, 0, 18));

        styleCheckBox(commandReadOnlyCheck);
        Label betaBadge = new Label("Beta");
        betaBadge.setStyle("-fx-background-color: #6C5CE7; -fx-text-fill: #FFFFFF; -fx-font-size: 10px; -fx-padding: 1 5 1 5; -fx-background-radius: 4;");
        HBox cmdRoBox = new HBox(6, commandReadOnlyCheck, betaBadge);
        cmdRoBox.setAlignment(Pos.CENTER_LEFT);
        cmdRoBox.setPadding(new Insets(0, 0, 0, 18));

        commandCompletionCheck.selectedProperty().addListener((obs, o, n) -> {
            commandSeparateGroupCheck.setDisable(!n);
            commandReadOnlyCheck.setDisable(!n);
        });

        // 4. Machine Learning-Assisted Completion
        Node mlSep = buildSectionSeparator("Machine Learning-Assisted Completion");

        Label mlPrefix = new Label("Go to");
        mlPrefix.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        inlineCompletionLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
        inlineCompletionLink.setOnMouseEntered(e -> inlineCompletionLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: true;"));
        inlineCompletionLink.setOnMouseExited(e -> inlineCompletionLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;"));
        inlineCompletionLink.setOnAction(e -> {
            if (onNavigateInlineCompletion != null) onNavigateInlineCompletion.run();
        });
        Label mlSuffix = new Label("to adjust inline completion (e.g. Full Line Code Completion) settings");
        mlSuffix.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        HBox inlineHintBox = new HBox(4, mlPrefix, inlineCompletionLink, mlSuffix);
        inlineHintBox.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(mlSortSuggestionsCheck);
        Node mlHelp = buildHelpIcon("Use machine learning models to rank completion options");
        HBox mlSortRow = new HBox(6, mlSortSuggestionsCheck, mlHelp);
        mlSortRow.setAlignment(Pos.CENTER_LEFT);

        // ML language checkboxes (vertical list with 18px indent as in screenshot)
        VBox mlLangsBox = new VBox(4);
        mlLangsBox.setPadding(new Insets(2, 0, 2, 18));
        for (String lang : CodeCompletionSettings.ML_LANGUAGES) {
            CheckBox cb = new CheckBox(lang);
            styleCheckBox(cb);
            mlLanguageChecks.put(lang, cb);
            mlLangsBox.getChildren().add(cb);
        }

        styleCheckBox(markPositionChangesCheck);
        Label arrowIcon = new Label("↑↓");
        arrowIcon.setStyle("-fx-text-fill: #59A869; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox markPosBox = new HBox(4, markPositionChangesCheck, arrowIcon);
        markPosBox.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(markRelevantItemCheck);
        Label starIcon = new Label("★");
        starIcon.setStyle("-fx-text-fill: #EDA200; -fx-font-size: 12px;");
        HBox markRelBox = new HBox(4, markRelevantItemCheck, starIcon);
        markRelBox.setAlignment(Pos.CENTER_LEFT);

        mlSortSuggestionsCheck.selectedProperty().addListener((obs, o, n) -> {
            for (CheckBox cb : mlLanguageChecks.values()) {
                cb.setDisable(!n);
            }
            markPositionChangesCheck.setDisable(!n);
            markRelevantItemCheck.setDisable(!n);
        });

        // 5. HTML
        Node htmlSep = buildSectionSeparator("HTML");
        styleCheckBox(htmlAutoPopupTagNameCheck);

        // 6. Python
        Node pythonSep = buildSectionSeparator("Python");
        styleCheckBox(pythonSuggestImportableCheck);
        Node pyHelp = buildHelpIcon("Suggest classes, functions and variables that are not yet imported");
        HBox pyRow = new HBox(6, pythonSuggestImportableCheck, pyHelp);
        pyRow.setAlignment(Pos.CENTER_LEFT);

        // 7. JavaScript
        Node jsSep = buildSectionSeparator("JavaScript");
        styleCheckBox(jsOnlyTypeBasedCheck);
        Label jsTypeHint = new Label("Show fewer completion suggestions based on type information. May significantly improve performance.");
        jsTypeHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        jsTypeHint.setPadding(new Insets(0, 0, 2, 18));

        styleCheckBox(jsSuggestOptionalChainingCheck);
        styleCheckBox(jsExpandMethodBodiesCheck);

        Label jsNamesLabel = new Label("Completion of names");
        jsNamesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        VBox.setMargin(jsNamesLabel, new Insets(4, 0, 0, 0));

        styleCheckBox(jsSuggestVariableParameterNamesCheck);
        styleCheckBox(jsSuggestClassFieldsCheck);
        styleCheckBox(jsAddTypeAnnotationsCheck);
        VBox jsNamesBox = new VBox(4, jsSuggestVariableParameterNamesCheck, jsSuggestClassFieldsCheck, jsAddTypeAnnotationsCheck);
        jsNamesBox.setPadding(new Insets(0, 0, 2, 18));

        // 8. Parameter Info
        Node paramSep = buildSectionSeparator("Parameter Info");
        styleCheckBox(paramShowHintsCheck);

        styleCheckBox(paramShowPopupCheck);
        paramPopupField.setPrefWidth(55);
        paramPopupField.setMaxWidth(65);
        paramPopupField.setPrefHeight(24);
        paramPopupField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 2 6 2 6;");
        Label paramMs = new Label("ms");
        paramMs.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox paramPopupRow = new HBox(6, paramShowPopupCheck, paramPopupField, paramMs);
        paramPopupRow.setAlignment(Pos.CENTER_LEFT);

        paramShowPopupCheck.selectedProperty().addListener((obs, o, n) -> paramPopupField.setDisable(!n));
        styleCheckBox(paramShowFullSignaturesCheck);

        // 9. Rust
        Node rustSep = buildSectionSeparator("Rust");
        styleCheckBox(rustSuggestOutOfScopeCheck);
        styleCheckBox(rustHighlightMoveErrorsCheck);

        // 10. Ruby
        Node rubySep = buildSectionSeparator("Ruby");
        styleCheckBox(rubyMatchAcrossNamespacesCheck);
        Label rubyHint = new Label("Allows any part of a qualified name to match the completion prefix");
        rubyHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        rubyHint.setPadding(new Insets(0, 0, 2, 18));

        styleCheckBox(rubySuggestMethodsAfterColonColonCheck);

        Label rubyPreselectLabel = new Label("Preselect the first suggestion as you type in:");
        rubyPreselectLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        VBox.setMargin(rubyPreselectLabel, new Insets(4, 0, 0, 0));

        styleCheckBox(rubyPreselectFirstInEditorsCheck);
        styleCheckBox(rubyPreselectFirstInConsolesCheck);
        VBox rubyPreselectBox = new VBox(4, rubyPreselectFirstInEditorsCheck, rubyPreselectFirstInConsolesCheck);
        rubyPreselectBox.setPadding(new Insets(0, 0, 2, 18));

        // 11. SQL
        Node sqlSep = buildSectionSeparator("SQL");

        Label sqlSuggestLabel = new Label("Suggest objects from:");
        sqlSuggestLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        styleRadio(sqlScopeSearchPathRadio);
        styleRadio(sqlScopeCurrentScopeRadio);
        styleRadio(sqlScopeAllSchemasRadio);
        sqlScopeSearchPathRadio.setToggleGroup(sqlScopeGroup);
        sqlScopeCurrentScopeRadio.setToggleGroup(sqlScopeGroup);
        sqlScopeAllSchemasRadio.setToggleGroup(sqlScopeGroup);

        VBox sqlScopeBox = new VBox(4, sqlScopeSearchPathRadio, sqlScopeCurrentScopeRadio, sqlScopeAllSchemasRadio);
        sqlScopeBox.setPadding(new Insets(0, 0, 4, 18));

        // Qualify object with
        Label qualifyWithLabel = new Label("Qualify object with:");
        qualifyWithLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        VBox.setMargin(qualifyWithLabel, new Insets(4, 0, 0, 0));

        GridPane qualifyWithGrid = new GridPane();
        qualifyWithGrid.setHgap(14);
        qualifyWithGrid.setVgap(6);
        qualifyWithGrid.setPadding(new Insets(2, 0, 4, 18));

        styleComboBox(sqlQualifyDatabaseCombo);
        styleComboBox(sqlQualifySchemaCombo);
        styleComboBox(sqlQualifyTableViewCombo);
        styleComboBox(sqlQualifyTableViewAliasCombo);

        Label dbLabel = new Label("Database:"); dbLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Label scLabel = new Label("Schema:"); scLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Label tvLabel = new Label("Table/View:"); tvLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Label taLabel = new Label("Table/view alias:"); taLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        qualifyWithGrid.add(dbLabel, 0, 0); qualifyWithGrid.add(sqlQualifyDatabaseCombo, 1, 0);
        qualifyWithGrid.add(scLabel, 0, 1); qualifyWithGrid.add(sqlQualifySchemaCombo, 1, 1);
        qualifyWithGrid.add(tvLabel, 0, 2); qualifyWithGrid.add(sqlQualifyTableViewCombo, 1, 2);
        qualifyWithGrid.add(taLabel, 0, 3); qualifyWithGrid.add(sqlQualifyTableViewAliasCombo, 1, 3);

        // Qualify object in
        Label qualifyInLabel = new Label("Qualify object in:");
        qualifyInLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        VBox.setMargin(qualifyInLabel, new Insets(4, 0, 0, 0));

        GridPane qualifyInGrid = new GridPane();
        qualifyInGrid.setHgap(14);
        qualifyInGrid.setVgap(6);
        qualifyInGrid.setPadding(new Insets(2, 0, 4, 18));

        styleComboBox(sqlQualifyInBasicCombo);
        styleComboBox(sqlQualifyInJoinCombo);
        styleComboBox(sqlQualifyInRefactoringCombo);
        styleComboBox(sqlQualifyInLiveTemplatesCombo);
        styleComboBox(sqlQualifyInDragDropCombo);

        Label qBasic = new Label("Basic completion"); qBasic.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Label qJoin = new Label("JOIN completion"); qJoin.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Label qRef = new Label("Refactoring"); qRef.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Label qLive = new Label("Live templates"); qLive.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Label qDrag = new Label("Drag-n-Drop"); qDrag.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        qualifyInGrid.add(qBasic, 0, 0); qualifyInGrid.add(sqlQualifyInBasicCombo, 1, 0);
        qualifyInGrid.add(qJoin, 0, 1); qualifyInGrid.add(sqlQualifyInJoinCombo, 1, 1);
        qualifyInGrid.add(qRef, 0, 2); qualifyInGrid.add(sqlQualifyInRefactoringCombo, 1, 2);
        qualifyInGrid.add(qLive, 0, 3); qualifyInGrid.add(sqlQualifyInLiveTemplatesCombo, 1, 3);
        qualifyInGrid.add(qDrag, 0, 4); qualifyInGrid.add(sqlQualifyInDragDropCombo, 1, 4);

        // JOIN clauses
        Label joinClausesLabel = new Label("JOIN clauses:");
        joinClausesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        VBox.setMargin(joinClausesLabel, new Insets(8, 0, 0, 0));

        styleCheckBox(sqlJoinUseAliasesCheck);
        styleCheckBox(sqlJoinInvertOperandsCheck);
        styleCheckBox(sqlJoinSuggestNonStrictFkCheck);
        VBox joinBox = new VBox(4, sqlJoinUseAliasesCheck, sqlJoinInvertOperandsCheck, sqlJoinSuggestNonStrictFkCheck);
        joinBox.setPadding(new Insets(2, 0, 4, 18));

        // Table aliases
        Label tableAliasesLabel = new Label("Table aliases:");
        tableAliasesLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        VBox.setMargin(tableAliasesLabel, new Insets(6, 0, 0, 0));

        styleCheckBox(sqlTableAliasAutoAddCheck);
        styleCheckBox(sqlTableAliasSuggestCheck);
        VBox aliasChecksBox = new VBox(4, sqlTableAliasAutoAddCheck, sqlTableAliasSuggestCheck);
        aliasChecksBox.setPadding(new Insets(2, 0, 4, 18));

        // Table aliases table view with toolbar (+ / -)
        Node aliasTableComponent = buildTableAliasesComponent();
        VBox.setMargin(aliasTableComponent, new Insets(2, 0, 8, 18));

        // Additional characters to accept completion
        Label additionalCharsLabel = new Label("Additional characters to accept completion:");
        additionalCharsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        additionalCharsField.setPrefWidth(220);
        additionalCharsField.setMaxWidth(300);
        additionalCharsField.setPrefHeight(25);
        additionalCharsField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 2 6 2 6;");

        HBox additionalCharsBox = new HBox(10, additionalCharsLabel, additionalCharsField);
        additionalCharsBox.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(additionalCharsBox, new Insets(8, 0, 8, 0));

        // Assemble all
        getChildren().addAll(
                matchCaseRow,
                autoInsertLabel,
                basicBox,
                typeBox,
                sortAlphabeticallyCheck,
                showSuggestionsAsYouTypeCheck,
                insertBySpaceBox,
                docRow,
                insertParenthesesCheck,
                configureExcludedClassesLink,
                commandSep,
                cmdHeader,
                cmdSepBox,
                cmdRoBox,
                mlSep,
                inlineHintBox,
                mlSortRow,
                mlLangsBox,
                markPosBox,
                markRelBox,
                htmlSep,
                htmlAutoPopupTagNameCheck,
                pythonSep,
                pyRow,
                jsSep,
                jsOnlyTypeBasedCheck,
                jsTypeHint,
                jsSuggestOptionalChainingCheck,
                jsExpandMethodBodiesCheck,
                jsNamesLabel,
                jsNamesBox,
                paramSep,
                paramShowHintsCheck,
                paramPopupRow,
                paramShowFullSignaturesCheck,
                rustSep,
                rustSuggestOutOfScopeCheck,
                rustHighlightMoveErrorsCheck,
                rubySep,
                rubyMatchAcrossNamespacesCheck,
                rubyHint,
                rubySuggestMethodsAfterColonColonCheck,
                rubyPreselectLabel,
                rubyPreselectBox,
                sqlSep,
                sqlSuggestLabel,
                sqlScopeBox,
                qualifyWithLabel,
                qualifyWithGrid,
                qualifyInLabel,
                qualifyInGrid,
                joinClausesLabel,
                joinBox,
                tableAliasesLabel,
                aliasChecksBox,
                aliasTableComponent,
                additionalCharsBox
        );
    }

    private Node buildTableAliasesComponent() {
        VBox container = new VBox();
        container.setMaxWidth(620);
        container.setPrefHeight(160);
        container.setStyle("-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: #2B2D30;");

        // Toolbar (+ / -)
        HBox toolbar = new HBox(2);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 6, 2, 6));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: transparent transparent #393B40 transparent; -fx-border-width: 0 0 1 0;");

        Button addBtn = new Button("+");
        addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        Button removeBtn = new Button("—");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");

        toolbar.getChildren().addAll(addBtn, removeBtn);

        // TableView
        tableAliasView.setEditable(true);
        tableAliasView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent;");
        tableAliasView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TableAliasRow, String> tableCol = new TableColumn<>("Table name");
        tableCol.setCellValueFactory(c -> c.getValue().tableNameProperty());
        tableCol.setCellFactory(TextFieldTableCell.forTableColumn());
        tableCol.setOnEditCommit(e -> {
            e.getRowValue().setTableName(e.getNewValue());
            notifyModified();
        });

        TableColumn<TableAliasRow, String> aliasCol = new TableColumn<>("Custom alias");
        aliasCol.setCellValueFactory(c -> c.getValue().customAliasProperty());
        aliasCol.setCellFactory(TextFieldTableCell.forTableColumn());
        aliasCol.setOnEditCommit(e -> {
            e.getRowValue().setCustomAlias(e.getNewValue());
            notifyModified();
        });

        tableAliasView.getColumns().addAll(tableCol, aliasCol);

        // Empty placeholder: "No custom aliases" + "Add alias" link
        VBox placeholder = new VBox(6);
        placeholder.setAlignment(Pos.CENTER);
        Label emptyLabel = new Label("No custom aliases");
        emptyLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        Hyperlink addAliasLink = new Hyperlink("Add alias");
        addAliasLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
        addAliasLink.setOnAction(e -> {
            TableAliasRow row = new TableAliasRow("table_name", "alias");
            tableAliasData.add(row);
            tableAliasView.getSelectionModel().select(row);
            notifyModified();
        });
        placeholder.getChildren().addAll(emptyLabel, addAliasLink);
        tableAliasView.setPlaceholder(placeholder);

        addBtn.setOnAction(e -> {
            TableAliasRow row = new TableAliasRow("table_name", "alias");
            tableAliasData.add(row);
            tableAliasView.getSelectionModel().select(row);
            notifyModified();
        });

        removeBtn.setOnAction(e -> {
            TableAliasRow selected = tableAliasView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                tableAliasData.remove(selected);
                notifyModified();
            }
        });

        tableAliasView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            removeBtn.setStyle(n != null
                    ? "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;"
                    : "-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 13px; -fx-cursor: default; -fx-padding: 2 6 2 6;");
        });

        VBox.setVgrow(tableAliasView, Priority.ALWAYS);
        container.getChildren().addAll(toolbar, tableAliasView);
        return container;
    }

    private void setupListeners() {
        matchCaseCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        matchCaseGroup.selectedToggleProperty().addListener((obs, o, n) -> notifyModified());
        autoInsertBasicCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        autoInsertTypeMatchingCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        sortAlphabeticallyCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showSuggestionsAsYouTypeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        insertBySpaceOrDotCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        showDocPopupCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        docPopupField.textProperty().addListener((obs, o, n) -> notifyModified());
        insertParenthesesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        commandCompletionCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        commandSeparateGroupCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        commandReadOnlyCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        mlSortSuggestionsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        for (CheckBox cb : mlLanguageChecks.values()) {
            cb.selectedProperty().addListener((obs, o, n) -> notifyModified());
        }
        markPositionChangesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        markRelevantItemCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        htmlAutoPopupTagNameCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        pythonSuggestImportableCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        jsOnlyTypeBasedCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        jsSuggestOptionalChainingCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        jsExpandMethodBodiesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        jsSuggestVariableParameterNamesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        jsSuggestClassFieldsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        jsAddTypeAnnotationsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        paramShowHintsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        paramShowPopupCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        paramPopupField.textProperty().addListener((obs, o, n) -> notifyModified());
        paramShowFullSignaturesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        rustSuggestOutOfScopeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        rustHighlightMoveErrorsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        rubyMatchAcrossNamespacesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        rubySuggestMethodsAfterColonColonCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        rubyPreselectFirstInEditorsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        rubyPreselectFirstInConsolesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        sqlScopeGroup.selectedToggleProperty().addListener((obs, o, n) -> notifyModified());
        sqlQualifyDatabaseCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        sqlQualifySchemaCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        sqlQualifyTableViewCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        sqlQualifyTableViewAliasCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        sqlQualifyInBasicCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        sqlQualifyInJoinCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        sqlQualifyInRefactoringCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        sqlQualifyInLiveTemplatesCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        sqlQualifyInDragDropCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        sqlJoinUseAliasesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        sqlJoinInvertOperandsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        sqlJoinSuggestNonStrictFkCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        sqlTableAliasAutoAddCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        sqlTableAliasSuggestCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        additionalCharsField.textProperty().addListener((obs, o, n) -> notifyModified());
    }

    public void loadFromSettings(CodeCompletionSettings s) {
        suppressEvents = true;
        try {
            matchCaseCheck.setSelected(s.isMatchCase());
            if (s.getMatchCaseMode() == MatchCaseMode.ALL_LETTERS) {
                allLettersRadio.setSelected(true);
            } else {
                firstLetterRadio.setSelected(true);
            }
            firstLetterRadio.setDisable(!s.isMatchCase());
            allLettersRadio.setDisable(!s.isMatchCase());

            autoInsertBasicCheck.setSelected(s.isAutoInsertBasic());
            autoInsertTypeMatchingCheck.setSelected(s.isAutoInsertTypeMatching());
            sortAlphabeticallyCheck.setSelected(s.isSortAlphabetically());
            showSuggestionsAsYouTypeCheck.setSelected(s.isShowSuggestionsAsYouType());
            insertBySpaceOrDotCheck.setSelected(s.isInsertBySpaceOrDot());
            insertBySpaceOrDotCheck.setDisable(!s.isShowSuggestionsAsYouType());

            showDocPopupCheck.setSelected(s.isShowDocPopup());
            docPopupField.setText(String.valueOf(s.getDocPopupDelayMs()));
            docPopupField.setDisable(!s.isShowDocPopup());
            insertParenthesesCheck.setSelected(s.isInsertParentheses());

            commandCompletionCheck.setSelected(s.isCommandCompletion());
            commandSeparateGroupCheck.setSelected(s.isCommandCompletionSeparateGroup());
            commandReadOnlyCheck.setSelected(s.isCommandCompletionReadOnly());
            commandSeparateGroupCheck.setDisable(!s.isCommandCompletion());
            commandReadOnlyCheck.setDisable(!s.isCommandCompletion());

            mlSortSuggestionsCheck.setSelected(s.isMlSortSuggestions());
            for (Map.Entry<String, CheckBox> e : mlLanguageChecks.entrySet()) {
                e.getValue().setSelected(s.isMlLanguageEnabled(e.getKey()));
                e.getValue().setDisable(!s.isMlSortSuggestions());
            }
            markPositionChangesCheck.setSelected(s.isMarkPositionChanges());
            markRelevantItemCheck.setSelected(s.isMarkRelevantItem());
            markPositionChangesCheck.setDisable(!s.isMlSortSuggestions());
            markRelevantItemCheck.setDisable(!s.isMlSortSuggestions());

            htmlAutoPopupTagNameCheck.setSelected(s.isHtmlAutoPopupTagName());
            pythonSuggestImportableCheck.setSelected(s.isPythonSuggestImportable());

            jsOnlyTypeBasedCheck.setSelected(s.isJsOnlyTypeBased());
            jsSuggestOptionalChainingCheck.setSelected(s.isJsSuggestOptionalChaining());
            jsExpandMethodBodiesCheck.setSelected(s.isJsExpandMethodBodies());
            jsSuggestVariableParameterNamesCheck.setSelected(s.isJsSuggestVariableParameterNames());
            jsSuggestClassFieldsCheck.setSelected(s.isJsSuggestClassFields());
            jsAddTypeAnnotationsCheck.setSelected(s.isJsAddTypeAnnotations());

            paramShowHintsCheck.setSelected(s.isParamShowHints());
            paramShowPopupCheck.setSelected(s.isParamShowPopup());
            paramPopupField.setText(String.valueOf(s.getParamPopupDelayMs()));
            paramPopupField.setDisable(!s.isParamShowPopup());
            paramShowFullSignaturesCheck.setSelected(s.isParamShowFullSignatures());

            rustSuggestOutOfScopeCheck.setSelected(s.isRustSuggestOutOfScope());
            rustHighlightMoveErrorsCheck.setSelected(s.isRustHighlightMoveErrors());

            rubyMatchAcrossNamespacesCheck.setSelected(s.isRubyMatchAcrossNamespaces());
            rubySuggestMethodsAfterColonColonCheck.setSelected(s.isRubySuggestMethodsAfterColonColon());
            rubyPreselectFirstInEditorsCheck.setSelected(s.isRubyPreselectFirstInEditors());
            rubyPreselectFirstInConsolesCheck.setSelected(s.isRubyPreselectFirstInConsoles());

            switch (s.getSqlSuggestObjectsFrom()) {
                case SEARCH_PATH_ONLY -> sqlScopeSearchPathRadio.setSelected(true);
                case ALL_SCHEMAS -> sqlScopeAllSchemasRadio.setSelected(true);
                default -> sqlScopeCurrentScopeRadio.setSelected(true);
            }

            sqlQualifyDatabaseCombo.setValue(s.getSqlQualifyDatabase());
            sqlQualifySchemaCombo.setValue(s.getSqlQualifySchema());
            sqlQualifyTableViewCombo.setValue(s.getSqlQualifyTableView());
            sqlQualifyTableViewAliasCombo.setValue(s.getSqlQualifyTableViewAlias());

            sqlQualifyInBasicCombo.setValue(s.getSqlQualifyInBasic());
            sqlQualifyInJoinCombo.setValue(s.getSqlQualifyInJoin());
            sqlQualifyInRefactoringCombo.setValue(s.getSqlQualifyInRefactoring());
            sqlQualifyInLiveTemplatesCombo.setValue(s.getSqlQualifyInLiveTemplates());
            sqlQualifyInDragDropCombo.setValue(s.getSqlQualifyInDragDrop());

            sqlJoinUseAliasesCheck.setSelected(s.isSqlJoinUseAliases());
            sqlJoinInvertOperandsCheck.setSelected(s.isSqlJoinInvertOperands());
            sqlJoinSuggestNonStrictFkCheck.setSelected(s.isSqlJoinSuggestNonStrictFk());

            sqlTableAliasAutoAddCheck.setSelected(s.isSqlTableAliasAutoAdd());
            sqlTableAliasSuggestCheck.setSelected(s.isSqlTableAliasSuggest());

            tableAliasData.clear();
            for (TableAliasEntry e : s.getSqlTableAliases()) {
                tableAliasData.add(new TableAliasRow(e.getTableName(), e.getCustomAlias()));
            }

            additionalCharsField.setText(s.getAdditionalCharsToAccept());
        } finally {
            suppressEvents = false;
        }
    }

    public void saveToSettings(CodeCompletionSettings s) {
        s.setMatchCase(matchCaseCheck.isSelected());
        s.setMatchCaseMode(allLettersRadio.isSelected() ? MatchCaseMode.ALL_LETTERS : MatchCaseMode.FIRST_LETTER_ONLY);
        s.setAutoInsertBasic(autoInsertBasicCheck.isSelected());
        s.setAutoInsertTypeMatching(autoInsertTypeMatchingCheck.isSelected());
        s.setSortAlphabetically(sortAlphabeticallyCheck.isSelected());
        s.setShowSuggestionsAsYouType(showSuggestionsAsYouTypeCheck.isSelected());
        s.setInsertBySpaceOrDot(insertBySpaceOrDotCheck.isSelected());
        s.setShowDocPopup(showDocPopupCheck.isSelected());
        try {
            s.setDocPopupDelayMs(Integer.parseInt(docPopupField.getText().trim()));
        } catch (NumberFormatException ignored) {}
        s.setInsertParentheses(insertParenthesesCheck.isSelected());

        s.setCommandCompletion(commandCompletionCheck.isSelected());
        s.setCommandCompletionSeparateGroup(commandSeparateGroupCheck.isSelected());
        s.setCommandCompletionReadOnly(commandReadOnlyCheck.isSelected());

        s.setMlSortSuggestions(mlSortSuggestionsCheck.isSelected());
        for (Map.Entry<String, CheckBox> e : mlLanguageChecks.entrySet()) {
            s.setMlLanguageEnabled(e.getKey(), e.getValue().isSelected());
        }
        s.setMarkPositionChanges(markPositionChangesCheck.isSelected());
        s.setMarkRelevantItem(markRelevantItemCheck.isSelected());

        s.setHtmlAutoPopupTagName(htmlAutoPopupTagNameCheck.isSelected());
        s.setPythonSuggestImportable(pythonSuggestImportableCheck.isSelected());

        s.setJsOnlyTypeBased(jsOnlyTypeBasedCheck.isSelected());
        s.setJsSuggestOptionalChaining(jsSuggestOptionalChainingCheck.isSelected());
        s.setJsExpandMethodBodies(jsExpandMethodBodiesCheck.isSelected());
        s.setJsSuggestVariableParameterNames(jsSuggestVariableParameterNamesCheck.isSelected());
        s.setJsSuggestClassFields(jsSuggestClassFieldsCheck.isSelected());
        s.setJsAddTypeAnnotations(jsAddTypeAnnotationsCheck.isSelected());

        s.setParamShowHints(paramShowHintsCheck.isSelected());
        s.setParamShowPopup(paramShowPopupCheck.isSelected());
        try {
            s.setParamPopupDelayMs(Integer.parseInt(paramPopupField.getText().trim()));
        } catch (NumberFormatException ignored) {}
        s.setParamShowFullSignatures(paramShowFullSignaturesCheck.isSelected());

        s.setRustSuggestOutOfScope(rustSuggestOutOfScopeCheck.isSelected());
        s.setRustHighlightMoveErrors(rustHighlightMoveErrorsCheck.isSelected());

        s.setRubyMatchAcrossNamespaces(rubyMatchAcrossNamespacesCheck.isSelected());
        s.setRubySuggestMethodsAfterColonColon(rubySuggestMethodsAfterColonColonCheck.isSelected());
        s.setRubyPreselectFirstInEditors(rubyPreselectFirstInEditorsCheck.isSelected());
        s.setRubyPreselectFirstInConsoles(rubyPreselectFirstInConsolesCheck.isSelected());

        if (sqlScopeSearchPathRadio.isSelected()) {
            s.setSqlSuggestObjectsFrom(SqlSuggestScope.SEARCH_PATH_ONLY);
        } else if (sqlScopeAllSchemasRadio.isSelected()) {
            s.setSqlSuggestObjectsFrom(SqlSuggestScope.ALL_SCHEMAS);
        } else {
            s.setSqlSuggestObjectsFrom(SqlSuggestScope.CURRENT_SCOPE);
        }

        if (sqlQualifyDatabaseCombo.getValue() != null) s.setSqlQualifyDatabase(sqlQualifyDatabaseCombo.getValue());
        if (sqlQualifySchemaCombo.getValue() != null) s.setSqlQualifySchema(sqlQualifySchemaCombo.getValue());
        if (sqlQualifyTableViewCombo.getValue() != null) s.setSqlQualifyTableView(sqlQualifyTableViewCombo.getValue());
        if (sqlQualifyTableViewAliasCombo.getValue() != null) s.setSqlQualifyTableViewAlias(sqlQualifyTableViewAliasCombo.getValue());

        if (sqlQualifyInBasicCombo.getValue() != null) s.setSqlQualifyInBasic(sqlQualifyInBasicCombo.getValue());
        if (sqlQualifyInJoinCombo.getValue() != null) s.setSqlQualifyInJoin(sqlQualifyInJoinCombo.getValue());
        if (sqlQualifyInRefactoringCombo.getValue() != null) s.setSqlQualifyInRefactoring(sqlQualifyInRefactoringCombo.getValue());
        if (sqlQualifyInLiveTemplatesCombo.getValue() != null) s.setSqlQualifyInLiveTemplates(sqlQualifyInLiveTemplatesCombo.getValue());
        if (sqlQualifyInDragDropCombo.getValue() != null) s.setSqlQualifyInDragDrop(sqlQualifyInDragDropCombo.getValue());

        s.setSqlJoinUseAliases(sqlJoinUseAliasesCheck.isSelected());
        s.setSqlJoinInvertOperands(sqlJoinInvertOperandsCheck.isSelected());
        s.setSqlJoinSuggestNonStrictFk(sqlJoinSuggestNonStrictFkCheck.isSelected());

        s.setSqlTableAliasAutoAdd(sqlTableAliasAutoAddCheck.isSelected());
        s.setSqlTableAliasSuggest(sqlTableAliasSuggestCheck.isSelected());

        List<TableAliasEntry> list = new ArrayList<>();
        for (TableAliasRow r : tableAliasData) {
            if (!r.getTableName().isBlank()) {
                list.add(new TableAliasEntry(r.getTableName().trim(), r.getCustomAlias().trim()));
            }
        }
        s.setSqlTableAliases(list);
        s.setAdditionalCharsToAccept(additionalCharsField.getText().trim());
    }

    public boolean isModified() {
        CodeCompletionSettings current = new CodeCompletionSettings();
        saveToSettings(current);
        return current.isModified(CodeCompletionSettings.getInstance());
    }

    public void apply() {
        saveToSettings(CodeCompletionSettings.getInstance());
        CodeCompletionSettings.getInstance().save();
    }

    public void reset() {
        loadFromSettings(CodeCompletionSettings.getInstance());
    }
}