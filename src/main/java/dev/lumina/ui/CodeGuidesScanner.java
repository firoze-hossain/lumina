package dev.lumina.ui;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovers method boundaries and block scopes for IntelliJ IDEA-parity:
 * - Method separator lines (above each method / constructor declaration)
 * - Code block indent guides (vertical lines spanning { ... } scopes)
 * - Active block scope resolution based on caret position
 */
public final class CodeGuidesScanner {

    public record BlockScope(
            int startLine,      // 0-based line index of opening statement
            int endLine,        // 0-based line index of closing brace
            int indentColumn,   // column offset (e.g. 0, 4, 8, 12 spaces)
            boolean isMethod,   // true if this block is a method/constructor
            String name         // identifier or block kind (e.g. "previewGetter", "if", "else")
    ) {}

    public record GuidesResult(
            List<Integer> methodSeparatorLines,
            List<BlockScope> blockScopes
    ) {
        public static final GuidesResult EMPTY = new GuidesResult(List.of(), List.of());
    }

    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "^[ \\t]*(?:(?:public|protected|private|static|final|native|synchronized|abstract|default)\\s+)*(?:<[^>]+>\\s+)?(?:[A-Za-z0-9_<>\\[\\]?,\\s]+)\\s+([a-zA-Z0-9_]+)\\s*\\([^;{}]*\\)\\s*(?:throws\s+[^{]+)?\\s*\\{?\\s*$"
    );

    private static final Pattern CONTROL_PATTERN = Pattern.compile(
            "^[ \\t]*(if|else\\s+if|else|for|while|do|switch|try|catch|finally|synchronized)\\b"
    );

    private CodeGuidesScanner() {}

    /**
     * Scans source text to detect all method separators and block scopes.
     * Uses AST parser when possible (e.g. Java), with robust brace-scanning fallback.
     */
    public static GuidesResult scan(String text, String fileName) {
        if (text == null || text.isBlank()) {
            return GuidesResult.EMPTY;
        }

        if (fileName != null && fileName.endsWith(".java")) {
            GuidesResult astResult = scanJavaAst(text);
            if (astResult != null && (!astResult.methodSeparatorLines().isEmpty() || !astResult.blockScopes().isEmpty())) {
                return astResult;
            }
        }

        return scanBracesFallback(text);
    }

    /**
     * Returns the innermost BlockScope enclosing the specified 0-based caret line.
     */
    public static BlockScope findActiveScope(List<BlockScope> scopes, int caretLine) {
        if (scopes == null || scopes.isEmpty() || caretLine < 0) {
            return null;
        }

        BlockScope best = null;
        for (BlockScope s : scopes) {
            if (s.startLine() <= caretLine && caretLine <= s.endLine()) {
                if (best == null) {
                    best = s;
                } else {
                    int bestSpan = best.endLine() - best.startLine();
                    int currentSpan = s.endLine() - s.startLine();
                    if (currentSpan < bestSpan || (currentSpan == bestSpan && s.indentColumn() > best.indentColumn())) {
                        best = s;
                    }
                }
            }
        }
        return best;
    }

    private static GuidesResult scanJavaAst(String text) {
        try {
            ParserConfiguration config = new ParserConfiguration();
            config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
            JavaParser parser = new JavaParser(config);
            ParseResult<CompilationUnit> pr = parser.parse(text);

            if (!pr.getResult().isPresent()) {
                return null;
            }

            CompilationUnit cu = pr.getResult().get();
            String[] lines = text.split("\\R", -1);

            Set<Integer> methodLines = new LinkedHashSet<>();
            List<BlockScope> scopes = new ArrayList<>();

            // 1. Classes
            for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (cls.getRange().isPresent()) {
                    var r = cls.getRange().get();
                    int sLine = r.begin.line - 1;
                    int eLine = r.end.line - 1;
                    if (eLine > sLine) {
                        int col = computeLeadingIndent(lines, sLine);
                        scopes.add(new BlockScope(sLine, eLine, col, false, cls.getNameAsString()));
                    }
                }
            }

            // 2. Constructors
            for (ConstructorDeclaration ctor : cu.findAll(ConstructorDeclaration.class)) {
                if (ctor.getRange().isPresent()) {
                    var r = ctor.getRange().get();
                    int sLine = r.begin.line - 1;
                    int eLine = r.end.line - 1;
                    int sepLine = sLine;
                    if (!ctor.getAnnotations().isEmpty() && ctor.getAnnotations().get(0).getRange().isPresent()) {
                        sepLine = ctor.getAnnotations().get(0).getRange().get().begin.line - 1;
                    }
                    if (ctor.getComment().isPresent() && ctor.getComment().get().getRange().isPresent()) {
                        int cLine = ctor.getComment().get().getRange().get().begin.line - 1;
                        if (cLine < sepLine) sepLine = cLine;
                    }
                    methodLines.add(sepLine);
                    if (ctor.getBody() != null && ctor.getBody().getRange().isPresent()) {
                        var br = ctor.getBody().getRange().get();
                        int bStart = br.begin.line - 1;
                        int bEnd = br.end.line - 1;
                        if (bEnd > bStart) {
                            int col = computeLeadingIndent(lines, bStart);
                            scopes.add(new BlockScope(bStart, bEnd, col, true, ctor.getNameAsString()));
                        }
                    } else if (eLine > sLine) {
                        int col = computeLeadingIndent(lines, sLine);
                        scopes.add(new BlockScope(sLine, eLine, col, true, ctor.getNameAsString()));
                    }
                }
            }

            // 3. Methods
            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                if (method.getRange().isPresent()) {
                    var r = method.getRange().get();
                    int sLine = r.begin.line - 1;
                    int eLine = r.end.line - 1;
                    int sepLine = sLine;
                    if (!method.getAnnotations().isEmpty() && method.getAnnotations().get(0).getRange().isPresent()) {
                        sepLine = method.getAnnotations().get(0).getRange().get().begin.line - 1;
                    }
                    if (method.getComment().isPresent() && method.getComment().get().getRange().isPresent()) {
                        int cLine = method.getComment().get().getRange().get().begin.line - 1;
                        if (cLine < sepLine) sepLine = cLine;
                    }
                    methodLines.add(sepLine);
                    if (method.getBody().isPresent() && method.getBody().get().getRange().isPresent()) {
                        var br = method.getBody().get().getRange().get();
                        int bStart = br.begin.line - 1;
                        int bEnd = br.end.line - 1;
                        if (bEnd > bStart) {
                            int col = computeLeadingIndent(lines, bStart);
                            scopes.add(new BlockScope(bStart, bEnd, col, true, method.getNameAsString()));
                        }
                    } else if (eLine > sLine) {
                        int col = computeLeadingIndent(lines, sLine);
                        scopes.add(new BlockScope(sLine, eLine, col, true, method.getNameAsString()));
                    }
                }
            }

            // 4. Control Blocks (if, else, for, while, try, catch, etc.)
            for (IfStmt stmt : cu.findAll(IfStmt.class)) {
                if (stmt.getThenStmt() instanceof BlockStmt block) {
                    addBlockScope(lines, stmt, block, "if", scopes);
                }
                if (stmt.getElseStmt().isPresent() && stmt.getElseStmt().get() instanceof BlockStmt block) {
                    addBlockScope(lines, stmt.getElseStmt().get(), block, "else", scopes);
                }
            }

            for (ForStmt stmt : cu.findAll(ForStmt.class)) {
                if (stmt.getBody() instanceof BlockStmt block) {
                    addBlockScope(lines, stmt, block, "for", scopes);
                }
            }

            for (ForEachStmt stmt : cu.findAll(ForEachStmt.class)) {
                if (stmt.getBody() instanceof BlockStmt block) {
                    addBlockScope(lines, stmt, block, "foreach", scopes);
                }
            }

            for (WhileStmt stmt : cu.findAll(WhileStmt.class)) {
                if (stmt.getBody() instanceof BlockStmt block) {
                    addBlockScope(lines, stmt, block, "while", scopes);
                }
            }

            for (DoStmt stmt : cu.findAll(DoStmt.class)) {
                if (stmt.getBody() instanceof BlockStmt block) {
                    addBlockScope(lines, stmt, block, "do", scopes);
                }
            }

            for (TryStmt stmt : cu.findAll(TryStmt.class)) {
                addBlockScope(lines, stmt, stmt.getTryBlock(), "try", scopes);
                for (CatchClause cc : stmt.getCatchClauses()) {
                    addBlockScope(lines, cc, cc.getBody(), "catch", scopes);
                }
                stmt.getFinallyBlock().ifPresent(fb -> addBlockScope(lines, stmt, fb, "finally", scopes));
            }

            for (SwitchStmt stmt : cu.findAll(SwitchStmt.class)) {
                if (stmt.getRange().isPresent()) {
                    var r = stmt.getRange().get();
                    int sLine = r.begin.line - 1;
                    int eLine = r.end.line - 1;
                    if (eLine > sLine) {
                        int col = computeLeadingIndent(lines, sLine);
                        scopes.add(new BlockScope(sLine, eLine, col, false, "switch"));
                    }
                }
            }

            for (SynchronizedStmt stmt : cu.findAll(SynchronizedStmt.class)) {
                addBlockScope(lines, stmt, stmt.getBody(), "synchronized", scopes);
            }

            List<Integer> sortedMethods = new ArrayList<>(methodLines);
            Collections.sort(sortedMethods);

            scopes.sort(Comparator.comparingInt(BlockScope::startLine)
                    .thenComparingInt(s -> s.endLine() - s.startLine()));

            return new GuidesResult(sortedMethods, scopes);

        } catch (Exception e) {
            return null;
        }
    }

    private static void addBlockScope(String[] lines, com.github.javaparser.ast.Node headerNode, BlockStmt block, String label, List<BlockScope> out) {
        if (block == null || !block.getRange().isPresent()) return;
        var r = block.getRange().get();
        int bStart = r.begin.line - 1;
        int bEnd = r.end.line - 1;

        int headerLine = bStart;
        if (headerNode != null && headerNode.getRange().isPresent()) {
            headerLine = headerNode.getRange().get().begin.line - 1;
        }

        if (bEnd > bStart) {
            int col = computeLeadingIndent(lines, headerLine);
            out.add(new BlockScope(bStart, bEnd, col, false, label));
        }
    }

    /**
     * Resilient scanner that parses balanced braces and indentation columns.
     */
    private static GuidesResult scanBracesFallback(String text) {
        String[] lines = text.split("\\R", -1);
        List<Integer> methodLines = new ArrayList<>();
        List<BlockScope> scopes = new ArrayList<>();

        record OpenBrace(int line, int col, boolean isMethod, String name) {}
        Deque<OpenBrace> stack = new ArrayDeque<>();

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            String trimmed = rawLine.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("*")) {
                continue;
            }

            int indentCol = computeLeadingIndent(lines, i);
            boolean isMethodLine = isLikelyMethod(trimmed);

            // Strip strings and comments before counting braces
            String code = stripStringsAndComments(rawLine);

            for (int c = 0; c < code.length(); c++) {
                char ch = code.charAt(c);
                if (ch == '{') {
                    String blockName = isMethodLine ? "method" : getControlKeyword(trimmed);
                    stack.push(new OpenBrace(i, indentCol, isMethodLine, blockName));
                    if (isMethodLine) {
                        methodLines.add(i);
                    }
                } else if (ch == '}') {
                    if (!stack.isEmpty()) {
                        OpenBrace ob = stack.pop();
                        if (i > ob.line) {
                            scopes.add(new BlockScope(ob.line, i, ob.col, ob.isMethod, ob.name));
                        }
                    }
                }
            }
        }

        Collections.sort(methodLines);
        scopes.sort(Comparator.comparingInt(BlockScope::startLine));
        return new GuidesResult(methodLines, scopes);
    }

    public static int computeLeadingIndent(String[] lines, int lineIdx) {
        if (lineIdx < 0 || lineIdx >= lines.length) return 0;
        String line = lines[lineIdx];
        int spaces = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') {
                spaces++;
            } else if (c == '\t') {
                spaces += 4; // 4 spaces per tab stop
            } else {
                break;
            }
        }
        return spaces;
    }

    private static boolean isLikelyMethod(String trimmed) {
        if (CONTROL_PATTERN.matcher(trimmed).find()) {
            return false;
        }
        return METHOD_PATTERN.matcher(trimmed).find();
    }

    private static String getControlKeyword(String trimmed) {
        Matcher m = CONTROL_PATTERN.matcher(trimmed);
        if (m.find()) {
            return m.group(1);
        }
        return "block";
    }

    private static String stripStringsAndComments(String line) {
        StringBuilder sb = new StringBuilder(line.length());
        boolean inStr = false;
        boolean inChar = false;
        char prev = 0;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/' && !inStr && !inChar) {
                break; // single line comment
            }
            if (c == '"' && prev != '\\' && !inChar) {
                inStr = !inStr;
                sb.append(' ');
                prev = c;
                continue;
            }
            if (c == '\'' && prev != '\\' && !inStr) {
                inChar = !inChar;
                sb.append(' ');
                prev = c;
                continue;
            }
            if (inStr || inChar) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
            prev = c;
        }
        return sb.toString();
    }
}
