// SettingsFileEncodingsPage.java
package dev.lumina.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Editor > File Encodings settings page.
 * Complete implementation matching the screenshot.
 */
public class SettingsFileEncodingsPage extends VBox {

    private final TableView<EncodingEntry> encodingTable = new TableView<>();
    private final ObservableList<EncodingEntry> encodingData = FXCollections.observableArrayList();

    public SettingsFileEncodingsPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(12, 20, 20, 20));
        setSpacing(14);

        // ============================================================
        // Global Encoding
        // ============================================================
        HBox globalRow = new HBox(12);
        globalRow.setAlignment(Pos.CENTER_LEFT);

        Label globalLabel = new Label("Global Encoding:");
        globalLabel.getStyleClass().add("settings-label");

        ComboBox<String> globalCombo = new ComboBox<>();
        globalCombo.getItems().addAll("UTF-8", "UTF-16", "ISO-8859-1", "Windows-1252", "US-ASCII");
        globalCombo.getSelectionModel().select("UTF-8");
        globalCombo.getStyleClass().add("settings-combo");
        globalCombo.setPrefWidth(200);

        globalRow.getChildren().addAll(globalLabel, globalCombo);

        // ============================================================
        // Project Encoding
        // ============================================================
        HBox projectRow = new HBox(12);
        projectRow.setAlignment(Pos.CENTER_LEFT);

        Label projectLabel = new Label("Project Encoding:");
        projectLabel.getStyleClass().add("settings-label");

        ComboBox<String> projectCombo = new ComboBox<>();
        projectCombo.getItems().addAll("<System Default: UTF-8>", "UTF-8", "UTF-16", "ISO-8859-1", "Windows-1252");
        projectCombo.getSelectionModel().selectFirst();
        projectCombo.getStyleClass().add("settings-combo");
        projectCombo.setPrefWidth(200);

        projectRow.getChildren().addAll(projectLabel, projectCombo);

        // ============================================================
        // Path/Encoding Table
        // ============================================================
        Label tableLabel = new Label("Path / Encoding");
        tableLabel.getStyleClass().add("settings-label");
        tableLabel.setPadding(new Insets(8, 0, 4, 0));

        encodingTable.getStyleClass().add("settings-list");
        encodingTable.setPrefHeight(160);

        TableColumn<EncodingEntry, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(cell -> cell.getValue().pathProperty());
        pathCol.setPrefWidth(300);

        TableColumn<EncodingEntry, String> encodingCol = new TableColumn<>("Encoding");
        encodingCol.setCellValueFactory(cell -> cell.getValue().encodingProperty());
        encodingCol.setPrefWidth(150);

        encodingTable.getColumns().addAll(pathCol, encodingCol);

        // Sample data
        encodingData.add(new EncodingEntry("Font", "UTF-8"));
        encodingData.add(new EncodingEntry("Color Scheme", "UTF-8"));
        encodingData.add(new EncodingEntry("Code Style", "Encoding"));
        encodingData.add(new EncodingEntry("Inspections", "Encoding"));
        encodingData.add(new EncodingEntry("File and Code Templates", "Encoding"));
        encodingData.add(new EncodingEntry("Live Templates", "Encoding"));
        encodingData.add(new EncodingEntry("File Types", "Encoding"));
        encodingData.add(new EncodingEntry("Copyright", "Encoding"));
        encodingData.add(new EncodingEntry("Inlay Hints", "Encoding"));
        encodingData.add(new EncodingEntry("Duplicates", "Encoding"));
        encodingData.add(new EncodingEntry("Emmet", "Encoding"));
        encodingData.add(new EncodingEntry("Intentions", "Encoding"));
        encodingData.add(new EncodingEntry("Language Injections", "Encoding"));
        encodingData.add(new EncodingEntry("Natural Languages", "Encoding"));
        encodingData.add(new EncodingEntry("Reader Mode", "Encoding"));
        encodingData.add(new EncodingEntry("TextMate Bundles", "Encoding"));
        encodingData.add(new EncodingEntry("TODO", "Encoding"));
        encodingData.add(new EncodingEntry("Plugins", "Encoding"));
        encodingData.add(new EncodingEntry("Version Control", "Encoding"));

        encodingTable.setItems(encodingData);

        // Buttons for table
        HBox tableButtons = new HBox(8);
        tableButtons.setAlignment(Pos.CENTER_LEFT);
        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("property-button");
        addBtn.setOnAction(e -> {
            // Show dialog to add new encoding
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add Encoding");
            dialog.setHeaderText("Enter path and encoding");
            dialog.setContentText("Path:");
            dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
            dialog.showAndWait().ifPresent(path -> {
                if (!path.trim().isEmpty()) {
                    // Show encoding selection dialog
                    Dialog<String> encodingDialog = new Dialog<>();
                    encodingDialog.setTitle("Select Encoding");
                    encodingDialog.setHeaderText("Select encoding for: " + path);
                    ComboBox<String> encCombo = new ComboBox<>();
                    encCombo.getItems().addAll("UTF-8", "UTF-16", "ISO-8859-1", "Windows-1252", "US-ASCII");
                    encCombo.getSelectionModel().selectFirst();
                    encodingDialog.getDialogPane().setContent(encCombo);
                    encodingDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                    encodingDialog.getDialogPane().getStylesheets().add(
                        getClass().getResource("/css/lumina-dark.css").toExternalForm());
                    encodingDialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(btn -> {
                        String enc = encCombo.getValue();
                        encodingData.add(new EncodingEntry(path.trim(), enc));
                    });
                }
            });
        });

        Button removeBtn = new Button("-");
        removeBtn.getStyleClass().add("property-button");
        removeBtn.setOnAction(e -> {
            EncodingEntry selected = encodingTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                encodingData.remove(selected);
            }
        });

        tableButtons.getChildren().addAll(addBtn, removeBtn);

        // Description below table
        Label tableDesc = new Label("Add the path to a file or directory and select the encoding IntelliJ IDEA should use.\n" +
            "Files and directories inherit the encoding from the parent directory or from the Project Encoding.\n" +
            "Built-in file encodings in JSP, HTML, and XML files override these settings.");
        tableDesc.getStyleClass().add("settings-hint");
        tableDesc.setWrapText(true);

        // ============================================================
        // Default encoding for properties files
        // ============================================================
        HBox propertiesRow = new HBox(12);
        propertiesRow.setAlignment(Pos.CENTER_LEFT);
        propertiesRow.setPadding(new Insets(8, 0, 4, 0));

        Label propertiesLabel = new Label("Default encoding for properties files:");
        propertiesLabel.getStyleClass().add("settings-label");

        ComboBox<String> propertiesCombo = new ComboBox<>();
        propertiesCombo.getItems().addAll("<Properties Default: ISO-8859-1>", "UTF-8", "UTF-16", "ISO-8859-1", "Windows-1252");
        propertiesCombo.getSelectionModel().selectFirst();
        propertiesCombo.getStyleClass().add("settings-combo");
        propertiesCombo.setPrefWidth(250);

        propertiesRow.getChildren().addAll(propertiesLabel, propertiesCombo);

        // ============================================================
        // Transparent native-to-ascii conversion
        // ============================================================
        CheckBox nativeToAscii = new CheckBox("Transparent native-to-ascii conversion");
        nativeToAscii.setSelected(false);
        nativeToAscii.getStyleClass().add("settings-check");
        nativeToAscii.setPadding(new Insets(4, 0, 4, 0));

        // ============================================================
        // Create UTF-8 files
        // ============================================================
        HBox utf8Row = new HBox(12);
        utf8Row.setAlignment(Pos.CENTER_LEFT);
        utf8Row.setPadding(new Insets(4, 0, 4, 0));

        Label utf8Label = new Label("Create UTF-8 files:");
        utf8Label.getStyleClass().add("settings-label");

        ComboBox<String> utf8Combo = new ComboBox<>();
        utf8Combo.getItems().addAll("with NO BOM", "with BOM");
        utf8Combo.getSelectionModel().selectFirst();
        utf8Combo.getStyleClass().add("settings-combo");
        utf8Combo.setPrefWidth(150);

        utf8Row.getChildren().addAll(utf8Label, utf8Combo);

        // ============================================================
        // Footer text
        // ============================================================
        Label footerText = new Label("IDEA will NOT add UTF-8 BOM to every created file in UTF-8 encoding");
        footerText.getStyleClass().add("settings-hint");
        footerText.setPadding(new Insets(4, 0, 0, 0));

        // ============================================================
        // Assemble all sections
        // ============================================================
        getChildren().addAll(
            globalRow,
            projectRow,
            tableLabel,
            encodingTable,
            tableButtons,
            tableDesc,
            propertiesRow,
            nativeToAscii,
            utf8Row,
            footerText
        );
    }

    /**
     * Entry for the encoding table.
     */
    public static class EncodingEntry {
        private final SimpleStringProperty path;
        private final SimpleStringProperty encoding;

        public EncodingEntry(String path, String encoding) {
            this.path = new SimpleStringProperty(path);
            this.encoding = new SimpleStringProperty(encoding);
        }

        public SimpleStringProperty pathProperty() { return path; }
        public SimpleStringProperty encodingProperty() { return encoding; }

        public String getPath() { return path.get(); }
        public String getEncoding() { return encoding.get(); }
        public void setPath(String path) { this.path.set(path); }
        public void setEncoding(String encoding) { this.encoding.set(encoding); }
    }
}