// SettingsCodeFoldingPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Code Folding settings page.
 * Complete implementation matching all three screenshots.
 */
public class SettingsCodeFoldingPage extends VBox {

    public SettingsCodeFoldingPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(8, 0, 8, 0));
        setSpacing(14);

        // ============================================================
        // Show code folding arrows
        // ============================================================
        CheckBox showFoldingArrows = new CheckBox("Show code folding arrows");
        showFoldingArrows.setSelected(true);
        showFoldingArrows.getStyleClass().add("settings-check");

        HBox hoverRow = new HBox(8);
        hoverRow.setPadding(new Insets(4, 0, 8, 20));
        hoverRow.setAlignment(Pos.CENTER_LEFT);
        Label hoverLabel = new Label("On mouse hover");
        hoverLabel.getStyleClass().add("settings-label");
        hoverRow.getChildren().add(hoverLabel);

        CheckBox showBottomArrows = new CheckBox("Show bottom arrows");
        showBottomArrows.setSelected(true);
        showBottomArrows.getStyleClass().add("settings-check");

        VBox arrowsBox = new VBox(4);
        arrowsBox.setPadding(new Insets(4, 0, 8, 20));
        arrowsBox.getChildren().addAll(showFoldingArrows, hoverRow, showBottomArrows);

        // ============================================================
        // Fold by default
        // ============================================================
        Label foldByDefaultLabel = new Label("Fold by default:");
        foldByDefaultLabel.getStyleClass().add("settings-section");

        VBox foldDefaultBox = new VBox(4);
        foldDefaultBox.setPadding(new Insets(4, 0, 8, 20));

        CheckBox fileHeader = new CheckBox("File header");
        fileHeader.setSelected(true);
        fileHeader.getStyleClass().add("settings-check");

        CheckBox imports = new CheckBox("Imports");
        imports.setSelected(true);
        imports.getStyleClass().add("settings-check");

        CheckBox docComments = new CheckBox("Documentation comments");
        docComments.setSelected(true);
        docComments.getStyleClass().add("settings-check");

        CheckBox methodBodies = new CheckBox("Method bodies");
        methodBodies.getStyleClass().add("settings-check");

        CheckBox customFolding = new CheckBox("Custom folding regions");
        customFolding.setSelected(true);
        customFolding.getStyleClass().add("settings-check");

        foldDefaultBox.getChildren().addAll(
            fileHeader, imports, docComments, methodBodies, customFolding
        );

        // ============================================================
        // JPA section
        // ============================================================
        Label jpaLabel = new Label("JPA");
        jpaLabel.getStyleClass().add("settings-section");

        CheckBox jpaQueries = new CheckBox("JPA Queries");
        jpaQueries.setSelected(true);
        jpaQueries.getStyleClass().add("settings-check");

        VBox jpaBox = new VBox(4);
        jpaBox.setPadding(new Insets(4, 0, 8, 20));
        jpaBox.getChildren().add(jpaQueries);

        // ============================================================
        // JSON section
        // ============================================================
        Label jsonLabel = new Label("JSON");
        jsonLabel.getStyleClass().add("settings-section");

        CheckBox showKeyCount = new CheckBox("Show key count in folded JSON");
        showKeyCount.setSelected(true);
        showKeyCount.getStyleClass().add("settings-check");

        CheckBox showFirstKey = new CheckBox("Show the first key in folded JSON");
        showFirstKey.setSelected(true);
        showFirstKey.getStyleClass().add("settings-check");
        Label firstKeyHint = new Label("Always show the first key in a folded JSON object. Otherwise, \"id\" or \"name\" keys will be shown if present.");
        firstKeyHint.getStyleClass().add("settings-hint");
        firstKeyHint.setWrapText(true);
        firstKeyHint.setPadding(new Insets(0, 0, 0, 20));

        VBox jsonBox = new VBox(4);
        jsonBox.setPadding(new Insets(4, 0, 8, 20));
        jsonBox.getChildren().addAll(showKeyCount, showFirstKey, firstKeyHint);

        // ============================================================
        // Java section
        // ============================================================
        Label javaLabel = new Label("Java");
        javaLabel.getStyleClass().add("settings-section");

        VBox javaBox = new VBox(4);
        javaBox.setPadding(new Insets(4, 0, 8, 20));

        String[] javaItems = {
            "One-line methods",
            "Simple property accessors",
            "Inner classes",
            "Anonymous classes",
            "Annotations",
            "\"Closures\" (anonymous classes implementing one method, before Java 8)",
            "Generic constructor and method parameters",
            "Replace 'var' with inferred type",
            "I18n strings",
            "@SuppressWarnings",
            "End of line comments sequence",
            "Multiline comments"
        };

        for (String item : javaItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            javaBox.getChildren().add(cb);
        }

        // ============================================================
        // JavaScript section
        // ============================================================
        Label jsLabel = new Label("JavaScript");
        jsLabel.getStyleClass().add("settings-section");

        VBox jsBox = new VBox(4);
        jsBox.setPadding(new Insets(4, 0, 8, 20));

        String[] jsItems = {
            "One-line functions in JavaScript and TypeScript",
            "Object literals",
            "Array literals",
            "XML literals"
        };

        for (String item : jsItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            jsBox.getChildren().add(cb);
        }

        // ============================================================
        // Kubernetes section
        // ============================================================
        Label k8sLabel = new Label("Kubernetes");
        k8sLabel.getStyleClass().add("settings-section");

        VBox k8sBox = new VBox(4);
        k8sBox.setPadding(new Insets(4, 0, 8, 20));

        String[] k8sItems = {
            "Value references in Helm templates",
            "EnvVar definitions in YAML files",
            "ExecAction definitions in YAML files"
        };

        for (String item : k8sItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            k8sBox.getChildren().add(cb);
        }

        // ============================================================
        // Markdown section
        // ============================================================
        Label mdLabel = new Label("Markdown");
        mdLabel.getStyleClass().add("settings-section");

        VBox mdBox = new VBox(4);
        mdBox.setPadding(new Insets(4, 0, 8, 20));

        String[] mdItems = {
            "Collapse front matter",
            "Collapse links",
            "Collapse tables",
            "Collapse code fences",
            "Collapse table of contents"
        };

        for (String item : mdItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            mdBox.getChildren().add(cb);
        }

        // ============================================================
        // Rust section
        // ============================================================
        Label rustLabel = new Label("Rust");
        rustLabel.getStyleClass().add("settings-section");

        CheckBox rustOneLine = new CheckBox("One-line methods");
        rustOneLine.setSelected(true);
        rustOneLine.getStyleClass().add("settings-check");

        VBox rustBox = new VBox(4);
        rustBox.setPadding(new Insets(4, 0, 8, 20));
        rustBox.getChildren().add(rustOneLine);

        // ============================================================
        // SQL section
        // ============================================================
        Label sqlLabel = new Label("SQL");
        sqlLabel.getStyleClass().add("settings-section");

        CheckBox sqlUnderscores = new CheckBox("Put underscores inside numeric literals (6-digit or longer)");
        sqlUnderscores.setSelected(true);
        sqlUnderscores.getStyleClass().add("settings-check");

        VBox sqlBox = new VBox(4);
        sqlBox.setPadding(new Insets(4, 0, 8, 20));
        sqlBox.getChildren().add(sqlUnderscores);

        // ============================================================
        // XML section
        // ============================================================
        Label xmlLabel = new Label("XML");
        xmlLabel.getStyleClass().add("settings-section");

        VBox xmlBox = new VBox(4);
        xmlBox.setPadding(new Insets(4, 0, 8, 20));

        String[] xmlItems = {
            "XML tags",
            "HTML 'style' attribute",
            "XML entities",
            "Data URIs"
        };

        for (String item : xmlItems) {
            CheckBox cb = new CheckBox(item);
            cb.setSelected(true);
            cb.getStyleClass().add("settings-check");
            xmlBox.getChildren().add(cb);
        }

        // ============================================================
        // YAML section
        // ============================================================
        Label yamlLabel = new Label("YAML");
        yamlLabel.getStyleClass().add("settings-section");

        HBox yamlRow = new HBox(8);
        yamlRow.setPadding(new Insets(4, 0, 8, 20));
        yamlRow.setAlignment(Pos.CENTER_LEFT);

        Label yamlLimitLabel = new Label("Limit folded keys and values to");
        yamlLimitLabel.getStyleClass().add("settings-label");
        Spinner<Integer> yamlLimitSpinner = new Spinner<>(10, 100, 20, 5);
        yamlLimitSpinner.setPrefWidth(70);
        yamlLimitSpinner.getStyleClass().add("settings-spinner");
        Label yamlCharsLabel = new Label("characters");
        yamlCharsLabel.getStyleClass().add("settings-label");

        yamlRow.getChildren().addAll(yamlLimitLabel, yamlLimitSpinner, yamlCharsLabel);

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            // Show code folding arrows
            showFoldingArrows,
            hoverRow,
            showBottomArrows,
            // Fold by default
            foldByDefaultLabel,
            foldDefaultBox,
            // JPA
            jpaLabel,
            jpaBox,
            // JSON
            jsonLabel,
            jsonBox,
            // Java
            javaLabel,
            javaBox,
            // JavaScript
            jsLabel,
            jsBox,
            // Kubernetes
            k8sLabel,
            k8sBox,
            // Markdown
            mdLabel,
            mdBox,
            // Rust
            rustLabel,
            rustBox,
            // SQL
            sqlLabel,
            sqlBox,
            // XML
            xmlLabel,
            xmlBox,
            // YAML
            yamlLabel,
            yamlRow
        );
    }
}