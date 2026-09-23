package dev.lumina.folding;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dynamically scans source code to identify all foldable code regions matching IntelliJ IDEA:
 * <ul>
 *   <li>Imports blocks (collapsed into "..." / import [...])</li>
 *   <li>Annotations blocks (collapsed into "@{...}")</li>
 *   <li>Method and constructor bodies (collapsed into "{...}")</li>
 *   <li>Class, interface, record, and enum bodies (collapsed into "{...}")</li>
 *   <li>Doc comments and multi-line comments (collapsed into "/**...*​/")</li>
 *   <li>Custom folding regions (//&lt;editor-fold&gt; or //#region)</li>
 * </ul>
 */
public class CodeFoldingScanner {

    private static final Pattern EDITOR_FOLD_START = Pattern.compile("^\\s*//\\s*<editor-fold(?:\\s+desc=[\"'](.*?)[\"'])?.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern EDITOR_FOLD_END = Pattern.compile("^\\s*//\\s*</editor-fold>", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGION_START = Pattern.compile("^\\s*//#region(?:\\s+(.*))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGION_END = Pattern.compile("^\\s*//#endregion", Pattern.CASE_INSENSITIVE);

    public static List<FoldRegion> scan(String text, String fileName) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<FoldRegion> regions = new ArrayList<>();
        String[] lines = text.split("\r?\n", -1);
        int totalLines = lines.length;

        CodeFoldingSettings settings = CodeFoldingSettings.getInstance();

        // 1. Scan Comments and Custom Regions
        scanCommentsAndCustomRegions(lines, regions, settings);

        // 2. Scan Import Statements
        scanImports(lines, regions, settings);

        // 3. Scan Annotations
        scanAnnotations(lines, regions, settings);

        // 4. Scan Braced Code Blocks (Classes, Methods, Initializers)
        scanBracedBlocks(lines, regions, settings);

        // Sort outer blocks before inner blocks, and by startLine ascending
        Collections.sort(regions);
        return regions;
    }

    private static void scanCommentsAndCustomRegions(String[] lines, List<FoldRegion> regions, CodeFoldingSettings settings) {
        int inCommentStart = -1;
        boolean isJavadoc = false;

        Deque<CustomRegionEntry> customRegionStack = new ArrayDeque<>();

        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            String trimmed = lines[i].trim();

            // Custom editor-fold / region
            Matcher mFoldStart = EDITOR_FOLD_START.matcher(lines[i]);
            if (mFoldStart.matches()) {
                String desc = mFoldStart.group(1);
                customRegionStack.push(new CustomRegionEntry(lineNum, desc != null && !desc.isBlank() ? desc : "..."));
                continue;
            }
            if (EDITOR_FOLD_END.matcher(lines[i]).matches() && !customRegionStack.isEmpty()) {
                CustomRegionEntry entry = customRegionStack.pop();
                regions.add(new FoldRegion(FoldRegion.RegionType.CUSTOM, entry.lineNum, lineNum, entry.desc, lines[entry.lineNum - 1], settings.isFoldCustomRegionsByDefault()));
                continue;
            }

            Matcher mRegionStart = REGION_START.matcher(lines[i]);
            if (mRegionStart.matches()) {
                String desc = mRegionStart.group(1);
                customRegionStack.push(new CustomRegionEntry(lineNum, desc != null && !desc.isBlank() ? desc : "..."));
                continue;
            }
            if (REGION_END.matcher(lines[i]).matches() && !customRegionStack.isEmpty()) {
                CustomRegionEntry entry = customRegionStack.pop();
                regions.add(new FoldRegion(FoldRegion.RegionType.CUSTOM, entry.lineNum, lineNum, entry.desc, lines[entry.lineNum - 1], settings.isFoldCustomRegionsByDefault()));
                continue;
            }

            // Block comments
            if (inCommentStart == -1) {
                if (trimmed.startsWith("/*")) {
                    inCommentStart = lineNum;
                    isJavadoc = trimmed.startsWith("/**");
                    if (trimmed.endsWith("*/") && trimmed.length() > 2) {
                        // Single line comment, don't fold
                        inCommentStart = -1;
                    }
                }
            } else {
                if (trimmed.contains("*/")) {
                    if (lineNum > inCommentStart) {
                        String placeholder = isJavadoc ? "/**...*/" : "/*...*/";
                        regions.add(new FoldRegion(FoldRegion.RegionType.COMMENT, inCommentStart, lineNum, placeholder, lines[inCommentStart - 1], settings.isFoldDocCommentsByDefault()));
                    }
                    inCommentStart = -1;
                }
            }
        }
    }

    private record CustomRegionEntry(int lineNum, String desc) {}

    private static void scanImports(String[] lines, List<FoldRegion> regions, CodeFoldingSettings settings) {
        int firstImportLine = -1;
        int lastImportLine = -1;
        int importCount = 0;

        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            String trimmed = lines[i].trim();

            if (trimmed.startsWith("import ") || (trimmed.startsWith("import\t"))) {
                if (firstImportLine == -1) {
                    firstImportLine = lineNum;
                }
                lastImportLine = lineNum;
                importCount++;
            } else if (firstImportLine != -1) {
                // If blank line between imports, allow it to continue
                if (trimmed.isEmpty()) {
                    continue;
                }
                // Non-import and non-blank line encountered: finalize imports block
                if (importCount >= 2 || (lastImportLine > firstImportLine)) {
                    regions.add(new FoldRegion(FoldRegion.RegionType.IMPORTS, firstImportLine, lastImportLine, "...", lines[firstImportLine - 1], settings.isFoldImportsByDefault()));
                }
                firstImportLine = -1;
                lastImportLine = -1;
                importCount = 0;
            }
        }

        // End of file edge case
        if (firstImportLine != -1 && (importCount >= 2 || lastImportLine > firstImportLine)) {
            regions.add(new FoldRegion(FoldRegion.RegionType.IMPORTS, firstImportLine, lastImportLine, "...", lines[firstImportLine - 1], settings.isFoldImportsByDefault()));
        }
    }

    private static void scanAnnotations(String[] lines, List<FoldRegion> regions, CodeFoldingSettings settings) {
        int firstAnnotationLine = -1;
        int lastAnnotationLine = -1;
        int annotationCount = 0;

        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            String trimmed = lines[i].trim();

            // Match annotation: starts with @ followed by identifier
            if (trimmed.startsWith("@") && trimmed.length() > 1 && Character.isJavaIdentifierStart(trimmed.charAt(1))) {
                if (firstAnnotationLine == -1) {
                    firstAnnotationLine = lineNum;
                }
                lastAnnotationLine = lineNum;
                annotationCount++;
            } else {
                if (firstAnnotationLine != -1) {
                    // Check if consecutive annotations span 2 or more lines
                    if (annotationCount >= 2 || (lastAnnotationLine > firstAnnotationLine)) {
                        regions.add(new FoldRegion(FoldRegion.RegionType.ANNOTATIONS, firstAnnotationLine, lastAnnotationLine, "@{...}", lines[firstAnnotationLine - 1], settings.isFoldAnnotationsByDefault()));
                    }
                    firstAnnotationLine = -1;
                    lastAnnotationLine = -1;
                    annotationCount = 0;
                }
            }
        }

        if (firstAnnotationLine != -1 && (annotationCount >= 2 || lastAnnotationLine > firstAnnotationLine)) {
            regions.add(new FoldRegion(FoldRegion.RegionType.ANNOTATIONS, firstAnnotationLine, lastAnnotationLine, "@{...}", lines[firstAnnotationLine - 1], settings.isFoldAnnotationsByDefault()));
        }
    }

    private static void scanBracedBlocks(String[] lines, List<FoldRegion> regions, CodeFoldingSettings settings) {
        Deque<BraceEntry> braceStack = new ArrayDeque<>();
        boolean inString = false;
        boolean inChar = false;
        boolean inBlockComment = false;

        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            String line = lines[i];

            for (int c = 0; c < line.length(); c++) {
                char ch = line.charAt(c);
                char next = (c + 1 < line.length()) ? line.charAt(c + 1) : '\0';

                // Comment handling
                if (!inString && !inChar) {
                    if (!inBlockComment && ch == '/' && next == '*') {
                        inBlockComment = true;
                        c++;
                        continue;
                    }
                    if (inBlockComment && ch == '*' && next == '/') {
                        inBlockComment = false;
                        c++;
                        continue;
                    }
                    if (!inBlockComment && ch == '/' && next == '/') {
                        // Line comment: skip rest of line
                        break;
                    }
                }

                if (inBlockComment) continue;

                // String literals
                if (!inChar && ch == '"' && (c == 0 || line.charAt(c - 1) != '\\')) {
                    inString = !inString;
                    continue;
                }
                // Char literals
                if (!inString && ch == '\'' && (c == 0 || line.charAt(c - 1) != '\\')) {
                    inChar = !inChar;
                    continue;
                }

                if (inString || inChar) continue;

                if (ch == '{') {
                    // Find header of block (line containing start of declaration)
                    int headerLine = lineNum;
                    String lineTrimmed = line.trim();
                    // If opening brace is alone on the line, look at preceding non-empty line
                    if (lineTrimmed.equals("{") && i > 0) {
                        for (int k = i - 1; k >= 0; k--) {
                            if (!lines[k].trim().isEmpty() && !lines[k].trim().startsWith("@")) {
                                headerLine = k + 1;
                                break;
                            }
                        }
                    }

                    // Classify block type
                    FoldRegion.RegionType type = determineBlockType(lines, headerLine - 1);
                    braceStack.push(new BraceEntry(lineNum, headerLine, type));
                } else if (ch == '}') {
                    if (!braceStack.isEmpty()) {
                        BraceEntry open = braceStack.pop();
                        if (lineNum > open.braceLine) {
                            boolean defaultFold = switch (open.type) {
                                case METHOD -> settings.isFoldMethodBodiesByDefault();
                                case CLASS -> false;
                                default -> false;
                            };
                            regions.add(new FoldRegion(open.type, open.braceLine, lineNum, "{...}", lines[open.braceLine - 1], defaultFold));
                        }
                    }
                }
            }
        }
    }

    private static FoldRegion.RegionType determineBlockType(String[] lines, int lineIdx) {
        if (lineIdx < 0 || lineIdx >= lines.length) return FoldRegion.RegionType.METHOD;
        String line = lines[lineIdx];

        if (line.contains("class ") || line.contains("interface ") || line.contains("enum ") || line.contains("record ")) {
            return FoldRegion.RegionType.CLASS;
        }
        return FoldRegion.RegionType.METHOD;
    }

    private record BraceEntry(int braceLine, int headerLine, FoldRegion.RegionType type) {}
}
