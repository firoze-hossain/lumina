package dev.lumina.ui;

import dev.lumina.go.GoSettings;
import dev.lumina.go.GoSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings page for Languages & Frameworks > Go > Formatting Functions.
 * Faithfully matches Image 2.
 */
public class SettingsLanguagesGoFormattingFunctionsPage extends VBox {

    private final GoSettingsManager manager = GoSettingsManager.getInstance();

    private final ObservableList<String> excludedFunctions = FXCollections.observableArrayList();
    private ListView<String> listView;

    private List<String> initialExcludedFunctions = new ArrayList<>();
    private Runnable onModified;

    public SettingsLanguagesGoFormattingFunctionsPage() {
        setSpacing(10);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadFromManager();
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (onModified != null) {
            onModified.run();
        }
    }

    private void buildUI() {
        // Section Header
        Label headerLabel = new Label("Excluded Formatting Functions");
        headerLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        // Toolbar (+ / —)
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(3, 6, 3, 6));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0; -fx-background-radius: 4 4 0 0;");

        Button addBtn = new Button("+");
        addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;");
        addBtn.setOnAction(e -> showAddFormattingFunctionDialog());

        Button removeBtn = new Button("—");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6;");
        removeBtn.setOnAction(e -> removeSelectedFunction());

        toolbar.getChildren().addAll(addBtn, removeBtn);

        // ListView with "Nothing to show" placeholder
        listView = new ListView<>(excludedFunctions);
        listView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4;");
        listView.setPrefHeight(240);
        VBox.setVgrow(listView, Priority.ALWAYS);

        Label emptyLabel = new Label("Nothing to show");
        emptyLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");
        listView.setPlaceholder(emptyLabel);

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
                }
            }
        });

        VBox container = new VBox(headerLabel, toolbar, listView);
        container.setSpacing(4);
        VBox.setVgrow(container, Priority.ALWAYS);

        getChildren().add(container);
    }

    private Stage getOwnerStage() {
        if (getScene() != null && getScene().getWindow() instanceof Stage s) {
            return s;
        }
        return null;
    }

    private void removeSelectedFunction() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < excludedFunctions.size()) {
            excludedFunctions.remove(idx);
            notifyModified();
        }
    }

    private void showAddFormattingFunctionDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(getOwnerStage());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Select formatting function");

        VBox root = new VBox(10);
        root.setPadding(new Insets(12, 14, 12, 14));
        root.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1px;");

        // Top Row: Title + Non-project CheckBox (matching Image 2)
        Label titleLabel = new Label("Select formatting function");
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        CheckBox nonProjectCheck = new CheckBox("Non-project");
        nonProjectCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        nonProjectCheck.setSelected(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(8, titleLabel, spacer, nonProjectCheck);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Search Field with 🔍 icon
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 ");
        searchField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-width: 1.5px; -fx-border-radius: 4px; -fx-padding: 4 8; -fx-font-size: 13px;");

        // Suggestions List
        ObservableList<String> suggestions = FXCollections.observableArrayList();
        ListView<String> suggestionListView = new ListView<>(suggestions);
        suggestionListView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4px;");
        suggestionListView.setPrefHeight(150);

        List<String> standardFunctions = List.of(
                "fmt.Printf", "fmt.Sprintf", "fmt.Fprintf", "fmt.Errorf",
                "fmt.Scanf", "fmt.Sscanf", "fmt.Fscanf",
                "log.Printf", "log.Fatalf", "log.Panicf",
                "testing.T.Logf", "testing.T.Errorf", "testing.T.Fatalf"
        );

        Runnable updateSuggestions = () -> {
            String q = searchField.getText().trim().toLowerCase();
            suggestions.clear();
            if (nonProjectCheck.isSelected()) {
                for (String fn : standardFunctions) {
                    if (q.isEmpty() || fn.toLowerCase().contains(q)) {
                        suggestions.add(fn);
                    }
                }
            }
        };

        updateSuggestions.run();

        nonProjectCheck.selectedProperty().addListener((obs, ov, nv) -> updateSuggestions.run());
        searchField.textProperty().addListener((obs, ov, nv) -> updateSuggestions.run());

        Runnable commitSelection = () -> {
            String selected = suggestionListView.getSelectionModel().getSelectedItem();
            if (selected == null && !searchField.getText().trim().isEmpty()) {
                selected = searchField.getText().trim();
            }
            if (selected != null && !selected.isBlank() && !excludedFunctions.contains(selected)) {
                excludedFunctions.add(selected);
                notifyModified();
            }
            dialog.close();
        };

        searchField.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ENTER) {
                commitSelection.run();
            } else if (evt.getCode() == KeyCode.ESCAPE) {
                dialog.close();
            }
        });

        suggestionListView.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2) {
                commitSelection.run();
            }
        });

        suggestionListView.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ENTER) {
                commitSelection.run();
            }
        });

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: #FFFFFF; -fx-padding: 4 12; -fx-cursor: hand; -fx-border-radius: 4; -fx-background-radius: 4;");
        okBtn.setOnAction(e -> commitSelection.run());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-padding: 4 12; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        btnRow.getChildren().addAll(okBtn, cancelBtn);

        root.getChildren().addAll(topRow, searchField, suggestionListView, btnRow);

        Scene scene = new Scene(root, 360, 260);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public void loadFromManager() {
        GoSettings s = manager.getSettings();
        excludedFunctions.setAll(s.getExcludedFormattingFunctions());
        initialExcludedFunctions = new ArrayList<>(excludedFunctions);
    }

    public boolean isModified() {
        return !excludedFunctions.equals(initialExcludedFunctions);
    }

    public void apply() {
        GoSettings s = manager.getSettings();
        s.setExcludedFormattingFunctions(new ArrayList<>(excludedFunctions));
        manager.setSettings(s);
        initialExcludedFunctions = new ArrayList<>(excludedFunctions);
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }

    public List<String> getExcludedFunctions() {
        return new ArrayList<>(excludedFunctions);
    }

    public void addExcludedFunction(String fn) {
        if (fn != null && !fn.isBlank() && !excludedFunctions.contains(fn)) {
            excludedFunctions.add(fn);
            notifyModified();
        }
    }
}
