package dev.lumina.ui;

import dev.lumina.kubernetes.KubernetesLanguageSettings;
import dev.lumina.kubernetes.KubernetesLanguageSettingsManager;
import dev.lumina.kubernetes.KubernetesResourceSpec;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Languages & Frameworks > Kubernetes settings page in Lumina IDE.
 * Matching the reference IDE layout with dynamic cluster discovery, API versioning, and CRD/OpenAPI specs.
 */
public class SettingsLanguagesKubernetesPage extends VBox {

    private final KubernetesLanguageSettingsManager manager = KubernetesLanguageSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private ComboBox<String> currentClusterCombo;
    private ComboBox<String> apiVersionCombo;
    private CheckBox useApiSchemaCheckBox;
    private ComboBox<String> kustomizeVersionCombo;

    private Button addButton;
    private Button removeButton;
    private Button moveUpButton;
    private Button moveDownButton;

    private TableView<KubernetesResourceSpec> tableView;
    private ObservableList<KubernetesResourceSpec> tableItems;

    private Button checkConfigurationButton;
    private Button resetSchemaCacheButton;
    private Label statusFeedbackLabel;

    private KubernetesLanguageSettings initialSettings;

    public SettingsLanguagesKubernetesPage() {
        setSpacing(10);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        KubernetesLanguageSettings current = manager.getSettings();

        // 1. Current Cluster row
        HBox clusterRow = new HBox(10);
        clusterRow.setAlignment(Pos.CENTER_LEFT);

        Label clusterLabel = new Label("Current Cluster:");
        clusterLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        clusterLabel.setPrefWidth(160);

        currentClusterCombo = new ComboBox<>();
        List<String> clusters = KubernetesLanguageSettings.detectAvailableClusters();
        currentClusterCombo.getItems().addAll(clusters);
        currentClusterCombo.setValue(current.getCurrentCluster());
        currentClusterCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(currentClusterCombo, Priority.ALWAYS);
        styleComboBox(currentClusterCombo);
        currentClusterCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        clusterRow.getChildren().addAll(clusterLabel, currentClusterCombo);

        // 2. Kubernetes API version row
        HBox apiRow = new HBox(10);
        apiRow.setAlignment(Pos.CENTER_LEFT);

        Label apiLabel = new Label("Kubernetes API version:");
        apiLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        apiLabel.setPrefWidth(160);

        apiVersionCombo = new ComboBox<>();
        apiVersionCombo.getItems().addAll(KubernetesLanguageSettings.AVAILABLE_API_VERSIONS);
        apiVersionCombo.setValue(current.getApiVersion());
        apiVersionCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(apiVersionCombo, Priority.ALWAYS);
        styleComboBox(apiVersionCombo);
        apiVersionCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        apiRow.getChildren().addAll(apiLabel, apiVersionCombo);

        // 3. Use API schema from active cluster checkbox (indented)
        HBox schemaBox = new HBox(10);
        schemaBox.setAlignment(Pos.CENTER_LEFT);
        schemaBox.setPadding(new Insets(0, 0, 4, 170));

        useApiSchemaCheckBox = new CheckBox("Use API schema from the active cluster if available");
        useApiSchemaCheckBox.setSelected(current.isUseApiSchemaFromActiveCluster());
        useApiSchemaCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        useApiSchemaCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        schemaBox.getChildren().add(useApiSchemaCheckBox);

        // 4. Kustomize version row
        HBox kustomizeRow = new HBox(10);
        kustomizeRow.setAlignment(Pos.CENTER_LEFT);

        Label kustomizeLabel = new Label("Kustomize version:");
        kustomizeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        kustomizeLabel.setPrefWidth(160);

        kustomizeVersionCombo = new ComboBox<>();
        kustomizeVersionCombo.getItems().addAll(KubernetesLanguageSettings.AVAILABLE_KUSTOMIZE_VERSIONS);
        kustomizeVersionCombo.setValue(current.getKustomizeVersion());
        kustomizeVersionCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(kustomizeVersionCombo, Priority.ALWAYS);
        styleComboBox(kustomizeVersionCombo);
        kustomizeVersionCombo.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        kustomizeRow.getChildren().addAll(kustomizeLabel, kustomizeVersionCombo);

        // 5. CRD / OpenAPI Specifications Table
        HBox tableToolbar = new HBox(4);
        tableToolbar.setAlignment(Pos.CENTER_LEFT);
        tableToolbar.setPadding(new Insets(6, 0, 2, 0));

        addButton = createToolbarButton("+", "Add specification");
        addButton.setOnAction(e -> promptAddSpecification());

        removeButton = createToolbarButton("-", "Remove specification");
        removeButton.setDisable(true);
        removeButton.setOnAction(e -> removeSelectedSpecification());

        moveUpButton = createToolbarButton("\u2191", "Move Up");
        moveUpButton.setDisable(true);
        moveUpButton.setOnAction(e -> moveSelectedRow(-1));

        moveDownButton = createToolbarButton("\u2193", "Move Down");
        moveDownButton.setDisable(true);
        moveDownButton.setOnAction(e -> moveSelectedRow(1));

        tableToolbar.getChildren().addAll(addButton, removeButton, moveUpButton, moveDownButton);

        tableView = new TableView<>();
        tableView.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4; " +
                "-fx-font-size: 13px;"
        );
        tableView.setPrefHeight(220);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        tableItems = FXCollections.observableArrayList();
        for (KubernetesResourceSpec spec : current.getSpecifications()) {
            tableItems.add(spec.copy());
        }
        tableView.setItems(tableItems);

        TableColumn<KubernetesResourceSpec, String> pathCol = new TableColumn<>("Local path or URL to custom resource definition (CRD) or OpenAPI specification");
        pathCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPathOrUrl()));
        pathCol.setPrefWidth(480);

        TableColumn<KubernetesResourceSpec, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setPrefWidth(90);

        TableColumn<KubernetesResourceSpec, String> scopeCol = new TableColumn<>("Scope");
        scopeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getScope()));
        scopeCol.setPrefWidth(90);

        tableView.getColumns().addAll(pathCol, statusCol, scopeCol);

        // Placeholder for empty table
        VBox emptyPlaceholder = new VBox(8);
        emptyPlaceholder.setAlignment(Pos.CENTER);

        HBox emptyLine1 = new HBox(4);
        emptyLine1.setAlignment(Pos.CENTER);
        Label emptyText1 = new Label("No external specification added. Add specifications (Alt+Insert) either by ");
        emptyText1.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");
        Hyperlink filesLink = new Hyperlink("files");
        filesLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-underline: true; -fx-padding: 0;");
        filesLink.setOnAction(e -> promptAddFile());
        Label orText = new Label(" or by ");
        orText.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");
        Hyperlink urlsLink = new Hyperlink("URLs");
        urlsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-underline: true; -fx-padding: 0;");
        urlsLink.setOnAction(e -> promptAddUrl());
        emptyLine1.getChildren().addAll(emptyText1, filesLink, orText, urlsLink);

        Hyperlink aboutLink = new Hyperlink("About supported external specification formats");
        aboutLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-underline: true; -fx-padding: 0;");

        emptyPlaceholder.getChildren().addAll(emptyLine1, aboutLink);
        tableView.setPlaceholder(emptyPlaceholder);

        tableView.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> updateToolbarState());

        // 6. Action buttons below table
        VBox actionBox = new VBox(8);
        actionBox.setAlignment(Pos.CENTER_LEFT);
        actionBox.setPadding(new Insets(4, 0, 0, 0));

        checkConfigurationButton = createActionButton("Check Configuration");
        checkConfigurationButton.setOnAction(e -> {
            boolean ok = manager.checkConfiguration();
            statusFeedbackLabel.setText(ok ? "Configuration is valid." : "Cluster is not configured. Active schema unavailable.");
            statusFeedbackLabel.setStyle(ok ? "-fx-text-fill: #62B543; -fx-font-size: 12px;" : "-fx-text-fill: #E5A84B; -fx-font-size: 12px;");
            statusFeedbackLabel.setVisible(true);
        });

        resetSchemaCacheButton = createActionButton("Reset Schema Cache");
        resetSchemaCacheButton.setOnAction(e -> {
            manager.resetSchemaCache();
            statusFeedbackLabel.setText("Schema cache reset successfully.");
            statusFeedbackLabel.setStyle("-fx-text-fill: #62B543; -fx-font-size: 12px;");
            statusFeedbackLabel.setVisible(true);
        });

        statusFeedbackLabel = new Label();
        statusFeedbackLabel.setVisible(false);

        actionBox.getChildren().addAll(checkConfigurationButton, resetSchemaCacheButton, statusFeedbackLabel);

        getChildren().addAll(
                clusterRow,
                apiRow,
                schemaBox,
                kustomizeRow,
                tableToolbar,
                tableView,
                actionBox
        );
    }

    private Button createToolbarButton(String text, String tooltip) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8;");
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    private Button createActionButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-font-size: 13px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 4 14;"
        );
        return btn;
    }

    private void styleComboBox(ComboBox<?> combo) {
        combo.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-font-size: 13px;"
        );
    }

    private void updateToolbarState() {
        int idx = tableView.getSelectionModel().getSelectedIndex();
        int size = tableItems.size();
        removeButton.setDisable(idx < 0);
        moveUpButton.setDisable(idx <= 0);
        moveDownButton.setDisable(idx < 0 || idx >= size - 1);
    }

    private void moveSelectedRow(int delta) {
        int idx = tableView.getSelectionModel().getSelectedIndex();
        int newIdx = idx + delta;
        if (idx >= 0 && newIdx >= 0 && newIdx < tableItems.size()) {
            KubernetesResourceSpec item = tableItems.remove(idx);
            tableItems.add(newIdx, item);
            tableView.getSelectionModel().select(newIdx);
            updateToolbarState();
            fireModified();
        }
    }

    private void removeSelectedSpecification() {
        int idx = tableView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            tableItems.remove(idx);
            updateToolbarState();
            fireModified();
        }
    }

    private void promptAddSpecification() {
        promptAddFile();
    }

    public void promptAddFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select CRD or OpenAPI Specification File");
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            tableItems.add(new KubernetesResourceSpec(file.getAbsolutePath(), "Valid", "Project"));
            fireModified();
        }
    }

    public void promptAddUrl() {
        TextInputDialog dialog = new TextInputDialog("https://");
        dialog.setTitle("Add Specification URL");
        dialog.setHeaderText("Enter CRD or OpenAPI Specification URL:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(url -> {
            if (!url.isBlank()) {
                tableItems.add(new KubernetesResourceSpec(url.trim(), "Valid", "Project"));
                fireModified();
            }
        });
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void takeSnapshot() {
        this.initialSettings = getFormSettings();
    }

    private KubernetesLanguageSettings getFormSettings() {
        KubernetesLanguageSettings s = new KubernetesLanguageSettings();
        if (currentClusterCombo.getValue() != null) {
            s.setCurrentCluster(currentClusterCombo.getValue());
        }
        if (apiVersionCombo.getValue() != null) {
            s.setApiVersion(apiVersionCombo.getValue());
        }
        s.setUseApiSchemaFromActiveCluster(useApiSchemaCheckBox.isSelected());
        if (kustomizeVersionCombo.getValue() != null) {
            s.setKustomizeVersion(kustomizeVersionCombo.getValue());
        }
        List<KubernetesResourceSpec> specs = new ArrayList<>();
        for (KubernetesResourceSpec spec : tableItems) {
            specs.add(spec.copy());
        }
        s.setSpecifications(specs);
        return s;
    }

    public boolean isModified() {
        return !Objects.equals(getFormSettings(), initialSettings);
    }

    public void apply() {
        manager.setSettings(getFormSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        if (initialSettings != null) {
            currentClusterCombo.setValue(initialSettings.getCurrentCluster());
            apiVersionCombo.setValue(initialSettings.getApiVersion());
            useApiSchemaCheckBox.setSelected(initialSettings.isUseApiSchemaFromActiveCluster());
            kustomizeVersionCombo.setValue(initialSettings.getKustomizeVersion());
            tableItems.clear();
            for (KubernetesResourceSpec spec : initialSettings.getSpecifications()) {
                tableItems.add(spec.copy());
            }
            updateToolbarState();
        }
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public ComboBox<String> getCurrentClusterCombo() {
        return currentClusterCombo;
    }

    public ComboBox<String> getApiVersionCombo() {
        return apiVersionCombo;
    }

    public CheckBox getUseApiSchemaCheckBox() {
        return useApiSchemaCheckBox;
    }

    public ComboBox<String> getKustomizeVersionCombo() {
        return kustomizeVersionCombo;
    }

    public TableView<KubernetesResourceSpec> getTableView() {
        return tableView;
    }

    public ObservableList<KubernetesResourceSpec> getTableItems() {
        return tableItems;
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public Button getMoveUpButton() {
        return moveUpButton;
    }

    public Button getMoveDownButton() {
        return moveDownButton;
    }

    public Button getCheckConfigurationButton() {
        return checkConfigurationButton;
    }

    public Button getResetSchemaCacheButton() {
        return resetSchemaCacheButton;
    }

    public Label getStatusFeedbackLabel() {
        return statusFeedbackLabel;
    }
}
