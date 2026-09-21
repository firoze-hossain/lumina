package dev.lumina.ui;

import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Modal dialog for selecting an IDE action to add into a menu or toolbar.
 */
public class AddActionDialog {

    private final Stage stage;
    private CustomActionItem selectedAction = null;

    public AddActionDialog(Window owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Add Action");
        stage.setResizable(true);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");
        root.setPadding(new Insets(14, 16, 14, 16));

        // Top Search Field
        TextField searchField = new TextField();
        searchField.setPromptText("Filter actions by name...");
        searchField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10 6 10; -fx-prompt-text-fill: #6F737A;");

        // Action List
        List<CustomActionItem> catalog = CustomActionsSchema.getInstance().getAllAvailableActions();
        ObservableList<CustomActionItem> masterData = FXCollections.observableArrayList(catalog);
        FilteredList<CustomActionItem> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            String query = (newV == null) ? "" : newV.trim().toLowerCase();
            filteredData.setPredicate(item -> {
                if (query.isEmpty()) return true;
                return item.getText().toLowerCase().contains(query) ||
                        item.getId().toLowerCase().contains(query);
            });
        });

        ListView<CustomActionItem> listView = new ListView<>(filteredData);
        listView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        listView.setCellFactory(lv -> new ListCell<>() {
            {
                selectedProperty().addListener((obs, oldV, newV) -> {
                    CustomActionItem item = getItem();
                    if (item != null && !isEmpty()) {
                        String bg = newV ? "#2E436E" : "#1E1F22";
                        String textFill = newV ? "#FFFFFF" : "#DFE1E5";
                        setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textFill + "; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                    } else {
                        setStyle("-fx-background-color: #1E1F22;");
                    }
                });
            }

            @Override
            protected void updateItem(CustomActionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    String prefix = item.getIconGlyph().isEmpty() ? "   " : item.getIconGlyph() + "  ";
                    setText(prefix + item.getText());
                    String bg = isSelected() ? "#2E436E" : "#1E1F22";
                    String textFill = isSelected() ? "#FFFFFF" : "#DFE1E5";
                    setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textFill + "; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                }
            }
        });

        VBox centerBox = new VBox(10, searchField, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);
        centerBox.setPadding(new Insets(10, 0, 10, 0));
        root.setCenter(centerBox);

        // Bottom Buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
        cancelBtn.setOnAction(e -> stage.close());

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        okBtn.setDisable(true);
        okBtn.setStyle("-fx-background-color: #2E3034; -fx-border-color: #3E4146; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #5E626B; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 14 5 14;");

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean hasSel = (newV != null);
            okBtn.setDisable(!hasSel);
            if (hasSel) {
                okBtn.setStyle("-fx-background-color: #3574F0; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
            } else {
                okBtn.setStyle("-fx-background-color: #2E3034; -fx-border-color: #3E4146; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #5E626B; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 14 5 14;");
            }
        });

        okBtn.setOnAction(e -> {
            CustomActionItem sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                selectedAction = sel.deepCopy();
                selectedAction.setCustom(true);
            }
            stage.close();
        });

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                okBtn.fire();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonBar.getChildren().addAll(spacer, cancelBtn, okBtn);
        root.setBottom(buttonBar);

        Scene scene = new Scene(root, 420, 480);
        stage.setScene(scene);
        stage.setMinWidth(360);
        stage.setMinHeight(380);
    }

    public CustomActionItem showAndWait() {
        stage.showAndWait();
        return selectedAction;
    }
}
