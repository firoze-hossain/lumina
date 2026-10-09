package dev.lumina.play;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings model for Languages & Frameworks > Play in Lumina IDE.
 * Dynamically resolves project modules, compiler flags, routes formatting,
 * and template imports.
 */
public class PlaySettings {

    private boolean usePlayCompiler = false;
    private boolean dontCompileWithinIdeBeforeRun = true;
    private String playModule = "Module: 'lumina-ide'";
    private String projectUri = "";
    private String additionalSbtOptions = "";

    private int minSpacesForRoutesFormatting = 8;
    private boolean ignoreUrlDepthInRouteFiles = false;
    private boolean reformatRoutesFileOnEnter = false;

    private boolean excludeTargetDirOnRefresh = true;
    private boolean coloredOutputConsole = true;
    private boolean showCodeRefsInOutputConsole = true;
    private boolean setTemplateImportsManually = false;
    private List<String> templateImports = new ArrayList<>();

    private boolean gutterActionMethods = true;
    private boolean gutterViewCalls = true;
    private boolean gutterResultCalls = true;

    public PlaySettings() {
        this.playModule = detectDefaultModule();
    }

    public PlaySettings(PlaySettings other) {
        if (other != null) {
            this.usePlayCompiler = other.usePlayCompiler;
            this.dontCompileWithinIdeBeforeRun = other.dontCompileWithinIdeBeforeRun;
            this.playModule = other.playModule;
            this.projectUri = other.projectUri;
            this.additionalSbtOptions = other.additionalSbtOptions;
            this.minSpacesForRoutesFormatting = other.minSpacesForRoutesFormatting;
            this.ignoreUrlDepthInRouteFiles = other.ignoreUrlDepthInRouteFiles;
            this.reformatRoutesFileOnEnter = other.reformatRoutesFileOnEnter;
            this.excludeTargetDirOnRefresh = other.excludeTargetDirOnRefresh;
            this.coloredOutputConsole = other.coloredOutputConsole;
            this.showCodeRefsInOutputConsole = other.showCodeRefsInOutputConsole;
            this.setTemplateImportsManually = other.setTemplateImportsManually;
            if (other.templateImports != null) {
                this.templateImports = new ArrayList<>(other.templateImports);
            }
            this.gutterActionMethods = other.gutterActionMethods;
            this.gutterViewCalls = other.gutterViewCalls;
            this.gutterResultCalls = other.gutterResultCalls;
        }
    }

    public PlaySettings copy() {
        return new PlaySettings(this);
    }

    /**
     * Dynamically detects available project modules from the current directory or build files.
     */
    public static List<String> detectAvailableModules() {
        List<String> modules = new ArrayList<>();
        try {
            Path currentDir = Path.of(System.getProperty("user.dir", "."));
            String moduleName = currentDir.getFileName() != null ? currentDir.getFileName().toString() : "lumina-ide";

            // If pom.xml exists, extract artifactId if possible
            Path pomPath = currentDir.resolve("pom.xml");
            if (Files.exists(pomPath)) {
                String pomContent = Files.readString(pomPath);
                int artIdx = pomContent.indexOf("<artifactId>");
                if (artIdx >= 0) {
                    int endIdx = pomContent.indexOf("</artifactId>", artIdx);
                    if (endIdx > artIdx) {
                        String art = pomContent.substring(artIdx + 12, endIdx).trim();
                        if (!art.isEmpty()) {
                            moduleName = art;
                        }
                    }
                }
            }

            modules.add("Module: '" + moduleName + "'");

            // Look for sub-modules with build.sbt or pom.xml
            File[] subdirs = currentDir.toFile().listFiles(File::isDirectory);
            if (subdirs != null) {
                for (File sub : subdirs) {
                    if (new File(sub, "build.sbt").exists() || new File(sub, "pom.xml").exists() || new File(sub, "conf/routes").exists()) {
                        String subModuleName = "Module: '" + sub.getName() + "'";
                        if (!modules.contains(subModuleName)) {
                            modules.add(subModuleName);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            modules.add("Module: 'lumina-ide'");
        }

        if (modules.isEmpty()) {
            modules.add("Module: 'lumina-ide'");
        }
        return modules;
    }

    public static String detectDefaultModule() {
        List<String> list = detectAvailableModules();
        return list.isEmpty() ? "Module: 'lumina-ide'" : list.get(0);
    }

    public boolean isUsePlayCompiler() {
        return usePlayCompiler;
    }

    public void setUsePlayCompiler(boolean usePlayCompiler) {
        this.usePlayCompiler = usePlayCompiler;
    }

    public boolean isDontCompileWithinIdeBeforeRun() {
        return dontCompileWithinIdeBeforeRun;
    }

    public void setDontCompileWithinIdeBeforeRun(boolean dontCompileWithinIdeBeforeRun) {
        this.dontCompileWithinIdeBeforeRun = dontCompileWithinIdeBeforeRun;
    }

    public String getPlayModule() {
        return playModule;
    }

    public void setPlayModule(String playModule) {
        this.playModule = playModule != null ? playModule : "Module: 'lumina-ide'";
    }

    public String getProjectUri() {
        return projectUri;
    }

    public void setProjectUri(String projectUri) {
        this.projectUri = projectUri != null ? projectUri : "";
    }

    public String getAdditionalSbtOptions() {
        return additionalSbtOptions;
    }

    public void setAdditionalSbtOptions(String additionalSbtOptions) {
        this.additionalSbtOptions = additionalSbtOptions != null ? additionalSbtOptions : "";
    }

    public int getMinSpacesForRoutesFormatting() {
        return minSpacesForRoutesFormatting;
    }

    public void setMinSpacesForRoutesFormatting(int minSpacesForRoutesFormatting) {
        this.minSpacesForRoutesFormatting = Math.max(1, Math.min(64, minSpacesForRoutesFormatting));
    }

    public boolean isIgnoreUrlDepthInRouteFiles() {
        return ignoreUrlDepthInRouteFiles;
    }

    public void setIgnoreUrlDepthInRouteFiles(boolean ignoreUrlDepthInRouteFiles) {
        this.ignoreUrlDepthInRouteFiles = ignoreUrlDepthInRouteFiles;
    }

    public boolean isReformatRoutesFileOnEnter() {
        return reformatRoutesFileOnEnter;
    }

    public void setReformatRoutesFileOnEnter(boolean reformatRoutesFileOnEnter) {
        this.reformatRoutesFileOnEnter = reformatRoutesFileOnEnter;
    }

    public boolean isExcludeTargetDirOnRefresh() {
        return excludeTargetDirOnRefresh;
    }

    public void setExcludeTargetDirOnRefresh(boolean excludeTargetDirOnRefresh) {
        this.excludeTargetDirOnRefresh = excludeTargetDirOnRefresh;
    }

    public boolean isColoredOutputConsole() {
        return coloredOutputConsole;
    }

    public void setColoredOutputConsole(boolean coloredOutputConsole) {
        this.coloredOutputConsole = coloredOutputConsole;
    }

    public boolean isShowCodeRefsInOutputConsole() {
        return showCodeRefsInOutputConsole;
    }

    public void setShowCodeRefsInOutputConsole(boolean showCodeRefsInOutputConsole) {
        this.showCodeRefsInOutputConsole = showCodeRefsInOutputConsole;
    }

    public boolean isSetTemplateImportsManually() {
        return setTemplateImportsManually;
    }

    public void setSetTemplateImportsManually(boolean setTemplateImportsManually) {
        this.setTemplateImportsManually = setTemplateImportsManually;
    }

    public List<String> getTemplateImports() {
        return templateImports;
    }

    public void setTemplateImports(List<String> templateImports) {
        this.templateImports.clear();
        if (templateImports != null) {
            this.templateImports.addAll(templateImports);
        }
    }

    public boolean isGutterActionMethods() {
        return gutterActionMethods;
    }

    public void setGutterActionMethods(boolean gutterActionMethods) {
        this.gutterActionMethods = gutterActionMethods;
    }

    public boolean isGutterViewCalls() {
        return gutterViewCalls;
    }

    public void setGutterViewCalls(boolean gutterViewCalls) {
        this.gutterViewCalls = gutterViewCalls;
    }

    public boolean isGutterResultCalls() {
        return gutterResultCalls;
    }

    public void setGutterResultCalls(boolean gutterResultCalls) {
        this.gutterResultCalls = gutterResultCalls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlaySettings that = (PlaySettings) o;
        return usePlayCompiler == that.usePlayCompiler &&
                dontCompileWithinIdeBeforeRun == that.dontCompileWithinIdeBeforeRun &&
                minSpacesForRoutesFormatting == that.minSpacesForRoutesFormatting &&
                ignoreUrlDepthInRouteFiles == that.ignoreUrlDepthInRouteFiles &&
                reformatRoutesFileOnEnter == that.reformatRoutesFileOnEnter &&
                excludeTargetDirOnRefresh == that.excludeTargetDirOnRefresh &&
                coloredOutputConsole == that.coloredOutputConsole &&
                showCodeRefsInOutputConsole == that.showCodeRefsInOutputConsole &&
                setTemplateImportsManually == that.setTemplateImportsManually &&
                gutterActionMethods == that.gutterActionMethods &&
                gutterViewCalls == that.gutterViewCalls &&
                gutterResultCalls == that.gutterResultCalls &&
                Objects.equals(playModule, that.playModule) &&
                Objects.equals(projectUri, that.projectUri) &&
                Objects.equals(additionalSbtOptions, that.additionalSbtOptions) &&
                Objects.equals(templateImports, that.templateImports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usePlayCompiler, dontCompileWithinIdeBeforeRun, playModule, projectUri,
                additionalSbtOptions, minSpacesForRoutesFormatting, ignoreUrlDepthInRouteFiles,
                reformatRoutesFileOnEnter, excludeTargetDirOnRefresh, coloredOutputConsole,
                showCodeRefsInOutputConsole, setTemplateImportsManually, templateImports,
                gutterActionMethods, gutterViewCalls, gutterResultCalls);
    }
}
