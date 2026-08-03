// SettingsPostfixCompletionPage.java
package dev.lumina.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;

/**
 * IntelliJ-style Editor > General > Postfix Completion settings page.
 * Complete implementation matching all screenshots.
 */
public class SettingsPostfixCompletionPage extends VBox {

    private final ListView<String> languageList = new ListView<>();
    private final ListView<String> templateList = new ListView<>();
    private final Label beforePreview = new Label();
    private final Label afterPreview = new Label();
    private final Label selectedTemplateLabel = new Label();

    private final Map<String, ObservableList<String>> languageTemplates = new HashMap<>();

    public SettingsPostfixCompletionPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Top section: Enable postfix completion
        // ============================================================
        CheckBox enablePostfix = new CheckBox("Enable postfix completion");
        enablePostfix.setSelected(true);
        enablePostfix.getStyleClass().add("settings-check");

        CheckBox showAsCommand = new CheckBox("Show postfix completions as command completions");
        showAsCommand.setSelected(true);
        showAsCommand.getStyleClass().add("settings-check");

        VBox topBox = new VBox(4);
        topBox.setPadding(new Insets(4, 0, 8, 20));
        topBox.getChildren().addAll(enablePostfix, showAsCommand);

        // ============================================================
        // Expand templates with
        // ============================================================
        HBox expandRow = new HBox(8);
        expandRow.setPadding(new Insets(4, 0, 8, 20));
        expandRow.setAlignment(Pos.CENTER_LEFT);

        Label expandLabel = new Label("Expand templates with");
        expandLabel.getStyleClass().add("settings-label");
        ComboBox<String> expandCombo = new ComboBox<>();
        expandCombo.getItems().addAll("Tab", "Space", "Enter");
        expandCombo.getSelectionModel().selectFirst();
        expandCombo.getStyleClass().add("settings-combo");
        expandCombo.setPrefWidth(120);
        expandRow.getChildren().addAll(expandLabel, expandCombo);

        // ============================================================
        // Main layout: Language list + Template list + Preview
        // ============================================================
        Label languagesLabel = new Label("Languages");
        languagesLabel.getStyleClass().add("settings-section");

        // Language list on the left
        languageList.getStyleClass().add("settings-list");
        languageList.setPrefHeight(280);
        languageList.setPrefWidth(150);
        languageList.getItems().addAll(
            "TypeScript",
            "Rust",
            "Java",
            "Kotlin",
            "JavaScript",
            "JVM languages",
            "SQL"
        );
        languageList.getSelectionModel().select("Java");

        // Template list on the right
        templateList.getStyleClass().add("settings-list");
        templateList.setPrefHeight(280);
        templateList.setPrefWidth(300);

        // Populate templates for each language
        populateTemplates();

        // Update template list when language changes
        languageList.getSelectionModel().selectedItemProperty().addListener((obs, old, lang) -> {
            if (lang != null) {
                templateList.setItems(languageTemplates.getOrDefault(lang, FXCollections.observableArrayList()));
                updatePreview();
            }
        });

        // Update preview when template selection changes
        templateList.getSelectionModel().selectedItemProperty().addListener((obs, old, template) -> {
            updatePreview();
        });

        // Language and template split
        HBox listSplit = new HBox(16);
        listSplit.setPadding(new Insets(4, 0, 8, 0));
        VBox langBox = new VBox(6, new Label("Language:"), languageList);
        VBox templateBox = new VBox(6, new Label("Templates:"), templateList);
        HBox.setHgrow(templateBox, Priority.ALWAYS);
        listSplit.getChildren().addAll(langBox, templateBox);

        // ============================================================
        // Language description
        // ============================================================
        Label descLabel = new Label("You have selected the postfix completion language.");
        descLabel.getStyleClass().add("settings-label");
        descLabel.setWrapText(true);

        Label descLabel2 = new Label("By clicking the checkbox, you can enable/disable all postfix templates for the language.");
        descLabel2.getStyleClass().add("settings-hint");
        descLabel2.setWrapText(true);

        Label descLabel3 = new Label("To enable/disable a postfix template select it inside the group.");
        descLabel3.getStyleClass().add("settings-hint");
        descLabel3.setWrapText(true);

        VBox descBox = new VBox(4);
        descBox.setPadding(new Insets(8, 0, 8, 0));
        descBox.getChildren().addAll(descLabel, descLabel2, descLabel3);

        // ============================================================
        // Before/After preview section
        // ============================================================
        Label beforeLabel = new Label("Before:");
        beforeLabel.getStyleClass().add("settings-section");

        beforePreview.getStyleClass().add("settings-preview");
        beforePreview.setWrapText(true);
        beforePreview.setPadding(new Insets(8, 12, 8, 12));
        beforePreview.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label afterLabel = new Label("After:");
        afterLabel.getStyleClass().add("settings-section");

        afterPreview.getStyleClass().add("settings-preview");
        afterPreview.setWrapText(true);
        afterPreview.setPadding(new Insets(8, 12, 8, 12));
        afterPreview.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");

        // Preview hints
        Label hint1 = new Label("1. The sample code featuring selected template will be shown here.");
        hint1.getStyleClass().add("settings-hint");
        hint1.setPadding(new Insets(4, 0, 0, 0));

        Label hint2 = new Label("2. Flashing rectangle shows the place where the intention is applicable.");
        hint2.getStyleClass().add("settings-hint");
        hint2.setPadding(new Insets(2, 0, 0, 0));

        Label hint3 = new Label("1. Postfix completion invocation result will be shown here.");
        hint3.getStyleClass().add("settings-hint");
        hint3.setPadding(new Insets(8, 0, 0, 0));

        VBox previewBox = new VBox(4);
        previewBox.setPadding(new Insets(4, 0, 8, 20));
        previewBox.getChildren().addAll(
            beforeLabel,
            beforePreview,
            hint1,
            hint2,
            afterLabel,
            afterPreview,
            hint3
        );

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            topBox,
            expandRow,
            languagesLabel,
            listSplit,
            descBox,
            previewBox
        );

        // Set initial selection and preview
        languageList.getSelectionModel().select("Java");
        templateList.getSelectionModel().selectFirst();
        updatePreview();
    }

    private void populateTemplates() {
        // Java templates
        ObservableList<String> javaTemplates = FXCollections.observableArrayList(
            "cast(<any> value)",
            "!lexpr",
            "arg functionCall(expr)",
            "assert assert expr",
            "cast ((SomeType) expr)",
            "castvar T name = (T) expr",
            "else if (!expr)",
            "field myField = expr",
            "for for (T item : expr)",
            "fori for (int i = 0; i < expr.length; i++)",
            "format String.format(expr)",
            "forr for (int i = expr.length - 1; i >= 0; i--)",
            "if if (expr)",
            "inst expr instanceof Type ? ((Type) expr) : null",
            "iter for (T item : expr)",
            "lambda () -> expr",
            "new new T()",
            "nn if (expr != null)",
            "not !expr",
            "notnull if (expr != null)",
            "null if (expr == null)",
            "opt Optional.ofNullable(expr)",
            "par (expr)",
            "reqnonnull Objects.requireNonNull(expr)",
            "return return expr",
            "ser System.err.println(expr)",
            "souf System.out.printf(\"%?\", expr)",
            "sout System.out.println(expr)"
        );
        languageTemplates.put("Java", javaTemplates);

        // TypeScript templates
        ObservableList<String> tsTemplates = FXCollections.observableArrayList(
            "cast(<any> value)",
            "!lexpr",
            "arg functionCall(expr)",
            "assert assert expr",
            "await await expr",
            "const const name = expr",
            "destruct let {} = expr",
            "destructAll let {x: [first, second]} = {x: [1, 2]}",
            "dforof for (let {p: [first]} of expr)",
            "else if (!expr)",
            "fori for (let i = 0; i < expr.length; i++)",
            "forin for(var obj in expr)",
            "forof for (let obj of expr)",
            "forr for (let i = expr.length - 1; i >= 0; i--)",
            "if if (expr)",
            "instanceof if(x instanceof x)",
            "itin for(var obj in expr)",
            "let let name = expr",
            "log console.log(expr)",
            "not !expr",
            "notnull if (expr != null)",
            "null if (expr == null)",
            "par (expr)",
            "return return expr",
            "switch switch (x) {...}",
            "throw throw expr",
            "typeof typeof expr",
            "typeofif if(typeof x == \"string\")",
            "undeif if (expr != \"undefined\")",
            "var var name = expr"
        );
        languageTemplates.put("TypeScript", tsTemplates);

        // Rust templates
        ObservableList<String> rustTemplates = FXCollections.observableArrayList(
            "assert!(expr);",
            "dbg(expr);",
            "dbgr dbg(&expr)",
            "debug_assert!(expr);",
            "deref *expr",
            "else if (!expr) {}",
            "err Err(expr)",
            "for for x in expr {}",
            "iter for x in expr {}",
            "lambda || expr",
            "let let name = expr;",
            "match match expr { }",
            "not !expr",
            "ok Ok(expr)",
            "par (expr)",
            "println!(\"%?\", expr);",
            "ref &expr",
            "ref &type",
            "refm &mut expr",
            "refm &mut type",
            "slice &list[i..j]",
            "some Some(expr)",
            "sublist &list[i..j]",
            "while while expr {}",
            "whilenot while !expr {}",
            "wrap $wrapper<$path>"
        );
        languageTemplates.put("Rust", rustTemplates);

        // Kotlin templates
        ObservableList<String> kotlinTemplates = FXCollections.observableArrayList(
            "arg functionCall(expr)",
            "arrayOf arrayOf(expr)",
            "assert assert(expr)",
            "else if (!expr) {}",
            "for (for (item in expr) {})",
            "fori (for (i in 0 until number) {})",
            "forit (for (index, name) in expr.withIndex()) {})",
            "forr (for (i in number downTo 0) {})",
            "forrev (for (item in expr.reversed()) {})",
            "if if (expr) {}",
            "iter for (item in expr) {}",
            "listOf listOf(expr)",
            "nn if (expr != null) {}",
            "not !expr",
            "notnull if (expr != null) {}",
            "null if (expr == null) {}",
            "par (expr)",
            "return return expr",
            "sequenceOf sequenceOf(expr)",
            "setOf setOf(expr)",
            "sout println(expr)",
            "spread *expr",
            "try try { expr } catch (e: Exception) {}",
            "unless if (!expr) {}",
            "val val name = expression",
            "var var name = expression",
            "when when (expr) {}",
            "while while (expr) {}",
            "with with(expr) {}"
        );
        languageTemplates.put("Kotlin", kotlinTemplates);

        // JavaScript templates
        ObservableList<String> jsTemplates = FXCollections.observableArrayList(
            "!lexpr",
            "arg functionCall(expr)",
            "await await expr",
            "const const name = expr",
            "destruct let {} = expr",
            "destructAll let {x: [first, second]} = {x: [1, 2]}",
            "dforof for (let {p: [first]} of expr)",
            "else if (!expr)",
            "fori for (let i = 0; i < expr.length; i++)",
            "forin for(var obj in expr)",
            "forof for (let obj of expr)",
            "forr for (let i = expr.length - 1; i >= 0; i--)",
            "if if (expr)",
            "instanceof if(x instanceof x)",
            "itin for(var obj in expr)",
            "let let name = expr",
            "log console.log(expr)",
            "not !expr",
            "notnull if (expr != null)",
            "null if (expr == null)",
            "par (expr)",
            "return return expr",
            "switch switch (x) {...}",
            "throw throw expr",
            "typeof typeof expr",
            "typeofif if(typeof x == \"string\")",
            "undeif if (expr != \"undefined\")",
            "var var name = expr"
        );
        languageTemplates.put("JavaScript", jsTemplates);

        // JVM languages templates
        ObservableList<String> jvmTemplates = FXCollections.observableArrayList(
            "autowire @Autowired T t",
            "inject @Inject T t",
            "arg functionCall(expr)",
            "cast expr as SomeType",
            "def def name = expr",
            "else if (!expr)",
            "filter expr.findAll()",
            "flatMap expr.collectMany()",
            "foldLeft expr.inject() {}",
            "for for (final e in expr)",
            "if if (expr)",
            "iter for (final e in expr)",
            "map expr.collect()",
            "new newExpr()",
            "nn if (expr != null)",
            "not !expr",
            "notnull if (expr != null)",
            "null if (expr == null)",
            "par (expr)",
            "reduce expr.inject()",
            "reqnonnull Objects.requireNonNull(expr)",
            "return return expr",
            "ser System.err.println(expr)",
            "sout print(expr)",
            "throw throw expr",
            "try try { expr } catch (e)",
            "var def name = expr",
            "while while (expr)"
        );
        languageTemplates.put("JVM languages", jvmTemplates);

        // SQL templates
        ObservableList<String> sqlTemplates = FXCollections.observableArrayList(
            "afrom select [c1 as a1, ..] from authors",
            "cfrom select [all columns] from authors",
            "from select * from authors",
            "join select * from authors join on |"
        );
        languageTemplates.put("SQL", sqlTemplates);
    }

    private void updatePreview() {
        String selectedLang = languageList.getSelectionModel().getSelectedItem();
        String selectedTemplate = templateList.getSelectionModel().getSelectedItem();

        if (selectedTemplate != null) {
            // Show the template as the preview
            String previewText = selectedTemplate;

            // Generate before/after examples based on the template
            String before = "expr" + selectedTemplate.substring(0, Math.min(selectedTemplate.length(), 20)) + "...";
            String after = generateAfterExample(selectedTemplate);

            beforePreview.setText(before);
            afterPreview.setText(after);
        } else {
            beforePreview.setText("Select a template to preview");
            afterPreview.setText("Select a template to preview");
        }
    }

    private String generateAfterExample(String template) {
        // Simple mapping of template to after example
        if (template.contains("sout")) {
            return "System.out.println(expr);";
        } else if (template.contains("for")) {
            return "for (T item : expr) {\n    // ...\n}";
        } else if (template.contains("if")) {
            return "if (expr) {\n    // ...\n}";
        } else if (template.contains("cast")) {
            return "(SomeType) expr";
        } else if (template.contains("notnull") || template.contains("nn")) {
            return "if (expr != null) {\n    // ...\n}";
        } else if (template.contains("null")) {
            return "if (expr == null) {\n    // ...\n}";
        } else if (template.contains("par")) {
            return "(expr)";
        } else if (template.contains("return")) {
            return "return expr;";
        } else if (template.contains("assert")) {
            return "assert expr;";
        } else if (template.contains("lambda")) {
            return "() -> expr";
        } else if (template.contains("new")) {
            return "new T()";
        } else if (template.contains("arg")) {
            return "functionCall(expr)";
        } else if (template.contains("await")) {
            return "await expr";
        } else if (template.contains("const") || template.contains("let") || template.contains("var")) {
            return "let name = expr;";
        } else if (template.contains("log")) {
            return "console.log(expr);";
        } else if (template.contains("typeof")) {
            return "typeof expr";
        } else if (template.contains("throw")) {
            return "throw expr;";
        } else if (template.contains("switch")) {
            return "switch (expr) {\n    // ...\n}";
        } else if (template.contains("arrayOf") || template.contains("listOf") || template.contains("setOf")) {
            return "arrayOf(expr)";
        } else if (template.contains("when")) {
            return "when (expr) {\n    // ...\n}";
        } else if (template.contains("autowire")) {
            return "@Autowired\nprivate T t;";
        } else if (template.contains("inject")) {
            return "@Inject\nprivate T t;";
        } else if (template.contains("filter")) {
            return "expr.findAll()";
        } else if (template.contains("map")) {
            return "expr.collect()";
        } else if (template.contains("from") || template.contains("select")) {
            return "SELECT * FROM expr";
        } else if (template.contains("dbg")) {
            return "dbg!(expr);";
        } else if (template.contains("match")) {
            return "match expr {\n    // ...\n}";
        } else if (template.contains("slice")) {
            return "&list[i..j]";
        } else if (template.contains("some")) {
            return "Some(expr)";
        } else if (template.contains("ok")) {
            return "Ok(expr)";
        } else if (template.contains("err")) {
            return "Err(expr)";
        } else if (template.contains("println")) {
            return "println!(\"%?\", expr);";
        } else if (template.contains("deref")) {
            return "*expr";
        } else {
            return "// After: " + template;
        }
    }
}