package dev.lumina.ui;

import dev.lumina.quicklist.QuickList;
import dev.lumina.quicklist.QuickListItem;
import dev.lumina.quicklist.QuickListManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Window;

/**
 * Quick Lists settings page.
 * Strictly matches media_1790045975645.png, media_1790045975646.png,
 * media_1790045975647.png, and media_1790045975661.png.
 */
public class SettingsQuickListsPage extends BorderPane {

    private final QuickListManager manager = QuickListManager.getInstance();

    // Left Panel
    private final ListView<QuickList> quickListView = new ListView<>();
    private final ObservableList<QuickList> quickListData = FXCollections.observableArrayList();
    private final Button addListBtn = new Button("+");
    private final Button removeListBtn = new Button("−");

    // Right Panel
    private final StackPane rightContainer = new StackPane();
    private final Label emptyListDetailPlaceholder = new Label("Select a quick list to view or edit details");
    private final VBox detailEditor = new VBox(10);

    private final TextField displayNameField = new TextField();
    private final TextField descriptionField = new TextField();

    // Inner Action Toolbar
    private final Button addActionBtn = new Button("+");
    private final Button addSeparatorBtn = new Button("---");
    private final Button removeActionBtn = new Button("−");
    private final Button moveUpBtn = new Button("↑");
    private final Button moveDownBtn = new Button("↓");

    private final ListView<QuickListItem> actionListView = new ListView<>();
    private final Label noActionsLabel = new Label("No actions");

    private QuickList currentQuickList = null;

    public SettingsQuickListsPage() {
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        splitPane.setDividerPositions(0.24);

        // ============================================================
        // 1. Left Panel (Toolbar + Quick Lists List)
        // ============================================================
        VBox leftPanel = new VBox();
        leftPanel.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent #393B40 transparent transparent; -fx-border-width: 0 1 0 0;");
        leftPanel.setMinWidth(180);
        leftPanel.setPrefWidth(220);

        HBox leftToolbar = new HBox(4);
        leftToolbar.setAlignment(Pos.CENTER_LEFT);
        leftToolbar.setPadding(new Insets(8, 12, 8, 12));
        leftToolbar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent #393B40 transparent; -fx-border-width: 0 0 1 0;");

        styleToolButton(addListBtn, "Add Quick List");
        addListBtn.setOnAction(e -> onAddNewQuickList());

        styleToolButton(removeListBtn, "Remove Quick List");
        removeListBtn.setOnAction(e -> onRemoveQuickList());

        leftToolbar.getChildren().addAll(addListBtn, removeListBtn);

        quickListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-background: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(quickListView, Priority.ALWAYS);

        quickListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(QuickList item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    setText(item.getName());
                    String bg = isSelected() ? "#2E436E" : "#1E1F22";
                    String textFill = isSelected() ? "#FFFFFF" : "#DFE1E5";
                    setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textFill + "; -fx-font-size: 12px; -fx-padding: 5 10 5 10;");
                }
            }
        });

        quickListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> onSelectQuickList(newV));

        leftPanel.getChildren().addAll(leftToolbar, quickListView);

        // ============================================================
        // 2. Right Panel (Form + Inner Action List)
        // ============================================================
        buildDetailEditor();

        emptyListDetailPlaceholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        rightContainer.setStyle("-fx-background-color: #1E1F22;");
        rightContainer.getChildren().setAll(detailEditor);

        splitPane.getItems().addAll(leftPanel, rightContainer);
        setCenter(splitPane);

        manager.addListener(this::reloadQuickLists);
        reloadQuickLists();

        // Select first item by default if available
        if (!quickListData.isEmpty()) {
            quickListView.getSelectionModel().selectFirst();
        }
    }

    private void buildDetailEditor() {
        detailEditor.setPadding(new Insets(14, 20, 16, 20));
        detailEditor.setStyle("-fx-background-color: #1E1F22;");
        detailEditor.setSpacing(10);
        VBox.setVgrow(detailEditor, Priority.ALWAYS);

        // Row 1: Display name
        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label("Display name:");
        nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 90px;");

        displayNameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        HBox.setHgrow(displayNameField, Priority.ALWAYS);
        displayNameField.textProperty().addListener((obs, oldV, newV) -> {
            if (currentQuickList != null && newV != null) {
                currentQuickList.setName(newV);
                quickListView.refresh();
            }
        });
        nameRow.getChildren().addAll(nameLbl, displayNameField);

        // Row 2: Description
        HBox descRow = new HBox(12);
        descRow.setAlignment(Pos.CENTER_LEFT);
        Label descLbl = new Label("Description:");
        descLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 90px;");

        descriptionField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-font-size: 12px;");
        HBox.setHgrow(descriptionField, Priority.ALWAYS);
        descriptionField.textProperty().addListener((obs, oldV, newV) -> {
            if (currentQuickList != null && newV != null) {
                currentQuickList.setDescription(newV);
            }
        });
        descRow.getChildren().addAll(descLbl, descriptionField);

        // Row 3: Inner Actions Toolbar (+, ---, -, Up, Down)
        HBox actionToolbar = new HBox(4);
        actionToolbar.setAlignment(Pos.CENTER_LEFT);
        actionToolbar.setPadding(new Insets(6, 0, 4, 0));

        styleToolButton(addActionBtn, "Add ⌘N");
        addActionBtn.setOnAction(e -> onAddAction());

        styleToolButton(addSeparatorBtn, "Add Separator");
        addSeparatorBtn.setOnAction(e -> onAddSeparator());

        styleToolButton(removeActionBtn, "Remove");
        removeActionBtn.setOnAction(e -> onRemoveAction());

        styleToolButton(moveUpBtn, "Move Up");
        moveUpBtn.setOnAction(e -> onMoveUp());

        styleToolButton(moveDownBtn, "Move Down");
        moveDownBtn.setOnAction(e -> onMoveDown());

        actionToolbar.getChildren().addAll(addActionBtn, addSeparatorBtn, removeActionBtn, moveUpBtn, moveDownBtn);

        // Row 4: Action List View with Centered Placeholder
        actionListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(actionListView, Priority.ALWAYS);

        noActionsLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        actionListView.setPlaceholder(noActionsLabel);

        actionListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(QuickListItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else if (item.isSeparator()) {
                    setText("-------------");
                    String bg = isSelected() ? "#2E436E" : "#1E1F22";
                    setStyle("-fx-background-color: " + bg + "; -fx-text-fill: #55575E; -fx-font-size: 12px; -fx-padding: 2 8 2 8;");
                } else {
                    String prefix = item.getIconGlyph().isEmpty() ? "   " : item.getIconGlyph() + "  ";
                    setText(prefix + item.getText());
                    String bg = isSelected() ? "#2E436E" : "#1E1F22";
                    String textFill = isSelected() ? "#FFFFFF" : "#DFE1E5";
                    setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textFill + "; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                }
            }
        });

        actionListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateActionButtonsState());

        // Keyboard Shortcut ⌘N / Ctrl+N to add action
        actionListView.setOnKeyPressed(e -> {
            KeyCombination cmdN = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
            if (cmdN.match(e)) {
                onAddAction();
                e.consume();
            }
        });

        detailEditor.getChildren().addAll(nameRow, descRow, actionToolbar, actionListView);
    }

    private void styleToolButton(Button btn, String tooltipText) {
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisabled()) btn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisabled()) btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
        });
    }

    private void reloadQuickLists() {
        quickListData.setAll(manager.getQuickLists());
        quickListView.setItems(quickListData);
        if (currentQuickList != null) {
            QuickList found = manager.getQuickList(currentQuickList.getName());
            if (found != null) {
                quickListView.getSelectionModel().select(found);
            } else if (!quickListData.isEmpty()) {
                quickListView.getSelectionModel().selectFirst();
            }
        }
        updateToolbarState();
    }

    private void onSelectQuickList(QuickList ql) {
        currentQuickList = ql;
        if (ql == null) {
            rightContainer.getChildren().setAll(emptyListDetailPlaceholder);
        } else {
            displayNameField.setText(ql.getName());
            descriptionField.setText(ql.getDescription());
            actionListView.setItems(ql.getItems());
            rightContainer.getChildren().setAll(detailEditor);
        }
        updateToolbarState();
        updateActionButtonsState();
    }

    private void updateToolbarState() {
        boolean hasSel = quickListView.getSelectionModel().getSelectedItem() != null;
        removeListBtn.setDisable(!hasSel);
    }

    private void updateActionButtonsState() {
        if (currentQuickList == null) {
            addActionBtn.setDisable(true);
            addSeparatorBtn.setDisable(true);
            removeActionBtn.setDisable(true);
            moveUpBtn.setDisable(true);
            moveDownBtn.setDisable(true);
            return;
        }

        addActionBtn.setDisable(false);
        addSeparatorBtn.setDisable(false);

        int idx = actionListView.getSelectionModel().getSelectedIndex();
        int size = currentQuickList.getItems().size();
        removeActionBtn.setDisable(idx < 0);
        moveUpBtn.setDisable(idx <= 0);
        moveDownBtn.setDisable(idx < 0 || idx >= size - 1);
    }

    private void onAddNewQuickList() {
        TextInputDialog dialog = new TextInputDialog("New Quick List");
        dialog.setTitle("New Quick List");
        dialog.setHeaderText(null);
        dialog.setContentText("Quick List name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                QuickList ql = new QuickList(name.trim(), "");
                manager.addQuickList(ql);
                quickListView.getSelectionModel().select(ql);
            }
        });
    }

    private void onRemoveQuickList() {
        QuickList sel = quickListView.getSelectionModel().getSelectedItem();
        if (sel != null) {
            manager.removeQuickList(sel);
        }
    }

    private void onAddAction() {
        if (currentQuickList == null) return;
        Window owner = getScene() != null ? getScene().getWindow() : null;
        AddActionsToQuickListDialog dialog = new AddActionsToQuickListDialog(owner);
        QuickListItem item = dialog.showAndWait();
        if (item != null) {
            int selectedIdx = actionListView.getSelectionModel().getSelectedIndex();
            if (selectedIdx >= 0) {
                currentQuickList.addItem(selectedIdx + 1, item);
                actionListView.getSelectionModel().select(selectedIdx + 1);
            } else {
                currentQuickList.addItem(item);
                actionListView.getSelectionModel().select(item);
            }
            manager.save();
        }
    }

    private void onAddSeparator() {
        if (currentQuickList == null) return;
        QuickListItem sep = QuickListItem.separator();
        int selectedIdx = actionListView.getSelectionModel().getSelectedIndex();
        if (selectedIdx >= 0) {
            currentQuickList.addItem(selectedIdx + 1, sep);
            actionListView.getSelectionModel().select(selectedIdx + 1);
        } else {
            currentQuickList.addItem(sep);
            actionListView.getSelectionModel().select(sep);
        }
        manager.save();
    }

    private void onRemoveAction() {
        if (currentQuickList == null) return;
        int idx = actionListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            currentQuickList.removeItem(idx);
            int newIdx = Math.min(idx, currentQuickList.getItems().size() - 1);
            if (newIdx >= 0) {
                actionListView.getSelectionModel().select(newIdx);
            }
            manager.save();
        }
    }

    private void onMoveUp() {
        if (currentQuickList == null) return;
        int idx = actionListView.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            currentQuickList.moveUp(idx);
            actionListView.getSelectionModel().select(idx - 1);
            manager.save();
        }
    }

    private void onMoveDown() {
        if (currentQuickList == null) return;
        int idx = actionListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < currentQuickList.getItems().size() - 1) {
            currentQuickList.moveDown(idx);
            actionListView.getSelectionModel().select(idx + 1);
            manager.save();
        }
    }

    public void revert() {
        manager.revertToDefaults();
        reloadQuickLists();
    }
}