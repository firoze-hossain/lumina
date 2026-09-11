package dev.lumina.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
 * Windows). Streams are piped, so line-based commands work (git, mvn, ls…);
 * full-screen TUI apps (vim, htop) need a real PTY and are out of scope.
 *
 * <p>Output renders through a {@link StyleClassedTextArea} with basic ANSI
 * SGR color support, instead of a plain {@code TextArea}: most real shell
 * prompts (oh-my-zsh, starship, colored `ls`/`git`) send color escape codes,
 * which a plain text box shows as literal garbage like {@code ^[[32m} \u2014
 * that garbled-looking output is almost certainly what "not good" meant.
 */
public class TerminalPane extends BorderPane {

    private final StyleClassedTextArea output = new StyleClassedTextArea();
    private final TextField input = new TextField();
    private final Label promptLabel = new Label();
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

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

        Label title = new Label("Terminal");
        title.getStyleClass().add("panel-header");

        Button restart = new Button("\u21BB");
        restart.getStyleClass().add("console-button");
        restart.setTooltip(new Tooltip("Restart"));
        restart.setOnAction(e -> start(workingDir));

        Button clear = new Button("\u2715");
        clear.getStyleClass().add("console-button");
        clear.setTooltip(new Tooltip("Clear"));
        clear.setOnAction(e -> output.clear());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, title, spacer, restart, clear);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 10, 6, 12));
        header.getStyleClass().add("console-header");

        output.setEditable(false);
        output.setWrapText(true);
        output.getStyleClass().add("terminal-output");

        promptLabel.getStyleClass().add("terminal-prompt");

        input.getStyleClass().add("terminal-input");
        input.setOnAction(e -> submit());
        input.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case UP -> navigateHistory(-1);
                case DOWN -> navigateHistory(1);
                default -> { }
            }
        });
        HBox.setHgrow(input, Priority.ALWAYS);

        HBox inputRow = new HBox(6, promptLabel, input);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.setPadding(new Insets(4, 12, 8, 12));
        inputRow.getStyleClass().add("terminal-input-row");

        setTop(header);
        setCenter(new VirtualizedScrollPane<>(output));
        setBottom(inputRow);
        setMinHeight(120);
    }

    /** (Re)start the shell in the given directory. */
    public void start(Path dir) {
        stop();
        this.workingDir = dir != null ? dir : Path.of(System.getProperty("user.home"));
        promptLabel.setText(shortPrompt(workingDir));
        output.clear();
        pendingAnsi = "";
        currentStyle = null;

        List<String> cmd = shellCommand();
        try {
            shell = new ProcessBuilder(cmd)
                    .directory(workingDir.toFile())
                    .redirectErrorStream(true)
                    .start();
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
            append("\n[shell exited]\n");
        }, "lumina-terminal-reader");
        reader.setDaemon(true);
        reader.start();
    }

    public void stop() {
        if (shell != null && shell.isAlive()) shell.destroy();
        shell = null;
        stdin = null;
    }

    public void focusInput() {
        Platform.runLater(input::requestFocus);
    }

    /** Programmatically run a command in this terminal (e.g. jdb attach). */
    public void sendCommand(String command) {
        Platform.runLater(() -> {
            input.setText(command);
            submit();
        });
    }

    // ---------------------------------------------------------------- input

    private void submit() {
        String line = input.getText();
        input.clear();
        if (shell == null || !shell.isAlive()) {
            start(workingDir);
        }
        if (stdin == null) return;
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
    }

    private void navigateHistory(int delta) {
        if (history.isEmpty()) return;
        historyIndex = Math.max(0, Math.min(history.size(), historyIndex + delta));
        input.setText(historyIndex < history.size() ? history.get(historyIndex) : "");
        input.end();
    }

    // -------------------------------------------------------------- helpers

    private static List<String> shellCommand() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return List.of("cmd.exe");
        String sh = System.getenv("SHELL");
        return List.of(sh != null && !sh.isBlank() ? sh : "/bin/bash");
    }

    private static String abbreviate(Path p) {
        String home = System.getProperty("user.home");
        String s = p.toAbsolutePath().toString();
        return s.startsWith(home) ? "~" + s.substring(home.length()) : s;
    }

    /** e.g. "lumina %" \u2014 a short, real-looking shell prompt for the input row. */
    private static String shortPrompt(Path dir) {
        String folder = dir.getFileName() != null ? dir.getFileName().toString() : abbreviate(dir);
        return folder + " %";
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