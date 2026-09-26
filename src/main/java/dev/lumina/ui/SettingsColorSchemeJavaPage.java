package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.AttributesDescriptor;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;
import java.util.function.Consumer;

/**
 * Editor > Color Scheme > Java settings page.
 * Replicates 1:1 visual and functional parity with reference screenshots:
 * - media_1790425875746.png (Annotations > Annotation name)
 * - media_1790425905497.png (Braces and Operators > Brackets)
 * - media_1790425922623.png (Class Fields > Instance field)
 * - media_1790425951849.png (Classes and Interfaces > Class)
 *
 * Features:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - Dynamic Category Tree view with hierarchical branches (Annotations, Braces and Operators,
 *   Class Fields, Classes and Interfaces, Comments, Keyword, Methods, Number, Parameters,
 *   Semantic highlighting, String, Variables, Visibility).
 * - Full Attribute Editor panel with Bold, Italic, Foreground, Background, Error stripe mark,
 *   Effects (dropdown), and "Inherit values from" with interactive jump hyperlink.
 * - Live Interactive Java Code Editor Preview with dotted indent guides, syntax tokens,
 *   token selection box, right margin guideline, error stripe bar, and bidirectional click synchronization.
 */
public class SettingsColorSchemeJavaPage extends VBox {

    private final ColorSchemeHeaderBar headerBar;
    private final TreeView<String> categoryTree;
    private final VBox attributeEditorBox;

    // Attribute controls
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
    private final Label inheritTargetLabel;
    private final Label inheritScopeLabel;

    // Preview
    private final VBox codeLinesBox;
    private final ScrollPane previewScrollPane;

    // State
    private String selectedKey = "Annotation name";
    private String currentInheritedTargetKey = "Metadata";
    private String currentInheritedScope = "(Language Defaults)";
    private String foregroundHex = "#B3AF60";
    private String backgroundHex = null;
    private String errorStripeHex = null;
    private String effectsHex = null;
    private boolean suppressEvents = false;
    private boolean modified = false;

    private Consumer<String> onNavigateToInheritedListener;
    private Runnable onModifiedListener;

    public SettingsColorSchemeJavaPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(12, 16, 16, 16));
        setSpacing(12);

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
        categoryTree.getStyleClass().add("color-scheme-tree");
        categoryTree.setStyle(
                "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        categoryTree.setPrefWidth(380);
        categoryTree.setMinWidth(300);
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

        buildCategoryTree();

        // 3. Right Attribute Editor Box
        attributeEditorBox = new VBox(10);
        attributeEditorBox.setPadding(new Insets(8, 16, 16, 24));
        attributeEditorBox.setStyle("-fx-background-color: #1E1F22;");
        attributeEditorBox.setPrefWidth(380);

        // Font styles
        HBox fontBox = new HBox(16);
        fontBox.setAlignment(Pos.CENTER_LEFT);
        boldCheck = new CheckBox("Bold");
        boldCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        italicCheck = new CheckBox("Italic");
        italicCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        fontBox.getChildren().addAll(boldCheck, italicCheck);

        // Foreground
        HBox fgRow = new HBox(8);
        fgRow.setAlignment(Pos.CENTER_LEFT);
        foregroundCheck = new CheckBox("Foreground");
        foregroundCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        foregroundCheck.setPrefWidth(140);
        foregroundSwatch = createSwatchButton();
        fgRow.getChildren().addAll(foregroundCheck, foregroundSwatch);

        // Background
        HBox bgRow = new HBox(8);
        bgRow.setAlignment(Pos.CENTER_LEFT);
        backgroundCheck = new CheckBox("Background");
        backgroundCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        backgroundCheck.setPrefWidth(140);
        backgroundSwatch = createSwatchButton();
        bgRow.getChildren().addAll(backgroundCheck, backgroundSwatch);

        // Error stripe mark
        HBox esRow = new HBox(8);
        esRow.setAlignment(Pos.CENTER_LEFT);
        errorStripeCheck = new CheckBox("Error stripe mark");
        errorStripeCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        errorStripeCheck.setPrefWidth(140);
        errorStripeSwatch = createSwatchButton();
        esRow.getChildren().addAll(errorStripeCheck, errorStripeSwatch);

        // Effects
        HBox effRow = new HBox(8);
        effRow.setAlignment(Pos.CENTER_LEFT);
        effectsCheck = new CheckBox("Effects");
        effectsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        effectsCheck.setPrefWidth(140);
        effectsSwatch = createSwatchButton();
        effRow.getChildren().addAll(effectsCheck, effectsSwatch);

        effectTypeCombo = new ComboBox<>();
        effectTypeCombo.getItems().addAll(EffectType.values());
        effectTypeCombo.setValue(EffectType.BORDERED);
        effectTypeCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 12px; -fx-pref-width: 140;"
        );

        // Inheritance controls
        inheritBox = new VBox(4);
        inheritBox.setPadding(new Insets(12, 0, 0, 0));
        inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        inheritCheck.setSelected(true);

        inheritTargetLabel = new Label("Metadata");
        inheritTargetLabel.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 0 0 20;");
        inheritTargetLabel.setOnMouseEntered(e -> inheritTargetLabel.setStyle("-fx-text-fill: #70B4F7; -fx-underline: true; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 0 0 20;"));
        inheritTargetLabel.setOnMouseExited(e -> inheritTargetLabel.setStyle("-fx-text-fill: #56A8F5; -fx-underline: false; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 0 0 20;"));
        inheritTargetLabel.setOnMouseClicked(e -> {
            if (onNavigateToInheritedListener != null && currentInheritedTargetKey != null) {
                onNavigateToInheritedListener.accept(currentInheritedTargetKey);
            }
        });

        inheritScopeLabel = new Label("(Language Defaults)");
        inheritScopeLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 0 0 0 20;");

        inheritBox.getChildren().addAll(inheritCheck, inheritTargetLabel, inheritScopeLabel);

        attributeEditorBox.getChildren().addAll(
                fontBox,
                fgRow,
                bgRow,
                esRow,
                effRow,
                effectTypeCombo,
                inheritBox
        );

        // Top horizontal split: Tree (left) + Attribute Editor (right)
        HBox topPane = new HBox(16);
        topPane.setPrefHeight(270);
        topPane.setMinHeight(220);
        HBox.setHgrow(categoryTree, Priority.ALWAYS);
        topPane.getChildren().addAll(categoryTree, attributeEditorBox);

        // 4. Live Java Code Preview Pane
        codeLinesBox = new VBox(2);
        codeLinesBox.setPadding(new Insets(8, 12, 12, 12));
        codeLinesBox.setStyle("-fx-background-color: #1E1F22;");

        StackPane editorArea = new StackPane();
        editorArea.setAlignment(Pos.TOP_LEFT);

        // Right margin guideline at ~540px
        Line rightMargin = new Line(540, 0, 540, 480);
        rightMargin.setStroke(Color.web("#2B2D30"));
        rightMargin.setStrokeWidth(1);

        editorArea.getChildren().addAll(rightMargin, codeLinesBox);

        previewScrollPane = new ScrollPane(editorArea);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: transparent; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        // Error stripe gutter (far right margin bar)
        VBox errorStripeGutter = new VBox(2);
        errorStripeGutter.setPrefWidth(12);
        errorStripeGutter.setMinWidth(12);
        errorStripeGutter.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent transparent #2B2D30;");
        errorStripeGutter.setPadding(new Insets(140, 2, 0, 2));

        Rectangle stripe1 = new Rectangle(8, 2, Color.web("#F75464"));
        Rectangle stripe2 = new Rectangle(8, 2, Color.web("#E0A82E"));
        Rectangle stripe3 = new Rectangle(8, 2, Color.web("#F75464"));
        errorStripeGutter.getChildren().addAll(stripe1, stripe2, stripe3);

        HBox previewWithGutter = new HBox();
        HBox.setHgrow(previewScrollPane, Priority.ALWAYS);
        previewWithGutter.getChildren().addAll(previewScrollPane, errorStripeGutter);
        VBox.setVgrow(previewWithGutter, Priority.ALWAYS);

        getChildren().addAll(headerBar, topPane, previewWithGutter);

        initListeners();
        selectDefaultKey();
        updatePreview();
    }

    private Button createSwatchButton() {
        Button btn = new Button();
        btn.setPrefSize(42, 22);
        btn.setMinSize(42, 22);
        btn.setMaxSize(42, 22);
        btn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");
        return btn;
    }

    private void buildCategoryTree() {
        TreeItem<String> root = new TreeItem<>("Root");

        // Annotations
        TreeItem<String> annotNode = new TreeItem<>("Annotations");
        annotNode.setExpanded(true);
        annotNode.getChildren().addAll(
                new TreeItem<>("Annotation attribute name"),
                new TreeItem<>("Annotation name")
        );

        // Braces and Operators
        TreeItem<String> bracesNode = new TreeItem<>("Braces and Operators");
        bracesNode.getChildren().addAll(
                new TreeItem<>("Braces"),
                new TreeItem<>("Brackets"),
                new TreeItem<>("Comma"),
                new TreeItem<>("Dot"),
                new TreeItem<>("Operator sign"),
                new TreeItem<>("Parentheses"),
                new TreeItem<>("Semicolon")
        );

        // Class Fields
        TreeItem<String> fieldsNode = new TreeItem<>("Class Fields");
        fieldsNode.getChildren().addAll(
                new TreeItem<>("Constant (static final field)"),
                new TreeItem<>("Constant (static final imported field)"),
                new TreeItem<>("Instance field"),
                new TreeItem<>("Instance final field"),
                new TreeItem<>("Record component"),
                new TreeItem<>("Static field"),
                new TreeItem<>("Static imported field")
        );

        // Classes and Interfaces
        TreeItem<String> classesNode = new TreeItem<>("Classes and Interfaces");
        classesNode.getChildren().addAll(
                new TreeItem<>("Abstract class"),
                new TreeItem<>("Anonymous class"),
                new TreeItem<>("Class"),
                new TreeItem<>("Enum"),
                new TreeItem<>("Interface"),
                new TreeItem<>("Record"),
                new TreeItem<>("Type parameter")
        );

        // Comments
        TreeItem<String> commentsNode = new TreeItem<>("Comments");
        TreeItem<String> javaDocNode = new TreeItem<>("JavaDoc");
        javaDocNode.getChildren().addAll(
                new TreeItem<>("Markup"),
                new TreeItem<>("Tag"),
                new TreeItem<>("Tag value"),
                new TreeItem<>("Text")
        );
        commentsNode.getChildren().addAll(
                new TreeItem<>("Block comment"),
                javaDocNode,
                new TreeItem<>("Line comment")
        );

        // Keyword
        TreeItem<String> keywordNode = new TreeItem<>("Keyword");

        // Methods
        TreeItem<String> methodsNode = new TreeItem<>("Methods");
        methodsNode.getChildren().addAll(
                new TreeItem<>("Constructor call"),
                new TreeItem<>("Constructor declaration"),
                new TreeItem<>("Instance method call"),
                new TreeItem<>("Instance method declaration"),
                new TreeItem<>("Static method call"),
                new TreeItem<>("Static method declaration")
        );

        // Number
        TreeItem<String> numberNode = new TreeItem<>("Number");

        // Parameters
        TreeItem<String> paramsNode = new TreeItem<>("Parameters");
        paramsNode.getChildren().addAll(
                new TreeItem<>("Implicit anonymous class parameter"),
                new TreeItem<>("Method call arguments"),
                new TreeItem<>("Parameter")
        );

        // Semantic highlighting
        TreeItem<String> semanticNode = new TreeItem<>("Semantic highlighting");

        // String
        TreeItem<String> stringNode = new TreeItem<>("String");
        TreeItem<String> escapeNode = new TreeItem<>("Escape sequence");
        escapeNode.getChildren().addAll(
                new TreeItem<>("Invalid"),
                new TreeItem<>("Valid")
        );
        stringNode.getChildren().addAll(
                escapeNode,
                new TreeItem<>("String text")
        );

        // Variables
        TreeItem<String> varsNode = new TreeItem<>("Variables");
        varsNode.getChildren().addAll(
                new TreeItem<>("Implicit parameter"),
                new TreeItem<>("Local variable"),
                new TreeItem<>("Reassigned local variable"),
                new TreeItem<>("Reassigned parameter")
        );

        // Visibility
        TreeItem<String> visNode = new TreeItem<>("Visibility");
        visNode.getChildren().addAll(
                new TreeItem<>("Package private"),
                new TreeItem<>("Private"),
                new TreeItem<>("Protected"),
                new TreeItem<>("Public")
        );

        root.getChildren().addAll(
                annotNode,
                bracesNode,
                fieldsNode,
                classesNode,
                commentsNode,
                keywordNode,
                methodsNode,
                numberNode,
                paramsNode,
                semanticNode,
                stringNode,
                varsNode,
                visNode
        );

        categoryTree.setRoot(root);
    }

    private void selectDefaultKey() {
        selectTreeItem("Annotation name");
    }

    public void selectTreeItem(String key) {
        if (key == null || categoryTree.getRoot() == null) return;
        findAndSelect(categoryTree.getRoot(), key);
    }

    private boolean findAndSelect(TreeItem<String> item, String key) {
        if (key.equals(item.getValue())) {
            expandAncestors(item);
            categoryTree.getSelectionModel().select(item);
            return true;
        }
        for (TreeItem<String> child : item.getChildren()) {
            if (findAndSelect(child, key)) {
                return true;
            }
        }
        return false;
    }

    private void expandAncestors(TreeItem<String> item) {
        TreeItem<String> p = item.getParent();
        while (p != null) {
            p.setExpanded(true);
            p = p.getParent();
        }
    }

    private void initListeners() {
        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isLeaf()) {
                selectedKey = newVal.getValue();
                loadAttributesForSelectedKey();
                updatePreview();
            }
        });

        boldCheck.setOnAction(e -> onAttributeControlChanged());
        italicCheck.setOnAction(e -> onAttributeControlChanged());

        foregroundCheck.setOnAction(e -> {
            if (foregroundCheck.isSelected() && foregroundHex == null) {
                foregroundHex = "#BCBEC4";
            }
            onAttributeControlChanged();
        });
        foregroundSwatch.setOnAction(e -> openColorPicker("Foreground", foregroundHex, color -> {
            foregroundHex = color;
            foregroundCheck.setSelected(true);
            onAttributeControlChanged();
        }));

        backgroundCheck.setOnAction(e -> {
            if (backgroundCheck.isSelected() && backgroundHex == null) {
                backgroundHex = "#2B2D30";
            }
            onAttributeControlChanged();
        });
        backgroundSwatch.setOnAction(e -> openColorPicker("Background", backgroundHex, color -> {
            backgroundHex = color;
            backgroundCheck.setSelected(true);
            onAttributeControlChanged();
        }));

        errorStripeCheck.setOnAction(e -> {
            if (errorStripeCheck.isSelected() && errorStripeHex == null) {
                errorStripeHex = "#436980";
            }
            onAttributeControlChanged();
        });
        errorStripeSwatch.setOnAction(e -> openColorPicker("Error Stripe Mark", errorStripeHex, color -> {
            errorStripeHex = color;
            errorStripeCheck.setSelected(true);
            onAttributeControlChanged();
        }));

        effectsCheck.setOnAction(e -> {
            if (effectsCheck.isSelected() && effectsHex == null) {
                effectsHex = "#3574F0";
            }
            onAttributeControlChanged();
        });
        effectsSwatch.setOnAction(e -> openColorPicker("Effects Color", effectsHex, color -> {
            effectsHex = color;
            effectsCheck.setSelected(true);
            onAttributeControlChanged();
        }));
        effectTypeCombo.setOnAction(e -> onAttributeControlChanged());

        inheritCheck.setOnAction(e -> {
            if (!inheritCheck.isSelected()) {
                // detaches from inheritance
                modified = true;
                notifyModified();
            } else {
                loadAttributesForSelectedKey();
                modified = true;
                notifyModified();
            }
        });
    }

    private void loadAttributesForSelectedKey() {
        suppressEvents = true;
        try {
            EditorColorSchemeSettings settings = EditorColorSchemeSettings.getInstance();
            ColorAttribute attr = settings.getAttribute(headerBar.getSelectedScheme(), selectedKey);
            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);

            if (attr == null && desc != null) {
                attr = desc.getDefaultAttribute();
            }

            // Update inheritance UI
            boolean hasInherit = desc != null && desc.hasInheritance();
            inheritBox.setVisible(hasInherit);
            inheritBox.setManaged(hasInherit);

            if (hasInherit) {
                currentInheritedTargetKey = desc.getInheritFrom();
                currentInheritedScope = desc.getInheritScope() != null ? desc.getInheritScope() : "(Language Defaults)";
                inheritCheck.setSelected(attr == null || attr.isInherit());
                inheritTargetLabel.setText(currentInheritedTargetKey);
                inheritScopeLabel.setText(currentInheritedScope);
            }

            boldCheck.setSelected(attr != null && attr.isBold());
            italicCheck.setSelected(attr != null && attr.isItalic());

            // Foreground
            boolean hasFg = attr != null && attr.getForeground() != null && !attr.getForeground().isBlank();
            foregroundCheck.setSelected(hasFg);
            foregroundHex = hasFg ? attr.getForeground() : null;
            updateSwatch(foregroundSwatch, foregroundHex);

            // Background
            boolean hasBg = attr != null && attr.getBackground() != null && !attr.getBackground().isBlank();
            backgroundCheck.setSelected(hasBg);
            backgroundHex = hasBg ? attr.getBackground() : null;
            updateSwatch(backgroundSwatch, backgroundHex);

            // Error stripe
            boolean hasStripe = attr != null && attr.getErrorStripeColor() != null && !attr.getErrorStripeColor().isBlank();
            errorStripeCheck.setSelected(hasStripe);
            errorStripeHex = hasStripe ? attr.getErrorStripeColor() : null;
            updateSwatch(errorStripeSwatch, errorStripeHex);

            // Effects
            boolean hasEff = attr != null && attr.getEffectColor() != null && !attr.getEffectColor().isBlank() && attr.getEffectType() != EffectType.NONE;
            effectsCheck.setSelected(hasEff);
            effectsHex = hasEff ? attr.getEffectColor() : null;
            updateSwatch(effectsSwatch, effectsHex);
            effectTypeCombo.setValue(attr != null && attr.getEffectType() != EffectType.NONE ? attr.getEffectType() : EffectType.BORDERED);

        } finally {
            suppressEvents = false;
        }
    }

    private void updateSwatch(Button swatch, String hex) {
        if (hex != null && !hex.isBlank()) {
            String cleanHex = hex.startsWith("#") ? hex : "#" + hex;
            swatch.setStyle("-fx-background-color: " + cleanHex + "; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");
            swatch.setText(cleanHex.substring(1).toUpperCase());
            swatch.setTextFill(isBright(cleanHex) ? Color.BLACK : Color.WHITE);
            swatch.setFont(Font.font("Monospaced", 9));
        } else {
            swatch.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");
            swatch.setText("");
        }
    }

    private boolean isBright(String hex) {
        try {
            Color c = Color.web(hex);
            return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) > 0.6;
        } catch (Exception e) {
            return false;
        }
    }

    private void onAttributeControlChanged() {
        if (suppressEvents) return;
        updateSwatch(foregroundSwatch, foregroundCheck.isSelected() ? foregroundHex : null);
        updateSwatch(backgroundSwatch, backgroundCheck.isSelected() ? backgroundHex : null);
        updateSwatch(errorStripeSwatch, errorStripeCheck.isSelected() ? errorStripeHex : null);
        updateSwatch(effectsSwatch, effectsCheck.isSelected() ? effectsHex : null);

        modified = true;
        notifyModified();
        updatePreview();
    }

    private void openColorPicker(String title, String initialColor, java.util.function.Consumer<String> onSelected) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        ColorPicker picker = new ColorPicker();
        if (initialColor != null && !initialColor.isBlank()) {
            try {
                picker.setValue(Color.web(initialColor.startsWith("#") ? initialColor : "#" + initialColor));
            } catch (Exception ignored) {}
        }
        VBox content = new VBox(10, new Label("Choose color:"), picker);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && picker.getValue() != null) {
                Color c = picker.getValue();
                return String.format("#%02X%02X%02X",
                        (int) Math.round(c.getRed() * 255),
                        (int) Math.round(c.getGreen() * 255),
                        (int) Math.round(c.getBlue() * 255));
            }
            return null;
        });
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(onSelected);
    }

    // -------------------------------------------------------------------------
    // Preview Rendering & Token Selection Box
    // -------------------------------------------------------------------------

    private static class JavaToken {
        final String text;
        final String categoryKey;
        final String defaultColor;
        final boolean isItalic;
        final boolean isUnderlined;

        JavaToken(String text, String categoryKey, String defaultColor, boolean isItalic, boolean isUnderlined) {
            this.text = text;
            this.categoryKey = categoryKey;
            this.defaultColor = defaultColor;
            this.isItalic = isItalic;
            this.isUnderlined = isUnderlined;
        }

        JavaToken(String text, String categoryKey, String defaultColor) {
            this(text, categoryKey, defaultColor, false, false);
        }
    }

    private void updatePreview() {
        codeLinesBox.getChildren().clear();

        EditorColorSchemeSettings settings = EditorColorSchemeSettings.getInstance();
        String scheme = headerBar.getSelectedScheme();

        // 19 Preview lines matching screenshots 2 through 5
        List<List<JavaToken>> lines = List.of(
                // Line 1: · · reassignedValue ++;
                List.of(
                        new JavaToken("· · ", null, "#393B40"),
                        new JavaToken("reassignedValue", "Reassigned local variable", "#BCBEC4", false, true),
                        new JavaToken(" ", null, "#BCBEC4"),
                        new JavaToken("++", "Operator sign", "#BCBEC4"),
                        new JavaToken(";", "Semicolon", "#BCBEC4")
                ),
                // Line 2: · · field.run();
                List.of(
                        new JavaToken("· · ", null, "#393B40"),
                        new JavaToken("field", "Instance field", "#C77DBB"),
                        new JavaToken(".", "Dot", "#BCBEC4"),
                        new JavaToken("run", "Instance method call", "#56A8F5"),
                        new JavaToken("();", "Parentheses", "#BCBEC4")
                ),
                // Line 3: · · new SomeClass() {
                List.of(
                        new JavaToken("· · ", null, "#393B40"),
                        new JavaToken("new ", "Keyword", "#CF8E6D"),
                        new JavaToken("SomeClass", "Class", "#BCBEC4"),
                        new JavaToken("() ", "Parentheses", "#BCBEC4"),
                        new JavaToken("{", "Braces", "#BCBEC4")
                ),
                // Line 4: · · · · {
                List.of(
                        new JavaToken("· · · · ", null, "#393B40"),
                        new JavaToken("{", "Braces", "#BCBEC4")
                ),
                // Line 5: · · · · · · int a = localVar;
                List.of(
                        new JavaToken("· · · · · · ", null, "#393B40"),
                        new JavaToken("int ", "Keyword", "#CF8E6D"),
                        new JavaToken("a", "Local variable", "#BCBEC4"),
                        new JavaToken(" = ", "Operator sign", "#BCBEC4"),
                        new JavaToken("localVar", "Local variable", "#BCBEC4"),
                        new JavaToken(";", "Semicolon", "#BCBEC4")
                ),
                // Line 6: · · · · }
                List.of(
                        new JavaToken("· · · · ", null, "#393B40"),
                        new JavaToken("}", "Braces", "#BCBEC4")
                ),
                // Line 7: · · };
                List.of(
                        new JavaToken("· · ", null, "#393B40"),
                        new JavaToken("};", "Braces", "#BCBEC4")
                ),
                // Line 8: · · int[] l = new ArrayList<String>().toArray(new int[CONSTANT]);
                List.of(
                        new JavaToken("· · ", null, "#393B40"),
                        new JavaToken("int", "Keyword", "#CF8E6D"),
                        new JavaToken("[", "Brackets", "#BCBEC4"),
                        new JavaToken("] ", "Brackets", "#BCBEC4"),
                        new JavaToken("l", "Local variable", "#BCBEC4"),
                        new JavaToken(" = ", "Operator sign", "#BCBEC4"),
                        new JavaToken("new ", "Keyword", "#CF8E6D"),
                        new JavaToken("ArrayList", "Class", "#BCBEC4"),
                        new JavaToken("<", "Operator sign", "#BCBEC4"),
                        new JavaToken("String", "Class", "#BCBEC4"),
                        new JavaToken(">().", "Parentheses", "#BCBEC4"),
                        new JavaToken("toArray", "Instance method call", "#56A8F5"),
                        new JavaToken("(", "Parentheses", "#BCBEC4"),
                        new JavaToken("new ", "Keyword", "#CF8E6D"),
                        new JavaToken("int", "Keyword", "#CF8E6D"),
                        new JavaToken("[", "Brackets", "#BCBEC4"),
                        new JavaToken("CONSTANT", "Constant (static final field)", "#C77DBB", true, false),
                        new JavaToken("]", "Brackets", "#BCBEC4"),
                        new JavaToken(");", "Parentheses", "#BCBEC4")
                ),
                // Line 9: }
                List.of(
                        new JavaToken("}", "Braces", "#BCBEC4")
                ),
                // Line 10: enum AnEnum { CONST1, CONST2 }
                List.of(
                        new JavaToken("enum ", "Keyword", "#CF8E6D"),
                        new JavaToken("AnEnum", "Enum", "#BCBEC4"),
                        new JavaToken(" { ", "Braces", "#BCBEC4"),
                        new JavaToken("CONST1", "Constant (static final field)", "#C77DBB", true, false),
                        new JavaToken(", ", "Comma", "#BCBEC4"),
                        new JavaToken("CONST2", "Constant (static final field)", "#C77DBB", true, false),
                        new JavaToken(" }", "Braces", "#BCBEC4")
                ),
                // Line 11: interface AnInterface {
                List.of(
                        new JavaToken("interface ", "Keyword", "#CF8E6D"),
                        new JavaToken("AnInterface", "Interface", "#BCBEC4"),
                        new JavaToken(" {", "Braces", "#BCBEC4")
                ),
                // Line 12: · · int CONSTANT = 2;
                List.of(
                        new JavaToken("· · ", null, "#393B40"),
                        new JavaToken("int ", "Keyword", "#CF8E6D"),
                        new JavaToken("CONSTANT", "Constant (static final field)", "#C77DBB", true, false),
                        new JavaToken(" = ", "Operator sign", "#BCBEC4"),
                        new JavaToken("2", "Number", "#2AACB8"),
                        new JavaToken(";", "Semicolon", "#BCBEC4")
                ),
                // Line 13: · · void method();
                List.of(
                        new JavaToken("· · ", null, "#393B40"),
                        new JavaToken("void ", "Keyword", "#CF8E6D"),
                        new JavaToken("method", "Instance method declaration", "#56A8F5"),
                        new JavaToken("();", "Parentheses", "#BCBEC4")
                ),
                // Line 14: }
                List.of(
                        new JavaToken("}", "Braces", "#BCBEC4")
                ),
                // Line 15: @interface AnnotationType {}
                List.of(
                        new JavaToken("@interface ", "Keyword", "#CF8E6D"),
                        new JavaToken("AnnotationType", "Annotation name", "#B3AF60"),
                        new JavaToken(" {}", "Braces", "#BCBEC4")
                ),
                // Line 16: record Point(int x, int y) {}
                List.of(
                        new JavaToken("record ", "Keyword", "#CF8E6D"),
                        new JavaToken("Point", "Record", "#BCBEC4"),
                        new JavaToken("(", "Parentheses", "#BCBEC4"),
                        new JavaToken("int ", "Keyword", "#CF8E6D"),
                        new JavaToken("x", "Record component", "#C77DBB"),
                        new JavaToken(", ", "Comma", "#BCBEC4"),
                        new JavaToken("int ", "Keyword", "#CF8E6D"),
                        new JavaToken("y", "Record component", "#C77DBB"),
                        new JavaToken(") {}", "Braces", "#BCBEC4")
                ),
                // Line 17: abstract class SomeAbstractClass {
                List.of(
                        new JavaToken("abstract ", "Keyword", "#CF8E6D"),
                        new JavaToken("class ", "Keyword", "#CF8E6D"),
                        new JavaToken("SomeAbstractClass", "Abstract class", "#BCBEC4"),
                        new JavaToken(" {", "Braces", "#BCBEC4")
                ),
                // Line 18: · · protected int instanceField = staticField;
                List.of(
                        new JavaToken("· · ", null, "#393B40"),
                        new JavaToken("protected ", "Keyword", "#CF8E6D"),
                        new JavaToken("int ", "Keyword", "#CF8E6D"),
                        new JavaToken("instanceField", "Instance field", "#C77DBB"),
                        new JavaToken(" = ", "Operator sign", "#BCBEC4"),
                        new JavaToken("staticField", "Static field", "#C77DBB", true, false),
                        new JavaToken(";", "Semicolon", "#BCBEC4")
                ),
                // Line 19: }
                List.of(
                        new JavaToken("}", "Braces", "#BCBEC4")
                )
        );

        for (List<JavaToken> line : lines) {
            HBox lineBox = new HBox();
            lineBox.setAlignment(Pos.CENTER_LEFT);

            for (JavaToken tok : line) {
                Label lbl = new Label(tok.text);
                lbl.setFont(Font.font("Monospaced", 12));

                String color = tok.defaultColor;
                boolean isBold = false;
                boolean isItalic = tok.isItalic;
                boolean isUnderlined = tok.isUnderlined;

                if (tok.categoryKey != null) {
                    ColorAttribute a = settings.getAttribute(scheme, tok.categoryKey);
                    if (a != null) {
                        if (a.getForeground() != null && !a.getForeground().isBlank()) {
                            color = a.getForeground();
                        }
                        isBold = a.isBold();
                        isItalic = a.isItalic() || tok.isItalic;
                        if (a.getEffectType() == EffectType.UNDERSCORED) {
                            isUnderlined = true;
                        }
                    }
                }

                StringBuilder style = new StringBuilder();
                style.append("-fx-text-fill: ").append(color).append(";");
                if (isBold) style.append(" -fx-font-weight: bold;");
                if (isItalic) style.append(" -fx-font-style: italic;");
                if (isUnderlined) style.append(" -fx-underline: true;");

                // Highlight box for active category match (Screenshots 2-5)
                if (tok.categoryKey != null && tok.categoryKey.equals(selectedKey)) {
                    style.append(" -fx-border-color: #5E626B; -fx-border-radius: 2; -fx-padding: 0 1 0 1; -fx-background-color: #26282E;");
                }

                if (tok.categoryKey != null) {
                    style.append(" -fx-cursor: hand;");
                    lbl.setOnMouseClicked(e -> selectTreeItem(tok.categoryKey));
                }

                lbl.setStyle(style.toString());
                lineBox.getChildren().add(lbl);
            }
            codeLinesBox.getChildren().add(lineBox);
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle & Public API
    // -------------------------------------------------------------------------

    public void apply() {
        if (!modified) return;
        EditorColorSchemeSettings settings = EditorColorSchemeSettings.getInstance();
        String scheme = headerBar.getSelectedScheme();

        ColorAttribute attr = new ColorAttribute();
        attr.setBold(boldCheck.isSelected());
        attr.setItalic(italicCheck.isSelected());
        attr.setInherit(inheritCheck.isSelected());

        if (foregroundCheck.isSelected() && foregroundHex != null) {
            attr.setForeground(foregroundHex);
        }
        if (backgroundCheck.isSelected() && backgroundHex != null) {
            attr.setBackground(backgroundHex);
        }
        if (errorStripeCheck.isSelected() && errorStripeHex != null) {
            attr.setErrorStripeColor(errorStripeHex);
        }
        if (effectsCheck.isSelected() && effectsHex != null) {
            attr.setEffectColor(effectsHex);
            attr.setEffectType(effectTypeCombo.getValue() != null ? effectTypeCombo.getValue() : EffectType.BORDERED);
        } else {
            attr.setEffectType(EffectType.NONE);
        }

        settings.setAttribute(scheme, selectedKey, attr);
        modified = false;
        notifyModified();
    }

    public void reset() {
        loadAttributesForSelectedKey();
        updatePreview();
        modified = false;
        notifyModified();
    }

    public boolean isModified() {
        return modified;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnNavigateToInheritedListener(Consumer<String> listener) {
        this.onNavigateToInheritedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Getters for tests
    public ColorSchemeHeaderBar getHeaderBar() { return headerBar; }
    public TreeView<String> getCategoryTree() { return categoryTree; }
    public VBox getAttributeEditorBox() { return attributeEditorBox; }
    public CheckBox getBoldCheck() { return boldCheck; }
    public CheckBox getItalicCheck() { return italicCheck; }
    public CheckBox getForegroundCheck() { return foregroundCheck; }
    public Button getForegroundSwatch() { return foregroundSwatch; }
    public CheckBox getBackgroundCheck() { return backgroundCheck; }
    public Button getBackgroundSwatch() { return backgroundSwatch; }
    public CheckBox getErrorStripeCheck() { return errorStripeCheck; }
    public Button getErrorStripeSwatch() { return errorStripeSwatch; }
    public CheckBox getEffectsCheck() { return effectsCheck; }
    public Button getEffectsSwatch() { return effectsSwatch; }
    public ComboBox<EffectType> getEffectTypeCombo() { return effectTypeCombo; }
    public CheckBox getInheritCheck() { return inheritCheck; }
    public VBox getInheritBox() { return inheritBox; }
    public Label getInheritTargetLabel() { return inheritTargetLabel; }
    public Label getInheritScopeLabel() { return inheritScopeLabel; }
    public VBox getCodeLinesBox() { return codeLinesBox; }
}
