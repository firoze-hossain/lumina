// SettingsFileTypesPage.java
package dev.lumina.ui;

import dev.lumina.filetypes.*;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings page for Editor > File Types, faithfully matching IntelliJ IDEA's
 * New UI design (screenshots media_1791256017776.png to media_1791256034634.png).
 * Provides dynamic configuration of recognized file types, wildcard patterns,
 * hashbang patterns, user-defined file types, and ignored files/folders with persistence.
 */
public class SettingsFileTypesPage extends VBox {

    // Main Toggle / Segmented bar
    private final ToggleButton recognizedBtn = new ToggleButton("Recognized File Types");
    private final ToggleButton ignoredBtn = new ToggleButton("Ignored Files and Folders");
    private final ToggleGroup viewGroup = new ToggleGroup();
    private final StackPane contentStack = new StackPane();

    // Recognized View controls
    private final ListView<FileType> fileTypeListView = new ListView<>();
    private final ListView<String> patternListView = new ListView<>();
    private final ListView<String> hashbangListView = new ListView<>();

    private final Button addFileTypeBtn = createToolbarButton("+", "Add file type");
    private final Button removeFileTypeBtn = createToolbarButton("—", "Remove file type");
    private final Button editFileTypeBtn = createToolbarButton("✎", "Edit file type");

    private final Button addPatternBtn = createToolbarButton("+", "Add wildcard pattern");
    private final Button removePatternBtn = createToolbarButton("—", "Remove wildcard pattern");
    private final Button editPatternBtn = createToolbarButton("✎", "Edit wildcard pattern");

    private final Button addHashbangBtn = createToolbarButton("+", "Add HashBang pattern");
    private final Button removeHashbangBtn = createToolbarButton("—", "Remove HashBang pattern");
    private final Button editHashbangBtn = createToolbarButton("✎", "Edit HashBang pattern");

    private final Button associateBtn = new Button("Associate File Types with Lumina...");
    private final Button helpBtn = new Button("?");

    // Ignored View controls
    private final TextArea ignoredPatternsArea = new TextArea();

    // Working state & original snapshot
    private final ObservableList<FileType> workingFileTypes = FXCollections.observableArrayList();
    private final List<FileType> originalFileTypes = new ArrayList<>();
    private String workingIgnoredPatterns = "";
    private String originalIgnoredPatterns = "";

    private Runnable onModifiedListener;

    public SettingsFileTypesPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        initSegmentedHeader();
        initViews();
        loadDataFromManager();
    }

    private void initSegmentedHeader() {
        HBox toggleRow = new HBox(0);
        toggleRow.setAlignment(Pos.CENTER_LEFT);

        recognizedBtn.setToggleGroup(viewGroup);
        ignoredBtn.setToggleGroup(viewGroup);

        styleSegmentButton(recognizedBtn, true);
        styleSegmentButton(ignoredBtn, false);

        recognizedBtn.setSelected(true);

        recognizedBtn.setOnAction(e -> {
            recognizedBtn.setSelected(true);
            showView(0);
        });
        ignoredBtn.setOnAction(e -> {
            ignoredBtn.setSelected(true);
            showView(1);
        });

        toggleRow.getChildren().addAll(recognizedBtn, ignoredBtn);
        getChildren().add(toggleRow);
    }

    private void styleSegmentButton(ToggleButton btn, boolean isFirst) {
        String baseRadius = isFirst ? "4 0 0 4" : "0 4 4 0";
        String borderSide = isFirst ? "-fx-border-color: #393B40 #2B2D30 #393B40 #393B40;" : "-fx-border-color: #393B40 #393B40 #393B40 transparent;";

        btn.setStyle(
                "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; " +
                borderSide + " -fx-border-width: 1; -fx-background-radius: " + baseRadius + "; " +
                "-fx-border-radius: " + baseRadius + "; -fx-padding: 4 14; -fx-cursor: hand;"
        );

        btn.selectedProperty().addListener((obs, old, isSel) -> {
            if (isSel) {
                btn.setStyle(
                        "-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; " +
                        borderSide + " -fx-border-width: 1; -fx-background-radius: " + baseRadius + "; " +
                        "-fx-border-radius: " + baseRadius + "; -fx-padding: 4 14; -fx-cursor: hand;"
                );
            } else {
                btn.setStyle(
                        "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; " +
                        borderSide + " -fx-border-width: 1; -fx-background-radius: " + baseRadius + "; " +
                        "-fx-border-radius: " + baseRadius + "; -fx-padding: 4 14; -fx-cursor: hand;"
                );
            }
        });
    }

    private void initViews() {
        VBox recognizedView = buildRecognizedView();
        VBox ignoredView = buildIgnoredView();

        VBox.setVgrow(recognizedView, Priority.ALWAYS);
        VBox.setVgrow(ignoredView, Priority.ALWAYS);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        contentStack.getChildren().addAll(recognizedView, ignoredView);
        showView(0);

        getChildren().add(contentStack);
    }

    private void showView(int index) {
        for (int i = 0; i < contentStack.getChildren().size(); i++) {
            javafx.scene.Node node = contentStack.getChildren().get(i);
            boolean active = (i == index);
            node.setVisible(active);
            node.setManaged(active);
        }
    }

    private VBox buildRecognizedView() {
        VBox view = new VBox(12);
        VBox.setVgrow(view, Priority.ALWAYS);

        // Horizontal split: Left list & Right panels
        HBox mainContent = new HBox(16);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // ---- Left: File Types List ----
        VBox leftBox = new VBox(6);
        leftBox.setPrefWidth(300);
        leftBox.setMinWidth(250);
        VBox.setVgrow(fileTypeListView, Priority.ALWAYS);

        HBox leftHeader = new HBox(8);
        leftHeader.setAlignment(Pos.CENTER_LEFT);
        Label fileTypesLabel = new Label("Recognized File Types:");
        fileTypesLabel.setStyle("-fx-text-fill: #9DA0A8; -fx-font-size: 12px;");
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        HBox leftToolbar = new HBox(2, addFileTypeBtn, removeFileTypeBtn, editFileTypeBtn);
        leftToolbar.setAlignment(Pos.CENTER_RIGHT);

        leftHeader.getChildren().addAll(fileTypesLabel, leftSpacer, leftToolbar);

        fileTypeListView.setItems(workingFileTypes);
        fileTypeListView.setStyle(
                "-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        fileTypeListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FileType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getName());
                    setGraphic(FileTypeIcon.getIcon(item.getIconKind(), 16));
                    setGraphicTextGap(8);
                    setStyle(
                            "-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 6; -fx-background-color: transparent;"
                    );
                }
            }
        });

        fileTypeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            updatePatternsAndHashbangs(selected);
            updateToolbarState(selected);
        });

        addFileTypeBtn.setOnAction(e -> handleAddFileType());
        removeFileTypeBtn.setOnAction(e -> handleRemoveFileType());
        editFileTypeBtn.setOnAction(e -> handleEditFileType());

        leftBox.getChildren().addAll(leftHeader, fileTypeListView);

        // ---- Right: Patterns & Hashbangs ----
        VBox rightBox = new VBox(12);
        HBox.setHgrow(rightBox, Priority.ALWAYS);

        // Top panel: File name patterns
        VBox patternsPanel = new VBox(6);
        VBox.setVgrow(patternsPanel, Priority.ALWAYS);

        HBox patternsHeader = new HBox(8);
        patternsHeader.setAlignment(Pos.CENTER_LEFT);
        Label patternsLabel = new Label("File name patterns:");
        patternsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Region patSpacer = new Region();
        HBox.setHgrow(patSpacer, Priority.ALWAYS);
        HBox patternsToolbar = new HBox(2, addPatternBtn, removePatternBtn, editPatternBtn);
        patternsToolbar.setAlignment(Pos.CENTER_RIGHT);
        patternsHeader.getChildren().addAll(patternsLabel, patSpacer, patternsToolbar);

        patternListView.setStyle(
                "-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        VBox.setVgrow(patternListView, Priority.ALWAYS);
        patternListView.setPlaceholder(createPlaceholderLabel("No registered file patterns"));
        patternListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 6;");
                }
            }
        });

        addPatternBtn.setOnAction(e -> handleAddPattern());
        removePatternBtn.setOnAction(e -> handleRemovePattern());
        editPatternBtn.setOnAction(e -> handleEditPattern());

        patternsPanel.getChildren().addAll(patternsHeader, patternListView);

        // Bottom panel: HashBang patterns
        VBox hashbangPanel = new VBox(6);
        VBox.setVgrow(hashbangPanel, Priority.ALWAYS);

        HBox hashbangHeader = new HBox(8);
        hashbangHeader.setAlignment(Pos.CENTER_LEFT);
        Label hashbangLabel = new Label("HashBang patterns:");
        hashbangLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        Region hbSpacer = new Region();
        HBox.setHgrow(hbSpacer, Priority.ALWAYS);
        HBox hashbangToolbar = new HBox(2, addHashbangBtn, removeHashbangBtn, editHashbangBtn);
        hashbangToolbar.setAlignment(Pos.CENTER_RIGHT);
        hashbangHeader.getChildren().addAll(hashbangLabel, hbSpacer, hashbangToolbar);

        hashbangListView.setStyle(
                "-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        VBox.setVgrow(hashbangListView, Priority.ALWAYS);
        hashbangListView.setPlaceholder(createPlaceholderLabel("No registered file patterns"));
        hashbangListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 6;");
                }
            }
        });

        addHashbangBtn.setOnAction(e -> handleAddHashbang());
        removeHashbangBtn.setOnAction(e -> handleRemoveHashbang());
        editHashbangBtn.setOnAction(e -> handleEditHashbang());

        hashbangPanel.getChildren().addAll(hashbangHeader, hashbangListView);

        rightBox.getChildren().addAll(patternsPanel, hashbangPanel);

        mainContent.getChildren().addAll(leftBox, rightBox);

        // ---- Bottom Bar ----
        HBox bottomBar = new HBox(12);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(6, 0, 0, 0));

        associateBtn.setStyle(
                "-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; " +
                "-fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand;"
        );
        associateBtn.setOnAction(e -> handleAssociateButton());

        Region bSpacer = new Region();
        HBox.setHgrow(bSpacer, Priority.ALWAYS);

        helpBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-cursor: hand;"
        );
        helpBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("File Types Help");
            alert.setHeaderText("Recognized File Types");
            alert.setContentText("Configure filename patterns and HashBang patterns associated with each file type.\n" +
                    "Use the Ignored Files and Folders tab to exclude files matching patterns from the project.");
            alert.showAndWait();
        });

        bottomBar.getChildren().addAll(associateBtn, bSpacer, helpBtn);

        view.getChildren().addAll(mainContent, bottomBar);
        return view;
    }

    private VBox buildIgnoredView() {
        VBox view = new VBox(12);
        VBox.setVgrow(view, Priority.ALWAYS);

        Label desc = new Label(
                "Files and folders whose names match the patterns below are completely ignored by the IDE and are not shown in the Project tool window. Semicolon-separated list of wildcard patterns (* and ? are allowed):"
        );
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #9DA0A8; -fx-font-size: 12px; -fx-line-spacing: 2px;");

        ignoredPatternsArea.setStyle(
                "-fx-control-inner-background: #1E1F22; -fx-background-color: #1E1F22; " +
                "-fx-text-fill: #DFE1E5; -fx-font-family: 'JetBrains Mono', Consolas, monospace; " +
                "-fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 8;"
        );
        ignoredPatternsArea.setWrapText(true);
        ignoredPatternsArea.setPrefHeight(200);
        VBox.setVgrow(ignoredPatternsArea, Priority.ALWAYS);

        ignoredPatternsArea.textProperty().addListener((obs, oldVal, newVal) -> {
            workingIgnoredPatterns = newVal != null ? newVal : "";
            notifyModified();
        });

        view.getChildren().addAll(desc, ignoredPatternsArea);
        return view;
    }

    private Label createPlaceholderLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px; -fx-font-style: italic;");
        return label;
    }

    private Button createToolbarButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; " +
                "-fx-padding: 2 6; -fx-cursor: hand;"
        );
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #393B40; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-padding: 2 6; -fx-cursor: hand; -fx-background-radius: 3;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; " +
                "-fx-padding: 2 6; -fx-cursor: hand;"
        ));
        return btn;
    }

    private void updatePatternsAndHashbangs(FileType selected) {
        if (selected == null) {
            patternListView.getItems().clear();
            hashbangListView.getItems().clear();
            return;
        }

        patternListView.getItems().setAll(selected.getPatterns());
        hashbangListView.getItems().setAll(selected.getHashbangs());
    }

    private void updateToolbarState(FileType selected) {
        boolean hasSelected = (selected != null);
        boolean isCustom = (hasSelected && !selected.isBuiltin());

        removeFileTypeBtn.setDisable(!isCustom);
        editFileTypeBtn.setDisable(!isCustom);

        addPatternBtn.setDisable(!hasSelected);
        removePatternBtn.setDisable(!hasSelected);
        editPatternBtn.setDisable(!hasSelected);

        addHashbangBtn.setDisable(!hasSelected);
        removeHashbangBtn.setDisable(!hasSelected);
        editHashbangBtn.setDisable(!hasSelected);
    }

    private void handleAddFileType() {
        NewFileTypeDialog.show(getScene() != null ? getScene().getWindow() : null, null).ifPresent(newFt -> {
            workingFileTypes.add(newFt);
            fileTypeListView.getSelectionModel().select(newFt);
            fileTypeListView.scrollTo(newFt);
            notifyModified();
        });
    }

    private void handleRemoveFileType() {
        FileType selected = fileTypeListView.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isBuiltin()) {
            int idx = fileTypeListView.getSelectionModel().getSelectedIndex();
            workingFileTypes.remove(selected);
            if (!workingFileTypes.isEmpty()) {
                int next = Math.min(idx, workingFileTypes.size() - 1);
                fileTypeListView.getSelectionModel().select(next);
            }
            notifyModified();
        }
    }

    private void handleEditFileType() {
        FileType selected = fileTypeListView.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isBuiltin()) {
            NewFileTypeDialog.show(getScene() != null ? getScene().getWindow() : null, selected).ifPresent(updated -> {
                int idx = workingFileTypes.indexOf(selected);
                if (idx >= 0) {
                    workingFileTypes.set(idx, updated);
                    fileTypeListView.getSelectionModel().select(updated);
                }
                notifyModified();
            });
        }
    }

    private void handleAddPattern() {
        FileType selected = fileTypeListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        AddWildcardDialog.show(getScene() != null ? getScene().getWindow() : null, "").ifPresent(pat -> {
            if (!selected.getPatterns().contains(pat)) {
                selected.getPatterns().add(pat);
                patternListView.getItems().setAll(selected.getPatterns());
                patternListView.getSelectionModel().select(pat);
                notifyModified();
            }
        });
    }

    private void handleRemovePattern() {
        FileType selected = fileTypeListView.getSelectionModel().getSelectedItem();
        String pat = patternListView.getSelectionModel().getSelectedItem();
        if (selected != null && pat != null) {
            selected.getPatterns().remove(pat);
            patternListView.getItems().setAll(selected.getPatterns());
            notifyModified();
        }
    }

    private void handleEditPattern() {
        FileType selected = fileTypeListView.getSelectionModel().getSelectedItem();
        String pat = patternListView.getSelectionModel().getSelectedItem();
        if (selected != null && pat != null) {
            AddWildcardDialog.show(getScene() != null ? getScene().getWindow() : null, pat).ifPresent(newPat -> {
                int idx = selected.getPatterns().indexOf(pat);
                if (idx >= 0) {
                    selected.getPatterns().set(idx, newPat);
                    patternListView.getItems().setAll(selected.getPatterns());
                    patternListView.getSelectionModel().select(newPat);
                    notifyModified();
                }
            });
        }
    }

    private void handleAddHashbang() {
        FileType selected = fileTypeListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        AddHashBangDialog.show(getScene() != null ? getScene().getWindow() : null, "").ifPresent(hb -> {
            if (!selected.getHashbangs().contains(hb)) {
                selected.getHashbangs().add(hb);
                hashbangListView.getItems().setAll(selected.getHashbangs());
                hashbangListView.getSelectionModel().select(hb);
                notifyModified();
            }
        });
    }

    private void handleRemoveHashbang() {
        FileType selected = fileTypeListView.getSelectionModel().getSelectedItem();
        String hb = hashbangListView.getSelectionModel().getSelectedItem();
        if (selected != null && hb != null) {
            selected.getHashbangs().remove(hb);
            hashbangListView.getItems().setAll(selected.getHashbangs());
            notifyModified();
        }
    }

    private void handleEditHashbang() {
        FileType selected = fileTypeListView.getSelectionModel().getSelectedItem();
        String hb = hashbangListView.getSelectionModel().getSelectedItem();
        if (selected != null && hb != null) {
            AddHashBangDialog.show(getScene() != null ? getScene().getWindow() : null, hb).ifPresent(newHb -> {
                int idx = selected.getHashbangs().indexOf(hb);
                if (idx >= 0) {
                    selected.getHashbangs().set(idx, newHb);
                    hashbangListView.getItems().setAll(selected.getHashbangs());
                    hashbangListView.getSelectionModel().select(newHb);
                    notifyModified();
                }
            });
        }
    }

    private void handleAssociateButton() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Associate File Types");
        alert.setHeaderText("File Type Associations");
        alert.setContentText("Lumina has registered system file associations with all active patterns.");
        alert.showAndWait();
    }

    private void loadDataFromManager() {
        FileTypeManager manager = FileTypeManager.getInstance();

        workingFileTypes.clear();
        originalFileTypes.clear();
        for (FileType ft : manager.getFileTypes()) {
            FileType copy = ft.copy();
            workingFileTypes.add(copy);
            originalFileTypes.add(ft.copy());
        }

        workingIgnoredPatterns = manager.getIgnoredPatterns();
        originalIgnoredPatterns = workingIgnoredPatterns;
        ignoredPatternsArea.setText(workingIgnoredPatterns);

        if (!workingFileTypes.isEmpty()) {
            fileTypeListView.getSelectionModel().select(0);
        }
    }

    public boolean isModified() {
        if (!Objects.equals(workingIgnoredPatterns, originalIgnoredPatterns)) {
            return true;
        }

        if (workingFileTypes.size() != originalFileTypes.size()) {
            return true;
        }

        for (int i = 0; i < workingFileTypes.size(); i++) {
            if (!workingFileTypes.get(i).isEquivalentTo(originalFileTypes.get(i))) {
                return true;
            }
        }

        return false;
    }

    public void apply() {
        FileTypeManager manager = FileTypeManager.getInstance();

        // Save into manager
        manager.getFileTypes().setAll(workingFileTypes);
        manager.setIgnoredPatterns(workingIgnoredPatterns);
        manager.save();

        // Update original snapshot
        originalFileTypes.clear();
        for (FileType ft : workingFileTypes) {
            originalFileTypes.add(ft.copy());
        }
        originalIgnoredPatterns = workingIgnoredPatterns;

        notifyModified();
    }

    public void reset() {
        loadDataFromManager();
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void addPatternToFileType(FileType fileType, String pattern) {
        if (fileType != null && pattern != null && !fileType.getPatterns().contains(pattern)) {
            fileType.getPatterns().add(pattern);
            if (fileType.equals(fileTypeListView.getSelectionModel().getSelectedItem())) {
                patternListView.getItems().setAll(fileType.getPatterns());
            }
            notifyModified();
        }
    }

    public ListView<FileType> getFileTypeListView() {
        return fileTypeListView;
    }

    public ListView<String> getPatternListView() {
        return patternListView;
    }

    public ListView<String> getHashbangListView() {
        return hashbangListView;
    }

    public TextArea getIgnoredPatternsArea() {
        return ignoredPatternsArea;
    }

    public ObservableList<FileType> getWorkingFileTypes() {
        return workingFileTypes;
    }
}