package dev.lumina.copyright;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Dialog for creating a new copyright profile, precisely matching
 * IntelliJ IDEA's "Create Copyright Profile" dialog (media_1791263819945.png).
 */
public class CreateCopyrightProfileDialog {

    private final Stage stage;
    private final TextField nameField;
    private CopyrightProfile result = null;

    public CreateCopyrightProfileDialog(Window owner, boolean shared) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Create Copyright Profile");
        stage.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(20, 24, 18, 24));
        root.setStyle("-fx-background-color: #2B2D30;");
        root.setPrefWidth(460);

        HBox mainRow = new HBox(14);
        mainRow.setAlignment(Pos.TOP_LEFT);

        // Circular blue question mark icon
        StackPane iconPane = new StackPane();
        Circle circle = new Circle(14, Color.web("#3574F0"));
        Label qMark = new Label("?");
        qMark.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-font-family: 'Segoe UI', sans-serif;");
        iconPane.getChildren().addAll(circle, qMark);
        iconPane.setPadding(new Insets(2, 0, 0, 0));

        VBox inputCol = new VBox(8);
        HBox.setHgrow(inputCol, Priority.ALWAYS);

        Label promptLabel = new Label("New copyright profile name:");
        promptLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        nameField = new TextField();
        nameField.setStyle(
                "-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 6 10; -fx-font-size: 13px;"
        );
        HBox.setHgrow(nameField, Priority.ALWAYS);

        inputCol.getChildren().addAll(promptLabel, nameField);
        mainRow.getChildren().addAll(iconPane, inputCol);

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
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                result = new CopyrightProfile(name, shared);
                stage.close();
            }
        });

        cancelBtn.setOnAction(e -> stage.close());

        buttonBar.getChildren().addAll(okBtn, cancelBtn);

        root.getChildren().addAll(mainRow, buttonBar);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    public static Optional<CopyrightProfile> show(Window owner, boolean shared) {
        CreateCopyrightProfileDialog dialog = new CreateCopyrightProfileDialog(owner, shared);
        dialog.stage.showAndWait();
        return Optional.ofNullable(dialog.result);
    }
}
