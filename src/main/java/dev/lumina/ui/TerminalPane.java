package dev.lumina.ui;

import com.pty4j.PtyProcess;
import dev.lumina.util.Settings;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built-in terminal, backed by a real pseudo-terminal (pty4j \u2014 the library
 * IntelliJ's own terminal is built on) when it's available, falling back to
 * a plain OS pipe if the native pty layer can't load on this machine.
 *
 * <p>With a real pty, the shell does everything a terminal normally does:
 * echoes what you type, redraws the line on backspace/tab-completion, and
 * receives Ctrl+C as a genuine SIGINT (that translation happens in the
 * kernel's tty driver, which only exists for a real pty \u2014 a plain pipe
 * can't do it at any level of application code). So instead of locally
 * faking an echo and re-implementing line editing, keystrokes are now
 * forwarded to the shell as raw bytes and whatever the shell sends back is
 * just rendered as-is \u2014 the same model a real terminal uses.
 *
 * <p>Rendering still isn't a full VT100/xterm emulator: SGR color codes are
 * decoded, and {@code \r}, {@code \n}, backspace, and line-erase are handled
 * well enough for normal shell use and readline redraws, but there's no
 * cursor-addressing or alternate-screen-buffer support. That's the one gap
 * that keeps full-screen apps (vim, htop, less) from rendering correctly \u2014
 * everything else (git, mvn, ls, interactive prompts, Ctrl+C, tab
 * completion, arrow-key history) works like a real terminal now.
 */
public class TerminalPane extends BorderPane {

    private final StyleClassedTextArea output = new StyleClassedTextArea();
    private final Label ghost = new Label();
    private final Rectangle cursor = new Rectangle();
    private final Timeline cursorBlink = new Timeline(
            new KeyFrame(Duration.millis(600), e -> cursor.setVisible(!cursor.isVisible())));
    private final List<String> history = new ArrayList<>();
    private final StringBuilder typedThisLine = new StringBuilder();
    // Mirrors the shell's own cursor position within typedThisLine, best
    // effort only (we don't see the shell's real line-editor state) -- used
    // solely to keep ghost-suggestion matching sane across Left/Right/Home/
    // End; the actual editing/rendering always comes from the shell itself.
    private int typedCursorPos = 0;
    private String acceptedSuggestion = null;

    private volatile Process shell;
    private String sessionShellOverride;   // set via startWithShell(); wins over Settings
    private boolean usingPty;
    private OutputStream stdin;
    private Path workingDir = Path.of(System.getProperty("user.home"));

    // Position-aware write cursor: writePos is where the next character
    // lands; lineStart is where the current logical line began. When
    // writePos < document length we're "overwriting" (a \r or backspace
    // moved us back), matching how readline redraws a line in place.
    private int writePos = 0;
    private int lineStart = 0;
    private String pendingAnsi = "";
    private String currentStyle = null;

    private static final Pattern CSI = Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]");
    private static final Pattern OSC = Pattern.compile("\u001B][^\u0007\u001B]*(\u0007|\u001B\\\\)");
    private static final Path HISTORY_FILE =
            Path.of(System.getProperty("user.home"), ".lumina", "terminal_history");
    private static final int MAX_HISTORY = 500;

    public TerminalPane() {
        getStyleClass().add("terminal-pane");
        loadHistory();

        Button restart = new Button("\u21BB");
        restart.getStyleClass().add("console-button");
        restart.setTooltip(new Tooltip("Restart"));
        restart.setOnAction(e -> start(workingDir));

        Button clear = new Button("\u2715");
        clear.getStyleClass().add("console-button");
        clear.setTooltip(new Tooltip("Clear"));
        clear.setOnAction(e -> {
            output.clear();
            writePos = 0;
            lineStart = 0;
        });

        VBox rail = new VBox(6, restart, clear);
        rail.getStyleClass().add("terminal-rail");
        rail.setAlignment(Pos.TOP_CENTER);
        rail.setPadding(new Insets(6, 4, 6, 4));

        output.setEditable(false);   // input is forwarded to the shell by
                                      // hand below; RichTextFX's own default
                                      // typing behavior stays switched off
        output.setWrapText(true);
        output.getStyleClass().add("terminal-output");
        output.setStyle(fontSizeStyle());
        output.setOnKeyTyped(this::onKeyTyped);
        output.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        ghost.getStyleClass().add("terminal-ghost-suggestion");
        ghost.setMouseTransparent(true);
        ghost.setVisible(false);

        cursor.getStyleClass().add("terminal-cursor");
        cursor.setMouseTransparent(true);
        applyCursorShape();
        cursorBlink.setCycleCount(Timeline.INDEFINITE);
        cursorBlink.play();

        StackPane overlay = new StackPane(new VirtualizedScrollPane<>(output), ghost, cursor);
        StackPane.setAlignment(ghost, Pos.TOP_LEFT);
        StackPane.setAlignment(cursor, Pos.TOP_LEFT);
        output.caretBoundsProperty().addListener((obs, old, bounds) ->
                bounds.ifPresent(b -> {
                    ghost.setLayoutX(b.getMaxX() + 1);
                    ghost.setLayoutY(b.getMinY());
                    positionCursor(b);
                    // Any caret movement (typing, a fresh prompt) resets the
                    // blink to visible, matching a real terminal instead of
                    // possibly landing mid-blink right when you start typing.
                    cursor.setVisible(true);
                    cursorBlink.playFromStart();
                }));

        setLeft(rail);
        setCenter(overlay);
        setMinHeight(120);
    }

    private void positionCursor(javafx.geometry.Bounds b) {
        double charWidth = b.getWidth() > 0 ? b.getWidth() : 8;
        cursor.setLayoutX(b.getMinX());
        switch (Settings.get(Settings.TERMINAL_CURSOR_SHAPE) == null
                ? "Block" : Settings.get(Settings.TERMINAL_CURSOR_SHAPE)) {
            case "Underline" -> {
                cursor.setLayoutY(b.getMaxY() - 2);
                cursor.setWidth(charWidth);
                cursor.setHeight(2);
            }
            case "Vertical Line" -> {
                cursor.setLayoutY(b.getMinY());
                cursor.setWidth(2);
                cursor.setHeight(b.getHeight());
            }
            default -> {   // Block
                cursor.setLayoutY(b.getMinY());
                cursor.setWidth(charWidth);
                cursor.setHeight(b.getHeight());
            }
        }
    }

    private void applyCursorShape() {
        // Bounds aren't known yet on construction; the real sizing happens
        // in positionCursor() once the caret first reports its bounds. This
        // just gives the cursor a sane default so it isn't a zero-size rect
        // before that first callback fires.
        cursor.setWidth(8);
        cursor.setHeight(15);
    }

    private static String fontSizeStyle() {
        String size = Settings.get(Settings.TERMINAL_FONT_SIZE);
        double px = 12;
        if (size != null) {
            try {
                px = Double.parseDouble(size);
            } catch (NumberFormatException ignored) {
            }
        }
        return "-fx-font-size: " + px + "px;";
    }

    /** (Re)start the shell in the given directory (overridden by the
     *  Settings &gt; Tools &gt; Terminal "Start directory" field, if set). */
    public void start(Path dir) {
        stop();
        String startDirOverride = Settings.get(Settings.TERMINAL_START_DIR);
        Path effectiveDir = startDirOverride != null && !startDirOverride.isBlank()
                ? Path.of(startDirOverride) : dir;
        this.workingDir = effectiveDir != null
                ? effectiveDir : Path.of(System.getProperty("user.home"));
        output.clear();
        writePos = 0;
        lineStart = 0;
        pendingAnsi = "";
        currentStyle = null;
        typedThisLine.setLength(0);
        typedCursorPos = 0;
        hideGhost();
        output.setStyle(fontSizeStyle());

        String[] cmd = shellCommand();
        Map<String, String> env = new java.util.HashMap<>(System.getenv());
        env.put("TERM", "xterm-256color");

        try {
            shell = PtyProcess.exec(cmd, env, workingDir.toString());
            usingPty = true;
        } catch (Throwable ptyFailure) {
            // Native pty layer didn't load on this machine (missing binary
            // for this OS/arch, permissions, etc.) \u2014 fall back to a plain
            // pipe rather than leaving the terminal dead.
            usingPty = false;
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd)
                        .directory(workingDir.toFile())
                        .redirectErrorStream(true);
                pb.environment().put("TERM", "xterm-256color");
                shell = pb.start();
                append("[real pty unavailable (" + ptyFailure.getClass().getSimpleName()
                        + "), falling back to a plain pipe \u2014 echo/history are still "
                        + "handled by the shell if it supports it]\n");
            } catch (IOException e) {
                append("Could not start shell: " + e.getMessage() + "\n");
                return;
            }
        }
        stdin = shell.getOutputStream();

        Process p = shell;
        Thread reader = new Thread(() -> {
            char[] buf = new char[4096];
            try (InputStreamReader in = new InputStreamReader(
                    p.getInputStream(), StandardCharsets.UTF_8)) {
                int n;
                while ((n = in.read(buf)) != -1) {
                    // A newer start() may have replaced `shell` while this
                    // old session's stream was still delivering its last
                    // buffered output; once that happens, this reader's
                    // data belongs to a dead session and must not leak
                    // into the new one's (freshly cleared) document.
                    if (p != shell) break;
                    append(p, new String(buf, 0, n));
                }
            } catch (IOException ignored) {
            }
            append(p, "\n[shell exited]\n");
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
        Platform.runLater(output::requestFocus);
    }

    /** Programmatically run a command in this terminal (e.g. jdb attach). */
    public void sendCommand(String command) {
        Platform.runLater(() -> sendRaw(command + "\r"));
    }

    // ---------------------------------------------------------------- input

    private void onKeyTyped(KeyEvent e) {
        String ch = e.getCharacter();
        if (ch.isEmpty()) return;
        char c = ch.charAt(0);
        if (c < 0x20 || c == 0x7F) return;   // control chars handled below
        typedThisLine.insert(typedCursorPos, ch);
        typedCursorPos += ch.length();
        updateGhost();
        sendRaw(ch);
        e.consume();
    }

    private void onKeyPressed(KeyEvent e) {
        KeyCode code = e.getCode();
        if (code == KeyCode.ENTER) {
            commitLine();
            sendRaw("\r");
            e.consume();
        } else if (code == KeyCode.RIGHT) {
            if (acceptedSuggestionAvailable()) {
                String rest = currentGhostRemainder();
                typedThisLine.append(rest);
                typedCursorPos = typedThisLine.length();
                hideGhost();
                sendRaw(rest);
            } else {
                if (typedCursorPos < typedThisLine.length()) typedCursorPos++;
                hideGhost();
                sendRaw("\u001B[C");
            }
            e.consume();
        } else if (code == KeyCode.TAB) {
            if (acceptedSuggestionAvailable()) {
                String rest = currentGhostRemainder();
                typedThisLine.append(rest);
                typedCursorPos = typedThisLine.length();
                hideGhost();
                sendRaw(rest);
            } else {
                sendRaw("\t");   // let the shell's own tab-completion run
            }
            e.consume();
        } else if (code == KeyCode.BACK_SPACE) {
            if (typedCursorPos > 0) {
                typedThisLine.deleteCharAt(typedCursorPos - 1);
                typedCursorPos--;
            }
            updateGhost();
            sendRaw("\u007F");
            e.consume();
        } else if (code == KeyCode.UP) {
            sendRaw("\u001B[A");
            e.consume();
        } else if (code == KeyCode.DOWN) {
            sendRaw("\u001B[B");
            e.consume();
        } else if (code == KeyCode.LEFT) {
            if (typedCursorPos > 0) typedCursorPos--;
            hideGhost();
            sendRaw("\u001B[D");
            e.consume();
        } else if (code == KeyCode.HOME) {
            typedCursorPos = 0;
            hideGhost();
            sendRaw("\u0001");   // readline: beginning-of-line
            e.consume();
        } else if (code == KeyCode.END) {
            typedCursorPos = typedThisLine.length();
            sendRaw("\u0005");   // readline: end-of-line
            updateGhost();
            e.consume();
        } else if ((code == KeyCode.C) && (e.isControlDown() || e.isShortcutDown())
                && output.getSelectedText().isEmpty()) {
            typedThisLine.setLength(0);
            typedCursorPos = 0;
            hideGhost();
            sendRaw("\u0003");   // real SIGINT, via the pty's line discipline
            e.consume();
        } else if (code == KeyCode.D && (e.isControlDown() || e.isShortcutDown())) {
            sendRaw("\u0004");
            e.consume();
        }
    }

    private void sendRaw(String s) {
        if (shell == null || !shell.isAlive()) {
            start(workingDir);
            if (shell == null) return;
        }
        try {
            stdin.write(s.getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        } catch (IOException ex) {
            append("[could not write to shell: " + ex.getMessage() + "]\n");
        }
    }

    private void commitLine() {
        String line = typedThisLine.toString();
        if (!line.isBlank() && (history.isEmpty()
                || !history.get(history.size() - 1).equals(line))) {
            history.add(line);
            if (history.size() > MAX_HISTORY) history.remove(0);
            saveHistoryAsync();
        }
        typedThisLine.setLength(0);
        typedCursorPos = 0;
        hideGhost();
    }

    // ----------------------------------------------------- ghost suggestion

    private void updateGhost() {
        // Only suggest when the cursor is at the end of the line \u2014 a
        // completion inserted mid-line wouldn't make sense, and we don't
        // truly track the shell's own cursor position, just this best
        // guess, so keep it to the one case that's unambiguous.
        String prefix = typedThisLine.toString();
        if (prefix.isBlank() || typedCursorPos != typedThisLine.length()) {
            hideGhost();
            return;
        }
        String match = null;
        for (int i = history.size() - 1; i >= 0; i--) {
            String h = history.get(i);
            if (h.startsWith(prefix) && h.length() > prefix.length()) {
                match = h;
                break;
            }
        }
        if (match == null) {
            hideGhost();
            return;
        }
        acceptedSuggestion = match;
        ghost.setText(match.substring(prefix.length()));
        ghost.setVisible(true);
    }

    private boolean acceptedSuggestionAvailable() {
        return ghost.isVisible() && acceptedSuggestion != null;
    }

    private String currentGhostRemainder() {
        return acceptedSuggestion != null
                ? acceptedSuggestion.substring(typedThisLine.length()) : "";
    }

    private void hideGhost() {
        ghost.setVisible(false);
        acceptedSuggestion = null;
    }

    private void loadHistory() {
        try {
            if (Files.exists(HISTORY_FILE)) {
                List<String> lines = Files.readAllLines(HISTORY_FILE, StandardCharsets.UTF_8);
                history.addAll(new LinkedHashSet<>(lines));
                while (history.size() > MAX_HISTORY) history.remove(0);
            }
        } catch (IOException ignored) {
        }
    }

    private void saveHistoryAsync() {
        List<String> snapshot = new ArrayList<>(history);
        Thread t = new Thread(() -> {
            try {
                Files.createDirectories(HISTORY_FILE.getParent());
                Files.write(HISTORY_FILE, snapshot, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        }, "lumina-terminal-history-save");
        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------------- helpers

    /** Start with a specific shell for this one session (from the "+"
     *  dropdown's shell picker), overriding the Settings default. */
    public void startWithShell(Path dir, String shellPath) {
        this.sessionShellOverride = shellPath;
        start(dir);
    }

    private String[] shellCommand() {
        String override = sessionShellOverride != null && !sessionShellOverride.isBlank()
                ? sessionShellOverride : Settings.get(Settings.TERMINAL_SHELL_PATH);
        if (override != null && !override.isBlank()) {
            return override.toLowerCase().contains("cmd.exe")
                    ? new String[]{override} : new String[]{override, "-i"};
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return new String[]{"cmd.exe"};
        String sh = System.getenv("SHELL");
        return new String[]{sh != null && !sh.isBlank() ? sh : "/bin/bash", "-i"};
    }

    // ------------------------------------------------------- ANSI decoding

    /**
     * Appends a chunk of raw shell output. Handles the escapes a normal
     * interactive shell actually relies on for line editing ({@code \r},
     * backspace, erase-to-end-of-line, SGR colors) by writing through the
     * position-aware {@link #writeChars} instead of blindly appending, so
     * readline redrawing the prompt after a backspace looks like an edit
     * rather than printing a second copy of the line.
     */
    private void append(String text) {
        append(shell, text);
    }

    private void append(Process owner, String text) {
        Platform.runLater(() -> {
            if (owner != shell) return;   // stale session, see start()'s reader thread
            String combined = pendingAnsi + text;
            pendingAnsi = "";
            StringBuilder plain = new StringBuilder();
            int i = 0;
            while (i < combined.length()) {
                char c = combined.charAt(i);
                if (c == '\r') {
                    flush(plain);
                    writePos = lineStart;
                    i++;
                } else if (c == '\n') {
                    flush(plain);
                    writePos = output.getLength();
                    output.appendText("\n");
                    writePos++;
                    lineStart = writePos;
                    i++;
                } else if (c == '\b') {
                    flush(plain);
                    if (writePos > lineStart) writePos--;
                    i++;
                } else if (c == '\u001B') {
                    flush(plain);
                    String rest = combined.substring(i);
                    Matcher csi = CSI.matcher(rest);
                    Matcher osc = OSC.matcher(rest);
                    if (csi.lookingAt()) {
                        String seq = csi.group();
                        if (seq.endsWith("m")) {
                            currentStyle = mapSgr(seq);
                        } else if (seq.endsWith("K")) {
                            eraseToEndOfLine();
                        }
                        i += seq.length();
                    } else if (osc.lookingAt()) {
                        i += osc.group().length();
                    } else if (rest.length() < 24) {
                        pendingAnsi = rest;   // sequence split across reads
                        break;
                    } else {
                        i++;   // unrecognized escape, drop just the ESC
                    }
                } else {
                    plain.append(c);
                    i++;
                }
            }
            flush(plain);
            output.moveTo(output.getLength());
            output.requestFollowCaret();
        });
    }

    private void flush(StringBuilder plain) {
        if (!plain.isEmpty()) {
            writeChars(plain.toString(), currentStyle);
            plain.setLength(0);
        }
    }

    /** Writes at {@link #writePos}, overwriting in place if a \r/backspace
     *  moved it before the end of the document (matches how a real
     *  terminal redraws a line), or appending normally otherwise. */
    private void writeChars(String s, String style) {
        if (s.isEmpty()) return;
        int docLen = output.getLength();
        String styleClass = style == null ? "" : style;
        if (writePos >= docLen) {
            output.insertText(docLen, s);
            output.setStyleClass(docLen, docLen + s.length(), styleClass);
            writePos = docLen + s.length();
            return;
        }
        int overwriteEnd = Math.min(docLen, writePos + s.length());
        int overwriteLen = overwriteEnd - writePos;
        output.replaceText(writePos, overwriteEnd, s.substring(0, overwriteLen));
        output.setStyleClass(writePos, writePos + overwriteLen, styleClass);
        writePos += overwriteLen;
        if (overwriteLen < s.length()) {
            String rest = s.substring(overwriteLen);
            output.insertText(writePos, rest);
            output.setStyleClass(writePos, writePos + rest.length(), styleClass);
            writePos += rest.length();
        }
    }

    private void eraseToEndOfLine() {
        String text = output.getText();
        int nextNewline = text.indexOf('\n', writePos);
        int end = nextNewline == -1 ? text.length() : nextNewline;
        if (end > writePos) output.deleteText(writePos, end);
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