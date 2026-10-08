package dev.lumina.docker;

import java.util.Objects;
import java.util.UUID;

/**
 * Configuration for a Docker Registry matching IntelliJ IDEA (Screenshot 5).
 */
public class DockerRegistryConfig implements Cloneable {

    private String id = UUID.randomUUID().toString();
    private String name = "Docker Registry";
    private DockerRegistryType registryType = DockerRegistryType.DOCKER_HUB;
    private String address = "registry-1.docker.io";
    private String username = "";
    private String password = "";

    public DockerRegistryConfig() {
    }

    public DockerRegistryConfig(String name) {
        this.name = name != null ? name : "Docker Registry";
    }

    public DockerRegistryConfig(String name, DockerRegistryType registryType) {
        this.name = name != null ? name : "Docker Registry";
        this.registryType = registryType != null ? registryType : DockerRegistryType.DOCKER_HUB;
        this.address = this.registryType.getDefaultAddress();
    }

    public DockerRegistryConfig(DockerRegistryConfig other) {
        if (other != null) {
            this.id = other.id;
            this.name = other.name;
            this.registryType = other.registryType != null ? other.registryType : DockerRegistryType.DOCKER_HUB;
            this.address = other.address;
            this.username = other.username;
            this.password = other.password;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "Docker Registry";
    }

    public DockerRegistryType getRegistryType() {
        return registryType;
    }

    public void setRegistryType(DockerRegistryType registryType) {
        this.registryType = registryType != null ? registryType : DockerRegistryType.DOCKER_HUB;
        if (this.registryType != DockerRegistryType.OTHER && !this.registryType.getDefaultAddress().isEmpty()) {
            this.address = this.registryType.getDefaultAddress();
        }
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address != null ? address : "";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username != null ? username : "";
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password != null ? password : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DockerRegistryConfig that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                registryType == that.registryType &&
                Objects.equals(address, that.address) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, registryType, address, username, password);
    }

    @Override
    public DockerRegistryConfig clone() {
        return new DockerRegistryConfig(this);
    }
}
