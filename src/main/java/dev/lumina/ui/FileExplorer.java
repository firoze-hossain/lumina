package dev.lumina.ui;

import dev.lumina.project.ExternalLibrariesService;
import dev.lumina.project.ProjectTreeNode;
import dev.lumina.project.ScratchesAndConsolesService;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Universal project explorer supporting:
 * 1. The Project Root folder (lazy file tree, flattened package chains, dimmed project/module path)
 * 2. External Libraries (dynamically discovered SDK/JDK modules, packages, and classes, plus Maven dependency JARs)
 * 3. Scratches and Consoles (dynamic Extensions tree like Database Tools and SQL, plus user scratch files)
 */
public class FileExplorer extends BorderPane {

    private final TreeView<ProjectTreeNode> tree = new TreeView<>();
    private final Consumer<Path> onOpenFile;
    private final StackPane emptyState;
    private Path rootPath;
    private Consumer<Path> onRun;
    private Consumer<Path> onRunTest;
    private Consumer<Path> onDelete;
    private Consumer<Path> onRename;
    private Consumer<Path> onNewJavaClass;
    private Consumer<Path> onNewPackage;
    private Consumer<Path> onNewFile;
    private Consumer<Path> onNewDirectory;
    private Consumer<Path> onCopyPath;
    private Consumer<Path> onOpenModuleSettings;
    /** Menu label of the item clicked, for scaffolding not wired up yet. */
    private Consumer<String> onPlaceholder;
    private NewMenuBuilder.CreationHandlers creationHandlers;

    public void setCreationHandlers(NewMenuBuilder.CreationHandlers handlers) {
        this.creationHandlers = handlers;
    }

    /** Wire run/test/delete actions used by the tree's right-click menu. */
    public void setActions(Consumer<Path> run,
                           Consumer<Path> runTest,
                           Consumer<Path> delete) {
        this.onRun = run;
        this.onRunTest = runTest;
        this.onDelete = delete;
    }

    /**
     * Wires the project-tree menu: New Class/Package/File/Directory, Rename,
     * Copy Path, Open Module Settings, and placeholder actions.
     */
    public void setExtendedActions(Consumer<Path> rename,
                                   Consumer<Path> newJavaClass,
                                   Consumer<Path> newPackage,
                                   Consumer<Path> newFile,
                                   Consumer<Path> newDirectory,
                                   Consumer<Path> copyPath,
                                   Consumer<Path> openModuleSettings,
                                   Consumer<String> placeholder) {
        this.onRename = rename;
        this.onNewJavaClass = newJavaClass;
        this.onNewPackage = newPackage;
        this.onNewFile = newFile;
        this.onNewDirectory = newDirectory;
        this.onCopyPath = copyPath;
        this.onOpenModuleSettings = openModuleSettings;
        this.onPlaceholder = placeholder;
    }

    private enum NodeKind { ROOT, SOURCE_ROOT, PACKAGE, DIRECTORY }

    private ContextMenu buildTreeContextMenu() {
        ContextMenu menu = new ContextMenu();

        Consumer<TreeItem<ProjectTreeNode>> rebuild = item -> {
            ProjectTreeNode node = item != null ? item.getValue() : null;
            Path p = node != null ? node.getPath() : null;
            List<MenuItem> items = new ArrayList<>();
            try {
                if (node == null) {
                    items = List.of();
                } else if (node.getKind() == ProjectTreeNode.NodeKind.EXTERNAL_LIBRARIES_ROOT) {
                    items.add(action("Reload from Disk", this::refresh));
                } else if (node.getKind() == ProjectTreeNode.NodeKind.SCRATCHES_ROOT
                        || node.getKind() == ProjectTreeNode.NodeKind.EXTENSIONS_ROOT) {
                    if (creationHandlers != null) {
                        items.add(action("New Scratch File\u2026", () -> creationHandlers.onNewScratchFile(p)));
                    }
                    items.add(action("Reload from Disk", this::refresh));
                } else if (p != null && Files.isDirectory(p)) {
                    items = directoryMenuItems(p, kindOf(p, node));
                } else if (p != null) {
                    items = fileMenuItems(p);
                } else {
                    items.add(action("Reload from Disk", this::refresh));
                }
            } catch (Exception ex) {
                items = List.of(disabledItem("(menu error: " + ex + ")"));
            }
            menu.getItems().setAll(items);
        };

        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> rebuild.accept(sel));
        menu.setOnShowing(e -> rebuild.accept(tree.getSelectionModel().getSelectedItem()));
        return menu;
    }

    private NodeKind kindOf(Path p, ProjectTreeNode node) {
        if (p.equals(rootPath) || (node != null && node.getKind() == ProjectTreeNode.NodeKind.PROJECT_ROOT)) return NodeKind.ROOT;
        if (node != null && node.getKind() == ProjectTreeNode.NodeKind.PACKAGE) return NodeKind.PACKAGE;
        if (isSourceRoot(p)) return NodeKind.SOURCE_ROOT;
        return NodeKind.DIRECTORY;
    }

    private static boolean isSourceRoot(Path p) {
        String s = p.toString().replace('\\', '/');
        for (String lang : new String[]{"java", "kotlin", "groovy"}) {
            if (s.endsWith("/src/main/" + lang) || s.endsWith("/src/test/" + lang)) return true;
        }
        return false;
    }

    private static boolean isMainSourceRoot(Path p) {
        String s = p.toString().replace('\\', '/');
        for (String lang : new String[]{"java", "kotlin", "groovy"}) {
            if (s.endsWith("/src/main/" + lang)) return true;
        }
        return false;
    }

    private static boolean isTestSourceRoot(Path p) {
        String s = p.toString().replace('\\', '/');
        for (String lang : new String[]{"java", "kotlin", "groovy"}) {
            if (s.endsWith("/src/test/" + lang)) return true;
        }
        return false;
    }

    private static boolean isResourceRoot(Path p) {
        String s = p.toString().replace('\\', '/');
        return s.endsWith("/src/main/resources") || s.endsWith("/src/test/resources");
    }

    private MenuItem disabledItem(String label) {
        MenuItem item = new MenuItem(label);
        item.setDisable(true);
        return item;
    }

    private List<MenuItem> fileMenuItems(Path p) {
        boolean isJava = p.getFileName().toString().endsWith(".java");
        boolean isTest = p.toString().replace('\\', '/').contains("/src/test/java/");
        List<MenuItem> items = new ArrayList<>();
        items.add(newMenu(p.getParent(), NodeKind.PACKAGE));
        items.add(new SeparatorMenuItem());
        items.add(action("Open", () -> onOpenFile.accept(p)));
        if (isJava && !isTest) items.add(action("\u25B6  Run", () -> run(onRun, p)));
        if (isJava && isTest) items.add(action("\u2705  Run Test", () -> run(onRunTest, p)));
        items.add(new SeparatorMenuItem());
        items.add(action("Cut", () -> ph("Cut")));
        items.add(action("Copy", () -> ph("Copy")));
        items.add(action("Copy Path/Reference\u2026", () -> run(onCopyPath, p)));
        items.add(new SeparatorMenuItem());
        items.add(action("Rename\u2026", () -> run(onRename, p)));
        items.add(placeholderMenu("Refactor"));
        items.add(action("Delete\u2026", () -> run(onDelete, p)));
        items.add(new SeparatorMenuItem());
        items.add(action("Local History\u2026", () -> ph("Local History")));
        items.add(action("Compare With\u2026", () -> ph("Compare With")));
        return items;
    }

    private List<MenuItem> directoryMenuItems(Path p, NodeKind kind) {
        boolean isRoot = kind == NodeKind.ROOT;
        boolean isSourceish = kind == NodeKind.SOURCE_ROOT || kind == NodeKind.PACKAGE;
        List<MenuItem> items = new ArrayList<>();

        items.add(newMenu(p, kind));
        items.add(new SeparatorMenuItem());
        items.add(action("Cut", () -> ph("Cut")));
        items.add(action("Copy", () -> ph("Copy")));
        items.add(action("Copy Path/Reference\u2026", () -> run(onCopyPath, p)));
        items.add(action("Paste", () -> ph("Paste")));
        items.add(action("Paste from History\u2026", () -> ph("Paste from History")));
        items.add(new SeparatorMenuItem());
        items.add(action("Find Usages", () -> ph("Find Usages")));
        items.add(action("Find in Files\u2026", () -> ph("Find in Files")));
        items.add(action("Replace in Files\u2026", () -> ph("Replace in Files")));
        items.add(placeholderMenu("Analyze"));
        items.add(new SeparatorMenuItem());
        items.add(action("Rename\u2026", () -> ph("Rename")));
        items.add(placeholderMenu("Refactor"));
        items.add(new SeparatorMenuItem());
        items.add(action("Bookmarks", () -> ph("Bookmarks")));
        items.add(new SeparatorMenuItem());
        items.add(action("Reformat Code", () -> ph("Reformat Code")));
        items.add(action("Optimize Imports", () -> ph("Optimize Imports")));

        if (isRoot) {
            items.add(action("Remove Module", () -> ph("Remove Module")));
        } else {
            items.add(action("Delete\u2026", () -> run(onDelete, p)));
        }
        items.add(new SeparatorMenuItem());

        if (isRoot) {
            items.add(action("Build Module '" + p.getFileName() + "'", () -> ph("Build Module")));
            items.add(action("Rebuild Module '" + p.getFileName() + "'", () -> ph("Rebuild Module")));
            items.add(new SeparatorMenuItem());
        } else if (isSourceish) {
            String label = kind == NodeKind.PACKAGE ? p.getFileName().toString() : "<default>";
            items.add(action("Rebuild '" + label + "'", () -> ph("Rebuild")));
            items.add(new SeparatorMenuItem());
        }

        items.add(placeholderMenu("Open In"));
        items.add(placeholderMenu("Local History"));
        items.add(placeholderMenu("Git"));
        items.add(action("Repair IDE on File", () -> ph("Repair IDE on File")));
        items.add(action("Reload from Disk", this::refresh));
        items.add(new SeparatorMenuItem());
        items.add(action("Compare With\u2026", () -> ph("Compare With")));

        if (isRoot || kind == NodeKind.SOURCE_ROOT) {
            items.add(new SeparatorMenuItem());
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
        }
        if (isRoot || isSourceish) {
            items.add(placeholderMenu("AI Assistant"));
            items.add(action("Upgrade Java Runtime and Frameworks", () -> ph("Upgrade Java Runtime and Frameworks")));
        }
        return items;
    }

    private Menu newMenu(Path dir, NodeKind kind) {
        Menu menu = new Menu("New");
        if (creationHandlers != null) {
            boolean isRoot = (kind == NodeKind.ROOT)
                    || (rootPath != null && dir != null && dir.toAbsolutePath().normalize().equals(rootPath.toAbsolutePath().normalize()))
                    || NewMenuBuilder.isProjectRoot(dir);
            NewMenuBuilder.populateNewMenu(menu, dir, true, isRoot, creationHandlers);
            return menu;
        }
        if (kind == NodeKind.ROOT) {
            menu.getItems().add(action("Module\u2026", () -> ph("Module")));
            menu.getItems().add(new SeparatorMenuItem());
        }

        boolean sourceRoot = kind == NodeKind.SOURCE_ROOT;
        boolean packageNode = kind == NodeKind.PACKAGE;

        if (sourceRoot || packageNode) {
            menu.getItems().add(action("Java Class", kindCircle("C", "#3592C4", 7.5), () -> run(onNewJavaClass, dir)));
            if (sourceRoot) {
                menu.getItems().add(action("Java Compact File", () -> ph("Java Compact File")));
            }
            menu.getItems().add(action("Kotlin Class/File", kindCircle("K", "#8A65D6", 7.5), () -> ph("Kotlin Class/File")));
            menu.getItems().add(action("File", letterBadge("\u25A2", "#BCBEC4", 8), () -> run(onNewFile, dir)));
            menu.getItems().add(action("Package", packageShape("#5A8FC2"), () -> run(onNewPackage, dir)));
            menu.getItems().add(action("FXML File", () -> ph("FXML File")));
            menu.getItems().add(action("JavaFX Application", () -> ph("JavaFX Application")));
            menu.getItems().add(action("package-info.java", () -> ph("package-info.java")));
            MenuItem moduleInfo = action("module-info.java", () -> ph("module-info.java"));
            moduleInfo.setDisable(true);
            menu.getItems().add(moduleInfo);
            menu.getItems().add(new SeparatorMenuItem());
            menu.getItems().add(action("Kotlin Notebook", () -> ph("Kotlin Notebook")));
            menu.getItems().add(action("Resource Bundle", () -> ph("Resource Bundle")));
            return menu;
        }

        menu.getItems().addAll(
                action("Java Class", kindCircle("C", "#3592C4", 7.5), () -> run(onNewJavaClass, dir)),
                action("Package", packageShape("#5A8FC2"), () -> run(onNewPackage, dir)),
                action("Directory", folderShape("#DCB67A"), () -> run(onNewDirectory, dir)),
                action("File", letterBadge("\u25A2", "#BCBEC4", 8), () -> run(onNewFile, dir)),
                action("Scratch File", () -> ph("Scratch File")),
                new SeparatorMenuItem(),
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

    private Menu placeholderMenu(String label) {
        Menu menu = new Menu(label);
        MenuItem soon = new MenuItem("(coming soon)");
        soon.setDisable(true);
        menu.getItems().add(soon);
        return menu;
    }

    private MenuItem action(String label, Runnable action) {
        return action(label, null, action);
    }

    private MenuItem action(String label, Node graphic, Runnable action) {
        MenuItem item = new MenuItem(label, graphic);
        item.setOnAction(e -> action.run());
        return item;
    }

    private void run(Consumer<Path> handler, Path p) {
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

        Button locate = new Button("\u25CE");
        locate.getStyleClass().add("console-button");
        locate.setTooltip(new Tooltip("Select Opened File"));
        locate.setOnAction(e -> {
            Path current = openedFile.get();
            if (current != null) selectFile(current);
        });

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(8, headerLabel, headerSpacer, locate);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 10, 6, 12));
        header.setMaxWidth(Double.MAX_VALUE);

        tree.getStyleClass().add("project-tree");
        tree.setShowRoot(false);
        tree.setCellFactory(tv -> new TreeNodeCell());
        dev.lumina.git.GitStatusManager.getInstance().addListener(() -> javafx.application.Platform.runLater(tree::refresh));

        tree.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Node n) {
                TreeCell<?> cell = findParentTreeCell(n);
                if (cell == null || cell.isEmpty() || cell.getItem() == null) {
                    tree.getSelectionModel().clearSelection();
                }
            }
            if (e.getClickCount() == 2) {
                TreeItem<ProjectTreeNode> item = tree.getSelectionModel().getSelectedItem();
                if (item != null && item.getValue() != null) {
                    item.getValue().open(onOpenFile);
                }
            }
        });

        tree.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                tree.getSelectionModel().clearSelection();
            } else if (e.getCode() == KeyCode.ENTER) {
                TreeItem<ProjectTreeNode> item = tree.getSelectionModel().getSelectedItem();
                if (item != null && item.getValue() != null) {
                    item.getValue().open(onOpenFile);
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

        TreeItem<ProjectTreeNode> invisibleRoot = new TreeItem<>(new ProjectTreeNode(
                ProjectTreeNode.NodeKind.PROJECT_ROOT,
                "Invisible Root",
                null,
                null,
                "folder",
                false,
                null,
                null
        ));

        // 1. The Project Root Folder
        ProjectTreeNode projectRootNode = createProjectRootNode(root);
        LazyTreeItem projectItem = new LazyTreeItem(projectRootNode);
        projectItem.setExpanded(true);

        // 2. External Libraries Node
        ProjectTreeNode externalLibsNode = ExternalLibrariesService.buildExternalLibrariesNode(root);
        LazyTreeItem externalLibsItem = new LazyTreeItem(externalLibsNode);

        // 3. Scratches and Consoles Node
        ProjectTreeNode scratchesNode = ScratchesAndConsolesService.buildScratchesAndConsolesNode(root);
        LazyTreeItem scratchesItem = new LazyTreeItem(scratchesNode);

        invisibleRoot.getChildren().addAll(projectItem, externalLibsItem, scratchesItem);
        tree.setRoot(invisibleRoot);
        tree.setShowRoot(false);
        setCenter(tree);
    }

    private ProjectTreeNode createProjectRootNode(Path root) {
        String dirName = root.getFileName() != null ? root.getFileName().toString() : root.toString();
        String moduleName = detectModuleName(root);
        String sub = "[" + moduleName + "] " + abbreviate(root);

        return new ProjectTreeNode(
                ProjectTreeNode.NodeKind.PROJECT_ROOT,
                dirName,
                sub,
                root,
                "folder",
                false,
                () -> buildProjectDirectoryChildren(root),
                null
        );
    }

    private String detectModuleName(Path root) {
        Path pom = root.resolve("pom.xml");
        if (Files.isRegularFile(pom)) {
            try {
                String content = Files.readString(pom);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("<artifactId>([^<]+)</artifactId>").matcher(content);
                if (m.find()) return m.group(1).trim();
            } catch (Exception ignored) {}
        }
        return root.getFileName() != null ? root.getFileName().toString() : "root";
    }

    private List<ProjectTreeNode> buildProjectDirectoryChildren(Path dir) {
        List<ProjectTreeNode> nodes = new ArrayList<>();
        for (Path p : listSorted(dir)) {
            if (Files.isDirectory(p) && underJavaRoot(p)) {
                // Flatten single-child directory chains into a dotted package
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
                final Path pkgEnd = end;
                nodes.add(new ProjectTreeNode(
                        ProjectTreeNode.NodeKind.PACKAGE,
                        name.toString(),
                        null,
                        pkgEnd,
                        "package",
                        false,
                        () -> buildProjectDirectoryChildren(pkgEnd),
                        null
                ));
            } else if (Files.isDirectory(p)) {
                String icon = "folder";
                if (isMainSourceRoot(p)) icon = "source-root";
                else if (isTestSourceRoot(p)) icon = "test-source-root";
                else if (isResourceRoot(p)) icon = "resource-root";

                final Path dirPath = p;
                nodes.add(new ProjectTreeNode(
                        ProjectTreeNode.NodeKind.DIRECTORY,
                        p.getFileName().toString(),
                        null,
                        dirPath,
                        icon,
                        false,
                        () -> buildProjectDirectoryChildren(dirPath),
                        null
                ));
            } else {
                String icon = detectFileIcon(p);
                nodes.add(ProjectTreeNode.file(p, icon));
            }
        }
        return nodes;
    }

    public void refresh() {
        refresh(null);
    }

    public void refresh(Path reveal) {
        if (rootPath == null) return;
        Set<Path> expanded = new LinkedHashSet<>();
        collectExpanded(tree.getRoot(), expanded);
        Path previouslySelected = getSelectedPath();
        setRoot(rootPath);
        for (Path p : expanded) expandTo(p);
        if (reveal != null) {
            selectFile(reveal);
        } else if (previouslySelected != null) {
            selectFile(previouslySelected);
        }
    }

    private void collectExpanded(TreeItem<ProjectTreeNode> node, Set<Path> out) {
        if (node == null) return;
        if (node != tree.getRoot() && node.isExpanded() && node.getValue() != null && node.getValue().getPath() != null) {
            out.add(node.getValue().getPath().toAbsolutePath().normalize());
        }
        for (TreeItem<ProjectTreeNode> child : node.getChildren()) {
            collectExpanded(child, out);
        }
    }

    public Set<Path> getExpandedPaths() {
        Set<Path> out = new LinkedHashSet<>();
        collectExpanded(tree.getRoot(), out);
        return out;
    }

    public void expandTo(Path target) {
        if (tree.getRoot() == null || target == null) return;
        Path t = target.toAbsolutePath().normalize();

        TreeItem<ProjectTreeNode> projectRootItem = findProjectRootItem();
        if (projectRootItem != null && projectRootItem.getValue() != null && projectRootItem.getValue().getPath() != null) {
            Path rootValue = projectRootItem.getValue().getPath().toAbsolutePath().normalize();
            if (t.startsWith(rootValue)) {
                expandDown(projectRootItem, t);
                return;
            }
        }

        // Also check Scratches / Extensions
        for (TreeItem<ProjectTreeNode> child : tree.getRoot().getChildren()) {
            if (child.getValue() != null && child.getValue().getPath() != null) {
                Path cv = child.getValue().getPath().toAbsolutePath().normalize();
                if (t.startsWith(cv)) {
                    expandDown(child, t);
                    return;
                }
            }
        }
    }

    private void expandDown(TreeItem<ProjectTreeNode> current, Path target) {
        Path currentPath = current.getValue() != null && current.getValue().getPath() != null
                ? current.getValue().getPath().toAbsolutePath().normalize() : null;

        if (currentPath == null) return;
        boolean progressed = true;
        while (progressed && !currentPath.equals(target)) {
            progressed = false;
            current.setExpanded(true);
            for (TreeItem<ProjectTreeNode> child : current.getChildren()) {
                if (child.getValue() != null && child.getValue().getPath() != null) {
                    Path cv = child.getValue().getPath().toAbsolutePath().normalize();
                    if (target.equals(cv) || target.startsWith(cv)) {
                        current = child;
                        currentPath = cv;
                        progressed = true;
                        break;
                    }
                }
            }
        }
        current.setExpanded(true);
    }

    public Path getSelectedPath() {
        TreeItem<ProjectTreeNode> item = tree.getSelectionModel().getSelectedItem();
        return item != null && item.getValue() != null ? item.getValue().getPath() : null;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public TreeView<ProjectTreeNode> getTree() {
        return tree;
    }

    public void selectFile(Path target) {
        if (tree.getRoot() == null || target == null) return;
        Path t = target.toAbsolutePath().normalize();
        expandTo(t);

        TreeItem<ProjectTreeNode> item = findItem(t);
        if (item != null) {
            tree.getSelectionModel().select(item);
            int row = tree.getRow(item);
            if (row >= 0) tree.scrollTo(Math.max(0, row - 5));
        }
    }

    private TreeItem<ProjectTreeNode> findProjectRootItem() {
        if (tree.getRoot() == null || tree.getRoot().getChildren().isEmpty()) return null;
        return tree.getRoot().getChildren().get(0);
    }

    private TreeItem<ProjectTreeNode> findItem(Path target) {
        TreeItem<ProjectTreeNode> root = tree.getRoot();
        if (root == null) return null;
        return searchItem(root, target);
    }

    private TreeItem<ProjectTreeNode> searchItem(TreeItem<ProjectTreeNode> current, Path target) {
        if (current.getValue() != null && current.getValue().getPath() != null) {
            if (current.getValue().getPath().toAbsolutePath().normalize().equals(target)) {
                return current;
            }
        }
        for (TreeItem<ProjectTreeNode> child : current.getChildren()) {
            TreeItem<ProjectTreeNode> res = searchItem(child, target);
            if (res != null) return res;
        }
        return null;
    }

    // ------------------------------------------------------------ TreeCell

    private class TreeNodeCell extends TreeCell<ProjectTreeNode> {
        TreeNodeCell() {
            setOnMousePressed(e -> {
                if (e.isSecondaryButtonDown() && !isEmpty() && getTreeItem() != null) {
                    getTreeView().getSelectionModel().select(getTreeItem());
                }
            });
        }

        @Override
        protected void updateItem(ProjectTreeNode node, boolean empty) {
            super.updateItem(node, empty);
            setGraphic(null);
            setText(null);
            getStyleClass().removeAll("git-added", "git-untracked", "git-modified");
            if (empty || node == null) {
                setStyle("");
                return;
            }

            Node icon = buildIcon(node.getIconKind());

            if (node.getSubText() != null && !node.getSubText().isBlank()) {
                Label mainLabel = new Label(node.getDisplayName());
                mainLabel.setStyle("-fx-text-fill: #BCBEC4; -fx-font-size: 13px;");
                Label subLabel = new Label(" " + node.getSubText());
                subLabel.setStyle("-fx-text-fill: #7A7E85; -fx-font-size: 12px;");
                HBox textRow = new HBox(mainLabel, subLabel);
                textRow.setAlignment(Pos.CENTER_LEFT);
                HBox fullRow = new HBox(6, icon, textRow);
                fullRow.setAlignment(Pos.CENTER_LEFT);
                setGraphic(fullRow);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                setText(node.getDisplayName());
                setGraphic(icon);
                setContentDisplay(ContentDisplay.LEFT);
            }

            if (node.getPath() != null && node.getKind() != ProjectTreeNode.NodeKind.PROJECT_ROOT) {
                dev.lumina.git.GitFileStatus gitStatus = dev.lumina.git.GitStatusManager.getInstance().getStatus(node.getPath());
                if (gitStatus != dev.lumina.git.GitFileStatus.NORMAL) {
                    setStyle("-fx-text-fill: " + gitStatus.getColorHex() + ";");
                    if (gitStatus == dev.lumina.git.GitFileStatus.ADDED) {
                        getStyleClass().add("git-added");
                    } else if (gitStatus == dev.lumina.git.GitFileStatus.UNTRACKED) {
                        getStyleClass().add("git-untracked");
                    } else if (gitStatus == dev.lumina.git.GitFileStatus.MODIFIED) {
                        getStyleClass().add("git-modified");
                    }
                } else {
                    setStyle("");
                }
            } else {
                setStyle("");
            }
        }
    }

    private static String detectFileIcon(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        if (n.endsWith(".java")) {
            return detectJavaKind(p);
        } else if (n.endsWith(".class")) {
            return "bytecode";
        } else if (n.endsWith(".xml") || n.endsWith(".pom")) {
            return "xml";
        } else if (n.endsWith(".md") || n.endsWith(".txt")) {
            return "text";
        } else if (n.endsWith(".properties") || n.endsWith(".yml") || n.endsWith(".yaml")) {
            return "config";
        } else if (n.startsWith(".git")) {
            return "git";
        } else if (n.endsWith(".groovy")) {
            return "groovy";
        } else if (n.endsWith(".js")) {
            return "javascript";
        } else if (n.endsWith(".sql")) {
            return "sql";
        }
        return "file";
    }

    private static String detectJavaKind(Path p) {
        try (var in = Files.newInputStream(p)) {
            String head = new String(in.readNBytes(4096), java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "\\b(?:public|private|protected|static|final|abstract|sealed"
                            + "|non-sealed|strictfp)\\s+(?:(?:public|private|protected|static"
                            + "|final|abstract|sealed|non-sealed|strictfp)\\s+)*"
                            + "(class|interface|enum|record|@interface)\\s+[A-Za-z_]")
                    .matcher(head);
            if (m.find()) return m.group(1);
        } catch (Exception ignored) {}
        return "class";
    }

    public static Node buildIcon(String kind) {
        if (kind == null) return letterBadge("\u2731", "#697089", 9);
        return switch (kind) {
            case "folder" -> folderShape("#DCB67A");
            case "source-root" -> sourceFolderShape("#4A88C7");
            case "test-source-root" -> sourceFolderShape("#57965C");
            case "resource-root" -> resourceFolderShape("#C29E5A");
            case "package" -> packageShape("#5A8FC2");
            case "interface" -> kindCircle("I", "#22A783");
            case "class" -> kindCircle("C", "#3592C4");
            case "exception" -> kindCircle("C", "#E5534B");
            case "enum" -> kindCircle("E", "#D9A03D");
            case "record" -> kindCircle("R", "#8A65D6");
            case "@interface" -> kindCircle("@", "#D9A03D");
            case "external-libraries" -> bookshelfIcon();
            case "sdk" -> kindCircle("\u2615", "#D9A03D", 8);
            case "library" -> libraryJarIcon();
            case "scratches" -> scratchesIcon();
            case "groovy" -> letterBadge("G", "#3592C4", 10);
            case "javascript" -> letterBadge("JS", "#E5A83B", 8);
            case "sql" -> letterBadge("SQL", "#4A88C7", 7);
            case "bytecode" -> letterBadge("\u2699", "#8B92A6", 13);
            case "xml" -> letterBadge("</>", "#8FCE8F", 8);
            case "text" -> letterBadge("\u2261", "#8B92A6", 12);
            case "config" -> letterBadge("\u2699", "#D9A03D", 11);
            case "git" -> kindCircle("git", "#E5534B", 7);
            case "file" -> letterBadge("\u2731", "#697089", 9);
            default -> kindCircle("C", "#3592C4");
        };
    }

    private static Node bookshelfIcon() {
        HBox shelf = new HBox(1.5);
        shelf.setAlignment(Pos.BOTTOM_CENTER);

        javafx.scene.shape.Rectangle b1 = new javafx.scene.shape.Rectangle(2.5, 9);
        b1.setFill(javafx.scene.paint.Color.web("#548AF7"));
        b1.setArcWidth(1);
        b1.setArcHeight(1);

        javafx.scene.shape.Rectangle b2 = new javafx.scene.shape.Rectangle(2.5, 11);
        b2.setFill(javafx.scene.paint.Color.web("#E5A83B"));
        b2.setArcWidth(1);
        b2.setArcHeight(1);

        javafx.scene.shape.Rectangle b3 = new javafx.scene.shape.Rectangle(2.5, 8);
        b3.setFill(javafx.scene.paint.Color.web("#A571E8"));
        b3.setArcWidth(1);
        b3.setArcHeight(1);

        shelf.getChildren().addAll(b1, b2, b3);
        return sized(shelf);
    }

    private static Node libraryJarIcon() {
        javafx.scene.shape.Rectangle jar = new javafx.scene.shape.Rectangle(11, 10);
        jar.setArcWidth(2);
        jar.setArcHeight(2);
        jar.setFill(javafx.scene.paint.Color.web("#C29E5A"));

        javafx.scene.shape.Line band = new javafx.scene.shape.Line(1, 4, 10, 4);
        band.setStroke(javafx.scene.paint.Color.web("#14161E"));
        band.setStrokeWidth(1);

        return sized(new StackPane(jar, band));
    }

    private static Node scratchesIcon() {
        return letterBadge("\u270E", "#8B92A6", 11);
    }

    private static Node packageShape(String colorHex) {
        javafx.scene.shape.Rectangle box = new javafx.scene.shape.Rectangle(12, 10);
        box.setArcWidth(2.5);
        box.setArcHeight(2.5);
        box.setFill(javafx.scene.paint.Color.web(colorHex));

        javafx.scene.shape.Line seam = new javafx.scene.shape.Line(2, 4, 10, 4);
        seam.setStroke(javafx.scene.paint.Color.web("#14161E"));
        seam.setStrokeWidth(1.1);

        javafx.scene.shape.Line tape = new javafx.scene.shape.Line(6, 4, 6, 9.5);
        tape.setStroke(javafx.scene.paint.Color.web("#14161E"));
        tape.setStrokeWidth(1.1);

        return sized(new StackPane(box, seam, tape));
    }

    private static Node sourceFolderShape(String colorHex) {
        Node folder = folderShape(colorHex);
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(2.0);
        dot.setFill(javafx.scene.paint.Color.web("#DFE1E5"));
        StackPane sp = new StackPane(folder, dot);
        StackPane.setAlignment(dot, Pos.CENTER);
        return sized(sp);
    }

    private static Node resourceFolderShape(String colorHex) {
        Node folder = folderShape(colorHex);
        javafx.scene.shape.Rectangle badge = new javafx.scene.shape.Rectangle(4, 4);
        badge.setFill(javafx.scene.paint.Color.web("#E8B450"));
        StackPane sp = new StackPane(folder, badge);
        StackPane.setAlignment(badge, Pos.BOTTOM_RIGHT);
        return sized(sp);
    }

    private static Node sized(Node n) {
        StackPane pane = new StackPane(n);
        pane.setPrefSize(15, 15);
        pane.setMinSize(15, 15);
        pane.setMaxSize(15, 15);
        return pane;
    }

    private static Node kindCircle(String letter, String colorHex) {
        return kindCircle(letter, colorHex, letter.length() > 1 ? 7 : 9);
    }

    private static Node kindCircle(String letter, String colorHex, double fontSize) {
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(6.5);
        circle.setFill(javafx.scene.paint.Color.web(colorHex));
        Label text = new Label(letter);
        text.setStyle("-fx-text-fill: #0B0E14; -fx-font-size: " + fontSize + "px; -fx-font-weight: bold;");
        return sized(new StackPane(circle, text));
    }

    private static Node letterBadge(String glyph, String colorHex, double fontSize) {
        Label text = new Label(glyph);
        text.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: " + fontSize + "px; -fx-font-weight: bold;");
        return sized(text);
    }

    private static Node folderShape(String colorHex) {
        javafx.scene.shape.Polygon folder = new javafx.scene.shape.Polygon(
                0, 2,   4, 2,   5.5, 0,   13, 0,   13, 2,
                13, 10, 0, 10);
        folder.setFill(javafx.scene.paint.Color.web(colorHex));
        return sized(folder);
    }

    private static String abbreviate(Path p) {
        String home = System.getProperty("user.home");
        String s = p.toAbsolutePath().toString();
        return s.startsWith(home) ? "~" + s.substring(home.length()) : s;
    }

    // ------------------------------------------------------- LazyTreeItem

    public static class LazyTreeItem extends TreeItem<ProjectTreeNode> {
        private boolean loaded = false;

        public LazyTreeItem(ProjectTreeNode node) {
            super(node);
        }

        @Override
        public boolean isLeaf() {
            return getValue() == null || getValue().isLeaf();
        }

        @Override
        public ObservableList<TreeItem<ProjectTreeNode>> getChildren() {
            if (!loaded && getValue() != null && !getValue().isLeaf()) {
                loaded = true;
                List<ProjectTreeNode> children = getValue().loadChildren();
                for (ProjectTreeNode child : children) {
                    super.getChildren().add(new LazyTreeItem(child));
                }
            }
            return super.getChildren();
        }
    }

    private static boolean underJavaRoot(Path p) {
        String s = p.toAbsolutePath().toString().replace('\\', '/');
        return s.contains("/src/main/java/") || s.contains("/src/test/java/")
                || s.contains("/src/main/kotlin/") || s.contains("/src/test/kotlin/")
                || s.contains("/src/main/groovy/") || s.contains("/src/test/groovy/");
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

    private TreeCell<?> findParentTreeCell(Node n) {
        while (n != null && n != tree) {
            if (n instanceof TreeCell<?> tc) return tc;
            n = n.getParent();
        }
        return null;
    }
}