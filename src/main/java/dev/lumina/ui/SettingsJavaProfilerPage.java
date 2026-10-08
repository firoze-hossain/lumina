package dev.lumina.ui;

import dev.lumina.profiler.JavaProfilerConfig;
import dev.lumina.profiler.JavaProfilerSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings page for Build, Execution, Deployment > Java Profiler (Screenshot 1).
 * Matches IntelliJ IDEA UI and functionality with master-detail layout,
 * profiler configurations, agent options, file browsing, and native call collection.
 */
public class SettingsJavaProfilerPage extends VBox {

    private final JavaProfilerSettingsManager manager = JavaProfilerSettingsManager.getInstance();

    private final ObservableList<JavaProfilerConfig> profilersList = FXCollections.observableArrayList();
    private final ListView<JavaProfilerConfig> profilerListView = new ListView<>(profilersList);

    // Toolbar buttons
    private final Button addButton = new Button("+");
    private final Button duplicateButton = new Button("📄");
    private final Button removeButton = new Button("—");
    private final Button moveUpButton = new Button("↑");
    private final Button moveDownButton = new Button("↓");

    // Detail form fields
    private final TextField nameField = new TextField();
    private final TextField agentOptionsField = new TextField();
    private final TextField agentPathField = new TextField();
    private final Button browseAgentButton = new Button("📁");
    private final Hyperlink readmeLink = new Hyperlink("Async profiler README.md ↗");
    private final CheckBox collectNativeCallsCheck = new CheckBox("Collect native calls (GC threads, JNI calls, etc.)");

    private List<JavaProfilerConfig> initialProfilers;
    private String initialSelectedId;
    private JavaProfilerConfig currentSelection = null;
    private boolean suppressEvents = false;
    private Runnable onModifiedListener;

    public SettingsJavaProfilerPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 20, 20, 20));
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // --- Left Master Pane ---
        VBox leftPane = new VBox();
        leftPane.setPrefWidth(220);
        leftPane.setMinWidth(180);
        leftPane.setMaxWidth(280);
        leftPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 0 0;");

        HBox toolbar = buildToolbar();
        setupListView();
        VBox.setVgrow(profilerListView, Priority.ALWAYS);

        leftPane.getChildren().addAll(toolbar, profilerListView);

        // --- Right Detail Pane ---
        ScrollPane rightScroll = new ScrollPane();
        rightScroll.setFitToWidth(true);
        rightScroll.setStyle("-fx-background-color: transparent; -fx-background: #1E1F22; -fx-border-color: transparent;");

        VBox rightContent = buildDetailContent();
        rightScroll.setContent(rightContent);

        splitPane.getItems().addAll(leftPane, rightScroll);
        splitPane.setDividerPositions(0.25);

        getChildren().add(splitPane);
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 6, 6, 6));
        toolbar.setStyle("-fx-border-color: #393B40; -fx-border-width: 0 0 1 0;");

        styleToolbarButton(addButton, "Add Profiler Configuration");
        styleToolbarButton(duplicateButton, "Duplicate Selected Profiler");
        styleToolbarButton(removeButton, "Remove Selected Profiler");
        styleToolbarButton(moveUpButton, "Move Up");
        styleToolbarButton(moveDownButton, "Move Down");

        addButton.setOnAction(e -> handleAddProfiler());
        duplicateButton.setOnAction(e -> handleDuplicateProfiler());
        removeButton.setOnAction(e -> handleRemoveProfiler());
        moveUpButton.setOnAction(e -> handleMoveUp());
        moveDownButton.setOnAction(e -> handleMoveDown());

        toolbar.getChildren().addAll(addButton, duplicateButton, removeButton, moveUpButton, moveDownButton);
        return toolbar;
    }

    private void setupListView() {
        profilerListView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent;");
        profilerListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JavaProfilerConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    setText(item.getName());
                    Label icon = new Label("⏱");
                    icon.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px;");
                    setGraphic(icon);
                    setStyle(isSelected()
                            ? "-fx-background-color: #2E436E; -fx-text-fill: #DFE1E5; -fx-padding: 5 8;"
                            : "-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-padding: 5 8;");
                }
            }
        });

        profilerListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (!suppressEvents && newVal != null) {
                commitCurrentForm();
                currentSelection = newVal;
                populateForm(newVal);
                updateToolbarButtons();
            }
        });
    }

    private VBox buildDetailContent() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(10, 24, 20, 24));
        box.setStyle("-fx-background-color: #1E1F22;");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(100);
        col1.setPrefWidth(110);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        // 1. Name
        Label nameLbl = new Label("Name:");
        styleLabel(nameLbl);
        styleTextField(nameField);
        nameField.textProperty().addListener((obs, o, n) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setName(n);
                profilerListView.refresh();
                notifyModified();
            }
        });
        grid.add(nameLbl, 0, 0);
        grid.add(nameField, 1, 0);

        // 2. Agent options
        Label optLbl = new Label("Agent options:");
        styleLabel(optLbl);
        styleTextField(agentOptionsField);
        agentOptionsField.textProperty().addListener((obs, o, n) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setAgentOptions(n);
                notifyModified();
            }
        });
        grid.add(optLbl, 0, 1);
        grid.add(agentOptionsField, 1, 1);

        // 3. Agent path + browse
        Label agentLbl = new Label("Agent:");
        styleLabel(agentLbl);
        styleTextField(agentPathField);
        agentPathField.textProperty().addListener((obs, o, n) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setAgentPath(n);
                notifyModified();
            }
        });

        styleBrowseButton(browseAgentButton);
        browseAgentButton.setOnAction(e -> handleBrowseAgent());

        HBox agentBox = new HBox(8, agentPathField, browseAgentButton);
        HBox.setHgrow(agentPathField, Priority.ALWAYS);
        agentBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(agentLbl, 0, 2);
        grid.add(agentBox, 1, 2);

        // 4. Hyperlink
        readmeLink.setStyle("-fx-text-fill: #3574F0; -fx-underline: true; -fx-border-color: transparent; -fx-padding: 0;");
        readmeLink.setOnAction(e -> openReadmeUrl());

        // 5. Collect native calls checkbox
        styleCheckBox(collectNativeCallsCheck);
        collectNativeCallsCheck.selectedProperty().addListener((obs, o, n) -> {
            if (!suppressEvents && currentSelection != null) {
                currentSelection.setCollectNativeCalls(n);
                notifyModified();
            }
        });

        VBox extraBox = new VBox(12, readmeLink, collectNativeCallsCheck);
        extraBox.setPadding(new Insets(6, 0, 0, 110));

        box.getChildren().addAll(grid, extraBox);
        return box;
    }

    private void handleBrowseAgent() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Java Profiler Agent");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Shared Libraries / JARs (*.so, *.dylib, *.dll, *.jar)", "*.so", "*.dylib", "*.dll", "*.jar"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            agentPathField.setText(file.getAbsolutePath());
        }
    }

    private void openReadmeUrl() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("https://github.com/async-profiler/async-profiler#readme"));
            }
        } catch (Exception ignored) {}
    }

    private void handleAddProfiler() {
        JavaProfilerConfig newCfg = new JavaProfilerConfig("Custom Profiler", "event=cpu,interval=10ms", "Bundled (Version: 4.1)", false);
        profilersList.add(newCfg);
        profilerListView.getSelectionModel().select(newCfg);
        notifyModified();
    }

    private void handleDuplicateProfiler() {
        if (currentSelection != null) {
            JavaProfilerConfig clone = currentSelection.clone();
            clone.setId(java.util.UUID.randomUUID().toString());
            clone.setName(currentSelection.getName() + " (Copy)");
            int idx = profilersList.indexOf(currentSelection);
            if (idx >= 0 && idx < profilersList.size() - 1) {
                profilersList.add(idx + 1, clone);
            } else {
                profilersList.add(clone);
            }
            profilerListView.getSelectionModel().select(clone);
            notifyModified();
        }
    }

    private void handleRemoveProfiler() {
        if (currentSelection != null && profilersList.size() > 1) {
            int idx = profilersList.indexOf(currentSelection);
            profilersList.remove(currentSelection);
            int nextIdx = Math.min(idx, profilersList.size() - 1);
            profilerListView.getSelectionModel().select(nextIdx);
            notifyModified();
        }
    }

    private void handleMoveUp() {
        int idx = profilerListView.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            JavaProfilerConfig item = profilersList.remove(idx);
            profilersList.add(idx - 1, item);
            profilerListView.getSelectionModel().select(idx - 1);
            notifyModified();
        }
    }

    private void handleMoveDown() {
        int idx = profilerListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < profilersList.size() - 1) {
            JavaProfilerConfig item = profilersList.remove(idx);
            profilersList.add(idx + 1, item);
            profilerListView.getSelectionModel().select(idx + 1);
            notifyModified();
        }
    }

    private void populateForm(JavaProfilerConfig cfg) {
        suppressEvents = true;
        try {
            if (cfg != null) {
                nameField.setText(cfg.getName() != null ? cfg.getName() : "");
                agentOptionsField.setText(cfg.getAgentOptions() != null ? cfg.getAgentOptions() : "");
                agentPathField.setText(cfg.getAgentPath() != null ? cfg.getAgentPath() : "");
                collectNativeCallsCheck.setSelected(cfg.isCollectNativeCalls());
                setFormDisabled(false);
            } else {
                nameField.clear();
                agentOptionsField.clear();
                agentPathField.clear();
                collectNativeCallsCheck.setSelected(false);
                setFormDisabled(true);
            }
        } finally {
            suppressEvents = false;
        }
    }

    private void commitCurrentForm() {
        if (currentSelection != null) {
            currentSelection.setName(nameField.getText());
            currentSelection.setAgentOptions(agentOptionsField.getText());
            currentSelection.setAgentPath(agentPathField.getText());
            currentSelection.setCollectNativeCalls(collectNativeCallsCheck.isSelected());
        }
    }

    private void setFormDisabled(boolean disabled) {
        nameField.setDisable(disabled);
        agentOptionsField.setDisable(disabled);
        agentPathField.setDisable(disabled);
        browseAgentButton.setDisable(disabled);
        readmeLink.setDisable(disabled);
        collectNativeCallsCheck.setDisable(disabled);
    }

    private void updateToolbarButtons() {
        int idx = profilerListView.getSelectionModel().getSelectedIndex();
        int size = profilersList.size();
        removeButton.setDisable(size <= 1);
        duplicateButton.setDisable(currentSelection == null);
        moveUpButton.setDisable(idx <= 0);
        moveDownButton.setDisable(idx < 0 || idx >= size - 1);
    }

    public void loadData() {
        suppressEvents = true;
        try {
            initialProfilers = manager.getProfilers();
            initialSelectedId = manager.getSelectedProfilerId();

            profilersList.clear();
            for (JavaProfilerConfig c : initialProfilers) {
                profilersList.add(c.clone());
            }

            JavaProfilerConfig sel = null;
            if (initialSelectedId != null) {
                for (JavaProfilerConfig c : profilersList) {
                    if (initialSelectedId.equals(c.getId())) {
                        sel = c;
                        break;
                    }
                }
            }
            if (sel == null && !profilersList.isEmpty()) {
                sel = profilersList.get(0);
            }

            currentSelection = sel;
            profilerListView.getSelectionModel().select(sel);
            populateForm(sel);
            updateToolbarButtons();
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        commitCurrentForm();
        if (initialProfilers == null || initialProfilers.size() != profilersList.size()) {
            return true;
        }
        for (int i = 0; i < initialProfilers.size(); i++) {
            if (!Objects.equals(initialProfilers.get(i), profilersList.get(i))) {
                return true;
            }
        }
        if (currentSelection != null && !Objects.equals(initialSelectedId, currentSelection.getId())) {
            return true;
        }
        return false;
    }

    public void apply() {
        commitCurrentForm();
        manager.setProfilers(new ArrayList<>(profilersList));
        if (currentSelection != null) {
            manager.setSelectedProfilerId(currentSelection.getId());
        }
        initialProfilers = manager.getProfilers();
        initialSelectedId = manager.getSelectedProfilerId();
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

    private void styleLabel(Label lbl) {
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-prompt-text-fill: #6F737A; -fx-font-size: 13px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 8;");
    }

    private void styleBrowseButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    // Getters for testing
    public ListView<JavaProfilerConfig> getProfilerListView() { return profilerListView; }
    public TextField getNameField() { return nameField; }
    public TextField getAgentOptionsField() { return agentOptionsField; }
    public TextField getAgentPathField() { return agentPathField; }
    public CheckBox getCollectNativeCallsCheck() { return collectNativeCallsCheck; }
    public Button getAddButton() { return addButton; }
    public Button getDuplicateButton() { return duplicateButton; }
    public Button getRemoveButton() { return removeButton; }
    public Button getMoveUpButton() { return moveUpButton; }
    public Button getMoveDownButton() { return moveDownButton; }
}
