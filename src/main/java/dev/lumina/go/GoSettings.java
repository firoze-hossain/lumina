package dev.lumina.go;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Go configuration in Lumina IDE.
 * Covers Go parent page, GOROOT, GOPATH, Go Modules, Build Tags, Formatting Functions, and Imports.
 * Matches Images 1, 2, 3, 4, and 5.
 */
public class GoSettings {

    // General Go Options
    private boolean suggestParametersNameInCompletion = true;
    private boolean suggestVariantsRequireAdditionalImports = true;
    private boolean indentOnEnterInRawStrings = false;
    private boolean showDocInParameterInfo = false;
    private boolean detectGoPackagesFromClipboard = false;
    private boolean askBeforeSharingInGoPlayground = true;

    private String whenDirectoryRenamed = "Show options"; // "Show options", "Rename package", "Do not rename package"
    private String whenPackageRenamed = "Show options"; // "Show options", "Rename directory", "Do not rename directory"
    private String whenFileRenamed = "Show options"; // "Show options", "Rename corresponding test or production file", "Do not rename corresponding test or production file"
    private String whenJsonPasted = "Show options"; // "Show options", "Convert JSON to a Go type", "Insert JSON as-is"
    private String whenTagRenamed = "Show options"; // "Show options", "Rename a tag", "Do not rename tag"

    // GOROOT
    private String goRootPath = "";
    private String goRootVersion = "";

    // GOPATH
    private List<String> globalGoPaths = new ArrayList<>();
    private List<String> projectGoPaths = new ArrayList<>();
    private boolean useGoPathFromEnv = true;
    private boolean indexEntireGoPath = false;

    // Go Modules
    private boolean enableGoModulesIntegration = true;
    private String environmentVariables = "";
    private boolean enableVendoringSupportAutomatically = true;
    private String downloadGoModuleDependencies = "Enable for all projects"; // "Enable for all projects", "Enable for non-vendored projects only", "Disabled"

    // Build Tags (Image 1)
    private List<String> buildTagScopes = new ArrayList<>();
    private String customBuildTags = "";
    private String osTarget = "any";
    private String archTarget = "any";

    // Formatting Functions (Image 2)
    private List<String> excludedFormattingFunctions = new ArrayList<>();

    // Imports (Image 3)
    private boolean showImportPopup = true;
    private boolean addUnambiguousImportsOnTheFly = true;
    private boolean optimizeImportsOnTheFly = true;
    private List<String> excludedImports = new ArrayList<>(List.of(
            "github.com/pkg/errors",
            "golang.org/x/net/context"
    ));

    public GoSettings() {}

    public GoSettings(GoSettings other) {
        if (other == null) return;
        this.suggestParametersNameInCompletion = other.suggestParametersNameInCompletion;
        this.suggestVariantsRequireAdditionalImports = other.suggestVariantsRequireAdditionalImports;
        this.indentOnEnterInRawStrings = other.indentOnEnterInRawStrings;
        this.showDocInParameterInfo = other.showDocInParameterInfo;
        this.detectGoPackagesFromClipboard = other.detectGoPackagesFromClipboard;
        this.askBeforeSharingInGoPlayground = other.askBeforeSharingInGoPlayground;
        this.whenDirectoryRenamed = other.whenDirectoryRenamed;
        this.whenPackageRenamed = other.whenPackageRenamed;
        this.whenFileRenamed = other.whenFileRenamed;
        this.whenJsonPasted = other.whenJsonPasted;
        this.whenTagRenamed = other.whenTagRenamed;
        this.goRootPath = other.goRootPath;
        this.goRootVersion = other.goRootVersion;
        this.globalGoPaths = new ArrayList<>(other.globalGoPaths);
        this.projectGoPaths = new ArrayList<>(other.projectGoPaths);
        this.useGoPathFromEnv = other.useGoPathFromEnv;
        this.indexEntireGoPath = other.indexEntireGoPath;
        this.enableGoModulesIntegration = other.enableGoModulesIntegration;
        this.environmentVariables = other.environmentVariables;
        this.enableVendoringSupportAutomatically = other.enableVendoringSupportAutomatically;
        this.downloadGoModuleDependencies = other.downloadGoModuleDependencies;

        this.buildTagScopes = new ArrayList<>(other.buildTagScopes);
        this.customBuildTags = other.customBuildTags;
        this.osTarget = other.osTarget;
        this.archTarget = other.archTarget;

        this.excludedFormattingFunctions = new ArrayList<>(other.excludedFormattingFunctions);

        this.showImportPopup = other.showImportPopup;
        this.addUnambiguousImportsOnTheFly = other.addUnambiguousImportsOnTheFly;
        this.optimizeImportsOnTheFly = other.optimizeImportsOnTheFly;
        this.excludedImports = new ArrayList<>(other.excludedImports);
    }

    public GoSettings copy() {
        return new GoSettings(this);
    }

    // Getters and setters
    public boolean isSuggestParametersNameInCompletion() {
        return suggestParametersNameInCompletion;
    }

    public void setSuggestParametersNameInCompletion(boolean suggestParametersNameInCompletion) {
        this.suggestParametersNameInCompletion = suggestParametersNameInCompletion;
    }

    public boolean isSuggestVariantsRequireAdditionalImports() {
        return suggestVariantsRequireAdditionalImports;
    }

    public void setSuggestVariantsRequireAdditionalImports(boolean suggestVariantsRequireAdditionalImports) {
        this.suggestVariantsRequireAdditionalImports = suggestVariantsRequireAdditionalImports;
    }

    public boolean isIndentOnEnterInRawStrings() {
        return indentOnEnterInRawStrings;
    }

    public void setIndentOnEnterInRawStrings(boolean indentOnEnterInRawStrings) {
        this.indentOnEnterInRawStrings = indentOnEnterInRawStrings;
    }

    public boolean isShowDocInParameterInfo() {
        return showDocInParameterInfo;
    }

    public void setShowDocInParameterInfo(boolean showDocInParameterInfo) {
        this.showDocInParameterInfo = showDocInParameterInfo;
    }

    public boolean isDetectGoPackagesFromClipboard() {
        return detectGoPackagesFromClipboard;
    }

    public void setDetectGoPackagesFromClipboard(boolean detectGoPackagesFromClipboard) {
        this.detectGoPackagesFromClipboard = detectGoPackagesFromClipboard;
    }

    public boolean isAskBeforeSharingInGoPlayground() {
        return askBeforeSharingInGoPlayground;
    }

    public void setAskBeforeSharingInGoPlayground(boolean askBeforeSharingInGoPlayground) {
        this.askBeforeSharingInGoPlayground = askBeforeSharingInGoPlayground;
    }

    public String getWhenDirectoryRenamed() {
        return whenDirectoryRenamed;
    }

    public void setWhenDirectoryRenamed(String whenDirectoryRenamed) {
        this.whenDirectoryRenamed = whenDirectoryRenamed != null ? whenDirectoryRenamed : "Show options";
    }

    public String getWhenPackageRenamed() {
        return whenPackageRenamed;
    }

    public void setWhenPackageRenamed(String whenPackageRenamed) {
        this.whenPackageRenamed = whenPackageRenamed != null ? whenPackageRenamed : "Show options";
    }

    public String getWhenFileRenamed() {
        return whenFileRenamed;
    }

    public void setWhenFileRenamed(String whenFileRenamed) {
        this.whenFileRenamed = whenFileRenamed != null ? whenFileRenamed : "Show options";
    }

    public String getWhenJsonPasted() {
        return whenJsonPasted;
    }

    public void setWhenJsonPasted(String whenJsonPasted) {
        this.whenJsonPasted = whenJsonPasted != null ? whenJsonPasted : "Show options";
    }

    public String getWhenTagRenamed() {
        return whenTagRenamed;
    }

    public void setWhenTagRenamed(String whenTagRenamed) {
        this.whenTagRenamed = whenTagRenamed != null ? whenTagRenamed : "Show options";
    }

    public String getGoRootPath() {
        return goRootPath;
    }

    public void setGoRootPath(String goRootPath) {
        this.goRootPath = goRootPath != null ? goRootPath : "";
    }

    public String getGoRootVersion() {
        return goRootVersion;
    }

    public void setGoRootVersion(String goRootVersion) {
        this.goRootVersion = goRootVersion != null ? goRootVersion : "";
    }

    public List<String> getGlobalGoPaths() {
        return globalGoPaths;
    }

    public void setGlobalGoPaths(List<String> globalGoPaths) {
        this.globalGoPaths = globalGoPaths != null ? new ArrayList<>(globalGoPaths) : new ArrayList<>();
    }

    public List<String> getProjectGoPaths() {
        return projectGoPaths;
    }

    public void setProjectGoPaths(List<String> projectGoPaths) {
        this.projectGoPaths = projectGoPaths != null ? new ArrayList<>(projectGoPaths) : new ArrayList<>();
    }

    public boolean isUseGoPathFromEnv() {
        return useGoPathFromEnv;
    }

    public void setUseGoPathFromEnv(boolean useGoPathFromEnv) {
        this.useGoPathFromEnv = useGoPathFromEnv;
    }

    public boolean isIndexEntireGoPath() {
        return indexEntireGoPath;
    }

    public void setIndexEntireGoPath(boolean indexEntireGoPath) {
        this.indexEntireGoPath = indexEntireGoPath;
    }

    public boolean isEnableGoModulesIntegration() {
        return enableGoModulesIntegration;
    }

    public void setEnableGoModulesIntegration(boolean enableGoModulesIntegration) {
        this.enableGoModulesIntegration = enableGoModulesIntegration;
    }

    public String getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(String environmentVariables) {
        this.environmentVariables = environmentVariables != null ? environmentVariables : "";
    }

    public boolean isEnableVendoringSupportAutomatically() {
        return enableVendoringSupportAutomatically;
    }

    public void setEnableVendoringSupportAutomatically(boolean enableVendoringSupportAutomatically) {
        this.enableVendoringSupportAutomatically = enableVendoringSupportAutomatically;
    }

    public String getDownloadGoModuleDependencies() {
        return downloadGoModuleDependencies;
    }

    public void setDownloadGoModuleDependencies(String downloadGoModuleDependencies) {
        this.downloadGoModuleDependencies = downloadGoModuleDependencies != null ? downloadGoModuleDependencies : "Enable for all projects";
    }

    // Build Tags
    public List<String> getBuildTagScopes() {
        return buildTagScopes;
    }

    public void setBuildTagScopes(List<String> buildTagScopes) {
        this.buildTagScopes = buildTagScopes != null ? new ArrayList<>(buildTagScopes) : new ArrayList<>();
    }

    public String getCustomBuildTags() {
        return customBuildTags;
    }

    public void setCustomBuildTags(String customBuildTags) {
        this.customBuildTags = customBuildTags != null ? customBuildTags : "";
    }

    public String getOsTarget() {
        return osTarget;
    }

    public void setOsTarget(String osTarget) {
        this.osTarget = osTarget != null ? osTarget : "any";
    }

    public String getArchTarget() {
        return archTarget;
    }

    public void setArchTarget(String archTarget) {
        this.archTarget = archTarget != null ? archTarget : "any";
    }

    // Formatting Functions
    public List<String> getExcludedFormattingFunctions() {
        return excludedFormattingFunctions;
    }

    public void setExcludedFormattingFunctions(List<String> excludedFormattingFunctions) {
        this.excludedFormattingFunctions = excludedFormattingFunctions != null ? new ArrayList<>(excludedFormattingFunctions) : new ArrayList<>();
    }

    // Imports
    public boolean isShowImportPopup() {
        return showImportPopup;
    }

    public void setShowImportPopup(boolean showImportPopup) {
        this.showImportPopup = showImportPopup;
    }

    public boolean isAddUnambiguousImportsOnTheFly() {
        return addUnambiguousImportsOnTheFly;
    }

    public void setAddUnambiguousImportsOnTheFly(boolean addUnambiguousImportsOnTheFly) {
        this.addUnambiguousImportsOnTheFly = addUnambiguousImportsOnTheFly;
    }

    public boolean isOptimizeImportsOnTheFly() {
        return optimizeImportsOnTheFly;
    }

    public void setOptimizeImportsOnTheFly(boolean optimizeImportsOnTheFly) {
        this.optimizeImportsOnTheFly = optimizeImportsOnTheFly;
    }

    public List<String> getExcludedImports() {
        return excludedImports;
    }

    public void setExcludedImports(List<String> excludedImports) {
        this.excludedImports = excludedImports != null ? new ArrayList<>(excludedImports) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoSettings that)) return false;
        return suggestParametersNameInCompletion == that.suggestParametersNameInCompletion &&
                suggestVariantsRequireAdditionalImports == that.suggestVariantsRequireAdditionalImports &&
                indentOnEnterInRawStrings == that.indentOnEnterInRawStrings &&
                showDocInParameterInfo == that.showDocInParameterInfo &&
                detectGoPackagesFromClipboard == that.detectGoPackagesFromClipboard &&
                askBeforeSharingInGoPlayground == that.askBeforeSharingInGoPlayground &&
                useGoPathFromEnv == that.useGoPathFromEnv &&
                indexEntireGoPath == that.indexEntireGoPath &&
                enableGoModulesIntegration == that.enableGoModulesIntegration &&
                enableVendoringSupportAutomatically == that.enableVendoringSupportAutomatically &&
                showImportPopup == that.showImportPopup &&
                addUnambiguousImportsOnTheFly == that.addUnambiguousImportsOnTheFly &&
                optimizeImportsOnTheFly == that.optimizeImportsOnTheFly &&
                Objects.equals(whenDirectoryRenamed, that.whenDirectoryRenamed) &&
                Objects.equals(whenPackageRenamed, that.whenPackageRenamed) &&
                Objects.equals(whenFileRenamed, that.whenFileRenamed) &&
                Objects.equals(whenJsonPasted, that.whenJsonPasted) &&
                Objects.equals(whenTagRenamed, that.whenTagRenamed) &&
                Objects.equals(goRootPath, that.goRootPath) &&
                Objects.equals(goRootVersion, that.goRootVersion) &&
                Objects.equals(globalGoPaths, that.globalGoPaths) &&
                Objects.equals(projectGoPaths, that.projectGoPaths) &&
                Objects.equals(environmentVariables, that.environmentVariables) &&
                Objects.equals(downloadGoModuleDependencies, that.downloadGoModuleDependencies) &&
                Objects.equals(buildTagScopes, that.buildTagScopes) &&
                Objects.equals(customBuildTags, that.customBuildTags) &&
                Objects.equals(osTarget, that.osTarget) &&
                Objects.equals(archTarget, that.archTarget) &&
                Objects.equals(excludedFormattingFunctions, that.excludedFormattingFunctions) &&
                Objects.equals(excludedImports, that.excludedImports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(suggestParametersNameInCompletion, suggestVariantsRequireAdditionalImports,
                indentOnEnterInRawStrings, showDocInParameterInfo, detectGoPackagesFromClipboard,
                askBeforeSharingInGoPlayground, whenDirectoryRenamed, whenPackageRenamed,
                whenFileRenamed, whenJsonPasted, whenTagRenamed, goRootPath, goRootVersion,
                globalGoPaths, projectGoPaths, useGoPathFromEnv, indexEntireGoPath,
                enableGoModulesIntegration, environmentVariables, enableVendoringSupportAutomatically,
                downloadGoModuleDependencies, buildTagScopes, customBuildTags, osTarget, archTarget,
                excludedFormattingFunctions, showImportPopup, addUnambiguousImportsOnTheFly,
                optimizeImportsOnTheFly, excludedImports);
    }
}
