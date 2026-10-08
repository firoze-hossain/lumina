package dev.lumina.ui;

import dev.lumina.todo.TodoFilter;
import dev.lumina.todo.TodoPattern;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Modal dialog for creating or editing a TODO filter in Lumina IDE.
 * Faithfully matches reference IDE design and controls (Image 2: media_1791419889481_d2007814.png).
 * Features Name field with blue focus border, "Patterns" section with titled separator line,
 * dark pattern checklist table with hidden header and selection highlight, help button, and OK/Cancel actions.
 */
public class TodoFilterDialog {

    private final Stage stage;
    private final TextField nameField = new TextField();
    private final TableView<PatternSelectionModel> tableView = new TableView<>();
    private final ObservableList<PatternSelectionModel> tableData = FXCollections.observableArrayList();
    private final Label errorLabel = new Label();

    private final Button helpBtn = new Button("?");
    private final Button okBtn = new Button("OK");
    private final Button cancelBtn = new Button("Cancel");

    private TodoFilter result = null;
    private final String filterId;

    public static class PatternSelectionModel {
        private final SimpleBooleanProperty selected;
        private final TodoPattern pattern;

        public PatternSelectionModel(TodoPattern pattern, boolean selected) {
            this.pattern = pattern;
            this.selected = new SimpleBooleanProperty(selected);
        }

        public SimpleBooleanProperty selectedProperty() {
            return selected;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public TodoPattern getPattern() {
            return pattern;
        }
    }

    public TodoFilterDialog(Window owner, TodoFilter initial, List<TodoPattern> availablePatterns) {
        stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(initial == null ? "Add Filter" : "Edit Filter");
        stage.setResizable(false);

        Set<String> selectedPatternIds = new HashSet<>();
        if (initial != null) {
            this.filterId = initial.getId();
            nameField.setText(initial.getName());
            selectedPatternIds.addAll(initial.getPatternIds());
        } else {
            this.filterId = UUID.randomUUID().toString();
        }

        if (availablePatterns != null) {
            for (TodoPattern p : availablePatterns) {
                tableData.add(new PatternSelectionModel(p, selectedPatternIds.contains(p.getId()) || selectedPatternIds.contains(p.getPattern())));
            }
        }

        buildUi();
        setupListeners();
    }

    private void buildUi() {
        VBox root = new VBox(10);
        root.setStyle("-fx-background-color: #2B2D30; -fx-padding: 14 18 14 18;");
        root.setPrefWidth(350);

        // --- 1. Name Row ---
        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        nameLabel.setPrefWidth(55);

        nameField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 6 4 6; -fx-font-size: 12px;");
        HBox.setHgrow(nameField, Priority.ALWAYS);

        HBox nameRow = new HBox(8, nameLabel, nameField);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        errorLabel.setStyle("-fx-text-fill: #FA5252; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // --- 2. Patterns Section Header with Separator line ---
        Label patternsHeaderLabel = new Label("Patterns");
        patternsHeaderLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #393B40; -fx-border-color: #393B40;");
        HBox.setHgrow(separator, Priority.ALWAYS);

        HBox sectionHeaderBox = new HBox(8, patternsHeaderLabel, separator);
        sectionHeaderBox.setAlignment(Pos.CENTER_LEFT);
        sectionHeaderBox.setPadding(new Insets(4, 0, 0, 0));

        // --- 3. Patterns Checklist Table ---
        tableView.setPrefHeight(125);
        tableView.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-control-inner-background-alt: #1E1F22; " +
                "-fx-background: #1E1F22; " +
                "-fx-table-cell-border-color: transparent; " +
                "-fx-table-header-border-color: transparent; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4;"
        );
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setItems(tableData);

        TableColumn<PatternSelectionModel, Boolean> checkCol = new TableColumn<>();
        checkCol.setPrefWidth(34);
        checkCol.setMinWidth(34);
        checkCol.setMaxWidth(34);
        checkCol.setCellValueFactory(param -> param.getValue().selectedProperty());
        checkCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkCol));
        checkCol.setEditable(true);

        TableColumn<PatternSelectionModel, String> patternCol = new TableColumn<>();
        patternCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().getPattern().getPattern()));
        patternCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-alignment: CENTER-LEFT; -fx-background-color: transparent;");
                }
            }
        });

        tableView.getColumns().add(checkCol);
        tableView.getColumns().add(patternCol);
        tableView.setEditable(true);

        // Custom row styling with selection highlight matching reference image (#2E436E)
        tableView.setRowFactory(tv -> {
            TableRow<PatternSelectionModel> row = new TableRow<>() {
                @Override
                protected void updateItem(PatternSelectionModel item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent;");
                    } else if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF;");
                    } else {
                        setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
                    }
                }
            };
            row.selectedProperty().addListener((obs, wasSel, isSel) -> {
                if (!row.isEmpty()) {
                    if (isSel) {
                        row.setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF;");
                    } else {
                        row.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5;");
                    }
                }
            });
            return row;
        });

        // Hide table header completely to match Image 2
        tableView.widthProperty().addListener((obs, oldVal, newVal) -> {
            Pane header = (Pane) tableView.lookup("TableHeaderRow");
            if (header != null) {
                header.setVisible(false);
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
            }
        });

        // Select first item by default if available (as shown in Screenshot 2)
        if (!tableData.isEmpty()) {
            tableView.getSelectionModel().select(0);
        }

        // --- 4. Bottom Button Bar ---
        helpBtn.setTooltip(new Tooltip("Help"));
        helpBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #4E5157; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #8C919D; -fx-font-size: 11px; -fx-padding: 2 6 2 6; -fx-cursor: hand;");

        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 18; -fx-background-radius: 4; -fx-cursor: hand;");
        cancelBtn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-font-size: 12px; -fx-padding: 5 18; -fx-background-radius: 4; -fx-cursor: hand;");

        HBox buttonBar = new HBox(8);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(8, 0, 0, 0));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        buttonBar.getChildren().addAll(helpBtn, spacer, okBtn, cancelBtn);

        root.getChildren().addAll(
                nameRow,
                errorLabel,
                sectionHeaderBox,
                tableView,
                buttonBar
        );

        Scene scene = new Scene(root);
        scene.setFill(Color.web("#2B2D30"));

        // Load IDE dark theme stylesheet
        try {
            var css = getClass().getResource("/css/lumina-dark.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {}

        // Add targeted dark rules ensuring zero white borders or headers
        String darkDialogCss = """
            .table-view {
                -fx-background-color: #1E1F22;
                -fx-control-inner-background: #1E1F22;
                -fx-control-inner-background-alt: #1E1F22;
                -fx-background: #1E1F22;
            }
            .table-view .column-header-background,
            .table-view .table-header-row {
                -fx-max-height: 0;
                -fx-pref-height: 0;
                -fx-min-height: 0;
                visibility: hidden;
            }
            .table-view .table-row-cell {
                -fx-background-color: #1E1F22;
                -fx-border-color: transparent;
            }
            .table-view .table-row-cell:odd {
                -fx-background-color: #1E1F22;
            }
            .table-view .table-row-cell:selected {
                -fx-background-color: #2E436E;
            }
            .table-view .table-cell {
                -fx-background-color: transparent;
                -fx-text-fill: #DFE1E5;
            }
            .check-box .box {
                -fx-background-color: #2B2D30;
                -fx-border-color: #4E5157;
                -fx-border-radius: 3;
                -fx-background-radius: 3;
            }
            .check-box:selected .box {
                -fx-background-color: #3574F0;
                -fx-border-color: #3574F0;
            }
            .check-box:selected .mark {
                -fx-background-color: white;
            }
        """;
        try {
            scene.getStylesheets().add("data:text/css," + URLEncoder.encode(darkDialogCss, StandardCharsets.UTF_8).replace("+", "%20"));
        } catch (Exception ignored) {}

        stage.setScene(scene);
    }

    private void setupListeners() {
        okBtn.setOnAction(e -> handleOk());
        cancelBtn.setOnAction(e -> stage.close());

        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleOk();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });
    }

    private void handleOk() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Filter name cannot be empty");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }

        Set<String> chosenIds = new LinkedHashSet<>();
        for (PatternSelectionModel row : tableData) {
            if (row.isSelected()) {
                chosenIds.add(row.getPattern().getId());
            }
        }

        result = new TodoFilter(filterId, name, chosenIds);
        stage.close();
    }

    public TodoFilter showDialog() {
        stage.showAndWait();
        return result;
    }

    // Accessors for testing
    public TextField getNameField() {
        return nameField;
    }

    public TableView<PatternSelectionModel> getTableView() {
        return tableView;
    }

    public ObservableList<PatternSelectionModel> getTableData() {
        return tableData;
    }

    public Button getOkBtn() {
        return okBtn;
    }

    public Button getCancelBtn() {
        return cancelBtn;
    }

    public Button getHelpBtn() {
        return helpBtn;
    }
}
