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
                var hit = codeArea.hit(e.getX(), e.getY());
                updateDiagTooltip(diagAt(hit.getInsertionIndex()));
            }
        });
        codeArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_EXITED,
                e -> {
                    clearNavUnderline();
                    updateDiagTooltip(null);
                });
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

        codeArea.textProperty().addListener((obs, old, txt) -> {
            editGeneration++;
            markDirty();
        });
        codeArea.caretPositionProperty().addListener((obs, old, pos) -> {
            notifyCaret();
            highlightCurrentLine();
        });

        // Completion popup keys, Ctrl+Space trigger, then auto-indent.
        codeArea.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (completionPopup.isShowing()) {
                switch (e.getCode()) {
                    case DOWN -> { completionPopup.moveSelection(1); e.consume(); return; }
                    case UP -> { completionPopup.moveSelection(-1); e.consume(); return; }
                    case ENTER, TAB -> { completionPopup.acceptSelected(); e.consume(); return; }
                    case ESCAPE -> { completionPopup.hide(); e.consume(); return; }
                    case BACK_SPACE -> Platform.runLater(this::refilterCompletion);
                    case LEFT, RIGHT, HOME, END, PAGE_UP, PAGE_DOWN ->
                            completionPopup.hide();
                    default -> { }
                }
            }
            if (e.getCode() == javafx.scene.input.KeyCode.SPACE && e.isControlDown()) {
                e.consume();
                triggerCompletion();
                return;
            }
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

        // '.' auto-triggers member completion; typing refines the open popup.
        codeArea.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, e -> {
            String ch = e.getCharacter();
            if (ch == null || ch.isEmpty()) return;
            char c = ch.charAt(0);
            if (c == '.') {
                Platform.runLater(this::triggerCompletion);
            } else if (c == '(') {
                completionPopup.hide();
                if (paramInfoTrigger != null) {
                    Platform.runLater(paramInfoTrigger);
                }
            } else if (completionPopup.isShowing()) {
                if (Character.isLetterOrDigit(c) || c == '_') {
                    Platform.runLater(this::refilterCompletion);
                } else {
                    completionPopup.hide();
                }
            }
        });
        codeArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                e -> completionPopup.hide());
        codeArea.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) completionPopup.hide();
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

        VirtualizedScrollPane<CodeArea> scroll = new VirtualizedScrollPane<>(codeArea);
        hintOverlay = new javafx.scene.layout.Pane();
        hintOverlay.setPickOnBounds(false);   // only the hint labels catch clicks
        javafx.scene.layout.StackPane stack =
                new javafx.scene.layout.StackPane(scroll, hintOverlay);
        // Recompute inline author positions on scroll / resize / edits.
        codeArea.estimatedScrollYProperty().addListener((o, a, b) -> refreshInlineHints());
        codeArea.estimatedScrollXProperty().addListener((o, a, b) -> refreshInlineHints());
        codeArea.widthProperty().addListener((o, a, b) -> refreshInlineHints());
        codeArea.heightProperty().addListener((o, a, b) -> refreshInlineHints());
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(150))
                .subscribe(ignore -> refreshInlineHints());
        setContent(stack);
    }

    // ----------------------------------------------------------- breakpoints

    private final java.util.Set<Integer> breakpoints = new java.util.TreeSet<>();
    private Runnable onBreakpointsChanged;

    /** 1-based line numbers with an active breakpoint. */
    public java.util.Set<Integer> getBreakpoints() {
        return java.util.Set.copyOf(breakpoints);
    }

    public void setOnBreakpointsChanged(Runnable handler) {
        this.onBreakpointsChanged = handler;
    }

    private void toggleBreakpoint(int line) {
        if (!breakpoints.remove(line)) breakpoints.add(line);
        refreshGutter();                       // repaint dots
        if (onBreakpointsChanged != null) onBreakpointsChanged.run();
    }

    // ---------------------------------------------------------------- blame

    private java.util.List<dev.lumina.git.GitService.BlameLine> blameLines;
    private boolean fullBlame;   // true = date+author on every line in the gutter
    private javafx.scene.layout.Pane hintOverlay;   // holds inline author labels

    public boolean hasBlame() {
        return fullBlame;
    }

    /** Show full per-line blame (date + author on every line), or clear (null). */
    public void setBlame(java.util.List<dev.lumina.git.GitService.BlameLine> lines) {
        if (lines == null) {
            this.fullBlame = false;          // keep blame data for the hints
        } else {
            this.blameLines = lines;
            this.fullBlame = true;
        }
        refreshGutter();
        Platform.runLater(this::refreshInlineHints);   // after gutter re-layout
    }

    /**
     * IntelliJ-style author hints: keep the gutter clean (line numbers only)
     * and show the author INLINE, just after each class/method declaration.
     */
    public void setAuthorHints(java.util.List<dev.lumina.git.GitService.BlameLine> lines) {
        this.blameLines = lines;
        this.fullBlame = false;
        refreshGutter();
        Platform.runLater(this::refreshInlineHints);
    }

    /** Regex for a class/interface/enum/record or a method declaration line. */
    private static final java.util.regex.Pattern DECL = java.util.regex.Pattern.compile(
            "\\b(class|interface|enum|record)\\s+[A-Z]"
                    + "|(?:public|private|protected|static|final|abstract|default|synchronized)"
                    + "[\\w<>,\\[\\]\\s]*\\s[a-zA-Z_][A-Za-z0-9_]*\\s*\\("
                    + "|^\\s*(?!return|throw|new|if|for|while|switch)"
                    + "[A-Za-z_][\\w<>,\\[\\]]*\\s+[a-zA-Z_][A-Za-z0-9_]*\\s*\\([^;]*\\)\\s*;\\s*$");

    private boolean isDeclarationLine(int paragraph) {
        if (paragraph >= codeArea.getParagraphs().size()) return false;
        String text = codeArea.getParagraph(paragraph).getText();
        return DECL.matcher(text).find();
    }

    private void refreshGutter() {
        java.util.function.IntFunction<javafx.scene.Node> lineNo =
                LineNumberFactory.get(codeArea);
        codeArea.setParagraphGraphicFactory(i -> {
            // breakpoint dot (click to toggle) — IntelliJ's red circle
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4.5);
            dot.getStyleClass().add("breakpoint-dot");
            dot.setVisible(breakpoints.contains(i + 1));
            javafx.scene.layout.StackPane dotBox =
                    new javafx.scene.layout.StackPane(dot);
            dotBox.setPrefWidth(14);
            dotBox.setMinWidth(14);
            dotBox.getStyleClass().add("breakpoint-box");
            dotBox.setCursor(javafx.scene.Cursor.DEFAULT);
            final int line = i + 1;
            dotBox.setOnMouseClicked(e -> { toggleBreakpoint(line); e.consume(); });

            javafx.scene.Node num = lineNo.apply(i);
            num.setOnMouseClicked(e -> { toggleBreakpoint(line); e.consume(); });

            javafx.scene.layout.HBox box;
            if (fullBlame && blameLines != null) {
                String text = i < blameLines.size() ? blameLines.get(i).gutter() : "";
                javafx.scene.control.Label annotation =
                        new javafx.scene.control.Label(text);
                annotation.getStyleClass().add("blame-label");
                annotation.setPrefWidth(150);
                if (i < blameLines.size() && !blameLines.get(i).summary().isBlank()) {
                    javafx.scene.control.Tooltip.install(annotation,
                            new javafx.scene.control.Tooltip(
                                    blameLines.get(i).author() + " \u2014 "
                                            + blameLines.get(i).date() + "\n"
                                            + blameLines.get(i).summary()));
                }
                box = new javafx.scene.layout.HBox(6, annotation, dotBox, num);
            } else {
                box = new javafx.scene.layout.HBox(2, dotBox, num);
            }
            box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            return box;
        });
    }

    private Runnable onHintClicked;   // set by LuminaApp to toggle full blame

    public void setOnHintClicked(Runnable handler) {
        this.onHintClicked = handler;
    }

    /**
     * Draw "author" hints pinned to the RIGHT edge of the editor, aligned with
     * each class/method declaration line (IntelliJ-style). Clicking a hint
     * toggles full per-line blame.
     */
    private void refreshInlineHints() {
        if (hintOverlay == null) return;
        hintOverlay.getChildren().clear();
        if (blameLines == null) return;   // hints stay visible in both modes

        for (int i = 0; i < codeArea.getParagraphs().size() && i < blameLines.size(); i++) {
            if (!isDeclarationLine(i)) continue;
            java.util.Optional<javafx.geometry.Bounds> bounds = lineBoundsAt(i);
            if (bounds.isEmpty()) continue;
            javafx.geometry.Bounds b = bounds.get();

            // person glyph drawn as an SVG path (reliable, unlike an emoji font)
            javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
            icon.setContent("M8 8a3 3 0 100-6 3 3 0 000 6zm0 1.5c-2.5 0-6 1.25"
                    + "-6 3.75V15h12v-1.75C14 10.75 10.5 9.5 8 9.5z");
            icon.getStyleClass().add("author-hint-icon");
            icon.setScaleX(0.8);
            icon.setScaleY(0.8);

            javafx.scene.control.Label name =
                    new javafx.scene.control.Label(blameLines.get(i).author());
            name.getStyleClass().add("author-hint");

            javafx.scene.layout.HBox hint =
                    new javafx.scene.layout.HBox(4, icon, name);
            hint.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            hint.getStyleClass().add("author-hint-box");
            hint.setCursor(javafx.scene.Cursor.HAND);
            if (!blameLines.get(i).summary().isBlank()) {
                javafx.scene.control.Tooltip.install(hint,
                        new javafx.scene.control.Tooltip(
                                blameLines.get(i).author() + " \u2014 "
                                        + blameLines.get(i).date() + "\n"
                                        + blameLines.get(i).summary()
                                        + (fullBlame
                                        ? "\n\nClick to hide per-line blame"
                                        : "\n\nClick to show per-line blame")));
            }
            hint.setOnMouseClicked(e -> {
                if (onHintClicked != null) onHintClicked.run();
            });

            // Place just past the end of the code, with a comfortable gap —
            // matches IntelliJ's "trailing hint" position and never overlaps text.
            hint.applyCss();
            hint.layout();
            hint.setLayoutX(b.getMaxX() + 40);
            hint.setLayoutY(b.getMinY() + (b.getHeight() - 16) / 2);
            hintOverlay.getChildren().add(hint);
        }
    }

    /** Screen->local bounds of a whole declaration line, if visible. */
    private java.util.Optional<javafx.geometry.Bounds> lineBoundsAt(int paragraph) {
        try {
            String text = codeArea.getParagraph(paragraph).getText();
            if (text.isBlank()) return java.util.Optional.empty();
            int start = codeArea.getAbsolutePosition(paragraph, 0);
            int end = codeArea.getAbsolutePosition(paragraph,
                    Math.max(1, text.length()));
            return codeArea.getCharacterBoundsOnScreen(start, end)
                    .map(screen -> hintOverlay.screenToLocal(screen));
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    // ------------------------------------------------------------ navigation

    private java.util.function.Consumer<String> navigationHandler;

    public void setNavigationHandler(java.util.function.Consumer<String> handler) {
        this.navigationHandler = handler;
    }

    /** 1-based caret line, matching the semantic engine's convention. */
    public int getCaretLine() {
        return codeArea.getCurrentParagraph() + 1;
    }

    /** 1-based caret column. */
    public int getCaretColumn() {
        return codeArea.getCaretColumn() + 1;
    }

    /** Identifier under the caret, or null. */
    public String wordAtCaret() {
        return wordAt(codeArea.getCaretPosition());
    }

    /** Text of the line the caret is on. */
    public String currentLineText() {
        return codeArea.getParagraph(codeArea.getCurrentParagraph()).getText();
    }

    /** Attach a right-click menu to the editor surface. */
    public void setEditorContextMenu(javafx.scene.control.ContextMenu menu) {
        codeArea.setContextMenu(menu);
    }

    /**
     * If the caret sits inside a @Test method, return its name. Scans upward
     * from the caret for a method signature preceded (within a few lines) by
     * a @Test annotation.
     */
    public String testMethodAtCaret() {
        int caretLine = codeArea.getCurrentParagraph();
        java.util.regex.Pattern methodSig = java.util.regex.Pattern.compile(
                "(?:public|private|protected)?\\s*(?:void|[A-Za-z0-9_<>\\[\\]]+)\\s+"
                        + "([a-zA-Z_][A-Za-z0-9_]*)\\s*\\(");
        for (int i = caretLine; i >= 0; i--) {
            String line = codeArea.getParagraph(i).getText();
            java.util.regex.Matcher m = methodSig.matcher(line);
            if (m.find()) {
                // look back up to 3 lines for a @Test annotation
                for (int j = i; j >= Math.max(0, i - 3); j--) {
                    if (codeArea.getParagraph(j).getText().contains("@Test")) {
                        return m.group(1);
                    }
                }
                return null; // nearest method isn't a test
            }
        }
        return null;
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
        String text = codeArea.getText();
        if (text.isEmpty()) return;
        org.fxmisc.richtext.model.StyleSpans<java.util.Collection<String>> spans;
        if (highlighter != null) {
            spans = highlighter.apply(text);
        } else if (!diagnostics.isEmpty()) {
            var plain = new org.fxmisc.richtext.model.StyleSpansBuilder<
                    java.util.Collection<String>>();
            plain.add(java.util.List.of(), text.length());
            spans = plain.create();
        } else {
            return;
        }
        if (!diagnostics.isEmpty()) {
            spans = spans.overlay(diagnosticSpans(text.length()), (a, b) -> {
                if (b.isEmpty()) return a;
                java.util.List<String> merged = new java.util.ArrayList<>(a);
                merged.addAll(b);
                return merged;
            });
        }
        codeArea.setStyleSpans(0, spans);
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

    // ---------------------------------------------------- diagnostics (M3)

    private java.util.function.BiFunction<Path, String, java.util.List<
            dev.lumina.diagnostics.JavaDiagnostics.Diag>> diagnosticsProvider;
    private java.util.List<dev.lumina.diagnostics.JavaDiagnostics.Diag>
            diagnostics = java.util.List.of();
    private java.util.function.Consumer<java.util.List<
            dev.lumina.diagnostics.JavaDiagnostics.Diag>> diagnosticsListener;
    private volatile int editGeneration;
    private javafx.scene.control.Tooltip diagTooltip;
    private Runnable paramInfoTrigger;

    /**
     * M3: install the compile-on-idle pipeline. The provider runs on a
     * worker thread; results are dropped if the buffer changed meanwhile.
     */
    public void setDiagnosticsProvider(java.util.function.BiFunction<Path,
            String, java.util.List<
            dev.lumina.diagnostics.JavaDiagnostics.Diag>> provider) {
        this.diagnosticsProvider = provider;
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(700))
                .subscribe(ignore -> scheduleDiagnostics());
        scheduleDiagnostics();
    }

    public void setDiagnosticsListener(java.util.function.Consumer<
            java.util.List<dev.lumina.diagnostics.JavaDiagnostics.Diag>> listener) {
        this.diagnosticsListener = listener;
    }

    public java.util.List<dev.lumina.diagnostics.JavaDiagnostics.Diag>
            getDiagnostics() {
        return diagnostics;
    }

    private void scheduleDiagnostics() {
        if (diagnosticsProvider == null || path == null
                || !codeArea.isEditable()
                || !path.toString().endsWith(".java")) {
            return;
        }
        final int generation = editGeneration;
        final String text = codeArea.getText();
        Thread worker = new Thread(() -> {
            java.util.List<dev.lumina.diagnostics.JavaDiagnostics.Diag> found;
            try {
                found = diagnosticsProvider.apply(path, text);
            } catch (Throwable t) {
                found = java.util.List.of();
            }
            final java.util.List<dev.lumina.diagnostics.JavaDiagnostics.Diag>
                    result = found;
            Platform.runLater(() -> {
                if (generation == editGeneration) {
                    setDiagnostics(result);
                }
            });
        }, "lumina-diagnostics");
        worker.setDaemon(true);
        worker.start();
    }

    /** Store squiggles and re-render (FX thread). */
    public void setDiagnostics(
            java.util.List<dev.lumina.diagnostics.JavaDiagnostics.Diag> diags) {
        this.diagnostics = diags == null ? java.util.List.of() : diags;
        applyHighlighting();
        if (diagnosticsListener != null) {
            diagnosticsListener.accept(this.diagnostics);
        }
    }

    private org.fxmisc.richtext.model.StyleSpans<java.util.Collection<String>>
            diagnosticSpans(int length) {
        java.util.List<dev.lumina.diagnostics.JavaDiagnostics.Diag> sorted =
                diagnostics.stream()
                        .filter(d -> d.start() < length)
                        .sorted(java.util.Comparator.comparingInt(
                                dev.lumina.diagnostics.JavaDiagnostics.Diag::start))
                        .toList();
        var builder = new org.fxmisc.richtext.model.StyleSpansBuilder<
                java.util.Collection<String>>();
        int last = 0;
        for (dev.lumina.diagnostics.JavaDiagnostics.Diag d : sorted) {
            int start = Math.max(d.start(), last);
            int end = Math.min(Math.max(d.end(), start + 1), length);
            if (start >= end) continue;
            if (start > last) {
                builder.add(java.util.List.of(), start - last);
            }
            builder.add(java.util.List.of(d.severity()
                    == dev.lumina.diagnostics.JavaDiagnostics.Severity.ERROR
                    ? "diag-error" : "diag-warning"), end - start);
            last = end;
        }
        if (length > last) {
            builder.add(java.util.List.of(), length - last);
        }
        return builder.create();
    }

    private dev.lumina.diagnostics.JavaDiagnostics.Diag diagAt(int offset) {
        for (dev.lumina.diagnostics.JavaDiagnostics.Diag d : diagnostics) {
            if (offset >= d.start() && offset <= d.end()) return d;
        }
        return null;
    }

    private void updateDiagTooltip(
            dev.lumina.diagnostics.JavaDiagnostics.Diag diag) {
        if (diag == null) {
            if (diagTooltip != null) {
                javafx.scene.control.Tooltip.uninstall(codeArea, diagTooltip);
                diagTooltip = null;
            }
            return;
        }
        if (diagTooltip != null
                && diag.message().equals(diagTooltip.getText())) {
            return;
        }
        if (diagTooltip != null) {
            javafx.scene.control.Tooltip.uninstall(codeArea, diagTooltip);
        }
        diagTooltip = new javafx.scene.control.Tooltip(diag.message());
        diagTooltip.setShowDelay(javafx.util.Duration.millis(250));
        diagTooltip.setWrapText(true);
        diagTooltip.setMaxWidth(520);
        diagTooltip.getStyleClass().add("diag-tooltip");
        javafx.scene.control.Tooltip.install(codeArea, diagTooltip);
    }

    // ------------------------------------------------------------ docs (M4)

    /** Fired when the user types '(' — LuminaApp shows parameter info. */
    public void setParamInfoTrigger(Runnable trigger) {
        this.paramInfoTrigger = trigger;
    }

    /** Absolute caret offset into the document. */
    public int getCaretOffset() {
        return codeArea.getCaretPosition();
    }

    /** Caret bounds in screen coordinates, for anchoring popups. */
    public java.util.Optional<javafx.geometry.Bounds> caretScreenBounds() {
        return codeArea.getCaretBounds();
    }

    // ------------------------------------------------------------ completion

    @FunctionalInterface
    public interface CompletionProvider {
        java.util.List<dev.lumina.semantics.Completion.Item> complete(
                Path file, String text, int caretLine,
                dev.lumina.semantics.Completion.Context ctx);
    }

    private final CompletionPopup completionPopup =
            new CompletionPopup(this::acceptCompletion);
    private CompletionProvider completionProvider;
    private dev.lumina.semantics.Completion.Context completionCtx;
    private java.util.List<dev.lumina.semantics.Completion.Item> completionBase =
            java.util.List.of();

    public void setCompletionProvider(CompletionProvider provider) {
        this.completionProvider = provider;
    }

    /** Compute the caret context and ask the provider on a worker thread. */
    private void triggerCompletion() {
        if (completionProvider == null || !codeArea.isEditable()) return;
        String text = codeArea.getText();
        int caret = codeArea.getCaretPosition();
        dev.lumina.semantics.Completion.Context ctx =
                dev.lumina.semantics.Completion.contextAt(text, caret);
        if (ctx == null) {
            completionPopup.hide();
            return;
        }
        int caretLine = getCaretLine();
        Thread worker = new Thread(() -> {
            java.util.List<dev.lumina.semantics.Completion.Item> items;
            try {
                items = completionProvider.complete(path, text, caretLine, ctx);
            } catch (Throwable t) {
                items = java.util.List.of();
            }
            final java.util.List<dev.lumina.semantics.Completion.Item> found = items;
            Platform.runLater(() -> {
                if (found.isEmpty()) {
                    completionPopup.hide();
                } else {
                    completionCtx = ctx;
                    completionBase = found;
                    refilterCompletion();
                }
            });
        }, "lumina-completion");
        worker.setDaemon(true);
        worker.start();
    }

    /** Filter the fetched items against the prefix as the user keeps typing. */
    private void refilterCompletion() {
        if (completionCtx == null) return;
        int caret = codeArea.getCaretPosition();
        int start = completionCtx.prefixStart();
        if (caret < start || caret > codeArea.getLength()) {
            completionPopup.hide();
            return;
        }
        String prefix = codeArea.getText(start, caret);
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                completionPopup.hide();
                return;
            }
        }
        java.util.List<dev.lumina.semantics.Completion.Item> filtered =
                completionBase.stream()
                        .filter(item -> dev.lumina.semantics.Completion
                                .matches(prefix, item.name()))
                        .limit(80)
                        .toList();
        var bounds = codeArea.getCaretBounds();
        if (filtered.isEmpty() || bounds.isEmpty()) {
            completionPopup.hide();
            return;
        }
        completionPopup.show(codeArea, filtered, bounds.get());
    }

    /** Insert the chosen item, add its import, and place the caret. */
    private void acceptCompletion(dev.lumina.semantics.Completion.Item item) {
        if (completionCtx == null) return;
        int caret = codeArea.getCaretPosition();
        int start = completionCtx.prefixStart();
        completionCtx = null;
        if (caret < start) return;

        int shift = 0;
        if (item.importFqcn() != null) {
            String full = codeArea.getText();
            if (dev.lumina.semantics.Completion.needsImport(full, item.importFqcn())) {
                int offset = dev.lumina.semantics.Completion.importInsertOffset(full);
                String importLine = "import " + item.importFqcn() + ";\n";
                codeArea.insertText(offset, importLine);
                if (offset <= start) shift = importLine.length();
            }
        }
        codeArea.replaceText(start + shift, caret + shift, item.insert());
        if (item.caretBack() > 0) {
            codeArea.moveTo(codeArea.getCaretPosition() - item.caretBack());
        }
        codeArea.requestFocus();
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