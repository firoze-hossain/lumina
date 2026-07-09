package dev.lumina.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Output console. Runs a single .java file with the JDK's
 * source-file launcher ("java File.java") and streams its output.
 */
public class ConsolePane extends BorderPane {

    private final TextArea output = new TextArea();
    private final Label title = new Label("CONSOLE");
    private volatile Process process;
    private Thread readerThread;

    public ConsolePane() {
        getStyleClass().add("console-pane");

        title.getStyleClass().add("panel-header");

        Button stop = new Button("Stop");
        stop.getStyleClass().add("console-button");
        stop.setOnAction(e -> stopProcess());

        Button clear = new Button("Clear");
        clear.getStyleClass().add("console-button");
        clear.setOnAction(e -> clear());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(8, title, spacer, stop, clear);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 12, 6, 12));
        header.getStyleClass().add("console-header");

        output.setEditable(false);
        output.setWrapText(true);
        output.getStyleClass().add("console-output");
        output.setPromptText("Run a file to see its output here  (\u2318R / Ctrl+R)");

        setTop(header);
        setCenter(output);
        setMinHeight(120);
    }

    /** Compile & run a single Java source file using the source launcher. */
    public void runJavaFile(Path file) {
        stopProcess();
        clear();

        String javaHome = System.getProperty("java.home");
        String javaBin = Path.of(javaHome, "bin", "java").toString();

        appendLine("\u25B6 Running " + file.getFileName() + " \u2026\n");
        title.setText("CONSOLE \u2014 running");

        ProcessBuilder pb = new ProcessBuilder(javaBin, file.toAbsolutePath().toString());
        pb.directory(file.getParent() != null ? file.getParent().toFile() : null);
        pb.redirectErrorStream(true);

        try {
            process = pb.start();
        } catch (IOException ex) {
            appendLine("Could not start java: " + ex.getMessage());
            title.setText("CONSOLE");
            return;
        }

        Process p = process;
        readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLine(line);
                }
                int code = p.waitFor();
                appendLine("\nProcess finished with exit code " + code);
            } catch (IOException | InterruptedException ignored) {
                // Stopped by the user or app shutdown.
            } finally {
                Platform.runLater(() -> title.setText("CONSOLE"));
            }
        }, "lumina-console-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void stopProcess() {
        Process p = process;
        if (p != null && p.isAlive()) {
            p.destroy();
            appendLine("\n\u25A0 Process stopped");
        }
        process = null;
    }

    public void shutdown() {
        Process p = process;
        if (p != null && p.isAlive()) p.destroyForcibly();
    }

    public void clear() {
        output.clear();
    }

    /** Public logging hook, e.g. for the project generator. */
    public void println(String line) {
        appendLine(line);
    }

    private void appendLine(String line) {
        Platform.runLater(() -> output.appendText(line + "\n"));
    }
}
