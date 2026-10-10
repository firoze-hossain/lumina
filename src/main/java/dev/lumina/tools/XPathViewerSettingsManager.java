package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Tools > XPath Viewer settings in Lumina IDE.
 */
public class XPathViewerSettingsManager {

    private static final String KEY_SCROLL_FIRST = "tools.xpath_viewer.scroll_first_hit";
    private static final String KEY_USE_NODE_CURSOR = "tools.xpath_viewer.use_node_cursor";
    private static final String KEY_HIGHLIGHT_START_TAG = "tools.xpath_viewer.highlight_start_tag";
    private static final String KEY_ADD_STRIPE_MARKERS = "tools.xpath_viewer.add_stripe_markers";
    private static final String KEY_HIGHLIGHT_COLOR = "tools.xpath_viewer.highlight_color";
    private static final String KEY_CONTEXT_NODE_COLOR = "tools.xpath_viewer.context_node_color";

    private static final XPathViewerSettingsManager INSTANCE = new XPathViewerSettingsManager();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private XPathViewerSettingsManager() {
    }

    public static XPathViewerSettingsManager getInstance() {
        return INSTANCE;
    }

    public XPathViewerSettings load() {
        return getSettings();
    }

    public XPathViewerSettings getSettings() {
        XPathViewerSettings s = new XPathViewerSettings();

        String sf = Settings.get(KEY_SCROLL_FIRST);
        if (sf != null) s.setScrollFirstHitIntoVisibleArea(Boolean.parseBoolean(sf));

        String unc = Settings.get(KEY_USE_NODE_CURSOR);
        if (unc != null) s.setUseNodeAtCursorAsContextNode(Boolean.parseBoolean(unc));

        String hst = Settings.get(KEY_HIGHLIGHT_START_TAG);
        if (hst != null) s.setHighlightOnlyStartTag(Boolean.parseBoolean(hst));

        String asm = Settings.get(KEY_ADD_STRIPE_MARKERS);
        if (asm != null) s.setAddErrorStripeMarkers(Boolean.parseBoolean(asm));

        String hc = Settings.get(KEY_HIGHLIGHT_COLOR);
        if (hc != null) s.setHighlightColor(hc);

        String cnc = Settings.get(KEY_CONTEXT_NODE_COLOR);
        if (cnc != null) s.setContextNodeColor(cnc);

        return s;
    }

    public void setSettings(XPathViewerSettings s) {
        if (s == null) return;

        Settings.put(KEY_SCROLL_FIRST, String.valueOf(s.isScrollFirstHitIntoVisibleArea()));
        Settings.put(KEY_USE_NODE_CURSOR, String.valueOf(s.isUseNodeAtCursorAsContextNode()));
        Settings.put(KEY_HIGHLIGHT_START_TAG, String.valueOf(s.isHighlightOnlyStartTag()));
        Settings.put(KEY_ADD_STRIPE_MARKERS, String.valueOf(s.isAddErrorStripeMarkers()));
        Settings.put(KEY_HIGHLIGHT_COLOR, s.getHighlightColor());
        Settings.put(KEY_CONTEXT_NODE_COLOR, s.getContextNodeColor());

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
