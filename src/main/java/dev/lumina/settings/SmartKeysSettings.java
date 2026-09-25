package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration model for Editor > General > Smart Keys settings
 * and language-specific Smart Keys subpages.
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

    // --- YAML ---
    private boolean yamlAutoExpandKeySequencesOnPaste = true;

    // --- HTML/CSS ---
    private boolean xmlHtmlInsertClosingTag = true;
    private boolean xmlHtmlInsertRequiredAttributes = true;
    private boolean xmlHtmlInsertRequiredSubtags = true;
    private boolean xmlHtmlStartAttribute = true;
    private boolean xmlHtmlAddQuotesForAttributeValue = true;
    private boolean xmlHtmlAutoCloseTag = true;
    private boolean xmlHtmlSimultaneousTagEditing = true;
    private boolean cssSelectWholeCssIdentifiersOnDoubleClick = true;

    // --- Python ---
    private boolean pythonSmartIndentPastedLines = false;
    private boolean pythonUseParenthesesInsteadOfBackslashes = true;
    private boolean pythonInsertSelfWhenDefiningMethod = true;
    private boolean pythonInsertTypePlaceholdersInDocCommentStub = false;

    // --- JSON ---
    private boolean jsonInsertMissingCommaOnEnter = true;
    private boolean jsonInsertMissingCommaAfterMatchingBracesQuotes = true;
    private boolean jsonAutoManageCommasPastingFragments = true;
    private boolean jsonEscapeTextOnPasteInStringLiterals = true;
    private boolean jsonAutoAddQuotesToPropertyNamesOnColon = true;
    private boolean jsonAutoAddWhitespaceOnColonAfterProperty = true;
    private boolean jsonAutoMoveColonAfterPropertyNameInsideQuotes = false;
    private boolean jsonAutoMoveCommaAfterValueInsideQuotes = false;

    // --- Rust ---
    private boolean rustInsertPairedHashForRawStrings = true;

    // --- Markdown ---
    private boolean markdownReformatTable = true;
    private boolean markdownInsertHtmlBreakInsideTableCells = true;
    private boolean markdownUseShiftEnterForNewTableRow = true;
    private boolean markdownUseTabShiftTabToNavigateCells = true;
    private boolean markdownAdjustIndentationOnType = true;
    private boolean markdownSmartEnterAndBackspace = true;
    private boolean markdownRenumberListWhenTyping = false;
    private String markdownListNumerating = "Sequentially";
    private boolean markdownInsertLinksOnDrop = true;

    // --- Scala ---
    private boolean scalaIndentPastedLinesAtCaret = true;
    private boolean scalaInsertPairQuotesForMultilineString = true;
    private boolean scalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace = true;
    private boolean scalaWrapSingleExpressionBodyWithClosingBraceAfterBrace = true;
    private boolean scalaDeleteClosingBraceAfterDeletingBrace = true;
    private boolean scalaAddBracesAutomaticallyBasedOnIndentation = true;
    private boolean scalaRemoveBracesAutomaticallyBasedOnIndentation = false;

    // --- SQL ---
    private boolean sqlInsertStringConcatOnEnter = true;
    private boolean sqlCloseCodeBlocksOnEnter = true;

    // --- Ruby ---
    private boolean rubyContinueLineCommentsOnEnter = true;
    private boolean rubyDeleteEmptyLineCommentsOnEnter = true;
    private boolean rubyStartInterpolationOnTypingHash = false;

    // --- JavaScript ---
    private boolean jsReplaceStringLiteralOnTemplate = true;
    private boolean jsStartTemplateStringInterpolation = false;
    private boolean jsEscapeTextOnPasteInStringLiterals = true;
    private boolean jsCloseHtmlSingleTagsInJsx = true;
    private boolean jsConvertHtmlAttributeNamesInJsx = true;
    private boolean jsEscapeJsDocLeadingAsterisks = true;

    // --- PHP ---
    private boolean phpEnableSmartFunctionParametersCompletion = false;
    private boolean phpSelectVarWithoutDollarOnDoubleClick = false;
    private boolean phpRemovePhpOpenCloseTagsWhilePasting = true;
    private boolean phpEscapeSymbolsOnPasteInStringLiterals = false;
    private boolean phpReplaceUnnecessaryDoubleQuotesOnPaste = false;
    private boolean phpAutoInsertPhpTagAfterTyping = true;
    private boolean phpAutoInsertSemicolon = true;
    private boolean phpShowAdditionalOptionsSearchingMethodUsages = true;
    private boolean phpAutoInsertClosingHtmlTagInDoc = true;
    private boolean phpAutoInsertArrowOnTypingMinusAfterObject = true;
    private boolean phpSmartIndent = true;

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

        // YAML
        yamlAutoExpandKeySequencesOnPaste = true;

        // HTML/CSS
        xmlHtmlInsertClosingTag = true;
        xmlHtmlInsertRequiredAttributes = true;
        xmlHtmlInsertRequiredSubtags = true;
        xmlHtmlStartAttribute = true;
        xmlHtmlAddQuotesForAttributeValue = true;
        xmlHtmlAutoCloseTag = true;
        xmlHtmlSimultaneousTagEditing = true;
        cssSelectWholeCssIdentifiersOnDoubleClick = true;

        // Python
        pythonSmartIndentPastedLines = false;
        pythonUseParenthesesInsteadOfBackslashes = true;
        pythonInsertSelfWhenDefiningMethod = true;
        pythonInsertTypePlaceholdersInDocCommentStub = false;

        // JSON
        jsonInsertMissingCommaOnEnter = true;
        jsonInsertMissingCommaAfterMatchingBracesQuotes = true;
        jsonAutoManageCommasPastingFragments = true;
        jsonEscapeTextOnPasteInStringLiterals = true;
        jsonAutoAddQuotesToPropertyNamesOnColon = true;
        jsonAutoAddWhitespaceOnColonAfterProperty = true;
        jsonAutoMoveColonAfterPropertyNameInsideQuotes = false;
        jsonAutoMoveCommaAfterValueInsideQuotes = false;

        // Rust
        rustInsertPairedHashForRawStrings = true;

        // Markdown
        markdownReformatTable = true;
        markdownInsertHtmlBreakInsideTableCells = true;
        markdownUseShiftEnterForNewTableRow = true;
        markdownUseTabShiftTabToNavigateCells = true;
        markdownAdjustIndentationOnType = true;
        markdownSmartEnterAndBackspace = true;
        markdownRenumberListWhenTyping = false;
        markdownListNumerating = "Sequentially";
        markdownInsertLinksOnDrop = true;

        // Scala
        scalaIndentPastedLinesAtCaret = true;
        scalaInsertPairQuotesForMultilineString = true;
        scalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace = true;
        scalaWrapSingleExpressionBodyWithClosingBraceAfterBrace = true;
        scalaDeleteClosingBraceAfterDeletingBrace = true;
        scalaAddBracesAutomaticallyBasedOnIndentation = true;
        scalaRemoveBracesAutomaticallyBasedOnIndentation = false;

        // SQL
        sqlInsertStringConcatOnEnter = true;
        sqlCloseCodeBlocksOnEnter = true;

        // Ruby
        rubyContinueLineCommentsOnEnter = true;
        rubyDeleteEmptyLineCommentsOnEnter = true;
        rubyStartInterpolationOnTypingHash = false;

        // JavaScript
        jsReplaceStringLiteralOnTemplate = true;
        jsStartTemplateStringInterpolation = false;
        jsEscapeTextOnPasteInStringLiterals = true;
        jsCloseHtmlSingleTagsInJsx = true;
        jsConvertHtmlAttributeNamesInJsx = true;
        jsEscapeJsDocLeadingAsterisks = true;

        // PHP
        phpEnableSmartFunctionParametersCompletion = false;
        phpSelectVarWithoutDollarOnDoubleClick = false;
        phpRemovePhpOpenCloseTagsWhilePasting = true;
        phpEscapeSymbolsOnPasteInStringLiterals = false;
        phpReplaceUnnecessaryDoubleQuotesOnPaste = false;
        phpAutoInsertPhpTagAfterTyping = true;
        phpAutoInsertSemicolon = true;
        phpShowAdditionalOptionsSearchingMethodUsages = true;
        phpAutoInsertClosingHtmlTagInDoc = true;
        phpAutoInsertArrowOnTypingMinusAfterObject = true;
        phpSmartIndent = true;
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

        // YAML
        val = Settings.get("smartkeys.yaml.auto.expand.key.sequences.on.paste");
        if (val != null) yamlAutoExpandKeySequencesOnPaste = Boolean.parseBoolean(val);

        // HTML/CSS
        val = Settings.get("smartkeys.html.insert.closing.tag");
        if (val != null) xmlHtmlInsertClosingTag = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.html.insert.required.attributes");
        if (val != null) xmlHtmlInsertRequiredAttributes = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.html.insert.required.subtags");
        if (val != null) xmlHtmlInsertRequiredSubtags = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.html.start.attribute");
        if (val != null) xmlHtmlStartAttribute = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.html.add.quotes.for.attribute");
        if (val != null) xmlHtmlAddQuotesForAttributeValue = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.html.auto.close.tag");
        if (val != null) xmlHtmlAutoCloseTag = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.html.simultaneous.tag.editing");
        if (val != null) xmlHtmlSimultaneousTagEditing = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.css.select.whole.identifiers.double.click");
        if (val != null) cssSelectWholeCssIdentifiersOnDoubleClick = Boolean.parseBoolean(val);

        // Python
        val = Settings.get("smartkeys.python.smart.indent.pasted.lines");
        if (val != null) pythonSmartIndentPastedLines = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.python.use.parentheses.for.breaking.lines");
        if (val != null) pythonUseParenthesesInsteadOfBackslashes = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.python.insert.self.when.defining.method");
        if (val != null) pythonInsertSelfWhenDefiningMethod = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.python.insert.type.placeholders.doc.stub");
        if (val != null) pythonInsertTypePlaceholdersInDocCommentStub = Boolean.parseBoolean(val);

        // JSON
        val = Settings.get("smartkeys.json.insert.missing.comma.enter");
        if (val != null) jsonInsertMissingCommaOnEnter = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.json.insert.missing.comma.braces.quotes");
        if (val != null) jsonInsertMissingCommaAfterMatchingBracesQuotes = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.json.auto.manage.commas.paste");
        if (val != null) jsonAutoManageCommasPastingFragments = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.json.escape.text.paste.literals");
        if (val != null) jsonEscapeTextOnPasteInStringLiterals = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.json.auto.add.quotes.property.colon");
        if (val != null) jsonAutoAddQuotesToPropertyNamesOnColon = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.json.auto.add.whitespace.colon");
        if (val != null) jsonAutoAddWhitespaceOnColonAfterProperty = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.json.auto.move.colon.inside.quotes");
        if (val != null) jsonAutoMoveColonAfterPropertyNameInsideQuotes = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.json.auto.move.comma.inside.quotes");
        if (val != null) jsonAutoMoveCommaAfterValueInsideQuotes = Boolean.parseBoolean(val);

        // Rust
        val = Settings.get("smartkeys.rust.insert.paired.hash.raw.strings");
        if (val != null) rustInsertPairedHashForRawStrings = Boolean.parseBoolean(val);

        // Markdown
        val = Settings.get("smartkeys.markdown.reformat.table");
        if (val != null) markdownReformatTable = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.markdown.insert.html.break.table");
        if (val != null) markdownInsertHtmlBreakInsideTableCells = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.markdown.shift.enter.table.row");
        if (val != null) markdownUseShiftEnterForNewTableRow = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.markdown.tab.navigate.table");
        if (val != null) markdownUseTabShiftTabToNavigateCells = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.markdown.adjust.indentation.type");
        if (val != null) markdownAdjustIndentationOnType = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.markdown.smart.enter.backspace");
        if (val != null) markdownSmartEnterAndBackspace = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.markdown.renumber.list.typing");
        if (val != null) markdownRenumberListWhenTyping = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.markdown.list.numerating");
        if (val != null) markdownListNumerating = val;

        val = Settings.get("smartkeys.markdown.insert.links.drop");
        if (val != null) markdownInsertLinksOnDrop = Boolean.parseBoolean(val);

        // Scala
        val = Settings.get("smartkeys.scala.indent.pasted.lines.at.caret");
        if (val != null) scalaIndentPastedLinesAtCaret = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.scala.insert.pair.quotes.multiline.string");
        if (val != null) scalaInsertPairQuotesForMultilineString = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.scala.upgrade.simple.string.interpolated");
        if (val != null) scalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.scala.wrap.single.expression.body.closing.brace");
        if (val != null) scalaWrapSingleExpressionBodyWithClosingBraceAfterBrace = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.scala.delete.closing.brace.after.deleting");
        if (val != null) scalaDeleteClosingBraceAfterDeletingBrace = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.scala.add.braces.automatically");
        if (val != null) scalaAddBracesAutomaticallyBasedOnIndentation = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.scala.remove.braces.automatically");
        if (val != null) scalaRemoveBracesAutomaticallyBasedOnIndentation = Boolean.parseBoolean(val);

        // SQL
        val = Settings.get("smartkeys.sql.insert.string.concat.enter");
        if (val != null) sqlInsertStringConcatOnEnter = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.sql.close.code.blocks.enter");
        if (val != null) sqlCloseCodeBlocksOnEnter = Boolean.parseBoolean(val);

        // Ruby
        val = Settings.get("smartkeys.ruby.continue.line.comments.on.enter");
        if (val != null) rubyContinueLineCommentsOnEnter = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.ruby.delete.empty.line.comments.on.enter");
        if (val != null) rubyDeleteEmptyLineCommentsOnEnter = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.ruby.start.interpolation.on.typing.hash");
        if (val != null) rubyStartInterpolationOnTypingHash = Boolean.parseBoolean(val);

        // JavaScript
        val = Settings.get("smartkeys.js.replace.string.literal.template");
        if (val != null) jsReplaceStringLiteralOnTemplate = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.js.start.template.string.interpolation");
        if (val != null) jsStartTemplateStringInterpolation = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.js.escape.text.paste.literals");
        if (val != null) jsEscapeTextOnPasteInStringLiterals = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.js.close.html.single.tags.jsx");
        if (val != null) jsCloseHtmlSingleTagsInJsx = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.js.convert.html.attribute.names.jsx");
        if (val != null) jsConvertHtmlAttributeNamesInJsx = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.js.escape.jsdoc.asterisks");
        if (val != null) jsEscapeJsDocLeadingAsterisks = Boolean.parseBoolean(val);

        // PHP
        val = Settings.get("smartkeys.php.enable.smart.function.parameters.completion");
        if (val != null) phpEnableSmartFunctionParametersCompletion = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.php.select.var.without.dollar");
        if (val != null) phpSelectVarWithoutDollarOnDoubleClick = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.php.remove.open.close.tags.pasting");
        if (val != null) phpRemovePhpOpenCloseTagsWhilePasting = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.php.escape.symbols.paste.literals");
        if (val == null) val = Settings.get("smartkeys.php.escape.text.paste.literals");
        if (val != null) phpEscapeSymbolsOnPasteInStringLiterals = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.php.replace.quotes.paste");
        if (val != null) phpReplaceUnnecessaryDoubleQuotesOnPaste = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.php.auto.insert.tag");
        if (val != null) phpAutoInsertPhpTagAfterTyping = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.php.auto.insert.semicolon");
        if (val != null) phpAutoInsertSemicolon = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.php.show.additional.options.method.usages");
        if (val != null) phpShowAdditionalOptionsSearchingMethodUsages = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.php.auto.insert.closing.html.tag.doc");
        if (val != null) phpAutoInsertClosingHtmlTagInDoc = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.php.auto.insert.arrow");
        if (val != null) phpAutoInsertArrowOnTypingMinusAfterObject = Boolean.parseBoolean(val);

        val = Settings.get("smartkeys.php.smart.indent");
        if (val != null) phpSmartIndent = Boolean.parseBoolean(val);
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

        // YAML
        Settings.put("smartkeys.yaml.auto.expand.key.sequences.on.paste", String.valueOf(yamlAutoExpandKeySequencesOnPaste));

        // HTML/CSS
        Settings.put("smartkeys.html.insert.closing.tag", String.valueOf(xmlHtmlInsertClosingTag));
        Settings.put("smartkeys.html.insert.required.attributes", String.valueOf(xmlHtmlInsertRequiredAttributes));
        Settings.put("smartkeys.html.insert.required.subtags", String.valueOf(xmlHtmlInsertRequiredSubtags));
        Settings.put("smartkeys.html.start.attribute", String.valueOf(xmlHtmlStartAttribute));
        Settings.put("smartkeys.html.add.quotes.for.attribute", String.valueOf(xmlHtmlAddQuotesForAttributeValue));
        Settings.put("smartkeys.html.auto.close.tag", String.valueOf(xmlHtmlAutoCloseTag));
        Settings.put("smartkeys.html.simultaneous.tag.editing", String.valueOf(xmlHtmlSimultaneousTagEditing));
        Settings.put("smartkeys.css.select.whole.identifiers.double.click", String.valueOf(cssSelectWholeCssIdentifiersOnDoubleClick));

        // Python
        Settings.put("smartkeys.python.smart.indent.pasted.lines", String.valueOf(pythonSmartIndentPastedLines));
        Settings.put("smartkeys.python.use.parentheses.for.breaking.lines", String.valueOf(pythonUseParenthesesInsteadOfBackslashes));
        Settings.put("smartkeys.python.insert.self.when.defining.method", String.valueOf(pythonInsertSelfWhenDefiningMethod));
        Settings.put("smartkeys.python.insert.type.placeholders.doc.stub", String.valueOf(pythonInsertTypePlaceholdersInDocCommentStub));

        // JSON
        Settings.put("smartkeys.json.insert.missing.comma.enter", String.valueOf(jsonInsertMissingCommaOnEnter));
        Settings.put("smartkeys.json.insert.missing.comma.braces.quotes", String.valueOf(jsonInsertMissingCommaAfterMatchingBracesQuotes));
        Settings.put("smartkeys.json.auto.manage.commas.paste", String.valueOf(jsonAutoManageCommasPastingFragments));
        Settings.put("smartkeys.json.escape.text.paste.literals", String.valueOf(jsonEscapeTextOnPasteInStringLiterals));
        Settings.put("smartkeys.json.auto.add.quotes.property.colon", String.valueOf(jsonAutoAddQuotesToPropertyNamesOnColon));
        Settings.put("smartkeys.json.auto.add.whitespace.colon", String.valueOf(jsonAutoAddWhitespaceOnColonAfterProperty));
        Settings.put("smartkeys.json.auto.move.colon.inside.quotes", String.valueOf(jsonAutoMoveColonAfterPropertyNameInsideQuotes));
        Settings.put("smartkeys.json.auto.move.comma.inside.quotes", String.valueOf(jsonAutoMoveCommaAfterValueInsideQuotes));

        // Rust
        Settings.put("smartkeys.rust.insert.paired.hash.raw.strings", String.valueOf(rustInsertPairedHashForRawStrings));

        // Markdown
        Settings.put("smartkeys.markdown.reformat.table", String.valueOf(markdownReformatTable));
        Settings.put("smartkeys.markdown.insert.html.break.table", String.valueOf(markdownInsertHtmlBreakInsideTableCells));
        Settings.put("smartkeys.markdown.shift.enter.table.row", String.valueOf(markdownUseShiftEnterForNewTableRow));
        Settings.put("smartkeys.markdown.tab.navigate.table", String.valueOf(markdownUseTabShiftTabToNavigateCells));
        Settings.put("smartkeys.markdown.adjust.indentation.type", String.valueOf(markdownAdjustIndentationOnType));
        Settings.put("smartkeys.markdown.smart.enter.backspace", String.valueOf(markdownSmartEnterAndBackspace));
        Settings.put("smartkeys.markdown.renumber.list.typing", String.valueOf(markdownRenumberListWhenTyping));
        Settings.put("smartkeys.markdown.list.numerating", markdownListNumerating);
        Settings.put("smartkeys.markdown.insert.links.drop", String.valueOf(markdownInsertLinksOnDrop));

        // Scala
        Settings.put("smartkeys.scala.indent.pasted.lines.at.caret", String.valueOf(scalaIndentPastedLinesAtCaret));
        Settings.put("smartkeys.scala.insert.pair.quotes.multiline.string", String.valueOf(scalaInsertPairQuotesForMultilineString));
        Settings.put("smartkeys.scala.upgrade.simple.string.interpolated", String.valueOf(scalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace));
        Settings.put("smartkeys.scala.wrap.single.expression.body.closing.brace", String.valueOf(scalaWrapSingleExpressionBodyWithClosingBraceAfterBrace));
        Settings.put("smartkeys.scala.delete.closing.brace.after.deleting", String.valueOf(scalaDeleteClosingBraceAfterDeletingBrace));
        Settings.put("smartkeys.scala.add.braces.automatically", String.valueOf(scalaAddBracesAutomaticallyBasedOnIndentation));
        Settings.put("smartkeys.scala.remove.braces.automatically", String.valueOf(scalaRemoveBracesAutomaticallyBasedOnIndentation));

        // SQL
        Settings.put("smartkeys.sql.insert.string.concat.enter", String.valueOf(sqlInsertStringConcatOnEnter));
        Settings.put("smartkeys.sql.close.code.blocks.enter", String.valueOf(sqlCloseCodeBlocksOnEnter));

        // Ruby
        Settings.put("smartkeys.ruby.continue.line.comments.on.enter", String.valueOf(rubyContinueLineCommentsOnEnter));
        Settings.put("smartkeys.ruby.delete.empty.line.comments.on.enter", String.valueOf(rubyDeleteEmptyLineCommentsOnEnter));
        Settings.put("smartkeys.ruby.start.interpolation.on.typing.hash", String.valueOf(rubyStartInterpolationOnTypingHash));

        // JavaScript
        Settings.put("smartkeys.js.replace.string.literal.template", String.valueOf(jsReplaceStringLiteralOnTemplate));
        Settings.put("smartkeys.js.start.template.string.interpolation", String.valueOf(jsStartTemplateStringInterpolation));
        Settings.put("smartkeys.js.escape.text.paste.literals", String.valueOf(jsEscapeTextOnPasteInStringLiterals));
        Settings.put("smartkeys.js.close.html.single.tags.jsx", String.valueOf(jsCloseHtmlSingleTagsInJsx));
        Settings.put("smartkeys.js.convert.html.attribute.names.jsx", String.valueOf(jsConvertHtmlAttributeNamesInJsx));
        Settings.put("smartkeys.js.escape.jsdoc.asterisks", String.valueOf(jsEscapeJsDocLeadingAsterisks));

        // PHP
        Settings.put("smartkeys.php.enable.smart.function.parameters.completion", String.valueOf(phpEnableSmartFunctionParametersCompletion));
        Settings.put("smartkeys.php.select.var.without.dollar", String.valueOf(phpSelectVarWithoutDollarOnDoubleClick));
        Settings.put("smartkeys.php.remove.open.close.tags.pasting", String.valueOf(phpRemovePhpOpenCloseTagsWhilePasting));
        Settings.put("smartkeys.php.escape.symbols.paste.literals", String.valueOf(phpEscapeSymbolsOnPasteInStringLiterals));
        Settings.put("smartkeys.php.replace.quotes.paste", String.valueOf(phpReplaceUnnecessaryDoubleQuotesOnPaste));
        Settings.put("smartkeys.php.auto.insert.tag", String.valueOf(phpAutoInsertPhpTagAfterTyping));
        Settings.put("smartkeys.php.auto.insert.semicolon", String.valueOf(phpAutoInsertSemicolon));
        Settings.put("smartkeys.php.show.additional.options.method.usages", String.valueOf(phpShowAdditionalOptionsSearchingMethodUsages));
        Settings.put("smartkeys.php.auto.insert.closing.html.tag.doc", String.valueOf(phpAutoInsertClosingHtmlTagInDoc));
        Settings.put("smartkeys.php.auto.insert.arrow", String.valueOf(phpAutoInsertArrowOnTypingMinusAfterObject));
        Settings.put("smartkeys.php.smart.indent", String.valueOf(phpSmartIndent));

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

        // YAML
        this.yamlAutoExpandKeySequencesOnPaste = other.yamlAutoExpandKeySequencesOnPaste;

        // HTML/CSS
        this.xmlHtmlInsertClosingTag = other.xmlHtmlInsertClosingTag;
        this.xmlHtmlInsertRequiredAttributes = other.xmlHtmlInsertRequiredAttributes;
        this.xmlHtmlInsertRequiredSubtags = other.xmlHtmlInsertRequiredSubtags;
        this.xmlHtmlStartAttribute = other.xmlHtmlStartAttribute;
        this.xmlHtmlAddQuotesForAttributeValue = other.xmlHtmlAddQuotesForAttributeValue;
        this.xmlHtmlAutoCloseTag = other.xmlHtmlAutoCloseTag;
        this.xmlHtmlSimultaneousTagEditing = other.xmlHtmlSimultaneousTagEditing;
        this.cssSelectWholeCssIdentifiersOnDoubleClick = other.cssSelectWholeCssIdentifiersOnDoubleClick;

        // Python
        this.pythonSmartIndentPastedLines = other.pythonSmartIndentPastedLines;
        this.pythonUseParenthesesInsteadOfBackslashes = other.pythonUseParenthesesInsteadOfBackslashes;
        this.pythonInsertSelfWhenDefiningMethod = other.pythonInsertSelfWhenDefiningMethod;
        this.pythonInsertTypePlaceholdersInDocCommentStub = other.pythonInsertTypePlaceholdersInDocCommentStub;

        // JSON
        this.jsonInsertMissingCommaOnEnter = other.jsonInsertMissingCommaOnEnter;
        this.jsonInsertMissingCommaAfterMatchingBracesQuotes = other.jsonInsertMissingCommaAfterMatchingBracesQuotes;
        this.jsonAutoManageCommasPastingFragments = other.jsonAutoManageCommasPastingFragments;
        this.jsonEscapeTextOnPasteInStringLiterals = other.jsonEscapeTextOnPasteInStringLiterals;
        this.jsonAutoAddQuotesToPropertyNamesOnColon = other.jsonAutoAddQuotesToPropertyNamesOnColon;
        this.jsonAutoAddWhitespaceOnColonAfterProperty = other.jsonAutoAddWhitespaceOnColonAfterProperty;
        this.jsonAutoMoveColonAfterPropertyNameInsideQuotes = other.jsonAutoMoveColonAfterPropertyNameInsideQuotes;
        this.jsonAutoMoveCommaAfterValueInsideQuotes = other.jsonAutoMoveCommaAfterValueInsideQuotes;

        // Rust
        this.rustInsertPairedHashForRawStrings = other.rustInsertPairedHashForRawStrings;

        // Markdown
        this.markdownReformatTable = other.markdownReformatTable;
        this.markdownInsertHtmlBreakInsideTableCells = other.markdownInsertHtmlBreakInsideTableCells;
        this.markdownUseShiftEnterForNewTableRow = other.markdownUseShiftEnterForNewTableRow;
        this.markdownUseTabShiftTabToNavigateCells = other.markdownUseTabShiftTabToNavigateCells;
        this.markdownAdjustIndentationOnType = other.markdownAdjustIndentationOnType;
        this.markdownSmartEnterAndBackspace = other.markdownSmartEnterAndBackspace;
        this.markdownRenumberListWhenTyping = other.markdownRenumberListWhenTyping;
        this.markdownListNumerating = other.markdownListNumerating;
        this.markdownInsertLinksOnDrop = other.markdownInsertLinksOnDrop;

        // Scala
        this.scalaIndentPastedLinesAtCaret = other.scalaIndentPastedLinesAtCaret;
        this.scalaInsertPairQuotesForMultilineString = other.scalaInsertPairQuotesForMultilineString;
        this.scalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace = other.scalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace;
        this.scalaWrapSingleExpressionBodyWithClosingBraceAfterBrace = other.scalaWrapSingleExpressionBodyWithClosingBraceAfterBrace;
        this.scalaDeleteClosingBraceAfterDeletingBrace = other.scalaDeleteClosingBraceAfterDeletingBrace;
        this.scalaAddBracesAutomaticallyBasedOnIndentation = other.scalaAddBracesAutomaticallyBasedOnIndentation;
        this.scalaRemoveBracesAutomaticallyBasedOnIndentation = other.scalaRemoveBracesAutomaticallyBasedOnIndentation;

        // SQL
        this.sqlInsertStringConcatOnEnter = other.sqlInsertStringConcatOnEnter;
        this.sqlCloseCodeBlocksOnEnter = other.sqlCloseCodeBlocksOnEnter;

        // Ruby
        this.rubyContinueLineCommentsOnEnter = other.rubyContinueLineCommentsOnEnter;
        this.rubyDeleteEmptyLineCommentsOnEnter = other.rubyDeleteEmptyLineCommentsOnEnter;
        this.rubyStartInterpolationOnTypingHash = other.rubyStartInterpolationOnTypingHash;

        // JavaScript
        this.jsReplaceStringLiteralOnTemplate = other.jsReplaceStringLiteralOnTemplate;
        this.jsStartTemplateStringInterpolation = other.jsStartTemplateStringInterpolation;
        this.jsEscapeTextOnPasteInStringLiterals = other.jsEscapeTextOnPasteInStringLiterals;
        this.jsCloseHtmlSingleTagsInJsx = other.jsCloseHtmlSingleTagsInJsx;
        this.jsConvertHtmlAttributeNamesInJsx = other.jsConvertHtmlAttributeNamesInJsx;
        this.jsEscapeJsDocLeadingAsterisks = other.jsEscapeJsDocLeadingAsterisks;

        // PHP
        this.phpEnableSmartFunctionParametersCompletion = other.phpEnableSmartFunctionParametersCompletion;
        this.phpSelectVarWithoutDollarOnDoubleClick = other.phpSelectVarWithoutDollarOnDoubleClick;
        this.phpRemovePhpOpenCloseTagsWhilePasting = other.phpRemovePhpOpenCloseTagsWhilePasting;
        this.phpEscapeSymbolsOnPasteInStringLiterals = other.phpEscapeSymbolsOnPasteInStringLiterals;
        this.phpReplaceUnnecessaryDoubleQuotesOnPaste = other.phpReplaceUnnecessaryDoubleQuotesOnPaste;
        this.phpAutoInsertPhpTagAfterTyping = other.phpAutoInsertPhpTagAfterTyping;
        this.phpAutoInsertSemicolon = other.phpAutoInsertSemicolon;
        this.phpShowAdditionalOptionsSearchingMethodUsages = other.phpShowAdditionalOptionsSearchingMethodUsages;
        this.phpAutoInsertClosingHtmlTagInDoc = other.phpAutoInsertClosingHtmlTagInDoc;
        this.phpAutoInsertArrowOnTypingMinusAfterObject = other.phpAutoInsertArrowOnTypingMinusAfterObject;
        this.phpSmartIndent = other.phpSmartIndent;
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
                || this.autoAddValKeywordToConstructorParams != other.autoAddValKeywordToConstructorParams
                // YAML
                || this.yamlAutoExpandKeySequencesOnPaste != other.yamlAutoExpandKeySequencesOnPaste
                // HTML/CSS
                || this.xmlHtmlInsertClosingTag != other.xmlHtmlInsertClosingTag
                || this.xmlHtmlInsertRequiredAttributes != other.xmlHtmlInsertRequiredAttributes
                || this.xmlHtmlInsertRequiredSubtags != other.xmlHtmlInsertRequiredSubtags
                || this.xmlHtmlStartAttribute != other.xmlHtmlStartAttribute
                || this.xmlHtmlAddQuotesForAttributeValue != other.xmlHtmlAddQuotesForAttributeValue
                || this.xmlHtmlAutoCloseTag != other.xmlHtmlAutoCloseTag
                || this.xmlHtmlSimultaneousTagEditing != other.xmlHtmlSimultaneousTagEditing
                || this.cssSelectWholeCssIdentifiersOnDoubleClick != other.cssSelectWholeCssIdentifiersOnDoubleClick
                // Python
                || this.pythonSmartIndentPastedLines != other.pythonSmartIndentPastedLines
                || this.pythonUseParenthesesInsteadOfBackslashes != other.pythonUseParenthesesInsteadOfBackslashes
                || this.pythonInsertSelfWhenDefiningMethod != other.pythonInsertSelfWhenDefiningMethod
                || this.pythonInsertTypePlaceholdersInDocCommentStub != other.pythonInsertTypePlaceholdersInDocCommentStub
                // JSON
                || this.jsonInsertMissingCommaOnEnter != other.jsonInsertMissingCommaOnEnter
                || this.jsonInsertMissingCommaAfterMatchingBracesQuotes != other.jsonInsertMissingCommaAfterMatchingBracesQuotes
                || this.jsonAutoManageCommasPastingFragments != other.jsonAutoManageCommasPastingFragments
                || this.jsonEscapeTextOnPasteInStringLiterals != other.jsonEscapeTextOnPasteInStringLiterals
                || this.jsonAutoAddQuotesToPropertyNamesOnColon != other.jsonAutoAddQuotesToPropertyNamesOnColon
                || this.jsonAutoAddWhitespaceOnColonAfterProperty != other.jsonAutoAddWhitespaceOnColonAfterProperty
                || this.jsonAutoMoveColonAfterPropertyNameInsideQuotes != other.jsonAutoMoveColonAfterPropertyNameInsideQuotes
                || this.jsonAutoMoveCommaAfterValueInsideQuotes != other.jsonAutoMoveCommaAfterValueInsideQuotes
                // Rust
                || this.rustInsertPairedHashForRawStrings != other.rustInsertPairedHashForRawStrings
                // Markdown
                || this.markdownReformatTable != other.markdownReformatTable
                || this.markdownInsertHtmlBreakInsideTableCells != other.markdownInsertHtmlBreakInsideTableCells
                || this.markdownUseShiftEnterForNewTableRow != other.markdownUseShiftEnterForNewTableRow
                || this.markdownUseTabShiftTabToNavigateCells != other.markdownUseTabShiftTabToNavigateCells
                || this.markdownAdjustIndentationOnType != other.markdownAdjustIndentationOnType
                || this.markdownSmartEnterAndBackspace != other.markdownSmartEnterAndBackspace
                || this.markdownRenumberListWhenTyping != other.markdownRenumberListWhenTyping
                || !Objects.equals(this.markdownListNumerating, other.markdownListNumerating)
                || this.markdownInsertLinksOnDrop != other.markdownInsertLinksOnDrop
                // Scala
                || this.scalaIndentPastedLinesAtCaret != other.scalaIndentPastedLinesAtCaret
                || this.scalaInsertPairQuotesForMultilineString != other.scalaInsertPairQuotesForMultilineString
                || this.scalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace != other.scalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace
                || this.scalaWrapSingleExpressionBodyWithClosingBraceAfterBrace != other.scalaWrapSingleExpressionBodyWithClosingBraceAfterBrace
                || this.scalaDeleteClosingBraceAfterDeletingBrace != other.scalaDeleteClosingBraceAfterDeletingBrace
                || this.scalaAddBracesAutomaticallyBasedOnIndentation != other.scalaAddBracesAutomaticallyBasedOnIndentation
                || this.scalaRemoveBracesAutomaticallyBasedOnIndentation != other.scalaRemoveBracesAutomaticallyBasedOnIndentation
                // SQL
                || this.sqlInsertStringConcatOnEnter != other.sqlInsertStringConcatOnEnter
                || this.sqlCloseCodeBlocksOnEnter != other.sqlCloseCodeBlocksOnEnter
                // Ruby
                || this.rubyContinueLineCommentsOnEnter != other.rubyContinueLineCommentsOnEnter
                || this.rubyDeleteEmptyLineCommentsOnEnter != other.rubyDeleteEmptyLineCommentsOnEnter
                || this.rubyStartInterpolationOnTypingHash != other.rubyStartInterpolationOnTypingHash
                // JavaScript
                || this.jsReplaceStringLiteralOnTemplate != other.jsReplaceStringLiteralOnTemplate
                || this.jsStartTemplateStringInterpolation != other.jsStartTemplateStringInterpolation
                || this.jsEscapeTextOnPasteInStringLiterals != other.jsEscapeTextOnPasteInStringLiterals
                || this.jsCloseHtmlSingleTagsInJsx != other.jsCloseHtmlSingleTagsInJsx
                || this.jsConvertHtmlAttributeNamesInJsx != other.jsConvertHtmlAttributeNamesInJsx
                || this.jsEscapeJsDocLeadingAsterisks != other.jsEscapeJsDocLeadingAsterisks
                // PHP
                || this.phpEnableSmartFunctionParametersCompletion != other.phpEnableSmartFunctionParametersCompletion
                || this.phpSelectVarWithoutDollarOnDoubleClick != other.phpSelectVarWithoutDollarOnDoubleClick
                || this.phpRemovePhpOpenCloseTagsWhilePasting != other.phpRemovePhpOpenCloseTagsWhilePasting
                || this.phpEscapeSymbolsOnPasteInStringLiterals != other.phpEscapeSymbolsOnPasteInStringLiterals
                || this.phpReplaceUnnecessaryDoubleQuotesOnPaste != other.phpReplaceUnnecessaryDoubleQuotesOnPaste
                || this.phpAutoInsertPhpTagAfterTyping != other.phpAutoInsertPhpTagAfterTyping
                || this.phpAutoInsertSemicolon != other.phpAutoInsertSemicolon
                || this.phpShowAdditionalOptionsSearchingMethodUsages != other.phpShowAdditionalOptionsSearchingMethodUsages
                || this.phpAutoInsertClosingHtmlTagInDoc != other.phpAutoInsertClosingHtmlTagInDoc
                || this.phpAutoInsertArrowOnTypingMinusAfterObject != other.phpAutoInsertArrowOnTypingMinusAfterObject
                || this.phpSmartIndent != other.phpSmartIndent;
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

    public boolean isHomeMovesCaretToFirstNonWhitespace() { return homeMovesCaretToFirstNonWhitespace; }
    public void setHomeMovesCaretToFirstNonWhitespace(boolean val) { this.homeMovesCaretToFirstNonWhitespace = val; }

    public boolean isEndOnBlankLineMovesCaretToIndent() { return endOnBlankLineMovesCaretToIndent; }
    public void setEndOnBlankLineMovesCaretToIndent(boolean val) { this.endOnBlankLineMovesCaretToIndent = val; }

    public boolean isInsertPairedBrackets() { return insertPairedBrackets; }
    public void setInsertPairedBrackets(boolean val) { this.insertPairedBrackets = val; }

    public boolean isInsertPairQuote() { return insertPairQuote; }
    public void setInsertPairQuote(boolean val) { this.insertPairQuote = val; }

    public boolean isReformatBlockOnTypingRBrace() { return reformatBlockOnTypingRBrace; }
    public void setReformatBlockOnTypingRBrace(boolean val) { this.reformatBlockOnTypingRBrace = val; }

    public boolean isUseCamelHumpsWords() { return useCamelHumpsWords; }
    public void setUseCamelHumpsWords(boolean val) { this.useCamelHumpsWords = val; }

    public boolean isHonorCamelHumpsOnDoubleClick() { return honorCamelHumpsOnDoubleClick; }
    public void setHonorCamelHumpsOnDoubleClick(boolean val) { this.honorCamelHumpsOnDoubleClick = val; }

    public boolean isSurroundSelectionOnQuoteOrBrace() { return surroundSelectionOnQuoteOrBrace; }
    public void setSurroundSelectionOnQuoteOrBrace(boolean val) { this.surroundSelectionOnQuoteOrBrace = val; }

    public boolean isAddMultipleCaretsOnDoubleCtrlArrow() { return addMultipleCaretsOnDoubleCtrlArrow; }
    public void setAddMultipleCaretsOnDoubleCtrlArrow(boolean val) { this.addMultipleCaretsOnDoubleCtrlArrow = val; }

    public boolean isJumpOutsideClosingBracketOrQuoteWithTab() { return jumpOutsideClosingBracketOrQuoteWithTab; }
    public void setJumpOutsideClosingBracketOrQuoteWithTab(boolean val) { this.jumpOutsideClosingBracketOrQuoteWithTab = val; }

    public boolean isSmartIndent() { return smartIndent; }
    public void setSmartIndent(boolean val) { this.smartIndent = val; }

    public boolean isInsertPairRBrace() { return insertPairRBrace; }
    public void setInsertPairRBrace(boolean val) { this.insertPairRBrace = val; }

    public boolean isCloseBlockComment() { return closeBlockComment; }
    public void setCloseBlockComment(boolean val) { this.closeBlockComment = val; }

    public boolean isInsertDocCommentStub() { return insertDocCommentStub; }
    public void setInsertDocCommentStub(boolean val) { this.insertDocCommentStub = val; }

    public UnindentOnBackspace getUnindentOnBackspace() { return unindentOnBackspace; }
    public void setUnindentOnBackspace(UnindentOnBackspace val) { this.unindentOnBackspace = val != null ? val : UnindentOnBackspace.TO_PROPER_INDENT; }

    public ReformatOnPaste getReformatOnPaste() { return reformatOnPaste; }
    public void setReformatOnPaste(ReformatOnPaste val) { this.reformatOnPaste = val != null ? val : ReformatOnPaste.INDENT_EACH_LINE; }

    public boolean isReformatAgainToRemoveCustomLineBreaks() { return reformatAgainToRemoveCustomLineBreaks; }
    public void setReformatAgainToRemoveCustomLineBreaks(boolean val) { this.reformatAgainToRemoveCustomLineBreaks = val; }

    public boolean isAutoInsertClosingTagInJavaDoc() { return autoInsertClosingTagInJavaDoc; }
    public void setAutoInsertClosingTagInJavaDoc(boolean val) { this.autoInsertClosingTagInJavaDoc = val; }

    public boolean isInsertPairPercentOnEnterInJsp() { return insertPairPercentOnEnterInJsp; }
    public void setInsertPairPercentOnEnterInJsp(boolean val) { this.insertPairPercentOnEnterInJsp = val; }

    public boolean isConvertPastedJavaToKotlin() { return convertPastedJavaToKotlin; }
    public void setConvertPastedJavaToKotlin(boolean val) { this.convertPastedJavaToKotlin = val; }

    public boolean isDontShowJavaToKotlinDialogOnPaste() { return dontShowJavaToKotlinDialogOnPaste; }
    public void setDontShowJavaToKotlinDialogOnPaste(boolean val) { this.dontShowJavaToKotlinDialogOnPaste = val; }

    public boolean isAutoAddValKeywordToConstructorParams() { return autoAddValKeywordToConstructorParams; }
    public void setAutoAddValKeywordToConstructorParams(boolean val) { this.autoAddValKeywordToConstructorParams = val; }

    // YAML
    public boolean isYamlAutoExpandKeySequencesOnPaste() { return yamlAutoExpandKeySequencesOnPaste; }
    public void setYamlAutoExpandKeySequencesOnPaste(boolean val) { this.yamlAutoExpandKeySequencesOnPaste = val; }

    // HTML/CSS
    public boolean isXmlHtmlInsertClosingTag() { return xmlHtmlInsertClosingTag; }
    public void setXmlHtmlInsertClosingTag(boolean val) { this.xmlHtmlInsertClosingTag = val; }

    public boolean isXmlHtmlInsertRequiredAttributes() { return xmlHtmlInsertRequiredAttributes; }
    public void setXmlHtmlInsertRequiredAttributes(boolean val) { this.xmlHtmlInsertRequiredAttributes = val; }

    public boolean isXmlHtmlInsertRequiredSubtags() { return xmlHtmlInsertRequiredSubtags; }
    public void setXmlHtmlInsertRequiredSubtags(boolean val) { this.xmlHtmlInsertRequiredSubtags = val; }

    public boolean isXmlHtmlStartAttribute() { return xmlHtmlStartAttribute; }
    public void setXmlHtmlStartAttribute(boolean val) { this.xmlHtmlStartAttribute = val; }

    public boolean isXmlHtmlAddQuotesForAttributeValue() { return xmlHtmlAddQuotesForAttributeValue; }
    public void setXmlHtmlAddQuotesForAttributeValue(boolean val) { this.xmlHtmlAddQuotesForAttributeValue = val; }

    public boolean isXmlHtmlAutoCloseTag() { return xmlHtmlAutoCloseTag; }
    public void setXmlHtmlAutoCloseTag(boolean val) { this.xmlHtmlAutoCloseTag = val; }

    public boolean isXmlHtmlSimultaneousTagEditing() { return xmlHtmlSimultaneousTagEditing; }
    public void setXmlHtmlSimultaneousTagEditing(boolean val) { this.xmlHtmlSimultaneousTagEditing = val; }

    public boolean isCssSelectWholeCssIdentifiersOnDoubleClick() { return cssSelectWholeCssIdentifiersOnDoubleClick; }
    public void setCssSelectWholeCssIdentifiersOnDoubleClick(boolean val) { this.cssSelectWholeCssIdentifiersOnDoubleClick = val; }

    // Python
    public boolean isPythonSmartIndentPastedLines() { return pythonSmartIndentPastedLines; }
    public void setPythonSmartIndentPastedLines(boolean val) { this.pythonSmartIndentPastedLines = val; }

    public boolean isPythonUseParenthesesInsteadOfBackslashes() { return pythonUseParenthesesInsteadOfBackslashes; }
    public void setPythonUseParenthesesInsteadOfBackslashes(boolean val) { this.pythonUseParenthesesInsteadOfBackslashes = val; }

    public boolean isPythonInsertSelfWhenDefiningMethod() { return pythonInsertSelfWhenDefiningMethod; }
    public void setPythonInsertSelfWhenDefiningMethod(boolean val) { this.pythonInsertSelfWhenDefiningMethod = val; }

    public boolean isPythonInsertTypePlaceholdersInDocCommentStub() { return pythonInsertTypePlaceholdersInDocCommentStub; }
    public void setPythonInsertTypePlaceholdersInDocCommentStub(boolean val) { this.pythonInsertTypePlaceholdersInDocCommentStub = val; }

    // JSON
    public boolean isJsonInsertMissingCommaOnEnter() { return jsonInsertMissingCommaOnEnter; }
    public void setJsonInsertMissingCommaOnEnter(boolean val) { this.jsonInsertMissingCommaOnEnter = val; }

    public boolean isJsonInsertMissingCommaAfterMatchingBracesQuotes() { return jsonInsertMissingCommaAfterMatchingBracesQuotes; }
    public void setJsonInsertMissingCommaAfterMatchingBracesQuotes(boolean val) { this.jsonInsertMissingCommaAfterMatchingBracesQuotes = val; }

    public boolean isJsonAutoManageCommasPastingFragments() { return jsonAutoManageCommasPastingFragments; }
    public void setJsonAutoManageCommasPastingFragments(boolean val) { this.jsonAutoManageCommasPastingFragments = val; }

    public boolean isJsonEscapeTextOnPasteInStringLiterals() { return jsonEscapeTextOnPasteInStringLiterals; }
    public void setJsonEscapeTextOnPasteInStringLiterals(boolean val) { this.jsonEscapeTextOnPasteInStringLiterals = val; }

    public boolean isJsonAutoAddQuotesToPropertyNamesOnColon() { return jsonAutoAddQuotesToPropertyNamesOnColon; }
    public void setJsonAutoAddQuotesToPropertyNamesOnColon(boolean val) { this.jsonAutoAddQuotesToPropertyNamesOnColon = val; }

    public boolean isJsonAutoAddWhitespaceOnColonAfterProperty() { return jsonAutoAddWhitespaceOnColonAfterProperty; }
    public void setJsonAutoAddWhitespaceOnColonAfterProperty(boolean val) { this.jsonAutoAddWhitespaceOnColonAfterProperty = val; }

    public boolean isJsonAutoMoveColonAfterPropertyNameInsideQuotes() { return jsonAutoMoveColonAfterPropertyNameInsideQuotes; }
    public void setJsonAutoMoveColonAfterPropertyNameInsideQuotes(boolean val) { this.jsonAutoMoveColonAfterPropertyNameInsideQuotes = val; }

    public boolean isJsonAutoMoveCommaAfterValueInsideQuotes() { return jsonAutoMoveCommaAfterValueInsideQuotes; }
    public void setJsonAutoMoveCommaAfterValueInsideQuotes(boolean val) { this.jsonAutoMoveCommaAfterValueInsideQuotes = val; }

    // Rust
    public boolean isRustInsertPairedHashForRawStrings() { return rustInsertPairedHashForRawStrings; }
    public void setRustInsertPairedHashForRawStrings(boolean val) { this.rustInsertPairedHashForRawStrings = val; }

    // Markdown
    public boolean isMarkdownReformatTable() { return markdownReformatTable; }
    public void setMarkdownReformatTable(boolean val) { this.markdownReformatTable = val; }

    public boolean isMarkdownInsertHtmlBreakInsideTableCells() { return markdownInsertHtmlBreakInsideTableCells; }
    public void setMarkdownInsertHtmlBreakInsideTableCells(boolean val) { this.markdownInsertHtmlBreakInsideTableCells = val; }

    public boolean isMarkdownUseShiftEnterForNewTableRow() { return markdownUseShiftEnterForNewTableRow; }
    public void setMarkdownUseShiftEnterForNewTableRow(boolean val) { this.markdownUseShiftEnterForNewTableRow = val; }

    public boolean isMarkdownUseTabShiftTabToNavigateCells() { return markdownUseTabShiftTabToNavigateCells; }
    public void setMarkdownUseTabShiftTabToNavigateCells(boolean val) { this.markdownUseTabShiftTabToNavigateCells = val; }

    public boolean isMarkdownAdjustIndentationOnType() { return markdownAdjustIndentationOnType; }
    public void setMarkdownAdjustIndentationOnType(boolean val) { this.markdownAdjustIndentationOnType = val; }

    public boolean isMarkdownSmartEnterAndBackspace() { return markdownSmartEnterAndBackspace; }
    public void setMarkdownSmartEnterAndBackspace(boolean val) { this.markdownSmartEnterAndBackspace = val; }

    public boolean isMarkdownRenumberListWhenTyping() { return markdownRenumberListWhenTyping; }
    public void setMarkdownRenumberListWhenTyping(boolean val) { this.markdownRenumberListWhenTyping = val; }

    public String getMarkdownListNumerating() { return markdownListNumerating; }
    public void setMarkdownListNumerating(String val) { this.markdownListNumerating = val != null ? val : "Sequentially"; }

    public boolean isMarkdownInsertLinksOnDrop() { return markdownInsertLinksOnDrop; }
    public void setMarkdownInsertLinksOnDrop(boolean val) { this.markdownInsertLinksOnDrop = val; }

    // Scala
    public boolean isScalaIndentPastedLinesAtCaret() { return scalaIndentPastedLinesAtCaret; }
    public void setScalaIndentPastedLinesAtCaret(boolean val) { this.scalaIndentPastedLinesAtCaret = val; }

    public boolean isScalaInsertPairQuotesForMultilineString() { return scalaInsertPairQuotesForMultilineString; }
    public void setScalaInsertPairQuotesForMultilineString(boolean val) { this.scalaInsertPairQuotesForMultilineString = val; }

    public boolean isScalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace() { return scalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace; }
    public void setScalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace(boolean val) { this.scalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace = val; }

    public boolean isScalaWrapSingleExpressionBodyWithClosingBraceAfterBrace() { return scalaWrapSingleExpressionBodyWithClosingBraceAfterBrace; }
    public void setScalaWrapSingleExpressionBodyWithClosingBraceAfterBrace(boolean val) { this.scalaWrapSingleExpressionBodyWithClosingBraceAfterBrace = val; }

    public boolean isScalaDeleteClosingBraceAfterDeletingBrace() { return scalaDeleteClosingBraceAfterDeletingBrace; }
    public void setScalaDeleteClosingBraceAfterDeletingBrace(boolean val) { this.scalaDeleteClosingBraceAfterDeletingBrace = val; }

    public boolean isScalaAddBracesAutomaticallyBasedOnIndentation() { return scalaAddBracesAutomaticallyBasedOnIndentation; }
    public void setScalaAddBracesAutomaticallyBasedOnIndentation(boolean val) { this.scalaAddBracesAutomaticallyBasedOnIndentation = val; }

    public boolean isScalaRemoveBracesAutomaticallyBasedOnIndentation() { return scalaRemoveBracesAutomaticallyBasedOnIndentation; }
    public void setScalaRemoveBracesAutomaticallyBasedOnIndentation(boolean val) { this.scalaRemoveBracesAutomaticallyBasedOnIndentation = val; }

    // SQL
    public boolean isSqlInsertStringConcatOnEnter() { return sqlInsertStringConcatOnEnter; }
    public void setSqlInsertStringConcatOnEnter(boolean val) { this.sqlInsertStringConcatOnEnter = val; }

    public boolean isSqlCloseCodeBlocksOnEnter() { return sqlCloseCodeBlocksOnEnter; }
    public void setSqlCloseCodeBlocksOnEnter(boolean val) { this.sqlCloseCodeBlocksOnEnter = val; }

    // Ruby
    public boolean isRubyContinueLineCommentsOnEnter() { return rubyContinueLineCommentsOnEnter; }
    public void setRubyContinueLineCommentsOnEnter(boolean val) { this.rubyContinueLineCommentsOnEnter = val; }

    public boolean isRubyDeleteEmptyLineCommentsOnEnter() { return rubyDeleteEmptyLineCommentsOnEnter; }
    public void setRubyDeleteEmptyLineCommentsOnEnter(boolean val) { this.rubyDeleteEmptyLineCommentsOnEnter = val; }

    public boolean isRubyStartInterpolationOnTypingHash() { return rubyStartInterpolationOnTypingHash; }
    public void setRubyStartInterpolationOnTypingHash(boolean val) { this.rubyStartInterpolationOnTypingHash = val; }

    // JavaScript
    public boolean isJsReplaceStringLiteralOnTemplate() { return jsReplaceStringLiteralOnTemplate; }
    public void setJsReplaceStringLiteralOnTemplate(boolean val) { this.jsReplaceStringLiteralOnTemplate = val; }

    public boolean isJsStartTemplateStringInterpolation() { return jsStartTemplateStringInterpolation; }
    public void setJsStartTemplateStringInterpolation(boolean val) { this.jsStartTemplateStringInterpolation = val; }

    public boolean isJsEscapeTextOnPasteInStringLiterals() { return jsEscapeTextOnPasteInStringLiterals; }
    public void setJsEscapeTextOnPasteInStringLiterals(boolean val) { this.jsEscapeTextOnPasteInStringLiterals = val; }

    public boolean isJsCloseHtmlSingleTagsInJsx() { return jsCloseHtmlSingleTagsInJsx; }
    public void setJsCloseHtmlSingleTagsInJsx(boolean val) { this.jsCloseHtmlSingleTagsInJsx = val; }

    public boolean isJsConvertHtmlAttributeNamesInJsx() { return jsConvertHtmlAttributeNamesInJsx; }
    public void setJsConvertHtmlAttributeNamesInJsx(boolean val) { this.jsConvertHtmlAttributeNamesInJsx = val; }

    public boolean isJsEscapeJsDocLeadingAsterisks() { return jsEscapeJsDocLeadingAsterisks; }
    public void setJsEscapeJsDocLeadingAsterisks(boolean val) { this.jsEscapeJsDocLeadingAsterisks = val; }

    // PHP
    public boolean isPhpEnableSmartFunctionParametersCompletion() { return phpEnableSmartFunctionParametersCompletion; }
    public void setPhpEnableSmartFunctionParametersCompletion(boolean val) { this.phpEnableSmartFunctionParametersCompletion = val; }

    public boolean isPhpSelectVarWithoutDollarOnDoubleClick() { return phpSelectVarWithoutDollarOnDoubleClick; }
    public void setPhpSelectVarWithoutDollarOnDoubleClick(boolean val) { this.phpSelectVarWithoutDollarOnDoubleClick = val; }

    public boolean isPhpRemovePhpOpenCloseTagsWhilePasting() { return phpRemovePhpOpenCloseTagsWhilePasting; }
    public void setPhpRemovePhpOpenCloseTagsWhilePasting(boolean val) { this.phpRemovePhpOpenCloseTagsWhilePasting = val; }

    public boolean isPhpEscapeSymbolsOnPasteInStringLiterals() { return phpEscapeSymbolsOnPasteInStringLiterals; }
    public void setPhpEscapeSymbolsOnPasteInStringLiterals(boolean val) { this.phpEscapeSymbolsOnPasteInStringLiterals = val; }
    public boolean isPhpEscapeTextOnPasteInStringLiterals() { return phpEscapeSymbolsOnPasteInStringLiterals; }
    public void setPhpEscapeTextOnPasteInStringLiterals(boolean val) { this.phpEscapeSymbolsOnPasteInStringLiterals = val; }

    public boolean isPhpReplaceUnnecessaryDoubleQuotesOnPaste() { return phpReplaceUnnecessaryDoubleQuotesOnPaste; }
    public void setPhpReplaceUnnecessaryDoubleQuotesOnPaste(boolean val) { this.phpReplaceUnnecessaryDoubleQuotesOnPaste = val; }

    public boolean isPhpAutoInsertPhpTagAfterTyping() { return phpAutoInsertPhpTagAfterTyping; }
    public void setPhpAutoInsertPhpTagAfterTyping(boolean val) { this.phpAutoInsertPhpTagAfterTyping = val; }

    public boolean isPhpAutoInsertSemicolon() { return phpAutoInsertSemicolon; }
    public void setPhpAutoInsertSemicolon(boolean val) { this.phpAutoInsertSemicolon = val; }

    public boolean isPhpShowAdditionalOptionsSearchingMethodUsages() { return phpShowAdditionalOptionsSearchingMethodUsages; }
    public void setPhpShowAdditionalOptionsSearchingMethodUsages(boolean val) { this.phpShowAdditionalOptionsSearchingMethodUsages = val; }

    public boolean isPhpAutoInsertClosingHtmlTagInDoc() { return phpAutoInsertClosingHtmlTagInDoc; }
    public void setPhpAutoInsertClosingHtmlTagInDoc(boolean val) { this.phpAutoInsertClosingHtmlTagInDoc = val; }

    public boolean isPhpAutoInsertArrowOnTypingMinusAfterObject() { return phpAutoInsertArrowOnTypingMinusAfterObject; }
    public void setPhpAutoInsertArrowOnTypingMinusAfterObject(boolean val) { this.phpAutoInsertArrowOnTypingMinusAfterObject = val; }

    public boolean isPhpSmartIndent() { return phpSmartIndent; }
    public void setPhpSmartIndent(boolean val) { this.phpSmartIndent = val; }

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
                autoAddValKeywordToConstructorParams,
                yamlAutoExpandKeySequencesOnPaste,
                xmlHtmlInsertClosingTag, xmlHtmlInsertRequiredAttributes,
                xmlHtmlInsertRequiredSubtags, xmlHtmlStartAttribute,
                xmlHtmlAddQuotesForAttributeValue, xmlHtmlAutoCloseTag,
                xmlHtmlSimultaneousTagEditing, cssSelectWholeCssIdentifiersOnDoubleClick,
                pythonSmartIndentPastedLines, pythonUseParenthesesInsteadOfBackslashes,
                pythonInsertSelfWhenDefiningMethod, pythonInsertTypePlaceholdersInDocCommentStub,
                jsonInsertMissingCommaOnEnter, jsonInsertMissingCommaAfterMatchingBracesQuotes,
                jsonAutoManageCommasPastingFragments, jsonEscapeTextOnPasteInStringLiterals,
                jsonAutoAddQuotesToPropertyNamesOnColon, jsonAutoAddWhitespaceOnColonAfterProperty,
                jsonAutoMoveColonAfterPropertyNameInsideQuotes, jsonAutoMoveCommaAfterValueInsideQuotes,
                rustInsertPairedHashForRawStrings,
                markdownReformatTable, markdownInsertHtmlBreakInsideTableCells,
                markdownUseShiftEnterForNewTableRow, markdownUseTabShiftTabToNavigateCells,
                markdownAdjustIndentationOnType, markdownSmartEnterAndBackspace,
                markdownRenumberListWhenTyping, markdownListNumerating, markdownInsertLinksOnDrop,
                scalaIndentPastedLinesAtCaret, scalaInsertPairQuotesForMultilineString,
                scalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace,
                scalaWrapSingleExpressionBodyWithClosingBraceAfterBrace,
                scalaDeleteClosingBraceAfterDeletingBrace,
                scalaAddBracesAutomaticallyBasedOnIndentation,
                scalaRemoveBracesAutomaticallyBasedOnIndentation,
                sqlInsertStringConcatOnEnter, sqlCloseCodeBlocksOnEnter,
                rubyContinueLineCommentsOnEnter, rubyDeleteEmptyLineCommentsOnEnter,
                rubyStartInterpolationOnTypingHash,
                jsReplaceStringLiteralOnTemplate, jsStartTemplateStringInterpolation,
                jsEscapeTextOnPasteInStringLiterals, jsCloseHtmlSingleTagsInJsx,
                jsConvertHtmlAttributeNamesInJsx, jsEscapeJsDocLeadingAsterisks,
                phpEnableSmartFunctionParametersCompletion, phpSelectVarWithoutDollarOnDoubleClick,
                phpRemovePhpOpenCloseTagsWhilePasting, phpEscapeSymbolsOnPasteInStringLiterals,
                phpReplaceUnnecessaryDoubleQuotesOnPaste, phpAutoInsertPhpTagAfterTyping,
                phpAutoInsertSemicolon, phpShowAdditionalOptionsSearchingMethodUsages,
                phpAutoInsertClosingHtmlTagInDoc, phpAutoInsertArrowOnTypingMinusAfterObject,
                phpSmartIndent
        );
    }
}
