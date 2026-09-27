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
 * 1:1 visual & functional replication of Editor > Color Scheme > Table Diff.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemeTableDiffPage extends VBox {

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
    private final VBox tableContainer;
    private final VBox errorStripeBar;

    private String selectedKey = "Fuzzy match - mismatched";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemeTableDiffPage() {
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

        TreeItem<String> defaultSelectedItem = null;
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getTableDiffDescriptors()) {
            TreeItem<String> item = new TreeItem<>(desc.getKey());
            rootItem.getChildren().add(item);
            if ("Fuzzy match - mismatched".equals(desc.getKey())) {
                defaultSelectedItem = item;
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
        inheritTargetLabel = new Hyperlink("Text search result (General)");
        inheritTargetLabel.setStyle("-fx-text-fill: #56A8F5; -fx-underline: false; -fx-padding: 0;");
        inheritTargetLabel.setOnAction(e -> {
            if (onNavigateToInherited != null) {
                AttributesDescriptor desc = getPageDescriptor(selectedKey);
                if (desc != null && desc.getInheritTarget() != null) {
                    onNavigateToInherited.accept(desc.getInheritTarget());
                }
            }
        });
        inheritScopeLabel = new Label("(General)");
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
        tableContainer = new VBox(0);
        tableContainer.setStyle("-fx-background-color: #1E1F22;");

        previewScrollPane = new ScrollPane(tableContainer);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1 0 1 1; -fx-border-radius: 4 0 0 4; -fx-background-radius: 4 0 0 4;");
        HBox.setHgrow(previewScrollPane, Priority.ALWAYS);

        errorStripeBar = new VBox(0);
        errorStripeBar.setPrefWidth(16);
        errorStripeBar.setMinWidth(16);
        errorStripeBar.setMaxWidth(16);
        errorStripeBar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 1 0; -fx-border-radius: 0 4 4 0; -fx-background-radius: 0 4 4 0;");

        HBox previewBoxWithGutter = new HBox(0, previewScrollPane, errorStripeBar);
        previewBoxWithGutter.setPrefHeight(240);
        VBox.setVgrow(previewBoxWithGutter, Priority.ALWAYS);

        getChildren().addAll(headerBar, topSplitPane, previewBoxWithGutter);

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
                selectCategory(newVal.getValue());
            }
        });

        // Initialize scheme and select default item
        loadScheme(settings.getActiveSchemeName());
        if (defaultSelectedItem != null) {
            categoryTree.getSelectionModel().select(defaultSelectedItem);
            categoryTree.scrollTo(categoryTree.getRow(defaultSelectedItem));
        }
        selectCategory("Fuzzy match - mismatched");
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

    private AttributesDescriptor getPageDescriptor(String key) {
        if (key == null) return null;
        AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor("Table Diff", key);
        if (desc != null) return desc;
        for (AttributesDescriptor d : EditorColorSchemeSettings.getTableDiffDescriptors()) {
            if (d.getKey().equals(key) || d.getDisplayName().equals(key)) {
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
                inheritScopeLabel.setText(desc.getInheritScope() != null ? desc.getInheritScope() : "(General)");
            }

            boolean isInheriting = canInherit && inheritCheck.isSelected();
            ColorAttribute inherited = isInheriting ? resolveInheritedAttribute(desc) : null;
            ColorAttribute base = attr != null ? attr : (desc != null ? desc.getDefaultAttribute() : null);

            String fg = (base != null && base.getForeground() != null) ? base.getForeground() : (inherited != null ? inherited.getForeground() : null);
            String bg = (base != null && base.getBackground() != null) ? base.getBackground() : (inherited != null ? inherited.getBackground() : null);
            String es = (base != null && base.getErrorStripeColor() != null) ? base.getErrorStripeColor() : (inherited != null ? inherited.getErrorStripeColor() : null);
            String eff = (base != null && base.getEffectColor() != null) ? base.getEffectColor() : (inherited != null ? inherited.getEffectColor() : null);
            EffectType effType = (base != null && base.getEffectType() != null && base.getEffectType() != EffectType.NONE) ? base.getEffectType() : (inherited != null && inherited.getEffectType() != null ? inherited.getEffectType() : EffectType.BORDERED);
            boolean isBold = (base != null && base.isBold()) || (inherited != null && inherited.isBold());
            boolean isItalic = (base != null && base.isItalic()) || (inherited != null && inherited.isItalic());

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
        for (TreeItem<String> child : item.getChildren()) {
            TreeItem<String> res = findItem(child, key);
            if (res != null) return res;
        }
        return null;
    }

    private ColorAttribute getCurrentAttribute(String key) {
        String scheme = settings.getActiveSchemeName();
        ColorAttribute attr = settings.getAttribute(scheme, "Table Diff // " + key);
        if (attr == null) {
            attr = settings.getAttribute(scheme, key);
        }
        AttributesDescriptor desc = getPageDescriptor(key);
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
            if (scope != null && scope.contains("General")) {
                for (AttributesDescriptor genDesc : EditorColorSchemeSettings.getGeneralDescriptors()) {
                    if (genDesc.getKey().equals(target) || genDesc.getDisplayName().equals(target) ||
                            (target.contains("->") && genDesc.getKey().endsWith(target.substring(target.lastIndexOf("->") + 2)))) {
                        attr = genDesc.getDefaultAttribute();
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
            settings.setAttribute(settings.getActiveSchemeName(), "Table Diff // " + desc.getKey(), attr);
            settings.setAttribute(settings.getActiveSchemeName(), desc.getKey(), attr);
        } else {
            settings.setAttribute(settings.getActiveSchemeName(), "Table Diff // " + selectedKey, attr);
            settings.setAttribute(settings.getActiveSchemeName(), selectedKey, attr);
        }

        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void loadScheme(String schemeName) {
        originalAttributes.clear();
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getTableDiffDescriptors()) {
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
            settings.setAttribute(settings.getActiveSchemeName(), "Table Diff // " + entry.getKey(), entry.getValue().clone());
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

    public VBox getTableContainer() {
        return tableContainer;
    }

    public VBox getErrorStripeBar() {
        return errorStripeBar;
    }

    // ---------------- Interactive Code Preview ----------------
    public void updatePreview() {
        tableContainer.getChildren().clear();
        errorStripeBar.getChildren().clear();

        ColorAttribute excludedAttr = getEffectiveAttribute("Excluded from diff");
        ColorAttribute matchedAttr = getEffectiveAttribute("Fuzzy match - matched");
        ColorAttribute mismatchedAttr = getEffectiveAttribute("Fuzzy match - mismatched");

        // Table Header
        HBox headerRow = createTableRow(true, null, null);
        headerRow.getChildren().addAll(
                createTableCell("col1", 60, true, null, null),
                createTableCell("col2", 140, true, null, null),
                createTableCell("col3", 140, true, null, null),
                createTableCell("col4", 140, true, null, null)
        );
        tableContainer.getChildren().add(headerRow);

        // Row 1: normal row
        HBox row1 = createTableRow(false, null, null);
        row1.getChildren().addAll(
                createTableCell("1", 60, false, null, null),
                createTableCell("some", 140, false, null, null),
                createTableCell("table", 140, false, null, null),
                createTableCell("data", 140, false, null, null)
        );
        tableContainer.getChildren().add(row1);

        // Row 2: fuzzy match / mismatch row
        HBox row2 = createTableRow(false, null, null);
        row2.getChildren().addAll(
                createTableCell("2", 60, false, null, null),
                createTableCell("fuzzy", 140, false, matchedAttr, "Fuzzy match - matched"),
                createTableCell("mismatche", 140, false, mismatchedAttr, "Fuzzy match - mismatched"),
                createTableCell("data", 140, false, null, null)
        );
        tableContainer.getChildren().add(row2);

        // Row 3: normal row
        HBox row3 = createTableRow(false, null, null);
        row3.getChildren().addAll(
                createTableCell("3", 60, false, null, null),
                createTableCell("normal", 140, false, null, null),
                createTableCell("row", 140, false, null, null),
                createTableCell("here", 140, false, null, null)
        );
        tableContainer.getChildren().add(row3);

        // Row 4: excluded from diff row
        HBox row4 = createTableRow(false, excludedAttr, "Excluded from diff");
        row4.getChildren().addAll(
                createTableCell("4", 60, false, excludedAttr, "Excluded from diff"),
                createTableCell("excluded", 140, false, excludedAttr, "Excluded from diff"),
                createTableCell("from", 140, false, excludedAttr, "Excluded from diff"),
                createTableCell("diff", 140, false, excludedAttr, "Excluded from diff")
        );
        tableContainer.getChildren().add(row4);

        // Gutter error stripe markers
        Region topSpacer = new Region();
        topSpacer.setPrefHeight(28); // header + row 1
        Region mismatchMarker = new Region();
        mismatchMarker.setPrefHeight(24);
        mismatchMarker.setPrefWidth(12);
        String esColor = (mismatchedAttr != null && mismatchedAttr.getErrorStripeColor() != null)
                ? mismatchedAttr.getErrorStripeColor() : "#72D6D6";
        mismatchMarker.setStyle("-fx-background-color: " + esColor + "; -fx-cursor: hand; -fx-background-radius: 2;");
        mismatchMarker.setOnMouseClicked(e -> selectTreeItem("Fuzzy match - mismatched"));

        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        errorStripeBar.getChildren().addAll(topSpacer, mismatchMarker, bottomSpacer);
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
                if (merged.getErrorStripeColor() == null) merged.setErrorStripeColor(inh.getErrorStripeColor());
                if (merged.getEffectColor() == null) merged.setEffectColor(inh.getEffectColor());
                return merged;
            }
        }
        return attr;
    }

    private HBox createTableRow(boolean isHeader, ColorAttribute rowAttr, String selectKey) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(24);
        if (isHeader) {
            row.setStyle("-fx-background-color: #26282E; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");
        } else if (rowAttr != null && rowAttr.getBackground() != null) {
            row.setStyle("-fx-background-color: " + rowAttr.getBackground() + "; -fx-border-color: #2F3136; -fx-border-width: 0 0 1 0;");
        } else {
            row.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2F3136; -fx-border-width: 0 0 1 0;");
        }
        if (selectKey != null) {
            row.setOnMouseClicked(e -> selectTreeItem(selectKey));
            row.setOnMouseEntered(e -> row.setCursor(javafx.scene.Cursor.HAND));
            row.setOnMouseExited(e -> row.setCursor(javafx.scene.Cursor.DEFAULT));
        }
        return row;
    }

    private Label createTableCell(String text, double width, boolean isHeader, ColorAttribute attr, String selectKey) {
        Label cell = new Label(text);
        cell.setPrefWidth(width);
        cell.setMinWidth(width);
        cell.setPadding(new Insets(2, 8, 2, 8));

        StringBuilder sb = new StringBuilder("-fx-font-family: 'Fira Code', 'DejaVu Sans Mono', monospace; -fx-font-size: 12px;");
        if (isHeader) {
            sb.append("-fx-text-fill: #848BA3; -fx-font-weight: bold; -fx-border-color: #393B40; -fx-border-width: 0 1 0 0;");
        } else {
            String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#DFE1E5";
            sb.append("-fx-text-fill: ").append(fg).append(";");
            if (attr != null) {
                if (attr.getBackground() != null) {
                    sb.append("-fx-background-color: ").append(attr.getBackground()).append(";");
                }
                if (attr.isBold()) sb.append("-fx-font-weight: bold;");
                if (attr.isItalic()) sb.append("-fx-font-style: italic;");
                if (attr.getEffectType() == EffectType.BORDERED && attr.getEffectColor() != null) {
                    sb.append("-fx-border-color: ").append(attr.getEffectColor()).append("; -fx-border-width: 1;");
                } else {
                    sb.append("-fx-border-color: #2F3136; -fx-border-width: 0 1 0 0;");
                }
            } else {
                sb.append("-fx-border-color: #2F3136; -fx-border-width: 0 1 0 0;");
            }
        }
        cell.setStyle(sb.toString());

        if (selectKey != null) {
            cell.setOnMouseClicked(e -> selectTreeItem(selectKey));
            cell.setOnMouseEntered(e -> cell.setCursor(javafx.scene.Cursor.HAND));
            cell.setOnMouseExited(e -> cell.setCursor(javafx.scene.Cursor.DEFAULT));
        }
        return cell;
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
