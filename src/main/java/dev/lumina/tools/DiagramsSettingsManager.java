package dev.lumina.tools;

import dev.lumina.util.Settings;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Tools > Diagrams settings in Lumina IDE.
 */
public class DiagramsSettingsManager {

    private static final String KEY_PREFIX = "tools.diagrams.";
    private static final String KEY_DEFAULT_SCOPE = KEY_PREFIX + "default_scope";
    private static final String KEY_NODE_ITEM_STYLE = KEY_PREFIX + "node_item_style";
    private static final String KEY_SHORTEN_LEN = KEY_PREFIX + "shorten_length";
    private static final String KEY_SHOW_GRID = KEY_PREFIX + "show_grid";
    private static final String KEY_ANIMATIONS = KEY_PREFIX + "enable_animations";
    private static final String KEY_SYNTAX = KEY_PREFIX + "syntax_highlighting";
    private static final String KEY_DEFAULT_LAYOUT = KEY_PREFIX + "default_layout";
    private static final String KEY_LAYOUT_SWITCH = KEY_PREFIX + "layout_on_switch";
    private static final String KEY_ANIM_DURATION = KEY_PREFIX + "animation_duration";
    private static final String KEY_FIT_CONTENT = KEY_PREFIX + "fit_content";
    private static final String KEY_RELAYOUT_NEW = KEY_PREFIX + "relayout_new_elements";
    private static final String KEY_CAT_PREFIX = KEY_PREFIX + "cat.";

    private static volatile DiagramsSettingsManager instance;

    private DiagramsSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DiagramsSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DiagramsSettingsManager getInstance() {
        if (instance == null) {
            synchronized (DiagramsSettingsManager.class) {
                if (instance == null) {
                    instance = new DiagramsSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DiagramsSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DiagramsSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DiagramsSettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    private DiagramsSettings loadSettings() {
        DiagramsSettings s = new DiagramsSettings();

        String scope = Settings.get(KEY_DEFAULT_SCOPE);
        if (scope != null) s.setDefaultScope(scope);

        String style = Settings.get(KEY_NODE_ITEM_STYLE);
        if (style != null) s.setNodeItemStyle(style);

        String shorten = Settings.get(KEY_SHORTEN_LEN);
        if (shorten != null) {
            try {
                s.setShortenNodeItemsLength(Integer.parseInt(shorten));
            } catch (NumberFormatException ignored) {}
        }

        String grid = Settings.get(KEY_SHOW_GRID);
        if (grid != null) s.setShowGridByDefault(Boolean.parseBoolean(grid));

        String anim = Settings.get(KEY_ANIMATIONS);
        if (anim != null) s.setEnableAnimations(Boolean.parseBoolean(anim));

        String syntax = Settings.get(KEY_SYNTAX);
        if (syntax != null) s.setEnableNodeItemsSyntaxHighlighting(Boolean.parseBoolean(syntax));

        String layout = Settings.get(KEY_DEFAULT_LAYOUT);
        if (layout != null) s.setDefaultLayout(layout);

        String lSwitch = Settings.get(KEY_LAYOUT_SWITCH);
        if (lSwitch != null) s.setLayoutOnCategorySwitch(lSwitch);

        String dur = Settings.get(KEY_ANIM_DURATION);
        if (dur != null) {
            try {
                s.setLayoutAnimationDuration(Integer.parseInt(dur));
            } catch (NumberFormatException ignored) {}
        }

        String fit = Settings.get(KEY_FIT_CONTENT);
        if (fit != null) s.setFitContentAfterLayout(Boolean.parseBoolean(fit));

        String relayout = Settings.get(KEY_RELAYOUT_NEW);
        if (relayout != null) s.setDoRelayoutWhenNewElementsAdded(Boolean.parseBoolean(relayout));

        for (String catKey : s.getCategorySelections().keySet()) {
            String val = Settings.get(KEY_CAT_PREFIX + catKey);
            if (val != null) {
                s.getCategorySelections().put(catKey, Boolean.parseBoolean(val));
            }
        }

        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        Settings.put(KEY_DEFAULT_SCOPE, currentSettings.getDefaultScope());
        Settings.put(KEY_NODE_ITEM_STYLE, currentSettings.getNodeItemStyle());
        Settings.put(KEY_SHORTEN_LEN, String.valueOf(currentSettings.getShortenNodeItemsLength()));
        Settings.put(KEY_SHOW_GRID, String.valueOf(currentSettings.isShowGridByDefault()));
        Settings.put(KEY_ANIMATIONS, String.valueOf(currentSettings.isEnableAnimations()));
        Settings.put(KEY_SYNTAX, String.valueOf(currentSettings.isEnableNodeItemsSyntaxHighlighting()));
        Settings.put(KEY_DEFAULT_LAYOUT, currentSettings.getDefaultLayout());
        Settings.put(KEY_LAYOUT_SWITCH, currentSettings.getLayoutOnCategorySwitch());
        Settings.put(KEY_ANIM_DURATION, String.valueOf(currentSettings.getLayoutAnimationDuration()));
        Settings.put(KEY_FIT_CONTENT, String.valueOf(currentSettings.isFitContentAfterLayout()));
        Settings.put(KEY_RELAYOUT_NEW, String.valueOf(currentSettings.isDoRelayoutWhenNewElementsAdded()));

        for (Map.Entry<String, Boolean> entry : currentSettings.getCategorySelections().entrySet()) {
            Settings.put(KEY_CAT_PREFIX + entry.getKey(), String.valueOf(entry.getValue()));
        }
    }
}
