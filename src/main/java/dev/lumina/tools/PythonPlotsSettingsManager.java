package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager handling persistence and change listeners for Tools > Python Plots settings.
 */
public class PythonPlotsSettingsManager {

    private static final PythonPlotsSettingsManager INSTANCE = new PythonPlotsSettingsManager();

    private static final String KEY_PREFIX = "tools.python_plots.";
    private static final String KEY_SHOW_PLOTS = KEY_PREFIX + "show_plots_in_tool_window";
    private static final String KEY_USE_MPLD3 = KEY_PREFIX + "use_mpld3_interactive_plots";
    private static final String KEY_MAX_PLOTS_COUNT = KEY_PREFIX + "max_plots_count";
    private static final String KEY_SUGGEST_KALEIDO = KEY_PREFIX + "suggest_install_kaleido";
    private static final String KEY_SUGGEST_MPLD3 = KEY_PREFIX + "suggest_install_mpld3";
    private static final String KEY_SUGGEST_PILLOW = KEY_PREFIX + "suggest_install_pillow";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private PythonPlotsSettingsManager() {
    }

    public static PythonPlotsSettingsManager getInstance() {
        return INSTANCE;
    }

    public PythonPlotsSettings load() {
        return getSettings();
    }

    public PythonPlotsSettings getSettings() {
        PythonPlotsSettings s = new PythonPlotsSettings();

        String val = Settings.get(KEY_SHOW_PLOTS);
        if (val != null) s.setShowPlotsInToolWindow(Boolean.parseBoolean(val));

        val = Settings.get(KEY_USE_MPLD3);
        if (val != null) s.setUseMpld3InteractivePlots(Boolean.parseBoolean(val));

        val = Settings.get(KEY_MAX_PLOTS_COUNT);
        if (val != null) {
            try {
                s.setMaxPlotsCount(Integer.parseInt(val));
            } catch (NumberFormatException ignored) {
            }
        }

        val = Settings.get(KEY_SUGGEST_KALEIDO);
        if (val != null) s.setSuggestInstallKaleido(Boolean.parseBoolean(val));

        val = Settings.get(KEY_SUGGEST_MPLD3);
        if (val != null) s.setSuggestInstallMpld3(Boolean.parseBoolean(val));

        val = Settings.get(KEY_SUGGEST_PILLOW);
        if (val != null) s.setSuggestInstallPillow(Boolean.parseBoolean(val));

        return s;
    }

    public void setSettings(PythonPlotsSettings s) {
        if (s == null) return;

        Settings.put(KEY_SHOW_PLOTS, String.valueOf(s.isShowPlotsInToolWindow()));
        Settings.put(KEY_USE_MPLD3, String.valueOf(s.isUseMpld3InteractivePlots()));
        Settings.put(KEY_MAX_PLOTS_COUNT, String.valueOf(s.getMaxPlotsCount()));
        Settings.put(KEY_SUGGEST_KALEIDO, String.valueOf(s.isSuggestInstallKaleido()));
        Settings.put(KEY_SUGGEST_MPLD3, String.valueOf(s.isSuggestInstallMpld3()));
        Settings.put(KEY_SUGGEST_PILLOW, String.valueOf(s.isSuggestInstallPillow()));

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
