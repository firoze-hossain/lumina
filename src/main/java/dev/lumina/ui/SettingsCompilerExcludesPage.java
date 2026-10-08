package dev.lumina.ui;

import dev.lumina.build.CompilerExcludeEntry;
import dev.lumina.build.CompilerExcludesSettings;
import dev.lumina.build.CompilerExcludesSettingsManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings subpage for Build, Execution, Deployment > Compiler > Excludes.
 * Matches 1:1 with reference screenshot media_1791430013046.png:
 *  - Toolbar: +, -
 *  - Table with columns: Path, Recursiv...
 *  - Placeholder: "No excludes"
 */
public class SettingsCompilerExcludesPage extends VBox {

    private final CompilerExcludesSettingsManager manager = CompilerExcludesSettingsManager.getInstance();

    private final Button addBtn = new Button("+");
    private final Button removeBtn = new Button("-");
    private final TableView<ExcludeRowItem> table = new TableView<>();
    private final ObservableList<ExcludeRowItem> tableItems = FXCollections.observableArrayList();

    private CompilerExcludesSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsCompilerExcludesPage() {
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(10);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        styleToolbarButton(addBtn);
        styleToolbarButton(removeBtn);

        addBtn.setOnAction(e -> showAddExcludeMenu());
        removeBtn.setOnAction(e -> removeSelectedExclude());

        toolbar.getChildren().addAll(addBtn, removeBtn);

        table.setEditable(true);
        table.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4;");
        table.setPlaceholder(new Label("No excludes"));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ExcludeRowItem, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(data -> data.getValue().pathProperty());
        pathCol.setCellFactory(TextFieldTableCell.forTableColumn());
        pathCol.setOnEditCommit(e -> {
            e.getRowValue().setPath(e.getNewValue());
            notifyModified();
        });
        pathCol.prefWidthProperty().bind(table.widthProperty().subtract(120));

        TableColumn<ExcludeRowItem, Boolean> recursiveCol = new TableColumn<>("Recursiv...");
        recursiveCol.setCellValueFactory(data -> data.getValue().recursiveProperty());
        recursiveCol.setCellFactory(CheckBoxTableCell.forTableColumn(recursiveCol));
        recursiveCol.setPrefWidth(110);
        recursiveCol.setEditable(true);

        table.getColumns().addAll(pathCol, recursiveCol);
        table.setItems(tableItems);

        getChildren().addAll(toolbar, table);
    }

    private void styleToolbarButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 8; -fx-cursor: hand; -fx-font-weight: bold;");
    }

    private void showAddExcludeMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem addFile = new MenuItem("Exclude File...");
        addFile.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exclude File from Compilation");
            File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                addExcludeEntry(file.getAbsolutePath(), false);
            }
        });

        MenuItem addDir = new MenuItem("Exclude Directory...");
        addDir.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Exclude Directory from Compilation");
            File dir = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (dir != null) {
                addExcludeEntry(dir.getAbsolutePath(), true);
            }
        });

        menu.getItems().addAll(addDir, addFile);
        menu.show(addBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    public void addExcludeEntry(String path, boolean recursive) {
        ExcludeRowItem item = new ExcludeRowItem(path, recursive);
        item.pathProperty().addListener((obs, o, n) -> notifyModified());
        item.recursiveProperty().addListener((obs, o, n) -> notifyModified());
        tableItems.add(item);
        table.getSelectionModel().select(item);
        notifyModified();
    }

    private void removeSelectedExclude() {
        ExcludeRowItem sel = table.getSelectionModel().getSelectedItem();
        if (sel != null) {
            tableItems.remove(sel);
            notifyModified();
        }
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        tableItems.clear();
        for (CompilerExcludeEntry e : initialSettings.getEntries()) {
            ExcludeRowItem item = new ExcludeRowItem(e.getPath(), e.isRecursive());
            item.pathProperty().addListener((obs, o, n) -> notifyModified());
            item.recursiveProperty().addListener((obs, o, n) -> notifyModified());
            tableItems.add(item);
        }

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        CompilerExcludesSettings cur = getCurrentSettings();
        return !initialSettings.equals(cur);
    }

    public void apply() {
        CompilerExcludesSettings cur = getCurrentSettings();
        manager.setSettings(cur);
        initialSettings = cur.clone();
    }

    public void reset() {
        loadData();
    }

    public CompilerExcludesSettings getCurrentSettings() {
        CompilerExcludesSettings s = new CompilerExcludesSettings();
        List<CompilerExcludeEntry> list = new ArrayList<>();
        for (ExcludeRowItem item : tableItems) {
            list.add(new CompilerExcludeEntry(item.getPath(), item.isRecursive()));
        }
        s.setEntries(list);
        return s;
    }

    public ObservableList<ExcludeRowItem> getTableItems() {
        return tableItems;
    }

    public TableView<ExcludeRowItem> getTable() {
        return table;
    }

    public Button getAddBtn() {
        return addBtn;
    }

    public Button getRemoveBtn() {
        return removeBtn;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public static class ExcludeRowItem {
        private final SimpleStringProperty path = new SimpleStringProperty();
        private final SimpleBooleanProperty recursive = new SimpleBooleanProperty();

        public ExcludeRowItem(String path, boolean recursive) {
            this.path.set(path);
            this.recursive.set(recursive);
        }

        public String getPath() { return path.get(); }
        public void setPath(String val) { path.set(val); }
        public SimpleStringProperty pathProperty() { return path; }

        public boolean isRecursive() { return recursive.get(); }
        public void setRecursive(boolean val) { recursive.set(val); }
        public SimpleBooleanProperty recursiveProperty() { return recursive; }
    }
}
