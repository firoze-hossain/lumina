package dev.lumina.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** IntelliJ-style "Go to File" popup: type to filter, Enter to open. */
public class GoToFileDialog {

    private static final Set<String> SKIP_DIRS =
            Set.of(".git", "target", "build", "node_modules", ".gradle", ".idea", "out");

    private final Stage stage = new Stage();
    private final ObservableList<Path> visible = FXCollections.observableArrayList();
    private final List<Path> allFiles;
    private final Path root;

    public GoToFileDialog(Stage owner, Path root, Consumer<Path> onOpen) {
        this.root = root;
        this.allFiles = scan(root);

        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);

        Label header = new Label("Go to File");
        header.getStyleClass().add("panel-header");

        TextField filter = new TextField();
        filter.setPromptText("Type a file name\u2026");
        filter.getStyleClass().add("goto-filter");

        ListView<Path> list = new ListView<>(visible);
        list.getStyleClass().add("goto-list");
        list.setPrefHeight(320);
        list.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                Path rel = root.relativize(item);
                setText(item.getFileName() + "   \u2014  " + rel);
            }
        });

        Runnable openSelected = () -> {
            Path sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) {
                stage.close();
                onOpen.accept(sel);
            }
        };

        filter.textProperty().addListener((obs, old, text) -> applyFilter(text));
        filter.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                list.requestFocus();
                list.getSelectionModel().selectFirst();
            } else if (e.getCode() == KeyCode.ENTER) {
                if (list.getSelectionModel().getSelectedItem() == null) {
                    list.getSelectionModel().selectFirst();
                }
                openSelected.run();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) openSelected.run();
        });
        list.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) openSelected.run();
            else if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });

        VBox top = new VBox(8, header, filter);
        top.setPadding(new Insets(12, 12, 8, 12));

        BorderPane rootPane = new BorderPane(list, top, null, null, null);
        rootPane.getStyleClass().addAll("app-root", "goto-dialog");

        Scene scene = new Scene(rootPane, 560, 400);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);

        applyFilter("");
        stage.setOnShown(e -> filter.requestFocus());
    }

    public void show() {
        stage.showAndWait();
    }

    private void applyFilter(String text) {
        String q = text == null ? "" : text.toLowerCase().trim();
        visible.setAll(allFiles.stream()
                .filter(p -> q.isEmpty()
                        || p.getFileName().toString().toLowerCase().contains(q))
                .limit(200)
                .toList());
    }

    private static List<Path> scan(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        for (Path part : root.relativize(p)) {
                            if (SKIP_DIRS.contains(part.toString())) return false;
                        }
                        return true;
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
