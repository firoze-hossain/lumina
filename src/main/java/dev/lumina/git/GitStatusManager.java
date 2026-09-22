package dev.lumina.git;

import javafx.application.Platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central singleton managing file Git statuses across the IDE.
 * Caches statuses, executes Git porcelain queries, and notifies UI components (FileExplorer, EditorTab, CommitPanel).
 */
public class GitStatusManager {

    private static final GitStatusManager INSTANCE = new GitStatusManager();

    private final Map<Path, GitFileStatus> statusCache = new ConcurrentHashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private GitStatusManager() {}

    public static GitStatusManager getInstance() {
        return INSTANCE;
    }

    /**
     * Traverses up to locate the Git repository root (containing .git folder).
     */
    public static Path findRepositoryRoot(Path path) {
        if (path == null) return null;
        try {
            Path current = Files.isDirectory(path) ? path.toAbsolutePath().normalize()
                    : path.toAbsolutePath().normalize().getParent();
            while (current != null) {
                if (Files.isDirectory(current.resolve(".git"))) {
                    return current;
                }
                current = current.getParent();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static boolean isInsideGitRepository(Path path) {
        return findRepositoryRoot(path) != null;
    }

    /**
     * Returns the Git status of the given file. Defaults to GitFileStatus.NORMAL.
     */
    public GitFileStatus getStatus(Path file) {
        if (file == null) return GitFileStatus.NORMAL;
        Path norm = file.toAbsolutePath().normalize();
        GitFileStatus status = statusCache.get(norm);
        return status != null ? status : GitFileStatus.NORMAL;
    }

    /**
     * Explicitly sets a file's status (e.g. immediately upon file creation or git add) and notifies listeners.
     */
    public void setStatus(Path file, GitFileStatus status) {
        if (file == null || status == null) return;
        Path norm = file.toAbsolutePath().normalize();
        statusCache.put(norm, status);
        notifyListeners();
    }

    /**
     * Removes a file from status tracking.
     */
    public void removeStatus(Path file) {
        if (file == null) return;
        Path norm = file.toAbsolutePath().normalize();
        if (statusCache.remove(norm) != null) {
            notifyListeners();
        }
    }

    public void clearCache() {
        statusCache.clear();
        notifyListeners();
    }

    /**
     * Refreshes file statuses from git status output for the given repository root.
     */
    public void refresh(Path repoRoot) {
        if (repoRoot == null || !GitService.isRepository(repoRoot)) return;
        Path root = repoRoot.toAbsolutePath().normalize();

        GitService.Result r = GitService.statusDetailed(root, true);
        if (!r.ok()) return;

        Map<Path, GitFileStatus> newStatuses = new HashMap<>();

        for (String line : r.output().split("\\R")) {
            if (line.isBlank() || line.startsWith("##")) continue;
            if (line.length() < 3) continue;

            String prefix = line.substring(0, 2);
            String relPath = line.substring(3).trim();
            if (relPath.startsWith("\"") && relPath.endsWith("\"") && relPath.length() >= 2) {
                relPath = relPath.substring(1, relPath.length() - 1);
            }
            if (relPath.contains(" -> ")) {
                relPath = relPath.substring(relPath.indexOf(" -> ") + 4).trim();
            }

            Path filePath = root.resolve(relPath).normalize();
            char x = prefix.charAt(0);
            char y = prefix.charAt(1);

            if (x == '?' && y == '?') {
                newStatuses.put(filePath, GitFileStatus.UNTRACKED);
            } else if (x == '!' && y == '!') {
                newStatuses.put(filePath, GitFileStatus.IGNORED);
            } else if (x == 'A' || y == 'A') {
                newStatuses.put(filePath, GitFileStatus.ADDED);
            } else if (x == 'D' || y == 'D') {
                newStatuses.put(filePath, GitFileStatus.DELETED);
            } else {
                newStatuses.put(filePath, GitFileStatus.MODIFIED);
            }
        }

        // Retain only statuses for this repo
        statusCache.keySet().removeIf(p -> p.startsWith(root));
        statusCache.putAll(newStatuses);

        notifyListeners();
    }

    public synchronized void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        Runnable task = () -> {
            for (Runnable l : new ArrayList<>(listeners)) {
                try {
                    l.run();
                } catch (Exception ignored) {}
            }
        };
        try {
            if (Platform.isFxApplicationThread()) {
                task.run();
            } else {
                Platform.runLater(task);
            }
        } catch (Exception e) {
            task.run();
        }
    }
}
