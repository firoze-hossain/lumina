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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;
import java.util.function.Consumer;

/**
 * Editor > Color Scheme > CSS settings page.
 * Replicates 1:1 visual and functional parity with reference screenshot media_1790427948503.png:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - Flat Category Tree view with 27 attributes:
 *   (At-keyword, Attribute name, Bad character, Braces, Brackets, Class name, Colon, Comma,
 *    Comment, Dot, Function, Hex color, Id selector, Identifier, Important, Number, Operation sign,
 *    Parenthesis, Property name, Property value, Pseudo selector, Semicolon, String, Tag name,
 *    Unicode range, Unit, URL).
 * - Default selection: "Attribute name" (foreground #D5B778, inherits Identifier from (CSS)).
 * - Full Attribute Editor panel with Bold, Italic, Foreground, Background, Error stripe mark,
 *   Effects (dropdown), and "Inherit values from" with interactive jump hyperlink.
 * - Live Interactive CSS Code Editor Preview with syntax tokens, token selection box,
 *   right margin guideline, and bidirectional click synchronization.
 */
public class SettingsColorSchemeCssPage extends VBox {

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
    private String selectedKey = "Attribute name";
    private String currentInheritedTargetKey = "Identifier";
    private String currentInheritedScope = "(CSS)";
    private String foregroundHex = "#D5B778";
    private String backgroundHex = null;
    private String errorStripeHex = null;
    private String effectsHex = null;
    private boolean suppressEvents = false;
    private boolean modified = false;

    private Consumer<String> onNavigateToInheritedListener;
    private Runnable onModifiedListener;

    public SettingsColorSchemeCssPage() {
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

        inheritTargetLabel = new Label("Identifier");
        inheritTargetLabel.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 0 0 20;");
        inheritTargetLabel.setOnMouseEntered(e -> inheritTargetLabel.setStyle("-fx-text-fill: #70B4F7; -fx-underline: true; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 0 0 20;"));
        inheritTargetLabel.setOnMouseExited(e -> inheritTargetLabel.setStyle("-fx-text-fill: #56A8F5; -fx-underline: false; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 0 0 20;"));
        inheritTargetLabel.setOnMouseClicked(e -> {
            if ("Identifier".equals(currentInheritedTargetKey)) {
                selectTreeItem("Identifier");
            } else if (onNavigateToInheritedListener != null && currentInheritedTargetKey != null) {
                onNavigateToInheritedListener.accept(currentInheritedTargetKey);
            }
        });

        inheritScopeLabel = new Label("(CSS)");
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

        // 4. Live CSS Code Preview Pane
        codeLinesBox = new VBox(2);
        codeLinesBox.setPadding(new Insets(8, 12, 12, 12));
        codeLinesBox.setStyle("-fx-background-color: #1E1F22;");

        StackPane editorArea = new StackPane();
        editorArea.setAlignment(Pos.TOP_LEFT);

        Line rightMargin = new Line(540, 0, 540, 480);
        rightMargin.setStroke(Color.web("#2B2D30"));
        rightMargin.setStrokeWidth(1);

        editorArea.getChildren().addAll(rightMargin, codeLinesBox);

        previewScrollPane = new ScrollPane(editorArea);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: transparent; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        getChildren().addAll(headerBar, topPane, previewScrollPane);

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

        // 27 flat attributes matching media_1790427948503.png
        root.getChildren().addAll(
                new TreeItem<>("At-keyword"),
                new TreeItem<>("Attribute name"),
                new TreeItem<>("Bad character"),
                new TreeItem<>("Braces"),
                new TreeItem<>("Brackets"),
                new TreeItem<>("Class name"),
                new TreeItem<>("Colon"),
                new TreeItem<>("Comma"),
                new TreeItem<>("Comment"),
                new TreeItem<>("Dot"),
                new TreeItem<>("Function"),
                new TreeItem<>("Hex color"),
                new TreeItem<>("Id selector"),
                new TreeItem<>("Identifier"),
                new TreeItem<>("Important"),
                new TreeItem<>("Number"),
                new TreeItem<>("Operation sign"),
                new TreeItem<>("Parenthesis"),
                new TreeItem<>("Property name"),
                new TreeItem<>("Property value"),
                new TreeItem<>("Pseudo selector"),
                new TreeItem<>("Semicolon"),
                new TreeItem<>("String"),
                new TreeItem<>("Tag name"),
                new TreeItem<>("Unicode range"),
                new TreeItem<>("Unit"),
                new TreeItem<>("URL")
        );

        categoryTree.setRoot(root);
    }

    private void selectDefaultKey() {
        selectTreeItem("Attribute name");
    }

    public void selectTreeItem(String keyName) {
        if (keyName == null || categoryTree.getRoot() == null) return;
        for (TreeItem<String> child : categoryTree.getRoot().getChildren()) {
            if (child.getValue().equals(keyName)) {
                categoryTree.getSelectionModel().select(child);
                categoryTree.scrollTo(categoryTree.getRow(child));
                return;
            }
        }
    }

    private void initListeners() {
        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                selectedKey = newVal.getValue();
                loadAttributesForSelectedKey();
                updatePreview();
            }
        });

        boldCheck.setOnAction(e -> onAttributeControlChanged());
        italicCheck.setOnAction(e -> onAttributeControlChanged());

        foregroundCheck.setOnAction(e -> {
            if (foregroundCheck.isSelected() && foregroundHex == null) {
                foregroundHex = "#D5B778";
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
            modified = true;
            notifyModified();
            if (inheritCheck.isSelected()) {
                loadAttributesForSelectedKey();
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

    private void openColorPicker(String title, String initialColor, Consumer<String> onSelected) {
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
    // Preview Rendering
    // -------------------------------------------------------------------------

    private static class Token {
        final String text;
        final String categoryKey;
        final String defaultColor;
        final boolean isUnderlined;

        Token(String text, String categoryKey, String defaultColor, boolean isUnderlined) {
            this.text = text;
            this.categoryKey = categoryKey;
            this.defaultColor = defaultColor;
            this.isUnderlined = isUnderlined;
        }

        Token(String text, String categoryKey, String defaultColor) {
            this(text, categoryKey, defaultColor, false);
        }
    }

    private void updatePreview() {
        codeLinesBox.getChildren().clear();
        EditorColorSchemeSettings settings = EditorColorSchemeSettings.getInstance();
        String scheme = headerBar.getSelectedScheme();

        // 6 preview lines matching media_1790427948503.png
        List<List<Token>> lines = List.of(
                List.of(
                        new Token("}", "Braces", "#BCBEC4")
                ),
                List.of(
                        new Token("", null, "#BCBEC4")
                ),
                List.of(
                        new Token("a", "Tag name", "#CF8E6D"),
                        new Token("[", "Brackets", "#BCBEC4"),
                        new Token("type", "Attribute name", "#D5B778"),
                        new Token("$=", "Operation sign", "#BCBEC4"),
                        new Token("\".pdf\"", "String", "#6AAB73"),
                        new Token("]", "Brackets", "#BCBEC4"),
                        new Token(":", "Colon", "#BCBEC4"),
                        new Token("after", "Pseudo selector", "#CF8E6D"),
                        new Token(" {", "Braces", "#BCBEC4")
                ),
                List.of(
                        new Token("  ", null, "#BCBEC4"),
                        new Token("content", "Property name", "#BCBEC4"),
                        new Token(":", "Colon", "#BCBEC4"),
                        new Token(" ", null, "#BCBEC4"),
                        new Token("' ('", "String", "#6AAB73"),
                        new Token(" ", null, "#BCBEC4"),
                        new Token("url", "Function", "#56A8F5"),
                        new Token("(", "Parenthesis", "#BCBEC4"),
                        new Token("pdf.svg", "URL", "#56A8F5", true),
                        new Token(")", "Parenthesis", "#BCBEC4"),
                        new Token(" ", null, "#BCBEC4"),
                        new Token("attr", "Function", "#56A8F5"),
                        new Token("(", "Parenthesis", "#BCBEC4"),
                        new Token("data-size", "Attribute name", "#D5B778"),
                        new Token(")", "Parenthesis", "#BCBEC4"),
                        new Token(" ", null, "#BCBEC4"),
                        new Token("')'", "String", "#6AAB73"),
                        new Token(";", "Semicolon", "#BCBEC4")
                ),
                List.of(
                        new Token("}", "Braces", "#BCBEC4")
                )
        );

        for (List<Token> lineTokens : lines) {
            HBox lineBox = new HBox();
            lineBox.setAlignment(Pos.CENTER_LEFT);
            lineBox.setMinHeight(20);

            if (lineTokens.isEmpty() || (lineTokens.size() == 1 && lineTokens.get(0).text.isEmpty())) {
                Label emptyLabel = new Label(" ");
                emptyLabel.setFont(Font.font("Monospaced", 13));
                lineBox.getChildren().add(emptyLabel);
                codeLinesBox.getChildren().add(lineBox);
                continue;
            }

            for (Token token : lineTokens) {
                Label label = new Label(token.text);
                label.setFont(Font.font("Monospaced", 13));

                String color = token.defaultColor;
                boolean isBold = false;
                boolean isItalic = false;
                String bgColor = null;
                boolean isSelectedToken = token.categoryKey != null && token.categoryKey.equals(selectedKey);

                if (token.categoryKey != null) {
                    ColorAttribute attr = isSelectedToken
                            ? getCurrentEditorAttribute()
                            : settings.getAttribute(scheme, token.categoryKey);

                    if (attr != null) {
                        if (attr.getForeground() != null && !attr.getForeground().isBlank()) {
                            color = attr.getForeground();
                        }
                        if (attr.getBackground() != null && !attr.getBackground().isBlank()) {
                            bgColor = attr.getBackground();
                        }
                        isBold = attr.isBold();
                        isItalic = attr.isItalic();
                    }
                }

                StringBuilder style = new StringBuilder();
                style.append("-fx-text-fill: ").append(color).append(";");
                if (token.isUnderlined) {
                    style.append(" -fx-underline: true;");
                }
                if (isBold && isItalic) {
                    label.setFont(Font.font("Monospaced", FontWeight.BOLD, javafx.scene.text.FontPosture.ITALIC, 13));
                } else if (isBold) {
                    label.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
                } else if (isItalic) {
                    label.setFont(Font.font("Monospaced", javafx.scene.text.FontPosture.ITALIC, 13));
                }

                if (bgColor != null) {
                    style.append(" -fx-background-color: ").append(bgColor).append(";");
                }

                if (isSelectedToken) {
                    style.append(" -fx-border-color: #3574F0; -fx-border-width: 1; -fx-padding: 0 1 0 1;");
                }

                if (token.categoryKey != null) {
                    label.setCursor(javafx.scene.Cursor.HAND);
                    label.setOnMouseClicked(e -> {
                        selectTreeItem(token.categoryKey);
                        e.consume();
                    });
                }

                label.setStyle(style.toString());
                lineBox.getChildren().add(label);
            }

            codeLinesBox.getChildren().add(lineBox);
        }
    }

    private ColorAttribute getCurrentEditorAttribute() {
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
        }
        return attr;
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
