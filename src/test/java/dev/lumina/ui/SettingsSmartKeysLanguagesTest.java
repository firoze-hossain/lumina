package dev.lumina.ui;

import dev.lumina.settings.SmartKeysSettings;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SettingsSmartKeysLanguagesTest {

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
    void testYAMLPageDefaultsAndModification() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsSmartKeysYAMLPage page = new SettingsSmartKeysYAMLPage();
                assertFalse(page.isModified());
                assertTrue(page.getAutoExpandCheck().isSelected());

                AtomicBoolean modifiedNotified = new AtomicBoolean(false);
                page.setOnModifiedListener(() -> modifiedNotified.set(true));

                page.getAutoExpandCheck().setSelected(false);
                assertTrue(page.isModified());
                assertTrue(modifiedNotified.get());

                page.apply();
                assertFalse(SmartKeysSettings.getInstance().isYamlAutoExpandKeySequencesOnPaste());
                assertFalse(page.isModified());

                page.getAutoExpandCheck().setSelected(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.getAutoExpandCheck().isSelected());
                assertFalse(page.isModified());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testHTMLCSSPageDefaultsAndModification() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsSmartKeysHTMLCSSPage page = new SettingsSmartKeysHTMLCSSPage();
                assertFalse(page.isModified());

                assertTrue(page.getInsertClosingTagCheck().isSelected());
                assertTrue(page.getInsertRequiredAttributesCheck().isSelected());
                assertTrue(page.getInsertRequiredSubtagsCheck().isSelected());
                assertTrue(page.getStartAttributeCheck().isSelected());
                assertTrue(page.getAddQuotesCheck().isSelected());
                assertTrue(page.getAutoCloseTagCheck().isSelected());
                assertTrue(page.getSimultaneousEditingCheck().isSelected());
                assertTrue(page.getSelectWholeCSSCheck().isSelected());

                page.getInsertClosingTagCheck().setSelected(false);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(SmartKeysSettings.getInstance().isXmlHtmlInsertClosingTag());
                assertFalse(page.isModified());

                page.reset();
                assertFalse(page.getInsertClosingTagCheck().isSelected());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testPythonPageDefaultsAndModification() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsSmartKeysPythonPage page = new SettingsSmartKeysPythonPage();
                assertFalse(page.isModified());

                // Exact defaults matching screenshot 3
                assertFalse(page.getSmartIndentPastedLinesCheck().isSelected());
                assertTrue(page.getUseParenthesesCheck().isSelected());
                assertTrue(page.getInsertSelfCheck().isSelected());
                assertFalse(page.getInsertTypePlaceholdersCheck().isSelected());

                page.getSmartIndentPastedLinesCheck().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertTrue(SmartKeysSettings.getInstance().isPythonSmartIndentPastedLines());
                assertFalse(page.isModified());

                page.reset();
                assertTrue(page.getSmartIndentPastedLinesCheck().isSelected());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testJSONPageDefaultsAndModification() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsSmartKeysJSONPage page = new SettingsSmartKeysJSONPage();
                assertFalse(page.isModified());

                // Exact defaults matching screenshot 4 (top 6 true, bottom 2 false)
                assertTrue(page.getInsertMissingCommaOnEnterCheck().isSelected());
                assertTrue(page.getInsertMissingCommaAfterMatchingBracesQuotesCheck().isSelected());
                assertTrue(page.getAutoManageCommasPastingFragmentsCheck().isSelected());
                assertTrue(page.getEscapeTextOnPasteInStringLiteralsCheck().isSelected());
                assertTrue(page.getAutoAddQuotesToPropertyNamesOnColonCheck().isSelected());
                assertTrue(page.getAutoAddWhitespaceOnColonAfterPropertyCheck().isSelected());
                assertFalse(page.getAutoMoveColonAfterPropertyNameInsideQuotesCheck().isSelected());
                assertFalse(page.getAutoMoveCommaAfterValueInsideQuotesCheck().isSelected());

                page.getAutoMoveColonAfterPropertyNameInsideQuotesCheck().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertTrue(SmartKeysSettings.getInstance().isJsonAutoMoveColonAfterPropertyNameInsideQuotes());
                assertFalse(page.isModified());

                page.reset();
                assertTrue(page.getAutoMoveColonAfterPropertyNameInsideQuotesCheck().isSelected());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testRustPageDefaultsAndModification() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsSmartKeysRustPage page = new SettingsSmartKeysRustPage();
                assertFalse(page.isModified());
                assertTrue(page.getPairedHashCheck().isSelected());

                page.getPairedHashCheck().setSelected(false);
                assertTrue(page.isModified());

                page.apply();
                assertFalse(SmartKeysSettings.getInstance().isRustInsertPairedHashForRawStrings());
                assertFalse(page.isModified());

                page.reset();
                assertFalse(page.getPairedHashCheck().isSelected());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testMarkdownPageDefaultsAndModification() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsSmartKeysMarkdownPage page = new SettingsSmartKeysMarkdownPage();
                assertFalse(page.isModified());

                assertTrue(page.getReformatTableCheck().isSelected());
                assertTrue(page.getInsertLineBreakCheck().isSelected());
                assertTrue(page.getShiftEnterRowCheck().isSelected());
                assertTrue(page.getTabNavigateCheck().isSelected());
                assertTrue(page.getAdjustIndentationCheck().isSelected());
                assertTrue(page.getSmartEnterBackspaceCheck().isSelected());
                assertFalse(page.getRenumberListCheck().isSelected());
                assertEquals("Sequentially", page.getNumeratingCombo().getValue());
                assertTrue(page.getInsertLinksCheck().isSelected());

                page.getRenumberListCheck().setSelected(true);
                assertTrue(page.isModified());

                page.apply();
                assertTrue(SmartKeysSettings.getInstance().isMarkdownRenumberListWhenTyping());
                assertFalse(page.isModified());

                page.reset();
                assertTrue(page.getRenumberListCheck().isSelected());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testRemainingLanguagePagesDefaultsAndModification() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                // Scala (Screenshot 4)
                SettingsSmartKeysScalaPage scalaPage = new SettingsSmartKeysScalaPage();
                assertFalse(scalaPage.isModified());
                assertTrue(scalaPage.getIndentPastedLinesCheck().isSelected());
                assertTrue(scalaPage.getInsertPairQuotesCheck().isSelected());
                assertTrue(scalaPage.getUpgradeSimpleStringCheck().isSelected());
                assertTrue(scalaPage.getWrapSingleExpressionCheck().isSelected());
                assertTrue(scalaPage.getDeleteClosingBraceCheck().isSelected());
                assertTrue(scalaPage.getAddBracesAutomaticallyCheck().isSelected());
                assertFalse(scalaPage.getRemoveBracesAutomaticallyCheck().isSelected());

                scalaPage.getRemoveBracesAutomaticallyCheck().setSelected(true);
                assertTrue(scalaPage.isModified());
                scalaPage.apply();
                assertTrue(SmartKeysSettings.getInstance().isScalaRemoveBracesAutomaticallyBasedOnIndentation());
                assertFalse(scalaPage.isModified());
                scalaPage.reset();
                assertTrue(scalaPage.getRemoveBracesAutomaticallyCheck().isSelected());

                // SQL (Screenshot 5)
                SettingsSmartKeysSQLPage sqlPage = new SettingsSmartKeysSQLPage();
                assertFalse(sqlPage.isModified());
                assertTrue(sqlPage.getInsertStringConcatCheck().isSelected());
                assertTrue(sqlPage.getCloseCodeBlocksCheck().isSelected());

                // Ruby
                SettingsSmartKeysRubyPage rubyPage = new SettingsSmartKeysRubyPage();
                assertFalse(rubyPage.isModified());
                assertTrue(rubyPage.getContinueLineCommentsCheck().isSelected());
                assertTrue(rubyPage.getDeleteEmptyLineCommentsCheck().isSelected());
                assertFalse(rubyPage.getStartInterpolationOnTypingHashCheck().isSelected());

                rubyPage.getStartInterpolationOnTypingHashCheck().setSelected(true);
                assertTrue(rubyPage.isModified());
                rubyPage.apply();
                assertTrue(SmartKeysSettings.getInstance().isRubyStartInterpolationOnTypingHash());
                assertFalse(rubyPage.isModified());
                rubyPage.reset();
                assertTrue(rubyPage.getStartInterpolationOnTypingHashCheck().isSelected());

                // JavaScript
                SettingsSmartKeysJavaScriptPage jsPage = new SettingsSmartKeysJavaScriptPage();
                assertFalse(jsPage.isModified());
                assertTrue(jsPage.getReplaceStringLiteralCheck().isSelected());
                assertFalse(jsPage.getStartTemplateStringCheck().isSelected());

                // PHP
                SettingsSmartKeysPHPPage phpPage = new SettingsSmartKeysPHPPage();
                assertFalse(phpPage.isModified());
                assertFalse(phpPage.getSmartParamsCompletionCheck().isSelected());
                assertFalse(phpPage.getSelectVarWithoutDollarCheck().isSelected());
                assertTrue(phpPage.getRemovePhpTagsOnPasteCheck().isSelected());
                assertFalse(phpPage.getEscapeSymbolsOnPasteCheck().isSelected());
                assertFalse(phpPage.getReplaceQuotesOnPasteCheck().isSelected());
                assertTrue(phpPage.getAutoInsertPhpTagCheck().isSelected());
                assertTrue(phpPage.getAutoInsertSemicolonCheck().isSelected());
                assertTrue(phpPage.getShowAdditionalOptionsMethodUsagesCheck().isSelected());
                assertTrue(phpPage.getAutoInsertClosingTagDocCheck().isSelected());

                phpPage.getSelectVarWithoutDollarCheck().setSelected(true);
                assertTrue(phpPage.isModified());
                phpPage.apply();
                assertTrue(SmartKeysSettings.getInstance().isPhpSelectVarWithoutDollarOnDoubleClick());
                assertFalse(phpPage.isModified());
                phpPage.reset();
                assertTrue(phpPage.getSelectVarWithoutDollarCheck().isSelected());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
