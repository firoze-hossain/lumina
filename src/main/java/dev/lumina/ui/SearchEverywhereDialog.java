package dev.lumina.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * IntelliJ-style "Search Everywhere" (double-Shift): one popup with
 * All / Classes / Files / Symbols / Actions / Text categories.
 */
public class SearchEverywhereDialog {

    public record Action(String name, Runnable run) {
    }

    private record Item(String glyph, String label, String detail, Runnable onOpen) {
    }

    private record Symbol(String name, String kind, Path file, int line) {
    }

    private static final Set<String> SKIP_DIRS =
            Set.of(".git", "target", "build", "node_modules", ".gradle", ".idea", "out");
    private static final Pattern TYPE_DECL = Pattern.compile(
            "\\b(class|interface|enum|record)\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern METHOD_DECL = Pattern.compile(
            "(?:public|private|protected|static|final|synchronized|abstract|default)"
                    + "[\\w<>,\\[\\]\\s]*\\s([a-z][A-Za-z0-9_]*)\\s*\\(");

    private final Stage stage = new Stage();
    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private final Path root;
    private final List<Action> actions;
    private final Consumer<Path> onOpenFile;
    private final BiConsumer<Path, Integer> onOpenAt;

    private final List<Path> allFiles = new ArrayList<>();
    private final List<Symbol> symbols = new ArrayList<>();
    private volatile boolean symbolsReady;

    private final TextField query = new TextField();
    private final ToggleGroup tabGroup = new ToggleGroup();
    private Thread textSearchThread;

    public SearchEverywhereDialog(Stage owner, Path root, List<Action> actions,
                                  Consumer<Path> onOpenFile,
                                  BiConsumer<Path, Integer> onOpenAt) {
        this.root = root;
        this.actions = actions;
        this.onOpenFile = onOpenFile;
        this.onOpenAt = onOpenAt;

        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);

        // tabs row
        HBox tabs = new HBox(4);
        tabs.setAlignment(Pos.CENTER_LEFT);
        for (String name : new String[]{"All", "Classes", "Files", "Symbols", "Actions", "Text"}) {
            ToggleButton tb = new ToggleButton(name);
            tb.setToggleGroup(tabGroup);
            tb.getStyleClass().add("se-tab");
            tb.setUserData(name);
            if (name.equals("All")) tb.setSelected(true);
            tabs.getChildren().add(tb);
        }
        tabGroup.selectedToggleProperty().addListener((obs, old, t) -> {
            if (t == null && old != null) old.setSelected(true); // never zero
            else refresh();
        });

        query.setPromptText("Type / to see commands, or just search\u2026");
        query.getStyleClass().add("goto-filter");

        ListView<Item> list = new ListView<>(items);
        list.getStyleClass().add("goto-list");
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label name = new Label(item.glyph() + "  " + item.label());
                name.getStyleClass().add("se-name");
                Label detail = new Label(item.detail());
                detail.getStyleClass().add("se-detail");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox row = new HBox(10, name, spacer, detail);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });

        Runnable openSelected = () -> {
            Item sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) {
                stage.close();
                sel.onOpen().run();
            }
        };

        query.textProperty().addListener((obs, old, text) -> refresh());
        query.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN -> {
                    list.requestFocus();
                    list.getSelectionModel().selectFirst();
                }
                case ENTER -> {
                    if (list.getSelectionModel().getSelectedItem() == null) {
                        list.getSelectionModel().selectFirst();
                    }
                    openSelected.run();
                }
                case ESCAPE -> stage.close();
                case TAB -> {
                    e.consume();
                    cycleTab(e.isShiftDown() ? -1 : 1);
                }
                default -> { }
            }
        });
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) openSelected.run();
        });
        list.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) openSelected.run();
            else if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });

        VBox top = new VBox(10, tabs, query);
        top.setPadding(new Insets(12, 12, 8, 12));

        BorderPane rootPane = new BorderPane(list, top, null, null, null);
        rootPane.getStyleClass().addAll("app-root", "goto-dialog");

        Scene scene = new Scene(rootPane, 700, 460);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);
        stage.setOnShown(e -> query.requestFocus());

        indexInBackground();
    }

    public void show() {
        refresh();
        stage.showAndWait();
    }

    // -------------------------------------------------------------- indexing

    private void indexInBackground() {
        Thread t = new Thread(() -> {
            if (root != null) {
                try (Stream<Path> walk = Files.walk(root)) {
                    walk.filter(Files::isRegularFile)
                            .filter(p -> !isSkipped(p))
                            .forEach(allFiles::add);
                } catch (IOException ignored) {
                }
                for (Path file : allFiles) {
                    if (!file.getFileName().toString().endsWith(".java")) continue;
                    try {
                        List<String> lines = Files.readAllLines(file);
                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i);
                            Matcher tm = TYPE_DECL.matcher(line);
                            while (tm.find()) {
                                symbols.add(new Symbol(tm.group(2), tm.group(1), file, i + 1));
                            }
                            Matcher mm = METHOD_DECL.matcher(line);
                            while (mm.find()) {
                                symbols.add(new Symbol(mm.group(1), "method", file, i + 1));
                            }
                        }
                    } catch (IOException | java.io.UncheckedIOException ignored) {
                    }
                }
            }
            symbolsReady = true;
            Platform.runLater(this::refresh);
        }, "lumina-se-index");
        t.setDaemon(true);
        t.start();
    }

    private boolean isSkipped(Path p) {
        for (Path part : root.relativize(p)) {
            if (SKIP_DIRS.contains(part.toString())) return true;
        }
        return false;
    }

    // ------------------------------------------------------------- filtering

    private String currentTab() {
        return tabGroup.getSelectedToggle() == null ? "All"
                : String.valueOf(tabGroup.getSelectedToggle().getUserData());
    }

    private void cycleTab(int delta) {
        List<javafx.scene.control.Toggle> toggles = tabGroup.getToggles();
        int idx = toggles.indexOf(tabGroup.getSelectedToggle());
        int next = ((idx + delta) % toggles.size() + toggles.size()) % toggles.size();
        toggles.get(next).setSelected(true);
    }

    private void refresh() {
        String q = query.getText() == null ? "" : query.getText().trim();
        String tab = currentTab();
        List<Item> out = new ArrayList<>();

        int per = tab.equals("All") ? 6 : 100;
        if (tab.equals("All") || tab.equals("Actions")) {
            collectActions(q, out, tab.equals("Actions") ? 100 : 5);
        }
        if (tab.equals("All") || tab.equals("Classes")) {
            collectClasses(q, out, per);
        }
        if (tab.equals("All") || tab.equals("Files")) {
            collectFiles(q, out, per);
        }
        if (tab.equals("All") || tab.equals("Symbols")) {
            collectSymbols(q, out, per);
        }
        items.setAll(out);

        if (tab.equals("Text")) {
            searchText(q); // async, replaces items when done
        }
    }

    private void collectActions(String q, List<Item> out, int limit) {
        String needle = q.startsWith("/") ? q.substring(1) : q;
        actions.stream()
                .filter(a -> matches(a.name(), needle))
                .limit(limit)
                .forEach(a -> out.add(new Item("\u2318", a.name(), "action", a.run())));
    }

    private void collectClasses(String q, List<Item> out, int limit) {
        if (root == null || q.isEmpty()) return;
        allFiles.stream()
                .filter(p -> p.getFileName().toString().endsWith(".java"))
                .filter(p -> matches(stripExt(p), q))
                .limit(limit)
                .forEach(p -> out.add(new Item("\u24B8", stripExt(p),
                        rel(p), () -> onOpenFile.accept(p))));
    }

    private void collectFiles(String q, List<Item> out, int limit) {
        if (root == null || q.isEmpty()) return;
        allFiles.stream()
                .filter(p -> matches(p.getFileName().toString(), q))
                .limit(limit)
                .forEach(p -> out.add(new Item("\uD83D\uDCC4",
                        p.getFileName().toString(), rel(p),
                        () -> onOpenFile.accept(p))));
    }

    private void collectSymbols(String q, List<Item> out, int limit) {
        if (root == null || q.isEmpty()) return;
        if (!symbolsReady) {
            out.add(new Item("\u23F3", "Indexing symbols\u2026", "", () -> { }));
            return;
        }
        symbols.stream()
                .filter(s -> matches(s.name(), q))
                .limit(limit)
                .forEach(s -> out.add(new Item(
                        s.kind().equals("method") ? "\u24C2" : "\u24B8",
                        s.name() + (s.kind().equals("method") ? "()" : ""),
                        rel(s.file()) + ":" + s.line(),
                        () -> onOpenAt.accept(s.file(), s.line()))));
    }

    private void searchText(String q) {
        if (textSearchThread != null) textSearchThread.interrupt();
        if (root == null || q.length() < 2) {
            items.clear();
            return;
        }
        String needle = q.toLowerCase();
        textSearchThread = new Thread(() -> {
            List<Item> found = new ArrayList<>();
            outer:
            for (Path file : allFiles) {
                if (Thread.currentThread().isInterrupted()) return;
                List<String> lines;
                try {
                    lines = Files.readAllLines(file);
                } catch (IOException | java.io.UncheckedIOException e) {
                    continue;
                }
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).toLowerCase().contains(needle)) {
                        final int line = i + 1;
                        final Path f = file;
                        String preview = lines.get(i).strip();
                        if (preview.length() > 90) preview = preview.substring(0, 90) + "\u2026";
                        found.add(new Item("\uD83D\uDD24", preview,
                                rel(file) + ":" + line,
                                () -> onOpenAt.accept(f, line)));
                        if (found.size() >= 200) break outer;
                    }
                }
            }
            List<Item> snapshot = List.copyOf(found);
            Platform.runLater(() -> {
                if (currentTab().equals("Text")) items.setAll(snapshot);
            });
        }, "lumina-se-text");
        textSearchThread.setDaemon(true);
        textSearchThread.start();
    }

    // -------------------------------------------------------------- helpers

    private static boolean matches(String candidate, String query) {
        return query.isEmpty()
                || candidate.toLowerCase().contains(query.toLowerCase());
    }

    private String rel(Path p) {
        try {
            return root.relativize(p).toString();
        } catch (IllegalArgumentException e) {
            return p.toString();
        }
    }

    private static String stripExt(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }
}
