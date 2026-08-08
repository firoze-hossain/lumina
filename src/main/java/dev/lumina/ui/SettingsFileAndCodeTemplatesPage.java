// SettingsFileAndCodeTemplatesPage.java
package dev.lumina.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * IntelliJ-style Editor > File and Code Templates settings page.
 * Complete implementation matching all screenshots with tabbed category design.
 */
public class SettingsFileAndCodeTemplatesPage extends VBox {

    private final ListView<String> templateList = new ListView<>();
    private final TextField nameField = new TextField();
    private final TextField extensionField = new TextField();
    private final TextArea templateContent = new TextArea();
    private final TextArea descriptionArea = new TextArea();
    private final ListView<String> variablesList = new ListView<>();
    private final CheckBox reformatCheck = new CheckBox("Reformat according to style");
    private final CheckBox liveTemplatesCheck = new CheckBox("Enable Live Templates");

    // Category buttons
    private final ToggleButton filesBtn = new ToggleButton("Files");
    private final ToggleButton includesBtn = new ToggleButton("Includes");
    private final ToggleButton codeBtn = new ToggleButton("Code");
    private final ToggleButton otherBtn = new ToggleButton("Other");

    private final ToggleGroup categoryGroup = new ToggleGroup();

    // Data for each category
    private final ObservableList<String> filesTemplates = FXCollections.observableArrayList(
            "HTML File", "Class", "Interface", "Enum", "Record",
            "SimpleSourceFile", "AnnotationType", "Exception", "package-info",
            "module-info", "CSS File", "Less File", "PostCSS File",
            "Sass File", "SCSS File", "Rust File", "JavaFXApplication",
            "FxmlFile", "XML Properties File"
    );

    private final ObservableList<String> includesTemplates = FXCollections.observableArrayList(
            "File Header", "File Footer", "Method Body", "Constructor Body",
            "Getter Body", "Setter Body", "Super Call", "This Call",
            "ToString Body", "Equals and HashCode Body"
    );

    private final ObservableList<String> codeTemplates = FXCollections.observableArrayList(
            "Catch Statement Body", "Catch Statement Declaration",
            "compose-desktop-builder", "compose-desktop-mapper", "compose-desktop-runtime",
            "compose-gradle", "compose-gradle-wrappers", "compose-settings.gradle",
            "Dynamic Method Body", "Groovy Unit SetUp", "Groovy Unit TearDown",
            "Groovy Unit Test Case", "Groovy Unit Test Method", "Groovy New Method",
            "I18nized Concatenation", "I18nized Expression", "I18nized JSP Expression",
            "Implemented Method", "JavaDoc Class", "JavaDoc Constructor"
    );

    private final ObservableList<String> otherTemplates = FXCollections.observableArrayList(
            "Default", "Project", "Application", "Deployment descriptor",
            "CDI", "JAX-RS", "JBoss/WildFly Server", "JPA", "JSP files",
            "Java Enterprise", "JavaFX", "Maven", "Spring", "Tomcat Server", "Web"
    );

    public SettingsFileAndCodeTemplatesPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(14);

        // ============================================================
        // Scheme row
        // ============================================================
        HBox schemeRow = new HBox(12);
        schemeRow.setAlignment(Pos.CENTER_LEFT);

        Label schemeLabel = new Label("Scheme:");
        schemeLabel.getStyleClass().add("settings-label");

        ComboBox<String> schemeCombo = new ComboBox<>();
        schemeCombo.getItems().addAll("Default", "Project", "Default (copy)");
        schemeCombo.getSelectionModel().selectFirst();
        schemeCombo.getStyleClass().add("settings-combo");
        schemeCombo.setPrefWidth(200);

        schemeRow.getChildren().addAll(schemeLabel, schemeCombo);

        // ============================================================
        // Category buttons (Files | Includes | Code | Other)
        // ============================================================
        HBox categoryRow = new HBox(0);
        categoryRow.setAlignment(Pos.CENTER_LEFT);
        categoryRow.setPadding(new Insets(8, 0, 8, 0));

        // Configure toggle buttons
        filesBtn.setToggleGroup(categoryGroup);
        includesBtn.setToggleGroup(categoryGroup);
        codeBtn.setToggleGroup(categoryGroup);
        otherBtn.setToggleGroup(categoryGroup);

        filesBtn.getStyleClass().addAll("segment", "segment-first");
        includesBtn.getStyleClass().addAll("segment");
        codeBtn.getStyleClass().addAll("segment");
        otherBtn.getStyleClass().addAll("segment", "segment-last");

        filesBtn.setSelected(true);

        // Add action listeners
        filesBtn.setOnAction(e -> switchCategory("Files"));
        includesBtn.setOnAction(e -> switchCategory("Includes"));
        codeBtn.setOnAction(e -> switchCategory("Code"));
        otherBtn.setOnAction(e -> switchCategory("Other"));

        categoryRow.getChildren().addAll(filesBtn, includesBtn, codeBtn, otherBtn);

        // ============================================================
        // Name and Extension row
        // ============================================================
        HBox nameRow = new HBox(12);
        nameRow.setPadding(new Insets(4, 0, 8, 0));

        Label nameLabel = new Label("Name:");
        nameLabel.getStyleClass().add("settings-label");
        nameField.getStyleClass().add("text-field");
        nameField.setPrefWidth(200);
        nameField.setPromptText("Template name");

        Label extensionLabel = new Label("Extension:");
        extensionLabel.getStyleClass().add("settings-label");
        extensionField.getStyleClass().add("text-field");
        extensionField.setPrefWidth(150);
        extensionField.setPromptText(".ext");

        nameRow.getChildren().addAll(nameLabel, nameField, extensionLabel, extensionField);

        // ============================================================
        // Main layout: Template list | Content area
        // ============================================================
        HBox mainLayout = new HBox(16);
        mainLayout.setPadding(new Insets(8, 0, 0, 0));

        // ---- Template list (left) ----
        VBox templateBox = new VBox(6);
        templateBox.setPrefWidth(200);
        templateBox.setMinWidth(180);

        Label templateLabel = new Label("Templates:");
        templateLabel.getStyleClass().add("settings-label");

        templateList.getStyleClass().add("settings-list");
        templateList.setPrefHeight(250);
        templateList.setItems(filesTemplates);
        templateList.getSelectionModel().select("HTML File");

        templateList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                updateContent(selected);
            }
        });

        templateBox.getChildren().addAll(templateLabel, templateList);

        // ---- Content area (right) ----
        VBox contentBox = new VBox(8);
        contentBox.setPrefWidth(450);
        contentBox.setMinWidth(350);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        // Template content editor
        Label contentLabel = new Label("Template Content:");
        contentLabel.getStyleClass().add("settings-label");

        templateContent.getStyleClass().add("settings-preview-text");
        templateContent.setPrefHeight(140);
        templateContent.setWrapText(true);
        templateContent.setStyle("-fx-background-color: #1F2230; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #D8DBE6; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12px;");
        templateContent.setText("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <title>#[[$Title$]]#</title>\n</head>\n<body>\n    #[[$END$]]#\n</body>\n</html>");

        // Checkboxes
        HBox checkboxRow = new HBox(20);
        checkboxRow.setAlignment(Pos.CENTER_LEFT);
        reformatCheck.setSelected(true);
        reformatCheck.getStyleClass().add("settings-check");
        liveTemplatesCheck.setSelected(true);
        liveTemplatesCheck.getStyleClass().add("settings-check");
        checkboxRow.getChildren().addAll(reformatCheck, liveTemplatesCheck);

        // Description
        Label descLabel = new Label("Description:");
        descLabel.getStyleClass().add("settings-label");

        descriptionArea.getStyleClass().add("settings-preview-text");
        descriptionArea.setPrefHeight(60);
        descriptionArea.setWrapText(true);
        descriptionArea.setEditable(false);
        descriptionArea.setStyle("-fx-background-color: #14161E; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #B9BECF; -fx-font-size: 11px;");
        descriptionArea.setText("In file templates, you can use text, code, comments, and predefined variables. A list of predefined variables is available below. When you use these variables in templates, they expand into corresponding values later in the editor.");

        // Variables
        Label varLabel = new Label("Predefined Variables:");
        varLabel.getStyleClass().add("settings-label");

        variablesList.getStyleClass().add("settings-list");
        variablesList.setPrefHeight(55);
        variablesList.getItems().setAll("${PACKAGE_NAME} - Name of the package in which the new file is created",
                "${NAME} - Name of the new file",
                "${USER} - Current user name");

        contentBox.getChildren().addAll(
                contentLabel, templateContent,
                checkboxRow,
                descLabel, descriptionArea,
                varLabel, variablesList
        );

        mainLayout.getChildren().addAll(templateBox, contentBox);

        // Update initial content
        updateContent("HTML File");

        getChildren().addAll(schemeRow, categoryRow, nameRow, mainLayout);
    }

    private void switchCategory(String category) {
        templateList.getItems().clear();
        switch (category) {
            case "Files":
                templateList.setItems(filesTemplates);
                break;
            case "Includes":
                templateList.setItems(includesTemplates);
                break;
            case "Code":
                templateList.setItems(codeTemplates);
                break;
            case "Other":
                templateList.setItems(otherTemplates);
                break;
        }
        templateList.getSelectionModel().selectFirst();
        updateContent(templateList.getSelectionModel().getSelectedItem());
    }

    private void updateContent(String templateName) {
        if (templateName == null) return;

        // Update name and extension
        if (templateName.equals("HTML File")) {
            nameField.setText("HTML File");
            extensionField.setText(".html");
        } else if (templateName.equals("Class")) {
            nameField.setText("Class");
            extensionField.setText(".java");
        } else if (templateName.equals("Interface")) {
            nameField.setText("Interface");
            extensionField.setText(".java");
        } else if (templateName.equals("Enum")) {
            nameField.setText("Enum");
            extensionField.setText(".java");
        } else if (templateName.equals("Record")) {
            nameField.setText("Record");
            extensionField.setText(".java");
        } else if (templateName.equals("CSS File")) {
            nameField.setText("CSS File");
            extensionField.setText(".css");
        } else if (templateName.equals("File Header")) {
            nameField.setText("File Header");
            extensionField.setText("");
        } else if (templateName.equals("Catch Statement Body")) {
            nameField.setText("Catch Statement Body");
            extensionField.setText("");
        } else {
            nameField.setText(templateName);
            extensionField.setText("");
        }

        // Update content based on selected template
        if (templateName.equals("HTML File")) {
            templateContent.setText("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <title>#[[$Title$]]#</title>\n</head>\n<body>\n    #[[$END$]]#\n</body>\n</html>");
            descriptionArea.setText("In file templates, you can use text, code, comments, and predefined variables. A list of predefined variables is available below. When you use these variables in templates, they expand into corresponding values later in the editor.");
            variablesList.getItems().setAll("${PACKAGE_NAME} - Name of the package in which the new file is created",
                    "${NAME} - Name of the new file",
                    "${USER} - Current user name",
                    "${DATE} - Current system date",
                    "${TIME} - Current system time",
                    "${YEAR} - Current year");
        } else if (templateName.equals("Class")) {
            templateContent.setText("#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};#end\n\npublic class ${NAME} {\n    $END$\n}");
            descriptionArea.setText("Creates a new Java class file. The template uses predefined variables that expand when the file is created.");
            variablesList.getItems().setAll("${PACKAGE_NAME} - Name of the package",
                    "${NAME} - Name of the class",
                    "${USER} - Current user name",
                    "${DATE} - Current system date");
        } else if (templateName.equals("Interface")) {
            templateContent.setText("#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != \"\")package ${PACKAGE_NAME};#end\n\npublic interface ${NAME} {\n    $END$\n}");
            descriptionArea.setText("Creates a new Java interface file.");
            variablesList.getItems().setAll("${PACKAGE_NAME} - Name of the package",
                    "${NAME} - Name of the interface");
        } else if (templateName.equals("Catch Statement Body")) {
            templateContent.setText("// TODO: Handle exception\n$EXCEPTION$.printStackTrace();\n$END$");
            descriptionArea.setText("Fills the body of a catch block when it is generated, e.g. when the Code | Surround with... function is applied. This built-in template is editable. Along with Java expressions and comments, you can also use the predefined variables that will be then expanded into the corresponding values.");
            variablesList.getItems().setAll("${EXCEPTION} - Name of the Exception variable specified as a catch parameter",
                    "${END} - Cursor position after template expansion");
        } else if (templateName.equals("File Header")) {
            templateContent.setText("/**\n * Created by ${USER} on ${DATE}.\n */\n$END$");
            descriptionArea.setText("This built-in template is editable. Along with static text, code, and comments, you can also use the predefined variables that will then be expanded like macros into the corresponding values.");
            variablesList.getItems().setAll("${USER} - Current user name",
                    "${DATE} - Current system date",
                    "${END} - Cursor position after template expansion");
        } else if (templateName.equals("Method Body")) {
            templateContent.setText("// TODO: Method body\n$END$");
            descriptionArea.setText("Fills the body of a generated method.");
            variablesList.getItems().setAll("${END} - Cursor position after template expansion");
        } else if (templateName.equals("Getter Body")) {
            templateContent.setText("return $field.name$;");
            descriptionArea.setText("Fills the body of a generated getter method.");
            variablesList.getItems().setAll("${field.name} - Name of the field");
        } else if (templateName.equals("Setter Body")) {
            templateContent.setText("this.$field.name$ = $field.name$;");
            descriptionArea.setText("Fills the body of a generated setter method.");
            variablesList.getItems().setAll("${field.name} - Name of the field");
        } else {
            templateContent.setText("// Template for: " + templateName + "\n$END$");
            descriptionArea.setText("Template for " + templateName + ". You can edit this template to customize the generated code.");
            variablesList.getItems().setAll("${END} - Cursor position after template expansion");
        }
    }
}