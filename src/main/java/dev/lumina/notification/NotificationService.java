package dev.lumina.notification;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Dynamic Notification Service matching IntelliJ IDEA's notification architecture.
 * Manages notification groups, persistent user preferences, timeline log,
 * and delivery of balloon popups.
 */
public final class NotificationService {

    private static final NotificationService INSTANCE = new NotificationService();

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(NotificationService.class);
    private static final String PREF_DISPLAY_BALLOONS = "lumina.notifications.display_balloons";
    private static final String PREF_ENABLE_SYSTEM = "lumina.notifications.enable_system";
    private static final String PREF_GROUPS_JSON = "lumina.notifications.groups_json";
    private static final String PREF_DONT_ASK_JSON = "lumina.notifications.dont_ask_json";
    private static final Gson GSON = new Gson();

    private boolean displayBalloonNotifications = true;
    private boolean enableSystemNotifications = true;

    private final Map<String, NotificationGroup> groups = new LinkedHashMap<>();
    private final ObservableList<NotificationGroup> observableGroups = FXCollections.observableArrayList();
    private final ObservableList<Notification> timeline = FXCollections.observableArrayList();
    private final ObservableList<String> dontAskAgainList = FXCollections.observableArrayList();

    private final List<Consumer<Notification>> toastListeners = new CopyOnWriteArrayList<>();

    private NotificationService() {
        registerDefaultGroups();
        loadSettings();
        seedInitialTimelineIfEmpty();
    }

    private void registerDefaultGroups() {
        // Standard IntelliJ IDEA notification groups from screenshot
        String[] defaultGroupTitles = {
                "AI Assistant installer",
                "Angular CLI",
                "Automatic indent detection disabled",
                "Backup and Sync messages",
                "Batch quick fix",
                "Black",
                "Branch context switched",
                "Breakpoint hit",
                "Browser configuration problems",
                "BSP",
                "Build failed to start",
                "Build finished",
                "Build script found",
                "Built-in HTTP server could not start",
                "Bundler",
                "Bundler integration",
                "CMake",
                "Code Inspection",
                "Compiler",
                "Database",
                "Debugger",
                "Deployment",
                "File Watcher",
                "Git",
                "Gradle",
                "Indexing",
                "Java compiler",
                "Maven",
                "Plugins",
                "Project structure",
                "Terminal",
                "VCS hosting integrations",
                "VCS important messages",
                "VCS messages",
                "VCS notifications",
                "VCS silent notifications",
                "Vite configuration analysis failed",
                "Vitest execution failed",
                "Vue",
                "Webpack configuration analysis failed"
        };

        for (String title : defaultGroupTitles) {
            String id = title.toLowerCase().replaceAll("[^a-z0-9]+", ".");
            NotificationGroup group = new NotificationGroup(
                    id, title, NotificationDisplayType.BALLOON, true, false, false
            );
            groups.put(id, group);
        }
        syncObservableGroups();
    }

    private void syncObservableGroups() {
        List<NotificationGroup> sorted = new ArrayList<>(groups.values());
        sorted.sort(Comparator.comparing(NotificationGroup::getTitle, String.CASE_INSENSITIVE_ORDER));
        observableGroups.setAll(sorted);
    }

    public synchronized void registerGroup(NotificationGroup group) {
        if (group == null) return;
        groups.put(group.getId(), group);
        syncObservableGroups();
        saveSettings();
    }

    public NotificationGroup getGroup(String idOrTitle) {
        if (idOrTitle == null) return null;
        NotificationGroup g = groups.get(idOrTitle);
        if (g != null) return g;
        // Search by title or normalized ID
        String norm = idOrTitle.toLowerCase().replaceAll("[^a-z0-9]+", ".");
        g = groups.get(norm);
        if (g != null) return g;

        for (NotificationGroup candidate : groups.values()) {
            if (candidate.getTitle().equalsIgnoreCase(idOrTitle)) {
                return candidate;
            }
        }
        return null;
    }

    public NotificationGroup getOrCreateGroup(String title) {
        NotificationGroup g = getGroup(title);
        if (g != null) return g;
        String id = title.toLowerCase().replaceAll("[^a-z0-9]+", ".");
        NotificationGroup newGroup = new NotificationGroup(id, title);
        registerGroup(newGroup);
        return newGroup;
    }

    public ObservableList<NotificationGroup> getObservableGroups() {
        return observableGroups;
    }

    public ObservableList<Notification> getTimeline() {
        return timeline;
    }

    public ObservableList<String> getDontAskAgainList() {
        return dontAskAgainList;
    }

    public boolean isDisplayBalloonNotifications() {
        return displayBalloonNotifications;
    }

    public void setDisplayBalloonNotifications(boolean displayBalloonNotifications) {
        this.displayBalloonNotifications = displayBalloonNotifications;
        saveSettings();
    }

    public boolean isEnableSystemNotifications() {
        return enableSystemNotifications;
    }

    public void setEnableSystemNotifications(boolean enableSystemNotifications) {
        this.enableSystemNotifications = enableSystemNotifications;
        saveSettings();
    }

    public void addDontAskAgain(String item) {
        if (item != null && !item.isBlank() && !dontAskAgainList.contains(item)) {
            dontAskAgainList.add(item);
            saveSettings();
        }
    }

    public void removeDontAskAgain(String item) {
        if (dontAskAgainList.remove(item)) {
            saveSettings();
        }
    }

    public void clearTimeline() {
        if (Platform.isFxApplicationThread()) {
            timeline.clear();
        } else {
            try {
                Platform.runLater(timeline::clear);
            } catch (IllegalStateException e) {
                timeline.clear();
            }
        }
    }

    public void removeNotification(Notification notification) {
        if (Platform.isFxApplicationThread()) {
            timeline.remove(notification);
        } else {
            try {
                Platform.runLater(() -> timeline.remove(notification));
            } catch (IllegalStateException e) {
                timeline.remove(notification);
            }
        }
    }

    /**
     * Registers a listener to receive balloon toast notifications.
     */
    public void addToastListener(Consumer<Notification> listener) {
        if (listener != null && !toastListeners.contains(listener)) {
            toastListeners.add(listener);
        }
    }

    public void removeToastListener(Consumer<Notification> listener) {
        toastListeners.remove(listener);
    }

    /**
     * Pushes a notification dynamically, respecting group settings, balloon display,
     * tool window logging, and sound.
     */
    public void notify(Notification notification) {
        if (notification == null) return;

        NotificationGroup group = getOrCreateGroup(notification.getGroupId());

        // 1. Tool window timeline logging
        if (group.isShowInToolWindow()) {
            Runnable addTimeline = () -> {
                timeline.addFirst(notification);
                // Keep timeline bounded
                if (timeline.size() > 500) {
                    timeline.removeLast();
                }
            };
            if (Platform.isFxApplicationThread()) {
                addTimeline.run();
            } else {
                try {
                    Platform.runLater(addTimeline);
                } catch (IllegalStateException e) {
                    addTimeline.run();
                }
            }
        }

        // 2. Play sound if configured
        if (group.isPlaySound()) {
            try {
                java.awt.Toolkit.getDefaultToolkit().beep();
            } catch (Exception ignored) {}
        }

        // 3. Balloon Popup Toast
        if (displayBalloonNotifications && group.getDisplayType() != NotificationDisplayType.NONE) {
            Runnable fireToast = () -> {
                for (Consumer<Notification> listener : toastListeners) {
                    listener.accept(notification);
                }
            };
            if (Platform.isFxApplicationThread()) {
                fireToast.run();
            } else {
                try {
                    Platform.runLater(fireToast);
                } catch (IllegalStateException e) {
                    fireToast.run();
                }
            }
        }
    }

    public void notify(String groupTitle, String title, String content, NotificationType type, NotificationAction... actions) {
        Notification n = new Notification(groupTitle, groupTitle, title, content, type, Instant.now());
        if (actions != null) {
            for (NotificationAction action : actions) {
                n.addAction(action);
            }
        }
        notify(n);
    }

    private void seedInitialTimelineIfEmpty() {
        if (!timeline.isEmpty()) return;

        // Seed with items directly matching IntelliJ reference screenshot media_1789961265558.png
        Notification n1 = new Notification(
                "vcs.messages", "VCS messages",
                "Pushed 1 commit to origin/master",
                "",
                NotificationType.INFORMATION,
                Instant.now()
        );

        Notification n2 = new Notification(
                "vcs.messages", "VCS messages",
                "8 files committed",
                "data editor and viewer done",
                NotificationType.INFORMATION,
                Instant.now()
        );

        Notification n3 = new Notification(
                "vcs.important.messages", "VCS important messages",
                "Commit and push checks failed",
                "70 errors and 1,113 warnings",
                NotificationType.ERROR,
                Instant.now()
        ).addAction("Commit anyway and push", () -> {})
         .addAction("Review code analysis", () -> {});

        Notification n4 = new Notification(
                "build.finished", "Build finished",
                "Build completed successfully with 2 warnings in 4 sec, 82 ms",
                "",
                NotificationType.WARNING,
                Instant.now().minusSeconds(120)
        );

        Notification n5 = new Notification(
                "vcs.messages", "VCS messages",
                "All files are up to date",
                "",
                NotificationType.INFORMATION,
                Instant.now().minusSeconds(1020)
        );

        Notification n6 = new Notification(
                "build.finished", "Build finished",
                "Build completed successfully with 2 warnings in 4 sec, 205 ms",
                "",
                NotificationType.WARNING,
                Instant.now().minusSeconds(1800)
        );

        Notification n7 = new Notification(
                "vcs.messages", "VCS messages",
                "13 files updated in 2 commits",
                "",
                NotificationType.INFORMATION,
                Instant.now().minusSeconds(1800)
        ).addAction("View commits", () -> {});

        timeline.addAll(n1, n2, n3, n4, n5, n6, n7);
    }

    public synchronized void saveSettings() {
        try {
            PREFS.putBoolean(PREF_DISPLAY_BALLOONS, displayBalloonNotifications);
            PREFS.putBoolean(PREF_ENABLE_SYSTEM, enableSystemNotifications);

            // Save groups
            List<StoredGroup> stored = new ArrayList<>();
            for (NotificationGroup g : groups.values()) {
                StoredGroup sg = new StoredGroup();
                sg.id = g.getId();
                sg.title = g.getTitle();
                sg.displayType = g.getDisplayType().name();
                sg.showInToolWindow = g.isShowInToolWindow();
                sg.playSound = g.isPlaySound();
                sg.readAloud = g.isReadAloud();
                stored.add(sg);
            }
            PREFS.put(PREF_GROUPS_JSON, GSON.toJson(stored));

            // Save Don't Ask Again
            PREFS.put(PREF_DONT_ASK_JSON, GSON.toJson(new ArrayList<>(dontAskAgainList)));
        } catch (Exception ignored) {}
    }

    public synchronized void loadSettings() {
        try {
            displayBalloonNotifications = PREFS.getBoolean(PREF_DISPLAY_BALLOONS, true);
            enableSystemNotifications = PREFS.getBoolean(PREF_ENABLE_SYSTEM, true);

            String groupsJson = PREFS.get(PREF_GROUPS_JSON, "");
            if (!groupsJson.isBlank()) {
                Type type = new TypeToken<List<StoredGroup>>() {}.getType();
                List<StoredGroup> list = GSON.fromJson(groupsJson, type);
                if (list != null) {
                    for (StoredGroup sg : list) {
                        NotificationDisplayType dt = NotificationDisplayType.BALLOON;
                        try {
                            dt = NotificationDisplayType.valueOf(sg.displayType);
                        } catch (Exception ignored) {}

                        NotificationGroup g = new NotificationGroup(
                                sg.id, sg.title, dt, sg.showInToolWindow, sg.playSound, sg.readAloud
                        );
                        groups.put(sg.id, g);
                    }
                    syncObservableGroups();
                }
            }

            String dontAskJson = PREFS.get(PREF_DONT_ASK_JSON, "");
            if (!dontAskJson.isBlank()) {
                Type type = new TypeToken<List<String>>() {}.getType();
                List<String> list = GSON.fromJson(dontAskJson, type);
                if (list != null) {
                    dontAskAgainList.setAll(list);
                }
            }
        } catch (Exception ignored) {}
    }

    private static class StoredGroup {
        String id;
        String title;
        String displayType;
        boolean showInToolWindow;
        boolean playSound;
        boolean readAloud;
    }
}
