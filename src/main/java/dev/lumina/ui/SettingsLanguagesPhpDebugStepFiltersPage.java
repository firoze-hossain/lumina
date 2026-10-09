package dev.lumina.ui;

import dev.lumina.php.PhpDebugSettings;
import dev.lumina.php.PhpSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings page for Languages & Frameworks > PHP > Debug > Step Filters.
 * Faithfully matches the reference IDE layout and dynamic configuration.
 */
public class SettingsLanguagesPhpDebugStepFiltersPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    private CheckBox skipMagicMethodsCheck;
    private CheckBox skipConstructorsCheck;

    private final ObservableList<String> skippedMethodsList = FXCollections.observableArrayList();
    private ListView<String> methodsListView;

    private final ObservableList<String> skippedFilesList = FXCollections.observableArrayList();
    private ListView<String> filesListView;

    // Initial state for dirty tracking
    private boolean initialSkipMagicMethods = false;
    private boolean initialSkipConstructors = false;
    private List<String> initialSkippedMethods = new ArrayList<>();
    private List<String> initialSkippedFiles = new ArrayList<>();

    private Runnable onModified;

    public SettingsLanguagesPhpDebugStepFiltersPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

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
        // Top checkboxes
        skipMagicMethodsCheck = new CheckBox("Skip magic methods");
        skipMagicMethodsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        skipMagicMethodsCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        skipConstructorsCheck = new CheckBox("Skip constructors");
        skipConstructorsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        skipConstructorsCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        VBox topChecksBox = new VBox(8, skipMagicMethodsCheck, skipConstructorsCheck);

        // Methods Section
        HBox methodsHeader = createSectionHeader("Methods");
        HBox methodsToolbar = new HBox(4);
        methodsToolbar.setAlignment(Pos.CENTER_LEFT);
        methodsToolbar.setPadding(new Insets(4, 0, 4, 0));

        Button addMethodBtn = createToolbarButton("+", "Add method pattern", this::handleAddMethod);
        Button removeMethodBtn = createToolbarButton("—", "Remove selected method", this::handleRemoveMethod);
        methodsToolbar.getChildren().addAll(addMethodBtn, removeMethodBtn);

        HBox methodsTableHeader = createTableHeader("Skipped Methods");
        methodsListView = createFilterListView(skippedMethodsList, "Nothing to show");
        VBox.setVgrow(methodsListView, Priority.ALWAYS);

        VBox methodsContainer = new VBox(methodsToolbar, methodsTableHeader, methodsListView);
        methodsContainer.setPrefHeight(180);
        VBox.setVgrow(methodsContainer, Priority.ALWAYS);

        // Files Section
        HBox filesHeader = createSectionHeader("Files");
        HBox filesToolbar = new HBox(4);
        filesToolbar.setAlignment(Pos.CENTER_LEFT);
        filesToolbar.setPadding(new Insets(4, 0, 4, 0));

        Button addFileBtn = createToolbarButton("+", "Add file or directory", this::handleAddFile);
        Button removeFileBtn = createToolbarButton("—", "Remove selected file", this::handleRemoveFile);
        filesToolbar.getChildren().addAll(addFileBtn, removeFileBtn);

        HBox filesTableHeader = createTableHeader("Skipped Files");
        filesListView = createFilterListView(skippedFilesList, "Nothing to show");
        VBox.setVgrow(filesListView, Priority.ALWAYS);

        VBox filesContainer = new VBox(filesToolbar, filesTableHeader, filesListView);
        filesContainer.setPrefHeight(180);
        VBox.setVgrow(filesContainer, Priority.ALWAYS);

        getChildren().addAll(topChecksBox, methodsHeader, methodsContainer, filesHeader, filesContainer);
    }

    private HBox createSectionHeader(String titleText) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(titleText);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(label, line);
        return box;
    }

    private HBox createTableHeader(String title) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 8, 4, 8));
        header.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1;");

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 11px; -fx-font-weight: bold;");
        header.getChildren().add(label);
        return header;
    }

    private ListView<String> createFilterListView(ObservableList<String> list, String emptyMessage) {
        ListView<String> lv = new ListView<>(list);
        lv.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1;");

        Label placeholder = new Label(emptyMessage);
        placeholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        lv.setPlaceholder(placeholder);

        lv.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 8;");
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-padding: 3 8;");
                    }
                }
            }
        });

        return lv;
    }

    private Button createToolbarButton(String text, String tooltip, Runnable action) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand; -fx-background-radius: 3;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;"));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void handleAddMethod() {
        TextInputDialog dialog = new TextInputDialog("*::*");
        dialog.setTitle("Add Skipped Method");
        dialog.setHeaderText("Enter class and method pattern (e.g. ClassName::method or *::*):");
        dialog.setContentText("Pattern:");
        Window win = getScene() != null ? getScene().getWindow() : null;
        if (win != null) {
            dialog.initOwner(win);
        }
        dialog.showAndWait().ifPresent(pattern -> {
            if (!pattern.isBlank() && !skippedMethodsList.contains(pattern.trim())) {
                skippedMethodsList.add(pattern.trim());
                methodsListView.getSelectionModel().select(pattern.trim());
                notifyModified();
            }
        });
    }

    private void handleRemoveMethod() {
        int idx = methodsListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < skippedMethodsList.size()) {
            skippedMethodsList.remove(idx);
            notifyModified();
        }
    }

    private void handleAddFile() {
        Window win = getScene() != null ? getScene().getWindow() : null;
        SelectPathDialog dlg = new SelectPathDialog(win, "");
        String chosen = dlg.showAndWait();
        if (chosen != null && !chosen.isBlank() && !skippedFilesList.contains(chosen)) {
            skippedFilesList.add(chosen);
            filesListView.getSelectionModel().select(chosen);
            notifyModified();
        }
    }

    private void handleRemoveFile() {
        int idx = filesListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < skippedFilesList.size()) {
            skippedFilesList.remove(idx);
            notifyModified();
        }
    }

    public void loadFromManager() {
        PhpDebugSettings ds = manager.getDebugSettings();
        skipMagicMethodsCheck.setSelected(ds.isSkipMagicMethods());
        skipConstructorsCheck.setSelected(ds.isSkipConstructors());
        skippedMethodsList.setAll(ds.getSkippedMethods());
        skippedFilesList.setAll(ds.getSkippedFiles());

        initialSkipMagicMethods = ds.isSkipMagicMethods();
        initialSkipConstructors = ds.isSkipConstructors();
        initialSkippedMethods = new ArrayList<>(ds.getSkippedMethods());
        initialSkippedFiles = new ArrayList<>(ds.getSkippedFiles());
    }

    public boolean isModified() {
        return skipMagicMethodsCheck.isSelected() != initialSkipMagicMethods ||
                skipConstructorsCheck.isSelected() != initialSkipConstructors ||
                !skippedMethodsList.equals(initialSkippedMethods) ||
                !skippedFilesList.equals(initialSkippedFiles);
    }

    public void apply() {
        PhpDebugSettings ds = manager.getDebugSettings();
        ds.setSkipMagicMethods(skipMagicMethodsCheck.isSelected());
        ds.setSkipConstructors(skipConstructorsCheck.isSelected());
        ds.setSkippedMethods(new ArrayList<>(skippedMethodsList));
        ds.setSkippedFiles(new ArrayList<>(skippedFilesList));
        manager.setDebugSettings(ds);

        initialSkipMagicMethods = ds.isSkipMagicMethods();
        initialSkipConstructors = ds.isSkipConstructors();
        initialSkippedMethods = new ArrayList<>(ds.getSkippedMethods());
        initialSkippedFiles = new ArrayList<>(ds.getSkippedFiles());
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }
}
