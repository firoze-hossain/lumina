package dev.lumina.ui;

import dev.lumina.build.MavenSettings;
import dev.lumina.build.MavenSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings subpage for Build, Execution, Deployment > Build Tools > Maven > Runner.
 * Matches 1:1 with reference specification and dynamically manages execution options.
 */
public class SettingsMavenRunnerPage extends VBox {

    private final MavenSettingsManager manager = MavenSettingsManager.getInstance();

    private final CheckBox delegateBuildRunCheck = new CheckBox("Delegate IDE build/run actions to Maven");
    private final TextField vmOptionsField = new TextField();
    private final Button expandVmOptionsBtn = new Button();
    private final ComboBox<String> jreCombo = new ComboBox<>();
    private final TextField envVarsField = new TextField();
    private final Button envVarsBtn = new Button();

    private final CheckBox skipTestsCheck = new CheckBox("Skip Tests");

    private final Button addPropBtn = createToolbarButton("+", "Add property");
    private final Button removePropBtn = createToolbarButton("\u2212", "Remove property");
    private final Button editPropBtn = createToolbarButton("\u270E", "Edit property");

    public record PropertyItem(String name, String value) {}
    private final ObservableList<PropertyItem> propertiesList = FXCollections.observableArrayList();
    private final TableView<PropertyItem> propertiesTable = new TableView<>();
    private final Label emptyPlaceholderLabel = new Label("No properties defined");
    private final StackPane tableContainer = new StackPane();

    private MavenSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsMavenRunnerPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // Delegate checkbox
        styleCheckBox(delegateBuildRunCheck);
        getChildren().add(delegateBuildRunCheck);

        // Form Grid
        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(12);
        formGrid.setPadding(new Insets(6, 0, 10, 0));

        ColumnConstraints colLabel = new ColumnConstraints(160);
        ColumnConstraints colField = new ColumnConstraints(300, 520, Double.MAX_VALUE);
        colField.setHgrow(Priority.ALWAYS);
        formGrid.getColumnConstraints().addAll(colLabel, colField);

        int row = 0;

        // Row 1: VM Options
        Label vmLabel = new Label("VM Options:");
        styleLabel(vmLabel);

        styleTextField(vmOptionsField);
        HBox.setHgrow(vmOptionsField, Priority.ALWAYS);

        expandVmOptionsBtn.setGraphic(createExpandIcon());
        styleIconButton(expandVmOptionsBtn);
        expandVmOptionsBtn.setTooltip(new Tooltip("Expand VM options editor"));
        expandVmOptionsBtn.setOnAction(e -> showExpandVmOptionsDialog());

        HBox vmBox = new HBox(6, vmOptionsField, expandVmOptionsBtn);
        vmBox.setAlignment(Pos.CENTER_LEFT);

        Label vmNote = new Label("Options specified in this field override the ones in .mvn/jvm.config files");
        vmNote.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        VBox vmFullBox = new VBox(4, vmBox, vmNote);
        formGrid.add(vmLabel, 0, row);
        formGrid.add(vmFullBox, 1, row);
        row++;

        // Row 2: JRE
        Label jreLabel = new Label("JRE:");
        styleLabel(jreLabel);
        setupJreCombo();
        HBox.setHgrow(jreCombo, Priority.ALWAYS);
        formGrid.add(jreLabel, 0, row);
        formGrid.add(jreCombo, 1, row);
        row++;

        // Row 3: Environment variables
        Label envLabel = new Label("Environment variables:");
        styleLabel(envLabel);

        styleTextField(envVarsField);
        envVarsField.setPromptText("Environment variables");
        HBox.setHgrow(envVarsField, Priority.ALWAYS);

        envVarsBtn.setGraphic(createTableListIcon());
        styleIconButton(envVarsBtn);
        envVarsBtn.setTooltip(new Tooltip("Edit environment variables"));
        envVarsBtn.setOnAction(e -> showEnvVarsDialog());

        HBox envBox = new HBox(6, envVarsField, envVarsBtn);
        envBox.setAlignment(Pos.CENTER_LEFT);

        formGrid.add(envLabel, 0, row);
        formGrid.add(envBox, 1, row);
        row++;

        getChildren().add(formGrid);

        // Section: Properties
        Label propsHeader = new Label("Properties:");
        styleLabel(propsHeader);
        getChildren().add(propsHeader);

        styleCheckBox(skipTestsCheck);
        getChildren().add(skipTestsCheck);

        // Toolbar (+, -, edit)
        removePropBtn.setDisable(true);
        editPropBtn.setDisable(true);

        addPropBtn.setOnAction(e -> showAddPropertyDialog(null));
        removePropBtn.setOnAction(e -> {
            PropertyItem sel = propertiesTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                propertiesList.remove(sel);
                updateTablePlaceholder();
                notifyModified();
            }
        });
        editPropBtn.setOnAction(e -> {
            PropertyItem sel = propertiesTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                showAddPropertyDialog(sel);
            }
        });

        HBox toolbar = new HBox(4, addPropBtn, removePropBtn, editPropBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 0, 4, 0));
        getChildren().add(toolbar);

        // Properties table with "No properties defined" placeholder
        setupPropertiesTable();

        emptyPlaceholderLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 13px;");
        emptyPlaceholderLabel.setAlignment(Pos.CENTER);

        tableContainer.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        tableContainer.setPrefHeight(220);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        tableContainer.getChildren().addAll(propertiesTable, emptyPlaceholderLabel);
        getChildren().add(tableContainer);

        // Selection listener
        propertiesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean hasSel = newSel != null;
            removePropBtn.setDisable(!hasSel);
            editPropBtn.setDisable(!hasSel);
        });

        // Change listeners
        delegateBuildRunCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
        vmOptionsField.textProperty().addListener((obs, o, n) -> notifyModified());
        jreCombo.valueProperty().addListener((obs, o, n) -> notifyModified());
        envVarsField.textProperty().addListener((obs, o, n) -> notifyModified());
        skipTestsCheck.selectedProperty().addListener((obs, o, n) -> notifyModified());
    }

    private void setupJreCombo() {
        styleComboBox(jreCombo);
        List<String> options = MavenSettingsManager.getAvailableJdkOptions();
        jreCombo.getItems().setAll(options);

        jreCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setTextFill(Color.web("#DFE1E5"));
                    setGraphic(createFolderSdkIcon());
                }
            }
        });

        jreCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setTextFill(Color.web("#DFE1E5"));
                    setGraphic(createFolderSdkIcon());
                }
            }
        });
    }

    private void setupPropertiesTable() {
        propertiesTable.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-table-cell-border-color: #2B2D30; -fx-border-color: transparent;");
        propertiesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        propertiesTable.setItems(propertiesList);

        TableColumn<PropertyItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameCol.setMinWidth(180);
        nameCol.setPrefWidth(220);
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setTextFill(Color.web("#DFE1E5"));
                }
            }
        });

        TableColumn<PropertyItem, String> valCol = new TableColumn<>("Value");
        valCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().value()));
        valCol.setMinWidth(220);
        valCol.setPrefWidth(350);
        valCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setTextFill(Color.web("#DFE1E5"));
                }
            }
        });

        propertiesTable.getColumns().setAll(nameCol, valCol);
    }

    private void updateTablePlaceholder() {
        boolean isEmpty = propertiesList.isEmpty();
        emptyPlaceholderLabel.setVisible(isEmpty);
        propertiesTable.setVisible(!isEmpty);
    }

    private void showExpandVmOptionsDialog() {
        Stage dialog = new Stage();
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("VM Options");

        TextArea area = new TextArea(vmOptionsField.getText());
        area.setWrapText(true);
        area.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-control-inner-background: #2B2D30; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-family: monospace;");
        VBox.setVgrow(area, Priority.ALWAYS);

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-padding: 5 16; -fx-background-radius: 4; -fx-cursor: hand;");
        okBtn.setOnAction(e -> {
            vmOptionsField.setText(area.getText().trim());
            notifyModified();
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-padding: 5 14; -fx-background-radius: 4; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox footer = new HBox(10, cancelBtn, okBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 16, 14, 16));

        VBox content = new VBox(12, area, footer);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1E1F22;");

        Scene scene = new Scene(content, 480, 260);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showEnvVarsDialog() {
        Stage dialog = new Stage();
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Environment Variables");

        TextArea area = new TextArea(envVarsField.getText());
        area.setPromptText("NAME=VALUE (one per line or semicolon-separated)");
        area.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-control-inner-background: #2B2D30; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-family: monospace;");
        VBox.setVgrow(area, Priority.ALWAYS);

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-padding: 5 16; -fx-background-radius: 4; -fx-cursor: hand;");
        okBtn.setOnAction(e -> {
            envVarsField.setText(area.getText().trim());
            notifyModified();
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-padding: 5 14; -fx-background-radius: 4; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox footer = new HBox(10, cancelBtn, okBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 16, 14, 16));

        VBox content = new VBox(12, area, footer);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #1E1F22;");

        Scene scene = new Scene(content, 480, 260);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showAddPropertyDialog(PropertyItem existingToEdit) {
        Stage dialog = new Stage();
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existingToEdit == null ? "Add Property" : "Edit Property");

        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField nameField = new TextField(existingToEdit != null ? existingToEdit.name() : "");
        styleTextField(nameField);

        Label valLabel = new Label("Value:");
        valLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        TextField valField = new TextField(existingToEdit != null ? existingToEdit.value() : "");
        styleTextField(valField);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(16, 18, 14, 18));
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(valLabel, 0, 1);
        grid.add(valField, 1, 1);

        ColumnConstraints col0 = new ColumnConstraints(60);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        Button submitBtn = new Button(existingToEdit == null ? "Add" : "Save");
        submitBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-padding: 5 16; -fx-background-radius: 4; -fx-cursor: hand;");
        submitBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String val = valField.getText().trim();
            if (name.isEmpty()) return;

            PropertyItem item = new PropertyItem(name, val);
            if (existingToEdit != null) {
                int idx = propertiesList.indexOf(existingToEdit);
                if (idx >= 0) {
                    propertiesList.set(idx, item);
                    propertiesTable.getSelectionModel().select(idx);
                }
            } else {
                propertiesList.add(item);
                propertiesTable.getSelectionModel().select(item);
            }
            updateTablePlaceholder();
            notifyModified();
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-padding: 5 14; -fx-background-radius: 4; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox footer = new HBox(10, cancelBtn, submitBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 18, 14, 18));
        footer.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        BorderPane root = new BorderPane();
        root.setCenter(grid);
        root.setBottom(footer);
        root.setStyle("-fx-background-color: #1E1F22;");

        Scene scene = new Scene(root, 420, 170);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private Button createToolbarButton(String text, String tooltipText) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C919D; -fx-font-size: 15px; -fx-cursor: hand; " +
                "-fx-padding: 0 4 0 4; -fx-min-width: 24px; -fx-min-height: 24px;");
        b.setTooltip(new Tooltip(tooltipText));
        b.setOnMouseEntered(e -> {
            if (!b.isDisabled()) {
                b.setStyle("-fx-background-color: #2E3136; -fx-text-fill: #DFE1E5; -fx-font-size: 15px; -fx-cursor: hand; " +
                        "-fx-padding: 0 4 0 4; -fx-min-width: 24px; -fx-min-height: 24px; -fx-background-radius: 3;");
            }
        });
        b.setOnMouseExited(e -> {
            if (!b.isDisabled()) {
                b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C919D; -fx-font-size: 15px; -fx-cursor: hand; " +
                        "-fx-padding: 0 4 0 4; -fx-min-width: 24px; -fx-min-height: 24px;");
            }
        });
        return b;
    }

    private Node createFolderSdkIcon() {
        SVGPath p = new SVGPath();
        p.setContent("M 2,3 L 6,3 L 7.5,4.5 L 14,4.5 L 14,12 L 2,12 Z");
        p.setStroke(Color.web("#3574F0"));
        p.setStrokeWidth(1.2);
        p.setFill(null);
        return p;
    }

    private Node createExpandIcon() {
        SVGPath p = new SVGPath();
        p.setContent("M 2,6 L 6,2 M 6,2 L 2,2 M 6,2 L 6,6 M 12,8 L 8,12 M 8,12 L 12,12 M 8,12 L 8,8");
        p.setStroke(Color.web("#8C919D"));
        p.setStrokeWidth(1.2);
        p.setFill(null);
        return p;
    }

    private Node createTableListIcon() {
        SVGPath p = new SVGPath();
        p.setContent("M 2,3 L 12,3 L 12,11 L 2,11 Z M 2,6 L 12,6 M 2,9 L 12,9 M 5,3 L 5,11");
        p.setStroke(Color.web("#8C919D"));
        p.setStrokeWidth(1.1);
        p.setFill(null);
        return p;
    }

    private void styleLabel(Label l) {
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8; -fx-font-size: 13px;");
    }

    private void styleIconButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 4 8;");
    }

    public void loadData() {
        updating = true;
        initialSettings = manager.getSettings();

        delegateBuildRunCheck.setSelected(initialSettings.isDelegateBuildRunToMaven());
        vmOptionsField.setText(initialSettings.getRunnerVmOptions());

        String savedJre = initialSettings.getRunnerJre();
        if (savedJre == null || savedJre.isBlank() || !jreCombo.getItems().contains(savedJre)) {
            String def = MavenSettingsManager.getDefaultProjectJdkDisplay();
            jreCombo.setValue(def);
            initialSettings.setRunnerJre(def);
        } else {
            jreCombo.setValue(savedJre);
        }

        envVarsField.setText(initialSettings.getEnvironmentVariables());
        skipTestsCheck.setSelected(initialSettings.isSkipTests());

        propertiesList.clear();
        for (Map.Entry<String, String> e : initialSettings.getRunnerProperties().entrySet()) {
            propertiesList.add(new PropertyItem(e.getKey(), e.getValue()));
        }
        updateTablePlaceholder();

        updating = false;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        MavenSettings cur = getCurrentSettings();
        return initialSettings.isDelegateBuildRunToMaven() != cur.isDelegateBuildRunToMaven()
                || !initialSettings.getRunnerVmOptions().equals(cur.getRunnerVmOptions())
                || !initialSettings.getRunnerJre().equals(cur.getRunnerJre())
                || !initialSettings.getEnvironmentVariables().equals(cur.getEnvironmentVariables())
                || initialSettings.isSkipTests() != cur.isSkipTests()
                || !initialSettings.getRunnerProperties().equals(cur.getRunnerProperties());
    }

    public void apply() {
        MavenSettings cur = getCurrentSettings();
        MavenSettings s = manager.getSettings();
        s.setDelegateBuildRunToMaven(cur.isDelegateBuildRunToMaven());
        s.setRunnerVmOptions(cur.getRunnerVmOptions());
        s.setRunnerJre(cur.getRunnerJre());
        s.setEnvironmentVariables(cur.getEnvironmentVariables());
        s.setSkipTests(cur.isSkipTests());
        s.setRunnerProperties(cur.getRunnerProperties());
        manager.setSettings(s);
        initialSettings = s.clone();
    }

    public void reset() {
        loadData();
    }

    public MavenSettings getCurrentSettings() {
        MavenSettings s = (initialSettings != null) ? initialSettings.clone() : new MavenSettings();
        s.setDelegateBuildRunToMaven(delegateBuildRunCheck.isSelected());
        s.setRunnerVmOptions(vmOptionsField.getText());
        s.setRunnerJre(jreCombo.getValue() != null ? jreCombo.getValue() : "");
        s.setEnvironmentVariables(envVarsField.getText());
        s.setSkipTests(skipTestsCheck.isSelected());

        Map<String, String> props = new LinkedHashMap<>();
        for (PropertyItem p : propertiesList) {
            props.put(p.name(), p.value());
        }
        s.setRunnerProperties(props);
        return s;
    }

    public ObservableList<PropertyItem> getPropertiesList() {
        return propertiesList;
    }

    public TableView<PropertyItem> getPropertiesTable() {
        return propertiesTable;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
