package dev.lumina.ui;

import dev.lumina.settings.AutoImportSettings;
import dev.lumina.settings.AutoImportSettings.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.util.List;

/**
 * Pixel-perfect IntelliJ IDEA-style Editor > General > Auto Import settings page.
 * Completely dynamic, backed by AutoImportSettings, with native inline table editors,
 * scope dropdowns, and live configuration binding.
 */
public class SettingsAutoImportPage extends VBox {

    // 1. XML
    private final CheckBox xmlShowTooltipCheck = new CheckBox("Show auto-import tooltip");

    // 2. Java
    private final CheckBox javaClassesCheck = new CheckBox("Classes");
    private final CheckBox javaStaticMethodsCheck = new CheckBox("Static methods and fields");
    private final ComboBox<InsertImportsMode> javaPasteCombo = new ComboBox<>();
    private final CheckBox javaUnambiguousCheck = new CheckBox("Add unambiguous imports on the fly");
    private final CheckBox javaOptimizeCheck = new CheckBox("Optimize imports on the fly");
    private final ObservableList<JavaImportEntry> javaStaticMembersList = FXCollections.observableArrayList();
    private final ListView<JavaImportEntry> javaStaticMembersListView = new ListView<>(javaStaticMembersList);
    private final ObservableList<JavaImportEntry> javaExcludeList = FXCollections.observableArrayList();
    private final ListView<JavaImportEntry> javaExcludeListView = new ListView<>(javaExcludeList);

    // 3. Python
    private final CheckBox pythonShowTooltipCheck = new CheckBox("Show auto-import tooltip");
    private final RadioButton pythonFromModuleRadio = new RadioButton("from <module> import <name>");
    private final RadioButton pythonImportModuleRadio = new RadioButton("import <module>.<name>");

    // 4. Rust
    private final CheckBox rustShowPopupCheck = new CheckBox("Show import popup");
    private final CheckBox rustOutOfScopeCheck = new CheckBox("Import out-of-scope items on completion");
    private final CheckBox rustInsertPasteCheck = new CheckBox("Insert imports on paste");
    private final ComboBox<InsertImportsMode> rustCrateDepsCombo = new ComboBox<>();
    private final CheckBox rustUnambiguousCheck = new CheckBox("Add unambiguous imports on the fly");
    private final ObservableList<RustExcludeEntry> rustExcludeList = FXCollections.observableArrayList();
    private final TableView<RustExcludeEntry> rustExcludeTable = new TableView<>(rustExcludeList);

    // 5. Scala
    private final ComboBox<InsertImportsMode> scalaPasteCombo = new ComboBox<>();
    private final CheckBox scalaPopupClassesCheck = new CheckBox("Classes");
    private final CheckBox scalaPopupStaticCheck = new CheckBox("Static members");
    private final CheckBox scalaPopupConversionsCheck = new CheckBox("Implicit conversions");
    private final CheckBox scalaPopupDefinitionsCheck = new CheckBox("Implicit definitions");
    private final CheckBox scalaPopupExtensionsCheck = new CheckBox("Extension methods");
    private final CheckBox scalaUnambiguousClassesCheck = new CheckBox("Classes");
    private final CheckBox scalaUnambiguousStaticCheck = new CheckBox("Static members");
    private final CheckBox scalaOptimizeCheck = new CheckBox("Optimize imports on the fly");

    // 6. JSP
    private final CheckBox jspUnambiguousCheck = new CheckBox("Add unambiguous imports on the fly");

    // 7. Kotlin
    private final CheckBox kotlinUnambiguousCheck = new CheckBox("Add unambiguous imports on the fly");
    private final CheckBox kotlinOptimizeCheck = new CheckBox("Optimize imports on the fly");

    // 8. Ktor
    private final CheckBox ktorAddImportsCheck = new CheckBox("Add imports for Ktor modules automatically");

    // 9. TypeScript / JavaScript
    private final CheckBox jsAddImportsCheck = new CheckBox("Add JavaScript imports automatically");
    private final CheckBox jsOnCompletionCheck = new CheckBox("On code completion");
    private final CheckBox jsWithTooltipCheck = new CheckBox("With auto-import tooltip");
    private final CheckBox tsAddImportsCheck = new CheckBox("Add TypeScript imports automatically");
    private final CheckBox tsOnCompletionCheck = new CheckBox("On code completion");
    private final CheckBox tsWithTooltipCheck = new CheckBox("With auto-import tooltip");
    private final CheckBox tsUnambiguousCheck = new CheckBox("Unambiguous imports on the fly");

    // 10. PHP
    private final ComboBox<InsertImportsMode> phpPasteCombo = new ComboBox<>();
    private final CheckBox phpFileScopeCheck = new CheckBox("Enable auto-import in file scope");
    private final CheckBox phpNamespaceScopeCheck = new CheckBox("Enable auto-import in namespace scope");
    private final ComboBox<String> phpClassCombo = new ComboBox<>();
    private final ComboBox<String> phpFunctionCombo = new ComboBox<>();
    private final ComboBox<String> phpConstantCombo = new ComboBox<>();

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsAutoImportPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 28, 24));
        setSpacing(6);

        buildUi();
        setupListeners();
        loadFromSettings(AutoImportSettings.getInstance());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private HBox createSectionHeader(String title) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14, 0, 4, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(label, line);
        return box;
    }

    private Label createHelpIcon(String tooltipText) {
        Label icon = new Label("?");
        icon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #848BA3; " +
                "-fx-border-color: #4E5157; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-min-width: 15px; -fx-min-height: 15px; -fx-max-width: 15px; -fx-max-height: 15px; " +
                "-fx-alignment: CENTER; -fx-cursor: hand;");
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setShowDelay(javafx.util.Duration.millis(200));
        Tooltip.install(icon, tooltip);
        return icon;
    }

    private SVGPath createProjectScopeIcon(String tooltipText) {
        SVGPath icon = new SVGPath();
        icon.setContent("M 1 2 L 10 2 L 10 9 L 1 9 Z M 1 4 L 10 4");
        icon.setFill(javafx.scene.paint.Color.TRANSPARENT);
        icon.setStroke(javafx.scene.paint.Color.web("#848BA3"));
        icon.setStrokeWidth(1.0);
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setShowDelay(javafx.util.Duration.millis(200));
        Tooltip.install(icon, tooltip);
        return icon;
    }

    private Hyperlink createCodeStyleLink() {
        Hyperlink link = new Hyperlink("Find more configuration options in Code Style");
        link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
        link.setOnMouseEntered(e -> link.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 11px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: true;"));
        link.setOnMouseExited(e -> link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;"));
        return link;
    }

    private <T> void configureComboStyle(ComboBox<T> combo) {
        combo.setPrefHeight(26);
        combo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private HBox createToolbar(Button addBtn, Button removeBtn) {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(2, 6, 2, 6));
        bar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0; -fx-border-radius: 3 3 0 0; -fx-background-radius: 3 3 0 0;");

        String btnStyle = "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 22px; -fx-min-height: 20px; -fx-padding: 0; -fx-cursor: hand;";
        String btnHover = "-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 22px; -fx-min-height: 20px; -fx-padding: 0; -fx-cursor: hand; -fx-background-radius: 3;";

        addBtn.setStyle(btnStyle);
        removeBtn.setStyle(btnStyle);

        addBtn.setOnMouseEntered(e -> addBtn.setStyle(btnHover));
        addBtn.setOnMouseExited(e -> addBtn.setStyle(btnStyle));
        removeBtn.setOnMouseEntered(e -> removeBtn.setStyle(btnHover));
        removeBtn.setOnMouseExited(e -> removeBtn.setStyle(btnStyle));

        bar.getChildren().addAll(addBtn, removeBtn);
        return bar;
    }

    private void buildUi() {
        // ============================================================
        // 1. XML
        // ============================================================
        HBox xmlHeader = createSectionHeader("XML");
        styleCheckBox(xmlShowTooltipCheck);
        VBox xmlBox = new VBox(6, xmlHeader, xmlShowTooltipCheck);

        // ============================================================
        // 2. Java
        // ============================================================
        HBox javaHeader = createSectionHeader("Java");

        Label javaTooltipLabel = new Label("Show auto-import tooltip for:");
        javaTooltipLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        styleCheckBox(javaClassesCheck);
        styleCheckBox(javaStaticMethodsCheck);
        HBox javaTooltipRow = new HBox(12, javaTooltipLabel, javaClassesCheck, javaStaticMethodsCheck);
        javaTooltipRow.setAlignment(Pos.CENTER_LEFT);

        Label javaPasteLabel = new Label("Insert imports on paste:");
        javaPasteLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        javaPasteCombo.getItems().setAll(InsertImportsMode.values());
        javaPasteCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(InsertImportsMode object) { return object != null ? object.getLabel() : ""; }
            @Override public InsertImportsMode fromString(String string) { return InsertImportsMode.fromLabel(string); }
        });
        configureComboStyle(javaPasteCombo);
        HBox javaPasteRow = new HBox(8, javaPasteLabel, javaPasteCombo);
        javaPasteRow.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(javaUnambiguousCheck);
        HBox javaUnambiguousRow = new HBox(6, javaUnambiguousCheck, createHelpIcon("Add import statements for unambiguous references on the fly while typing"));
        javaUnambiguousRow.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(javaOptimizeCheck);
        HBox javaOptimizeRow = new HBox(6, javaOptimizeCheck, createHelpIcon("Automatically optimize imports on the fly"), createProjectScopeIcon("Project-level setting"));
        javaOptimizeRow.setAlignment(Pos.CENTER_LEFT);

        // Include static members table
        Label staticMembersLabel = new Label("Include auto-import of static members in completion:");
        staticMembersLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        VBox staticContainer = createJavaListContainer(javaStaticMembersListView, javaStaticMembersList);

        Label staticExampleLabel = new Label("Examples: 'java.util.Objects' or 'java.util.Objects.requireNonNull'");
        staticExampleLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");
        staticExampleLabel.setWrapText(true);
        staticExampleLabel.setMaxWidth(Double.MAX_VALUE);
        staticExampleLabel.maxWidthProperty().bind(widthProperty().subtract(48));

        VBox staticBox = new VBox(4, staticMembersLabel, staticContainer, staticExampleLabel);

        // Exclude from auto-import table
        Label excludeLabel = new Label("Exclude from auto-import and completion:");
        excludeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        VBox excludeContainer = createJavaListContainer(javaExcludeListView, javaExcludeList);

        Label excludeWildcardLabel = new Label("Use the * wildcard to exclude all members of a specified class or package");
        excludeWildcardLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");
        excludeWildcardLabel.setWrapText(true);
        excludeWildcardLabel.setMaxWidth(Double.MAX_VALUE);
        excludeWildcardLabel.maxWidthProperty().bind(widthProperty().subtract(48));

        VBox excludeBox = new VBox(4, excludeLabel, excludeContainer, excludeWildcardLabel);

        VBox javaBox = new VBox(6,
                javaHeader,
                javaTooltipRow,
                javaPasteRow,
                javaUnambiguousRow,
                javaOptimizeRow,
                staticBox,
                excludeBox
        );

        // ============================================================
        // 3. Python
        // ============================================================
        HBox pythonHeader = createSectionHeader("Python");
        styleCheckBox(pythonShowTooltipCheck);

        Label pythonStyleLabel = new Label("Preferred import style:");
        pythonStyleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        ToggleGroup pythonGroup = new ToggleGroup();
        pythonFromModuleRadio.setToggleGroup(pythonGroup);
        pythonImportModuleRadio.setToggleGroup(pythonGroup);
        styleRadioButton(pythonFromModuleRadio);
        styleRadioButton(pythonImportModuleRadio);

        VBox pythonRadioBox = new VBox(4, pythonFromModuleRadio, pythonImportModuleRadio);
        pythonRadioBox.setPadding(new Insets(2, 0, 0, 16));

        VBox pythonBox = new VBox(6,
                pythonHeader,
                pythonShowTooltipCheck,
                pythonStyleLabel,
                pythonRadioBox
        );

        // ============================================================
        // 4. Rust
        // ============================================================
        HBox rustHeader = createSectionHeader("Rust");
        styleCheckBox(rustShowPopupCheck);
        styleCheckBox(rustOutOfScopeCheck);
        styleCheckBox(rustInsertPasteCheck);

        Label rustCrateLabel = new Label("Add crate dependencies on paste:");
        rustCrateLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        rustCrateDepsCombo.getItems().setAll(InsertImportsMode.values());
        rustCrateDepsCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(InsertImportsMode object) { return object != null ? object.getLabel() : ""; }
            @Override public InsertImportsMode fromString(String string) { return InsertImportsMode.fromLabel(string); }
        });
        configureComboStyle(rustCrateDepsCombo);
        HBox rustCrateRow = new HBox(8, rustCrateLabel, rustCrateDepsCombo);
        rustCrateRow.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(rustUnambiguousCheck);
        HBox rustUnambiguousRow = new HBox(6, rustUnambiguousCheck, createHelpIcon("Add unambiguous Rust imports on the fly"));
        rustUnambiguousRow.setAlignment(Pos.CENTER_LEFT);

        Label rustExcludeLabel = new Label("Exclude from auto-import and completion:");
        rustExcludeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        Button addRustExcludeBtn = new Button("+");
        Button removeRustExcludeBtn = new Button("—");
        HBox rustExcludeToolbar = createToolbar(addRustExcludeBtn, removeRustExcludeBtn);

        TableColumn<RustExcludeEntry, String> rustItemCol = buildRustTable();

        addRustExcludeBtn.setOnAction(e -> {
            RustExcludeEntry newEntry = new RustExcludeEntry("", RustApplyTo.EVERYTHING, RustScope.IDE);
            rustExcludeList.add(newEntry);
            rustExcludeTable.getSelectionModel().select(newEntry);
            rustExcludeTable.scrollTo(newEntry);
            Platform.runLater(() -> rustExcludeTable.edit(rustExcludeList.size() - 1, rustItemCol));
            notifyModified();
        });
        removeRustExcludeBtn.setOnAction(e -> {
            RustExcludeEntry sel = rustExcludeTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                rustExcludeList.remove(sel);
                notifyModified();
            }
        });

        VBox rustTableContainer = new VBox(rustExcludeToolbar, rustExcludeTable);
        rustTableContainer.setStyle("-fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: #1E1F22;");

        Label rustHintLabel = new Label("Specify each path just as you would in a use declaration. Add ::* to a path if you want to disable auto-import for all items whose paths include the given prefix. When excluding traits, specify whether you want to disable auto-import only for trait methods or for the trait name too. Note that a use declaration overwrites these settings.");
        rustHintLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");
        rustHintLabel.setWrapText(true);
        rustHintLabel.setMaxWidth(Double.MAX_VALUE);
        rustHintLabel.maxWidthProperty().bind(widthProperty().subtract(48));

        VBox rustBox = new VBox(6,
                rustHeader,
                rustShowPopupCheck,
                rustOutOfScopeCheck,
                rustInsertPasteCheck,
                rustCrateRow,
                rustUnambiguousRow,
                rustExcludeLabel,
                rustTableContainer,
                rustHintLabel
        );

        // ============================================================
        // 5. Scala
        // ============================================================
        HBox scalaHeader = createSectionHeader("Scala");

        Label scalaPasteLabel = new Label("Insert imports on paste:");
        scalaPasteLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        scalaPasteCombo.getItems().setAll(InsertImportsMode.values());
        scalaPasteCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(InsertImportsMode object) { return object != null ? object.getLabel() : ""; }
            @Override public InsertImportsMode fromString(String string) { return InsertImportsMode.fromLabel(string); }
        });
        configureComboStyle(scalaPasteCombo);
        HBox scalaPasteRow = new HBox(8, scalaPasteLabel, scalaPasteCombo);
        scalaPasteRow.setAlignment(Pos.CENTER_LEFT);

        Label scalaPopupLabel = new Label("Show import popup for:");
        scalaPopupLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        styleCheckBox(scalaPopupClassesCheck);
        styleCheckBox(scalaPopupStaticCheck);
        styleCheckBox(scalaPopupConversionsCheck);
        styleCheckBox(scalaPopupDefinitionsCheck);
        styleCheckBox(scalaPopupExtensionsCheck);

        VBox scalaPopupBox = new VBox(4,
                scalaPopupClassesCheck,
                scalaPopupStaticCheck,
                scalaPopupConversionsCheck,
                scalaPopupDefinitionsCheck,
                scalaPopupExtensionsCheck
        );
        scalaPopupBox.setPadding(new Insets(2, 0, 0, 16));

        Label scalaUnambiguousLabel = new Label("Add unambiguous imports on the fly for:");
        scalaUnambiguousLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        styleCheckBox(scalaUnambiguousClassesCheck);
        styleCheckBox(scalaUnambiguousStaticCheck);
        VBox scalaUnambiguousBox = new VBox(4, scalaUnambiguousClassesCheck, scalaUnambiguousStaticCheck);
        scalaUnambiguousBox.setPadding(new Insets(2, 0, 0, 16));

        styleCheckBox(scalaOptimizeCheck);
        Hyperlink scalaCodeStyleLink = createCodeStyleLink();

        VBox scalaBox = new VBox(6,
                scalaHeader,
                scalaPasteRow,
                scalaPopupLabel,
                scalaPopupBox,
                scalaUnambiguousLabel,
                scalaUnambiguousBox,
                scalaOptimizeCheck,
                scalaCodeStyleLink
        );

        // ============================================================
        // 6. JSP
        // ============================================================
        HBox jspHeader = createSectionHeader("JSP");
        styleCheckBox(jspUnambiguousCheck);
        VBox jspBox = new VBox(6, jspHeader, jspUnambiguousCheck);

        // ============================================================
        // 7. Kotlin
        // ============================================================
        HBox kotlinHeader = createSectionHeader("Kotlin");
        styleCheckBox(kotlinUnambiguousCheck);
        styleCheckBox(kotlinOptimizeCheck);
        VBox kotlinBox = new VBox(6, kotlinHeader, kotlinUnambiguousCheck, kotlinOptimizeCheck);

        // ============================================================
        // 8. Ktor
        // ============================================================
        HBox ktorHeader = createSectionHeader("Ktor");
        styleCheckBox(ktorAddImportsCheck);
        VBox ktorBox = new VBox(6, ktorHeader, ktorAddImportsCheck);

        // ============================================================
        // 9. TypeScript / JavaScript
        // ============================================================
        HBox jsTsHeader = createSectionHeader("TypeScript / JavaScript");

        styleCheckBox(jsAddImportsCheck);
        Hyperlink jsCodeStyleLink = createCodeStyleLink();
        jsCodeStyleLink.setPadding(new Insets(0, 0, 0, 16));
        styleCheckBox(jsOnCompletionCheck);
        styleCheckBox(jsWithTooltipCheck);
        VBox jsSubBox = new VBox(4, jsCodeStyleLink, jsOnCompletionCheck, jsWithTooltipCheck);
        jsSubBox.setPadding(new Insets(2, 0, 4, 16));

        styleCheckBox(tsAddImportsCheck);
        Hyperlink tsCodeStyleLink = createCodeStyleLink();
        tsCodeStyleLink.setPadding(new Insets(0, 0, 0, 16));
        styleCheckBox(tsOnCompletionCheck);
        styleCheckBox(tsWithTooltipCheck);
        styleCheckBox(tsUnambiguousCheck);
        HBox tsUnambiguousRow = new HBox(6, tsUnambiguousCheck, createHelpIcon("Add unambiguous TypeScript imports on the fly"));
        tsUnambiguousRow.setAlignment(Pos.CENTER_LEFT);

        VBox tsSubBox = new VBox(4, tsCodeStyleLink, tsOnCompletionCheck, tsWithTooltipCheck, tsUnambiguousRow);
        tsSubBox.setPadding(new Insets(2, 0, 4, 16));

        VBox jsTsBox = new VBox(6,
                jsTsHeader,
                jsAddImportsCheck,
                jsSubBox,
                tsAddImportsCheck,
                tsSubBox
        );

        // ============================================================
        // 10. PHP
        // ============================================================
        HBox phpHeader = createSectionHeader("PHP");

        Label phpPasteLabel = new Label("Insert imports on paste:");
        phpPasteLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        phpPasteCombo.getItems().setAll(InsertImportsMode.values());
        phpPasteCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(InsertImportsMode object) { return object != null ? object.getLabel() : ""; }
            @Override public InsertImportsMode fromString(String string) { return InsertImportsMode.fromLabel(string); }
        });
        configureComboStyle(phpPasteCombo);
        HBox phpPasteRow = new HBox(8, phpPasteLabel, phpPasteCombo);
        phpPasteRow.setAlignment(Pos.CENTER_LEFT);

        styleCheckBox(phpFileScopeCheck);
        styleCheckBox(phpNamespaceScopeCheck);

        HBox phpGlobalHeader = createSectionHeader("Treat symbols from the global space");

        List<String> symbolOptions = List.of("prefer FQN", "prefer fallback", "prefer unqualified");
        phpClassCombo.getItems().setAll(symbolOptions);
        configureComboStyle(phpClassCombo);

        phpFunctionCombo.getItems().setAll(symbolOptions);
        configureComboStyle(phpFunctionCombo);

        phpConstantCombo.getItems().setAll(symbolOptions);
        configureComboStyle(phpConstantCombo);

        GridPane phpGrid = new GridPane();
        phpGrid.setHgap(10);
        phpGrid.setVgap(6);
        phpGrid.setPadding(new Insets(2, 0, 0, 16));

        Label classLbl = new Label("Class:");
        classLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        phpGrid.add(classLbl, 0, 0);
        phpGrid.add(phpClassCombo, 1, 0);

        Label funcLbl = new Label("Function:");
        funcLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        phpGrid.add(funcLbl, 0, 1);
        phpGrid.add(phpFunctionCombo, 1, 1);

        Label constLbl = new Label("Constant:");
        constLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        phpGrid.add(constLbl, 0, 2);
        phpGrid.add(phpConstantCombo, 1, 2);

        VBox phpBox = new VBox(6,
                phpHeader,
                phpPasteRow,
                phpFileScopeCheck,
                phpNamespaceScopeCheck,
                phpGlobalHeader,
                phpGrid
        );

        getChildren().addAll(
                xmlBox,
                javaBox,
                pythonBox,
                rustBox,
                scalaBox,
                jspBox,
                kotlinBox,
                ktorBox,
                jsTsBox,
                phpBox
        );
    }

    private void setupJavaListView(ListView<JavaImportEntry> listView) {
        listView.setEditable(true);
        listView.setMinHeight(95);
        listView.setPrefHeight(95);
        listView.setMaxHeight(120);
        listView.setStyle("-fx-background-color: #1E1F22; -fx-background-insets: 0; -fx-padding: 0; -fx-border-width: 0;");
        listView.setCellFactory(lv -> new JavaImportListCell(this::notifyModified));
    }

    private VBox createJavaListContainer(ListView<JavaImportEntry> listView, ObservableList<JavaImportEntry> list) {
        setupJavaListView(listView);

        Button addBtn = new Button("+");
        Button removeBtn = new Button("—");
        HBox toolbar = createToolbar(addBtn, removeBtn);

        addBtn.setOnAction(e -> {
            JavaImportEntry newEntry = new JavaImportEntry("", RustScope.IDE);
            list.add(newEntry);
            listView.getSelectionModel().select(newEntry);
            listView.scrollTo(newEntry);
            int idx = list.size() - 1;
            Platform.runLater(() -> listView.edit(idx));
            notifyModified();
        });

        removeBtn.setOnAction(e -> {
            JavaImportEntry sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                list.remove(sel);
                notifyModified();
            }
        });

        listView.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.DELETE || e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
                JavaImportEntry sel = listView.getSelectionModel().getSelectedItem();
                if (sel != null && listView.getEditingIndex() < 0) {
                    list.remove(sel);
                    notifyModified();
                }
            } else if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                int idx = listView.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && listView.getEditingIndex() < 0) {
                    listView.edit(idx);
                }
            }
        });

        VBox container = new VBox(toolbar, listView);
        container.setStyle("-fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: #1E1F22;");
        return container;
    }

    private static class JavaImportListCell extends ListCell<JavaImportEntry> {
        private final HBox rowBox = new HBox(8);
        private final Label label = new Label();
        private final TextField textField = new TextField();
        private final ComboBox<RustScope> scopeCombo = new ComboBox<>();
        private final Runnable onModified;

        public JavaImportListCell(Runnable onModified) {
            this.onModified = onModified;

            rowBox.setAlignment(Pos.CENTER_LEFT);
            rowBox.setPadding(new Insets(2, 8, 2, 8));
            rowBox.setPrefHeight(24);

            label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            HBox.setHgrow(label, Priority.ALWAYS);
            label.setMaxWidth(Double.MAX_VALUE);

            textField.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #3574F0; -fx-border-width: 1; -fx-border-radius: 2; -fx-background-radius: 2; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 2 6 2 6;");
            HBox.setHgrow(textField, Priority.ALWAYS);
            textField.setMaxWidth(Double.MAX_VALUE);

            textField.setOnAction(e -> commitEdit(getItem()));
            textField.focusedProperty().addListener((obs, oldV, focused) -> {
                if (!focused && isEditing()) {
                    commitEdit(getItem());
                }
            });
            textField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });

            scopeCombo.getItems().setAll(RustScope.values());
            scopeCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(RustScope o) { return o != null ? o.getLabel() : ""; }
                @Override public RustScope fromString(String s) { return RustScope.fromLabel(s); }
            });
            scopeCombo.setPrefWidth(75);
            scopeCombo.setMinWidth(70);
            scopeCombo.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 0 4 0 4;");
            scopeCombo.setOnAction(e -> {
                JavaImportEntry item = getItem();
                if (item != null && scopeCombo.getValue() != null && item.getScope() != scopeCombo.getValue()) {
                    item.setScope(scopeCombo.getValue());
                    if (onModified != null) onModified.run();
                }
            });

            selectedProperty().addListener((obs, o, isSel) -> updateBackground(isSel));
            hoverProperty().addListener((obs, o, isHover) -> {
                if (!isSelected()) {
                    setStyle(isHover ? "-fx-background-color: #26282E; -fx-padding: 0;" : "-fx-background-color: transparent; -fx-padding: 0;");
                }
            });

            setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !isEmpty()) {
                    startEdit();
                }
            });
        }

        private void updateBackground(boolean isSel) {
            if (isSel) {
                setStyle("-fx-background-color: #2E436E; -fx-padding: 0;");
            } else {
                setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            }
        }

        @Override
        public void startEdit() {
            if (!isEditable() || !getListView().isEditable()) return;
            super.startEdit();
            JavaImportEntry item = getItem();
            textField.setText(item != null ? item.getItem() : "");
            scopeCombo.setValue(item != null ? item.getScope() : RustScope.IDE);
            rowBox.getChildren().setAll(textField, scopeCombo);
            setGraphic(rowBox);
            setText(null);
            Platform.runLater(() -> {
                textField.requestFocus();
                textField.selectAll();
            });
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            updateDisplay(getItem());
        }

        @Override
        public void commitEdit(JavaImportEntry item) {
            if (item != null) {
                String newText = textField.getText() != null ? textField.getText().trim() : "";
                item.setItem(newText);
                if (onModified != null) onModified.run();
            }
            super.commitEdit(item);
            updateDisplay(item);
        }

        @Override
        protected void updateItem(JavaImportEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
            } else if (isEditing()) {
                textField.setText(item.getItem());
                scopeCombo.setValue(item.getScope());
                rowBox.getChildren().setAll(textField, scopeCombo);
                setGraphic(rowBox);
                setText(null);
                updateBackground(isSelected());
            } else {
                updateDisplay(item);
            }
        }

        private void updateDisplay(JavaImportEntry item) {
            if (item == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }
            label.setText(item.getItem().isEmpty() ? " " : item.getItem());
            scopeCombo.setValue(item.getScope());
            rowBox.getChildren().setAll(label, scopeCombo);
            setGraphic(rowBox);
            setText(null);
            updateBackground(isSelected());
        }
    }

    private TableColumn<RustExcludeEntry, String> buildRustTable() {
        rustExcludeTable.setMinHeight(160);
        rustExcludeTable.setPrefHeight(160);
        rustExcludeTable.setMaxHeight(200);
        rustExcludeTable.setEditable(true);
        rustExcludeTable.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent; -fx-border-width: 0;");
        rustExcludeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<RustExcludeEntry, String> itemCol = new TableColumn<>("Item or module");
        itemCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItemOrModule()));
        itemCol.setCellFactory(tc -> new TableCell<>() {
            private final TextField textField = new TextField();
            {
                textField.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #3574F0; -fx-border-width: 1; -fx-border-radius: 2; -fx-background-radius: 2; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 2 6 2 6;");
                textField.setOnAction(e -> commitEdit(textField.getText()));
                textField.focusedProperty().addListener((obs, oldV, focused) -> {
                    if (!focused && isEditing()) {
                        commitEdit(textField.getText());
                    }
                });
            }

            @Override
            public void startEdit() {
                super.startEdit();
                RustExcludeEntry entry = getTableRow() != null ? getTableRow().getItem() : null;
                textField.setText(entry != null ? entry.getItemOrModule() : "");
                setText(null);
                setGraphic(textField);
                Platform.runLater(() -> {
                    textField.requestFocus();
                    textField.selectAll();
                });
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                RustExcludeEntry entry = getTableRow() != null ? getTableRow().getItem() : null;
                setText(entry != null ? entry.getItemOrModule() : null);
                setGraphic(null);
            }

            @Override
            public void commitEdit(String newValue) {
                super.commitEdit(newValue);
                RustExcludeEntry entry = getTableRow() != null ? getTableRow().getItem() : null;
                if (entry != null) {
                    entry.setItemOrModule(newValue != null ? newValue.trim() : "");
                    setText(entry.getItemOrModule());
                    setGraphic(null);
                    notifyModified();
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (isEditing()) {
                    textField.setText(item != null ? item : "");
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(item);
                    setGraphic(null);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                }
            }
        });
        itemCol.setMinWidth(240);
        itemCol.setPrefWidth(280);

        TableColumn<RustExcludeEntry, RustApplyTo> applyCol = new TableColumn<>("Apply to");
        applyCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getApplyTo()));
        applyCol.setCellFactory(tc -> new TableCell<>() {
            private final ComboBox<RustApplyTo> combo = new ComboBox<>();
            {
                combo.getItems().setAll(RustApplyTo.values());
                combo.setConverter(new javafx.util.StringConverter<>() {
                    @Override public String toString(RustApplyTo o) { return o != null ? o.getLabel() : ""; }
                    @Override public RustApplyTo fromString(String s) { return RustApplyTo.fromLabel(s); }
                });
                combo.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 0;");
                combo.setOnAction(e -> {
                    RustExcludeEntry row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row != null && combo.getValue() != null && row.getApplyTo() != combo.getValue()) {
                        row.setApplyTo(combo.getValue());
                        notifyModified();
                    }
                });
            }

            @Override
            protected void updateItem(RustApplyTo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    combo.setValue(item);
                    setGraphic(combo);
                }
            }
        });
        applyCol.setMinWidth(130);
        applyCol.setPrefWidth(140);
        applyCol.setMaxWidth(160);

        TableColumn<RustExcludeEntry, RustScope> scopeCol = new TableColumn<>("Scope");
        scopeCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getScope()));
        scopeCol.setCellFactory(tc -> new TableCell<>() {
            private final ComboBox<RustScope> combo = new ComboBox<>();
            {
                combo.getItems().setAll(RustScope.values());
                combo.setConverter(new javafx.util.StringConverter<>() {
                    @Override public String toString(RustScope o) { return o != null ? o.getLabel() : ""; }
                    @Override public RustScope fromString(String s) { return RustScope.fromLabel(s); }
                });
                combo.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 0;");
                combo.setOnAction(e -> {
                    RustExcludeEntry row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row != null && combo.getValue() != null && row.getScope() != combo.getValue()) {
                        row.setScope(combo.getValue());
                        notifyModified();
                    }
                });
            }

            @Override
            protected void updateItem(RustScope item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    combo.setValue(item);
                    setGraphic(combo);
                }
            }
        });
        scopeCol.setMinWidth(80);
        scopeCol.setPrefWidth(85);
        scopeCol.setMaxWidth(100);

        rustExcludeTable.getColumns().setAll(itemCol, applyCol, scopeCol);
        return itemCol;
    }

    private void setupListeners() {
        // XML
        xmlShowTooltipCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        // Java
        javaClassesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaStaticMethodsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaPasteCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        javaUnambiguousCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        javaOptimizeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        // Python
        pythonShowTooltipCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        pythonFromModuleRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());
        pythonImportModuleRadio.selectedProperty().addListener((obs, o, n) -> notifyModified());

        // Rust
        rustShowPopupCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        rustOutOfScopeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        rustInsertPasteCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        rustCrateDepsCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        rustUnambiguousCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        // Scala
        scalaPasteCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        scalaPopupClassesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaPopupStaticCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaPopupConversionsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaPopupDefinitionsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaPopupExtensionsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaUnambiguousClassesCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaUnambiguousStaticCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        scalaOptimizeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        // JSP
        jspUnambiguousCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        // Kotlin
        kotlinUnambiguousCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        kotlinOptimizeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        // Ktor
        ktorAddImportsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        // JS / TS
        jsAddImportsCheck.selectedProperty().addListener((obs, o, n) -> {
            jsOnCompletionCheck.setDisable(!n);
            jsWithTooltipCheck.setDisable(!n);
            notifyModified();
        });
        jsOnCompletionCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        jsWithTooltipCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        tsAddImportsCheck.selectedProperty().addListener((obs, o, n) -> {
            tsOnCompletionCheck.setDisable(!n);
            tsWithTooltipCheck.setDisable(!n);
            tsUnambiguousCheck.setDisable(!n);
            notifyModified();
        });
        tsOnCompletionCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        tsWithTooltipCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        tsUnambiguousCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());

        // PHP
        phpPasteCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        phpFileScopeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        phpNamespaceScopeCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        phpClassCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        phpFunctionCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        phpConstantCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
    }

    public void loadFromSettings(AutoImportSettings s) {
        suppressEvents = true;
        try {
            // XML
            xmlShowTooltipCheck.setSelected(s.isXmlShowAutoImportTooltip());

            // Java
            javaClassesCheck.setSelected(s.isJavaShowTooltipClasses());
            javaStaticMethodsCheck.setSelected(s.isJavaShowTooltipStaticMethods());
            javaPasteCombo.setValue(s.getJavaInsertImportsOnPaste());
            javaUnambiguousCheck.setSelected(s.isJavaAddUnambiguousImportsOnTheFly());
            javaOptimizeCheck.setSelected(s.isJavaOptimizeImportsOnTheFly());
            javaStaticMembersList.clear();
            for (JavaImportEntry e : s.getJavaIncludeStaticMembers()) {
                javaStaticMembersList.add(new JavaImportEntry(e.getItem(), e.getScope()));
            }
            javaExcludeList.clear();
            for (JavaImportEntry e : s.getJavaExcludeFromAutoImport()) {
                javaExcludeList.add(new JavaImportEntry(e.getItem(), e.getScope()));
            }

            // Python
            pythonShowTooltipCheck.setSelected(s.isPythonShowAutoImportTooltip());
            if (s.getPythonPreferredImportStyle() == PythonImportStyle.FROM_MODULE_IMPORT_NAME) {
                pythonFromModuleRadio.setSelected(true);
            } else {
                pythonImportModuleRadio.setSelected(true);
            }

            // Rust
            rustShowPopupCheck.setSelected(s.isRustShowImportPopup());
            rustOutOfScopeCheck.setSelected(s.isRustImportOutOfScopeItems());
            rustInsertPasteCheck.setSelected(s.isRustInsertImportsOnPaste());
            rustCrateDepsCombo.setValue(s.getRustAddCrateDependenciesOnPaste());
            rustUnambiguousCheck.setSelected(s.isRustAddUnambiguousImportsOnTheFly());
            rustExcludeList.clear();
            if (s.getRustExcludeEntries().isEmpty()) {
                s.initDefaultRustExclusions();
            }
            for (RustExcludeEntry e : s.getRustExcludeEntries()) {
                rustExcludeList.add(new RustExcludeEntry(e.getItemOrModule(), e.getApplyTo(), e.getScope()));
            }

            // Scala
            scalaPasteCombo.setValue(s.getScalaInsertImportsOnPaste());
            scalaPopupClassesCheck.setSelected(s.isScalaShowPopupClasses());
            scalaPopupStaticCheck.setSelected(s.isScalaShowPopupStaticMembers());
            scalaPopupConversionsCheck.setSelected(s.isScalaShowPopupImplicitConversions());
            scalaPopupDefinitionsCheck.setSelected(s.isScalaShowPopupImplicitDefinitions());
            scalaPopupExtensionsCheck.setSelected(s.isScalaShowPopupExtensionMethods());
            scalaUnambiguousClassesCheck.setSelected(s.isScalaAddUnambiguousClasses());
            scalaUnambiguousStaticCheck.setSelected(s.isScalaAddUnambiguousStaticMembers());
            scalaOptimizeCheck.setSelected(s.isScalaOptimizeImportsOnTheFly());

            // JSP
            jspUnambiguousCheck.setSelected(s.isJspAddUnambiguousImportsOnTheFly());

            // Kotlin
            kotlinUnambiguousCheck.setSelected(s.isKotlinAddUnambiguousImportsOnTheFly());
            kotlinOptimizeCheck.setSelected(s.isKotlinOptimizeImportsOnTheFly());

            // Ktor
            ktorAddImportsCheck.setSelected(s.isKtorAddImportsAutomatically());

            // TS / JS
            jsAddImportsCheck.setSelected(s.isJsAddImportsAutomatically());
            jsOnCompletionCheck.setDisable(!s.isJsAddImportsAutomatically());
            jsOnCompletionCheck.setSelected(s.isJsOnCodeCompletion());
            jsWithTooltipCheck.setDisable(!s.isJsAddImportsAutomatically());
            jsWithTooltipCheck.setSelected(s.isJsWithAutoImportTooltip());

            tsAddImportsCheck.setSelected(s.isTsAddImportsAutomatically());
            tsOnCompletionCheck.setDisable(!s.isTsAddImportsAutomatically());
            tsOnCompletionCheck.setSelected(s.isTsOnCodeCompletion());
            tsWithTooltipCheck.setDisable(!s.isTsAddImportsAutomatically());
            tsWithTooltipCheck.setSelected(s.isTsWithAutoImportTooltip());
            tsUnambiguousCheck.setDisable(!s.isTsAddImportsAutomatically());
            tsUnambiguousCheck.setSelected(s.isTsUnambiguousImportsOnTheFly());

            // PHP
            phpPasteCombo.setValue(s.getPhpInsertImportsOnPaste());
            phpFileScopeCheck.setSelected(s.isPhpEnableInFileScope());
            phpNamespaceScopeCheck.setSelected(s.isPhpEnableInNamespaceScope());
            phpClassCombo.setValue(s.getPhpClassGlobalSymbol());
            phpFunctionCombo.setValue(s.getPhpFunctionGlobalSymbol());
            phpConstantCombo.setValue(s.getPhpConstantGlobalSymbol());
        } finally {
            suppressEvents = false;
        }
    }

    public void saveToSettings(AutoImportSettings s) {
        // XML
        s.setXmlShowAutoImportTooltip(xmlShowTooltipCheck.isSelected());

        // Java
        s.setJavaShowTooltipClasses(javaClassesCheck.isSelected());
        s.setJavaShowTooltipStaticMethods(javaStaticMethodsCheck.isSelected());
        if (javaPasteCombo.getValue() != null) s.setJavaInsertImportsOnPaste(javaPasteCombo.getValue());
        s.setJavaAddUnambiguousImportsOnTheFly(javaUnambiguousCheck.isSelected());
        s.setJavaOptimizeImportsOnTheFly(javaOptimizeCheck.isSelected());
        s.getJavaIncludeStaticMembers().clear();
        for (JavaImportEntry e : javaStaticMembersList) {
            if (!e.getItem().isBlank()) s.getJavaIncludeStaticMembers().add(new JavaImportEntry(e.getItem(), e.getScope()));
        }
        s.getJavaExcludeFromAutoImport().clear();
        for (JavaImportEntry e : javaExcludeList) {
            if (!e.getItem().isBlank()) s.getJavaExcludeFromAutoImport().add(new JavaImportEntry(e.getItem(), e.getScope()));
        }

        // Python
        s.setPythonShowAutoImportTooltip(pythonShowTooltipCheck.isSelected());
        s.setPythonPreferredImportStyle(pythonFromModuleRadio.isSelected()
                ? PythonImportStyle.FROM_MODULE_IMPORT_NAME
                : PythonImportStyle.IMPORT_MODULE_NAME);

        // Rust
        s.setRustShowImportPopup(rustShowPopupCheck.isSelected());
        s.setRustImportOutOfScopeItems(rustOutOfScopeCheck.isSelected());
        s.setRustInsertImportsOnPaste(rustInsertPasteCheck.isSelected());
        if (rustCrateDepsCombo.getValue() != null) s.setRustAddCrateDependenciesOnPaste(rustCrateDepsCombo.getValue());
        s.setRustAddUnambiguousImportsOnTheFly(rustUnambiguousCheck.isSelected());
        s.getRustExcludeEntries().clear();
        for (RustExcludeEntry e : rustExcludeList) {
            if (!e.getItemOrModule().isBlank()) {
                s.getRustExcludeEntries().add(new RustExcludeEntry(e.getItemOrModule(), e.getApplyTo(), e.getScope()));
            }
        }

        // Scala
        if (scalaPasteCombo.getValue() != null) s.setScalaInsertImportsOnPaste(scalaPasteCombo.getValue());
        s.setScalaShowPopupClasses(scalaPopupClassesCheck.isSelected());
        s.setScalaShowPopupStaticMembers(scalaPopupStaticCheck.isSelected());
        s.setScalaShowPopupImplicitConversions(scalaPopupConversionsCheck.isSelected());
        s.setScalaShowPopupImplicitDefinitions(scalaPopupDefinitionsCheck.isSelected());
        s.setScalaShowPopupExtensionMethods(scalaPopupExtensionsCheck.isSelected());
        s.setScalaAddUnambiguousClasses(scalaUnambiguousClassesCheck.isSelected());
        s.setScalaAddUnambiguousStaticMembers(scalaUnambiguousStaticCheck.isSelected());
        s.setScalaOptimizeImportsOnTheFly(scalaOptimizeCheck.isSelected());

        // JSP
        s.setJspAddUnambiguousImportsOnTheFly(jspUnambiguousCheck.isSelected());

        // Kotlin
        s.setKotlinAddUnambiguousImportsOnTheFly(kotlinUnambiguousCheck.isSelected());
        s.setKotlinOptimizeImportsOnTheFly(kotlinOptimizeCheck.isSelected());

        // Ktor
        s.setKtorAddImportsAutomatically(ktorAddImportsCheck.isSelected());

        // TS / JS
        s.setJsAddImportsAutomatically(jsAddImportsCheck.isSelected());
        s.setJsOnCodeCompletion(jsOnCompletionCheck.isSelected());
        s.setJsWithAutoImportTooltip(jsWithTooltipCheck.isSelected());

        s.setTsAddImportsAutomatically(tsAddImportsCheck.isSelected());
        s.setTsOnCodeCompletion(tsOnCompletionCheck.isSelected());
        s.setTsWithAutoImportTooltip(tsWithTooltipCheck.isSelected());
        s.setTsUnambiguousImportsOnTheFly(tsUnambiguousCheck.isSelected());

        // PHP
        if (phpPasteCombo.getValue() != null) s.setPhpInsertImportsOnPaste(phpPasteCombo.getValue());
        s.setPhpEnableInFileScope(phpFileScopeCheck.isSelected());
        s.setPhpEnableInNamespaceScope(phpNamespaceScopeCheck.isSelected());
        if (phpClassCombo.getValue() != null) s.setPhpClassGlobalSymbol(phpClassCombo.getValue());
        if (phpFunctionCombo.getValue() != null) s.setPhpFunctionGlobalSymbol(phpFunctionCombo.getValue());
        if (phpConstantCombo.getValue() != null) s.setPhpConstantGlobalSymbol(phpConstantCombo.getValue());
    }

    public boolean isModified() {
        AutoImportSettings current = new AutoImportSettings();
        saveToSettings(current);
        return current.isModified(AutoImportSettings.getInstance());
    }

    public void apply() {
        saveToSettings(AutoImportSettings.getInstance());
        AutoImportSettings.getInstance().save();
    }

    public void reset() {
        loadFromSettings(AutoImportSettings.getInstance());
    }
}