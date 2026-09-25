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
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.*;

/**
 * Editor > Color Scheme > General settings page.
 * Matches 1:1 with IntelliJ reference screenshots:
 * - media_1790343268397.png through media_1790343353746.png
 * - media_1790343452222.png through media_1790343516571.png
 *
 * Dynamic Features:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - Upper Section:
 *     Left: Category TreeView with exact hierarchy (Code, Editor with Breadcrumbs, Guides, Tabs, Sticky Lines, etc.).
 *     Right: Attribute Editor panel matching exact layout (Bold/Italic at top right, Foreground, Background,
 *            Error stripe mark, Effects with custom colored hex swatches, and "Inherit values from:" section).
 *            Empty/hidden when category group node is selected.
 * - Lower Section: Realistic live syntax-colored code preview with token-level styling,
 *   column guides, error stripe gutter, and bi-directional token-tree navigation.
 */
public class SettingsColorSchemeGeneralPage extends VBox {

    private final ColorSchemeHeaderBar headerBar = new ColorSchemeHeaderBar();

    // Category tree
    private final TreeView<String> categoryTree = new TreeView<>();

    // Attribute editor panel and controls
    private final VBox attributeEditorBox = new VBox(10);
    private final CheckBox boldCheck = new CheckBox("Bold");
    private final CheckBox italicCheck = new CheckBox("Italic");

    private final CheckBox foregroundCheck = new CheckBox("Foreground");
    private final Button foregroundSwatch = new Button();
    private String foregroundHex = null;

    private final CheckBox backgroundCheck = new CheckBox("Background");
    private final Button backgroundSwatch = new Button();
    private String backgroundHex = null;

    private final CheckBox errorStripeCheck = new CheckBox("Error stripe mark");
    private final Button errorStripeSwatch = new Button();
    private String errorStripeHex = null;

    private final CheckBox effectsCheck = new CheckBox("Effects");
    private final Button effectsSwatch = new Button();
    private String effectsHex = null;
    private final ComboBox<EffectType> effectTypeCombo = new ComboBox<>();

    // Inheritance controls (media_1790343485724.png & media_1790343496020.png)
    private final VBox inheritBox = new VBox(4);
    private final CheckBox inheritCheck = new CheckBox("Inherit values from:");
    private final Hyperlink inheritLink = new Hyperlink();
    private final Label inheritScopeLabel = new Label("(General)");
    private String currentInheritedTargetKey = null;

    // Lower preview pane
    private final Pane previewContentPane = new Pane();
    private final VBox codeLinesBox = new VBox(2);
    private final Pane errorStripeGutter = new Pane();

    // Selection history for back/forward navigation
    private final List<String> navigationHistory = new ArrayList<>();
    private int historyIndex = -1;
    private boolean isNavigatingHistory = false;

    private Runnable onModifiedListener;
    private boolean suppressEvents = false;
    private String selectedKey = "Code // Identifier under caret";

    public SettingsColorSchemeGeneralPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(12);

        buildUi();
        setupListeners();
        selectTreeItem(selectedKey);
        updatePreview();
    }

    private void buildUi() {
        // --- Upper Section: TreeView + Attribute Options ---
        HBox upperSection = new HBox(16);
        upperSection.setPrefHeight(280);
        upperSection.setMinHeight(260);
        upperSection.setMaxHeight(320);

        buildCategoryTree();
        categoryTree.setPrefWidth(380);
        categoryTree.setMinWidth(320);
        categoryTree.getStyleClass().add("color-scheme-tree");
        categoryTree.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        categoryTree.setCellFactory(tv -> new TreeCell<>() {
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

        buildAttributeEditor();
        HBox.setHgrow(attributeEditorBox, Priority.ALWAYS);

        upperSection.getChildren().addAll(categoryTree, attributeEditorBox);

        // --- Lower Section: Live Interactive Code Preview ---
        VBox lowerSection = buildPreviewSection();
        VBox.setVgrow(lowerSection, Priority.ALWAYS);

        getChildren().addAll(headerBar, upperSection, lowerSection);
    }

    private void buildCategoryTree() {
        TreeItem<String> root = new TreeItem<>("Root");
        root.setExpanded(true);

        // Code (media_1790343290072.png & media_1790343304243.png)
        addTreeCategory(root, "Code", List.of(
                "Identifier under caret",
                "Identifier under caret (write)",
                "Injected language fragment",
                "Line number",
                "Line number on caret row",
                "Matched brace",
                "Method separator color",
                "TODO defaults",
                "Unmatched brace"
        ));

        // Editor (media_1790343317824.png, media_1790343353746.png, media_1790343452222.png - media_1790343516571.png)
        TreeItem<String> editorCat = new TreeItem<>("Editor");
        editorCat.setExpanded(true);
        editorCat.getChildren().add(new TreeItem<>("Bookmarks"));

        TreeItem<String> breadcrumbs = new TreeItem<>("Breadcrumbs");
        breadcrumbs.setExpanded(true);
        breadcrumbs.getChildren().addAll(
                new TreeItem<>("Border"),
                new TreeItem<>("Current"),
                new TreeItem<>("Default"),
                new TreeItem<>("Hovered"),
                new TreeItem<>("Inactive")
        );
        editorCat.getChildren().add(breadcrumbs);

        editorCat.getChildren().add(new TreeItem<>("Caret"));
        editorCat.getChildren().add(new TreeItem<>("Caret row"));

        // Guides (media_1790343452222.png)
        TreeItem<String> guides = new TreeItem<>("Guides");
        guides.getChildren().addAll(
                new TreeItem<>("Hard wrap guide"),
                new TreeItem<>("Indent guide"),
                new TreeItem<>("Indent guide selected"),
                new TreeItem<>("Matched brace guide"),
                new TreeItem<>("Visual guides")
        );
        editorCat.getChildren().add(guides);

        editorCat.getChildren().addAll(
                new TreeItem<>("Gutter background"),
                new TreeItem<>("Notification background"),
                new TreeItem<>("Selection background"),
                new TreeItem<>("Selection foreground")
        );

        // Sticky Lines (media_1790343472376.png, media_1790343485724.png, media_1790343496020.png)
        TreeItem<String> stickyLines = new TreeItem<>("Sticky Lines");
        stickyLines.getChildren().addAll(
                new TreeItem<>("Background"),
                new TreeItem<>("Border"),
                new TreeItem<>("Hovered")
        );
        editorCat.getChildren().add(stickyLines);

        // Tabs (media_1790343516571.png)
        TreeItem<String> tabs = new TreeItem<>("Tabs");
        tabs.getChildren().addAll(
                new TreeItem<>("Modified icon color"),
                new TreeItem<>("Selected Tab"),
                new TreeItem<>("Selected Tab inactive"),
                new TreeItem<>("Underline"),
                new TreeItem<>("Underline inactive")
        );
        editorCat.getChildren().add(tabs);

        editorCat.getChildren().addAll(
                new TreeItem<>("Tear line"),
                new TreeItem<>("Tear line selection")
        );

        TreeItem<String> scrollbar = new TreeItem<>("Vertical Scrollbar");
        scrollbar.getChildren().addAll(
                new TreeItem<>("Thumb"),
                new TreeItem<>("Thumb while scrolling"),
                new TreeItem<>("Track")
        );
        editorCat.getChildren().add(scrollbar);

        root.getChildren().add(editorCat);

        // Errors and Warnings (media_1790345048357.png - media_1790345076709.png)
        addTreeCategory(root, "Errors and Warnings", List.of(
                "Deprecated symbol",
                "Deprecated symbol marked for removal",
                "Duplicate from server",
                "Error",
                "Grammar error",
                "Problem from server",
                "Runtime problem",
                "Text style suggestion",
                "Typo",
                "Unknown symbol",
                "Unused code",
                "Warning",
                "Weak Warning"
        ));

        // Hyperlinks
        addTreeCategory(root, "Hyperlinks", List.of("Inactive hyperlink", "Followed hyperlink", "Reference hyperlink"));

        // Identifiers
        addTreeCategory(root, "Identifiers", List.of("Identifier under caret", "Identifier under caret (write)"));

        // Line Coverage
        addTreeCategory(root, "Line Coverage", List.of("Full coverage", "Partial coverage", "Uncovered"));

        // Live Templates
        addTreeCategory(root, "Live Templates", List.of("Active template", "Inactive template"));

        // Popups and Hints
        addTreeCategory(root, "Popups and Hints", List.of("Parameter hint", "Inlay hint"));

        // Preview
        addTreeCategory(root, "Preview", List.of("Preview scope"));

        // Search Results
        addTreeCategory(root, "Search Results", List.of("Search result", "Search result (write access)"));

        // Text
        addTreeCategory(root, "Text", List.of("Default text", "Folded text", "Deleted text", "Injected language fragment"));

        categoryTree.setRoot(root);
        categoryTree.setShowRoot(false);
    }

    private void addTreeCategory(TreeItem<String> root, String category, List<String> children) {
        TreeItem<String> catItem = new TreeItem<>(category);
        catItem.setExpanded(false);
        for (String child : children) {
            catItem.getChildren().add(new TreeItem<>(child));
        }
        root.getChildren().add(catItem);
    }

    private void buildAttributeEditor() {
        attributeEditorBox.setPadding(new Insets(6, 16, 12, 16));
        attributeEditorBox.setStyle("-fx-background-color: transparent;");

        // Top-right aligned Bold and Italic checkboxes (media_1790343290072.png)
        styleCheckBox(boldCheck);
        styleCheckBox(italicCheck);
        HBox fontStyleRow = new HBox(16, boldCheck, italicCheck);
        fontStyleRow.setAlignment(Pos.CENTER_RIGHT);

        // Foreground row
        styleCheckBox(foregroundCheck);
        setupSwatchButton(foregroundSwatch, foregroundCheck, hex -> {
            foregroundHex = hex;
            handleAttributeChanged();
        });
        HBox fgRow = createAttributeRow(foregroundCheck, foregroundSwatch);

        // Background row
        styleCheckBox(backgroundCheck);
        setupSwatchButton(backgroundSwatch, backgroundCheck, hex -> {
            backgroundHex = hex;
            handleAttributeChanged();
        });
        HBox bgRow = createAttributeRow(backgroundCheck, backgroundSwatch);

        // Error stripe mark row
        styleCheckBox(errorStripeCheck);
        setupSwatchButton(errorStripeSwatch, errorStripeCheck, hex -> {
            errorStripeHex = hex;
            handleAttributeChanged();
        });
        HBox stripeRow = createAttributeRow(errorStripeCheck, errorStripeSwatch);

        // Effects row
        styleCheckBox(effectsCheck);
        setupSwatchButton(effectsSwatch, effectsCheck, hex -> {
            effectsHex = hex;
            handleAttributeChanged();
        });
        HBox effectsRow = createAttributeRow(effectsCheck, effectsSwatch);

        // Effects dropdown indented below effects row
        effectTypeCombo.getItems().setAll(
                EffectType.UNDERSCORED,
                EffectType.BOLD_UNDERSCORED,
                EffectType.UNDERWAVED,
                EffectType.BORDERED,
                EffectType.STRIKEOUT,
                EffectType.DOTTED_LINE
        );
        effectTypeCombo.setValue(EffectType.UNDERSCORED);
        effectTypeCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        effectTypeCombo.setPrefWidth(140);
        HBox effectComboRow = new HBox(effectTypeCombo);
        effectComboRow.setPadding(new Insets(0, 0, 0, 24));

        // Inheritance section matching media_1790343485724.png and media_1790343496020.png
        inheritBox.setSpacing(4);
        inheritBox.setPadding(new Insets(14, 0, 0, 0));
        styleCheckBox(inheritCheck);
        inheritCheck.setText("Inherit values from:");
        inheritCheck.selectedProperty().addListener((obs, o, n) -> handleInheritToggled(n));

        inheritLink.setStyle("-fx-text-fill: #589DF6; -fx-underline: false; -fx-padding: 0 0 0 22; -fx-font-size: 13px; -fx-cursor: hand;");
        inheritLink.setOnAction(e -> {
            if (currentInheritedTargetKey != null) {
                selectTreeItem(currentInheritedTargetKey);
            }
        });

        inheritScopeLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-padding: 0 0 0 22;");

        inheritBox.getChildren().addAll(inheritCheck, inheritLink, inheritScopeLabel);
        inheritBox.setVisible(false);
        inheritBox.setManaged(false);

        attributeEditorBox.getChildren().addAll(fontStyleRow, fgRow, bgRow, stripeRow, effectsRow, effectComboRow, inheritBox);
    }

    private HBox createAttributeRow(CheckBox cb, Button swatch) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(cb, spacer, swatch);
        return row;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void setupSwatchButton(Button btn, CheckBox boundCheck, java.util.function.Consumer<String> onHexChanged) {
        btn.setPrefWidth(72);
        btn.setMinWidth(72);
        btn.setMaxWidth(72);
        btn.setPrefHeight(24);
        btn.setMinHeight(24);
        btn.setMaxHeight(24);
        btn.setFont(Font.font("JetBrains Mono", 11));

        btn.setOnAction(e -> {
            if (inheritCheck.isSelected()) {
                inheritCheck.setSelected(false);
            }
            if (!boundCheck.isSelected()) {
                boundCheck.setSelected(true);
            }
            openColorPickerDialog(btn.getText(), hex -> {
                updateSwatchButton(btn, hex, true);
                onHexChanged.accept(hex);
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

    private void openColorPickerDialog(String currentHex, java.util.function.Consumer<String> onChosen) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Color");
        dialog.setHeaderText(null);

        ColorPicker picker = new ColorPicker();
        if (currentHex != null && !currentHex.isBlank()) {
            try {
                picker.setValue(Color.web("#" + (currentHex.startsWith("#") ? currentHex.substring(1) : currentHex)));
            } catch (Exception ignored) {}
        }

        TextField hexField = new TextField(currentHex != null ? currentHex : "DFE1E5");
        hexField.setPrefWidth(90);
        picker.valueProperty().addListener((obs, old, val) -> {
            hexField.setText(toHex(val).substring(1));
        });

        HBox content = new HBox(12, picker, new Label("#"), hexField);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(16));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? hexField.getText().trim() : null);

        dialog.showAndWait().ifPresent(onChosen);
    }

    private VBox buildPreviewSection() {
        VBox section = new VBox();
        section.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        StackPane editorStack = new StackPane();
        editorStack.setStyle("-fx-background-color: #1E1F22;");

        // Code lines container
        codeLinesBox.setPadding(new Insets(8, 14, 8, 14));

        // Background vertical column guide lines (IntelliJ visual guides)
        Pane columnGuidePane = new Pane();
        columnGuidePane.setMouseTransparent(true);
        Line guideLine1 = new Line(480, 0, 480, 800);
        guideLine1.setStroke(Color.web("#2B2D30"));
        guideLine1.setStrokeWidth(1);
        Line guideLine2 = new Line(560, 0, 560, 800);
        guideLine2.setStroke(Color.web("#282A2E"));
        guideLine2.setStrokeWidth(1);
        columnGuidePane.getChildren().addAll(guideLine1, guideLine2);

        // Error stripe gutter on the right
        errorStripeGutter.setPrefWidth(14);
        errorStripeGutter.setMinWidth(14);
        errorStripeGutter.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent transparent #2B2D30;");

        HBox contentRow = new HBox();
        HBox.setHgrow(codeLinesBox, Priority.ALWAYS);
        contentRow.getChildren().addAll(codeLinesBox, errorStripeGutter);

        editorStack.getChildren().addAll(columnGuidePane, contentRow);

        ScrollPane scroll = new ScrollPane(editorStack);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-padding: 0;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        section.getChildren().add(scroll);
        return section;
    }

    private void setupListeners() {
        headerBar.setOnSchemeChanged(scheme -> {
            loadAttributesForSelectedKey();
            updatePreview();
            notifyModified();
        });

        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && selected.getValue() != null) {
                // If top level category group with children is selected (e.g. Code in media_1790343268397.png)
                if (selected.getParent() != null && selected.getParent().getValue().equals("Root")) {
                    selectedKey = selected.getValue();
                    attributeEditorBox.setVisible(false);
                    attributeEditorBox.setManaged(false);
                    recordNavigation(selectedKey);
                    return;
                }

                attributeEditorBox.setVisible(true);
                attributeEditorBox.setManaged(true);

                // Build path: e.g. Editor // Sticky Lines // Border or Code // Identifier under caret
                TreeItem<String> curr = selected;
                List<String> path = new ArrayList<>();
                while (curr != null && curr.getValue() != null && !curr.getValue().equals("Root")) {
                    path.add(0, curr.getValue());
                    curr = curr.getParent();
                }
                selectedKey = String.join(" // ", path);
                recordNavigation(selectedKey);
                loadAttributesForSelectedKey();
            }
        });

        boldCheck.selectedProperty().addListener((obs, o, n) -> handleAttributeChanged());
        italicCheck.selectedProperty().addListener((obs, o, n) -> handleAttributeChanged());

        foregroundCheck.selectedProperty().addListener((obs, o, n) -> {
            if (n && foregroundHex == null) foregroundHex = "DFE1E5";
            updateSwatchButton(foregroundSwatch, foregroundHex, n);
            handleAttributeChanged();
        });

        backgroundCheck.selectedProperty().addListener((obs, o, n) -> {
            if (n && backgroundHex == null) backgroundHex = "373B39";
            updateSwatchButton(backgroundSwatch, backgroundHex, n);
            handleAttributeChanged();
        });

        errorStripeCheck.selectedProperty().addListener((obs, o, n) -> {
            if (n && errorStripeHex == null) errorStripeHex = "5B786A";
            updateSwatchButton(errorStripeSwatch, errorStripeHex, n);
            handleAttributeChanged();
        });

        effectsCheck.selectedProperty().addListener((obs, o, n) -> {
            if (n && effectsHex == null) effectsHex = "589DF6";
            updateSwatchButton(effectsSwatch, effectsHex, n);
            effectTypeCombo.setDisable(!n);
            handleAttributeChanged();
        });

        effectTypeCombo.valueProperty().addListener((obs, o, n) -> handleAttributeChanged());
    }

    private void handleInheritToggled(boolean inherit) {
        if (suppressEvents) return;
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        String active = s.getActiveSchemeName();
        ColorAttribute attr = s.getAttribute(active, selectedKey);
        if (attr == null) attr = new ColorAttribute();
        attr.setInherit(inherit);
        s.setAttribute(active, selectedKey, attr);
        loadAttributesForSelectedKey();
        updatePreview();
        notifyModified();
    }

    private void handleAttributeChanged() {
        if (suppressEvents) return;

        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        String active = s.getActiveSchemeName();

        ColorAttribute attr = s.getAttribute(active, selectedKey);
        if (attr == null) attr = new ColorAttribute();
        attr.setInherit(inheritCheck.isSelected());

        if (foregroundCheck.isSelected() && foregroundHex != null) {
            attr.setForeground("#" + (foregroundHex.startsWith("#") ? foregroundHex.substring(1) : foregroundHex));
        } else {
            attr.setForeground(null);
        }

        if (backgroundCheck.isSelected() && backgroundHex != null) {
            attr.setBackground("#" + (backgroundHex.startsWith("#") ? backgroundHex.substring(1) : backgroundHex));
        } else {
            attr.setBackground(null);
        }

        if (errorStripeCheck.isSelected() && errorStripeHex != null) {
            attr.setErrorStripeColor("#" + (errorStripeHex.startsWith("#") ? errorStripeHex.substring(1) : errorStripeHex));
        } else {
            attr.setErrorStripeColor(null);
        }

        if (effectsCheck.isSelected()) {
            attr.setEffectType(effectTypeCombo.getValue());
            if (effectsHex != null) {
                attr.setEffectColor("#" + (effectsHex.startsWith("#") ? effectsHex.substring(1) : effectsHex));
            }
        } else {
            attr.setEffectType(EffectType.NONE);
        }

        attr.setBold(boldCheck.isSelected());
        attr.setItalic(italicCheck.isSelected());

        s.setAttribute(active, selectedKey, attr);
        updatePreview();
        notifyModified();
    }

    private void loadAttributesForSelectedKey() {
        suppressEvents = true;
        try {
            EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
            ColorAttribute raw = s.getAttribute(s.getActiveSchemeName(), selectedKey);
            if (raw == null) raw = new ColorAttribute();

            // Inheritance setup matching media_1790343485724.png & media_1790343496020.png
            if (raw.getInheritFrom() != null && !raw.getInheritFrom().isBlank()) {
                inheritBox.setVisible(true);
                inheritBox.setManaged(true);
                inheritCheck.setSelected(raw.isInherit());
                currentInheritedTargetKey = raw.getInheritFrom();
                inheritLink.setText(currentInheritedTargetKey.replace(" // ", "->"));
            } else {
                inheritBox.setVisible(false);
                inheritBox.setManaged(false);
                currentInheritedTargetKey = null;
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
                effectTypeCombo.setValue(EffectType.UNDERSCORED);
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

    public void navigateBack() {
        if (historyIndex > 0) {
            historyIndex--;
            isNavigatingHistory = true;
            try {
                selectTreeItem(navigationHistory.get(historyIndex));
            } finally {
                isNavigatingHistory = false;
            }
        }
    }

    public void navigateForward() {
        if (historyIndex < navigationHistory.size() - 1) {
            historyIndex++;
            isNavigatingHistory = true;
            try {
                selectTreeItem(navigationHistory.get(historyIndex));
            } finally {
                isNavigatingHistory = false;
            }
        }
    }

    public void selectTreeItem(String fullKey) {
        String[] parts = fullKey.split(" // ");
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
        selectedKey = fullKey;
        loadAttributesForSelectedKey();
    }

    private void updatePreview() {
        codeLinesBox.getChildren().clear();
        errorStripeGutter.getChildren().clear();

        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        String scheme = s.getActiveSchemeName();

        // Line 0: //TODO: Visit Lumina Web resources: (media_1790344007565.png)
        addLine(
                todoCommentToken("//TODO: Visit Lumina Web resources:", "Code // TODO defaults")
        );

        // Line 1: Lumina Home Page: http://www.lumina.dev
        addLine(
                token("Lumina Home Page: ", null),
                token("http://www.lumina.dev", "Hyperlinks // Reference hyperlink")
        );

        // Line 2: Lumina Developer Community: https://www.lumina.dev/community
        addLine(
                token("Lumina Developer Community: ", null),
                token("https://www.lumina.dev/community", "Hyperlinks // Reference hyperlink")
        );

        // Line 3: ReferenceHyperlink
        addLine(
                token("ReferenceHyperlink", "Hyperlinks // Reference hyperlink")
        );

        // Line 4: Inactive hyperlink in code: "http://lumina.dev"
        addLine(
                token("Inactive hyperlink in code: ", null),
                token("\"http://lumina.dev\"", "Hyperlinks // Inactive hyperlink")
        );

        // Line 5: empty
        addEmptyLine();

        // Line 6: Search:
        addLine(token("Search:", null));

        // Line 7:   result = "text, text, text";
        addLine(
                token("  ", null),
                token("result", "Search Results // Search result (write access)"),
                token(" = \"", null),
                token("text", "Search Results // Search result"),
                token(", ", null),
                token("text", "Search Results // Search result"),
                token(", ", null),
                token("text", "Search Results // Search result"),
                token("\";", null)
        );

        // Line 8:   i = result
        addLine(
                token("  i = ", null),
                token("result", "Code // Identifier under caret")
        );

        // Line 9:   return i;
        addLine(
                token("  return ", null),
                token("i", "Code // Identifier under caret"),
                token(";", null)
        );

        // Line 10: empty
        addEmptyLine();

        // Line 11: Folded text
        addLine(
                foldedBadge("Folded text", "Text // Folded text")
        );

        // Line 12: Folded text with highlighting
        addLine(
                foldedBadge("Folded text with highlighting", "Text // Folded text")
        );

        // Line 13: Deleted text
        addLine(
                deletedToken("Deleted text", "Text // Deleted text")
        );

        // Line 14: Live template: active inactive $VARIABLE$
        addLine(
                token("Live template: ", null),
                borderedToken("active", "Live Templates // Active template", "#385E9D"),
                token(" ", null),
                borderedToken("inactive", "Live Templates // Inactive template", "#5A5D63"),
                token(" ", null),
                variableToken("$VARIABLE$", "#C77DBB")
        );

        // Line 15: Injected language: \.(gif|jpg|png)$
        addLine(
                token("Injected language: ", null),
                injectedFragmentToken("\\.(gif|jpg|png)$", "Code // Injected language fragment")
        );

        // Line 16: empty
        addEmptyLine();

        // Line 17: Code Inspections:
        addLine(token("Code Inspections:", null));

        // Line 18:   Error
        addLine(
                token("  ", null),
                inspectedToken("Error", "Errors and Warnings // Error", "#F75464")
        );

        // Line 19:   Warning
        addLine(
                token("  ", null),
                inspectedToken("Warning", "Errors and Warnings // Warning", "#F2C55C")
        );

        // Line 20:   Weak warning
        addLine(
                token("  ", null),
                inspectedToken("Weak warning", "Errors and Warnings // Weak Warning", "#B9BECF")
        );

        // Line 21:   Deprecated symbol
        addLine(
                token("  ", null),
                strikeToken("Deprecated symbol", "Errors and Warnings // Deprecated symbol", "#8C8C8C")
        );

        // Line 22:   Deprecated symbol marked for removal
        addLine(
                token("  ", null),
                strikeToken("Deprecated symbol marked for removal", "Errors and Warnings // Deprecated symbol marked for removal", "#F75464")
        );

        // Line 23:   Unused symbol
        addLine(
                token("  ", null),
                coloredToken("Unused symbol", "Errors and Warnings // Unused code", "#70727B")
        );

        // Line 24:   Unknown symbol
        addLine(
                token("  ", null),
                coloredToken("Unknown symbol", "Errors and Warnings // Unknown symbol", "#F75464")
        );

        // Line 25:   Runtime problem
        addLine(
                token("  ", null),
                inspectedToken("Runtime problem", "Errors and Warnings // Runtime problem", "#F75464")
        );

        // Line 26:   Problem from server
        addLine(
                token("  ", null),
                borderedToken("Problem from server", "Errors and Warnings // Problem from server", "#C29E4A")
        );

        // Line 27:   Duplicate from server
        addLine(
                token("  ", null),
                badgeToken("Duplicate from server", "Errors and Warnings // Duplicate from server", "#5E5339", "#DFE1E5")
        );

        // Line 28:   typo
        addLine(
                token("  ", null),
                inspectedToken("typo", "Errors and Warnings // Typo", "#4B7258")
        );

        // Line 29:   style_suggestion
        addLine(
                token("  ", null),
                inspectedToken("style_suggestion", "Errors and Warnings // Text style suggestion", "#589DF6")
        );

        // Line 30:   grammar_error
        addLine(
                token("  ", null),
                inspectedToken("grammar_error", "Errors and Warnings // Grammar error", "#713D40")
        );

        // Error stripe markings matching right gutter in screenshots
        addGutterStripe(0, "#57B855");  // Top green line (TODO)
        addGutterStripe(4, "#C77DBB");  // Pink line (Followed/Reference)
        addGutterStripe(7, "#2E5F7E");  // Search cyan line
        addGutterStripe(11, "#8C6C38"); // Folded brown line
        addGutterStripe(12, "#8C6C38"); // Folded brown line
        addGutterStripe(18, "#E5534B"); // Error red line
        addGutterStripe(19, "#C29E4A"); // Warning yellow line
        addGutterStripe(20, "#B9BECF"); // Weak warning line
        addGutterStripe(22, "#E5534B"); // Deprecated symbol marked for removal
        addGutterStripe(24, "#E5534B"); // Unknown symbol
        addGutterStripe(25, "#E5534B"); // Runtime problem
        addGutterStripe(26, "#C29E4A"); // Problem from server
        addGutterStripe(27, "#8C6C38"); // Duplicate from server
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

    private void addEmptyLine() {
        Region empty = new Region();
        empty.setPrefHeight(20);
        empty.setMinHeight(20);
        empty.setMaxHeight(20);
        codeLinesBox.getChildren().add(empty);
    }

    private Node token(String text, String key) {
        Text t = new Text(text);
        t.setFont(Font.font("JetBrains Mono", FontWeight.NORMAL, FontPosture.REGULAR, 12.5));

        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = (key != null) ? s.resolveAttribute(s.getActiveSchemeName(), key) : null;

        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#DFE1E5";
        t.setFill(Color.web(fg));

        if (attr != null) {
            FontWeight weight = attr.isBold() ? FontWeight.BOLD : FontWeight.NORMAL;
            FontPosture posture = attr.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR;
            t.setFont(Font.font("JetBrains Mono", weight, posture, 12.5));

            if (attr.getEffectType() == EffectType.UNDERSCORED || attr.getEffectType() == EffectType.BOLD_UNDERSCORED) {
                t.setUnderline(true);
            }
        }

        StackPane container = new StackPane(t);
        container.setAlignment(Pos.CENTER_LEFT);

        if (attr != null && attr.getBackground() != null) {
            container.setStyle(String.format("-fx-background-color: %s; -fx-padding: 1 3 1 3; -fx-background-radius: 2;", attr.getBackground()));
        }

        if (key != null) {
            container.setStyle(container.getStyle() + "-fx-cursor: hand;");
            container.setOnMouseClicked(e -> selectTreeItem(key));
        }

        return container;
    }

    private Node todoCommentToken(String text, String key) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = s.resolveAttribute(s.getActiveSchemeName(), key);

        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#549159";
        boolean bold = attr != null && attr.isBold();
        boolean italic = attr != null ? attr.isItalic() : true;

        Text t = new Text(text);
        FontWeight weight = bold ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = italic ? FontPosture.ITALIC : FontPosture.REGULAR;
        t.setFont(Font.font("JetBrains Mono", weight, posture, 12.5));
        t.setFill(Color.web(fg));

        HBox box = new HBox(t);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-cursor: hand;");
        box.setOnMouseClicked(e -> selectTreeItem(key));
        return box;
    }

    private Node foldedBadge(String text, String key) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = s.resolveAttribute(s.getActiveSchemeName(), key);

        String bg = (attr != null && attr.getBackground() != null) ? attr.getBackground() : "#393B40";
        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#8C8C8C";

        Text t = new Text(text);
        t.setFont(Font.font("JetBrains Mono", 12.0));
        t.setFill(Color.web(fg));

        HBox badge = new HBox(t);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 3; -fx-padding: 1 6 1 6; -fx-cursor: hand;", bg));
        badge.setOnMouseClicked(e -> selectTreeItem(key));
        return badge;
    }

    private Node deletedToken(String text, String key) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = s.resolveAttribute(s.getActiveSchemeName(), key);

        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#E5534B";
        Text t = new Text(text);
        t.setFont(Font.font("JetBrains Mono", 12.5));
        t.setFill(Color.web(fg));
        t.setStrikethrough(true);

        HBox box = new HBox(t);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-cursor: hand;");
        box.setOnMouseClicked(e -> selectTreeItem(key));
        return box;
    }

    private Node borderedToken(String text, String key, String defaultBorderColor) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = s.resolveAttribute(s.getActiveSchemeName(), key);

        String borderColor = (attr != null && attr.getEffectColor() != null) ? attr.getEffectColor() : defaultBorderColor;
        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#DFE1E5";

        Text t = new Text(text);
        t.setFont(Font.font("JetBrains Mono", 12.0));
        t.setFill(Color.web(fg));

        HBox box = new HBox(t);
        box.setAlignment(Pos.CENTER);
        box.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 1; -fx-border-radius: 2; -fx-padding: 0 4 0 4; -fx-cursor: hand;", borderColor));
        box.setOnMouseClicked(e -> selectTreeItem(key));
        return box;
    }

    private Node variableToken(String text, String fgColor) {
        Text t = new Text(text);
        t.setFont(Font.font("JetBrains Mono", 12.5));
        t.setFill(Color.web(fgColor));
        return new HBox(t);
    }

    private Node injectedFragmentToken(String text, String key) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = s.resolveAttribute(s.getActiveSchemeName(), key);

        String bg = (attr != null && attr.getBackground() != null) ? attr.getBackground() : "#2B3838";
        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#78A389";

        Text t = new Text(text);
        t.setFont(Font.font("JetBrains Mono", 12.5));
        t.setFill(Color.web(fg));

        HBox box = new HBox(t);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle(String.format("-fx-background-color: %s; -fx-padding: 1 4 1 4; -fx-background-radius: 2; -fx-cursor: hand;", bg));
        box.setOnMouseClicked(e -> selectTreeItem(key));
        return box;
    }

    private Node inspectedToken(String text, String key, String defaultColor) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = s.resolveAttribute(s.getActiveSchemeName(), key);

        String waveColor = (attr != null && attr.getEffectColor() != null) ? attr.getEffectColor() : defaultColor;

        Text t = new Text(text);
        t.setFont(Font.font("JetBrains Mono", 12.5));
        t.setFill(Color.web("#BCBEC4"));

        // Approximate wave canvas
        double width = text.length() * 7.5;
        Canvas wave = new Canvas(width, 3);
        GraphicsContext gc = wave.getGraphicsContext2D();
        gc.setStroke(Color.web(waveColor));
        gc.setLineWidth(1.1);
        for (double x = 0; x < width; x += 4) {
            gc.strokeLine(x, 2, x + 2, 0);
            gc.strokeLine(x + 2, 0, x + 4, 2);
        }

        VBox box = new VBox(1, t, wave);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-cursor: hand;");
        box.setOnMouseClicked(e -> selectTreeItem(key));
        return box;
    }

    private Node strikeToken(String text, String key, String defaultStrikeColor) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = s.resolveAttribute(s.getActiveSchemeName(), key);

        String strikeColor = (attr != null && attr.getEffectColor() != null) ? attr.getEffectColor() : defaultStrikeColor;
        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : "#BCBEC4";

        Text t = new Text(text);
        t.setFont(Font.font("JetBrains Mono", 12.5));
        t.setFill(Color.web(fg));
        t.setStrikethrough(true);

        HBox box = new HBox(t);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-cursor: hand;");
        box.setOnMouseClicked(e -> selectTreeItem(key));
        return box;
    }

    private Node coloredToken(String text, String key, String defaultColor) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = s.resolveAttribute(s.getActiveSchemeName(), key);

        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : defaultColor;

        Text t = new Text(text);
        t.setFont(Font.font("JetBrains Mono", 12.5));
        t.setFill(Color.web(fg));

        HBox box = new HBox(t);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-cursor: hand;");
        box.setOnMouseClicked(e -> selectTreeItem(key));
        return box;
    }

    private Node badgeToken(String text, String key, String defaultBg, String defaultFg) {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        ColorAttribute attr = s.resolveAttribute(s.getActiveSchemeName(), key);

        String bg = (attr != null && attr.getBackground() != null) ? attr.getBackground() : defaultBg;
        String fg = (attr != null && attr.getForeground() != null) ? attr.getForeground() : defaultFg;

        Text t = new Text(text);
        t.setFont(Font.font("JetBrains Mono", 12.0));
        t.setFill(Color.web(fg));

        HBox badge = new HBox(t);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 2; -fx-padding: 0 4 0 4; -fx-cursor: hand;", bg));
        badge.setOnMouseClicked(e -> selectTreeItem(key));
        return badge;
    }

    private void addGutterStripe(int lineIndex, String colorHex) {
        Rectangle stripe = new Rectangle(0, lineIndex * 20.0 + 4, 14, 2);
        stripe.setFill(Color.web(colorHex));
        errorStripeGutter.getChildren().add(stripe);
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!suppressEvents && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public boolean isModified() {
        String active = EditorColorSchemeSettings.getInstance().getActiveSchemeName();
        return EditorColorSchemeSettings.getInstance().isSchemeModified(active);
    }

    public void apply() {
        EditorColorSchemeSettings.getInstance().save();
    }

    public void reset() {
        String active = EditorColorSchemeSettings.getInstance().getActiveSchemeName();
        EditorColorSchemeSettings.getInstance().restoreDefaults(active);
        loadAttributesForSelectedKey();
        updatePreview();
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

    public VBox getInheritBox() {
        return inheritBox;
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
