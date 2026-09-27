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
 * 1:1 visual & functional replication of Editor > Color Scheme > PostCSS.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemePostCSSPage extends VBox {

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

    private String selectedKey = "Colon";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemePostCSSPage() {
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

        String[] items = {
                "Ampersand", "At-keyword", "Attribute name", "Bad character", "Braces",
                "Brackets", "Class name", "Colon", "Comma", "Comment", "Dot", "Function",
                "Hex color", "Id selector", "Identifier", "Important", "Number",
                "Operators", "Parenthesis", "Property name", "Property value",
                "Pseudo selector", "Semicolon", "String", "Tag name", "Unicode range",
                "Unit", "URL"
        };

        TreeItem<String> defaultSelectItem = null;
        for (String item : items) {
            TreeItem<String> leaf = new TreeItem<>(item);
            rootItem.getChildren().add(leaf);
            if ("Colon".equals(item)) {
                defaultSelectItem = leaf;
            }
        }

        categoryTree = new TreeView<>(rootItem);
        categoryTree.setShowRoot(false);
        categoryTree.getStyleClass().add("settings-tree-view");
        categoryTree.setPrefWidth(300);
        categoryTree.setPrefHeight(270);
        categoryTree.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #BCBEC4; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");

        // Filter search field above tree
        TextField searchField = new TextField();
        searchField.setPromptText("Search");
        searchField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #BCBEC4; -fx-prompt-text-fill: #5F6166; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTree(newVal));

        VBox treeBox = new VBox(6, searchField, categoryTree);
        VBox.setVgrow(categoryTree, Priority.ALWAYS);
        treeBox.setPrefWidth(300);
        treeBox.setPrefHeight(270);

        // ---------------- Right Attribute Panel ----------------
        boldCheck = new CheckBox("Bold");
        boldCheck.setStyle("-fx-text-fill: #BCBEC4;");
        italicCheck = new CheckBox("Italic");
        italicCheck.setStyle("-fx-text-fill: #BCBEC4;");

        HBox fontStylesBox = new HBox(16, boldCheck, italicCheck);
        fontStylesBox.setAlignment(Pos.CENTER_LEFT);

        // Foreground
        foregroundCheck = new CheckBox("Foreground");
        foregroundCheck.setStyle("-fx-text-fill: #BCBEC4;");
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
        inheritTargetLabel = new Hyperlink("Colon");
        inheritTargetLabel.setStyle("-fx-text-fill: #56A8F5; -fx-underline: false; -fx-padding: 0;");
        inheritTargetLabel.setOnAction(e -> {
            if (onNavigateToInherited != null) {
                AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
                if (desc != null && desc.getInheritTarget() != null) {
                    onNavigateToInherited.accept(desc.getInheritTarget());
                }
            }
        });
        inheritScopeLabel = new Label("(CSS)");
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
                selectCategory(newVal.getValue());
            }
        });

        // Initialize scheme and select default item
        loadScheme(settings.getActiveSchemeName());
        if (defaultSelectItem != null) {
            categoryTree.getSelectionModel().select(defaultSelectItem);
        }
        selectCategory("Colon");
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

    private void showColorPickerPopup(Button anchor, String initialHex, Consumer<String> onColorSelected) {
        Stage popup = new Stage(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);

        ColorPicker picker = new ColorPicker();
        if (initialHex != null && !initialHex.isBlank()) {
            try {
                picker.setValue(Color.web(initialHex.startsWith("#") ? initialHex : "#" + initialHex));
            } catch (Exception ignored) {}
        }

        Button okBtn = new Button("Choose");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-border-radius: 4; -fx-background-radius: 4;");
        okBtn.setOnAction(e -> {
            Color c = picker.getValue();
            String hex = String.format("%02X%02X%02X", (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
            onColorSelected.accept(hex);
            popup.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #BCBEC4; -fx-border-radius: 4; -fx-background-radius: 4;");
        cancelBtn.setOnAction(e -> popup.close());

        HBox btnBox = new HBox(8, okBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(10, picker, btnBox);
        content.setPadding(new Insets(12));
        content.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        popup.setScene(new Scene(content));
        javafx.geometry.Point2D p = anchor.localToScreen(0, anchor.getHeight());
        if (p != null) {
            popup.setX(p.getX());
            popup.setY(p.getY() + 4);
        }
        popup.show();
    }

    private void markModified() {
        ColorAttribute current = getCurrentEditorAttribute();
        ColorAttribute orig = originalAttributes.get(selectedKey);
        boolean dirty = !Objects.equals(current, orig);
        if (dirty != isModified) {
            isModified = dirty;
            if (onModifiedListener != null) onModifiedListener.run();
        }
    }

    private void loadScheme(String scheme) {
        originalAttributes.clear();
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getPostCSSDescriptors()) {
            ColorAttribute attr = settings.getAttribute(scheme, desc.getKey());
            originalAttributes.put(desc.getKey(), attr != null ? attr.clone() : desc.getDefaultAttribute().clone());
        }
        isModified = false;
        if (onModifiedListener != null) onModifiedListener.run();
        selectCategory(selectedKey);
    }

    public void selectCategory(String key) {
        selectedKey = key;
        suppressEvents = true;
        try {
            ColorAttribute attr = settings.getAttribute(settings.getActiveSchemeName(), key);
            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(key);

            if (attr == null && desc != null) {
                attr = desc.getDefaultAttribute();
            }

            if (attr != null) {
                boldCheck.setSelected(attr.isBold());
                italicCheck.setSelected(attr.isItalic());

                boolean hasFg = attr.getForeground() != null;
                foregroundCheck.setSelected(hasFg);
                foregroundHex = hasFg ? attr.getForeground() : (desc != null && desc.getDefaultForeground() != null ? desc.getDefaultForeground() : null);
                updateSwatch(foregroundSwatch, foregroundHex);

                boolean hasBg = attr.getBackground() != null;
                backgroundCheck.setSelected(hasBg);
                backgroundHex = hasBg ? attr.getBackground() : null;
                updateSwatch(backgroundSwatch, backgroundHex);

                boolean hasEs = attr.getErrorStripeColor() != null;
                errorStripeCheck.setSelected(hasEs);
                errorStripeHex = hasEs ? attr.getErrorStripeColor() : null;
                updateSwatch(errorStripeSwatch, errorStripeHex);

                boolean hasEff = attr.getEffectColor() != null && attr.getEffectType() != null && attr.getEffectType() != EffectType.NONE;
                effectsCheck.setSelected(hasEff);
                effectsHex = hasEff ? attr.getEffectColor() : null;
                updateSwatch(effectsSwatch, effectsHex);
                if (attr.getEffectType() != null && attr.getEffectType() != EffectType.NONE) {
                    effectTypeCombo.setValue(attr.getEffectType());
                } else {
                    effectTypeCombo.setValue(EffectType.BORDERED);
                }

                if (desc != null && desc.getInheritTarget() != null) {
                    inheritBox.setVisible(true);
                    inheritBox.setManaged(true);
                    inheritCheck.setSelected(attr.isInherit());
                    inheritTargetLabel.setText(desc.getInheritTarget());
                    inheritScopeLabel.setText(desc.getInheritScope() != null ? desc.getInheritScope() : "");
                } else {
                    inheritBox.setVisible(false);
                    inheritBox.setManaged(false);
                }
            }
        } finally {
            suppressEvents = false;
        }
    }

    private ColorAttribute getCurrentEditorAttribute() {
        ColorAttribute attr = new ColorAttribute();
        attr.setBold(boldCheck.isSelected());
        attr.setItalic(italicCheck.isSelected());
        if (foregroundCheck.isSelected() && foregroundHex != null) {
            attr.setForeground(foregroundHex.startsWith("#") ? foregroundHex : "#" + foregroundHex);
        }
        if (backgroundCheck.isSelected() && backgroundHex != null) {
            attr.setBackground(backgroundHex.startsWith("#") ? backgroundHex : "#" + backgroundHex);
        }
        if (errorStripeCheck.isSelected() && errorStripeHex != null) {
            attr.setErrorStripeColor(errorStripeHex.startsWith("#") ? errorStripeHex : "#" + errorStripeHex);
        }
        if (effectsCheck.isSelected() && effectsHex != null) {
            attr.setEffectColor(effectsHex.startsWith("#") ? effectsHex : "#" + effectsHex);
            attr.setEffectType(effectTypeCombo.getValue());
        } else {
            attr.setEffectType(EffectType.NONE);
        }
        attr.setInherit(inheritCheck.isSelected());
        AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
        if (desc != null) {
            attr.setInheritFrom(desc.getInheritTarget());
            attr.setInheritScope(desc.getInheritScope());
        }
        return attr;
    }

    // -------------------------------------------------------------------------
    // Interactive Preview Pane
    // -------------------------------------------------------------------------

    private static class Token {
        final String text;
        final String categoryKey;
        final String fallbackColor;

        Token(String text, String categoryKey, String fallbackColor) {
            this.text = text;
            this.categoryKey = categoryKey;
            this.fallbackColor = fallbackColor;
        }
    }

    private void updatePreview() {
        codeLinesBox.getChildren().clear();

        List<List<Token>> lines = List.of(
                List.of(
                        new Token("@import", "At-keyword", "#CF8E6D"),
                        new Token(" ", null, null),
                        new Token("\"manual.css\"", "String", "#6AAB73"),
                        new Token(";", "Semicolon", "#BCBEC4")
                ),
                List.of(
                        new Token("", null, null)
                ),
                List.of(
                        new Token("@font-face", "At-keyword", "#CF8E6D"),
                        new Token(" {", "Braces", "#BCBEC4")
                ),
                List.of(
                        new Token("  ", null, null),
                        new Token("font-family", "Property name", "#56A8F5"),
                        new Token(": ", "Colon", "#BCBEC4"),
                        new Token("DroidSans", "Identifier", "#BCBEC4"),
                        new Token(";", "Semicolon", "#BCBEC4")
                ),
                List.of(
                        new Token("  ", null, null),
                        new Token("src", "Property name", "#56A8F5"),
                        new Token(": ", "Colon", "#BCBEC4"),
                        new Token("url", "Function", "#56A8F5"),
                        new Token("(", "Parenthesis", "#BCBEC4"),
                        new Token("DroidSans.ttf", "URL", "#6AAB73"),
                        new Token(");", "Parenthesis", "#BCBEC4")
                ),
                List.of(
                        new Token("  ", null, null),
                        new Token("unicode-range", "Property name", "#56A8F5"),
                        new Token(": ", "Colon", "#BCBEC4"),
                        new Token("U+000-5FF", "Unicode range", "#2AAC88"),
                        new Token(", ", "Comma", "#BCBEC4"),
                        new Token("U+1e00-1fff", "Unicode range", "#2AAC88"),
                        new Token(", ", "Comma", "#BCBEC4"),
                        new Token("U+2000-2300", "Unicode range", "#2AAC88"),
                        new Token(";", "Semicolon", "#BCBEC4")
                ),
                List.of(
                        new Token("}", "Braces", "#BCBEC4")
                )
        );

        for (List<Token> lineTokens : lines) {
            HBox lineBox = new HBox();
            lineBox.setAlignment(Pos.CENTER_LEFT);
            for (Token token : lineTokens) {
                Label label = new Label(token.text);
                label.setFont(Font.font("Monospaced", 12));

                String color = token.fallbackColor != null ? token.fallbackColor : "#BCBEC4";
                String bgColor = null;
                boolean isBold = false;
                boolean isItalic = false;

                if (token.categoryKey != null) {
                    ColorAttribute attr = settings.getAttribute(settings.getActiveSchemeName(), token.categoryKey);
                    if (attr == null) {
                        AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(token.categoryKey);
                        if (desc != null) attr = desc.getDefaultAttribute();
                    }
                    if (attr != null) {
                        if (attr.getForeground() != null) {
                            color = attr.getForeground();
                        }
                        if (attr.getBackground() != null) {
                            bgColor = attr.getBackground();
                        }
                        isBold = attr.isBold();
                        isItalic = attr.isItalic();
                    }
                }

                StringBuilder style = new StringBuilder("-fx-text-fill: " + (color.startsWith("#") ? color : "#" + color) + ";");
                if (bgColor != null) {
                    style.append(" -fx-background-color: ").append(bgColor.startsWith("#") ? bgColor : "#" + bgColor).append(";");
                }
                if (isBold && isItalic) {
                    style.append(" -fx-font-weight: bold; -fx-font-style: italic;");
                } else if (isBold) {
                    style.append(" -fx-font-weight: bold;");
                } else if (isItalic) {
                    style.append(" -fx-font-style: italic;");
                }

                label.setStyle(style.toString());

                if (token.categoryKey != null) {
                    label.setCursor(javafx.scene.Cursor.HAND);
                    label.setOnMouseClicked(e -> selectTreeItem(token.categoryKey));
                }

                lineBox.getChildren().add(label);
            }
            codeLinesBox.getChildren().add(lineBox);
        }
    }

    public void selectTreeItem(String key) {
        TreeItem<String> item = findTreeItem(rootItem, key);
        if (item != null) {
            categoryTree.getSelectionModel().select(item);
            selectCategory(item.getValue());
        }
    }

    private TreeItem<String> findTreeItem(TreeItem<String> parent, String key) {
        for (TreeItem<String> child : parent.getChildren()) {
            if (child.getValue().equals(key)) {
                return child;
            }
            TreeItem<String> found = findTreeItem(child, key);
            if (found != null) return found;
        }
        return null;
    }

    public void apply() {
        ColorAttribute current = getCurrentEditorAttribute();
        settings.setAttribute(settings.getActiveSchemeName(), selectedKey, current);
        originalAttributes.put(selectedKey, current.clone());
        isModified = false;
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void reset() {
        for (Map.Entry<String, ColorAttribute> entry : originalAttributes.entrySet()) {
            settings.setAttribute(settings.getActiveSchemeName(), entry.getKey(), entry.getValue().clone());
        }
        isModified = false;
        if (onModifiedListener != null) onModifiedListener.run();
        selectCategory(selectedKey);
        updatePreview();
    }

    public boolean isModified() {
        return isModified;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnNavigateToInherited(Consumer<String> consumer) {
        this.onNavigateToInherited = consumer;
    }

    public void setOnNavigateToInheritedListener(Consumer<String> listener) {
        this.onNavigateToInherited = listener;
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

    public boolean isInheritChecked() {
        return inheritCheck.isSelected();
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
