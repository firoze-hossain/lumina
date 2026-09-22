package dev.lumina.ui;

import dev.lumina.git.GitLabAccountManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modal dialog matching IntelliJ IDEA Image 4: "Add GitLab Account".
 */
public class AddGitLabAccountDialog extends Stage {

    private final TextField serverField = new TextField("https://gitlab.com");
    private final PasswordField tokenField = new PasswordField();
    private final Label statusLabel = new Label();
    private final Button loginBtn = new Button("Log In");
    private final Button cancelBtn = new Button("Cancel");

    public AddGitLabAccountDialog(Window owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Add GitLab Account");
        setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(20, 24, 20, 24));
        root.setStyle("-fx-background-color: #2B2D30; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        Label serverLabel = new Label("Server:");
        serverLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        serverField.setPrefWidth(280);
        serverField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10 6 10; -fx-font-size: 13px;");

        Label tokenLabel = new Label("Token:");
        tokenLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        tokenField.setPrefWidth(200);
        tokenField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10 6 10; -fx-font-size: 13px;");
        HBox.setHgrow(tokenField, Priority.ALWAYS);

        Button generateBtn = new Button("Generate...");
        generateBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 12 6 12; -fx-font-size: 12px; -fx-cursor: hand;");
        generateBtn.setOnAction(e -> {
            try {
                String server = serverField.getText().trim();
                if (!server.startsWith("http://") && !server.startsWith("https://")) {
                    server = "https://" + server;
                }
                while (server.endsWith("/")) server = server.substring(0, server.length() - 1);
                String url = server + "/-/profile/personal_access_tokens?name=Lumina+IDE&scopes=api,read_user";
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(url));
                }
            } catch (Exception ignored) {}
        });

        HBox tokenBox = new HBox(8, tokenField, generateBtn);
        tokenBox.setAlignment(Pos.CENTER_LEFT);

        Label scopesLabel = new Label("The following scopes must be granted to the access token: [api, read_user]");
        scopesLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

        grid.add(serverLabel, 0, 0);
        grid.add(serverField, 1, 0);
        grid.add(tokenLabel, 0, 1);
        grid.add(tokenBox, 1, 1);
        grid.add(scopesLabel, 1, 2);

        statusLabel.setStyle("-fx-text-fill: #ED5E62; -fx-font-size: 11px;");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(12, 0, 0, 0));

        loginBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 18 6 18; -fx-background-radius: 4; -fx-cursor: hand;");
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> onLogin());

        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 16 6 16; -fx-font-size: 13px; -fx-cursor: hand;");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(e -> close());

        buttonBar.getChildren().addAll(loginBtn, cancelBtn);
        root.getChildren().addAll(grid, statusLabel, buttonBar);

        Scene scene = new Scene(root, 440, 210);
        setScene(scene);
    }

    private void onLogin() {
        String token = tokenField.getText().trim();
        if (token.isEmpty()) {
            showError("Token cannot be empty");
            return;
        }

        String server = serverField.getText().trim();
        if (!server.startsWith("http://") && !server.startsWith("https://")) {
            server = "https://" + server;
        }
        while (server.endsWith("/")) server = server.substring(0, server.length() - 1);
        final String finalServer = server;

        statusLabel.setText("Verifying with GitLab...");
        statusLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        loginBtn.setDisable(true);

        Thread t = new Thread(() -> {
            String[] userAndName = validateGitLabToken(finalServer, token);
            Platform.runLater(() -> {
                loginBtn.setDisable(false);
                if (userAndName == null) {
                    showError("Invalid token or cannot connect to GitLab");
                    return;
                }
                GitLabAccountManager.getInstance().addAccount(
                        new GitLabAccountManager.GitLabAccount(userAndName[1], userAndName[0], finalServer, token, true)
                );
                close();
            });
        }, "gitlab-auth-verify");
        t.setDaemon(true);
        t.start();
    }

    private String[] validateGitLabToken(String server, String token) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(server + "/api/v4/user"))
                    .header("PRIVATE-TOKEN", token)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String body = resp.body();
                Pattern uPat = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"");
                Pattern nPat = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                Matcher um = uPat.matcher(body);
                Matcher nm = nPat.matcher(body);
                String username = um.find() ? um.group(1) : "gitlab-user";
                String name = nm.find() ? nm.group(1) : username;
                return new String[]{username, name};
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #ED5E62; -fx-font-size: 11px;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
