package dev.lumina.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.*;
import java.util.function.Consumer;

/**
 * Modal dialog for managing User and System Environment Variables matching IntelliJ IDEA.
 */
public class EnvironmentVariablesDialog {

    public static class EnvVar {
        private final StringProperty name;
        private final StringProperty value;

        public EnvVar(String name, String value) {
            this.name = new SimpleStringProperty(name != null ? name : "");
            this.value = new SimpleStringProperty(value != null ? value : "");
        }

        public String getName() {
            return name.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public StringProperty nameProperty() {
            return name;
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String value) {
            this.value.set(value);
        }

        public StringProperty valueProperty() {
            return value;
        }
    }

    /**
     * Custom TableCell supporting inline editing matching IntelliJ IDEA:
     * - Double-click or single-click when cell is already focused starts editing.
     * - Pressing Enter or F2 starts editing.
     * - Escape cancels edit.
     * - Enter commits edit.
     * - Losing focus commits edit (prevents losing typed changes).
     */
    public static class EnvTableCell extends TableCell<EnvVar, String> {
        private TextField textField;
        private final boolean editable;

        public EnvTableCell(boolean editable) {
            this.editable = editable;
            if (editable) {
                setOnMouseClicked(evt -> {
                    if (evt.getClickCount() == 2) {
                        startEdit();
                    } else if (evt.getClickCount() == 1 && isFocused()) {
                        startEdit();
                    }
                });
            }
        }

        @Override
        public void startEdit() {
            if (!editable || !getTableView().isEditable() || !getTableColumn().isEditable()) {
                return;
            }
            super.startEdit();
            if (textField == null) {
                createTextField();
            }
            textField.setText(getItem() != null ? getItem() : "");
            setText(null);
            setGraphic(textField);
            textField.selectAll();
            textField.requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() != null ? getItem() : "");
            setGraphic(null);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(item != null ? item : "");
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(item != null ? item : "");
                    setGraphic(null);
                    if (!editable) {
                        setTextFill(Color.web("#8C919D"));
                    } else {
                        setTextFill(Color.web("#DFE1E5"));
                    }
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getItem() != null ? getItem() : "");
            textField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; "
                    + "-fx-border-color: #3574F0; -fx-border-width: 1.5px; -fx-border-radius: 2px; "
                    + "-fx-padding: 2 6 2 6; -fx-font-size: 12px;");
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

            textField.setOnAction(evt -> commitEdit(textField.getText()));

            textField.setOnKeyPressed(t -> {
                if (t.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                    t.consume();
                }
            });

            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused && isEditing()) {
                    commitEdit(textField.getText());
                }
            });
        }
    }

    private final Stage dialog = new Stage();
    private final ObservableList<EnvVar> userVariables = FXCollections.observableArrayList();
    private final TableView<EnvVar> userTable = new TableView<>(userVariables);

    private final ObservableList<EnvVar> systemVariables = FXCollections.observableArrayList();
    private final TableView<EnvVar> systemTable = new TableView<>(systemVariables);
    private final Map<String, String> initialSystemValues = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public EnvironmentVariablesDialog(Window owner, String initialValue, Consumer<String> onSave) {
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Environment Variables");

        // Parse initial user variables: "KEY1=val1;KEY2=val2"
        if (initialValue != null && !initialValue.isBlank()) {
            for (String part : initialValue.split("[;\\r\\n]+")) {
                int eq = part.indexOf('=');
                if (eq > 0) {
                    userVariables.add(new EnvVar(part.substring(0, eq).trim(), part.substring(eq + 1).trim()));
                } else if (!part.isBlank()) {
                    userVariables.add(new EnvVar(part.trim(), ""));
                }
            }
        }

        // Load system environment variables sorted
        loadSystemVariables();

        // 1. User variables section
        Label userLabel = new Label("User environment variables:");
        userLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 12px;");

        Button addBtn = createToolbarButton("+", "Add environment variable");
        Button removeBtn = createToolbarButton("\u2212", "Remove environment variable");
        Button copyUserBtn = createIconButton(createCopyIcon(), "Copy");
        Button pasteUserBtn = createIconButton(createPasteIcon(), "Paste");

        addBtn.setOnAction(e -> {
            EnvVar newVar = new EnvVar("", "");
            userVariables.add(newVar);
            int idx = userVariables.size() - 1;
            userTable.getSelectionModel().select(idx);
            userTable.scrollTo(idx);
            userTable.edit(idx, userTable.getColumns().get(0));
        });

        removeBtn.setOnAction(e -> {
            EnvVar selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                userVariables.remove(selected);
            }
        });

        copyUserBtn.setOnAction(e -> copyToClipboard(userTable));
        pasteUserBtn.setOnAction(e -> pasteFromClipboard(userVariables));

        HBox userToolbar = new HBox(4, addBtn, removeBtn, copyUserBtn, pasteUserBtn);
        userToolbar.setAlignment(Pos.CENTER_LEFT);

        setupTable(userTable, true);
        userTable.setPrefHeight(160);
        Label userPlaceholder = new Label("No variables");
        userPlaceholder.setStyle("-fx-text-fill: #6C7387; -fx-font-size: 12px;");
        userTable.setPlaceholder(userPlaceholder);

        VBox userSection = new VBox(6, userLabel, userToolbar, userTable);
        VBox.setVgrow(userTable, Priority.ALWAYS);
        VBox.setVgrow(userSection, Priority.ALWAYS);

        // 2. System variables section (Clean bold header matching IntelliJ, NO checkbox)
        Label sysLabel = new Label("System environment variables:");
        sysLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 12px;");

        Button copySysBtn = createIconButton(createCopyIcon(), "Copy");
        Button pasteSysBtn = createIconButton(createPasteIcon(), "Paste to user variables");
        Button revertSysBtn = createIconButton(createRevertIcon(), "Revert system variables");

        copySysBtn.setOnAction(e -> copyToClipboard(systemTable));
        pasteSysBtn.setOnAction(e -> pasteFromClipboard(userVariables));
        revertSysBtn.setOnAction(e -> loadSystemVariables());

        HBox sysToolbar = new HBox(4, copySysBtn, pasteSysBtn, revertSysBtn);
        sysToolbar.setAlignment(Pos.CENTER_LEFT);

        setupTable(systemTable, false);
        systemTable.setPrefHeight(180);
        Label sysPlaceholder = new Label("No variables");
        sysPlaceholder.setStyle("-fx-text-fill: #6C7387; -fx-font-size: 12px;");
        systemTable.setPlaceholder(sysPlaceholder);

        VBox sysSection = new VBox(6, sysLabel, sysToolbar, systemTable);
        VBox.setVgrow(systemTable, Priority.ALWAYS);
        VBox.setVgrow(sysSection, Priority.ALWAYS);

        VBox centerBox = new VBox(14, userSection, sysSection);
        centerBox.setPadding(new Insets(14, 16, 12, 16));

        // 3. Footer buttons
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().setAll("dialog-primary");
        okBtn.setPrefWidth(76);
        okBtn.setOnAction(e -> {
            commitActiveEdit(userTable);
            commitActiveEdit(systemTable);

            StringBuilder sb = new StringBuilder();
            // User variables
            for (EnvVar v : userVariables) {
                String nm = v.getName().trim();
                if (!nm.isEmpty()) {
                    if (sb.length() > 0) sb.append(";");
                    sb.append(nm).append("=").append(v.getValue().trim());
                }
            }
            // Include modified system variables as overrides
            for (EnvVar v : systemVariables) {
                String nm = v.getName().trim();
                String origVal = initialSystemValues.get(nm);
                if (origVal != null && !origVal.equals(v.getValue().trim())) {
                    boolean alreadyInUser = userVariables.stream()
                            .anyMatch(u -> u.getName().trim().equalsIgnoreCase(nm));
                    if (!alreadyInUser && !nm.isEmpty()) {
                        if (sb.length() > 0) sb.append(";");
                        sb.append(nm).append("=").append(v.getValue().trim());
                    }
                }
            }
            onSave.accept(sb.toString());
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().setAll("dialog-secondary");
        cancelBtn.setPrefWidth(76);
        cancelBtn.setOnAction(e -> dialog.close());

        HBox footer = new HBox(10, okBtn, cancelBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 16, 14, 16));
        footer.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        BorderPane root = new BorderPane();
        root.setCenter(centerBox);
        root.setBottom(footer);
        root.setStyle("-fx-background-color: #1E1F22;");

        Scene scene = new Scene(root, 620, 520);
        String css = getClass().getResource("/css/lumina-dark.css") != null
                ? getClass().getResource("/css/lumina-dark.css").toExternalForm() : null;
        if (css != null) scene.getStylesheets().add(css);
        dialog.setScene(scene);
    }

    public void show() {
        dialog.showAndWait();
    }

    public static Optional<String> show(Window owner, String initialValue) {
        String[] result = new String[1];
        EnvironmentVariablesDialog dialog = new EnvironmentVariablesDialog(owner, initialValue, val -> result[0] = val);
        dialog.show();
        return Optional.ofNullable(result[0]);
    }

    private void loadSystemVariables() {
        systemVariables.clear();
        initialSystemValues.clear();
        Map<String, String> env = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        env.putAll(System.getenv());
        for (Map.Entry<String, String> entry : env.entrySet()) {
            systemVariables.add(new EnvVar(entry.getKey(), entry.getValue()));
            initialSystemValues.put(entry.getKey(), entry.getValue());
        }
    }

    private void commitActiveEdit(TableView<EnvVar> table) {
        TablePosition<EnvVar, ?> editingCell = table.getEditingCell();
        if (editingCell != null) {
            table.edit(-1, null);
        }
    }

    private void setupTable(TableView<EnvVar> table, boolean isUserTable) {
        table.setEditable(true);
        table.getStyleClass().setAll("table-view", "env-variables-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<EnvVar, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setMinWidth(150);
        nameCol.setPrefWidth(240);
        nameCol.setEditable(isUserTable);
        nameCol.setCellFactory(col -> new EnvTableCell(isUserTable));
        if (isUserTable) {
            nameCol.setOnEditCommit(evt -> evt.getRowValue().setName(evt.getNewValue()));
        }

        TableColumn<EnvVar, String> valCol = new TableColumn<>("Value");
        valCol.setCellValueFactory(data -> data.getValue().valueProperty());
        valCol.setMinWidth(180);
        valCol.setPrefWidth(330);
        valCol.setEditable(true);
        valCol.setCellFactory(col -> new EnvTableCell(true));
        valCol.setOnEditCommit(evt -> evt.getRowValue().setValue(evt.getNewValue()));

        table.getColumns().setAll(nameCol, valCol);

        // Enter or F2 on focused cell begins editing
        table.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ENTER || evt.getCode() == KeyCode.F2) {
                TablePosition<EnvVar, ?> focusedCell = table.getFocusModel().getFocusedCell();
                if (focusedCell != null && focusedCell.getTableColumn() != null && focusedCell.getTableColumn().isEditable()) {
                    table.edit(focusedCell.getRow(), focusedCell.getTableColumn());
                    evt.consume();
                }
            }
        });
    }

    private Button createToolbarButton(String text, String tooltipText) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C919D; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 4 0 4; -fx-min-width: 22px; -fx-min-height: 22px;");
        b.setTooltip(new Tooltip(tooltipText));
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #2E3136; -fx-text-fill: #DFE1E5; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 4 0 4; -fx-min-width: 22px; -fx-min-height: 22px; -fx-background-radius: 3;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8C919D; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 4 0 4; -fx-min-width: 22px; -fx-min-height: 22px;"));
        return b;
    }

    private Button createIconButton(javafx.scene.Node graphic, String tooltipText) {
        Button b = new Button();
        b.setGraphic(graphic);
        b.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2 4 2 4; -fx-min-width: 22px; -fx-min-height: 22px;");
        b.setTooltip(new Tooltip(tooltipText));
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #2E3136; -fx-cursor: hand; -fx-padding: 2 4 2 4; -fx-min-width: 22px; -fx-min-height: 22px; -fx-background-radius: 3;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2 4 2 4; -fx-min-width: 22px; -fx-min-height: 22px;"));
        return b;
    }

    private void copyToClipboard(TableView<EnvVar> table) {
        TablePosition<EnvVar, ?> focusedCell = table.getFocusModel().getFocusedCell();
        EnvVar sel = table.getSelectionModel().getSelectedItem();
        StringBuilder text = new StringBuilder();
        if (focusedCell != null && focusedCell.getTableColumn() != null && focusedCell.getRow() >= 0
                && focusedCell.getRow() < table.getItems().size()) {
            Object cellVal = focusedCell.getTableColumn().getCellData(focusedCell.getRow());
            if (cellVal != null && !cellVal.toString().isEmpty()) {
                text.append(cellVal);
            } else if (sel != null) {
                text.append(sel.getName()).append("=").append(sel.getValue());
            }
        } else if (sel != null) {
            text.append(sel.getName()).append("=").append(sel.getValue());
        } else {
            for (EnvVar v : table.getItems()) {
                if (!v.getName().isBlank()) {
                    text.append(v.getName()).append("=").append(v.getValue()).append("\n");
                }
            }
        }
        if (text.length() > 0) {
            ClipboardContent content = new ClipboardContent();
            content.putString(text.toString().trim());
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    private void pasteFromClipboard(ObservableList<EnvVar> list) {
        Clipboard cb = Clipboard.getSystemClipboard();
        if (cb.hasString()) {
            String text = cb.getString();
            for (String line : text.split("[\\r\\n;]+")) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    list.add(new EnvVar(line.substring(0, eq).trim(), line.substring(eq + 1).trim()));
                } else if (!line.isBlank()) {
                    list.add(new EnvVar(line.trim(), ""));
                }
            }
        }
    }

    private static javafx.scene.Node createCopyIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 4,4 L 10,4 L 10,12 L 4,12 Z M 6,2 L 12,2 L 12,10");
        path.setStroke(Color.web("#8C919D"));
        path.setStrokeWidth(1.1);
        path.setFill(null);
        return path;
    }

    private static javafx.scene.Node createPasteIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 5,2 L 9,2 M 3,4 L 11,4 L 11,13 L 3,13 Z");
        path.setStroke(Color.web("#8C919D"));
        path.setStrokeWidth(1.1);
        path.setFill(null);
        return path;
    }

    private static javafx.scene.Node createRevertIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 4,7 C 4,4.5 6.5,3 8.5,3 C 11.5,3 13,5.5 13,7 C 13,8.5 11.5,11 8.5,11 L 4,11 M 6,5 L 3.5,7 L 6,9");
        path.setStroke(Color.web("#8C919D"));
        path.setStrokeWidth(1.1);
        path.setFill(null);
        return path;
    }
}
