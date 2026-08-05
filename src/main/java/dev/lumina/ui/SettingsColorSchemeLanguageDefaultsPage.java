// SettingsColorSchemeLanguageDefaultsPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * IntelliJ-style Editor > Color Scheme > Language Defaults page.
 * Complete implementation matching IntelliJ IDEA screenshots.
 */
public class SettingsColorSchemeLanguageDefaultsPage extends VBox {

    private Label selectedCategoryLabel;

    public SettingsColorSchemeLanguageDefaultsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(16);

        // ============================================================
        // Header with Scheme dropdown
        // ============================================================
        HBox schemeRow = new HBox(12);
        schemeRow.setAlignment(Pos.CENTER_LEFT);

        Label schemeLabel = new Label("Scheme:");
        schemeLabel.getStyleClass().add("settings-label");

        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.getItems().addAll("Islands Dark Theme default", "Darcula", "IntelliJ Light");
        schemeCombo.getSelectionModel().selectFirst();
        schemeCombo.getStyleClass().add("settings-combo");
        schemeCombo.setPrefWidth(260);

        Hyperlink themeLink = new Hyperlink("Change IDE Theme...");
        themeLink.getStyleClass().add("settings-link");

        schemeRow.getChildren().addAll(schemeLabel, schemeCombo, themeLink);

        // ============================================================
        // Main content: Categories on left, Preview on right
        // ============================================================
        HBox mainContent = new HBox(20);
        mainContent.setPadding(new Insets(12, 0, 0, 0));
        mainContent.setAlignment(Pos.TOP_LEFT);

        // ---- Left: Category list ----
        VBox categoryBox = new VBox(2);
        categoryBox.setPrefWidth(200);
        categoryBox.setMinWidth(180);

        String[] categories = {
            "Bad character",
            "Braces and Operators",
            "Classes",
            "Comments",
            "Identifiers",
            "Inline hints",
            "Keyword",
            "Markup",
            "Metadata",
            "Number",
            "Semantic highlighting",
            "String",
            "Template language"
        };

        // Store reference to the selected category label for updating preview
        selectedCategoryLabel = new Label("Bad character");
        selectedCategoryLabel.getStyleClass().add("settings-label");
        selectedCategoryLabel.setStyle("-fx-text-fill: #E8B450; -fx-font-weight: bold;");

        VBox categoryList = new VBox(2);
        for (String cat : categories) {
            Label item = new Label(cat);
            item.getStyleClass().add("settings-label");
            if (cat.equals("Bad character")) {
                item.setStyle("-fx-text-fill: #E8B450; -fx-font-weight: bold; -fx-background-color: #2A2416; -fx-background-radius: 4;");
            } else {
                item.setStyle("-fx-text-fill: #B9BECF;");
            }
            item.setPadding(new Insets(4, 10, 4, 10));
            final String categoryName = cat;
            item.setOnMouseClicked(e -> {
                // Update selection
                for (javafx.scene.Node node : categoryList.getChildren()) {
                    Label l = (Label) node;
                    l.setStyle("-fx-text-fill: #B9BECF;");
                    l.setStyle("-fx-background-color: transparent;");
                }
                item.setStyle("-fx-text-fill: #E8B450; -fx-font-weight: bold; -fx-background-color: #2A2416; -fx-background-radius: 4;");
                selectedCategoryLabel.setText(categoryName);
                updatePreview(categoryName);
            });
            item.setOnMouseEntered(e -> {
                if (!item.getStyle().contains("#E8B450")) {
                    item.setStyle("-fx-background-color: #1F2230; -fx-text-fill: #D8DBE6; -fx-background-radius: 4;");
                }
            });
            item.setOnMouseExited(e -> {
                if (!item.getStyle().contains("#E8B450")) {
                    item.setStyle("-fx-text-fill: #B9BECF; -fx-background-color: transparent;");
                }
            });
            categoryList.getChildren().add(item);
        }

        categoryBox.getChildren().addAll(categoryList);

        // ---- Right: Preview area ----
        VBox previewBox = new VBox(10);
        previewBox.setPrefWidth(560);
        previewBox.setMinWidth(400);
        HBox.setHgrow(previewBox, Priority.ALWAYS);

        Label previewTitle = new Label("Preview");
        previewTitle.getStyleClass().add("settings-section");

        // Code preview area with sample syntax
        VBox codeArea = new VBox(3);
        codeArea.getStyleClass().add("code-area");
        codeArea.setPadding(new Insets(14));
        codeArea.setSpacing(4);
        codeArea.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6;");
        codeArea.setPrefHeight(400);

        // We'll add the preview content dynamically
        updatePreviewContent(codeArea, "Bad character");

        ScrollPane codeScroll = new ScrollPane(codeArea);
        codeScroll.setFitToWidth(true);
        codeScroll.setPrefViewportHeight(380);
        codeScroll.getStyleClass().add("settings-scroll");
        codeScroll.setStyle("-fx-background-color: #1F2230;");

        previewBox.getChildren().addAll(previewTitle, codeScroll);

        mainContent.getChildren().addAll(categoryBox, previewBox);
        HBox.setHgrow(previewBox, Priority.ALWAYS);

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(schemeRow, mainContent);
    }

    private void updatePreview(String category) {
        // Find the code area in the scene and update it
        // This is called when a category is clicked
        for (javafx.scene.Node node : getChildren()) {
            if (node instanceof HBox) {
                HBox main = (HBox) node;
                for (javafx.scene.Node child : main.getChildren()) {
                    if (child instanceof VBox) {
                        VBox rightPanel = (VBox) child;
                        if (rightPanel.getPrefWidth() > 300) {
                            for (javafx.scene.Node inner : rightPanel.getChildren()) {
                                if (inner instanceof ScrollPane) {
                                    ScrollPane scroll = (ScrollPane) inner;
                                    javafx.scene.Node content = scroll.getContent();
                                    if (content instanceof VBox) {
                                        VBox codeArea = (VBox) content;
                                        updatePreviewContent(codeArea, category);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updatePreviewContent(VBox codeArea, String category) {
        codeArea.getChildren().clear();

        // Based on the category, show different highlighted samples
        if (category.equals("Bad character")) {
            addBadCharacterPreview(codeArea);
        } else if (category.equals("Braces and Operators")) {
            addBracesOperatorsPreview(codeArea);
        } else if (category.equals("Classes")) {
            addClassesPreview(codeArea);
        } else if (category.equals("Comments")) {
            addCommentsPreview(codeArea);
        } else if (category.equals("Identifiers")) {
            addIdentifiersPreview(codeArea);
        } else if (category.equals("Inline hints")) {
            addInlineHintsPreview(codeArea);
        } else if (category.equals("Keyword")) {
            addKeywordPreview(codeArea);
        } else if (category.equals("Markup")) {
            addMarkupPreview(codeArea);
        } else if (category.equals("Metadata")) {
            addMetadataPreview(codeArea);
        } else if (category.equals("Number")) {
            addNumberPreview(codeArea);
        } else if (category.equals("Semantic highlighting")) {
            addSemanticHighlightingPreview(codeArea);
        } else if (category.equals("String")) {
            addStringPreview(codeArea);
        } else if (category.equals("Template language")) {
            addTemplateLanguagePreview(codeArea);
        }
    }

    private void addBadCharacterPreview(VBox codeArea) {
        // Bad character: ????? shown with string highlighting
        addLine(codeArea, new String[]{"Bad character: ", "sx-comment"}, new String[]{"?????", "sx-string"});
        addLine(codeArea, new String[]{"Keyword ", "sx-keyword"}, new String[]{"Identifier ", "sx-type"}, new String[]{"String ", "sx-string"}, new String[]{"\\n\\?", "sx-string"});
        addLine(codeArea, new String[]{"12345", "sx-number"}, new String[]{" Operator ", "sx-paren"}, new String[]{"Dot: . comma: , semicolon: ;", "sx-paren"});
        addLine(codeArea, new String[]{"{ Braces } ", "sx-paren"}, new String[]{"( Parentheses ) ", "sx-paren"}, new String[]{"[ Brackets ]", "sx-paren"});
        addLine(codeArea, new String[]{"// Line comment", "sx-comment"});
        addLine(codeArea, new String[]{"/* Block comment */", "sx-comment"});
        addLine(codeArea, new String[]{":Label ", "sx-annotation"}, new String[]{"predefined_symbol()", "sx-method"});
        addLine(codeArea, new String[]{"CONSTANT", "sx-const"}, new String[]{" Global variable", "sx-field"});
    }

    private void addBracesOperatorsPreview(VBox codeArea) {
        addLine(codeArea, new String[]{"{ Braces } ", "sx-paren"}, new String[]{"( Parentheses ) ", "sx-paren"}, new String[]{"[ Brackets ]", "sx-paren"});
        addLine(codeArea, new String[]{"Operator ", "sx-paren"}, new String[]{"+ - * / = . , ; ! ?", "sx-paren"});
        addLine(codeArea, new String[]{"// Line comment", "sx-comment"});
        addLine(codeArea, new String[]{"/* Block comment */", "sx-comment"});
    }

    private void addClassesPreview(VBox codeArea) {
        addLine(codeArea, new String[]{"class ", "sx-keyword"}, new String[]{"MyClass ", "sx-type"}, new String[]{"extends ", "sx-keyword"}, new String[]{"BaseClass", "sx-type"});
        addLine(codeArea, new String[]{"interface ", "sx-keyword"}, new String[]{"MyInterface", "sx-type"});
        addLine(codeArea, new String[]{"enum ", "sx-keyword"}, new String[]{"Status", "sx-type"});
        addLine(codeArea, new String[]{"record ", "sx-keyword"}, new String[]{"Person", "sx-type"});
        addLine(codeArea, new String[]{"@Metadata", "sx-annotation"});
        addLine(codeArea, new String[]{"Class Name", "sx-type"});
        addLine(codeArea, new String[]{"  instance method", "sx-method"});
        addLine(codeArea, new String[]{"  instance field", "sx-field"});
        addLine(codeArea, new String[]{"  static method", "sx-method"});
        addLine(codeArea, new String[]{"  static field", "sx-field"});
    }

    private void addCommentsPreview(VBox codeArea) {
        addLine(codeArea, new String[]{"// Single line comment", "sx-comment"});
        addLine(codeArea, new String[]{"/* Multi-line", "sx-comment"});
        addLine(codeArea, new String[]{"   comment block */", "sx-comment"});
        addLine(codeArea, new String[]{"/**", "sx-comment"});
        addLine(codeArea, new String[]{" * Doc comment", "sx-comment"});
        addLine(codeArea, new String[]{" * @tag ", "sx-comment"}, new String[]{"<code>Markup</code>", "sx-string"});
        addLine(codeArea, new String[]{" */", "sx-comment"});
        addLine(codeArea, new String[]{"Rendered documentation with link", "sx-comment"});
    }

    private void addIdentifiersPreview(VBox codeArea) {
        addLine(codeArea, new String[]{"Identifier ", "sx-type"}, new String[]{"variableName", "sx-field"});
        addLine(codeArea, new String[]{"Function ", "sx-method"}, new String[]{"call(param1, param2)", "sx-paren"});
        addLine(codeArea, new String[]{"CONSTANT", "sx-const"});
        addLine(codeArea, new String[]{"Global variable", "sx-field"});
        addLine(codeArea, new String[]{":Label", "sx-annotation"});
        addLine(codeArea, new String[]{"predefined_symbol()", "sx-method"});
        addLine(codeArea, new String[]{"instance method", "sx-method"});
        addLine(codeArea, new String[]{"instance field", "sx-field"});
        addLine(codeArea, new String[]{"static method", "sx-method"});
        addLine(codeArea, new String[]{"static field", "sx-field"});
    }

    private void addInlineHintsPreview(VBox codeArea) {
        addLine(codeArea, new String[]{"var ", "sx-keyword"}, new String[]{"result", "sx-field"}, new String[]{" = ", "sx-paren"}, new String[]{"compute", "sx-method"}, new String[]{"()", "sx-paren"});
        addLine(codeArea, new String[]{"// Inline hint: ", "sx-comment"}, new String[]{"String", "sx-type"});
        addLine(codeArea, new String[]{"Parameter ", "sx-field"}, new String[]{": ", "sx-paren"}, new String[]{"String", "sx-type"});
        addLine(codeArea, new String[]{"Local variable ", "sx-field"}, new String[]{": ", "sx-paren"}, new String[]{"int", "sx-type"});
    }

    private void addKeywordPreview(VBox codeArea) {
        String[] keywords = {"abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"};

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(20, keywords.length); i++) {
            sb.append(keywords[i]).append(" ");
        }
        addLine(codeArea, new String[]{sb.toString(), "sx-keyword"});
        addLine(codeArea, new String[]{"// Keywords are highlighted in ", "sx-comment"}, new String[]{"bold amber", "sx-keyword"});
        addLine(codeArea, new String[]{"public ", "sx-keyword"}, new String[]{"class ", "sx-keyword"}, new String[]{"MyClass ", "sx-type"}, new String[]{"extends ", "sx-keyword"}, new String[]{"Base", "sx-type"});
    }

    private void addMarkupPreview(VBox codeArea) {
        addLine(codeArea, new String[]{"<html>", "sx-string"});
        addLine(codeArea, new String[]{"  <body>", "sx-string"});
        addLine(codeArea, new String[]{"    <h1>", "sx-string"}, new String[]{"Title", "sx-type"}, new String[]{"</h1>", "sx-string"});
        addLine(codeArea, new String[]{"    <p>", "sx-string"}, new String[]{"Paragraph text", "sx-type"}, new String[]{"</p>", "sx-string"});
        addLine(codeArea, new String[]{"  </body>", "sx-string"});
        addLine(codeArea, new String[]{"</html>", "sx-string"});
        addLine(codeArea, new String[]{"* @tag ", "sx-comment"}, new String[]{"<code>Markup</code>", "sx-string"});
    }

    private void addMetadataPreview(VBox codeArea) {
        addLine(codeArea, new String[]{"@Metadata", "sx-annotation"});
        addLine(codeArea, new String[]{"@Deprecated", "sx-annotation"});
        addLine(codeArea, new String[]{"@Override", "sx-annotation"});
        addLine(codeArea, new String[]{"@SuppressWarnings", "sx-annotation"});
        addLine(codeArea, new String[]{"@TA6 ", "sx-annotation"}, new String[]{"attribute = ", "sx-paren"}, new String[]{"Value", "sx-string"});
        addLine(codeArea, new String[]{"Entity: ", "sx-paren"}, new String[]{"&amp;", "sx-string"});
        addLine(codeArea, new String[]{"{% ", "sx-paren"}, new String[]{"Template language", "sx-string"}, new String[]{" %}", "sx-paren"});
    }

    private void addNumberPreview(VBox codeArea) {
        addLine(codeArea, new String[]{"12345", "sx-number"});
        addLine(codeArea, new String[]{"3.14159", "sx-number"});
        addLine(codeArea, new String[]{"0x1A", "sx-number"});
        addLine(codeArea, new String[]{"1_000_000", "sx-number"});
        addLine(codeArea, new String[]{"2.5e-3", "sx-number"});
        addLine(codeArea, new String[]{"// Numbers are highlighted in ", "sx-comment"}, new String[]{"teal", "sx-number"});
        addLine(codeArea, new String[]{"int ", "sx-keyword"}, new String[]{"count", "sx-field"}, new String[]{" = ", "sx-paren"}, new String[]{"42", "sx-number"});
        addLine(codeArea, new String[]{"double ", "sx-keyword"}, new String[]{"pi", "sx-field"}, new String[]{" = ", "sx-paren"}, new String[]{"3.14159", "sx-number"});
    }

    private void addSemanticHighlightingPreview(VBox codeArea) {
        addLine(codeArea, new String[]{"Semantic highlighting:", "sx-comment"});
        addLine(codeArea, new String[]{"Generated spectrum to pick colors for local variables and parameters", "sx-comment"});
        addLine(codeArea, new String[]{"Color#1: ", "sx-keyword"}, new String[]{"SC1.1 ", "sx-number"}, new String[]{"SC1.2 ", "sx-number"}, new String[]{"SC1.3 ", "sx-number"}, new String[]{"SC1.4 ", "sx-number"});
        addLine(codeArea, new String[]{"Color#2: ", "sx-keyword"}, new String[]{"SC2.1 ", "sx-number"}, new String[]{"SC2.2 ", "sx-number"}, new String[]{"SC2.3 ", "sx-number"}, new String[]{"SC2.4 ", "sx-number"});
        addLine(codeArea, new String[]{"Color#3: ", "sx-keyword"}, new String[]{"SC3.1 ", "sx-number"}, new String[]{"SC3.2 ", "sx-number"}, new String[]{"SC3.3 ", "sx-number"}, new String[]{"SC3.4 ", "sx-number"});
        addLine(codeArea, new String[]{"localVar1 ", "sx-field"}, new String[]{"localVar2 ", "sx-field"}, new String[]{"localVar3", "sx-field"});
        addLine(codeArea, new String[]{"parameter1 ", "sx-field"}, new String[]{"parameter2 ", "sx-field"}, new String[]{"parameter3", "sx-field"});
    }

    private void addStringPreview(VBox codeArea) {
        addLine(codeArea, new String[]{"\"Hello, World!\"", "sx-string"});
        addLine(codeArea, new String[]{"'String \\n\\?'", "sx-string"});
        addLine(codeArea, new String[]{"\"\"\"", "sx-string"});
        addLine(codeArea, new String[]{"  Multi-line", "sx-string"});
        addLine(codeArea, new String[]{"  string literal", "sx-string"});
        addLine(codeArea, new String[]{"\"\"\"", "sx-string"});
        addLine(codeArea, new String[]{"// Strings are highlighted in ", "sx-comment"}, new String[]{"green", "sx-string"});
        addLine(codeArea, new String[]{"String ", "sx-keyword"}, new String[]{"name", "sx-field"}, new String[]{" = ", "sx-paren"}, new String[]{"\"John\"", "sx-string"});
    }

    private void addTemplateLanguagePreview(VBox codeArea) {
        addLine(codeArea, new String[]{"{% ", "sx-paren"}, new String[]{"if user.isActive", "sx-string"}, new String[]{" %}", "sx-paren"});
        addLine(codeArea, new String[]{"  {% ", "sx-paren"}, new String[]{"include 'header.html'", "sx-string"}, new String[]{" %}", "sx-paren"});
        addLine(codeArea, new String[]{"  <h1>", "sx-string"}, new String[]{"{{ user.name }}", "sx-type"}, new String[]{"</h1>", "sx-string"});
        addLine(codeArea, new String[]{"  <p>", "sx-string"}, new String[]{"{{ user.bio }}", "sx-type"}, new String[]{"</p>", "sx-string"});
        addLine(codeArea, new String[]{"{% ", "sx-paren"}, new String[]{"endif", "sx-string"}, new String[]{" %}", "sx-paren"});
        addLine(codeArea, new String[]{"Entity: ", "sx-paren"}, new String[]{"&amp;", "sx-string"});
        addLine(codeArea, new String[]{"{% ", "sx-paren"}, new String[]{"Template language", "sx-string"}, new String[]{" %}", "sx-paren"});
        addLine(codeArea, new String[]{"@TA6 ", "sx-annotation"}, new String[]{"attribute = ", "sx-paren"}, new String[]{"Value", "sx-string"});
    }

    private void addLine(VBox codeArea, String[]... parts) {
        TextFlow flow = new TextFlow();
        for (String[] part : parts) {
            Text text = new Text(part[0]);
            if (part.length > 1) {
                text.getStyleClass().add(part[1]);
            }
            flow.getChildren().add(text);
        }
        // Add a newline after each line
        codeArea.getChildren().add(flow);
    }
}