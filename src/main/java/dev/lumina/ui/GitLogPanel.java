package dev.lumina.ui;

import dev.lumina.git.GitFileStatus;
import dev.lumina.git.GitLogCommit;
import dev.lumina.git.GitLogGraph;
import dev.lumina.git.GitService;
import dev.lumina.git.IssueNavigationManager;
import dev.lumina.git.VcsLogSettingsManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Three-pane Git Log tool window with left action toolbar, branch tree hierarchy,
 * commit table with multi-branch topological graph lines, search and filter toolbar,
 * changed files tree with syntax colors and expand/collapse actions, and commit metadata panel.
 */
public final class GitLogPanel extends VBox {

    private final Supplier<Path> projectRoot;
    private BiConsumer<String, String> onOpenFileDiff; // (commitHash, relativePath)

    // Sub-tabs at top of Git tool window
    private final HBox subTabBar = new HBox(4);
    private final StackPane contentStack = new StackPane();
    private final SplitPane mainSplit = new SplitPane();
    private final TextArea consoleArea = new TextArea();

    // Left pane: Toolbar + Branch tree
    private final HBox leftBranchPane = new HBox();
    private final VBox leftActionToolbar = new VBox(2);
    private final VBox branchTreeBox = new VBox();
    private final TextField branchFilterField = new TextField();
    private final TreeView<String> branchTree = new TreeView<>();
    private final Button toggleBranchTreeBtn = new Button("‹");
    private boolean branchTreeVisible = true;
    private final Set<String> favoriteBranches = new HashSet<>();

    // Center pane: Toolbar & Commit table
    private final TextField searchField = new TextField();
    private final ToggleButton matchCaseToggle = new ToggleButton("Cc");
    private final MenuButton branchMenuButton = new MenuButton("Branch");
    private final MenuButton userMenuButton = new MenuButton("User");
    private final MenuButton dateMenuButton = new MenuButton("Date");
    private final MenuButton pathsMenuButton = new MenuButton("Paths");
    private final Button sortOrderButton = new Button("⇅");
    private final TableView<GitLogCommit> commitTable = new TableView<>();

    // Right pane: Vertical split (Top = Changed files, Bottom = Commit details)
    private final SplitPane rightSplit = new SplitPane();
    private final TreeView<Object> changedFilesTree = new TreeView<>();
    private final StackPane changedFilesContainer = new StackPane();
    private final Label emptyChangesLabel = new Label("Select commit to view changes");
    private final VBox commitDetailsBox = new VBox(6);
    private final StackPane commitDetailsContainer = new StackPane();
    private final Label emptyDetailsLabel = new Label("Commit details");

    // Commit data
    private final ObservableList<GitLogCommit> masterCommitList = FXCollections.observableArrayList();
    private final FilteredList<GitLogCommit> filteredCommitList;
    private final SortedList<GitLogCommit> sortedCommitList;
    private Map<String, GitLogGraph.RowGraph> graphData = new HashMap<>();

    // Active filters
    private String activeBranchFilter = "All";
    private String activeUserFilter = "All";
    private String activeDateFilter = "Select...";
    private boolean sortAscending = false;

    // Sub-tab buttons
    private final Button logTabButton = new Button("Log");
    private final Button consoleTabButton = new Button("Console");

    public GitLogPanel(Supplier<Path> projectRoot) {
        this.projectRoot = projectRoot;
        getStyleClass().add("git-log-panel");
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        filteredCommitList = new FilteredList<>(masterCommitList, c -> true);
        sortedCommitList = new SortedList<>(filteredCommitList, (a, b) -> Long.compare(b.timestamp(), a.timestamp()));

        buildTopTabBar();
        buildLeftBranchPane();
        buildRightPane();

        // 3-Pane horizontal layout
        mainSplit.getItems().addAll(leftBranchPane, buildCenterContainer(), rightSplit);
        mainSplit.setDividerPositions(0.22, 0.70);
        SplitPane.setResizableWithParent(leftBranchPane, false);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        contentStack.getChildren().addAll(consoleArea, mainSplit);
        consoleArea.setVisible(false);
        consoleArea.setStyle("-fx-control-inner-background: #1E1F22; -fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-font-family: monospace;");
        consoleArea.setEditable(false);

        getChildren().addAll(subTabBar, contentStack);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        wireFilterEvents();

        // Listen for dynamic settings changes
        IssueNavigationManager.getInstance().addListener(this::refresh);
        VcsLogSettingsManager.getInstance().addListener(this::refresh);
    }

    public void setOnOpenFileDiff(BiConsumer<String, String> handler) {
        this.onOpenFileDiff = handler;
    }

    public void selectLogTab() {
        logTabButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-border-color: #3574F0; -fx-border-width: 0 0 2 0; -fx-padding: 4 12;");
        consoleTabButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-padding: 4 12;");
        mainSplit.setVisible(true);
        consoleArea.setVisible(false);
    }

    public void selectConsoleTab() {
        consoleTabButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-border-color: #3574F0; -fx-border-width: 0 0 2 0; -fx-padding: 4 12;");
        logTabButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-padding: 4 12;");
        mainSplit.setVisible(false);
        consoleArea.setVisible(true);
    }

    private void buildTopTabBar() {
        subTabBar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0; -fx-padding: 2 8 0 8;");
        subTabBar.setAlignment(Pos.CENTER_LEFT);

        logTabButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-border-color: #3574F0; -fx-border-width: 0 0 2 0; -fx-padding: 4 12;");
        consoleTabButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-padding: 4 12;");

        logTabButton.setOnAction(e -> selectLogTab());
        consoleTabButton.setOnAction(e -> selectConsoleTab());

        Button addTabButton = new Button("+");
        addTabButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 13px; -fx-cursor: hand;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button tabActionsButton = new Button("▾");
        tabActionsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-cursor: hand;");

        subTabBar.getChildren().addAll(logTabButton, consoleTabButton, addTabButton, spacer, tabActionsButton);
    }

    private Button createToolbarIconButton(String icon, String tooltipText, Runnable action) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3; -fx-min-width: 24px; -fx-pref-width: 24px; -fx-min-height: 24px; -fx-pref-height: 24px; -fx-alignment: center;");
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3; -fx-min-width: 24px; -fx-pref-width: 24px; -fx-min-height: 24px; -fx-pref-height: 24px; -fx-background-radius: 3; -fx-alignment: center;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3; -fx-min-width: 24px; -fx-pref-width: 24px; -fx-min-height: 24px; -fx-pref-height: 24px; -fx-alignment: center;"));
        if (action != null) {
            btn.setOnAction(e -> action.run());
        }
        return btn;
    }

    private void buildLeftBranchPane() {
        leftBranchPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 0 0;");
        leftBranchPane.setPrefWidth(228);
        leftBranchPane.setMinWidth(28);

        // 1. Left Action Toolbar Strip
        leftActionToolbar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 0 0; -fx-padding: 4 2;");
        leftActionToolbar.setPrefWidth(28);
        leftActionToolbar.setMinWidth(28);
        leftActionToolbar.setMaxWidth(28);
        leftActionToolbar.setAlignment(Pos.TOP_CENTER);

        toggleBranchTreeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3; -fx-min-width: 24px; -fx-pref-width: 24px; -fx-min-height: 24px; -fx-pref-height: 24px; -fx-alignment: center;");
        toggleBranchTreeBtn.setTooltip(new Tooltip("Collapse Tree View"));
        toggleBranchTreeBtn.setOnAction(e -> toggleBranchTreePane());

        Separator sep1 = new Separator(Orientation.HORIZONTAL);
        sep1.setStyle("-fx-background-color: #393B40; -fx-padding: 2 0;");

        Button newBranchBtn = createToolbarIconButton("+", "New Branch ⌥⌘N", this::showCreateBranchDialog);
        Button checkoutBtn = createToolbarIconButton("↙", "Checkout", this::checkoutSelectedBranch);
        Button deleteBranchBtn = createToolbarIconButton("🗑", "Delete Branch ⌫", this::deleteSelectedBranch);
        Button compareBtn = createToolbarIconButton("⇄", "Compare with Current", this::compareSelectedBranch);
        Button favoriteBtn = createToolbarIconButton("★", "Favorite", this::toggleFavoriteBranch);
        Button currentRevBtn = createToolbarIconButton("🎯", "Show Current Revision", () -> {
            if (branchTree.getRoot() != null && !branchTree.getRoot().getChildren().isEmpty()) {
                branchTree.getSelectionModel().select(branchTree.getRoot().getChildren().get(0));
            }
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button expandAllBranchesBtn = createToolbarIconButton("⊞", "Expand All ⌘+", this::expandAllBranchTree);
        Button collapseAllBranchesBtn = createToolbarIconButton("⊟", "Collapse All ⌘-", this::collapseAllBranchTree);

        leftActionToolbar.getChildren().addAll(
                toggleBranchTreeBtn, sep1,
                newBranchBtn, checkoutBtn, deleteBranchBtn, compareBtn, favoriteBtn, currentRevBtn,
                spacer,
                expandAllBranchesBtn, collapseAllBranchesBtn
        );

        // 2. Branch Tree Box
        branchTreeBox.setStyle("-fx-background-color: #1E1F22;");
        branchTreeBox.setPrefWidth(200);
        HBox.setHgrow(branchTreeBox, Priority.ALWAYS);

        HBox searchHeader = new HBox(4);
        searchHeader.setPadding(new Insets(4, 6, 4, 6));
        searchHeader.setAlignment(Pos.CENTER_LEFT);
        searchHeader.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: #8C8E94; -fx-font-size: 11px;");
        branchFilterField.setPromptText("Search branches");
        branchFilterField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-font-size: 12px; -fx-padding: 2 6;");
        HBox.setHgrow(branchFilterField, Priority.ALWAYS);
        searchHeader.getChildren().addAll(searchIcon, branchFilterField);

        branchTree.setShowRoot(false);
        branchTree.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-width: 0;");
        branchTree.setCellFactory(tv -> {
            TreeCell<String> cell = new TreeCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        setText(item);
                        updateBranchCellStyle(this);
                    }
                }
            };
            cell.selectedProperty().addListener((obs, was, is) -> updateBranchCellStyle(cell));
            return cell;
        });
        VBox.setVgrow(branchTree, Priority.ALWAYS);

        branchTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String val = newVal.getValue();
            if ("HEAD (Current Branch)".equals(val)) {
                activeBranchFilter = "HEAD";
            } else if (val != null && !val.equals("Local") && !val.equals("Remote") && !val.equals("origin")) {
                activeBranchFilter = val;
            } else {
                activeBranchFilter = "All";
            }
            branchMenuButton.setText(activeBranchFilter.equals("All") ? "Branch" : activeBranchFilter);
            applyFilters();
        });

        // Keyboard shortcuts for branch tree
        branchTree.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                deleteSelectedBranch();
            } else if (e.getCode() == KeyCode.N && e.isAltDown() && e.isShortcutDown()) {
                showCreateBranchDialog();
            } else if ((e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.EQUALS) && e.isShortcutDown()) {
                expandAllBranchTree();
            } else if (e.getCode() == KeyCode.MINUS && e.isShortcutDown()) {
                collapseAllBranchTree();
            }
        });

        branchTreeBox.getChildren().addAll(searchHeader, branchTree);
        leftBranchPane.getChildren().addAll(leftActionToolbar, branchTreeBox);
    }

    private void toggleBranchTreePane() {
        branchTreeVisible = !branchTreeVisible;
        branchTreeBox.setVisible(branchTreeVisible);
        branchTreeBox.setManaged(branchTreeVisible);
        toggleBranchTreeBtn.setText(branchTreeVisible ? "‹" : "›");
        toggleBranchTreeBtn.setTooltip(new Tooltip(branchTreeVisible ? "Collapse Tree View" : "Expand Tree View"));
        leftBranchPane.setPrefWidth(branchTreeVisible ? 228 : 28);
    }

    private void expandAllBranchTree() {
        setTreeExpanded(branchTree.getRoot(), true);
    }

    private void collapseAllBranchTree() {
        if (branchTree.getRoot() == null) return;
        for (TreeItem<String> child : branchTree.getRoot().getChildren()) {
            setTreeExpanded(child, false);
            child.setExpanded(true); // Keep top-level Local / Remote visible
        }
    }

    private void showCreateBranchDialog() {
        Path dir = projectRoot.get();
        if (dir == null || !GitService.isRepository(dir)) return;

        TreeItem<String> selected = branchTree.getSelectionModel().getSelectedItem();
        String startPoint = "HEAD";
        if (selected != null && selected.getValue() != null) {
            String val = selected.getValue();
            if (!val.equals("Local") && !val.equals("Remote") && !val.equals("origin") && !val.startsWith("HEAD")) {
                TreeItem<String> parent = selected.getParent();
                if (parent != null && "origin".equals(parent.getValue())) {
                    startPoint = "origin/" + val;
                } else {
                    startPoint = val;
                }
            }
        } else {
            GitLogCommit commit = commitTable.getSelectionModel().getSelectedItem();
            if (commit != null) {
                startPoint = commit.shortHash();
            }
        }

        CreateBranchDialog dlg = new CreateBranchDialog(getScene() != null ? getScene().getWindow() : null, dir, startPoint);
        dlg.showAndWait();
        if (dlg.getCreatedBranchName() != null) {
            refresh();
        }
    }

    private void checkoutSelectedBranch() {
        Path dir = projectRoot.get();
        if (dir == null || !GitService.isRepository(dir)) return;

        TreeItem<String> selected = branchTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;
        String val = selected.getValue();
        if (val.equals("Local") || val.equals("Remote") || val.equals("origin") || val.startsWith("HEAD")) {
            return;
        }

        TreeItem<String> parent = selected.getParent();
        boolean isRemote = parent != null && "origin".equals(parent.getValue());
        if (isRemote) {
            showCreateBranchDialog();
        } else {
            GitService.checkout(dir, val);
            refresh();
        }
    }

    private void deleteSelectedBranch() {
        Path dir = projectRoot.get();
        if (dir == null || !GitService.isRepository(dir)) return;

        TreeItem<String> selected = branchTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;
        String val = selected.getValue();
        if (val.equals("Local") || val.equals("Remote") || val.equals("origin") || val.startsWith("HEAD")) {
            return;
        }

        TreeItem<String> parent = selected.getParent();
        boolean isRemote = parent != null && "origin".equals(parent.getValue());
        String fullRef = isRemote ? "origin/" + val : val;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Branch");
        alert.setHeaderText("Delete branch '" + fullRef + "'?");
        alert.setContentText("Are you sure you want to delete this branch? This action cannot be undone.");
        alert.getDialogPane().setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");
        if (getScene() != null && getScene().getWindow() != null) {
            alert.initOwner(getScene().getWindow());
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (isRemote) {
                GitService.deleteRemoteBranch(dir, "origin", val);
            } else {
                GitService.deleteBranch(dir, val, true);
            }
            refresh();
        }
    }

    private void compareSelectedBranch() {
        TreeItem<String> selected = branchTree.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null) {
            String val = selected.getValue();
            if (!val.equals("Local") && !val.equals("Remote") && !val.equals("origin")) {
                activeBranchFilter = val;
                branchMenuButton.setText(val);
                refresh();
            }
        }
    }

    private void toggleFavoriteBranch() {
        TreeItem<String> selected = branchTree.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null) {
            String val = selected.getValue();
            if (favoriteBranches.contains(val)) {
                favoriteBranches.remove(val);
            } else {
                favoriteBranches.add(val);
            }
            populateBranchTree(branchFilterField.getText());
        }
    }

    private static void updateBranchCellStyle(TreeCell<String> cell) {
        if (cell.getItem() == null) {
            cell.setStyle("-fx-background-color: transparent;");
        } else if (cell.isSelected()) {
            cell.setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-background-radius: 3; -fx-font-size: 12px;");
        } else {
            cell.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        }
    }

    private Node buildCenterContainer() {
        VBox centerBox = new VBox();
        centerBox.setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(centerBox, Priority.ALWAYS);

        // Filter Toolbar
        HBox toolbar = new HBox(6);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 8, 4, 8));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        searchField.setPromptText("Text or hash");
        searchField.setMinWidth(110);
        searchField.setPrefWidth(140);
        searchField.setMaxWidth(180);
        searchField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-padding: 2 6; -fx-font-size: 12px;");

        matchCaseToggle.setText("Cc");
        matchCaseToggle.setMinWidth(28);
        matchCaseToggle.setPrefWidth(28);
        matchCaseToggle.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #8C8E94; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-font-size: 11px; -fx-padding: 2 4; -fx-cursor: hand;");
        matchCaseToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            matchCaseToggle.setStyle(newVal
                    ? "-fx-background-color: #2E436E; -fx-text-fill: #56A8F5; -fx-border-color: #3574F0; -fx-border-radius: 3; -fx-font-size: 11px; -fx-padding: 2 4; -fx-cursor: hand;"
                    : "-fx-background-color: #1E1F22; -fx-text-fill: #8C8E94; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-font-size: 11px; -fx-padding: 2 4; -fx-cursor: hand;");
            applyFilters();
        });

        branchMenuButton.setText("Branch");
        branchMenuButton.setMinWidth(Region.USE_PREF_SIZE);
        userMenuButton.setText("User");
        userMenuButton.setMinWidth(Region.USE_PREF_SIZE);
        dateMenuButton.setText("Date");
        dateMenuButton.setMinWidth(Region.USE_PREF_SIZE);
        pathsMenuButton.setText("Paths");
        pathsMenuButton.setMinWidth(Region.USE_PREF_SIZE);

        styleMenuButton(branchMenuButton);
        styleMenuButton(userMenuButton);
        styleMenuButton(dateMenuButton);
        styleMenuButton(pathsMenuButton);

        sortOrderButton.setText("⇅");
        sortOrderButton.setMinWidth(28);
        sortOrderButton.setPrefWidth(28);
        sortOrderButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 4;");
        sortOrderButton.setTooltip(new Tooltip("Toggle Sort Order"));
        sortOrderButton.setOnAction(e -> {
            sortAscending = !sortAscending;
            sortedCommitList.setComparator((a, b) -> sortAscending
                    ? Long.compare(a.timestamp(), b.timestamp())
                    : Long.compare(b.timestamp(), a.timestamp()));
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = createToolbarIconButton("↻", "Refresh", this::refresh);
        Button headJumpBtn = createToolbarIconButton("🎯", "Scroll to HEAD", () -> {
            if (!commitTable.getItems().isEmpty()) {
                commitTable.getSelectionModel().select(0);
                commitTable.scrollTo(0);
            }
        });
        Button toggleDetailsBtn = createToolbarIconButton("👁", "Show/Hide Preview Pane", () -> {
            if (mainSplit.getItems().contains(rightSplit)) {
                mainSplit.getItems().remove(rightSplit);
            } else {
                mainSplit.getItems().add(rightSplit);
                mainSplit.setDividerPositions(0.22, 0.70);
            }
        });

        toolbar.getChildren().addAll(
                searchField, matchCaseToggle,
                branchMenuButton, userMenuButton, dateMenuButton, pathsMenuButton,
                sortOrderButton, spacer,
                refreshBtn, headJumpBtn, toggleDetailsBtn
        );

        setupCommitTable();
        VBox.setVgrow(commitTable, Priority.ALWAYS);

        centerBox.getChildren().addAll(toolbar, commitTable);
        return centerBox;
    }

    private void styleMenuButton(MenuButton btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 3;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 6;"));
    }

    private void setupCommitTable() {
        commitTable.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-table-cell-border-color: transparent; -fx-border-width: 0;");
        commitTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<GitLogCommit, GitLogCommit> graphMsgCol = new TableColumn<>("Graph / Subject");
        graphMsgCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        graphMsgCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(GitLogCommit commit, boolean empty) {
                super.updateItem(commit, empty);
                if (empty || commit == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                HBox rowBox = new HBox(6);
                rowBox.setAlignment(Pos.CENTER_LEFT);

                // Multi-branch graph rendering
                GitLogGraph.RowGraph rg = graphData.get(commit.hash());
                Canvas canvas = renderGraphLane(commit, rg);
                rowBox.getChildren().add(canvas);

                // Commit subject with issue navigation support
                Node subjectNode = buildSubjectNode(commit.subject());
                rowBox.getChildren().add(subjectNode);

                // Branch & tag badges
                List<String> refs = commit.refs();
                if (refs != null && !refs.isEmpty()) {
                    HBox badgeBox = new HBox(4);
                    badgeBox.setAlignment(Pos.CENTER_LEFT);
                    for (String ref : refs) {
                        Label badge = new Label(ref);
                        boolean isOrigin = ref.startsWith("origin/") || ref.contains("origin");
                        String bg = isOrigin ? "#3C2B47" : "#2E3A4D";
                        String fg = isOrigin ? "#C792EA" : "#70A5EB";
                        badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 1 5; -fx-background-radius: 3;");
                        badgeBox.getChildren().add(badge);
                    }
                    rowBox.getChildren().add(badgeBox);
                }

                setGraphic(rowBox);
                setText(null);
            }
        });
        graphMsgCol.setPrefWidth(420);

        TableColumn<GitLogCommit, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().authorName()));
        authorCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String author, boolean empty) {
                super.updateItem(author, empty);
                if (empty || author == null) {
                    setText(null);
                } else {
                    setText(author);
                    setStyle("-fx-text-fill: #8C8E94; -fx-font-size: 12px;");
                }
            }
        });
        authorCol.setPrefWidth(110);
        authorCol.setMaxWidth(160);

        TableColumn<GitLogCommit, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getFormattedDate()));
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date);
                    setStyle("-fx-text-fill: #8C8E94; -fx-font-size: 12px;");
                }
            }
        });
        dateCol.setPrefWidth(130);
        dateCol.setMaxWidth(180);

        commitTable.getColumns().addAll(List.of(graphMsgCol, authorCol, dateCol));
        commitTable.setItems(sortedCommitList);

        // Selection listener to update right pane
        commitTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, commit) -> {
            updateRightPane(commit);
        });
    }

    private Canvas renderGraphLane(GitLogCommit commit, GitLogGraph.RowGraph rg) {
        int lane = rg != null ? rg.commitLane() : 0;
        int maxLanes = Math.max(lane + 1, rg != null && rg.activePassingLanes() != null ? rg.activePassingLanes().size() + 1 : 1);
        double laneWidth = 14.0;
        double width = Math.max(24.0, (maxLanes + 1) * laneWidth);
        double height = 22.0;

        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double commitX = (lane * laneWidth) + (laneWidth / 2.0);
        double centerY = height / 2.0;

        // Draw passing lanes
        if (rg != null && rg.activePassingLanes() != null) {
            for (int passLane : rg.activePassingLanes()) {
                double px = (passLane * laneWidth) + (laneWidth / 2.0);
                gc.setStroke(Color.web(GitLogGraph.getLaneColor(passLane)));
                gc.setLineWidth(2.0);
                gc.strokeLine(px, 0, px, height);
            }
        }

        // Draw down connections
        if (rg != null && rg.downConnections() != null) {
            for (GitLogGraph.Connection conn : rg.downConnections()) {
                double targetX = (conn.toLane() * laneWidth) + (laneWidth / 2.0);
                gc.setStroke(Color.web(GitLogGraph.getLaneColor(conn.toLane())));
                gc.setLineWidth(2.0);
                gc.beginPath();
                gc.moveTo(commitX, centerY);
                gc.bezierCurveTo(commitX, height, targetX, centerY, targetX, height);
                gc.stroke();
            }
        }

        // Draw up connections
        if (rg != null && rg.upConnections() != null) {
            for (GitLogGraph.Connection conn : rg.upConnections()) {
                double sourceX = (conn.fromLane() * laneWidth) + (laneWidth / 2.0);
                gc.setStroke(Color.web(GitLogGraph.getLaneColor(lane)));
                gc.setLineWidth(2.0);
                gc.beginPath();
                gc.moveTo(sourceX, 0);
                gc.bezierCurveTo(sourceX, centerY, commitX, 0, commitX, centerY);
                gc.stroke();
            }
        }

        // Draw commit node circle
        gc.setFill(Color.web(rg != null ? rg.getCommitColor() : GitLogGraph.LANE_COLORS[0]));
        double r = 4.0;
        gc.fillOval(commitX - r, centerY - r, r * 2, r * 2);

        return canvas;
    }

    private Node buildSubjectNode(String subjectText) {
        if (subjectText == null) subjectText = "";
        List<IssueNavigationManager.IssueMatch> matches = IssueNavigationManager.getInstance().findIssueMatches(subjectText);
        if (matches.isEmpty()) {
            Label subject = new Label(subjectText);
            subject.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
            return subject;
        }

        TextFlow flow = new TextFlow();
        int cursor = 0;
        for (IssueNavigationManager.IssueMatch m : matches) {
            if (m.start() > cursor) {
                Text plain = new Text(subjectText.substring(cursor, m.start()));
                plain.setStyle("-fx-fill: #DFE1E5; -fx-font-size: 13px;");
                flow.getChildren().add(plain);
            }

            Hyperlink link = new Hyperlink(m.issueKey());
            link.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 13px; -fx-padding: 0; -fx-underline: false;");
            link.setTooltip(new Tooltip(m.targetUrl()));
            link.setOnAction(e -> {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(URI.create(m.targetUrl()));
                    }
                } catch (Exception ignored) {}
            });
            flow.getChildren().add(link);

            cursor = Math.max(cursor, m.end());
        }

        if (cursor < subjectText.length()) {
            Text tail = new Text(subjectText.substring(cursor));
            tail.setStyle("-fx-fill: #DFE1E5; -fx-font-size: 13px;");
            flow.getChildren().add(tail);
        }

        return flow;
    }

    private void buildRightPane() {
        rightSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        rightSplit.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 0 0 1;");
        rightSplit.setPrefWidth(350);
        rightSplit.setMinWidth(200);

        // --- Top: Changed Files
        VBox changedFilesBox = new VBox();
        changedFilesBox.setStyle("-fx-background-color: #1E1F22;");
        HBox filesToolbar = new HBox(4);
        filesToolbar.setAlignment(Pos.CENTER_LEFT);
        filesToolbar.setPadding(new Insets(4, 6, 4, 6));
        filesToolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        Button diffBtn = createToolbarIconButton("⇄", "Show Diff (Double-click file)", this::openSelectedFileDiff);
        Button rollbackBtn = createToolbarIconButton("↶", "Rollback Changes", this::rollbackSelectedFile);
        Button groupFoldersBtn = createToolbarIconButton("📁", "Group by Directory", null);
        Button expandAllBtn = createToolbarIconButton("⊞", "Expand All ⌘+", this::expandAllChangedFiles);
        Button collapseAllBtn = createToolbarIconButton("⊟", "Collapse All ⌘-", this::collapseAllChangedFiles);

        Region filesSpacer = new Region();
        HBox.setHgrow(filesSpacer, Priority.ALWAYS);

        Button closeRightPaneBtn = createToolbarIconButton("×", "Close Preview Pane", () -> mainSplit.getItems().remove(rightSplit));

        filesToolbar.getChildren().addAll(diffBtn, rollbackBtn, groupFoldersBtn, expandAllBtn, collapseAllBtn, filesSpacer, closeRightPaneBtn);

        changedFilesTree.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-width: 0;");
        changedFilesTree.setShowRoot(true);
        changedFilesTree.setCellFactory(tv -> {
            TreeCell<Object> cell = new TreeCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("-fx-background-color: transparent;");
                        return;
                    }
                    updateChangedFileCellStyle(this);

                    if (item instanceof GitService.CommitFile cf) {
                        Label fileLabel = new Label(cf.fileName());
                        String status = cf.statusPrefix();
                        String color = switch (status) {
                            case "A" -> GitFileStatus.ADDED.getColorHex();
                            case "D" -> GitFileStatus.DELETED.getColorHex();
                            default -> GitFileStatus.MODIFIED.getColorHex();
                        };
                        fileLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px;");
                        setGraphic(fileLabel);
                        setText(null);
                    } else if (item instanceof String folderName) {
                        Label folderLabel = new Label(folderName);
                        folderLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                        setGraphic(folderLabel);
                        setText(null);
                    }
                }
            };
            cell.selectedProperty().addListener((obs, was, is) -> updateChangedFileCellStyle(cell));
            return cell;
        });

        changedFilesTree.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                openSelectedFileDiff();
            }
        });

        changedFilesTree.setOnKeyPressed(e -> {
            if ((e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.EQUALS) && e.isShortcutDown()) {
                expandAllChangedFiles();
            } else if (e.getCode() == KeyCode.MINUS && e.isShortcutDown()) {
                collapseAllChangedFiles();
            }
        });

        emptyChangesLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        changedFilesContainer.setStyle("-fx-background-color: #1E1F22;");
        changedFilesContainer.getChildren().addAll(changedFilesTree, emptyChangesLabel);
        VBox.setVgrow(changedFilesContainer, Priority.ALWAYS);

        changedFilesBox.getChildren().addAll(filesToolbar, changedFilesContainer);
        VBox.setVgrow(changedFilesBox, Priority.ALWAYS);

        // --- Bottom: Commit Details
        commitDetailsBox.setPadding(new Insets(10));
        commitDetailsBox.setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(commitDetailsBox, Priority.ALWAYS);

        emptyDetailsLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        commitDetailsContainer.setStyle("-fx-background-color: #1E1F22;");
        commitDetailsContainer.getChildren().addAll(commitDetailsBox, emptyDetailsLabel);
        VBox.setVgrow(commitDetailsContainer, Priority.ALWAYS);

        rightSplit.getItems().addAll(changedFilesBox, commitDetailsContainer);
        rightSplit.setDividerPositions(0.60);

        // Default empty states
        showEmptyRightPane();
    }

    private void expandAllChangedFiles() {
        setTreeExpanded(changedFilesTree.getRoot(), true);
    }

    private void collapseAllChangedFiles() {
        if (changedFilesTree.getRoot() == null) return;
        for (TreeItem<Object> child : changedFilesTree.getRoot().getChildren()) {
            setTreeExpanded(child, false);
        }
    }

    private void rollbackSelectedFile() {
        TreeItem<Object> selected = changedFilesTree.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() instanceof GitService.CommitFile cf) {
            Path dir = projectRoot.get();
            if (dir != null) {
                GitService.rollback(dir, List.of(cf.relativePath()));
                refresh();
            }
        }
    }

    private static void updateChangedFileCellStyle(TreeCell<?> cell) {
        if (cell.getItem() == null) {
            cell.setStyle("-fx-background-color: transparent;");
        } else if (cell.isSelected()) {
            cell.setStyle("-fx-background-color: #2E436E; -fx-background-radius: 3;");
        } else {
            cell.setStyle("-fx-background-color: transparent;");
        }
    }

    private void showEmptyRightPane() {
        changedFilesTree.setVisible(false);
        emptyChangesLabel.setVisible(true);

        commitDetailsBox.getChildren().clear();
        commitDetailsBox.setVisible(false);
        emptyDetailsLabel.setVisible(true);
    }

    private void updateRightPane(GitLogCommit commit) {
        if (commit == null) {
            showEmptyRightPane();
            return;
        }

        emptyChangesLabel.setVisible(false);
        changedFilesTree.setVisible(true);

        emptyDetailsLabel.setVisible(false);
        commitDetailsBox.setVisible(true);

        Path dir = projectRoot.get();
        if (dir == null) return;

        // Load changed files asynchronously
        Thread t = new Thread(() -> {
            List<GitService.CommitFile> files = GitService.getCommitFiles(dir, commit.hash());
            List<String> branches = GitService.getBranchesContaining(dir, commit.hash());

            Platform.runLater(() -> {
                renderChangedFiles(files, dir);
                renderCommitDetails(commit, branches);
            });
        }, "lumina-git-commit-details");
        t.setDaemon(true);
        t.start();
    }

    private void renderChangedFiles(List<GitService.CommitFile> files, Path dir) {
        String rootName = dir.getFileName() != null ? dir.getFileName().toString() : "Repository";
        TreeItem<Object> root = new TreeItem<>(rootName + "  " + files.size() + " files");
        root.setExpanded(true);

        // Group files by folder
        Map<String, TreeItem<Object>> folderMap = new HashMap<>();

        for (GitService.CommitFile cf : files) {
            String dirPath = cf.dirPath();
            if (dirPath == null || dirPath.isBlank()) {
                root.getChildren().add(new TreeItem<>(cf));
            } else {
                TreeItem<Object> folderItem = folderMap.computeIfAbsent(dirPath, dp -> {
                    TreeItem<Object> fi = new TreeItem<>(dp);
                    fi.setExpanded(true);
                    root.getChildren().add(fi);
                    return fi;
                });
                folderItem.getChildren().add(new TreeItem<>(cf));
            }
        }

        changedFilesTree.setRoot(root);
    }

    private void renderCommitDetails(GitLogCommit commit, List<String> branches) {
        commitDetailsBox.getChildren().clear();

        // Subject / full message
        Label subjectLabel = new Label(commit.fullMessage() != null && !commit.fullMessage().isBlank() ? commit.fullMessage() : commit.subject());
        subjectLabel.setWrapText(true);
        subjectLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");

        // Hash, author, email, and date
        String detailsStr = commit.shortHash() + " " + commit.authorName()
                + " <" + commit.authorEmail() + "> on " + commit.getFormattedDate();
        Label metaLabel = new Label(detailsStr);
        metaLabel.setWrapText(true);
        metaLabel.setStyle("-fx-text-fill: #8C8E94; -fx-font-size: 12px;");

        commitDetailsBox.getChildren().addAll(subjectLabel, metaLabel);

        // Branch tags & membership
        if (branches != null && !branches.isEmpty()) {
            HBox branchBadges = new HBox(4);
            branchBadges.setAlignment(Pos.CENTER_LEFT);
            for (int i = 0; i < Math.min(3, branches.size()); i++) {
                String b = branches.get(i);
                Label badge = new Label(b);
                badge.setStyle("-fx-background-color: #2E3A4D; -fx-text-fill: #70A5EB; -fx-font-size: 11px; -fx-padding: 1 5; -fx-background-radius: 3;");
                branchBadges.getChildren().add(badge);
            }
            commitDetailsBox.getChildren().add(branchBadges);

            String branchSummary = "In " + branches.size() + " branches: " + String.join(", ", branches);
            Label branchSummaryLabel = new Label(branchSummary);
            branchSummaryLabel.setWrapText(true);
            branchSummaryLabel.setStyle("-fx-text-fill: #8C8E94; -fx-font-size: 11px;");
            commitDetailsBox.getChildren().add(branchSummaryLabel);
        }
    }

    private void openSelectedFileDiff() {
        TreeItem<Object> selected = changedFilesTree.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() instanceof GitService.CommitFile cf) {
            GitLogCommit commit = commitTable.getSelectionModel().getSelectedItem();
            if (commit != null && onOpenFileDiff != null) {
                onOpenFileDiff.accept(commit.hash(), cf.relativePath());
            }
        }
    }

    private void setTreeExpanded(TreeItem<?> item, boolean expanded) {
        if (item == null) return;
        item.setExpanded(expanded);
        for (TreeItem<?> child : item.getChildren()) {
            setTreeExpanded(child, expanded);
        }
    }

    private void wireFilterEvents() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Date dropdown menu items matching screenshot media_1790132097585.png
        MenuItem selectDateItem = new MenuItem("Select...");
        selectDateItem.setOnAction(e -> {
            activeDateFilter = "Select...";
            dateMenuButton.setText("Date");
            applyFilters();
        });
        MenuItem last24hItem = new MenuItem("Last 24 hours");
        last24hItem.setOnAction(e -> {
            activeDateFilter = "Last 24 hours";
            dateMenuButton.setText("Last 24 hours");
            applyFilters();
        });
        MenuItem last7dItem = new MenuItem("Last 7 days");
        last7dItem.setOnAction(e -> {
            activeDateFilter = "Last 7 days";
            dateMenuButton.setText("Last 7 days");
            applyFilters();
        });
        dateMenuButton.getItems().addAll(selectDateItem, last24hItem, last7dItem);

        // Paths dropdown menu items
        MenuItem allPathsItem = new MenuItem("All");
        allPathsItem.setOnAction(e -> {
            pathsMenuButton.setText("Paths");
            applyFilters();
        });
        pathsMenuButton.getItems().add(allPathsItem);

        // Branch filter text field
        branchFilterField.textProperty().addListener((obs, oldVal, newVal) -> filterBranchTree(newVal));
    }

    private void filterBranchTree(String query) {
        populateBranchTree(query);
    }

    private void applyFilters() {
        String query = searchField.getText();
        boolean matchCase = matchCaseToggle.isSelected();
        long nowSeconds = Instant.now().getEpochSecond();

        filteredCommitList.setPredicate(commit -> {
            if (commit == null) return false;

            // Search query filter
            if (query != null && !query.isBlank()) {
                if (!commit.matches(query, matchCase)) return false;
            }

            // User filter
            if (!"All".equalsIgnoreCase(activeUserFilter)) {
                if (!activeUserFilter.equalsIgnoreCase(commit.authorName())) return false;
            }

            // Date filter
            if ("Last 24 hours".equalsIgnoreCase(activeDateFilter)) {
                long diffSeconds = nowSeconds - commit.timestamp();
                if (diffSeconds > 86400) return false;
            } else if ("Last 7 days".equalsIgnoreCase(activeDateFilter)) {
                long diffSeconds = nowSeconds - commit.timestamp();
                if (diffSeconds > 7 * 86400) return false;
            }

            return true;
        });

        // Recompute graph for visible filtered commits
        graphData = GitLogGraph.compute(new ArrayList<>(filteredCommitList));
        commitTable.refresh();
    }

    public void refresh() {
        Path dir = projectRoot.get();
        if (dir == null || !GitService.isRepository(dir)) {
            masterCommitList.clear();
            showEmptyRightPane();
            consoleArea.setText("Not a git repository");
            return;
        }

        Thread t = new Thread(() -> {
            List<String> localBranches = GitService.localBranches(dir);
            List<String> remoteBranches = GitService.remoteBranches(dir);
            String currentBranch = GitService.currentBranch(dir);

            // Fetch commits
            List<GitLogCommit> commits = GitService.getLogCommits(dir, 1000, activeBranchFilter);

            // Fetch recent git operations log for Console tab
            GitService.Result logResult = GitService.exec(dir, "log", "-20", "--oneline");
            GitService.Result statusResult = GitService.status(dir);

            StringBuilder consoleText = new StringBuilder();
            consoleText.append("# Git Status:\n").append(statusResult.output()).append("\n\n");
            consoleText.append("# Recent Commits:\n").append(logResult.output());

            Platform.runLater(() -> {
                populateBranchHierarchy(currentBranch, localBranches, remoteBranches);
                populateFilterMenus(commits, localBranches, remoteBranches);

                masterCommitList.setAll(commits);
                graphData = GitLogGraph.compute(commits);
                applyFilters();

                consoleArea.setText(consoleText.toString());

                if (!commitTable.getItems().isEmpty()) {
                    commitTable.getSelectionModel().select(0);
                } else {
                    showEmptyRightPane();
                }
            });
        }, "lumina-git-log-refresh");
        t.setDaemon(true);
        t.start();
    }

    private void populateBranchHierarchy(String currentBranch, List<String> localBranches, List<String> remoteBranches) {
        this.cachedLocalBranches = localBranches;
        this.cachedRemoteBranches = remoteBranches;
        this.cachedCurrentBranch = currentBranch;
        populateBranchTree(branchFilterField.getText());
    }

    private List<String> cachedLocalBranches = List.of();
    private List<String> cachedRemoteBranches = List.of();
    private String cachedCurrentBranch = "master";

    private void populateBranchTree(String filter) {
        TreeItem<String> root = new TreeItem<>("Root");

        // HEAD
        TreeItem<String> headItem = new TreeItem<>("HEAD (Current Branch)");
        root.getChildren().add(headItem);

        // Local
        TreeItem<String> localItem = new TreeItem<>("Local");
        localItem.setExpanded(true);
        for (String b : cachedLocalBranches) {
            if (filter == null || filter.isBlank() || b.toLowerCase().contains(filter.toLowerCase())) {
                String displayName = favoriteBranches.contains(b) ? "★ " + b : b;
                localItem.getChildren().add(new TreeItem<>(displayName));
            }
        }
        root.getChildren().add(localItem);

        // Remote
        TreeItem<String> remoteItem = new TreeItem<>("Remote");
        remoteItem.setExpanded(true);

        Map<String, TreeItem<String>> remoteGroups = new LinkedHashMap<>();
        for (String rb : cachedRemoteBranches) {
            if (rb == null || rb.isBlank() || rb.endsWith("/HEAD")) continue;
            String remoteName = "origin";
            String branchName = rb;
            int slash = rb.indexOf('/');
            if (slash > 0) {
                remoteName = rb.substring(0, slash);
                branchName = rb.substring(slash + 1);
            }
            if (branchName.equals(remoteName) || branchName.equals("HEAD")) continue;

            if (filter == null || filter.isBlank() || branchName.toLowerCase().contains(filter.toLowerCase())) {
                TreeItem<String> rGroup = remoteGroups.computeIfAbsent(remoteName, rn -> {
                    TreeItem<String> item = new TreeItem<>(rn);
                    item.setExpanded(true);
                    remoteItem.getChildren().add(item);
                    return item;
                });
                String displayName = favoriteBranches.contains(branchName) ? "★ " + branchName : branchName;
                rGroup.getChildren().add(new TreeItem<>(displayName));
            }
        }
        root.getChildren().add(remoteItem);

        branchTree.setRoot(root);
    }

    private void populateFilterMenus(List<GitLogCommit> commits, List<String> localBranches, List<String> remoteBranches) {
        // Branch menu
        branchMenuButton.getItems().clear();
        MenuItem allBranchItem = new MenuItem("All");
        allBranchItem.setOnAction(e -> {
            activeBranchFilter = "All";
            branchMenuButton.setText("Branch");
            refresh();
        });
        MenuItem headBranchItem = new MenuItem("HEAD (Current Branch)");
        headBranchItem.setOnAction(e -> {
            activeBranchFilter = "HEAD";
            branchMenuButton.setText("HEAD");
            refresh();
        });
        branchMenuButton.getItems().addAll(allBranchItem, headBranchItem);

        for (String b : localBranches) {
            MenuItem mi = new MenuItem(b);
            mi.setOnAction(e -> {
                activeBranchFilter = b;
                branchMenuButton.setText(b);
                refresh();
            });
            branchMenuButton.getItems().add(mi);
        }

        // User menu
        userMenuButton.getItems().clear();
        MenuItem allUserItem = new MenuItem("All");
        allUserItem.setOnAction(e -> {
            activeUserFilter = "All";
            userMenuButton.setText("User");
            applyFilters();
        });
        userMenuButton.getItems().add(allUserItem);

        Set<String> authors = new LinkedHashSet<>();
        for (GitLogCommit c : commits) {
            if (c.authorName() != null && !c.authorName().isBlank()) {
                authors.add(c.authorName());
            }
        }
        for (String author : authors) {
            MenuItem mi = new MenuItem(author);
            mi.setOnAction(e -> {
                activeUserFilter = author;
                userMenuButton.setText(author);
                applyFilters();
            });
            userMenuButton.getItems().add(mi);
        }
    }
}