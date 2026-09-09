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
                           String defaultValue) {
    }

    private SpringConfigMetadata() {
    }

    /** Scans every .jar on the given classpath string; safe to call from a
     *  background thread, never throws \u2014 a missing or unreadable jar is
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
                out.addAll(parse(json));
            } catch (Exception ignored) {
                // unreadable jar or malformed metadata: just skip it
            }
        }
        return out;
    }

    /** Pure parsing, safe to unit-test without touching a jar file. */
    public static List<Property> parse(String json) {
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
                out.add(new Property(name, type, description, defaultValue));
            }
        } catch (Exception ignored) {
            // malformed metadata: return whatever was parsed so far (empty)
        }
        return out;
    }
}