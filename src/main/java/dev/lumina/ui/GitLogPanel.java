package dev.lumina.ui;

import dev.lumina.git.GitService;
import dev.lumina.git.IssueNavigationManager;
import dev.lumina.git.VcsLogSettingsManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A flat {@code git log}, most-recent first, with the current branch shown
 * above it. Incorporates dynamic issue navigation link resolution and VCS Log settings.
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

                Node subjectNode = buildSubjectNode(c[3]);

                VcsLogSettingsManager logSettings = VcsLogSettingsManager.getInstance();
                List<String> metaParts = new ArrayList<>();
                if (logSettings.isHashVisible()) {
                    metaParts.add(c[0]);
                }
                if (logSettings.isAuthorVisible()) {
                    metaParts.add(c[1]);
                }
                if (logSettings.isDateVisible()) {
                    metaParts.add(c[2]);
                }
                if (metaParts.isEmpty()) {
                    metaParts.add(c[1]);
                    metaParts.add(c[2]);
                }

                Label meta = new Label(String.join("  \u00b7  ", metaParts));
                meta.getStyleClass().add("side-subtle");

                VBox box = new VBox(2, subjectNode, meta);
                setGraphic(box);
                setText(null);
            }
        });

        getChildren().addAll(branchLabel, log);

        // Listen for dynamic settings changes
        IssueNavigationManager.getInstance().addListener(this::refresh);
        VcsLogSettingsManager.getInstance().addListener(this::refresh);
    }

    private Node buildSubjectNode(String subjectText) {
        if (subjectText == null) subjectText = "";
        List<IssueNavigationManager.IssueMatch> matches = IssueNavigationManager.getInstance().findIssueMatches(subjectText);
        if (matches.isEmpty()) {
            Label subject = new Label(subjectText);
            subject.getStyleClass().add("git-log-subject");
            return subject;
        }

        TextFlow flow = new TextFlow();
        int cursor = 0;
        for (IssueNavigationManager.IssueMatch m : matches) {
            if (m.start() > cursor) {
                Text plain = new Text(subjectText.substring(cursor, m.start()));
                plain.setStyle("-fx-fill: #DFE1E5; -fx-font-size: 13px;");
                flow.getChildren().add(plain);
            }

            Hyperlink link = new Hyperlink(m.issueKey());
            link.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 13px; -fx-padding: 0; -fx-underline: false;");
            link.setTooltip(new Tooltip(m.targetUrl()));
            link.setOnAction(e -> {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(URI.create(m.targetUrl()));
                    }
                } catch (Exception ignored) {}
            });
            flow.getChildren().add(link);

            cursor = Math.max(cursor, m.end());
        }

        if (cursor < subjectText.length()) {
            Text tail = new Text(subjectText.substring(cursor));
            tail.setStyle("-fx-fill: #DFE1E5; -fx-font-size: 13px;");
            flow.getChildren().add(tail);
        }

        return flow;
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