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
 * 1:1 visual & functional replication of Editor > Color Scheme > JPA/Hibernate QL.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemeJpaHibernateQlPage extends VBox {

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

    private String selectedKey = "Identification variable";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemeJpaHibernateQlPage() {
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

        TreeItem<String> idVarItem = new TreeItem<>("Identification variable");

        rootItem.getChildren().addAll(
                new TreeItem<>("Attribute"),
                new TreeItem<>("Bad token"),
                new TreeItem<>("Comma"),
                new TreeItem<>("Date and time"),
                new TreeItem<>("Dot"),
                new TreeItem<>("Entity"),
                new TreeItem<>("Function"),
                idVarItem,
                new TreeItem<>("Keyword"),
                new TreeItem<>("Number"),
                new TreeItem<>("Operator sign"),
                new TreeItem<>("Parameter"),
                new TreeItem<>("Parenthesis"),
                new TreeItem<>("String")
        );

        categoryTree = new TreeView<>(rootItem);
        categoryTree.setShowRoot(false);
        categoryTree.getStyleClass().add("color-scheme-tree");
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

        HBox inheritTargetRow = new HBox(6);
        inheritTargetRow.setAlignment(Pos.CENTER_LEFT);
        inheritTargetRow.setPadding(new Insets(0, 0, 0, 22));

        inheritTargetLabel = new Hyperlink("Identifiers->Local variable");
        inheritTargetLabel.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        inheritTargetLabel.setOnAction(e -> {
            if (onNavigateToInherited != null && inheritTargetLabel.getUserData() != null) {
                onNavigateToInherited.accept((String) inheritTargetLabel.getUserData());
            }
        });

        inheritScopeLabel = new Label("(Language Defaults)");
        inheritScopeLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        inheritTargetRow.getChildren().addAll(inheritTargetLabel, inheritScopeLabel);
        inheritBox.getChildren().addAll(inheritCheckRow, inheritTargetRow);

        attributeEditor.getChildren().addAll(
                fontBox,
                fgRow,
                bgRow,
                esRow,
                effRow,
                effectTypeRow,
                inheritBox
        );

        // Top Split: Tree + Attribute Editor
        HBox topSplit = new HBox(16);
        topSplit.getChildren().addAll(categoryTree, attributeEditor);
        HBox.setHgrow(categoryTree, Priority.NEVER);
        HBox.setHgrow(attributeEditor, Priority.ALWAYS);

        // ---------------- Bottom Preview Pane ----------------
        codeLinesBox = new VBox(2);
        codeLinesBox.setPadding(new Insets(10, 14, 10, 14));
        codeLinesBox.setStyle("-fx-background-color: #1E1F22;");

        previewScrollPane = new ScrollPane(codeLinesBox);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setPrefHeight(230);
        previewScrollPane.setMinHeight(160);
        previewScrollPane.setStyle(
                "-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;"
        );

        getChildren().addAll(headerBar, topSplit, previewScrollPane);
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        initListeners();
        loadScheme(settings.getActiveSchemeName());

        categoryTree.getSelectionModel().select(idVarItem);
        selectCategory("Identification variable");
    }

    private Button createSwatchButton() {
        Button btn = new Button();
        btn.setPrefSize(72, 22);
        btn.setMinSize(72, 22);
        btn.setMaxSize(72, 22);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-padding: 0;");
        return btn;
    }

    private void updateSwatch(Button btn, String hexColor) {
        if (hexColor == null || hexColor.isBlank()) {
            btn.setText("");
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-text-fill: #848BA3; -fx-font-size: 11px;");
        } else {
            String cleanHex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
            btn.setText(cleanHex.toUpperCase());
            btn.setStyle(
                    "-fx-background-color: #" + cleanHex + "33; " +
                    "-fx-border-color: #" + cleanHex + "; " +
                    "-fx-border-radius: 3; -fx-background-radius: 3; " +
                    "-fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-font-weight: bold;"
            );
        }
    }

    private void initListeners() {
        boldCheck.setOnAction(e -> onAttributeControlChanged());
        italicCheck.setOnAction(e -> onAttributeControlChanged());

        foregroundCheck.setOnAction(e -> {
            if (suppressEvents) return;
            if (foregroundCheck.isSelected() && foregroundHex == null) {
                foregroundHex = "BCBEC4";
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
                backgroundHex = "26282E";
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
                errorStripeHex = "F75464";
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
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getJpaHibernateQlDescriptors()) {
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
                effectsHex = hasEff ? attr.getEffectColor() : (desc != null && desc.getDefaultAttribute().getEffectColor() != null ? desc.getDefaultAttribute().getEffectColor() : null);
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

        List<List<Token>> lines = List.of(
                List.of(
                        new Token("select", "Keyword", "#CF8E6D"),
                        new Token(" ", null, null),
                        new Token("object", "Function", "#56A8F5"),
                        new Token("(", "Parenthesis", "#BCBEC4"),
                        new Token("o", "Identification variable", "#BCBEC4"),
                        new Token(")", "Parenthesis", "#BCBEC4")
                ),
                List.of(
                        new Token("  ", null, null),
                        new Token("from", "Keyword", "#CF8E6D"),
                        new Token(" ", null, null),
                        new Token("Product", "Entity", "#BCBEC4"),
                        new Token(" ", null, null),
                        new Token("o", "Identification variable", "#BCBEC4"),
                        new Token(" ", null, null),
                        new Token("where", "Keyword", "#CF8E6D"),
                        new Token(" ", null, null),
                        new Token("o", "Identification variable", "#BCBEC4"),
                        new Token(".", "Dot", "#BCBEC4"),
                        new Token("id", "Attribute", "#C77DBB"),
                        new Token(" ", null, null),
                        new Token(">", "Operator sign", "#BCBEC4"),
                        new Token(" ", null, null),
                        new Token("99999", "Number", "#2AAC88")
                ),
                List.of(
                        new Token("   ", null, null),
                        new Token("and", "Keyword", "#CF8E6D"),
                        new Token(" ", null, null),
                        new Token("o", "Identification variable", "#BCBEC4"),
                        new Token(".", "Dot", "#BCBEC4"),
                        new Token("date", "Attribute", "#C77DBB"),
                        new Token(" ", null, null),
                        new Token(">", "Operator sign", "#BCBEC4"),
                        new Token(" ", null, null),
                        new Token("{d '2010-01-01'}", "Date and time", "#2AAC88")
                ),
                List.of(
                        new Token("   ", null, null),
                        new Token("and", "Keyword", "#CF8E6D"),
                        new Token(" ", null, null),
                        new Token("UPPER", "Function", "#56A8F5"),
                        new Token("(", "Parenthesis", "#BCBEC4"),
                        new Token("o", "Identification variable", "#BCBEC4"),
                        new Token(".", "Dot", "#BCBEC4"),
                        new Token("name", "Attribute", "#C77DBB"),
                        new Token(")", "Parenthesis", "#BCBEC4"),
                        new Token(" ", null, null),
                        new Token("like", "Keyword", "#CF8E6D"),
                        new Token(" ", null, null),
                        new Token("'S%'", "String", "#6AAB73")
                ),
                List.of(
                        new Token("   ", null, null),
                        new Token("and", "Keyword", "#CF8E6D"),
                        new Token(" ", null, null),
                        new Token("o", "Identification variable", "#BCBEC4"),
                        new Token(".", "Dot", "#BCBEC4"),
                        new Token("price", "Attribute", "#C77DBB"),
                        new Token(" ", null, null),
                        new Token(">", "Operator sign", "#BCBEC4"),
                        new Token(" ", null, null),
                        new Token(":minPrice", "Parameter", "#BCBEC4")
                )
        );

        for (List<Token> lineTokens : lines) {
            HBox lineBox = new HBox();
            lineBox.setAlignment(Pos.CENTER_LEFT);

            for (Token token : lineTokens) {
                Label label = new Label(token.text);
                label.setFont(Font.font("Monospaced", 13));

                if (token.categoryKey != null) {
                    boolean isSelected = token.categoryKey.equals(selectedKey);

                    ColorAttribute attr;
                    if (isSelected) {
                        attr = getCurrentEditorAttribute();
                    } else {
                        attr = settings.getAttribute(settings.getActiveSchemeName(), token.categoryKey);
                        if (attr == null) {
                            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(token.categoryKey);
                            if (desc != null) attr = desc.getDefaultAttribute();
                        }
                    }

                    String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : token.fallbackColor;
                    if (!fg.startsWith("#")) fg = "#" + fg;

                    StringBuilder style = new StringBuilder();
                    style.append("-fx-text-fill: ").append(fg).append(";");

                    if (attr != null && attr.getBackground() != null) {
                        String bg = attr.getBackground();
                        if (!bg.startsWith("#")) bg = "#" + bg;
                        style.append("-fx-background-color: ").append(bg).append(";");
                    }

                    if (attr != null && attr.isBold()) style.append("-fx-font-weight: bold;");
                    if (attr != null && attr.isItalic()) style.append("-fx-font-style: italic;");

                    if (isSelected) {
                        style.append("-fx-border-color: #589DF6; -fx-border-width: 1; -fx-border-radius: 2;");
                    }

                    label.setStyle(style.toString());
                    label.setCursor(javafx.scene.Cursor.HAND);
                    label.setOnMouseClicked(e -> {
                        selectTreeItem(token.categoryKey);
                    });
                } else {
                    label.setStyle("-fx-text-fill: #BCBEC4;");
                }

                lineBox.getChildren().add(label);
            }

            codeLinesBox.getChildren().add(lineBox);
        }
    }

    private void selectTreeItem(String key) {
        for (TreeItem<String> item : rootItem.getChildren()) {
            if (item.getValue().equals(key)) {
                categoryTree.getSelectionModel().select(item);
                selectCategory(key);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Color Picker Dialog
    // -------------------------------------------------------------------------

    private void openColorPicker(String title, String currentHex, Consumer<String> onColorSelected) {
        Stage pickerStage = new Stage();
        pickerStage.initModality(Modality.APPLICATION_MODAL);
        pickerStage.initStyle(StageStyle.UTILITY);
        pickerStage.setTitle(title);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #2B2D30;");

        Color initial = Color.web("#BCBEC4");
        if (currentHex != null && !currentHex.isBlank()) {
            try {
                String clean = currentHex.startsWith("#") ? currentHex : "#" + currentHex;
                initial = Color.web(clean);
            } catch (Exception ignored) {}
        }

        ColorPicker picker = new ColorPicker(initial);
        picker.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5;");

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5;");
        cancelBtn.setOnAction(e -> pickerStage.close());

        Button chooseBtn = new Button("Choose");
        chooseBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        chooseBtn.setOnAction(e -> {
            Color c = picker.getValue();
            String hex = String.format("%02X%02X%02X",
                    (int) (c.getRed() * 255),
                    (int) (c.getGreen() * 255),
                    (int) (c.getBlue() * 255));
            onColorSelected.accept(hex);
            pickerStage.close();
        });

        btnBox.getChildren().addAll(cancelBtn, chooseBtn);
        root.getChildren().addAll(picker, btnBox);

        Scene scene = new Scene(root);
        pickerStage.setScene(scene);
        pickerStage.showAndWait();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void apply() {
        ColorAttribute attr = getCurrentEditorAttribute();
        settings.setAttribute(settings.getActiveSchemeName(), selectedKey, attr);
        originalAttributes.put(selectedKey, attr.clone());
        isModified = false;
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void reset() {
        loadScheme(settings.getActiveSchemeName());
        isModified = false;
        if (onModifiedListener != null) onModifiedListener.run();
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

    public VBox getInheritBox() {
        return inheritBox;
    }

    public VBox getCodeLinesBox() {
        return codeLinesBox;
    }
}
