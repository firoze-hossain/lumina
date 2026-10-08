package dev.lumina.run;

/**
 * Types of execution targets supported in Lumina IDE (matching IntelliJ IDEA Run Targets).
 */
public enum RunTargetType {
    SSH("SSH", "🔌"),
    DOCKER("Docker", "🐳"),
    DOCKER_COMPOSE("Docker Compose", "🐙");

    private final String displayName;
    private final String iconSymbol;

    RunTargetType(String displayName, String iconSymbol) {
        this.displayName = displayName;
        this.iconSymbol = iconSymbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconSymbol() {
        return iconSymbol;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
