package dev.lumina.ui;

import dev.lumina.util.Settings;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The Terminal tool window: a strip of session tabs ("Local", "Local (2)", ...)
 * plus a standard tool window header with title, "+" to add another session,
 * "▾" dropdown listing available shells, settings shortcut, options menu, and hide control.
 *
 * <p>Exposes the same {@code start}/{@code stop}/{@code focusInput}/
 * {@code sendCommand} API a single {@link TerminalPane} did, delegating to
 * whichever session tab is currently active.
 */
public final class TerminalToolWindow extends BorderPane {

    private final ToolWindowHeader header;
    private final TabPane tabs = new TabPane();
    private final Supplier<Path> projectRoot;
    private final Runnable onOpenSettings;
    private final Runnable onAllSessionsClosed;
    private final Runnable onHideRequested;
    private final Consumer<Tab> onMoveToEditor;
    private int sessionCounter = 0;
    private boolean everHadSession = false;
    private boolean toolbarVisible = true;
    private final Map<Tab, Button> tabButtons = new HashMap<>();

    public TerminalToolWindow(Supplier<Path> projectRoot, Runnable onOpenSettings,
                               Runnable onAllSessionsClosed, Runnable onHideRequested,
                               Consumer<Tab> onMoveToEditor) {
        this.projectRoot = projectRoot;
        this.onOpenSettings = onOpenSettings;
        this.onAllSessionsClosed = onAllSessionsClosed;
        this.onHideRequested = onHideRequested;
        this.onMoveToEditor = onMoveToEditor;
        getStyleClass().add("terminal-tool-window");

        this.header = new ToolWindowHeader("Terminal");
        header.setOnHide(onHideRequested);

        header.addLeftIconButton("+", "New Session", () -> addSessionTab(null));
        Button shellDropdown = header.addLeftIconButton("▾", "Select Shell", null);
        shellDropdown.setOnAction(e -> showShellMenu(shellDropdown));

        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.setOnAction(e -> onOpenSettings.run());
        header.addOptionsMenuItem(settingsItem);

        header.setOnToolbarToggle(show -> {
            toolbarVisible = show;
            for (Tab t : tabs.getTabs()) {
                if (t.getContent() instanceof TerminalPane tp) {
                    tp.setToolbarVisible(toolbarVisible);
                }
            }
        });

        tabs.getStyleClass().addAll("tool-tabs", "terminal-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) return;
            if (sel.getContent() instanceof TerminalPane tp) {
                tp.focusInput();
            }
        });

        setTop(header);
        setCenter(tabs);
    }

    public ToolWindowHeader getHeader() {
        return header;
    }

    public void setOnMaximize(Runnable onMaximize) {
        header.setOnMaximize(onMaximize);
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

    /** True if a real session tab is currently active. */
    public boolean hasSession() {
        return activePane() != null;
    }

    /** Always opens a brand-new session in the given directory. */
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

        Button tabBtn = header.addTab(name, true, () -> tabs.getSelectionModel().select(tab), () -> {
            pane.stop();
            tabs.getTabs().remove(tab);
            tabButtons.remove(tab);
            if (realSessionTabs().isEmpty()) handleAllSessionsClosed();
        });
        tabButtons.put(tab, tabBtn);

        tabs.getTabs().add(tab);
        tabs.getSelectionModel().select(tab);
        if (shellOverride != null) {
            pane.startWithShell(dir, shellOverride);
        } else {
            pane.start(dir);
        }
        pane.focusInput();
        return tab;
    }

    /** Reset when every session tab is closed. */
    private void handleAllSessionsClosed() {
        everHadSession = false;
        sessionCounter = 0;
        if (onAllSessionsClosed != null) onAllSessionsClosed.run();
    }

    private List<Tab> realSessionTabs() {
        return new ArrayList<>(tabs.getTabs());
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
        ssh.setDisable(true);
        menu.getItems().add(ssh);
        menu.getItems().add(new SeparatorMenuItem());
        MenuItem settings = new MenuItem("Settings");
        settings.setOnAction(e -> onOpenSettings.run());
        menu.getItems().add(settings);
        menu.show(anchor, Side.BOTTOM, 0, 4);
    }

    // ------------------------------------------------- session tab right-click

    /**
     * Standard terminal-tab context menu.
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
            tabButtons.remove(tab);
            if (realSessionTabs().isEmpty()) handleAllSessionsClosed();
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
            if (!name.isBlank()) {
                tab.setText(name.trim());
                Button btn = tabButtons.get(tab);
                if (btn != null) btn.setText(name.trim());
            }
        });
    }

    private void closeSessions(List<Tab> toClose) {
        if (toClose.isEmpty()) return;
        for (Tab t : toClose) {
            if (t.getContent() instanceof TerminalPane tp) tp.stop();
            tabButtons.remove(t);
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