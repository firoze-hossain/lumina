package dev.lumina.schemas;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager for Languages & Frameworks > Schemas and DTDs in Lumina IDE.
 * Manages:
 *  - External & Ignored Schemas/DTDs
 *  - Default XML Schemas (HTML level, XML schema version)
 *  - JSON Schema Mappings (Master-Detail dynamic configurations)
 */
public class SchemasAndDtdsSettingsManager {

    // Persistence Keys
    public static final String KEY_SCHEMAS_EXTERNAL_RESOURCES = "schemas.dtds.external.resources";
    public static final String KEY_SCHEMAS_IGNORED_SCHEMAS = "schemas.dtds.ignored.schemas";

    public static final String KEY_SCHEMAS_HTML_LEVEL = "schemas.xml.html.level";
    public static final String KEY_SCHEMAS_OTHER_DOCTYPE = "schemas.xml.other.doctype";
    public static final String KEY_SCHEMAS_XML_VERSION = "schemas.xml.schema.version";

    public static final String KEY_SCHEMAS_JSON_MAPPINGS = "schemas.json.mappings";

    private static SchemasAndDtdsSettingsManager instance;

    private SchemasAndDtdsSettings schemasAndDtdsSettings = new SchemasAndDtdsSettings();
    private DefaultXmlSchemasSettings defaultXmlSchemasSettings = new DefaultXmlSchemasSettings();
    private JsonSchemaMappingsSettings jsonSchemaMappingsSettings = new JsonSchemaMappingsSettings();

    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private SchemasAndDtdsSettingsManager() {
        loadSettings();
    }

    public static synchronized SchemasAndDtdsSettingsManager getInstance() {
        if (instance == null) {
            instance = new SchemasAndDtdsSettingsManager();
        }
        return instance;
    }

    // Schemas & DTDs overview
    public synchronized SchemasAndDtdsSettings getSchemasAndDtdsSettings() {
        return schemasAndDtdsSettings.copy();
    }

    public synchronized void setSchemasAndDtdsSettings(SchemasAndDtdsSettings settings) {
        this.schemasAndDtdsSettings = settings != null ? settings.copy() : new SchemasAndDtdsSettings();
        saveSettings();
        notifyListeners();
    }

    // Default XML Schemas
    public synchronized DefaultXmlSchemasSettings getDefaultXmlSchemasSettings() {
        return defaultXmlSchemasSettings.copy();
    }

    public synchronized void setDefaultXmlSchemasSettings(DefaultXmlSchemasSettings settings) {
        this.defaultXmlSchemasSettings = settings != null ? settings.copy() : new DefaultXmlSchemasSettings();
        saveSettings();
        notifyListeners();
    }

    // JSON Schema Mappings
    public synchronized JsonSchemaMappingsSettings getJsonSchemaMappingsSettings() {
        return jsonSchemaMappingsSettings.copy();
    }

    public synchronized void setJsonSchemaMappingsSettings(JsonSchemaMappingsSettings settings) {
        this.jsonSchemaMappingsSettings = settings != null ? settings.copy() : new JsonSchemaMappingsSettings();
        saveSettings();
        notifyListeners();
    }

    public synchronized void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public synchronized void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }

    public synchronized void saveSettings() {
        // 1. External Resources
        StringBuilder extBuilder = new StringBuilder();
        for (ExternalResourceEntry res : schemasAndDtdsSettings.getExternalResources()) {
            if (extBuilder.length() > 0) extBuilder.append("###");
            extBuilder.append(res.getUri()).append("@@@").append(res.getLocation());
        }
        Settings.put(KEY_SCHEMAS_EXTERNAL_RESOURCES, extBuilder.toString());

        // 2. Ignored Schemas
        StringBuilder ignBuilder = new StringBuilder();
        for (String ign : schemasAndDtdsSettings.getIgnoredSchemas()) {
            if (ignBuilder.length() > 0) ignBuilder.append("###");
            ignBuilder.append(ign);
        }
        Settings.put(KEY_SCHEMAS_IGNORED_SCHEMAS, ignBuilder.toString());

        // 3. Default XML Schemas
        Settings.put(KEY_SCHEMAS_HTML_LEVEL, defaultXmlSchemasSettings.getHtmlLanguageLevel());
        Settings.put(KEY_SCHEMAS_OTHER_DOCTYPE, defaultXmlSchemasSettings.getOtherDoctype());
        Settings.put(KEY_SCHEMAS_XML_VERSION, defaultXmlSchemasSettings.getXmlSchemaVersion());

        // 4. JSON Schema Mappings
        StringBuilder jsonBuilder = new StringBuilder();
        for (JsonSchemaMapping mapping : jsonSchemaMappingsSettings.getMappings()) {
            if (jsonBuilder.length() > 0) jsonBuilder.append("###");
            StringBuilder patBuilder = new StringBuilder();
            for (JsonSchemaPattern p : mapping.getPatterns()) {
                if (patBuilder.length() > 0) patBuilder.append(";;;");
                patBuilder.append(p.getPattern()).append(":::").append(p.getType().name());
            }
            jsonBuilder.append(mapping.getName()).append("@@@")
                    .append(mapping.getSchemaFileOrUrl()).append("@@@")
                    .append(mapping.getSchemaVersion()).append("@@@")
                    .append(patBuilder);
        }
        Settings.put(KEY_SCHEMAS_JSON_MAPPINGS, jsonBuilder.toString());
    }

    public synchronized void loadSettings() {
        // 1. Schemas & DTDs overview
        SchemasAndDtdsSettings sds = new SchemasAndDtdsSettings();
        String ext = Settings.get(KEY_SCHEMAS_EXTERNAL_RESOURCES);
        if (ext != null && !ext.trim().isEmpty()) {
            List<ExternalResourceEntry> list = new ArrayList<>();
            String[] parts = ext.split("###");
            for (String part : parts) {
                String[] pair = part.split("@@@", 2);
                if (pair.length == 2) {
                    list.add(new ExternalResourceEntry(pair[0], pair[1]));
                }
            }
            sds.setExternalResources(list);
        }

        String ign = Settings.get(KEY_SCHEMAS_IGNORED_SCHEMAS);
        if (ign != null && !ign.trim().isEmpty()) {
            List<String> list = new ArrayList<>();
            String[] parts = ign.split("###");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    list.add(part);
                }
            }
            sds.setIgnoredSchemas(list);
        }
        this.schemasAndDtdsSettings = sds;

        // 2. Default XML Schemas
        DefaultXmlSchemasSettings dxs = new DefaultXmlSchemasSettings();
        String htmlLvl = Settings.get(KEY_SCHEMAS_HTML_LEVEL);
        if (htmlLvl != null) dxs.setHtmlLanguageLevel(htmlLvl);
        String otherDoc = Settings.get(KEY_SCHEMAS_OTHER_DOCTYPE);
        if (otherDoc != null) dxs.setOtherDoctype(otherDoc);
        String xmlVer = Settings.get(KEY_SCHEMAS_XML_VERSION);
        if (xmlVer != null) dxs.setXmlSchemaVersion(xmlVer);
        this.defaultXmlSchemasSettings = dxs;

        // 3. JSON Schema Mappings
        JsonSchemaMappingsSettings jms = new JsonSchemaMappingsSettings();
        String jsonMap = Settings.get(KEY_SCHEMAS_JSON_MAPPINGS);
        if (jsonMap != null && !jsonMap.trim().isEmpty()) {
            List<JsonSchemaMapping> list = new ArrayList<>();
            String[] entries = jsonMap.split("###");
            for (String entry : entries) {
                String[] fields = entry.split("@@@", 4);
                if (fields.length >= 3) {
                    List<JsonSchemaPattern> patterns = new ArrayList<>();
                    if (fields.length == 4 && !fields[3].isEmpty()) {
                        String[] patStrs = fields[3].split(";;;");
                        for (String patStr : patStrs) {
                            String[] pPair = patStr.split(":::", 2);
                            if (pPair.length == 2) {
                                try {
                                    patterns.add(new JsonSchemaPattern(pPair[0], JsonSchemaPattern.PatternType.valueOf(pPair[1])));
                                } catch (IllegalArgumentException e) {
                                    patterns.add(new JsonSchemaPattern(pPair[0], JsonSchemaPattern.PatternType.PATTERN));
                                }
                            }
                        }
                    }
                    list.add(new JsonSchemaMapping(fields[0], fields[1], fields[2], patterns));
                }
            }
            if (!list.isEmpty()) {
                jms.setMappings(list);
            }
        }
        this.jsonSchemaMappingsSettings = jms;
    }

    public synchronized void resetDefaults() {
        this.schemasAndDtdsSettings = new SchemasAndDtdsSettings();
        this.defaultXmlSchemasSettings = new DefaultXmlSchemasSettings();
        this.jsonSchemaMappingsSettings = new JsonSchemaMappingsSettings();
        saveSettings();
        notifyListeners();
    }
}
