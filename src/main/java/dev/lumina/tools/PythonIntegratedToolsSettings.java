package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Python Integrated Tools configuration settings in Lumina IDE.
 */
public class PythonIntegratedToolsSettings implements Cloneable {

    public static final String DEFAULT_TEST_RUNNER = "Autodetect";
    public static final String DEFAULT_DOCSTRING_FORMAT = "reStructuredText";

    // Packaging
    private String packageRequirementsFile = "";

    // Pipenv
    private String pipenvExecutablePath = "";

    // Testing
    private String defaultTestRunner = DEFAULT_TEST_RUNNER;
    private boolean detectTestsInJupyterNotebooks = false;

    // Docstrings
    private String docstringFormat = DEFAULT_DOCSTRING_FORMAT;
    private boolean analyzePythonCodeInDocstrings = true;
    private boolean renderExternalDocumentationForStdlib = false;

    // reStructuredText
    private String sphinxWorkingDirectory = "";
    private boolean treatTxtFilesAsReStructuredText = false;

    public PythonIntegratedToolsSettings() {
    }

    public String getPackageRequirementsFile() {
        return packageRequirementsFile;
    }

    public void setPackageRequirementsFile(String packageRequirementsFile) {
        this.packageRequirementsFile = packageRequirementsFile != null ? packageRequirementsFile : "";
    }

    public String getPipenvExecutablePath() {
        return pipenvExecutablePath;
    }

    public void setPipenvExecutablePath(String pipenvExecutablePath) {
        this.pipenvExecutablePath = pipenvExecutablePath != null ? pipenvExecutablePath : "";
    }

    public String getDefaultTestRunner() {
        return defaultTestRunner;
    }

    public void setDefaultTestRunner(String defaultTestRunner) {
        this.defaultTestRunner = defaultTestRunner != null ? defaultTestRunner : DEFAULT_TEST_RUNNER;
    }

    public boolean isDetectTestsInJupyterNotebooks() {
        return detectTestsInJupyterNotebooks;
    }

    public void setDetectTestsInJupyterNotebooks(boolean detectTestsInJupyterNotebooks) {
        this.detectTestsInJupyterNotebooks = detectTestsInJupyterNotebooks;
    }

    public String getDocstringFormat() {
        return docstringFormat;
    }

    public void setDocstringFormat(String docstringFormat) {
        this.docstringFormat = docstringFormat != null ? docstringFormat : DEFAULT_DOCSTRING_FORMAT;
    }

    public boolean isAnalyzePythonCodeInDocstrings() {
        return analyzePythonCodeInDocstrings;
    }

    public void setAnalyzePythonCodeInDocstrings(boolean analyzePythonCodeInDocstrings) {
        this.analyzePythonCodeInDocstrings = analyzePythonCodeInDocstrings;
    }

    public boolean isRenderExternalDocumentationForStdlib() {
        return renderExternalDocumentationForStdlib;
    }

    public void setRenderExternalDocumentationForStdlib(boolean renderExternalDocumentationForStdlib) {
        this.renderExternalDocumentationForStdlib = renderExternalDocumentationForStdlib;
    }

    public String getSphinxWorkingDirectory() {
        return sphinxWorkingDirectory;
    }

    public void setSphinxWorkingDirectory(String sphinxWorkingDirectory) {
        this.sphinxWorkingDirectory = sphinxWorkingDirectory != null ? sphinxWorkingDirectory : "";
    }

    public boolean isTreatTxtFilesAsReStructuredText() {
        return treatTxtFilesAsReStructuredText;
    }

    public void setTreatTxtFilesAsReStructuredText(boolean treatTxtFilesAsReStructuredText) {
        this.treatTxtFilesAsReStructuredText = treatTxtFilesAsReStructuredText;
    }

    public PythonIntegratedToolsSettings copy() {
        return clone();
    }

    @Override
    public PythonIntegratedToolsSettings clone() {
        try {
            return (PythonIntegratedToolsSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            PythonIntegratedToolsSettings copy = new PythonIntegratedToolsSettings();
            copy.packageRequirementsFile = this.packageRequirementsFile;
            copy.pipenvExecutablePath = this.pipenvExecutablePath;
            copy.defaultTestRunner = this.defaultTestRunner;
            copy.detectTestsInJupyterNotebooks = this.detectTestsInJupyterNotebooks;
            copy.docstringFormat = this.docstringFormat;
            copy.analyzePythonCodeInDocstrings = this.analyzePythonCodeInDocstrings;
            copy.renderExternalDocumentationForStdlib = this.renderExternalDocumentationForStdlib;
            copy.sphinxWorkingDirectory = this.sphinxWorkingDirectory;
            copy.treatTxtFilesAsReStructuredText = this.treatTxtFilesAsReStructuredText;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PythonIntegratedToolsSettings that = (PythonIntegratedToolsSettings) o;
        return detectTestsInJupyterNotebooks == that.detectTestsInJupyterNotebooks &&
                analyzePythonCodeInDocstrings == that.analyzePythonCodeInDocstrings &&
                renderExternalDocumentationForStdlib == that.renderExternalDocumentationForStdlib &&
                treatTxtFilesAsReStructuredText == that.treatTxtFilesAsReStructuredText &&
                Objects.equals(packageRequirementsFile, that.packageRequirementsFile) &&
                Objects.equals(pipenvExecutablePath, that.pipenvExecutablePath) &&
                Objects.equals(defaultTestRunner, that.defaultTestRunner) &&
                Objects.equals(docstringFormat, that.docstringFormat) &&
                Objects.equals(sphinxWorkingDirectory, that.sphinxWorkingDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageRequirementsFile, pipenvExecutablePath, defaultTestRunner,
                detectTestsInJupyterNotebooks, docstringFormat, analyzePythonCodeInDocstrings,
                renderExternalDocumentationForStdlib, sphinxWorkingDirectory, treatTxtFilesAsReStructuredText);
    }
}
