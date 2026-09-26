package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.AttributesDescriptor;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.*;
import java.util.function.Consumer;

/**
 * Editor > Color Scheme > GitLab CI Expression settings page.
 * Replicates 1:1 visual and functional parity with reference screenshot media_1790430732315.png:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - Flat Category Tree view with 8 attributes:
 *     Bad character,
 *     Comparison operator (==, !=, =~, !~),
 *     Identifier,
 *     Logical operator (, ||),
 *     Parentheses,
 *     Regular expression,
 *     String,
 *     Variable prefix ($).
 * - Default selection: "Parentheses" (Foreground #BCBEC4, Inherit values from Braces and Operators->Parentheses).
 * - Attribute Editor panel with Bold, Italic, Foreground, Background, Error stripe mark, Effects,
 *   and "Inherit values from" checkbox + hyperlink.
 * - Live interactive preview pane with GitLab CI expression snippet and bidirectional token selection.
 */
public class SettingsColorSchemeGitLabCiExpressionPage extends VBox {

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

    // Inherit controls
    private final VBox inheritBox;
    private final CheckBox inheritCheck;
    private final Hyperlink inheritTargetLabel;
    private final Label inheritScopeLabel;

    // Preview
    private final ScrollPane previewScrollPane;
    private final VBox codeLinesBox;

    // State
    private String selectedKey = "Parentheses";
    private String foregroundHex = "#BCBEC4";
    private String backgroundHex = null;
    private String errorStripeHex = null;
    private String effectsHex = null;
    private String currentInheritedTargetKey = "Braces and Operators->Parentheses";
    private String currentInheritedScope = "(Language Defaults)";
    private boolean suppressEvents = false;
    private boolean modified = false;

    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInheritedListener;

    public SettingsColorSchemeGitLabCiExpressionPage() {
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
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-padding: 3 8 3 8;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8;");
                    }
                }
            }
        });

        buildCategoryTree();

        // 3. Attribute Editor Box
        attributeEditorBox = new VBox(10);
        attributeEditorBox.setPadding(new Insets(10, 14, 14, 14));
        attributeEditorBox.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        attributeEditorBox.setPrefWidth(360);
        attributeEditorBox.setMinWidth(320);

        HBox fontStyleRow = new HBox(16);
        fontStyleRow.setAlignment(Pos.CENTER_LEFT);
        boldCheck = new CheckBox("Bold");
        boldCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        italicCheck = new CheckBox("Italic");
        italicCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        fontStyleRow.getChildren().addAll(boldCheck, italicCheck);

        // Foreground
        HBox fgRow = new HBox(8);
        fgRow.setAlignment(Pos.CENTER_LEFT);
        foregroundCheck = new CheckBox("Foreground");
        foregroundCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        foregroundCheck.setPrefWidth(120);
        foregroundSwatch = createSwatchButton();
        Region fgSpacer = new Region();
        HBox.setHgrow(fgSpacer, Priority.ALWAYS);
        fgRow.getChildren().addAll(foregroundCheck, fgSpacer, foregroundSwatch);

        // Background
        HBox bgRow = new HBox(8);
        bgRow.setAlignment(Pos.CENTER_LEFT);
        backgroundCheck = new CheckBox("Background");
        backgroundCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        backgroundCheck.setPrefWidth(120);
        backgroundSwatch = createSwatchButton();
        Region bgSpacer = new Region();
        HBox.setHgrow(bgSpacer, Priority.ALWAYS);
        bgRow.getChildren().addAll(backgroundCheck, bgSpacer, backgroundSwatch);

        // Error stripe
        HBox esRow = new HBox(8);
        esRow.setAlignment(Pos.CENTER_LEFT);
        errorStripeCheck = new CheckBox("Error stripe mark");
        errorStripeCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        errorStripeCheck.setPrefWidth(130);
        errorStripeSwatch = createSwatchButton();
        Region esSpacer = new Region();
        HBox.setHgrow(esSpacer, Priority.ALWAYS);
        esRow.getChildren().addAll(errorStripeCheck, esSpacer, errorStripeSwatch);

        // Effects
        HBox effRow = new HBox(8);
        effRow.setAlignment(Pos.CENTER_LEFT);
        effectsCheck = new CheckBox("Effects");
        effectsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        effectsCheck.setPrefWidth(120);
        effectsSwatch = createSwatchButton();
        Region effSpacer = new Region();
        HBox.setHgrow(effSpacer, Priority.ALWAYS);
        effRow.getChildren().addAll(effectsCheck, effSpacer, effectsSwatch);

        effectTypeCombo = new ComboBox<>();
        effectTypeCombo.getItems().addAll(
                EffectType.UNDERSCORED,
                EffectType.BOLD_UNDERSCORED,
                EffectType.UNDERWAVED,
                EffectType.BORDERED,
                EffectType.STRIKEOUT,
                EffectType.DOTTED_LINE
        );
        effectTypeCombo.setValue(EffectType.BORDERED);
        effectTypeCombo.setPrefWidth(140);
        effectTypeCombo.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox effectTypeRow = new HBox(effectTypeCombo);
        effectTypeRow.setAlignment(Pos.CENTER_RIGHT);
        effectTypeRow.setPadding(new Insets(0, 0, 4, 0));

        // Inherit values from
        inheritBox = new VBox(4);
        inheritBox.setPadding(new Insets(10, 0, 0, 0));
        HBox inheritCheckRow = new HBox(8);
        inheritCheckRow.setAlignment(Pos.CENTER_LEFT);
        inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        inheritCheckRow.getChildren().add(inheritCheck);

        VBox inheritDetailsBox = new VBox(2);
        inheritDetailsBox.setPadding(new Insets(0, 0, 0, 22));

        inheritTargetLabel = new Hyperlink("Braces and Operators->Parentheses");
        inheritTargetLabel.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0;");
        inheritTargetLabel.setOnAction(e -> {
            if (onNavigateToInheritedListener != null && currentInheritedTargetKey != null) {
                onNavigateToInheritedListener.accept(currentInheritedTargetKey);
            }
        });

        inheritScopeLabel = new Label("(Language Defaults)");
        inheritScopeLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px;");
        inheritDetailsBox.getChildren().addAll(inheritTargetLabel, inheritScopeLabel);
        inheritBox.getChildren().addAll(inheritCheckRow, inheritDetailsBox);

        attributeEditorBox.getChildren().addAll(
                fontStyleRow,
                fgRow,
                bgRow,
                esRow,
                effRow,
                effectTypeRow,
                inheritBox
        );

        // Split container for tree and editor
        HBox topPane = new HBox(12);
        topPane.getChildren().addAll(categoryTree, attributeEditorBox);
        HBox.setHgrow(categoryTree, Priority.ALWAYS);

        // 4. Preview Pane
        codeLinesBox = new VBox(2);
        codeLinesBox.setPadding(new Insets(8, 12, 8, 12));
        codeLinesBox.setStyle("-fx-background-color: #1E1F22;");

        previewScrollPane = new ScrollPane(codeLinesBox);
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

        // 8 flat attributes matching media_1790430732315.png
        root.getChildren().addAll(
                new TreeItem<>("Bad character"),
                new TreeItem<>("Comparison operator (==, !=, =~, !~)"),
                new TreeItem<>("Identifier"),
                new TreeItem<>("Logical operator (, ||)"),
                new TreeItem<>("Parentheses"),
                new TreeItem<>("Regular expression"),
                new TreeItem<>("String"),
                new TreeItem<>("Variable prefix ($)")
        );

        categoryTree.setRoot(root);
    }

    private void selectDefaultKey() {
        selectTreeItem("Parentheses");
    }

    public void selectTreeItem(String keyName) {
        if (keyName == null || categoryTree.getRoot() == null) return;
        for (TreeItem<String> child : categoryTree.getRoot().getChildren()) {
            if (child.getValue().equals(keyName)) {
                categoryTree.getSelectionModel().select(child);
                return;
            }
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
                errorStripeHex = "#FF6B68";
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
                effectsHex = foregroundHex != null ? foregroundHex : "#BCBEC4";
            }
            onAttributeControlChanged();
        });
        effectsSwatch.setOnAction(e -> openColorPicker("Effects Color", effectsHex, color -> {
            effectsHex = color;
            effectsCheck.setSelected(true);
            onAttributeControlChanged();
        }));

        effectTypeCombo.setOnAction(e -> {
            if (!suppressEvents) {
                effectsCheck.setSelected(true);
                onAttributeControlChanged();
            }
        });

        inheritCheck.setOnAction(e -> {
            if (suppressEvents) return;
            boolean inherit = inheritCheck.isSelected();
            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
            if (inherit && desc != null) {
                ColorAttribute def = desc.getDefaultAttribute();
                if (def != null) {
                    foregroundHex = def.getForeground();
                    foregroundCheck.setSelected(foregroundHex != null);
                    backgroundHex = def.getBackground();
                    backgroundCheck.setSelected(backgroundHex != null);
                    errorStripeHex = def.getErrorStripeColor();
                    errorStripeCheck.setSelected(errorStripeHex != null);
                    boldCheck.setSelected(def.isBold());
                    italicCheck.setSelected(def.isItalic());
                    if (def.getEffectType() != null && def.getEffectType() != EffectType.NONE) {
                        effectsCheck.setSelected(true);
                        effectsHex = def.getEffectColor();
                        effectTypeCombo.setValue(def.getEffectType());
                    } else {
                        effectsCheck.setSelected(false);
                    }
                    updateSwatch(foregroundSwatch, foregroundCheck.isSelected() ? foregroundHex : null);
                    updateSwatch(backgroundSwatch, backgroundCheck.isSelected() ? backgroundHex : null);
                    updateSwatch(errorStripeSwatch, errorStripeCheck.isSelected() ? errorStripeHex : null);
                    updateSwatch(effectsSwatch, effectsCheck.isSelected() ? effectsHex : null);
                }
            }
            modified = true;
            notifyModified();
            updatePreview();
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
                inheritTargetLabel.setText(currentInheritedTargetKey);
                inheritScopeLabel.setText(currentInheritedScope);
                inheritCheck.setSelected(attr == null || attr.isInherit());
            }

            if (attr != null) {
                boldCheck.setSelected(attr.isBold());
                italicCheck.setSelected(attr.isItalic());

                boolean hasFg = attr.getForeground() != null && !attr.getForeground().isBlank();
                foregroundCheck.setSelected(hasFg);
                foregroundHex = hasFg ? attr.getForeground() : null;
                updateSwatch(foregroundSwatch, foregroundHex);

                boolean hasBg = attr.getBackground() != null && !attr.getBackground().isBlank();
                backgroundCheck.setSelected(hasBg);
                backgroundHex = hasBg ? attr.getBackground() : null;
                updateSwatch(backgroundSwatch, backgroundHex);

                boolean hasStripe = attr.getErrorStripeColor() != null && !attr.getErrorStripeColor().isBlank();
                errorStripeCheck.setSelected(hasStripe);
                errorStripeHex = hasStripe ? attr.getErrorStripeColor() : null;
                updateSwatch(errorStripeSwatch, errorStripeHex);

                boolean hasEff = attr.getEffectColor() != null && !attr.getEffectColor().isBlank() && attr.getEffectType() != EffectType.NONE;
                effectsCheck.setSelected(hasEff);
                effectsHex = hasEff ? attr.getEffectColor() : null;
                updateSwatch(effectsSwatch, effectsHex);

                if (attr.getEffectType() != null && attr.getEffectType() != EffectType.NONE) {
                    effectTypeCombo.setValue(attr.getEffectType());
                } else if (desc != null && desc.getDefaultEffectType() != null) {
                    effectTypeCombo.setValue(desc.getDefaultEffectType());
                } else {
                    effectTypeCombo.setValue(EffectType.BORDERED);
                }
            } else {
                boldCheck.setSelected(false);
                italicCheck.setSelected(false);
                foregroundCheck.setSelected(false);
                foregroundHex = null;
                updateSwatch(foregroundSwatch, null);
                backgroundCheck.setSelected(false);
                backgroundHex = null;
                updateSwatch(backgroundSwatch, null);
                errorStripeCheck.setSelected(false);
                errorStripeHex = null;
                updateSwatch(errorStripeSwatch, null);
                effectsCheck.setSelected(false);
                effectsHex = null;
                updateSwatch(effectsSwatch, null);
                effectTypeCombo.setValue(EffectType.BORDERED);
            }

            if (desc != null) {
                boldCheck.setDisable(!desc.isSupportsFont());
                italicCheck.setDisable(!desc.isSupportsFont());
                foregroundCheck.setDisable(!desc.isSupportsForeground());
                foregroundSwatch.setDisable(!desc.isSupportsForeground());
                backgroundCheck.setDisable(!desc.isSupportsBackground());
                backgroundSwatch.setDisable(!desc.isSupportsBackground());
                errorStripeCheck.setDisable(!desc.isSupportsErrorStripe());
                errorStripeSwatch.setDisable(!desc.isSupportsErrorStripe());
                effectsCheck.setDisable(!desc.isSupportsEffects());
                effectsSwatch.setDisable(!desc.isSupportsEffects());
                effectTypeCombo.setDisable(!desc.isSupportsEffects());
            } else {
                boldCheck.setDisable(false);
                italicCheck.setDisable(false);
                foregroundCheck.setDisable(false);
                foregroundSwatch.setDisable(false);
                backgroundCheck.setDisable(false);
                backgroundSwatch.setDisable(false);
                errorStripeCheck.setDisable(false);
                errorStripeSwatch.setDisable(false);
                effectsCheck.setDisable(false);
                effectsSwatch.setDisable(false);
                effectTypeCombo.setDisable(false);
            }
        } finally {
            suppressEvents = false;
        }
    }

    private void onAttributeControlChanged() {
        if (suppressEvents) return;

        updateSwatch(foregroundSwatch, foregroundCheck.isSelected() ? foregroundHex : null);
        updateSwatch(backgroundSwatch, backgroundCheck.isSelected() ? backgroundHex : null);
        updateSwatch(errorStripeSwatch, errorStripeCheck.isSelected() ? errorStripeHex : null);
        updateSwatch(effectsSwatch, effectsCheck.isSelected() ? effectsHex : null);

        if (inheritBox.isVisible()) {
            inheritCheck.setSelected(false);
        }

        modified = true;
        updatePreview();
        notifyModified();
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

    private void openColorPicker(String title, String currentHex, Consumer<String> onSelected) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        Color initialColor = Color.web("#BCBEC4");
        if (currentHex != null && !currentHex.isBlank()) {
            try {
                initialColor = Color.web(currentHex.startsWith("#") ? currentHex : "#" + currentHex);
            } catch (Exception ignored) {
            }
        }
        ColorPicker picker = new ColorPicker(initialColor);
        VBox content = new VBox(10, new Label("Select color:"), picker);
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

    private static class Token {
        final String text;
        final String categoryKey;
        final String defaultColor;

        Token(String text, String categoryKey, String defaultColor) {
            this.text = text;
            this.categoryKey = categoryKey;
            this.defaultColor = defaultColor;
        }
    }

    private ColorAttribute getCurrentEditorAttribute() {
        ColorAttribute attr = new ColorAttribute();
        attr.setBold(boldCheck.isSelected());
        attr.setItalic(italicCheck.isSelected());
        attr.setInherit(inheritCheck.isSelected());
        if (foregroundCheck.isSelected() && foregroundHex != null) attr.setForeground(foregroundHex);
        if (backgroundCheck.isSelected() && backgroundHex != null) attr.setBackground(backgroundHex);
        if (errorStripeCheck.isSelected() && errorStripeHex != null) attr.setErrorStripeColor(errorStripeHex);
        if (effectsCheck.isSelected() && effectsHex != null) {
            attr.setEffectColor(effectsHex);
            attr.setEffectType(effectTypeCombo.getValue() != null ? effectTypeCombo.getValue() : EffectType.BORDERED);
        }
        return attr;
    }

    private void updatePreview() {
        codeLinesBox.getChildren().clear();
        EditorColorSchemeSettings settings = EditorColorSchemeSettings.getInstance();
        String scheme = headerBar.getSelectedScheme();

        // GitLab CI expression snippet matching media_1790430732315.png:
        // $VAR0 == "my-branch" || (($VAR1 == "thing" || $VAR2 == "thing") && $VAR3) && ($VAR1 =~ /^content.*/i || $VAR2) && ($VAR3 !~ /thing$
        List<List<Token>> lines = List.of(
                List.of(
                        new Token("$", "Variable prefix ($)", "#CF8E6D"),
                        new Token("VAR0", "Identifier", "#BCBEC4"),
                        new Token(" == ", "Comparison operator (==, !=, =~, !~)", "#BCBEC4"),
                        new Token("\"my-branch\"", "String", "#6AAB73"),
                        new Token(" || ", "Logical operator (, ||)", "#BCBEC4"),
                        new Token("(", "Parentheses", "#BCBEC4"),
                        new Token("(", "Parentheses", "#BCBEC4"),
                        new Token("$", "Variable prefix ($)", "#CF8E6D"),
                        new Token("VAR1", "Identifier", "#BCBEC4"),
                        new Token(" == ", "Comparison operator (==, !=, =~, !~)", "#BCBEC4"),
                        new Token("\"thing\"", "String", "#6AAB73"),
                        new Token(" || ", "Logical operator (, ||)", "#BCBEC4"),
                        new Token("$", "Variable prefix ($)", "#CF8E6D"),
                        new Token("VAR2", "Identifier", "#BCBEC4"),
                        new Token(" == ", "Comparison operator (==, !=, =~, !~)", "#BCBEC4"),
                        new Token("\"thing\"", "String", "#6AAB73"),
                        new Token(")", "Parentheses", "#BCBEC4"),
                        new Token(" && ", "Logical operator (, ||)", "#BCBEC4"),
                        new Token("$", "Variable prefix ($)", "#CF8E6D"),
                        new Token("VAR3", "Identifier", "#BCBEC4"),
                        new Token(")", "Parentheses", "#BCBEC4"),
                        new Token(" && ", "Logical operator (, ||)", "#BCBEC4"),
                        new Token("(", "Parentheses", "#BCBEC4"),
                        new Token("$", "Variable prefix ($)", "#CF8E6D"),
                        new Token("VAR1", "Identifier", "#BCBEC4"),
                        new Token(" =~ ", "Comparison operator (==, !=, =~, !~)", "#BCBEC4"),
                        new Token("/^content.*/i", "Regular expression", "#42C3D4"),
                        new Token(" || ", "Logical operator (, ||)", "#BCBEC4"),
                        new Token("$", "Variable prefix ($)", "#CF8E6D"),
                        new Token("VAR2", "Identifier", "#BCBEC4"),
                        new Token(")", "Parentheses", "#BCBEC4"),
                        new Token(" && ", "Logical operator (, ||)", "#BCBEC4"),
                        new Token("(", "Parentheses", "#BCBEC4"),
                        new Token("$", "Variable prefix ($)", "#CF8E6D"),
                        new Token("VAR3", "Identifier", "#BCBEC4"),
                        new Token(" !~ ", "Comparison operator (==, !=, =~, !~)", "#BCBEC4"),
                        new Token("/thing$", "Regular expression", "#42C3D4")
                )
        );

        for (List<Token> lineTokens : lines) {
            HBox lineBox = new HBox(0);
            lineBox.setAlignment(Pos.CENTER_LEFT);

            for (Token token : lineTokens) {
                Label label = new Label(token.text);
                label.setFont(Font.font("Monospaced", 13));

                ColorAttribute attr = null;
                boolean isSelectedToken = false;
                if (token.categoryKey != null) {
                    isSelectedToken = token.categoryKey.equals(selectedKey);
                    if (isSelectedToken) {
                        attr = getCurrentEditorAttribute();
                    } else {
                        attr = settings.getAttribute(scheme, token.categoryKey);
                        if (attr == null) {
                            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(token.categoryKey);
                            if (desc != null) attr = desc.getDefaultAttribute();
                        }
                    }
                }

                String fgColor = token.defaultColor;
                String bgColor = null;
                boolean isBold = false;
                boolean isItalic = false;

                if (attr != null) {
                    if (attr.getForeground() != null && !attr.getForeground().isBlank()) {
                        fgColor = attr.getForeground();
                    }
                    if (attr.getBackground() != null && !attr.getBackground().isBlank()) {
                        bgColor = attr.getBackground();
                    }
                    isBold = attr.isBold();
                    isItalic = attr.isItalic();
                }

                StringBuilder style = new StringBuilder();
                style.append("-fx-text-fill: ").append(fgColor).append(";");
                if (isBold && isItalic) {
                    style.append(" -fx-font-weight: bold; -fx-font-style: italic;");
                } else if (isBold) {
                    style.append(" -fx-font-weight: bold;");
                } else if (isItalic) {
                    style.append(" -fx-font-style: italic;");
                }

                if (bgColor != null) {
                    style.append(" -fx-background-color: ").append(bgColor).append(";");
                }

                if (isSelectedToken) {
                    style.append(" -fx-border-color: #3574F0; -fx-border-width: 1; -fx-padding: 0 1 0 1;");
                }

                if (token.categoryKey != null) {
                    label.setCursor(javafx.scene.Cursor.HAND);
                    label.setOnMouseClicked(e -> selectTreeItem(token.categoryKey));
                }

                label.setStyle(style.toString());
                lineBox.getChildren().add(label);
            }
            codeLinesBox.getChildren().add(lineBox);
        }
    }

    private void notifyModified() {
        modified = true;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnNavigateToInheritedListener(Consumer<String> listener) {
        this.onNavigateToInheritedListener = listener;
    }

    public boolean isModified() {
        return modified;
    }

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

    public ColorSchemeHeaderBar getHeaderBar() {
        return headerBar;
    }

    public TreeView<String> getCategoryTree() {
        return categoryTree;
    }

    public String getSelectedKey() {
        return selectedKey;
    }

    public String getForegroundHex() {
        return foregroundHex;
    }

    public boolean isInheritChecked() {
        return inheritCheck.isSelected();
    }

    public VBox getAttributeEditorBox() {
        return attributeEditorBox;
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

    public ScrollPane getPreviewScrollPane() {
        return previewScrollPane;
    }

    public VBox getCodeLinesBox() {
        return codeLinesBox;
    }
}
