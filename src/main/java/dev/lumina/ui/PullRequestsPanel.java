package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IntelliJ's Pull Requests tool window: resolves the current repo's GitHub
 * remote, lists open PRs via the REST API using the signed-in token, and
 * falls back to the same "could not connect... log in again" messaging
 * IntelliJ itself shows when there's no valid session.
 */
public final class PullRequestsPanel extends VBox {

    private final Supplier<Path> projectRoot;
    private final Supplier<String> tokenSupplier;
    private final Supplier<String> userSupplier;
    private final Runnable onSignIn;
    private final Consumer<String> openBrowser;

    private final Label repoLabel = new Label();
    private final VBox body = new VBox(6);

    private static final Pattern PR = Pattern.compile(
            "\"number\"\\s*:\\s*(\\d+).*?\"title\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
                    + ".*?\"login\"\\s*:\\s*\"([^\"]+)\".*?\"html_url\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.DOTALL);

    public PullRequestsPanel(Supplier<Path> projectRoot, Supplier<String> tokenSupplier,
                              Supplier<String> userSupplier, Runnable onSignIn,
                              Consumer<String> openBrowser) {
        this.projectRoot = projectRoot;
        this.tokenSupplier = tokenSupplier;
        this.userSupplier = userSupplier;
        this.onSignIn = onSignIn;
        this.openBrowser = openBrowser;

        getStyleClass().add("pull-requests-panel");
        setSpacing(10);
        setPadding(new Insets(14));

        Label header = new Label("Pull Requests");
        header.getStyleClass().add("panel-header");
        repoLabel.getStyleClass().add("side-subtle");

        getChildren().addAll(header, repoLabel, body);
    }

    public void refresh() {
        body.getChildren().clear();
        Path dir = projectRoot.get();
        String remote = dir != null ? GitService.remoteBrowserUrl(dir) : null;
        if (remote == null) {
            repoLabel.setText("");
            body.getChildren().add(new Label("No GitHub remote found for this project."));
            return;
        }
        String repoPath = remote.replaceFirst("^https://github\\.com/", "");
        repoLabel.setText(repoPath + "  \u00b7  origin");

        String token = tokenSupplier.get();
        String user = userSupplier.get();
        if (token == null || user == null) {
            showSignInPrompt(repoPath);
            return;
        }

        Label loading = new Label("Loading pull requests\u2026");
        body.getChildren().add(loading);

        Thread t = new Thread(() -> {
            String result = fetch(repoPath, token);
            Platform.runLater(() -> {
                body.getChildren().clear();
                if (result == null) {
                    showConnectError(repoPath, user);
                } else if (result.isBlank()) {
                    body.getChildren().add(new Label("No open pull requests."));
                } else {
                    List<String[]> prs = parse(result);
                    if (prs.isEmpty()) {
                        body.getChildren().add(new Label("No open pull requests."));
                    } else {
                        for (String[] pr : prs) {
                            body.getChildren().add(prRow(pr));
                        }
                    }
                }
            });
        }, "lumina-pr-fetch");
        t.setDaemon(true);
        t.start();
    }

    private VBox prRow(String[] pr) {
        Hyperlink link = new Hyperlink("#" + pr[0] + "  " + pr[1]);
        link.getStyleClass().add("pr-link");
        link.setOnAction(e -> openBrowser.accept(pr[3]));
        Label author = new Label("by " + pr[2]);
        author.getStyleClass().add("side-subtle");
        VBox row = new VBox(1, link, author);
        row.getStyleClass().add("pr-row");
        return row;
    }

    private void showSignInPrompt(String repoPath) {
        Label msg = new Label("Sign in to GitHub to see pull requests for " + repoPath + ".");
        msg.setWrapText(true);
        Hyperlink signIn = new Hyperlink("Log in\u2026");
        signIn.setOnAction(e -> onSignIn.run());
        body.getChildren().addAll(msg, signIn);
    }

    private void showConnectError(String repoPath, String user) {
        // Mirrors IntelliJ's own wording for this exact failure state.
        Label msg = new Label("Could not connect to repository github.com/" + repoPath
                + " with account github.com/" + user + "\n"
                + "Can't get repository\n\n"
                + "Could not resolve to a Repository with the name '" + repoPath + "'.");
        msg.setWrapText(true);
        msg.getStyleClass().add("form-error");
        Hyperlink again = new Hyperlink("Log in again\u2026");
        again.setOnAction(e -> onSignIn.run());
        Label hint = new Label("Change repository or account later under the More icon");
        hint.getStyleClass().add("side-subtle");
        body.getChildren().addAll(msg, again, hint);
    }

    /** Returns the raw JSON array body, "" on a clean empty list, or null on failure. */
    private static String fetch(String repoPath, String token) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("https://api.github.com/repos/" + repoPath
                                    + "/pulls?state=open&per_page=30"))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .timeout(Duration.ofSeconds(15))
                    .GET().build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            String body = response.body().trim();
            return body.equals("[]") ? "" : body;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String[]> parse(String json) {
        List<String[]> out = new ArrayList<>();
        Matcher m = PR.matcher(json);
        while (m.find()) {
            String title = m.group(2).replace("\\\"", "\"").replace("\\\\", "\\");
            out.add(new String[]{m.group(1), title, m.group(3), m.group(4)});
        }
        return out;
    }
}