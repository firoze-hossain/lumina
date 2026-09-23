package dev.lumina.ui;

import dev.lumina.git.GitService;
import dev.lumina.git.GitService.CommitFile;
import dev.lumina.git.GitService.OutgoingCommit;
import dev.lumina.notification.Notification;
import dev.lumina.notification.NotificationService;
import dev.lumina.notification.NotificationType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * IntelliJ IDEA-styled "Push Commits to <Repository>" modal dialog matching
 * media_1790090061765.png and media_1790090079938.png:
 * - Warning banner: "Commit contains problems: 1 warning" with "Review code analysis"
 * - Left pane: branch routing (e.g. master -> origin : master) and unpushed commits list
 * - Right pane: toolbar (Diff, View Options, Edit, Stash, Expand/Collapse) and changed files tree
 * - Bottom bar: ? Help, Push tags: [All v], [ Push Anyway v ] with Force Push popup, and [ Cancel ]
 */
public class PushCommitsDialog extends Stage {

    public record DirectoryGroup(String dirPath, List<CommitFile> files) {}

    @FunctionalInterface
    public interface DiffOpener {
        void openDiff(String title, String commitHash, String relativePath, Path localPath, String currentContent, String commitContent);
    }

    private final Path projectRoot;
    private final Consumer<String> log;
    private final int problemsCount;
    private Runnable onReviewCodeAnalysis;
    private DiffOpener onOpenDiff;

    // UI Components
    private final ListView<OutgoingCommit> commitsListView = new ListView<>();
    private final TreeView<Object> filesTreeView = new TreeView<>();
    private final Label branchRoutingLabel = new Label();
    private final CheckBox pushTagsCheck = new CheckBox("Push tags:");
    private final ComboBox<String> tagModeCombo = new ComboBox<>();
    private final Button pushMainBtn = new Button();
    private final Button pushChevronBtn = new Button("▾");
    private final Button cancelBtn = new Button("Cancel");
    private final Button helpBtn = new Button("?");

    private List<OutgoingCommit> outgoingCommits = new ArrayList<>();
    private boolean groupByDirectory = true;

    public PushCommitsDialog(Window owner, Path projectRoot, Consumer<String> log, int problemsCount) {
        this.projectRoot = projectRoot;
        this.log = log;
        this.problemsCount = problemsCount;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        String repoName = projectRoot != null && projectRoot.getFileName() != null
                ? projectRoot.getFileName().toString() : "Repository";
        setTitle("Push Commits to " + repoName);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Top warning banner (if problems exist)
        Node warningBanner = buildWarningBanner();
        if (warningBanner != null) {
            root.getChildren().add(warningBanner);
        }

        // 2. Main split view (left commits, right file tree)
        SplitPane mainSplit = buildMainSplit();
        VBox.setVgrow(mainSplit, Priority.ALWAYS);
        root.getChildren().add(mainSplit);

        // 3. Bottom action bar
        HBox bottomBar = buildBottomBar();
        root.getChildren().add(bottomBar);

        Scene scene = new Scene(root, 780, 520);
        scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        setScene(scene);
        setMinWidth(640);
        setMinHeight(420);

        // Load data dynamically
        loadCommits();
    }

    public void setOnReviewCodeAnalysis(Runnable onReviewCodeAnalysis) {
        this.onReviewCodeAnalysis = onReviewCodeAnalysis;
    }

    public void setOnOpenDiff(DiffOpener onOpenDiff) {
        this.onOpenDiff = onOpenDiff;
    }

    // ----------------------------------------------------------------- Warning Banner

    private Node buildWarningBanner() {
        if (problemsCount <= 0) return null;

        HBox banner = new HBox(8);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(8, 14, 8, 14));
        banner.setStyle("-fx-background-color: #382424; -fx-border-color: #5C3232; -fx-border-width: 0 0 1 0;");

        Circle iconBg = new Circle(7);
        iconBg.setFill(Color.web("#ED6C63"));
        Label exclamation = new Label("!");
        exclamation.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 10px; -fx-font-weight: bold;");
        StackPane icon = new StackPane(iconBg, exclamation);

        Label warningText = new Label("Commit contains problems: " + problemsCount + " warning" + (problemsCount == 1 ? "" : "s"));
        warningText.setStyle("-fx-text-fill: #E58686; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Hyperlink reviewLink = new Hyperlink("Review code analysis");
        reviewLink.setStyle("-fx-text-fill: #56A8F5; -fx-underline: false; -fx-font-size: 12px; -fx-cursor: hand;");
        reviewLink.setOnAction(e -> {
            close();
            if (onReviewCodeAnalysis != null) onReviewCodeAnalysis.run();
        });

        banner.getChildren().addAll(icon, warningText, spacer, reviewLink);
        return banner;
    }

    // ----------------------------------------------------------------- Main Split

    private SplitPane buildMainSplit() {
        // Left Column: Branch routing + Commit list
        VBox leftPane = new VBox(6);
        leftPane.setPadding(new Insets(8, 8, 8, 12));
        leftPane.setStyle("-fx-background-color: #1E1F22;");

        branchRoutingLabel.setStyle("-fx-font-size: 12px;");
        updateBranchRoutingHeader();

        commitsListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(commitsListView, Priority.ALWAYS);

        commitsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(OutgoingCommit commit, boolean empty) {
                super.updateItem(commit, empty);
                if (empty || commit == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    Label subject = new Label(commit.subject());
                    subject.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    setGraphic(subject);
                    setText(null);
                    setStyle("-fx-background-color: " + (isSelected() ? "#2E436E;" : "#1E1F22;") + " -fx-padding: 3 6 3 6;");
                }
            }
        });

        commitsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                loadCommitFilesTree(selected);
            }
        });

        leftPane.getChildren().addAll(branchRoutingLabel, commitsListView);

        // Right Column: Toolbar + File tree
        VBox rightPane = new VBox(4);
        rightPane.setPadding(new Insets(6, 12, 8, 8));
        rightPane.setStyle("-fx-background-color: #1E1F22;");

        HBox rightToolbar = buildRightToolbar();

        filesTreeView.setShowRoot(true);
        filesTreeView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(filesTreeView, Priority.ALWAYS);

        filesTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else if (item instanceof String header) {
                    Label icon = new Label("\uD83D\uDCC1");
                    icon.setStyle("-fx-font-size: 11px;");
                    Label label = new Label(header);
                    label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");
                    HBox box = new HBox(6, icon, label);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                    setStyle("-fx-background-color: " + (isSelected() ? "#2E436E;" : "#1E1F22;"));
                } else if (item instanceof DirectoryGroup dir) {
                    Label icon = new Label("\uD83D\uDCC1");
                    icon.setStyle("-fx-font-size: 11px;");
                    Label name = new Label(dir.dirPath());
                    name.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    Label badge = new Label(dir.files().size() + " file" + (dir.files().size() == 1 ? "" : "s"));
                    badge.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");
                    HBox box = new HBox(6, icon, name, badge);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                    setStyle("-fx-background-color: " + (isSelected() ? "#2E436E;" : "#1E1F22;"));
                } else if (item instanceof CommitFile file) {
                    Node icon = createFileIcon(file.fileName());
                    Label name = new Label(file.fileName());
                    name.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 12px;");
                    HBox box = new HBox(6, icon, name);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                    setStyle("-fx-background-color: " + (isSelected() ? "#2E436E;" : "#1E1F22;"));
                }
            }
        });

        filesTreeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                openSelectedFileDiff();
            }
        });

        filesTreeView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.F4) {
                openSelectedFileDiff();
            }
        });

        rightPane.getChildren().addAll(rightToolbar, filesTreeView);

        SplitPane split = new SplitPane(leftPane, rightPane);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.36);
        split.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        return split;
    }

    private void updateBranchRoutingHeader() {
        String currentBranch = GitService.currentBranch(projectRoot);
        if (currentBranch == null) currentBranch = "master";

        String upstream = GitService.getUpstreamBranch(projectRoot);
        String remoteBranch = currentBranch;
        if (upstream != null && upstream.contains("/")) {
            remoteBranch = upstream.substring(upstream.indexOf('/') + 1);
        }

        HBox header = new HBox(4);
        header.setAlignment(Pos.CENTER_LEFT);

        Label local = new Label(currentBranch);
        local.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label arrow = new Label(" \u2192 origin : ");
        arrow.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        Label remote = new Label(remoteBranch);
        remote.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 12px; -fx-cursor: hand;");

        header.getChildren().addAll(local, arrow, remote);
        branchRoutingLabel.setGraphic(header);
    }

    private HBox buildRightToolbar() {
        Button diffBtn = createToolbarButton("\uD83D\uDD0D", "Show Diff (Ctrl+D)");
        diffBtn.setOnAction(e -> openSelectedFileDiff());

        Button eyeBtn = createToolbarButton("\uD83D\uDC41", "View Options");
        eyeBtn.setOnAction(e -> {
            ContextMenu menu = new ContextMenu();
            menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5;");
            CheckMenuItem groupItem = new CheckMenuItem("Group By Directory");
            groupItem.setSelected(groupByDirectory);
            groupItem.setOnAction(ev -> {
                groupByDirectory = groupItem.isSelected();
                OutgoingCommit sel = commitsListView.getSelectionModel().getSelectedItem();
                if (sel != null) loadCommitFilesTree(sel);
            });
            menu.getItems().add(groupItem);
            menu.show(eyeBtn, Side.BOTTOM, 0, 0);
        });

        Button editBtn = createToolbarButton("✎", "Edit Commit Message");
        Button shelfBtn = createToolbarButton("\uD83D\uDCE6", "Shelve\u2026");

        Button expandBtn = createToolbarButton("\u2922", "Expand All");
        expandBtn.setOnAction(e -> expandAll(true));

        Button collapseBtn = createToolbarButton("\u2921", "Collapse All");
        collapseBtn.setOnAction(e -> expandAll(false));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(4, diffBtn, eyeBtn, editBtn, shelfBtn, spacer, expandBtn, collapseBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 0, 4, 0));
        return toolbar;
    }

    // ----------------------------------------------------------------- Bottom Action Bar

    private HBox buildBottomBar() {
        helpBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-weight: bold; -fx-min-width: 24; -fx-min-height: 24; -fx-cursor: hand;");
        helpBtn.setTooltip(new Tooltip("Push commits to remote Git repository"));

        pushTagsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");

        tagModeCombo.setItems(FXCollections.observableArrayList("All", "Current Branch"));
        tagModeCombo.getSelectionModel().select(0);
        tagModeCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        tagModeCombo.disableProperty().bind(pushTagsCheck.selectedProperty().not());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Primary Split Button [ Push Anyway | ▾ ] or [ Push | ▾ ]
        String pushText = problemsCount > 0 ? "Push Anyway" : "Push";
        pushMainBtn.setText(pushText);
        pushMainBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 5 14 5 14; -fx-background-radius: 4 0 0 4; -fx-cursor: hand;");
        pushMainBtn.setOnAction(e -> executePush(false));

        pushChevronBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 5 8 5 8; -fx-background-radius: 0 4 4 0; -fx-cursor: hand; -fx-border-color: transparent transparent transparent #2960CA; -fx-border-width: 0 0 0 1;");
        pushChevronBtn.setOnAction(e -> showForcePushMenu(pushChevronBtn));

        HBox pushSplitBtn = new HBox(0, pushMainBtn, pushChevronBtn);
        pushSplitBtn.setAlignment(Pos.CENTER_LEFT);

        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5; -fx-padding: 5 16 5 16; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        HBox bottom = new HBox(8, helpBtn, pushTagsCheck, tagModeCombo, spacer, pushSplitBtn, cancelBtn);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(10, 14, 12, 14));
        bottom.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2E3136; -fx-border-width: 1 0 0 0;");
        return bottom;
    }

    private void showForcePushMenu(Button anchor) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5;");

        MenuItem forcePushItem = new MenuItem("Force Push");
        forcePushItem.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Force Push");
            confirm.setHeaderText("Force push commits to remote?");
            confirm.setContentText("Force pushing will overwrite the remote repository branch.\nAre you sure you want to proceed?");
            confirm.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    executePush(true);
                }
            });
        });

        menu.getItems().add(forcePushItem);
        menu.show(anchor, Side.BOTTOM, 0, 0);
    }

    // ----------------------------------------------------------------- Data & Logic

    public void loadCommits() {
        if (projectRoot == null || !GitService.isRepository(projectRoot)) return;

        outgoingCommits = GitService.outgoingCommits(projectRoot);
        commitsListView.setItems(FXCollections.observableArrayList(outgoingCommits));

        if (!outgoingCommits.isEmpty()) {
            commitsListView.getSelectionModel().select(0);
            loadCommitFilesTree(outgoingCommits.get(0));
            pushMainBtn.setDisable(false);
            pushChevronBtn.setDisable(false);
        } else {
            filesTreeView.setRoot(null);
            pushMainBtn.setDisable(true);
            pushChevronBtn.setDisable(true);
        }
    }

    private void loadCommitFilesTree(OutgoingCommit commit) {
        String repoName = projectRoot != null && projectRoot.getFileName() != null
                ? projectRoot.getFileName().toString() : "Repository";

        List<CommitFile> files = commit.files();
        String rootTitle = repoName + " " + files.size() + " file" + (files.size() == 1 ? "" : "s");
        TreeItem<Object> rootItem = new TreeItem<>(rootTitle);
        rootItem.setExpanded(true);

        if (!groupByDirectory) {
            for (CommitFile f : files) {
                rootItem.getChildren().add(new TreeItem<>(f));
            }
        } else {
            Map<String, List<CommitFile>> dirMap = new LinkedHashMap<>();
            for (CommitFile f : files) {
                dirMap.computeIfAbsent(f.dirPath(), k -> new ArrayList<>()).add(f);
            }

            for (Map.Entry<String, List<CommitFile>> entry : dirMap.entrySet()) {
                String dirPath = entry.getKey();
                if (!dirPath.isBlank()) {
                    TreeItem<Object> dirItem = new TreeItem<>(new DirectoryGroup(dirPath, entry.getValue()));
                    dirItem.setExpanded(true);
                    for (CommitFile f : entry.getValue()) {
                        dirItem.getChildren().add(new TreeItem<>(f));
                    }
                    rootItem.getChildren().add(dirItem);
                } else {
                    for (CommitFile f : entry.getValue()) {
                        rootItem.getChildren().add(new TreeItem<>(f));
                    }
                }
            }
        }

        filesTreeView.setRoot(rootItem);
    }

    private void openSelectedFileDiff() {
        TreeItem<Object> sel = filesTreeView.getSelectionModel().getSelectedItem();
        if (sel == null || !(sel.getValue() instanceof CommitFile file)) return;
        OutgoingCommit commit = commitsListView.getSelectionModel().getSelectedItem();
        if (commit == null) return;

        Path localFile = projectRoot.resolve(file.relativePath());
        String currentContent = "";
        try {
            if (Files.exists(localFile)) {
                currentContent = Files.readString(localFile);
            }
        } catch (Exception ignored) {}

        String commitContent = "";
        try {
            GitService.Result r = GitService.exec(projectRoot, "show", commit.hash() + ":" + file.relativePath());
            if (r.ok()) commitContent = r.output();
        } catch (Exception ignored) {}

        String title = "Push Diff: " + file.fileName() + " (" + commit.shortHash() + ")";
        if (onOpenDiff != null) {
            onOpenDiff.openDiff(title, commit.shortHash(), file.relativePath(), localFile, currentContent, commitContent);
        }
    }

    private void executePush(boolean force) {
        if (projectRoot == null || !GitService.isRepository(projectRoot)) return;

        boolean pushTags = pushTagsCheck.isSelected();
        String tagMode = tagModeCombo.getSelectionModel().getSelectedItem();

        pushMainBtn.setDisable(true);
        pushChevronBtn.setDisable(true);
        cancelBtn.setDisable(true);

        new Thread(() -> {
            GitService.Result r = GitService.push(projectRoot, force, pushTags, tagMode);
            Platform.runLater(() -> {
                if (r.ok()) {
                    if (log != null) log.accept("\u2713 Successfully pushed to remote " + (force ? "(force)" : ""));
                    Notification pushNotif = new Notification(
                            "Git",
                            "Push successful",
                            "Pushed " + outgoingCommits.size() + " commit(s) to remote",
                            NotificationType.INFORMATION
                    );
                    NotificationService.getInstance().notify(pushNotif);
                    close();
                } else {
                    pushMainBtn.setDisable(false);
                    pushChevronBtn.setDisable(false);
                    cancelBtn.setDisable(false);
                    if (log != null) log.accept("Push failed: " + r.output().trim());

                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Push Failed");
                    err.setHeaderText("Git Push Rejected");
                    err.setContentText(r.output().trim());
                    err.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
                    err.showAndWait();
                }
            });
        }, "lumina-git-push").start();
    }

    private void expandAll(boolean expand) {
        if (filesTreeView.getRoot() != null) {
            filesTreeView.getRoot().setExpanded(true);
            for (TreeItem<Object> child : filesTreeView.getRoot().getChildren()) {
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

    // ----------------------------------------------------------------- Icons & Helpers

    private Button createToolbarButton(String text, String tip) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 13px; -fx-padding: 3 6 3 6; -fx-background-radius: 4; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2E3136; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 3 6 3 6; -fx-background-radius: 4; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 13px; -fx-padding: 3 6 3 6; -fx-background-radius: 4; -fx-cursor: hand;"));
        if (tip != null && !tip.isBlank()) btn.setTooltip(new Tooltip(tip));
        return btn;
    }

    private Node createFileIcon(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".java")) {
            Circle circle = new Circle(6.0);
            circle.setFill(Color.web("#3592C4"));
            Label letter = new Label("C");
            letter.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 8px; -fx-font-weight: bold;");
            return sizedIcon(new StackPane(circle, letter));
        } else if (lower.endsWith(".xml") || lower.endsWith(".html")) {
            Label badge = new Label("</>");
            badge.setStyle("-fx-text-fill: #8FCE8F; -fx-font-size: 8px; -fx-font-weight: bold;");
            return sizedIcon(badge);
        } else if (lower.endsWith(".yml") || lower.endsWith(".yaml") || lower.endsWith(".properties") || lower.endsWith(".json")) {
            Label badge = new Label("\u2699");
            badge.setStyle("-fx-text-fill: #D9A03D; -fx-font-size: 11px;");
            return sizedIcon(badge);
        } else {
            Rectangle rect = new Rectangle(9, 12);
            rect.setFill(Color.web("#697089"));
            rect.setArcWidth(2);
            rect.setArcHeight(2);
            return sizedIcon(rect);
        }
    }

    private Node sizedIcon(Node n) {
        StackPane sp = new StackPane(n);
        sp.setPrefSize(16, 16);
        sp.setMinSize(16, 16);
        sp.setMaxSize(16, 16);
        sp.setAlignment(Pos.CENTER);
        return sp;
    }

    // Accessors for testing
    public ListView<OutgoingCommit> getCommitsListView() { return commitsListView; }
    public TreeView<Object> getFilesTreeView() { return filesTreeView; }
    public Button getPushMainBtn() { return pushMainBtn; }
    public Button getPushChevronBtn() { return pushChevronBtn; }
    public Button getCancelBtn() { return cancelBtn; }
    public CheckBox getPushTagsCheck() { return pushTagsCheck; }
    public ComboBox<String> getTagModeCombo() { return tagModeCombo; }
    public List<OutgoingCommit> getOutgoingCommits() { return outgoingCommits; }
}
