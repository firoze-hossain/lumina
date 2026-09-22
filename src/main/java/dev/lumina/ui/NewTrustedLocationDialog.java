package dev.lumina.ui;

import java.io.File;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Modal dialog for specifying a new trusted location.
 * Strictly matches media_1790046849995.png.
 */
public class NewTrustedLocationDialog {

    private final Stage stage;
    private final TextField pathField = new TextField();
    private String result = null;

    public NewTrustedLocationDialog(Window owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("New Trusted Location");
        stage.setResizable(false);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");
        root.setPadding(new Insets(16, 20, 16, 20));

        // Center Input with Browse Folder button
        HBox inputRow = new HBox(0);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        pathField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #DFE1E5; -fx-padding: 6 10 6 10; -fx-font-size: 12px;");
        pathField.setPrefWidth(320);
        HBox.setHgrow(pathField, Priority.ALWAYS);

        Button browseBtn = new Button("📁");
        browseBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Trusted Directory");
            String current = pathField.getText().trim();
            if (!current.isEmpty()) {
                File curFile = new File(current);
                if (curFile.exists() && curFile.isDirectory()) {
                    chooser.setInitialDirectory(curFile);
                }
            }
            File chosen = chooser.showDialog(stage);
            if (chosen != null) {
                pathField.setText(chosen.getAbsolutePath());
            }
        });

        inputRow.getChildren().addAll(pathField, browseBtn);
        root.setCenter(inputRow);

        // Bottom Button Bar
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(16, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        cancelBtn.setOnAction(e -> stage.close());

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        okBtn.setOnAction(e -> {
            String path = pathField.getText().trim();
            if (!path.isEmpty()) {
                result = path;
            }
            stage.close();
        });

        buttonBar.getChildren().addAll(cancelBtn, okBtn);
        root.setBottom(buttonBar);

        Scene scene = new Scene(root, 390, 120);
        stage.setScene(scene);
    }

    public String showAndWait() {
        stage.showAndWait();
        return result;
    }
}
