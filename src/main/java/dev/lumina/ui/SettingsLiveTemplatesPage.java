// SettingsLiveTemplatesPage.java
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
 * IntelliJ-style Editor > Live Templates settings page.
 * Complete implementation matching all screenshots.
 */
public class SettingsLiveTemplatesPage extends VBox {

    private final ListView<String> groupList = new ListView<>();
    private final ListView<String> templateList = new ListView<>();
    private final Label noSelectionLabel = new Label("No live templates are selected");
    private final TextArea previewArea = new TextArea();
    private final Map<String, ObservableList<String>> groupTemplates = new HashMap<>();

    public SettingsLiveTemplatesPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(14);

        // ============================================================
        // By default expand with
        // ============================================================
        HBox expandRow = new HBox(12);
        expandRow.setAlignment(Pos.CENTER_LEFT);

        Label expandLabel = new Label("By default expand with");
        expandLabel.getStyleClass().add("settings-label");

        ComboBox<String> expandCombo = new ComboBox<>();
        expandCombo.getItems().addAll("Tab", "Space", "Enter");
        expandCombo.getSelectionModel().selectFirst();
        expandCombo.getStyleClass().add("settings-combo");
        expandCombo.setPrefWidth(120);

        expandRow.getChildren().addAll(expandLabel, expandCombo);

        // ============================================================
        // Main layout: Group list | Template list
        // ============================================================
        HBox mainLayout = new HBox(16);
        mainLayout.setPadding(new Insets(8, 0, 8, 0));

        // ---- Group list (left) ----
        VBox groupBox = new VBox(6);
        groupBox.setPrefWidth(200);
        groupBox.setMinWidth(180);

        Label groupLabel = new Label("Template Groups:");
        groupLabel.getStyleClass().add("settings-label");

        groupList.getStyleClass().add("settings-list");
        groupList.setPrefHeight(280);
        populateGroups();
        groupList.getSelectionModel().select("Java");

        groupList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                updateTemplates(selected);
            }
        });

        groupBox.getChildren().addAll(groupLabel, groupList);

        // ---- Template list (right) ----
        VBox templateBox = new VBox(6);
        templateBox.setPrefWidth(350);
        templateBox.setMinWidth(300);
        HBox.setHgrow(templateBox, Priority.ALWAYS);

        Label templateLabel = new Label("Templates:");
        templateLabel.getStyleClass().add("settings-label");

        templateList.getStyleClass().add("settings-list");
        templateList.setPrefHeight(280);

        // Custom cell factory to show template descriptions
        templateList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                // Parse template name and description
                if (item.contains(" - ")) {
                    String[] parts = item.split(" - ", 2);
                    Label name = new Label(parts[0]);
                    name.getStyleClass().add("settings-label");
                    Label desc = new Label(parts.length > 1 ? parts[1] : "");
                    desc.getStyleClass().add("settings-hint");
                    desc.setStyle("-fx-font-size: 10px; -fx-text-fill: #697089;");
                    VBox box = new VBox(2, name, desc);
                    setGraphic(box);
                    setText(null);
                } else {
                    setText(item);
                }
            }
        });

        templateList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                showTemplatePreview(selected);
            } else {
                showNoSelection();
            }
        });

        templateBox.getChildren().addAll(templateLabel, templateList);

        mainLayout.getChildren().addAll(groupBox, templateBox);

        // ============================================================
        // Preview area (bottom)
        // ============================================================
        VBox previewBox = new VBox(6);
        previewBox.setPadding(new Insets(4, 0, 0, 0));

        previewArea.getStyleClass().add("settings-preview-text");
        previewArea.setPrefHeight(60);
        previewArea.setWrapText(true);
        previewArea.setEditable(false);
        previewArea.setStyle("-fx-background-color: #14161E; -fx-border-color: #2C3042; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #B9BECF; -fx-font-size: 12px;");
        previewArea.setText("No live templates are selected");

        previewBox.getChildren().add(previewArea);

        // Update initial selection
        groupList.getSelectionModel().select("Java");
        updateTemplates("Java");

        getChildren().addAll(expandRow, mainLayout, previewBox);
    }

    private void populateGroups() {
        ObservableList<String> groups = FXCollections.observableArrayList(
            "Angular",
            "Groovy",
            "gRPC Request",
            "HTML/XML",
            "HTTP Request",
            "Java",
            "JavaScript",
            "JavaScript Testing",
            "JSP",
            "Kotlin",
            "Kubernetes",
            "OpenAPI Specifications (.json)",
            "OpenAPI Specifications (.yaml)",
            "Qute",
            "React"
        );
        groupList.setItems(groups);

        // Populate templates for each group
        // Angular templates
        ObservableList<String> angularTemplates = FXCollections.observableArrayList(
            "@defer - Surround with @defer block",
            "@else - Surround with @else block",
            "@else if - Surround with @else if block",
            "@for - Surround with @for block",
            "@if - Surround with @if block",
            "@if + @else - Surround with @if and @else block",
            "@switch - surround with @switch",
            "a-class - Angular [class] binding",
            "a-component - Angular component",
            "a-common-inline - Angular component with an inline template"
        );
        groupTemplates.put("Angular", angularTemplates);

        // Java templates
        ObservableList<String> javaTemplates = FXCollections.observableArrayList(
            "Instance 'main' methods for implicitly declared classes",
            "Instance 'main' methods for normal classes",
            "C - Surround with Callable",
            "else-if - Add else-if branch",
            "fori - Create iteration loop",
            "geti - Inserts singleton method getInstance",
            "I - Iterate Iterable or array",
            "itco - Iterate with collections",
            "itli - Iterate with list",
            "psvm - main method declaration",
            "sout - System.out.println",
            "soutv - System.out.println with variable"
        );
        groupTemplates.put("Java", javaTemplates);

        // React templates
        ObservableList<String> reactTemplates = FXCollections.observableArrayList(
            "con - Constructor with props argument",
            "fsc - React Flow arrow function component",
            "fsf - React Flow function component",
            "props - this.props.",
            "rcc - React class component",
            "rccp - React class component with PropTypes",
            "rcfc - React class component with PropTypes and lifecycle methods"
        );
        groupTemplates.put("React", reactTemplates);

        // JavaScript templates
        ObservableList<String> jsTemplates = FXCollections.observableArrayList(
            "af - Arrow function",
            "cl - console.log",
            "fn - Function declaration",
            "if - if statement",
            "for - for loop",
            "fori - for loop with index"
        );
        groupTemplates.put("JavaScript", jsTemplates);

        // Kotlin templates
        ObservableList<String> kotlinTemplates = FXCollections.observableArrayList(
            "main - main function",
            "sout - println",
            "for - for loop",
            "if - if expression",
            "when - when expression"
        );
        groupTemplates.put("Kotlin", kotlinTemplates);

        // HTML/XML templates
        ObservableList<String> htmlTemplates = FXCollections.observableArrayList(
            "a - Anchor tag",
            "div - Div tag",
            "img - Image tag",
            "input - Input tag",
            "script - Script tag",
            "style - Style tag"
        );
        groupTemplates.put("HTML/XML", htmlTemplates);

        // HTTP Request templates
        ObservableList<String> httpTemplates = FXCollections.observableArrayList(
            "GET - GET request",
            "POST - POST request",
            "PUT - PUT request",
            "DELETE - DELETE request",
            "PATCH - PATCH request"
        );
        groupTemplates.put("HTTP Request", httpTemplates);

        // Kubernetes templates
        ObservableList<String> k8sTemplates = FXCollections.observableArrayList(
            "pod - Pod definition",
            "service - Service definition",
            "deployment - Deployment definition",
            "configmap - ConfigMap definition",
            "secret - Secret definition"
        );
        groupTemplates.put("Kubernetes", k8sTemplates);

        // Groovy templates
        ObservableList<String> groovyTemplates = FXCollections.observableArrayList(
            "def - Define variable",
            "class - Class definition",
            "method - Method definition",
            "sout - println"
        );
        groupTemplates.put("Groovy", groovyTemplates);

        // JSP templates
        ObservableList<String> jspTemplates = FXCollections.observableArrayList(
            "page - Page directive",
            "include - Include directive",
            "taglib - Taglib directive",
            "scriplet - Scriptlet"
        );
        groupTemplates.put("JSP", jspTemplates);

        // JavaScript Testing templates
        ObservableList<String> jsTestTemplates = FXCollections.observableArrayList(
            "describe - Describe block",
            "it - Test case",
            "beforeEach - Before each hook",
            "afterEach - After each hook",
            "expect - Expect assertion"
        );
        groupTemplates.put("JavaScript Testing", jsTestTemplates);

        // gRPC Request templates
        ObservableList<String> grpcTemplates = FXCollections.observableArrayList(
            "unary - Unary call",
            "serverStream - Server streaming",
            "clientStream - Client streaming",
            "bidirectional - Bidirectional streaming"
        );
        groupTemplates.put("gRPC Request", grpcTemplates);

        // OpenAPI templates
        ObservableList<String> openapiJsonTemplates = FXCollections.observableArrayList(
            "openapi - OpenAPI definition",
            "info - Info section",
            "paths - Paths section",
            "components - Components section"
        );
        groupTemplates.put("OpenAPI Specifications (.json)", openapiJsonTemplates);

        ObservableList<String> openapiYamlTemplates = FXCollections.observableArrayList(
            "openapi - OpenAPI definition",
            "info - Info section",
            "paths - Paths section",
            "components - Components section"
        );
        groupTemplates.put("OpenAPI Specifications (.yaml)", openapiYamlTemplates);

        // Qute templates
        ObservableList<String> quteTemplates = FXCollections.observableArrayList(
            "if - If expression",
            "for - For loop",
            "let - Let binding",
            "include - Include template"
        );
        groupTemplates.put("Qute", quteTemplates);
    }

    private void updateTemplates(String group) {
        if (group != null && groupTemplates.containsKey(group)) {
            templateList.setItems(groupTemplates.get(group));
            if (!templateList.getItems().isEmpty()) {
                templateList.getSelectionModel().selectFirst();
                showTemplatePreview(templateList.getSelectionModel().getSelectedItem());
            } else {
                showNoSelection();
            }
        } else {
            templateList.setItems(FXCollections.observableArrayList());
            showNoSelection();
        }
    }

    private void showTemplatePreview(String template) {
        if (template == null || template.isEmpty()) {
            showNoSelection();
            return;
        }

        // Parse template name and show preview
        String templateName = template.split(" - ")[0];

        // Generate preview based on template
        String preview = generatePreview(templateName);
        previewArea.setText(preview);
    }

    private void showNoSelection() {
        previewArea.setText("No live templates are selected");
    }

    private String generatePreview(String templateName) {
        // Generate preview for common templates
        switch (templateName) {
            case "psvm":
                return "public static void main(String[] args) {\n    $END$\n}";
            case "sout":
                return "System.out.println($END$);";
            case "soutv":
                return "System.out.println(\"$VAR$ = \" + $VAR$);";
            case "fori":
                return "for (int i = 0; i < $END$; i++) {\n    \n}";
            case "if":
                return "if ($END$) {\n    \n}";
            case "else-if":
                return "else if ($END$) {\n    \n}";
            case "C":
                return "Callable<$END$> callable = new Callable<$END$>() {\n    @Override\n    public $END$ call() throws Exception {\n        \n    }\n};";
            case "geti":
                return "private static $CLASS$ instance;\n\npublic static $CLASS$ getInstance() {\n    if (instance == null) {\n        instance = new $CLASS$();\n    }\n    return instance;\n}";
            case "I":
                return "for ($TYPE$ $VAR$ : $END$) {\n    \n}";
            case "@defer":
                return "@defer {\n    $END$\n}";
            case "@if":
                return "@if ($CONDITION$) {\n    $END$\n}";
            case "a-component":
                return "import { Component } from '@angular/core';\n\n@Component({\n    selector: '$SELECTOR$',\n    template: `\n        $END$\n    `\n})\nexport class $NAME$Component {\n    \n}";
            case "rcc":
                return "import React, { Component } from 'react';\n\nclass $NAME$ extends Component {\n    render() {\n        return (\n            <div>\n                $END$\n            </div>\n        );\n    }\n}\n\nexport default $NAME$;";
            case "fsc":
                return "import React from 'react';\n\nconst $NAME$ = () => {\n    return (\n        <div>\n            $END$\n        </div>\n    );\n};\n\nexport default $NAME$;";
            case "props":
                return "this.props.$END$";
            case "con":
                return "constructor(props) {\n    super(props);\n    $END$\n}";
            case "cl":
                return "console.log($END$);";
            case "af":
                return "const $NAME$ = ($PARAMS$) => {\n    $END$\n};";
            case "fn":
                return "function $NAME$($PARAMS$) {\n    $END$\n}";
            case "main":
                return "fun main() {\n    $END$\n}";
            case "when":
                return "when ($END$) {\n    \n}";
            case "pod":
                return "apiVersion: v1\nkind: Pod\nmetadata:\n  name: $NAME$\nspec:\n  containers:\n  - name: $CONTAINER$\n    image: $IMAGE$\n    $END$";
            case "deployment":
                return "apiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: $NAME$\nspec:\n  replicas: 1\n  selector:\n    matchLabels:\n      app: $APP$\n  template:\n    metadata:\n      labels:\n        app: $APP$\n    spec:\n      containers:\n      - name: $CONTAINER$\n        image: $IMAGE$\n        $END$";
            case "service":
                return "apiVersion: v1\nkind: Service\nmetadata:\n  name: $NAME$\nspec:\n  selector:\n    app: $APP$\n  ports:\n  - port: $PORT$\n    targetPort: $TARGET_PORT$\n    $END$";
            case "GET":
                return "GET $URL$\n$END$";
            case "POST":
                return "POST $URL$\nContent-Type: application/json\n\n{\n    $END$\n}";
            case "a":
                return "<a href=\"$URL$\">$END$</a>";
            case "div":
                return "<div>\n    $END$\n</div>";
            case "img":
                return "<img src=\"$SRC$\" alt=\"$ALT$\" />";
            case "input":
                return "<input type=\"$TYPE$\" name=\"$NAME$\" />";
            default:
                return "// Live template: " + templateName + "\n$END$";
        }
    }
}