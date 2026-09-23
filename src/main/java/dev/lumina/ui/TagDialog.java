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
 * Modern Git Tag dialog matching reference design, supporting dynamic Git root,
 * active branch display, tag naming, commit target with validation, message,
 * and force tag creation.
 */
public class TagDialog extends Stage {

    @FunctionalInterface
    public interface TagConsumer {
        void accept(String tagName, String commit, String message, boolean force);
    }

    private final Path projectRoot;
    private final TagConsumer onTag;

    private final ComboBox<String> gitRootCombo = new ComboBox<>();
    private final Label currentBranchLabel = new Label();
    private final TextField tagNameField = new TextField();
    private final CheckBox forceBox = new CheckBox("Force");
    private final TextField commitField = new TextField();
    private final Button validateBtn = new Button("Validate");
    private final Label validationStatusLabel = new Label();
    private final TextArea messageArea = new TextArea();
    private final Button createTagBtn = new Button("Create Tag");
    private final Button cancelBtn = new Button("Cancel");
    private final Button helpBtn = new Button("?");

    public TagDialog(Window owner, Path projectRoot, TagConsumer onTag) {
        this(owner, projectRoot, null, null, onTag);
    }

    public TagDialog(Window owner, Path projectRoot, String prefilledCommit, String prefilledTag, TagConsumer onTag) {
        this.projectRoot = projectRoot;
        this.onTag = onTag;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Tag");
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

        // 3. Tag Name Row
        HBox nameRow = new HBox(10);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameTitle = new Label("Tag Name:");
        nameTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nameTitle.setPrefWidth(100);

        if (prefilledTag != null && !prefilledTag.isBlank()) {
            tagNameField.setText(prefilledTag);
        }
        tagNameField.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(tagNameField, Priority.ALWAYS);
        nameRow.getChildren().addAll(nameTitle, tagNameField);

        // 4. Force Checkbox Row
        HBox forceRow = new HBox(10);
        forceRow.setAlignment(Pos.CENTER_LEFT);
        forceRow.setPadding(new Insets(0, 0, 0, 110));
        forceBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        forceRow.getChildren().add(forceBox);

        // 5. Commit Row with [ Validate ] button
        HBox commitRow = new HBox(8);
        commitRow.setAlignment(Pos.CENTER_LEFT);
        Label commitTitle = new Label("Commit:");
        commitTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        commitTitle.setPrefWidth(100);

        if (prefilledCommit != null && !prefilledCommit.isBlank()) {
            commitField.setText(prefilledCommit);
        }
        commitField.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
        HBox.setHgrow(commitField, Priority.ALWAYS);

        validateBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 5 12; -fx-cursor: hand;");
        validateBtn.setOnAction(e -> doValidateCommit());

        commitRow.getChildren().addAll(commitTitle, commitField, validateBtn);

        // Validation status
        validationStatusLabel.setStyle("-fx-font-size: 11px; -fx-padding: 0 0 0 110;");
        validationStatusLabel.setVisible(false);
        validationStatusLabel.setManaged(false);

        // 6. Message Row
        HBox messageRow = new HBox(10);
        messageRow.setAlignment(Pos.TOP_LEFT);
        Label messageTitle = new Label("Message:");
        messageTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 0 0 0;");
        messageTitle.setPrefWidth(100);

        messageArea.setPromptText("Tag message\u2026");
        messageArea.setPrefRowCount(3);
        messageArea.setStyle("-fx-control-inner-background: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");
        HBox.setHgrow(messageArea, Priority.ALWAYS);
        messageRow.getChildren().addAll(messageTitle, messageArea);

        // 7. Bottom Button Bar
        helpBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-weight: bold; -fx-min-width: 24; -fx-min-height: 24; -fx-cursor: hand;");
        helpBtn.setTooltip(new Tooltip("Create a Git tag for the specified commit or HEAD"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-padding: 6 16; -fx-cursor: hand; -fx-font-size: 12px;");
        cancelBtn.setOnAction(e -> close());

        // Update Create Tag button state
        updateCreateButtonState();
        tagNameField.textProperty().addListener((obs, oldV, newV) -> updateCreateButtonState());

        createTagBtn.setOnAction(e -> doCreateTag());

        HBox bottomBar = new HBox(8, helpBtn, spacer, cancelBtn, createTagBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(8, 0, 0, 0));

        root.getChildren().addAll(rootRow, branchRow, nameRow, forceRow, commitRow, validationStatusLabel, messageRow, bottomBar);

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !messageArea.isFocused()) {
                if (!createTagBtn.isDisabled()) {
                    doCreateTag();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });

        setScene(scene);
    }

    private void updateCreateButtonState() {
        String tag = tagNameField.getText();
        boolean hasTag = tag != null && !tag.trim().isEmpty();
        createTagBtn.setDisable(!hasTag);
        if (hasTag) {
            createTagBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 6 18; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand; -fx-font-size: 12px;");
        } else {
            createTagBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #6C707E; -fx-border-color: #393B40; -fx-font-weight: bold; -fx-padding: 6 18; -fx-background-radius: 4; -fx-border-radius: 4; -fx-font-size: 12px;");
        }
    }

    private void doValidateCommit() {
        String target = commitField.getText();
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

    private void doCreateTag() {
        String tag = tagNameField.getText() != null ? tagNameField.getText().trim() : "";
        if (tag.isEmpty()) return;

        String commit = commitField.getText() != null ? commitField.getText().trim() : "";
        String message = messageArea.getText() != null ? messageArea.getText().trim() : "";
        boolean force = forceBox.isSelected();

        close();
        if (onTag != null) {
            onTag.accept(tag, commit, message, force);
        }
    }

    public ComboBox<String> getGitRootCombo() {
        return gitRootCombo;
    }

    public Label getCurrentBranchLabel() {
        return currentBranchLabel;
    }

    public TextField getTagNameField() {
        return tagNameField;
    }

    public CheckBox getForceBox() {
        return forceBox;
    }

    public TextField getCommitField() {
        return commitField;
    }

    public Button getValidateButton() {
        return validateBtn;
    }

    public Label getValidationStatusLabel() {
        return validationStatusLabel;
    }

    public TextArea getMessageArea() {
        return messageArea;
    }

    public Button getCreateTagButton() {
        return createTagBtn;
    }

    public Button getCancelButton() {
        return cancelBtn;
    }

    public Button getHelpButton() {
        return helpBtn;
    }
}
