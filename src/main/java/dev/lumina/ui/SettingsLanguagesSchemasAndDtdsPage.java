package dev.lumina.ui;

import dev.lumina.schemas.ExternalResourceEntry;
import dev.lumina.schemas.SchemasAndDtdsSettings;
import dev.lumina.schemas.SchemasAndDtdsSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Languages & Frameworks > Schemas and DTDs settings page in Lumina IDE.
 * Matches Image 3:
 *  - External schemas and DTDs (Toolbar +, -, edit, Table with "No external resources")
 *  - Ignored schemas and DTDs (Toolbar +, -, edit, List with 8 default URIs)
 */
public class SettingsLanguagesSchemasAndDtdsPage extends VBox {

    private final SchemasAndDtdsSettingsManager manager = SchemasAndDtdsSettingsManager.getInstance();
    private Runnable onModifiedListener;

    // External resources
    private Button addExtBtn;
    private Button removeExtBtn;
    private Button editExtBtn;
    private TableView<ExternalResourceRow> extTable;
    private ObservableList<ExternalResourceRow> extItems;

    // Ignored schemas
    private Button addIgnBtn;
    private Button removeIgnBtn;
    private Button editIgnBtn;
    private ListView<String> ignoredListView;
    private ObservableList<String> ignoredItems;

    private SchemasAndDtdsSettings initialSettings;

    public static class ExternalResourceRow {
        private final SimpleStringProperty uri;
        private final SimpleStringProperty location;

        public ExternalResourceRow(String uri, String location) {
            this.uri = new SimpleStringProperty(uri != null ? uri : "");
            this.location = new SimpleStringProperty(location != null ? location : "");
        }

        public String getUri() {
            return uri.get();
        }

        public void setUri(String uri) {
            this.uri.set(uri);
        }

        public SimpleStringProperty uriProperty() {
            return uri;
        }

        public String getLocation() {
            return location.get();
        }

        public void setLocation(String location) {
            this.location.set(location);
        }

        public SimpleStringProperty locationProperty() {
            return location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExternalResourceRow that = (ExternalResourceRow) o;
            return Objects.equals(getUri(), that.getUri()) &&
                    Objects.equals(getLocation(), that.getLocation());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getUri(), getLocation());
        }
    }

    public SettingsLanguagesSchemasAndDtdsPage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        SchemasAndDtdsSettings current = manager.getSchemasAndDtdsSettings();

        // 1. External schemas and DTDs section
        Label extTitle = new Label("External schemas and DTDs:");
        extTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox extToolbar = new HBox(4);
        extToolbar.setAlignment(Pos.CENTER_LEFT);

        addExtBtn = new Button("+");
        styleToolbarButton(addExtBtn);
        addExtBtn.setOnAction(e -> addExternalResource());

        removeExtBtn = new Button("-");
        styleToolbarButton(removeExtBtn);
        removeExtBtn.setOnAction(e -> removeSelectedExternalResource());

        editExtBtn = new Button("\u270E"); // pencil edit
        styleToolbarButton(editExtBtn);
        editExtBtn.setOnAction(e -> editSelectedExternalResource());

        extToolbar.getChildren().addAll(addExtBtn, removeExtBtn, editExtBtn);

        extTable = new TableView<>();
        extTable.setEditable(true);
        extTable.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4;");
        Label extPlaceholder = new Label("No external resources");
        extPlaceholder.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");
        extTable.setPlaceholder(extPlaceholder);
        extTable.setPrefHeight(180);

        TableColumn<ExternalResourceRow, String> uriCol = new TableColumn<>("URI");
        uriCol.setCellValueFactory(data -> data.getValue().uriProperty());
        uriCol.setCellFactory(TextFieldTableCell.forTableColumn());
        uriCol.setOnEditCommit(e -> {
            e.getRowValue().setUri(e.getNewValue());
            fireModified();
        });
        uriCol.setPrefWidth(280);

        TableColumn<ExternalResourceRow, String> locCol = new TableColumn<>("Location");
        locCol.setCellValueFactory(data -> data.getValue().locationProperty());
        locCol.setCellFactory(TextFieldTableCell.forTableColumn());
        locCol.setOnEditCommit(e -> {
            e.getRowValue().setLocation(e.getNewValue());
            fireModified();
        });
        locCol.prefWidthProperty().bind(extTable.widthProperty().subtract(300));

        extTable.getColumns().addAll(uriCol, locCol);

        extItems = FXCollections.observableArrayList();
        for (ExternalResourceEntry entry : current.getExternalResources()) {
            extItems.add(new ExternalResourceRow(entry.getUri(), entry.getLocation()));
        }
        extTable.setItems(extItems);

        VBox extBox = new VBox(6, extTitle, extToolbar, extTable);

        // 2. Ignored schemas and DTDs section
        Label ignTitle = new Label("Ignored schemas and DTDs:");
        ignTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox ignToolbar = new HBox(4);
        ignToolbar.setAlignment(Pos.CENTER_LEFT);

        addIgnBtn = new Button("+");
        styleToolbarButton(addIgnBtn);
        addIgnBtn.setOnAction(e -> addIgnoredSchema());

        removeIgnBtn = new Button("-");
        styleToolbarButton(removeIgnBtn);
        removeIgnBtn.setOnAction(e -> removeSelectedIgnoredSchema());

        editIgnBtn = new Button("\u270E");
        styleToolbarButton(editIgnBtn);
        editIgnBtn.setOnAction(e -> editSelectedIgnoredSchema());

        ignToolbar.getChildren().addAll(addIgnBtn, removeIgnBtn, editIgnBtn);

        ignoredListView = new ListView<>();
        ignoredListView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4;");
        ignoredListView.setPrefHeight(220);
        VBox.setVgrow(ignoredListView, Priority.ALWAYS);

        ignoredItems = FXCollections.observableArrayList(current.getIgnoredSchemas());
        ignoredListView.setItems(ignoredItems);

        VBox ignBox = new VBox(6, ignTitle, ignToolbar, ignoredListView);
        VBox.setVgrow(ignBox, Priority.ALWAYS);

        getChildren().addAll(extBox, ignBox);
    }

    private void addExternalResource() {
        ExternalResourceRow newRow = new ExternalResourceRow("http://example.com/schema.xsd", "/path/to/schema.xsd");
        extItems.add(newRow);
        extTable.getSelectionModel().select(newRow);
        fireModified();
    }

    private void removeSelectedExternalResource() {
        ExternalResourceRow selected = extTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            extItems.remove(selected);
            fireModified();
        }
    }

    private void editSelectedExternalResource() {
        int idx = extTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            extTable.edit(idx, extTable.getColumns().get(0));
        }
    }

    private void addIgnoredSchema() {
        TextInputDialog dialog = new TextInputDialog("http://example.com/ignored-schema");
        dialog.setTitle("Add Ignored Schema");
        dialog.setHeaderText("Enter Schema URI to ignore:");
        dialog.showAndWait().ifPresent(uri -> {
            if (!uri.trim().isEmpty() && !ignoredItems.contains(uri.trim())) {
                ignoredItems.add(uri.trim());
                fireModified();
            }
        });
    }

    private void removeSelectedIgnoredSchema() {
        String selected = ignoredListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ignoredItems.remove(selected);
            fireModified();
        }
    }

    private void editSelectedIgnoredSchema() {
        String selected = ignoredListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            TextInputDialog dialog = new TextInputDialog(selected);
            dialog.setTitle("Edit Ignored Schema");
            dialog.setHeaderText("Edit Schema URI:");
            dialog.showAndWait().ifPresent(uri -> {
                if (!uri.trim().isEmpty()) {
                    int idx = ignoredListView.getSelectionModel().getSelectedIndex();
                    ignoredItems.set(idx, uri.trim());
                    fireModified();
                }
            });
        }
    }

    private void styleToolbarButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 10; -fx-cursor: hand; -fx-font-weight: bold;");
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void takeSnapshot() {
        this.initialSettings = getCurrentUiSettings();
    }

    public SchemasAndDtdsSettings getCurrentUiSettings() {
        List<ExternalResourceEntry> extList = new ArrayList<>();
        for (ExternalResourceRow row : extItems) {
            extList.add(new ExternalResourceEntry(row.getUri(), row.getLocation()));
        }
        return new SchemasAndDtdsSettings(extList, new ArrayList<>(ignoredItems));
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentUiSettings());
    }

    public void apply() {
        manager.setSchemasAndDtdsSettings(getCurrentUiSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        SchemasAndDtdsSettings saved = manager.getSchemasAndDtdsSettings();
        applySettingsToUi(saved);
        takeSnapshot();
        fireModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            applySettingsToUi(initialSettings);
            fireModified();
        }
    }

    public void resetDefaults() {
        applySettingsToUi(new SchemasAndDtdsSettings());
        fireModified();
    }

    private void applySettingsToUi(SchemasAndDtdsSettings s) {
        if (s == null) return;
        extItems.clear();
        for (ExternalResourceEntry entry : s.getExternalResources()) {
            extItems.add(new ExternalResourceRow(entry.getUri(), entry.getLocation()));
        }
        ignoredItems.setAll(s.getIgnoredSchemas());
    }

    // Direct UI accessors for tests
    public TableView<ExternalResourceRow> getExtTable() {
        return extTable;
    }

    public ObservableList<ExternalResourceRow> getExtItems() {
        return extItems;
    }

    public ListView<String> getIgnoredListView() {
        return ignoredListView;
    }

    public ObservableList<String> getIgnoredItems() {
        return ignoredItems;
    }

    public Button getAddExtBtn() {
        return addExtBtn;
    }

    public Button getRemoveExtBtn() {
        return removeExtBtn;
    }

    public Button getAddIgnBtn() {
        return addIgnBtn;
    }

    public Button getRemoveIgnBtn() {
        return removeIgnBtn;
    }
}
