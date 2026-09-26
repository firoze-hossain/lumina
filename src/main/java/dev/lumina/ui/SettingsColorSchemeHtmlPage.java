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
 * 1:1 visual & functional replication of Editor > Color Scheme > HTML.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemeHtmlPage extends VBox {

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

    private String selectedKey = "Tag name";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemeHtmlPage() {
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

        TreeItem<String> tagNameItem = new TreeItem<>("Tag name");

        rootItem.getChildren().addAll(
                new TreeItem<>("Attribute name"),
                new TreeItem<>("Attribute value"),
                new TreeItem<>("Comment"),
                new TreeItem<>("Custom Tag Name"),
                new TreeItem<>("Entity reference"),
                new TreeItem<>("HTML code"),
                new TreeItem<>("Injected Language Fragment"),
                new TreeItem<>("Tag"),
                tagNameItem,
                new TreeItem<>("Tag tree (level 1)"),
                new TreeItem<>("Tag tree (level 2)"),
                new TreeItem<>("Tag tree (level 3)"),
                new TreeItem<>("Tag tree (level 4)"),
                new TreeItem<>("Tag tree (level 5)"),
                new TreeItem<>("Tag tree (level 6)")
        );

        categoryTree = new TreeView<>(rootItem);
        categoryTree.setShowRoot(false);
        categoryTree.setPrefWidth(330);
        categoryTree.setMinWidth(260);
        categoryTree.setMaxWidth(420);
        categoryTree.setPrefHeight(230);
        categoryTree.setStyle(
                "-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-text-fill: #DFE1E5; -fx-font-size: 13px;"
        );

        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isLeaf()) {
                selectCategory(newVal.getValue());
            }
        });

        // ---------------- Right Attribute Panel ----------------
        VBox attributeEditor = new VBox(6);
        attributeEditor.setPadding(new Insets(6, 12, 12, 16));
        attributeEditor.setStyle("-fx-background-color: #1E1F22;");
        attributeEditor.setPrefWidth(320);

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
        Region fgSpacer = new Region();
        HBox.setHgrow(fgSpacer, Priority.ALWAYS);
        foregroundSwatch = createSwatchButton();
        fgRow.getChildren().addAll(foregroundCheck, fgSpacer, foregroundSwatch);

        // Background
        HBox bgRow = new HBox(8);
        bgRow.setAlignment(Pos.CENTER_LEFT);
        backgroundCheck = new CheckBox("Background");
        backgroundCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Region bgSpacer = new Region();
        HBox.setHgrow(bgSpacer, Priority.ALWAYS);
        backgroundSwatch = createSwatchButton();
        bgRow.getChildren().addAll(backgroundCheck, bgSpacer, backgroundSwatch);

        // Error stripe
        HBox esRow = new HBox(8);
        esRow.setAlignment(Pos.CENTER_LEFT);
        errorStripeCheck = new CheckBox("Error stripe mark");
        errorStripeCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Region esSpacer = new Region();
        HBox.setHgrow(esSpacer, Priority.ALWAYS);
        errorStripeSwatch = createSwatchButton();
        esRow.getChildren().addAll(errorStripeCheck, esSpacer, errorStripeSwatch);

        // Effects
        HBox effRow = new HBox(8);
        effRow.setAlignment(Pos.CENTER_LEFT);
        effectsCheck = new CheckBox("Effects");
        effectsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Region effSpacer = new Region();
        HBox.setHgrow(effSpacer, Priority.ALWAYS);
        effectsSwatch = createSwatchButton();
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

        inheritTargetLabel = new Hyperlink();
        inheritTargetLabel.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-padding: 0 0 0 20;");
        inheritTargetLabel.setOnAction(e -> {
            if (onNavigateToInherited != null && inheritTargetLabel.getUserData() != null) {
                onNavigateToInherited.accept((String) inheritTargetLabel.getUserData());
            }
        });

        inheritScopeLabel = new Label();
        inheritScopeLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-padding: 0 0 0 20;");

        inheritBox.getChildren().addAll(inheritCheckRow, inheritTargetLabel, inheritScopeLabel);

        attributeEditor.getChildren().addAll(
                fontBox,
                fgRow,
                bgRow,
                esRow,
                effRow,
                effectTypeRow,
                inheritBox
        );

        HBox topPane = new HBox(16, categoryTree, attributeEditor);
        topPane.setPrefHeight(230);
        VBox.setVgrow(topPane, Priority.NEVER);

        // ---------------- Bottom Preview Pane ----------------
        codeLinesBox = new VBox(2);
        codeLinesBox.setPadding(new Insets(12, 14, 14, 14));
        codeLinesBox.setStyle("-fx-background-color: #1E1F22;");

        previewScrollPane = new ScrollPane(codeLinesBox);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: transparent; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        getChildren().addAll(headerBar, topPane, previewScrollPane);

        setupListeners();

        // Initial selection: Tag name
        categoryTree.getSelectionModel().select(tagNameItem);
        loadScheme(settings.getActiveSchemeName());
        selectCategory("Tag name");
    }

    private Button createSwatchButton() {
        Button btn = new Button();
        btn.setPrefSize(72, 22);
        btn.setMinSize(72, 22);
        btn.setMaxSize(72, 22);
        btn.setFont(Font.font("Monospaced", 9));
        btn.setAlignment(Pos.CENTER);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: #55575E; -fx-border-radius: 3; -fx-background-radius: 3;");
        return btn;
    }

    private void updateSwatch(Button swatch, String hex) {
        if (hex == null || hex.isBlank()) {
            swatch.setText("");
            swatch.setStyle("-fx-background-color: transparent; -fx-border-color: #55575E; -fx-border-radius: 3; -fx-background-radius: 3;");
            return;
        }
        String cleanHex = hex.replace("#", "").toUpperCase();
        swatch.setText(cleanHex);
        String textColor = computeContrastTextColor(cleanHex);
        swatch.setStyle(String.format(
                "-fx-background-color: #%s; -fx-text-fill: %s; -fx-border-color: #55575E; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-weight: bold;",
                cleanHex, textColor
        ));
    }

    private String computeContrastTextColor(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            double yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000.0;
            return yiq >= 128 ? "#000000" : "#FFFFFF";
        } catch (Exception e) {
            return "#FFFFFF";
        }
    }

    private void setupListeners() {
        boldCheck.setOnAction(e -> onAttributeControlChanged());
        italicCheck.setOnAction(e -> onAttributeControlChanged());

        foregroundCheck.setOnAction(e -> {
            if (suppressEvents) return;
            if (foregroundCheck.isSelected() && foregroundHex == null) {
                foregroundHex = "D5B778";
                updateSwatch(foregroundSwatch, foregroundHex);
            }
            onAttributeControlChanged();
        });

        foregroundSwatch.setOnAction(e -> openColorPicker("Foreground Color", foregroundHex, color -> {
            foregroundHex = color;
            foregroundCheck.setSelected(true);
            updateSwatch(foregroundSwatch, foregroundHex);
            onAttributeControlChanged();
        }));

        backgroundCheck.setOnAction(e -> {
            if (suppressEvents) return;
            if (backgroundCheck.isSelected() && backgroundHex == null) {
                backgroundHex = "262626";
                updateSwatch(backgroundSwatch, backgroundHex);
            }
            onAttributeControlChanged();
        });

        backgroundSwatch.setOnAction(e -> openColorPicker("Background Color", backgroundHex, color -> {
            backgroundHex = color;
            backgroundCheck.setSelected(true);
            updateSwatch(backgroundSwatch, backgroundHex);
            onAttributeControlChanged();
        }));

        errorStripeCheck.setOnAction(e -> {
            if (suppressEvents) return;
            if (errorStripeCheck.isSelected() && errorStripeHex == null) {
                errorStripeHex = "FA6675";
                updateSwatch(errorStripeSwatch, errorStripeHex);
            }
            onAttributeControlChanged();
        });

        errorStripeSwatch.setOnAction(e -> openColorPicker("Error Stripe Mark Color", errorStripeHex, color -> {
            errorStripeHex = color;
            errorStripeCheck.setSelected(true);
            updateSwatch(errorStripeSwatch, errorStripeHex);
            onAttributeControlChanged();
        }));

        effectsCheck.setOnAction(e -> {
            if (suppressEvents) return;
            if (effectsCheck.isSelected() && effectsHex == null) {
                effectsHex = "FA6675";
                updateSwatch(effectsSwatch, effectsHex);
            }
            onAttributeControlChanged();
        });

        effectsSwatch.setOnAction(e -> openColorPicker("Effects Color", effectsHex, color -> {
            effectsHex = color;
            effectsCheck.setSelected(true);
            updateSwatch(effectsSwatch, effectsHex);
            onAttributeControlChanged();
        }));

        effectTypeCombo.setOnAction(e -> onAttributeControlChanged());

        inheritCheck.setOnAction(e -> {
            if (suppressEvents) return;
            onAttributeControlChanged();
        });
    }

    private void onAttributeControlChanged() {
        if (suppressEvents) return;
        checkDirty();
        updatePreview();
    }

    private void checkDirty() {
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
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getHtmlDescriptors()) {
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

                boolean hasEff = attr.getEffectColor() != null;
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

                inheritCheck.setSelected(attr.isInherit());
                if (desc != null && desc.getInheritTarget() != null) {
                    inheritBox.setVisible(true);
                    inheritTargetLabel.setText(desc.getInheritTarget());
                    inheritTargetLabel.setUserData(desc.getInheritTarget());
                    inheritScopeLabel.setText(desc.getInheritScope() != null ? desc.getInheritScope() : "");
                } else {
                    inheritBox.setVisible(false);
                }
            }
        } finally {
            suppressEvents = false;
        }
        updatePreview();
    }

    public ColorAttribute getCurrentEditorAttribute() {
        ColorAttribute attr = new ColorAttribute();
        attr.setBold(boldCheck.isSelected());
        attr.setItalic(italicCheck.isSelected());
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
        String scheme = settings.getActiveSchemeName();

        List<List<Token>> lines = List.of(
                List.of(new Token("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">", "HTML code", "#E8BF6A")),
                List.of(new Token("<!--", "Comment", "#7A7E85")),
                List.of(new Token("  * Sample comment", "Comment", "#7A7E85")),
                List.of(new Token("-->", "Comment", "#7A7E85")),
                List.of(
                        new Token("<", "Tag", "#BCBEC4"),
                        new Token("HTML", "Tag name", "#D5B778"),
                        new Token(">", "Tag", "#BCBEC4")
                ),
                List.of(
                        new Token("<", "Tag", "#BCBEC4"),
                        new Token("head", "Tag name", "#D5B778"),
                        new Token(">", "Tag", "#BCBEC4")
                ),
                List.of(
                        new Token("<", "Tag", "#BCBEC4"),
                        new Token("title", "Tag name", "#D5B778"),
                        new Token(">", "Tag", "#BCBEC4"),
                        new Token("Lumina IDE", null, "#BCBEC4"),
                        new Token("</", "Tag", "#BCBEC4"),
                        new Token("title", "Tag name", "#D5B778"),
                        new Token(">", "Tag", "#BCBEC4")
                ),
                List.of(
                        new Token("</", "Tag", "#BCBEC4"),
                        new Token("head", "Tag name", "#D5B778"),
                        new Token(">", "Tag", "#BCBEC4")
                ),
                List.of(
                        new Token("<", "Tag", "#BCBEC4"),
                        new Token("body", "Tag name", "#D5B778"),
                        new Token(">", "Tag", "#BCBEC4")
                ),
                List.of(
                        new Token("<", "Tag", "#BCBEC4"),
                        new Token("h1", "Tag name", "#D5B778"),
                        new Token(">", "Tag", "#BCBEC4"),
                        new Token("Lumina IDE", null, "#BCBEC4"),
                        new Token("</", "Tag", "#BCBEC4"),
                        new Token("h1", "Tag name", "#D5B778"),
                        new Token(">", "Tag", "#BCBEC4")
                ),
                List.of(
                        new Token("<", "Tag", "#BCBEC4"),
                        new Token("p", "Tag name", "#D5B778"),
                        new Token("><", "Tag", "#BCBEC4"),
                        new Token("br", "Tag name", "#D5B778"),
                        new Token("><", "Tag", "#BCBEC4"),
                        new Token("b", "Tag name", "#D5B778"),
                        new Token("><", "Tag", "#BCBEC4"),
                        new Token("IMG", "Tag name", "#D5B778"),
                        new Token(" ", null, "#BCBEC4"),
                        new Token("border", "Attribute name", "#BABABA"),
                        new Token("=0 ", null, "#BCBEC4"),
                        new Token("height", "Attribute name", "#BABABA"),
                        new Token("=12 ", null, "#BCBEC4"),
                        new Token("src", "Attribute name", "#BABABA"),
                        new Token("=", null, "#BCBEC4"),
                        new Token("\"images/hg.gif\"", "Attribute value", "#6AAB73"),
                        new Token(" ", null, "#BCBEC4"),
                        new Token("width", "Attribute name", "#BABABA"),
                        new Token("=18 >", "Tag", "#BCBEC4")
                ),
                List.of(
                        new Token("What is Lumina", null, "#BCBEC4"),
                        new Token("&nbsp;", "Entity reference", "#2AACB8"),
                        new Token("IDE? ", null, "#BCBEC4"),
                        new Token("&#x00B7;", "Entity reference", "#2AACB8"),
                        new Token(" ", null, "#BCBEC4"),
                        new Token("&Alpha;", "Entity reference", "#2AACB8"),
                        new Token("; ", null, "#BCBEC4"),
                        new Token("</", "Tag", "#BCBEC4"),
                        new Token("b", "Tag name", "#D5B778"),
                        new Token("><", "Tag", "#BCBEC4"),
                        new Token("br", "Tag name", "#D5B778"),
                        new Token("><", "Tag", "#BCBEC4"),
                        new Token("br", "Tag name", "#D5B778"),
                        new Token(">", "Tag", "#BCBEC4")
                ),
                List.of(
                        new Token("<", "Tag", "#BCBEC4"),
                        new Token("custom-tag", "Custom Tag Name", "#56A8F5"),
                        new Token(">", "Tag", "#BCBEC4"),
                        new Token("hello", null, "#BCBEC4"),
                        new Token("</", "Tag", "#BCBEC4"),
                        new Token("custom-tag", "Custom Tag Name", "#56A8F5"),
                        new Token(">", "Tag", "#BCBEC4")
                ),
                List.of(
                        new Token("</", "Tag", "#BCBEC4"),
                        new Token("body", "Tag name", "#D5B778"),
                        new Token(">", "Tag", "#BCBEC4")
                ),
                List.of(
                        new Token("</", "Tag", "#BCBEC4"),
                        new Token("html", "Tag name", "#D5B778"),
                        new Token(">", "Tag", "#BCBEC4")
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

                String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : token.fallbackColor;
                boolean bold = attr != null && attr.isBold();
                boolean italic = attr != null && attr.isItalic();

                StringBuilder style = new StringBuilder();
                style.append("-fx-text-fill: #").append(fg.replace("#", "")).append(";");
                if (bold) style.append(" -fx-font-weight: bold;");
                if (italic) style.append(" -fx-font-style: italic;");
                if (attr != null && attr.getBackground() != null) {
                    style.append(" -fx-background-color: #").append(attr.getBackground().replace("#", "")).append(";");
                }
                if (isSelectedToken) {
                    style.append(" -fx-border-color: #3574F0; -fx-border-width: 1; -fx-border-style: solid;");
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
        TreeItem<String> found = findItem(rootItem, key);
        if (found != null) {
            categoryTree.getSelectionModel().select(found);
            categoryTree.scrollTo(categoryTree.getRow(found));
        } else {
            selectCategory(key);
        }
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

    private void openColorPicker(String title, String initialHex, Consumer<String> onSelected) {
        Stage pickerStage = new Stage();
        pickerStage.initModality(Modality.APPLICATION_MODAL);
        pickerStage.initStyle(StageStyle.UTILITY);
        pickerStage.setTitle(title);

        Color initialColor = Color.web(initialHex != null ? (initialHex.startsWith("#") ? initialHex : "#" + initialHex) : "#FFFFFF");
        ColorPicker picker = new ColorPicker(initialColor);
        picker.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5;");

        Button okBtn = new Button("Choose");
        okBtn.getStyleClass().add("dialog-primary");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-cursor: hand;");
        okBtn.setOnAction(e -> {
            Color c = picker.getValue();
            String hex = String.format("%02X%02X%02X",
                    (int) Math.round(c.getRed() * 255),
                    (int) Math.round(c.getGreen() * 255),
                    (int) Math.round(c.getBlue() * 255));
            onSelected.accept(hex);
            pickerStage.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-secondary");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> pickerStage.close());

        HBox btnBox = new HBox(8, okBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(14, new Label(title), picker, btnBox);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #2B2D30;");

        pickerStage.setScene(new Scene(root, 280, 150));
        pickerStage.showAndWait();
    }

    public void apply() {
        ColorAttribute attr = getCurrentEditorAttribute();
        settings.setAttribute(settings.getActiveSchemeName(), selectedKey, attr);
        originalAttributes.put(selectedKey, attr.clone());
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
    }

    public boolean isModified() {
        return isModified;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
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
