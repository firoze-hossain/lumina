package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > XPath Viewer configuration settings in Lumina IDE.
 */
public class XPathViewerSettings implements Cloneable {

    // Settings
    private boolean scrollFirstHitIntoVisibleArea = true;
    private boolean useNodeAtCursorAsContextNode = true;
    private boolean highlightOnlyStartTag = true;
    private boolean addErrorStripeMarkers = true;

    // Colors
    private String highlightColor = "#FFD578";
    private String contextNodeColor = "#C2FFD4";

    public XPathViewerSettings() {
    }

    public boolean isScrollFirstHitIntoVisibleArea() {
        return scrollFirstHitIntoVisibleArea;
    }

    public void setScrollFirstHitIntoVisibleArea(boolean scrollFirstHitIntoVisibleArea) {
        this.scrollFirstHitIntoVisibleArea = scrollFirstHitIntoVisibleArea;
    }

    public boolean isUseNodeAtCursorAsContextNode() {
        return useNodeAtCursorAsContextNode;
    }

    public void setUseNodeAtCursorAsContextNode(boolean useNodeAtCursorAsContextNode) {
        this.useNodeAtCursorAsContextNode = useNodeAtCursorAsContextNode;
    }

    public boolean isHighlightOnlyStartTag() {
        return highlightOnlyStartTag;
    }

    public void setHighlightOnlyStartTag(boolean highlightOnlyStartTag) {
        this.highlightOnlyStartTag = highlightOnlyStartTag;
    }

    public boolean isAddErrorStripeMarkers() {
        return addErrorStripeMarkers;
    }

    public void setAddErrorStripeMarkers(boolean addErrorStripeMarkers) {
        this.addErrorStripeMarkers = addErrorStripeMarkers;
    }

    public String getHighlightColor() {
        return highlightColor != null ? highlightColor : "#FFD578";
    }

    public void setHighlightColor(String highlightColor) {
        this.highlightColor = highlightColor != null ? highlightColor : "#FFD578";
    }

    public String getContextNodeColor() {
        return contextNodeColor != null ? contextNodeColor : "#C2FFD4";
    }

    public void setContextNodeColor(String contextNodeColor) {
        this.contextNodeColor = contextNodeColor != null ? contextNodeColor : "#C2FFD4";
    }

    @Override
    public XPathViewerSettings clone() {
        try {
            return (XPathViewerSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            XPathViewerSettings copy = new XPathViewerSettings();
            copy.scrollFirstHitIntoVisibleArea = this.scrollFirstHitIntoVisibleArea;
            copy.useNodeAtCursorAsContextNode = this.useNodeAtCursorAsContextNode;
            copy.highlightOnlyStartTag = this.highlightOnlyStartTag;
            copy.addErrorStripeMarkers = this.addErrorStripeMarkers;
            copy.highlightColor = this.highlightColor;
            copy.contextNodeColor = this.contextNodeColor;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XPathViewerSettings that = (XPathViewerSettings) o;
        return scrollFirstHitIntoVisibleArea == that.scrollFirstHitIntoVisibleArea &&
                useNodeAtCursorAsContextNode == that.useNodeAtCursorAsContextNode &&
                highlightOnlyStartTag == that.highlightOnlyStartTag &&
                addErrorStripeMarkers == that.addErrorStripeMarkers &&
                Objects.equals(highlightColor, that.highlightColor) &&
                Objects.equals(contextNodeColor, that.contextNodeColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scrollFirstHitIntoVisibleArea, useNodeAtCursorAsContextNode,
                highlightOnlyStartTag, addErrorStripeMarkers, highlightColor, contextNodeColor);
    }
}
