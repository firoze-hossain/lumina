package dev.lumina.ui;

import dev.lumina.git.GitService;
import dev.lumina.notification.Notification;
import dev.lumina.notification.NotificationService;
import dev.lumina.notification.NotificationType;
import dev.lumina.util.Settings;
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
import java.util.function.Consumer;

/**
 * Modern "Update Project" dialog:
 * - Radio: (•) Merge incoming changes into the current branch
 * - Radio: ( ) Rebase the current branch on top of incoming changes
 * - Bottom: ? Help, [ ] Don't show again, [ OK ], [ Cancel ]
 */
public class UpdateProjectDialog extends Stage {

    public static final String PREF_DONT_SHOW = "git.update.dont_show";
    public static final String PREF_STRATEGY = "git.update.strategy"; // "merge" or "rebase"

    private final Path projectRoot;
    private final Consumer<String> log;
    private final Runnable onUpdated;

    private final RadioButton mergeRadio = new RadioButton("Merge incoming changes into the current branch");
    private final RadioButton rebaseRadio = new RadioButton("Rebase the current branch on top of incoming changes");
    private final CheckBox dontShowAgainCheck = new CheckBox("Don't show again");
    private final Button okBtn = new Button("OK");
    private final Button cancelBtn = new Button("Cancel");
    private final Button helpBtn = new Button("?");

    public UpdateProjectDialog(Window owner, Path projectRoot, Consumer<String> log, Runnable onUpdated) {
        this.projectRoot = projectRoot;
        this.log = log;
        this.onUpdated = onUpdated;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Update Project");
        setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(18, 20, 16, 20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        root.setPrefWidth(480);

        // 1. Radio strategy options
        ToggleGroup strategyGroup = new ToggleGroup();
        mergeRadio.setToggleGroup(strategyGroup);
        rebaseRadio.setToggleGroup(strategyGroup);

        mergeRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        rebaseRadio.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        String savedStrategy = Settings.get(PREF_STRATEGY);
        if ("rebase".equalsIgnoreCase(savedStrategy)) {
            rebaseRadio.setSelected(true);
        } else {
            mergeRadio.setSelected(true);
        }

        VBox optionsBox = new VBox(12, mergeRadio, rebaseRadio);
        optionsBox.setPadding(new Insets(4, 0, 8, 0));

        // 2. Bottom Action Bar
        helpBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-weight: bold; -fx-min-width: 24; -fx-min-height: 24; -fx-cursor: hand;");
        helpBtn.setTooltip(new Tooltip("Update project from tracked remote branch"));

        dontShowAgainCheck.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-cursor: hand;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 5 18 5 18; -fx-background-radius: 4; -fx-cursor: hand;");
        okBtn.setOnAction(e -> doUpdate());

        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5; -fx-padding: 5 14 5 14; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        HBox bottomBar = new HBox(8, helpBtn, dontShowAgainCheck, spacer, okBtn, cancelBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(optionsBox, bottomBar);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        setScene(scene);
    }

    private void doUpdate() {
        boolean isRebase = rebaseRadio.isSelected();
        if (dontShowAgainCheck.isSelected()) {
            Settings.put(PREF_DONT_SHOW, "true");
            Settings.put(PREF_STRATEGY, isRebase ? "rebase" : "merge");
        }

        okBtn.setDisable(true);
        cancelBtn.setDisable(true);

        new Thread(() -> {
            GitService.Result r = GitService.updateProject(projectRoot, isRebase);
            Platform.runLater(() -> {
                String branch = GitService.currentBranch(projectRoot);
                if (branch == null) branch = "master";

                if (r.ok()) {
                    if (log != null) log.accept("\u2713 Updated project on branch " + branch + (isRebase ? " (rebase)" : ""));
                    Notification notif = new Notification(
                            "Git",
                            "Updated project",
                            r.output().isBlank() ? "Already up to date on " + branch : r.output().trim(),
                            NotificationType.INFORMATION
                    );
                    NotificationService.getInstance().notify(notif);
                    if (onUpdated != null) onUpdated.run();
                    close();
                } else {
                    okBtn.setDisable(false);
                    cancelBtn.setDisable(false);
                    if (log != null) log.accept("Update project failed: " + r.output().trim());

                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Update Failed");
                    err.setHeaderText("Git Update Project Failed");
                    err.setContentText(r.output().trim());
                    err.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
                    err.showAndWait();
                }
            });
        }, "lumina-git-update-project").start();
    }

    // Accessors for testing
    public RadioButton getMergeRadio() { return mergeRadio; }
    public RadioButton getRebaseRadio() { return rebaseRadio; }
    public CheckBox getDontShowAgainCheck() { return dontShowAgainCheck; }
    public Button getOkBtn() { return okBtn; }
    public Button getCancelBtn() { return cancelBtn; }
}
