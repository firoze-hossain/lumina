package dev.lumina.util;

import dev.lumina.LuminaApp;
import dev.lumina.plugin.PluginItem;
import dev.lumina.plugin.PluginManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Handles IDE restarts when plugins are installed, updated,
 * or when the user invokes File -> Restart IDE.
 */
public final class IdeRestartHelper {

    private IdeRestartHelper() {}

    /**
     * Prompts the user with a confirmation dialog before restarting.
     *
     * @param owner       owner window or stage (can be null)
     * @param reason      custom reason or message if pluginNames is null
     * @param pluginNames list of plugin names requiring restart
     * @return true if restart was initiated, false if user chose "Not Now"
     */
    public static boolean promptAndRestart(Window owner, String reason, List<String> pluginNames) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Restart Lumina");
        alert.setHeaderText("Restart Lumina to activate plugin changes?");

        StringBuilder sb = new StringBuilder();
        if (pluginNames != null && !pluginNames.isEmpty()) {
            sb.append("Lumina must be restarted to activate the following plugin(s):\n");
            for (String name : pluginNames) {
                sb.append("  • ").append(name).append("\n");
            }
            sb.append("\nWould you like to restart Lumina now?");
        } else if (reason != null && !reason.isBlank()) {
            sb.append(reason).append("\n\nWould you like to restart Lumina now?");
        } else {
            sb.append("Lumina will be restarted to apply changes. Do you want to proceed?");
        }

        alert.setContentText(sb.toString());
        if (owner != null) {
            alert.initOwner(owner);
        }

        ButtonType restartBtn = new ButtonType("Restart", ButtonBar.ButtonData.OK_DONE);
        ButtonType notNowBtn = new ButtonType("Not Now", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(restartBtn, notNowBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == restartBtn) {
            restart();
            return true;
        }
        return false;
    }

    /**
     * Saves all current IDE state, launches a fresh Lumina process, and cleanly terminates
     * the current process.
     */
    public static void restart() {
        // 1. Save all open documents, active sessions, and plugin state
        try {
            for (LuminaApp app : new ArrayList<>(LuminaApp.ACTIVE_INSTANCES)) {
                if (app != null) {
                    app.saveAllEditors();
                }
            }
            PluginManager.getInstance().savePersistedState();
        } catch (Throwable ignored) {}

        // 2. Spawn fresh Lumina process
        boolean spawned = false;
        try {
            List<String> command = buildRestartCommand();
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.inheritIO();
            pb.start();
            spawned = true;
        } catch (Exception ignored) {}

        if (spawned) {
            // Cleanly close windows and exit JVM
            Platform.runLater(() -> {
                try {
                    for (LuminaApp app : new ArrayList<>(LuminaApp.ACTIVE_INSTANCES)) {
                        if (app != null) app.closeWindow();
                    }
                } catch (Throwable ignored) {}
                Platform.exit();
                System.exit(0);
            });
        } else {
            // In-process fallback: close stages and start fresh instance
            Platform.runLater(() -> {
                try {
                    for (LuminaApp app : new ArrayList<>(LuminaApp.ACTIVE_INSTANCES)) {
                        if (app != null) app.closeWindow();
                    }
                    new LuminaApp().start(new Stage());
                } catch (Throwable t) {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Failed to restart Lumina: " + t.getMessage());
                    err.showAndWait();
                }
            });
        }
    }

    /**
     * Builds the exact command line to relaunch Lumina with current JVM classpath and arguments.
     */
    public static List<String> buildRestartCommand() {
        List<String> cmd = new ArrayList<>();
        String javaHome = System.getProperty("java.home");
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");

        File javaw = new File(javaHome, "bin" + File.separator + (isWindows ? "javaw.exe" : "java"));
        File java = new File(javaHome, "bin" + File.separator + (isWindows ? "java.exe" : "java"));
        String javaBin = javaw.exists() ? javaw.getAbsolutePath() : java.getAbsolutePath();
        cmd.add(javaBin);

        // VM arguments (preserve JVM options such as memory, module path, etc.)
        List<String> vmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : vmArgs) {
            // Skip remote debug ports to prevent collision
            if (arg.startsWith("-agentlib:jdwp") || arg.startsWith("-Xrunjdwp")) {
                continue;
            }
            cmd.add(arg);
        }

        String classpath = System.getProperty("java.class.path");
        String sunCommand = System.getProperty("sun.java.command");

        if (sunCommand != null && (sunCommand.endsWith(".jar") || sunCommand.contains(".jar "))) {
            cmd.add("-jar");
            String[] parts = sunCommand.split("\\s+");
            cmd.addAll(Arrays.asList(parts));
        } else {
            if (classpath != null && !classpath.isBlank()) {
                cmd.add("-cp");
                cmd.add(classpath);
            }
            if (sunCommand != null && !sunCommand.isBlank()) {
                String[] parts = sunCommand.split("\\s+");
                cmd.addAll(Arrays.asList(parts));
            } else {
                cmd.add("dev.lumina.LuminaApp");
            }
        }
        return cmd;
    }
}
