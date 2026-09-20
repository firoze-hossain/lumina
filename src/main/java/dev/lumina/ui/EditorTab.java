package dev.lumina.ui;

import dev.lumina.semantics.Docs;
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
    private boolean pinned;

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
        if (n.endsWith(".properties")) {
            return dev.lumina.syntax.PropertiesSyntaxHighlighter::computeHighlighting;
        }
        if (n.endsWith(".yml") || n.endsWith(".yaml")) {
            return dev.lumina.syntax.YamlSyntaxHighlighter::computeHighlighting;
        }
        return null;
    }

    public EditorTab(String name, Path path) {
        this.baseName = name;
        this.path = path;
        setText(name);

        codeArea.getStyleClass().add("code-area");
        diagPopup.setAutoFix(true);
        diagPopup.setAutoHide(true);
        diagPopup.setHideOnEscape(false);
        ghostPopup.setAutoFix(false);
        ghostPopup.setAutoHide(false);
        ghostPopup.setHideOnEscape(false);
        ghostLabel.getStyleClass().add("editor-ghost-suggestion");
        ghostLabel.setMouseTransparent(true);
        ghostPopup.getContent().add(ghostLabel);
        setOnClosed(e -> {
            hideDiagPopup();
            hideGhostSuggestion();
            contextActionsPopup.hide();
            generatePopup.hide();
            quickDocPopup.hide();
            if (flashTimeline != null) flashTimeline.stop();
        });
        quickDocPopup.setOnSafeDelete(diag -> {
            if (diag.quickFix() != null) {
                runQuickFix(diag.quickFix());
            }
        });
        quickDocPopup.setOnMoreActions(this::openContextActions);
        diagHideTimer.setOnFinished(ev -> {
            if (!isMouseOverDiagPopup) {
                hideDiagPopup();
            }
        });
        refreshGutter();

        // Ctrl/Cmd + hover -> hand cursor, hinting go-to-declaration.
        // Ctrl/Cmd + hover: hand cursor AND underline the identifier, exactly
        // like IntelliJ's navigation hint.
        codeArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, e -> {
            boolean nav = e.isControlDown() || e.isMetaDown();
            codeArea.setCursor(nav ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.TEXT);
            if (nav) {
                clearNavUnderline();
                hideDiagPopup();
                quickDocDebounce.stop();
                if (quickDocPopup.isShowing() && !quickDocPopup.isMouseOver()) {
                    quickDocPopup.hide();
                }
                var hit = codeArea.hit(e.getX(), e.getY());
                underlineWordAt(hit.getInsertionIndex());
            } else {
                clearNavUnderline();
                var hit = codeArea.hit(e.getX(), e.getY());
                int charIdx = hit.getInsertionIndex();
                dev.lumina.diagnostics.JavaDiagnostics.Diag hitDiag = diagAt(charIdx);
                String word = wordAt(charIdx);
                boolean isUnusedSymbolDiag = hitDiag != null && hitDiag.quickFix() != null
                        && (hitDiag.quickFix().startsWith("unused-method:") || hitDiag.quickFix().startsWith("unused-field:"));

                if (hitDiag != null && !isUnusedSymbolDiag) {
                    quickDocDebounce.stop();
                    if (quickDocPopup.isShowing() && !quickDocPopup.isMouseOver()) {
                        quickDocPopup.hide();
                    }
                    diagHideTimer.stop();
                    if (currentPopupDiag != hitDiag) {
                        currentPopupDiag = hitDiag;
                        diagHoverTimer.setOnFinished(ev -> showDiagPopup(hitDiag));
                        diagHoverTimer.playFromStart();
                    }
                } else if (word != null && quickDocProvider != null) {
                    diagHoverTimer.stop();
                    if (diagPopup.isShowing() && !isMouseOverDiagPopup) {
                        diagHideTimer.playFromStart();
                    }
                    int[] range = wordRangeAt(charIdx);
                    if (range != null && (lastHoverWordStart != range[0] || lastHoverWordEnd != range[1])) {
                        lastHoverWordStart = range[0];
                        lastHoverWordEnd = range[1];
                        quickDocDebounce.stop();
                        final dev.lumina.diagnostics.JavaDiagnostics.Diag activeDiag = hitDiag;
                        quickDocDebounce.setOnFinished(ev -> {
                            if (quickDocPopup.isMouseOver()) return;
                            int line = codeArea.offsetToPosition(charIdx, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor() + 1;
                            int col = codeArea.offsetToPosition(charIdx, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMinor() + 1;
                            Docs.SymbolDoc doc = quickDocProvider.apply(line, col);
                            if (doc != null) {
                                javafx.geometry.Bounds b = getWordBoundsOnScreen(charIdx);
                                if (b != null) {
                                    quickDocPopup.show(codeArea, doc, b, activeDiag);
                                }
                            } else if (activeDiag != null) {
                                showDiagPopup(activeDiag);
                            }
                        });
                        quickDocDebounce.playFromStart();
                    }
                } else if (hitDiag != null) {
                    quickDocDebounce.stop();
                    if (quickDocPopup.isShowing() && !quickDocPopup.isMouseOver()) {
                        quickDocPopup.hide();
                    }
                    diagHideTimer.stop();
                    if (currentPopupDiag != hitDiag) {
                        currentPopupDiag = hitDiag;
                        diagHoverTimer.setOnFinished(ev -> showDiagPopup(hitDiag));
                        diagHoverTimer.playFromStart();
                    }
                } else {
                    diagHoverTimer.stop();
                    if (diagPopup.isShowing() && !isMouseOverDiagPopup) {
                        diagHideTimer.playFromStart();
                    }
                    lastHoverWordStart = -1;
                    lastHoverWordEnd = -1;
                    quickDocDebounce.stop();
                    if (quickDocPopup.isShowing() && !quickDocPopup.isMouseOver()) {
                        quickDocPopup.scheduleHide();
                    }
                }
            }
        });
        codeArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_EXITED, e -> {
            clearNavUnderline();
            diagHoverTimer.stop();
            if (diagPopup.isShowing() && !isMouseOverDiagPopup) {
                diagHideTimer.playFromStart();
            }
            quickDocDebounce.stop();
            lastHoverWordStart = -1;
            lastHoverWordEnd = -1;
            if (quickDocPopup.isShowing() && !quickDocPopup.isMouseOver()) {
                quickDocPopup.scheduleHide();
            }
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
                    case ENTER, TAB -> { completionPopup.acceptSelected(); hideGhostSuggestion(); e.consume(); return; }
                    case ESCAPE -> { completionPopup.hide(); hideGhostSuggestion(); e.consume(); return; }
                    case BACK_SPACE -> Platform.runLater(this::refilterCompletion);
                    case LEFT, RIGHT, HOME, END, PAGE_UP, PAGE_DOWN -> {
                        completionPopup.hide();
                        hideGhostSuggestion();
                    }
                    default -> { }
                }
            } else if (ghostPopup.isShowing() && currentGhostSuggestion != null) {
                if (e.getCode() == javafx.scene.input.KeyCode.TAB || e.getCode() == javafx.scene.input.KeyCode.RIGHT) {
                    e.consume();
                    String toInsert = currentGhostSuggestion;
                    hideGhostSuggestion();
                    codeArea.insertText(codeArea.getCaretPosition(), toInsert);
                    applyHighlighting();
                    scheduleDiagnostics();
                    return;
                } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    hideGhostSuggestion();
                    e.consume();
                    return;
                }
            }
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                if (quickDocPopup.isShowing()) {
                    quickDocPopup.hide();
                    e.consume();
                    return;
                }
                if (diagPopup.isShowing()) {
                    hideDiagPopup();
                    e.consume();
                    return;
                }
                if (contextActionsPopup.isShowing()) {
                    contextActionsPopup.hide();
                    e.consume();
                    return;
                }
                if (generatePopup.isShowing()) {
                    generatePopup.hide();
                    e.consume();
                    return;
                }
            }
            if (e.getCode() == javafx.scene.input.KeyCode.SPACE && e.isControlDown()) {
                e.consume();
                triggerCompletion();
                return;
            }
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && e.isAltDown() && e.isShiftDown()) {
                e.consume();
                dev.lumina.diagnostics.JavaDiagnostics.Diag diag =
                        diagAt(codeArea.getCaretPosition());
                if (diag == null) {
                    diag = diagAtLine(codeArea.getCurrentParagraph() + 1);
                }
                if (diag != null && diag.quickFix() != null) {
                    hideDiagPopup();
                    runQuickFix(diag.quickFix());
                }
                return;
            }
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && e.isAltDown()) {
                e.consume();
                hideDiagPopup();
                openContextActions();
                return;
            }
            if (e.getCode() == javafx.scene.input.KeyCode.INSERT && e.isAltDown()) {
                e.consume();
                openGeneratePopup();
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
            quickDocPopup.hide();
            String ch = e.getCharacter();
            if (ch == null || ch.isEmpty()) return;
            char c = ch.charAt(0);
            boolean configFile = isSpringConfigFile();
            if (c == '.' && !configFile) {
                Platform.runLater(this::triggerCompletion);
            } else if (c == '@' && !configFile) {
                Platform.runLater(this::triggerCompletion);
            } else if (c == '(') {
                completionPopup.hide();
                hideGhostSuggestion();
                if (paramInfoTrigger != null) {
                    Platform.runLater(paramInfoTrigger);
                }
            } else if (configFile && (Character.isLetterOrDigit(c)
                    || c == '.' || c == '-' || c == '_')) {
                // application.properties / .yml: continuous key completion,
                // not just after a dot — IntelliJ completes these live.
                Platform.runLater(completionPopup.isShowing()
                        ? this::refilterCompletion : this::triggerCompletion);
            } else if (completionPopup.isShowing()) {
                if (Character.isLetterOrDigit(c) || c == '_') {
                    Platform.runLater(this::refilterCompletion);
                } else {
                    completionPopup.hide();
                    hideGhostSuggestion();
                }
            } else if (c == ' ') {
                int pos = codeArea.getCaretPosition();
                String text = codeArea.getText();
                if (pos >= 8 && text.substring(0, pos).endsWith("extends ")) {
                    Platform.runLater(this::triggerCompletion);
                } else {
                    hideGhostSuggestion();
                }
            } else if (Character.isLetter(c) || c == '_') {
                Platform.runLater(completionPopup.isShowing()
                        ? this::refilterCompletion : this::triggerCompletion);
            } else {
                hideGhostSuggestion();
            }
        });
        codeArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            completionPopup.hide();
            hideDiagPopup();
            hideGhostSuggestion();
        });
        // Hide completion when focus leaves the code area.
        codeArea.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                completionPopup.hide();
                hideGhostSuggestion();
                quickDocPopup.hide();
                if (!isMouseOverDiagPopup) hideDiagPopup();
            }
        });

        // Ctrl/Cmd+Click on an identifier -> go to its declaration or show usages.
        codeArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if (e.isControlDown() || e.isMetaDown()) {
                var hit = codeArea.hit(e.getX(), e.getY());
                int charIdx = hit.getInsertionIndex();
                codeArea.moveTo(charIdx);
                String word = wordAt(charIdx);
                if (word != null) {
                    e.consume();
                    quickDocPopup.hide();
                    int line = codeArea.getCurrentParagraph() + 1;
                    int col = codeArea.getCaretColumn() + 1;
                    javafx.geometry.Bounds b = getWordBoundsOnScreen(charIdx);
                    if (navigationCoordinatesHandler != null) {
                        navigationCoordinatesHandler.navigate(word, line, col, b);
                    } else if (navigationHandler != null) {
                        navigationHandler.accept(word);
                    }
                }
            }
        });

        VirtualizedScrollPane<CodeArea> scroll = new VirtualizedScrollPane<>(codeArea);
        codeGuidesOverlay = new CodeGuidesOverlay(codeArea);
        hintOverlay = new javafx.scene.layout.Pane();
        hintOverlay.setPickOnBounds(false);   // only the hint labels catch clicks
        javafx.scene.layout.StackPane stack =
                new javafx.scene.layout.StackPane(scroll, codeGuidesOverlay, hintOverlay);
        javafx.scene.layout.StackPane.setAlignment(codeGuidesOverlay, javafx.geometry.Pos.TOP_LEFT);
        javafx.scene.layout.StackPane.setAlignment(hintOverlay, javafx.geometry.Pos.TOP_LEFT);
        codeGuidesOverlay.widthProperty().bind(scroll.widthProperty());
        codeGuidesOverlay.heightProperty().bind(scroll.heightProperty());

        mavenSyncBanner = buildMavenSyncBanner();
        stack.getChildren().add(mavenSyncBanner);
        javafx.scene.layout.StackPane.setAlignment(mavenSyncBanner, javafx.geometry.Pos.TOP_RIGHT);
        javafx.scene.layout.StackPane.setMargin(mavenSyncBanner,
                new javafx.geometry.Insets(10, 18, 0, 0));
        // Recompute inline author positions and guide lines on scroll / resize / edits.
        codeArea.estimatedScrollYProperty().addListener((o, a, b) -> {
            refreshInlineHints();
            hideDiagPopup();
            quickDocPopup.hide();
            codeGuidesOverlay.render();
        });
        codeArea.estimatedScrollXProperty().addListener((o, a, b) -> {
            refreshInlineHints();
            hideDiagPopup();
            quickDocPopup.hide();
            codeGuidesOverlay.render();
        });
        codeArea.widthProperty().addListener((o, a, b) -> {
            refreshInlineHints();
            codeGuidesOverlay.render();
        });
        codeArea.heightProperty().addListener((o, a, b) -> {
            refreshInlineHints();
            codeGuidesOverlay.render();
        });
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(150))
                .subscribe(ignore -> {
                    refreshInlineHints();
                    updateCodeGuides();
                    if (onContentSettled != null) onContentSettled.run();
                });
        setContent(stack);
        javafx.application.Platform.runLater(this::updateCodeGuides);
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
    private CodeGuidesOverlay codeGuidesOverlay;

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
            final int line = i + 1;
            dev.lumina.diagnostics.JavaDiagnostics.Diag diagOnLine = diagAtLine(line);
            javafx.scene.control.Label bulb = null;
            if (diagOnLine != null && diagOnLine.quickFix() != null && !breakpoints.contains(line)) {
                boolean isErr = diagOnLine.severity()
                        == dev.lumina.diagnostics.JavaDiagnostics.Severity.ERROR;
                bulb = new javafx.scene.control.Label("\uD83D\uDCA1");
                bulb.getStyleClass().add(isErr ? "gutter-fix-error" : "gutter-fix-warning");
                bulb.setCursor(javafx.scene.Cursor.HAND);
                final dev.lumina.diagnostics.JavaDiagnostics.Diag d = diagOnLine;
                bulb.setOnMouseClicked(e -> {
                    e.consume();
                    showDiagPopupAtLine(line, d);
                });
            }

            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4.5);
            dot.getStyleClass().add("breakpoint-dot");
            dot.setVisible(breakpoints.contains(line));
            javafx.scene.layout.StackPane dotBox =
                    bulb != null ? new javafx.scene.layout.StackPane(dot, bulb)
                                 : new javafx.scene.layout.StackPane(dot);
            dotBox.setPrefWidth(14);
            dotBox.setMinWidth(14);
            dotBox.getStyleClass().add("breakpoint-box");
            dotBox.setCursor(javafx.scene.Cursor.DEFAULT);
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
            box.getStyleClass().add("gutter-row");
            return box;
        });
    }

    private Runnable onHintClicked;   // set by LuminaApp to toggle full blame

    public void setOnHintClicked(Runnable handler) {
        this.onHintClicked = handler;
    }

    private int addStartersLine = -1;   // 0-based paragraph, -1 = no hint
    private Runnable onAddStartersClicked;

    /** Show a clickable "+ Add Starters..." hint above the given line
     *  (IntelliJ's CodeVision hint above <dependencies>/dependencies {}).
     *  Pass -1 to remove it \u2014 e.g. the file isn't a Spring build file. */
    public void setAddStartersLine(int paragraph) {
        this.addStartersLine = paragraph;
        Platform.runLater(this::refreshInlineHints);
    }

    public void setOnAddStartersClicked(Runnable handler) {
        this.onAddStartersClicked = handler;
    }

    // ------------------------------------------------------- Maven sync hint

    private javafx.scene.layout.HBox mavenSyncBanner;
    private boolean mavenChangesPending;
    private Runnable onMavenReloadClicked;
    private Runnable onContentSettled;
    /** Text at the moment the user dismissed the hint; re-shown once the
     *  buffer changes again, so dismissing doesn't silence it forever. */
    private String mavenDismissedSnapshot;

    private javafx.scene.layout.HBox buildMavenSyncBanner() {
        javafx.scene.control.Label reload = new javafx.scene.control.Label("\u21BB");
        reload.getStyleClass().add("maven-sync-reload");
        reload.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.control.Tooltip.install(reload,
                new javafx.scene.control.Tooltip("Load Maven Changes"));
        reload.setOnMouseClicked(e -> {
            if (onMavenReloadClicked != null) onMavenReloadClicked.run();
        });

        javafx.scene.control.Label dismiss = new javafx.scene.control.Label("\u2715");
        dismiss.getStyleClass().add("maven-sync-dismiss");
        dismiss.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.control.Tooltip.install(dismiss, new javafx.scene.control.Tooltip("Dismiss"));
        dismiss.setOnMouseClicked(e -> {
            mavenDismissedSnapshot = getEditorText();
            setMavenChangesPending(false);
        });

        javafx.scene.layout.HBox banner = new javafx.scene.layout.HBox(6, reload, dismiss);
        banner.getStyleClass().add("maven-sync-banner");
        banner.setPadding(new javafx.geometry.Insets(3, 8, 3, 8));
        banner.setAlignment(javafx.geometry.Pos.CENTER);
        // A StackPane stretches every resizable child to fill its content
        // area by default — without this, the banner (a Region) grows to
        // cover the whole editor instead of hugging its own small content,
        // painting the pane solid with its background color.
        banner.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE,
                javafx.scene.layout.Region.USE_PREF_SIZE);
        banner.setPickOnBounds(false);
        banner.setVisible(false);
        banner.setManaged(false);
        return banner;
    }

    /** Whether the "Load Maven Changes" hint is currently showing on this tab. */
    public boolean isMavenChangesPending() {
        return mavenChangesPending;
    }

    /** Show/hide the IntelliJ-style "Load Maven Changes" hint in the top-right
     *  corner of the editor — shown when the build file (pom.xml /
     *  build.gradle) has been edited since dependencies were last resolved. */
    public void setMavenChangesPending(boolean pending) {
        this.mavenChangesPending = pending;
        if (mavenSyncBanner != null) {
            mavenSyncBanner.setVisible(pending);
            mavenSyncBanner.setManaged(pending);
        }
    }

    public void setOnMavenReloadClicked(Runnable handler) {
        this.onMavenReloadClicked = handler;
    }

    /** True once the hint has been dismissed for the buffer's CURRENT text;
     *  any further edit makes this false again, letting the hint reappear. */
    public boolean isMavenDismissed() {
        return mavenDismissedSnapshot != null && mavenDismissedSnapshot.equals(getEditorText());
    }

    /** Clears the dismissal (called after a successful reload). */
    public void clearMavenDismissed() {
        mavenDismissedSnapshot = null;
    }

    /** Fired ~150ms after typing/pasting settles, so the caller can re-check
     *  live state (e.g. the Maven sync hint) without doing it per keystroke. */
    public void setOnContentSettled(Runnable handler) {
        this.onContentSettled = handler;
    }


    /**
     * Draw "author" hints pinned to the RIGHT edge of the editor, aligned with
     * each class/method declaration line (IntelliJ-style). Clicking a hint
     * toggles full per-line blame.
     */
    private void refreshInlineHints() {
        if (hintOverlay == null) return;
        hintOverlay.getChildren().clear();

        // Unused field inlay hints ("no usages")
        if (diagnostics != null && !diagnostics.isEmpty()) {
            for (dev.lumina.diagnostics.JavaDiagnostics.Diag d : diagnostics) {
                if (d.quickFix() != null && d.quickFix().startsWith("unused-field:")) {
                    int lineIdx = Math.max(0, d.line() - 1);
                    if (lineIdx < codeArea.getParagraphs().size()) {
                        lineBoundsAt(lineIdx).ifPresent(b -> {
                            javafx.scene.control.Label noUsages =
                                    new javafx.scene.control.Label("no usages");
                            noUsages.getStyleClass().add("inlay-no-usages");
                            noUsages.applyCss();
                            noUsages.layout();
                            noUsages.setLayoutX(b.getMaxX() + 10);
                            noUsages.setLayoutY(b.getMinY() + (b.getHeight() - 14) / 2);
                            hintOverlay.getChildren().add(noUsages);
                        });
                    }
                }
            }
        }

        if (addStartersLine >= 0 && addStartersLine < codeArea.getParagraphs().size()) {
            lineBoundsAt(addStartersLine).ifPresent(b -> {
                javafx.scene.control.Label link =
                        new javafx.scene.control.Label("+ Add Starters\u2026");
                link.getStyleClass().add("add-starters-hint");
                link.setCursor(javafx.scene.Cursor.HAND);
                link.setOnMouseClicked(e -> {
                    if (onAddStartersClicked != null) onAddStartersClicked.run();
                });
                link.applyCss();
                link.layout();
                link.setLayoutX(b.getMinX());
                link.setLayoutY(b.getMinY() - 20);
                hintOverlay.getChildren().add(link);
            });
        }

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

    @FunctionalInterface
    public interface NavigationCoordinatesHandler {
        void navigate(String word, int line, int column, javafx.geometry.Bounds screenBounds);
    }

    private NavigationCoordinatesHandler navigationCoordinatesHandler;
    private java.util.function.BiFunction<Integer, Integer, Docs.SymbolDoc> quickDocProvider;

    public void setNavigationCoordinatesHandler(NavigationCoordinatesHandler handler) {
        this.navigationCoordinatesHandler = handler;
    }

    public void setQuickDocProvider(java.util.function.BiFunction<Integer, Integer, Docs.SymbolDoc> provider) {
        this.quickDocProvider = provider;
    }

    public QuickDocPopup getQuickDocPopup() {
        return quickDocPopup;
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

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

    public int[] wordRangeAt(int index) {
        String text = codeArea.getText();
        if (text.isEmpty()) return null;
        int i = Math.max(0, Math.min(index, text.length() - 1));
        if (!isWordChar(text.charAt(i)) && i > 0 && isWordChar(text.charAt(i - 1))) i--;
        if (!isWordChar(text.charAt(i))) return null;
        int start = i, end = i;
        while (start > 0 && isWordChar(text.charAt(start - 1))) start--;
        while (end < text.length() && isWordChar(text.charAt(end))) end++;
        return new int[]{start, end};
    }

    public javafx.geometry.Bounds getWordBoundsOnScreen(int index) {
        int[] range = wordRangeAt(index);
        if (range != null) {
            var opt = codeArea.getCharacterBoundsOnScreen(range[0], range[1]);
            if (opt.isPresent()) return opt.get();
        }
        return codeArea.getCaretBounds().orElse(null);
    }

    public void flashSymbolAt(int line, String word, boolean showQuickDoc) {
        final int targetLine = line <= 0 ? 1 : line;
        int targetParagraph = Math.max(0, Math.min(targetLine - 1, codeArea.getParagraphs().size() - 1));

        if (codeArea.getHeight() <= 0 || codeArea.getWidth() <= 0) {
            javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.millis(60));
            pt.setOnFinished(e -> flashSymbolAt(targetLine, word, showQuickDoc));
            pt.play();
            return;
        }

        int matchStart = -1;
        int matchEnd = -1;

        // Search in target paragraph first, then expand outward
        int[] searchOffsets = new int[]{0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6, 7, -7, 8, -8};
        for (int offset : searchOffsets) {
            int p = targetParagraph + offset;
            if (p < 0 || p >= codeArea.getParagraphs().size()) continue;
            String pText = codeArea.getParagraph(p).getText();
            int idx = findIdentifierInLine(pText, word);
            if (idx >= 0) {
                targetParagraph = p;
                int absStart = codeArea.getAbsolutePosition(p, idx);
                matchStart = absStart;
                matchEnd = absStart + word.length();
                break;
            }
        }

        if (matchStart >= 0) {
            final int fStart = matchStart;
            final int fEnd = matchEnd;
            final int fParagraph = targetParagraph;

            codeArea.showParagraphAtCenter(fParagraph);
            codeArea.moveTo(fStart);
            codeArea.requestFollowCaret();
            focusEditor();

            Runnable triggerAnimation = () -> {
                codeArea.selectRange(fStart, fEnd);

                java.util.function.Consumer<javafx.geometry.Bounds> showOverlay = screenB -> {
                    javafx.geometry.Bounds localB = hintOverlay.screenToLocal(screenB);
                    if (localB != null) {
                        if (flashOverlayRect != null) {
                            hintOverlay.getChildren().remove(flashOverlayRect);
                        }
                        if (flashTimeline != null) {
                            flashTimeline.stop();
                        }
                        flashOverlayRect = new javafx.scene.shape.Rectangle(
                                localB.getMinX() - 2, localB.getMinY() - 2,
                                localB.getWidth() + 4, localB.getHeight() + 4);
                        flashOverlayRect.setArcWidth(4);
                        flashOverlayRect.setArcHeight(4);
                        flashOverlayRect.setFill(javafx.scene.paint.Color.web("#ffffff", 0.65));
                        flashOverlayRect.setStroke(javafx.scene.paint.Color.web("#6ba7f7", 0.9));
                        flashOverlayRect.setStrokeWidth(1.5);
                        flashOverlayRect.setMouseTransparent(true);
                        hintOverlay.getChildren().add(flashOverlayRect);

                        flashTimeline = new javafx.animation.Timeline(
                                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                                        new javafx.animation.KeyValue(flashOverlayRect.opacityProperty(), 1.0)),
                                new javafx.animation.KeyFrame(javafx.util.Duration.millis(350),
                                        new javafx.animation.KeyValue(flashOverlayRect.opacityProperty(), 0.8)),
                                new javafx.animation.KeyFrame(javafx.util.Duration.millis(1200),
                                        new javafx.animation.KeyValue(flashOverlayRect.opacityProperty(), 0.0))
                        );
                        flashTimeline.setOnFinished(e -> {
                            hintOverlay.getChildren().remove(flashOverlayRect);
                            flashOverlayRect = null;
                        });
                        flashTimeline.play();
                    }
                };

                try {
                    codeArea.getCharacterBoundsOnScreen(fStart, fEnd).ifPresentOrElse(
                            showOverlay,
                            () -> {
                                javafx.animation.PauseTransition retryPt = new javafx.animation.PauseTransition(javafx.util.Duration.millis(50));
                                retryPt.setOnFinished(ev -> {
                                    try {
                                        codeArea.getCharacterBoundsOnScreen(fStart, fEnd).ifPresent(showOverlay);
                                    } catch (Exception ignored) {}
                                });
                                retryPt.play();
                            }
                    );
                } catch (Exception ignored) {
                }

                if (showQuickDoc && quickDocProvider != null) {
                    javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
                    pt.setOnFinished(e -> {
                        int col = codeArea.getCaretColumn() + 1;
                        Docs.SymbolDoc doc = quickDocProvider.apply(fParagraph + 1, col);
                        if (doc != null) {
                            javafx.geometry.Bounds b = getWordBoundsOnScreen(fStart);
                            if (b != null) {
                                quickDocPopup.show(codeArea, doc, b);
                            }
                        }
                    });
                    pt.play();
                }
            };
            Platform.runLater(triggerAnimation);
        } else {
            goToLine(line);
        }
    }

    private int findIdentifierInLine(String text, String word) {
        if (text == null || word == null || word.isEmpty()) return -1;
        int from = 0;
        while (from < text.length()) {
            int idx = text.indexOf(word, from);
            if (idx < 0) break;
            boolean startOk = idx == 0 || !Character.isJavaIdentifierPart(text.charAt(idx - 1));
            boolean endOk = (idx + word.length() >= text.length())
                    || !Character.isJavaIdentifierPart(text.charAt(idx + word.length()));
            if (startOk && endOk) return idx;
            from = idx + 1;
        }
        return text.indexOf(word);
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

    public boolean isDirty() {
        return dirty;
    }

    public boolean isPinned() {
        return pinned;
    }

    /** Pinned tabs lose their close "x" (matching IntelliJ) and get a
     *  small pin graphic instead \u2014 toggled from the tab's right-click menu. */
    public void setPinned(boolean pinned) {
        this.pinned = pinned;
        setClosable(!pinned);
        setGraphic(pinned ? pinIcon() : null);
    }

    private static javafx.scene.Node pinIcon() {
        javafx.scene.shape.Circle head = new javafx.scene.shape.Circle(2.4);
        head.setFill(javafx.scene.paint.Color.web("#D8A657"));
        javafx.scene.shape.Line pin = new javafx.scene.shape.Line(0, 1.5, 0, 7);
        pin.setStroke(javafx.scene.paint.Color.web("#D8A657"));
        pin.setStrokeWidth(1.3);
        javafx.scene.layout.StackPane pane = new javafx.scene.layout.StackPane(pin, head);
        pane.setPrefSize(11, 11);
        return pane;
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
        if (codeGuidesOverlay != null) {
            codeGuidesOverlay.setActiveCaretLine(line);
        }
    }

    public void updateCodeGuides() {
        if (codeGuidesOverlay == null) return;
        String text = codeArea.getText();
        CodeGuidesScanner.GuidesResult res = CodeGuidesScanner.scan(text, baseName);
        codeGuidesOverlay.updateGuides(res, codeArea.getCurrentParagraph());
    }

    public CodeGuidesOverlay getCodeGuidesOverlay() {
        return codeGuidesOverlay;
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
    private final javafx.stage.Popup diagPopup = new javafx.stage.Popup();
    private dev.lumina.diagnostics.JavaDiagnostics.Diag currentPopupDiag;
    private final javafx.animation.PauseTransition diagHoverTimer =
            new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));
    private final javafx.animation.PauseTransition diagHideTimer =
            new javafx.animation.PauseTransition(javafx.util.Duration.millis(650));
    private boolean isMouseOverDiagPopup = false;
    private final QuickDocPopup quickDocPopup = new QuickDocPopup();
    private final javafx.animation.PauseTransition quickDocDebounce =
            new javafx.animation.PauseTransition(javafx.util.Duration.millis(350));
    private int lastHoverWordStart = -1;
    private int lastHoverWordEnd = -1;
    private javafx.animation.Timeline flashTimeline;
    private javafx.scene.shape.Rectangle flashOverlayRect;
    private Runnable paramInfoTrigger;
    private java.util.function.Consumer<String> onQuickFix;
    private java.util.function.Function<String, String> typeResolver;

    public void setTypeResolver(java.util.function.Function<String, String> resolver) {
        this.typeResolver = resolver;
    }

    /** Called with a diagnostic's {@code quickFix} id (e.g.
     *  "add-dependency:mysql" or "remove-property-line:3") when the user clicks
     *  the action link or presses Alt+Shift+Enter / Alt+Enter on a squiggle. */
    public void setOnQuickFix(java.util.function.Consumer<String> handler) {
        this.onQuickFix = handler;
    }

    private void runQuickFix(String id) {
        if (id == null) return;
        if (id.startsWith("remove-property-line:")) {
            try {
                int lineNum = Integer.parseInt(id.substring("remove-property-line:".length()));
                int lineIdx = lineNum - 1;
                if (lineIdx >= 0 && lineIdx < codeArea.getParagraphs().size()) {
                    int start = codeArea.getAbsolutePosition(lineIdx, 0);
                    int end;
                    if (lineIdx + 1 < codeArea.getParagraphs().size()) {
                        end = codeArea.getAbsolutePosition(lineIdx + 1, 0);
                    } else {
                        end = codeArea.getLength();
                        if (lineIdx > 0) {
                            start = codeArea.getAbsolutePosition(lineIdx - 1,
                                    codeArea.getParagraph(lineIdx - 1).length());
                        }
                    }
                    codeArea.replaceText(start, end, "");
                    applyHighlighting();
                    scheduleDiagnostics();
                }
            } catch (Exception ignored) {
            }
        } else if (id.startsWith("import-class:")) {
            String sym = id.substring("import-class:".length()).trim();
            String fqcn = typeResolver != null ? typeResolver.apply(sym) : null;
            if (fqcn != null) {
                String full = codeArea.getText();
                if (dev.lumina.semantics.Completion.needsImport(full, fqcn)) {
                    int offset = dev.lumina.semantics.Completion.importInsertOffset(full);
                    codeArea.insertText(offset, "import " + fqcn + ";\n");
                    applyHighlighting();
                    scheduleDiagnostics();
                }
            } else if (onQuickFix != null) {
                onQuickFix.accept(id);
            }
        } else if (id.startsWith("add-id-attribute:")) {
            String className = id.substring("add-id-attribute:".length()).trim();
            String full = codeArea.getText();

            boolean useJakarta = full.contains("jakarta.persistence")
                    || (path != null && !full.contains("javax.persistence"));
            String jpaPkg = useJakarta ? "jakarta.persistence" : "javax.persistence";

            int offset = dev.lumina.semantics.Completion.importInsertOffset(full);
            StringBuilder importBuf = new StringBuilder();
            if (dev.lumina.semantics.Completion.needsImport(full, jpaPkg + ".Id")) {
                importBuf.append("import ").append(jpaPkg).append(".Id;\n");
            }
            if (dev.lumina.semantics.Completion.needsImport(full, jpaPkg + ".GeneratedValue")) {
                importBuf.append("import ").append(jpaPkg).append(".GeneratedValue;\n");
            }
            if (dev.lumina.semantics.Completion.needsImport(full, jpaPkg + ".GenerationType")) {
                importBuf.append("import ").append(jpaPkg).append(".GenerationType;\n");
            }

            if (importBuf.length() > 0) {
                codeArea.insertText(offset, importBuf.toString());
                full = codeArea.getText();
            }

            java.util.regex.Pattern classPat = java.util.regex.Pattern.compile(
                    "(?:public\\s+)?class\\s+" + java.util.regex.Pattern.quote(className) + "[^{]*\\{");
            java.util.regex.Matcher matcher = classPat.matcher(full);
            if (matcher.find()) {
                int insertPos = matcher.end();
                String snippet = "\n\n    @Id\n"
                        + "    @GeneratedValue(strategy = GenerationType.IDENTITY)\n"
                        + "    private Long id;\n\n"
                        + "    public Long getId() {\n"
                        + "        return id;\n"
                        + "    }\n\n"
                        + "    public void setId(Long id) {\n"
                        + "        this.id = id;\n"
                        + "    }\n";
                codeArea.insertText(insertPos, snippet);
                applyHighlighting();
                scheduleDiagnostics();
            } else if (onQuickFix != null) {
                onQuickFix.accept(id);
            }
        } else if (id.startsWith("unused-method:")) {
            String methodSig = id.substring("unused-method:".length()).trim();
            String methodName = methodSig;
            int paren = methodSig.indexOf('(');
            if (paren > 0) methodName = methodSig.substring(0, paren);
            String full = codeArea.getText();
            String updated = dev.lumina.codegen.JavaCodeGenerator.removeMethod(full, methodName);
            if (!updated.equals(full)) {
                codeArea.replaceText(0, full.length(), updated);
                applyHighlighting();
                scheduleDiagnostics();
            }
        } else if (id.startsWith("unused-field:")) {
            String fieldName = id.substring("unused-field:".length()).trim();
            String full = codeArea.getText();
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateAddConstructorParam(full, fieldName);
            if (!updated.equals(full)) {
                codeArea.replaceText(0, full.length(), updated);
                applyHighlighting();
                scheduleDiagnostics();
            }
        } else if (onQuickFix != null) {
            onQuickFix.accept(id);
        }
        hideDiagPopup();
    }

    private static String quickFixActionLabel(String id) {
        if (id == null) return "Apply fix";
        if (id.startsWith("unused-method:")) {
            String sig = id.substring("unused-method:".length()).trim();
            return "Safe delete '" + sig + "'";
        }
        if (id.startsWith("unused-field:")) {
            return "Add constructor parameter";
        }
        if (id.startsWith("remove-property-line:")) {
            return "Remove property";
        }
        if (id.startsWith("import-class:")) {
            return "Import class";
        }
        if (id.startsWith("add-id-attribute:")) {
            return "Add Id attribute";
        }
        if (id.startsWith("add-dependency:")) {
            String dep = id.substring("add-dependency:".length());
            String pretty = switch (dep.toLowerCase()) {
                case "mysql" -> "MySQL";
                case "postgresql" -> "PostgreSQL";
                case "mariadb" -> "MariaDB";
                case "h2" -> "H2";
                case "oracle" -> "Oracle";
                case "sqlserver" -> "Microsoft SQL Server";
                default -> dep;
            };
            return "Add dependency on " + pretty;
        }
        return "Apply fix";
    }

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
        if (diagnosticsProvider == null || path == null || !codeArea.isEditable()) {
            return;
        }
        String fname = path.getFileName().toString().toLowerCase();
        boolean diagnosable = fname.endsWith(".java") || fname.endsWith(".properties")
                || fname.endsWith(".yml") || fname.endsWith(".yaml");
        if (!diagnosable) {
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
        refreshGutter();
        Platform.runLater(this::refreshInlineHints);
        if (diagnosticsListener != null) {
            diagnosticsListener.accept(this.diagnostics);
        }
    }

    private org.fxmisc.richtext.model.StyleSpans<java.util.Collection<String>>
            diagnosticSpans(int length) {
        java.util.List<dev.lumina.diagnostics.JavaDiagnostics.Diag> sorted =
                diagnostics.stream()
                        .filter(d -> d.start() < length)
                        .sorted(java.util.Comparator
                                .comparingInt(dev.lumina.diagnostics.JavaDiagnostics.Diag::start)
                                .thenComparingInt(d -> d.severity() == dev.lumina.diagnostics.JavaDiagnostics.Severity.ERROR ? 0 : 1))
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
            boolean isUnusedField = d.quickFix() != null && d.quickFix().startsWith("unused-field:");
            boolean isUnusedMethod = d.quickFix() != null && d.quickFix().startsWith("unused-method:");
            java.util.List<String> classes = new java.util.ArrayList<>();
            if (isUnusedField) {
                classes.add("unused-field");
                classes.add("unused-symbol");
            } else if (isUnusedMethod) {
                classes.add("unused-method");
                classes.add("unused-symbol");
            } else if (d.severity() == dev.lumina.diagnostics.JavaDiagnostics.Severity.ERROR) {
                classes.add("diag-error");
            } else {
                classes.add("diag-warning");
            }
            builder.add(classes, end - start);
            last = end;
        }
        if (length > last) {
            builder.add(java.util.List.of(), length - last);
        }
        return builder.create();
    }

    private dev.lumina.diagnostics.JavaDiagnostics.Diag diagAt(int offset) {
        dev.lumina.diagnostics.JavaDiagnostics.Diag best = null;
        for (dev.lumina.diagnostics.JavaDiagnostics.Diag d : diagnostics) {
            if (offset >= d.start() && offset <= d.end()) {
                if (best == null) {
                    best = d;
                } else if (best.severity() != dev.lumina.diagnostics.JavaDiagnostics.Severity.ERROR
                        && d.severity() == dev.lumina.diagnostics.JavaDiagnostics.Severity.ERROR) {
                    best = d;
                } else if (best.quickFix() == null && d.quickFix() != null) {
                    best = d;
                }
            }
        }
        return best;
    }

    private dev.lumina.diagnostics.JavaDiagnostics.Diag diagAtLine(int line1Based) {
        dev.lumina.diagnostics.JavaDiagnostics.Diag best = null;
        for (dev.lumina.diagnostics.JavaDiagnostics.Diag d : diagnostics) {
            if (d.line() == line1Based) {
                if (best == null) {
                    best = d;
                } else if (best.severity() != dev.lumina.diagnostics.JavaDiagnostics.Severity.ERROR
                        && d.severity() == dev.lumina.diagnostics.JavaDiagnostics.Severity.ERROR) {
                    best = d;
                } else if (best.quickFix() == null && d.quickFix() != null) {
                    best = d;
                }
            }
        }
        return best;
    }

    private void showDiagPopupAtLine(int line, dev.lumina.diagnostics.JavaDiagnostics.Diag diag) {
        if (diag == null) return;
        currentPopupDiag = diag;
        showDiagPopup(diag);
    }

    private void showDiagPopup(dev.lumina.diagnostics.JavaDiagnostics.Diag diag) {
        if (diag == null || codeArea.getScene() == null || codeArea.getScene().getWindow() == null) {
            return;
        }
        diagHideTimer.stop();
        javafx.scene.layout.VBox card = buildDiagCard(diag);
        diagPopup.getContent().setAll(card);

        int start = Math.min(Math.max(0, diag.start()), codeArea.getLength());
        int end = Math.min(Math.max(start + 1, diag.end()), codeArea.getLength());
        java.util.Optional<javafx.geometry.Bounds> b = codeArea.getCharacterBoundsOnScreen(start, end);
        double x, y;
        if (b.isPresent()) {
            x = b.get().getMinX();
            y = b.get().getMaxY() + 4;
        } else {
            int lineIdx = Math.max(0, diag.line() - 1);
            if (lineIdx < codeArea.getParagraphs().size()) {
                int lineStart = codeArea.getAbsolutePosition(lineIdx, 0);
                var lb = codeArea.getCharacterBoundsOnScreen(lineStart, lineStart + 1);
                if (lb.isPresent()) {
                    x = lb.get().getMinX() + 20;
                    y = lb.get().getMaxY() + 4;
                } else {
                    return;
                }
            } else {
                return;
            }
        }
        if (!diagPopup.isShowing()) {
            diagPopup.show(codeArea.getScene().getWindow(), x, y);
        } else {
            diagPopup.setX(x);
            diagPopup.setY(y);
        }
    }

    private void hideDiagPopup() {
        diagHoverTimer.stop();
        diagHideTimer.stop();
        if (diagPopup.isShowing()) {
            diagPopup.hide();
        }
        currentPopupDiag = null;
        isMouseOverDiagPopup = false;
    }

    /**
     * Build IntelliJ IDEA 3-section inspection card:
     * Header (title, quick-fix action link, Alt+Shift+Enter, More actions Alt+Enter),
     * Body (property / context, type, description, [file]),
     * Footer (archive/folder icon, origin jar/module, link, menu).
     */
    private javafx.scene.layout.VBox buildDiagCard(
            dev.lumina.diagnostics.JavaDiagnostics.Diag diag) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox();
        card.getStyleClass().add("diag-card");
        card.setPrefWidth(460);
        card.setMaxWidth(520);
        card.setEffect(new javafx.scene.effect.DropShadow(14, 0, 4,
                javafx.scene.paint.Color.rgb(0, 0, 0, 0.45)));

        card.setOnMouseEntered(e -> {
            isMouseOverDiagPopup = true;
            diagHideTimer.stop();
        });
        card.setOnMouseExited(e -> {
            isMouseOverDiagPopup = false;
            diagHideTimer.playFromStart();
        });

        // 1. Header
        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(6);
        header.getStyleClass().add("diag-header");
        header.setPadding(new javafx.geometry.Insets(10, 14, 8, 14));

        javafx.scene.layout.HBox titleRow = new javafx.scene.layout.HBox(8);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String titleStr = diag.title() != null && !diag.title().isBlank()
                ? diag.title() : diag.message();
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(titleStr);
        titleLabel.getStyleClass().add("diag-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Label titleMenu = new javafx.scene.control.Label("\u22EE");
        titleMenu.getStyleClass().add("diag-menu-btn");
        titleRow.getChildren().addAll(titleLabel, titleMenu);

        javafx.scene.layout.HBox actionRow = new javafx.scene.layout.HBox(10);
        actionRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        if (diag.quickFix() != null) {
            String fixLabel = quickFixActionLabel(diag.quickFix());
            javafx.scene.control.Label fixLink = new javafx.scene.control.Label(fixLabel);
            fixLink.getStyleClass().add("diag-action-link");
            fixLink.setCursor(javafx.scene.Cursor.HAND);
            final String fixId = diag.quickFix();
            fixLink.setOnMouseClicked(e -> {
                e.consume();
                hideDiagPopup();
                runQuickFix(fixId);
            });

            javafx.scene.control.Label shortcut = new javafx.scene.control.Label("Alt+Shift+Enter");
            shortcut.getStyleClass().add("diag-shortcut-hint");

            javafx.scene.layout.HBox fixBox = new javafx.scene.layout.HBox(6, fixLink, shortcut);
            fixBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            fixBox.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.HBox.setHgrow(fixBox, javafx.scene.layout.Priority.ALWAYS);
            actionRow.getChildren().add(fixBox);
        } else {
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            spacer.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            actionRow.getChildren().add(spacer);
        }

        javafx.scene.control.Label moreLink = new javafx.scene.control.Label("More actions\u2026");
        moreLink.getStyleClass().add("diag-action-link");
        moreLink.setCursor(javafx.scene.Cursor.HAND);

        javafx.scene.control.Label moreShortcut = new javafx.scene.control.Label("Alt+Enter");
        moreShortcut.getStyleClass().add("diag-shortcut-hint");

        javafx.scene.layout.HBox moreBox = new javafx.scene.layout.HBox(6, moreLink, moreShortcut);
        moreBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        moreBox.setCursor(javafx.scene.Cursor.HAND);
        final dev.lumina.diagnostics.JavaDiagnostics.Diag currentDiag = diag;
        moreBox.setOnMouseClicked(e -> {
            e.consume();
            hideDiagPopup();
            openContextActions(currentDiag);
        });
        actionRow.getChildren().add(moreBox);

        header.getChildren().addAll(titleRow, actionRow);
        card.getChildren().add(header);

        // Divider 1
        javafx.scene.layout.Region div1 = new javafx.scene.layout.Region();
        div1.getStyleClass().add("diag-divider");
        div1.setPrefHeight(1);
        div1.setMinHeight(1);
        div1.setMaxHeight(1);
        card.getChildren().add(div1);

        // 2. Body
        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(4);
        body.getStyleClass().add("diag-body");
        body.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));

        boolean isUnusedField = diag.quickFix() != null && diag.quickFix().startsWith("unused-field:");
        boolean hasTypeOrDesc = (diag.propertyType() != null && !diag.propertyType().isBlank())
                || (diag.description() != null && !diag.description().isBlank());

        if (isUnusedField) {
            if (diag.context() != null && !diag.context().isBlank()) {
                javafx.scene.layout.HBox ctxRow = new javafx.scene.layout.HBox(6);
                ctxRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.control.Label cIcon = new javafx.scene.control.Label("\u24B8");
                cIcon.getStyleClass().add("diag-class-icon");
                javafx.scene.control.Label ctxLabel = new javafx.scene.control.Label(diag.context());
                ctxLabel.getStyleClass().add("diag-context-label");
                ctxRow.getChildren().addAll(cIcon, ctxLabel);
                body.getChildren().add(ctxRow);
            }
            if (diag.description() != null && !diag.description().isBlank()) {
                javafx.scene.control.Label codeLabel = new javafx.scene.control.Label(diag.description());
                codeLabel.getStyleClass().add("diag-code-snippet");
                body.getChildren().add(codeLabel);
            }
        } else if (hasTypeOrDesc) {
            String propName = diag.context() != null ? diag.context() : "Property";
            javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(propName);
            nameLabel.getStyleClass().add("diag-prop-name");
            body.getChildren().add(nameLabel);

            if (diag.propertyType() != null && !diag.propertyType().isBlank()) {
                javafx.scene.control.Label typeLabel = new javafx.scene.control.Label(diag.propertyType());
                typeLabel.getStyleClass().add("diag-type-label");
                body.getChildren().add(typeLabel);
            }

            if (diag.description() != null && !diag.description().isBlank()) {
                javafx.scene.control.Label descLabel = new javafx.scene.control.Label(diag.description());
                descLabel.getStyleClass().add("diag-desc-label");
                descLabel.setWrapText(true);
                descLabel.setMaxWidth(480);
                javafx.scene.layout.VBox.setMargin(descLabel, new javafx.geometry.Insets(6, 0, 0, 0));
                body.getChildren().add(descLabel);
            }
        } else if (diag.context() != null && !diag.context().isBlank()) {
            javafx.scene.control.Label ctxLabel = new javafx.scene.control.Label(diag.context());
            ctxLabel.getStyleClass().add("diag-context-label");
            ctxLabel.setWrapText(true);
            ctxLabel.setMaxWidth(480);

            String fileName = path != null ? path.getFileName().toString() : "application.properties";
            javafx.scene.control.Label fileLabel = new javafx.scene.control.Label("[" + fileName + "]");
            fileLabel.getStyleClass().add("diag-file-context");

            body.getChildren().addAll(ctxLabel, fileLabel);
        } else {
            javafx.scene.control.Label msgLabel = new javafx.scene.control.Label(diag.message());
            msgLabel.getStyleClass().add("diag-desc-label");
            msgLabel.setWrapText(true);
            body.getChildren().add(msgLabel);
        }
        card.getChildren().add(body);

        // 3. Footer (if origin is present)
        String originText = diag.origin();
        if (originText != null && !originText.isBlank()) {
            javafx.scene.layout.Region div2 = new javafx.scene.layout.Region();
            div2.getStyleClass().add("diag-divider");
            div2.setPrefHeight(1);
            div2.setMinHeight(1);
            div2.setMaxHeight(1);
            card.getChildren().add(div2);

            javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(8);
            footer.getStyleClass().add("diag-footer");
            footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            footer.setPadding(new javafx.geometry.Insets(8, 14, 8, 14));

            boolean isJar = originText.startsWith("Maven:") || originText.contains(".jar");
            javafx.scene.control.Label iconLabel = new javafx.scene.control.Label(
                    isJar ? "\uD83D\uDCE6" : "\uD83D\uDCC1");
            iconLabel.getStyleClass().add("diag-footer-icon");

            javafx.scene.control.Label origLabel = new javafx.scene.control.Label(originText);
            origLabel.getStyleClass().add("diag-footer-origin");
            origLabel.setMaxWidth(Double.MAX_VALUE);
            origLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CENTER_ELLIPSIS);
            javafx.scene.layout.HBox.setHgrow(origLabel, javafx.scene.layout.Priority.ALWAYS);

            boolean isFieldDiag = diag.quickFix() != null && diag.quickFix().startsWith("unused-field:");
            javafx.scene.control.Label linkBtn = new javafx.scene.control.Label(isFieldDiag ? "\u270F" : "\uD83D\uDD17");
            linkBtn.getStyleClass().add("diag-footer-link");
            linkBtn.setCursor(javafx.scene.Cursor.HAND);

            javafx.scene.control.Label footerMenu = new javafx.scene.control.Label("\u22EE");
            footerMenu.getStyleClass().add("diag-menu-btn");

            footer.getChildren().addAll(iconLabel, origLabel, linkBtn, footerMenu);
            card.getChildren().add(footer);
        }

        return card;
    }

    // ------------------------------------------------------------ docs (M4)

    /** Fired when the user types '(' — LuminaApp shows parameter info. */
    public void setParamInfoTrigger(Runnable trigger) {
        this.paramInfoTrigger = trigger;
    }

    /** Selection start offset (== caret when nothing is selected). */
    public int getSelectionStart() {
        return codeArea.getSelection().getStart();
    }

    /** Selection end offset. */
    public int getSelectionEnd() {
        return codeArea.getSelection().getEnd();
    }

    public String getSelectedText() {
        return codeArea.getSelectedText();
    }

    /** Replace a range; each call is one undoable step. */
    public void replaceRange(int start, int end, String text) {
        codeArea.replaceText(start, end, text);
    }

    public void insertAt(int offset, String text) {
        codeArea.insertText(offset, text);
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
    private final ContextActionsPopup contextActionsPopup = new ContextActionsPopup();
    private final GeneratePopup generatePopup = new GeneratePopup();
    private Runnable onGenerateTest;

    public void setOnGenerateTest(Runnable handler) {
        this.onGenerateTest = handler;
    }

    public void openContextActions() {
        openContextActions(null);
    }

    public void openContextActions(dev.lumina.diagnostics.JavaDiagnostics.Diag targetDiag) {
        hideDiagPopup();

        dev.lumina.diagnostics.JavaDiagnostics.Diag diag = targetDiag;
        if (diag != null) {
            if (diag.start() >= 0 && diag.start() <= codeArea.getLength()) {
                codeArea.moveTo(diag.start());
            }
        } else {
            diag = diagAt(codeArea.getCaretPosition());
            if (diag == null) {
                diag = diagAtLine(codeArea.getCurrentParagraph() + 1);
            }
        }

        javafx.geometry.Bounds anchor = null;
        if (diag != null && diag.start() >= 0 && diag.start() <= codeArea.getLength()) {
            int endPos = diag.end() > diag.start() && diag.end() <= codeArea.getLength()
                    ? diag.end() : Math.min(diag.start() + 1, codeArea.getLength());
            var b = codeArea.getCharacterBoundsOnScreen(diag.start(), endPos);
            if (b.isPresent()) {
                anchor = b.get();
            }
        }
        if (anchor == null) {
            anchor = codeArea.getCaretBounds().orElse(null);
        }
        if (anchor == null) {
            int pos = codeArea.getCaretPosition();
            var b = codeArea.getCharacterBoundsOnScreen(pos, Math.min(pos + 1, codeArea.getLength()));
            if (b.isPresent()) anchor = b.get();
        }
        if (anchor == null) {
            anchor = new javafx.geometry.BoundingBox(100, 100, 10, 10);
        }

        java.util.List<ContextActionsPopup.ActionItem> items = new java.util.ArrayList<>();
        String full = codeArea.getText();

        // Detect field name dynamically from diagnostic or current line
        String fieldName = null;
        if (diag != null && diag.quickFix() != null && diag.quickFix().startsWith("unused-field:")) {
            fieldName = diag.quickFix().substring("unused-field:".length()).trim();
        } else if (diag != null && diag.description() != null && diag.description().startsWith("private ")) {
            String[] parts = diag.description().trim().split("\\s+");
            if (parts.length >= 3) {
                fieldName = parts[parts.length - 1].replace(";", "").trim();
            }
        }
        if (fieldName == null) {
            int lineIdx = diag != null && diag.line() > 0 ? diag.line() - 1 : codeArea.getCurrentParagraph();
            if (lineIdx >= 0 && lineIdx < codeArea.getParagraphs().size()) {
                String lineText = codeArea.getParagraph(lineIdx).getText();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                        "\\b(?:private|protected|public)\\s+(?:final\\s+)?(?:static\\s+)?([A-Za-z0-9_<>\\[\\],\\s]+?)\\s+([a-z][A-Za-z0-9_]*)\\s*(?:=\\s*[^;]+)?;")
                        .matcher(lineText);
                if (m.find()) {
                    fieldName = m.group(2);
                }
            }
        }

        // Detect method or field from diagnostic
        String methodSig = null;
        if (diag != null && diag.quickFix() != null && diag.quickFix().startsWith("unused-method:")) {
            methodSig = diag.quickFix().substring("unused-method:".length()).trim();
        }

        if (methodSig != null) {
            final String targetSig = methodSig;
            String mName = targetSig;
            int paren = targetSig.indexOf('(');
            if (paren > 0) mName = targetSig.substring(0, paren);
            final String targetName = mName;

            var pRem = dev.lumina.codegen.JavaCodeGenerator.previewRemoveMethod(full, targetName);
            items.add(ContextActionsPopup.ActionItem.fix("safe-delete-method",
                    "Safe delete '" + targetSig + "'", pRem, () -> {
                        String updated = dev.lumina.codegen.JavaCodeGenerator.removeMethod(codeArea.getText(), targetName);
                        codeArea.replaceText(0, codeArea.getLength(), updated);
                        applyHighlighting();
                        scheduleDiagnostics();
                    }));
            items.add(ContextActionsPopup.ActionItem.separator());
            items.add(ContextActionsPopup.ActionItem.item("change-access", "Change access modifier", () -> {}));
            items.add(ContextActionsPopup.ActionItem.itemWithIcon("copilot-chat", "Open GitHub Copilot Inline Chat", "\uD83D\uDCAC", () -> {}));
            items.add(ContextActionsPopup.ActionItem.item("add-javadoc", "Add Javadoc", () -> {}));
        } else if (fieldName != null) {
            final String targetField = fieldName;
            var pCtor = dev.lumina.codegen.JavaCodeGenerator.previewAddConstructorParam(full, targetField);
            items.add(ContextActionsPopup.ActionItem.fixWithDots("add-ctor-param",
                    "Add constructor parameter", pCtor, () -> {
                        String updated = dev.lumina.codegen.JavaCodeGenerator.generateAddConstructorParam(codeArea.getText(), targetField);
                        codeArea.replaceText(0, codeArea.getLength(), updated);
                        applyHighlighting();
                        scheduleDiagnostics();
                    }));

            var pGs = dev.lumina.codegen.JavaCodeGenerator.previewGettersAndSetters(full, targetField);
            items.add(ContextActionsPopup.ActionItem.fix("create-getter-setter",
                    "Create getter and setter for '" + targetField + "'", pGs, () -> {
                        String updated = dev.lumina.codegen.JavaCodeGenerator.generateGettersAndSetters(codeArea.getText(), targetField);
                        codeArea.replaceText(0, codeArea.getLength(), updated);
                        applyHighlighting();
                        scheduleDiagnostics();
                    }));

            var pG = dev.lumina.codegen.JavaCodeGenerator.previewGetter(full, targetField);
            items.add(ContextActionsPopup.ActionItem.fix("create-getter",
                    "Create getter for '" + targetField + "'", pG, () -> {
                        String updated = dev.lumina.codegen.JavaCodeGenerator.generateGetters(codeArea.getText(), targetField);
                        codeArea.replaceText(0, codeArea.getLength(), updated);
                        applyHighlighting();
                        scheduleDiagnostics();
                    }));

            var pS = dev.lumina.codegen.JavaCodeGenerator.previewSetter(full, targetField);
            items.add(ContextActionsPopup.ActionItem.fix("create-setter",
                    "Create setter for '" + targetField + "'", pS, () -> {
                        String updated = dev.lumina.codegen.JavaCodeGenerator.generateSetters(codeArea.getText(), targetField);
                        codeArea.replaceText(0, codeArea.getLength(), updated);
                        applyHighlighting();
                        scheduleDiagnostics();
                    }));

            var pRem = dev.lumina.codegen.JavaCodeGenerator.previewRemoveField(full, targetField);
            items.add(ContextActionsPopup.ActionItem.fix("remove-field",
                    "Remove field '" + targetField + "'", pRem, () -> {
                        String updated = dev.lumina.codegen.JavaCodeGenerator.removeField(codeArea.getText(), targetField);
                        codeArea.replaceText(0, codeArea.getLength(), updated);
                        applyHighlighting();
                        scheduleDiagnostics();
                    }));

            items.add(ContextActionsPopup.ActionItem.separator());
            items.add(ContextActionsPopup.ActionItem.item("change-access", "Change access modifier", () -> {}));
            items.add(ContextActionsPopup.ActionItem.itemWithIcon("copilot-chat", "Open GitHub Copilot Inline Chat", "\uD83D\uDCAC", () -> {}));
            items.add(ContextActionsPopup.ActionItem.item("add-javadoc", "Add Javadoc", () -> {
                int line = codeArea.getCurrentParagraph();
                int lineStart = codeArea.getAbsolutePosition(line, 0);
                codeArea.insertText(lineStart, "    /** Field " + targetField + " */\n");
                applyHighlighting();
                scheduleDiagnostics();
            }));

            var pThread = dev.lumina.codegen.JavaCodeGenerator.previewThreadLocal(full, targetField);
            items.add(ContextActionsPopup.ActionItem.itemWithPreview("thread-local", "Convert to 'ThreadLocal'", pThread, () -> {
                String updated = dev.lumina.codegen.JavaCodeGenerator.convertToThreadLocal(codeArea.getText(), targetField);
                codeArea.replaceText(0, codeArea.getLength(), updated);
                applyHighlighting();
                scheduleDiagnostics();
            }));

            var pAtomic = dev.lumina.codegen.JavaCodeGenerator.previewAtomic(full, targetField);
            items.add(ContextActionsPopup.ActionItem.itemWithPreview("atomic", "Convert to atomic", pAtomic, () -> {
                String updated = dev.lumina.codegen.JavaCodeGenerator.convertToAtomic(codeArea.getText(), targetField);
                codeArea.replaceText(0, codeArea.getLength(), updated);
                applyHighlighting();
                scheduleDiagnostics();
            }));

            items.add(ContextActionsPopup.ActionItem.separator());
            items.add(ContextActionsPopup.ActionItem.itemWithIcon("ai-actions", "AI Actions...", "@", () -> {}));

        } else if (diag != null && diag.quickFix() != null) {
            String label = quickFixActionLabel(diag.quickFix());
            final String fixId = diag.quickFix();
            items.add(ContextActionsPopup.ActionItem.fix("quickfix", label, null, () -> runQuickFix(fixId)));
            items.add(ContextActionsPopup.ActionItem.separator());
            items.add(ContextActionsPopup.ActionItem.item("change-access", "Change access modifier", () -> {}));
            items.add(ContextActionsPopup.ActionItem.itemWithIcon("copilot-chat", "Open GitHub Copilot Inline Chat", "\uD83D\uDCAC", () -> {}));
            items.add(ContextActionsPopup.ActionItem.item("add-javadoc", "Add Javadoc", () -> {}));
            items.add(ContextActionsPopup.ActionItem.separator());
            items.add(ContextActionsPopup.ActionItem.itemWithIcon("ai-actions", "AI Actions...", "@", () -> {}));

        } else {
            // General context actions matching IntelliJ
            items.add(ContextActionsPopup.ActionItem.item("change-access", "Change access modifier", () -> {}));
            items.add(ContextActionsPopup.ActionItem.itemWithIcon("copilot-chat", "Open GitHub Copilot Inline Chat", "\uD83D\uDCAC", () -> {}));
            items.add(ContextActionsPopup.ActionItem.item("add-javadoc", "Add Javadoc", () -> {}));
            items.add(ContextActionsPopup.ActionItem.separator());
            items.add(ContextActionsPopup.ActionItem.itemWithIcon("ai-actions", "AI Actions...", "@", () -> {}));
        }

        contextActionsPopup.show(codeArea, anchor, items);
    }

    public void openGeneratePopup() {
        javafx.geometry.Bounds anchor = codeArea.getCaretBounds().orElse(null);
        if (anchor == null) {
            int pos = codeArea.getCaretPosition();
            var b = codeArea.getCharacterBoundsOnScreen(pos, Math.min(pos + 1, codeArea.getLength()));
            if (b.isPresent()) anchor = b.get();
        }
        if (anchor == null) {
            anchor = new javafx.geometry.BoundingBox(100, 100, 10, 10);
        }

        java.util.List<GeneratePopup.GenerateItem> items = new java.util.ArrayList<>();
        items.add(GeneratePopup.GenerateItem.of("spring-comp", "Spring Component\u2026", null, "\uD83C\uDF3F", () -> {}));
        items.add(GeneratePopup.GenerateItem.of("constructor", "Constructor", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateConstructor(codeArea.getText());
            codeArea.replaceText(0, codeArea.getLength(), updated);
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("logger", "Logger", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateLogger(codeArea.getText());
            codeArea.replaceText(0, codeArea.getLength(), updated);
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("getter", "Getter", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateGetters(codeArea.getText());
            codeArea.replaceText(0, codeArea.getLength(), updated);
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("setter", "Setter", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateSetters(codeArea.getText());
            codeArea.replaceText(0, codeArea.getLength(), updated);
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("getter-setter", "Getter and Setter", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateGettersAndSetters(codeArea.getText());
            codeArea.replaceText(0, codeArea.getLength(), updated);
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("equals-hashcode", "equals() and hashCode()", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateEqualsAndHashCode(codeArea.getText());
            codeArea.replaceText(0, codeArea.getLength(), updated);
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("tostring", "toString()", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateToString(codeArea.getText());
            codeArea.replaceText(0, codeArea.getLength(), updated);
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("override", "Override Methods\u2026", "Ctrl+O", null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateToString(codeArea.getText());
            codeArea.replaceText(0, codeArea.getLength(), updated);
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("delegate", "Delegate Methods\u2026", null, null, () -> {}));
        items.add(GeneratePopup.GenerateItem.of("test", "Test\u2026", null, null, () -> {
            if (onGenerateTest != null) onGenerateTest.run();
        }));
        items.add(GeneratePopup.GenerateItem.of("copyright", "Copyright", null, null, () -> {}));

        generatePopup.show(codeArea, anchor, items);
    }
    private final javafx.stage.Popup ghostPopup = new javafx.stage.Popup();
    private final javafx.scene.control.Label ghostLabel = new javafx.scene.control.Label();
    private String currentGhostSuggestion = null;
    private CompletionProvider completionProvider;
    private dev.lumina.semantics.Completion.Context completionCtx;
    private java.util.List<dev.lumina.semantics.Completion.Item> completionBase =
            java.util.List.of();

    private void showGhostSuggestion(String text, javafx.geometry.Bounds caretBounds) {
        if (text == null || text.isBlank() || caretBounds == null) {
            hideGhostSuggestion();
            return;
        }
        currentGhostSuggestion = text;
        ghostLabel.setText(text);
        if (codeArea.getScene() == null || codeArea.getScene().getWindow() == null) return;
        if (!ghostPopup.isShowing()) {
            ghostPopup.show(codeArea.getScene().getWindow(), caretBounds.getMaxX(), caretBounds.getMinY());
        } else {
            ghostPopup.setX(caretBounds.getMaxX());
            ghostPopup.setY(caretBounds.getMinY());
        }
    }

    private void hideGhostSuggestion() {
        currentGhostSuggestion = null;
        if (ghostPopup.isShowing()) {
            ghostPopup.hide();
        }
    }

    public void setCompletionProvider(CompletionProvider provider) {
        this.completionProvider = provider;
    }

    /** Compute the caret context and ask the provider on a worker thread. */
    private boolean isSpringConfigFile() {
        if (path == null) return false;
        String n = path.getFileName().toString();
        return n.endsWith(".properties") || n.endsWith(".yml") || n.endsWith(".yaml");
    }

    private boolean isYamlConfigFile() {
        if (path == null) return false;
        String n = path.getFileName().toString();
        return n.endsWith(".yml") || n.endsWith(".yaml");
    }

    private void triggerCompletion() {
        if (completionProvider == null || !codeArea.isEditable()) return;
        String text = codeArea.getText();
        int caret = codeArea.getCaretPosition();
        dev.lumina.semantics.Completion.Context ctx = isYamlConfigFile()
                ? dev.lumina.semantics.Completion.contextForYaml(text, caret)
                : isSpringConfigFile()
                        ? dev.lumina.semantics.Completion.contextForProperties(text, caret)
                        : dev.lumina.semantics.Completion.contextAt(text, caret);
        if (ctx == null) {
            completionPopup.hide();
            hideGhostSuggestion();
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
                    hideGhostSuggestion();
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
            hideGhostSuggestion();
            return;
        }
        String prefix = codeArea.getText(start, caret);
        boolean configFile = isSpringConfigFile();
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            boolean allowed = Character.isLetterOrDigit(c) || c == '_'
                    || (configFile && (c == '.' || c == '-'));
            if (!allowed) {
                completionPopup.hide();
                hideGhostSuggestion();
                return;
            }
        }
        java.util.List<dev.lumina.semantics.Completion.Item> filtered =
                completionBase.stream()
                        .filter(item -> dev.lumina.semantics.Completion
                                .matches(prefix, item.name()))
                        .sorted((a, b) -> {
                            if (completionCtx.annotation()) {
                                int rA = prefixRank(prefix, a.name());
                                int rB = prefixRank(prefix, b.name());
                                if (rA != rB) return Integer.compare(rA, rB);
                                if (rA == 1) {
                                    int l = Integer.compare(a.name().length(), b.name().length());
                                    if (l != 0) return l;
                                }
                            } else if (completionCtx.extendsInterface() != null) {
                                boolean aRepo = a.insert().contains("<");
                                boolean bRepo = b.insert().contains("<");
                                if (aRepo != bRepo) return aRepo ? -1 : 1;
                                int rA = prefixRank(prefix, a.name());
                                int rB = prefixRank(prefix, b.name());
                                if (rA != rB) return Integer.compare(rA, rB);
                            }
                            return a.name().compareToIgnoreCase(b.name());
                        })
                        .limit(80)
                        .toList();
        var bounds = codeArea.getCaretBounds();
        if (filtered.isEmpty() || bounds.isEmpty()) {
            completionPopup.hide();
            hideGhostSuggestion();
            return;
        }
        completionPopup.show(codeArea, filtered, bounds.get());

        if (completionCtx.extendsInterface() != null && !filtered.isEmpty()) {
            dev.lumina.semantics.Completion.Item top = filtered.get(0);
            if (top.insert().contains("<")) {
                String full = top.insert();
                String ghost;
                if (prefix.isEmpty()) {
                    ghost = full;
                } else if (full.regionMatches(true, 0, prefix, 0, prefix.length())) {
                    ghost = full.substring(prefix.length());
                } else {
                    ghost = null;
                }
                if (ghost != null && !ghost.isEmpty()) {
                    showGhostSuggestion(ghost, bounds.get());
                } else {
                    hideGhostSuggestion();
                }
            } else {
                hideGhostSuggestion();
            }
        } else {
            hideGhostSuggestion();
        }
    }

    private static int prefixRank(String prefix, String name) {
        if (name.equalsIgnoreCase(prefix)) return 0;
        if (name.regionMatches(true, 0, prefix, 0, prefix.length())) return 1;
        if (dev.lumina.semantics.Completion.matches(prefix, name)) return 2;
        return 3;
    }

    /** Insert the chosen item, add its import, and place the caret. */
    private void acceptCompletion(dev.lumina.semantics.Completion.Item item) {
        if (completionCtx == null) return;
        int caret = codeArea.getCaretPosition();
        int start = completionCtx.prefixStart();
        completionCtx = null;
        hideGhostSuggestion();
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
        if (codeArea.getHeight() <= 0 || codeArea.getWidth() <= 0) {
            javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.millis(60));
            pt.setOnFinished(e -> goToLine(line));
            pt.play();
            return;
        }
        codeArea.showParagraphAtCenter(target);
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