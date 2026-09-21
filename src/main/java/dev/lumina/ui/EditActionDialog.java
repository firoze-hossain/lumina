package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Modal dialog for editing an action's display name or icon.
 */
public class EditActionDialog {

    private final Stage stage;
    private boolean confirmed = false;
    private final TextField nameField;
    private final TextField iconField;

    public EditActionDialog(Window owner, CustomActionItem item) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Edit Action");
        stage.setResizable(false);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");
        root.setPadding(new Insets(16, 20, 16, 20));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.setPadding(new Insets(10, 0, 14, 0));

        Label nameLbl = new Label("Text:");
        nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        nameField = new TextField(item != null ? item.getText() : "");
        nameField.setPrefWidth(260);
        nameField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8 5 8;");

        Label iconLbl = new Label("Icon glyph:");
        iconLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        iconField = new TextField(item != null ? item.getIconGlyph() : "");
        iconField.setPrefWidth(80);
        iconField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8 5 8;");

        form.add(nameLbl, 0, 0);
        form.add(nameField, 1, 0);
        form.add(iconLbl, 0, 1);
        form.add(iconField, 1, 1);

        root.setCenter(form);

        // Buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        cancelBtn.setOnAction(e -> stage.close());

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        okBtn.setOnAction(e -> {
            confirmed = true;
            stage.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonBar.getChildren().addAll(spacer, cancelBtn, okBtn);
        root.setBottom(buttonBar);

        Scene scene = new Scene(root, 360, 180);
        stage.setScene(scene);
    }

    public boolean showAndWait(CustomActionItem item) {
        stage.showAndWait();
        if (confirmed && item != null) {
            item.setText(nameField.getText().trim());
            item.setIconGlyph(iconField.getText().trim());
            item.setCustom(true);
            return true;
        }
        return false;
    }
}
