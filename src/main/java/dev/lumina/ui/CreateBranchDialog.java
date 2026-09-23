package dev.lumina.ui;

import dev.lumina.git.GitService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.nio.file.Path;

/**
 * Modal dialog for creating a new Git branch from a base reference or HEAD,
 * with checkout and overwrite options.
 */
public class CreateBranchDialog extends Stage {

    private final Path projectRoot;
    private final String startPoint;
    private String createdBranchName = null;

    private final TextField nameField = new TextField();
    private final CheckBox checkoutBox = new CheckBox("Checkout branch");
    private final CheckBox overwriteBox = new CheckBox("Overwrite existing branch");
    private final Label errorLabel = new Label();

    public CreateBranchDialog(Window owner, Path projectRoot, String startPoint) {
        this.projectRoot = projectRoot;
        this.startPoint = (startPoint != null && !startPoint.isBlank()) ? startPoint.trim() : "HEAD";

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Create Branch from " + this.startPoint);
        setResizable(false);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: #2B2D30; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        root.setPrefWidth(460);

        // Branch Name row
        HBox nameRow = new HBox(10);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("Branch Name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nameLabel.setPrefWidth(100);

        // Default suggested name: if startPoint is origin/feature, suggest feature
        String suggestedName = "";
        if (this.startPoint.contains("/")) {
            suggestedName = this.startPoint.substring(this.startPoint.lastIndexOf('/') + 1);
        } else if (!"HEAD".equalsIgnoreCase(this.startPoint)) {
            suggestedName = this.startPoint;
        }

        nameField.setText(suggestedName);
        nameField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 3; -fx-padding: 5 8; -fx-font-size: 13px;");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        nameRow.getChildren().addAll(nameLabel, nameField);

        // Checkboxes row
        HBox optionsRow = new HBox(16);
        optionsRow.setAlignment(Pos.CENTER_LEFT);
        optionsRow.setPadding(new Insets(0, 0, 0, 110));

        checkoutBox.setSelected(true);
        checkoutBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        overwriteBox.setSelected(false);
        overwriteBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        optionsRow.getChildren().addAll(checkoutBox, overwriteBox);

        // Error message
        errorLabel.setStyle("-fx-text-fill: #ED6C63; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        // Button bar
        HBox buttonBar = new HBox(8);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(8, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-padding: 5 14; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        Button createBtn = new Button("Create");
        createBtn.setDefaultButton(true);
        createBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 3; -fx-padding: 5 18; -fx-cursor: hand;");
        createBtn.setOnAction(e -> doCreate());

        buttonBar.getChildren().addAll(cancelBtn, createBtn);

        root.getChildren().addAll(nameRow, optionsRow, errorLabel, buttonBar);

        root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        Scene scene = new Scene(root);
        setScene(scene);

        setOnShown(e -> {
            nameField.requestFocus();
            nameField.selectAll();
        });
    }

    private void doCreate() {
        String name = nameField.getText() != null ? nameField.getText().trim() : "";
        if (name.isEmpty()) {
            showError("Branch name cannot be empty");
            return;
        }
        if (name.contains(" ") || name.contains("~") || name.contains("^") || name.contains(":") || name.contains("\\")) {
            showError("Branch name contains invalid characters");
            return;
        }

        GitService.Result r = GitService.createBranch(
                projectRoot, name, startPoint, checkoutBox.isSelected(), overwriteBox.isSelected()
        );

        if (!r.ok()) {
            showError(r.output().isBlank() ? "Failed to create branch '" + name + "'" : r.output().trim());
            return;
        }

        this.createdBranchName = name;
        close();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    public String getCreatedBranchName() {
        return createdBranchName;
    }
}
