package dev.lumina.ui;

import dev.lumina.naturallang.NaturalLanguagesManager;
import dev.lumina.naturallang.SpellingDictionaryItem;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Collections;
import java.util.List;

/**
 * Settings page for Editor > Natural Languages > Spelling.
 * Faithfully matches reference IDE design (Images 2, 3, 4):
 * - "Use single dictionary for saving words:" checkbox with target dropdown (project-level / application-level)
 * - "Custom dictionaries (plain text word lists, hunspell):" list with toolbar (+ / — / ✏)
 *     - Displays Application-level dictionary [built-in] and Project-level dictionary [built-in]
 *     - Add button opens SelectPathDialog (Image 3)
 * - "Accepted words:" list with toolbar (+ / —)
 *     - Empty state displays centered "No words added"
 *     - Add button opens AddNewWordDialog (Image 4)
 * - "Configure 'Spelling' inspection..." navigation link
 * - Full dirty-tracking and apply/reset lifecycle integrated with NaturalLanguagesManager
 */
public class SettingsSpellingPage extends VBox {

    private final NaturalLanguagesManager manager = NaturalLanguagesManager.getInstance();

    // 1. Single dictionary option controls
    private final CheckBox useSingleDictCheck = new CheckBox("Use single dictionary for saving words:");
    private final ComboBox<String> singleDictCombo = new ComboBox<>();

    // 2. Custom dictionaries controls
    private final ObservableList<SpellingDictionaryItem> dictionariesList = FXCollections.observableArrayList();
    private final ListView<SpellingDictionaryItem> dictionariesView = new ListView<>(dictionariesList);
    private final Button addDictBtn = new Button("+");
    private final Button removeDictBtn = new Button("—");
    private final Button editDictBtn = new Button("✏");

    // 3. Accepted words controls
    private final ObservableList<String> acceptedWordsList = FXCollections.observableArrayList();
    private final ListView<String> acceptedWordsView = new ListView<>(acceptedWordsList);
    private final Label noWordsPlaceholder = new Label("No words added");
    private final Button addWordBtn = new Button("+");
    private final Button removeWordBtn = new Button("—");

    // 4. Bottom inspection link
    private final Hyperlink configureInspectionLink = new Hyperlink("Configure 'Spelling' inspection...");

    private final BooleanProperty modifiedProperty = new SimpleBooleanProperty(false);
    private Runnable onModifiedListener;
    private boolean updatingUI = false;

    public SettingsSpellingPage() {
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 18, 24));
        setSpacing(14);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();

        manager.setOnModifiedListener(() -> {
            notifyModified();
        });
    }

    private void buildUI() {
        getChildren().clear();

        // 1. Top row: Use single dictionary for saving words
        HBox singleDictRow = new HBox(8);
        singleDictRow.setAlignment(Pos.CENTER_LEFT);

        useSingleDictCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        singleDictCombo.getItems().setAll("project-level", "application-level");
        singleDictCombo.setValue("project-level");
        styleComboBox(singleDictCombo, 180);

        singleDictCombo.disableProperty().bind(useSingleDictCheck.selectedProperty().not());

        useSingleDictCheck.setOnAction(e -> {
            if (!updatingUI) {
                manager.setUseSingleDictionary(useSingleDictCheck.isSelected());
                notifyModified();
            }
        });

        singleDictCombo.setOnAction(e -> {
            if (!updatingUI && singleDictCombo.getValue() != null) {
                manager.setSingleDictionaryTarget(singleDictCombo.getValue());
                notifyModified();
            }
        });

        singleDictRow.getChildren().addAll(useSingleDictCheck, singleDictCombo);

        // 2. Section 1: Custom dictionaries (plain text word lists, hunspell)
        VBox customDictSection = new VBox(6);
        Label customDictHeader = new Label("Custom dictionaries (plain text word lists, hunspell):");
        customDictHeader.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        VBox dictContainer = new VBox(0);
        dictContainer.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
        dictContainer.setPrefHeight(150);
        dictContainer.setMaxHeight(180);

        HBox dictToolbar = new HBox(4);
        dictToolbar.setAlignment(Pos.CENTER_LEFT);
        dictToolbar.setPadding(new Insets(3, 8, 3, 8));
        dictToolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        styleToolbarButton(addDictBtn);
        styleToolbarButton(removeDictBtn);
        styleToolbarButton(editDictBtn);

        removeDictBtn.setDisable(true);
        editDictBtn.setDisable(true);

        addDictBtn.setOnAction(e -> handleAddDictionary());
        removeDictBtn.setOnAction(e -> handleRemoveDictionary());
        editDictBtn.setOnAction(e -> handleEditDictionary());

        dictToolbar.getChildren().addAll(addDictBtn, removeDictBtn, editDictBtn);

        dictionariesView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: transparent;");
        dictionariesView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SpellingDictionaryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    TextFlow flow = new TextFlow();
                    Text nameText = new Text(item.name());
                    nameText.setStyle("-fx-fill: #DFE1E5; -fx-font-size: 12px;");
                    flow.getChildren().add(nameText);

                    if (item.isBuiltIn()) {
                        Text tag = new Text(" [built-in]");
                        tag.setStyle("-fx-fill: #8C919D; -fx-font-size: 11px;");
                        flow.getChildren().add(tag);
                    }
                    setGraphic(flow);
                    setText(null);
                    setStyle("-fx-padding: 2 6;");
                }
            }
        });

        dictionariesView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            boolean isCustom = sel != null && !sel.isBuiltIn();
            removeDictBtn.setDisable(!isCustom);
            editDictBtn.setDisable(!isCustom);
        });

        VBox.setVgrow(dictionariesView, Priority.ALWAYS);
        dictContainer.getChildren().addAll(dictToolbar, dictionariesView);
        customDictSection.getChildren().addAll(customDictHeader, dictContainer);

        // 3. Section 2: Accepted words
        VBox acceptedWordsSection = new VBox(6);
        VBox.setVgrow(acceptedWordsSection, Priority.ALWAYS);

        Label acceptedWordsHeader = new Label("Accepted words:");
        acceptedWordsHeader.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        VBox wordsContainer = new VBox(0);
        wordsContainer.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
        wordsContainer.setPrefHeight(220);
        VBox.setVgrow(wordsContainer, Priority.ALWAYS);

        HBox wordsToolbar = new HBox(4);
        wordsToolbar.setAlignment(Pos.CENTER_LEFT);
        wordsToolbar.setPadding(new Insets(3, 8, 3, 8));
        wordsToolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        styleToolbarButton(addWordBtn);
        styleToolbarButton(removeWordBtn);
        removeWordBtn.setDisable(true);

        addWordBtn.setOnAction(e -> handleAddWord());
        removeWordBtn.setOnAction(e -> handleRemoveWord());

        wordsToolbar.getChildren().addAll(addWordBtn, removeWordBtn);

        StackPane wordsStack = new StackPane();
        VBox.setVgrow(wordsStack, Priority.ALWAYS);

        acceptedWordsView.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: transparent;");
        acceptedWordsView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 2 6;");
                }
            }
        });

        noWordsPlaceholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 12px;");
        noWordsPlaceholder.visibleProperty().bind(Bindings.isEmpty(acceptedWordsList));
        noWordsPlaceholder.managedProperty().bind(noWordsPlaceholder.visibleProperty());

        acceptedWordsView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            removeWordBtn.setDisable(sel == null);
        });

        wordsStack.getChildren().addAll(acceptedWordsView, noWordsPlaceholder);
        wordsContainer.getChildren().addAll(wordsToolbar, wordsStack);
        acceptedWordsSection.getChildren().addAll(acceptedWordsHeader, wordsContainer);

        // 4. Bottom Link: Configure 'Spelling' inspection...
        configureInspectionLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 4 0 0 0;");

        getChildren().addAll(singleDictRow, customDictSection, acceptedWordsSection, configureInspectionLink);
    }

    private void handleAddDictionary() {
        SelectPathDialog dialog = new SelectPathDialog(getScene() != null ? getScene().getWindow() : null, null);
        String selectedPath = dialog.showAndWait();
        if (selectedPath != null && !selectedPath.trim().isEmpty()) {
            if (manager.addCustomDictionary(selectedPath.trim())) {
                refreshDictionaries();
                notifyModified();
            }
        }
    }

    private void handleRemoveDictionary() {
        SpellingDictionaryItem sel = dictionariesView.getSelectionModel().getSelectedItem();
        if (sel != null && !sel.isBuiltIn()) {
            if (manager.removeCustomDictionary(sel.path())) {
                refreshDictionaries();
                notifyModified();
            }
        }
    }

    private void handleEditDictionary() {
        SpellingDictionaryItem sel = dictionariesView.getSelectionModel().getSelectedItem();
        if (sel != null && !sel.isBuiltIn()) {
            SelectPathDialog dialog = new SelectPathDialog(getScene() != null ? getScene().getWindow() : null, sel.path());
            String newPath = dialog.showAndWait();
            if (newPath != null && !newPath.equals(sel.path())) {
                manager.removeCustomDictionary(sel.path());
                manager.addCustomDictionary(newPath);
                refreshDictionaries();
                notifyModified();
            }
        }
    }

    private void handleAddWord() {
        AddNewWordDialog dialog = new AddNewWordDialog(getScene() != null ? getScene().getWindow() : null);
        String word = dialog.showAndWait();
        if (word != null && !word.trim().isEmpty()) {
            if (manager.addAcceptedWord(word.trim())) {
                refreshAcceptedWords();
                notifyModified();
            }
        }
    }

    private void handleRemoveWord() {
        String sel = acceptedWordsView.getSelectionModel().getSelectedItem();
        if (sel != null) {
            if (manager.removeAcceptedWord(sel)) {
                refreshAcceptedWords();
                notifyModified();
            }
        }
    }

    private void styleToolbarButton(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 8; -fx-border-color: transparent;");
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisable()) {
                btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 8; -fx-border-color: transparent; -fx-background-radius: 3;");
            }
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 8; -fx-border-color: transparent;");
        });
    }

    private void styleComboBox(ComboBox<String> combo, double prefWidth) {
        combo.setPrefWidth(prefWidth);
        combo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #3E4147; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px; -fx-padding: 1 4;");
    }

    private void refreshDictionaries() {
        dictionariesList.setAll(manager.getAllDictionaries());
    }

    private void refreshAcceptedWords() {
        acceptedWordsList.setAll(manager.getAcceptedWords());
    }

    private void notifyModified() {
        modifiedProperty.set(isModified());
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void loadData() {
        updatingUI = true;
        try {
            useSingleDictCheck.setSelected(manager.isUseSingleDictionary());
            singleDictCombo.setValue(manager.getSingleDictionaryTarget());
            refreshDictionaries();
            refreshAcceptedWords();
        } finally {
            updatingUI = false;
        }
    }

    // =========================================================================
    // Settings Lifecycle
    // =========================================================================

    public boolean isModified() {
        return manager.isModified();
    }

    public void apply() {
        manager.apply();
        modifiedProperty.set(false);
    }

    public void reset() {
        manager.reset();
        modifiedProperty.set(false);
        loadData();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public BooleanProperty modifiedProperty() {
        return modifiedProperty;
    }

    // Accessors for testing
    public CheckBox getUseSingleDictCheck() {
        return useSingleDictCheck;
    }

    public ComboBox<String> getSingleDictCombo() {
        return singleDictCombo;
    }

    public ListView<SpellingDictionaryItem> getDictionariesView() {
        return dictionariesView;
    }

    public ListView<String> getAcceptedWordsView() {
        return acceptedWordsView;
    }

    public Button getAddDictBtn() {
        return addDictBtn;
    }

    public Button getRemoveDictBtn() {
        return removeDictBtn;
    }

    public Button getEditDictBtn() {
        return editDictBtn;
    }

    public Button getAddWordBtn() {
        return addWordBtn;
    }

    public Button getRemoveWordBtn() {
        return removeWordBtn;
    }

    public Hyperlink getConfigureInspectionLink() {
        return configureInspectionLink;
    }

    public Label getNoWordsPlaceholder() {
        return noWordsPlaceholder;
    }
}
