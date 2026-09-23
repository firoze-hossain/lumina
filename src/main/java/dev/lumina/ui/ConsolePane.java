package dev.lumina.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Output console for Run and Build tool windows. Executes single commands or sequences
 * (e.g. compile then run) on a background thread, streaming combined stdout/stderr.
 */
public class ConsolePane extends BorderPane {

    private final TextArea output = new TextArea();
    private final ToolWindowHeader toolWindowHeader;
    private final Button outputTabButton;
    private final String toolWindowTitle;
    private volatile Process process;
    private volatile boolean cancelled;
    private java.util.function.Consumer<Boolean> runningListener;

    // last run, so the IDE can offer a Rerun button
    private String lastHeader;
    private List<List<String>> lastCommands;
    private Path lastWorkDir;
    private java.util.Map<String, String> lastEnv;
    private Runnable lastOnSuccess;
    private Runnable lastOnDone;

    /** Notified with true when a process starts, false when it ends. */
    public void setOnRunningChanged(java.util.function.Consumer<Boolean> listener) {
        this.runningListener = listener;
    }

    /** Re-run the previous command sequence. Returns false if none yet. */
    public boolean restartLast() {
        if (lastCommands == null) return false;
        runSequence(lastHeader, lastCommands, lastWorkDir, lastEnv,
                lastOnSuccess, lastOnDone);
        return true;
    }

    /** Run one command and ALWAYS call onDone afterwards (even on failure). */
    public void runCommandThen(String header, List<String> command, Path workDir,
                               Runnable onDone) {
        runSequence(header, List.of(command), workDir, null, null, onDone);
    }

    public ConsolePane() {
        this("Run");
    }

    public ConsolePane(String windowTitle) {
        this.toolWindowTitle = windowTitle != null ? windowTitle : "Run";
        getStyleClass().add("console-pane");
        setStyle("-fx-background-color: #1E1F22;");

        toolWindowHeader = new ToolWindowHeader(this.toolWindowTitle);
        String defaultTab = "Build".equalsIgnoreCase(this.toolWindowTitle) ? "Build Output" : "Output";
        outputTabButton = toolWindowHeader.addTab(defaultTab, false, null, null);

        if ("Run".equalsIgnoreCase(this.toolWindowTitle)) {
            toolWindowHeader.addRightActionButton("Stop", "Stop Process", this::stopProcess);
            toolWindowHeader.addRightActionButton("Clear", "Clear Console", this::clear);
        } else {
            toolWindowHeader.addRightActionButton("Clear", "Clear Output", this::clear);
        }

        output.setEditable(false);
        output.setWrapText(true);
        output.getStyleClass().add("console-output");
        output.setStyle("-fx-control-inner-background: #1E1F22; -fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-font-family: monospace;");
        output.setPromptText("Build".equalsIgnoreCase(this.toolWindowTitle)
                ? "Build a project to see compilation output here"
                : "Run a file or project to see output here  (\u2318R / Ctrl+R)");

        setTop(toolWindowHeader);
        setCenter(output);
        setMinHeight(120);
    }

    public void setOnHideToolWindow(Runnable onHide) {
        toolWindowHeader.setOnHide(onHide);
    }

    public void setOnMaximizeToolWindow(Runnable onMaximize) {
        toolWindowHeader.setOnMaximize(onMaximize);
    }

    public ToolWindowHeader getToolWindowHeader() {
        return toolWindowHeader;
    }

    /** Compile & run a single Java source file using the source launcher. */
    public void runJavaFile(Path file) {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        runSequence("Running " + file.getFileName(),
                List.of(List.of(javaBin, file.toAbsolutePath().toString())),
                file.getParent());
    }

    public void runCommand(String header, List<String> command, Path workDir) {
        runSequence(header, List.of(command), workDir);
    }

    /** Run commands one after another; stop at the first non-zero exit. */
    public void runSequence(String header, List<List<String>> commands, Path workDir) {
        runSequence(header, commands, workDir, null);
    }

    /** Like runSequence, with a callback on the FX thread after a clean exit. */
    public void runSequence(String header, List<List<String>> commands, Path workDir,
                            Runnable onSuccess) {
        runSequence(header, commands, workDir, null, onSuccess);
    }

    /** Full form: optional extra environment variables (e.g. GIT_ASKPASS). */
    public void runSequence(String header, List<List<String>> commands, Path workDir,
                            java.util.Map<String, String> env, Runnable onSuccess) {
        runSequence(header, commands, workDir, env, onSuccess, null);
    }

    /** Full form: onSuccess fires only on exit 0; onDone fires regardless. */
    public void runSequence(String header, List<List<String>> commands, Path workDir,
                            java.util.Map<String, String> env, Runnable onSuccess,
                            Runnable onDone) {
        stopProcess();
        this.lastHeader = header;
        this.lastCommands = commands;
        this.lastWorkDir = workDir;
        this.lastEnv = env;
        this.lastOnSuccess = onSuccess;
        this.lastOnDone = onDone;
        clear();
        cancelled = false;
        println("\u25B6 " + header + "\n");
        setBusy(true);

        Thread worker = new Thread(() -> {
            int code = 0;
            try {
                for (List<String> command : commands) {
                    if (cancelled) break;
                    println("\u276F " + String.join(" ", command));
                    ProcessBuilder pb = new ProcessBuilder(command)
                            .redirectErrorStream(true);
                    if (workDir != null) pb.directory(workDir.toFile());
                    if (env != null) pb.environment().putAll(env);
                    Process p = pb.start();
                    process = p;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            p.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            println(line);
                        }
                    }
                    code = p.waitFor();
                    if (code != 0) break;
                }
                if (!cancelled) {
                    println("\nProcess finished with exit code " + code);
                    if (code == 0 && onSuccess != null) {
                        Platform.runLater(onSuccess);
                    }
                }
            } catch (IOException ex) {
                println("\u2717 " + ex.getMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                process = null;
                setBusy(false);
                if (onDone != null) Platform.runLater(onDone);
            }
        }, "lumina-run");
        worker.setDaemon(true);
        worker.start();
    }

    public void stopProcess() {
        cancelled = true;
        Process p = process;
        if (p != null && p.isAlive()) {
            p.destroy();
            println("\n\u25A0 Process stopped");
        }
        process = null;
    }

    public void shutdown() {
        Process p = process;
        if (p != null && p.isAlive()) p.destroyForcibly();
    }

    public void clear() {
        Platform.runLater(output::clear);
    }

    /** Public logging hook, e.g. for the project generator or git. */
    public void println(String line) {
        Platform.runLater(() -> output.appendText(line + "\n"));
    }

    private void setBusy(boolean busy) {
        Platform.runLater(() -> {
            if (runningListener != null) runningListener.accept(busy);
        });
    }
}