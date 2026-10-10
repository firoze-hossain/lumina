package dev.lumina.ui;

import dev.lumina.tools.WebBrowserEntry;
import dev.lumina.tools.WebBrowsersSettings;
import dev.lumina.tools.WebBrowsersSettingsManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings UI page for Tools > Web Browsers and Preview in Lumina IDE.
 * 1:1 visual match with reference screenshot layout.
 */
public class SettingsToolsWebBrowsersPage extends VBox {

    private final ObservableList<WebBrowserEntry> browsersList = FXCollections.observableArrayList();
    private final TableView<WebBrowserEntry> tableView;

    private final ComboBox<String> defaultBrowserCombo;
    private final TextField customBrowserPathField;

    private final CheckBox popupHtmlCheck;
    private final CheckBox popupXmlCheck;

    private final ComboBox<String> reloadBrowserCombo;
    private final ComboBox<String> reloadPreviewCombo;

    private final TextField serverPortField;
    private final CheckBox acceptExternalCheck;
    private final CheckBox allowUnsignedCheck;

    private Runnable onModified;
    private boolean suppressEvents = false;

    public SettingsToolsWebBrowsersPage() {
        setSpacing(12);
        setPadding(new Insets(16, 20, 20, 20));
        setStyle("-fx-background-color: #1E1F22;");

        // Action Toolbar: +, −, ✏, ↑, ↓, 📋
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 4, 0));

        Button addBtn = createToolbarButton("+", "Add Browser", this::addBrowser);
        Button removeBtn = createToolbarButton("−", "Remove Browser", this::removeBrowser);
        Button editBtn = createToolbarButton("✏", "Edit Browser", this::editBrowser);
        Button upBtn = createToolbarButton("↑", "Move Up", this::moveUp);
        Button downBtn = createToolbarButton("↓", "Move Down", this::moveDown);
        Button copyBtn = createToolbarButton("📋", "Duplicate Browser", this::copyBrowser);

        toolbar.getChildren().addAll(addBtn, removeBtn, editBtn, upBtn, downBtn, copyBtn);

        // TableView
        tableView = new TableView<>();
        tableView.setItems(browsersList);
        tableView.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");
        tableView.setPrefHeight(190);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<WebBrowserEntry, Boolean> activeCol = new TableColumn<>("");
        activeCol.setCellValueFactory(param -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(param.getValue().isActive());
            prop.addListener((obs, ov, nv) -> {
                param.getValue().setActive(nv);
                notifyModified();
            });
            return prop;
        });
        activeCol.setCellFactory(CheckBoxTableCell.forTableColumn(activeCol));
        activeCol.setPrefWidth(40);
        activeCol.setMaxWidth(45);

        TableColumn<WebBrowserEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(160);

        TableColumn<WebBrowserEntry, String> familyCol = new TableColumn<>("Family");
        familyCol.setCellValueFactory(new PropertyValueFactory<>("family"));
        familyCol.setCellFactory(col -> new TableCell<WebBrowserEntry, String>() {
            @Override
            protected void updateItem(String family, boolean empty) {
                super.updateItem(family, empty);
                if (empty || family == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String icon = switch (family) {
                        case "Firefox" -> "🦊";
                        case "Safari" -> "🧭";
                        case "Opera" -> "🔴";
                        case "Internet Explorer" -> "🌐";
                        default -> "🌐";
                    };
                    setText(icon + "  " + family);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                }
            }
        });
        familyCol.setPrefWidth(160);

        TableColumn<WebBrowserEntry, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(new PropertyValueFactory<>("path"));
        pathCol.setPrefWidth(260);

        tableView.getColumns().addAll(activeCol, nameCol, familyCol, pathCol);

        // Default Browser Row
        Label defaultBrowserLabel = new Label("Default Browser:");
        defaultBrowserLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 120px;");

        defaultBrowserCombo = new ComboBox<>();
        defaultBrowserCombo.getItems().addAll("System default", "First listed", "Chrome", "Firefox");
        defaultBrowserCombo.setValue("System default");
        styleComboBox(defaultBrowserCombo);
        defaultBrowserCombo.setPrefWidth(140);

        customBrowserPathField = new TextField();
        styleTextField(customBrowserPathField);
        HBox.setHgrow(customBrowserPathField, Priority.ALWAYS);

        Button browseCustomBtn = new Button("📁");
        styleIconButton(browseCustomBtn);
        browseCustomBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Custom Browser Executable");
            File chosen = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (chosen != null) {
                customBrowserPathField.setText(chosen.getAbsolutePath());
                notifyModified();
            }
        });

        HBox defaultBrowserRow = new HBox(8, defaultBrowserLabel, defaultBrowserCombo, customBrowserPathField, browseCustomBtn);
        defaultBrowserRow.setAlignment(Pos.CENTER_LEFT);

        // Section: Show browser popup in the editor
        HBox popupHeader = createSectionHeader("Show browser popup in the editor");

        popupHtmlCheck = new CheckBox("For HTML files");
        styleCheckBox(popupHtmlCheck);
        popupHtmlCheck.setSelected(true);

        popupXmlCheck = new CheckBox("For XML files");
        styleCheckBox(popupXmlCheck);
        popupXmlCheck.setSelected(false);

        VBox popupBox = new VBox(6, popupHtmlCheck, popupXmlCheck);
        popupBox.setPadding(new Insets(0, 0, 0, 16));

        // Section: Reload behavior
        HBox reloadHeader = createSectionHeader("Reload behavior");

        Label reloadBrowserLabel = new Label("Reload page in browser:");
        reloadBrowserLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 200px;");
        reloadBrowserCombo = new ComboBox<>();
        reloadBrowserCombo.getItems().addAll("On Save", "Disabled", "Manually");
        reloadBrowserCombo.setValue("On Save");
        styleComboBox(reloadBrowserCombo);
        HBox reloadBrowserRow = new HBox(10, reloadBrowserLabel, reloadBrowserCombo);
        reloadBrowserRow.setAlignment(Pos.CENTER_LEFT);
        reloadBrowserRow.setPadding(new Insets(0, 0, 0, 16));

        Label reloadPreviewLabel = new Label("Reload page in built-in preview:");
        reloadPreviewLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 200px;");
        reloadPreviewCombo = new ComboBox<>();
        reloadPreviewCombo.getItems().addAll("On Save", "Disabled", "Manually");
        reloadPreviewCombo.setValue("On Save");
        styleComboBox(reloadPreviewCombo);
        HBox reloadPreviewRow = new HBox(10, reloadPreviewLabel, reloadPreviewCombo);
        reloadPreviewRow.setAlignment(Pos.CENTER_LEFT);
        reloadPreviewRow.setPadding(new Insets(0, 0, 0, 16));

        // Section: Built-in Server
        HBox serverHeader = createSectionHeader("Built-in Server");

        Label portLabel = new Label("Port:");
        portLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-min-width: 40px;");

        serverPortField = new TextField("63342");
        serverPortField.setPrefWidth(70);
        styleTextField(serverPortField);

        Label portHelp = new Label("?");
        portHelp.setStyle("-fx-background-color: #393B40; -fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-padding: 1 5; -fx-background-radius: 8;");
        portHelp.setTooltip(new Tooltip("HTTP port used by Lumina's internal web server for live HTML preview."));

        HBox portRow = new HBox(8, portLabel, serverPortField, portHelp);
        portRow.setAlignment(Pos.CENTER_LEFT);
        portRow.setPadding(new Insets(0, 0, 0, 16));

        acceptExternalCheck = new CheckBox("Can accept external connections");
        styleCheckBox(acceptExternalCheck);
        acceptExternalCheck.setPadding(new Insets(0, 0, 0, 16));

        allowUnsignedCheck = new CheckBox("Allow unsigned requests");
        styleCheckBox(allowUnsignedCheck);
        allowUnsignedCheck.setPadding(new Insets(0, 0, 0, 16));

        getChildren().addAll(
                toolbar,
                tableView,
                defaultBrowserRow,
                popupHeader,
                popupBox,
                reloadHeader,
                reloadBrowserRow,
                reloadPreviewRow,
                serverHeader,
                portRow,
                acceptExternalCheck,
                allowUnsignedCheck
        );

        setupListeners();
        loadSettings();
    }

    private Button createToolbarButton(String text, String tooltip, Runnable action) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltip));
        styleIconButton(btn);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void styleIconButton(Button btn) {
        btn.setStyle("-fx-background-color: #393B40; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8; -fx-cursor: hand;");
    }

    private void styleTextField(TextField field) {
        field.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 4 8;");
    }

    private void styleComboBox(ComboBox<String> combo) {
        combo.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private HBox createSectionHeader(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setStyle("-fx-background-color: #393B40; -fx-min-height: 1; -fx-max-height: 1;");
        HBox.setHgrow(line, Priority.ALWAYS);

        HBox box = new HBox(12, titleLabel, line);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 0, 4, 0));
        return box;
    }

    private void addBrowser() {
        showBrowserDialog(null);
    }

    private void editBrowser() {
        WebBrowserEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showBrowserDialog(selected);
        }
    }

    private void removeBrowser() {
        WebBrowserEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            browsersList.remove(selected);
            notifyModified();
        }
    }

    private void moveUp() {
        int idx = tableView.getSelectionModel().getSelectedIndex();
        if (idx > 0) {
            WebBrowserEntry item = browsersList.remove(idx);
            browsersList.add(idx - 1, item);
            tableView.getSelectionModel().select(idx - 1);
            notifyModified();
        }
    }

    private void moveDown() {
        int idx = tableView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < browsersList.size() - 1) {
            WebBrowserEntry item = browsersList.remove(idx);
            browsersList.add(idx + 1, item);
            tableView.getSelectionModel().select(idx + 1);
            notifyModified();
        }
    }

    private void copyBrowser() {
        WebBrowserEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            WebBrowserEntry copy = selected.clone();
            copy.setName(selected.getName() + " (Copy)");
            browsersList.add(copy);
            tableView.getSelectionModel().select(copy);
            notifyModified();
        }
    }

    private void showBrowserDialog(WebBrowserEntry existing) {
        Dialog<WebBrowserEntry> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Browser" : "Edit Browser");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.setStyle("-fx-background-color: #2B2D30;");

        TextField nameFld = new TextField(existing != null ? existing.getName() : "");
        ComboBox<String> famFld = new ComboBox<>();
        famFld.getItems().addAll("Chrome", "Firefox", "Safari", "Opera", "Internet Explorer");
        famFld.setValue(existing != null ? existing.getFamily() : "Chrome");
        TextField pathFld = new TextField(existing != null ? existing.getPath() : "");

        Button browseBtn = new Button("📁");
        browseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            File chosen = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (chosen != null) pathFld.setText(chosen.getAbsolutePath());
        });

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameFld, 1, 0);
        grid.add(new Label("Family:"), 0, 1);
        grid.add(famFld, 1, 1);
        grid.add(new Label("Path:"), 0, 2);
        grid.add(new HBox(6, pathFld, browseBtn), 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                if (existing != null) {
                    existing.setName(nameFld.getText().trim());
                    existing.setFamily(famFld.getValue());
                    existing.setPath(pathFld.getText().trim());
                    return existing;
                } else {
                    return new WebBrowserEntry(true, nameFld.getText().trim(), famFld.getValue(), pathFld.getText().trim());
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            if (existing == null) {
                browsersList.add(res);
                tableView.getSelectionModel().select(res);
            } else {
                tableView.refresh();
            }
            notifyModified();
        });
    }

    private void setupListeners() {
        defaultBrowserCombo.valueProperty().addListener((o, ov, nv) -> notifyModified());
        customBrowserPathField.textProperty().addListener((o, ov, nv) -> notifyModified());
        popupHtmlCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        popupXmlCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        reloadBrowserCombo.valueProperty().addListener((o, ov, nv) -> notifyModified());
        reloadPreviewCombo.valueProperty().addListener((o, ov, nv) -> notifyModified());
        serverPortField.textProperty().addListener((o, ov, nv) -> notifyModified());
        acceptExternalCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
        allowUnsignedCheck.selectedProperty().addListener((o, ov, nv) -> notifyModified());
    }

    public void setOnModified(Runnable onModified) {
        this.onModified = onModified;
    }

    private void notifyModified() {
        if (!suppressEvents && onModified != null) {
            onModified.run();
        }
    }

    public void loadSettings() {
        suppressEvents = true;
        try {
            WebBrowsersSettings s = WebBrowsersSettingsManager.getInstance().getSettings();
            browsersList.clear();
            for (WebBrowserEntry b : s.getBrowsers()) {
                browsersList.add(b.clone());
            }
            defaultBrowserCombo.setValue(s.getDefaultBrowser());
            customBrowserPathField.setText(s.getCustomBrowserPath());
            popupHtmlCheck.setSelected(s.isShowPopupForHtml());
            popupXmlCheck.setSelected(s.isShowPopupForXml());
            reloadBrowserCombo.setValue(s.getReloadInBrowser());
            reloadPreviewCombo.setValue(s.getReloadInBuiltInPreview());
            serverPortField.setText(String.valueOf(s.getBuiltInServerPort()));
            acceptExternalCheck.setSelected(s.isCanAcceptExternalConnections());
            allowUnsignedCheck.setSelected(s.isAllowUnsignedRequests());
        } finally {
            suppressEvents = false;
        }
    }

    public boolean isModified() {
        WebBrowsersSettings saved = WebBrowsersSettingsManager.getInstance().getSettings();
        if (browsersList.size() != saved.getBrowsers().size()) return true;
        for (int i = 0; i < browsersList.size(); i++) {
            if (!Objects.equals(browsersList.get(i), saved.getBrowsers().get(i))) return true;
        }
        if (!Objects.equals(defaultBrowserCombo.getValue(), saved.getDefaultBrowser())) return true;
        if (!Objects.equals(customBrowserPathField.getText().trim(), saved.getCustomBrowserPath())) return true;
        if (popupHtmlCheck.isSelected() != saved.isShowPopupForHtml()) return true;
        if (popupXmlCheck.isSelected() != saved.isShowPopupForXml()) return true;
        if (!Objects.equals(reloadBrowserCombo.getValue(), saved.getReloadInBrowser())) return true;
        if (!Objects.equals(reloadPreviewCombo.getValue(), saved.getReloadInBuiltInPreview())) return true;
        if (parsePort() != saved.getBuiltInServerPort()) return true;
        if (acceptExternalCheck.isSelected() != saved.isCanAcceptExternalConnections()) return true;
        return allowUnsignedCheck.isSelected() != saved.isAllowUnsignedRequests();
    }

    public void apply() {
        WebBrowsersSettings s = new WebBrowsersSettings();
        List<WebBrowserEntry> copies = new ArrayList<>();
        for (WebBrowserEntry b : browsersList) {
            copies.add(b.clone());
        }
        s.setBrowsers(copies);
        s.setDefaultBrowser(defaultBrowserCombo.getValue());
        s.setCustomBrowserPath(customBrowserPathField.getText().trim());
        s.setShowPopupForHtml(popupHtmlCheck.isSelected());
        s.setShowPopupForXml(popupXmlCheck.isSelected());
        s.setReloadInBrowser(reloadBrowserCombo.getValue());
        s.setReloadInBuiltInPreview(reloadPreviewCombo.getValue());
        s.setBuiltInServerPort(parsePort());
        s.setCanAcceptExternalConnections(acceptExternalCheck.isSelected());
        s.setAllowUnsignedRequests(allowUnsignedCheck.isSelected());

        WebBrowsersSettingsManager.getInstance().setSettings(s);
    }

    public void reset() {
        loadSettings();
    }

    public void revertChanges() {
        reset();
    }

    private int parsePort() {
        try {
            int p = Integer.parseInt(serverPortField.getText().trim());
            return p > 0 ? p : 63342;
        } catch (NumberFormatException e) {
            return 63342;
        }
    }

    public ObservableList<WebBrowserEntry> getBrowsersList() {
        return browsersList;
    }

    public TableView<WebBrowserEntry> getTableView() {
        return tableView;
    }

    public CheckBox getPopupHtmlCheck() {
        return popupHtmlCheck;
    }

    public TextField getServerPortField() {
        return serverPortField;
    }
}
