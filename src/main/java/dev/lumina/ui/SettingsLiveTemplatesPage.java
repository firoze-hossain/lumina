package dev.lumina.ui;

import dev.lumina.livetemplates.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.*;

/**
 * Complete, dynamic IntelliJ IDEA Editor > Live Templates settings page.
 * Faithfully reproduces the layout, typography, controls, and dynamic behavior
 * shown in the IntelliJ screenshots (Images 1 - 5).
 */
public class SettingsLiveTemplatesPage extends VBox {

    private final LiveTemplateManager manager = LiveTemplateManager.getInstance();

    // Baseline & working state
    private String baselineExpandWith;
    private List<LiveTemplateGroup> baselineGroups = new ArrayList<>();
    private final List<LiveTemplateGroup> workingGroups = new ArrayList<>();
    private String workingExpandWith;

    // UI Components
    private final ComboBox<String> defaultExpandCombo = new ComboBox<>();
    private final TreeView<TreeItemData> treeView = new TreeView<>();
    private final StackPane bottomContainer = new StackPane();
    private final Label noSelectionLabel = new Label("No live templates are selected");
    private final VBox editorPanel = new VBox(8);

    // Detail Editor Controls
    private final TextField abbrevField = new TextField();
    private final TextField descField = new TextField();
    private final TextArea templateTextArea = new TextArea();
    private final Button editVariablesBtn = new Button("Edit Variables...");
    private final Label applicableLabel = new Label();
    private final Hyperlink changeContextLink = new Hyperlink("Change ˅");

    // Options Panel Controls
    private final ComboBox<String> expandWithCombo = new ComboBox<>();
    private final CheckBox reformatCheck = new CheckBox("Reformat according to style");
    private final CheckBox staticImportCheck = new CheckBox("Use static import if possible");
    private final CheckBox shortenFQNamesCheck = new CheckBox("Shorten FQ names");
    private final VBox staticImportContainer = new VBox(staticImportCheck);

    // Currently selected template in working tree
    private LiveTemplate currentSelectedTemplate = null;
    private boolean updatingFields = false;

    // Toolbar buttons
    private final Button addBtn = new Button();
    private final Button removeBtn = new Button();
    private final Button duplicateBtn = new Button();
    private final Button revertBtn = new Button();

    private Runnable onModifiedListener;

    public SettingsLiveTemplatesPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(12, 20, 16, 20));
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. By default expand with row (Images 1-5)
        HBox expandRow = buildTopExpandRow();

        // 2. Toolbar above the TreeView (+, —, duplicate, revert)
        HBox toolbar = buildToolbar();

        // 3. TreeView
        buildTreeView();
        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeView.setMinHeight(160);
        treeView.setPrefHeight(230);

        VBox treeContainer = new VBox(toolbar, treeView);
        treeContainer.setStyle("-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: #1E1F22;");
        VBox.setVgrow(treeContainer, Priority.ALWAYS);

        // 4. Bottom Detail Area: "No live templates are selected" OR full template editor
        buildBottomArea();

        getChildren().addAll(expandRow, treeContainer, bottomContainer);

        // Load working copy from manager and populate tree
        reset();
    }

    // ============================================================
    // 1. Top Expand Row
    // ============================================================
    private HBox buildTopExpandRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("By default expand with");
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        defaultExpandCombo.setItems(FXCollections.observableArrayList("Space", "Tab", "Enter", "Custom..."));
        defaultExpandCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-pref-width: 100px;"
        );
        defaultExpandCombo.setOnAction(e -> {
            if (!updatingFields) {
                workingExpandWith = defaultExpandCombo.getValue();
                updateExpandWithDefaultLabel();
                notifyModified();
            }
        });

        row.getChildren().addAll(label, defaultExpandCombo);
        return row;
    }

    private void updateExpandWithDefaultLabel() {
        String def = "Default (" + (workingExpandWith != null ? workingExpandWith : "Tab") + ")";
        ObservableList<String> items = FXCollections.observableArrayList(def, "Space", "Tab", "Enter", "None");
        String current = expandWithCombo.getValue();
        expandWithCombo.setItems(items);
        if (current != null && current.startsWith("Default")) {
            expandWithCombo.setValue(def);
        }
    }

    // ============================================================
    // 2. Action Toolbar (+, —, Copy, Revert)
    // ============================================================
    private HBox buildToolbar() {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 6, 4, 6));
        bar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent #393B40 transparent; -fx-border-width: 0 0 1 0;");

        // + (Add) with ContextMenu
        addBtn.setGraphic(createSvgIcon("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"));
        addBtn.setTooltip(new Tooltip("Add... (Alt+Insert)"));
        styleToolbarBtn(addBtn);

        ContextMenu addMenu = new ContextMenu();
        addMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 6;");

        MenuItem addTemplateItem = new MenuItem("1. Live Template");
        addTemplateItem.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        addTemplateItem.setOnAction(e -> handleAddTemplate());

        MenuItem addGroupItem = new MenuItem("2. Template Group...");
        addGroupItem.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        addGroupItem.setOnAction(e -> handleAddGroup());

        addMenu.getItems().addAll(addTemplateItem, addGroupItem);
        addBtn.setOnAction(e -> addMenu.show(addBtn, javafx.geometry.Side.BOTTOM, 0, 0));

        // — (Remove)
        removeBtn.setGraphic(createSvgIcon("M19 13H5v-2h14v2z"));
        removeBtn.setTooltip(new Tooltip("Remove (Delete)"));
        styleToolbarBtn(removeBtn);
        removeBtn.setOnAction(e -> handleRemoveSelected());

        // Duplicate
        duplicateBtn.setGraphic(createSvgIcon("M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"));
        duplicateBtn.setTooltip(new Tooltip("Duplicate (Ctrl+D)"));
        styleToolbarBtn(duplicateBtn);
        duplicateBtn.setOnAction(e -> handleDuplicateSelected());

        // Revert (Restore)
        revertBtn.setGraphic(createSvgIcon("M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.22 3.16-1.98 5.12-1.98 3.77 0 6.94 2.61 7.75 6.13l2.43-.8C21.6 11.23 17.5 8 12.5 8z"));
        revertBtn.setTooltip(new Tooltip("Restore defaults"));
        styleToolbarBtn(revertBtn);
        revertBtn.setOnAction(e -> handleRevertSelected());

        bar.getChildren().addAll(addBtn, removeBtn, duplicateBtn, revertBtn);
        return bar;
    }

    private void styleToolbarBtn(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 6 3 6; -fx-cursor: hand; -fx-background-radius: 4;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2E3136; -fx-padding: 3 6 3 6; -fx-cursor: hand; -fx-background-radius: 4;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 6 3 6; -fx-cursor: hand; -fx-background-radius: 4;"));
    }

    private SVGPath createSvgIcon(String content) {
        SVGPath path = new SVGPath();
        path.setContent(content);
        path.setFill(Color.web("#B9BECF"));
        path.setScaleX(0.7);
        path.setScaleY(0.7);
        return path;
    }

    // ============================================================
    // 3. Hierarchical CheckBox TreeView
    // ============================================================
    public static class TreeItemData {
        public final boolean isGroup;
        public final boolean isSubgroup;
        public final LiveTemplateGroup group;
        public final String subgroupName;
        public final LiveTemplate template;
        public final BooleanProperty checked = new SimpleBooleanProperty(true);

        public TreeItemData(LiveTemplateGroup group) {
            this.isGroup = true;
            this.isSubgroup = false;
            this.group = group;
            this.subgroupName = null;
            this.template = null;
            this.checked.set(group.isEnabled());
        }

        public TreeItemData(LiveTemplateGroup group, String subgroupName, boolean enabled) {
            this.isGroup = false;
            this.isSubgroup = true;
            this.group = group;
            this.subgroupName = subgroupName;
            this.template = null;
            this.checked.set(enabled);
        }

        public TreeItemData(LiveTemplate template) {
            this.isGroup = false;
            this.isSubgroup = false;
            this.group = null;
            this.subgroupName = null;
            this.template = template;
            this.checked.set(template.isEnabled());
        }
    }

    private void buildTreeView() {
        treeView.setShowRoot(false);
        treeView.setStyle(
                "-fx-background-color: #1E1F22; -fx-border-color: transparent; " +
                "-fx-control-inner-background: #1E1F22; -fx-padding: 2;"
        );

        treeView.setCellFactory(tv -> new TreeCell<>() {
            private final CheckBox checkBox = new CheckBox();
            private final TextFlow textFlow = new TextFlow();
            private final Text primaryText = new Text();
            private final Text secondaryText = new Text();
            private final HBox graphicBox = new HBox(6, checkBox, textFlow);

            {
                graphicBox.setAlignment(Pos.CENTER_LEFT);
                primaryText.setStyle("-fx-fill: #DFE1E5; -fx-font-size: 12px;");
                secondaryText.setStyle("-fx-fill: #868A91; -fx-font-size: 12px;");
                textFlow.getChildren().addAll(primaryText, secondaryText);

                checkBox.setOnAction(e -> {
                    TreeItem<TreeItemData> item = getTreeItem();
                    if (item == null || item.getValue() == null) return;
                    boolean selected = checkBox.isSelected();
                    TreeItemData data = item.getValue();
                    data.checked.set(selected);

                    if (data.isGroup) {
                        data.group.setEnabled(selected);
                        setChildrenChecked(item, selected);
                    } else if (data.isSubgroup) {
                        setChildrenChecked(item, selected);
                        updateParentGroupChecked(item);
                    } else if (data.template != null) {
                        data.template.setEnabled(selected);
                        updateParentGroupChecked(item);
                    }
                    notifyModified();
                });
            }

            @Override
            protected void updateItem(TreeItemData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    checkBox.setSelected(item.checked.get());
                    if (item.isGroup) {
                        primaryText.setText(item.group.getName());
                        primaryText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                        secondaryText.setText("");
                    } else if (item.isSubgroup) {
                        primaryText.setText(item.subgroupName);
                        primaryText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                        secondaryText.setText("");
                    } else if (item.template != null) {
                        primaryText.setText(item.template.getAbbreviation());
                        primaryText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                        String desc = item.template.getDescription();
                        secondaryText.setText(desc.isEmpty() ? "" : " (" + desc + ")");
                    }

                    setGraphic(graphicBox);
                    setText(null);
                }
            }

            private void setChildrenChecked(TreeItem<TreeItemData> parent, boolean val) {
                for (TreeItem<TreeItemData> child : parent.getChildren()) {
                    if (child.getValue() != null) {
                        child.getValue().checked.set(val);
                        if (child.getValue().template != null) {
                            child.getValue().template.setEnabled(val);
                        }
                    }
                    setChildrenChecked(child, val);
                }
            }

            private void updateParentGroupChecked(TreeItem<TreeItemData> child) {
                TreeItem<TreeItemData> parent = child.getParent();
                if (parent == null || parent.getValue() == null) return;
                boolean anyChecked = false;
                for (TreeItem<TreeItemData> sibling : parent.getChildren()) {
                    if (sibling.getValue() != null && sibling.getValue().checked.get()) {
                        anyChecked = true;
                        break;
                    }
                }
                parent.getValue().checked.set(anyChecked);
                if (parent.getValue().isGroup) {
                    parent.getValue().group.setEnabled(anyChecked);
                }
                updateParentGroupChecked(parent);
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && selected.getValue() != null && selected.getValue().template != null) {
                showTemplateEditor(selected.getValue().template);
            } else {
                showNoSelection();
            }
        });
    }

    private void populateTree(String selectAbbrev) {
        TreeItem<TreeItemData> root = new TreeItem<>(new TreeItemData(new LiveTemplateGroup("ROOT")));
        TreeItem<TreeItemData> itemToSelect = null;

        for (LiveTemplateGroup g : workingGroups) {
            TreeItem<TreeItemData> groupItem = new TreeItem<>(new TreeItemData(g));

            // Map subgroups
            Map<String, TreeItem<TreeItemData>> subgroupItems = new LinkedHashMap<>();
            for (LiveTemplate t : g.getTemplates()) {
                String sub = t.getSubgroup();
                if (sub != null && !sub.isBlank()) {
                    TreeItem<TreeItemData> subItem = subgroupItems.computeIfAbsent(sub, s -> {
                        TreeItem<TreeItemData> si = new TreeItem<>(new TreeItemData(g, s, g.isEnabled()));
                        groupItem.getChildren().add(si);
                        return si;
                    });
                    TreeItem<TreeItemData> templateItem = new TreeItem<>(new TreeItemData(t));
                    subItem.getChildren().add(templateItem);

                    if (selectAbbrev != null && selectAbbrev.equals(t.getAbbreviation())) {
                        itemToSelect = templateItem;
                    }
                } else {
                    TreeItem<TreeItemData> templateItem = new TreeItem<>(new TreeItemData(t));
                    groupItem.getChildren().add(templateItem);

                    if (selectAbbrev != null && selectAbbrev.equals(t.getAbbreviation())) {
                        itemToSelect = templateItem;
                    }
                }
            }

            root.getChildren().add(groupItem);
        }

        treeView.setRoot(root);

        // Expand first groups by default (Angular, Groovy, Java, Zen XSL)
        for (TreeItem<TreeItemData> gItem : root.getChildren()) {
            if (gItem.getValue() != null) {
                String name = gItem.getValue().group.getName();
                if ("Java".equals(name) || "Zen XSL".equals(name)) {
                    gItem.setExpanded(true);
                    for (TreeItem<TreeItemData> child : gItem.getChildren()) {
                        child.setExpanded(true);
                    }
                }
            }
        }

        if (itemToSelect != null) {
            treeView.getSelectionModel().select(itemToSelect);
            treeView.scrollTo(treeView.getRow(itemToSelect));
        } else {
            // Find Zen XSL !!! or Java main to highlight initially if available
            TreeItem<TreeItemData> defaultSelect = findTreeItemByAbbreviation(root, "!!!");
            if (defaultSelect == null) {
                defaultSelect = findTreeItemByAbbreviation(root, "main");
            }
            if (defaultSelect != null) {
                treeView.getSelectionModel().select(defaultSelect);
                treeView.scrollTo(treeView.getRow(defaultSelect));
            } else {
                showNoSelection();
            }
        }
    }

    private TreeItem<TreeItemData> findTreeItemByAbbreviation(TreeItem<TreeItemData> root, String abbrev) {
        for (TreeItem<TreeItemData> child : root.getChildren()) {
            if (child.getValue() != null && child.getValue().template != null
                    && abbrev.equals(child.getValue().template.getAbbreviation())) {
                return child;
            }
            TreeItem<TreeItemData> nested = findTreeItemByAbbreviation(child, abbrev);
            if (nested != null) return nested;
        }
        return null;
    }

    // ============================================================
    // 4. Bottom Detail Area (Images 1-5)
    // ============================================================
    private void buildBottomArea() {
        bottomContainer.setMinHeight(240);
        bottomContainer.setPrefHeight(270);
        VBox.setVgrow(bottomContainer, Priority.ALWAYS);

        // Placeholder for no selection (Images 1, 2, 3)
        noSelectionLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px; -fx-alignment: center;");
        StackPane.setAlignment(noSelectionLabel, Pos.CENTER);

        // Editor panel for selected template (Images 4, 5)
        editorPanel.setSpacing(10);
        editorPanel.setStyle("-fx-background-color: #1E1F22;");

        // Row 1: Abbreviation & Description
        HBox row1 = new HBox(16);
        row1.setAlignment(Pos.CENTER_LEFT);

        HBox abbrevBox = new HBox(8);
        abbrevBox.setAlignment(Pos.CENTER_LEFT);
        Label abbrevLbl = new Label("Abbreviation:");
        abbrevLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        abbrevField.setPrefWidth(140);
        styleTextField(abbrevField);
        abbrevField.textProperty().addListener((obs, old, val) -> {
            if (!updatingFields && currentSelectedTemplate != null) {
                currentSelectedTemplate.setAbbreviation(val);
                treeView.refresh();
                notifyModified();
            }
        });
        abbrevBox.getChildren().addAll(abbrevLbl, abbrevField);

        HBox descBox = new HBox(8);
        descBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(descBox, Priority.ALWAYS);
        Label descLbl = new Label("Description:");
        descLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        styleTextField(descField);
        HBox.setHgrow(descField, Priority.ALWAYS);
        descField.textProperty().addListener((obs, old, val) -> {
            if (!updatingFields && currentSelectedTemplate != null) {
                currentSelectedTemplate.setDescription(val);
                treeView.refresh();
                notifyModified();
            }
        });
        descBox.getChildren().addAll(descLbl, descField);

        row1.getChildren().addAll(abbrevBox, descBox);

        // Row 2: "Template text:" label + "Edit Variables..." button
        HBox row2 = new HBox();
        row2.setAlignment(Pos.CENTER_LEFT);

        Label templateTextLbl = new Label("Template text:");
        templateTextLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        Region r2Spacer = new Region();
        HBox.setHgrow(r2Spacer, Priority.ALWAYS);

        editVariablesBtn.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 12 4 12; -fx-cursor: hand;"
        );
        editVariablesBtn.setOnAction(e -> {
            if (currentSelectedTemplate != null) {
                boolean changed = EditVariablesDialog.show(getScene().getWindow(), currentSelectedTemplate);
                if (changed) {
                    notifyModified();
                }
            }
        });

        row2.getChildren().addAll(templateTextLbl, r2Spacer, editVariablesBtn);

        // Row 3: Editor area (Left) + Options Box (Right)
        HBox row3 = new HBox(16);
        row3.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(row3, Priority.ALWAYS);

        // Left: Code Editor TextArea & Context footer
        VBox leftEditor = new VBox(6);
        HBox.setHgrow(leftEditor, Priority.ALWAYS);
        VBox.setVgrow(leftEditor, Priority.ALWAYS);

        templateTextArea.setStyle(
                "-fx-control-inner-background: #14161E; -fx-background-color: #14161E; -fx-border-color: #393B40; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; " +
                "-fx-font-family: 'JetBrains Mono', 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;"
        );
        templateTextArea.setWrapText(false);
        templateTextArea.setPrefHeight(130);
        VBox.setVgrow(templateTextArea, Priority.ALWAYS);

        templateTextArea.textProperty().addListener((obs, old, val) -> {
            if (!updatingFields && currentSelectedTemplate != null) {
                currentSelectedTemplate.setTemplateText(val);
                LiveTemplateManager.extractVariables(currentSelectedTemplate);
                notifyModified();
            }
        });

        // Context footer: "Applicable in XML: XSL Text." + "Change ˅"
        HBox contextFooter = new HBox(6);
        contextFooter.setAlignment(Pos.CENTER_LEFT);

        applicableLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");

        changeContextLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;");
        changeContextLink.setOnAction(e -> {
            if (currentSelectedTemplate != null) {
                ContextSelectionPopup.show(changeContextLink, currentSelectedTemplate, newLabel -> {
                    applicableLabel.setText(newLabel);
                    notifyModified();
                });
            }
        });

        contextFooter.getChildren().addAll(applicableLabel, changeContextLink);
        leftEditor.getChildren().addAll(templateTextArea, contextFooter);

        // Right: Options panel (Images 4, 5)
        VBox optionsBox = buildOptionsPanel();
        optionsBox.setMinWidth(210);
        optionsBox.setPrefWidth(220);

        row3.getChildren().addAll(leftEditor, optionsBox);

        editorPanel.getChildren().addAll(row1, row2, row3);
        bottomContainer.getChildren().setAll(noSelectionLabel);
    }

    private void styleTextField(TextField tf) {
        tf.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8 4 8;"
        );
    }

    private VBox buildOptionsPanel() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.TOP_LEFT);

        // Titled separator: Options ────
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Options");
        title.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");
        Separator sep = new Separator();
        HBox.setHgrow(sep, Priority.ALWAYS);
        sep.setStyle("-fx-background-color: #393B40;");
        titleRow.getChildren().addAll(title, sep);

        // Expand with
        HBox expandBox = new HBox(8);
        expandBox.setAlignment(Pos.CENTER_LEFT);
        Label expandLbl = new Label("Expand with");
        expandLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        expandWithCombo.setStyle(
                "-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-pref-width: 120px;"
        );
        expandWithCombo.setOnAction(e -> {
            if (!updatingFields && currentSelectedTemplate != null) {
                currentSelectedTemplate.setExpandWith(expandWithCombo.getValue());
                notifyModified();
            }
        });
        expandBox.getChildren().addAll(expandLbl, expandWithCombo);

        // Checkboxes
        styleCheckBox(reformatCheck);
        reformatCheck.setOnAction(e -> {
            if (!updatingFields && currentSelectedTemplate != null) {
                currentSelectedTemplate.setReformat(reformatCheck.isSelected());
                notifyModified();
            }
        });

        styleCheckBox(staticImportCheck);
        staticImportCheck.setOnAction(e -> {
            if (!updatingFields && currentSelectedTemplate != null) {
                currentSelectedTemplate.setUseStaticImport(staticImportCheck.isSelected());
                notifyModified();
            }
        });

        styleCheckBox(shortenFQNamesCheck);
        shortenFQNamesCheck.setOnAction(e -> {
            if (!updatingFields && currentSelectedTemplate != null) {
                currentSelectedTemplate.setShortenFQNames(shortenFQNamesCheck.isSelected());
                notifyModified();
            }
        });

        box.getChildren().addAll(titleRow, expandBox, reformatCheck, staticImportContainer, shortenFQNamesCheck);
        return box;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    // ============================================================
    // Selection Display Handler
    // ============================================================
    private void showTemplateEditor(LiveTemplate template) {
        this.currentSelectedTemplate = template;
        updatingFields = true;
        try {
            abbrevField.setText(template.getAbbreviation());
            descField.setText(template.getDescription());
            templateTextArea.setText(template.getTemplateText());

            updateExpandWithDefaultLabel();
            expandWithCombo.setValue(template.getExpandWith());

            reformatCheck.setSelected(template.isReformat());
            staticImportCheck.setSelected(template.isUseStaticImport());
            shortenFQNamesCheck.setSelected(template.isShortenFQNames());

            // "Use static import if possible" only visible if Java template (Image 5 vs Image 4)
            boolean isJava = "Java".equalsIgnoreCase(template.getGroupId())
                    || template.getContexts().contains(LiveTemplateContext.JAVA)
                    || template.getContexts().contains(LiveTemplateContext.JAVA_DECLARATION)
                    || template.getContexts().contains(LiveTemplateContext.JAVA_STATEMENT);
            staticImportContainer.setVisible(isJava);
            staticImportContainer.setManaged(isJava);

            String contextText = LiveTemplateContext.formatApplicableText(template.getContexts());
            applicableLabel.setText(contextText);

            bottomContainer.getChildren().setAll(editorPanel);
        } finally {
            updatingFields = false;
        }
    }

    private void showNoSelection() {
        this.currentSelectedTemplate = null;
        bottomContainer.getChildren().setAll(noSelectionLabel);
    }

    // ============================================================
    // Toolbar Actions
    // ============================================================
    private void handleAddTemplate() {
        TreeItem<TreeItemData> selected = treeView.getSelectionModel().getSelectedItem();
        String groupName = "Java";
        if (selected != null && selected.getValue() != null) {
            if (selected.getValue().isGroup) {
                groupName = selected.getValue().group.getName();
            } else if (selected.getValue().template != null) {
                groupName = selected.getValue().template.getGroupId();
            }
        }

        LiveTemplate newT = new LiveTemplate("new_template", "New live template", "$END$");
        newT.setGroupId(groupName);
        newT.setBuiltin(false);

        LiveTemplateGroup g = findWorkingGroup(groupName);
        if (g != null) {
            g.getTemplates().add(newT);
            populateTree(newT.getAbbreviation());
            notifyModified();
            abbrevField.requestFocus();
            abbrevField.selectAll();
        }
    }

    private void handleAddGroup() {
        TextInputDialog dialog = new TextInputDialog("user");
        dialog.setTitle("New Template Group");
        dialog.setHeaderText("Enter template group name:");
        dialog.initOwner(getScene().getWindow());
        Optional<String> res = dialog.showAndWait();
        if (res.isPresent() && !res.get().isBlank()) {
            String name = res.get().trim();
            if (findWorkingGroup(name) == null) {
                LiveTemplateGroup newGroup = new LiveTemplateGroup(name, false);
                workingGroups.add(newGroup);
                populateTree(null);
                notifyModified();
            }
        }
    }

    private void handleRemoveSelected() {
        TreeItem<TreeItemData> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;

        TreeItemData data = selected.getValue();
        if (data.isGroup) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete group '" + data.group.getName() + "' and all its templates?", ButtonType.YES, ButtonType.NO);
            alert.initOwner(getScene().getWindow());
            alert.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    workingGroups.remove(data.group);
                    populateTree(null);
                    notifyModified();
                }
            });
        } else if (data.template != null) {
            LiveTemplate t = data.template;
            LiveTemplateGroup g = findWorkingGroup(t.getGroupId());
            if (g != null) {
                g.getTemplates().remove(t);
                populateTree(null);
                notifyModified();
            }
        }
    }

    private void handleDuplicateSelected() {
        if (currentSelectedTemplate == null) return;
        LiveTemplate copy = currentSelectedTemplate.copy();
        copy.setId(UUID.randomUUID().toString());
        copy.setBuiltin(false);
        copy.setAbbreviation(currentSelectedTemplate.getAbbreviation() + "_copy");
        copy.setDescription(currentSelectedTemplate.getDescription() + " (Copy)");

        LiveTemplateGroup g = findWorkingGroup(currentSelectedTemplate.getGroupId());
        if (g != null) {
            int idx = g.getTemplates().indexOf(currentSelectedTemplate);
            if (idx >= 0) {
                g.getTemplates().add(idx + 1, copy);
            } else {
                g.getTemplates().add(copy);
            }
            populateTree(copy.getAbbreviation());
            notifyModified();
        }
    }

    private void handleRevertSelected() {
        TreeItem<TreeItemData> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;

        TreeItemData data = selected.getValue();
        if (data.isGroup) {
            manager.revertGroup(data.group);
            reset();
            notifyModified();
        } else if (data.template != null) {
            manager.revertTemplate(data.template);
            reset();
            notifyModified();
        }
    }

    private LiveTemplateGroup findWorkingGroup(String name) {
        if (name == null) return null;
        for (LiveTemplateGroup g : workingGroups) {
            if (name.equalsIgnoreCase(g.getName())) return g;
        }
        return null;
    }

    // ============================================================
    // Settings Lifecycle (apply, reset, isModified)
    // ============================================================
    public void reset() {
        updatingFields = true;
        try {
            this.baselineExpandWith = manager.getDefaultExpandWith();
            this.workingExpandWith = baselineExpandWith;
            this.defaultExpandCombo.setValue(workingExpandWith);

            this.baselineGroups = manager.copyGroups();
            this.workingGroups.clear();
            for (LiveTemplateGroup g : baselineGroups) {
                this.workingGroups.add(g.copy());
            }

            populateTree(null);
        } finally {
            updatingFields = false;
        }
    }

    public void apply() {
        manager.apply(workingGroups, workingExpandWith);
        this.baselineExpandWith = workingExpandWith;
        this.baselineGroups.clear();
        for (LiveTemplateGroup g : workingGroups) {
            this.baselineGroups.add(g.copy());
        }
        notifyModified();
    }

    public boolean isModified() {
        if (!Objects.equals(baselineExpandWith, workingExpandWith)) {
            return true;
        }
        if (baselineGroups.size() != workingGroups.size()) {
            return true;
        }
        for (int i = 0; i < baselineGroups.size(); i++) {
            if (!baselineGroups.get(i).isEquivalentTo(workingGroups.get(i))) {
                return true;
            }
        }
        return false;
    }

    public void setOnModifiedListener(Runnable onModifiedListener) {
        this.onModifiedListener = onModifiedListener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public LiveTemplate getCurrentSelectedTemplate() {
        return currentSelectedTemplate;
    }

    public TreeView<TreeItemData> getTreeView() {
        return treeView;
    }
}