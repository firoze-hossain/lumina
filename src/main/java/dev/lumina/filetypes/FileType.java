package dev.lumina.filetypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a recognized or user-defined File Type in Lumina,
 * mirroring IntelliJ IDEA's File Type configuration.
 */
public class FileType {

    private String name;
    private String description;
    private String iconKind;
    private boolean builtin;

    private List<String> patterns = new ArrayList<>();
    private List<String> hashbangs = new ArrayList<>();

    // Syntax Highlighting configuration (for user-defined file types)
    private String lineComment = "//";
    private boolean lineCommentAtStartOnly = false;
    private String blockCommentStart = "/*";
    private String blockCommentEnd = "*/";
    private String hexPrefix = "0x";
    private String numberPostfixes = "";
    private boolean supportPairedBraces = true;
    private boolean supportPairedBrackets = true;
    private boolean supportPairedParens = true;
    private boolean supportPairedStringEscapes = true;
    private boolean ignoreCase = false;

    // 4 Keyword sets (matching IntelliJ's 4 tabs)
    private String keywordsGroup1 = "";
    private String keywordsGroup2 = "";
    private String keywordsGroup3 = "";
    private String keywordsGroup4 = "";

    public FileType() {
    }

    public FileType(String name, String description, String iconKind, boolean builtin, List<String> patterns, List<String> hashbangs) {
        this.name = name;
        this.description = description;
        this.iconKind = iconKind;
        this.builtin = builtin;
        if (patterns != null) {
            this.patterns.addAll(patterns);
        }
        if (hashbangs != null) {
            this.hashbangs.addAll(hashbangs);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconKind() {
        return iconKind != null ? iconKind : "generic";
    }

    public void setIconKind(String iconKind) {
        this.iconKind = iconKind;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public void setBuiltin(boolean builtin) {
        this.builtin = builtin;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns != null ? new ArrayList<>(patterns) : new ArrayList<>();
    }

    public List<String> getHashbangs() {
        return hashbangs;
    }

    public void setHashbangs(List<String> hashbangs) {
        this.hashbangs = hashbangs != null ? new ArrayList<>(hashbangs) : new ArrayList<>();
    }

    public String getLineComment() {
        return lineComment != null ? lineComment : "";
    }

    public void setLineComment(String lineComment) {
        this.lineComment = lineComment;
    }

    public boolean isLineCommentAtStartOnly() {
        return lineCommentAtStartOnly;
    }

    public void setLineCommentAtStartOnly(boolean lineCommentAtStartOnly) {
        this.lineCommentAtStartOnly = lineCommentAtStartOnly;
    }

    public String getBlockCommentStart() {
        return blockCommentStart != null ? blockCommentStart : "";
    }

    public void setBlockCommentStart(String blockCommentStart) {
        this.blockCommentStart = blockCommentStart;
    }

    public String getBlockCommentEnd() {
        return blockCommentEnd != null ? blockCommentEnd : "";
    }

    public void setBlockCommentEnd(String blockCommentEnd) {
        this.blockCommentEnd = blockCommentEnd;
    }

    public String getHexPrefix() {
        return hexPrefix != null ? hexPrefix : "";
    }

    public void setHexPrefix(String hexPrefix) {
        this.hexPrefix = hexPrefix;
    }

    public String getNumberPostfixes() {
        return numberPostfixes != null ? numberPostfixes : "";
    }

    public void setNumberPostfixes(String numberPostfixes) {
        this.numberPostfixes = numberPostfixes;
    }

    public boolean isSupportPairedBraces() {
        return supportPairedBraces;
    }

    public void setSupportPairedBraces(boolean supportPairedBraces) {
        this.supportPairedBraces = supportPairedBraces;
    }

    public boolean isSupportPairedBrackets() {
        return supportPairedBrackets;
    }

    public void setSupportPairedBrackets(boolean supportPairedBrackets) {
        this.supportPairedBrackets = supportPairedBrackets;
    }

    public boolean isSupportPairedParens() {
        return supportPairedParens;
    }

    public void setSupportPairedParens(boolean supportPairedParens) {
        this.supportPairedParens = supportPairedParens;
    }

    public boolean isSupportPairedStringEscapes() {
        return supportPairedStringEscapes;
    }

    public void setSupportPairedStringEscapes(boolean supportPairedStringEscapes) {
        this.supportPairedStringEscapes = supportPairedStringEscapes;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public String getKeywordsGroup1() {
        return keywordsGroup1 != null ? keywordsGroup1 : "";
    }

    public void setKeywordsGroup1(String keywordsGroup1) {
        this.keywordsGroup1 = keywordsGroup1;
    }

    public String getKeywordsGroup2() {
        return keywordsGroup2 != null ? keywordsGroup2 : "";
    }

    public void setKeywordsGroup2(String keywordsGroup2) {
        this.keywordsGroup2 = keywordsGroup2;
    }

    public String getKeywordsGroup3() {
        return keywordsGroup3 != null ? keywordsGroup3 : "";
    }

    public void setKeywordsGroup3(String keywordsGroup3) {
        this.keywordsGroup3 = keywordsGroup3;
    }

    public String getKeywordsGroup4() {
        return keywordsGroup4 != null ? keywordsGroup4 : "";
    }

    public void setKeywordsGroup4(String keywordsGroup4) {
        this.keywordsGroup4 = keywordsGroup4;
    }

    public FileType copy() {
        FileType clone = new FileType();
        clone.name = this.name;
        clone.description = this.description;
        clone.iconKind = this.iconKind;
        clone.builtin = this.builtin;
        clone.patterns = new ArrayList<>(this.patterns);
        clone.hashbangs = new ArrayList<>(this.hashbangs);
        clone.lineComment = this.lineComment;
        clone.lineCommentAtStartOnly = this.lineCommentAtStartOnly;
        clone.blockCommentStart = this.blockCommentStart;
        clone.blockCommentEnd = this.blockCommentEnd;
        clone.hexPrefix = this.hexPrefix;
        clone.numberPostfixes = this.numberPostfixes;
        clone.supportPairedBraces = this.supportPairedBraces;
        clone.supportPairedBrackets = this.supportPairedBrackets;
        clone.supportPairedParens = this.supportPairedParens;
        clone.supportPairedStringEscapes = this.supportPairedStringEscapes;
        clone.ignoreCase = this.ignoreCase;
        clone.keywordsGroup1 = this.keywordsGroup1;
        clone.keywordsGroup2 = this.keywordsGroup2;
        clone.keywordsGroup3 = this.keywordsGroup3;
        clone.keywordsGroup4 = this.keywordsGroup4;
        return clone;
    }

    public boolean isEquivalentTo(FileType other) {
        if (other == null) return false;
        return Objects.equals(name, other.name)
                && Objects.equals(description, other.description)
                && Objects.equals(iconKind, other.iconKind)
                && builtin == other.builtin
                && Objects.equals(patterns, other.patterns)
                && Objects.equals(hashbangs, other.hashbangs)
                && Objects.equals(lineComment, other.lineComment)
                && lineCommentAtStartOnly == other.lineCommentAtStartOnly
                && Objects.equals(blockCommentStart, other.blockCommentStart)
                && Objects.equals(blockCommentEnd, other.blockCommentEnd)
                && Objects.equals(hexPrefix, other.hexPrefix)
                && Objects.equals(numberPostfixes, other.numberPostfixes)
                && supportPairedBraces == other.supportPairedBraces
                && supportPairedBrackets == other.supportPairedBrackets
                && supportPairedParens == other.supportPairedParens
                && supportPairedStringEscapes == other.supportPairedStringEscapes
                && ignoreCase == other.ignoreCase
                && Objects.equals(keywordsGroup1, other.keywordsGroup1)
                && Objects.equals(keywordsGroup2, other.keywordsGroup2)
                && Objects.equals(keywordsGroup3, other.keywordsGroup3)
                && Objects.equals(keywordsGroup4, other.keywordsGroup4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileType fileType = (FileType) o;
        return Objects.equals(name, fileType.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
