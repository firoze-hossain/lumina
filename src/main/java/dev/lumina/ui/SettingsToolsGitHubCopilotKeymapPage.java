package dev.lumina.ui;

import dev.lumina.tools.GitHubCopilotKeymapEntry;
import dev.lumina.tools.GitHubCopilotKeymapSettings;
import dev.lumina.tools.GitHubCopilotKeymapSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tools > GitHub Copilot > Keymap settings page in Lumina IDE matching Images 1 & 2.
 */
public class SettingsToolsGitHubCopilotKeymapPage extends VBox {

    private final GitHubCopilotKeymapSettingsManager manager;
    private GitHubCopilotKeymapSettings initialSettings;
    private Runnable onModifiedListener;
    private Consumer<String> onNavigateToKeymap;
    private boolean updating = false;

    private TableView<GitHubCopilotKeymapEntry> table;
    private ObservableList<GitHubCopilotKeymapEntry> tableItems;
    private Button editInKeymapBtn;

    public SettingsToolsGitHubCopilotKeymapPage() {
        this(null);
    }

    public SettingsToolsGitHubCopilotKeymapPage(Consumer<String> onNavigateToKeymap) {
        this.manager = GitHubCopilotKeymapSettingsManager.getInstance();
        this.onNavigateToKeymap = onNavigateToKeymap;
        buildUI();
        loadData();
    }

    public void setOnNavigateToKeymap(Consumer<String> onNavigateToKeymap) {
        this.onNavigateToKeymap = onNavigateToKeymap;
    }

    private void buildUI() {
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // Table
        table = new TableView<>();
        table.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<GitHubCopilotKeymapEntry, String> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(420);
        actionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAction()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
                }
            }
        });

        TableColumn<GitHubCopilotKeymapEntry, String> keymapCol = new TableColumn<>("Keymap");
        keymapCol.setPrefWidth(220);
        keymapCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKeymap()));
        keymapCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText(item);
                    if (GitHubCopilotKeymapEntry.NOT_ASSIGNED.equals(item)) {
                        setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-padding: 4 8;");
                    } else {
                        setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8; -fx-font-weight: bold;");
                    }
                }
            }
        });

        table.getColumns().addAll(actionCol, keymapCol);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        tableItems = FXCollections.observableArrayList();
        table.setItems(tableItems);

        // Double-click edit shortcut dialog
        table.setRowFactory(tv -> {
            TableRow<GitHubCopilotKeymapEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    editShortcut(row.getItem());
                }
            });
            return row;
        });

        // Bottom button
        editInKeymapBtn = new Button("Edit in Keymap Settings...");
        editInKeymapBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 12; -fx-cursor: hand; -fx-font-size: 12px;");
        editInKeymapBtn.setOnAction(e -> {
            if (onNavigateToKeymap != null) {
                GitHubCopilotKeymapEntry sel = table.getSelectionModel().getSelectedItem();
                onNavigateToKeymap.accept(sel != null ? sel.getAction() : "Copilot");
            }
        });

        HBox bottomBar = new HBox(editInKeymapBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(table, bottomBar);
    }

    private void editShortcut(GitHubCopilotKeymapEntry entry) {
        if (entry == null) return;
        TextInputDialog dialog = new TextInputDialog(GitHubCopilotKeymapEntry.NOT_ASSIGNED.equals(entry.getKeymap()) ? "" : entry.getKeymap());
        dialog.setTitle("Edit Keyboard Shortcut");
        dialog.setHeaderText("Set shortcut for: " + entry.getAction());
        dialog.setContentText("Key combination (e.g. Ctrl+Alt+Shift+O, or leave empty to clear):");
        dialog.showAndWait().ifPresent(sc -> {
            String updated = sc.trim().isEmpty() ? GitHubCopilotKeymapEntry.NOT_ASSIGNED : sc.trim();
            if (!Objects.equals(entry.getKeymap(), updated)) {
                entry.setKeymap(updated);
                table.refresh();
                notifyModified();
            }
        });
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(GitHubCopilotKeymapSettings s) {
        tableItems.clear();
        if (s != null) {
            for (GitHubCopilotKeymapEntry e : s.getEntries()) {
                tableItems.add(e.copy());
            }
        }
    }

    private GitHubCopilotKeymapSettings getCurrentSettingsFromUI() {
        List<GitHubCopilotKeymapEntry> list = new ArrayList<>();
        for (GitHubCopilotKeymapEntry e : tableItems) {
            list.add(e.copy());
        }
        return new GitHubCopilotKeymapSettings(list);
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        GitHubCopilotKeymapSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void resetDefaults() {
        applySettingsToUI(new GitHubCopilotKeymapSettings());
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Accessors for testing and interaction
    public TableView<GitHubCopilotKeymapEntry> getTable() {
        return table;
    }

    public TableView<GitHubCopilotKeymapEntry> getTableView() {
        return table;
    }

    public ObservableList<GitHubCopilotKeymapEntry> getTableItems() {
        return tableItems;
    }

    public Button getEditInKeymapBtn() {
        return editInKeymapBtn;
    }
}
