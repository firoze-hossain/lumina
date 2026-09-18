package dev.lumina.ui;

import dev.lumina.project.MavenArchetypeMetadata;
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
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Modal dialog for managing Maven Archetype catalogs matching IntelliJ IDEA.
 */
public class ManageCatalogsDialog {

    private final Stage dialog = new Stage();
    private final ObservableList<CatalogEntry> catalogsList;
    private final TableView<CatalogEntry> catalogsTable = new TableView<>();
    private final Consumer<List<CatalogEntry>> onSave;

    public ManageCatalogsDialog(Window owner, List<CatalogEntry> currentCatalogs, Consumer<List<CatalogEntry>> onSave) {
        this.onSave = onSave;
        this.catalogsList = FXCollections.observableArrayList(
                currentCatalogs != null && !currentCatalogs.isEmpty()
                        ? currentCatalogs
                        : MavenArchetypeMetadata.defaultCatalogs()
        );

        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Manage Catalogs");

        // 1. Toolbar (+, -, edit)
        Button addBtn = createToolbarButton("+", "Add catalog");
        Button removeBtn = createToolbarButton("\u2212", "Remove catalog");
        Button editBtn = createToolbarButton("\u270E", "Edit catalog");

        addBtn.setOnAction(e -> showAddCatalogDialog(null));
        removeBtn.setOnAction(e -> {
            CatalogEntry selected = catalogsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (selected.isSystem()) {
                    showWarning("Cannot Remove", "System catalogs cannot be removed.");
                } else {
                    catalogsList.remove(selected);
                }
            }
        });
        editBtn.setOnAction(e -> {
            CatalogEntry selected = catalogsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (selected.isSystem()) {
                    showWarning("Cannot Edit", "System catalogs cannot be modified.");
                } else {
                    showAddCatalogDialog(selected);
                }
            }
        });

        HBox toolbar = new HBox(4, addBtn, removeBtn, editBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 0, 4, 0));

        // 2. Table
        setupTable();
        catalogsTable.setItems(catalogsList);
        catalogsTable.setPrefHeight(220);

        VBox centerBox = new VBox(6, toolbar, catalogsTable);
        VBox.setVgrow(catalogsTable, Priority.ALWAYS);
        centerBox.setPadding(new Insets(10, 16, 10, 16));

        // 3. Footer buttons
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().setAll("dialog-primary");
        okBtn.setPrefWidth(76);
        okBtn.setOnAction(e -> {
            if (onSave != null) {
                onSave.accept(new ArrayList<>(catalogsList));
            }
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().setAll("dialog-secondary");
        cancelBtn.setPrefWidth(76);
        cancelBtn.setOnAction(e -> dialog.close());

        HBox footer = new HBox(10, okBtn, cancelBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 16, 14, 16));
        footer.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        BorderPane root = new BorderPane();
        root.setCenter(centerBox);
        root.setBottom(footer);
        root.setStyle("-fx-background-color: #1E1F22;");

        Scene scene = new Scene(root, 620, 360);
        String css = getClass().getResource("/css/lumina-dark.css") != null
                ? getClass().getResource("/css/lumina-dark.css").toExternalForm() : null;
        if (css != null) scene.getStylesheets().add(css);
        dialog.setScene(scene);
    }

    public void show() {
        dialog.showAndWait();
    }

    public static Optional<List<CatalogEntry>> show(Window owner, List<CatalogEntry> current) {
        List<CatalogEntry>[] result = new List[1];
        ManageCatalogsDialog dialog = new ManageCatalogsDialog(owner, current, list -> result[0] = list);
        dialog.show();
        return Optional.ofNullable(result[0]);
    }

    private void setupTable() {
        catalogsTable.getStyleClass().setAll("table-view", "env-variables-table");
        catalogsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<CatalogEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameCol.setMinWidth(140);
        nameCol.setPrefWidth(160);

        TableColumn<CatalogEntry, CatalogEntry> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        typeCol.setMinWidth(85);
        typeCol.setPrefWidth(95);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(CatalogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (item.isSystem()) {
                        HBox box = new HBox(5);
                        box.setAlignment(Pos.CENTER_LEFT);
                        SVGPath lock = createLockIcon();
                        Label lbl = new Label("System");
                        lbl.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
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
        locCol.setMinWidth(220);
        locCol.setPrefWidth(330);

        catalogsTable.getColumns().setAll(nameCol, typeCol, locCol);
    }

    private void showAddCatalogDialog(CatalogEntry existingToEdit) {
        Stage addDialog = new Stage();
        addDialog.initOwner(dialog);
        addDialog.initModality(Modality.APPLICATION_MODAL);
        addDialog.setTitle(existingToEdit == null ? "Add Catalog" : "Edit Catalog");

        Label locLabel = new Label("Location:");
        locLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        locLabel.setPrefWidth(65);

        TextField locField = new TextField(existingToEdit != null ? existingToEdit.location() : "");
        locField.setPromptText("Path to file or URL");
        locField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(locField, Priority.ALWAYS);

        Button browseBtn = new Button();
        browseBtn.setGraphic(createFolderBrowseIcon());
        browseBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3 6 3 6;");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Catalog File (e.g. archetype-catalog.xml)");
            File f = fc.showOpenDialog(addDialog);
            if (f != null) {
                locField.setText(f.getAbsolutePath());
            } else {
                DirectoryChooser dc = new DirectoryChooser();
                dc.setTitle("Select Local Maven Repository Directory");
                File d = dc.showDialog(addDialog);
                if (d != null) {
                    locField.setText(d.getAbsolutePath());
                }
            }
        });

        HBox locRow = new HBox(8, locField, browseBtn);
        locRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        nameLabel.setPrefWidth(65);

        TextField nameField = new TextField(existingToEdit != null ? existingToEdit.name() : "");
        nameField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        HBox.setHgrow(nameField, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(16, 18, 12, 18));
        grid.add(locLabel, 0, 0);
        grid.add(locRow, 1, 0);
        grid.add(nameLabel, 0, 1);
        grid.add(nameField, 1, 1);

        ColumnConstraints col0 = new ColumnConstraints(65);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        Button submitBtn = new Button(existingToEdit == null ? "Add" : "Save");
        submitBtn.getStyleClass().setAll("dialog-primary");
        submitBtn.setPrefWidth(76);
        submitBtn.setOnAction(e -> {
            String n = nameField.getText().trim();
            String l = locField.getText().trim();
            if (n.isEmpty()) {
                showWarning("Missing Name", "Catalog name is required.");
                return;
            }
            if (existingToEdit != null) {
                int idx = catalogsList.indexOf(existingToEdit);
                CatalogEntry updated = new CatalogEntry(n, "Custom", l, false);
                if (idx >= 0) {
                    catalogsList.set(idx, updated);
                    catalogsTable.getSelectionModel().select(idx);
                }
            } else {
                CatalogEntry entry = new CatalogEntry(n, "Custom", l, false);
                catalogsList.add(entry);
                catalogsTable.getSelectionModel().select(entry);
            }
            addDialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().setAll("dialog-secondary");
        cancelBtn.setPrefWidth(76);
        cancelBtn.setOnAction(e -> addDialog.close());

        HBox footer = new HBox(10, submitBtn, cancelBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 18, 14, 18));
        footer.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        BorderPane bp = new BorderPane();
        bp.setCenter(grid);
        bp.setBottom(footer);
        bp.setStyle("-fx-background-color: #1E1F22;");

        Scene s = new Scene(bp, 460, 180);
        String css = getClass().getResource("/css/lumina-dark.css") != null
                ? getClass().getResource("/css/lumina-dark.css").toExternalForm() : null;
        if (css != null) s.getStylesheets().add(css);
        addDialog.setScene(s);
        addDialog.showAndWait();
    }

    private Button createToolbarButton(String text, String tooltipText) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C919D; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 4 0 4; -fx-min-width: 22px; -fx-min-height: 22px;");
        b.setTooltip(new Tooltip(tooltipText));
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #2E3136; -fx-text-fill: #DFE1E5; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 4 0 4; -fx-min-width: 22px; -fx-min-height: 22px; -fx-background-radius: 3;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C919D; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 4 0 4; -fx-min-width: 22px; -fx-min-height: 22px;"));
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

    private void showWarning(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(dialog);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
