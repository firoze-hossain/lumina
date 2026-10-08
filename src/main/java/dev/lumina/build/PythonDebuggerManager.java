package dev.lumina.build;

import dev.lumina.util.Settings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service managing Python Debugger settings and runtime type caches in Lumina IDE.
 */
public class PythonDebuggerManager {

    public static final String KEY_ATTACH_SUBPROCESS = "python.debugger.attach.subprocess";
    public static final String KEY_COLLECT_RUN_TIME_TYPES = "python.debugger.collect.runtime.types";
    public static final String KEY_GEVENT_COMPATIBLE = "python.debugger.gevent.compatible";
    public static final String KEY_DROP_FAILED_TESTS = "python.debugger.drop.failed.tests";
    public static final String KEY_PYQT_COMPATIBLE = "python.debugger.pyqt.compatible";
    public static final String KEY_PYQT_BACKEND = "python.debugger.pyqt.backend";
    public static final String KEY_ATTACH_FILTER = "python.debugger.attach.process.filter";
    public static final String KEY_TIMEOUT_MS = "python.debugger.eval.timeout.ms";

    private static PythonDebuggerManager instance;

    private PythonDebuggerSettings settings = new PythonDebuggerSettings();
    private final List<String> availablePyQtBackends = new CopyOnWriteArrayList<>();
    private final List<Runnable> cacheClearListeners = new CopyOnWriteArrayList<>();

    private PythonDebuggerManager() {
        // Built-in backends matching Python Debugger specification
        availablePyQtBackends.add("Auto");
        availablePyQtBackends.add("PyQt4");
        availablePyQtBackends.add("PyQt5");
        availablePyQtBackends.add("PyQt6");
        availablePyQtBackends.add("PySide");
        availablePyQtBackends.add("PySide2");
        availablePyQtBackends.add("PySide6");

        loadSettings();
    }

    public static synchronized PythonDebuggerManager getInstance() {
        if (instance == null) {
            instance = new PythonDebuggerManager();
        }
        return instance;
    }

    public PythonDebuggerSettings getSettings() {
        return settings.clone();
    }

    public void setSettings(PythonDebuggerSettings newSettings) {
        if (newSettings == null) return;
        this.settings = newSettings.clone();
        saveSettings();
    }

    public List<String> getAvailablePyQtBackends() {
        return Collections.unmodifiableList(new ArrayList<>(availablePyQtBackends));
    }

    public void registerPyQtBackend(String backend) {
        if (backend != null && !backend.isBlank() && !availablePyQtBackends.contains(backend)) {
            availablePyQtBackends.add(backend);
        }
    }

    public void clearCaches() {
        for (Runnable listener : cacheClearListeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    public void addCacheClearListener(Runnable listener) {
        if (listener != null) {
            cacheClearListeners.add(listener);
        }
    }

    public void removeCacheClearListener(Runnable listener) {
        cacheClearListeners.remove(listener);
    }

    public void loadSettings() {
        String attach = Settings.get(KEY_ATTACH_SUBPROCESS);
        if (attach != null) settings.setAttachToSubprocess(Boolean.parseBoolean(attach));

        String collect = Settings.get(KEY_COLLECT_RUN_TIME_TYPES);
        if (collect != null) settings.setCollectRunTimeTypes(Boolean.parseBoolean(collect));

        String gevent = Settings.get(KEY_GEVENT_COMPATIBLE);
        if (gevent != null) settings.setGeventCompatible(Boolean.parseBoolean(gevent));

        String drop = Settings.get(KEY_DROP_FAILED_TESTS);
        if (drop != null) settings.setDropIntoDebuggerOnFailedTests(Boolean.parseBoolean(drop));

        String pyqt = Settings.get(KEY_PYQT_COMPATIBLE);
        if (pyqt != null) settings.setPyQtCompatible(Boolean.parseBoolean(pyqt));

        String backend = Settings.get(KEY_PYQT_BACKEND);
        if (backend != null && !backend.isBlank()) settings.setPyQtBackend(backend);

        String filter = Settings.get(KEY_ATTACH_FILTER);
        if (filter != null) settings.setAttachProcessFilter(filter);

        String timeout = Settings.get(KEY_TIMEOUT_MS);
        if (timeout != null) {
            try {
                settings.setEvalResponseTimeoutMs(Integer.parseInt(timeout));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public void saveSettings() {
        Settings.set(KEY_ATTACH_SUBPROCESS, String.valueOf(settings.isAttachToSubprocess()));
        Settings.set(KEY_COLLECT_RUN_TIME_TYPES, String.valueOf(settings.isCollectRunTimeTypes()));
        Settings.set(KEY_GEVENT_COMPATIBLE, String.valueOf(settings.isGeventCompatible()));
        Settings.set(KEY_DROP_FAILED_TESTS, String.valueOf(settings.isDropIntoDebuggerOnFailedTests()));
        Settings.set(KEY_PYQT_COMPATIBLE, String.valueOf(settings.isPyQtCompatible()));
        Settings.set(KEY_PYQT_BACKEND, settings.getPyQtBackend());
        Settings.set(KEY_ATTACH_FILTER, settings.getAttachProcessFilter());
        Settings.set(KEY_TIMEOUT_MS, String.valueOf(settings.getEvalResponseTimeoutMs()));
    }
}
