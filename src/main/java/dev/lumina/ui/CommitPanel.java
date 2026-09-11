package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * IntelliJ's Commit tool window, backed by the real git CLI: lists changed
 * files from {@code git status}, lets you pick which ones to stage, and
 * commits (optionally pushes) via {@link GitService}.
 */
public final class CommitPanel extends VBox {

    private final Supplier<Path> projectRoot;
    private final Consumer<String> log;
    private final VBox changesList = new VBox(2);
    private final TextArea message = new TextArea();
    private final Label status = new Label();

    public CommitPanel(Supplier<Path> projectRoot, Consumer<String> log) {
        this.projectRoot = projectRoot;
        this.log = log;
        getStyleClass().add("commit-panel");
        setSpacing(8);
        setPadding(new Insets(10));

        Label header = new Label("Changes");
        header.getStyleClass().add("panel-header");

        Button refresh = new Button("\u21BB");
        refresh.getStyleClass().add("console-button");
        refresh.setTooltip(new javafx.scene.control.Tooltip("Refresh"));
        refresh.setOnAction(e -> refresh());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerRow = new HBox(6, header, spacer, refresh);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(changesList);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("commit-changes-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        message.setPromptText("Commit message");
        message.setPrefRowCount(4);
        message.setWrapText(true);
        message.getStyleClass().add("commit-message");

        Button commit = new Button("Commit");
        commit.getStyleClass().add("dialog-primary");
        commit.setOnAction(e -> doCommit(false));

        Button commitAndPush = new Button("Commit and Push\u2026");
        commitAndPush.getStyleClass().add("dialog-secondary");
        commitAndPush.setOnAction(e -> doCommit(true));

        HBox buttons = new HBox(8, commit, commitAndPush);

        status.getStyleClass().add("side-subtle");
        status.setWrapText(true);

        getChildren().addAll(headerRow, scroll, message, buttons, status);
    }

    /** Re-reads {@code git status} and rebuilds the checkbox list. */
    public void refresh() {
        Path dir = projectRoot.get();
        changesList.getChildren().clear();
        if (dir == null || !GitService.isRepository(dir)) {
            status.setText("Not a git repository.");
            return;
        }
        GitService.Result r = GitService.status(dir);
        if (!r.ok()) {
            status.setText("git status failed: " + r.output().trim());
            return;
        }
        List<String> files = new ArrayList<>();
        for (String line : r.output().split("\\R")) {
            if (line.isBlank() || line.startsWith("##")) continue;
            // porcelain short format: "XY path" (X/Y = 1-char status codes)
            String path = line.length() > 3 ? line.substring(3).trim() : line.trim();
            files.add(path);
        }
        if (files.isEmpty()) {
            changesList.getChildren().add(new Label("No changes detected."));
        } else {
            for (String f : files) {
                CheckBox cb = new CheckBox(f);
                cb.setSelected(true);
                changesList.getChildren().add(cb);
            }
        }
        status.setText(files.size() + " file(s) changed.");
    }

    private void doCommit(boolean push) {
        Path dir = projectRoot.get();
        if (dir == null || !GitService.isRepository(dir)) {
            status.setText("Not a git repository.");
            return;
        }
        String msg = message.getText().trim();
        if (msg.isEmpty()) {
            status.setText("Enter a commit message first.");
            return;
        }
        List<String> selected = new ArrayList<>();
        for (javafx.scene.Node n : changesList.getChildren()) {
            if (n instanceof CheckBox cb && cb.isSelected()) {
                selected.add(cb.getText());
            }
        }
        if (selected.isEmpty()) {
            status.setText("Nothing selected to commit.");
            return;
        }
        status.setText("Committing\u2026");
        Thread t = new Thread(() -> {
            GitService.Result add = GitService.add(dir, selected);
            if (!add.ok()) {
                Platform.runLater(() -> status.setText("git add failed: " + add.output().trim()));
                return;
            }
            GitService.Result commit = GitService.commit(dir, msg);
            if (!commit.ok()) {
                Platform.runLater(() -> status.setText("Commit failed: " + commit.output().trim()));
                return;
            }
            String pushMsg = "";
            if (push) {
                GitService.Result pushResult = GitService.push(dir);
                pushMsg = pushResult.ok() ? " and pushed" : " (push failed: "
                        + pushResult.output().trim() + ")";
            }
            String finalPushMsg = pushMsg;
            Platform.runLater(() -> {
                log.accept("\u2713 Committed" + finalPushMsg + ": " + msg);
                message.clear();
                status.setText("Committed" + finalPushMsg + ".");
                refresh();
            });
        }, "lumina-git-commit");
        t.setDaemon(true);
        t.start();
    }
}