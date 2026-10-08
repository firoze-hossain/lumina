package dev.lumina.docker;

/**
 * Docker Registry types matching IntelliJ IDEA (Screenshot 5).
 */
public enum DockerRegistryType {
    DOCKER_HUB("Docker Hub", "registry-1.docker.io"),
    GITLAB("GitLab", "registry.gitlab.com"),
    GITHUB("GitHub", "ghcr.io"),
    OTHER("Custom / Other", "");

    private final String displayName;
    private final String defaultAddress;

    DockerRegistryType(String displayName, String defaultAddress) {
        this.displayName = displayName;
        this.defaultAddress = defaultAddress;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultAddress() {
        return defaultAddress;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
