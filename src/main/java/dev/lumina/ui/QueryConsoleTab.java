package dev.lumina.ui;

import dev.lumina.database.DbConnectionManager;
import dev.lumina.database.DbConnectionProfile;
import dev.lumina.syntax.SqlSyntaxHighlighter;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Database Query Console tab in Lumina, opened in the central editor area.
 */
public class QueryConsoleTab extends Tab {

    private final DbConnectionProfile profile;
    private final CodeArea codeArea = new CodeArea();
    private final TableView<ObservableList<String>> resultsTable = new TableView<>();
    private final Label statusLabel = new Label("Ready");
    private final Button runBtn = new Button("\u25B6 Run");
    private final Button cancelBtn = new Button("\u25A0 Cancel");
    private final ComboBox<String> limitBox = new ComboBox<>(FXCollections.observableArrayList("100", "500", "1000", "All"));
    private final Label connectionChip = new Label();

    private volatile Statement runningStatement;
    private final List<String> currentHeaders = new ArrayList<>();
    private final List<ObservableList<String>> currentRows = new ArrayList<>();

    public QueryConsoleTab(DbConnectionProfile profile) {
        this.profile = profile;
        setText("console.sql [" + profile.getName() + "]");

        // Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.setStyle("-fx-background-color: #14161E; -fx-border-color: transparent transparent #262A3A transparent;");

        runBtn.getStyleClass().add("dialog-primary");
        runBtn.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");
        runBtn.setTooltip(new Tooltip("Execute Query (Ctrl+Enter)"));
        runBtn.setOnAction(e -> executeQuery());

        cancelBtn.setStyle("-fx-cursor: hand;");
        cancelBtn.setDisable(true);
        cancelBtn.setOnAction(e -> cancelQuery());

        limitBox.setValue("500");
        limitBox.setPrefWidth(90);

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> {
            codeArea.clear();
            clearResults("Console cleared");
        });

        connectionChip.setText("\u25CF " + profile.getEngine().getDisplayName() + " - " + profile.getName());
        connectionChip.setStyle("-fx-text-fill: #50E3C2; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportCsvBtn = new Button("Export CSV");
        exportCsvBtn.setOnAction(e -> exportToCsv());

        Button exportJsonBtn = new Button("Export JSON");
        exportJsonBtn.setOnAction(e -> exportToJson());

        toolbar.getChildren().addAll(runBtn, cancelBtn, new Label("Limit:"), limitBox, clearBtn, spacer, connectionChip, exportCsvBtn, exportJsonBtn);

        // CodeArea setup
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(100))
                .subscribe(ignore -> codeArea.setStyleSpans(0, SqlSyntaxHighlighter.computeHighlighting(codeArea.getText())));

        // Sample query
        codeArea.replaceText("-- SQL Query Console for " + profile.getName() + "\nSELECT * FROM \n");

        // Key shortcut Ctrl+Enter to run
        codeArea.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN).match(e)) {
                executeQuery();
                e.consume();
            }
        });

        VirtualizedScrollPane<CodeArea> editorScrollPane = new VirtualizedScrollPane<>(codeArea);

        // Results TableView
        resultsTable.getStyleClass().add("db-results");
        resultsTable.setPlaceholder(new Label("Run a query to view results"));
        resultsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Status bar
        HBox statusBar = new HBox(10, statusLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(4, 10, 4, 10));
        statusBar.setStyle("-fx-background-color: #14161E; -fx-border-color: #262A3A transparent transparent transparent;");
        statusLabel.setStyle("-fx-text-fill: #A6ADC4; -fx-font-size: 11px;");

        BorderPane resultsPane = new BorderPane();
        resultsPane.setCenter(resultsTable);
        resultsPane.setBottom(statusBar);

        // Split editor & results
        SplitPane splitPane = new SplitPane(editorScrollPane, resultsPane);
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPositions(0.40);

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(splitPane);

        setContent(root);
    }

    public void setQueryText(String sql) {
        codeArea.replaceText(sql);
        codeArea.moveTo(codeArea.getLength());
    }

    private void executeQuery() {
        String selected = codeArea.getSelectedText().trim();
        String sql = !selected.isEmpty() ? selected : codeArea.getText().trim();
        if (sql.isEmpty()) {
            statusLabel.setText("Please enter a SQL statement to execute.");
            return;
        }

        runBtn.setDisable(true);
        cancelBtn.setDisable(false);
        statusLabel.setText("Executing query\u2026");
        statusLabel.setStyle("-fx-text-fill: #F2D9A6;");

        int limit = 500;
        try {
            String limitStr = limitBox.getValue();
            if (!"All".equalsIgnoreCase(limitStr)) {
                limit = Integer.parseInt(limitStr);
            } else {
                limit = 0;
            }
        } catch (Throwable ignored) {}

        final int maxRows = limit;
        long startTime = System.currentTimeMillis();

        Thread t = new Thread(() -> {
            try {
                Connection conn = DbConnectionManager.getInstance().getConnection(profile);
                Statement stmt = conn.createStatement();
                runningStatement = stmt;
                if (maxRows > 0) stmt.setMaxRows(maxRows);

                boolean hasResultSet = stmt.execute(sql);
                long elapsed = System.currentTimeMillis() - startTime;

                if (!hasResultSet) {
                    int affected = stmt.getUpdateCount();
                    Platform.runLater(() -> {
                        clearResults("Statement executed successfully in " + elapsed + " ms (" + affected + " row(s) affected)");
                        runBtn.setDisable(false);
                        cancelBtn.setDisable(false);
                    });
                    return;
                }

                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    List<String> headers = new ArrayList<>();
                    for (int i = 1; i <= colCount; i++) {
                        headers.add(meta.getColumnLabel(i));
                    }

                    List<ObservableList<String>> rows = new ArrayList<>();
                    while (rs.next()) {
                        ObservableList<String> row = FXCollections.observableArrayList();
                        for (int i = 1; i <= colCount; i++) {
                            Object val = rs.getObject(i);
                            row.add(val != null ? String.valueOf(val) : "NULL");
                        }
                        rows.add(row);
                    }

                    Platform.runLater(() -> {
                        populateResults(headers, rows);
                        statusLabel.setText("\u2713 " + rows.size() + " row(s) retrieved in " + elapsed + " ms");
                        statusLabel.setStyle("-fx-text-fill: #50E3C2; -fx-font-weight: bold;");
                        runBtn.setDisable(false);
                        cancelBtn.setDisable(true);
                    });
                }
            } catch (SQLException ex) {
                long elapsed = System.currentTimeMillis() - startTime;
                Platform.runLater(() -> {
                    statusLabel.setText("\u2717 Error (" + elapsed + " ms): " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #FF6B6B;");
                    runBtn.setDisable(false);
                    cancelBtn.setDisable(true);
                });
            } catch (Throwable ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("\u2717 Connection Error: " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #FF6B6B;");
                    runBtn.setDisable(false);
                    cancelBtn.setDisable(true);
                });
            } finally {
                runningStatement = null;
            }
        }, "lumina-query-exec");
        t.setDaemon(true);
        t.start();
    }

    private void cancelQuery() {
        if (runningStatement != null) {
            try {
                runningStatement.cancel();
                statusLabel.setText("Query execution cancelled.");
                cancelBtn.setDisable(true);
            } catch (Throwable ignored) {}
        }
    }

    private void clearResults(String message) {
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();
        currentHeaders.clear();
        currentRows.clear();
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #A6ADC4;");
    }

    private void populateResults(List<String> headers, List<ObservableList<String>> rows) {
        currentHeaders.clear();
        currentHeaders.addAll(headers);
        currentRows.clear();
        currentRows.addAll(rows);

        resultsTable.getColumns().clear();

        // 1. Row number column (#)
        TableColumn<ObservableList<String>, String> rowNumCol = new TableColumn<>("#");
        rowNumCol.setSortable(false);
        rowNumCol.setPrefWidth(45);
        rowNumCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #697089;");
        rowNumCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(resultsTable.getItems().indexOf(data.getValue()) + 1)));
        resultsTable.getColumns().add(rowNumCol);

        // 2. Data columns
        for (int i = 0; i < headers.size(); i++) {
            final int colIdx = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(headers.get(i));
            col.setCellValueFactory(cell -> {
                ObservableList<String> r = cell.getValue();
                return new ReadOnlyStringWrapper(colIdx < r.size() ? r.get(colIdx) : "");
            });
            col.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else if ("NULL".equals(item)) {
                        setText("NULL");
                        setStyle("-fx-text-fill: #697089; -fx-font-style: italic;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #C9CDDC;");
                    }
                }
            });
            col.setPrefWidth(130);
            resultsTable.getColumns().add(col);
        }

        resultsTable.getItems().setAll(rows);
    }

    private void exportToCsv() {
        if (currentRows.isEmpty()) {
            statusLabel.setText("No results to export.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Query Results to CSV");
        fc.setInitialFileName("query_result.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File f = fc.showSaveDialog(codeArea.getScene().getWindow());
        if (f == null) return;

        try (FileWriter fw = new FileWriter(f)) {
            // Header
            fw.write(String.join(",", currentHeaders.stream().map(h -> "\"" + h.replace("\"", "\"\"") + "\"").toList()) + "\n");
            // Rows
            for (ObservableList<String> r : currentRows) {
                List<String> escaped = new ArrayList<>();
                for (String val : r) {
                    escaped.add("\"" + (val != null ? val.replace("\"", "\"\"") : "") + "\"");
                }
                fw.write(String.join(",", escaped) + "\n");
            }
            statusLabel.setText("\u2713 Exported " + currentRows.size() + " rows to " + f.getName());
        } catch (Throwable ex) {
            statusLabel.setText("\u2717 Export error: " + ex.getMessage());
        }
    }

    private void exportToJson() {
        if (currentRows.isEmpty()) {
            statusLabel.setText("No results to export.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Query Results to JSON");
        fc.setInitialFileName("query_result.json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
        File f = fc.showSaveDialog(codeArea.getScene().getWindow());
        if (f == null) return;

        try (FileWriter fw = new FileWriter(f)) {
            fw.write("[\n");
            for (int r = 0; r < currentRows.size(); r++) {
                ObservableList<String> row = currentRows.get(r);
                fw.write("  {\n");
                for (int c = 0; c < currentHeaders.size(); c++) {
                    String h = currentHeaders.get(c);
                    String val = c < row.size() ? row.get(c) : null;
                    fw.write("    \"" + h + "\": " + ("NULL".equals(val) || val == null ? "null" : "\"" + val.replace("\"", "\\\"") + "\""));
                    if (c < currentHeaders.size() - 1) fw.write(",");
                    fw.write("\n");
                }
                fw.write(r < currentRows.size() - 1 ? "  },\n" : "  }\n");
            }
            fw.write("]\n");
            statusLabel.setText("\u2713 Exported JSON to " + f.getName());
        } catch (Throwable ex) {
            statusLabel.setText("\u2717 Export error: " + ex.getMessage());
        }
    }
}
