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
