package dev.lumina.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration model for Build, Execution, Deployment > Coverage settings in Lumina IDE.
 * Matches 1:1 with reference screenshot media_1791450052884_14c89804.png:
 *  - When new coverage is gathered:
 *    - Show options before applying coverage to the editor
 *    - Do not apply collected coverage
 *    - Replace active suites with the new one
 *    - Add to the active suites
 *    - Activate Coverage View
 *    - Show coverage in the project view
 *  - Python coverage:
 *    - Use bundled coverage.py
 *    - Branch coverage
 *  - Java Coverage:
 *    - Choose coverage runner (Lumina / JaCoCo)
 *    - Branch coverage
 *    - Track per test coverage
 *    - Collect coverage in test folders
 *    - Ignore implicitly declared default constructors
 *    - Exclude annotations
 */
public class CoverageSettings implements Cloneable {

    public enum GatherPolicy {
        SHOW_OPTIONS("Show options before applying coverage to the editor"),
        DO_NOT_APPLY("Do not apply collected coverage"),
        REPLACE_ACTIVE_SUITES("Replace active suites with the new one"),
        ADD_TO_ACTIVE_SUITES("Add to the active suites");

        private final String label;

        GatherPolicy(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public static final String RUNNER_LUMINA = "Lumina";
    public static final String RUNNER_JACOCO = "JaCoCo";

    private GatherPolicy gatherPolicy = GatherPolicy.SHOW_OPTIONS;
    private boolean activateCoverageView = true;
    private boolean showCoverageInProjectView = true;

    private boolean pythonUseBundledCoverage = false;
    private boolean pythonBranchCoverage = false;

    private String javaCoverageRunner = RUNNER_LUMINA;
    private boolean javaBranchCoverage = true;
    private boolean javaTrackPerTestCoverage = false;
    private boolean javaCollectInTestFolders = false;
    private boolean javaIgnoreDefaultConstructors = true;
    private List<String> excludeAnnotations = new ArrayList<>(List.of("*Generated*"));

    public CoverageSettings() {
    }

    public CoverageSettings(CoverageSettings other) {
        if (other != null) {
            this.gatherPolicy = other.gatherPolicy;
            this.activateCoverageView = other.activateCoverageView;
            this.showCoverageInProjectView = other.showCoverageInProjectView;
            this.pythonUseBundledCoverage = other.pythonUseBundledCoverage;
            this.pythonBranchCoverage = other.pythonBranchCoverage;
            this.javaCoverageRunner = other.javaCoverageRunner;
            this.javaBranchCoverage = other.javaBranchCoverage;
            this.javaTrackPerTestCoverage = other.javaTrackPerTestCoverage;
            this.javaCollectInTestFolders = other.javaCollectInTestFolders;
            this.javaIgnoreDefaultConstructors = other.javaIgnoreDefaultConstructors;
            this.excludeAnnotations = new ArrayList<>(other.excludeAnnotations != null ? other.excludeAnnotations : List.of("*Generated*"));
        }
    }

    public static List<String> getAvailableRunners() {
        return List.of(RUNNER_LUMINA, RUNNER_JACOCO);
    }

    public GatherPolicy getGatherPolicy() { return gatherPolicy; }
    public void setGatherPolicy(GatherPolicy gatherPolicy) { this.gatherPolicy = gatherPolicy != null ? gatherPolicy : GatherPolicy.SHOW_OPTIONS; }

    public boolean isActivateCoverageView() { return activateCoverageView; }
    public void setActivateCoverageView(boolean activateCoverageView) { this.activateCoverageView = activateCoverageView; }

    public boolean isShowCoverageInProjectView() { return showCoverageInProjectView; }
    public void setShowCoverageInProjectView(boolean showCoverageInProjectView) { this.showCoverageInProjectView = showCoverageInProjectView; }

    public boolean isPythonUseBundledCoverage() { return pythonUseBundledCoverage; }
    public void setPythonUseBundledCoverage(boolean pythonUseBundledCoverage) { this.pythonUseBundledCoverage = pythonUseBundledCoverage; }

    public boolean isPythonBranchCoverage() { return pythonBranchCoverage; }
    public void setPythonBranchCoverage(boolean pythonBranchCoverage) { this.pythonBranchCoverage = pythonBranchCoverage; }

    public String getJavaCoverageRunner() { return javaCoverageRunner; }
    public void setJavaCoverageRunner(String javaCoverageRunner) { this.javaCoverageRunner = javaCoverageRunner != null ? javaCoverageRunner : RUNNER_LUMINA; }

    public boolean isJavaBranchCoverage() { return javaBranchCoverage; }
    public void setJavaBranchCoverage(boolean javaBranchCoverage) { this.javaBranchCoverage = javaBranchCoverage; }

    public boolean isJavaTrackPerTestCoverage() { return javaTrackPerTestCoverage; }
    public void setJavaTrackPerTestCoverage(boolean javaTrackPerTestCoverage) { this.javaTrackPerTestCoverage = javaTrackPerTestCoverage; }

    public boolean isJavaCollectInTestFolders() { return javaCollectInTestFolders; }
    public void setJavaCollectInTestFolders(boolean javaCollectInTestFolders) { this.javaCollectInTestFolders = javaCollectInTestFolders; }

    public boolean isJavaIgnoreDefaultConstructors() { return javaIgnoreDefaultConstructors; }
    public void setJavaIgnoreDefaultConstructors(boolean javaIgnoreDefaultConstructors) { this.javaIgnoreDefaultConstructors = javaIgnoreDefaultConstructors; }

    public List<String> getExcludeAnnotations() { return excludeAnnotations; }
    public void setExcludeAnnotations(List<String> excludeAnnotations) {
        this.excludeAnnotations = excludeAnnotations != null ? new ArrayList<>(excludeAnnotations) : new ArrayList<>(List.of("*Generated*"));
    }

    @Override
    public CoverageSettings clone() {
        return new CoverageSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoverageSettings that = (CoverageSettings) o;
        return activateCoverageView == that.activateCoverageView &&
                showCoverageInProjectView == that.showCoverageInProjectView &&
                pythonUseBundledCoverage == that.pythonUseBundledCoverage &&
                pythonBranchCoverage == that.pythonBranchCoverage &&
                javaBranchCoverage == that.javaBranchCoverage &&
                javaTrackPerTestCoverage == that.javaTrackPerTestCoverage &&
                javaCollectInTestFolders == that.javaCollectInTestFolders &&
                javaIgnoreDefaultConstructors == that.javaIgnoreDefaultConstructors &&
                gatherPolicy == that.gatherPolicy &&
                Objects.equals(javaCoverageRunner, that.javaCoverageRunner) &&
                Objects.equals(excludeAnnotations, that.excludeAnnotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gatherPolicy, activateCoverageView, showCoverageInProjectView,
                pythonUseBundledCoverage, pythonBranchCoverage, javaCoverageRunner,
                javaBranchCoverage, javaTrackPerTestCoverage, javaCollectInTestFolders,
                javaIgnoreDefaultConstructors, excludeAnnotations);
    }
}
