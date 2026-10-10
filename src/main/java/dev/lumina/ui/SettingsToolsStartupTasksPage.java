package dev.lumina.ui;

import dev.lumina.tools.StartupTaskEntry;
import dev.lumina.tools.StartupTasksSettings;
import dev.lumina.tools.StartupTasksSettingsManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings UI page for Tools > Startup Tasks in Lumina IDE.
 * 1:1 visual match with reference layout.
 */
public class SettingsToolsStartupTasksPage extends VBox {

    private final ObservableList<StartupTaskEntry> tableData = FXCollections.observableArrayList();
    private final TableView<StartupTaskEntry> tableView;

    private Runnable onModified;
    private boolean suppressEvents = false;

    public SettingsToolsStartupTasksPage() {
        setSpacing(10);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

        // Top Row: "To be started on project opening:" on left, "Run tasks and tools via run configurations" on right
        Label leftHeader = new Label("To be started on project opening:");
        leftHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label rightHint = new Label("Run tasks and tools via run configurations");
        rightHint.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        HBox topRow = new HBox(leftHeader, spacer, rightHint);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Toolbar: +, −, ✏
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 0, 4, 0));

        MenuButton addBtn = createAddMenuButton();
        Button removeBtn = createToolbarButton("−", "Remove startup task", this::removeSelectedTask);
        Button editBtn = createToolbarButton("✏", "Edit startup task name", this::editSelectedTask);

        toolbar.getChildren().addAll(addBtn, removeBtn, editBtn);

        // TableView
        tableView = new TableView<>(tableData);
        tableView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-text-fill: #DFE1E5;");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        TableColumn<StartupTaskEntry, String> nameCol = new TableColumn<>("Configuration");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    StartupTaskEntry entry = getTableRow().getItem();
                    setText(entry.getName() + "  [" + entry.getConfigurationType() + "]");
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                }
            }
        });

        TableColumn<StartupTaskEntry, Boolean> sharedCol = new TableColumn<>("Shared");
        sharedCol.setPrefWidth(90);
        sharedCol.setMaxWidth(100);
        sharedCol.setCellValueFactory(param -> {
            StartupTaskEntry entry = param.getValue();
            SimpleBooleanProperty prop = new SimpleBooleanProperty(entry.isShared());
            prop.addListener((obs, oldV, newV) -> {
                entry.setShared(newV);
                notifyModified();
            });
            return prop;
        });
        sharedCol.setCellFactory(CheckBoxTableCell.forTableColumn(sharedCol));
        sharedCol.setStyle("-fx-alignment: CENTER;");

        tableView.getColumns().add(nameCol);
        tableView.getColumns().add(sharedCol);

        // Empty placeholder
        Label placeholderLabel = new Label("Add run configurations with the + button");
        placeholderLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px;");
        tableView.setPlaceholder(placeholderLabel);

        getChildren().addAll(topRow, toolbar, tableView);

        loadSettings();
    }

    private MenuButton createAddMenuButton() {
        MenuButton btn = new MenuButton("+");
        btn.setTooltip(new Tooltip("Add New Configuration"));
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #AFB1B6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: transparent; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 4;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #AFB1B6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;"));

        // Add options matching Image 2
        String[] configTypes = {
                "Ammonite", "Application", "Attach to Node.js/Chrome", "BSP test",
                "Cargo", "Composer Script", "Compound", "Cypress",
                "Go Build", "Go Remote", "Go Test",
                "Gradle", "Groovy", "Grunt.js", "Gulp.js", "HTTP Request",
                "JAR Application", "npm", "Python", "Shell Script"
        };

        for (String type : configTypes) {
            MenuItem item = new MenuItem(type);
            item.setOnAction(e -> addTaskWithType(type));
            btn.getItems().add(item);
        }

        // Docker sub-menu
        Menu dockerMenu = new Menu("Docker");
        MenuItem dockerfileItem = new MenuItem("Dockerfile");
        dockerfileItem.setOnAction(e -> addTaskWithType("Docker: Dockerfile"));
        MenuItem dockerImageItem = new MenuItem("Docker Image");
        dockerImageItem.setOnAction(e -> addTaskWithType("Docker: Docker Image"));
        MenuItem dockerComposeItem = new MenuItem("Docker Compose");
        dockerComposeItem.setOnAction(e -> addTaskWithType("Docker: Docker Compose"));
        dockerMenu.getItems().addAll(dockerfileItem, dockerImageItem, dockerComposeItem);
        btn.getItems().add(8, dockerMenu);

        return btn;
    }

    private Button createToolbarButton(String text, String tooltip, Runnable action) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #AFB1B6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: transparent; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 4;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #AFB1B6; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;"));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void addTaskWithType(String type) {
        String defaultName = "Run " + type;
        StartupTaskEntry entry = new StartupTaskEntry(defaultName, type, false);
        tableData.add(entry);
        tableView.getSelectionModel().select(entry);
        notifyModified();
    }

    private void removeSelectedTask() {
        StartupTaskEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            tableData.remove(selected);
            notifyModified();
        }
    }

    private void editSelectedTask() {
        StartupTaskEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            TextInputDialog dialog = new TextInputDialog(selected.getName());
            dialog.setTitle("Edit Startup Task");
            dialog.setHeaderText("Configuration name:");
            dialog.showAndWait().ifPresent(newName -> {
                selected.setName(newName.trim());
                tableView.refresh();
                notifyModified();
            });
        }
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (!suppressEvents && onModified != null) {
            onModified.run();
        }
    }

    public void loadSettings() {
        suppressEvents = true;
        try {
            StartupTasksSettings s = StartupTasksSettingsManager.getInstance().getSettings();
            tableData.clear();
            for (StartupTaskEntry task : s.getTasks()) {
                tableData.add(task.clone());
            }
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        StartupTasksSettings current = StartupTasksSettingsManager.getInstance().getSettings();
        if (tableData.size() != current.getTasks().size()) return true;
        for (int i = 0; i < tableData.size(); i++) {
            if (!Objects.equals(tableData.get(i), current.getTasks().get(i))) {
                return true;
            }
        }
        return false;
    }

    public void apply() {
        StartupTasksSettings s = new StartupTasksSettings();
        List<StartupTaskEntry> copies = new ArrayList<>();
        for (StartupTaskEntry task : tableData) {
            copies.add(task.clone());
        }
        s.setTasks(copies);
        StartupTasksSettingsManager.getInstance().setSettings(s);
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    public ObservableList<StartupTaskEntry> getTableData() {
        return tableData;
    }

    public TableView<StartupTaskEntry> getTableView() {
        return tableView;
    }
}
