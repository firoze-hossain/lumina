package dev.lumina.ui;

import dev.lumina.scope.NamedScope;
import dev.lumina.scope.ScopeManager;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Window;

/**
 * Scopes settings page.
 * Strictly matches media_1790045064382.png.
 */
public class SettingsScopesPage extends BorderPane {

    private final ScopeManager scopeManager = ScopeManager.getInstance();

    // Left Panel Components
    private final MenuButton addButton = new MenuButton("+");
    private final Button removeButton = new Button("−");
    private final Button copyButton = new Button("📄");
    private final Button saveAsButton = new Button("💾");
    private final Button moveUpButton = new Button("↑");
    private final Button moveDownButton = new Button("↓");

    private final ListView<NamedScope> scopeListView = new ListView<>();
    private final ObservableList<NamedScope> scopeListData = FXCollections.observableArrayList();
    private final Label emptyListPlaceholder = new Label("No scopes added.");

    // Right Panel Components
    private final StackPane rightContainer = new StackPane();
    private final Label emptyDetailPlaceholder = new Label("Select a scope to view or edit its details here");
    private final VBox detailEditorBox = new VBox(12);

    private final TextField nameField = new TextField();
    private final CheckBox sharedThroughVcsCheck = new CheckBox("Share through VCS");
    private final TextField patternField = new TextField();
    private final TreeView<FileNode> fileTreeView = new TreeView<>();

    private NamedScope currentScope = null;

    public SettingsScopesPage() {
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        splitPane.setDividerPositions(0.35);

        // ============================================================
        // 1. Left Panel (Toolbar + Scope List)
        // ============================================================
        VBox leftPanel = new VBox();
        leftPanel.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent #393B40 transparent transparent; -fx-border-width: 0 1 0 0;");
        leftPanel.setMinWidth(220);
        leftPanel.setPrefWidth(280);

        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 12, 8, 12));
        toolbar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent #393B40 transparent; -fx-border-width: 0 0 1 0;");

        // Add Scope Dropdown Button
        addButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");

        MenuItem localScopeItem = new MenuItem("Local");
        localScopeItem.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Label localIcon = new Label("🔘 ");
        localIcon.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 11px;");
        localScopeItem.setGraphic(localIcon);
        localScopeItem.setOnAction(e -> onAddNewScope(false));

        MenuItem sharedScopeItem = new MenuItem("Shared");
        sharedScopeItem.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Label sharedIcon = new Label("🔗 ");
        sharedIcon.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px;");
        sharedScopeItem.setGraphic(sharedIcon);
        sharedScopeItem.setOnAction(e -> onAddNewScope(true));

        addButton.getItems().addAll(localScopeItem, sharedScopeItem);

        styleToolButton(removeButton, "Remove scope");
        removeButton.setOnAction(e -> onRemoveScope());

        styleToolButton(copyButton, "Copy / Duplicate scope");
        copyButton.setOnAction(e -> onCopyScope());

        styleToolButton(saveAsButton, "Save scope as...");
        saveAsButton.setOnAction(e -> onSaveAsScope());

        styleToolButton(moveUpButton, "Move Up");
        moveUpButton.setOnAction(e -> onMoveUp());

        styleToolButton(moveDownButton, "Move Down");
        moveDownButton.setOnAction(e -> onMoveDown());

        toolbar.getChildren().addAll(addButton, removeButton, copyButton, saveAsButton, moveUpButton, moveDownButton);

        // Scope List View
        emptyListPlaceholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        scopeListView.setPlaceholder(emptyListPlaceholder);
        scopeListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-background: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(scopeListView, Priority.ALWAYS);

        scopeListView.setCellFactory(lv -> new ListCell<>() {
            {
                selectedProperty().addListener((obs, oldV, newV) -> {
                    NamedScope item = getItem();
                    if (item != null && !isEmpty()) {
                        String bg = newV ? "#2E436E" : "#1E1F22";
                        String textFill = newV ? "#FFFFFF" : "#DFE1E5";
                        setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textFill + "; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                    } else {
                        setStyle("-fx-background-color: #1E1F22;");
                    }
                });
            }

            @Override
            protected void updateItem(NamedScope item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    String icon = item.isShared() ? "🔗 " : "🔘 ";
                    setText(icon + item.getName());
                    String bg = isSelected() ? "#2E436E" : "#1E1F22";
                    String textFill = isSelected() ? "#FFFFFF" : "#DFE1E5";
                    setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textFill + "; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                }
            }
        });

        scopeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> onSelectScope(newV));

        leftPanel.getChildren().addAll(toolbar, scopeListView);

        // ============================================================
        // 2. Right Panel (Empty Placeholder or Detail Editor)
        // ============================================================
        emptyDetailPlaceholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        buildDetailEditor();

        rightContainer.setStyle("-fx-background-color: #1E1F22;");
        rightContainer.getChildren().setAll(emptyDetailPlaceholder);

        splitPane.getItems().addAll(leftPanel, rightContainer);
        setCenter(splitPane);

        // Load data
        reloadScopesList();
        updateToolbarButtonStates();
    }

    private void buildDetailEditor() {
        detailEditorBox.setPadding(new Insets(14, 20, 16, 20));
        detailEditorBox.setStyle("-fx-background-color: #1E1F22;");

        // Row 1: Name and Shared checkbox
        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label("Name:");
        nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 50px;");

        nameField.setPrefWidth(220);
        nameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        nameField.textProperty().addListener((obs, oldV, newV) -> {
            if (currentScope != null && newV != null) {
                currentScope.setName(newV.trim());
                scopeListView.refresh();
            }
        });

        sharedThroughVcsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        sharedThroughVcsCheck.setOnAction(e -> {
            if (currentScope != null) {
                currentScope.setShared(sharedThroughVcsCheck.isSelected());
                scopeListView.refresh();
            }
        });

        nameRow.getChildren().addAll(nameLbl, nameField, sharedThroughVcsCheck);

        // Row 2: Pattern
        HBox patternRow = new HBox(12);
        patternRow.setAlignment(Pos.CENTER_LEFT);

        Label patternLbl = new Label("Pattern:");
        patternLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 50px;");

        patternField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        HBox.setHgrow(patternField, Priority.ALWAYS);
        patternField.textProperty().addListener((obs, oldV, newV) -> {
            if (currentScope != null && newV != null) {
                currentScope.setPattern(newV.trim());
            }
        });

        patternRow.getChildren().addAll(patternLbl, patternField);

        // Row 3: Include / Exclude Action Buttons Bar
        HBox treeActionBar = new HBox(8);
        treeActionBar.setAlignment(Pos.CENTER_LEFT);
        treeActionBar.setPadding(new Insets(4, 0, 4, 0));

        Button includeBtn = new Button("Include");
        styleActionButton(includeBtn);
        includeBtn.setOnAction(e -> applyInclude(false));

        Button includeRecBtn = new Button("Include Recursively");
        styleActionButton(includeRecBtn);
        includeRecBtn.setOnAction(e -> applyInclude(true));

        Button excludeBtn = new Button("Exclude");
        styleActionButton(excludeBtn);
        excludeBtn.setOnAction(e -> applyExclude(false));

        Button excludeRecBtn = new Button("Exclude Recursively");
        styleActionButton(excludeRecBtn);
        excludeRecBtn.setOnAction(e -> applyExclude(true));

        treeActionBar.getChildren().addAll(includeBtn, includeRecBtn, excludeBtn, excludeRecBtn);

        // Row 4: File Tree View
        fileTreeView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(fileTreeView, Priority.ALWAYS);
        initFileTree();

        detailEditorBox.getChildren().addAll(nameRow, patternRow, treeActionBar, fileTreeView);
    }

    private void initFileTree() {
        File rootDir = new File(".");
        FileNode rootNode = new FileNode(rootDir);
        TreeItem<FileNode> rootItem = buildFileTreeItem(rootNode);
        rootItem.setExpanded(true);
        fileTreeView.setRoot(rootItem);
        fileTreeView.setShowRoot(true);

        fileTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(FileNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    String prefix = item.file.isDirectory() ? "📁 " : "📄 ";
                    setText(prefix + item.file.getName());
                    String bg = isSelected() ? "#2E436E" : "#1E1F22";
                    String textFill = isSelected() ? "#FFFFFF" : "#DFE1E5";
                    setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textFill + "; -fx-font-size: 12px;");
                }
            }
        });
    }

    private TreeItem<FileNode> buildFileTreeItem(FileNode node) {
        TreeItem<FileNode> item = new TreeItem<>(node);
        if (node.file.isDirectory()) {
            File[] children = node.file.listFiles((f) -> !f.getName().startsWith("."));
            if (children != null) {
                for (File child : children) {
                    item.getChildren().add(new TreeItem<>(new FileNode(child)));
                }
            }
        }
        return item;
    }

    private void applyInclude(boolean recursively) {
        TreeItem<FileNode> sel = fileTreeView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getValue() == null) return;
        String rel = sel.getValue().file.getPath().replace('\\', '/');
        if (rel.startsWith("./")) rel = rel.substring(2);
        String pat = "file:*" + rel + (recursively ? "//* " : "* ");
        patternField.setText(patternField.getText() + pat);
    }

    private void applyExclude(boolean recursively) {
        TreeItem<FileNode> sel = fileTreeView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getValue() == null) return;
        String rel = sel.getValue().file.getPath().replace('\\', '/');
        if (rel.startsWith("./")) rel = rel.substring(2);
        String pat = "!file:*" + rel + (recursively ? "//* " : "* ");
        patternField.setText(patternField.getText() + pat);
    }

    private void onSelectScope(NamedScope scope) {
        currentScope = scope;
        if (scope == null) {
            rightContainer.getChildren().setAll(emptyDetailPlaceholder);
        } else {
            nameField.setText(scope.getName());
            sharedThroughVcsCheck.setSelected(scope.isShared());
            patternField.setText(scope.getPattern());
            rightContainer.getChildren().setAll(detailEditorBox);
        }
        updateToolbarButtonStates();
    }

    private void onAddNewScope(boolean shared) {
        TextInputDialog dialog = new TextInputDialog("New Scope");
        dialog.setTitle("Add Scope");
        dialog.setHeaderText(null);
        dialog.setContentText("Scope name:");
        dialog.showAndWait().ifPresent(name -> {
            String clean = name.trim();
            if (!clean.isEmpty()) {
                NamedScope newScope = new NamedScope(clean, "", shared, false);
                scopeManager.addCustomScope(newScope);
                reloadScopesList();
                scopeListView.getSelectionModel().select(newScope);
            }
        });
    }

    private void onRemoveScope() {
        NamedScope sel = scopeListView.getSelectionModel().getSelectedItem();
        if (sel != null) {
            scopeManager.removeCustomScope(sel);
            reloadScopesList();
        }
    }

    private void onCopyScope() {
        NamedScope sel = scopeListView.getSelectionModel().getSelectedItem();
        if (sel != null) {
            NamedScope copy = new NamedScope(sel.getName() + " Copy", sel.getPattern(), sel.isShared(), false);
            scopeManager.addCustomScope(copy);
            reloadScopesList();
            scopeListView.getSelectionModel().select(copy);
        }
    }

    private void onSaveAsScope() {
        NamedScope sel = scopeListView.getSelectionModel().getSelectedItem();
        if (sel != null) {
            TextInputDialog dialog = new TextInputDialog(sel.getName());
            dialog.setTitle("Save Scope As");
            dialog.setHeaderText(null);
            dialog.setContentText("New scope name:");
            dialog.showAndWait().ifPresent(name -> {
                String clean = name.trim();
                if (!clean.isEmpty()) {
                    NamedScope saved = new NamedScope(clean, sel.getPattern(), sel.isShared(), false);
                    scopeManager.addCustomScope(saved);
                    reloadScopesList();
                    scopeListView.getSelectionModel().select(saved);
                }
            });
        }
    }

    private void onMoveUp() {
        int idx = scopeListView.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            scopeManager.moveUp(idx);
            reloadScopesList();
            scopeListView.getSelectionModel().select(idx - 1);
        }
    }

    private void onMoveDown() {
        int idx = scopeListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < scopeListData.size() - 1) {
            scopeManager.moveDown(idx);
            reloadScopesList();
            scopeListView.getSelectionModel().select(idx + 1);
        }
    }

    private void reloadScopesList() {
        scopeListData.setAll(scopeManager.getCustomScopes());
        scopeListView.setItems(scopeListData);
        if (scopeListData.isEmpty()) {
            rightContainer.getChildren().setAll(emptyDetailPlaceholder);
        }
        updateToolbarButtonStates();
    }

    private void updateToolbarButtonStates() {
        int idx = scopeListView.getSelectionModel().getSelectedIndex();
        boolean hasSel = (idx >= 0);
        removeButton.setDisable(!hasSel);
        copyButton.setDisable(!hasSel);
        saveAsButton.setDisable(!hasSel);
        moveUpButton.setDisable(idx <= 0);
        moveDownButton.setDisable(!hasSel || idx >= scopeListData.size() - 1);
    }

    private void styleToolButton(Button btn, String tooltipText) {
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisable()) {
                btn.setStyle("-fx-background-color: #35373C; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisable()) {
                btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
            }
        });
        if (tooltipText != null) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
    }

    private void styleActionButton(Button btn) {
        btn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 4 10 4 10;");
    }

    private static class FileNode {
        final File file;

        FileNode(File file) {
            this.file = file;
        }

        @Override
        public String toString() {
            return file.getName();
        }
    }
}