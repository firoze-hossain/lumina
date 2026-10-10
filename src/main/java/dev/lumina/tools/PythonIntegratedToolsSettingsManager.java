package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager handling persistence and change listeners for Tools > Python Integrated Tools settings.
 */
public class PythonIntegratedToolsSettingsManager {

    private static final PythonIntegratedToolsSettingsManager INSTANCE = new PythonIntegratedToolsSettingsManager();

    private static final String KEY_PREFIX = "tools.python_integrated.";
    private static final String KEY_PACKAGE_REQ_FILE = KEY_PREFIX + "package_requirements_file";
    private static final String KEY_PIPENV_EXEC_PATH = KEY_PREFIX + "pipenv_executable_path";
    private static final String KEY_DEFAULT_TEST_RUNNER = KEY_PREFIX + "default_test_runner";
    private static final String KEY_DETECT_TESTS_JUPYTER = KEY_PREFIX + "detect_tests_in_jupyter";
    private static final String KEY_DOCSTRING_FORMAT = KEY_PREFIX + "docstring_format";
    private static final String KEY_ANALYZE_DOCSTRINGS = KEY_PREFIX + "analyze_code_in_docstrings";
    private static final String KEY_RENDER_EXT_DOC_STDLIB = KEY_PREFIX + "render_ext_doc_stdlib";
    private static final String KEY_SPHINX_WORKING_DIR = KEY_PREFIX + "sphinx_working_dir";
    private static final String KEY_TREAT_TXT_AS_RST = KEY_PREFIX + "treat_txt_as_rst";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private PythonIntegratedToolsSettingsManager() {
    }

    public static PythonIntegratedToolsSettingsManager getInstance() {
        return INSTANCE;
    }

    public PythonIntegratedToolsSettings load() {
        return getSettings();
    }

    public PythonIntegratedToolsSettings getSettings() {
        PythonIntegratedToolsSettings s = new PythonIntegratedToolsSettings();

        String val = Settings.get(KEY_PACKAGE_REQ_FILE);
        if (val != null) s.setPackageRequirementsFile(val);

        val = Settings.get(KEY_PIPENV_EXEC_PATH);
        if (val != null) s.setPipenvExecutablePath(val);

        val = Settings.get(KEY_DEFAULT_TEST_RUNNER);
        if (val != null) s.setDefaultTestRunner(val);

        val = Settings.get(KEY_DETECT_TESTS_JUPYTER);
        if (val != null) s.setDetectTestsInJupyterNotebooks(Boolean.parseBoolean(val));

        val = Settings.get(KEY_DOCSTRING_FORMAT);
        if (val != null) s.setDocstringFormat(val);

        val = Settings.get(KEY_ANALYZE_DOCSTRINGS);
        if (val != null) s.setAnalyzePythonCodeInDocstrings(Boolean.parseBoolean(val));

        val = Settings.get(KEY_RENDER_EXT_DOC_STDLIB);
        if (val != null) s.setRenderExternalDocumentationForStdlib(Boolean.parseBoolean(val));

        val = Settings.get(KEY_SPHINX_WORKING_DIR);
        if (val != null) s.setSphinxWorkingDirectory(val);

        val = Settings.get(KEY_TREAT_TXT_AS_RST);
        if (val != null) s.setTreatTxtFilesAsReStructuredText(Boolean.parseBoolean(val));

        return s;
    }

    public void setSettings(PythonIntegratedToolsSettings s) {
        if (s == null) return;

        Settings.put(KEY_PACKAGE_REQ_FILE, s.getPackageRequirementsFile());
        Settings.put(KEY_PIPENV_EXEC_PATH, s.getPipenvExecutablePath());
        Settings.put(KEY_DEFAULT_TEST_RUNNER, s.getDefaultTestRunner());
        Settings.put(KEY_DETECT_TESTS_JUPYTER, String.valueOf(s.isDetectTestsInJupyterNotebooks()));
        Settings.put(KEY_DOCSTRING_FORMAT, s.getDocstringFormat());
        Settings.put(KEY_ANALYZE_DOCSTRINGS, String.valueOf(s.isAnalyzePythonCodeInDocstrings()));
        Settings.put(KEY_RENDER_EXT_DOC_STDLIB, String.valueOf(s.isRenderExternalDocumentationForStdlib()));
        Settings.put(KEY_SPHINX_WORKING_DIR, s.getSphinxWorkingDirectory());
        Settings.put(KEY_TREAT_TXT_AS_RST, String.valueOf(s.isTreatTxtFilesAsReStructuredText()));

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        if (listener != null) listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
