package dev.lumina.ui;

import dev.lumina.run.TestReport;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * IntelliJ-style test results: a summary bar plus a tree of suites and
 * test methods with pass/fail/skip icons. Double-click navigates to the
 * test source; toolbar offers Rerun and Rerun Failed.
 */
public class TestResultsPanel extends BorderPane {

    private final TreeView<Object> tree = new TreeView<>();
    private final Label summary = new Label("No tests run yet \u2014 "
            + "right-click a test or press Ctrl/Cmd+Shift+T");
    private final Button rerun = new Button("\u27F3 Rerun");
    private final Button rerunFailed = new Button("\u27F3 Rerun Failed");

    private BiConsumer<String, String> navigator;     // (className, method)
    private Runnable onRerun;
    private Consumer<List<TestReport.Case>> onRerunFailed;
    private List<TestReport.Suite> lastSuites = List.of();

    public TestResultsPanel() {
        getStyleClass().add("console-pane");

        summary.getStyleClass().add("test-summary");
        rerun.getStyleClass().add("console-button");
        rerunFailed.getStyleClass().add("console-button");
        rerun.setTooltip(new Tooltip("Run the previous test selection again"));
        rerunFailed.setTooltip(new Tooltip("Run only the tests that failed"));
        rerun.setDisable(true);
        rerunFailed.setDisable(true);
        rerun.setOnAction(e -> {
            if (onRerun != null) onRerun.run();
        });
        rerunFailed.setOnAction(e -> {
            if (onRerunFailed != null) onRerunFailed.accept(failedCases());
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, summary, spacer, rerunFailed, rerun);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 10, 6, 10));
        header.getStyleClass().add("console-header");
        setTop(header);

        tree.getStyleClass().add("tests-tree");
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
        setCenter(tree);
    }

    /** (className, methodName) -> open that test in the editor. */
    public void setNavigator(BiConsumer<String, String> navigator) {
        this.navigator = navigator;
    }

    public void setHandlers(Runnable rerunAll,
                            Consumer<List<TestReport.Case>> rerunFailedTests) {
        this.onRerun = rerunAll;
        this.onRerunFailed = rerunFailedTests;
    }

    public void showRunning(String label) {
        summary.setText("\u25B6 Running " + label + "\u2026");
    }

    /** Populate the tree from parsed report suites. */
    public void showResults(List<TestReport.Suite> suites) {
        this.lastSuites = suites;
        TreeItem<Object> root = new TreeItem<>("results");
        long passed = 0;
        long failed = 0;
        long skipped = 0;
        double time = 0;
        for (TestReport.Suite s : suites) {
            passed += s.passed();
            failed += s.failed();
            skipped += s.skipped();
            time += s.time();
            TreeItem<Object> suiteItem = new TreeItem<>(s);
            suiteItem.setExpanded(s.failed() > 0);   // IntelliJ expands failures
            for (TestReport.Case c : s.cases()) {
                suiteItem.getChildren().add(new TreeItem<>(c));
            }
            root.getChildren().add(suiteItem);
        }
        tree.setRoot(root);

        if (suites.isEmpty()) {
            summary.setText("No test reports found \u2014 did the build fail "
                    + "before tests ran? Check the Run tab.");
            rerunFailed.setDisable(true);
            return;
        }
        String text = (failed > 0 ? "\u2717 " : "\u2713 ")
                + passed + " passed"
                + (failed > 0 ? ", " + failed + " failed" : "")
                + (skipped > 0 ? ", " + skipped + " skipped" : "")
                + " \u2014 " + String.format("%.2fs", time);
        summary.setText(text);
        summary.getStyleClass().removeAll("test-summary-pass", "test-summary-fail");
        summary.getStyleClass().add(failed > 0 ? "test-summary-fail" : "test-summary-pass");
        rerun.setDisable(false);
        rerunFailed.setDisable(failed == 0);
    }

    private List<TestReport.Case> failedCases() {
        return lastSuites.stream()
                .flatMap(s -> s.cases().stream())
                .filter(c -> c.status() == TestReport.Status.FAILED
                        || c.status() == TestReport.Status.ERROR)
                .toList();
    }

    // ------------------------------------------------------------- rendering

    private static final class ResultCell extends TreeCell<Object> {
        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll("test-pass", "test-fail", "test-skip");
            setTooltip(null);
            if (empty || item == null) {
                setText(null);
                return;
            }
            if (item instanceof TestReport.Suite s) {
                String icon = s.failed() > 0 ? "\u2717" : "\u2713";
                setText(icon + "  " + simpleName(s.name())
                        + "   (" + s.passed() + "/" + s.cases().size()
                        + " passed, " + String.format("%.2fs", s.time()) + ")");
                getStyleClass().add(s.failed() > 0 ? "test-fail" : "test-pass");
            } else if (item instanceof TestReport.Case c) {
                String icon = switch (c.status()) {
                    case PASSED -> "\u2713";
                    case FAILED, ERROR -> "\u2717";
                    case SKIPPED -> "\u25CB";
                };
                setText(icon + "  " + c.method()
                        + "   " + String.format("%.3fs", c.time()));
                getStyleClass().add(switch (c.status()) {
                    case PASSED -> "test-pass";
                    case FAILED, ERROR -> "test-fail";
                    case SKIPPED -> "test-skip";
                });
                if (!c.message().isBlank()) {
                    setTooltip(new Tooltip(c.message()
                            + "\n\nDouble-click to open the test"));
                }
            } else {
                setText(String.valueOf(item));
            }
        }

        private static String simpleName(String fqcn) {
            int dot = fqcn.lastIndexOf('.');
            return dot >= 0 ? fqcn.substring(dot + 1) : fqcn;
        }
    }
}