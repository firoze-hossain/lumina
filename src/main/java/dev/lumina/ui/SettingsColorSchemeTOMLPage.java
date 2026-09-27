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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.*;
import java.util.function.Consumer;

/**
 * 1:1 visual & functional replication of Editor > Color Scheme > TOML.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemeTOMLPage extends VBox {

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

    private String selectedKey = "String // Escape sequence // Invalid";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemeTOMLPage() {
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

        TreeItem<String> defaultSelectedItem = buildTree();

        categoryTree = new TreeView<>(rootItem);
        categoryTree.setShowRoot(false);
        categoryTree.getStyleClass().addAll("color-scheme-tree", "settings-tree-view");
        categoryTree.setPrefWidth(300);
        categoryTree.setPrefHeight(270);
        categoryTree.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
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
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8;");
                } else {
                    setText(item);
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 3 8 3 8;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8;");
                    }
                }
            }
        });

        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                String fullKey = calculateFullKey(newVal);
                selectCategory(fullKey);
            }
        });

        // Search field for tree
        TextField searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #7A7E85; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 4 8 4 8;");
        searchField.textProperty().addListener((obs, old, text) -> filterTree(text));

        VBox treeContainer = new VBox(6, searchField, categoryTree);
        VBox.setVgrow(categoryTree, Priority.ALWAYS);
        treeContainer.setPrefWidth(300);

        // ---------------- Right Attribute Panel ----------------
        boldCheck = new CheckBox("Bold");
        boldCheck.setStyle("-fx-text-fill: #DFE1E5;");
        italicCheck = new CheckBox("Italic");
        italicCheck.setStyle("-fx-text-fill: #DFE1E5;");

        HBox fontStyleBox = new HBox(16, boldCheck, italicCheck);
        fontStyleBox.setAlignment(Pos.CENTER_LEFT);

        // Foreground
        foregroundCheck = new CheckBox("Foreground");
        foregroundCheck.setStyle("-fx-text-fill: #DFE1E5;");
        foregroundSwatch = createSwatchButton();
        foregroundSwatch.setOnAction(e -> pickColor("Foreground", foregroundHex, color -> {
            foregroundHex = color;
            updateSwatch(foregroundSwatch, foregroundHex);
            onAttributeModified();
        }));
        HBox foregroundRow = new HBox(12, foregroundCheck, foregroundSwatch);
        foregroundRow.setAlignment(Pos.CENTER_LEFT);

        // Background
        backgroundCheck = new CheckBox("Background");
        backgroundCheck.setStyle("-fx-text-fill: #DFE1E5;");
        backgroundSwatch = createSwatchButton();
        backgroundSwatch.setOnAction(e -> pickColor("Background", backgroundHex, color -> {
            backgroundHex = color;
            updateSwatch(backgroundSwatch, backgroundHex);
            onAttributeModified();
        }));
        HBox backgroundRow = new HBox(12, backgroundCheck, backgroundSwatch);
        backgroundRow.setAlignment(Pos.CENTER_LEFT);

        // Error stripe mark
        errorStripeCheck = new CheckBox("Error stripe mark");
        errorStripeCheck.setStyle("-fx-text-fill: #DFE1E5;");
        errorStripeSwatch = createSwatchButton();
        errorStripeSwatch.setOnAction(e -> pickColor("Error stripe mark", errorStripeHex, color -> {
            errorStripeHex = color;
            updateSwatch(errorStripeSwatch, errorStripeHex);
            onAttributeModified();
        }));
        HBox errorStripeRow = new HBox(12, errorStripeCheck, errorStripeSwatch);
        errorStripeRow.setAlignment(Pos.CENTER_LEFT);

        // Effects
        effectsCheck = new CheckBox("Effects");
        effectsCheck.setStyle("-fx-text-fill: #DFE1E5;");
        effectsSwatch = createSwatchButton();
        effectsSwatch.setOnAction(e -> pickColor("Effects", effectsHex, color -> {
            effectsHex = color;
            updateSwatch(effectsSwatch, effectsHex);
            onAttributeModified();
        }));
        effectTypeCombo = new ComboBox<>();
        effectTypeCombo.getItems().addAll(EffectType.values());
        effectTypeCombo.setValue(EffectType.UNDERWAVED);
        effectTypeCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
        HBox effectsRow = new HBox(12, effectsCheck, effectsSwatch, effectTypeCombo);
        effectsRow.setAlignment(Pos.CENTER_LEFT);

        // Inherit section
        inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        inheritTargetLabel = new Hyperlink();
        inheritTargetLabel.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-underline: false;");
        inheritTargetLabel.setOnAction(e -> {
            if (onNavigateToInherited != null && inheritTargetLabel.getText() != null) {
                onNavigateToInherited.accept(inheritTargetLabel.getText());
            }
        });
        inheritScopeLabel = new Label();
        inheritScopeLabel.setStyle("-fx-text-fill: #808080; -fx-font-size: 12px;");

        HBox inheritTargetRow = new HBox(4, inheritTargetLabel, inheritScopeLabel);
        inheritTargetRow.setAlignment(Pos.CENTER_LEFT);
        inheritTargetRow.setPadding(new Insets(0, 0, 0, 22));

        inheritBox = new VBox(4, inheritCheck, inheritTargetRow);

        // Listeners
        boldCheck.selectedProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents) onAttributeModified();
        });
        italicCheck.selectedProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents) onAttributeModified();
        });
        foregroundCheck.selectedProperty().addListener((o, ov, nv) -> {
            foregroundSwatch.setDisable(!nv || (inheritBox.isVisible() && inheritCheck.isSelected()));
            if (!suppressEvents) onAttributeModified();
        });
        backgroundCheck.selectedProperty().addListener((o, ov, nv) -> {
            backgroundSwatch.setDisable(!nv || (inheritBox.isVisible() && inheritCheck.isSelected()));
            if (!suppressEvents) onAttributeModified();
        });
        errorStripeCheck.selectedProperty().addListener((o, ov, nv) -> {
            errorStripeSwatch.setDisable(!nv || (inheritBox.isVisible() && inheritCheck.isSelected()));
            if (!suppressEvents) onAttributeModified();
        });
        effectsCheck.selectedProperty().addListener((o, ov, nv) -> {
            effectsSwatch.setDisable(!nv || (inheritBox.isVisible() && inheritCheck.isSelected()));
            effectTypeCombo.setDisable(!nv || (inheritBox.isVisible() && inheritCheck.isSelected()));
            if (!suppressEvents) onAttributeModified();
        });
        effectTypeCombo.valueProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents) onAttributeModified();
        });
        inheritCheck.selectedProperty().addListener((o, ov, nv) -> {
            setControlsInheritedMode(nv);
            if (!suppressEvents) onAttributeModified();
        });

        VBox rightControls = new VBox(12,
                fontStyleBox,
                foregroundRow,
                backgroundRow,
                errorStripeRow,
                effectsRow,
                inheritBox
        );
        rightControls.setPadding(new Insets(8, 12, 12, 16));
        HBox.setHgrow(rightControls, Priority.ALWAYS);

        HBox topSplit = new HBox(16, treeContainer, rightControls);
        topSplit.setPrefHeight(270);

        // ---------------- Bottom Preview ----------------
        codeLinesBox = new VBox(2);
        codeLinesBox.setStyle("-fx-background-color: #1E1F22; -fx-padding: 12;");

        previewScrollPane = new ScrollPane(codeLinesBox);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setPrefHeight(230);
        previewScrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4;");

        getChildren().addAll(headerBar, topSplit, previewScrollPane);

        // Initial backup
        backupOriginalAttributes();

        // Default selection
        if (defaultSelectedItem != null) {
            categoryTree.getSelectionModel().select(defaultSelectedItem);
        } else if (!rootItem.getChildren().isEmpty()) {
            categoryTree.getSelectionModel().select(rootItem.getChildren().get(0));
        }

        updatePreview();
    }

    private TreeItem<String> buildTree() {
        TreeItem<String> defaultSel = null;

        rootItem.getChildren().add(new TreeItem<>("Boolean"));
        rootItem.getChildren().add(new TreeItem<>("Comments"));
        rootItem.getChildren().add(new TreeItem<>("Date"));
        rootItem.getChildren().add(new TreeItem<>("Keys"));
        rootItem.getChildren().add(new TreeItem<>("Number"));

        TreeItem<String> stringGroup = new TreeItem<>("String");
        stringGroup.setExpanded(true);

        TreeItem<String> escGroup = new TreeItem<>("Escape sequence");
        escGroup.setExpanded(true);

        TreeItem<String> invalidItem = new TreeItem<>("Invalid");
        TreeItem<String> validItem = new TreeItem<>("Valid");
        escGroup.getChildren().addAll(invalidItem, validItem);

        TreeItem<String> stringTextItem = new TreeItem<>("String text");
        stringGroup.getChildren().addAll(escGroup, stringTextItem);

        rootItem.getChildren().add(stringGroup);

        defaultSel = invalidItem;
        return defaultSel;
    }

    private String calculateFullKey(TreeItem<String> item) {
        if (item == null || item.getValue() == null) return "";
        List<String> parts = new ArrayList<>();
        TreeItem<String> curr = item;
        while (curr != null && curr != rootItem) {
            parts.add(0, curr.getValue());
            curr = curr.getParent();
        }
        return String.join(" // ", parts);
    }

    private void filterTree(String text) {
        rootItem.getChildren().clear();
        buildTree();
        if (text != null && !text.isBlank()) {
            filterBranch(rootItem, text.toLowerCase());
        }
    }

    private boolean filterBranch(TreeItem<String> parent, String query) {
        List<TreeItem<String>> toRemove = new ArrayList<>();
        for (TreeItem<String> child : parent.getChildren()) {
            boolean keep = child.getValue().toLowerCase().contains(query);
            if (!child.getChildren().isEmpty()) {
                boolean childKept = filterBranch(child, query);
                keep = keep || childKept;
            }
            if (!keep) {
                toRemove.add(child);
            }
        }
        parent.getChildren().removeAll(toRemove);
        return !parent.getChildren().isEmpty();
    }

    public void selectCategory(String fullKey) {
        this.selectedKey = fullKey;
        AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor("TOML", fullKey);
        if (desc == null) {
            desc = EditorColorSchemeSettings.getDescriptor(fullKey);
        }
        suppressEvents = true;
        try {
            ColorAttribute attr = getActiveAttribute(desc, fullKey);

            boolean hasInherit = desc != null && desc.getInheritTarget() != null && !desc.getInheritTarget().isEmpty();
            inheritBox.setVisible(hasInherit);
            inheritBox.setManaged(hasInherit);

            if (hasInherit) {
                inheritTargetLabel.setText(desc.getInheritTarget());
                inheritScopeLabel.setText(desc.getInheritScope() != null ? desc.getInheritScope() : "");
                boolean isInheriting = attr != null && attr.isInherit();
                inheritCheck.setSelected(isInheriting);
                setControlsInheritedMode(isInheriting);
            } else {
                setControlsInheritedMode(false);
            }

            ColorAttribute effective = attr;
            if (effective == null && desc != null) {
                effective = desc.getDefaultAttribute();
            }

            if (effective != null) {
                boldCheck.setSelected(effective.isBold());
                italicCheck.setSelected(effective.isItalic());

                foregroundCheck.setSelected(effective.getForeground() != null);
                foregroundHex = effective.getForeground() != null ? effective.getForeground().replace("#", "") : null;
                updateSwatch(foregroundSwatch, foregroundHex);
                foregroundSwatch.setDisable(!foregroundCheck.isSelected() || (hasInherit && inheritCheck.isSelected()));

                backgroundCheck.setSelected(effective.getBackground() != null);
                backgroundHex = effective.getBackground() != null ? effective.getBackground().replace("#", "") : null;
                updateSwatch(backgroundSwatch, backgroundHex);
                backgroundSwatch.setDisable(!backgroundCheck.isSelected() || (hasInherit && inheritCheck.isSelected()));

                errorStripeCheck.setSelected(effective.getErrorStripeColor() != null);
                errorStripeHex = effective.getErrorStripeColor() != null ? effective.getErrorStripeColor().replace("#", "") : null;
                updateSwatch(errorStripeSwatch, errorStripeHex);
                errorStripeSwatch.setDisable(!errorStripeCheck.isSelected() || (hasInherit && inheritCheck.isSelected()));

                effectsCheck.setSelected(effective.getEffectType() != null && effective.getEffectType() != EffectType.NONE);
                effectsHex = effective.getEffectColor() != null ? effective.getEffectColor().replace("#", "") : null;
                updateSwatch(effectsSwatch, effectsHex);
                effectTypeCombo.setValue(effective.getEffectType() != null ? effective.getEffectType() : EffectType.UNDERWAVED);
                effectsSwatch.setDisable(!effectsCheck.isSelected() || (hasInherit && inheritCheck.isSelected()));
                effectTypeCombo.setDisable(!effectsCheck.isSelected() || (hasInherit && inheritCheck.isSelected()));
            } else {
                boldCheck.setSelected(false);
                italicCheck.setSelected(false);
                foregroundCheck.setSelected(false);
                updateSwatch(foregroundSwatch, null);
                backgroundCheck.setSelected(false);
                updateSwatch(backgroundSwatch, null);
                errorStripeCheck.setSelected(false);
                updateSwatch(errorStripeSwatch, null);
                effectsCheck.setSelected(false);
                updateSwatch(effectsSwatch, null);
            }

        } finally {
            suppressEvents = false;
        }
    }

    private ColorAttribute getActiveAttribute(AttributesDescriptor desc, String key) {
        String scheme = settings.getActiveSchemeName();
        ColorAttribute attr = settings.getAttribute(scheme, "TOML // " + key);
        if (attr == null) {
            attr = settings.getAttribute(scheme, key);
        }
        if (attr == null && desc != null) {
            attr = desc.getDefaultAttribute();
        }
        return attr;
    }

    private void setControlsInheritedMode(boolean inherited) {
        boldCheck.setDisable(inherited);
        italicCheck.setDisable(inherited);
        foregroundCheck.setDisable(inherited);
        foregroundSwatch.setDisable(inherited || !foregroundCheck.isSelected());
        backgroundCheck.setDisable(inherited);
        backgroundSwatch.setDisable(inherited || !backgroundCheck.isSelected());
        errorStripeCheck.setDisable(inherited);
        errorStripeSwatch.setDisable(inherited || !errorStripeCheck.isSelected());
        effectsCheck.setDisable(inherited);
        effectsSwatch.setDisable(inherited || !effectsCheck.isSelected());
        effectTypeCombo.setDisable(inherited || !effectsCheck.isSelected());

        if (inherited) {
            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor("TOML", selectedKey);
            ColorAttribute inh = resolveInheritedAttribute(desc);
            if (inh != null) {
                boolean prevSuppress = suppressEvents;
                suppressEvents = true;
                try {
                    boldCheck.setSelected(inh.isBold());
                    italicCheck.setSelected(inh.isItalic());
                    foregroundCheck.setSelected(inh.getForeground() != null);
                    foregroundHex = inh.getForeground() != null ? inh.getForeground().replace("#", "") : null;
                    updateSwatch(foregroundSwatch, foregroundHex);
                    backgroundCheck.setSelected(inh.getBackground() != null);
                    backgroundHex = inh.getBackground() != null ? inh.getBackground().replace("#", "") : null;
                    updateSwatch(backgroundSwatch, backgroundHex);
                    errorStripeCheck.setSelected(inh.getErrorStripeColor() != null);
                    errorStripeHex = inh.getErrorStripeColor() != null ? inh.getErrorStripeColor().replace("#", "") : null;
                    updateSwatch(errorStripeSwatch, errorStripeHex);
                    effectsCheck.setSelected(inh.getEffectType() != null && inh.getEffectType() != EffectType.NONE);
                    effectsHex = inh.getEffectColor() != null ? inh.getEffectColor().replace("#", "") : null;
                    updateSwatch(effectsSwatch, effectsHex);
                    effectTypeCombo.setValue(inh.getEffectType() != null ? inh.getEffectType() : EffectType.UNDERWAVED);
                } finally {
                    suppressEvents = prevSuppress;
                }
            }
        }
    }

    private ColorAttribute resolveInheritedAttribute(AttributesDescriptor desc) {
        if (desc == null || desc.getInheritTarget() == null) return null;
        String target = desc.getInheritTarget();
        ColorAttribute attr = null;
        if (!target.equals(desc.getKey())) {
            attr = settings.getAttribute(settings.getActiveSchemeName(), target);
            if (attr == null) {
                String alt = target.replace("->", " // ");
                attr = settings.getAttribute(settings.getActiveSchemeName(), alt);
                if (attr == null && target.contains("->")) {
                    String sub = target.substring(target.lastIndexOf("->") + 2);
                    attr = settings.getAttribute(settings.getActiveSchemeName(), sub);
                }
            }
        }
        if (attr == null) {
            String scope = desc.getInheritScope();
            if (scope != null && scope.contains("Language Defaults")) {
                for (AttributesDescriptor ldDesc : EditorColorSchemeSettings.getLanguageDefaultsDescriptors()) {
                    if (ldDesc.getKey().equals(target) || ldDesc.getDisplayName().equals(target) ||
                            (target.contains("->") && ldDesc.getKey().endsWith(target.substring(target.lastIndexOf("->") + 2)))) {
                        attr = ldDesc.getDefaultAttribute();
                        break;
                    }
                }
            }
        }
        if (attr == null) {
            AttributesDescriptor targetDesc = EditorColorSchemeSettings.getDescriptor(target);
            if (targetDesc == null) {
                targetDesc = EditorColorSchemeSettings.getDescriptor(target.replace("->", " // "));
                if (targetDesc == null && target.contains("->")) {
                    targetDesc = EditorColorSchemeSettings.getDescriptor(target.substring(target.lastIndexOf("->") + 2));
                }
            }
            if (targetDesc != null && targetDesc != desc) {
                attr = targetDesc.getDefaultAttribute();
            }
        }
        return attr != null ? attr : (desc != null ? desc.getDefaultAttribute() : null);
    }

    private Button createSwatchButton() {
        Button btn = new Button();
        btn.setPrefSize(72, 22);
        btn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 11px; -fx-text-fill: #DFE1E5; -fx-font-family: monospace;");
        return btn;
    }

    private void updateSwatch(Button btn, String hex) {
        if (hex != null && !hex.isEmpty()) {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            btn.setText(clean.toUpperCase());
            Color c = parseColorSafe("#" + clean);
            double lum = (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114);
            String textCol = lum > 0.6 ? "#000000" : "#FFFFFF";
            btn.setStyle(String.format("-fx-background-color: #%s; -fx-text-fill: %s; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 11px; -fx-font-family: monospace;", clean, textCol));
        } else {
            btn.setText("");
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 11px; -fx-text-fill: #DFE1E5;");
        }
    }

    private void pickColor(String title, String currentHex, Consumer<String> onSelected) {
        Stage pickerStage = new Stage(StageStyle.UTILITY);
        pickerStage.initModality(Modality.APPLICATION_MODAL);
        pickerStage.setTitle("Select " + title + " Color");

        ColorPicker cp = new ColorPicker(parseColorSafe(currentHex != null ? "#" + currentHex : "#BCBEC4"));
        Button ok = new Button("OK");
        ok.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-padding: 4 14;");
        ok.setOnAction(e -> {
            Color c = cp.getValue();
            String hex = String.format("%02X%02X%02X",
                    (int) (c.getRed() * 255),
                    (int) (c.getGreen() * 255),
                    (int) (c.getBlue() * 255));
            onSelected.accept(hex);
            pickerStage.close();
        });

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> pickerStage.close());

        HBox btnBox = new HBox(8, ok, cancel);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(12, cp, btnBox);
        layout.setPadding(new Insets(16));
        layout.setStyle("-fx-background-color: #2B2D30;");

        pickerStage.setScene(new Scene(layout));
        pickerStage.showAndWait();
    }

    private Color parseColorSafe(String str) {
        try {
            if (str != null && !str.isBlank()) return Color.web(str);
        } catch (Exception ignored) {}
        return Color.web("#BCBEC4");
    }

    private void onAttributeModified() {
        if (suppressEvents) return;
        isModified = true;
        updatePreview();
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void backupOriginalAttributes() {
        originalAttributes.clear();
        String scheme = settings.getActiveSchemeName();
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getTomlDescriptors()) {
            ColorAttribute a = settings.getAttribute(scheme, "TOML // " + desc.getKey());
            if (a == null) a = settings.getAttribute(scheme, desc.getKey());
            if (a == null) a = desc.getDefaultAttribute();
            if (a != null) originalAttributes.put(desc.getKey(), a.clone());
        }
    }

    public void apply() {
        String scheme = settings.getActiveSchemeName();
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getTomlDescriptors()) {
            if (desc.getKey().equals(selectedKey)) {
                ColorAttribute current = buildCurrentAttributeFromControls(desc);
                settings.setAttribute(scheme, "TOML // " + desc.getKey(), current);
                settings.setAttribute(scheme, desc.getKey(), current);
            }
        }
        isModified = false;
        backupOriginalAttributes();
        updatePreview();
    }

    public void reset() {
        String scheme = settings.getActiveSchemeName();
        for (Map.Entry<String, ColorAttribute> entry : originalAttributes.entrySet()) {
            settings.setAttribute(scheme, "TOML // " + entry.getKey(), entry.getValue().clone());
            settings.setAttribute(scheme, entry.getKey(), entry.getValue().clone());
        }
        selectCategory(selectedKey);
        isModified = false;
        updatePreview();
    }

    private ColorAttribute buildCurrentAttributeFromControls(AttributesDescriptor desc) {
        boolean inh = inheritBox.isVisible() && inheritCheck.isSelected();
        if (inh) {
            ColorAttribute attr = new ColorAttribute();
            attr.setInherit(true);
            attr.setInheritFrom(desc.getInheritTarget());
            attr.setInheritScope(desc.getInheritScope());
            return attr;
        }
        String fg = foregroundCheck.isSelected() && foregroundHex != null ? "#" + foregroundHex : null;
        String bg = backgroundCheck.isSelected() && backgroundHex != null ? "#" + backgroundHex : null;
        String stripe = errorStripeCheck.isSelected() && errorStripeHex != null ? "#" + errorStripeHex : null;
        String eff = effectsCheck.isSelected() && effectsHex != null ? "#" + effectsHex : null;
        EffectType et = effectsCheck.isSelected() ? effectTypeCombo.getValue() : EffectType.NONE;

        ColorAttribute attr = new ColorAttribute(fg, bg, stripe, et, eff, boldCheck.isSelected(), italicCheck.isSelected());
        attr.setInherit(false);
        return attr;
    }

    public void selectTreeItem(String key) {
        TreeItem<String> found = findItem(rootItem, key);
        if (found != null) {
            expandAncestors(found);
            categoryTree.getSelectionModel().select(found);
        }
    }

    private TreeItem<String> findItem(TreeItem<String> root, String text) {
        if (root == null || text == null) return null;
        String fullKey = calculateFullKey(root);
        if (text.equals(root.getValue()) || text.equals(fullKey)) {
            return root;
        }
        for (TreeItem<String> child : root.getChildren()) {
            TreeItem<String> f = findItem(child, text);
            if (f != null) return f;
        }
        return null;
    }

    private void expandAncestors(TreeItem<String> item) {
        TreeItem<String> p = item.getParent();
        while (p != null) {
            p.setExpanded(true);
            p = p.getParent();
        }
    }

    private void updatePreview() {
        codeLinesBox.getChildren().clear();

        codeLinesBox.getChildren().add(makeLine(
                createToken("[servers]", "Keys")
        ));
        codeLinesBox.getChildren().add(makeLine(
                createToken("alpha", "Keys"),
                createPlain(" = { "),
                createToken("ip", "Keys"),
                createPlain(" = "),
                createToken("'10.0.0.1'", "String // String text"),
                createPlain(", "),
                createToken("dc", "Keys"),
                createPlain(" = "),
                createToken("\"eqdc10\"", "String // String text"),
                createPlain(" }")
        ));
        codeLinesBox.getChildren().add(makeLine(
                createToken("beta", "Keys"),
                createPlain(" = { "),
                createToken("ip", "Keys"),
                createPlain(" = "),
                createToken("'10.0.0.2'", "String // String text"),
                createPlain(", "),
                createToken("dc", "Keys"),
                createPlain(" = "),
                createToken("\"eqdc10\"", "String // String text"),
                createPlain(" }")
        ));
        codeLinesBox.getChildren().add(makeLine(createPlain("")));

        codeLinesBox.getChildren().add(makeLine(
                createToken("[clients]", "Keys")
        ));
        codeLinesBox.getChildren().add(makeLine(
                createToken("data", "Keys"),
                createPlain(" = [ ["),
                createToken("\"gamma\"", "String // String text"),
                createPlain(", "),
                createToken("\"delta\"", "String // String text"),
                createPlain("], ["),
                createToken("1.0", "Number"),
                createPlain(", "),
                createToken("2.0", "Number"),
                createPlain("] ]")
        ));
        codeLinesBox.getChildren().add(makeLine(createPlain("")));

        codeLinesBox.getChildren().add(makeLine(
                createToken("hosts", "Keys"),
                createPlain(" = [")
        ));
        codeLinesBox.getChildren().add(makeLine(
                createPlain("  "),
                createToken("\"alpha\"", "String // String text"),
                createPlain(",")
        ));
        codeLinesBox.getChildren().add(makeLine(
                createPlain("  "),
                createToken("\"omega\"", "String // String text"),
                createPlain(",")
        ));
        codeLinesBox.getChildren().add(makeLine(createPlain("]")));
        codeLinesBox.getChildren().add(makeLine(createPlain("")));

        codeLinesBox.getChildren().add(makeLine(
                createToken("valid-escapes", "Keys"),
                createPlain(" = \"\"\""),
                createToken("\\t", "String // Escape sequence // Valid"),
                createToken("line ", "String // String text"),
                createToken("\\\"", "String // Escape sequence // Valid"),
                createToken("1", "String // String text"),
                createToken("\\\"", "String // Escape sequence // Valid")
        ));
        codeLinesBox.getChildren().add(makeLine(
                createPlain("  "),
                createToken("line", "String // String text"),
                createToken("\\u0020", "String // Escape sequence // Valid"),
                createToken("2", "String // String text")
        ));
        codeLinesBox.getChildren().add(makeLine(
                createPlain("  "),
                createToken("line 3", "String // String text"),
                createToken("\\U0000002E", "String // Escape sequence // Valid")
        ));
        codeLinesBox.getChildren().add(makeLine(
                createToken("\"\"\"", "String // String text")
        ));
        codeLinesBox.getChildren().add(makeLine(
                createToken("invalid-escapes", "Keys"),
                createPlain(" = \""),
                createToken("\\a", "String // Escape sequence // Invalid"),
                createPlain(" "),
                createToken("\\u20", "String // Escape sequence // Invalid"),
                createPlain(" "),
                createToken("\\U0020", "String // Escape sequence // Invalid"),
                createPlain("\"")
        ));
    }

    private HBox makeLine(Label... tokens) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(tokens);
        return row;
    }

    private Label createPlain(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: monospace; -fx-font-size: 13px; -fx-text-fill: #BCBEC4;");
        return l;
    }

    private Label createToken(String text, String key) {
        Label token = new Label(text);
        token.setStyle("-fx-font-family: monospace; -fx-font-size: 13px; " + resolveStyleForKey(key));
        token.setOnMouseClicked(e -> selectTreeItem(key));
        token.setOnMouseEntered(e -> token.setUnderline(true));
        token.setOnMouseExited(e -> token.setUnderline(false));
        return token;
    }

    private String resolveStyleForKey(String key) {
        if (key.equals(selectedKey)) {
            boolean bold = boldCheck.isSelected();
            boolean italic = italicCheck.isSelected();
            String fg = foregroundCheck.isSelected() && foregroundHex != null ? "#" + foregroundHex : "#DFE1E5";
            String extra = "";
            if (effectsCheck.isSelected() && effectsHex != null && effectTypeCombo.getValue() == EffectType.UNDERWAVED) {
                extra = "-fx-border-color: transparent transparent #" + effectsHex + " transparent; -fx-border-style: segments(2, 2); -fx-border-width: 0 0 1 0;";
            }
            return String.format("-fx-text-fill: %s; %s %s %s",
                    fg,
                    bold ? "-fx-font-weight: bold;" : "-fx-font-weight: normal;",
                    italic ? "-fx-font-style: italic;" : "-fx-font-style: normal;",
                    extra);
        }

        AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor("TOML", key);
        ColorAttribute attr = getActiveAttribute(desc, key);
        if (attr != null && attr.isInherit()) {
            attr = resolveInheritedAttribute(desc);
        }
        if (attr == null && desc != null) {
            attr = desc.getDefaultAttribute();
        }

        if (attr != null) {
            String fg = attr.getForeground() != null ? attr.getForeground() : "#DFE1E5";
            String extra = "";
            if (attr.getEffectType() == EffectType.UNDERWAVED && attr.getEffectColor() != null) {
                extra = String.format("-fx-border-color: transparent transparent %s transparent; -fx-border-style: segments(2, 2); -fx-border-width: 0 0 1 0;", attr.getEffectColor());
            }
            return String.format("-fx-text-fill: %s; %s %s %s",
                    fg,
                    attr.isBold() ? "-fx-font-weight: bold;" : "-fx-font-weight: normal;",
                    attr.isItalic() ? "-fx-font-style: italic;" : "-fx-font-style: normal;",
                    extra);
        }
        return "-fx-text-fill: #DFE1E5;";
    }

    public void loadScheme(String scheme) {
        selectCategory(selectedKey);
        backupOriginalAttributes();
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

    public VBox getCodeLinesBox() {
        return codeLinesBox;
    }
}
