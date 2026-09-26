package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.AttributesDescriptor;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.*;
import java.util.function.Consumer;

/**
 * Editor > Color Scheme > Diagrams settings page.
 * Replicates 1:1 visual and functional parity with reference screenshot media_1790428910213.png:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - Hierarchical Category Tree view with groups:
 *     Bends (Bend, Bend selection),
 *     Coarse grid,
 *     Edges (Annotation edge, Bad edge, Default edge, Edge selection, Generalization edge, Inner class edge, Realization edge),
 *     Fine grid,
 *     Hot spots,
 *     Nodes (Highlighted node border, Node background, Node border, Node header, Overview node background, Selected node border),
 *     Notes (Note background, Note border, Overview note background),
 *     Port,
 *     Selection Box (Selection box background, Selection box border),
 *     Snapping lines.
 * - Default selection: "Edge selection" (Foreground #CC7832, Effects Underscored).
 * - Full Attribute Editor panel with Bold, Italic, Foreground, Background, Error stripe mark,
 *   and Effects (dropdown).
 */
public class SettingsColorSchemeDiagramsPage extends VBox {

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
    private String selectedKey = "Edge selection";
    private String foregroundHex = "#CC7832";
    private String backgroundHex = null;
    private String errorStripeHex = null;
    private String effectsHex = null;
    private boolean suppressEvents = false;
    private boolean modified = false;

    private Runnable onModifiedListener;

    public SettingsColorSchemeDiagramsPage() {
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

        buildCategoryTree();

        // 3. Right Attribute Editor Box
        attributeEditorBox = new VBox(12);
        attributeEditorBox.setPadding(new Insets(8, 16, 16, 24));
        attributeEditorBox.setStyle("-fx-background-color: #1E1F22;");
        attributeEditorBox.setPrefWidth(380);

        // Font styles
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
        foregroundCheck.setPrefWidth(140);
        foregroundSwatch = createSwatchButton();
        fgRow.getChildren().addAll(foregroundCheck, foregroundSwatch);

        // Background
        HBox bgRow = new HBox(8);
        bgRow.setAlignment(Pos.CENTER_LEFT);
        backgroundCheck = new CheckBox("Background");
        backgroundCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        backgroundCheck.setPrefWidth(140);
        backgroundSwatch = createSwatchButton();
        bgRow.getChildren().addAll(backgroundCheck, backgroundSwatch);

        // Error stripe
        HBox esRow = new HBox(8);
        esRow.setAlignment(Pos.CENTER_LEFT);
        errorStripeCheck = new CheckBox("Error stripe mark");
        errorStripeCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        errorStripeCheck.setPrefWidth(140);
        errorStripeSwatch = createSwatchButton();
        esRow.getChildren().addAll(errorStripeCheck, errorStripeSwatch);

        // Effects
        HBox effRow = new HBox(8);
        effRow.setAlignment(Pos.CENTER_LEFT);
        effectsCheck = new CheckBox("Effects");
        effectsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        effectsCheck.setPrefWidth(140);
        effectsSwatch = createSwatchButton();
        effRow.getChildren().addAll(effectsCheck, effectsSwatch);

        effectTypeCombo = new ComboBox<>();
        effectTypeCombo.getItems().addAll(EffectType.values());
        effectTypeCombo.setValue(EffectType.UNDERSCORED);
        effectTypeCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 12px; -fx-pref-width: 140;"
        );

        attributeEditorBox.getChildren().addAll(
                fontBox,
                fgRow,
                bgRow,
                esRow,
                effRow,
                effectTypeCombo
        );

        // Main horizontal split: Tree (left) + Attribute Editor (right)
        HBox mainSplit = new HBox(16);
        HBox.setHgrow(categoryTree, Priority.ALWAYS);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);
        mainSplit.getChildren().addAll(categoryTree, attributeEditorBox);

        getChildren().addAll(headerBar, mainSplit);

        initListeners();
        selectDefaultKey();
    }

    private Button createSwatchButton() {
        Button btn = new Button();
        btn.setPrefSize(42, 22);
        btn.setMinSize(42, 22);
        btn.setMaxSize(42, 22);
        btn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");
        return btn;
    }

    private void buildCategoryTree() {
        TreeItem<String> root = new TreeItem<>("Root");

        // 1. Bends
        TreeItem<String> bendsNode = new TreeItem<>("Bends");
        bendsNode.setExpanded(true);
        bendsNode.getChildren().addAll(
                new TreeItem<>("Bend"),
                new TreeItem<>("Bend selection")
        );

        // 2. Coarse grid
        TreeItem<String> coarseGrid = new TreeItem<>("Coarse grid");

        // 3. Edges
        TreeItem<String> edgesNode = new TreeItem<>("Edges");
        edgesNode.setExpanded(true);
        edgesNode.getChildren().addAll(
                new TreeItem<>("Annotation edge"),
                new TreeItem<>("Bad edge"),
                new TreeItem<>("Default edge"),
                new TreeItem<>("Edge selection"),
                new TreeItem<>("Generalization edge"),
                new TreeItem<>("Inner class edge"),
                new TreeItem<>("Realization edge")
        );

        // 4. Fine grid
        TreeItem<String> fineGrid = new TreeItem<>("Fine grid");

        // 5. Hot spots
        TreeItem<String> hotSpots = new TreeItem<>("Hot spots");

        // 6. Nodes
        TreeItem<String> nodesNode = new TreeItem<>("Nodes");
        nodesNode.setExpanded(true);
        nodesNode.getChildren().addAll(
                new TreeItem<>("Highlighted node border"),
                new TreeItem<>("Node background"),
                new TreeItem<>("Node border"),
                new TreeItem<>("Node header"),
                new TreeItem<>("Overview node background"),
                new TreeItem<>("Selected node border")
        );

        // 7. Notes
        TreeItem<String> notesNode = new TreeItem<>("Notes");
        notesNode.setExpanded(true);
        notesNode.getChildren().addAll(
                new TreeItem<>("Note background"),
                new TreeItem<>("Note border"),
                new TreeItem<>("Overview note background")
        );

        // 8. Port
        TreeItem<String> port = new TreeItem<>("Port");

        // 9. Selection Box
        TreeItem<String> selBoxNode = new TreeItem<>("Selection Box");
        selBoxNode.setExpanded(true);
        selBoxNode.getChildren().addAll(
                new TreeItem<>("Selection box background"),
                new TreeItem<>("Selection box border")
        );

        // 10. Snapping lines
        TreeItem<String> snappingLines = new TreeItem<>("Snapping lines");

        root.getChildren().addAll(
                bendsNode,
                coarseGrid,
                edgesNode,
                fineGrid,
                hotSpots,
                nodesNode,
                notesNode,
                port,
                selBoxNode,
                snappingLines
        );

        categoryTree.setRoot(root);
    }

    private void selectDefaultKey() {
        selectTreeItem("Edge selection");
    }

    public void selectTreeItem(String key) {
        if (key == null || categoryTree.getRoot() == null) return;
        for (TreeItem<String> item : categoryTree.getRoot().getChildren()) {
            if (key.equals(item.getValue())) {
                categoryTree.getSelectionModel().select(item);
                return;
            }
            for (TreeItem<String> child : item.getChildren()) {
                if (key.equals(child.getValue())) {
                    categoryTree.getSelectionModel().select(child);
                    return;
                }
            }
        }
    }

    private void initListeners() {
        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isLeaf()) {
                selectedKey = newVal.getValue();
                loadAttributesForSelectedKey();
            }
        });

        boldCheck.setOnAction(e -> onAttributeControlChanged());
        italicCheck.setOnAction(e -> onAttributeControlChanged());

        foregroundCheck.setOnAction(e -> {
            if (foregroundCheck.isSelected() && foregroundHex == null) {
                foregroundHex = "#CC7832";
            }
            onAttributeControlChanged();
        });
        foregroundSwatch.setOnAction(e -> openColorPicker("Foreground", foregroundHex, color -> {
            foregroundHex = color;
            foregroundCheck.setSelected(true);
            onAttributeControlChanged();
        }));

        backgroundCheck.setOnAction(e -> {
            if (backgroundCheck.isSelected() && backgroundHex == null) {
                backgroundHex = "#313335";
            }
            onAttributeControlChanged();
        });
        backgroundSwatch.setOnAction(e -> openColorPicker("Background", backgroundHex, color -> {
            backgroundHex = color;
            backgroundCheck.setSelected(true);
            onAttributeControlChanged();
        }));

        errorStripeCheck.setOnAction(e -> {
            if (errorStripeCheck.isSelected() && errorStripeHex == null) {
                errorStripeHex = "#FF6B68";
            }
            onAttributeControlChanged();
        });
        errorStripeSwatch.setOnAction(e -> openColorPicker("Error Stripe Mark", errorStripeHex, color -> {
            errorStripeHex = color;
            errorStripeCheck.setSelected(true);
            onAttributeControlChanged();
        }));

        effectsCheck.setOnAction(e -> {
            if (effectsCheck.isSelected() && effectsHex == null) {
                effectsHex = foregroundHex != null ? foregroundHex : "#CC7832";
            }
            onAttributeControlChanged();
        });
        effectsSwatch.setOnAction(e -> openColorPicker("Effects Color", effectsHex, color -> {
            effectsHex = color;
            effectsCheck.setSelected(true);
            onAttributeControlChanged();
        }));

        effectTypeCombo.setOnAction(e -> onAttributeControlChanged());
    }

    public String getFullDescriptorKey(String leafName) {
        if (leafName == null) return null;
        switch (leafName) {
            case "Bend": return "Bends // Bend";
            case "Bend selection": return "Bends // Bend selection";
            case "Annotation edge": return "Edges // Annotation edge";
            case "Bad edge": return "Edges // Bad edge";
            case "Default edge": return "Edges // Default edge";
            case "Edge selection": return "Edges // Edge selection";
            case "Generalization edge": return "Edges // Generalization edge";
            case "Inner class edge": return "Edges // Inner class edge";
            case "Realization edge": return "Edges // Realization edge";
            case "Highlighted node border": return "Nodes // Highlighted node border";
            case "Node background": return "Nodes // Node background";
            case "Node border": return "Nodes // Node border";
            case "Node header": return "Nodes // Node header";
            case "Overview node background": return "Nodes // Overview node background";
            case "Selected node border": return "Nodes // Selected node border";
            case "Note background": return "Notes // Note background";
            case "Note border": return "Notes // Note border";
            case "Overview note background": return "Notes // Overview note background";
            case "Selection box background": return "Selection Box // Selection box background";
            case "Selection box border": return "Selection Box // Selection box border";
            default: return leafName;
        }
    }

    private void loadAttributesForSelectedKey() {
        suppressEvents = true;
        try {
            EditorColorSchemeSettings settings = EditorColorSchemeSettings.getInstance();
            String fullKey = getFullDescriptorKey(selectedKey);
            ColorAttribute attr = settings.getAttribute(headerBar.getSelectedScheme(), fullKey);
            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(fullKey);

            if (attr == null && desc != null) {
                attr = desc.getDefaultAttribute();
            }

            if (attr != null) {
                boldCheck.setSelected(attr.isBold());
                italicCheck.setSelected(attr.isItalic());

                boolean hasFg = attr.getForeground() != null && !attr.getForeground().isBlank();
                foregroundCheck.setSelected(hasFg);
                foregroundHex = hasFg ? attr.getForeground() : null;
                updateSwatch(foregroundSwatch, foregroundHex);

                boolean hasBg = attr.getBackground() != null && !attr.getBackground().isBlank();
                backgroundCheck.setSelected(hasBg);
                backgroundHex = hasBg ? attr.getBackground() : null;
                updateSwatch(backgroundSwatch, backgroundHex);

                boolean hasStripe = attr.getErrorStripeColor() != null && !attr.getErrorStripeColor().isBlank();
                errorStripeCheck.setSelected(hasStripe);
                errorStripeHex = hasStripe ? attr.getErrorStripeColor() : null;
                updateSwatch(errorStripeSwatch, errorStripeHex);

                boolean hasEff = attr.getEffectColor() != null && !attr.getEffectColor().isBlank() && attr.getEffectType() != EffectType.NONE;
                effectsCheck.setSelected(hasEff);
                effectsHex = hasEff ? attr.getEffectColor() : null;
                updateSwatch(effectsSwatch, effectsHex);

                if (attr.getEffectType() != null && attr.getEffectType() != EffectType.NONE) {
                    effectTypeCombo.setValue(attr.getEffectType());
                } else if (desc != null && desc.getDefaultEffectType() != null) {
                    effectTypeCombo.setValue(desc.getDefaultEffectType());
                } else {
                    effectTypeCombo.setValue(EffectType.UNDERSCORED);
                }
            } else {
                boldCheck.setSelected(false);
                italicCheck.setSelected(false);
                foregroundCheck.setSelected(false);
                foregroundHex = null;
                updateSwatch(foregroundSwatch, null);
                backgroundCheck.setSelected(false);
                backgroundHex = null;
                updateSwatch(backgroundSwatch, null);
                errorStripeCheck.setSelected(false);
                errorStripeHex = null;
                updateSwatch(errorStripeSwatch, null);
                effectsCheck.setSelected(false);
                effectsHex = null;
                updateSwatch(effectsSwatch, null);
                effectTypeCombo.setValue(EffectType.UNDERSCORED);
            }

            if (desc != null) {
                boldCheck.setDisable(!desc.isSupportsFont());
                italicCheck.setDisable(!desc.isSupportsFont());
                foregroundCheck.setDisable(!desc.isSupportsForeground());
                foregroundSwatch.setDisable(!desc.isSupportsForeground());
                backgroundCheck.setDisable(!desc.isSupportsBackground());
                backgroundSwatch.setDisable(!desc.isSupportsBackground());
                errorStripeCheck.setDisable(!desc.isSupportsErrorStripe());
                errorStripeSwatch.setDisable(!desc.isSupportsErrorStripe());
                effectsCheck.setDisable(!desc.isSupportsEffects());
                effectsSwatch.setDisable(!desc.isSupportsEffects());
                effectTypeCombo.setDisable(!desc.isSupportsEffects());
            } else {
                boldCheck.setDisable(false);
                italicCheck.setDisable(false);
                foregroundCheck.setDisable(false);
                foregroundSwatch.setDisable(false);
                backgroundCheck.setDisable(false);
                backgroundSwatch.setDisable(false);
                errorStripeCheck.setDisable(false);
                errorStripeSwatch.setDisable(false);
                effectsCheck.setDisable(false);
                effectsSwatch.setDisable(false);
                effectTypeCombo.setDisable(false);
            }
        } finally {
            suppressEvents = false;
        }
    }

    private void onAttributeControlChanged() {
        if (suppressEvents) return;

        updateSwatch(foregroundSwatch, foregroundCheck.isSelected() ? foregroundHex : null);
        updateSwatch(backgroundSwatch, backgroundCheck.isSelected() ? backgroundHex : null);
        updateSwatch(errorStripeSwatch, errorStripeCheck.isSelected() ? errorStripeHex : null);
        updateSwatch(effectsSwatch, effectsCheck.isSelected() ? effectsHex : null);

        modified = true;
        notifyModified();
    }

    private void updateSwatch(Button swatch, String hex) {
        if (hex != null && !hex.isBlank()) {
            String cleanHex = hex.startsWith("#") ? hex : "#" + hex;
            swatch.setStyle("-fx-background-color: " + cleanHex + "; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");
            swatch.setText(cleanHex.substring(1).toUpperCase());
            swatch.setTextFill(isBright(cleanHex) ? Color.BLACK : Color.WHITE);
            swatch.setFont(Font.font("Monospaced", 9));
        } else {
            swatch.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");
            swatch.setText("");
        }
    }

    private boolean isBright(String hex) {
        try {
            Color c = Color.web(hex);
            return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) > 0.6;
        } catch (Exception e) {
            return false;
        }
    }

    private void openColorPicker(String title, String initialColor, Consumer<String> onSelected) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        ColorPicker picker = new ColorPicker();
        if (initialColor != null && !initialColor.isBlank()) {
            try {
                picker.setValue(Color.web(initialColor.startsWith("#") ? initialColor : "#" + initialColor));
            } catch (Exception ignored) {}
        }
        VBox content = new VBox(10, new Label("Choose color:"), picker);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && picker.getValue() != null) {
                Color c = picker.getValue();
                return String.format("#%02X%02X%02X",
                        (int) Math.round(c.getRed() * 255),
                        (int) Math.round(c.getGreen() * 255),
                        (int) Math.round(c.getBlue() * 255));
            }
            return null;
        });
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(onSelected);
    }

    private void notifyModified() {
        modified = true;
        if (onModifiedListener != null) {
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
        if (!modified) return;
        EditorColorSchemeSettings settings = EditorColorSchemeSettings.getInstance();
        String scheme = headerBar.getSelectedScheme();
        String fullKey = getFullDescriptorKey(selectedKey);

        ColorAttribute attr = new ColorAttribute();
        attr.setBold(boldCheck.isSelected());
        attr.setItalic(italicCheck.isSelected());
        attr.setInherit(false);

        if (foregroundCheck.isSelected() && foregroundHex != null) {
            attr.setForeground(foregroundHex);
        }
        if (backgroundCheck.isSelected() && backgroundHex != null) {
            attr.setBackground(backgroundHex);
        }
        if (errorStripeCheck.isSelected() && errorStripeHex != null) {
            attr.setErrorStripeColor(errorStripeHex);
        }
        if (effectsCheck.isSelected() && effectsHex != null) {
            attr.setEffectColor(effectsHex);
            attr.setEffectType(effectTypeCombo.getValue() != null ? effectTypeCombo.getValue() : EffectType.UNDERSCORED);
        } else {
            attr.setEffectType(EffectType.NONE);
        }

        settings.setAttribute(scheme, fullKey, attr);
        modified = false;
        notifyModified();
    }

    public void reset() {
        loadAttributesForSelectedKey();
        modified = false;
        notifyModified();
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

    public String getForegroundHex() {
        return foregroundHex;
    }

    public String getBackgroundHex() {
        return backgroundHex;
    }

    public EffectType getSelectedEffectType() {
        return effectTypeCombo.getValue();
    }

    public VBox getAttributeEditorBox() {
        return attributeEditorBox;
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
}
