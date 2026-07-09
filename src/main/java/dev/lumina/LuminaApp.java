package dev.lumina;

import dev.lumina.project.ProjectGenerator;
import dev.lumina.project.ProjectSpec;
import dev.lumina.ui.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Lumina IDE — Phase 2.
 * IntelliJ-style shell: icon rail, toolbar with run controls, project
 * explorer, tabbed editor, console, breadcrumb status bar — plus a
 * New Project wizard (Java & Spring Boot).
 */
public class LuminaApp extends Application {

    private Stage stage;
    private TabPane editorTabs;
    private WelcomeView welcomeView;
    private FileExplorer fileExplorer;
    private ConsolePane console;
    private SplitPane verticalSplit;
    private SplitPane horizontalSplit;
    private IconRail iconRail;
    private Label projectChip;
    private HBox breadcrumbBar;
    private Label statusCaret;

    private Path projectRoot;
    private int untitledCounter = 1;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        // --- top: menu bar + toolbar ---
        VBox top = new VBox(buildMenuBar(), buildToolBar());
        root.setTop(top);

        // --- left rail ---
        iconRail = new IconRail(
                this::toggleProjectPanel,
                this::toggleConsole,
                this::runCurrentFile,
                this::showNewProjectDialog);
        root.setLeft(iconRail);

        // --- project explorer ---
        fileExplorer = new FileExplorer(this::openFile);

        // --- editor tabs over welcome view ---
        editorTabs = new TabPane();
        editorTabs.getStyleClass().add("editor-tabs");
        editorTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        welcomeView = new WelcomeView(
                this::showNewProjectDialog, this::newFile,
                this::openFileDialog, this::openFolderDialog);
        StackPane editorArea = new StackPane(welcomeView, editorTabs);
        editorTabs.getTabs().addListener(
                (javafx.collections.ListChangeListener<Tab>) c -> updateEditorVisibility());

        // --- console ---
        console = new ConsolePane();

        verticalSplit = new SplitPane(editorArea, console);
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.setDividerPositions(0.72);

        horizontalSplit = new SplitPane(fileExplorer, verticalSplit);
        horizontalSplit.setDividerPositions(0.22);
        SplitPane.setResizableWithParent(fileExplorer, false);

        root.setCenter(horizontalSplit);
        root.setBottom(buildStatusBar());
        updateEditorVisibility();

        Scene scene = new Scene(root, 1360, 840);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());

        stage.setTitle("Lumina");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> console.shutdown());
        stage.show();

        editorTabs.getSelectionModel().selectedItemProperty().addListener((obs, old, tab) -> {
            if (tab instanceof EditorTab et) {
                updateBreadcrumbs(et.getPath(), et.getText());
                et.setCaretListener(this::updateCaretStatus);
                et.focusEditor();
            } else {
                updateBreadcrumbs(null, null);
                statusCaret.setText("");
            }
        });
    }

    // ------------------------------------------------------------------ menu

    private MenuBar buildMenuBar() {
        // File > New submenu, IntelliJ-style
        Menu newMenu = new Menu("New");
        newMenu.getItems().addAll(
                item("Project\u2026", "Shortcut+Shift+N", e -> showNewProjectDialog()),
                new SeparatorMenuItem(),
                item("Java Class", null, e -> newJavaClass()),
                item("Package", null, e -> newPackage()),
                item("File", "Shortcut+N", e -> newFile()));

        Menu file = new Menu("File");
        file.getItems().addAll(
                newMenu,
                item("Open File\u2026", "Shortcut+O", e -> openFileDialog()),
                item("Open Folder\u2026", "Shortcut+Shift+O", e -> openFolderDialog()),
                new SeparatorMenuItem(),
                item("Save", "Shortcut+S", e -> saveCurrent(false)),
                item("Save As\u2026", "Shortcut+Shift+S", e -> saveCurrent(true)),
                new SeparatorMenuItem(),
                item("Close Tab", "Shortcut+W", e -> closeCurrentTab()),
                item("Exit", null, e -> Platform.exit()));

        Menu edit = new Menu("Edit");
        edit.getItems().addAll(
                item("Undo", "Shortcut+Z", e -> withEditor(EditorTab::undo)),
                item("Redo", "Shortcut+Shift+Z", e -> withEditor(EditorTab::redo)),
                new SeparatorMenuItem(),
                item("Cut", "Shortcut+X", e -> withEditor(EditorTab::cut)),
                item("Copy", "Shortcut+C", e -> withEditor(EditorTab::copy)),
                item("Paste", "Shortcut+V", e -> withEditor(EditorTab::paste)),
                item("Select All", "Shortcut+A", e -> withEditor(EditorTab::selectAll)));

        Menu run = new Menu("Run");
        run.getItems().addAll(
                item("Run File", "Shortcut+R", e -> runCurrentFile()),
                item("Stop", "Shortcut+F2", e -> console.stopProcess()),
                new SeparatorMenuItem(),
                item("Clear Console", null, e -> console.clear()));

        Menu view = new Menu("View");
        CheckMenuItem toggleConsoleItem = new CheckMenuItem("Console");
        toggleConsoleItem.setSelected(true);
        toggleConsoleItem.setOnAction(e -> {
            toggleConsole(toggleConsoleItem.isSelected());
            iconRail.setConsoleSelected(toggleConsoleItem.isSelected());
        });
        view.getItems().add(toggleConsoleItem);

        Menu help = new Menu("Help");
        help.getItems().add(item("About Lumina", null, e -> showAbout()));

        MenuBar bar = new MenuBar(file, edit, run, view, help);
        bar.setUseSystemMenuBar(false); // same in-window bar on mac/win/linux
        return bar;
    }

    private MenuItem item(String text, String accelerator,
                          javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        MenuItem mi = new MenuItem(text);
        if (accelerator != null) mi.setAccelerator(KeyCombination.keyCombination(accelerator));
        mi.setOnAction(action);
        return mi;
    }

    // --------------------------------------------------------------- toolbar

    private HBox buildToolBar() {
        projectChip = new Label("No project");
        projectChip.getStyleClass().add("project-chip");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button runBtn = toolButton("\u25B6", "Run current file (Ctrl/Cmd+R)");
        runBtn.getStyleClass().add("tool-run");
        runBtn.setOnAction(e -> runCurrentFile());

        Button stopBtn = toolButton("\u25A0", "Stop (Ctrl/Cmd+F2)");
        stopBtn.getStyleClass().add("tool-stop");
        stopBtn.setOnAction(e -> console.stopProcess());

        HBox bar = new HBox(10, projectChip, spacer, runBtn, stopBtn);
        bar.getStyleClass().add("tool-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(5, 12, 5, 12));
        return bar;
    }

    private Button toolButton(String glyph, String tip) {
        Button b = new Button(glyph);
        b.getStyleClass().add("tool-button");
        b.setTooltip(new Tooltip(tip));
        return b;
    }

    // ------------------------------------------------------------ status bar

    private HBox buildStatusBar() {
        breadcrumbBar = new HBox(4);
        breadcrumbBar.setAlignment(Pos.CENTER_LEFT);
        updateBreadcrumbs(null, null);

        statusCaret = new Label("");
        Label brand = new Label("Lumina 0.2");
        brand.getStyleClass().add("status-brand");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(16, breadcrumbBar, spacer, statusCaret, brand);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(3, 12, 3, 12));
        return bar;
    }

    private void updateBreadcrumbs(Path filePath, String fallback) {
        breadcrumbBar.getChildren().clear();
        Path shown = filePath;
        if (shown == null && projectRoot != null) shown = projectRoot;
        if (shown == null) {
            breadcrumbBar.getChildren().add(crumb(fallback != null ? fallback : "Ready"));
            return;
        }
        Path rel = shown;
        if (projectRoot != null && shown.startsWith(projectRoot)) {
            breadcrumbBar.getChildren().add(crumb(projectRoot.getFileName().toString()));
            rel = projectRoot.relativize(shown);
        }
        for (Path segment : rel) {
            if (segment.toString().isEmpty()) continue;
            if (!breadcrumbBar.getChildren().isEmpty()) {
                Label sep = new Label("\u203A");
                sep.getStyleClass().add("crumb-sep");
                breadcrumbBar.getChildren().add(sep);
            }
            breadcrumbBar.getChildren().add(crumb(segment.toString()));
        }
    }

    private Label crumb(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("crumb");
        return l;
    }

    private void updateCaretStatus(int line, int col) {
        statusCaret.setText(line + ":" + col);
    }

    // ----------------------------------------------------------- new project

    private void showNewProjectDialog() {
        new NewProjectDialog(stage, this::createProject).show();
    }

    private void createProject(ProjectSpec spec) {
        console.clear();
        console.println("\u25B6 Creating " + spec.name() + " ("
                + (spec.generator() == ProjectSpec.Generator.SPRING_BOOT
                ? "Spring Boot" : "Java") + ", " + spec.buildSystem() + ") \u2026");

        Thread worker = new Thread(() -> {
            try {
                Path dir = ProjectGenerator.generate(spec, console::println);
                Platform.runLater(() -> openProject(dir));
            } catch (IOException ex) {
                console.println("\u2717 " + ex.getMessage());
            }
        }, "lumina-project-generator");
        worker.setDaemon(true);
        worker.start();
    }

    private void openProject(Path dir) {
        projectRoot = dir;
        fileExplorer.setRoot(dir);
        projectChip.setText("\uD83D\uDCC1 " + dir.getFileName());
        stage.setTitle("Lumina \u2014 " + dir.getFileName());
        updateBreadcrumbs(null, null);

        // open the most interesting file: *Application.java, else Main.java, else any .java
        try (Stream<Path> walk = Files.walk(dir)) {
            Optional<Path> toOpen = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted((a, b) -> Integer.compare(rank(a), rank(b)))
                    .findFirst();
            toOpen.ifPresent(this::openFile);
        } catch (IOException ignored) {
        }
    }

    private int rank(Path p) {
        String n = p.getFileName().toString();
        if (n.endsWith("Application.java")) return 0;
        if (n.equals("Main.java")) return 1;
        return 2;
    }

    // ---------------------------------------------------- new class/pkg/file

    private void newJavaClass() {
        Path dir = targetDirectory();
        if (dir == null) return;
        prompt("New Java Class", "Class name:", "MyClass").ifPresent(raw -> {
            String name = raw.replace(".java", "").trim();
            if (name.isEmpty()) return;
            String pkg = inferPackage(dir);
            String body = (pkg.isEmpty() ? "" : "package " + pkg + ";\n\n")
                    + "public class " + name + " {\n\n}\n";
            Path file = dir.resolve(name + ".java");
            writeAndOpen(file, body);
        });
    }

    private void newPackage() {
        Path dir = targetDirectory();
        if (dir == null) return;
        prompt("New Package", "Package name (dotted):", "com.example.util").ifPresent(raw -> {
            String pkg = raw.trim();
            if (pkg.isEmpty()) return;
            try {
                Files.createDirectories(dir.resolve(pkg.replace('.', '/')));
                fileExplorer.refresh();
            } catch (IOException ex) {
                error("Could not create package", ex.getMessage());
            }
        });
    }

    private void newFile() {
        if (projectRoot == null && fileExplorer.getRootPath() == null) {
            // no project open: fall back to an untitled buffer
            addTab(new EditorTab("Untitled-" + untitledCounter++ + ".java", null));
            return;
        }
        Path dir = targetDirectory();
        if (dir == null) return;
        prompt("New File", "File name:", "notes.md").ifPresent(raw -> {
            String name = raw.trim();
            if (name.isEmpty()) return;
            writeAndOpen(dir.resolve(name), "");
        });
    }

    private Path targetDirectory() {
        Path selected = fileExplorer.getSelectedPath();
        if (selected != null) {
            return Files.isDirectory(selected) ? selected : selected.getParent();
        }
        Path root = fileExplorer.getRootPath();
        if (root == null) {
            error("No folder open", "Open or create a project first (File \u2192 New \u2192 Project\u2026).");
            return null;
        }
        return root;
    }

    /** Derive a package from a directory under src/main/java or src/test/java. */
    private String inferPackage(Path dir) {
        Path abs = dir.toAbsolutePath().normalize();
        String[] roots = {"src/main/java", "src/test/java"};
        Path base = fileExplorer.getRootPath();
        if (base == null) return "";
        for (String r : roots) {
            Path marker = base.toAbsolutePath().normalize().resolve(r);
            if (abs.startsWith(marker)) {
                Path rel = marker.relativize(abs);
                return rel.toString().isEmpty() ? "" : rel.toString()
                        .replace(File.separatorChar, '.').replace('/', '.');
            }
        }
        return "";
    }

    private void writeAndOpen(Path file, String content) {
        try {
            if (Files.exists(file)) {
                error("Already exists", file.getFileName() + " already exists here.");
                return;
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
            fileExplorer.refresh();
            openFile(file);
        } catch (IOException ex) {
            error("Could not create file", ex.getMessage());
        }
    }

    private Optional<String> prompt(String title, String label, String initial) {
        TextInputDialog dialog = new TextInputDialog(initial);
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.setContentText(label);
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        return dialog.showAndWait();
    }

    // --------------------------------------------------------------- actions

    private void updateEditorVisibility() {
        boolean hasTabs = !editorTabs.getTabs().isEmpty();
        editorTabs.setVisible(hasTabs);
        welcomeView.setVisible(!hasTabs);
    }

    private void openFileDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Java Sources", "*.java"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File f = chooser.showOpenDialog(stage);
        if (f != null) openFile(f.toPath());
    }

    private void openFolderDialog() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Folder");
        File dir = chooser.showDialog(stage);
        if (dir != null) openProject(dir.toPath());
    }

    private void openFile(Path path) {
        for (Tab t : editorTabs.getTabs()) {
            if (t instanceof EditorTab et && path.equals(et.getPath())) {
                editorTabs.getSelectionModel().select(t);
                return;
            }
        }
        try {
            String content = Files.readString(path);
            EditorTab tab = new EditorTab(path.getFileName().toString(), path);
            tab.setEditorText(content);
            addTab(tab);
        } catch (IOException ex) {
            error("Could not open file", ex.getMessage());
        }
    }

    private void addTab(EditorTab tab) {
        editorTabs.getTabs().add(tab);
        editorTabs.getSelectionModel().select(tab);
        tab.focusEditor();
    }

    private void saveCurrent(boolean saveAs) {
        EditorTab tab = currentEditor();
        if (tab == null) return;

        Path target = tab.getPath();
        if (saveAs || target == null) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save File");
            chooser.setInitialFileName(tab.getText().replace("\u25CF ", ""));
            File f = chooser.showSaveDialog(stage);
            if (f == null) return;
            target = f.toPath();
        }
        try {
            Files.writeString(target, tab.getEditorText());
            tab.markSaved(target);
            updateBreadcrumbs(target, null);
            fileExplorer.refresh();
        } catch (IOException ex) {
            error("Could not save file", ex.getMessage());
        }
    }

    private void closeCurrentTab() {
        Tab t = editorTabs.getSelectionModel().getSelectedItem();
        if (t != null) editorTabs.getTabs().remove(t);
    }

    private void runCurrentFile() {
        EditorTab tab = currentEditor();
        if (tab == null) return;
        saveCurrent(false);
        if (tab.getPath() == null) return;
        runVisible();
        console.runJavaFile(tab.getPath());
    }

    private void runVisible() {
        if (!verticalSplit.getItems().contains(console)) {
            toggleConsole(true);
            iconRail.setConsoleSelected(true);
        }
    }

    private void toggleConsole(boolean show) {
        if (show && !verticalSplit.getItems().contains(console)) {
            verticalSplit.getItems().add(console);
            verticalSplit.setDividerPositions(0.72);
        } else if (!show) {
            verticalSplit.getItems().remove(console);
        }
    }

    private void toggleProjectPanel(boolean show) {
        if (show && !horizontalSplit.getItems().contains(fileExplorer)) {
            horizontalSplit.getItems().add(0, fileExplorer);
            horizontalSplit.setDividerPositions(0.22);
        } else if (!show) {
            horizontalSplit.getItems().remove(fileExplorer);
        }
    }

    private EditorTab currentEditor() {
        Tab t = editorTabs.getSelectionModel().getSelectedItem();
        return (t instanceof EditorTab et) ? et : null;
    }

    private void withEditor(java.util.function.Consumer<EditorTab> action) {
        EditorTab tab = currentEditor();
        if (tab != null) action.accept(tab);
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Lumina");
        alert.setHeaderText("Lumina IDE 0.2");
        alert.setContentText("""
                A luminous, lightweight Java IDE.
                Built with Java 25, JavaFX and Maven.

                Phase 2: IntelliJ-style shell, New Project wizard
                (Java & Spring Boot via start.spring.io), class/package
                creation, run & console.""");
        alert.initOwner(stage);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        alert.showAndWait();
    }

    private void error(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lumina");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
