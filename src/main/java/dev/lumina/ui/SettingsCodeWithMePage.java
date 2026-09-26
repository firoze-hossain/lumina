// SettingsCodeWithMePage.java
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
 * Editor > Color Scheme > Code With Me settings page.
 * Matches 1:1 with reference image:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - Upper Section:
 *     Left: TreeView with 12 collaborative user items (User 1..6 cursor & selection).
 *     Right: Attribute Editor panel (Bold/Italic, Foreground, Background, Error stripe mark, Effects).
 * - Lower Section: Realistic collaborative JSON code preview with multi-user cursor flags,
 *   selection highlights, and bi-directional click navigation.
 */
public class SettingsCodeWithMePage extends VBox {

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

    // Live preview
    private final VBox codeLinesBox;
    private final ScrollPane previewScrollPane;

    // State
    private String selectedKey = "User 2 selection";
    private String foregroundHex = null;
    private String backgroundHex = null;
    private String errorStripeHex = null;
    private String effectsHex = null;
    private boolean suppressEvents = false;
    private boolean modified = false;

    private Runnable onModifiedListener;

    public SettingsCodeWithMePage() {
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
        categoryTree.setPrefWidth(320);
        categoryTree.setMinWidth(260);
        categoryTree.setCellFactory(tv -> new TreeCell<>() {
            {
                setOnMouseEntered(e -> {
                    if (!isEmpty() && getItem() != null && !isSelected()) {
                        setStyle("-fx-background-color: #35373B; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 6 3 6;");
                    }
                });
                setOnMouseExited(e -> {
                    if (!isEmpty() && getItem() != null && !isSelected()) {
                        setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 6 3 6;");
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #2B2D30;");
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
                    setStyle("-fx-background-color: #2B2D30;");
                } else if (isSelected()) {
                    setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-padding: 3 6 3 6;");
                } else {
                    setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 6 3 6;");
                }
            }
        });

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
                selectedKey = selected.getValue();
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
                EffectType.STRIKEOUT
        );
        effectTypeCombo.setValue(EffectType.BORDERED);
        effectTypeCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;"
        );
        effectTypeCombo.setPrefWidth(130);
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

        // Top horizontal split: Tree on left, Attribute Editor on right
        HBox topArea = new HBox(16, categoryTree, attributeEditorBox);
        topArea.setPrefHeight(250);
        topArea.setMinHeight(220);

        // 4. Live JSON Preview with multi-user cursors & selections
        codeLinesBox = new VBox(2);
        codeLinesBox.setStyle("-fx-background-color: #1E1F22; -fx-padding: 10 12 10 12;");
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

        // Initial selection: "User 2 selection" matching Image 2
        selectTreeItem(selectedKey);
        updatePreview();
    }

    private void buildCategoryTree(TreeItem<String> root) {
        for (EditorColorSchemeSettings.AttributesDescriptor desc : EditorColorSchemeSettings.getCodeWithMeDescriptors()) {
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
            ColorAttribute raw = s.getAttribute(s.getActiveSchemeName(), selectedKey);
            if (raw == null) {
                raw = (desc != null) ? desc.getDefaultAttribute() : new ColorAttribute();
            }

            ColorAttribute effective = s.resolveAttribute(s.getActiveSchemeName(), selectedKey);

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
                effectTypeCombo.setValue(EffectType.BORDERED);
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
                return;
            }
        }
    }

    private void updatePreview() {
        codeLinesBox.getChildren().clear();

        // Line 1: // JSON File as an example
        addLine(
                commentToken("// JSON File as an example")
        );

        // Line 2: /* Some block comment */
        addLine(
                commentToken("/* Some block comment */")
        );

        // Line 3: "keywords": [
        addLine(
                stringToken("\"keywords\""),
                plainToken(": [")
        );

        // Line 4:   true,
        addLine(
                plainToken("  "),
                keywordToken("true"),
                plainToken(",")
        );

        // Line 5:   false,
        addLine(
                plainToken("  "),
                keywordToken("false"),
                plainToken(",")
        );

        // Line 6:   null
        addLine(
                plainToken("  "),
                selectionToken("null", "User 4 selection", "User 4 cursor", "User 4")
        );

        // Line 7: ],
        addLine(
                plainToken("],")
        );

        // Line 8: "strings": {
        addLine(
                stringToken("\"strings\""),
                plainToken(": {")
        );

        // Line 9:   "no escapes": "pseudopolynomiality",
        addLine(
                plainToken("  "),
                stringToken("\"no escapes\""),
                plainToken(": "),
                cursorToken("User 1 cursor", "User 1"),
                stringToken("\"pseudopolynomiality\""),
                plainToken(",")
        );

        // Line 10:   "escapes": "C-style\nandunicode\u0021"
        addLine(
                plainToken("  "),
                stringToken("\"escapes\""),
                plainToken(": "),
                cursorToken("User 2 cursor", "User 2"),
                selectionToken("\"C-style\\nandunicode\\u0021\"", "User 2 selection", "User 2 cursor", null)
        );

        // Line 11: },
        addLine(
                plainToken("},")
        );

        // Line 12: "Some numbers": [
        addLine(
                stringToken("\"Some numbers\""),
                plainToken(": [")
        );

        // Line 13:   42,
        addLine(
                plainToken("  "),
                numberToken("42"),
                plainToken(",")
        );

        // Line 14:   -0.0e-0,
        addLine(
                plainToken("  "),
                numberToken("-0.0e-0"),
                plainToken(",")
        );

        // Line 15:   6.626e-34
        addLine(
                plainToken("  "),
                cursorToken("User 3 cursor", "User 3"),
                numberToken("6.626e-34")
        );

        // Line 16: ]
        addLine(
                plainToken("]")
        );
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

    private Node plainToken(String text) {
        Text t = new Text(text);
        t.setFill(Color.web("#BCBEC4"));
        t.setFont(Font.font("JetBrains Mono", 12.5));
        return t;
    }

    private Node commentToken(String text) {
        Text t = new Text(text);
        t.setFill(Color.web("#7A7E85"));
        t.setFont(Font.font("JetBrains Mono", FontPosture.ITALIC, 12.5));
        return t;
    }

    private Node stringToken(String text) {
        Text t = new Text(text);
        t.setFill(Color.web("#6AAB73"));
        t.setFont(Font.font("JetBrains Mono", 12.5));
        return t;
    }

    private Node keywordToken(String text) {
        Text t = new Text(text);
        t.setFill(Color.web("#CF8E6D"));
        t.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 12.5));
        return t;
    }

    private Node numberToken(String text) {
        Text t = new Text(text);
        t.setFill(Color.web("#2AACB8"));
        t.setFont(Font.font("JetBrains Mono", 12.5));
        return t;
    }

    private Node cursorToken(String userCursorKey, String userName) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = s.resolveAttribute(s.getActiveSchemeName(), userCursorKey);
        String colorHex = (attr != null && attr.getForeground() != null) ? attr.getForeground() :
                (attr != null && attr.getBackground() != null ? attr.getBackground() : "#59A869");

        HBox cursorBox = new HBox(1);
        cursorBox.setAlignment(Pos.CENTER_LEFT);
        cursorBox.setStyle("-fx-cursor: hand;");

        // Small badge with username
        Label badge = new Label(userName);
        badge.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: #1E1F22; -fx-font-size: 9.5px; -fx-font-weight: bold; " +
                "-fx-padding: 0 3 0 3; -fx-background-radius: 2;",
                colorHex
        ));

        // Caret bar
        Region caretBar = new Region();
        caretBar.setPrefSize(2, 14);
        caretBar.setMinSize(2, 14);
        caretBar.setMaxSize(2, 14);
        caretBar.setStyle(String.format("-fx-background-color: %s;", colorHex));

        cursorBox.getChildren().addAll(badge, caretBar);

        if (userCursorKey.equals(selectedKey)) {
            cursorBox.setStyle("-fx-border-color: #525866; -fx-border-width: 1; -fx-border-radius: 2; -fx-cursor: hand;");
        }

        cursorBox.setOnMouseClicked(e -> selectTreeItem(userCursorKey));
        return cursorBox;
    }

    private Node selectionToken(String text, String userSelectionKey, String userCursorKey, String userName) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = s.resolveAttribute(s.getActiveSchemeName(), userSelectionKey);
        String bgHex = (attr != null && attr.getBackground() != null) ? attr.getBackground() : "#2D4733";

        StackPane container = new StackPane();
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle("-fx-cursor: hand;");

        HBox contentBox = new HBox(2);
        contentBox.setAlignment(Pos.CENTER_LEFT);

        if (userName != null && userCursorKey != null) {
            contentBox.getChildren().add(cursorToken(userCursorKey, userName));
        }

        Text t = new Text(text);
        t.setFill(Color.web("#BCBEC4"));
        t.setFont(Font.font("JetBrains Mono", 12.5));
        contentBox.getChildren().add(t);

        StringBuilder style = new StringBuilder();
        style.append(String.format("-fx-background-color: %s; -fx-padding: 1 3 1 3; -fx-background-radius: 2; ", bgHex));

        if (userSelectionKey.equals(selectedKey)) {
            style.append("-fx-border-color: #525866; -fx-border-width: 1; -fx-border-radius: 2; ");
        }

        style.append("-fx-cursor: hand;");
        container.setStyle(style.toString());
        container.getChildren().add(contentBox);

        container.setOnMouseClicked(e -> selectTreeItem(userSelectionKey));
        return container;
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    private void notifyModified() {
        this.modified = true;
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public boolean isModified() {
        return modified;
    }

    public void apply() {
        EditorColorSchemeSettings.getInstance().save();
        this.modified = false;
    }

    public void reset() {
        String active = EditorColorSchemeSettings.getInstance().getActiveSchemeName();
        EditorColorSchemeSettings.getInstance().restoreDefaults(active);
        loadAttributesForSelectedKey();
        updatePreview();
        this.modified = false;
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

    public VBox getAttributeEditorBox() {
        return attributeEditorBox;
    }
}