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
 * Editor > Color Scheme > JVM Logging settings page.
 * Matches 1:1 with reference images media_1790422729751.png & media_1790422751274.png:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - Upper Section:
 *     Left: TreeView with "Classes" (Class name) and "Log string" (Placeholder).
 *     Right: Attribute Editor panel with Bold/Italic, Foreground, Background,
 *            Error stripe mark, Effects with "Dotted Line" dropdown, and Inherit checkbox/link.
 * - Lower Section: Code preview:
 *     com.example.ClassName
 *     log.info("{} {}", "arg1", "arg2")
 *     With ClassName dotted line effect, placeholder highlighting, and bidirectional navigation.
 */
public class SettingsColorSchemeJvmLoggingPage extends VBox {

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

    private final VBox inheritBox;
    private final CheckBox inheritCheck;
    private final Hyperlink inheritLink;

    // Preview controls
    private final ScrollPane previewScrollPane;
    private final VBox codeLinesBox;

    // State
    private String selectedKey = "Log string // Placeholder";
    private String foregroundHex = "#CF8E6D";
    private String backgroundHex = null;
    private String errorStripeHex = null;
    private String effectsHex = null;
    private boolean suppressEvents = false;
    private boolean modified = false;

    private Runnable onModifiedListener;

    public SettingsColorSchemeJvmLoggingPage() {
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
            if (newVal != null && newVal.getValue() != null && newVal.isLeaf()) {
                TreeItem<String> parent = newVal.getParent();
                String fullKey = (parent != null && parent != root)
                        ? parent.getValue() + " // " + newVal.getValue()
                        : newVal.getValue();
                selectedKey = fullKey;
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

        // Inherit Box
        inheritBox = new VBox(6);
        inheritBox.setPadding(new Insets(8, 0, 0, 0));
        inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-cursor: hand;");
        inheritCheck.selectedProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents) {
                onInheritToggled(nv);
            }
        });

        inheritLink = new Hyperlink("String->Escape sequence->Valid (Language Defaults)");
        inheritLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-padding: 0 0 0 20; -fx-border-color: transparent;");
        inheritBox.getChildren().addAll(inheritCheck, inheritLink);

        attributeEditorBox.getChildren().addAll(
                fontStyleRow,
                foregroundRow,
                backgroundRow,
                errorStripeRow,
                effectsRow,
                effectTypeRow,
                inheritBox
        );

        HBox topArea = new HBox(16, categoryTree, attributeEditorBox);
        topArea.setPrefHeight(250);
        topArea.setMinHeight(220);

        // 4. Code Preview
        codeLinesBox = new VBox(8);
        codeLinesBox.setPadding(new Insets(16, 20, 16, 20));
        codeLinesBox.setStyle("-fx-background-color: #1E1F22;");
        HBox.setHgrow(codeLinesBox, Priority.ALWAYS);

        previewScrollPane = new ScrollPane(codeLinesBox);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle(
                "-fx-background: #1E1F22; -fx-background-color: #1E1F22; " +
                "-fx-border-color: #2B2D30; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        previewScrollPane.setPrefHeight(250);
        previewScrollPane.setMinHeight(180);
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        getChildren().addAll(headerBar, topArea, previewScrollPane);

        // Initial selection: Placeholder
        selectTreeItem("Placeholder");
        updatePreview();
    }

    private void buildCategoryTree(TreeItem<String> root) {
        TreeItem<String> classesGroup = new TreeItem<>("Classes");
        classesGroup.setExpanded(true);
        classesGroup.getChildren().add(new TreeItem<>("Class name"));

        TreeItem<String> logStringGroup = new TreeItem<>("Log string");
        logStringGroup.setExpanded(true);
        logStringGroup.getChildren().add(new TreeItem<>("Placeholder"));

        root.getChildren().addAll(classesGroup, logStringGroup);
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
        attr.setInherit(inheritCheck.isSelected());

        s.setAttribute(s.getActiveSchemeName(), selectedKey, attr);
        notifyModified();
        updatePreview();
    }

    private void onInheritToggled(boolean inherit) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute raw = s.getAttribute(s.getActiveSchemeName(), selectedKey);
        if (raw == null) {
            EditorColorSchemeSettings.AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
            raw = desc != null ? desc.getDefaultAttribute() : new ColorAttribute();
        }
        raw.setInherit(inherit);
        s.setAttribute(s.getActiveSchemeName(), selectedKey, raw);
        loadAttributesForSelectedKey();
        notifyModified();
        updatePreview();
    }

    public void loadAttributesForSelectedKey() {
        suppressEvents = true;
        try {
            EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
            EditorColorSchemeSettings.AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
            ColorAttribute raw = s.getAttribute(s.getActiveSchemeName(), selectedKey);
            if (raw == null && desc != null) {
                raw = desc.getDefaultAttribute();
            }

            ColorAttribute effective = s.resolveAttribute(s.getActiveSchemeName(), selectedKey);

            boolean isInherit = raw != null && raw.isInherit() && desc != null && desc.hasInheritance();
            inheritBox.setVisible(desc != null && desc.hasInheritance());
            inheritCheck.setSelected(isInherit);

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

    public void selectTreeItem(String leafName) {
        if (leafName == null) return;
        selectTreeItemRecursive(categoryTree.getRoot(), leafName);
    }

    private boolean selectTreeItemRecursive(TreeItem<String> parent, String leafName) {
        for (TreeItem<String> child : parent.getChildren()) {
            if (child.isLeaf() && child.getValue().equalsIgnoreCase(leafName)) {
                categoryTree.getSelectionModel().select(child);
                TreeItem<String> p = child.getParent();
                String fullKey = (p != null && p != categoryTree.getRoot())
                        ? p.getValue() + " // " + child.getValue()
                        : child.getValue();
                selectedKey = fullKey;
                loadAttributesForSelectedKey();
                updatePreview();
                return true;
            }
            if (!child.isLeaf()) {
                if (selectTreeItemRecursive(child, leafName)) return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // JVM Logging Code Preview
    // -------------------------------------------------------------------------

    private void updatePreview() {
        codeLinesBox.getChildren().clear();

        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        String scheme = s.getActiveSchemeName();

        ColorAttribute classAttr = s.resolveAttribute(scheme, "Classes // Class name");
        ColorAttribute placeholderAttr = s.resolveAttribute(scheme, "Log string // Placeholder");

        String classEffectColor = classAttr != null && classAttr.getEffectColor() != null ? classAttr.getEffectColor() : "#888880";
        EffectType classEffect = classAttr != null ? classAttr.getEffectType() : EffectType.DOTTED_LINE;

        String placeholderFg = placeholderAttr != null && placeholderAttr.getForeground() != null ? placeholderAttr.getForeground() : "#CF8E6D";
        boolean placeholderBold = placeholderAttr != null && placeholderAttr.isBold();

        // Line 1: com.example.ClassName
        HBox line1 = new HBox();
        line1.setAlignment(Pos.CENTER_LEFT);

        Text pkgText = new Text("com.example.");
        pkgText.setFont(Font.font("JetBrains Mono", 13));
        pkgText.setFill(Color.web("#BCBEC4"));

        Label classLabel = new Label("ClassName");
        classLabel.setFont(Font.font("JetBrains Mono", 13));
        classLabel.setStyle(String.format(
                "-fx-text-fill: #BCBEC4; -fx-cursor: hand; %s",
                renderEffectCss(classEffect, classEffectColor)
        ));
        classLabel.setOnMouseClicked(e -> selectTreeItem("Class name"));

        if (selectedKey.contains("Class name")) {
            classLabel.setStyle(classLabel.getStyle() + " -fx-background-color: #2E436E;");
        }

        line1.getChildren().addAll(pkgText, classLabel);

        // Line 2: Empty line
        HBox line2 = new HBox();
        line2.setMinHeight(14);

        // Line 3: log.info("{} {}", "arg1", "arg2")
        HBox line3 = new HBox();
        line3.setAlignment(Pos.CENTER_LEFT);

        Text logInfo = new Text("log.info(\"");
        logInfo.setFont(Font.font("JetBrains Mono", 13));
        logInfo.setFill(Color.web("#6AAB73"));

        Label ph1 = createPlaceholderLabel("{}", placeholderFg, placeholderBold);
        ph1.setOnMouseClicked(e -> selectTreeItem("Placeholder"));

        Text spaceStr = new Text(" ");
        spaceStr.setFont(Font.font("JetBrains Mono", 13));
        spaceStr.setFill(Color.web("#6AAB73"));

        Label ph2 = createPlaceholderLabel("{}", placeholderFg, placeholderBold);
        ph2.setOnMouseClicked(e -> selectTreeItem("Placeholder"));

        Text rest = new Text("\", \"arg1\", \"arg2\")");
        rest.setFont(Font.font("JetBrains Mono", 13));
        rest.setFill(Color.web("#6AAB73"));

        line3.getChildren().addAll(logInfo, ph1, spaceStr, ph2, rest);

        codeLinesBox.getChildren().addAll(line1, line2, line3);
    }

    private Label createPlaceholderLabel(String text, String fg, boolean bold) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("JetBrains Mono", bold ? FontWeight.BOLD : FontWeight.NORMAL, 13));
        lbl.setStyle(String.format(
                "-fx-text-fill: %s; -fx-cursor: hand; %s",
                fg,
                selectedKey.contains("Placeholder") ? "-fx-background-color: #2E436E; -fx-background-radius: 2;" : ""
        ));
        return lbl;
    }

    private String renderEffectCss(EffectType type, String colorHex) {
        if (type == null || type == EffectType.NONE || colorHex == null) return "";
        return switch (type) {
            case UNDERSCORED -> String.format("-fx-border-color: %s; -fx-border-width: 0 0 1 0;", colorHex);
            case BOLD_UNDERSCORED -> String.format("-fx-border-color: %s; -fx-border-width: 0 0 2 0;", colorHex);
            case DOTTED_LINE -> String.format("-fx-border-color: %s; -fx-border-style: dotted; -fx-border-width: 0 0 1 0;", colorHex);
            case BORDERED -> String.format("-fx-border-color: %s; -fx-border-width: 1 1 1 1;", colorHex);
            case STRIKEOUT -> "-fx-underline: false;";
            default -> String.format("-fx-border-color: %s; -fx-border-width: 0 0 1 0;", colorHex);
        };
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
    public CheckBox getInheritCheck() { return inheritCheck; }
    public Hyperlink getInheritLink() { return inheritLink; }
    public ScrollPane getPreviewScrollPane() { return previewScrollPane; }
    public VBox getCodeLinesBox() { return codeLinesBox; }
}
