package dev.lumina.ui;

import dev.lumina.git.GitHubAccountManager;
import dev.lumina.git.GitHubAuth;
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

/**
 * Modal dialog for adding a GitHub account matching IntelliJ IDEA.
 */
public class AddGitHubAccountDialog extends Stage {

    private final boolean enterprise;
    private final TextField serverField = new TextField();
    private final PasswordField tokenField = new PasswordField();
    private final Label statusLabel = new Label();
    private final Button loginBtn = new Button("Log In");
    private final Button cancelBtn = new Button("Cancel");

    public AddGitHubAccountDialog(Window owner, boolean enterprise) {
        this.enterprise = enterprise;
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle(enterprise ? "Log In to GitHub Enterprise" : "Log In to GitHub");
        setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(20, 24, 20, 24));
        root.setStyle("-fx-background-color: #2B2D30; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        Label serverLabel = new Label("Server:");
        serverLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        serverField.setText(enterprise ? "https://github.mycompany.com" : "github.com");
        serverField.setPrefWidth(280);
        serverField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10 6 10; -fx-font-size: 13px;");
        if (!enterprise) {
            serverField.setDisable(true);
        }

        Label tokenLabel = new Label("Token:");
        tokenLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        tokenField.setPromptText("ghp_...");
        tokenField.setPrefWidth(200);
        tokenField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10 6 10; -fx-font-size: 13px;");
        HBox.setHgrow(tokenField, Priority.ALWAYS);

        Button generateBtn = new Button("Generate...");
        generateBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 12 6 12; -fx-font-size: 12px; -fx-cursor: hand;");
        generateBtn.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(GitHubAuth.tokenUrl()));
                }
            } catch (Exception ignored) {}
        });

        HBox tokenBox = new HBox(8, tokenField, generateBtn);
        tokenBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(serverLabel, 0, 0);
        grid.add(serverField, 1, 0);
        grid.add(tokenLabel, 0, 1);
        grid.add(tokenBox, 1, 1);

        statusLabel.setStyle("-fx-text-fill: #ED5E62; -fx-font-size: 11px;");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        loginBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 18 6 18; -fx-background-radius: 4; -fx-cursor: hand;");
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> onLogin());

        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 16 6 16; -fx-font-size: 13px; -fx-cursor: hand;");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(e -> close());

        buttonBar.getChildren().addAll(loginBtn, cancelBtn);
        root.getChildren().addAll(grid, statusLabel, buttonBar);

        Scene scene = new Scene(root, 420, 200);
        setScene(scene);
    }

    private void onLogin() {
        String token = tokenField.getText().trim();
        if (token.isEmpty()) {
            showError("Token cannot be empty");
            return;
        }

        statusLabel.setText("Verifying token with GitHub...");
        statusLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        loginBtn.setDisable(true);

        Thread t = new Thread(() -> {
            String login = GitHubAuth.validate(token);
            Platform.runLater(() -> {
                loginBtn.setDisable(false);
                if (login == null) {
                    showError("Invalid token or connection failed");
                    return;
                }
                String server = serverField.getText().trim();
                GitHubAccountManager.getInstance().addAccount(
                        new GitHubAccountManager.GitHubAccount(login, login, server, token, "", true)
                );
                close();
            });
        }, "github-auth-verify");
        t.setDaemon(true);
        t.start();
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #ED5E62; -fx-font-size: 11px;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
