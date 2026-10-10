package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Jupyter > Jupyter VCS settings in Lumina IDE.
 * Matches 1:1 with dynamic configuration for notebook output clearing before commit.
 */
public class JupyterVcsSettings implements Cloneable {

    public static final String MODE_SUGGEST = "SUGGEST";
    public static final String MODE_ALWAYS = "ALWAYS";

    private boolean clearNotebookOutputsBeforeCommit = true;
    private String clearOutputsMode = MODE_SUGGEST;
    private int fileSizeExceedsMb = 10;

    // Preserved for diff & inspection options
    private boolean showRichDiffForNotebooks = true;
    private boolean ignoreOutputsInDiff = false;

    public JupyterVcsSettings() {
    }

    public boolean isClearNotebookOutputsBeforeCommit() {
        return clearNotebookOutputsBeforeCommit;
    }

    public void setClearNotebookOutputsBeforeCommit(boolean clearNotebookOutputsBeforeCommit) {
        this.clearNotebookOutputsBeforeCommit = clearNotebookOutputsBeforeCommit;
    }

    /**
     * Backward-compatibility alias for clearNotebookOutputsBeforeCommit.
     */
    public boolean isClearOutputsOnCommit() {
        return clearNotebookOutputsBeforeCommit;
    }

    public void setClearOutputsOnCommit(boolean clearOutputsOnCommit) {
        this.clearNotebookOutputsBeforeCommit = clearOutputsOnCommit;
    }

    public String getClearOutputsMode() {
        return clearOutputsMode != null ? clearOutputsMode : MODE_SUGGEST;
    }

    public void setClearOutputsMode(String clearOutputsMode) {
        this.clearOutputsMode = clearOutputsMode != null ? clearOutputsMode : MODE_SUGGEST;
    }

    public int getFileSizeExceedsMb() {
        return fileSizeExceedsMb;
    }

    public void setFileSizeExceedsMb(int fileSizeExceedsMb) {
        this.fileSizeExceedsMb = fileSizeExceedsMb;
    }

    public boolean isShowRichDiffForNotebooks() {
        return showRichDiffForNotebooks;
    }

    public void setShowRichDiffForNotebooks(boolean showRichDiffForNotebooks) {
        this.showRichDiffForNotebooks = showRichDiffForNotebooks;
    }

    public boolean isIgnoreOutputsInDiff() {
        return ignoreOutputsInDiff;
    }

    public void setIgnoreOutputsInDiff(boolean ignoreOutputsInDiff) {
        this.ignoreOutputsInDiff = ignoreOutputsInDiff;
    }

    public JupyterVcsSettings copy() {
        return clone();
    }

    @Override
    public JupyterVcsSettings clone() {
        JupyterVcsSettings c = new JupyterVcsSettings();
        c.clearNotebookOutputsBeforeCommit = this.clearNotebookOutputsBeforeCommit;
        c.clearOutputsMode = this.clearOutputsMode;
        c.fileSizeExceedsMb = this.fileSizeExceedsMb;
        c.showRichDiffForNotebooks = this.showRichDiffForNotebooks;
        c.ignoreOutputsInDiff = this.ignoreOutputsInDiff;
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JupyterVcsSettings that = (JupyterVcsSettings) o;
        return clearNotebookOutputsBeforeCommit == that.clearNotebookOutputsBeforeCommit &&
                fileSizeExceedsMb == that.fileSizeExceedsMb &&
                showRichDiffForNotebooks == that.showRichDiffForNotebooks &&
                ignoreOutputsInDiff == that.ignoreOutputsInDiff &&
                Objects.equals(clearOutputsMode, that.clearOutputsMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clearNotebookOutputsBeforeCommit, clearOutputsMode, fileSizeExceedsMb, showRichDiffForNotebooks, ignoreOutputsInDiff);
    }

    @Override
    public String toString() {
        return "JupyterVcsSettings{" +
                "clearOutputsBeforeCommit=" + clearNotebookOutputsBeforeCommit +
                ", mode='" + clearOutputsMode + '\'' +
                ", fileSizeExceedsMb=" + fileSizeExceedsMb +
                '}';
    }
}
