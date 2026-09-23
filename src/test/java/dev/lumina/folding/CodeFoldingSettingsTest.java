package dev.lumina.folding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeFoldingSettingsTest {

    @Test
    void testDefaultValuesMatchingIntelliJ() {
        CodeFoldingSettings settings = CodeFoldingSettings.getInstance();
        assertTrue(settings.isFoldImportsByDefault(), "Imports should be folded by default in IntelliJ");
        assertTrue(settings.isFoldDocCommentsByDefault(), "Doc comments should be folded by default in IntelliJ");
        assertFalse(settings.isFoldMethodBodiesByDefault(), "Method bodies should not be folded by default in IntelliJ");
        assertFalse(settings.isFoldAnnotationsByDefault(), "Annotations should not be folded by default in IntelliJ");
        assertTrue(settings.isShowFoldingArrows(), "Folding arrows should be shown by default in IntelliJ");
    }

    @Test
    void testSettingsMutationAndPersistence() {
        CodeFoldingSettings settings = CodeFoldingSettings.getInstance();
        boolean original = settings.isFoldMethodBodiesByDefault();
        try {
            settings.setFoldMethodBodiesByDefault(!original);
            assertEquals(!original, settings.isFoldMethodBodiesByDefault());
        } finally {
            settings.setFoldMethodBodiesByDefault(original);
        }
    }
}
