package dev.lumina.ui;

import dev.lumina.naturallang.*;
import dev.lumina.naturallang.rules.*;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Natural Languages, Grammar and Style, and Spelling settings.
 * Tests catalog completeness matching user reference screenshots, dialect count badges,
 * proofreading scopes (18 file types and code elements), AI cloud toggles, SPI extensibility,
 * lifecycle (dirty tracking, apply, reset), UI page models, and strict brand isolation.
 */
public class SettingsNaturalLanguagesTest {

    private static boolean javaFxAvailable = false;
    private NaturalLanguagesManager manager;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> javaFxAvailable = true);
            javaFxAvailable = true;
        } catch (IllegalStateException e) {
            // Toolkit already initialized
            javaFxAvailable = true;
        } catch (Throwable t) {
            javaFxAvailable = false;
        }
    }

    @BeforeEach
    void setUp() {
        manager = NaturalLanguagesManager.getInstance();
        manager.resetToDefaults();
    }

    @Test
    void testAvailableLanguagesCatalogCompleteness() {
        List<NaturalLanguage> available = manager.getAvailableLanguages();
        assertFalse(available.isEmpty(), "Available languages should not be empty");
        assertTrue(available.size() >= 34, "Expected at least 34 available languages from catalog, found " + available.size());

        // Check English dialects
        NaturalLanguage enUs = available.stream().filter(l -> "en-US".equals(l.getCode())).findFirst().orElse(null);
        assertNotNull(enUs, "en-US must exist");
        assertEquals("English (USA)", enUs.getDisplayName());

        NaturalLanguage enCa = available.stream().filter(l -> "en-CA".equals(l.getCode())).findFirst().orElse(null);
        assertNotNull(enCa, "en-CA must exist");
        assertEquals("English (Canada)", enCa.getDisplayName());
        assertTrue(enCa.isDialect());

        NaturalLanguage enGb = available.stream().filter(l -> "en-GB".equals(l.getCode())).findFirst().orElse(null);
        assertNotNull(enGb, "en-GB must exist");
        assertEquals("English (Great Britain)", enGb.getDisplayName());
        assertTrue(enGb.isDialect());

        // Check dialect counts matching Screenshot 2
        NaturalLanguage nl = available.stream().filter(l -> "nl".equals(l.getCode())).findFirst().orElse(null);
        assertNotNull(nl, "Nederlands must exist");
        assertEquals(37, nl.getRuleCount(), "Nederlands dialect rule count should be 37");

        NaturalLanguage deDe = available.stream().filter(l -> "de-DE".equals(l.getCode())).findFirst().orElse(null);
        assertNotNull(deDe, "Deutsch (Deutschland) must exist");
        assertEquals(22, deDe.getRuleCount(), "Deutsch (Deutschland) dialect rule count should be 22");

        NaturalLanguage ga = available.stream().filter(l -> "ga".equals(l.getCode())).findFirst().orElse(null);
        assertNotNull(ga, "Gaeilge must exist");
        assertEquals(13, ga.getRuleCount(), "Gaeilge dialect rule count should be 13");

        NaturalLanguage ru = available.stream().filter(l -> "ru".equals(l.getCode())).findFirst().orElse(null);
        assertNotNull(ru, "Русский must exist");
        assertEquals(7, ru.getRuleCount(), "Русский dialect rule count should be 7");

        NaturalLanguage pl = available.stream().filter(l -> "pl".equals(l.getCode())).findFirst().orElse(null);
        assertNotNull(pl, "Polski must exist");
        assertEquals(5, pl.getRuleCount(), "Polski dialect rule count should be 5");

        NaturalLanguage ca = available.stream().filter(l -> "ca".equals(l.getCode())).findFirst().orElse(null);
        assertNotNull(ca, "Català must exist");
        assertEquals(4, ca.getRuleCount(), "Català dialect rule count should be 4");

        NaturalLanguage es = available.stream().filter(l -> "es".equals(l.getCode())).findFirst().orElse(null);
        assertNotNull(es, "Español must exist");
        assertEquals(3, es.getRuleCount(), "Español dialect rule count should be 3");

        NaturalLanguage fr = available.stream().filter(l -> "fr".equals(l.getCode())).findFirst().orElse(null);
        assertNotNull(fr, "Français must exist");
        assertEquals(2, fr.getRuleCount(), "Français dialect rule count should be 2");
    }

    @Test
    void testLanguagesAddRemoveAndLifecycle() {
        assertEquals(1, manager.getWorkingLanguages().size(), "Default should have 1 active language (en-US)");
        assertEquals("en-US", manager.getWorkingLanguages().get(0).getCode());
        assertFalse(manager.isModified(), "Initial state must not be modified");

        // Add Deutsch (Deutschland)
        NaturalLanguage deDe = manager.getAvailableLanguages().stream()
                .filter(l -> "de-DE".equals(l.getCode()))
                .findFirst()
                .orElseThrow();

        assertTrue(manager.addLanguage(deDe));
        assertEquals(2, manager.getWorkingLanguages().size());
        assertTrue(manager.isModified(), "Manager should be modified after adding a language");

        // Available to add list should no longer contain de-DE or en-US
        List<NaturalLanguage> availableToAdd = manager.getLanguagesAvailableToAdd();
        assertFalse(availableToAdd.stream().anyMatch(l -> "de-DE".equals(l.getCode())));
        assertFalse(availableToAdd.stream().anyMatch(l -> "en-US".equals(l.getCode())));

        // Apply changes
        manager.apply();
        assertFalse(manager.isModified(), "Manager must not be modified after apply");
        assertEquals(2, manager.getWorkingLanguages().size());

        // Remove Deutsch and reset
        assertTrue(manager.removeLanguage(deDe));
        assertEquals(1, manager.getWorkingLanguages().size());
        assertTrue(manager.isModified());

        manager.reset();
        assertFalse(manager.isModified(), "Manager must not be modified after reset");
        assertEquals(2, manager.getWorkingLanguages().size(), "Reset should restore the 2 applied languages");
    }

    @Test
    void testCloudToggleAutoFixAndOxfordSpelling() {
        assertFalse(manager.isCloudEnabled());
        assertFalse(manager.isAutoFix());
        assertFalse(manager.isOxfordSpelling());
        assertFalse(manager.isModified());

        manager.setCloudEnabled(true);
        assertTrue(manager.isModified());
        assertTrue(manager.isCloudEnabled());

        manager.setAutoFix(true);
        assertTrue(manager.isAutoFix());

        manager.setOxfordSpelling(true);
        assertTrue(manager.isOxfordSpelling());

        manager.apply();
        assertFalse(manager.isModified());
        assertTrue(manager.isCloudEnabled());
        assertTrue(manager.isAutoFix());
        assertTrue(manager.isOxfordSpelling());

        manager.setCloudEnabled(false);
        assertTrue(manager.isModified());
        manager.reset();
        assertFalse(manager.isModified());
        assertTrue(manager.isCloudEnabled());
    }

    @Test
    void testProofreadingScopeFileTypesAndCodeElements() {
        // Verify 18 default file types from Screenshots 3 & 4
        List<String> expectedTypes = List.of(
                "ChatInput", "Go", "HTML", "JSON", "Java", "JavaScript",
                "Kotlin", "Markdown", "Plain text", "Properties", "Python",
                "Ruby", "Rust", "SQL", "Scala", "TOML", "XML", "YAML"
        );
        assertEquals(18, NaturalLanguagesManager.DEFAULT_FILE_TYPES.size());
        for (String expected : expectedTypes) {
            assertTrue(manager.isFileTypeEnabled(expected), "File type should be enabled by default: " + expected);
        }

        // Verify default code elements check-in
        assertFalse(manager.isCheckStringLiterals(), "String literals should be unchecked by default");
        assertTrue(manager.isCheckComments(), "Comments should be checked by default");
        assertTrue(manager.isCheckDocumentation(), "Documentation should be checked by default");
        assertTrue(manager.isCheckCommitMessages(), "Commit messages should be checked by default");
        assertFalse(manager.isModified());

        // Disable Kotlin
        manager.setFileTypeEnabled("Kotlin", false);
        assertFalse(manager.isFileTypeEnabled("Kotlin"));
        assertTrue(manager.isModified());

        // Toggle String literals
        manager.setCheckStringLiterals(true);
        assertTrue(manager.isCheckStringLiterals());

        manager.apply();
        assertFalse(manager.isModified());
        assertFalse(manager.isFileTypeEnabled("Kotlin"));
        assertTrue(manager.isCheckStringLiterals());

        // Reset test
        manager.setFileTypeEnabled("Kotlin", true);
        assertTrue(manager.isModified());
        manager.reset();
        assertFalse(manager.isModified());
        assertFalse(manager.isFileTypeEnabled("Kotlin"));
    }

    @Test
    void testSpellingManagerSettings() {
        // Built-in dictionaries
        List<SpellingDictionaryItem> allDicts = manager.getAllDictionaries();
        assertTrue(allDicts.stream().anyMatch(d -> "Application-level dictionary".equals(d.name()) && d.isBuiltIn()));
        assertTrue(allDicts.stream().anyMatch(d -> "Project-level dictionary".equals(d.name()) && d.isBuiltIn()));

        // Single dictionary saving option
        assertFalse(manager.isUseSingleDictionary());
        assertEquals("project-level", manager.getSingleDictionaryTarget());

        manager.setUseSingleDictionary(true);
        manager.setSingleDictionaryTarget("application-level");
        assertTrue(manager.isModified());
        assertTrue(manager.isUseSingleDictionary());
        assertEquals("application-level", manager.getSingleDictionaryTarget());

        manager.apply();
        assertFalse(manager.isModified());

        // Add custom dictionary
        assertTrue(manager.addCustomDictionary("/path/to/custom.dic"));
        assertTrue(manager.isModified());
        assertTrue(manager.getCustomDictionaries().contains("/path/to/custom.dic"));

        // Add accepted word
        assertTrue(manager.addAcceptedWord("LuminaIDE"));
        assertTrue(manager.getAcceptedWords().contains("LuminaIDE"));

        manager.apply();
        assertFalse(manager.isModified());

        // Remove custom dictionary
        assertTrue(manager.removeCustomDictionary("/path/to/custom.dic"));
        assertTrue(manager.isModified());
        manager.reset();
        assertTrue(manager.getCustomDictionaries().contains("/path/to/custom.dic"));

        // Grammar exceptions (Exceptions tab)
        assertTrue(manager.getGrammarExceptions().isEmpty());
        assertTrue(manager.addGrammarException("ignore_special_syntax"));
        assertTrue(manager.isModified());
        assertTrue(manager.getGrammarExceptions().contains("ignore_special_syntax"));

        manager.apply();
        assertFalse(manager.isModified());
        assertTrue(manager.removeGrammarException("ignore_special_syntax"));
        assertTrue(manager.isModified());
        manager.reset();
        assertTrue(manager.getGrammarExceptions().contains("ignore_special_syntax"));
    }

    @Test
    void testSpellingDictionaryProviderSPI() {
        SpellingDictionaryProvider provider = new SpellingDictionaryProvider() {
            @Override
            public String getProviderName() {
                return "Medical Terms Dictionary";
            }

            @Override
            public List<String> getDictionaries() {
                return List.of("/path/to/medical.dic");
            }
        };

        manager.registerDictionaryProvider(provider);
        List<SpellingDictionaryItem> all = manager.getAllDictionaries();
        assertTrue(all.stream().anyMatch(d -> "/path/to/medical.dic".equals(d.path())));
    }

    @Test
    void testCustomLanguageProviderSPI() {
        NaturalLanguageProvider customProvider = new NaturalLanguageProvider() {
            @Override
            public String getProviderName() {
                return "Klingon Language Pack";
            }

            @Override
            public List<NaturalLanguage> getLanguages() {
                return List.of(new NaturalLanguage("tlh", "tlhIngan Hol", 12));
            }
        };

        manager.registerProvider(customProvider);
        NaturalLanguage klingon = manager.getAvailableLanguages().stream()
                .filter(l -> "tlh".equals(l.getCode()))
                .findFirst()
                .orElse(null);
        assertNotNull(klingon, "Custom SPI provider language must be registered");
        assertEquals("tlhIngan Hol", klingon.getDisplayName());
        assertEquals(12, klingon.getRuleCount());

        assertTrue(manager.addLanguage(klingon));
        assertTrue(manager.getWorkingLanguages().contains(klingon));
    }

    @Test
    void testUIComponentsIfJavaFXAvailable() {
        if (!javaFxAvailable) return;

        SettingsNaturalLanguagesPage nlPage = new SettingsNaturalLanguagesPage();
        assertNotNull(nlPage);
        assertFalse(nlPage.isModified());
        assertNotNull(nlPage.getLanguagesView());
        assertEquals(1, nlPage.getLanguagesView().getItems().size());

        SettingsGrammarAndStylePage gsPage = new SettingsGrammarAndStylePage();
        assertNotNull(gsPage);
        assertFalse(gsPage.isModified());
        assertEquals(18, gsPage.getFileTypeCheckboxes().size());
        assertTrue(gsPage.getCheckComments().isSelected());
        assertFalse(gsPage.getCheckStringLiterals().isSelected());

        // Check tree components in Grammar & Style
        assertEquals(16, gsPage.getTreeCategoryCheckBoxes().size());
        assertEquals(5, gsPage.getWritingStyleCombo().getItems().size());
        gsPage.expandCategory("Grammar");
        assertTrue(gsPage.getUserExpandedCategories().contains("Grammar"));
        assertNotNull(gsPage.getTreeRuleCheckBox("tree.grammar.19_century"));

        // Check Exceptions tab in Grammar & Style
        assertTrue(gsPage.getExceptionsList().isEmpty());
        assertTrue(gsPage.getRemoveExceptionBtn().isDisable());
        assertTrue(gsPage.getEmptyExceptionsPlaceholder().isVisible());

        // Check Spelling page matching Image 2
        SettingsSpellingPage spPage = new SettingsSpellingPage();
        assertNotNull(spPage);
        assertFalse(spPage.isModified());
        assertFalse(spPage.getUseSingleDictCheck().isSelected());
        assertEquals("project-level", spPage.getSingleDictCombo().getValue());
        assertTrue(spPage.getSingleDictCombo().isDisable());
        assertEquals(2, spPage.getDictionariesView().getItems().size());
        assertTrue(spPage.getDictionariesView().getItems().get(0).isBuiltIn());
        assertTrue(spPage.getAcceptedWordsView().getItems().isEmpty());
        assertTrue(spPage.getNoWordsPlaceholder().isVisible());
        assertTrue(spPage.getRemoveDictBtn().isDisable());
        assertTrue(spPage.getRemoveWordBtn().isDisable());
    }

    @Test
    void testProofreadingRulesCatalogCompleteness() {
        ProofreadingRulesManager rulesManager = ProofreadingRulesManager.getInstance();
        List<ProofreadingRule> allRules = rulesManager.getAllRules();

        assertEquals(43, allRules.size(), "Total proofreading rules should be 43 matching all reference screenshots");

        List<ProofreadingRule> general = rulesManager.getRulesByCategory(ProofreadingRuleCategory.GENERAL);
        assertEquals(6, general.size(), "General category should have 6 rules");

        List<ProofreadingRule> punctuation = rulesManager.getRulesByCategory(ProofreadingRuleCategory.PUNCTUATION);
        assertEquals(4, punctuation.size(), "Punctuation category should have 4 rules");

        List<ProofreadingRule> typography = rulesManager.getRulesByCategory(ProofreadingRuleCategory.TYPOGRAPHY);
        assertEquals(8, typography.size(), "Typography category should have 8 rules");

        List<ProofreadingRule> readability = rulesManager.getRulesByCategory(ProofreadingRuleCategory.READABILITY);
        assertEquals(11, readability.size(), "Readability category should have 11 rules");

        List<ProofreadingRule> formality = rulesManager.getRulesByCategory(ProofreadingRuleCategory.FORMALITY);
        assertEquals(9, formality.size(), "Formality category should have 9 rules");
        assertTrue(formality.stream().anyMatch(r -> "formality.avoid_colloquialism".equals(r.getId())));
        assertTrue(formality.stream().anyMatch(r -> "formality.avoid_shortened_word_forms".equals(r.getId())));
        assertTrue(formality.stream().anyMatch(r -> "formality.avoid_subject_ellipsis".equals(r.getId())));
        assertTrue(formality.stream().anyMatch(r -> "formality.check_pronouns_compound_subjects".equals(r.getId())));
        assertTrue(formality.stream().anyMatch(r -> "formality.check_verb_agreement_compound_or".equals(r.getId())));
        assertTrue(formality.stream().anyMatch(r -> "formality.spell_out_symbols_and".equals(r.getId())));
        assertTrue(formality.stream().anyMatch(r -> "formality.avoid_first_person_singular".equals(r.getId())));
        assertTrue(formality.stream().anyMatch(r -> "formality.avoid_first_person_plural".equals(r.getId())));
        assertTrue(formality.stream().anyMatch(r -> "formality.avoid_second_person".equals(r.getId())));

        List<ProofreadingRule> inclusivity = rulesManager.getRulesByCategory(ProofreadingRuleCategory.INCLUSIVITY);
        assertEquals(5, inclusivity.size(), "Inclusivity category should have 5 rules");
        assertTrue(inclusivity.stream().anyMatch(r -> "inclusivity.avoid_male_pronouns_gender_neutral".equals(r.getId())));
        assertTrue(inclusivity.stream().anyMatch(r -> "inclusivity.avoid_gender_specific_nouns".equals(r.getId())));
        assertTrue(inclusivity.stream().anyMatch(r -> "inclusivity.use_neutral_honorifics".equals(r.getId())));
        assertTrue(inclusivity.stream().anyMatch(r -> "inclusivity.avoid_words_racial_connotations".equals(r.getId())));
        assertTrue(inclusivity.stream().anyMatch(r -> "inclusivity.avoid_violent_language".equals(r.getId())));

        // Rule parameters and options checks
        ProofreadingRule contractions = allRules.stream()
                .filter(r -> "general.prefer_contractions".equals(r.getId()))
                .findFirst().orElseThrow();
        assertTrue(contractions.hasOption());
        assertEquals("Always", contractions.getOption().defaultChoice());
        assertTrue(contractions.hasExpandedDetails());

        ProofreadingRule serialCommas = allRules.stream()
                .filter(r -> "punctuation.use_serial_commas".equals(r.getId()))
                .findFirst().orElseThrow();
        assertTrue(serialCommas.hasOption());
        assertTrue(serialCommas.isRequiresCloud(), "Oxford commas rule requires Lumina AI Cloud");

        ProofreadingRule largeNumbers = allRules.stream()
                .filter(r -> "typography.format_large_numbers".equals(r.getId()))
                .findFirst().orElseThrow();
        assertTrue(largeNumbers.hasOption());
        assertEquals("Comma (10,000)", largeNumbers.getOption().defaultChoice());
    }

    @Test
    void testWritingStylesDropdownOptions() {
        assertEquals(5, ProofreadingRulesManager.WRITING_STYLES.size());
        assertEquals(List.of(
                "Casual (messengers, forums)",
                "Default",
                "Public (blog posts, documentation)",
                "Formal (official communication)",
                "Academic"
        ), ProofreadingRulesManager.WRITING_STYLES);
    }

    @Test
    void testOtherRulesTreeCatalogCompleteness() {
        ProofreadingRulesManager rulesManager = ProofreadingRulesManager.getInstance();
        List<ProofreadingTreeCategory> treeCats = rulesManager.getTreeCategories();

        assertEquals(16, treeCats.size(), "Expected 16 categories in 'Other rules' tree");

        List<String> expectedCategoryNames = List.of(
                "British English phrases",
                "Capitalization",
                "Collocations",
                "Commonly Confused Words",
                "Compounding",
                "Grammar",
                "Machine Learning",
                "Miscellaneous",
                "Nonstandard Phrases",
                "Orthographic errors",
                "Possible Typo",
                "Proper Nouns",
                "Punctuation",
                "Semantics",
                "Style",
                "Upper/Lowercase"
        );

        for (int i = 0; i < expectedCategoryNames.size(); i++) {
            assertEquals(expectedCategoryNames.get(i), treeCats.get(i).getName());
        }

        // Verify child rules in Grammar category (Image 5)
        ProofreadingTreeCategory grammarCat = treeCats.stream()
                .filter(c -> "Grammar".equals(c.getName()))
                .findFirst().orElseThrow();
        assertTrue(grammarCat.getRules().size() >= 11);
        assertTrue(grammarCat.getRules().stream().anyMatch(r -> "tree.grammar.19_century".equals(r.getId())));
        assertTrue(grammarCat.getRules().stream().anyMatch(r -> "tree.grammar.base_form_verbs".equals(r.getId())));
        assertTrue(grammarCat.getRules().stream().anyMatch(r -> "tree.grammar.gerund_infinitive".equals(r.getId())));
        assertTrue(grammarCat.getRules().stream().anyMatch(r -> "tree.grammar.afford_verbs_use".equals(r.getId())));
        assertTrue(grammarCat.getRules().stream().anyMatch(r -> "tree.grammar.afford_base_form".equals(r.getId())));
        assertTrue(grammarCat.getRules().stream().anyMatch(r -> "tree.grammar.afraid_of".equals(r.getId())));
        assertTrue(grammarCat.getRules().stream().anyMatch(r -> "tree.grammar.allow_to".equals(r.getId())));
        assertTrue(grammarCat.getRules().stream().anyMatch(r -> "tree.grammar.an_and_any".equals(r.getId())));
        assertTrue(grammarCat.getRules().stream().anyMatch(r -> "tree.grammar.arrive_proper_noun".equals(r.getId())));
        assertTrue(grammarCat.getRules().stream().anyMatch(r -> "tree.grammar.did_past_tense".equals(r.getId())));
        assertTrue(grammarCat.getRules().stream().anyMatch(r -> "tree.grammar.does_base_verb".equals(r.getId())));
    }

    @Test
    void testOtherRulesTreeStateAndLifecycle() {
        ProofreadingRulesManager rulesManager = ProofreadingRulesManager.getInstance();
        assertFalse(rulesManager.isModified());

        // Check initial category states
        assertTrue(rulesManager.isCategoryFullyEnabled("British English phrases"));
        assertTrue(rulesManager.isCategoryPartiallyEnabled("Grammar"));

        // Toggle individual tree rule
        boolean original = rulesManager.isTreeRuleEnabled("tree.grammar.19_century");
        rulesManager.setTreeRuleEnabled("tree.grammar.19_century", !original);
        assertTrue(rulesManager.isModified());
        assertTrue(manager.isModified());

        // Apply changes
        manager.apply();
        assertFalse(rulesManager.isModified());
        assertFalse(manager.isModified());
        assertEquals(!original, rulesManager.isTreeRuleEnabled("tree.grammar.19_century"));

        // Disable whole category
        rulesManager.setCategoryEnabled("British English phrases", false);
        assertFalse(rulesManager.isCategoryFullyEnabled("British English phrases"));
        assertFalse(rulesManager.isTreeRuleEnabled("tree.british.weekend"));
        assertTrue(rulesManager.isModified());

        // Reset
        manager.reset();
        assertFalse(rulesManager.isModified());
        assertTrue(rulesManager.isCategoryFullyEnabled("British English phrases"));
        assertTrue(rulesManager.isTreeRuleEnabled("tree.british.weekend"));
    }

    @Test
    void testTreeRulesSearchFiltering() {
        ProofreadingRulesManager rulesManager = ProofreadingRulesManager.getInstance();

        // Search "19 century"
        List<ProofreadingTreeCategory> matchCentury = rulesManager.getFilteredTreeCategories("19 century");
        assertEquals(1, matchCentury.size());
        assertEquals("Grammar", matchCentury.get(0).getName());
        assertEquals(1, matchCentury.get(0).getRules().size());
        assertEquals("tree.grammar.19_century", matchCentury.get(0).getRules().get(0).getId());

        // Search "Collocations"
        List<ProofreadingTreeCategory> matchColloc = rulesManager.getFilteredTreeCategories("Collocations");
        assertEquals(1, matchColloc.size());
        assertEquals("Collocations", matchColloc.get(0).getName());

        // Search "weekend"
        List<ProofreadingTreeCategory> matchWeekend = rulesManager.getFilteredTreeCategories("weekend");
        assertEquals(1, matchWeekend.size());
        assertEquals("British English phrases", matchWeekend.get(0).getName());
        assertEquals("tree.british.weekend", matchWeekend.get(0).getRules().get(0).getId());
    }

    @Test
    void testRulesSearchFiltering() {
        ProofreadingRulesManager rulesManager = ProofreadingRulesManager.getInstance();

        List<ProofreadingRule> searchArticles = rulesManager.getFilteredRules("articles");
        assertEquals(1, searchArticles.size());
        assertEquals("general.missing_articles", searchArticles.get(0).getId());

        List<ProofreadingRule> searchOxford = rulesManager.getFilteredRules("oxford");
        assertEquals(1, searchOxford.size());
        assertEquals("punctuation.use_serial_commas", searchOxford.get(0).getId());

        List<ProofreadingRule> searchDashes = rulesManager.getFilteredRules("en dash");
        assertTrue(searchDashes.size() >= 2);

        List<ProofreadingRule> searchEmpty = rulesManager.getFilteredRules("nonexistent_random_rule_query_12345");
        assertTrue(searchEmpty.isEmpty());
    }

    @Test
    void testRulesStateModificationAndLifecycle() {
        ProofreadingRulesManager rulesManager = ProofreadingRulesManager.getInstance();
        assertFalse(rulesManager.isModified());

        // Toggle a rule
        boolean original = rulesManager.isRuleEnabled("general.missing_articles");
        rulesManager.setRuleEnabled("general.missing_articles", !original);
        assertTrue(rulesManager.isModified());
        assertTrue(manager.isModified(), "NaturalLanguagesManager should be modified when rules change");

        // Change rule option
        rulesManager.setRuleOptionValue("general.prefer_contractions", "When unambiguous");
        assertEquals("When unambiguous", rulesManager.getRuleOptionValue("general.prefer_contractions"));

        // Change domain
        rulesManager.setDomain("Commit messages");
        assertEquals("Commit messages", rulesManager.getDomain());

        // Apply
        manager.apply();
        assertFalse(rulesManager.isModified());
        assertFalse(manager.isModified());
        assertEquals(!original, rulesManager.isRuleEnabled("general.missing_articles"));
        assertEquals("Commit messages", rulesManager.getDomain());

        // Change and Reset
        rulesManager.setDomain("Documentation");
        assertTrue(rulesManager.isModified());
        manager.reset();
        assertFalse(rulesManager.isModified());
        assertEquals("Commit messages", rulesManager.getDomain());
    }

    @Test
    void testCustomProofreadingRuleProviderSPI() {
        ProofreadingRulesManager rulesManager = ProofreadingRulesManager.getInstance();
        int initialCount = rulesManager.getAllRules().size();

        ProofreadingRuleProvider customRuleProvider = new ProofreadingRuleProvider() {
            @Override
            public String getProviderName() {
                return "Security Writing Rule Provider";
            }

            @Override
            public List<ProofreadingRule> getRules() {
                return List.of(new ProofreadingRule(
                        "security.avoid_plaintext_passwords",
                        ProofreadingRuleCategory.GENERAL,
                        "Warn on sensitive credential names",
                        "Avoid hardcoded secret references in documentation",
                        null,
                        List.of(new RuleExample("secret = 123", "secret = <hidden>")),
                        true,
                        null,
                        false,
                        null
                ));
            }
        };

        rulesManager.registerProvider(customRuleProvider);
        assertEquals(initialCount + 1, rulesManager.getAllRules().size());
        assertFalse(rulesManager.getFilteredRules("sensitive credential").isEmpty());
    }

    @Test
    void testStrictBrandIsolation() throws Exception {
        List<String> filesToCheck = List.of(
                "src/main/java/dev/lumina/naturallang/NaturalLanguage.java",
                "src/main/java/dev/lumina/naturallang/NaturalLanguageProvider.java",
                "src/main/java/dev/lumina/naturallang/BuiltInNaturalLanguageProvider.java",
                "src/main/java/dev/lumina/naturallang/NaturalLanguagesManager.java",
                "src/main/java/dev/lumina/naturallang/rules/ProofreadingRuleCategory.java",
                "src/main/java/dev/lumina/naturallang/rules/RuleExample.java",
                "src/main/java/dev/lumina/naturallang/rules/RuleOption.java",
                "src/main/java/dev/lumina/naturallang/rules/ProofreadingRule.java",
                "src/main/java/dev/lumina/naturallang/rules/ProofreadingRuleProvider.java",
                "src/main/java/dev/lumina/naturallang/rules/BuiltInProofreadingRuleProvider.java",
                "src/main/java/dev/lumina/naturallang/rules/ProofreadingRulesManager.java",
                "src/main/java/dev/lumina/naturallang/rules/ProofreadingTreeRule.java",
                "src/main/java/dev/lumina/naturallang/rules/ProofreadingTreeCategory.java",
                "src/main/java/dev/lumina/naturallang/rules/ProofreadingTreeRulesProvider.java",
                "src/main/java/dev/lumina/naturallang/rules/BuiltInProofreadingTreeRulesProvider.java",
                "src/main/java/dev/lumina/naturallang/SpellingDictionaryProvider.java",
                "src/main/java/dev/lumina/naturallang/SpellingDictionaryItem.java",
                "src/main/java/dev/lumina/ui/SettingsNaturalLanguagesPage.java",
                "src/main/java/dev/lumina/ui/SettingsGrammarAndStylePage.java",
                "src/main/java/dev/lumina/ui/SettingsSpellingPage.java",
                "src/main/java/dev/lumina/ui/AddNewWordDialog.java",
                "src/main/java/dev/lumina/ui/SelectPathDialog.java"
        );

        List<String> forbiddenBrands = List.of(
                new String(new char[]{'j','e','t','b','r','a','i','n','s'}),
                new String(new char[]{'i','n','t','e','l','l','i','j'}),
                new String(new char[]{'g','r','a','z','i','e'}),
                new String(new char[]{'c','l','i','o','n'}),
                new String(new char[]{'p','y','c','h','a','r','m'}),
                new String(new char[]{'w','e','b','s','t','o','r','m'}),
                new String(new char[]{'r','u','b','y','m','i','n','e'}),
                new String(new char[]{'r','i','d','e','r'}),
                new String(new char[]{'d','a','t','a','g','r','i','p'}),
                new String(new char[]{'a','p','p','c','o','d','e'}),
                new String(new char[]{'f','l','e','e','t'})
        );

        for (String filePath : filesToCheck) {
            File file = new File(filePath);
            assertTrue(file.exists(), "File should exist: " + filePath);
            String content = Files.readString(file.toPath()).toLowerCase();
            for (String brand : forbiddenBrands) {
                assertFalse(content.contains(brand),
                        "File " + filePath + " contains forbidden brand name: " + brand);
            }
        }
    }
}
