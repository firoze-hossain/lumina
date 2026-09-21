package dev.lumina.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Window;

/**
 * IntelliJ IDEA-identical Menus and Toolbars customization settings page.
 * Powered dynamically by CustomActionsSchema without hardcoding.
 */
public class SettingsMenusToolbarsPage extends VBox {

    private final CustomActionsSchema schema = CustomActionsSchema.getInstance();
    private final TreeView<CustomActionItem> treeView = new TreeView<>();
    private final MenuButton addButton = new MenuButton("Add...");
    private final Button editButton = new Button("✎");
    private final Button moveUpButton = new Button("↑");
    private final Button moveDownButton = new Button("↓");
    private final Button removeButton = new Button("🗑");
    private final Button restoreButton = new Button("↺");
    private final TextField searchField = new TextField();

    public SettingsMenusToolbarsPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(10, 20, 14, 20));
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        // ============================================================
        // 1. Top Action Toolbar
        // ============================================================
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 0, 4, 0));

        // Add... dropdown button
        addButton.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 3 8 3 8;");
        MenuItem addActionItem = new MenuItem("Add Action...");
        addActionItem.setOnAction(e -> onAddAction());
        MenuItem addSeparatorItem = new MenuItem("Add Separator");
        addSeparatorItem.setOnAction(e -> onAddSeparator());
        addButton.getItems().addAll(addActionItem, addSeparatorItem);

        // Edit button
        styleToolButton(editButton, "Edit selected item");
        editButton.setOnAction(e -> onEditAction());

        // Move Up button
        styleToolButton(moveUpButton, "Move Up");
        moveUpButton.setOnAction(e -> onMoveUp());

        // Move Down button
        styleToolButton(moveDownButton, "Move Down");
        moveDownButton.setOnAction(e -> onMoveDown());

        // Remove button
        styleToolButton(removeButton, "Remove");
        removeButton.setOnAction(e -> onRemove());

        // Restore button
        styleToolButton(restoreButton, "Restore default");
        restoreButton.setOnAction(e -> onRestore());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search Field on Right with icon container
        HBox searchContainer = new HBox(6);
        searchContainer.setAlignment(Pos.CENTER_LEFT);
        searchContainer.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 8 3 8;");
        searchContainer.setPrefWidth(210);

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");

        searchField.setPromptText("Filter tree");
        searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-font-size: 12px; -fx-padding: 0 4 0 4; -fx-background-insets: 0;");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, oldV, newV) -> filterTree(newV));

        searchContainer.getChildren().addAll(searchIcon, searchField);
        toolbar.getChildren().addAll(addButton, editButton, moveUpButton, moveDownButton, removeButton, restoreButton, spacer, searchContainer);

        // ============================================================
        // 2. Tree View
        // ============================================================
        treeView.setShowRoot(false);
        treeView.getStyleClass().add("menus-toolbars-tree");
        treeView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeView.setMaxHeight(Double.MAX_VALUE);

        treeView.setCellFactory(tv -> new TreeCell<>() {
            {
                selectedProperty().addListener((obs, oldV, newV) -> {
                    CustomActionItem item = getItem();
                    if (item != null && !isEmpty()) {
                        updateCellStyle(this, item, newV);
                    } else {
                        setStyle("-fx-background-color: #1E1F22;");
                    }
                });
            }

            @Override
            protected void updateItem(CustomActionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    if (item.isSeparator()) {
                        setText("--------------------");
                        setGraphic(null);
                    } else {
                        String icon = item.getIconGlyph();
                        if (icon != null && !icon.isEmpty()) {
                            setText(icon + "  " + item.getText());
                        } else {
                            setText(item.getText());
                        }
                    }
                    updateCellStyle(this, item, isSelected());
                }
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateButtonStates());

        getChildren().addAll(toolbar, treeView);

        // Populate tree
        reloadTree();
        updateButtonStates();
    }

    private void updateCellStyle(TreeCell<CustomActionItem> cell, CustomActionItem item, boolean selected) {
        if (item.isSeparator()) {
            cell.setStyle("-fx-background-color: " + (selected ? "#2E436E;" : "#1E1F22;") +
                    " -fx-text-fill: #5E626B; -fx-font-size: 11px;");
        } else {
            String textFill = selected ? "#FFFFFF" : (item.isGroup() ? "#DFE1E5" : "#BCBEC4");
            String bg = selected ? "#2E436E" : "#1E1F22";
            cell.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textFill + "; -fx-font-size: 12px;");
        }
    }

    private void styleToolButton(Button btn, String tooltipText) {
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 6 3 6;");
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisable()) {
                btn.setStyle("-fx-background-color: #35373C; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 6 3 6;");
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisable()) {
                btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 6 3 6;");
            }
        });
        if (tooltipText != null) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
    }

    private void updateButtonStates() {
        TreeItem<CustomActionItem> sel = treeView.getSelectionModel().getSelectedItem();
        boolean hasSel = (sel != null && sel.getValue() != null);
        boolean isRootLevel = (sel != null && sel.getParent() == treeView.getRoot());

        editButton.setDisable(!hasSel || sel.getValue().isSeparator());
        removeButton.setDisable(!hasSel || isRootLevel);
        restoreButton.setDisable(!hasSel);

        if (hasSel && sel.getParent() != null) {
            int idx = sel.getParent().getChildren().indexOf(sel);
            int total = sel.getParent().getChildren().size();
            moveUpButton.setDisable(idx <= 0);
            moveDownButton.setDisable(idx >= total - 1);
        } else {
            moveUpButton.setDisable(true);
            moveDownButton.setDisable(true);
        }
    }

    public void reloadTree() {
        CustomActionItem virtualRoot = new CustomActionItem("Root", "Root", "", true, false);
        for (CustomActionItem group : schema.getRootGroups()) {
            virtualRoot.addChild(group);
        }
        TreeItem<CustomActionItem> rootItem = buildTreeItem(virtualRoot);
        treeView.setRoot(rootItem);
        updateButtonStates();
    }

    private TreeItem<CustomActionItem> buildTreeItem(CustomActionItem node) {
        TreeItem<CustomActionItem> item = new TreeItem<>(node);
        item.setExpanded(false);
        for (CustomActionItem child : node.getChildren()) {
            item.getChildren().add(buildTreeItem(child));
        }
        return item;
    }

    // ============================================================
    // Revert Changes
    // ============================================================

    public void revertChanges() {
        schema.revertToDefaults();
        reloadTree();
        schema.save();
    }

    // ============================================================
    // Action Handlers
    // ============================================================

    private void onAddAction() {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        AddActionDialog dialog = new AddActionDialog(owner);
        CustomActionItem newAction = dialog.showAndWait();
        if (newAction != null) {
            insertItem(newAction);
        }
    }

    private void onAddSeparator() {
        CustomActionItem sep = CustomActionItem.separator();
        sep.setCustom(true);
        insertItem(sep);
    }

    private void insertItem(CustomActionItem newItem) {
        TreeItem<CustomActionItem> sel = treeView.getSelectionModel().getSelectedItem();
        TreeItem<CustomActionItem> parentItem;
        int insertIndex;

        if (sel == null) {
            parentItem = treeView.getRoot();
            insertIndex = parentItem.getChildren().size();
        } else if (sel.getValue().isGroup()) {
            parentItem = sel;
            insertIndex = sel.getChildren().size();
            sel.setExpanded(true);
        } else {
            parentItem = sel.getParent();
            insertIndex = parentItem.getChildren().indexOf(sel) + 1;
        }

        TreeItem<CustomActionItem> newTreeItem = buildTreeItem(newItem);
        parentItem.getChildren().add(insertIndex, newTreeItem);
        parentItem.getValue().addChild(insertIndex, newItem);

        treeView.getSelectionModel().select(newTreeItem);
        schema.setModified(true);
        schema.save();
    }

    private void onEditAction() {
        TreeItem<CustomActionItem> sel = treeView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getValue() == null || sel.getValue().isSeparator()) return;

        Window owner = getScene() != null ? getScene().getWindow() : null;
        EditActionDialog dialog = new EditActionDialog(owner, sel.getValue());
        if (dialog.showAndWait(sel.getValue())) {
            treeView.refresh();
            schema.setModified(true);
            schema.save();
        }
    }

    private void onMoveUp() {
        TreeItem<CustomActionItem> sel = treeView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getParent() == null) return;

        TreeItem<CustomActionItem> parent = sel.getParent();
        int idx = parent.getChildren().indexOf(sel);
        if (idx > 0) {
            parent.getChildren().remove(idx);
            parent.getChildren().add(idx - 1, sel);

            parent.getValue().getChildren().remove(idx);
            parent.getValue().getChildren().add(idx - 1, sel.getValue());

            treeView.getSelectionModel().select(sel);
            schema.setModified(true);
            schema.save();
        }
    }

    private void onMoveDown() {
        TreeItem<CustomActionItem> sel = treeView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getParent() == null) return;

        TreeItem<CustomActionItem> parent = sel.getParent();
        int idx = parent.getChildren().indexOf(sel);
        if (idx >= 0 && idx < parent.getChildren().size() - 1) {
            parent.getChildren().remove(idx);
            parent.getChildren().add(idx + 1, sel);

            parent.getValue().getChildren().remove(idx);
            parent.getValue().getChildren().add(idx + 1, sel.getValue());

            treeView.getSelectionModel().select(sel);
            schema.setModified(true);
            schema.save();
        }
    }

    private void onRemove() {
        TreeItem<CustomActionItem> sel = treeView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getParent() == null) return;

        TreeItem<CustomActionItem> parent = sel.getParent();
        parent.getChildren().remove(sel);
        parent.getValue().removeChild(sel.getValue());

        schema.setModified(true);
        schema.save();
        updateButtonStates();
    }

    private void onRestore() {
        TreeItem<CustomActionItem> sel = treeView.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        revertChanges();
    }

    // ============================================================
    // Real-Time Tree Filter
    // ============================================================

    private void filterTree(String query) {
        if (query == null || query.trim().isEmpty()) {
            reloadTree();
            return;
        }

        String filter = query.trim().toLowerCase();
        CustomActionItem virtualRoot = new CustomActionItem("Root", "Root", "", true, false);
        for (CustomActionItem group : schema.getRootGroups()) {
            CustomActionItem filtered = filterItem(group, filter);
            if (filtered != null) {
                virtualRoot.addChild(filtered);
            }
        }

        TreeItem<CustomActionItem> rootItem = buildTreeItem(virtualRoot);
        expandAll(rootItem);
        treeView.setRoot(rootItem);
    }

    private CustomActionItem filterItem(CustomActionItem node, String filter) {
        boolean matchSelf = node.getText().toLowerCase().contains(filter) ||
                node.getId().toLowerCase().contains(filter);

        List<CustomActionItem> matchingChildren = new ArrayList<>();
        for (CustomActionItem child : node.getChildren()) {
            CustomActionItem filteredChild = filterItem(child, filter);
            if (filteredChild != null) {
                matchingChildren.add(filteredChild);
            }
        }

        if (matchSelf || !matchingChildren.isEmpty()) {
            CustomActionItem copy = new CustomActionItem(node.getId(), node.getText(), node.getIconGlyph(), node.isGroup(), node.isSeparator());
            for (CustomActionItem c : matchingChildren) {
                copy.addChild(c);
            }
            return copy;
        }
        return null;
    }

    private void expandAll(TreeItem<?> item) {
        if (item != null) {
            item.setExpanded(true);
            for (TreeItem<?> child : item.getChildren()) {
                expandAll(child);
            }
        }
    }
}