package dev.lumina.ui;

import dev.lumina.plugin.PluginManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Dialog for managing custom plugin repository URLs.
 */
public class PluginRepositoriesDialog extends Stage {

    public PluginRepositoriesDialog(Window owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Custom Plugin Repositories");

        PluginManager manager = PluginManager.getInstance();
        ObservableList<String> repos = FXCollections.observableArrayList(manager.getCustomRepositories());

        ListView<String> listView = new ListView<>(repos);
        listView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40;");
        listView.setPlaceholder(new Label("No custom plugin repositories added"));

        Button addBtn = new Button("+");
        addBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-border-color: #393B40; -fx-border-radius: 4;");
        addBtn.setOnAction(e -> {
            TextInputDialog input = new TextInputDialog("https://");
            input.initOwner(this);
            input.setTitle("Add Repository URL");
            input.setHeaderText("Enter Custom Plugin Repository URL:");
            input.setContentText("URL:");
            Optional<String> res = input.showAndWait();
            res.ifPresent(url -> {
                if (!url.isBlank() && !repos.contains(url)) {
                    repos.add(url.trim());
                    manager.addCustomRepository(url.trim());
                }
            });
        });

        Button removeBtn = new Button("−");
        removeBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-border-color: #393B40; -fx-border-radius: 4;");
        removeBtn.setOnAction(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                repos.remove(selected);
                manager.removeCustomRepository(selected);
            }
        });

        HBox toolBar = new HBox(8, addBtn, removeBtn);
        toolBar.setAlignment(Pos.CENTER_LEFT);

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 16;");
        okBtn.setOnAction(e -> close());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-padding: 6 16;");
        cancelBtn.setOnAction(e -> close());

        HBox btnBar = new HBox(10, okBtn, cancelBtn);
        btnBar.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12,
                new Label("Custom repositories provide additional plugins for Lumina IDE:"),
                toolBar,
                listView,
                btnBar
        );
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(listView, Priority.ALWAYS);

        Scene scene = new Scene(root, 520, 360);
        setScene(scene);
    }
}
