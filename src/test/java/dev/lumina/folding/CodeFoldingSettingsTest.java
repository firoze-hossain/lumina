package dev.lumina.folding;

import dev.lumina.folding.CodeFoldingSettings.FoldingArrowsMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeFoldingSettingsTest {

    private CodeFoldingSettings settings;

    @BeforeEach
    void setUp() {
        settings = new CodeFoldingSettings();
        settings.initDefaults();
    }

    @Test
    void testDefaultValuesMatchingIntelliJScreenshots() {
        // Top
        assertTrue(settings.isShowFoldingArrows());
        assertEquals(FoldingArrowsMode.ON_MOUSE_HOVER, settings.getCodeFoldingArrowsMode());
        assertFalse(settings.isShowBottomArrows());

        // General
        assertTrue(settings.isFoldFileHeader());
        assertTrue(settings.isFoldImports());
        assertFalse(settings.isFoldDocComments());
        assertFalse(settings.isFoldMethodBodies());
        assertFalse(settings.isFoldCustomRegions());

        // JPA QL
        assertTrue(settings.isFoldJpaQueries());

        // JSON
        assertTrue(settings.isJsonShowKeyCount());
        assertFalse(settings.isJsonShowFirstKey());

        // Java
        assertTrue(settings.isJavaOneLineMethods());
        assertFalse(settings.isJavaSimplePropertyAccessors());
        assertFalse(settings.isJavaInnerClasses());
        assertFalse(settings.isJavaAnonymousClasses());
        assertFalse(settings.isJavaAnnotations());
        assertTrue(settings.isJavaClosures());
        assertTrue(settings.isJavaGenericParams());
        assertFalse(settings.isJavaReplaceVar());
        assertTrue(settings.isJavaI18nStrings());
        assertTrue(settings.isJavaSuppressWarnings());
        assertFalse(settings.isJavaEndOfLineComments());
        assertFalse(settings.isJavaMultilineComments());

        // JavaScript
        assertFalse(settings.isJsOneLineFunctions());
        assertFalse(settings.isJsObjectLiterals());
        assertFalse(settings.isJsArrayLiterals());
        assertFalse(settings.isJsXmlLiterals());

        // Kubernetes
        assertTrue(settings.isK8sHelmValueReferences());
        assertTrue(settings.isK8sEnvVarYaml());
        assertTrue(settings.isK8sExecActionYaml());

        // Markdown
        assertTrue(settings.isMarkdownCollapseFrontMatter());
        assertTrue(settings.isMarkdownCollapseLinks());
        assertFalse(settings.isMarkdownCollapseTables());
        assertFalse(settings.isMarkdownCollapseCodeFences());
        assertTrue(settings.isMarkdownCollapseToc());

        // PHP
        assertFalse(settings.isPhpClassBody());
        assertTrue(settings.isPhpImports());
        assertFalse(settings.isPhpMethodBody());
        assertFalse(settings.isPhpFunctionBody());
        assertFalse(settings.isPhpTags());
        assertFalse(settings.isPhpHeredoc());
        assertFalse(settings.isPhpAttribute());
        assertFalse(settings.isPhpAttributeList());

        // Python
        assertFalse(settings.isPythonLongStringLiterals());
        assertFalse(settings.isPythonLongCollectionLiterals());
        assertFalse(settings.isPythonSequentialComments());
        assertFalse(settings.isPythonTypeAnnotations());

        // Ruby i18n & Rust & SQL
        assertTrue(settings.isRubyI18nStrings());
        assertTrue(settings.isRustOneLineMethods());
        assertFalse(settings.isSqlUnderscoresInNumericLiterals());

        // Scala
        assertFalse(settings.isScalaBlockComments());
        assertFalse(settings.isScalaMethodCallBodies());
        assertFalse(settings.isScalaTemplateDefinitionBodies());
        assertFalse(settings.isScalaDefinitionBodies());
        assertFalse(settings.isScalaTypeLambdas());
        assertFalse(settings.isScalaPackages());
        assertFalse(settings.isScalaMultiLineStrings());
        assertFalse(settings.isScalaCustomRegions());
        assertFalse(settings.isScalaMultiLineBlocks());
        assertFalse(settings.isScalaShowOutlineForMultiLineBlocks());

        // XML & YAML
        assertFalse(settings.isXmlTags());
        assertTrue(settings.isXmlHtmlStyleAttribute());
        assertTrue(settings.isXmlEntities());
        assertTrue(settings.isXmlDataUris());
        assertTrue(settings.isYamlLimitFoldedKeysAndValues());
        assertEquals(20, settings.getYamlLimitCharacters());

        // Go
        assertTrue(settings.isGoOneLineIfErrorHandling());
        assertTrue(settings.isGoOneLineFunctionsSingleReturn());
        assertTrue(settings.isGoOneLineCaseClauses());
        assertTrue(settings.isGoEmptyFunctions());
        assertTrue(settings.isGoEmptyStructOrInterface());
        assertFalse(settings.isGoFormattedStrings());
    }

    @Test
    void testIsModified() {
        CodeFoldingSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setShowBottomArrows(true);
        assertTrue(settings.isModified(copy));

        copy.setShowBottomArrows(false);
        assertFalse(settings.isModified(copy));

        copy.setCodeFoldingArrowsMode(FoldingArrowsMode.ALWAYS);
        assertTrue(settings.isModified(copy));

        copy.setCodeFoldingArrowsMode(FoldingArrowsMode.ON_MOUSE_HOVER);
        assertFalse(settings.isModified(copy));

        copy.setFoldDocComments(true);
        assertTrue(settings.isModified(copy));
    }

    @Test
    void testMutationAndPersistence() {
        boolean orig = settings.isJavaInnerClasses();
        try {
            settings.setJavaInnerClasses(!orig);
            settings.save();

            CodeFoldingSettings loaded = new CodeFoldingSettings();
            assertEquals(!orig, loaded.isJavaInnerClasses());
        } finally {
            settings.setJavaInnerClasses(orig);
            settings.save();
        }
    }

    @Test
    void testListeners() {
        boolean[] called = {false};
        CodeFoldingSettings.Listener l = s -> called[0] = true;
        settings.addListener(l);

        settings.setFoldFileHeader(false);
        settings.save();

        assertTrue(called[0]);
        settings.removeListener(l);
    }
}
