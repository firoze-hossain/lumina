package dev.lumina.ui;

import dev.lumina.tools.PythonDocUrlEntry;
import dev.lumina.tools.PythonExternalDocumentationSettings;
import dev.lumina.tools.PythonExternalDocumentationSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tools > Python External Documentation settings page in Lumina IDE.
 * Matches screenshot 1 1:1 with table, macro dialog, and dynamic configuration.
 */
public class SettingsToolsPythonExternalDocumentationPage extends VBox {

    private final PythonExternalDocumentationSettingsManager manager;
    private PythonExternalDocumentationSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private final ObservableList<PythonDocUrlEntry> tableData = FXCollections.observableArrayList();
    private TableView<PythonDocUrlEntry> tableView;
    private Button addButton;
    private Button removeButton;
    private Button editButton;

    public SettingsToolsPythonExternalDocumentationPage() {
        this.manager = PythonExternalDocumentationSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(10);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        Label descLabel = new Label("Configure URLs for Python external documentation to enable quick access to relevant resources.");
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // Toolbar
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-padding: 4 6; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1;");

        addButton = createToolbarButton("+", "Add Documentation URL");
        removeButton = createToolbarButton("−", "Remove Selected URL");
        editButton = createToolbarButton("✏", "Edit Documentation URL");

        toolbar.getChildren().addAll(addButton, removeButton, editButton);

        // Table
        tableView = new TableView<>(tableData);
        tableView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-text-fill: #DFE1E5;");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        TableColumn<PythonDocUrlEntry, String> moduleCol = new TableColumn<>("Module Name");
        moduleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getModuleName()));
        moduleCol.setPrefWidth(140);
        moduleCol.setMinWidth(100);

        TableColumn<PythonDocUrlEntry, String> urlCol = new TableColumn<>("URL/Path Pattern");
        urlCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUrlPattern()));
        urlCol.setPrefWidth(600);

        tableView.getColumns().addAll(moduleCol, urlCol);

        tableView.setRowFactory(tv -> {
            TableRow<PythonDocUrlEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showEditDialog(row.getItem());
                }
            });
            return row;
        });

        getChildren().addAll(descLabel, toolbar, tableView);

        setupListeners();
    }

    private void setupListeners() {
        addButton.setOnAction(e -> showAddDialog());
        removeButton.setOnAction(e -> {
            PythonDocUrlEntry sel = tableView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                tableData.remove(sel);
                notifyModified();
            }
        });
        editButton.setOnAction(e -> {
            PythonDocUrlEntry sel = tableView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                showEditDialog(sel);
            }
        });

        tableData.addListener((javafx.collections.ListChangeListener<PythonDocUrlEntry>) c -> notifyModified());
    }

    private void showAddDialog() {
        PythonDocUrlEntry newEntry = new PythonDocUrlEntry("", "");
        openEditDialog("Add Documentation URL", newEntry, true);
    }

    private void showEditDialog(PythonDocUrlEntry entry) {
        openEditDialog("Edit Documentation URL", entry, false);
    }

    private void openEditDialog(String title, PythonDocUrlEntry entry, boolean isNew) {
        Dialog<PythonDocUrlEntry> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(16, 20, 16, 20));
        contentBox.setStyle("-fx-background-color: #1E1F22;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label moduleLabel = new Label("Module Name:");
        moduleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField moduleField = new TextField(entry.getModuleName());
        moduleField.setPrefWidth(420);
        moduleField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4;");

        Label urlLabel = new Label("URL/Path Pattern:");
        urlLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField urlField = new TextField(entry.getUrlPattern());
        urlField.setPrefWidth(420);
        urlField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4;");

        grid.add(moduleLabel, 0, 0);
        grid.add(moduleField, 1, 0);
        grid.add(urlLabel, 0, 1);
        grid.add(urlField, 1, 1);

        Label macrosLabel = new Label("Available Macros");
        macrosLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");

        ListView<String> macrosList = new ListView<>();
        macrosList.getItems().addAll(
                "{element.name} - name of element under caret",
                "{element.qname} - full-qualified name of element under caret",
                "{module.name} - name of module containing element under caret",
                "{module.basename} - last component of the full-qualified module name of element under caret",
                "{class.name} - name of class containing element under caret",
                "{function.name} - name of function under caret"
        );
        macrosList.setPrefHeight(120);
        macrosList.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157;");

        Button insertButton = new Button("Insert");
        insertButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-cursor: hand;");
        insertButton.setOnAction(e -> {
            String sel = macrosList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                int dashIdx = sel.indexOf(" - ");
                String macro = (dashIdx != -1) ? sel.substring(0, dashIdx) : sel;
                urlField.replaceSelection(macro);
            }
        });

        HBox insertBox = new HBox(insertButton);
        insertBox.setAlignment(Pos.CENTER_RIGHT);

        contentBox.getChildren().addAll(grid, macrosLabel, macrosList, insertBox);
        dialog.getDialogPane().setContent(contentBox);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                return new PythonDocUrlEntry(moduleField.getText().trim(), urlField.getText().trim());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            if (isNew) {
                tableData.add(res);
            } else {
                entry.setModuleName(res.getModuleName());
                entry.setUrlPattern(res.getUrlPattern());
                tableView.refresh();
            }
            notifyModified();
        });
    }

    private Button createToolbarButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px; -fx-padding: 2 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px; -fx-padding: 2 6;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px; -fx-padding: 2 6;"));
        if (tooltipText != null) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        return btn;
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

    private void applySettingsToUI(PythonExternalDocumentationSettings s) {
        if (s == null) return;
        tableData.clear();
        for (PythonDocUrlEntry e : s.getEntries()) {
            tableData.add(e.clone());
        }
    }

    private PythonExternalDocumentationSettings getCurrentSettingsFromUI() {
        PythonExternalDocumentationSettings s = new PythonExternalDocumentationSettings();
        List<PythonDocUrlEntry> copy = new ArrayList<>();
        for (PythonDocUrlEntry e : tableData) {
            copy.add(e.clone());
        }
        s.setEntries(copy);
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        PythonExternalDocumentationSettings current = getCurrentSettingsFromUI();
        return !Objects.equals(initialSettings, current);
    }

    public void apply() {
        PythonExternalDocumentationSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            updating = true;
            try {
                applySettingsToUI(initialSettings);
            } finally {
                updating = false;
            }
            notifyModified();
        }
    }

    public void reset() {
        revertChanges();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public TableView<PythonDocUrlEntry> getTableView() {
        return tableView;
    }

    public ObservableList<PythonDocUrlEntry> getTableData() {
        return tableData;
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public Button getEditButton() {
        return editButton;
    }
}
