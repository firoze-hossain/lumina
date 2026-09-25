package dev.lumina.settings;

import dev.lumina.settings.SmartKeysSettings.ReformatOnPaste;
import dev.lumina.settings.SmartKeysSettings.UnindentOnBackspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SmartKeysSettingsTest {

    private SmartKeysSettings settings;

    @BeforeEach
    void setUp() {
        settings = new SmartKeysSettings();
        settings.initDefaults();
    }

    @Test
    void testDefaultsMatchScreenshots() {
        // General Smart Keys
        assertTrue(settings.isHomeMovesCaretToFirstNonWhitespace());
        assertTrue(settings.isEndOnBlankLineMovesCaretToIndent());
        assertTrue(settings.isInsertPairedBrackets());
        assertTrue(settings.isInsertPairQuote());
        assertTrue(settings.isReformatBlockOnTypingRBrace());
        assertFalse(settings.isUseCamelHumpsWords());
        assertTrue(settings.isHonorCamelHumpsOnDoubleClick());
        assertTrue(settings.isSurroundSelectionOnQuoteOrBrace());
        assertTrue(settings.isAddMultipleCaretsOnDoubleCtrlArrow());
        assertTrue(settings.isJumpOutsideClosingBracketOrQuoteWithTab());

        // Enter
        assertTrue(settings.isSmartIndent());
        assertTrue(settings.isInsertPairRBrace());
        assertTrue(settings.isCloseBlockComment());
        assertTrue(settings.isInsertDocCommentStub());

        // Backspace & Paste
        assertEquals(UnindentOnBackspace.TO_PROPER_INDENT, settings.getUnindentOnBackspace());
        assertEquals(ReformatOnPaste.INDENT_EACH_LINE, settings.getReformatOnPaste());
        assertFalse(settings.isReformatAgainToRemoveCustomLineBreaks());

        // JavaDoc
        assertTrue(settings.isAutoInsertClosingTagInJavaDoc());

        // JSP
        assertTrue(settings.isInsertPairPercentOnEnterInJsp());

        // Kotlin
        assertTrue(settings.isConvertPastedJavaToKotlin());
        assertFalse(settings.isDontShowJavaToKotlinDialogOnPaste());
        assertTrue(settings.isAutoAddValKeywordToConstructorParams());

        // YAML (Screenshot 1)
        assertTrue(settings.isYamlAutoExpandKeySequencesOnPaste());

        // HTML/CSS (Screenshot 2)
        assertTrue(settings.isXmlHtmlInsertClosingTag());
        assertTrue(settings.isXmlHtmlInsertRequiredAttributes());
        assertTrue(settings.isXmlHtmlInsertRequiredSubtags());
        assertTrue(settings.isXmlHtmlStartAttribute());
        assertTrue(settings.isXmlHtmlAddQuotesForAttributeValue());
        assertTrue(settings.isXmlHtmlAutoCloseTag());
        assertTrue(settings.isXmlHtmlSimultaneousTagEditing());
        assertTrue(settings.isCssSelectWholeCssIdentifiersOnDoubleClick());

        // Python (Screenshot 3)
        assertFalse(settings.isPythonSmartIndentPastedLines());
        assertTrue(settings.isPythonUseParenthesesInsteadOfBackslashes());
        assertTrue(settings.isPythonInsertSelfWhenDefiningMethod());
        assertFalse(settings.isPythonInsertTypePlaceholdersInDocCommentStub());

        // JSON (Screenshot 4)
        assertTrue(settings.isJsonInsertMissingCommaOnEnter());
        assertTrue(settings.isJsonInsertMissingCommaAfterMatchingBracesQuotes());
        assertTrue(settings.isJsonAutoManageCommasPastingFragments());
        assertTrue(settings.isJsonEscapeTextOnPasteInStringLiterals());
        assertTrue(settings.isJsonAutoAddQuotesToPropertyNamesOnColon());
        assertTrue(settings.isJsonAutoAddWhitespaceOnColonAfterProperty());
        assertFalse(settings.isJsonAutoMoveColonAfterPropertyNameInsideQuotes());
        assertFalse(settings.isJsonAutoMoveCommaAfterValueInsideQuotes());

        // Rust (Screenshot 5)
        assertTrue(settings.isRustInsertPairedHashForRawStrings());

        // Markdown
        assertTrue(settings.isMarkdownReformatTable());
        assertTrue(settings.isMarkdownInsertHtmlBreakInsideTableCells());
        assertTrue(settings.isMarkdownUseShiftEnterForNewTableRow());
        assertTrue(settings.isMarkdownUseTabShiftTabToNavigateCells());
        assertTrue(settings.isMarkdownAdjustIndentationOnType());
        assertTrue(settings.isMarkdownSmartEnterAndBackspace());
        assertFalse(settings.isMarkdownRenumberListWhenTyping());
        assertEquals("Sequentially", settings.getMarkdownListNumerating());
        assertTrue(settings.isMarkdownInsertLinksOnDrop());

        // Scala (Screenshot 4)
        assertTrue(settings.isScalaIndentPastedLinesAtCaret());
        assertTrue(settings.isScalaInsertPairQuotesForMultilineString());
        assertTrue(settings.isScalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace());
        assertTrue(settings.isScalaWrapSingleExpressionBodyWithClosingBraceAfterBrace());
        assertTrue(settings.isScalaDeleteClosingBraceAfterDeletingBrace());
        assertTrue(settings.isScalaAddBracesAutomaticallyBasedOnIndentation());
        assertFalse(settings.isScalaRemoveBracesAutomaticallyBasedOnIndentation());

        // SQL (Screenshot 5)
        assertTrue(settings.isSqlInsertStringConcatOnEnter());
        assertTrue(settings.isSqlCloseCodeBlocksOnEnter());

        // Ruby (prior screenshot)
        assertTrue(settings.isRubyContinueLineCommentsOnEnter());
        assertTrue(settings.isRubyDeleteEmptyLineCommentsOnEnter());
        assertFalse(settings.isRubyStartInterpolationOnTypingHash());

        // JavaScript
        assertTrue(settings.isJsReplaceStringLiteralOnTemplate());
        assertTrue(settings.isJsStartTemplateStringInterpolation());
        assertTrue(settings.isJsEscapeTextOnPasteInStringLiterals());
        assertTrue(settings.isJsCloseHtmlSingleTagsInJsx());
        assertTrue(settings.isJsConvertHtmlAttributeNamesInJsx());
        assertTrue(settings.isJsEscapeJsDocLeadingAsterisks());

        // PHP
        assertTrue(settings.isPhpSelectVarWithoutDollarOnDoubleClick());
        assertTrue(settings.isPhpEscapeTextOnPasteInStringLiterals());
        assertTrue(settings.isPhpReplaceUnnecessaryDoubleQuotesOnPaste());
        assertTrue(settings.isPhpAutoInsertPhpTagAfterTyping());
        assertTrue(settings.isPhpAutoInsertArrowOnTypingMinusAfterObject());
        assertTrue(settings.isPhpAutoInsertSemicolon());
        assertTrue(settings.isPhpAutoInsertClosingHtmlTagInDoc());
        assertTrue(settings.isPhpEnableSmartFunctionParametersCompletion());
        assertTrue(settings.isPhpSmartIndent());
    }

    @Test
    void testIsModified() {
        SmartKeysSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setHomeMovesCaretToFirstNonWhitespace(false);
        assertTrue(settings.isModified(copy));
        copy.setHomeMovesCaretToFirstNonWhitespace(true);
        assertFalse(settings.isModified(copy));

        copy.setUseCamelHumpsWords(true);
        assertTrue(settings.isModified(copy));
        copy.setUseCamelHumpsWords(false);
        assertFalse(settings.isModified(copy));

        copy.setUnindentOnBackspace(UnindentOnBackspace.DISABLED);
        assertTrue(settings.isModified(copy));
        copy.setUnindentOnBackspace(UnindentOnBackspace.TO_PROPER_INDENT);
        assertFalse(settings.isModified(copy));

        copy.setReformatOnPaste(ReformatOnPaste.REFORMAT_BLOCK);
        assertTrue(settings.isModified(copy));
        copy.setReformatOnPaste(ReformatOnPaste.INDENT_EACH_LINE);
        assertFalse(settings.isModified(copy));

        copy.setConvertPastedJavaToKotlin(false);
        assertTrue(settings.isModified(copy));
        copy.setConvertPastedJavaToKotlin(true);
        assertFalse(settings.isModified(copy));

        copy.setDontShowJavaToKotlinDialogOnPaste(true);
        assertTrue(settings.isModified(copy));
        copy.setDontShowJavaToKotlinDialogOnPaste(false);
        assertFalse(settings.isModified(copy));

        // Language isModified checks
        copy.setYamlAutoExpandKeySequencesOnPaste(false);
        assertTrue(settings.isModified(copy));
        copy.setYamlAutoExpandKeySequencesOnPaste(true);
        assertFalse(settings.isModified(copy));

        copy.setPythonSmartIndentPastedLines(true);
        assertTrue(settings.isModified(copy));
        copy.setPythonSmartIndentPastedLines(false);
        assertFalse(settings.isModified(copy));

        copy.setJsonAutoMoveColonAfterPropertyNameInsideQuotes(true);
        assertTrue(settings.isModified(copy));
        copy.setJsonAutoMoveColonAfterPropertyNameInsideQuotes(false);
        assertFalse(settings.isModified(copy));

        copy.setRustInsertPairedHashForRawStrings(false);
        assertTrue(settings.isModified(copy));
        copy.setRustInsertPairedHashForRawStrings(true);
        assertFalse(settings.isModified(copy));

        copy.setScalaRemoveBracesAutomaticallyBasedOnIndentation(true);
        assertTrue(settings.isModified(copy));
        copy.setScalaRemoveBracesAutomaticallyBasedOnIndentation(false);
        assertFalse(settings.isModified(copy));

        copy.setRubyStartInterpolationOnTypingHash(true);
        assertTrue(settings.isModified(copy));
        copy.setRubyStartInterpolationOnTypingHash(false);
        assertFalse(settings.isModified(copy));

        copy.setPhpSelectVarWithoutDollarOnDoubleClick(false);
        assertTrue(settings.isModified(copy));
        copy.setPhpSelectVarWithoutDollarOnDoubleClick(true);
        assertFalse(settings.isModified(copy));
    }

    @Test
    void testSaveAndLoad() {
        settings.setHomeMovesCaretToFirstNonWhitespace(false);
        settings.setUseCamelHumpsWords(true);
        settings.setUnindentOnBackspace(UnindentOnBackspace.TO_NEAREST_INDENT);
        settings.setReformatOnPaste(ReformatOnPaste.NONE);
        settings.setReformatAgainToRemoveCustomLineBreaks(true);
        settings.setConvertPastedJavaToKotlin(false);
        settings.setDontShowJavaToKotlinDialogOnPaste(true);
        settings.save();

        SmartKeysSettings loaded = new SmartKeysSettings();
        loaded.load();

        assertFalse(loaded.isHomeMovesCaretToFirstNonWhitespace());
        assertTrue(loaded.isUseCamelHumpsWords());
        assertEquals(UnindentOnBackspace.TO_NEAREST_INDENT, loaded.getUnindentOnBackspace());
        assertEquals(ReformatOnPaste.NONE, loaded.getReformatOnPaste());
        assertTrue(loaded.isReformatAgainToRemoveCustomLineBreaks());
        assertFalse(loaded.isConvertPastedJavaToKotlin());
        assertTrue(loaded.isDontShowJavaToKotlinDialogOnPaste());

        // Restore defaults
        settings.initDefaults();
        settings.save();
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        SmartKeysSettings.Listener l = s -> notified.set(true);

        settings.addListener(l);
        settings.save();
        assertTrue(notified.get());

        notified.set(false);
        settings.removeListener(l);
        settings.save();
        assertFalse(notified.get());
    }

    @Test
    void testEnumsRoundTrip() {
        assertEquals(UnindentOnBackspace.DISABLED, UnindentOnBackspace.fromLabel("Disabled"));
        assertEquals(UnindentOnBackspace.TO_PROPER_INDENT, UnindentOnBackspace.fromLabel("To proper indent position"));
        assertEquals(UnindentOnBackspace.TO_NEAREST_INDENT, UnindentOnBackspace.fromLabel("To nearest indent position"));
        assertEquals(UnindentOnBackspace.TO_PROPER_INDENT, UnindentOnBackspace.fromLabel("unknown"));

        assertEquals(ReformatOnPaste.NONE, ReformatOnPaste.fromLabel("None"));
        assertEquals(ReformatOnPaste.INDENT_BLOCK, ReformatOnPaste.fromLabel("Indent block"));
        assertEquals(ReformatOnPaste.INDENT_EACH_LINE, ReformatOnPaste.fromLabel("Indent each line"));
        assertEquals(ReformatOnPaste.REFORMAT_BLOCK, ReformatOnPaste.fromLabel("Reformat block"));
        assertEquals(ReformatOnPaste.INDENT_EACH_LINE, ReformatOnPaste.fromLabel("unknown"));
    }
}
