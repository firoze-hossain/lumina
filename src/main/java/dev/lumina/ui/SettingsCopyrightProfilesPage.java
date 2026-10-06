// SettingsCopyrightProfilesPage.java
package dev.lumina.ui;

import dev.lumina.copyright.CopyrightManager;
import dev.lumina.copyright.CopyrightProfile;
import dev.lumina.copyright.CreateCopyrightProfileDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings page for Editor > Copyright > Copyright Profiles, faithfully matching
 * IntelliJ IDEA's design (screenshots media_1791263777140.png, media_1791263800981.png, media_1791263819945.png).
 * Manages copyright notice templates, keyword detection, and Local/Shared storage.
 */
public class SettingsCopyrightProfilesPage extends VBox {

    // Master list (left)
    private final ListView<CopyrightProfile> profileListView = new ListView<>();
    private final Button addBtn = createToolbarButton("+", "Add Profile");
    private final Button removeBtn = createToolbarButton("—", "Remove Profile");
    private final Button copyBtn = createToolbarButton("⧉", "Copy Profile");
    private final Button importBtn = createToolbarButton("↙", "Import Profile");

    // Detail pane (right)
    private final StackPane detailContainer = new StackPane();
    private final Label emptyPlaceholder = new Label("Select a profile to view or edit its details here");
    private final VBox editorForm = new VBox(12);

    private final TextField nameField = new TextField();
    private final TextArea noticeArea = new TextArea();
    private final TextField keywordField = new TextField("Copyright");
    private final CheckBox allowReplacingCheck = new CheckBox("Allow replacing copyright if different");
    private final CheckBox sharedCheck = new CheckBox("Shared (save in project)");

    // Working state & original snapshot
    private final ObservableList<CopyrightProfile> workingProfiles = FXCollections.observableArrayList();
    private final List<CopyrightProfile> originalProfiles = new ArrayList<>();

    private CopyrightProfile currentlyEditing = null;
    private boolean isUpdatingFields = false;
    private Runnable onModifiedListener;

    public SettingsCopyrightProfilesPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        initLayout();
        loadDataFromManager();
    }

    private void initLayout() {
        HBox mainSplit = new HBox(16);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        // ---- Left: Master Profiles Panel ----
        VBox leftPanel = new VBox(8);
        leftPanel.setPrefWidth(280);
        leftPanel.setMinWidth(240);
        leftPanel.setMaxWidth(340);
        VBox.setVgrow(leftPanel, Priority.ALWAYS);

        // Toolbar
        HBox toolbar = new HBox(2, addBtn, removeBtn, copyBtn, importBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 4, 0));

        setupAddButtonMenu();

        removeBtn.setOnAction(e -> handleRemoveProfile());
        copyBtn.setOnAction(e -> handleCopyProfile());
        importBtn.setOnAction(e -> handleImportProfile());

        // Profile List View
        profileListView.setItems(workingProfiles);
        profileListView.setStyle(
                "-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;"
        );
        VBox.setVgrow(profileListView, Priority.ALWAYS);

        // Custom empty placeholder matching Image 3:
        // "No copyright profiles added." and "Add profile (Alt+Insert)" link
        VBox listPlaceholder = new VBox(6);
        listPlaceholder.setAlignment(Pos.CENTER);
        Label noProfilesLabel = new Label("No copyright profiles added.");
        noProfilesLabel.setStyle("-fx-text-fill: #868A91; -fx-font-size: 12px;");

        Hyperlink addLink = new Hyperlink("Add profile (Alt+Insert)");
        addLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false;");
        addLink.setOnMouseEntered(e -> addLink.setStyle("-fx-text-fill: #70B0FF; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: true;"));
        addLink.setOnMouseExited(e -> addLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false;"));
        addLink.setOnAction(e -> showCreateDialog(false));

        listPlaceholder.getChildren().addAll(noProfilesLabel, addLink);
        profileListView.setPlaceholder(listPlaceholder);

        profileListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CopyrightProfile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getName());
                    StackPane icon = createProfileIcon(item.isShared());
                    setGraphic(icon);
                    setGraphicTextGap(8);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8; -fx-background-color: transparent;");
                }
            }
        });

        profileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            updateSelectedProfile(selected);
        });

        leftPanel.getChildren().addAll(toolbar, profileListView);

        // ---- Right: Detail Editor Form ----
        VBox.setVgrow(detailContainer, Priority.ALWAYS);
        HBox.setHgrow(detailContainer, Priority.ALWAYS);

        emptyPlaceholder.setStyle("-fx-text-fill: #868A91; -fx-font-size: 13px;");
        emptyPlaceholder.setAlignment(Pos.CENTER);

        initEditorForm();

        detailContainer.getChildren().addAll(emptyPlaceholder, editorForm);
        showDetailForm(false);

        mainSplit.getChildren().addAll(leftPanel, detailContainer);
        getChildren().add(mainSplit);
    }

    private void setupAddButtonMenu() {
        ContextMenu addMenu = new ContextMenu();
        addMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4;");

        MenuItem headerItem = new MenuItem("Add Profile");
        headerItem.setDisable(true);
        headerItem.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px; -fx-font-weight: bold;");

        MenuItem localItem = new MenuItem("Local");
        localItem.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        localItem.setOnAction(e -> showCreateDialog(false));

        MenuItem sharedItem = new MenuItem("Shared");
        sharedItem.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        sharedItem.setOnAction(e -> showCreateDialog(true));

        addMenu.getItems().addAll(headerItem, new SeparatorMenuItem(), localItem, sharedItem);

        addBtn.setOnAction(e -> {
            addMenu.show(addBtn, Side.BOTTOM, 0, 0);
        });
    }

    private void initEditorForm() {
        editorForm.setPadding(new Insets(0, 0, 0, 10));
        VBox.setVgrow(editorForm, Priority.ALWAYS);

        // Name Row
        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        nameLabel.setPrefWidth(120);

        nameField.setStyle(inputStyle());
        nameField.setPrefWidth(300);
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFields && currentlyEditing != null) {
                currentlyEditing.setName(newVal);
                profileListView.refresh();
                notifyModified();
            }
        });
        nameRow.getChildren().addAll(nameLabel, nameField);

        // Notice Row
        VBox noticeBox = new VBox(6);
        VBox.setVgrow(noticeBox, Priority.ALWAYS);
        Label noticeLabel = new Label("Copyright notice:");
        noticeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        noticeArea.setStyle(
                "-fx-control-inner-background: #1E1F22; -fx-background-color: #1E1F22; " +
                "-fx-text-fill: #DFE1E5; -fx-font-family: 'JetBrains Mono', Consolas, monospace; " +
                "-fx-font-size: 12px; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 8;"
        );
        VBox.setVgrow(noticeArea, Priority.ALWAYS);
        noticeArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFields && currentlyEditing != null) {
                currentlyEditing.setNotice(newVal);
                notifyModified();
            }
        });

        Label varsHint = new Label("Velocity template variables: $today.year, $today.month, $today.day, $project.name, $file.fileName, etc.");
        varsHint.setStyle("-fx-text-fill: #868A91; -fx-font-size: 11px;");

        noticeBox.getChildren().addAll(noticeLabel, noticeArea, varsHint);

        // Keyword row
        HBox keywordRow = new HBox(12);
        keywordRow.setAlignment(Pos.CENTER_LEFT);
        Label keywordLabel = new Label("Keyword to detect copyright:");
        keywordLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        keywordLabel.setPrefWidth(180);

        keywordField.setStyle(inputStyle());
        keywordField.setPrefWidth(240);
        keywordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFields && currentlyEditing != null) {
                currentlyEditing.setKeyword(newVal);
                notifyModified();
            }
        });
        keywordRow.getChildren().addAll(keywordLabel, keywordField);

        // Checkboxes
        allowReplacingCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        allowReplacingCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFields && currentlyEditing != null) {
                currentlyEditing.setAllowReplacing(newVal);
                notifyModified();
            }
        });

        sharedCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        sharedCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFields && currentlyEditing != null) {
                currentlyEditing.setShared(newVal);
                profileListView.refresh();
                notifyModified();
            }
        });

        VBox checksBox = new VBox(8, allowReplacingCheck, sharedCheck);

        editorForm.getChildren().addAll(nameRow, noticeBox, keywordRow, checksBox);
    }

    private void updateSelectedProfile(CopyrightProfile profile) {
        currentlyEditing = profile;
        boolean hasSelected = (profile != null);

        removeBtn.setDisable(!hasSelected);
        copyBtn.setDisable(!hasSelected);

        showDetailForm(hasSelected);

        if (hasSelected) {
            isUpdatingFields = true;
            nameField.setText(profile.getName());
            noticeArea.setText(profile.getNotice());
            keywordField.setText(profile.getKeyword());
            allowReplacingCheck.setSelected(profile.isAllowReplacing());
            sharedCheck.setSelected(profile.isShared());
            isUpdatingFields = false;
        }
    }

    private void showDetailForm(boolean show) {
        emptyPlaceholder.setVisible(!show);
        emptyPlaceholder.setManaged(!show);
        editorForm.setVisible(show);
        editorForm.setManaged(show);
    }

    private void showCreateDialog(boolean shared) {
        CreateCopyrightProfileDialog.show(getScene() != null ? getScene().getWindow() : null, shared).ifPresent(profile -> {
            workingProfiles.add(profile);
            profileListView.getSelectionModel().select(profile);
            profileListView.scrollTo(profile);
            notifyModified();
        });
    }

    private void handleRemoveProfile() {
        CopyrightProfile selected = profileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int idx = profileListView.getSelectionModel().getSelectedIndex();
            workingProfiles.remove(selected);
            if (!workingProfiles.isEmpty()) {
                int next = Math.min(idx, workingProfiles.size() - 1);
                profileListView.getSelectionModel().select(next);
            }
            notifyModified();
        }
    }

    private void handleCopyProfile() {
        CopyrightProfile selected = profileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            CopyrightProfile copy = selected.copy();
            copy.setName(selected.getName() + "_copy");
            workingProfiles.add(copy);
            profileListView.getSelectionModel().select(copy);
            profileListView.scrollTo(copy);
            notifyModified();
        }
    }

    private void handleImportProfile() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Import Copyright Profile");
        alert.setHeaderText("Import from XML");
        alert.setContentText("Select an exported IntelliJ IDEA or Lumina copyright profile XML file to import.");
        alert.showAndWait();
    }

    private StackPane createProfileIcon(boolean shared) {
        StackPane pane = new StackPane();
        pane.setPrefSize(16, 16);
        pane.setMinSize(16, 16);
        pane.setMaxSize(16, 16);

        Circle circle = new Circle(8, shared ? Color.web("#3574F0") : Color.web("#5C616C"));
        Label label = new Label("©");
        label.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");

        pane.getChildren().addAll(circle, label);
        return pane;
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

        workingProfiles.clear();
        originalProfiles.clear();
        for (CopyrightProfile p : manager.getProfiles()) {
            workingProfiles.add(p.copy());
            originalProfiles.add(p.copy());
        }

        if (!workingProfiles.isEmpty()) {
            profileListView.getSelectionModel().select(0);
        } else {
            updateSelectedProfile(null);
        }
    }

    public boolean isModified() {
        if (workingProfiles.size() != originalProfiles.size()) {
            return true;
        }

        for (int i = 0; i < workingProfiles.size(); i++) {
            if (!workingProfiles.get(i).isEquivalentTo(originalProfiles.get(i))) {
                return true;
            }
        }

        return false;
    }

    public void apply() {
        CopyrightManager manager = CopyrightManager.getInstance();

        manager.getProfiles().setAll(workingProfiles);
        manager.save();

        originalProfiles.clear();
        for (CopyrightProfile p : workingProfiles) {
            originalProfiles.add(p.copy());
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

    public void notifyModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public ListView<CopyrightProfile> getProfileListView() {
        return profileListView;
    }

    public ObservableList<CopyrightProfile> getWorkingProfiles() {
        return workingProfiles;
    }

    public TextField getNameField() {
        return nameField;
    }

    public TextArea getNoticeArea() {
        return noticeArea;
    }

    private static String inputStyle() {
        return "-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-padding: 4 8; -fx-font-size: 12px;";
    }
}