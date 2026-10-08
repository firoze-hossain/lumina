package dev.lumina.textmate;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central manager and dynamic registry for TextMate Bundles in Lumina IDE.
 * Aggregates bundles from SPI providers and manages custom user-registered bundles.
 * Supports full dirty tracking, lifecycle management, and built-in protection.
 */
public class TextMateBundlesManager {

    private static final TextMateBundlesManager INSTANCE = new TextMateBundlesManager();

    public static TextMateBundlesManager getInstance() {
        return INSTANCE;
    }

    private final List<TextMateBundleProvider> providers = new CopyOnWriteArrayList<>();

    private final List<TextMateBundle> committedBundles = new ArrayList<>();
    private final List<TextMateBundle> workingBundles = new ArrayList<>();

    private Runnable onModifiedListener;

    private TextMateBundlesManager() {
        // Register default provider with 62 built-in bundles
        registerProvider(new DefaultTextMateBundleProvider());
        reloadFromProviders();
        commit();
    }

    public synchronized void registerProvider(TextMateBundleProvider provider) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
            reloadFromProviders();
        }
    }

    public synchronized void unregisterProvider(TextMateBundleProvider provider) {
        if (provider != null) {
            providers.remove(provider);
            reloadFromProviders();
        }
    }

    private synchronized void reloadFromProviders() {
        Map<String, TextMateBundle> existingCustom = new LinkedHashMap<>();
        Map<String, Boolean> existingStates = new HashMap<>();

        for (TextMateBundle b : workingBundles) {
            if (!b.isBuiltIn()) {
                existingCustom.put(b.getName().toLowerCase(Locale.ROOT), b);
            }
            existingStates.put(b.getName().toLowerCase(Locale.ROOT), b.isEnabled());
        }

        workingBundles.clear();

        // 1. Load bundles from all registered providers
        for (TextMateBundleProvider p : providers) {
            List<TextMateBundle> providerBundles = p.getBundles();
            if (providerBundles != null) {
                for (TextMateBundle b : providerBundles) {
                    TextMateBundle copy = new TextMateBundle(b);
                    Boolean prev = existingStates.get(copy.getName().toLowerCase(Locale.ROOT));
                    if (prev != null) {
                        copy.setEnabled(prev);
                    }
                    workingBundles.add(copy);
                }
            }
        }

        // 2. Re-add custom bundles
        workingBundles.addAll(existingCustom.values());

        sortBundles(workingBundles);
    }

    private void sortBundles(List<TextMateBundle> list) {
        list.sort(Comparator.comparing(b -> b.getName().toLowerCase(Locale.ROOT)));
    }

    public synchronized List<TextMateBundle> getWorkingBundles() {
        return Collections.unmodifiableList(workingBundles);
    }

    public synchronized List<TextMateBundle> getCommittedBundles() {
        return Collections.unmodifiableList(committedBundles);
    }

    public synchronized Optional<TextMateBundle> findBundle(String name) {
        if (name == null) return Optional.empty();
        return workingBundles.stream()
                .filter(b -> b.getName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    public synchronized boolean addCustomBundle(String name, String path) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String trimmedName = name.trim();
        boolean exists = workingBundles.stream()
                .anyMatch(b -> b.getName().equalsIgnoreCase(trimmedName));
        if (exists) {
            return false;
        }

        TextMateBundle bundle = TextMateBundle.custom(trimmedName, path != null ? path.trim() : "");
        workingBundles.add(bundle);
        sortBundles(workingBundles);
        notifyModified();
        return true;
    }

    public synchronized boolean removeBundle(String name) {
        if (name == null) return false;
        Optional<TextMateBundle> match = findBundle(name);
        if (match.isPresent()) {
            TextMateBundle bundle = match.get();
            // Built-in bundles cannot be removed!
            if (bundle.isBuiltIn()) {
                return false;
            }
            workingBundles.remove(bundle);
            notifyModified();
            return true;
        }
        return false;
    }

    public synchronized boolean setBundleEnabled(String name, boolean enabled) {
        if (name == null) return false;
        Optional<TextMateBundle> match = findBundle(name);
        if (match.isPresent()) {
            TextMateBundle bundle = match.get();
            if (bundle.isEnabled() != enabled) {
                bundle.setEnabled(enabled);
                notifyModified();
            }
            return true;
        }
        return false;
    }

    public synchronized boolean isModified() {
        if (workingBundles.size() != committedBundles.size()) {
            return true;
        }
        for (int i = 0; i < workingBundles.size(); i++) {
            if (!workingBundles.get(i).equals(committedBundles.get(i))) {
                return true;
            }
        }
        return false;
    }

    public synchronized void apply() {
        commit();
        notifyModified();
    }

    public synchronized void reset() {
        workingBundles.clear();
        for (TextMateBundle b : committedBundles) {
            workingBundles.add(new TextMateBundle(b));
        }
        notifyModified();
    }

    private void commit() {
        committedBundles.clear();
        for (TextMateBundle b : workingBundles) {
            committedBundles.add(new TextMateBundle(b));
        }
    }

    public synchronized void resetToFactoryDefaults() {
        workingBundles.clear();
        reloadFromProviders();
        for (TextMateBundle b : workingBundles) {
            b.setEnabled(true);
        }
        commit();
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
