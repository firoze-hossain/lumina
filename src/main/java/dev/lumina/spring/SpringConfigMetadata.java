package dev.lumina.spring;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * Reads every dependency jar's {@code META-INF/spring-configuration-
 * metadata.json} \u2014 the same file Spring Boot generates for each starter
 * and the same one IntelliJ Ultimate's Spring completion reads from. This
 * makes application.properties/application.yml completion exact and
 * current for whatever starters the project actually has on its
 * classpath, rather than a guessed or hand-maintained list.
 */
public final class SpringConfigMetadata {

    public record Property(String name, String type, String description,
                           String defaultValue, String sourceJar) {
        public Property(String name, String type, String description, String defaultValue) {
            this(name, type, description, defaultValue, null);
        }
    }

    private static final java.util.Map<String, Property> BUILTIN_PROPERTIES = new java.util.HashMap<>();
    static {
        BUILTIN_PROPERTIES.put("spring.application.name", new Property(
                "spring.application.name", "java.lang.String",
                "Application name. Typically used with logging to help identify the application.",
                "", "org.springframework.boot:spring-boot-4.1.1.jar"));
        BUILTIN_PROPERTIES.put("spring.datasource.url", new Property(
                "spring.datasource.url", "java.lang.String",
                "JDBC URL of the database.",
                "", "org.springframework.boot:spring-boot-autoconfigure.jar"));
        BUILTIN_PROPERTIES.put("spring.datasource.username", new Property(
                "spring.datasource.username", "java.lang.String",
                "Login username of the database.",
                "", "org.springframework.boot:spring-boot-autoconfigure.jar"));
        BUILTIN_PROPERTIES.put("spring.datasource.password", new Property(
                "spring.datasource.password", "java.lang.String",
                "Login password of the database.",
                "", "org.springframework.boot:spring-boot-autoconfigure.jar"));
        BUILTIN_PROPERTIES.put("spring.jpa.hibernate.ddl-auto", new Property(
                "spring.jpa.hibernate.ddl-auto", "java.lang.String",
                "DDL mode. This is actually a shortcut for the 'hibernate.hbm2ddl.auto' property.",
                "", "org.springframework.boot:spring-boot-autoconfigure.jar"));
        BUILTIN_PROPERTIES.put("spring.jpa.properties.hibernate.format_sql", new Property(
                "spring.jpa.properties.hibernate.format_sql", "java.lang.Boolean",
                "Whether to format SQL output.",
                "", "org.springframework.boot:spring-boot-autoconfigure.jar"));
        BUILTIN_PROPERTIES.put("spring.thymeleaf.cache", new Property(
                "spring.thymeleaf.cache", "java.lang.Boolean",
                "Whether to enable template caching.",
                "", "org.springframework.boot:spring-boot-autoconfigure.jar"));
        BUILTIN_PROPERTIES.put("spring.servlet.multipart.max-file-size", new Property(
                "spring.servlet.multipart.max-file-size", "org.springframework.util.unit.DataSize",
                "Max file size.",
                "-1", "org.springframework.boot:spring-boot-autoconfigure.jar"));
        BUILTIN_PROPERTIES.put("spring.servlet.multipart.max-request-size", new Property(
                "spring.servlet.multipart.max-request-size", "org.springframework.util.unit.DataSize",
                "Max request size.",
                "-1", "org.springframework.boot:spring-boot-autoconfigure.jar"));
    }

    public static Property getBuiltinProperty(String name) {
        if (name == null) return null;
        return BUILTIN_PROPERTIES.get(name.trim());
    }

    private SpringConfigMetadata() {
    }

    /** Scans every .jar on the given classpath string; safe to call from a
     *  background thread, never throws — a missing or unreadable jar is
     *  just skipped. */
    public static List<Property> scan(String classpath) {
        List<Property> out = new ArrayList<>();
        if (classpath == null || classpath.isBlank()) return out;
        for (String part : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (!part.endsWith(".jar")) continue;
            try (ZipFile zip = new ZipFile(part)) {
                var entry = zip.getEntry("META-INF/spring-configuration-metadata.json");
                if (entry == null) continue;
                String json = new String(zip.getInputStream(entry).readAllBytes(),
                        StandardCharsets.UTF_8);
                String jarName = new File(part).getName();
                out.addAll(parse(json, jarName));
            } catch (Exception ignored) {
                // unreadable jar or malformed metadata: just skip it
            }
        }
        return out;
    }

    /** Pure parsing, safe to unit-test without touching a jar file. */
    public static List<Property> parse(String json) {
        return parse(json, null);
    }

    public static List<Property> parse(String json, String sourceJar) {
        List<Property> out = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("properties")) return out;
            for (JsonElement el : root.getAsJsonArray("properties")) {
                JsonObject p = el.getAsJsonObject();
                if (!p.has("name")) continue;
                String name = p.get("name").getAsString();
                String type = p.has("type") && !p.get("type").isJsonNull()
                        ? p.get("type").getAsString() : "";
                String description = p.has("description") && !p.get("description").isJsonNull()
                        ? p.get("description").getAsString() : "";
                String defaultValue = p.has("defaultValue") && !p.get("defaultValue").isJsonNull()
                        ? String.valueOf(p.get("defaultValue")) : "";
                out.add(new Property(name, type, description, defaultValue, sourceJar));
            }
        } catch (Exception ignored) {
            // malformed metadata: return whatever was parsed so far (empty)
        }
        return out;
    }
}