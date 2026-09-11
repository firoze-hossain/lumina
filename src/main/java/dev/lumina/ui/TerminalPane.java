package dev.lumina.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built-in terminal backed by the system shell (bash/zsh on Unix, cmd on
 * Windows), rendered as a single continuous view \u2014 like IntelliJ's own
 * terminal \u2014 instead of a scrolling log with a separate input box glued
 * underneath it. Typing happens directly in the same scrolling area, right
 * after the prompt; there's no second widget stealing space at the bottom.
 *
 * <p><b>What this is and isn't:</b> commands run through a plain OS pipe,
 * not a real pseudo-terminal (PTY), so this fakes the interactive parts a
 * PTY would normally give you for free: it locally echoes what you type
 * (a real pipe has no echo of its own) and only sends a command to the
 * shell once you press Enter. That means:
 * <ul>
 *   <li>Normal commands, git, mvn, ls, colored output \u2014 all work.</li>
 *   <li>Ctrl+C can't deliver a real SIGINT the way a PTY's line discipline
 *       does (that signal only exists at the PTY level) \u2014 here it kills
 *       and restarts the shell instead, which stops a stuck command but
 *       isn't the same thing.</li>
 *   <li>Full-screen terminal apps (vim, htop, less) need a real PTY and
 *       won't render correctly here.</li>
 * </ul>
 * A real PTY (the same {@code pty4j} library IntelliJ itself uses) would
 * close both gaps, but it's a new native-library dependency in a modular
 * app \u2014 worth doing as a deliberate follow-up you can test, not something
 * to wire in blind.
 */
public class TerminalPane extends BorderPane {

    private final StyleClassedTextArea output = new StyleClassedTextArea();
    private final StringBuilder currentLine = new StringBuilder();
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;
    private int inputStart = 0;

    private Process shell;
    private BufferedWriter stdin;
    private Path workingDir = Path.of(System.getProperty("user.home"));

    // ---- ANSI handling state (persists across chunks of streamed output) ----
    private String pendingAnsi = "";
    private String currentStyle = null;
    private static final Pattern CSI = Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]");
    private static final Pattern OSC = Pattern.compile("\u001B][^\u0007\u001B]*(\u0007|\u001B\\\\)");

    public TerminalPane() {
        getStyleClass().add("terminal-pane");

        Button restart = new Button("\u21BB");
        restart.getStyleClass().add("console-button");
        restart.setTooltip(new Tooltip("Restart"));
        restart.setOnAction(e -> start(workingDir));

        Button clear = new Button("\u2715");
        clear.getStyleClass().add("console-button");
        clear.setTooltip(new Tooltip("Clear"));
        clear.setOnAction(e -> { output.clear(); inputStart = 0; });

        VBox rail = new VBox(6, restart, clear);
        rail.getStyleClass().add("terminal-rail");
        rail.setAlignment(Pos.TOP_CENTER);
        rail.setPadding(new Insets(6, 4, 6, 4));

        output.setEditable(false);   // suppress RichTextFX's own default typing
                                      // behavior; input is handled manually
                                      // below so it lands in the right place
        output.setWrapText(true);
        output.getStyleClass().add("terminal-output");
        output.setOnKeyTyped(this::onKeyTyped);
        output.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, this::onKeyPressed);

        setLeft(rail);
        setCenter(new VirtualizedScrollPane<>(output));
        setMinHeight(120);
    }

    /** (Re)start the shell in the given directory. */
    public void start(Path dir) {
        stop();
        this.workingDir = dir != null ? dir : Path.of(System.getProperty("user.home"));
        output.clear();
        inputStart = 0;
        pendingAnsi = "";
        currentStyle = null;
        currentLine.setLength(0);

        List<String> cmd = shellCommand();
        ProcessBuilder pb = new ProcessBuilder(cmd)
                .directory(workingDir.toFile())
                .redirectErrorStream(true);
        pb.environment().put("TERM", "xterm-256color");
        try {
            shell = pb.start();
        } catch (IOException e) {
            append("Could not start shell " + cmd + ": " + e.getMessage() + "\n");
            return;
        }
        stdin = new BufferedWriter(new OutputStreamWriter(
                shell.getOutputStream(), StandardCharsets.UTF_8));

        Process p = shell;
        Thread reader = new Thread(() -> {
            char[] buf = new char[2048];
            try (InputStreamReader in = new InputStreamReader(
                    p.getInputStream(), StandardCharsets.UTF_8)) {
                int n;
                while ((n = in.read(buf)) != -1) {
                    append(new String(buf, 0, n));
                }
            } catch (IOException ignored) {
            }
            if (p == shell) append("\n[shell exited]\n");
        }, "lumina-terminal-reader");
        reader.setDaemon(true);
        reader.start();
        promptForInput();
    }

    public void stop() {
        if (shell != null && shell.isAlive()) shell.destroy();
        shell = null;
        stdin = null;
    }

    public void focusInput() {
        Platform.runLater(() -> {
            output.requestFocus();
            output.moveTo(output.getLength());
        });
    }

    /** Programmatically run a command in this terminal (e.g. jdb attach). */
    public void sendCommand(String command) {
        Platform.runLater(() -> {
            focusInput();
            currentLine.setLength(0);
            currentLine.append(command);
            int pos = output.getLength();
            output.insertText(pos, command);
            output.moveTo(output.getLength());
            submitLine();
        });
    }

    // ---------------------------------------------------------------- input

    /** Shows a fresh prompt and opens the input line right after it, in
     *  the same scrolling view \u2014 no separate input widget. */
    private void promptForInput() {
        Platform.runLater(() -> {
            String folder = workingDir.getFileName() != null
                    ? workingDir.getFileName().toString() : workingDir.toString();
            int start = output.getLength();
            output.appendText(folder + " % ");
            output.setStyleClass(start, output.getLength(), "terminal-prompt-inline");
            inputStart = output.getLength();
            currentLine.setLength(0);
            output.moveTo(output.getLength());
            output.requestFollowCaret();
        });
    }

    private void onKeyTyped(javafx.scene.input.KeyEvent e) {
        String ch = e.getCharacter();
        if (ch.isEmpty()) return;
        char c = ch.charAt(0);
        if (c < 0x20 || c == 0x7F) return;   // control chars handled in onKeyPressed
        ensureCaretAtEnd();
        currentLine.append(ch);
        output.insertText(output.getLength(), ch);
        output.moveTo(output.getLength());
        e.consume();
    }

    private void onKeyPressed(javafx.scene.input.KeyEvent e) {
        KeyCode code = e.getCode();
        if (code == KeyCode.ENTER) {
            submitLine();
            e.consume();
        } else if (code == KeyCode.BACK_SPACE) {
            ensureCaretAtEnd();
            if (currentLine.length() > 0) {
                currentLine.deleteCharAt(currentLine.length() - 1);
                int end = output.getLength();
                output.deleteText(end - 1, end);
            }
            e.consume();
        } else if (code == KeyCode.UP) {
            navigateHistory(-1);
            e.consume();
        } else if (code == KeyCode.DOWN) {
            navigateHistory(1);
            e.consume();
        } else if (code == KeyCode.C && (e.isControlDown() || e.isShortcutDown())
                && output.getSelectedText().isEmpty()) {
            // Not a real SIGINT (that needs a PTY's line discipline) \u2014 best
            // effort is restarting the shell so a stuck command doesn't
            // wedge the whole terminal.
            append("^C\n");
            start(workingDir);
            e.consume();
        } else if (code == KeyCode.LEFT || code == KeyCode.RIGHT
                || code == KeyCode.HOME || code == KeyCode.END) {
            e.consume();   // no free caret movement in the simplified line model
        }
    }

    private void ensureCaretAtEnd() {
        if (output.getCaretPosition() != output.getLength()) {
            output.moveTo(output.getLength());
        }
    }

    private void submitLine() {
        String line = currentLine.toString();
        output.appendText("\n");
        if (shell == null || !shell.isAlive()) {
            start(workingDir);
            return;
        }
        if (!line.isBlank()) {
            history.add(line);
        }
        historyIndex = history.size();
        try {
            stdin.write(line);
            stdin.newLine();
            stdin.flush();
        } catch (IOException e) {
            append("[could not write to shell: " + e.getMessage() + "]\n");
        }
        promptForInput();
    }

    private void navigateHistory(int delta) {
        if (history.isEmpty()) return;
        historyIndex = Math.max(0, Math.min(history.size(), historyIndex + delta));
        String replacement = historyIndex < history.size() ? history.get(historyIndex) : "";
        output.deleteText(inputStart, output.getLength());
        output.insertText(inputStart, replacement);
        currentLine.setLength(0);
        currentLine.append(replacement);
        output.moveTo(output.getLength());
    }

    // -------------------------------------------------------------- helpers

    private static List<String> shellCommand() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return List.of("cmd.exe");
        String sh = System.getenv("SHELL");
        return List.of(sh != null && !sh.isBlank() ? sh : "/bin/bash");
    }

    // ------------------------------------------------------- ANSI decoding

    /**
     * Appends a chunk of raw shell output, translating ANSI SGR color codes
     * into style classes on the styled text area and dropping other escape
     * sequences (cursor movement, screen clears, OSC title-setting) that a
     * scrolling log can't meaningfully act on anyway.
     */
    private void append(String text) {
        Platform.runLater(() -> {
            String combined = pendingAnsi + text;
            pendingAnsi = "";
            StringBuilder plain = new StringBuilder();
            int i = 0;
            while (i < combined.length()) {
                char c = combined.charAt(i);
                if (c != '\u001B') {
                    plain.append(c);
                    i++;
                    continue;
                }
                String rest = combined.substring(i);
                Matcher csi = CSI.matcher(rest);
                Matcher osc = OSC.matcher(rest);
                if (csi.lookingAt()) {
                    flush(plain);
                    String seq = csi.group();
                    if (seq.endsWith("m")) currentStyle = mapSgr(seq);
                    i += seq.length();
                } else if (osc.lookingAt()) {
                    flush(plain);
                    i += osc.group().length();
                } else if (rest.length() < 24) {
                    // Might be a sequence split across two stream reads \u2014 hold
                    // it back and complete it once more bytes arrive.
                    pendingAnsi = rest;
                    break;
                } else {
                    // Not a recognized/completable escape \u2014 drop just the ESC.
                    i++;
                }
            }
            flush(plain);
        });
    }

    private void flush(StringBuilder plain) {
        if (plain.isEmpty()) return;
        int start = output.getLength();
        output.appendText(plain.toString());
        output.setStyleClass(start, output.getLength(),
                currentStyle == null ? "" : currentStyle);
        plain.setLength(0);
        inputStart = output.getLength();
        output.moveTo(output.getLength());
        output.requestFollowCaret();
    }

    /** Maps the numeric codes in an SGR sequence like "\e[1;32m" to a style class. */
    private static String mapSgr(String seq) {
        String body = seq.substring(2, seq.length() - 1);   // strip ESC[ and m
        if (body.isEmpty()) return null;
        String result = null;
        for (String code : body.split(";")) {
            switch (code) {
                case "", "0", "39" -> result = null;
                case "30", "31", "32", "33", "34", "35", "36", "37",
                        "90", "91", "92", "93", "94", "95", "96", "97" ->
                        result = "ansi-fg-" + code;
                default -> { }
            }
        }
        return result;
    }
}