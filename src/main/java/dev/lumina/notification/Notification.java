package dev.lumina.notification;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A notification instance matching IntelliJ IDEA's Notification object.
 */
public class Notification {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")
            .withZone(ZoneId.systemDefault());

    private final String id;
    private final String groupId;
    private final String title;
    private final String content;
    private final NotificationType type;
    private final Instant timestamp;
    private final String formattedTime;
    private final List<NotificationAction> actions = new ArrayList<>();
    private boolean read;

    public Notification(String groupId, String title, String content, NotificationType type) {
        this(UUID.randomUUID().toString(), groupId, title, content, type, Instant.now());
    }

    public Notification(String id, String groupId, String title, String content, NotificationType type) {
        this(id, groupId, title, content, type, Instant.now());
    }

    public Notification(String id, String groupId, String title, String content, NotificationType type, Instant timestamp) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.groupId = groupId != null ? groupId : "General";
        this.title = title != null ? title : "";
        this.content = content != null ? content : "";
        this.type = type != null ? type : NotificationType.INFORMATION;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.formattedTime = TIME_FORMATTER.format(this.timestamp);
    }

    public Notification addAction(String text, Runnable action) {
        actions.add(new NotificationAction(text, action));
        return this;
    }

    public Notification addAction(NotificationAction action) {
        if (action != null) {
            actions.add(action);
        }
        return this;
    }

    public String getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public NotificationType getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        return formattedTime;
    }

    public List<NotificationAction> getActions() {
        return actions;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
