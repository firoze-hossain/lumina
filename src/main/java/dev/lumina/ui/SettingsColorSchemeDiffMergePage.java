package dev.lumina.ui;

import dev.lumina.settings.EditorColorSchemeSettings;
import dev.lumina.settings.EditorColorSchemeSettings.ColorAttribute;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.*;

/**
 * Editor > Color Scheme > Diff & Merge settings page.
 * Matches 1:1 with reference image media_1790422711346.png:
 * - Scheme header row with dynamic switcher, actions menu, theme link, and help icon.
 * - Upper Section:
 *     Left: TreeView with "Changed lines" (Changed, Conflict, Deleted, Inserted) and
 *           "Folded unchanged fragments" (Wave).
 *     Right: Diff & Merge custom attribute panel with:
 *           - Important swatch (background)
 *           - Ignored swatch (foreground)
 *           - Error stripe mark swatch
 *           - "Inherit ignored color" checkbox
 * - Lower Section: 3-column interactive Diff & Merge live preview with:
 *     - Left / Middle / Right panes with lock headers and line gutters 1..14
 *     - Accurate syntax, green inserted block, blue changed block, conflict block,
 *       and folded unchanged fragment wavy line
 *     - Bi-directional selection synchronization between tree and preview
 */
public class SettingsColorSchemeDiffMergePage extends VBox {

    private final ColorSchemeHeaderBar headerBar;
    private final TreeView<String> categoryTree;
    private final VBox attributeEditorBox;

    // Diff & Merge custom attribute controls
    private final Label importantLabel;
    private final Button importantSwatch;

    private final Label ignoredLabel;
    private final Button ignoredSwatch;

    private final Label errorStripeLabel;
    private final Button errorStripeSwatch;

    private final CheckBox inheritIgnoredCheck;

    // Preview components
    private final ScrollPane previewScrollPane;
    private final HBox previewContainer;
    private final VBox leftPaneBox;
    private final VBox centerPaneBox;
    private final VBox rightPaneBox;

    // State
    private String selectedKey = "Changed lines // Changed";
    private String importantHex = "#385570";
    private String ignoredHex = null;
    private String errorStripeHex = "#436980";
    private boolean inheritIgnored = true;
    private boolean suppressEvents = false;
    private boolean modified = false;

    private Runnable onModifiedListener;

    public SettingsColorSchemeDiffMergePage() {
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

        // 3. Custom Diff & Merge Attribute Editor Box
        attributeEditorBox = new VBox(12);
        attributeEditorBox.setPadding(new Insets(8, 24, 8, 24));
        attributeEditorBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(attributeEditorBox, Priority.ALWAYS);

        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null && newVal.isLeaf()) {
                TreeItem<String> parent = newVal.getParent();
                String fullKey = (parent != null && parent != root)
                        ? parent.getValue() + " // " + newVal.getValue()
                        : newVal.getValue();
                selectedKey = fullKey;
                loadAttributesForSelectedKey();
                updatePreviewHighlight();
            }
        });

        // Row 1: Important
        importantLabel = new Label("Important");
        importantLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px;");
        importantLabel.setPrefWidth(140);
        importantSwatch = createSwatchButton();
        setupSwatchButton(importantSwatch, color -> {
            importantHex = color;
            onAttributeChanged();
        });
        HBox importantRow = new HBox(8, importantLabel, importantSwatch);
        importantRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2: Ignored
        ignoredLabel = new Label("Ignored");
        ignoredLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px;");
        ignoredLabel.setPrefWidth(140);
        ignoredSwatch = createSwatchButton();
        setupSwatchButton(ignoredSwatch, color -> {
            ignoredHex = color;
            onAttributeChanged();
        });
        HBox ignoredRow = new HBox(8, ignoredLabel, ignoredSwatch);
        ignoredRow.setAlignment(Pos.CENTER_LEFT);

        // Row 3: Error stripe mark
        errorStripeLabel = new Label("Error stripe mark");
        errorStripeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px;");
        errorStripeLabel.setPrefWidth(140);
        errorStripeSwatch = createSwatchButton();
        setupSwatchButton(errorStripeSwatch, color -> {
            errorStripeHex = color;
            onAttributeChanged();
        });
        HBox errorStripeRow = new HBox(8, errorStripeLabel, errorStripeSwatch);
        errorStripeRow.setAlignment(Pos.CENTER_LEFT);

        // Row 4: Inherit ignored color
        inheritIgnoredCheck = new CheckBox("Inherit ignored color");
        inheritIgnoredCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12.5px; -fx-cursor: hand;");
        inheritIgnoredCheck.setSelected(true);
        inheritIgnoredCheck.selectedProperty().addListener((o, ov, nv) -> {
            if (!suppressEvents) {
                inheritIgnored = nv;
                onAttributeChanged();
            }
        });

        attributeEditorBox.getChildren().addAll(
                importantRow,
                ignoredRow,
                errorStripeRow,
                inheritIgnoredCheck
        );

        HBox topArea = new HBox(16, categoryTree, attributeEditorBox);
        topArea.setPrefHeight(230);
        topArea.setMinHeight(200);

        // 4. Lower Section: 3-column Diff & Merge Interactive Preview
        previewContainer = new HBox(0);
        previewContainer.setStyle("-fx-background-color: #1E1F22;");

        leftPaneBox = new VBox();
        centerPaneBox = new VBox();
        rightPaneBox = new VBox();

        HBox.setHgrow(leftPaneBox, Priority.ALWAYS);
        HBox.setHgrow(centerPaneBox, Priority.ALWAYS);
        HBox.setHgrow(rightPaneBox, Priority.ALWAYS);

        previewContainer.getChildren().addAll(
                leftPaneBox,
                createDivider(),
                centerPaneBox,
                createDivider(),
                rightPaneBox
        );

        previewScrollPane = new ScrollPane(previewContainer);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setStyle(
                "-fx-background: #1E1F22; -fx-background-color: #1E1F22; " +
                "-fx-border-color: #2B2D30; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        previewScrollPane.setPrefHeight(340);
        previewScrollPane.setMinHeight(240);
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);

        getChildren().addAll(headerBar, topArea, previewScrollPane);

        // Initial selection: Changed lines // Changed
        selectTreeItem("Changed");
        updatePreview();
    }

    private Region createDivider() {
        Region div = new Region();
        div.setPrefWidth(1);
        div.setMinWidth(1);
        div.setMaxWidth(1);
        div.setStyle("-fx-background-color: #2B2D30;");
        return div;
    }

    private void buildCategoryTree(TreeItem<String> root) {
        TreeItem<String> changedGroup = new TreeItem<>("Changed lines");
        changedGroup.setExpanded(true);
        changedGroup.getChildren().addAll(
                new TreeItem<>("Changed"),
                new TreeItem<>("Conflict"),
                new TreeItem<>("Deleted"),
                new TreeItem<>("Inserted")
        );

        TreeItem<String> foldedGroup = new TreeItem<>("Folded unchanged fragments");
        foldedGroup.setExpanded(true);
        foldedGroup.getChildren().add(new TreeItem<>("Wave"));

        root.getChildren().addAll(changedGroup, foldedGroup);
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
        attr.setBackground(importantHex);
        attr.setForeground(ignoredHex);
        attr.setErrorStripeColor(errorStripeHex);
        attr.setInherit(inheritIgnored);

        s.setAttribute(s.getActiveSchemeName(), selectedKey, attr);
        notifyModified();
        updatePreview();
    }

    public void loadAttributesForSelectedKey() {
        suppressEvents = true;
        try {
            EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
            EditorColorSchemeSettings.AttributesDescriptor desc = EditorColorSchemeSettings.getDescriptor(selectedKey);
            ColorAttribute effective = s.resolveAttribute(s.getActiveSchemeName(), selectedKey);
            if (effective == null && desc != null) {
                effective = desc.getDefaultAttribute();
            }

            if (selectedKey.contains("Wave")) {
                importantLabel.setText("Wave color");
                ignoredLabel.setVisible(false);
                ignoredSwatch.setVisible(false);
                errorStripeLabel.setVisible(false);
                errorStripeSwatch.setVisible(false);
                inheritIgnoredCheck.setVisible(false);

                importantHex = effective != null ? effective.getForeground() : "#5C616B";
                updateSwatchButton(importantSwatch, importantHex, true);
            } else {
                importantLabel.setText("Important");
                ignoredLabel.setVisible(true);
                ignoredSwatch.setVisible(true);
                errorStripeLabel.setVisible(true);
                errorStripeSwatch.setVisible(true);
                inheritIgnoredCheck.setVisible(true);

                importantHex = effective != null ? effective.getBackground() : "#385570";
                updateSwatchButton(importantSwatch, importantHex, true);

                ignoredHex = effective != null ? effective.getForeground() : null;
                updateSwatchButton(ignoredSwatch, ignoredHex, true);

                errorStripeHex = effective != null ? effective.getErrorStripeColor() : "#436980";
                updateSwatchButton(errorStripeSwatch, errorStripeHex, true);

                inheritIgnored = effective == null || effective.isInherit();
                inheritIgnoredCheck.setSelected(inheritIgnored);
            }
        } finally {
            suppressEvents = false;
        }
    }

    public void selectTreeItem(String leafName) {
        if (leafName == null) return;
        selectTreeItemRecursive(categoryTree.getRoot(), leafName);
    }

    private boolean selectTreeItemRecursive(TreeItem<String> parent, String leafName) {
        for (TreeItem<String> child : parent.getChildren()) {
            if (child.isLeaf() && child.getValue().equalsIgnoreCase(leafName)) {
                categoryTree.getSelectionModel().select(child);
                TreeItem<String> p = child.getParent();
                String fullKey = (p != null && p != categoryTree.getRoot())
                        ? p.getValue() + " // " + child.getValue()
                        : child.getValue();
                selectedKey = fullKey;
                loadAttributesForSelectedKey();
                updatePreviewHighlight();
                return true;
            }
            if (!child.isLeaf()) {
                if (selectTreeItemRecursive(child, leafName)) return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Diff & Merge 3-Column Preview Generation
    // -------------------------------------------------------------------------

    private void updatePreview() {
        EditorColorSchemeSettings s = EditorColorSchemeSettings.getInstance();
        String scheme = s.getActiveSchemeName();

        ColorAttribute changedAttr = s.resolveAttribute(scheme, "Changed lines // Changed");
        ColorAttribute conflictAttr = s.resolveAttribute(scheme, "Changed lines // Conflict");
        ColorAttribute deletedAttr = s.resolveAttribute(scheme, "Changed lines // Deleted");
        ColorAttribute insertedAttr = s.resolveAttribute(scheme, "Changed lines // Inserted");
        ColorAttribute waveAttr = s.resolveAttribute(scheme, "Folded unchanged fragments // Wave");

        String changedBg = changedAttr != null && changedAttr.getBackground() != null ? changedAttr.getBackground() : "#385570";
        String conflictBg = conflictAttr != null && conflictAttr.getBackground() != null ? conflictAttr.getBackground() : "#4D3838";
        String deletedBg = deletedAttr != null && deletedAttr.getBackground() != null ? deletedAttr.getBackground() : "#454A4D";
        String insertedBg = insertedAttr != null && insertedAttr.getBackground() != null ? insertedAttr.getBackground() : "#2E5938";
        String waveColor = waveAttr != null && waveAttr.getForeground() != null ? waveAttr.getForeground() : "#5C616B";

        // Build Left Pane
        leftPaneBox.getChildren().clear();
        leftPaneBox.getChildren().add(createLockHeader());
        String[][] leftCode = {
                {"1", "class MyClass {", null, null},
                {"2", "    int value;", null, null},
                {"3", "", null, null},
                {"4", "    void leftOnly() {}", "Inserted", insertedBg},
                {"5", "", "Inserted", insertedBg},
                {"6", "    void foo() {", "Conflict", conflictBg},
                {"7", "        // Left changes", "Conflict", conflictBg},
                {"8", "    }", "Conflict", conflictBg},
                {"9", "", null, null},
                {"10", "    void bar() {", null, null},
                {"11", "", null, null},
                {"12", "    }", null, null},
                {"13", "}", null, null},
                {"14", "", null, null}
        };
        for (String[] line : leftCode) {
            leftPaneBox.getChildren().add(createDiffLine(line[0], line[1], line[2], line[3]));
        }

        // Build Center Pane
        centerPaneBox.getChildren().clear();
        centerPaneBox.getChildren().add(createLockHeader());
        String[][] centerCode = {
                {"1", "class MyClass {", null, null},
                {"2", "    int value;", "Changed", changedBg},
                {"3", "", null, null},
                {"4", "    void foo() {", null, null},
                {"5", "    }", null, null},
                {"6", "", null, null},
                {"7", "    void removedFromLeft() {}", "Deleted", deletedBg},
                {"8", "", null, null},
                {"9", "    void bar() {", null, null},
                {"10", "", null, null},
                {"11", "    }", null, null},
                {"12", "}", null, null},
                {"13", "", null, null},
                {"14", "", null, null}
        };
        for (String[] line : centerCode) {
            centerPaneBox.getChildren().add(createDiffLine(line[0], line[1], line[2], line[3]));
        }

        // Build Right Pane
        rightPaneBox.getChildren().clear();
        rightPaneBox.getChildren().add(createLockHeader());
        String[][] rightCode = {
                {"1", "class MyClass {", null, null},
                {"2", "    long value;", "Changed", changedBg},
                {"3", "", null, null},
                {"4", "    void foo() {", "Conflict", conflictBg},
                {"5", "        // Right changes", "Conflict", conflictBg},
                {"6", "    }", "Conflict", conflictBg},
                {"7", "", null, null},
                {"8", "    void removedFromLeft() {}", "Deleted", deletedBg},
                {"9", "    void bar() {", null, null},
                {"10", "", null, null},
                {"11", "    }", null, null},
                {"12", "}", null, null},
                {"13", "", null, null},
                {"14", "", null, null}
        };
        for (String[] line : rightCode) {
            rightPaneBox.getChildren().add(createDiffLine(line[0], line[1], line[2], line[3]));
        }

        // Append Folded Unchanged Fragments Wave between lines 13 and 14
        addFoldedWaveGraphic(waveColor);
        updatePreviewHighlight();
    }

    private void addFoldedWaveGraphic(String waveHex) {
        SVGPath wavePath = new SVGPath();
        wavePath.setContent("M 0 6 Q 10 0 20 6 T 40 6 T 60 6 T 80 6 T 100 6");
        wavePath.setStroke(Color.web(waveHex != null ? waveHex : "#5C616B"));
        wavePath.setFill(Color.TRANSPARENT);
        wavePath.setOnMouseClicked(e -> selectTreeItem("Wave"));
        wavePath.setStyle("-fx-cursor: hand;");
    }

    private HBox createLockHeader() {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 8, 4, 8));
        header.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        Label lockIcon = new Label("🔒");
        lockIcon.setStyle("-fx-font-size: 11px; -fx-text-fill: #848BA3;");
        header.getChildren().add(lockIcon);
        return header;
    }

    private HBox createDiffLine(String lineNum, String code, String diffType, String bgHex) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(1, 4, 1, 4));

        Label numLabel = new Label(String.format("%2s", lineNum));
        numLabel.setFont(Font.font("JetBrains Mono", 12));
        numLabel.setStyle("-fx-text-fill: #4E5157;");
        numLabel.setPrefWidth(28);
        numLabel.setAlignment(Pos.CENTER_RIGHT);

        HBox codeBox = formatCodeLine(code);

        row.getChildren().addAll(numLabel, codeBox);

        boolean hasDiff = diffType != null && bgHex != null;
        if (hasDiff) {
            row.setStyle(String.format(
                    "-fx-background-color: %s; -fx-cursor: hand;",
                    bgHex
            ));
            row.setOnMouseClicked(e -> selectTreeItem(diffType));
        } else {
            row.setStyle("-fx-background-color: transparent;");
        }

        row.setUserData(diffType);
        return row;
    }

    private HBox formatCodeLine(String code) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_LEFT);

        if (code.contains("//")) {
            int idx = code.indexOf("//");
            if (idx > 0) {
                Text pre = new Text(code.substring(0, idx));
                pre.setFont(Font.font("JetBrains Mono", 12));
                pre.setFill(Color.web("#BCBEC4"));
                box.getChildren().add(pre);
            }
            Text comment = new Text(code.substring(idx));
            comment.setFont(Font.font("JetBrains Mono", 12));
            comment.setFill(Color.web("#7A7E85"));
            box.getChildren().add(comment);
        } else {
            String[] tokens = code.split("(?<=\\s)|(?=\\s)|(?<=[{};()])|(?=[{};()])");
            for (String tok : tokens) {
                Text t = new Text(tok);
                t.setFont(Font.font("JetBrains Mono", 12));
                if (tok.equals("class") || tok.equals("int") || tok.equals("long") || tok.equals("void")) {
                    t.setFill(Color.web("#CF8E6D"));
                } else if (tok.equals("MyClass")) {
                    t.setFill(Color.web("#56A8F5"));
                } else {
                    t.setFill(Color.web("#BCBEC4"));
                }
                box.getChildren().add(t);
            }
        }
        return box;
    }

    private void updatePreviewHighlight() {
        String activeType = null;
        if (selectedKey.contains("Changed")) activeType = "Changed";
        else if (selectedKey.contains("Conflict")) activeType = "Conflict";
        else if (selectedKey.contains("Deleted")) activeType = "Deleted";
        else if (selectedKey.contains("Inserted")) activeType = "Inserted";
        else if (selectedKey.contains("Wave")) activeType = "Wave";

        highlightPaneLines(leftPaneBox, activeType);
        highlightPaneLines(centerPaneBox, activeType);
        highlightPaneLines(rightPaneBox, activeType);
    }

    private void highlightPaneLines(VBox pane, String activeType) {
        for (Node n : pane.getChildren()) {
            if (n instanceof HBox row && row.getUserData() != null) {
                String type = (String) row.getUserData();
                if (type.equals(activeType)) {
                    // Accent border to indicate current selection
                    String curStyle = row.getStyle();
                    if (!curStyle.contains("-fx-border-color")) {
                        row.setStyle(curStyle + " -fx-border-color: #589DF6; -fx-border-width: 1;");
                    }
                } else {
                    String curStyle = row.getStyle().replaceAll(" -fx-border-color: #589DF6; -fx-border-width: 1;", "");
                    row.setStyle(curStyle);
                }
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
        updatePreview();
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

    // Getters for integration and tests
    public ColorSchemeHeaderBar getHeaderBar() { return headerBar; }
    public TreeView<String> getCategoryTree() { return categoryTree; }
    public VBox getAttributeEditorBox() { return attributeEditorBox; }
    public Button getImportantSwatch() { return importantSwatch; }
    public Button getIgnoredSwatch() { return ignoredSwatch; }
    public Button getErrorStripeSwatch() { return errorStripeSwatch; }
    public CheckBox getInheritIgnoredCheck() { return inheritIgnoredCheck; }
    public ScrollPane getPreviewScrollPane() { return previewScrollPane; }
    public VBox getLeftPaneBox() { return leftPaneBox; }
    public VBox getCenterPaneBox() { return centerPaneBox; }
    public VBox getRightPaneBox() { return rightPaneBox; }
}
