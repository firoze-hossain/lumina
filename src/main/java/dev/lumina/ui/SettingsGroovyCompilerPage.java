package dev.lumina.ui;

import dev.lumina.build.CompilerExcludeEntry;
import dev.lumina.build.GroovyCompilerSettings;
import dev.lumina.build.GroovyCompilerSettingsManager;
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
import java.util.function.Consumer;

/**
 * Settings subpage for Build, Execution, Deployment > Compiler > Groovy Compiler.
 * Matches 1:1 with reference screenshot media_1791430028667.png:
 *  - Info hyperlink: Alternatively, you can specify Groovy-Eclipse compiler at Java Compiler page
 *  - Path to configscript: with browse button
 *  - Invoke dynamic support checkbox
 *  - Exclude from stub generation table with +, - toolbar
 */
public class SettingsGroovyCompilerPage extends VBox {

    private final GroovyCompilerSettingsManager manager = GroovyCompilerSettingsManager.getInstance();

    private final Hyperlink javaCompilerLink = new Hyperlink("Java Compiler page");
    private final TextField configScriptField = new TextField();
    private final Button browseScriptBtn = new Button("📁");
    private final CheckBox invokeDynamicSupportCheck = new CheckBox("Invoke dynamic support");

    private final Button addExcludeBtn = new Button("+");
    private final Button removeExcludeBtn = new Button("-");
    private final TableView<ExcludeItem> excludesTable = new TableView<>();
    private final ObservableList<ExcludeItem> excludesItems = FXCollections.observableArrayList();

    private GroovyCompilerSettings initialSettings;
    private Runnable onModifiedListener;
    private Consumer<String> navigationHandler;
    private boolean updating = false;

    public SettingsGroovyCompilerPage() {
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // 1. Info header
        Label infoPrefix = new Label("Alternatively, you can specify Groovy-Eclipse compiler at ");
        infoPrefix.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        javaCompilerLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-padding: 0;");
        javaCompilerLink.setOnAction(e -> {
            if (navigationHandler != null) {
                navigationHandler.accept("Java Compiler");
            }
        });

        HBox infoBox = new HBox(infoPrefix, javaCompilerLink);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        // 2. Path to configscript
        Label scriptLabel = new Label("Path to configscript:");
        scriptLabel.setMinWidth(160);
        scriptLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        configScriptField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
        HBox.setHgrow(configScriptField, Priority.ALWAYS);

        browseScriptBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10; -fx-cursor: hand; -fx-font-size: 12px;");
        browseScriptBtn.setOnAction(e -> browseConfigScript());

        HBox scriptRow = new HBox(8, scriptLabel, configScriptField, browseScriptBtn);
        scriptRow.setAlignment(Pos.CENTER_LEFT);

        // 3. Invoke dynamic support
        invokeDynamicSupportCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // 4. Exclude from stub generation
        Label excludesHeader = new Label("Exclude from stub generation:");
        excludesHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        styleToolbarButton(addExcludeBtn);
        styleToolbarButton(removeExcludeBtn);

        addExcludeBtn.setOnAction(e -> showAddExcludeMenu());
        removeExcludeBtn.setOnAction(e -> removeSelectedExclude());

        toolbar.getChildren().addAll(addExcludeBtn, removeExcludeBtn);

        excludesTable.setEditable(true);
        excludesTable.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4;");
        excludesTable.setPlaceholder(new Label("No excludes"));
        VBox.setVgrow(excludesTable, Priority.ALWAYS);

        TableColumn<ExcludeItem, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(data -> data.getValue().pathProperty());
        pathCol.setCellFactory(TextFieldTableCell.forTableColumn());
        pathCol.setOnEditCommit(e -> {
            e.getRowValue().setPath(e.getNewValue());
            notifyModified();
        });
        pathCol.prefWidthProperty().bind(excludesTable.widthProperty().subtract(120));

        TableColumn<ExcludeItem, Boolean> recursiveCol = new TableColumn<>("Recursiv...");
        recursiveCol.setCellValueFactory(data -> data.getValue().recursiveProperty());
        recursiveCol.setCellFactory(CheckBoxTableCell.forTableColumn(recursiveCol));
        recursiveCol.setPrefWidth(110);
        recursiveCol.setEditable(true);

        excludesTable.getColumns().addAll(pathCol, recursiveCol);
        excludesTable.setItems(excludesItems);

        getChildren().addAll(infoBox, scriptRow, invokeDynamicSupportCheck, excludesHeader, toolbar, excludesTable);

        configScriptField.textProperty().addListener((obs, o, n) -> notifyModified());
        invokeDynamicSupportCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
    }

    private void styleToolbarButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 8; -fx-cursor: hand; -fx-font-weight: bold;");
    }

    private void browseConfigScript() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Groovy Config Script");
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            configScriptField.setText(file.getAbsolutePath());
        }
    }

    private void showAddExcludeMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem addFile = new MenuItem("Exclude File...");
        addFile.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exclude File from Stub Generation");
            File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                addExcludeEntry(file.getAbsolutePath(), false);
            }
        });

        MenuItem addDir = new MenuItem("Exclude Directory...");
        addDir.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Exclude Directory from Stub Generation");
            File dir = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (dir != null) {
                addExcludeEntry(dir.getAbsolutePath(), true);
            }
        });

        menu.getItems().addAll(addDir, addFile);
        menu.show(addExcludeBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    public void addExcludeEntry(String path, boolean recursive) {
        ExcludeItem item = new ExcludeItem(path, recursive);
        item.pathProperty().addListener((obs, o, n) -> notifyModified());
        item.recursiveProperty().addListener((obs, o, n) -> notifyModified());
        excludesItems.add(item);
        excludesTable.getSelectionModel().select(item);
        notifyModified();
    }

    private void removeSelectedExclude() {
        ExcludeItem sel = excludesTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            excludesItems.remove(sel);
            notifyModified();
        }
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        configScriptField.setText(initialSettings.getConfigScriptPath());
        invokeDynamicSupportCheck.setSelected(initialSettings.isInvokeDynamicSupport());

        excludesItems.clear();
        for (CompilerExcludeEntry e : initialSettings.getStubGenerationExcludes()) {
            ExcludeItem item = new ExcludeItem(e.getPath(), e.isRecursive());
            item.pathProperty().addListener((obs, o, n) -> notifyModified());
            item.recursiveProperty().addListener((obs, o, n) -> notifyModified());
            excludesItems.add(item);
        }

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        GroovyCompilerSettings cur = getCurrentSettings();
        return !initialSettings.equals(cur);
    }

    public void apply() {
        GroovyCompilerSettings cur = getCurrentSettings();
        manager.setSettings(cur);
        initialSettings = cur.clone();
    }

    public void reset() {
        loadData();
    }

    public GroovyCompilerSettings getCurrentSettings() {
        GroovyCompilerSettings s = new GroovyCompilerSettings();
        s.setConfigScriptPath(configScriptField.getText());
        s.setInvokeDynamicSupport(invokeDynamicSupportCheck.isSelected());
        List<CompilerExcludeEntry> list = new ArrayList<>();
        for (ExcludeItem item : excludesItems) {
            list.add(new CompilerExcludeEntry(item.getPath(), item.isRecursive()));
        }
        s.setStubGenerationExcludes(list);
        return s;
    }

    public TextField getConfigScriptField() {
        return configScriptField;
    }

    public CheckBox getInvokeDynamicSupportCheck() {
        return invokeDynamicSupportCheck;
    }

    public ObservableList<ExcludeItem> getExcludesItems() {
        return excludesItems;
    }

    public TableView<ExcludeItem> getExcludesTable() {
        return excludesTable;
    }

    public Hyperlink getJavaCompilerLink() {
        return javaCompilerLink;
    }

    public void setNavigationHandler(Consumer<String> handler) {
        this.navigationHandler = handler;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public static class ExcludeItem {
        private final SimpleStringProperty path = new SimpleStringProperty();
        private final SimpleBooleanProperty recursive = new SimpleBooleanProperty();

        public ExcludeItem(String path, boolean recursive) {
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
