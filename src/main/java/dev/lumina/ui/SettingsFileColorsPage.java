package dev.lumina.ui;

import dev.lumina.scope.NamedScope;
import dev.lumina.scope.ScopeManager;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * File Colors settings page.
 * Strictly matches media_1790045065301.png and media_1790045065908.png.
 */
public class SettingsFileColorsPage extends VBox {

    private final FileColorManager colorManager = FileColorManager.getInstance();
    private final TableView<FileColorConfiguration> table = new TableView<>();
    private final ObservableList<FileColorConfiguration> tableData = FXCollections.observableArrayList();
    private final Runnable navigateToScopesCallback;

    private final Button addButton = new Button("+");
    private final Button removeButton = new Button("−");
    private final Button moveUpButton = new Button("↑");
    private final Button moveDownButton = new Button("↓");

    public SettingsFileColorsPage() {
        this(null);
    }

    public SettingsFileColorsPage(Runnable navigateToScopesCallback) {
        this.navigateToScopesCallback = navigateToScopesCallback;

        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 16, 24));
        setSpacing(12);
        VBox.setVgrow(this, Priority.ALWAYS);

        // ============================================================
        // 1. Top Options Checkboxes
        // ============================================================
        HBox topOptions = new HBox(20);
        topOptions.setAlignment(Pos.CENTER_LEFT);

        CheckBox enableColorsCheck = new CheckBox("Enable file colors");
        enableColorsCheck.setSelected(colorManager.isEnableFileColors());
        styleCheck(enableColorsCheck);
        enableColorsCheck.setOnAction(e -> colorManager.setEnableFileColors(enableColorsCheck.isSelected()));

        CheckBox useInTabsCheck = new CheckBox("Use in editor tabs");
        useInTabsCheck.setSelected(colorManager.isUseInEditorTabs());
        styleCheck(useInTabsCheck);
        useInTabsCheck.setOnAction(e -> colorManager.setUseInEditorTabs(useInTabsCheck.isSelected()));

        CheckBox useInProjectCheck = new CheckBox("Use in project view");
        useInProjectCheck.setSelected(colorManager.isUseInProjectView());
        styleCheck(useInProjectCheck);
        useInProjectCheck.setOnAction(e -> colorManager.setUseInProjectView(useInProjectCheck.isSelected()));

        topOptions.getChildren().addAll(enableColorsCheck, useInTabsCheck, useInProjectCheck);

        // ============================================================
        // 2. Table Toolbar (+, -, Up, Down)
        // ============================================================
        HBox tableToolbar = new HBox(6);
        tableToolbar.setAlignment(Pos.CENTER_LEFT);
        tableToolbar.setPadding(new Insets(4, 0, 4, 0));

        styleToolButton(addButton, "Add file color");
        addButton.setOnAction(e -> onAddEntry());

        styleToolButton(removeButton, "Remove");
        removeButton.setOnAction(e -> onRemoveEntry());

        styleToolButton(moveUpButton, "Move Up");
        moveUpButton.setOnAction(e -> onMoveUp());

        styleToolButton(moveDownButton, "Move Down");
        moveDownButton.setOnAction(e -> onMoveDown());

        tableToolbar.getChildren().addAll(addButton, removeButton, moveUpButton, moveDownButton);

        // ============================================================
        // 3. TableView
        // ============================================================
        table.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Column 1: Scope
        TableColumn<FileColorConfiguration, String> scopeCol = new TableColumn<>("Scope");
        scopeCol.setCellValueFactory(cellData -> cellData.getValue().scopeNameProperty());
        scopeCol.setPrefWidth(260);
        scopeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    setText(item);
                    String bg = isSelected() ? "#2E436E" : "#1E1F22";
                    String textFill = isSelected() ? "#FFFFFF" : "#DFE1E5";
                    setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textFill + "; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                }
            }
        });

        // Column 2: Color with authentic dropdown swatches
        TableColumn<FileColorConfiguration, String> colorCol = new TableColumn<>("Color");
        colorCol.setCellValueFactory(cellData -> cellData.getValue().colorNameProperty());
        colorCol.setPrefWidth(220);
        colorCol.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> combo = createColorComboBox();

            {
                combo.valueProperty().addListener((obs, oldV, newV) -> {
                    if (newV != null && getTableRow() != null) {
                        FileColorConfiguration config = getTableRow().getItem();
                        if (config != null) {
                            if ("Custom".equalsIgnoreCase(newV)) {
                                ColorPicker picker = new ColorPicker(Color.web(config.getCustomHex()));
                                Dialog<Color> colorDialog = new Dialog<>();
                                colorDialog.setTitle("Custom File Color");
                                colorDialog.getDialogPane().setContent(new VBox(10, new Label("Select custom color:"), picker));
                                colorDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                                colorDialog.setResultConverter(btn -> btn == ButtonType.OK ? picker.getValue() : null);
                                colorDialog.showAndWait().ifPresent(c -> {
                                    String hex = String.format("#%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
                                    config.setCustomHex(hex);
                                });
                            }
                            config.setColorName(newV);
                            colorManager.savePreferences();
                            updateComboStyle(combo, newV);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    combo.getSelectionModel().select(item);
                    updateComboStyle(combo, item);
                    setGraphic(combo);
                    String bg = isSelected() ? "#2E436E" : "#1E1F22";
                    setStyle("-fx-background-color: " + bg + "; -fx-alignment: center-left; -fx-padding: 2 6 2 6;");
                }
            }
        });

        // Column 3: Share through VCS
        Label vcsHeaderLabel = new Label("Share through VCS 🛈");
        vcsHeaderLabel.setTooltip(new Tooltip("Share this color assignment with team members via version control"));
        vcsHeaderLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        TableColumn<FileColorConfiguration, Boolean> vcsCol = new TableColumn<>();
        vcsCol.setGraphic(vcsHeaderLabel);
        vcsCol.setCellValueFactory(cellData -> cellData.getValue().sharedThroughVcsProperty());
        vcsCol.setPrefWidth(160);
        vcsCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                styleCheck(checkBox);
                checkBox.setOnAction(e -> {
                    if (getTableRow() != null) {
                        FileColorConfiguration config = getTableRow().getItem();
                        if (config != null) {
                            config.setSharedThroughVcs(checkBox.isSelected());
                            colorManager.savePreferences();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    checkBox.setSelected(item);
                    setGraphic(checkBox);
                    String bg = isSelected() ? "#2E436E" : "#1E1F22";
                    setStyle("-fx-background-color: " + bg + "; -fx-alignment: center;");
                }
            }
        });

        table.getColumns().addAll(scopeCol, colorCol, vcsCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateButtonStates());

        // Load data
        reloadTableData();

        // ============================================================
        // 4. Footer Section (Explanation & Manage scopes link)
        // ============================================================
        VBox footerBox = new VBox(6);
        footerBox.setPadding(new Insets(8, 0, 0, 0));

        Label desc = new Label("Files can belong to several scopes. If there are two colors for one scope, the color of the first scope in the list is used.");
        desc.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");
        desc.setWrapText(true);

        Hyperlink manageScopesLink = new Hyperlink("Manage scopes...");
        manageScopesLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;");
        manageScopesLink.setOnMouseEntered(e -> manageScopesLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0;"));
        manageScopesLink.setOnMouseExited(e -> manageScopesLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;"));
        manageScopesLink.setOnAction(e -> {
            if (navigateToScopesCallback != null) {
                navigateToScopesCallback.run();
            }
        });

        footerBox.getChildren().addAll(desc, manageScopesLink);

        getChildren().addAll(topOptions, tableToolbar, table, footerBox);
        updateButtonStates();
    }

    private void reloadTableData() {
        tableData.setAll(colorManager.getConfigurations());
        table.setItems(tableData);
        updateButtonStates();
    }

    private void updateButtonStates() {
        int idx = table.getSelectionModel().getSelectedIndex();
        boolean hasSel = (idx >= 0);
        removeButton.setDisable(!hasSel);
        moveUpButton.setDisable(idx <= 0);
        moveDownButton.setDisable(!hasSel || idx >= tableData.size() - 1);
    }

    private void onAddEntry() {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        AddColorDialog dialog = new AddColorDialog(owner);
        FileColorConfiguration newEntry = dialog.showAndWait();
        if (newEntry != null) {
            colorManager.addConfiguration(newEntry);
            reloadTableData();
            table.getSelectionModel().select(newEntry);
        }
    }

    private void onRemoveEntry() {
        FileColorConfiguration sel = table.getSelectionModel().getSelectedItem();
        if (sel != null) {
            colorManager.removeConfiguration(sel);
            reloadTableData();
        }
    }

    private void onMoveUp() {
        int idx = table.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            colorManager.moveUp(idx);
            reloadTableData();
            table.getSelectionModel().select(idx - 1);
        }
    }

    private void onMoveDown() {
        int idx = table.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < tableData.size() - 1) {
            colorManager.moveDown(idx);
            reloadTableData();
            table.getSelectionModel().select(idx + 1);
        }
    }

    private ComboBox<String> createColorComboBox() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll("Blue", "Gray", "Green", "Orange", "Rose", "Violet", "Yellow", "Custom");
        combo.setPrefWidth(180);

        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    setText(item);
                    String hex = FileColorManager.COLOR_HEX_MAP.get(item);
                    if (hex != null) {
                        Rectangle swatch = new Rectangle(14, 14, Color.web(hex));
                        swatch.setArcWidth(3);
                        swatch.setArcHeight(3);
                        setGraphic(swatch);
                        setStyle("-fx-background-color: " + hex + "; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 8 3 8;");
                    } else {
                        setGraphic(null);
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 3 8 3 8;");
                    }
                }
            }
        });

        return combo;
    }

    private void updateComboStyle(ComboBox<String> combo, String colorName) {
        String hex = FileColorManager.COLOR_HEX_MAP.get(colorName);
        if (hex != null) {
            combo.setStyle("-fx-background-color: " + hex + "; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 12px;");
        } else {
            combo.setStyle("-fx-background-color: #2E436E; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 12px;");
        }
    }

    private void styleCheck(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
    }

    private void styleToolButton(Button btn, String tooltipText) {
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 8 3 8;");
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisable()) {
                btn.setStyle("-fx-background-color: #35373C; -fx-border-color: #4E5157; -fx-border-radius: 3; -fx-background-radius: 3; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 8 3 8;");
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisable()) {
                btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8C9099; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 8 3 8;");
            }
        });
        if (tooltipText != null) {
            btn.setTooltip(new Tooltip(tooltipText));
        }
    }

    /**
     * Dialog to add a new file color mapping.
     */
    private static class AddColorDialog {
        private final Stage stage;
        private FileColorConfiguration result = null;

        public AddColorDialog(Window owner) {
            stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add File Color");
            stage.setResizable(false);

            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5;");
            root.setPadding(new Insets(16, 20, 16, 20));

            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(12);
            grid.setPadding(new Insets(10, 0, 14, 0));

            Label scopeLbl = new Label("Scope:");
            scopeLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            ComboBox<String> scopeCombo = new ComboBox<>();
            for (NamedScope s : ScopeManager.getInstance().getAllScopes()) {
                scopeCombo.getItems().add(s.getName());
            }
            if (!scopeCombo.getItems().isEmpty()) scopeCombo.getSelectionModel().select(0);
            scopeCombo.setPrefWidth(220);
            scopeCombo.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

            Label colorLbl = new Label("Color:");
            colorLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
            ComboBox<String> colorCombo = new ComboBox<>();
            colorCombo.getItems().addAll("Blue", "Gray", "Green", "Orange", "Rose", "Violet", "Yellow", "Custom");
            colorCombo.getSelectionModel().select("Blue");
            colorCombo.setPrefWidth(220);
            colorCombo.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

            CheckBox vcsCheck = new CheckBox("Share through VCS");
            vcsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

            grid.add(scopeLbl, 0, 0);
            grid.add(scopeCombo, 1, 0);
            grid.add(colorLbl, 0, 1);
            grid.add(colorCombo, 1, 1);
            grid.add(vcsCheck, 1, 2);

            root.setCenter(grid);

            HBox btnBox = new HBox(10);
            btnBox.setAlignment(Pos.CENTER_RIGHT);

            Button cancelBtn = new Button("Cancel");
            cancelBtn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
            cancelBtn.setOnAction(e -> stage.close());

            Button okBtn = new Button("OK");
            okBtn.setDefaultButton(true);
            okBtn.setStyle("-fx-background-color: #3574F0; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 14 5 14;");
            okBtn.setOnAction(e -> {
                String scope = scopeCombo.getValue();
                String col = colorCombo.getValue();
                if (scope != null && col != null) {
                    result = new FileColorConfiguration(scope, col, vcsCheck.isSelected());
                }
                stage.close();
            });

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            btnBox.getChildren().addAll(sp, cancelBtn, okBtn);
            root.setBottom(btnBox);

            Scene scene = new Scene(root, 360, 200);
            stage.setScene(scene);
        }

        public FileColorConfiguration showAndWait() {
            stage.showAndWait();
            return result;
        }
    }
}