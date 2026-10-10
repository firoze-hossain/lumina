package dev.lumina.ui;

import dev.lumina.scala.ScalaExtensionLibrary;
import dev.lumina.scala.ScalaExtensionsSettings;
import dev.lumina.scala.ScalaLanguageSettingsManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Languages & Frameworks > Scala > Extensions settings page in Lumina IDE.
 * Matches Image 2:
 *  - [x] Enable loading external extensions (?)
 *  - Section: Known extension libraries
 *    - Toolbar: +, -
 *    - Table / Placeholder: No known extension libraries
 *  - Section: Extensions in selected library
 *    - Placeholder: Select library from the list above
 */
public class SettingsLanguagesScalaExtensionsPage extends VBox {

    private final ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private CheckBox enableLoadingCheckBox;
    private Button addBtn;
    private Button removeBtn;
    private TableView<LibraryRow> libraryTable;
    private ObservableList<LibraryRow> libraryItems;
    private ListView<String> extensionsListView;
    private Label extensionsPlaceholder;

    private ScalaExtensionsSettings initialSettings;

    public static class LibraryRow {
        private final SimpleStringProperty name;
        private final SimpleStringProperty path;
        private final SimpleBooleanProperty enabled;
        private final List<String> extensions = new ArrayList<>();

        public LibraryRow(String name, String path, boolean enabled, List<String> extensions) {
            this.name = new SimpleStringProperty(name != null ? name : "");
            this.path = new SimpleStringProperty(path != null ? path : "");
            this.enabled = new SimpleBooleanProperty(enabled);
            if (extensions != null) {
                this.extensions.addAll(extensions);
            }
        }

        public String getName() {
            return name.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public String getPath() {
            return path.get();
        }

        public void setPath(String path) {
            this.path.set(path);
        }

        public SimpleStringProperty pathProperty() {
            return path;
        }

        public boolean isEnabled() {
            return enabled.get();
        }

        public void setEnabled(boolean enabled) {
            this.enabled.set(enabled);
        }

        public SimpleBooleanProperty enabledProperty() {
            return enabled;
        }

        public List<String> getExtensions() {
            return extensions;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LibraryRow that = (LibraryRow) o;
            return isEnabled() == that.isEnabled() &&
                    Objects.equals(getName(), that.getName()) &&
                    Objects.equals(getPath(), that.getPath()) &&
                    Objects.equals(extensions, that.extensions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getName(), getPath(), isEnabled(), extensions);
        }
    }

    public SettingsLanguagesScalaExtensionsPage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ScalaExtensionsSettings current = manager.getExtensionsSettings();

        // 1. Enable loading external extensions row with (?) icon
        HBox enableRow = new HBox(6);
        enableRow.setAlignment(Pos.CENTER_LEFT);

        enableLoadingCheckBox = new CheckBox("Enable loading external extensions");
        enableLoadingCheckBox.setSelected(current.isEnableLoadingExternalExtensions());
        styleCheckBox(enableLoadingCheckBox);
        enableLoadingCheckBox.selectedProperty().addListener((obs, oldV, newV) -> {
            updateControlsState();
            fireModified();
        });

        Label helpIcon = new Label("(?)");
        helpIcon.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px; -fx-cursor: hand;");
        helpIcon.setTooltip(new Tooltip("Enables loading third-party Scala plugin extensions from configured libraries"));

        enableRow.getChildren().addAll(enableLoadingCheckBox, helpIcon);

        // 2. Section 1: Known extension libraries
        HBox libHeader = createSectionHeader("Known extension libraries");

        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        addBtn = new Button("+");
        styleToolbarButton(addBtn);
        addBtn.setOnAction(e -> addLibraryRow());

        removeBtn = new Button("-");
        styleToolbarButton(removeBtn);
        removeBtn.setOnAction(e -> removeSelectedLibraryRow());

        toolbar.getChildren().addAll(addBtn, removeBtn);

        libraryTable = new TableView<>();
        libraryTable.setEditable(true);
        libraryTable.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4;");
        Label placeholder = new Label("No known extension libraries");
        placeholder.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");
        libraryTable.setPlaceholder(placeholder);
        libraryTable.setPrefHeight(180);

        TableColumn<LibraryRow, Boolean> enabledCol = new TableColumn<>("");
        enabledCol.setCellValueFactory(data -> data.getValue().enabledProperty());
        enabledCol.setCellFactory(CheckBoxTableCell.forTableColumn(enabledCol));
        enabledCol.setPrefWidth(35);
        enabledCol.setEditable(true);

        TableColumn<LibraryRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> {
            e.getRowValue().setName(e.getNewValue());
            fireModified();
        });
        nameCol.setPrefWidth(220);

        TableColumn<LibraryRow, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(data -> data.getValue().pathProperty());
        pathCol.setCellFactory(TextFieldTableCell.forTableColumn());
        pathCol.setOnEditCommit(e -> {
            e.getRowValue().setPath(e.getNewValue());
            fireModified();
        });
        pathCol.prefWidthProperty().bind(libraryTable.widthProperty().subtract(265));

        libraryTable.getColumns().addAll(enabledCol, nameCol, pathCol);

        libraryItems = FXCollections.observableArrayList();
        for (ScalaExtensionLibrary lib : current.getKnownLibraries()) {
            libraryItems.add(new LibraryRow(lib.getName(), lib.getPath(), lib.isEnabled(), lib.getExtensions()));
        }
        libraryTable.setItems(libraryItems);

        libraryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            updateSelectedLibraryExtensions(newV);
        });

        VBox libBox = new VBox(6, toolbar, libraryTable);

        // 3. Section 2: Extensions in selected library
        HBox extHeader = createSectionHeader("Extensions in selected library");

        extensionsPlaceholder = new Label("Select library from the list above");
        extensionsPlaceholder.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");

        extensionsListView = new ListView<>();
        extensionsListView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #43454A; -fx-border-radius: 4;");
        extensionsListView.setPlaceholder(extensionsPlaceholder);
        extensionsListView.setPrefHeight(140);

        getChildren().addAll(enableRow, libHeader, libBox, extHeader, extensionsListView);

        updateControlsState();
    }

    private void updateSelectedLibraryExtensions(LibraryRow selected) {
        if (selected == null) {
            extensionsPlaceholder.setText("Select library from the list above");
            extensionsListView.getItems().clear();
        } else {
            extensionsPlaceholder.setText("No extensions found in this library");
            extensionsListView.getItems().setAll(selected.getExtensions());
        }
    }

    private void updateControlsState() {
        boolean enabled = enableLoadingCheckBox.isSelected();
        addBtn.setDisable(!enabled);
        removeBtn.setDisable(!enabled);
        libraryTable.setDisable(!enabled);
        libraryTable.setOpacity(enabled ? 1.0 : 0.6);
        extensionsListView.setDisable(!enabled);
        extensionsListView.setOpacity(enabled ? 1.0 : 0.6);
    }

    private void addLibraryRow() {
        LibraryRow newRow = new LibraryRow("Library " + (libraryItems.size() + 1), "/path/to/library.jar", true, new ArrayList<>());
        libraryItems.add(newRow);
        libraryTable.getSelectionModel().select(newRow);
        fireModified();
    }

    private void removeSelectedLibraryRow() {
        LibraryRow selected = libraryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            libraryItems.remove(selected);
            fireModified();
        }
    }

    private HBox createSectionHeader(String title) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 0, 2, 0));

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #393B40; -fx-opacity: 0.5;");
        HBox.setHgrow(sep, Priority.ALWAYS);

        header.getChildren().addAll(label, sep);
        return header;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
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

    public ScalaExtensionsSettings getCurrentUiSettings() {
        List<ScalaExtensionLibrary> list = new ArrayList<>();
        for (LibraryRow row : libraryItems) {
            list.add(new ScalaExtensionLibrary(row.getName(), row.getPath(), row.isEnabled(), row.getExtensions()));
        }
        return new ScalaExtensionsSettings(enableLoadingCheckBox.isSelected(), list);
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentUiSettings());
    }

    public void apply() {
        manager.setExtensionsSettings(getCurrentUiSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        ScalaExtensionsSettings saved = manager.getExtensionsSettings();
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
        applySettingsToUi(new ScalaExtensionsSettings());
        fireModified();
    }

    private void applySettingsToUi(ScalaExtensionsSettings s) {
        if (s == null) return;
        enableLoadingCheckBox.setSelected(s.isEnableLoadingExternalExtensions());
        libraryItems.clear();
        for (ScalaExtensionLibrary lib : s.getKnownLibraries()) {
            libraryItems.add(new LibraryRow(lib.getName(), lib.getPath(), lib.isEnabled(), lib.getExtensions()));
        }
        updateControlsState();
        updateSelectedLibraryExtensions(null);
    }

    // Direct UI accessors for tests
    public CheckBox getEnableLoadingCheckBox() {
        return enableLoadingCheckBox;
    }

    public Button getAddBtn() {
        return addBtn;
    }

    public Button getRemoveBtn() {
        return removeBtn;
    }

    public TableView<LibraryRow> getLibraryTable() {
        return libraryTable;
    }

    public ObservableList<LibraryRow> getLibraryItems() {
        return libraryItems;
    }

    public ListView<String> getExtensionsListView() {
        return extensionsListView;
    }
}
