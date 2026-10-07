package dev.lumina.ui;

import dev.lumina.settings.DuplicatesSettings;
import dev.lumina.settings.DuplicatesSettings.LanguageProfile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Settings page for Editor > Duplicates.
 * Matches IntelliJ IDEA layout with language list on the left and duplicate detection options on the right.
 */
public class SettingsDuplicatesPage extends VBox {

    private final DuplicatesSettings workingSettings;
    private Runnable onModifiedListener;

    private final ListView<LanguageProfile> languageListView = new ListView<>();
    private final ObservableList<LanguageProfile> languageItems = FXCollections.observableArrayList();

    // Right side controls
    private final Label rightTitleLabel = new Label("Detect duplicates with different:");
    private final CheckBox varNamesCheck = new CheckBox("Variable or identifier names");
    private final CheckBox funcNamesCheck = new CheckBox("Function or field names");
    private final CheckBox constValuesCheck = new CheckBox("Constant values");
    private final VBox rightPanel = new VBox(12);

    private LanguageProfile currentlySelected;
    private boolean updatingUi = false;

    public SettingsDuplicatesPage() {
        this.workingSettings = DuplicatesSettings.getInstance().copy();
        getStyleClass().add("settings-page");
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");

        buildUI();
        loadData();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (!updatingUi && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void buildUI() {
        // Layout: Split into left language list (~280px) and right options panel
        languageListView.setItems(languageItems);
        languageListView.setPrefWidth(260);
        languageListView.setMinWidth(220);
        languageListView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4;");
        languageListView.setCellFactory(lv -> new LanguageProfileCell());

        languageListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showProfileDetails(newVal);
            }
        });

        // Right side
        rightTitleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-weight: bold; -fx-font-size: 13px;");
        varNamesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        funcNamesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        constValuesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        varNamesCheck.setOnAction(e -> {
            if (currentlySelected != null && !updatingUi) {
                currentlySelected.setDifferentVariableNames(varNamesCheck.isSelected());
                fireModified();
            }
        });

        funcNamesCheck.setOnAction(e -> {
            if (currentlySelected != null && !updatingUi) {
                currentlySelected.setDifferentFunctionNames(funcNamesCheck.isSelected());
                fireModified();
            }
        });

        constValuesCheck.setOnAction(e -> {
            if (currentlySelected != null && !updatingUi) {
                currentlySelected.setDifferentConstantValues(constValuesCheck.isSelected());
                fireModified();
            }
        });

        rightPanel.setPadding(new Insets(6, 12, 12, 20));
        rightPanel.getChildren().addAll(rightTitleLabel, varNamesCheck, funcNamesCheck, constValuesCheck);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        HBox mainSplit = new HBox(16, languageListView, rightPanel);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);
        VBox.setVgrow(languageListView, Priority.ALWAYS);

        getChildren().add(mainSplit);
    }

    private void loadData() {
        updatingUi = true;
        languageItems.setAll(workingSettings.getProfiles());
        if (!languageItems.isEmpty()) {
            languageListView.getSelectionModel().select(0);
        }
        updatingUi = false;
    }

    private void showProfileDetails(LanguageProfile profile) {
        this.currentlySelected = profile;
        updatingUi = true;
        varNamesCheck.setSelected(profile.isDifferentVariableNames());
        funcNamesCheck.setSelected(profile.isDifferentFunctionNames());
        constValuesCheck.setSelected(profile.isDifferentConstantValues());

        boolean enabled = profile.isEnabled();
        varNamesCheck.setDisable(!enabled);
        funcNamesCheck.setDisable(!enabled);
        constValuesCheck.setDisable(!enabled);
        updatingUi = false;
    }

    public void apply() {
        DuplicatesSettings.getInstance().applyFrom(workingSettings);
        DuplicatesSettings.getInstance().save();
    }

    public void reset() {
        workingSettings.applyFrom(DuplicatesSettings.getInstance());
        loadData();
    }

    public boolean isModified() {
        return workingSettings.isModified(DuplicatesSettings.getInstance());
    }

    // Custom Cell with CheckBox on the left and Language Label
    private class LanguageProfileCell extends ListCell<LanguageProfile> {
        private final CheckBox checkBox = new CheckBox();
        private final Label label = new Label();
        private final HBox root = new HBox(10, checkBox, label);

        public LanguageProfileCell() {
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(3, 8, 3, 8));
            label.setStyle("-fx-text-fill: inherit; -fx-font-size: 13px;");

            checkBox.setOnAction(e -> {
                LanguageProfile item = getItem();
                if (item != null && !updatingUi) {
                    item.setEnabled(checkBox.isSelected());
                    if (currentlySelected == item) {
                        varNamesCheck.setDisable(!item.isEnabled());
                        funcNamesCheck.setDisable(!item.isEnabled());
                        constValuesCheck.setDisable(!item.isEnabled());
                    }
                    fireModified();
                }
            });
        }

        @Override
        protected void updateItem(LanguageProfile item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                updatingUi = true;
                label.setText(item.getName());
                checkBox.setSelected(item.isEnabled());
                updatingUi = false;

                if (isSelected()) {
                    setStyle("-fx-background-color: #2E436E; -fx-text-fill: #DFE1E5;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5;");
                }
                setGraphic(root);
                setText(null);
            }
        }
    }
}
