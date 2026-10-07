package dev.lumina.ui;

import dev.lumina.naturallang.NaturalLanguage;
import dev.lumina.naturallang.NaturalLanguagesManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;

/**
 * Natural Languages settings page matching reference IDE design (Screenshot 1 & 2).
 * Features:
 * - Configured languages list with + / — toolbar
 * - Available Languages popup with dialect/rule count badges
 * - Clickable "Configure 'Proofreading' inspections..." link
 * - "Language processing: Local | Enable Cloud" with AI cloud help text
 * - Auto-fix and Oxford spelling checkboxes
 * - Full dirty-tracking and apply/reset lifecycle
 */
public class SettingsNaturalLanguagesPage extends VBox {

    private final NaturalLanguagesManager manager = NaturalLanguagesManager.getInstance();

    // Languages list
    private final ObservableList<NaturalLanguage> languagesList = FXCollections.observableArrayList();
    private final ListView<NaturalLanguage> languagesView = new ListView<>(languagesList);
    private final Button addButton = new Button("+");
    private final Button removeButton = new Button("—");

    // Language processing
    private final Label processingLabel = new Label("Language processing:");
    private final Circle localIndicator = new Circle(4, Color.web("#62B543"));
    private final Label localStatusLabel = new Label("Local");
    private final Hyperlink cloudToggleLink = new Hyperlink("Enable Cloud");
    private final Label cloudHelpLabel = new Label("Lumina AI Cloud improves spelling, grammar, and style checks and provides additional features such as rephrasing");

    // Checkboxes
    private final CheckBox autoFixCheck = new CheckBox("Automatically fix simple issues as you type (e.g., convert hyphens to dashes)");
    private final CheckBox oxfordSpellingCheck = new CheckBox("Use Oxford Spelling for British English");

    // Inspections link
    private final Hyperlink proofreadingLink = new Hyperlink("Configure 'Proofreading' inspections...");

    private final BooleanProperty modifiedProperty = new SimpleBooleanProperty(false);
    private Runnable onModifiedListener;
    private Runnable onNavigateToInspections;
    private boolean updatingUI = false;

    public SettingsNaturalLanguagesPage() {
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(14, 24, 18, 24));
        setSpacing(12);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();

        manager.setOnModifiedListener(() -> {
            modifiedProperty.set(manager.isModified());
            if (onModifiedListener != null) {
                onModifiedListener.run();
            }
        });
    }

    private void buildUI() {
        getChildren().clear();

        // 1. Languages Label
        Label languagesHeader = new Label("Languages:");
        languagesHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        // 2. Languages List & Toolbar Box
        VBox listContainer = buildLanguagesListContainer();

        // 3. Inspections Link
        proofreadingLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 2 0;");
        proofreadingLink.setOnMouseEntered(e -> proofreadingLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 2 0;"));
        proofreadingLink.setOnMouseExited(e -> proofreadingLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 2 0;"));
        proofreadingLink.setOnAction(e -> {
            if (onNavigateToInspections != null) {
                onNavigateToInspections.run();
            }
        });

        // 4. Language Processing Section
        VBox processingBox = buildProcessingSection();

        // 5. Separator
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #393B40;");
        separator.setPadding(new Insets(4, 0, 4, 0));

        // 6. Checkboxes Section
        VBox checkboxesBox = new VBox(8);
        autoFixCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        oxfordSpellingCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");

        autoFixCheck.setOnAction(e -> {
            if (!updatingUI) {
                manager.setAutoFix(autoFixCheck.isSelected());
                notifyModified();
            }
        });

        oxfordSpellingCheck.setOnAction(e -> {
            if (!updatingUI) {
                manager.setOxfordSpelling(oxfordSpellingCheck.isSelected());
                notifyModified();
            }
        });

        checkboxesBox.getChildren().addAll(autoFixCheck, oxfordSpellingCheck);

        getChildren().addAll(
                languagesHeader,
                listContainer,
                proofreadingLink,
                processingBox,
                separator,
                checkboxesBox
        );
    }

    private VBox buildLanguagesListContainer() {
        VBox container = new VBox();
        container.setMaxWidth(380);

        languagesView.setPrefHeight(130);
        languagesView.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 4 4 0 0;"
        );

        languagesView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(NaturalLanguage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    setText(item.getDisplayName());
                    boolean sel = isSelected();
                    if (sel) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF; -fx-padding: 4 8;");
                    } else {
                        setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-padding: 4 8;");
                    }
                }
            }
        });

        languagesView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateToolbarState());

        // Toolbar (+ / —)
        HBox toolbar = new HBox(2);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 6, 2, 6));
        toolbar.setStyle("-fx-background-color: #25272B; -fx-border-color: transparent #393B40 #393B40 #393B40; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 4 4;");

        styleToolButton(addButton);
        styleToolButton(removeButton);

        addButton.setTooltip(new Tooltip("Add Natural Language"));
        removeButton.setTooltip(new Tooltip("Remove Selected Natural Language"));

        addButton.setOnAction(e -> showAvailableLanguagesPopup(addButton));
        removeButton.setOnAction(e -> removeSelectedLanguage());

        toolbar.getChildren().addAll(addButton, removeButton);
        container.getChildren().addAll(languagesView, toolbar);
        return container;
    }

    private void styleToolButton(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373C; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand; -fx-background-radius: 3;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #868A91; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;"));
    }

    private void updateToolbarState() {
        boolean hasSel = languagesView.getSelectionModel().getSelectedItem() != null;
        boolean canRemove = hasSel && languagesList.size() > 1;
        removeButton.setDisable(!canRemove);
        removeButton.setStyle(canRemove
                ? "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-text-fill: #55575E; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6;");
    }

    // =========================================================================
    // Available Languages Popup (Screenshot 2)
    // =========================================================================

    private void showAvailableLanguagesPopup(Node anchor) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #3E4147; -fx-border-radius: 4; -fx-background-radius: 4;");

        // Header Item
        CustomMenuItem headerItem = new CustomMenuItem(new Label("Available Languages"));
        headerItem.setHideOnClick(false);
        Label headerLbl = (Label) headerItem.getContent();
        headerLbl.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10 2 10;");
        menu.getItems().add(headerItem);

        List<NaturalLanguage> availableToAdd = manager.getLanguagesAvailableToAdd();
        if (availableToAdd.isEmpty()) {
            MenuItem emptyItem = new MenuItem("All available languages added");
            emptyItem.setDisable(true);
            menu.getItems().add(emptyItem);
            menu.show(anchor, Side.BOTTOM, 0, 0);
            return;
        }

        // 1. Dialects section
        boolean addedDialect = false;
        for (NaturalLanguage lang : availableToAdd) {
            if (lang.isDialect()) {
                menu.getItems().add(createLanguageMenuItem(lang, menu));
                addedDialect = true;
            }
        }

        if (addedDialect) {
            menu.getItems().add(new SeparatorMenuItem());
        }

        // 2. All other languages
        for (NaturalLanguage lang : availableToAdd) {
            if (!lang.isDialect()) {
                menu.getItems().add(createLanguageMenuItem(lang, menu));
            }
        }

        menu.show(anchor, Side.BOTTOM, 0, 0);
    }

    private CustomMenuItem createLanguageMenuItem(NaturalLanguage lang, ContextMenu parentMenu) {
        BorderPane row = new BorderPane();
        row.setPrefWidth(220);
        row.setPadding(new Insets(3, 8, 3, 8));

        Label nameLbl = new Label(lang.getDisplayName());
        nameLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        row.setLeft(nameLbl);

        if (lang.getRuleCount() > 0) {
            Label countLbl = new Label(String.valueOf(lang.getRuleCount()));
            countLbl.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");
            row.setRight(countLbl);
        }

        CustomMenuItem item = new CustomMenuItem(row);
        item.setOnAction(e -> {
            manager.addLanguage(lang);
            loadData();
            languagesView.getSelectionModel().select(lang);
            notifyModified();
        });

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #2E436E; -fx-background-radius: 3;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent;"));

        return item;
    }

    private void removeSelectedLanguage() {
        NaturalLanguage sel = languagesView.getSelectionModel().getSelectedItem();
        if (sel != null && languagesList.size() > 1) {
            manager.removeLanguage(sel);
            loadData();
            notifyModified();
        }
    }

    // =========================================================================
    // Language Processing Section
    // =========================================================================

    private VBox buildProcessingSection() {
        VBox box = new VBox(4);

        HBox processingRow = new HBox(8);
        processingRow.setAlignment(Pos.CENTER_LEFT);

        processingLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        localStatusLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        cloudToggleLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        cloudToggleLink.setOnMouseEntered(e -> cloudToggleLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true; -fx-padding: 0;"));
        cloudToggleLink.setOnMouseExited(e -> cloudToggleLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false; -fx-padding: 0;"));
        cloudToggleLink.setOnAction(e -> {
            boolean current = manager.isCloudEnabled();
            manager.setCloudEnabled(!current);
            updateCloudUIState();
            notifyModified();
        });

        HBox localIndicatorBox = new HBox(4, localIndicator, localStatusLabel);
        localIndicatorBox.setAlignment(Pos.CENTER_LEFT);

        processingRow.getChildren().addAll(processingLabel, localIndicatorBox, cloudToggleLink);

        cloudHelpLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        box.getChildren().addAll(processingRow, cloudHelpLabel);
        return box;
    }

    private void updateCloudUIState() {
        boolean cloud = manager.isCloudEnabled();
        if (cloud) {
            localIndicator.setFill(Color.web("#589DF6"));
            localStatusLabel.setText("Lumina AI Cloud Active");
            cloudToggleLink.setText("Disable Cloud");
        } else {
            localIndicator.setFill(Color.web("#62B543"));
            localStatusLabel.setText("Local");
            cloudToggleLink.setText("Enable Cloud");
        }
    }

    private void notifyModified() {
        modifiedProperty.set(manager.isModified());
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void loadData() {
        updatingUI = true;
        try {
            languagesList.setAll(manager.getWorkingLanguages());
            if (!languagesList.isEmpty() && languagesView.getSelectionModel().getSelectedItem() == null) {
                languagesView.getSelectionModel().select(0);
            }
            updateToolbarState();
            updateCloudUIState();
            autoFixCheck.setSelected(manager.isAutoFix());
            oxfordSpellingCheck.setSelected(manager.isOxfordSpelling());
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

    public void setOnNavigateToInspections(Runnable listener) {
        this.onNavigateToInspections = listener;
    }

    public BooleanProperty modifiedProperty() {
        return modifiedProperty;
    }

    // Accessors for testing
    public ListView<NaturalLanguage> getLanguagesView() {
        return languagesView;
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public Hyperlink getCloudToggleLink() {
        return cloudToggleLink;
    }

    public CheckBox getAutoFixCheck() {
        return autoFixCheck;
    }

    public CheckBox getOxfordSpellingCheck() {
        return oxfordSpellingCheck;
    }
}
