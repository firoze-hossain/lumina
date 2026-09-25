package dev.lumina.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CodeEditingSettingsTest {

    private CodeEditingSettings settings;

    @BeforeEach
    void setUp() {
        settings = CodeEditingSettings.getInstance();
        settings.initDefaults();
    }

    @Test
    void testDefaultsMatchReferenceScreenshots() {
        // Highlight on Caret Movement
        assertTrue(settings.isMatchedBrace());
        assertFalse(settings.isCurrentScope());
        assertTrue(settings.isUsagesOfElementAtCaret());

        // Quick Documentation
        assertTrue(settings.isShowQuickDocOnHover());

        // Refactorings
        assertEquals(CodeEditingSettings.RefactoringOption.IN_EDITOR, settings.getRefactoringOption());
        assertTrue(settings.isPreselectCurrentSymbolForRename());
        assertTrue(settings.isShowInlineDialogForLocalVariables());

        // Error Highlighting
        assertEquals(2, settings.getErrorStripeMarkMinHeight());
        assertEquals(300, settings.getAutoreparseDelay());
        assertEquals("The problems with the highest priority", settings.getNextErrorActionGoesThrough());
        assertTrue(settings.isSuppressWithSuppressWarnings());

        // Editor Tooltips
        assertEquals(500, settings.getTooltipDelay());
    }

    @Test
    void testIsModified() {
        CodeEditingSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setMatchedBrace(false);
        assertTrue(settings.isModified(copy));
        copy.setMatchedBrace(true);
        assertFalse(settings.isModified(copy));

        copy.setCurrentScope(true);
        assertTrue(settings.isModified(copy));
        copy.setCurrentScope(false);
        assertFalse(settings.isModified(copy));

        copy.setRefactoringOption(CodeEditingSettings.RefactoringOption.IN_MODAL_DIALOGS);
        assertTrue(settings.isModified(copy));
        copy.setRefactoringOption(CodeEditingSettings.RefactoringOption.IN_EDITOR);
        assertFalse(settings.isModified(copy));

        copy.setErrorStripeMarkMinHeight(4);
        assertTrue(settings.isModified(copy));
        copy.setErrorStripeMarkMinHeight(2);
        assertFalse(settings.isModified(copy));
    }

    @Test
    void testSaveAndLoad() {
        settings.setMatchedBrace(false);
        settings.setCurrentScope(true);
        settings.setRefactoringOption(CodeEditingSettings.RefactoringOption.IN_MODAL_DIALOGS);
        settings.setErrorStripeMarkMinHeight(5);
        settings.setNextErrorActionGoesThrough("All problems");
        settings.setTooltipDelay(750);
        settings.save();

        CodeEditingSettings loaded = new CodeEditingSettings();
        loaded.load();

        assertFalse(loaded.isMatchedBrace());
        assertTrue(loaded.isCurrentScope());
        assertEquals(CodeEditingSettings.RefactoringOption.IN_MODAL_DIALOGS, loaded.getRefactoringOption());
        assertEquals(5, loaded.getErrorStripeMarkMinHeight());
        assertEquals("All problems", loaded.getNextErrorActionGoesThrough());
        assertEquals(750, loaded.getTooltipDelay());
    }

    @Test
    void testListeners() {
        AtomicBoolean notified = new AtomicBoolean(false);
        CodeEditingSettings.Listener listener = s -> notified.set(true);
        settings.addListener(listener);

        settings.save();
        assertTrue(notified.get());

        notified.set(false);
        settings.removeListener(listener);
        settings.save();
        assertFalse(notified.get());
    }
}
