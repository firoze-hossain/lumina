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
 * 1:1 visual & functional replication of Editor > Color Scheme > By Scope.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemeByScopePage extends VBox {

    private final EditorColorSchemeSettings settings = EditorColorSchemeSettings.getInstance();
    private final ColorSchemeHeaderBar headerBar;

    private final TreeView<String> categoryTree;
    private final TreeItem<String> rootItem;
    private final Button manageScopesButton;

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

    private String selectedKey = "Project Files";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Runnable onManageScopesListener;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemeByScopePage() {
        setSpacing(10);
        setPadding(new Insets(12, 16, 16, 16));
        setStyle("-fx-background-color: #1E1F22;");

        headerBar = new ColorSchemeHeaderBar();
        headerBar.setOnSchemeChanged(scheme -> loadScheme(scheme));

        // ---------------- Left TreeView ----------------
        rootItem = new TreeItem<>("Root");
        rootItem.setExpanded(true);

        TreeItem<String> defaultSelectedItem = null;
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getByScopeDescriptors()) {
            TreeItem<String> item = new TreeItem<>(desc.getKey());
            rootItem.getChildren().add(item);
            if ("Project Files".equals(desc.getKey())) {
                defaultSelectedItem = item;
            }
        }

        categoryTree = new TreeView<>(rootItem);
        categoryTree.setShowRoot(false);
        categoryTree.getStyleClass().addAll("color-scheme-tree", "settings-tree-view");
        categoryTree.setPrefWidth(300);
        categoryTree.setPrefHeight(340);
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

        // Manage Scopes... button
        manageScopesButton = new Button("Manage Scopes...");
        manageScopesButton.setPrefWidth(300);
        manageScopesButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 6 12 6 12;");
        manageScopesButton.setOnAction(e -> {
            if (onManageScopesListener != null) {
                onManageScopesListener.run();
            }
        });

        VBox treeContainer = new VBox(6, searchField, categoryTree, manageScopesButton);
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

        // Listeners
        boldCheck.selectedProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents) onAttributeModified();
        });
        italicCheck.selectedProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents) onAttributeModified();
        });
        foregroundCheck.selectedProperty().addListener((o, ov, nv) -> {
            foregroundSwatch.setDisable(!nv);
            if (!suppressEvents) onAttributeModified();
        });
        backgroundCheck.selectedProperty().addListener((o, ov, nv) -> {
            backgroundSwatch.setDisable(!nv);
            if (!suppressEvents) onAttributeModified();
        });
        errorStripeCheck.selectedProperty().addListener((o, ov, nv) -> {
            errorStripeSwatch.setDisable(!nv);
            if (!suppressEvents) onAttributeModified();
        });
        effectsCheck.selectedProperty().addListener((o, ov, nv) -> {
            effectsSwatch.setDisable(!nv);
            effectTypeCombo.setDisable(!nv);
            if (!suppressEvents) onAttributeModified();
        });
        effectTypeCombo.valueProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents) onAttributeModified();
        });

        VBox rightControls = new VBox(14,
                fontStyleBox,
                foregroundRow,
                backgroundRow,
                errorStripeRow,
                effectsRow
        );
        rightControls.setPadding(new Insets(8, 12, 12, 16));
        HBox.setHgrow(rightControls, Priority.ALWAYS);

        HBox mainSplit = new HBox(16, treeContainer, rightControls);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        getChildren().addAll(headerBar, mainSplit);

        // Initial backup
        backupOriginalAttributes();

        // Default selection
        if (defaultSelectedItem != null) {
            categoryTree.getSelectionModel().select(defaultSelectedItem);
        } else if (!rootItem.getChildren().isEmpty()) {
            categoryTree.getSelectionModel().select(rootItem.getChildren().get(0));
        }
    }

    private void filterTree(String text) {
        rootItem.getChildren().clear();
        String query = text == null ? "" : text.trim().toLowerCase();
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getByScopeDescriptors()) {
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
            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor("By Scope", key);
            ColorAttribute attr = getActiveAttribute(desc, key);

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
                foregroundSwatch.setDisable(!foregroundCheck.isSelected());

                backgroundCheck.setSelected(effective.getBackground() != null);
                backgroundHex = effective.getBackground() != null ? effective.getBackground().replace("#", "") : null;
                updateSwatch(backgroundSwatch, backgroundHex);
                backgroundSwatch.setDisable(!backgroundCheck.isSelected());

                errorStripeCheck.setSelected(effective.getErrorStripeColor() != null);
                errorStripeHex = effective.getErrorStripeColor() != null ? effective.getErrorStripeColor().replace("#", "") : null;
                updateSwatch(errorStripeSwatch, errorStripeHex);
                errorStripeSwatch.setDisable(!errorStripeCheck.isSelected());

                effectsCheck.setSelected(effective.getEffectType() != null && effective.getEffectType() != EffectType.NONE);
                effectsHex = effective.getEffectColor() != null ? effective.getEffectColor().replace("#", "") : null;
                updateSwatch(effectsSwatch, effectsHex);
                effectTypeCombo.setValue(effective.getEffectType() != null ? effective.getEffectType() : EffectType.BORDERED);
                effectsSwatch.setDisable(!effectsCheck.isSelected());
                effectTypeCombo.setDisable(!effectsCheck.isSelected());
            } else {
                boldCheck.setSelected(false);
                italicCheck.setSelected(false);
                foregroundHex = null;
                foregroundCheck.setSelected(false);
                updateSwatch(foregroundSwatch, null);
                backgroundHex = null;
                backgroundCheck.setSelected(false);
                updateSwatch(backgroundSwatch, null);
                errorStripeHex = null;
                errorStripeCheck.setSelected(false);
                updateSwatch(errorStripeSwatch, null);
                effectsHex = null;
                effectsCheck.setSelected(false);
                updateSwatch(effectsSwatch, null);
                effectTypeCombo.setValue(EffectType.BORDERED);
            }
        } finally {
            suppressEvents = prevSuppress;
        }
    }

    private ColorAttribute getActiveAttribute(AttributesDescriptor desc, String key) {
        String scheme = settings.getActiveSchemeName();
        ColorAttribute attr = settings.getAttribute(scheme, "By Scope // " + key);
        if (attr == null) {
            attr = settings.getAttribute(scheme, key);
        }
        if (attr == null && desc != null) {
            attr = desc.getDefaultAttribute();
        }
        return attr;
    }

    private void onAttributeModified() {
        if (suppressEvents) return;
        isModified = true;
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
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getByScopeDescriptors()) {
            if (desc.getKey().equals(selectedKey)) {
                ColorAttribute current = buildCurrentAttributeFromControls();
                settings.setAttribute(scheme, "By Scope // " + desc.getKey(), current);
                settings.setAttribute(scheme, desc.getKey(), current);
            }
        }
        isModified = false;
        backupOriginalAttributes();
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void reset() {
        String scheme = settings.getActiveSchemeName();
        for (Map.Entry<String, ColorAttribute> entry : originalAttributes.entrySet()) {
            settings.setAttribute(scheme, "By Scope // " + entry.getKey(), entry.getValue().clone());
            settings.setAttribute(scheme, entry.getKey(), entry.getValue().clone());
        }
        selectCategory(selectedKey);
        isModified = false;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private ColorAttribute buildCurrentAttributeFromControls() {
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
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getByScopeDescriptors()) {
            ColorAttribute attr = settings.getAttribute(activeScheme, "By Scope // " + desc.getKey());
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

    public void setOnManageScopesListener(Runnable listener) {
        this.onManageScopesListener = listener;
    }

    public ColorSchemeHeaderBar getHeaderBar() {
        return headerBar;
    }

    public TreeView<String> getCategoryTree() {
        return categoryTree;
    }

    public Button getManageScopesButton() {
        return manageScopesButton;
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
}
