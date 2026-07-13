package dev.lumina.diagnostics;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * M3 — live error highlighting: compiles one file in memory with the real
 * JDK compiler and returns precise diagnostics. Pure JDK (javax.tools), no
 * UI imports, fully unit-testable.
 */
public final class JavaDiagnostics {

    public enum Severity { ERROR, WARNING }

    /**
     * One squiggle: 1-based line, absolute [start,end) offsets into the
     * exact text that was compiled, and the compiler's message.
     */
    public record Diag(Severity severity, int line, int start, int end,
                       String message) {
    }

    private static volatile Path outputDir;

    private JavaDiagnostics() {
    }

    /**
     * Compile the given editor text as fileName's content. classpath may be
     * null. Returns diagnostics with offsets into text; empty on any setup
     * failure (never throws) and empty for Lombok files when lombok.jar
     * cannot be found on the classpath (avoids a false-error flood).
     */
    public static List<Diag> compile(Path file, String text, String classpath,
                                     List<Path> sourceRoots) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) return List.of();

            boolean lombok = text.contains("import lombok");
            String lombokJar = lombok ? findLombokJar(classpath) : null;
            if (lombok && lombokJar == null) {
                return List.of();   // honest skip: cannot resolve generated members
            }

            List<String> options = new ArrayList<>();
            if (classpath != null && !classpath.isBlank()) {
                options.add("-classpath");
                options.add(classpath);
            }
            if (sourceRoots != null && !sourceRoots.isEmpty()) {
                StringBuilder sp = new StringBuilder();
                for (Path root : sourceRoots) {
                    if (!sp.isEmpty()) sp.append(File.pathSeparator);
                    sp.append(root);
                }
                options.add("-sourcepath");
                options.add(sp.toString());
                options.add("-implicit:none");   // never compile siblings to disk
            }
            if (lombokJar != null) {
                options.add("-processorpath");
                options.add(lombokJar);
            } else {
                options.add("-proc:none");
            }
            options.add("-d");
            options.add(ensureOutputDir().toString());

            String unitName = unitNameFor(file, text);
            JavaFileObject source = new StringSource(unitName, text);
            DiagnosticCollector<JavaFileObject> collector =
                    new DiagnosticCollector<>();

            StandardJavaFileManager fm = compiler.getStandardFileManager(
                    collector, null, StandardCharsets.UTF_8);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fm, collector, options, null, List.of(source));
            task.call();
            fm.close();

            List<Diag> result = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> d
                    : collector.getDiagnostics()) {
                if (d.getSource() != source) continue;   // sourcepath noise
                Severity severity = switch (d.getKind()) {
                    case ERROR -> Severity.ERROR;
                    case WARNING, MANDATORY_WARNING -> Severity.WARNING;
                    default -> null;
                };
                if (severity == null) continue;
                int line = (int) Math.max(1, d.getLineNumber());
                int start = (int) d.getStartPosition();
                int end = (int) d.getEndPosition();
                if (start < 0 || start >= text.length()) {
                    start = offsetOf(text, line,
                            (int) Math.max(1, d.getColumnNumber()));
                }
                if (end <= start) {
                    end = start;
                    while (end < text.length()
                            && Character.isJavaIdentifierPart(text.charAt(end))) {
                        end++;
                    }
                    if (end == start) end = Math.min(start + 1, text.length());
                }
                end = Math.min(end, text.length());
                result.add(new Diag(severity, line, start, end,
                        d.getMessage(null)));
            }
            return result;
        } catch (Throwable t) {
            return List.of();
        }
    }

    // --------------------------------------------------------------- helpers

    private static String findLombokJar(String classpath) {
        if (classpath == null) return null;
        for (String part : classpath.split(Pattern.quote(File.pathSeparator))) {
            String name = Path.of(part).getFileName() != null
                    ? Path.of(part).getFileName().toString() : "";
            if (name.startsWith("lombok") && name.endsWith(".jar")
                    && Files.isRegularFile(Path.of(part))) {
                return part;
            }
        }
        return null;
    }

    /** "pkg/Name" derived from the package line and the file name. */
    private static String unitNameFor(Path file, String text) {
        String simple = file != null
                ? file.getFileName().toString().replace(".java", "")
                : "Editor";
        for (String raw : text.split("\n")) {
            String line = raw.strip();
            if (line.startsWith("package ") && line.endsWith(";")) {
                String pkg = line.substring(8, line.length() - 1).trim();
                return pkg.replace('.', '/') + "/" + simple;
            }
            if (line.startsWith("import ") || line.contains("class ")) break;
        }
        return simple;
    }

    private static int offsetOf(String text, int line, int column) {
        int offset = 0;
        int current = 1;
        while (current < line) {
            int nl = text.indexOf('\n', offset);
            if (nl < 0) break;
            offset = nl + 1;
            current++;
        }
        return Math.min(offset + Math.max(0, column - 1), text.length());
    }

    private static Path ensureOutputDir() throws Exception {
        Path dir = outputDir;
        if (dir == null || !Files.isDirectory(dir)) {
            synchronized (JavaDiagnostics.class) {
                if (outputDir == null || !Files.isDirectory(outputDir)) {
                    outputDir = Files.createTempDirectory("lumina-diag");
                }
                dir = outputDir;
            }
        }
        return dir;
    }

    private static final class StringSource extends SimpleJavaFileObject {
        private final String content;

        StringSource(String unitName, String content) {
            super(URI.create("string:///" + unitName + ".java"), Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }
}