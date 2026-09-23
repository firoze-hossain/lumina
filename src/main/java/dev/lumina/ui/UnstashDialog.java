package dev.lumina.ui;

import dev.lumina.git.GitService;
import dev.lumina.notification.Notification;
import dev.lumina.notification.NotificationService;
import dev.lumina.notification.NotificationType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
 * IntelliJ IDEA-styled "Unstash Changes" dialog matching Git > Uncommitted Changes > Unstash Changes...
 */
public class UnstashDialog extends Stage {

    private final Path projectRoot;
    private final Consumer<String> log;
    private final Runnable onUnstashed;

    private final ListView<GitService.StashEntry> stashListView = new ListView<>();
    private final CheckBox popStashCheck = new CheckBox("Pop stash");
    private final CheckBox reinstateIndexCheck = new CheckBox("Reinstate index");
    private final Button applyBtn = new Button("Apply Stash");
    private final Button dropBtn = new Button("Drop Stash");
    private final Button cancelBtn = new Button("Cancel");
    private final Button helpBtn = new Button("?");

    public UnstashDialog(Window owner, Path projectRoot, Consumer<String> log, Runnable onUnstashed) {
        this.projectRoot = projectRoot;
        this.log = log;
        this.onUnstashed = onUnstashed;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Unstash Changes");
        setResizable(false);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        root.setPrefWidth(500);

        Label header = new Label("Select stash to apply:");
        header.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        // Stash list
        stashListView.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");
        stashListView.setPrefHeight(180);

        stashListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GitService.StashEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #2B2D30;");
                } else {
                    Label refLabel = new Label(item.ref());
                    refLabel.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 11px; -fx-min-width: 60;");

                    Label msgLabel = new Label(item.message());
                    msgLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                    HBox.setHgrow(msgLabel, Priority.ALWAYS);

                    Label branchLabel = new Label(item.branch());
                    branchLabel.setStyle("-fx-text-fill: #C29E5A; -fx-font-size: 11px;");

                    HBox row = new HBox(6, refLabel, msgLabel, branchLabel);
                    row.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(row);
                    setText(null);
                    setStyle("-fx-background-color: " + (isSelected() ? "#2E436E;" : "#2B2D30;"));
                }
            }
        });

        // Load stashes
        loadStashes();

        // Options
        popStashCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        reinstateIndexCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        HBox optionsBox = new HBox(16, popStashCheck, reinstateIndexCheck);

        // Bottom Bar
        helpBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-weight: bold; -fx-min-width: 24; -fx-min-height: 24; -fx-cursor: hand;");
        helpBtn.setTooltip(new Tooltip("Apply changes recorded in stash back into your working directory"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        applyBtn.setDefaultButton(true);
        applyBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 5 18 5 18; -fx-background-radius: 4; -fx-cursor: hand;");
        applyBtn.setOnAction(e -> doApply());

        dropBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #B53E3E; -fx-text-fill: #ED6C63; -fx-padding: 5 12 5 12; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
        dropBtn.setOnAction(e -> doDrop());

        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5; -fx-padding: 5 14 5 14; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        HBox bottomBar = new HBox(8, helpBtn, dropBtn, spacer, applyBtn, cancelBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(header, stashListView, optionsBox, bottomBar);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        } catch (Exception ignored) {}
        setScene(scene);
    }

    private void loadStashes() {
        if (projectRoot == null || !GitService.isRepository(projectRoot)) return;
        List<GitService.StashEntry> stashes = GitService.stashListDetailed(projectRoot);
        stashListView.setItems(FXCollections.observableArrayList(stashes));
        if (!stashes.isEmpty()) {
            stashListView.getSelectionModel().select(0);
        } else {
            applyBtn.setDisable(true);
            dropBtn.setDisable(true);
        }
    }

    private void doApply() {
        GitService.StashEntry sel = stashListView.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        boolean pop = popStashCheck.isSelected();
        applyBtn.setDisable(true);
        cancelBtn.setDisable(true);

        new Thread(() -> {
            GitService.Result r = pop ? GitService.stashPop(projectRoot, sel.ref()) : GitService.stashApply(projectRoot, sel.ref());
            Platform.runLater(() -> {
                if (r.ok()) {
                    if (log != null) log.accept("\u2713 " + (pop ? "Popped" : "Applied") + " stash " + sel.ref());
                    Notification notif = new Notification(
                            "Git",
                            pop ? "Stash popped" : "Stash applied",
                            sel.message(),
                            NotificationType.INFORMATION
                    );
                    NotificationService.getInstance().notify(notif);
                    if (onUnstashed != null) onUnstashed.run();
                    close();
                } else {
                    applyBtn.setDisable(false);
                    cancelBtn.setDisable(false);
                    if (log != null) log.accept("Unstash failed: " + r.output().trim());

                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Unstash Failed");
                    err.setHeaderText("Git " + (pop ? "Pop" : "Apply") + " Failed");
                    err.setContentText(r.output().trim());
                    err.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
                    err.showAndWait();
                }
            });
        }, "lumina-git-unstash").start();
    }

    private void doDrop() {
        GitService.StashEntry sel = stashListView.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Drop Stash");
        confirm.setHeaderText("Drop stash " + sel.ref() + "?");
        confirm.setContentText("The changes stored in this stash will be permanently removed.");
        confirm.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    GitService.Result r = GitService.stashDrop(projectRoot, sel.ref());
                    Platform.runLater(() -> {
                        if (r.ok()) {
                            if (log != null) log.accept("\u2713 Dropped stash " + sel.ref());
                            loadStashes();
                            if (onUnstashed != null) onUnstashed.run();
                        } else {
                            if (log != null) log.accept("Drop stash failed: " + r.output().trim());
                        }
                    });
                }, "lumina-git-drop-stash").start();
            }
        });
    }

    // Accessors for testing
    public ListView<GitService.StashEntry> getStashListView() { return stashListView; }
    public CheckBox getPopStashCheck() { return popStashCheck; }
    public CheckBox getReinstateIndexCheck() { return reinstateIndexCheck; }
    public Button getApplyBtn() { return applyBtn; }
    public Button getDropBtn() { return dropBtn; }
    public Button getCancelBtn() { return cancelBtn; }
}
