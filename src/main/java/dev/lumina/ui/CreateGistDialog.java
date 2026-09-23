package dev.lumina.ui;

import dev.lumina.git.GitHubAccountManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Modal dialog for "GitHub > Create Gist..." matching IntelliJ IDEA.
 */
public class CreateGistDialog extends Stage {

    private final String initialFileName;
    private final String initialContent;
    private final Consumer<String> onGistCreated;

    private final TextField fileNameField = new TextField();
    private final TextField descField = new TextField();
    private final CheckBox secretCheck = new CheckBox("Secret");
    private final TextArea contentArea = new TextArea();
    private final Button createBtn = new Button("Create Gist");
    private final Button cancelBtn = new Button("Cancel");
    private final Label statusLbl = new Label();

    public CreateGistDialog(Stage owner, String initialFileName, String initialContent, Consumer<String> onGistCreated) {
        this.initialFileName = initialFileName != null ? initialFileName : "snippet.txt";
        this.initialContent = initialContent != null ? initialContent : "";
        this.onGistCreated = onGistCreated;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Create Gist");
        setMinWidth(520);
        setMinHeight(380);
        setWidth(580);
        setHeight(420);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1E1F22;");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 18, 12, 18));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label fnLbl = new Label("File name:");
        fnLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        fileNameField.setText(this.initialFileName);
        fileNameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
        GridPane.setHgrow(fileNameField, Priority.ALWAYS);

        Label descLbl = new Label("Description:");
        descLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        descField.setPromptText("Optional description");
        descField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
        GridPane.setHgrow(descField, Priority.ALWAYS);

        secretCheck.setSelected(true);
        secretCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        grid.add(fnLbl, 0, 0);
        grid.add(fileNameField, 1, 0);
        grid.add(descLbl, 0, 1);
        grid.add(descField, 1, 1);

        contentArea.setText(this.initialContent);
        contentArea.setStyle("-fx-control-inner-background: #1E1F22; -fx-text-fill: #DFE1E5; -fx-font-family: monospace;");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        content.getChildren().addAll(grid, secretCheck, new Label("Content:"), contentArea);
        root.setCenter(content);

        // Bottom Bar
        HBox bottomBar = new HBox(8);
        bottomBar.setPadding(new Insets(10, 18, 12, 18));
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2B2D30 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        statusLbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        createBtn.setDefaultButton(true);
        createBtn.setStyle(
                "-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 4; -fx-padding: 5 18; -fx-cursor: hand;"
        );
        createBtn.setOnAction(e -> doCreateGist());

        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle(
                "-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; " +
                "-fx-background-radius: 4; -fx-padding: 5 14; -fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> close());

        bottomBar.getChildren().addAll(statusLbl, spacer, createBtn, cancelBtn);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root);
        try {
            var css = getClass().getResource("/css/lumina-dark.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        setScene(scene);
    }

    private void doCreateGist() {
        String fn = fileNameField.getText().trim();
        if (fn.isEmpty()) fn = "snippet.txt";
        String desc = descField.getText().trim();
        boolean isPublic = !secretCheck.isSelected();
        String fileContent = contentArea.getText();

        var defAcc = GitHubAccountManager.getInstance().getDefaultAccount();
        String token = defAcc != null ? defAcc.getToken() : null;

        createBtn.setDisable(true);
        statusLbl.setText("Creating gist...");

        final String finalFn = fn;
        Thread t = new Thread(() -> {
            try {
                // Escape JSON strings
                String jsonBody = "{"
                        + "\"description\":\"" + escapeJson(desc) + "\","
                        + "\"public\":" + isPublic + ","
                        + "\"files\":{\"" + escapeJson(finalFn) + "\":{\"content\":\"" + escapeJson(fileContent) + "\"}}"
                        + "}";

                var reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/gists"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/vnd.github.v3+json");

                if (token != null && !token.isBlank()) {
                    reqBuilder.header("Authorization", "Bearer " + token);
                }

                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> resp = client.send(reqBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build(), HttpResponse.BodyHandlers.ofString());

                javafx.application.Platform.runLater(() -> {
                    createBtn.setDisable(false);
                    if (resp.statusCode() == 201) {
                        String body = resp.body();
                        // Find html_url
                        int idx = body.indexOf("\"html_url\":\"");
                        String gistUrl = "https://gist.github.com";
                        if (idx >= 0) {
                            int end = body.indexOf("\"", idx + 12);
                            if (end > idx) {
                                gistUrl = body.substring(idx + 12, end);
                            }
                        }
                        close();
                        if (onGistCreated != null) {
                            onGistCreated.accept(gistUrl);
                        }
                    } else {
                        statusLbl.setText("");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.initOwner(this);
                        alert.setTitle("Gist Creation Failed");
                        alert.setHeaderText("GitHub API returned " + resp.statusCode());
                        alert.setContentText(resp.body());
                        alert.showAndWait();
                    }
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    createBtn.setDisable(false);
                    statusLbl.setText("");
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.initOwner(this);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to create Gist");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                });
            }
        }, "lumina-create-gist");
        t.setDaemon(true);
        t.start();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
