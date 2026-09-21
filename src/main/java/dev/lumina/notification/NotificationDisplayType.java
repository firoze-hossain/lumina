package dev.lumina.notification;

/**
 * Display type for notification popups, matching IntelliJ IDEA:
 * - "No popup"
 * - "Balloon"
 * - "Sticky balloon"
 */
public enum NotificationDisplayType {
    NONE("No popup"),
    BALLOON("Balloon"),
    STICKY_BALLOON("Sticky balloon");

    private final String label;

    NotificationDisplayType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static NotificationDisplayType fromLabel(String label) {
        if (label == null) return BALLOON;
        for (NotificationDisplayType t : values()) {
            if (t.label.equalsIgnoreCase(label.trim())) {
                return t;
            }
        }
        return BALLOON;
    }
}
