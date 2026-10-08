package dev.lumina.ui;

import dev.lumina.todo.TodoFilter;
import dev.lumina.todo.TodoIconType;
import dev.lumina.todo.TodoPattern;
import dev.lumina.todo.TodoSettingsManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Settings page for Editor > TODO in Lumina IDE.
 * Faithfully matches reference IDE design and controls (Screenshot 5).
 * Features dynamic pattern/filter registries, modal editors, empty state placeholder, and dirty tracking.
 */
public class SettingsTodoPage extends VBox {

    private final TodoSettingsManager manager = TodoSettingsManager.getInstance();

    private final CheckBox treatIndentedTextCheckBox = new CheckBox("Treat indented text on the following lines as part of the same TODO");

    // Patterns section
    private final Label patternsLabel = new Label("Patterns:");
    private final Button addPatternBtn = new Button();
    private final Button removePatternBtn = new Button();
    private final Button editPatternBtn = new Button();
    private final TableView<TodoPattern> patternsTable = new TableView<>();
    private final ObservableList<TodoPattern> patternsData = FXCollections.observableArrayList();

    // Filters section
    private final Label filtersLabel = new Label("Filters:");
    private final Button addFilterBtn = new Button();
    private final Button removeFilterBtn = new Button();
    private final Button editFilterBtn = new Button();
    private final TableView<TodoFilter> filtersTable = new TableView<>();
    private final ObservableList<TodoFilter> filtersData = FXCollections.observableArrayList();

    private Runnable onModifiedListener;
    private boolean updatingUi = false;

    public SettingsTodoPage() {
        setSpacing(14);
        setPadding(new Insets(16, 20, 16, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUi();
        setupListeners();
        loadFromManager();
    }

    private void buildUi() {
        // --- Top CheckBox ---
        treatIndentedTextCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // --- Patterns Section ---
        patternsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox patternsToolbar = createToolbar(addPatternBtn, removePatternBtn, editPatternBtn);
        buildPatternsTable();

        VBox patternsSection = new VBox(6, patternsLabel, patternsToolbar, patternsTable);
        VBox.setVgrow(patternsTable, Priority.ALWAYS);
        patternsTable.setPrefHeight(220);

        // --- Filters Section ---
        filtersLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox filtersToolbar = createToolbar(addFilterBtn, removeFilterBtn, editFilterBtn);
        buildFiltersTable();

        VBox filtersSection = new VBox(6, filtersLabel, filtersToolbar, filtersTable);
        VBox.setVgrow(filtersTable, Priority.ALWAYS);
        filtersTable.setPrefHeight(220);

        getChildren().addAll(treatIndentedTextCheckBox, patternsSection, filtersSection);
    }

    private HBox createToolbar(Button addBtn, Button removeBtn, Button editBtn) {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(2, 0, 2, 0));

        // Add (+)
        SVGPath plusSvg = new SVGPath();
        plusSvg.setContent("M11 5v6H5v2h6v6h2v-6h6v-2h-6V5h-2z");
        plusSvg.setFill(Color.web("#DFE1E5"));
        plusSvg.setScaleX(0.7);
        plusSvg.setScaleY(0.7);
        addBtn.setGraphic(plusSvg);
        addBtn.setTooltip(new Tooltip("Add (Alt+Insert)"));
        styleToolbarButton(addBtn);

        // Remove (-)
        SVGPath minusSvg = new SVGPath();
        minusSvg.setContent("M5 11h14v2H5z");
        minusSvg.setFill(Color.web("#DFE1E5"));
        minusSvg.setScaleX(0.7);
        minusSvg.setScaleY(0.7);
        removeBtn.setGraphic(minusSvg);
        removeBtn.setTooltip(new Tooltip("Remove (Delete)"));
        styleToolbarButton(removeBtn);
        removeBtn.setDisable(true);

        // Edit (Pencil)
        SVGPath editSvg = new SVGPath();
        editSvg.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
        editSvg.setFill(Color.web("#DFE1E5"));
        editSvg.setScaleX(0.65);
        editSvg.setScaleY(0.65);
        editBtn.setGraphic(editSvg);
        editBtn.setTooltip(new Tooltip("Edit"));
        styleToolbarButton(editBtn);
        editBtn.setDisable(true);

        bar.getChildren().addAll(addBtn, removeBtn, editBtn);
        return bar;
    }

    private void styleToolbarButton(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 3 5 3 5; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void buildPatternsTable() {
        patternsTable.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        patternsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        patternsTable.setItems(patternsData);

        // Col 1: Icon
        TableColumn<TodoPattern, TodoIconType> iconCol = new TableColumn<>("Icon");
        iconCol.setPrefWidth(55);
        iconCol.setMinWidth(50);
        iconCol.setMaxWidth(60);
        iconCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getIconType()));
        iconCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TodoIconType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Node iconNode = renderTodoIcon(item);
                    setGraphic(iconNode);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Col 2: Case Sensitive
        TableColumn<TodoPattern, Boolean> caseCol = new TableColumn<>("Case Sensitive");
        caseCol.setPrefWidth(110);
        caseCol.setMinWidth(90);
        caseCol.setMaxWidth(130);
        caseCol.setCellValueFactory(param -> new SimpleBooleanProperty(param.getValue().isCaseSensitive()));
        caseCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setDisable(true); // display state as shown in screenshot
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    cb.setSelected(item);
                    setGraphic(cb);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Col 3: Pattern
        TableColumn<TodoPattern, String> patternCol = new TableColumn<>("Pattern");
        patternCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPattern()));
        patternCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-family: monospace; -fx-font-size: 12px;");
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        patternsTable.getColumns().add(iconCol);
        patternsTable.getColumns().add(caseCol);
        patternsTable.getColumns().add(patternCol);
    }

    private Node renderTodoIcon(TodoIconType type) {
        if (type == TodoIconType.FIXME) {
            Circle c = new Circle(4.5);
            c.setFill(Color.web("#FF6B68"));
            return c;
        } else {
            Circle c = new Circle(4.5);
            c.setFill(Color.web("#3574F0"));
            return c;
        }
    }

    private void buildFiltersTable() {
        filtersTable.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        filtersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        filtersTable.setItems(filtersData);

        Label placeholder = new Label("No filters configured");
        placeholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        filtersTable.setPlaceholder(placeholder);

        // Col 1: Name
        TableColumn<TodoFilter, String> nameCol = new TableColumn<>("Name");
        nameCol.setPrefWidth(160);
        nameCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
                }
            }
        });

        // Col 2: Patterns
        TableColumn<TodoFilter, String> patternsCol = new TableColumn<>("Patterns");
        patternsCol.setCellValueFactory(param -> {
            TodoFilter f = param.getValue();
            String summary = f.getPatternIds().stream()
                    .map(id -> {
                        for (TodoPattern p : patternsData) {
                            if (p.getId().equals(id)) return p.getPattern();
                        }
                        return id;
                    })
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(summary);
        });
        patternsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
                }
            }
        });

        filtersTable.getColumns().add(nameCol);
        filtersTable.getColumns().add(patternsCol);
    }

    private void setupListeners() {
        treatIndentedTextCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingUi) {
                manager.setTreatIndentedText(newVal);
                notifyModified();
            }
        });

        // Patterns table selection
        patternsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSel = newVal != null;
            removePatternBtn.setDisable(!hasSel);
            editPatternBtn.setDisable(!hasSel);
        });

        patternsTable.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                handleEditPattern();
            }
        });

        addPatternBtn.setOnAction(e -> handleAddPattern());
        removePatternBtn.setOnAction(e -> handleRemovePattern());
        editPatternBtn.setOnAction(e -> handleEditPattern());

        patternsTable.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                handleRemovePattern();
            }
        });

        // Filters table selection
        filtersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSel = newVal != null;
            removeFilterBtn.setDisable(!hasSel);
            editFilterBtn.setDisable(!hasSel);
        });

        filtersTable.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                handleEditFilter();
            }
        });

        addFilterBtn.setOnAction(e -> handleAddFilter());
        removeFilterBtn.setOnAction(e -> handleRemoveFilter());
        editFilterBtn.setOnAction(e -> handleEditFilter());

        filtersTable.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                handleRemoveFilter();
            }
        });
    }

    private void handleAddPattern() {
        TodoPatternDialog dialog = new TodoPatternDialog(getScene() != null ? getScene().getWindow() : null, null);
        TodoPattern pattern = dialog.showDialog();
        if (pattern != null) {
            manager.addPattern(pattern);
            loadFromManager();
            patternsTable.getSelectionModel().select(pattern);
            notifyModified();
        }
    }

    private void handleEditPattern() {
        TodoPattern selected = patternsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            TodoPatternDialog dialog = new TodoPatternDialog(getScene() != null ? getScene().getWindow() : null, selected);
            TodoPattern updated = dialog.showDialog();
            if (updated != null) {
                manager.updatePattern(selected.getId(), updated);
                loadFromManager();
                patternsTable.getSelectionModel().select(updated);
                notifyModified();
            }
        }
    }

    private void handleRemovePattern() {
        TodoPattern selected = patternsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            manager.removePattern(selected.getId());
            loadFromManager();
            notifyModified();
        }
    }

    private void handleAddFilter() {
        TodoFilterDialog dialog = new TodoFilterDialog(getScene() != null ? getScene().getWindow() : null, null, manager.getWorkingPatterns());
        TodoFilter filter = dialog.showDialog();
        if (filter != null) {
            manager.addFilter(filter);
            loadFromManager();
            filtersTable.getSelectionModel().select(filter);
            notifyModified();
        }
    }

    private void handleEditFilter() {
        TodoFilter selected = filtersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            TodoFilterDialog dialog = new TodoFilterDialog(getScene() != null ? getScene().getWindow() : null, selected, manager.getWorkingPatterns());
            TodoFilter updated = dialog.showDialog();
            if (updated != null) {
                manager.updateFilter(selected.getId(), updated);
                loadFromManager();
                filtersTable.getSelectionModel().select(updated);
                notifyModified();
            }
        }
    }

    private void handleRemoveFilter() {
        TodoFilter selected = filtersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            manager.removeFilter(selected.getId());
            loadFromManager();
            notifyModified();
        }
    }

    public void loadFromManager() {
        updatingUi = true;
        try {
            treatIndentedTextCheckBox.setSelected(manager.isTreatIndentedText());

            patternsData.clear();
            patternsData.addAll(manager.getWorkingPatterns());

            filtersData.clear();
            filtersData.addAll(manager.getWorkingFilters());

            TodoPattern pSel = patternsTable.getSelectionModel().getSelectedItem();
            removePatternBtn.setDisable(pSel == null);
            editPatternBtn.setDisable(pSel == null);

            TodoFilter fSel = filtersTable.getSelectionModel().getSelectedItem();
            removeFilterBtn.setDisable(fSel == null);
            editFilterBtn.setDisable(fSel == null);
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
    public CheckBox getTreatIndentedTextCheckBox() {
        return treatIndentedTextCheckBox;
    }

    public TableView<TodoPattern> getPatternsTable() {
        return patternsTable;
    }

    public ObservableList<TodoPattern> getPatternsData() {
        return patternsData;
    }

    public TableView<TodoFilter> getFiltersTable() {
        return filtersTable;
    }

    public ObservableList<TodoFilter> getFiltersData() {
        return filtersData;
    }

    public Button getAddPatternBtn() {
        return addPatternBtn;
    }

    public Button getRemovePatternBtn() {
        return removePatternBtn;
    }

    public Button getEditPatternBtn() {
        return editPatternBtn;
    }

    public Button getAddFilterBtn() {
        return addFilterBtn;
    }

    public Button getRemoveFilterBtn() {
        return removeFilterBtn;
    }

    public Button getEditFilterBtn() {
        return editFilterBtn;
    }
}
