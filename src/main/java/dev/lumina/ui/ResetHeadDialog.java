package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.nio.file.Path;

/**
 * Modern Git Reset HEAD dialog matching reference design, supporting dynamic Git root,
 * active branch display, reset type selection (Mixed, Soft, Hard),
 * commit validation, and safe execution.
 */
public class ResetHeadDialog extends Stage {

    public enum ResetType {
        MIXED("Mixed"),
        SOFT("Soft"),
        HARD("Hard");

        private final String label;

        ResetType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @FunctionalInterface
    public interface ResetHeadConsumer {
        void accept(String resetType, String commit);
    }

    private final Path projectRoot;
    private final ResetHeadConsumer onReset;

    private final ComboBox<String> gitRootCombo = new ComboBox<>();
    private final Label currentBranchLabel = new Label();
    private final ComboBox<ResetType> resetTypeCombo = new ComboBox<>();
    private final TextField toCommitField = new TextField("HEAD");
    private final Button validateBtn = new Button("Validate");
    private final Label validationStatusLabel = new Label();
    private final Button resetBtn = new Button("Reset");
    private final Button cancelBtn = new Button("Cancel");
    private final Button helpBtn = new Button("?");

    public ResetHeadDialog(Window owner, Path projectRoot, ResetHeadConsumer onReset) {
        this(owner, projectRoot, "HEAD", onReset);
    }

    public ResetHeadDialog(Window owner, Path projectRoot, String targetCommit, ResetHeadConsumer onReset) {
        this.projectRoot = projectRoot;
        this.onReset = onReset;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Reset Head");
        setResizable(false);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: #2B2D30; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        root.setPrefWidth(500);

        String currentBranch = projectRoot != null ? GitService.currentBranch(projectRoot) : "HEAD";
        if (currentBranch == null || currentBranch.isBlank()) currentBranch = "HEAD";

        // 1. Git Root Row
        HBox rootRow = new HBox(10);
        rootRow.setAlignment(Pos.CENTER_LEFT);
        Label gitRootTitle = new Label("Git Root:");
        gitRootTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        gitRootTitle.setPrefWidth(100);

        String rootDisplay = projectRoot != null ? projectRoot.toAbsolutePath().toString() : "";
        gitRootCombo.getItems().setAll(rootDisplay);
        gitRootCombo.setValue(rootDisplay);
        gitRootCombo.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox.setHgrow(gitRootCombo, Priority.ALWAYS);
        rootRow.getChildren().addAll(gitRootTitle, gitRootCombo);

        // 2. Current Branch Row
        HBox branchRow = new HBox(10);
        branchRow.setAlignment(Pos.CENTER_LEFT);
        Label branchTitle = new Label("Current Branch:");
        branchTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        branchTitle.setPrefWidth(100);

        currentBranchLabel.setText(currentBranch);
        currentBranchLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        branchRow.getChildren().addAll(branchTitle, currentBranchLabel);

        // 3. Reset Type Row
        HBox typeRow = new HBox(10);
        typeRow.setAlignment(Pos.CENTER_LEFT);
        Label typeTitle = new Label("Reset Type:");
        typeTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        typeTitle.setPrefWidth(100);

        resetTypeCombo.getItems().setAll(ResetType.values());
        resetTypeCombo.setValue(ResetType.MIXED);
        resetTypeCombo.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        typeRow.getChildren().addAll(typeTitle, resetTypeCombo);

        // 4. To Commit Row with [ Validate ] button
        HBox commitRow = new HBox(8);
        commitRow.setAlignment(Pos.CENTER_LEFT);
        Label commitTitle = new Label("To Commit:");
        commitTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        commitTitle.setPrefWidth(100);

        if (targetCommit != null && !targetCommit.isBlank()) {
            toCommitField.setText(targetCommit.trim());
        }
        toCommitField.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(toCommitField, Priority.ALWAYS);

        validateBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 5 12; -fx-cursor: hand;");
        validateBtn.setOnAction(e -> doValidateCommit());

        commitRow.getChildren().addAll(commitTitle, toCommitField, validateBtn);

        // Validation status
        validationStatusLabel.setStyle("-fx-font-size: 11px; -fx-padding: 0 0 0 110;");
        validationStatusLabel.setVisible(false);
        validationStatusLabel.setManaged(false);

        // 5. Bottom Button Bar
        helpBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-weight: bold; -fx-min-width: 24; -fx-min-height: 24; -fx-cursor: hand;");
        helpBtn.setTooltip(new Tooltip("Mixed: Keep working tree, discard staged changes\nSoft: Keep working tree and staged changes\nHard: Discard all local changes to match target commit"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-padding: 6 16; -fx-cursor: hand; -fx-font-size: 12px;");
        cancelBtn.setOnAction(e -> close());

        resetBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 6 18; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand; -fx-font-size: 12px;");
        resetBtn.setOnAction(e -> doReset());

        HBox bottomBar = new HBox(8, helpBtn, spacer, cancelBtn, resetBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(8, 0, 0, 0));

        root.getChildren().addAll(rootRow, branchRow, typeRow, commitRow, validationStatusLabel, bottomBar);

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                doReset();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });

        setScene(scene);
    }

    private void doValidateCommit() {
        String target = toCommitField.getText();
        if (target == null || target.isBlank()) {
            target = "HEAD";
        }
        GitService.Result r = GitService.validateRevision(projectRoot, target);
        validationStatusLabel.setVisible(true);
        validationStatusLabel.setManaged(true);
        if (r.ok()) {
            String fullHash = r.output().trim();
            String shortHash = fullHash.length() > 7 ? fullHash.substring(0, 7) : fullHash;
            validationStatusLabel.setText("\u2713 Valid commit: " + shortHash);
            validationStatusLabel.setStyle("-fx-text-fill: #59A869; -fx-font-size: 11px; -fx-padding: 0 0 0 110;");
        } else {
            validationStatusLabel.setText("\u2717 Invalid commit reference");
            validationStatusLabel.setStyle("-fx-text-fill: #ED6C63; -fx-font-size: 11px; -fx-padding: 0 0 0 110;");
        }
    }

    private void doReset() {
        String commit = toCommitField.getText() != null ? toCommitField.getText().trim() : "";
        if (commit.isEmpty()) commit = "HEAD";

        ResetType selectedType = resetTypeCombo.getValue();
        String typeStr = selectedType != null ? selectedType.getLabel() : "Mixed";

        // Prompt confirmation for Hard reset
        if (selectedType == ResetType.HARD) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Hard Reset");
            alert.setHeaderText("Discard all local changes?");
            alert.setContentText("A hard reset to '" + commit + "' will irrevocably discard all uncommitted changes in your working tree.");
            alert.getDialogPane().setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");
            if (getScene() != null && getScene().getWindow() != null) {
                alert.initOwner(getScene().getWindow());
            }
            if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        close();
        if (onReset != null) {
            onReset.accept(typeStr, commit);
        }
    }

    public ComboBox<String> getGitRootCombo() {
        return gitRootCombo;
    }

    public Label getCurrentBranchLabel() {
        return currentBranchLabel;
    }

    public ComboBox<ResetType> getResetTypeCombo() {
        return resetTypeCombo;
    }

    public TextField getToCommitField() {
        return toCommitField;
    }

    public Button getValidateButton() {
        return validateBtn;
    }

    public Label getValidationStatusLabel() {
        return validationStatusLabel;
    }

    public Button getResetButton() {
        return resetBtn;
    }

    public Button getCancelButton() {
        return cancelBtn;
    }

    public Button getHelpButton() {
        return helpBtn;
    }
}
