package dev.lumina.folding;

import java.util.Objects;

/**
 * Represents a collapsible/expandable code region matching IntelliJ IDEA folding.
 */
public class FoldRegion implements Comparable<FoldRegion> {

    public enum RegionType {
        IMPORTS,
        ANNOTATIONS,
        METHOD,
        CLASS,
        COMMENT,
        CUSTOM
    }

    private final RegionType type;
    private int startLine; // 1-based line number (inclusive)
    private int endLine;   // 1-based line number (inclusive)
    private String placeholder;
    private String headerText;
    private boolean folded;
    private String foldedContent;

    public FoldRegion(RegionType type, int startLine, int endLine, String placeholder) {
        this(type, startLine, endLine, placeholder, null, false);
    }

    public FoldRegion(RegionType type, int startLine, int endLine, String placeholder, String headerText, boolean folded) {
        this.type = type;
        this.startLine = startLine;
        this.endLine = endLine;
        this.placeholder = placeholder != null ? placeholder : "...";
        this.headerText = headerText;
        this.folded = folded;
    }

    public RegionType getType() {
        return type;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getHeaderText() {
        return headerText;
    }

    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    public boolean isFolded() {
        return folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    public String getFoldedContent() {
        return foldedContent;
    }

    public void setFoldedContent(String foldedContent) {
        this.foldedContent = foldedContent;
    }

    public void toggle() {
        this.folded = !this.folded;
    }

    public int getLineCount() {
        return Math.max(1, endLine - startLine + 1);
    }

    public boolean containsLine(int line) {
        return line >= startLine && line <= endLine;
    }

    @Override
    public int compareTo(FoldRegion other) {
        if (other == null) return 1;
        int cmp = Integer.compare(this.startLine, other.startLine);
        if (cmp != 0) return cmp;
        return Integer.compare(other.endLine, this.endLine); // Outer/larger block first
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FoldRegion that)) return false;
        return startLine == that.startLine && endLine == that.endLine && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, startLine, endLine);
    }

    @Override
    public String toString() {
        return "FoldRegion[" + type + " L" + startLine + "-L" + endLine + " folded=" + folded + " " + placeholder + "]";
    }
}
