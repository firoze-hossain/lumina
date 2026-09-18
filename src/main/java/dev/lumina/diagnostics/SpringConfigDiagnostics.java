package dev.lumina.diagnostics;

import dev.lumina.diagnostics.JavaDiagnostics.Diag;
import dev.lumina.diagnostics.JavaDiagnostics.Severity;
import dev.lumina.spring.SpringConfigMetadata;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IntelliJ Ultimate-style inspections for application.properties /
 * application.yml, driven entirely by what is actually on the project's
 * classpath (the same {@code META-INF/spring-configuration-metadata.json}
 * IntelliJ itself reads) rather than a fixed, hand-maintained list:
 *
 * <ul>
 *   <li>"Cannot resolve configuration property 'x.y.z'" for a key that
 *       doesn't match any known property, namespace or Map-typed root of
 *       the starters actually present.</li>
 *   <li>"Driver class ... not found in dependencies" for a
 *       spring.datasource.url whose jdbc: vendor has no matching driver jar
 *       on the classpath, with a real one-click "Add dependency" fix.</li>
 * </ul>
 */
public final class SpringConfigDiagnostics {

    /** One key found in the file: its full dotted path plus where it sits. */
    private record Entry(String path, String value, boolean leaf,
                         int line, int keyStart, int keyEnd,
                         int valueStart, int valueEnd) {
    }

    /** vendor -> {driver class, jar-name substring, AddStartersDialog id (or null)} */
    private static final Map<String, String[]> JDBC_VENDORS = new HashMap<>();
    static {
        JDBC_VENDORS.put("mysql", new String[]{"com.mysql.cj.jdbc.Driver", "mysql-connector", "mysql"});
        JDBC_VENDORS.put("mariadb", new String[]{"org.mariadb.jdbc.Driver", "mariadb-java-client", "mariadb"});
        JDBC_VENDORS.put("postgresql", new String[]{"org.postgresql.Driver", "postgresql", "postgresql"});
        JDBC_VENDORS.put("h2", new String[]{"org.h2.Driver", "h2-", "h2"});
        JDBC_VENDORS.put("sqlserver", new String[]{"com.microsoft.sqlserver.jdbc.SQLServerDriver", "mssql-jdbc", null});
        JDBC_VENDORS.put("oracle", new String[]{"oracle.jdbc.OracleDriver", "ojdbc", null});
    }

    private static final Pattern JDBC_URL = Pattern.compile("^jdbc:([A-Za-z0-9]+):");
    private static final int MAX_LENGTH = 400_000;

    private SpringConfigDiagnostics() {
    }

    /** Never throws — any parsing surprise just yields fewer diagnostics. */
    public static List<Diag> analyze(String text, boolean yaml,
                                     List<SpringConfigMetadata.Property> known,
                                     String classpath) {
        return analyze(text, yaml, known, classpath, null);
    }

    public static List<Diag> analyze(String text, boolean yaml,
                                     List<SpringConfigMetadata.Property> known,
                                     String classpath,
                                     String moduleName) {
        try {
            if (text == null || text.length() > MAX_LENGTH) return List.of();
            List<Entry> entries = yaml ? parseYaml(text) : parseProperties(text);
            List<Diag> out = new ArrayList<>();
            Names names = Names.from(known == null ? List.of() : known);

            Map<String, SpringConfigMetadata.Property> propMap = new HashMap<>();
            if (known != null) {
                for (SpringConfigMetadata.Property p : known) {
                    propMap.put(normalize(p.name()), p);
                }
            }

            Map<String, Entry> seenLeaves = new HashMap<>();

            for (Entry e : entries) {
                String norm = normalize(e.path());
                if (e.leaf()) {
                    if (seenLeaves.containsKey(norm)) {
                        SpringConfigMetadata.Property prop = findProperty(norm, propMap);
                        String propType = prop != null ? simpleTypeName(prop.type()) : "String";
                        String desc = prop != null ? prop.description() : null;
                        String origin = (prop != null && prop.sourceJar() != null)
                                ? "Maven: " + prop.sourceJar()
                                : "Maven: org.springframework.boot:spring-boot-4.1.1.jar";
                        out.add(new Diag(
                                Severity.ERROR,
                                e.line(),
                                e.keyStart(),
                                e.keyEnd(),
                                "Duplicate property key",
                                "remove-property-line:" + e.line(),
                                "Duplicate property key",
                                propType,
                                desc,
                                e.path(),
                                origin
                        ));
                    } else {
                        seenLeaves.put(norm, e);
                    }
                }

                boolean hasDriverIssue = false;
                if (e.leaf() && norm.equals("spring.datasource.url")) {
                    hasDriverIssue = checkDriver(e, classpath, moduleName, out);
                }
                if (!hasDriverIssue && !names.knows(norm)) {
                    out.add(new Diag(Severity.WARNING, e.line(), e.keyStart(), e.keyEnd(),
                            "Cannot resolve configuration property '" + e.path() + "'",
                            null,
                            "Cannot resolve configuration property '" + e.path() + "'",
                            null, null, e.path(), null));
                }
            }
            return out;
        } catch (Throwable t) {
            return List.of();
        }
    }

    private static SpringConfigMetadata.Property findProperty(
            String norm,
            Map<String, SpringConfigMetadata.Property> propMap) {
        SpringConfigMetadata.Property p = propMap.get(norm);
        if (p != null) return p;
        return SpringConfigMetadata.getBuiltinProperty(norm);
    }

    private static String simpleTypeName(String type) {
        if (type == null || type.isBlank()) return "String";
        if (type.startsWith("java.lang.")) return type.substring("java.lang.".length());
        int dot = type.lastIndexOf('.');
        return dot >= 0 ? type.substring(dot + 1) : type;
    }

    private static boolean checkDriver(Entry e, String classpath, String moduleName, List<Diag> out) {
        Matcher m = JDBC_URL.matcher(e.value());
        if (!m.find()) return false;
        String[] info = JDBC_VENDORS.get(m.group(1).toLowerCase());
        if (info == null) return false;
        String driverClass = info[0];
        String jarNeedle = info[1];
        String starterId = info[2];
        if (classpathHasJar(classpath, jarNeedle)) return false;
        String title = "Driver class " + driverClass + " not found in dependencies";
        String quickFix = starterId != null ? "add-dependency:" + starterId : null;
        String context = e.path() + "=\"" + e.value() + "\"";
        String origin = (moduleName != null && !moduleName.isBlank()) ? moduleName : "module";
        out.add(new Diag(Severity.ERROR, e.line(), e.keyStart(), e.valueEnd(),
                title, quickFix, title, null, null, context, origin));
        return true;
    }

    private static boolean classpathHasJar(String classpath, String needle) {
        if (classpath == null || classpath.isBlank()) return false;
        String n = needle.toLowerCase();
        for (String part : classpath.split(Pattern.quote(File.pathSeparator))) {
            if (part.isBlank()) continue;
            String name = new File(part).getName().toLowerCase();
            if (name.contains(n)) return true;
        }
        return false;
    }

    // -------------------------------------------------- known-property set

    /** Relaxed-binding lookup: exact leaves, valid ancestor namespaces, and
     *  Map-typed roots (whose children are arbitrary user keys). */
    private static final class Names {
        final Set<String> exact = new HashSet<>();
        final Set<String> prefixes = new HashSet<>();
        final Set<String> mapRoots = new HashSet<>();

        static Names from(List<SpringConfigMetadata.Property> props) {
            Names n = new Names();
            for (SpringConfigMetadata.Property p : props) {
                String norm = normalize(p.name());
                n.exact.add(norm);
                String[] segs = norm.split("\\.");
                StringBuilder pre = new StringBuilder();
                for (int i = 0; i < segs.length - 1; i++) {
                    if (i > 0) pre.append('.');
                    pre.append(segs[i]);
                    n.prefixes.add(pre.toString());
                }
                String type = p.type();
                if (type != null && (type.contains("java.util.Map")
                        || type.contains("java.util.Properties"))) {
                    n.mapRoots.add(norm);
                }
            }
            return n;
        }

        boolean knows(String norm) {
            if (exact.contains(norm) || prefixes.contains(norm)) return true;
            for (String root : mapRoots) {
                if (norm.equals(root) || norm.startsWith(root + ".")) return true;
            }
            // Always-dynamic namespaces IntelliJ never flags, regardless of
            // classpath — arbitrary logger names / profile groups.
            if (norm.startsWith("logging.level.") || norm.equals("logging.level")) return true;
            if (norm.startsWith("logging.group.")) return true;
            if (norm.startsWith("spring.profiles.group.")) return true;
            return exact.isEmpty() && prefixes.isEmpty();   // no metadata at all: stay silent
        }
    }

    /** Spring's relaxed binding treats kebab-case/camelCase/UPPER_SNAKE as
     *  the same key; normalize each dotted segment for comparison. */
    private static String normalize(String name) {
        StringBuilder out = new StringBuilder();
        for (String seg : name.split("\\.")) {
            int bracket = seg.indexOf('[');
            if (bracket >= 0) seg = seg.substring(0, bracket);
            if (!out.isEmpty()) out.append('.');
            for (int i = 0; i < seg.length(); i++) {
                char c = seg.charAt(i);
                if (c != '-' && c != '_') out.append(Character.toLowerCase(c));
            }
        }
        return out.toString();
    }

    // ------------------------------------------------------------- parsing

    private static List<Entry> parseProperties(String text) {
        List<Entry> out = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        int offset = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.stripLeading();
            int indent = line.length() - trimmed.length();
            if (!trimmed.isEmpty() && trimmed.charAt(0) != '#' && trimmed.charAt(0) != '!') {
                int sep = findSeparator(line, indent);
                int keyEndCol = sep < 0 ? line.length() : sep;
                String key = line.substring(indent, keyEndCol).strip();
                if (!key.isEmpty()) {
                    String value = sep < 0 || sep + 1 > line.length()
                            ? "" : line.substring(sep + 1).strip();
                    int valueStart = sep < 0 ? offset + line.length() : offset + sep + 1;
                    while (valueStart < offset + line.length()
                            && Character.isWhitespace(line.charAt(valueStart - offset))) {
                        valueStart++;
                    }
                    out.add(new Entry(key, value, true, i + 1,
                            offset + indent, offset + indent + key.length(),
                            valueStart, offset + line.length()));
                }
            }
            offset += line.length() + 1;
        }
        return out;
    }

    /** First unescaped '=' or ':' or run of whitespace after the key. */
    private static int findSeparator(String line, int from) {
        for (int i = from; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == '=' || c == ':' || Character.isWhitespace(c)) return i;
        }
        return -1;
    }

    private static List<Entry> parseYaml(String text) {
        List<Entry> out = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        List<int[]> stack = new ArrayList<>();   // [indent] parallel to keys
        List<String> keys = new ArrayList<>();
        int offset = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.stripLeading();
            int indent = line.length() - trimmed.length();
            if (trimmed.isEmpty() || trimmed.charAt(0) == '#') {
                offset += line.length() + 1;
                continue;
            }
            if (trimmed.startsWith("---")) {
                stack.clear();
                keys.clear();
                offset += line.length() + 1;
                continue;
            }
            int dashExtra = 0;
            if (trimmed.startsWith("- ")) {
                // strip exactly the "-" plus any further leading spaces
                int stripped = 1;
                while (stripped < trimmed.length() && trimmed.charAt(stripped) == ' ') stripped++;
                dashExtra = stripped;
                trimmed = trimmed.substring(stripped);
            } else if (trimmed.equals("-")) {
                offset += line.length() + 1;
                continue;
            }
            int effectiveIndent = indent + dashExtra;

            int colon = findYamlColon(trimmed);
            if (colon < 0) {
                offset += line.length() + 1;
                continue;
            }
            String key = trimmed.substring(0, colon).strip();
            if (key.isEmpty() || key.startsWith("#")) {
                offset += line.length() + 1;
                continue;
            }
            String rawValue = colon + 1 <= trimmed.length() ? trimmed.substring(colon + 1) : "";
            String value = stripYamlComment(rawValue).strip();

            while (!stack.isEmpty() && stack.get(stack.size() - 1)[0] >= effectiveIndent) {
                stack.remove(stack.size() - 1);
                keys.remove(keys.size() - 1);
            }

            StringBuilder path = new StringBuilder();
            for (String k : keys) { path.append(k).append('.'); }
            path.append(key);

            int keyStartCol = indent + dashExtra;
            int keyStart = offset + keyStartCol;
            int keyEnd = keyStart + key.length();

            if (!value.isEmpty() && !value.equals("|") && !value.equals(">")) {
                int rawStartCol = keyStartCol + colon + 1;
                int valueCol = rawStartCol;
                while (valueCol < line.length() && Character.isWhitespace(line.charAt(valueCol))) {
                    valueCol++;
                }
                int valueStart = offset + Math.min(valueCol, line.length());
                out.add(new Entry(path.toString(), value, true, i + 1,
                        keyStart, keyEnd, valueStart, valueStart + value.length()));
            } else {
                out.add(new Entry(path.toString(), "", false, i + 1,
                        keyStart, keyEnd, keyEnd, keyEnd));
            }

            stack.add(new int[]{effectiveIndent});
            keys.add(key);
            offset += line.length() + 1;
        }
        return out;
    }

    /** First ':' that acts as a key/value separator (followed by whitespace
     *  or end of line) — skips colons inside quoted scalars and URLs like
     *  "http://host:1234" which have no space after the ':'. */
    private static int findYamlColon(String s) {
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (c == ':' && !inSingle && !inDouble) {
                if (i + 1 >= s.length() || Character.isWhitespace(s.charAt(i + 1))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String stripYamlComment(String value) {
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (c == '#' && !inSingle && !inDouble
                    && (i == 0 || Character.isWhitespace(value.charAt(i - 1)))) {
                return value.substring(0, i);
            }
        }
        return value;
    }
}
