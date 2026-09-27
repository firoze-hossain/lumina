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
 * 1:1 visual & functional replication of Editor > Color Scheme > YAML.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemeYAMLPage extends VBox {

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

    private String selectedKey = "Comment";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemeYAMLPage() {
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

        TreeItem<String> defaultSelectedItem = null;
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getYamlDescriptors()) {
            TreeItem<String> item = new TreeItem<>(desc.getKey());
            rootItem.getChildren().add(item);
            if ("Comment".equals(desc.getKey())) {
                defaultSelectedItem = item;
            }
        }

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
                selectCategory(newVal.getValue());
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
        effectTypeCombo.setValue(EffectType.BORDERED);
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

    private void filterTree(String text) {
        rootItem.getChildren().clear();
        String query = text == null ? "" : text.trim().toLowerCase();
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getYamlDescriptors()) {
            if (query.isEmpty() || desc.getKey().toLowerCase().contains(query)) {
                rootItem.getChildren().add(new TreeItem<>(desc.getKey()));
            }
        }
    }

    public void selectCategory(String key) {
        if (key == null) return;
        this.selectedKey = key;

        boolean prevSuppress = suppressEvents;
        suppressEvents = true;
        try {
            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor("YAML", key);
            ColorAttribute attr = getActiveAttribute(desc, key);

            boolean hasInherit = desc != null && desc.getInheritTarget() != null && !desc.getInheritTarget().isEmpty();
            inheritBox.setVisible(hasInherit);
            inheritBox.setManaged(hasInherit);

            if (hasInherit) {
                inheritTargetLabel.setText(desc.getInheritTarget());
                inheritScopeLabel.setText(desc.getInheritScope() != null ? desc.getInheritScope() : "(Language Defaults)");
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
                effectTypeCombo.setValue(effective.getEffectType() != null ? effective.getEffectType() : EffectType.BORDERED);
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
            suppressEvents = prevSuppress;
        }
    }

    private ColorAttribute getActiveAttribute(AttributesDescriptor desc, String key) {
        String scheme = settings.getActiveSchemeName();
        ColorAttribute attr = settings.getAttribute(scheme, "YAML // " + key);
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
            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor("YAML", selectedKey);
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
                    effectTypeCombo.setValue(inh.getEffectType() != null ? inh.getEffectType() : EffectType.BORDERED);
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

    private void onAttributeModified() {
        if (suppressEvents) return;
        isModified = true;
        updatePreview();
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private Button createSwatchButton() {
        Button btn = new Button();
        btn.setPrefSize(42, 22);
        btn.setMinSize(42, 22);
        btn.setMaxSize(42, 22);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: #55575E; -fx-border-radius: 2; -fx-padding: 0;");
        return btn;
    }

    private void updateSwatch(Button btn, String hexColor) {
        if (hexColor != null && !hexColor.isBlank()) {
            String color = hexColor.startsWith("#") ? hexColor : "#" + hexColor;
            btn.setStyle("-fx-background-color: " + color + "; -fx-border-color: #55575E; -fx-border-radius: 2; -fx-padding: 0;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: #55575E; -fx-border-radius: 2; -fx-padding: 0;");
        }
    }

    private void pickColor(String title, String initialHex, Consumer<String> onSelected) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);
        stage.setTitle("Select " + title + " Color");

        ColorPicker picker = new ColorPicker();
        if (initialHex != null && !initialHex.isBlank()) {
            try {
                picker.setValue(Color.web(initialHex.startsWith("#") ? initialHex : "#" + initialHex));
            } catch (Exception ignored) {}
        }

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("dialog-primary");
        okBtn.setOnAction(e -> {
            Color c = picker.getValue();
            String hex = String.format("#%02X%02X%02X",
                    (int) (c.getRed() * 255),
                    (int) (c.getGreen() * 255),
                    (int) (c.getBlue() * 255));
            onSelected.accept(hex);
            stage.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("dialog-secondary");
        cancelBtn.setOnAction(e -> stage.close());

        HBox btnBox = new HBox(10, okBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(14, new Label(title + ":"), picker, btnBox);
        layout.setPadding(new Insets(16));
        layout.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");

        stage.setScene(new Scene(layout));
        stage.showAndWait();
    }

    public void apply() {
        String scheme = settings.getActiveSchemeName();
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getYamlDescriptors()) {
            if (desc.getKey().equals(selectedKey)) {
                ColorAttribute current = buildCurrentAttributeFromControls(desc);
                settings.setAttribute(scheme, "YAML // " + desc.getKey(), current);
                settings.setAttribute(scheme, desc.getKey(), current);
            }
        }
        isModified = false;
        backupOriginalAttributes();
        updatePreview();
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void reset() {
        String scheme = settings.getActiveSchemeName();
        for (Map.Entry<String, ColorAttribute> entry : originalAttributes.entrySet()) {
            settings.setAttribute(scheme, "YAML // " + entry.getKey(), entry.getValue().clone());
            settings.setAttribute(scheme, entry.getKey(), entry.getValue().clone());
        }
        selectCategory(selectedKey);
        isModified = false;
        updatePreview();
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
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

    private void backupOriginalAttributes() {
        originalAttributes.clear();
        String activeScheme = headerBar.getSelectedScheme();
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getYamlDescriptors()) {
            ColorAttribute attr = settings.getAttribute(activeScheme, "YAML // " + desc.getKey());
            if (attr == null) {
                attr = settings.getAttribute(activeScheme, desc.getKey());
            }
            if (attr == null) {
                attr = desc.getDefaultAttribute();
            }
            if (attr != null) {
                originalAttributes.put(desc.getKey(), attr.clone());
            }
        }
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
    public Hyperlink getInheritTargetLabel() { return inheritTargetLabel; }
    public Label getInheritScopeLabel() { return inheritScopeLabel; }

    public void selectTreeItem(String key) {
        if (key == null) return;
        for (TreeItem<String> item : rootItem.getChildren()) {
            if (item.getValue().equalsIgnoreCase(key)) {
                categoryTree.getSelectionModel().select(item);
                selectCategory(item.getValue());
                return;
            }
        }
    }

    private void loadScheme(String scheme) {
        backupOriginalAttributes();
        selectCategory(selectedKey);
    }

    // ---------------- Interactive Code Preview ----------------
    private void updatePreview() {
        codeLinesBox.getChildren().clear();

        // Snippet matching media_1790501633442.png:
        // ---
        // # Read about fixtures at http://ar.rubyonrails.org/classes/Fixtures.html
        // static_sidebar:
        //   id: "foo"
        //   name: 'side_bar'
        //   staged_position: 1
        //   blog_id: 1
        //   config: |+
        //     --- !map:HashWithIndifferentAccess
        //     title: Static Sidebar
        //     body: The body of a static sidebar
        //   type: StaticSidebar
        //   description: >
        //     Sidebar configuration example
        //   extensions:
        //     - &params

        codeLinesBox.getChildren().add(buildLine(new Token("---", "Sign: brace, comma, etc")));
        codeLinesBox.getChildren().add(buildLine(new Token("# Read about fixtures at http://ar.rubyonrails.org/classes/Fixtures.html", "Comment")));
        codeLinesBox.getChildren().add(buildLine(
                new Token("static_sidebar", "Key"),
                new Token(":", "Sign: brace, comma, etc")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("  id", "Key"),
                new Token(": ", "Sign: brace, comma, etc"),
                new Token("\"foo\"", "Double quoted string")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("  name", "Key"),
                new Token(": ", "Sign: brace, comma, etc"),
                new Token("'side_bar'", "String")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("  staged_position", "Key"),
                new Token(": ", "Sign: brace, comma, etc"),
                new Token("1", null, "#2AACB8")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("  blog_id", "Key"),
                new Token(": ", "Sign: brace, comma, etc"),
                new Token("1", null, "#2AACB8")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("  config", "Key"),
                new Token(": ", "Sign: brace, comma, etc"),
                new Token("|+", "'|' block")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("    --- !map:HashWithIndifferentAccess", "Sign: brace, comma, etc")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("    title", "Key"),
                new Token(": ", "Sign: brace, comma, etc"),
                new Token("Static Sidebar", "Text")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("    body", "Key"),
                new Token(": ", "Sign: brace, comma, etc"),
                new Token("The body of a static sidebar", "Text")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("  type", "Key"),
                new Token(": ", "Sign: brace, comma, etc"),
                new Token("StaticSidebar", "Text")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("  description", "Key"),
                new Token(": ", "Sign: brace, comma, etc"),
                new Token(">", "'>' block")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("    Sidebar configuration example", "Text")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("  extensions", "Key"),
                new Token(":", "Sign: brace, comma, etc")
        ));
        codeLinesBox.getChildren().add(buildLine(
                new Token("    - ", "Sign: brace, comma, etc"),
                new Token("&params", "Anchor/Alias")
        ));
    }

    private static class Token {
        final String text;
        final String categoryKey;
        final String fallbackColor;

        Token(String text, String categoryKey) {
            this(text, categoryKey, null);
        }

        Token(String text, String categoryKey, String fallbackColor) {
            this.text = text;
            this.categoryKey = categoryKey;
            this.fallbackColor = fallbackColor;
        }
    }

    private HBox buildLine(Token... tokens) {
        HBox line = new HBox(0);
        line.setAlignment(Pos.CENTER_LEFT);

        for (Token token : tokens) {
            Label label = new Label(token.text);
            label.setFont(javafx.scene.text.Font.font("monospace", 12.5));

            ColorAttribute attr = token.categoryKey != null ? getActiveAttribute(EditorColorSchemeSettings.getDescriptor("YAML", token.categoryKey), token.categoryKey) : null;
            String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() :
                    (token.fallbackColor != null ? token.fallbackColor : "#DFE1E5");
            String bg = (attr != null && attr.getBackground() != null) ? attr.getBackground() : null;
            boolean bold = attr != null && attr.isBold();
            boolean italic = attr != null && attr.isItalic();

            StringBuilder sb = new StringBuilder();
            sb.append("-fx-text-fill: ").append(fg).append(";");
            if (bg != null && !bg.isBlank()) {
                sb.append(" -fx-background-color: ").append(bg).append(";");
            }
            if (bold) sb.append(" -fx-font-weight: bold;");
            if (italic) sb.append(" -fx-font-style: italic;");
            label.setStyle(sb.toString());

            if (token.categoryKey != null) {
                label.setOnMouseClicked(e -> selectCategory(token.categoryKey));
            }
            line.getChildren().add(label);
        }

        return line;
    }
}
