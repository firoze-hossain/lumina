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
 * 1:1 visual & functional replication of Editor > Color Scheme > Jupyter Notebooks.
 * Dynamically managed via EditorColorSchemeSettings and AttributesDescriptor architecture.
 */
public class SettingsColorSchemeJupyterNotebooksPage extends VBox {

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

    // Inheritance controls (hidden for Jupyter Notebooks as per reference screenshot media_1790467611145.png)
    private final VBox inheritBox;
    private final CheckBox inheritCheck;
    private final Hyperlink inheritTargetLabel;
    private final Label inheritScopeLabel;

    // Preview
    private final ScrollPane previewScrollPane;
    private final VBox codeLinesBox;

    private String selectedKey = "Input execution count";
    private boolean suppressEvents = false;
    private boolean isModified = false;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToInherited;

    private final Map<String, ColorAttribute> originalAttributes = new HashMap<>();

    public SettingsColorSchemeJupyterNotebooksPage() {
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

        TreeItem<String> cellFrameGroup = new TreeItem<>("Cell frame");
        cellFrameGroup.setExpanded(true);
        cellFrameGroup.getChildren().addAll(
                new TreeItem<>("Hovered"),
                new TreeItem<>("Selected")
        );

        TreeItem<String> cellStripeGroup = new TreeItem<>("Cell stripe");
        cellStripeGroup.setExpanded(true);
        cellStripeGroup.getChildren().addAll(
                new TreeItem<>("Hovered"),
                new TreeItem<>("Selected")
        );

        TreeItem<String> inputCountItem = new TreeItem<>("Input execution count");

        rootItem.getChildren().addAll(
                new TreeItem<>("Caret row background"),
                cellFrameGroup,
                cellStripeGroup,
                new TreeItem<>("Code cell background"),
                new TreeItem<>("Editor background"),
                inputCountItem,
                new TreeItem<>("Output execution count"),
                new TreeItem<>("Progress bar of a running cell")
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
        effectTypeCombo.getItems().addAll(
                EffectType.UNDERSCORED,
                EffectType.BOLD_UNDERSCORED,
                EffectType.UNDERWAVED,
                EffectType.BORDERED,
                EffectType.STRIKEOUT,
                EffectType.DOTTED_LINE
        );
        effectTypeCombo.setValue(EffectType.UNDERSCORED);
        effectTypeCombo.setPrefWidth(140);
        effectTypeCombo.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox effectTypeRow = new HBox(effectTypeCombo);
        effectTypeRow.setAlignment(Pos.CENTER_RIGHT);
        effectTypeRow.setPadding(new Insets(0, 0, 4, 0));

        // Inherit values from (hidden for Jupyter Notebooks)
        inheritBox = new VBox(4);
        inheritBox.setPadding(new Insets(10, 0, 0, 0));
        HBox inheritCheckRow = new HBox(8);
        inheritCheckRow.setAlignment(Pos.CENTER_LEFT);
        inheritCheck = new CheckBox("Inherit values from:");
        inheritCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        inheritCheckRow.getChildren().add(inheritCheck);

        HBox inheritTargetRow = new HBox(6);
        inheritTargetRow.setAlignment(Pos.CENTER_LEFT);
        inheritTargetRow.setPadding(new Insets(0, 0, 0, 22));

        inheritTargetLabel = new Hyperlink("");
        inheritTargetLabel.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        inheritTargetLabel.setOnAction(e -> {
            if (onNavigateToInherited != null && inheritTargetLabel.getUserData() != null) {
                onNavigateToInherited.accept((String) inheritTargetLabel.getUserData());
            }
        });

        inheritScopeLabel = new Label("(Language Defaults)");
        inheritScopeLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        inheritTargetRow.getChildren().addAll(inheritTargetLabel, inheritScopeLabel);
        inheritBox.getChildren().addAll(inheritCheckRow, inheritTargetRow);
        inheritBox.setVisible(false);
        inheritBox.setManaged(false);

        attributeEditor.getChildren().addAll(
                fontBox,
                fgRow,
                bgRow,
                esRow,
                effRow,
                effectTypeRow,
                inheritBox
        );

        // Top Split: Tree + Attribute Editor
        HBox topSplit = new HBox(16);
        topSplit.getChildren().addAll(categoryTree, attributeEditor);
        HBox.setHgrow(categoryTree, Priority.NEVER);
        HBox.setHgrow(attributeEditor, Priority.ALWAYS);

        // ---------------- Bottom Preview Pane ----------------
        codeLinesBox = new VBox(8);
        codeLinesBox.setPadding(new Insets(12, 14, 12, 14));
        codeLinesBox.setStyle("-fx-background-color: #1E1F22;");

        previewScrollPane = new ScrollPane(codeLinesBox);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setPrefHeight(230);
        previewScrollPane.setMinHeight(160);
        previewScrollPane.setStyle(
                "-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;"
        );

        getChildren().addAll(headerBar, topSplit, previewScrollPane);
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        initListeners();
        loadScheme(settings.getActiveSchemeName());

        categoryTree.getSelectionModel().select(inputCountItem);
        selectCategory("Input execution count");
    }

    private Button createSwatchButton() {
        Button btn = new Button();
        btn.setPrefSize(72, 22);
        btn.setMinSize(72, 22);
        btn.setMaxSize(72, 22);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-padding: 0;");
        return btn;
    }

    private void updateSwatch(Button btn, String hexColor) {
        if (hexColor == null || hexColor.isBlank()) {
            btn.setText("");
            btn.setStyle("-fx-background-color: transparent; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-text-fill: #848BA3; -fx-font-size: 11px;");
        } else {
            String cleanHex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
            btn.setText(cleanHex.toUpperCase());
            btn.setStyle(
                    "-fx-background-color: #" + cleanHex + "33; " +
                    "-fx-border-color: #" + cleanHex + "; " +
                    "-fx-border-radius: 3; -fx-background-radius: 3; " +
                    "-fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-font-weight: bold;"
            );
        }
    }

    private void initListeners() {
        boldCheck.setOnAction(e -> onAttributeControlChanged());
        italicCheck.setOnAction(e -> onAttributeControlChanged());

        foregroundCheck.setOnAction(e -> {
            if (suppressEvents) return;
            if (foregroundCheck.isSelected() && foregroundHex == null) {
                foregroundHex = "3E7CD7";
                updateSwatch(foregroundSwatch, foregroundHex);
            }
            onAttributeControlChanged();
        });

        foregroundSwatch.setOnAction(e -> openColorPicker("Foreground Color", foregroundHex, color -> {
            foregroundHex = color;
            foregroundCheck.setSelected(true);
            updateSwatch(foregroundSwatch, foregroundHex);
            onAttributeControlChanged();
        }));

        backgroundCheck.setOnAction(e -> {
            if (suppressEvents) return;
            if (backgroundCheck.isSelected() && backgroundHex == null) {
                backgroundHex = "26282E";
                updateSwatch(backgroundSwatch, backgroundHex);
            }
            onAttributeControlChanged();
        });

        backgroundSwatch.setOnAction(e -> openColorPicker("Background Color", backgroundHex, color -> {
            backgroundHex = color;
            backgroundCheck.setSelected(true);
            updateSwatch(backgroundSwatch, backgroundHex);
            onAttributeControlChanged();
        }));

        errorStripeCheck.setOnAction(e -> {
            if (suppressEvents) return;
            if (errorStripeCheck.isSelected() && errorStripeHex == null) {
                errorStripeHex = "F75464";
                updateSwatch(errorStripeSwatch, errorStripeHex);
            }
            onAttributeControlChanged();
        });

        errorStripeSwatch.setOnAction(e -> openColorPicker("Error Stripe Mark Color", errorStripeHex, color -> {
            errorStripeHex = color;
            errorStripeCheck.setSelected(true);
            updateSwatch(errorStripeSwatch, errorStripeHex);
            onAttributeControlChanged();
        }));

        effectsCheck.setOnAction(e -> {
            if (suppressEvents) return;
            if (effectsCheck.isSelected() && effectsHex == null) {
                effectsHex = "3E7CD7";
                updateSwatch(effectsSwatch, effectsHex);
            }
            onAttributeControlChanged();
        });

        effectsSwatch.setOnAction(e -> openColorPicker("Effects Color", effectsHex, color -> {
            effectsHex = color;
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
        for (AttributesDescriptor desc : EditorColorSchemeSettings.getJupyterNotebooksDescriptors()) {
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

                boolean hasEff = attr.getEffectColor() != null;
                effectsCheck.setSelected(hasEff);
                effectsHex = hasEff ? attr.getEffectColor() : (desc != null && desc.getDefaultAttribute().getEffectColor() != null ? desc.getDefaultAttribute().getEffectColor() : null);
                updateSwatch(effectsSwatch, effectsHex);

                if (attr.getEffectType() != null && attr.getEffectType() != EffectType.NONE) {
                    effectTypeCombo.setValue(attr.getEffectType());
                } else if (desc != null && desc.getDefaultEffectType() != null) {
                    effectTypeCombo.setValue(desc.getDefaultEffectType());
                } else {
                    effectTypeCombo.setValue(EffectType.UNDERSCORED);
                }

                boolean hasInherit = desc != null && desc.hasInheritance();
                inheritBox.setVisible(hasInherit);
                inheritBox.setManaged(hasInherit);
                if (hasInherit) {
                    inheritCheck.setSelected(attr.isInherit());
                    inheritTargetLabel.setText(desc.getInheritTarget());
                    inheritTargetLabel.setUserData(desc.getInheritTarget());
                    inheritScopeLabel.setText(desc.getInheritScope() != null ? desc.getInheritScope() : "");
                }
            }
        } finally {
            suppressEvents = false;
        }
        updatePreview();
    }

    public ColorAttribute getCurrentEditorAttribute() {
        ColorAttribute attr = new ColorAttribute();
        attr.setBold(boldCheck.isSelected());
        attr.setItalic(italicCheck.isSelected());
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

    private void updatePreview() {
        codeLinesBox.getChildren().clear();

        // 1. Code cell 1
        VBox cell1 = new VBox(4);
        cell1.setPadding(new Insets(8, 12, 8, 12));
        cell1.setStyle("-fx-background-color: #26282E; -fx-border-color: #3574F0; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");

        HBox inputRow = new HBox(8);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        Label inLabel = new Label("In [1]:");
        inLabel.setFont(Font.font("Monospaced", 13));
        String inFg = foregroundHex != null && "Input execution count".equals(selectedKey) ? foregroundHex : "#3E7CD7";
        if (!inFg.startsWith("#")) inFg = "#" + inFg;
        inLabel.setStyle("-fx-text-fill: " + inFg + "; -fx-font-weight: bold; -fx-cursor: hand;");
        inLabel.setOnMouseClicked(e -> selectTreeItem("Input execution count"));

        Label code1 = new Label("import numpy as np\nprint(\"Lumina Jupyter Integration\")");
        code1.setFont(Font.font("Monospaced", 13));
        code1.setStyle("-fx-text-fill: #BCBEC4;");

        inputRow.getChildren().addAll(inLabel, code1);
        cell1.getChildren().add(inputRow);

        // Output of cell 1
        HBox outputRow = new HBox(8);
        outputRow.setAlignment(Pos.CENTER_LEFT);

        Label outLabel = new Label("Out[1]:");
        outLabel.setFont(Font.font("Monospaced", 13));
        outLabel.setStyle("-fx-text-fill: #7A7E85; -fx-cursor: hand;");
        outLabel.setOnMouseClicked(e -> selectTreeItem("Output execution count"));

        Label outText = new Label("Lumina Jupyter Integration");
        outText.setFont(Font.font("Monospaced", 13));
        outText.setStyle("-fx-text-fill: #6AAB73;");

        outputRow.getChildren().addAll(outLabel, outText);
        cell1.getChildren().add(outputRow);

        // 2. Cell 2 (Running / progress)
        VBox cell2 = new VBox(4);
        cell2.setPadding(new Insets(8, 12, 8, 12));
        cell2.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");

        HBox inputRow2 = new HBox(8);
        inputRow2.setAlignment(Pos.CENTER_LEFT);

        Label inLabel2 = new Label("In [*]:");
        inLabel2.setFont(Font.font("Monospaced", 13));
        inLabel2.setStyle("-fx-text-fill: #3E7CD7; -fx-cursor: hand;");
        inLabel2.setOnMouseClicked(e -> selectTreeItem("Input execution count"));

        Label code2 = new Label("time.sleep(2)  # Cell executing");
        code2.setFont(Font.font("Monospaced", 13));
        code2.setStyle("-fx-text-fill: #BCBEC4;");

        inputRow2.getChildren().addAll(inLabel2, code2);

        ProgressBar pb = new ProgressBar(0.6);
        pb.setPrefWidth(220);
        pb.setStyle("-fx-accent: #3574F0; -fx-cursor: hand;");
        pb.setOnMouseClicked(e -> selectTreeItem("Progress bar of a running cell"));

        cell2.getChildren().addAll(inputRow2, pb);

        codeLinesBox.getChildren().addAll(cell1, cell2);
    }

    private void selectTreeItem(String key) {
        for (TreeItem<String> item : rootItem.getChildren()) {
            if (item.getValue().equals(key)) {
                categoryTree.getSelectionModel().select(item);
                selectCategory(key);
                return;
            }
            if (item.getChildren() != null) {
                for (TreeItem<String> child : item.getChildren()) {
                    if (child.getValue().equals(key)) {
                        categoryTree.getSelectionModel().select(child);
                        selectCategory(key);
                        return;
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Color Picker Dialog
    // -------------------------------------------------------------------------

    private void openColorPicker(String title, String currentHex, Consumer<String> onColorSelected) {
        Stage pickerStage = new Stage();
        pickerStage.initModality(Modality.APPLICATION_MODAL);
        pickerStage.initStyle(StageStyle.UTILITY);
        pickerStage.setTitle(title);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #2B2D30;");

        Color initial = Color.web("#BCBEC4");
        if (currentHex != null && !currentHex.isBlank()) {
            try {
                String clean = currentHex.startsWith("#") ? currentHex : "#" + currentHex;
                initial = Color.web(clean);
            } catch (Exception ignored) {}
        }

        ColorPicker picker = new ColorPicker(initial);
        picker.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5;");

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5;");
        cancelBtn.setOnAction(e -> pickerStage.close());

        Button chooseBtn = new Button("Choose");
        chooseBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        chooseBtn.setOnAction(e -> {
            Color c = picker.getValue();
            String hex = String.format("%02X%02X%02X",
                    (int) (c.getRed() * 255),
                    (int) (c.getGreen() * 255),
                    (int) (c.getBlue() * 255));
            onColorSelected.accept(hex);
            pickerStage.close();
        });

        btnBox.getChildren().addAll(cancelBtn, chooseBtn);
        root.getChildren().addAll(picker, btnBox);

        Scene scene = new Scene(root);
        pickerStage.setScene(scene);
        pickerStage.showAndWait();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void apply() {
        ColorAttribute attr = getCurrentEditorAttribute();
        settings.setAttribute(settings.getActiveSchemeName(), selectedKey, attr);
        originalAttributes.put(selectedKey, attr.clone());
        isModified = false;
        if (onModifiedListener != null) onModifiedListener.run();
    }

    public void reset() {
        loadScheme(settings.getActiveSchemeName());
        isModified = false;
        if (onModifiedListener != null) onModifiedListener.run();
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

    public VBox getInheritBox() {
        return inheritBox;
    }

    public VBox getCodeLinesBox() {
        return codeLinesBox;
    }
}
