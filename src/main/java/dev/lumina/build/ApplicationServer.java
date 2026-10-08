package dev.lumina.build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a configured Application Server instance in Lumina IDE.
 */
public class ApplicationServer implements Cloneable {

    private String id;
    private String name;
    private String typeId;
    private String homePath;
    private String baseDirectory;
    private String version;
    private final List<String> libraries = new ArrayList<>();

    public ApplicationServer() {
        this.id = UUID.randomUUID().toString();
    }

    public ApplicationServer(String name, String typeId, String homePath) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.typeId = typeId;
        this.homePath = homePath;
    }

    public ApplicationServer(ApplicationServer other) {
        if (other != null) {
            this.id = other.id;
            this.name = other.name;
            this.typeId = other.typeId;
            this.homePath = other.homePath;
            this.baseDirectory = other.baseDirectory;
            this.version = other.version;
            this.libraries.addAll(other.libraries);
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
        this.name = name;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getHomePath() {
        return homePath;
    }

    public void setHomePath(String homePath) {
        this.homePath = homePath;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getLibraries() {
        return Collections.unmodifiableList(libraries);
    }

    public void setLibraries(List<String> newLibs) {
        this.libraries.clear();
        if (newLibs != null) {
            this.libraries.addAll(newLibs);
        }
    }

    public void addLibrary(String library) {
        if (library != null && !libraries.contains(library)) {
            this.libraries.add(library);
        }
    }

    @Override
    public ApplicationServer clone() {
        return new ApplicationServer(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationServer that = (ApplicationServer) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(typeId, that.typeId) &&
                Objects.equals(homePath, that.homePath) &&
                Objects.equals(baseDirectory, that.baseDirectory) &&
                Objects.equals(version, that.version) &&
                Objects.equals(libraries, that.libraries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, typeId, homePath, baseDirectory, version, libraries);
    }
}
