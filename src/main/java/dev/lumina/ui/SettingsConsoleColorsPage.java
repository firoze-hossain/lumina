// SettingsConsoleColorsPage.java
package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.*;

/**
 * Editor > Color Scheme > Console Colors settings page.
 * Provides descriptor-driven tree hierarchy, attribute editing (foreground, background,
 * error stripe mark, effects, inheritance), and live interactive terminal/console preview
 * with bidirectional navigation.
 */
public class SettingsConsoleColorsPage extends VBox {

    private final ColorSchemeHeaderBar headerBar;
    private final TreeView<String> categoryTree;
    private final VBox attributeEditorBox;

    // Checkboxes and controls for editing attributes
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
    private final Hyperlink inheritLink;
    private final Label inheritScopeLabel;

    // Live preview
    private final VBox consoleLinesBox;
    private final ScrollPane previewScrollPane;

    // State
    private String selectedKey = "Console // Error output";
    private String currentInheritedTargetKey = null;
    private String foregroundHex = null;
    private String backgroundHex = null;
    private String errorStripeHex = null;
    private String effectsHex = null;
    private boolean suppressEvents = false;
    private boolean modified = false;

    private final List<String> navigationHistory = new ArrayList<>();
    private int historyIndex = -1;
    private boolean isNavigatingHistory = false;

    private Runnable onModifiedListener;

    public SettingsConsoleColorsPage() {
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
                if (!selected.getChildren().isEmpty()) {
                    selectedKey = selected.getValue();
                    attributeEditorBox.setVisible(false);
                    attributeEditorBox.setManaged(false);
                    return;
                }
                attributeEditorBox.setVisible(true);
                attributeEditorBox.setManaged(true);

                selectedKey = buildFullKey(selected);
                recordNavigation(selectedKey);
                loadAttributesForSelectedKey();
                updatePreview();
            }
        });

        // Bold & Italic checkboxes
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
                updateSwatchButton(foregroundSwatch, null, false, false);
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
                updateSwatchButton(backgroundSwatch, null, false, false);
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
                updateSwatchButton(errorStripeSwatch, null, false, false);
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
                updateSwatchButton(effectsSwatch, null, false, false);
                onAttributeChanged();
            }
        });

        HBox effectsRow = new HBox(8, effectsCheck, effectsSwatch);
        effectsRow.setAlignment(Pos.CENTER_LEFT);

        HBox effectTypeRow = new HBox(8, new Region() {{ setPrefWidth(140); }}, effectTypeCombo);
        effectTypeRow.setAlignment(Pos.CENTER_LEFT);

        // Inheritance Box
        inheritBox = new VBox(4);
        inheritBox.setPadding(new Insets(12, 0, 0, 0));

        inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-cursor: hand;");
        inheritCheck.selectedProperty().addListener((o, ov, nv) -> onInheritChanged(nv));

        inheritLink = new Hyperlink();
        inheritLink.setStyle(
                "-fx-text-fill: #3574F0; -fx-font-size: 12.5px; -fx-padding: 0 0 0 22; " +
                "-fx-underline: false; -fx-cursor: hand;"
        );
        inheritLink.setOnAction(e -> {
            if (currentInheritedTargetKey != null) {
                selectTreeItem(currentInheritedTargetKey);
            }
        });

        inheritScopeLabel = new Label();
        inheritScopeLabel.setStyle("-fx-text-fill: #868991; -fx-font-size: 12px; -fx-padding: 0 0 0 22;");

        inheritBox.getChildren().addAll(inheritCheck, inheritLink, inheritScopeLabel);

        attributeEditorBox.getChildren().addAll(
                fontStyleRow,
                foregroundRow,
                backgroundRow,
                errorStripeRow,
                effectsRow,
                effectTypeRow,
                inheritBox
        );

        // Top horizontal split: Tree on left, Attribute Editor on right
        HBox topArea = new HBox(16, categoryTree, attributeEditorBox);
        topArea.setPrefHeight(250);
        topArea.setMinHeight(220);

        // 4. Interactive Live Console Preview
        consoleLinesBox = new VBox(3);
        consoleLinesBox.setStyle("-fx-background-color: #1E1F22; -fx-padding: 10 12 10 12;");
        HBox.setHgrow(consoleLinesBox, Priority.ALWAYS);

        previewScrollPane = new ScrollPane(consoleLinesBox);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle(
                "-fx-background: #1E1F22; -fx-background-color: #1E1F22; " +
                "-fx-border-color: #2B2D30; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        previewScrollPane.setPrefHeight(290);
        previewScrollPane.setMinHeight(200);
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        getChildren().addAll(headerBar, topArea, previewScrollPane);

        // Initial selection: "ANSI colors // Bright Black" matching Image 1
        selectTreeItem(selectedKey);
        updatePreview();
    }

    private void buildCategoryTree(TreeItem<String> root) {
        Map<String, TreeItem<String>> nodeMap = new LinkedHashMap<>();
        for (EditorColorSchemeSettings.AttributesDescriptor desc : EditorColorSchemeSettings.getConsoleColorsDescriptors()) {
            TreeItem<String> parent = root;
            StringBuilder pathAcc = new StringBuilder();
            for (String cat : desc.getCategoryPath()) {
                if (pathAcc.length() > 0) pathAcc.append(" // ");
                pathAcc.append(cat);
                String fullPath = pathAcc.toString();
                TreeItem<String> catNode = nodeMap.get(fullPath);
                if (catNode == null) {
                    catNode = new TreeItem<>(cat);
                    nodeMap.put(fullPath, catNode);
                    parent.getChildren().add(catNode);
                }
                parent = catNode;
            }
            TreeItem<String> leaf = new TreeItem<>(desc.getDisplayName());
            nodeMap.put(desc.getKey(), leaf);
            parent.getChildren().add(leaf);
        }
    }

    private String buildFullKey(TreeItem<String> item) {
        List<String> parts = new ArrayList<>();
        TreeItem<String> curr = item;
        while (curr != null && curr.getParent() != null) {
            parts.add(0, curr.getValue());
            curr = curr.getParent();
        }
        return String.join(" // ", parts);
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
            if (inheritBox.isVisible() && inheritCheck.isSelected()) {
                inheritCheck.setSelected(false);
            }
            openColorChooserDialog(btn.getText(), color -> {
                updateSwatchButton(btn, color, true, false);
                onColorSelected.accept(color);
            });
        });
    }

    private void updateSwatchButton(Button btn, String hex, boolean enabled, boolean isInherited) {
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
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: %s; -fx-padding: 0; -fx-opacity: %s;",
                    fullHex, textFill, isInherited ? "default" : "hand", isInherited ? "0.65" : "1.0"
            ));
            btn.setDisable(isInherited);
        } else {
            btn.setText("");
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 0;");
            btn.setDisable(isInherited || !enabled);
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

        if (inheritBox.isVisible()) {
            attr.setInherit(inheritCheck.isSelected());
            attr.setInheritFrom(currentInheritedTargetKey);
            attr.setInheritScope(inheritScopeLabel.getText());
        }

        s.setAttribute(s.getActiveSchemeName(), selectedKey, attr);
        notifyModified();
        updatePreview();
    }

    private void onInheritChanged(boolean inherit) {
        if (suppressEvents) return;
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute raw = s.getAttribute(s.getActiveSchemeName(), selectedKey);
        if (raw == null) raw = new ColorAttribute();
        raw.setInherit(inherit);
        if (inherit && currentInheritedTargetKey != null) {
            raw.setInheritFrom(currentInheritedTargetKey);
            raw.setInheritScope(inheritScopeLabel.getText());
        }
        s.setAttribute(s.getActiveSchemeName(), selectedKey, raw);
        loadAttributesForSelectedKey();
        notifyModified();
        updatePreview();
    }

    public void loadAttributesForSelectedKey() {
        suppressEvents = true;
        try {
            EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
            selectedKey = EditorColorSchemeSettings.normalizeKey(selectedKey);

            EditorColorSchemeSettings.AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
            ColorAttribute raw = s.getAttribute(s.getActiveSchemeName(), selectedKey);
            if (raw == null) {
                raw = (desc != null) ? desc.getDefaultAttribute() : new ColorAttribute();
            }

            boolean hasInheritDef = desc != null && desc.hasInheritance();
            if (hasInheritDef || (raw.getInheritFrom() != null && !raw.getInheritFrom().isBlank())) {
                inheritBox.setVisible(true);
                inheritBox.setManaged(true);
                inheritCheck.setSelected(raw.isInherit());
                String target = raw.getInheritFrom() != null ? raw.getInheritFrom() : desc.getInheritFrom();
                currentInheritedTargetKey = target;
                inheritLink.setText(target != null ? target.replace(" // ", " -> ") : "");
                String scope = raw.getInheritScope() != null ? raw.getInheritScope() : (desc != null ? desc.getInheritScope() : "(Console Colors)");
                inheritScopeLabel.setText(scope != null ? scope : "(Console Colors)");
            } else {
                inheritBox.setVisible(false);
                inheritBox.setManaged(false);
                currentInheritedTargetKey = null;
            }

            ColorAttribute effective = s.resolveAttribute(s.getActiveSchemeName(), selectedKey);
            boolean isInherited = inheritBox.isVisible() && inheritCheck.isSelected();

            boldCheck.setSelected(effective.isBold());
            italicCheck.setSelected(effective.isItalic());
            boldCheck.setDisable(isInherited);
            italicCheck.setDisable(isInherited);

            foregroundHex = effective.getForeground();
            foregroundCheck.setSelected(foregroundHex != null);
            foregroundCheck.setDisable(isInherited);
            updateSwatchButton(foregroundSwatch, foregroundHex, foregroundCheck.isSelected(), isInherited);

            backgroundHex = effective.getBackground();
            backgroundCheck.setSelected(backgroundHex != null);
            backgroundCheck.setDisable(isInherited);
            updateSwatchButton(backgroundSwatch, backgroundHex, backgroundCheck.isSelected(), isInherited);

            errorStripeHex = effective.getErrorStripeColor();
            errorStripeCheck.setSelected(errorStripeHex != null);
            errorStripeCheck.setDisable(isInherited);
            updateSwatchButton(errorStripeSwatch, errorStripeHex, errorStripeCheck.isSelected(), isInherited);

            boolean hasEffects = effective.getEffectType() != null && effective.getEffectType() != EffectType.NONE;
            effectsCheck.setSelected(hasEffects);
            effectsCheck.setDisable(isInherited);
            effectsHex = effective.getEffectColor();
            updateSwatchButton(effectsSwatch, effectsHex, hasEffects, isInherited);
            effectTypeCombo.setDisable(isInherited || !hasEffects);

            if (effective.getEffectType() != null && effective.getEffectType() != EffectType.NONE) {
                effectTypeCombo.setValue(effective.getEffectType());
            } else if (desc != null && desc.getDefaultEffectType() != null) {
                effectTypeCombo.setValue(desc.getDefaultEffectType());
            } else {
                effectTypeCombo.setValue(EffectType.BORDERED);
            }
        } finally {
            suppressEvents = false;
        }
    }

    private void recordNavigation(String key) {
        if (isNavigatingHistory) return;
        if (historyIndex >= 0 && historyIndex < navigationHistory.size() && navigationHistory.get(historyIndex).equals(key)) {
            return;
        }
        while (navigationHistory.size() > historyIndex + 1) {
            navigationHistory.remove(navigationHistory.size() - 1);
        }
        navigationHistory.add(key);
        historyIndex = navigationHistory.size() - 1;
    }

    public void selectTreeItem(String fullKey) {
        if (fullKey == null) return;
        String normKey = EditorColorSchemeSettings.normalizeKey(fullKey);
        String[] parts = normKey.split(" // ");
        TreeItem<String> curr = categoryTree.getRoot();

        for (String part : parts) {
            TreeItem<String> matched = null;
            for (TreeItem<String> child : curr.getChildren()) {
                if (child.getValue().equalsIgnoreCase(part)) {
                    matched = child;
                    child.setExpanded(true);
                    break;
                }
            }
            if (matched == null) return;
            curr = matched;
        }

        categoryTree.getSelectionModel().select(curr);
        selectedKey = normKey;
        if (curr.getChildren().isEmpty()) {
            loadAttributesForSelectedKey();
        } else {
            attributeEditorBox.setVisible(false);
            attributeEditorBox.setManaged(false);
        }
    }

    private void updatePreview() {
        consoleLinesBox.getChildren().clear();

        // 1. DOS prompt / execution section (media_1790393725684.png)
        addLine(token("C:\\command.com", "Console // System output"));
        addLine(token("- C:>", "Console // System output"));
        addLine(
                token("- ", "Console // System output"),
                token("help", "Console // User input")
        );
        addLine(token("Bad command or file name", "Console // Error output"));

        // Spacer
        addLine(emptySpacer());

        // 2. Log console entries (media_1790393727515.png)
        addLine(token("Log error", "Log console // Error"));
        addLine(token("Log warning", "Log console // Warning"));
        addLine(token("Log info", "Log console // Info"));
        addLine(token("Log verbose", "Log console // Verbose"));
        addLine(token("Log debug", "Log console // Debug"));
        addLine(token("An expired log entry", "Log console // Expired entry"));

        // Spacer
        addLine(emptySpacer());

        // 3. ANSI colors palette demonstration (media_1790393731972.png & media_1790393738767.png)
        addLine(token("# Process output highlighted using ANSI colors codes", "Console // System output"));
        addLine(ansiBlockLine("       ", "ANSI colors // Black", "#000000", "#DFE1E5"));
        addLine(ansiBlockLine("ANSI: red", "ANSI colors // Red", "#F75464", "#FFFFFF"));
        addLine(ansiBlockLine("ANSI: green", "ANSI colors // Green", "#59A869", "#1E1F22"));
        addLine(ansiBlockLine("ANSI: yellow", "ANSI colors // Yellow", "#F5D259", "#1E1F22"));
        addLine(ansiBlockLine("ANSI: blue", "ANSI colors // Blue", "#3574F0", "#FFFFFF"));
        addLine(ansiBlockLine("ANSI: magenta", "ANSI colors // Magenta", "#C77DBB", "#FFFFFF"));
        addLine(ansiBlockLine("ANSI: cyan", "ANSI colors // Cyan", "#22B4D6", "#1E1F22"));
        addLine(ansiBlockLine("ANSI: gray", "ANSI colors // White (Gray)", "#DFE1E5", "#1E1F22"));
        addLine(ansiBlockLine("ANSI: dark gray", "ANSI colors // Bright Black", "#595959", "#FFFFFF"));
        addLine(ansiBlockLine("ANSI: bright red", "ANSI colors // Bright Red", "#F75464", "#FFFFFF"));
        addLine(ansiBlockLine("ANSI: bright green", "ANSI colors // Bright Green", "#59A869", "#1E1F22"));
        addLine(ansiBlockLine("ANSI: bright yellow", "ANSI colors // Bright Yellow", "#F5D259", "#1E1F22"));
        addLine(ansiBlockLine("ANSI: bright blue", "ANSI colors // Bright Blue", "#3574F0", "#FFFFFF"));
        addLine(ansiBlockLine("ANSI: bright magenta", "ANSI colors // Bright Magenta", "#C77DBB", "#FFFFFF"));
        addLine(ansiBlockLine("ANSI: bright cyan", "ANSI colors // Bright Cyan", "#22B4D6", "#1E1F22"));
        addLine(ansiBlockLine("       ", "ANSI colors // Bright White", "#FFFFFF", "#1E1F22"));

        // Spacer
        addLine(emptySpacer());

        // 4. Terminal Command and Process exit (media_1790393731972.png)
        addLine(token("git log", "Terminal // Command to run using IDE"));
        addLine(token("Process finished with exit code 1", "Console // Error output"));
    }

    private Node emptySpacer() {
        Region r = new Region();
        r.setPrefHeight(10);
        r.setMinHeight(10);
        r.setMaxHeight(10);
        return r;
    }

    private Node ansiBlockLine(String text, String key, String defaultBg, String defaultFg) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = (key != null) ? s.resolveAttribute(s.getActiveSchemeName(), key) : null;

        String bg = (attr != null && attr.getBackground() != null) ? attr.getBackground() :
                (attr != null && attr.getForeground() != null ? attr.getForeground() : defaultBg);
        String fg = defaultFg;

        if (bg != null) {
            try {
                Color c = Color.web(bg);
                double brightness = (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114);
                fg = brightness > 0.5 ? "#1E1F22" : "#FFFFFF";
            } catch (Exception ignored) {
            }
        }

        Text t = new Text(text);
        t.setFill(Color.web(fg));
        t.setFont(Font.font("JetBrains Mono", FontWeight.NORMAL, 12.0));

        StackPane box = new StackPane(t);
        box.setAlignment(Pos.CENTER_LEFT);
        boolean isSelected = key != null && (key.equals(selectedKey) || selectedKey.endsWith(" // " + key) || key.endsWith(" // " + selectedKey));
        box.setStyle(String.format(
                "-fx-background-color: %s; -fx-padding: 1 6 1 6; -fx-background-radius: 2; -fx-cursor: hand;%s",
                bg != null ? bg : "#000000",
                isSelected ? " -fx-border-color: #525866; -fx-border-width: 1; -fx-border-radius: 2;" : ""
        ));

        box.setOnMouseClicked(e -> selectTreeItem(key));
        return box;
    }

    private void addLine(Node... nodes) {
        HBox lineBox = new HBox(0);
        lineBox.setAlignment(Pos.CENTER_LEFT);
        lineBox.setPrefHeight(20);
        lineBox.setMinHeight(20);
        lineBox.setMaxHeight(20);
        lineBox.getChildren().addAll(nodes);
        consoleLinesBox.getChildren().add(lineBox);
    }

    private Node token(String text, String key) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = (key != null) ? s.resolveAttribute(s.getActiveSchemeName(), key) : null;

        Text t = new Text(text);
        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#DFE1E5";
        t.setFill(Color.web(fg));

        FontWeight weight = (attr != null && attr.isBold()) ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = (attr != null && attr.isItalic()) ? FontPosture.ITALIC : FontPosture.REGULAR;
        t.setFont(Font.font("JetBrains Mono", weight, posture, 12.5));

        if (attr != null) {
            if (attr.getEffectType() == EffectType.UNDERSCORED || attr.getEffectType() == EffectType.BOLD_UNDERSCORED) {
                t.setUnderline(true);
            }
            if (attr.getEffectType() == EffectType.STRIKEOUT) {
                t.setStrikethrough(true);
            }
        }

        Node visualNode = t;
        if (attr != null && attr.getEffectType() == EffectType.UNDERWAVED) {
            String waveColor = attr.getEffectColor() != null ? attr.getEffectColor() : fg;
            double width = text.length() * 7.5;
            Canvas wave = new Canvas(width, 3);
            GraphicsContext gc = wave.getGraphicsContext2D();
            gc.setStroke(Color.web(waveColor));
            gc.setLineWidth(1.1);
            for (double x = 0; x < width; x += 4) {
                gc.strokeLine(x, 2, x + 2, 0);
                gc.strokeLine(x + 2, 0, x + 4, 2);
            }
            VBox vb = new VBox(1, t, wave);
            vb.setAlignment(Pos.CENTER_LEFT);
            visualNode = vb;
        }

        StackPane container = new StackPane(visualNode);
        container.setAlignment(Pos.CENTER_LEFT);

        StringBuilder style = new StringBuilder();
        if (attr != null && attr.getBackground() != null) {
            style.append(String.format("-fx-background-color: %s; -fx-padding: 1 3 1 3; -fx-background-radius: 2; ", attr.getBackground()));
        }
        if (attr != null && attr.getEffectType() == EffectType.BORDERED) {
            String bc = attr.getEffectColor() != null ? attr.getEffectColor() : "#393B40";
            style.append(String.format("-fx-border-color: %s; -fx-border-width: 1; -fx-border-radius: 2; ", bc));
        }

        boolean isSelected = key != null && (key.equals(selectedKey) || selectedKey.endsWith(" // " + key) || key.endsWith(" // " + selectedKey));
        if (isSelected) {
            style.append("-fx-border-color: #525866; -fx-border-width: 1; -fx-border-radius: 2; ");
        }

        if (key != null) {
            style.append("-fx-cursor: hand; ");
            container.setOnMouseClicked(e -> selectTreeItem(key));
        }
        if (style.length() > 0) {
            container.setStyle(style.toString());
        }

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

    public CheckBox getInheritCheck() {
        return inheritCheck;
    }

    public Hyperlink getInheritLink() {
        return inheritLink;
    }

    public VBox getAttributeEditorBox() {
        return attributeEditorBox;
    }
}