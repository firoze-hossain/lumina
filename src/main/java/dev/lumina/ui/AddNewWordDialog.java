package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Modal dialog for adding a new accepted word to the spelling dictionary.
 * Faithfully matches reference IDE screenshot (Image 4).
 */
public class AddNewWordDialog {

    private final Stage stage;
    private final TextField wordField = new TextField();
    private String result = null;

    public AddNewWordDialog(Window owner) {
        stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Add New Word");
        stage.setResizable(false);

        VBox root = new VBox(10);
        root.setStyle("-fx-background-color: #2B2D30; -fx-padding: 14 16 12 16;");
        root.setPrefWidth(320);

        Label label = new Label("Enter word:");
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        wordField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-font-size: 12px;");
        wordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleOk();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });

        HBox buttonBar = new HBox(8);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(6, 0, 0, 0));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 14; -fx-cursor: hand;");
        okBtn.setOnAction(e -> handleOk());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 12; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        buttonBar.getChildren().addAll(spacer, okBtn, cancelBtn);
        root.getChildren().addAll(label, wordField, buttonBar);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    private void handleOk() {
        String text = wordField.getText().trim();
        if (!text.isEmpty()) {
            result = text;
            stage.close();
        }
    }

    public String showAndWait() {
        stage.showAndWait();
        return result;
    }

    public TextField getWordField() {
        return wordField;
    }

    public Stage getStage() {
        return stage;
    }
}
