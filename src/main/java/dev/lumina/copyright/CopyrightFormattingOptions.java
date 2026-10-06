package dev.lumina.copyright;

import java.util.Objects;

/**
 * Configuration options for formatting a copyright notice comment,
 * mirroring IntelliJ IDEA's Editor > Copyright > Formatting options.
 */
public class CopyrightFormattingOptions {

    public enum CommentType {
        BLOCK,
        LINE
    }

    public enum RelativeLocation {
        BEFORE_OTHER_COMMENTS,
        AFTER_OTHER_COMMENTS
    }

    public enum LocationInFile {
        // Java
        BEFORE_PACKAGE,
        BEFORE_IMPORTS,
        BEFORE_CLASS,

        // HTML / XML / DTD / JSP
        BEFORE_DOCTYPE,
        BEFORE_ROOT_TAG
    }

    private CommentType commentType = CommentType.BLOCK;
    private boolean prefixEachLine = true;
    private RelativeLocation relativeLocation = RelativeLocation.BEFORE_OTHER_COMMENTS;

    private boolean separatorBefore = false;
    private int separatorBeforeLength = 80;
    private boolean separatorAfter = false;
    private int separatorAfterLength = 80;
    private String separatorChar = "";
    private boolean box = false;
    private boolean blankLineBefore = false;
    private boolean blankLineAfter = true;

    private LocationInFile locationInFile = LocationInFile.BEFORE_DOCTYPE;

    public CopyrightFormattingOptions() {
    }

    public CommentType getCommentType() {
        return commentType;
    }

    public void setCommentType(CommentType commentType) {
        this.commentType = commentType != null ? commentType : CommentType.BLOCK;
    }

    public boolean isPrefixEachLine() {
        return prefixEachLine;
    }

    public void setPrefixEachLine(boolean prefixEachLine) {
        this.prefixEachLine = prefixEachLine;
    }

    public RelativeLocation getRelativeLocation() {
        return relativeLocation;
    }

    public void setRelativeLocation(RelativeLocation relativeLocation) {
        this.relativeLocation = relativeLocation != null ? relativeLocation : RelativeLocation.BEFORE_OTHER_COMMENTS;
    }

    public boolean isSeparatorBefore() {
        return separatorBefore;
    }

    public void setSeparatorBefore(boolean separatorBefore) {
        this.separatorBefore = separatorBefore;
    }

    public int getSeparatorBeforeLength() {
        return separatorBeforeLength;
    }

    public void setSeparatorBeforeLength(int separatorBeforeLength) {
        this.separatorBeforeLength = separatorBeforeLength;
    }

    public boolean isSeparatorAfter() {
        return separatorAfter;
    }

    public void setSeparatorAfter(boolean separatorAfter) {
        this.separatorAfter = separatorAfter;
    }

    public int getSeparatorAfterLength() {
        return separatorAfterLength;
    }

    public void setSeparatorAfterLength(int separatorAfterLength) {
        this.separatorAfterLength = separatorAfterLength;
    }

    public String getSeparatorChar() {
        return separatorChar != null ? separatorChar : "";
    }

    public void setSeparatorChar(String separatorChar) {
        this.separatorChar = separatorChar != null ? separatorChar : "";
    }

    public boolean isBox() {
        return box;
    }

    public void setBox(boolean box) {
        this.box = box;
    }

    public boolean isBlankLineBefore() {
        return blankLineBefore;
    }

    public void setBlankLineBefore(boolean blankLineBefore) {
        this.blankLineBefore = blankLineBefore;
    }

    public boolean isBlankLineAfter() {
        return blankLineAfter;
    }

    public void setBlankLineAfter(boolean blankLineAfter) {
        this.blankLineAfter = blankLineAfter;
    }

    public LocationInFile getLocationInFile() {
        return locationInFile;
    }

    public void setLocationInFile(LocationInFile locationInFile) {
        this.locationInFile = locationInFile != null ? locationInFile : LocationInFile.BEFORE_DOCTYPE;
    }

    public static LocationInFile getDefaultLocationForLanguage(String language) {
        if (language == null) return LocationInFile.BEFORE_PACKAGE;
        String l = language.trim().toLowerCase();
        if (l.equals("java")) {
            return LocationInFile.BEFORE_PACKAGE;
        }
        if (l.contains("xml") || l.contains("svg") || l.contains("vue")) {
            return LocationInFile.BEFORE_ROOT_TAG;
        }
        return LocationInFile.BEFORE_DOCTYPE;
    }

    public CopyrightFormattingOptions copy() {
        CopyrightFormattingOptions clone = new CopyrightFormattingOptions();
        clone.commentType = this.commentType;
        clone.prefixEachLine = this.prefixEachLine;
        clone.relativeLocation = this.relativeLocation;
        clone.separatorBefore = this.separatorBefore;
        clone.separatorBeforeLength = this.separatorBeforeLength;
        clone.separatorAfter = this.separatorAfter;
        clone.separatorAfterLength = this.separatorAfterLength;
        clone.separatorChar = this.separatorChar;
        clone.box = this.box;
        clone.blankLineBefore = this.blankLineBefore;
        clone.blankLineAfter = this.blankLineAfter;
        clone.locationInFile = this.locationInFile;
        return clone;
    }

    public boolean isEquivalentTo(CopyrightFormattingOptions other) {
        if (other == null) return false;
        return this.commentType == other.commentType
                && this.prefixEachLine == other.prefixEachLine
                && this.relativeLocation == other.relativeLocation
                && this.separatorBefore == other.separatorBefore
                && this.separatorBeforeLength == other.separatorBeforeLength
                && this.separatorAfter == other.separatorAfter
                && this.separatorAfterLength == other.separatorAfterLength
                && Objects.equals(this.separatorChar, other.separatorChar)
                && this.box == other.box
                && this.blankLineBefore == other.blankLineBefore
                && this.blankLineAfter == other.blankLineAfter
                && this.locationInFile == other.locationInFile;
    }

    /**
     * Formats copyright text according to these options and language comment markers.
     */
    public String formatPreview(String[] lines, String blockStart, String blockEnd, String linePrefix, String lineComment) {
        StringBuilder sb = new StringBuilder();

        if (blankLineBefore) {
            sb.append("\n");
        }

        String sep = (!separatorChar.isEmpty()) ? separatorChar.substring(0, 1) : "=";

        if (commentType == CommentType.BLOCK) {
            sb.append(blockStart).append("\n");

            if (separatorBefore) {
                String bar = sep.repeat(Math.max(1, Math.min(120, separatorBeforeLength)));
                sb.append(prefixEachLine ? (linePrefix + bar) : bar).append("\n");
            }

            for (String line : lines) {
                if (prefixEachLine) {
                    sb.append(linePrefix).append(line);
                    if (box) {
                        int pad = Math.max(0, 70 - line.length());
                        sb.append(" ".repeat(pad)).append(linePrefix.trim());
                    }
                    sb.append("\n");
                } else {
                    sb.append(line).append("\n");
                }
            }

            if (separatorAfter) {
                String bar = sep.repeat(Math.max(1, Math.min(120, separatorAfterLength)));
                sb.append(prefixEachLine ? (linePrefix + bar) : bar).append("\n");
            }

            sb.append(blockEnd);
        } else {
            // Line comment
            if (separatorBefore) {
                String bar = sep.repeat(Math.max(1, Math.min(120, separatorBeforeLength)));
                sb.append(lineComment).append(bar).append("\n");
            }

            for (String line : lines) {
                sb.append(lineComment).append(line).append("\n");
            }

            if (separatorAfter) {
                String bar = sep.repeat(Math.max(1, Math.min(120, separatorAfterLength)));
                sb.append(lineComment).append(bar).append("\n");
            }
        }

        if (blankLineAfter) {
            sb.append("\n");
        }

        return sb.toString();
    }
}
