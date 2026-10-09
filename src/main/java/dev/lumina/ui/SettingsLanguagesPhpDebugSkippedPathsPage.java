package dev.lumina.ui;

import dev.lumina.php.PhpDebugSettings;
import dev.lumina.php.PhpSettingsManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings page for Languages & Frameworks > PHP > Debug > Skipped Paths.
 * Faithfully matches Image 5.
 */
public class SettingsLanguagesPhpDebugSkippedPathsPage extends VBox {

    private final PhpSettingsManager manager = PhpSettingsManager.getInstance();

    private CheckBox notifySkippedFilesCheck;
    private final ObservableList<String> pathsList = FXCollections.observableArrayList();
    private ListView<String> pathsListView;

    private boolean initialNotify = true;
    private List<String> initialPaths = new ArrayList<>();

    private Runnable onModifiedListener;

    public SettingsLanguagesPhpDebugSkippedPathsPage() {
        setSpacing(10);
        setPadding(new Insets(16, 22, 20, 22));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildControls();
        loadFromManager();
        takeSnapshot();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void buildControls() {
        // 1. Notify checkbox
        notifySkippedFilesCheck = new CheckBox("Notify of skipped files");
        notifySkippedFilesCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        notifySkippedFilesCheck.selectedProperty().addListener((obs, ov, nv) -> notifyModified());

        // 2. Toolbar (+ / —)
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(3, 6, 3, 6));
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-border-radius: 4 4 0 0; -fx-background-radius: 4 4 0 0;");

        Button addBtn = createToolbarButton("+", "Add skipped path", this::handleAddPath);
        Button removeBtn = createToolbarButton("—", "Remove selected path", this::handleRemovePath);
        toolbar.getChildren().addAll(addBtn, removeBtn);

        // 3. Header Label
        Label tableHeader = new Label("Skipped paths");
        tableHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8; -fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1;");
        tableHeader.setMaxWidth(Double.MAX_VALUE);

        // 4. ListView with custom cells containing path field and browse button
        pathsListView = new ListView<>(pathsList);
        pathsListView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #3574F0; -fx-border-width: 1; -fx-border-radius: 0 0 4 4;");
        VBox.setVgrow(pathsListView, Priority.ALWAYS);

        pathsListView.setCellFactory(lv -> new ListCell<>() {
            private final TextField pathField = new TextField();
            private final Button browseBtn = new Button("📁");
            private final HBox row = new HBox(8, pathField, browseBtn);

            {
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(pathField, Priority.ALWAYS);
                pathField.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-border-color: transparent; -fx-font-size: 13px;");
                browseBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9DA0A8; -fx-cursor: hand; -fx-padding: 2 6;");

                pathField.textProperty().addListener((obs, ov, nv) -> {
                    int index = getIndex();
                    if (index >= 0 && index < pathsList.size() && !nv.equals(pathsList.get(index))) {
                        pathsList.set(index, nv);
                        notifyModified();
                    }
                });

                browseBtn.setOnAction(e -> {
                    Window win = getScene() != null ? getScene().getWindow() : null;
                    SelectPathDialog dlg = new SelectPathDialog(win, pathField.getText());
                    String chosen = dlg.showAndWait();
                    if (chosen != null && !chosen.isBlank()) {
                        pathField.setText(chosen);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    pathField.setText(item);
                    setGraphic(row);
                    setText(null);
                    if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E;");
                    } else {
                        setStyle("-fx-background-color: transparent;");
                    }
                }
            }
        });

        // Wrap list in container
        VBox tableContainer = new VBox(toolbar, tableHeader, pathsListView);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        getChildren().addAll(notifySkippedFilesCheck, tableContainer);
    }

    private Button createToolbarButton(String text, String tooltip, Runnable action) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #393B40; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand; -fx-background-radius: 3;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;"));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void handleAddPath() {
        Window win = getScene() != null ? getScene().getWindow() : null;
        SelectPathDialog dlg = new SelectPathDialog(win, "");
        String chosen = dlg.showAndWait();
        if (chosen != null && !chosen.isBlank()) {
            pathsList.add(chosen);
        } else {
            pathsList.add("");
        }
        pathsListView.getSelectionModel().select(pathsList.size() - 1);
        notifyModified();
    }

    private void handleRemovePath() {
        int selected = pathsListView.getSelectionModel().getSelectedIndex();
        if (selected >= 0 && selected < pathsList.size()) {
            pathsList.remove(selected);
            notifyModified();
        }
    }

    public void loadFromManager() {
        PhpDebugSettings s = manager.getDebugSettings();
        notifySkippedFilesCheck.setSelected(s.isNotifySkippedFiles());
        pathsList.setAll(s.getSkippedPaths());
        if (pathsList.isEmpty()) {
            // Add a single empty row as shown in Image 5
            pathsList.add("");
        }
    }

    public void takeSnapshot() {
        this.initialNotify = notifySkippedFilesCheck.isSelected();
        this.initialPaths = new ArrayList<>(pathsList);
    }

    public boolean isModified() {
        return notifySkippedFilesCheck.isSelected() != initialNotify
                || !pathsList.equals(initialPaths);
    }

    public void apply() {
        PhpDebugSettings s = manager.getDebugSettings();
        s.setNotifySkippedFiles(notifySkippedFilesCheck.isSelected());
        List<String> cleanPaths = new ArrayList<>();
        for (String p : pathsList) {
            if (p != null && !p.isBlank()) {
                cleanPaths.add(p.trim());
            }
        }
        s.setSkippedPaths(cleanPaths);
        manager.setDebugSettings(s);
        takeSnapshot();
        notifyModified();
    }

    public void reset() {
        loadFromManager();
        takeSnapshot();
        notifyModified();
    }

    public void revertChanges() {
        notifySkippedFilesCheck.setSelected(initialNotify);
        pathsList.setAll(initialPaths);
        notifyModified();
    }

    // Getters for testing
    public CheckBox getNotifySkippedFilesCheck() {
        return notifySkippedFilesCheck;
    }

    public ObservableList<String> getPathsList() {
        return pathsList;
    }
}
