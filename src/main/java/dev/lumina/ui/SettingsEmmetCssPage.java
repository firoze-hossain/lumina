package dev.lumina.ui;

import dev.lumina.settings.EmmetSettings;
import dev.lumina.settings.EmmetSettings.CssPrefixEntry;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;

/**
 * Settings page for Editor > Emmet > CSS.
 * Matches IntelliJ IDEA layout with the 4 options, toolbar (+ / -), and vendor prefixes table.
 */
public class SettingsEmmetCssPage extends VBox {

    private final EmmetSettings workingSettings;
    private Runnable onModifiedListener;

    private final CheckBox enableCssEmmetCheck = new CheckBox("Enable CSS Emmet");
    private final CheckBox enableFuzzySearchCheck = new CheckBox("Enable fuzzy search among CSS abbreviations");
    private final CheckBox enableUnknownPropsCheck = new CheckBox("Enable expansion of unknown properties ('unknown' to 'unknown: ;')");
    private final CheckBox autoInsertPrefixesCheck = new CheckBox("Auto insert css vendor prefixes");

    private final TableView<CssPrefixEntry> prefixTable = new TableView<>();
    private final ObservableList<CssPrefixEntry> tableData = FXCollections.observableArrayList();

    public SettingsEmmetCssPage() {
        this.workingSettings = EmmetSettings.getInstance().copy();
        getStyleClass().add("settings-page");
        setPadding(new Insets(18, 24, 18, 24));
        setSpacing(10);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void buildUI() {
        // Top 4 checkboxes
        enableCssEmmetCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        enableFuzzySearchCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        enableUnknownPropsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        autoInsertPrefixesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        enableCssEmmetCheck.setOnAction(e -> {
            workingSettings.setEnableCssEmmet(enableCssEmmetCheck.isSelected());
            updateEnableState();
            fireModified();
        });

        enableFuzzySearchCheck.setOnAction(e -> {
            workingSettings.setEnableFuzzySearch(enableFuzzySearchCheck.isSelected());
            fireModified();
        });

        enableUnknownPropsCheck.setOnAction(e -> {
            workingSettings.setEnableUnknownProperties(enableUnknownPropsCheck.isSelected());
            fireModified();
        });

        autoInsertPrefixesCheck.setOnAction(e -> {
            workingSettings.setAutoInsertVendorPrefixes(autoInsertPrefixesCheck.isSelected());
            prefixTable.setDisable(!autoInsertPrefixesCheck.isSelected() || !enableCssEmmetCheck.isSelected());
            fireModified();
        });

        VBox topOptions = new VBox(8, enableCssEmmetCheck, enableFuzzySearchCheck, enableUnknownPropsCheck, autoInsertPrefixesCheck);
        topOptions.setPadding(new Insets(0, 0, 6, 0));

        // Toolbar (+ and - buttons)
        Button addBtn = new Button("+");
        addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 8;");
        addBtn.setOnAction(e -> showAddPropertyDialog());

        Button removeBtn = new Button("—");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 8;");
        removeBtn.setOnAction(e -> {
            CssPrefixEntry selected = prefixTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                workingSettings.removeCssProperty(selected.getProperty());
                tableData.remove(selected);
                fireModified();
            }
        });

        HBox toolbar = new HBox(6, addBtn, removeBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 0, 4, 0));

        // TableView
        prefixTable.setItems(tableData);
        prefixTable.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40;");
        prefixTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<CssPrefixEntry, String> propCol = new TableColumn<>("Property");
        propCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProperty()));
        propCol.setPrefWidth(220);
        propCol.setMinWidth(160);

        TableColumn<CssPrefixEntry, Boolean> webkitCol = createCheckboxColumn("-webkit", CssPrefixEntry::isWebkit, CssPrefixEntry::setWebkit);
        TableColumn<CssPrefixEntry, Boolean> mozCol = createCheckboxColumn("-moz", CssPrefixEntry::isMoz, CssPrefixEntry::setMoz);
        TableColumn<CssPrefixEntry, Boolean> msCol = createCheckboxColumn("-ms", CssPrefixEntry::isMs, CssPrefixEntry::setMs);
        TableColumn<CssPrefixEntry, Boolean> oCol = createCheckboxColumn("-o", CssPrefixEntry::isO, CssPrefixEntry::setO);
        TableColumn<CssPrefixEntry, Boolean> khtmlCol = createCheckboxColumn("-khtml", CssPrefixEntry::isKhtml, CssPrefixEntry::setKhtml);

        prefixTable.getColumns().addAll(propCol, webkitCol, mozCol, msCol, oCol, khtmlCol);
        VBox.setVgrow(prefixTable, Priority.ALWAYS);

        getChildren().addAll(topOptions, toolbar, prefixTable);
    }

    private TableColumn<CssPrefixEntry, Boolean> createCheckboxColumn(String title,
                                                                      java.util.function.Function<CssPrefixEntry, Boolean> getter,
                                                                      java.util.function.BiConsumer<CssPrefixEntry, Boolean> setter) {
        TableColumn<CssPrefixEntry, Boolean> col = new TableColumn<>(title);
        col.setPrefWidth(90);
        col.setMinWidth(60);
        col.setCellValueFactory(data -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(getter.apply(data.getValue()));
            prop.addListener((obs, oldVal, newVal) -> {
                setter.accept(data.getValue(), newVal);
                fireModified();
            });
            return prop;
        });
        col.setCellFactory(column -> new CheckBoxTableCell<>() {
            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
            }
        });
        col.setEditable(true);
        return col;
    }

    private void showAddPropertyDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add CSS Property");
        dialog.setHeaderText("Enter CSS property name:");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/lumina-dark.css").toExternalForm());
        dialog.showAndWait().ifPresent(name -> {
            String prop = name.trim().toLowerCase();
            if (!prop.isEmpty()) {
                workingSettings.addOrUpdateCssProperty(prop, false, false, false, false, false);
                tableData.setAll(workingSettings.getCssProperties());
                for (CssPrefixEntry e : tableData) {
                    if (e.getProperty().equalsIgnoreCase(prop)) {
                        prefixTable.getSelectionModel().select(e);
                        prefixTable.scrollTo(e);
                        break;
                    }
                }
                fireModified();
            }
        });
    }

    private void loadData() {
        enableCssEmmetCheck.setSelected(workingSettings.isEnableCssEmmet());
        enableFuzzySearchCheck.setSelected(workingSettings.isEnableFuzzySearch());
        enableUnknownPropsCheck.setSelected(workingSettings.isEnableUnknownProperties());
        autoInsertPrefixesCheck.setSelected(workingSettings.isAutoInsertVendorPrefixes());

        tableData.setAll(workingSettings.getCssProperties());
        updateEnableState();
    }

    private void updateEnableState() {
        boolean cssEnabled = enableCssEmmetCheck.isSelected();
        enableFuzzySearchCheck.setDisable(!cssEnabled);
        enableUnknownPropsCheck.setDisable(!cssEnabled);
        autoInsertPrefixesCheck.setDisable(!cssEnabled);
        prefixTable.setDisable(!cssEnabled || !autoInsertPrefixesCheck.isSelected());
    }

    public void apply() {
        EmmetSettings.getInstance().applyFrom(workingSettings);
        EmmetSettings.getInstance().save();
    }

    public void reset() {
        workingSettings.applyFrom(EmmetSettings.getInstance());
        loadData();
    }

    public boolean isModified() {
        return workingSettings.isModified(EmmetSettings.getInstance());
    }
}
