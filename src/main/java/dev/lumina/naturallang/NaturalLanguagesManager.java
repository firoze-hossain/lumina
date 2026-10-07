package dev.lumina.naturallang;

import dev.lumina.naturallang.rules.ProofreadingRulesManager;

import java.util.*;

/**
 * Central manager and registry for Natural Languages and Proofreading settings in Lumina.
 * Manages enabled languages, available language providers, AI cloud processing,
 * proofreading scopes (file types, code elements), rules, and exceptions.
 */
public class NaturalLanguagesManager {

    public static final List<String> DEFAULT_FILE_TYPES = List.of(
            "ChatInput", "Go", "HTML", "JSON", "Java", "JavaScript",
            "Kotlin", "Markdown", "Plain text", "Properties", "Python",
            "Ruby", "Rust", "SQL", "Scala", "TOML", "XML", "YAML"
    );

    public static final List<String> DEFAULT_BUNDLED_DICTIONARIES = List.of(
            "english.dic",
            "lumina.dic",
            "community.dic"
    );

    private static final NaturalLanguagesManager INSTANCE = new NaturalLanguagesManager();

    public static NaturalLanguagesManager getInstance() {
        return INSTANCE;
    }

    private final List<NaturalLanguageProvider> providers = new ArrayList<>();
    private final Map<String, NaturalLanguage> availableLanguages = new LinkedHashMap<>();

    // Committed settings
    private final List<NaturalLanguage> committedLanguages = new ArrayList<>();
    private boolean committedCloudEnabled = false;
    private boolean committedAutoFix = false;
    private boolean committedOxfordSpelling = false;
    private final Set<String> committedEnabledFileTypes = new LinkedHashSet<>(DEFAULT_FILE_TYPES);
    private boolean committedCheckStringLiterals = false;
    private boolean committedCheckComments = true;
    private boolean committedCheckDocumentation = true;
    private boolean committedCheckCommitMessages = true;

    // Committed spelling settings
    private final List<String> committedCustomDictionaries = new ArrayList<>();
    private final Set<String> committedAcceptedWords = new LinkedHashSet<>();
    private boolean committedUseSingleDictionary = false;
    private String committedSingleDictionaryTarget = "project-level";
    private final List<String> committedGrammarExceptions = new ArrayList<>();
    private boolean committedProcessCode = true;
    private boolean committedProcessComments = true;
    private boolean committedProcessLiterals = true;

    // Working settings (in-memory changes before apply)
    private final List<NaturalLanguage> workingLanguages = new ArrayList<>();
    private boolean workingCloudEnabled = false;
    private boolean workingAutoFix = false;
    private boolean workingOxfordSpelling = false;
    private final Set<String> workingEnabledFileTypes = new LinkedHashSet<>(DEFAULT_FILE_TYPES);
    private boolean workingCheckStringLiterals = false;
    private boolean workingCheckComments = true;
    private boolean workingCheckDocumentation = true;
    private boolean workingCheckCommitMessages = true;

    // Working spelling settings
    private final List<String> workingCustomDictionaries = new ArrayList<>();
    private final Set<String> workingAcceptedWords = new LinkedHashSet<>();
    private boolean workingUseSingleDictionary = false;
    private String workingSingleDictionaryTarget = "project-level";
    private final List<String> workingGrammarExceptions = new ArrayList<>();
    private boolean workingProcessCode = true;
    private boolean workingProcessComments = true;
    private boolean workingProcessLiterals = true;

    private final List<SpellingDictionaryProvider> dictionaryProviders = new ArrayList<>();

    private Runnable onModifiedListener;

    private NaturalLanguagesManager() {
        registerProvider(new BuiltInNaturalLanguageProvider());
        ProofreadingRulesManager.getInstance().setOnModifiedListener(this::fireModified);
        resetToDefaults();
    }

    public synchronized void registerProvider(NaturalLanguageProvider provider) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
            for (NaturalLanguage lang : provider.getLanguages()) {
                availableLanguages.put(lang.getCode(), lang);
            }
        }
    }

    public synchronized void resetToDefaults() {
        committedLanguages.clear();
        NaturalLanguage defaultEnUs = availableLanguages.get("en-US");
        if (defaultEnUs != null) {
            committedLanguages.add(defaultEnUs);
        } else {
            committedLanguages.add(new NaturalLanguage("en-US", "English (USA)", 1));
        }

        committedCloudEnabled = false;
        committedAutoFix = false;
        committedOxfordSpelling = false;

        committedEnabledFileTypes.clear();
        committedEnabledFileTypes.addAll(DEFAULT_FILE_TYPES);

        committedCheckStringLiterals = false;
        committedCheckComments = true;
        committedCheckDocumentation = true;
        committedCheckCommitMessages = true;

        committedCustomDictionaries.clear();
        committedAcceptedWords.clear();
        committedUseSingleDictionary = false;
        committedSingleDictionaryTarget = "project-level";
        committedGrammarExceptions.clear();
        committedProcessCode = true;
        committedProcessComments = true;
        committedProcessLiterals = true;

        ProofreadingRulesManager.getInstance().resetToDefaults();

        reset();
    }

    public synchronized void reset() {
        workingLanguages.clear();
        workingLanguages.addAll(committedLanguages);

        workingCloudEnabled = committedCloudEnabled;
        workingAutoFix = committedAutoFix;
        workingOxfordSpelling = committedOxfordSpelling;

        workingEnabledFileTypes.clear();
        workingEnabledFileTypes.addAll(committedEnabledFileTypes);

        workingCheckStringLiterals = committedCheckStringLiterals;
        workingCheckComments = committedCheckComments;
        workingCheckDocumentation = committedCheckDocumentation;
        workingCheckCommitMessages = committedCheckCommitMessages;

        workingCustomDictionaries.clear();
        workingCustomDictionaries.addAll(committedCustomDictionaries);

        workingAcceptedWords.clear();
        workingAcceptedWords.addAll(committedAcceptedWords);

        workingUseSingleDictionary = committedUseSingleDictionary;
        workingSingleDictionaryTarget = committedSingleDictionaryTarget;

        workingGrammarExceptions.clear();
        workingGrammarExceptions.addAll(committedGrammarExceptions);

        workingProcessCode = committedProcessCode;
        workingProcessComments = committedProcessComments;
        workingProcessLiterals = committedProcessLiterals;

        ProofreadingRulesManager.getInstance().reset();

        fireModified();
    }

    public synchronized void apply() {
        committedLanguages.clear();
        committedLanguages.addAll(workingLanguages);

        committedCloudEnabled = workingCloudEnabled;
        committedAutoFix = workingAutoFix;
        committedOxfordSpelling = workingOxfordSpelling;

        committedEnabledFileTypes.clear();
        committedEnabledFileTypes.addAll(workingEnabledFileTypes);

        committedCheckStringLiterals = workingCheckStringLiterals;
        committedCheckComments = workingCheckComments;
        committedCheckDocumentation = workingCheckDocumentation;
        committedCheckCommitMessages = workingCheckCommitMessages;

        committedCustomDictionaries.clear();
        committedCustomDictionaries.addAll(workingCustomDictionaries);

        committedAcceptedWords.clear();
        committedAcceptedWords.addAll(workingAcceptedWords);

        committedUseSingleDictionary = workingUseSingleDictionary;
        committedSingleDictionaryTarget = workingSingleDictionaryTarget;

        committedGrammarExceptions.clear();
        committedGrammarExceptions.addAll(workingGrammarExceptions);

        committedProcessCode = workingProcessCode;
        committedProcessComments = workingProcessComments;
        committedProcessLiterals = workingProcessLiterals;

        ProofreadingRulesManager.getInstance().apply();

        fireModified();
    }

    public synchronized boolean isModified() {
        if (!workingLanguages.equals(committedLanguages)) return true;
        if (workingCloudEnabled != committedCloudEnabled) return true;
        if (workingAutoFix != committedAutoFix) return true;
        if (workingOxfordSpelling != committedOxfordSpelling) return true;
        if (!workingEnabledFileTypes.equals(committedEnabledFileTypes)) return true;
        if (workingCheckStringLiterals != committedCheckStringLiterals) return true;
        if (workingCheckComments != committedCheckComments) return true;
        if (workingCheckDocumentation != committedCheckDocumentation) return true;
        if (workingCheckCommitMessages != committedCheckCommitMessages) return true;
        if (!workingCustomDictionaries.equals(committedCustomDictionaries)) return true;
        if (!workingAcceptedWords.equals(committedAcceptedWords)) return true;
        if (workingUseSingleDictionary != committedUseSingleDictionary) return true;
        if (!Objects.equals(workingSingleDictionaryTarget, committedSingleDictionaryTarget)) return true;
        if (!workingGrammarExceptions.equals(committedGrammarExceptions)) return true;
        if (workingProcessCode != committedProcessCode) return true;
        if (workingProcessComments != committedProcessComments) return true;
        if (workingProcessLiterals != committedProcessLiterals) return true;
        if (ProofreadingRulesManager.getInstance().isModified()) return true;
        return false;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // =========================================================================
    // Languages Management
    // =========================================================================

    public synchronized List<NaturalLanguage> getWorkingLanguages() {
        return Collections.unmodifiableList(new ArrayList<>(workingLanguages));
    }

    public synchronized List<NaturalLanguage> getAvailableLanguages() {
        return Collections.unmodifiableList(new ArrayList<>(availableLanguages.values()));
    }

    public synchronized List<NaturalLanguage> getLanguagesAvailableToAdd() {
        List<NaturalLanguage> result = new ArrayList<>();
        Set<String> addedCodes = new HashSet<>();
        for (NaturalLanguage l : workingLanguages) {
            addedCodes.add(l.getCode());
        }
        for (NaturalLanguage l : availableLanguages.values()) {
            if (!addedCodes.contains(l.getCode())) {
                result.add(l);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized boolean addLanguage(NaturalLanguage language) {
        if (language != null && !workingLanguages.contains(language)) {
            workingLanguages.add(language);
            fireModified();
            return true;
        }
        return false;
    }

    public synchronized boolean removeLanguage(NaturalLanguage language) {
        if (language != null && workingLanguages.contains(language)) {
            workingLanguages.remove(language);
            fireModified();
            return true;
        }
        return false;
    }

    // =========================================================================
    // General Toggles
    // =========================================================================

    public synchronized boolean isCloudEnabled() {
        return workingCloudEnabled;
    }

    public synchronized void setCloudEnabled(boolean cloudEnabled) {
        this.workingCloudEnabled = cloudEnabled;
        fireModified();
    }

    public synchronized boolean isAutoFix() {
        return workingAutoFix;
    }

    public synchronized void setAutoFix(boolean autoFix) {
        this.workingAutoFix = autoFix;
        fireModified();
    }

    public synchronized boolean isOxfordSpelling() {
        return workingOxfordSpelling;
    }

    public synchronized void setOxfordSpelling(boolean oxfordSpelling) {
        this.workingOxfordSpelling = oxfordSpelling;
        fireModified();
    }

    // =========================================================================
    // Scope (File types & Code elements)
    // =========================================================================

    public synchronized boolean isFileTypeEnabled(String fileType) {
        return workingEnabledFileTypes.contains(fileType);
    }

    public synchronized void setFileTypeEnabled(String fileType, boolean enabled) {
        if (enabled) {
            workingEnabledFileTypes.add(fileType);
        } else {
            workingEnabledFileTypes.remove(fileType);
        }
        fireModified();
    }

    public synchronized Set<String> getEnabledFileTypes() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(workingEnabledFileTypes));
    }

    public synchronized boolean isCheckStringLiterals() {
        return workingCheckStringLiterals;
    }

    public synchronized void setCheckStringLiterals(boolean check) {
        this.workingCheckStringLiterals = check;
        fireModified();
    }

    public synchronized boolean isCheckComments() {
        return workingCheckComments;
    }

    public synchronized void setCheckComments(boolean check) {
        this.workingCheckComments = check;
        fireModified();
    }

    public synchronized boolean isCheckDocumentation() {
        return workingCheckDocumentation;
    }

    public synchronized void setCheckDocumentation(boolean check) {
        this.workingCheckDocumentation = check;
        fireModified();
    }

    public synchronized boolean isCheckCommitMessages() {
        return workingCheckCommitMessages;
    }

    public synchronized void setCheckCommitMessages(boolean check) {
        this.workingCheckCommitMessages = check;
        fireModified();
    }

    // =========================================================================
    // Spelling Settings
    // =========================================================================

    public synchronized List<String> getBundledDictionaries() {
        return Collections.unmodifiableList(DEFAULT_BUNDLED_DICTIONARIES);
    }

    public synchronized List<String> getCustomDictionaries() {
        return Collections.unmodifiableList(new ArrayList<>(workingCustomDictionaries));
    }

    public synchronized boolean addCustomDictionary(String path) {
        if (path != null && !path.trim().isEmpty() && !workingCustomDictionaries.contains(path.trim())) {
            workingCustomDictionaries.add(path.trim());
            fireModified();
            return true;
        }
        return false;
    }

    public synchronized boolean removeCustomDictionary(String path) {
        if (path != null && workingCustomDictionaries.contains(path)) {
            workingCustomDictionaries.remove(path);
            fireModified();
            return true;
        }
        return false;
    }

    public synchronized Set<String> getAcceptedWords() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(workingAcceptedWords));
    }

    public synchronized boolean addAcceptedWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            boolean added = workingAcceptedWords.add(word.trim());
            if (added) {
                fireModified();
            }
            return added;
        }
        return false;
    }

    public synchronized boolean removeAcceptedWord(String word) {
        if (word != null && workingAcceptedWords.remove(word)) {
            fireModified();
            return true;
        }
        return false;
    }

    public synchronized boolean isProcessCode() {
        return workingProcessCode;
    }

    public synchronized void setProcessCode(boolean processCode) {
        this.workingProcessCode = processCode;
        fireModified();
    }

    public synchronized boolean isProcessComments() {
        return workingProcessComments;
    }

    public synchronized void setProcessComments(boolean processComments) {
        this.workingProcessComments = processComments;
        fireModified();
    }

    public synchronized boolean isProcessLiterals() {
        return workingProcessLiterals;
    }

    public synchronized void setProcessLiterals(boolean processLiterals) {
        this.workingProcessLiterals = processLiterals;
        fireModified();
    }

    // =========================================================================
    // Single Dictionary & Custom Dictionaries
    // =========================================================================

    public synchronized boolean isUseSingleDictionary() {
        return workingUseSingleDictionary;
    }

    public synchronized void setUseSingleDictionary(boolean useSingleDictionary) {
        if (this.workingUseSingleDictionary != useSingleDictionary) {
            this.workingUseSingleDictionary = useSingleDictionary;
            fireModified();
        }
    }

    public synchronized String getSingleDictionaryTarget() {
        return workingSingleDictionaryTarget;
    }

    public synchronized void setSingleDictionaryTarget(String target) {
        if (target != null && !target.equals(this.workingSingleDictionaryTarget)) {
            this.workingSingleDictionaryTarget = target;
            fireModified();
        }
    }

    public synchronized List<SpellingDictionaryItem> getAllDictionaries() {
        List<SpellingDictionaryItem> list = new ArrayList<>();
        list.add(SpellingDictionaryItem.builtIn("Application-level dictionary"));
        list.add(SpellingDictionaryItem.builtIn("Project-level dictionary"));
        for (String custom : workingCustomDictionaries) {
            list.add(SpellingDictionaryItem.custom(custom));
        }
        for (SpellingDictionaryProvider provider : dictionaryProviders) {
            for (String dict : provider.getDictionaries()) {
                list.add(SpellingDictionaryItem.custom(dict));
            }
        }
        return Collections.unmodifiableList(list);
    }

    public synchronized void registerDictionaryProvider(SpellingDictionaryProvider provider) {
        if (provider != null && !dictionaryProviders.contains(provider)) {
            dictionaryProviders.add(provider);
            fireModified();
        }
    }

    // =========================================================================
    // Grammar Exceptions (Grammar & Style -> Exceptions tab)
    // =========================================================================

    public synchronized List<String> getGrammarExceptions() {
        return Collections.unmodifiableList(new ArrayList<>(workingGrammarExceptions));
    }

    public synchronized boolean addGrammarException(String exception) {
        if (exception != null && !exception.trim().isEmpty() && !workingGrammarExceptions.contains(exception.trim())) {
            workingGrammarExceptions.add(exception.trim());
            fireModified();
            return true;
        }
        return false;
    }

    public synchronized boolean removeGrammarException(String exception) {
        if (exception != null && workingGrammarExceptions.remove(exception)) {
            fireModified();
            return true;
        }
        return false;
    }
}
