package dev.lumina.notification;

/**
 * An executable action hyperlink attached to a notification.
 */
public record NotificationAction(String text, Runnable action) {
    public NotificationAction {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Action text cannot be null or blank");
        }
        if (action == null) {
            action = () -> {};
        }
    }
}
