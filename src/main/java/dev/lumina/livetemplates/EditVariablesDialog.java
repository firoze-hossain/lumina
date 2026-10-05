package dev.lumina.livetemplates;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * IntelliJ IDEA-style modal dialog for editing Live Template variables:
 * Name, Expression, Default value, and Skip if defined.
 */
public class EditVariablesDialog {

    public static class VariableRow {
        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleStringProperty expression = new SimpleStringProperty();
        private final SimpleStringProperty defaultValue = new SimpleStringProperty();
        private final SimpleBooleanProperty skipIfDefined = new SimpleBooleanProperty();

        public VariableRow(LiveTemplateVariable v) {
            this.name.set(v.getName());
            this.expression.set(v.getExpression());
            this.defaultValue.set(v.getDefaultValue());
            this.skipIfDefined.set(v.isSkipIfDefined());
        }

        public LiveTemplateVariable toVariable() {
            return new LiveTemplateVariable(
                    name.get(), expression.get(), defaultValue.get(), skipIfDefined.get()
            );
        }
    }

    public static boolean show(Window owner, LiveTemplate template) {
        if (template == null) return false;

        // Auto-extract any new variables in template text first
        LiveTemplateManager.extractVariables(template);

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Edit Template Variables");

        VBox root = new VBox(12);
        root.setStyle("-fx-background-color: #1E1F22; -fx-padding: 16;");

        Label title = new Label("Variables for '" + template.getAbbreviation() + "':");
        title.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        TableView<VariableRow> table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4;");
        VBox.setVgrow(table, Priority.ALWAYS);

        ObservableList<VariableRow> data = FXCollections.observableArrayList();
        for (LiveTemplateVariable v : template.getVariables()) {
            data.add(new VariableRow(v));
        }
        table.setItems(data);

        // Column: Name
        TableColumn<VariableRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> c.getValue().name);
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setPrefWidth(120);

        // Column: Expression
        TableColumn<VariableRow, String> exprCol = new TableColumn<>("Expression");
        exprCol.setCellValueFactory(c -> c.getValue().expression);
        exprCol.setCellFactory(TextFieldTableCell.forTableColumn());
        exprCol.setPrefWidth(180);

        // Column: Default value
        TableColumn<VariableRow, String> defaultCol = new TableColumn<>("Default value");
        defaultCol.setCellValueFactory(c -> c.getValue().defaultValue);
        defaultCol.setCellFactory(TextFieldTableCell.forTableColumn());
        defaultCol.setPrefWidth(140);

        // Column: Skip if defined
        TableColumn<VariableRow, Boolean> skipCol = new TableColumn<>("Skip if defined");
        skipCol.setCellValueFactory(c -> c.getValue().skipIfDefined);
        skipCol.setCellFactory(CheckBoxTableCell.forTableColumn(skipCol));
        skipCol.setPrefWidth(110);

        table.getColumns().addAll(nameCol, exprCol, defaultCol, skipCol);

        // Toolbar for Add, Remove, Up, Down
        HBox toolbar = new HBox(6);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = createIconBtn("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z", "Add Variable");
        addBtn.setOnAction(e -> {
            LiveTemplateVariable newV = new LiveTemplateVariable("VAR_" + (data.size() + 1), "", "", false);
            VariableRow row = new VariableRow(newV);
            data.add(row);
            table.getSelectionModel().select(row);
        });

        Button removeBtn = createIconBtn("M19 13H5v-2h14v2z", "Remove Variable");
        removeBtn.setOnAction(e -> {
            VariableRow sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                data.remove(sel);
            }
        });

        Button upBtn = createIconBtn("M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z", "Move Up");
        upBtn.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                VariableRow item = data.remove(idx);
                data.add(idx - 1, item);
                table.getSelectionModel().select(idx - 1);
            }
        });

        Button downBtn = createIconBtn("M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z", "Move Down");
        downBtn.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < data.size() - 1) {
                VariableRow item = data.remove(idx);
                data.add(idx + 1, item);
                table.getSelectionModel().select(idx + 1);
            }
        });

        Region toolSpacer = new Region();
        HBox.setHgrow(toolSpacer, Priority.ALWAYS);

        Label hint = new Label("Standard expressions: suggestVariableName(), className(), clipboard(), date()");
        hint.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");

        toolbar.getChildren().addAll(addBtn, removeBtn, upBtn, downBtn, toolSpacer, hint);

        // Footer buttons (OK, Cancel)
        final boolean[] result = {false};
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 5 14 5 14;");
        cancelBtn.setOnAction(e -> stage.close());

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 4; -fx-padding: 5 18 5 18;");
        okBtn.setOnAction(e -> {
            List<LiveTemplateVariable> updated = new ArrayList<>();
            for (VariableRow row : data) {
                updated.add(row.toVariable());
            }
            template.setVariables(updated);
            result[0] = true;
            stage.close();
        });

        footer.getChildren().addAll(cancelBtn, okBtn);

        root.getChildren().addAll(title, toolbar, table, footer);

        Scene scene = new Scene(root, 620, 380);
        stage.setScene(scene);
        stage.showAndWait();

        return result[0];
    }

    private static Button createIconBtn(String svgPath, String tooltip) {
        Button btn = new Button();
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
        SVGPath path = new SVGPath();
        path.setContent(svgPath);
        path.setFill(Color.web("#B9BECF"));
        path.setScaleX(0.7);
        path.setScaleY(0.7);
        btn.setGraphic(path);
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }
}
