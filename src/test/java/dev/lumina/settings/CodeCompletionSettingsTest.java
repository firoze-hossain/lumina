package dev.lumina.settings;

import dev.lumina.settings.CodeCompletionSettings.MatchCaseMode;
import dev.lumina.settings.CodeCompletionSettings.SqlQualifyOption;
import dev.lumina.settings.CodeCompletionSettings.SqlSuggestScope;
import dev.lumina.settings.CodeCompletionSettings.TableAliasEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodeCompletionSettingsTest {

    private CodeCompletionSettings settings;

    @BeforeEach
    void setUp() {
        settings = new CodeCompletionSettings();
        settings.initDefaults();
    }

    @Test
    void testDefaultsMatchIntelliJScreenshots() {
        assertTrue(settings.isMatchCase());
        assertEquals(MatchCaseMode.FIRST_LETTER_ONLY, settings.getMatchCaseMode());
        assertTrue(settings.isAutoInsertBasic());
        assertTrue(settings.isAutoInsertTypeMatching());
        assertFalse(settings.isSortAlphabetically());
        assertTrue(settings.isShowSuggestionsAsYouType());
        assertFalse(settings.isInsertBySpaceOrDot());
        assertFalse(settings.isShowDocPopup());
        assertEquals(500, settings.getDocPopupDelayMs());
        assertTrue(settings.isInsertParentheses());

        // Command completion
        assertTrue(settings.isCommandCompletion());
        assertTrue(settings.isCommandCompletionSeparateGroup());
        assertFalse(settings.isCommandCompletionReadOnly());

        // ML
        assertTrue(settings.isMlSortSuggestions());
        assertTrue(settings.isMlLanguageEnabled("Java"));
        assertTrue(settings.isMlLanguageEnabled("Rust"));
        assertTrue(settings.isMlLanguageEnabled("Python"));
        assertFalse(settings.isMlLanguageEnabled("PHP")); // PHP is unchecked by default in screenshot!
        assertFalse(settings.isMarkPositionChanges());
        assertFalse(settings.isMarkRelevantItem());

        // Languages
        assertTrue(settings.isHtmlAutoPopupTagName());
        assertTrue(settings.isPythonSuggestImportable());
        assertFalse(settings.isJsOnlyTypeBased());
        assertTrue(settings.isJsSuggestOptionalChaining());
        assertTrue(settings.isJsExpandMethodBodies());
        assertFalse(settings.isJsSuggestVariableParameterNames());
        assertFalse(settings.isJsSuggestClassFields());
        assertFalse(settings.isJsAddTypeAnnotations());

        // Parameter info
        assertFalse(settings.isParamShowHints());
        assertTrue(settings.isParamShowPopup());
        assertEquals(1000, settings.getParamPopupDelayMs());
        assertFalse(settings.isParamShowFullSignatures());

        // Rust & Ruby
        assertTrue(settings.isRustSuggestOutOfScope());
        assertTrue(settings.isRustHighlightMoveErrors());
        assertTrue(settings.isRubyMatchAcrossNamespaces());
        assertFalse(settings.isRubySuggestMethodsAfterColonColon());
        assertTrue(settings.isRubyPreselectFirstInEditors());
        assertTrue(settings.isRubyPreselectFirstInConsoles());

        // SQL
        assertEquals(SqlSuggestScope.CURRENT_SCOPE, settings.getSqlSuggestObjectsFrom());
        assertEquals(SqlQualifyOption.ALWAYS, settings.getSqlQualifyDatabase());
        assertEquals(SqlQualifyOption.ALWAYS, settings.getSqlQualifySchema());
        assertEquals(SqlQualifyOption.ALWAYS, settings.getSqlQualifyTableView());
        assertEquals(SqlQualifyOption.ALWAYS, settings.getSqlQualifyTableViewAlias());
        assertEquals(SqlQualifyOption.ON_COLLISIONS, settings.getSqlQualifyInBasic());
        assertEquals(SqlQualifyOption.ALWAYS, settings.getSqlQualifyInJoin());
        assertEquals(SqlQualifyOption.ON_COLLISIONS, settings.getSqlQualifyInRefactoring());
        assertEquals(SqlQualifyOption.ON_COLLISIONS, settings.getSqlQualifyInLiveTemplates());
        assertEquals(SqlQualifyOption.ON_COLLISIONS, settings.getSqlQualifyInDragDrop());
        assertTrue(settings.isSqlJoinUseAliases());
        assertFalse(settings.isSqlJoinInvertOperands());
        assertTrue(settings.isSqlJoinSuggestNonStrictFk());
        assertFalse(settings.isSqlTableAliasAutoAdd());
        assertTrue(settings.isSqlTableAliasSuggest());
        assertTrue(settings.getSqlTableAliases().isEmpty());
        assertEquals("", settings.getAdditionalCharsToAccept());
    }

    @Test
    void testIsModified() {
        CodeCompletionSettings copy = settings.copy();
        assertFalse(settings.isModified(copy));

        copy.setSortAlphabetically(true);
        assertTrue(settings.isModified(copy));

        copy.setSortAlphabetically(false);
        assertFalse(settings.isModified(copy));

        copy.setMatchCaseMode(MatchCaseMode.ALL_LETTERS);
        assertTrue(settings.isModified(copy));

        copy.setMatchCaseMode(MatchCaseMode.FIRST_LETTER_ONLY);
        assertFalse(settings.isModified(copy));

        copy.setSqlSuggestObjectsFrom(SqlSuggestScope.ALL_SCHEMAS);
        assertTrue(settings.isModified(copy));
    }

    @Test
    void testTableAliasesPersistence() {
        settings.setSqlTableAliases(List.of(
                new TableAliasEntry("users", "u"),
                new TableAliasEntry("orders", "o")
        ));
        settings.save();

        CodeCompletionSettings loaded = new CodeCompletionSettings();
        loaded.load();
        assertEquals(2, loaded.getSqlTableAliases().size());
        assertEquals("users", loaded.getSqlTableAliases().get(0).getTableName());
        assertEquals("u", loaded.getSqlTableAliases().get(0).getCustomAlias());
        assertEquals("orders", loaded.getSqlTableAliases().get(1).getTableName());
        assertEquals("o", loaded.getSqlTableAliases().get(1).getCustomAlias());
    }

    @Test
    void testListenerNotification() {
        boolean[] called = {false};
        CodeCompletionSettings.Listener listener = s -> called[0] = true;
        settings.addListener(listener);

        settings.setPythonSuggestImportable(false);
        settings.save();

        assertTrue(called[0]);
        settings.removeListener(listener);
    }
}
