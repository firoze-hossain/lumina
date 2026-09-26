package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.*;

/**
 * Editor > Color Scheme > Language Defaults settings page.
 * Provides dynamic descriptor-driven architecture, dynamic tree generation,
 * inheritance resolution, and interactive live code preview with bidirectional selection
 * exactly matching IntelliJ IDEA.
 */
public class SettingsColorSchemeLanguageDefaultsPage extends VBox {

    private final ColorSchemeHeaderBar headerBar;
    private final TreeView<String> categoryTree;
    private final VBox attributeEditorBox;

    // Checkboxes and controls for editing attributes
    private final CheckBox boldCheck;
    private final CheckBox italicCheck;

    private final CheckBox foregroundCheck;
    private final Button foregroundSwatch;

    private final CheckBox backgroundCheck;
    private final Button backgroundSwatch;

    private final CheckBox errorStripeCheck;
    private final Button errorStripeSwatch;

    private final CheckBox effectsCheck;
    private final Button effectsSwatch;
    private final ComboBox<EffectType> effectTypeCombo;

    // Inheritance controls
    private final VBox inheritBox;
    private final CheckBox inheritCheck;
    private final Hyperlink inheritLink;
    private final Label inheritScopeLabel;

    // Live preview
    private final VBox codeLinesBox;
    private final Pane errorStripeGutter;
    private final ScrollPane previewScrollPane;

    // State
    private String selectedKey = "Bad character";
    private String currentInheritedTargetKey = null;
    private String foregroundHex = "#F75464";
    private String backgroundHex = null;
    private String errorStripeHex = null;
    private String effectsHex = null;
    private boolean suppressEvents = false;

    // Navigation history
    private final List<String> navigationHistory = new ArrayList<>();
    private int historyIndex = -1;
    private boolean isNavigatingHistory = false;

    private Runnable onModifiedListener;

    public SettingsColorSchemeLanguageDefaultsPage() {
        setSpacing(12);
        setPadding(new Insets(12, 16, 16, 16));
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Header Bar
        headerBar = new ColorSchemeHeaderBar();
        headerBar.setOnSchemeChanged(scheme -> {
            loadAttributesForSelectedKey();
            updatePreview();
            notifyModified();
        });

        // 2. Tree View
        categoryTree = new TreeView<>();
        categoryTree.setShowRoot(false);
        categoryTree.setStyle(
                "-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #2B2D30; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        categoryTree.setPrefWidth(320);
        categoryTree.setMinWidth(260);

        TreeItem<String> root = new TreeItem<>("Root");
        categoryTree.setRoot(root);
        buildCategoryTree(root);

        // 3. Attribute Editor Box (Right Pane)
        attributeEditorBox = new VBox(10);
        attributeEditorBox.setPadding(new Insets(8, 12, 8, 12));
        attributeEditorBox.setStyle("-fx-background-color: #1E1F22;");
        HBox.setHgrow(attributeEditorBox, Priority.ALWAYS);

        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && selected.getValue() != null) {
                // If category group with children is selected (e.g. Braces and Operators, Classes, Comments)
                if (!selected.getChildren().isEmpty()) {
                    selectedKey = selected.getValue();
                    attributeEditorBox.setVisible(false);
                    attributeEditorBox.setManaged(false);
                    return;
                }
                attributeEditorBox.setVisible(true);
                attributeEditorBox.setManaged(true);

                // Build full hierarchical key
                selectedKey = buildFullKey(selected);
                recordNavigation(selectedKey);
                loadAttributesForSelectedKey();
                updatePreview();
            }
        });

        // Bold & Italic checkboxes (top row, right aligned)
        HBox fontStyleRow = new HBox(16);
        fontStyleRow.setAlignment(Pos.CENTER_RIGHT);
        boldCheck = new CheckBox("Bold");
        boldCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-cursor: hand;");
        italicCheck = new CheckBox("Italic");
        italicCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-cursor: hand;");
        fontStyleRow.getChildren().addAll(boldCheck, italicCheck);

        boldCheck.selectedProperty().addListener((o, ov, nv) -> onAttributeChanged());
        italicCheck.selectedProperty().addListener((o, ov, nv) -> onAttributeChanged());

        // Foreground row
        foregroundCheck = new CheckBox("Foreground");
        foregroundCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-cursor: hand;");
        foregroundCheck.setPrefWidth(140);
        foregroundSwatch = createSwatchButton();
        setupSwatchButton(foregroundSwatch, color -> {
            foregroundHex = color;
            foregroundCheck.setSelected(color != null);
            onAttributeChanged();
        });
        foregroundCheck.selectedProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && !nv) {
                foregroundHex = null;
                updateSwatchButton(foregroundSwatch, null, false, false);
                onAttributeChanged();
            }
        });
        HBox foregroundRow = new HBox(8, foregroundCheck, foregroundSwatch);
        foregroundRow.setAlignment(Pos.CENTER_LEFT);

        // Background row
        backgroundCheck = new CheckBox("Background");
        backgroundCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-cursor: hand;");
        backgroundCheck.setPrefWidth(140);
        backgroundSwatch = createSwatchButton();
        setupSwatchButton(backgroundSwatch, color -> {
            backgroundHex = color;
            backgroundCheck.setSelected(color != null);
            onAttributeChanged();
        });
        backgroundCheck.selectedProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && !nv) {
                backgroundHex = null;
                updateSwatchButton(backgroundSwatch, null, false, false);
                onAttributeChanged();
            }
        });
        HBox backgroundRow = new HBox(8, backgroundCheck, backgroundSwatch);
        backgroundRow.setAlignment(Pos.CENTER_LEFT);

        // Error stripe mark row
        errorStripeCheck = new CheckBox("Error stripe mark");
        errorStripeCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-cursor: hand;");
        errorStripeCheck.setPrefWidth(140);
        errorStripeSwatch = createSwatchButton();
        setupSwatchButton(errorStripeSwatch, color -> {
            errorStripeHex = color;
            errorStripeCheck.setSelected(color != null);
            onAttributeChanged();
        });
        errorStripeCheck.selectedProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && !nv) {
                errorStripeHex = null;
                updateSwatchButton(errorStripeSwatch, null, false, false);
                onAttributeChanged();
            }
        });
        HBox errorStripeRow = new HBox(8, errorStripeCheck, errorStripeSwatch);
        errorStripeRow.setAlignment(Pos.CENTER_LEFT);

        // Effects row
        effectsCheck = new CheckBox("Effects");
        effectsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-cursor: hand;");
        effectsCheck.setPrefWidth(140);
        effectsSwatch = createSwatchButton();
        setupSwatchButton(effectsSwatch, color -> {
            effectsHex = color;
            effectsCheck.setSelected(color != null);
            onAttributeChanged();
        });

        effectTypeCombo = new ComboBox<>();
        effectTypeCombo.getItems().addAll(
                EffectType.UNDERSCORED,
                EffectType.BOLD_UNDERSCORED,
                EffectType.UNDERWAVED,
                EffectType.BORDERED,
                EffectType.STRIKEOUT
        );
        effectTypeCombo.setValue(EffectType.UNDERSCORED);
        effectTypeCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;"
        );
        effectTypeCombo.setPrefWidth(130);
        effectTypeCombo.valueProperty().addListener((o, ov, nv) -> onAttributeChanged());

        effectsCheck.selectedProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && !nv) {
                effectsHex = null;
                updateSwatchButton(effectsSwatch, null, false, false);
                onAttributeChanged();
            }
        });

        HBox effectsRow = new HBox(8, effectsCheck, effectsSwatch);
        effectsRow.setAlignment(Pos.CENTER_LEFT);

        HBox effectTypeRow = new HBox(8, new Region() {{ setPrefWidth(140); }}, effectTypeCombo);
        effectTypeRow.setAlignment(Pos.CENTER_LEFT);

        // Inheritance Box
        inheritBox = new VBox(4);
        inheritBox.setPadding(new Insets(12, 0, 0, 0));

        inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-cursor: hand;");
        inheritCheck.selectedProperty().addListener((o, ov, nv) -> onInheritChanged(nv));

        inheritLink = new Hyperlink();
        inheritLink.setStyle(
                "-fx-text-fill: #3574F0; -fx-font-size: 12.5px; -fx-padding: 0 0 0 22; " +
                "-fx-underline: false; -fx-cursor: hand;"
        );
        inheritLink.setOnAction(e -> {
            if (currentInheritedTargetKey != null) {
                selectTreeItem(currentInheritedTargetKey);
            }
        });

        inheritScopeLabel = new Label();
        inheritScopeLabel.setStyle("-fx-text-fill: #868991; -fx-font-size: 12px; -fx-padding: 0 0 0 22;");

        inheritBox.getChildren().addAll(inheritCheck, inheritLink, inheritScopeLabel);

        attributeEditorBox.getChildren().addAll(
                fontStyleRow,
                foregroundRow,
                backgroundRow,
                errorStripeRow,
                effectsRow,
                effectTypeRow,
                inheritBox
        );

        // Top horizontal split: Tree on left, Attribute Editor on right
        HBox topArea = new HBox(16, categoryTree, attributeEditorBox);
        topArea.setPrefHeight(230);
        topArea.setMinHeight(200);

        // 4. Code Preview Area
        codeLinesBox = new VBox(2);
        codeLinesBox.setStyle("-fx-background-color: #1E1F22; -fx-padding: 10 12 10 12;");
        HBox.setHgrow(codeLinesBox, Priority.ALWAYS);

        errorStripeGutter = new Pane();
        errorStripeGutter.setPrefWidth(14);
        errorStripeGutter.setMinWidth(14);
        errorStripeGutter.setMaxWidth(14);
        errorStripeGutter.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent transparent #2B2D30;");

        HBox previewContainer = new HBox(0, codeLinesBox, errorStripeGutter);
        previewContainer.setStyle("-fx-background-color: #1E1F22;");
        HBox.setHgrow(previewContainer, Priority.ALWAYS);

        previewScrollPane = new ScrollPane(previewContainer);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle(
                "-fx-background: #1E1F22; -fx-background-color: #1E1F22; " +
                "-fx-border-color: #2B2D30; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        previewScrollPane.setPrefHeight(320);
        previewScrollPane.setMinHeight(220);
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        getChildren().addAll(headerBar, topArea, previewScrollPane);

        // Initial selection: "Bad character"
        selectTreeItem("Bad character");
        updatePreview();
    }

    private void buildCategoryTree(TreeItem<String> root) {
        Map<String, TreeItem<String>> nodeMap = new LinkedHashMap<>();
        for (EditorColorSchemeSettings.AttributesDescriptor desc : EditorColorSchemeSettings.getLanguageDefaultsDescriptors()) {
            TreeItem<String> parent = root;
            StringBuilder pathAcc = new StringBuilder();
            for (String cat : desc.getCategoryPath()) {
                if (pathAcc.length() > 0) pathAcc.append(" // ");
                pathAcc.append(cat);
                String fullPath = pathAcc.toString();
                TreeItem<String> catNode = nodeMap.get(fullPath);
                if (catNode == null) {
                    catNode = new TreeItem<>(cat);
                    nodeMap.put(fullPath, catNode);
                    parent.getChildren().add(catNode);
                }
                parent = catNode;
            }
            TreeItem<String> leaf = new TreeItem<>(desc.getDisplayName());
            nodeMap.put(desc.getKey(), leaf);
            parent.getChildren().add(leaf);
        }
    }

    private String buildFullKey(TreeItem<String> item) {
        List<String> parts = new ArrayList<>();
        TreeItem<String> curr = item;
        while (curr != null && curr.getParent() != null) {
            parts.add(0, curr.getValue());
            curr = curr.getParent();
        }
        return String.join(" // ", parts);
    }

    private Button createSwatchButton() {
        Button btn = new Button();
        btn.setPrefSize(55, 24);
        btn.setMinSize(55, 24);
        btn.setMaxSize(55, 24);
        btn.setFont(Font.font("JetBrains Mono", FontWeight.NORMAL, 11));
        return btn;
    }

    private void setupSwatchButton(Button btn, java.util.function.Consumer<String> onColorSelected) {
        btn.setOnAction(e -> {
            Color initial = Color.web(btn.getText() != null && !btn.getText().isBlank() ? "#" + btn.getText() : "#DFE1E5");
            ColorPicker picker = new ColorPicker(initial);
            picker.setOnAction(ev -> {
                Color c = picker.getValue();
                if (c != null) {
                    String hex = toHex(c);
                    onColorSelected.accept(hex);
                }
            });
            picker.show();
        });
    }

    private void updateSwatchButton(Button btn, String hex, boolean enabled, boolean isInherited) {
        if (enabled && hex != null && !hex.isBlank()) {
            String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
            String fullHex = "#" + cleanHex;
            Color c = Color.web(fullHex);
            double brightness = (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
            String textFill = brightness > 0.6 ? "#000000" : "#FFFFFF";

            btn.setText(cleanHex.toUpperCase());
            btn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: %s; -fx-padding: 0; -fx-opacity: %s;",
                    fullHex, textFill, isInherited ? "default" : "hand", isInherited ? "0.65" : "1.0"
            ));
            btn.setDisable(isInherited);
        } else {
            btn.setText("");
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 0;");
            btn.setDisable(isInherited || !enabled);
        }
    }

    private void onInheritChanged(boolean inherit) {
        if (suppressEvents) return;
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        String active = s.getActiveSchemeName();
        ColorAttribute attr = s.getAttribute(active, selectedKey);
        if (attr == null) {
            EditorColorSchemeSettings.AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
            attr = (desc != null) ? desc.getDefaultAttribute() : new ColorAttribute();
        }
        attr.setInherit(inherit);
        s.setAttribute(active, selectedKey, attr);
        loadAttributesForSelectedKey();
        updatePreview();
        notifyModified();
    }

    private void onAttributeChanged() {
        if (suppressEvents) return;
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        String active = s.getActiveSchemeName();

        ColorAttribute attr = s.getAttribute(active, selectedKey);
        if (attr == null) {
            EditorColorSchemeSettings.AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
            attr = (desc != null) ? desc.getDefaultAttribute() : new ColorAttribute();
        }
        attr.setInherit(inheritBox.isVisible() && inheritCheck.isSelected());

        if (foregroundCheck.isSelected() && foregroundHex != null) {
            attr.setForeground("#" + (foregroundHex.startsWith("#") ? foregroundHex.substring(1) : foregroundHex));
        } else {
            attr.setForeground(null);
        }

        if (backgroundCheck.isSelected() && backgroundHex != null) {
            attr.setBackground("#" + (backgroundHex.startsWith("#") ? backgroundHex.substring(1) : backgroundHex));
        } else {
            attr.setBackground(null);
        }

        if (errorStripeCheck.isSelected() && errorStripeHex != null) {
            attr.setErrorStripeColor("#" + (errorStripeHex.startsWith("#") ? errorStripeHex.substring(1) : errorStripeHex));
        } else {
            attr.setErrorStripeColor(null);
        }

        if (effectsCheck.isSelected() && effectsHex != null) {
            attr.setEffectColor("#" + (effectsHex.startsWith("#") ? effectsHex.substring(1) : effectsHex));
            attr.setEffectType(effectTypeCombo.getValue() != null ? effectTypeCombo.getValue() : EffectType.UNDERSCORED);
        } else {
            attr.setEffectColor(null);
            attr.setEffectType(EffectType.NONE);
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
            selectedKey = EditorColorSchemeSettings.normalizeKey(selectedKey);

            EditorColorSchemeSettings.AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
            ColorAttribute raw = s.getAttribute(s.getActiveSchemeName(), selectedKey);
            if (raw == null) {
                raw = (desc != null) ? desc.getDefaultAttribute() : new ColorAttribute();
            }

            // Inheritance setup matching IntelliJ Language Defaults
            boolean hasInheritDef = desc != null && desc.hasInheritance();
            if (hasInheritDef || (raw.getInheritFrom() != null && !raw.getInheritFrom().isBlank())) {
                inheritBox.setVisible(true);
                inheritBox.setManaged(true);
                inheritCheck.setSelected(raw.isInherit());
                String target = raw.getInheritFrom() != null ? raw.getInheritFrom() : desc.getInheritFrom();
                currentInheritedTargetKey = target;
                inheritLink.setText(target.replace(" // ", "->"));
                String scope = raw.getInheritScope() != null ? raw.getInheritScope() : (desc != null ? desc.getInheritScope() : "(Language Defaults)");
                inheritScopeLabel.setText(scope != null ? scope : "(Language Defaults)");
            } else {
                inheritBox.setVisible(false);
                inheritBox.setManaged(false);
                currentInheritedTargetKey = null;
            }

            ColorAttribute effective = s.resolveAttribute(s.getActiveSchemeName(), selectedKey);

            boolean isInherited = inheritBox.isVisible() && inheritCheck.isSelected();

            boldCheck.setSelected(effective.isBold());
            italicCheck.setSelected(effective.isItalic());
            boldCheck.setDisable(isInherited);
            italicCheck.setDisable(isInherited);

            foregroundHex = effective.getForeground();
            foregroundCheck.setSelected(foregroundHex != null);
            foregroundCheck.setDisable(isInherited);
            updateSwatchButton(foregroundSwatch, foregroundHex, foregroundCheck.isSelected(), isInherited);

            backgroundHex = effective.getBackground();
            backgroundCheck.setSelected(backgroundHex != null);
            backgroundCheck.setDisable(isInherited);
            updateSwatchButton(backgroundSwatch, backgroundHex, backgroundCheck.isSelected(), isInherited);

            errorStripeHex = effective.getErrorStripeColor();
            errorStripeCheck.setSelected(errorStripeHex != null);
            errorStripeCheck.setDisable(isInherited);
            updateSwatchButton(errorStripeSwatch, errorStripeHex, errorStripeCheck.isSelected(), isInherited);

            boolean hasEffects = effective.getEffectType() != null && effective.getEffectType() != EffectType.NONE;
            effectsCheck.setSelected(hasEffects);
            effectsCheck.setDisable(isInherited);
            effectsHex = effective.getEffectColor();
            updateSwatchButton(effectsSwatch, effectsHex, hasEffects, isInherited);
            effectTypeCombo.setDisable(isInherited || !hasEffects);

            if (effective.getEffectType() != null && effective.getEffectType() != EffectType.NONE) {
                effectTypeCombo.setValue(effective.getEffectType());
            } else if (desc != null && desc.getDefaultEffectType() != null) {
                effectTypeCombo.setValue(desc.getDefaultEffectType());
            } else {
                effectTypeCombo.setValue(EffectType.UNDERSCORED);
            }
        } finally {
            suppressEvents = false;
        }
    }

    private void recordNavigation(String key) {
        if (isNavigatingHistory) return;
        if (historyIndex >= 0 && historyIndex < navigationHistory.size() && navigationHistory.get(historyIndex).equals(key)) {
            return;
        }
        while (navigationHistory.size() > historyIndex + 1) {
            navigationHistory.remove(navigationHistory.size() - 1);
        }
        navigationHistory.add(key);
        historyIndex = navigationHistory.size() - 1;
    }

    public void navigateBack() {
        if (historyIndex > 0) {
            historyIndex--;
            isNavigatingHistory = true;
            try {
                selectTreeItem(navigationHistory.get(historyIndex));
            } finally {
                isNavigatingHistory = false;
            }
        }
    }

    public void navigateForward() {
        if (historyIndex < navigationHistory.size() - 1) {
            historyIndex++;
            isNavigatingHistory = true;
            try {
                selectTreeItem(navigationHistory.get(historyIndex));
            } finally {
                isNavigatingHistory = false;
            }
        }
    }

    public void selectTreeItem(String fullKey) {
        String normKey = EditorColorSchemeSettings.normalizeKey(fullKey);
        String[] parts = normKey.split(" // ");
        TreeItem<String> curr = categoryTree.getRoot();

        for (String part : parts) {
            TreeItem<String> matched = null;
            for (TreeItem<String> child : curr.getChildren()) {
                if (child.getValue().equalsIgnoreCase(part)) {
                    matched = child;
                    child.setExpanded(true);
                    break;
                }
            }
            if (matched == null) return;
            curr = matched;
        }

        categoryTree.getSelectionModel().select(curr);
        selectedKey = normKey;
        if (curr.getChildren().isEmpty()) {
            loadAttributesForSelectedKey();
        } else {
            attributeEditorBox.setVisible(false);
            attributeEditorBox.setManaged(false);
        }
    }

    private void updatePreview() {
        codeLinesBox.getChildren().clear();
        errorStripeGutter.getChildren().clear();

        // Line 0: Bad characters: ???? (media_1790383980530.png)
        addLine(
                token("Bad characters: ", null),
                token("????", "Bad character")
        );

        // Line 1: Keyword
        addLine(
                token("Keyword", "Keyword")
        );

        // Line 2: Identifier
        addLine(
                token("Identifier", "Identifiers // Default")
        );

        // Line 3: 'String \n\?'
        addLine(
                token("'String ", "String // String text"),
                token("\\n", "String // Escape sequence"),
                token("\\?", "String // Invalid escape sequence"),
                token("'", "String // String text")
        );

        // Line 4: 12345
        addLine(
                token("12345", "Number")
        );

        // Line 5: Operator
        addLine(
                token("Operator", "Braces and Operators // Operation sign")
        );

        // Line 6: Dot: . comma: ,, semicolon: ;
        addLine(
                token("Dot: ", null),
                token(".", "Braces and Operators // Dot"),
                token(" comma: ", null),
                token(",", "Braces and Operators // Comma"),
                token(" semicolon: ", null),
                token(";", "Braces and Operators // Semicolon")
        );

        // Line 7: { Braces }
        addLine(
                token("{", "Braces and Operators // Braces"),
                token(" Braces ", null),
                token("}", "Braces and Operators // Braces")
        );

        // Line 8: ( Parentheses )
        addLine(
                token("(", "Braces and Operators // Parentheses"),
                token(" Parentheses ", null),
                token(")", "Braces and Operators // Parentheses")
        );

        // Line 9: [ Brackets ] (media_1790383998278.png)
        addLine(
                token("[", "Braces and Operators // Brackets"),
                token(" Brackets ", null),
                token("]", "Braces and Operators // Brackets")
        );

        // Line 10: // Line comment
        addLine(
                token("// Line comment", "Comments // Line comment")
        );

        // Line 11: /* Block comment */
        addLine(
                token("/* Block comment */", "Comments // Block comment")
        );

        // Line 12: :Label
        addLine(
                token(":Label", "Identifiers // Label")
        );

        // Line 13: predefined_symbol()
        addLine(
                token("predefined_symbol", "Identifiers // Predefined symbol"),
                token("()", null)
        );

        // Line 14: CONSTANT
        addLine(
                token("CONSTANT", "Identifiers // Constant")
        );

        // Line 15: Global variable
        addLine(
                token("Global variable", "Identifiers // Global variable")
        );

        // Line 16: | Rendered documentation with link (media_1790384048158.png)
        addLine(
                renderedDocGuide(),
                token("Rendered documentation with ", null),
                token("link", "Comments // Doc comment // Link in rendered view")
        );

        // Line 17: /**
        addLine(
                token("/**", "Comments // Doc comment // Text")
        );

        // Line 18:  * Doc comment
        addLine(
                token(" * Doc comment", "Comments // Doc comment // Text")
        );

        // Line 19:  * @tag <code>Markup</code>
        addLine(
                token(" * ", "Comments // Doc comment // Text"),
                token("@tag", "Comments // Doc comment // Tag"),
                token(" ", null),
                token("<code>", "Comments // Doc comment // Text"),
                token("Markup", "Comments // Doc comment // Markup"),
                token("</code>", "Comments // Doc comment // Text")
        );

        // Line 20:  * Semantic highlighting:
        addLine(
                token(" * ", "Comments // Doc comment // Text"),
                token("Semantic highlighting:", "Semantic highlighting")
        );

        // Line 21:  * Generated spectrum to pick colors for local variables and parameters:
        addLine(
                token(" * Generated spectrum to pick colors for local variables and parameters:", "Comments // Doc comment // Text")
        );

        // Line 22: Spectrum line 1
        addLine(
                spectrumLine1()
        );

        // Line 23: Spectrum line 2
        addLine(
                spectrumLine2()
        );

        // Line 24:  * @param parameter1 documentation
        addLine(
                token(" * ", "Comments // Doc comment // Text"),
                token("@param", "Comments // Doc comment // Tag"),
                token(" ", null),
                token("parameter1", "Identifiers // Parameter"),
                token(" documentation", "Comments // Doc comment // Text")
        );

        // Line 25:  */
        addLine(
                token(" */", "Comments // Doc comment // Text")
        );

        // Right gutter error stripes
        addGutterStripe(0, "#F75464"); // Bad characters error mark
        addGutterStripe(3, "#F75464"); // Invalid escape sequence mark
    }

    private void addLine(Node... nodes) {
        HBox lineBox = new HBox(0);
        lineBox.setAlignment(Pos.CENTER_LEFT);
        lineBox.setPrefHeight(20);
        lineBox.setMinHeight(20);
        lineBox.setMaxHeight(20);
        lineBox.getChildren().addAll(nodes);
        codeLinesBox.getChildren().add(lineBox);
    }

    private Node token(String text, String key) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = (key != null) ? s.resolveAttribute(s.getActiveSchemeName(), key) : null;

        Text t = new Text(text);
        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#DFE1E5";
        t.setFill(Color.web(fg));

        FontWeight weight = (attr != null && attr.isBold()) ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = (attr != null && attr.isItalic()) ? FontPosture.ITALIC : FontPosture.REGULAR;
        t.setFont(Font.font("JetBrains Mono", weight, posture, 12.5));

        if (attr != null) {
            if (attr.getEffectType() == EffectType.UNDERSCORED || attr.getEffectType() == EffectType.BOLD_UNDERSCORED) {
                t.setUnderline(true);
            }
            if (attr.getEffectType() == EffectType.STRIKEOUT) {
                t.setStrikethrough(true);
            }
        }

        Node visualNode = t;
        if (attr != null && attr.getEffectType() == EffectType.UNDERWAVED) {
            String waveColor = attr.getEffectColor() != null ? attr.getEffectColor() : fg;
            double width = text.length() * 7.5;
            Canvas wave = new Canvas(width, 3);
            GraphicsContext gc = wave.getGraphicsContext2D();
            gc.setStroke(Color.web(waveColor));
            gc.setLineWidth(1.1);
            for (double x = 0; x < width; x += 4) {
                gc.strokeLine(x, 2, x + 2, 0);
                gc.strokeLine(x + 2, 0, x + 4, 2);
            }
            VBox vb = new VBox(1, t, wave);
            vb.setAlignment(Pos.CENTER_LEFT);
            visualNode = vb;
        }

        StackPane container = new StackPane(visualNode);
        container.setAlignment(Pos.CENTER_LEFT);

        StringBuilder style = new StringBuilder();
        if (attr != null && attr.getBackground() != null) {
            style.append(String.format("-fx-background-color: %s; -fx-padding: 1 3 1 3; -fx-background-radius: 2; ", attr.getBackground()));
        }
        if (attr != null && attr.getEffectType() == EffectType.BORDERED) {
            String bc = attr.getEffectColor() != null ? attr.getEffectColor() : "#393B40";
            style.append(String.format("-fx-border-color: %s; -fx-border-width: 1; -fx-border-radius: 2; ", bc));
        }

        // Active selection focus border matching IntelliJ Screenshot 3
        boolean isSelected = key != null && (key.equals(selectedKey) || selectedKey.endsWith(" // " + key) || key.endsWith(" // " + selectedKey));
        if (isSelected) {
            style.append("-fx-border-color: #525866; -fx-border-width: 1; -fx-border-radius: 2; ");
        }

        if (key != null) {
            style.append("-fx-cursor: hand; ");
            container.setOnMouseClicked(e -> selectTreeItem(key));
        }
        if (style.length() > 0) {
            container.setStyle(style.toString());
        }

        return container;
    }

    private Node renderedDocGuide() {
        Rectangle guide = new Rectangle(2, 14);
        guide.setFill(Color.web("#3887A1"));
        HBox box = new HBox(guide);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 6, 0, 0));
        return box;
    }

    private Node[] spectrumLine1() {
        return new Node[] {
                token(" * ", "Comments // Doc comment // Text"),
                coloredToken("Color#1 ", "#4CD98B", "Semantic highlighting"),
                coloredToken("SC1.1 ", "#4CD98B", "Semantic highlighting"),
                coloredToken("SC1.2 ", "#4CD98B", "Semantic highlighting"),
                coloredToken("SC1.3 ", "#4CD98B", "Semantic highlighting"),
                coloredToken("SC1.4 ", "#4CD98B", "Semantic highlighting"),
                coloredToken("Color#2 ", "#56A8F5", "Semantic highlighting"),
                coloredToken("SC2.1 ", "#56A8F5", "Semantic highlighting"),
                coloredToken("SC2.2 ", "#56A8F5", "Semantic highlighting"),
                coloredToken("SC2.3 ", "#56A8F5", "Semantic highlighting"),
                coloredToken("SC2.4 ", "#56A8F5", "Semantic highlighting"),
                coloredToken("Color#3", "#C77DBB", "Semantic highlighting")
        };
    }

    private Node[] spectrumLine2() {
        return new Node[] {
                token(" * ", "Comments // Doc comment // Text"),
                coloredToken("Color#3 ", "#C77DBB", "Semantic highlighting"),
                coloredToken("SC3.1 ", "#C77DBB", "Semantic highlighting"),
                coloredToken("SC3.2 ", "#C77DBB", "Semantic highlighting"),
                coloredToken("SC3.3 ", "#C77DBB", "Semantic highlighting"),
                coloredToken("SC3.4 ", "#C77DBB", "Semantic highlighting"),
                coloredToken("Color#4 ", "#E5B567", "Semantic highlighting"),
                coloredToken("SC4.1 ", "#E5B567", "Semantic highlighting"),
                coloredToken("SC4.2 ", "#E5B567", "Semantic highlighting"),
                coloredToken("SC4.3 ", "#E5B567", "Semantic highlighting"),
                coloredToken("SC4.4 ", "#E5B567", "Semantic highlighting"),
                coloredToken("Color#5", "#F75464", "Semantic highlighting")
        };
    }

    private Node coloredToken(String text, String colorHex, String key) {
        Text t = new Text(text);
        t.setFont(Font.font("JetBrains Mono", 12.0));
        t.setFill(Color.web(colorHex));
        HBox box = new HBox(t);
        box.setAlignment(Pos.CENTER_LEFT);
        if (key != null) {
            box.setStyle("-fx-cursor: hand;");
            box.setOnMouseClicked(e -> selectTreeItem(key));
        }
        return box;
    }

    private void addGutterStripe(int lineIndex, String colorHex) {
        Rectangle stripe = new Rectangle(0, lineIndex * 20.0 + 4, 14, 2);
        stripe.setFill(Color.web(colorHex));
        errorStripeGutter.getChildren().add(stripe);
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

    public CheckBox getBoldCheck() {
        return boldCheck;
    }

    public CheckBox getItalicCheck() {
        return italicCheck;
    }

    public VBox getInheritBox() {
        return inheritBox;
    }

    public CheckBox getInheritCheck() {
        return inheritCheck;
    }

    public Hyperlink getInheritLink() {
        return inheritLink;
    }

    public VBox getAttributeEditorBox() {
        return attributeEditorBox;
    }
}