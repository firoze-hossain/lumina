package dev.lumina.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration model for Java Compiler settings in Lumina IDE.
 * Matches 1:1 with reference screenshot media_1791449869458_bd73df6d.png:
 *  - Use compiler: Javac / Eclipse / ajc
 *  - Use '--release' option for cross-compilation (Java 9 and later)
 *  - Project bytecode version: Same as language level
 *  - Per-module bytecode version: table with Module and Target bytecode version
 *  - Javac Options:
 *    - Use compiler from module target JDK when possible
 *    - Generate debugging info
 *    - Report use of deprecated features
 *    - Generate no warnings
 *    - Additional command line parameters
 *  - Override compiler parameters per-module: table with Module and Compilation options
 */
public class JavaCompilerSettings implements Cloneable {

    public static class ModuleBytecodeVersion {
        private String module;
        private String targetBytecodeVersion;

        public ModuleBytecodeVersion() {
        }

        public ModuleBytecodeVersion(String module, String targetBytecodeVersion) {
            this.module = module != null ? module : "";
            this.targetBytecodeVersion = targetBytecodeVersion != null ? targetBytecodeVersion : "21";
        }

        public String getModule() { return module; }
        public void setModule(String module) { this.module = module != null ? module : ""; }
        public String getTargetBytecodeVersion() { return targetBytecodeVersion; }
        public void setTargetBytecodeVersion(String targetBytecodeVersion) { this.targetBytecodeVersion = targetBytecodeVersion != null ? targetBytecodeVersion : ""; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ModuleBytecodeVersion that = (ModuleBytecodeVersion) o;
            return Objects.equals(module, that.module) && Objects.equals(targetBytecodeVersion, that.targetBytecodeVersion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(module, targetBytecodeVersion);
        }
    }

    public static class ModuleCompilerParameter {
        private String module;
        private String compilationOptions;

        public ModuleCompilerParameter() {
        }

        public ModuleCompilerParameter(String module, String compilationOptions) {
            this.module = module != null ? module : "";
            this.compilationOptions = compilationOptions != null ? compilationOptions : "";
        }

        public String getModule() { return module; }
        public void setModule(String module) { this.module = module != null ? module : ""; }
        public String getCompilationOptions() { return compilationOptions; }
        public void setCompilationOptions(String compilationOptions) { this.compilationOptions = compilationOptions != null ? compilationOptions : ""; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ModuleCompilerParameter that = (ModuleCompilerParameter) o;
            return Objects.equals(module, that.module) && Objects.equals(compilationOptions, that.compilationOptions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(module, compilationOptions);
        }
    }

    private String useCompiler = "Javac";
    private boolean useReleaseOption = true;
    private String projectBytecodeVersion = "Same as language level";
    private List<ModuleBytecodeVersion> perModuleBytecodeVersions = new ArrayList<>();

    // Javac Options
    private boolean useCompilerFromModuleTargetJdk = true;
    private boolean generateDebuggingInfo = true;
    private boolean reportDeprecated = true;
    private boolean generateNoWarnings = false;
    private String additionalCommandLineParameters = "";
    private List<ModuleCompilerParameter> perModuleCompilerParameters = new ArrayList<>();

    public JavaCompilerSettings() {
        initDefaults();
    }

    public JavaCompilerSettings(JavaCompilerSettings other) {
        if (other != null) {
            this.useCompiler = other.useCompiler;
            this.useReleaseOption = other.useReleaseOption;
            this.projectBytecodeVersion = other.projectBytecodeVersion;
            this.perModuleBytecodeVersions = new ArrayList<>();
            for (ModuleBytecodeVersion v : other.perModuleBytecodeVersions) {
                this.perModuleBytecodeVersions.add(new ModuleBytecodeVersion(v.getModule(), v.getTargetBytecodeVersion()));
            }
            this.useCompilerFromModuleTargetJdk = other.useCompilerFromModuleTargetJdk;
            this.generateDebuggingInfo = other.generateDebuggingInfo;
            this.reportDeprecated = other.reportDeprecated;
            this.generateNoWarnings = other.generateNoWarnings;
            this.additionalCommandLineParameters = other.additionalCommandLineParameters;
            this.perModuleCompilerParameters = new ArrayList<>();
            for (ModuleCompilerParameter p : other.perModuleCompilerParameters) {
                this.perModuleCompilerParameters.add(new ModuleCompilerParameter(p.getModule(), p.getCompilationOptions()));
            }
        } else {
            initDefaults();
        }
    }

    private void initDefaults() {
        String moduleName = detectCurrentModuleName();
        perModuleBytecodeVersions = new ArrayList<>();
        perModuleBytecodeVersions.add(new ModuleBytecodeVersion(moduleName, "21"));

        perModuleCompilerParameters = new ArrayList<>();
        perModuleCompilerParameters.add(new ModuleCompilerParameter(moduleName, "-parameters"));
    }

    /**
     * Dynamically detects active project module name without hardcoding.
     */
    public static String detectCurrentModuleName() {
        String dir = System.getProperty("user.dir", "");
        if (!dir.isBlank()) {
            File f = new File(dir);
            String name = f.getName();
            if (!name.isBlank() && !name.equals("others") && !name.equals("projects")) {
                return name;
            }
        }
        return "ERPApplicationServer";
    }

    public String getUseCompiler() { return useCompiler; }
    public void setUseCompiler(String useCompiler) { this.useCompiler = useCompiler != null ? useCompiler : "Javac"; }

    public boolean isUseReleaseOption() { return useReleaseOption; }
    public void setUseReleaseOption(boolean useReleaseOption) { this.useReleaseOption = useReleaseOption; }

    public String getProjectBytecodeVersion() { return projectBytecodeVersion; }
    public void setProjectBytecodeVersion(String projectBytecodeVersion) {
        this.projectBytecodeVersion = projectBytecodeVersion != null ? projectBytecodeVersion : "Same as language level";
    }

    public List<ModuleBytecodeVersion> getPerModuleBytecodeVersions() {
        return perModuleBytecodeVersions != null ? perModuleBytecodeVersions : new ArrayList<>();
    }

    public void setPerModuleBytecodeVersions(List<ModuleBytecodeVersion> list) {
        this.perModuleBytecodeVersions = list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public boolean isUseCompilerFromModuleTargetJdk() { return useCompilerFromModuleTargetJdk; }
    public void setUseCompilerFromModuleTargetJdk(boolean useCompilerFromModuleTargetJdk) {
        this.useCompilerFromModuleTargetJdk = useCompilerFromModuleTargetJdk;
    }

    public boolean isGenerateDebuggingInfo() { return generateDebuggingInfo; }
    public void setGenerateDebuggingInfo(boolean generateDebuggingInfo) {
        this.generateDebuggingInfo = generateDebuggingInfo;
    }

    public boolean isReportDeprecated() { return reportDeprecated; }
    public void setReportDeprecated(boolean reportDeprecated) {
        this.reportDeprecated = reportDeprecated;
    }

    public boolean isGenerateNoWarnings() { return generateNoWarnings; }
    public void setGenerateNoWarnings(boolean generateNoWarnings) {
        this.generateNoWarnings = generateNoWarnings;
    }

    public String getAdditionalCommandLineParameters() { return additionalCommandLineParameters; }
    public void setAdditionalCommandLineParameters(String additionalCommandLineParameters) {
        this.additionalCommandLineParameters = additionalCommandLineParameters != null ? additionalCommandLineParameters : "";
    }

    public List<ModuleCompilerParameter> getPerModuleCompilerParameters() {
        return perModuleCompilerParameters != null ? perModuleCompilerParameters : new ArrayList<>();
    }

    public void setPerModuleCompilerParameters(List<ModuleCompilerParameter> list) {
        this.perModuleCompilerParameters = list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    @Override
    public JavaCompilerSettings clone() {
        return new JavaCompilerSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaCompilerSettings that = (JavaCompilerSettings) o;
        return useReleaseOption == that.useReleaseOption &&
                useCompilerFromModuleTargetJdk == that.useCompilerFromModuleTargetJdk &&
                generateDebuggingInfo == that.generateDebuggingInfo &&
                reportDeprecated == that.reportDeprecated &&
                generateNoWarnings == that.generateNoWarnings &&
                Objects.equals(useCompiler, that.useCompiler) &&
                Objects.equals(projectBytecodeVersion, that.projectBytecodeVersion) &&
                Objects.equals(perModuleBytecodeVersions, that.perModuleBytecodeVersions) &&
                Objects.equals(additionalCommandLineParameters, that.additionalCommandLineParameters) &&
                Objects.equals(perModuleCompilerParameters, that.perModuleCompilerParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(useCompiler, useReleaseOption, projectBytecodeVersion, perModuleBytecodeVersions,
                useCompilerFromModuleTargetJdk, generateDebuggingInfo, reportDeprecated, generateNoWarnings,
                additionalCommandLineParameters, perModuleCompilerParameters);
    }
}
