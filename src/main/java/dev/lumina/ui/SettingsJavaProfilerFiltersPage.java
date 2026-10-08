package dev.lumina.ui;

import dev.lumina.profiler.JavaProfilerSettingsManager;
import dev.lumina.profiler.ProfilerFilterGroup;
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
import java.util.Optional;

/**
 * Settings page for Build, Execution, Deployment > Java Profiler > Filters (Screenshot 2).
 * Allows managing filter groups and patterns with inline editing, adding, removing, and reverting to defaults.
 */
public class SettingsJavaProfilerFiltersPage extends VBox {

    private final JavaProfilerSettingsManager manager = JavaProfilerSettingsManager.getInstance();

    private final ObservableList<ProfilerFilterGroup> filterList = FXCollections.observableArrayList();
    private final TableView<ProfilerFilterGroup> table = new TableView<>(filterList);

    private final Button addButton = new Button("+");
    private final Button removeButton = new Button("—");
    private final Button resetButton = new Button("↶");

    private List<ProfilerFilterGroup> initialFilters;
    private Runnable onModifiedListener;

    public SettingsJavaProfilerFiltersPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 20, 20, 20));
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        HBox toolbar = buildToolbar();
        setupTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().addAll(toolbar, table);
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 4, 6, 4));
        toolbar.setStyle("-fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        styleToolbarButton(addButton, "Add Filter Group");
        styleToolbarButton(removeButton, "Remove Selected Filter Group");
        styleToolbarButton(resetButton, "Reset to Default Filters");

        addButton.setOnAction(e -> handleAddGroup());
        removeButton.setOnAction(e -> handleRemoveGroup());
        resetButton.setOnAction(e -> handleResetToDefaults());

        toolbar.getChildren().addAll(addButton, removeButton, resetButton);
        return toolbar;
    }

    private void setupTable() {
        table.setEditable(true);
        table.setStyle("-fx-background-color: #1E1F22; -fx-base: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ProfilerFilterGroup, String> groupCol = new TableColumn<>("Group");
        groupCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGroupName()));
        groupCol.setCellFactory(TextFieldTableCell.forTableColumn());
        groupCol.setOnEditCommit(event -> {
            ProfilerFilterGroup group = event.getRowValue();
            group.setGroupName(event.getNewValue());
            notifyModified();
        });
        groupCol.setMinWidth(140);
        groupCol.setMaxWidth(220);

        TableColumn<ProfilerFilterGroup, String> filtersCol = new TableColumn<>("Filters");
        filtersCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFiltersPattern()));
        filtersCol.setCellFactory(TextFieldTableCell.forTableColumn());
        filtersCol.setOnEditCommit(event -> {
            ProfilerFilterGroup group = event.getRowValue();
            group.setFiltersPattern(event.getNewValue());
            notifyModified();
        });

        table.getColumns().add(groupCol);
        table.getColumns().add(filtersCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            removeButton.setDisable(newV == null);
        });
        removeButton.setDisable(true);
    }

    private void handleAddGroup() {
        Dialog<ProfilerFilterGroup> dialog = new Dialog<>();
        dialog.setTitle("Add Profiler Filter Group");
        dialog.setHeaderText("Specify filter group name and comma-separated patterns:");

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #1E1F22;");
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameTf = new TextField();
        nameTf.setPromptText("Group Name (e.g. Spring)");
        styleTextField(nameTf);

        TextField patternTf = new TextField();
        patternTf.setPromptText("Patterns (e.g. org.springframework.*)");
        styleTextField(patternTf);

        VBox content = new VBox(10,
                new Label("Group:") {{ setStyle("-fx-text-fill: #DFE1E5;"); }}, nameTf,
                new Label("Filters:") {{ setStyle("-fx-text-fill: #DFE1E5;"); }}, patternTf
        );
        content.setPadding(new Insets(10));
        pane.setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !nameTf.getText().isBlank()) {
                return new ProfilerFilterGroup(nameTf.getText().trim(), patternTf.getText().trim());
            }
            return null;
        });

        Optional<ProfilerFilterGroup> res = dialog.showAndWait();
        res.ifPresent(group -> {
            filterList.add(group);
            table.getSelectionModel().select(group);
            notifyModified();
        });
    }

    private void handleRemoveGroup() {
        int idx = table.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            filterList.remove(idx);
            notifyModified();
        }
    }

    private void handleResetToDefaults() {
        filterList.clear();
        for (ProfilerFilterGroup g : JavaProfilerSettingsManager.createDefaultFilterGroups()) {
            filterList.add(g.clone());
        }
        notifyModified();
    }

    public void loadData() {
        initialFilters = manager.getFilterGroups();
        filterList.clear();
        for (ProfilerFilterGroup g : initialFilters) {
            filterList.add(g.clone());
        }
    }

    public boolean isModified() {
        if (initialFilters == null || initialFilters.size() != filterList.size()) {
            return true;
        }
        for (int i = 0; i < initialFilters.size(); i++) {
            if (!Objects.equals(initialFilters.get(i), filterList.get(i))) {
                return true;
            }
        }
        return false;
    }

    public void apply() {
        manager.setFilterGroups(new ArrayList<>(filterList));
        initialFilters = manager.getFilterGroups();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    // --- Styling Helpers ---

    private void styleToolbarButton(Button btn, String tooltipText) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 7;");
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373C; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 7; -fx-background-radius: 4;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 3 7;"));
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-font-size: 13px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8;");
    }

    // Getters for testing
    public TableView<ProfilerFilterGroup> getTable() { return table; }
    public ObservableList<ProfilerFilterGroup> getFilterList() { return filterList; }
    public Button getAddButton() { return addButton; }
    public Button getRemoveButton() { return removeButton; }
    public Button getResetButton() { return resetButton; }
}
