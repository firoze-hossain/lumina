package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Modal dialog for "Share Project on GitHub" / "Share Project on GitLab" matching IntelliJ IDEA.
 */
public class ShareProjectDialog extends Stage {

    public enum Service {
        GITHUB("GitHub", "https://github.com/"),
        GITLAB("GitLab", "https://gitlab.com/");

        final String displayName;
        final String baseServer;

        Service(String displayName, String baseServer) {
            this.displayName = displayName;
            this.baseServer = baseServer;
        }
    }

    private final Service service;
    private final Path projectRoot;
    private final String accountUsername;
    private final Map<String, String> gitEnv;
    private final Consumer<String> onSuccess;

    private final TextField repoNameField = new TextField();
    private final TextField remoteNameField = new TextField("origin");
    private final TextField descField = new TextField();
    private final CheckBox privateCheck = new CheckBox("Private");
    private final Button shareBtn = new Button("Share");
    private final Button cancelBtn = new Button("Cancel");
    private final Label statusLbl = new Label();

    public ShareProjectDialog(Stage owner, Service service, Path projectRoot,
                              String accountUsername, Map<String, String> gitEnv,
                              Consumer<String> onSuccess) {
        this.service = service;
        this.projectRoot = projectRoot;
        this.accountUsername = accountUsername != null && !accountUsername.isBlank() ? accountUsername : "user";
        this.gitEnv = gitEnv;
        this.onSuccess = onSuccess;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Share Project on " + service.displayName);
        setMinWidth(480);
        setMinHeight(260);
        setWidth(520);
        setHeight(280);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1E1F22;");

        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 20, 14, 20));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        Label repoLbl = new Label("Repository name:");
        repoLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        String folderName = projectRoot != null ? projectRoot.getFileName().toString() : "my-project";
        repoNameField.setText(folderName);
        repoNameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
        GridPane.setHgrow(repoNameField, Priority.ALWAYS);

        Label remoteLbl = new Label("Remote:");
        remoteLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        remoteNameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
        GridPane.setHgrow(remoteNameField, Priority.ALWAYS);

        Label descLbl = new Label("Description:");
        descLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        descField.setPromptText("Optional description");
        descField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
        GridPane.setHgrow(descField, Priority.ALWAYS);

        privateCheck.setSelected(true);
        privateCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        grid.add(repoLbl, 0, 0);
        grid.add(repoNameField, 1, 0);
        grid.add(remoteLbl, 0, 1);
        grid.add(remoteNameField, 1, 1);
        grid.add(descLbl, 0, 2);
        grid.add(descField, 1, 2);

        content.getChildren().addAll(grid, privateCheck);
        root.setCenter(content);

        // Bottom Bar
        HBox bottomBar = new HBox(8);
        bottomBar.setPadding(new Insets(10, 18, 12, 18));
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2B2D30 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        statusLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        shareBtn.setDefaultButton(true);
        shareBtn.setStyle(
                "-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 4; -fx-padding: 5 18; -fx-cursor: hand;"
        );
        shareBtn.setOnAction(e -> doShare());

        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle(
                "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; " +
                "-fx-background-radius: 4; -fx-padding: 5 14; -fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> close());

        bottomBar.getChildren().addAll(statusLbl, spacer, shareBtn, cancelBtn);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root);
        try {
            var css = getClass().getResource("/css/lumina-dark.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        setScene(scene);
    }

    private void doShare() {
        String repoName = repoNameField.getText().trim();
        String remoteName = remoteNameField.getText().trim();
        if (repoName.isEmpty()) {
            showError("Repository name cannot be empty");
            return;
        }
        if (remoteName.isEmpty()) {
            showError("Remote name cannot be empty");
            return;
        }
        if (projectRoot == null) return;

        shareBtn.setDisable(true);
        statusLbl.setText("Sharing project...");

        String repoUrl = service.baseServer + accountUsername + "/" + repoName + ".git";

        Thread t = new Thread(() -> {
            // Check if remote already exists
            boolean remoteExists = GitService.remotes(projectRoot).stream().anyMatch(remoteName::equalsIgnoreCase);
            GitService.Result r;
            if (remoteExists) {
                r = GitService.setRemoteUrl(projectRoot, remoteName, repoUrl);
            } else {
                r = GitService.addRemote(projectRoot, remoteName, repoUrl);
            }

            Platform.runLater(() -> {
                shareBtn.setDisable(false);
                if (r.ok()) {
                    close();
                    if (onSuccess != null) {
                        onSuccess.accept(repoUrl);
                    }
                } else {
                    statusLbl.setText("");
                    showError("Failed to configure remote: " + r.output());
                }
            });
        }, "lumina-share-project");
        t.setDaemon(true);
        t.start();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(this);
        alert.setTitle("Error");
        alert.setHeaderText("Share Project Error");
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
