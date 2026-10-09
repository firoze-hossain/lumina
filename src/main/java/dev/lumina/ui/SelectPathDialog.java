package dev.lumina.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Modal path selector dialog for custom dictionaries and file/directory paths.
 * Faithfully matches reference IDE screenshot (Image 3).
 */
public class SelectPathDialog {

    private final Stage stage;
    private final TextField pathField = new TextField();
    private final TreeView<FileItem> treeView = new TreeView<>();
    private final Button okBtn = new Button("OK");
    private final Button cancelBtn = new Button("Cancel");
    private final Hyperlink togglePathLink = new Hyperlink("Hide path");
    private final HBox pathFieldBox = new HBox(4);

    private String result = null;
    private File currentSelectedFile = null;

    public record FileItem(File file, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    public SelectPathDialog(Window owner, String initialPath) {
        stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Select Path");
        stage.setWidth(480);
        stage.setHeight(440);
        stage.setMinWidth(400);
        stage.setMinHeight(340);

        File startDir = null;
        if (initialPath != null && !initialPath.trim().isEmpty()) {
            File f = new File(initialPath.trim());
            if (f.exists()) {
                startDir = f.isDirectory() ? f : f.getParentFile();
                currentSelectedFile = f;
            }
        }
        if (startDir == null) {
            startDir = new File(System.getProperty("user.dir", "."));
            currentSelectedFile = startDir;
        }

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1E1F22;");
        root.setPadding(new Insets(10, 14, 12, 14));

        // 1. Top Section: Toolbar and Path Input
        VBox topBox = new VBox(6);
        topBox.setPadding(new Insets(0, 0, 8, 0));

        HBox toolbar = buildToolbar();
        buildPathFieldBox(currentSelectedFile.getAbsolutePath());

        topBox.getChildren().addAll(toolbar, pathFieldBox);
        root.setTop(topBox);

        // 2. Center Section: TreeView + Drag & Drop label
        VBox centerBox = new VBox(4);
        VBox.setVgrow(centerBox, Priority.ALWAYS);

        setupTreeView(startDir);
        VBox.setVgrow(treeView, Priority.ALWAYS);

        Label dragDropHint = new Label("Drag and drop a file into the space above to quickly locate it");
        dragDropHint.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px; -fx-padding: 2 0 0 2;");

        centerBox.getChildren().addAll(treeView, dragDropHint);
        root.setCenter(centerBox);

        // 3. Bottom Section: ? Help and OK / Cancel Buttons
        HBox bottomBar = new HBox(8);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));

        Button helpBtn = new Button("?");
        helpBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #4E5157; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-cursor: hand; -fx-min-width: 22px; -fx-min-height: 22px; -fx-max-width: 22px; -fx-max-height: 22px; -fx-padding: 0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 16; -fx-cursor: hand;");
        okBtn.setOnAction(e -> handleOk());

        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        bottomBar.getChildren().addAll(helpBtn, spacer, okBtn, cancelBtn);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root);
        try {
            var css = getClass().getResource("/css/lumina-dark.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {}
        stage.setScene(scene);
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);

        Button homeBtn = createToolButton("🏠", "Home");
        homeBtn.setOnAction(e -> navigateTo(new File(System.getProperty("user.home"))));

        Button upBtn = createToolButton("⬆", "Up");
        upBtn.setOnAction(e -> {
            if (currentSelectedFile != null && currentSelectedFile.getParentFile() != null) {
                navigateTo(currentSelectedFile.getParentFile());
            }
        });

        Button newFolderBtn = createToolButton("📁+", "New Folder");
        newFolderBtn.setOnAction(e -> handleCreateNewFolder());

        Button deleteBtn = createToolButton("🗑", "Delete");
        deleteBtn.setOnAction(e -> handleDeleteSelected());

        Button refreshBtn = createToolButton("🔄", "Refresh");
        refreshBtn.setOnAction(e -> {
            if (currentSelectedFile != null) {
                setupTreeView(currentSelectedFile.isDirectory() ? currentSelectedFile : currentSelectedFile.getParentFile());
            }
        });

        Button collapseBtn = createToolButton("⇤", "Collapse All");
        collapseBtn.setOnAction(e -> collapseAll(treeView.getRoot()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        togglePathLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-border-color: transparent; -fx-padding: 0;");
        togglePathLink.setOnAction(e -> {
            boolean visible = !pathFieldBox.isVisible();
            pathFieldBox.setVisible(visible);
            pathFieldBox.setManaged(visible);
            togglePathLink.setText(visible ? "Hide path" : "Show path");
        });

        bar.getChildren().addAll(homeBtn, upBtn, newFolderBtn, deleteBtn, refreshBtn, collapseBtn, spacer, togglePathLink);
        return bar;
    }

    private Button createToolButton(String icon, String tooltip) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C919D; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6; -fx-border-color: transparent;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6; -fx-border-color: transparent; -fx-background-radius: 3;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C919D; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6; -fx-border-color: transparent;"));
        return btn;
    }

    private void buildPathFieldBox(String initialPath) {
        pathFieldBox.setAlignment(Pos.CENTER_LEFT);
        pathFieldBox.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 2 6;");

        pathField.setText(initialPath);
        pathField.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 2 0;");
        HBox.setHgrow(pathField, Priority.ALWAYS);

        Label dropdownArrow = new Label("▼");
        dropdownArrow.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 9px; -fx-padding: 0 2 0 4; -fx-cursor: hand;");

        pathField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                File target = new File(pathField.getText().trim());
                if (target.exists()) {
                    navigateTo(target);
                }
            }
        });

        pathFieldBox.getChildren().addAll(pathField, dropdownArrow);
    }

    private void setupTreeView(File rootDir) {
        File displayRoot = rootDir;
        if (displayRoot.getParentFile() != null && displayRoot.getParentFile().getParentFile() != null) {
            displayRoot = displayRoot.getParentFile().getParentFile();
        }

        TreeItem<FileItem> rootItem = createLazyNode(displayRoot);
        rootItem.setExpanded(true);
        treeView.setRoot(rootItem);
        treeView.setShowRoot(true);

        treeView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(FileItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    String icon = item.file().isDirectory() ? "📁 " : "📄 ";
                    setText(icon + item.displayName());
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-padding: 2 4;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 2 4;");
                    }
                }
            }
        });

        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                handleOk();
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.getValue() != null) {
                currentSelectedFile = newV.getValue().file();
                pathField.setText(currentSelectedFile.getAbsolutePath());
            }
        });

        // Drag and drop support
        treeView.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        treeView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                File dropped = db.getFiles().get(0);
                navigateTo(dropped);
                event.setDropCompleted(true);
            }
            event.consume();
        });
    }

    private TreeItem<FileItem> createLazyNode(File file) {
        String name = file.getName().isEmpty() ? file.getAbsolutePath() : file.getName();
        TreeItem<FileItem> item = new TreeItem<>(new FileItem(file, name)) {
            private boolean isFirstChildren = true;

            @Override
            public boolean isLeaf() {
                return !file.isDirectory();
            }

            @Override
            public ObservableList<TreeItem<FileItem>> getChildren() {
                if (isFirstChildren) {
                    isFirstChildren = false;
                    loadChildren(this);
                }
                return super.getChildren();
            }
        };
        return item;
    }

    private void loadChildren(TreeItem<FileItem> parent) {
        File dir = parent.getValue().file();
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.sort(files, Comparator.comparing((File f) -> !f.isDirectory()).thenComparing(File::getName));
                for (File child : files) {
                    if (child.getName().startsWith(".")) continue; // skip hidden by default
                    parent.getChildren().add(createLazyNode(child));
                }
            }
        }
    }

    private void collapseAll(TreeItem<FileItem> item) {
        if (item != null) {
            item.setExpanded(false);
            for (TreeItem<FileItem> child : item.getChildren()) {
                collapseAll(child);
            }
        }
    }

    private void navigateTo(File file) {
        currentSelectedFile = file;
        pathField.setText(file.getAbsolutePath());
        setupTreeView(file.isDirectory() ? file : file.getParentFile());
    }

    private void handleCreateNewFolder() {
        TextInputDialog dialog = new TextInputDialog("new_folder");
        dialog.initOwner(stage);
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Enter directory name:");
        dialog.showAndWait().ifPresent(name -> {
            File parentDir = currentSelectedFile != null && currentSelectedFile.isDirectory()
                    ? currentSelectedFile : (currentSelectedFile != null ? currentSelectedFile.getParentFile() : new File("."));
            File newFolder = new File(parentDir, name);
            if (newFolder.mkdirs()) {
                navigateTo(newFolder);
            }
        });
    }

    private void handleDeleteSelected() {
        if (currentSelectedFile != null && currentSelectedFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + currentSelectedFile.getName() + "?", ButtonType.YES, ButtonType.NO);
            alert.initOwner(stage);
            alert.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    currentSelectedFile.delete();
                    if (currentSelectedFile.getParentFile() != null) {
                        navigateTo(currentSelectedFile.getParentFile());
                    }
                }
            });
        }
    }

    private void handleOk() {
        String entered = pathField.getText().trim();
        if (!entered.isEmpty()) {
            result = entered;
            stage.close();
        } else if (currentSelectedFile != null) {
            result = currentSelectedFile.getAbsolutePath();
            stage.close();
        }
    }

    public String showAndWait() {
        stage.showAndWait();
        return result;
    }

    public TextField getPathField() {
        return pathField;
    }

    public TreeView<FileItem> getTreeView() {
        return treeView;
    }

    public Button getOkBtn() {
        return okBtn;
    }

    public Button getCancelBtn() {
        return cancelBtn;
    }
}
