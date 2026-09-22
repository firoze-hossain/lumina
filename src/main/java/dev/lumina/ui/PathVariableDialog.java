package dev.lumina.ui;

import dev.lumina.pathvar.PathVariable;
import java.io.File;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Modal dialog for adding or editing a Path Variable.
 * Strictly matches media_1790046850137.png.
 */
public class PathVariableDialog {

    private final Stage stage;
    private final TextField nameField = new TextField();
    private final TextField valueField = new TextField();
    private PathVariable result = null;

    public PathVariableDialog(Window owner, PathVariable existing) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(existing != null ? "Edit Variable" : "Add Variable");
        stage.setResizable(false);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");
        root.setPadding(new Insets(16, 20, 16, 20));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label("Name:");
        nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        nameField.setPrefWidth(260);
        nameField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8 5 8; -fx-font-size: 12px;");

        Label valueLbl = new Label("Value:");
        valueLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox valueContainer = new HBox(0);
        valueContainer.setAlignment(Pos.CENTER_LEFT);
        valueContainer.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        valueContainer.setPrefWidth(260);

        valueField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #DFE1E5; -fx-padding: 5 8 5 8; -fx-font-size: 12px;");
        HBox.setHgrow(valueField, Priority.ALWAYS);

        Button browseBtn = new Button("📁");
        browseBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Path Variable Directory");
            String current = valueField.getText().trim();
            if (!current.isEmpty()) {
                File curFile = new File(current);
                if (curFile.exists() && curFile.isDirectory()) {
                    chooser.setInitialDirectory(curFile);
                }
            }
            File chosen = chooser.showDialog(stage);
            if (chosen != null) {
                valueField.setText(chosen.getAbsolutePath().replace('\\', '/'));
            }
        });

        valueContainer.getChildren().addAll(valueField, browseBtn);

        if (existing != null) {
            nameField.setText(existing.getName());
            valueField.setText(existing.getValue());
        }

        grid.add(nameLbl, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(valueLbl, 0, 1);
        grid.add(valueContainer, 1, 1);

        root.setCenter(grid);

        // Bottom Button Bar
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(16, 0, 0, 0));

        Button helpBtn = new Button("?");
        helpBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #4E5157; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px; -fx-max-width: 24px; -fx-max-height: 24px; -fx-padding: 0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        cancelBtn.setOnAction(e -> stage.close());

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        okBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String value = valueField.getText().trim();
            if (!name.isEmpty()) {
                result = new PathVariable(name, value);
            }
            stage.close();
        });

        buttonBar.getChildren().addAll(helpBtn, spacer, cancelBtn, okBtn);
        root.setBottom(buttonBar);

        Scene scene = new Scene(root, 360, 160);
        stage.setScene(scene);
    }

    public PathVariable showAndWait() {
        stage.showAndWait();
        return result;
    }
}
