package dev.lumina.ui;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.fxmisc.richtext.CodeArea;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hardware-accelerated transparent canvas overlay for IntelliJ IDEA-style guide lines:
 * - Method separator lines (thin horizontal line above each method)
 * - Code block indent guides (thin vertical lines for class/method/control scopes)
 * - Active block guide highlighting
 */
public class CodeGuidesOverlay extends Canvas {

    private final CodeArea codeArea;
    private boolean showMethodSeparators = false; // Disabled by default matching IntelliJ IDEA
    private boolean showIndentGuides = true;

    private List<Integer> methodSeparatorLines = new ArrayList<>();
    private List<CodeGuidesScanner.BlockScope> blockScopes = new ArrayList<>();
    private int activeCaretLine = -1;

    // Cached metrics
    private double cachedCharWidth = 7.8;
    private double cachedLineHeight = 19.0;
    private double cachedTextX0 = 60.0;

    // IntelliJ IDEA dark theme guide line colors
    private static final Color NORMAL_GUIDE_COLOR = Color.rgb(255, 255, 255, 0.08);
    private static final Color ACTIVE_GUIDE_COLOR = Color.rgb(255, 255, 255, 0.22);
    private static final Color METHOD_SEP_COLOR = Color.rgb(255, 255, 255, 0.08);

    public CodeGuidesOverlay(CodeArea codeArea) {
        this.codeArea = codeArea;
        setMouseTransparent(true);

        widthProperty().addListener((obs, oldW, newW) -> render());
        heightProperty().addListener((obs, oldH, newH) -> render());
    }

    public void setShowMethodSeparators(boolean show) {
        this.showMethodSeparators = show;
        render();
    }

    public boolean isShowMethodSeparators() {
        return showMethodSeparators;
    }

    public void setShowIndentGuides(boolean show) {
        this.showIndentGuides = show;
        render();
    }

    public boolean isShowIndentGuides() {
        return showIndentGuides;
    }

    public void updateGuides(CodeGuidesScanner.GuidesResult result, int caretLine) {
        if (result != null) {
            this.methodSeparatorLines = result.methodSeparatorLines() != null
                    ? result.methodSeparatorLines() : List.of();
            this.blockScopes = result.blockScopes() != null
                    ? result.blockScopes() : List.of();
        }
        this.activeCaretLine = caretLine;
        render();
    }

    public void setActiveCaretLine(int caretLine) {
        if (this.activeCaretLine != caretLine) {
            this.activeCaretLine = caretLine;
            render();
        }
    }

    public void render() {
        if (getScene() == null) return;

        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        if (w <= 0 || h <= 0) return;

        gc.clearRect(0, 0, w, h);

        if (!showMethodSeparators && !showIndentGuides) {
            return;
        }

        int totalLines = codeArea.getParagraphs().size();
        if (totalLines == 0) return;

        Map<Integer, Double> lineTopY = new HashMap<>();
        Map<Integer, Double> lineBottomY = new HashMap<>();

        int firstPar = 0;
        int lastPar = totalLines - 1;

        try {
            firstPar = codeArea.firstVisibleParToAllParIndex();
            lastPar = codeArea.lastVisibleParToAllParIndex();
        } catch (Throwable ignored) {
            firstPar = 0;
            lastPar = Math.min(totalLines - 1, 100);
        }

        firstPar = Math.max(0, Math.min(firstPar, totalLines - 1));
        lastPar = Math.max(firstPar, Math.min(lastPar, totalLines - 1));

        int firstVis = -1;
        int lastVis = -1;

        for (int i = firstPar; i <= lastPar; i++) {
            var opt = codeArea.getParagraphBoundsOnScreen(i);
            if (opt.isPresent()) {
                Bounds local = screenToLocal(opt.get());
                lineTopY.put(i, local.getMinY());
                lineBottomY.put(i, local.getMaxY());

                if (firstVis == -1) firstVis = i;
                lastVis = i;

                if (local.getHeight() > 10) {
                    cachedLineHeight = local.getHeight();
                }
            }

            // Sample character 0 to get exact local X of column 0 and char width
            int textLen = codeArea.getParagraph(i).getText().length();
            if (textLen > 0) {
                int start = codeArea.getAbsolutePosition(i, 0);
                if (start + 1 <= codeArea.getLength()) {
                    try {
                        var sc = codeArea.getCharacterBoundsOnScreen(start, start + 1);
                        if (sc.isPresent()) {
                            Bounds localChar = screenToLocal(sc.get());
                            if (localChar.getMinX() > 10) {
                                cachedTextX0 = localChar.getMinX();
                            }
                            double cw = localChar.getWidth();
                            if (cw > 4 && cw < 25) {
                                cachedCharWidth = cw;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        if (firstVis == -1) {
            return;
        }

        // Two-way interpolation for empty or unsampled lines within visible range
        for (int i = firstVis; i <= lastVis; i++) {
            if (!lineTopY.containsKey(i)) {
                if (i > 0 && lineBottomY.containsKey(i - 1)) {
                    double top = lineBottomY.get(i - 1);
                    lineTopY.put(i, top);
                    lineBottomY.put(i, top + cachedLineHeight);
                }
            }
        }
        for (int i = lastVis; i >= firstVis; i--) {
            if (!lineTopY.containsKey(i)) {
                if (i + 1 < totalLines && lineTopY.containsKey(i + 1)) {
                    double bot = lineTopY.get(i + 1);
                    lineTopY.put(i, bot - cachedLineHeight);
                    lineBottomY.put(i, bot);
                }
            }
        }

        double textX0 = cachedTextX0;
        double charW = cachedCharWidth;
        double gutterEdge = Math.max(0, textX0 - 4);

        gc.save();
        // Clip to code text area to prevent drawing inside the line number gutter
        gc.beginPath();
        gc.rect(gutterEdge, 0, w - gutterEdge, h);
        gc.clip();

        // 1. Draw Method Separator Lines (if enabled)
        if (showMethodSeparators && !methodSeparatorLines.isEmpty()) {
            gc.setStroke(METHOD_SEP_COLOR);
            gc.setLineWidth(1.0);

            for (int mLine : methodSeparatorLines) {
                Double top = lineTopY.get(mLine);
                if (top != null && top >= -5 && top <= h + 5) {
                    double y = Math.floor(top) - 0.5;
                    gc.strokeLine(gutterEdge, y, w, y);
                }
            }
        }

        // 2. Draw Code Block Indent Guides
        if (showIndentGuides && !blockScopes.isEmpty()) {
            CodeGuidesScanner.BlockScope activeScope =
                    CodeGuidesScanner.findActiveScope(blockScopes, activeCaretLine);

            for (CodeGuidesScanner.BlockScope scope : blockScopes) {
                int col = scope.indentColumn();
                if (col < 4) continue; // Skip class-level indent 0

                int bodyStart = scope.startLine() + 1;
                int bodyEnd = scope.endLine() - 1;
                if (bodyEnd < bodyStart) continue; // Skip empty or single-line blocks

                int visStart = Math.max(bodyStart, firstVis);
                int visEnd = Math.min(bodyEnd, lastVis);
                if (visEnd < visStart) continue; // Completely off screen

                double guideX = Math.floor(textX0 + col * charW) + 0.5;
                boolean isActive = (scope == activeScope);
                gc.setStroke(isActive ? ACTIVE_GUIDE_COLOR : NORMAL_GUIDE_COLOR);
                gc.setLineWidth(1.0);

                int segStart = -1;
                for (int i = visStart; i <= visEnd; i++) {
                    String pText = codeArea.getParagraph(i).getText();
                    boolean blank = isWhitespaceOnly(pText);
                    boolean shouldDraw = blank || (col < countLeadingSpaces(pText));

                    if (shouldDraw) {
                        if (segStart == -1) {
                            segStart = i;
                        }
                    } else {
                        if (segStart != -1) {
                            Double y1 = lineTopY.get(segStart);
                            Double y2 = lineBottomY.get(i - 1);
                            if (y1 != null && y2 != null) {
                                gc.strokeLine(guideX, Math.max(0, y1), guideX, Math.min(h, y2));
                            }
                            segStart = -1;
                        }
                    }
                }

                if (segStart != -1) {
                    Double y1 = lineTopY.get(segStart);
                    Double y2 = lineBottomY.get(visEnd);
                    if (y1 != null && y2 != null) {
                        gc.strokeLine(guideX, Math.max(0, y1), guideX, Math.min(h, y2));
                    }
                }
            }
        }

        gc.restore();
    }

    private static boolean isWhitespaceOnly(String text) {
        if (text == null || text.isEmpty()) return true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                return false;
            }
        }
        return true;
    }

    private static int countLeadingSpaces(String text) {
        if (text == null) return 0;
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += 4;
            } else {
                break;
            }
        }
        return count;
    }
}
