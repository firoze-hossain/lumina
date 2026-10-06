package dev.lumina.filetypes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Manages recognized and user-defined File Types, wildcard patterns,
 * hashbang mappings, and ignored files/folders configuration.
 */
public class FileTypeManager {

    private static final String DEFAULT_IGNORED_PATTERNS =
            "*.hprof;*.pyc;*.pyo;*.rbc;*.yarb;*~;.DS_Store;.git;.hg;.svn;CVS;__pycache__;_svn;vssver.scc;vssver2.scc;";

    private static final String CONFIG_DIR =
            System.getProperty("user.home") + File.separator + ".lumina";
    private static final String CONFIG_FILE =
            CONFIG_DIR + File.separator + "filetypes.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final FileTypeManager INSTANCE = new FileTypeManager();

    private final ObservableList<FileType> fileTypes = FXCollections.observableArrayList();
    private String ignoredPatterns = DEFAULT_IGNORED_PATTERNS;

    private FileTypeManager() {
        initDefaults();
        load();
    }

    public static FileTypeManager getInstance() {
        return INSTANCE;
    }

    public ObservableList<FileType> getFileTypes() {
        return fileTypes;
    }

    public String getIgnoredPatterns() {
        return ignoredPatterns;
    }

    public void setIgnoredPatterns(String ignoredPatterns) {
        this.ignoredPatterns = ignoredPatterns != null ? ignoredPatterns : DEFAULT_IGNORED_PATTERNS;
    }

    public FileType findFileTypeByName(String name) {
        if (name == null) return null;
        for (FileType ft : fileTypes) {
            if (ft.getName().equalsIgnoreCase(name.trim())) {
                return ft;
            }
        }
        return null;
    }

    public FileType findFileTypeByFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        String name = fileName.replace('\\', '/');
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }

        // 1. Exact match with registered patterns
        for (FileType ft : fileTypes) {
            for (String pattern : ft.getPatterns()) {
                if (pattern.equalsIgnoreCase(name)) {
                    return ft;
                }
            }
        }

        // 2. Wildcard pattern matching (* and ?)
        for (FileType ft : fileTypes) {
            for (String pattern : ft.getPatterns()) {
                if (matchesWildcard(name, pattern)) {
                    return ft;
                }
            }
        }

        return null;
    }

    public FileType findFileTypeByHashbang(String hashbangLine) {
        if (hashbangLine == null || !hashbangLine.startsWith("#!")) {
            return null;
        }

        String content = hashbangLine.substring(2).trim();
        for (FileType ft : fileTypes) {
            for (String hb : ft.getHashbangs()) {
                if (hb != null && !hb.isEmpty()) {
                    if (content.contains("/" + hb) || content.endsWith(" " + hb) || content.equals(hb)) {
                        return ft;
                    }
                }
            }
        }

        return null;
    }

    public boolean isIgnored(String fileName) {
        if (fileName == null || fileName.isEmpty()) return false;
        String name = fileName.replace('\\', '/');
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }

        String[] parts = ignoredPatterns.split(";");
        for (String part : parts) {
            String p = part.trim();
            if (!p.isEmpty() && matchesWildcard(name, p)) {
                return true;
            }
        }
        return false;
    }

    public void registerFileType(FileType fileType) {
        if (fileType == null || fileType.getName() == null) return;
        for (int i = 0; i < fileTypes.size(); i++) {
            if (fileTypes.get(i).getName().equalsIgnoreCase(fileType.getName())) {
                fileTypes.set(i, fileType);
                return;
            }
        }
        fileTypes.add(fileType);
    }

    public boolean removeFileType(String name) {
        if (name == null) return false;
        return fileTypes.removeIf(ft -> !ft.isBuiltin() && ft.getName().equalsIgnoreCase(name));
    }

    public static boolean matchesWildcard(String text, String wildcardPattern) {
        if (text == null || wildcardPattern == null) return false;
        if (wildcardPattern.equals("*")) return true;

        String regex = wildcardToRegex(wildcardPattern);
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).matches();
    }

    private static String wildcardToRegex(String wildcard) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < wildcard.length(); i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    sb.append(".*");
                    break;
                case '?':
                    sb.append(".");
                    break;
                case '.':
                case '\\':
                case '[':
                case ']':
                case '(':
                case ')':
                case '{':
                case '}':
                case '+':
                case '^':
                case '$':
                case '|':
                    sb.append('\\').append(c);
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        sb.append("$");
        return sb.toString();
    }

    public void resetToDefaults() {
        fileTypes.clear();
        initDefaults();
        ignoredPatterns = DEFAULT_IGNORED_PATTERNS;
    }

    public void save() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            ConfigState state = new ConfigState();
            state.fileTypes = new ArrayList<>(this.fileTypes);
            state.ignoredPatterns = this.ignoredPatterns;

            try (FileWriter writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(state, writer);
            }
        } catch (IOException e) {
            // Log or ignore gracefully in non-critical settings save
        }
    }

    public void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            ConfigState state = GSON.fromJson(reader, ConfigState.class);
            if (state != null) {
                if (state.ignoredPatterns != null && !state.ignoredPatterns.trim().isEmpty()) {
                    this.ignoredPatterns = state.ignoredPatterns;
                }
                if (state.fileTypes != null && !state.fileTypes.isEmpty()) {
                    Map<String, FileType> loadedMap = new LinkedHashMap<>();
                    for (FileType ft : state.fileTypes) {
                        if (ft.getName() != null) {
                            loadedMap.put(ft.getName().toLowerCase(), ft);
                        }
                    }

                    // Keep existing builtins but overwrite patterns if modified, and preserve user-defined types
                    List<FileType> merged = new ArrayList<>();
                    Set<String> seen = new HashSet<>();

                    // First pass: add loaded types
                    for (FileType ft : state.fileTypes) {
                        merged.add(ft);
                        seen.add(ft.getName().toLowerCase());
                    }

                    // Second pass: make sure any built-ins not in config are included
                    for (FileType def : createDefaultFileTypes()) {
                        if (!seen.contains(def.getName().toLowerCase())) {
                            merged.add(def);
                        }
                    }

                    merged.sort(Comparator.comparing(FileType::getName, String.CASE_INSENSITIVE_ORDER));
                    this.fileTypes.setAll(merged);
                }
            }
        } catch (Exception e) {
            // Fallback to defaults
        }
    }

    private void initDefaults() {
        List<FileType> list = createDefaultFileTypes();
        list.sort(Comparator.comparing(FileType::getName, String.CASE_INSENSITIVE_ORDER));
        fileTypes.setAll(list);
    }

    private List<FileType> createDefaultFileTypes() {
        List<FileType> list = new ArrayList<>();

        list.add(new FileType("GitLab CI Expression language", "GitLab CI configuration expressions", "generic", true,
                List.of(".gitlabciexpression"), List.of()));

        list.add(new FileType(".aiignore (AiIgnore)", "AI tool ignore files", "git", true,
                List.of(".aiignore"), List.of()));

        list.add(new FileType(".dockerignore (DockerIgnore)", "Docker ignore files", "docker", true,
                List.of(".dockerignore"), List.of()));

        list.add(new FileType(".gitignore (Gitignore)", "Git ignore files", "git", true,
                List.of(".gitignore", ".gitattributes", ".gitmodules"), List.of()));

        list.add(new FileType(".hgignore (HgIgnore)", "Mercurial ignore files", "git", true,
                List.of(".hgignore"), List.of()));

        list.add(new FileType(".ignore (IgnoreLang)", "Generic ignore files", "git", true,
                List.of(".ignore", "*.ignore"), List.of()));

        list.add(new FileType("Angular HTML Template", "Angular template HTML files", "angular", true,
                List.of("*.component.html"), List.of()));

        list.add(new FileType("Angular HTML Template (17+)", "Angular template files for v17+", "angular", true,
                List.of(), List.of()));

        list.add(new FileType("Angular HTML Template (18.1+)", "Angular template files for v18.1+", "angular", true,
                List.of(), List.of()));

        list.add(new FileType("Angular HTML Template (20+)", "Angular template files for v20+", "angular", true,
                List.of(), List.of()));

        list.add(new FileType("Angular SVG Template", "Angular template SVG files", "angular", true,
                List.of("*.component.svg"), List.of()));

        list.add(new FileType("Angular SVG Template (17+)", "Angular SVG files for v17+", "angular", true,
                List.of(), List.of()));

        list.add(new FileType("Angular SVG Template (18.1+)", "Angular SVG files for v18.1+", "angular", true,
                List.of(), List.of()));

        list.add(new FileType("Angular SVG Template (20+)", "Angular SVG files for v20+", "angular", true,
                List.of(), List.of()));

        list.add(new FileType("Archive", "Compressed archive files", "archive", true,
                List.of("*.zip", "*.jar", "*.war", "*.ear", "*.tar", "*.gz", "*.7z", "*.rar"), List.of()));

        list.add(new FileType("AspectJ (syntax highlighting only)", "AspectJ source files", "java", true,
                List.of("*.aj"), List.of()));

        list.add(new FileType("C#", "C# source files", "csharp", true,
                List.of("*.cs", "*.csx"), List.of()));

        list.add(new FileType("C/C++", "C/C++ source and header files", "cpp", true,
                List.of("*.c", "*.cpp", "*.cc", "*.cxx", "*.h", "*.hpp", "*.hxx"), List.of()));

        list.add(new FileType("Cascading Style Sheet", "CSS stylesheets", "css", true,
                List.of("*.css", "*.scss", "*.sass", "*.less"), List.of()));

        list.add(new FileType("CSV", "Comma-separated values files", "text", true,
                List.of("*.csv", "*.tsv"), List.of()));

        list.add(new FileType("Dart", "Dart programming language source files", "generic", true,
                List.of("*.dart"), List.of()));

        list.add(new FileType("Dockerfile", "Docker container build definitions", "docker", true,
                List.of("Dockerfile", "Dockerfile*", "*.dockerfile"), List.of()));

        list.add(new FileType("Go", "Go programming language source files", "generic", true,
                List.of("*.go"), List.of()));

        list.add(new FileType("Groovy", "Groovy source files and scripts", "java", true,
                List.of("*.groovy", "*.gvy", "*.gy", "*.gsh", "Jenkinsfile*"), List.of("groovy")));

        list.add(new FileType("HTML", "HTML hypertext markup documents", "html", true,
                List.of("*.html", "*.htm", "*.xhtml"), List.of()));

        list.add(new FileType("Java", "Java source files", "java", true,
                List.of("*.java"), List.of()));

        list.add(new FileType("JavaScript", "JavaScript source files and scripts", "javascript", true,
                List.of("*.js", "*.mjs", "*.cjs", "*.jsx"), List.of("node")));

        list.add(new FileType("JSON", "JavaScript Object Notation files", "json", true,
                List.of("*.json", "*.json5"), List.of()));

        list.add(new FileType("Kotlin", "Kotlin programming language source files", "kotlin", true,
                List.of("*.kt", "*.kts"), List.of()));

        list.add(new FileType("Lua", "Lua script files", "generic", true,
                List.of("*.lua"), List.of("lua")));

        list.add(new FileType("Markdown", "Markdown documentation files", "markdown", true,
                List.of("*.md", "*.markdown"), List.of()));

        list.add(new FileType("PHP", "PHP hypertext preprocessor files", "generic", true,
                List.of("*.php", "*.phtml", "*.php4", "*.php5"), List.of("php")));

        list.add(new FileType("Properties", "Java property configuration files", "text", true,
                List.of("*.properties"), List.of()));

        list.add(new FileType("Python", "Python scripts and source files", "python", true,
                List.of("*.py", "*.pyw"), List.of("python", "python3")));

        list.add(new FileType("R", "R language statistical scripts", "generic", true,
                List.of("*.r", "*.R"), List.of("Rscript")));

        list.add(new FileType("Ruby", "Ruby scripts and source files", "generic", true,
                List.of("*.rb", "*.rbw", "Rakefile", "Gemfile"), List.of("ruby")));

        list.add(new FileType("Rust", "Rust systems programming source files", "rust", true,
                List.of("*.rs"), List.of()));

        list.add(new FileType("Scala", "Scala programming language source files", "java", true,
                List.of("*.scala", "*.sc"), List.of()));

        list.add(new FileType("Shell Script", "Unix shell script files", "shell", true,
                List.of("*.sh", "*.bash", "*.zsh", "*.fish"), List.of("sh", "bash", "zsh")));

        list.add(new FileType("SQL", "Structured Query Language scripts", "sql", true,
                List.of("*.sql", "*.ddl"), List.of()));

        list.add(new FileType("SVG", "Scalable Vector Graphics files", "html", true,
                List.of("*.svg"), List.of()));

        list.add(new FileType("Swift", "Swift programming language source files", "generic", true,
                List.of("*.swift"), List.of()));

        list.add(new FileType("Text", "Plain text files", "text", true,
                List.of("*.txt", "README*", "LICENSE*", "CHANGELOG*"), List.of()));

        list.add(new FileType("TOML", "Tom's Obvious Minimal Language configuration files", "text", true,
                List.of("*.toml"), List.of()));

        list.add(new FileType("TypeScript", "TypeScript typed JavaScript files", "typescript", true,
                List.of("*.ts", "*.tsx", "*.mts", "*.cts"), List.of()));

        list.add(new FileType("XML", "Extensible Markup Language documents", "xml", true,
                List.of("*.xml", "*.xsd", "*.xsl", "*.pom", "*.fxml"), List.of()));

        list.add(new FileType("YAML", "YAML Ain't Markup Language configuration files", "yaml", true,
                List.of("*.yaml", "*.yml"), List.of()));

        return list;
    }

    private static class ConfigState {
        List<FileType> fileTypes;
        String ignoredPatterns;
    }
}
