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
import java.util.function.Consumer;

/**
 * Modern modal dialog for creating a new Git branch matching the reference design,
 * with checkout branch and overwrite existing branch options.
 */
public class CreateBranchDialog extends Stage {

    private final Path projectRoot;
    private final String startPoint;
    private final Consumer<String> onBranchCreated;
    private String createdBranchName = null;

    private final TextField nameField = new TextField();
    private final CheckBox checkoutBox = new CheckBox("Checkout branch");
    private final CheckBox overwriteBox = new CheckBox("Overwrite existing branch");
    private final Label errorLabel = new Label();
    private final Button cancelBtn = new Button("Cancel");
    private final Button createBtn = new Button("Create");

    public CreateBranchDialog(Window owner, Path projectRoot, String startPoint) {
        this(owner, projectRoot, startPoint, null);
    }

    public CreateBranchDialog(Window owner, Path projectRoot, String startPoint, Consumer<String> onBranchCreated) {
        this.projectRoot = projectRoot;
        this.startPoint = (startPoint != null && !startPoint.isBlank()) ? startPoint.trim() : "HEAD";
        this.onBranchCreated = onBranchCreated;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle("Create New Branch");
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
        nameLabel.setPrefWidth(90);

        // Default suggested name: if startPoint is origin/feature, suggest feature, or current branch name
        String suggestedName = "";
        if (this.startPoint.contains("/")) {
            suggestedName = this.startPoint.substring(this.startPoint.lastIndexOf('/') + 1);
        } else if (!"HEAD".equalsIgnoreCase(this.startPoint)) {
            suggestedName = this.startPoint;
        } else if (projectRoot != null) {
            String cur = GitService.currentBranch(projectRoot);
            if (cur != null && !cur.isBlank()) {
                suggestedName = cur;
            }
        }

        nameField.setText(suggestedName);
        nameField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        nameRow.getChildren().addAll(nameLabel, nameField);

        // Checkboxes row
        HBox optionsRow = new HBox(20);
        optionsRow.setAlignment(Pos.CENTER_LEFT);
        optionsRow.setPadding(new Insets(2, 0, 2, 100));

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

        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 12px;");
        cancelBtn.setOnAction(e -> close());

        createBtn.setDefaultButton(true);
        createBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 4; -fx-border-radius: 4; -fx-padding: 5 18; -fx-cursor: hand; -fx-font-size: 12px;");
        createBtn.setOnAction(e -> doCreate());

        buttonBar.getChildren().addAll(cancelBtn, createBtn);

        root.getChildren().addAll(nameRow, optionsRow, errorLabel, buttonBar);

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                doCreate();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });

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
        if (onBranchCreated != null) {
            onBranchCreated.accept(name);
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    public String getCreatedBranchName() {
        return createdBranchName;
    }

    public TextField getNameField() {
        return nameField;
    }

    public CheckBox getCheckoutBox() {
        return checkoutBox;
    }

    public CheckBox getOverwriteBox() {
        return overwriteBox;
    }

    public Button getCreateButton() {
        return createBtn;
    }

    public Button getCancelButton() {
        return cancelBtn;
    }
}
