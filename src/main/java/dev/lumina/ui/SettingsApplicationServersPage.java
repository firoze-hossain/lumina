package dev.lumina.ui;

import dev.lumina.build.ApplicationServer;
import dev.lumina.build.ApplicationServerProvider;
import dev.lumina.build.ApplicationServersManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings page for Build, Execution, Deployment > Application Servers.
 * Matches the exact master-detail layout, popup menu, and empty states from reference specification.
 */
public class SettingsApplicationServersPage extends BorderPane {

    private final ApplicationServersManager manager = ApplicationServersManager.getInstance();

    private final ObservableList<ApplicationServer> workingServers = FXCollections.observableArrayList();
    private List<ApplicationServer> initialServers = new ArrayList<>();

    private final ListView<ApplicationServer> serverListView = new ListView<>(workingServers);
    private final Label emptyListLabel = new Label("Not configured");
    private final StackPane listContainer = new StackPane();

    private final StackPane detailContainer = new StackPane();
    private final Label noSelectionLabel = new Label("No Application Server Selected");
    private final VBox detailForm = new VBox(14);

    private final TextField nameField = new TextField();
    private final TextField homeField = new TextField();
    private final TextField baseField = new TextField();
    private final Label homeLabel = new Label("Application Server Home:");
    private final Label baseLabel = new Label("Base directory:");
    private final HBox baseRow = new HBox(10);
    private final Label versionLabel = new Label();
    private final ListView<String> librariesListView = new ListView<>();

    private final Button addButton = new Button("+");
    private final Button removeButton = new Button("—");

    private ApplicationServer selectedServer;
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsApplicationServersPage() {
        setStyle("-fx-background-color: #1E1F22;");
        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- LEFT PANE (Master list & toolbar) ---
        VBox leftPane = new VBox();
        leftPane.setPrefWidth(240);
        leftPane.setMinWidth(200);
        leftPane.setMaxWidth(300);
        leftPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 0 0;");

        // Toolbar
        HBox toolbar = new HBox(2);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 8, 6, 8));
        toolbar.setStyle("-fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        styleToolbarButton(addButton);
        styleToolbarButton(removeButton);
        removeButton.setDisable(true);

        // Add server context menu
        ContextMenu addMenu = new ContextMenu();
        addMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");

        MenuItem headerItem = new MenuItem("Add application server");
        headerItem.setDisable(true);
        headerItem.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 11px; -fx-opacity: 1.0; -fx-padding: 4 10;");

        addMenu.getItems().add(headerItem);
        addMenu.getItems().add(new SeparatorMenuItem());

        for (ApplicationServerProvider provider : manager.getProviders()) {
            MenuItem item = new MenuItem(provider.getDisplayName(), provider.createIcon());
            item.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 10;");
            item.setOnAction(e -> handleAddServer(provider));
            addMenu.getItems().add(item);
        }

        addButton.setOnAction(e -> addMenu.show(addButton, Side.BOTTOM, 0, 2));

        removeButton.setOnAction(e -> handleRemoveServer());

        toolbar.getChildren().addAll(addButton, removeButton);

        // List and empty placeholder
        emptyListLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        emptyListLabel.setAlignment(Pos.CENTER);

        serverListView.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
        serverListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ApplicationServer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getName());
                    ApplicationServerProvider p = manager.getProvider(item.getTypeId());
                    setGraphic(p != null ? p.createIcon() : null);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
                }
            }
        });

        serverListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedServer = newVal;
            removeButton.setDisable(newVal == null);
            updateDetailPane();
        });

        listContainer.getChildren().setAll(serverListView, emptyListLabel);
        VBox.setVgrow(listContainer, Priority.ALWAYS);

        leftPane.getChildren().addAll(toolbar, listContainer);
        setLeft(leftPane);

        // --- RIGHT PANE (Detail form) ---
        noSelectionLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 13px;");
        noSelectionLabel.setAlignment(Pos.CENTER);

        detailForm.setPadding(new Insets(20, 24, 20, 24));
        detailForm.setStyle("-fx-background-color: #1E1F22;");

        // Row 1: Name
        HBox nameRow = new HBox(10);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label("Name:");
        nameLbl.setPrefWidth(160);
        nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        styleTextField(nameField);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        nameField.textProperty().addListener((obs, o, n) -> {
            if (!updating && selectedServer != null) {
                selectedServer.setName(n);
                serverListView.refresh();
                notifyModified();
            }
        });
        nameRow.getChildren().addAll(nameLbl, nameField);

        // Row 2: Home Directory
        HBox homeRow = new HBox(10);
        homeRow.setAlignment(Pos.CENTER_LEFT);
        homeLabel.setPrefWidth(160);
        homeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        styleTextField(homeField);
        HBox.setHgrow(homeField, Priority.ALWAYS);
        Button browseHome = new Button("…");
        styleBrowseButton(browseHome);
        browseHome.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Application Server Home Directory");
            if (homeField.getText() != null && !homeField.getText().isBlank()) {
                File cur = new File(homeField.getText());
                if (cur.isDirectory()) dc.setInitialDirectory(cur);
            }
            File chosen = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (chosen != null) {
                homeField.setText(chosen.getAbsolutePath());
                if (selectedServer != null) {
                    ApplicationServerProvider p = manager.getProvider(selectedServer.getTypeId());
                    if (p != null) {
                        String ver = p.detectVersion(chosen.getAbsolutePath());
                        selectedServer.setVersion(ver);
                        selectedServer.setLibraries(p.detectLibraries(chosen.getAbsolutePath()));
                        versionLabel.setText(ver);
                        librariesListView.getItems().setAll(selectedServer.getLibraries());
                    }
                }
            }
        });
        homeField.textProperty().addListener((obs, o, n) -> {
            if (!updating && selectedServer != null) {
                selectedServer.setHomePath(n);
                notifyModified();
            }
        });
        homeRow.getChildren().addAll(homeLabel, homeField, browseHome);

        // Row 3: Base Directory
        baseRow.setAlignment(Pos.CENTER_LEFT);
        baseLabel.setPrefWidth(160);
        baseLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        styleTextField(baseField);
        HBox.setHgrow(baseField, Priority.ALWAYS);
        Button browseBase = new Button("…");
        styleBrowseButton(browseBase);
        browseBase.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Application Server Base Directory");
            File chosen = dc.showDialog(getScene() != null ? getScene().getWindow() : null);
            if (chosen != null) {
                baseField.setText(chosen.getAbsolutePath());
            }
        });
        baseField.textProperty().addListener((obs, o, n) -> {
            if (!updating && selectedServer != null) {
                selectedServer.setBaseDirectory(n);
                notifyModified();
            }
        });
        baseRow.getChildren().addAll(baseLabel, baseField, browseBase);

        // Row 4: Version
        HBox versionRow = new HBox(10);
        versionRow.setAlignment(Pos.CENTER_LEFT);
        Label verTitle = new Label("Detected version:");
        verTitle.setPrefWidth(160);
        verTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        versionLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 13px;");
        versionRow.getChildren().addAll(verTitle, versionLabel);

        // Row 5: Libraries
        Label libsTitle = new Label("Libraries:");
        libsTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        librariesListView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4;");
        librariesListView.setPrefHeight(220);
        librariesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 6;");
                }
            }
        });

        detailForm.getChildren().addAll(nameRow, homeRow, baseRow, versionRow, libsTitle, librariesListView);

        detailContainer.getChildren().setAll(noSelectionLabel);
        setCenter(detailContainer);
    }

    private void styleToolbarButton(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 14px; " +
                "-fx-min-width: 26px; -fx-min-height: 24px; -fx-padding: 0;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373B; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-min-width: 26px; -fx-min-height: 24px; -fx-padding: 0;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-min-width: 26px; -fx-min-height: 24px; -fx-padding: 0;"));
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8;");
    }

    private void styleBrowseButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 10;");
    }

    private void handleAddServer(ApplicationServerProvider provider) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select " + provider.getDisplayName() + " Home Directory");
        File chosen = dc.showDialog(getScene() != null ? getScene().getWindow() : null);

        String path = chosen != null ? chosen.getAbsolutePath() : "";
        ApplicationServer server = provider.createServer(path);

        // If duplicate name, generate distinct name
        long sameCount = workingServers.stream().filter(s -> s.getTypeId().equals(provider.getTypeId())).count();
        if (sameCount > 0) {
            server.setName(server.getName() + " (" + (sameCount + 1) + ")");
        }

        workingServers.add(server);
        updateListVisibility();
        serverListView.getSelectionModel().select(server);
        notifyModified();
    }

    private void handleRemoveServer() {
        if (selectedServer != null) {
            int idx = serverListView.getSelectionModel().getSelectedIndex();
            workingServers.remove(selectedServer);
            updateListVisibility();
            if (!workingServers.isEmpty()) {
                int nextIdx = Math.min(idx, workingServers.size() - 1);
                serverListView.getSelectionModel().select(nextIdx);
            } else {
                selectedServer = null;
                updateDetailPane();
            }
            notifyModified();
        }
    }

    private void updateListVisibility() {
        boolean empty = workingServers.isEmpty();
        emptyListLabel.setVisible(empty);
        serverListView.setVisible(!empty);
    }

    private void updateDetailPane() {
        if (selectedServer == null) {
            detailContainer.getChildren().setAll(noSelectionLabel);
        } else {
            updating = true;
            nameField.setText(selectedServer.getName());
            homeField.setText(selectedServer.getHomePath());
            baseField.setText(selectedServer.getBaseDirectory() != null ? selectedServer.getBaseDirectory() : "");
            versionLabel.setText(selectedServer.getVersion() != null ? selectedServer.getVersion() : "Unknown");
            librariesListView.getItems().setAll(selectedServer.getLibraries());

            ApplicationServerProvider p = manager.getProvider(selectedServer.getTypeId());
            if (p != null) {
                homeLabel.setText(p.getDisplayName() + " Home:");
                baseRow.setVisible(p.supportsBaseDirectory());
                baseRow.setManaged(p.supportsBaseDirectory());
            } else {
                homeLabel.setText("Application Server Home:");
                baseRow.setVisible(false);
                baseRow.setManaged(false);
            }

            detailContainer.getChildren().setAll(detailForm);
            updating = false;
        }
    }

    public void loadData() {
        updating = true;
        initialServers.clear();
        for (ApplicationServer s : manager.getServers()) {
            initialServers.add(s.clone());
        }

        workingServers.clear();
        for (ApplicationServer s : initialServers) {
            workingServers.add(s.clone());
        }

        updateListVisibility();

        if (!workingServers.isEmpty()) {
            serverListView.getSelectionModel().select(0);
        } else {
            selectedServer = null;
            updateDetailPane();
        }
        updating = false;
    }

    public boolean isModified() {
        if (initialServers.size() != workingServers.size()) return true;
        for (int i = 0; i < initialServers.size(); i++) {
            if (!initialServers.get(i).equals(workingServers.get(i))) {
                return true;
            }
        }
        return false;
    }

    public void apply() {
        List<ApplicationServer> toSave = new ArrayList<>();
        for (ApplicationServer s : workingServers) {
            toSave.add(s.clone());
        }
        manager.setServers(toSave);
        initialServers.clear();
        for (ApplicationServer s : toSave) {
            initialServers.add(s.clone());
        }
    }

    public void reset() {
        loadData();
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
