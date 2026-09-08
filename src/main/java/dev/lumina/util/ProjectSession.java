package dev.lumina.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Per-project session state \u2014 which folders were expanded in the project
 * tree, which files were open, and which tab was active \u2014 restored on
 * reopen exactly like IntelliJ's per-project workspace.xml. Keyed by a hash
 * of the project's absolute path so multiple projects each keep their own
 * independent session under ~/.lumina/sessions/.
 */
public final class ProjectSession {

    public record State(Set<String> expanded, List<String> openFiles, String activeFile) {
        public static final State EMPTY = new State(Set.of(), List.of(), null);
    }

    private ProjectSession() {
    }

    private static Path sessionFile(Path projectRoot) {
        String hash = Integer.toHexString(
                projectRoot.toAbsolutePath().normalize().toString().hashCode());
        return Path.of(System.getProperty("user.home"), ".lumina", "sessions",
                hash + ".properties");
    }

    /**
     * @param expandedAbs absolute paths of every expanded folder
     * @param openAbs     absolute paths of every open tab, in tab order
     * @param activeAbs   absolute path of the focused tab, or null
     */
    public static void save(Path projectRoot, Set<Path> expandedAbs,
                            List<Path> openAbs, Path activeAbs) {
        try {
            Path file = sessionFile(projectRoot);
            Files.createDirectories(file.getParent());
            Properties props = new Properties();
            props.setProperty("expanded", join(relativize(projectRoot, expandedAbs)));
            props.setProperty("open", join(relativizeList(projectRoot, openAbs)));
            if (activeAbs != null) {
                props.setProperty("active", relativizeOne(projectRoot, activeAbs));
            }
            try (var out = Files.newOutputStream(file)) {
                props.store(out, "Lumina project session \u2014 auto-generated, do not edit");
            }
        } catch (IOException ignored) {
            // session restore is a convenience, never worth failing a close over
        }
    }

    public static State load(Path projectRoot) {
        Path file = sessionFile(projectRoot);
        if (!Files.isRegularFile(file)) return State.EMPTY;
        try {
            Properties props = new Properties();
            try (var in = Files.newInputStream(file)) {
                props.load(in);
            }
            Set<String> expanded = new LinkedHashSet<>(
                    split(props.getProperty("expanded", "")));
            List<String> open = split(props.getProperty("open", ""));
            String active = props.getProperty("active");
            return new State(expanded, open, active == null || active.isBlank() ? null : active);
        } catch (IOException ignored) {
            return State.EMPTY;
        }
    }

    // --------------------------------------------------------------- helpers

    private static String relativizeOne(Path root, Path abs) {
        try {
            return root.toAbsolutePath().normalize()
                    .relativize(abs.toAbsolutePath().normalize())
                    .toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return abs.toAbsolutePath().toString().replace('\\', '/');
        }
    }

    private static List<String> relativize(Path root, Set<Path> paths) {
        List<String> out = new ArrayList<>();
        for (Path p : paths) out.add(relativizeOne(root, p));
        return out;
    }

    private static List<String> relativizeList(Path root, List<Path> paths) {
        List<String> out = new ArrayList<>();
        for (Path p : paths) out.add(relativizeOne(root, p));
        return out;
    }

    private static String join(List<String> parts) {
        return String.join("\u0001", parts);
    }

    private static List<String> split(String joined) {
        if (joined == null || joined.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String part : joined.split("\u0001")) {
            if (!part.isBlank()) out.add(part);
        }
        return out;
    }
}