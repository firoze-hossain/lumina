package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * IntelliJ IDEA-styled Commit tool window:
 * - Changes & Unversioned Files grouped TreeView with tri-state category checkboxes
 * - Colored file status indicators (Modified=cyan, Untracked/Added=green, Deleted=red/strikethrough)
 * - File type icons (.java, .yml, .xml, .gitignore, document)
 * - Professional toolbar: Refresh, Rollback (Ctrl+Alt+Z), Eye (Group By Directory, Show Ignored), Expand All, Collapse All
 * - Commit message toolbar with Amend (auto-loads previous commit) & Recent commit messages (🕒)
 * - Clear, readable commit message placeholder text
 * - Commit (Primary blue) & Commit and Push... buttons with dynamic validation & count badges
 * - Double-click / Enter to jump to source in the editor
 */
public final class CommitPanel extends VBox {

    public enum ChangeType {
        MODIFIED, ADDED, DELETED, RENAMED, UNTRACKED, IGNORED
    }

    public static final class FileItem {
        private final String relativePath;
        private final String fileName;
        private final String directoryPath;
        private final ChangeType changeType;
        private final boolean isUnversioned;
        private final boolean isIgnored;
        private final BooleanProperty selected;

        public FileItem(String relativePath, ChangeType changeType, boolean isUnversioned, boolean isIgnored, boolean defaultSelected) {
            this.relativePath = relativePath;
            this.changeType = changeType;
            this.isUnversioned = isUnversioned;
            this.isIgnored = isIgnored;
            int idx = relativePath.lastIndexOf('/');
            if (idx >= 0) {
                this.fileName = relativePath.substring(idx + 1);
                this.directoryPath = relativePath.substring(0, idx);
            } else {
                this.fileName = relativePath;
                this.directoryPath = "";
            }
            this.selected = new SimpleBooleanProperty(defaultSelected);
        }

        public String getRelativePath() { return relativePath; }
        public String getFileName() { return fileName; }
        public String getDirectoryPath() { return directoryPath; }
        public ChangeType getChangeType() { return changeType; }
        public boolean isUnversioned() { return isUnversioned; }
        public boolean isIgnored() { return isIgnored; }
        public BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean val) { selected.set(val); }
    }

    public static final class CategoryItem {
        private final String name;
        private final ObservableList<FileItem> files = FXCollections.observableArrayList();
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        private final BooleanProperty indeterminate = new SimpleBooleanProperty(false);

        public CategoryItem(String name) {
            this.name = name;
        }

        public String getName() { return name; }
        public ObservableList<FileItem> getFiles() { return files; }
        public BooleanProperty selectedProperty() { return selected; }
        public BooleanProperty indeterminateProperty() { return indeterminate; }

        public void updateSelectionState() {
            if (files.isEmpty()) {
                selected.set(false);
                indeterminate.set(false);
                return;
            }
            long checkedCount = files.stream().filter(FileItem::isSelected).count();
            if (checkedCount == files.size()) {
                selected.set(true);
                indeterminate.set(false);
            } else if (checkedCount == 0) {
                selected.set(false);
                indeterminate.set(false);
            } else {
                selected.set(false);
                indeterminate.set(true);
            }
        }

        public void toggleAll(boolean check) {
            for (FileItem f : files) {
                f.setSelected(check);
            }
            selected.set(check);
            indeterminate.set(false);
        }
    }

    public static final class DirectoryGroup {
        private final String dirPath;
        private final List<FileItem> files = new ArrayList<>();

        public DirectoryGroup(String dirPath) {
            this.dirPath = dirPath;
        }

        public String getDirPath() { return dirPath; }
        public List<FileItem> getFiles() { return files; }
    }

    private final Supplier<Path> projectRoot;
    private final Consumer<String> log;
    private Consumer<Path> onOpenFile;

    private final CategoryItem changesCategory = new CategoryItem("Changes");
    private final CategoryItem unversionedCategory = new CategoryItem("Unversioned Files");
    private final CategoryItem ignoredCategory = new CategoryItem("Ignored Files");

    private boolean groupByDirectory = false;
    private boolean showIgnored = false;

    private final TreeView<Object> treeView = new TreeView<>();
    private final TextArea message = new TextArea();
    private final CheckBox amendCheck = new CheckBox("Amend");
    private final Button commitBtn = new Button("Commit");
    private final Button commitAndPushBtn = new Button("Commit and Push\u2026");
    private final Label status = new Label();

    private TreeItem<Object> changesTreeItem;
    private TreeItem<Object> unversionedTreeItem;
    private TreeItem<Object> ignoredTreeItem;

    public CommitPanel(Supplier<Path> projectRoot, Consumer<String> log) {
        this.projectRoot = projectRoot;
        this.log = log;

        getStyleClass().add("commit-panel");
        setStyle("-fx-background-color: #1E1F22;");
        setSpacing(6);
        setPadding(new Insets(6, 10, 10, 10));

        // 0. Top tabs matching IntelliJ Commit window header (Commit | Stash)
        Button commitTabBtn = new Button("Commit");
        Button stashTabBtn = new Button("Stash");
        commitTabBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-background-radius: 4; -fx-padding: 3 9 3 9; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;");
        stashTabBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 12px; -fx-padding: 3 9 3 9; -fx-cursor: hand;");
        stashTabBtn.setOnMouseEntered(e -> stashTabBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 9 3 9; -fx-cursor: hand; -fx-background-radius: 4;"));
        stashTabBtn.setOnMouseExited(e -> stashTabBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 12px; -fx-padding: 3 9 3 9; -fx-cursor: hand;"));
        stashTabBtn.setOnAction(e -> showStashMenu(stashTabBtn));
        commitTabBtn.setOnAction(e -> refresh());

        HBox topTabs = new HBox(4, commitTabBtn, stashTabBtn);
        topTabs.setAlignment(Pos.CENTER_LEFT);
        topTabs.setPadding(new Insets(0, 0, 2, 0));

        // 1. Toolbar matching IntelliJ Commit tool window
        Button refreshBtn = createToolbarIconButton(createRefreshIcon(), "Refresh (Ctrl+F5)");
        refreshBtn.setOnAction(e -> refresh());

        Button rollbackBtn = createToolbarIconButton(createRollbackIcon(), "Rollback\u2026 (Ctrl+Alt+Z)");
        rollbackBtn.setOnAction(e -> doRollback());

        Button shelveBtn = createToolbarIconButton(createShelveIcon(), "Shelve / Stash Changes\u2026");
        shelveBtn.setOnAction(e -> doStashOrShelve());

        Button diffPreviewBtn = createToolbarIconButton(createDiffPreviewIcon(), "Show Diff Preview");
        diffPreviewBtn.setOnAction(e -> {
            TreeItem<Object> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getValue() instanceof FileItem file) {
                openFileInEditor(file);
            }
        });

        Button eyeBtn = createToolbarIconButton(createEyeIcon(), "View Options");
        eyeBtn.setOnAction(e -> showOptionsMenu(eyeBtn));

        Button expandAllBtn = createToolbarIconButton(createExpandAllIcon(), "Expand All (Ctrl+NumPad +)");
        expandAllBtn.setOnAction(e -> expandAll(true));

        Button collapseAllBtn = createToolbarIconButton(createCollapseAllIcon(), "Collapse All (Ctrl+NumPad -)");
        collapseAllBtn.setOnAction(e -> expandAll(false));

        Region toolSpacer = new Region();
        HBox.setHgrow(toolSpacer, Priority.ALWAYS);

        HBox toolbar = new HBox(2, refreshBtn, rollbackBtn, shelveBtn, diffPreviewBtn, eyeBtn, expandAllBtn, collapseAllBtn, toolSpacer);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 0, 4, 0));

        // 2. TreeView setup
        treeView.setShowRoot(false);
        treeView.getStyleClass().add("commit-tree");
        treeView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else if (item instanceof CategoryItem cat) {
                    setGraphic(buildCategoryCell(cat));
                    setText(null);
                    setStyle("-fx-background-color: " + (isSelected() ? "#2E436E;" : "#1E1F22;"));
                } else if (item instanceof FileItem file) {
                    setGraphic(buildFileCell(file));
                    setText(null);
                    setStyle("-fx-background-color: " + (isSelected() ? "#2E436E;" : "#1E1F22;"));
                } else if (item instanceof DirectoryGroup dir) {
                    setGraphic(buildDirectoryCell(dir));
                    setText(null);
                    setStyle("-fx-background-color: " + (isSelected() ? "#2E436E;" : "#1E1F22;"));
                }
            }
        });

        // Context menu and double-click to open
        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<Object> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() instanceof FileItem file) {
                    openFileInEditor(file);
                }
            }
        });

        treeView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.F4) {
                TreeItem<Object> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() instanceof FileItem file) {
                    openFileInEditor(file);
                }
            } else if (e.isControlDown() && e.isAltDown() && e.getCode() == KeyCode.Z) {
                doRollback();
            } else if (e.isControlDown() && e.isAltDown() && e.getCode() == KeyCode.P) {
                groupByDirectory = !groupByDirectory;
                buildTree();
            }
        });

        ContextMenu treeContextMenu = new ContextMenu();
        treeContextMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5;");
        MenuItem jumpItem = new MenuItem("Jump to Source (F4)");
        jumpItem.setOnAction(e -> {
            TreeItem<Object> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getValue() instanceof FileItem file) {
                openFileInEditor(file);
            }
        });
        MenuItem rollbackCtxItem = new MenuItem("Rollback\u2026 (Ctrl+Alt+Z)");
        rollbackCtxItem.setOnAction(e -> doRollback());
        treeContextMenu.getItems().addAll(jumpItem, rollbackCtxItem);
        treeView.setContextMenu(treeContextMenu);

        // 3. Commit message mini-toolbar
        amendCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        amendCheck.selectedProperty().addListener((obs, old, isAmend) -> {
            if (isAmend) {
                Path dir = projectRoot.get();
                if (dir != null && GitService.isRepository(dir)) {
                    String lastMsg = GitService.lastCommitMessage(dir);
                    if (!lastMsg.isBlank()) {
                        message.setText(lastMsg);
                    }
                }
            }
            updateCommitButtonState();
        });

        Button recentBtn = createToolbarButton("\uD83D\uDD52", "Recent Commit Messages");
        recentBtn.setOnAction(e -> showRecentMessages(recentBtn));

        HBox messageToolbar = new HBox(8, amendCheck, recentBtn);
        messageToolbar.setAlignment(Pos.CENTER_LEFT);
        messageToolbar.setPadding(new Insets(4, 0, 2, 0));

        // 4. Commit message input
        message.setPromptText("Commit Message");
        message.setPrefRowCount(4);
        message.setWrapText(true);
        message.getStyleClass().add("commit-message");
        message.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #868A91; -fx-font-size: 12px;");
        message.textProperty().addListener((obs, old, text) -> updateCommitButtonState());

        // 5. Commit action buttons
        commitBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 5 16 5 16; -fx-background-radius: 4; -fx-cursor: hand;");
        commitBtn.setOnAction(e -> doCommit(false));

        commitAndPushBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5; -fx-padding: 5 14 5 14; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
        commitAndPushBtn.setOnAction(e -> doCommit(true));

        Button commitOptionsBtn = createToolbarButton("\u2699", "Commit Options");
        commitOptionsBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Commit Options");
            alert.setHeaderText("Git Commit Settings");
            alert.setContentText("Pre-commit checks:\n\u2022 Reformat code\n\u2022 Optimize imports\n\u2022 Check TODOs");
            alert.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
            alert.showAndWait();
        });

        Region btnSpacer = new Region();
        HBox.setHgrow(btnSpacer, Priority.ALWAYS);

        HBox buttonsRow = new HBox(8, commitBtn, commitAndPushBtn, btnSpacer, commitOptionsBtn);
        buttonsRow.setAlignment(Pos.CENTER_LEFT);
        buttonsRow.setPadding(new Insets(4, 0, 2, 0));

        status.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");
        status.setWrapText(true);

        getChildren().addAll(topTabs, toolbar, treeView, messageToolbar, message, buttonsRow, status);
    }

    public void setOnOpenFile(Consumer<Path> onOpenFile) {
        this.onOpenFile = onOpenFile;
    }

    private void openFileInEditor(FileItem file) {
        Path dir = projectRoot.get();
        if (dir != null && onOpenFile != null) {
            onOpenFile.accept(dir.resolve(file.getRelativePath()));
        }
    }

    /** Re-reads {@code git status} and rebuilds the tree. */
    public void refresh() {
        Path dir = projectRoot.get();
        changesCategory.getFiles().clear();
        unversionedCategory.getFiles().clear();
        ignoredCategory.getFiles().clear();

        if (dir == null || !GitService.isRepository(dir)) {
            status.setText("Not a git repository.");
            buildTree();
            updateCommitButtonState();
            return;
        }

        GitService.Result r = GitService.statusDetailed(dir, showIgnored);
        if (!r.ok()) {
            status.setText("git status failed: " + r.output().trim());
            buildTree();
            updateCommitButtonState();
            return;
        }

        for (String line : r.output().split("\\R")) {
            if (line.isBlank() || line.startsWith("##")) continue;
            if (line.length() < 3) continue;

            String statusPrefix = line.substring(0, 2);
            String path = line.substring(3).trim();
            if (path.startsWith("\"") && path.endsWith("\"") && path.length() >= 2) {
                path = path.substring(1, path.length() - 1);
            }
            if (path.contains(" -> ")) {
                path = path.substring(path.indexOf(" -> ") + 4).trim();
            }

            char x = statusPrefix.charAt(0);
            char y = statusPrefix.charAt(1);

            if (x == '?' && y == '?') {
                unversionedCategory.getFiles().add(new FileItem(path, ChangeType.UNTRACKED, true, false, false));
            } else if (x == '!' && y == '!') {
                ignoredCategory.getFiles().add(new FileItem(path, ChangeType.IGNORED, false, true, false));
            } else {
                ChangeType changeType = ChangeType.MODIFIED;
                if (x == 'A' || y == 'A') changeType = ChangeType.ADDED;
                else if (x == 'D' || y == 'D') changeType = ChangeType.DELETED;
                else if (x == 'R' || y == 'R') changeType = ChangeType.RENAMED;

                changesCategory.getFiles().add(new FileItem(path, changeType, false, false, true));
            }
        }

        buildTree();

        int changeCount = changesCategory.getFiles().size();
        int unversionedCount = unversionedCategory.getFiles().size();
        status.setText(changeCount + " file(s) changed" + (unversionedCount > 0 ? ", " + unversionedCount + " unversioned." : "."));
        updateCommitButtonState();
    }

    private void buildTree() {
        TreeItem<Object> root = new TreeItem<>("Root");

        changesCategory.updateSelectionState();
        changesTreeItem = new TreeItem<>(changesCategory);
        changesTreeItem.setExpanded(true);
        populateCategoryNodes(changesTreeItem, changesCategory.getFiles());
        root.getChildren().add(changesTreeItem);

        unversionedCategory.updateSelectionState();
        unversionedTreeItem = new TreeItem<>(unversionedCategory);
        unversionedTreeItem.setExpanded(true);
        populateCategoryNodes(unversionedTreeItem, unversionedCategory.getFiles());
        root.getChildren().add(unversionedTreeItem);

        if (showIgnored && !ignoredCategory.getFiles().isEmpty()) {
            ignoredCategory.updateSelectionState();
            ignoredTreeItem = new TreeItem<>(ignoredCategory);
            ignoredTreeItem.setExpanded(true);
            populateCategoryNodes(ignoredTreeItem, ignoredCategory.getFiles());
            root.getChildren().add(ignoredTreeItem);
        }

        treeView.setRoot(root);
    }

    private void populateCategoryNodes(TreeItem<Object> parentTreeItem, List<FileItem> files) {
        if (!groupByDirectory) {
            for (FileItem f : files) {
                parentTreeItem.getChildren().add(new TreeItem<>(f));
            }
        } else {
            Map<String, DirectoryGroup> dirMap = new LinkedHashMap<>();
            List<FileItem> rootFiles = new ArrayList<>();

            for (FileItem f : files) {
                String d = f.getDirectoryPath();
                if (d.isBlank()) {
                    rootFiles.add(f);
                } else {
                    dirMap.computeIfAbsent(d, DirectoryGroup::new).getFiles().add(f);
                }
            }

            for (DirectoryGroup dg : dirMap.values()) {
                TreeItem<Object> dirItem = new TreeItem<>(dg);
                dirItem.setExpanded(true);
                for (FileItem f : dg.getFiles()) {
                    dirItem.getChildren().add(new TreeItem<>(f));
                }
                parentTreeItem.getChildren().add(dirItem);
            }
            for (FileItem f : rootFiles) {
                parentTreeItem.getChildren().add(new TreeItem<>(f));
            }
        }
    }

    private Node buildCategoryCell(CategoryItem cat) {
        CheckBox cb = new CheckBox();
        cb.setStyle("-fx-cursor: hand;");
        cb.setSelected(cat.selectedProperty().get());
        cb.setIndeterminate(cat.indeterminateProperty().get());

        cb.setOnAction(e -> {
            boolean newVal = cb.isSelected();
            cat.toggleAll(newVal);
            treeView.refresh();
            updateCommitButtonState();
        });

        Label title = new Label(cat.getName());
        title.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label badge = new Label(cat.getFiles().size() + " files");
        badge.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");

        HBox box = new HBox(8, cb, title, badge);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(2, 0, 2, 0));
        return box;
    }

    private Node buildFileCell(FileItem file) {
        CheckBox cb = new CheckBox();
        cb.setStyle("-fx-cursor: hand;");
        cb.setSelected(file.isSelected());

        cb.setOnAction(e -> {
            file.setSelected(cb.isSelected());
            changesCategory.updateSelectionState();
            unversionedCategory.updateSelectionState();
            ignoredCategory.updateSelectionState();
            treeView.refresh();
            updateCommitButtonState();
        });

        Node icon = createFileIcon(file.getFileName(), file.getChangeType());

        Label nameLabel = new Label(file.getFileName());
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + getStatusColor(file.getChangeType()) + ";");
        if (file.getChangeType() == ChangeType.DELETED) {
            nameLabel.setStyle(nameLabel.getStyle() + " -fx-strikethrough: true;");
        }

        HBox box = new HBox(6, cb, icon, nameLabel);
        box.setAlignment(Pos.CENTER_LEFT);

        if (!groupByDirectory && !file.getDirectoryPath().isBlank()) {
            Label pathLabel = new Label(file.getDirectoryPath());
            pathLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");
            box.getChildren().add(pathLabel);
        }

        box.setPadding(new Insets(1, 0, 1, 0));
        return box;
    }

    private Node buildDirectoryCell(DirectoryGroup dir) {
        Label icon = new Label("\uD83D\uDCC1");
        icon.setStyle("-fx-font-size: 11px;");
        Label label = new Label(dir.getDirPath());
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox box = new HBox(6, icon, label);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Node createFileIcon(String fileName, ChangeType type) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".java")) {
            boolean isTest = lower.contains("test");
            Circle circle = new Circle(6.0);
            circle.setFill(Color.web("#3592C4"));
            Label letter = new Label("C");
            letter.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 8px; -fx-font-weight: bold;");
            StackPane base = new StackPane(circle, letter);
            if (isTest) {
                Circle badge = new Circle(2.2);
                badge.setFill(Color.web("#57965C"));
                StackPane sp = new StackPane(base, badge);
                StackPane.setAlignment(badge, Pos.BOTTOM_RIGHT);
                return sizedIcon(sp);
            }
            return sizedIcon(base);
        } else if (lower.endsWith(".xml") || lower.endsWith(".html")) {
            Label badge = new Label("</>");
            badge.setStyle("-fx-text-fill: #8FCE8F; -fx-font-size: 8px; -fx-font-weight: bold;");
            return sizedIcon(badge);
        } else if (lower.endsWith(".yml") || lower.endsWith(".yaml") || lower.endsWith(".properties") || lower.endsWith(".json")) {
            Label badge = new Label("\u2699");
            badge.setStyle("-fx-text-fill: #D9A03D; -fx-font-size: 11px;");
            return sizedIcon(badge);
        } else if (lower.endsWith(".css")) {
            Label badge = new Label("CSS");
            badge.setStyle("-fx-text-fill: #3592C4; -fx-font-size: 7px; -fx-font-weight: bold;");
            return sizedIcon(badge);
        } else if (lower.startsWith(".git") || lower.contains("ignore")) {
            Circle dot = new Circle(4.0);
            dot.setFill(Color.web("#F05133"));
            return sizedIcon(dot);
        } else {
            Rectangle rect = new Rectangle(9, 12);
            rect.setFill(Color.web("#697089"));
            rect.setArcWidth(2);
            rect.setArcHeight(2);
            return sizedIcon(rect);
        }
    }

    public static String getStatusColor(ChangeType type) {
        return switch (type) {
            case MODIFIED -> "#56A8F5";   // Cyan / Blue
            case ADDED -> "#629755";      // Green (staged new file)
            case UNTRACKED -> "#ED6C63";  // Light red / Coral (IntelliJ Unversioned Files)
            case DELETED -> "#E06C75";    // Red / Dim
            case RENAMED -> "#9876AA";    // Purple
            case IGNORED -> "#6F737A";    // Gray
        };
    }

    private void updateCommitButtonState() {
        int checkedCount = getCheckedFiles().size();
        String msg = message.getText().trim();
        boolean hasFiles = checkedCount > 0;
        boolean hasMsg = !msg.isEmpty();

        boolean isAmend = amendCheck.isSelected();
        if (isAmend) {
            commitBtn.setText(checkedCount > 0 ? "Commit (Amend) (" + checkedCount + ")" : "Commit (Amend)");
            commitBtn.setDisable(!hasMsg);
            commitAndPushBtn.setDisable(!hasMsg);
        } else {
            commitBtn.setText(checkedCount > 0 ? "Commit (" + checkedCount + ")" : "Commit");
            commitBtn.setDisable(!hasFiles || !hasMsg);
            commitAndPushBtn.setDisable(!hasFiles || !hasMsg);
        }
    }

    public List<FileItem> getCheckedFiles() {
        List<FileItem> checked = new ArrayList<>();
        for (FileItem f : changesCategory.getFiles()) {
            if (f.isSelected()) checked.add(f);
        }
        for (FileItem f : unversionedCategory.getFiles()) {
            if (f.isSelected()) checked.add(f);
        }
        return checked;
    }

    private List<String> getCheckedPaths() {
        List<String> paths = new ArrayList<>();
        for (FileItem f : getCheckedFiles()) {
            paths.add(f.getRelativePath());
        }
        return paths;
    }

    private void doRollback() {
        Path dir = projectRoot.get();
        if (dir == null || !GitService.isRepository(dir)) return;

        List<FileItem> targets = new ArrayList<>();
        TreeItem<Object> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() instanceof FileItem file) {
            targets.add(file);
        } else {
            targets.addAll(getCheckedFiles());
        }

        if (targets.isEmpty()) {
            status.setText("No files selected to rollback.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Rollback Changes");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to rollback changes in " + targets.size() + " file(s)?");
        confirm.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        List<String> modifiedPaths = new ArrayList<>();
        List<String> unversionedPaths = new ArrayList<>();
        for (FileItem item : targets) {
            if (item.isUnversioned()) {
                unversionedPaths.add(item.getRelativePath());
            } else {
                modifiedPaths.add(item.getRelativePath());
            }
        }

        if (!modifiedPaths.isEmpty()) {
            GitService.rollback(dir, modifiedPaths);
        }
        for (String p : unversionedPaths) {
            try {
                Files.deleteIfExists(dir.resolve(p));
            } catch (Exception ignored) {}
        }

        refresh();
        status.setText("Rolled back " + targets.size() + " file(s).");
    }

    private void doCommit(boolean push) {
        Path dir = projectRoot.get();
        if (dir == null || !GitService.isRepository(dir)) {
            status.setText("Not a git repository.");
            return;
        }
        String msg = message.getText().trim();
        if (msg.isEmpty()) {
            status.setText("Enter a commit message first.");
            return;
        }
        List<String> selectedPaths = getCheckedPaths();
        if (selectedPaths.isEmpty()) {
            status.setText("Nothing selected to commit.");
            return;
        }
        boolean isAmend = amendCheck.isSelected();
        status.setText(isAmend ? "Amending commit\u2026" : "Committing\u2026");

        new Thread(() -> {
            GitService.Result add = GitService.add(dir, selectedPaths);
            if (!add.ok()) {
                Platform.runLater(() -> status.setText("git add failed: " + add.output().trim()));
                return;
            }
            GitService.Result commit = isAmend ? GitService.commitAmend(dir, msg) : GitService.commit(dir, msg);
            if (!commit.ok()) {
                Platform.runLater(() -> status.setText("Commit failed: " + commit.output().trim()));
                return;
            }
            String pushMsg = "";
            if (push) {
                GitService.Result pushRes = GitService.push(dir);
                pushMsg = pushRes.ok() ? " and pushed" : " (push failed: " + pushRes.output().trim() + ")";
            }
            String finalPushMsg = pushMsg;
            Platform.runLater(() -> {
                if (!isAmend) message.clear();
                amendCheck.setSelected(false);
                refresh();
                status.setText((isAmend ? "Amend successful" : "Commit successful") + finalPushMsg);
                if (log != null) log.accept("\u2713 " + (isAmend ? "Amended" : "Committed") + finalPushMsg + ": " + msg);
            });
        }, "lumina-git-commit").start();
    }

    public void expandAll(boolean expand) {
        if (treeView.getRoot() != null) {
            treeView.getRoot().setExpanded(true); // Root stays expanded so category headers remain visible
            for (TreeItem<Object> child : treeView.getRoot().getChildren()) {
                setExpandedRecursive(child, expand);
            }
        }
    }

    private void setExpandedRecursive(TreeItem<Object> item, boolean expand) {
        if (item == null) return;
        item.setExpanded(expand);
        for (TreeItem<Object> child : item.getChildren()) {
            setExpandedRecursive(child, expand);
        }
    }

    private void doStashOrShelve() {
        Path dir = projectRoot.get();
        if (dir == null || !GitService.isRepository(dir)) return;

        TextInputDialog dialog = new TextInputDialog("Stash on " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        dialog.setTitle("Stash Changes");
        dialog.setHeaderText("Save changes to Git stash");
        dialog.setContentText("Stash message:");
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
        dialog.showAndWait().ifPresent(msg -> {
            new Thread(() -> {
                GitService.stash(dir, msg);
                Platform.runLater(this::refresh);
            }, "lumina-git-stash").start();
        });
    }

    private void showStashMenu(Button anchor) {
        Path dir = projectRoot.get();
        if (dir == null || !GitService.isRepository(dir)) return;

        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5;");

        MenuItem newStashItem = new MenuItem("+ Stash Changes\u2026");
        newStashItem.setStyle("-fx-text-fill: #56A8F5; -fx-font-weight: bold;");
        newStashItem.setOnAction(e -> doStashOrShelve());
        menu.getItems().add(newStashItem);

        GitService.Result r = GitService.stashList(dir);
        if (r.ok() && !r.output().isBlank()) {
            menu.getItems().add(new SeparatorMenuItem());
            for (String line : r.output().split("\\R")) {
                if (line.isBlank()) continue;
                MenuItem item = new MenuItem(line.trim());
                item.setStyle("-fx-text-fill: #DFE1E5;");
                item.setOnAction(e -> {
                    GitService.stashPop(dir);
                    refresh();
                });
                menu.getItems().add(item);
            }
        } else {
            MenuItem emptyItem = new MenuItem("No stashes saved");
            emptyItem.setDisable(true);
            emptyItem.setStyle("-fx-text-fill: #868A91;");
            menu.getItems().addAll(new SeparatorMenuItem(), emptyItem);
        }
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void showOptionsMenu(Button anchor) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5;");

        MenuItem groupHeader = new MenuItem("Group By");
        groupHeader.setDisable(true);
        groupHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #868A91;");

        CheckMenuItem dirItem = new CheckMenuItem("Directory");
        dirItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN));
        dirItem.setSelected(groupByDirectory);
        dirItem.setOnAction(e -> {
            groupByDirectory = dirItem.isSelected();
            buildTree();
        });

        CheckMenuItem moduleItem = new CheckMenuItem("Module");
        moduleItem.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN));
        moduleItem.setSelected(false);
        moduleItem.setDisable(true);

        SeparatorMenuItem sep = new SeparatorMenuItem();

        MenuItem showHeader = new MenuItem("Show");
        showHeader.setDisable(true);
        showHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #868A91;");

        CheckMenuItem ignoredItem = new CheckMenuItem("Ignored Files");
        ignoredItem.setSelected(showIgnored);
        ignoredItem.setOnAction(e -> {
            showIgnored = ignoredItem.isSelected();
            refresh();
        });

        menu.getItems().addAll(groupHeader, dirItem, moduleItem, sep, showHeader, ignoredItem);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void showRecentMessages(Button anchor) {
        Path dir = projectRoot.get();
        if (dir == null || !GitService.isRepository(dir)) return;

        List<String> recents = GitService.recentCommitMessages(dir, 8);
        if (recents.isEmpty()) {
            ContextMenu emptyMenu = new ContextMenu(new MenuItem("No recent commit messages"));
            emptyMenu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
            return;
        }

        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5;");

        for (String msg : recents) {
            MenuItem item = new MenuItem(msg);
            item.setStyle("-fx-text-fill: #DFE1E5;");
            item.setOnAction(e -> {
                message.setText(msg);
                message.positionCaret(msg.length());
            });
            menu.getItems().add(item);
        }
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private Button createToolbarIconButton(Node icon, String tooltipText) {
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 4 3 4; -fx-cursor: hand; -fx-background-radius: 4;");
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisable()) btn.setStyle("-fx-background-color: #35373C; -fx-padding: 3 4 3 4; -fx-cursor: hand; -fx-background-radius: 4;");
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisable()) btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 4 3 4; -fx-cursor: hand; -fx-background-radius: 4;");
        });
        btn.setTooltip(new Tooltip(tooltipText));
        return btn;
    }

    private Button createToolbarButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #AFB1B6; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 3 7 3 7; -fx-cursor: hand; -fx-background-radius: 4;");
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisable()) btn.setStyle("-fx-background-color: #35373C; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 3 7 3 7; -fx-cursor: hand; -fx-background-radius: 4;");
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisable()) btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #AFB1B6; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 3 7 3 7; -fx-cursor: hand; -fx-background-radius: 4;");
        });
        btn.setTooltip(new Tooltip(tooltipText));
        return btn;
    }

    private static Node sizedIcon(Node n) {
        StackPane sp = new StackPane(n);
        sp.setPrefSize(16, 16);
        sp.setMinSize(16, 16);
        sp.setMaxSize(16, 16);
        sp.setAlignment(Pos.CENTER);
        return sp;
    }

    private static Node createRefreshIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 12 8 A 4 4 0 1 1 10.8 5.2 M 10.8 5.2 L 10.8 2.5 M 10.8 5.2 L 13.5 5.2");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web("#AFB1B6"));
        path.setStrokeWidth(1.35);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        return sizedIcon(path);
    }

    private static Node createRollbackIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 6 4.5 L 2.5 8 L 6 11.5 M 2.5 8 L 9.5 8 C 11.5 8 13.5 9.5 13.5 12");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web("#AFB1B6"));
        path.setStrokeWidth(1.35);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        return sizedIcon(path);
    }

    private static Node createShelveIcon() {
        SVGPath tray = new SVGPath();
        tray.setContent("M 3 9.5 L 3 13 L 13 13 L 13 9.5");
        tray.setFill(Color.TRANSPARENT);
        tray.setStroke(Color.web("#AFB1B6"));
        tray.setStrokeWidth(1.35);
        tray.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        tray.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        SVGPath arrow = new SVGPath();
        arrow.setContent("M 8 3 L 8 10 M 5.5 7.5 L 8 10 L 10.5 7.5");
        arrow.setFill(Color.TRANSPARENT);
        arrow.setStroke(Color.web("#AFB1B6"));
        arrow.setStrokeWidth(1.35);
        arrow.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        arrow.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        return sizedIcon(new StackPane(tray, arrow));
    }

    private static Node createDiffPreviewIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 3 3.5 L 13 3.5 L 13 12.5 L 3 12.5 Z M 8 3.5 L 8 12.5");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web("#AFB1B6"));
        path.setStrokeWidth(1.3);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        return sizedIcon(path);
    }

    private static Node createEyeIcon() {
        SVGPath eye = new SVGPath();
        eye.setContent("M 2 8 C 4 4.5, 12 4.5, 14 8 C 12 11.5, 4 11.5, 2 8 Z");
        eye.setFill(Color.TRANSPARENT);
        eye.setStroke(Color.web("#AFB1B6"));
        eye.setStrokeWidth(1.3);

        Circle pupil = new Circle(1.8);
        pupil.setFill(Color.web("#AFB1B6"));

        return sizedIcon(new StackPane(eye, pupil));
    }

    private static Node createExpandAllIcon() {
        SVGPath topChevron = new SVGPath();
        topChevron.setContent("M 3.5 6 L 8 1.5 L 12.5 6");
        topChevron.setFill(Color.TRANSPARENT);
        topChevron.setStroke(Color.web("#AFB1B6"));
        topChevron.setStrokeWidth(1.35);
        topChevron.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        topChevron.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        SVGPath bottomChevron = new SVGPath();
        bottomChevron.setContent("M 3.5 10 L 8 14.5 L 12.5 10");
        bottomChevron.setFill(Color.TRANSPARENT);
        bottomChevron.setStroke(Color.web("#AFB1B6"));
        bottomChevron.setStrokeWidth(1.35);
        bottomChevron.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        bottomChevron.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        return sizedIcon(new StackPane(topChevron, bottomChevron));
    }

    private static Node createCollapseAllIcon() {
        SVGPath topChevron = new SVGPath();
        topChevron.setContent("M 3.5 2.5 L 8 7 L 12.5 2.5");
        topChevron.setFill(Color.TRANSPARENT);
        topChevron.setStroke(Color.web("#AFB1B6"));
        topChevron.setStrokeWidth(1.35);
        topChevron.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        topChevron.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        SVGPath bottomChevron = new SVGPath();
        bottomChevron.setContent("M 3.5 13.5 L 8 9 L 12.5 13.5");
        bottomChevron.setFill(Color.TRANSPARENT);
        bottomChevron.setStroke(Color.web("#AFB1B6"));
        bottomChevron.setStrokeWidth(1.35);
        bottomChevron.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        bottomChevron.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        return sizedIcon(new StackPane(topChevron, bottomChevron));
    }

    // Accessors for testing
    public TreeView<Object> getTreeView() { return treeView; }
    public TextArea getMessage() { return message; }
    public Button getCommitBtn() { return commitBtn; }
    public Button getCommitAndPushBtn() { return commitAndPushBtn; }
    public CheckBox getAmendCheck() { return amendCheck; }
    public CategoryItem getChangesCategory() { return changesCategory; }
    public CategoryItem getUnversionedCategory() { return unversionedCategory; }
    public Label getStatusLabel() { return status; }
}