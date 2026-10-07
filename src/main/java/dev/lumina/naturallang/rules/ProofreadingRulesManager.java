package dev.lumina.naturallang.rules;

import java.util.*;

/**
 * Central registry and state manager for dynamic Proofreading Rules, Domains,
 * Writing Styles, and Language-specific configurations.
 * Allows extensions to contribute rules dynamically via ProofreadingRuleProvider.
 */
public class ProofreadingRulesManager {

    private static final ProofreadingRulesManager INSTANCE = new ProofreadingRulesManager();

    public static ProofreadingRulesManager getInstance() {
        return INSTANCE;
    }

    public static final List<String> DOMAINS = List.of(
            "Other",
            "Commit messages",
            "Documentation",
            "Comments",
            "Code identifiers"
    );

    public static final List<String> WRITING_STYLES = List.of(
            "Casual (messengers, forums)",
            "Default",
            "Public (blog posts, documentation)",
            "Formal (official communication)",
            "Academic"
    );

    public static final List<String> LANGUAGES = List.of(
            "English",
            "Deutsch",
            "Español",
            "Français",
            "Italiano",
            "Nederlands",
            "Polski",
            "Português",
            "Русский"
    );

    private final List<ProofreadingRuleProvider> providers = new ArrayList<>();
    private final Map<String, ProofreadingRule> rulesMap = new LinkedHashMap<>();

    // Tree rules (Other rules tree from images 3, 4, 5)
    private final List<ProofreadingTreeRulesProvider> treeProviders = new ArrayList<>();
    private final Map<String, ProofreadingTreeCategory> treeCategoriesMap = new LinkedHashMap<>();
    private final Map<String, ProofreadingTreeRule> treeRulesMap = new LinkedHashMap<>();
    private final Map<String, Boolean> committedTreeRuleEnabled = new HashMap<>();
    private final Map<String, Boolean> workingTreeRuleEnabled = new HashMap<>();

    // Committed state
    private String committedDomain = "Other";
    private String committedWritingStyle = "Default";
    private String committedLanguage = "English";
    private final Map<String, Boolean> committedRuleEnabled = new HashMap<>();
    private final Map<String, String> committedRuleOptions = new HashMap<>();

    // Working state
    private String workingDomain = "Other";
    private String workingWritingStyle = "Default";
    private String workingLanguage = "English";
    private final Map<String, Boolean> workingRuleEnabled = new HashMap<>();
    private final Map<String, String> workingRuleOptions = new HashMap<>();

    private final List<Runnable> onModifiedListeners = new ArrayList<>();

    private ProofreadingRulesManager() {
        registerProvider(new BuiltInProofreadingRuleProvider());
        registerTreeProvider(new BuiltInProofreadingTreeRulesProvider());
        resetToDefaults();
    }

    public synchronized void registerProvider(ProofreadingRuleProvider provider) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
            for (ProofreadingRule rule : provider.getRules()) {
                rulesMap.put(rule.getId(), rule);
                if (!committedRuleEnabled.containsKey(rule.getId())) {
                    committedRuleEnabled.put(rule.getId(), rule.isDefaultEnabled());
                    workingRuleEnabled.put(rule.getId(), rule.isDefaultEnabled());
                }
                if (rule.hasOption() && !committedRuleOptions.containsKey(rule.getId())) {
                    committedRuleOptions.put(rule.getId(), rule.getOption().defaultChoice());
                    workingRuleOptions.put(rule.getId(), rule.getOption().defaultChoice());
                }
            }
        }
    }

    public synchronized void registerTreeProvider(ProofreadingTreeRulesProvider provider) {
        if (provider != null && !treeProviders.contains(provider)) {
            treeProviders.add(provider);
            for (ProofreadingTreeCategory cat : provider.getTreeCategories()) {
                treeCategoriesMap.put(cat.getName(), cat);
                for (ProofreadingTreeRule rule : cat.getRules()) {
                    treeRulesMap.put(rule.getId(), rule);
                    if (!committedTreeRuleEnabled.containsKey(rule.getId())) {
                        committedTreeRuleEnabled.put(rule.getId(), rule.isDefaultEnabled());
                        workingTreeRuleEnabled.put(rule.getId(), rule.isDefaultEnabled());
                    }
                }
            }
        }
    }

    public synchronized void resetToDefaults() {
        committedDomain = "Other";
        committedWritingStyle = "Default";
        committedLanguage = "English";

        committedRuleEnabled.clear();
        committedRuleOptions.clear();

        for (ProofreadingRule rule : rulesMap.values()) {
            committedRuleEnabled.put(rule.getId(), rule.isDefaultEnabled());
            if (rule.hasOption()) {
                committedRuleOptions.put(rule.getId(), rule.getOption().defaultChoice());
            }
        }

        committedTreeRuleEnabled.clear();
        for (ProofreadingTreeRule rule : treeRulesMap.values()) {
            committedTreeRuleEnabled.put(rule.getId(), rule.isDefaultEnabled());
        }

        reset();
    }

    public synchronized void reset() {
        workingDomain = committedDomain;
        workingWritingStyle = committedWritingStyle;
        workingLanguage = committedLanguage;

        workingRuleEnabled.clear();
        workingRuleEnabled.putAll(committedRuleEnabled);

        workingRuleOptions.clear();
        workingRuleOptions.putAll(committedRuleOptions);

        workingTreeRuleEnabled.clear();
        workingTreeRuleEnabled.putAll(committedTreeRuleEnabled);

        fireModified();
    }

    public synchronized void apply() {
        committedDomain = workingDomain;
        committedWritingStyle = workingWritingStyle;
        committedLanguage = workingLanguage;

        committedRuleEnabled.clear();
        committedRuleEnabled.putAll(workingRuleEnabled);

        committedRuleOptions.clear();
        committedRuleOptions.putAll(workingRuleOptions);

        committedTreeRuleEnabled.clear();
        committedTreeRuleEnabled.putAll(workingTreeRuleEnabled);

        fireModified();
    }

    public synchronized boolean isModified() {
        if (!Objects.equals(workingDomain, committedDomain)) return true;
        if (!Objects.equals(workingWritingStyle, committedWritingStyle)) return true;
        if (!Objects.equals(workingLanguage, committedLanguage)) return true;
        if (!workingRuleEnabled.equals(committedRuleEnabled)) return true;
        if (!workingRuleOptions.equals(committedRuleOptions)) return true;
        if (!workingTreeRuleEnabled.equals(committedTreeRuleEnabled)) return true;
        return false;
    }

    public synchronized void addOnModifiedListener(Runnable listener) {
        if (listener != null && !onModifiedListeners.contains(listener)) {
            onModifiedListeners.add(listener);
        }
    }

    public synchronized void setOnModifiedListener(Runnable listener) {
        if (listener != null) {
            addOnModifiedListener(listener);
        }
    }

    private synchronized void fireModified() {
        for (Runnable r : onModifiedListeners) {
            r.run();
        }
    }

    // =========================================================================
    // Rules Access & Filtering
    // =========================================================================

    public synchronized List<ProofreadingRule> getAllRules() {
        return Collections.unmodifiableList(new ArrayList<>(rulesMap.values()));
    }

    public synchronized List<ProofreadingRule> getRulesByCategory(ProofreadingRuleCategory category) {
        List<ProofreadingRule> list = new ArrayList<>();
        for (ProofreadingRule r : rulesMap.values()) {
            if (r.getCategory() == category) {
                list.add(r);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public synchronized List<ProofreadingRule> getFilteredRules(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllRules();
        }
        String q = query.trim().toLowerCase();
        List<ProofreadingRule> filtered = new ArrayList<>();
        for (ProofreadingRule r : rulesMap.values()) {
            boolean match = r.getTitle().toLowerCase().contains(q)
                    || r.getDescription().toLowerCase().contains(q)
                    || r.getCategory().getDisplayName().toLowerCase().contains(q)
                    || r.getId().toLowerCase().contains(q);
            if (!match && r.hasOption()) {
                for (String choice : r.getOption().choices()) {
                    if (choice.toLowerCase().contains(q)) {
                        match = true;
                        break;
                    }
                }
            }
            if (match) {
                filtered.add(r);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    public synchronized boolean isRuleEnabled(String ruleId) {
        return workingRuleEnabled.getOrDefault(ruleId, false);
    }

    public synchronized void setRuleEnabled(String ruleId, boolean enabled) {
        workingRuleEnabled.put(ruleId, enabled);
        fireModified();
    }

    public synchronized String getRuleOptionValue(String ruleId) {
        ProofreadingRule r = rulesMap.get(ruleId);
        if (r != null && r.hasOption()) {
            return workingRuleOptions.getOrDefault(ruleId, r.getOption().defaultChoice());
        }
        return null;
    }

    public synchronized void setRuleOptionValue(String ruleId, String choice) {
        workingRuleOptions.put(ruleId, choice);
        fireModified();
    }

    // =========================================================================
    // Dropdown Contexts
    // =========================================================================

    public synchronized String getDomain() {
        return workingDomain;
    }

    public synchronized void setDomain(String domain) {
        if (domain != null && !domain.equals(workingDomain)) {
            this.workingDomain = domain;
            fireModified();
        }
    }

    public synchronized String getWritingStyle() {
        return workingWritingStyle;
    }

    public synchronized void setWritingStyle(String style) {
        if (style != null && !style.equals(workingWritingStyle)) {
            this.workingWritingStyle = style;
            fireModified();
        }
    }

    public synchronized String getLanguage() {
        return workingLanguage;
    }

    public synchronized void setLanguage(String lang) {
        if (lang != null && !lang.equals(workingLanguage)) {
            this.workingLanguage = lang;
            fireModified();
        }
    }

    // =========================================================================
    // Tree Rules Access & Filtering ("Other rules" tree)
    // =========================================================================

    public synchronized List<ProofreadingTreeCategory> getTreeCategories() {
        return Collections.unmodifiableList(new ArrayList<>(treeCategoriesMap.values()));
    }

    public synchronized List<ProofreadingTreeRule> getAllTreeRules() {
        return Collections.unmodifiableList(new ArrayList<>(treeRulesMap.values()));
    }

    public synchronized ProofreadingTreeRule getTreeRule(String ruleId) {
        return treeRulesMap.get(ruleId);
    }

    public synchronized List<ProofreadingTreeCategory> getFilteredTreeCategories(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getTreeCategories();
        }
        String q = query.trim().toLowerCase();
        List<ProofreadingTreeCategory> filtered = new ArrayList<>();
        for (ProofreadingTreeCategory cat : treeCategoriesMap.values()) {
            boolean catMatches = cat.getName().toLowerCase().contains(q);
            List<ProofreadingTreeRule> matchingRules = new ArrayList<>();
            for (ProofreadingTreeRule rule : cat.getRules()) {
                if (catMatches || rule.getName().toLowerCase().contains(q)
                        || rule.getDescription().toLowerCase().contains(q)
                        || rule.getId().toLowerCase().contains(q)) {
                    matchingRules.add(rule);
                }
            }
            if (!matchingRules.isEmpty()) {
                filtered.add(new ProofreadingTreeCategory(cat.getName(), matchingRules));
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    public synchronized boolean isTreeRuleEnabled(String ruleId) {
        return workingTreeRuleEnabled.getOrDefault(ruleId, false);
    }

    public synchronized void setTreeRuleEnabled(String ruleId, boolean enabled) {
        workingTreeRuleEnabled.put(ruleId, enabled);
        fireModified();
    }

    public synchronized boolean isCategoryFullyEnabled(String catName) {
        ProofreadingTreeCategory cat = treeCategoriesMap.get(catName);
        if (cat == null || cat.getRules().isEmpty()) return false;
        for (ProofreadingTreeRule r : cat.getRules()) {
            if (!isTreeRuleEnabled(r.getId())) return false;
        }
        return true;
    }

    public synchronized boolean isCategoryPartiallyEnabled(String catName) {
        ProofreadingTreeCategory cat = treeCategoriesMap.get(catName);
        if (cat == null || cat.getRules().isEmpty()) return false;
        boolean hasTrue = false;
        boolean hasFalse = false;
        for (ProofreadingTreeRule r : cat.getRules()) {
            if (isTreeRuleEnabled(r.getId())) {
                hasTrue = true;
            } else {
                hasFalse = true;
            }
            if (hasTrue && hasFalse) return true;
        }
        return false;
    }

    public synchronized void setCategoryEnabled(String catName, boolean enabled) {
        ProofreadingTreeCategory cat = treeCategoriesMap.get(catName);
        if (cat != null) {
            for (ProofreadingTreeRule r : cat.getRules()) {
                workingTreeRuleEnabled.put(r.getId(), enabled);
            }
            fireModified();
        }
    }
}
