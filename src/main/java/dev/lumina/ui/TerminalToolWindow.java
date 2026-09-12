package dev.lumina.ui;

import dev.lumina.util.Settings;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The Terminal tool window as IntelliJ actually lays it out: a strip of
 * session tabs ("Local", "Local (2)", ...) plus a "+" to add another and a
 * "\u25be" dropdown next to it listing available shells, an (unimplemented,
 * honestly disabled) SSH session entry, and a Settings shortcut straight to
 * Settings &gt; Tools &gt; Terminal.
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
    private int sessionCounter = 0;
    private boolean everHadSession = false;

    public TerminalToolWindow(Supplier<Path> projectRoot, Runnable onOpenSettings,
                               Runnable onAllSessionsClosed) {
        this.projectRoot = projectRoot;
        this.onOpenSettings = onOpenSettings;
        this.onAllSessionsClosed = onAllSessionsClosed;
        getStyleClass().add("terminal-tool-window");
        tabs.getStyleClass().addAll("tool-tabs", "terminal-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null || !sel.getStyleClass().contains("new-tab-sentinel")) return;
            if (everHadSession && tabs.getTabs().size() == 1) {
                // The user just closed their last remaining session tab
                // (as opposed to this being the very first, construction-
                // time selection, before any session has existed yet) --
                // don't silently spawn a replacement; closing the last tab
                // closes the terminal, the same way re-clicking its rail
                // icon would. Numbering resets too, matching IntelliJ:
                // the next session starts fresh as "Local" again, not
                // picking up from some high count of long-closed tabs.
                everHadSession = false;
                sessionCounter = 0;
                if (onAllSessionsClosed != null) onAllSessionsClosed.run();
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
        Tab tab = new Tab(name, pane);
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