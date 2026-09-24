package dev.lumina.ui;

import dev.lumina.run.TestReport;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Authentic test runner panel:
 * - Left vertical toolbar (Rerun, Stop, Auto-test, Filter Passed, Filter Ignored, Sort A-Z, Sort Duration, Expand/Collapse)
 * - Center-left test execution tree with top progress summary and horizontal progress line
 * - Right embedded streaming console output pane
 * - Divided by an adjustable SplitPane
 */
public class TestResultsPanel extends BorderPane {

    private final TreeView<Object> tree = new TreeView<>();
    private final TextArea consoleOutput = new TextArea();

    // Summary components
    private final Label statusIcon = new Label("\u2713");
    private final Label statusLabel = new Label("No tests run yet");
    private final Label statsLabel = new Label("");
    private final Region progressLine = new Region();

    // Toolbar controls
    private final Button rerunBtn = createToolbarButton("\u21BA", "Rerun (Ctrl+F5)");
    private final Button stopBtn = createToolbarButton("\u25A0", "Stop (Ctrl+F2)");
    private final ToggleButton autoTestToggle = createToggleToolButton("\uD83D\uDD01", "Toggle auto-test");
    private final ToggleButton pauseToggle = createToggleToolButton("\u23F8", "Pause execution");
    private final ToggleButton filterPassedBtn = createToggleToolButton("\u2713", "Show Passed");
    private final ToggleButton filterIgnoredBtn = createToggleToolButton("\u2296", "Show Ignored");
    private final ToggleButton sortAlphaBtn = createToggleToolButton("A-Z", "Sort alphabetically");
    private final ToggleButton sortByDurationBtn = createToggleToolButton("\u23F1", "Sort by duration");
    private final Button expandAllBtn = createToolbarButton("\u229E", "Expand All");
    private final Button collapseAllBtn = createToolbarButton("\u229F", "Collapse All");

    private BiConsumer<String, String> navigator;     // (className, method)
    private Runnable onRerun;
    private Consumer<List<TestReport.Case>> onRerunFailed;
    private Runnable onStop;
    private List<TestReport.Suite> lastSuites = List.of();

    private boolean showPassed = true;
    private boolean showIgnored = true;
    private boolean sortAlphabetical = false;
    private boolean sortByDuration = false;

    public TestResultsPanel() {
        getStyleClass().add("console-pane");
        setStyle("-fx-background-color: #1E1F22;");

        // 1. Left Vertical Toolbar
        VBox toolbar = buildVerticalToolbar();

        // 2. Center-Left Test Tree + Summary
        VBox treeContainer = buildTreeContainer();

        // 3. Right Console Pane
        buildConsoleOutput();

        // 4. SplitPane combining Tree and Console
        SplitPane splitPane = new SplitPane(treeContainer, consoleOutput);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.36);
        splitPane.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");

        setLeft(toolbar);
        setCenter(splitPane);
        setMinHeight(160);
    }

    private VBox buildVerticalToolbar() {
        VBox bar = new VBox(2);
        bar.setAlignment(Pos.TOP_CENTER);
        bar.setPadding(new Insets(6, 4, 6, 4));
        bar.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #2B2D30; -fx-border-width: 0 1 0 0;");
        bar.setPrefWidth(32);
        bar.setMinWidth(32);
        bar.setMaxWidth(32);

        rerunBtn.setOnAction(e -> {
            if (onRerun != null) onRerun.run();
        });
        stopBtn.setOnAction(e -> {
            if (onStop != null) onStop.run();
        });

        filterPassedBtn.setSelected(true);
        filterPassedBtn.setOnAction(e -> {
            showPassed = filterPassedBtn.isSelected();
            rebuildTree();
        });

        filterIgnoredBtn.setSelected(true);
        filterIgnoredBtn.setOnAction(e -> {
            showIgnored = filterIgnoredBtn.isSelected();
            rebuildTree();
        });

        sortAlphaBtn.setOnAction(e -> {
            sortAlphabetical = sortAlphaBtn.isSelected();
            if (sortAlphabetical) {
                sortByDuration = false;
                sortByDurationBtn.setSelected(false);
            }
            rebuildTree();
        });

        sortByDurationBtn.setOnAction(e -> {
            sortByDuration = sortByDurationBtn.isSelected();
            if (sortByDuration) {
                sortAlphabetical = false;
                sortAlphaBtn.setSelected(false);
            }
            rebuildTree();
        });

        expandAllBtn.setOnAction(e -> expandAll(true));
        collapseAllBtn.setOnAction(e -> expandAll(false));

        Region sep1 = createToolbarSeparator();
        Region sep2 = createToolbarSeparator();
        Region sep3 = createToolbarSeparator();

        bar.getChildren().addAll(
                rerunBtn, stopBtn, autoTestToggle, pauseToggle,
                sep1,
                filterPassedBtn, filterIgnoredBtn,
                sep2,
                sortAlphaBtn, sortByDurationBtn,
                sep3,
                expandAllBtn, collapseAllBtn
        );
        return bar;
    }

    private Region createToolbarSeparator() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxHeight(1);
        sep.setPrefWidth(20);
        sep.setStyle("-fx-background-color: #2B2D30; -fx-margin: 3 0 3 0;");
        return sep;
    }

    private VBox buildTreeContainer() {
        VBox container = new VBox();
        container.setStyle("-fx-background-color: #1E1F22;");

        // Top Summary Row: status icon + text + stats
        statusIcon.setStyle("-fx-text-fill: #59A869; -fx-font-weight: bold; -fx-font-size: 13px;");
        statusLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 12px;");
        statsLabel.setStyle("-fx-text-fill: #80848B; -fx-font-size: 12px;");

        HBox summaryRow = new HBox(8, statusIcon, statusLabel, statsLabel);
        summaryRow.setAlignment(Pos.CENTER_LEFT);
        summaryRow.setPadding(new Insets(6, 10, 6, 10));
        summaryRow.setStyle("-fx-background-color: #1E1F22;");

        // Thin Horizontal Progress Line
        progressLine.setPrefHeight(2.5);
        progressLine.setMinHeight(2.5);
        progressLine.setMaxHeight(2.5);
        progressLine.setStyle("-fx-background-color: #59A869;");

        // Tree View
        tree.getStyleClass().add("tests-tree");
        tree.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-box-border: transparent;");
        tree.setShowRoot(false);
        tree.setCellFactory(tv -> new ResultCell());
        tree.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && navigator != null) {
                TreeItem<Object> item = tree.getSelectionModel().getSelectedItem();
                if (item != null && item.getValue() instanceof TestReport.Case c) {
                    navigator.accept(c.className(), c.method());
                }
            }
        });
        VBox.setVgrow(tree, Priority.ALWAYS);

        container.getChildren().addAll(summaryRow, progressLine, tree);
        return container;
    }

    private void buildConsoleOutput() {
        consoleOutput.setEditable(false);
        consoleOutput.setWrapText(true);
        consoleOutput.getStyleClass().add("console-output");
        consoleOutput.setStyle("-fx-control-inner-background: #1E1F22; -fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-font-family: monospace; -fx-font-size: 12px; -fx-box-border: transparent;");
        consoleOutput.setPromptText("Test execution output will appear here...");
    }

    private Button createToolbarButton(String glyph, String tooltipText) {
        Button b = new Button(glyph);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #80848B; -fx-font-size: 12px; -fx-padding: 3; -fx-cursor: hand;");
        b.setTooltip(new Tooltip(tooltipText));
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3; -fx-cursor: hand; -fx-background-radius: 4;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #80848B; -fx-font-size: 12px; -fx-padding: 3; -fx-cursor: hand;"));
        b.setPrefSize(24, 24);
        b.setMinSize(24, 24);
        b.setMaxSize(24, 24);
        return b;
    }

    private ToggleButton createToggleToolButton(String glyph, String tooltipText) {
        ToggleButton b = new ToggleButton(glyph);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #80848B; -fx-font-size: 11px; -fx-padding: 3; -fx-cursor: hand;");
        b.setTooltip(new Tooltip(tooltipText));
        b.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) {
                b.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 3; -fx-cursor: hand; -fx-background-radius: 4;");
            } else {
                b.setStyle("-fx-background-color: transparent; -fx-text-fill: #80848B; -fx-font-size: 11px; -fx-padding: 3; -fx-cursor: hand;");
            }
        });
        b.setPrefSize(24, 24);
        b.setMinSize(24, 24);
        b.setMaxSize(24, 24);
        return b;
    }

    /** Navigation callback when double-clicking a test case. */
    public void setNavigator(BiConsumer<String, String> navigator) {
        this.navigator = navigator;
    }

    /** Handlers for rerun, rerun failed, and stop. */
    public void setHandlers(Runnable rerunAll,
                            Consumer<List<TestReport.Case>> rerunFailedTests) {
        setHandlers(rerunAll, rerunFailedTests, null);
    }

    public void setHandlers(Runnable rerunAll,
                            Consumer<List<TestReport.Case>> rerunFailedTests,
                            Runnable stop) {
        this.onRerun = rerunAll;
        this.onRerunFailed = rerunFailedTests;
        this.onStop = stop;
    }

    public void showRunning(String label) {
        statusIcon.setText("\u25B6");
        statusIcon.setStyle("-fx-text-fill: #3574F0; -fx-font-weight: bold; -fx-font-size: 13px;");
        statusLabel.setText("Running " + label + "\u2026");
        statusLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 12px;");
        statsLabel.setText("");
        progressLine.setStyle("-fx-background-color: #3574F0;");

        rerunBtn.setDisable(true);
        stopBtn.setDisable(false);

        TreeItem<Object> runningRoot = new TreeItem<>("running");
        TreeItem<Object> runningNode = new TreeItem<>(label);
        runningRoot.getChildren().add(runningNode);
        tree.setRoot(runningRoot);
        clearConsole();
    }

    public void appendConsole(String line) {
        Platform.runLater(() -> consoleOutput.appendText(line + "\n"));
    }

    public void clearConsole() {
        Platform.runLater(consoleOutput::clear);
    }

    /** Populate the tree from parsed test report suites. */
    public void showResults(List<TestReport.Suite> suites) {
        this.lastSuites = suites != null ? suites : List.of();
        rerunBtn.setDisable(false);
        stopBtn.setDisable(true);

        long passed = 0;
        long failed = 0;
        long skipped = 0;
        double time = 0;

        for (TestReport.Suite s : this.lastSuites) {
            passed += s.passed();
            failed += s.failed();
            skipped += s.skipped();
            time += s.time();
        }

        long total = passed + failed + skipped;
        int timeMs = (int) Math.round(time * 1000);

        if (this.lastSuites.isEmpty() || total == 0) {
            statusIcon.setText("\u2713");
            statusIcon.setStyle("-fx-text-fill: #59A869; -fx-font-weight: bold; -fx-font-size: 13px;");
            statusLabel.setText("No tests found or test run finished");
            statsLabel.setText("");
            progressLine.setStyle("-fx-background-color: #59A869;");
            tree.setRoot(new TreeItem<>("empty"));
            return;
        }

        if (failed == 0) {
            statusIcon.setText("\u2713");
            statusIcon.setStyle("-fx-text-fill: #59A869; -fx-font-weight: bold; -fx-font-size: 13px;");
            statusLabel.setText(passed + " test" + (passed == 1 ? "" : "s") + " passed");
            statsLabel.setText(total + " test" + (total == 1 ? "" : "s") + " total, " + timeMs + " ms");
            progressLine.setStyle("-fx-background-color: #59A869;");
        } else {
            statusIcon.setText("\u2717");
            statusIcon.setStyle("-fx-text-fill: #E06C75; -fx-font-weight: bold; -fx-font-size: 13px;");
            statusLabel.setText(failed + " failed, " + passed + " passed");
            statsLabel.setText(total + " total, " + timeMs + " ms");
            progressLine.setStyle("-fx-background-color: #E06C75;");
        }

        rebuildTree();
    }

    private void rebuildTree() {
        TreeItem<Object> root = new TreeItem<>("results");
        root.setExpanded(true);

        List<TestReport.Suite> suites = new ArrayList<>(lastSuites);
        if (sortAlphabetical) {
            suites.sort(Comparator.comparing(TestReport.Suite::name));
        } else if (sortByDuration) {
            suites.sort((a, b) -> Double.compare(b.time(), a.time()));
        }

        for (TestReport.Suite s : suites) {
            List<TestReport.Case> cases = new ArrayList<>(s.cases());
            if (!showPassed) {
                cases.removeIf(c -> c.status() == TestReport.Status.PASSED);
            }
            if (!showIgnored) {
                cases.removeIf(c -> c.status() == TestReport.Status.SKIPPED);
            }
            if (cases.isEmpty() && !s.cases().isEmpty() && !showPassed && s.failed() == 0) {
                continue;
            }

            if (sortAlphabetical) {
                cases.sort(Comparator.comparing(TestReport.Case::method));
            } else if (sortByDuration) {
                cases.sort((a, b) -> Double.compare(b.time(), a.time()));
            }

            TreeItem<Object> suiteItem = new TreeItem<>(s);
            suiteItem.setExpanded(true);
            for (TestReport.Case c : cases) {
                suiteItem.getChildren().add(new TreeItem<>(c));
            }
            root.getChildren().add(suiteItem);
        }
        tree.setRoot(root);
    }

    private void expandAll(boolean expand) {
        if (tree.getRoot() == null) return;
        setExpandedRecursive(tree.getRoot(), expand);
    }

    private void setExpandedRecursive(TreeItem<Object> item, boolean expand) {
        item.setExpanded(expand);
        for (TreeItem<Object> child : item.getChildren()) {
            setExpandedRecursive(child, expand);
        }
    }

    public List<TestReport.Case> failedCases() {
        return lastSuites.stream()
                .flatMap(s -> s.cases().stream())
                .filter(c -> c.status() == TestReport.Status.FAILED
                        || c.status() == TestReport.Status.ERROR)
                .toList();
    }

    public List<TestReport.Suite> getLastSuites() {
        return lastSuites;
    }

    // ------------------------------------------------------------- rendering

    private static final class ResultCell extends TreeCell<Object> {
        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll("test-pass", "test-fail", "test-skip");
            setTooltip(null);
            setGraphic(null);
            if (empty || item == null) {
                setText(null);
                return;
            }
            if (item instanceof TestReport.Suite s) {
                boolean hasFailure = s.failed() > 0;
                setGraphic(createStatusGraphic(hasFailure ? StatusType.FAILED : StatusType.PASSED));
                int timeMs = (int) Math.round(s.time() * 1000);
                setText(simpleName(s.name()) + "  {" + packageName(s.name()) + "  " + timeMs + " ms}");
                setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            } else if (item instanceof TestReport.Case c) {
                StatusType st = switch (c.status()) {
                    case PASSED -> StatusType.PASSED;
                    case FAILED, ERROR -> StatusType.FAILED;
                    case SKIPPED -> StatusType.SKIPPED;
                };
                setGraphic(createStatusGraphic(st));
                int timeMs = (int) Math.round(c.time() * 1000);
                setText(c.method() + "  " + timeMs + " ms");
                setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                if (!c.message().isBlank()) {
                    setTooltip(new Tooltip(c.message() + "\n\nDouble-click to open test source"));
                }
            } else {
                setText(String.valueOf(item));
                setStyle("-fx-text-fill: #80848B; -fx-font-size: 12px;");
            }
        }

        private enum StatusType { PASSED, FAILED, SKIPPED }

        private Node createStatusGraphic(StatusType type) {
            StackPane root = new StackPane();
            root.setPrefSize(14, 14);
            root.setMinSize(14, 14);
            root.setMaxSize(14, 14);
            root.setAlignment(Pos.CENTER);

            if (type == StatusType.PASSED) {
                Polyline check = new Polyline(
                        -3.5, 0.0,
                        -1.0, 2.8,
                        3.5, -2.5
                );
                check.setStroke(Color.web("#59A869"));
                check.setStrokeWidth(1.6);
                root.getChildren().add(check);
            } else if (type == StatusType.FAILED) {
                Line l1 = new Line(-3.0, -3.0, 3.0, 3.0);
                l1.setStroke(Color.web("#E06C75"));
                l1.setStrokeWidth(1.6);
                Line l2 = new Line(-3.0, 3.0, 3.0, -3.0);
                l2.setStroke(Color.web("#E06C75"));
                l2.setStrokeWidth(1.6);
                root.getChildren().addAll(l1, l2);
            } else {
                Circle c = new Circle(4.0);
                c.setFill(Color.TRANSPARENT);
                c.setStroke(Color.web("#80848B"));
                c.setStrokeWidth(1.2);
                root.getChildren().add(c);
            }
            return root;
        }

        private static String simpleName(String fqcn) {
            int dot = fqcn.lastIndexOf('.');
            return dot >= 0 ? fqcn.substring(dot + 1) : fqcn;
        }

        private static String packageName(String fqcn) {
            int dot = fqcn.lastIndexOf('.');
            return dot >= 0 ? fqcn.substring(0, dot) : "";
        }
    }
}