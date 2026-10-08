package dev.lumina.ui;

import dev.lumina.build.MavenSettings;
import dev.lumina.build.MavenSettingsManager;
import dev.lumina.project.MavenArchetypeMetadata.CatalogEntry;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings subpage for Build, Execution, Deployment > Build Tools > Maven > Archetype Catalogs.
 * Matches 1:1 with reference specification and dynamically manages system and custom catalogs.
 */
public class SettingsMavenArchetypeCatalogsPage extends VBox {

    private final MavenSettingsManager manager = MavenSettingsManager.getInstance();
    private final ObservableList<CatalogEntry> catalogsList = FXCollections.observableArrayList();
    private final TableView<CatalogEntry> catalogsTable = new TableView<>();

    private final Button addBtn = createToolbarButton("+", "Add catalog");
    private final Button removeBtn = createToolbarButton("\u2212", "Remove catalog");
    private final Button editBtn = createToolbarButton("\u270E", "Edit catalog");

    private List<CatalogEntry> initialCustomCatalogs = new ArrayList<>();
    private Runnable onModifiedListener;
    private boolean updating = false;

    public SettingsMavenArchetypeCatalogsPage() {
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    private void buildUI() {
        // Descriptive label matching reference image
        Label descLabel = new Label("Add, remove, and edit your archetype catalogs");
        descLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        getChildren().add(descLabel);

        // Toolbar (+, -, edit)
        removeBtn.setDisable(true);
        editBtn.setDisable(true);

        addBtn.setOnAction(e -> showAddCatalogDialog(null));
        removeBtn.setOnAction(e -> {
            CatalogEntry selected = catalogsTable.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.isSystem()) {
                catalogsList.remove(selected);
                notifyModified();
            }
        });
        editBtn.setOnAction(e -> {
            CatalogEntry selected = catalogsTable.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.isSystem()) {
                showAddCatalogDialog(selected);
            }
        });

        HBox toolbar = new HBox(4, addBtn, removeBtn, editBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 0, 4, 0));
        getChildren().add(toolbar);

        // Table
        setupTable();
        catalogsTable.setItems(catalogsList);
        catalogsTable.setPrefHeight(380);
        VBox.setVgrow(catalogsTable, Priority.ALWAYS);
        getChildren().add(catalogsTable);

        // Selection listener to update toolbar button states
        catalogsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean isCustom = (newSel != null && !newSel.isSystem());
            removeBtn.setDisable(!isCustom);
            editBtn.setDisable(!isCustom);
        });
    }

    private void setupTable() {
        catalogsTable.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-table-cell-border-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        catalogsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<CatalogEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameCol.setMinWidth(150);
        nameCol.setPrefWidth(180);
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

        TableColumn<CatalogEntry, CatalogEntry> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        typeCol.setMinWidth(100);
        typeCol.setPrefWidth(120);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(CatalogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (item.isSystem()) {
                        HBox box = new HBox(6);
                        box.setAlignment(Pos.CENTER_LEFT);
                        SVGPath lock = createLockIcon();
                        Label lbl = new Label("System");
                        lbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 13px;");
                        box.getChildren().addAll(lock, lbl);
                        setGraphic(box);
                        setText(null);
                    } else {
                        setText("Custom");
                        setTextFill(Color.web("#DFE1E5"));
                        setGraphic(null);
                    }
                }
            }
        });

        TableColumn<CatalogEntry, String> locCol = new TableColumn<>("Location");
        locCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().location()));
        locCol.setMinWidth(250);
        locCol.setPrefWidth(420);
        locCol.setCellFactory(col -> new TableCell<>() {
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

        catalogsTable.getColumns().setAll(nameCol, typeCol, locCol);
    }

    private void showAddCatalogDialog(CatalogEntry existingToEdit) {
        Stage dialog = new Stage();
        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existingToEdit == null ? "Add Catalog" : "Edit Catalog");

        Label locLabel = new Label("Location:");
        locLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        locLabel.setPrefWidth(70);

        TextField locField = new TextField(existingToEdit != null ? existingToEdit.location() : "");
        locField.setPromptText("Path to file or URL");
        locField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6C707E; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8;");
        HBox.setHgrow(locField, Priority.ALWAYS);

        Button browseBtn = new Button();
        browseBtn.setGraphic(createFolderBrowseIcon());
        browseBtn.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4; " +
                "-fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 4 8;");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Catalog File (e.g. archetype-catalog.xml)");
            File f = fc.showOpenDialog(dialog);
            if (f != null) {
                locField.setText(f.getAbsolutePath());
            } else {
                DirectoryChooser dc = new DirectoryChooser();
                dc.setTitle("Select Local Maven Repository Directory");
                File d = dc.showDialog(dialog);
                if (d != null) {
                    locField.setText(d.getAbsolutePath());
                }
            }
        });

        HBox locRow = new HBox(8, locField, browseBtn);
        locRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nameLabel.setPrefWidth(70);

        TextField nameField = new TextField(existingToEdit != null ? existingToEdit.name() : "");
        nameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8;");
        HBox.setHgrow(nameField, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(14);
        grid.setPadding(new Insets(18, 20, 16, 20));
        grid.add(locLabel, 0, 0);
        grid.add(locRow, 1, 0);
        grid.add(nameLabel, 0, 1);
        grid.add(nameField, 1, 1);

        ColumnConstraints col0 = new ColumnConstraints(70);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        Button submitBtn = new Button(existingToEdit == null ? "Add" : "Save");
        submitBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-background-radius: 4; -fx-padding: 5 16; -fx-cursor: hand;");
        submitBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String loc = locField.getText().trim();
            if (name.isEmpty()) {
                nameField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; " +
                        "-fx-border-color: #E55353; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8;");
                return;
            }
            if (existingToEdit != null) {
                int idx = catalogsList.indexOf(existingToEdit);
                CatalogEntry updated = new CatalogEntry(name, "Custom", loc, false);
                if (idx >= 0) {
                    catalogsList.set(idx, updated);
                    catalogsTable.getSelectionModel().select(idx);
                }
            } else {
                CatalogEntry entry = new CatalogEntry(name, "Custom", loc, false);
                catalogsList.add(entry);
                catalogsTable.getSelectionModel().select(entry);
            }
            notifyModified();
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 14; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox footer = new HBox(10, cancelBtn, submitBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 20, 16, 20));
        footer.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        BorderPane root = new BorderPane();
        root.setCenter(grid);
        root.setBottom(footer);
        root.setStyle("-fx-background-color: #1E1F22;");

        Scene scene = new Scene(root, 480, 190);
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

    private static SVGPath createLockIcon() {
        SVGPath p = new SVGPath();
        p.setContent("M 3,5 L 3,3 C 3,1.3 4.3,0 6,0 C 7.7,0 9,1.3 9,3 L 9,5 M 1.5,5 L 10.5,5 C 11,5 11.5,5.5 11.5,6 L 11.5,11 C 11.5,11.5 11,12 10.5,12 L 1.5,12 C 1,12 0.5,11.5 0.5,11 L 0.5,6 C 0.5,5.5 1,5 1.5,5 Z M 6,7.5 L 6,9.5");
        p.setStroke(Color.web("#8C919D"));
        p.setStrokeWidth(1.1);
        p.setFill(null);
        return p;
    }

    private static SVGPath createFolderBrowseIcon() {
        SVGPath p = new SVGPath();
        p.setContent("M 2,3 L 6,3 L 7.5,4.5 L 14,4.5 L 14,12 L 2,12 Z");
        p.setStroke(Color.web("#8C919D"));
        p.setStrokeWidth(1.2);
        p.setFill(null);
        return p;
    }

    public void loadData() {
        updating = true;
        MavenSettings s = manager.getSettings();
        initialCustomCatalogs = s.getCustomArchetypeCatalogs();

        catalogsList.setAll(s.getArchetypeCatalogs());
        if (!catalogsList.isEmpty()) {
            catalogsTable.getSelectionModel().select(0);
        }
        updating = false;
    }

    public boolean isModified() {
        List<CatalogEntry> currentCustom = getCurrentCustomCatalogs();
        return !initialCustomCatalogs.equals(currentCustom);
    }

    public void apply() {
        MavenSettings s = manager.getSettings();
        List<CatalogEntry> currentCustom = getCurrentCustomCatalogs();
        s.setCustomArchetypeCatalogs(currentCustom);
        manager.setSettings(s);
        initialCustomCatalogs = new ArrayList<>(currentCustom);
    }

    public void reset() {
        loadData();
    }

    public List<CatalogEntry> getCurrentCustomCatalogs() {
        List<CatalogEntry> list = new ArrayList<>();
        for (CatalogEntry c : catalogsList) {
            if (!c.isSystem()) {
                list.add(c);
            }
        }
        return list;
    }

    public ObservableList<CatalogEntry> getCatalogsList() {
        return catalogsList;
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
