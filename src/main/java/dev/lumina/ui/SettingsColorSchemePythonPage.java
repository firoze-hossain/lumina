package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.AttributesDescriptor;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.*;
import java.util.function.Consumer;

/**
 * 1:1 visual & functional replication of Editor > Color Scheme > Python.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemePythonPage extends VBox {

    private final EditorColorSchemeSettings settings = EditorColorSchemeSettings.getInstance();
    private final ColorSchemeHeaderBar headerBar;

    private final TreeView<String> categoryTree;
    private final TreeItem<String> rootItem;

    // Attribute controls
    private final CheckBox boldCheck;
    private final CheckBox italicCheck;

    private final CheckBox foregroundCheck;
    private final Button foregroundSwatch;
    private String foregroundHex = null;

    private final CheckBox backgroundCheck;
    private final Button backgroundSwatch;
    private String backgroundHex = null;

    private final CheckBox errorStripeCheck;
    private final Button errorStripeSwatch;
    private String errorStripeHex = null;

    private final CheckBox effectsCheck;
    private final Button effectsSwatch;
    private String effectsHex = null;
    private final ComboBox<EffectType> effectTypeCombo;

    // Inheritance controls
    private final VBox inheritBox;
    private final CheckBox inheritCheck;
    private final Hyperlink inheritTargetLabel;
    private final Label inheritScopeLabel;

    // Preview
    private final ScrollPane previewScrollPane;
    private final VBox codeLinesBox;

    private String selectedKey = "Keyword argument";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemePythonPage() {
        setSpacing(10);
        setPadding(new Insets(12, 16, 16, 16));
        setStyle("-fx-background-color: #1E1F22;");

        headerBar = new ColorSchemeHeaderBar();
        headerBar.setOnSchemeChanged(scheme -> {
            loadScheme(scheme);
            updatePreview();
        });

        // ---------------- Left TreeView ----------------
        rootItem = new TreeItem<>("Root");
        rootItem.setExpanded(true);

        TreeItem<String> bracesGroup = new TreeItem<>("Braces and Operators");
        bracesGroup.getChildren().addAll(
                new TreeItem<>("Braces"),
                new TreeItem<>("Brackets"),
                new TreeItem<>("Comma"),
                new TreeItem<>("Dot"),
                new TreeItem<>("Operation sign"),
                new TreeItem<>("Parentheses"),
                new TreeItem<>("Semicolon")
        );

        TreeItem<String> builtInNameItem = new TreeItem<>("Built-in name");
        TreeItem<String> classDefItem = new TreeItem<>("Class definition");
        TreeItem<String> decoratorItem = new TreeItem<>("Decorator");

        TreeItem<String> docstringGroup = new TreeItem<>("Docstring");
        docstringGroup.getChildren().addAll(
                new TreeItem<>("Docstring"),
                new TreeItem<>("Docstring tag"),
                new TreeItem<>("Docstring value")
        );

        TreeItem<String> functionsGroup = new TreeItem<>("Functions");
        functionsGroup.getChildren().addAll(
                new TreeItem<>("Function call"),
                new TreeItem<>("Function declaration"),
                new TreeItem<>("Method call"),
                new TreeItem<>("Nested function definition")
        );

        TreeItem<String> keywordItem = new TreeItem<>("Keyword");
        TreeItem<String> keywordArgItem = new TreeItem<>("Keyword argument");
        TreeItem<String> lineCommentItem = new TreeItem<>("Line comment");
        TreeItem<String> localVarsItem = new TreeItem<>("Local variables");
        TreeItem<String> numberItem = new TreeItem<>("Number");

        TreeItem<String> paramsGroup = new TreeItem<>("Parameters");
        paramsGroup.getChildren().addAll(
                new TreeItem<>("Parameter"),
                new TreeItem<>("Self parameter")
        );

        TreeItem<String> semanticHighlightingItem = new TreeItem<>("Semantic highlighting");

        TreeItem<String> specialNamesGroup = new TreeItem<>("Special names");
        specialNamesGroup.getChildren().addAll(
                new TreeItem<>("Dunder method"),
                new TreeItem<>("Predefined definition")
        );

        TreeItem<String> stringGroup = new TreeItem<>("String");
        stringGroup.getChildren().addAll(
                new TreeItem<>("String"),
                new TreeItem<>("f-string expression"),
                new TreeItem<>("f-string text"),
                new TreeItem<>("Valid escape sequence"),
                new TreeItem<>("Invalid escape sequence")
        );

        TreeItem<String> typeAnnotationItem = new TreeItem<>("Type annotation");
        TreeItem<String> typeParamsItem = new TreeItem<>("Type parameters");

        rootItem.getChildren().addAll(
                bracesGroup, builtInNameItem, classDefItem, decoratorItem,
                docstringGroup, functionsGroup, keywordItem, keywordArgItem,
                lineCommentItem, localVarsItem, numberItem, paramsGroup,
                semanticHighlightingItem, specialNamesGroup, stringGroup,
                typeAnnotationItem, typeParamsItem
        );

        categoryTree = new TreeView<>(rootItem);
        categoryTree.setShowRoot(false);
        categoryTree.getStyleClass().addAll("color-scheme-tree", "settings-tree-view");
        categoryTree.setPrefWidth(300);
        categoryTree.setPrefHeight(270);
        categoryTree.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        categoryTree.setCellFactory(tv -> new TreeCell<>() {
            {
                setOnMouseEntered(e -> {
                    if (!isEmpty() && getItem() != null && !isSelected()) {
                        setStyle("-fx-background-color: #35373B; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8;");
                    }
                });
                setOnMouseExited(e -> {
                    if (!isEmpty() && getItem() != null && !isSelected()) {
                        setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8;");
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    updateStyle();
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                updateStyle();
            }

            private void updateStyle() {
                if (isEmpty() || getItem() == null) {
                    setStyle("-fx-background-color: transparent;");
                } else if (isSelected()) {
                    setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-padding: 3 8 3 8;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8;");
                }
            }
        });

        TextField searchField = new TextField();
        searchField.setPromptText("Filter...");
        searchField.getStyleClass().add("settings-search-field");
        searchField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTree(newVal));

        VBox treeBox = new VBox(6, searchField, categoryTree);
        VBox.setVgrow(categoryTree, Priority.ALWAYS);
        treeBox.setPrefWidth(300);
        treeBox.setMinWidth(260);

        // ---------------- Right Controls ----------------
        boldCheck = new CheckBox("Bold");
        boldCheck.setStyle("-fx-text-fill: #BCBEC4;");
        italicCheck = new CheckBox("Italic");
        italicCheck.setStyle("-fx-text-fill: #BCBEC4;");
        HBox fontStylesBox = new HBox(16, boldCheck, italicCheck);
        fontStylesBox.setAlignment(Pos.CENTER_LEFT);

        // Foreground
        foregroundCheck = new CheckBox("Foreground");
        foregroundCheck.setStyle("-fx-text-fill: #BCBEC4;");
        foregroundCheck.setPrefWidth(120);
        foregroundSwatch = createColorButton();
        foregroundSwatch.setOnAction(e -> showColorChooser("Foreground", foregroundHex, color -> {
            foregroundHex = color;
            updateSwatch(foregroundSwatch, foregroundHex);
            foregroundCheck.setSelected(true);
            markModified();
            updatePreview();
        }));
        HBox fgRow = new HBox(12, foregroundCheck, foregroundSwatch);
        fgRow.setAlignment(Pos.CENTER_LEFT);

        // Background
        backgroundCheck = new CheckBox("Background");
        backgroundCheck.setStyle("-fx-text-fill: #BCBEC4;");
        backgroundCheck.setPrefWidth(120);
        backgroundSwatch = createColorButton();
        backgroundSwatch.setOnAction(e -> showColorChooser("Background", backgroundHex, color -> {
            backgroundHex = color;
            updateSwatch(backgroundSwatch, backgroundHex);
            backgroundCheck.setSelected(true);
            markModified();
            updatePreview();
        }));
        HBox bgRow = new HBox(12, backgroundCheck, backgroundSwatch);
        bgRow.setAlignment(Pos.CENTER_LEFT);

        // Error stripe mark
        errorStripeCheck = new CheckBox("Error stripe mark");
        errorStripeCheck.setStyle("-fx-text-fill: #BCBEC4;");
        errorStripeCheck.setPrefWidth(120);
        errorStripeSwatch = createColorButton();
        errorStripeSwatch.setOnAction(e -> showColorChooser("Error stripe mark", errorStripeHex, color -> {
            errorStripeHex = color;
            updateSwatch(errorStripeSwatch, errorStripeHex);
            errorStripeCheck.setSelected(true);
            markModified();
            updatePreview();
        }));
        HBox esRow = new HBox(12, errorStripeCheck, errorStripeSwatch);
        esRow.setAlignment(Pos.CENTER_LEFT);

        // Effects
        effectsCheck = new CheckBox("Effects");
        effectsCheck.setStyle("-fx-text-fill: #BCBEC4;");
        effectsCheck.setPrefWidth(120);
        effectsSwatch = createColorButton();
        effectsSwatch.setOnAction(e -> showColorChooser("Effects", effectsHex, color -> {
            effectsHex = color;
            updateSwatch(effectsSwatch, effectsHex);
            effectsCheck.setSelected(true);
            markModified();
            updatePreview();
        }));
        effectTypeCombo = new ComboBox<>();
        effectTypeCombo.getItems().addAll(EffectType.values());
        effectTypeCombo.setValue(EffectType.BORDERED);
        effectTypeCombo.setPrefWidth(110);

        HBox effRow = new HBox(12, effectsCheck, effectsSwatch, effectTypeCombo);
        effRow.setAlignment(Pos.CENTER_LEFT);

        // Inherit section
        inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: #BCBEC4; -fx-font-weight: bold;");
        inheritTargetLabel = new Hyperlink("Identifiers->Parameter");
        inheritTargetLabel.setStyle("-fx-text-fill: #56A8F5; -fx-underline: false; -fx-padding: 0;");
        inheritTargetLabel.setOnAction(e -> {
            if (onNavigateToInherited != null) {
                AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
                if (desc != null && desc.getInheritTarget() != null) {
                    onNavigateToInherited.accept(desc.getInheritTarget());
                }
            }
        });
        inheritScopeLabel = new Label("(Language Defaults)");
        inheritScopeLabel.setStyle("-fx-text-fill: #7A7E85;");

        VBox inheritDetailsBox = new VBox(2, inheritTargetLabel, inheritScopeLabel);
        inheritDetailsBox.setPadding(new Insets(2, 0, 0, 20));

        inheritBox = new VBox(4, inheritCheck, inheritDetailsBox);
        inheritBox.setPadding(new Insets(8, 0, 0, 0));

        VBox rightControls = new VBox(10, fontStylesBox, fgRow, bgRow, esRow, effRow, inheritBox);
        rightControls.setPadding(new Insets(10, 16, 16, 20));
        HBox.setHgrow(rightControls, Priority.ALWAYS);

        HBox topSplitPane = new HBox(12, treeBox, rightControls);
        topSplitPane.setPrefHeight(270);

        // ---------------- Bottom Preview Pane ----------------
        codeLinesBox = new VBox(2);
        codeLinesBox.setPadding(new Insets(10, 12, 10, 12));
        codeLinesBox.setStyle("-fx-background-color: #1E1F22;");

        previewScrollPane = new ScrollPane(codeLinesBox);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
        previewScrollPane.setPrefHeight(240);
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        getChildren().addAll(headerBar, topSplitPane, previewScrollPane);

        // Listeners for modifications
        boldCheck.setOnAction(e -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        italicCheck.setOnAction(e -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        foregroundCheck.setOnAction(e -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        backgroundCheck.setOnAction(e -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        errorStripeCheck.setOnAction(e -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        effectsCheck.setOnAction(e -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        effectTypeCombo.setOnAction(e -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        inheritCheck.setOnAction(e -> { if (!suppressEvents) { markModified(); updatePreview(); } });

        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getChildren().isEmpty()) {
                String fullKey = resolveFullKey(newVal);
                selectCategory(fullKey);
            }
        });

        // Initialize scheme and select default item
        loadScheme(settings.getActiveSchemeName());
        categoryTree.getSelectionModel().select(keywordArgItem);
        selectCategory("Keyword argument");
        updatePreview();
    }

    private String resolveFullKey(TreeItem<String> item) {
        if (item.getParent() != null && item.getParent() != rootItem) {
            return item.getParent().getValue() + " // " + item.getValue();
        }
        return item.getValue();
    }

    private void filterTree(String query) {
        if (query == null || query.isBlank()) {
            categoryTree.setRoot(rootItem);
            return;
        }
        String lower = query.toLowerCase();
        TreeItem<String> filteredRoot = new TreeItem<>("Filtered");
        filteredRoot.setExpanded(true);

        for (TreeItem<String> child : rootItem.getChildren()) {
            if (child.getChildren().isEmpty()) {
                if (child.getValue().toLowerCase().contains(lower)) {
                    filteredRoot.getChildren().add(new TreeItem<>(child.getValue()));
                }
            } else {
                TreeItem<String> groupCopy = new TreeItem<>(child.getValue());
                groupCopy.setExpanded(true);
                for (TreeItem<String> leaf : child.getChildren()) {
                    if (leaf.getValue().toLowerCase().contains(lower)) {
                        groupCopy.getChildren().add(new TreeItem<>(leaf.getValue()));
                    }
                }
                if (!groupCopy.getChildren().isEmpty()) {
                    filteredRoot.getChildren().add(groupCopy);
                }
            }
        }
        categoryTree.setRoot(filteredRoot);
    }

    private Button createColorButton() {
        Button btn = new Button();
        btn.setPrefSize(74, 24);
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #BCBEC4; -fx-border-color: #55575E; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-family: 'Monospaced', monospace; -fx-font-size: 11px;");
        return btn;
    }

    private void updateSwatch(Button btn, String hex) {
        if (hex != null && !hex.isBlank()) {
            String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
            btn.setText(cleanHex.toUpperCase());
            btn.setStyle("-fx-background-color: #" + cleanHex + "; -fx-text-fill: " + getContrastTextColor(cleanHex) + "; -fx-border-color: #55575E; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-family: 'Monospaced', monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            btn.setText("");
            btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #BCBEC4; -fx-border-color: #55575E; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-family: 'Monospaced', monospace; -fx-font-size: 11px;");
        }
    }

    private String getContrastTextColor(String hexColor) {
        try {
            int r = Integer.parseInt(hexColor.substring(0, 2), 16);
            int g = Integer.parseInt(hexColor.substring(2, 4), 16);
            int b = Integer.parseInt(hexColor.substring(4, 6), 16);
            double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
            return luminance > 0.5 ? "#000000" : "#FFFFFF";
        } catch (Exception e) {
            return "#BCBEC4";
        }
    }

    private void loadScheme(String schemeName) {
        originalAttributes.clear();
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getPythonDescriptors()) {
            ColorAttribute attr = settings.getAttribute(schemeName, desc.getKey());
            if (attr != null) {
                originalAttributes.put(desc.getKey(), attr.clone());
            }
        }
        selectCategory(selectedKey);
    }

    public void selectCategory(String key) {
        this.selectedKey = key;
        suppressEvents = true;
        try {
            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(key);
            ColorAttribute current = settings.getAttribute(settings.getActiveSchemeName(), key);
            if (current == null && desc != null) {
                current = desc.getDefaultAttribute();
            }

            if (desc != null && desc.getInheritTarget() != null) {
                inheritBox.setVisible(true);
                inheritBox.setManaged(true);
                inheritTargetLabel.setText(desc.getInheritTarget());
                inheritScopeLabel.setText(desc.getInheritScope() != null ? desc.getInheritScope() : "(Language Defaults)");
                inheritCheck.setSelected(current != null && current.isInherit());
            } else {
                inheritBox.setVisible(false);
                inheritBox.setManaged(false);
                inheritCheck.setSelected(false);
            }

            if (current != null) {
                boldCheck.setSelected(current.isBold());
                italicCheck.setSelected(current.isItalic());

                foregroundCheck.setSelected(current.getForeground() != null);
                foregroundHex = current.getForeground() != null ? current.getForeground() : (desc != null && desc.getDefaultAttribute() != null ? desc.getDefaultAttribute().getForeground() : "#AA4926");
                updateSwatch(foregroundSwatch, foregroundHex);

                backgroundCheck.setSelected(current.getBackground() != null);
                backgroundHex = current.getBackground() != null ? current.getBackground() : "";
                updateSwatch(backgroundSwatch, backgroundHex);

                errorStripeCheck.setSelected(current.getErrorStripeColor() != null);
                errorStripeHex = current.getErrorStripeColor() != null ? current.getErrorStripeColor() : "";
                updateSwatch(errorStripeSwatch, errorStripeHex);

                effectsCheck.setSelected(current.getEffectType() != null && current.getEffectType() != EffectType.NONE);
                effectsHex = current.getEffectColor() != null ? current.getEffectColor() : "";
                updateSwatch(effectsSwatch, effectsHex);
                if (current.getEffectType() != null && current.getEffectType() != EffectType.NONE) {
                    effectTypeCombo.setValue(current.getEffectType());
                } else {
                    effectTypeCombo.setValue(EffectType.BORDERED);
                }
            }
        } finally {
            suppressEvents = false;
        }
    }

    private void showColorChooser(String title, String initialColorHex, Consumer<String> onSelected) {
        Stage pickerStage = new Stage();
        pickerStage.initModality(Modality.APPLICATION_MODAL);
        pickerStage.initStyle(StageStyle.UTILITY);
        pickerStage.setTitle("Choose " + title + " Color");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #2B2D30;");

        Color initial = Color.web(initialColorHex != null && !initialColorHex.isBlank() ? initialColorHex : "#AA4926");
        ColorPicker picker = new ColorPicker(initial);

        TextField hexField = new TextField(initialColorHex != null ? initialColorHex : "#AA4926");
        hexField.setPromptText("#RRGGBB");
        hexField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #BCBEC4; -fx-border-color: #55575E;");

        picker.valueProperty().addListener((obs, oldVal, newVal) -> {
            String hex = String.format("#%02X%02X%02X",
                    (int) (newVal.getRed() * 255),
                    (int) (newVal.getGreen() * 255),
                    (int) (newVal.getBlue() * 255));
            hexField.setText(hex);
        });

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.getStyleClass().add("dialog-button");
        okBtn.setOnAction(e -> {
            onSelected.accept(hexField.getText().trim());
            pickerStage.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.getStyleClass().add("dialog-button");
        cancelBtn.setOnAction(e -> pickerStage.close());

        btnBox.getChildren().addAll(okBtn, cancelBtn);
        content.getChildren().addAll(new Label("Select color:"), picker, hexField, btnBox);

        Scene scene = new Scene(content, 280, 200);
        pickerStage.setScene(scene);
        pickerStage.showAndWait();
    }

    private void updatePreview() {
        codeLinesBox.getChildren().clear();

        // Python code lines matching media_1790473064608.png
        // @decorator(param=1)
        // def f(x):
        //     """
        //     Syntax Highlighting Demo
        //     @param x Parameter
        //     """
        //
        //     Semantic highlighting:
        //     Generated spectrum to pick colors for local variables and parameters:
        //     Color#1 SC1.1 SC1.2 SC1.3 SC1.4 Color#2 SC2.1 SC2.2 SC2.3 SC2.4 Color#3
        //     Color#3 SC3.1 SC3.2 SC3.3 SC3.4 Color#4 SC4.1 SC4.2 SC4.3 SC4.4 Color#5

        codeLinesBox.getChildren().addAll(
                buildLine(
                        new Token("@decorator", "Decorator", "#B3AE60"),
                        new Token("(", "Braces and Operators // Parentheses", "#BCBEC4"),
                        new Token("param", "Keyword argument", "#AA4926"),
                        new Token("=", "Braces and Operators // Operation sign", "#BCBEC4"),
                        new Token("1", "Number", "#2AAC88"),
                        new Token(")", "Braces and Operators // Parentheses", "#BCBEC4")
                ),
                buildLine(
                        new Token("def", "Keyword", "#CF8E6D"),
                        new Token(" ", null, null),
                        new Token("f", "Functions // Function declaration", "#56A8F5"),
                        new Token("(", "Braces and Operators // Parentheses", "#BCBEC4"),
                        new Token("x", "Parameters // Parameter", "#BCBEC4"),
                        new Token("):", "Braces and Operators // Parentheses", "#BCBEC4")
                ),
                buildLine(
                        new Token("    \"\"\"", "Docstring // Docstring", "#5F8C7D", false, true)
                ),
                buildLine(
                        new Token("    Syntax Highlighting Demo", "Docstring // Docstring", "#5F8C7D", false, true)
                ),
                buildLine(
                        new Token("    ", null, null),
                        new Token("@param", "Docstring // Docstring tag", "#56A8F5"),
                        new Token(" ", null, null),
                        new Token("x", "Docstring // Docstring value", "#5F8C7D"),
                        new Token(" Parameter", "Docstring // Docstring", "#5F8C7D", false, true)
                ),
                buildLine(
                        new Token("    \"\"\"", "Docstring // Docstring", "#5F8C7D", false, true)
                ),
                buildLine(new Token("", null, null)),
                buildLine(
                        new Token("    Semantic highlighting:", "Line comment", "#7A7E85", false, true)
                ),
                buildLine(
                        new Token("    Generated spectrum to pick colors for local variables and parameters:", "Line comment", "#7A7E85", false, true)
                ),
                buildLine(
                        new Token("    ", null, null),
                        new Token("Color#1", "Local variables", "#3C82EB"),
                        new Token(" ", null, null),
                        new Token("SC1.1", "Local variables", "#4BB594"),
                        new Token(" ", null, null),
                        new Token("SC1.2", "Local variables", "#D86B5A"),
                        new Token(" ", null, null),
                        new Token("SC1.3", "Local variables", "#B3AE60"),
                        new Token(" ", null, null),
                        new Token("SC1.4", "Local variables", "#C77DBB"),
                        new Token(" ", null, null),
                        new Token("Color#2", "Local variables", "#3C82EB"),
                        new Token(" ", null, null),
                        new Token("SC2.1", "Local variables", "#4BB594"),
                        new Token(" ", null, null),
                        new Token("SC2.2", "Local variables", "#D86B5A"),
                        new Token(" ", null, null),
                        new Token("SC2.3", "Local variables", "#B3AE60"),
                        new Token(" ", null, null),
                        new Token("SC2.4", "Local variables", "#C77DBB"),
                        new Token(" ", null, null),
                        new Token("Color#3", "Local variables", "#3C82EB")
                ),
                buildLine(
                        new Token("    ", null, null),
                        new Token("Color#3", "Local variables", "#3C82EB"),
                        new Token(" ", null, null),
                        new Token("SC3.1", "Local variables", "#4BB594"),
                        new Token(" ", null, null),
                        new Token("SC3.2", "Local variables", "#D86B5A"),
                        new Token(" ", null, null),
                        new Token("SC3.3", "Local variables", "#B3AE60"),
                        new Token(" ", null, null),
                        new Token("SC3.4", "Local variables", "#C77DBB"),
                        new Token(" ", null, null),
                        new Token("Color#4", "Local variables", "#3C82EB"),
                        new Token(" ", null, null),
                        new Token("SC4.1", "Local variables", "#4BB594"),
                        new Token(" ", null, null),
                        new Token("SC4.2", "Local variables", "#D86B5A"),
                        new Token(" ", null, null),
                        new Token("SC4.3", "Local variables", "#B3AE60"),
                        new Token(" ", null, null),
                        new Token("SC4.4", "Local variables", "#C77DBB"),
                        new Token(" ", null, null),
                        new Token("Color#5", "Local variables", "#3C82EB")
                )
        );
    }

    private static class Token {
        final String text;
        final String attributeKey;
        final String defaultColor;
        final boolean isBold;
        final boolean isItalic;
        final EffectType effectType;
        final String effectColor;

        Token(String text, String attributeKey, String defaultColor) {
            this(text, attributeKey, defaultColor, false, false, EffectType.NONE, null);
        }

        Token(String text, String attributeKey, String defaultColor, boolean isBold, boolean isItalic) {
            this(text, attributeKey, defaultColor, isBold, isItalic, EffectType.NONE, null);
        }

        Token(String text, String attributeKey, String defaultColor, boolean isBold, boolean isItalic, EffectType effectType, String effectColor) {
            this.text = text;
            this.attributeKey = attributeKey;
            this.defaultColor = defaultColor;
            this.isBold = isBold;
            this.isItalic = isItalic;
            this.effectType = effectType;
            this.effectColor = effectColor;
        }
    }

    private HBox buildLine(Token... tokens) {
        HBox line = new HBox();
        line.setAlignment(Pos.CENTER_LEFT);

        for (Token tok : tokens) {
            Label label = new Label(tok.text);
            label.setFont(Font.font("Monospaced", 12.5));

            String fg = tok.defaultColor != null ? tok.defaultColor : "#BCBEC4";
            String bg = null;
            boolean bold = tok.isBold;
            boolean italic = tok.isItalic;
            EffectType eff = tok.effectType;
            String effColor = tok.effectColor;

            if (tok.attributeKey != null) {
                ColorAttribute attr = settings.resolveAttribute(settings.getActiveSchemeName(), tok.attributeKey);
                if (attr != null) {
                    if (attr.getForeground() != null) fg = attr.getForeground();
                    if (attr.getBackground() != null) bg = attr.getBackground();
                    if (attr.isBold()) bold = true;
                    if (attr.isItalic()) italic = true;
                    if (attr.getEffectType() != null && attr.getEffectType() != EffectType.NONE) {
                        eff = attr.getEffectType();
                        effColor = attr.getEffectColor();
                    }
                }

                final String targetKey = tok.attributeKey;
                label.setOnMouseClicked(e -> {
                    selectKeyInTree(targetKey);
                    selectCategory(targetKey);
                });
                label.setStyle("-fx-cursor: hand;");
            }

            StringBuilder css = new StringBuilder();
            css.append("-fx-text-fill: ").append(fg).append(";");
            if (bg != null && !bg.isBlank()) {
                css.append(" -fx-background-color: ").append(bg).append("; -fx-padding: 0 1 0 1;");
            }
            if (bold) {
                css.append(" -fx-font-weight: bold;");
            }
            if (italic) {
                css.append(" -fx-font-style: italic;");
            }
            if (eff == EffectType.BORDERED && effColor != null) {
                css.append(" -fx-border-color: ").append(effColor).append("; -fx-border-width: 1; -fx-padding: 0 1 0 1;");
            } else if (eff == EffectType.UNDERSCORED && effColor != null) {
                css.append(" -fx-border-color: transparent transparent ").append(effColor).append(" transparent; -fx-border-width: 0 0 1 0;");
            }

            label.setStyle(label.getStyle() + " " + css);
            line.getChildren().add(label);
        }
        return line;
    }

    private void selectKeyInTree(String fullKey) {
        for (TreeItem<String> item : rootItem.getChildren()) {
            if (item.getChildren().isEmpty()) {
                if (item.getValue().equals(fullKey)) {
                    categoryTree.getSelectionModel().select(item);
                    return;
                }
            } else {
                for (TreeItem<String> leaf : item.getChildren()) {
                    String leafFull = item.getValue() + " // " + leaf.getValue();
                    if (leafFull.equals(fullKey) || leaf.getValue().equals(fullKey)) {
                        categoryTree.getSelectionModel().select(leaf);
                        return;
                    }
                }
            }
        }
    }

    private void markModified() {
        isModified = true;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnNavigateToInheritedListener(Consumer<String> listener) {
        this.onNavigateToInherited = listener;
    }

    public boolean isModified() {
        return isModified;
    }

    public void apply() {
        if (!isModified) return;

        ColorAttribute current = new ColorAttribute();
        current.setInherit(inheritCheck.isSelected());
        if (foregroundCheck.isSelected() && foregroundHex != null) {
            current.setForeground(foregroundHex);
        }
        if (backgroundCheck.isSelected() && backgroundHex != null) {
            current.setBackground(backgroundHex);
        }
        if (errorStripeCheck.isSelected() && errorStripeHex != null) {
            current.setErrorStripeColor(errorStripeHex);
        }
        if (effectsCheck.isSelected()) {
            current.setEffectType(effectTypeCombo.getValue());
            current.setEffectColor(effectsHex);
        }
        current.setBold(boldCheck.isSelected());
        current.setItalic(italicCheck.isSelected());

        AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
        if (desc != null) {
            current.setInheritFrom(desc.getInheritTarget());
        }

        settings.setAttribute(settings.getActiveSchemeName(), selectedKey, current);
        isModified = false;
        loadScheme(settings.getActiveSchemeName());
        updatePreview();
    }

    public void reset() {
        for (Map.Entry<String, ColorAttribute> entry : originalAttributes.entrySet()) {
            settings.setAttribute(settings.getActiveSchemeName(), entry.getKey(), entry.getValue());
        }
        isModified = false;
        loadScheme(settings.getActiveSchemeName());
        updatePreview();
    }

    public TreeView<String> getCategoryTree() {
        return categoryTree;
    }

    public ColorSchemeHeaderBar getHeaderBar() {
        return headerBar;
    }

    public boolean isInheritChecked() {
        return inheritCheck.isSelected();
    }

    public void selectTreeItem(String key) {
        selectKeyInTree(key);
        selectCategory(key);
    }

    public String getSelectedKey() {
        return selectedKey;
    }

    public CheckBox getBoldCheck() {
        return boldCheck;
    }

    public CheckBox getItalicCheck() {
        return italicCheck;
    }

    public CheckBox getForegroundCheck() {
        return foregroundCheck;
    }

    public Button getForegroundSwatch() {
        return foregroundSwatch;
    }

    public CheckBox getBackgroundCheck() {
        return backgroundCheck;
    }

    public Button getBackgroundSwatch() {
        return backgroundSwatch;
    }

    public CheckBox getErrorStripeCheck() {
        return errorStripeCheck;
    }

    public Button getErrorStripeSwatch() {
        return errorStripeSwatch;
    }

    public CheckBox getEffectsCheck() {
        return effectsCheck;
    }

    public Button getEffectsSwatch() {
        return effectsSwatch;
    }

    public ComboBox<EffectType> getEffectTypeCombo() {
        return effectTypeCombo;
    }

    public CheckBox getInheritCheck() {
        return inheritCheck;
    }

    public Hyperlink getInheritTargetLabel() {
        return inheritTargetLabel;
    }

    public Label getInheritScopeLabel() {
        return inheritScopeLabel;
    }

    public VBox getInheritBox() {
        return inheritBox;
    }

    public VBox getCodeLinesBox() {
        return codeLinesBox;
    }
}
