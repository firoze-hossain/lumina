package dev.lumina.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Service for dynamically discovering, structuring, and exploring Scratches and Consoles
 * (user scratch files and IDE extension scripts like Database Tools and SQL).
 */
public final class ScratchesAndConsolesService {

    private ScratchesAndConsolesService() {}

    public static Path getLuminaConfigDir() {
        Path userLumina = Path.of(System.getProperty("user.home"), ".lumina");
        try {
            if (!Files.exists(userLumina)) {
                Files.createDirectories(userLumina);
            }
            return userLumina;
        } catch (Exception e) {
            Path fallback = Path.of(System.getProperty("user.dir"), ".lumina");
            try {
                Files.createDirectories(fallback);
            } catch (Exception ignored) {}
            return fallback;
        }
    }

    public static Path getScratchesDir() {
        Path scratches = getLuminaConfigDir().resolve("scratches");
        try {
            if (!Files.exists(scratches)) {
                Files.createDirectories(scratches);
            }
        } catch (Exception ignored) {}
        return scratches;
    }

    public static Path getExtensionsDir() {
        Path extensions = getLuminaConfigDir().resolve("extensions");
        try {
            if (!Files.exists(extensions)) {
                Files.createDirectories(extensions);
            }
        } catch (Exception ignored) {}
        ensureDefaultExtensions(extensions);
        return extensions;
    }

    /**
     * Builds the top-level "Scratches and Consoles" tree node.
     */
    public static ProjectTreeNode buildScratchesAndConsolesNode(Path projectRoot) {
        return ProjectTreeNode.scratchesRoot(() -> buildTopLevelScratches(projectRoot));
    }

    private static List<ProjectTreeNode> buildTopLevelScratches(Path projectRoot) {
        List<ProjectTreeNode> list = new ArrayList<>();

        // 1. Extensions
        Path extensionsDir = getExtensionsDir();
        if (Files.isDirectory(extensionsDir)) {
            list.add(new ProjectTreeNode(
                    ProjectTreeNode.NodeKind.EXTENSIONS_ROOT,
                    "Extensions",
                    null,
                    extensionsDir,
                    "folder",
                    false,
                    () -> buildDirectoryChildren(extensionsDir),
                    null
            ));
        }

        // 2. Scratches
        Path scratchesDir = getScratchesDir();
        if (Files.isDirectory(scratchesDir)) {
            list.add(new ProjectTreeNode(
                    ProjectTreeNode.NodeKind.SCRATCHES_ROOT,
                    "Scratches",
                    null,
                    scratchesDir,
                    "folder",
                    false,
                    () -> buildDirectoryChildren(scratchesDir),
                    null
            ));
        }

        return list;
    }

    /**
     * Recursively and lazily builds file and folder tree nodes from disk.
     */
    public static List<ProjectTreeNode> buildDirectoryChildren(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return List.of();

        List<ProjectTreeNode> nodes = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> entries = stream
                    .sorted(Comparator
                            .comparing((Path p) -> !Files.isDirectory(p))
                            .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                    .toList();

            for (Path entry : entries) {
                String fileName = entry.getFileName().toString();
                if (fileName.startsWith(".")) continue;

                if (Files.isDirectory(entry)) {
                    nodes.add(new ProjectTreeNode(
                            ProjectTreeNode.NodeKind.EXTENSION_FOLDER,
                            fileName,
                            null,
                            entry,
                            "folder",
                            false,
                            () -> buildDirectoryChildren(entry),
                            null
                    ));
                } else {
                    String icon = detectScriptIcon(fileName);
                    nodes.add(new ProjectTreeNode(
                            ProjectTreeNode.NodeKind.EXTENSION_FILE,
                            fileName,
                            null,
                            entry,
                            icon,
                            true,
                            null,
                            opener -> {
                                if (opener != null) opener.accept(entry);
                            }
                    ));
                }
            }
        } catch (IOException ignored) {}

        return nodes;
    }

    private static String detectScriptIcon(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".groovy")) return "groovy";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".sql")) return "sql";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".xml")) return "xml";
        if (lower.endsWith(".html")) return "html";
        if (lower.endsWith(".csv") || lower.endsWith(".txt") || lower.endsWith(".md")) return "text";
        return "file";
    }

    /**
     * Automatically ensures standard Database Tools and SQL extensions are present on disk.
     */
    private static void ensureDefaultExtensions(Path extensionsDir) {
        Path dbTools = extensionsDir.resolve("Database Tools and SQL");
        if (Files.isDirectory(dbTools)) return;

        try {
            Files.createDirectories(dbTools);

            // 1. data/aggregators
            Path aggregators = dbTools.resolve("data").resolve("aggregators");
            Files.createDirectories(aggregators);
            List<String> aggFiles = List.of(
                    "AVG.groovy", "COEFFICIENT_OF_VARIATION.groovy", "COLS.groovy", "COUNT.groovy",
                    "COUNT_NUMS.groovy", "MAX.groovy", "MEDIAN.groovy", "MIN.groovy", "ROWS.groovy", "SUM.groovy"
            );
            for (String f : aggFiles) {
                createScriptIfMissing(aggregators.resolve(f), "// Data aggregator script for " + f + "\n");
            }

            // 2. data/extractors
            Path extractors = dbTools.resolve("data").resolve("extractors");
            Files.createDirectories(extractors);
            List<String> extFiles = List.of(
                    "CSV-Groovy.csv.groovy", "HTML-Groovy.html.groovy", "HTML-JavaScript.html.js",
                    "JSON-Groovy.json.groovy", "Markdown-Groovy.md.groovy", "One-row.sql.groovy",
                    "Pretty-Groovy.txt.groovy", "SQL-Insert-Multirow.sql.groovy",
                    "SQL-Insert-Statements.sql.groovy", "XML-Groovy.xml.groovy"
            );
            for (String f : extFiles) {
                createScriptIfMissing(extractors.resolve(f), "// Data extractor script for " + f + "\n");
            }

            // 3. schema
            Path schema = dbTools.resolve("schema");
            Files.createDirectories(schema);
            createScriptIfMissing(schema.resolve("Generate POJOs.groovy"), "// Schema generator for POJOs\n");

            // 4. schema.layouts
            Path layouts = dbTools.resolve("schema.layouts");
            Files.createDirectories(layouts);
            List<String> layoutFiles = List.of(
                    "File per object.groovy", "File per object by schema.groovy",
                    "File per object by schema and database.groovy",
                    "File per object by schema and type.groovy",
                    "File per object with order.groovy"
            );
            for (String f : layoutFiles) {
                createScriptIfMissing(layouts.resolve(f), "// Schema layout script for " + f + "\n");
            }
        } catch (Exception ignored) {}
    }

    private static void createScriptIfMissing(Path file, String template) {
        if (!Files.exists(file)) {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, template);
            } catch (Exception ignored) {}
        }
    }
}
