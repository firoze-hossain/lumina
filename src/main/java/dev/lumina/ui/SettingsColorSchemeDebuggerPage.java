package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;

/**
 * Editor > Color Scheme > Debugger settings page.
 * Matches 1:1 with reference image media_1790422678161.png:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - Left: Category Tree/List with 11 debugger attributes (Breakpoint line, Inlined modified values, etc.).
 * - Right: Attribute Editor panel (Bold/Italic, Foreground, Background, Error stripe mark, Effects).
 * - Full-height content layout (no bottom code preview).
 */
public class SettingsColorSchemeDebuggerPage extends VBox {

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

    // State
    private String selectedKey = "Inlined modified values";
    private String foregroundHex = null;
    private String backgroundHex = null;
    private String errorStripeHex = null;
    private String effectsHex = null;
    private boolean suppressEvents = false;
    private boolean modified = false;

    private Runnable onModifiedListener;

    public SettingsColorSchemeDebuggerPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(12, 16, 16, 16));
        setSpacing(12);

        // 1. Header Bar
        headerBar = new ColorSchemeHeaderBar();
        headerBar.setOnSchemeChanged(scheme -> {
            loadAttributesForSelectedKey();
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
        attributeEditorBox.setPadding(new Insets(8, 16, 8, 16));
        attributeEditorBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(attributeEditorBox, Priority.ALWAYS);

        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                selectedKey = newVal.getValue();
                loadAttributesForSelectedKey();
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

        // Content Area fills all available height
        HBox contentArea = new HBox(16, categoryTree, attributeEditorBox);
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        HBox.setHgrow(categoryTree, Priority.NEVER);
        HBox.setHgrow(attributeEditorBox, Priority.ALWAYS);

        getChildren().addAll(headerBar, contentArea);

        // Initial selection: "Inlined modified values" matching reference image
        selectTreeItem(selectedKey);
    }

    private void buildCategoryTree(TreeItem<String> root) {
        for (EditorColorSchemeSettings.AttributesDescriptor desc : EditorColorSchemeSettings.getDebuggerDescriptors()) {
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

            // Capabilities check
            boolean canFg = desc == null || desc.isSupportsForeground();
            boolean canBg = desc == null || desc.isSupportsBackground();
            boolean canStripe = desc == null || desc.isSupportsErrorStripe();
            boolean canEffects = desc == null || desc.isSupportsEffects();
            boolean canFont = desc == null || desc.isSupportsFont();

            boldCheck.setDisable(!canFont);
            italicCheck.setDisable(!canFont);
            boldCheck.setSelected(effective.isBold());
            italicCheck.setSelected(effective.isItalic());

            foregroundCheck.setDisable(!canFg);
            foregroundHex = effective.getForeground();
            foregroundCheck.setSelected(canFg && foregroundHex != null);
            updateSwatchButton(foregroundSwatch, foregroundHex, foregroundCheck.isSelected());

            backgroundCheck.setDisable(!canBg);
            backgroundHex = effective.getBackground();
            backgroundCheck.setSelected(canBg && backgroundHex != null);
            updateSwatchButton(backgroundSwatch, backgroundHex, backgroundCheck.isSelected());

            errorStripeCheck.setDisable(!canStripe);
            errorStripeHex = effective.getErrorStripeColor();
            errorStripeCheck.setSelected(canStripe && errorStripeHex != null);
            updateSwatchButton(errorStripeSwatch, errorStripeHex, errorStripeCheck.isSelected());

            effectsCheck.setDisable(!canEffects);
            boolean hasEffects = canEffects && effective.getEffectType() != null && effective.getEffectType() != EffectType.NONE;
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
                return;
            }
        }
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

    // Getters for test and integration
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
}
