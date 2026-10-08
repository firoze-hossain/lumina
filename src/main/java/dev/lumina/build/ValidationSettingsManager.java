package dev.lumina.build;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton manager responsible for loading and saving Validation compiler settings in Lumina IDE.
 */
public class ValidationSettingsManager {

    public static final String KEY_VALIDATE_ON_BUILD = "compiler.validation.validate.on.build";
    public static final String KEY_FREEMARKER = "compiler.validation.freemarker";
    public static final String KEY_HIBERNATE = "compiler.validation.hibernate";
    public static final String KEY_JPA = "compiler.validation.jpa";
    public static final String KEY_JASPER = "compiler.validation.jasper";
    public static final String KEY_SPRING_MODEL = "compiler.validation.spring.model";
    public static final String KEY_WEB_XML = "compiler.validation.web.xml";
    public static final String KEY_EXCLUDES = "compiler.validation.excludes";

    private static volatile ValidationSettingsManager instance;
    private ValidationSettings currentSettings;

    private ValidationSettingsManager() {
        loadSettings();
    }

    public static ValidationSettingsManager getInstance() {
        if (instance == null) {
            synchronized (ValidationSettingsManager.class) {
                if (instance == null) {
                    instance = new ValidationSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized ValidationSettings getSettings() {
        if (currentSettings == null) {
            loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(ValidationSettings newSettings) {
        if (newSettings == null) return;
        this.currentSettings = newSettings.clone();
        saveSettings();
    }

    public synchronized void loadSettings() {
        ValidationSettings s = new ValidationSettings();

        String valOnBuild = Settings.get(KEY_VALIDATE_ON_BUILD);
        if (valOnBuild != null) s.setValidateOnBuild(Boolean.parseBoolean(valOnBuild));

        String fm = Settings.get(KEY_FREEMARKER);
        if (fm != null) s.setFreeMarker(Boolean.parseBoolean(fm));

        String hib = Settings.get(KEY_HIBERNATE);
        if (hib != null) s.setHibernate(Boolean.parseBoolean(hib));

        String jpa = Settings.get(KEY_JPA);
        if (jpa != null) s.setJpa(Boolean.parseBoolean(jpa));

        String jasper = Settings.get(KEY_JASPER);
        if (jasper != null) s.setJasper(Boolean.parseBoolean(jasper));

        String spring = Settings.get(KEY_SPRING_MODEL);
        if (spring != null) s.setSpringModel(Boolean.parseBoolean(spring));

        String web = Settings.get(KEY_WEB_XML);
        if (web != null) s.setWebXml(Boolean.parseBoolean(web));

        String exc = Settings.get(KEY_EXCLUDES);
        if (exc != null && !exc.isBlank()) {
            List<CompilerExcludeEntry> list = new ArrayList<>();
            for (String item : exc.split(";;")) {
                String[] parts = item.split("::", 2);
                if (parts.length == 2) {
                    list.add(new CompilerExcludeEntry(parts[0], Boolean.parseBoolean(parts[1])));
                }
            }
            s.setExcludes(list);
        }

        this.currentSettings = s;
    }

    public synchronized void saveSettings() {
        if (currentSettings == null) return;
        Settings.set(KEY_VALIDATE_ON_BUILD, String.valueOf(currentSettings.isValidateOnBuild()));
        Settings.set(KEY_FREEMARKER, String.valueOf(currentSettings.isFreeMarker()));
        Settings.set(KEY_HIBERNATE, String.valueOf(currentSettings.isHibernate()));
        Settings.set(KEY_JPA, String.valueOf(currentSettings.isJpa()));
        Settings.set(KEY_JASPER, String.valueOf(currentSettings.isJasper()));
        Settings.set(KEY_SPRING_MODEL, String.valueOf(currentSettings.isSpringModel()));
        Settings.set(KEY_WEB_XML, String.valueOf(currentSettings.isWebXml()));

        StringBuilder sb = new StringBuilder();
        for (CompilerExcludeEntry e : currentSettings.getExcludes()) {
            if (!sb.isEmpty()) sb.append(";;");
            sb.append(e.getPath()).append("::").append(e.isRecursive());
        }
        Settings.set(KEY_EXCLUDES, sb.toString());
    }
}
