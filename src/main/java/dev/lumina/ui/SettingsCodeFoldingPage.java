package dev.lumina.ui;

import dev.lumina.folding.CodeFoldingSettings;
import dev.lumina.folding.CodeFoldingSettings.FoldingArrowsMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

/**
 * Pixel-perfect IntelliJ IDEA-style Editor > General > Code Folding settings page.
 * Completely dynamic, backed by CodeFoldingSettings, supporting dirty tracking,
 * real-time persistence, and all 16 language/feature folding groups.
 */
public class SettingsCodeFoldingPage extends VBox {

    // Top options
    private final CheckBox showFoldingArrowsCheck = new CheckBox("Show code folding arrows");
    private final ComboBox<FoldingArrowsMode> foldingArrowsModeCombo = new ComboBox<>();
    private final CheckBox showBottomArrowsCheck = new CheckBox("Show bottom arrows");

    // General
    private final CheckBox generalFileHeaderCheck = new CheckBox("File header");
    private final CheckBox generalImportsCheck = new CheckBox("Imports");
    private final CheckBox generalDocCommentsCheck = new CheckBox("Documentation comments");
    private final CheckBox generalMethodBodiesCheck = new CheckBox("Method bodies");
    private final CheckBox generalCustomRegionsCheck = new CheckBox("Custom folding regions");

    // JPA QL
    private final CheckBox jpaQueriesCheck = new CheckBox("Queries");

    // JSON
    private final CheckBox jsonShowKeyCountCheck = new CheckBox("Show key count in folded JSON");
    private final CheckBox jsonShowFirstKeyCheck = new CheckBox("Show the first key in folded JSON");
    private final Label jsonFirstKeyHint = new Label("Always show the first key in a folded JSON object. Otherwise, \"id\" or \"name\" keys will be shown if present.");

    // Java
    private final CheckBox javaOneLineMethodsCheck = new CheckBox("One-line methods");
    private final CheckBox javaSimplePropertyAccessorsCheck = new CheckBox("Simple property accessors");
    private final CheckBox javaInnerClassesCheck = new CheckBox("Inner classes");
    private final CheckBox javaAnonymousClassesCheck = new CheckBox("Anonymous classes");
    private final CheckBox javaAnnotationsCheck = new CheckBox("Annotations");
    private final CheckBox javaClosuresCheck = new CheckBox("\"Closures\" (anonymous classes implementing one method, before Java 8)");
    private final CheckBox javaGenericParamsCheck = new CheckBox("Generic constructor and method parameters");
    private final CheckBox javaReplaceVarCheck = new CheckBox("Replace 'var' with inferred type");
    private final CheckBox javaI18nStringsCheck = new CheckBox("I18n strings");
    private final CheckBox javaSuppressWarningsCheck = new CheckBox("@SuppressWarnings");
    private final CheckBox javaEndOfLineCommentsCheck = new CheckBox("End of line comments sequence");
    private final CheckBox javaMultilineCommentsCheck = new CheckBox("Multiline comments");

    // JavaScript
    private final CheckBox jsOneLineFunctionsCheck = new CheckBox("One-line functions in JavaScript and TypeScript");
    private final CheckBox jsObjectLiteralsCheck = new CheckBox("Object literals");
    private final CheckBox jsArrayLiteralsCheck = new CheckBox("Array literals");
    private final CheckBox jsXmlLiteralsCheck = new CheckBox("XML literals");

    // Kubernetes
    private final CheckBox k8sHelmValueReferencesCheck = new CheckBox("Value references in Helm templates");
    private final CheckBox k8sEnvVarYamlCheck = new CheckBox("EnvVar definitions in YAML files");
    private final CheckBox k8sExecActionYamlCheck = new CheckBox("ExecAction definitions in YAML files");

    // Markdown
    private final CheckBox markdownCollapseFrontMatterCheck = new CheckBox("Collapse front matter");
    private final CheckBox markdownCollapseLinksCheck = new CheckBox("Collapse links");
    private final CheckBox markdownCollapseTablesCheck = new CheckBox("Collapse tables");
    private final CheckBox markdownCollapseCodeFencesCheck = new CheckBox("Collapse code fences");
    private final CheckBox markdownCollapseTocCheck = new CheckBox("Collapse table of contents");

    // PHP
    private final CheckBox phpClassBodyCheck = new CheckBox("Class body");
    private final CheckBox phpImportsCheck = new CheckBox("Imports");
    private final CheckBox phpMethodBodyCheck = new CheckBox("Method body");
    private final CheckBox phpFunctionBodyCheck = new CheckBox("Function body");
    private final CheckBox phpTagsCheck = new CheckBox("PHP tags");
    private final CheckBox phpHeredocCheck = new CheckBox("Heredoc");
    private final CheckBox phpAttributeCheck = new CheckBox("Attribute");
    private final CheckBox phpAttributeListCheck = new CheckBox("Attribute list");

    // Python
    private final CheckBox pythonLongStringLiteralsCheck = new CheckBox("Long string literals");
    private final CheckBox pythonLongCollectionLiteralsCheck = new CheckBox("Long collection literals");
    private final CheckBox pythonSequentialCommentsCheck = new CheckBox("Sequential comments");
    private final CheckBox pythonTypeAnnotationsCheck = new CheckBox("Type annotations");
    private final Label pythonTypeAnnotationsHint = new Label("When unchecked, type annotations remain expanded on first \"Collapse All\" action.");

    // Ruby i18n
    private final CheckBox rubyI18nStringsCheck = new CheckBox("I18n strings");

    // Rust
    private final CheckBox rustOneLineMethodsCheck = new CheckBox("One-line methods");

    // SQL
    private final CheckBox sqlUnderscoresInNumericLiteralsCheck = new CheckBox("Put underscores inside numeric literals (6-digit or longer)");

    // Scala
    private final CheckBox scalaBlockCommentsCheck = new CheckBox("Block comments");
    private final CheckBox scalaMethodCallBodiesCheck = new CheckBox("Method call bodies");
    private final CheckBox scalaTemplateDefinitionBodiesCheck = new CheckBox("Template definition bodies");
    private final CheckBox scalaDefinitionBodiesCheck = new CheckBox("Definition bodies");
    private final CheckBox scalaTypeLambdasCheck = new CheckBox("Type lambdas");
    private final CheckBox scalaPackagesCheck = new CheckBox("Packages");
    private final CheckBox scalaMultiLineStringsCheck = new CheckBox("Multi-line strings");
    private final CheckBox scalaCustomRegionsCheck = new CheckBox("Custom regions");
    private final CheckBox scalaMultiLineBlocksCheck = new CheckBox("Multi-line blocks");
    private final CheckBox scalaShowOutlineForMultiLineBlocksCheck = new CheckBox("Show outline for multi-line blocks");

    // XML
    private final CheckBox xmlTagsCheck = new CheckBox("XML tags");
    private final CheckBox xmlHtmlStyleAttributeCheck = new CheckBox("HTML 'style' attribute");
    private final CheckBox xmlEntitiesCheck = new CheckBox("XML entities");
    private final CheckBox xmlDataUrisCheck = new CheckBox("Data URIs");

    // YAML
    private final CheckBox yamlLimitFoldedKeysAndValuesCheck = new CheckBox("Limit folded keys and values to");
    private final TextField yamlLimitCharactersField = new TextField("20");
    private final Label yamlCharactersLabel = new Label("characters");

    // Go
    private final CheckBox goOneLineIfErrorHandlingCheck = new CheckBox("One-line if error handling");
    private final CheckBox goOneLineFunctionsSingleReturnCheck = new CheckBox("One-line functions with single return statement");
    private final CheckBox goOneLineCaseClausesCheck = new CheckBox("One-line case clauses");
    private final CheckBox goEmptyFunctionsCheck = new CheckBox("Empty functions");
    private final CheckBox goEmptyStructOrInterfaceCheck = new CheckBox("Empty struct or interface");
    private final CheckBox goFormattedStringsCheck = new CheckBox("Formatted strings");

    // Listener & event suppression
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsCodeFoldingPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 28, 24));
        setSpacing(6);

        buildUi();
        setupListeners();
        loadFromSettings(CodeFoldingSettings.getInstance());
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
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private Node buildSectionSeparator(String title) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14, 0, 4, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1px; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(label, line);
        return box;
    }

    private VBox createGroupContainer(Node... nodes) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(2, 0, 4, 20));
        box.getChildren().addAll(nodes);
        return box;
    }

    private void buildUi() {
        // --- Top Options ---
        styleCheckBox(showFoldingArrowsCheck);

        foldingArrowsModeCombo.getItems().setAll(FoldingArrowsMode.values());
        foldingArrowsModeCombo.setValue(FoldingArrowsMode.ON_MOUSE_HOVER);
        foldingArrowsModeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(FoldingArrowsMode mode) {
                return mode != null ? mode.getLabel() : "";
            }

            @Override
            public FoldingArrowsMode fromString(String string) {
                return FoldingArrowsMode.fromLabel(string);
            }
        });
        foldingArrowsModeCombo.setPrefWidth(140);
        foldingArrowsModeCombo.setPrefHeight(25);
        foldingArrowsModeCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox arrowsRow = new HBox(8, showFoldingArrowsCheck, foldingArrowsModeCombo);
        arrowsRow.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(showBottomArrowsCheck);
        VBox bottomArrowsBox = new VBox(showBottomArrowsCheck);
        bottomArrowsBox.setPadding(new Insets(0, 0, 6, 20));

        showFoldingArrowsCheck.selectedProperty().addListener((obs, o, n) -> {
            foldingArrowsModeCombo.setDisable(!n);
            showBottomArrowsCheck.setDisable(!n);
        });

        // --- Fold by default Header ---
        Label foldByDefaultLabel = new Label("Fold by default:");
        foldByDefaultLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        VBox.setMargin(foldByDefaultLabel, new Insets(8, 0, 2, 0));

        // --- General ---
        styleCheckBox(generalFileHeaderCheck);
        styleCheckBox(generalImportsCheck);
        styleCheckBox(generalDocCommentsCheck);
        styleCheckBox(generalMethodBodiesCheck);
        styleCheckBox(generalCustomRegionsCheck);
        VBox generalBox = createGroupContainer(
                generalFileHeaderCheck,
                generalImportsCheck,
                generalDocCommentsCheck,
                generalMethodBodiesCheck,
                generalCustomRegionsCheck
        );

        // --- JPA QL ---
        styleCheckBox(jpaQueriesCheck);
        VBox jpaBox = createGroupContainer(jpaQueriesCheck);

        // --- JSON ---
        styleCheckBox(jsonShowKeyCountCheck);
        styleCheckBox(jsonShowFirstKeyCheck);
        jsonFirstKeyHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        jsonFirstKeyHint.setWrapText(true);
        jsonFirstKeyHint.setPadding(new Insets(0, 0, 2, 22));
        VBox jsonBox = createGroupContainer(jsonShowKeyCountCheck, jsonShowFirstKeyCheck, jsonFirstKeyHint);

        // --- Java ---
        styleCheckBox(javaOneLineMethodsCheck);
        styleCheckBox(javaSimplePropertyAccessorsCheck);
        styleCheckBox(javaInnerClassesCheck);
        styleCheckBox(javaAnonymousClassesCheck);
        styleCheckBox(javaAnnotationsCheck);
        styleCheckBox(javaClosuresCheck);
        styleCheckBox(javaGenericParamsCheck);
        styleCheckBox(javaReplaceVarCheck);
        styleCheckBox(javaI18nStringsCheck);
        styleCheckBox(javaSuppressWarningsCheck);
        styleCheckBox(javaEndOfLineCommentsCheck);
        styleCheckBox(javaMultilineCommentsCheck);
        VBox javaBox = createGroupContainer(
                javaOneLineMethodsCheck,
                javaSimplePropertyAccessorsCheck,
                javaInnerClassesCheck,
                javaAnonymousClassesCheck,
                javaAnnotationsCheck,
                javaClosuresCheck,
                javaGenericParamsCheck,
                javaReplaceVarCheck,
                javaI18nStringsCheck,
                javaSuppressWarningsCheck,
                javaEndOfLineCommentsCheck,
                javaMultilineCommentsCheck
        );

        // --- JavaScript ---
        styleCheckBox(jsOneLineFunctionsCheck);
        styleCheckBox(jsObjectLiteralsCheck);
        styleCheckBox(jsArrayLiteralsCheck);
        styleCheckBox(jsXmlLiteralsCheck);
        VBox jsBox = createGroupContainer(
                jsOneLineFunctionsCheck,
                jsObjectLiteralsCheck,
                jsArrayLiteralsCheck,
                jsXmlLiteralsCheck
        );

        // --- Kubernetes ---
        styleCheckBox(k8sHelmValueReferencesCheck);
        styleCheckBox(k8sEnvVarYamlCheck);
        styleCheckBox(k8sExecActionYamlCheck);
        VBox k8sBox = createGroupContainer(
                k8sHelmValueReferencesCheck,
                k8sEnvVarYamlCheck,
                k8sExecActionYamlCheck
        );

        // --- Markdown ---
        styleCheckBox(markdownCollapseFrontMatterCheck);
        styleCheckBox(markdownCollapseLinksCheck);
        styleCheckBox(markdownCollapseTablesCheck);
        styleCheckBox(markdownCollapseCodeFencesCheck);
        styleCheckBox(markdownCollapseTocCheck);
        VBox markdownBox = createGroupContainer(
                markdownCollapseFrontMatterCheck,
                markdownCollapseLinksCheck,
                markdownCollapseTablesCheck,
                markdownCollapseCodeFencesCheck,
                markdownCollapseTocCheck
        );

        // --- PHP ---
        styleCheckBox(phpClassBodyCheck);
        styleCheckBox(phpImportsCheck);
        styleCheckBox(phpMethodBodyCheck);
        styleCheckBox(phpFunctionBodyCheck);
        styleCheckBox(phpTagsCheck);
        styleCheckBox(phpHeredocCheck);
        styleCheckBox(phpAttributeCheck);
        styleCheckBox(phpAttributeListCheck);
        VBox phpBox = createGroupContainer(
                phpClassBodyCheck,
                phpImportsCheck,
                phpMethodBodyCheck,
                phpFunctionBodyCheck,
                phpTagsCheck,
                phpHeredocCheck,
                phpAttributeCheck,
                phpAttributeListCheck
        );

        // --- Python ---
        styleCheckBox(pythonLongStringLiteralsCheck);
        styleCheckBox(pythonLongCollectionLiteralsCheck);
        styleCheckBox(pythonSequentialCommentsCheck);
        styleCheckBox(pythonTypeAnnotationsCheck);
        pythonTypeAnnotationsHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        pythonTypeAnnotationsHint.setWrapText(true);
        pythonTypeAnnotationsHint.setPadding(new Insets(0, 0, 2, 22));
        VBox pythonBox = createGroupContainer(
                pythonLongStringLiteralsCheck,
                pythonLongCollectionLiteralsCheck,
                pythonSequentialCommentsCheck,
                pythonTypeAnnotationsCheck,
                pythonTypeAnnotationsHint
        );

        // --- Ruby i18n ---
        styleCheckBox(rubyI18nStringsCheck);
        VBox rubyBox = createGroupContainer(rubyI18nStringsCheck);

        // --- Rust ---
        styleCheckBox(rustOneLineMethodsCheck);
        VBox rustBox = createGroupContainer(rustOneLineMethodsCheck);

        // --- SQL ---
        styleCheckBox(sqlUnderscoresInNumericLiteralsCheck);
        VBox sqlBox = createGroupContainer(sqlUnderscoresInNumericLiteralsCheck);

        // --- Scala ---
        styleCheckBox(scalaBlockCommentsCheck);
        styleCheckBox(scalaMethodCallBodiesCheck);
        styleCheckBox(scalaTemplateDefinitionBodiesCheck);
        styleCheckBox(scalaDefinitionBodiesCheck);
        styleCheckBox(scalaTypeLambdasCheck);
        styleCheckBox(scalaPackagesCheck);
        styleCheckBox(scalaMultiLineStringsCheck);
        styleCheckBox(scalaCustomRegionsCheck);
        styleCheckBox(scalaMultiLineBlocksCheck);
        styleCheckBox(scalaShowOutlineForMultiLineBlocksCheck);
        VBox scalaBox = createGroupContainer(
                scalaBlockCommentsCheck,
                scalaMethodCallBodiesCheck,
                scalaTemplateDefinitionBodiesCheck,
                scalaDefinitionBodiesCheck,
                scalaTypeLambdasCheck,
                scalaPackagesCheck,
                scalaMultiLineStringsCheck,
                scalaCustomRegionsCheck,
                scalaMultiLineBlocksCheck,
                scalaShowOutlineForMultiLineBlocksCheck
        );

        // --- XML ---
        styleCheckBox(xmlTagsCheck);
        styleCheckBox(xmlHtmlStyleAttributeCheck);
        styleCheckBox(xmlEntitiesCheck);
        styleCheckBox(xmlDataUrisCheck);
        VBox xmlBox = createGroupContainer(
                xmlTagsCheck,
                xmlHtmlStyleAttributeCheck,
                xmlEntitiesCheck,
                xmlDataUrisCheck
        );

        // --- YAML ---
        styleCheckBox(yamlLimitFoldedKeysAndValuesCheck);
        yamlLimitCharactersField.setPrefWidth(55);
        yamlLimitCharactersField.setPrefHeight(25);
        yamlLimitCharactersField.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        yamlCharactersLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox yamlRow = new HBox(8, yamlLimitFoldedKeysAndValuesCheck, yamlLimitCharactersField, yamlCharactersLabel);
        yamlRow.setAlignment(Pos.CENTER_LEFT);
        yamlLimitFoldedKeysAndValuesCheck.selectedProperty().addListener((obs, o, n) -> yamlLimitCharactersField.setDisable(!n));
        VBox yamlBox = createGroupContainer(yamlRow);

        // --- Go ---
        styleCheckBox(goOneLineIfErrorHandlingCheck);
        styleCheckBox(goOneLineFunctionsSingleReturnCheck);
        styleCheckBox(goOneLineCaseClausesCheck);
        styleCheckBox(goEmptyFunctionsCheck);
        styleCheckBox(goEmptyStructOrInterfaceCheck);
        styleCheckBox(goFormattedStringsCheck);
        VBox goBox = createGroupContainer(
                goOneLineIfErrorHandlingCheck,
                goOneLineFunctionsSingleReturnCheck,
                goOneLineCaseClausesCheck,
                goEmptyFunctionsCheck,
                goEmptyStructOrInterfaceCheck,
                goFormattedStringsCheck
        );

        getChildren().addAll(
                arrowsRow,
                bottomArrowsBox,
                foldByDefaultLabel,
                buildSectionSeparator("General"),
                generalBox,
                buildSectionSeparator("JPA QL"),
                jpaBox,
                buildSectionSeparator("JSON"),
                jsonBox,
                buildSectionSeparator("Java"),
                javaBox,
                buildSectionSeparator("JavaScript"),
                jsBox,
                buildSectionSeparator("Kubernetes"),
                k8sBox,
                buildSectionSeparator("Markdown"),
                markdownBox,
                buildSectionSeparator("PHP"),
                phpBox,
                buildSectionSeparator("Python"),
                pythonBox,
                buildSectionSeparator("Ruby i18n"),
                rubyBox,
                buildSectionSeparator("Rust"),
                rustBox,
                buildSectionSeparator("SQL"),
                sqlBox,
                buildSectionSeparator("Scala"),
                scalaBox,
                buildSectionSeparator("XML"),
                xmlBox,
                buildSectionSeparator("YAML"),
                yamlBox,
                buildSectionSeparator("Go"),
                goBox
        );
    }

    private void setupListeners() {
        showFoldingArrowsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        foldingArrowsModeCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        showBottomArrowsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        generalFileHeaderCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        generalImportsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        generalDocCommentsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        generalMethodBodiesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        generalCustomRegionsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        jpaQueriesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        jsonShowKeyCountCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        jsonShowFirstKeyCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        javaOneLineMethodsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaSimplePropertyAccessorsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaInnerClassesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaAnonymousClassesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaAnnotationsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaClosuresCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaGenericParamsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaReplaceVarCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaI18nStringsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaSuppressWarningsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaEndOfLineCommentsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaMultilineCommentsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        jsOneLineFunctionsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        jsObjectLiteralsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        jsArrayLiteralsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        jsXmlLiteralsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        k8sHelmValueReferencesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        k8sEnvVarYamlCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        k8sExecActionYamlCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        markdownCollapseFrontMatterCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        markdownCollapseLinksCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        markdownCollapseTablesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        markdownCollapseCodeFencesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        markdownCollapseTocCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        phpClassBodyCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        phpImportsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        phpMethodBodyCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        phpFunctionBodyCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        phpTagsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        phpHeredocCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        phpAttributeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        phpAttributeListCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        pythonLongStringLiteralsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        pythonLongCollectionLiteralsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        pythonSequentialCommentsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        pythonTypeAnnotationsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        rubyI18nStringsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        rustOneLineMethodsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        sqlUnderscoresInNumericLiteralsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        scalaBlockCommentsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaMethodCallBodiesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaTemplateDefinitionBodiesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaDefinitionBodiesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaTypeLambdasCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaPackagesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaMultiLineStringsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaCustomRegionsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaMultiLineBlocksCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaShowOutlineForMultiLineBlocksCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        xmlTagsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        xmlHtmlStyleAttributeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        xmlEntitiesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        xmlDataUrisCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        yamlLimitFoldedKeysAndValuesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        yamlLimitCharactersField.textProperty().addListener((obs, o, n) -> notifyModified());

        goOneLineIfErrorHandlingCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        goOneLineFunctionsSingleReturnCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        goOneLineCaseClausesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        goEmptyFunctionsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        goEmptyStructOrInterfaceCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        goFormattedStringsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
    }

    public void loadFromSettings(CodeFoldingSettings s) {
        suppressEvents = true;
        try {
            showFoldingArrowsCheck.setSelected(s.isShowFoldingArrows());
            foldingArrowsModeCombo.setValue(s.getCodeFoldingArrowsMode());
            showBottomArrowsCheck.setSelected(s.isShowBottomArrows());

            boolean arrowsEnabled = s.isShowFoldingArrows();
            foldingArrowsModeCombo.setDisable(!arrowsEnabled);
            showBottomArrowsCheck.setDisable(!arrowsEnabled);

            generalFileHeaderCheck.setSelected(s.isFoldFileHeader());
            generalImportsCheck.setSelected(s.isFoldImports());
            generalDocCommentsCheck.setSelected(s.isFoldDocComments());
            generalMethodBodiesCheck.setSelected(s.isFoldMethodBodies());
            generalCustomRegionsCheck.setSelected(s.isFoldCustomRegions());

            jpaQueriesCheck.setSelected(s.isFoldJpaQueries());

            jsonShowKeyCountCheck.setSelected(s.isJsonShowKeyCount());
            jsonShowFirstKeyCheck.setSelected(s.isJsonShowFirstKey());

            javaOneLineMethodsCheck.setSelected(s.isJavaOneLineMethods());
            javaSimplePropertyAccessorsCheck.setSelected(s.isJavaSimplePropertyAccessors());
            javaInnerClassesCheck.setSelected(s.isJavaInnerClasses());
            javaAnonymousClassesCheck.setSelected(s.isJavaAnonymousClasses());
            javaAnnotationsCheck.setSelected(s.isJavaAnnotations());
            javaClosuresCheck.setSelected(s.isJavaClosures());
            javaGenericParamsCheck.setSelected(s.isJavaGenericParams());
            javaReplaceVarCheck.setSelected(s.isJavaReplaceVar());
            javaI18nStringsCheck.setSelected(s.isJavaI18nStrings());
            javaSuppressWarningsCheck.setSelected(s.isJavaSuppressWarnings());
            javaEndOfLineCommentsCheck.setSelected(s.isJavaEndOfLineComments());
            javaMultilineCommentsCheck.setSelected(s.isJavaMultilineComments());

            jsOneLineFunctionsCheck.setSelected(s.isJsOneLineFunctions());
            jsObjectLiteralsCheck.setSelected(s.isJsObjectLiterals());
            jsArrayLiteralsCheck.setSelected(s.isJsArrayLiterals());
            jsXmlLiteralsCheck.setSelected(s.isJsXmlLiterals());

            k8sHelmValueReferencesCheck.setSelected(s.isK8sHelmValueReferences());
            k8sEnvVarYamlCheck.setSelected(s.isK8sEnvVarYaml());
            k8sExecActionYamlCheck.setSelected(s.isK8sExecActionYaml());

            markdownCollapseFrontMatterCheck.setSelected(s.isMarkdownCollapseFrontMatter());
            markdownCollapseLinksCheck.setSelected(s.isMarkdownCollapseLinks());
            markdownCollapseTablesCheck.setSelected(s.isMarkdownCollapseTables());
            markdownCollapseCodeFencesCheck.setSelected(s.isMarkdownCollapseCodeFences());
            markdownCollapseTocCheck.setSelected(s.isMarkdownCollapseToc());

            phpClassBodyCheck.setSelected(s.isPhpClassBody());
            phpImportsCheck.setSelected(s.isPhpImports());
            phpMethodBodyCheck.setSelected(s.isPhpMethodBody());
            phpFunctionBodyCheck.setSelected(s.isPhpFunctionBody());
            phpTagsCheck.setSelected(s.isPhpTags());
            phpHeredocCheck.setSelected(s.isPhpHeredoc());
            phpAttributeCheck.setSelected(s.isPhpAttribute());
            phpAttributeListCheck.setSelected(s.isPhpAttributeList());

            pythonLongStringLiteralsCheck.setSelected(s.isPythonLongStringLiterals());
            pythonLongCollectionLiteralsCheck.setSelected(s.isPythonLongCollectionLiterals());
            pythonSequentialCommentsCheck.setSelected(s.isPythonSequentialComments());
            pythonTypeAnnotationsCheck.setSelected(s.isPythonTypeAnnotations());

            rubyI18nStringsCheck.setSelected(s.isRubyI18nStrings());
            rustOneLineMethodsCheck.setSelected(s.isRustOneLineMethods());
            sqlUnderscoresInNumericLiteralsCheck.setSelected(s.isSqlUnderscoresInNumericLiterals());

            scalaBlockCommentsCheck.setSelected(s.isScalaBlockComments());
            scalaMethodCallBodiesCheck.setSelected(s.isScalaMethodCallBodies());
            scalaTemplateDefinitionBodiesCheck.setSelected(s.isScalaTemplateDefinitionBodies());
            scalaDefinitionBodiesCheck.setSelected(s.isScalaDefinitionBodies());
            scalaTypeLambdasCheck.setSelected(s.isScalaTypeLambdas());
            scalaPackagesCheck.setSelected(s.isScalaPackages());
            scalaMultiLineStringsCheck.setSelected(s.isScalaMultiLineStrings());
            scalaCustomRegionsCheck.setSelected(s.isScalaCustomRegions());
            scalaMultiLineBlocksCheck.setSelected(s.isScalaMultiLineBlocks());
            scalaShowOutlineForMultiLineBlocksCheck.setSelected(s.isScalaShowOutlineForMultiLineBlocks());

            xmlTagsCheck.setSelected(s.isXmlTags());
            xmlHtmlStyleAttributeCheck.setSelected(s.isXmlHtmlStyleAttribute());
            xmlEntitiesCheck.setSelected(s.isXmlEntities());
            xmlDataUrisCheck.setSelected(s.isXmlDataUris());

            yamlLimitFoldedKeysAndValuesCheck.setSelected(s.isYamlLimitFoldedKeysAndValues());
            yamlLimitCharactersField.setText(String.valueOf(s.getYamlLimitCharacters()));
            yamlLimitCharactersField.setDisable(!s.isYamlLimitFoldedKeysAndValues());

            goOneLineIfErrorHandlingCheck.setSelected(s.isGoOneLineIfErrorHandling());
            goOneLineFunctionsSingleReturnCheck.setSelected(s.isGoOneLineFunctionsSingleReturn());
            goOneLineCaseClausesCheck.setSelected(s.isGoOneLineCaseClauses());
            goEmptyFunctionsCheck.setSelected(s.isGoEmptyFunctions());
            goEmptyStructOrInterfaceCheck.setSelected(s.isGoEmptyStructOrInterface());
            goFormattedStringsCheck.setSelected(s.isGoFormattedStrings());
        } finally {
            suppressEvents = false;
        }
    }

    public void saveToSettings(CodeFoldingSettings s) {
        s.setShowFoldingArrows(showFoldingArrowsCheck.isSelected());
        if (foldingArrowsModeCombo.getValue() != null) {
            s.setCodeFoldingArrowsMode(foldingArrowsModeCombo.getValue());
        }
        s.setShowBottomArrows(showBottomArrowsCheck.isSelected());

        s.setFoldFileHeader(generalFileHeaderCheck.isSelected());
        s.setFoldImports(generalImportsCheck.isSelected());
        s.setFoldDocComments(generalDocCommentsCheck.isSelected());
        s.setFoldMethodBodies(generalMethodBodiesCheck.isSelected());
        s.setFoldCustomRegions(generalCustomRegionsCheck.isSelected());

        s.setFoldJpaQueries(jpaQueriesCheck.isSelected());

        s.setJsonShowKeyCount(jsonShowKeyCountCheck.isSelected());
        s.setJsonShowFirstKey(jsonShowFirstKeyCheck.isSelected());

        s.setJavaOneLineMethods(javaOneLineMethodsCheck.isSelected());
        s.setJavaSimplePropertyAccessors(javaSimplePropertyAccessorsCheck.isSelected());
        s.setJavaInnerClasses(javaInnerClassesCheck.isSelected());
        s.setJavaAnonymousClasses(javaAnonymousClassesCheck.isSelected());
        s.setJavaAnnotations(javaAnnotationsCheck.isSelected());
        s.setJavaClosures(javaClosuresCheck.isSelected());
        s.setJavaGenericParams(javaGenericParamsCheck.isSelected());
        s.setJavaReplaceVar(javaReplaceVarCheck.isSelected());
        s.setJavaI18nStrings(javaI18nStringsCheck.isSelected());
        s.setJavaSuppressWarnings(javaSuppressWarningsCheck.isSelected());
        s.setJavaEndOfLineComments(javaEndOfLineCommentsCheck.isSelected());
        s.setJavaMultilineComments(javaMultilineCommentsCheck.isSelected());

        s.setJsOneLineFunctions(jsOneLineFunctionsCheck.isSelected());
        s.setJsObjectLiterals(jsObjectLiteralsCheck.isSelected());
        s.setJsArrayLiterals(jsArrayLiteralsCheck.isSelected());
        s.setJsXmlLiterals(jsXmlLiteralsCheck.isSelected());

        s.setK8sHelmValueReferences(k8sHelmValueReferencesCheck.isSelected());
        s.setK8sEnvVarYaml(k8sEnvVarYamlCheck.isSelected());
        s.setK8sExecActionYaml(k8sExecActionYamlCheck.isSelected());

        s.setMarkdownCollapseFrontMatter(markdownCollapseFrontMatterCheck.isSelected());
        s.setMarkdownCollapseLinks(markdownCollapseLinksCheck.isSelected());
        s.setMarkdownCollapseTables(markdownCollapseTablesCheck.isSelected());
        s.setMarkdownCollapseCodeFences(markdownCollapseCodeFencesCheck.isSelected());
        s.setMarkdownCollapseToc(markdownCollapseTocCheck.isSelected());

        s.setPhpClassBody(phpClassBodyCheck.isSelected());
        s.setPhpImports(phpImportsCheck.isSelected());
        s.setPhpMethodBody(phpMethodBodyCheck.isSelected());
        s.setPhpFunctionBody(phpFunctionBodyCheck.isSelected());
        s.setPhpTags(phpTagsCheck.isSelected());
        s.setPhpHeredoc(phpHeredocCheck.isSelected());
        s.setPhpAttribute(phpAttributeCheck.isSelected());
        s.setPhpAttributeList(phpAttributeListCheck.isSelected());

        s.setPythonLongStringLiterals(pythonLongStringLiteralsCheck.isSelected());
        s.setPythonLongCollectionLiterals(pythonLongCollectionLiteralsCheck.isSelected());
        s.setPythonSequentialComments(pythonSequentialCommentsCheck.isSelected());
        s.setPythonTypeAnnotations(pythonTypeAnnotationsCheck.isSelected());

        s.setRubyI18nStrings(rubyI18nStringsCheck.isSelected());
        s.setRustOneLineMethods(rustOneLineMethodsCheck.isSelected());
        s.setSqlUnderscoresInNumericLiterals(sqlUnderscoresInNumericLiteralsCheck.isSelected());

        s.setScalaBlockComments(scalaBlockCommentsCheck.isSelected());
        s.setScalaMethodCallBodies(scalaMethodCallBodiesCheck.isSelected());
        s.setScalaTemplateDefinitionBodies(scalaTemplateDefinitionBodiesCheck.isSelected());
        s.setScalaDefinitionBodies(scalaDefinitionBodiesCheck.isSelected());
        s.setScalaTypeLambdas(scalaTypeLambdasCheck.isSelected());
        s.setScalaPackages(scalaPackagesCheck.isSelected());
        s.setScalaMultiLineStrings(scalaMultiLineStringsCheck.isSelected());
        s.setScalaCustomRegions(scalaCustomRegionsCheck.isSelected());
        s.setScalaMultiLineBlocks(scalaMultiLineBlocksCheck.isSelected());
        s.setScalaShowOutlineForMultiLineBlocks(scalaShowOutlineForMultiLineBlocksCheck.isSelected());

        s.setXmlTags(xmlTagsCheck.isSelected());
        s.setXmlHtmlStyleAttribute(xmlHtmlStyleAttributeCheck.isSelected());
        s.setXmlEntities(xmlEntitiesCheck.isSelected());
        s.setXmlDataUris(xmlDataUrisCheck.isSelected());

        s.setYamlLimitFoldedKeysAndValues(yamlLimitFoldedKeysAndValuesCheck.isSelected());
        try {
            s.setYamlLimitCharacters(Integer.parseInt(yamlLimitCharactersField.getText().trim()));
        } catch (NumberFormatException ignored) {}

        s.setGoOneLineIfErrorHandling(goOneLineIfErrorHandlingCheck.isSelected());
        s.setGoOneLineFunctionsSingleReturn(goOneLineFunctionsSingleReturnCheck.isSelected());
        s.setGoOneLineCaseClauses(goOneLineCaseClausesCheck.isSelected());
        s.setGoEmptyFunctions(goEmptyFunctionsCheck.isSelected());
        s.setGoEmptyStructOrInterface(goEmptyStructOrInterfaceCheck.isSelected());
        s.setGoFormattedStrings(goFormattedStringsCheck.isSelected());
    }

    public boolean isModified() {
        CodeFoldingSettings current = new CodeFoldingSettings();
        saveToSettings(current);
        return current.isModified(CodeFoldingSettings.getInstance());
    }

    public void apply() {
        saveToSettings(CodeFoldingSettings.getInstance());
        CodeFoldingSettings.getInstance().save();
    }

    public void reset() {
        loadFromSettings(CodeFoldingSettings.getInstance());
    }
}