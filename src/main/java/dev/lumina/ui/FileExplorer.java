package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Left-hand project explorer: a lazy file tree; double-click opens a file. */
public class FileExplorer extends BorderPane {

    private final TreeView<Path> tree = new TreeView<>();
    private final Consumer<Path> onOpenFile;
    private final StackPane emptyState;
    private Path rootPath;

    public FileExplorer(Consumer<Path> onOpenFile) {
        this.onOpenFile = onOpenFile;
        getStyleClass().add("file-explorer");

        Label header = new Label("PROJECT");
        header.getStyleClass().add("panel-header");
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setMaxWidth(Double.MAX_VALUE);

        tree.getStyleClass().add("project-tree");
        tree.setShowRoot(true);
        tree.setCellFactory(tv -> new PathCell());
        tree.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<Path> item = tree.getSelectionModel().getSelectedItem();
                if (item != null && Files.isRegularFile(item.getValue())) {
                    onOpenFile.accept(item.getValue());
                }
            }
        });

        Label empty = new Label("No folder open\nFile \u2192 Open Folder\u2026");
        empty.getStyleClass().add("explorer-empty");
        emptyState = new StackPane(empty);
        emptyState.getStyleClass().add("file-explorer");

        VBox.setVgrow(tree, Priority.ALWAYS);
        setTop(header);
        setCenter(emptyState);
        setMinWidth(180);
    }

    public void setRoot(Path root) {
        this.rootPath = root;
        TreeItem<Path> rootItem = new LazyPathItem(root);
        rootItem.setExpanded(true);
        tree.setRoot(rootItem);
        setCenter(tree);
    }

    /** Re-scan the currently opened folder (e.g. after saving a new file). */
    public void refresh() {
        if (rootPath != null) setRoot(rootPath);
    }

    /** The path selected in the tree, or null when nothing is selected. */
    public Path getSelectedPath() {
        TreeItem<Path> item = tree.getSelectionModel().getSelectedItem();
        return item != null ? item.getValue() : null;
    }

    public Path getRootPath() {
        return rootPath;
    }

    // ------------------------------------------------------------ tree cell

    private static class PathCell extends TreeCell<Path> {
        @Override
        protected void updateItem(Path item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            String name = item.getFileName() != null
                    ? item.getFileName().toString()
                    : item.toString();
            setText(glyphFor(item) + "  " + name);
        }

        private String glyphFor(Path p) {
            if (Files.isDirectory(p)) return "\uD83D\uDCC1";           // folder
            String n = p.getFileName().toString().toLowerCase();
            if (n.endsWith(".java")) return "\u2615";                   // hot beverage
            if (n.endsWith(".xml") || n.endsWith(".pom")) return "\uD83D\uDCE6"; // package
            if (n.endsWith(".md") || n.endsWith(".txt")) return "\uD83D\uDCC4";  // page
            if (n.endsWith(".properties") || n.endsWith(".yml")
                    || n.endsWith(".yaml")) return "\u2699\uFE0F";      // gear
            return "\uD83D\uDCC4";
        }
    }

    // ------------------------------------------------------- lazy tree item

    /** Loads children only when a directory is first expanded. */
    private static class LazyPathItem extends TreeItem<Path> {
        private boolean loaded;

        LazyPathItem(Path path) {
            super(path);
        }

        @Override
        public boolean isLeaf() {
            return !Files.isDirectory(getValue());
        }

        @Override
        public javafx.collections.ObservableList<TreeItem<Path>> getChildren() {
            if (!loaded && Files.isDirectory(getValue())) {
                loaded = true;
                try (Stream<Path> entries = Files.list(getValue())) {
                    entries.filter(p -> !p.getFileName().toString().startsWith("."))
                            .sorted(Comparator
                                    .comparing((Path p) -> !Files.isDirectory(p))
                                    .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                            .forEach(p -> super.getChildren().add(new LazyPathItem(p)));
                } catch (IOException ignored) {
                    // Unreadable directory: show as empty.
                }
            }
            return super.getChildren();
        }
    }
}
