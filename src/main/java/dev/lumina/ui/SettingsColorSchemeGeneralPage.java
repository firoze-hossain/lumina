package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.*;

/**
 * Editor > Color Scheme > General settings page.
 * Matches 1:1 with reference screenshot media_1790339331164.png:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - Upper section: Category TreeView on the left, Attribute Editor panel on the right.
 * - Lower section: Interactive syntax-colored code preview with line numbers gutter and error stripe minimap.
 * - Real-time preview synchronization and bi-directional token-tree navigation.
 */
public class SettingsColorSchemeGeneralPage extends VBox {

    private final ColorSchemeHeaderBar headerBar = new ColorSchemeHeaderBar();

    // Category tree
    private final TreeView<String> categoryTree = new TreeView<>();

    // Attribute editor controls
    private final CheckBox inheritCheck = new CheckBox("Inherit values from:");
    private final Hyperlink inheritTargetLink = new Hyperlink("Default language text");

    private final CheckBox foregroundCheck = new CheckBox("Foreground");
    private final ColorPicker foregroundPicker = new ColorPicker();

    private final CheckBox backgroundCheck = new CheckBox("Background");
    private final ColorPicker backgroundPicker = new ColorPicker();

    private final CheckBox errorStripeCheck = new CheckBox("Error stripe mark");
    private final ColorPicker errorStripePicker = new ColorPicker();

    private final CheckBox effectsCheck = new CheckBox("Effects");
    private final ComboBox<EffectType> effectTypeCombo = new ComboBox<>();
    private final ColorPicker effectColorPicker = new ColorPicker();

    private final CheckBox boldCheck = new CheckBox("Bold");
    private final CheckBox italicCheck = new CheckBox("Italic");

    // Preview Pane
    private final VBox previewBox = new VBox();
    private final VBox lineNumbersGutter = new VBox();
    private final VBox codeLinesBox = new VBox();
    private final Pane errorStripeGutter = new Pane();

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;
    private String selectedKey = "Text // Default text";

    public SettingsColorSchemeGeneralPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);

        buildUi();
        setupListeners();
        selectTreeItem(selectedKey);
        updatePreview();
    }

    private void buildUi() {
        // --- Upper Section: TreeView + Attribute Options ---
        HBox upperSection = new HBox(16);
        upperSection.setPrefHeight(270);
        upperSection.setMaxHeight(320);

        buildCategoryTree();
        categoryTree.setPrefWidth(300);
        categoryTree.setMinWidth(250);
        categoryTree.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        VBox attributeEditor = buildAttributeEditor();
        HBox.setHgrow(attributeEditor, Priority.ALWAYS);

        upperSection.getChildren().addAll(categoryTree, attributeEditor);

        // --- Lower Section: Live Interactive Code Preview ---
        VBox lowerSection = buildPreviewSection();
        VBox.setVgrow(lowerSection, Priority.ALWAYS);

        getChildren().addAll(headerBar, upperSection, lowerSection);
    }

    private void buildCategoryTree() {
        TreeItem<String> root = new TreeItem<>("Root");
        root.setExpanded(true);

        addTreeCategory(root, "Code", List.of("Method declaration", "Parameter", "Local variable"));
        addTreeCategory(root, "Editor", List.of("Caret", "Line numbers", "Selection", "Breadcrumbs"));
        addTreeCategory(root, "Errors and Warnings", List.of("Error", "Warning", "Weak Warning"));
        addTreeCategory(root, "Hyperlinks", List.of("Inactive hyperlink", "Followed hyperlink", "Reference hyperlink"));
        addTreeCategory(root, "Identifiers", List.of("Identifier under caret", "Identifier under caret (write)"));
        addTreeCategory(root, "Line Coverage", List.of("Full coverage", "Partial coverage", "Uncovered"));
        addTreeCategory(root, "Live Templates", List.of("Active template", "Inactive template"));
        addTreeCategory(root, "Popups and Hints", List.of("Parameter hint", "Inlay hint"));
        addTreeCategory(root, "Preview", List.of("Preview scope"));
        addTreeCategory(root, "Search Results", List.of("Search result", "Search result (write access)"));
        addTreeCategory(root, "Text", List.of("Default text", "Folded text", "Deleted text", "Injected language fragment"));

        categoryTree.setRoot(root);
        categoryTree.setShowRoot(false);
    }

    private void addTreeCategory(TreeItem<String> root, String category, List<String> children) {
        TreeItem<String> catItem = new TreeItem<>(category);
        catItem.setExpanded(true);
        for (String child : children) {
            catItem.getChildren().add(new TreeItem<>(child));
        }
        root.getChildren().add(catItem);
    }

    private VBox buildAttributeEditor() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(8, 12, 8, 12));
        box.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        // Inherit values row
        styleCheckBox(inheritCheck);
        inheritTargetLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0; -fx-underline: false;");
        HBox inheritRow = new HBox(8, inheritCheck, inheritTargetLink);
        inheritRow.setAlignment(Pos.CENTER_LEFT);

        // Foreground row
        styleCheckBox(foregroundCheck);
        styleColorPicker(foregroundPicker);
        HBox fgRow = new HBox(12, foregroundCheck, foregroundPicker);
        fgRow.setAlignment(Pos.CENTER_LEFT);

        // Background row
        styleCheckBox(backgroundCheck);
        styleColorPicker(backgroundPicker);
        HBox bgRow = new HBox(12, backgroundCheck, backgroundPicker);
        bgRow.setAlignment(Pos.CENTER_LEFT);

        // Error stripe mark row
        styleCheckBox(errorStripeCheck);
        styleColorPicker(errorStripePicker);
        HBox stripeRow = new HBox(12, errorStripeCheck, errorStripePicker);
        stripeRow.setAlignment(Pos.CENTER_LEFT);

        // Effects row
        styleCheckBox(effectsCheck);
        effectTypeCombo.getItems().setAll(EffectType.values());
        effectTypeCombo.setValue(EffectType.NONE);
        effectTypeCombo.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");
        effectTypeCombo.setPrefWidth(140);
        styleColorPicker(effectColorPicker);
        HBox effectsRow = new HBox(12, effectsCheck, effectTypeCombo, effectColorPicker);
        effectsRow.setAlignment(Pos.CENTER_LEFT);

        // Font styles
        styleCheckBox(boldCheck);
        styleCheckBox(italicCheck);
        HBox fontStyleRow = new HBox(16, boldCheck, italicCheck);
        fontStyleRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(inheritRow, fgRow, bgRow, stripeRow, effectsRow, fontStyleRow);
        return box;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleColorPicker(ColorPicker cp) {
        cp.setPrefWidth(70);
        cp.setStyle("-fx-background-color: #1E1F22; -fx-color-label-visible: false; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private VBox buildPreviewSection() {
        VBox section = new VBox();
        section.setStyle("-fx-background-color: #18191B; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        HBox editorBox = new HBox();
        editorBox.setStyle("-fx-background-color: #18191B;");

        // Line numbers gutter
        lineNumbersGutter.setPrefWidth(36);
        lineNumbersGutter.setMinWidth(36);
        lineNumbersGutter.setPadding(new Insets(10, 6, 10, 8));
        lineNumbersGutter.setStyle("-fx-background-color: #18191B; -fx-border-color: transparent #2B2D30 transparent transparent;");

        // Code lines container
        codeLinesBox.setPadding(new Insets(10, 12, 10, 12));
        codeLinesBox.setSpacing(3);
        HBox.setHgrow(codeLinesBox, Priority.ALWAYS);

        // Error stripe gutter
        errorStripeGutter.setPrefWidth(14);
        errorStripeGutter.setMinWidth(14);
        errorStripeGutter.setStyle("-fx-background-color: #232529;");

        editorBox.getChildren().addAll(lineNumbersGutter, codeLinesBox, errorStripeGutter);

        ScrollPane scroll = new ScrollPane(editorBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #18191B; -fx-background-color: #18191B; -fx-padding: 0;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        section.getChildren().add(scroll);
        return section;
    }

    private void setupListeners() {
        headerBar.setOnSchemeChanged(scheme -> {
            loadAttributesForSelectedKey();
            updatePreview();
            notifyModified();
        });

        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && selected.getValue() != null) {
                TreeItem<String> parent = selected.getParent();
                if (parent != null && parent.getValue() != null && !parent.getValue().equals("Root")) {
                    selectedKey = parent.getValue() + " // " + selected.getValue();
                } else {
                    selectedKey = selected.getValue() + " // " + (selected.getChildren().isEmpty() ? "" : selected.getChildren().get(0).getValue());
                }
                loadAttributesForSelectedKey();
            }
        });

        // Attribute change listeners
        inheritCheck.selectedProperty().addListener((obs, o, n) -> handleAttributeChanged());
        foregroundCheck.selectedProperty().addListener((obs, o, n) -> handleAttributeChanged());
        foregroundPicker.valueProperty().addListener((obs, o, n) -> handleAttributeChanged());
        backgroundCheck.selectedProperty().addListener((obs, o, n) -> handleAttributeChanged());
        backgroundPicker.valueProperty().addListener((obs, o, n) -> handleAttributeChanged());
        errorStripeCheck.selectedProperty().addListener((obs, o, n) -> handleAttributeChanged());
        errorStripePicker.valueProperty().addListener((obs, o, n) -> handleAttributeChanged());
        effectsCheck.selectedProperty().addListener((obs, o, n) -> handleAttributeChanged());
        effectTypeCombo.valueProperty().addListener((obs, o, n) -> handleAttributeChanged());
        effectColorPicker.valueProperty().addListener((obs, o, n) -> handleAttributeChanged());
        boldCheck.selectedProperty().addListener((obs, o, n) -> handleAttributeChanged());
        italicCheck.selectedProperty().addListener((obs, o, n) -> handleAttributeChanged());
    }

    private void handleAttributeChanged() {
        if (suppressEvents) return;

        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        String active = s.getActiveSchemeName();

        ColorAttribute attr = new ColorAttribute();
        attr.setInherit(inheritCheck.isSelected());
        if (foregroundCheck.isSelected() && foregroundPicker.getValue() != null) {
            attr.setForeground(toHex(foregroundPicker.getValue()));
        }
        if (backgroundCheck.isSelected() && backgroundPicker.getValue() != null) {
            attr.setBackground(toHex(backgroundPicker.getValue()));
        }
        if (errorStripeCheck.isSelected() && errorStripePicker.getValue() != null) {
            attr.setErrorStripeColor(toHex(errorStripePicker.getValue()));
        }
        if (effectsCheck.isSelected()) {
            attr.setEffectType(effectTypeCombo.getValue());
            if (effectColorPicker.getValue() != null) {
                attr.setEffectColor(toHex(effectColorPicker.getValue()));
            }
        }
        attr.setBold(boldCheck.isSelected());
        attr.setItalic(italicCheck.isSelected());

        s.setAttribute(active, selectedKey, attr);
        updatePreview();
        notifyModified();
    }

    private void loadAttributesForSelectedKey() {
        suppressEvents = true;
        try {
            EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
            ColorAttribute attr = s.getAttribute(s.getActiveSchemeName(), selectedKey);
            if (attr == null) attr = new ColorAttribute();

            inheritCheck.setSelected(attr.isInherit());
            inheritTargetLink.setDisable(!attr.isInherit());

            foregroundCheck.setSelected(attr.getForeground() != null);
            foregroundPicker.setDisable(attr.getForeground() == null);
            if (attr.getForeground() != null) {
                foregroundPicker.setValue(Color.web(attr.getForeground()));
            }

            backgroundCheck.setSelected(attr.getBackground() != null);
            backgroundPicker.setDisable(attr.getBackground() == null);
            if (attr.getBackground() != null) {
                backgroundPicker.setValue(Color.web(attr.getBackground()));
            }

            errorStripeCheck.setSelected(attr.getErrorStripeColor() != null);
            errorStripePicker.setDisable(attr.getErrorStripeColor() == null);
            if (attr.getErrorStripeColor() != null) {
                errorStripePicker.setValue(Color.web(attr.getErrorStripeColor()));
            }

            effectsCheck.setSelected(attr.getEffectType() != null && attr.getEffectType() != EffectType.NONE);
            effectTypeCombo.setDisable(!effectsCheck.isSelected());
            effectColorPicker.setDisable(!effectsCheck.isSelected());
            if (attr.getEffectType() != null) {
                effectTypeCombo.setValue(attr.getEffectType());
            }
            if (attr.getEffectColor() != null) {
                effectColorPicker.setValue(Color.web(attr.getEffectColor()));
            }

            boldCheck.setSelected(attr.isBold());
            italicCheck.setSelected(attr.isItalic());
        } finally {
            suppressEvents = false;
        }
    }

    public void selectTreeItem(String fullKey) {
        String[] parts = fullKey.split(" // ");
        String catName = parts[0];
        String childName = parts.length > 1 ? parts[1] : null;

        for (TreeItem<String> cat : categoryTree.getRoot().getChildren()) {
            if (cat.getValue().equalsIgnoreCase(catName)) {
                cat.setExpanded(true);
                if (childName != null) {
                    for (TreeItem<String> child : cat.getChildren()) {
                        if (child.getValue().equalsIgnoreCase(childName)) {
                            categoryTree.getSelectionModel().select(child);
                            selectedKey = fullKey;
                            loadAttributesForSelectedKey();
                            return;
                        }
                    }
                }
                categoryTree.getSelectionModel().select(cat);
                selectedKey = fullKey;
                loadAttributesForSelectedKey();
                return;
            }
        }
    }

    private void updatePreview() {
        lineNumbersGutter.getChildren().clear();
        codeLinesBox.getChildren().clear();
        errorStripeGutter.getChildren().clear();

        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        String scheme = s.getActiveSchemeName();

        // Sample code lines exactly matching reference screenshot Image 5 (strict brand isolation to Lumina)
        String[][] lines = {
                {"//TODO: Visit Lumina resources:", "Code // Method declaration"},
                {"Lumina Home Page: http://www.lumina.dev", "Hyperlinks // Reference hyperlink"},
                {"Lumina Developer Community: https://www.lumina.dev/community", "Hyperlinks // Reference hyperlink"},
                {"ReferenceHyperlink", "Hyperlinks // Reference hyperlink"},
                {"Inactive hyperlink in code: \"http://lumina.dev\"", "Hyperlinks // Inactive hyperlink"},
                {"", null},
                {"Search:", "Text // Default text"},
                {"  result = \"text, text, text\";", "Search Results // Search result"},
                {"  i = result", "Identifiers // Identifier under caret"},
                {"  return i;", "Code // Method declaration"},
                {"", null},
                {"Folded text", "Text // Folded text"},
                {"Folded text with highlighting", "Text // Folded text"},
                {"Deleted text", "Text // Deleted text"},
                {"Live template: active inactive $VARIABLE$", "Live Templates // Active template"},
                {"Injected language: \\.(gif|jpg|png)$", "Text // Injected language fragment"},
                {"", null},
                {"Code Inspections:", "Text // Default text"},
                {"  Error", "Errors and Warnings // Error"},
                {"  Warning", "Errors and Warnings // Warning"}
        };

        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            Label numLabel = new Label(String.valueOf(lineNum));
            numLabel.setStyle("-fx-text-fill: #4E5157; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px;");
            lineNumbersGutter.getChildren().add(numLabel);

            String text = lines[i][0];
            String key = lines[i][1];

            HBox lineBox = new HBox(4);
            lineBox.setAlignment(Pos.CENTER_LEFT);

            if (text.isEmpty()) {
                Label empty = new Label(" ");
                empty.setStyle("-fx-font-size: 12px;");
                lineBox.getChildren().add(empty);
            } else {
                Text lineText = new Text(text);
                lineText.setFont(Font.font("JetBrains Mono", 12));

                // Apply style from settings
                ColorAttribute attr = (key != null) ? s.getAttribute(scheme, key) : null;
                applyStyleToText(lineText, attr, key, text);

                // Make interactive: click to select tree item
                lineBox.setOnMouseClicked(e -> {
                    if (key != null) {
                        selectTreeItem(key);
                    }
                });
                lineBox.setStyle("-fx-cursor: hand;");

                lineBox.getChildren().add(lineText);
            }

            codeLinesBox.getChildren().add(lineBox);

            // Error stripe markings
            if ("Errors and Warnings // Error".equals(key)) {
                Rectangle stripe = new Rectangle(0, i * 19.0 + 10, 14, 4);
                stripe.setFill(Color.web("#E5534B"));
                errorStripeGutter.getChildren().add(stripe);
            } else if ("Errors and Warnings // Warning".equals(key)) {
                Rectangle stripe = new Rectangle(0, i * 19.0 + 10, 14, 4);
                stripe.setFill(Color.web("#D8A657"));
                errorStripeGutter.getChildren().add(stripe);
            } else if ("Search Results // Search result".equals(key)) {
                Rectangle stripe = new Rectangle(0, i * 19.0 + 10, 14, 3);
                stripe.setFill(Color.web("#2E5F7E"));
                errorStripeGutter.getChildren().add(stripe);
            }
        }
    }

    private void applyStyleToText(Text t, ColorAttribute attr, String key, String raw) {
        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#DFE1E5";
        boolean bold = attr != null && attr.isBold();
        boolean italic = attr != null && attr.isItalic();

        // Default syntax coloring if not overridden
        if (key != null) {
            if (key.contains("Hyperlink")) {
                fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : (raw.contains("Inactive") ? "#7A7E85" : "#589DF6");
                t.setUnderline(true);
            } else if (key.contains("Deleted text")) {
                t.setStrikethrough(true);
                fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#E5534B";
            } else if (key.contains("Error")) {
                t.setUnderline(true);
                fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#E5534B";
            } else if (key.contains("Warning")) {
                t.setUnderline(true);
                fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#D8A657";
            } else if (raw.startsWith("//")) {
                fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#57A64A";
                italic = true;
            } else if (key.contains("Folded")) {
                fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#8C8C8C";
            }
        }

        t.setFill(Color.web(fg));
        FontWeight weight = bold ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = italic ? FontPosture.ITALIC : FontPosture.REGULAR;
        t.setFont(Font.font("JetBrains Mono", weight, posture, 12));
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public boolean isModified() {
        String active = EditorColorSchemeSettings.getInstance().getActiveSchemeName();
        return EditorColorSchemeSettings.getInstance().isSchemeModified(active);
    }

    public void apply() {
        EditorColorSchemeSettings.getInstance().save();
    }

    public void reset() {
        String active = EditorColorSchemeSettings.getInstance().getActiveSchemeName();
        EditorColorSchemeSettings.getInstance().restoreDefaults(active);
        loadAttributesForSelectedKey();
        updatePreview();
    }

    public ColorSchemeHeaderBar getHeaderBar() {
        return headerBar;
    }

    public TreeView<String> getCategoryTree() {
        return categoryTree;
    }

    public CheckBox getForegroundCheck() {
        return foregroundCheck;
    }

    public ColorPicker getForegroundPicker() {
        return foregroundPicker;
    }

    public CheckBox getBoldCheck() {
        return boldCheck;
    }

    public CheckBox getItalicCheck() {
        return italicCheck;
    }
}
