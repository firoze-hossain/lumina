package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton configuration manager for Tools > Web Browsers and Preview settings in Lumina IDE.
 */
public class WebBrowsersSettingsManager {

    private static final String KEY_DEFAULT_BROWSER = "tools.web_browsers.default_browser";
    private static final String KEY_CUSTOM_PATH = "tools.web_browsers.custom_path";
    private static final String KEY_POPUP_HTML = "tools.web_browsers.popup_html";
    private static final String KEY_POPUP_XML = "tools.web_browsers.popup_xml";
    private static final String KEY_RELOAD_BROWSER = "tools.web_browsers.reload_browser";
    private static final String KEY_RELOAD_PREVIEW = "tools.web_browsers.reload_preview";
    private static final String KEY_SERVER_PORT = "tools.web_browsers.server_port";
    private static final String KEY_ACCEPT_EXTERNAL = "tools.web_browsers.accept_external";
    private static final String KEY_ALLOW_UNSIGNED = "tools.web_browsers.allow_unsigned";
    private static final String KEY_BROWSERS_DATA = "tools.web_browsers.browsers_data";

    private static final WebBrowsersSettingsManager INSTANCE = new WebBrowsersSettingsManager();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private WebBrowsersSettingsManager() {
    }

    public static WebBrowsersSettingsManager getInstance() {
        return INSTANCE;
    }

    public WebBrowsersSettings load() {
        return getSettings();
    }

    public WebBrowsersSettings getSettings() {
        WebBrowsersSettings s = new WebBrowsersSettings();

        String def = Settings.get(KEY_DEFAULT_BROWSER);
        if (def != null) s.setDefaultBrowser(def);

        String cp = Settings.get(KEY_CUSTOM_PATH);
        if (cp != null) s.setCustomBrowserPath(cp);

        String phtml = Settings.get(KEY_POPUP_HTML);
        if (phtml != null) s.setShowPopupForHtml(Boolean.parseBoolean(phtml));

        String pxml = Settings.get(KEY_POPUP_XML);
        if (pxml != null) s.setShowPopupForXml(Boolean.parseBoolean(pxml));

        String rb = Settings.get(KEY_RELOAD_BROWSER);
        if (rb != null) s.setReloadInBrowser(rb);

        String rp = Settings.get(KEY_RELOAD_PREVIEW);
        if (rp != null) s.setReloadInBuiltInPreview(rp);

        String sp = Settings.get(KEY_SERVER_PORT);
        if (sp != null) {
            try { s.setBuiltInServerPort(Integer.parseInt(sp)); } catch (NumberFormatException ignored) {}
        }

        String ae = Settings.get(KEY_ACCEPT_EXTERNAL);
        if (ae != null) s.setCanAcceptExternalConnections(Boolean.parseBoolean(ae));

        String au = Settings.get(KEY_ALLOW_UNSIGNED);
        if (au != null) s.setAllowUnsignedRequests(Boolean.parseBoolean(au));

        String data = Settings.get(KEY_BROWSERS_DATA);
        if (data != null && !data.isBlank()) {
            List<WebBrowserEntry> parsed = deserializeBrowsers(data);
            if (!parsed.isEmpty()) {
                s.setBrowsers(parsed);
            }
        }

        return s;
    }

    public void setSettings(WebBrowsersSettings s) {
        if (s == null) return;

        Settings.put(KEY_DEFAULT_BROWSER, s.getDefaultBrowser());
        Settings.put(KEY_CUSTOM_PATH, s.getCustomBrowserPath());
        Settings.put(KEY_POPUP_HTML, String.valueOf(s.isShowPopupForHtml()));
        Settings.put(KEY_POPUP_XML, String.valueOf(s.isShowPopupForXml()));
        Settings.put(KEY_RELOAD_BROWSER, s.getReloadInBrowser());
        Settings.put(KEY_RELOAD_PREVIEW, s.getReloadInBuiltInPreview());
        Settings.put(KEY_SERVER_PORT, String.valueOf(s.getBuiltInServerPort()));
        Settings.put(KEY_ACCEPT_EXTERNAL, String.valueOf(s.isCanAcceptExternalConnections()));
        Settings.put(KEY_ALLOW_UNSIGNED, String.valueOf(s.isAllowUnsignedRequests()));
        Settings.put(KEY_BROWSERS_DATA, serializeBrowsers(s.getBrowsers()));

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

    private String serializeBrowsers(List<WebBrowserEntry> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (WebBrowserEntry e : list) {
            if (!sb.isEmpty()) sb.append(";;;");
            sb.append(e.getId()).append(":::")
                    .append(e.isActive()).append(":::")
                    .append(escape(e.getName())).append(":::")
                    .append(escape(e.getFamily())).append(":::")
                    .append(escape(e.getPath()));
        }
        return sb.toString();
    }

    private List<WebBrowserEntry> deserializeBrowsers(String data) {
        List<WebBrowserEntry> list = new ArrayList<>();
        if (data == null || data.isBlank()) return list;
        String[] records = data.split(";;;");
        for (String r : records) {
            if (r.isBlank()) continue;
            String[] parts = r.split(":::", -1);
            if (parts.length >= 5) {
                WebBrowserEntry e = new WebBrowserEntry();
                e.setId(parts[0]);
                e.setActive(Boolean.parseBoolean(parts[1]));
                e.setName(unescape(parts[2]));
                e.setFamily(unescape(parts[3]));
                e.setPath(unescape(parts[4]));
                list.add(e);
            }
        }
        return list;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("%", "%25").replace(":", "%3A").replace(";", "%3B");
    }

    private String unescape(String s) {
        if (s == null) return "";
        return s.replace("%3B", ";").replace("%3A", ":").replace("%25", "%");
    }
}
