package dev.lumina.build;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Singleton manager responsible for loading and saving Scala Compiler settings in Lumina IDE.
 */
public class ScalaCompilerSettingsManager {

    public static final String KEY_INCREMENTALITY_TYPE = "compiler.scala.incrementality.type";
    public static final String KEY_PROFILES_LIST = "compiler.scala.profiles";
    public static final String PREFIX_PROFILE = "compiler.scala.profile.";

    private static volatile ScalaCompilerSettingsManager instance;
    private ScalaCompilerSettings currentSettings;

    private ScalaCompilerSettingsManager() {
        loadSettings();
    }

    public static ScalaCompilerSettingsManager getInstance() {
        if (instance == null) {
            synchronized (ScalaCompilerSettingsManager.class) {
                if (instance == null) {
                    instance = new ScalaCompilerSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized ScalaCompilerSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(ScalaCompilerSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        ScalaCompilerSettings s = new ScalaCompilerSettings();

        String incType = Settings.get(KEY_INCREMENTALITY_TYPE);
        if (incType != null && !incType.isBlank()) s.setIncrementalityType(incType);

        String profilesList = Settings.get(KEY_PROFILES_LIST);
        if (profilesList != null && !profilesList.isBlank()) {
            List<ScalaCompilerProfile> loaded = new ArrayList<>();
            for (String profileName : profilesList.split(";;")) {
                if (profileName.isBlank()) continue;
                String prefix = PREFIX_PROFILE + profileName + ".";
                ScalaCompilerProfile p = new ScalaCompilerProfile(profileName);

                String mods = Settings.get(prefix + "modules");
                if (mods != null && !mods.isBlank()) {
                    p.setModules(new ArrayList<>(Arrays.asList(mods.split(","))));
                }

                String order = Settings.get(prefix + "compileOrder");
                if (order != null) p.setCompileOrder(order);

                // Features
                String dyn = Settings.get(prefix + "dynamics");
                if (dyn != null) p.setDynamics(Boolean.parseBoolean(dyn));
                String postfix = Settings.get(prefix + "postfix");
                if (postfix != null) p.setPostfixOperatorNotation(Boolean.parseBoolean(postfix));
                String refl = Settings.get(prefix + "reflective");
                if (refl != null) p.setReflectiveCalls(Boolean.parseBoolean(refl));
                String impl = Settings.get(prefix + "implicit");
                if (impl != null) p.setImplicitConversions(Boolean.parseBoolean(impl));
                String hk = Settings.get(prefix + "higherKinded");
                if (hk != null) p.setHigherKindedTypes(Boolean.parseBoolean(hk));
                String exist = Settings.get(prefix + "existential");
                if (exist != null) p.setExistentialTypes(Boolean.parseBoolean(exist));
                String macros = Settings.get(prefix + "macros");
                if (macros != null) p.setMacros(Boolean.parseBoolean(macros));
                String exp = Settings.get(prefix + "experimental");
                if (exp != null) p.setExperimentalFeatures(Boolean.parseBoolean(exp));

                // Options
                String warn = Settings.get(prefix + "warnings");
                if (warn != null) p.setEnableWarnings(Boolean.parseBoolean(warn));
                String dep = Settings.get(prefix + "deprecation");
                if (dep != null) p.setDeprecationWarnings(Boolean.parseBoolean(dep));
                String unch = Settings.get(prefix + "unchecked");
                if (unch != null) p.setUncheckedWarnings(Boolean.parseBoolean(unch));
                String feat = Settings.get(prefix + "featureWarn");
                if (feat != null) p.setFeatureWarnings(Boolean.parseBoolean(feat));
                String opt = Settings.get(prefix + "optimise");
                if (opt != null) p.setOptimiseBytecode(Boolean.parseBoolean(opt));
                String explain = Settings.get(prefix + "explainErrors");
                if (explain != null) p.setExplainTypeErrors(Boolean.parseBoolean(explain));
                String spec = Settings.get(prefix + "specialization");
                if (spec != null) p.setEnableSpecialization(Boolean.parseBoolean(spec));
                String cont = Settings.get(prefix + "continuations");
                if (cont != null) p.setEnableContinuations(Boolean.parseBoolean(cont));

                String debug = Settings.get(prefix + "debugLevel");
                if (debug != null) p.setDebuggingInfoLevel(debug);

                String addOpts = Settings.get(prefix + "additionalOptions");
                if (addOpts != null) p.setAdditionalCompilerOptions(addOpts);

                String plugins = Settings.get(prefix + "plugins");
                if (plugins != null && !plugins.isBlank()) {
                    p.setCompilerPlugins(new ArrayList<>(Arrays.asList(plugins.split(";;"))));
                }

                loaded.add(p);
            }
            if (!loaded.isEmpty()) {
                s.setProfiles(loaded);
            }
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_INCREMENTALITY_TYPE, currentSettings.getIncrementalityType());

        StringBuilder sb = new StringBuilder();
        for (ScalaCompilerProfile p : currentSettings.getProfiles()) {
            if (!sb.isEmpty()) sb.append(";;");
            sb.append(p.getName());

            String prefix = PREFIX_PROFILE + p.getName() + ".";
            Settings.set(prefix + "modules", String.join(",", p.getModules()));
            Settings.set(prefix + "compileOrder", p.getCompileOrder());

            Settings.set(prefix + "dynamics", String.valueOf(p.isDynamics()));
            Settings.set(prefix + "postfix", String.valueOf(p.isPostfixOperatorNotation()));
            Settings.set(prefix + "reflective", String.valueOf(p.isReflectiveCalls()));
            Settings.set(prefix + "implicit", String.valueOf(p.isImplicitConversions()));
            Settings.set(prefix + "higherKinded", String.valueOf(p.isHigherKindedTypes()));
            Settings.set(prefix + "existential", String.valueOf(p.isExistentialTypes()));
            Settings.set(prefix + "macros", String.valueOf(p.isMacros()));
            Settings.set(prefix + "experimental", String.valueOf(p.isExperimentalFeatures()));

            Settings.set(prefix + "warnings", String.valueOf(p.isEnableWarnings()));
            Settings.set(prefix + "deprecation", String.valueOf(p.isDeprecationWarnings()));
            Settings.set(prefix + "unchecked", String.valueOf(p.isUncheckedWarnings()));
            Settings.set(prefix + "featureWarn", String.valueOf(p.isFeatureWarnings()));
            Settings.set(prefix + "optimise", String.valueOf(p.isOptimiseBytecode()));
            Settings.set(prefix + "explainErrors", String.valueOf(p.isExplainTypeErrors()));
            Settings.set(prefix + "specialization", String.valueOf(p.isEnableSpecialization()));
            Settings.set(prefix + "continuations", String.valueOf(p.isEnableContinuations()));

            Settings.set(prefix + "debugLevel", p.getDebuggingInfoLevel());
            Settings.set(prefix + "additionalOptions", p.getAdditionalCompilerOptions());
            Settings.set(prefix + "plugins", String.join(";;", p.getCompilerPlugins()));
        }
        Settings.set(KEY_PROFILES_LIST, sb.toString());
    }
}
