package dev.lumina.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * IntelliJ-style "Find Usages" popup: every word-boundary occurrence of an
 * identifier across the project, declarations starred, click to jump.
 */
public class UsagesDialog {

    private record Usage(Path file, int line, String preview, boolean declaration) {
    }

    private static final Set<String> SKIP_DIRS =
            Set.of(".git", "target", "build", "node_modules", ".gradle", ".idea", "out");

    private final Stage stage = new Stage();
    private final ObservableList<Usage> usages = FXCollections.observableArrayList();
    private final Path root;
    private final String word;
    private final Label countLabel = new Label("Searching\u2026");

    /** A precomputed usage row (from the semantic engine). */
    public record Hit(Path file, int line, String preview, boolean declaration) {
    }

    private final List<Hit> precomputed;

    public UsagesDialog(Stage owner, Path root, String word,
                        BiConsumer<Path, Integer> onOpenAt) {
        this(owner, root, word, null, onOpenAt);
    }

    /** Semantic variant: rows are already resolved, no text scan runs. */
    public UsagesDialog(Stage owner, Path root, String word, List<Hit> hits,
                        BiConsumer<Path, Integer> onOpenAt) {
        this.precomputed = hits;
        this.root = root;
        this.word = word;

        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.initStyle(StageStyle.UNDECORATED);

        Label header = new Label("Usages of '" + word + "'");
        header.getStyleClass().add("panel-header");
        countLabel.getStyleClass().add("side-subtle");

        ListView<Usage> list = new ListView<>(usages);
        list.getStyleClass().add("goto-list");
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Usage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText((item.declaration() ? "\u2605 " : "   ")
                        + root.relativize(item.file()) + ":" + item.line()
                        + "   " + item.preview());
            }
        });

        Runnable openSelected = () -> {
            Usage sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) {
                stage.close();
                onOpenAt.accept(sel.file(), sel.line());
            }
        };
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) openSelected.run();
        });
        list.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) openSelected.run();
        });

        VBox top = new VBox(4, header, countLabel);
        top.setPadding(new Insets(12, 12, 8, 12));

        BorderPane rootPane = new BorderPane(list, top, null, null, null);
        rootPane.getStyleClass().addAll("app-root", "goto-dialog");

        Scene scene = new Scene(rootPane, 640, 400);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                ev.consume();
                stage.close();
            }
        });
        stage.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) stage.close();
        });

        if (precomputed == null) {
            scanInBackground();
        } else {
            List<Usage> rows = precomputed.stream()
                    .map(h -> new Usage(h.file(), h.line(), h.preview(),
                            h.declaration()))
                    .toList();
            usages.setAll(rows);
            countLabel.setText(rows.size()
                    + " usages \u2014 resolved semantically, \u2605 declaration");
        }
    }

    public void show() {
        stage.show();
    }

    private void scanInBackground() {
        Pattern occurrence = Pattern.compile("\\b" + Pattern.quote(word) + "\\b");
        Pattern declaration = Character.isUpperCase(word.charAt(0))
                ? Pattern.compile("\\b(class|interface|enum|record)\\s+"
                        + Pattern.quote(word) + "\\b")
                : Pattern.compile("[\\w>\\]]\\s+" + Pattern.quote(word) + "\\s*\\(");

        Thread t = new Thread(() -> {
            List<Usage> found = new ArrayList<>();
            try (Stream<Path> walk = Files.walk(root)) {
                var files = walk.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".java"))
                        .filter(p -> !isSkipped(p))
                        .toList();
                outer:
                for (Path file : files) {
                    List<String> lines;
                    try {
                        lines = Files.readAllLines(file);
                    } catch (IOException | java.io.UncheckedIOException e) {
                        continue;
                    }
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        if (!occurrence.matcher(line).find()) continue;
                        boolean isDecl = declaration.matcher(line).find()
                                && !line.strip().endsWith(";");
                        String preview = line.strip();
                        if (preview.length() > 90) {
                            preview = preview.substring(0, 90) + "\u2026";
                        }
                        found.add(new Usage(file, i + 1, preview, isDecl));
                        if (found.size() >= 300) break outer;
                    }
                }
            } catch (IOException ignored) {
            }
            found.sort((a, b) -> Boolean.compare(b.declaration(), a.declaration()));
            List<Usage> snapshot = List.copyOf(found);
            Platform.runLater(() -> {
                usages.setAll(snapshot);
                countLabel.setText(snapshot.size()
                        + (snapshot.size() >= 300 ? "+ usages" : " usages")
                        + " \u2014 \u2605 marks the declaration");
            });
        }, "lumina-usages");
        t.setDaemon(true);
        t.start();
    }

    private boolean isSkipped(Path p) {
        for (Path part : root.relativize(p)) {
            if (SKIP_DIRS.contains(part.toString())) return true;
        }
        return false;
    }
}