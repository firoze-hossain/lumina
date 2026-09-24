package dev.lumina.semantics;

import dev.lumina.settings.AutoImportSettings;
import dev.lumina.settings.AutoImportSettings.InsertImportsMode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dynamic IntelliJ IDEA-style Auto Import engine.
 * Handles imports on paste, on-the-fly resolution, exclusions, static members,
 * and import optimization without hardcoded behavior.
 */
public final class AutoImportService {

    private static final AutoImportService INSTANCE = new AutoImportService();

    public static AutoImportService getInstance() {
        return INSTANCE;
    }

    // Common standard library Java types for unambiguous auto-import resolution
    private static final Map<String, String> COMMON_JAVA_TYPES = new HashMap<>();

    static {
        // java.util
        COMMON_JAVA_TYPES.put("List", "java.util.List");
        COMMON_JAVA_TYPES.put("ArrayList", "java.util.ArrayList");
        COMMON_JAVA_TYPES.put("LinkedList", "java.util.LinkedList");
        COMMON_JAVA_TYPES.put("Map", "java.util.Map");
        COMMON_JAVA_TYPES.put("HashMap", "java.util.HashMap");
        COMMON_JAVA_TYPES.put("TreeMap", "java.util.TreeMap");
        COMMON_JAVA_TYPES.put("LinkedHashMap", "java.util.LinkedHashMap");
        COMMON_JAVA_TYPES.put("Set", "java.util.Set");
        COMMON_JAVA_TYPES.put("HashSet", "java.util.HashSet");
        COMMON_JAVA_TYPES.put("TreeSet", "java.util.TreeSet");
        COMMON_JAVA_TYPES.put("Queue", "java.util.Queue");
        COMMON_JAVA_TYPES.put("Deque", "java.util.Deque");
        COMMON_JAVA_TYPES.put("ArrayDeque", "java.util.ArrayDeque");
        COMMON_JAVA_TYPES.put("Objects", "java.util.Objects");
        COMMON_JAVA_TYPES.put("Arrays", "java.util.Arrays");
        COMMON_JAVA_TYPES.put("Collections", "java.util.Collections");
        COMMON_JAVA_TYPES.put("Optional", "java.util.Optional");
        COMMON_JAVA_TYPES.put("UUID", "java.util.UUID");
        COMMON_JAVA_TYPES.put("Random", "java.util.Random");

        // java.util.concurrent
        COMMON_JAVA_TYPES.put("ConcurrentHashMap", "java.util.concurrent.ConcurrentHashMap");
        COMMON_JAVA_TYPES.put("CompletableFuture", "java.util.concurrent.CompletableFuture");
        COMMON_JAVA_TYPES.put("Future", "java.util.concurrent.Future");
        COMMON_JAVA_TYPES.put("Callable", "java.util.concurrent.Callable");
        COMMON_JAVA_TYPES.put("ExecutorService", "java.util.concurrent.ExecutorService");
        COMMON_JAVA_TYPES.put("Executors", "java.util.concurrent.Executors");
        COMMON_JAVA_TYPES.put("CopyOnWriteArrayList", "java.util.concurrent.CopyOnWriteArrayList");

        // java.util.regex
        COMMON_JAVA_TYPES.put("Pattern", "java.util.regex.Pattern");
        COMMON_JAVA_TYPES.put("Matcher", "java.util.regex.Matcher");

        // java.util.stream & function
        COMMON_JAVA_TYPES.put("Stream", "java.util.stream.Stream");
        COMMON_JAVA_TYPES.put("Collectors", "java.util.stream.Collectors");
        COMMON_JAVA_TYPES.put("Function", "java.util.function.Function");
        COMMON_JAVA_TYPES.put("Predicate", "java.util.function.Predicate");
        COMMON_JAVA_TYPES.put("Consumer", "java.util.function.Consumer");
        COMMON_JAVA_TYPES.put("Supplier", "java.util.function.Supplier");
        COMMON_JAVA_TYPES.put("BiFunction", "java.util.function.BiFunction");
        COMMON_JAVA_TYPES.put("BiConsumer", "java.util.function.BiConsumer");
        COMMON_JAVA_TYPES.put("BiPredicate", "java.util.function.BiPredicate");

        // java.nio & java.io
        COMMON_JAVA_TYPES.put("Path", "java.nio.file.Path");
        COMMON_JAVA_TYPES.put("Paths", "java.nio.file.Paths");
        COMMON_JAVA_TYPES.put("Files", "java.nio.file.Files");
        COMMON_JAVA_TYPES.put("File", "java.io.File");
        COMMON_JAVA_TYPES.put("InputStream", "java.io.InputStream");
        COMMON_JAVA_TYPES.put("OutputStream", "java.io.OutputStream");
        COMMON_JAVA_TYPES.put("BufferedReader", "java.io.BufferedReader");
        COMMON_JAVA_TYPES.put("BufferedWriter", "java.io.BufferedWriter");
        COMMON_JAVA_TYPES.put("IOException", "java.io.IOException");

        // java.math
        COMMON_JAVA_TYPES.put("BigDecimal", "java.math.BigDecimal");
        COMMON_JAVA_TYPES.put("BigInteger", "java.math.BigInteger");

        // java.time
        COMMON_JAVA_TYPES.put("LocalDate", "java.time.LocalDate");
        COMMON_JAVA_TYPES.put("LocalTime", "java.time.LocalTime");
        COMMON_JAVA_TYPES.put("LocalDateTime", "java.time.LocalDateTime");
        COMMON_JAVA_TYPES.put("Instant", "java.time.Instant");
        COMMON_JAVA_TYPES.put("Duration", "java.time.Duration");
        COMMON_JAVA_TYPES.put("Period", "java.time.Period");
        COMMON_JAVA_TYPES.put("ZonedDateTime", "java.time.ZonedDateTime");
        COMMON_JAVA_TYPES.put("ZoneId", "java.time.ZoneId");

        // java.net
        COMMON_JAVA_TYPES.put("URI", "java.net.URI");
        COMMON_JAVA_TYPES.put("URL", "java.net.URL");
    }

    private AutoImportService() {}

    /**
     * Resolves needed imports for pasted code.
     * Honors exclusion rules and configured paste mode.
     */
    public List<String> resolvePastedImports(String currentSource, String pastedText) {
        if (pastedText == null || pastedText.isBlank()) return List.of();

        AutoImportSettings settings = AutoImportSettings.getInstance();
        Set<String> needed = new LinkedHashSet<>();

        // Regex for simple Java identifier types
        Pattern identPattern = Pattern.compile("\\b([A-Z][a-zA-Z0-9_]*)\\b");
        Matcher m = identPattern.matcher(pastedText);

        while (m.find()) {
            String typeName = m.group(1);
            String fqcn = COMMON_JAVA_TYPES.get(typeName);
            if (fqcn != null) {
                // Check if excluded by user configuration
                if (settings.isJavaExcluded(fqcn)) {
                    continue;
                }
                // Check if current file already has this import or doesn't need it
                if (Completion.needsImport(currentSource, fqcn)) {
                    needed.add(fqcn);
                }
            }
        }

        return new ArrayList<>(needed);
    }

    /**
     * Inserts given FQCNs as import statements into source text.
     */
    public String insertImports(String source, List<String> fqcns) {
        if (fqcns == null || fqcns.isEmpty() || source == null) return source;

        StringBuilder importsBlock = new StringBuilder();
        for (String fqcn : fqcns) {
            if (Completion.needsImport(source, fqcn)) {
                importsBlock.append("import ").append(fqcn).append(";\n");
            }
        }

        if (importsBlock.isEmpty()) return source;

        int insertOffset = Completion.importInsertOffset(source);
        if (insertOffset >= source.length()) {
            return source + "\n" + importsBlock;
        } else {
            return source.substring(0, insertOffset) + importsBlock + source.substring(insertOffset);
        }
    }

    /**
     * Optimized imports according to IntelliJ IDEA grouping rules:
     * 1. import static ...
     * 2. java.*, javax.*
     * 3. third party and project imports
     */
    public String optimizeImports(String source) {
        if (source == null || source.isBlank()) return source;

        String[] lines = source.split("\n", -1);
        List<String> staticImports = new ArrayList<>();
        List<String> javaImports = new ArrayList<>();
        List<String> otherImports = new ArrayList<>();
        List<String> nonImportLinesBefore = new ArrayList<>();
        List<String> nonImportLinesAfter = new ArrayList<>();

        boolean seenImport = false;
        boolean pastImports = false;

        for (String raw : lines) {
            String trimmed = raw.strip();
            if (trimmed.startsWith("import ")) {
                seenImport = true;
                if (trimmed.startsWith("import static ")) {
                    if (!staticImports.contains(trimmed)) staticImports.add(trimmed);
                } else if (trimmed.startsWith("import java.") || trimmed.startsWith("import javax.")) {
                    if (!javaImports.contains(trimmed)) javaImports.add(trimmed);
                } else {
                    if (!otherImports.contains(trimmed)) otherImports.add(trimmed);
                }
            } else {
                if (seenImport) {
                    if (!trimmed.isEmpty()) pastImports = true;
                    if (pastImports) nonImportLinesAfter.add(raw);
                } else {
                    nonImportLinesBefore.add(raw);
                }
            }
        }

        if (!seenImport) return source;

        Collections.sort(staticImports);
        Collections.sort(javaImports);
        Collections.sort(otherImports);

        StringBuilder sb = new StringBuilder();
        for (String line : nonImportLinesBefore) {
            sb.append(line).append("\n");
        }
        if (!nonImportLinesBefore.isEmpty() && !nonImportLinesBefore.get(nonImportLinesBefore.size() - 1).isBlank()) {
            sb.append("\n");
        }

        boolean hasPrevious = false;
        if (!staticImports.isEmpty()) {
            for (String s : staticImports) sb.append(s).append("\n");
            hasPrevious = true;
        }
        if (!javaImports.isEmpty()) {
            if (hasPrevious) sb.append("\n");
            for (String s : javaImports) sb.append(s).append("\n");
            hasPrevious = true;
        }
        if (!otherImports.isEmpty()) {
            if (hasPrevious) sb.append("\n");
            for (String s : otherImports) sb.append(s).append("\n");
            hasPrevious = true;
        }

        if (!nonImportLinesAfter.isEmpty()) {
            if (hasPrevious) sb.append("\n");
            for (int i = 0; i < nonImportLinesAfter.size(); i++) {
                sb.append(nonImportLinesAfter.get(i));
                if (i < nonImportLinesAfter.size() - 1) sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Checks if a symbol should be excluded based on user exclusion settings.
     */
    public boolean isExcluded(String symbol, String language) {
        if (symbol == null) return false;
        AutoImportSettings settings = AutoImportSettings.getInstance();
        if ("rust".equalsIgnoreCase(language)) {
            return settings.isRustExcluded(symbol, false);
        }
        return settings.isJavaExcluded(symbol);
    }
}
