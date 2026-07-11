package dev.lumina.ui;

import dev.lumina.syntax.JavaSyntaxHighlighter;
import javafx.application.Platform;
import javafx.scene.control.Tab;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.nio.file.Path;
import java.time.Duration;

/** One open file: a syntax-highlighted CodeArea inside a closable tab. */
public class EditorTab extends Tab {

    private static final String DIRTY_MARK = "\u25CF ";

    private final CodeArea codeArea = new CodeArea();
    private Path path;
    private String baseName;
    private boolean dirty;

    @FunctionalInterface
    public interface CaretListener {
        void caretMoved(int line, int column);
    }

    private CaretListener caretListener;
    private java.util.function.Function<String,
            org.fxmisc.richtext.model.StyleSpans<java.util.Collection<String>>> highlighter;

    private static java.util.function.Function<String,
            org.fxmisc.richtext.model.StyleSpans<java.util.Collection<String>>>
    highlighterFor(String fileName) {
        String n = fileName.toLowerCase();
        if (n.endsWith(".java")) return JavaSyntaxHighlighter::computeHighlighting;
        if (n.endsWith(".xml") || n.endsWith(".pom")
                || n.endsWith(".xsd") || n.endsWith(".html")
                || n.endsWith(".fxml")) {
            return dev.lumina.syntax.XmlSyntaxHighlighter::computeHighlighting;
        }
        return null;
    }

    public EditorTab(String name, Path path) {
        this.baseName = name;
        this.path = path;
        setText(name);

        codeArea.getStyleClass().add("code-area");
        refreshGutter();

        // Ctrl/Cmd + hover -> hand cursor, hinting go-to-declaration.
        // Ctrl/Cmd + hover: hand cursor AND underline the identifier, exactly
        // like IntelliJ's navigation hint.
        codeArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, e -> {
            boolean nav = e.isControlDown() || e.isMetaDown();
            codeArea.setCursor(nav ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.TEXT);
            if (nav) {
                var hit = codeArea.hit(e.getX(), e.getY());
                underlineWordAt(hit.getInsertionIndex());
            } else {
                clearNavUnderline();
            }
        });
        codeArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_EXITED,
                e -> clearNavUnderline());
        // Dropping the modifier key removes the underline.
        codeArea.addEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.CONTROL
                    || e.getCode() == javafx.scene.input.KeyCode.META) {
                clearNavUnderline();
                codeArea.setCursor(javafx.scene.Cursor.TEXT);
            }
        });

        // Re-highlight after brief pauses in typing, per file type.
        java.util.function.Function<String,
                org.fxmisc.richtext.model.StyleSpans<java.util.Collection<String>>> highlighter =
                highlighterFor(name);
        if (highlighter != null) {
            this.highlighter = highlighter;
            codeArea.multiPlainChanges()
                    .successionEnds(Duration.ofMillis(120))
                    .subscribe(ignore -> applyHighlighting());
        }

        codeArea.textProperty().addListener((obs, old, txt) -> markDirty());
        codeArea.caretPositionProperty().addListener((obs, old, pos) -> {
            notifyCaret();
            highlightCurrentLine();
        });

        // Auto-indent: keep leading whitespace of the previous line on Enter.
        codeArea.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                int paragraph = codeArea.getCurrentParagraph();
                String line = codeArea.getParagraph(paragraph).getText();
                String indent = line.replaceAll("\\S.*$", "");
                Platform.runLater(() -> codeArea.insertText(codeArea.getCaretPosition(), indent));
            } else if (e.getCode() == javafx.scene.input.KeyCode.TAB) {
                e.consume();
                codeArea.insertText(codeArea.getCaretPosition(), "    ");
            }
        });

        // Ctrl/Cmd+Click on an identifier -> go to its declaration.
        codeArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if ((e.isControlDown() || e.isMetaDown()) && navigationHandler != null) {
                var hit = codeArea.hit(e.getX(), e.getY());
                String word = wordAt(hit.getInsertionIndex());
                if (word != null) {
                    e.consume();
                    navigationHandler.accept(word);
                }
            }
        });

        setContent(new VirtualizedScrollPane<>(codeArea));
    }

    // ---------------------------------------------------------------- blame

    private java.util.List<dev.lumina.git.GitService.BlameLine> blameLines;

    public boolean hasBlame() {
        return blameLines != null;
    }

    /** Show (or clear, with null) git blame annotations in the gutter. */
    public void setBlame(java.util.List<dev.lumina.git.GitService.BlameLine> lines) {
        this.blameLines = lines;
        refreshGutter();
    }

    private void refreshGutter() {
        java.util.function.IntFunction<javafx.scene.Node> lineNo =
                LineNumberFactory.get(codeArea);
        if (blameLines == null) {
            codeArea.setParagraphGraphicFactory(lineNo);
            return;
        }
        codeArea.setParagraphGraphicFactory(i -> {
            javafx.scene.control.Label annotation = new javafx.scene.control.Label(
                    i < blameLines.size() ? blameLines.get(i).gutter() : "");
            annotation.getStyleClass().add("blame-label");
            annotation.setPrefWidth(150);
            if (i < blameLines.size() && !blameLines.get(i).summary().isBlank()) {
                javafx.scene.control.Tooltip.install(annotation,
                        new javafx.scene.control.Tooltip(blameLines.get(i).summary()));
            }
            javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(
                    6, annotation, lineNo.apply(i));
            box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            return box;
        });
    }

    // ------------------------------------------------------------ navigation

    private java.util.function.Consumer<String> navigationHandler;

    public void setNavigationHandler(java.util.function.Consumer<String> handler) {
        this.navigationHandler = handler;
    }

    /** Identifier under the caret, or null. */
    public String wordAtCaret() {
        return wordAt(codeArea.getCaretPosition());
    }

    /** Text of the line the caret is on. */
    public String currentLineText() {
        return codeArea.getParagraph(codeArea.getCurrentParagraph()).getText();
    }

    private int navFrom = -1, navTo = -1;

    /** Underline the identifier spanning the given index (Ctrl+hover hint). */
    private void underlineWordAt(int index) {
        String text = codeArea.getText();
        if (text.isEmpty()) { clearNavUnderline(); return; }
        int i = Math.max(0, Math.min(index, text.length() - 1));
        if (!isWordChar(text.charAt(i)) && i > 0 && isWordChar(text.charAt(i - 1))) i--;
        if (!isWordChar(text.charAt(i))) { clearNavUnderline(); return; }
        int start = i, end = i;
        while (start > 0 && isWordChar(text.charAt(start - 1))) start--;
        while (end < text.length() && isWordChar(text.charAt(end))) end++;
        if (start == navFrom && end == navTo) return;   // already underlined
        clearNavUnderline();
        navFrom = start; navTo = end;
        codeArea.setStyleClass(start, end, "nav-underline");
    }

    private void clearNavUnderline() {
        if (navFrom >= 0 && navTo > navFrom && navTo <= codeArea.getLength()) {
            // Re-run the highlighter so the correct token color is restored.
            if (highlighter != null) {
                applyHighlighting();
            } else {
                codeArea.clearStyle(navFrom, navTo);
            }
        }
        navFrom = navTo = -1;
    }

    private String wordAt(int index) {
        String text = codeArea.getText();
        if (text.isEmpty()) return null;
        int i = Math.max(0, Math.min(index, text.length() - 1));
        if (!isWordChar(text.charAt(i)) && i > 0 && isWordChar(text.charAt(i - 1))) i--;
        if (!isWordChar(text.charAt(i))) return null;
        int start = i, end = i;
        while (start > 0 && isWordChar(text.charAt(start - 1))) start--;
        while (end < text.length() && isWordChar(text.charAt(end))) end++;
        String word = text.substring(start, end);
        return word.isBlank() ? null : word;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    // ---------------------------------------------------------------- state

    public void setEditorText(String text) {
        codeArea.replaceText(text);
        codeArea.getUndoManager().forgetHistory();
        codeArea.moveTo(0);
        codeArea.requestFollowCaret();
        dirty = false;
        setText(baseName);
        applyHighlighting();
    }

    public String getEditorText() {
        return codeArea.getText();
    }

    public Path getPath() {
        return path;
    }

    public String getDisplayPath() {
        return path != null ? path.toAbsolutePath().toString() : baseName;
    }

    private void markDirty() {
        if (!dirty) {
            dirty = true;
            setText(DIRTY_MARK + baseName);
        }
    }

    public void markSaved(Path savedTo) {
        this.path = savedTo;
        this.baseName = savedTo.getFileName().toString();
        dirty = false;
        setText(baseName);
    }

    private void applyHighlighting() {
        if (highlighter == null) return;
        codeArea.setStyleSpans(0, highlighter.apply(codeArea.getText()));
    }

    // ---------------------------------------------------------------- caret

    public void setCaretListener(CaretListener listener) {
        this.caretListener = listener;
        notifyCaret();
    }

    private int currentHighlightedLine = -1;

    /** Tint the caret's paragraph as the current line (IntelliJ-style). */
    private void highlightCurrentLine() {
        int line = codeArea.getCurrentParagraph();
        if (line == currentHighlightedLine) return;
        if (currentHighlightedLine >= 0
                && currentHighlightedLine < codeArea.getParagraphs().size()) {
            codeArea.setParagraphStyle(currentHighlightedLine,
                    java.util.Collections.emptyList());
        }
        if (line >= 0 && line < codeArea.getParagraphs().size()) {
            codeArea.setParagraphStyle(line, java.util.List.of("has-caret"));
        }
        currentHighlightedLine = line;
    }

    private void notifyCaret() {
        if (caretListener != null) {
            caretListener.caretMoved(codeArea.getCurrentParagraph() + 1,
                    codeArea.getCaretColumn() + 1);
        }
    }

    public void focusEditor() {
        Platform.runLater(codeArea::requestFocus);
    }

    // ------------------------------------------------------------ edit menu

    public void undo() { codeArea.undo(); }
    public void redo() { codeArea.redo(); }
    public void cut() { codeArea.cut(); }
    public void copy() { codeArea.copy(); }
    public void paste() { codeArea.paste(); }
    public void selectAll() { codeArea.selectAll(); }

    // --------------------------------------------------------- extra tools

    /** Used for decompiled .class views. */
    public void setReadOnly() {
        codeArea.setEditable(false);
    }

    public void goToLine(int line) {
        int target = Math.max(0, Math.min(line - 1, codeArea.getParagraphs().size() - 1));
        codeArea.moveTo(target, 0);
        codeArea.requestFollowCaret();
        focusEditor();
    }

    /** Toggle // on the selected lines (or the caret line). */
    public void toggleComment() {
        if (!codeArea.isEditable()) return;
        int start = codeArea.offsetToPosition(
                        codeArea.getSelection().getStart(), org.fxmisc.richtext.model.TwoDimensional.Bias.Forward)
                .getMajor();
        int end = codeArea.offsetToPosition(
                        codeArea.getSelection().getEnd(), org.fxmisc.richtext.model.TwoDimensional.Bias.Backward)
                .getMajor();

        boolean allCommented = true;
        for (int i = start; i <= end; i++) {
            String text = codeArea.getParagraph(i).getText();
            if (!text.isBlank() && !text.stripLeading().startsWith("//")) {
                allCommented = false;
                break;
            }
        }
        for (int i = start; i <= end; i++) {
            String text = codeArea.getParagraph(i).getText();
            if (text.isBlank()) continue;
            if (allCommented) {
                int idx = text.indexOf("//");
                int removeEnd = idx + 2;
                if (removeEnd < text.length() && text.charAt(removeEnd) == ' ') removeEnd++;
                codeArea.replaceText(i, idx, i, removeEnd, "");
            } else {
                codeArea.insertText(i, 0, "// ");
            }
        }
    }
}