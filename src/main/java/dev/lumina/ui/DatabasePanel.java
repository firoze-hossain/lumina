package dev.lumina.ui;

import dev.lumina.util.Settings;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight database client: connect over JDBC (PostgreSQL & MySQL
 * drivers are bundled), browse tables, run SQL, view results in a grid.
 */
public class DatabasePanel extends BorderPane {

    private final TextField urlField = new TextField();
    private final TextField userField = new TextField();
    private final PasswordField passField = new PasswordField();
    private final Label status = new Label("Not connected");
    private final ListView<String> tables = new ListView<>();
    private final TextField queryField = new TextField();
    private final TableView<ObservableList<String>> results = new TableView<>();

    private Connection connection;

    public DatabasePanel() {
        getStyleClass().add("side-panel");

        Label header = new Label("DATABASE");
        header.getStyleClass().add("panel-header");
        header.setPadding(new Insets(8, 12, 8, 12));

        urlField.setPromptText("jdbc:postgresql://localhost:5432/mydb");
        userField.setPromptText("user");
        passField.setPromptText("password");
        String savedUrl = Settings.get(Settings.DB_URL);
        if (savedUrl != null) urlField.setText(savedUrl);
        String savedUser = Settings.get(Settings.DB_USER);
        if (savedUser != null) userField.setText(savedUser);

        Button connect = new Button("Connect");
        connect.getStyleClass().add("dialog-primary");
        connect.setOnAction(e -> connect());

        status.getStyleClass().add("side-subtle");
        status.setWrapText(true);

        VBox form = new VBox(8, urlField, new HBox(8, grow(userField), grow(passField)),
                new HBox(8, connect, status));
        form.setPadding(new Insets(0, 12, 10, 12));

        tables.getStyleClass().add("goal-list");
        tables.setPlaceholder(placeholder("Connect to list tables"));
        tables.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String table = tables.getSelectionModel().getSelectedItem();
                if (table != null) runQuery("SELECT * FROM " + table);
            }
        });

        queryField.setPromptText("SQL \u2014 press Enter to run");
        queryField.setOnAction(e -> {
            String sql = queryField.getText().trim();
            if (!sql.isEmpty()) runQuery(sql);
        });
        HBox queryRow = new HBox(queryField);
        HBox.setHgrow(queryField, Priority.ALWAYS);
        queryRow.setPadding(new Insets(6, 8, 6, 8));

        results.getStyleClass().add("db-results");
        results.setPlaceholder(placeholder("Double-click a table or run SQL"));
        results.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        BorderPane bottom = new BorderPane(results, queryRow, null, null, null);

        SplitPane split = new SplitPane(tables, bottom);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.42);

        setTop(new VBox(header, form));
        setCenter(split);
        setMinWidth(230);
    }

    // -------------------------------------------------------------- connect

    private void connect() {
        String url = urlField.getText().trim();
        String user = userField.getText().trim();
        String pass = passField.getText();
        if (url.isEmpty()) {
            status.setText("Enter a JDBC URL");
            return;
        }
        status.setText("Connecting\u2026");
        Thread t = new Thread(() -> {
            closeQuiet();
            try {
                Connection c = DriverManager.getConnection(url, user, pass);
                connection = c;
                List<String> names = new ArrayList<>();
                DatabaseMetaData md = c.getMetaData();
                try (ResultSet rs = md.getTables(c.getCatalog(), null, "%",
                        new String[]{"TABLE", "VIEW"})) {
                    while (rs.next()) {
                        String schema = rs.getString("TABLE_SCHEM");
                        String name = rs.getString("TABLE_NAME");
                        names.add(schema == null || schema.isBlank()
                                || "public".equalsIgnoreCase(schema)
                                ? name : schema + "." + name);
                    }
                }
                String product = md.getDatabaseProductName();
                Platform.runLater(() -> {
                    tables.getItems().setAll(names);
                    status.setText("\u2713 " + product + " \u2014 "
                            + names.size() + " tables");
                    Settings.put(Settings.DB_URL, url);
                    Settings.put(Settings.DB_USER, user);
                });
            } catch (SQLException ex) {
                Platform.runLater(() -> status.setText("\u2717 " + shorten(ex.getMessage())));
            }
        }, "lumina-db-connect");
        t.setDaemon(true);
        t.start();
    }

    // ---------------------------------------------------------------- query

    private void runQuery(String sql) {
        if (connection == null) {
            status.setText("Connect first");
            return;
        }
        status.setText("Running\u2026");
        Thread t = new Thread(() -> {
            try (Statement stmt = connection.createStatement()) {
                stmt.setMaxRows(200);
                boolean hasResultSet = stmt.execute(sql);
                if (!hasResultSet) {
                    int n = stmt.getUpdateCount();
                    Platform.runLater(() -> {
                        results.getColumns().clear();
                        results.getItems().clear();
                        status.setText("\u2713 " + n + " row(s) affected");
                    });
                    return;
                }
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    List<String> headers = new ArrayList<>();
                    for (int i = 1; i <= cols; i++) headers.add(md.getColumnLabel(i));

                    List<ObservableList<String>> rows = new ArrayList<>();
                    while (rs.next()) {
                        ObservableList<String> row = FXCollections.observableArrayList();
                        for (int i = 1; i <= cols; i++) {
                            Object v = rs.getObject(i);
                            row.add(v == null ? "NULL" : String.valueOf(v));
                        }
                        rows.add(row);
                    }
                    Platform.runLater(() -> {
                        results.getColumns().clear();
                        for (int i = 0; i < headers.size(); i++) {
                            final int idx = i;
                            TableColumn<ObservableList<String>, String> col =
                                    new TableColumn<>(headers.get(i));
                            col.setCellValueFactory(data ->
                                    new ReadOnlyStringWrapper(
                                            idx < data.getValue().size()
                                                    ? data.getValue().get(idx) : ""));
                            col.setPrefWidth(120);
                            results.getColumns().add(col);
                        }
                        results.getItems().setAll(rows);
                        status.setText("\u2713 " + rows.size() + " row(s)");
                    });
                }
            } catch (SQLException ex) {
                Platform.runLater(() -> status.setText("\u2717 " + shorten(ex.getMessage())));
            }
        }, "lumina-db-query");
        t.setDaemon(true);
        t.start();
    }

    public void shutdown() {
        closeQuiet();
    }

    private void closeQuiet() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }

    private static String shorten(String s) {
        if (s == null) return "error";
        s = s.replace('\n', ' ');
        return s.length() > 120 ? s.substring(0, 120) + "\u2026" : s;
    }

    private Label placeholder(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("explorer-empty");
        return l;
    }

    private static TextField grow(TextField f) {
        HBox.setHgrow(f, Priority.ALWAYS);
        return f;
    }
}
