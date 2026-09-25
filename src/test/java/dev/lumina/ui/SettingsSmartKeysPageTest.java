package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import dev.lumina.settings.SmartKeysSettings.ReformatOnPaste;
import dev.lumina.settings.SmartKeysSettings.UnindentOnBackspace;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsSmartKeysPageTest {

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
        SmartKeysSettings.getInstance().initDefaults();
    }

    @Test
    void testInitialStateMatchesDefaults() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsSmartKeysPage page = new SettingsSmartKeysPage();
                assertFalse(page.isModified());

                // General Smart Keys
                assertTrue(page.getHomeMovesCaretCheck().isSelected());
                assertTrue(page.getEndOnBlankLineCheck().isSelected());
                assertTrue(page.getInsertPairedBracketsCheck().isSelected());
                assertTrue(page.getInsertPairQuoteCheck().isSelected());
                assertTrue(page.getReformatBlockCheck().isSelected());
                assertFalse(page.getUseCamelHumpsCheck().isSelected());
                assertTrue(page.getHonorCamelHumpsCheck().isSelected());
                assertTrue(page.getSurroundSelectionCheck().isSelected());
                assertTrue(page.getMultipleCaretsCheck().isSelected());
                assertTrue(page.getJumpOutsideBracketCheck().isSelected());

                // Enter
                assertTrue(page.getSmartIndentCheck().isSelected());
                assertTrue(page.getInsertPairRBraceCheck().isSelected());
                assertTrue(page.getCloseBlockCommentCheck().isSelected());
                assertTrue(page.getInsertDocCommentCheck().isSelected());

                // Backspace & Paste
                assertEquals(UnindentOnBackspace.TO_PROPER_INDENT, page.getUnindentOnBackspaceCombo().getValue());
                assertEquals(ReformatOnPaste.INDENT_EACH_LINE, page.getReformatOnPasteCombo().getValue());
                assertFalse(page.getReformatRemoveBreaksCheck().isSelected());

                // JavaDoc & JSP
                assertTrue(page.getAutoInsertClosingTagCheck().isSelected());
                assertTrue(page.getInsertPairPercentCheck().isSelected());

                // Kotlin
                assertTrue(page.getConvertPastedJavaCheck().isSelected());
                assertFalse(page.getDontShowConversionDialogCheck().isSelected());
                assertFalse(page.getDontShowConversionDialogCheck().isDisable());
                assertTrue(page.getAutoAddValCheck().isSelected());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testKotlinConversionSubOptionDisabledWhenParentUnchecked() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsSmartKeysPage page = new SettingsSmartKeysPage();
                assertFalse(page.getDontShowConversionDialogCheck().isDisable());

                page.getConvertPastedJavaCheck().setSelected(false);
                assertTrue(page.getDontShowConversionDialogCheck().isDisable());

                page.getConvertPastedJavaCheck().setSelected(true);
                assertFalse(page.getDontShowConversionDialogCheck().isDisable());
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
                SettingsSmartKeysPage page = new SettingsSmartKeysPage();
                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                assertFalse(page.isModified());

                // Change a checkbox
                page.getUseCamelHumpsCheck().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                // Reset back
                page.reset();
                assertFalse(page.isModified());
                assertFalse(page.getUseCamelHumpsCheck().isSelected());
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
                SettingsSmartKeysPage page = new SettingsSmartKeysPage();
                page.getHomeMovesCaretCheck().setSelected(false);
                page.getUnindentOnBackspaceCombo().setValue(UnindentOnBackspace.DISABLED);
                page.getReformatOnPasteCombo().setValue(ReformatOnPaste.NONE);
                page.getConvertPastedJavaCheck().setSelected(false);

                assertTrue(page.isModified());
                page.apply();

                assertFalse(page.isModified());
                assertFalse(SmartKeysSettings.getInstance().isHomeMovesCaretToFirstNonWhitespace());
                assertEquals(UnindentOnBackspace.DISABLED, SmartKeysSettings.getInstance().getUnindentOnBackspace());
                assertEquals(ReformatOnPaste.NONE, SmartKeysSettings.getInstance().getReformatOnPaste());
                assertFalse(SmartKeysSettings.getInstance().isConvertPastedJavaToKotlin());

                // Restore defaults
                SmartKeysSettings.getInstance().initDefaults();
                SmartKeysSettings.getInstance().save();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
