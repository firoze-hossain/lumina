// SettingsCodeCompletionPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Code Completion settings page.
 * Complete implementation matching all four screenshots.
 */
public class SettingsCodeCompletionPage extends VBox {

    public SettingsCodeCompletionPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Match case section
        // ============================================================
        Label matchCaseLabel = new Label("Match case:");
        matchCaseLabel.getStyleClass().add("settings-label");

        HBox matchCaseRow = new HBox(16);
        matchCaseRow.setPadding(new Insets(4, 0, 8, 20));
        matchCaseRow.setAlignment(Pos.CENTER_LEFT);

        RadioButton firstLetterOnly = new RadioButton("First letter only");
        firstLetterOnly.setSelected(true);
        RadioButton allLetters = new RadioButton("All letters");

        ToggleGroup matchGroup = new ToggleGroup();
        firstLetterOnly.setToggleGroup(matchGroup);
        allLetters.setToggleGroup(matchGroup);

        matchCaseRow.getChildren().addAll(matchCaseLabel, firstLetterOnly, allLetters);

        // ============================================================
        // Automatically insert single suggestions
        // ============================================================
        CheckBox basicCompletion = new CheckBox("Basic Completion Ctrl+Space");
        basicCompletion.setSelected(true);
        basicCompletion.getStyleClass().add("settings-check");

        CheckBox typeMatchingCompletion = new CheckBox("Type-Matching Completion Ctrl+Shift+Space");
        typeMatchingCompletion.setSelected(true);
        typeMatchingCompletion.getStyleClass().add("settings-check");

        CheckBox sortAlphabetically = new CheckBox("Sort suggestions alphabetically");
        sortAlphabetically.getStyleClass().add("settings-check");

        CheckBox showSuggestions = new CheckBox("Show suggestions as you type");
        showSuggestions.setSelected(true);
        showSuggestions.getStyleClass().add("settings-check");

        VBox autoInsertBox = new VBox(4);
        autoInsertBox.setPadding(new Insets(4, 0, 8, 20));
        autoInsertBox.getChildren().addAll(
            basicCompletion,
            typeMatchingCompletion,
            sortAlphabetically,
            showSuggestions
        );

        // ============================================================
        // Insert selected suggestion
        // ============================================================
        CheckBox insertSuggestion = new CheckBox("Insert selected suggestion by pressing space, dot, or other context-dependent keys");
        insertSuggestion.setSelected(true);
        insertSuggestion.getStyleClass().add("settings-check");

        VBox suggestionOptions = new VBox(4);
        suggestionOptions.setPadding(new Insets(4, 0, 8, 20));

        HBox docPopupRow = new HBox(8);
        docPopupRow.setAlignment(Pos.CENTER_LEFT);
        Label docLabel = new Label("Show the documentation popup in");
        docLabel.getStyleClass().add("settings-label");
        Spinner<Integer> docSpinner = new Spinner<>(100, 2000, 500, 100);
        docSpinner.setPrefWidth(70);
        docSpinner.getStyleClass().add("settings-spinner");
        Label msLabel = new Label("ms");
        msLabel.getStyleClass().add("settings-label");
        docPopupRow.getChildren().addAll(docLabel, docSpinner, msLabel);

        CheckBox insertParentheses = new CheckBox("Insert parentheses automatically when applicable");
        insertParentheses.setSelected(true);
        insertParentheses.getStyleClass().add("settings-check");

        suggestionOptions.getChildren().addAll(insertSuggestion, docPopupRow, insertParentheses);

        // ============================================================
        // Configure classes excluded from completion
        // ============================================================
        Label excludedLabel = new Label("Configure classes excluded from completion");
        excludedLabel.getStyleClass().add("settings-section");

        // Command Completion section
        CheckBox enableCommandCompletion = new CheckBox("Enable command completion");
        enableCommandCompletion.setSelected(true);
        enableCommandCompletion.getStyleClass().add("settings-check");

        CheckBox showCommandSeparate = new CheckBox("Show command completion as a separate group");
        showCommandSeparate.getStyleClass().add("settings-check");

        CheckBox enableReadOnly = new CheckBox("Enable command completion for read-only files Beta");
        enableReadOnly.getStyleClass().add("settings-check");

        VBox commandBox = new VBox(4);
        commandBox.setPadding(new Insets(4, 0, 8, 20));
        commandBox.getChildren().addAll(
            enableCommandCompletion,
            showCommandSeparate,
            enableReadOnly
        );

        // ============================================================
        // Machine Learning-Assisted Completion
        // ============================================================
        Label mlLabel = new Label("Machine Learning-Assisted Completion");
        mlLabel.getStyleClass().add("settings-section");

        Label mlHint = new Label("Go to Inline Completion settings page to adjust inline completion (e.g. Full Line Code Completion) settings");
        mlHint.getStyleClass().add("settings-hint");
        mlHint.setWrapText(true);

        CheckBox sortML = new CheckBox("Sort completion suggestions based on machine learning");
        sortML.setSelected(true);
        sortML.getStyleClass().add("settings-check");

        GridPane mlGrid = new GridPane();
        mlGrid.setHgap(16);
        mlGrid.setVgap(4);
        mlGrid.setPadding(new Insets(4, 0, 8, 20));

        String[] mlLanguages = {
            "Java", "JavaScript", "Kotlin", "Rust", "SQL", "Shell Script", "TypeScript"
        };

        for (int i = 0; i < mlLanguages.length; i++) {
            CheckBox cb = new CheckBox(mlLanguages[i]);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            mlGrid.add(cb, i % 3, i / 3);
        }

        VBox mlBox = new VBox(4, mlHint, sortML, mlGrid);

        // ============================================================
        // Mark position changes in the completion popup
        // ============================================================
        CheckBox markRelevant = new CheckBox("Mark the most relevant item in the completion popup");
        markRelevant.setSelected(true);
        markRelevant.getStyleClass().add("settings-check");

        VBox markBox = new VBox(4);
        markBox.setPadding(new Insets(4, 0, 8, 20));
        markBox.getChildren().add(markRelevant);

        // ============================================================
        // HTML section
        // ============================================================
        Label htmlLabel = new Label("HTML");
        htmlLabel.getStyleClass().add("settings-section");

        CheckBox autoPopupHTML = new CheckBox("Enable auto-popup of tag name code completion when typing in HTML text");
        autoPopupHTML.setSelected(true);
        autoPopupHTML.getStyleClass().add("settings-check");

        VBox htmlBox = new VBox(4);
        htmlBox.setPadding(new Insets(4, 0, 8, 20));
        htmlBox.getChildren().add(autoPopupHTML);

        // ============================================================
        // JavaScript section
        // ============================================================
        Label jsLabel = new Label("JavaScript");
        jsLabel.getStyleClass().add("settings-section");

        CheckBox typeBasedCompletion = new CheckBox("Only type-based completion");
        typeBasedCompletion.getStyleClass().add("settings-check");
        Label typeHint = new Label("Show fewer completion suggestions based on type information. May significantly improve performance.");
        typeHint.getStyleClass().add("settings-hint");
        typeHint.setPadding(new Insets(0, 0, 0, 20));
        typeHint.setWrapText(true);

        CheckBox optionalChaining = new CheckBox("Suggest items with optional chaining for nullable types");
        optionalChaining.setSelected(true);
        optionalChaining.getStyleClass().add("settings-check");

        CheckBox expandMethodBodies = new CheckBox("Expand method bodies in completion for overrides");
        expandMethodBodies.getStyleClass().add("settings-check");

        CheckBox completionOfNames = new CheckBox("Completion of names");
        completionOfNames.setSelected(true);
        completionOfNames.getStyleClass().add("settings-check");

        CheckBox suggestVariableNames = new CheckBox("Suggest variable and parameter names");
        suggestVariableNames.setSelected(true);
        suggestVariableNames.getStyleClass().add("settings-check");

        CheckBox suggestClassFields = new CheckBox("Suggest names for class fields");
        suggestClassFields.setSelected(true);
        suggestClassFields.getStyleClass().add("settings-check");

        CheckBox addTypeAnnotations = new CheckBox("Add type annotations for suggested parameter names");
        addTypeAnnotations.setSelected(true);
        addTypeAnnotations.getStyleClass().add("settings-check");

        VBox jsBox = new VBox(4);
        jsBox.setPadding(new Insets(4, 0, 8, 20));
        jsBox.getChildren().addAll(
            typeBasedCompletion,
            typeHint,
            optionalChaining,
            expandMethodBodies,
            completionOfNames,
            suggestVariableNames,
            suggestClassFields,
            addTypeAnnotations
        );

        // ============================================================
        // Parameter Info section
        // ============================================================
        Label paramLabel = new Label("Parameter Info");
        paramLabel.getStyleClass().add("settings-section");

        CheckBox paramNameHints = new CheckBox("Show parameter name hints on completion");
        paramNameHints.setSelected(true);
        paramNameHints.getStyleClass().add("settings-check");

        HBox paramPopupRow = new HBox(8);
        paramPopupRow.setPadding(new Insets(4, 0, 4, 20));
        paramPopupRow.setAlignment(Pos.CENTER_LEFT);
        Label paramPopupLabel = new Label("Show the parameter info popup in");
        paramPopupLabel.getStyleClass().add("settings-label");
        Spinner<Integer> paramPopupSpinner = new Spinner<>(100, 3000, 1000, 100);
        paramPopupSpinner.setPrefWidth(70);
        paramPopupSpinner.getStyleClass().add("settings-spinner");
        Label paramMsLabel = new Label("ms");
        paramMsLabel.getStyleClass().add("settings-label");
        paramPopupRow.getChildren().addAll(paramPopupLabel, paramPopupSpinner, paramMsLabel);

        CheckBox fullSignatures = new CheckBox("Show full method signatures");
        fullSignatures.setSelected(true);
        fullSignatures.getStyleClass().add("settings-check");

        VBox paramBox = new VBox(4);
        paramBox.setPadding(new Insets(4, 0, 8, 20));
        paramBox.getChildren().addAll(paramNameHints, paramPopupRow, fullSignatures);

        // ============================================================
        // Rust section
        // ============================================================
        Label rustLabel = new Label("Rust");
        rustLabel.getStyleClass().add("settings-section");

        CheckBox suggestOutOfScope = new CheckBox("Suggest out-of-scope items");
        suggestOutOfScope.setSelected(true);
        suggestOutOfScope.getStyleClass().add("settings-check");

        CheckBox highlightMoveErrors = new CheckBox("Highlight move errors in completion list");
        highlightMoveErrors.setSelected(true);
        highlightMoveErrors.getStyleClass().add("settings-check");

        VBox rustBox = new VBox(4);
        rustBox.setPadding(new Insets(4, 0, 8, 20));
        rustBox.getChildren().addAll(suggestOutOfScope, highlightMoveErrors);

        // ============================================================
        // SQL section
        // ============================================================
        Label sqlLabel = new Label("SQL");
        sqlLabel.getStyleClass().add("settings-section");

        Label suggestObjectsLabel = new Label("Suggest objects from:");
        suggestObjectsLabel.getStyleClass().add("settings-label");
        suggestObjectsLabel.setPadding(new Insets(4, 0, 2, 0));

        VBox sqlBox = new VBox(6);
        sqlBox.setPadding(new Insets(4, 0, 8, 20));

        RadioButton searchPathOnly = new RadioButton("The current search path only");
        RadioButton currentScope = new RadioButton("The current scope");
        RadioButton allSchemas = new RadioButton("All available schemas");
        allSchemas.setSelected(true);

        ToggleGroup sqlGroup = new ToggleGroup();
        searchPathOnly.setToggleGroup(sqlGroup);
        currentScope.setToggleGroup(sqlGroup);
        allSchemas.setToggleGroup(sqlGroup);

        sqlBox.getChildren().addAll(searchPathOnly, currentScope, allSchemas);

        // Qualify object with
        Label qualifyLabel = new Label("Qualify object with:");
        qualifyLabel.getStyleClass().add("settings-label");
        qualifyLabel.setPadding(new Insets(8, 0, 2, 0));

        GridPane qualifyGrid = new GridPane();
        qualifyGrid.setHgap(16);
        qualifyGrid.setVgap(4);
        qualifyGrid.setPadding(new Insets(4, 0, 8, 20));

        String[] qualifyItems = {"Database:", "Schema:", "Table/View:", "Table/view alias:"};
        for (int i = 0; i < qualifyItems.length; i++) {
            Label label = new Label(qualifyItems[i]);
            label.getStyleClass().add("settings-label");
            ComboBox<String> combo = new ComboBox<>();
            combo.getItems().addAll("Always", "Never", "On collisions");
            combo.getSelectionModel().select("Always");
            combo.getStyleClass().add("settings-combo");
            combo.setPrefWidth(130);
            qualifyGrid.add(label, 0, i);
            qualifyGrid.add(combo, 1, i);
        }

        // Qualify object in
        Label qualifyInLabel = new Label("Qualify object in:");
        qualifyInLabel.getStyleClass().add("settings-label");
        qualifyInLabel.setPadding(new Insets(8, 0, 2, 0));

        GridPane qualifyInGrid = new GridPane();
        qualifyInGrid.setHgap(16);
        qualifyInGrid.setVgap(4);
        qualifyInGrid.setPadding(new Insets(4, 0, 8, 20));

        String[] qualifyInItems = {"Basic completion:", "JOIN completion:", "Refactoring:", "Live templates:", "Drag-n-Drop:"};
        String[] qualifyInDefaults = {"On collisions", "Always", "On collisions", "On collisions", "On collisions"};

        for (int i = 0; i < qualifyInItems.length; i++) {
            Label label = new Label(qualifyInItems[i]);
            label.getStyleClass().add("settings-label");
            ComboBox<String> combo = new ComboBox<>();
            combo.getItems().addAll("Always", "Never", "On collisions");
            combo.getSelectionModel().select(qualifyInDefaults[i]);
            combo.getStyleClass().add("settings-combo");
            combo.setPrefWidth(130);
            qualifyInGrid.add(label, 0, i);
            qualifyInGrid.add(combo, 1, i);
        }

        // JOIN clauses
        Label joinLabel = new Label("JOIN clauses:");
        joinLabel.getStyleClass().add("settings-label");
        joinLabel.setPadding(new Insets(8, 0, 2, 0));

        CheckBox useAliasesJoin = new CheckBox("Use aliases in completion for JOIN");
        useAliasesJoin.setSelected(true);
        useAliasesJoin.getStyleClass().add("settings-check");

        CheckBox invertOrder = new CheckBox("Invert order of operands in auto-generated ON clause");
        invertOrder.getStyleClass().add("settings-check");

        CheckBox suggestForeignKeys = new CheckBox("Suggest non-strict foreign keys based on the name matching");
        suggestForeignKeys.setSelected(true);
        suggestForeignKeys.getStyleClass().add("settings-check");

        VBox joinBox = new VBox(4);
        joinBox.setPadding(new Insets(4, 0, 8, 20));
        joinBox.getChildren().addAll(useAliasesJoin, invertOrder, suggestForeignKeys);

        // Table aliases
        Label tableAliasLabel = new Label("Table aliases:");
        tableAliasLabel.getStyleClass().add("settings-label");
        tableAliasLabel.setPadding(new Insets(8, 0, 2, 0));

        CheckBox autoAddAliases = new CheckBox("Automatically add aliases when completing table names");
        autoAddAliases.setSelected(true);
        autoAddAliases.getStyleClass().add("settings-check");

        CheckBox suggestAliasNames = new CheckBox("Suggest alias names in completion after table names");
        suggestAliasNames.setSelected(true);
        suggestAliasNames.getStyleClass().add("settings-check");

        VBox tableAliasBox = new VBox(4);
        tableAliasBox.setPadding(new Insets(4, 0, 8, 20));
        tableAliasBox.getChildren().addAll(autoAddAliases, suggestAliasNames);

        // Table name - Custom alias
        Label tableNameLabel = new Label("Table name:");
        tableNameLabel.getStyleClass().add("settings-label");
        tableNameLabel.setPadding(new Insets(8, 0, 2, 0));

        HBox tableNameRow = new HBox(8);
        tableNameRow.setPadding(new Insets(4, 0, 8, 20));
        tableNameRow.setAlignment(Pos.CENTER_LEFT);

        RadioButton customAlias = new RadioButton("Custom alias");
        customAlias.setSelected(true);
        RadioButton noCustomAlias = new RadioButton("No custom aliases");

        ToggleGroup tableAliasGroup = new ToggleGroup();
        customAlias.setToggleGroup(tableAliasGroup);
        noCustomAlias.setToggleGroup(tableAliasGroup);

        tableNameRow.getChildren().addAll(tableNameLabel, customAlias, noCustomAlias);

        // Additional characters to accept completion
        Label additionalCharsLabel = new Label("Additional characters to accept completion:");
        additionalCharsLabel.getStyleClass().add("settings-label");
        additionalCharsLabel.setPadding(new Insets(8, 0, 2, 0));

        TextField additionalCharsField = new TextField();
        additionalCharsField.setPromptText("e.g. . : ;");
        additionalCharsField.getStyleClass().add("text-field");
        additionalCharsField.setPrefWidth(300);
        additionalCharsField.setPadding(new Insets(4, 0, 8, 20));

        VBox additionalCharsBox = new VBox(4);
        additionalCharsBox.setPadding(new Insets(4, 0, 8, 20));
        additionalCharsBox.getChildren().add(additionalCharsField);

        // Assemble SQL section
        VBox sqlFullBox = new VBox(4);
        sqlFullBox.getChildren().addAll(
            sqlLabel,
            suggestObjectsLabel,
            sqlBox,
            qualifyLabel,
            qualifyGrid,
            qualifyInLabel,
            qualifyInGrid,
            joinLabel,
            joinBox,
            tableAliasLabel,
            tableAliasBox,
            tableNameLabel,
            tableNameRow,
            additionalCharsLabel,
            additionalCharsBox
        );

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            matchCaseLabel, matchCaseRow,
            basicCompletion, typeMatchingCompletion, sortAlphabetically, showSuggestions,
            insertSuggestion, docPopupRow, insertParentheses,
            excludedLabel,
            commandBox,
            mlLabel, mlBox,
            markRelevant,
            htmlLabel, htmlBox,
            jsLabel, jsBox,
            paramLabel, paramBox,
            rustLabel, rustBox,
            sqlFullBox
        );
    }
}