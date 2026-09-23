package dev.lumina.ui;

import dev.lumina.git.GitService;
import dev.lumina.notification.Notification;
import dev.lumina.notification.NotificationService;
import dev.lumina.notification.NotificationType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * IntelliJ IDEA-styled "Stash" modal dialog matching media_1790163780694.png:
 * - Git Root: [ /home/firoze/projects/others/lumina ⌄ ]
 * - Current Branch: master
 * - Message: [🕒] [🛈]
 * - Multi-line stash message text area
 * - [ ] Keep index
 * - Bottom: ? Help, [ Create Stash ], [ Cancel ]
 */
public class StashDialog extends Stage {

    private final Path projectRoot;
    private final Consumer<String> log;
    private final Runnable onStashed;

    private final ComboBox<String> gitRootCombo = new ComboBox<>();
    private final Label currentBranchLabel = new Label("Current Branch: master");
    private final TextArea messageArea = new TextArea();
    private final CheckBox keepIndexCheck = new CheckBox("Keep index");
    private final Button createStashBtn = new Button("Create Stash");
    private final Button cancelBtn = new Button("Cancel");
    private final Button helpBtn = new Button("?");

    public StashDialog(Window owner, Path projectRoot, Consumer<String> log, Runnable onStashed) {
        this.projectRoot = projectRoot;
        this.log = log;
        this.onStashed = onStashed;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Stash");
        setResizable(false);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        root.setPrefWidth(490);

        // 1. Git Root Row
        Label gitRootLabel = new Label("Git Root:");
        gitRootLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-min-width: 90;");

        String rootPathStr = projectRoot != null ? projectRoot.toAbsolutePath().normalize().toString() : "";
        gitRootCombo.getItems().add(rootPathStr);
        gitRootCombo.getSelectionModel().select(0);
        gitRootCombo.setMaxWidth(Double.MAX_VALUE);
        gitRootCombo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        HBox.setHgrow(gitRootCombo, Priority.ALWAYS);

        HBox rootRow = new HBox(8, gitRootLabel, gitRootCombo);
        rootRow.setAlignment(Pos.CENTER_LEFT);

        // 2. Current Branch Row
        String currentBranch = projectRoot != null ? GitService.currentBranch(projectRoot) : "master";
        if (currentBranch == null || currentBranch.isBlank()) currentBranch = "master";
        currentBranchLabel.setText("Current Branch: " + currentBranch);
        currentBranchLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px; -fx-padding: 0 0 2 0;");

        // 3. Message Header Row (Message: label + Recent button + info icon)
        Label messageLabel = new Label("Message:");
        messageLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        Region msgSpacer = new Region();
        HBox.setHgrow(msgSpacer, Priority.ALWAYS);

        Button recentBtn = new Button("🕒");
        recentBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 4 2 4;");
        recentBtn.setTooltip(new Tooltip("Recent stash messages"));
        recentBtn.setOnAction(e -> showRecentMessagesMenu(recentBtn));

        Label infoIcon = new Label("🛈");
        infoIcon.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px; -fx-cursor: hand;");
        infoIcon.setTooltip(new Tooltip("Saves uncommitted changes into a Git stash for later use"));

        HBox msgHeader = new HBox(6, messageLabel, msgSpacer, recentBtn, infoIcon);
        msgHeader.setAlignment(Pos.CENTER_LEFT);

        // 4. Message Text Area
        messageArea.setPromptText("Optional stash message");
        messageArea.setPrefRowCount(3);
        messageArea.setWrapText(true);
        messageArea.setStyle("-fx-control-inner-background: #2B2D30; -fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        // 5. Keep Index Checkbox
        keepIndexCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        keepIndexCheck.setTooltip(new Tooltip("All changes already added to the index are left intact"));

        // 6. Bottom Action Bar
        helpBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-weight: bold; -fx-min-width: 24; -fx-min-height: 24; -fx-cursor: hand;");
        helpBtn.setTooltip(new Tooltip("Stash uncommitted changes"));

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        createStashBtn.setDefaultButton(true);
        createStashBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 5 18 5 18; -fx-background-radius: 4; -fx-cursor: hand;");
        createStashBtn.setOnAction(e -> doCreateStash());

        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5; -fx-padding: 5 14 5 14; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        HBox bottomBar = new HBox(8, helpBtn, bottomSpacer, createStashBtn, cancelBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(rootRow, currentBranchLabel, msgHeader, messageArea, keepIndexCheck, bottomBar);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        } catch (Exception ignored) {}
        setScene(scene);
    }

    private void showRecentMessagesMenu(Button anchor) {
        if (projectRoot == null) return;
        List<String> recents = GitService.recentCommitMessages(projectRoot, 6);
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5;");
        if (recents.isEmpty()) {
            MenuItem emptyItem = new MenuItem("No recent messages");
            emptyItem.setDisable(true);
            menu.getItems().add(emptyItem);
        } else {
            for (String msg : recents) {
                MenuItem item = new MenuItem(msg);
                item.setOnAction(e -> messageArea.setText(msg));
                menu.getItems().add(item);
            }
        }
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void doCreateStash() {
        if (projectRoot == null || !GitService.isRepository(projectRoot)) return;

        createStashBtn.setDisable(true);
        cancelBtn.setDisable(true);

        String msg = messageArea.getText().trim();
        boolean keepIndex = keepIndexCheck.isSelected();

        new Thread(() -> {
            GitService.Result r = GitService.stash(projectRoot, msg, keepIndex);
            Platform.runLater(() -> {
                if (r.ok()) {
                    if (log != null) log.accept("\u2713 Stashed changes: " + (msg.isBlank() ? "WIP" : msg));
                    Notification notif = new Notification(
                            "Git",
                            "Stashed changes",
                            r.output().isBlank() ? (msg.isBlank() ? "Saved working directory changes" : msg) : r.output().trim(),
                            NotificationType.INFORMATION
                    );
                    NotificationService.getInstance().notify(notif);
                    if (onStashed != null) onStashed.run();
                    close();
                } else {
                    createStashBtn.setDisable(false);
                    cancelBtn.setDisable(false);
                    if (log != null) log.accept("Stash failed: " + r.output().trim());

                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Stash Failed");
                    err.setHeaderText("Git Stash Failed");
                    err.setContentText(r.output().trim());
                    err.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
                    err.showAndWait();
                }
            });
        }, "lumina-git-create-stash").start();
    }

    // Accessors for testing
    public ComboBox<String> getGitRootCombo() { return gitRootCombo; }
    public Label getCurrentBranchLabel() { return currentBranchLabel; }
    public TextArea getMessageArea() { return messageArea; }
    public CheckBox getKeepIndexCheck() { return keepIndexCheck; }
    public Button getCreateStashBtn() { return createStashBtn; }
    public Button getCancelBtn() { return cancelBtn; }
}
