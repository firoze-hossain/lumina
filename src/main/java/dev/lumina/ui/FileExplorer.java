package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ContentDisplay;
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
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * IntelliJ-style project explorer: lazy file tree, flattened package chains
 * under src/&#42;/java (e.g. "dev.lumina"), dimmed project path on the root.
 */
public class FileExplorer extends BorderPane {

    private final TreeView<Path> tree = new TreeView<>();
    private final Consumer<Path> onOpenFile;
    private final StackPane emptyState;
    private Path rootPath;

    public FileExplorer(Consumer<Path> onOpenFile,
                        java.util.function.Supplier<Path> openedFile) {
        this.onOpenFile = onOpenFile;
        getStyleClass().add("file-explorer");

        Label headerLabel = new Label("PROJECT");
        headerLabel.getStyleClass().add("panel-header");

        javafx.scene.control.Button locate = new javafx.scene.control.Button("\u25CE");
        locate.getStyleClass().add("console-button");
        locate.setTooltip(new javafx.scene.control.Tooltip("Select Opened File"));
        locate.setOnAction(e -> {
            Path current = openedFile.get();
            if (current != null) selectFile(current);
        });

        javafx.scene.layout.Region headerSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(headerSpacer,
                Priority.ALWAYS);
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(
                8, headerLabel, headerSpacer, locate);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 10, 6, 12));
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
        if (root == null) {
            tree.setRoot(null);
            setCenter(emptyState);
            return;
        }
        TreeItem<Path> rootItem = new LazyPathItem(root, null);
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

    /** Expand the tree down to a file and select it (like IntelliJ \u25CE). */
    public void selectFile(Path target) {
        if (tree.getRoot() == null || target == null) return;
        Path t = target.toAbsolutePath().normalize();
        TreeItem<Path> current = tree.getRoot();
        Path rootValue = current.getValue().toAbsolutePath().normalize();
        if (!t.startsWith(rootValue)) return;

        boolean progressed = true;
        while (progressed && !current.getValue()
                .toAbsolutePath().normalize().equals(t)) {
            progressed = false;
            current.setExpanded(true);
            for (TreeItem<Path> child : current.getChildren()) {
                Path cv = child.getValue().toAbsolutePath().normalize();
                if (t.equals(cv) || t.startsWith(cv)) {
                    current = child;
                    progressed = true;
                    break;
                }
            }
        }
        current.setExpanded(true);
        tree.getSelectionModel().select(current);
        int row = tree.getRow(current);
        if (row >= 0) tree.scrollTo(Math.max(0, row - 5));
    }

    // ------------------------------------------------------------ tree cell

    private class PathCell extends TreeCell<Path> {
        @Override
        protected void updateItem(Path item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(null);
            if (empty || item == null) {
                setText(null);
                return;
            }
            LazyPathItem node = (LazyPathItem) getTreeItem();
            boolean isRoot = node != null && node.getParent() == null;

            String display = node != null && node.displayName != null
                    ? node.displayName
                    : (item.getFileName() != null ? item.getFileName().toString()
                                                  : item.toString());
            setText(glyphFor(item, node) + "  " + display);

            if (isRoot) {
                Label pathLabel = new Label(abbreviate(item));
                pathLabel.getStyleClass().add("tree-root-path");
                setGraphic(pathLabel);
                setContentDisplay(ContentDisplay.RIGHT);
            }
        }

        private String glyphFor(Path p, LazyPathItem node) {
            if (Files.isDirectory(p)) {
                if (node != null && node.isPackage) return "\uD83D\uDDC2\uFE0F"; // card index
                return "\uD83D\uDCC1";                                            // folder
            }
            String n = p.getFileName().toString().toLowerCase();
            if (n.endsWith(".java")) return "\u2615";
            if (n.endsWith(".class")) return "\u2699\uFE0F";
            if (n.endsWith(".xml") || n.endsWith(".pom")) return "\uD83E\uDDFE";
            if (n.endsWith(".md") || n.endsWith(".txt")) return "\uD83D\uDCC4";
            if (n.endsWith(".properties") || n.endsWith(".yml")
                    || n.endsWith(".yaml")) return "\u2699\uFE0F";
            if (n.startsWith(".git")) return "\uD83D\uDD00";
            return "\uD83D\uDCC4";
        }
    }

    private static String abbreviate(Path p) {
        String home = System.getProperty("user.home");
        String s = p.toAbsolutePath().toString();
        return s.startsWith(home) ? "~" + s.substring(home.length()) : s;
    }

    // ------------------------------------------------------- lazy tree item

    /**
     * Loads children on first expansion. Chains of single-child directories
     * under a java source root are flattened into one "a.b.c" package node.
     */
    private static class LazyPathItem extends TreeItem<Path> {
        final String displayName;   // null -> use file name
        final boolean isPackage;
        private boolean loaded;

        LazyPathItem(Path path, String displayName) {
            this(path, displayName, false);
        }

        LazyPathItem(Path path, String displayName, boolean isPackage) {
            super(path);
            this.displayName = displayName;
            this.isPackage = isPackage;
        }

        @Override
        public boolean isLeaf() {
            return !Files.isDirectory(getValue());
        }

        @Override
        public javafx.collections.ObservableList<TreeItem<Path>> getChildren() {
            if (!loaded && Files.isDirectory(getValue())) {
                loaded = true;
                for (Path p : listSorted(getValue())) {
                    super.getChildren().add(createChild(p));
                }
            }
            return super.getChildren();
        }

        private static LazyPathItem createChild(Path p) {
            if (Files.isDirectory(p) && underJavaRoot(p)) {
                // flatten single-child directory chains into a dotted package
                StringBuilder name = new StringBuilder(p.getFileName().toString());
                Path end = p;
                while (true) {
                    List<Path> entries = listSorted(end);
                    if (entries.size() == 1 && Files.isDirectory(entries.get(0))) {
                        end = entries.get(0);
                        name.append('.').append(end.getFileName());
                    } else {
                        break;
                    }
                }
                return new LazyPathItem(end, name.toString(), true);
            }
            return new LazyPathItem(p, null, false);
        }

        private static boolean underJavaRoot(Path p) {
            String s = p.toAbsolutePath().toString().replace('\\', '/');
            return s.contains("/src/main/java/") || s.contains("/src/test/java/");
        }

        private static List<Path> listSorted(Path dir) {
            try (Stream<Path> entries = Files.list(dir)) {
                return entries
                        .filter(p -> !p.getFileName().toString().equals(".git"))
                        .filter(p -> !p.getFileName().toString().equals(".DS_Store"))
                        .sorted(Comparator
                                .comparing((Path p) -> !Files.isDirectory(p))
                                .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                        .toList();
            } catch (IOException e) {
                return List.of();
            }
        }
    }
}
