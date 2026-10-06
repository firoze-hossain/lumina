package dev.lumina.copyright;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CopyrightFormattingTest {

    private CopyrightManager manager;

    @BeforeEach
    void setUp() {
        manager = CopyrightManager.getInstance();
        manager.resetToDefaults();
    }

    @Test
    void testDefaultFormattingOptions() {
        CopyrightFormattingOptions opts = new CopyrightFormattingOptions();
        assertEquals(CopyrightFormattingOptions.CommentType.BLOCK, opts.getCommentType());
        assertTrue(opts.isPrefixEachLine());
        assertEquals(CopyrightFormattingOptions.RelativeLocation.BEFORE_OTHER_COMMENTS, opts.getRelativeLocation());
        assertFalse(opts.isSeparatorBefore());
        assertEquals(80, opts.getSeparatorBeforeLength());
        assertFalse(opts.isSeparatorAfter());
        assertEquals(80, opts.getSeparatorAfterLength());
        assertEquals("", opts.getSeparatorChar());
        assertFalse(opts.isBox());
        assertFalse(opts.isBlankLineBefore());
        assertTrue(opts.isBlankLineAfter());
        assertEquals(CopyrightFormattingOptions.LocationInFile.BEFORE_DOCTYPE, opts.getLocationInFile());
    }

    @Test
    void testCopyAndEquivalence() {
        CopyrightFormattingOptions opts = new CopyrightFormattingOptions();
        opts.setCommentType(CopyrightFormattingOptions.CommentType.LINE);
        opts.setSeparatorBefore(true);
        opts.setSeparatorBeforeLength(50);
        opts.setSeparatorChar("-");
        opts.setBox(true);
        opts.setBlankLineBefore(true);
        opts.setBlankLineAfter(false);
        opts.setLocationInFile(CopyrightFormattingOptions.LocationInFile.BEFORE_ROOT_TAG);

        CopyrightFormattingOptions copy = opts.copy();
        assertTrue(opts.isEquivalentTo(copy));

        copy.setSeparatorBeforeLength(60);
        assertFalse(opts.isEquivalentTo(copy));
    }

    @Test
    void testFormatPreviewBlockComment() {
        CopyrightFormattingOptions opts = new CopyrightFormattingOptions();
        String[] lines = new String[]{"Copyright (c) 2026 Lumina", "All rights reserved."};

        String formatted = opts.formatPreview(lines, "/*", " */", " * ", "// ");
        assertTrue(formatted.startsWith("/*\n"));
        assertTrue(formatted.contains(" * Copyright (c) 2026 Lumina\n"));
        assertTrue(formatted.contains(" * All rights reserved.\n"));
        assertTrue(formatted.contains(" */\n"));

        // Without prefix
        opts.setPrefixEachLine(false);
        String formattedNoPrefix = opts.formatPreview(lines, "/*", " */", " * ", "// ");
        assertTrue(formattedNoPrefix.contains("Copyright (c) 2026 Lumina\n"));
        assertFalse(formattedNoPrefix.contains(" * Copyright (c) 2026 Lumina\n"));
    }

    @Test
    void testFormatPreviewLineComment() {
        CopyrightFormattingOptions opts = new CopyrightFormattingOptions();
        opts.setCommentType(CopyrightFormattingOptions.CommentType.LINE);
        String[] lines = new String[]{"Copyright (c) 2026 Lumina", "Line 2"};

        String formatted = opts.formatPreview(lines, "/*", " */", " * ", "// ");
        assertTrue(formatted.contains("// Copyright (c) 2026 Lumina\n"));
        assertTrue(formatted.contains("// Line 2\n"));
        assertFalse(formatted.contains("/*"));
    }

    @Test
    void testFormatPreviewSeparatorsAndBox() {
        CopyrightFormattingOptions opts = new CopyrightFormattingOptions();
        opts.setSeparatorBefore(true);
        opts.setSeparatorBeforeLength(10);
        opts.setSeparatorAfter(true);
        opts.setSeparatorAfterLength(10);
        opts.setSeparatorChar("=");
        opts.setBox(true);
        opts.setBlankLineBefore(true);

        String[] lines = new String[]{"Test notice"};
        String formatted = opts.formatPreview(lines, "/*", " */", " * ", "// ");

        assertTrue(formatted.startsWith("\n/*\n"));
        assertTrue(formatted.contains(" * ==========\n"));
        assertTrue(formatted.contains(" * Test notice"));
        assertTrue(formatted.contains("*"));
    }

    @Test
    void testLanguageFormattingOverrideModel() {
        LanguageFormattingOverride override = new LanguageFormattingOverride("CSS", LanguageFormattingOverride.Mode.USE_DEFAULT);
        assertEquals("CSS", override.getLanguage());
        assertEquals(LanguageFormattingOverride.Mode.USE_DEFAULT, override.getMode());

        LanguageFormattingOverride copy = override.copy();
        assertTrue(override.isEquivalentTo(copy));

        override.setMode(LanguageFormattingOverride.Mode.USE_CUSTOM);
        assertFalse(override.isEquivalentTo(copy));

        override.getCustomOptions().setSeparatorChar("#");
        assertNotEquals(copy.getCustomOptions().getSeparatorChar(), override.getCustomOptions().getSeparatorChar());
    }

    @Test
    void testEffectiveFormattingResolution() {
        CopyrightFormattingOptions defaultOpts = new CopyrightFormattingOptions();
        defaultOpts.setSeparatorChar("=");
        manager.setDefaultFormatting(defaultOpts);

        // Language with default mode
        CopyrightFormattingOptions effectiveJava = manager.getEffectiveFormatting("Java");
        assertNotNull(effectiveJava);
        assertEquals("=", effectiveJava.getSeparatorChar());

        // Language with No Copyright mode
        LanguageFormattingOverride noCopyCss = new LanguageFormattingOverride("CSS", LanguageFormattingOverride.Mode.NO_COPYRIGHT);
        manager.setLanguageOverride("CSS", noCopyCss);
        assertNull(manager.getEffectiveFormatting("CSS"));

        // Language with Custom mode
        CopyrightFormattingOptions customHtml = new CopyrightFormattingOptions();
        customHtml.setSeparatorChar("~");
        LanguageFormattingOverride customOverride = new LanguageFormattingOverride("HTML", LanguageFormattingOverride.Mode.USE_CUSTOM, customHtml);
        manager.setLanguageOverride("HTML", customOverride);

        CopyrightFormattingOptions effectiveHtml = manager.getEffectiveFormatting("HTML");
        assertNotNull(effectiveHtml);
        assertEquals("~", effectiveHtml.getSeparatorChar());
    }

    @Test
    void testManagerFormattingPersistence() {
        CopyrightFormattingOptions defaultOpts = new CopyrightFormattingOptions();
        defaultOpts.setSeparatorBefore(true);
        defaultOpts.setSeparatorChar("*");
        manager.setDefaultFormatting(defaultOpts);

        LanguageFormattingOverride override = new LanguageFormattingOverride("Rust", LanguageFormattingOverride.Mode.USE_CUSTOM);
        override.getCustomOptions().setCommentType(CopyrightFormattingOptions.CommentType.LINE);
        manager.setLanguageOverride("Rust", override);

        manager.save();

        manager.resetToDefaults();
        assertEquals("", manager.getDefaultFormatting().getSeparatorChar());
        assertTrue(manager.getLanguageOverrides().isEmpty());

        manager.load();
        assertEquals("*", manager.getDefaultFormatting().getSeparatorChar());
        assertTrue(manager.getDefaultFormatting().isSeparatorBefore());

        LanguageFormattingOverride loadedRust = manager.getLanguageOverride("Rust");
        assertNotNull(loadedRust);
        assertEquals(LanguageFormattingOverride.Mode.USE_CUSTOM, loadedRust.getMode());
        assertEquals(CopyrightFormattingOptions.CommentType.LINE, loadedRust.getCustomOptions().getCommentType());
    }

    @Test
    void testJavaLocationInFileEnum() {
        CopyrightFormattingOptions opts = new CopyrightFormattingOptions();
        opts.setLocationInFile(CopyrightFormattingOptions.LocationInFile.BEFORE_PACKAGE);
        assertEquals(CopyrightFormattingOptions.LocationInFile.BEFORE_PACKAGE, opts.getLocationInFile());

        opts.setLocationInFile(CopyrightFormattingOptions.LocationInFile.BEFORE_IMPORTS);
        assertEquals(CopyrightFormattingOptions.LocationInFile.BEFORE_IMPORTS, opts.getLocationInFile());

        opts.setLocationInFile(CopyrightFormattingOptions.LocationInFile.BEFORE_CLASS);
        assertEquals(CopyrightFormattingOptions.LocationInFile.BEFORE_CLASS, opts.getLocationInFile());

        assertEquals(CopyrightFormattingOptions.LocationInFile.BEFORE_PACKAGE,
                CopyrightFormattingOptions.getDefaultLocationForLanguage("Java"));
        assertEquals(CopyrightFormattingOptions.LocationInFile.BEFORE_ROOT_TAG,
                CopyrightFormattingOptions.getDefaultLocationForLanguage("XML"));
        assertEquals(CopyrightFormattingOptions.LocationInFile.BEFORE_DOCTYPE,
                CopyrightFormattingOptions.getDefaultLocationForLanguage("HTML"));
    }
}
