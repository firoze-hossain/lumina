package dev.lumina.ui;

import dev.lumina.database.DbConnectionProfile;
import dev.lumina.database.DbEngineType;
import dev.lumina.database.DbMetadataService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Modern Data Source dialog in Lumina matching DBNavigator's full configuration suite.
 */
public class DbConnectionDialog extends Dialog<DbConnectionProfile> {

    private final DbConnectionProfile profile;

    private final ComboBox<DbEngineType> engineCombo = new ComboBox<>();
    private final TextField nameField = new TextField();
    private final TextField hostField = new TextField();
    private final TextField portField = new TextField();
    private final TextField databaseField = new TextField();
    private final Label databaseLabel = new Label("Database:");
    private final Button browseSqliteBtn = new Button("Browse\u2026");
    private final TextField userField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final CheckBox savePasswordCheck = new CheckBox("Save password");
    private final CheckBox sslCheck = new CheckBox("Use SSL");
    private final Button testBtn = new Button("Test Connection");
    private final Label testResultLabel = new Label();

    // Oracle options
    private final ToggleGroup oracleToggleGroup = new ToggleGroup();
    private final RadioButton oracleServiceRadio = new RadioButton("Service Name");
    private final RadioButton oracleSidRadio = new RadioButton("SID");
    private final TextField oracleSidField = new TextField("XE");
    private final HBox oracleRow = new HBox(10);

    // MongoDB options
    private final ToggleGroup mongoToggleGroup = new ToggleGroup();
    private final RadioButton mongoDefaultRadio = new RadioButton("Standard");
    private final RadioButton mongoSrvRadio = new RadioButton("Atlas (SRV)");
    private final HBox mongoRow = new HBox(10);

    public DbConnectionDialog(DbConnectionProfile existing) {
        this.profile = existing != null ? existing.copy() : new DbConnectionProfile(DbEngineType.POSTGRESQL);

        setTitle(existing != null ? "Edit Data Source" : "New Data Source");
        setHeaderText(null);

        DialogPane dp = getDialogPane();
        dp.getStyleClass().add("settings-dialog");
        dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dp.setPrefWidth(550);

        try {
            dp.getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        } catch (Throwable ignored) {}

        // Populate engines
        engineCombo.getItems().setAll(DbEngineType.values());
        engineCombo.setValue(profile.getEngine());
        engineCombo.setPrefWidth(260);
        engineCombo.setOnAction(e -> onEngineChanged(engineCombo.getValue()));

        nameField.setText(profile.getName());
        hostField.setText(profile.getHost());
        portField.setText(String.valueOf(profile.getPort()));
        databaseField.setText(profile.getDatabase());
        userField.setText(profile.getUser());
        passwordField.setText(profile.getPassword());
        savePasswordCheck.setSelected(profile.isSavePassword());
        sslCheck.setSelected(profile.isUseSsl());

        // Oracle radios
        oracleServiceRadio.setToggleGroup(oracleToggleGroup);
        oracleSidRadio.setToggleGroup(oracleToggleGroup);
        if (profile.getOracleConnectionType() == DbConnectionProfile.OracleConnectionType.SID) {
            oracleSidRadio.setSelected(true);
        } else {
            oracleServiceRadio.setSelected(true);
        }
        oracleSidField.setText(profile.getOracleSid());
        oracleSidField.setPrefWidth(120);
        oracleRow.setAlignment(Pos.CENTER_LEFT);
        oracleRow.getChildren().setAll(new Label("Connection:"), oracleServiceRadio, oracleSidRadio, new Label("SID:"), oracleSidField);

        // Mongo radios
        mongoDefaultRadio.setToggleGroup(mongoToggleGroup);
        mongoSrvRadio.setToggleGroup(mongoToggleGroup);
        if (profile.getMongoConnectionType() == DbConnectionProfile.MongoConnectionType.SRV) {
            mongoSrvRadio.setSelected(true);
        } else {
            mongoDefaultRadio.setSelected(true);
        }
        mongoRow.setAlignment(Pos.CENTER_LEFT);
        mongoRow.getChildren().setAll(new Label("Connection:"), mongoDefaultRadio, mongoSrvRadio);

        // SQLite browse button
        browseSqliteBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select SQLite Database File");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("SQLite Databases (*.db, *.sqlite, *.sqlite3)", "*.db", "*.sqlite", "*.sqlite3"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            File f = fc.showOpenDialog(getDialogPane().getScene().getWindow());
            if (f != null) {
                databaseField.setText(f.getAbsolutePath());
            }
        });

        // Test connection
        testBtn.getStyleClass().add("button");
        testBtn.setOnAction(e -> runTestConnection());
        testResultLabel.setWrapText(true);
        testResultLabel.setStyle("-fx-font-size: 11px;");

        // Layout form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        int row = 0;
        grid.add(fieldLabel("Driver / Engine:"), 0, row);
        grid.add(engineCombo, 1, row);
        row++;

        grid.add(fieldLabel("Name:"), 0, row);
        grid.add(nameField, 1, row);
        row++;

        grid.add(fieldLabel("Host:"), 0, row);
        HBox hostPortBox = new HBox(8, hostField, new Label("Port:"), portField);
        HBox.setHgrow(hostField, Priority.ALWAYS);
        portField.setPrefWidth(85);
        grid.add(hostPortBox, 1, row);
        row++;

        grid.add(databaseLabel, 0, row);
        HBox dbBox = new HBox(8, databaseField, browseSqliteBtn);
        HBox.setHgrow(databaseField, Priority.ALWAYS);
        grid.add(dbBox, 1, row);
        row++;

        grid.add(new Label(""), 0, row);
        VBox extraOptionsBox = new VBox(6, oracleRow, mongoRow);
        grid.add(extraOptionsBox, 1, row);
        row++;

        grid.add(fieldLabel("Authentication:"), 0, row);
        HBox authBox = new HBox(8, userField, passwordField);
        userField.setPromptText("Username");
        passwordField.setPromptText("Password");
        HBox.setHgrow(userField, Priority.ALWAYS);
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        grid.add(authBox, 1, row);
        row++;

        grid.add(new Label(""), 0, row);
        HBox checksBox = new HBox(16, savePasswordCheck, sslCheck);
        grid.add(checksBox, 1, row);
        row++;

        grid.add(new Label(""), 0, row);
        HBox testRow = new HBox(12, testBtn, testResultLabel);
        testRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(testRow, 1, row);

        dp.setContent(grid);

        // Adjust visibility for initial engine
        onEngineChanged(profile.getEngine());

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                applyToProfile();
                return profile;
            }
            return null;
        });
    }

    private void onEngineChanged(DbEngineType engine) {
        if (engine == null) return;
        boolean isSqlite = engine == DbEngineType.SQLITE;
        boolean isOracle = engine == DbEngineType.ORACLE;
        boolean isMongo = engine == DbEngineType.MONGODB;

        browseSqliteBtn.setVisible(isSqlite);
        browseSqliteBtn.setManaged(isSqlite);

        oracleRow.setVisible(isOracle);
        oracleRow.setManaged(isOracle);

        mongoRow.setVisible(isMongo);
        mongoRow.setManaged(isMongo);

        hostField.setDisable(isSqlite);
        portField.setDisable(isSqlite);

        if (isSqlite) {
            databaseLabel.setText("Database File:");
            databaseField.setPromptText("/path/to/database.db");
            userField.setDisable(true);
            passwordField.setDisable(true);
            sslCheck.setDisable(true);
        } else {
            databaseLabel.setText("Database:");
            databaseField.setPromptText("database");
            userField.setDisable(false);
            passwordField.setDisable(false);
            sslCheck.setDisable(false);
            if (portField.getText().isBlank() || "0".equals(portField.getText())) {
                portField.setText(String.valueOf(engine.getDefaultPort()));
            }
            if (userField.getText().isBlank()) {
                userField.setText(engine.getDefaultUser());
            }
        }
    }

    private void runTestConnection() {
        applyToProfile();
        testBtn.setDisable(true);
        testResultLabel.setText("Connecting\u2026");
        testResultLabel.setStyle("-fx-text-fill: #A6ADC4;");

        Thread t = new Thread(() -> {
            DbMetadataService.TestResult res = DbMetadataService.testConnection(profile);
            Platform.runLater(() -> {
                testBtn.setDisable(false);
                if (res.success()) {
                    String ver = res.serverVersion() != null ? " (" + res.serverVersion() + ")" : "";
                    testResultLabel.setText("\u2713 Connected in " + res.latencyMs() + " ms" + ver);
                    testResultLabel.setStyle("-fx-text-fill: #50E3C2; -fx-font-weight: bold;");
                } else {
                    testResultLabel.setText("\u2717 " + (res.message() != null ? res.message() : "Failed to connect"));
                    testResultLabel.setStyle("-fx-text-fill: #FF6B6B;");
                }
            });
        }, "lumina-db-test-conn");
        t.setDaemon(true);
        t.start();
    }

    private void applyToProfile() {
        profile.setEngine(engineCombo.getValue());
        profile.setName(nameField.getText().trim());
        profile.setHost(hostField.getText().trim());
        try {
            profile.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException e) {
            profile.setPort(profile.getEngine().getDefaultPort());
        }
        profile.setDatabase(databaseField.getText().trim());
        profile.setUser(userField.getText().trim());
        profile.setPassword(passwordField.getText());
        profile.setSavePassword(savePasswordCheck.isSelected());
        profile.setUseSsl(sslCheck.isSelected());

        if (profile.getEngine() == DbEngineType.ORACLE) {
            profile.setOracleConnectionType(oracleSidRadio.isSelected()
                    ? DbConnectionProfile.OracleConnectionType.SID
                    : DbConnectionProfile.OracleConnectionType.SERVICE_NAME);
            profile.setOracleSid(oracleSidField.getText().trim());
        } else if (profile.getEngine() == DbEngineType.MONGODB) {
            profile.setMongoConnectionType(mongoSrvRadio.isSelected()
                    ? DbConnectionProfile.MongoConnectionType.SRV
                    : DbConnectionProfile.MongoConnectionType.DEFAULT);
        }
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #C9CDDC;");
        return l;
    }
}
