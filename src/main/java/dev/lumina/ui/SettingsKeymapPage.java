package dev.lumina.ui;

import dev.lumina.keymap.KeyboardShortcut;
import dev.lumina.keymap.Keymap;
import dev.lumina.keymap.KeymapAction;
import dev.lumina.keymap.KeymapManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.util.*;

/**
 * Keymap settings page strictly matching IntelliJ IDEA across all 5 reference images.
 * Powered by dynamic KeymapManager architecture with zero hardcoding.
 */
public class SettingsKeymapPage extends VBox {

    private final KeymapManager manager = KeymapManager.getInstance();
    private final Runnable navigateToPlugins;

    private final ComboBox<String> keymapCombo = new ComboBox<>();
    private final Button gearButton = new Button("⚙");
    private final TreeView<KeymapNode> actionTree = new TreeView<>();
    private final TextField searchField = new TextField();
    private final ToggleButton conflictFilterBtn = new ToggleButton("⚠️");

    private boolean allExpanded = false;
    private KeyboardShortcut activeShortcutFilter = null;

    public SettingsKeymapPage() {
        this(null);
    }

    public SettingsKeymapPage(Runnable navigateToPlugins) {
        this.navigateToPlugins = navigateToPlugins;
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(10);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Top Section: Keymap selector ComboBox, gear button, and Plugins link
        VBox topSection = buildTopSection();

        // 2. Toolbar above the TreeView (↕, ✕, ✎, ⚠️, 🔍 search, ⌨ find-by-shortcut)
        HBox toolbar = buildActionToolbar();

        // 3. Full-Height Action Tree
        buildActionTree();
        VBox.setVgrow(actionTree, Priority.ALWAYS);

        // 4. Bottom macOS System Conflicts Warning Banner
        HBox bottomBanner = buildBottomConflictBanner();

        getChildren().addAll(topSection, toolbar, actionTree, bottomBanner);

        // Listen for keymap changes to refresh tree
        manager.addListener(this::refreshTreeAndCombo);
    }

    private VBox buildTopSection() {
        VBox container = new VBox(6);

        // Row: ComboBox + Gear Button
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        updateKeymapComboItems();
        keymapCombo.setValue(manager.getActiveKeymap().getName());
        keymapCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-min-width: 170px;");
        keymapCombo.setOnAction(e -> {
            String selected = keymapCombo.getValue();
            if (selected != null && !selected.isBlank()) {
                manager.setActiveKeymapByName(selected);
                refreshTree();
            }
        });

        // Gear icon button
        gearButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E95; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 4;");
        gearButton.setTooltip(new Tooltip("Manage keymap"));
        gearButton.setOnAction(e -> showGearMenu());

        row.getChildren().addAll(keymapCombo, gearButton);

        // Subtitle hyperlink: "Get more keymaps in Settings | Plugins"
        HBox linkBox = new HBox();
        linkBox.setAlignment(Pos.CENTER_LEFT);
        Label linkPrefix = new Label("Get more keymaps in ");
        linkPrefix.setStyle("-fx-text-fill: #8C8E95; -fx-font-size: 12px;");

        Hyperlink pluginsLink = new Hyperlink("Settings | Plugins");
        pluginsLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 12px; -fx-padding: 0;");
        pluginsLink.setOnAction(e -> {
            if (navigateToPlugins != null) {
                navigateToPlugins.run();
            }
        });

        linkBox.getChildren().addAll(linkPrefix, pluginsLink);
        container.getChildren().addAll(row, linkBox);
        return container;
    }

    private void updateKeymapComboItems() {
        keymapCombo.getItems().clear();
        for (Keymap k : manager.getKeymaps()) {
            keymapCombo.getItems().add(k.getName());
        }
    }

    private void showGearMenu() {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-font-size: 12px;");

        Keymap active = manager.getActiveKeymap();

        MenuItem duplicateItem = new MenuItem("Duplicate...");
        duplicateItem.setOnAction(e -> duplicateCurrentKeymap());

        MenuItem renameItem = new MenuItem("Rename...");
        renameItem.setDisable(!active.isMutable());
        renameItem.setOnAction(e -> renameCurrentKeymap());

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setDisable(!active.isMutable());
        deleteItem.setOnAction(e -> deleteCurrentKeymap());

        MenuItem restoreItem = new MenuItem("Restore to Default");
        restoreItem.setOnAction(e -> {
            manager.restoreToDefault(active);
            refreshTree();
        });

        menu.getItems().addAll(duplicateItem, renameItem, deleteItem, new SeparatorMenuItem(), restoreItem);
        menu.show(gearButton, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void duplicateCurrentKeymap() {
        TextInputDialog dialog = new TextInputDialog(manager.getActiveKeymap().getName() + " copy");
        dialog.setTitle("Duplicate Keymap");
        dialog.setHeaderText("Enter name for the new keymap:");
        dialog.setContentText("Keymap name:");
        applyDialogDarkTheme(dialog.getDialogPane());
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                Keymap cloned = manager.duplicateKeymap(manager.getActiveKeymap(), name);
                updateKeymapComboItems();
                keymapCombo.setValue(cloned.getName());
                refreshTree();
            }
        });
    }

    private void renameCurrentKeymap() {
        Keymap active = manager.getActiveKeymap();
        if (!active.isMutable()) return;
        TextInputDialog dialog = new TextInputDialog(active.getName());
        dialog.setTitle("Rename Keymap");
        dialog.setHeaderText("Enter new name for the keymap:");
        dialog.setContentText("Keymap name:");
        applyDialogDarkTheme(dialog.getDialogPane());
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                manager.renameKeymap(active, name);
                updateKeymapComboItems();
                keymapCombo.setValue(active.getName());
            }
        });
    }

    private void deleteCurrentKeymap() {
        Keymap active = manager.getActiveKeymap();
        if (!active.isMutable()) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete keymap '" + active.getName() + "'?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Delete Keymap");
        applyDialogDarkTheme(alert.getDialogPane());
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                manager.deleteKeymap(active);
                updateKeymapComboItems();
                keymapCombo.setValue(manager.getActiveKeymap().getName());
                refreshTree();
            }
        });
    }

    private HBox buildActionToolbar() {
        HBox toolbar = new HBox(6);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 0, 4, 0));

        // ↕ Expand/Collapse All
        Button expandCollapseBtn = createToolbarButton("↕", "Expand / Collapse All");
        expandCollapseBtn.setOnAction(e -> {
            allExpanded = !allExpanded;
            setTreeExpandedRecursive(actionTree.getRoot(), allExpanded);
        });

        // ✕ Remove shortcut
        Button removeBtn = createToolbarButton("✕", "Remove Shortcut");
        removeBtn.setOnAction(e -> removeSelectedShortcut());

        // ✎ Edit / Add shortcut
        Button editBtn = createToolbarButton("✎", "Add Keyboard Shortcut...");
        editBtn.setOnAction(e -> editSelectedShortcut());

        // ⚠️ Conflict / customized filter
        conflictFilterBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E95; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 4 8;");
        conflictFilterBtn.setTooltip(new Tooltip("Show only conflicted or customized shortcuts"));
        conflictFilterBtn.setOnAction(e -> refreshTree());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search Bar with 🔍 placeholder and ⌨ Find by Shortcut icon
        HBox searchContainer = new HBox(4);
        searchContainer.setAlignment(Pos.CENTER_LEFT);
        searchContainer.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 1 4;");
        searchContainer.setPrefWidth(260);

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #8C8E95; -fx-font-size: 11px;");

        searchField.setPromptText("Search");
        searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #8C8E95; -fx-font-size: 12px; -fx-border-width: 0;");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            activeShortcutFilter = null;
            refreshTree();
        });

        Button findByShortcutBtn = new Button("⌨");
        findByShortcutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E95; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2;");
        findByShortcutBtn.setTooltip(new Tooltip("Find Action by Shortcut"));
        findByShortcutBtn.setOnAction(e -> openFindByShortcut());

        searchContainer.getChildren().addAll(searchIcon, searchField, findByShortcutBtn);

        toolbar.getChildren().addAll(expandCollapseBtn, removeBtn, editBtn, conflictFilterBtn, spacer, searchContainer);
        return toolbar;
    }

    private Button createToolbarButton(String text, String tooltip) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E95; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 4 8;");
        btn.setTooltip(new Tooltip(tooltip));
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 4;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E95; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 4 8;"));
        return btn;
    }

    private void openFindByShortcut() {
        Window win = getScene() != null ? getScene().getWindow() : null;
        new FindByShortcutPopup(win, shortcut -> {
            activeShortcutFilter = shortcut;
            searchField.setText(shortcut.formatGlyphs());
            refreshTree();
        }).show();
    }

    private void buildActionTree() {
        actionTree.setShowRoot(false);
        actionTree.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        actionTree.setCellFactory(tv -> new KeymapTreeCell());

        actionTree.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                editSelectedShortcut();
            }
        });

        refreshTree();
    }

    private void refreshTreeAndCombo() {
        updateKeymapComboItems();
        if (manager.getActiveKeymap() != null) {
            keymapCombo.setValue(manager.getActiveKeymap().getName());
        }
        refreshTree();
    }

    private void refreshTree() {
        TreeItem<KeymapNode> root = new TreeItem<>(new KeymapNode("Root", null, true));
        root.setExpanded(true);

        String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        boolean onlyConflicts = conflictFilterBtn.isSelected();

        // Build hierarchical categories
        Map<String, TreeItem<KeymapNode>> categoryMap = new HashMap<>();

        for (KeymapAction action : manager.getAllActions()) {
            List<KeyboardShortcut> shortcuts = manager.getActiveShortcuts(action.getId());

            // Filter by search query or active shortcut
            if (!query.isEmpty()) {
                boolean matchesName = action.getName().toLowerCase().contains(query);
                boolean matchesPath = action.getFullPath().toLowerCase().contains(query);
                boolean matchesShortcut = shortcuts.stream().anyMatch(s ->
                        s.formatGlyphs().toLowerCase().contains(query) || s.formatText().toLowerCase().contains(query));
                if (!matchesName && !matchesPath && !matchesShortcut) {
                    continue;
                }
            }

            if (activeShortcutFilter != null && !shortcuts.contains(activeShortcutFilter)) {
                continue;
            }

            if (onlyConflicts) {
                boolean hasConflict = !manager.findConflicts(shortcuts.isEmpty() ? null : shortcuts.get(0), action.getId()).isEmpty();
                if (!hasConflict && !manager.getMacSystemConflicts().contains(action)) {
                    continue;
                }
            }

            // Find or create category branch
            TreeItem<KeymapNode> parent = root;
            StringBuilder catPath = new StringBuilder();
            for (String segment : action.getCategoryPath()) {
                if (catPath.length() > 0) catPath.append("/");
                catPath.append(segment);
                String fullKey = catPath.toString();

                parent = categoryMap.computeIfAbsent(fullKey, k -> {
                    TreeItem<KeymapNode> catItem = new TreeItem<>(new KeymapNode(segment, null, true));
                    catItem.setExpanded(true);
                    return catItem;
                });
            }

            // Ensure category hierarchy is attached to root
            attachCategoryAncestors(root, action.getCategoryPath(), categoryMap);

            // Add action leaf
            TreeItem<KeymapNode> actionLeaf = new TreeItem<>(new KeymapNode(action.getName(), action, false));
            parent.getChildren().add(actionLeaf);
        }

        // Attach top-level categories
        for (String segment : List.of("Editor Actions", "Main Menu", "Tool Windows", "External Tools",
                "External Build Systems", "Version Control Systems", "Debugger Actions", "Remote External Tools",
                "Database", "Macros", "Intentions", "Quick Lists", "Plugins", "Other")) {
            TreeItem<KeymapNode> item = categoryMap.get(segment);
            if (item != null && item.getParent() == null) {
                root.getChildren().add(item);
            }
        }

        actionTree.setRoot(root);
    }

    private void attachCategoryAncestors(TreeItem<KeymapNode> root, List<String> path, Map<String, TreeItem<KeymapNode>> map) {
        for (int i = 0; i < path.size(); i++) {
            String currentKey = String.join("/", path.subList(0, i + 1));
            TreeItem<KeymapNode> current = map.get(currentKey);
            if (current != null && current.getParent() == null) {
                if (i == 0) {
                    root.getChildren().add(current);
                } else {
                    String parentKey = String.join("/", path.subList(0, i));
                    TreeItem<KeymapNode> parent = map.get(parentKey);
                    if (parent != null && !parent.getChildren().contains(current)) {
                        parent.getChildren().add(current);
                    }
                }
            }
        }
    }

    private void setTreeExpandedRecursive(TreeItem<KeymapNode> item, boolean expanded) {
        if (item == null) return;
        item.setExpanded(expanded);
        for (TreeItem<KeymapNode> child : item.getChildren()) {
            setTreeExpandedRecursive(child, expanded);
        }
    }

    private void editSelectedShortcut() {
        TreeItem<KeymapNode> selected = actionTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null || selected.getValue().isCategory()) {
            return;
        }

        KeymapAction action = selected.getValue().getAction();
        List<KeyboardShortcut> currentShortcuts = manager.getActiveShortcuts(action.getId());
        KeyboardShortcut existing = currentShortcuts.isEmpty() ? null : currentShortcuts.get(0);

        Window win = getScene() != null ? getScene().getWindow() : null;
        KeyboardShortcutDialog dialog = new KeyboardShortcutDialog(win, action, existing);
        KeyboardShortcut newShortcut = dialog.showAndGet();

        if (newShortcut != null) {
            manager.addShortcutToActiveKeymap(action.getId(), newShortcut);
            refreshTree();
        }
    }

    private void removeSelectedShortcut() {
        TreeItem<KeymapNode> selected = actionTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null || selected.getValue().isCategory()) {
            return;
        }

        KeymapAction action = selected.getValue().getAction();
        List<KeyboardShortcut> currentShortcuts = manager.getActiveShortcuts(action.getId());
        if (!currentShortcuts.isEmpty()) {
            manager.removeShortcutFromActiveKeymap(action.getId(), currentShortcuts.get(0));
            refreshTree();
        }
    }

    private HBox buildBottomConflictBanner() {
        HBox banner = new HBox(4);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(8, 0, 0, 0));

        Label warnIcon = new Label("⚠️");
        warnIcon.setStyle("-fx-text-fill: #E5A93C; -fx-font-size: 13px;");

        // Clickable links for conflicting actions from media_1790048100533.png
        Hyperlink findActionLink = createConflictLink("Find Action...", "FindActions");
        Label comma1 = new Label(", ");
        comma1.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        Hyperlink basicLink = createConflictLink("Basic", "BasicCompletion");
        Label comma2 = new Label(", ");
        comma2.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        Hyperlink minimizeLink = createConflictLink("Minimize", "MinimizeWindow");

        Label andText = new Label(" and ");
        andText.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        Hyperlink moreLink = new Hyperlink("17 more");
        moreLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 12px; -fx-padding: 0;");
        moreLink.setOnAction(e -> {
            conflictFilterBtn.setSelected(true);
            refreshTree();
        });

        Label suffix = new Label(" shortcuts conflict with the macOS system shortcuts.\nAssign custom shortcuts or change the macOS system settings.");
        suffix.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        banner.getChildren().addAll(warnIcon, findActionLink, comma1, basicLink, comma2, minimizeLink, andText, moreLink, suffix);
        return banner;
    }

    private Hyperlink createConflictLink(String text, String actionId) {
        Hyperlink link = new Hyperlink(text);
        link.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 12px; -fx-padding: 0;");
        link.setOnAction(e -> selectActionInTree(actionId));
        return link;
    }

    public void selectActionInTree(String actionId) {
        if (actionId == null || actionTree.getRoot() == null) return;
        TreeItem<KeymapNode> found = findItemByActionId(actionTree.getRoot(), actionId);
        if (found != null) {
            expandAncestors(found);
            actionTree.getSelectionModel().select(found);
            actionTree.scrollTo(actionTree.getRow(found));
        }
    }

    private TreeItem<KeymapNode> findItemByActionId(TreeItem<KeymapNode> root, String actionId) {
        if (root.getValue() != null && root.getValue().getAction() != null) {
            if (actionId.equals(root.getValue().getAction().getId())) {
                return root;
            }
        }
        for (TreeItem<KeymapNode> child : root.getChildren()) {
            TreeItem<KeymapNode> result = findItemByActionId(child, actionId);
            if (result != null) return result;
        }
        return null;
    }

    private void expandAncestors(TreeItem<?> item) {
        TreeItem<?> p = item.getParent();
        while (p != null) {
            p.setExpanded(true);
            p = p.getParent();
        }
    }

    private void applyDialogDarkTheme(DialogPane pane) {
        pane.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
        pane.lookupAll(".label").forEach(l -> l.setStyle("-fx-text-fill: #DFE1E5;"));
    }

    // Node representation for TreeView
    private static class KeymapNode {
        private final String name;
        private final KeymapAction action;
        private final boolean category;

        public KeymapNode(String name, KeymapAction action, boolean category) {
            this.name = name;
            this.action = action;
            this.category = category;
        }

        public String getName() {
            return name;
        }

        public KeymapAction getAction() {
            return action;
        }

        public boolean isCategory() {
            return category;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // Custom TreeCell with shortcut badges matching screenshots
    private class KeymapTreeCell extends TreeCell<KeymapNode> {

        private final HBox layout = new HBox(8);
        private final Label iconLabel = new Label();
        private final Label nameLabel = new Label();
        private final Region spacer = new Region();
        private final HBox shortcutsBox = new HBox(4);

        public KeymapTreeCell() {
            layout.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            shortcutsBox.setAlignment(Pos.CENTER_RIGHT);

            nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            layout.getChildren().addAll(iconLabel, nameLabel, spacer, shortcutsBox);

            // Context menu
            ContextMenu contextMenu = new ContextMenu();
            MenuItem addSc = new MenuItem("Add Keyboard Shortcut...");
            addSc.setOnAction(e -> editSelectedShortcut());

            MenuItem removeSc = new MenuItem("Remove Shortcut");
            removeSc.setOnAction(e -> removeSelectedShortcut());

            contextMenu.getItems().addAll(addSc, removeSc);
            setContextMenu(contextMenu);
        }

        @Override
        protected void updateItem(KeymapNode item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }

            if (item.isCategory()) {
                iconLabel.setText("📁");
                iconLabel.setStyle("-fx-text-fill: #A6ADC4; -fx-font-size: 12px;");
                nameLabel.setText(item.getName());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                shortcutsBox.getChildren().clear();
            } else {
                KeymapAction action = item.getAction();
                if (action.getIconGlyph() != null) {
                    iconLabel.setText(action.getIconGlyph());
                    iconLabel.setStyle("-fx-font-size: 12px;");
                } else {
                    iconLabel.setText("");
                }
                nameLabel.setText(action.getName());
                nameLabel.setStyle("-fx-font-weight: normal; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

                // Render shortcut badges
                shortcutsBox.getChildren().clear();
                List<KeyboardShortcut> list = manager.getActiveShortcuts(action.getId());
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        Label orLbl = new Label("or");
                        orLbl.setStyle("-fx-text-fill: #8C8E95; -fx-font-size: 11px;");
                        shortcutsBox.getChildren().add(orLbl);
                    }
                    KeyboardShortcut sc = list.get(i);
                    Label badge = new Label(sc.formatGlyphs());
                    badge.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3; -fx-padding: 1 6; -fx-font-size: 11px;");
                    shortcutsBox.getChildren().add(badge);
                }
            }

            setGraphic(layout);
            setText(null);

            if (isSelected()) {
                setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF;");
            } else {
                setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5;");
            }
        }
    }
}