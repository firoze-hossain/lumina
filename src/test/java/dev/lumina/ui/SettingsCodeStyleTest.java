package dev.lumina.ui;

import dev.lumina.settings.AngularHtmlCodeStyleSettings;
import dev.lumina.settings.CodeStyleSettings;
import dev.lumina.settings.CodeStyleSettings.CodeStyleScheme;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;
import dev.lumina.settings.CodeStyleSettings.LineSeparator;
import dev.lumina.settings.EditorConfigCodeStyleSettings;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SettingsCodeStyleTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                javaFxAvailable = true;
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyStarted) {
            try {
                Platform.setImplicitExit(false);
                CountDownLatch checkLatch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    javaFxAvailable = true;
                    checkLatch.countDown();
                });
                checkLatch.await(1, TimeUnit.SECONDS);
            } catch (Throwable t) {
                javaFxAvailable = false;
            }
        } catch (Throwable ignored) {
            javaFxAvailable = false;
        }
    }

    @Test
    void testCodeStyleSettingsDefaultsAndPersistence() {
        CodeStyleSettings settings = CodeStyleSettings.getInstance();
        assertNotNull(settings);

        CodeStyleScheme active = settings.getActiveScheme();
        assertNotNull(active, "Active code style scheme must exist");
        assertTrue("Default".equals(active.getName()) || "Project".equals(active.getName()));

        assertEquals(120, active.getHardWrapAt(), "Default hard wrap should be 120");
        assertFalse(active.isWrapOnTyping(), "Default wrap on typing should be false");
        assertTrue(active.isDetectAndUseExistingFileIndents(), "Default detect indents should be true");
        assertTrue(active.isEnableEditorConfigSupport(), "Default editorconfig support should be true");
        assertEquals("@formatter:off", active.getFormatterOffMarker());
        assertEquals("@formatter:on", active.getFormatterOnMarker());

        // Modify and test save/load
        int oldHardWrap = active.getHardWrapAt();
        try {
            active.setHardWrapAt(100);
            settings.saveSettings();
            settings.loadSettings();
            assertEquals(100, settings.getActiveScheme().getHardWrapAt());
        } finally {
            active.setHardWrapAt(oldHardWrap);
            settings.saveSettings();
        }
    }

    @Test
    void testLanguageCodeStyleProviders() {
        List<LanguageCodeStyleProvider> providers = LanguageCodeStyleProvider.getAllProviders();
        assertFalse(providers.isEmpty(), "Providers must not be empty");

        // Verify key languages from reference screenshots are present
        List<String> requiredLanguages = List.of(
                "Java", "Kotlin", "Angular HTML template", "EditorConfig", "ERB", "Go",
                "Gradle Declarative Configuration", "Groovy", "HTML", "HTTP Request",
                "JavaScript", "JSON", "JSP", "JSPX", "Markdown", "PHP", "Properties",
                "Protocol Buffer", "Protocol Buffer Text", "Python", "Qute", "Ruby",
                "Rust", "Scala", "Shell Script", "SQL", "TOML", "TypeScript", "XML", "YAML"
        );

        for (String lang : requiredLanguages) {
            LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider(lang);
            assertNotNull(provider, "Provider for " + lang + " must be registered");
            assertEquals(lang, provider.getDisplayName());
            assertNotNull(provider.getSampleCode(), "Sample code for " + lang + " must not be null");
            assertFalse(provider.getSupportedTabs().isEmpty(), "Tabs for " + lang + " must not be empty");
        }

        // Verify specific defaults
        LanguageCodeStyleProvider javaProvider = LanguageCodeStyleProvider.getProvider("Java");
        LanguageCodeStyleSettings javaSettings = javaProvider.createDefaultSettings();
        assertEquals(4, javaSettings.getTabSize());
        assertEquals(4, javaSettings.getIndent());
        assertEquals(8, javaSettings.getContinuationIndent());
        assertFalse(javaSettings.isUseTabCharacter());

        LanguageCodeStyleProvider goProvider = LanguageCodeStyleProvider.getProvider("Go");
        LanguageCodeStyleSettings goSettings = goProvider.createDefaultSettings();
        assertTrue(goSettings.isUseTabCharacter(), "Go should use tab characters by default");

        LanguageCodeStyleProvider jsProvider = LanguageCodeStyleProvider.getProvider("JavaScript");
        LanguageCodeStyleSettings jsSettings = jsProvider.createDefaultSettings();
        assertEquals(2, jsSettings.getIndent(), "JavaScript default indent should be 2");
    }

    @Test
    void testCodeSampleDynamicFormatting() {
        LanguageCodeStyleProvider javaProvider = LanguageCodeStyleProvider.getProvider("Java");
        String sample = javaProvider.getSampleCode();

        LanguageCodeStyleSettings settings = javaProvider.createDefaultSettings();
        settings.setIndent(4);
        String formatted4 = CodeStyleSettings.formatCodeSample(sample, settings);
        assertTrue(formatted4.contains("    public int[] X"), "Should have 4 spaces for 1 level indent");
        assertTrue(formatted4.contains("label1:"), "Label should be present");

        // Change indent to 2
        settings.setIndent(2);
        String formatted2 = CodeStyleSettings.formatCodeSample(sample, settings);
        assertTrue(formatted2.contains("  public int[] X"), "Should have 2 spaces for 1 level indent when indent=2");

        // Change to tabs
        settings.setIndent(4);
        settings.setUseTabCharacter(true);
        settings.setTabSize(4);
        String formattedTab = CodeStyleSettings.formatCodeSample(sample, settings);
        assertTrue(formattedTab.contains("\tpublic int[] X"), "Should have tab character when useTabCharacter=true");
    }

    @Test
    void testSchemeDuplicationAndExport() {
        CodeStyleSettings settings = CodeStyleSettings.getInstance();
        CodeStyleScheme custom = settings.duplicateScheme("Default", "Custom Team Scheme");

        assertNotNull(custom);
        assertEquals("Custom Team Scheme", custom.getName());
        assertEquals("Custom Team Scheme", settings.getActiveScheme().getName());

        // Test XML export
        String xml = settings.exportToXml(custom);
        assertNotNull(xml);
        assertTrue(xml.contains("<code_scheme name=\"Custom Team Scheme\""), "XML export must contain scheme name");
        assertTrue(xml.contains("<codeStyleSettings language=\"Java\">"), "XML export must contain Java settings");

        // Test EditorConfig export
        String ec = settings.exportToEditorConfig(custom);
        assertNotNull(ec);
        assertTrue(ec.contains("root = true"), "EditorConfig must contain root = true");
        assertTrue(ec.contains("[*.java]"), "EditorConfig must contain [*.java]");

        // Clean up
        settings.removeScheme("Custom Team Scheme");
        assertEquals("Default", settings.getActiveScheme().getName());
    }

    @Test
    void testSettingsCodeStylePageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeStylePage page = new SettingsCodeStylePage();
                assertNotNull(page);
                assertNotNull(page.getHeaderBar());
                assertFalse(page.isModified());

                // Test modifying settings
                page.getHeaderBar().getSchemeCombo().setValue("Project");
                assertNotNull(CodeStyleSettings.getInstance().getActiveScheme());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsCodeStyleLanguagePageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeStyleLanguagePage page = new SettingsCodeStyleLanguagePage("Java");
                assertNotNull(page);
                assertEquals("Java", page.getLanguageId());
                assertNotNull(page.getHeaderBar());
                assertFalse(page.isModified());

                // Reset and apply
                page.reset();
                page.apply();
                assertFalse(page.isModified());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testJavaCodeStyleSettingsModel() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();
        assertEquals(4, js.getTabSize());
        assertEquals(4, js.getIndent());
        assertEquals(8, js.getContinuationIndent());
        assertFalse(js.isUseTabCharacter());
        assertEquals(120, js.getHardWrapAt());

        // Test Google Style preset
        js.applyGoogleStyle();
        assertEquals(2, js.getIndent());
        assertEquals(2, js.getTabSize());
        assertEquals(100, js.getHardWrapAt());
        assertEquals(999, js.getClassCountToUseImportOnDemand());

        // Test Platform Default preset
        js.applyPlatformDefault();
        assertEquals(4, js.getIndent());
        assertEquals(120, js.getHardWrapAt());
        assertEquals(5, js.getClassCountToUseImportOnDemand());

        // Test sync with LanguageCodeStyleSettings
        CodeStyleSettings.LanguageCodeStyleSettings lcs = new CodeStyleSettings.LanguageCodeStyleSettings("Java");
        lcs.setIndent(6);
        lcs.setUseTabCharacter(true);
        js.syncFrom(lcs);
        assertEquals(6, js.getIndent());
        assertTrue(js.isUseTabCharacter());

        js.setIndent(8);
        js.syncTo(lcs);
        assertEquals(8, lcs.getIndent());
    }

    @Test
    void testWrappingAndBlankLinesSettings() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();

        // Blank lines defaults
        assertEquals(2, js.getKeepBlankLinesInDeclarations());
        assertEquals(2, js.getKeepBlankLinesInCode());
        assertEquals(2, js.getKeepBlankLinesBeforeRBrace());
        assertEquals(2, js.getKeepBlankLinesBetweenHeaderAndPackage());
        assertEquals(0, js.getBlankLinesBeforePackage());
        assertEquals(1, js.getBlankLinesAfterPackage());
        assertEquals(1, js.getBlankLinesBeforeImports());
        assertEquals(1, js.getBlankLinesAfterImports());
        assertEquals(1, js.getBlankLinesAroundClass());
        assertEquals(0, js.getBlankLinesAfterClassHeader());
        assertEquals(0, js.getBlankLinesBeforeClassEnd());
        assertEquals(0, js.getBlankLinesAfterAnonymousClassHeader());
        assertEquals(0, js.getBlankLinesBeforeFieldInInterface());
        assertEquals(0, js.getBlankLinesBeforeFieldWithoutAnnotations());
        assertEquals(0, js.getBlankLinesBeforeFieldWithAnnotations());
        assertEquals(1, js.getBlankLinesAroundMethodInInterface());
        assertEquals(1, js.getBlankLinesAroundMethod());
        assertEquals(0, js.getBlankLinesBeforeMethodBody());
        assertEquals(1, js.getBlankLinesAroundInitializer());
        assertEquals(0, js.getBlankLinesBetweenRecordComponents());

        // Wrapping and Braces defaults
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getMethodCallArgumentsWrap());
        assertFalse(js.isAlignMethodCallArguments());
        assertFalse(js.isTakePriorityOverCallChainWrapping());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getChainedMethodCallsWrap());
        assertFalse(js.isWrapFirstCall());
        assertFalse(js.isAlignChainedCallsMultiline());
        assertEquals("Method names", js.getBuilderMethods());
        assertFalse(js.isKeepBuilderMethodsIndents());
        assertFalse(js.isMoveSemicolonToNewLine());

        assertEquals("Do not force", js.getIfForceBraces());
        assertFalse(js.isElseOnNewLine());
        assertTrue(js.isSpecialElseIfTreatment());

        assertEquals("Do not force", js.getForForceBraces());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getForStatementWrap());
        assertTrue(js.isAlignForMultiline());

        assertEquals("Do not force", js.getWhileForceBraces());
        assertEquals("Do not force", js.getDoWhileForceBraces());
        assertFalse(js.isWhileOnNewLine());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_IF_LONG, js.getSwitchWrap());
        assertTrue(js.isIndentCaseBranches());
        assertTrue(js.isEachCaseOnSeparateLine());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getTryWithResourcesWrap());
        assertTrue(js.isAlignTryWithResourcesMultiline());
        assertFalse(js.isCatchOnNewLine());
        assertFalse(js.isFinallyOnNewLine());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getBinaryExpressionsWrap());
        assertFalse(js.isAlignBinaryExpressionsMultiline());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getAssignmentWrap());
        assertFalse(js.isAlignAssignmentMultiline());
        assertFalse(js.isAlignFieldsInColumns());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getTernaryWrap());
        assertFalse(js.isAlignTernaryMultiline());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getArrayInitializerWrap());
        assertFalse(js.isAlignArrayInitializerMultiline());
        assertFalse(js.isWrapAfterModifierList());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getAssertStatementWrap());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getEnumConstantsWrap());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_ALWAYS, js.getClassAnnotationsWrap());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_ALWAYS, js.getMethodAnnotationsWrap());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_ALWAYS, js.getFieldAnnotationsWrap());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getParameterAnnotationsWrap());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getLocalVariableAnnotationsWrap());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getEnumFieldAnnotationsWrap());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.DO_NOT_WRAP, js.getAnnotationParametersWrap());
        assertFalse(js.isAlignAnnotationParametersMultiline());

        assertFalse(js.isAlignTextBlocksMultiline());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_IF_LONG, js.getRecordComponentsWrap());
        assertTrue(js.isAlignRecordComponentsMultiline());

        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_IF_LONG, js.getDeconstructionPatternsWrap());
        assertTrue(js.isAlignDeconstructionPatternsMultiline());

        // Test copy and equality
        dev.lumina.settings.JavaCodeStyleSettings copy = js.copy();
        assertEquals(js, copy);

        copy.setBlankLinesAroundClass(3);
        assertNotEquals(js, copy);

        dev.lumina.settings.JavaCodeStyleSettings copy2 = js.copy();
        copy2.setChainedMethodCallsWrap(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_ALWAYS);
        assertNotEquals(js, copy2);
    }

    @Test
    void testJavaDocAndImportsSettings() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();

        // JavaDoc defaults matching Screenshot 1
        assertTrue(js.isEnableJavaDocFormatting());
        assertTrue(js.isAlignParamDescriptions());
        assertTrue(js.isAlignThrownExceptions());
        assertTrue(js.isBlankLinesAfterDescription());
        assertFalse(js.isBlankLinesAfterParamDescriptions());
        assertFalse(js.isBlankLinesAfterReturnTag());
        assertTrue(js.isKeepInvalidTags());
        assertTrue(js.isKeepEmptyParamTags());
        assertTrue(js.isKeepEmptyReturnTags());
        assertTrue(js.isKeepEmptyThrowsTags());
        assertFalse(js.isWrapAtRightMargin());
        assertTrue(js.isEnableLeadingAsterisks());
        assertTrue(js.isUseThrowsRatherThanException());
        assertTrue(js.isGeneratePOnEmptyLines());
        assertTrue(js.isKeepEmptyLines());
        assertFalse(js.isDoNotWrapOneLineComments());
        assertFalse(js.isPreserveLineFeeds());
        assertFalse(js.isParamDescriptionsOnNewLine());
        assertFalse(js.isIndentContinuationLines());

        // Imports defaults matching Screenshots 2, 3, 4, 5
        assertTrue(js.isUseSingleClassImport());
        assertFalse(js.isUseFullyQualifiedClassNames());
        assertFalse(js.isInsertInnerClassImports());
        assertTrue(js.getExcludedInnerClasses().isEmpty());
        assertTrue(js.isDoNotSeparateModuleImports());
        assertFalse(js.isDeleteUnusedModuleImports());
        assertEquals("If not already imported", js.getUseFqNamesInJavadoc());
        assertEquals(5, js.getClassCountToUseImportOnDemand());
        assertEquals(3, js.getNamesCountToUseStaticImportOnDemand());

        // Packages to Use Import with '*'
        assertNotNull(js.getPackagesToUseImportOnDemand());
        assertEquals(2, js.getPackagesToUseImportOnDemand().size());
        assertEquals("java.awt.*", js.getPackagesToUseImportOnDemand().get(0).getPackageName());
        assertFalse(js.getPackagesToUseImportOnDemand().get(0).isStatic());
        assertFalse(js.getPackagesToUseImportOnDemand().get(0).isWithSubpackages());
        assertEquals("javax.swing.*", js.getPackagesToUseImportOnDemand().get(1).getPackageName());

        // Options
        assertTrue(js.isPlaceOnDemandImportBeforeSingleClassImports());
        assertTrue(js.isLayoutStaticImportsSeparately());

        // Import layout
        assertNotNull(js.getImportLayout());
        assertEquals(7, js.getImportLayout().size());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.ImportLayoutEntry.EntryType.MODULE_IMPORTS, js.getImportLayout().get(0).getType());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.ImportLayoutEntry.EntryType.ALL_OTHER_IMPORTS, js.getImportLayout().get(1).getType());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.ImportLayoutEntry.EntryType.BLANK_LINE, js.getImportLayout().get(2).getType());
        assertEquals("javax.*", js.getImportLayout().get(3).getPackageName());
        assertTrue(js.getImportLayout().get(3).isWithSubpackages());
        assertEquals("java.*", js.getImportLayout().get(4).getPackageName());
        assertTrue(js.getImportLayout().get(4).isWithSubpackages());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.ImportLayoutEntry.EntryType.BLANK_LINE, js.getImportLayout().get(5).getType());
        assertEquals(dev.lumina.settings.JavaCodeStyleSettings.ImportLayoutEntry.EntryType.STATIC_ALL_OTHER_IMPORTS, js.getImportLayout().get(6).getType());

        // Test copy and equality
        dev.lumina.settings.JavaCodeStyleSettings copy = js.copy();
        assertEquals(js, copy);

        copy.setEnableJavaDocFormatting(false);
        assertNotEquals(js, copy);

        dev.lumina.settings.JavaCodeStyleSettings copy2 = js.copy();
        copy2.setClassCountToUseImportOnDemand(10);
        assertNotEquals(js, copy2);

        dev.lumina.settings.JavaCodeStyleSettings copy3 = js.copy();
        copy3.getPackagesToUseImportOnDemand().add(new dev.lumina.settings.JavaCodeStyleSettings.ImportEntry(false, "org.junit.*", false));
        assertNotEquals(js, copy3);
    }

    @Test
    void testSettingsCodeStyleJavaPageUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeStyleJavaPage page = new SettingsCodeStyleJavaPage();
                assertNotNull(page);
                assertNotNull(page.getHeaderBar());
                assertEquals("Tabs and Indents", page.getActiveTab());
                assertFalse(page.isModified());

                // Test switching tabs across all 9 Java tabs
                List<String> tabs = List.of(
                        "Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines",
                        "JavaDoc", "Imports", "Arrangement", "Code Generation", "Java EE Names"
                );
                for (String tab : tabs) {
                    page.setActiveTab(tab);
                    assertEquals(tab, page.getActiveTab());
                }

                // Verify Wrapping and Braces tab sample
                page.setActiveTab("Wrapping and Braces");
                assertTrue(page.getSampleForActiveTab().contains("ThisIsASampleClass"));
                assertTrue(page.getSampleForActiveTab().contains("0x0051"));

                // Verify Blank Lines tab sample
                page.setActiveTab("Blank Lines");
                assertTrue(page.getSampleForActiveTab().contains("dev.lumina.samples"));
                assertTrue(page.getSampleForActiveTab().contains("@NotNull"));

                // Modify setting
                page.getCurrentSettings().setIndent(2);
                assertTrue(page.isModified());

                // Reset and apply
                page.reset();
                assertFalse(page.isModified());

                // Modify wrapping setting
                page.getCurrentSettings().setMethodCallArgumentsWrap(dev.lumina.settings.JavaCodeStyleSettings.WrapOption.WRAP_ALWAYS);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Modify blank lines setting
                page.getCurrentSettings().setBlankLinesAroundClass(3);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Verify JavaDoc tab sample and modification
                page.setActiveTab("JavaDoc");
                assertTrue(page.getSampleForActiveTab().contains("package sample;"));
                assertTrue(page.getSampleForActiveTab().contains("public class Sample"));
                assertTrue(page.getSampleForActiveTab().contains("@param i"));
                assertTrue(page.getSampleForActiveTab().contains("@throws XXXException"));
                page.getCurrentSettings().setEnableJavaDocFormatting(false);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Verify Spaces tab and modification
                page.setActiveTab("Spaces");
                assertEquals("Spaces", page.getActiveTab());
                page.getCurrentSettings().setSpaceBeforeMethodDeclParen(true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Verify Imports tab and modification
                page.setActiveTab("Imports");
                assertEquals("Imports", page.getActiveTab());
                page.getCurrentSettings().setClassCountToUseImportOnDemand(8);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Verify Arrangement tab and modification
                page.setActiveTab("Arrangement");
                assertEquals("Arrangement", page.getActiveTab());
                page.getCurrentSettings().setKeepGettersAndSettersTogether(false);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Verify Code Generation tab and modification
                page.setActiveTab("Code Generation");
                assertEquals("Code Generation", page.getActiveTab());
                page.getCurrentSettings().setFieldPrefix("m_");
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Verify Java EE Names tab and modification
                page.setActiveTab("Java EE Names");
                assertEquals("Java EE Names", page.getActiveTab());
                page.getCurrentSettings().setEntityBeanSuffix("EJB");
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSettingsDialogCodeStyleRouting() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<SettingsDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsDialog dialog = new SettingsDialog(null, "Code Style");
                ref.set(dialog);

                assertNotNull(dialog.getCurrentCodeStylePage(), "Code Style page should be loaded");

                // Test selecting Java under Code Style -> Dedicated SettingsCodeStyleJavaPage
                dialog.selectCategory("Java");
                assertNotNull(dialog.getCurrentCodeStyleJavaPage(), "Dedicated Java Code Style page should be loaded for Java");

                // Test selecting Kotlin under Code Style -> Generic SettingsCodeStyleLanguagePage
                dialog.selectCategory("Kotlin");
                assertNotNull(dialog.getCurrentCodeStyleLanguagePage(), "Code Style Language page should be loaded for Kotlin");
                assertEquals("Kotlin", dialog.getCurrentCodeStyleLanguagePage().getLanguageId());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSpacesBeforeParenthesesAndAroundOperators() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();

        // 1. Before parentheses matching reference screenshots 1 & 2
        assertFalse(js.isSpaceBeforeMethodDeclParen());
        assertFalse(js.isSpaceBeforeMethodCallParen());
        assertTrue(js.isSpaceBeforeIfParen());
        assertTrue(js.isSpaceBeforeForParen());
        assertTrue(js.isSpaceBeforeWhileParen());
        assertTrue(js.isSpaceBeforeSwitchParen());
        assertTrue(js.isSpaceBeforeTryParen());
        assertTrue(js.isSpaceBeforeCatchParen());
        assertTrue(js.isSpaceBeforeSynchronizedParen());
        assertFalse(js.isSpaceBeforeAnnotationParens());
        assertFalse(js.isSpaceBeforeDeconstructionList());

        // 2. Around operators matching reference screenshots 1 & 2
        assertTrue(js.isSpaceAroundAssignmentOps());
        assertTrue(js.isSpaceAroundLogicalOps());
        assertTrue(js.isSpaceAroundEqualityOps());
        assertTrue(js.isSpaceAroundRelationalOps());
        assertTrue(js.isSpaceAroundBitwiseOps());
        assertTrue(js.isSpaceAroundAdditiveOps());
        assertTrue(js.isSpaceAroundMultiplicativeOps());
        assertTrue(js.isSpaceAroundShiftOps());
        assertFalse(js.isSpaceAroundUnaryOps());
        assertTrue(js.isSpaceAroundLambdaArrow());
        assertFalse(js.isSpaceAroundMethodRefDoubleColon());

        // Test copy and equality
        dev.lumina.settings.JavaCodeStyleSettings copy = js.copy();
        assertEquals(js, copy);

        copy.setSpaceBeforeMethodDeclParen(true);
        assertNotEquals(js, copy);

        dev.lumina.settings.JavaCodeStyleSettings copy2 = js.copy();
        copy2.setSpaceAroundUnaryOps(true);
        assertNotEquals(js, copy2);

        dev.lumina.settings.JavaCodeStyleSettings copy3 = js.copy();
        copy3.setSpaceBeforeSynchronizedParen(false);
        assertNotEquals(js, copy3);
    }

    @Test
    void testArrangementSettings() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();

        // Grouping rules defaults matching reference screenshots
        assertTrue(js.isKeepGettersAndSettersTogether());
        assertFalse(js.isKeepOverriddenMethodsTogether());
        assertEquals("keep order", js.getOverriddenMethodsOrder());
        assertFalse(js.isKeepDependentMethodsTogether());
        assertEquals("breadth-first order", js.getDependentMethodsOrder());

        // Matching rules default count is 26
        List<dev.lumina.settings.JavaCodeStyleSettings.ArrangementRule> rules = js.getMatchingRules();
        assertNotNull(rules);
        assertEquals(26, rules.size());

        // Verify key default rules matching reference screenshots
        assertEquals(List.of("field", "public", "static", "final"), rules.get(0).getTags());
        assertEquals(List.of("field", "protected", "static", "final"), rules.get(1).getTags());
        assertEquals(List.of("field", "package private", "static", "final"), rules.get(2).getTags());
        assertEquals(List.of("field", "private", "static", "final"), rules.get(3).getTags());
        assertEquals(List.of("initializer block", "static"), rules.get(8).getTags());
        assertEquals(List.of("constructor"), rules.get(19).getTags());
        assertEquals(List.of("method", "static"), rules.get(20).getTags());
        assertEquals(List.of("method"), rules.get(21).getTags());
        assertEquals(List.of("enum"), rules.get(22).getTags());
        assertEquals(List.of("interface"), rules.get(23).getTags());
        assertEquals(List.of("class", "static"), rules.get(24).getTags());
        assertEquals(List.of("class"), rules.get(25).getTags());

        // Test copy and equality
        dev.lumina.settings.JavaCodeStyleSettings copy = js.copy();
        assertEquals(js, copy);

        copy.setKeepGettersAndSettersTogether(false);
        assertNotEquals(js, copy);

        dev.lumina.settings.JavaCodeStyleSettings copy2 = js.copy();
        copy2.setOverriddenMethodsOrder("order by name");
        assertNotEquals(js, copy2);

        dev.lumina.settings.JavaCodeStyleSettings copy3 = js.copy();
        copy3.getMatchingRules().remove(0);
        assertNotEquals(js, copy3);
    }

    @Test
    void testCodeGenerationSettings() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();

        // Naming prefixes/suffixes defaults matching reference screenshots
        assertTrue(js.isPreferLongerNames());
        assertEquals("", js.getFieldPrefix());
        assertEquals("", js.getFieldSuffix());
        assertEquals("", js.getStaticFieldPrefix());
        assertEquals("", js.getStaticFieldSuffix());
        assertEquals("", js.getParameterPrefix());
        assertEquals("", js.getParameterSuffix());
        assertEquals("", js.getLocalVariablePrefix());
        assertEquals("", js.getLocalVariableSuffix());
        assertEquals("", js.getSubclassPrefix());
        assertEquals("Impl", js.getSubclassSuffix());
        assertEquals("", js.getTestClassPrefix());
        assertEquals("Test", js.getTestClassSuffix());

        // Default visibility
        assertEquals("Public", js.getDefaultVisibility());

        // Variable declaration
        assertFalse(js.isMakeGeneratedLocalsFinal());
        assertFalse(js.isMakeGeneratedParametersFinal());
        assertFalse(js.isUseVarForLocalVariables());

        // Comment code
        assertTrue(js.isLineCommentAtFirstColumn());
        assertFalse(js.isAddSpaceAtLineCommentStart());
        assertFalse(js.isEnforceOnReformat());
        assertTrue(js.isBlockCommentAtFirstColumn());
        assertFalse(js.isAddSpacesAroundBlockComments());

        // Override method signature
        assertTrue(js.isInsertOverride());
        assertFalse(js.isRepeatSynchronized());
        assertNotNull(js.getAnnotationsToCopy());
        assertTrue(js.getAnnotationsToCopy().isEmpty());
        assertFalse(js.isUseExternalAnnotations());
        assertTrue(js.isInsertTypeUseAnnotationsBeforeType());

        // Lambda body
        assertFalse(js.isUseClassIsInstanceAndCast());
        assertTrue(js.isReplaceNullCheckWithObjectsNonNull());
        assertTrue(js.isUseIntegerSumWhenPossible());

        // Test copy and equality
        dev.lumina.settings.JavaCodeStyleSettings copy = js.copy();
        assertEquals(js, copy);

        copy.setDefaultVisibility("Private");
        assertNotEquals(js, copy);

        dev.lumina.settings.JavaCodeStyleSettings copy2 = js.copy();
        copy2.setSubclassSuffix("Base");
        assertNotEquals(js, copy2);

        dev.lumina.settings.JavaCodeStyleSettings copy3 = js.copy();
        copy3.setAnnotationsToCopy(List.of("java.lang.Deprecated"));
        assertNotEquals(js, copy3);
    }

    @Test
    void testJavaEeNamesSettings() {
        dev.lumina.settings.JavaCodeStyleSettings js = new dev.lumina.settings.JavaCodeStyleSettings();

        // Entity Bean defaults matching reference screenshots
        assertEquals("", js.getEntityEjbClassPrefix());
        assertEquals("Bean", js.getEntityEjbClassSuffix());
        assertEquals("", js.getEntityHomeInterfacePrefix());
        assertEquals("Home", js.getEntityHomeInterfaceSuffix());
        assertEquals("", js.getEntityRemoteInterfacePrefix());
        assertEquals("", js.getEntityRemoteInterfaceSuffix());
        assertEquals("Local", js.getEntityLocalHomeInterfacePrefix());
        assertEquals("Home", js.getEntityLocalHomeInterfaceSuffix());
        assertEquals("Local", js.getEntityLocalInterfacePrefix());
        assertEquals("", js.getEntityLocalInterfaceSuffix());
        assertEquals("", js.getEntityEjbNameTagPrefix());
        assertEquals("EJB", js.getEntityEjbNameTagSuffix());
        assertEquals("", js.getEntityTransferObjectPrefix());
        assertEquals("VO", js.getEntityTransferObjectSuffix());
        assertEquals("java.lang.String", js.getEntityDefaultPkClass());

        // Session Bean
        assertEquals("", js.getSessionEjbClassPrefix());
        assertEquals("Bean", js.getSessionEjbClassSuffix());
        assertEquals("", js.getSessionHomeInterfacePrefix());
        assertEquals("Home", js.getSessionHomeInterfaceSuffix());
        assertEquals("", js.getSessionRemoteInterfacePrefix());
        assertEquals("", js.getSessionRemoteInterfaceSuffix());
        assertEquals("Local", js.getSessionLocalHomeInterfacePrefix());
        assertEquals("Home", js.getSessionLocalHomeInterfaceSuffix());
        assertEquals("Local", js.getSessionLocalInterfacePrefix());
        assertEquals("", js.getSessionLocalInterfaceSuffix());
        assertEquals("", js.getSessionServiceEndpointPrefix());
        assertEquals("Service", js.getSessionServiceEndpointSuffix());
        assertEquals("", js.getSessionEjbNameTagPrefix());
        assertEquals("EJB", js.getSessionEjbNameTagSuffix());

        // Servlet, MDB, Filter, Listener
        assertEquals("", js.getServletClassPrefix());
        assertEquals("", js.getServletClassSuffix());
        assertEquals("", js.getServletNameTagPrefix());
        assertEquals("", js.getServletNameTagSuffix());

        assertEquals("", js.getMdbEjbClassPrefix());
        assertEquals("Bean", js.getMdbEjbClassSuffix());
        assertEquals("", js.getMdbEjbNameTagPrefix());
        assertEquals("EJB", js.getMdbEjbNameTagSuffix());

        assertEquals("", js.getFilterClassPrefix());
        assertEquals("", js.getFilterClassSuffix());
        assertEquals("", js.getFilterNameTagPrefix());
        assertEquals("", js.getFilterNameTagSuffix());

        assertEquals("", js.getListenerClassPrefix());
        assertEquals("", js.getListenerClassSuffix());

        // Test copy and equality
        dev.lumina.settings.JavaCodeStyleSettings copy = js.copy();
        assertEquals(js, copy);

        copy.setEntityDefaultPkClass("java.lang.Long");
        assertNotEquals(js, copy);

        dev.lumina.settings.JavaCodeStyleSettings copy2 = js.copy();
        copy2.setSessionEjbClassSuffix("EJB");
        assertNotEquals(js, copy2);
    }

    @Test
    void testKotlinCodeStyleSettingsDefaults() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("Kotlin");
        assertNotNull(provider, "Kotlin provider must be registered");
        assertEquals("Kotlin", provider.getDisplayName());

        List<String> tabs = provider.getSupportedTabs();
        assertEquals(List.of(
                "Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines",
                "Imports", "Other", "Code Generation", "Load/Save"
        ), tabs);

        LanguageCodeStyleSettings settings = provider.createDefaultSettings();
        assertTrue(settings instanceof dev.lumina.settings.KotlinCodeStyleSettings);
        dev.lumina.settings.KotlinCodeStyleSettings ks = (dev.lumina.settings.KotlinCodeStyleSettings) settings;

        // Tabs and Indents
        assertEquals(4, ks.getTabSize());
        assertEquals(4, ks.getIndent());
        assertEquals(8, ks.getContinuationIndent());
        assertFalse(ks.isUseTabCharacter());
        assertFalse(ks.isSmartTabs());
        assertFalse(ks.isKeepIndentsOnEmptyLines());

        // Spaces - Before parentheses
        assertTrue(ks.isSpaceBeforeIfParentheses());
        assertTrue(ks.isSpaceBeforeForParentheses());
        assertTrue(ks.isSpaceBeforeWhileParentheses());
        assertTrue(ks.isSpaceBeforeCatchParentheses());
        assertTrue(ks.isSpaceBeforeWhenParentheses());

        // Spaces - Around operators
        assertTrue(ks.isSpaceAroundAssignmentOperators());
        assertTrue(ks.isSpaceAroundLogicalOperators());
        assertTrue(ks.isSpaceAroundEqualityOperators());
        assertTrue(ks.isSpaceAroundRelationalOperators());
        assertTrue(ks.isSpaceAroundAdditiveOperators());
        assertTrue(ks.isSpaceAroundMultiplicativeOperators());
        assertFalse(ks.isSpaceAroundUnaryOperators());
        assertFalse(ks.isSpaceAroundRangeOperators());
        assertTrue(ks.isSpaceAroundElvisOperator());

        // Spaces - Other
        assertFalse(ks.isSpaceBeforeComma());
        assertTrue(ks.isSpaceAfterComma());
        assertFalse(ks.isSpaceBeforeColonAfterDeclarationName());
        assertTrue(ks.isSpaceAfterColonBeforeDeclarationType());
        assertTrue(ks.isSpaceBeforeColonInNewTypeDefinition());
        assertTrue(ks.isSpaceAfterColonInNewTypeDefinition());
        assertTrue(ks.isSpaceInSimpleOneLineMethods());
        assertTrue(ks.isSpaceAroundArrowInFunctionTypes());
        assertTrue(ks.isSpaceAroundArrowInWhenClause());
        assertTrue(ks.isSpaceBeforeLambdaArrow());

        // Wrapping and Braces
        assertTrue(ks.isLineBreaksKeepWhenReformatting());
        assertFalse(ks.isCommentAtFirstColumnKeepWhenReformatting());
        assertEquals("Wrap if long", ks.getExtendsListWrap());
        assertFalse(ks.isAlignMultilineExtendsList());
        assertEquals("Chop down if long", ks.getFunctionParametersWrap());
        assertTrue(ks.isAlignMultilineFunctionParameters());
        assertTrue(ks.isNewLineAfterOpenParenFunctionParameters());
        assertTrue(ks.isPlaceCloseParenOnNewLineFunctionParameters());
        assertEquals("Chop down if long", ks.getFunctionArgumentsWrap());
        assertFalse(ks.isAlignMultilineFunctionArguments());
        assertTrue(ks.isNewLineAfterOpenParenFunctionArguments());
        assertTrue(ks.isPlaceCloseParenOnNewLineFunctionArguments());
        assertFalse(ks.isAlignMultilineFunctionParentheses());
        assertEquals("Wrap if long", ks.getChainedCallsWrap());
        assertFalse(ks.isWrapFirstCallChainedCalls());
        assertFalse(ks.isElseOnNewLine());
        assertTrue(ks.isIfCloseParenOnNewLine());
        assertFalse(ks.isWhileOnNewLine());
        assertFalse(ks.isCatchOnNewLine());
        assertFalse(ks.isFinallyOnNewLine());
        assertFalse(ks.isAlignMultilineBinaryExpressions());

        assertEquals("Wrap if long", ks.getAssignmentWrap());
        assertEquals("Do not wrap", ks.getEnumConstantsWrap());
        assertEquals("Wrap always", ks.getClassAnnotationsWrap());
        assertEquals("Wrap always", ks.getFunctionAnnotationsWrap());
        assertEquals("Wrap always", ks.getPropertyAnnotationsWrap());
        assertEquals("Do not wrap", ks.getParameterAnnotationsWrap());
        assertEquals("Do not wrap", ks.getLocalVariableAnnotationsWrap());
        assertEquals("Wrap always", ks.getPropertyContextParametersWrap());
        assertEquals("Wrap always", ks.getFunctionContextParametersWrap());

        assertFalse(ks.isAlignWhenBranchesInColumns());
        assertTrue(ks.isNewLineAfterMultilineWhenEntry());
        assertTrue(ks.isIndentBeforeArrowOnNewLine());
        assertFalse(ks.isPutLeftBraceOnNewLine());
        assertEquals("Wrap if long", ks.getExpressionBodyFunctionsWrap());
        assertEquals("Wrap if long", ks.getElvisExpressionsWrap());

        // Blank lines
        assertEquals(2, ks.getKeepBlankLinesInDeclarations());
        assertEquals(2, ks.getKeepBlankLinesInCode());
        assertEquals(2, ks.getKeepBlankLinesBeforeClosingBrace());
        assertEquals(0, ks.getMinBlankLinesAfterClassHeader());
        assertEquals(0, ks.getMinBlankLinesAroundWhenBranchesWithBraces());
        assertEquals(1, ks.getMinBlankLinesBeforeDeclarationWithCommentOrAnnotation());

        // Copy test
        dev.lumina.settings.KotlinCodeStyleSettings copy = ks.copy();
        assertEquals(ks.getAllProperties(), copy.getAllProperties());
        copy.setSpaceAroundRangeOperators(true);
        assertNotEquals(ks.getAllProperties(), copy.getAllProperties());
    }

    @Test
    void testKotlinCodeStyleSamples() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("Kotlin");
        assertNotNull(provider);

        String tabsSample = provider.getSampleCode("Tabs and Indents");
        assertTrue(tabsSample.contains("open class Some"));
        assertTrue(tabsSample.contains("10..<42"));
        assertTrue(tabsSample.contains("bar ?: 12"));

        String spacesSample = provider.getSampleCode("Spaces");
        assertTrue(spacesSample.contains("open class Some"));
        assertTrue(spacesSample.contains("when (test)"));

        String wrapSample = provider.getSampleCode("Wrapping and Braces");
        assertTrue(wrapSample.contains("@Deprecated(\"Foo\")"));
        assertTrue(wrapSample.contains("public class ThisIsASampleClass :"));
        assertTrue(wrapSample.contains("Comparable<*>"));
        assertTrue(wrapSample.contains("fun multilineMethod("));

        String blankLinesSample = provider.getSampleCode("Blank Lines");
        assertTrue(blankLinesSample.contains("class Foo"));
        assertTrue(blankLinesSample.contains("private var field1: Int = 1"));
        assertTrue(blankLinesSample.contains("when (field1)"));

        String importsSample = provider.getSampleCode("Imports");
        assertTrue(importsSample.contains("package dev.lumina.demo"));
        assertTrue(importsSample.contains("import java.util.List"));
    }

    @Test
    void testDynamicSettingsCodeStyleLanguagePageKotlinUI() throws Exception {
        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeStyleLanguagePage page = new SettingsCodeStyleLanguagePage("Kotlin");
                assertNotNull(page);
                assertEquals("Kotlin", page.getLanguageId());
                assertEquals("Tabs and Indents", page.getActiveTab());
                assertFalse(page.isModified());

                // Test switching tabs across all 8 Kotlin tabs
                List<String> tabs = List.of(
                        "Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines",
                        "Imports", "Other", "Code Generation", "Load/Save"
                );
                for (String tab : tabs) {
                    page.setActiveTab(tab);
                    assertEquals(tab, page.getActiveTab());
                }

                // Verify samples per tab
                page.setActiveTab("Wrapping and Braces");
                assertTrue(page.getSampleForActiveTab().contains("ThisIsASampleClass"));

                page.setActiveTab("Blank Lines");
                assertTrue(page.getSampleForActiveTab().contains("class Foo"));

                // Test modification of standard indent
                page.setActiveTab("Tabs and Indents");
                page.getCurrentSettings().setIndent(2);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Test modification of dynamic spaces property
                page.setActiveTab("Spaces");
                page.getCurrentSettings().setBoolean(dev.lumina.settings.KotlinCodeStyleSettings.SPACE_AROUND_RANGE_OPERATORS, true);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Test modification of wrapping combo
                page.setActiveTab("Wrapping and Braces");
                page.getCurrentSettings().setString(dev.lumina.settings.KotlinCodeStyleSettings.WRAP_FUNCTION_PARAMETERS, "Wrap always");
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

                // Test modification of blank lines number
                page.setActiveTab("Blank Lines");
                page.getCurrentSettings().setInt(dev.lumina.settings.KotlinCodeStyleSettings.BLANK_LINES_KEEP_IN_DECLARATIONS, 4);
                assertTrue(page.isModified());
                page.reset();
                assertFalse(page.isModified());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testKotlinDynamicPersistence() {
        CodeStyleSettings settings = CodeStyleSettings.getInstance();
        CodeStyleScheme scheme = settings.getActiveScheme();
        assertNotNull(scheme);

        LanguageCodeStyleSettings langSettings = scheme.getLanguageSettings("Kotlin");
        assertNotNull(langSettings);

        boolean oldRange = langSettings.getBoolean(dev.lumina.settings.KotlinCodeStyleSettings.SPACE_AROUND_RANGE_OPERATORS, false);
        int oldBlankLines = langSettings.getInt(dev.lumina.settings.KotlinCodeStyleSettings.BLANK_LINES_KEEP_IN_DECLARATIONS, 2);

        try {
            langSettings.setBoolean(dev.lumina.settings.KotlinCodeStyleSettings.SPACE_AROUND_RANGE_OPERATORS, true);
            langSettings.setInt(dev.lumina.settings.KotlinCodeStyleSettings.BLANK_LINES_KEEP_IN_DECLARATIONS, 3);
            settings.saveSettings();

            // Reload and verify
            settings.loadSettings();
            LanguageCodeStyleSettings reloaded = settings.getActiveScheme().getLanguageSettings("Kotlin");
            assertTrue(reloaded.getBoolean(dev.lumina.settings.KotlinCodeStyleSettings.SPACE_AROUND_RANGE_OPERATORS, false));
            assertEquals(3, reloaded.getInt(dev.lumina.settings.KotlinCodeStyleSettings.BLANK_LINES_KEEP_IN_DECLARATIONS, 2));
        } finally {
            langSettings.setBoolean(dev.lumina.settings.KotlinCodeStyleSettings.SPACE_AROUND_RANGE_OPERATORS, oldRange);
            langSettings.setInt(dev.lumina.settings.KotlinCodeStyleSettings.BLANK_LINES_KEEP_IN_DECLARATIONS, oldBlankLines);
            settings.saveSettings();
        }
    }

    @Test
    void testKotlinTabsDynamicPreviewVisibility() throws Exception {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("Kotlin");
        assertNotNull(provider);

        // Preview presence: Tabs 1-4 true, Tabs 5-8 false
        assertTrue(provider.hasPreview("Tabs and Indents"));
        assertTrue(provider.hasPreview("Spaces"));
        assertTrue(provider.hasPreview("Wrapping and Braces"));
        assertTrue(provider.hasPreview("Blank Lines"));
        assertFalse(provider.hasPreview("Imports"));
        assertFalse(provider.hasPreview("Other"));
        assertFalse(provider.hasPreview("Code Generation"));
        assertFalse(provider.hasPreview("Load/Save"));

        if (!javaFxAvailable) return;

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                SettingsCodeStyleLanguagePage page = new SettingsCodeStyleLanguagePage("Kotlin");

                // Tab 1: preview visible
                page.setActiveTab("Tabs and Indents");
                assertEquals("Tabs and Indents", page.getActiveTab());

                // Tab 5: Imports - full width options, preview pane removed
                page.setActiveTab("Imports");
                assertEquals("Imports", page.getActiveTab());

                // Tab 6: Other
                page.setActiveTab("Other");
                assertEquals("Other", page.getActiveTab());

                // Tab 7: Code Generation
                page.setActiveTab("Code Generation");
                assertEquals("Code Generation", page.getActiveTab());

                // Tab 8: Load/Save
                page.setActiveTab("Load/Save");
                assertEquals("Load/Save", page.getActiveTab());

                // Back to Spaces: preview pane restored
                page.setActiveTab("Spaces");
                assertEquals("Spaces", page.getActiveTab());

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testKotlinImportsAndTrailingCommaAndCodeGenOptions() {
        dev.lumina.settings.KotlinCodeStyleSettings ks = new dev.lumina.settings.KotlinCodeStyleSettings();

        // Imports
        assertEquals("WHEN_AT_LEAST", ks.getTopLevelImportMode());
        assertEquals(5, ks.getTopLevelImportThreshold());
        assertEquals("WHEN_AT_LEAST", ks.getJavaStaticsImportMode());
        assertEquals(3, ks.getJavaStaticsImportThreshold());
        assertFalse(ks.isInsertImportsForNestedClasses());
        assertTrue(ks.isImportAliasesSeparately());

        assertFalse(ks.getPackagesToUseImportOnDemand().isEmpty());
        assertEquals(3, ks.getPackagesToUseImportOnDemand().size());
        assertEquals("import java.util.*", ks.getPackagesToUseImportOnDemand().get(0).getPackageName());
        assertFalse(ks.getPackagesToUseImportOnDemand().get(0).isWithSubpackages());
        assertTrue(ks.getPackagesToUseImportOnDemand().get(1).isWithSubpackages());

        assertFalse(ks.getImportLayout().isEmpty());
        assertEquals(3, ks.getImportLayout().size());
        assertEquals("import javax.*", ks.getImportLayout().get(0).getPackageName());
        assertTrue(ks.getImportLayout().get(0).isWithSubpackages());
        assertEquals("import all alias imports", ks.getImportLayout().get(2).getPackageName());

        // Other (Trailing comma)
        assertFalse(ks.isTrailingCommaEnabled());
        assertTrue(ks.isTrailingCommaTypeParameterList());
        assertFalse(ks.isTrailingCommaDestructuringDeclaration());
        assertTrue(ks.isTrailingCommaWhenEntry());
        assertTrue(ks.isTrailingCommaFunctionLiteral());
        assertTrue(ks.isTrailingCommaValueParameterList());
        assertTrue(ks.isTrailingCommaContextReceiverList());
        assertFalse(ks.isTrailingCommaCollectionLiteralExpression());
        assertFalse(ks.isTrailingCommaTypeArgumentList());
        assertFalse(ks.isTrailingCommaIndices());
        assertFalse(ks.isTrailingCommaValueArgumentList());

        // Code Generation
        assertTrue(ks.isLineCommentAtFirstColumn());
        assertFalse(ks.isAddSpaceAtLineCommentStart());
        assertFalse(ks.isEnforceOnReformat());
        assertTrue(ks.isBlockCommentAtFirstColumn());
        assertFalse(ks.isAddSpacesAroundBlockComments());

        // Load/Save
        assertEquals("<ide defaults>", ks.getString(dev.lumina.settings.KotlinCodeStyleSettings.LOAD_SAVE_USE_DEFAULTS_FROM, "<ide defaults>"));
        ks.applyKotlinCodingConventions();
        assertEquals(4, ks.getContinuationIndent());
        assertEquals("Kotlin Coding Conventions", ks.getString(dev.lumina.settings.KotlinCodeStyleSettings.LOAD_SAVE_USE_DEFAULTS_FROM, ""));

        ks.applyIdeDefaults();
        assertEquals(8, ks.getContinuationIndent());
        assertEquals("<ide defaults>", ks.getString(dev.lumina.settings.KotlinCodeStyleSettings.LOAD_SAVE_USE_DEFAULTS_FROM, ""));
    }

    @Test
    void testAngularHtmlCodeStyleSettings() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("Angular HTML template");
        assertNotNull(provider, "Angular HTML template provider must be registered");
        assertEquals("Angular HTML template", provider.getLanguageId());
        assertEquals("Angular HTML template", provider.getDisplayName());

        assertEquals(List.of("Spaces", "Wrapping and Braces", "Arrangement"), provider.getSupportedTabs());
        assertTrue(provider.hasPreview("Spaces"));
        assertTrue(provider.hasPreview("Wrapping and Braces"));
        assertFalse(provider.hasPreview("Arrangement"), "Arrangement tab must have no preview pane");

        LanguageCodeStyleSettings settings = provider.createDefaultSettings();
        assertTrue(settings instanceof AngularHtmlCodeStyleSettings);
        AngularHtmlCodeStyleSettings as = (AngularHtmlCodeStyleSettings) settings;

        // Tabs and indents defaults
        assertEquals(2, as.getTabSize());
        assertEquals(2, as.getIndent());
        assertEquals(4, as.getContinuationIndent());
        assertFalse(as.isUseTabCharacter());

        // Spaces tab defaults (media_1790574549424.png)
        assertTrue(as.isSpacesWithinInterpolations());

        // Wrapping and Braces defaults (media_1790574558704.png)
        assertEquals("Default: None", as.getVisualGuides());
        assertEquals("Do not wrap", as.getInterpolationsWrap());
        assertTrue(as.isNewLineAfterOpenInterpolation());
        assertTrue(as.isNewLineBeforeCloseInterpolation());

        // Arrangement tab defaults (media_1790574570361.png)
        assertEquals(List.of("attribute"), as.getMatchingRules());

        // Copy independence
        AngularHtmlCodeStyleSettings copy = as.copy();
        assertEquals(as.getMatchingRules(), copy.getMatchingRules());
        copy.getMatchingRules().add("tag");
        assertNotEquals(as.getMatchingRules(), copy.getMatchingRules());
    }

    @Test
    void testEditorConfigCodeStyleSettings() {
        LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider("EditorConfig");
        assertNotNull(provider, "EditorConfig provider must be registered");
        assertEquals("EditorConfig", provider.getLanguageId());
        assertEquals("EditorConfig", provider.getDisplayName());

        assertEquals(List.of("Spaces", "Wrapping and Braces"), provider.getSupportedTabs());
        assertTrue(provider.hasPreview("Spaces"));
        assertTrue(provider.hasPreview("Wrapping and Braces"));

        LanguageCodeStyleSettings settings = provider.createDefaultSettings();
        assertTrue(settings instanceof EditorConfigCodeStyleSettings);
        EditorConfigCodeStyleSettings es = (EditorConfigCodeStyleSettings) settings;

        // Tabs and indents defaults
        assertEquals(4, es.getTabSize());
        assertEquals(4, es.getIndent());
        assertEquals(8, es.getContinuationIndent());
        assertFalse(es.isUseTabCharacter());

        // Spaces tab defaults (media_1790574580900.png)
        assertTrue(es.isSpacesAroundSeparator());
        assertFalse(es.isSpacesBeforeColon());
        assertFalse(es.isSpacesAfterColon());
        assertFalse(es.isSpacesBeforeComma());
        assertTrue(es.isSpacesAfterComma());

        // Wrapping and Braces defaults (media_1790574588939.png)
        assertEquals("Default: None", es.getVisualGuides());
        assertFalse(es.isAlignFieldsInColumns());

        // Option groups and right-aligned checkbox verification
        List<CodeStyleSettings.CodeStyleGroup> wrappingGroups = provider.getOptionGroups("Wrapping and Braces");
        assertNotNull(wrappingGroups);
        boolean foundAlignFields = false;
        for (CodeStyleSettings.CodeStyleGroup g : wrappingGroups) {
            for (CodeStyleSettings.CodeStyleOption opt : g.getOptions()) {
                if (EditorConfigCodeStyleSettings.WRAP_ALIGN_FIELDS_IN_COLUMNS.equals(opt.getKey())) {
                    assertTrue(opt.isRightAligned(), "Align fields in columns must be right aligned");
                    foundAlignFields = true;
                }
            }
        }
        assertTrue(foundAlignFields, "Align fields in columns option must be present in Wrapping and Braces");

        // Copy independence
        EditorConfigCodeStyleSettings copy = es.copy();
        assertEquals(es.isAlignFieldsInColumns(), copy.isAlignFieldsInColumns());
        copy.setAlignFieldsInColumns(true);
        assertNotEquals(es.isAlignFieldsInColumns(), copy.isAlignFieldsInColumns());
    }

    @Test
    void testAngularHtmlAndEditorConfigLanguagePages() {
        if (!javaFxAvailable) {
            System.out.println("JavaFX not available, skipping testAngularHtmlAndEditorConfigLanguagePages");
            return;
        }

        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                // Test Angular HTML template page
                SettingsCodeStyleLanguagePage angularPage = new SettingsCodeStyleLanguagePage("Angular HTML template");
                assertNotNull(angularPage);
                assertEquals("Spaces", angularPage.getActiveTab(), "Angular HTML template must default to Spaces tab");

                angularPage.setActiveTab("Wrapping and Braces");
                assertEquals("Wrapping and Braces", angularPage.getActiveTab());

                angularPage.setActiveTab("Arrangement");
                assertEquals("Arrangement", angularPage.getActiveTab());
                assertFalse(angularPage.isModified());

                // Test EditorConfig page
                SettingsCodeStyleLanguagePage editorConfigPage = new SettingsCodeStyleLanguagePage("EditorConfig");
                assertNotNull(editorConfigPage);
                assertEquals("Spaces", editorConfigPage.getActiveTab(), "EditorConfig must default to Spaces tab");

                editorConfigPage.setActiveTab("Wrapping and Braces");
                assertEquals("Wrapping and Braces", editorConfigPage.getActiveTab());
                assertFalse(editorConfigPage.isModified());

            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Language pages test timed out");
        } catch (InterruptedException e) {
            fail("Language pages test interrupted");
        }

        if (error.get() != null) {
            fail("Exception in JavaFX thread: " + error.get().getMessage(), error.get());
        }
    }

    @Test
    void testBrandIsolation() throws Exception {
        Pattern competitorPattern = Pattern.compile("(?i)\\b(intellij|jetbrains|idea)\\b");

        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/settings/CodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/JavaCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/KotlinCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/AngularHtmlCodeStyleSettings.java",
                "src/main/java/dev/lumina/settings/EditorConfigCodeStyleSettings.java",
                "src/main/java/dev/lumina/ui/CodeStyleHeaderBar.java",
                "src/main/java/dev/lumina/ui/SettingsCodeStylePage.java",
                "src/main/java/dev/lumina/ui/SettingsCodeStyleLanguagePage.java",
                "src/main/java/dev/lumina/ui/SettingsCodeStyleJavaPage.java"
        );

        for (String relPath : filesToCheck) {
            Path path = Path.of(relPath);
            assertTrue(Files.exists(path), "File must exist: " + relPath);
            String content = Files.readString(path);
            var matcher = competitorPattern.matcher(content);
            assertFalse(matcher.find(), "Competitor brand found in file " + relPath + ": " + (matcher.hitEnd() ? "" : matcher.group()));
        }
    }
}
