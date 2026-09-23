package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * Modern IDE "Git Branches" popup widget and window matching reference design:
 * - Search bar: "Search for branches and actions"
 * - Actions: Update Project... (Ctrl+T), Commit... (Ctrl+K), Push... (Ctrl+Shift+K)
 * - Branch creation: + New Branch... (Ctrl+Alt+N), Checkout Tag or Revision...
 * - Grouped lists: Recent, Local, Remote (origin/...) with outgoing badges ↗4
 * - Full submenus for Local Branches and Remote Branches
 */
public class GitBranchesPopup extends Popup {

    public interface BranchCallbacks {
        void onUpdateProject();
        void onCommit();
        void onPush();
        void onNewBranch();
        void onCheckoutTag();
        void onBranchChanged();
        default void onMergeBranch(String branch) {}
        default void onRebaseBranch(String target) {}
    }

    private final java.util.function.Supplier<Path> projectRootSupplier;
    private final Consumer<String> log;
    private final BranchCallbacks callbacks;

    private final TextField searchField = new TextField();
    private final VBox contentBox = new VBox(2);
    private final ScrollPane scrollPane = new ScrollPane(contentBox);

    private String currentBranch = "master";
    private final List<String> localBranches = new ArrayList<>();
    private final List<String> remoteBranches = new ArrayList<>();
    private final Set<String> recentBranches = new LinkedHashSet<>();
    private final Set<String> favoriteBranches = new HashSet<>();
    private final Map<String, Integer> outgoingCounts = new HashMap<>();

    // Submenu popup
    private final ContextMenu subMenu = new ContextMenu();

    public GitBranchesPopup(java.util.function.Supplier<Path> projectRootSupplier, Consumer<String> log, BranchCallbacks callbacks) {
        this.projectRootSupplier = projectRootSupplier;
        this.log = log;
        this.callbacks = callbacks;

        setAutoHide(true);
        setHideOnEscape(true);

        VBox root = new VBox(4);
        root.setPrefWidth(360);
        root.setMaxHeight(500);
        root.setPadding(new Insets(6, 6, 6, 6));
        root.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2B2D30; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 12, 0, 0, 4);");

        // 1. Search Bar at Top
        HBox searchBox = buildSearchBar();
        root.getChildren().add(searchBox);

        // 2. Scrollable Action & Branch List
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        root.getChildren().add(scrollPane);

        getContent().add(root);

        searchField.textProperty().addListener((obs, old, text) -> populateList(text.trim().toLowerCase()));
    }

    public GitBranchesPopup(Path projectRoot, Consumer<String> log, BranchCallbacks callbacks) {
        this(() -> projectRoot, log, callbacks);
    }

    public Path getProjectRoot() {
        return projectRootSupplier != null ? projectRootSupplier.get() : null;
    }

    public void toggleBelow(Node anchor) {
        if (isShowing()) {
            hide();
        } else {
            showBelow(anchor);
        }
    }

    public void showBelow(Node anchor) {
        loadGitData();
        searchField.clear();
        populateList("");

        Window window = anchor.getScene() != null ? anchor.getScene().getWindow() : null;
        if (window != null) {
            javafx.geometry.Point2D p = anchor.localToScreen(0, anchor.getBoundsInLocal().getHeight() + 4);
            show(window, p.getX(), p.getY());
        }
        Platform.runLater(searchField::requestFocus);
    }

    public void showCentered(Window window) {
        loadGitData();
        searchField.clear();
        populateList("");

        if (window != null) {
            double x = window.getX() + Math.max(10, (window.getWidth() - 360) / 2.0);
            double y = window.getY() + Math.max(60, (window.getHeight() - 500) / 3.0);
            show(window, x, y);
        }
        Platform.runLater(searchField::requestFocus);
    }

    private void loadGitData() {
        Path root = getProjectRoot();
        if (root == null || !GitService.isRepository(root)) return;

        currentBranch = GitService.currentBranch(root);
        if (currentBranch == null || currentBranch.isBlank()) currentBranch = "master";

        localBranches.clear();
        localBranches.addAll(GitService.localBranches(root));
        if (localBranches.isEmpty()) localBranches.add(currentBranch);

        remoteBranches.clear();
        remoteBranches.addAll(GitService.remoteBranches(root));

        outgoingCounts.clear();
        for (String b : localBranches) {
            int count = GitService.unpushedCommitsCount(root, b);
            if (count > 0) outgoingCounts.put(b, count);
        }

        if (!recentBranches.contains(currentBranch)) {
            recentBranches.add(currentBranch);
        }
        for (String b : localBranches) {
            if (recentBranches.size() < 4) {
                recentBranches.add(b);
            }
        }
    }

    // ----------------------------------------------------------------- Search Bar

    private HBox buildSearchBar() {
        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        searchField.setPromptText("Search for branches and actions");
        searchField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #868A91; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px; -fx-padding: 4 6 4 6;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button resizeBtn = new Button("\u2922");
        resizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 11px; -fx-padding: 2 4; -fx-cursor: hand;");

        Button settingsBtn = new Button("⚙");
        settingsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 12px; -fx-padding: 2 4; -fx-cursor: hand;");

        HBox box = new HBox(6, searchIcon, searchField, resizeBtn, settingsBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(2, 4, 6, 4));
        box.setStyle("-fx-border-color: transparent transparent #2B2D30 transparent; -fx-border-width: 0 0 1 0;");
        return box;
    }

    // ----------------------------------------------------------------- List Population

    private void populateList(String filter) {
        contentBox.getChildren().clear();

        // 1. Top Actions
        boolean matchUpdate = filter.isEmpty() || "update project".contains(filter);
        boolean matchCommit = filter.isEmpty() || "commit".contains(filter);
        boolean matchPush = filter.isEmpty() || "push".contains(filter);

        if (matchUpdate) {
            contentBox.getChildren().add(buildActionRow("⤓", "Update Project\u2026", "⌘T", e -> {
                hide();
                if (callbacks != null) callbacks.onUpdateProject();
            }));
        }
        if (matchCommit) {
            contentBox.getChildren().add(buildActionRow("✓", "Commit\u2026", "⌘K", e -> {
                hide();
                if (callbacks != null) callbacks.onCommit();
            }));
        }
        if (matchPush) {
            contentBox.getChildren().add(buildActionRow("⤉", "Push\u2026", "⇧⌘K", e -> {
                hide();
                if (callbacks != null) callbacks.onPush();
            }));
        }

        if (matchUpdate || matchCommit || matchPush) {
            contentBox.getChildren().add(createSeparator());
        }

        // 2. Branch creation
        boolean matchNewBranch = filter.isEmpty() || "new branch".contains(filter);
        boolean matchTag = filter.isEmpty() || "tag".contains(filter) || "revision".contains(filter) || "checkout".contains(filter);

        if (matchNewBranch) {
            contentBox.getChildren().add(buildActionRow("+", "New Branch\u2026", "⌥⌘N", e -> {
                hide();
                if (callbacks != null) callbacks.onNewBranch();
            }));
        }
        if (matchTag) {
            contentBox.getChildren().add(buildActionRow(" ", "Checkout Tag or Revision\u2026", "", e -> {
                hide();
                if (callbacks != null) callbacks.onCheckoutTag();
            }));
        }

        if (matchNewBranch || matchTag) {
            contentBox.getChildren().add(createSeparator());
        }

        // 3. Recent Section
        List<String> matchingRecent = recentBranches.stream()
                .filter(b -> filter.isEmpty() || b.toLowerCase().contains(filter))
                .toList();

        if (!matchingRecent.isEmpty()) {
            contentBox.getChildren().add(createCategoryHeader("Recent"));
            for (String b : matchingRecent) {
                contentBox.getChildren().add(buildLocalBranchRow(b));
            }
        }

        // 4. Local Section
        List<String> matchingLocal = localBranches.stream()
                .filter(b -> filter.isEmpty() || b.toLowerCase().contains(filter))
                .toList();

        if (!matchingLocal.isEmpty()) {
            contentBox.getChildren().add(createCategoryHeader("Local"));
            for (String b : matchingLocal) {
                contentBox.getChildren().add(buildLocalBranchRow(b));
            }
        }

        // 5. Remote Section
        List<String> matchingRemote = remoteBranches.stream()
                .filter(b -> filter.isEmpty() || b.toLowerCase().contains(filter))
                .toList();

        if (!matchingRemote.isEmpty()) {
            contentBox.getChildren().add(createCategoryHeader("Remote"));
            contentBox.getChildren().add(createSubCategoryHeader("origin"));
            for (String rb : matchingRemote) {
                String cleanName = rb.startsWith("origin/") ? rb.substring(7) : rb;
                contentBox.getChildren().add(buildRemoteBranchRow("origin", cleanName, rb));
            }
        }
    }

    // ----------------------------------------------------------------- Rows & Submenus

    private HBox buildActionRow(String iconStr, String text, String shortcut, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        Button btn = new Button();
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        btn.setMaxWidth(Double.MAX_VALUE);

        Label icon = new Label(iconStr);
        icon.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 12px; -fx-min-width: 18;");

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label sc = new Label(shortcut);
        sc.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        HBox row = new HBox(6, icon, label, spacer, sc);
        row.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphic(row);
        btn.setOnAction(onAction);

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2E436E; -fx-cursor: hand; -fx-padding: 4 8 4 8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 8 4 8;"));

        HBox wrapper = new HBox(btn);
        HBox.setHgrow(btn, Priority.ALWAYS);
        return wrapper;
    }

    private HBox buildLocalBranchRow(String branch) {
        boolean isCurrent = branch.equals(currentBranch);
        Button btn = new Button();
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        btn.setMaxWidth(Double.MAX_VALUE);

        Node icon = isCurrent ? createCurrentBranchCheckIcon() : createBranchForkIcon();

        Label name = new Label(branch);
        name.setStyle("-fx-text-fill: " + (isCurrent ? "#FFFFFF; -fx-font-weight: bold;" : "#DFE1E5;") + " -fx-font-size: 12px;");

        HBox left = new HBox(6, icon, name);
        left.setAlignment(Pos.CENTER_LEFT);

        int outCount = outgoingCounts.getOrDefault(branch, 0);
        if (outCount > 0) {
            Label badge = new Label("\u2197" + outCount);
            badge.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 11px; -fx-font-weight: bold;");
            left.getChildren().add(badge);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label tracking = new Label("origin/" + branch + " >");
        tracking.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        HBox row = new HBox(6, left, spacer, tracking);
        row.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphic(row);

        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-background-color: #2E436E; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
            showLocalBranchMenu(btn, branch);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        });
        btn.setOnAction(e -> showLocalBranchMenu(btn, branch));

        HBox wrapper = new HBox(btn);
        HBox.setHgrow(btn, Priority.ALWAYS);
        return wrapper;
    }

    private HBox buildRemoteBranchRow(String remote, String branchName, String fullRemoteBranch) {
        boolean isMaster = "master".equals(branchName) || "main".equals(branchName);
        boolean isFav = favoriteBranches.contains(fullRemoteBranch) || isMaster;

        Button btn = new Button();
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 8 4 20;");
        btn.setMaxWidth(Double.MAX_VALUE);

        Label icon = new Label(isFav ? "★" : "⑂");
        icon.setStyle("-fx-text-fill: " + (isFav ? "#E8B450;" : "#868A91;") + " -fx-font-size: 11px;");

        Label name = new Label(branchName);
        name.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label arrow = new Label(">");
        arrow.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        HBox row = new HBox(6, icon, name, spacer, arrow);
        row.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphic(row);

        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-background-color: #2E436E; -fx-cursor: hand; -fx-padding: 4 8 4 20;");
            showRemoteBranchMenu(btn, remote, branchName, fullRemoteBranch);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 8 4 20;");
        });
        btn.setOnAction(e -> showRemoteBranchMenu(btn, remote, branchName, fullRemoteBranch));

        HBox wrapper = new HBox(btn);
        HBox.setHgrow(btn, Priority.ALWAYS);
        return wrapper;
    }

    // ----------------------------------------------------------------- Submenus

    private void showLocalBranchMenu(Button anchor, String branch) {
        subMenu.hide();
        subMenu.getItems().clear();
        subMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5;");

        boolean isCurrent = branch.equals(currentBranch);

        MenuItem checkoutItem = new MenuItem("Checkout");
        checkoutItem.setDisable(isCurrent);
        checkoutItem.setOnAction(e -> doCheckout(branch));

        MenuItem newBranchFromItem = new MenuItem("New Branch from '" + branch + "'\u2026");
        newBranchFromItem.setOnAction(e -> promptNewBranchFrom(branch));

        MenuItem checkoutRebaseItem = new MenuItem("Checkout and Rebase onto '" + currentBranch + "'");
        checkoutRebaseItem.setDisable(isCurrent);
        checkoutRebaseItem.setOnAction(e -> {
            hide();
            new Thread(() -> {
                GitService.Result r1 = GitService.checkout(getProjectRoot(), branch);
                if (r1.ok()) {
                    GitService.Result r2 = GitService.rebaseOnto(getProjectRoot(), currentBranch);
                    Platform.runLater(() -> {
                        notifyResult("Rebase", r2);
                        if (callbacks != null) callbacks.onBranchChanged();
                    });
                } else {
                    Platform.runLater(() -> notifyResult("Checkout", r1));
                }
            }, "lumina-git-checkout-rebase").start();
        });

        MenuItem compareItem = new MenuItem("Compare with '" + currentBranch + "'");
        compareItem.setDisable(isCurrent);
        compareItem.setOnAction(e -> {
            hide();
            if (log != null) log.accept("Comparing " + branch + " with " + currentBranch);
        });

        MenuItem showDiffItem = new MenuItem("Show Diff with Working Tree");
        showDiffItem.setOnAction(e -> {
            hide();
            if (log != null) log.accept("Showing diff for " + branch + " with working tree");
        });

        MenuItem rebaseOntoItem = new MenuItem("Rebase '" + currentBranch + "' onto '" + branch + "'");
        rebaseOntoItem.setDisable(isCurrent);
        rebaseOntoItem.setOnAction(e -> {
            hide();
            if (callbacks != null) {
                callbacks.onRebaseBranch(branch);
            }
        });

        MenuItem mergeItem = new MenuItem("Merge '" + branch + "' into '" + currentBranch + "'");
        mergeItem.setDisable(isCurrent);
        mergeItem.setOnAction(e -> {
            hide();
            if (callbacks != null) {
                callbacks.onMergeBranch(branch);
            }
        });

        MenuItem updateItem = new MenuItem("Update");
        updateItem.setOnAction(e -> {
            hide();
            if (callbacks != null) callbacks.onUpdateProject();
        });

        MenuItem pushItem = new MenuItem("Push\u2026");
        pushItem.setOnAction(e -> {
            hide();
            if (callbacks != null) callbacks.onPush();
        });

        MenuItem renameItem = new MenuItem("Rename\u2026");
        renameItem.setOnAction(e -> promptRenameBranch(branch));

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setDisable(isCurrent);
        deleteItem.setOnAction(e -> confirmDeleteBranch(branch));

        subMenu.getItems().addAll(
                checkoutItem, newBranchFromItem, checkoutRebaseItem,
                new SeparatorMenuItem(),
                compareItem, showDiffItem,
                new SeparatorMenuItem(),
                rebaseOntoItem, mergeItem,
                new SeparatorMenuItem(),
                updateItem, pushItem,
                new SeparatorMenuItem(),
                renameItem, deleteItem
        );

        subMenu.show(anchor, Side.RIGHT, 0, 0);
    }

    private void showRemoteBranchMenu(Button anchor, String remote, String branchName, String fullRemoteBranch) {
        subMenu.hide();
        subMenu.getItems().clear();
        subMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5;");

        MenuItem checkoutItem = new MenuItem("Checkout");
        checkoutItem.setOnAction(e -> doCheckout(fullRemoteBranch));

        MenuItem newBranchFromItem = new MenuItem("New Branch from '" + fullRemoteBranch + "'\u2026");
        newBranchFromItem.setOnAction(e -> promptNewBranchFrom(fullRemoteBranch));

        MenuItem checkoutRebaseItem = new MenuItem("Checkout and Rebase onto '" + currentBranch + "'");
        checkoutRebaseItem.setOnAction(e -> {
            hide();
            new Thread(() -> {
                GitService.Result r1 = GitService.checkout(getProjectRoot(), fullRemoteBranch);
                if (r1.ok()) {
                    GitService.Result r2 = GitService.rebaseOnto(getProjectRoot(), currentBranch);
                    Platform.runLater(() -> {
                        notifyResult("Rebase", r2);
                        if (callbacks != null) callbacks.onBranchChanged();
                    });
                } else {
                    Platform.runLater(() -> notifyResult("Checkout", r1));
                }
            }, "lumina-git-checkout-rebase").start();
        });

        MenuItem compareItem = new MenuItem("Compare with '" + currentBranch + "'");
        compareItem.setOnAction(e -> {
            hide();
            if (log != null) log.accept("Comparing " + fullRemoteBranch + " with " + currentBranch);
        });

        MenuItem showDiffItem = new MenuItem("Show Diff with Working Tree");
        showDiffItem.setOnAction(e -> {
            hide();
            if (log != null) log.accept("Showing diff for " + fullRemoteBranch + " with working tree");
        });

        MenuItem rebaseOntoItem = new MenuItem("Rebase '" + currentBranch + "' onto '" + fullRemoteBranch + "'");
        rebaseOntoItem.setOnAction(e -> {
            hide();
            if (callbacks != null) {
                callbacks.onRebaseBranch(fullRemoteBranch);
            }
        });

        MenuItem mergeItem = new MenuItem("Merge '" + fullRemoteBranch + "' into '" + currentBranch + "'");
        mergeItem.setOnAction(e -> {
            hide();
            if (callbacks != null) {
                callbacks.onMergeBranch(fullRemoteBranch);
            }
        });

        MenuItem pullRebaseItem = new MenuItem("Pull into '" + currentBranch + "' Using Rebase");
        pullRebaseItem.setOnAction(e -> {
            hide();
            new Thread(() -> {
                GitService.Result r = GitService.pullIntoCurrent(getProjectRoot(), fullRemoteBranch, true);
                Platform.runLater(() -> {
                    notifyResult("Pull (Rebase)", r);
                    if (r.ok() && callbacks != null) callbacks.onBranchChanged();
                });
            }, "lumina-git-pull").start();
        });

        MenuItem pullMergeItem = new MenuItem("Pull into '" + currentBranch + "' Using Merge");
        pullMergeItem.setOnAction(e -> {
            hide();
            new Thread(() -> {
                GitService.Result r = GitService.pullIntoCurrent(getProjectRoot(), fullRemoteBranch, false);
                Platform.runLater(() -> {
                    notifyResult("Pull (Merge)", r);
                    if (r.ok() && callbacks != null) callbacks.onBranchChanged();
                });
            }, "lumina-git-pull").start();
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> confirmDeleteRemoteBranch(remote, branchName));

        subMenu.getItems().addAll(
                checkoutItem, newBranchFromItem, checkoutRebaseItem,
                new SeparatorMenuItem(),
                compareItem, showDiffItem,
                new SeparatorMenuItem(),
                rebaseOntoItem, mergeItem,
                new SeparatorMenuItem(),
                pullRebaseItem, pullMergeItem,
                new SeparatorMenuItem(),
                deleteItem
        );

        subMenu.show(anchor, Side.RIGHT, 0, 0);
    }

    // ----------------------------------------------------------------- Branch Actions

    private void doCheckout(String target) {
        hide();
        new Thread(() -> {
            GitService.Result r = GitService.checkout(getProjectRoot(), target);
            Platform.runLater(() -> {
                if (r.ok()) {
                    if (log != null) log.accept("\u2713 Switched to branch '" + target + "'");
                    if (callbacks != null) callbacks.onBranchChanged();
                } else {
                    if (log != null) log.accept("Checkout failed: " + r.output().trim());
                }
            });
        }, "lumina-git-checkout").start();
    }

    private void promptNewBranchFrom(String baseBranch) {
        hide();
        Window owner = getOwnerWindow();
        CreateBranchDialog dialog = new CreateBranchDialog(owner, getProjectRoot(), baseBranch, newBranch -> {
            if (log != null) log.accept("\u2713 Created branch '" + newBranch + "' from '" + baseBranch + "'");
            if (callbacks != null) callbacks.onBranchChanged();
        });
        dialog.show();
    }

    private void promptRenameBranch(String oldName) {
        hide();
        TextInputDialog dialog = new TextInputDialog(oldName);
        dialog.setTitle("Rename Branch");
        dialog.setHeaderText("Rename branch '" + oldName + "'");
        dialog.setContentText("New branch name:");
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
        dialog.showAndWait().ifPresent(newName -> {
            newName = newName.trim();
            if (!newName.isBlank() && !newName.equals(oldName)) {
                GitService.Result r = GitService.renameBranch(getProjectRoot(), oldName, newName);
                notifyResult("Rename Branch", r);
                if (r.ok() && callbacks != null) callbacks.onBranchChanged();
            }
        });
    }

    private void confirmDeleteBranch(String branch) {
        hide();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Branch");
        alert.setHeaderText("Delete local branch '" + branch + "'?");
        alert.setContentText("This branch will be permanently removed.");
        alert.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                GitService.Result r = GitService.deleteBranch(getProjectRoot(), branch, true);
                notifyResult("Delete Branch", r);
                if (r.ok() && callbacks != null) callbacks.onBranchChanged();
            }
        });
    }

    private void confirmDeleteRemoteBranch(String remote, String branch) {
        hide();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Remote Branch");
        alert.setHeaderText("Delete remote branch '" + remote + "/" + branch + "'?");
        alert.setContentText("This will remove the branch from the remote repository.");
        alert.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                GitService.Result r = GitService.deleteRemoteBranch(getProjectRoot(), remote, branch);
                notifyResult("Delete Remote Branch", r);
                if (r.ok() && callbacks != null) callbacks.onBranchChanged();
            }
        });
    }

    private void notifyResult(String action, GitService.Result r) {
        if (r.ok()) {
            if (log != null) log.accept("\u2713 " + action + " succeeded: " + r.output().trim());
        } else {
            if (log != null) log.accept(action + " failed: " + r.output().trim());
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle(action + " Failed");
            err.setHeaderText("Git " + action + " Error");
            err.setContentText(r.output().trim());
            err.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
            err.showAndWait();
        }
    }

    // ----------------------------------------------------------------- Visual Helpers

    private Separator createSeparator() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2B2D30; -fx-padding: 2 0 2 0;");
        return sep;
    }

    private Label createCategoryHeader(String title) {
        Label l = new Label("▾ " + title);
        l.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8 2 8;");
        return l;
    }

    private Label createSubCategoryHeader(String title) {
        Label l = new Label("  ▾ " + title);
        l.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-padding: 2 8 2 14;");
        return l;
    }

    private Node createBranchForkIcon() {
        Label l = new Label("⑂");
        l.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");
        return l;
    }

    private Node createCurrentBranchCheckIcon() {
        Label l = new Label("✓");
        l.setStyle("-fx-text-fill: #3574F0; -fx-font-weight: bold; -fx-font-size: 11px;");
        return l;
    }

    // Accessors for testing
    public TextField getSearchField() { return searchField; }
    public VBox getContentBox() { return contentBox; }
    public String getCurrentBranch() { return currentBranch; }
    public List<String> getLocalBranches() { return localBranches; }
    public List<String> getRemoteBranches() { return remoteBranches; }
    public Set<String> getRecentBranches() { return recentBranches; }
    public Map<String, Integer> getOutgoingCounts() { return outgoingCounts; }
    public ContextMenu getSubMenu() { return subMenu; }
}
