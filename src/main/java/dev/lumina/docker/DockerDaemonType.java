package dev.lumina.docker;

/**
 * Docker daemon connection types matching IntelliJ IDEA (Screenshot 3).
 */
public enum DockerDaemonType {
    UNIX_SOCKET("Unix socket:"),
    TCP_SOCKET("TCP socket"),
    PODMAN("Podman"),
    DOCKER_IN_DOCKER("Docker-in-Docker"),
    SSH("SSH:");

    private final String displayName;

    DockerDaemonType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
