package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.AttributesDescriptor;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import dev.lumina.settings.EditorColorSchemeSettings.EffectType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;

/**
 * Editor > Color Scheme > VCS settings page.
 * Replicates 1:1 visual and functional parity with reference screenshot media_1790425840060.png:
 * - Top header bar with Scheme selector, theme link, and help button.
 * - Dynamic category tree with "Editor Gutter" (9 items) and "VCS Annotations" (7 items).
 * - Full attribute editor panel with Bold, Italic, Foreground, Background, Error stripe mark, and Effects.
 * - Default selection: "Changed lines popup".
 * - Live interactive VCS preview pane with VCS annotations gutter, line numbers, change markers
 *   (added, modified, deleted, whitespace, ignored borders), change popup banner, and bidirectional token click selection.
 */
public class SettingsColorSchemeVcsPage extends VBox {

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

    // Preview
    private final VBox previewBox;
    private final VBox annotationColumn;
    private final VBox lineNumColumn;
    private final VBox editorLinesColumn;

    // State
    private String selectedKey = "Changed lines popup";
    private String foregroundHex = null;
    private String backgroundHex = null;
    private String errorStripeHex = null;
    private String effectsHex = null;
    private boolean suppressEvents = false;
    private boolean modified = false;

    private Runnable onModifiedListener;

    public SettingsColorSchemeVcsPage() {
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
                    setStyle("-fx-background-color: transparent;");
                } else if (isSelected()) {
                    setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-padding: 3 8 3 8;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 8 3 8;");
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

        // Error stripe mark
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

        // Top horizontal split: Tree (left) + Attribute Editor (right)
        HBox topPane = new HBox(16);
        topPane.setPrefHeight(270);
        topPane.setMinHeight(220);
        HBox.setHgrow(categoryTree, Priority.ALWAYS);
        topPane.getChildren().addAll(categoryTree, attributeEditorBox);

        // 4. Live VCS Preview Pane
        previewBox = new VBox();
        previewBox.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(previewBox, Priority.ALWAYS);

        annotationColumn = new VBox();
        annotationColumn.setPrefWidth(190);
        annotationColumn.setMinWidth(170);

        lineNumColumn = new VBox();
        lineNumColumn.setPrefWidth(32);
        lineNumColumn.setMinWidth(28);
        lineNumColumn.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent #2B2D30 transparent transparent;");

        editorLinesColumn = new VBox();
        HBox.setHgrow(editorLinesColumn, Priority.ALWAYS);

        HBox previewCols = new HBox();
        previewCols.getChildren().addAll(annotationColumn, lineNumColumn, editorLinesColumn);

        ScrollPane previewScroll = new ScrollPane(previewCols);
        previewScroll.setFitToWidth(true);
        previewScroll.setStyle("-fx-background: #1E1F22; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(previewScroll, Priority.ALWAYS);
        previewBox.getChildren().add(previewScroll);

        getChildren().addAll(headerBar, topPane, previewBox);

        initListeners();
        selectDefaultKey();
        updatePreview();
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

        // 1. Editor Gutter
        TreeItem<String> gutterNode = new TreeItem<>("Editor Gutter");
        gutterNode.setExpanded(true);
        String[] gutterItems = {
                "Added ignored lines border",
                "Added lines",
                "Border",
                "Changed lines popup",
                "Deleted ignored lines border",
                "Deleted lines",
                "Modified ignored lines border",
                "Modified lines",
                "Whitespace-modified lines"
        };
        for (String item : gutterItems) {
            gutterNode.getChildren().add(new TreeItem<>(item));
        }

        // 2. VCS Annotations
        TreeItem<String> annotNode = new TreeItem<>("VCS Annotations");
        annotNode.setExpanded(true);
        String[] annotItems = {
                "Background color #1",
                "Background color #2",
                "Background color #3",
                "Background color #4",
                "Background color #5",
                "Foreground",
                "Foreground for last commit"
        };
        for (String item : annotItems) {
            annotNode.getChildren().add(new TreeItem<>(item));
        }

        root.getChildren().addAll(gutterNode, annotNode);
        categoryTree.setRoot(root);
    }

    private void selectDefaultKey() {
        selectTreeItem("Changed lines popup");
    }

    public void selectTreeItem(String key) {
        if (key == null || categoryTree.getRoot() == null) return;
        for (TreeItem<String> group : categoryTree.getRoot().getChildren()) {
            if (key.equals(group.getValue())) {
                categoryTree.getSelectionModel().select(group);
                return;
            }
            for (TreeItem<String> child : group.getChildren()) {
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
                updatePreview();
            }
        });

        boldCheck.setOnAction(e -> onAttributeControlChanged());
        italicCheck.setOnAction(e -> onAttributeControlChanged());

        foregroundCheck.setOnAction(e -> {
            if (foregroundCheck.isSelected() && foregroundHex == null) {
                foregroundHex = "#BCBEC4";
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
                backgroundHex = "#2B2D30";
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
                errorStripeHex = "#436980";
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
                effectsHex = "#3574F0";
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

    private void loadAttributesForSelectedKey() {
        suppressEvents = true;
        try {
            EditorColorSchemeSettings settings = EditorColorSchemeSettings.getInstance();
            ColorAttribute attr = settings.getAttribute(headerBar.getSelectedScheme(), selectedKey);
            AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);

            if (attr == null && desc != null) {
                attr = desc.getDefaultAttribute();
            }

            boolean isAnnotFg = "Foreground".equals(selectedKey) || "Foreground for last commit".equals(selectedKey);
            boldCheck.setDisable(!isAnnotFg);
            italicCheck.setDisable(!isAnnotFg);
            boldCheck.setSelected(attr != null && attr.isBold());
            italicCheck.setSelected(attr != null && attr.isItalic());

            // Foreground
            boolean hasFg = attr != null && attr.getForeground() != null && !attr.getForeground().isBlank();
            foregroundCheck.setSelected(hasFg);
            foregroundHex = hasFg ? attr.getForeground() : null;
            updateSwatch(foregroundSwatch, foregroundHex);

            // Background
            boolean hasBg = attr != null && attr.getBackground() != null && !attr.getBackground().isBlank();
            backgroundCheck.setSelected(hasBg);
            backgroundHex = hasBg ? attr.getBackground() : null;
            updateSwatch(backgroundSwatch, backgroundHex);

            // Error stripe
            boolean hasStripe = attr != null && attr.getErrorStripeColor() != null && !attr.getErrorStripeColor().isBlank();
            errorStripeCheck.setSelected(hasStripe);
            errorStripeHex = hasStripe ? attr.getErrorStripeColor() : null;
            updateSwatch(errorStripeSwatch, errorStripeHex);

            // Effects
            boolean hasEff = attr != null && attr.getEffectColor() != null && !attr.getEffectColor().isBlank() && attr.getEffectType() != EffectType.NONE;
            effectsCheck.setSelected(hasEff);
            effectsHex = hasEff ? attr.getEffectColor() : null;
            updateSwatch(effectsSwatch, effectsHex);
            effectTypeCombo.setValue(attr != null && attr.getEffectType() != EffectType.NONE ? attr.getEffectType() : EffectType.UNDERSCORED);

        } finally {
            suppressEvents = false;
        }
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

    private void onAttributeControlChanged() {
        if (suppressEvents) return;
        updateSwatch(foregroundSwatch, foregroundCheck.isSelected() ? foregroundHex : null);
        updateSwatch(backgroundSwatch, backgroundCheck.isSelected() ? backgroundHex : null);
        updateSwatch(errorStripeSwatch, errorStripeCheck.isSelected() ? errorStripeHex : null);
        updateSwatch(effectsSwatch, effectsCheck.isSelected() ? effectsHex : null);

        modified = true;
        notifyModified();
        updatePreview();
    }

    private void openColorPicker(String title, String initialColor, java.util.function.Consumer<String> onSelected) {
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

    // -------------------------------------------------------------------------
    // Preview Rendering & Synchronization
    // -------------------------------------------------------------------------

    private static class VcsPreviewLine {
        final int lineNum;
        final String text;
        final String annotationText;
        final String annotationBgKey;
        final String gutterKey;
        final boolean isPopupTarget;

        VcsPreviewLine(int lineNum, String text, String annotationText, String annotationBgKey, String gutterKey, boolean isPopupTarget) {
            this.lineNum = lineNum;
            this.text = text;
            this.annotationText = annotationText;
            this.annotationBgKey = annotationBgKey;
            this.gutterKey = gutterKey;
            this.isPopupTarget = isPopupTarget;
        }
    }

    private void updatePreview() {
        annotationColumn.getChildren().clear();
        lineNumColumn.getChildren().clear();
        editorLinesColumn.getChildren().clear();

        EditorColorSchemeSettings settings = EditorColorSchemeSettings.getInstance();
        String scheme = headerBar.getSelectedScheme();

        // Color lookups
        String annotBg1 = getColorHex(settings, scheme, "Background color #1", "#23382B");
        String annotBg2 = getColorHex(settings, scheme, "Background color #2", "#273548");
        String annotBg3 = getColorHex(settings, scheme, "Background color #3", "#3D382B");
        String annotBg4 = getColorHex(settings, scheme, "Background color #4", "#3C2B38");

        String annotFg = getColorHex(settings, scheme, "Foreground", "#868A91");
        String annotLastCommitFg = getColorHex(settings, scheme, "Foreground for last commit", "#DFE1E5");

        String addedGutterColor = getColorHex(settings, scheme, "Added lines", "#385E39");
        String modifiedGutterColor = getColorHex(settings, scheme, "Modified lines", "#385570");
        String wsModifiedGutterColor = getColorHex(settings, scheme, "Whitespace-modified lines", "#4E535E");

        List<VcsPreviewLine> lines = List.of(
                new VcsPreviewLine(1, "Deleted line below", "Annotation background #1", annotBg1, "Deleted lines", true),
                new VcsPreviewLine(2, "", "Annotation background", annotBg1, "Deleted lines", false),
                new VcsPreviewLine(3, "Modified line", "Annotation background", annotBg1, "Modified lines", false),
                new VcsPreviewLine(4, "", "Annotation background", annotBg1, null, false),
                new VcsPreviewLine(5, "Added line", "Annotation background", annotBg1, "Added lines", false),
                new VcsPreviewLine(6, "", "Annotation background #2", annotBg2, null, false),
                new VcsPreviewLine(7, "Line with modified whitespaces", "Annotation background", annotBg2, "Whitespace-modified lines", false),
                new VcsPreviewLine(8, "", "Annotation background", annotBg2, null, false),
                new VcsPreviewLine(9, "Added line", "Annotation background", annotBg2, "Added lines", false),
                new VcsPreviewLine(10, "Line with modified whitespaces and deletion after", "Annotation background", annotBg2, "Whitespace-modified lines", false),
                new VcsPreviewLine(11, "", "Annotation background #3", annotBg3, "Deleted lines", false),
                new VcsPreviewLine(12, "Deleted ignored line below", "Annotation background", annotBg3, "Deleted ignored lines border", false),
                new VcsPreviewLine(13, "", "Annotation background", annotBg3, "Deleted ignored lines border", false),
                new VcsPreviewLine(14, "Modified ignored line", "Annotation background", annotBg3, "Modified ignored lines border", false),
                new VcsPreviewLine(15, "", "Annotation background", annotBg3, null, false),
                new VcsPreviewLine(16, "Added ignored line", "Annotation background #4", annotBg4, "Added ignored lines border", false)
        );

        for (VcsPreviewLine line : lines) {
            // 1. Annotation item
            HBox annotRow = new HBox();
            annotRow.setPrefHeight(22);
            annotRow.setMinHeight(22);
            annotRow.setAlignment(Pos.CENTER_LEFT);
            annotRow.setPadding(new Insets(0, 8, 0, 8));
            annotRow.setStyle("-fx-background-color: " + line.annotationBgKey + "; -fx-cursor: hand;");

            Label annotLbl = new Label(line.annotationText);
            boolean isLastCommit = line.lineNum == 1 || line.lineNum == 6 || line.lineNum == 11 || line.lineNum == 16;
            annotLbl.setStyle("-fx-text-fill: " + (isLastCommit ? annotLastCommitFg : annotFg) + "; -fx-font-family: 'Monospaced'; -fx-font-size: 11px;");
            annotRow.getChildren().add(annotLbl);

            annotRow.setOnMouseClicked(e -> {
                if (line.lineNum <= 5) selectTreeItem("Background color #1");
                else if (line.lineNum <= 10) selectTreeItem("Background color #2");
                else if (line.lineNum <= 15) selectTreeItem("Background color #3");
                else selectTreeItem("Background color #4");
            });
            annotationColumn.getChildren().add(annotRow);

            // 2. Line number item
            HBox numRow = new HBox();
            numRow.setPrefHeight(22);
            numRow.setMinHeight(22);
            numRow.setAlignment(Pos.CENTER_RIGHT);
            numRow.setPadding(new Insets(0, 6, 0, 0));
            numRow.setStyle("-fx-background-color: #1E1F22;");
            Label numLbl = new Label(String.valueOf(line.lineNum));
            numLbl.setStyle("-fx-text-fill: #5E626B; -fx-font-family: 'Monospaced'; -fx-font-size: 11px;");
            numRow.getChildren().add(numLbl);
            lineNumColumn.getChildren().add(numRow);

            // 3. Editor Line with Gutter Mark
            HBox codeRow = new HBox(8);
            codeRow.setPrefHeight(22);
            codeRow.setMinHeight(22);
            codeRow.setAlignment(Pos.CENTER_LEFT);
            codeRow.setPadding(new Insets(0, 8, 0, 4));
            codeRow.setStyle("-fx-cursor: hand;");

            // Gutter marker graphic (left 8px)
            Pane gutterMarker = createGutterMarker(line.gutterKey, line.lineNum, addedGutterColor, modifiedGutterColor, wsModifiedGutterColor);
            gutterMarker.setPrefSize(8, 22);
            gutterMarker.setMinSize(8, 22);

            Label codeLbl = new Label(line.text);
            codeLbl.setStyle("-fx-text-fill: #BCBEC4; -fx-font-family: 'Monospaced'; -fx-font-size: 12px;");

            // Row background styling
            if ("Changed lines popup".equals(selectedKey) && line.isPopupTarget) {
                // Line 1 selected popup styling matching Screenshot 1
                codeRow.setStyle("-fx-background-color: #2E436E; -fx-cursor: hand;");
                codeLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: 'Monospaced'; -fx-font-size: 12px;");
            } else if (line.gutterKey != null && line.gutterKey.equals(selectedKey)) {
                codeRow.setStyle("-fx-background-color: #2A313E; -fx-cursor: hand;");
            } else {
                codeRow.setStyle("-fx-background-color: #1E1F22; -fx-cursor: hand;");
            }

            codeRow.getChildren().addAll(gutterMarker, codeLbl);

            codeRow.setOnMouseClicked(e -> {
                if (line.isPopupTarget) {
                    selectTreeItem("Changed lines popup");
                } else if (line.gutterKey != null) {
                    selectTreeItem(line.gutterKey);
                }
            });

            editorLinesColumn.getChildren().add(codeRow);
        }
    }

    private Pane createGutterMarker(String gutterKey, int lineNum, String addCol, String modCol, String wsCol) {
        Pane pane = new Pane();
        if (gutterKey == null) return pane;

        if ("Added lines".equals(gutterKey)) {
            Rectangle rect = new Rectangle(4, 22);
            rect.setFill(Color.web(addCol));
            pane.getChildren().add(rect);
        } else if ("Modified lines".equals(gutterKey)) {
            Rectangle rect = new Rectangle(4, 22);
            rect.setFill(Color.web(modCol));
            pane.getChildren().add(rect);
        } else if ("Whitespace-modified lines".equals(gutterKey)) {
            Rectangle rect = new Rectangle(4, 22);
            rect.setFill(Color.web(wsCol));
            pane.getChildren().add(rect);
        } else if ("Deleted lines".equals(gutterKey)) {
            Polygon triangle = new Polygon();
            triangle.getPoints().addAll(
                    0.0, 16.0,
                    6.0, 20.0,
                    0.0, 22.0
            );
            triangle.setFill(Color.web("#6E3B3B"));
            pane.getChildren().add(triangle);
        } else if ("Added ignored lines border".equals(gutterKey)) {
            Rectangle rect = new Rectangle(4, 22);
            rect.setFill(Color.web("#3C4E42"));
            rect.setStroke(Color.web("#5C7E64"));
            rect.setStrokeWidth(1);
            pane.getChildren().add(rect);
        } else if ("Modified ignored lines border".equals(gutterKey)) {
            Rectangle rect = new Rectangle(4, 22);
            rect.setFill(Color.web("#3D4C59"));
            rect.setStroke(Color.web("#5D7080"));
            rect.setStrokeWidth(1);
            pane.getChildren().add(rect);
        } else if ("Deleted ignored lines border".equals(gutterKey)) {
            Polygon triangle = new Polygon();
            triangle.getPoints().addAll(
                    0.0, 16.0,
                    6.0, 20.0,
                    0.0, 22.0
            );
            triangle.setFill(Color.web("#544040"));
            pane.getChildren().add(triangle);
        }
        return pane;
    }

    private String getColorHex(EditorColorSchemeSettings settings, String scheme, String key, String defaultVal) {
        ColorAttribute a = settings.getAttribute(scheme, key);
        if (a != null) {
            if (a.getBackground() != null && !a.getBackground().isBlank()) {
                return a.getBackground().startsWith("#") ? a.getBackground() : "#" + a.getBackground();
            }
            if (a.getForeground() != null && !a.getForeground().isBlank()) {
                return a.getForeground().startsWith("#") ? a.getForeground() : "#" + a.getForeground();
            }
        }
        return defaultVal;
    }

    // -------------------------------------------------------------------------
    // Lifecycle & Public API
    // -------------------------------------------------------------------------

    public void apply() {
        if (!modified) return;
        EditorColorSchemeSettings settings = EditorColorSchemeSettings.getInstance();
        String scheme = headerBar.getSelectedScheme();

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

        settings.setAttribute(scheme, selectedKey, attr);
        modified = false;
        notifyModified();
    }

    public void reset() {
        loadAttributesForSelectedKey();
        updatePreview();
        modified = false;
        notifyModified();
    }

    public boolean isModified() {
        return modified;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Getters for tests
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
    public VBox getEditorLinesColumn() { return editorLinesColumn; }
    public VBox getAnnotationColumn() { return annotationColumn; }
}
