package dev.lumina.filetypes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Dialog for adding or editing a wildcard pattern, exactly matching
 * IntelliJ IDEA's "Add Wildcard" dialog (media_1791255990117.png).
 */
public class AddWildcardDialog {

    private final Stage stage;
    private final TextField patternField;
    private String result = null;

    public AddWildcardDialog(Window owner, String initialPattern) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle(initialPattern == null || initialPattern.isEmpty() ? "Add Wildcard" : "Edit Wildcard");
        stage.setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(16, 20, 16, 20));
        root.setStyle("-fx-background-color: #2B2D30;");

        Label label = new Label("Enter new wildcard ('*' and '?' allowed):");
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        patternField = new TextField(initialPattern != null ? initialPattern : "");
        patternField.setStyle(
                "-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 6 10; -fx-font-size: 13px;"
        );
        patternField.setPrefWidth(380);

        HBox buttonBar = new HBox(8);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle(
                "-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-padding: 5 18; -fx-background-radius: 4; -fx-cursor: hand;"
        );

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle(
                "-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; " +
                "-fx-padding: 5 16; -fx-background-radius: 4; -fx-cursor: hand;"
        );

        okBtn.setOnAction(e -> {
            String text = patternField.getText().trim();
            if (!text.isEmpty()) {
                result = text;
                stage.close();
            }
        });

        cancelBtn.setOnAction(e -> stage.close());

        buttonBar.getChildren().addAll(okBtn, cancelBtn);

        root.getChildren().addAll(label, patternField, buttonBar);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    public static Optional<String> show(Window owner, String initialPattern) {
        AddWildcardDialog dialog = new AddWildcardDialog(owner, initialPattern);
        dialog.stage.showAndWait();
        return Optional.ofNullable(dialog.result);
    }
}
