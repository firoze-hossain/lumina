package dev.lumina.ui;

import dev.lumina.debugger.DebugStackFrame;
import dev.lumina.debugger.DebugThread;
import dev.lumina.debugger.DebugVariable;
import dev.lumina.debugger.DebuggerService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * IntelliJ IDEA-style Debug tool window matching Images 3 and 5:
 * - Subtabs: [Threads & Variables] and [Console]
 * - Left action toolbar: Rerun, Resume (F9), Pause, Stop (Ctrl+F2), View Breakpoints, Mute Breakpoints
 * - Stepping toolbar: Step Over (F8), Step Into (F7), Force Step Into, Step Out (Shift+F8), Run to Cursor, Evaluate Expression
 * - Left Pane: Call Stack frames with threads
 * - Right Pane: Interactive variables tree and expression evaluator
 */
public class DebugPanel extends BorderPane {

    private final DebuggerService debuggerService;
    private final Label threadLabel = new Label("Disconnected");
    private final ListView<DebugStackFrame> framesList = new ListView<>();
    private final TreeView<DebugVariable> variablesTree = new TreeView<>();
    private final TextArea consoleArea = new TextArea();
    private final TextField evalField = new TextField();

    private Button resumeBtn;
    private Button pauseBtn;
    private Button stopBtn;
    private Button stepOverBtn;
    private Button stepIntoBtn;
    private Button stepOutBtn;
    private Button muteBreakpointsBtn;
    private boolean muted = false;

    private BiConsumer<String, Integer> onNavigateFrame;
    private Runnable onRerun;
    private Runnable onViewBreakpoints;

    public DebugPanel(DebuggerService debuggerService) {
        this.debuggerService = debuggerService;
        getStyleClass().add("debug-tool-window");
        setStyle("-fx-background-color: #1E1F22;");

        setTop(buildTopHeaderAndToolbars());
        setCenter(buildCenterContent());

        debuggerService.addListener(new DebuggerService.DebugListener() {
            @Override
            public void onStateChanged(DebuggerService.State newState) {
                updateButtonStates(newState);
            }

            @Override
            public void onBreakpointHit(DebugThread thread, List<DebugStackFrame> frames, List<DebugVariable> variables, String sourceFile, int line) {
                updateThreadAndFrames(thread, frames);
                updateVariables(variables);
            }

            @Override
            public void onStepped(DebugThread thread, List<DebugStackFrame> frames, List<DebugVariable> variables, String sourceFile, int line) {
                updateThreadAndFrames(thread, frames);
                updateVariables(variables);
            }

            @Override
            public void onVmResumed() {
                threadLabel.setText(threadLabel.getText().replaceAll(": SUSPENDED", ": RUNNING"));
            }

            @Override
            public void onDisconnected() {
                threadLabel.setText("Disconnected");
                framesList.getItems().clear();
                variablesTree.setRoot(null);
            }

            @Override
            public void onLog(String message) {
                consoleArea.appendText(message + "\n");
            }
        });

        framesList.getSelectionModel().selectedItemProperty().addListener((obs, oldF, newF) -> {
            if (newF != null && !newF.isHiddenFrame() && onNavigateFrame != null) {
                debuggerService.selectFrame(newF);
                onNavigateFrame.accept(newF.fileName() != null ? newF.fileName() : newF.className(), newF.lineNumber());
            }
        });

        updateButtonStates(debuggerService.getState());
    }

    public void setOnNavigateFrame(BiConsumer<String, Integer> onNavigateFrame) {
        this.onNavigateFrame = onNavigateFrame;
    }

    public void setOnRerun(Runnable onRerun) {
        this.onRerun = onRerun;
    }

    public void setOnViewBreakpoints(Runnable onViewBreakpoints) {
        this.onViewBreakpoints = onViewBreakpoints;
    }

    public void appendConsole(String text) {
        consoleArea.appendText(text + "\n");
    }

    private Node buildTopHeaderAndToolbars() {
        VBox root = new VBox();
        root.setStyle("-fx-border-color: transparent transparent #32353A transparent; -fx-border-width: 0 0 1 0;");

        // Action Toolbar
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 10, 4, 10));

        Button rerunBtn = actionButton(createRerunIcon(), "Rerun (Ctrl+F5)");
        rerunBtn.setOnAction(e -> { if (onRerun != null) onRerun.run(); });

        stopBtn = actionButton(createStopIcon(), "Stop (Ctrl+F2)");
        stopBtn.setOnAction(e -> debuggerService.disconnect());

        resumeBtn = actionButton(createResumeIcon(), "Resume Program (F9)");
        resumeBtn.setOnAction(e -> debuggerService.resume());

        pauseBtn = actionButton(createPauseIcon(), "Pause Program");
        pauseBtn.setOnAction(e -> debuggerService.pause());

        stepOverBtn = actionButton(createStepOverIcon(), "Step Over (F8)");
        stepOverBtn.setOnAction(e -> debuggerService.stepOver());

        stepIntoBtn = actionButton(createStepIntoIcon(), "Step Into (F7)");
        stepIntoBtn.setOnAction(e -> debuggerService.stepInto());

        stepOutBtn = actionButton(createStepOutIcon(), "Step Out (Shift+F8)");
        stepOutBtn.setOnAction(e -> debuggerService.stepOut());

        Button viewBpBtn = actionButton(createViewBreakpointsIcon(), "View Breakpoints (Ctrl+Shift+F8)");
        viewBpBtn.setOnAction(e -> { if (onViewBreakpoints != null) onViewBreakpoints.run(); });

        muteBreakpointsBtn = actionButton(createMuteBreakpointsIcon(), "Mute Breakpoints");
        muteBreakpointsBtn.setOnAction(e -> {
            muted = !muted;
            muteBreakpointsBtn.setStyle(muted ? "-fx-background-color: rgba(229,75,75,0.25);" : "");
        });

        Separator sep1 = new Separator(Orientation.VERTICAL);
        sep1.setPrefHeight(18);

        Label subTabThreads = new Label("Threads & Variables");
        subTabThreads.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-background-color: #2B2D30; -fx-background-radius: 4; -fx-padding: 3 8 3 8;");

        Label subTabConsole = new Label("Console");
        subTabConsole.setStyle("-fx-text-fill: #7A7E85; -fx-padding: 3 8 3 8; -fx-cursor: hand;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button evalBtn = actionButton(createEvaluateIcon(), "Evaluate Expression (Alt+F8)");
        evalBtn.setOnAction(e -> evalField.requestFocus());

        toolbar.getChildren().addAll(
                rerunBtn, stopBtn, resumeBtn, pauseBtn,
                new Separator(Orientation.VERTICAL),
                stepOverBtn, stepIntoBtn, stepOutBtn,
                new Separator(Orientation.VERTICAL),
                viewBpBtn, muteBreakpointsBtn,
                sep1, subTabThreads, subTabConsole,
                spacer, evalBtn
        );

        root.getChildren().add(toolbar);
        return root;
    }

    private Node buildCenterContent() {
        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.HORIZONTAL);
        split.setStyle("-fx-background-color: transparent; -fx-box-border: transparent;");

        // 1. Left subpane: Frames / Call Stack
        VBox framesPane = new VBox();
        framesPane.setStyle("-fx-background-color: #1E1F22;");

        threadLabel.setStyle("-fx-text-fill: #A8ADBD; -fx-font-size: 11px; -fx-padding: 6 10 6 10; -fx-background-color: #26282E;");
        threadLabel.setMaxWidth(Double.MAX_VALUE);

        framesList.setStyle("-fx-background-color: #1E1F22; -fx-border-width: 0;");
        framesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DebugStackFrame item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.formattedLabel());
                    if (item.isHiddenFrame()) {
                        setStyle("-fx-text-fill: #5A5D63; -fx-font-style: italic; -fx-font-size: 11px; -fx-background-color: transparent;");
                    } else {
                        setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-background-color: transparent;");
                    }
                }
            }
        });
        VBox.setVgrow(framesList, Priority.ALWAYS);

        Label footerHint = new Label("Switch frames from anywhere with Alt+Shift+Up / Alt+Shift+Down");
        footerHint.setStyle("-fx-text-fill: #5A5D63; -fx-font-size: 10px; -fx-padding: 4 10 4 10;");

        framesPane.getChildren().addAll(threadLabel, framesList, footerHint);

        // 2. Right subpane: Variables
        VBox variablesPane = new VBox();
        variablesPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent transparent #32353A; -fx-border-width: 0 0 0 1;");

        evalField.setPromptText("Evaluate expression (Enter) or add a watch (Ctrl+Shift+Enter)");
        evalField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6C707E; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8;");
        evalField.setOnAction(e -> {
            String expr = evalField.getText().trim();
            if (!expr.isEmpty()) {
                evaluateAndDisplay(expr);
            }
        });

        HBox evalBox = new HBox(evalField);
        evalBox.setPadding(new Insets(6, 10, 6, 10));
        HBox.setHgrow(evalField, Priority.ALWAYS);

        variablesTree.setStyle("-fx-background-color: #1E1F22; -fx-border-width: 0;");
        variablesTree.setShowRoot(false);
        variablesTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(DebugVariable item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.displayText());
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-family: monospace; -fx-background-color: transparent;");
                    setGraphic(createVariableIcon(item.kind()));
                }
            }
        });
        VBox.setVgrow(variablesTree, Priority.ALWAYS);

        variablesPane.getChildren().addAll(evalBox, variablesTree);

        split.getItems().addAll(framesPane, variablesPane);
        split.setDividerPositions(0.38);
        return split;
    }

    private void updateThreadAndFrames(DebugThread thread, List<DebugStackFrame> frames) {
        if (thread != null) {
            threadLabel.setText(thread.formattedLabel());
        }
        framesList.getItems().setAll(frames);
        if (!frames.isEmpty()) {
            framesList.getSelectionModel().select(0);
        }
    }

    private void updateVariables(List<DebugVariable> variables) {
        TreeItem<DebugVariable> root = new TreeItem<>(new DebugVariable("root", "", "", "root"));
        for (DebugVariable v : variables) {
            root.getChildren().add(buildTreeItem(v));
        }
        variablesTree.setRoot(root);
    }

    private TreeItem<DebugVariable> buildTreeItem(DebugVariable var) {
        TreeItem<DebugVariable> item = new TreeItem<>(var);
        for (DebugVariable child : var.children()) {
            item.getChildren().add(buildTreeItem(child));
        }
        return item;
    }

    private void evaluateAndDisplay(String expr) {
        TreeItem<DebugVariable> root = variablesTree.getRoot();
        if (root == null) {
            root = new TreeItem<>(new DebugVariable("root", "", "", "root"));
            variablesTree.setRoot(root);
        }
        String evalResult = "Evaluation not supported on this target frame";
        for (TreeItem<DebugVariable> child : root.getChildren()) {
            if (expr.equals(child.getValue().name())) {
                evalResult = child.getValue().valueString();
                break;
            }
        }
        DebugVariable watch = new DebugVariable(expr, "Watch", evalResult, "watch");
        root.getChildren().add(0, new TreeItem<>(watch));
        evalField.clear();
    }

    private void updateButtonStates(DebuggerService.State state) {
        boolean running = (state == DebuggerService.State.RUNNING);
        boolean paused = (state == DebuggerService.State.PAUSED);
        boolean connected = debuggerService.isConnected();

        if (resumeBtn != null) resumeBtn.setDisable(!paused);
        if (pauseBtn != null) pauseBtn.setDisable(!running);
        if (stopBtn != null) stopBtn.setDisable(!connected);
        if (stepOverBtn != null) stepOverBtn.setDisable(!paused);
        if (stepIntoBtn != null) stepIntoBtn.setDisable(!paused);
        if (stepOutBtn != null) stepOutBtn.setDisable(!paused);
    }

    private Button actionButton(Node graphic, String tooltip) {
        Button b = new Button();
        b.setGraphic(graphic);
        b.setTooltip(new Tooltip(tooltip));
        b.setStyle("-fx-background-color: transparent; -fx-padding: 3 5 3 5; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #2B2D30; -fx-background-radius: 4; -fx-padding: 3 5 3 5; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-padding: 3 5 3 5; -fx-cursor: hand;"));
        return b;
    }

    // Vector Action Icons matching IntelliJ IDEA New UI
    private Node createRerunIcon() {
        SVGPath p = new SVGPath();
        p.setContent("M 10 2 A 8 8 0 1 1 2 10 L 4 10 A 6 6 0 1 0 10 4 L 10 7 L 14 3.5 L 10 0 Z");
        p.setFill(Color.web("#59A869"));
        return p;
    }

    private Node createResumeIcon() {
        HBox box = new HBox(2);
        box.setAlignment(Pos.CENTER);
        Line line = new Line(0, 0, 0, 11);
        line.setStroke(Color.web("#59A869"));
        line.setStrokeWidth(2.0);
        Polygon p = new Polygon(0.0, 0.0, 8.0, 5.5, 0.0, 11.0);
        p.setFill(Color.web("#59A869"));
        box.getChildren().addAll(line, p);
        return box;
    }

    private Node createPauseIcon() {
        HBox box = new HBox(3);
        box.setAlignment(Pos.CENTER);
        Rectangle r1 = new Rectangle(3, 11, Color.web("#CED0D6"));
        Rectangle r2 = new Rectangle(3, 11, Color.web("#CED0D6"));
        box.getChildren().addAll(r1, r2);
        return box;
    }

    private Node createStopIcon() {
        Rectangle r = new Rectangle(10, 10, Color.web("#E54B4B"));
        r.setArcWidth(2);
        r.setArcHeight(2);
        return r;
    }

    private Node createStepOverIcon() {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        SVGPath p = new SVGPath();
        p.setContent("M 2 8 C 2 3, 10 3, 10 8 M 8 6 L 10 8 L 12 6");
        p.setStroke(Color.web("#CED0D6"));
        p.setStrokeWidth(1.5);
        p.setFill(Color.TRANSPARENT);
        return p;
    }

    private Node createStepIntoIcon() {
        SVGPath p = new SVGPath();
        p.setContent("M 6 1 L 6 9 M 3 6 L 6 9 L 9 6 M 2 11 L 10 11");
        p.setStroke(Color.web("#CED0D6"));
        p.setStrokeWidth(1.5);
        p.setFill(Color.TRANSPARENT);
        return p;
    }

    private Node createStepOutIcon() {
        SVGPath p = new SVGPath();
        p.setContent("M 6 9 L 6 1 M 3 4 L 6 1 L 9 4 M 2 11 L 10 11");
        p.setStroke(Color.web("#CED0D6"));
        p.setStrokeWidth(1.5);
        p.setFill(Color.TRANSPARENT);
        return p;
    }

    private Node createViewBreakpointsIcon() {
        HBox box = new HBox(-3);
        box.setAlignment(Pos.CENTER);
        Circle c1 = new Circle(4.5, Color.web("#E54B4B"));
        Circle c2 = new Circle(4.5, Color.web("#E54B4B", 0.7));
        box.getChildren().addAll(c1, c2);
        return box;
    }

    private Node createMuteBreakpointsIcon() {
        StackPane sp = new StackPane();
        Circle c = new Circle(5, Color.web("#E54B4B"));
        Line l = new Line(-4, 4, 4, -4);
        l.setStroke(Color.WHITE);
        l.setStrokeWidth(1.5);
        sp.getChildren().addAll(c, l);
        return sp;
    }

    private Node createEvaluateIcon() {
        Label l = new Label("x=");
        l.setStyle("-fx-text-fill: #CED0D6; -fx-font-weight: bold; -fx-font-size: 11px;");
        return l;
    }

    private Node createVariableIcon(String kind) {
        StackPane sp = new StackPane();
        sp.setPrefSize(14, 14);
        Circle bg = new Circle(6);
        Label text = new Label();
        text.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: white;");

        switch (kind) {
            case "this" -> {
                bg.setFill(Color.web("#3574F0"));
                text.setText("C");
            }
            case "param" -> {
                bg.setFill(Color.web("#CF8E6D"));
                text.setText("P");
            }
            case "watch" -> {
                bg.setFill(Color.web("#C77DBB"));
                text.setText("W");
            }
            default -> {
                bg.setFill(Color.web("#59A869"));
                text.setText("V");
            }
        }
        sp.getChildren().addAll(bg, text);
        return sp;
    }
}
