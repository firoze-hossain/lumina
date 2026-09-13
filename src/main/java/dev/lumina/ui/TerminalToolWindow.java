package dev.lumina.ui;

import dev.lumina.util.Settings;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The Terminal tool window as IntelliJ actually lays it out: a strip of
 * session tabs ("Local", "Local (2)", ...) plus a "+" to add another and a
 * "\u25be" dropdown next to it listing available shells, an (unimplemented,
 * honestly disabled) SSH session entry, and a Settings shortcut straight to
 * Settings &gt; Tools &gt; Terminal. Right-clicking a session tab opens a
 * full context menu matching IntelliJ's own \u2014 see
 * {@link #buildSessionContextMenu} for what's real vs. shown-but-disabled.
 *
 * <p>Exposes the same {@code start}/{@code stop}/{@code focusInput}/
 * {@code sendCommand} API a single {@link TerminalPane} did, delegating to
 * whichever session tab is currently active, so the rest of the app (which
 * only ever drove one terminal) didn't need to change to gain multiple
 * sessions.
 */
public final class TerminalToolWindow extends BorderPane {

    private final TabPane tabs = new TabPane();
    private final Supplier<Path> projectRoot;
    private final Runnable onOpenSettings;
    private final Runnable onAllSessionsClosed;
    private final Runnable onHideRequested;
    private final Consumer<Tab> onMoveToEditor;
    private int sessionCounter = 0;
    private boolean everHadSession = false;
    private boolean toolbarVisible = true;

    public TerminalToolWindow(Supplier<Path> projectRoot, Runnable onOpenSettings,
                               Runnable onAllSessionsClosed, Runnable onHideRequested,
                               Consumer<Tab> onMoveToEditor) {
        this.projectRoot = projectRoot;
        this.onOpenSettings = onOpenSettings;
        this.onAllSessionsClosed = onAllSessionsClosed;
        this.onHideRequested = onHideRequested;
        this.onMoveToEditor = onMoveToEditor;
        getStyleClass().add("terminal-tool-window");
        tabs.getStyleClass().addAll("tool-tabs", "terminal-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null || !sel.getStyleClass().contains("new-tab-sentinel")) return;
            if (everHadSession && tabs.getTabs().size() == 1) {
                handleAllSessionsClosed();
            } else {
                addSessionTab(null);
            }
        });
        // No session tab yet \u2014 the caller drives the first one via start(),
        // once it actually knows the directory to start in (home dir at app
        // launch, then the real project dir once one's opened). Starting a
        // shell here too would just mean spawning and immediately killing
        // one the moment start() is called moments later.
        ensureSentinel();
        setCenter(tabs);
    }

    // ------------------------------------------------------------ public API

    public void start(Path dir) {
        TerminalPane active = activePane();
        if (active != null) {
            active.start(dir);
        } else {
            addSessionTabIn(dir, null);
        }
    }

    public void stop() {
        for (Tab t : tabs.getTabs()) {
            if (t.getContent() instanceof TerminalPane tp) tp.stop();
        }
    }

    public void focusInput() {
        TerminalPane active = activePane();
        if (active != null) active.focusInput();
    }

    /** True if a real session tab is currently active (as opposed to the
     *  "+/\u25be" sentinel being the only thing left, e.g. right after the
     *  last session tab was closed). */
    public boolean hasSession() {
        return activePane() != null;
    }

    /** Always opens a brand-new session in the given directory (used by
     *  "Open In &gt; Terminal" from a file/tab's right-click menu), rather
     *  than restarting whatever's already active. */
    public void openNewSessionIn(Path dir) {
        addSessionTabIn(dir, null);
    }

    public void sendCommand(String command) {
        TerminalPane active = activePane();
        if (active != null) active.sendCommand(command);
    }

    private TerminalPane activePane() {
        Tab sel = tabs.getSelectionModel().getSelectedItem();
        return sel != null && sel.getContent() instanceof TerminalPane tp ? tp : null;
    }

    // ------------------------------------------------------------ sessions

    private Tab addSessionTab(String shellOverride) {
        return addSessionTabIn(projectRoot.get(), shellOverride);
    }

    private Tab addSessionTabIn(Path dir, String shellOverride) {
        sessionCounter++;
        everHadSession = true;
        String base = defaultTabName();
        String name = sessionCounter == 1 ? base : base + " (" + sessionCounter + ")";
        TerminalPane pane = new TerminalPane();
        pane.setToolbarVisible(toolbarVisible);
        Tab tab = new Tab(name, pane);
        tab.setContextMenu(buildSessionContextMenu(tab, pane));
        tab.setOnCloseRequest(e -> pane.stop());
        tabs.getTabs().add(sentinelIndex(), tab);
        tabs.getSelectionModel().select(tab);
        if (shellOverride != null) {
            pane.startWithShell(dir, shellOverride);
        } else {
            pane.start(dir);
        }
        pane.focusInput();
        ensureSentinel();
        return tab;
    }

    /** Reset when every session tab is closed \u2014 shared by the sentinel-
     *  reselection path and "Close All Tabs" from the context menu, so
     *  numbering resets to "Local" again either way instead of only when
     *  tabs are closed one at a time. */
    private void handleAllSessionsClosed() {
        everHadSession = false;
        sessionCounter = 0;
        if (onAllSessionsClosed != null) onAllSessionsClosed.run();
    }

    private List<Tab> realSessionTabs() {
        List<Tab> real = new ArrayList<>(tabs.getTabs());
        real.removeIf(t -> t.getStyleClass().contains("new-tab-sentinel"));
        return real;
    }

    private int sentinelIndex() {
        for (int i = 0; i < tabs.getTabs().size(); i++) {
            if (tabs.getTabs().get(i).getStyleClass().contains("new-tab-sentinel")) return i;
        }
        return tabs.getTabs().size();
    }

    /** Re-adds the trailing "+ / \u25be" pseudo-tab, since real session tabs
     *  are always inserted before it. */
    private void ensureSentinel() {
        tabs.getTabs().removeIf(t -> t.getStyleClass().contains("new-tab-sentinel"));
        Tab plus = new Tab();
        plus.getStyleClass().add("new-tab-sentinel");
        plus.setClosable(false);

        Label plusLabel = new Label("+");
        plusLabel.getStyleClass().add("terminal-tab-plus");
        Button dropdown = new Button("\u25BE");
        dropdown.getStyleClass().add("terminal-tab-dropdown");
        dropdown.setOnMouseClicked(ev -> {
            ev.consume();   // don't also trigger the sentinel's own selection
            showShellMenu(dropdown);
        });
        HBox graphic = new HBox(2, plusLabel, dropdown);
        graphic.getStyleClass().add("terminal-tab-new-session");
        graphic.setAlignment(Pos.CENTER);
        plus.setGraphic(graphic);
        plus.setContent(new Region());
        tabs.getTabs().add(plus);
    }

    private void showShellMenu(Node anchor) {
        ContextMenu menu = new ContextMenu();
        List<String> shells = detectShells();
        for (String sh : shells) {
            MenuItem item = new MenuItem(shellLabel(sh));
            item.setOnAction(e -> addSessionTab(sh));
            menu.getItems().add(item);
        }
        if (shells.isEmpty()) {
            MenuItem def = new MenuItem("New Session");
            def.setOnAction(e -> addSessionTab(null));
            menu.getItems().add(def);
        }
        MenuItem ssh = new MenuItem("New SSH Session\u2026");
        ssh.setDisable(true);   // no SSH transport implemented \u2014 shown, not faked
        menu.getItems().add(ssh);
        menu.getItems().add(new SeparatorMenuItem());
        MenuItem settings = new MenuItem("Settings");
        settings.setOnAction(e -> onOpenSettings.run());
        menu.getItems().add(settings);
        menu.show(anchor, Side.BOTTOM, 0, 4);
    }

    // ------------------------------------------------- session tab right-click

    /**
     * Matches IntelliJ's own terminal-tab menu item-for-item. Real:
     * rename, move-to-editor, close (tab/all/others), next/previous/list
     * tab navigation, Settings, and Show Toolbar. Shown but honestly
     * disabled, since the subsystems behind them don't exist here: Split
     * (terminal splitting is a separate feature from editor splitting,
     * not built), Group Tabs, View Mode, Move to, Resize, Remove from
     * Sidebar. Terminal Engine lists the one engine there is.
     */
    private ContextMenu buildSessionContextMenu(Tab tab, TerminalPane pane) {
        ContextMenu menu = new ContextMenu();

        MenuItem rename = new MenuItem("Rename Session");
        rename.setOnAction(e -> renameSession(tab));

        MenuItem moveToEditor = new MenuItem("Move to Editor");
        moveToEditor.setDisable(onMoveToEditor == null);
        moveToEditor.setOnAction(e -> {
            tabs.getTabs().remove(tab);
            onMoveToEditor.accept(tab);
        });

        MenuItem close = new MenuItem("Close Tab");
        close.setOnAction(e -> {
            pane.stop();
            tabs.getTabs().remove(tab);
        });

        MenuItem closeAllTabs = new MenuItem("Close All Tabs");
        closeAllTabs.setOnAction(e -> closeSessions(realSessionTabs()));

        MenuItem closeOthers = new MenuItem("Close Other Tabs");
        closeOthers.setOnAction(e -> {
            List<Tab> others = realSessionTabs();
            others.remove(tab);
            closeSessions(others);
        });

        MenuItem splitRight = disabledItem("Split Right");
        MenuItem splitMoveRight = disabledItem("Split and Move Right");
        MenuItem splitDown = disabledItem("Split Down");
        MenuItem splitMoveDown = disabledItem("Split and Move Down");

        MenuItem nextTab = new MenuItem("Select Next Tab");
        nextTab.setOnAction(e -> selectRelativeSession(1));
        MenuItem prevTab = new MenuItem("Select Previous Tab");
        prevTab.setOnAction(e -> selectRelativeSession(-1));
        MenuItem showList = new MenuItem("Show List of Tabs");
        showList.setOnAction(e -> showSessionList());

        Menu terminalEngine = new Menu("Terminal Engine");
        terminalEngine.getItems().add(disabledItem("Reworked 2025"));

        MenuItem settings = new MenuItem("Settings");
        settings.setOnAction(e -> onOpenSettings.run());

        MenuItem closeAll = new MenuItem("Close All");
        closeAll.setOnAction(e -> closeSessions(realSessionTabs()));

        CheckMenuItem showToolbar = new CheckMenuItem("Show Toolbar");
        showToolbar.setOnAction(e -> {
            toolbarVisible = showToolbar.isSelected();
            for (Tab t : tabs.getTabs()) {
                if (t.getContent() instanceof TerminalPane tp) {
                    tp.setToolbarVisible(toolbarVisible);
                }
            }
        });

        MenuItem groupTabs = disabledItem("Group Tabs");
        Menu viewMode = new Menu("View Mode");
        viewMode.getItems().add(disabledItem("Distraction Free"));
        Menu moveTo = new Menu("Move to");
        moveTo.getItems().add(disabledItem("Left Top"));
        Menu resize = new Menu("Resize");
        resize.getItems().add(disabledItem("Maximize"));

        MenuItem removeFromSidebar = disabledItem("Remove from Sidebar");
        MenuItem hide = new MenuItem("Hide");
        hide.setDisable(onHideRequested == null);
        hide.setOnAction(e -> onHideRequested.run());

        menu.getItems().addAll(
                rename, moveToEditor,
                new SeparatorMenuItem(),
                close, closeAllTabs, closeOthers,
                new SeparatorMenuItem(),
                splitRight, splitMoveRight, splitDown, splitMoveDown,
                new SeparatorMenuItem(),
                nextTab, prevTab, showList,
                new SeparatorMenuItem(),
                terminalEngine, settings,
                new SeparatorMenuItem(),
                closeAll, showToolbar, groupTabs, viewMode, moveTo, resize,
                new SeparatorMenuItem(),
                removeFromSidebar, hide);
        menu.setOnShowing(e -> showToolbar.setSelected(toolbarVisible));
        return menu;
    }

    private void renameSession(Tab tab) {
        TextInputDialog dialog = new TextInputDialog(tab.getText());
        dialog.setTitle("Rename Session");
        dialog.setHeaderText(null);
        dialog.setContentText("Session name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) tab.setText(name.trim());
        });
    }

    private void closeSessions(List<Tab> toClose) {
        if (toClose.isEmpty()) return;
        for (Tab t : toClose) {
            if (t.getContent() instanceof TerminalPane tp) tp.stop();
        }
        tabs.getTabs().removeAll(toClose);
        if (realSessionTabs().isEmpty()) handleAllSessionsClosed();
    }

    private void selectRelativeSession(int delta) {
        List<Tab> real = realSessionTabs();
        if (real.isEmpty()) return;
        int idx = real.indexOf(tabs.getSelectionModel().getSelectedItem());
        if (idx < 0) idx = 0;
        int next = ((idx + delta) % real.size() + real.size()) % real.size();
        tabs.getSelectionModel().select(real.get(next));
    }

    private void showSessionList() {
        ContextMenu menu = new ContextMenu();
        for (Tab t : realSessionTabs()) {
            MenuItem item = new MenuItem(t.getText());
            item.setOnAction(e -> tabs.getSelectionModel().select(t));
            menu.getItems().add(item);
        }
        menu.show(tabs, Side.BOTTOM, 0, 0);
    }

    private static MenuItem disabledItem(String text) {
        MenuItem item = new MenuItem(text);
        item.setDisable(true);
        return item;
    }

    private static List<String> detectShells() {
        List<String> found = new ArrayList<>();
        for (String candidate : new String[]{
                "/bin/bash", "/usr/bin/bash", "/bin/zsh", "/usr/bin/zsh", "/bin/sh"}) {
            if (Files.isExecutable(Path.of(candidate))) found.add(candidate);
        }
        return found;
    }

    private static String shellLabel(String path) {
        Path p = Path.of(path);
        String name = p.getFileName() != null ? p.getFileName().toString() : path;
        return name + " (" + path + ")";
    }

    private static String defaultTabName() {
        String v = Settings.get(Settings.TERMINAL_TAB_NAME);
        return v != null && !v.isBlank() ? v : "Local";
    }
}