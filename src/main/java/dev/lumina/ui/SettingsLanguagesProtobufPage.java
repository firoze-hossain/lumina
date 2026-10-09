package dev.lumina.ui;

import dev.lumina.protobuf.ProtobufImportPath;
import dev.lumina.protobuf.ProtobufSettings;
import dev.lumina.protobuf.ProtobufSettingsManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Languages & Frameworks > Protocol Buffers settings page in Lumina IDE.
 * Matches Image 4 with Auto-Configuration Options, Import Paths table, and Descriptor path.
 */
public class SettingsLanguagesProtobufPage extends VBox {

    private final ProtobufSettingsManager manager = ProtobufSettingsManager.getInstance();
    private Runnable onModifiedListener;

    // Auto-Configuration Checkboxes
    private CheckBox applyThirdPartyCheckBox;
    private CheckBox includeStandardProtoCheckBox;
    private CheckBox includeProjectContentRootsCheckBox;
    private CheckBox searchInIndexesCheckBox;
    private CheckBox includeBundledWellKnownCheckBox;

    // Import Paths Table and Toolbar
    private Button addPathBtn;
    private Button removePathBtn;
    private Button editPathBtn;
    private Button moveUpBtn;
    private Button moveDownBtn;
    private Button copyPathBtn;
    private TableView<ProtobufImportPath> pathsTableView;
    private ObservableList<ProtobufImportPath> pathsList;

    // Descriptor Path
    private ComboBox<String> descriptorPathComboBox;

    private ProtobufSettings initialSettings;

    public SettingsLanguagesProtobufPage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ProtobufSettings current = manager.getSettings();

        // 1. Auto-Configuration Options Section
        Label autoConfigTitle = new Label("Auto-Configuration Options");
        autoConfigTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        applyThirdPartyCheckBox = new CheckBox("Apply third-party configurations");
        applyThirdPartyCheckBox.setSelected(current.isApplyThirdPartyConfigurations());
        styleCheckBox(applyThirdPartyCheckBox);
        applyThirdPartyCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        includeStandardProtoCheckBox = new CheckBox("Include standard proto directories");
        includeStandardProtoCheckBox.setSelected(current.isIncludeStandardProtoDirectories());
        styleCheckBox(includeStandardProtoCheckBox);
        includeStandardProtoCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        includeProjectContentRootsCheckBox = new CheckBox("Include project content roots");
        includeProjectContentRootsCheckBox.setSelected(current.isIncludeProjectContentRoots());
        styleCheckBox(includeProjectContentRootsCheckBox);
        includeProjectContentRootsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        searchInIndexesCheckBox = new CheckBox("Search for imported files in indexes");
        searchInIndexesCheckBox.setSelected(current.isSearchForImportedFilesInIndexes());
        styleCheckBox(searchInIndexesCheckBox);
        searchInIndexesCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        includeBundledWellKnownCheckBox = new CheckBox("Include bundled 'Well Known Proto files'");
        includeBundledWellKnownCheckBox.setSelected(current.isIncludeBundledWellKnownProtoFiles());
        styleCheckBox(includeBundledWellKnownCheckBox);
        includeBundledWellKnownCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        VBox autoConfigBox = new VBox(8,
                autoConfigTitle,
                applyThirdPartyCheckBox,
                includeStandardProtoCheckBox,
                includeProjectContentRootsCheckBox,
                searchInIndexesCheckBox,
                includeBundledWellKnownCheckBox
        );

        // 2. Import Paths Section
        HBox importPathsHeader = createSectionHeader("Import Paths");

        HBox tableToolbar = new HBox(4);
        tableToolbar.setAlignment(Pos.CENTER_LEFT);

        addPathBtn = createIconButton("+", "Add import path");
        removePathBtn = createIconButton("\u2014", "Remove selected path");
        editPathBtn = createIconButton("\u270E", "Edit selected path");
        moveUpBtn = createIconButton("\u2191", "Move Up");
        moveDownBtn = createIconButton("\u2193", "Move Down");
        copyPathBtn = createIconButton("\uD83D\uDCCB", "Copy path to clipboard");

        addPathBtn.setOnAction(e -> handleAddPath());
        removePathBtn.setOnAction(e -> handleRemovePath());
        editPathBtn.setOnAction(e -> handleEditPath());
        moveUpBtn.setOnAction(e -> handleMoveUp());
        moveDownBtn.setOnAction(e -> handleMoveDown());
        copyPathBtn.setOnAction(e -> handleCopyPath());

        tableToolbar.getChildren().addAll(addPathBtn, removePathBtn, editPathBtn, moveUpBtn, moveDownBtn, copyPathBtn);

        // TableView for Import Paths
        pathsList = FXCollections.observableArrayList();
        for (ProtobufImportPath p : current.getImportPaths()) {
            pathsList.add(p.copy());
        }

        pathsTableView = new TableView<>(pathsList);
        pathsTableView.setPrefHeight(180);
        pathsTableView.setMaxHeight(240);
        pathsTableView.setStyle("-fx-background-color: #2B2D30; -fx-control-inner-background: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4;");

        TableColumn<ProtobufImportPath, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLocation()));
        locationCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    ProtobufImportPath rowData = getTableRow() != null ? getTableRow().getItem() : null;
                    if (rowData != null && rowData.isSystem()) {
                        setText("\u24D8 " + item);
                        setStyle("-fx-text-fill: #A0A5B0; -fx-font-size: 13px;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                    }
                }
            }
        });
        locationCol.setPrefWidth(550);

        TableColumn<ProtobufImportPath, String> prefixCol = new TableColumn<>("Prefix");
        prefixCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPrefix()));
        prefixCol.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        prefixCol.setPrefWidth(200);

        pathsTableView.getColumns().addAll(locationCol, prefixCol);
        pathsTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        pathsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean isCustom = newV != null && !newV.isSystem();
            removePathBtn.setDisable(!isCustom);
            editPathBtn.setDisable(!isCustom);
        });
        removePathBtn.setDisable(true);
        editPathBtn.setDisable(true);

        // 3. Descriptor Path Row
        HBox descriptorRow = new HBox(12);
        descriptorRow.setAlignment(Pos.CENTER_LEFT);

        Label descriptorLabel = new Label("Descriptor path:");
        descriptorLabel.setMinWidth(110);
        descriptorLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        List<String> detectedDescs = ProtobufSettings.detectAvailableDescriptorFiles();
        descriptorPathComboBox = new ComboBox<>(FXCollections.observableArrayList(detectedDescs));
        descriptorPathComboBox.setEditable(true);
        descriptorPathComboBox.getEditor().setText(current.getDescriptorPath());
        descriptorPathComboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(descriptorPathComboBox, Priority.ALWAYS);
        styleComboBox(descriptorPathComboBox);

        descriptorPathComboBox.valueProperty().addListener((obs, oldV, newV) -> fireModified());
        descriptorPathComboBox.getEditor().textProperty().addListener((obs, oldV, newV) -> fireModified());

        descriptorRow.getChildren().addAll(descriptorLabel, descriptorPathComboBox);

        getChildren().addAll(
                autoConfigBox,
                importPathsHeader,
                tableToolbar,
                pathsTableView,
                descriptorRow
        );
    }

    private void handleAddPath() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Import Path");
        dialog.setHeaderText("Enter directory path for Protocol Buffer imports:");
        dialog.setContentText("Location:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(loc -> {
            String trimmed = loc.trim();
            if (!trimmed.isEmpty()) {
                pathsList.add(new ProtobufImportPath(trimmed, "", false));
                fireModified();
            }
        });
    }

    private void handleRemovePath() {
        ProtobufImportPath selected = pathsTableView.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isSystem()) {
            pathsList.remove(selected);
            fireModified();
        }
    }

    private void handleEditPath() {
        ProtobufImportPath selected = pathsTableView.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isSystem()) {
            TextInputDialog dialog = new TextInputDialog(selected.getLocation());
            dialog.setTitle("Edit Import Path");
            dialog.setHeaderText("Modify location:");
            dialog.setContentText("Location:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newLoc -> {
                String trimmed = newLoc.trim();
                if (!trimmed.isEmpty()) {
                    selected.setLocation(trimmed);
                    pathsTableView.refresh();
                    fireModified();
                }
            });
        }
    }

    private void handleMoveUp() {
        int idx = pathsTableView.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            ProtobufImportPath item = pathsList.remove(idx);
            pathsList.add(idx - 1, item);
            pathsTableView.getSelectionModel().select(idx - 1);
            fireModified();
        }
    }

    private void handleMoveDown() {
        int idx = pathsTableView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < pathsList.size() - 1) {
            ProtobufImportPath item = pathsList.remove(idx);
            pathsList.add(idx + 1, item);
            pathsTableView.getSelectionModel().select(idx + 1);
            fireModified();
        }
    }

    private void handleCopyPath() {
        ProtobufImportPath selected = pathsTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(selected.getLocation());
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 0, 4, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator line = new Separator();
        line.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(line, Priority.ALWAYS);

        header.getChildren().addAll(label, line);
        return header;
    }

    private Button createIconButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-min-width: 26px; -fx-min-height: 24px; -fx-padding: 2 6 2 6; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-cursor: hand;");
        btn.setTooltip(new Tooltip(tooltipText));
        return btn;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-font-size: 13px;");
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void setOnModified(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void revertChanges() {
        reset();
    }

    public void takeSnapshot() {
        this.initialSettings = buildSettingsFromUi();
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, buildSettingsFromUi());
    }

    public ProtobufSettings buildSettingsFromUi() {
        ProtobufSettings s = new ProtobufSettings();
        s.setApplyThirdPartyConfigurations(applyThirdPartyCheckBox.isSelected());
        s.setIncludeStandardProtoDirectories(includeStandardProtoCheckBox.isSelected());
        s.setIncludeProjectContentRoots(includeProjectContentRootsCheckBox.isSelected());
        s.setSearchForImportedFilesInIndexes(searchInIndexesCheckBox.isSelected());
        s.setIncludeBundledWellKnownProtoFiles(includeBundledWellKnownCheckBox.isSelected());

        List<ProtobufImportPath> list = new ArrayList<>();
        for (ProtobufImportPath p : pathsList) {
            list.add(p.copy());
        }
        s.setImportPaths(list);

        String desc = descriptorPathComboBox.getEditor().getText();
        if (desc == null || desc.isBlank()) {
            desc = descriptorPathComboBox.getValue() != null ? descriptorPathComboBox.getValue() : "";
        }
        s.setDescriptorPath(desc);

        // Keep existing text format setting from manager snapshot
        if (initialSettings != null) {
            s.setWarnAboutMissingSchemaAssociations(initialSettings.isWarnAboutMissingSchemaAssociations());
        }
        return s;
    }

    public void apply() {
        ProtobufSettings s = buildSettingsFromUi();
        manager.setSettings(s);
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        ProtobufSettings current = manager.getSettings();

        applyThirdPartyCheckBox.setSelected(current.isApplyThirdPartyConfigurations());
        includeStandardProtoCheckBox.setSelected(current.isIncludeStandardProtoDirectories());
        includeProjectContentRootsCheckBox.setSelected(current.isIncludeProjectContentRoots());
        searchInIndexesCheckBox.setSelected(current.isSearchForImportedFilesInIndexes());
        includeBundledWellKnownCheckBox.setSelected(current.isIncludeBundledWellKnownProtoFiles());

        pathsList.clear();
        for (ProtobufImportPath p : current.getImportPaths()) {
            pathsList.add(p.copy());
        }

        descriptorPathComboBox.getEditor().setText(current.getDescriptorPath());
        descriptorPathComboBox.setValue(current.getDescriptorPath());

        takeSnapshot();
        fireModified();
    }

    // Public getters for tests
    public CheckBox getApplyThirdPartyCheckBox() { return applyThirdPartyCheckBox; }
    public CheckBox getIncludeStandardProtoCheckBox() { return includeStandardProtoCheckBox; }
    public CheckBox getIncludeProjectContentRootsCheckBox() { return includeProjectContentRootsCheckBox; }
    public CheckBox getSearchInIndexesCheckBox() { return searchInIndexesCheckBox; }
    public CheckBox getIncludeBundledWellKnownCheckBox() { return includeBundledWellKnownCheckBox; }
    public TableView<ProtobufImportPath> getPathsTableView() { return pathsTableView; }
    public ObservableList<ProtobufImportPath> getPathsList() { return pathsList; }
    public ComboBox<String> getDescriptorPathComboBox() { return descriptorPathComboBox; }
    public Button getAddPathBtn() { return addPathBtn; }
    public Button getRemovePathBtn() { return removePathBtn; }
    public Button getEditPathBtn() { return editPathBtn; }
    public Button getMoveUpBtn() { return moveUpBtn; }
    public Button getMoveDownBtn() { return moveDownBtn; }
    public Button getCopyPathBtn() { return copyPathBtn; }
}
