package dev.lumina.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in terminal backed by the system shell (bash/zsh on Unix, cmd on
 * Windows). Streams are piped, so line-based commands work (git, mvn, ls…);
 * full-screen TUI apps (vim, htop) need a real PTY and are out of scope.
 */
public class TerminalPane extends BorderPane {

    private final TextArea output = new TextArea();
    private final TextField input = new TextField();
    private final Label cwdLabel = new Label("");
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    private Process shell;
    private BufferedWriter stdin;
    private Path workingDir = Path.of(System.getProperty("user.home"));

    public TerminalPane() {
        getStyleClass().add("terminal-pane");

        Label title = new Label("TERMINAL");
        title.getStyleClass().add("panel-header");

        cwdLabel.getStyleClass().add("terminal-cwd");

        Button restart = new Button("Restart");
        restart.getStyleClass().add("console-button");
        restart.setOnAction(e -> start(workingDir));

        Button clear = new Button("Clear");
        clear.getStyleClass().add("console-button");
        clear.setOnAction(e -> output.clear());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, title, cwdLabel, spacer, restart, clear);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 12, 6, 12));
        header.getStyleClass().add("console-header");

        output.setEditable(false);
        output.setWrapText(true);
        output.getStyleClass().add("terminal-output");

        Label prompt = new Label("\u276F");
        prompt.getStyleClass().add("terminal-prompt");

        input.getStyleClass().add("terminal-input");
        input.setPromptText("type a command and press Enter");
        input.setOnAction(e -> submit());
        input.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case UP -> navigateHistory(-1);
                case DOWN -> navigateHistory(1);
                default -> { }
            }
        });
        HBox.setHgrow(input, Priority.ALWAYS);

        HBox inputRow = new HBox(8, prompt, input);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.setPadding(new Insets(6, 12, 8, 12));
        inputRow.getStyleClass().add("terminal-input-row");

        setTop(header);
        setCenter(output);
        setBottom(inputRow);
        setMinHeight(120);
    }

    /** (Re)start the shell in the given directory. */
    public void start(Path dir) {
        stop();
        this.workingDir = dir != null ? dir : Path.of(System.getProperty("user.home"));
        cwdLabel.setText(abbreviate(workingDir));
        output.clear();

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

        append("Started " + String.join(" ", cmd) + " in " + abbreviate(workingDir) + "\n");

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
        append("\u276F " + line + "\n");
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

    private void append(String text) {
        Platform.runLater(() -> output.appendText(text));
    }
}
