package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * A flat {@code git log}, most-recent first, with the current branch shown
 * above it. IntelliJ's real Git tool window renders a full commit graph
 * with branch/merge lines; this is a simpler list view over the same data,
 * which is the practical two-thirds of what people actually use it for.
 */
public final class GitLogPanel extends VBox {

    private final Supplier<Path> projectRoot;
    private final Label branchLabel = new Label();
    private final ListView<String[]> log = new ListView<>();

    public GitLogPanel(Supplier<Path> projectRoot) {
        this.projectRoot = projectRoot;
        getStyleClass().add("git-log-panel");
        setSpacing(6);
        setPadding(new Insets(8));
        branchLabel.getStyleClass().add("panel-header");
        VBox.setVgrow(log, Priority.ALWAYS);
        log.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(String[] c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label subject = new Label(c[3]);
                subject.getStyleClass().add("git-log-subject");
                Label meta = new Label(c[0] + "  \u00b7  " + c[1] + "  \u00b7  " + c[2]);
                meta.getStyleClass().add("side-subtle");
                VBox box = new VBox(1, subject, meta);
                setGraphic(box);
                setText(null);
            }
        });
        getChildren().addAll(branchLabel, log);
    }

    public void refresh() {
        Path dir = projectRoot.get();
        log.getItems().clear();
        if (dir == null || !GitService.isRepository(dir)) {
            branchLabel.setText("Not a git repository");
            return;
        }
        String branch = GitService.currentBranch(dir);
        branchLabel.setText(branch != null ? "\u2387 " + branch : "Git");

        Thread t = new Thread(() -> {
            GitService.Result r = GitService.log(dir, 200);
            Platform.runLater(() -> {
                if (!r.ok()) {
                    log.getItems().clear();
                    return;
                }
                for (String line : r.output().split("\\R")) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split("\u0001", 4);
                    if (parts.length == 4) log.getItems().add(parts);
                }
            });
        }, "lumina-git-log");
        t.setDaemon(true);
        t.start();
    }
}