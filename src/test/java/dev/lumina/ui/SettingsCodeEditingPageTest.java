package dev.lumina.ui;

import dev.lumina.settings.CodeEditingSettings;
import dev.lumina.settings.CodeEditingSettings.RefactoringOption;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsCodeEditingPageTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
                return;
            }
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (Throwable ignored) {
            javaFxAvailable = false;
        }
    }

    @BeforeEach
    void setUp() {
        CodeEditingSettings.getInstance().initDefaults();
    }

    @Test
    void testInitialStateMatchesDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeEditingPage page = new SettingsCodeEditingPage();
                assertFalse(page.isModified());

                // Highlight on Caret Movement
                assertTrue(page.getMatchedBraceCheck().isSelected());
                assertFalse(page.getCurrentScopeCheck().isSelected());
                assertTrue(page.getUsagesAtCaretCheck().isSelected());

                // Quick Documentation
                assertTrue(page.getShowQuickDocHoverCheck().isSelected());

                // Refactorings
                assertTrue(page.getInEditorRadio().isSelected());
                assertFalse(page.getInModalRadio().isSelected());
                assertTrue(page.getPreselectRenameCheck().isSelected());
                assertTrue(page.getShowInlineDialogCheck().isSelected());

                // Error Highlighting
                assertEquals("2", page.getErrorStripeMinHeightField().getText());
                assertEquals("300", page.getAutoreparseDelayField().getText());
                assertEquals("The problems with the highest priority", page.getNextErrorCombo().getValue());
                assertTrue(page.getSuppressWarningsCheck().isSelected());

                // Editor Tooltips
                assertEquals("500", page.getTooltipDelayField().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testDirtyTrackingAndReset() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeEditingPage page = new SettingsCodeEditingPage();
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                assertFalse(page.isModified());

                // Modify a checkbox
                page.getMatchedBraceCheck().setSelected(false);
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                // Modify radio button
                page.getInModalRadio().setSelected(true);
                assertTrue(page.isModified());

                // Modify text field
                page.getTooltipDelayField().setText("800");
                assertTrue(page.isModified());

                // Reset back to defaults
                page.reset();
                assertFalse(page.isModified());
                assertTrue(page.getMatchedBraceCheck().isSelected());
                assertTrue(page.getInEditorRadio().isSelected());
                assertEquals("500", page.getTooltipDelayField().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testApplySavesToSettings() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeEditingPage page = new SettingsCodeEditingPage();
                page.getMatchedBraceCheck().setSelected(false);
                page.getCurrentScopeCheck().setSelected(true);
                page.getShowQuickDocHoverCheck().setSelected(false);
                page.getInModalRadio().setSelected(true);
                page.getPreselectRenameCheck().setSelected(false);
                page.getShowInlineDialogCheck().setSelected(false);
                page.getErrorStripeMinHeightField().setText("5");
                page.getAutoreparseDelayField().setText("600");
                page.getNextErrorCombo().setValue("All problems");
                page.getSuppressWarningsCheck().setSelected(false);
                page.getTooltipDelayField().setText("1000");

                assertTrue(page.isModified());
                page.apply();

                assertFalse(page.isModified());
                CodeEditingSettings s = CodeEditingSettings.getInstance();
                assertFalse(s.isMatchedBrace());
                assertTrue(s.isCurrentScope());
                assertFalse(s.isShowQuickDocOnHover());
                assertEquals(RefactoringOption.IN_MODAL_DIALOGS, s.getRefactoringOption());
                assertFalse(s.isPreselectCurrentSymbolForRename());
                assertFalse(s.isShowInlineDialogForLocalVariables());
                assertEquals(5, s.getErrorStripeMarkMinHeight());
                assertEquals(600, s.getAutoreparseDelay());
                assertEquals("All problems", s.getNextErrorActionGoesThrough());
                assertFalse(s.isSuppressWithSuppressWarnings());
                assertEquals(1000, s.getTooltipDelay());

                // Restore defaults
                s.initDefaults();
                s.save();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
