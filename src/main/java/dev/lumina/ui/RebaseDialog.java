package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.*;

/**
 * Modern Git Rebase dialog supporting dynamic branch/hash selection,
 * configurable rebase options, --onto target, branch selection,
 * and direct rebase execution.
 */
public class RebaseDialog extends Stage {

    public enum RebaseOption {
        SELECT_BRANCH("Select another branch to rebase", ""),
        ONTO("Specify a new base for the rebased commits", "--onto"),
        REBASE_MERGES("Recreate commits topology", "--rebase-merges"),
        KEEP_EMPTY("Do not remove empty commits during rebase", "--keep-empty"),
        ROOT("Rebase all commits in the branch", "--root"),
        INTERACTIVE("Edit commits before rebasing", "--interactive"),
        UPDATE_REFS("Auto-update branches pointing to rebased commits", "--update-refs");

        private final String description;
        private final String flag;

        RebaseOption(String description, String flag) {
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
    public interface RebaseConsumer {
        void accept(String branchOrHash, String ontoBranch, String branchToRebase, List<String> options);
    }

    private final Path projectRoot;
    private final RebaseConsumer onRebase;

    private final ComboBox<String> branchCombo = new ComboBox<>();
    private final FlowPane chipsPane = new FlowPane(6, 6);
    private final ComboBox<String> ontoCombo = new ComboBox<>();
    private final ComboBox<String> branchToRebaseCombo = new ComboBox<>();
    private final Button rebaseBtn = new Button("Rebase");
    private final Button cancelBtn = new Button("Cancel");
    private final Button modifyOptionsBtn = new Button("Modify options \u25BE");
    private final Button helpBtn = new Button("?");

    private final Set<RebaseOption> activeOptions = new LinkedHashSet<>();
    private String currentBranch;

    public RebaseDialog(Window owner, Path projectRoot, RebaseConsumer onRebase) {
        this(owner, projectRoot, null, onRebase);
    }

    public RebaseDialog(Window owner, Path projectRoot, String preselectedTarget, RebaseConsumer onRebase) {
        this.projectRoot = projectRoot;
        this.onRebase = onRebase;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        currentBranch = projectRoot != null ? GitService.currentBranch(projectRoot) : null;
        if (currentBranch == null || currentBranch.isBlank()) {
            currentBranch = "HEAD";
        }
        setTitle("Rebase");
        setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        root.setPrefWidth(480);

        // 1. Command bar row: [git rebase] [branch or hash ▾]
        Label gitRebaseBadge = new Label("git rebase");
        gitRebaseBadge.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10; -fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");

        branchCombo.setEditable(true);
        branchCombo.setPromptText("branch or hash");
        branchCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        HBox.setHgrow(branchCombo, Priority.ALWAYS);

        populateBranches(preselectedTarget);

        HBox commandRow = new HBox(8, gitRebaseBadge, branchCombo);
        commandRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Active option chips pane
        chipsPane.setAlignment(Pos.CENTER_LEFT);
        chipsPane.setVisible(false);
        chipsPane.setManaged(false);

        // 3. Optional secondary fields:
        // Onto branch/base combo
        ontoCombo.setEditable(true);
        ontoCombo.setPromptText("new base (--onto)");
        ontoCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        ontoCombo.setMaxWidth(Double.MAX_VALUE);
        ontoCombo.setVisible(false);
        ontoCombo.setManaged(false);

        // Branch to rebase combo
        branchToRebaseCombo.setEditable(true);
        branchToRebaseCombo.setPromptText("branch to rebase");
        branchToRebaseCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        branchToRebaseCombo.setMaxWidth(Double.MAX_VALUE);
        branchToRebaseCombo.setVisible(false);
        branchToRebaseCombo.setManaged(false);

        // Populate secondary combos with branch list
        List<String> branches = projectRoot != null ? GitService.allBranches(projectRoot) : Collections.emptyList();
        if (!branches.isEmpty()) {
            ontoCombo.getItems().setAll(branches);
            branchToRebaseCombo.getItems().setAll(branches);
        }

        // 4. Bottom bar: [?] [Modify options ▾] ... spacer ... [Cancel] [Rebase]
        helpBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-weight: bold; -fx-min-width: 24; -fx-min-height: 24; -fx-cursor: hand;");
        helpBtn.setTooltip(new Tooltip("Rebase current branch onto another branch or commit hash"));

        modifyOptionsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3574F0; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 4 4;");
        modifyOptionsBtn.setOnMouseEntered(e -> modifyOptionsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B9BF5; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 4 4; -fx-underline: true;"));
        modifyOptionsBtn.setOnMouseExited(e -> modifyOptionsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3574F0; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 4 4; -fx-underline: false;"));
        modifyOptionsBtn.setOnAction(e -> showModifyOptionsMenu(modifyOptionsBtn));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5; -fx-padding: 6 16; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand; -fx-font-size: 13px;");
        cancelBtn.setOnAction(e -> close());

        // Update Rebase button enablement based on branch/hash input
        updateRebaseButtonState();
        branchCombo.getEditor().textProperty().addListener((obs, oldV, newV) -> updateRebaseButtonState());
        branchCombo.valueProperty().addListener((obs, oldV, newV) -> updateRebaseButtonState());

        rebaseBtn.setOnAction(e -> doRebase());

        HBox bottomBar = new HBox(8, helpBtn, modifyOptionsBtn, spacer, cancelBtn, rebaseBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(6, 0, 0, 0));

        root.getChildren().addAll(commandRow, chipsPane, ontoCombo, branchToRebaseCombo, bottomBar);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (!rebaseBtn.isDisabled()) {
                    doRebase();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });

        setScene(scene);
    }

    private void updateRebaseButtonState() {
        String target = getTargetBranchOrHash();
        boolean hasTarget = target != null && !target.isBlank();
        rebaseBtn.setDisable(!hasTarget);
        if (hasTarget) {
            rebaseBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 6 20; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-size: 13px;");
        } else {
            rebaseBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #6C707E; -fx-border-color: #393B40; -fx-font-weight: bold; -fx-padding: 6 20; -fx-background-radius: 4; -fx-border-radius: 4; -fx-font-size: 13px;");
        }
    }

    private void populateBranches(String preselectedTarget) {
        if (projectRoot == null) return;
        List<String> branches = GitService.allBranches(projectRoot);
        branchCombo.getItems().setAll(branches);
        if (preselectedTarget != null && !preselectedTarget.isBlank()) {
            branchCombo.setValue(preselectedTarget);
            if (branchCombo.getEditor() != null) {
                branchCombo.getEditor().setText(preselectedTarget);
            }
        }
    }

    private void showModifyOptionsMenu(Button anchor) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("git-tool-options-menu");
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-padding: 4 0;");

        Label headerLabel = new Label("Add Rebase Options");
        headerLabel.setStyle("-fx-text-fill: #8C8E94; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 12 4 12;");
        CustomMenuItem headerItem = new CustomMenuItem(headerLabel, false);
        menu.getItems().addAll(headerItem, new SeparatorMenuItem());

        int count = 0;
        for (RebaseOption opt : RebaseOption.values()) {
            // Add separator between root and interactive matching reference
            if (opt == RebaseOption.INTERACTIVE && count > 0) {
                menu.getItems().add(new SeparatorMenuItem());
            }

            HBox itemBox = new HBox(12);
            itemBox.setAlignment(Pos.CENTER_LEFT);

            Label checkMark = new Label(activeOptions.contains(opt) ? "\u2713" : " ");
            checkMark.setStyle("-fx-text-fill: #3574F0; -fx-font-weight: bold; -fx-min-width: 14;");

            Label desc = new Label(opt.getDescription());
            desc.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            HBox.setHgrow(desc, Priority.ALWAYS);

            Label flag = new Label(opt.getFlag());
            flag.setStyle("-fx-text-fill: #8C8E94; -fx-font-size: 11px; -fx-font-family: monospace;");

            itemBox.getChildren().addAll(checkMark, desc, flag);

            CustomMenuItem mi = new CustomMenuItem(itemBox, false);
            mi.setOnAction(e -> toggleOption(opt));
            menu.getItems().add(mi);
            count++;
        }

        menu.show(anchor, Side.BOTTOM, 0, 2);
    }

    public void toggleOption(RebaseOption opt) {
        if (activeOptions.contains(opt)) {
            activeOptions.remove(opt);
        } else {
            activeOptions.add(opt);
        }
        rebuildChips();
    }

    private void rebuildChips() {
        chipsPane.getChildren().clear();
        for (RebaseOption opt : activeOptions) {
            HBox chip = new HBox(4);
            chip.setAlignment(Pos.CENTER);
            chip.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 2 8;");

            String labelText = opt.getFlag().isBlank() ? "branch" : opt.getFlag();
            Label lbl = new Label(labelText);
            lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-font-family: monospace;");

            Button removeBtn = new Button("\u2715");
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

        boolean hasOnto = activeOptions.contains(RebaseOption.ONTO);
        ontoCombo.setVisible(hasOnto);
        ontoCombo.setManaged(hasOnto);

        boolean hasSelectBranch = activeOptions.contains(RebaseOption.SELECT_BRANCH);
        branchToRebaseCombo.setVisible(hasSelectBranch);
        branchToRebaseCombo.setManaged(hasSelectBranch);
    }

    public String getTargetBranchOrHash() {
        String editorText = branchCombo.getEditor() != null ? branchCombo.getEditor().getText() : null;
        if (editorText != null && !editorText.isBlank()) {
            return editorText.trim();
        }
        String val = branchCombo.getValue();
        return val != null ? val.trim() : "";
    }

    private void doRebase() {
        String target = getTargetBranchOrHash();
        if (target.isBlank()) return;

        List<String> options = new ArrayList<>();
        for (RebaseOption opt : activeOptions) {
            if (opt != RebaseOption.ONTO && opt != RebaseOption.SELECT_BRANCH && !opt.getFlag().isBlank()) {
                options.add(opt.getFlag());
            }
        }

        String onto = null;
        if (activeOptions.contains(RebaseOption.ONTO)) {
            String ontoText = ontoCombo.getEditor() != null ? ontoCombo.getEditor().getText() : null;
            if (ontoText != null && !ontoText.isBlank()) {
                onto = ontoText.trim();
            } else if (ontoCombo.getValue() != null && !ontoCombo.getValue().isBlank()) {
                onto = ontoCombo.getValue().trim();
            }
        }

        String branchToRebase = null;
        if (activeOptions.contains(RebaseOption.SELECT_BRANCH)) {
            String bText = branchToRebaseCombo.getEditor() != null ? branchToRebaseCombo.getEditor().getText() : null;
            if (bText != null && !bText.isBlank()) {
                branchToRebase = bText.trim();
            } else if (branchToRebaseCombo.getValue() != null && !branchToRebaseCombo.getValue().isBlank()) {
                branchToRebase = branchToRebaseCombo.getValue().trim();
            }
        }

        close();
        if (onRebase != null) {
            onRebase.accept(target, onto, branchToRebase, options);
        }
    }

    public ComboBox<String> getBranchCombo() {
        return branchCombo;
    }

    public ComboBox<String> getOntoCombo() {
        return ontoCombo;
    }

    public ComboBox<String> getBranchToRebaseCombo() {
        return branchToRebaseCombo;
    }

    public Set<RebaseOption> getActiveOptions() {
        return Collections.unmodifiableSet(activeOptions);
    }

    public Button getRebaseButton() {
        return rebaseBtn;
    }

    public Button getCancelButton() {
        return cancelBtn;
    }

    public Button getModifyOptionsButton() {
        return modifyOptionsBtn;
    }
}
