package dev.lumina.diagnostics;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspections for JPA entity rules matching IntelliJ IDEA:
 * - Persistent entity should have primary key (@Id / @EmbeddedId).
 */
public final class JpaDiagnostics {

    private static final Pattern ENTITY_ANN = Pattern.compile("@(?:jakarta\\.persistence\\.|javax\\.persistence\\.)?Entity\\b");
    private static final Pattern CLASS_DECL = Pattern.compile("(?:public\\s+)?class\\s+([A-Za-z0-9_]+)");
    private static final Pattern ID_ANN = Pattern.compile("@(?:jakarta\\.persistence\\.|javax\\.persistence\\.)?(?:Id|EmbeddedId)\\b");

    private JpaDiagnostics() {}

    /**
     * Inspects Java source code for persistent entity violations.
     * When an @Entity class does not declare an @Id or @EmbeddedId attribute,
     * emits an ERROR diagnostic on the class identifier with an "Add Id attribute" quick-fix.
     */
    public static List<JavaDiagnostics.Diag> checkPersistentEntities(Path file, String text, String moduleName) {
        if (text == null || !ENTITY_ANN.matcher(text).find()) {
            return List.of();
        }

        List<JavaDiagnostics.Diag> diags = new ArrayList<>();
        boolean hasId = ID_ANN.matcher(text).find();

        if (!hasId) {
            Matcher classMatcher = CLASS_DECL.matcher(text);
            if (classMatcher.find()) {
                String className = classMatcher.group(1);
                int start = classMatcher.start(1);
                int end = classMatcher.end(1);

                int line = 1;
                for (int i = 0; i < start; i++) {
                    if (text.charAt(i) == '\n') line++;
                }

                String title = "Persistent entity '" + className + "' should have primary key";
                String message = "Persistent entity should have primary key";
                String quickFix = "add-id-attribute:" + className;

                diags.add(new JavaDiagnostics.Diag(
                        JavaDiagnostics.Severity.ERROR,
                        line,
                        start,
                        end,
                        message,
                        quickFix,
                        title,
                        null,
                        null,
                        null,
                        moduleName
                ));
            }
        }
        return diags;
    }
}
