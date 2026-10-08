package dev.lumina.readermode;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central manager and dynamic registry for Reader Mode configuration in Lumina IDE.
 * Manages Reader Mode settings, SPI-contributed options, schemes, dirty tracking, and lifecycle.
 */
public class ReaderModeManager {

    private static final ReaderModeManager INSTANCE = new ReaderModeManager();

    public static ReaderModeManager getInstance() {
        return INSTANCE;
    }

    private final List<ReaderModeOptionProvider> optionProviders = new CopyOnWriteArrayList<>();
    private final List<ReaderModeSchemeProvider> schemeProviders = new CopyOnWriteArrayList<>();

    private ReaderModeSettings committedSettings = new ReaderModeSettings();
    private ReaderModeSettings workingSettings = new ReaderModeSettings();

    private Runnable onModifiedListener;

    private ReaderModeManager() {
        // Register default scheme provider
        schemeProviders.add(new ReaderModeSchemeProvider() {
            @Override
            public String getActiveSchemeName() {
                return "Default";
            }

            @Override
            public List<String> getAvailableSchemes() {
                return List.of("Default IDE", "Project");
            }
        });
    }

    public synchronized void registerOptionProvider(ReaderModeOptionProvider provider) {
        if (provider != null && !optionProviders.contains(provider)) {
            optionProviders.add(provider);
            // Initialize defaults for newly added provider options
            for (ReaderModeOption opt : provider.getOptions()) {
                if (!workingSettings.getDynamicOptions().containsKey(opt.id())) {
                    workingSettings.setDynamicOption(opt.id(), opt.defaultValue());
                }
                if (!committedSettings.getDynamicOptions().containsKey(opt.id())) {
                    committedSettings.setDynamicOption(opt.id(), opt.defaultValue());
                }
            }
        }
    }

    public synchronized void unregisterOptionProvider(ReaderModeOptionProvider provider) {
        if (provider != null) {
            optionProviders.remove(provider);
        }
    }

    public synchronized void registerSchemeProvider(ReaderModeSchemeProvider provider) {
        if (provider != null && !schemeProviders.contains(provider)) {
            schemeProviders.add(0, provider);
        }
    }

    public synchronized void unregisterSchemeProvider(ReaderModeSchemeProvider provider) {
        if (provider != null) {
            schemeProviders.remove(provider);
        }
    }

    public List<ReaderModeOption> getAllContributedOptions() {
        List<ReaderModeOption> options = new ArrayList<>();
        for (ReaderModeOptionProvider provider : optionProviders) {
            List<ReaderModeOption> providerOptions = provider.getOptions();
            if (providerOptions != null) {
                options.addAll(providerOptions);
            }
        }
        return Collections.unmodifiableList(options);
    }

    public String getActiveSchemeName() {
        for (ReaderModeSchemeProvider provider : schemeProviders) {
            String active = provider.getActiveSchemeName();
            if (active != null && !active.isBlank()) {
                return active;
            }
        }
        return "Default";
    }

    public List<String> getAvailableSchemes() {
        Set<String> schemes = new LinkedHashSet<>();
        for (ReaderModeSchemeProvider provider : schemeProviders) {
            List<String> list = provider.getAvailableSchemes();
            if (list != null) {
                schemes.addAll(list);
            }
        }
        if (schemes.isEmpty()) {
            schemes.add("Default IDE");
        }
        return new ArrayList<>(schemes);
    }

    public ReaderModeSettings getWorkingSettings() {
        return workingSettings;
    }

    public ReaderModeSettings getCommittedSettings() {
        return committedSettings;
    }

    public boolean isModified() {
        return !workingSettings.equals(committedSettings);
    }

    public synchronized void apply() {
        committedSettings = new ReaderModeSettings(workingSettings);
        notifyModified();
    }

    public synchronized void reset() {
        workingSettings = new ReaderModeSettings(committedSettings);
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    /**
     * Resets working and committed settings back to factory defaults.
     */
    public synchronized void resetToFactoryDefaults() {
        committedSettings = new ReaderModeSettings();
        workingSettings = new ReaderModeSettings();
        notifyModified();
    }
}
