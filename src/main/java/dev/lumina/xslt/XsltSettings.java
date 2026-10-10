package dev.lumina.xslt;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > XSLT settings in Lumina IDE.
 * Matches reference screenshot media_1791604044717_ba9f771b.png:
 *  - Show associated files in project view checkbox
 */
public class XsltSettings implements Cloneable {

    private boolean showAssociatedFilesInProjectView = true;

    public XsltSettings() {
    }

    public XsltSettings(XsltSettings other) {
        if (other != null) {
            this.showAssociatedFilesInProjectView = other.showAssociatedFilesInProjectView;
        }
    }

    public boolean isShowAssociatedFilesInProjectView() {
        return showAssociatedFilesInProjectView;
    }

    public void setShowAssociatedFilesInProjectView(boolean showAssociatedFilesInProjectView) {
        this.showAssociatedFilesInProjectView = showAssociatedFilesInProjectView;
    }

    @Override
    public XsltSettings clone() {
        return new XsltSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XsltSettings that = (XsltSettings) o;
        return showAssociatedFilesInProjectView == that.showAssociatedFilesInProjectView;
    }

    @Override
    public int hashCode() {
        return Objects.hash(showAssociatedFilesInProjectView);
    }
}
