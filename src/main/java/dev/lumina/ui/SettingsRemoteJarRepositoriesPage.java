package dev.lumina.ui;

import dev.lumina.build.RemoteJarRepositoriesSettingsManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Settings page for Build, Execution, Deployment > Remote Jar Repositories (Screenshot 1).
 * Matches IntelliJ IDEA 1:1, allowing dynamic management of Maven Jar Repositories
 * and Artifactory/Nexus Service URLs with live connection testing and resetting.
 */
public class SettingsRemoteJarRepositoriesPage extends VBox {

    private final RemoteJarRepositoriesSettingsManager manager = RemoteJarRepositoriesSettingsManager.getInstance();

    // Maven Jar Repositories
    private final ObservableList<String> mavenReposList = FXCollections.observableArrayList();
    private final ListView<String> mavenReposListView = new ListView<>(mavenReposList);
    private final Button addMavenBtn = new Button("Add");
    private final Button editMavenBtn = new Button("Edit");
    private final Button removeMavenBtn = new Button("Remove");
    private final Button resetMavenDefaultsBtn = new Button("Reset Defaults");

    // Artifactory or Nexus Service URLs
    private final ObservableList<String> nexusUrlsList = FXCollections.observableArrayList();
    private final ListView<String> nexusUrlsListView = new ListView<>(nexusUrlsList);
    private final Button addNexusBtn = new Button("Add");
    private final Button editNexusBtn = new Button("Edit");
    private final Button removeNexusBtn = new Button("Remove");
    private final Button testNexusBtn = new Button("Test");
    private final Button resetNexusDefaultsBtn = new Button("Reset Defaults");

    private List<String> initialMavenRepos;
    private List<String> initialNexusUrls;
    private Runnable onModifiedListener;

    public SettingsRemoteJarRepositoriesPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 24, 24, 24));
        setSpacing(24);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildUI();
        loadData();
    }

    private void buildUI() {
        // --- Top Section: Maven Jar Repositories ---
        VBox mavenSection = new VBox(8);
        Label mavenLabel = new Label("Maven Jar Repositories:");
        mavenLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        setupListViewStyle(mavenReposListView);
        mavenReposListView.setPrefHeight(180);

        VBox mavenButtons = new VBox(6);
        styleActionButton(addMavenBtn);
        styleActionButton(editMavenBtn);
        styleActionButton(removeMavenBtn);
        mavenButtons.getChildren().addAll(addMavenBtn, editMavenBtn, removeMavenBtn);

        HBox mavenListAndButtons = new HBox(12, mavenReposListView, mavenButtons);
        HBox.setHgrow(mavenReposListView, Priority.ALWAYS);

        styleActionButton(resetMavenDefaultsBtn);
        HBox resetMavenBox = new HBox(resetMavenDefaultsBtn);
        resetMavenBox.setAlignment(Pos.CENTER_RIGHT);

        mavenSection.getChildren().addAll(mavenLabel, mavenListAndButtons, resetMavenBox);

        // --- Bottom Section: Artifactory or Nexus Service URLs ---
        VBox nexusSection = new VBox(8);
        Label nexusLabel = new Label("Artifactory or Nexus Service URLs:");
        nexusLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        setupListViewStyle(nexusUrlsListView);
        nexusUrlsListView.setPrefHeight(160);

        VBox nexusButtons = new VBox(6);
        styleActionButton(addNexusBtn);
        styleActionButton(editNexusBtn);
        styleActionButton(removeNexusBtn);
        styleActionButton(testNexusBtn);
        nexusButtons.getChildren().addAll(addNexusBtn, editNexusBtn, removeNexusBtn, testNexusBtn);

        HBox nexusListAndButtons = new HBox(12, nexusUrlsListView, nexusButtons);
        HBox.setHgrow(nexusUrlsListView, Priority.ALWAYS);

        styleActionButton(resetNexusDefaultsBtn);
        HBox resetNexusBox = new HBox(resetNexusDefaultsBtn);
        resetNexusBox.setAlignment(Pos.CENTER_RIGHT);

        nexusSection.getChildren().addAll(nexusLabel, nexusListAndButtons, resetNexusBox);

        getChildren().addAll(mavenSection, nexusSection);

        initHandlers();
    }

    private void setupListViewStyle(ListView<String> lv) {
        lv.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");
        lv.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #1E1F22;");
                } else {
                    setText(item);
                    setStyle(isSelected()
                            ? "-fx-background-color: #2E436E; -fx-text-fill: #DFE1E5; -fx-padding: 4 8;"
                            : "-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-padding: 4 8;");
                }
            }
        });
    }

    private void initHandlers() {
        // Selection tracking for button enabling
        mavenReposListView.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            boolean hasSel = nv != null;
            editMavenBtn.setDisable(!hasSel);
            removeMavenBtn.setDisable(!hasSel);
        });
        editMavenBtn.setDisable(true);
        removeMavenBtn.setDisable(true);

        nexusUrlsListView.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            boolean hasSel = nv != null;
            editNexusBtn.setDisable(!hasSel);
            removeNexusBtn.setDisable(!hasSel);
            testNexusBtn.setDisable(!hasSel);
        });
        editNexusBtn.setDisable(true);
        removeNexusBtn.setDisable(true);
        testNexusBtn.setDisable(true);

        // Maven actions
        addMavenBtn.setOnAction(e -> handleAddUrl(mavenReposList, "Add Maven Repository URL", "Enter Maven repository URL:"));
        editMavenBtn.setOnAction(e -> handleEditUrl(mavenReposListView, mavenReposList, "Edit Maven Repository URL"));
        removeMavenBtn.setOnAction(e -> {
            int idx = mavenReposListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                mavenReposList.remove(idx);
                notifyModified();
            }
        });
        resetMavenDefaultsBtn.setOnAction(e -> {
            mavenReposList.clear();
            mavenReposList.addAll(RemoteJarRepositoriesSettingsManager.getDefaultMavenRepositories());
            notifyModified();
        });

        // Nexus actions
        addNexusBtn.setOnAction(e -> handleAddUrl(nexusUrlsList, "Add Service URL", "Enter Artifactory or Nexus service URL:"));
        editNexusBtn.setOnAction(e -> handleEditUrl(nexusUrlsListView, nexusUrlsList, "Edit Service URL"));
        removeNexusBtn.setOnAction(e -> {
            int idx = nexusUrlsListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                nexusUrlsList.remove(idx);
                notifyModified();
            }
        });
        resetNexusDefaultsBtn.setOnAction(e -> {
            nexusUrlsList.clear();
            nexusUrlsList.addAll(RemoteJarRepositoriesSettingsManager.getDefaultArtifactoryNexusUrls());
            notifyModified();
        });

        testNexusBtn.setOnAction(e -> {
            String selected = nexusUrlsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                testNexusBtn.setDisable(true);
                testNexusBtn.setText("Testing...");
                new Thread(() -> {
                    RemoteJarRepositoriesSettingsManager.TestResult res = manager.testServiceUrl(selected);
                    Platform.runLater(() -> {
                        testNexusBtn.setDisable(false);
                        testNexusBtn.setText("Test");

                        Alert alert = new Alert(res.success() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                        alert.setTitle("Repository Connection Test");
                        alert.setHeaderText(res.success() ? "Connection Successful" : "Connection Failed");
                        alert.setContentText(res.message() + "\nURL: " + selected);
                        alert.getDialogPane().setStyle("-fx-background-color: #1E1F22;");
                        alert.showAndWait();
                    });
                }).start();
            }
        });
    }

    private void handleAddUrl(ObservableList<String> list, String title, String prompt) {
        TextInputDialog dialog = new TextInputDialog("https://");
        dialog.setTitle(title);
        dialog.setHeaderText(prompt);
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22;");
        Optional<String> res = dialog.showAndWait();
        res.ifPresent(url -> {
            String trimmed = url.trim();
            if (!trimmed.isBlank() && !list.contains(trimmed)) {
                list.add(trimmed);
                notifyModified();
            }
        });
    }

    private void handleEditUrl(ListView<String> lv, ObservableList<String> list, String title) {
        String curr = lv.getSelectionModel().getSelectedItem();
        int idx = lv.getSelectionModel().getSelectedIndex();
        if (curr != null && idx >= 0) {
            TextInputDialog dialog = new TextInputDialog(curr);
            dialog.setTitle(title);
            dialog.setHeaderText("Edit URL:");
            dialog.getDialogPane().setStyle("-fx-background-color: #1E1F22;");
            Optional<String> res = dialog.showAndWait();
            res.ifPresent(newUrl -> {
                String trimmed = newUrl.trim();
                if (!trimmed.isBlank() && !trimmed.equals(curr)) {
                    list.set(idx, trimmed);
                    notifyModified();
                }
            });
        }
    }

    // --- State Persistence & Lifecycle ---

    public void loadData() {
        initialMavenRepos = manager.getMavenRepositories();
        initialNexusUrls = manager.getArtifactoryNexusUrls();

        mavenReposList.clear();
        mavenReposList.addAll(initialMavenRepos);

        nexusUrlsList.clear();
        nexusUrlsList.addAll(initialNexusUrls);
    }

    public boolean isModified() {
        if (initialMavenRepos == null || initialNexusUrls == null) return false;
        return !Objects.equals(initialMavenRepos, new ArrayList<>(mavenReposList))
                || !Objects.equals(initialNexusUrls, new ArrayList<>(nexusUrlsList));
    }

    public void apply() {
        manager.setMavenRepositories(new ArrayList<>(mavenReposList));
        manager.setArtifactoryNexusUrls(new ArrayList<>(nexusUrlsList));
        initialMavenRepos = manager.getMavenRepositories();
        initialNexusUrls = manager.getArtifactoryNexusUrls();
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

    private void styleActionButton(Button btn) {
        btn.setPrefWidth(110);
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 12; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisabled()) {
                btn.setStyle("-fx-background-color: #35373C; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-border-color: #4C5056; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 12; -fx-cursor: hand;");
            }
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 12; -fx-cursor: hand;");
        });
    }

    // Getters for testing
    public ListView<String> getMavenReposListView() { return mavenReposListView; }
    public ObservableList<String> getMavenReposList() { return mavenReposList; }
    public Button getAddMavenBtn() { return addMavenBtn; }
    public Button getEditMavenBtn() { return editMavenBtn; }
    public Button getRemoveMavenBtn() { return removeMavenBtn; }
    public Button getResetMavenDefaultsBtn() { return resetMavenDefaultsBtn; }

    public ListView<String> getNexusUrlsListView() { return nexusUrlsListView; }
    public ObservableList<String> getNexusUrlsList() { return nexusUrlsList; }
    public Button getAddNexusBtn() { return addNexusBtn; }
    public Button getEditNexusBtn() { return editNexusBtn; }
    public Button getRemoveNexusBtn() { return removeNexusBtn; }
    public Button getTestNexusBtn() { return testNexusBtn; }
    public Button getResetNexusDefaultsBtn() { return resetNexusDefaultsBtn; }
}
