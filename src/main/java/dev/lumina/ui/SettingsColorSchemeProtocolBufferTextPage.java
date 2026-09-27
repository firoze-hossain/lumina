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
 * 1:1 visual & functional replication of Editor > Color Scheme > Protocol Buffer Text.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemeProtocolBufferTextPage extends VBox {

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

    private String selectedKey = "Line comment";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemeProtocolBufferTextPage() {
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

        TreeItem<String> bracesItem = new TreeItem<>("Braces");
        TreeItem<String> bracketsItem = new TreeItem<>("Brackets");
        TreeItem<String> commaItem = new TreeItem<>("Comma");
        TreeItem<String> commentDirectiveItem = new TreeItem<>("Comment directive");
        TreeItem<String> dotItem = new TreeItem<>("Dot");
        TreeItem<String> enumValueItem = new TreeItem<>("Enum value");
        TreeItem<String> identifierItem = new TreeItem<>("Identifier");
        TreeItem<String> invalidEscapeItem = new TreeItem<>("Invalid escape sequence");
        TreeItem<String> keywordItem = new TreeItem<>("Keyword");
        TreeItem<String> lineCommentItem = new TreeItem<>("Line comment");
        TreeItem<String> numberItem = new TreeItem<>("Number");
        TreeItem<String> operatorItem = new TreeItem<>("Operator");
        TreeItem<String> semicolonItem = new TreeItem<>("Semicolon");
        TreeItem<String> stringItem = new TreeItem<>("String");
        TreeItem<String> validEscapeItem = new TreeItem<>("Valid escape sequence");

        rootItem.getChildren().addAll(
                bracesItem, bracketsItem, commaItem, commentDirectiveItem, dotItem,
                enumValueItem, identifierItem, invalidEscapeItem, keywordItem, lineCommentItem,
                numberItem, operatorItem, semicolonItem, stringItem, validEscapeItem
        );

        categoryTree = new TreeView<>(rootItem);
        categoryTree.setShowRoot(false);
        categoryTree.getStyleClass().add("settings-tree-view");
        categoryTree.setPrefWidth(300);

        TextField searchField = new TextField();
        searchField.setPromptText("Filter...");
        searchField.getStyleClass().add("settings-search-field");
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
        foregroundSwatch.setOnAction(e -> showColorChooser("Foreground", foregroundHex, color -> {
            foregroundHex = color;
            updateSwatch(foregroundSwatch, foregroundHex);
            foregroundCheck.setSelected(true);
            markModified();
            updatePreview();
        }));
        HBox fgRow = new HBox(12, foregroundCheck, foregroundSwatch);
        fgRow.setAlignment(Pos.CENTER_LEFT);

        // Background
        backgroundCheck = new CheckBox("Background");
        backgroundCheck.setStyle("-fx-text-fill: #BCBEC4;");
        backgroundCheck.setPrefWidth(120);
        backgroundSwatch = createColorButton();
        backgroundSwatch.setOnAction(e -> showColorChooser("Background", backgroundHex, color -> {
            backgroundHex = color;
            updateSwatch(backgroundSwatch, backgroundHex);
            backgroundCheck.setSelected(true);
            markModified();
            updatePreview();
        }));
        HBox bgRow = new HBox(12, backgroundCheck, backgroundSwatch);
        bgRow.setAlignment(Pos.CENTER_LEFT);

        // Error stripe mark
        errorStripeCheck = new CheckBox("Error stripe mark");
        errorStripeCheck.setStyle("-fx-text-fill: #BCBEC4;");
        errorStripeCheck.setPrefWidth(120);
        errorStripeSwatch = createColorButton();
        errorStripeSwatch.setOnAction(e -> showColorChooser("Error stripe mark", errorStripeHex, color -> {
            errorStripeHex = color;
            updateSwatch(errorStripeSwatch, errorStripeHex);
            errorStripeCheck.setSelected(true);
            markModified();
            updatePreview();
        }));
        HBox esRow = new HBox(12, errorStripeCheck, errorStripeSwatch);
        esRow.setAlignment(Pos.CENTER_LEFT);

        // Effects
        effectsCheck = new CheckBox("Effects");
        effectsCheck.setStyle("-fx-text-fill: #BCBEC4;");
        effectsCheck.setPrefWidth(120);
        effectsSwatch = createColorButton();
        effectsSwatch.setOnAction(e -> showColorChooser("Effects", effectsHex, color -> {
            effectsHex = color;
            updateSwatch(effectsSwatch, effectsHex);
            effectsCheck.setSelected(true);
            markModified();
            updatePreview();
        }));
        effectTypeCombo = new ComboBox<>();
        effectTypeCombo.getItems().addAll(EffectType.values());
        effectTypeCombo.setValue(EffectType.BORDERED);
        effectTypeCombo.setPrefWidth(110);

        HBox effRow = new HBox(12, effectsCheck, effectsSwatch, effectTypeCombo);
        effRow.setAlignment(Pos.CENTER_LEFT);

        // Inherit section
        inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: #BCBEC4; -fx-font-weight: bold;");
        inheritTargetLabel = new Hyperlink("Comments->Line comment");
        inheritTargetLabel.setStyle("-fx-text-fill: #56A8F5; -fx-underline: false; -fx-padding: 0;");
        inheritTargetLabel.setOnAction(e -> {
            if (onNavigateToInherited != null) {
                AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
                if (desc != null && desc.getInheritTarget() != null) {
                    onNavigateToInherited.accept(desc.getInheritTarget());
                }
            }
        });
        inheritScopeLabel = new Label("(Language Defaults)");
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
        categoryTree.getSelectionModel().select(lineCommentItem);
        selectCategory("Line comment");
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

    private void loadScheme(String schemeName) {
        originalAttributes.clear();
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getProtocolBufferTextDescriptors()) {
            ColorAttribute attr = settings.getAttribute(schemeName, desc.getKey());
            if (attr != null) {
                originalAttributes.put(desc.getKey(), attr.clone());
            }
        }
        selectCategory(selectedKey);
    }

    public void selectCategory(String key) {
        this.selectedKey = key;
        suppressEvents = true;
        try {
            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(key);
            ColorAttribute current = settings.getAttribute(settings.getActiveSchemeName(), key);
            if (current == null && desc != null) {
                current = desc.getDefaultAttribute();
            }

            if (desc != null && desc.getInheritTarget() != null) {
                inheritBox.setVisible(true);
                inheritBox.setManaged(true);
                inheritTargetLabel.setText(desc.getInheritTarget());
                inheritScopeLabel.setText(desc.getInheritScope() != null ? desc.getInheritScope() : "(Language Defaults)");
                inheritCheck.setSelected(current != null && current.isInherit());
            } else {
                inheritBox.setVisible(false);
                inheritBox.setManaged(false);
                inheritCheck.setSelected(false);
            }

            if (current != null) {
                boldCheck.setSelected(current.isBold());
                italicCheck.setSelected(current.isItalic());

                foregroundCheck.setSelected(current.getForeground() != null);
                foregroundHex = current.getForeground() != null ? current.getForeground() : (desc != null && desc.getDefaultAttribute() != null ? desc.getDefaultAttribute().getForeground() : "#7A7E85");
                updateSwatch(foregroundSwatch, foregroundHex);

                backgroundCheck.setSelected(current.getBackground() != null);
                backgroundHex = current.getBackground() != null ? current.getBackground() : "";
                updateSwatch(backgroundSwatch, backgroundHex);

                errorStripeCheck.setSelected(current.getErrorStripeColor() != null);
                errorStripeHex = current.getErrorStripeColor() != null ? current.getErrorStripeColor() : "";
                updateSwatch(errorStripeSwatch, errorStripeHex);

                effectsCheck.setSelected(current.getEffectType() != null && current.getEffectType() != EffectType.NONE);
                effectsHex = current.getEffectColor() != null ? current.getEffectColor() : "";
                updateSwatch(effectsSwatch, effectsHex);
                if (current.getEffectType() != null && current.getEffectType() != EffectType.NONE) {
                    effectTypeCombo.setValue(current.getEffectType());
                } else {
                    effectTypeCombo.setValue(EffectType.BORDERED);
                }
            }
        } finally {
            suppressEvents = false;
        }
    }

    private void showColorChooser(String title, String initialColorHex, Consumer<String> onSelected) {
        Stage pickerStage = new Stage();
        pickerStage.initModality(Modality.APPLICATION_MODAL);
        pickerStage.initStyle(StageStyle.UTILITY);
        pickerStage.setTitle("Choose " + title + " Color");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #2B2D30;");

        Color initial = Color.web(initialColorHex != null && !initialColorHex.isBlank() ? initialColorHex : "#7A7E85");
        ColorPicker picker = new ColorPicker(initial);

        TextField hexField = new TextField(initialColorHex != null ? initialColorHex : "#7A7E85");
        hexField.setPromptText("#RRGGBB");
        hexField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #BCBEC4; -fx-border-color: #55575E;");

        picker.valueProperty().addListener((obs, oldVal, newVal) -> {
            String hex = String.format("#%02X%02X%02X",
                    (int) (newVal.getRed() * 255),
                    (int) (newVal.getGreen() * 255),
                    (int) (newVal.getBlue() * 255));
            hexField.setText(hex);
        });

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.getStyleClass().add("dialog-button");
        okBtn.setOnAction(e -> {
            onSelected.accept(hexField.getText().trim());
            pickerStage.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.getStyleClass().add("dialog-button");
        cancelBtn.setOnAction(e -> pickerStage.close());

        btnBox.getChildren().addAll(okBtn, cancelBtn);
        content.getChildren().addAll(new Label("Select color:"), picker, hexField, btnBox);

        Scene scene = new Scene(content, 280, 200);
        pickerStage.setScene(scene);
        pickerStage.showAndWait();
    }

    private void updatePreview() {
        codeLinesBox.getChildren().clear();

        // Protocol Buffer Text format code lines matching media_1790473030971.png
        // # This is an example of Protocol Buffer's text format.
        // # Unlike .proto files, only sh-style line comments are supported.
        //
        // name: "John Smith"
        //
        // pet {
        //   kind: DOG
        //   name: "Fluffy"
        //   tail_wagginess: 0.65f
        // }

        codeLinesBox.getChildren().addAll(
                buildLine(new Token("# This is an example of Protocol Buffer's text format.", "Line comment", "#7A7E85", false, true)),
                buildLine(new Token("# Unlike .proto files, only sh-style line comments are supported.", "Line comment", "#7A7E85", false, true)),
                buildLine(new Token("", null, null)),
                buildLine(
                        new Token("name", "Identifier", "#BCBEC4"),
                        new Token(":", "Operator", "#BCBEC4"),
                        new Token(" ", null, null),
                        new Token("\"John Smith\"", "String", "#6AAB73")
                ),
                buildLine(new Token("", null, null)),
                buildLine(
                        new Token("pet", "Identifier", "#BCBEC4"),
                        new Token(" ", null, null),
                        new Token("{", "Braces", "#BCBEC4")
                ),
                buildLine(
                        new Token("  ", null, null),
                        new Token("kind", "Identifier", "#BCBEC4"),
                        new Token(":", "Operator", "#BCBEC4"),
                        new Token(" ", null, null),
                        new Token("DOG", "Enum value", "#C77DBB")
                ),
                buildLine(
                        new Token("  ", null, null),
                        new Token("name", "Identifier", "#BCBEC4"),
                        new Token(":", "Operator", "#BCBEC4"),
                        new Token(" ", null, null),
                        new Token("\"Fluffy\"", "String", "#6AAB73")
                ),
                buildLine(
                        new Token("  ", null, null),
                        new Token("tail_wagginess", "Identifier", "#BCBEC4"),
                        new Token(":", "Operator", "#BCBEC4"),
                        new Token(" ", null, null),
                        new Token("0.65f", "Number", "#2AAC88")
                ),
                buildLine(
                        new Token("}", "Braces", "#BCBEC4")
                )
        );
    }

    private static class Token {
        final String text;
        final String attributeKey;
        final String defaultColor;
        final boolean isBold;
        final boolean isItalic;
        final EffectType effectType;
        final String effectColor;

        Token(String text, String attributeKey, String defaultColor) {
            this(text, attributeKey, defaultColor, false, false, EffectType.NONE, null);
        }

        Token(String text, String attributeKey, String defaultColor, boolean isBold, boolean isItalic) {
            this(text, attributeKey, defaultColor, isBold, isItalic, EffectType.NONE, null);
        }

        Token(String text, String attributeKey, String defaultColor, boolean isBold, boolean isItalic, EffectType effectType, String effectColor) {
            this.text = text;
            this.attributeKey = attributeKey;
            this.defaultColor = defaultColor;
            this.isBold = isBold;
            this.isItalic = isItalic;
            this.effectType = effectType;
            this.effectColor = effectColor;
        }
    }

    private HBox buildLine(Token... tokens) {
        HBox line = new HBox();
        line.setAlignment(Pos.CENTER_LEFT);

        for (Token tok : tokens) {
            Label label = new Label(tok.text);
            label.setFont(Font.font("Monospaced", 12.5));

            String fg = tok.defaultColor != null ? tok.defaultColor : "#BCBEC4";
            String bg = null;
            boolean bold = tok.isBold;
            boolean italic = tok.isItalic;
            EffectType eff = tok.effectType;
            String effColor = tok.effectColor;

            if (tok.attributeKey != null) {
                ColorAttribute attr = settings.resolveAttribute(settings.getActiveSchemeName(), tok.attributeKey);
                if (attr != null) {
                    if (attr.getForeground() != null) fg = attr.getForeground();
                    if (attr.getBackground() != null) bg = attr.getBackground();
                    if (attr.isBold()) bold = true;
                    if (attr.isItalic()) italic = true;
                    if (attr.getEffectType() != null && attr.getEffectType() != EffectType.NONE) {
                        eff = attr.getEffectType();
                        effColor = attr.getEffectColor();
                    }
                }

                final String targetKey = tok.attributeKey;
                label.setOnMouseClicked(e -> {
                    selectKeyInTree(targetKey);
                    selectCategory(targetKey);
                });
                label.setStyle("-fx-cursor: hand;");
            }

            StringBuilder css = new StringBuilder();
            css.append("-fx-text-fill: ").append(fg).append(";");
            if (bg != null && !bg.isBlank()) {
                css.append(" -fx-background-color: ").append(bg).append("; -fx-padding: 0 1 0 1;");
            }
            if (bold) {
                css.append(" -fx-font-weight: bold;");
            }
            if (italic) {
                css.append(" -fx-font-style: italic;");
            }
            if (eff == EffectType.BORDERED && effColor != null) {
                css.append(" -fx-border-color: ").append(effColor).append("; -fx-border-width: 1; -fx-padding: 0 1 0 1;");
            } else if (eff == EffectType.UNDERSCORED && effColor != null) {
                css.append(" -fx-border-color: transparent transparent ").append(effColor).append(" transparent; -fx-border-width: 0 0 1 0;");
            }

            label.setStyle(label.getStyle() + " " + css);
            line.getChildren().add(label);
        }
        return line;
    }

    private void selectKeyInTree(String key) {
        for (TreeItem<String> item : rootItem.getChildren()) {
            if (item.getValue().equals(key)) {
                categoryTree.getSelectionModel().select(item);
                return;
            }
        }
    }

    private void markModified() {
        isModified = true;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnNavigateToInheritedListener(Consumer<String> listener) {
        this.onNavigateToInherited = listener;
    }

    public boolean isModified() {
        return isModified;
    }

    public void apply() {
        if (!isModified) return;

        ColorAttribute current = new ColorAttribute();
        current.setInherit(inheritCheck.isSelected());
        if (foregroundCheck.isSelected() && foregroundHex != null) {
            current.setForeground(foregroundHex);
        }
        if (backgroundCheck.isSelected() && backgroundHex != null) {
            current.setBackground(backgroundHex);
        }
        if (errorStripeCheck.isSelected() && errorStripeHex != null) {
            current.setErrorStripeColor(errorStripeHex);
        }
        if (effectsCheck.isSelected()) {
            current.setEffectType(effectTypeCombo.getValue());
            current.setEffectColor(effectsHex);
        }
        current.setBold(boldCheck.isSelected());
        current.setItalic(italicCheck.isSelected());

        AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
        if (desc != null) {
            current.setInheritFrom(desc.getInheritTarget());
        }

        settings.setAttribute(settings.getActiveSchemeName(), selectedKey, current);
        isModified = false;
        loadScheme(settings.getActiveSchemeName());
        updatePreview();
    }

    public void reset() {
        for (Map.Entry<String, ColorAttribute> entry : originalAttributes.entrySet()) {
            settings.setAttribute(settings.getActiveSchemeName(), entry.getKey(), entry.getValue());
        }
        isModified = false;
        loadScheme(settings.getActiveSchemeName());
        updatePreview();
    }

    public TreeView<String> getCategoryTree() {
        return categoryTree;
    }

    public ColorSchemeHeaderBar getHeaderBar() {
        return headerBar;
    }

    public boolean isInheritChecked() {
        return inheritCheck.isSelected();
    }

    public void selectTreeItem(String key) {
        selectKeyInTree(key);
        selectCategory(key);
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
