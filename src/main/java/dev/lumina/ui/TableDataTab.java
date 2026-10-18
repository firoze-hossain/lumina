package dev.lumina.ui;

import dev.lumina.database.DbConnectionManager;
import dev.lumina.database.DbConnectionProfile;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated editor tab for viewing and paginating table data in Lumina.
 */
public class TableDataTab extends Tab {

    private final DbConnectionProfile profile;
    private final String schema;
    private final String tableName;

    private final TableView<ObservableList<String>> tableView = new TableView<>();
    private final Label statusLabel = new Label("Loading...");
    private final Button prevBtn = new Button("\u25C0");
    private final Button nextBtn = new Button("\u25B6");
    private final Label pageLabel = new Label("Page 1");
    private final ComboBox<Integer> pageSizeBox = new ComboBox<>(FXCollections.observableArrayList(50, 100, 250, 500));
    private final TextField filterField = new TextField();

    private int currentPage = 1;
    private final List<String> currentHeaders = new ArrayList<>();
    private final List<ObservableList<String>> currentRows = new ArrayList<>();

    public TableDataTab(DbConnectionProfile profile, String schema, String tableName) {
        this.profile = profile;
        this.schema = schema;
        this.tableName = tableName;

        setText(tableName + " [" + profile.getName() + "]");

        // Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.setStyle("-fx-background-color: #14161E; -fx-border-color: transparent transparent #262A3A transparent;");

        Button refreshBtn = new Button("\u27F3 Refresh");
        refreshBtn.setOnAction(e -> loadPage(currentPage));

        prevBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadPage(currentPage);
            }
        });

        nextBtn.setOnAction(e -> {
            currentPage++;
            loadPage(currentPage);
        });

        pageSizeBox.setValue(100);
        pageSizeBox.setOnAction(e -> {
            currentPage = 1;
            loadPage(currentPage);
        });

        filterField.setPromptText("Filter rows\u2026");
        filterField.setPrefWidth(180);
        filterField.textProperty().addListener((obs, old, txt) -> applyLocalFilter(txt));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportCsvBtn = new Button("Export CSV");
        exportCsvBtn.setOnAction(e -> exportToCsv());

        Button exportJsonBtn = new Button("Export JSON");
        exportJsonBtn.setOnAction(e -> exportToJson());

        toolbar.getChildren().addAll(refreshBtn, prevBtn, pageLabel, nextBtn, new Label("Page Size:"), pageSizeBox, filterField, spacer, exportCsvBtn, exportJsonBtn);

        // Results table
        tableView.getStyleClass().add("db-results");
        tableView.setPlaceholder(new Label("No rows found"));
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Status bar
        HBox statusBar = new HBox(10, statusLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(4, 10, 4, 10));
        statusBar.setStyle("-fx-background-color: #14161E; -fx-border-color: #262A3A transparent transparent transparent;");
        statusLabel.setStyle("-fx-text-fill: #A6ADC4; -fx-font-size: 11px;");

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(tableView);
        root.setBottom(statusBar);

        setContent(root);

        loadPage(1);
    }

    private void loadPage(int page) {
        statusLabel.setText("Fetching rows for " + tableName + "\u2026");
        int pageSize = pageSizeBox.getValue() != null ? pageSizeBox.getValue() : 100;
        int offset = (page - 1) * pageSize;

        String fullTableName = (schema != null && !schema.isBlank() && !"default".equalsIgnoreCase(schema))
                ? schema + "." + tableName : tableName;

        String query = "SELECT * FROM " + fullTableName + " LIMIT " + pageSize + " OFFSET " + offset;
        // SQLite, Postgres, MySQL, MariaDB, StratosDB use LIMIT ... OFFSET ...
        // Oracle 12c+ supports OFFSET ... ROWS FETCH NEXT ... ROWS ONLY
        if (profile.getEngine() == dev.lumina.database.DbEngineType.ORACLE) {
            query = "SELECT * FROM " + fullTableName + " OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY";
        } else if (profile.getEngine() == dev.lumina.database.DbEngineType.SQLSERVER) {
            query = "SELECT * FROM " + fullTableName + " ORDER BY (SELECT NULL) OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY";
        }

        final String finalSql = query;
        long startTime = System.currentTimeMillis();

        Thread t = new Thread(() -> {
            try {
                Connection conn = DbConnectionManager.getInstance().getConnection(profile);
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(finalSql)) {
                    long elapsed = System.currentTimeMillis() - startTime;
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
                        currentHeaders.clear();
                        currentHeaders.addAll(headers);
                        currentRows.clear();
                        currentRows.addAll(rows);

                        populateTable(headers, rows);
                        pageLabel.setText("Page " + page);
                        prevBtn.setDisable(page <= 1);
                        nextBtn.setDisable(rows.size() < pageSize);
                        statusLabel.setText("\u2713 Loaded " + rows.size() + " row(s) in " + elapsed + " ms");
                        statusLabel.setStyle("-fx-text-fill: #50E3C2;");
                    });
                }
            } catch (Throwable ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("\u2717 Failed to load table data: " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #FF6B6B;");
                });
            }
        }, "lumina-table-data-load");
        t.setDaemon(true);
        t.start();
    }

    private void populateTable(List<String> headers, List<ObservableList<String>> rows) {
        tableView.getColumns().clear();

        // Row number
        TableColumn<ObservableList<String>, String> rowNumCol = new TableColumn<>("#");
        rowNumCol.setSortable(false);
        rowNumCol.setPrefWidth(45);
        rowNumCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #697089;");
        rowNumCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(tableView.getItems().indexOf(data.getValue()) + 1)));
        tableView.getColumns().add(rowNumCol);

        // Columns
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
            tableView.getColumns().add(col);
        }

        tableView.getItems().setAll(rows);
    }

    private void applyLocalFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            tableView.getItems().setAll(currentRows);
            return;
        }
        String f = filter.toLowerCase();
        List<ObservableList<String>> filtered = currentRows.stream()
                .filter(row -> row.stream().anyMatch(val -> val != null && val.toLowerCase().contains(f)))
                .toList();
        tableView.getItems().setAll(filtered);
    }

    private void exportToCsv() {
        if (currentRows.isEmpty()) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Table Data to CSV");
        fc.setInitialFileName(tableName + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File f = fc.showSaveDialog(tableView.getScene().getWindow());
        if (f == null) return;

        try (FileWriter fw = new FileWriter(f)) {
            fw.write(String.join(",", currentHeaders.stream().map(h -> "\"" + h.replace("\"", "\"\"") + "\"").toList()) + "\n");
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
        if (currentRows.isEmpty()) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Table Data to JSON");
        fc.setInitialFileName(tableName + ".json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
        File f = fc.showSaveDialog(tableView.getScene().getWindow());
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
