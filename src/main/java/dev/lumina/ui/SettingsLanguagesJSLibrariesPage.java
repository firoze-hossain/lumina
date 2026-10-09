package dev.lumina.ui;

import dev.lumina.javascript.JavaScriptLibrary;
import dev.lumina.javascript.JavaScriptSettingsManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Languages & Frameworks > JavaScript > Libraries settings page in Lumina IDE.
 */
public class SettingsLanguagesJSLibrariesPage extends VBox {

    private final JavaScriptSettingsManager manager = JavaScriptSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private TableView<LibraryRowModel> tableView;
    private final ObservableList<LibraryRowModel> libraryRows = FXCollections.observableArrayList();

    private Button addButton;
    private Button editButton;
    private Button removeButton;
    private Button downloadButton;
    private Button manageScopesButton;

    private List<JavaScriptLibrary> initialLibraries;

    public static class LibraryRowModel {
        private final SimpleBooleanProperty enabled = new SimpleBooleanProperty();
        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleStringProperty type = new SimpleStringProperty();
        private final JavaScriptLibrary original;

        public LibraryRowModel(JavaScriptLibrary lib) {
            this.original = lib.copy();
            this.enabled.set(lib.isEnabled());
            this.name.set(lib.getName());
            this.type.set(lib.getType());
        }

        public SimpleBooleanProperty enabledProperty() {
            return enabled;
        }

        public boolean isEnabled() {
            return enabled.get();
        }

        public void setEnabled(boolean val) {
            enabled.set(val);
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public String getName() {
            return name.get();
        }

        public void setName(String val) {
            name.set(val);
        }

        public SimpleStringProperty typeProperty() {
            return type;
        }

        public String getType() {
            return type.get();
        }

        public void setType(String val) {
            type.set(val);
        }

        public JavaScriptLibrary toLibrary() {
            JavaScriptLibrary lib = original.copy();
            lib.setName(getName());
            lib.setEnabled(isEnabled());
            lib.setType(getType());
            return lib;
        }
    }

    public SettingsLanguagesJSLibrariesPage() {
        setSpacing(10);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        Label titleLabel = new Label("Libraries:");
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox mainRow = new HBox(12);
        VBox.setVgrow(mainRow, Priority.ALWAYS);

        // Table
        tableView = new TableView<>();
        tableView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        HBox.setHgrow(tableView, Priority.ALWAYS);

        TableColumn<LibraryRowModel, Boolean> enabledCol = new TableColumn<>("Enabled");
        enabledCol.setCellValueFactory(cellData -> cellData.getValue().enabledProperty());
        enabledCol.setCellFactory(tc -> new CheckBoxTableCell<LibraryRowModel, Boolean>() {
            {
                setAlignment(Pos.CENTER);
            }
        });
        enabledCol.setPrefWidth(70);
        enabledCol.setMaxWidth(90);
        enabledCol.setMinWidth(60);

        TableColumn<LibraryRowModel, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setPrefWidth(340);

        TableColumn<LibraryRowModel, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        typeCol.setPrefWidth(120);

        tableView.getColumns().addAll(enabledCol, nameCol, typeCol);
        tableView.setItems(libraryRows);

        loadRowsFromManager();

        // Right button column
        VBox buttonsBox = new VBox(8);
        buttonsBox.setPrefWidth(125);

        addButton = createButton("Add...");
        addButton.setOnAction(e -> handleAdd());

        editButton = createButton("Edit...");
        editButton.setOnAction(e -> handleEdit());

        removeButton = createButton("Remove");
        removeButton.setOnAction(e -> handleRemove());

        downloadButton = createButton("Download...");
        downloadButton.setOnAction(e -> handleDownload());

        Region spacer = new Region();
        spacer.setPrefHeight(16);

        manageScopesButton = createButton("Manage Scopes...");
        manageScopesButton.setOnAction(e -> handleManageScopes());

        buttonsBox.getChildren().addAll(addButton, editButton, removeButton, downloadButton, spacer, manageScopesButton);

        mainRow.getChildren().addAll(tableView, buttonsBox);
        getChildren().addAll(titleLabel, mainRow);

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateButtonStates());
        updateButtonStates();
    }

    private Button createButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px; -fx-padding: 5 10 5 10; -fx-cursor: hand;");
        return btn;
    }

    private void updateButtonStates() {
        LibraryRowModel selected = tableView.getSelectionModel().getSelectedItem();
        boolean hasSelection = selected != null;
        editButton.setDisable(!hasSelection);
        boolean isPredefined = hasSelection && "Predefined".equalsIgnoreCase(selected.getType());
        removeButton.setDisable(!hasSelection || isPredefined);
    }

    private void loadRowsFromManager() {
        libraryRows.clear();
        for (JavaScriptLibrary lib : manager.getLibraries()) {
            LibraryRowModel row = new LibraryRowModel(lib);
            row.enabledProperty().addListener((obs, oldV, newV) -> fireModified());
            libraryRows.add(row);
        }
    }

    private void handleAdd() {
        TextInputDialog dlg = new TextInputDialog("my-library");
        dlg.setTitle("Add JavaScript Library");
        dlg.setHeaderText("Add a new JavaScript Library");
        dlg.setContentText("Library name:");
        Optional<String> res = dlg.showAndWait();
        res.ifPresent(name -> {
            if (!name.isBlank()) {
                JavaScriptLibrary lib = new JavaScriptLibrary(name.trim(), true, "Project");
                LibraryRowModel row = new LibraryRowModel(lib);
                row.enabledProperty().addListener((obs, oldV, newV) -> fireModified());
                libraryRows.add(row);
                tableView.getSelectionModel().select(row);
                fireModified();
            }
        });
    }

    private void handleEdit() {
        LibraryRowModel selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dlg = new TextInputDialog(selected.getName());
        dlg.setTitle("Edit JavaScript Library");
        dlg.setHeaderText("Edit library name");
        dlg.setContentText("Library name:");
        Optional<String> res = dlg.showAndWait();
        res.ifPresent(name -> {
            if (!name.isBlank()) {
                selected.setName(name.trim());
                fireModified();
            }
        });
    }

    private void handleRemove() {
        LibraryRowModel selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null && !"Predefined".equalsIgnoreCase(selected.getType())) {
            libraryRows.remove(selected);
            fireModified();
        }
    }

    private void handleDownload() {
        ChoiceDialog<String> dlg = new ChoiceDialog<>("jquery (TypeScript definition)",
                "jquery (TypeScript definition)",
                "react (TypeScript definition)",
                "lodash (TypeScript definition)",
                "vue (TypeScript definition)");
        dlg.setTitle("Download Library");
        dlg.setHeaderText("Download community type definitions");
        dlg.setContentText("Package:");
        dlg.showAndWait().ifPresent(pkg -> {
            String name = pkg.split(" ")[0];
            JavaScriptLibrary lib = new JavaScriptLibrary(name, true, "Global");
            LibraryRowModel row = new LibraryRowModel(lib);
            row.enabledProperty().addListener((obs, oldV, newV) -> fireModified());
            libraryRows.add(row);
            tableView.getSelectionModel().select(row);
            fireModified();
        });
    }

    private void handleManageScopes() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Manage Library Scopes");
        alert.setHeaderText("JavaScript Library Scopes");
        alert.setContentText("Libraries are currently mapped to the entire project scope.");
        alert.showAndWait();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void takeSnapshot() {
        this.initialLibraries = getCurrentLibraries();
    }

    private List<JavaScriptLibrary> getCurrentLibraries() {
        List<JavaScriptLibrary> list = new ArrayList<>();
        for (LibraryRowModel row : libraryRows) {
            list.add(row.toLibrary());
        }
        return list;
    }

    public boolean isModified() {
        return !Objects.equals(getCurrentLibraries(), initialLibraries);
    }

    public void apply() {
        manager.setLibraries(getCurrentLibraries());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        libraryRows.clear();
        if (initialLibraries != null) {
            for (JavaScriptLibrary lib : initialLibraries) {
                LibraryRowModel row = new LibraryRowModel(lib);
                row.enabledProperty().addListener((obs, oldV, newV) -> fireModified());
                libraryRows.add(row);
            }
        }
        updateButtonStates();
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public TableView<LibraryRowModel> getTableView() {
        return tableView;
    }

    public ObservableList<LibraryRowModel> getLibraryRows() {
        return libraryRows;
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getEditButton() {
        return editButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public Button getDownloadButton() {
        return downloadButton;
    }

    public Button getManageScopesButton() {
        return manageScopesButton;
    }
}
