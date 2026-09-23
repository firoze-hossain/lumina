package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.*;

/**
 * Modern Git Pull dialog supporting dynamic remote & branch selection,
 * 'Modify options' pull flags configuration, branch refreshing (⌘R / Ctrl+R),
 * and direct command execution.
 */
public class PullDialog extends Stage {

    public enum PullOption {
        REBASE("Rebase incoming changes on top of the current branch", "--rebase"),
        FF_ONLY("Merge only if it can be fast-forwarded", "--ff-only"),
        NO_FF("Create a merge commit even if it can be fast-forwarded", "--no-ff"),
        SQUASH("Create a single commit for all pulled changes", "--squash"),
        NO_COMMIT("Merge, but do not commit the result", "--no-commit"),
        NO_VERIFY("Bypass the pre-merge and commit message hooks", "--no-verify");

        private final String description;
        private final String flag;

        PullOption(String description, String flag) {
            this.description = description;
            this.flag = flag;
        }

        public String getDescription() {
            return description;
        }

        public String getFlag() {
            return flag;
        }
    }

    @FunctionalInterface
    public interface PullConsumer {
        void accept(String remote, String branch, List<String> options);
    }

    private final Path projectRoot;
    private final PullConsumer onPull;

    private final ComboBox<String> remoteCombo = new ComboBox<>();
    private final ComboBox<String> branchCombo = new ComboBox<>();
    private final FlowPane chipsPane = new FlowPane(6, 6);
    private final Label refreshHintLabel = new Label("Press ⌘R to update branches");
    private final Button pullBtn = new Button("Pull");
    private final Button cancelBtn = new Button("Cancel");
    private final Button modifyOptionsBtn = new Button("Modify options ▾");
    private final Button helpBtn = new Button("?");

    private final Set<PullOption> activeOptions = new LinkedHashSet<>();
    private String currentBranch;

    public PullDialog(Window owner, Path projectRoot, PullConsumer onPull) {
        this.projectRoot = projectRoot;
        this.onPull = onPull;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        currentBranch = projectRoot != null ? GitService.currentBranch(projectRoot) : null;
        if (currentBranch == null || currentBranch.isBlank()) {
            currentBranch = "HEAD";
        }
        setTitle("Pull to " + currentBranch);
        setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        root.setPrefWidth(480);

        // 1. Command bar row: [git pull] [remote ▾] [branch ▾]
        Label gitPullBadge = new Label("git pull");
        gitPullBadge.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10; -fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");

        remoteCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        remoteCombo.setPrefWidth(100);

        branchCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        HBox.setHgrow(branchCombo, Priority.ALWAYS);

        populateRemotesAndBranches();

        remoteCombo.setOnAction(e -> updateBranchesForSelectedRemote());

        HBox commandRow = new HBox(8, gitPullBadge, remoteCombo, branchCombo);
        commandRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Active option chips pane
        chipsPane.setAlignment(Pos.CENTER_LEFT);
        chipsPane.setVisible(false);
        chipsPane.setManaged(false);

        // 3. Bottom bar: [?] [Modify options ▾] ... spacer ... [Cancel] [Pull]
        helpBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-weight: bold; -fx-min-width: 24; -fx-min-height: 24; -fx-cursor: hand;");
        helpBtn.setTooltip(new Tooltip("Pull changes from remote repository and merge/rebase into current branch"));

        modifyOptionsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3574F0; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 4 4;");
        modifyOptionsBtn.setOnMouseEntered(e -> modifyOptionsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B9BF5; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 4 4; -fx-underline: true;"));
        modifyOptionsBtn.setOnMouseExited(e -> modifyOptionsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3574F0; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 4 4; -fx-underline: false;"));
        modifyOptionsBtn.setOnAction(e -> showModifyOptionsMenu(modifyOptionsBtn));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5; -fx-padding: 6 16; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand; -fx-font-size: 13px;");
        cancelBtn.setOnAction(e -> close());

        pullBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 6 20; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-size: 13px;");
        pullBtn.setOnAction(e -> doPull());

        HBox bottomBar = new HBox(8, helpBtn, modifyOptionsBtn, spacer, cancelBtn, pullBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(6, 0, 0, 0));

        // Footer hint for branches refresh
        refreshHintLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");

        root.getChildren().addAll(commandRow, chipsPane, refreshHintLabel, bottomBar);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());

        // Keyboard shortcuts: Enter -> Pull, Escape -> Close, Cmd/Ctrl+R -> Refresh branches
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                doPull();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            } else if (e.isShortcutDown() && e.getCode() == KeyCode.R) {
                refreshBranches();
                e.consume();
            }
        });

        setScene(scene);
    }

    private void populateRemotesAndBranches() {
        if (projectRoot == null) return;
        List<String> remotes = GitService.remotes(projectRoot);
        if (remotes.isEmpty()) {
            remotes = List.of("origin");
        }
        remoteCombo.getItems().setAll(remotes);
        remoteCombo.setValue(remotes.contains("origin") ? "origin" : remotes.get(0));

        updateBranchesForSelectedRemote();
    }

    public void updateBranchesForSelectedRemote() {
        if (projectRoot == null) return;
        String remote = remoteCombo.getValue();
        List<String> branches = GitService.remoteBranchesForRemote(projectRoot, remote);

        if (branches.isEmpty()) {
            // Fallback to local branches if no remote branches tracked yet
            branches = GitService.localBranches(projectRoot);
        }
        if (branches.isEmpty() && currentBranch != null) {
            branches = List.of(currentBranch);
        }

        branchCombo.getItems().setAll(branches);
        if (branches.contains(currentBranch)) {
            branchCombo.setValue(currentBranch);
        } else if (!branches.isEmpty()) {
            branchCombo.setValue(branches.get(0));
        }
    }

    public void refreshBranches() {
        refreshHintLabel.setText("Updating branches\u2026");
        Thread t = new Thread(() -> {
            GitService.exec(projectRoot, "remote", "update", "--prune");
            Platform.runLater(() -> {
                updateBranchesForSelectedRemote();
                refreshHintLabel.setText("Press ⌘R to update branches");
            });
        }, "lumina-git-pull-refresh-branches");
        t.setDaemon(true);
        t.start();
    }

    private void showModifyOptionsMenu(Button anchor) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("git-tool-options-menu");
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-padding: 4 0;");

        // Menu header label
        Label headerLabel = new Label("Add Pull Options");
        headerLabel.setStyle("-fx-text-fill: #8C8E94; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 12 4 12;");
        CustomMenuItem headerItem = new CustomMenuItem(headerLabel, false);
        menu.getItems().addAll(headerItem, new SeparatorMenuItem());

        for (PullOption opt : PullOption.values()) {
            HBox itemBox = new HBox(12);
            itemBox.setAlignment(Pos.CENTER_LEFT);

            Label checkMark = new Label(activeOptions.contains(opt) ? "✓" : " ");
            checkMark.setStyle("-fx-text-fill: #3574F0; -fx-font-weight: bold; -fx-min-width: 14;");

            Label desc = new Label(opt.getDescription());
            desc.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            HBox.setHgrow(desc, Priority.ALWAYS);

            Label flag = new Label(opt.getFlag());
            flag.setStyle("-fx-text-fill: #8C8E94; -fx-font-size: 11px; -fx-font-family: monospace;");

            itemBox.getChildren().addAll(checkMark, desc, flag);

            CustomMenuItem mi = new CustomMenuItem(itemBox, false);
            mi.setOnAction(e -> {
                toggleOption(opt);
                // Keep menu or re-show
            });
            menu.getItems().add(mi);
        }

        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 2);
    }

    public void toggleOption(PullOption opt) {
        if (activeOptions.contains(opt)) {
            activeOptions.remove(opt);
        } else {
            // Handle mutually exclusive options
            if (opt == PullOption.REBASE) {
                activeOptions.remove(PullOption.FF_ONLY);
                activeOptions.remove(PullOption.NO_FF);
            } else if (opt == PullOption.FF_ONLY) {
                activeOptions.remove(PullOption.REBASE);
                activeOptions.remove(PullOption.NO_FF);
            } else if (opt == PullOption.NO_FF) {
                activeOptions.remove(PullOption.REBASE);
                activeOptions.remove(PullOption.FF_ONLY);
            }
            activeOptions.add(opt);
        }
        rebuildChips();
    }

    private void rebuildChips() {
        chipsPane.getChildren().clear();
        for (PullOption opt : activeOptions) {
            HBox chip = new HBox(4);
            chip.setAlignment(Pos.CENTER);
            chip.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 2 8;");

            Label lbl = new Label(opt.getFlag());
            lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-font-family: monospace;");

            Button removeBtn = new Button("✕");
            removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 9px; -fx-padding: 0 2; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                activeOptions.remove(opt);
                rebuildChips();
            });

            chip.getChildren().addAll(lbl, removeBtn);
            chipsPane.getChildren().add(chip);
        }
        boolean hasChips = !activeOptions.isEmpty();
        chipsPane.setVisible(hasChips);
        chipsPane.setManaged(hasChips);
    }

    private void doPull() {
        String remote = remoteCombo.getValue();
        String branch = branchCombo.getValue();
        List<String> options = new ArrayList<>();
        for (PullOption opt : activeOptions) {
            options.add(opt.getFlag());
        }
        close();
        if (onPull != null) {
            onPull.accept(remote, branch, options);
        }
    }

    public ComboBox<String> getRemoteCombo() {
        return remoteCombo;
    }

    public ComboBox<String> getBranchCombo() {
        return branchCombo;
    }

    public Set<PullOption> getActiveOptions() {
        return Collections.unmodifiableSet(activeOptions);
    }

    public Button getPullButton() {
        return pullBtn;
    }

    public Button getCancelButton() {
        return cancelBtn;
    }

    public Button getModifyOptionsButton() {
        return modifyOptionsBtn;
    }
}
