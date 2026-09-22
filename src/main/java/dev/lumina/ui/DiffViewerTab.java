package dev.lumina.ui;

import dev.lumina.diff.DiffEngine;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;

import java.nio.file.Path;
import java.util.*;

/**
 * IntelliJ IDEA-styled Side-by-Side Diff Viewer Tab:
 * - Two-pane comparison: Left = Current working version, Right = Stashed version (read-only)
 * - Difference navigation toolbar: Previous (↑) / Next (↓), Revert chunk (↩),
 *   Side-by-side / Unified mode selector, Whitespace options, Word highlighting selector,
 *   Difference counter badge (e.g. "34 differences"), and Settings gear menu.
 * - Bidirectional synchronized vertical scrolling.
 * - Central connector canvas drawing polygon bands between matching diff chunks.
 * - Gutter line numbers and chunk transition arrows (<< / >>).
 */
public final class DiffViewerTab extends Tab {

    private final String stashRef;
    private final String relativePath;
    private final Path localPath;
    private final String leftText;
    private final String rightText;

    private boolean showLineNumbers = true;
    private boolean ignoreWhitespaces = false;
    private boolean highlightWords = true;

    private DiffEngine.DiffResult diffResult;
    private int currentChunkIndex = -1;

    private final ScrollPane leftScroll = new ScrollPane();
    private final ScrollPane rightScroll = new ScrollPane();
    private final VBox leftLinesBox = new VBox();
    private final VBox rightLinesBox = new VBox();
    private final Canvas connectorCanvas = new Canvas(28, 600);
    private final Label diffCountLabel = new Label("0 differences");

    public DiffViewerTab(String title, String stashRef, String relativePath, Path localPath, String leftText, String rightText) {
        super(title);
        this.stashRef = stashRef;
        this.relativePath = relativePath;
        this.localPath = localPath;
        this.leftText = leftText != null ? leftText : "";
        this.rightText = rightText != null ? rightText : "";

        setGraphic(createDiffTabIcon());
        setClosable(true);

        VBox root = new VBox();
        root.setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(root, Priority.ALWAYS);

        // 1. Top Toolbar
        HBox toolbar = buildToolbar();

        // 2. Diff Panes
        Node diffPanes = buildDiffPanes();
        VBox.setVgrow(diffPanes, Priority.ALWAYS);

        root.getChildren().addAll(toolbar, diffPanes);
        setContent(root);

        // Perform initial diff calculation
        recomputeDiff();
    }

    public String getStashRef() { return stashRef; }
    public String getRelativePath() { return relativePath; }

    private HBox buildToolbar() {
        Button prevBtn = createToolbarIconButton(createUpArrowIcon(), "Previous Difference (Ctrl+Shift+Up)");
        prevBtn.setOnAction(e -> navigateDifference(-1));

        Button nextBtn = createToolbarIconButton(createDownArrowIcon(), "Next Difference (Ctrl+Shift+Down)");
        nextBtn.setOnAction(e -> navigateDifference(1));

        Button revertBtn = createToolbarIconButton(createRollbackIcon(), "Revert Difference Chunk");
        revertBtn.setOnAction(e -> revertCurrentChunk());

        ComboBox<String> viewModeCombo = new ComboBox<>();
        viewModeCombo.getItems().addAll("Side-by-side viewer", "Unified viewer");
        viewModeCombo.setValue("Side-by-side viewer");
        styleComboBox(viewModeCombo);

        ComboBox<String> whitespaceCombo = new ComboBox<>();
        whitespaceCombo.getItems().addAll("Do not ignore", "Ignore whitespaces", "Ignore inner whitespaces");
        whitespaceCombo.setValue("Do not ignore");
        styleComboBox(whitespaceCombo);
        whitespaceCombo.setOnAction(e -> {
            ignoreWhitespaces = !whitespaceCombo.getValue().equals("Do not ignore");
            recomputeDiff();
        });

        ComboBox<String> highlightCombo = new ComboBox<>();
        highlightCombo.getItems().addAll("Highlight words", "Highlight lines", "Highlight split");
        highlightCombo.setValue("Highlight words");
        styleComboBox(highlightCombo);
        highlightCombo.setOnAction(e -> {
            highlightWords = highlightCombo.getValue().contains("words");
            recomputeDiff();
        });

        Button settingsBtn = createToolbarIconButton(createGearIcon(), "Diff Settings");
        settingsBtn.setOnAction(e -> showDiffSettingsMenu(settingsBtn));

        Button helpBtn = createToolbarIconButton(createHelpIcon(), "Help");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        diffCountLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px; -fx-font-weight: normal;");

        HBox toolbar = new HBox(4, prevBtn, nextBtn, revertBtn, viewModeCombo, whitespaceCombo, highlightCombo, settingsBtn, helpBtn, spacer, diffCountLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 10, 4, 10));
        toolbar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2E3136; -fx-border-width: 0 0 1 0;");
        return toolbar;
    }

    private Node buildDiffPanes() {
        // Headers
        Label leftTitle = new Label("Current version  " + (relativePath != null ? relativePath : ""));
        leftTitle.setGraphic(createWarningIcon());
        leftTitle.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-font-family: monospace;");
        HBox leftHeader = new HBox(6, leftTitle);
        leftHeader.setAlignment(Pos.CENTER_LEFT);
        leftHeader.setPadding(new Insets(4, 8, 4, 8));
        leftHeader.setStyle("-fx-background-color: #26282E; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        Label rightTitle = new Label(stashRef != null ? stashRef : "Stash");
        rightTitle.setGraphic(createLockIcon());
        rightTitle.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-font-family: monospace;");
        HBox rightHeader = new HBox(6, rightTitle);
        rightHeader.setAlignment(Pos.CENTER_LEFT);
        rightHeader.setPadding(new Insets(4, 8, 4, 8));
        rightHeader.setStyle("-fx-background-color: #26282E; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        // ScrollPanes
        setupScrollPane(leftScroll, leftLinesBox);
        setupScrollPane(rightScroll, rightLinesBox);

        // Bidirectional synchronized vertical scrolling
        leftScroll.vvalueProperty().bindBidirectional(rightScroll.vvalueProperty());
        leftScroll.vvalueProperty().addListener((obs, old, val) -> drawConnectors());

        VBox leftColumn = new VBox(leftHeader, leftScroll);
        VBox.setVgrow(leftScroll, Priority.ALWAYS);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        VBox rightColumn = new VBox(rightHeader, rightScroll);
        VBox.setVgrow(rightScroll, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        StackPane centerPane = new StackPane(connectorCanvas);
        centerPane.setPrefWidth(28);
        centerPane.setMinWidth(28);
        centerPane.setMaxWidth(28);
        centerPane.setStyle("-fx-background-color: #1E1F22;");

        connectorCanvas.widthProperty().bind(centerPane.widthProperty());
        connectorCanvas.heightProperty().bind(centerPane.heightProperty());
        connectorCanvas.heightProperty().addListener((obs, old, val) -> drawConnectors());

        HBox split = new HBox(leftColumn, centerPane, rightColumn);
        split.setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(split, Priority.ALWAYS);
        return split;
    }

    private void setupScrollPane(ScrollPane sp, VBox content) {
        content.setStyle("-fx-background-color: #1E1F22;");
        sp.setContent(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: transparent;");
    }

    private void recomputeDiff() {
        diffResult = DiffEngine.diff(leftText, rightText, ignoreWhitespaces);
        int diffCount = diffResult.differenceCount();
        diffCountLabel.setText(diffCount + " difference" + (diffCount == 1 ? "" : "s"));

        renderLines();
        Platform.runLater(this::drawConnectors);
    }

    private void renderLines() {
        leftLinesBox.getChildren().clear();
        rightLinesBox.getChildren().clear();

        List<String> leftLines = diffResult.leftLines();
        List<String> rightLines = diffResult.rightLines();
        List<DiffEngine.DiffChunk> chunks = diffResult.chunks();

        // Map line index to chunk
        Map<Integer, DiffEngine.DiffChunk> leftChunkMap = new HashMap<>();
        Map<Integer, DiffEngine.DiffChunk> rightChunkMap = new HashMap<>();
        for (DiffEngine.DiffChunk c : chunks) {
            for (int i = c.leftStart(); i < c.leftEnd(); i++) leftChunkMap.put(i, c);
            for (int j = c.rightStart(); j < c.rightEnd(); j++) rightChunkMap.put(j, c);
        }

        // Render left lines
        for (int i = 0; i < leftLines.size(); i++) {
            DiffEngine.DiffChunk chunk = leftChunkMap.get(i);
            Node lineRow = buildLineRow(i + 1, leftLines.get(i), chunk, true);
            leftLinesBox.getChildren().add(lineRow);
        }

        // Render right lines
        for (int j = 0; j < rightLines.size(); j++) {
            DiffEngine.DiffChunk chunk = rightChunkMap.get(j);
            Node lineRow = buildLineRow(j + 1, rightLines.get(j), chunk, false);
            rightLinesBox.getChildren().add(lineRow);
        }
    }

    private Node buildLineRow(int lineNum, String lineText, DiffEngine.DiffChunk chunk, boolean isLeft) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinHeight(20);
        row.setPrefHeight(20);

        // Background color
        String bg = "#1E1F22";
        if (chunk != null) {
            bg = switch (chunk.type()) {
                case MODIFIED -> "#24344D";
                case INSERTED -> isLeft ? "#1E1F22" : "#223B2D";
                case DELETED -> isLeft ? "#3B2225" : "#1E1F22";
            };
        }
        row.setStyle("-fx-background-color: " + bg + ";");

        // Line number gutter
        if (showLineNumbers) {
            Label numLabel = new Label(String.valueOf(lineNum));
            numLabel.setPrefWidth(42);
            numLabel.setAlignment(Pos.CENTER_RIGHT);
            numLabel.setPadding(new Insets(0, 8, 0, 0));
            numLabel.setStyle("-fx-text-fill: #5C616B; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px;");
            row.getChildren().add(numLabel);
        }

        // Chunk transition chevron arrow in gutter for left pane
        if (isLeft) {
            Label arrowLabel = new Label();
            arrowLabel.setPrefWidth(16);
            arrowLabel.setAlignment(Pos.CENTER);
            if (chunk != null && (chunk.leftStart() == lineNum - 1)) {
                arrowLabel.setText("\u00AB"); // «
                arrowLabel.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;");
                arrowLabel.setTooltip(new Tooltip("Revert this difference chunk"));
                arrowLabel.setOnMouseClicked(e -> revertChunk(chunk));
            }
            row.getChildren().add(arrowLabel);
        }

        // Line text
        Label textLabel = new Label(lineText);
        textLabel.setFont(Font.font("JetBrains Mono", 12));
        textLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-family: 'JetBrains Mono', Menlo, monospace; -fx-font-size: 12px;");
        row.getChildren().add(textLabel);

        return row;
    }

    private void drawConnectors() {
        GraphicsContext gc = connectorCanvas.getGraphicsContext2D();
        double w = connectorCanvas.getWidth();
        double h = connectorCanvas.getHeight();
        gc.clearRect(0, 0, w, h);

        if (diffResult == null || diffResult.chunks().isEmpty()) return;

        double lineHeight = 20.0;
        double scrollOffset = leftScroll.getVvalue() * Math.max(0, leftLinesBox.getHeight() - leftScroll.getHeight());

        for (DiffEngine.DiffChunk c : diffResult.chunks()) {
            double y1 = (c.leftStart() * lineHeight) - scrollOffset;
            double y2 = (c.leftEnd() * lineHeight) - scrollOffset;
            double y3 = (c.rightStart() * lineHeight) - scrollOffset;
            double y4 = (c.rightEnd() * lineHeight) - scrollOffset;

            Color fill = switch (c.type()) {
                case MODIFIED -> Color.web("#2E436E", 0.65);
                case INSERTED -> Color.web("#2A523A", 0.65);
                case DELETED -> Color.web("#5E2B30", 0.65);
            };

            gc.setFill(fill);
            gc.beginPath();
            gc.moveTo(0, y1);
            gc.bezierCurveTo(w * 0.5, y1, w * 0.5, y3, w, y3);
            gc.lineTo(w, y4);
            gc.bezierCurveTo(w * 0.5, y4, w * 0.5, y2, 0, y2);
            gc.closePath();
            gc.fill();

            // Border lines
            gc.setStroke(fill.brighter());
            gc.setLineWidth(1.0);
            gc.strokeLine(0, y1, w, y3);
            gc.strokeLine(0, y2, w, y4);
        }
    }

    private void navigateDifference(int direction) {
        if (diffResult == null || diffResult.chunks().isEmpty()) return;
        int count = diffResult.chunks().size();
        currentChunkIndex = (currentChunkIndex + direction + count) % count;

        DiffEngine.DiffChunk chunk = diffResult.chunks().get(currentChunkIndex);
        double targetLine = chunk.leftStart();
        double totalLines = Math.max(1, diffResult.leftLines().size());
        double vval = targetLine / totalLines;
        leftScroll.setVvalue(Math.min(1.0, Math.max(0.0, vval)));
        drawConnectors();
    }

    private void revertCurrentChunk() {
        if (diffResult != null && !diffResult.chunks().isEmpty()) {
            int idx = currentChunkIndex >= 0 ? currentChunkIndex : 0;
            revertChunk(diffResult.chunks().get(idx));
        }
    }

    private void revertChunk(DiffEngine.DiffChunk chunk) {
        if (chunk == null || localPath == null) return;
        try {
            List<String> curLines = new ArrayList<>(diffResult.leftLines());
            List<String> stashLines = diffResult.rightLines();

            List<String> replacement = stashLines.subList(chunk.rightStart(), chunk.rightEnd());
            for (int i = 0; i < chunk.leftCount(); i++) {
                if (chunk.leftStart() < curLines.size()) {
                    curLines.remove(chunk.leftStart());
                }
            }
            curLines.addAll(chunk.leftStart(), replacement);

            String newContent = String.join("\n", curLines);
            java.nio.file.Files.writeString(localPath, newContent);
            recomputeDiff();
        } catch (Exception ignored) {}
    }

    private void showDiffSettingsMenu(Button anchor) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-text-fill: #DFE1E5;");

        MenuItem blameItem = new MenuItem("Annotate with Git Blame");
        blameItem.setStyle("-fx-text-fill: #DFE1E5;");

        MenuItem sepWinItem = new MenuItem("Show Diff in Separate Window");
        sepWinItem.setStyle("-fx-text-fill: #DFE1E5;");

        SeparatorMenuItem sep1 = new SeparatorMenuItem();

        CheckMenuItem whitespaceItem = new CheckMenuItem("Show Whitespaces");
        whitespaceItem.setStyle("-fx-text-fill: #DFE1E5;");

        CheckMenuItem lineNumItem = new CheckMenuItem("Show Line Numbers");
        lineNumItem.setSelected(showLineNumbers);
        lineNumItem.setStyle("-fx-text-fill: #DFE1E5;");
        lineNumItem.setOnAction(e -> {
            showLineNumbers = lineNumItem.isSelected();
            renderLines();
        });

        CheckMenuItem indentGuidesItem = new CheckMenuItem("Show Indent Guides");
        indentGuidesItem.setSelected(true);
        indentGuidesItem.setStyle("-fx-text-fill: #DFE1E5;");

        CheckMenuItem softWrapItem = new CheckMenuItem("Soft-Wrap");
        softWrapItem.setStyle("-fx-text-fill: #DFE1E5;");

        Menu highlightLevelMenu = new Menu("Highlighting Level");
        highlightLevelMenu.getItems().addAll(new MenuItem("All Problems"), new MenuItem("Syntax Only"), new MenuItem("None"));

        Menu breadcrumbsMenu = new Menu("Breadcrumbs");
        breadcrumbsMenu.getItems().addAll(new MenuItem("Top"), new MenuItem("Bottom"), new MenuItem("Don't Show"));

        CheckMenuItem alignChangesItem = new CheckMenuItem("Align Changes in Side-by-Side Diff");
        alignChangesItem.setSelected(true);
        alignChangesItem.setStyle("-fx-text-fill: #DFE1E5;");

        menu.getItems().addAll(
                blameItem, sepWinItem, sep1,
                whitespaceItem, lineNumItem, indentGuidesItem, softWrapItem,
                highlightLevelMenu, breadcrumbsMenu, alignChangesItem
        );
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void styleComboBox(ComboBox<String> combo) {
        combo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 11px;");
    }

    private Button createToolbarIconButton(Node icon, String tooltip) {
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 5 3 5; -fx-cursor: hand; -fx-background-radius: 4;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373C; -fx-padding: 3 5 3 5; -fx-cursor: hand; -fx-background-radius: 4;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 5 3 5; -fx-cursor: hand; -fx-background-radius: 4;"));
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    private static Node createDiffTabIcon() {
        SVGPath branch = new SVGPath();
        branch.setContent("M 3 3 L 3 13 M 3 6 C 3 9, 10 7, 10 10 L 10 13");
        branch.setFill(Color.TRANSPARENT);
        branch.setStroke(Color.web("#56A8F5"));
        branch.setStrokeWidth(1.2);
        StackPane sp = new StackPane(branch);
        sp.setPrefSize(14, 14);
        return sp;
    }

    private static Node createUpArrowIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 3 10 L 8 4 L 13 10");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web("#AFB1B6"));
        path.setStrokeWidth(1.4);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        return sized(path);
    }

    private static Node createDownArrowIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 3 6 L 8 12 L 13 6");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web("#AFB1B6"));
        path.setStrokeWidth(1.4);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        return sized(path);
    }

    private static Node createRollbackIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 5 5 L 2 8 L 5 11 M 2 8 L 9 8 C 11 8 13 9.5 13 12");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web("#AFB1B6"));
        path.setStrokeWidth(1.35);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        return sized(path);
    }

    private static Node createGearIcon() {
        Label l = new Label("\u2699");
        l.setStyle("-fx-text-fill: #AFB1B6; -fx-font-size: 13px;");
        return sized(l);
    }

    private static Node createHelpIcon() {
        Label l = new Label("?");
        l.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-font-weight: bold;");
        return sized(l);
    }

    private static Node createWarningIcon() {
        SVGPath warn = new SVGPath();
        warn.setContent("M 7 1 L 13 13 L 1 13 Z");
        warn.setFill(Color.web("#E5C07B"));
        StackPane sp = new StackPane(warn);
        sp.setPrefSize(12, 12);
        return sp;
    }

    private static Node createLockIcon() {
        SVGPath lock = new SVGPath();
        lock.setContent("M 3 6 L 3 4 C 3 2.3, 4.3 1, 6 1 C 7.7 1, 9 2.3, 9 4 L 9 6 M 2 6 L 10 6 L 10 12 L 2 12 Z");
        lock.setFill(Color.TRANSPARENT);
        lock.setStroke(Color.web("#868A91"));
        lock.setStrokeWidth(1.2);
        StackPane sp = new StackPane(lock);
        sp.setPrefSize(12, 12);
        return sp;
    }

    private static Node sized(Node n) {
        StackPane sp = new StackPane(n);
        sp.setPrefSize(16, 16);
        sp.setAlignment(Pos.CENTER);
        return sp;
    }
}
