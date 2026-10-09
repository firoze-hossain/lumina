package dev.lumina.php;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing PHP Quality Tools settings in Lumina IDE.
 * Covers Quality Tools parent page, PHP_CodeSniffer, PHP CS Fixer, Laravel Pint, and Mess Detector.
 */
public class PhpQualityToolsSettings {

    private String externalFormatter = "none"; // "phpcbf", "php_cs_fixer", "laravel_pint", "none"
    private PhpCodeSnifferConfig codeSniffer = new PhpCodeSnifferConfig();
    private PhpCsFixerConfig csFixer = new PhpCsFixerConfig();
    private LaravelPintConfig laravelPint = new LaravelPintConfig();
    private PhpMessDetectorConfig messDetector = new PhpMessDetectorConfig();

    public static class CustomRuleset {
        private String name;
        private String file;

        public CustomRuleset() {
            this("", "");
        }

        public CustomRuleset(String name, String file) {
            this.name = name != null ? name : "";
            this.file = file != null ? file : "";
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name != null ? name : "";
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file != null ? file : "";
        }

        public CustomRuleset copy() {
            return new CustomRuleset(name, file);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CustomRuleset that)) return false;
            return Objects.equals(name, that.name) && Objects.equals(file, that.file);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, file);
        }
    }

    public static class PhpCodeSnifferConfig {
        private boolean inspectionEnabled = false;
        private String configurationName = "System PHP";
        private String phpcsPath = "";
        private String phpcbfPath = "";
        private String checkFilesWithExtensions = "php,js,css,inc";
        private boolean showWarningAs = true;
        private String warningSeverity = "Weak Warning";
        private boolean showSniffName = false;
        private boolean installedStandardsPathEnabled = false;
        private String installedStandardsPath = "";
        private String codingStandard = "PSR2";

        public PhpCodeSnifferConfig() {}

        public PhpCodeSnifferConfig(PhpCodeSnifferConfig other) {
            if (other == null) return;
            this.inspectionEnabled = other.inspectionEnabled;
            this.configurationName = other.configurationName;
            this.phpcsPath = other.phpcsPath;
            this.phpcbfPath = other.phpcbfPath;
            this.checkFilesWithExtensions = other.checkFilesWithExtensions;
            this.showWarningAs = other.showWarningAs;
            this.warningSeverity = other.warningSeverity;
            this.showSniffName = other.showSniffName;
            this.installedStandardsPathEnabled = other.installedStandardsPathEnabled;
            this.installedStandardsPath = other.installedStandardsPath;
            this.codingStandard = other.codingStandard;
        }

        public PhpCodeSnifferConfig copy() {
            return new PhpCodeSnifferConfig(this);
        }

        public boolean isInspectionEnabled() {
            return inspectionEnabled;
        }

        public void setInspectionEnabled(boolean inspectionEnabled) {
            this.inspectionEnabled = inspectionEnabled;
        }

        public String getConfigurationName() {
            return configurationName;
        }

        public void setConfigurationName(String configurationName) {
            this.configurationName = configurationName != null ? configurationName : "System PHP";
        }

        public String getPhpcsPath() {
            return phpcsPath;
        }

        public void setPhpcsPath(String phpcsPath) {
            this.phpcsPath = phpcsPath != null ? phpcsPath : "";
        }

        public String getPhpcbfPath() {
            return phpcbfPath;
        }

        public void setPhpcbfPath(String phpcbfPath) {
            this.phpcbfPath = phpcbfPath != null ? phpcbfPath : "";
        }

        public String getCheckFilesWithExtensions() {
            return checkFilesWithExtensions;
        }

        public void setCheckFilesWithExtensions(String checkFilesWithExtensions) {
            this.checkFilesWithExtensions = checkFilesWithExtensions != null ? checkFilesWithExtensions : "php,js,css,inc";
        }

        public boolean isShowWarningAs() {
            return showWarningAs;
        }

        public void setShowWarningAs(boolean showWarningAs) {
            this.showWarningAs = showWarningAs;
        }

        public String getWarningSeverity() {
            return warningSeverity;
        }

        public void setWarningSeverity(String warningSeverity) {
            this.warningSeverity = warningSeverity != null ? warningSeverity : "Weak Warning";
        }

        public boolean isShowSniffName() {
            return showSniffName;
        }

        public void setShowSniffName(boolean showSniffName) {
            this.showSniffName = showSniffName;
        }

        public boolean isInstalledStandardsPathEnabled() {
            return installedStandardsPathEnabled;
        }

        public void setInstalledStandardsPathEnabled(boolean installedStandardsPathEnabled) {
            this.installedStandardsPathEnabled = installedStandardsPathEnabled;
        }

        public String getInstalledStandardsPath() {
            return installedStandardsPath;
        }

        public void setInstalledStandardsPath(String installedStandardsPath) {
            this.installedStandardsPath = installedStandardsPath != null ? installedStandardsPath : "";
        }

        public String getCodingStandard() {
            return codingStandard;
        }

        public void setCodingStandard(String codingStandard) {
            this.codingStandard = codingStandard != null ? codingStandard : "PSR2";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PhpCodeSnifferConfig that)) return false;
            return inspectionEnabled == that.inspectionEnabled &&
                    showWarningAs == that.showWarningAs &&
                    showSniffName == that.showSniffName &&
                    installedStandardsPathEnabled == that.installedStandardsPathEnabled &&
                    Objects.equals(configurationName, that.configurationName) &&
                    Objects.equals(phpcsPath, that.phpcsPath) &&
                    Objects.equals(phpcbfPath, that.phpcbfPath) &&
                    Objects.equals(checkFilesWithExtensions, that.checkFilesWithExtensions) &&
                    Objects.equals(warningSeverity, that.warningSeverity) &&
                    Objects.equals(installedStandardsPath, that.installedStandardsPath) &&
                    Objects.equals(codingStandard, that.codingStandard);
        }

        @Override
        public int hashCode() {
            return Objects.hash(inspectionEnabled, configurationName, phpcsPath, phpcbfPath,
                    checkFilesWithExtensions, showWarningAs, warningSeverity, showSniffName,
                    installedStandardsPathEnabled, installedStandardsPath, codingStandard);
        }
    }

    public static class PhpCsFixerConfig {
        private boolean inspectionEnabled = false;
        private String configurationName = "System PHP";
        private String phpCsFixerPath = "";
        private boolean allowRiskyRules = false;
        private String ruleset = "PSR2";

        public PhpCsFixerConfig() {}

        public PhpCsFixerConfig(PhpCsFixerConfig other) {
            if (other == null) return;
            this.inspectionEnabled = other.inspectionEnabled;
            this.configurationName = other.configurationName;
            this.phpCsFixerPath = other.phpCsFixerPath;
            this.allowRiskyRules = other.allowRiskyRules;
            this.ruleset = other.ruleset;
        }

        public PhpCsFixerConfig copy() {
            return new PhpCsFixerConfig(this);
        }

        public boolean isInspectionEnabled() {
            return inspectionEnabled;
        }

        public void setInspectionEnabled(boolean inspectionEnabled) {
            this.inspectionEnabled = inspectionEnabled;
        }

        public String getConfigurationName() {
            return configurationName;
        }

        public void setConfigurationName(String configurationName) {
            this.configurationName = configurationName != null ? configurationName : "System PHP";
        }

        public String getPhpCsFixerPath() {
            return phpCsFixerPath;
        }

        public void setPhpCsFixerPath(String phpCsFixerPath) {
            this.phpCsFixerPath = phpCsFixerPath != null ? phpCsFixerPath : "";
        }

        public boolean isAllowRiskyRules() {
            return allowRiskyRules;
        }

        public void setAllowRiskyRules(boolean allowRiskyRules) {
            this.allowRiskyRules = allowRiskyRules;
        }

        public String getRuleset() {
            return ruleset;
        }

        public void setRuleset(String ruleset) {
            this.ruleset = ruleset != null ? ruleset : "PSR2";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PhpCsFixerConfig that)) return false;
            return inspectionEnabled == that.inspectionEnabled &&
                    allowRiskyRules == that.allowRiskyRules &&
                    Objects.equals(configurationName, that.configurationName) &&
                    Objects.equals(phpCsFixerPath, that.phpCsFixerPath) &&
                    Objects.equals(ruleset, that.ruleset);
        }

        @Override
        public int hashCode() {
            return Objects.hash(inspectionEnabled, configurationName, phpCsFixerPath, allowRiskyRules, ruleset);
        }
    }

    public static class LaravelPintConfig {
        private boolean inspectionEnabled = false;
        private String configurationName = "System PHP";
        private String pintPath = "";
        private boolean reformatOnlyUncommittedFiles = false;
        private String pathToPintJson = "";
        private String ruleset = "laravel";

        public LaravelPintConfig() {}

        public LaravelPintConfig(LaravelPintConfig other) {
            if (other == null) return;
            this.inspectionEnabled = other.inspectionEnabled;
            this.configurationName = other.configurationName;
            this.pintPath = other.pintPath;
            this.reformatOnlyUncommittedFiles = other.reformatOnlyUncommittedFiles;
            this.pathToPintJson = other.pathToPintJson;
            this.ruleset = other.ruleset;
        }

        public LaravelPintConfig copy() {
            return new LaravelPintConfig(this);
        }

        public boolean isInspectionEnabled() {
            return inspectionEnabled;
        }

        public void setInspectionEnabled(boolean inspectionEnabled) {
            this.inspectionEnabled = inspectionEnabled;
        }

        public String getConfigurationName() {
            return configurationName;
        }

        public void setConfigurationName(String configurationName) {
            this.configurationName = configurationName != null ? configurationName : "System PHP";
        }

        public String getPintPath() {
            return pintPath;
        }

        public void setPintPath(String pintPath) {
            this.pintPath = pintPath != null ? pintPath : "";
        }

        public boolean isReformatOnlyUncommittedFiles() {
            return reformatOnlyUncommittedFiles;
        }

        public void setReformatOnlyUncommittedFiles(boolean reformatOnlyUncommittedFiles) {
            this.reformatOnlyUncommittedFiles = reformatOnlyUncommittedFiles;
        }

        public String getPathToPintJson() {
            return pathToPintJson;
        }

        public void setPathToPintJson(String pathToPintJson) {
            this.pathToPintJson = pathToPintJson != null ? pathToPintJson : "";
        }

        public String getRuleset() {
            return ruleset;
        }

        public void setRuleset(String ruleset) {
            this.ruleset = ruleset != null ? ruleset : "laravel";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LaravelPintConfig that)) return false;
            return inspectionEnabled == that.inspectionEnabled &&
                    reformatOnlyUncommittedFiles == that.reformatOnlyUncommittedFiles &&
                    Objects.equals(configurationName, that.configurationName) &&
                    Objects.equals(pintPath, that.pintPath) &&
                    Objects.equals(pathToPintJson, that.pathToPintJson) &&
                    Objects.equals(ruleset, that.ruleset);
        }

        @Override
        public int hashCode() {
            return Objects.hash(inspectionEnabled, configurationName, pintPath, reformatOnlyUncommittedFiles, pathToPintJson, ruleset);
        }
    }

    public static class PhpMessDetectorConfig {
        private boolean inspectionEnabled = false;
        private String configurationName = "System PHP";
        private String phpmdPath = "";
        private boolean codeSizeRules = false;
        private boolean controversialRules = false;
        private boolean designRules = false;
        private boolean namingRules = false;
        private boolean unusedCodeRules = false;
        private List<CustomRuleset> customRulesets = new ArrayList<>();

        public PhpMessDetectorConfig() {}

        public PhpMessDetectorConfig(PhpMessDetectorConfig other) {
            if (other == null) return;
            this.inspectionEnabled = other.inspectionEnabled;
            this.configurationName = other.configurationName;
            this.phpmdPath = other.phpmdPath;
            this.codeSizeRules = other.codeSizeRules;
            this.controversialRules = other.controversialRules;
            this.designRules = other.designRules;
            this.namingRules = other.namingRules;
            this.unusedCodeRules = other.unusedCodeRules;
            this.customRulesets = new ArrayList<>();
            for (CustomRuleset cr : other.customRulesets) {
                this.customRulesets.add(cr.copy());
            }
        }

        public PhpMessDetectorConfig copy() {
            return new PhpMessDetectorConfig(this);
        }

        public boolean isInspectionEnabled() {
            return inspectionEnabled;
        }

        public void setInspectionEnabled(boolean inspectionEnabled) {
            this.inspectionEnabled = inspectionEnabled;
        }

        public String getConfigurationName() {
            return configurationName;
        }

        public void setConfigurationName(String configurationName) {
            this.configurationName = configurationName != null ? configurationName : "System PHP";
        }

        public String getPhpmdPath() {
            return phpmdPath;
        }

        public void setPhpmdPath(String phpmdPath) {
            this.phpmdPath = phpmdPath != null ? phpmdPath : "";
        }

        public boolean isCodeSizeRules() {
            return codeSizeRules;
        }

        public void setCodeSizeRules(boolean codeSizeRules) {
            this.codeSizeRules = codeSizeRules;
        }

        public boolean isControversialRules() {
            return controversialRules;
        }

        public void setControversialRules(boolean controversialRules) {
            this.controversialRules = controversialRules;
        }

        public boolean isDesignRules() {
            return designRules;
        }

        public void setDesignRules(boolean designRules) {
            this.designRules = designRules;
        }

        public boolean isNamingRules() {
            return namingRules;
        }

        public void setNamingRules(boolean namingRules) {
            this.namingRules = namingRules;
        }

        public boolean isUnusedCodeRules() {
            return unusedCodeRules;
        }

        public void setUnusedCodeRules(boolean unusedCodeRules) {
            this.unusedCodeRules = unusedCodeRules;
        }

        public List<CustomRuleset> getCustomRulesets() {
            return customRulesets;
        }

        public void setCustomRulesets(List<CustomRuleset> customRulesets) {
            this.customRulesets = new ArrayList<>();
            if (customRulesets != null) {
                for (CustomRuleset cr : customRulesets) {
                    this.customRulesets.add(cr.copy());
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PhpMessDetectorConfig that)) return false;
            return inspectionEnabled == that.inspectionEnabled &&
                    codeSizeRules == that.codeSizeRules &&
                    controversialRules == that.controversialRules &&
                    designRules == that.designRules &&
                    namingRules == that.namingRules &&
                    unusedCodeRules == that.unusedCodeRules &&
                    Objects.equals(configurationName, that.configurationName) &&
                    Objects.equals(phpmdPath, that.phpmdPath) &&
                    Objects.equals(customRulesets, that.customRulesets);
        }

        @Override
        public int hashCode() {
            return Objects.hash(inspectionEnabled, configurationName, phpmdPath, codeSizeRules,
                    controversialRules, designRules, namingRules, unusedCodeRules, customRulesets);
        }
    }

    public PhpQualityToolsSettings() {}

    public PhpQualityToolsSettings(PhpQualityToolsSettings other) {
        if (other == null) return;
        this.externalFormatter = other.externalFormatter;
        this.codeSniffer = other.codeSniffer != null ? other.codeSniffer.copy() : new PhpCodeSnifferConfig();
        this.csFixer = other.csFixer != null ? other.csFixer.copy() : new PhpCsFixerConfig();
        this.laravelPint = other.laravelPint != null ? other.laravelPint.copy() : new LaravelPintConfig();
        this.messDetector = other.messDetector != null ? other.messDetector.copy() : new PhpMessDetectorConfig();
    }

    public PhpQualityToolsSettings copy() {
        return new PhpQualityToolsSettings(this);
    }

    public String getExternalFormatter() {
        return externalFormatter;
    }

    public void setExternalFormatter(String externalFormatter) {
        this.externalFormatter = externalFormatter != null ? externalFormatter : "none";
    }

    public PhpCodeSnifferConfig getCodeSniffer() {
        return codeSniffer;
    }

    public void setCodeSniffer(PhpCodeSnifferConfig codeSniffer) {
        this.codeSniffer = codeSniffer != null ? codeSniffer.copy() : new PhpCodeSnifferConfig();
    }

    public PhpCsFixerConfig getCsFixer() {
        return csFixer;
    }

    public void setCsFixer(PhpCsFixerConfig csFixer) {
        this.csFixer = csFixer != null ? csFixer.copy() : new PhpCsFixerConfig();
    }

    public LaravelPintConfig getLaravelPint() {
        return laravelPint;
    }

    public void setLaravelPint(LaravelPintConfig laravelPint) {
        this.laravelPint = laravelPint != null ? laravelPint.copy() : new LaravelPintConfig();
    }

    public PhpMessDetectorConfig getMessDetector() {
        return messDetector;
    }

    public void setMessDetector(PhpMessDetectorConfig messDetector) {
        this.messDetector = messDetector != null ? messDetector.copy() : new PhpMessDetectorConfig();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhpQualityToolsSettings that)) return false;
        return Objects.equals(externalFormatter, that.externalFormatter) &&
                Objects.equals(codeSniffer, that.codeSniffer) &&
                Objects.equals(csFixer, that.csFixer) &&
                Objects.equals(laravelPint, that.laravelPint) &&
                Objects.equals(messDetector, that.messDetector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalFormatter, codeSniffer, csFixer, laravelPint, messDetector);
    }
}
