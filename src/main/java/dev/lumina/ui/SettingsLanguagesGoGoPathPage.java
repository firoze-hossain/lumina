package dev.lumina.ui;

import dev.lumina.go.GoSettings;
import dev.lumina.go.GoSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings page for Languages & Frameworks > Go > GOPATH.
 * Faithfully matches Image 4.
 */
public class SettingsLanguagesGoGoPathPage extends VBox {

    private final GoSettingsManager manager = GoSettingsManager.getInstance();

    private final ObservableList<String> globalPaths = FXCollections.observableArrayList();
    private final ObservableList<String> projectPaths = FXCollections.observableArrayList();

    private ListView<String> globalListView;
    private ListView<String> projectListView;

    private CheckBox useEnvGoPathCheck;
    private CheckBox indexEntireGoPathCheck;

    private List<String> initialGlobalPaths = new ArrayList<>();
    private List<String> initialProjectPaths = new ArrayList<>();
    private boolean initialUseEnv = true;
    private boolean initialIndexEntire = false;

    private Runnable onModified;

    public SettingsLanguagesGoGoPathPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadFromManager();
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        // 1. Global GOPATH section
        HBox globalHeader = createSectionHeader("Global GOPATH");
        HBox globalToolbar = new HBox(4);
        globalToolbar.setAlignment(Pos.CENTER_LEFT);
        globalToolbar.setPadding(new Insets(3, 6, 3, 6));
        globalToolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0; -fx-background-radius: 4 4 0 0;");

        globalListView = createPathListView(globalPaths);

        Button addGlobalBtn = createToolbarButton("+", "Add Global GOPATH", () -> addDirectory(globalPaths));
        Button removeGlobalBtn = createToolbarButton("—", "Remove Selected Path", () -> removeSelected(globalListView, globalPaths));
        Button upGlobalBtn = createToolbarButton("↑", "Move Up", () -> moveSelectedUp(globalListView, globalPaths));
        Button downGlobalBtn = createToolbarButton("↓", "Move Down", () -> moveSelectedDown(globalListView, globalPaths));
        globalToolbar.getChildren().addAll(addGlobalBtn, removeGlobalBtn, upGlobalBtn, downGlobalBtn);

        VBox globalBox = new VBox(globalHeader, globalToolbar, globalListView);
        globalBox.setSpacing(4);

        // 2. Project GOPATH section
        HBox projectHeader = createSectionHeader("Project GOPATH");
        HBox projectToolbar = new HBox(4);
        projectToolbar.setAlignment(Pos.CENTER_LEFT);
        projectToolbar.setPadding(new Insets(3, 6, 3, 6));
        projectToolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0; -fx-background-radius: 4 4 0 0;");

        projectListView = createPathListView(projectPaths);

        Button addProjectBtn = createToolbarButton("+", "Add Project GOPATH", () -> addDirectory(projectPaths));
        Button removeProjectBtn = createToolbarButton("—", "Remove Selected Path", () -> removeSelected(projectListView, projectPaths));
        Button upProjectBtn = createToolbarButton("↑", "Move Up", () -> moveSelectedUp(projectListView, projectPaths));
        Button downProjectBtn = createToolbarButton("↓", "Move Down", () -> moveSelectedDown(projectListView, projectPaths));
        projectToolbar.getChildren().addAll(addProjectBtn, removeProjectBtn, upProjectBtn, downProjectBtn);

        VBox projectBox = new VBox(projectHeader, projectToolbar, projectListView);
        projectBox.setSpacing(4);

        // 3. Checkboxes
        useEnvGoPathCheck = new CheckBox("Use GOPATH that's defined in system environment");
        useEnvGoPathCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        useEnvGoPathCheck.setSelected(true);
        useEnvGoPathCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        indexEntireGoPathCheck = new CheckBox("Index entire GOPATH");
        indexEntireGoPathCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        indexEntireGoPathCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        Label indexHelp = new Label("?");
        indexHelp.setStyle("-fx-text-fill: #707890; -fx-font-size: 11px; -fx-cursor: hand; "
                + "-fx-border-color: #707890; -fx-border-radius: 8; -fx-min-width: 15px; -fx-min-height: 15px; "
                + "-fx-alignment: center; -fx-padding: 0 3 0 3;");
        Tooltip.install(indexHelp, new Tooltip("When enabled, indexes all packages in GOPATH. Otherwise, only used packages are indexed."));

        HBox indexRow = new HBox(6, indexEntireGoPathCheck, indexHelp);
        indexRow.setAlignment(Pos.CENTER_LEFT);

        VBox checksBox = new VBox(8, useEnvGoPathCheck, indexRow);
        checksBox.setPadding(new Insets(4, 0, 4, 0));

        // 4. Module GOPATH section
        HBox moduleHeader = createSectionHeader("Module GOPATH");
        Label modulePlaceholder = new Label("Nothing to show");
        modulePlaceholder.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        StackPane moduleBoxContainer = new StackPane(modulePlaceholder);
        moduleBoxContainer.setPrefHeight(100);
        moduleBoxContainer.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4px;");

        VBox moduleBox = new VBox(moduleHeader, moduleBoxContainer);
        moduleBox.setSpacing(4);

        getChildren().addAll(globalBox, projectBox, checksBox, moduleBox);
    }

    private ListView<String> createPathListView(ObservableList<String> list) {
        ListView<String> lv = new ListView<>(list);
        lv.setPrefHeight(120);
        lv.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4;");

        Label placeholder = new Label("Nothing to show");
        placeholder.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
        lv.setPlaceholder(placeholder);

        lv.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                }
            }
        });

        return lv;
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 0, 4, 0));

        Label chevron = new Label("⌵");
        chevron.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-pref-height: 1px; -fx-max-height: 1px;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(chevron, lbl, line);
        return header;
    }

    private Button createToolbarButton(String text, String tooltipText, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;");
        if (tooltipText != null) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private Stage getOwnerStage() {
        if (getScene() != null && getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    private void addDirectory(ObservableList<String> list) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select GOPATH Directory");
        File dir = chooser.showDialog(getOwnerStage());
        if (dir != null && !list.contains(dir.getAbsolutePath())) {
            list.add(dir.getAbsolutePath());
            notifyModified();
        }
    }

    private void removeSelected(ListView<String> lv, ObservableList<String> list) {
        int idx = lv.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < list.size()) {
            list.remove(idx);
            notifyModified();
        }
    }

    private void moveSelectedUp(ListView<String> lv, ObservableList<String> list) {
        int idx = lv.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            String item = list.remove(idx);
            list.add(idx - 1, item);
            lv.getSelectionModel().select(idx - 1);
            notifyModified();
        }
    }

    private void moveSelectedDown(ListView<String> lv, ObservableList<String> list) {
        int idx = lv.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < list.size() - 1) {
            String item = list.remove(idx);
            list.add(idx + 1, item);
            lv.getSelectionModel().select(idx + 1);
            notifyModified();
        }
    }

    public void loadFromManager() {
        GoSettings s = manager.getSettings();
        globalPaths.setAll(s.getGlobalGoPaths());
        projectPaths.setAll(s.getProjectGoPaths());
        useEnvGoPathCheck.setSelected(s.isUseGoPathFromEnv());
        indexEntireGoPathCheck.setSelected(s.isIndexEntireGoPath());

        initialGlobalPaths = new ArrayList<>(globalPaths);
        initialProjectPaths = new ArrayList<>(projectPaths);
        initialUseEnv = s.isUseGoPathFromEnv();
        initialIndexEntire = s.isIndexEntireGoPath();
    }

    public boolean isModified() {
        return !globalPaths.equals(initialGlobalPaths) ||
                !projectPaths.equals(initialProjectPaths) ||
                useEnvGoPathCheck.isSelected() != initialUseEnv ||
                indexEntireGoPathCheck.isSelected() != initialIndexEntire;
    }

    public void apply() {
        GoSettings s = manager.getSettings();
        s.setGlobalGoPaths(new ArrayList<>(globalPaths));
        s.setProjectGoPaths(new ArrayList<>(projectPaths));
        s.setUseGoPathFromEnv(useEnvGoPathCheck.isSelected());
        s.setIndexEntireGoPath(indexEntireGoPathCheck.isSelected());
        manager.setSettings(s);

        initialGlobalPaths = new ArrayList<>(globalPaths);
        initialProjectPaths = new ArrayList<>(projectPaths);
        initialUseEnv = useEnvGoPathCheck.isSelected();
        initialIndexEntire = indexEntireGoPathCheck.isSelected();
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }

    public List<String> getGlobalPaths() {
        return new ArrayList<>(globalPaths);
    }

    public List<String> getProjectPaths() {
        return new ArrayList<>(projectPaths);
    }

    public boolean isUseEnvGoPath() {
        return useEnvGoPathCheck.isSelected();
    }

    public boolean isIndexEntireGoPath() {
        return indexEntireGoPathCheck.isSelected();
    }
}
