package dev.lumina.ui;

import dev.lumina.scala.ScalaBasePackageEntry;
import dev.lumina.scala.ScalaBasePackageSettings;
import dev.lumina.scala.ScalaLanguageSettingsManager;
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
 * Languages & Frameworks > Scala > Base Package settings page in Lumina IDE.
 * Matches Image 4:
 *  - Radio: (o) Inherit from Package Prefix of a Source Folder with (?) help icon
 *  - Radio: ( ) Use custom:
 *  - TableView with columns: Module, Base Package
 *  - Toolbar: +, -
 *  - Placeholder: Nothing to show
 */
public class SettingsLanguagesScalaBasePackagePage extends VBox {

    private final ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private RadioButton inheritRadioButton;
    private RadioButton customRadioButton;
    private ToggleGroup toggleGroup;

    private Button addBtn;
    private Button removeBtn;
    private TableView<BasePackageRow> table;
    private ObservableList<BasePackageRow> tableItems;

    private ScalaBasePackageSettings initialSettings;

    public static class BasePackageRow {
        private final SimpleStringProperty module;
        private final SimpleStringProperty basePackage;

        public BasePackageRow(String module, String basePackage) {
            this.module = new SimpleStringProperty(module != null ? module : "");
            this.basePackage = new SimpleStringProperty(basePackage != null ? basePackage : "");
        }

        public String getModule() {
            return module.get();
        }

        public void setModule(String module) {
            this.module.set(module);
        }

        public SimpleStringProperty moduleProperty() {
            return module;
        }

        public String getBasePackage() {
            return basePackage.get();
        }

        public void setBasePackage(String basePackage) {
            this.basePackage.set(basePackage);
        }

        public SimpleStringProperty basePackageProperty() {
            return basePackage;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BasePackageRow that = (BasePackageRow) o;
            return Objects.equals(getModule(), that.getModule()) &&
                    Objects.equals(getBasePackage(), that.getBasePackage());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getModule(), getBasePackage());
        }
    }

    public SettingsLanguagesScalaBasePackagePage() {
        setSpacing(12);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ScalaBasePackageSettings current = manager.getBasePackageSettings();

        // 1. Radio buttons
        toggleGroup = new ToggleGroup();

        HBox inheritRow = new HBox(6);
        inheritRow.setAlignment(Pos.CENTER_LEFT);

        inheritRadioButton = new RadioButton("Inherit from Package Prefix of a Source Folder");
        inheritRadioButton.setToggleGroup(toggleGroup);
        inheritRadioButton.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Label helpIcon = new Label("(?)");
        helpIcon.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-cursor: hand;");
        helpIcon.setTooltip(new Tooltip("Derives the Scala base package prefix directly from the source folder prefix settings"));

        inheritRow.getChildren().addAll(inheritRadioButton, helpIcon);

        customRadioButton = new RadioButton("Use custom:");
        customRadioButton.setToggleGroup(toggleGroup);
        customRadioButton.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        if (current.isInheritFromPackagePrefix()) {
            inheritRadioButton.setSelected(true);
        } else {
            customRadioButton.setSelected(true);
        }

        toggleGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            updateTableEnabledState();
            fireModified();
        });

        // 2. Toolbar & Table
        VBox tableBox = new VBox(6);
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        addBtn = new Button("+");
        styleToolbarButton(addBtn);
        addBtn.setOnAction(e -> addRow());

        removeBtn = new Button("-");
        styleToolbarButton(removeBtn);
        removeBtn.setOnAction(e -> removeSelectedRow());

        toolbar.getChildren().addAll(addBtn, removeBtn);

        table = new TableView<>();
        table.setEditable(true);
        table.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4;");
        Label placeholder = new Label("Nothing to show");
        placeholder.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");
        table.setPlaceholder(placeholder);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<BasePackageRow, String> moduleCol = new TableColumn<>("Module");
        moduleCol.setCellValueFactory(data -> data.getValue().moduleProperty());
        moduleCol.setCellFactory(TextFieldTableCell.forTableColumn());
        moduleCol.setOnEditCommit(e -> {
            e.getRowValue().setModule(e.getNewValue());
            fireModified();
        });
        moduleCol.setPrefWidth(220);

        TableColumn<BasePackageRow, String> pkgCol = new TableColumn<>("Base Package");
        pkgCol.setCellValueFactory(data -> data.getValue().basePackageProperty());
        pkgCol.setCellFactory(TextFieldTableCell.forTableColumn());
        pkgCol.setOnEditCommit(e -> {
            e.getRowValue().setBasePackage(e.getNewValue());
            fireModified();
        });
        pkgCol.prefWidthProperty().bind(table.widthProperty().subtract(240));

        table.getColumns().addAll(moduleCol, pkgCol);

        tableItems = FXCollections.observableArrayList();
        for (ScalaBasePackageEntry entry : current.getCustomBasePackages()) {
            tableItems.add(new BasePackageRow(entry.getModule(), entry.getBasePackage()));
        }
        table.setItems(tableItems);

        tableBox.getChildren().addAll(toolbar, table);

        getChildren().addAll(inheritRow, customRadioButton, tableBox);

        updateTableEnabledState();
    }

    private void updateTableEnabledState() {
        boolean customSelected = customRadioButton.isSelected();
        addBtn.setDisable(!customSelected);
        removeBtn.setDisable(!customSelected);
        table.setDisable(!customSelected);
        table.setOpacity(customSelected ? 1.0 : 0.6);
    }

    private void addRow() {
        BasePackageRow newRow = new BasePackageRow("Module" + (tableItems.size() + 1), "com.example");
        tableItems.add(newRow);
        table.getSelectionModel().select(newRow);
        fireModified();
    }

    private void removeSelectedRow() {
        BasePackageRow selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            tableItems.remove(selected);
            fireModified();
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

    public ScalaBasePackageSettings getCurrentUiSettings() {
        List<ScalaBasePackageEntry> entries = new ArrayList<>();
        for (BasePackageRow row : tableItems) {
            entries.add(new ScalaBasePackageEntry(row.getModule(), row.getBasePackage()));
        }
        return new ScalaBasePackageSettings(inheritRadioButton.isSelected(), entries);
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentUiSettings());
    }

    public void apply() {
        manager.setBasePackageSettings(getCurrentUiSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        ScalaBasePackageSettings saved = manager.getBasePackageSettings();
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
        applySettingsToUi(new ScalaBasePackageSettings());
        fireModified();
    }

    private void applySettingsToUi(ScalaBasePackageSettings s) {
        if (s == null) return;
        if (s.isInheritFromPackagePrefix()) {
            inheritRadioButton.setSelected(true);
        } else {
            customRadioButton.setSelected(true);
        }
        tableItems.clear();
        for (ScalaBasePackageEntry entry : s.getCustomBasePackages()) {
            tableItems.add(new BasePackageRow(entry.getModule(), entry.getBasePackage()));
        }
        updateTableEnabledState();
    }

    // Direct UI accessors for tests
    public RadioButton getInheritRadioButton() {
        return inheritRadioButton;
    }

    public RadioButton getCustomRadioButton() {
        return customRadioButton;
    }

    public TableView<BasePackageRow> getTable() {
        return table;
    }

    public ObservableList<BasePackageRow> getTableItems() {
        return tableItems;
    }

    public Button getAddBtn() {
        return addBtn;
    }

    public Button getRemoveBtn() {
        return removeBtn;
    }
}
