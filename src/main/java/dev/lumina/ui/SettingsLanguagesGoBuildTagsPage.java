package dev.lumina.ui;

import dev.lumina.go.GoSettings;
import dev.lumina.go.GoSettingsManager;
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
 * Settings page for Languages & Frameworks > Go > Build Tags.
 * Faithfully matches Image 1.
 */
public class SettingsLanguagesGoBuildTagsPage extends HBox {

    private final GoSettingsManager manager = GoSettingsManager.getInstance();

    private final ObservableList<String> scopesList = FXCollections.observableArrayList();
    private ListView<String> scopesListView;
    private StackPane leftContainer;

    private TextField customTagsField;
    private ComboBox<String> osTargetCombo;
    private ComboBox<String> archTargetCombo;
    private VBox rightDetailPane;

    private List<String> initialScopes = new ArrayList<>();
    private String initialCustomTags = "";
    private String initialOsTarget = "any";
    private String initialArchTarget = "any";

    private Runnable onModified;

    public SettingsLanguagesGoBuildTagsPage() {
        setSpacing(16);
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
        // Left side: tall bordered container matching Image 1
        leftContainer = new StackPane();
        leftContainer.setPrefWidth(220);
        leftContainer.setMinWidth(180);
        leftContainer.setMaxWidth(260);
        leftContainer.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 1; -fx-border-radius: 4px;");
        VBox.setVgrow(leftContainer, Priority.ALWAYS);
        HBox.setHgrow(leftContainer, Priority.NEVER);

        Label emptyLabel = new Label("Nothing to show");
        emptyLabel.setStyle("-fx-text-fill: #8C919D; -fx-font-size: 12px;");

        scopesListView = new ListView<>(scopesList);
        scopesListView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        scopesListView.setPlaceholder(emptyLabel);
        scopesListView.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> updateRightPane(nv));

        leftContainer.getChildren().add(scopesListView);

        // Right side: Detail pane (hidden / clean dark area when nothing selected, matching Image 1)
        rightDetailPane = new VBox(12);
        rightDetailPane.setPadding(new Insets(4, 12, 12, 12));
        rightDetailPane.setVisible(false);
        HBox.setHgrow(rightDetailPane, Priority.ALWAYS);

        Label customTagsLabel = new Label("Custom tags:");
        customTagsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        customTagsField = new TextField();
        customTagsField.setPromptText("e.g. linux, integration");
        customTagsField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 13px;");
        customTagsField.textProperty().addListener((obs, ov, nv) -> notifyModified());

        Label osLabel = new Label("OS:");
        osLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        osTargetCombo = new ComboBox<>();
        osTargetCombo.getItems().addAll("any", "linux", "darwin", "windows", "freebsd", "openbsd");
        osTargetCombo.setValue("any");
        osTargetCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 13px;");
        osTargetCombo.valueProperty().addListener((obs, ov, nv) -> notifyModified());

        Label archLabel = new Label("Arch:");
        archLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        archTargetCombo = new ComboBox<>();
        archTargetCombo.getItems().addAll("any", "amd64", "arm64", "386", "arm", "s390x", "wasm");
        archTargetCombo.setValue("any");
        archTargetCombo.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-font-size: 13px;");
        archTargetCombo.valueProperty().addListener((obs, ov, nv) -> notifyModified());

        rightDetailPane.getChildren().addAll(
                customTagsLabel, customTagsField,
                osLabel, osTargetCombo,
                archLabel, archTargetCombo
        );

        getChildren().addAll(leftContainer, rightDetailPane);
    }

    private void updateRightPane(String selectedScope) {
        if (selectedScope == null || selectedScope.isBlank()) {
            rightDetailPane.setVisible(false);
        } else {
            rightDetailPane.setVisible(true);
        }
    }

    public void loadFromManager() {
        GoSettings s = manager.getSettings();
        scopesList.setAll(s.getBuildTagScopes());
        customTagsField.setText(s.getCustomBuildTags());
        osTargetCombo.setValue(s.getOsTarget().isBlank() ? "any" : s.getOsTarget());
        archTargetCombo.setValue(s.getArchTarget().isBlank() ? "any" : s.getArchTarget());

        initialScopes = new ArrayList<>(scopesList);
        initialCustomTags = s.getCustomBuildTags();
        initialOsTarget = osTargetCombo.getValue();
        initialArchTarget = archTargetCombo.getValue();

        if (!scopesList.isEmpty()) {
            scopesListView.getSelectionModel().select(0);
        } else {
            rightDetailPane.setVisible(false);
        }
    }

    public boolean isModified() {
        return !scopesList.equals(initialScopes) ||
                !Objects.equals(customTagsField.getText().trim(), initialCustomTags.trim()) ||
                !Objects.equals(osTargetCombo.getValue(), initialOsTarget) ||
                !Objects.equals(archTargetCombo.getValue(), initialArchTarget);
    }

    public void apply() {
        GoSettings s = manager.getSettings();
        s.setBuildTagScopes(new ArrayList<>(scopesList));
        s.setCustomBuildTags(customTagsField.getText().trim());
        s.setOsTarget(osTargetCombo.getValue());
        s.setArchTarget(archTargetCombo.getValue());
        manager.setSettings(s);

        initialScopes = new ArrayList<>(scopesList);
        initialCustomTags = customTagsField.getText().trim();
        initialOsTarget = osTargetCombo.getValue();
        initialArchTarget = archTargetCombo.getValue();
    }

    public void reset() {
        loadFromManager();
    }

    public void revertChanges() {
        reset();
    }

    public List<String> getScopesList() {
        return new ArrayList<>(scopesList);
    }

    public String getCustomBuildTags() {
        return customTagsField.getText().trim();
    }
}
