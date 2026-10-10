package dev.lumina.ui;

import dev.lumina.xslt.XsltFileAssociation;
import dev.lumina.xslt.XsltFileAssociationsSettings;
import dev.lumina.xslt.XsltFileAssociationsSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;

/**
 * Settings page for Languages & Frameworks > XSLT File Associations.
 * Matches reference screenshots media_1791604079444_011b2914.png & media_1791604099408_0878138e.png:
 *  - Left Pane: Project XSLT Files: [TreeView of project directory files]
 *  - Right Pane: Associated Files: [ + ] [ - ], ListView with "No associated files" placeholder
 */
public class SettingsLanguagesXsltFileAssociationsPage extends VBox {

    private final XsltFileAssociationsSettingsManager manager = XsltFileAssociationsSettingsManager.getInstance();

    private final TreeView<FileNode> projectFilesTree = new TreeView<>();
    private final ListView<String> associatedFilesList = new ListView<>();
    private final ObservableList<String> associatedData = FXCollections.observableArrayList();

    private final Button addBtn = new Button();
    private final Button removeBtn = new Button();

    private final Map<String, List<String>> currentWorkingMap = new HashMap<>();

    private XsltFileAssociationsSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;
    private String selectedFilePath = null;

    public static class FileNode {
        private final File file;
        private final String displayName;

        public FileNode(File file, String displayName) {
            this.file = file;
            this.displayName = displayName;
        }

        public File getFile() {
            return file;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public SettingsLanguagesXsltFileAssociationsPage() {
        setSpacing(10);
        setPadding(new Insets(14, 20, 16, 20));
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // --- Left Pane: Project XSLT Files ---
        VBox leftPane = new VBox(6);
        leftPane.setPadding(new Insets(0, 8, 0, 0));
        VBox.setVgrow(leftPane, Priority.ALWAYS);

        Label leftHeader = new Label("Project XSLT Files:");
        leftHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        buildProjectFilesTree();
        VBox.setVgrow(projectFilesTree, Priority.ALWAYS);

        leftPane.getChildren().addAll(leftHeader, projectFilesTree);

        // --- Right Pane: Associated Files ---
        VBox rightPane = new VBox(6);
        rightPane.setPadding(new Insets(0, 0, 0, 8));
        VBox.setVgrow(rightPane, Priority.ALWAYS);

        HBox rightHeaderBox = new HBox(8);
        rightHeaderBox.setAlignment(Pos.CENTER_LEFT);

        Label rightHeader = new Label("Associated Files:");
        rightHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox toolbar = buildToolbar();
        rightHeaderBox.getChildren().addAll(rightHeader, toolbar);

        buildAssociatedFilesList();
        VBox.setVgrow(associatedFilesList, Priority.ALWAYS);

        rightPane.getChildren().addAll(rightHeaderBox, associatedFilesList);

        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.48);

        getChildren().add(splitPane);
    }

    private void buildProjectFilesTree() {
        File projectDir = new File(System.getProperty("user.dir", "."));
        TreeItem<FileNode> rootItem = buildTreeItem(projectDir, true);
        rootItem.setExpanded(true);

        projectFilesTree.setRoot(rootItem);
        projectFilesTree.setShowRoot(true);
        projectFilesTree.setStyle(
                "-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; " +
                "-fx-font-size: 13px; -fx-text-fill: #DFE1E5;"
        );

        projectFilesTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(FileNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                    SVGPath icon = new SVGPath();
                    if (item.getFile() != null && item.getFile().isDirectory()) {
                        icon.setContent("M 2 3 L 6 3 L 7.5 5 L 14 5 L 14 13 L 2 13 Z");
                        icon.setFill(Color.web("#6F737A"));
                    } else {
                        icon.setContent("M 3 2 L 10 2 L 13 5 L 13 14 L 3 14 Z M 9 2 L 9 6 L 13 6");
                        icon.setFill(Color.TRANSPARENT);
                        icon.setStroke(Color.web("#AFB1B6"));
                        icon.setStrokeWidth(1.0);
                    }
                    setGraphic(icon);
                }
            }
        });

        projectFilesTree.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.getValue() != null && newV.getValue().getFile() != null) {
                File f = newV.getValue().getFile();
                selectedFilePath = f.getAbsolutePath();
                addBtn.setDisable(false);
                displayAssociationsFor(selectedFilePath);
            } else {
                selectedFilePath = null;
                addBtn.setDisable(true);
                associatedData.clear();
            }
        });
    }

    private TreeItem<FileNode> buildTreeItem(File file, boolean isRoot) {
        String label = isRoot ? file.getName() + "  ~" + file.getAbsolutePath() : file.getName();
        TreeItem<FileNode> item = new TreeItem<>(new FileNode(file, label));
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                Arrays.sort(files, (a, b) -> {
                    if (a.isDirectory() != b.isDirectory()) {
                        return a.isDirectory() ? -1 : 1;
                    }
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                for (File child : files) {
                    if (!child.getName().startsWith(".git") && !child.getName().equals("target")) {
                        item.getChildren().add(buildTreeItem(child, false));
                    }
                }
            }
        }
        return item;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);

        // Plus
        SVGPath plus = new SVGPath();
        plus.setContent("M 6 2 L 6 10 M 2 6 L 10 6");
        styleSvg(plus);
        addBtn.setGraphic(plus);
        styleToolBtn(addBtn, "Add Associated File");
        addBtn.setDisable(true);
        addBtn.setOnAction(e -> onAddAssociatedFile());

        // Minus
        SVGPath minus = new SVGPath();
        minus.setContent("M 2 6 L 10 6");
        styleSvg(minus);
        removeBtn.setGraphic(minus);
        styleToolBtn(removeBtn, "Remove");
        removeBtn.setDisable(true);
        removeBtn.setOnAction(e -> onRemoveAssociatedFile());

        bar.getChildren().addAll(addBtn, removeBtn);
        return bar;
    }

    private void styleSvg(SVGPath p) {
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web("#AFB1B6"));
        p.setStrokeWidth(1.2);
    }

    private void styleToolBtn(Button btn, String tooltipText) {
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 6 3 6; -fx-cursor: hand;");
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisable()) btn.setStyle("-fx-background-color: #35373B; -fx-padding: 3 6 3 6; -fx-background-radius: 4; -fx-cursor: hand;");
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisable()) btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 6 3 6; -fx-cursor: hand;");
        });
    }

    private void buildAssociatedFilesList() {
        associatedFilesList.setItems(associatedData);
        associatedFilesList.setStyle(
                "-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; " +
                "-fx-font-size: 13px; -fx-text-fill: #DFE1E5;"
        );

        Label placeholder = new Label("No associated files");
        placeholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        associatedFilesList.setPlaceholder(placeholder);

        associatedFilesList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            removeBtn.setDisable(newV == null);
        });
    }

    private void displayAssociationsFor(String path) {
        associatedData.clear();
        List<String> files = currentWorkingMap.get(path);
        if (files != null) {
            associatedData.addAll(files);
        }
    }

    private void onAddAssociatedFile() {
        if (selectedFilePath == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Associated File (XML / Schema / XSLT)");
        File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (f != null) {
            String path = f.getAbsolutePath();
            List<String> list = currentWorkingMap.computeIfAbsent(selectedFilePath, k -> new ArrayList<>());
            if (!list.contains(path)) {
                list.add(path);
                associatedData.add(path);
                fireModified();
            }
        }
    }

    private void onRemoveAssociatedFile() {
        if (selectedFilePath == null) return;
        String selected = associatedFilesList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            List<String> list = currentWorkingMap.get(selectedFilePath);
            if (list != null) {
                list.remove(selected);
                associatedData.remove(selected);
                fireModified();
            }
        }
    }

    public void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(XsltFileAssociationsSettings s) {
        currentWorkingMap.clear();
        if (s != null) {
            for (XsltFileAssociation a : s.getAssociations()) {
                currentWorkingMap.put(a.getXsltFilePath(), new ArrayList<>(a.getAssociatedFiles()));
            }
        }
        if (selectedFilePath != null) {
            displayAssociationsFor(selectedFilePath);
        } else {
            associatedData.clear();
        }
    }

    public XsltFileAssociationsSettings getCurrentSettingsFromUI() {
        XsltFileAssociationsSettings s = new XsltFileAssociationsSettings();
        List<XsltFileAssociation> list = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : currentWorkingMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                list.add(new XsltFileAssociation(entry.getKey(), entry.getValue()));
            }
        }
        s.setAssociations(list);
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        XsltFileAssociationsSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
        fireModified();
    }

    public void reset() {
        loadData();
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Getters for inspection and testing
    public TreeView<FileNode> getProjectFilesTree() {
        return projectFilesTree;
    }

    public ListView<String> getAssociatedFilesList() {
        return associatedFilesList;
    }

    public ObservableList<String> getAssociatedData() {
        return associatedData;
    }

    public Map<String, List<String>> getCurrentWorkingMap() {
        return currentWorkingMap;
    }

    public Button getAddBtn() {
        return addBtn;
    }

    public Button getRemoveBtn() {
        return removeBtn;
    }
}
