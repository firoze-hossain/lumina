package dev.lumina.ui;

import dev.lumina.copyright.CopyrightFormattingOptions;
import dev.lumina.copyright.CopyrightManager;
import dev.lumina.copyright.LanguageFormattingOverride;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsCopyrightFormattingPageTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (IllegalStateException e) {
            javaFxAvailable = true;
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    @BeforeEach
    void setUp() {
        CopyrightManager.getInstance().resetToDefaults();
    }

    @Test
    void testRootFormattingPageLifecycle() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCopyrightFormattingPage page = new SettingsCopyrightFormattingPage();
                assertNotNull(page);
                assertFalse(page.isModified());

                boolean[] modifiedFired = {false};
                page.setOnModifiedListener(() -> modifiedFired[0] = true);

                // Modify comment type to LINE
                page.getLineCommentRadio().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired[0]);

                // Apply changes
                page.apply();
                assertFalse(page.isModified());
                assertEquals(CopyrightFormattingOptions.CommentType.LINE,
                        CopyrightManager.getInstance().getDefaultFormatting().getCommentType());

                // Modify separator char
                page.getSeparatorCharField().setText("#");
                assertTrue(page.isModified());

                // Reset changes
                page.reset();
                assertFalse(page.isModified());
                assertEquals("", page.getSeparatorCharField().getText());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testLanguageFormattingPageModeSwitching() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCopyrightFormattingLanguagePage page = new SettingsCopyrightFormattingLanguagePage("CSS");
                assertNotNull(page);
                assertEquals("CSS", page.getLanguage());
                assertFalse(page.isModified());

                // By default USE_DEFAULT is selected
                assertTrue(page.getUseDefaultRadio().isSelected());
                assertTrue(page.getOptionsSection().isDisabled());

                boolean[] modifiedFired = {false};
                page.setOnModifiedListener(() -> modifiedFired[0] = true);

                // Switch to USE_CUSTOM
                page.getUseCustomRadio().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(modifiedFired[0]);
                assertFalse(page.getOptionsSection().isDisabled());

                // Customize separator before
                page.getSeparatorBeforeCheck().setSelected(true);
                page.apply();
                assertFalse(page.isModified());

                LanguageFormattingOverride override = CopyrightManager.getInstance().getLanguageOverride("CSS");
                assertEquals(LanguageFormattingOverride.Mode.USE_CUSTOM, override.getMode());
                assertTrue(override.getCustomOptions().isSeparatorBefore());

                // Switch to NO_COPYRIGHT
                page.getNoCopyrightRadio().setSelected(true);
                assertTrue(page.isModified());
                assertTrue(page.getOptionsSection().isDisabled());

                page.apply();
                assertFalse(page.isModified());
                assertEquals(LanguageFormattingOverride.Mode.NO_COPYRIGHT,
                        CopyrightManager.getInstance().getLanguageOverride("CSS").getMode());
                assertNull(CopyrightManager.getInstance().getEffectiveFormatting("CSS"));
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testLanguageSubclassesAndHtmlLocation() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCopyrightFormattingCSSPage cssPage = new SettingsCopyrightFormattingCSSPage();
                assertEquals("CSS", cssPage.getLanguage());
                assertNull(cssPage.getLocationInFileGroup());

                SettingsCopyrightFormattingHTMLPage htmlPage = new SettingsCopyrightFormattingHTMLPage();
                assertEquals("HTML", htmlPage.getLanguage());
                assertNotNull(htmlPage.getLocationInFileGroup());

                SettingsCopyrightFormattingDTDPage dtdPage = new SettingsCopyrightFormattingDTDPage();
                assertEquals("DTD", dtdPage.getLanguage());
                assertNotNull(dtdPage.getLocationInFileGroup());

                // HTML preview contains HTML comment tags
                String htmlPreview = htmlPage.getPreviewCodeArea().getText();
                assertTrue(htmlPreview.contains("<!--"));
                assertTrue(htmlPreview.contains("-->"));

                // CSS preview contains CSS comment tags
                String cssPreview = cssPage.getPreviewCodeArea().getText();
                assertTrue(cssPreview.contains("/*"));
                assertTrue(cssPreview.contains("*/"));
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testJavaLanguageFormattingPageLocationInFile() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCopyrightFormattingJavaPage javaPage = new SettingsCopyrightFormattingJavaPage();
                assertEquals("Java", javaPage.getLanguage());
                assertNotNull(javaPage.getLocationInFileBox());

                // Default location is BEFORE_PACKAGE
                assertTrue(javaPage.getBeforePackageRadio().isSelected());
                assertFalse(javaPage.getBeforeImportsRadio().isSelected());
                assertFalse(javaPage.getBeforeClassRadio().isSelected());

                // Location in File is enabled even on "Use default settings" (matching Image 1)
                assertTrue(javaPage.getUseDefaultRadio().isSelected());
                assertFalse(javaPage.getLocationInFileBox().isDisabled());

                // Select Before imports
                javaPage.getBeforeImportsRadio().setSelected(true);
                assertTrue(javaPage.isModified());

                javaPage.apply();
                assertFalse(javaPage.isModified());
                LanguageFormattingOverride override = CopyrightManager.getInstance().getLanguageOverride("Java");
                assertEquals(CopyrightFormattingOptions.LocationInFile.BEFORE_IMPORTS,
                        override.getCustomOptions().getLocationInFile());

                // Switching to No copyright disables Location in File
                javaPage.getNoCopyrightRadio().setSelected(true);
                assertTrue(javaPage.getLocationInFileBox().isDisabled());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
