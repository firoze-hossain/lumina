// SettingsDataEditorPage.java
package dev.lumina.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Data Editor and Viewer settings page.
 */
public class SettingsDataEditorPage extends VBox {

    public SettingsDataEditorPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);

        // Use custom font
        CheckBox customFont = new CheckBox("Use custom font:");
        customFont.setSelected(true);
        customFont.getStyleClass().add("settings-check");

        HBox fontRow = new HBox(8);
        fontRow.setAlignment(Pos.CENTER_LEFT);
        ComboBox<String> fontCombo = new ComboBox<>();
        fontCombo.getItems().addAll("JetBrains Mono", "Consolas", "Menlo", "Courier New");
        fontCombo.getSelectionModel().selectFirst();
        fontCombo.getStyleClass().add("settings-combo");
        fontCombo.setPrefWidth(150);

        Label sizeLabel = new Label("Size:");
        sizeLabel.getStyleClass().add("settings-label");
        Spinner<Double> sizeSpinner = new Spinner<>(8.0, 24.0, 13.0, 0.5);
        sizeSpinner.setPrefWidth(70);
        sizeSpinner.getStyleClass().add("settings-spinner");

        Label lineHeightLabel = new Label("Line height:");
        lineHeightLabel.getStyleClass().add("settings-label");
        Spinner<Double> lineHeightSpinner = new Spinner<>(1.0, 2.0, 1.2, 0.1);
        lineHeightSpinner.setPrefWidth(70);
        lineHeightSpinner.getStyleClass().add("settings-spinner");

        fontRow.getChildren().addAll(fontCombo, sizeLabel, sizeSpinner, lineHeightLabel, lineHeightSpinner);

        VBox fontBox = new VBox(4, customFont, fontRow);
        fontBox.setPadding(new Insets(0, 0, 8, 0));

        // Alternate row colors
        CheckBox alternateRows = new CheckBox("Alternate row colors");
        alternateRows.getStyleClass().add("settings-check");

        // Show boolean values as
        HBox booleanRow = new HBox(8);
        booleanRow.setAlignment(Pos.CENTER_LEFT);
        Label booleanLabel = new Label("Show boolean values as:");
        booleanLabel.getStyleClass().add("settings-label");
        ComboBox<String> booleanCombo = new ComboBox<>();
        booleanCombo.getItems().addAll("Text", "Checkbox", "Dropdown");
        booleanCombo.getSelectionModel().selectFirst();
        booleanCombo.getStyleClass().add("settings-combo");
        booleanRow.getChildren().addAll(booleanLabel, booleanCombo);

        // Table preview
        TableView<DataRow> table = new TableView<>();
        table.getStyleClass().add("settings-list");
        table.setPrefHeight(200);

        String[] columns = {"customer_id", "first_name", "last_name", "active", "create_date"};
        for (String col : columns) {
            TableColumn<DataRow, String> tc = new TableColumn<>(col);
            tc.setCellValueFactory(cell -> cell.getValue().getProperty(col));
            tc.setPrefWidth(100);
            table.getColumns().add(tc);
        }

        // Sample data
        String[][] data = {
            {"1", "MARY", "SMITH", "true", "2006-02-14"},
            {"2", "PATRICIA", "JOHNSON", "true", "2006-02-14"},
            {"3", "LINDA", "WILLIAMS", "true", "2006-02-14"},
            {"4", "BARBARA", "JONES", "false", "2006-02-14"},
            {"5", "ELIZABETH", "BROWN", "false", "2006-02-14"},
            {"6", "JENNIFER", "DAVIS", "true", "2006-02-14"},
            {"7", "MARIA", "MILLER", "false", "2006-02-14"},
            {"8", "SUSAN", "WILSON", "true", "2006-02-14"},
            {"9", "MARGARET", "MOORE", "true", "2006-02-14"},
            {"10", "DOROTHY", "TAYLOR", "true", "2006-02-14"},
            {"11", "LISA", "ANDERSON", "true", "2006-02-14"},
            {"12", "NANCY", "THOMAS", "true", "2006-02-14"},
            {"13", "KAREN", "JACKSON", "false", "2006-02-14"},
            {"14", "DETTY", "WHITE", "true", "2006-02-14"}
        };

        for (String[] row : data) {
            DataRow dr = new DataRow();
            dr.setProperty("customer_id", row[0]);
            dr.setProperty("first_name", row[1]);
            dr.setProperty("last_name", row[2]);
            dr.setProperty("active", row[3]);
            dr.setProperty("create_date", row[4]);
            table.getItems().add(dr);
        }

        VBox content = new VBox(12, fontBox, alternateRows, booleanRow, table);
        getChildren().addAll(content);
    }

    private static class DataRow {
        private final java.util.Map<String, SimpleStringProperty> props = new java.util.HashMap<>();

        public void setProperty(String key, String value) {
            props.put(key, new SimpleStringProperty(value));
        }

        public SimpleStringProperty getProperty(String key) {
            return props.getOrDefault(key, new SimpleStringProperty(""));
        }
    }
}