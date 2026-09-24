package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic persistent configuration model for IntelliJ IDEA-style Editor > General > Code Completion settings.
 * Backed by ~/.lumina/lumina.properties, supporting listeners, dirty tracking, and real-time updates.
 */
public final class CodeCompletionSettings {

    public enum MatchCaseMode {
        FIRST_LETTER_ONLY("First letter only"),
        ALL_LETTERS("All letters");

        private final String label;

        MatchCaseMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static MatchCaseMode fromLabel(String label) {
            for (MatchCaseMode m : values()) {
                if (m.label.equalsIgnoreCase(label) || m.name().equalsIgnoreCase(label)) {
                    return m;
                }
            }
            return FIRST_LETTER_ONLY;
        }
    }

    public enum SqlSuggestScope {
        SEARCH_PATH_ONLY("The current search path only"),
        CURRENT_SCOPE("The current scope"),
        ALL_SCHEMAS("All available schemas");

        private final String label;

        SqlSuggestScope(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static SqlSuggestScope fromLabel(String label) {
            for (SqlSuggestScope s : values()) {
                if (s.label.equalsIgnoreCase(label) || s.name().equalsIgnoreCase(label)) {
                    return s;
                }
            }
            return CURRENT_SCOPE;
        }
    }

    public enum SqlQualifyOption {
        ALWAYS("Always"),
        ON_COLLISIONS("On collisions"),
        NEVER("Never");

        private final String label;

        SqlQualifyOption(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static SqlQualifyOption fromLabel(String label) {
            for (SqlQualifyOption o : values()) {
                if (o.label.equalsIgnoreCase(label) || o.name().equalsIgnoreCase(label)) {
                    return o;
                }
            }
            return ALWAYS;
        }
    }

    public static class TableAliasEntry {
        private String tableName;
        private String customAlias;

        public TableAliasEntry() {
            this("", "");
        }

        public TableAliasEntry(String tableName, String customAlias) {
            this.tableName = tableName != null ? tableName : "";
            this.customAlias = customAlias != null ? customAlias : "";
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName != null ? tableName : "";
        }

        public String getCustomAlias() {
            return customAlias;
        }

        public void setCustomAlias(String customAlias) {
            this.customAlias = customAlias != null ? customAlias : "";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TableAliasEntry that)) return false;
            return Objects.equals(tableName, that.tableName) && Objects.equals(customAlias, that.customAlias);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tableName, customAlias);
        }

        public TableAliasEntry copy() {
            return new TableAliasEntry(tableName, customAlias);
        }
    }

    public static final List<String> ML_LANGUAGES = List.of(
            "Go", "Java", "JavaScript", "Kotlin", "PHP",
            "Python", "Ruby", "Rust", "SQL", "Scala", "Shell Script", "TypeScript"
    );

    private static final CodeCompletionSettings INSTANCE = new CodeCompletionSettings();

    public static CodeCompletionSettings getInstance() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface Listener {
        void onSettingsChanged(CodeCompletionSettings settings);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    // 1. General completion options
    private boolean matchCase = true;
    private MatchCaseMode matchCaseMode = MatchCaseMode.FIRST_LETTER_ONLY;
    private boolean autoInsertBasic = true;
    private boolean autoInsertTypeMatching = true;
    private boolean sortAlphabetically = false;
    private boolean showSuggestionsAsYouType = true;
    private boolean insertBySpaceOrDot = false;
    private boolean showDocPopup = false;
    private int docPopupDelayMs = 500;
    private boolean insertParentheses = true;

    // 2. Command completion
    private boolean commandCompletion = true;
    private boolean commandCompletionSeparateGroup = true;
    private boolean commandCompletionReadOnly = false;

    // 3. Machine learning-assisted completion
    private boolean mlSortSuggestions = true;
    private final Map<String, Boolean> mlLanguages = new LinkedHashMap<>();
    private boolean markPositionChanges = false;
    private boolean markRelevantItem = false;

    // 4. HTML
    private boolean htmlAutoPopupTagName = true;

    // 5. Python
    private boolean pythonSuggestImportable = true;

    // 6. JavaScript
    private boolean jsOnlyTypeBased = false;
    private boolean jsSuggestOptionalChaining = true;
    private boolean jsExpandMethodBodies = true;
    private boolean jsSuggestVariableParameterNames = false;
    private boolean jsSuggestClassFields = false;
    private boolean jsAddTypeAnnotations = false;

    // 7. Parameter info
    private boolean paramShowHints = false;
    private boolean paramShowPopup = true;
    private int paramPopupDelayMs = 1000;
    private boolean paramShowFullSignatures = false;

    // 8. Rust
    private boolean rustSuggestOutOfScope = true;
    private boolean rustHighlightMoveErrors = true;

    // 9. Ruby
    private boolean rubyMatchAcrossNamespaces = true;
    private boolean rubySuggestMethodsAfterColonColon = false;
    private boolean rubyPreselectFirstInEditors = true;
    private boolean rubyPreselectFirstInConsoles = true;

    // 10. SQL
    private SqlSuggestScope sqlSuggestObjectsFrom = SqlSuggestScope.CURRENT_SCOPE;
    private SqlQualifyOption sqlQualifyDatabase = SqlQualifyOption.ALWAYS;
    private SqlQualifyOption sqlQualifySchema = SqlQualifyOption.ALWAYS;
    private SqlQualifyOption sqlQualifyTableView = SqlQualifyOption.ALWAYS;
    private SqlQualifyOption sqlQualifyTableViewAlias = SqlQualifyOption.ALWAYS;
    private SqlQualifyOption sqlQualifyInBasic = SqlQualifyOption.ON_COLLISIONS;
    private SqlQualifyOption sqlQualifyInJoin = SqlQualifyOption.ALWAYS;
    private SqlQualifyOption sqlQualifyInRefactoring = SqlQualifyOption.ON_COLLISIONS;
    private SqlQualifyOption sqlQualifyInLiveTemplates = SqlQualifyOption.ON_COLLISIONS;
    private SqlQualifyOption sqlQualifyInDragDrop = SqlQualifyOption.ON_COLLISIONS;
    private boolean sqlJoinUseAliases = true;
    private boolean sqlJoinInvertOperands = false;
    private boolean sqlJoinSuggestNonStrictFk = true;
    private boolean sqlTableAliasAutoAdd = false;
    private boolean sqlTableAliasSuggest = true;
    private final List<TableAliasEntry> sqlTableAliases = new ArrayList<>();
    private String additionalCharsToAccept = "";

    public CodeCompletionSettings() {
        initDefaults();
        load();
    }

    public void initDefaults() {
        matchCase = true;
        matchCaseMode = MatchCaseMode.FIRST_LETTER_ONLY;
        autoInsertBasic = true;
        autoInsertTypeMatching = true;
        sortAlphabetically = false;
        showSuggestionsAsYouType = true;
        insertBySpaceOrDot = false;
        showDocPopup = false;
        docPopupDelayMs = 500;
        insertParentheses = true;

        commandCompletion = true;
        commandCompletionSeparateGroup = true;
        commandCompletionReadOnly = false;

        mlSortSuggestions = true;
        mlLanguages.clear();
        for (String lang : ML_LANGUAGES) {
            mlLanguages.put(lang, !"PHP".equals(lang)); // PHP unchecked by default in IntelliJ screenshot
        }
        markPositionChanges = false;
        markRelevantItem = false;

        htmlAutoPopupTagName = true;
        pythonSuggestImportable = true;

        jsOnlyTypeBased = false;
        jsSuggestOptionalChaining = true;
        jsExpandMethodBodies = true;
        jsSuggestVariableParameterNames = false;
        jsSuggestClassFields = false;
        jsAddTypeAnnotations = false;

        paramShowHints = false;
        paramShowPopup = true;
        paramPopupDelayMs = 1000;
        paramShowFullSignatures = false;

        rustSuggestOutOfScope = true;
        rustHighlightMoveErrors = true;

        rubyMatchAcrossNamespaces = true;
        rubySuggestMethodsAfterColonColon = false;
        rubyPreselectFirstInEditors = true;
        rubyPreselectFirstInConsoles = true;

        sqlSuggestObjectsFrom = SqlSuggestScope.CURRENT_SCOPE;
        sqlQualifyDatabase = SqlQualifyOption.ALWAYS;
        sqlQualifySchema = SqlQualifyOption.ALWAYS;
        sqlQualifyTableView = SqlQualifyOption.ALWAYS;
        sqlQualifyTableViewAlias = SqlQualifyOption.ALWAYS;
        sqlQualifyInBasic = SqlQualifyOption.ON_COLLISIONS;
        sqlQualifyInJoin = SqlQualifyOption.ALWAYS;
        sqlQualifyInRefactoring = SqlQualifyOption.ON_COLLISIONS;
        sqlQualifyInLiveTemplates = SqlQualifyOption.ON_COLLISIONS;
        sqlQualifyInDragDrop = SqlQualifyOption.ON_COLLISIONS;
        sqlJoinUseAliases = true;
        sqlJoinInvertOperands = false;
        sqlJoinSuggestNonStrictFk = true;
        sqlTableAliasAutoAdd = false;
        sqlTableAliasSuggest = true;
        sqlTableAliases.clear();
        additionalCharsToAccept = "";
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener l : listeners) {
            try {
                l.onSettingsChanged(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void load() {
        matchCase = getBool("matchCase", matchCase);
        matchCaseMode = MatchCaseMode.fromLabel(getStr("matchCaseMode", matchCaseMode.getLabel()));
        autoInsertBasic = getBool("autoInsertBasic", autoInsertBasic);
        autoInsertTypeMatching = getBool("autoInsertTypeMatching", autoInsertTypeMatching);
        sortAlphabetically = getBool("sortAlphabetically", sortAlphabetically);
        showSuggestionsAsYouType = getBool("showSuggestionsAsYouType", showSuggestionsAsYouType);
        insertBySpaceOrDot = getBool("insertBySpaceOrDot", insertBySpaceOrDot);
        showDocPopup = getBool("showDocPopup", showDocPopup);
        docPopupDelayMs = getInt("docPopupDelayMs", docPopupDelayMs);
        insertParentheses = getBool("insertParentheses", insertParentheses);

        commandCompletion = getBool("commandCompletion", commandCompletion);
        commandCompletionSeparateGroup = getBool("commandCompletionSeparateGroup", commandCompletionSeparateGroup);
        commandCompletionReadOnly = getBool("commandCompletionReadOnly", commandCompletionReadOnly);

        mlSortSuggestions = getBool("mlSortSuggestions", mlSortSuggestions);
        for (String lang : ML_LANGUAGES) {
            String val = Settings.get("editor.completion.ml.lang." + lang.toLowerCase().replace(' ', '_'));
            if (val != null) {
                mlLanguages.put(lang, Boolean.parseBoolean(val));
            }
        }
        markPositionChanges = getBool("markPositionChanges", markPositionChanges);
        markRelevantItem = getBool("markRelevantItem", markRelevantItem);

        htmlAutoPopupTagName = getBool("htmlAutoPopupTagName", htmlAutoPopupTagName);
        pythonSuggestImportable = getBool("pythonSuggestImportable", pythonSuggestImportable);

        jsOnlyTypeBased = getBool("jsOnlyTypeBased", jsOnlyTypeBased);
        jsSuggestOptionalChaining = getBool("jsSuggestOptionalChaining", jsSuggestOptionalChaining);
        jsExpandMethodBodies = getBool("jsExpandMethodBodies", jsExpandMethodBodies);
        jsSuggestVariableParameterNames = getBool("jsSuggestVariableParameterNames", jsSuggestVariableParameterNames);
        jsSuggestClassFields = getBool("jsSuggestClassFields", jsSuggestClassFields);
        jsAddTypeAnnotations = getBool("jsAddTypeAnnotations", jsAddTypeAnnotations);

        paramShowHints = getBool("paramShowHints", paramShowHints);
        paramShowPopup = getBool("paramShowPopup", paramShowPopup);
        paramPopupDelayMs = getInt("paramPopupDelayMs", paramPopupDelayMs);
        paramShowFullSignatures = getBool("paramShowFullSignatures", paramShowFullSignatures);

        rustSuggestOutOfScope = getBool("rustSuggestOutOfScope", rustSuggestOutOfScope);
        rustHighlightMoveErrors = getBool("rustHighlightMoveErrors", rustHighlightMoveErrors);

        rubyMatchAcrossNamespaces = getBool("rubyMatchAcrossNamespaces", rubyMatchAcrossNamespaces);
        rubySuggestMethodsAfterColonColon = getBool("rubySuggestMethodsAfterColonColon", rubySuggestMethodsAfterColonColon);
        rubyPreselectFirstInEditors = getBool("rubyPreselectFirstInEditors", rubyPreselectFirstInEditors);
        rubyPreselectFirstInConsoles = getBool("rubyPreselectFirstInConsoles", rubyPreselectFirstInConsoles);

        sqlSuggestObjectsFrom = SqlSuggestScope.fromLabel(getStr("sqlSuggestObjectsFrom", sqlSuggestObjectsFrom.getLabel()));
        sqlQualifyDatabase = SqlQualifyOption.fromLabel(getStr("sqlQualifyDatabase", sqlQualifyDatabase.getLabel()));
        sqlQualifySchema = SqlQualifyOption.fromLabel(getStr("sqlQualifySchema", sqlQualifySchema.getLabel()));
        sqlQualifyTableView = SqlQualifyOption.fromLabel(getStr("sqlQualifyTableView", sqlQualifyTableView.getLabel()));
        sqlQualifyTableViewAlias = SqlQualifyOption.fromLabel(getStr("sqlQualifyTableViewAlias", sqlQualifyTableViewAlias.getLabel()));
        sqlQualifyInBasic = SqlQualifyOption.fromLabel(getStr("sqlQualifyInBasic", sqlQualifyInBasic.getLabel()));
        sqlQualifyInJoin = SqlQualifyOption.fromLabel(getStr("sqlQualifyInJoin", sqlQualifyInJoin.getLabel()));
        sqlQualifyInRefactoring = SqlQualifyOption.fromLabel(getStr("sqlQualifyInRefactoring", sqlQualifyInRefactoring.getLabel()));
        sqlQualifyInLiveTemplates = SqlQualifyOption.fromLabel(getStr("sqlQualifyInLiveTemplates", sqlQualifyInLiveTemplates.getLabel()));
        sqlQualifyInDragDrop = SqlQualifyOption.fromLabel(getStr("sqlQualifyInDragDrop", sqlQualifyInDragDrop.getLabel()));
        sqlJoinUseAliases = getBool("sqlJoinUseAliases", sqlJoinUseAliases);
        sqlJoinInvertOperands = getBool("sqlJoinInvertOperands", sqlJoinInvertOperands);
        sqlJoinSuggestNonStrictFk = getBool("sqlJoinSuggestNonStrictFk", sqlJoinSuggestNonStrictFk);
        sqlTableAliasAutoAdd = getBool("sqlTableAliasAutoAdd", sqlTableAliasAutoAdd);
        sqlTableAliasSuggest = getBool("sqlTableAliasSuggest", sqlTableAliasSuggest);
        additionalCharsToAccept = getStr("additionalCharsToAccept", additionalCharsToAccept);

        // Load SQL table aliases
        String aliasesRaw = Settings.get("editor.completion.sql.tableAliases");
        sqlTableAliases.clear();
        if (aliasesRaw != null && !aliasesRaw.isBlank()) {
            String[] pairs = aliasesRaw.split(";");
            for (String pair : pairs) {
                if (!pair.isBlank()) {
                    String[] parts = pair.split("=", 2);
                    String tbl = parts[0].trim();
                    String als = parts.length > 1 ? parts[1].trim() : "";
                    if (!tbl.isEmpty()) {
                        sqlTableAliases.add(new TableAliasEntry(tbl, als));
                    }
                }
            }
        }
    }

    public void save() {
        putBool("matchCase", matchCase);
        putStr("matchCaseMode", matchCaseMode.getLabel());
        putBool("autoInsertBasic", autoInsertBasic);
        putBool("autoInsertTypeMatching", autoInsertTypeMatching);
        putBool("sortAlphabetically", sortAlphabetically);
        putBool("showSuggestionsAsYouType", showSuggestionsAsYouType);
        putBool("insertBySpaceOrDot", insertBySpaceOrDot);
        putBool("showDocPopup", showDocPopup);
        putInt("docPopupDelayMs", docPopupDelayMs);
        putBool("insertParentheses", insertParentheses);

        putBool("commandCompletion", commandCompletion);
        putBool("commandCompletionSeparateGroup", commandCompletionSeparateGroup);
        putBool("commandCompletionReadOnly", commandCompletionReadOnly);

        putBool("mlSortSuggestions", mlSortSuggestions);
        for (Map.Entry<String, Boolean> e : mlLanguages.entrySet()) {
            Settings.put("editor.completion.ml.lang." + e.getKey().toLowerCase().replace(' ', '_'), String.valueOf(e.getValue()));
        }
        putBool("markPositionChanges", markPositionChanges);
        putBool("markRelevantItem", markRelevantItem);

        putBool("htmlAutoPopupTagName", htmlAutoPopupTagName);
        putBool("pythonSuggestImportable", pythonSuggestImportable);

        putBool("jsOnlyTypeBased", jsOnlyTypeBased);
        putBool("jsSuggestOptionalChaining", jsSuggestOptionalChaining);
        putBool("jsExpandMethodBodies", jsExpandMethodBodies);
        putBool("jsSuggestVariableParameterNames", jsSuggestVariableParameterNames);
        putBool("jsSuggestClassFields", jsSuggestClassFields);
        putBool("jsAddTypeAnnotations", jsAddTypeAnnotations);

        putBool("paramShowHints", paramShowHints);
        putBool("paramShowPopup", paramShowPopup);
        putInt("paramPopupDelayMs", paramPopupDelayMs);
        putBool("paramShowFullSignatures", paramShowFullSignatures);

        putBool("rustSuggestOutOfScope", rustSuggestOutOfScope);
        putBool("rustHighlightMoveErrors", rustHighlightMoveErrors);

        putBool("rubyMatchAcrossNamespaces", rubyMatchAcrossNamespaces);
        putBool("rubySuggestMethodsAfterColonColon", rubySuggestMethodsAfterColonColon);
        putBool("rubyPreselectFirstInEditors", rubyPreselectFirstInEditors);
        putBool("rubyPreselectFirstInConsoles", rubyPreselectFirstInConsoles);

        putStr("sqlSuggestObjectsFrom", sqlSuggestObjectsFrom.getLabel());
        putStr("sqlQualifyDatabase", sqlQualifyDatabase.getLabel());
        putStr("sqlQualifySchema", sqlQualifySchema.getLabel());
        putStr("sqlQualifyTableView", sqlQualifyTableView.getLabel());
        putStr("sqlQualifyTableViewAlias", sqlQualifyTableViewAlias.getLabel());
        putStr("sqlQualifyInBasic", sqlQualifyInBasic.getLabel());
        putStr("sqlQualifyInJoin", sqlQualifyInJoin.getLabel());
        putStr("sqlQualifyInRefactoring", sqlQualifyInRefactoring.getLabel());
        putStr("sqlQualifyInLiveTemplates", sqlQualifyInLiveTemplates.getLabel());
        putStr("sqlQualifyInDragDrop", sqlQualifyInDragDrop.getLabel());
        putBool("sqlJoinUseAliases", sqlJoinUseAliases);
        putBool("sqlJoinInvertOperands", sqlJoinInvertOperands);
        putBool("sqlJoinSuggestNonStrictFk", sqlJoinSuggestNonStrictFk);
        putBool("sqlTableAliasAutoAdd", sqlTableAliasAutoAdd);
        putBool("sqlTableAliasSuggest", sqlTableAliasSuggest);
        putStr("additionalCharsToAccept", additionalCharsToAccept);

        // Serialize SQL table aliases
        StringBuilder sb = new StringBuilder();
        for (TableAliasEntry entry : sqlTableAliases) {
            if (entry != null && !entry.getTableName().isBlank()) {
                if (!sb.isEmpty()) sb.append(";");
                sb.append(entry.getTableName()).append("=").append(entry.getCustomAlias());
            }
        }
        Settings.put("editor.completion.sql.tableAliases", sb.toString());

        notifyListeners();
    }

    public void copyFrom(CodeCompletionSettings o) {
        this.matchCase = o.matchCase;
        this.matchCaseMode = o.matchCaseMode;
        this.autoInsertBasic = o.autoInsertBasic;
        this.autoInsertTypeMatching = o.autoInsertTypeMatching;
        this.sortAlphabetically = o.sortAlphabetically;
        this.showSuggestionsAsYouType = o.showSuggestionsAsYouType;
        this.insertBySpaceOrDot = o.insertBySpaceOrDot;
        this.showDocPopup = o.showDocPopup;
        this.docPopupDelayMs = o.docPopupDelayMs;
        this.insertParentheses = o.insertParentheses;

        this.commandCompletion = o.commandCompletion;
        this.commandCompletionSeparateGroup = o.commandCompletionSeparateGroup;
        this.commandCompletionReadOnly = o.commandCompletionReadOnly;

        this.mlSortSuggestions = o.mlSortSuggestions;
        this.mlLanguages.clear();
        this.mlLanguages.putAll(o.mlLanguages);
        this.markPositionChanges = o.markPositionChanges;
        this.markRelevantItem = o.markRelevantItem;

        this.htmlAutoPopupTagName = o.htmlAutoPopupTagName;
        this.pythonSuggestImportable = o.pythonSuggestImportable;

        this.jsOnlyTypeBased = o.jsOnlyTypeBased;
        this.jsSuggestOptionalChaining = o.jsSuggestOptionalChaining;
        this.jsExpandMethodBodies = o.jsExpandMethodBodies;
        this.jsSuggestVariableParameterNames = o.jsSuggestVariableParameterNames;
        this.jsSuggestClassFields = o.jsSuggestClassFields;
        this.jsAddTypeAnnotations = o.jsAddTypeAnnotations;

        this.paramShowHints = o.paramShowHints;
        this.paramShowPopup = o.paramShowPopup;
        this.paramPopupDelayMs = o.paramPopupDelayMs;
        this.paramShowFullSignatures = o.paramShowFullSignatures;

        this.rustSuggestOutOfScope = o.rustSuggestOutOfScope;
        this.rustHighlightMoveErrors = o.rustHighlightMoveErrors;

        this.rubyMatchAcrossNamespaces = o.rubyMatchAcrossNamespaces;
        this.rubySuggestMethodsAfterColonColon = o.rubySuggestMethodsAfterColonColon;
        this.rubyPreselectFirstInEditors = o.rubyPreselectFirstInEditors;
        this.rubyPreselectFirstInConsoles = o.rubyPreselectFirstInConsoles;

        this.sqlSuggestObjectsFrom = o.sqlSuggestObjectsFrom;
        this.sqlQualifyDatabase = o.sqlQualifyDatabase;
        this.sqlQualifySchema = o.sqlQualifySchema;
        this.sqlQualifyTableView = o.sqlQualifyTableView;
        this.sqlQualifyTableViewAlias = o.sqlQualifyTableViewAlias;
        this.sqlQualifyInBasic = o.sqlQualifyInBasic;
        this.sqlQualifyInJoin = o.sqlQualifyInJoin;
        this.sqlQualifyInRefactoring = o.sqlQualifyInRefactoring;
        this.sqlQualifyInLiveTemplates = o.sqlQualifyInLiveTemplates;
        this.sqlQualifyInDragDrop = o.sqlQualifyInDragDrop;
        this.sqlJoinUseAliases = o.sqlJoinUseAliases;
        this.sqlJoinInvertOperands = o.sqlJoinInvertOperands;
        this.sqlJoinSuggestNonStrictFk = o.sqlJoinSuggestNonStrictFk;
        this.sqlTableAliasAutoAdd = o.sqlTableAliasAutoAdd;
        this.sqlTableAliasSuggest = o.sqlTableAliasSuggest;
        this.sqlTableAliases.clear();
        for (TableAliasEntry e : o.sqlTableAliases) {
            this.sqlTableAliases.add(e.copy());
        }
        this.additionalCharsToAccept = o.additionalCharsToAccept;
    }

    public CodeCompletionSettings copy() {
        CodeCompletionSettings c = new CodeCompletionSettings();
        c.copyFrom(this);
        return c;
    }

    public boolean isModified(CodeCompletionSettings o) {
        if (o == null) return true;
        return this.matchCase != o.matchCase
                || this.matchCaseMode != o.matchCaseMode
                || this.autoInsertBasic != o.autoInsertBasic
                || this.autoInsertTypeMatching != o.autoInsertTypeMatching
                || this.sortAlphabetically != o.sortAlphabetically
                || this.showSuggestionsAsYouType != o.showSuggestionsAsYouType
                || this.insertBySpaceOrDot != o.insertBySpaceOrDot
                || this.showDocPopup != o.showDocPopup
                || this.docPopupDelayMs != o.docPopupDelayMs
                || this.insertParentheses != o.insertParentheses
                || this.commandCompletion != o.commandCompletion
                || this.commandCompletionSeparateGroup != o.commandCompletionSeparateGroup
                || this.commandCompletionReadOnly != o.commandCompletionReadOnly
                || this.mlSortSuggestions != o.mlSortSuggestions
                || !Objects.equals(this.mlLanguages, o.mlLanguages)
                || this.markPositionChanges != o.markPositionChanges
                || this.markRelevantItem != o.markRelevantItem
                || this.htmlAutoPopupTagName != o.htmlAutoPopupTagName
                || this.pythonSuggestImportable != o.pythonSuggestImportable
                || this.jsOnlyTypeBased != o.jsOnlyTypeBased
                || this.jsSuggestOptionalChaining != o.jsSuggestOptionalChaining
                || this.jsExpandMethodBodies != o.jsExpandMethodBodies
                || this.jsSuggestVariableParameterNames != o.jsSuggestVariableParameterNames
                || this.jsSuggestClassFields != o.jsSuggestClassFields
                || this.jsAddTypeAnnotations != o.jsAddTypeAnnotations
                || this.paramShowHints != o.paramShowHints
                || this.paramShowPopup != o.paramShowPopup
                || this.paramPopupDelayMs != o.paramPopupDelayMs
                || this.paramShowFullSignatures != o.paramShowFullSignatures
                || this.rustSuggestOutOfScope != o.rustSuggestOutOfScope
                || this.rustHighlightMoveErrors != o.rustHighlightMoveErrors
                || this.rubyMatchAcrossNamespaces != o.rubyMatchAcrossNamespaces
                || this.rubySuggestMethodsAfterColonColon != o.rubySuggestMethodsAfterColonColon
                || this.rubyPreselectFirstInEditors != o.rubyPreselectFirstInEditors
                || this.rubyPreselectFirstInConsoles != o.rubyPreselectFirstInConsoles
                || this.sqlSuggestObjectsFrom != o.sqlSuggestObjectsFrom
                || this.sqlQualifyDatabase != o.sqlQualifyDatabase
                || this.sqlQualifySchema != o.sqlQualifySchema
                || this.sqlQualifyTableView != o.sqlQualifyTableView
                || this.sqlQualifyTableViewAlias != o.sqlQualifyTableViewAlias
                || this.sqlQualifyInBasic != o.sqlQualifyInBasic
                || this.sqlQualifyInJoin != o.sqlQualifyInJoin
                || this.sqlQualifyInRefactoring != o.sqlQualifyInRefactoring
                || this.sqlQualifyInLiveTemplates != o.sqlQualifyInLiveTemplates
                || this.sqlQualifyInDragDrop != o.sqlQualifyInDragDrop
                || this.sqlJoinUseAliases != o.sqlJoinUseAliases
                || this.sqlJoinInvertOperands != o.sqlJoinInvertOperands
                || this.sqlJoinSuggestNonStrictFk != o.sqlJoinSuggestNonStrictFk
                || this.sqlTableAliasAutoAdd != o.sqlTableAliasAutoAdd
                || this.sqlTableAliasSuggest != o.sqlTableAliasSuggest
                || !Objects.equals(this.sqlTableAliases, o.sqlTableAliases)
                || !Objects.equals(this.additionalCharsToAccept, o.additionalCharsToAccept);
    }

    // Helper getters/setters
    private boolean getBool(String key, boolean def) {
        String val = Settings.get("editor.completion." + key);
        return val != null ? Boolean.parseBoolean(val) : def;
    }

    private void putBool(String key, boolean val) {
        Settings.put("editor.completion." + key, String.valueOf(val));
    }

    private int getInt(String key, int def) {
        String val = Settings.get("editor.completion." + key);
        if (val != null) {
            try { return Integer.parseInt(val); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private void putInt(String key, int val) {
        Settings.put("editor.completion." + key, String.valueOf(val));
    }

    private String getStr(String key, String def) {
        String val = Settings.get("editor.completion." + key);
        return val != null ? val : def;
    }

    private void putStr(String key, String val) {
        Settings.put("editor.completion." + key, val != null ? val : "");
    }

    // Getters and Setters
    public boolean isMatchCase() { return matchCase; }
    public void setMatchCase(boolean matchCase) { this.matchCase = matchCase; }

    public MatchCaseMode getMatchCaseMode() { return matchCaseMode; }
    public void setMatchCaseMode(MatchCaseMode matchCaseMode) { this.matchCaseMode = matchCaseMode; }

    public boolean isAutoInsertBasic() { return autoInsertBasic; }
    public void setAutoInsertBasic(boolean autoInsertBasic) { this.autoInsertBasic = autoInsertBasic; }

    public boolean isAutoInsertTypeMatching() { return autoInsertTypeMatching; }
    public void setAutoInsertTypeMatching(boolean autoInsertTypeMatching) { this.autoInsertTypeMatching = autoInsertTypeMatching; }

    public boolean isSortAlphabetically() { return sortAlphabetically; }
    public void setSortAlphabetically(boolean sortAlphabetically) { this.sortAlphabetically = sortAlphabetically; }

    public boolean isShowSuggestionsAsYouType() { return showSuggestionsAsYouType; }
    public void setShowSuggestionsAsYouType(boolean showSuggestionsAsYouType) { this.showSuggestionsAsYouType = showSuggestionsAsYouType; }

    public boolean isInsertBySpaceOrDot() { return insertBySpaceOrDot; }
    public void setInsertBySpaceOrDot(boolean insertBySpaceOrDot) { this.insertBySpaceOrDot = insertBySpaceOrDot; }

    public boolean isShowDocPopup() { return showDocPopup; }
    public void setShowDocPopup(boolean showDocPopup) { this.showDocPopup = showDocPopup; }

    public int getDocPopupDelayMs() { return docPopupDelayMs; }
    public void setDocPopupDelayMs(int docPopupDelayMs) { this.docPopupDelayMs = docPopupDelayMs; }

    public boolean isInsertParentheses() { return insertParentheses; }
    public void setInsertParentheses(boolean insertParentheses) { this.insertParentheses = insertParentheses; }

    public boolean isCommandCompletion() { return commandCompletion; }
    public void setCommandCompletion(boolean commandCompletion) { this.commandCompletion = commandCompletion; }

    public boolean isCommandCompletionSeparateGroup() { return commandCompletionSeparateGroup; }
    public void setCommandCompletionSeparateGroup(boolean commandCompletionSeparateGroup) { this.commandCompletionSeparateGroup = commandCompletionSeparateGroup; }

    public boolean isCommandCompletionReadOnly() { return commandCompletionReadOnly; }
    public void setCommandCompletionReadOnly(boolean commandCompletionReadOnly) { this.commandCompletionReadOnly = commandCompletionReadOnly; }

    public boolean isMlSortSuggestions() { return mlSortSuggestions; }
    public void setMlSortSuggestions(boolean mlSortSuggestions) { this.mlSortSuggestions = mlSortSuggestions; }

    public Map<String, Boolean> getMlLanguages() { return Collections.unmodifiableMap(mlLanguages); }
    public boolean isMlLanguageEnabled(String lang) { return mlLanguages.getOrDefault(lang, false); }
    public void setMlLanguageEnabled(String lang, boolean enabled) { mlLanguages.put(lang, enabled); }

    public boolean isMarkPositionChanges() { return markPositionChanges; }
    public void setMarkPositionChanges(boolean markPositionChanges) { this.markPositionChanges = markPositionChanges; }

    public boolean isMarkRelevantItem() { return markRelevantItem; }
    public void setMarkRelevantItem(boolean markRelevantItem) { this.markRelevantItem = markRelevantItem; }

    public boolean isHtmlAutoPopupTagName() { return htmlAutoPopupTagName; }
    public void setHtmlAutoPopupTagName(boolean htmlAutoPopupTagName) { this.htmlAutoPopupTagName = htmlAutoPopupTagName; }

    public boolean isPythonSuggestImportable() { return pythonSuggestImportable; }
    public void setPythonSuggestImportable(boolean pythonSuggestImportable) { this.pythonSuggestImportable = pythonSuggestImportable; }

    public boolean isJsOnlyTypeBased() { return jsOnlyTypeBased; }
    public void setJsOnlyTypeBased(boolean jsOnlyTypeBased) { this.jsOnlyTypeBased = jsOnlyTypeBased; }

    public boolean isJsSuggestOptionalChaining() { return jsSuggestOptionalChaining; }
    public void setJsSuggestOptionalChaining(boolean jsSuggestOptionalChaining) { this.jsSuggestOptionalChaining = jsSuggestOptionalChaining; }

    public boolean isJsExpandMethodBodies() { return jsExpandMethodBodies; }
    public void setJsExpandMethodBodies(boolean jsExpandMethodBodies) { this.jsExpandMethodBodies = jsExpandMethodBodies; }

    public boolean isJsSuggestVariableParameterNames() { return jsSuggestVariableParameterNames; }
    public void setJsSuggestVariableParameterNames(boolean jsSuggestVariableParameterNames) { this.jsSuggestVariableParameterNames = jsSuggestVariableParameterNames; }

    public boolean isJsSuggestClassFields() { return jsSuggestClassFields; }
    public void setJsSuggestClassFields(boolean jsSuggestClassFields) { this.jsSuggestClassFields = jsSuggestClassFields; }

    public boolean isJsAddTypeAnnotations() { return jsAddTypeAnnotations; }
    public void setJsAddTypeAnnotations(boolean jsAddTypeAnnotations) { this.jsAddTypeAnnotations = jsAddTypeAnnotations; }

    public boolean isParamShowHints() { return paramShowHints; }
    public void setParamShowHints(boolean paramShowHints) { this.paramShowHints = paramShowHints; }

    public boolean isParamShowPopup() { return paramShowPopup; }
    public void setParamShowPopup(boolean paramShowPopup) { this.paramShowPopup = paramShowPopup; }

    public int getParamPopupDelayMs() { return paramPopupDelayMs; }
    public void setParamPopupDelayMs(int paramPopupDelayMs) { this.paramPopupDelayMs = paramPopupDelayMs; }

    public boolean isParamShowFullSignatures() { return paramShowFullSignatures; }
    public void setParamShowFullSignatures(boolean paramShowFullSignatures) { this.paramShowFullSignatures = paramShowFullSignatures; }

    public boolean isRustSuggestOutOfScope() { return rustSuggestOutOfScope; }
    public void setRustSuggestOutOfScope(boolean rustSuggestOutOfScope) { this.rustSuggestOutOfScope = rustSuggestOutOfScope; }

    public boolean isRustHighlightMoveErrors() { return rustHighlightMoveErrors; }
    public void setRustHighlightMoveErrors(boolean rustHighlightMoveErrors) { this.rustHighlightMoveErrors = rustHighlightMoveErrors; }

    public boolean isRubyMatchAcrossNamespaces() { return rubyMatchAcrossNamespaces; }
    public void setRubyMatchAcrossNamespaces(boolean rubyMatchAcrossNamespaces) { this.rubyMatchAcrossNamespaces = rubyMatchAcrossNamespaces; }

    public boolean isRubySuggestMethodsAfterColonColon() { return rubySuggestMethodsAfterColonColon; }
    public void setRubySuggestMethodsAfterColonColon(boolean rubySuggestMethodsAfterColonColon) { this.rubySuggestMethodsAfterColonColon = rubySuggestMethodsAfterColonColon; }

    public boolean isRubyPreselectFirstInEditors() { return rubyPreselectFirstInEditors; }
    public void setRubyPreselectFirstInEditors(boolean rubyPreselectFirstInEditors) { this.rubyPreselectFirstInEditors = rubyPreselectFirstInEditors; }

    public boolean isRubyPreselectFirstInConsoles() { return rubyPreselectFirstInConsoles; }
    public void setRubyPreselectFirstInConsoles(boolean rubyPreselectFirstInConsoles) { this.rubyPreselectFirstInConsoles = rubyPreselectFirstInConsoles; }

    public SqlSuggestScope getSqlSuggestObjectsFrom() { return sqlSuggestObjectsFrom; }
    public void setSqlSuggestObjectsFrom(SqlSuggestScope sqlSuggestObjectsFrom) { this.sqlSuggestObjectsFrom = sqlSuggestObjectsFrom; }

    public SqlQualifyOption getSqlQualifyDatabase() { return sqlQualifyDatabase; }
    public void setSqlQualifyDatabase(SqlQualifyOption sqlQualifyDatabase) { this.sqlQualifyDatabase = sqlQualifyDatabase; }

    public SqlQualifyOption getSqlQualifySchema() { return sqlQualifySchema; }
    public void setSqlQualifySchema(SqlQualifyOption sqlQualifySchema) { this.sqlQualifySchema = sqlQualifySchema; }

    public SqlQualifyOption getSqlQualifyTableView() { return sqlQualifyTableView; }
    public void setSqlQualifyTableView(SqlQualifyOption sqlQualifyTableView) { this.sqlQualifyTableView = sqlQualifyTableView; }

    public SqlQualifyOption getSqlQualifyTableViewAlias() { return sqlQualifyTableViewAlias; }
    public void setSqlQualifyTableViewAlias(SqlQualifyOption sqlQualifyTableViewAlias) { this.sqlQualifyTableViewAlias = sqlQualifyTableViewAlias; }

    public SqlQualifyOption getSqlQualifyInBasic() { return sqlQualifyInBasic; }
    public void setSqlQualifyInBasic(SqlQualifyOption sqlQualifyInBasic) { this.sqlQualifyInBasic = sqlQualifyInBasic; }

    public SqlQualifyOption getSqlQualifyInJoin() { return sqlQualifyInJoin; }
    public void setSqlQualifyInJoin(SqlQualifyOption sqlQualifyInJoin) { this.sqlQualifyInJoin = sqlQualifyInJoin; }

    public SqlQualifyOption getSqlQualifyInRefactoring() { return sqlQualifyInRefactoring; }
    public void setSqlQualifyInRefactoring(SqlQualifyOption sqlQualifyInRefactoring) { this.sqlQualifyInRefactoring = sqlQualifyInRefactoring; }

    public SqlQualifyOption getSqlQualifyInLiveTemplates() { return sqlQualifyInLiveTemplates; }
    public void setSqlQualifyInLiveTemplates(SqlQualifyOption sqlQualifyInLiveTemplates) { this.sqlQualifyInLiveTemplates = sqlQualifyInLiveTemplates; }

    public SqlQualifyOption getSqlQualifyInDragDrop() { return sqlQualifyInDragDrop; }
    public void setSqlQualifyInDragDrop(SqlQualifyOption sqlQualifyInDragDrop) { this.sqlQualifyInDragDrop = sqlQualifyInDragDrop; }

    public boolean isSqlJoinUseAliases() { return sqlJoinUseAliases; }
    public void setSqlJoinUseAliases(boolean sqlJoinUseAliases) { this.sqlJoinUseAliases = sqlJoinUseAliases; }

    public boolean isSqlJoinInvertOperands() { return sqlJoinInvertOperands; }
    public void setSqlJoinInvertOperands(boolean sqlJoinInvertOperands) { this.sqlJoinInvertOperands = sqlJoinInvertOperands; }

    public boolean isSqlJoinSuggestNonStrictFk() { return sqlJoinSuggestNonStrictFk; }
    public void setSqlJoinSuggestNonStrictFk(boolean sqlJoinSuggestNonStrictFk) { this.sqlJoinSuggestNonStrictFk = sqlJoinSuggestNonStrictFk; }

    public boolean isSqlTableAliasAutoAdd() { return sqlTableAliasAutoAdd; }
    public void setSqlTableAliasAutoAdd(boolean sqlTableAliasAutoAdd) { this.sqlTableAliasAutoAdd = sqlTableAliasAutoAdd; }

    public boolean isSqlTableAliasSuggest() { return sqlTableAliasSuggest; }
    public void setSqlTableAliasSuggest(boolean sqlTableAliasSuggest) { this.sqlTableAliasSuggest = sqlTableAliasSuggest; }

    public List<TableAliasEntry> getSqlTableAliases() { return sqlTableAliases; }
    public void setSqlTableAliases(List<TableAliasEntry> list) {
        this.sqlTableAliases.clear();
        if (list != null) {
            for (TableAliasEntry e : list) {
                this.sqlTableAliases.add(e.copy());
            }
        }
    }

    public String getAdditionalCharsToAccept() { return additionalCharsToAccept; }
    public void setAdditionalCharsToAccept(String additionalCharsToAccept) { this.additionalCharsToAccept = additionalCharsToAccept != null ? additionalCharsToAccept : ""; }
}
