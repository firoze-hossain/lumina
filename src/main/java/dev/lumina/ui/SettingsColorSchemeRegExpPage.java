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
 * 1:1 visual & functional replication of Editor > Color Scheme > RegExp.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemeRegExpPage extends VBox {

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

    private String selectedKey = "Comment";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemeRegExpPage() {
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

        TreeItem<String> commentItem = null;
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getRegExpDescriptors()) {
            TreeItem<String> item = new TreeItem<>(desc.getKey());
            rootItem.getChildren().add(item);
            if ("Comment".equals(desc.getKey())) {
                commentItem = item;
            }
        }

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
        inheritTargetLabel = new Hyperlink("Comments->Line comment");
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
            if (newVal != null && newVal.getChildren().isEmpty()) {
                selectCategory(newVal.getValue());
            }
        });

        // Initialize scheme and select default item
        loadScheme(settings.getActiveSchemeName());
        if (commentItem != null) {
            categoryTree.getSelectionModel().select(commentItem);
        }
        selectCategory("Comment");
        updatePreview();
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
            if (child.getValue().toLowerCase().contains(lower)) {
                filteredRoot.getChildren().add(new TreeItem<>(child.getValue()));
            }
        }
        categoryTree.setRoot(filteredRoot);
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
        for (TreeItem<String> child : rootItem.getChildren()) {
            if (child.getValue().equals(key)) {
                categoryTree.getSelectionModel().select(child);
                categoryTree.scrollTo(categoryTree.getRow(child));
                selectCategory(key);
                return;
            }
        }
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
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getRegExpDescriptors()) {
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

        ColorAttribute opAttr = getEffectiveAttribute("Operator character");
        ColorAttribute bracketAttr = getEffectiveAttribute("Bracket");
        ColorAttribute charClassAttr = getEffectiveAttribute("Character class");
        ColorAttribute plainAttr = getEffectiveAttribute("Plain character");
        ColorAttribute quantAttr = getEffectiveAttribute("Quantifier");
        ColorAttribute parenAttr = getEffectiveAttribute("Parenthesis");
        ColorAttribute escAttr = getEffectiveAttribute("Escaped character");
        ColorAttribute redEscAttr = getEffectiveAttribute("Redundant escape sequence");
        ColorAttribute braceAttr = getEffectiveAttribute("Brace");
        ColorAttribute commaAttr = getEffectiveAttribute("Comma");
        ColorAttribute inlineOptAttr = getEffectiveAttribute("Inline option");
        ColorAttribute invalidEscAttr = getEffectiveAttribute("Invalid escape sequence");
        ColorAttribute matchedGroupAttr = getEffectiveAttribute("Matched groups");
        ColorAttribute commentAttr = getEffectiveAttribute("Comment");
        ColorAttribute dotAttr = getEffectiveAttribute("Dot");
        ColorAttribute quoteEscAttr = getEffectiveAttribute("Quote escape");

        // Line 1: ^[\w.-]+@([\[\w\-]+|\.)+[A-Z0-9]{2,4}(?x)
        HBox line1 = createLineBox();
        line1.getChildren().add(createStyledTokenSpan("^", opAttr, "Operator character"));
        line1.getChildren().add(createStyledTokenSpan("[", bracketAttr, "Bracket"));
        line1.getChildren().add(createStyledTokenSpan("\\w", charClassAttr, "Character class"));
        line1.getChildren().add(createStyledTokenSpan(".", plainAttr, "Plain character"));
        line1.getChildren().add(createStyledTokenSpan("-", plainAttr, "Plain character"));
        line1.getChildren().add(createStyledTokenSpan("]", bracketAttr, "Bracket"));
        line1.getChildren().add(createStyledTokenSpan("+", quantAttr, "Quantifier"));
        line1.getChildren().add(createStyledTokenSpan("@", plainAttr, "Plain character"));
        line1.getChildren().add(createStyledTokenSpan("(", parenAttr, "Parenthesis"));
        line1.getChildren().add(createStyledTokenSpan("[", bracketAttr, "Bracket"));
        line1.getChildren().add(createStyledTokenSpan("\\[", escAttr, "Escaped character"));
        line1.getChildren().add(createStyledTokenSpan("\\w", charClassAttr, "Character class"));
        line1.getChildren().add(createStyledTokenSpan("\\-", redEscAttr, "Redundant escape sequence"));
        line1.getChildren().add(createStyledTokenSpan("]", bracketAttr, "Bracket"));
        line1.getChildren().add(createStyledTokenSpan("+", quantAttr, "Quantifier"));
        line1.getChildren().add(createStyledTokenSpan("|", opAttr, "Operator character"));
        line1.getChildren().add(createStyledTokenSpan("\\.", escAttr, "Escaped character"));
        line1.getChildren().add(createStyledTokenSpan(")", parenAttr, "Parenthesis"));
        line1.getChildren().add(createStyledTokenSpan("+", quantAttr, "Quantifier"));
        line1.getChildren().add(createStyledTokenSpan("[", bracketAttr, "Bracket"));
        line1.getChildren().add(createStyledTokenSpan("A", plainAttr, "Plain character"));
        line1.getChildren().add(createStyledTokenSpan("-", opAttr, "Operator character"));
        line1.getChildren().add(createStyledTokenSpan("Z", plainAttr, "Plain character"));
        line1.getChildren().add(createStyledTokenSpan("0", plainAttr, "Plain character"));
        line1.getChildren().add(createStyledTokenSpan("-", opAttr, "Operator character"));
        line1.getChildren().add(createStyledTokenSpan("9", plainAttr, "Plain character"));
        line1.getChildren().add(createStyledTokenSpan("]", bracketAttr, "Bracket"));
        line1.getChildren().add(createStyledTokenSpan("{", braceAttr, "Brace"));
        line1.getChildren().add(createStyledTokenSpan("2", quantAttr, "Quantifier"));
        line1.getChildren().add(createStyledTokenSpan(",", commaAttr, "Comma"));
        line1.getChildren().add(createStyledTokenSpan("4", quantAttr, "Quantifier"));
        line1.getChildren().add(createStyledTokenSpan("}", braceAttr, "Brace"));
        line1.getChildren().add(createStyledTokenSpan("(", parenAttr, "Parenthesis"));
        line1.getChildren().add(createStyledTokenSpan("?x", inlineOptAttr, "Inline option"));
        line1.getChildren().add(createStyledTokenSpan(")", parenAttr, "Parenthesis"));
        codeLinesBox.getChildren().add(line1);

        // Line 2: \x0g\#\p{Alpha}\1(?#comment)
        HBox line2 = createLineBox();
        line2.getChildren().add(createStyledTokenSpan("\\x0g", invalidEscAttr, "Invalid escape sequence"));
        line2.getChildren().add(createStyledTokenSpan("\\#", escAttr, "Escaped character"));
        line2.getChildren().add(createStyledTokenSpan("\\p{Alpha}", charClassAttr, "Character class"));
        line2.getChildren().add(createStyledTokenSpan("\\1", matchedGroupAttr, "Matched groups"));
        line2.getChildren().add(createStyledTokenSpan("(", parenAttr, "Parenthesis"));
        line2.getChildren().add(createStyledTokenSpan("?#comment", commentAttr, "Comment"));
        line2.getChildren().add(createStyledTokenSpan(")", parenAttr, "Parenthesis"));
        codeLinesBox.getChildren().add(line2);

        // Line 3: .*\Q...\E$# end-of-line comment
        HBox line3 = createLineBox();
        line3.getChildren().add(createStyledTokenSpan(".", dotAttr, "Dot"));
        line3.getChildren().add(createStyledTokenSpan("*", quantAttr, "Quantifier"));
        line3.getChildren().add(createStyledTokenSpan("\\Q", quoteEscAttr, "Quote escape"));
        line3.getChildren().add(createStyledTokenSpan("...", plainAttr, "Plain character"));
        line3.getChildren().add(createStyledTokenSpan("\\E", quoteEscAttr, "Quote escape"));
        line3.getChildren().add(createStyledTokenSpan("$", opAttr, "Operator character"));
        line3.getChildren().add(createStyledTokenSpan("# end-of-line comment", commentAttr, "Comment"));
        codeLinesBox.getChildren().add(line3);
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
