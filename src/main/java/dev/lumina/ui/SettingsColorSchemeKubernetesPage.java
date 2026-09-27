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
 * 1:1 visual & functional replication of Editor > Color Scheme > Kubernetes.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemeKubernetesPage extends VBox {

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

    private String selectedKey = "Enumeration";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemeKubernetesPage() {
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

        TreeItem<String> enumerationItem = new TreeItem<>("Enumeration");

        rootItem.getChildren().addAll(
                new TreeItem<>("Boolean"),
                enumerationItem,
                new TreeItem<>("Group, version, kind"),
                new TreeItem<>("Number"),
                new TreeItem<>("Semantic highlighting")
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
        effectTypeCombo.getItems().addAll(EffectType.values());
        effectTypeCombo.setValue(EffectType.BORDERED);
        effectTypeCombo.setPrefWidth(140);
        effectTypeCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5;"
        );

        HBox effTypeRow = new HBox(8);
        effTypeRow.setAlignment(Pos.CENTER_RIGHT);
        effTypeRow.getChildren().add(effectTypeCombo);

        // Inheritance section
        inheritBox = new VBox(4);
        inheritBox.setPadding(new Insets(10, 0, 0, 0));
        HBox inheritCheckRow = new HBox(8);
        inheritCheckRow.setAlignment(Pos.CENTER_LEFT);
        inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        inheritCheckRow.getChildren().add(inheritCheck);

        HBox inheritTargetRow = new HBox(6);
        inheritTargetRow.setPadding(new Insets(0, 0, 0, 20));
        inheritTargetRow.setAlignment(Pos.CENTER_LEFT);
        inheritTargetLabel = new Hyperlink("");
        inheritTargetLabel.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-underline: false; -fx-padding: 0;");
        inheritTargetLabel.setOnAction(e -> {
            if (onNavigateToInherited != null && inheritTargetLabel.getText() != null) {
                onNavigateToInherited.accept(inheritTargetLabel.getText());
            }
        });
        inheritScopeLabel = new Label("");
        inheritScopeLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");
        inheritTargetRow.getChildren().addAll(inheritTargetLabel, inheritScopeLabel);

        inheritBox.getChildren().addAll(inheritCheckRow, inheritTargetRow);

        attributeEditor.getChildren().addAll(
                fontBox,
                fgRow,
                bgRow,
                esRow,
                effRow,
                effTypeRow,
                inheritBox
        );

        // Split / Upper view
        HBox upperSplit = new HBox(12);
        upperSplit.setPrefHeight(240);
        HBox.setHgrow(categoryTree, Priority.ALWAYS);
        upperSplit.getChildren().addAll(categoryTree, attributeEditor);

        // ---------------- Bottom Preview ----------------
        previewScrollPane = new ScrollPane();
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setPrefHeight(230);
        previewScrollPane.setStyle("-fx-background-color: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4;");

        codeLinesBox = new VBox(2);
        codeLinesBox.setPadding(new Insets(8, 12, 12, 12));
        codeLinesBox.setStyle("-fx-background-color: #1E1F22;");
        previewScrollPane.setContent(codeLinesBox);

        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);
        getChildren().addAll(headerBar, upperSplit, previewScrollPane);

        // Bind control listeners
        setupControlListeners();

        // Initial scheme load & selection
        loadScheme(settings.getActiveSchemeName());
        categoryTree.getSelectionModel().select(enumerationItem);
        selectCategory("Enumeration");
        updatePreview();
    }

    private Button createSwatchButton() {
        Button swatch = new Button();
        swatch.setPrefSize(48, 20);
        swatch.setMinSize(48, 20);
        swatch.setMaxSize(48, 20);
        swatch.setStyle(
                "-fx-background-color: transparent; -fx-border-color: #4E5157; " +
                "-fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 10px; -fx-padding: 0;"
        );
        return swatch;
    }

    private void updateSwatch(Button swatch, String hex) {
        if (hex != null && !hex.isBlank()) {
            String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
            String textCol = isBrightColor(cleanHex) ? "#1E1F22" : "#FFFFFF";
            swatch.setText(cleanHex.toUpperCase());
            swatch.setStyle(
                    "-fx-background-color: #" + cleanHex + "; -fx-text-fill: " + textCol + "; " +
                    "-fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 10px; -fx-padding: 0;"
            );
        } else {
            swatch.setText("");
            swatch.setStyle(
                    "-fx-background-color: transparent; -fx-border-color: #4E5157; " +
                    "-fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 10px; -fx-padding: 0;"
            );
        }
    }

    private boolean isBrightColor(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            double lum = 0.299 * r + 0.587 * g + 0.114 * b;
            return lum > 160;
        } catch (Exception e) {
            return false;
        }
    }

    private void openColorPickerPopup(Button ownerButton, String currentHex, Consumer<String> onColorChosen) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UTILITY);
        popup.setTitle("Choose Color");

        ColorPicker picker = new ColorPicker();
        if (currentHex != null && !currentHex.isBlank()) {
            try {
                picker.setValue(Color.web(currentHex.startsWith("#") ? currentHex : "#" + currentHex));
            } catch (Exception ignored) {
            }
        }

        Button okBtn = new Button("Choose");
        okBtn.getStyleClass().add("dialog-primary");
        okBtn.setOnAction(e -> {
            Color c = picker.getValue();
            if (c != null) {
                int r = (int) Math.round(c.getRed() * 255);
                int g = (int) Math.round(c.getGreen() * 255);
                int b = (int) Math.round(c.getBlue() * 255);
                String hex = String.format("%02X%02X%02X", r, g, b);
                onColorChosen.accept(hex);
            }
            popup.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-secondary");
        cancelBtn.setOnAction(e -> popup.close());

        HBox btnRow = new HBox(8, okBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(12, picker, btnRow);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #2B2D30;");

        popup.setScene(new Scene(content));
        popup.showAndWait();
    }

    private void setupControlListeners() {
        boldCheck.setOnAction(e -> onAttributeControlChanged());
        italicCheck.setOnAction(e -> onAttributeControlChanged());

        foregroundCheck.setOnAction(e -> onAttributeControlChanged());
        foregroundSwatch.setOnAction(e -> openColorPickerPopup(foregroundSwatch, foregroundHex, hex -> {
            foregroundHex = hex;
            foregroundCheck.setSelected(true);
            updateSwatch(foregroundSwatch, foregroundHex);
            onAttributeControlChanged();
        }));

        backgroundCheck.setOnAction(e -> onAttributeControlChanged());
        backgroundSwatch.setOnAction(e -> openColorPickerPopup(backgroundSwatch, backgroundHex, hex -> {
            backgroundHex = hex;
            backgroundCheck.setSelected(true);
            updateSwatch(backgroundSwatch, backgroundHex);
            onAttributeControlChanged();
        }));

        errorStripeCheck.setOnAction(e -> onAttributeControlChanged());
        errorStripeSwatch.setOnAction(e -> openColorPickerPopup(errorStripeSwatch, errorStripeHex, hex -> {
            errorStripeHex = hex;
            errorStripeCheck.setSelected(true);
            updateSwatch(errorStripeSwatch, errorStripeHex);
            onAttributeControlChanged();
        }));

        effectsCheck.setOnAction(e -> onAttributeControlChanged());
        effectsSwatch.setOnAction(e -> openColorPickerPopup(effectsSwatch, effectsHex, hex -> {
            effectsHex = hex;
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
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getKubernetesDescriptors()) {
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
                        new Token("apiVersion", null, "#CF8E6D"),
                        new Token(": ", null, "#BCBEC4"),
                        new Token("v1", "Group, version, kind", "#9876AA")
                ),
                List.of(
                        new Token("kind", null, "#CF8E6D"),
                        new Token(": ", null, "#BCBEC4"),
                        new Token("Pod", "Group, version, kind", "#9876AA")
                ),
                List.of(
                        new Token("metadata", null, "#CF8E6D"),
                        new Token(":", null, "#BCBEC4")
                ),
                List.of(
                        new Token("  name", null, "#CF8E6D"),
                        new Token(": ", null, "#BCBEC4"),
                        new Token("demo", null, "#6AAB73")
                ),
                List.of(
                        new Token("spec", null, "#CF8E6D"),
                        new Token(":", null, "#BCBEC4")
                ),
                List.of(
                        new Token("  containers", null, "#CF8E6D"),
                        new Token(":", null, "#BCBEC4")
                ),
                List.of(
                        new Token("    - name", null, "#CF8E6D"),
                        new Token(": ", null, "#BCBEC4"),
                        new Token("demo", null, "#6AAB73")
                ),
                List.of(
                        new Token("      imagePullPolicy", null, "#CF8E6D"),
                        new Token(": ", null, "#BCBEC4"),
                        new Token("Always", "Enumeration", "#56A8F5")
                ),
                List.of(
                        new Token("      ports", null, "#CF8E6D"),
                        new Token(":", null, "#BCBEC4")
                ),
                List.of(
                        new Token("        - name", null, "#CF8E6D"),
                        new Token(": ", null, "#BCBEC4"),
                        new Token("default", null, "#6AAB73")
                ),
                List.of(
                        new Token("          containerPort", null, "#CF8E6D"),
                        new Token(": ", null, "#BCBEC4"),
                        new Token("8080", "Number", "#2AAC88")
                ),
                List.of(
                        new Token("          protocol", null, "#CF8E6D"),
                        new Token(": ", null, "#BCBEC4"),
                        new Token("TCP", "Enumeration", "#56A8F5")
                ),
                List.of(
                        new Token("      volumeMounts", null, "#CF8E6D"),
                        new Token(":", null, "#BCBEC4")
                ),
                List.of(
                        new Token("        - mountPath", null, "#CF8E6D"),
                        new Token(": ", null, "#BCBEC4"),
                        new Token("~/test", null, "#6AAB73")
                ),
                List.of(
                        new Token("          name", null, "#CF8E6D"),
                        new Token(": ", null, "#BCBEC4"),
                        new Token("test", null, "#6AAB73")
                ),
                List.of(
                        new Token("          readOnly", null, "#CF8E6D"),
                        new Token(": ", null, "#BCBEC4"),
                        new Token("false", "Boolean", "#CF8E6D")
                )
        );

        for (List<Token> lineTokens : lines) {
            HBox lineBox = new HBox();
            lineBox.setAlignment(Pos.CENTER_LEFT);

            for (Token token : lineTokens) {
                Label label = new Label(token.text);
                label.setFont(Font.font("Monospaced", 12));

                String color = token.fallbackColor != null ? token.fallbackColor : "#BCBEC4";
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
                        isBold = attr.isBold();
                        isItalic = attr.isItalic();
                    }
                }

                StringBuilder style = new StringBuilder("-fx-text-fill: " + (color.startsWith("#") ? color : "#" + color) + ";");
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

    private TreeItem<String> findTreeItem(TreeItem<String> root, String value) {
        if (root == null) return null;
        if (Objects.equals(root.getValue(), value)) return root;
        for (TreeItem<String> child : root.getChildren()) {
            TreeItem<String> res = findTreeItem(child, value);
            if (res != null) return res;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Lifecycle & Public API
    // -------------------------------------------------------------------------

    public void apply() {
        ColorAttribute current = getCurrentEditorAttribute();
        settings.setAttribute(settings.getActiveSchemeName(), selectedKey, current);
        originalAttributes.put(selectedKey, current.clone());
        isModified = false;
        if (onModifiedListener != null) onModifiedListener.run();
        updatePreview();
    }

    public void reset() {
        ColorAttribute orig = originalAttributes.get(selectedKey);
        if (orig != null) {
            settings.setAttribute(settings.getActiveSchemeName(), selectedKey, orig.clone());
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

    public void setOnNavigateToInheritedListener(Consumer<String> listener) {
        this.onNavigateToInherited = listener;
    }

    public String getSelectedKey() {
        return selectedKey;
    }

    public TreeView<String> getCategoryTree() {
        return categoryTree;
    }

    public ColorSchemeHeaderBar getHeaderBar() {
        return headerBar;
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
