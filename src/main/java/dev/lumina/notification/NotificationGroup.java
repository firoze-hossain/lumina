package dev.lumina.notification;

import java.util.Objects;

/**
 * A notification group matching IntelliJ IDEA's NotificationGroup concept.
 * Each group configures popup behavior, tool window logging, sound, and read-aloud.
 */
public class NotificationGroup {
    private final String id;
    private String title;
    private NotificationDisplayType displayType;
    private boolean showInToolWindow;
    private boolean playSound;
    private boolean readAloud;

    public NotificationGroup(String id, String title) {
        this(id, title, NotificationDisplayType.BALLOON, true, false, false);
    }

    public NotificationGroup(String id, String title, NotificationDisplayType displayType,
                             boolean showInToolWindow, boolean playSound, boolean readAloud) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.title = title != null ? title : id;
        this.displayType = displayType != null ? displayType : NotificationDisplayType.BALLOON;
        this.showInToolWindow = showInToolWindow;
        this.playSound = playSound;
        this.readAloud = readAloud;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public NotificationDisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(NotificationDisplayType displayType) {
        this.displayType = displayType;
    }

    public boolean isShowInToolWindow() {
        return showInToolWindow;
    }

    public void setShowInToolWindow(boolean showInToolWindow) {
        this.showInToolWindow = showInToolWindow;
    }

    public boolean isPlaySound() {
        return playSound;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }

    public boolean isReadAloud() {
        return readAloud;
    }

    public void setReadAloud(boolean readAloud) {
        this.readAloud = readAloud;
    }

    public NotificationGroup copy() {
        return new NotificationGroup(id, title, displayType, showInToolWindow, playSound, readAloud);
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationGroup that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
