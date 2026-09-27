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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.*;
import java.util.function.Consumer;

/**
 * 1:1 visual & functional replication of Editor > Color Scheme > Ruby.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemeRubyPage extends VBox {

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

    private String selectedKey = "Constants // Constant declaration";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemeRubyPage() {
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

        TreeItem<String> commentItem = new TreeItem<>("Comment");

        TreeItem<String> constantsGroup = new TreeItem<>("Constants");
        constantsGroup.setExpanded(true);
        TreeItem<String> constDeclItem = new TreeItem<>("Constant declaration");
        constantsGroup.getChildren().addAll(
                new TreeItem<>("Constant"),
                constDeclItem
        );

        TreeItem<String> hashesGroup = new TreeItem<>("Hashes");
        hashesGroup.getChildren().addAll(
                new TreeItem<>("Hash key"),
                new TreeItem<>("Hash value"),
                new TreeItem<>("Symbol hash key")
        );

        TreeItem<String> identifiersGroup = new TreeItem<>("Identifiers");
        identifiersGroup.getChildren().addAll(
                new TreeItem<>("Constant"),
                new TreeItem<>("Global variable"),
                new TreeItem<>("Instance variable"),
                new TreeItem<>("Local variable"),
                new TreeItem<>("Parameter")
        );

        TreeItem<String> keywordItem = new TreeItem<>("Keyword");

        TreeItem<String> methodsGroup = new TreeItem<>("Methods");
        methodsGroup.getChildren().addAll(
                new TreeItem<>("Method call"),
                new TreeItem<>("Method declaration")
        );

        TreeItem<String> numberItem = new TreeItem<>("Number");

        TreeItem<String> stringsGroup = new TreeItem<>("Strings and Symbols");
        stringsGroup.getChildren().addAll(
                new TreeItem<>("Escape sequence"),
                new TreeItem<>("String text"),
                new TreeItem<>("Symbol")
        );

        TreeItem<String> variablesGroup = new TreeItem<>("Variables");
        variablesGroup.setExpanded(true);
        variablesGroup.getChildren().addAll(
                new TreeItem<>("Class variable"),
                new TreeItem<>("Global variable"),
                new TreeItem<>("Instance variable"),
                new TreeItem<>("Local variable")
        );

        rootItem.getChildren().addAll(
                bracesGroup,
                commentItem,
                constantsGroup,
                hashesGroup,
                identifiersGroup,
                keywordItem,
                methodsGroup,
                numberItem,
                stringsGroup,
                variablesGroup
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
        foregroundSwatch.setOnAction(e -> showColorPickerPopup(foregroundSwatch, foregroundHex, color -> {
            foregroundHex = color;
            updateSwatch(foregroundSwatch, color);
            markModified();
            updatePreview();
        }));
        HBox fgRow = new HBox(8, foregroundCheck, foregroundSwatch);
        fgRow.setAlignment(Pos.CENTER_LEFT);

        // Background
        backgroundCheck = new CheckBox("Background");
        backgroundCheck.setStyle("-fx-text-fill: #BCBEC4;");
        backgroundCheck.setPrefWidth(120);
        backgroundSwatch = createColorButton();
        backgroundSwatch.setOnAction(e -> showColorPickerPopup(backgroundSwatch, backgroundHex, color -> {
            backgroundHex = color;
            updateSwatch(backgroundSwatch, color);
            markModified();
            updatePreview();
        }));
        HBox bgRow = new HBox(8, backgroundCheck, backgroundSwatch);
        bgRow.setAlignment(Pos.CENTER_LEFT);

        // Error stripe
        errorStripeCheck = new CheckBox("Error stripe mark");
        errorStripeCheck.setStyle("-fx-text-fill: #BCBEC4;");
        errorStripeCheck.setPrefWidth(120);
        errorStripeSwatch = createColorButton();
        errorStripeSwatch.setOnAction(e -> showColorPickerPopup(errorStripeSwatch, errorStripeHex, color -> {
            errorStripeHex = color;
            updateSwatch(errorStripeSwatch, color);
            markModified();
            updatePreview();
        }));
        HBox esRow = new HBox(8, errorStripeCheck, errorStripeSwatch);
        esRow.setAlignment(Pos.CENTER_LEFT);

        // Effects
        effectsCheck = new CheckBox("Effects");
        effectsCheck.setStyle("-fx-text-fill: #BCBEC4;");
        effectsCheck.setPrefWidth(120);
        effectsSwatch = createColorButton();
        effectsSwatch.setOnAction(e -> showColorPickerPopup(effectsSwatch, effectsHex, color -> {
            effectsHex = color;
            updateSwatch(effectsSwatch, color);
            markModified();
            updatePreview();
        }));
        effectTypeCombo = new ComboBox<>();
        effectTypeCombo.getItems().addAll(EffectType.values());
        effectTypeCombo.setValue(EffectType.BORDERED);
        effectTypeCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #BCBEC4; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox effRow = new HBox(8, effectsCheck, effectsSwatch, effectTypeCombo);
        effRow.setAlignment(Pos.CENTER_LEFT);

        // Inherit section
        inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: #BCBEC4; -fx-font-weight: bold;");
        inheritTargetLabel = new Hyperlink("Identifiers->Constant");
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
        boldCheck.selectedProperty().addListener((o, ov, nv) -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        italicCheck.selectedProperty().addListener((o, ov, nv) -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        foregroundCheck.selectedProperty().addListener((o, ov, nv) -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        backgroundCheck.selectedProperty().addListener((o, ov, nv) -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        errorStripeCheck.selectedProperty().addListener((o, ov, nv) -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        effectsCheck.selectedProperty().addListener((o, ov, nv) -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        effectTypeCombo.valueProperty().addListener((o, ov, nv) -> { if (!suppressEvents) { markModified(); updatePreview(); } });
        inheritCheck.selectedProperty().addListener((o, ov, nv) -> { if (!suppressEvents) { markModified(); updatePreview(); } });

        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isLeaf()) {
                String fullKey = getFullPath(newVal);
                selectCategory(fullKey);
            }
        });

        // Initialize scheme and select default item
        loadScheme(settings.getActiveSchemeName());
        categoryTree.getSelectionModel().select(constDeclItem);
        categoryTree.scrollTo(categoryTree.getRow(constDeclItem));
        selectCategory("Constants // Constant declaration");
        updatePreview();
    }

    private String getFullPath(TreeItem<String> item) {
        if (item == null || item.getParent() == null || item.getParent() == rootItem) {
            return item != null ? item.getValue() : "";
        }
        Deque<String> parts = new ArrayDeque<>();
        TreeItem<String> curr = item;
        while (curr != null && curr != rootItem) {
            parts.addFirst(curr.getValue());
            curr = curr.getParent();
        }
        return String.join(" // ", parts);
    }

    private void filterTree(String query) {
        if (query == null || query.isBlank()) {
            categoryTree.setRoot(rootItem);
            return;
        }
        String lower = query.toLowerCase();
        TreeItem<String> filteredRoot = new TreeItem<>("Filtered");
        filteredRoot.setExpanded(true);
        filterNode(rootItem, filteredRoot, lower);
        categoryTree.setRoot(filteredRoot);
    }

    private boolean filterNode(TreeItem<String> source, TreeItem<String> targetParent, String query) {
        for (TreeItem<String> child : source.getChildren()) {
            if (child.getChildren().isEmpty()) {
                if (child.getValue().toLowerCase().contains(query)) {
                    targetParent.getChildren().add(new TreeItem<>(child.getValue()));
                }
            } else {
                TreeItem<String> groupCopy = new TreeItem<>(child.getValue());
                boolean matchesGroup = child.getValue().toLowerCase().contains(query);
                if (matchesGroup) {
                    for (TreeItem<String> sub : child.getChildren()) {
                        groupCopy.getChildren().add(new TreeItem<>(sub.getValue()));
                    }
                    groupCopy.setExpanded(true);
                    targetParent.getChildren().add(groupCopy);
                } else {
                    boolean hasMatch = filterNode(child, groupCopy, query);
                    if (hasMatch) {
                        groupCopy.setExpanded(true);
                        targetParent.getChildren().add(groupCopy);
                    }
                }
            }
        }
        return !targetParent.getChildren().isEmpty();
    }

    public void selectCategory(String key) {
        this.selectedKey = key;
        AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(key);

        suppressEvents = true;
        try {
            ColorAttribute attr = getCurrentAttribute(key);
            if (attr == null && desc != null) {
                attr = desc.getDefaultAttribute();
            }

            boolean canInherit = desc != null && desc.getInheritTarget() != null;
            inheritBox.setVisible(canInherit);
            inheritBox.setManaged(canInherit);

            if (canInherit) {
                inheritCheck.setSelected(attr != null && attr.isInherit());
                inheritTargetLabel.setText(desc.getInheritTarget());
                inheritScopeLabel.setText(desc.getInheritScope() != null ? desc.getInheritScope() : "(Language Defaults)");
            }

            boolean isInheriting = canInherit && inheritCheck.isSelected();
            ColorAttribute inherited = isInheriting ? resolveInheritedAttribute(desc) : null;
            ColorAttribute effective = inherited != null ? inherited : attr;

            boldCheck.setSelected(effective != null && effective.isBold());
            italicCheck.setSelected(effective != null && effective.isItalic());

            foregroundHex = effective != null ? effective.getForeground() : null;
            foregroundCheck.setSelected(foregroundHex != null);
            updateSwatch(foregroundSwatch, foregroundHex);

            backgroundHex = effective != null ? effective.getBackground() : null;
            backgroundCheck.setSelected(backgroundHex != null);
            updateSwatch(backgroundSwatch, backgroundHex);

            errorStripeHex = effective != null ? effective.getErrorStripeColor() : null;
            errorStripeCheck.setSelected(errorStripeHex != null);
            updateSwatch(errorStripeSwatch, errorStripeHex);

            effectsHex = effective != null ? effective.getEffectColor() : null;
            effectsCheck.setSelected(effectsHex != null);
            updateSwatch(effectsSwatch, effectsHex);

            if (effective != null && effective.getEffectType() != null) {
                effectTypeCombo.setValue(effective.getEffectType());
            } else {
                effectTypeCombo.setValue(EffectType.BORDERED);
            }

            boolean disabled = isInheriting;
            boldCheck.setDisable(disabled);
            italicCheck.setDisable(disabled);
            foregroundCheck.setDisable(disabled);
            foregroundSwatch.setDisable(disabled);
            backgroundCheck.setDisable(disabled);
            backgroundSwatch.setDisable(disabled);
            errorStripeCheck.setDisable(disabled);
            errorStripeSwatch.setDisable(disabled);
            effectsCheck.setDisable(disabled);
            effectsSwatch.setDisable(disabled);
            effectTypeCombo.setDisable(disabled);

        } finally {
            suppressEvents = false;
        }
    }

    public void selectTreeItem(String key) {
        TreeItem<String> item = findItem(rootItem, key);
        if (item != null) {
            TreeItem<String> parent = item.getParent();
            while (parent != null && parent != rootItem) {
                parent.setExpanded(true);
                parent = parent.getParent();
            }
            categoryTree.getSelectionModel().select(item);
            categoryTree.scrollTo(categoryTree.getRow(item));
        }
        selectCategory(key);
    }

    private TreeItem<String> findItem(TreeItem<String> item, String key) {
        if (item == null) return null;
        if (getFullPath(item).equals(key) || item.getValue().equals(key)) {
            return item;
        }
        for (TreeItem<String> child : item.getChildren()) {
            TreeItem<String> res = findItem(child, key);
            if (res != null) return res;
        }
        return null;
    }

    private ColorAttribute getCurrentAttribute(String key) {
        String scheme = settings.getActiveSchemeName();
        return settings.getAttribute(scheme, key);
    }

    private ColorAttribute resolveInheritedAttribute(AttributesDescriptor desc) {
        if (desc == null || desc.getInheritTarget() == null) return null;
        String target = desc.getInheritTarget();
        ColorAttribute attr = settings.getAttribute(settings.getActiveSchemeName(), target);
        if (attr == null) {
            String alt1 = target.replace("->", " // ");
            attr = settings.getAttribute(settings.getActiveSchemeName(), alt1);
            if (attr == null && target.contains("->")) {
                String alt2 = target.substring(target.lastIndexOf("->") + 2);
                attr = settings.getAttribute(settings.getActiveSchemeName(), alt2);
            }
        }
        return attr;
    }

    private void markModified() {
        if (suppressEvents) return;
        isModified = true;

        ColorAttribute attr = new ColorAttribute();
        attr.setBold(boldCheck.isSelected());
        attr.setItalic(italicCheck.isSelected());
        attr.setForeground(foregroundCheck.isSelected() ? foregroundHex : null);
        attr.setBackground(backgroundCheck.isSelected() ? backgroundHex : null);
        attr.setErrorStripeColor(errorStripeCheck.isSelected() ? errorStripeHex : null);
        attr.setEffectColor(effectsCheck.isSelected() ? effectsHex : null);
        attr.setEffectType(effectsCheck.isSelected() ? effectTypeCombo.getValue() : EffectType.NONE);
        attr.setInherit(inheritBox.isVisible() && inheritCheck.isSelected());

        AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
        if (desc != null) {
            attr.setInheritFrom(desc.getInheritTarget());
            attr.setInheritScope(desc.getInheritScope());
        }

        settings.setAttribute(settings.getActiveSchemeName(), selectedKey, attr);

        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void loadScheme(String schemeName) {
        originalAttributes.clear();
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getRubyDescriptors()) {
            ColorAttribute current = settings.getAttribute(schemeName, desc.getKey());
            if (current != null) {
                originalAttributes.put(desc.getKey(), current.clone());
            }
        }
        isModified = false;
        selectCategory(selectedKey);
    }

    public void apply() {
        isModified = false;
        settings.save();
        loadScheme(settings.getActiveSchemeName());
    }

    public void reset() {
        for (Map.Entry<String, ColorAttribute> entry : originalAttributes.entrySet()) {
            settings.setAttribute(settings.getActiveSchemeName(), entry.getKey(), entry.getValue().clone());
        }
        isModified = false;
        selectCategory(selectedKey);
        updatePreview();
    }

    public boolean isModified() {
        return isModified;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnNavigateToInheritedListener(Consumer<String> consumer) {
        this.onNavigateToInherited = consumer;
    }

    public ColorSchemeHeaderBar getHeaderBar() {
        return headerBar;
    }

    public TreeView<String> getCategoryTree() {
        return categoryTree;
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

    public CheckBox getInheritCheck() {
        return inheritCheck;
    }

    public Hyperlink getInheritTargetLabel() {
        return inheritTargetLabel;
    }

    public Label getInheritScopeLabel() {
        return inheritScopeLabel;
    }

    public VBox getCodeLinesBox() {
        return codeLinesBox;
    }

    // ---------------- Interactive Code Preview ----------------
    public void updatePreview() {
        codeLinesBox.getChildren().clear();

        ColorAttribute kwAttr = getEffectiveAttribute("Keyword");
        ColorAttribute stringAttr = getEffectiveAttribute("Strings and Symbols // String text");
        ColorAttribute constDeclAttr = getEffectiveAttribute("Constants // Constant declaration");
        ColorAttribute constAttr = getEffectiveAttribute("Constants // Constant");
        ColorAttribute opSignAttr = getEffectiveAttribute("Braces and Operators // Operation sign");
        ColorAttribute numAttr = getEffectiveAttribute("Number");
        ColorAttribute commentAttr = getEffectiveAttribute("Comment");
        ColorAttribute methodCallAttr = getEffectiveAttribute("Methods // Method call");
        ColorAttribute methodDeclAttr = getEffectiveAttribute("Methods // Method declaration");
        ColorAttribute symHashKeyAttr = getEffectiveAttribute("Hashes // Symbol hash key");
        ColorAttribute parenAttr = getEffectiveAttribute("Braces and Operators // Parentheses");
        ColorAttribute paramAttr = getEffectiveAttribute("Identifiers // Parameter");
        ColorAttribute instVarAttr = getEffectiveAttribute("Variables // Instance variable");
        ColorAttribute localVarAttr = getEffectiveAttribute("Variables // Local variable");
        ColorAttribute semiAttr = getEffectiveAttribute("Braces and Operators // Semicolon");

        // Line 1: require "test"
        HBox line1 = createLineBox();
        line1.getChildren().add(createStyledTokenSpan("require", kwAttr, "Keyword"));
        line1.getChildren().add(createPlainSpan(" "));
        line1.getChildren().add(createStyledTokenSpan("\"test\"", stringAttr, "Strings and Symbols // String text"));
        codeLinesBox.getChildren().add(line1);

        // Line 2: CONSTANT = 777
        HBox line2 = createLineBox();
        line2.getChildren().add(createStyledTokenSpan("CONSTANT", constDeclAttr, "Constants // Constant declaration"));
        line2.getChildren().add(createPlainSpan(" "));
        line2.getChildren().add(createStyledTokenSpan("=", opSignAttr, "Braces and Operators // Operation sign"));
        line2.getChildren().add(createPlainSpan(" "));
        line2.getChildren().add(createStyledTokenSpan("777", numAttr, "Number"));
        codeLinesBox.getChildren().add(line2);

        // Line 3: blank
        codeLinesBox.getChildren().add(createPlainSpan(""));

        // Line 4: # Sample comment
        HBox line4 = createLineBox();
        line4.getChildren().add(createStyledTokenSpan("# Sample comment", commentAttr, "Comment"));
        codeLinesBox.getChildren().add(line4);

        // Line 5: blank
        codeLinesBox.getChildren().add(createPlainSpan(""));

        // Line 6: class Module::Class
        HBox line6 = createLineBox();
        line6.getChildren().add(createStyledTokenSpan("class", kwAttr, "Keyword"));
        line6.getChildren().add(createPlainSpan(" "));
        line6.getChildren().add(createStyledTokenSpan("Module", constAttr, "Constants // Constant"));
        line6.getChildren().add(createStyledTokenSpan("::", opSignAttr, "Braces and Operators // Operation sign"));
        line6.getChildren().add(createStyledTokenSpan("Class", constDeclAttr, "Constants // Constant declaration"));
        codeLinesBox.getChildren().add(line6);

        // Line 7:   include Testcase
        HBox line7 = createLineBox();
        line7.getChildren().add(createPlainSpan("  "));
        line7.getChildren().add(createStyledTokenSpan("include", kwAttr, "Keyword"));
        line7.getChildren().add(createPlainSpan(" "));
        line7.getChildren().add(createStyledTokenSpan("Testcase", constAttr, "Constants // Constant"));
        codeLinesBox.getChildren().add(line7);

        // Line 8: blank
        codeLinesBox.getChildren().add(createPlainSpan(""));

        // Line 9:   render :action => 'foo'
        HBox line9 = createLineBox();
        line9.getChildren().add(createPlainSpan("  "));
        line9.getChildren().add(createStyledTokenSpan("render", methodCallAttr, "Methods // Method call"));
        line9.getChildren().add(createPlainSpan(" "));
        line9.getChildren().add(createStyledTokenSpan(":action", symHashKeyAttr, "Hashes // Symbol hash key"));
        line9.getChildren().add(createPlainSpan(" "));
        line9.getChildren().add(createStyledTokenSpan("=>", opSignAttr, "Braces and Operators // Operation sign"));
        line9.getChildren().add(createPlainSpan(" "));
        line9.getChildren().add(createStyledTokenSpan("'foo'", stringAttr, "Strings and Symbols // String text"));
        codeLinesBox.getChildren().add(line9);

        // Line 10:   def foo(parameter)
        HBox line10 = createLineBox();
        line10.getChildren().add(createPlainSpan("  "));
        line10.getChildren().add(createStyledTokenSpan("def", kwAttr, "Keyword"));
        line10.getChildren().add(createPlainSpan(" "));
        line10.getChildren().add(createStyledTokenSpan("foo", methodDeclAttr, "Methods // Method declaration"));
        line10.getChildren().add(createStyledTokenSpan("(", parenAttr, "Braces and Operators // Parentheses"));
        line10.getChildren().add(createStyledTokenSpan("parameter", paramAttr, "Identifiers // Parameter"));
        line10.getChildren().add(createStyledTokenSpan(")", parenAttr, "Braces and Operators // Parentheses"));
        codeLinesBox.getChildren().add(line10);

        // Line 11:     @parameter = parameter
        HBox line11 = createLineBox();
        line11.getChildren().add(createPlainSpan("    "));
        line11.getChildren().add(createStyledTokenSpan("@parameter", instVarAttr, "Variables // Instance variable"));
        line11.getChildren().add(createPlainSpan(" "));
        line11.getChildren().add(createStyledTokenSpan("=", opSignAttr, "Braces and Operators // Operation sign"));
        line11.getChildren().add(createPlainSpan(" "));
        line11.getChildren().add(createStyledTokenSpan("parameter", paramAttr, "Identifiers // Parameter"));
        codeLinesBox.getChildren().add(line11);

        // Line 12:   end
        HBox line12 = createLineBox();
        line12.getChildren().add(createPlainSpan("  "));
        line12.getChildren().add(createStyledTokenSpan("end", kwAttr, "Keyword"));
        codeLinesBox.getChildren().add(line12);

        // Line 13: blank
        codeLinesBox.getChildren().add(createPlainSpan(""));

        // Line 14:   local_var = eval <<-"FOO";\
        HBox line14 = createLineBox();
        line14.getChildren().add(createPlainSpan("  "));
        line14.getChildren().add(createStyledTokenSpan("local_var", localVarAttr, "Variables // Local variable"));
        line14.getChildren().add(createPlainSpan(" "));
        line14.getChildren().add(createStyledTokenSpan("=", opSignAttr, "Braces and Operators // Operation sign"));
        line14.getChildren().add(createPlainSpan(" "));
        line14.getChildren().add(createStyledTokenSpan("eval", methodCallAttr, "Methods // Method call"));
        line14.getChildren().add(createPlainSpan(" "));
        line14.getChildren().add(createStyledTokenSpan("<<-\"FOO\"", stringAttr, "Strings and Symbols // String text"));
        line14.getChildren().add(createStyledTokenSpan(";", semiAttr, "Braces and Operators // Semicolon"));
        line14.getChildren().add(createStyledTokenSpan("\\", opSignAttr, "Braces and Operators // Operation sign"));
        codeLinesBox.getChildren().add(line14);

        // Line 15:   printIndex "Hello world!"
        HBox line15 = createLineBox();
        line15.getChildren().add(createPlainSpan("  "));
        line15.getChildren().add(createStyledTokenSpan("printIndex", methodCallAttr, "Methods // Method call"));
        line15.getChildren().add(createPlainSpan(" "));
        line15.getChildren().add(createStyledTokenSpan("\"Hello world!\"", stringAttr, "Strings and Symbols // String text"));
        codeLinesBox.getChildren().add(line15);
    }

    private ColorAttribute getEffectiveAttribute(String key) {
        ColorAttribute attr = getCurrentAttribute(key);
        AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(key);
        if (attr == null && desc != null) {
            attr = desc.getDefaultAttribute();
        }
        if (attr != null && attr.isInherit() && desc != null && desc.getInheritTarget() != null) {
            return settings.getAttribute(settings.getActiveSchemeName(), desc.getInheritTarget());
        }
        return attr;
    }

    private HBox createLineBox() {
        HBox box = new HBox(0);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Label createPlainSpan(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: 'Fira Code', 'DejaVu Sans Mono', monospace; -fx-font-size: 13px; -fx-text-fill: #DFE1E5;");
        return lbl;
    }

    private Label createStyledTokenSpan(String text, ColorAttribute attr, String key) {
        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#DFE1E5";
        boolean bold = attr != null && attr.isBold();
        boolean italic = attr != null && attr.isItalic();

        Label lbl = new Label(text);
        StringBuilder sb = new StringBuilder("-fx-font-family: 'Fira Code', 'DejaVu Sans Mono', monospace; -fx-font-size: 13px;");
        if (fg != null) sb.append("-fx-text-fill: ").append(fg).append(";");
        if (bold) sb.append("-fx-font-weight: bold;");
        if (italic) sb.append("-fx-font-style: italic;");

        if (attr != null) {
            if (attr.getBackground() != null) {
                sb.append("-fx-background-color: ").append(attr.getBackground()).append(";");
            }
            if (attr.getEffectType() == EffectType.UNDERSCORED || attr.getEffectType() == EffectType.BOLD_UNDERSCORED) {
                lbl.setUnderline(true);
            }
        }
        lbl.setStyle(sb.toString());

        if (key != null) {
            lbl.setOnMouseClicked(e -> selectTreeItem(key));
            lbl.setOnMouseEntered(e -> lbl.setCursor(javafx.scene.Cursor.HAND));
            lbl.setOnMouseExited(e -> lbl.setCursor(javafx.scene.Cursor.DEFAULT));
        }
        return lbl;
    }

    private Button createColorButton() {
        Button btn = new Button();
        btn.setPrefSize(70, 22);
        btn.setMinSize(70, 22);
        btn.setMaxSize(70, 22);
        btn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3;");
        return btn;
    }

    private void updateSwatch(Button btn, String hex) {
        if (hex != null && !hex.isBlank()) {
            btn.setText(hex.startsWith("#") ? hex.substring(1).toUpperCase() : hex.toUpperCase());
            btn.setStyle("-fx-background-color: " + hex + "; -fx-text-fill: " + getContrastingText(hex) + "; -fx-font-family: monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3;");
        } else {
            btn.setText("");
            btn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3;");
        }
    }

    private String getContrastingText(String hex) {
        try {
            Color c = Color.web(hex.startsWith("#") ? hex : "#" + hex);
            double lum = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
            return lum > 0.5 ? "#000000" : "#FFFFFF";
        } catch (Exception e) {
            return "#FFFFFF";
        }
    }

    private void showColorPickerPopup(Button owner, String currentHex, Consumer<String> onSelected) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UTILITY);
        popup.setTitle("Choose Color");

        ColorPicker picker = new ColorPicker(currentHex != null ? Color.web(currentHex) : Color.WHITE);
        picker.setStyle("-fx-background-color: #2B2D30;");

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-padding: 4 12;");
        okBtn.setOnAction(e -> {
            Color c = picker.getValue();
            String hex = String.format("#%02X%02X%02X",
                    (int) (c.getRed() * 255),
                    (int) (c.getGreen() * 255),
                    (int) (c.getBlue() * 255));
            onSelected.accept(hex);
            popup.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-padding: 4 12;");
        cancelBtn.setOnAction(e -> popup.close());

        HBox btns = new HBox(8, okBtn, cancelBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(12, picker, btns);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1;");

        Scene scene = new Scene(content);
        popup.setScene(scene);
        popup.showAndWait();
    }
}
