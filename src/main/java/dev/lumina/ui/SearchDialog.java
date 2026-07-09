package dev.lumina.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/** IntelliJ-style "Find in Files": search project text, jump to file:line. */
public class SearchDialog {

    public record Hit(Path file, int line, String preview) {
    }

    private static final Set<String> SKIP_DIRS =
            Set.of(".git", "target", "build", "node_modules", ".gradle", ".idea", "out");
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "java", "xml", "pom", "md", "txt", "properties", "yml", "yaml",
            "json", "gradle", "kts", "html", "css", "js", "sql", "sh", "cmd",
            "bat", "gitignore", "fxml");
    private static final int MAX_RESULTS = 500;

    private final Stage stage = new Stage();
    private final ObservableList<Hit> hits = FXCollections.observableArrayList();
    private final Path root;
    private Thread searchThread;

    public SearchDialog(Stage owner, Path root, BiConsumer<Path, Integer> onOpen) {
        this.root = root;

        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.initStyle(StageStyle.UNDECORATED);

        Label header = new Label("Find in Files");
        header.getStyleClass().add("panel-header");

        TextField query = new TextField();
        query.setPromptText("Search text\u2026");
        query.getStyleClass().add("goto-filter");
        CheckBox matchCase = new CheckBox("Match case");

        Label count = new Label("");
        count.getStyleClass().add("side-subtle");

        ListView<Hit> list = new ListView<>(hits);
        list.getStyleClass().add("goto-list");
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Hit item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(root.relativize(item.file()) + ":" + item.line()
                        + "   " + item.preview());
            }
        });

        Runnable openSelected = () -> {
            Hit sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) {
                stage.close();
                onOpen.accept(sel.file(), sel.line());
            }
        };

        Runnable triggerSearch = () ->
                search(query.getText(), matchCase.isSelected(), count);

        query.textProperty().addListener((obs, old, text) -> triggerSearch.run());
        matchCase.selectedProperty().addListener((obs, old, v) -> triggerSearch.run());
        query.setOnKeyPressed(e -> {
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

        HBox options = new HBox(12, matchCase, spacer(), count);
        VBox top = new VBox(8, header, query, options);
        top.setPadding(new Insets(12, 12, 8, 12));

        BorderPane rootPane = new BorderPane(list, top, null, null, null);
        rootPane.getStyleClass().addAll("app-root", "goto-dialog");

        Scene scene = new Scene(rootPane, 680, 460);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);

        // IntelliJ popup behavior: Esc anywhere or clicking outside closes it.
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                ev.consume();
                stage.close();
            }
        });
        stage.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) stage.close();
        });
        stage.setOnShown(e -> query.requestFocus());
    }

    public void show() {
        stage.showAndWait();
    }

    // ---------------------------------------------------------------- search

    private void search(String rawQuery, boolean matchCase, Label count) {
        if (searchThread != null) searchThread.interrupt();
        String q = rawQuery == null ? "" : rawQuery;
        if (q.length() < 2) {
            hits.clear();
            count.setText("");
            return;
        }
        String needle = matchCase ? q : q.toLowerCase();

        searchThread = new Thread(() -> {
            List<Hit> found = new ArrayList<>();
            try (Stream<Path> walk = Files.walk(root)) {
                var files = walk.filter(Files::isRegularFile)
                        .filter(SearchDialog::isSearchable)
                        .filter(p -> !isSkipped(p))
                        .toList();
                outer:
                for (Path file : files) {
                    if (Thread.currentThread().isInterrupted()) return;
                    List<String> lines;
                    try {
                        lines = Files.readAllLines(file);
                    } catch (IOException | java.io.UncheckedIOException e) {
                        continue; // binary or unreadable
                    }
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        String haystack = matchCase ? line : line.toLowerCase();
                        if (haystack.contains(needle)) {
                            found.add(new Hit(file, i + 1, trim(line)));
                            if (found.size() >= MAX_RESULTS) break outer;
                        }
                    }
                }
            } catch (IOException ignored) {
            }
            List<Hit> snapshot = List.copyOf(found);
            Platform.runLater(() -> {
                hits.setAll(snapshot);
                count.setText(snapshot.size()
                        + (snapshot.size() >= MAX_RESULTS ? "+ results" : " results"));
            });
        }, "lumina-search");
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private boolean isSkipped(Path p) {
        for (Path part : root.relativize(p)) {
            if (SKIP_DIRS.contains(part.toString())) return true;
        }
        return false;
    }

    private static boolean isSearchable(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
        return TEXT_EXTENSIONS.contains(ext) || name.equals("mvnw") || name.equals("gradlew");
    }

    private static String trim(String line) {
        String t = line.strip();
        return t.length() > 120 ? t.substring(0, 120) + "\u2026" : t;
    }

    private javafx.scene.layout.Region spacer() {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}
