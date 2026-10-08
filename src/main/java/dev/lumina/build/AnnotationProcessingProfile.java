package dev.lumina.build;

import java.util.*;

/**
 * Model representing an annotation processing profile in Lumina IDE.
 * Matches standard IDE profile configuration (media_1791429993536.png).
 */
public class AnnotationProcessingProfile implements Cloneable {

    private String name = "Default";
    private boolean enabled = true;
    private boolean obtainFromClasspath = true;
    private String processorPath = "";
    private boolean useProcessorModulePath = false;
    private boolean storeRelativeToContentRoot = true;
    private String productionSourcesDirectory = "target/generated-sources/annotations";
    private String testSourcesDirectory = "target/generated-test-sources/test-annotations";
    private boolean runInSeparateStep = false;
    private List<String> modules = new ArrayList<>();
    private List<String> processorNames = new ArrayList<>();
    private Map<String, String> options = new LinkedHashMap<>();

    public AnnotationProcessingProfile() {
    }

    public AnnotationProcessingProfile(String name) {
        this.name = name != null ? name : "Default";
    }

    public AnnotationProcessingProfile(AnnotationProcessingProfile other) {
        if (other != null) {
            this.name = other.name;
            this.enabled = other.enabled;
            this.obtainFromClasspath = other.obtainFromClasspath;
            this.processorPath = other.processorPath;
            this.useProcessorModulePath = other.useProcessorModulePath;
            this.storeRelativeToContentRoot = other.storeRelativeToContentRoot;
            this.productionSourcesDirectory = other.productionSourcesDirectory;
            this.testSourcesDirectory = other.testSourcesDirectory;
            this.runInSeparateStep = other.runInSeparateStep;
            this.modules = new ArrayList<>(other.modules);
            this.processorNames = new ArrayList<>(other.processorNames);
            this.options = new LinkedHashMap<>(other.options);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "Default";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isObtainFromClasspath() {
        return obtainFromClasspath;
    }

    public void setObtainFromClasspath(boolean obtainFromClasspath) {
        this.obtainFromClasspath = obtainFromClasspath;
    }

    public String getProcessorPath() {
        return processorPath != null ? processorPath : "";
    }

    public void setProcessorPath(String processorPath) {
        this.processorPath = processorPath != null ? processorPath.trim() : "";
    }

    public boolean isUseProcessorModulePath() {
        return useProcessorModulePath;
    }

    public void setUseProcessorModulePath(boolean useProcessorModulePath) {
        this.useProcessorModulePath = useProcessorModulePath;
    }

    public boolean isStoreRelativeToContentRoot() {
        return storeRelativeToContentRoot;
    }

    public void setStoreRelativeToContentRoot(boolean storeRelativeToContentRoot) {
        this.storeRelativeToContentRoot = storeRelativeToContentRoot;
    }

    public String getProductionSourcesDirectory() {
        return productionSourcesDirectory != null ? productionSourcesDirectory : "target/generated-sources/annotations";
    }

    public void setProductionSourcesDirectory(String productionSourcesDirectory) {
        this.productionSourcesDirectory = productionSourcesDirectory != null ? productionSourcesDirectory.trim() : "target/generated-sources/annotations";
    }

    public String getTestSourcesDirectory() {
        return testSourcesDirectory != null ? testSourcesDirectory : "target/generated-test-sources/test-annotations";
    }

    public void setTestSourcesDirectory(String testSourcesDirectory) {
        this.testSourcesDirectory = testSourcesDirectory != null ? testSourcesDirectory.trim() : "target/generated-test-sources/test-annotations";
    }

    public boolean isRunInSeparateStep() {
        return runInSeparateStep;
    }

    public void setRunInSeparateStep(boolean runInSeparateStep) {
        this.runInSeparateStep = runInSeparateStep;
    }

    public List<String> getModules() {
        return modules != null ? modules : new ArrayList<>();
    }

    public void setModules(List<String> modules) {
        this.modules = modules != null ? new ArrayList<>(modules) : new ArrayList<>();
    }

    public List<String> getProcessorNames() {
        return processorNames != null ? processorNames : new ArrayList<>();
    }

    public void setProcessorNames(List<String> processorNames) {
        this.processorNames = processorNames != null ? new ArrayList<>(processorNames) : new ArrayList<>();
    }

    public Map<String, String> getOptions() {
        return options != null ? options : new LinkedHashMap<>();
    }

    public void setOptions(Map<String, String> options) {
        this.options = options != null ? new LinkedHashMap<>(options) : new LinkedHashMap<>();
    }

    @Override
    public AnnotationProcessingProfile clone() {
        return new AnnotationProcessingProfile(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnotationProcessingProfile that = (AnnotationProcessingProfile) o;
        return enabled == that.enabled &&
                obtainFromClasspath == that.obtainFromClasspath &&
                useProcessorModulePath == that.useProcessorModulePath &&
                storeRelativeToContentRoot == that.storeRelativeToContentRoot &&
                runInSeparateStep == that.runInSeparateStep &&
                Objects.equals(name, that.name) &&
                Objects.equals(processorPath, that.processorPath) &&
                Objects.equals(productionSourcesDirectory, that.productionSourcesDirectory) &&
                Objects.equals(testSourcesDirectory, that.testSourcesDirectory) &&
                Objects.equals(modules, that.modules) &&
                Objects.equals(processorNames, that.processorNames) &&
                Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, enabled, obtainFromClasspath, processorPath, useProcessorModulePath,
                storeRelativeToContentRoot, productionSourcesDirectory, testSourcesDirectory, runInSeparateStep,
                modules, processorNames, options);
    }
}
