package dev.lumina.ui;

import dev.lumina.docker.DockerRegistryConfig;
import dev.lumina.docker.DockerRegistryType;
import dev.lumina.docker.DockerSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Docker > Docker Registry (Screenshot 5).
 * Accurately replicates the UI and behavior shown in IntelliJ IDEA.
 */
public class SettingsDockerRegistryPage extends VBox {

    private final DockerSettingsManager manager = DockerSettingsManager.getInstance();

    private final ObservableList<DockerRegistryConfig> registryList = FXCollections.observableArrayList();
    private final ListView<DockerRegistryConfig> registryListView = new ListView<>(registryList);

    private final Button addButton = new Button("+");
    private final Button removeButton = new Button("—");

    private final TextField nameField = new TextField("Docker Registry");
    private final ComboBox<DockerRegistryType> registryTypeCombo = new ComboBox<>();
    private final TextField addressField = new TextField("registry-1.docker.io");
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();

    private final Label testResultLabel = new Label("Cannot connect: Username required");

    private DockerRegistryConfig currentlyEditing = null;
    private List<DockerRegistryConfig> initialRegistries;
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDockerRegistryPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 20, 20, 20));
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // --- Left Sidebar: Master List of Registries ---
        VBox leftPane = new VBox();
        leftPane.setPrefWidth(200);
        leftPane.setMinWidth(160);
        leftPane.setMaxWidth(260);
        leftPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 0 0;");

        HBox toolbar = buildToolbar();
        setupRegistryListView();
        VBox.setVgrow(registryListView, Priority.ALWAYS);

        leftPane.getChildren().addAll(toolbar, registryListView);

        // --- Right Pane: Detail Editor ---
        ScrollPane rightScroll = new ScrollPane();
        rightScroll.setFitToWidth(true);
        rightScroll.setStyle("-fx-background-color: transparent; -fx-background: #1E1F22; -fx-border-color: transparent;");

        VBox rightContent = buildDetailContent();
        rightScroll.setContent(rightContent);

        splitPane.getItems().addAll(leftPane, rightScroll);
        splitPane.setDividerPositions(0.22);

        getChildren().add(splitPane);
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(2);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 6, 6, 6));
        toolbar.setStyle("-fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        styleToolbarButton(addButton, "Add Docker Registry");
        styleToolbarButton(removeButton, "Remove Docker Registry");

        addButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("Docker Registry " + (registryList.size() + 1));
            dialog.setTitle("Add Docker Registry");
            dialog.setHeaderText("Specify registry name:");
            dialog.setContentText("Name:");
            dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22;");
            dialog.showAndWait().ifPresent(name -> {
                if (!name.isBlank()) {
                    DockerRegistryConfig cfg = new DockerRegistryConfig(name.trim(), DockerRegistryType.DOCKER_HUB);
                    registryList.add(cfg);
                    registryListView.getSelectionModel().select(cfg);
                    checkModified();
                }
            });
        });

        removeButton.setOnAction(e -> {
            DockerRegistryConfig sel = registryListView.getSelectionModel().getSelectedItem();
            if (sel != null && registryList.size() > 1) {
                registryList.remove(sel);
                checkModified();
            }
        });

        toolbar.getChildren().addAll(addButton, removeButton);
        return toolbar;
    }

    private void styleToolbarButton(Button btn, String tooltipText) {
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 8 3 8;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373B; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 8 3 8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 8 3 8;"));
    }

    private void setupRegistryListView() {
        registryListView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        registryListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DockerRegistryConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label("📦");
                    icon.setStyle("-fx-font-size: 13px;");

                    Label nameLbl = new Label(item.getName());
                    nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

                    row.getChildren().addAll(icon, nameLbl);
                    setGraphic(row);
                    setText(null);

                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-background-radius: 4;");
                    } else {
                        setStyle("-fx-background-color: transparent;");
                    }
                }
            }
        });

        registryListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            saveCurrentDetailValues();
            displayRegistryDetail(newVal);
        });
    }

    private VBox buildDetailContent() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 24, 24, 24));
        box.setStyle("-fx-background-color: #1E1F22;");

        // 1. Name
        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = createLabel("Name:");
        nameField.setPrefWidth(380);
        styleTextField(nameField);
        nameField.textProperty().addListener((obs, o, n) -> checkModified());
        nameRow.getChildren().addAll(nameLbl, nameField);

        // 2. Registry ComboBox
        HBox regRow = new HBox(12);
        regRow.setAlignment(Pos.CENTER_LEFT);
        Label regLbl = createLabel("Registry:");
        registryTypeCombo.getItems().setAll(DockerRegistryType.values());
        registryTypeCombo.setValue(DockerRegistryType.DOCKER_HUB);
        styleComboBox(registryTypeCombo);
        registryTypeCombo.setPrefWidth(380);

        registryTypeCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null && n != DockerRegistryType.OTHER) {
                addressField.setText(n.getDefaultAddress());
                addressField.setDisable(true);
            } else {
                addressField.setDisable(false);
            }
            testConnection();
            checkModified();
        });
        regRow.getChildren().addAll(regLbl, registryTypeCombo);

        // 3. Address
        HBox addrRow = new HBox(12);
        addrRow.setAlignment(Pos.CENTER_LEFT);
        Label addrLbl = createLabel("Address:");
        addressField.setPrefWidth(380);
        styleTextField(addressField);
        addressField.setDisable(true); // default Docker Hub is disabled/read-only
        addressField.textProperty().addListener((obs, o, n) -> checkModified());
        addrRow.getChildren().addAll(addrLbl, addressField);

        // 4. Username
        HBox userRow = new HBox(12);
        userRow.setAlignment(Pos.CENTER_LEFT);
        Label userLbl = createLabel("Username:");
        usernameField.setPrefWidth(380);
        styleTextField(usernameField);
        usernameField.textProperty().addListener((obs, o, n) -> {
            testConnection();
            checkModified();
        });
        userRow.getChildren().addAll(userLbl, usernameField);

        // 5. Password
        HBox passRow = new HBox(12);
        passRow.setAlignment(Pos.CENTER_LEFT);
        Label passLbl = createLabel("Password:");
        passwordField.setPrefWidth(380);
        stylePasswordField(passwordField);
        passwordField.textProperty().addListener((obs, o, n) -> {
            testConnection();
            checkModified();
        });
        passRow.getChildren().addAll(passLbl, passwordField);

        // 6. Test connection section
        Node testConnHeader = createSectionHeader("Test connection");
        testResultLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        VBox.setMargin(testResultLabel, new Insets(4, 0, 0, 0));

        box.getChildren().addAll(
                nameRow,
                regRow,
                addrRow,
                userRow,
                passRow,
                testConnHeader,
                testResultLabel
        );

        return box;
    }

    private Node createSectionHeader(String title) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 0, 4, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #8C9199; -fx-font-size: 13px;");

        Region line = new Region();
        HBox.setHgrow(line, Priority.ALWAYS);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");

        header.getChildren().addAll(label, line);
        return header;
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        lbl.setPrefWidth(90);
        return lbl;
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
    }

    private void stylePasswordField(PasswordField pf) {
        pf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 5 8 5 8;");
    }

    private <T> void styleComboBox(ComboBox<T> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4;");
    }

    private void testConnection() {
        DockerRegistryConfig temp = getCurrentDetailValues();
        String result = manager.testRegistryConnection(temp);
        testResultLabel.setText(result);
        if ("Connection successful".equals(result)) {
            testResultLabel.setStyle("-fx-text-fill: #57965C; -fx-font-size: 13px;");
        } else {
            testResultLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        }
    }

    private void displayRegistryDetail(DockerRegistryConfig config) {
        this.currentlyEditing = config;
        if (config == null) return;

        suppressEvents = true;
        nameField.setText(config.getName());
        registryTypeCombo.setValue(config.getRegistryType());
        addressField.setText(config.getAddress());
        addressField.setDisable(config.getRegistryType() != DockerRegistryType.OTHER);
        usernameField.setText(config.getUsername());
        passwordField.setText(config.getPassword());

        testConnection();
        suppressEvents = false;
    }

    private DockerRegistryConfig getCurrentDetailValues() {
        DockerRegistryConfig cfg = new DockerRegistryConfig();
        cfg.setName(nameField.getText());
        cfg.setRegistryType(registryTypeCombo.getValue() != null ? registryTypeCombo.getValue() : DockerRegistryType.DOCKER_HUB);
        cfg.setAddress(addressField.getText());
        cfg.setUsername(usernameField.getText());
        cfg.setPassword(passwordField.getText());
        return cfg;
    }

    private void saveCurrentDetailValues() {
        if (currentlyEditing != null && !suppressEvents) {
            DockerRegistryConfig updated = getCurrentDetailValues();
            currentlyEditing.setName(updated.getName());
            currentlyEditing.setRegistryType(updated.getRegistryType());
            currentlyEditing.setAddress(updated.getAddress());
            currentlyEditing.setUsername(updated.getUsername());
            currentlyEditing.setPassword(updated.getPassword());
        }
    }

    public void loadData() {
        suppressEvents = true;
        initialRegistries = manager.getRegistries();

        registryList.clear();
        for (DockerRegistryConfig r : initialRegistries) {
            registryList.add(r.clone());
        }

        if (!registryList.isEmpty()) {
            registryListView.getSelectionModel().select(0);
            displayRegistryDetail(registryList.get(0));
        }

        suppressEvents = false;
        checkModified();
    }

    public List<DockerRegistryConfig> getCurrentRegistries() {
        saveCurrentDetailValues();
        List<DockerRegistryConfig> result = new ArrayList<>();
        for (DockerRegistryConfig r : registryList) {
            result.add(r.clone());
        }
        return result;
    }

    public boolean isModified() {
        if (initialRegistries == null) return false;
        saveCurrentDetailValues();
        return !Objects.equals(initialRegistries, registryList);
    }

    public void apply() {
        if (isModified()) {
            saveCurrentDetailValues();
            manager.setRegistries(getCurrentRegistries());
            initialRegistries = manager.getRegistries();
            checkModified();
        }
    }

    public void reset() {
        loadData();
    }

    public void revert() {
        manager.resetToDefaults();
        loadData();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void checkModified() {
        if (suppressEvents) return;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
