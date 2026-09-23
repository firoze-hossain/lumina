package dev.lumina;

import dev.lumina.git.GitHubAccountManager;
import dev.lumina.git.GitHubAuth;
import dev.lumina.git.GitLabAccountManager;
import dev.lumina.git.GitService;
import dev.lumina.project.ProjectGenerator;
import dev.lumina.project.ProjectSpec;
import dev.lumina.project.RecentProjectsManager;
import dev.lumina.run.RunConfiguration;
import dev.lumina.notification.Notification;
import dev.lumina.notification.NotificationService;
import dev.lumina.notification.NotificationType;
import dev.lumina.semantics.Docs;
import dev.lumina.settings.SystemSettings;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * Lumina IDE — Phase 3.
 * Full IntelliJ-style menu bar, Run/Terminal tool windows, Git integration
 * with branch switching, smart run (Java file / Maven / Gradle / Spring
 * Boot), Go to File, and .class viewing via javap.
 */
public class LuminaApp extends Application {

    /** Global registry of all active LuminaApp window instances. */
    public static final List<LuminaApp> ACTIVE_INSTANCES = new CopyOnWriteArrayList<>();

    private Stage stage;
    private TabPane editorTabs;
    private StackPane editorArea;
    private javafx.scene.Node editorRoot;
    private final List<TabPane> editorGroups = new java.util.ArrayList<>();
    private TabPane activeEditorGroup;
    private WelcomeView welcomeView;
    private FileExplorer fileExplorer;
    private ConsolePane console;
    private ConsolePane buildConsole;
    private ServicesPanel servicesPanel;
    private McpLogPanel mcpLogPanel;
    private Tab mcpBottomTab;
    private Tab mcpRightTab;
    private Tab mcpLeftTab;
    private TerminalToolWindow terminal;
    private TabPane bottomTabs;
    private TabPane rightTabs;
    private BorderPane rightDock;
    private TabPane leftTabs;
    private BorderPane leftDock;
    private CommitPanel commitPanel;
    private PullRequestsPanel pullRequestsPanel;
    private StructurePanel structurePanel;
    private GitLogPanel gitLogPanel;
    private SplitPane outerSplit;
    private MavenPanel mavenPanel;
    private DatabasePanel dbPanel;
    private SplitPane verticalSplit;
    private SplitPane horizontalSplit;
    private IconRail iconRail;
    private Button projectChip;
    private dev.lumina.ui.ProjectWidgetPopup projectWidgetPopup;
    private Button branchButton;
    private GitBranchesPopup gitBranchesPopup;
    private VcsOperationsPopup vcsOperationsPopup;
    private Button githubButton;
    private ComboBox<RunConfiguration> runConfigBox;
    private Button runButton;
    private Button stopButton;
    private RightToolRail rightRail;
    private Label rightToolTitle;
    private NotificationsToolWindowPanel notificationsPanel;
    private TestResultsPanel testsPanel;
    private long testRunStart;
    private Runnable lastTestRun;
    private volatile dev.lumina.semantics.SemanticEngine semantics;
    private final ProblemsPanel problemsPanel = new ProblemsPanel();
    private final DocPopup docPopup = new DocPopup();
    private final ParamInfoPopup paramPopup = new ParamInfoPopup();
    private UsagesPopup usagesPopup;
    private Label statusProblems;
    private HBox breadcrumbBar;
    private Label statusCaret;
    private HBox mavenProgressRow;
    private Label mavenProgressLabel;
    private ProgressBar mavenProgressBarNode;
    private HBox gitProgressBox;
    private Label gitProgressLabel;
    private ProgressBar gitProgressBar;
    private Button gitProgressCancel;
    private Process activeGitTaskProcess;
    /** pom.xml/build.gradle text as of the last successful dependency
     *  resolve; every open build-file tab is compared against this live. */
    private volatile String mavenSyncBaselineText;

    private Path projectRoot;
    private Path pendingProjectToOpen;
    private int untitledCounter = 1;
    private long lastShiftPress;

    public Path getProjectRoot() {
        return projectRoot;
    }

    public Stage getStage() {
        return stage;
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(new VBox(buildMenuBar(), buildToolBar()));

        iconRail = new IconRail(this::onLeftRailSelect, this::showMoreToolWindows,
                this::onBottomRailSelect);
        root.setLeft(iconRail);

        fileExplorer = new FileExplorer(this::openFile, () -> {
            EditorTab tab = currentEditor();
            return tab != null ? tab.getPath() : null;
        });
        fileExplorer.setActions(
                file -> { openFile(file); Platform.runLater(this::runCurrentFile); },
                file -> { openFile(file); Platform.runLater(this::runCurrentTestClass); },
                this::deleteFromTree);
        fileExplorer.setExtendedActions(
                this::renameFile,
                this::newJavaClass,
                this::newPackage,
                this::newFile,
                this::newDirectory,
                this::copyPathToClipboard,
                p -> showComingSoon("Open Module Settings"),
                this::showComingSoon);
        fileExplorer.setCreationHandlers(createNewMenuHandlers());

        editorTabs = new TabPane();
        editorGroups.add(editorTabs);
        activeEditorGroup = editorTabs;
        editorRoot = editorTabs;
        wireEditorGroupTracking(editorTabs);

        welcomeView = new WelcomeView(
                this::showNewProjectDialog, this::newFile,
                this::openFileDialog, this::openFolderDialog);
        editorArea = new StackPane(welcomeView, editorRoot);

        // bottom tool windows: Run + Terminal
        console = new ConsolePane("Run");
        buildConsole = new ConsolePane("Build");
        servicesPanel = new ServicesPanel();
        mcpLogPanel = new McpLogPanel(() -> showComingSoon("Edit Config"));
        terminal = new TerminalToolWindow(() -> projectRoot, this::openTerminalSettings,
                this::hideTerminalPanel, this::hideTerminalPanel,
                this::moveTerminalTabToEditor);
        terminal.setOnMaximize(this::toggleMaximizeBottomPanel);

        console.setOnHideToolWindow(() -> toggleBottomPanel(false));
        console.setOnMaximizeToolWindow(this::toggleMaximizeBottomPanel);

        buildConsole.setOnHideToolWindow(() -> toggleBottomPanel(false));
        buildConsole.setOnMaximizeToolWindow(this::toggleMaximizeBottomPanel);

        servicesPanel.setOnHideToolWindow(() -> toggleBottomPanel(false));
        servicesPanel.setOnMaximizeToolWindow(this::toggleMaximizeBottomPanel);

        mcpBottomTab = toolTab("GitHub Copilot MCP Log", mcpLogPanel);
        mcpLogPanel.setOnHideToolWindow(this::hideMcpPanel);
        mcpLogPanel.setOnMaximizeToolWindow(this::toggleMaximizeBottomPanel);
        mcpLogPanel.setOnMoveToToolWindow(this::moveMcpPanel);

        testsPanel = new TestResultsPanel();
        testsPanel.setNavigator(this::openTestSource);
        testsPanel.setHandlers(
                () -> { if (lastTestRun != null) lastTestRun.run(); },
                this::rerunFailedTests);
        bottomTabs = new TabPane(
                toolTab("Run", console),
                toolTab("Tests", testsPanel),
                toolTab("Problems", problemsPanel),
                toolTab("Terminal", terminal),
                toolTab("Git", gitLogPanel = new GitLogPanel(() -> projectRoot)),
                toolTab("Build", buildConsole),
                toolTab("Services", servicesPanel),
                mcpBottomTab);
        gitLogPanel.setOnOpenFileDiff(this::openGitCommitFileDiff);
        gitLogPanel.setOnOpenFileInEditor(this::openFile);
        gitLogPanel.setOnHideToolWindow(() -> toggleBottomPanel(false));
        gitLogPanel.setOnMaximizeToolWindow(this::toggleMaximizeBottomPanel);
        gitLogPanel.setOnMergeBranch(this::showMergeDialog);
        gitLogPanel.setOnRebaseBranch(this::showRebaseDialog);

        problemsPanel.setOnHideToolWindow(() -> toggleBottomPanel(false));
        problemsPanel.setOnMaximizeToolWindow(this::toggleMaximizeBottomPanel);
        problemsPanel.setOnJump(line -> {
            EditorTab editor = currentEditor();
            if (editor != null) editor.goToLine(line);
        });
        // Modern IDE button states: stop is red only while running, and
        // the Run button turns into Rerun in the exact same toolbar slot
        // while something from this session is already running.
        console.setOnRunningChanged(running -> {
            stopButton.setDisable(!running);
            stopButton.getStyleClass().remove("stop-active");
            if (running) stopButton.getStyleClass().add("stop-active");

            runButton.setText(running ? "\u27F3" : "\u25B6");
            runButton.getStyleClass().removeAll("tool-run", "tool-restart");
            runButton.getStyleClass().add(running ? "tool-restart" : "tool-run");
            runButton.setTooltip(new Tooltip(running
                    ? "Rerun \u2014 restart the last run (Ctrl/Cmd+R)"
                    : "Run selected configuration (Ctrl/Cmd+R)"));
        });
        bottomTabs.getStyleClass().add("tool-tabs");
        bottomTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        verticalSplit = new SplitPane(editorArea, bottomTabs);
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.setDividerPositions(0.70);

        // Left tool windows, selected from the top group of the icon rail:
        // Project / Commit / Pull Requests / Structure, one at a time, no
        // tab-header strip of its own (mirrors the right dock below).
        commitPanel = new CommitPanel(() -> projectRoot, console::println);
        commitPanel.setOnOpenFile(this::openFile);
        commitPanel.setOnOpenDiff(this::openDiffViewer);
        commitPanel.setOnHide(() -> toggleLeftPanel(false));
        commitPanel.setOnPush(this::showPushDialog);
        pullRequestsPanel = new PullRequestsPanel(() -> projectRoot,
                () -> Settings.get(Settings.GITHUB_TOKEN),
                () -> Settings.get(Settings.GITHUB_USER),
                this::showGitHubSignIn, this::openBrowser);
        structurePanel = new StructurePanel();
        structurePanel.setOnJumpToLine(line -> {
            EditorTab editor = currentEditor();
            if (editor != null) editor.goToLine(line);
        });

        leftTabs = new TabPane(
                toolTab("Project", fileExplorer),
                toolTab("Commit", commitPanel),
                toolTab("Pull Requests", pullRequestsPanel),
                toolTab("Structure", structurePanel));
        leftTabs.getStyleClass().addAll("tool-tabs", "right-tabs", "left-tabs");
        leftTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        leftTabs.getSelectionModel().select(0);

        Label leftToolTitle = new Label("Project");
        leftToolTitle.getStyleClass().add("right-tool-title");
        Button leftToolClose = new Button("\u2715");
        leftToolClose.getStyleClass().add("right-tool-close");
        leftToolClose.setTooltip(new Tooltip("Hide"));
        leftToolClose.setOnAction(e -> toggleLeftPanel(false));
        Region leftTitleSpacer = new Region();
        HBox.setHgrow(leftTitleSpacer, Priority.ALWAYS);
        HBox leftToolTitleBar = new HBox(leftToolTitle, leftTitleSpacer, leftToolClose);
        leftToolTitleBar.getStyleClass().add("right-tool-titlebar");
        leftToolTitleBar.setAlignment(Pos.CENTER_LEFT);
        leftTabs.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            int selectedIdx = leftTabs.getSelectionModel().getSelectedIndex();
            if (selectedIdx == 1) { // Commit
                leftDock.setTop(null);
            } else {
                leftDock.setTop(leftToolTitleBar);
                leftToolTitle.setText(sel != null ? sel.getText() : "");
            }
            refreshLeftPanel();
        });

        leftDock = new BorderPane(leftTabs);
        leftDock.setTop(leftToolTitleBar);
        leftDock.getStyleClass().add("right-tool-dock");
        leftDock.setMinWidth(230);
        SplitPane.setResizableWithParent(leftDock, false);

        horizontalSplit = new SplitPane(leftDock, verticalSplit);
        horizontalSplit.setDividerPositions(0.22);
        iconRail.selectTop(0);

        // Right tool windows, selected from a compact IntelliJ-style vertical rail.
        mavenPanel = new MavenPanel(this::runBuildGoal, this::openFile);
        dbPanel = new DatabasePanel();
        notificationsPanel = new NotificationsToolWindowPanel();
        rightTabs = new TabPane(
                toolTab("Notifications", notificationsPanel),
                toolTab("AI Chat", rightPlaceholder("AI Chat", "Multiline code completion\n\nCode generation in the editor\n\nStart a new chat to ask Lumina for help.")),
                toolTab("Database", dbPanel), toolTab("Maven", mavenPanel),
                toolTab("Services", rightPlaceholder("Services", "No services are running")),
                toolTab("GitHub Copilot", rightPlaceholder("GitHub Copilot", "Ask Copilot\n\nAI assistance is ready when GitHub Copilot is connected.")));
        // No tab-header strip here: which panel shows is driven entirely by
        // rightRail below, exactly like IntelliJ's own tool windows, which
        // never show a row of text tabs above their content.
        rightTabs.getStyleClass().addAll("tool-tabs", "right-tabs");
        rightTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        rightTabs.getSelectionModel().select(3);

        rightToolTitle = new Label();
        rightToolTitle.getStyleClass().add("right-tool-title");

        Button rightToolOptions = new Button("⋮");
        rightToolOptions.getStyleClass().add("right-tool-close");
        rightToolOptions.setTooltip(new Tooltip("Options"));
        rightToolOptions.setOnAction(e -> {
            ContextMenu optMenu = new ContextMenu();
            optMenu.getStyleClass().add("git-tool-options-menu");
            optMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-padding: 4 0;");

            Menu viewModeMenu = new Menu("View Mode");
            for (String mode : new String[]{"Dock Pinned", "Dock Unpinned", "Undock", "Float", "Window"}) {
                CheckMenuItem mi = new CheckMenuItem(mode);
                mi.setSelected("Dock Pinned".equals(mode));
                viewModeMenu.getItems().add(mi);
            }

            Menu moveToMenu = new Menu("Move to");
            for (String pos : new String[]{"Right", "Left", "Bottom"}) {
                CheckMenuItem mi = new CheckMenuItem(pos);
                mi.setSelected("Right".equals(pos));
                moveToMenu.getItems().add(mi);
            }

            MenuItem removeSidebar = new MenuItem("Remove from Sidebar");
            removeSidebar.setOnAction(ev -> toggleRightPanel(false));

            optMenu.getItems().addAll(viewModeMenu, moveToMenu, new SeparatorMenuItem(), removeSidebar);
            optMenu.show(rightToolOptions, javafx.geometry.Side.BOTTOM, -100, 0);
        });

        Button rightToolClose = new Button("—");
        rightToolClose.getStyleClass().add("right-tool-close");
        rightToolClose.setTooltip(new Tooltip("Hide ⇧⎋"));
        rightToolClose.setOnAction(e -> toggleRightPanel(false));
        Region rightTitleSpacer = new Region();
        HBox.setHgrow(rightTitleSpacer, Priority.ALWAYS);
        HBox rightToolTitleBar = new HBox(6, rightToolTitle, rightTitleSpacer, rightToolOptions, rightToolClose);
        rightToolTitleBar.getStyleClass().add("right-tool-titlebar");
        rightToolTitleBar.setAlignment(Pos.CENTER_LEFT);
        rightTabs.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> rightToolTitle.setText(sel != null ? sel.getText() : ""));
        rightToolTitle.setText(rightTabs.getSelectionModel().getSelectedItem().getText());

        rightDock = new BorderPane(rightTabs);
        rightDock.setTop(rightToolTitleBar);
        rightDock.getStyleClass().add("right-tool-dock");
        rightDock.setMinWidth(300);
        SplitPane.setResizableWithParent(rightDock, false);

        // The icon rail itself is a permanent fixture flush against the
        // window's right edge \u2014 outside the split \u2014 so it stays put and
        // visible even while the docked tool window (rightDock) is closed,
        // matching IntelliJ instead of disappearing along with the panel.
        rightRail = new RightToolRail(this::onRightRailSelect);
        root.setRight(rightRail);

        // Starts closed \u2014 a fresh IntelliJ session doesn't force a tool
        // window open on launch, so rightDock only gets added to the split
        // once the user actually opens it via the rail (see
        // onRightRailSelect/showRightPanel).
        outerSplit = new SplitPane(horizontalSplit);
        outerSplit.setDividerPositions(0.74);

        VBox toastContainer = new VBox(8);
        toastContainer.setAlignment(Pos.BOTTOM_RIGHT);
        toastContainer.setPadding(new Insets(0, 16, 16, 0));
        toastContainer.setPickOnBounds(false);

        StackPane centerStack = new StackPane(outerSplit, toastContainer);
        StackPane.setAlignment(toastContainer, Pos.BOTTOM_RIGHT);

        NotificationService.getInstance().addToastListener(notification -> {
            NotificationBalloonToast toast = new NotificationBalloonToast(
                    notification,
                    () -> showRightPanel(0),
                    () -> toastContainer.getChildren().removeIf(node -> node.getUserData() == notification)
            );
            toast.setUserData(notification);
            toastContainer.getChildren().add(toast);
        });

        root.setCenter(centerStack);
        root.setBottom(buildBottomArea());
        updateEditorVisibility();

        Scene scene = new Scene(root, 1400, 860);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());

        // Key shortcuts
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            // Alt+` -> VCS Operations Popup (matching IntelliJ IDEA)
            if (e.isAltDown() && !e.isControlDown() && !e.isMetaDown() &&
                    (e.getCode() == javafx.scene.input.KeyCode.BACK_QUOTE || "`".equals(e.getText()))) {
                showVcsOperationsPopup();
                e.consume();
                return;
            }

            // IntelliJ-style double-Shift -> Search Everywhere
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
        ACTIVE_INSTANCES.add(this);

        SystemSettings.getInstance().apply();

        stage.setOnCloseRequest(e -> {
            if (SystemSettings.getInstance().isConfirmExit() && ACTIVE_INSTANCES.size() == 1) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Exit Lumina");
                alert.setHeaderText("Confirm Exit");
                alert.initOwner(stage);

                CheckBox dontAsk = new CheckBox("Do not ask again");
                dontAsk.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 8 0 0 0;");
                Label msg = new Label("Are you sure you want to exit Lumina?");
                msg.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                VBox content = new VBox(10, msg, dontAsk);
                alert.getDialogPane().setContent(content);

                Optional<ButtonType> res = alert.showAndWait();
                if (res.isPresent() && res.get() == ButtonType.OK) {
                    if (dontAsk.isSelected()) {
                        SystemSettings.getInstance().setConfirmExit(false);
                        SystemSettings.getInstance().save();
                    }
                    closeWindow();
                } else {
                    e.consume();
                }
            } else {
                closeWindow();
            }
        });

        stage.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused && SystemSettings.getInstance().isSaveOnFocusLost()) {
                saveAllEditors();
            }
        });

        stage.show();

        terminal.start(Path.of(System.getProperty("user.home")));
        refreshRunConfigs();
        refreshGitInfo();

        if (pendingProjectToOpen != null) {
            // this window was opened via "New Window" for a specific project
            openProject(pendingProjectToOpen);
        } else if (SystemSettings.getInstance().isReopenProjectsOnStartup()) {
            // reopen the last project unless it was explicitly closed
            String last = Settings.get(Settings.LAST_PROJECT);
            if (last != null && Files.isDirectory(Path.of(last))) {
                openProject(Path.of(last));
            }
        }

        wireEditorGroupSelection(editorTabs);
    }

    /** Style, closing policy, and the two structural behaviors every editor
     *  group needs: hide/show the welcome screen when the LAST group empties
     *  out, and collapse a split away once its own tabs are all closed. */
    private void wireEditorGroupTracking(TabPane group) {
        group.getStyleClass().add("editor-tabs");
        group.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        group.getTabs().addListener((javafx.collections.ListChangeListener<Tab>) c -> {
            updateEditorVisibility();
            // editorTabs is never auto-collapsed away, even if it empties
            // out while a split exists \u2014 several other methods (groupOf's
            // fallback, collapseAllSplits) assume it always exists in the
            // tree; better to leave it sitting empty in that rare case than
            // to break that assumption.
            if (group.getTabs().isEmpty() && group != editorTabs && editorGroups.size() > 1) {
                closeEditorGroup(group);
            }
        });
    }

    private List<Tab> allEditorTabs() {
        List<Tab> all = new java.util.ArrayList<>();
        for (TabPane group : editorGroups) all.addAll(group.getTabs());
        return all;
    }

    /** Which group currently holds this tab \u2014 falls back to the main
     *  group only if the tab isn't found anywhere (shouldn't normally
     *  happen, but a hard NPE here would be worse than a wrong-but-safe
     *  default). */
    private TabPane groupOf(Tab tab) {
        for (TabPane group : editorGroups) {
            if (group.getTabs().contains(tab)) return group;
        }
        return editorTabs;
    }

    /** Attaches the "this group is now active, and here's what to do when
     *  its selection changes" behavior to an editor group \u2014 called once
     *  for the main {@link #editorTabs} here, and again for every new split
     *  group created later (see {@link #splitEditorGroup}). */
    private void wireEditorGroupSelection(TabPane group) {
        group.getSelectionModel().selectedItemProperty().addListener((obs, old, tab) -> {
            activeEditorGroup = group;
            onEditorSelectionChanged(tab);
        });
    }

    private void onEditorSelectionChanged(Tab tab) {
        if (tab instanceof EditorTab et) {
            updateBreadcrumbs(et.getPath(), et.getText());
            et.setCaretListener((line, col) -> {
                updateCaretStatus(line, col);
                trackParamInfoCaret(et);
            });
            et.focusEditor();
            problemsPanel.show(et.getDiagnostics());
            updateProblemsStatus(et.getDiagnostics());
            if (leftTabs.getSelectionModel().getSelectedIndex() == 3) refreshStructurePanel();
        } else {
            updateBreadcrumbs(null, null);
            statusCaret.setText("");
            problemsPanel.show(List.of());
            updateProblemsStatus(List.of());
            if (leftTabs.getSelectionModel().getSelectedIndex() == 3) refreshStructurePanel();
        }
    }

    private Tab toolTab(String name, javafx.scene.Node content) {
        Tab t = new Tab(name, content);
        t.setClosable(false);
        return t;
    }

    private javafx.scene.Node rightPlaceholder(String title, String message) {
        Label heading = new Label(title);
        heading.getStyleClass().add("right-tool-heading");
        Label body = new Label(message);
        body.getStyleClass().add("right-tool-empty");
        body.setWrapText(true);
        VBox panel = new VBox(18, heading, body);
        panel.setPadding(new Insets(14));
        panel.getStyleClass().add("side-panel");
        return panel;
    }

    // ------------------------------------------------------------------ menus

    private MenuBar buildMenuBar() {
        // ---- File
        Menu newMenu = new Menu("New");
        Runnable refreshNewMenu = () -> {
            Path target = activeTargetPath();
            boolean isRoot = target != null && (
                    (projectRoot != null && target.toAbsolutePath().normalize().equals(projectRoot.toAbsolutePath().normalize()))
                    || (fileExplorer != null && fileExplorer.getRootPath() != null && target.toAbsolutePath().normalize().equals(fileExplorer.getRootPath().toAbsolutePath().normalize()))
                    || NewMenuBuilder.isProjectRoot(target)
            );
            NewMenuBuilder.populateNewMenu(newMenu, target, false, isRoot, createNewMenuHandlers());
        };
        newMenu.setOnShowing(e -> refreshNewMenu.run());
        refreshNewMenu.run();

        Menu openRecentProjectMenu = new Menu("Open Recent Project");
        Runnable refreshRecentProjects = () -> {
            openRecentProjectMenu.getItems().clear();
            List<RecentProjectsManager.RecentProject> recents =
                    RecentProjectsManager.getInstance().getRecentProjects();
            if (recents.isEmpty()) {
                MenuItem none = new MenuItem("No recent projects");
                none.setDisable(true);
                openRecentProjectMenu.getItems().add(none);
            } else {
                for (RecentProjectsManager.RecentProject rp : recents) {
                    String label = RecentProjectsManager.getInstance()
                            .getDisplayLabel(rp, recents);
                    MenuItem mi = new MenuItem(label);
                    mi.setOnAction(e -> openProjectInteractive(Path.of(rp.path())));
                    openRecentProjectMenu.getItems().add(mi);
                }
                openRecentProjectMenu.getItems().add(new SeparatorMenuItem());
                MenuItem clearList = new MenuItem("Clear List");
                clearList.setOnAction(e -> {
                    RecentProjectsManager.getInstance().clear();
                    openRecentProjectMenu.getItems().clear();
                    MenuItem none = new MenuItem("No recent projects");
                    none.setDisable(true);
                    openRecentProjectMenu.getItems().add(none);
                });
                openRecentProjectMenu.getItems().add(clearList);
            }
        };
        openRecentProjectMenu.setOnShowing(e -> refreshRecentProjects.run());
        refreshRecentProjects.run();

        MenuItem closeProjectItem = item("Close Project", null, e -> closeProject());
        MenuItem closeAllProjectsItem = item("Close All Projects", null, e -> closeAllProjects());
        MenuItem closeOtherProjectsItem = item("Close Other Projects", null, e -> closeOtherProjects());

        Menu fileProperties = new Menu("File Properties");
        fileProperties.getItems().addAll(
                item("File Encoding", null, e -> showInfo("File Encoding", "UTF-8")),
                item("Line Separators", null, e -> showInfo("Line Separators", "LF")));
        Menu localHistory = new Menu("Local History");
        localHistory.getItems().addAll(
                item("Show History", null, e -> showInfo("Local History", "No local changes recorded yet.")),
                item("Put Label…", null, e -> showInfo("Local History", "Labels will be available in a future update.")));
        Menu manageSettings = new Menu("Manage IDE Settings");
        manageSettings.getItems().addAll(
                item("Restore Default Settings…", null, e -> showInfo("Settings", "Default settings restored.")),
                item("Reset \"Open Project\" Prompt", null, e -> {
                    Settings.put(Settings.OPEN_PROJECT_MODE, null);
                    console.println("\u2713 The Open Project prompt (Cancel / New "
                            + "Window / This Window) will ask again next time.");
                }),
                item("Import Settings…", null, e -> showInfo("Settings", "Import settings is not available yet.")),
                item("Export Settings…", null, e -> showInfo("Settings", "Export settings is not available yet.")));
        Menu newProjectsSetup = new Menu("New Projects Setup");
        newProjectsSetup.getItems().addAll(
                item("Settings for New Projects…", null, e -> showInfo("New Projects Setup", "New project defaults are configured in the project wizard.")),
                item("Structure for New Projects…", null, e -> showInfo("New Projects Setup", "New project structure is not configurable yet.")));

        CheckMenuItem powerSave = new CheckMenuItem("Power Save Mode");
        powerSave.setOnAction(e -> console.println(powerSave.isSelected()
                ? "Power Save Mode enabled" : "Power Save Mode disabled"));

        Menu file = new Menu("File");
        file.setOnShowing(e -> {
            refreshNewMenu.run();
            refreshRecentProjects.run();
            boolean hasProject = projectRoot != null;
            boolean hasOtherProjects = ACTIVE_INSTANCES.stream()
                    .anyMatch(a -> a != this && a.projectRoot != null);
            boolean hasAnyProject = hasProject || hasOtherProjects;

            closeProjectItem.setDisable(!hasProject);
            closeAllProjectsItem.setDisable(!hasAnyProject);
            closeOtherProjectsItem.setDisable(!hasOtherProjects);
        });

        file.getItems().addAll(
                newMenu,
                item("Open…", "Shortcut+O", e -> openFolderDialog()),
                openRecentProjectMenu,
                closeProjectItem,
                closeAllProjectsItem,
                closeOtherProjectsItem,
                item("Remote Development…", null, e -> new RemoteDevelopmentDialog(stage).show()),
                new SeparatorMenuItem(),
                item("Settings…", "Shortcut+Alt+S", e -> new SettingsDialog(stage).show()),
                item("Project Structure…", projectStructureShortcut(), e -> openProjectStructure()),
                fileProperties,
                localHistory,
                new SeparatorMenuItem(),
                item("Save All", "Shortcut+S", e -> saveAllEditors()),
                item("Reload All from Disk", "Shortcut+Alt+Y", e -> reloadAllFromDisk()),
                item("Repair IDE", null, e -> showInfo("Repair IDE", "The project indexes and tool windows are healthy.")),
                //  item("Invalidate Caches…", null, e -> showInfo("Invalidate Caches", "Caches will be rebuilt the next time a project opens.")),
                // In buildMenuBar(), find the Invalidate Caches item:
                item("Invalidate Caches…", null, e -> {
                    new InvalidateCachesDialog(stage, () -> {
                        // Restart logic - close and reopen the IDE
                        Platform.runLater(() -> {
                            try {
                                // Save current state
                                Settings.put(Settings.LAST_PROJECT, projectRoot != null ? projectRoot.toString() : null);
                                // Restart the application
                                Stage currentStage = (Stage) stage.getScene().getWindow();
                                currentStage.close();
                                // Re-launch
                                new LuminaApp().start(new Stage());
                            } catch (Exception ex) {
                                showInfo("Restart", "Please restart Lumina manually to complete cache invalidation.");
                            }
                        });
                    }).show();
                }),
                new SeparatorMenuItem(),
                manageSettings,
                newProjectsSetup,
                item("Save File as Template…", null, e -> showInfo("Save File as Template", "File templates are not available yet.")),
                new SeparatorMenuItem(),
                item("Export", null, e -> showInfo("Export", "Export is not available yet.")),
                //item("Print…", null, e -> showInfo("Print", "Printing is not available yet.")),
                // In buildMenuBar(), find the Print item:
                item("Print…", null, e -> {
                    EditorTab tab = currentEditor();
                    Path filePath = tab != null ? tab.getPath() : null;
                    new PrintDialog(stage, filePath).show();
                }),
                new SeparatorMenuItem(),
                powerSave,
                new SeparatorMenuItem(),
                item("Exit", null, e -> Platform.exit()));

        // ---- Edit
        Menu pasteMenu = new Menu("Paste");
        pasteMenu.getItems().addAll(
                item("Paste", "Shortcut+V", e -> withEditor(EditorTab::paste)),
                item("Paste from History…", null, e -> showInfo("Paste from History", "Clipboard history is empty.")));
        Menu findMenu = new Menu("Find");
        findMenu.getItems().addAll(
                item("Find…", "Shortcut+F", e -> findInFiles()),
                item("Replace…", "Shortcut+R", e -> showInfo("Replace", "Replace in the current file is not available yet.")),
                item("Find in Files…", "Shortcut+Shift+F", e -> findInFiles()));
        Menu usagesMenu = new Menu("Find Usages");
        usagesMenu.getItems().addAll(
                item("Find Usages", "Alt+F7", e -> {
                    EditorTab tab = currentEditor();
                    if (tab != null) showUsages(tab.wordAtCaret());
                }),
                item("Find Usages Settings…", null, e -> showInfo("Find Usages", "Usage search settings are not available yet.")));
        Menu convertIndents = new Menu("Convert Indents");
        convertIndents.getItems().addAll(
                item("To Spaces", null, e -> showInfo("Convert Indents", "Indent conversion is not available yet.")),
                item("To Tabs", null, e -> showInfo("Convert Indents", "Indent conversion is not available yet.")));
        Menu macros = new Menu("Macros");
        macros.getItems().addAll(item("Start Macro Recording", null,
                e -> showInfo("Macros", "Macro recording is not available yet.")));
        Menu bookmarks = new Menu("Bookmarks");
        bookmarks.getItems().addAll(item("Toggle Bookmark", "F11",
                e -> showInfo("Bookmarks", "Bookmarks are not available yet.")));

        Menu edit = new Menu("Edit");
        edit.getItems().addAll(
                item("Undo", "Shortcut+Z", e -> withEditor(EditorTab::undo)),
                item("Redo", "Shortcut+Shift+Z", e -> withEditor(EditorTab::redo)),
                new SeparatorMenuItem(),
                item("Cut", "Shortcut+X", e -> withEditor(EditorTab::cut)),
                item("Copy", "Shortcut+C", e -> withEditor(EditorTab::copy)),
                item("Copy Path/Reference…", null, e -> showInfo("Copy Path/Reference", "No file is selected.")),
                pasteMenu,
                item("Delete", "Delete", e -> withEditor(EditorTab::cut)),
                new SeparatorMenuItem(),
                disabled("Search In Selection"),
                findMenu,
                usagesMenu,
                new SeparatorMenuItem(),
                item("Column Selection Mode", "Alt+Shift+Insert", e -> showInfo("Column Selection", "Column selection is not available yet.")),
                item("Select All", "Shortcut+A", e -> withEditor(EditorTab::selectAll)),
                item("Add Carets to Ends of Selected Lines", "Alt+Shift+G", e -> showInfo("Multiple Carets", "Multiple carets are not available yet.")),
                item("Extend Selection", "Shortcut+W", e -> showInfo("Selection", "Selection expansion is not available yet.")),
                item("Shrink Selection", "Shortcut+Shift+W", e -> showInfo("Selection", "Selection expansion is not available yet.")),
                new SeparatorMenuItem(),
                item("Toggle Case", "Shortcut+Shift+U", e -> showInfo("Toggle Case", "Case conversion is not available yet.")),
                item("Join Lines", "Shortcut+Shift+J", e -> showInfo("Join Lines", "Line joining is not available yet.")),
                item("Duplicate Line", "Shortcut+D", e -> showInfo("Duplicate Line", "Line duplication is not available yet.")),
                disabled("Fill Paragraph"),
                item("Sort Lines", null, e -> showInfo("Sort Lines", "Line sorting is not available yet.")),
                item("Reverse Lines", null, e -> showInfo("Reverse Lines", "Line reversal is not available yet.")),
                item("Transpose", null, e -> showInfo("Transpose", "Transposition is not available yet.")),
                new SeparatorMenuItem(),
                disabled("Indent Selection"),
                item("Unindent Line or Selection", "Shift+Tab", e -> showInfo("Unindent", "Unindent is not available yet.")),
                convertIndents,
                new SeparatorMenuItem(),
                macros,
                bookmarks,
                item("Emoji & Symbols", "Shortcut+Alt+;", e -> showInfo("Emoji & Symbols", "Emoji picker is not available yet.")));

        // ---- View
        Menu toolWindows = new Menu("Tool Windows");
        toolWindows.getItems().addAll(
                item("Project", null, e -> toggleProjectPanel(
                        !horizontalSplit.getItems().contains(leftDock))),
                item("Run", null, e -> showRunPanel()),
                item("Terminal", null, e -> showTerminal()),
                item("Maven / Build", null, e -> showRightPanel(3)),
                item("Database", null, e -> showRightPanel(2)));
        Menu appearance = new Menu("Appearance");
        appearance.getItems().addAll(placeholder("Enter Distraction Free Mode", null),
                placeholder("Enter Full Screen", null), placeholder("Toolbar", null));
        Menu activeEditor = new Menu("Active Editor");
        activeEditor.getItems().addAll(placeholder("Split Right", null), placeholder("Close", null));
        Menu bidi = new Menu("Bidi Text Base Direction");
        bidi.getItems().addAll(placeholder("Left-to-Right", null), placeholder("Right-to-Left", null));
        Menu view = new Menu("View");
        view.getItems().addAll(
                toolWindows, appearance, new SeparatorMenuItem(),
                item("Quick Definition", "Shortcut+Shift+I", e -> {
                    EditorTab tab = currentEditor();
                    if (tab != null) goToDeclaration(tab.wordAtCaret());
                }),
                placeholder("Show Siblings", null), placeholder("Quick Type Definition", null),
                item("Quick Documentation", "Shortcut+Q", e -> quickDocAtCaret()),
                placeholder("Show Bytecode", null),
                item("Parameter Info", "Shortcut+P", e -> showParameterInfo()),
                placeholder("Type Info", "Shortcut+Shift+P"), placeholder("Context Info", "Alt+Q"),
                new SeparatorMenuItem(),
                item("Jump to Source", "F4", e -> {
                    EditorTab tab = currentEditor();
                    if (tab != null) tab.focusEditor();
                }),
                placeholder("Recent Locations", "Shortcut+Shift+E"), placeholder("Recent Files", "Shortcut+E"),
                placeholder("Recently Changed Files", null), placeholder("Recent Changes", "Alt+Shift+C"),
                new SeparatorMenuItem(), placeholder("Compare With…", "Shortcut+D"), placeholder("Compare with Clipboard", null),
                new SeparatorMenuItem(), placeholder("Quick Switch Scheme…", null), activeEditor,
                placeholder("Increase Font Size in All Editors", "Alt+Shift+."),
                placeholder("Decrease Font Size in All Editors", "Alt+Shift+Comma"),
                placeholder("Reset Font Size in All Editors", null), bidi);

        // ---- Navigate
        Menu navigateInFile = new Menu("Navigate in File");
        navigateInFile.getItems().addAll(placeholder("Next Method", null), placeholder("Previous Method", null));
        Menu navigate = new Menu("Navigate");
        navigate.getItems().addAll(
                placeholder("Back", "Alt+Shift+Left"),
                disabled("Forward"),
                new SeparatorMenuItem(),
                item("Search Everywhere (double Shift)", "Shortcut+Shift+A",
                        e -> searchEverywhere()),
                placeholder("Class…", "Shortcut+N"),
                item("File…", "Shortcut+Shift+N", e -> goToFile()),
                placeholder("Symbol…", "Shortcut+Alt+Shift+N"),
                placeholder("Text…", "Shortcut+Shift+F"),
                item("Line:Column…", "Shortcut+G", e -> goToLine()),
                placeholder("Endpoint…", null),
                new SeparatorMenuItem(),
                placeholder("Next Highlighted Error", "F2"),
                placeholder("Previous Highlighted Error", "Shift+F2"),
                new SeparatorMenuItem(),
                disabled("Last Edit Location"),
                disabled("Next Edit Location"),
                new SeparatorMenuItem(),
                navigateInFile,
                placeholder("Select In…", "Alt+Shift+1"),
                placeholder("Jump to Navigation Bar", "Alt+Home"),
                new SeparatorMenuItem(),
                item("Go to Declaration", "Shortcut+B", e -> {
                    EditorTab tab = currentEditor();
                    if (tab != null) goToDeclaration(tab.wordAtCaret());
                }),
                placeholder("Implementation(s)", "Shortcut+Alt+B"),
                placeholder("Type Declaration", "Shortcut+Shift+B"),
                placeholder("Super Class or Interface", "Shortcut+U"),
                placeholder("Test", "Shortcut+Shift+T"),
                placeholder("Related Symbol…", "Shortcut+Alt+Home"),
                new SeparatorMenuItem(),
                placeholder("File Structure", "Shortcut+F12"),
                placeholder("File Path", "Shortcut+Alt+Shift+2"),
                placeholder("Type Hierarchy", "Shortcut+H"),
                disabled("Method Hierarchy"),
                disabled("Call Hierarchy"),
                item("Quick Documentation", "Shortcut+Q",
                        e -> quickDocAtCaret()),
                item("Parameter Info", "Shortcut+Shift+P",
                        e -> showParameterInfo()),
                item("Go to Implementation(s)", "Shortcut+Alt+B",
                        e -> goToImplementation()),
                item("Find Usages", "Alt+F7", e -> {
                    EditorTab tab = currentEditor();
                    if (tab != null) showUsages(tab.wordAtCaret());
                }),
                new SeparatorMenuItem(),
                item("Go to File\u2026", "Shortcut+P", e -> goToFile()),
                item("Go to Line\u2026", "Shortcut+G", e -> goToLine()));

        // ---- Code
        Menu completion = new Menu("Code Completion");
        completion.getItems().addAll(placeholder("Basic", "Shortcut+Space"), placeholder("Smart Type", "Shortcut+Shift+Space"));
        Menu folding = new Menu("Folding");
        folding.getItems().addAll(placeholder("Expand", "Shortcut+Plus"), placeholder("Collapse", "Shortcut+Minus"));
        Menu code = new Menu("Code");
        code.getItems().addAll(
                placeholder("Override Methods…", "Shortcut+O"), placeholder("Implement Methods…", "Shortcut+I"),
                placeholder("Delegate Methods…", null), placeholder("Generate…", "Alt+Insert"), completion,
                new SeparatorMenuItem(), placeholder("Inspect Code…", null), placeholder("Code Cleanup…", null),
                placeholder("Analyze Code", null), placeholder("Analyze Stack Trace or Thread Dump…", null),
                new SeparatorMenuItem(), placeholder("Insert Live Template…", "Shortcut+J"), disabled("Save as Live Template…"),
                placeholder("Surround With…", "Shortcut+Alt+Shift+B"), placeholder("Unwrap/Remove…", "Shortcut+Shift+Delete"),
                new SeparatorMenuItem(), folding,
                item("Comment with Line Comment", "Shortcut+Slash", e -> withEditor(EditorTab::toggleComment)),
                placeholder("Comment with Block Comment", "Shortcut+Shift+Slash"), placeholder("Reformat Code", "Shortcut+Alt+L"),
                placeholder("Reformat File…", "Shortcut+Alt+Shift+L"), placeholder("Auto-Indent Lines", "Shortcut+Alt+I"),
                placeholder("Optimize Imports", "Shortcut+Alt+O"), placeholder("Rearrange Code", null), new SeparatorMenuItem(),
                placeholder("Move Statement Down", "Shortcut+Shift+Down"), placeholder("Move Statement Up", "Shortcut+Shift+Up"),
                disabled("Move Element Left"), disabled("Move Element Right"), placeholder("Move Line Down", null), placeholder("Move Line Up", null),
                new SeparatorMenuItem(), disabled("Update Copyright…"), placeholder("Generate module-info Descriptors", null),
                disabled("Reformat File with Rustfmt"), disabled("Reformat Cargo Project with Rustfmt"), new SeparatorMenuItem(),
                placeholder("Convert Java File to Kotlin File", "Shortcut+Alt+Shift+K"), item("Generate Test for Current Class", null, e -> generateTest()));

        // ---- Refactor
        Menu extract = new Menu("Extract/Introduce");
        extract.getItems().addAll(placeholder("Variable…", "Shortcut+Alt+V"), placeholder("Method…", "Shortcut+Alt+M"));
        Menu migrate = new Menu("Migrate Packages and Classes");
        migrate.getItems().add(placeholder("Migrate…", null));
        Menu refactor = new Menu("Refactor");
        refactor.getItems().addAll(
                placeholder("Refactor This…", "Shortcut+Alt+Shift+T"), item("Rename…", "Shift+F6", e -> renameAtCaret()),
                item("Rename File…", null, e -> renameSelectedFile()), placeholder("Change Signature…", "Shortcut+F6"), extract,
                placeholder("Inline…", "Shortcut+Alt+N"), placeholder("Find and Replace Code Duplicates…", null), new SeparatorMenuItem(),
                placeholder("Move Class…", "F6"), placeholder("Copy Class…", "F5"), disabled("Safe Delete…"), new SeparatorMenuItem(),
                placeholder("Pull Members Up…", null), placeholder("Push Members Down…", null), new SeparatorMenuItem(), disabled("Type Migration…"),
                disabled("Make Static…"), disabled("Convert To Instance Method…"), new SeparatorMenuItem(),
                placeholder("Use Interface Where Possible…", null), placeholder("Replace Inheritance with Delegation…", null),
                placeholder("Encapsulate Fields…", null), migrate, disabled("Invert Boolean…"), disabled("Internationalize…"));

        // ---- Build
        Menu build = new Menu("Build");
        build.getItems().addAll(
                item("Build Project", "Shortcut+F9", e -> buildProject()),
                placeholder("Build Module 'lumina-ide'", null), placeholder("Recompile 'ProjectSpec.java'", "Shortcut+Shift+F9"),
                item("Rebuild Project", null, e -> { cleanProject(); buildProject(); }), disabled("Build Artifacts…"),
                placeholder("Groovy Resources", null));

        // ---- Run
        Menu debugging = new Menu("Debugging Actions"); debugging.getItems().add(placeholder("Resume Program", "F9"));
        Menu breakpoints = new Menu("Toggle Breakpoint"); breakpoints.getItems().add(placeholder("Line Breakpoint", "Shortcut+F8"));
        Menu testHistory = new Menu("Test History"); testHistory.getItems().add(disabled("No test history"));
        Menu profiler = new Menu("Open Profiler Snapshot"); profiler.getItems().add(placeholder("Open…", null));
        Menu run = new Menu("Run");
        run.getItems().addAll(
                item("Run 'LuminaApp'", "Shift+F10", e -> runSelectedConfig()), item("Debug 'LuminaApp'", "Shift+F9", e -> debugSelectedConfig()),
                placeholder("Run 'LuminaApp' with Coverage", null), placeholder("Profile 'LuminaApp' with 'IntelliJ Profiler'", null), new SeparatorMenuItem(),
                item("Run…", "Alt+Shift+F10", e -> runCurrentFile()), item("Debug…", "Alt+Shift+F9", e -> debugSelectedConfig()),
                placeholder("Attach to Process…", "Shortcut+Alt+5"), placeholder("Edit Configurations…", null), placeholder("Manage Targets…", null),
                new SeparatorMenuItem(), item("Stop", "Shortcut+F2", e -> console.stopProcess()), disabled("Stop Background Processes…"),
                disabled("Show Running List"), new SeparatorMenuItem(), debugging, breakpoints, placeholder("View Breakpoints…", "Shortcut+Shift+F8"),
                testHistory, placeholder("Import Tests from File…", null), placeholder("Manage Coverage Reports…", "Shortcut+Alt+6"),
                new SeparatorMenuItem(), placeholder("Attach Profiler to Process…", null), profiler,
                new SeparatorMenuItem(), item("Run All Tests", "Shortcut+Shift+T", e -> runAllTests()), item("Run Current Test Class", null, e -> runCurrentTestClass()),
                item("Clear Run Output", null, e -> console.clear()));

        // ---- Git
        Menu patch = new Menu("Patch");
        patch.getItems().addAll(
                item("Create Patch from Local Changes\u2026", null, e -> showCreatePatchDialog()),
                item("Apply Patch\u2026", null, e -> applyPatchFromFile()),
                item("Apply Patch from Clipboard\u2026", null, e -> applyPatchFromClipboard())
        );

        Menu changes = new Menu("Uncommitted Changes");
        changes.getItems().addAll(
                item("Shelve Changes\u2026", null, e -> shelveChanges()),
                item("Show Shelf", null, e -> showShelf()),
                item("Show Git Stash", null, e -> showGitStash()),
                item("Stash Changes\u2026", null, e -> showStashDialog()),
                item("Unstash Changes\u2026", null, e -> showUnstashDialog()),
                item("Rollback\u2026", "Shortcut+Alt+Z", e -> rollbackAllUncommittedChanges()),
                item("Show Local Changes as UML", "Shortcut+Alt+Shift+D", e -> showLocalChangesUml())
        );
        // ---- Current File Submenu (media_1790164464044.png)
        Menu currentFile = new Menu("Current File");
        MenuItem cfCommit = item("Commit File\u2026", null, e -> commitCurrentFile());
        MenuItem cfAdd = item("+ Add", "Shortcut+Alt+A", e -> addCurrentFile());
        MenuItem cfBlame = item("Annotate with Git Blame", null, e -> toggleBlame());
        MenuItem cfDiff = itemWithGraphic("Show Diff", GitIcons.diffIcon(), "Shortcut+D", e -> showDiffCurrentFile());
        MenuItem cfCompareRev = item("Compare with Revision\u2026", null, e -> showCompareWithRevision());
        MenuItem cfCompareBranch = item("Compare with Branch or Tag\u2026", null, e -> showCompareWithBranch());
        MenuItem cfHistory = itemWithGraphic("Show History", GitIcons.clockIcon(), null, e -> showFileHistory());
        MenuItem cfHistorySelection = item("Show History for Selection\u2026", null, e -> showSelectionHistory());

        currentFile.getItems().addAll(
                cfCommit, cfAdd, cfBlame, cfDiff,
                cfCompareRev, cfCompareBranch, cfHistory, cfHistorySelection
        );
        currentFile.setOnShowing(e -> {
            EditorTab tab = currentEditor();
            boolean hasFile = tab != null && tab.getPath() != null;
            boolean inRepo = hasFile && projectRoot != null && GitService.isRepository(projectRoot);
            cfCommit.setDisable(!inRepo);
            cfAdd.setDisable(!inRepo);
            cfBlame.setDisable(!inRepo);
            cfDiff.setDisable(!inRepo);
            cfCompareRev.setDisable(!inRepo);
            cfCompareBranch.setDisable(!inRepo);
            cfHistory.setDisable(!inRepo);
            cfHistorySelection.setDisable(!inRepo || tab.getSelectedText().isEmpty());
        });

        // ---- GitLab Submenu (media_1790164479292.png)
        Menu gitLab = new Menu("GitLab", GitIcons.gitLabIcon(14));
        gitLab.getItems().addAll(
                itemWithGraphic("Share Project on GitLab", GitIcons.gitLabIcon(14), null, e -> shareProjectOnGitLab()),
                itemWithGraphic("Clone Repository\u2026", GitIcons.gitLabIcon(14), null, e -> showCloneRepositoryDialog("GitLab")),
                itemWithGraphic("Manage Accounts\u2026", GitIcons.gitLabIcon(14), null, e -> new SettingsDialog(stage, "GitLab").show())
        );

        // ---- GitHub Submenu (media_1790164487756.png)
        Menu github = new Menu("GitHub", GitIcons.gitHubIcon(14, "#DFE1E5"));
        github.getItems().addAll(
                itemWithGraphic("Create Pull Request\u2026", GitIcons.gitHubIcon(14, "#DFE1E5"), null, e -> createPullRequest()),
                item("View Pull Requests", null, e -> viewPullRequests()),
                itemWithGraphic("Sync Fork", GitIcons.syncIcon(14, "#DFE1E5"), null, e -> syncFork()),
                itemWithGraphic("Create Gist\u2026", GitIcons.gitHubIcon(14, "#DFE1E5"), null, e -> createGist()),
                itemWithGraphic("View in browser", GitIcons.globeIcon(14, "#DFE1E5"), null, e -> viewInBrowser()),
                itemWithGraphic("Share Project on GitHub", GitIcons.gitHubIcon(14, "#DFE1E5"), null, e -> shareProjectOnGitHub()),
                itemWithGraphic("Clone Repository\u2026", GitIcons.gitHubIcon(14, "#DFE1E5"), null, e -> showCloneRepositoryDialog("GitHub")),
                itemWithGraphic("Manage Accounts\u2026", GitIcons.gitHubIcon(14, "#DFE1E5"), null, e -> new SettingsDialog(stage, "GitHub").show())
        );

        Menu git = new Menu("Git");
        git.getItems().addAll(
                item("Commit\u2026", "Shortcut+K", e -> gitCommit()),
                item("Push\u2026", "Shortcut+Shift+K", e -> showPushDialog()),
                item("Update Project\u2026", "Shortcut+T", e -> showUpdateProjectDialog()),
                item("Pull\u2026", null, e -> showPullDialog()),
                item("Fetch", null, e -> gitFetch()),
                new SeparatorMenuItem(), item("Merge\u2026", null, e -> showMergeDialog()), item("Rebase\u2026", null, e -> showRebaseDialog()), new SeparatorMenuItem(),
                item("Branches\u2026", null, e -> showBranchesPopup()), item("New Branch\u2026", "Shortcut+Alt+N", e -> showCreateBranchDialog()),
                item("New Tag\u2026", null, e -> showNewTagDialog()), item("Reset HEAD\u2026", null, e -> showResetHeadDialog()), new SeparatorMenuItem(),
                item("Show Git Log", null, e -> showGitLog()), patch, changes, currentFile,
                gitLab, github, item("Manage Remotes\u2026", null, e -> showManageRemotesDialog()), item("Clone\u2026", null, e -> showCloneRepositoryDialog(null)),
                new SeparatorMenuItem(), item("VCS Operations Popup\u2026", "Alt+`", e -> showVcsOperationsPopup()));

        // ---- Tools
        Menu tasks = new Menu("Tasks & Contexts"); tasks.getItems().add(placeholder("Open Task…", null));
        Menu services = new Menu("Services"); services.getItems().add(item("Terminal", "Alt+F12", e -> showTerminal()));
        Menu xml = new Menu("XML Actions"); xml.getItems().add(placeholder("Validate XML", null));
        Menu markdown = new Menu("Markdown"); markdown.getItems().add(placeholder("Preview", null));
        Menu security = new Menu("Security Analysis"); security.getItems().add(placeholder("Inspect", null));
        Menu deployment = new Menu("Deployment"); deployment.getItems().add(placeholder("Browse Remote Host", null));
        Menu kotlinTools = new Menu("Kotlin"); kotlinTools.getItems().add(placeholder("Configure Kotlin", null));
        Menu http = new Menu("HTTP Client"); http.getItems().add(placeholder("Create Request", null));
        Menu qodana = new Menu("Qodana"); qodana.getItems().add(placeholder("Run Inspection", null));
        Menu copilot = new Menu("GitHub Copilot"); copilot.getItems().add(placeholder("Open Chat", null));
        Menu tools = new Menu("Tools");
        tools.getItems().addAll(placeholder("AI Assistant", null), tasks, placeholder("Code With Me…", "Shortcut+Shift+Y"),
                placeholder("Generate Javadoc…", null), placeholder("Create Command Line Launcher…", null), placeholder("Create Desktop Entry…", null),
                new SeparatorMenuItem(), services, xml, markdown, placeholder("Create IntelliJ IDEA Plugin…", null), security, deployment,
                placeholder("Start SSH Session…", null), placeholder("Groovy Console", null), kotlinTools, http, qodana, copilot,
                new SeparatorMenuItem(), item("New Project Wizard…", null, e -> showNewProjectDialog()), item("Plugins…", null, e -> showPluginManager()));

        // ---- Window
        Menu layouts = new Menu("Layouts"); layouts.getItems().add(placeholder("Save Current Layout as…", null));
        Menu activeWindow = new Menu("Active Tool Window"); activeWindow.getItems().add(item("Terminal", null, e -> showTerminal()));
        Menu editorTabsMenu = new Menu("Editor Tabs"); editorTabsMenu.getItems().add(placeholder("Close All", null));
        Menu notifications = new Menu("Notifications"); notifications.getItems().add(item("Show Notifications", null, e -> showRightPanel(0)));
        Menu processes = new Menu("Processes"); processes.getItems().add(disabled("No running processes"));
        Menu window = new Menu("Window");
        window.getItems().addAll(layouts, activeWindow, editorTabsMenu, notifications, processes, new SeparatorMenuItem(),
                disabled("Next Project Window"), disabled("Previous Project Window"), new SeparatorMenuItem(),
                item("Toggle Project Panel", null, e -> toggleProjectPanel(
                        !horizontalSplit.getItems().contains(leftDock))),
                item("Toggle Bottom Panel", null, e -> {
                    boolean show = !verticalSplit.getItems().contains(bottomTabs);
                    toggleBottomPanel(show);
                }), new CheckMenuItem("Lumina"));

        // ---- Help
        Menu diagnostics = new Menu("Diagnostic Tools"); diagnostics.getItems().add(placeholder("Activity Monitor", null));
        Menu help = new Menu("Help");
        help.getItems().addAll(item("Find Action…", "Shortcut+Shift+A", e -> searchEverywhere()), placeholder("Help", null),
                placeholder("Learn IDE Features", null), new SeparatorMenuItem(), placeholder("What's New in IntelliJ IDEA", null),
                placeholder("Getting Started", null), placeholder("IntelliJ IDEA on YouTube", null), placeholder("Keyboard Shortcuts PDF", null),
                placeholder("Tip of the Day", null), new SeparatorMenuItem(), placeholder("My Productivity", null),
                placeholder("Contact Support…", null), placeholder("Submit a Bug Report…", null), placeholder("Submit Feedback…", null),
                new SeparatorMenuItem(), placeholder("Show Log in Files", null), placeholder("Show SQL Log in Files", null),
                placeholder("Collect Logs and Diagnostic Data", null), placeholder("Delete Leftover IDE Directories…", null), diagnostics,
                placeholder("Change Memory Settings", null), placeholder("Edit Custom Properties…", null), placeholder("Edit Custom VM Options…", null),
                placeholder("Manage Subscriptions…", null), new SeparatorMenuItem(), placeholder("Check for Updates…", null), item("About", null, e -> showAbout()));

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

    private MenuItem itemWithGraphic(String text, javafx.scene.Node graphic, String accelerator,
                                     javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        MenuItem mi = item(text, accelerator, action);
        if (graphic != null) mi.setGraphic(graphic);
        return mi;
    }

    private MenuItem disabled(String text) {
        MenuItem mi = new MenuItem(text);
        mi.setDisable(true);
        return mi;
    }

    private MenuItem placeholder(String text, String accelerator) {
        return item(text, accelerator, e -> showInfo(text.replace("…", ""),
                text.replace("…", "") + " is not available yet."));
    }

    private void reloadAllFromDisk() {
        saveAllEditors();
        if (projectRoot != null) {
            fileExplorer.refresh();
            console.println("Reloaded project files from disk");
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        alert.showAndWait();
    }

    private void showPluginManager() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("Plugins");
        alert.setHeaderText("Plugin Manager coming soon");
        alert.setContentText("Plugin-based project templates and more IDE extensions "
                + "will be available in a future version.");
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        alert.showAndWait();
    }

    // --------------------------------------------------------------- toolbar

    private HBox buildToolBar() {
        projectChip = new Button("No project \u25BE");
        projectChip.getStyleClass().add("project-chip");
        projectChip.setOnAction(e -> toggleProjectWidgetPopup());

        branchButton = new Button("no vcs \u25BE");
        branchButton.getStyleClass().add("branch-chip");
        branchButton.setOnAction(e -> showBranchesPopup());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        runConfigBox = new ComboBox<>();
        runConfigBox.getStyleClass().add("run-config-box");
        runConfigBox.setPrefWidth(240);

        // Run occupies one toolbar slot for its whole life: it reads "Run"
        // until something starts, then becomes "Rerun" in that exact same
        // spot (matching modern IDE new-UI run widget) instead of showing a
        // second, separate restart button next to it.
        runButton = toolButton("\u25B6", "Run selected configuration (Ctrl/Cmd+R)");
        runButton.getStyleClass().add("tool-run");
        runButton.setOnAction(e -> {
            if (!stopButton.isDisabled()) {          // something is running
                if (!console.restartLast()) runSelectedConfig();
            } else {
                runSelectedConfig();
            }
        });

        Button debugBtn = new Button("Debug", bugIcon());
        debugBtn.setGraphicTextGap(6);
        debugBtn.getStyleClass().addAll("tool-button", "tool-debug");
        debugBtn.setTooltip(new Tooltip(
                "Debug (Ctrl/Cmd+D) \u2014 launches with JDWP on port 5005 and attaches jdb"));
        debugBtn.setOnAction(e -> debugSelectedConfig());

        stopButton = toolButton("\u25A0", "Stop (Ctrl/Cmd+F2)");
        stopButton.getStyleClass().add("tool-stop");
        stopButton.setDisable(true);   // enabled only while something runs
        stopButton.setOnAction(e -> console.stopProcess());

        Button searchBtn = toolButton("\uD83D\uDD0D", "Search Everywhere (double Shift)");
        searchBtn.setOnAction(e -> searchEverywhere());

        githubButton = toolButton("\uD83D\uDC64 Sign in",
                "Sign in to GitHub \u2014 authenticates push/pull/clone in the IDE");
        githubButton.setOnAction(e -> onGitHubButton());
        refreshGitHubButton();

        Button settingsBtn = toolButton("\u2699", "Settings and more");
        settingsBtn.setOnAction(e -> showMainSettingsMenu(settingsBtn));

        HBox bar = new HBox(10, projectChip, branchButton, spacer,
                runConfigBox, runButton, debugBtn, stopButton,
                searchBtn, githubButton, settingsBtn);
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

    /**
     * Small vector "bug" glyph for the Debug button, drawn with shapes
     * instead of the ladybug emoji \u2014 emoji outside the Basic Multilingual
     * Plane (like \uD83D\uDC1E) render as a blank box on systems with no
     * color-emoji font installed, which is why Debug looked icon-less.
     */
    private javafx.scene.Node bugIcon() {
        javafx.scene.shape.Ellipse body = new javafx.scene.shape.Ellipse(4.2, 5.2);
        body.setFill(javafx.scene.paint.Color.web("#5FB865"));
        javafx.scene.shape.Circle head = new javafx.scene.shape.Circle(2.1);
        head.setFill(javafx.scene.paint.Color.web("#5FB865"));
        head.setTranslateY(-6.4);
        javafx.scene.Group legs = new javafx.scene.Group();
        for (int i = -1; i <= 1; i++) {
            for (int side = -1; side <= 1; side += 2) {
                javafx.scene.shape.Line leg = new javafx.scene.shape.Line(
                        0, i * 3.0, side * 6.0, i * 3.0);
                leg.setStroke(javafx.scene.paint.Color.web("#3C7A40"));
                leg.setStrokeWidth(1.1);
                legs.getChildren().add(leg);
            }
        }
        StackPane pane =
                new StackPane(legs, body, head);
        pane.setPrefSize(15, 15);
        pane.setMinSize(15, 15);
        pane.setMaxSize(15, 15);
        return pane;
    }

    // -------------------------------------------------------------- git chip

    private void refreshGitInfo() {
        String branch = projectRoot != null ? GitService.currentBranch(projectRoot) : null;
        if (branch != null) {
            int outgoing = GitService.unpushedCommitsCount(projectRoot, branch);
            String outgoingText = outgoing > 0 ? " \u2197" + outgoing : "";
            branchButton.setText(branch + outgoingText + " \u25BE");
            branchButton.setTooltip(new Tooltip("Git Branch: " + branch + (outgoing > 0 ? "\n" + outgoing + " outgoing commit" + (outgoing == 1 ? "" : "s") : "")));
        } else {
            branchButton.setText("no vcs \u25BE");
            branchButton.setTooltip(new Tooltip("No Git repository"));
        }
    }

    private void showUpdateProjectDialog() {
        if (!requireProject()) return;
        boolean dontShow = "true".equalsIgnoreCase(Settings.get(UpdateProjectDialog.PREF_DONT_SHOW));
        if (dontShow) {
            boolean isRebase = "rebase".equalsIgnoreCase(Settings.get(UpdateProjectDialog.PREF_STRATEGY));
            new Thread(() -> {
                GitService.Result r = GitService.updateProject(projectRoot, isRebase);
                Platform.runLater(() -> {
                    String branch = GitService.currentBranch(projectRoot);
                    if (branch == null) branch = "master";
                    if (r.ok()) {
                        console.println("\u2713 Updated project on branch " + branch + (isRebase ? " (rebase)" : ""));
                        Notification notif = new Notification(
                                "Git",
                                "Updated project",
                                r.output().isBlank() ? "Already up to date on " + branch : r.output().trim(),
                                NotificationType.INFORMATION
                        );
                        NotificationService.getInstance().notify(notif);
                        refreshGitInfo();
                        fileExplorer.refresh();
                    } else {
                        console.println("Update project failed: " + r.output().trim());
                        Alert err = new Alert(Alert.AlertType.ERROR);
                        err.setTitle("Update Failed");
                        err.setHeaderText("Git Update Project Failed");
                        err.setContentText(r.output().trim());
                        err.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
                        err.showAndWait();
                    }
                });
            }, "lumina-git-update-project").start();
            return;
        }

        UpdateProjectDialog dialog = new UpdateProjectDialog(stage, projectRoot, console::println, () -> {
            refreshGitInfo();
            fileExplorer.refresh();
        });
        dialog.show();
    }

    private void showCreatePatchDialog() {
        if (!requireProject()) return;
        CreatePatchDialog dialog = new CreatePatchDialog(stage, projectRoot, console::println, null);
        dialog.show();
    }

    private void applyPatchFromFile() {
        if (!requireProject()) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Apply Patch");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Patch Files (*.patch, *.diff)", "*.patch", "*.diff"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        if (projectRoot != null) fc.setInitialDirectory(projectRoot.toFile());
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        new Thread(() -> {
            GitService.Result r = GitService.applyPatch(projectRoot, file.toPath());
            Platform.runLater(() -> {
                if (r.ok()) {
                    console.println("\u2713 Applied patch: " + file.getName());
                    Notification notif = new Notification(
                            "Git",
                            "Patch Applied",
                            file.getName(),
                            NotificationType.INFORMATION
                    );
                    NotificationService.getInstance().notify(notif);
                    if (commitPanel != null) commitPanel.refresh();
                    fileExplorer.refresh();
                } else {
                    console.println("Apply patch failed: " + r.output().trim());
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Apply Patch Failed");
                    err.setHeaderText("Git Apply Patch Failed");
                    err.setContentText(r.output().trim());
                    err.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
                    err.showAndWait();
                }
            });
        }, "lumina-git-apply-patch").start();
    }

    private void applyPatchFromClipboard() {
        if (!requireProject()) return;
        String content = javafx.scene.input.Clipboard.getSystemClipboard().getString();
        if (content == null || content.isBlank() || (!content.contains("diff --git") && !content.contains("--- ") && !content.contains("+++ "))) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Apply Patch from Clipboard");
            alert.setHeaderText("No Patch Content in Clipboard");
            alert.setContentText("The system clipboard does not contain valid patch data.");
            alert.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
            alert.showAndWait();
            return;
        }

        new Thread(() -> {
            GitService.Result r = GitService.applyPatchFromText(projectRoot, content);
            Platform.runLater(() -> {
                if (r.ok()) {
                    console.println("\u2713 Applied patch from clipboard");
                    Notification notif = new Notification(
                            "Git",
                            "Patch Applied",
                            "Applied changes from clipboard",
                            NotificationType.INFORMATION
                    );
                    NotificationService.getInstance().notify(notif);
                    if (commitPanel != null) commitPanel.refresh();
                    fileExplorer.refresh();
                } else {
                    console.println("Apply patch from clipboard failed: " + r.output().trim());
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Apply Patch Failed");
                    err.setHeaderText("Git Apply Patch from Clipboard Failed");
                    err.setContentText(r.output().trim());
                    err.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
                    err.showAndWait();
                }
            });
        }, "lumina-git-apply-patch-clipboard").start();
    }

    private void shelveChanges() {
        if (!requireProject()) return;
        TextInputDialog dialog = new TextInputDialog("Shelf " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        dialog.setTitle("Shelve Changes");
        dialog.setHeaderText("Shelve Changes to Shelf Directory");
        dialog.setContentText("Shelf name:");
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
        dialog.showAndWait().ifPresent(name -> {
            new Thread(() -> {
                GitService.Result r = GitService.shelve(projectRoot, name);
                Platform.runLater(() -> {
                    if (r.ok()) {
                        console.println("\u2713 " + r.output());
                        Notification notif = new Notification(
                                "Git",
                                "Shelved Changes",
                                name,
                                NotificationType.INFORMATION
                        );
                        NotificationService.getInstance().notify(notif);
                        if (commitPanel != null) commitPanel.refresh();
                        fileExplorer.refresh();
                    } else {
                        console.println("Shelve failed: " + r.output());
                    }
                });
            }, "lumina-git-shelve").start();
        });
    }

    private void showShelf() {
        if (!requireProject()) return;
        List<Path> shelves = GitService.shelfList(projectRoot);
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Shelf");
        info.setHeaderText("Shelved Changes (" + shelves.size() + ")");
        if (shelves.isEmpty()) {
            info.setContentText("No shelved changes currently in shelf directory.");
        } else {
            StringBuilder sb = new StringBuilder("Shelved files in .shelf/:\n");
            for (Path p : shelves) {
                sb.append("• ").append(p.getFileName()).append("\n");
            }
            info.setContentText(sb.toString());
        }
        info.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
        info.showAndWait();
    }

    private void showGitStash() {
        if (!requireProject()) return;
        toggleLeftPanel(true);
        leftTabs.getSelectionModel().select(1);
        iconRail.selectTop(1);
        if (commitPanel != null) {
            commitPanel.switchViewMode(CommitPanel.ViewMode.STASH);
            commitPanel.refreshStashView();
        }
    }

    private void showStashDialog() {
        if (!requireProject()) return;
        StashDialog dialog = new StashDialog(stage, projectRoot, console::println, () -> {
            refreshGitInfo();
            fileExplorer.refresh();
            if (commitPanel != null) {
                commitPanel.refresh();
                if (commitPanel.getActiveViewMode() == CommitPanel.ViewMode.STASH) {
                    commitPanel.refreshStashView();
                }
            }
        });
        dialog.show();
    }

    private void showUnstashDialog() {
        if (!requireProject()) return;
        UnstashDialog dialog = new UnstashDialog(stage, projectRoot, console::println, () -> {
            refreshGitInfo();
            fileExplorer.refresh();
            if (commitPanel != null) {
                commitPanel.refresh();
                if (commitPanel.getActiveViewMode() == CommitPanel.ViewMode.STASH) {
                    commitPanel.refreshStashView();
                }
            }
        });
        dialog.show();
    }

    private void rollbackAllUncommittedChanges() {
        if (!requireProject()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Rollback Changes");
        confirm.setHeaderText("Rollback all uncommitted changes in project?");
        confirm.setContentText("All working tree modifications will be discarded. This cannot be undone.");
        confirm.getDialogPane().setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    GitService.Result r = GitService.rollbackAll(projectRoot);
                    Platform.runLater(() -> {
                        if (r.ok()) {
                            console.println("\u2713 Rolled back all uncommitted changes");
                            Notification notif = new Notification(
                                    "Git",
                                    "Rollback Completed",
                                    "All uncommitted changes discarded",
                                    NotificationType.INFORMATION
                            );
                            NotificationService.getInstance().notify(notif);
                            refreshGitInfo();
                            fileExplorer.refresh();
                            if (commitPanel != null) commitPanel.refresh();
                        } else {
                            console.println("Rollback failed: " + r.output().trim());
                        }
                    });
                }, "lumina-git-rollback-all").start();
            }
        });
    }

    private void showLocalChangesUml() {
        if (!requireProject()) return;
        console.println("Local changes UML: scanned uncommitted classes in project.");
        NotificationService.getInstance().notify(new Notification(
                "Git",
                "Local Changes",
                "Showing local uncommitted modifications",
                NotificationType.INFORMATION
        ));
        gitCommit();
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
        toggleLeftPanel(true);
        leftTabs.getSelectionModel().select(1);
        iconRail.selectTop(1);
        if (commitPanel != null) {
            commitPanel.refresh();
            commitPanel.focusCommitMessage();
        }
    }

    private void showPushDialog() {
        if (!requireProject()) return;
        int problems = 0;
        EditorTab et = currentEditor();
        if (et != null && et.getDiagnostics() != null) {
            problems = et.getDiagnostics().size();
        }
        PushCommitsDialog dialog = new PushCommitsDialog(stage, projectRoot, console::println, problems);
        dialog.setOnReviewCodeAnalysis(() -> onBottomRailSelect(5));
        dialog.setOnOpenDiff(this::openDiffViewer);
        dialog.show();
    }

    private void gitRun(String label, String subcommand) {
        if (!requireProject()) return;
        showRunPanel();
        console.runSequence("git " + label.toLowerCase(),
                List.of(List.of("git", subcommand)), projectRoot, gitEnv(), null);
    }

    /** Extra env so git authenticates with the signed-in GitHub token. */
    private Map<String, String> gitEnv() {
        String token = Settings.get(Settings.GITHUB_TOKEN);
        if (token == null) return null;
        Path askpass = GitHubAuth.ensureAskpass();
        if (askpass == null) return null;
        Map<String, String> env = new java.util.HashMap<>();
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

    private void showBranchesPopup() {
        if (!requireProject()) return;
        ensureGitBranchesPopup();
        if (branchButton != null && branchButton.getScene() != null && branchButton.isVisible()) {
            gitBranchesPopup.toggleBelow(branchButton);
        } else {
            gitBranchesPopup.showCentered(stage);
        }
    }

    private void ensureGitBranchesPopup() {
        if (gitBranchesPopup == null) {
            gitBranchesPopup = new GitBranchesPopup(() -> projectRoot, console::println, new GitBranchesPopup.BranchCallbacks() {
                @Override public void onUpdateProject() { showUpdateProjectDialog(); }
                @Override public void onCommit() { gitCommit(); }
                @Override public void onPush() { showPushDialog(); }
                @Override public void onNewBranch() { showCreateBranchDialog(); }
                @Override public void onCheckoutTag() { showComingSoon("Checkout Tag or Revision"); }
                @Override public void onBranchChanged() {
                    refreshGitInfo();
                    if (fileExplorer != null) fileExplorer.refresh();
                    if (gitLogPanel != null) gitLogPanel.refresh();
                }
                @Override public void onMergeBranch(String branch) { showMergeDialog(branch); }
                @Override public void onRebaseBranch(String target) { showRebaseDialog(target); }
            });
        }
    }

    private void showVcsOperationsPopup() {
        if (stage == null || !stage.isShowing()) return;
        ensureVcsOperationsPopup();
        vcsOperationsPopup.showCentered(stage);
    }

    private void ensureVcsOperationsPopup() {
        if (vcsOperationsPopup == null) {
            vcsOperationsPopup = new VcsOperationsPopup(
                    new VcsOperationsPopup.VcsContext() {
                        @Override public Path getProjectRoot() { return projectRoot; }
                        @Override public Path getActiveFile() {
                            EditorTab tab = currentEditor();
                            return tab != null ? tab.getPath() : null;
                        }
                        @Override public String getActiveBranch() {
                            String b = projectRoot != null ? GitService.currentBranch(projectRoot) : null;
                            return (b != null && !b.isBlank()) ? b : "master";
                        }
                    },
                    new VcsOperationsPopup.VcsCallbacks() {
                        @Override public void onCommit() { gitCommit(); }
                        @Override public void onCommitSelected() { commitCurrentFile(); }
                        @Override public void onRollback() { rollbackAllUncommittedChanges(); }
                        @Override public void onShowHistory(Path file) { showFileHistory(); }
                        @Override public void onAnnotate(Path file) { toggleBlame(); }
                        @Override public void onShowDiff(Path file) { showDiffCurrentFile(); }
                        @Override public void onBranches() { showBranchesPopup(); }
                        @Override public void onPush() { showPushDialog(); }
                        @Override public void onStash() { showStashDialog(); }
                        @Override public void onUnstash() { showUnstashDialog(); }
                        @Override public void onCopyBranchName(String branch) { copyBranchNameToClipboard(branch); }
                        @Override public void onShowLocalHistory(Path file) {
                            NotificationService.getInstance().notify(new Notification(
                                    "Local History", "Local History", (file != null ? file.getFileName().toString() : "Project") + " history up to date", NotificationType.INFORMATION));
                        }
                    }
            );
        }
    }

    private void copyBranchNameToClipboard(String branch) {
        String name = (branch != null && !branch.isBlank()) ? branch : "master";
        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(name);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
        NotificationService.getInstance().notify(new Notification(
                "Git", "Branch Name Copied", "Branch name '" + name + "' copied to clipboard", NotificationType.INFORMATION));
    }

    private void toggleProjectWidgetPopup() {
        ensureProjectWidgetPopup();
        if (projectChip != null && projectChip.getScene() != null && projectChip.isVisible()) {
            projectWidgetPopup.toggleBelow(projectChip);
        } else if (projectChip != null) {
            projectWidgetPopup.showBelow(projectChip);
        }
    }

    private void ensureProjectWidgetPopup() {
        if (projectWidgetPopup == null) {
            projectWidgetPopup = new dev.lumina.ui.ProjectWidgetPopup(
                    () -> {
                        List<dev.lumina.ui.ProjectWidgetPopup.OpenProject> list = new ArrayList<>();
                        for (LuminaApp app : ACTIVE_INSTANCES) {
                            if (app != null && app.projectRoot != null) {
                                Path root = app.projectRoot;
                                String name = root.getFileName() != null ? root.getFileName().toString() : root.toString();
                                list.add(new dev.lumina.ui.ProjectWidgetPopup.OpenProject(root, name, app.stage, () -> {
                                    if (app.stage != null) {
                                        app.stage.toFront();
                                        app.stage.requestFocus();
                                    }
                                }));
                            }
                        }
                        return list;
                    },
                    () -> dev.lumina.project.RecentProjectsManager.getInstance().getRecentProjects(),
                    new dev.lumina.ui.ProjectWidgetPopup.ProjectWidgetCallbacks() {
                        @Override public void onNewProject() { showNewProjectDialog(); }
                        @Override public void onOpenFolder() { openFolderDialog(); }
                        @Override public void onCloneRepository() { showCloneRepositoryDialog(null); }
                        @Override public void onOpenProject(Path projectDir) { openProjectInteractive(projectDir); }
                        @Override public void onRemoveRecentProject(String projectPath) {
                            dev.lumina.project.RecentProjectsManager.getInstance().removeProject(projectPath);
                        }
                    }
            );
        }
    }

    private void updateProjectChip(Path dir) {
        if (projectChip == null) return;
        if (dir != null) {
            String name = dir.getFileName() != null ? dir.getFileName().toString() : dir.toString();
            projectChip.setText(name + " \u25BE");
            projectChip.setGraphic(dev.lumina.ui.ProjectWidgetPopup.createMonogramBadge(name, 16));
            projectChip.setTooltip(new Tooltip(dir.toString()));
        } else {
            projectChip.setText("No project \u25BE");
            projectChip.setGraphic(null);
            projectChip.setTooltip(new Tooltip("No project open"));
        }
    }

    private void showCreateBranchDialog() {
        showCreateBranchDialog(null);
    }

    private void showCreateBranchDialog(String startPoint) {
        if (!requireProject()) return;
        CreateBranchDialog dlg = new CreateBranchDialog(stage, projectRoot, startPoint, newBranch -> {
            console.println("Created branch '" + newBranch + "'");
            refreshGitInfo();
            if (fileExplorer != null) fileExplorer.refresh();
            if (gitLogPanel != null) gitLogPanel.refresh();
        });
        dlg.show();
    }

    private void gitNewBranch() {
        showCreateBranchDialog();
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
        showBuildPanel();
        buildConsole.runSequence("Build " + projectRoot.getFileName(), commands, projectRoot);
    }

    private void cleanProject() {
        if (!requireProject()) return;
        List<List<String>> commands = RunConfiguration.cleanProject(projectRoot);
        if (commands == null) {
            error("Not a build project", "No pom.xml or build.gradle found.");
            return;
        }
        showBuildPanel();
        buildConsole.runSequence("Clean " + projectRoot.getFileName(), commands, projectRoot);
    }

    /** Build/Rebuild/Clean all land in their own dedicated Build panel \u2014
     *  matching IntelliJ, where Build Output is separate from Run \u2014
     *  instead of mixing compiler output into the same console as
     *  whatever you last ran. */
    private void showBuildPanel() {
        toggleBottomPanel(true);
        bottomTabs.getSelectionModel().select(5);
        iconRail.selectBottom(1);
    }

    // ------------------------------------------------- blame, usages, tests

    private void toggleBlame() {
        EditorTab tab = currentEditor();
        if (tab == null || tab.getPath() == null) return;
        // Cycle: full blame -> off (author hints reload on next open).
        if (tab.hasBlame()) {
            tab.setBlame(null);
            loadAuthorHints(tab);   // fall back to inline author hints
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

    /**
     * Load IntelliJ-style inline author hints (author shown next to each
     * class/method declaration). Runs quietly; does nothing outside a repo.
     */
    private void loadAuthorHints(EditorTab tab) {
        if (tab == null || tab.getPath() == null) return;
        tab.setOnHintClicked(this::toggleBlame);
        Path file = tab.getPath();
        Thread t = new Thread(() -> {
            List<GitService.BlameLine> lines = GitService.blame(file);
            if (lines != null) {
                Platform.runLater(() -> tab.setAuthorHints(lines));
            }
        }, "lumina-author-hints");
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

    private void handleNavigationOrUsages(EditorTab tab, String word, int line, int col, javafx.geometry.Bounds screenBounds) {
        if (word == null || word.isBlank()) return;
        boolean isDecl = false;
        if (semantics != null && tab.getPath() != null) {
            try {
                var res = semantics.resolveAt(tab.getPath(), tab.getEditorText(), line, col);
                if (res.kind() == dev.lumina.semantics.SemanticEngine.Kind.DECLARATION) {
                    isDecl = true;
                }
            } catch (Throwable ignored) {
            }
        }
        if (!isDecl) {
            isDecl = isDeclarationLine(tab.currentLineText(), word);
        }

        if (isDecl) {
            showUsagesPopup(tab, word, line, col, screenBounds);
        } else {
            goToDeclaration(word);
        }
    }

    private void showUsagesPopup(EditorTab editor, String word, int line, int column, javafx.geometry.Bounds screenBounds) {
        if (word == null || word.isBlank() || projectRoot == null) return;
        if (editor == null || editor.getPath() == null) return;

        if (usagesPopup == null) {
            usagesPopup = new UsagesPopup((path, ln) -> openFileAtLineAndSymbol(path, ln, word, true));
            usagesPopup.setOnPopout(() -> showUsages(word));
        }

        final Path file = editor.getPath();
        final String text = editor.getEditorText();
        dev.lumina.semantics.SemanticEngine engine = semantics;

        Thread t = new Thread(() -> {
            List<dev.lumina.semantics.SemanticEngine.Usage> hits = List.of();
            Docs.SymbolDoc doc = null;
            if (engine != null) {
                try {
                    hits = engine.findUsages(file, text, line, column);
                    doc = engine.symbolDocAt(file, text, line, column);
                } catch (Throwable ignored) {
                }
            }

            final List<dev.lumina.semantics.SemanticEngine.Usage> resolvedHits = hits;
            final Docs.SymbolDoc resolvedDoc = doc;

            Platform.runLater(() -> {
                if (resolvedHits.isEmpty()) {
                    showUsages(word);
                    return;
                }

                StringBuilder headerBuilder = new StringBuilder();
                if (resolvedDoc != null) {
                    String kindStr = "interface".equalsIgnoreCase(resolvedDoc.kind())
                            ? "Abstract method "
                            : ("class".equalsIgnoreCase(resolvedDoc.kind()) ? "Method " : (resolvedDoc.kind() + " "));
                    headerBuilder.append(kindStr).append(resolvedDoc.name());
                    if (resolvedDoc.params() != null) {
                        headerBuilder.append("(");
                        for (int i = 0; i < resolvedDoc.params().size(); i++) {
                            String p = resolvedDoc.params().get(i);
                            int spaceIdx = p.indexOf(' ');
                            headerBuilder.append(spaceIdx > 0 ? p.substring(0, spaceIdx) : p);
                            if (i < resolvedDoc.params().size() - 1) headerBuilder.append(", ");
                        }
                        headerBuilder.append(")");
                    }
                    if (resolvedDoc.containerFqcn() != null && !resolvedDoc.containerFqcn().isBlank()) {
                        headerBuilder.append(" of ").append(resolvedDoc.containerFqcn());
                    }
                } else {
                    headerBuilder.append("Usages of ").append(word);
                }

                List<UsagesPopup.UsageItem> items = resolvedHits.stream()
                        .map(u -> {
                            String rel = projectRoot != null && u.file().startsWith(projectRoot)
                                    ? projectRoot.relativize(u.file()).toString()
                                    : u.file().toString();
                            return new UsagesPopup.UsageItem(u.file(), u.line(), u.preview(), u.declaration(), rel);
                        })
                        .toList();

                javafx.geometry.Bounds bounds = screenBounds != null ? screenBounds : editor.getWordBoundsOnScreen(editor.getCodeArea().getCaretPosition());
                if (bounds != null) {
                    usagesPopup.show(editor.getCodeArea(), projectRoot, word, headerBuilder.toString(), items, bounds);
                } else {
                    showUsages(word);
                }
            });
        }, "lumina-usages-popup");
        t.setDaemon(true);
        t.start();
    }

    private void showUsages(String word) {
        if (word == null || word.isBlank()) return;
        if (!requireProject()) return;
        EditorTab editor = currentEditor();
        dev.lumina.semantics.SemanticEngine engine = semantics;
        if (engine == null || editor == null || editor.getPath() == null) {
            new UsagesDialog(stage, projectRoot, word, (p, ln) -> openFileAtLineAndSymbol(p, ln, word, true)).show();
            return;
        }
        final Path file = editor.getPath();
        final String text = editor.getEditorText();
        final int line = editor.getCaretLine();
        final int column = editor.getCaretColumn();
        Thread t = new Thread(() -> {
            List<dev.lumina.semantics.SemanticEngine.Usage> hits = List.of();
            try {
                hits = engine.findUsages(file, text, line, column);
            } catch (Throwable ignored) {
            }
            final var resolved = hits;
            Platform.runLater(() -> {
                if (resolved.isEmpty()) {
                    // unresolved symbol: keep the old text scan as safety net
                    new UsagesDialog(stage, projectRoot, word,
                            (p, ln) -> openFileAtLineAndSymbol(p, ln, word, true)).show();
                } else {
                    List<UsagesDialog.Hit> rows = resolved.stream()
                            .map(u -> new UsagesDialog.Hit(
                                    u.file(), u.line(), u.preview(),
                                    u.declaration()))
                            .toList();
                    new UsagesDialog(stage, projectRoot, word, rows,
                            (p, ln) -> openFileAtLineAndSymbol(p, ln, word, true)).show();
                }
            });
        }, "lumina-semantic-usages");
        t.setDaemon(true);
        t.start();
    }

    private void runAllTests() {
        if (!requireProject()) return;
        List<String> cmd = RunConfiguration.isMavenProject(projectRoot)
                ? RunConfiguration.maven(projectRoot, "test")
                : RunConfiguration.gradleCmd(projectRoot, "test");
        launchTests("All tests — " + projectRoot.getFileName(), cmd,
                this::runAllTests);
    }

    /** Run a test command, then parse the reports into the Tests window. */
    private void launchTests(String label, List<String> cmd, Runnable rerun) {
        lastTestRun = rerun;
        testRunStart = System.currentTimeMillis();
        testsPanel.showRunning(label);
        showRunPanel();
        console.runCommandThen(label, cmd, projectRoot, this::collectTestResults);
    }

    private void collectTestResults() {
        final long since = testRunStart - 2000;   // clock skew safety
        Thread t = new Thread(() -> {
            List<dev.lumina.run.TestReport.Suite> suites =
                    dev.lumina.run.TestReport.parse(projectRoot, since);
            Platform.runLater(() -> {
                testsPanel.showResults(suites);
                if (!suites.isEmpty()) {
                    toggleBottomPanel(true);
                    iconRail.clearBottomSelection();
                    bottomTabs.getSelectionModel().select(1);   // Tests tab
                }
            });
        }, "lumina-test-report");
        t.setDaemon(true);
        t.start();
    }

    /** Open a test class by fully-qualified name and jump to a method. */
    private void openTestSource(String className, String method) {
        if (projectRoot == null) return;
        Path file = projectRoot.resolve("src/test/java")
                .resolve(className.replace('.', '/') + ".java");
        if (!Files.isRegularFile(file)) return;
        openFile(file);
        try {
            List<String> lines = Files.readAllLines(file);
            java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
                    "\\b" + java.util.regex.Pattern.quote(method) + "\\s*\\(");
            for (int i = 0; i < lines.size(); i++) {
                if (decl.matcher(lines.get(i)).find()) {
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

    /** Rerun only the failed tests (surefire Class#m1+m2 syntax). */
    private void rerunFailedTests(List<dev.lumina.run.TestReport.Case> failed) {
        if (failed.isEmpty() || projectRoot == null) return;
        if (RunConfiguration.isMavenProject(projectRoot)) {
            Map<String, List<String>> byClass = new java.util.LinkedHashMap<>();
            for (var c : failed) {
                String simple = c.className()
                        .substring(c.className().lastIndexOf('.') + 1);
                byClass.computeIfAbsent(simple, k -> new java.util.ArrayList<>())
                        .add(c.method());
            }
            String spec = byClass.entrySet().stream()
                    .map(e -> e.getKey() + "#" + String.join("+", e.getValue()))
                    .reduce((a, b) -> a + "," + b).orElse("");
            List<String> cmd = RunConfiguration.maven(
                    projectRoot, "-Dtest=" + spec, "test");
            launchTests("Failed tests (" + failed.size() + ")", cmd,
                    () -> rerunFailedTests(failed));
        } else {
            List<String> cmd = new java.util.ArrayList<>(
                    RunConfiguration.gradleCmd(projectRoot, "test"));
            for (var c : failed) {
                cmd.add("--tests");
                cmd.add(c.className() + "." + c.method());
            }
            launchTests("Failed tests (" + failed.size() + ")", cmd,
                    () -> rerunFailedTests(failed));
        }
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
        launchTests("Test " + simple, cmd, this::runCurrentTestClass);
    }

    /** Run a single @Test method (IntelliJ-style), or the whole class if null. */
    private void runTestMethod(String method) {
        EditorTab tab = currentEditor();
        if (tab == null || tab.getPath() == null) return;
        String fqcn = testFqcnOf(tab.getPath());
        if (fqcn == null) {
            error("Not a test class",
                    tab.getPath().getFileName() + " is not under src/test/java.");
            return;
        }
        if (method == null) {
            runCurrentTestClass();
            return;
        }
        saveCurrent(false);
        String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);
        List<String> cmd = RunConfiguration.isMavenProject(projectRoot)
                ? RunConfiguration.maven(projectRoot, "-Dtest=" + simple + "#" + method, "test")
                : RunConfiguration.gradleCmd(projectRoot, "test",
                "--tests", fqcn + "." + method);
        final String m = method;
        launchTests("Test " + simple + "." + method + "()", cmd,
                () -> runTestMethod(m));
    }

    /** Right-click menu inside the code editor (IntelliJ-style). */
    private ContextMenu buildEditorContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem showContext = item("\uD83D\uDCA1  Show Context Actions", "Alt+Enter", e -> {
            EditorTab t = currentEditor();
            if (t != null) t.openContextActions();
        });

        Menu aiActions = new Menu("@ AI Actions");
        MenuItem explainCode = item("Explain Code", null, e -> {});
        aiActions.getItems().add(explainCode);

        MenuItem paste = item("Paste", "Ctrl+V", e -> withEditor(EditorTab::paste));
        Menu copyPasteSpecial = new Menu("Copy / Paste Special");
        MenuItem copyRef = item("Copy Reference", "Ctrl+Alt+Shift+C", e -> {});
        copyPasteSpecial.getItems().add(copyRef);
        MenuItem columnSelection = item("Column Selection Mode", "Alt+Shift+Insert", e -> {});

        Menu goTo = new Menu("Go To");
        MenuItem gotoDecl = item("Declaration or Usages", "Shortcut+B", e -> {
            EditorTab t = currentEditor();
            if (t != null) goToDeclaration(t.wordAtCaret());
        });
        MenuItem gotoImpl = item("Implementation(s)", "Shortcut+Alt+B", e -> {});
        MenuItem superMethod = item("Super Method", "Shortcut+U", e -> {});
        MenuItem gotoTest = item("Test", "Shortcut+Shift+T", e -> generateTest());
        goTo.getItems().addAll(gotoDecl, gotoImpl, superMethod, gotoTest);

        Menu folding = new Menu("Folding");
        Menu analyze = new Menu("Analyze");

        MenuItem rename = item("Rename\u2026", "Shift+F6", e -> {});
        Menu refactor = new Menu("Refactor");
        MenuItem generate = item("Generate\u2026", "Alt+Insert", e -> {
            EditorTab t = currentEditor();
            if (t != null) t.openGeneratePopup();
        });

        MenuItem runTest = item("\u25B6  Run Test", null, e -> {
            EditorTab t = currentEditor();
            runTestMethod(t != null ? t.testMethodAtCaret() : null);
        });
        MenuItem run = item("\u25B6  Run", null, e -> runCurrentFile());
        MenuItem debug = item("\uD83D\uDC1E  Debug", null, e -> debugSelectedConfig());

        Menu openIn = new Menu("Open In");
        Menu localHistory = new Menu("Local History");

        MenuItem compareClipboard = item("Compare with Clipboard", null, e -> {});
        Menu diagrams = new Menu("Diagrams");
        MenuItem createGist = item("Create Gist\u2026", null, e -> {});

        MenuItem addFileToChat = item("Add file to Chat", null, e -> {});
        MenuItem inlineChat = item("Open Inline Chat", "Shortcut+Shift+G", e -> {});
        Menu githubCopilot = new Menu("GitHub Copilot");
        MenuItem upgradeJava = item("Upgrade Java Runtime and Frameworks", null, e -> {});

        menu.setOnShowing(e -> {
            EditorTab t = currentEditor();
            boolean isTest = t != null && t.getPath() != null
                    && testFqcnOf(t.getPath()) != null;
            boolean isMain = t != null && t.getPath() != null
                    && fqcnOf(t.getPath()) != null;
            String testMethod = (t != null && isTest) ? t.testMethodAtCaret() : null;
            runTest.setText(testMethod != null
                    ? "\u25B6  Run '" + testMethod + "()'" : "\u25B6  Run Test");
            runTest.setVisible(isTest);
            run.setVisible(isMain);
            debug.setVisible(isMain);
        });

        menu.getItems().addAll(
                showContext, aiActions,
                new SeparatorMenuItem(),
                paste, copyPasteSpecial, columnSelection,
                new SeparatorMenuItem(),
                goTo, folding, analyze,
                new SeparatorMenuItem(),
                rename, refactor, generate,
                new SeparatorMenuItem(),
                runTest, run, debug,
                new SeparatorMenuItem(),
                openIn, localHistory,
                new SeparatorMenuItem(),
                compareClipboard, diagrams, createGist,
                new SeparatorMenuItem(),
                addFileToChat, inlineChat, githubCopilot, upgradeJava
        );
        return menu;
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
        githubButton.getStyleClass().remove("github-signed-in");
        if (user != null) {
            githubButton.setText("\uD83D\uDC64 " + user);
            githubButton.getStyleClass().add("github-signed-in");
            githubButton.setTooltip(new Tooltip("Signed in to GitHub as " + user
                    + " \u2014 click for profile / sign out"));
        } else {
            githubButton.setText("\uD83D\uDC64 Sign in");
            githubButton.setTooltip(new Tooltip(
                    "Sign in to GitHub \u2014 authenticates push/pull/clone in the IDE"));
        }
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
                "1. Click below: your browser opens the GitHub token page,\n"
                        + "   pre-filled with a unique name and the 'repo' scope.\n"
                        + "2. Scroll down, click 'Generate token', copy it, paste here.\n"
                        + "Already have a Lumina token? Just paste it \u2014 no need to make\n"
                        + "a new one. Lumina uses it to authenticate push, pull and clone.");
        info.getStyleClass().add("form-static");
        info.setWrapText(true);

        Button open = new Button("Open GitHub token page in browser");
        open.getStyleClass().add("dialog-secondary");
        open.setOnAction(e -> openBrowser(GitHubAuth.tokenUrl()));

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
                new SearchEverywhereDialog.Action("Git: Push\u2026", this::showPushDialog),
                new SearchEverywhereDialog.Action("Git: Update Project\u2026", this::showUpdateProjectDialog),
                new SearchEverywhereDialog.Action("Git: Pull\u2026", this::showPullDialog),
                new SearchEverywhereDialog.Action("Git: Fetch", this::gitFetch),
                new SearchEverywhereDialog.Action("Git: Merge\u2026", this::showMergeDialog),
                new SearchEverywhereDialog.Action("Git: Rebase\u2026", this::showRebaseDialog),
                new SearchEverywhereDialog.Action("Git: Branches\u2026", this::showBranchesPopup),
                new SearchEverywhereDialog.Action("Git: New Branch\u2026", this::gitNewBranch),
                new SearchEverywhereDialog.Action("Git: New Tag\u2026", this::showNewTagDialog),
                new SearchEverywhereDialog.Action("Git: Reset HEAD\u2026", this::showResetHeadDialog),
                new SearchEverywhereDialog.Action("Git: Stash Changes\u2026", this::showStashDialog),
                new SearchEverywhereDialog.Action("Git: Show Stash", this::showGitStash),
                new SearchEverywhereDialog.Action("Git: Unstash Changes\u2026", this::showUnstashDialog),
                new SearchEverywhereDialog.Action("Git: Create Patch\u2026", this::showCreatePatchDialog),
                new SearchEverywhereDialog.Action("Git: Apply Patch\u2026", this::applyPatchFromFile),
                new SearchEverywhereDialog.Action("Git: Rollback\u2026", this::rollbackAllUncommittedChanges),
                new SearchEverywhereDialog.Action("Git: Manage Remotes\u2026", this::showManageRemotesDialog),
                new SearchEverywhereDialog.Action("Git: Clone\u2026", () -> showCloneRepositoryDialog(null)),
                new SearchEverywhereDialog.Action("Git: VCS Operations Popup\u2026", this::showVcsOperationsPopup),
                new SearchEverywhereDialog.Action("Git: Show Diff (Current File)", this::showDiffCurrentFile),
                new SearchEverywhereDialog.Action("Git: Annotate with Git Blame", this::toggleBlame),
                new SearchEverywhereDialog.Action("Git: Compare with Revision\u2026", this::showCompareWithRevision),
                new SearchEverywhereDialog.Action("Git: Compare with Branch or Tag\u2026", this::showCompareWithBranch),
                new SearchEverywhereDialog.Action("Git: Show History (Current File)", this::showFileHistory),
                new SearchEverywhereDialog.Action("Git: GitHub - Share Project\u2026", this::shareProjectOnGitHub),
                new SearchEverywhereDialog.Action("Git: GitHub - Create Pull Request\u2026", this::createPullRequest),
                new SearchEverywhereDialog.Action("Git: GitHub - Sync Fork", this::syncFork),
                new SearchEverywhereDialog.Action("Git: GitHub - Create Gist\u2026", this::createGist),
                new SearchEverywhereDialog.Action("Git: GitLab - Share Project\u2026", this::shareProjectOnGitLab),
                new SearchEverywhereDialog.Action("Find in Files\u2026", this::findInFiles),
                new SearchEverywhereDialog.Action("Go to Line\u2026", this::goToLine),
                new SearchEverywhereDialog.Action("Maven Panel", () -> showRightPanel(3)),
                new SearchEverywhereDialog.Action("Database Panel", () -> showRightPanel(2)),
                new SearchEverywhereDialog.Action("About Lumina", this::showAbout));
        new SearchEverywhereDialog(stage,
                projectRoot != null ? projectRoot : fileExplorer.getRootPath(),
                ideActions, this::openFile, this::openFileAtLine).show();
    }

    // ============================================================== M5

    /** Every open, file-backed tab is written to disk (rename needs truth). */
    private void saveAllEditors() {
        for (Tab t : allEditorTabs()) {
            if (t instanceof EditorTab et && et.getPath() != null
                    && et.getText().startsWith("\u25CF")) {
                try {
                    Files.writeString(et.getPath(), et.getEditorText());
                    et.markSaved(et.getPath());
                    recheckMavenSync(et);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private EditorTab openTabFor(Path path) {
        for (Tab t : allEditorTabs()) {
            if (t instanceof EditorTab et && path.equals(et.getPath())) {
                return et;
            }
        }
        return null;
    }

    private Optional<String> promptIdentifier(String title,
                                              String header,
                                              String initial) {
        TextInputDialog dialog = new TextInputDialog(initial);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.initOwner(stage);
        dialog.getDialogPane().getStylesheets().add(getClass()
                .getResource("/css/lumina-dark.css").toExternalForm());
        Optional<String> result = dialog.showAndWait()
                .map(String::trim);
        if (result.isEmpty()) return Optional.empty();
        if (!dev.lumina.refactor.Refactor.isValidIdentifier(result.get())) {
            error(title, "'" + result.get()
                    + "' is not a valid Java identifier.");
            return Optional.empty();
        }
        return result;
    }

    /** M5: semantic rename with preview (Shift+F6). */
    private void renameAtCaret() {
        EditorTab editor = currentEditor();
        if (editor == null || editor.getPath() == null) return;
        String word = editor.wordAtCaret();
        if (word == null || word.isBlank()) return;
        dev.lumina.semantics.SemanticEngine engine = semantics;
        if (engine == null) {
            error("Rename", "The semantic engine is still indexing \u2014 "
                    + "try again in a moment.");
            return;
        }
        Optional<String> input = promptIdentifier("Rename",
                "Rename '" + word + "' to:", word);
        if (input.isEmpty() || input.get().equals(word)) return;
        final String newName = input.get();
        saveAllEditors();
        final Path file = editor.getPath();
        final String text = editor.getEditorText();
        final int line = editor.getCaretLine();
        final int column = editor.getCaretColumn();
        Thread t = new Thread(() -> {
            List<dev.lumina.semantics.SemanticEngine.Usage> usages =
                    List.of();
            try {
                usages = engine.findUsages(file, text, line, column);
            } catch (Throwable ignored) {
            }
            final List<dev.lumina.semantics.SemanticEngine.Usage>
                    found = usages;
            Platform.runLater(() -> {
                if (found.isEmpty()) {
                    error("Rename", "Could not resolve '" + word
                            + "' semantically \u2014 rename aborted "
                            + "(Lumina never renames by text search).");
                    return;
                }
                new RenamePreviewDialog(stage, projectRoot, word, newName,
                        found, chosen -> applyRename(word, newName, chosen))
                        .show();
            });
        }, "lumina-rename");
        t.setDaemon(true);
        t.start();
    }

    private void applyRename(String oldName, String newName,
                             List<dev.lumina.semantics.SemanticEngine.Usage> usages) {
        Map<Path, List<
                dev.lumina.semantics.SemanticEngine.Usage>> byFile =
                new java.util.LinkedHashMap<>();
        for (dev.lumina.semantics.SemanticEngine.Usage u : usages) {
            if (u.startCol() > 0) {
                byFile.computeIfAbsent(u.file(),
                        k -> new java.util.ArrayList<>()).add(u);
            }
        }
        int edits = 0;
        Path fileToRename = null;
        for (Map.Entry<Path, List<
                dev.lumina.semantics.SemanticEngine.Usage>> entry
                : byFile.entrySet()) {
            Path path = entry.getKey();
            EditorTab open = openTabFor(path);
            String content;
            try {
                content = open != null ? open.getEditorText()
                        : Files.readString(path);
            } catch (IOException ex) {
                continue;
            }
            List<dev.lumina.refactor.Refactor.Edit> fileEdits =
                    new java.util.ArrayList<>();
            for (dev.lumina.semantics.SemanticEngine.Usage u
                    : entry.getValue()) {
                fileEdits.add(dev.lumina.refactor.Refactor.editForUsage(
                        content, u.line(), u.startCol(), u.endCol(), newName));
            }
            fileEdits.sort(java.util.Comparator.comparingInt(
                    dev.lumina.refactor.Refactor.Edit::start).reversed());
            if (open != null) {
                for (dev.lumina.refactor.Refactor.Edit edit : fileEdits) {
                    open.replaceRange(edit.start(), edit.end(), edit.text());
                }
            } else {
                try {
                    Files.writeString(path, dev.lumina.refactor.Refactor
                            .apply(content, fileEdits));
                } catch (IOException ex) {
                    console.println("Rename: could not write " + path);
                    continue;
                }
            }
            edits += fileEdits.size();
            boolean hasDecl = entry.getValue().stream()
                    .anyMatch(dev.lumina.semantics.SemanticEngine
                            .Usage::declaration);
            if (hasDecl && path.getFileName().toString()
                    .equals(oldName + ".java")) {
                fileToRename = path;
            }
        }
        // renaming a public type renames its file, IntelliJ-style
        if (fileToRename != null) {
            Path target = fileToRename.resolveSibling(newName + ".java");
            try {
                EditorTab open = openTabFor(fileToRename);
                if (open != null) {
                    Files.writeString(fileToRename, open.getEditorText());
                    groupOf(open).getTabs().remove(open);
                }
                Files.move(fileToRename, target,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                openFileAtLine(target, 1);
            } catch (IOException ex) {
                console.println("Rename: could not rename the file \u2014 "
                        + ex.getMessage());
            }
        }
        fileExplorer.refresh();
        console.println("\u2713 Renamed '" + oldName + "' \u2192 '" + newName
                + "': " + edits + " usages in " + byFile.size() + " files.");
    }

    /** M5: Extract Variable (Ctrl+Alt+V) \u2014 selection becomes a local. */
    private void extractVariable() {
        EditorTab editor = currentEditor();
        if (editor == null || editor.getPath() == null) return;
        String selected = editor.getSelectedText();
        if (selected == null || selected.isBlank()) {
            error("Extract Variable", "Select an expression first.");
            return;
        }
        if (selected.contains(";") || selected.contains("\n")
                || selected.contains("{")) {
            error("Extract Variable",
                    "Select a single expression, not statements.");
            return;
        }
        int selStart = editor.getSelectionStart();
        int selEnd = editor.getSelectionEnd();
        String text = editor.getEditorText();
        Optional<String> input = promptIdentifier("Extract Variable",
                "Variable name:", dev.lumina.refactor.Refactor
                        .guessVarName(selected));
        if (input.isEmpty()) return;
        String name = input.get();
        int lineStart = dev.lumina.refactor.Refactor
                .startOfLineAt(text, selStart);
        String indent = dev.lumina.refactor.Refactor.indentAt(text, lineStart);
        editor.replaceRange(selStart, selEnd, name);
        editor.insertAt(lineStart, indent + "var " + name + " = "
                + selected.strip() + ";\n");
    }

    /** M5: Extract Method (Ctrl+Alt+M) on a whole-line selection. */
    private void extractMethod() {
        EditorTab editor = currentEditor();
        dev.lumina.semantics.SemanticEngine engine = semantics;
        if (editor == null || editor.getPath() == null) return;
        if (engine == null) {
            error("Extract Method", "The semantic engine is still indexing "
                    + "\u2014 try again in a moment.");
            return;
        }
        int selStart = editor.getSelectionStart();
        int selEnd = editor.getSelectionEnd();
        if (selEnd <= selStart) {
            error("Extract Method", "Select the statements to extract.");
            return;
        }
        String text = editor.getEditorText();
        int startLine = dev.lumina.refactor.Refactor.lineOf(text, selStart);
        int endLine = dev.lumina.refactor.Refactor.lineOf(text,
                Math.max(selStart, selEnd - 1));
        int from = dev.lumina.refactor.Refactor.lineStartOffset(text, startLine);
        int to = dev.lumina.refactor.Refactor.lineEndOffset(text, endLine);
        String body = text.substring(from, to);
        if (body.contains("return ") || body.contains("return;")) {
            error("Extract Method",
                    "The selection contains a return statement.");
            return;
        }
        dev.lumina.refactor.Refactor.MethodPlan plan =
                engine.planExtractMethod(editor.getPath(), text,
                        startLine, endLine);
        if (!plan.valid()) {
            error("Extract Method", plan.reason());
            return;
        }
        Optional<String> input = promptIdentifier("Extract Method",
                "Method name:", "extracted");
        if (input.isEmpty()) return;
        String name = input.get();
        String methodIndent = dev.lumina.refactor.Refactor.indentAt(text,
                dev.lumina.refactor.Refactor.lineStartOffset(text,
                        plan.insertAfterLine()));
        String callIndent = dev.lumina.refactor.Refactor.indentAt(text, from);
        String methodText = dev.lumina.refactor.Refactor.buildMethodText(
                name, plan, body.lines().toList(), methodIndent);
        String callLine = dev.lumina.refactor.Refactor.buildCallLine(
                name, plan, callIndent);
        int insertOffset = dev.lumina.refactor.Refactor.lineEndOffset(text,
                plan.insertAfterLine());
        editor.insertAt(insertOffset, methodText);   // below the selection
        editor.replaceRange(from, to, callLine);
    }

    // ---------------------------------------------------- "Add Starters"

    /**
     * Shows a "+ Add Starters..." hint above &lt;dependencies&gt;
     * (pom.xml) or dependencies { (build.gradle) for a Spring Boot
     * project \u2014 the same IntelliJ Ultimate feature, opened from an
     * already-open build file instead of the New Project wizard.
     */
    private void wireAddStarters(EditorTab tab) {
        Path path = tab.getPath();
        if (path == null) {
            tab.setAddStartersLine(-1);
            return;
        }
        String fileName = path.getFileName().toString();
        boolean gradle = fileName.equals("build.gradle") || fileName.equals("build.gradle.kts");
        boolean maven = fileName.equals("pom.xml");
        if (!gradle && !maven) {
            tab.setAddStartersLine(-1);
            return;
        }
        String text = tab.getEditorText();
        if (!text.contains("org.springframework.boot")) {
            tab.setAddStartersLine(-1);
            return;
        }
        int line = lineIndexOf(text, maven ? "<dependencies>" : "dependencies {");
        if (line < 0) {
            tab.setAddStartersLine(-1);
            return;
        }
        tab.setAddStartersLine(line);
        tab.setOnAddStartersClicked(() -> showAddStartersDialog(tab, gradle));
    }

    // -------------------------------------------------- "Load Maven Changes"

    /**
     * One-time wiring for a newly opened editor tab: the reload click
     * handler, a live re-check on every settled edit (so the hint reacts
     * the moment you type — add, remove, or change a dependency — not
     * only after a save), and an initial check.
     */
    private void wireMavenSync(EditorTab tab) {
        tab.setOnMavenReloadClicked(() -> reloadMavenDependencies(tab));
        tab.setOnContentSettled(() -> recheckMavenSync(tab));
        recheckMavenSync(tab);
    }

    /**
     * Shows IntelliJ's "Load Maven Changes" hint (top-right of the editor)
     * whenever the tab's LIVE text (not just what's saved to disk) differs
     * from {@link #mavenSyncBaselineText} — the build file's content as of
     * the last successful dependency resolve. Content-based rather than
     * timestamp-based, so it reacts immediately while typing/pasting and
     * doesn't care whether the change was an addition, removal, or edit.
     */
    private void recheckMavenSync(EditorTab tab) {
        Path path = tab.getPath();
        if (path == null || projectRoot == null) {
            tab.setMavenChangesPending(false);
            return;
        }
        String fileName = path.getFileName().toString();
        boolean buildFile = fileName.equals("pom.xml")
                || fileName.equals("build.gradle") || fileName.equals("build.gradle.kts");
        if (!buildFile || !path.equals(projectRoot.resolve(fileName))) {
            tab.setMavenChangesPending(false);
            return;
        }
        boolean changed = mavenSyncBaselineText == null
                || !mavenSyncBaselineText.equals(tab.getEditorText());
        tab.setMavenChangesPending(changed && !tab.isMavenDismissed());
    }

    /** Re-checks the "Load Maven Changes" hint on every currently open tab. */
    private void refreshMavenSyncForOpenTabs() {
        for (Tab t : allEditorTabs()) {
            if (t instanceof EditorTab et) recheckMavenSync(et);
        }
    }

    /**
     * Runs when the user clicks the "Load Maven Changes" icon: saves the
     * build file first (Maven/Gradle only ever read it from disk, so an
     * unsaved edit must be persisted — IntelliJ does the same before
     * running an external tool), re-downloads dependencies, rebuilds the
     * cached classpath, and refreshes code completion. Shown with a
     * background-task progress row instead of blocking the UI.
     */
    private void reloadMavenDependencies(EditorTab tab) {
        if (projectRoot == null) return;
        Path root = projectRoot;
        Path path = tab.getPath();
        if (path != null) {
            try {
                Files.writeString(path, tab.getEditorText());
                tab.markSaved(path);
                fileExplorer.refresh();
            } catch (IOException ex) {
                console.println("\u2717 Could not save " + path.getFileName()
                        + " before reloading: " + ex.getMessage());
                return;
            }
        }
        String label = "Resolving dependencies of " + root.getFileName() + "\u2026";

        if (RunConfiguration.isMavenProject(root)) {
            try {
                Files.deleteIfExists(root.resolve("target/lumina.cp"));
            } catch (IOException ignored) {
            }
            showMavenSyncProgress(true, label);
            console.runSequence("Reload dependencies", List.of(
                            RunConfiguration.maven(root, "-q", "dependency:resolve"),
                            RunConfiguration.maven(root, "-q", "dependency:build-classpath",
                                    "-Dmdep.outputFile=target/lumina.cp")),
                    root, null,
                    () -> onMavenReloadSucceeded(tab, root),
                    () -> showMavenSyncProgress(false, null));
        } else if (RunConfiguration.isGradleProject(root)) {
            showMavenSyncProgress(true, label);
            Thread worker = new Thread(() -> {
                Path cp = root.resolve("build/lumina.cp");
                try {
                    Files.deleteIfExists(cp);
                } catch (IOException ignored) {
                }
                resolveGradleClasspath(cp);
                boolean ok = Files.isRegularFile(cp);
                Platform.runLater(() -> {
                    showMavenSyncProgress(false, null);
                    if (ok) {
                        onMavenReloadSucceeded(tab, root);
                    } else {
                        console.println("\u2717 Dependency resolution failed \u2014 see Run console");
                    }
                });
            }, "lumina-gradle-reload");
            worker.setDaemon(true);
            worker.start();
        }
    }

    private void onMavenReloadSucceeded(EditorTab tab, Path root) {
        mavenSyncBaselineText = tab.getEditorText();
        tab.clearMavenDismissed();
        tab.setMavenChangesPending(false);
        console.println("\u2713 Dependencies resolved for " + root.getFileName());
        try {
            Path cpFile = RunConfiguration.isMavenProject(root)
                    ? root.resolve("target/lumina.cp") : root.resolve("build/lumina.cp");
            Files.writeString(sidecarFor(cpFile), mavenSyncBaselineText);
        } catch (IOException ignored) {
        }
        initSemanticEngine(root);
        if (mavenPanel != null) mavenPanel.setProject(root);
    }

    // ------------------------------------------- Spring config quick-fixes

    /** The open tab for path, if any — used so a quick-fix that edits the
     *  build file also refreshes it live if it happens to be open. */
    private EditorTab editorTabFor(Path path) {
        for (Tab t : allEditorTabs()) {
            if (t instanceof EditorTab et && path.equals(et.getPath())) return et;
        }
        return null;
    }

    /**
     * Runs a diagnostic's {@code quickFix} id from an application.properties
     * / application.yml tab — currently just "add-dependency:&lt;id&gt;"
     * from the missing-JDBC-driver inspection. Appends the starter to the
     * build file (headless, no dialog — IntelliJ's one-click fix) and
     * immediately re-resolves dependencies, exactly like clicking "Load
     * Maven Changes" after using Add Starters.
     */
    private void applySpringConfigQuickFix(String fixId) {
        if (projectRoot == null || fixId == null || !fixId.startsWith("add-dependency:")) {
            return;
        }
        Path root = projectRoot;
        boolean gradle = RunConfiguration.isGradleProject(root);
        Path buildFile = gradle ? firstExistingBuildGradle(root) : root.resolve("pom.xml");
        if (buildFile == null || !Files.isRegularFile(buildFile)) return;
        String id = fixId.substring("add-dependency:".length());
        boolean added = AddStartersDialog.addDependencyHeadless(buildFile, gradle, id);
        if (!added) {
            console.println("Could not add a dependency for '" + id + "' to "
                    + buildFile.getFileName());
            return;
        }
        fileExplorer.refresh();
        console.println("\u2713 Added dependency: " + id);
        EditorTab openBuildTab = editorTabFor(buildFile);
        if (openBuildTab != null) {
            try {
                openBuildTab.setEditorText(Files.readString(buildFile));
                openBuildTab.markSaved(buildFile);
                wireAddStarters(openBuildTab);
            } catch (IOException ignored) {
            }
        }
        reloadDependenciesFromQuickFix(buildFile, root, gradle);
    }

    /** Same resolve pipeline as {@link #reloadMavenDependencies}, but for a
     *  build file that was just edited directly on disk (by a quick-fix)
     *  rather than through an open, unsaved editor tab. */
    private void reloadDependenciesFromQuickFix(Path buildFile, Path root, boolean gradle) {
        String label = "Resolving dependencies of " + root.getFileName() + "\u2026";
        if (RunConfiguration.isMavenProject(root)) {
            try {
                Files.deleteIfExists(root.resolve("target/lumina.cp"));
            } catch (IOException ignored) {
            }
            showMavenSyncProgress(true, label);
            console.runSequence("Reload dependencies", List.of(
                            RunConfiguration.maven(root, "-q", "dependency:resolve"),
                            RunConfiguration.maven(root, "-q", "dependency:build-classpath",
                                    "-Dmdep.outputFile=target/lumina.cp")),
                    root, null,
                    () -> onMavenReloadSucceededFromDisk(buildFile, root),
                    () -> showMavenSyncProgress(false, null));
        } else if (gradle) {
            showMavenSyncProgress(true, label);
            Thread worker = new Thread(() -> {
                Path cp = root.resolve("build/lumina.cp");
                try {
                    Files.deleteIfExists(cp);
                } catch (IOException ignored) {
                }
                resolveGradleClasspath(cp);
                boolean ok = Files.isRegularFile(cp);
                Platform.runLater(() -> {
                    showMavenSyncProgress(false, null);
                    if (ok) {
                        onMavenReloadSucceededFromDisk(buildFile, root);
                    } else {
                        console.println("\u2717 Dependency resolution failed \u2014 see Run console");
                    }
                });
            }, "lumina-gradle-reload");
            worker.setDaemon(true);
            worker.start();
        }
    }

    private void onMavenReloadSucceededFromDisk(Path buildFile, Path root) {
        String baseline = null;
        try {
            baseline = Files.readString(buildFile);
        } catch (IOException ignored) {
        }
        if (baseline != null) mavenSyncBaselineText = baseline;
        refreshMavenSyncForOpenTabs();
        console.println("\u2713 Dependencies resolved for " + root.getFileName());
        if (baseline != null) {
            try {
                Path cpFile = RunConfiguration.isMavenProject(root)
                        ? root.resolve("target/lumina.cp") : root.resolve("build/lumina.cp");
                Files.writeString(sidecarFor(cpFile), baseline);
            } catch (IOException ignored) {
            }
        }
        initSemanticEngine(root);
        if (mavenPanel != null) mavenPanel.setProject(root);
    }

    private static int lineIndexOf(String text, String needle) {
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(needle)) return i;
        }
        return -1;
    }

    private void showAddStartersDialog(EditorTab tab, boolean gradle) {
        Path path = tab.getPath();
        if (path == null) return;
        String text = tab.getEditorText();
        String bootVersion = detectBootVersion(text, gradle);
        java.util.Set<String> present = detectPresentDependencyIds(text);
        new AddStartersDialog(stage, path, gradle, bootVersion, present, () -> {
            try {
                tab.setEditorText(Files.readString(path));
                tab.markSaved(path);
                wireAddStarters(tab);
                recheckMavenSync(tab);
                console.println("\u2713 Starters added to " + path.getFileName());
            } catch (IOException ex) {
                console.println("Add Starters: could not refresh the editor \u2014 "
                        + ex.getMessage());
            }
        }).show();
    }

    private static String detectBootVersion(String text, boolean gradle) {
        java.util.regex.Matcher m = gradle
                ? java.util.regex.Pattern.compile(
                        "org\\.springframework\\.boot['\"]\\s+version\\s+['\"]([\\d.]+)['\"]")
                .matcher(text)
                : java.util.regex.Pattern.compile(
                "spring-boot-starter-parent</artifactId>\\s*<version>([\\d.]+)</version>",
                java.util.regex.Pattern.DOTALL).matcher(text);
        return m.find() ? m.group(1) : "";
    }

    /**
     * Best-effort reverse mapping from what's already in the file back to
     * Initializr dependency ids, so Add Starters can gray out and pre-check
     * what's already there. Anything it can't recognize (custom or
     * hand-added dependencies) is simply not pre-checked \u2014 it is never
     * touched or removed either way.
     */
    private static java.util.Set<String> detectPresentDependencyIds(String text) {
        java.util.Set<String> ids = new java.util.LinkedHashSet<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("spring-boot-starter-([a-zA-Z0-9-]+)").matcher(text);
        while (m.find()) ids.add(m.group(1));
        if (text.contains("spring-boot-devtools")) ids.add("devtools");
        if (text.contains(":lombok") || text.contains(">lombok<")) ids.add("lombok");
        if (text.contains("spring-boot-configuration-processor")) {
            ids.add("configuration-processor");
        }
        if (text.contains(":postgresql") || text.contains(">postgresql<")) ids.add("postgresql");
        if (text.contains("mysql-connector")) ids.add("mysql");
        if (text.contains(":h2") || text.contains(">h2<")) ids.add("h2");
        if (text.contains("liquibase-core")) ids.add("liquibase");
        if (text.contains("flyway-core")) ids.add("flyway");
        return ids;
    }

    /** M3: reflect the current file's diagnostics in the status bar. */
    private void updateProblemsStatus(
            List<dev.lumina.diagnostics.JavaDiagnostics.Diag> diags) {
        if (statusProblems == null) return;
        long errors = diags.stream()
                .filter(d -> d.severity()
                        == dev.lumina.diagnostics.JavaDiagnostics.Severity.ERROR)
                .count();
        long warnings = diags.size() - errors;
        if (diags.isEmpty()) {
            statusProblems.setText("");
        } else {
            statusProblems.setText(
                    (errors > 0 ? errors + " \u2716  " : "")
                            + (warnings > 0 ? warnings + " \u26A0" : ""));
            statusProblems.setStyle(errors > 0
                    ? "-fx-text-fill: #E5534B;"
                    : "-fx-text-fill: #D8A657;");
        }
    }

    /** M4: quick documentation (Ctrl+Q) for the symbol at the caret. */
    private void quickDocAtCaret() {
        EditorTab editor = currentEditor();
        if (editor == null || editor.getPath() == null) return;
        dev.lumina.semantics.SemanticEngine engine = semantics;
        if (engine == null) {
            console.println("Quick Documentation needs the semantic engine "
                    + "(still indexing\u2026)");
            return;
        }
        final Path file = editor.getPath();
        final String text = editor.getEditorText();
        final int line = editor.getCaretLine();
        final int column = editor.getCaretColumn();
        Thread t = new Thread(() -> {
            dev.lumina.semantics.Docs.DocTarget target = null;
            String doc = null;
            try {
                target = engine.docTargetAt(file, text, line, column);
                if (target != null && target.isProject()) {
                    List<String> lines = target.file().equals(file)
                            ? text.lines().toList()
                            : Files.readAllLines(target.file());
                    doc = dev.lumina.semantics.Docs.javadocAbove(
                            lines, target.line());
                } else if (target != null && target.isLibrary()) {
                    String source = findLibrarySource(target.libraryFqcn());
                    if (source != null) {
                        int declLine = dev.lumina.semantics.Docs.findMemberLine(
                                source, target.member(), target.paramCount());
                        if (declLine > 0) {
                            doc = dev.lumina.semantics.Docs.javadocAbove(
                                    source.lines().toList(), declLine);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            final dev.lumina.semantics.Docs.DocTarget shownTarget = target;
            final String shownDoc = doc;
            Platform.runLater(() -> {
                if (shownTarget == null) return;
                editor.caretScreenBounds().ifPresent(bounds ->
                        docPopup.show(editor.getContent(),
                                shownTarget.signature(), shownDoc, bounds));
            });
        }, "lumina-quick-doc");
        t.setDaemon(true);
        t.start();
    }

    /** M4: parameter info for the call around the caret. */
    private void showParameterInfo() {
        EditorTab editor = currentEditor();
        dev.lumina.semantics.SemanticEngine engine = semantics;
        if (editor == null || editor.getPath() == null || engine == null) {
            return;
        }
        final String text = editor.getEditorText();
        final int caret = editor.getCaretOffset();
        final dev.lumina.semantics.Docs.Call call =
                dev.lumina.semantics.Docs.enclosingCall(text, caret);
        if (call == null) {
            paramPopup.hide();
            return;
        }
        final Path file = editor.getPath();
        final int line = editor.getCaretLine();
        Thread t = new Thread(() -> {
            List<dev.lumina.semantics.Docs.Signature> signatures =
                    engine.signaturesFor(file, text, line,
                            call.receiver(), call.method());
            Platform.runLater(() -> {
                if (signatures.isEmpty()) return;
                editor.caretScreenBounds().ifPresent(bounds ->
                        paramPopup.show(editor.getContent(), signatures,
                                call.argIndex(), call.openParenOffset(),
                                bounds));
            });
        }, "lumina-param-info");
        t.setDaemon(true);
        t.start();
    }

    /** Keep the bold argument in sync while the caret moves inside a call. */
    private void trackParamInfoCaret(EditorTab editor) {
        if (!paramPopup.isShowing()) return;
        String text = editor.getEditorText();
        dev.lumina.semantics.Docs.Call call =
                dev.lumina.semantics.Docs.enclosingCall(
                        text, editor.getCaretOffset());
        if (call == null
                || call.openParenOffset() != paramPopup.openParenOffset()) {
            paramPopup.hide();
        } else {
            paramPopup.updateArgIndex(call.argIndex());
        }
    }

    /**
     * M1: build the semantic engine in the background. Navigation works via
     * the old heuristics until it is ready (IntelliJ's "dumb mode" pattern).
     */
    private volatile List<dev.lumina.spring.SpringConfigMetadata.Property> springProperties
            = List.of();

    private void initSemanticEngine(Path dir) {
        semantics = null;
        springProperties = List.of();
        Thread t = new Thread(() -> {
            console.println("Semantic engine: indexing project\u2026");
            String classpath = ensureClasspath();   // cached in target/lumina.cp
            dev.lumina.semantics.SemanticEngine engine =
                    dev.lumina.semantics.SemanticEngine.create(
                            dir, classpath, console::println);
            if (engine != null && projectRoot != null
                    && projectRoot.equals(dir)) {
                semantics = engine;
                console.println("\u2713 Semantic engine ready \u2014 "
                        + engine.sourceRoots().size() + " source roots, "
                        + engine.jarCount() + " dependency jars. Ctrl+Click and "
                        + "Find Usages are now exact.");
            }
            if (looksLikeSpringBoot(dir) && classpath != null) {
                List<dev.lumina.spring.SpringConfigMetadata.Property> props =
                        dev.lumina.spring.SpringConfigMetadata.scan(classpath);
                if (!props.isEmpty() && projectRoot != null && projectRoot.equals(dir)) {
                    springProperties = props;
                    console.println("\u2713 Spring Boot config completion ready \u2014 "
                            + props.size() + " properties from "
                            + "application.properties/.yml starters on the classpath.");
                }
            }
            // The classpath cache (target/lumina.cp) may have just been built
            // for the first time \u2014 (re)load what it was resolved from and
            // re-check any open pom.xml/build.gradle tab so a stale "Load
            // Maven Changes" hint isn't left showing (or missing).
            if (projectRoot != null && projectRoot.equals(dir)) {
                loadMavenSyncBaseline(dir);
                Platform.runLater(this::refreshMavenSyncForOpenTabs);
            }
        }, "lumina-semantics-init");
        t.setDaemon(true);
        t.start();
    }

    private static boolean looksLikeSpringBoot(Path dir) {
        try {
            for (String name : new String[]{"pom.xml", "build.gradle", "build.gradle.kts"}) {
                Path f = dir.resolve(name);
                if (Files.isRegularFile(f)
                        && Files.readString(f).contains("org.springframework.boot")) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
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
        final String editorText = editor != null ? editor.getEditorText() : null;
        final int caretLine = editor != null ? editor.getCaretLine() : -1;
        final int caretColumn = editor != null ? editor.getCaretColumn() : -1;

        Thread t = new Thread(() -> {
            // (0) M1 semantic engine: exact resolution. Falls through to the
            //     heuristics below whenever the engine is absent or unsure.
            dev.lumina.semantics.SemanticEngine engine = semantics;
            if (engine != null && currentFile != null && caretLine > 0) {
                try {
                    var res = engine.resolveAt(
                            currentFile, editorText, caretLine, caretColumn);
                    switch (res.kind()) {
                        case PROJECT -> {
                            var loc = res.location();
                            Platform.runLater(() ->
                                    openFileAtLineAndSymbol(loc.file(), loc.line(), word, true));
                            return;
                        }
                        case LIBRARY -> {
                            String member = res.member() != null ? res.member() : word;
                            int params = res.paramCount();
                            Platform.runLater(() ->
                                    openLibraryMember(res.libraryFqcn(), member, params));
                            return;
                        }
                        case DECLARATION -> {
                            Platform.runLater(() -> showUsagesPopup(currentEditor(), word, caretLine, caretColumn, null));
                            return;
                        }
                        case NONE -> { /* fall through to heuristics */ }
                    }
                } catch (Throwable fallThrough) {
                    // engine hiccup: heuristics still work below
                }
            }
            // (1) method declaration inside the project (only for real methods)
            if (!looksLikeMethodCall || qualifier == null) {
                Path[] hitFile = new Path[]{null};
                int[] hit = new int[]{-1};
                findInProject(word, currentFile, hitFile, hit);
                if (hitFile[0] != null) {
                    final Path f = hitFile[0];
                    final int ln = hit[0];
                    Platform.runLater(() -> openFileAtLineAndSymbol(f, ln, word, true));
                    return;
                }
            }
            // (2) a project type (the clicked type, or the call's qualifier type)
            if (targetType != null) {
                Path typeFile = findTypeFile(targetType);
                if (typeFile != null) {
                    if (looksLikeMethodCall && !isMethodDeclaredInFile(typeFile, word)) {
                        String superType = findSuperTypeInFile(typeFile);
                        if (superType != null) {
                            String fqcn = resolveImportedFqcn(typeFile, superType);
                            String resolvedFqcn = fqcn != null ? fqcn : superType;
                            Platform.runLater(() -> openLibraryMember(resolvedFqcn, word, -1));
                            return;
                        }
                    }
                    Platform.runLater(() -> openFileAtLineAndSymbol(typeFile, 1, word, true));
                    return;
                }
                // (3) library/JDK type -> decompile from the classpath (like IntelliJ)
                String fqcn = resolveImportedFqcn(currentFile, targetType);
                String resolvedFqcn = fqcn != null ? fqcn : targetType;
                Platform.runLater(() -> openLibraryMember(resolvedFqcn, word, -1));
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

    private boolean isMethodDeclaredInFile(Path file, String methodName) {
        if (file == null || !Files.isRegularFile(file)) return false;
        try {
            String text = Files.readString(file);
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "\\b" + java.util.regex.Pattern.quote(methodName) + "\\s*\\(");
            for (String line : text.split("\n")) {
                String s = line.strip();
                if (s.startsWith("*") || s.startsWith("//") || s.startsWith("return ") || s.contains("." + methodName + "(")) continue;
                if (p.matcher(line).find()) return true;
            }
        } catch (IOException ignored) {}
        return false;
    }

    private String findSuperTypeInFile(Path file) {
        if (file == null || !Files.isRegularFile(file)) return null;
        try {
            String text = Files.readString(file);
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "\\b(extends|implements)\\s+([A-Za-z0-9_<>,\t ]+)");
            var m = p.matcher(text);
            if (m.find()) {
                String raw = m.group(2);
                String first = raw.split("[,<]")[0].trim();
                if (!first.isBlank()) return first;
            }
        } catch (IOException ignored) {}
        return null;
    }

    /**
     * Resolve a library/JDK member like IntelliJ: prefer real *-sources.jar
     * (actual .java with line numbers and persistent disk caching),
     * jump to the exact declaration line, and flash/blink the symbol.
     */
    private List<Path> classpathJars() {
        String cp = ensureClasspath();
        List<Path> jars = new java.util.ArrayList<>();
        if (cp != null && !cp.isBlank()) {
            for (String part : cp.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                if (part.endsWith(".jar")) {
                    Path p = Path.of(part);
                    if (Files.isRegularFile(p)) jars.add(p);
                }
            }
        }
        return jars;
    }

    private void openLibraryMember(String fqcn, String member, int paramCount) {
        if (fqcn == null || fqcn.isBlank()) return;
        Thread t = new Thread(() -> {
            try {
                dev.lumina.semantics.LibrarySourceService service =
                        dev.lumina.semantics.LibrarySourceService.getInstance();
                List<Path> jars = classpathJars();
                String cp = ensureClasspath();

                Path sourceFile = service.getOrResolveSource(
                        fqcn, projectRoot, jars, cp,
                        msg -> Platform.runLater(() -> console.println(msg))
                );

                if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
                    Platform.runLater(() -> console.println(
                            "Could not resolve " + fqcn + ". Run Build Project once so "
                                    + "dependencies are downloaded, then Ctrl+Click again."));
                    return;
                }

                String sourceText = Files.readString(sourceFile);
                int line = dev.lumina.semantics.LibrarySourceService.findMemberLine(sourceText, member, paramCount);

                Platform.runLater(() -> {
                    openFileAtLineAndSymbol(sourceFile, line, member, true);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> console.println(
                        "Resolve failed: " + ex.getMessage()));
            }
        }, "lumina-resolve-lib");
        t.setDaemon(true);
        t.start();
    }

    private void openLibraryType(String fqcn, String word) {
        openLibraryMember(fqcn, word, -1);
    }

    /** Search downloaded *-sources.jar files for fqcn's .java; return its text. */
    private String findLibrarySource(String fqcn) {
        try {
            dev.lumina.semantics.LibrarySourceService service =
                    dev.lumina.semantics.LibrarySourceService.getInstance();
            List<Path> jars = classpathJars();
            String cp = ensureClasspath();
            Path sourceFile = service.getOrResolveSource(
                    fqcn, projectRoot, jars, cp, null);
            if (sourceFile != null && Files.isRegularFile(sourceFile)) {
                return Files.readString(sourceFile);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** All *-sources.jar under the Maven local repo referenced by this project. */
    private List<Path> sourceJars() {
        List<Path> jars = new java.util.ArrayList<>();
        Path m2 = Path.of(System.getProperty("user.home"), ".m2", "repository");
        if (!Files.isDirectory(m2)) return jars;
        try (Stream<Path> walk = Files.walk(m2)) {
            walk.filter(p -> p.getFileName().toString().endsWith("-sources.jar"))
                    .forEach(jars::add);
        } catch (IOException ignored) {
        }
        return jars;
    }

    /**
     * Reliable classpath via dependency:build-classpath (writes a file we read),
     * plus the project's own compiled classes. Cached in target/lumina.cp.
     */
    private String ensureClasspath() {
        List<String> parts = new java.util.ArrayList<>();
        Path classes = projectRoot.resolve("target/classes");
        if (Files.isDirectory(classes)) parts.add(classes.toString());
        Path gradleClasses = projectRoot.resolve("build/classes/java/main");
        if (Files.isDirectory(gradleClasses)) parts.add(gradleClasses.toString());

        if (RunConfiguration.isMavenProject(projectRoot)) {
            Path cpFile = projectRoot.resolve("target/lumina.cp");
            if (!Files.isRegularFile(cpFile)) {
                runBuildToolQuiet(RunConfiguration.maven(projectRoot, "-q",
                        "dependency:build-classpath",
                        "-Dmdep.outputFile=target/lumina.cp"));
                saveMavenSyncBaseline(projectRoot.resolve("pom.xml"), cpFile);
            }
            try {
                if (Files.isRegularFile(cpFile)) {
                    String cp = Files.readString(cpFile).trim();
                    if (!cp.isBlank()) parts.add(cp);
                }
            } catch (IOException ignored) {
            }
        } else if (RunConfiguration.isGradleProject(projectRoot)) {
            Path cpFile = projectRoot.resolve("build/lumina.cp");
            if (!Files.isRegularFile(cpFile)) {
                resolveGradleClasspath(cpFile);
                saveMavenSyncBaseline(firstExistingBuildGradle(projectRoot), cpFile);
            }
            try {
                if (Files.isRegularFile(cpFile)) {
                    String cp = Files.readString(cpFile).trim();
                    if (!cp.isBlank()) parts.add(cp);
                }
            } catch (IOException ignored) {
            }
        }
        return parts.isEmpty() ? null : String.join(File.pathSeparator, parts);
    }

    // --------------------------------------------- "Load Maven Changes" state

    /**
     * Records the exact build-file text that a just-built classpath cache
     * corresponds to, in a sidecar next to it (e.g. {@code
     * target/lumina.cp.src}), so the "Load Maven Changes" hint survives
     * an app restart instead of only living in memory.
     */
    private void saveMavenSyncBaseline(Path buildFile, Path cpFile) {
        try {
            if (buildFile != null && Files.isRegularFile(buildFile)) {
                Files.writeString(sidecarFor(cpFile), Files.readString(buildFile));
            }
        } catch (IOException ignored) {
        }
    }

    private Path sidecarFor(Path cpFile) {
        return cpFile.resolveSibling(cpFile.getFileName() + ".src");
    }

    private static Path firstExistingBuildGradle(Path dir) {
        Path kts = dir.resolve("build.gradle.kts");
        return Files.isRegularFile(kts) ? kts : dir.resolve("build.gradle");
    }

    /**
     * Loads the last-resolved build-file text into {@link #mavenSyncBaselineText}
     * so every open tab can be compared against it live, with no disk I/O
     * per keystroke. Falls back to the file's current content (i.e.
     * "assume already resolved") when no sidecar exists yet, so upgrading
     * Lumina doesn't spuriously flag every already-working project.
     */
    private void loadMavenSyncBaseline(Path dir) {
        mavenSyncBaselineText = null;
        try {
            Path cpFile = null, buildFile = null;
            if (RunConfiguration.isMavenProject(dir)) {
                cpFile = dir.resolve("target/lumina.cp");
                buildFile = dir.resolve("pom.xml");
            } else if (RunConfiguration.isGradleProject(dir)) {
                cpFile = dir.resolve("build/lumina.cp");
                buildFile = firstExistingBuildGradle(dir);
            }
            if (buildFile == null || !Files.isRegularFile(buildFile)) return;
            Path sidecar = sidecarFor(cpFile);
            mavenSyncBaselineText = Files.isRegularFile(sidecar)
                    ? Files.readString(sidecar)
                    : Files.readString(buildFile);
        } catch (IOException ignored) {
        }
    }

    /**
     * Gradle has no built-in "print the classpath" plugin the way Maven
     * does, so this registers a one-off task via an init script (applies
     * to any project using the java plugin, without touching build.gradle)
     * that dumps main's runtime classpath to a file \u2014 the Gradle
     * equivalent of dependency:build-classpath. Without this, Spring Boot
     * imports in a Gradle project never resolve and every file shows
     * "package \u2026 does not exist" in Problems, even though the project
     * itself is fine.
     */
    private void resolveGradleClasspath(Path cpFile) {
        try {
            Path initScript = Files.createTempFile("lumina-classpath", ".gradle");
            Files.writeString(initScript, """
                    allprojects {
                        afterEvaluate { proj ->
                            if (proj.plugins.hasPlugin('java')) {
                                proj.tasks.register('luminaClasspath') {
                                    doLast {
                                        def cp = proj.sourceSets.main.runtimeClasspath.files
                                                .join(File.pathSeparator)
                                        new File(proj.projectDir, 'build/lumina.cp').parentFile.mkdirs()
                                        new File(proj.projectDir, 'build/lumina.cp').text = cp
                                    }
                                }
                            }
                        }
                    }
                    """);
            List<String> cmd = new java.util.ArrayList<>(RunConfiguration.gradleCmd(
                    projectRoot, "-q", "--init-script", initScript.toString(),
                    "luminaClasspath"));
            runBuildToolQuiet(cmd);
            Files.deleteIfExists(initScript);
        } catch (IOException ignored) {
            // no classpath this time; the engine still works on project
            // sources alone and completion/diagnostics degrade gracefully
        }
    }

    private void runBuildToolQuiet(List<String> cmd) {
        try {
            Process p = new ProcessBuilder(cmd)
                    .directory(projectRoot.toFile())
                    .redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            p.waitFor();
        } catch (IOException | InterruptedException ignored) {
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /** Per-project sources marker in ~/.lumina, surviving `mvn clean`. */
    private Path sourcesMarker(Path root) {
        String key = Integer.toHexString(
                root.toAbsolutePath().toString().hashCode());
        return Path.of(System.getProperty("user.home"), ".lumina",
                "sources-" + key + ".done");
    }

    private void deleteFromTree(Path path) {
        if (path == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete");
        confirm.setHeaderText("Delete " + path.getFileName() + "?");
        confirm.setContentText("This cannot be undone.");
        confirm.initOwner(stage);
        confirm.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    if (Files.isDirectory(path)) {
                        try (Stream<Path> walk = Files.walk(path)) {
                            walk.sorted(java.util.Comparator.reverseOrder())
                                    .forEach(p -> p.toFile().delete());
                        }
                    } else {
                        Files.deleteIfExists(path);
                    }
                    closeEditorTabsWhere(t ->
                            t instanceof EditorTab et && path.equals(et.getPath()));
                    fileExplorer.refresh();
                } catch (IOException ex) {
                    error("Could not delete", ex.getMessage());
                }
            }
        });
    }

    private String simpleName(String fqcn) {
        return fqcn.substring(fqcn.lastIndexOf('.') + 1);
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

        // Collect breakpoints from the red gutter dots in all open editors.
        List<String> stops = breakpointStops();
        if (!stops.isEmpty()) {
            console.println("Breakpoints: " + stops.size()
                    + " \u2014 they will be set in jdb automatically.");
        }

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
            try {
                Thread.sleep(2500);   // let jdb finish attaching
            } catch (InterruptedException e) {
                return;
            }
            for (String stop : stops) {
                Platform.runLater(() -> terminal.sendCommand(stop));
                try {
                    Thread.sleep(180);
                } catch (InterruptedException e) {
                    return;
                }
            }
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                return;
            }
            // Resume the suspended VM; it will pause at the first breakpoint.
            Platform.runLater(() -> terminal.sendCommand("cont"));
        }, "lumina-jdb-attach");
        attach.setDaemon(true);
        attach.start();
    }

    /** Red gutter dots across open editors as jdb "stop at" commands. */
    private List<String> breakpointStops() {
        List<String> stops = new java.util.ArrayList<>();
        for (Tab t : allEditorTabs()) {
            if (!(t instanceof EditorTab et) || et.getPath() == null) continue;
            String fqcn = fqcnOf(et.getPath());
            if (fqcn == null) fqcn = testFqcnOf(et.getPath());
            if (fqcn == null) continue;
            for (int line : et.getBreakpoints()) {
                stops.add("stop at " + fqcn + ":" + line);
            }
        }
        return stops;
    }

    private void findInFiles() {
        if (!requireProject()) return;
        new SearchDialog(stage, projectRoot, this::openFileAtLine).show();
    }

    private void openFileAtLine(Path path, int line) {
        openFileAtLineAndSymbol(path, line, null, false);
    }

    private void openFileAtLineAndSymbol(Path path, int line, String symbol, boolean flashAndDoc) {
        EditorTab tab = openFile(path);
        Platform.runLater(() -> {
            EditorTab target = tab != null ? tab : currentEditor();
            if (target != null) {
                if (symbol != null && !symbol.isBlank() && flashAndDoc) {
                    target.flashSymbolAt(line, symbol, true);
                } else {
                    target.goToLine(line);
                }
            }
        });
    }

    private void runBuildGoal(String goal) {
        if (projectRoot == null) return;
        String[] parts = goal.trim().split("\\s+");
        List<String> cmd = RunConfiguration.isMavenProject(projectRoot)
                ? RunConfiguration.maven(projectRoot, parts)
                : RunConfiguration.gradleCmd(projectRoot, parts);
        showRunPanel();
        console.runCommand(goal, cmd, projectRoot);
    }

    // -------------------------------------------------------- Git Actions

    private void addCurrentFile() {
        if (!requireProject()) return;
        EditorTab tab = currentEditor();
        if (tab == null || tab.getPath() == null) {
            error("No Active File", "Open a file to add it to Git.");
            return;
        }
        Path file = tab.getPath();
        String rel = projectRoot.relativize(file).toString().replace('\\', '/');
        GitService.Result r = GitService.add(projectRoot, List.of(rel));
        if (r.ok()) {
            console.println("\u2713 Added " + rel + " to Git stage");
            NotificationService.getInstance().notify(new Notification(
                    "Git", "File Staged", rel, NotificationType.INFORMATION));
            refreshGitInfo();
            if (commitPanel != null) commitPanel.refresh();
        } else {
            error("Git Add Failed", r.output());
        }
    }

    private void commitCurrentFile() {
        if (!requireProject()) return;
        gitCommit();
    }

    private void showDiffCurrentFile() {
        if (!requireProject()) return;
        EditorTab tab = currentEditor();
        if (tab == null || tab.getPath() == null) return;
        Path file = tab.getPath();
        String rel = projectRoot.relativize(file).toString().replace('\\', '/');
        GitService.Result r = GitService.fileContentAtRevision(projectRoot, "HEAD", rel);
        String headText = r.ok() ? r.output() : "";
        String curText = tab.getEditorText();
        openDiffViewer("Diff " + file.getFileName() + " (HEAD vs Working Tree)", "HEAD", rel, file, headText, curText);
    }

    private void showCompareWithRevision() {
        if (!requireProject()) return;
        EditorTab tab = currentEditor();
        if (tab == null || tab.getPath() == null) return;
        new CompareWithRevisionDialog(stage, projectRoot, tab.getPath(), tab.getEditorText(), this::openDiffViewer).show();
    }

    private void showCompareWithBranch() {
        if (!requireProject()) return;
        EditorTab tab = currentEditor();
        if (tab == null || tab.getPath() == null) return;
        new CompareWithBranchDialog(stage, projectRoot, tab.getPath(), tab.getEditorText(), this::openDiffViewer).show();
    }

    private void showFileHistory() {
        if (!requireProject()) return;
        showGitLog();
    }

    private void showSelectionHistory() {
        if (!requireProject()) return;
        showGitLog();
    }

    private void showManageRemotesDialog() {
        if (!requireProject()) return;
        new GitRemotesDialog(stage, projectRoot, msg -> {
            console.println("Git Remotes: " + msg);
            refreshGitInfo();
        }).show();
    }

    private void showCloneRepositoryDialog(String initialTab) {
        Path defaultParent = projectRoot != null && projectRoot.getParent() != null
                ? projectRoot.getParent()
                : Path.of(System.getProperty("user.home"), "projects");
        new CloneRepositoryDialog(stage, defaultParent, initialTab, gitEnv(), clonedDir -> {
            console.println("\u2713 Successfully cloned repository into " + clonedDir);
            NotificationService.getInstance().notify(new Notification(
                    "Git", "Repository Cloned", clonedDir.toString(), NotificationType.INFORMATION));
            openProjectInteractive(clonedDir);
        }).show();
    }

    private void gitClone() {
        showCloneRepositoryDialog(null);
    }

    private void shareProjectOnGitLab() {
        if (!requireProject()) return;
        var def = GitLabAccountManager.getInstance().getDefaultAccount();
        if (def == null) {
            new AddGitLabAccountDialog(stage).showAndWait();
            def = GitLabAccountManager.getInstance().getDefaultAccount();
            if (def == null) return;
        }
        new ShareProjectDialog(stage, ShareProjectDialog.Service.GITLAB, projectRoot,
                def.getUsername(), gitEnv(), url -> {
            console.println("\u2713 Shared project on GitLab: " + url);
            refreshGitInfo();
            openBrowser(url);
        }).show();
    }

    private void shareProjectOnGitHub() {
        if (!requireProject()) return;
        var def = GitHubAccountManager.getInstance().getDefaultAccount();
        if (def == null) {
            new AddGitHubAccountDialog(stage, false).showAndWait();
            def = GitHubAccountManager.getInstance().getDefaultAccount();
            if (def == null) return;
        }
        new ShareProjectDialog(stage, ShareProjectDialog.Service.GITHUB, projectRoot,
                def.getUsername(), gitEnv(), url -> {
            console.println("\u2713 Shared project on GitHub: " + url);
            refreshGitInfo();
            openBrowser(url);
        }).show();
    }

    private void syncFork() {
        if (!requireProject()) return;
        List<String> remotes = GitService.remotes(projectRoot);
        String remote = remotes.contains("upstream") ? "upstream" : "origin";
        String branch = GitService.currentBranch(projectRoot);
        if (branch == null) branch = "master";
        final String syncBranch = branch;
        console.println("Syncing fork with " + remote + "/" + syncBranch + "\u2026");
        Thread t = new Thread(() -> {
            GitService.fetch(projectRoot, remote, gitEnv());
            GitService.Result r = GitService.merge(projectRoot, remote + "/" + syncBranch, null, null, gitEnv());
            Platform.runLater(() -> {
                if (r.ok()) {
                    console.println("\u2713 Fork synchronized with " + remote + "/" + syncBranch);
                    NotificationService.getInstance().notify(new Notification(
                            "Git", "Fork Synchronized", "Merged " + remote + "/" + syncBranch, NotificationType.INFORMATION));
                    refreshGitInfo();
                } else {
                    console.println("Sync fork result: " + r.output());
                }
            });
        }, "lumina-sync-fork");
        t.setDaemon(true);
        t.start();
    }

    private void createGist() {
        EditorTab tab = currentEditor();
        String fileName = tab != null && tab.getPath() != null ? tab.getPath().getFileName().toString() : "snippet.txt";
        String content = tab != null ? (tab.getSelectedText().isEmpty() ? tab.getEditorText() : tab.getSelectedText()) : "";
        new CreateGistDialog(stage, fileName, content, gistUrl -> {
            console.println("\u2713 Created Gist: " + gistUrl);
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(gistUrl);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            NotificationService.getInstance().notify(new Notification(
                    "GitHub", "Gist Created", "Link copied to clipboard", NotificationType.INFORMATION));
            openBrowser(gistUrl);
        }).show();
    }

    private void viewInBrowser() {
        if (!requireProject()) return;
        String repoUrl = GitService.remoteBrowserUrl(projectRoot);
        if (repoUrl == null) {
            error("No Remote", "This repository has no 'origin' remote URL configured.");
            return;
        }
        EditorTab tab = currentEditor();
        if (tab != null && tab.getPath() != null) {
            String rel = projectRoot.relativize(tab.getPath()).toString().replace('\\', '/');
            String branch = GitService.currentBranch(projectRoot);
            if (branch == null) branch = "master";
            int line = tab.getCaretLine();
            String fileUrl = repoUrl + "/blob/" + branch + "/" + rel + (line > 0 ? "#L" + line : "");
            openBrowser(fileUrl);
        } else {
            openBrowser(repoUrl);
        }
    }

    private void createPullRequest() {
        if (!requireProject()) return;
        String repoUrl = GitService.remoteBrowserUrl(projectRoot);
        if (repoUrl != null) {
            String branch = GitService.currentBranch(projectRoot);
            String prUrl = repoUrl + (branch != null ? "/compare/" + branch + "?expand=1" : "/pulls");
            openBrowser(prUrl);
        } else {
            error("No Remote", "Repository has no remote configured for pull requests.");
        }
    }

    private void viewPullRequests() {
        if (!requireProject()) return;
        String repoUrl = GitService.remoteBrowserUrl(projectRoot);
        if (repoUrl != null) {
            openBrowser(repoUrl + "/pulls");
        } else {
            error("No Remote", "Repository has no remote configured.");
        }
    }

    private void showRightPanel(int tabIndex) {
        toggleRightPanel(true);
        rightTabs.getSelectionModel().select(tabIndex);
        rightRail.select(tabIndex);
    }

    private void toggleRightPanel(boolean show) {
        if (show && !outerSplit.getItems().contains(rightDock)) {
            outerSplit.getItems().add(rightDock);
            outerSplit.setDividerPositions(0.74);
        } else if (!show) {
            outerSplit.getItems().remove(rightDock);
            rightRail.clearSelection();
        }
    }

    /**
     * A rail icon click either opens that tool (switching to it if the
     * panel is already open on a different one), or \u2014 clicked a second
     * time on the tool that's already showing \u2014 closes the panel, exactly
     * like clicking an already-active IntelliJ tool-window icon.
     */
    private void onRightRailSelect(int index) {
        boolean alreadyShowingThis = outerSplit.getItems().contains(rightDock)
                && rightTabs.getSelectionModel().getSelectedIndex() == index;
        if (alreadyShowingThis) {
            toggleRightPanel(false);
        } else {
            showRightPanel(index);
        }
    }

    // ---------------------------------------------------------- tool windows

    /** Tests opens the bottom panel without a dedicated rail icon (same as
     *  IntelliJ: it's triggered by running tests, not clicked open) \u2014 but
     *  Run does have one now, so a run highlights it like any other rail
     *  selection would. */
    private void showRunPanel() {
        toggleBottomPanel(true);
        bottomTabs.getSelectionModel().select(0);
        iconRail.selectBottom(0);
    }

    private void showTerminal() {
        toggleBottomPanel(true);
        bottomTabs.getSelectionModel().select(3);
        iconRail.selectBottom(4);
        ensureTerminalSession();
        terminal.focusInput();
    }

    public void showGitLog() {
        toggleBottomPanel(true);
        bottomTabs.getSelectionModel().select(4);
        iconRail.selectBottom(6);
        if (gitLogPanel != null) {
            gitLogPanel.selectLogTab();
            gitLogPanel.refresh();
        }
    }

    private void openGitCommitFileDiff(String hash, String relPath) {
        Path root = projectRoot;
        if (root == null || hash == null || relPath == null) return;
        Path localPath = root.resolve(relPath);
        String shortHash = hash.length() > 7 ? hash.substring(0, 7) : hash;
        String fileName = localPath.getFileName() != null ? localPath.getFileName().toString() : relPath;
        String title = fileName + " (" + shortHash + ")";

        Thread t = new Thread(() -> {
            String commitText = GitService.getFileContentAtCommit(root, hash, relPath);
            String parentText = GitService.getFileContentAtCommit(root, hash + "^1", relPath);
            Platform.runLater(() -> {
                openDiffViewer(title, hash, relPath, localPath, parentText, commitText);
            });
        }, "lumina-git-diff-loader");
        t.setDaemon(true);
        t.start();
    }

    private void toggleBottomPanel(boolean show) {
        if (show && !verticalSplit.getItems().contains(bottomTabs)) {
            verticalSplit.getItems().add(bottomTabs);
            verticalSplit.setDividerPositions(0.70);
        } else if (!show) {
            verticalSplit.getItems().remove(bottomTabs);
            iconRail.clearBottomSelection();
        }
    }

    private void toggleMaximizeBottomPanel() {
        if (verticalSplit != null && !verticalSplit.getDividers().isEmpty()) {
            double current = verticalSplit.getDividerPositions()[0];
            verticalSplit.setDividerPositions(current < 0.20 ? 0.70 : 0.05);
        }
    }

    /** Bottom rail: 0=Run, 1=Build, 2=GitHub Copilot MCP Log, 3=Services,
     *  4=Terminal, 5=Problems, 6=Git \u2014 mapped onto the bottom dock's
     *  actual tab indices. */
    private void onBottomRailSelect(int railIndex) {
        int tabIndex = switch (railIndex) {
            case 0 -> 0;   // Run
            case 1 -> 5;   // Build
            case 2 -> mcpBottomTab != null ? bottomTabs.getTabs().indexOf(mcpBottomTab) : 7;   // GitHub Copilot MCP Log
            case 3 -> 6;   // Services
            case 4 -> 3;   // Terminal
            case 5 -> 2;   // Problems
            default -> 4;  // Git
        };
        if (railIndex == 2 && tabIndex < 0) {
            String pos = mcpLogPanel.getActiveMoveToPosition();
            if ("Right".equalsIgnoreCase(pos)) {
                boolean alreadyShowing = outerSplit.getItems().contains(rightDock)
                        && rightTabs.getSelectionModel().getSelectedItem() == mcpRightTab;
                if (alreadyShowing) {
                    toggleRightPanel(false);
                } else {
                    toggleRightPanel(true);
                    rightTabs.getSelectionModel().select(mcpRightTab);
                }
            } else if ("Left".equalsIgnoreCase(pos)) {
                boolean alreadyShowing = horizontalSplit.getItems().contains(leftDock)
                        && leftTabs.getSelectionModel().getSelectedItem() == mcpLeftTab;
                if (alreadyShowing) {
                    toggleLeftPanel(false);
                } else {
                    toggleLeftPanel(true);
                    leftTabs.getSelectionModel().select(mcpLeftTab);
                }
            }
            return;
        }
        boolean alreadyShowingThis = verticalSplit.getItems().contains(bottomTabs)
                && bottomTabs.getSelectionModel().getSelectedIndex() == tabIndex;
        if (alreadyShowingThis) {
            toggleBottomPanel(false);
            return;
        }
        toggleBottomPanel(true);
        bottomTabs.getSelectionModel().select(tabIndex);
        iconRail.selectBottom(railIndex);
        if (railIndex == 4) {
            ensureTerminalSession();
            terminal.focusInput();
        }
        if (railIndex == 6) gitLogPanel.refresh();
    }

    private void hideMcpPanel() {
        String pos = mcpLogPanel.getActiveMoveToPosition();
        if ("Right".equalsIgnoreCase(pos)) {
            toggleRightPanel(false);
        } else if ("Left".equalsIgnoreCase(pos)) {
            toggleLeftPanel(false);
        } else {
            toggleBottomPanel(false);
        }
    }

    private void moveMcpPanel(String position) {
        if ("Right".equalsIgnoreCase(position)) {
            bottomTabs.getTabs().remove(mcpBottomTab);
            if (mcpLeftTab != null) leftTabs.getTabs().remove(mcpLeftTab);

            if (mcpRightTab == null) {
                mcpRightTab = toolTab("GitHub Copilot MCP Log", mcpLogPanel);
            }
            if (!rightTabs.getTabs().contains(mcpRightTab)) {
                rightTabs.getTabs().add(mcpRightTab);
            }
            mcpLogPanel.setActiveMoveToPosition("Right");
            toggleRightPanel(true);
            rightTabs.getSelectionModel().select(mcpRightTab);
            if (bottomTabs.getTabs().isEmpty() || bottomTabs.getSelectionModel().getSelectedItem() == mcpBottomTab) {
                toggleBottomPanel(false);
            }
        } else if ("Left".equalsIgnoreCase(position)) {
            bottomTabs.getTabs().remove(mcpBottomTab);
            if (mcpRightTab != null) rightTabs.getTabs().remove(mcpRightTab);

            if (mcpLeftTab == null) {
                mcpLeftTab = toolTab("GitHub Copilot MCP Log", mcpLogPanel);
            }
            if (!leftTabs.getTabs().contains(mcpLeftTab)) {
                leftTabs.getTabs().add(mcpLeftTab);
            }
            mcpLogPanel.setActiveMoveToPosition("Left");
            toggleLeftPanel(true);
            leftTabs.getSelectionModel().select(mcpLeftTab);
            if (bottomTabs.getTabs().isEmpty() || bottomTabs.getSelectionModel().getSelectedItem() == mcpBottomTab) {
                toggleBottomPanel(false);
            }
        } else { // "Bottom"
            if (mcpRightTab != null) rightTabs.getTabs().remove(mcpRightTab);
            if (mcpLeftTab != null) leftTabs.getTabs().remove(mcpLeftTab);

            if (!bottomTabs.getTabs().contains(mcpBottomTab)) {
                bottomTabs.getTabs().add(mcpBottomTab);
            }
            mcpLogPanel.setActiveMoveToPosition("Bottom");
            toggleBottomPanel(true);
            bottomTabs.getSelectionModel().select(mcpBottomTab);
            iconRail.selectBottom(2);
        }
    }

    /** Reopening the Terminal tool window after every session tab was
     *  closed shouldn't leave you staring at just the "+" button \u2014 spin
     *  up a fresh session automatically, same as IntelliJ does. Only fires
     *  when there's truly no active session; an already-running one is
     *  left alone rather than being restarted every time you switch back
     *  to it. */
    private void ensureTerminalSession() {
        if (!terminal.hasSession()) {
            terminal.start(projectRoot != null ? projectRoot
                    : Path.of(System.getProperty("user.home")));
        }
    }

    private void toggleProjectPanel(boolean show) {
        toggleLeftPanel(show);
        if (show) leftTabs.getSelectionModel().select(0);
    }

    private void toggleLeftPanel(boolean show) {
        if (show && !horizontalSplit.getItems().contains(leftDock)) {
            horizontalSplit.getItems().add(0, leftDock);
            horizontalSplit.setDividerPositions(0.22);
        } else if (!show) {
            horizontalSplit.getItems().remove(leftDock);
            iconRail.clearTopSelection();
        }
    }

    /** Left rail: 0=Project, 1=Commit, 2=Pull Requests, 3=Structure. */
    private void onLeftRailSelect(int index) {
        boolean alreadyShowingThis = horizontalSplit.getItems().contains(leftDock)
                && leftTabs.getSelectionModel().getSelectedIndex() == index;
        if (alreadyShowingThis) {
            toggleLeftPanel(false);
            return;
        }
        toggleLeftPanel(true);
        leftTabs.getSelectionModel().select(index);
        iconRail.selectTop(index);
    }

    /** Refreshes whichever left tool window just became visible. */
    private void refreshLeftPanel() {
        switch (leftTabs.getSelectionModel().getSelectedIndex()) {
            case 1 -> commitPanel.refresh();
            case 2 -> pullRequestsPanel.refresh();
            case 3 -> refreshStructurePanel();
            default -> { }
        }
    }

    private void refreshStructurePanel() {
        EditorTab editor = currentEditor();
        if (editor == null || editor.getPath() == null) {
            structurePanel.showOutline(null, null);
            return;
        }
        String name = editor.getPath().getFileName() != null
                ? editor.getPath().getFileName().toString() : "";
        structurePanel.showOutline(name, editor.getEditorText());
    }

    private void showMoreToolWindows() {
        ContextMenu menu = new ContextMenu(
                item("Bookmarks", "Shortcut+2", e -> showComingSoon("Bookmarks")),
                item("Find", "Shortcut+3", e -> goToFile()),
                item("Debug", "Shortcut+5", e -> onBottomRailSelect(0)),
                item("Spring", null, e -> showComingSoon("Spring")),
                item("Coverage", null, e -> showComingSoon("Coverage")),
                item("GitHub Copilot Multiple Code Suggestions", null,
                        e -> showComingSoon("GitHub Copilot")),
                item("Hierarchy", null, e -> showComingSoon("Hierarchy")),
                item("Learn", null, e -> showComingSoon("Learn")),
                item("Persistence", null, e -> showComingSoon("Persistence")),
                item("Profiler", null, e -> showComingSoon("Profiler")),
                item("TODO", null, e -> showTodoList()));
        menu.show(iconRail, javafx.geometry.Side.RIGHT, 0, -220);
    }

    /** Scans open project .java files for TODO/FIXME comments. */
    private void showTodoList() {
        if (projectRoot == null) {
            showComingSoon("TODO");
            return;
        }
        Thread t = new Thread(() -> {
            List<String> hits = new java.util.ArrayList<>();
            try (Stream<Path> paths = Files.walk(projectRoot)) {
                paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                    try {
                        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                        for (int i = 0; i < lines.size(); i++) {
                            String l = lines.get(i);
                            if (l.contains("TODO") || l.contains("FIXME")) {
                                hits.add(projectRoot.relativize(p) + ":" + (i + 1) + "  "
                                        + l.trim());
                            }
                        }
                    } catch (IOException ignored) {
                    }
                });
            } catch (IOException ignored) {
            }
            Platform.runLater(() -> {
                showRunPanel();
                console.println("--- TODO / FIXME (" + hits.size() + ") ---");
                hits.forEach(console::println);
            });
        }, "lumina-todo-scan");
        t.setDaemon(true);
        t.start();
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

    /**
     * Bottom of the window: an IntelliJ-style "background task" row (shown
     * only while Maven/Gradle dependencies are being resolved) stacked
     * above the permanent status bar.
     */
    private VBox buildBottomArea() {
        mavenProgressLabel = new Label("");
        mavenProgressBarNode = new ProgressBar();
        mavenProgressBarNode.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        mavenProgressBarNode.setPrefWidth(160);
        mavenProgressBarNode.getStyleClass().add("maven-progress-bar");

        Button cancel = new Button("\u2715");
        cancel.getStyleClass().add("maven-progress-cancel");
        cancel.setTooltip(new Tooltip("Cancel"));
        cancel.setOnAction(e -> cancelMavenReload());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        mavenProgressRow = new HBox(10, mavenProgressLabel, mavenProgressBarNode, spacer, cancel);
        mavenProgressRow.getStyleClass().add("maven-progress-row");
        mavenProgressRow.setAlignment(Pos.CENTER_LEFT);
        mavenProgressRow.setPadding(new Insets(4, 12, 4, 12));
        mavenProgressRow.setVisible(false);
        mavenProgressRow.setManaged(false);

        return new VBox(mavenProgressRow, buildStatusBar());
    }

    /** Show/hide the background-task row at the bottom of the window. */
    private void showMavenSyncProgress(boolean show, String message) {
        if (mavenProgressRow == null) return;
        mavenProgressRow.setVisible(show);
        mavenProgressRow.setManaged(show);
        if (show) mavenProgressLabel.setText(message);
    }

    private void cancelMavenReload() {
        console.stopProcess();
        showMavenSyncProgress(false, null);
        console.println("\u25A0 Dependency resolution cancelled");
    }

    private HBox buildStatusBar() {
        breadcrumbBar = new HBox(4);
        breadcrumbBar.setAlignment(Pos.CENTER_LEFT);
        updateBreadcrumbs(null, null);

        statusProblems = new Label("");
        statusProblems.getStyleClass().add("status-problems");
        statusProblems.setOnMouseClicked(e -> {
            if (!verticalSplit.getItems().contains(bottomTabs)) {
                verticalSplit.getItems().add(bottomTabs);
                verticalSplit.setDividerPositions(0.68);
            }
            bottomTabs.getSelectionModel().select(2);   // Problems
        });
        statusCaret = new Label("");
        Label brand = new Label("Lumina 1.24");
        brand.getStyleClass().add("status-brand");

        gitProgressLabel = new Label("Fetching\u2026");
        gitProgressLabel.setStyle("-fx-text-fill: #A8ADBD; -fx-font-size: 11px;");

        gitProgressBar = new ProgressBar();
        gitProgressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        gitProgressBar.setPrefWidth(110);
        gitProgressBar.setPrefHeight(4);
        gitProgressBar.setMaxHeight(4);
        gitProgressBar.getStyleClass().add("git-status-progress-bar");

        gitProgressCancel = new Button("✕");
        gitProgressCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 10px; -fx-padding: 0 4; -fx-cursor: hand;");
        gitProgressCancel.setTooltip(new Tooltip("Cancel"));
        gitProgressCancel.setOnMouseEntered(e -> gitProgressCancel.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 10px; -fx-padding: 0 4; -fx-cursor: hand; -fx-background-radius: 3;"));
        gitProgressCancel.setOnMouseExited(e -> gitProgressCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C8E94; -fx-font-size: 10px; -fx-padding: 0 4; -fx-cursor: hand;"));

        gitProgressBox = new HBox(6, gitProgressLabel, gitProgressBar, gitProgressCancel);
        gitProgressBox.setAlignment(Pos.CENTER_LEFT);
        gitProgressBox.setVisible(false);
        gitProgressBox.setManaged(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(16, breadcrumbBar, spacer, gitProgressBox, statusProblems, statusCaret, brand);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(3, 12, 3, 12));
        return bar;
    }

    private void showGitProgress(boolean show, String text, Runnable onCancel) {
        Platform.runLater(() -> {
            if (gitProgressBox == null) return;
            gitProgressBox.setVisible(show);
            gitProgressBox.setManaged(show);
            if (show) {
                gitProgressLabel.setText(text != null ? text : "Working\u2026");
                gitProgressCancel.setOnAction(e -> {
                    if (onCancel != null) onCancel.run();
                });
            }
        });
    }

    private void gitFetch() {
        if (!requireProject()) return;
        Thread t = new Thread(() -> {
            try {
                showGitProgress(true, "Fetching\u2026", () -> {
                    if (activeGitTaskProcess != null && activeGitTaskProcess.isAlive()) {
                        activeGitTaskProcess.destroyForcibly();
                    }
                    showGitProgress(false, null, null);
                });
                activeGitTaskProcess = GitService.startProcess(projectRoot, gitEnv(), "fetch", "--all", "--prune");
                String out = new String(activeGitTaskProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int code = activeGitTaskProcess.waitFor();
                Platform.runLater(() -> {
                    showGitProgress(false, null, null);
                    boolean ok = code == 0;
                    Notification notif = new Notification(
                            "git.fetch",
                            "Git Fetch",
                            ok ? "Fetch successful" : ("Fetch failed:\n" + out),
                            ok ? NotificationType.INFORMATION : NotificationType.ERROR
                    );
                    NotificationService.getInstance().notify(notif);
                    if (ok) {
                        if (gitLogPanel != null) gitLogPanel.refresh();
                        refreshGitInfo();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showGitProgress(false, null, null);
                    Notification notif = new Notification(
                            "git.fetch",
                            "Git Fetch",
                            "Fetch error: " + ex.getMessage(),
                            NotificationType.ERROR
                    );
                    NotificationService.getInstance().notify(notif);
                });
            }
        }, "lumina-git-fetch");
        t.setDaemon(true);
        t.start();
    }

    private void showPullDialog() {
        if (!requireProject()) return;
        PullDialog dialog = new PullDialog(stage, projectRoot, this::gitPull);
        dialog.show();
    }

    private void gitPull(String remote, String branch, List<String> options) {
        if (!requireProject()) return;
        Thread t = new Thread(() -> {
            try {
                showGitProgress(true, "Pulling\u2026", () -> {
                    if (activeGitTaskProcess != null && activeGitTaskProcess.isAlive()) {
                        activeGitTaskProcess.destroyForcibly();
                    }
                    showGitProgress(false, null, null);
                });
                List<String> args = new ArrayList<>();
                args.add("pull");
                if (options != null) args.addAll(options);
                if (remote != null && !remote.isBlank()) args.add(remote);
                if (branch != null && !branch.isBlank()) args.add(branch);

                activeGitTaskProcess = GitService.startProcess(projectRoot, gitEnv(), args.toArray(new String[0]));
                String out = new String(activeGitTaskProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int code = activeGitTaskProcess.waitFor();
                Platform.runLater(() -> {
                    showGitProgress(false, null, null);
                    boolean ok = code == 0;
                    String message = out.isBlank() ? "Pull successful" : out.trim();
                    Notification notif = new Notification(
                            "git.pull",
                            "Git Pull",
                            ok ? message : ("Pull failed:\n" + out),
                            ok ? NotificationType.INFORMATION : NotificationType.ERROR
                    );
                    NotificationService.getInstance().notify(notif);
                    if (ok) {
                        if (gitLogPanel != null) gitLogPanel.refresh();
                        refreshGitInfo();
                        if (fileExplorer != null) fileExplorer.refresh();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showGitProgress(false, null, null);
                    Notification notif = new Notification(
                            "git.pull",
                            "Git Pull",
                            "Pull error: " + ex.getMessage(),
                            NotificationType.ERROR
                    );
                    NotificationService.getInstance().notify(notif);
                });
            }
        }, "lumina-git-pull");
        t.setDaemon(true);
        t.start();
    }

    private void showMergeDialog() {
        showMergeDialog(null);
    }

    private void showMergeDialog(String preselectedBranch) {
        if (!requireProject()) return;
        MergeDialog dialog = new MergeDialog(stage, projectRoot, preselectedBranch, this::gitMerge);
        dialog.show();
    }

    private void gitMerge(String branch, List<String> options, String commitMessage) {
        if (!requireProject()) return;
        Thread t = new Thread(() -> {
            try {
                showGitProgress(true, "Merging\u2026", () -> {
                    if (activeGitTaskProcess != null && activeGitTaskProcess.isAlive()) {
                        activeGitTaskProcess.destroyForcibly();
                    }
                    showGitProgress(false, null, null);
                });
                List<String> args = new ArrayList<>();
                args.add("merge");
                if (options != null) {
                    for (String opt : options) {
                        if (opt != null && !opt.isBlank()) args.add(opt.trim());
                    }
                }
                if (commitMessage != null && !commitMessage.isBlank()) {
                    args.add("-m");
                    args.add(commitMessage.trim());
                }
                if (branch != null && !branch.isBlank()) {
                    args.add(branch.trim());
                }

                activeGitTaskProcess = GitService.startProcess(projectRoot, gitEnv(), args.toArray(new String[0]));
                String out = new String(activeGitTaskProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int code = activeGitTaskProcess.waitFor();
                Platform.runLater(() -> {
                    showGitProgress(false, null, null);
                    boolean ok = code == 0;
                    String message = out.isBlank() ? "Merge successful" : out.trim();
                    Notification notif = new Notification(
                            "git.merge",
                            "Git Merge",
                            ok ? message : ("Merge failed:\n" + out),
                            ok ? NotificationType.INFORMATION : NotificationType.ERROR
                    );
                    NotificationService.getInstance().notify(notif);
                    if (ok) {
                        if (gitLogPanel != null) gitLogPanel.refresh();
                        refreshGitInfo();
                        if (fileExplorer != null) fileExplorer.refresh();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showGitProgress(false, null, null);
                    Notification notif = new Notification(
                            "git.merge",
                            "Git Merge",
                            "Merge error: " + ex.getMessage(),
                            NotificationType.ERROR
                    );
                    NotificationService.getInstance().notify(notif);
                });
            }
        }, "lumina-git-merge");
        t.setDaemon(true);
        t.start();
    }

    private void showRebaseDialog() {
        showRebaseDialog(null);
    }

    private void showRebaseDialog(String preselectedTarget) {
        if (!requireProject()) return;
        RebaseDialog dialog = new RebaseDialog(stage, projectRoot, preselectedTarget, this::gitRebase);
        dialog.show();
    }

    private void gitRebase(String branchOrHash, String ontoBranch, String branchToRebase, List<String> options) {
        if (!requireProject()) return;
        Thread t = new Thread(() -> {
            try {
                showGitProgress(true, "Rebasing\u2026", () -> {
                    if (activeGitTaskProcess != null && activeGitTaskProcess.isAlive()) {
                        activeGitTaskProcess.destroyForcibly();
                    }
                    showGitProgress(false, null, null);
                });
                List<String> args = new ArrayList<>();
                args.add("rebase");
                if (options != null) {
                    for (String opt : options) {
                        if (opt != null && !opt.isBlank()) args.add(opt.trim());
                    }
                }
                if (ontoBranch != null && !ontoBranch.isBlank()) {
                    args.add("--onto");
                    args.add(ontoBranch.trim());
                }
                if (branchOrHash != null && !branchOrHash.isBlank()) {
                    args.add(branchOrHash.trim());
                }
                if (branchToRebase != null && !branchToRebase.isBlank()) {
                    args.add(branchToRebase.trim());
                }

                activeGitTaskProcess = GitService.startProcess(projectRoot, gitEnv(), args.toArray(new String[0]));
                String out = new String(activeGitTaskProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int code = activeGitTaskProcess.waitFor();
                Platform.runLater(() -> {
                    showGitProgress(false, null, null);
                    boolean ok = code == 0;
                    String message = out.isBlank() ? "Rebase successful" : out.trim();
                    Notification notif = new Notification(
                            "git.rebase",
                            "Git Rebase",
                            ok ? message : ("Rebase failed:\n" + out),
                            ok ? NotificationType.INFORMATION : NotificationType.ERROR
                    );
                    NotificationService.getInstance().notify(notif);
                    if (ok) {
                        if (gitLogPanel != null) gitLogPanel.refresh();
                        refreshGitInfo();
                        if (fileExplorer != null) fileExplorer.refresh();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showGitProgress(false, null, null);
                    Notification notif = new Notification(
                            "git.rebase",
                            "Git Rebase",
                            "Rebase error: " + ex.getMessage(),
                            NotificationType.ERROR
                    );
                    NotificationService.getInstance().notify(notif);
                });
            }
        }, "lumina-git-rebase");
        t.setDaemon(true);
        t.start();
    }

    private void showNewTagDialog() {
        showNewTagDialog(null, null);
    }

    private void showNewTagDialog(String prefilledCommit, String prefilledTag) {
        if (!requireProject()) return;
        TagDialog dialog = new TagDialog(stage, projectRoot, prefilledCommit, prefilledTag, this::gitCreateTag);
        dialog.show();
    }

    private void gitCreateTag(String tagName, String commit, String message, boolean force) {
        if (!requireProject()) return;
        Thread t = new Thread(() -> {
            try {
                showGitProgress(true, "Creating Tag\u2026", () -> {
                    if (activeGitTaskProcess != null && activeGitTaskProcess.isAlive()) {
                        activeGitTaskProcess.destroyForcibly();
                    }
                    showGitProgress(false, null, null);
                });
                List<String> args = new ArrayList<>();
                args.add("tag");
                if (force) {
                    args.add("-f");
                }
                if (message != null && !message.isBlank()) {
                    args.add("-a");
                    args.add("-m");
                    args.add(message.trim());
                }
                if (tagName != null && !tagName.isBlank()) {
                    args.add(tagName.trim());
                }
                if (commit != null && !commit.isBlank()) {
                    args.add(commit.trim());
                }

                activeGitTaskProcess = GitService.startProcess(projectRoot, gitEnv(), args.toArray(new String[0]));
                String out = new String(activeGitTaskProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int code = activeGitTaskProcess.waitFor();
                Platform.runLater(() -> {
                    showGitProgress(false, null, null);
                    boolean ok = code == 0;
                    String msg = ok ? ("Created tag '" + tagName + "'") : ("Create tag failed:\n" + out);
                    Notification notif = new Notification(
                            "git.tag",
                            "Git Tag",
                            msg,
                            ok ? NotificationType.INFORMATION : NotificationType.ERROR
                    );
                    NotificationService.getInstance().notify(notif);
                    if (ok) {
                        if (gitLogPanel != null) gitLogPanel.refresh();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showGitProgress(false, null, null);
                    Notification notif = new Notification(
                            "git.tag",
                            "Git Tag",
                            "Create tag error: " + ex.getMessage(),
                            NotificationType.ERROR
                    );
                    NotificationService.getInstance().notify(notif);
                });
            }
        }, "lumina-git-tag");
        t.setDaemon(true);
        t.start();
    }

    private void showResetHeadDialog() {
        showResetHeadDialog(null);
    }

    private void showResetHeadDialog(String targetCommit) {
        if (!requireProject()) return;
        ResetHeadDialog dialog = new ResetHeadDialog(stage, projectRoot, targetCommit, this::gitResetHead);
        dialog.show();
    }

    private void gitResetHead(String resetType, String commit) {
        if (!requireProject()) return;
        Thread t = new Thread(() -> {
            try {
                showGitProgress(true, "Resetting HEAD\u2026", () -> {
                    if (activeGitTaskProcess != null && activeGitTaskProcess.isAlive()) {
                        activeGitTaskProcess.destroyForcibly();
                    }
                    showGitProgress(false, null, null);
                });
                List<String> args = new ArrayList<>();
                args.add("reset");
                String type = (resetType != null && !resetType.isBlank()) ? resetType.trim().toLowerCase() : "mixed";
                if ("hard".equals(type)) {
                    args.add("--hard");
                } else if ("soft".equals(type)) {
                    args.add("--soft");
                } else {
                    args.add("--mixed");
                }
                String target = (commit != null && !commit.isBlank()) ? commit.trim() : "HEAD";
                args.add(target);

                activeGitTaskProcess = GitService.startProcess(projectRoot, gitEnv(), args.toArray(new String[0]));
                String out = new String(activeGitTaskProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int code = activeGitTaskProcess.waitFor();
                Platform.runLater(() -> {
                    showGitProgress(false, null, null);
                    boolean ok = code == 0;
                    String msg = ok ? ("Reset HEAD to '" + target + "' (" + resetType + ")") : ("Reset HEAD failed:\n" + out);
                    Notification notif = new Notification(
                            "git.reset",
                            "Git Reset HEAD",
                            msg,
                            ok ? NotificationType.INFORMATION : NotificationType.ERROR
                    );
                    NotificationService.getInstance().notify(notif);
                    if (ok) {
                        if (gitLogPanel != null) gitLogPanel.refresh();
                        refreshGitInfo();
                        if (fileExplorer != null) fileExplorer.refresh();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showGitProgress(false, null, null);
                    Notification notif = new Notification(
                            "git.reset",
                            "Git Reset HEAD",
                            "Reset HEAD error: " + ex.getMessage(),
                            NotificationType.ERROR
                    );
                    NotificationService.getInstance().notify(notif);
                });
            }
        }, "lumina-git-reset");
        t.setDaemon(true);
        t.start();
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
                Platform.runLater(() -> openProjectInteractive(dir));
            } catch (IOException ex) {
                console.println("\u2717 " + ex.getMessage());
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.initOwner(stage);
                    alert.setTitle("Couldn't Create Project");
                    alert.setHeaderText("Failed to create \u201c" + spec.name() + "\u201d");
                    alert.setContentText(ex.getMessage());
                    alert.getDialogPane().getStylesheets().add(getClass()
                            .getResource("/css/lumina-dark.css").toExternalForm());
                    alert.showAndWait();
                });
            }
        }, "lumina-project-generator");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * IntelliJ-style gate in front of every "open this directory as a
     * project" action (new project creation, Open Folder, git clone,
     * Recent Projects). When this window has nothing open yet, the project
     * opens here immediately \u2014 no prompt, exactly like a fresh IntelliJ
     * window. Otherwise it asks Cancel / New Window / This Window, and
     * remembers the choice if "Don't ask again" was checked.
     */
    private void openProjectInteractive(Path dir) {
        Path target = dir.toAbsolutePath().normalize();
        if (projectRoot == null) {
            openProject(dir);
            return;
        }
        if (target.equals(projectRoot.toAbsolutePath().normalize())) {
            stage.toFront();
            stage.requestFocus();
            return;
        }
        for (LuminaApp app : ACTIVE_INSTANCES) {
            if (app.projectRoot != null && target.equals(app.projectRoot.toAbsolutePath().normalize())) {
                app.stage.toFront();
                app.stage.requestFocus();
                return;
            }
        }
        SystemSettings.OpenProjectMode openMode = SystemSettings.getInstance().getOpenProjectMode();
        if (openMode == SystemSettings.OpenProjectMode.NEW_WINDOW) {
            openInNewWindow(dir);
            return;
        }
        if (openMode == SystemSettings.OpenProjectMode.CURRENT_WINDOW) {
            replaceInThisWindow(dir);
            return;
        }
        OpenProjectChoiceDialog.show(stage, dir.getFileName().toString(),
                (choice, rememberChoice) -> {
                    switch (choice) {
                        case NEW_WINDOW -> {
                            if (rememberChoice) {
                                SystemSettings.getInstance().setOpenProjectMode(SystemSettings.OpenProjectMode.NEW_WINDOW);
                                SystemSettings.getInstance().save();
                            }
                            openInNewWindow(dir);
                        }
                        case THIS_WINDOW -> {
                            if (rememberChoice) {
                                SystemSettings.getInstance().setOpenProjectMode(SystemSettings.OpenProjectMode.CURRENT_WINDOW);
                                SystemSettings.getInstance().save();
                            }
                            replaceInThisWindow(dir);
                        }
                        case CANCEL -> console.println(
                                "Open cancelled \u2014 '" + dir.getFileName()
                                        + "' was not opened. Files remain at " + dir);
                    }
                });
    }

    /** Opens dir in a brand-new top-level window; this window is untouched. */
    private void openInNewWindow(Path dir) {
        LuminaApp app = new LuminaApp();
        app.pendingProjectToOpen = dir;
        app.start(new Stage());
    }

    /** Replaces this window's project with dir, clearing the old one first. */
    private void replaceInThisWindow(Path dir) {
        resetProjectStateInWindow();
        openProject(dir);
    }

    private void openProject(Path dir) {
        projectRoot = dir;
        dev.lumina.project.LuminaFolderGenerator.ensure(dir, dir.getFileName().toString());
        fileExplorer.setRoot(dir);
        updateProjectChip(dir);
        stage.setTitle("Lumina \u2014 " + dir.getFileName());
        updateBreadcrumbs(null, null);
        refreshRunConfigs();
        refreshGitInfo();
        mavenPanel.setProject(dir);
        Settings.put(Settings.LAST_PROJECT, dir.toString());
        RecentProjectsManager.getInstance().recordProjectOpened(dir);
        initSemanticEngine(dir);
        terminal.start(dir);

        // IntelliJ-style session restore: the tree's expansion and the
        // exact tabs that were open (with the same one focused) come back
        // exactly as they were left. Only a project with no saved session
        // yet (first-ever open) falls back to the old "guess the entry
        // point" heuristic.
        dev.lumina.util.ProjectSession.State session =
                dev.lumina.util.ProjectSession.load(dir);
        for (String rel : session.expanded()) {
            fileExplorer.expandTo(dir.resolve(rel));
        }
        if (!session.openFiles().isEmpty()) {
            Path toFocus = null;
            for (String rel : session.openFiles()) {
                Path f = dir.resolve(rel);
                if (Files.isRegularFile(f)) {
                    openFile(f);
                    if (rel.equals(session.activeFile())) toFocus = f;
                }
            }
            if (toFocus != null) {
                Path focusTarget = toFocus;
                Platform.runLater(() -> {
                    for (Tab t : allEditorTabs()) {
                        if (t instanceof EditorTab et
                                && focusTarget.equals(et.getPath())) {
                            groupOf(t).getSelectionModel().select(t);
                            break;
                        }
                    }
                });
            }
        } else {
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
    }

    /** Snapshots the tree's expanded folders and the open tabs so the next
     *  time this project opens, it looks exactly like it does right now. */
    private void saveSession() {
        if (projectRoot == null) return;
        java.util.Set<Path> expanded = fileExplorer != null ? fileExplorer.getExpandedPaths() : java.util.Set.of();
        List<Path> open = new java.util.ArrayList<>();
        Path active = null;
        Tab selectedTab = (activeEditorGroup != null && activeEditorGroup.getSelectionModel() != null)
                ? activeEditorGroup.getSelectionModel().getSelectedItem() : null;
        for (Tab t : allEditorTabs()) {
            if (t instanceof EditorTab et && et.getPath() != null) {
                open.add(et.getPath());
                if (t == selectedTab) active = et.getPath();
            }
        }
        dev.lumina.util.ProjectSession.save(projectRoot, expanded, open, active);
    }

    /**
     * Cleanly closes this window and deregisters it from the global active instances.
     */
    public void closeWindow() {
        ACTIVE_INSTANCES.remove(this);
        saveSession();
        try {
            console.shutdown();
            terminal.stop();
            dbPanel.shutdown();
        } catch (Exception ignored) {
        }
        stage.close();
    }

    /**
     * Resets the project state in this window to the clean "No project" welcome state.
     */
    private void resetProjectStateInWindow() {
        saveSession();
        projectRoot = null;
        closeEditorTabsWhere(t -> true);
        collapseAllSplits();
        fileExplorer.setRoot(null);
        updateProjectChip(null);
        stage.setTitle("Lumina");
        refreshRunConfigs();
        refreshGitInfo();
        mavenPanel.setProject(null);
        semantics = null;
        Settings.put(Settings.LAST_PROJECT, null);
        updateEditorVisibility();
    }

    /**
     * In IntelliJ: if multiple project windows are open, closes this window;
     * if this is the only window open, resets to the clean "No project" welcome state.
     */
    private void closeProject() {
        if (ACTIVE_INSTANCES.size() > 1) {
            closeWindow();
        } else {
            resetProjectStateInWindow();
        }
    }

    /**
     * Closes all projects across all windows. All other windows are closed,
     * and this window is reset to the clean "No project" welcome state.
     */
    private void closeAllProjects() {
        for (LuminaApp app : new java.util.ArrayList<>(ACTIVE_INSTANCES)) {
            if (app != this) {
                app.closeWindow();
            }
        }
        resetProjectStateInWindow();
    }

    /**
     * Closes all other project windows, leaving this window and its project untouched.
     */
    private void closeOtherProjects() {
        for (LuminaApp app : new java.util.ArrayList<>(ACTIVE_INSTANCES)) {
            if (app != this) {
                app.closeWindow();
            }
        }
    }

    private int rank(Path p) {
        String n = p.getFileName().toString();
        if (n.endsWith("Application.java")) return 0;
        if (n.equals("Main.java")) return 1;
        return 2;
    }

    // ---------------------------------------------------- new class/pkg/file

    private final NewJavaClassPopup newJavaClassPopup = new NewJavaClassPopup();
    private final NewPackagePopup newPackagePopup = new NewPackagePopup();

    private void newJavaClass() {
        newJavaClass(null);
    }

    private void newJavaClass(Path target) {
        Path dir = target != null ? (Files.isDirectory(target) ? target : target.getParent())
                : targetDirectory();
        if (dir == null) return;
        javafx.geometry.Point2D anchor = fileExplorer.localToScreen(40, 60);
        double x = anchor != null ? anchor.getX() : stage.getX() + 260;
        double y = anchor != null ? anchor.getY() : stage.getY() + 200;
        newJavaClassPopup.show(fileExplorer, x, y,
                (name, kind) -> createJavaClass(dir, name, kind));
    }

    /**
     * Creates the chosen kind of Java type, IntelliJ-style: typing a dotted
     * name ("util.Helpers") creates the intermediate package too, via the
     * same writeAndOpen() that already makes parent directories.
     */
    private void createJavaClass(Path dir, String raw, NewJavaClassPopup.Kind kind) {
        String cleaned = raw.replace(".java", "").trim();
        if (cleaned.isEmpty()) return;

        Path targetDir = dir;
        String simpleName = cleaned;
        int lastDot = cleaned.lastIndexOf('.');
        if (lastDot > 0) {
            String subPackage = cleaned.substring(0, lastDot).replace('.', '/');
            targetDir = dir.resolve(subPackage);
            simpleName = cleaned.substring(lastDot + 1);
        }
        if (simpleName.isEmpty()) return;

        String pkg = inferPackage(targetDir);
        String packageLine = pkg.isEmpty() ? "" : "package " + pkg + ";\n\n";
        String body = switch (kind) {
            case CLASS -> packageLine + "public class " + simpleName + " {\n\n}\n";
            case INTERFACE -> packageLine + "public interface " + simpleName + " {\n\n}\n";
            case RECORD -> packageLine + "public record " + simpleName + "() {\n\n}\n";
            case ENUM -> packageLine + "public enum " + simpleName + " {\n\n}\n";
            case ANNOTATION -> packageLine + "public @interface " + simpleName + " {\n\n}\n";
            case EXCEPTION -> packageLine + "public class " + simpleName + " extends Exception {\n"
                    + "    public " + simpleName + "(String message) {\n"
                    + "        super(message);\n"
                    + "    }\n\n"
                    + "    public " + simpleName + "(String message, Throwable cause) {\n"
                    + "        super(message, cause);\n"
                    + "    }\n"
                    + "}\n";
            // JEP-style compact source file: no package, no class wrapper.
            case COMPACT_SOURCE_FILE -> "void main() {\n    \n}\n";
        };
        writeAndOpen(targetDir.resolve(simpleName + ".java"), body);
    }

    private void newPackage() {
        newPackage(null);
    }

    private void newPackage(Path target) {
        Path dir = target != null ? (Files.isDirectory(target) ? target : target.getParent())
                : targetDirectory();
        if (dir == null) return;

        String currentPkg = inferPackage(dir);
        String initial = currentPkg.isEmpty() ? "" : currentPkg + ".";

        newPackagePopup.show(fileExplorer, initial, raw -> {
            String pkg = raw.trim();
            while (pkg.endsWith(".")) {
                pkg = pkg.substring(0, pkg.length() - 1).trim();
            }
            if (pkg.isEmpty()) return;

            try {
                Path sourceRoot = findSourceRoot(dir);
                Path created;
                if (sourceRoot != null) {
                    if (!currentPkg.isEmpty() && pkg.startsWith(currentPkg + ".")) {
                        created = sourceRoot.resolve(pkg.replace('.', '/'));
                    } else if (!currentPkg.isEmpty() && pkg.equals(currentPkg)) {
                        created = dir;
                    } else if (pkg.contains(".")) {
                        created = sourceRoot.resolve(pkg.replace('.', '/'));
                    } else if (!currentPkg.isEmpty()) {
                        created = dir.resolve(pkg.replace('.', '/'));
                    } else {
                        created = sourceRoot.resolve(pkg.replace('.', '/'));
                    }
                } else {
                    created = dir.resolve(pkg.replace('.', '/'));
                }

                Files.createDirectories(created);
                fileExplorer.refresh(created);
                fileExplorer.selectFile(created);
            } catch (IOException ex) {
                error("Could not create package", ex.getMessage());
            }
        });
    }

    private void newFile() {
        newFile(null);
    }

    private void newFile(Path target) {
        if (projectRoot == null && fileExplorer.getRootPath() == null) {
            addTab(new EditorTab("Untitled-" + untitledCounter++ + ".java", null));
            return;
        }
        Path dir = target != null ? (Files.isDirectory(target) ? target : target.getParent())
                : targetDirectory();
        if (dir == null) return;
        prompt("New File", "File name:", "notes.md").ifPresent(raw -> {
            String name = raw.trim();
            if (name.isEmpty()) return;
            writeAndOpen(dir.resolve(name), "");
        });
    }

    private void newDirectory() {
        newDirectory(null);
    }

    private void newDirectory(Path target) {
        Path dir = target != null ? (Files.isDirectory(target) ? target : target.getParent())
                : targetDirectory();
        if (dir == null) return;
        prompt("New Directory", "Directory name:", "folder").ifPresent(raw -> {
            String name = raw.trim();
            if (name.isEmpty()) return;
            try {
                Path created = dir.resolve(name);
                Files.createDirectories(created);
                fileExplorer.refresh(created);
                fileExplorer.selectFile(created);
            } catch (IOException ex) {
                error("Could not create directory", ex.getMessage());
            }
        });
    }

    private NewMenuBuilder.CreationHandlers createNewMenuHandlers() {
        return new NewMenuBuilder.CreationHandlers() {
            @Override public void onNewProject() { showNewProjectDialog(); }
            @Override public void onNewProjectFromExisting() { openFolderDialog(); }
            @Override public void onNewProjectFromVCS() { cloneGitRepositoryDialog(); }
            @Override public void onNewModule() { showComingSoon("New Module"); }
            @Override public void onNewModuleFromExisting() { openFolderDialog(); }
            @Override public void onNewJavaClass(Path dir) { newJavaClass(dir); }
            @Override public void onNewKotlinClass(Path dir) { newKotlinClass(dir); }
            @Override public void onNewFile(Path dir) { newFile(dir); }
            @Override public void onNewPackage(Path dir) { newPackage(dir); }
            @Override public void onNewDirectory(Path dir) { newDirectory(dir); }
            @Override public void onNewFxml(Path dir) { newFxmlFile(dir); }
            @Override public void onNewJavaFxApp(Path dir) { newJavaFxApp(dir); }
            @Override public void onNewPackageInfo(Path dir) { newPackageInfo(dir); }
            @Override public void onNewModuleInfo(Path dir) { newModuleInfo(dir); }
            @Override public void onNewKotlinNotebook(Path dir) {
                newSpecificFile(dir, "untitled.ipynb", "{\\n \\\"cells\\\": [],\\n \\\"metadata\\\": {},\\n \\\"nbformat\\\": 4,\\n \\\"nbformat_minor\\\": 2\\n}\\n");
            }
            @Override public void onNewResourceBundle(Path dir) { newResourceBundle(dir); }
            @Override public void onNewScratchFile(Path dir) {
                addTab(new EditorTab("scratch_" + System.currentTimeMillis() + ".txt", null));
            }
            @Override public void onNewSpecificFile(Path dir, String defaultName, String defaultContent) {
                newSpecificFile(dir, defaultName, defaultContent);
            }
            @Override public void onPlaceholder(String title) { showComingSoon(title); }
        };
    }

    private void newFxmlFile(Path target) {
        Path dir = target != null ? (Files.isDirectory(target) ? target : target.getParent())
                : targetDirectory();
        if (dir == null) return;
        prompt("New FXML File", "FXML file name:", "view.fxml").ifPresent(raw -> {
            String name = raw.trim();
            if (name.isEmpty()) return;
            if (!name.endsWith(".fxml")) name += ".fxml";
            String content = """
                    <?xml version="1.0" encoding="UTF-8"?>

                    <?import javafx.scene.layout.VBox?>

                    <VBox xmlns="http://javafx.com/javafx"
                          xmlns:fx="http://javafx.com/fxml">

                    </VBox>
                    """;
            writeAndOpen(dir.resolve(name), content);
        });
    }

    private void newJavaFxApp(Path target) {
        Path dir = target != null ? (Files.isDirectory(target) ? target : target.getParent())
                : targetDirectory();
        if (dir == null) return;
        prompt("New JavaFX Application", "Class name:", "MainApplication").ifPresent(raw -> {
            String name = raw.trim();
            if (name.isEmpty()) return;
            if (name.endsWith(".java")) name = name.substring(0, name.length() - 5);
            String pkg = inferPackage(dir);
            String pkgStmt = pkg.isEmpty() ? "" : "package " + pkg + ";\n\n";
            String content = pkgStmt + """
                    import javafx.application.Application;
                    import javafx.scene.Scene;
                    import javafx.scene.layout.StackPane;
                    import javafx.stage.Stage;

                    public class %s extends Application {

                        @Override
                        public void start(Stage primaryStage) {
                            primaryStage.setTitle("%s");
                            primaryStage.setScene(new Scene(new StackPane(), 800, 600));
                            primaryStage.show();
                        }

                        public static void main(String[] args) {
                            launch(args);
                        }
                    }
                    """.formatted(name, name);
            writeAndOpen(dir.resolve(name + ".java"), content);
        });
    }

    private void newPackageInfo(Path target) {
        Path dir = target != null ? (Files.isDirectory(target) ? target : target.getParent())
                : targetDirectory();
        if (dir == null) return;
        String pkg = inferPackage(dir);
        String pkgStmt = pkg.isEmpty() ? "" : "package " + pkg + ";\n";
        String content = """
                /**
                 * Package documentation for {@code %s}.
                 */
                %s""".formatted(pkg.isEmpty() ? "default package" : pkg, pkgStmt);
        writeAndOpen(dir.resolve("package-info.java"), content);
    }

    private void newModuleInfo(Path target) {
        Path dir = target != null ? (Files.isDirectory(target) ? target : target.getParent())
                : targetDirectory();
        if (dir == null) return;
        Path sourceRoot = findSourceRoot(dir);
        Path root = sourceRoot != null ? sourceRoot : dir;
        String modName = (projectRoot != null ? projectRoot.getFileName().toString() : "app")
                .toLowerCase().replaceAll("[^a-zA-Z0-9_.]", "");
        String content = """
                module %s {
                    requires javafx.controls;
                    requires javafx.fxml;
                }
                """.formatted(modName);
        writeAndOpen(root.resolve("module-info.java"), content);
    }

    private void newKotlinClass(Path target) {
        Path dir = target != null ? (Files.isDirectory(target) ? target : target.getParent())
                : targetDirectory();
        if (dir == null) return;
        prompt("New Kotlin Class/File", "Name:", "MyClass").ifPresent(raw -> {
            String name = raw.trim();
            if (name.isEmpty()) return;
            if (name.endsWith(".kt")) name = name.substring(0, name.length() - 3);
            String pkg = inferPackage(dir);
            String pkgStmt = pkg.isEmpty() ? "" : "package " + pkg + "\n\n";
            String content = pkgStmt + "class " + name + " {\n}\n";
            writeAndOpen(dir.resolve(name + ".kt"), content);
        });
    }

    private void newResourceBundle(Path target) {
        Path dir = target != null ? (Files.isDirectory(target) ? target : target.getParent())
                : targetDirectory();
        if (dir == null) return;
        prompt("New Resource Bundle", "Resource bundle base name:", "messages").ifPresent(raw -> {
            String name = raw.trim();
            if (name.isEmpty()) return;
            if (name.endsWith(".properties")) name = name.substring(0, name.length() - 11);
            writeAndOpen(dir.resolve(name + ".properties"), "# Resource bundle: " + name + "\n");
        });
    }

    private void newSpecificFile(Path target, String defaultName, String defaultContent) {
        Path dir = target != null ? (Files.isDirectory(target) ? target : target.getParent())
                : targetDirectory();
        if (dir == null) return;
        prompt("New " + defaultName, "File name:", defaultName).ifPresent(raw -> {
            String name = raw.trim();
            if (name.isEmpty()) return;
            writeAndOpen(dir.resolve(name), defaultContent != null ? defaultContent : "");
        });
    }

    private void cloneGitRepositoryDialog() {
        prompt("Get from Version Control", "Repository URL:", "https://github.com/").ifPresent(url -> {
            String u = url.trim();
            if (u.isEmpty()) return;
            console.println("Cloning repository: " + u);
        });
    }

    private void copyPathToClipboard(Path p) {
        if (p == null) return;
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(p.toAbsolutePath().toString());
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        console.println("\u2713 Copied path: " + p.toAbsolutePath());
    }

    /** The gear icon at the far right of the toolbar \u2014 replaces what used
     *  to be a "Maven / Database panel" toggle that duplicated what the
     *  right-hand rail's own icons already do. Matches IntelliJ's own
     *  menu under its equivalent gear icon. */
    private void showMainSettingsMenu(javafx.scene.Node anchor) {
        Menu viewMode = new Menu("View Mode");
        viewMode.getItems().addAll(
                disabled("Distraction Free Mode"),
                disabled("Zen Mode"),
                disabled("Presentation Mode"));

        ContextMenu menu = new ContextMenu(
                item("Check for Updates\u2026", null,
                        e -> showComingSoon("Check for Updates")),
                new SeparatorMenuItem(),
                item("Run Anything\u2026", null, e -> showComingSoon("Run Anything")),
                new SeparatorMenuItem(),
                item("Project Structure\u2026", projectStructureShortcut(),
                        e -> openProjectStructure()),
                item("Settings\u2026", "Shortcut+Alt+S", e -> new SettingsDialog(stage).show()),
                item("Plugins\u2026", null, e -> new PluginManagerDialog(stage).show()),
                disabled("Backup and Sync\u2026 Off"),
                new SeparatorMenuItem(),
                item("Theme\u2026", null, e -> new SettingsDialog(stage, "Appearance").show()),
                item("Keymap\u2026", null, e -> new SettingsDialog(stage, "Keymap").show()),
                viewMode,
                item("Customize Main Toolbar\u2026", null,
                        e -> showComingSoon("Customize Main Toolbar")));
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    private String projectStructureShortcut() {
        boolean isMac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        return isMac ? "Shortcut+SEMICOLON" : "Shortcut+Alt+Shift+S";
    }

    private void openProjectStructure() {
        if (projectRoot == null) {
            showInfo("Project Structure", "Open a project first to see its structure.");
            return;
        }
        new ProjectStructureDialog(stage, projectRoot.getFileName().toString(), projectRoot).show();
    }

    /** Opens Settings straight to Tools \u2192 Terminal, from the terminal
     *  tab strip's "+" dropdown. */
    private void openTerminalSettings() {
        new SettingsDialog(stage, "Terminal").show();
    }

    /** Hides the whole bottom Terminal dock \u2014 shared by "closing the last
     *  session tab" and the tab menu's own "Hide" item, which are two
     *  different triggers for the exact same underlying action. */
    private void hideTerminalPanel() {
        toggleBottomPanel(false);
        iconRail.clearBottomSelection();
    }

    /** "Move to Editor" from a terminal tab's right-click menu: the same
     *  live {@code Tab} (and the {@link TerminalPane} session inside it,
     *  process and all) is just reparented into the active editor group,
     *  not recreated \u2014 closing it there stops the shell exactly like
     *  closing it in the terminal tool window would have. */
    private void moveTerminalTabToEditor(Tab tab) {
        if (tab.getContent() instanceof TerminalPane tp) {
            tab.setOnCloseRequest(e -> tp.stop());
        }
        tab.setClosable(true);
        activeEditorGroup.getTabs().add(tab);
        activeEditorGroup.getSelectionModel().select(tab);
    }

    /** Scaffolded menu items land here until they get real behavior. */
    private void showComingSoon(String feature) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle(feature);
        alert.setHeaderText(feature);
        alert.setContentText("Coming in a later phase.");
        alert.getDialogPane().getStylesheets().add(getClass()
                .getResource("/css/lumina-dark.css").toExternalForm());
        alert.showAndWait();
    }

    /**
     * Nested-path-aware yaml completion: fullPrefix is the dotted ancestor
     * chain plus whatever's typed on the current line (built by
     * Completion.contextForYaml). Each suggestion is only the *next*
     * segment under that path \u2014 a leaf property name if one exists at
     * exactly that depth, or an intermediate key (shown with "\u2026") when
     * there's more nesting below it, exactly like IntelliJ's yaml
     * completion never spells out the whole remaining path at once.
     */
    private List<dev.lumina.semantics.Completion.Item> springYamlCompletions(String fullPrefix) {
        List<dev.lumina.spring.SpringConfigMetadata.Property> props = springProperties;
        if (props.isEmpty()) return List.of();
        String ancestor;
        String local;
        int lastDot = fullPrefix.lastIndexOf('.');
        if (lastDot < 0) {
            ancestor = "";
            local = fullPrefix;
        } else {
            ancestor = fullPrefix.substring(0, lastDot);
            local = fullPrefix.substring(lastDot + 1);
        }
        String ancestorDot = ancestor.isEmpty() ? "" : ancestor + ".";
        java.util.LinkedHashMap<String, dev.lumina.spring.SpringConfigMetadata.Property> bySegment =
                new java.util.LinkedHashMap<>();
        for (var p : props) {
            if (!ancestor.isEmpty() && !p.name().startsWith(ancestorDot)) continue;
            String remainder = ancestor.isEmpty() ? p.name()
                    : p.name().substring(ancestorDot.length());
            if (remainder.isEmpty()) continue;
            int dot = remainder.indexOf('.');
            String nextSegment = dot < 0 ? remainder : remainder.substring(0, dot);
            if (!dev.lumina.semantics.Completion.matches(local, nextSegment)) continue;
            bySegment.putIfAbsent(nextSegment, p);
        }
        List<dev.lumina.semantics.Completion.Item> items = new java.util.ArrayList<>();
        for (var entry : bySegment.entrySet()) {
            String seg = entry.getKey();
            var p = entry.getValue();
            boolean isLeaf = p.name().equals(ancestorDot + seg);
            String detail;
            if (isLeaf) {
                StringBuilder d = new StringBuilder();
                if (!p.type().isEmpty()) d.append(simplePropertyType(p.type()));
                if (!p.description().isEmpty()) {
                    if (!d.isEmpty()) d.append("  \u2014  ");
                    d.append(firstSentence(p.description()));
                }
                detail = d.toString();
            } else {
                detail = "\u2026";
            }
            items.add(new dev.lumina.semantics.Completion.Item(seg, seg, seg,
                    detail, dev.lumina.semantics.Completion.Kind.FIELD, null, 0));
            if (items.size() >= 60) break;
        }
        return items;
    }

    private static boolean isSpringConfigFile(Path file) {
        String n = file.getFileName().toString();
        return n.equals("application.properties") || n.equals("application.yml")
                || n.equals("application.yaml")
                || (n.endsWith(".properties") && n.startsWith("application"))
                || ((n.endsWith(".yml") || n.endsWith(".yaml")) && n.startsWith("application"));
    }

    private List<dev.lumina.semantics.Completion.Item> springPropertyCompletions(String prefix) {
        List<dev.lumina.spring.SpringConfigMetadata.Property> props = springProperties;
        if (prefix.isEmpty() || props.isEmpty()) return List.of();
        List<dev.lumina.semantics.Completion.Item> items = new java.util.ArrayList<>();
        String needle = prefix.toLowerCase();
        for (var p : props) {
            if (!p.name().toLowerCase().contains(needle)) continue;
            StringBuilder detail = new StringBuilder();
            if (!p.type().isEmpty()) detail.append(simplePropertyType(p.type()));
            if (!p.description().isEmpty()) {
                if (!detail.isEmpty()) detail.append("  \u2014  ");
                detail.append(firstSentence(p.description()));
            }
            items.add(new dev.lumina.semantics.Completion.Item(p.name(), p.name(),
                    p.name(), detail.toString(),
                    dev.lumina.semantics.Completion.Kind.FIELD, null, 0));
            if (items.size() >= 60) break;
        }
        return items;
    }

    private static String simplePropertyType(String fqcn) {
        int lt = fqcn.indexOf('<');
        String base = lt < 0 ? fqcn : fqcn.substring(0, lt);
        int dot = base.lastIndexOf('.');
        return dot < 0 ? base : base.substring(dot + 1);
    }

    private static String firstSentence(String description) {
        int dot = description.indexOf(". ");
        String s = dot < 0 ? description : description.substring(0, dot + 1);
        return s.length() > 90 ? s.substring(0, 90) + "\u2026" : s;
    }

    /** IntelliJ's Ctrl+Alt+B: @Autowired field/interface \u2192 its concrete
     *  Spring-annotated implementation, not just the contract. */
    private void goToImplementation() {
        EditorTab editor = currentEditor();
        dev.lumina.semantics.SemanticEngine engine = semantics;
        if (editor == null || editor.getPath() == null) return;
        if (engine == null) {
            console.println("Go to Implementation needs the semantic engine "
                    + "(still indexing\u2026)");
            return;
        }
        final Path file = editor.getPath();
        final String text = editor.getEditorText();
        final int line = editor.getCaretLine();
        final int column = editor.getCaretColumn();
        Thread t = new Thread(() -> {
            List<dev.lumina.semantics.SemanticEngine.Location> impls =
                    List.of();
            try {
                impls = engine.findImplementations(file, text, line, column);
            } catch (Throwable ignored) {
            }
            final List<dev.lumina.semantics.SemanticEngine.Location> found = impls;
            Platform.runLater(() -> {
                if (found.isEmpty()) {
                    console.println("No Spring-annotated implementation found "
                            + "for the symbol at the caret.");
                } else if (found.size() == 1) {
                    openFileAtLine(found.get(0).file(), found.get(0).line());
                } else {
                    List<UsagesDialog.Hit> rows = found.stream()
                            .map(loc -> new UsagesDialog.Hit(loc.file(), loc.line(),
                                    loc.file().getFileName().toString(), false))
                            .toList();
                    new UsagesDialog(stage, projectRoot, "implementations",
                            rows, this::openFileAtLine).show();
                }
            });
        }, "lumina-goto-impl");
        t.setDaemon(true);
        t.start();
    }

    private void renameSelectedFile() {
        renameFile(null);
    }

    private void renameFile(Path target) {
        Path selected = target != null ? target : fileExplorer.getSelectedPath();
        if (selected == null || !Files.isRegularFile(selected)) {
            error("Nothing selected", "Select a file in the project tree first.");
            return;
        }
        prompt("Rename File", "New name:", selected.getFileName().toString()).ifPresent(raw -> {
            String name = raw.trim();
            if (name.isEmpty()) return;
            try {
                Path renamedTarget = selected.resolveSibling(name);
                Files.move(selected, renamedTarget);
                closeEditorTabsWhere(t ->
                        t instanceof EditorTab et && selected.equals(et.getPath()));
                fileExplorer.refresh(renamedTarget);
                openFile(renamedTarget);
            } catch (IOException ex) {
                error("Could not rename", ex.getMessage());
            }
        });
    }

    private Path activeTargetPath() {
        boolean projectExplorerActive = horizontalSplit != null
                && horizontalSplit.getItems().contains(leftDock)
                && leftTabs != null
                && leftTabs.getSelectionModel().getSelectedIndex() == 0;

        if (projectExplorerActive && fileExplorer != null) {
            Path selected = fileExplorer.getSelectedPath();
            if (selected != null) {
                return Files.isDirectory(selected) ? selected : selected.getParent();
            }
        }
        return null;
    }

    private Path targetDirectory() {
        Path target = activeTargetPath();
        if (target != null) {
            return target;
        }
        if (fileExplorer != null && fileExplorer.getRootPath() != null) {
            return fileExplorer.getRootPath();
        }
        if (projectRoot != null) {
            return projectRoot;
        }
        error("No folder open",
                "Open or create a project first (File \u2192 New \u2192 Project\u2026).");
        return null;
    }

    public static Path findSourceRoot(Path dir) {
        if (dir == null) return null;
        Path curr = dir.toAbsolutePath().normalize();
        while (curr != null) {
            String s = curr.toString().replace('\\', '/');
            for (String marker : new String[]{
                    "/src/main/java", "/src/test/java",
                    "/src/main/kotlin", "/src/test/kotlin",
                    "/src/main/groovy", "/src/test/groovy"
            }) {
                if (s.endsWith(marker)) {
                    return curr;
                }
            }
            curr = curr.getParent();
        }
        return null;
    }

    public static String inferPackageName(Path dir, Path rootFallback) {
        if (dir == null) return "";
        Path abs = dir.toAbsolutePath().normalize();
        Path sourceRoot = findSourceRoot(abs);
        if (sourceRoot != null) {
            if (abs.equals(sourceRoot)) return "";
            Path rel = sourceRoot.relativize(abs);
            return rel.toString().isEmpty() ? "" : rel.toString()
                    .replace(File.separatorChar, '.').replace('/', '.');
        }
        if (rootFallback == null) return "";
        for (String r : new String[]{"src/main/java", "src/test/java", "src/main/kotlin", "src/test/kotlin"}) {
            Path marker = rootFallback.toAbsolutePath().normalize().resolve(r);
            if (abs.startsWith(marker)) {
                Path rel = marker.relativize(abs);
                return rel.toString().isEmpty() ? "" : rel.toString()
                        .replace(File.separatorChar, '.').replace('/', '.');
            }
        }
        return "";
    }

    private String inferPackage(Path dir) {
        return inferPackageName(dir, fileExplorer != null ? fileExplorer.getRootPath() : null);
    }

    private void writeAndOpen(Path file, String content) {
        try {
            if (Files.exists(file)) {
                error("Already exists", file.getFileName() + " already exists here.");
                return;
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);

            // Handle Git confirmation policy if inside a git repository
            handleGitFileCreation(file);

            fileExplorer.refresh(file);
            openFile(file);
            if (commitPanel != null) {
                commitPanel.refresh();
            }
        } catch (IOException ex) {
            error("Could not create file", ex.getMessage());
        }
    }

    private void handleGitFileCreation(Path file) {
        Path repoRoot = dev.lumina.git.GitStatusManager.findRepositoryRoot(file);
        if (repoRoot == null) return;

        dev.lumina.git.GitConfirmationManager confirmMgr = dev.lumina.git.GitConfirmationManager.getInstance();
        dev.lumina.git.GitConfirmationManager.FileCreationPolicy policy = confirmMgr.getFileCreationPolicy();

        String relPath;
        try {
            relPath = repoRoot.relativize(file).toString().replace('\\', '/');
        } catch (Exception e) {
            relPath = file.getFileName().toString();
        }

        boolean shouldAdd = false;

        if (policy == dev.lumina.git.GitConfirmationManager.FileCreationPolicy.ASK) {
            AddFileToGitDialog dialog = new AddFileToGitDialog(stage, file, repoRoot);
            AddFileToGitDialog.Decision decision = dialog.showAndGet();
            shouldAdd = decision.add();

            if (decision.doNotAskAgain()) {
                confirmMgr.setFileCreationPolicy(shouldAdd
                        ? dev.lumina.git.GitConfirmationManager.FileCreationPolicy.ADD_SILENTLY
                        : dev.lumina.git.GitConfirmationManager.FileCreationPolicy.DO_NOT_ADD);
            }
        } else if (policy == dev.lumina.git.GitConfirmationManager.FileCreationPolicy.ADD_SILENTLY) {
            shouldAdd = true;
        } else {
            shouldAdd = false;
        }

        if (shouldAdd) {
            dev.lumina.git.GitService.add(repoRoot, List.of(relPath));
            dev.lumina.git.GitStatusManager.getInstance().setStatus(file, dev.lumina.git.GitFileStatus.ADDED);
        } else {
            dev.lumina.git.GitStatusManager.getInstance().setStatus(file, dev.lumina.git.GitFileStatus.UNTRACKED);
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
        boolean hasTabs = editorGroups.stream().anyMatch(g -> !g.getTabs().isEmpty());
        editorRoot.setVisible(hasTabs);
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
        String defDir = SystemSettings.getInstance().getDefaultProjectDirectory();
        if (defDir != null && !defDir.isBlank()) {
            File d = new File(defDir);
            if (d.isDirectory()) chooser.setInitialDirectory(d);
        }
        File dir = chooser.showDialog(stage);
        if (dir != null) openProjectInteractive(dir.toPath());
    }

    private EditorTab openFile(Path path) {
        for (Tab t : allEditorTabs()) {
            if (t instanceof EditorTab et && path.equals(et.getPath())) {
                groupOf(t).getSelectionModel().select(t);
                return et;
            }
        }
        if (path.getFileName().toString().endsWith(".class")) {
            return openClassFile(path);
        }
        try {
            String content = Files.readString(path);
            EditorTab tab = new EditorTab(path.getFileName().toString(), path);
            tab.setEditorText(content);
            if (dev.lumina.semantics.LibrarySourceService.getInstance().isLibraryCacheFile(path)) {
                tab.setReadOnly();
            }
            addTab(tab);
            return tab;
        } catch (IOException ex) {
            error("Could not open file", ex.getMessage());
            return null;
        }
    }

    /** Disassemble a .class file with javap and show it read-only. */
    private EditorTab openClassFile(Path path) {
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
            return tab;
        } catch (IOException ex) {
            error("Could not disassemble class", ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private void openDiffViewer(String title, String stashRef, String relPath, Path localPath, String curText, String stashText) {
        if (activeEditorGroup == null || !editorGroups.contains(activeEditorGroup)) {
            activeEditorGroup = editorGroups.isEmpty() ? editorTabs : editorGroups.get(0);
        }
        for (Tab t : allEditorTabs()) {
            if (t instanceof DiffViewerTab dvt
                    && java.util.Objects.equals(dvt.getStashRef(), stashRef)
                    && java.util.Objects.equals(dvt.getRelativePath(), relPath)) {
                groupOf(t).getSelectionModel().select(t);
                updateEditorVisibility();
                return;
            }
        }
        DiffViewerTab tab = new DiffViewerTab(title, stashRef, relPath, localPath, curText, stashText);
        tab.setOnClosed(e -> updateEditorVisibility());
        activeEditorGroup.getTabs().add(tab);
        activeEditorGroup.getSelectionModel().select(tab);
        updateEditorVisibility();
    }

    private void addTab(EditorTab tab) {
        addTabTo(activeEditorGroup, tab);
    }

    /** Same as {@link #addTab}, but targets a specific editor group instead
     *  of whichever one is currently active \u2014 used when creating a new
     *  split, where the destination group isn't "active" yet. */
    private void addTabTo(TabPane group, EditorTab tab) {
        tab.setEditorContextMenu(buildEditorContextMenu());
        tab.setContextMenu(buildEditorTabContextMenu(tab));
        wireAddStarters(tab);
        wireMavenSync(tab);
        tab.setOnOpenDiff((title, baseText, curText) -> {
            Path p = tab.getPath();
            if (p != null) {
                Path repoRoot = dev.lumina.git.GitStatusManager.findRepositoryRoot(p);
                String rel = repoRoot != null ? repoRoot.relativize(p).toString() : p.getFileName().toString();
                openDiffViewer(title, "HEAD", rel, p, curText, baseText);
            }
        });
        tab.setOnOpenCommit(() -> {
            toggleLeftPanel(true);
            leftTabs.getSelectionModel().select(1);
            if (commitPanel != null) commitPanel.refresh();
        });
        // M2: completion — engine results plus keywords and live templates.
        tab.setCompletionProvider((file, text, caretLine, ctx) -> {
            if (file != null && isSpringConfigFile(file)) {
                String n = file.getFileName().toString();
                return (n.endsWith(".yml") || n.endsWith(".yaml"))
                        ? springYamlCompletions(ctx.prefix())
                        : springPropertyCompletions(ctx.prefix());
            }
            if (ctx.annotation()) {
                dev.lumina.semantics.SemanticEngine engine = semantics;
                if (engine != null) {
                    return engine.annotationCompletions(ctx.prefix());
                }
                return dev.lumina.semantics.SemanticEngine.fallbackAnnotationCompletions(ctx.prefix());
            }
            if (ctx.extendsInterface() != null) {
                dev.lumina.semantics.SemanticEngine engine = semantics;
                List<dev.lumina.semantics.Completion.Item> items = new java.util.ArrayList<>();
                if (engine != null) {
                    items.addAll(engine.repositoryExtendsCompletions(ctx.extendsInterface(), ctx.prefix()));
                } else {
                    items.addAll(dev.lumina.semantics.SemanticEngine.fallbackRepositoryCompletions(ctx.extendsInterface(), ctx.prefix()));
                }
                if (engine != null && file != null) {
                    try {
                        items.addAll(engine.scopeCompletions(file, text, caretLine, ctx.prefix()));
                    } catch (Throwable ignored) {
                    }
                }
                return items;
            }
            List<dev.lumina.semantics.Completion.Item> items =
                    new java.util.ArrayList<>();
            dev.lumina.semantics.SemanticEngine engine = semantics;
            if (engine != null && file != null) {
                try {
                    items.addAll(ctx.member()
                            ? engine.memberCompletions(file, text, caretLine,
                            ctx.receiver(), ctx.prefix())
                            : engine.scopeCompletions(file, text, caretLine,
                            ctx.prefix()));
                } catch (Throwable ignored) {
                }
            }
            if (!ctx.member()) {
                items.addAll(dev.lumina.semantics.Completion
                        .templateItems(ctx.prefix()));
                items.addAll(dev.lumina.semantics.Completion
                        .keywordItems(ctx.prefix()));
            }
            return items;
        });
        // M3: compile-on-idle diagnostics for project .java files, plus
        // IntelliJ-style Spring Boot config inspections for application
        // .properties / .yml (unknown properties, missing JDBC driver).
        tab.setDiagnosticsProvider((file, text) -> {
            if (file == null || projectRoot == null) return List.of();
            String fname = file.getFileName().toString();
            if (fname.endsWith(".properties") || fname.endsWith(".yml") || fname.endsWith(".yaml")) {
                if (!isSpringConfigFile(file)) return List.of();
                boolean yaml = fname.endsWith(".yml") || fname.endsWith(".yaml");
                return dev.lumina.diagnostics.SpringConfigDiagnostics.analyze(
                        text, yaml, springProperties, ensureClasspath(),
                        projectRoot != null ? projectRoot.getFileName().toString() : null);
            }
            String cp = ensureClasspath();
            String classes = projectRoot.resolve("target/classes").toString();
            String full = cp == null || cp.isBlank() ? classes
                    : cp + File.pathSeparator + classes;
            List<Path> roots = new java.util.ArrayList<>();
            for (String rel : new String[]{"src/main/java", "src/test/java"}) {
                Path root = projectRoot.resolve(rel);
                if (Files.isDirectory(root)) roots.add(root);
            }
            List<dev.lumina.diagnostics.JavaDiagnostics.Diag> results =
                    new java.util.ArrayList<>(dev.lumina.diagnostics.JavaDiagnostics.compile(
                            file, text, full, roots));
            String mod = projectRoot != null ? projectRoot.getFileName().toString() : null;
            results.addAll(dev.lumina.diagnostics.JpaDiagnostics.checkPersistentEntities(file, text, roots, mod));
            results.addAll(dev.lumina.diagnostics.FieldDiagnostics.checkUnusedFields(file, text, mod));
            results.addAll(dev.lumina.diagnostics.MethodDiagnostics.checkUnusedMethods(file, text, roots, mod));
            return results;
        });
        tab.setDiagnosticsListener(diags -> {
            if (currentEditor() == tab) {
                problemsPanel.show(diags);
                updateProblemsStatus(diags);
            }
        });
        tab.setOnQuickFix(this::applySpringConfigQuickFix);
        tab.setOnGenerateTest(this::generateTest);
        tab.setTypeResolver(sym -> {
            dev.lumina.semantics.SemanticEngine eng = semantics;
            return eng != null ? eng.resolveFqcn(sym) : null;
        });
        // M4: '(' opens parameter info.
        tab.setParamInfoTrigger(this::showParameterInfo);
        loadAuthorHints(tab);
        tab.setQuickDocProvider((line, col) -> {
            if (semantics == null || tab.getPath() == null) return null;
            return semantics.symbolDocAt(tab.getPath(), tab.getEditorText(), line, col);
        });
        tab.setNavigationCoordinatesHandler((word, line, col, screenBounds) -> {
            handleNavigationOrUsages(tab, word, line, col, screenBounds);
        });
        tab.setNavigationHandler(word -> {
            handleNavigationOrUsages(tab, word, tab.getCaretLine(), tab.getCaretColumn(), null);
        });
        group.getTabs().add(tab);
        group.getSelectionModel().select(tab);
        tab.focusEditor();
    }

    // ------------------------------------------------- editor tab right-click

    /**
     * Right-click menu for an editor tab header, laid out to match
     * IntelliJ's own item-for-item. Close/Pin/Copy Path/Rename/Annotate/
     * Open In are real; Split (no split-editor support exists),
     * multi-window, Kotlin conversion, Gist creation, and Copilot are
     * shown \u2014 matching the layout \u2014 but honestly disabled rather than
     * faked, since none of those subsystems exist in Lumina.
     */
    private ContextMenu buildEditorTabContextMenu(EditorTab tab) {
        ContextMenu menu = new ContextMenu();

        MenuItem close = new MenuItem("Close");
        close.setOnAction(e -> groupOf(tab).getTabs().remove(tab));

        MenuItem closeOthers = new MenuItem("Close Other Tabs");
        closeOthers.setOnAction(e -> closeEditorTabsWhere(t -> t != tab && !isPinned(t)));

        MenuItem closeAll = new MenuItem("Close All Tabs");
        closeAll.setOnAction(e -> closeEditorTabsWhere(t -> !isPinned(t)));

        MenuItem closeUnmodified = new MenuItem("Close Unmodified Tabs");
        closeUnmodified.setOnAction(e -> closeEditorTabsWhere(
                t -> !isPinned(t) && t instanceof EditorTab et && !et.isDirty()));

        MenuItem closeLeft = new MenuItem("Close Tabs to the Left");
        closeLeft.setOnAction(e -> closeEditorTabsRelativeTo(tab, true));

        MenuItem closeRight = new MenuItem("Close Tabs to the Right");
        closeRight.setOnAction(e -> closeEditorTabsRelativeTo(tab, false));

        MenuItem copyPath = new MenuItem("Copy Path/Reference\u2026");
        copyPath.setOnAction(e -> copyPathToClipboard(tab.getPath()));

        MenuItem splitRight = new MenuItem("Split Right");
        splitRight.setDisable(tab.getPath() == null);
        splitRight.setOnAction(e -> splitEditorGroup(groupOf(tab), tab,
                Orientation.HORIZONTAL, false));
        MenuItem splitMoveRight = new MenuItem("Split and Move Right");
        splitMoveRight.setDisable(tab.getPath() == null);
        splitMoveRight.setOnAction(e -> splitEditorGroup(groupOf(tab), tab,
                Orientation.HORIZONTAL, true));
        MenuItem splitDown = new MenuItem("Split Down");
        splitDown.setDisable(tab.getPath() == null);
        splitDown.setOnAction(e -> splitEditorGroup(groupOf(tab), tab,
                Orientation.VERTICAL, false));
        MenuItem splitMoveDown = new MenuItem("Split and Move Down");
        splitMoveDown.setDisable(tab.getPath() == null);
        splitMoveDown.setOnAction(e -> splitEditorGroup(groupOf(tab), tab,
                Orientation.VERTICAL, true));

        MenuItem pin = new MenuItem(tab.isPinned() ? "Unpin Tab" : "Pin Tab");
        pin.setOnAction(e -> tab.setPinned(!tab.isPinned()));

        MenuItem newWindow = disabled("Open Tab in New Window");
        MenuItem configureTabs = item("Configure Editor Tabs\u2026", null,
                e -> showComingSoon("Configure Editor Tabs"));

        Menu bookmarks = new Menu("Bookmarks");
        bookmarks.getItems().add(disabled("Toggle Bookmark"));

        MenuItem overrideFileType = disabled("Override File Type");

        Menu openIn = new Menu("Open In");
        MenuItem openInTerminal = new MenuItem("Terminal");
        openInTerminal.setDisable(tab.getPath() == null);
        openInTerminal.setOnAction(e -> openTabLocationInTerminal(tab));
        MenuItem openInFileManager = new MenuItem(systemFileManagerLabel());
        openInFileManager.setDisable(tab.getPath() == null);
        openInFileManager.setOnAction(e -> revealInFileManager(tab));
        openIn.getItems().addAll(openInTerminal, openInFileManager);

        Menu localHistory = new Menu("Local History");
        localHistory.getItems().add(disabled("Show History"));

        Menu git = new Menu("Git");
        MenuItem annotate = new MenuItem("Annotate with Git Blame");
        annotate.setDisable(tab.getPath() == null);
        annotate.setOnAction(e -> {
            groupOf(tab).getSelectionModel().select(tab);
            toggleBlame();
        });
        git.getItems().add(annotate);

        MenuItem rename = new MenuItem("Rename File\u2026");
        rename.setDisable(tab.getPath() == null);
        rename.setOnAction(e -> renameTabFile(tab));

        MenuItem convertKotlin = disabled("Convert Java File to Kotlin File");
        MenuItem createGist = disabled("Create Gist\u2026");

        MenuItem addToChat = disabled("Add file to Chat");
        Menu copilot = new Menu("GitHub Copilot");
        copilot.getItems().add(disabled("Open Chat"));
        MenuItem upgrade = disabled("Upgrade Java Runtime and Frameworks");

        menu.getItems().addAll(
                close, closeOthers, closeAll, closeUnmodified, closeLeft, closeRight,
                new SeparatorMenuItem(),
                copyPath,
                new SeparatorMenuItem(),
                splitRight, splitMoveRight, splitDown, splitMoveDown,
                new SeparatorMenuItem(),
                pin, newWindow, configureTabs,
                new SeparatorMenuItem(),
                bookmarks,
                new SeparatorMenuItem(),
                overrideFileType,
                new SeparatorMenuItem(),
                openIn,
                new SeparatorMenuItem(),
                localHistory, git,
                new SeparatorMenuItem(),
                rename,
                new SeparatorMenuItem(),
                convertKotlin, createGist,
                new SeparatorMenuItem(),
                addToChat, copilot, upgrade);
        menu.setOnShowing(e -> pin.setText(tab.isPinned() ? "Unpin Tab" : "Pin Tab"));
        return menu;
    }

    private static boolean isPinned(Tab t) {
        return t instanceof EditorTab et && et.isPinned();
    }

    // -------------------------------------------------------- split editing

    /**
     * Splits {@code source} in the given direction, either duplicating the
     * given tab into a fresh editor (re-read from disk \u2014 a genuinely
     * separate buffer, not a live-synced view of the same one; saving one
     * side doesn't update the other until it's reloaded) or, for "Split and
     * Move", relocating the actual tab instance with nothing re-read.
     *
     * <p>Splits are always binary: each one wraps the source group in a new
     * two-item {@link SplitPane}. Nesting further splits inside either side
     * works the same way, so arbitrarily deep split layouts are possible \u2014
     * same as IntelliJ \u2014 they just aren't persisted across restarts.
     */
    private void splitEditorGroup(TabPane source, Tab sourceTab,
                                  Orientation orientation, boolean move) {
        if (!(sourceTab instanceof EditorTab sourceEditor) || sourceEditor.getPath() == null) {
            return;
        }
        TabPane newGroup = new TabPane();
        wireEditorGroupTracking(newGroup);
        wireEditorGroupSelection(newGroup);
        editorGroups.add(newGroup);

        SplitPane split = new SplitPane();
        split.setOrientation(orientation);
        split.getItems().addAll(source, newGroup);
        split.setDividerPositions(0.5);

        // Rebuilds the tree from scratch with `source` replaced by `split`,
        // rather than mutating whatever live SplitPane currently contains
        // `source` in place \u2014 the same principle closeEditorGroup uses
        // below, and for the same reason: this is what makes splitting an
        // *already-split* pane (building a 2x2 grid, etc.) just as safe as
        // the very first split, instead of accumulating fragile live-tree
        // surgery the deeper the layout gets.
        javafx.scene.Node rebuilt = cloneTreeReplacing(editorRoot, source, split);
        if (rebuilt != editorRoot) {
            editorArea.getChildren().set(editorArea.getChildren().indexOf(editorRoot), rebuilt);
            editorRoot = rebuilt;
        }

        if (move) {
            source.getTabs().remove(sourceEditor);
            newGroup.getTabs().add(sourceEditor);
            newGroup.getSelectionModel().select(sourceEditor);
            sourceEditor.focusEditor();
        } else {
            EditorTab duplicate = duplicateEditorTabFor(sourceEditor.getPath());
            if (duplicate != null) addTabTo(newGroup, duplicate);
        }
        activeEditorGroup = newGroup;
    }

    /** A fresh {@link EditorTab} reading the same file from disk again \u2014
     *  used for "Split Right"/"Split Down", which duplicate rather than
     *  move. Doesn't go through {@link #openFile}'s already-open dedup
     *  check, since duplicating is the entire point here. */
    private EditorTab duplicateEditorTabFor(Path path) {
        try {
            String content = Files.readString(path);
            EditorTab tab = new EditorTab(path.getFileName().toString(), path);
            tab.setEditorText(content);
            return tab;
        } catch (IOException ex) {
            error("Could not open file", ex.getMessage());
            return null;
        }
    }

    /**
     * Rebuilds a fresh copy of the editor tree rooted at {@code node},
     * substituting {@code replacement} wherever {@code target} appears.
     * Every {@link SplitPane} along the path down to {@code target} is
     * reconstructed from scratch, exactly like {@link #cloneTreeExcluding}
     * below \u2014 the pair together mean nothing in the editor area is ever
     * mutated in place, whether a pane is being added or removed, at any
     * nesting depth. {@code target} itself is not recursed into; it's
     * simply swapped for {@code replacement} (which is expected to
     * already contain {@code target} as one of its own children, as
     * {@link #splitEditorGroup} sets up before calling this).
     */
    private javafx.scene.Node cloneTreeReplacing(javafx.scene.Node node,
                                                 javafx.scene.Node target,
                                                 javafx.scene.Node replacement) {
        if (node == target) return replacement;
        if (node instanceof SplitPane sp) {
            List<javafx.scene.Node> children = new java.util.ArrayList<>();
            for (javafx.scene.Node child : new java.util.ArrayList<>(sp.getItems())) {
                children.add(cloneTreeReplacing(child, target, replacement));
            }
            SplitPane fresh = new SplitPane();
            fresh.setOrientation(sp.getOrientation());
            fresh.getItems().addAll(children);
            return fresh;
        }
        return node;
    }

    /**
     * Rebuilds a fresh copy of the editor tree rooted at {@code node},
     * dropping {@code excluded} wherever it appears. Every {@link SplitPane}
     * along the way is reconstructed from scratch (same orientation, same
     * remaining children) rather than reused \u2014 {@link TabPane} leaves are
     * the only nodes carried over as-is, since they hold the actual open
     * tabs. A branch that collapses to a single child returns that child
     * directly instead of a redundant one-item SplitPane, so closing one
     * pane out of a deep grid only ever un-splits the row/column it was
     * actually in \u2014 the rest of the layout is untouched.
     *
     * <p>This (and {@link #cloneTreeReplacing} above) replaces the earlier
     * approach of surgically mutating a live SplitPane's items in place.
     * That incremental surgery left the SplitPane's own skin in a bad
     * state often enough to matter \u2014 sometimes a stale ghost render,
     * sometimes the editor area going fully black. Building an entirely
     * new SplitPane at each level sidesteps that class of bug outright:
     * there is never a SplitPane whose skin has to reconcile a mutation,
     * because none of the SplitPanes it renders were ever mutated.
     */
    private javafx.scene.Node cloneTreeExcluding(javafx.scene.Node node, TabPane excluded) {
        if (node == excluded) return null;
        if (node instanceof TabPane) return node;
        if (node instanceof SplitPane sp) {
            List<javafx.scene.Node> kept = new java.util.ArrayList<>();
            for (javafx.scene.Node child : new java.util.ArrayList<>(sp.getItems())) {
                javafx.scene.Node rebuilt = cloneTreeExcluding(child, excluded);
                if (rebuilt != null) kept.add(rebuilt);
            }
            if (kept.isEmpty()) return null;
            if (kept.size() == 1) return kept.get(0);
            SplitPane fresh = new SplitPane();
            fresh.setOrientation(sp.getOrientation());
            fresh.getItems().addAll(kept);
            return fresh;
        }
        return node;
    }

    /** Called whenever a (non-main) editor group's last tab closes: rebuilds
     *  the editor tree without it, exactly like IntelliJ collapsing a split
     *  back down \u2014 in a multi-pane grid, only the row/column that pane was
     *  actually in un-splits; every other pane is untouched, and remaining
     *  tabs never land in a stray blank pane. */
    private void closeEditorGroup(TabPane group) {
        editorGroups.remove(group);
        javafx.scene.Node rebuilt = cloneTreeExcluding(editorRoot, group);
        if (rebuilt == null) rebuilt = editorTabs;   // shouldn't happen; safety net
        if (rebuilt != editorRoot) {
            editorArea.getChildren().set(editorArea.getChildren().indexOf(editorRoot), rebuilt);
            editorRoot = rebuilt;
        }
        if (activeEditorGroup == group) {
            activeEditorGroup = editorGroups.isEmpty() ? editorTabs : editorGroups.get(0);
        }
        updateEditorVisibility();
        Platform.runLater(() -> {
            Tab sel = activeEditorGroup.getSelectionModel().getSelectedItem();
            if (sel instanceof EditorTab et) et.focusEditor();
        });
    }

    /** Hard reset back to a single unsplit editor group \u2014 used when
     *  closing a project, so the next one doesn't inherit stale splits.
     *  Safe to call even if every split already collapsed away on its own
     *  as its tabs closed (the usual path); this just makes sure. */
    private void collapseAllSplits() {
        for (TabPane group : new java.util.ArrayList<>(editorGroups)) {
            if (group != editorTabs) group.getTabs().clear();
        }
        if (editorRoot != editorTabs) {
            editorArea.getChildren().set(editorArea.getChildren().indexOf(editorRoot), editorTabs);
            editorRoot = editorTabs;
        }
        editorGroups.clear();
        editorGroups.add(editorTabs);
        activeEditorGroup = editorTabs;
    }

    private void closeEditorTabsWhere(java.util.function.Predicate<Tab> predicate) {
        for (TabPane group : new java.util.ArrayList<>(editorGroups)) {
            List<Tab> toClose = new java.util.ArrayList<>();
            for (Tab t : group.getTabs()) {
                if (predicate.test(t)) toClose.add(t);
            }
            group.getTabs().removeAll(toClose);
        }
    }

    private void closeEditorTabsRelativeTo(Tab reference, boolean toTheLeft) {
        TabPane group = groupOf(reference);
        int idx = group.getTabs().indexOf(reference);
        if (idx < 0) return;
        List<Tab> toClose = new java.util.ArrayList<>();
        List<Tab> tabs = group.getTabs();
        for (int i = 0; i < tabs.size(); i++) {
            boolean onTargetSide = toTheLeft ? i < idx : i > idx;
            if (onTargetSide && !isPinned(tabs.get(i))) toClose.add(tabs.get(i));
        }
        tabs.removeAll(toClose);
    }

    private void openTabLocationInTerminal(EditorTab tab) {
        if (tab.getPath() == null) return;
        Path dir = tab.getPath().getParent();
        toggleBottomPanel(true);
        bottomTabs.getSelectionModel().select(3);
        iconRail.selectBottom(4);
        terminal.openNewSessionIn(dir);
    }

    private void revealInFileManager(EditorTab tab) {
        if (tab.getPath() == null) return;
        if (!java.awt.Desktop.isDesktopSupported()) {
            showComingSoon(systemFileManagerLabel());
            return;
        }
        try {
            java.awt.Desktop.getDesktop().open(tab.getPath().getParent().toFile());
        } catch (IOException ex) {
            error("Could not open " + systemFileManagerLabel(), ex.getMessage());
        }
    }

    private static String systemFileManagerLabel() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return "Finder";
        if (os.contains("win")) return "Explorer";
        return "Files";
    }

    private void renameTabFile(EditorTab tab) {
        Path path = tab.getPath();
        if (path == null) return;
        String currentName = path.getFileName().toString();
        Optional<String> input = prompt("Rename File", "New name:", currentName);
        if (input.isEmpty() || input.get().isBlank()
                || input.get().equals(currentName)) {
            return;
        }
        Path target = path.resolveSibling(input.get().trim());
        if (Files.exists(target)) {
            error("Rename", target.getFileName() + " already exists here.");
            return;
        }
        try {
            Files.writeString(path, tab.getEditorText());
            Files.move(path, target);
            tab.markSaved(target);
            fileExplorer.refresh(target);
            console.println("\u2713 Renamed to " + target.getFileName());
        } catch (IOException ex) {
            error("Could not rename file", ex.getMessage());
        }
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
            recheckMavenSync(tab);
        } catch (IOException ex) {
            error("Could not save file", ex.getMessage());
        }
    }

    private void closeCurrentTab() {
        if (activeEditorGroup == null || activeEditorGroup.getSelectionModel() == null) return;
        Tab t = activeEditorGroup.getSelectionModel().getSelectedItem();
        if (t != null) activeEditorGroup.getTabs().remove(t);
    }

    private EditorTab currentEditor() {
        if (activeEditorGroup == null || activeEditorGroup.getSelectionModel() == null) return null;
        Tab t = activeEditorGroup.getSelectionModel().getSelectedItem();
        return (t instanceof EditorTab et) ? et : null;
    }

    private void withEditor(java.util.function.Consumer<EditorTab> action) {
        EditorTab tab = currentEditor();
        if (tab != null) action.accept(tab);
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Lumina");
        alert.setHeaderText("Lumina IDE 1.24");
        alert.setContentText("""
                A luminous, lightweight Java IDE.
                Built with Java 25, JavaFX and Maven.

                Fix: both creating and closing editor splits now
                rebuild the tree from scratch instead of mutating
                a live SplitPane \u2014 multi-pane grid layouts (split
                an already-split pane) are as robust as the first
                split, matching IntelliJ.""");
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