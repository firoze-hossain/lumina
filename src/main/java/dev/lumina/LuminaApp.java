package dev.lumina;

import dev.lumina.git.GitHubAuth;
import dev.lumina.git.GitService;
import dev.lumina.project.ProjectGenerator;
import dev.lumina.project.ProjectSpec;
import dev.lumina.run.RunConfiguration;
import dev.lumina.ui.*;
import dev.lumina.util.Settings;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Lumina IDE — Phase 3.
 * Full IntelliJ-style menu bar, Run/Terminal tool windows, Git integration
 * with branch switching, smart run (Java file / Maven / Gradle / Spring
 * Boot), Go to File, and .class viewing via javap.
 */
public class LuminaApp extends Application {

    private Stage stage;
    private TabPane editorTabs;
    private WelcomeView welcomeView;
    private FileExplorer fileExplorer;
    private ConsolePane console;
    private TerminalPane terminal;
    private TabPane bottomTabs;
    private TabPane rightTabs;
    private SplitPane outerSplit;
    private MavenPanel mavenPanel;
    private DatabasePanel dbPanel;
    private SplitPane verticalSplit;
    private SplitPane horizontalSplit;
    private IconRail iconRail;
    private Label projectChip;
    private MenuButton branchButton;
    private Button githubButton;
    private ComboBox<RunConfiguration> runConfigBox;
    private HBox breadcrumbBar;
    private Label statusCaret;

    private Path projectRoot;
    private int untitledCounter = 1;
    private long lastShiftPress;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(new VBox(buildMenuBar(), buildToolBar()));

        iconRail = new IconRail(
                this::toggleProjectPanel,
                this::toggleBottomPanel,
                this::showTerminal,
                this::runSelectedConfig,
                this::showNewProjectDialog);
        root.setLeft(iconRail);

        fileExplorer = new FileExplorer(this::openFile, () -> {
            EditorTab tab = currentEditor();
            return tab != null ? tab.getPath() : null;
        });

        editorTabs = new TabPane();
        editorTabs.getStyleClass().add("editor-tabs");
        editorTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        welcomeView = new WelcomeView(
                this::showNewProjectDialog, this::newFile,
                this::openFileDialog, this::openFolderDialog);
        StackPane editorArea = new StackPane(welcomeView, editorTabs);
        editorTabs.getTabs().addListener(
                (javafx.collections.ListChangeListener<Tab>) c -> updateEditorVisibility());

        // bottom tool windows: Run + Terminal
        console = new ConsolePane();
        terminal = new TerminalPane();
        bottomTabs = new TabPane(
                toolTab("Run", console),
                toolTab("Terminal", terminal));
        bottomTabs.getStyleClass().add("tool-tabs");
        bottomTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        verticalSplit = new SplitPane(editorArea, bottomTabs);
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.setDividerPositions(0.70);

        horizontalSplit = new SplitPane(fileExplorer, verticalSplit);
        horizontalSplit.setDividerPositions(0.22);
        SplitPane.setResizableWithParent(fileExplorer, false);

        // right tool windows: Maven/Gradle goals + Database (hidden by default)
        mavenPanel = new MavenPanel(this::runBuildGoal);
        dbPanel = new DatabasePanel();
        rightTabs = new TabPane(toolTab("Maven", mavenPanel), toolTab("Database", dbPanel));
        rightTabs.getStyleClass().add("tool-tabs");
        rightTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        SplitPane.setResizableWithParent(rightTabs, false);

        outerSplit = new SplitPane(horizontalSplit);
        outerSplit.setDividerPositions(0.78);

        root.setCenter(outerSplit);
        root.setBottom(buildStatusBar());
        updateEditorVisibility();

        Scene scene = new Scene(root, 1400, 860);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());

        // IntelliJ-style double-Shift -> Search Everywhere
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.SHIFT) {
                long now = System.currentTimeMillis();
                if (now - lastShiftPress < 350) {
                    lastShiftPress = 0;
                    searchEverywhere();
                } else {
                    lastShiftPress = now;
                }
            } else {
                lastShiftPress = 0;
            }
        });

        stage.setTitle("Lumina");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            console.shutdown();
            terminal.stop();
            dbPanel.shutdown();
        });
        stage.show();

        terminal.start(Path.of(System.getProperty("user.home")));
        refreshRunConfigs();
        refreshGitInfo();

        // reopen the last project unless it was explicitly closed
        String last = Settings.get(Settings.LAST_PROJECT);
        if (last != null && Files.isDirectory(Path.of(last))) {
            openProject(Path.of(last));
        }

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

    private Tab toolTab(String name, javafx.scene.Node content) {
        Tab t = new Tab(name, content);
        t.setClosable(false);
        return t;
    }

    // ------------------------------------------------------------------ menus

    private MenuBar buildMenuBar() {
        // ---- File
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
                item("Close Project", null, e -> closeProject()),
                new SeparatorMenuItem(),
                item("Save", "Shortcut+S", e -> saveCurrent(false)),
                item("Save As\u2026", "Shortcut+Shift+S", e -> saveCurrent(true)),
                new SeparatorMenuItem(),
                item("Close Tab", "Shortcut+W", e -> closeCurrentTab()),
                item("Exit", null, e -> Platform.exit()));

        // ---- Edit
        Menu edit = new Menu("Edit");
        edit.getItems().addAll(
                item("Undo", "Shortcut+Z", e -> withEditor(EditorTab::undo)),
                item("Redo", "Shortcut+Shift+Z", e -> withEditor(EditorTab::redo)),
                new SeparatorMenuItem(),
                item("Cut", "Shortcut+X", e -> withEditor(EditorTab::cut)),
                item("Copy", "Shortcut+C", e -> withEditor(EditorTab::copy)),
                item("Paste", "Shortcut+V", e -> withEditor(EditorTab::paste)),
                item("Select All", "Shortcut+A", e -> withEditor(EditorTab::selectAll)),
                new SeparatorMenuItem(),
                item("Find in Files\u2026", "Shortcut+Shift+F", e -> findInFiles()));

        // ---- View
        Menu toolWindows = new Menu("Tool Windows");
        toolWindows.getItems().addAll(
                item("Project", null, e -> toggleProjectPanel(
                        !horizontalSplit.getItems().contains(fileExplorer))),
                item("Run", null, e -> showRunPanel()),
                item("Terminal", null, e -> showTerminal()),
                item("Maven / Build", null, e -> showRightPanel(0)),
                item("Database", null, e -> showRightPanel(1)));
        Menu view = new Menu("View");
        view.getItems().add(toolWindows);

        // ---- Navigate
        Menu navigate = new Menu("Navigate");
        navigate.getItems().addAll(
                item("Search Everywhere (double Shift)", "Shortcut+Shift+A",
                        e -> searchEverywhere()),
                item("Go to Declaration", "Shortcut+B", e -> {
                    EditorTab tab = currentEditor();
                    if (tab != null) goToDeclaration(tab.wordAtCaret());
                }),
                item("Find Usages", "Alt+F7", e -> {
                    EditorTab tab = currentEditor();
                    if (tab != null) showUsages(tab.wordAtCaret());
                }),
                new SeparatorMenuItem(),
                item("Go to File\u2026", "Shortcut+P", e -> goToFile()),
                item("Go to Line\u2026", "Shortcut+G", e -> goToLine()));

        // ---- Code
        Menu code = new Menu("Code");
        code.getItems().addAll(
                item("Toggle Line Comment", "Shortcut+Slash",
                        e -> withEditor(EditorTab::toggleComment)),
                item("Generate Test for Current Class", null, e -> generateTest()),
                disabled("Reformat Code (soon)"),
                disabled("Optimize Imports (soon)"));

        // ---- Refactor
        Menu refactor = new Menu("Refactor");
        refactor.getItems().addAll(
                item("Rename File\u2026", null, e -> renameSelectedFile()),
                disabled("Rename Symbol (soon)"),
                disabled("Extract Method (soon)"));

        // ---- Build
        Menu build = new Menu("Build");
        build.getItems().addAll(
                item("Build Project", "Shortcut+F9", e -> buildProject()),
                item("Clean Project", null, e -> cleanProject()));

        // ---- Run
        Menu run = new Menu("Run");
        run.getItems().addAll(
                item("Run", "Shortcut+R", e -> runSelectedConfig()),
                item("Run Current File", "Shortcut+Shift+R", e -> runCurrentFile()),
                item("Debug", "Shortcut+D", e -> debugSelectedConfig()),
                new SeparatorMenuItem(),
                item("Run All Tests", "Shortcut+Shift+T", e -> runAllTests()),
                item("Run Current Test Class", null, e -> runCurrentTestClass()),
                new SeparatorMenuItem(),
                item("Stop", "Shortcut+F2", e -> console.stopProcess()),
                new SeparatorMenuItem(),
                item("Clear Run Output", null, e -> console.clear()));

        // ---- Git
        Menu git = new Menu("Git");
        git.getItems().addAll(
                item("Clone Repository\u2026", null, e -> gitClone()),
                item("Init Repository", null, e -> gitInit()),
                item("Commit\u2026", "Shortcut+K", e -> gitCommit()),
                item("Push", "Shortcut+Shift+K", e -> gitRun("Push", "push")),
                item("Pull", null, e -> gitRun("Pull", "pull")),
                item("Fetch", null, e -> gitRun("Fetch", "fetch")),
                item("Show Status", null, e -> gitStatus()),
                item("Toggle Blame Annotations", "Shortcut+Alt+B", e -> toggleBlame()),
                new SeparatorMenuItem(),
                item("New Branch\u2026", null, e -> gitNewBranch()),
                new SeparatorMenuItem(),
                item("Sign in to GitHub\u2026", null, e -> onGitHubButton()),
                item("Open Repository on GitHub", null, e -> openRemote()));

        // ---- Tools
        Menu tools = new Menu("Tools");
        tools.getItems().addAll(
                item("Terminal", "Shortcut+T", e -> showTerminal()),
                item("New Project Wizard\u2026", null, e -> showNewProjectDialog()));

        // ---- Window
        Menu window = new Menu("Window");
        window.getItems().addAll(
                item("Toggle Project Panel", null, e -> toggleProjectPanel(
                        !horizontalSplit.getItems().contains(fileExplorer))),
                item("Toggle Bottom Panel", null, e -> {
                    boolean show = !verticalSplit.getItems().contains(bottomTabs);
                    toggleBottomPanel(show);
                    iconRail.setBottomSelected(show);
                }));

        // ---- Help
        Menu help = new Menu("Help");
        help.getItems().add(item("About Lumina", null, e -> showAbout()));

        MenuBar bar = new MenuBar(file, edit, view, navigate, code, refactor,
                build, run, git, tools, window, help);
        bar.setUseSystemMenuBar(false);
        return bar;
    }

    private MenuItem item(String text, String accelerator,
                          javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        MenuItem mi = new MenuItem(text);
        if (accelerator != null) mi.setAccelerator(KeyCombination.keyCombination(accelerator));
        mi.setOnAction(action);
        return mi;
    }

    private MenuItem disabled(String text) {
        MenuItem mi = new MenuItem(text);
        mi.setDisable(true);
        return mi;
    }

    // --------------------------------------------------------------- toolbar

    private HBox buildToolBar() {
        projectChip = new Label("No project");
        projectChip.getStyleClass().add("project-chip");

        branchButton = new MenuButton("\u2387 no vcs");
        branchButton.getStyleClass().add("branch-chip");
        branchButton.setOnShowing(e -> populateBranchMenu());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        runConfigBox = new ComboBox<>();
        runConfigBox.getStyleClass().add("run-config-box");
        runConfigBox.setPrefWidth(240);

        Button runBtn = toolButton("\u25B6", "Run selected configuration (Ctrl/Cmd+R)");
        runBtn.getStyleClass().add("tool-run");
        runBtn.setOnAction(e -> runSelectedConfig());

        Button debugBtn = new Button("Debug \uD83D\uDC1E");
        debugBtn.getStyleClass().addAll("tool-button", "tool-debug");
        debugBtn.setTooltip(new Tooltip(
                "Debug (Ctrl/Cmd+D) \u2014 launches with JDWP on port 5005 and attaches jdb"));
        debugBtn.setOnAction(e -> debugSelectedConfig());

        Button stopBtn = toolButton("\u25A0", "Stop (Ctrl/Cmd+F2)");
        stopBtn.getStyleClass().add("tool-stop");
        stopBtn.setOnAction(e -> console.stopProcess());

        Button searchBtn = toolButton("\uD83D\uDD0D", "Search Everywhere (double Shift)");
        searchBtn.setOnAction(e -> searchEverywhere());

        githubButton = toolButton("\uD83D\uDC64 Sign in",
                "Sign in to GitHub \u2014 authenticates push/pull/clone in the IDE");
        githubButton.setOnAction(e -> onGitHubButton());
        refreshGitHubButton();

        Button sideBtn = toolButton("\u25A5", "Maven / Database panel");
        sideBtn.setOnAction(e -> {
            if (outerSplit.getItems().contains(rightTabs)) toggleRightPanel(false);
            else showRightPanel(rightTabs.getSelectionModel().getSelectedIndex());
        });

        HBox bar = new HBox(10, projectChip, branchButton, spacer,
                runConfigBox, runBtn, debugBtn, stopBtn,
                searchBtn, githubButton, sideBtn);
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

    // -------------------------------------------------------------- git chip

    private void refreshGitInfo() {
        String branch = projectRoot != null ? GitService.currentBranch(projectRoot) : null;
        branchButton.setText(branch != null ? "\u2387 " + branch : "\u2387 no vcs");
    }

    private void populateBranchMenu() {
        branchButton.getItems().clear();
        if (projectRoot == null || !GitService.isRepository(projectRoot)) {
            MenuItem none = new MenuItem(projectRoot == null
                    ? "Open a project first" : "Not a git repository \u2014 Git \u2192 Init");
            none.setDisable(true);
            branchButton.getItems().add(none);
            return;
        }
        String current = GitService.currentBranch(projectRoot);
        for (String b : GitService.localBranches(projectRoot)) {
            MenuItem mi = new MenuItem((b.equals(current) ? "\u2713 " : "    ") + b);
            mi.setOnAction(e -> checkout(b));
            branchButton.getItems().add(mi);
        }
        branchButton.getItems().addAll(new SeparatorMenuItem(),
                item("New Branch\u2026", null, e -> gitNewBranch()));
    }

    private void checkout(String branch) {
        Thread t = new Thread(() -> {
            GitService.Result r = GitService.checkout(projectRoot, branch);
            console.println(r.output().isBlank()
                    ? "Switched to branch '" + branch + "'" : r.output().trim());
            Platform.runLater(() -> {
                refreshGitInfo();
                fileExplorer.refresh();
            });
        }, "lumina-git");
        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------------- git menu

    private boolean requireProject() {
        if (projectRoot == null) {
            error("No project open", "Open or create a project first.");
            return false;
        }
        return true;
    }

    private void gitInit() {
        if (!requireProject()) return;
        GitService.Result r = GitService.init(projectRoot);
        console.println(r.output().trim());
        refreshGitInfo();
        fileExplorer.refresh();
    }

    private void gitCommit() {
        if (!requireProject()) return;
        prompt("Commit", "Commit message:", "update").ifPresent(msg -> {
            if (msg.isBlank()) return;
            showRunPanel();
            console.runSequence("git commit",
                    List.of(List.of("git", "add", "-A"),
                            List.of("git", "commit", "-m", msg)),
                    projectRoot);
        });
    }

    private void gitRun(String label, String subcommand) {
        if (!requireProject()) return;
        showRunPanel();
        console.runSequence("git " + label.toLowerCase(),
                List.of(List.of("git", subcommand)), projectRoot, gitEnv(), null);
    }

    /** Extra env so git authenticates with the signed-in GitHub token. */
    private java.util.Map<String, String> gitEnv() {
        String token = Settings.get(Settings.GITHUB_TOKEN);
        if (token == null) return null;
        Path askpass = GitHubAuth.ensureAskpass();
        if (askpass == null) return null;
        java.util.Map<String, String> env = new java.util.HashMap<>();
        env.put("GIT_ASKPASS", askpass.toString());
        env.put("LUMINA_GH_TOKEN", token);
        env.put("GIT_TERMINAL_PROMPT", "0");
        return env;
    }

    private void gitStatus() {
        if (!requireProject()) return;
        showRunPanel();
        console.clear();
        GitService.Result r = GitService.status(projectRoot);
        console.println("\u25B6 git status\n" + r.output());
    }

    private void gitNewBranch() {
        if (!requireProject()) return;
        prompt("New Branch", "Branch name:", "feature/").ifPresent(name -> {
            if (name.isBlank()) return;
            GitService.Result r = GitService.createBranch(projectRoot, name.trim());
            console.println(r.output().isBlank()
                    ? "Created and switched to '" + name.trim() + "'" : r.output().trim());
            refreshGitInfo();
        });
    }

    private void openRemote() {
        if (!requireProject()) return;
        String url = GitService.remoteBrowserUrl(projectRoot);
        if (url != null) openBrowser(url);
        else error("No remote", "This repository has no 'origin' remote yet.");
    }

    private void openBrowser(String url) {
        getHostServices().showDocument(url);
    }

    // ------------------------------------------------------------------- run

    private void refreshRunConfigs() {
        List<RunConfiguration> configs = RunConfiguration.detect(projectRoot);
        runConfigBox.getItems().setAll(configs);
        // prefer a project config over Current File when one exists
        runConfigBox.getSelectionModel().select(configs.size() > 1 ? 1 : 0);
    }

    private void runSelectedConfig() {
        RunConfiguration config = runConfigBox.getValue();
        if (config == null || config.commands() == null) {
            runCurrentFile();
            return;
        }
        showRunPanel();
        console.runSequence(config.label(), config.commands(), config.workDir());
    }

    private void runCurrentFile() {
        EditorTab tab = currentEditor();
        if (tab == null) {
            error("Nothing to run", "Open a Java file or pick a run configuration.");
            return;
        }
        saveCurrent(false);
        if (tab.getPath() == null) return;
        if (testFqcnOf(tab.getPath()) != null) {
            runCurrentTestClass();   // test classes run through the test runner
            return;
        }
        showRunPanel();

        // Inside a Maven/Gradle project: compile the project, run this class
        // on the project classpath so imports from other files resolve.
        String fqcn = fqcnOf(tab.getPath());
        if (fqcn != null && projectRoot != null) {
            List<List<String>> commands =
                    RunConfiguration.compileAndRunClass(projectRoot, fqcn);
            if (commands != null) {
                console.runSequence("Running " + fqcn, commands, projectRoot);
                return;
            }
        }
        console.runJavaFile(tab.getPath());
    }

    /** Fully-qualified class name if the file sits under src/main/java. */
    private String fqcnOf(Path file) {
        if (projectRoot == null || !file.getFileName().toString().endsWith(".java")) return null;
        Path marker = projectRoot.resolve("src/main/java").toAbsolutePath().normalize();
        Path abs = file.toAbsolutePath().normalize();
        if (!abs.startsWith(marker)) return null;
        String rel = marker.relativize(abs).toString()
                .replace(File.separatorChar, '.').replace('/', '.');
        return rel.substring(0, rel.length() - ".java".length());
    }

    private void buildProject() {
        if (!requireProject()) return;
        List<List<String>> commands = RunConfiguration.buildProject(projectRoot);
        if (commands == null) {
            error("Not a build project", "No pom.xml or build.gradle found.");
            return;
        }
        showRunPanel();
        console.runSequence("Build " + projectRoot.getFileName(), commands, projectRoot);
    }

    private void cleanProject() {
        if (!requireProject()) return;
        List<List<String>> commands = RunConfiguration.cleanProject(projectRoot);
        if (commands == null) {
            error("Not a build project", "No pom.xml or build.gradle found.");
            return;
        }
        showRunPanel();
        console.runSequence("Clean " + projectRoot.getFileName(), commands, projectRoot);
    }

    // ------------------------------------------------- blame, usages, tests

    private void toggleBlame() {
        EditorTab tab = currentEditor();
        if (tab == null || tab.getPath() == null) return;
        if (tab.hasBlame()) {
            tab.setBlame(null);
            return;
        }
        Path file = tab.getPath();
        Thread t = new Thread(() -> {
            List<GitService.BlameLine> lines = GitService.blame(file);
            Platform.runLater(() -> {
                if (lines != null) {
                    tab.setBlame(lines);
                } else {
                    console.println("Blame unavailable: " + file.getFileName()
                            + " is not tracked in a git repository yet.");
                }
            });
        }, "lumina-blame");
        t.setDaemon(true);
        t.start();
    }

    private boolean isDeclarationLine(String line, String word) {
        if (line == null || line.strip().endsWith(";")) return false;
        java.util.regex.Pattern p = Character.isUpperCase(word.charAt(0))
                ? java.util.regex.Pattern.compile("\\b(class|interface|enum|record)\\s+"
                + java.util.regex.Pattern.quote(word) + "\\b")
                : java.util.regex.Pattern.compile("[\\w>\\]]\\s+"
                + java.util.regex.Pattern.quote(word) + "\\s*\\(");
        return p.matcher(line).find();
    }

    private void showUsages(String word) {
        if (word == null || word.isBlank()) return;
        if (!requireProject()) return;
        new UsagesDialog(stage, projectRoot, word, this::openFileAtLine).show();
    }

    private void runAllTests() {
        if (!requireProject()) return;
        List<String> cmd = RunConfiguration.isMavenProject(projectRoot)
                ? RunConfiguration.maven(projectRoot, "test")
                : RunConfiguration.gradleCmd(projectRoot, "test");
        showRunPanel();
        console.runCommand("All tests — " + projectRoot.getFileName(),
                cmd, projectRoot);
    }

    private void runCurrentTestClass() {
        EditorTab tab = currentEditor();
        if (tab == null || tab.getPath() == null) {
            error("No test open", "Open a test class under src/test/java first.");
            return;
        }
        String fqcn = testFqcnOf(tab.getPath());
        if (fqcn == null) {
            error("Not a test class",
                    tab.getPath().getFileName() + " is not under src/test/java.");
            return;
        }
        saveCurrent(false);
        String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);
        List<String> cmd = RunConfiguration.isMavenProject(projectRoot)
                ? RunConfiguration.maven(projectRoot, "-Dtest=" + simple, "test")
                : RunConfiguration.gradleCmd(projectRoot, "test", "--tests", fqcn);
        showRunPanel();
        console.runCommand("Test " + simple, cmd, projectRoot);
    }

    /** FQCN when the file sits under src/test/java, else null. */
    private String testFqcnOf(Path file) {
        if (projectRoot == null || !file.getFileName().toString().endsWith(".java")) return null;
        Path marker = projectRoot.resolve("src/test/java").toAbsolutePath().normalize();
        Path abs = file.toAbsolutePath().normalize();
        if (!abs.startsWith(marker)) return null;
        String rel = marker.relativize(abs).toString()
                .replace(File.separatorChar, '.').replace('/', '.');
        return rel.substring(0, rel.length() - ".java".length());
    }

    private void generateTest() {
        EditorTab tab = currentEditor();
        if (tab == null || tab.getPath() == null || !requireProject()) return;
        String fqcn = fqcnOf(tab.getPath());
        if (fqcn == null) {
            error("Not a main class",
                    "Generate Test works for classes under src/main/java.");
            return;
        }
        String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);
        String pkg = fqcn.contains(".") ? fqcn.substring(0, fqcn.lastIndexOf('.')) : "";
        Path testDir = projectRoot.resolve("src/test/java")
                .resolve(pkg.replace('.', '/'));
        Path testFile = testDir.resolve(simple + "Test.java");
        if (Files.exists(testFile)) {
            openFile(testFile);
            return;
        }
        boolean springBoot = false;
        try {
            springBoot = Files.readString(tab.getPath()).contains("@SpringBootApplication");
        } catch (IOException ignored) {
        }
        StringBuilder body = new StringBuilder();
        if (!pkg.isEmpty()) {
            body.append("package ").append(pkg).append(";\n\n");
        }
        if (springBoot) {
            body.append("import org.junit.jupiter.api.Test;\n")
                    .append("import org.springframework.boot.test.context.SpringBootTest;\n\n")
                    .append("@SpringBootTest\n")
                    .append("class ").append(simple).append("Test {\n\n")
                    .append("    @Test\n")
                    .append("    void contextLoads() {\n    }\n}\n");
        } else {
            body.append("import org.junit.jupiter.api.Test;\n")
                    .append("import static org.junit.jupiter.api.Assertions.*;\n\n")
                    .append("class ").append(simple).append("Test {\n\n")
                    .append("    @Test\n")
                    .append("    void shouldWork() {\n")
                    .append("        // TODO: exercise ").append(simple).append("\n")
                    .append("        assertTrue(true);\n    }\n}\n");
        }
        try {
            Files.createDirectories(testDir);
            Files.writeString(testFile, body.toString());
            fileExplorer.refresh();
            openFile(testFile);
        } catch (IOException ex) {
            error("Could not create test", ex.getMessage());
        }
    }

    // --------------------------------------------------------- github auth

    private void refreshGitHubButton() {
        String user = Settings.get(Settings.GITHUB_USER);
        githubButton.setText(user != null
                ? "\uD83D\uDC64 " + user : "\uD83D\uDC64 Sign in");
    }

    private void onGitHubButton() {
        String user = Settings.get(Settings.GITHUB_USER);
        if (user == null) {
            showGitHubSignIn();
            return;
        }
        ContextMenu menu = new ContextMenu(
                item("Open github.com/" + user, null,
                        e -> openBrowser("https://github.com/" + user)),
                item("Sign out", null, e -> {
                    Settings.put(Settings.GITHUB_TOKEN, null);
                    Settings.put(Settings.GITHUB_USER, null);
                    refreshGitHubButton();
                }));
        menu.show(githubButton, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    private void showGitHubSignIn() {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Sign in to GitHub");

        Label info = new Label(
                "1. Click below: your browser opens a pre-filled GitHub"
                        + " token page (scope: repo).\n"
                        + "2. Generate the token and paste it here.\n"
                        + "Lumina uses it to authenticate push, pull and clone.");
        info.getStyleClass().add("form-static");
        info.setWrapText(true);

        Button open = new Button("Open GitHub token page in browser");
        open.getStyleClass().add("dialog-secondary");
        open.setOnAction(e -> openBrowser(GitHubAuth.TOKEN_URL));

        PasswordField tokenField = new PasswordField();
        tokenField.setPromptText("ghp_\u2026 paste token here");

        Label status = new Label(" ");
        status.getStyleClass().add("form-error");

        Button signIn = new Button("Sign in");
        signIn.getStyleClass().add("dialog-primary");
        signIn.setDefaultButton(true);
        signIn.setOnAction(e -> {
            String token = tokenField.getText().trim();
            if (token.isEmpty()) {
                status.setText("Paste a token first.");
                return;
            }
            status.setText("Verifying with api.github.com\u2026");
            signIn.setDisable(true);
            Thread t = new Thread(() -> {
                String login = GitHubAuth.validate(token);
                Platform.runLater(() -> {
                    signIn.setDisable(false);
                    if (login == null) {
                        status.setText("Token rejected \u2014 check it and try again.");
                        return;
                    }
                    Settings.put(Settings.GITHUB_TOKEN, token);
                    Settings.put(Settings.GITHUB_USER, login);
                    refreshGitHubButton();
                    console.println("\u2713 Signed in to GitHub as " + login
                            + " \u2014 push/pull/clone are now authenticated.");
                    dialog.close();
                });
            }, "lumina-gh-verify");
            t.setDaemon(true);
            t.start();
        });

        Label note = new Label(
                "Token is stored in ~/.lumina/lumina.properties on this machine.");
        note.getStyleClass().add("side-subtle");

        VBox box = new VBox(12, info, open, tokenField, signIn, status, note);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("app-root");

        Scene scene = new Scene(box, 460, 340);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    // ------------------------------------------- search everywhere & goto

    private void searchEverywhere() {
        List<SearchEverywhereDialog.Action> ideActions = List.of(
                new SearchEverywhereDialog.Action("New Project\u2026", this::showNewProjectDialog),
                new SearchEverywhereDialog.Action("Open Folder\u2026", this::openFolderDialog),
                new SearchEverywhereDialog.Action("Save", () -> saveCurrent(false)),
                new SearchEverywhereDialog.Action("Run", this::runSelectedConfig),
                new SearchEverywhereDialog.Action("Debug", this::debugSelectedConfig),
                new SearchEverywhereDialog.Action("Stop", console::stopProcess),
                new SearchEverywhereDialog.Action("Build Project", this::buildProject),
                new SearchEverywhereDialog.Action("Clean Project", this::cleanProject),
                new SearchEverywhereDialog.Action("Terminal", this::showTerminal),
                new SearchEverywhereDialog.Action("Git: Commit\u2026", this::gitCommit),
                new SearchEverywhereDialog.Action("Git: Push", () -> gitRun("Push", "push")),
                new SearchEverywhereDialog.Action("Git: Pull", () -> gitRun("Pull", "pull")),
                new SearchEverywhereDialog.Action("Git: New Branch\u2026", this::gitNewBranch),
                new SearchEverywhereDialog.Action("Find in Files\u2026", this::findInFiles),
                new SearchEverywhereDialog.Action("Go to Line\u2026", this::goToLine),
                new SearchEverywhereDialog.Action("Maven Panel", () -> showRightPanel(0)),
                new SearchEverywhereDialog.Action("Database Panel", () -> showRightPanel(1)),
                new SearchEverywhereDialog.Action("About Lumina", this::showAbout));
        new SearchEverywhereDialog(stage,
                projectRoot != null ? projectRoot : fileExplorer.getRootPath(),
                ideActions, this::openFile, this::openFileAtLine).show();
    }

    /** Heuristic go-to-declaration: types by name, methods by signature scan. */
    private void goToDeclaration(String word) {
        if (word == null || word.isBlank()) return;
        if (projectRoot == null) {
            error("No project open", "Go to Declaration needs an open project.");
            return;
        }
        // If the click is on a method in a "Qualifier.method(" call, prefer the
        // qualifier's type: that is what IntelliJ jumps to for library calls.
        EditorTab editor = currentEditor();
        String line = editor != null ? editor.currentLineText() : "";
        String qualifier = qualifierBefore(line, word);
        boolean looksLikeMethodCall = looksLikeCall(line, word);

        String typeToOpen = null;
        if (Character.isUpperCase(word.charAt(0))) {
            typeToOpen = word;                       // clicked a type directly
        } else if (qualifier != null && Character.isUpperCase(qualifier.charAt(0))) {
            typeToOpen = qualifier;                  // Type.method(...) -> open Type
        } else if (qualifier != null) {
            // instance.method(...) -> infer the variable's declared type, e.g.
            // "itemRepository" -> "ItemRepository" (fields, params, locals).
            String inferred = inferVariableType(editor, qualifier);
            if (inferred != null) typeToOpen = inferred;
        }

        final String targetType = typeToOpen;
        final Path currentFile = editor != null ? editor.getPath() : null;

        Thread t = new Thread(() -> {
            // (1) method declaration inside the project (only for real methods)
            if (!looksLikeMethodCall || qualifier == null) {
                Path[] hitFile = new Path[]{null};
                int[] hit = new int[]{-1};
                findInProject(word, currentFile, hitFile, hit);
                if (hitFile[0] != null) {
                    final Path f = hitFile[0];
                    final int ln = hit[0];
                    Platform.runLater(() -> openFileAtLine(f, ln));
                    return;
                }
            }
            // (2) a project type (the clicked type, or the call's qualifier type)
            if (targetType != null) {
                Path typeFile = findTypeFile(targetType);
                if (typeFile != null) {
                    Platform.runLater(() -> openTypeAndMaybeMethod(typeFile, word));
                    return;
                }
                // (3) library/JDK type -> decompile from the classpath (like IntelliJ)
                String fqcn = resolveImportedFqcn(currentFile, targetType);
                Platform.runLater(() -> openLibraryType(fqcn != null ? fqcn : targetType, word));
                return;
            }
            Platform.runLater(() ->
                    console.println("Declaration not found for: " + word));
        }, "lumina-goto-decl");
        t.setDaemon(true);
        t.start();
    }

    /** The token immediately before ".word" on the line, or null. */
    /**
     * Best-effort type inference for an instance qualifier: scans the current
     * file for a declaration of the variable (field, parameter, or local) and
     * returns its type's simple name. Mirrors what IntelliJ resolves precisely.
     */
    private String inferVariableType(EditorTab editor, String var) {
        if (editor == null || editor.getPath() == null) return null;
        // Common Spring pattern: a field like "private final ItemRepository itemRepository;"
        // or a constructor/method parameter "ItemRepository itemRepository".
        java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
                "\\b([A-Z][A-Za-z0-9_]*)(?:<[^>]*>)?\\s+"
                        + java.util.regex.Pattern.quote(var) + "\\b\\s*[;,)=]");
        try {
            for (String line : Files.readAllLines(editor.getPath())) {
                java.util.regex.Matcher m = decl.matcher(line);
                if (m.find()) return m.group(1);
            }
        } catch (IOException ignored) {
        }
        // Fallback: convention "xxxRepository" -> "XxxRepository".
        if (!var.isEmpty()) {
            return Character.toUpperCase(var.charAt(0)) + var.substring(1);
        }
        return null;
    }

    private String qualifierBefore(String line, String word) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "([A-Za-z_][A-Za-z0-9_]*)\\s*\\.\\s*"
                        + java.util.regex.Pattern.quote(word) + "\\b").matcher(line);
        return m.find() ? m.group(1) : null;
    }

    private boolean looksLikeCall(String line, String word) {
        return java.util.regex.Pattern.compile(
                java.util.regex.Pattern.quote(word) + "\\s*\\(").matcher(line).find();
    }

    /** Scan project java files for a method declaration named word. */
    private void findInProject(String word, Path currentFile, Path[] hitFile, int[] hit) {
        java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
                "[\\w>\\]]\\s+" + java.util.regex.Pattern.quote(word) + "\\s*\\(");
        try (Stream<Path> walk = Files.walk(projectRoot)) {
            List<Path> files = new java.util.ArrayList<>(walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !inBuildDir(p))
                    .toList());
            files.sort((a, b) -> Integer.compare(
                    declRank(a, word, currentFile), declRank(b, word, currentFile)));
            for (Path file : files) {
                List<String> lines;
                try {
                    lines = Files.readAllLines(file);
                } catch (IOException | java.io.UncheckedIOException e) {
                    continue;
                }
                for (int i = 0; i < lines.size(); i++) {
                    String ln = lines.get(i);
                    if (!decl.matcher(ln).find()) continue;
                    String trimmed = ln.strip();
                    if (trimmed.startsWith(word + "(")
                            || trimmed.contains("." + word + "(")
                            || trimmed.startsWith("return ")
                            || trimmed.endsWith(";")) continue;
                    hitFile[0] = file;
                    hit[0] = i + 1;
                    return;
                }
            }
        } catch (IOException ignored) {
        }
    }

    /** Find the .java file declaring a type of the given simple name. */
    private Path findTypeFile(String simpleType) {
        java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
                "\\b(class|interface|enum|record)\\s+"
                        + java.util.regex.Pattern.quote(simpleType) + "\\b");
        try (Stream<Path> walk = Files.walk(projectRoot)) {
            return walk.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !inBuildDir(p))
                    .filter(p -> {
                        try {
                            return decl.matcher(Files.readString(p)).find();
                        } catch (IOException | java.io.UncheckedIOException e) {
                            return false;
                        }
                    })
                    .findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private boolean inBuildDir(Path p) {
        String s = p.toString();
        return s.contains(File.separator + "target" + File.separator)
                || s.contains(File.separator + "build" + File.separator);
    }

    /** Open a project type file and jump to the method line if we can find it. */
    private void openTypeAndMaybeMethod(Path file, String word) {
        openFile(file);
        if (Character.isLowerCase(word.charAt(0))) {
            try {
                List<String> lines = Files.readAllLines(file);
                java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
                        "[\\w>\\]]\\s+" + java.util.regex.Pattern.quote(word) + "\\s*\\(");
                for (int i = 0; i < lines.size(); i++) {
                    if (decl.matcher(lines.get(i)).find()
                            && !lines.get(i).strip().endsWith(";")) {
                        final int ln = i + 1;
                        Platform.runLater(() -> {
                            EditorTab t = currentEditor();
                            if (t != null) t.goToLine(ln);
                        });
                        return;
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    /** Read imports of the current file to turn a simple type into an FQCN. */
    private String resolveImportedFqcn(Path currentFile, String simpleType) {
        if (currentFile == null) return null;
        try {
            for (String line : Files.readAllLines(currentFile)) {
                String s = line.strip();
                if (s.startsWith("import ") && s.endsWith("." + simpleType + ";")) {
                    return s.substring("import ".length(), s.length() - 1).trim();
                }
                if (!s.startsWith("import") && s.startsWith("public")) break;
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    /** Decompile a library/JDK class from the run classpath, like IntelliJ. */
    private void openLibraryType(String fqcn, String word) {
        console.println("Resolving " + fqcn + " from the classpath\u2026");
        Thread t = new Thread(() -> {
            String javap = Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name", "").toLowerCase().contains("win")
                            ? "javap.exe" : "javap").toString();
            List<String> cmd = new java.util.ArrayList<>(List.of(
                    javap, "-p", "-protected"));
            String cp = classpathFor(projectRoot);
            if (cp != null) {
                cmd.add("-classpath");
                cmd.add(cp);
            }
            cmd.add(fqcn);
            try {
                Process p = new ProcessBuilder(cmd)
                        .redirectErrorStream(true).start();
                String out = new String(p.getInputStream().readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
                p.waitFor();
                final String[] fout = new String[1];
                if (out.isBlank() || out.contains("Error:")
                        || out.contains("not found")) {
                    // Dependencies may not be copied yet: fetch them, then retry once.
                    if (RunConfiguration.isMavenProject(projectRoot)
                            && !Files.isDirectory(projectRoot.resolve("target/dependency"))) {
                        Platform.runLater(() -> console.println(
                                "Fetching dependencies so " + fqcn
                                        + " can be resolved (one-time)\u2026"));
                        try {
                            Process dep = new ProcessBuilder(RunConfiguration.maven(
                                    projectRoot, "-q",
                                    "dependency:copy-dependencies"))
                                    .directory(projectRoot.toFile())
                                    .redirectErrorStream(true).start();
                            dep.getInputStream().readAllBytes();
                            dep.waitFor();
                        } catch (IOException | InterruptedException ignored) {
                        }
                        List<String> retry = new java.util.ArrayList<>(List.of(
                                javap, "-p", "-protected"));
                        String cp2 = classpathFor(projectRoot);
                        if (cp2 != null) { retry.add("-classpath"); retry.add(cp2); }
                        retry.add(fqcn);
                        try {
                            Process p2 = new ProcessBuilder(retry)
                                    .redirectErrorStream(true).start();
                            out = new String(p2.getInputStream().readAllBytes(),
                                    java.nio.charset.StandardCharsets.UTF_8);
                            p2.waitFor();
                        } catch (IOException | InterruptedException ignored) {
                        }
                    }
                }
                if (out.isBlank() || out.contains("Error:") || out.contains("not found")) {
                    Platform.runLater(() -> console.println(
                            "Could not resolve " + fqcn
                                    + " \u2014 try Build Project first, then Ctrl+Click again."));
                    return;
                }
                fout[0] = out;
                Platform.runLater(() -> {
                    EditorTab tab = new EditorTab(
                            fqcn.substring(fqcn.lastIndexOf('.') + 1) + ".class", null);
                    tab.setEditorText("// Decompiled from classpath (javap) \u2014 read-only\n"
                            + "// " + fqcn + "\n\n" + fout[0]);
                    tab.setReadOnly();
                    addTab(tab);
                    if (Character.isLowerCase(word.charAt(0))) {
                        EditorTab cur = currentEditor();
                        if (cur != null) {
                            int idx = fout[0].indexOf(" " + word + "(");
                            if (idx >= 0) {
                                long lineNo = fout[0].substring(0, idx).chars()
                                        .filter(c -> c == '\n').count() + 4;
                                cur.goToLine((int) lineNo);
                            }
                        }
                    }
                });
            } catch (IOException | InterruptedException ex) {
                Platform.runLater(() -> console.println(
                        "javap failed: " + ex.getMessage()));
            }
        }, "lumina-javap");
        t.setDaemon(true);
        t.start();
    }

    /** Build a classpath string from the project's compiled output + deps. */
    private String classpathFor(Path root) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        Path classes = root.resolve("target/classes");
        if (Files.isDirectory(classes)) parts.add(classes.toString());
        Path gradleClasses = root.resolve("build/classes/java/main");
        if (Files.isDirectory(gradleClasses)) parts.add(gradleClasses.toString());
        // Maven dependency jars, if the local repo layout is present
        Path deps = root.resolve("target/dependency");
        if (Files.isDirectory(deps)) {
            try (Stream<Path> jars = Files.list(deps)) {
                jars.filter(p -> p.toString().endsWith(".jar"))
                        .forEach(p -> parts.add(p.toString()));
            } catch (IOException ignored) {
            }
        }
        return parts.isEmpty() ? null : String.join(File.pathSeparator, parts);
    }


    private int declRank(Path p, String word, Path currentFile) {
        if (p.equals(currentFile)) return 0;
        if (p.getFileName().toString().equals(word + ".java")) return 1;
        return 2;
    }

    // ------------------------------------------------------- debug & search

    private void debugSelectedConfig() {
        RunConfiguration config = runConfigBox.getValue();
        List<List<String>> commands = null;
        Path workDir = projectRoot;
        String label;

        if (config != null && config.commands() != null) {
            commands = config.commands();
            workDir = config.workDir();
            label = config.label();
        } else {
            EditorTab tab = currentEditor();
            if (tab == null) {
                error("Nothing to debug", "Open a Java file or pick a run configuration.");
                return;
            }
            saveCurrent(false);
            if (tab.getPath() == null) return;
            String fqcn = fqcnOf(tab.getPath());
            if (fqcn != null && projectRoot != null) {
                commands = RunConfiguration.compileAndRunClass(projectRoot, fqcn);
            }
            if (commands == null) {
                commands = List.of(List.of(RunConfiguration.javaBin(),
                        tab.getPath().toAbsolutePath().toString()));
                workDir = tab.getPath().getParent();
            }
            label = "Debug " + tab.getPath().getFileName();
        }

        showRunPanel();
        console.runSequence(label + " [debug \u2014 JDWP :5005]",
                RunConfiguration.debugify(commands), workDir);
        console.println("JVM suspends until a debugger attaches on port 5005.");
        console.println("Attaching jdb in the Terminal \u2014 useful commands: "
                + "stop in pkg.Class.method | cont | step | locals | where");

        Thread attach = new Thread(() -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                return;
            }
            Platform.runLater(() -> {
                showTerminal();
                terminal.sendCommand("jdb -attach 5005");
            });
        }, "lumina-jdb-attach");
        attach.setDaemon(true);
        attach.start();
    }

    private void findInFiles() {
        if (!requireProject()) return;
        new SearchDialog(stage, projectRoot, this::openFileAtLine).show();
    }

    private void openFileAtLine(Path path, int line) {
        openFile(path);
        Platform.runLater(() -> {
            EditorTab tab = currentEditor();
            if (tab != null) tab.goToLine(line);
        });
    }

    private void runBuildGoal(String goal) {
        if (projectRoot == null) return;
        List<String> cmd = RunConfiguration.isMavenProject(projectRoot)
                ? RunConfiguration.maven(projectRoot, goal)
                : RunConfiguration.gradleCmd(projectRoot, goal);
        showRunPanel();
        console.runCommand(goal, cmd, projectRoot);
    }

    private void gitClone() {
        prompt("Clone Repository", "Repository URL:",
                "https://github.com/user/repo.git").ifPresent(raw -> {
            String url = raw.trim();
            if (url.isEmpty()) return;
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Clone into folder");
            File parent = chooser.showDialog(stage);
            if (parent == null) return;

            String name = url.substring(url.lastIndexOf('/') + 1)
                    .replace(".git", "");
            Path target = parent.toPath().resolve(name);
            showRunPanel();
            console.runSequence("git clone " + url,
                    List.of(List.of("git", "clone", url, target.toString())),
                    parent.toPath(), gitEnv(),
                    () -> openProject(target));
        });
    }

    private void showRightPanel(int tabIndex) {
        toggleRightPanel(true);
        rightTabs.getSelectionModel().select(tabIndex);
    }

    private void toggleRightPanel(boolean show) {
        if (show && !outerSplit.getItems().contains(rightTabs)) {
            outerSplit.getItems().add(rightTabs);
            outerSplit.setDividerPositions(0.78);
        } else if (!show) {
            outerSplit.getItems().remove(rightTabs);
        }
    }

    // ---------------------------------------------------------- tool windows

    private void showRunPanel() {
        toggleBottomPanel(true);
        iconRail.setBottomSelected(true);
        bottomTabs.getSelectionModel().select(0);
    }

    private void showTerminal() {
        toggleBottomPanel(true);
        iconRail.setBottomSelected(true);
        bottomTabs.getSelectionModel().select(1);
        terminal.focusInput();
    }

    private void toggleBottomPanel(boolean show) {
        if (show && !verticalSplit.getItems().contains(bottomTabs)) {
            verticalSplit.getItems().add(bottomTabs);
            verticalSplit.setDividerPositions(0.70);
        } else if (!show) {
            verticalSplit.getItems().remove(bottomTabs);
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

    // -------------------------------------------------------------- navigate

    private void goToFile() {
        if (!requireProject()) return;
        new GoToFileDialog(stage, projectRoot, this::openFile).show();
    }

    private void goToLine() {
        EditorTab tab = currentEditor();
        if (tab == null) return;
        prompt("Go to Line", "Line number:", "1").ifPresent(text -> {
            try {
                tab.goToLine(Integer.parseInt(text.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
    }

    // ------------------------------------------------------------ status bar

    private HBox buildStatusBar() {
        breadcrumbBar = new HBox(4);
        breadcrumbBar.setAlignment(Pos.CENTER_LEFT);
        updateBreadcrumbs(null, null);

        statusCaret = new Label("");
        Label brand = new Label("Lumina 0.6");
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
        showRunPanel();
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
        refreshRunConfigs();
        refreshGitInfo();
        mavenPanel.setProject(dir);
        Settings.put(Settings.LAST_PROJECT, dir.toString());
        terminal.start(dir);

        try (Stream<Path> walk = Files.walk(dir)) {
            Optional<Path> toOpen = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("target")
                            && !p.toString().contains(File.separator + "build" + File.separator))
                    .sorted((a, b) -> Integer.compare(rank(a), rank(b)))
                    .findFirst();
            toOpen.ifPresent(this::openFile);
        } catch (IOException ignored) {
        }
    }

    private void closeProject() {
        projectRoot = null;
        editorTabs.getTabs().clear();
        fileExplorer.setRoot(null);
        projectChip.setText("No project");
        stage.setTitle("Lumina");
        refreshRunConfigs();
        refreshGitInfo();
        mavenPanel.setProject(null);
        Settings.put(Settings.LAST_PROJECT, null);
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
            writeAndOpen(dir.resolve(name + ".java"), body);
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

    private void renameSelectedFile() {
        Path selected = fileExplorer.getSelectedPath();
        if (selected == null || !Files.isRegularFile(selected)) {
            error("Nothing selected", "Select a file in the project tree first.");
            return;
        }
        prompt("Rename File", "New name:", selected.getFileName().toString()).ifPresent(raw -> {
            String name = raw.trim();
            if (name.isEmpty()) return;
            try {
                Path target = selected.resolveSibling(name);
                Files.move(selected, target);
                editorTabs.getTabs().removeIf(t ->
                        t instanceof EditorTab et && selected.equals(et.getPath()));
                fileExplorer.refresh();
                openFile(target);
            } catch (IOException ex) {
                error("Could not rename", ex.getMessage());
            }
        });
    }

    private Path targetDirectory() {
        Path selected = fileExplorer.getSelectedPath();
        if (selected != null) {
            return Files.isDirectory(selected) ? selected : selected.getParent();
        }
        Path root = fileExplorer.getRootPath();
        if (root == null) {
            error("No folder open",
                    "Open or create a project first (File \u2192 New \u2192 Project\u2026).");
            return null;
        }
        return root;
    }

    private String inferPackage(Path dir) {
        Path abs = dir.toAbsolutePath().normalize();
        Path base = fileExplorer.getRootPath();
        if (base == null) return "";
        for (String r : new String[]{"src/main/java", "src/test/java"}) {
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
        if (path.getFileName().toString().endsWith(".class")) {
            openClassFile(path);
            return;
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

    /** Disassemble a .class file with javap and show it read-only. */
    private void openClassFile(Path path) {
        try {
            String javap = Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name", "").toLowerCase().contains("win")
                            ? "javap.exe" : "javap").toString();
            Process p = new ProcessBuilder(javap, "-p", "-c", path.toString())
                    .redirectErrorStream(true)
                    .start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor();
            EditorTab tab = new EditorTab(path.getFileName().toString(), path);
            tab.setEditorText("// Decompiled with javap \u2014 read-only\n\n" + out);
            tab.setReadOnly();
            addTab(tab);
        } catch (IOException ex) {
            error("Could not disassemble class", ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void addTab(EditorTab tab) {
        tab.setNavigationHandler(word -> {
            // Ctrl+Click on a declaration -> usages; on a usage -> declaration.
            EditorTab current = currentEditor();
            if (current != null && word != null && isDeclarationLine(
                    current.currentLineText(), word)) {
                showUsages(word);
            } else {
                goToDeclaration(word);
            }
        });
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
        alert.setHeaderText("Lumina IDE 0.6");
        alert.setContentText("""
                A luminous, lightweight Java IDE.
                Built with Java 25, JavaFX and Maven.

                Phase 6: git blame annotations, Find Usages,
                locate-in-tree, and JUnit test running & generation.""");
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