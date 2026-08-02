package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > General > Auto Import settings page.
 * Complete implementation matching the screenshots.
 */
public class SettingsAutoImportPage extends VBox {

    public SettingsAutoImportPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(0, 0, 0, 0));
        setSpacing(16);

        // ============================================================
        // XML Section
        // ============================================================
        Label xmlLabel = new Label("XML");
        xmlLabel.getStyleClass().add("settings-section");

        CheckBox showAutoImportTooltip = new CheckBox("Show auto-import tooltip");
        showAutoImportTooltip.getStyleClass().add("settings-check");

        VBox xmlBox = new VBox(4, xmlLabel, showAutoImportTooltip);

        // ============================================================
        // Java Section
        // ============================================================
        Label javaLabel = new Label("Java");
        javaLabel.getStyleClass().add("settings-section");

        CheckBox showAutoImportTooltipJava = new CheckBox("Show auto-import tooltip for:");
        showAutoImportTooltipJava.getStyleClass().add("settings-check");

        HBox javaCheckboxes = new HBox(20);
        javaCheckboxes.setPadding(new Insets(4, 0, 0, 20));
        javaCheckboxes.setAlignment(Pos.CENTER_LEFT);

        CheckBox classesCheck = new CheckBox("Classes");
        classesCheck.getStyleClass().add("settings-check");
        CheckBox staticMethodsCheck = new CheckBox("Static methods and fields");
        staticMethodsCheck.getStyleClass().add("settings-check");

        javaCheckboxes.getChildren().addAll(classesCheck, staticMethodsCheck);

        // Insert imports on paste
        Label insertPasteLabel = new Label("Insert imports on paste:");
        insertPasteLabel.getStyleClass().add("settings-label");

        HBox insertPasteRow = new HBox(8);
        insertPasteRow.setPadding(new Insets(4, 0, 0, 20));
        insertPasteRow.setAlignment(Pos.CENTER_LEFT);

        RadioButton alwaysPaste = new RadioButton("Always");
        RadioButton neverPaste = new RadioButton("Never");
        RadioButton askPaste = new RadioButton("Ask");
        askPaste.setSelected(true);

        ToggleGroup pasteGroup = new ToggleGroup();
        alwaysPaste.setToggleGroup(pasteGroup);
        neverPaste.setToggleGroup(pasteGroup);
        askPaste.setToggleGroup(pasteGroup);

        insertPasteRow.getChildren().addAll(alwaysPaste, neverPaste, askPaste);

        CheckBox addUnambiguousImports = new CheckBox("Add unambiguous imports on the fly");
        addUnambiguousImports.getStyleClass().add("settings-check");
        addUnambiguousImports.setPadding(new Insets(4, 0, 0, 20));

        CheckBox optimizeImportsOnFly = new CheckBox("Optimize imports on the fly");
        optimizeImportsOnFly.getStyleClass().add("settings-check");
        optimizeImportsOnFly.setPadding(new Insets(4, 0, 0, 20));

        // Include static members in completion
        CheckBox includeStaticMembers = new CheckBox("Include auto-import of static members in completion:");
        includeStaticMembers.getStyleClass().add("settings-check");

        HBox addButtonRow = new HBox(8);
        addButtonRow.setPadding(new Insets(4, 0, 0, 20));
        addButtonRow.setAlignment(Pos.CENTER_LEFT);

        Button addClassButton = new Button("+");
        addClassButton.getStyleClass().add("dialog-secondary");
        Label addClassHint = new Label("Add a class or member of a class to include in auto-import and completion:");
        addClassHint.getStyleClass().add("settings-hint");

        TextField addClassField = new TextField("java.util.Objects or java.util.Objects.requireNonNull");
        addClassField.getStyleClass().add("text-field");
        addClassField.setPrefWidth(400);

        VBox addClassBox = new VBox(4, addClassHint, addClassField);
        addClassBox.setPadding(new Insets(4, 0, 0, 20));

        // Exclude from auto-import
        Label excludeLabel = new Label("Exclude from auto-import and completion:");
        excludeLabel.getStyleClass().add("settings-label");

        Button addExcludeButton = new Button("+");
        addExcludeButton.getStyleClass().add("dialog-secondary");
        Label excludeHint = new Label("Add a class, package, or member to exclude from auto-import and completion:");
        excludeHint.getStyleClass().add("settings-hint");
        Label excludeWildcardHint = new Label("Use the * wildcard to exclude all members of a specified class or package");
        excludeWildcardHint.getStyleClass().add("settings-hint");

        HBox excludeButtonRow = new HBox(8, addExcludeButton);
        excludeButtonRow.setPadding(new Insets(4, 0, 0, 20));

        VBox javaBox = new VBox(8,
            javaLabel,
            showAutoImportTooltipJava,
            javaCheckboxes,
            insertPasteLabel,
            insertPasteRow,
            addUnambiguousImports,
            optimizeImportsOnFly,
            includeStaticMembers,
            addButtonRow,
            addClassBox,
            excludeLabel,
            excludeButtonRow,
            excludeHint,
            excludeWildcardHint
        );

        // ============================================================
        // Rust Section
        // ============================================================
        Label rustLabel = new Label("Rust");
        rustLabel.getStyleClass().add("settings-section");

        CheckBox showImportPopup = new CheckBox("Show import popup");
        showImportPopup.getStyleClass().add("settings-check");

        CheckBox importOutOfScope = new CheckBox("Import out-of-scope items on completion");
        importOutOfScope.setSelected(true);
        importOutOfScope.getStyleClass().add("settings-check");

        CheckBox insertImportsOnPaste = new CheckBox("Insert imports on paste");
        insertImportsOnPaste.setSelected(true);
        insertImportsOnPaste.getStyleClass().add("settings-check");

        // Add crate dependencies on paste
        Label crateLabel = new Label("Add crate dependencies on paste:");
        crateLabel.getStyleClass().add("settings-label");

        HBox crateRow = new HBox(8);
        crateRow.setPadding(new Insets(4, 0, 0, 20));
        crateRow.setAlignment(Pos.CENTER_LEFT);

        RadioButton askCrate = new RadioButton("Ask");
        askCrate.setSelected(true);
        RadioButton alwaysCrate = new RadioButton("Always");
        RadioButton neverCrate = new RadioButton("Never");

        ToggleGroup crateGroup = new ToggleGroup();
        askCrate.setToggleGroup(crateGroup);
        alwaysCrate.setToggleGroup(crateGroup);
        neverCrate.setToggleGroup(crateGroup);

        crateRow.getChildren().addAll(askCrate, alwaysCrate, neverCrate);

        CheckBox addUnambiguousImportsRust = new CheckBox("Add unambiguous imports on the fly");
        addUnambiguousImportsRust.getStyleClass().add("settings-check");
        addUnambiguousImportsRust.setPadding(new Insets(4, 0, 0, 20));

        // Exclude from auto-import Rust
        Label excludeRustLabel = new Label("Exclude from auto-import and completion:");
        excludeRustLabel.getStyleClass().add("settings-label");

        Button addExcludeRustButton = new Button("+");
        addExcludeRustButton.getStyleClass().add("dialog-secondary");

        HBox excludeRustButtonRow = new HBox(8, addExcludeRustButton);
        excludeRustButtonRow.setPadding(new Insets(4, 0, 0, 20));

        CheckBox itemOrModule = new CheckBox("Item or module");
        itemOrModule.getStyleClass().add("settings-check");
        itemOrModule.setPadding(new Insets(4, 0, 0, 20));

        TextField excludeRustField = new TextField("std::borrow::Borrow");
        excludeRustField.getStyleClass().add("text-field");
        excludeRustField.setPrefWidth(300);
        excludeRustField.setPadding(new Insets(4, 0, 0, 20));

        VBox rustBox = new VBox(8,
            rustLabel,
            showImportPopup,
            importOutOfScope,
            insertImportsOnPaste,
            crateLabel,
            crateRow,
            addUnambiguousImportsRust,
            excludeRustLabel,
            excludeRustButtonRow,
            itemOrModule,
            excludeRustField
        );

        // ============================================================
        // JavaScript / TypeScript Section
        // ============================================================
        Label jsLabel = new Label("JavaScript / TypeScript");
        jsLabel.getStyleClass().add("settings-section");

        CheckBox addJsImports = new CheckBox("Add JavaScript imports automatically");
        addJsImports.getStyleClass().add("settings-check");

        Label jsHint = new Label("Find more configuration options in Code Style");
        jsHint.getStyleClass().add("settings-hint");
        jsHint.setPadding(new Insets(0, 0, 0, 20));

        RadioButton jsOnCompletion = new RadioButton("On code completion");
        RadioButton jsWithTooltip = new RadioButton("With auto-import tooltip");
        jsOnCompletion.setSelected(true);

        ToggleGroup jsGroup = new ToggleGroup();
        jsOnCompletion.setToggleGroup(jsGroup);
        jsWithTooltip.setToggleGroup(jsGroup);

        HBox jsRadioRow = new HBox(20);
        jsRadioRow.setPadding(new Insets(4, 0, 0, 20));
        jsRadioRow.setAlignment(Pos.CENTER_LEFT);
        jsRadioRow.getChildren().addAll(jsOnCompletion, jsWithTooltip);

        CheckBox addTsImports = new CheckBox("Add TypeScript imports automatically");
        addTsImports.getStyleClass().add("settings-check");

        Label tsHint = new Label("Find more configuration options in Code Style");
        tsHint.getStyleClass().add("settings-hint");
        tsHint.setPadding(new Insets(0, 0, 0, 20));

        RadioButton tsOnCompletion = new RadioButton("On code completion");
        RadioButton tsWithTooltip = new RadioButton("With auto-import tooltip");
        tsOnCompletion.setSelected(true);

        ToggleGroup tsGroup = new ToggleGroup();
        tsOnCompletion.setToggleGroup(tsGroup);
        tsWithTooltip.setToggleGroup(tsGroup);

        HBox tsRadioRow = new HBox(20);
        tsRadioRow.setPadding(new Insets(4, 0, 0, 20));
        tsRadioRow.setAlignment(Pos.CENTER_LEFT);
        tsRadioRow.getChildren().addAll(tsOnCompletion, tsWithTooltip);

        VBox jsBox = new VBox(8,
            jsLabel,
            addJsImports,
            jsHint,
            jsRadioRow,
            addTsImports,
            tsHint,
            tsRadioRow
        );

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            xmlBox,
            javaBox,
            rustBox,
            jsBox
        );
    }
}