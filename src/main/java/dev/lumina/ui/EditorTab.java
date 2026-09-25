package dev.lumina.ui;

import dev.lumina.diff.DiffEngine;
import dev.lumina.folding.CodeFoldingScanner;
import dev.lumina.folding.CodeFoldingSettings;
import dev.lumina.folding.FoldRegion;
import dev.lumina.git.GitFileStatus;
import dev.lumina.git.GitService;
import dev.lumina.git.GitStatusManager;
import dev.lumina.semantics.Docs;
import dev.lumina.settings.SmartKeysSettings;
import dev.lumina.syntax.JavaSyntaxHighlighter;
import javafx.application.Platform;
import javafx.scene.control.*;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

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

    private final Runnable gitStatusListener = () -> javafx.application.Platform.runLater(this::updateGitStatus);

    // Git line status tracking
    private String headContent = null;
    private boolean headLoaded = false;
    private final Map<Integer, DiffEngine.DiffChunk> gitLineChunks = new java.util.concurrent.ConcurrentHashMap<>();
    private final List<DiffEngine.DiffChunk> gitChunks = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final javafx.scene.canvas.Canvas errorStripeCanvas = new javafx.scene.canvas.Canvas(12, 100);
    private javafx.stage.Popup gitChangePopup;

    // Code folding state
    private final List<FoldRegion> foldRegions = new ArrayList<>();
    private final Map<Integer, Integer> visibleLineToFullLine = new HashMap<>();
    private final Map<Integer, Integer> fullLineToVisibleLine = new HashMap<>();
    private final Map<Integer, FoldRegion> visibleLineToFoldedRegion = new HashMap<>();
    private String fullDocumentText = "";
    private boolean isApplyingFolding = false;

    // Gutter markers state
    private final Map<Integer, List<dev.lumina.gutter.GutterMarker>> lineMarkers = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer, List<dev.lumina.gutter.GutterMarker>> rawLineMarkers = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.function.Consumer<dev.lumina.settings.GutterIconsSettings> gutterIconsListener = s -> javafx.application.Platform.runLater(this::applyGutterMarkerSettings);
    private java.util.function.BiConsumer<Path, Integer> onNavigateLocation;
    private java.util.function.BiConsumer<javafx.scene.Node, List<dev.lumina.gutter.GutterMarker.NavigationTarget>> onShowImplementationList;
    private javafx.stage.Popup markerHoverPopup;

    public interface TestRunnerCallback {
        void runTest(String className, String methodName, boolean debug);
    }
    private TestRunnerCallback onRunTest;

    public void setOnRunTest(TestRunnerCallback callback) {
        this.onRunTest = callback;
    }

    public void setGutterMarkers(Map<Integer, List<dev.lumina.gutter.GutterMarker>> markers) {
        this.rawLineMarkers.clear();
        if (markers != null) {
            this.rawLineMarkers.putAll(markers);
        }
        applyGutterMarkerSettings();
    }

    private void applyGutterMarkerSettings() {
        this.lineMarkers.clear();
        dev.lumina.settings.GutterIconsSettings s = dev.lumina.settings.GutterIconsSettings.getInstance();
        if (s.isShowGutterIcons()) {
            for (Map.Entry<Integer, List<dev.lumina.gutter.GutterMarker>> entry : rawLineMarkers.entrySet()) {
                List<dev.lumina.gutter.GutterMarker> list = new ArrayList<>();
                for (dev.lumina.gutter.GutterMarker m : entry.getValue()) {
                    if (isMarkerEnabled(m, s)) {
                        list.add(m);
                    }
                }
                if (!list.isEmpty()) {
                    this.lineMarkers.put(entry.getKey(), list);
                }
            }
        }
        javafx.application.Platform.runLater(this::refreshGutter);
    }

    private static boolean isMarkerEnabled(dev.lumina.gutter.GutterMarker m, dev.lumina.settings.GutterIconsSettings s) {
        if (!s.isShowGutterIcons()) return false;
        return switch (m.type()) {
            case IMPLEMENTED_METHOD, IMPLEMENTED_INTERFACE -> s.isIconEnabled("java.implemented.method");
            case IMPLEMENTS_METHOD, IMPLEMENTS_INTERFACE -> s.isIconEnabled("java.implementing.method");
            case OVERRIDES_METHOD -> s.isIconEnabled("java.overriding.method");
            case INFERRED_ANNOTATION -> s.isIconEnabled("java.inferred.contract.annotations") || s.isIconEnabled("java.inferred.nullability.annotations");
            case BEAN_INJECTION -> s.isIconEnabled("spring.bean") || s.isIconEnabled("spring.autowired");
            case TEST_CLASS, TEST_METHOD, TEST_METHOD_PASSED, TEST_METHOD_FAILED -> s.isIconEnabled("common.run.line.marker");
        };
    }

    public void setOnNavigateLocation(java.util.function.BiConsumer<Path, Integer> handler) {
        this.onNavigateLocation = handler;
    }

    public void setOnShowImplementationList(java.util.function.BiConsumer<javafx.scene.Node, List<dev.lumina.gutter.GutterMarker.NavigationTarget>> handler) {
        this.onShowImplementationList = handler;
    }

    public Map<Integer, List<dev.lumina.gutter.GutterMarker>> getLineMarkers() {
        return Collections.unmodifiableMap(lineMarkers);
    }

    public List<dev.lumina.gutter.GutterMarker> getLineMarkersForLine(int line) {
        return lineMarkers.getOrDefault(line, List.of());
    }

    public javafx.scene.Node getNode() {
        return getContent();
    }

    private Runnable onContentEdited;
    private final javafx.animation.PauseTransition gutterMarkerDebounce =
            new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
    public void setOnContentEdited(Runnable r) { this.onContentEdited = r; }

    @FunctionalInterface
    public interface DiffOpenerCallback {
        void openDiff(String title, String baseText, String currentText);
    }
    private DiffOpenerCallback onOpenDiff;
    private Runnable onOpenCommit;

    public void setOnOpenDiff(DiffOpenerCallback callback) { this.onOpenDiff = callback; }
    public void setOnOpenCommit(Runnable callback) { this.onOpenCommit = callback; }

    private int currentFontSize = 13;
    private static final java.util.List<EditorTab> OPEN_EDITOR_TABS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final dev.lumina.settings.EditorGeneralSettings.Listener editorGeneralListener = this::applyEditorGeneralSettings;

    public void zoomFontSize(int delta) {
        int newSize = Math.max(8, Math.min(40, currentFontSize + delta));
        if (newSize == currentFontSize) return;
        dev.lumina.settings.EditorGeneralSettings eg = dev.lumina.settings.EditorGeneralSettings.getInstance();
        if (eg.getMouseControlFontSizeScope() == dev.lumina.settings.EditorGeneralSettings.MouseWheelFontSizeScope.ALL_EDITORS) {
            for (EditorTab tab : OPEN_EDITOR_TABS) {
                tab.setFontSize(newSize);
            }
        } else {
            setFontSize(newSize);
        }
    }

    public void setFontSize(int size) {
        this.currentFontSize = size;
        codeArea.setStyle(codeArea.getStyle() + "; -fx-font-size: " + size + "px;");
    }

    public void applyEditorGeneralSettings(dev.lumina.settings.EditorGeneralSettings eg) {
        if (eg == null) return;
        javafx.application.Platform.runLater(() -> {
            boolean wrap = eg.matchesSoftWrapPattern(baseName);
            codeArea.setWrapText(wrap);
            if (eg.isVirtualSpaceAtBottom()) {
                codeArea.setPadding(new javafx.geometry.Insets(0, 0, 250, 0));
            } else {
                codeArea.setPadding(new javafx.geometry.Insets(0, 0, 0, 0));
            }
        });
    }

    public void updateEditorTextOnSave(String text) {
        if (text == null) return;
        int pos = codeArea.getCaretPosition();
        this.fullDocumentText = text;
        codeArea.replaceText(text);
        if (pos <= text.length()) {
            codeArea.moveTo(pos);
        }
    }

    public EditorTab(String name, Path path) {
        this.baseName = name;
        this.path = path;
        setText(name);

        OPEN_EDITOR_TABS.add(this);
        applyEditorGeneralSettings(dev.lumina.settings.EditorGeneralSettings.getInstance());
        dev.lumina.settings.EditorGeneralSettings.getInstance().addListener(editorGeneralListener);
        dev.lumina.settings.GutterIconsSettings.getInstance().addListener(gutterIconsListener);

        // Ctrl/Cmd + Mouse Wheel font zoom
        codeArea.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (e.isControlDown() || e.isMetaDown()) {
                dev.lumina.settings.EditorGeneralSettings eg = dev.lumina.settings.EditorGeneralSettings.getInstance();
                if (eg.isMouseControlChangeFontSize()) {
                    e.consume();
                    if (e.getDeltaY() > 0) {
                        zoomFontSize(1);
                    } else if (e.getDeltaY() < 0) {
                        zoomFontSize(-1);
                    }
                }
            }
        });

        if (path != null) {
            dev.lumina.git.GitStatusManager.getInstance().addListener(gitStatusListener);
            loadHeadContent();
            updateGitStatus();
        }

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
            OPEN_EDITOR_TABS.remove(this);
            dev.lumina.settings.EditorGeneralSettings.getInstance().removeListener(editorGeneralListener);
            dev.lumina.settings.GutterIconsSettings.getInstance().removeListener(gutterIconsListener);
            if (gitChangePopup != null && gitChangePopup.isShowing()) {
                gitChangePopup.hide();
            }
            if (path != null) {
                dev.lumina.git.GitStatusManager.getInstance().removeListener(gitStatusListener);
            }
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
                            int visLine = codeArea.offsetToPosition(charIdx, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor() + 1;
                            int line = getRealLineNumber(visLine);
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

        gutterMarkerDebounce.setOnFinished(e -> {
            if (onContentEdited != null) {
                onContentEdited.run();
            }
        });
        codeArea.textProperty().addListener((obs, old, txt) -> {
            if (isApplyingFolding) return;
            syncFullDocumentFromUserEdit();
            editGeneration++;
            markDirty();
            gutterMarkerDebounce.playFromStart();
        });
        codeArea.caretPositionProperty().addListener((obs, old, pos) -> {
            notifyCaret();
            highlightCurrentLine();
        });

        // Click on folded placeholder (e.g. import [...], @{...}, { ... }) to unfold
        codeArea.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                var hit = codeArea.hit(e.getX(), e.getY());
                int charIdx = hit.getInsertionIndex();
                int visibleLine = codeArea.offsetToPosition(charIdx, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor() + 1;
                FoldRegion folded = visibleLineToFoldedRegion.get(visibleLine);
                if (folded != null && folded.isFolded()) {
                    toggleFoldRegion(folded);
                    e.consume();
                }
            }
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
                    case SPACE -> {
                        if (dev.lumina.settings.CodeCompletionSettings.getInstance().isInsertBySpaceOrDot()) {
                            completionPopup.acceptSelected();
                            hideGhostSuggestion();
                            e.consume();
                            return;
                        }
                    }
                    case PERIOD -> {
                        if (dev.lumina.settings.CodeCompletionSettings.getInstance().isInsertBySpaceOrDot()) {
                            completionPopup.acceptSelected();
                            hideGhostSuggestion();
                        }
                    }
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
            if (e.isAltDown() && e.isShiftDown() && (e.getCode() == javafx.scene.input.KeyCode.BACK_SLASH || e.getCode() == javafx.scene.input.KeyCode.DOWN)) {
                e.consume();
                triggerCompletion();
                return;
            }
            if (tryExpandPostfix(e.getCode())) {
                e.consume();
                return;
            }
            if ((e.isControlDown() || e.isMetaDown()) && e.getCode() == javafx.scene.input.KeyCode.V) {
                e.consume();
                paste();
                return;
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
            if (e.isControlDown() && e.isAltDown() && e.getCode() == javafx.scene.input.KeyCode.Z) {
                e.consume();
                rollbackAtCaret();
                return;
            }
            if (e.isControlDown() && e.isAltDown() && e.getCode() == javafx.scene.input.KeyCode.UP) {
                e.consume();
                navigateChange(-1);
                return;
            }
            if (e.isControlDown() && e.isAltDown() && e.getCode() == javafx.scene.input.KeyCode.DOWN) {
                e.consume();
                navigateChange(1);
                return;
            }
            if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.D) {
                int line = codeArea.getCurrentParagraph() + 1;
                DiffEngine.DiffChunk chunk = gitLineChunks.get(line);
                if (chunk != null) {
                    e.consume();
                    showDiffForChunk(chunk);
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
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !e.isAltDown() && !e.isControlDown() && !e.isMetaDown()) {
                SmartKeysSettings sk = SmartKeysSettings.getInstance();
                int paragraph = codeArea.getCurrentParagraph();
                String line = codeArea.getParagraph(paragraph).getText();
                int col = codeArea.getCaretColumn();
                String prefix = line.substring(0, Math.min(col, line.length()));
                String suffix = line.substring(Math.min(col, line.length()));
                String baseIndent = line.replaceAll("\\S.*$", "");

                if (e.isShiftDown()) {
                    if (isMarkdownFile() && sk.isMarkdownUseShiftEnterForNewTableRow() && line.contains("|")) {
                        e.consume();
                        long pipes = line.chars().filter(ch -> ch == '|').count();
                        StringBuilder newRow = new StringBuilder("\n|");
                        for (int i = 0; i < Math.max(1, pipes - 1); i++) {
                            newRow.append("  |");
                        }
                        int cur = codeArea.getCaretPosition();
                        codeArea.insertText(cur, newRow.toString());
                        codeArea.moveTo(cur + 3);
                        return;
                    }
                    if (isRubyFile() && sk.isRubyContinueLineCommentsOnEnter() && prefix.trim().startsWith("#")) {
                        e.consume();
                        codeArea.replaceSelection("\n" + baseIndent + "# ");
                        return;
                    }
                    e.consume();
                    codeArea.replaceSelection("\n" + baseIndent);
                    return;
                }

                // 1. Markdown Smart Enter
                if (isMarkdownFile()) {
                    if (sk.isMarkdownInsertHtmlBreakInsideTableCells() && prefix.contains("|") && suffix.contains("|")) {
                        e.consume();
                        codeArea.insertText(codeArea.getCaretPosition(), "<br/>");
                        return;
                    }
                    if (sk.isMarkdownSmartEnterAndBackspace()) {
                        if (prefix.trim().matches("^([-*+]|\\d+\\.)\\s*$") && suffix.trim().isEmpty()) {
                            e.consume();
                            int lineStart = codeArea.getCaretPosition() - col;
                            codeArea.replaceText(lineStart, lineStart + line.length(), baseIndent);
                            codeArea.moveTo(lineStart + baseIndent.length());
                            return;
                        }
                        java.util.regex.Matcher bm = java.util.regex.Pattern.compile("^(\\s*)([-*+])\\s+").matcher(prefix);
                        if (bm.find()) {
                            e.consume();
                            codeArea.replaceSelection("\n" + bm.group(1) + bm.group(2) + " ");
                            return;
                        }
                        java.util.regex.Matcher nm = java.util.regex.Pattern.compile("^(\\s*)(\\d+)\\.\\s+").matcher(prefix);
                        if (nm.find()) {
                            e.consume();
                            int num = Integer.parseInt(nm.group(2));
                            String nextNum;
                            String numerating = sk.getMarkdownListNumerating();
                            if ("With '1.'".equals(numerating)) {
                                nextNum = "1";
                            } else if ("With previous number".equals(numerating)) {
                                nextNum = String.valueOf(num);
                            } else {
                                nextNum = String.valueOf(num + 1);
                            }
                            codeArea.replaceSelection("\n" + nm.group(1) + nextNum + ". ");
                            return;
                        }
                    }
                }

                // 2. SQL Smart Enter
                if (isSqlFile()) {
                    if (sk.isSqlInsertStringConcatOnEnter()) {
                        int quotesBefore = 0;
                        for (int i = 0; i < prefix.length(); i++) {
                            if (prefix.charAt(i) == '\'') quotesBefore++;
                        }
                        if (quotesBefore % 2 == 1 && suffix.contains("'")) {
                            e.consume();
                            codeArea.replaceSelection("' ||\n" + baseIndent + "    '");
                            return;
                        }
                    }
                    if (sk.isSqlCloseCodeBlocksOnEnter()) {
                        String trimmed = prefix.trim().toUpperCase();
                        if (trimmed.endsWith("BEGIN") || trimmed.endsWith("CASE")) {
                            e.consume();
                            String close = trimmed.endsWith("CASE") ? "END" : "END;";
                            codeArea.replaceSelection("\n" + baseIndent + "    \n" + baseIndent + close);
                            codeArea.moveTo(codeArea.getCaretPosition() - baseIndent.length() - close.length() - 1);
                            return;
                        }
                    }
                }

                // 3. Ruby line comments
                if (isRubyFile() && sk.isRubyContinueLineCommentsOnEnter() && prefix.trim().startsWith("#")) {
                    if (sk.isRubyDeleteEmptyLineCommentsOnEnter() && prefix.trim().equals("#") && suffix.trim().isEmpty()) {
                        e.consume();
                        int lineStart = codeArea.getCaretPosition() - col;
                        codeArea.replaceText(lineStart, lineStart + line.length(), baseIndent);
                        codeArea.moveTo(lineStart + baseIndent.length());
                        return;
                    }
                    e.consume();
                    codeArea.replaceSelection("\n" + baseIndent + "# ");
                    return;
                }

                if (sk.isSmartIndent()) {
                    boolean afterOpenBrace = prefix.trim().endsWith("{");
                    boolean beforeCloseBrace = suffix.trim().startsWith("}");
                    boolean docComment = prefix.trim().startsWith("/**") || prefix.trim().startsWith("*");
                    boolean jspTag = path != null && path.toString().endsWith(".jsp") && prefix.trim().endsWith("<%");

                    if (afterOpenBrace && beforeCloseBrace && sk.isInsertPairRBrace()) {
                        e.consume();
                        String indentPlus = baseIndent + "    ";
                        codeArea.replaceSelection("\n" + indentPlus + "\n" + baseIndent);
                        codeArea.moveTo(codeArea.getCaretPosition() - baseIndent.length() - 1);
                        return;
                    } else if (afterOpenBrace) {
                        Platform.runLater(() -> codeArea.insertText(codeArea.getCaretPosition(), baseIndent + "    "));
                    } else if (docComment && sk.isInsertDocCommentStub()) {
                        String docIndent = baseIndent;
                        if (!prefix.trim().startsWith("*")) {
                            docIndent = baseIndent + " ";
                        }
                        String stub = docIndent + "* ";
                        Platform.runLater(() -> codeArea.insertText(codeArea.getCaretPosition(), stub));
                    } else if (jspTag && sk.isInsertPairPercentOnEnterInJsp()) {
                        e.consume();
                        codeArea.replaceSelection("\n" + baseIndent + "    \n" + baseIndent + "%>");
                        codeArea.moveTo(codeArea.getCaretPosition() - baseIndent.length() - 3);
                        return;
                    } else {
                        Platform.runLater(() -> codeArea.insertText(codeArea.getCaretPosition(), baseIndent));
                    }
                } else {
                    Platform.runLater(() -> codeArea.insertText(codeArea.getCaretPosition(), baseIndent));
                }
                return;
            } else if (e.getCode() == javafx.scene.input.KeyCode.TAB && !e.isAltDown() && !e.isControlDown() && !e.isMetaDown()) {
                SmartKeysSettings sk = SmartKeysSettings.getInstance();
                int paragraph = codeArea.getCurrentParagraph();
                String line = codeArea.getParagraph(paragraph).getText();
                int col = codeArea.getCaretColumn();

                if (isMarkdownFile() && sk.isMarkdownUseTabShiftTabToNavigateCells() && line.contains("|")) {
                    int nextPipe = line.indexOf('|', col);
                    if (nextPipe != -1) {
                        e.consume();
                        int lineStart = codeArea.getCaretPosition() - col;
                        int target = nextPipe + 1;
                        if (target < line.length() && line.charAt(target) == ' ') target++;
                        codeArea.moveTo(lineStart + target);
                        return;
                    }
                }

                if (sk.isJumpOutsideClosingBracketOrQuoteWithTab() && codeArea.getSelection().getLength() == 0) {
                    int pos = codeArea.getCaretPosition();
                    String text = codeArea.getText();
                    if (pos < text.length()) {
                        char nextChar = text.charAt(pos);
                        if (nextChar == ')' || nextChar == ']' || nextChar == '}' || nextChar == '>' || nextChar == '"' || nextChar == '\'') {
                            e.consume();
                            codeArea.moveTo(pos + 1);
                            return;
                        }
                    }
                }
                e.consume();
                codeArea.insertText(codeArea.getCaretPosition(), "    ");
                return;
            } else if (e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE && !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !completionPopup.isShowing()) {
                SmartKeysSettings sk = SmartKeysSettings.getInstance();
                int col = codeArea.getCaretColumn();
                int paragraph = codeArea.getCurrentParagraph();
                String line = codeArea.getParagraph(paragraph).getText();
                String prefix = line.substring(0, Math.min(col, line.length()));
                String suffix = line.substring(Math.min(col, line.length()));
                String baseIndent = line.replaceAll("\\S.*$", "");

                if (isMarkdownFile() && sk.isMarkdownSmartEnterAndBackspace()) {
                    if (prefix.matches("^\\s*([*+-]|\\d+\\.)\\s$") && suffix.isEmpty()) {
                        e.consume();
                        int lineStart = codeArea.getCaretPosition() - col;
                        codeArea.replaceText(lineStart, lineStart + line.length(), baseIndent);
                        codeArea.moveTo(lineStart + baseIndent.length());
                        return;
                    }
                }

                if (isScalaFile() && sk.isScalaDeleteClosingBraceAfterDeletingBrace()) {
                    if (col > 0 && prefix.endsWith("{") && suffix.startsWith("}")) {
                        e.consume();
                        int pos = codeArea.getCaretPosition();
                        codeArea.deleteText(pos - 1, pos + 1);
                        return;
                    }
                }

                if (sk.getUnindentOnBackspace() != SmartKeysSettings.UnindentOnBackspace.DISABLED
                        && codeArea.getSelection().getLength() == 0) {
                    String leading = line.substring(0, Math.min(col, line.length()));
                    if (col > 0 && leading.trim().isEmpty()) {
                        e.consume();
                        int spacesToRemove = col % 4 == 0 ? 4 : (col % 4);
                        if (spacesToRemove > col) spacesToRemove = col;
                        int pos = codeArea.getCaretPosition();
                        codeArea.deleteText(pos - spacesToRemove, pos);
                        return;
                    }
                }
            } else if (e.getCode() == javafx.scene.input.KeyCode.HOME && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown()) {
                SmartKeysSettings sk = SmartKeysSettings.getInstance();
                if (sk.isHomeMovesCaretToFirstNonWhitespace()) {
                    int col = codeArea.getCaretColumn();
                    int paragraph = codeArea.getCurrentParagraph();
                    String line = codeArea.getParagraph(paragraph).getText();
                    int firstNonWs = 0;
                    while (firstNonWs < line.length() && Character.isWhitespace(line.charAt(firstNonWs))) {
                        firstNonWs++;
                    }
                    if (firstNonWs < line.length()) {
                        e.consume();
                        int lineStart = codeArea.getCaretPosition() - col;
                        if (col == firstNonWs) {
                            if (e.isShiftDown()) {
                                codeArea.selectRange(codeArea.getAnchor(), lineStart);
                            } else {
                                codeArea.moveTo(lineStart);
                            }
                        } else {
                            if (e.isShiftDown()) {
                                codeArea.selectRange(codeArea.getAnchor(), lineStart + firstNonWs);
                            } else {
                                codeArea.moveTo(lineStart + firstNonWs);
                            }
                        }
                        return;
                    }
                }
            } else if (e.getCode() == javafx.scene.input.KeyCode.END && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown()) {
                SmartKeysSettings sk = SmartKeysSettings.getInstance();
                if (sk.isEndOnBlankLineMovesCaretToIndent()) {
                    int paragraph = codeArea.getCurrentParagraph();
                    String line = codeArea.getParagraph(paragraph).getText();
                    if (line.trim().isEmpty() && paragraph > 0) {
                        String prevLine = codeArea.getParagraph(paragraph - 1).getText();
                        String prevIndent = prevLine.replaceAll("\\S.*$", "");
                        if (prevLine.trim().endsWith("{")) {
                            prevIndent += "    ";
                        }
                        if (!prevIndent.isEmpty()) {
                            e.consume();
                            int lineStart = codeArea.getCaretPosition() - codeArea.getCaretColumn();
                            codeArea.replaceText(lineStart, lineStart + line.length(), prevIndent);
                            codeArea.moveTo(lineStart + prevIndent.length());
                            return;
                        }
                    }
                }
            }
        });

        // '.' auto-triggers member completion; typing refines the open popup.
        codeArea.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, e -> {
            quickDocPopup.hide();
            String ch = e.getCharacter();
            if (ch == null || ch.isEmpty()) return;
            char c = ch.charAt(0);

            SmartKeysSettings sk = SmartKeysSettings.getInstance();
            if (codeArea.getSelection().getLength() > 0 && sk.isSurroundSelectionOnQuoteOrBrace()) {
                char close = 0;
                if (c == '(') close = ')';
                else if (c == '[') close = ']';
                else if (c == '{') close = '}';
                else if (c == '<') close = '>';
                else if (c == '"') close = '"';
                else if (c == '\'') close = '\'';
                if (close != 0) {
                    e.consume();
                    String selected = codeArea.getSelectedText();
                    int start = codeArea.getSelection().getStart();
                    codeArea.replaceSelection(c + selected + close);
                    codeArea.selectRange(start + 1, start + 1 + selected.length());
                    return;
                }
            }

            int curPos = codeArea.getCaretPosition();
            String fullDoc = codeArea.getText();
            char nextC = curPos < fullDoc.length() ? fullDoc.charAt(curPos) : 0;
            int paragraph = codeArea.getCurrentParagraph();
            String line = codeArea.getParagraph(paragraph).getText();
            int col = codeArea.getCaretColumn();
            String prefix = line.substring(0, Math.min(col, line.length()));

            // Rust raw string hash pairing: r# -> r#""#
            if (isRustFile() && sk.isRustInsertPairedHashForRawStrings() && c == '"') {
                if (prefix.matches(".*r#+$")) {
                    e.consume();
                    int hashes = 0;
                    for (int i = prefix.length() - 1; i >= 0 && prefix.charAt(i) == '#'; i--) {
                        hashes++;
                    }
                    String closing = "\"" + "#".repeat(hashes);
                    codeArea.insertText(curPos, "\"" + closing);
                    codeArea.moveTo(curPos + 1);
                    return;
                }
            }

            // Scala multiline quotes: """ -> """"""
            if (isScalaFile() && sk.isScalaInsertPairQuotesForMultilineString() && c == '"' && prefix.endsWith("\"\"")) {
                e.consume();
                codeArea.insertText(curPos, "\"\"\"\"");
                codeArea.moveTo(curPos + 1);
                return;
            }

            // Scala string interpolation upgrade: "$|" + '{' -> 's"...${|}'
            if (isScalaFile() && c == '{') {
                if (sk.isScalaUpgradeSimpleStringIntoInterpolatedAfterDollarBrace() && prefix.endsWith("$")) {
                    int quoteIdx = prefix.lastIndexOf('"');
                    if (quoteIdx != -1 && (quoteIdx == 0 || !Character.isLetter(prefix.charAt(quoteIdx - 1)))) {
                        int lineStart = curPos - col;
                        codeArea.insertText(lineStart + quoteIdx, "s");
                        curPos++;
                    }
                    e.consume();
                    codeArea.insertText(curPos, "{}");
                    codeArea.moveTo(curPos + 1);
                    return;
                }
                if (sk.isScalaWrapSingleExpressionBodyWithClosingBraceAfterBrace() && prefix.trim().endsWith("=")) {
                    e.consume();
                    codeArea.insertText(curPos, " {}");
                    codeArea.moveTo(curPos + 2);
                    return;
                }
            }

            // Python method self insertion: def foo(| -> def foo(self)
            if (isPythonFile() && sk.isPythonInsertSelfWhenDefiningMethod() && c == '(') {
                if (prefix.matches("^\\s+def\\s+[A-Za-z0-9_]+$")) {
                    e.consume();
                    codeArea.insertText(curPos, "(self)");
                    codeArea.moveTo(curPos + 6);
                    return;
                }
            }

            // PHP auto-insert '->' after variable
            if (isPhpFile() && sk.isPhpAutoInsertArrowOnTypingMinusAfterObject() && c == '-') {
                if (prefix.matches(".*\\$[A-Za-z0-9_]+$")) {
                    e.consume();
                    codeArea.insertText(curPos, "->");
                    codeArea.moveTo(curPos + 2);
                    return;
                }
            }

            // PHP auto-insert '<?php' after '<?'
            if (isPhpFile() && sk.isPhpAutoInsertPhpTagAfterTyping() && c == '?') {
                if (prefix.endsWith("<")) {
                    e.consume();
                    codeArea.insertText(curPos, "?php ");
                    codeArea.moveTo(curPos + 5);
                    return;
                }
            }

            if (sk.isInsertPairedBrackets()) {
                if (c == '(') {
                    e.consume();
                    codeArea.insertText(curPos, "()");
                    codeArea.moveTo(curPos + 1);
                    completionPopup.hide();
                    hideGhostSuggestion();
                    if (paramInfoTrigger != null) {
                        Platform.runLater(paramInfoTrigger);
                    }
                    return;
                } else if (c == '[') {
                    e.consume();
                    codeArea.insertText(curPos, "[]");
                    codeArea.moveTo(curPos + 1);
                    return;
                } else if (c == '{') {
                    e.consume();
                    codeArea.insertText(curPos, "{}");
                    codeArea.moveTo(curPos + 1);
                    return;
                } else if ((c == ')' || c == ']' || c == '}') && nextC == c) {
                    e.consume();
                    codeArea.moveTo(curPos + 1);
                    return;
                }
            }

            if (sk.isInsertPairQuote()) {
                if (c == '"') {
                    if (nextC == '"') {
                        e.consume();
                        codeArea.moveTo(curPos + 1);
                        return;
                    } else {
                        e.consume();
                        codeArea.insertText(curPos, "\"\"");
                        codeArea.moveTo(curPos + 1);
                        return;
                    }
                } else if (c == '\'') {
                    if (nextC == '\'') {
                        e.consume();
                        codeArea.moveTo(curPos + 1);
                        return;
                    } else {
                        e.consume();
                        codeArea.insertText(curPos, "''");
                        codeArea.moveTo(curPos + 1);
                        return;
                    }
                }
            }

            if (c == '}' && sk.isReformatBlockOnTypingRBrace()) {
                if (line.trim().isEmpty()) {
                    int indentLen = line.length();
                    int targetIndent = Math.max(0, indentLen - 4);
                    String newIndent = " ".repeat(targetIndent);
                    int lineStart = curPos - line.length();
                    e.consume();
                    codeArea.replaceText(lineStart, curPos, newIndent + "}");
                    codeArea.moveTo(lineStart + newIndent.length() + 1);
                    return;
                }
            }
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
                if (dev.lumina.settings.CodeCompletionSettings.getInstance().isShowSuggestionsAsYouType()) {
                    Platform.runLater(completionPopup.isShowing()
                            ? this::refilterCompletion : this::triggerCompletion);
                }
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
                    int line = getRealLineNumber(codeArea.getCurrentParagraph() + 1);
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

        // Double-click in PHP: select variable name without '$'
        codeArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() == 2 && isPhpFile() && SmartKeysSettings.getInstance().isPhpSelectVarWithoutDollarOnDoubleClick()) {
                Platform.runLater(() -> {
                    String sel = codeArea.getSelectedText();
                    if (sel != null && sel.startsWith("$") && sel.length() > 1) {
                        int selStart = codeArea.getSelection().getStart();
                        int selEnd = codeArea.getSelection().getEnd();
                        codeArea.selectRange(selStart + 1, selEnd);
                    }
                });
            }
        });

        VirtualizedScrollPane<CodeArea> scroll = new VirtualizedScrollPane<>(codeArea);
        codeGuidesOverlay = new CodeGuidesOverlay(codeArea);
        hintOverlay = new javafx.scene.layout.Pane();
        hintOverlay.setPickOnBounds(false);   // only the hint labels catch clicks

        errorStripeCanvas.widthProperty().set(12);
        errorStripeCanvas.heightProperty().bind(scroll.heightProperty());
        errorStripeCanvas.heightProperty().addListener((o, a, b) -> renderErrorStripe());
        errorStripeCanvas.setPickOnBounds(true);
        errorStripeCanvas.setOnMouseClicked(e -> {
            double h = errorStripeCanvas.getHeight();
            if (h > 0 && !codeArea.getParagraphs().isEmpty()) {
                int totalLines = Math.max(1, getRealLineCount());
                int targetLine = (int) ((e.getY() / h) * totalLines) + 1;
                goToLine(Math.min(totalLines, Math.max(1, targetLine)));
            }
        });

        dev.lumina.git.GitConfirmationManager.getInstance().addListener(() -> {
            javafx.application.Platform.runLater(() -> {
                refreshGutter();
                renderErrorStripe();
            });
        });

        javafx.scene.layout.StackPane stack =
                new javafx.scene.layout.StackPane(scroll, codeGuidesOverlay, hintOverlay, errorStripeCanvas);
        javafx.scene.layout.StackPane.setAlignment(codeGuidesOverlay, javafx.geometry.Pos.TOP_LEFT);
        javafx.scene.layout.StackPane.setAlignment(hintOverlay, javafx.geometry.Pos.TOP_LEFT);
        javafx.scene.layout.StackPane.setAlignment(errorStripeCanvas, javafx.geometry.Pos.TOP_RIGHT);
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
                    recomputeGitLineStatus();
                    if (onContentSettled != null) onContentSettled.run();
                });
        setContent(stack);
        javafx.application.Platform.runLater(this::updateCodeGuides);
        javafx.application.Platform.runLater(this::applyAppearanceSettings);
        dev.lumina.settings.EditorAppearanceSettings.getInstance().addListener(s -> javafx.application.Platform.runLater(this::applyAppearanceSettings));
        dev.lumina.folding.CodeFoldingSettings.getInstance().addListener(s -> javafx.application.Platform.runLater(this::refreshGutter));
    }

    public void applyAppearanceSettings() {
        dev.lumina.settings.EditorAppearanceSettings s = dev.lumina.settings.EditorAppearanceSettings.getInstance();
        try {
            var caretNode = codeArea.getCaretSelectionBind().getUnderlyingCaret();
            if (caretNode != null) {
                if (s.isCaretBlinking() && s.getCaretBlinkingMs() > 0) {
                    caretNode.setBlinkRate(javafx.util.Duration.millis(s.getCaretBlinkingMs()));
                } else {
                    caretNode.setBlinkRate(javafx.util.Duration.ZERO);
                }
                if (s.isUseBlockCaret()) {
                    caretNode.setStrokeWidth(7.0);
                } else {
                    caretNode.setStrokeWidth(s.isUseFullLineHeightCaret() ? 2.0 : 1.5);
                }
            }
        } catch (Exception ignored) {}

        if (codeGuidesOverlay != null) {
            codeGuidesOverlay.setShowIndentGuides(s.isShowIndentGuides());
            codeGuidesOverlay.setShowMethodSeparators(s.isShowMethodSeparators());
        }
        refreshGutter();
    }

    // ----------------------------------------------------------- breakpoints & debug

    private final java.util.Set<Integer> breakpoints = new java.util.TreeSet<>();
    private Runnable onBreakpointsChanged;
    private int debugActiveLine = -1; // 1-based real line number
    private final java.util.Map<Integer, String> inlineDebugHints = new java.util.concurrent.ConcurrentHashMap<>();

    public int getDebugActiveLine() {
        return debugActiveLine;
    }

    public void setDebugActiveLine(int realLine) {
        this.debugActiveLine = realLine;
        refreshGutter();
        updateAllParagraphStyles();
        Platform.runLater(this::refreshInlineHints);
    }

    public void clearDebugActiveLine() {
        this.debugActiveLine = -1;
        this.inlineDebugHints.clear();
        refreshGutter();
        updateAllParagraphStyles();
        Platform.runLater(this::refreshInlineHints);
    }

    public void setInlineDebugHint(int realLine, String hint) {
        if (hint == null) {
            inlineDebugHints.remove(realLine);
        } else {
            inlineDebugHints.put(realLine, hint);
        }
        Platform.runLater(this::refreshInlineHints);
    }

    public void clearInlineDebugHints() {
        inlineDebugHints.clear();
        Platform.runLater(this::refreshInlineHints);
    }

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
        updateAllParagraphStyles();
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
     * Author hints: keep the gutter clean (line numbers only)
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

    private javafx.scene.Node createBreakpointGraphic(boolean isBreakpoint, boolean isExecutionLine) {
        javafx.scene.layout.StackPane stack = new javafx.scene.layout.StackPane();
        stack.setPrefSize(14, 14);
        stack.setMinSize(14, 14);
        stack.setMaxSize(14, 14);

        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(6.5);
        circle.setFill(javafx.scene.paint.Color.web("#E54B4B"));
        circle.setStroke(javafx.scene.paint.Color.web("#B02A2A"));
        circle.setStrokeWidth(1.0);
        stack.getChildren().add(circle);

        if (isExecutionLine) {
            javafx.scene.shape.Polygon arrow = new javafx.scene.shape.Polygon(
                    -2.5, -3.5,
                    3.5, 0.0,
                    -2.5, 3.5
            );
            arrow.setFill(javafx.scene.paint.Color.WHITE);
            stack.getChildren().add(arrow);
        } else if (isBreakpoint) {
            javafx.scene.shape.Polyline check = new javafx.scene.shape.Polyline(
                    -3.0, 0.0,
                    -1.0, 2.5,
                    3.0, -2.5
            );
            check.setStroke(javafx.scene.paint.Color.WHITE);
            check.setStrokeWidth(1.3);
            stack.getChildren().add(check);
        }
        return stack;
    }

    private javafx.scene.Node createBreakpointPreviewGraphic() {
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(6.0);
        circle.setFill(javafx.scene.paint.Color.web("#E54B4B", 0.40));
        circle.setStroke(javafx.scene.paint.Color.web("#B02A2A", 0.40));
        circle.setStrokeWidth(1.0);
        return circle;
    }

    private void refreshGutter() {
        dev.lumina.settings.EditorAppearanceSettings appSettings = dev.lumina.settings.EditorAppearanceSettings.getInstance();
        codeArea.setParagraphGraphicFactory(i -> {
            final int visibleLine = i + 1;
            final int line = getRealLineNumber(visibleLine);
            dev.lumina.diagnostics.JavaDiagnostics.Diag diagOnLine = diagAtLine(line);
            javafx.scene.control.Label bulb = null;
            if (appSettings.isShowIntentionBulb() && diagOnLine != null && diagOnLine.quickFix() != null && !breakpoints.contains(line)) {
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

            boolean hasBreakpoint = breakpoints.contains(line);
            boolean isExecution = (line == debugActiveLine);

            javafx.scene.layout.StackPane bpBox = new javafx.scene.layout.StackPane();
            bpBox.setPrefWidth(16);
            bpBox.setMinWidth(16);
            bpBox.setMaxWidth(16);
            bpBox.setAlignment(javafx.geometry.Pos.CENTER);
            bpBox.setCursor(javafx.scene.Cursor.HAND);

            if (hasBreakpoint || isExecution) {
                bpBox.getChildren().add(createBreakpointGraphic(hasBreakpoint, isExecution));
            }

            javafx.scene.control.Label num = new javafx.scene.control.Label();
            num.getStyleClass().add("lineno");
            if (appSettings.isShowLineNumbers()) {
                int maxLine = Math.max(10, getRealLineCount());
                int digits = Math.max(2, String.valueOf(maxLine).length());
                double w = Math.max(28.0, digits * 9.0 + 8.0);
                num.setPrefWidth(w);
                num.setMinWidth(w);
                num.setMaxWidth(w);
                num.setCursor(javafx.scene.Cursor.HAND);
                num.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                var mode = appSettings.getLineNumbersMode();
                if (mode == dev.lumina.settings.EditorAppearanceSettings.LineNumbersMode.ABSOLUTE) {
                    num.setText(String.valueOf(line));
                } else if (mode == dev.lumina.settings.EditorAppearanceSettings.LineNumbersMode.RELATIVE) {
                    int currentRealLine = getCaretLine();
                    num.setText(String.valueOf(Math.abs(line - currentRealLine)));
                } else if (mode == dev.lumina.settings.EditorAppearanceSettings.LineNumbersMode.HYBRID) {
                    int currentRealLine = getCaretLine();
                    if (line == currentRealLine) {
                        num.setText(String.valueOf(line));
                    } else {
                        num.setText(String.valueOf(Math.abs(line - currentRealLine)));
                    }
                }
            } else {
                num.setVisible(false);
                num.setManaged(false);
                num.setPrefWidth(0);
                num.setMinWidth(0);
                num.setMaxWidth(0);
                num.setText("");
            }

            // Hover handlers on bpBox and num for breakpoint preview without clearing line number:
            javafx.event.EventHandler<javafx.scene.input.MouseEvent> onEnter = e -> {
                if (!breakpoints.contains(line) && line != debugActiveLine) {
                    if (bpBox.getChildren().isEmpty()) {
                        bpBox.getChildren().add(createBreakpointPreviewGraphic());
                    }
                }
            };
            javafx.event.EventHandler<javafx.scene.input.MouseEvent> onExit = e -> {
                if (!breakpoints.contains(line) && line != debugActiveLine) {
                    bpBox.getChildren().clear();
                }
            };
            bpBox.setOnMouseEntered(onEnter);
            bpBox.setOnMouseExited(onExit);
            num.setOnMouseEntered(onEnter);
            num.setOnMouseExited(onExit);

            javafx.event.EventHandler<javafx.scene.input.MouseEvent> onToggleBp = e -> {
                toggleBreakpoint(line);
                e.consume();
            };
            bpBox.setOnMouseClicked(onToggleBp);
            num.setOnMouseClicked(onToggleBp);

            javafx.scene.layout.StackPane foldBox = new javafx.scene.layout.StackPane();
            foldBox.setPrefWidth(12);
            foldBox.setMinWidth(12);
            foldBox.setMaxWidth(12);
            foldBox.setAlignment(javafx.geometry.Pos.CENTER);

            boolean showFoldingArrows = CodeFoldingSettings.getInstance().isShowFoldingArrows();
            CodeFoldingSettings.FoldingArrowsMode arrowMode = CodeFoldingSettings.getInstance().getCodeFoldingArrowsMode();
            FoldRegion region = getFoldRegionAtStartLine(line);
            javafx.scene.control.Label chevron = null;
            if (region != null && showFoldingArrows) {
                chevron = new javafx.scene.control.Label(region.isFolded() ? "\u203A" : "\u2304");
                chevron.getStyleClass().add("fold-chevron");
                chevron.setCursor(javafx.scene.Cursor.HAND);
                chevron.setOnMouseClicked(e -> {
                    toggleFoldRegion(region);
                    e.consume();
                });
                if (arrowMode == CodeFoldingSettings.FoldingArrowsMode.ON_MOUSE_HOVER && !region.isFolded()) {
                    chevron.setOpacity(0.0);
                }
                foldBox.getChildren().add(chevron);
            }

            DiffEngine.DiffChunk chunk = gitLineChunks.get(line);
            if (chunk == null && region != null && region.isFolded()) {
                for (int k = region.getStartLine(); k <= region.getEndLine(); k++) {
                    DiffEngine.DiffChunk sub = gitLineChunks.get(k);
                    if (sub != null) {
                        chunk = sub;
                        break;
                    }
                }
            }

            javafx.scene.layout.Region gitStripe = new javafx.scene.layout.Region();
            gitStripe.setPrefWidth(3.5);
            gitStripe.setMinWidth(3.5);
            gitStripe.setMaxWidth(3.5);
            gitStripe.setMinHeight(16);

            boolean showGutterStripe = dev.lumina.git.GitConfirmationManager.getInstance().isHighlightModifiedLinesInGutter();
            if (chunk != null && showGutterStripe) {
                String stripeColor = switch (chunk.type()) {
                    case INSERTED -> "#59A869";
                    case MODIFIED -> "#56A8F5";
                    case DELETED -> "#E06C75";
                };
                gitStripe.setStyle("-fx-background-color: " + stripeColor + "; -fx-cursor: hand;");
                final DiffEngine.DiffChunk activeChunk = chunk;
                gitStripe.setOnMouseClicked(e -> {
                    e.consume();
                    showGitChangePopup(line, activeChunk, gitStripe);
                });
                gitStripe.setOnMouseEntered(e -> gitStripe.setStyle("-fx-background-color: " + stripeColor + "; -fx-opacity: 0.8; -fx-cursor: hand;"));
                gitStripe.setOnMouseExited(e -> gitStripe.setStyle("-fx-background-color: " + stripeColor + "; -fx-opacity: 1.0; -fx-cursor: hand;"));
            } else {
                gitStripe.setStyle("-fx-background-color: transparent;");
            }

            javafx.scene.layout.HBox markersBox = new javafx.scene.layout.HBox(2);
            markersBox.setAlignment(javafx.geometry.Pos.CENTER);
            markersBox.setMinWidth(16);
            List<dev.lumina.gutter.GutterMarker> markers = lineMarkers.get(line);
            if (markers != null && !markers.isEmpty()) {
                for (dev.lumina.gutter.GutterMarker marker : markers) {
                    javafx.scene.Node iconNode = dev.lumina.gutter.GutterMarkerIcons.getIcon(marker.type());
                    iconNode.setOnMouseClicked(e -> {
                        e.consume();
                        handleMarkerClicked(iconNode, marker);
                    });
                    attachMarkerTooltip(iconNode, marker);
                    markersBox.getChildren().add(iconNode);
                }
            }

            javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(3);
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
                box.getChildren().add(annotation);
            }
            if (bulb != null) {
                box.getChildren().add(bulb);
            }
            box.getChildren().addAll(bpBox, num, markersBox, foldBox, gitStripe);
            box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            box.getStyleClass().add("gutter-row");

            if (chevron != null && arrowMode == CodeFoldingSettings.FoldingArrowsMode.ON_MOUSE_HOVER && (region == null || !region.isFolded())) {
                final javafx.scene.control.Label activeChevron = chevron;
                box.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED, e -> activeChevron.setOpacity(1.0));
                box.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_EXITED, e -> activeChevron.setOpacity(0.0));
            }

            if (appSettings.isShowMethodSeparators() && isMethodStartLine(line)) {
                box.setStyle("-fx-border-color: #2D3035 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
            }

            return box;
        });
    }

    private boolean isMethodStartLine(int line) {
        if (line <= 1) return false;
        int p = line - 1;
        if (p < 0 || p >= codeArea.getParagraphs().size()) return false;
        String text = codeArea.getParagraph(p).getText().trim();
        if (text.startsWith("@Test") || text.startsWith("@Override")
                || text.startsWith("@Transactional") || text.startsWith("@ParameterizedTest")) {
            if (p > 0 && !codeArea.getParagraph(p - 1).getText().trim().startsWith("@")) {
                return true;
            }
        }
        if (DECL.matcher(text).find() && (p == 0 || !codeArea.getParagraph(p - 1).getText().trim().startsWith("@"))) {
            return true;
        }
        return false;
    }

    private void handleMarkerClicked(javafx.scene.Node iconNode, dev.lumina.gutter.GutterMarker marker) {
        if (marker.isTestMarker()) {
            String className = null;
            String methodName = null;
            if (!marker.targets().isEmpty()) {
                var target = marker.targets().get(0);
                if (marker.type() == dev.lumina.gutter.GutterMarker.MarkerType.TEST_CLASS) {
                    className = target.targetName();
                } else {
                    methodName = target.targetName();
                    className = target.signature();
                }
            }
            if (className == null && getPath() != null) {
                String fn = getPath().getFileName().toString();
                if (fn.endsWith(".java")) className = fn.substring(0, fn.length() - 5);
            }
            final String cName = className;
            final String mName = methodName;

            javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
            String runLabel = mName != null ? "\u25B6  Run '" + mName + "()'" : "\u25B6  Run '" + cName + "'";
            javafx.scene.control.MenuItem runItem = new javafx.scene.control.MenuItem(runLabel);
            runItem.setOnAction(e -> {
                if (onRunTest != null) onRunTest.runTest(cName, mName, false);
            });

            String debugLabel = mName != null ? "\uD83D\uDC1E  Debug '" + mName + "()'" : "\uD83D\uDC1E  Debug '" + cName + "'";
            javafx.scene.control.MenuItem debugItem = new javafx.scene.control.MenuItem(debugLabel);
            debugItem.setOnAction(e -> {
                if (onRunTest != null) onRunTest.runTest(cName, mName, true);
            });

            menu.getItems().addAll(runItem, debugItem);
            menu.show(iconNode, javafx.geometry.Side.RIGHT, 4, 0);
            return;
        }

        if (marker.targets().isEmpty()) return;
        if (marker.targets().size() == 1) {
            var target = marker.targets().get(0);
            if (onNavigateLocation != null) {
                onNavigateLocation.accept(target.file(), target.line());
            }
        } else if (onShowImplementationList != null) {
            onShowImplementationList.accept(iconNode, marker.targets());
        }
    }

    private void attachMarkerTooltip(javafx.scene.Node node, dev.lumina.gutter.GutterMarker marker) {
        node.setOnMouseEntered(e -> {
            if (markerHoverPopup == null) {
                markerHoverPopup = new javafx.stage.Popup();
                markerHoverPopup.setAutoHide(true);
            }
            markerHoverPopup.getContent().clear();
            markerHoverPopup.getContent().add(buildMarkerTooltipContent(marker));
            javafx.geometry.Bounds b = node.localToScreen(node.getBoundsInLocal());
            if (b != null) {
                markerHoverPopup.show(node, b.getMaxX() + 6, b.getMinY() - 4);
            }
        });
        node.setOnMouseExited(e -> {
            if (markerHoverPopup != null && markerHoverPopup.isShowing()) {
                markerHoverPopup.hide();
            }
        });
    }

    private javafx.scene.Node buildMarkerTooltipContent(dev.lumina.gutter.GutterMarker marker) {
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(4);
        box.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 6; "
                + "-fx-background-radius: 6; -fx-padding: 8 12 8 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 3);");

        if (marker.signaturePreview() != null && !marker.signaturePreview().isBlank()) {
            javafx.scene.control.Label title = new javafx.scene.control.Label(marker.tooltipTitle());
            title.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 11.5px; -fx-font-style: italic;");

            javafx.scene.control.Label sig = new javafx.scene.control.Label(marker.signaturePreview());
            sig.setStyle("-fx-text-fill: #56A8F5; -fx-font-family: monospace; -fx-font-size: 11px; "
                    + "-fx-background-color: #1E1F22; -fx-padding: 6 8 6 8; -fx-border-radius: 4; -fx-background-radius: 4;");

            box.getChildren().addAll(title, sig);
        } else {
            javafx.scene.control.Label title = new javafx.scene.control.Label(marker.tooltipTitle());
            title.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            box.getChildren().add(title);

            if (marker.tooltipSubtitle() != null && !marker.tooltipSubtitle().isBlank()) {
                javafx.scene.control.Label sub = new javafx.scene.control.Label(marker.tooltipSubtitle());
                sub.setStyle("-fx-text-fill: #80848B; -fx-font-size: 11px;");
                box.getChildren().add(sub);
            }
        }
        return box;
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

        // Inline debug variable hints
        for (java.util.Map.Entry<Integer, String> entry : inlineDebugHints.entrySet()) {
            int realLine = entry.getKey();
            int visLine = getVisibleLineNumber(realLine);
            int lineIdx = visLine - 1;
            if (lineIdx >= 0 && lineIdx < codeArea.getParagraphs().size()) {
                lineBoundsAt(lineIdx).ifPresent(b -> {
                    javafx.scene.control.Label hint =
                            new javafx.scene.control.Label(entry.getValue());
                    hint.getStyleClass().add("inlay-debug-value");
                    hint.applyCss();
                    hint.layout();
                    hint.setLayoutX(b.getMaxX() + 14);
                    hint.setLayoutY(b.getMinY() + (b.getHeight() - 14) / 2);
                    hintOverlay.getChildren().add(hint);
                });
            }
        }

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
        return getRealLineNumber(codeArea.getCurrentParagraph() + 1);
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
                String methodName = m.group(1);
                // look back up to 5 lines for a test annotation
                for (int j = i; j >= Math.max(0, i - 5); j--) {
                    String prev = codeArea.getParagraph(j).getText();
                    if (prev.contains("@Test") || prev.contains("@ParameterizedTest")
                            || prev.contains("@RepeatedTest") || prev.contains("@TestFactory")
                            || prev.contains("@TestTemplate")) {
                        return methodName;
                    }
                }
                // Check if in test class and method starts with test
                String fn = getPath() != null ? getPath().getFileName().toString() : "";
                if ((fn.endsWith("Test.java") || fn.endsWith("Tests.java")) && methodName.startsWith("test")) {
                    return methodName;
                }
                return null;
            }
        }
        return null;
    }

    public String testClassAtCaret() {
        if (getPath() != null) {
            String fn = getPath().getFileName().toString();
            if (fn.endsWith(".java")) {
                return fn.substring(0, fn.length() - 5);
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
                        Docs.SymbolDoc doc = quickDocProvider.apply(getRealLineNumber(fParagraph + 1), col);
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
        this.fullDocumentText = text != null ? text : "";
        rescanFoldRegions();
        applyFoldingView(false);
        codeArea.getUndoManager().forgetHistory();
        codeArea.moveTo(0);
        codeArea.requestFollowCaret();
        dirty = false;
        setText(baseName);
        applyHighlighting();
    }

    public String getEditorText() {
        if (foldRegions.isEmpty() || visibleLineToFoldedRegion.isEmpty()) {
            return codeArea.getText();
        }
        return reconstructFullDocumentText();
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
            if (path != null) {
                dev.lumina.git.GitStatusManager.getInstance().setStatus(path, dev.lumina.git.GitFileStatus.MODIFIED);
                updateGitStatus();
            }
        }
    }

    public void markSaved(Path savedTo) {
        this.path = savedTo;
        this.baseName = savedTo.getFileName().toString();
        dirty = false;
        setText(baseName);
        loadHeadContent();
        updateGitStatus();
    }

    public void updateGitStatus() {
        if (path == null) return;
        dev.lumina.git.GitFileStatus status = dev.lumina.git.GitStatusManager.getInstance().getStatus(path);
        getStyleClass().removeAll("git-added", "git-untracked", "git-modified");
        if (status == dev.lumina.git.GitFileStatus.ADDED) {
            getStyleClass().add("git-added");
        } else if (status == dev.lumina.git.GitFileStatus.UNTRACKED) {
            getStyleClass().add("git-untracked");
        } else if (status == dev.lumina.git.GitFileStatus.MODIFIED) {
            getStyleClass().add("git-modified");
        }
    }

    public void loadHeadContent() {
        if (path == null) return;
        Path repoRoot = GitStatusManager.findRepositoryRoot(path);
        if (repoRoot == null) return;
        new Thread(() -> {
            try {
                String relPath = repoRoot.relativize(path).toString();
                String text = GitService.headShowFile(repoRoot, relPath);
                headContent = text;
                headLoaded = true;
                Platform.runLater(this::recomputeGitLineStatus);
            } catch (Exception ignored) {}
        }).start();
    }

    public void recomputeGitLineStatus() {
        if (path == null) return;
        Path repoRoot = GitStatusManager.findRepositoryRoot(path);
        if (repoRoot == null) return;
        if (!headLoaded && headContent == null) {
            loadHeadContent();
            return;
        }
        String curText = getEditorText();
        String base = headContent != null ? headContent : "";
        DiffEngine.DiffResult diff = DiffEngine.diff(base, curText, false);

        gitChunks.clear();
        gitChunks.addAll(diff.chunks());

        gitLineChunks.clear();
        for (DiffEngine.DiffChunk c : diff.chunks()) {
            if (c.type() == DiffEngine.DiffType.DELETED) {
                int line = c.rightStart() + 1;
                gitLineChunks.put(line, c);
            } else {
                for (int line = c.rightStart() + 1; line <= c.rightEnd(); line++) {
                    gitLineChunks.put(line, c);
                }
            }
        }

        if (!diff.chunks().isEmpty()) {
            GitStatusManager.getInstance().setStatus(path, GitFileStatus.MODIFIED);
            updateGitStatus();
        } else if (headLoaded) {
            GitStatusManager.getInstance().setStatus(path, GitFileStatus.NORMAL);
            updateGitStatus();
        }

        Platform.runLater(() -> {
            refreshGutter();
            renderErrorStripe();
        });
    }

    public void rollbackAtCaret() {
        int line = getRealLineNumber(codeArea.getCurrentParagraph() + 1);
        DiffEngine.DiffChunk chunk = gitLineChunks.get(line);
        if (chunk != null) {
            rollbackChunk(chunk);
        }
    }

    public void navigateChange(int dir) {
        if (gitChunks.isEmpty()) return;
        int currentLine = getRealLineNumber(codeArea.getCurrentParagraph() + 1);
        DiffEngine.DiffChunk target = null;
        if (dir > 0) {
            for (DiffEngine.DiffChunk c : gitChunks) {
                if (c.rightStart() + 1 > currentLine) {
                    target = c;
                    break;
                }
            }
            if (target == null) target = gitChunks.get(0);
        } else {
            for (int i = gitChunks.size() - 1; i >= 0; i--) {
                DiffEngine.DiffChunk c = gitChunks.get(i);
                if (c.rightStart() + 1 < currentLine) {
                    target = c;
                    break;
                }
            }
            if (target == null) target = gitChunks.get(gitChunks.size() - 1);
        }
        if (target != null) {
            goToLine(target.rightStart() + 1);
            showGitChangePopup(target.rightStart() + 1, target, null);
        }
    }

    public void rollbackChunk(DiffEngine.DiffChunk chunk) {
        if (chunk == null) return;
        if (gitChangePopup != null && gitChangePopup.isShowing()) {
            gitChangePopup.hide();
        }
        try {
            String base = headContent != null ? headContent : "";
            List<String> headLines = Arrays.asList(base.split("\\R", -1));
            List<String> curLines = new ArrayList<>(Arrays.asList(getEditorText().split("\\R", -1)));

            if (chunk.type() == DiffEngine.DiffType.INSERTED) {
                int start = Math.min(curLines.size(), chunk.rightStart());
                int count = chunk.rightCount();
                for (int i = 0; i < count && start < curLines.size(); i++) {
                    curLines.remove(start);
                }
            } else if (chunk.type() == DiffEngine.DiffType.MODIFIED) {
                int start = Math.min(curLines.size(), chunk.rightStart());
                int count = chunk.rightCount();
                for (int i = 0; i < count && start < curLines.size(); i++) {
                    curLines.remove(start);
                }
                int leftStart = Math.min(headLines.size(), chunk.leftStart());
                int leftEnd = Math.min(headLines.size(), chunk.leftEnd());
                curLines.addAll(start, headLines.subList(leftStart, leftEnd));
            } else if (chunk.type() == DiffEngine.DiffType.DELETED) {
                int start = Math.min(curLines.size(), chunk.rightStart());
                int leftStart = Math.min(headLines.size(), chunk.leftStart());
                int leftEnd = Math.min(headLines.size(), chunk.leftEnd());
                curLines.addAll(start, headLines.subList(leftStart, leftEnd));
            }
            setEditorText(String.join("\n", curLines));
            recomputeGitLineStatus();
        } catch (Exception ex) {
            recomputeGitLineStatus();
        }
    }

    public void showDiffForChunk(DiffEngine.DiffChunk chunk) {
        if (gitChangePopup != null && gitChangePopup.isShowing()) {
            gitChangePopup.hide();
        }
        if (onOpenDiff != null && path != null) {
            String base = headContent != null ? headContent : "";
            String cur = getEditorText();
            String title = "Diff: " + (baseName != null ? baseName : path.getFileName().toString());
            onOpenDiff.openDiff(title, base, cur);
        }
    }

    private void renderErrorStripe() {
        javafx.scene.canvas.GraphicsContext gc = errorStripeCanvas.getGraphicsContext2D();
        double w = errorStripeCanvas.getWidth();
        double h = errorStripeCanvas.getHeight();
        gc.clearRect(0, 0, w, h);
        if (!dev.lumina.git.GitConfirmationManager.getInstance().isHighlightModifiedLinesInErrorStripe()) {
            return;
        }

        int totalLines = Math.max(1, getRealLineCount());
        for (DiffEngine.DiffChunk chunk : gitChunks) {
            double y = ((double) chunk.rightStart() / totalLines) * h;
            double markH = Math.max(3.0, ((double) Math.max(1, chunk.rightCount()) / totalLines) * h);

            javafx.scene.paint.Color color = switch (chunk.type()) {
                case INSERTED -> javafx.scene.paint.Color.web("#59A869");
                case MODIFIED -> javafx.scene.paint.Color.web("#56A8F5");
                case DELETED -> javafx.scene.paint.Color.web("#E06C75");
            };
            gc.setFill(color);
            gc.fillRect(2, y, w - 4, markH);
        }
    }

    public void showGitChangePopup(int line, DiffEngine.DiffChunk chunk, javafx.scene.Node anchor) {
        if (gitChangePopup == null) {
            gitChangePopup = new javafx.stage.Popup();
            gitChangePopup.setAutoHide(true);
            gitChangePopup.setAutoFix(true);
        }
        gitChangePopup.getContent().clear();
        gitChangePopup.getContent().add(buildGitChangePopupContent(line, chunk));

        if (anchor != null) {
            javafx.geometry.Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
            if (b != null) {
                gitChangePopup.show(codeArea, b.getMaxX() + 6, b.getMinY() + 18);
                return;
            }
        }
        javafx.geometry.Bounds winB = codeArea.localToScreen(codeArea.getBoundsInLocal());
        if (winB != null) {
            int visLine = getVisibleLineNumber(line);
            double yOffset = Math.min(winB.getHeight() - 40, Math.max(0, (visLine - 1) * 20.0 + 30));
            gitChangePopup.show(codeArea, winB.getMinX() + 60, winB.getMinY() + yOffset);
        }
    }

    private javafx.scene.Node buildGitChangePopupContent(int line, DiffEngine.DiffChunk chunk) {
        javafx.scene.layout.HBox bar = new javafx.scene.layout.HBox(4);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.setPadding(new javafx.geometry.Insets(3, 6, 3, 6));
        bar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 8, 0, 0, 3);");

        javafx.scene.control.Button prevBtn = createPopupIconButton("M 3 8 L 7 4 L 11 8", "Previous Change (Ctrl+Alt+Up)");
        prevBtn.setOnAction(e -> navigateChange(-1));

        javafx.scene.control.Button nextBtn = createPopupIconButton("M 3 4 L 7 8 L 11 4", "Next Change (Ctrl+Alt+Down)");
        nextBtn.setOnAction(e -> navigateChange(1));

        javafx.scene.control.Button rollbackBtn = createPopupIconButton("M 5 4 L 2 7 L 5 10 M 2 7 L 8 7 C 10 7 11 8.5 11 11", "Rollback Lines (Ctrl+Alt+Z)");
        rollbackBtn.setOnAction(e -> rollbackChunk(chunk));

        javafx.scene.control.Button diffBtn = createPopupIconButton("M 2 3 L 5 6 L 2 9 M 10 3 L 7 6 L 10 9 M 5 6 L 7 6", "Show Diff for Lines (Ctrl+D)");
        diffBtn.setOnAction(e -> showDiffForChunk(chunk));

        javafx.scene.control.Button copyBtn = createPopupIconButton("M 4 2 L 9 2 L 9 7 L 4 7 Z M 2 5 L 2 10 L 7 10", "Copy (Ctrl+C)");
        copyBtn.setOnAction(e -> {
            copyChunkText(chunk);
            if (gitChangePopup != null) gitChangePopup.hide();
        });

        javafx.scene.control.Button changelistBtn = createPopupIconButton("M 2 3 L 10 3 L 10 9 L 2 9 Z M 5 3 L 5 9 M 2 6 L 10 6", "New Changelist...");
        changelistBtn.setOnAction(e -> {
            if (gitChangePopup != null) gitChangePopup.hide();
        });

        javafx.scene.control.Label commitLbl = new javafx.scene.control.Label("Commit This change");
        commitLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6 2 6; -fx-background-radius: 3;");
        commitLbl.setOnMouseEntered(e -> commitLbl.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6 2 6; -fx-background-color: #393B40; -fx-background-radius: 3;"));
        commitLbl.setOnMouseExited(e -> commitLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6 2 6; -fx-background-radius: 3;"));
        commitLbl.setOnMouseClicked(e -> {
            if (gitChangePopup != null) gitChangePopup.hide();
            if (onOpenCommit != null) onOpenCommit.run();
        });

        javafx.scene.control.Button closeBtn = createPopupIconButton("M 3 3 L 8 8 L 3 13", "Close");
        closeBtn.setOnAction(e -> {
            if (gitChangePopup != null) gitChangePopup.hide();
        });

        bar.getChildren().addAll(prevBtn, nextBtn, rollbackBtn, diffBtn, copyBtn, changelistBtn, commitLbl, closeBtn);
        return bar;
    }

    private javafx.scene.control.Button createPopupIconButton(String svgPath, String tooltip) {
        javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
        path.setContent(svgPath);
        path.setFill(javafx.scene.paint.Color.TRANSPARENT);
        path.setStroke(javafx.scene.paint.Color.web("#AFB1B6"));
        path.setStrokeWidth(1.3);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        javafx.scene.control.Button btn = new javafx.scene.control.Button();
        btn.setGraphic(path);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 5 3 5; -fx-background-radius: 3; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-padding: 3 5 3 5; -fx-background-radius: 3; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 5 3 5; -fx-background-radius: 3; -fx-cursor: hand;"));
        btn.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        return btn;
    }

    private void copyChunkText(DiffEngine.DiffChunk chunk) {
        if (chunk == null) return;
        try {
            int startPara = chunk.rightStart();
            int endPara = chunk.rightEnd();
            List<String> lines = new ArrayList<>();
            for (int p = startPara; p < endPara && p < codeArea.getParagraphs().size(); p++) {
                lines.add(codeArea.getParagraph(p).getText());
            }
            String content = String.join("\n", lines);
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(content);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
        } catch (Exception ignored) {}
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

    public void updateAllParagraphStyles() {
        int activeVisLine = debugActiveLine > 0 ? getVisibleLineNumber(debugActiveLine) : -1;
        int caretPar = codeArea.getCurrentParagraph();
        int totalPars = codeArea.getParagraphs().size();
        for (int i = 0; i < totalPars; i++) {
            int visLine = i + 1;
            int real = getRealLineNumber(visLine);
            if (visLine == activeVisLine) {
                codeArea.setParagraphStyle(i, java.util.List.of("debug-current-line"));
            } else if (breakpoints.contains(real)) {
                codeArea.setParagraphStyle(i, java.util.List.of("breakpoint-line"));
            } else if (i == caretPar) {
                codeArea.setParagraphStyle(i, java.util.List.of("has-caret"));
            } else {
                codeArea.setParagraphStyle(i, java.util.Collections.emptyList());
            }
        }
        currentHighlightedLine = caretPar;
    }

    /** Tint the caret's paragraph as the current line (IntelliJ-style). */
    private void highlightCurrentLine() {
        updateAllParagraphStyles();
        if (codeGuidesOverlay != null) {
            codeGuidesOverlay.setActiveCaretLine(codeArea.getCurrentParagraph());
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
            caretListener.caretMoved(getRealLineNumber(codeArea.getCurrentParagraph() + 1),
                    codeArea.getCaretColumn() + 1);
        }
        if (dev.lumina.settings.EditorAppearanceSettings.getInstance().getLineNumbersMode() != dev.lumina.settings.EditorAppearanceSettings.LineNumbersMode.ABSOLUTE) {
            refreshGutter();
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
            String full = getEditorText();
            String updated = dev.lumina.codegen.JavaCodeGenerator.removeMethod(full, methodName);
            if (!updated.equals(full)) {
                setEditorText(updated);
                markDirty();
                applyHighlighting();
                scheduleDiagnostics();
            }
        } else if (id.startsWith("unused-field:")) {
            String fieldName = id.substring("unused-field:".length()).trim();
            String full = getEditorText();
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateAddConstructorParam(full, fieldName);
            if (!updated.equals(full)) {
                setEditorText(updated);
                markDirty();
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
        final String text = getEditorText();
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
        var builder = new org.fxmisc.richtext.model.StyleSpansBuilder<
                java.util.Collection<String>>();
        int last = 0;

        record VisDiag(int start, int end, dev.lumina.diagnostics.JavaDiagnostics.Diag diag) implements Comparable<VisDiag> {
            @Override
            public int compareTo(VisDiag o) {
                int cmp = Integer.compare(this.start, o.start);
                if (cmp != 0) return cmp;
                return Integer.compare(this.end, o.end);
            }
        }

        List<VisDiag> visibleDiags = new ArrayList<>();
        for (dev.lumina.diagnostics.JavaDiagnostics.Diag d : diagnostics) {
            if (isLineHiddenByFold(d.line())) continue;
            int[] range = mapDiagnosticToVisibleRange(d);
            if (range != null && range[0] < length) {
                int s = Math.max(0, range[0]);
                int e = Math.min(length, Math.max(s + 1, range[1]));
                visibleDiags.add(new VisDiag(s, e, d));
            }
        }
        Collections.sort(visibleDiags);

        for (VisDiag vd : visibleDiags) {
            int start = Math.max(vd.start(), last);
            int end = Math.min(vd.end(), length);
            if (start >= end) continue;
            if (start > last) {
                builder.add(java.util.List.of(), start - last);
            }
            dev.lumina.diagnostics.JavaDiagnostics.Diag d = vd.diag();
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

    private int[] mapDiagnosticToVisibleRange(dev.lumina.diagnostics.JavaDiagnostics.Diag d) {
        if (d.line() <= 0) return null;
        if (isLineHiddenByFold(d.line())) {
            return null;
        }
        int visLine = getVisibleLineNumber(d.line());
        int paraIdx = visLine - 1;
        if (paraIdx < 0 || paraIdx >= codeArea.getParagraphs().size()) {
            return null;
        }

        FoldRegion region = visibleLineToFoldedRegion.get(visLine);
        if (region != null && region.isFolded()) {
            if (region.getType() == FoldRegion.RegionType.IMPORTS
                    || region.getType() == FoldRegion.RegionType.COMMENT
                    || region.getType() == FoldRegion.RegionType.CUSTOM) {
                return null;
            }
        }

        int colStart = 0;
        int colEnd = 0;
        if (fullDocumentText != null) {
            String[] fullLines = fullDocumentText.split("\r?\n", -1);
            if (d.line() - 1 < fullLines.length) {
                int lineStartOffset = 0;
                for (int i = 0; i < d.line() - 1; i++) {
                    lineStartOffset += fullLines[i].length() + 1;
                }
                colStart = Math.max(0, d.start() - lineStartOffset);
                colEnd = Math.max(colStart + 1, d.end() - lineStartOffset);
            }
        }

        int paraLen = codeArea.getParagraph(paraIdx).length();
        if (colStart >= paraLen) {
            return null;
        }
        int paraStartInCodeArea = codeArea.getAbsolutePosition(paraIdx, 0);
        int vStart = paraStartInCodeArea + Math.min(colStart, paraLen);
        int vEnd = paraStartInCodeArea + Math.min(colEnd, paraLen);
        if (vEnd <= vStart && paraLen > 0) {
            vEnd = Math.min(vStart + 1, paraStartInCodeArea + paraLen);
        }
        if (vEnd <= vStart) {
            return null;
        }
        return new int[]{vStart, vEnd};
    }

    private dev.lumina.diagnostics.JavaDiagnostics.Diag diagAt(int offset) {
        if (codeArea.getParagraphs().isEmpty()) return null;
        int para = codeArea.offsetToPosition(offset, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor();
        int realLine = getRealLineNumber(para + 1);
        return diagAtLine(realLine);
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
                        String updated = dev.lumina.codegen.JavaCodeGenerator.removeMethod(getEditorText(), targetName);
                        setEditorText(updated);
                        markDirty();
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
                        String updated = dev.lumina.codegen.JavaCodeGenerator.generateAddConstructorParam(getEditorText(), targetField);
                        setEditorText(updated);
                        markDirty();
                        applyHighlighting();
                        scheduleDiagnostics();
                    }));

            var pGs = dev.lumina.codegen.JavaCodeGenerator.previewGettersAndSetters(full, targetField);
            items.add(ContextActionsPopup.ActionItem.fix("create-getter-setter",
                    "Create getter and setter for '" + targetField + "'", pGs, () -> {
                        String updated = dev.lumina.codegen.JavaCodeGenerator.generateGettersAndSetters(getEditorText(), targetField);
                        setEditorText(updated);
                        markDirty();
                        applyHighlighting();
                        scheduleDiagnostics();
                    }));

            var pG = dev.lumina.codegen.JavaCodeGenerator.previewGetter(full, targetField);
            items.add(ContextActionsPopup.ActionItem.fix("create-getter",
                    "Create getter for '" + targetField + "'", pG, () -> {
                        String updated = dev.lumina.codegen.JavaCodeGenerator.generateGetters(getEditorText(), targetField);
                        setEditorText(updated);
                        markDirty();
                        applyHighlighting();
                        scheduleDiagnostics();
                    }));

            var pS = dev.lumina.codegen.JavaCodeGenerator.previewSetter(full, targetField);
            items.add(ContextActionsPopup.ActionItem.fix("create-setter",
                    "Create setter for '" + targetField + "'", pS, () -> {
                        String updated = dev.lumina.codegen.JavaCodeGenerator.generateSetters(getEditorText(), targetField);
                        setEditorText(updated);
                        markDirty();
                        applyHighlighting();
                        scheduleDiagnostics();
                    }));

            var pRem = dev.lumina.codegen.JavaCodeGenerator.previewRemoveField(full, targetField);
            items.add(ContextActionsPopup.ActionItem.fix("remove-field",
                    "Remove field '" + targetField + "'", pRem, () -> {
                        String updated = dev.lumina.codegen.JavaCodeGenerator.removeField(getEditorText(), targetField);
                        setEditorText(updated);
                        markDirty();
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
                String updated = dev.lumina.codegen.JavaCodeGenerator.convertToThreadLocal(getEditorText(), targetField);
                setEditorText(updated);
                markDirty();
                applyHighlighting();
                scheduleDiagnostics();
            }));

            var pAtomic = dev.lumina.codegen.JavaCodeGenerator.previewAtomic(full, targetField);
            items.add(ContextActionsPopup.ActionItem.itemWithPreview("atomic", "Convert to atomic", pAtomic, () -> {
                String updated = dev.lumina.codegen.JavaCodeGenerator.convertToAtomic(getEditorText(), targetField);
                setEditorText(updated);
                markDirty();
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
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateConstructor(getEditorText());
            setEditorText(updated);
            markDirty();
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("logger", "Logger", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateLogger(getEditorText());
            setEditorText(updated);
            markDirty();
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("getter", "Getter", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateGetters(getEditorText());
            setEditorText(updated);
            markDirty();
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("setter", "Setter", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateSetters(getEditorText());
            setEditorText(updated);
            markDirty();
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("getter-setter", "Getter and Setter", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateGettersAndSetters(getEditorText());
            setEditorText(updated);
            markDirty();
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("equals-hashcode", "equals() and hashCode()", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateEqualsAndHashCode(getEditorText());
            setEditorText(updated);
            markDirty();
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("tostring", "toString()", null, null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateToString(getEditorText());
            setEditorText(updated);
            markDirty();
            applyHighlighting();
            scheduleDiagnostics();
        }));
        items.add(GeneratePopup.GenerateItem.of("override", "Override Methods\u2026", "Ctrl+O", null, () -> {
            String updated = dev.lumina.codegen.JavaCodeGenerator.generateToString(getEditorText());
            setEditorText(updated);
            markDirty();
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
                            if (dev.lumina.settings.CodeCompletionSettings.getInstance().isSortAlphabetically()) {
                                return a.name().compareToIgnoreCase(b.name());
                            }
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

    private boolean tryExpandPostfix(javafx.scene.input.KeyCode code) {
        dev.lumina.settings.PostfixCompletionSettings settings = dev.lumina.settings.PostfixCompletionSettings.getInstance();
        if (!settings.isEnablePostfixCompletion()) return false;

        String shortcut = settings.getExpandShortcut();
        boolean matchesKey = switch (shortcut) {
            case "Space" -> code == javafx.scene.input.KeyCode.SPACE;
            case "Enter" -> code == javafx.scene.input.KeyCode.ENTER;
            default -> code == javafx.scene.input.KeyCode.TAB;
        };
        if (!matchesKey) return false;

        int caret = codeArea.getCaretPosition();
        if (caret <= 0) return false;

        int lineIdx = codeArea.getCurrentParagraph();
        String lineText = codeArea.getText(lineIdx);
        int col = codeArea.getCaretColumn();
        if (col <= 0 || col > lineText.length()) return false;

        String beforeCaretInLine = lineText.substring(0, col);
        int lastDot = beforeCaretInLine.lastIndexOf('.');
        if (lastDot < 0) return false;

        String key = beforeCaretInLine.substring(lastDot + 1).trim();
        if (key.isEmpty()) return false;

        int exprStart = lastDot - 1;
        while (exprStart >= 0) {
            char c = beforeCaretInLine.charAt(exprStart);
            if (Character.isWhitespace(c) || c == '(' || c == '[' || c == '{' || c == '=' || c == ',' || c == ';') {
                exprStart++;
                break;
            }
            exprStart--;
        }
        if (exprStart < 0) exprStart = 0;
        String expr = beforeCaretInLine.substring(exprStart, lastDot).trim();
        if (expr.isEmpty()) return false;

        String lang = getLanguageForFile();
        java.util.Optional<String> expanded = settings.expand(lang, key, expr);
        if (expanded.isPresent()) {
            int replaceStart = caret - (beforeCaretInLine.length() - exprStart);
            int replaceEnd = caret;
            String rep = expanded.get();
            codeArea.replaceText(replaceStart, replaceEnd, rep);
            applyHighlighting();
            scheduleDiagnostics();
            return true;
        }
        return false;
    }

    private String getLanguageForFile() {
        if (path == null) return "Java";
        String n = path.getFileName().toString().toLowerCase();
        if (n.endsWith(".java")) return "Java";
        if (n.endsWith(".ts")) return "TypeScript";
        if (n.endsWith(".js")) return "JavaScript";
        if (n.endsWith(".rs")) return "Rust";
        if (n.endsWith(".kt") || n.endsWith(".kts")) return "Kotlin";
        if (n.endsWith(".py")) return "Python";
        if (n.endsWith(".go")) return "Go";
        if (n.endsWith(".sql")) return "SQL";
        if (n.endsWith(".groovy")) return "Groovy";
        if (n.endsWith(".rb")) return "Ruby";
        if (n.endsWith(".scala")) return "Scala";
        if (n.endsWith(".php")) return "PHP";
        if (n.endsWith(".md") || n.endsWith(".markdown")) return "Markdown";
        if (n.endsWith(".json")) return "JSON";
        return "Java";
    }

    private boolean isMarkdownFile() {
        return path != null && (path.toString().endsWith(".md") || path.toString().endsWith(".markdown"));
    }
    private boolean isSqlFile() {
        return path != null && path.toString().endsWith(".sql");
    }
    private boolean isRustFile() {
        return path != null && path.toString().endsWith(".rs");
    }
    private boolean isScalaFile() {
        return path != null && path.toString().endsWith(".scala");
    }
    private boolean isPythonFile() {
        return path != null && path.toString().endsWith(".py");
    }
    private boolean isPhpFile() {
        return path != null && path.toString().endsWith(".php");
    }
    private boolean isRubyFile() {
        return path != null && path.toString().endsWith(".rb");
    }
    private boolean isJsonFile() {
        return path != null && path.toString().endsWith(".json");
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
            if (!dev.lumina.settings.AutoImportSettings.getInstance().isJavaExcluded(item.importFqcn())) {
                String full = codeArea.getText();
                if (dev.lumina.semantics.Completion.needsImport(full, item.importFqcn())) {
                    int offset = dev.lumina.semantics.Completion.importInsertOffset(full);
                    String importLine = "import " + item.importFqcn() + ";\n";
                    codeArea.insertText(offset, importLine);
                    if (offset <= start) shift = importLine.length();
                }
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

    public void paste() {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            String pasted = clipboard.getString();
            SmartKeysSettings sk = SmartKeysSettings.getInstance();

            if (path != null && (path.toString().endsWith(".kt") || path.toString().endsWith(".kts"))
                    && sk.isConvertPastedJavaToKotlin()) {
                pasted = convertJavaSnippetToKotlin(pasted);
            }

            if (sk.getReformatOnPaste() == SmartKeysSettings.ReformatOnPaste.INDENT_EACH_LINE
                    || (isPythonFile() && sk.isPythonSmartIndentPastedLines())
                    || (isScalaFile() && sk.isScalaIndentPastedLinesAtCaret())) {
                int paragraph = codeArea.getCurrentParagraph();
                String line = codeArea.getParagraph(paragraph).getText();
                String baseIndent = line.replaceAll("\\S.*$", "");
                if (!baseIndent.isEmpty() && pasted.contains("\n")) {
                    String[] lines = pasted.split("\n", -1);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < lines.length; i++) {
                        if (i == 0) {
                            sb.append(lines[i]);
                        } else {
                            sb.append("\n").append(baseIndent).append(lines[i].stripLeading());
                        }
                    }
                    pasted = sb.toString();
                }
            }

            if (isPhpFile() && sk.isPhpEscapeTextOnPasteInStringLiterals()) {
                int paragraph = codeArea.getCurrentParagraph();
                String line = codeArea.getParagraph(paragraph).getText();
                int col = codeArea.getCaretColumn();
                String prefix = line.substring(0, Math.min(col, line.length()));
                String suffix = line.substring(Math.min(col, line.length()));
                if ((prefix.endsWith("\"") && suffix.startsWith("\""))
                        || (prefix.endsWith("'") && suffix.startsWith("'"))) {
                    pasted = pasted.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'");
                }
            }

            if (isPhpFile() && sk.isPhpReplaceUnnecessaryDoubleQuotesOnPaste()) {
                if (pasted.startsWith("\"") && pasted.endsWith("\"") && pasted.length() >= 2) {
                    String inside = pasted.substring(1, pasted.length() - 1);
                    if (!inside.contains("$") && !inside.contains("\\")) {
                        pasted = "'" + inside + "'";
                    }
                }
            }

            if (path != null && path.toString().endsWith(".java")) {
                String current = codeArea.getText();
                java.util.List<String> needed = dev.lumina.semantics.AutoImportService.getInstance().resolvePastedImports(current, pasted);
                if (!needed.isEmpty()) {
                    dev.lumina.settings.AutoImportSettings.InsertImportsMode mode =
                            dev.lumina.settings.AutoImportSettings.getInstance().getJavaInsertImportsOnPaste();
                    if (mode == dev.lumina.settings.AutoImportSettings.InsertImportsMode.ALWAYS) {
                        applyPasteWithImports(pasted, needed);
                        return;
                    } else if (mode == dev.lumina.settings.AutoImportSettings.InsertImportsMode.ASK) {
                        promptPasteImports(pasted, needed);
                        return;
                    }
                }
            }

            codeArea.replaceSelection(pasted);
            return;
        }
        codeArea.paste();
    }

    private String convertJavaSnippetToKotlin(String javaCode) {
        if (javaCode == null) return "";
        return javaCode
                .replace("System.out.println", "println")
                .replace("System.out.print", "print")
                .replace("public static void main(String[] args)", "fun main(args: Array<String>)")
                .replace("public static void main(String... args)", "fun main(vararg args: String)")
                .replaceAll("(?m);\\s*$", "")
                .replaceAll("\\bpublic void \\b", "fun ")
                .replaceAll("\\bprivate void \\b", "private fun ")
                .replaceAll("\\bprotected void \\b", "protected fun ")
                .replaceAll("\\bpublic class \\b", "class ")
                .replaceAll("\\bnew \\b", "");
    }

    private void applyPasteWithImports(String pasted, java.util.List<String> needed) {
        String current = codeArea.getText();
        int caret = codeArea.getCaretPosition();
        int insertOffset = dev.lumina.semantics.Completion.importInsertOffset(current);

        StringBuilder sb = new StringBuilder();
        for (String fqcn : needed) {
            if (dev.lumina.semantics.Completion.needsImport(current, fqcn)) {
                sb.append("import ").append(fqcn).append(";\n");
            }
        }
        String importBlock = sb.toString();
        int shift = 0;
        if (!importBlock.isEmpty()) {
            codeArea.insertText(insertOffset, importBlock);
            if (insertOffset <= caret) {
                shift = importBlock.length();
            }
        }
        codeArea.moveTo(caret + shift);
        codeArea.replaceSelection(pasted);
    }

    private void promptPasteImports(String pasted, java.util.List<String> needed) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Insert Imports on Paste");
        alert.setHeaderText("The following classes will be imported:");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(6);
        content.setPadding(new javafx.geometry.Insets(10, 0, 10, 0));
        java.util.List<CheckBox> checkBoxes = new java.util.ArrayList<>();
        for (String fqcn : needed) {
            CheckBox cb = new CheckBox(fqcn);
            cb.setSelected(true);
            cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            checkBoxes.add(cb);
            content.getChildren().add(cb);
        }
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setStyle("-fx-background-color: #2B2D30;");
        var contentLabel = alert.getDialogPane().lookup(".content.label");
        if (contentLabel != null) contentLabel.setStyle("-fx-text-fill: #DFE1E5;");

        ButtonType importBtn = new ButtonType("Import", ButtonBar.ButtonData.OK_DONE);
        ButtonType dontImportBtn = new ButtonType("Don't Import", ButtonBar.ButtonData.NO);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(importBtn, dontImportBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == importBtn) {
                java.util.List<String> chosen = new java.util.ArrayList<>();
                for (CheckBox cb : checkBoxes) {
                    if (cb.isSelected()) chosen.add(cb.getText());
                }
                applyPasteWithImports(pasted, chosen);
            } else if (result.get() == dontImportBtn) {
                codeArea.replaceSelection(pasted);
            }
        }
    }

    public void optimizeImports() {
        if (path != null && path.toString().endsWith(".java")) {
            String current = codeArea.getText();
            String optimized = dev.lumina.semantics.AutoImportService.getInstance().optimizeImports(current);
            if (!optimized.equals(current)) {
                int pos = Math.min(codeArea.getCaretPosition(), optimized.length());
                codeArea.replaceText(optimized);
                codeArea.moveTo(pos);
            }
        }
    }

    public void selectAll() { codeArea.selectAll(); }

    // --------------------------------------------------------- extra tools

    /** Used for decompiled .class views. */
    public void setReadOnly() {
        codeArea.setEditable(false);
    }

    public void goToLine(int line) {
        FoldRegion region = getActiveFoldedRegionAtLine(line);
        if (region != null && region.isFolded()) {
            toggleFoldRegion(region);
        }
        int visibleLine = getVisibleLineNumber(line);
        int target = Math.max(0, Math.min(visibleLine - 1, codeArea.getParagraphs().size() - 1));
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

    // ---------------------------------------------------- code folding
    public int getRealLineNumber(int visibleLine) {
        Integer real = visibleLineToFullLine.get(visibleLine);
        return real != null ? real : visibleLine;
    }

    public int getVisibleLineNumber(int realLine) {
        Integer vis = fullLineToVisibleLine.get(realLine);
        return vis != null ? vis : realLine;
    }

    public int getRealLineCount() {
        if (fullDocumentText == null || fullDocumentText.isEmpty()) {
            return codeArea.getParagraphs().size();
        }
        return fullDocumentText.split("\r?\n", -1).length;
    }

    public boolean isLineHiddenByFold(int realLine) {
        FoldRegion region = getActiveFoldedRegionAtLine(realLine);
        return region != null && region.isFolded() && realLine > region.getStartLine();
    }

    public void rescanFoldRegions() {
        foldRegions.clear();
        if (fullDocumentText == null || fullDocumentText.isEmpty()) {
            return;
        }
        foldRegions.addAll(CodeFoldingScanner.scan(fullDocumentText, baseName));
    }

    public FoldRegion getActiveFoldRegionAtLine(int realLine) {
        FoldRegion best = null;
        for (FoldRegion r : foldRegions) {
            if (r.containsLine(realLine)) {
                if (best == null || r.getLineCount() < best.getLineCount()) {
                    best = r;
                }
            }
        }
        return best;
    }

    public FoldRegion getActiveFoldedRegionAtLine(int realLine) {
        FoldRegion outermostFolded = null;
        for (FoldRegion r : foldRegions) {
            if (r.isFolded() && r.containsLine(realLine)) {
                if (outermostFolded == null || r.getLineCount() > outermostFolded.getLineCount()) {
                    outermostFolded = r;
                }
            }
        }
        return outermostFolded;
    }

    public FoldRegion getFoldRegionAtStartLine(int realLine) {
        FoldRegion found = null;
        for (FoldRegion r : foldRegions) {
            if (r.getStartLine() == realLine) {
                if (found == null || r.isFolded()) {
                    found = r;
                }
            }
        }
        return found;
    }

    public void toggleFoldRegion(FoldRegion region) {
        if (region == null) return;
        fullDocumentText = reconstructFullDocumentText();
        region.toggle();
        boolean wasDirty = dirty;
        applyFoldingView(true);
        if (!wasDirty) {
            dirty = false;
            setText(baseName);
        }
    }

    public void collapseRegionAtCaret() {
        int visibleLine = codeArea.getCurrentParagraph() + 1;
        int realLine = getRealLineNumber(visibleLine);
        FoldRegion region = getActiveFoldRegionAtLine(realLine);
        if (region != null && !region.isFolded()) {
            toggleFoldRegion(region);
        }
    }

    public void expandRegionAtCaret() {
        int visibleLine = codeArea.getCurrentParagraph() + 1;
        FoldRegion foldedRegion = visibleLineToFoldedRegion.get(visibleLine);
        if (foldedRegion != null && foldedRegion.isFolded()) {
            toggleFoldRegion(foldedRegion);
            return;
        }
        int realLine = getRealLineNumber(visibleLine);
        FoldRegion region = getActiveFoldRegionAtLine(realLine);
        if (region != null && region.isFolded()) {
            toggleFoldRegion(region);
        }
    }

    public void collapseAll() {
        fullDocumentText = reconstructFullDocumentText();
        for (FoldRegion r : foldRegions) {
            r.setFolded(true);
        }
        boolean wasDirty = dirty;
        applyFoldingView(true);
        if (!wasDirty) {
            dirty = false;
            setText(baseName);
        }
    }

    public void expandAll() {
        fullDocumentText = reconstructFullDocumentText();
        for (FoldRegion r : foldRegions) {
            r.setFolded(false);
        }
        boolean wasDirty = dirty;
        applyFoldingView(true);
        if (!wasDirty) {
            dirty = false;
            setText(baseName);
        }
    }

    public List<FoldRegion> getFoldRegions() {
        return Collections.unmodifiableList(foldRegions);
    }

    private void syncFullDocumentFromUserEdit() {
        fullDocumentText = reconstructFullDocumentText();
        int curVisLine = codeArea.getCurrentParagraph() + 1;
        FoldRegion foldedOnLine = visibleLineToFoldedRegion.get(curVisLine);
        if (foldedOnLine != null && foldedOnLine.isFolded()) {
            foldedOnLine.setFolded(false);
            Platform.runLater(() -> applyFoldingView(true));
        }
    }

    private String reconstructFullDocumentText() {
        if (visibleLineToFoldedRegion.isEmpty()) {
            return codeArea.getText();
        }
        String[] visibleLines = codeArea.getText().split("\r?\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < visibleLines.length; i++) {
            int visibleLine = i + 1;
            FoldRegion region = visibleLineToFoldedRegion.get(visibleLine);
            if (region != null && region.isFolded() && region.getFoldedContent() != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(region.getFoldedContent());
            } else {
                if (sb.length() > 0) sb.append("\n");
                sb.append(visibleLines[i]);
            }
        }
        return sb.toString();
    }

    public void applyFoldingView(boolean preserveCaret) {
        if (fullDocumentText == null) fullDocumentText = "";
        isApplyingFolding = true;
        try {
            int oldCaretLine = codeArea.getCurrentParagraph() + 1;
            int oldRealLine = getRealLineNumber(oldCaretLine);
            int oldCol = codeArea.getCaretColumn();

            String[] fullLines = fullDocumentText.split("\r?\n", -1);
            StringBuilder visible = new StringBuilder();
            visibleLineToFullLine.clear();
            fullLineToVisibleLine.clear();
            visibleLineToFoldedRegion.clear();

            int currentVisibleLine = 1;
            int lineIdx = 0;
            while (lineIdx < fullLines.length) {
                int fullLine = lineIdx + 1;
                FoldRegion folded = getActiveFoldedRegionAtLine(fullLine);
                if (folded != null && folded.isFolded()) {
                    int startIdx = folded.getStartLine() - 1;
                    int endIdx = Math.min(folded.getEndLine() - 1, fullLines.length - 1);

                    StringBuilder foldedSb = new StringBuilder();
                    for (int k = startIdx; k <= endIdx; k++) {
                        if (foldedSb.length() > 0) foldedSb.append("\n");
                        foldedSb.append(fullLines[k]);
                    }
                    folded.setFoldedContent(foldedSb.toString());

                    String startLineText = startIdx < fullLines.length ? fullLines[startIdx] : "";
                    String indent = getLeadingWhitespace(startLineText);
                    String placeholderLine;
                    if (folded.getType() == FoldRegion.RegionType.IMPORTS) {
                        placeholderLine = indent + "import ...";
                    } else if (folded.getType() == FoldRegion.RegionType.ANNOTATIONS) {
                        placeholderLine = indent + "@{...}";
                    } else if (folded.getType() == FoldRegion.RegionType.METHOD || folded.getType() == FoldRegion.RegionType.CLASS) {
                        int brace = startLineText.indexOf('{');
                        if (brace != -1) {
                            placeholderLine = startLineText.substring(0, brace).stripTrailing() + " {...}";
                        } else {
                            placeholderLine = startLineText.stripTrailing() + " {...}";
                        }
                    } else if (folded.getType() == FoldRegion.RegionType.COMMENT) {
                        placeholderLine = indent + (startLineText.trim().startsWith("/**") ? "/**...*/" : "/*...*/");
                    } else {
                        placeholderLine = indent + folded.getPlaceholder();
                    }

                    if (currentVisibleLine > 1) visible.append("\n");
                    visible.append(placeholderLine);

                    visibleLineToFullLine.put(currentVisibleLine, folded.getStartLine());
                    fullLineToVisibleLine.put(folded.getStartLine(), currentVisibleLine);
                    visibleLineToFoldedRegion.put(currentVisibleLine, folded);

                    for (int k = folded.getStartLine(); k <= folded.getEndLine(); k++) {
                        fullLineToVisibleLine.put(k, currentVisibleLine);
                    }

                    currentVisibleLine++;
                    lineIdx = endIdx + 1;
                } else {
                    if (currentVisibleLine > 1) visible.append("\n");
                    visible.append(fullLines[lineIdx]);

                    visibleLineToFullLine.put(currentVisibleLine, fullLine);
                    fullLineToVisibleLine.put(fullLine, currentVisibleLine);
                    currentVisibleLine++;
                    lineIdx++;
                }
            }

            codeArea.replaceText(visible.toString());

            if (preserveCaret && !codeArea.getParagraphs().isEmpty()) {
                int targetVisibleLine = getVisibleLineNumber(oldRealLine);
                int p = Math.max(0, Math.min(targetVisibleLine - 1, codeArea.getParagraphs().size() - 1));
                int len = codeArea.getParagraph(p).length();
                codeArea.moveTo(p, Math.min(oldCol, len));
                codeArea.requestFollowCaret();
            }

            refreshGutter();
            applyHighlighting();
        } finally {
            isApplyingFolding = false;
        }
    }

    private static String getLeadingWhitespace(String s) {
        if (s == null) return "";
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return s.substring(0, i);
    }
}