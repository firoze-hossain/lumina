package dev.lumina.ui;

import dev.lumina.scala.ScalaInterpolatedStringInjection;
import dev.lumina.scala.ScalaLanguageSettingsManager;
import dev.lumina.scala.ScalaMiscSettings;
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
 * Languages & Frameworks > Scala > Misc settings page in Lumina IDE.
 * Matches Image 5:
 *  - ScalaTest default super class: [org.scalatest.funsuite.AnyFunSuiteLike]
 *  - Trailing commas: [Auto]
 *  - Section: Language Injection Settings for Interpolated Strings
 *  - Toolbar: +, -
 *  - TableView with columns: Interpolated String prefix, Language ID (19 default rules)
 */
public class SettingsLanguagesScalaMiscPage extends VBox {

    private final ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private TextField scalaTestSuperClassField;
    private ComboBox<String> trailingCommasComboBox;

    private Button addBtn;
    private Button removeBtn;
    private TableView<InjectionRow> table;
    private ObservableList<InjectionRow> tableItems;

    private ScalaMiscSettings initialSettings;

    public static class InjectionRow {
        private final SimpleStringProperty prefix;
        private final SimpleStringProperty languageId;

        public InjectionRow(String prefix, String languageId) {
            this.prefix = new SimpleStringProperty(prefix != null ? prefix : "");
            this.languageId = new SimpleStringProperty(languageId != null ? languageId : "");
        }

        public String getPrefix() {
            return prefix.get();
        }

        public void setPrefix(String prefix) {
            this.prefix.set(prefix);
        }

        public SimpleStringProperty prefixProperty() {
            return prefix;
        }

        public String getLanguageId() {
            return languageId.get();
        }

        public void setLanguageId(String languageId) {
            this.languageId.set(languageId);
        }

        public SimpleStringProperty languageIdProperty() {
            return languageId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InjectionRow that = (InjectionRow) o;
            return Objects.equals(getPrefix(), that.getPrefix()) &&
                    Objects.equals(getLanguageId(), that.getLanguageId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getPrefix(), getLanguageId());
        }
    }

    public SettingsLanguagesScalaMiscPage() {
        setSpacing(12);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ScalaMiscSettings current = manager.getMiscSettings();

        // 1. ScalaTest super class row
        HBox superClassRow = new HBox(12);
        superClassRow.setAlignment(Pos.CENTER_LEFT);
        Label superClassLabel = createLabel("ScalaTest default super class:");
        scalaTestSuperClassField = new TextField(current.getScalaTestDefaultSuperClass());
        scalaTestSuperClassField.setPrefWidth(350);
        styleTextField(scalaTestSuperClassField);
        scalaTestSuperClassField.textProperty().addListener((obs, oldV, newV) -> fireModified());
        superClassRow.getChildren().addAll(superClassLabel, scalaTestSuperClassField);

        // 2. Trailing commas row
        HBox trailingRow = new HBox(12);
        trailingRow.setAlignment(Pos.CENTER_LEFT);
        Label trailingLabel = createLabel("Trailing commas:");
        trailingCommasComboBox = new ComboBox<>(FXCollections.observableArrayList("Auto", "Always", "Never"));
        trailingCommasComboBox.setValue(current.getTrailingCommas());
        trailingCommasComboBox.setPrefWidth(120);
        styleComboBox(trailingCommasComboBox);
        trailingCommasComboBox.valueProperty().addListener((obs, oldV, newV) -> fireModified());
        trailingRow.getChildren().addAll(trailingLabel, trailingCommasComboBox);

        // 3. Section Header: Language Injection Settings for Interpolated Strings
        HBox injectionHeader = new HBox(8);
        injectionHeader.setAlignment(Pos.CENTER_LEFT);
        injectionHeader.setPadding(new Insets(10, 0, 4, 0));

        Label sectionTitle = new Label("Language Injection Settings for Interpolated Strings");
        sectionTitle.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        injectionHeader.getChildren().addAll(sectionTitle, sep);

        // 4. Toolbar & Table
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
        Label placeholder = new Label("No language injections configured");
        placeholder.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");
        table.setPlaceholder(placeholder);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<InjectionRow, String> prefixCol = new TableColumn<>("Interpolated String prefix");
        prefixCol.setCellValueFactory(data -> data.getValue().prefixProperty());
        prefixCol.setCellFactory(TextFieldTableCell.forTableColumn());
        prefixCol.setOnEditCommit(e -> {
            e.getRowValue().setPrefix(e.getNewValue());
            fireModified();
        });
        prefixCol.setPrefWidth(240);

        TableColumn<InjectionRow, String> langCol = new TableColumn<>("Language ID");
        langCol.setCellValueFactory(data -> data.getValue().languageIdProperty());
        langCol.setCellFactory(TextFieldTableCell.forTableColumn());
        langCol.setOnEditCommit(e -> {
            e.getRowValue().setLanguageId(e.getNewValue());
            fireModified();
        });
        langCol.prefWidthProperty().bind(table.widthProperty().subtract(260));

        table.getColumns().addAll(prefixCol, langCol);

        tableItems = FXCollections.observableArrayList();
        for (ScalaInterpolatedStringInjection inj : current.getInjections()) {
            tableItems.add(new InjectionRow(inj.getPrefix(), inj.getLanguageId()));
        }
        table.setItems(tableItems);

        tableBox.getChildren().addAll(toolbar, table);

        getChildren().addAll(superClassRow, trailingRow, injectionHeader, tableBox);
    }

    private void addRow() {
        InjectionRow newRow = new InjectionRow("prefix", "Language");
        tableItems.add(newRow);
        table.getSelectionModel().select(newRow);
        fireModified();
    }

    private void removeSelectedRow() {
        InjectionRow selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            tableItems.remove(selected);
            fireModified();
        }
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        return l;
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-padding: 4 8;");
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4;");
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

    public ScalaMiscSettings getCurrentUiSettings() {
        List<ScalaInterpolatedStringInjection> list = new ArrayList<>();
        for (InjectionRow row : tableItems) {
            list.add(new ScalaInterpolatedStringInjection(row.getPrefix(), row.getLanguageId()));
        }
        return new ScalaMiscSettings(
                scalaTestSuperClassField.getText(),
                trailingCommasComboBox.getValue(),
                list
        );
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentUiSettings());
    }

    public void apply() {
        manager.setMiscSettings(getCurrentUiSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        ScalaMiscSettings saved = manager.getMiscSettings();
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
        applySettingsToUi(new ScalaMiscSettings());
        fireModified();
    }

    private void applySettingsToUi(ScalaMiscSettings s) {
        if (s == null) return;
        scalaTestSuperClassField.setText(s.getScalaTestDefaultSuperClass());
        if (trailingCommasComboBox != null) {
            trailingCommasComboBox.setValue(s.getTrailingCommas());
        }
        tableItems.clear();
        for (ScalaInterpolatedStringInjection inj : s.getInjections()) {
            tableItems.add(new InjectionRow(inj.getPrefix(), inj.getLanguageId()));
        }
    }

    // Direct UI accessors for tests
    public TextField getScalaTestSuperClassField() {
        return scalaTestSuperClassField;
    }

    public ComboBox<String> getTrailingCommasComboBox() {
        return trailingCommasComboBox;
    }

    public TableView<InjectionRow> getTable() {
        return table;
    }

    public ObservableList<InjectionRow> getTableItems() {
        return tableItems;
    }

    public Button getAddBtn() {
        return addBtn;
    }

    public Button getRemoveBtn() {
        return removeBtn;
    }
}
