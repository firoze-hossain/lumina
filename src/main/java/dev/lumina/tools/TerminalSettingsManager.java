package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Tools > Terminal settings in Lumina IDE.
 * Backed by Settings persistence and synchronized with terminal execution engine.
 */
public class TerminalSettingsManager {

    private static final String KEY_ENGINE = "tools.terminal.engine";
    private static final String KEY_SHOW_COMPLETION = "tools.terminal.show_completion";
    private static final String KEY_COMPLETION_MODE = "tools.terminal.completion_mode";
    private static final String KEY_COMPLETION_SHORTCUT = "tools.terminal.completion_shortcut";
    private static final String KEY_INSERT_SHORTCUT = "tools.terminal.insert_shortcut";
    private static final String KEY_START_DIR = "tools.terminal.start_dir";
    private static final String KEY_ENV_VARS = "tools.terminal.env_vars";
    private static final String KEY_FONT_FAMILY = "tools.terminal.font_family";
    private static final String KEY_FALLBACK_FONT = "tools.terminal.fallback_font";
    private static final String KEY_FONT_SIZE = "tools.terminal.font_size";
    private static final String KEY_LINE_HEIGHT = "tools.terminal.line_height";
    private static final String KEY_COLUMN_WIDTH = "tools.terminal.column_width";
    private static final String KEY_SHELL_PATH = "tools.terminal.shell_path";
    private static final String KEY_TAB_NAME = "tools.terminal.tab_name";
    private static final String KEY_ENFORCE_CONTRAST = "tools.terminal.enforce_contrast";
    private static final String KEY_CONTRAST_RATIO = "tools.terminal.contrast_ratio";
    private static final String KEY_SEPARATORS = "tools.terminal.separators";
    private static final String KEY_AUDIBLE_BELL = "tools.terminal.audible_bell";
    private static final String KEY_CLOSE_SESSION = "tools.terminal.close_session";
    private static final String KEY_MOUSE_REPORTING = "tools.terminal.mouse_reporting";
    private static final String KEY_MOVE_FOCUS_ESC = "tools.terminal.move_focus_escape";
    private static final String KEY_PASTE_MIDDLE_CLICK = "tools.terminal.paste_middle_click";
    private static final String KEY_OVERRIDE_SHORTCUTS = "tools.terminal.override_shortcuts";
    private static final String KEY_SHELL_INTEGRATION = "tools.terminal.shell_integration";
    private static final String KEY_HIGHLIGHT_HYPERLINKS = "tools.terminal.highlight_hyperlinks";
    private static final String KEY_ACTIVATE_VENV = "tools.terminal.activate_venv";
    private static final String KEY_ADD_PHP_TO_PATH = "tools.terminal.add_php_to_path";
    private static final String KEY_CURSOR_SHAPE = "tools.terminal.cursor_shape";

    private static final TerminalSettingsManager INSTANCE = new TerminalSettingsManager();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private TerminalSettingsManager() {
    }

    public static TerminalSettingsManager getInstance() {
        return INSTANCE;
    }

    public TerminalSettings load() {
        return getSettings();
    }

    public TerminalSettings getSettings() {
        TerminalSettings s = new TerminalSettings();

        String eng = Settings.get(KEY_ENGINE);
        if (eng != null) s.setTerminalEngine(eng);

        String sc = Settings.get(KEY_SHOW_COMPLETION);
        if (sc != null) s.setShowCompletionPopup(Boolean.parseBoolean(sc));

        String cm = Settings.get(KEY_COMPLETION_MODE);
        if (cm != null) s.setCompletionPopupMode(cm);

        String cs = Settings.get(KEY_COMPLETION_SHORTCUT);
        if (cs != null) s.setShowCompletionPopupShortcut(cs);

        String is = Settings.get(KEY_INSERT_SHORTCUT);
        if (is != null) s.setInsertSuggestionShortcut(is);

        // Start directory (fallback to Settings.TERMINAL_START_DIR or user.dir)
        String sd = Settings.get(KEY_START_DIR);
        if (sd == null) sd = Settings.get(Settings.TERMINAL_START_DIR);
        if (sd == null || sd.isBlank()) sd = System.getProperty("user.dir", "");
        s.setStartDirectory(sd);

        String ev = Settings.get(KEY_ENV_VARS);
        if (ev != null) s.setEnvironmentVariables(ev);

        String ff = Settings.get(KEY_FONT_FAMILY);
        if (ff != null) s.setFontFamily(ff);

        String fbf = Settings.get(KEY_FALLBACK_FONT);
        if (fbf != null) s.setFallbackFontFamily(fbf);

        // Font size (fallback to Settings.TERMINAL_FONT_SIZE)
        String fs = Settings.get(KEY_FONT_SIZE);
        if (fs == null) fs = Settings.get(Settings.TERMINAL_FONT_SIZE);
        if (fs != null) {
            try { s.setFontSize(Double.parseDouble(fs)); } catch (NumberFormatException ignored) {}
        }

        String lh = Settings.get(KEY_LINE_HEIGHT);
        if (lh != null) {
            try { s.setLineHeight(Double.parseDouble(lh)); } catch (NumberFormatException ignored) {}
        }

        String cw = Settings.get(KEY_COLUMN_WIDTH);
        if (cw != null) {
            try { s.setColumnWidth(Double.parseDouble(cw)); } catch (NumberFormatException ignored) {}
        }

        // Shell path (fallback to Settings.TERMINAL_SHELL_PATH or auto-detect)
        String sp = Settings.get(KEY_SHELL_PATH);
        if (sp == null) sp = Settings.get(Settings.TERMINAL_SHELL_PATH);
        if (sp == null || sp.isBlank()) {
            String envShell = System.getenv("SHELL");
            sp = (envShell != null && !envShell.isBlank()) ? envShell : "/bin/bash";
        }
        s.setShellPath(sp);

        // Tab name (fallback to Settings.TERMINAL_TAB_NAME)
        String tn = Settings.get(KEY_TAB_NAME);
        if (tn == null) tn = Settings.get(Settings.TERMINAL_TAB_NAME);
        if (tn != null) s.setDefaultTabName(tn);

        String emc = Settings.get(KEY_ENFORCE_CONTRAST);
        if (emc != null) s.setEnforceMinimumContrastRatio(Boolean.parseBoolean(emc));

        String cr = Settings.get(KEY_CONTRAST_RATIO);
        if (cr != null) {
            try { s.setContrastRatio(Double.parseDouble(cr)); } catch (NumberFormatException ignored) {}
        }

        String sep = Settings.get(KEY_SEPARATORS);
        if (sep != null) s.setShowSeparatorsBetweenExecutedCommands(Boolean.parseBoolean(sep));

        String bell = Settings.get(KEY_AUDIBLE_BELL);
        if (bell != null) s.setAudibleBell(Boolean.parseBoolean(bell));

        String csess = Settings.get(KEY_CLOSE_SESSION);
        if (csess != null) s.setCloseSessionWhenItEnds(Boolean.parseBoolean(csess));

        String mr = Settings.get(KEY_MOUSE_REPORTING);
        if (mr != null) s.setMouseReporting(Boolean.parseBoolean(mr));

        String mfe = Settings.get(KEY_MOVE_FOCUS_ESC);
        if (mfe != null) s.setMoveFocusToEditorWithEscape(Boolean.parseBoolean(mfe));

        String pmm = Settings.get(KEY_PASTE_MIDDLE_CLICK);
        if (pmm != null) s.setPasteOnMiddleMouseButtonClick(Boolean.parseBoolean(pmm));

        String ois = Settings.get(KEY_OVERRIDE_SHORTCUTS);
        if (ois != null) s.setOverrideIdeShortcuts(Boolean.parseBoolean(ois));

        String si = Settings.get(KEY_SHELL_INTEGRATION);
        if (si != null) s.setShellIntegration(Boolean.parseBoolean(si));

        String hl = Settings.get(KEY_HIGHLIGHT_HYPERLINKS);
        if (hl != null) s.setHighlightHyperlinks(Boolean.parseBoolean(hl));

        String av = Settings.get(KEY_ACTIVATE_VENV);
        if (av != null) s.setActivateVirtualenv(Boolean.parseBoolean(av));

        String php = Settings.get(KEY_ADD_PHP_TO_PATH);
        if (php != null) s.setAddDefaultPhpInterpreterToPath(Boolean.parseBoolean(php));

        // Cursor shape (fallback to Settings.TERMINAL_CURSOR_SHAPE)
        String cshape = Settings.get(KEY_CURSOR_SHAPE);
        if (cshape == null) cshape = Settings.get(Settings.TERMINAL_CURSOR_SHAPE);
        if (cshape != null) s.setCursorShape(cshape);

        return s;
    }

    public void setSettings(TerminalSettings s) {
        if (s == null) return;

        Settings.put(KEY_ENGINE, s.getTerminalEngine());
        Settings.put(KEY_SHOW_COMPLETION, String.valueOf(s.isShowCompletionPopup()));
        Settings.put(KEY_COMPLETION_MODE, s.getCompletionPopupMode());
        Settings.put(KEY_COMPLETION_SHORTCUT, s.getShowCompletionPopupShortcut());
        Settings.put(KEY_INSERT_SHORTCUT, s.getInsertSuggestionShortcut());
        Settings.put(KEY_START_DIR, s.getStartDirectory());
        Settings.put(KEY_ENV_VARS, s.getEnvironmentVariables());
        Settings.put(KEY_FONT_FAMILY, s.getFontFamily());
        Settings.put(KEY_FALLBACK_FONT, s.getFallbackFontFamily());
        Settings.put(KEY_FONT_SIZE, String.valueOf(s.getFontSize()));
        Settings.put(KEY_LINE_HEIGHT, String.valueOf(s.getLineHeight()));
        Settings.put(KEY_COLUMN_WIDTH, String.valueOf(s.getColumnWidth()));
        Settings.put(KEY_SHELL_PATH, s.getShellPath());
        Settings.put(KEY_TAB_NAME, s.getDefaultTabName());
        Settings.put(KEY_ENFORCE_CONTRAST, String.valueOf(s.isEnforceMinimumContrastRatio()));
        Settings.put(KEY_CONTRAST_RATIO, String.valueOf(s.getContrastRatio()));
        Settings.put(KEY_SEPARATORS, String.valueOf(s.isShowSeparatorsBetweenExecutedCommands()));
        Settings.put(KEY_AUDIBLE_BELL, String.valueOf(s.isAudibleBell()));
        Settings.put(KEY_CLOSE_SESSION, String.valueOf(s.isCloseSessionWhenItEnds()));
        Settings.put(KEY_MOUSE_REPORTING, String.valueOf(s.isMouseReporting()));
        Settings.put(KEY_MOVE_FOCUS_ESC, String.valueOf(s.isMoveFocusToEditorWithEscape()));
        Settings.put(KEY_PASTE_MIDDLE_CLICK, String.valueOf(s.isPasteOnMiddleMouseButtonClick()));
        Settings.put(KEY_OVERRIDE_SHORTCUTS, String.valueOf(s.isOverrideIdeShortcuts()));
        Settings.put(KEY_SHELL_INTEGRATION, String.valueOf(s.isShellIntegration()));
        Settings.put(KEY_HIGHLIGHT_HYPERLINKS, String.valueOf(s.isHighlightHyperlinks()));
        Settings.put(KEY_ACTIVATE_VENV, String.valueOf(s.isActivateVirtualenv()));
        Settings.put(KEY_ADD_PHP_TO_PATH, String.valueOf(s.isAddDefaultPhpInterpreterToPath()));
        Settings.put(KEY_CURSOR_SHAPE, s.getCursorShape());

        // Cross-sync with legacy keys for active terminal session interoperability
        Settings.put(Settings.TERMINAL_CURSOR_SHAPE, s.getCursorShape());
        Settings.put(Settings.TERMINAL_FONT_SIZE, String.valueOf(s.getFontSize()));
        Settings.put(Settings.TERMINAL_START_DIR, s.getStartDirectory());
        Settings.put(Settings.TERMINAL_SHELL_PATH, s.getShellPath());
        Settings.put(Settings.TERMINAL_TAB_NAME, s.getDefaultTabName());

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
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
