package dev.lumina.ui;

import dev.lumina.textmate.TextMateBundle;
import dev.lumina.textmate.TextMateBundlesManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * Settings page for Editor > TextMate Bundles in Lumina IDE.
 * Faithfully matches reference IDE design and controls (Screenshots 2, 3, 4).
 * Dynamic SPI-backed bundle registry, built-in removal protection, and dirty tracking.
 */
public class SettingsTextMateBundlesPage extends VBox {

    private final TextMateBundlesManager manager = TextMateBundlesManager.getInstance();

    private final Label descriptionLabel = new Label("Use this page to add TextMate bundles for languages that do not have first-class support in Lumina IDE.");

    private final Button addBtn = new Button();
    private final Button removeBtn = new Button();
    private final TableView<BundleRowModel> tableView = new TableView<>();
    private final ObservableList<BundleRowModel> tableData = FXCollections.observableArrayList();

    private Runnable onModifiedListener;
    private boolean updatingUi = false;

    public static class BundleRowModel {
        private final SimpleBooleanProperty enabled;
        private final SimpleStringProperty name;
        private final SimpleStringProperty info;
        private final boolean builtIn;

        public BundleRowModel(TextMateBundle bundle) {
            this.enabled = new SimpleBooleanProperty(bundle.isEnabled());
            this.name = new SimpleStringProperty(bundle.getName());
            this.builtIn = bundle.isBuiltIn();
            this.info = new SimpleStringProperty(bundle.isBuiltIn() ? "Built-in" : (bundle.getPath() != null ? bundle.getPath() : ""));
        }

        public SimpleBooleanProperty enabledProperty() {
            return enabled;
        }

        public boolean isEnabled() {
            return enabled.get();
        }

        public void setEnabled(boolean enabled) {
            this.enabled.set(enabled);
        }

        public String getName() {
            return name.get();
        }

        public String getInfo() {
            return info.get();
        }

        public boolean isBuiltIn() {
            return builtIn;
        }
    }

    public SettingsTextMateBundlesPage() {
        setSpacing(12);
        setPadding(new Insets(16, 20, 16, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUi();
        setupListeners();
        loadFromManager();
    }

    private void buildUi() {
        descriptionLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
        descriptionLabel.setWrapText(true);

        // --- Toolbar ---
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 0, 4, 0));

        // Plus icon
        SVGPath plusSvg = new SVGPath();
        plusSvg.setContent("M11 5v6H5v2h6v6h2v-6h6v-2h-6V5h-2z");
        plusSvg.setFill(Color.web("#DFE1E5"));
        plusSvg.setScaleX(0.7);
        plusSvg.setScaleY(0.7);
        addBtn.setGraphic(plusSvg);
        addBtn.setTooltip(new Tooltip("Add (Alt+Insert)"));
        addBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6 4 6; -fx-border-radius: 4; -fx-background-radius: 4;");

        // Minus icon
        SVGPath minusSvg = new SVGPath();
        minusSvg.setContent("M5 11h14v2H5z");
        minusSvg.setFill(Color.web("#DFE1E5"));
        minusSvg.setScaleX(0.7);
        minusSvg.setScaleY(0.7);
        removeBtn.setGraphic(minusSvg);
        removeBtn.setTooltip(new Tooltip("Remove (Delete)"));
        removeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6 4 6; -fx-border-radius: 4; -fx-background-radius: 4;");
        removeBtn.setDisable(true); // initially disabled

        toolbar.getChildren().addAll(addBtn, removeBtn);

        // --- Table ---
        tableView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new Label("No TextMate bundles available"));
        tableView.setItems(tableData);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // Column 1: Checkbox
        TableColumn<BundleRowModel, Boolean> checkCol = new TableColumn<>();
        checkCol.setPrefWidth(36);
        checkCol.setMinWidth(36);
        checkCol.setMaxWidth(36);
        checkCol.setCellValueFactory(param -> param.getValue().enabledProperty());
        checkCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkCol));
        checkCol.setEditable(true);

        // Column 2: Name
        TableColumn<BundleRowModel, String> nameCol = new TableColumn<>();
        nameCol.setPrefWidth(300);
        nameCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-alignment: CENTER-LEFT;");
                }
            }
        });

        // Column 3: Info ("Built-in" or path)
        TableColumn<BundleRowModel, String> infoCol = new TableColumn<>();
        infoCol.setPrefWidth(140);
        infoCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getInfo()));
        infoCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });

        tableView.getColumns().add(checkCol);
        tableView.getColumns().add(nameCol);
        tableView.getColumns().add(infoCol);
        tableView.setEditable(true);

        // Hide table header to match reference screenshots
        tableView.widthProperty().addListener((obs, oldVal, newVal) -> {
            Pane header = (Pane) tableView.lookup("TableHeaderRow");
            if (header != null && header.isVisible()) {
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
            }
        });

        getChildren().addAll(descriptionLabel, toolbar, tableView);
    }

    private void setupListeners() {
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateRemoveButtonState(newVal);
        });

        addBtn.setOnAction(e -> handleAddBundle());
        removeBtn.setOnAction(e -> handleRemoveBundle());

        tableView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                handleRemoveBundle();
            }
        });
    }

    private void updateRemoveButtonState(BundleRowModel selected) {
        if (selected == null) {
            removeBtn.setDisable(true);
        } else {
            // Built-in bundles cannot be removed (disabled in screenshot)
            removeBtn.setDisable(selected.isBuiltIn());
        }
    }

    private void handleAddBundle() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select TextMate Bundle Directory");
        File dir = chooser.showDialog(getScene() != null ? getScene().getWindow() : null);
        if (dir != null && dir.exists()) {
            String bundleName = dir.getName();
            if (bundleName.endsWith(".tmbundle")) {
                bundleName = bundleName.substring(0, bundleName.length() - ".tmbundle".length());
            }
            boolean added = manager.addCustomBundle(bundleName, dir.getAbsolutePath());
            if (added) {
                loadFromManager();
                // Select newly added bundle
                for (BundleRowModel row : tableData) {
                    if (row.getName().equalsIgnoreCase(bundleName)) {
                        tableView.getSelectionModel().select(row);
                        tableView.scrollTo(row);
                        break;
                    }
                }
                notifyModified();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Bundle Exists");
                alert.setHeaderText(null);
                alert.setContentText("A TextMate bundle with the name '" + bundleName + "' already exists.");
                alert.showAndWait();
            }
        }
    }

    private void handleRemoveBundle() {
        BundleRowModel selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isBuiltIn()) {
            boolean removed = manager.removeBundle(selected.getName());
            if (removed) {
                loadFromManager();
                notifyModified();
            }
        }
    }

    public void loadFromManager() {
        updatingUi = true;
        try {
            tableData.clear();
            List<TextMateBundle> bundles = manager.getWorkingBundles();
            for (TextMateBundle b : bundles) {
                BundleRowModel model = new BundleRowModel(b);
                model.enabledProperty().addListener((obs, oldVal, newVal) -> {
                    if (!updatingUi) {
                        manager.setBundleEnabled(model.getName(), newVal);
                        notifyModified();
                    }
                });
                tableData.add(model);
            }
            updateRemoveButtonState(tableView.getSelectionModel().getSelectedItem());
        } finally {
            updatingUi = false;
        }
    }

    public boolean isModified() {
        return manager.isModified();
    }

    public void apply() {
        manager.apply();
    }

    public void reset() {
        manager.reset();
        loadFromManager();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
        manager.setOnModifiedListener(listener);
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // Accessors for testing
    public TableView<BundleRowModel> getTableView() {
        return tableView;
    }

    public ObservableList<BundleRowModel> getTableData() {
        return tableData;
    }

    public Button getAddBtn() {
        return addBtn;
    }

    public Button getRemoveBtn() {
        return removeBtn;
    }
}
