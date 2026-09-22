package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.nio.file.Path;

/**
 * IntelliJ-style "Add File to Git" confirmation modal dialog.
 * Prompts user to stage newly created files with a "Do not ask again" option.
 */
public class AddFileToGitDialog extends Stage {

    public record Decision(boolean add, boolean doNotAskAgain) {}

    private Decision result = new Decision(false, false);

    private final CheckBox doNotAskAgainCheck = new CheckBox("Do not ask again");

    public AddFileToGitDialog(Window owner, Path file, Path repoRoot) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Add File to Git");
        setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        root.setPrefWidth(460);

        // Header message
        Label prompt = new Label("Do you want to add the following file to Git?");
        prompt.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");

        // Display relative or full file path
        String displayPath;
        if (repoRoot != null && file.isAbsolute()) {
            try {
                displayPath = repoRoot.relativize(file).toString().replace('\\', '/');
            } catch (Exception e) {
                displayPath = file.toString().replace('\\', '/');
            }
        } else {
            displayPath = file.toString().replace('\\', '/');
        }

        Label fileIcon = new Label("📄");
        fileIcon.setStyle("-fx-font-size: 13px;");

        Label pathLabel = new Label(displayPath);
        pathLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-family: monospace;");

        HBox pathBox = new HBox(8, fileIcon, pathLabel);
        pathBox.setAlignment(Pos.CENTER_LEFT);
        pathBox.setPadding(new Insets(8, 12, 8, 12));
        pathBox.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        // "Do not ask again" checkbox
        doNotAskAgainCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        // Bottom action bar
        Button helpBtn = new Button("?");
        helpBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E95; -fx-border-color: #393B40; -fx-border-radius: 12; -fx-min-width: 24; -fx-min-height: 24; -fx-font-size: 11px; -fx-cursor: hand;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 16; -fx-font-size: 12px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> {
            result = new Decision(false, doNotAskAgainCheck.isSelected());
            close();
        });

        Button addBtn = new Button("Add");
        addBtn.setDefaultButton(true);
        addBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 20; -fx-font-size: 12px; -fx-cursor: hand;");
        addBtn.setOnAction(e -> {
            result = new Decision(true, doNotAskAgainCheck.isSelected());
            close();
        });

        HBox rightBtns = new HBox(8, cancelBtn, addBtn);
        rightBtns.setAlignment(Pos.CENTER_RIGHT);

        BorderPane bottomBar = new BorderPane();
        bottomBar.setLeft(helpBtn);
        bottomBar.setRight(rightBtns);
        bottomBar.setPadding(new Insets(6, 0, 0, 0));

        root.getChildren().addAll(prompt, pathBox, doNotAskAgainCheck, bottomBar);

        Scene scene = new Scene(root);
        scene.setFill(Color.web("#1E1F22"));
        setScene(scene);
    }

    public Decision showAndGet() {
        showAndWait();
        return result;
    }
}
