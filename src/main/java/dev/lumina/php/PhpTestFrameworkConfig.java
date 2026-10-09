package dev.lumina.php;

import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a configured PHP Test Framework (PHPUnit) in Lumina IDE (Languages & Frameworks > PHP > Test Frameworks).
 * Faithfully matches Image 5.
 */
public class PhpTestFrameworkConfig {

    private String id;
    private String name;
    private String interpreterId;
    private boolean useComposerAutoloader = true;
    private String pathToScript = "";
    private String detectedVersion = "Not installed";
    private boolean useDefaultConfigFile = false;
    private String defaultConfigFilePath = "";
    private boolean useDefaultBootstrapFile = false;
    private String defaultBootstrapFilePath = "";
    private String defaultParaTestBinary = "";
    private String testRootsDirectory = "tests";

    public PhpTestFrameworkConfig() {
        this(UUID.randomUUID().toString(), "Main Local PHP 8.3.6", "default-cli-php");
    }

    public PhpTestFrameworkConfig(String id, String name, String interpreterId) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name != null ? name : "Main Local PHP";
        this.interpreterId = interpreterId != null ? interpreterId : "default-cli-php";
    }

    public PhpTestFrameworkConfig copy() {
        PhpTestFrameworkConfig copy = new PhpTestFrameworkConfig(id, name, interpreterId);
        copy.useComposerAutoloader = this.useComposerAutoloader;
        copy.pathToScript = this.pathToScript;
        copy.detectedVersion = this.detectedVersion;
        copy.useDefaultConfigFile = this.useDefaultConfigFile;
        copy.defaultConfigFilePath = this.defaultConfigFilePath;
        copy.useDefaultBootstrapFile = this.useDefaultBootstrapFile;
        copy.defaultBootstrapFilePath = this.defaultBootstrapFilePath;
        copy.defaultParaTestBinary = this.defaultParaTestBinary;
        copy.testRootsDirectory = this.testRootsDirectory;
        return copy;
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
        this.name = name != null ? name : "Main Local PHP";
    }

    public String getInterpreterId() {
        return interpreterId;
    }

    public void setInterpreterId(String interpreterId) {
        this.interpreterId = interpreterId != null ? interpreterId : "default-cli-php";
    }

    public boolean isUseComposerAutoloader() {
        return useComposerAutoloader;
    }

    public void setUseComposerAutoloader(boolean useComposerAutoloader) {
        this.useComposerAutoloader = useComposerAutoloader;
    }

    public String getPathToScript() {
        return pathToScript;
    }

    public void setPathToScript(String pathToScript) {
        this.pathToScript = pathToScript != null ? pathToScript : "";
    }

    public String getDetectedVersion() {
        return detectedVersion;
    }

    public void setDetectedVersion(String detectedVersion) {
        this.detectedVersion = detectedVersion != null ? detectedVersion : "Not installed";
    }

    public boolean isUseDefaultConfigFile() {
        return useDefaultConfigFile;
    }

    public void setUseDefaultConfigFile(boolean useDefaultConfigFile) {
        this.useDefaultConfigFile = useDefaultConfigFile;
    }

    public String getDefaultConfigFilePath() {
        return defaultConfigFilePath;
    }

    public void setDefaultConfigFilePath(String defaultConfigFilePath) {
        this.defaultConfigFilePath = defaultConfigFilePath != null ? defaultConfigFilePath : "";
    }

    public boolean isUseDefaultBootstrapFile() {
        return useDefaultBootstrapFile;
    }

    public void setUseDefaultBootstrapFile(boolean useDefaultBootstrapFile) {
        this.useDefaultBootstrapFile = useDefaultBootstrapFile;
    }

    public String getDefaultBootstrapFilePath() {
        return defaultBootstrapFilePath;
    }

    public void setDefaultBootstrapFilePath(String defaultBootstrapFilePath) {
        this.defaultBootstrapFilePath = defaultBootstrapFilePath != null ? defaultBootstrapFilePath : "";
    }

    public String getDefaultParaTestBinary() {
        return defaultParaTestBinary;
    }

    public void setDefaultParaTestBinary(String defaultParaTestBinary) {
        this.defaultParaTestBinary = defaultParaTestBinary != null ? defaultParaTestBinary : "";
    }

    public String getTestRootsDirectory() {
        return testRootsDirectory;
    }

    public void setTestRootsDirectory(String testRootsDirectory) {
        this.testRootsDirectory = testRootsDirectory != null ? testRootsDirectory : "tests";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhpTestFrameworkConfig that = (PhpTestFrameworkConfig) o;
        return useComposerAutoloader == that.useComposerAutoloader &&
                useDefaultConfigFile == that.useDefaultConfigFile &&
                useDefaultBootstrapFile == that.useDefaultBootstrapFile &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(interpreterId, that.interpreterId) &&
                Objects.equals(pathToScript, that.pathToScript) &&
                Objects.equals(detectedVersion, that.detectedVersion) &&
                Objects.equals(defaultConfigFilePath, that.defaultConfigFilePath) &&
                Objects.equals(defaultBootstrapFilePath, that.defaultBootstrapFilePath) &&
                Objects.equals(defaultParaTestBinary, that.defaultParaTestBinary) &&
                Objects.equals(testRootsDirectory, that.testRootsDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, interpreterId, useComposerAutoloader, pathToScript,
                detectedVersion, useDefaultConfigFile, defaultConfigFilePath, useDefaultBootstrapFile,
                defaultBootstrapFilePath, defaultParaTestBinary, testRootsDirectory);
    }

    @Override
    public String toString() {
        return name != null ? name : "PHPUnit";
    }
}
