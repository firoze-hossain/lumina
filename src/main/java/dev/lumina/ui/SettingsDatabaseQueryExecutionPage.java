package dev.lumina.ui;

import dev.lumina.database.DatabaseQueryExecutionSettings;
import dev.lumina.database.DatabaseQueryExecutionSettingsManager;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Tools > Database > Query Execution settings page in Lumina IDE matching 1:1 design of the reference IDE.
 */
public class SettingsDatabaseQueryExecutionPage extends VBox {

    private final DatabaseQueryExecutionSettingsManager manager;
    private DatabaseQueryExecutionSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private ListView<String> profilesListView;
    private ComboBox<String> whenCaretInsideCombo;
    private ComboBox<String> whenCaretOutsideCombo;
    private ComboBox<String> forSelectionCombo;
    private CheckBox openResultsInNewTabCheck;

    private ComboBox<String> splitScriptCombo;
    private CheckBox reviewParametersBeforeExecutionCheck;
    private CheckBox showWarningUnsafeQueriesCheck;

    public SettingsDatabaseQueryExecutionPage() {
        this.manager = DatabaseQueryExecutionSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setPadding(new Insets(16, 24, 20, 24));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22;");

        // Upper Section: Profiles List on Left + Execution options on Right
        HBox upperBox = new HBox(16);
        upperBox.setAlignment(Pos.TOP_LEFT);

        // Left profiles list
        profilesListView = new ListView<>();
        profilesListView.getItems().addAll("Execute", "Execute (2)", "Execute (3)");
        profilesListView.getSelectionModel().selectFirst();
        profilesListView.setPrefWidth(160);
        profilesListView.setPrefHeight(150);
        profilesListView.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px;"
        );
        profilesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = new HBox(8);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    Label nameLbl = new Label(item);
                    nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                    cell.getChildren().add(nameLbl);
                    if ("Execute".equals(item)) {
                        Region sp = new Region();
                        HBox.setHgrow(sp, Priority.ALWAYS);
                        Label scLbl = new Label("⌘↩");
                        scLbl.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 11px;");
                        cell.getChildren().addAll(sp, scLbl);
                    }
                    setGraphic(cell);
                    setText(null);
                }
            }
        });

        // Right details area
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(10);
        detailsGrid.setVgap(10);
        HBox.setHgrow(detailsGrid, Priority.ALWAYS);

        Label scHeader = new Label("Shortcut:");
        scHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        Label scVal = new Label("⌘↩");
        scVal.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-font-weight: bold;");
        detailsGrid.add(scHeader, 0, 0);
        detailsGrid.add(scVal, 1, 0);

        Label insideLabel = new Label("When caret inside statement execute:");
        insideLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        whenCaretInsideCombo = new ComboBox<>();
        whenCaretInsideCombo.getItems().addAll("Ask what to execute", "Smallest statement", "Largest statement", "Whole script");
        styleCombo(whenCaretInsideCombo, 220);
        whenCaretInsideCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        detailsGrid.add(insideLabel, 0, 1);
        detailsGrid.add(whenCaretInsideCombo, 1, 1);

        Label outsideLabel = new Label("When caret outside statement execute:");
        outsideLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        whenCaretOutsideCombo = new ComboBox<>();
        whenCaretOutsideCombo.getItems().addAll("Nothing", "Next statement", "Previous statement", "Ask what to execute");
        styleCombo(whenCaretOutsideCombo, 220);
        whenCaretOutsideCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        detailsGrid.add(outsideLabel, 0, 2);
        detailsGrid.add(whenCaretOutsideCombo, 1, 2);

        Label forSelLabel = new Label("For selection execute:");
        forSelLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        forSelectionCombo = new ComboBox<>();
        forSelectionCombo.getItems().addAll("Exactly as separate statements", "As single statement", "Ask what to execute");
        styleCombo(forSelectionCombo, 220);
        forSelectionCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        detailsGrid.add(forSelLabel, 0, 3);
        detailsGrid.add(forSelectionCombo, 1, 3);

        openResultsInNewTabCheck = new CheckBox("Open results in new tab");
        openResultsInNewTabCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        openResultsInNewTabCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        detailsGrid.add(openResultsInNewTabCheck, 1, 4);

        upperBox.getChildren().addAll(profilesListView, detailsGrid);

        // Lower Section: Generic/ANSI SQL & Safety Checks
        VBox lowerBox = new VBox(10);
        lowerBox.setPadding(new Insets(16, 0, 0, 0));

        Label splitLabel = new Label("Split a script for execution in Generic and ANSI SQL dialects:");
        splitLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        splitScriptCombo = new ComboBox<>();
        splitScriptCombo.getItems().addAll(
                "Into valid ANSI SQL statements or by separator",
                "By separator only"
        );
        styleCombo(splitScriptCombo, 320);
        splitScriptCombo.valueProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        reviewParametersBeforeExecutionCheck = new CheckBox("Review parameters before execution");
        reviewParametersBeforeExecutionCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        reviewParametersBeforeExecutionCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        showWarningUnsafeQueriesCheck = new CheckBox("Show warning before running potentially unsafe queries");
        showWarningUnsafeQueriesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        showWarningUnsafeQueriesCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyModified());

        lowerBox.getChildren().addAll(
                splitLabel, splitScriptCombo,
                reviewParametersBeforeExecutionCheck, showWarningUnsafeQueriesCheck
        );

        getChildren().addAll(upperBox, lowerBox);
    }

    private void styleCombo(ComboBox<String> combo, double width) {
        combo.setPrefWidth(width);
        combo.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-font-size: 13px;"
        );
    }

    private void loadData() {
        updating = true;
        DatabaseQueryExecutionSettings s = manager.getSettings();
        profilesListView.getItems().setAll(s.getProfiles());
        profilesListView.getSelectionModel().select(s.getSelectedProfile());
        whenCaretInsideCombo.setValue(s.getWhenCaretInside());
        whenCaretOutsideCombo.setValue(s.getWhenCaretOutside());
        forSelectionCombo.setValue(s.getForSelection());
        openResultsInNewTabCheck.setSelected(s.isOpenResultsInNewTab());
        splitScriptCombo.setValue(s.getSplitScript());
        reviewParametersBeforeExecutionCheck.setSelected(s.isReviewParametersBeforeExecution());
        showWarningUnsafeQueriesCheck.setSelected(s.isShowWarningUnsafeQueries());
        initialSettings = getCurrentSettingsFromUI();
        updating = false;
    }

    public DatabaseQueryExecutionSettings getCurrentSettingsFromUI() {
        DatabaseQueryExecutionSettings s = new DatabaseQueryExecutionSettings();
        s.setProfiles(profilesListView.getItems());
        s.setSelectedProfile(profilesListView.getSelectionModel().getSelectedItem());
        s.setWhenCaretInside(whenCaretInsideCombo.getValue());
        s.setWhenCaretOutside(whenCaretOutsideCombo.getValue());
        s.setForSelection(forSelectionCombo.getValue());
        s.setOpenResultsInNewTab(openResultsInNewTabCheck.isSelected());
        s.setSplitScript(splitScriptCombo.getValue());
        s.setReviewParametersBeforeExecution(reviewParametersBeforeExecutionCheck.isSelected());
        s.setShowWarningUnsafeQueries(showWarningUnsafeQueriesCheck.isSelected());
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettingsFromUI());
    }

    public void apply() {
        DatabaseQueryExecutionSettings updated = getCurrentSettingsFromUI();
        manager.setSettings(updated);
        initialSettings = updated.clone();
        notifyModified();
    }

    public void reset() {
        loadData();
        notifyModified();
    }

    public void revertChanges() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public ComboBox<String> getWhenCaretInsideCombo() {
        return whenCaretInsideCombo;
    }

    public ComboBox<String> getWhenCaretOutsideCombo() {
        return whenCaretOutsideCombo;
    }

    public ComboBox<String> getForSelectionCombo() {
        return forSelectionCombo;
    }

    public CheckBox getOpenResultsInNewTabCheck() {
        return openResultsInNewTabCheck;
    }

    public ComboBox<String> getSplitScriptCombo() {
        return splitScriptCombo;
    }

    public CheckBox getReviewParametersBeforeExecutionCheck() {
        return reviewParametersBeforeExecutionCheck;
    }

    public CheckBox getShowWarningUnsafeQueriesCheck() {
        return showWarningUnsafeQueriesCheck;
    }
}
