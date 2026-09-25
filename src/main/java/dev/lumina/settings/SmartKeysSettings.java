package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration model for Editor > General > Smart Keys settings.
 * Backed by ~/.lumina/lumina.properties, supporting listeners, dirty tracking, and real-time updates.
 */
public final class SmartKeysSettings {

    public enum UnindentOnBackspace {
        DISABLED("Disabled"),
        TO_PROPER_INDENT("To proper indent position"),
        TO_NEAREST_INDENT("To nearest indent position");

        private final String label;

        UnindentOnBackspace(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static UnindentOnBackspace fromLabel(String label) {
            for (UnindentOnBackspace u : values()) {
                if (u.label.equalsIgnoreCase(label) || u.name().equalsIgnoreCase(label)) {
                    return u;
                }
            }
            return TO_PROPER_INDENT;
        }
    }

    public enum ReformatOnPaste {
        NONE("None"),
        INDENT_BLOCK("Indent block"),
        INDENT_EACH_LINE("Indent each line"),
        REFORMAT_BLOCK("Reformat block");

        private final String label;

        ReformatOnPaste(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static ReformatOnPaste fromLabel(String label) {
            for (ReformatOnPaste r : values()) {
                if (r.label.equalsIgnoreCase(label) || r.name().equalsIgnoreCase(label)) {
                    return r;
                }
            }
            return INDENT_EACH_LINE;
        }
    }

    private static final SmartKeysSettings INSTANCE = new SmartKeysSettings();

    public static SmartKeysSettings getInstance() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(SmartKeysSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    // --- General Smart Keys ---
    private boolean homeMovesCaretToFirstNonWhitespace = true;
    private boolean endOnBlankLineMovesCaretToIndent = true;
    private boolean insertPairedBrackets = true;
    private boolean insertPairQuote = true;
    private boolean reformatBlockOnTypingRBrace = true;
    private boolean useCamelHumpsWords = false;
    private boolean honorCamelHumpsOnDoubleClick = true;
    private boolean surroundSelectionOnQuoteOrBrace = true;
    private boolean addMultipleCaretsOnDoubleCtrlArrow = true;
    private boolean jumpOutsideClosingBracketOrQuoteWithTab = true;

    // --- Enter ---
    private boolean smartIndent = true;
    private boolean insertPairRBrace = true;
    private boolean closeBlockComment = true;
    private boolean insertDocCommentStub = true;

    // --- Backspace & Paste ---
    private UnindentOnBackspace unindentOnBackspace = UnindentOnBackspace.TO_PROPER_INDENT;
    private ReformatOnPaste reformatOnPaste = ReformatOnPaste.INDENT_EACH_LINE;
    private boolean reformatAgainToRemoveCustomLineBreaks = false;

    // --- JavaDoc ---
    private boolean autoInsertClosingTagInJavaDoc = true;

    // --- JSP ---
    private boolean insertPairPercentOnEnterInJsp = true;

    // --- Kotlin ---
    private boolean convertPastedJavaToKotlin = true;
    private boolean dontShowJavaToKotlinDialogOnPaste = false;
    private boolean autoAddValKeywordToConstructorParams = true;

    public SmartKeysSettings() {
        initDefaults();
        load();
    }

    public void initDefaults() {
        homeMovesCaretToFirstNonWhitespace = true;
        endOnBlankLineMovesCaretToIndent = true;
        insertPairedBrackets = true;
        insertPairQuote = true;
        reformatBlockOnTypingRBrace = true;
        useCamelHumpsWords = false;
        honorCamelHumpsOnDoubleClick = true;
        surroundSelectionOnQuoteOrBrace = true;
        addMultipleCaretsOnDoubleCtrlArrow = true;
        jumpOutsideClosingBracketOrQuoteWithTab = true;

        smartIndent = true;
        insertPairRBrace = true;
        closeBlockComment = true;
        insertDocCommentStub = true;

        unindentOnBackspace = UnindentOnBackspace.TO_PROPER_INDENT;
        reformatOnPaste = ReformatOnPaste.INDENT_EACH_LINE;
        reformatAgainToRemoveCustomLineBreaks = false;

        autoInsertClosingTagInJavaDoc = true;
        insertPairPercentOnEnterInJsp = true;

        convertPastedJavaToKotlin = true;
        dontShowJavaToKotlinDialogOnPaste = false;
        autoAddValKeywordToConstructorParams = true;
    }

    public void load() {
        String val;

        val = Settings.get("smartkeys.home.moves.to.non.whitespace");
        if (val != null) homeMovesCaretToFirstNonWhitespace = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.end.blank.line.to.indent");
        if (val != null) endOnBlankLineMovesCaretToIndent = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.insert.paired.brackets");
        if (val != null) insertPairedBrackets = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.insert.pair.quote");
        if (val != null) insertPairQuote = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.reformat.block.on.rbrace");
        if (val != null) reformatBlockOnTypingRBrace = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.camelhumps.words");
        if (val != null) useCamelHumpsWords = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.honor.camelhumps.double.click");
        if (val != null) honorCamelHumpsOnDoubleClick = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.surround.selection");
        if (val != null) surroundSelectionOnQuoteOrBrace = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.multiple.carets.double.ctrl");
        if (val != null) addMultipleCaretsOnDoubleCtrlArrow = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.jump.outside.closing.bracket");
        if (val != null) jumpOutsideClosingBracketOrQuoteWithTab = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.enter.smart.indent");
        if (val != null) smartIndent = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.enter.insert.pair.rbrace");
        if (val != null) insertPairRBrace = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.enter.close.block.comment");
        if (val != null) closeBlockComment = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.enter.insert.doc.comment.stub");
        if (val != null) insertDocCommentStub = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.backspace.unindent");
        if (val != null) unindentOnBackspace = UnindentOnBackspace.fromLabel(val);

        val = Settings.get("smartkeys.paste.reformat");
        if (val != null) reformatOnPaste = ReformatOnPaste.fromLabel(val);

        val = Settings.get("smartkeys.paste.remove.custom.linebreaks");
        if (val != null) reformatAgainToRemoveCustomLineBreaks = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.javadoc.auto.insert.closing.tag");
        if (val != null) autoInsertClosingTagInJavaDoc = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.jsp.insert.pair.percent");
        if (val != null) insertPairPercentOnEnterInJsp = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.kotlin.convert.pasted.java");
        if (val != null) convertPastedJavaToKotlin = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.kotlin.dont.show.dialog");
        if (val != null) dontShowJavaToKotlinDialogOnPaste = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.kotlin.auto.add.val");
        if (val != null) autoAddValKeywordToConstructorParams = Boolean.parseBoolean(val);
    }

    public void save() {
        Settings.put("smartkeys.home.moves.to.non.whitespace", String.valueOf(homeMovesCaretToFirstNonWhitespace));
        Settings.put("smartkeys.end.blank.line.to.indent", String.valueOf(endOnBlankLineMovesCaretToIndent));
        Settings.put("smartkeys.insert.paired.brackets", String.valueOf(insertPairedBrackets));
        Settings.put("smartkeys.insert.pair.quote", String.valueOf(insertPairQuote));
        Settings.put("smartkeys.reformat.block.on.rbrace", String.valueOf(reformatBlockOnTypingRBrace));
        Settings.put("smartkeys.camelhumps.words", String.valueOf(useCamelHumpsWords));
        Settings.put("smartkeys.honor.camelhumps.double.click", String.valueOf(honorCamelHumpsOnDoubleClick));
        Settings.put("smartkeys.surround.selection", String.valueOf(surroundSelectionOnQuoteOrBrace));
        Settings.put("smartkeys.multiple.carets.double.ctrl", String.valueOf(addMultipleCaretsOnDoubleCtrlArrow));
        Settings.put("smartkeys.jump.outside.closing.bracket", String.valueOf(jumpOutsideClosingBracketOrQuoteWithTab));

        Settings.put("smartkeys.enter.smart.indent", String.valueOf(smartIndent));
        Settings.put("smartkeys.enter.insert.pair.rbrace", String.valueOf(insertPairRBrace));
        Settings.put("smartkeys.enter.close.block.comment", String.valueOf(closeBlockComment));
        Settings.put("smartkeys.enter.insert.doc.comment.stub", String.valueOf(insertDocCommentStub));

        Settings.put("smartkeys.backspace.unindent", unindentOnBackspace.getLabel());
        Settings.put("smartkeys.paste.reformat", reformatOnPaste.getLabel());
        Settings.put("smartkeys.paste.remove.custom.linebreaks", String.valueOf(reformatAgainToRemoveCustomLineBreaks));

        Settings.put("smartkeys.javadoc.auto.insert.closing.tag", String.valueOf(autoInsertClosingTagInJavaDoc));
        Settings.put("smartkeys.jsp.insert.pair.percent", String.valueOf(insertPairPercentOnEnterInJsp));

        Settings.put("smartkeys.kotlin.convert.pasted.java", String.valueOf(convertPastedJavaToKotlin));
        Settings.put("smartkeys.kotlin.dont.show.dialog", String.valueOf(dontShowJavaToKotlinDialogOnPaste));
        Settings.put("smartkeys.kotlin.auto.add.val", String.valueOf(autoAddValKeywordToConstructorParams));

        notifyListeners();
    }

    public SmartKeysSettings copy() {
        SmartKeysSettings clone = new SmartKeysSettings();
        clone.applyFrom(this);
        return clone;
    }

    public void applyFrom(SmartKeysSettings other) {
        if (other == null) return;
        this.homeMovesCaretToFirstNonWhitespace = other.homeMovesCaretToFirstNonWhitespace;
        this.endOnBlankLineMovesCaretToIndent = other.endOnBlankLineMovesCaretToIndent;
        this.insertPairedBrackets = other.insertPairedBrackets;
        this.insertPairQuote = other.insertPairQuote;
        this.reformatBlockOnTypingRBrace = other.reformatBlockOnTypingRBrace;
        this.useCamelHumpsWords = other.useCamelHumpsWords;
        this.honorCamelHumpsOnDoubleClick = other.honorCamelHumpsOnDoubleClick;
        this.surroundSelectionOnQuoteOrBrace = other.surroundSelectionOnQuoteOrBrace;
        this.addMultipleCaretsOnDoubleCtrlArrow = other.addMultipleCaretsOnDoubleCtrlArrow;
        this.jumpOutsideClosingBracketOrQuoteWithTab = other.jumpOutsideClosingBracketOrQuoteWithTab;

        this.smartIndent = other.smartIndent;
        this.insertPairRBrace = other.insertPairRBrace;
        this.closeBlockComment = other.closeBlockComment;
        this.insertDocCommentStub = other.insertDocCommentStub;

        this.unindentOnBackspace = other.unindentOnBackspace;
        this.reformatOnPaste = other.reformatOnPaste;
        this.reformatAgainToRemoveCustomLineBreaks = other.reformatAgainToRemoveCustomLineBreaks;

        this.autoInsertClosingTagInJavaDoc = other.autoInsertClosingTagInJavaDoc;
        this.insertPairPercentOnEnterInJsp = other.insertPairPercentOnEnterInJsp;

        this.convertPastedJavaToKotlin = other.convertPastedJavaToKotlin;
        this.dontShowJavaToKotlinDialogOnPaste = other.dontShowJavaToKotlinDialogOnPaste;
        this.autoAddValKeywordToConstructorParams = other.autoAddValKeywordToConstructorParams;
    }

    public boolean isModified(SmartKeysSettings other) {
        if (other == null) return true;
        return this.homeMovesCaretToFirstNonWhitespace != other.homeMovesCaretToFirstNonWhitespace
                || this.endOnBlankLineMovesCaretToIndent != other.endOnBlankLineMovesCaretToIndent
                || this.insertPairedBrackets != other.insertPairedBrackets
                || this.insertPairQuote != other.insertPairQuote
                || this.reformatBlockOnTypingRBrace != other.reformatBlockOnTypingRBrace
                || this.useCamelHumpsWords != other.useCamelHumpsWords
                || this.honorCamelHumpsOnDoubleClick != other.honorCamelHumpsOnDoubleClick
                || this.surroundSelectionOnQuoteOrBrace != other.surroundSelectionOnQuoteOrBrace
                || this.addMultipleCaretsOnDoubleCtrlArrow != other.addMultipleCaretsOnDoubleCtrlArrow
                || this.jumpOutsideClosingBracketOrQuoteWithTab != other.jumpOutsideClosingBracketOrQuoteWithTab
                || this.smartIndent != other.smartIndent
                || this.insertPairRBrace != other.insertPairRBrace
                || this.closeBlockComment != other.closeBlockComment
                || this.insertDocCommentStub != other.insertDocCommentStub
                || this.unindentOnBackspace != other.unindentOnBackspace
                || this.reformatOnPaste != other.reformatOnPaste
                || this.reformatAgainToRemoveCustomLineBreaks != other.reformatAgainToRemoveCustomLineBreaks
                || this.autoInsertClosingTagInJavaDoc != other.autoInsertClosingTagInJavaDoc
                || this.insertPairPercentOnEnterInJsp != other.insertPairPercentOnEnterInJsp
                || this.convertPastedJavaToKotlin != other.convertPastedJavaToKotlin
                || this.dontShowJavaToKotlinDialogOnPaste != other.dontShowJavaToKotlinDialogOnPaste
                || this.autoAddValKeywordToConstructorParams != other.autoAddValKeywordToConstructorParams;
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener l : listeners) {
            try {
                l.onSettingsChanged(this);
            } catch (Exception ignored) {}
        }
    }

    // --- Getters and Setters ---

    public boolean isHomeMovesCaretToFirstNonWhitespace() {
        return homeMovesCaretToFirstNonWhitespace;
    }

    public void setHomeMovesCaretToFirstNonWhitespace(boolean homeMovesCaretToFirstNonWhitespace) {
        this.homeMovesCaretToFirstNonWhitespace = homeMovesCaretToFirstNonWhitespace;
    }

    public boolean isEndOnBlankLineMovesCaretToIndent() {
        return endOnBlankLineMovesCaretToIndent;
    }

    public void setEndOnBlankLineMovesCaretToIndent(boolean endOnBlankLineMovesCaretToIndent) {
        this.endOnBlankLineMovesCaretToIndent = endOnBlankLineMovesCaretToIndent;
    }

    public boolean isInsertPairedBrackets() {
        return insertPairedBrackets;
    }

    public void setInsertPairedBrackets(boolean insertPairedBrackets) {
        this.insertPairedBrackets = insertPairedBrackets;
    }

    public boolean isInsertPairQuote() {
        return insertPairQuote;
    }

    public void setInsertPairQuote(boolean insertPairQuote) {
        this.insertPairQuote = insertPairQuote;
    }

    public boolean isReformatBlockOnTypingRBrace() {
        return reformatBlockOnTypingRBrace;
    }

    public void setReformatBlockOnTypingRBrace(boolean reformatBlockOnTypingRBrace) {
        this.reformatBlockOnTypingRBrace = reformatBlockOnTypingRBrace;
    }

    public boolean isUseCamelHumpsWords() {
        return useCamelHumpsWords;
    }

    public void setUseCamelHumpsWords(boolean useCamelHumpsWords) {
        this.useCamelHumpsWords = useCamelHumpsWords;
    }

    public boolean isHonorCamelHumpsOnDoubleClick() {
        return honorCamelHumpsOnDoubleClick;
    }

    public void setHonorCamelHumpsOnDoubleClick(boolean honorCamelHumpsOnDoubleClick) {
        this.honorCamelHumpsOnDoubleClick = honorCamelHumpsOnDoubleClick;
    }

    public boolean isSurroundSelectionOnQuoteOrBrace() {
        return surroundSelectionOnQuoteOrBrace;
    }

    public void setSurroundSelectionOnQuoteOrBrace(boolean surroundSelectionOnQuoteOrBrace) {
        this.surroundSelectionOnQuoteOrBrace = surroundSelectionOnQuoteOrBrace;
    }

    public boolean isAddMultipleCaretsOnDoubleCtrlArrow() {
        return addMultipleCaretsOnDoubleCtrlArrow;
    }

    public void setAddMultipleCaretsOnDoubleCtrlArrow(boolean addMultipleCaretsOnDoubleCtrlArrow) {
        this.addMultipleCaretsOnDoubleCtrlArrow = addMultipleCaretsOnDoubleCtrlArrow;
    }

    public boolean isJumpOutsideClosingBracketOrQuoteWithTab() {
        return jumpOutsideClosingBracketOrQuoteWithTab;
    }

    public void setJumpOutsideClosingBracketOrQuoteWithTab(boolean jumpOutsideClosingBracketOrQuoteWithTab) {
        this.jumpOutsideClosingBracketOrQuoteWithTab = jumpOutsideClosingBracketOrQuoteWithTab;
    }

    public boolean isSmartIndent() {
        return smartIndent;
    }

    public void setSmartIndent(boolean smartIndent) {
        this.smartIndent = smartIndent;
    }

    public boolean isInsertPairRBrace() {
        return insertPairRBrace;
    }

    public void setInsertPairRBrace(boolean insertPairRBrace) {
        this.insertPairRBrace = insertPairRBrace;
    }

    public boolean isCloseBlockComment() {
        return closeBlockComment;
    }

    public void setCloseBlockComment(boolean closeBlockComment) {
        this.closeBlockComment = closeBlockComment;
    }

    public boolean isInsertDocCommentStub() {
        return insertDocCommentStub;
    }

    public void setInsertDocCommentStub(boolean insertDocCommentStub) {
        this.insertDocCommentStub = insertDocCommentStub;
    }

    public UnindentOnBackspace getUnindentOnBackspace() {
        return unindentOnBackspace;
    }

    public void setUnindentOnBackspace(UnindentOnBackspace unindentOnBackspace) {
        this.unindentOnBackspace = unindentOnBackspace != null ? unindentOnBackspace : UnindentOnBackspace.TO_PROPER_INDENT;
    }

    public ReformatOnPaste getReformatOnPaste() {
        return reformatOnPaste;
    }

    public void setReformatOnPaste(ReformatOnPaste reformatOnPaste) {
        this.reformatOnPaste = reformatOnPaste != null ? reformatOnPaste : ReformatOnPaste.INDENT_EACH_LINE;
    }

    public boolean isReformatAgainToRemoveCustomLineBreaks() {
        return reformatAgainToRemoveCustomLineBreaks;
    }

    public void setReformatAgainToRemoveCustomLineBreaks(boolean reformatAgainToRemoveCustomLineBreaks) {
        this.reformatAgainToRemoveCustomLineBreaks = reformatAgainToRemoveCustomLineBreaks;
    }

    public boolean isAutoInsertClosingTagInJavaDoc() {
        return autoInsertClosingTagInJavaDoc;
    }

    public void setAutoInsertClosingTagInJavaDoc(boolean autoInsertClosingTagInJavaDoc) {
        this.autoInsertClosingTagInJavaDoc = autoInsertClosingTagInJavaDoc;
    }

    public boolean isInsertPairPercentOnEnterInJsp() {
        return insertPairPercentOnEnterInJsp;
    }

    public void setInsertPairPercentOnEnterInJsp(boolean insertPairPercentOnEnterInJsp) {
        this.insertPairPercentOnEnterInJsp = insertPairPercentOnEnterInJsp;
    }

    public boolean isConvertPastedJavaToKotlin() {
        return convertPastedJavaToKotlin;
    }

    public void setConvertPastedJavaToKotlin(boolean convertPastedJavaToKotlin) {
        this.convertPastedJavaToKotlin = convertPastedJavaToKotlin;
    }

    public boolean isDontShowJavaToKotlinDialogOnPaste() {
        return dontShowJavaToKotlinDialogOnPaste;
    }

    public void setDontShowJavaToKotlinDialogOnPaste(boolean dontShowJavaToKotlinDialogOnPaste) {
        this.dontShowJavaToKotlinDialogOnPaste = dontShowJavaToKotlinDialogOnPaste;
    }

    public boolean isAutoAddValKeywordToConstructorParams() {
        return autoAddValKeywordToConstructorParams;
    }

    public void setAutoAddValKeywordToConstructorParams(boolean autoAddValKeywordToConstructorParams) {
        this.autoAddValKeywordToConstructorParams = autoAddValKeywordToConstructorParams;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SmartKeysSettings that = (SmartKeysSettings) o;
        return !isModified(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                homeMovesCaretToFirstNonWhitespace, endOnBlankLineMovesCaretToIndent,
                insertPairedBrackets, insertPairQuote, reformatBlockOnTypingRBrace,
                useCamelHumpsWords, honorCamelHumpsOnDoubleClick, surroundSelectionOnQuoteOrBrace,
                addMultipleCaretsOnDoubleCtrlArrow, jumpOutsideClosingBracketOrQuoteWithTab,
                smartIndent, insertPairRBrace, closeBlockComment, insertDocCommentStub,
                unindentOnBackspace, reformatOnPaste, reformatAgainToRemoveCustomLineBreaks,
                autoInsertClosingTagInJavaDoc, insertPairPercentOnEnterInJsp,
                convertPastedJavaToKotlin, dontShowJavaToKotlinDialogOnPaste,
                autoAddValKeywordToConstructorParams
        );
    }
}
