package dev.lumina.scope;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a named file scope in Lumina IDE (e.g. Tests, Non-Project Files,
 * Generated Files, or user-defined Local/Shared scopes).
 */
public class NamedScope implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String pattern;
    private boolean shared;
    private boolean builtIn;

    public NamedScope() {
    }

    public NamedScope(String name, String pattern, boolean shared, boolean builtIn) {
        this.name = name;
        this.pattern = pattern != null ? pattern : "";
        this.shared = shared;
        this.builtIn = builtIn;
    }

    public static NamedScope builtIn(String name, String pattern) {
        return new NamedScope(name, pattern, false, true);
    }

    public static NamedScope local(String name, String pattern) {
        return new NamedScope(name, pattern, false, false);
    }

    public static NamedScope shared(String name, String pattern) {
        return new NamedScope(name, pattern, true, false);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern != null ? pattern : "";
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    /**
     * Checks whether a given path matches this scope.
     */
    public boolean matches(Path path, Path projectRoot) {
        if (path == null) return false;
        String pathStr = path.toString().replace('\\', '/');

        // Built-in scope matching logic
        if ("Tests".equalsIgnoreCase(name)) {
            return pathStr.contains("/test/") || pathStr.contains("/tests/") ||
                    pathStr.contains("Test.java") || pathStr.contains("Tests.java") ||
                    pathStr.contains("Spec.java");
        }
        if ("Generated Files".equalsIgnoreCase(name)) {
            return pathStr.contains("/target/generated-sources/") ||
                    pathStr.contains("/build/generated/") ||
                    pathStr.contains("/.system_generated/") ||
                    pathStr.contains("/generated/");
        }
        if ("Non-Project Files".equalsIgnoreCase(name)) {
            if (projectRoot != null) {
                String rootStr = projectRoot.toString().replace('\\', '/');
                return !pathStr.startsWith(rootStr);
            }
            return false;
        }
        if ("Project Files".equalsIgnoreCase(name)) {
            if (projectRoot != null) {
                String rootStr = projectRoot.toString().replace('\\', '/');
                return pathStr.startsWith(rootStr);
            }
            return true;
        }

        // Custom pattern matching (standard glob syntax)
        if (pattern != null && !pattern.trim().isEmpty()) {
            try {
                String regex = globToRegex(pattern);
                return pathStr.matches(regex);
            } catch (Exception ignored) {
                return pathStr.contains(pattern);
            }
        }

        return false;
    }

    static String globToRegex(String pattern) {
        if (pattern == null || pattern.isBlank()) return ".*";
        String p = pattern.trim().replace('\\', '/');
        if (p.startsWith("file:*")) {
            p = p.substring(6);
        } else if (p.startsWith("file:")) {
            p = p.substring(5);
        } else if (p.startsWith("file[") && p.contains("]:")) {
            p = p.substring(p.indexOf("]:") + 2);
        }

        p = p.replace("//*", "/**");
        p = p.replace("//", "/");

        StringBuilder sb = new StringBuilder(".*");
        int i = 0;
        int len = p.length();
        while (i < len) {
            char c = p.charAt(i);
            if (c == '*' && i + 1 < len && p.charAt(i + 1) == '*') {
                sb.append(".*");
                i += 2;
                if (i < len && p.charAt(i) == '/') {
                    sb.append("/?");
                    i++;
                }
            } else if (c == '*') {
                sb.append("[^/]*");
                i++;
            } else if (c == '?') {
                sb.append("[^/]");
                i++;
            } else if (".()[]^$+{}|\\".indexOf(c) != -1) {
                sb.append('\\').append(c);
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        sb.append(".*");
        return sb.toString();
    }

    public NamedScope deepCopy() {
        return new NamedScope(this.name, this.pattern, this.shared, this.builtIn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedScope that = (NamedScope) o;
        return shared == that.shared &&
                builtIn == that.builtIn &&
                Objects.equals(name, that.name) &&
                Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pattern, shared, builtIn);
    }

    @Override
    public String toString() {
        return name + (shared ? " [Shared]" : "");
    }
}
