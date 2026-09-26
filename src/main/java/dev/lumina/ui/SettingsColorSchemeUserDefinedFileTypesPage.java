package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.*;

/**
 * Editor > Color Scheme > User-Defined File Types settings page.
 * Matches 1:1 with reference image media_1790422763100.png:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - Upper Section:
 *     Left: TreeView with 10 flat attributes: Block comment, Invalid string escape,
 *           Keyword1..4, Line comment, Number, String, Valid string escape.
 *     Right: Attribute Editor panel (Bold/Italic, Foreground, Background, Error stripe mark, Effects).
 * - Lower Section: Code preview with multi-keyword definitions, valid/invalid escapes,
 *   comments, numbers, strings, and bi-directional interactive token navigation.
 */
public class SettingsColorSchemeUserDefinedFileTypesPage extends VBox {

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

    // Preview controls
    private final ScrollPane previewScrollPane;
    private final VBox codeLinesBox;

    // State
    private String selectedKey = "Keyword2";
    private String foregroundHex = "#C77DBB";
    private String backgroundHex = null;
    private String errorStripeHex = null;
    private String effectsHex = null;
    private boolean suppressEvents = false;
    private boolean modified = false;

    private Runnable onModifiedListener;

    public SettingsColorSchemeUserDefinedFileTypesPage() {
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

        TreeItem<String> root = new TreeItem<>("Root");
        buildCategoryTree(root);
        categoryTree.setRoot(root);

        // 3. Attribute Editor Box
        attributeEditorBox = new VBox(12);
        attributeEditorBox.setPadding(new Insets(8, 24, 8, 24));
        attributeEditorBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(attributeEditorBox, Priority.ALWAYS);

        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                selectedKey = newVal.getValue();
                loadAttributesForSelectedKey();
                updatePreview();
            }
        });

        // Font style row
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
                updateSwatchButton(foregroundSwatch, null, false);
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
                updateSwatchButton(backgroundSwatch, null, false);
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
                updateSwatchButton(errorStripeSwatch, null, false);
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
                EffectType.STRIKEOUT,
                EffectType.DOTTED_LINE
        );
        effectTypeCombo.setValue(EffectType.BORDERED);
        effectTypeCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;"
        );
        effectTypeCombo.setPrefWidth(140);
        effectTypeCombo.valueProperty().addListener((o, ov, nv) -> onAttributeChanged());

        effectsCheck.selectedProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents && !nv) {
                effectsHex = null;
                updateSwatchButton(effectsSwatch, null, false);
                onAttributeChanged();
            }
        });

        HBox effectsRow = new HBox(8, effectsCheck, effectsSwatch);
        effectsRow.setAlignment(Pos.CENTER_LEFT);

        HBox effectTypeRow = new HBox(8, new Region() {{ setPrefWidth(140); }}, effectTypeCombo);
        effectTypeRow.setAlignment(Pos.CENTER_LEFT);

        attributeEditorBox.getChildren().addAll(
                fontStyleRow,
                foregroundRow,
                backgroundRow,
                errorStripeRow,
                effectsRow,
                effectTypeRow
        );

        HBox topArea = new HBox(16, categoryTree, attributeEditorBox);
        topArea.setPrefHeight(250);
        topArea.setMinHeight(220);

        // 4. Code Preview
        codeLinesBox = new VBox(4);
        codeLinesBox.setPadding(new Insets(12, 16, 12, 16));
        codeLinesBox.setStyle("-fx-background-color: #1E1F22;");
        HBox.setHgrow(codeLinesBox, Priority.ALWAYS);

        previewScrollPane = new ScrollPane(codeLinesBox);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle(
                "-fx-background: #1E1F22; -fx-background-color: #1E1F22; " +
                "-fx-border-color: #2B2D30; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        previewScrollPane.setPrefHeight(290);
        previewScrollPane.setMinHeight(200);
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        getChildren().addAll(headerBar, topArea, previewScrollPane);

        // Initial selection: Keyword2 (matching Screenshot media_1790422763100.png)
        selectTreeItem(selectedKey);
        updatePreview();
    }

    private void buildCategoryTree(TreeItem<String> root) {
        for (EditorColorSchemeSettings.AttributesDescriptor desc : EditorColorSchemeSettings.getUserDefinedFileTypesDescriptors()) {
            TreeItem<String> leaf = new TreeItem<>(desc.getDisplayName());
            root.getChildren().add(leaf);
        }
    }

    private Button createSwatchButton() {
        Button btn = new Button();
        btn.setPrefSize(72, 24);
        btn.setMinSize(72, 24);
        btn.setMaxSize(72, 24);
        btn.setFont(Font.font("JetBrains Mono", FontWeight.NORMAL, 11));
        return btn;
    }

    private void setupSwatchButton(Button btn, java.util.function.Consumer<String> onColorSelected) {
        btn.setOnAction(e -> {
            openColorChooserDialog(btn.getText(), color -> {
                updateSwatchButton(btn, color, true);
                onColorSelected.accept(color);
            });
        });
    }

    private void updateSwatchButton(Button btn, String hex, boolean enabled) {
        if (enabled && hex != null && !hex.isBlank()) {
            String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
            String fullHex = "#" + cleanHex;
            Color c;
            try {
                c = Color.web(fullHex);
            } catch (Exception ex) {
                c = Color.GRAY;
            }
            double brightness = (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114);
            String textFill = brightness > 0.5 ? "#1E1F22" : "#DFE1E5";

            btn.setText(cleanHex.toUpperCase());
            btn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 0;",
                    fullHex, textFill
            ));
            btn.setDisable(false);
        } else {
            btn.setText("");
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 0;");
            btn.setDisable(!enabled);
        }
    }

    private void openColorChooserDialog(String currentHex, java.util.function.Consumer<String> onChosen) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Color");
        dialog.setHeaderText(null);

        ColorPicker picker = new ColorPicker();
        if (currentHex != null && !currentHex.isBlank()) {
            try {
                picker.setValue(Color.web(currentHex.startsWith("#") ? currentHex : "#" + currentHex));
            } catch (Exception ignored) {
            }
        }

        VBox content = new VBox(10, new Label("Choose color:"), picker);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? toHex(picker.getValue()) : null);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(onChosen);
    }

    private String toHex(Color color) {
        if (color == null) return null;
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    private void onAttributeChanged() {
        if (suppressEvents) return;

        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = new ColorAttribute();
        attr.setBold(boldCheck.isSelected());
        attr.setItalic(italicCheck.isSelected());
        attr.setForeground(foregroundCheck.isSelected() ? foregroundHex : null);
        attr.setBackground(backgroundCheck.isSelected() ? backgroundHex : null);
        attr.setErrorStripeColor(errorStripeCheck.isSelected() ? errorStripeHex : null);
        if (effectsCheck.isSelected()) {
            attr.setEffectColor(effectsHex);
            attr.setEffectType(effectTypeCombo.getValue());
        } else {
            attr.setEffectColor(null);
            attr.setEffectType(EffectType.NONE);
        }

        s.setAttribute(s.getActiveSchemeName(), selectedKey, attr);
        notifyModified();
        updatePreview();
    }

    public void loadAttributesForSelectedKey() {
        suppressEvents = true;
        try {
            EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
            EditorColorSchemeSettings.AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
            ColorAttribute effective = s.resolveAttribute(s.getActiveSchemeName(), selectedKey);
            if (effective == null && desc != null) {
                effective = desc.getDefaultAttribute();
            }

            boldCheck.setSelected(effective.isBold());
            italicCheck.setSelected(effective.isItalic());

            foregroundHex = effective.getForeground();
            foregroundCheck.setSelected(foregroundHex != null);
            updateSwatchButton(foregroundSwatch, foregroundHex, foregroundCheck.isSelected());

            backgroundHex = effective.getBackground();
            backgroundCheck.setSelected(backgroundHex != null);
            updateSwatchButton(backgroundSwatch, backgroundHex, backgroundCheck.isSelected());

            errorStripeHex = effective.getErrorStripeColor();
            errorStripeCheck.setSelected(errorStripeHex != null);
            updateSwatchButton(errorStripeSwatch, errorStripeHex, errorStripeCheck.isSelected());

            boolean hasEffects = effective.getEffectType() != null && effective.getEffectType() != EffectType.NONE;
            effectsCheck.setSelected(hasEffects);
            effectsHex = effective.getEffectColor();
            updateSwatchButton(effectsSwatch, effectsHex, hasEffects);
            effectTypeCombo.setDisable(!hasEffects);

            if (effective.getEffectType() != null && effective.getEffectType() != EffectType.NONE) {
                effectTypeCombo.setValue(effective.getEffectType());
            } else {
                effectTypeCombo.setValue(desc != null ? desc.getDefaultEffectType() : EffectType.BORDERED);
            }
        } finally {
            suppressEvents = false;
        }
    }

    public void selectTreeItem(String key) {
        if (key == null) return;
        for (TreeItem<String> item : categoryTree.getRoot().getChildren()) {
            if (item.getValue().equalsIgnoreCase(key)) {
                categoryTree.getSelectionModel().select(item);
                selectedKey = item.getValue();
                loadAttributesForSelectedKey();
                updatePreview();
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // User-Defined File Types Code Preview
    // -------------------------------------------------------------------------

    private void updatePreview() {
        codeLinesBox.getChildren().clear();

        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        String scheme = s.getActiveSchemeName();

        ColorAttribute lineCommentAttr = s.resolveAttribute(scheme, "Line comment");
        ColorAttribute blockCommentAttr = s.resolveAttribute(scheme, "Block comment");
        ColorAttribute kw1Attr = s.resolveAttribute(scheme, "Keyword1");
        ColorAttribute kw2Attr = s.resolveAttribute(scheme, "Keyword2");
        ColorAttribute kw3Attr = s.resolveAttribute(scheme, "Keyword3");
        ColorAttribute kw4Attr = s.resolveAttribute(scheme, "Keyword4");
        ColorAttribute numAttr = s.resolveAttribute(scheme, "Number");
        ColorAttribute strAttr = s.resolveAttribute(scheme, "String");
        ColorAttribute validEscapeAttr = s.resolveAttribute(scheme, "Valid string escape");
        ColorAttribute invalidEscapeAttr = s.resolveAttribute(scheme, "Invalid string escape");

        // Line 1: # Line comment
        codeLinesBox.getChildren().add(createCommentLine("# Line comment", "Line comment", lineCommentAttr));

        // Line 2: aKeyword1 variable = 123;
        codeLinesBox.getChildren().add(createStatementLine("aKeyword1", kw1Attr, "Keyword1", "variable", "123", numAttr));

        // Line 3: anotherKeyword1 someString = "SomeString";
        codeLinesBox.getChildren().add(createStringStatementLine("anotherKeyword1", kw1Attr, "Keyword1", "someString", "\"SomeString\"", strAttr));

        // Line 4: aKeyword2 variable = 123;
        codeLinesBox.getChildren().add(createStatementLine("aKeyword2", kw2Attr, "Keyword2", "variable", "123", numAttr));

        // Line 5: anotherKeyword2 someString = "SomeString";
        codeLinesBox.getChildren().add(createStringStatementLine("anotherKeyword2", kw2Attr, "Keyword2", "someString", "\"SomeString\"", strAttr));

        // Line 6: aKeyword3 variable = 123;
        codeLinesBox.getChildren().add(createStatementLine("aKeyword3", kw3Attr, "Keyword3", "variable", "123", numAttr));

        // Line 7: anotherKeyword3 someString = "SomeString";
        codeLinesBox.getChildren().add(createStringStatementLine("anotherKeyword3", kw3Attr, "Keyword3", "someString", "\"SomeString\"", strAttr));

        // Line 8: aKeyword4 variable = 123;
        codeLinesBox.getChildren().add(createStatementLine("aKeyword4", kw4Attr, "Keyword4", "variable", "123", numAttr));

        // Line 9: anotherKeyword4 someString = "SomeString \n\x \&\g";
        codeLinesBox.getChildren().add(createEscapedStringStatementLine("anotherKeyword4", kw4Attr, "Keyword4", "someString", strAttr, validEscapeAttr, invalidEscapeAttr));

        // Line 10-12: /* Block comment */
        codeLinesBox.getChildren().add(createCommentLine("/*", "Block comment", blockCommentAttr));
        codeLinesBox.getChildren().add(createCommentLine(" * Block comment", "Block comment", blockCommentAttr));
        codeLinesBox.getChildren().add(createCommentLine(" */", "Block comment", blockCommentAttr));
    }

    private HBox createCommentLine(String text, String key, ColorAttribute attr) {
        HBox line = new HBox();
        line.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(text);
        lbl.setFont(Font.font("JetBrains Mono", (attr != null && attr.isItalic()) ? FontPosture.ITALIC : FontPosture.REGULAR, 12.5));
        String fg = attr != null && attr.getForeground() != null ? attr.getForeground() : "#7A7E85";
        lbl.setStyle(String.format(
                "-fx-text-fill: %s; -fx-cursor: hand; %s",
                fg,
                selectedKey.equals(key) ? "-fx-background-color: #2E436E; -fx-background-radius: 2;" : ""
        ));
        lbl.setOnMouseClicked(e -> selectTreeItem(key));
        line.getChildren().add(lbl);
        return line;
    }

    private HBox createStatementLine(String keyword, ColorAttribute kwAttr, String kwKey, String varName, String numVal, ColorAttribute numAttr) {
        HBox line = new HBox(6);
        line.setAlignment(Pos.CENTER_LEFT);

        Label kwLbl = createClickableToken(keyword, kwKey, kwAttr, "#CF8E6D");
        Text varTxt = new Text(varName + " = ");
        varTxt.setFont(Font.font("JetBrains Mono", 12.5));
        varTxt.setFill(Color.web("#BCBEC4"));

        Label numLbl = createClickableToken(numVal, "Number", numAttr, "#2AACB8");
        Text semi = new Text(";");
        semi.setFont(Font.font("JetBrains Mono", 12.5));
        semi.setFill(Color.web("#BCBEC4"));

        line.getChildren().addAll(kwLbl, varTxt, numLbl, semi);
        return line;
    }

    private HBox createStringStatementLine(String keyword, ColorAttribute kwAttr, String kwKey, String varName, String strVal, ColorAttribute strAttr) {
        HBox line = new HBox(6);
        line.setAlignment(Pos.CENTER_LEFT);

        Label kwLbl = createClickableToken(keyword, kwKey, kwAttr, "#CF8E6D");
        Text varTxt = new Text(varName + " = ");
        varTxt.setFont(Font.font("JetBrains Mono", 12.5));
        varTxt.setFill(Color.web("#BCBEC4"));

        Label strLbl = createClickableToken(strVal, "String", strAttr, "#6AAB73");
        Text semi = new Text(";");
        semi.setFont(Font.font("JetBrains Mono", 12.5));
        semi.setFill(Color.web("#BCBEC4"));

        line.getChildren().addAll(kwLbl, varTxt, strLbl, semi);
        return line;
    }

    private HBox createEscapedStringStatementLine(String keyword, ColorAttribute kwAttr, String kwKey, String varName,
                                                 ColorAttribute strAttr, ColorAttribute validEscapeAttr, ColorAttribute invalidEscapeAttr) {
        HBox line = new HBox(6);
        line.setAlignment(Pos.CENTER_LEFT);

        Label kwLbl = createClickableToken(keyword, kwKey, kwAttr, "#CF8E6D");
        Text varTxt = new Text(varName + " = ");
        varTxt.setFont(Font.font("JetBrains Mono", 12.5));
        varTxt.setFill(Color.web("#BCBEC4"));

        HBox strBox = new HBox();
        strBox.setAlignment(Pos.CENTER_LEFT);

        Label quoteOpen = createClickableToken("\"SomeString ", "String", strAttr, "#6AAB73");
        Label valid1 = createClickableToken("\\n", "Valid string escape", validEscapeAttr, "#CF8E6D");
        Label invalid1 = createClickableToken("\\x", "Invalid string escape", invalidEscapeAttr, "#F75464");
        Label space = createClickableToken(" ", "String", strAttr, "#6AAB73");
        Label valid2 = createClickableToken("\\&", "Valid string escape", validEscapeAttr, "#CF8E6D");
        Label invalid2 = createClickableToken("\\g", "Invalid string escape", invalidEscapeAttr, "#F75464");
        Label quoteClose = createClickableToken("\"", "String", strAttr, "#6AAB73");

        strBox.getChildren().addAll(quoteOpen, valid1, invalid1, space, valid2, invalid2, quoteClose);

        Text semi = new Text(";");
        semi.setFont(Font.font("JetBrains Mono", 12.5));
        semi.setFill(Color.web("#BCBEC4"));

        line.getChildren().addAll(kwLbl, varTxt, strBox, semi);
        return line;
    }

    private Label createClickableToken(String text, String key, ColorAttribute attr, String fallbackHex) {
        Label lbl = new Label(text);
        boolean bold = attr != null && attr.isBold();
        boolean italic = attr != null && attr.isItalic();
        FontWeight weight = bold ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = italic ? FontPosture.ITALIC : FontPosture.REGULAR;
        lbl.setFont(Font.font("JetBrains Mono", weight, posture, 12.5));

        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : fallbackHex;
        String effectCss = "";
        if (attr != null && attr.getEffectType() != null && attr.getEffectType() != EffectType.NONE) {
            String effectCol = attr.getEffectColor() != null ? attr.getEffectColor() : fg;
            effectCss = switch (attr.getEffectType()) {
                case UNDERWAVED -> String.format("-fx-border-color: %s; -fx-border-style: dashed; -fx-border-width: 0 0 1 0;", effectCol);
                case UNDERSCORED -> String.format("-fx-border-color: %s; -fx-border-width: 0 0 1 0;", effectCol);
                case BOLD_UNDERSCORED -> String.format("-fx-border-color: %s; -fx-border-width: 0 0 2 0;", effectCol);
                case BORDERED -> String.format("-fx-border-color: %s; -fx-border-width: 1;", effectCol);
                default -> "";
            };
        }

        lbl.setStyle(String.format(
                "-fx-text-fill: %s; -fx-cursor: hand; %s %s",
                fg,
                effectCss,
                selectedKey.equals(key) ? "-fx-background-color: #2E436E; -fx-background-radius: 2;" : ""
        ));
        lbl.setOnMouseClicked(e -> selectTreeItem(key));
        return lbl;
    }

    public void apply() {
        EditorColorSchemeSettings.getInstance().save();
        modified = false;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void reset() {
        EditorColorSchemeSettings.getInstance().load();
        loadAttributesForSelectedKey();
        updatePreview();
        modified = false;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public boolean isModified() {
        return modified || EditorColorSchemeSettings.getInstance().isSchemeModified(headerBar.getSchemeCombo().getValue());
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        modified = true;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Getters for integration and tests
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
    public ScrollPane getPreviewScrollPane() { return previewScrollPane; }
    public VBox getCodeLinesBox() { return codeLinesBox; }
}
