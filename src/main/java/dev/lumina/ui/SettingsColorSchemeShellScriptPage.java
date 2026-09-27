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
 * 1:1 visual & functional replication of Editor > Color Scheme > Shell Script.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemeShellScriptPage extends VBox {

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

    private String selectedKey = "Braces // Parentheses";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemeShellScriptPage() {
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

        Map<String, TreeItem<String>> groupNodes = new HashMap<>();
        TreeItem<String> defaultSelectedItem = null;

        for (AttributesDescriptor desc : EditorColorSchemeSettings.getShellScriptDescriptors()) {
            if (desc.getGroupPath().isEmpty()) {
                TreeItem<String> item = new TreeItem<>(desc.getDisplayName());
                rootItem.getChildren().add(item);
                if ("Parentheses".equals(desc.getDisplayName()) || desc.getKey().equals(selectedKey)) {
                    defaultSelectedItem = item;
                }
            } else {
                String groupName = desc.getGroupPath().get(0);
                TreeItem<String> groupItem = groupNodes.computeIfAbsent(groupName, g -> {
                    TreeItem<String> gi = new TreeItem<>(g);
                    gi.setExpanded(true);
                    rootItem.getChildren().add(gi);
                    return gi;
                });
                TreeItem<String> item = new TreeItem<>(desc.getDisplayName());
                groupItem.getChildren().add(item);
                if ("Parentheses".equals(desc.getDisplayName()) && "Braces".equals(groupName)) {
                    defaultSelectedItem = item;
                }
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
        inheritTargetLabel = new Hyperlink("Parentheses (Language Defaults)");
        inheritTargetLabel.setStyle("-fx-text-fill: #56A8F5; -fx-underline: false; -fx-padding: 0;");
        inheritTargetLabel.setOnAction(e -> {
            if (onNavigateToInherited != null) {
                AttributesDescriptor desc = getPageDescriptor(selectedKey);
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

        // Listeners
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
                String val = newVal.getValue();
                TreeItem<String> parent = newVal.getParent();
                if (parent != null && parent != rootItem && parent.getValue() != null) {
                    selectCategory(parent.getValue() + " // " + val);
                } else {
                    selectCategory(val);
                }
            }
        });

        // Initialize scheme and select default item
        loadScheme(settings.getActiveSchemeName());
        if (defaultSelectedItem != null) {
            categoryTree.getSelectionModel().select(defaultSelectedItem);
            categoryTree.scrollTo(categoryTree.getRow(defaultSelectedItem));
        }
        selectCategory("Braces // Parentheses");
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
            if (child.isLeaf()) {
                if (child.getValue().toLowerCase().contains(lower)) {
                    filteredRoot.getChildren().add(new TreeItem<>(child.getValue()));
                }
            } else {
                TreeItem<String> groupMatch = new TreeItem<>(child.getValue());
                groupMatch.setExpanded(true);
                for (TreeItem<String> subChild : child.getChildren()) {
                    if (subChild.getValue().toLowerCase().contains(lower)) {
                        groupMatch.getChildren().add(new TreeItem<>(subChild.getValue()));
                    }
                }
                if (!groupMatch.getChildren().isEmpty()) {
                    filteredRoot.getChildren().add(groupMatch);
                }
            }
        }
        categoryTree.setRoot(filteredRoot);
    }

    private AttributesDescriptor getPageDescriptor(String key) {
        if (key == null) return null;
        AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor("Shell Script", key);
        if (desc != null) return desc;
        for (AttributesDescriptor d : EditorColorSchemeSettings.getShellScriptDescriptors()) {
            if (d.getKey().equals(key) || d.getDisplayName().equals(key)) {
                return d;
            }
            if (d.getKey().endsWith(" // " + key)) {
                return d;
            }
        }
        return EditorColorSchemeSettings.getDescriptor(key);
    }

    public void selectCategory(String key) {
        this.selectedKey = key;
        AttributesDescriptor desc = getPageDescriptor(key);

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
            ColorAttribute base = attr != null ? attr : (desc != null ? desc.getDefaultAttribute() : null);

            String fg = (inherited != null && inherited.getForeground() != null) ? inherited.getForeground() : (base != null ? base.getForeground() : null);
            String bg = (inherited != null && inherited.getBackground() != null) ? inherited.getBackground() : (base != null ? base.getBackground() : null);
            String es = (inherited != null && inherited.getErrorStripeColor() != null) ? inherited.getErrorStripeColor() : (base != null ? base.getErrorStripeColor() : null);
            String eff = (inherited != null && inherited.getEffectColor() != null) ? inherited.getEffectColor() : (base != null ? base.getEffectColor() : null);
            EffectType effType = (inherited != null && inherited.getEffectType() != null && inherited.getEffectType() != EffectType.NONE) ? inherited.getEffectType() : (base != null ? base.getEffectType() : EffectType.BORDERED);
            boolean isBold = (inherited != null && inherited.isBold()) || (base != null && base.isBold());
            boolean isItalic = (inherited != null && inherited.isItalic()) || (base != null && base.isItalic());

            boldCheck.setSelected(isBold);
            italicCheck.setSelected(isItalic);

            foregroundHex = fg;
            foregroundCheck.setSelected(foregroundHex != null);
            updateSwatch(foregroundSwatch, foregroundHex);

            backgroundHex = bg;
            backgroundCheck.setSelected(backgroundHex != null);
            updateSwatch(backgroundSwatch, backgroundHex);

            errorStripeHex = es;
            errorStripeCheck.setSelected(errorStripeHex != null);
            updateSwatch(errorStripeSwatch, errorStripeHex);

            effectsHex = eff;
            effectsCheck.setSelected(effectsHex != null);
            updateSwatch(effectsSwatch, effectsHex);

            effectTypeCombo.setValue(effType != null ? effType : EffectType.BORDERED);

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
            categoryTree.getSelectionModel().select(item);
            categoryTree.scrollTo(categoryTree.getRow(item));
        }
        selectCategory(key);
    }

    private TreeItem<String> findItem(TreeItem<String> item, String key) {
        if (item == null) return null;
        if (item.getValue().equals(key)) {
            return item;
        }
        if (key.contains(" // ")) {
            String shortName = key.substring(key.lastIndexOf(" // ") + 4);
            if (item.getValue().equals(shortName)) {
                return item;
            }
        }
        for (TreeItem<String> child : item.getChildren()) {
            TreeItem<String> res = findItem(child, key);
            if (res != null) return res;
        }
        return null;
    }

    private ColorAttribute getCurrentAttribute(String key) {
        String scheme = settings.getActiveSchemeName();
        ColorAttribute attr = settings.getAttribute(scheme, "Shell Script // " + key);
        if (attr == null) {
            attr = settings.getAttribute(scheme, key);
        }
        AttributesDescriptor desc = getPageDescriptor(key);
        if (attr == null && desc != null) {
            if (!desc.getKey().equals(key)) {
                attr = settings.getAttribute(scheme, "Shell Script // " + desc.getKey());
                if (attr == null) {
                    attr = settings.getAttribute(scheme, desc.getKey());
                }
            }
        }
        if (attr == null && desc != null) {
            return desc.getDefaultAttribute();
        }
        if (attr != null && desc != null && desc.getDefaultAttribute() != null) {
            ColorAttribute def = desc.getDefaultAttribute();
            if (attr.isInherit()) {
                if (attr.getBackground() == null && def.getBackground() != null) {
                    ColorAttribute merged = attr.clone();
                    merged.setBackground(def.getBackground());
                    return merged;
                }
            }
        }
        return attr;
    }

    private ColorAttribute resolveInheritedAttribute(AttributesDescriptor desc) {
        if (desc == null || desc.getInheritTarget() == null) return null;
        String target = desc.getInheritTarget();
        ColorAttribute attr = null;
        if (!target.equals(desc.getKey())) {
            attr = settings.getAttribute(settings.getActiveSchemeName(), target);
            if (attr == null) {
                String alt1 = target.replace("->", " // ");
                attr = settings.getAttribute(settings.getActiveSchemeName(), alt1);
                if (attr == null && target.contains("->")) {
                    String alt2 = target.substring(target.lastIndexOf("->") + 2);
                    attr = settings.getAttribute(settings.getActiveSchemeName(), alt2);
                }
            }
        }
        if (attr == null) {
            String scope = desc.getInheritScope();
            if (scope != null && scope.contains("Language Defaults")) {
                for (AttributesDescriptor ldDesc : EditorColorSchemeSettings.getLanguageDefaultsDescriptors()) {
                    if (ldDesc.getKey().equals(target) || ldDesc.getDisplayName().equals(target) ||
                            (target.contains("->") && ldDesc.getKey().endsWith(target.substring(target.lastIndexOf("->") + 2)))) {
                        attr = ldDesc.getDefaultAttribute();
                        break;
                    }
                }
            }
        }
        if (attr == null) {
            AttributesDescriptor targetDesc = EditorColorSchemeSettings.getDescriptor(target);
            if (targetDesc == null) {
                targetDesc = EditorColorSchemeSettings.getDescriptor(target.replace("->", " // "));
                if (targetDesc == null && target.contains("->")) {
                    targetDesc = EditorColorSchemeSettings.getDescriptor(target.substring(target.lastIndexOf("->") + 2));
                }
            }
            if (targetDesc != null && targetDesc != desc) {
                attr = targetDesc.getDefaultAttribute();
            }
        }
        return attr != null ? attr : (desc != null ? desc.getDefaultAttribute() : null);
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

        AttributesDescriptor desc = getPageDescriptor(selectedKey);
        if (desc != null) {
            attr.setInheritFrom(desc.getInheritTarget());
            attr.setInheritScope(desc.getInheritScope());
            settings.setAttribute(settings.getActiveSchemeName(), "Shell Script // " + desc.getKey(), attr);
            settings.setAttribute(settings.getActiveSchemeName(), desc.getKey(), attr);
        } else {
            settings.setAttribute(settings.getActiveSchemeName(), "Shell Script // " + selectedKey, attr);
            settings.setAttribute(settings.getActiveSchemeName(), selectedKey, attr);
        }

        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void loadScheme(String schemeName) {
        originalAttributes.clear();
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getShellScriptDescriptors()) {
            ColorAttribute current = getCurrentAttribute(desc.getKey());
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
            settings.setAttribute(settings.getActiveSchemeName(), "Shell Script // " + entry.getKey(), entry.getValue().clone());
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

    public VBox getCodeLinesBox() {
        return codeLinesBox;
    }

    // ---------------- Interactive Code Preview ----------------
    public void updatePreview() {
        codeLinesBox.getChildren().clear();

        ColorAttribute shebangAttr = getEffectiveAttribute("Shebang comment");
        ColorAttribute commentAttr = getEffectiveAttribute("Line comment");
        ColorAttribute varAttr = getEffectiveAttribute("Variables // Variable use");
        ColorAttribute varDeclAttr = getEffectiveAttribute("Variables // Variable declaration");
        ColorAttribute composedVarAttr = getEffectiveAttribute("Variables // Composed variable");
        ColorAttribute subshellAttr = getEffectiveAttribute("Variables // Subshell command substitution");
        ColorAttribute parenAttr = getEffectiveAttribute("Braces // Parentheses");
        ColorAttribute curlyAttr = getEffectiveAttribute("Braces // curly brackets");
        ColorAttribute squareAttr = getEffectiveAttribute("Braces // square brackets");
        ColorAttribute builtinAttr = getEffectiveAttribute("Commands // Builtin command");
        ColorAttribute extCmdAttr = getEffectiveAttribute("Commands // External command");
        ColorAttribute condAttr = getEffectiveAttribute("Conditional operators");
        ColorAttribute fnDeclAttr = getEffectiveAttribute("Function declaration");
        ColorAttribute kwAttr = getEffectiveAttribute("Keyword");
        ColorAttribute numAttr = getEffectiveAttribute("Number");
        ColorAttribute strAttr = getEffectiveAttribute("String");
        ColorAttribute redirAttr = getEffectiveAttribute("Redirection");

        // Line 1: #!/bin/bash
        HBox line1 = createLineBox();
        line1.getChildren().add(createStyledTokenSpan("#!/bin/bash", shebangAttr, "Shebang comment"));
        codeLinesBox.getChildren().add(line1);

        // Line 2: # Description:
        HBox line2 = createLineBox();
        line2.getChildren().add(createStyledTokenSpan("# Description:", commentAttr, "Line comment"));
        codeLinesBox.getChildren().add(line2);

        // Line 3: #   Runs bashdb, the bash debugger.
        HBox line3 = createLineBox();
        line3.getChildren().add(createStyledTokenSpan("#   Runs bashdb, the bash debugger.", commentAttr, "Line comment"));
        codeLinesBox.getChildren().add(line3);

        // Line 4: #
        HBox line4 = createLineBox();
        line4.getChildren().add(createStyledTokenSpan("#", commentAttr, "Line comment"));
        codeLinesBox.getChildren().add(line4);

        // Line 5: blank
        codeLinesBox.getChildren().add(createPlainSpan(""));

        // Line 6: dirname="${BASH_SOURCE[0]%/*}"
        HBox line6 = createLineBox();
        line6.getChildren().add(createStyledTokenSpan("dirname", varDeclAttr, "Variables // Variable declaration"));
        line6.getChildren().add(createPlainSpan("="));
        line6.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        line6.getChildren().add(createStyledTokenSpan("${", curlyAttr, "Braces // curly brackets"));
        line6.getChildren().add(createStyledTokenSpan("BASH_SOURCE", composedVarAttr, "Variables // Composed variable"));
        line6.getChildren().add(createStyledTokenSpan("[", squareAttr, "Braces // square brackets"));
        line6.getChildren().add(createStyledTokenSpan("0", numAttr, "Number"));
        line6.getChildren().add(createStyledTokenSpan("]", squareAttr, "Braces // square brackets"));
        line6.getChildren().add(createStyledTokenSpan("%/*", condAttr, "Conditional operators"));
        line6.getChildren().add(createStyledTokenSpan("}", curlyAttr, "Braces // curly brackets"));
        line6.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        codeLinesBox.getChildren().add(line6);

        // Line 7: if [[ ! -d "$dirname" ]]; then
        HBox line7 = createLineBox();
        line7.getChildren().add(createStyledTokenSpan("if", kwAttr, "Keyword"));
        line7.getChildren().add(createPlainSpan(" "));
        line7.getChildren().add(createStyledTokenSpan("[[", squareAttr, "Braces // square brackets"));
        line7.getChildren().add(createPlainSpan(" "));
        line7.getChildren().add(createStyledTokenSpan("!", condAttr, "Conditional operators"));
        line7.getChildren().add(createPlainSpan(" "));
        line7.getChildren().add(createStyledTokenSpan("-d", condAttr, "Conditional operators"));
        line7.getChildren().add(createPlainSpan(" "));
        line7.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        line7.getChildren().add(createStyledTokenSpan("$dirname", varAttr, "Variables // Variable use"));
        line7.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        line7.getChildren().add(createPlainSpan(" "));
        line7.getChildren().add(createStyledTokenSpan("]]", squareAttr, "Braces // square brackets"));
        line7.getChildren().add(createPlainSpan("; "));
        line7.getChildren().add(createStyledTokenSpan("then", kwAttr, "Keyword"));
        codeLinesBox.getChildren().add(line7);

        // Line 8:   dirname="$PWD"
        HBox line8 = createLineBox();
        line8.getChildren().add(createPlainSpan("  "));
        line8.getChildren().add(createStyledTokenSpan("dirname", varDeclAttr, "Variables // Variable declaration"));
        line8.getChildren().add(createPlainSpan("="));
        line8.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        line8.getChildren().add(createStyledTokenSpan("$PWD", composedVarAttr, "Variables // Composed variable"));
        line8.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        codeLinesBox.getChildren().add(line8);

        // Line 9: fi
        HBox line9 = createLineBox();
        line9.getChildren().add(createStyledTokenSpan("fi", kwAttr, "Keyword"));
        codeLinesBox.getChildren().add(line9);

        // Line 10: blank
        codeLinesBox.getChildren().add(createPlainSpan(""));

        // Line 11: if [[ -f "$dirname/bashdb-main.inc" ]]; then
        HBox line11 = createLineBox();
        line11.getChildren().add(createStyledTokenSpan("if", kwAttr, "Keyword"));
        line11.getChildren().add(createPlainSpan(" "));
        line11.getChildren().add(createStyledTokenSpan("[[", squareAttr, "Braces // square brackets"));
        line11.getChildren().add(createPlainSpan(" "));
        line11.getChildren().add(createStyledTokenSpan("-f", condAttr, "Conditional operators"));
        line11.getChildren().add(createPlainSpan(" "));
        line11.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        line11.getChildren().add(createStyledTokenSpan("$dirname", varAttr, "Variables // Variable use"));
        line11.getChildren().add(createStyledTokenSpan("/bashdb-main.inc", strAttr, "String"));
        line11.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        line11.getChildren().add(createPlainSpan(" "));
        line11.getChildren().add(createStyledTokenSpan("]]", squareAttr, "Braces // square brackets"));
        line11.getChildren().add(createPlainSpan("; "));
        line11.getChildren().add(createStyledTokenSpan("then", kwAttr, "Keyword"));
        codeLinesBox.getChildren().add(line11);

        // Line 12:   _dirname="$dirname"
        HBox line12 = createLineBox();
        line12.getChildren().add(createPlainSpan("  "));
        line12.getChildren().add(createStyledTokenSpan("_dirname", varDeclAttr, "Variables // Variable declaration"));
        line12.getChildren().add(createPlainSpan("="));
        line12.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        line12.getChildren().add(createStyledTokenSpan("$dirname", varAttr, "Variables // Variable use"));
        line12.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        codeLinesBox.getChildren().add(line12);

        // Line 13: elif [[ -f "$dirname/../bashdb-main.inc" ]]; then
        HBox line13 = createLineBox();
        line13.getChildren().add(createStyledTokenSpan("elif", kwAttr, "Keyword"));
        line13.getChildren().add(createPlainSpan(" "));
        line13.getChildren().add(createStyledTokenSpan("[[", squareAttr, "Braces // square brackets"));
        line13.getChildren().add(createPlainSpan(" "));
        line13.getChildren().add(createStyledTokenSpan("-f", condAttr, "Conditional operators"));
        line13.getChildren().add(createPlainSpan(" "));
        line13.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        line13.getChildren().add(createStyledTokenSpan("$dirname", varAttr, "Variables // Variable use"));
        line13.getChildren().add(createStyledTokenSpan("/../bashdb-main.inc", strAttr, "String"));
        line13.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        line13.getChildren().add(createPlainSpan(" "));
        line13.getChildren().add(createStyledTokenSpan("]]", squareAttr, "Braces // square brackets"));
        line13.getChildren().add(createPlainSpan("; "));
        line13.getChildren().add(createStyledTokenSpan("then", kwAttr, "Keyword"));
        codeLinesBox.getChildren().add(line13);

        // Line 14:   _dirname="$dirname/.."
        HBox line14 = createLineBox();
        line14.getChildren().add(createPlainSpan("  "));
        line14.getChildren().add(createStyledTokenSpan("_dirname", varDeclAttr, "Variables // Variable declaration"));
        line14.getChildren().add(createPlainSpan("="));
        line14.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        line14.getChildren().add(createStyledTokenSpan("$dirname", varAttr, "Variables // Variable use"));
        line14.getChildren().add(createStyledTokenSpan("/..", strAttr, "String"));
        line14.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        codeLinesBox.getChildren().add(line14);

        // Line 15: fi
        HBox line15 = createLineBox();
        line15.getChildren().add(createStyledTokenSpan("fi", kwAttr, "Keyword"));
        codeLinesBox.getChildren().add(line15);

        // Line 16: blank
        codeLinesBox.getChildren().add(createPlainSpan(""));

        // Line 17: source "$_dirname/bashdb-trace.inc"
        HBox line17 = createLineBox();
        line17.getChildren().add(createStyledTokenSpan("source", builtinAttr, "Commands // Builtin command"));
        line17.getChildren().add(createPlainSpan(" "));
        line17.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        line17.getChildren().add(createStyledTokenSpan("$_dirname", varAttr, "Variables // Variable use"));
        line17.getChildren().add(createStyledTokenSpan("/bashdb-trace.inc", strAttr, "String"));
        line17.getChildren().add(createStyledTokenSpan("\"", strAttr, "String"));
        line17.getChildren().add(createPlainSpan(" "));
        line17.getChildren().add(createStyledTokenSpan("2>&1", redirAttr, "Redirection"));
        codeLinesBox.getChildren().add(line17);

        // Line 18: _bashdb_setup
        HBox line18 = createLineBox();
        line18.getChildren().add(createStyledTokenSpan("_bashdb_setup", fnDeclAttr, "Function declaration"));
        line18.getChildren().add(createStyledTokenSpan("(", parenAttr, "Braces // Parentheses"));
        line18.getChildren().add(createStyledTokenSpan(")", parenAttr, "Braces // Parentheses"));
        codeLinesBox.getChildren().add(line18);
    }

    private ColorAttribute getEffectiveAttribute(String key) {
        ColorAttribute attr = getCurrentAttribute(key);
        AttributesDescriptor desc = getPageDescriptor(key);
        if (attr == null && desc != null) {
            attr = desc.getDefaultAttribute();
        }
        if (attr != null && attr.isInherit() && desc != null && desc.getInheritTarget() != null) {
            ColorAttribute inh = resolveInheritedAttribute(desc);
            if (inh != null) {
                ColorAttribute merged = attr.clone();
                if (merged.getForeground() == null) merged.setForeground(inh.getForeground());
                if (merged.getBackground() == null) merged.setBackground(inh.getBackground());
                return merged;
            }
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
