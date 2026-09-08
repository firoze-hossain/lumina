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
    private java.util.function.Consumer<Path> onRun;
    private java.util.function.Consumer<Path> onRunTest;
    private java.util.function.Consumer<Path> onDelete;
    private java.util.function.Consumer<Path> onRename;
    private java.util.function.Consumer<Path> onNewJavaClass;
    private java.util.function.Consumer<Path> onNewPackage;
    private java.util.function.Consumer<Path> onNewFile;
    private java.util.function.Consumer<Path> onNewDirectory;
    private java.util.function.Consumer<Path> onCopyPath;
    private java.util.function.Consumer<Path> onOpenModuleSettings;
    /** Menu label of the item clicked, for scaffolding not wired up yet. */
    private java.util.function.Consumer<String> onPlaceholder;

    /** Wire run/test/delete actions used by the tree's right-click menu. */
    public void setActions(java.util.function.Consumer<Path> run,
                           java.util.function.Consumer<Path> runTest,
                           java.util.function.Consumer<Path> delete) {
        this.onRun = run;
        this.onRunTest = runTest;
        this.onDelete = delete;
    }

    /**
     * Wires the rest of the IntelliJ-style project-tree menu: New Class/
     * Package/File/Directory, Rename, Copy Path, Open Module Settings, and
     * a catch-all for every scaffolded item that has no behavior yet.
     */
    public void setExtendedActions(java.util.function.Consumer<Path> rename,
                                   java.util.function.Consumer<Path> newJavaClass,
                                   java.util.function.Consumer<Path> newPackage,
                                   java.util.function.Consumer<Path> newFile,
                                   java.util.function.Consumer<Path> newDirectory,
                                   java.util.function.Consumer<Path> copyPath,
                                   java.util.function.Consumer<Path> openModuleSettings,
                                   java.util.function.Consumer<String> placeholder) {
        this.onRename = rename;
        this.onNewJavaClass = newJavaClass;
        this.onNewPackage = newPackage;
        this.onNewFile = newFile;
        this.onNewDirectory = newDirectory;
        this.onCopyPath = copyPath;
        this.onOpenModuleSettings = openModuleSettings;
        this.onPlaceholder = placeholder;
    }

    /**
     * The tree menu is rebuilt fresh every time it opens, because a file, a
     * regular directory, and the project root (module) each get genuinely
     * different IntelliJ menus \u2014 not the same items with some hidden.
     */
    private javafx.scene.control.ContextMenu buildTreeContextMenu() {
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();

        // Populated proactively on selection change (not lazily inside
        // setOnShowing) — a ContextMenu created with zero items and only
        // filled in on show is a known-fragile JavaFX pattern; building the
        // items ahead of time guarantees they're already there by the time
        // any right-click can possibly trigger the popup.
        java.util.function.Consumer<Path> rebuild = p -> {
            List<javafx.scene.control.MenuItem> items;
            try {
                if (p == null) {
                    items = List.of();
                } else if (Files.isDirectory(p)) {
                    items = directoryMenuItems(p);
                } else {
                    items = fileMenuItems(p);
                }
            } catch (Exception ex) {
                items = List.of(disabledItem("(menu error: " + ex + ")"));
            }
            menu.getItems().setAll(items);
        };

        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) ->
                rebuild.accept(sel != null ? sel.getValue() : null));
        // Safety net in case a right-click ever reaches here before the
        // selection listener above has run.
        menu.setOnShowing(e -> {
            TreeItem<Path> sel = tree.getSelectionModel().getSelectedItem();
            rebuild.accept(sel != null ? sel.getValue() : null);
        });
        return menu;
    }

    private javafx.scene.control.MenuItem disabledItem(String label) {
        javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(label);
        item.setDisable(true);
        return item;
    }

    // ---------------------------------------------------------- file node

    private List<javafx.scene.control.MenuItem> fileMenuItems(Path p) {
        boolean isJava = p.getFileName().toString().endsWith(".java");
        boolean isTest = p.toString().replace('\\', '/').contains("/src/test/java/");
        List<javafx.scene.control.MenuItem> items = new java.util.ArrayList<>();
        items.add(action("Open", () -> onOpenFile.accept(p)));
        if (isJava && !isTest) items.add(action("\u25B6  Run", () -> run(onRun, p)));
        if (isJava && isTest) items.add(action("\u2705  Run Test", () -> run(onRunTest, p)));
        items.add(new javafx.scene.control.SeparatorMenuItem());
        items.add(action("Cut", () -> ph("Cut")));
        items.add(action("Copy", () -> ph("Copy")));
        items.add(action("Copy Path/Reference\u2026", () -> run(onCopyPath, p)));
        items.add(new javafx.scene.control.SeparatorMenuItem());
        items.add(action("Rename\u2026", () -> run(onRename, p)));
        items.add(placeholderMenu("Refactor"));
        items.add(action("Delete\u2026", () -> run(onDelete, p)));
        items.add(new javafx.scene.control.SeparatorMenuItem());
        items.add(action("Local History\u2026", () -> ph("Local History")));
        items.add(action("Compare With\u2026", () -> ph("Compare With")));
        return items;
    }

    // ------------------------------------------------- directory / root node

    private List<javafx.scene.control.MenuItem> directoryMenuItems(Path p) {
        boolean isRoot = p.equals(rootPath);
        List<javafx.scene.control.MenuItem> items = new java.util.ArrayList<>();

        items.add(newMenu(p, isRoot));
        items.add(new javafx.scene.control.SeparatorMenuItem());
        items.add(action("Cut", () -> ph("Cut")));
        items.add(action("Copy", () -> ph("Copy")));
        items.add(action("Copy Path/Reference\u2026", () -> run(onCopyPath, p)));
        items.add(action("Paste", () -> ph("Paste")));
        items.add(action("Paste from History\u2026", () -> ph("Paste from History")));
        items.add(new javafx.scene.control.SeparatorMenuItem());
        items.add(action("Find Usages", () -> ph("Find Usages")));
        items.add(action("Find in Files\u2026", () -> ph("Find in Files")));
        items.add(action("Replace in Files\u2026", () -> ph("Replace in Files")));
        items.add(placeholderMenu("Analyze"));
        items.add(new javafx.scene.control.SeparatorMenuItem());
        items.add(action("Rename\u2026", () -> ph("Rename")));
        items.add(placeholderMenu("Refactor"));
        items.add(new javafx.scene.control.SeparatorMenuItem());
        items.add(action("Bookmarks", () -> ph("Bookmarks")));
        items.add(new javafx.scene.control.SeparatorMenuItem());
        items.add(action("Reformat Code", () -> ph("Reformat Code")));
        items.add(action("Optimize Imports", () -> ph("Optimize Imports")));

        if (isRoot) {
            items.add(action("Remove Module", () -> ph("Remove Module")));
        } else {
            items.add(action("Delete\u2026", () -> run(onDelete, p)));
        }
        items.add(new javafx.scene.control.SeparatorMenuItem());

        if (isRoot) {
            items.add(action("Build Module '" + p.getFileName() + "'",
                    () -> ph("Build Module")));
            items.add(action("Rebuild Module '" + p.getFileName() + "'",
                    () -> ph("Rebuild Module")));
            items.add(new javafx.scene.control.SeparatorMenuItem());
        }

        items.add(placeholderMenu("Open In"));
        items.add(placeholderMenu("Local History"));
        items.add(placeholderMenu("Git"));
        items.add(action("Repair IDE on File", () -> ph("Repair IDE on File")));
        items.add(action("Reload from Disk", this::refresh));
        items.add(new javafx.scene.control.SeparatorMenuItem());
        items.add(action("Compare With\u2026", () -> ph("Compare With")));

        if (isRoot) {
            items.add(new javafx.scene.control.SeparatorMenuItem());
            items.add(action("Open Module Settings", () -> run(onOpenModuleSettings, p)));
        }
        items.add(placeholderMenu("Mark Directory As"));

        if (isRoot) {
            items.add(action("Analyze Dependencies\u2026", () -> ph("Analyze Dependencies")));
        }
        items.add(placeholderMenu("Diagrams"));
        items.add(action("Create Gist\u2026", () -> ph("Create Gist")));

        if (isRoot) {
            items.add(placeholderMenu("Maven"));
            items.add(placeholderMenu("GitHub Copilot"));
            items.add(action("Upgrade Java Runtime and Frameworks",
                    () -> ph("Upgrade Java Runtime and Frameworks")));
        }
        return items;
    }

    /** The "New" submenu: Module (root only), then every file/resource type
     *  IntelliJ offers. Java Class/Package/File/Directory are wired to
     *  Lumina's real creation flow; the rest are scaffolded for later. */
    private javafx.scene.control.Menu newMenu(Path dir, boolean isRoot) {
        javafx.scene.control.Menu menu = new javafx.scene.control.Menu("New");
        if (isRoot) {
            menu.getItems().add(action("Module\u2026", () -> ph("Module")));
            menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
        }
        menu.getItems().addAll(
                action("Java Class", () -> run(onNewJavaClass, dir)),
                action("Package", () -> run(onNewPackage, dir)),
                action("Directory", () -> run(onNewDirectory, dir)),
                action("File", () -> run(onNewFile, dir)),
                action("Scratch File", () -> ph("Scratch File")),
                new javafx.scene.control.SeparatorMenuItem(),
                action("Kotlin Script", () -> ph("Kotlin Script")),
                action("Kotlin Notebook", () -> ph("Kotlin Notebook")),
                action("JavaScript File", () -> ph("JavaScript File")),
                action("TypeScript File", () -> ph("TypeScript File")),
                action("HTML File", () -> ph("HTML File")),
                action("Stylesheet", () -> ph("Stylesheet")),
                action("Dockerfile", () -> ph("Dockerfile")),
                action("Dev Container Config\u2026", () -> ph("Dev Container Config")),
                action("HTTP Request", () -> ph("HTTP Request")),
                action("OpenAPI Specification", () -> ph("OpenAPI Specification")),
                action("Kubernetes Resource", () -> ph("Kubernetes Resource")),
                action("Helm Chart", () -> ph("Helm Chart")),
                action("Resource Bundle", () -> ph("Resource Bundle")),
                action("EditorConfig File", () -> ph("EditorConfig File")),
                action("Data Source in Path", () -> ph("Data Source in Path")));
        return menu;
    }

    private javafx.scene.control.Menu placeholderMenu(String label) {
        javafx.scene.control.Menu menu = new javafx.scene.control.Menu(label);
        javafx.scene.control.MenuItem soon = new javafx.scene.control.MenuItem("(coming soon)");
        soon.setDisable(true);
        menu.getItems().add(soon);
        return menu;
    }

    private javafx.scene.control.MenuItem action(String label, Runnable action) {
        javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(label);
        item.setOnAction(e -> action.run());
        return item;
    }

    private void run(java.util.function.Consumer<Path> handler, Path p) {
        if (handler != null) handler.accept(p);
    }

    private void ph(String feature) {
        if (onPlaceholder != null) onPlaceholder.accept(feature);
    }

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
        tree.setContextMenu(buildTreeContextMenu());

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