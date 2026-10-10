package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Tools > Web Browsers and Preview configuration settings in Lumina IDE.
 */
public class WebBrowsersSettings implements Cloneable {

    private List<WebBrowserEntry> browsers = new ArrayList<>();
    private String defaultBrowser = "System default";
    private String customBrowserPath = "";

    // Show browser popup in the editor
    private boolean showPopupForHtml = true;
    private boolean showPopupForXml = false;

    // Reload behavior
    private String reloadInBrowser = "On Save"; // "On Save", "Disabled", "Manually"
    private String reloadInBuiltInPreview = "On Save";

    // Built-in Server
    private int builtInServerPort = 63342;
    private boolean canAcceptExternalConnections = false;
    private boolean allowUnsignedRequests = false;

    public WebBrowsersSettings() {
        this.browsers = WebBrowserEntry.defaultBrowsers();
    }

    public List<WebBrowserEntry> getBrowsers() {
        if (browsers == null) browsers = new ArrayList<>();
        return browsers;
    }

    public void setBrowsers(List<WebBrowserEntry> browsers) {
        this.browsers = browsers != null ? browsers : new ArrayList<>();
    }

    public String getDefaultBrowser() {
        return defaultBrowser != null ? defaultBrowser : "System default";
    }

    public void setDefaultBrowser(String defaultBrowser) {
        this.defaultBrowser = defaultBrowser != null ? defaultBrowser : "System default";
    }

    public String getCustomBrowserPath() {
        return customBrowserPath != null ? customBrowserPath : "";
    }

    public void setCustomBrowserPath(String customBrowserPath) {
        this.customBrowserPath = customBrowserPath != null ? customBrowserPath : "";
    }

    public boolean isShowPopupForHtml() {
        return showPopupForHtml;
    }

    public void setShowPopupForHtml(boolean showPopupForHtml) {
        this.showPopupForHtml = showPopupForHtml;
    }

    public boolean isShowPopupForXml() {
        return showPopupForXml;
    }

    public void setShowPopupForXml(boolean showPopupForXml) {
        this.showPopupForXml = showPopupForXml;
    }

    public String getReloadInBrowser() {
        return reloadInBrowser != null ? reloadInBrowser : "On Save";
    }

    public void setReloadInBrowser(String reloadInBrowser) {
        this.reloadInBrowser = reloadInBrowser != null ? reloadInBrowser : "On Save";
    }

    public String getReloadInBuiltInPreview() {
        return reloadInBuiltInPreview != null ? reloadInBuiltInPreview : "On Save";
    }

    public void setReloadInBuiltInPreview(String reloadInBuiltInPreview) {
        this.reloadInBuiltInPreview = reloadInBuiltInPreview != null ? reloadInBuiltInPreview : "On Save";
    }

    public int getBuiltInServerPort() {
        return builtInServerPort;
    }

    public void setBuiltInServerPort(int builtInServerPort) {
        this.builtInServerPort = builtInServerPort > 0 ? builtInServerPort : 63342;
    }

    public boolean isCanAcceptExternalConnections() {
        return canAcceptExternalConnections;
    }

    public void setCanAcceptExternalConnections(boolean canAcceptExternalConnections) {
        this.canAcceptExternalConnections = canAcceptExternalConnections;
    }

    public boolean isAllowUnsignedRequests() {
        return allowUnsignedRequests;
    }

    public void setAllowUnsignedRequests(boolean allowUnsignedRequests) {
        this.allowUnsignedRequests = allowUnsignedRequests;
    }

    @Override
    public WebBrowsersSettings clone() {
        try {
            WebBrowsersSettings copy = (WebBrowsersSettings) super.clone();
            copy.browsers = new ArrayList<>();
            for (WebBrowserEntry b : this.browsers) {
                copy.browsers.add(b.clone());
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            WebBrowsersSettings copy = new WebBrowsersSettings();
            copy.browsers = new ArrayList<>();
            for (WebBrowserEntry b : this.browsers) {
                copy.browsers.add(b.clone());
            }
            copy.defaultBrowser = this.defaultBrowser;
            copy.customBrowserPath = this.customBrowserPath;
            copy.showPopupForHtml = this.showPopupForHtml;
            copy.showPopupForXml = this.showPopupForXml;
            copy.reloadInBrowser = this.reloadInBrowser;
            copy.reloadInBuiltInPreview = this.reloadInBuiltInPreview;
            copy.builtInServerPort = this.builtInServerPort;
            copy.canAcceptExternalConnections = this.canAcceptExternalConnections;
            copy.allowUnsignedRequests = this.allowUnsignedRequests;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebBrowsersSettings that = (WebBrowsersSettings) o;
        return showPopupForHtml == that.showPopupForHtml &&
                showPopupForXml == that.showPopupForXml &&
                builtInServerPort == that.builtInServerPort &&
                canAcceptExternalConnections == that.canAcceptExternalConnections &&
                allowUnsignedRequests == that.allowUnsignedRequests &&
                Objects.equals(browsers, that.browsers) &&
                Objects.equals(defaultBrowser, that.defaultBrowser) &&
                Objects.equals(customBrowserPath, that.customBrowserPath) &&
                Objects.equals(reloadInBrowser, that.reloadInBrowser) &&
                Objects.equals(reloadInBuiltInPreview, that.reloadInBuiltInPreview);
    }

    @Override
    public int hashCode() {
        return Objects.hash(browsers, defaultBrowser, customBrowserPath, showPopupForHtml,
                showPopupForXml, reloadInBrowser, reloadInBuiltInPreview, builtInServerPort,
                canAcceptExternalConnections, allowUnsignedRequests);
    }
}
