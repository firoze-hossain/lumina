// SettingsCopyrightPage.java
package dev.lumina.ui;

import dev.lumina.copyright.AddScopeMappingDialog;
import dev.lumina.copyright.CopyrightManager;
import dev.lumina.copyright.CopyrightProfile;
import dev.lumina.copyright.ScopeCopyrightMapping;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings page for Editor > Copyright, faithfully matching IntelliJ IDEA's
 * design (screenshots media_1791263712479.png and media_1791263738908.png).
 * Configures default project copyright and scope-to-copyright profile bindings.
 */
public class SettingsCopyrightPage extends VBox {

    private final ComboBox<String> defaultCopyrightCombo = new ComboBox<>();
    private final TableView<ScopeCopyrightMapping> mappingTable = new TableView<>();

    private final Button addBtn = createToolbarButton("+", "Add mapping");
    private final Button removeBtn = createToolbarButton("—", "Remove mapping");
    private final Button upBtn = createToolbarButton("↑", "Move Up");
    private final Button downBtn = createToolbarButton("↓", "Move Down");

    private final Hyperlink scopesLink = new Hyperlink("Select Scopes to add new scopes or modify existing ones");

    // Working state & snapshots
    private final ObservableList<ScopeCopyrightMapping> workingMappings = FXCollections.observableArrayList();
    private final List<ScopeCopyrightMapping> originalMappings = new ArrayList<>();
    private String workingDefault = CopyrightManager.NO_COPYRIGHT;
    private String originalDefault = CopyrightManager.NO_COPYRIGHT;

    private Runnable onModifiedListener;
    private Runnable onNavigateToScopes;

    public SettingsCopyrightPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        initLayout();
        loadDataFromManager();
    }

    private void initLayout() {
        // ============================================================
        // Top: Default project copyright row
        // ============================================================
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label defaultLabel = new Label("Default project copyright:");
        defaultLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        defaultCopyrightCombo.setStyle(
                "-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 3 8; -fx-font-size: 13px;"
        );
        defaultCopyrightCombo.setPrefWidth(420);
        HBox.setHgrow(defaultCopyrightCombo, Priority.ALWAYS);

        defaultCopyrightCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            workingDefault = newVal != null ? newVal : CopyrightManager.NO_COPYRIGHT;
            notifyModified();
        });

        topRow.getChildren().addAll(defaultLabel, defaultCopyrightCombo);

        // ============================================================
        // Table Toolbar
        // ============================================================
        HBox toolbar = new HBox(2, addBtn, removeBtn, upBtn, downBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 0, 0, 0));

        addBtn.setOnAction(e -> handleAddMapping());
        removeBtn.setOnAction(e -> handleRemoveMapping());
        upBtn.setOnAction(e -> handleMoveUp());
        downBtn.setOnAction(e -> handleMoveDown());

        // ============================================================
        // Scope-to-Copyright Table
        // ============================================================
        mappingTable.setItems(workingMappings);
        mappingTable.setStyle(
                "-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        VBox.setVgrow(mappingTable, Priority.ALWAYS);

        TableColumn<ScopeCopyrightMapping, String> scopeCol = new TableColumn<>("Scope");
        scopeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getScope()));
        scopeCol.setPrefWidth(320);

        TableColumn<ScopeCopyrightMapping, String> copyrightCol = new TableColumn<>("Copyright");
        copyrightCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCopyright()));
        copyrightCol.setPrefWidth(380);

        mappingTable.getColumns().add(scopeCol);
        mappingTable.getColumns().add(copyrightCol);
        mappingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Exact placeholder from screenshot: "Nothing to show"
        Label placeholderLabel = new Label("Nothing to show");
        placeholderLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        mappingTable.setPlaceholder(placeholderLabel);

        mappingTable.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            updateButtonState(newVal.intValue());
        });

        // ============================================================
        // Bottom: Scopes Hyperlink
        // ============================================================
        scopesLink.setStyle(
                "-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; " +
                "-fx-underline: false; -fx-padding: 6 0 0 0;"
        );
        scopesLink.setOnMouseEntered(e -> scopesLink.setStyle(
                "-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; " +
                "-fx-underline: true; -fx-padding: 6 0 0 0;"
        ));
        scopesLink.setOnMouseExited(e -> scopesLink.setStyle(
                "-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; " +
                "-fx-underline: false; -fx-padding: 6 0 0 0;"
        ));
        scopesLink.setOnAction(e -> {
            if (onNavigateToScopes != null) {
                onNavigateToScopes.run();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Scopes");
                alert.setHeaderText("Scope Configuration");
                alert.setContentText("Navigate to Appearance & Behavior > Scopes to configure custom scopes.");
                alert.showAndWait();
            }
        });

        getChildren().addAll(topRow, toolbar, mappingTable, scopesLink);
    }

    private void updateButtonState(int selectedIndex) {
        boolean hasSelected = (selectedIndex >= 0);
        removeBtn.setDisable(!hasSelected);
        upBtn.setDisable(!hasSelected || selectedIndex == 0);
        downBtn.setDisable(!hasSelected || selectedIndex == workingMappings.size() - 1);
    }

    private void handleAddMapping() {
        CopyrightManager manager = CopyrightManager.getInstance();
        List<String> profileNames = new ArrayList<>();
        for (CopyrightProfile p : manager.getProfiles()) {
            profileNames.add(p.getName());
        }
        if (profileNames.isEmpty()) {
            profileNames.add(CopyrightManager.NO_COPYRIGHT);
        }

        List<String> scopes = List.of("All Places", "Project Files", "Production", "Tests", "Scratches and Consoles");

        AddScopeMappingDialog.show(getScene() != null ? getScene().getWindow() : null, scopes, profileNames)
                .ifPresent(mapping -> {
                    workingMappings.add(mapping);
                    mappingTable.getSelectionModel().select(mapping);
                    notifyModified();
                });
    }

    private void handleRemoveMapping() {
        int idx = mappingTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < workingMappings.size()) {
            workingMappings.remove(idx);
            if (!workingMappings.isEmpty()) {
                int next = Math.min(idx, workingMappings.size() - 1);
                mappingTable.getSelectionModel().select(next);
            }
            notifyModified();
        }
    }

    private void handleMoveUp() {
        int idx = mappingTable.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            ScopeCopyrightMapping item = workingMappings.remove(idx);
            workingMappings.add(idx - 1, item);
            mappingTable.getSelectionModel().select(idx - 1);
            notifyModified();
        }
    }

    private void handleMoveDown() {
        int idx = mappingTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < workingMappings.size() - 1) {
            ScopeCopyrightMapping item = workingMappings.remove(idx);
            workingMappings.add(idx + 1, item);
            mappingTable.getSelectionModel().select(idx + 1);
            notifyModified();
        }
    }

    private Button createToolbarButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; " +
                "-fx-padding: 2 6; -fx-cursor: hand;"
        );
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #393B40; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-padding: 2 6; -fx-cursor: hand; -fx-background-radius: 3;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; " +
                "-fx-padding: 2 6; -fx-cursor: hand;"
        ));
        return btn;
    }

    private void loadDataFromManager() {
        CopyrightManager manager = CopyrightManager.getInstance();

        // Populate combo
        List<String> comboItems = new ArrayList<>();
        comboItems.add(CopyrightManager.NO_COPYRIGHT);
        for (CopyrightProfile p : manager.getProfiles()) {
            comboItems.add(p.getName());
        }
        defaultCopyrightCombo.getItems().setAll(comboItems);

        workingDefault = manager.getDefaultProjectCopyright();
        originalDefault = workingDefault;
        defaultCopyrightCombo.getSelectionModel().select(workingDefault);

        // Populate mappings
        workingMappings.clear();
        originalMappings.clear();
        for (ScopeCopyrightMapping m : manager.getScopeMappings()) {
            workingMappings.add(m.copy());
            originalMappings.add(m.copy());
        }

        updateButtonState(-1);
    }

    public boolean isModified() {
        if (!Objects.equals(workingDefault, originalDefault)) {
            return true;
        }

        if (workingMappings.size() != originalMappings.size()) {
            return true;
        }

        for (int i = 0; i < workingMappings.size(); i++) {
            if (!workingMappings.get(i).isEquivalentTo(originalMappings.get(i))) {
                return true;
            }
        }

        return false;
    }

    public void apply() {
        CopyrightManager manager = CopyrightManager.getInstance();

        manager.setDefaultProjectCopyright(workingDefault);
        manager.getScopeMappings().setAll(workingMappings);
        manager.save();

        originalDefault = workingDefault;
        originalMappings.clear();
        for (ScopeCopyrightMapping m : workingMappings) {
            originalMappings.add(m.copy());
        }

        notifyModified();
    }

    public void reset() {
        loadDataFromManager();
        notifyModified();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    public void setOnNavigateToScopes(Runnable listener) {
        this.onNavigateToScopes = listener;
    }

    public void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void addMapping(String scope, String profile) {
        if (scope != null && profile != null) {
            workingMappings.add(new ScopeCopyrightMapping(scope, profile));
            notifyModified();
        }
    }

    public ComboBox<String> getDefaultCopyrightCombo() {
        return defaultCopyrightCombo;
    }

    public TableView<ScopeCopyrightMapping> getMappingTable() {
        return mappingTable;
    }

    public ObservableList<ScopeCopyrightMapping> getWorkingMappings() {
        return workingMappings;
    }
}