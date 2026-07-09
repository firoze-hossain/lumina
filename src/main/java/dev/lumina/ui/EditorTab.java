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

    public EditorTab(String name, Path path) {
        this.baseName = name;
        this.path = path;
        setText(name);

        codeArea.getStyleClass().add("code-area");
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        // Re-highlight after brief pauses in typing (only for .java files).
        if (isJava()) {
            codeArea.multiPlainChanges()
                    .successionEnds(Duration.ofMillis(120))
                    .subscribe(ignore -> applyHighlighting());
        }

        codeArea.textProperty().addListener((obs, old, txt) -> markDirty());
        codeArea.caretPositionProperty().addListener((obs, old, pos) -> notifyCaret());

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

        setContent(new VirtualizedScrollPane<>(codeArea));
    }

    private boolean isJava() {
        return baseName.endsWith(".java");
    }

    // ---------------------------------------------------------------- state

    public void setEditorText(String text) {
        codeArea.replaceText(text);
        codeArea.getUndoManager().forgetHistory();
        codeArea.moveTo(0);
        codeArea.requestFollowCaret();
        dirty = false;
        setText(baseName);
        if (isJava()) applyHighlighting();
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
        codeArea.setStyleSpans(0, JavaSyntaxHighlighter.computeHighlighting(codeArea.getText()));
    }

    // ---------------------------------------------------------------- caret

    public void setCaretListener(CaretListener listener) {
        this.caretListener = listener;
        notifyCaret();
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
