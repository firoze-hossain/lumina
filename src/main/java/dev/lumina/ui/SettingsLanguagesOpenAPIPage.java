package dev.lumina.ui;

import dev.lumina.openapi.OpenAPISettings;
import dev.lumina.openapi.OpenAPISettingsManager;
import dev.lumina.openapi.RemoteOpenAPISpec;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Languages & Frameworks > OpenAPI Specifications settings page in Lumina IDE.
 * Matching the reference IDE layout with SwaggerHub on-premise address, API key, and remote specifications list.
 */
public class SettingsLanguagesOpenAPIPage extends VBox {

    private final OpenAPISettingsManager manager = OpenAPISettingsManager.getInstance();
    private Runnable onModifiedListener;

    private CheckBox gutterIconsCheckBox;
    private TextField swaggerHubAddressField;
    private PasswordField swaggerHubApiKeyField;

    private Button addButton;
    private Button removeButton;
    private Button refreshButton;

    private TableView<RemoteOpenAPISpec> tableView;
    private ObservableList<RemoteOpenAPISpec> tableItems;

    private OpenAPISettings initialSettings;

    public SettingsLanguagesOpenAPIPage() {
        setSpacing(10);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        OpenAPISettings current = manager.getSettings();

        // 1. Editor Features Section
        HBox editorFeaturesHeader = createSectionHeader("Editor Features");

        gutterIconsCheckBox = new CheckBox("Gutter icons for quick specification edits");
        gutterIconsCheckBox.setSelected(current.isGutterIconsForEdits());
        gutterIconsCheckBox.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");
        gutterIconsCheckBox.selectedProperty().addListener((obs, oldV, newV) -> fireModified());

        // 2. Swagger Hub Section
        HBox swaggerHubHeader = createSectionHeader("Swagger Hub");

        VBox swaggerHubForm = new VBox(6);
        Label addressLabel = new Label("On-Premise Installation Address:");
        addressLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        swaggerHubAddressField = new TextField(current.getSwaggerHubAddress());
        styleTextField(swaggerHubAddressField);
        swaggerHubAddressField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        Hyperlink moreDetailsLink = new Hyperlink("More details \u2197");
        moreDetailsLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");

        Label keyLabel = new Label("SwaggerHub API key:");
        keyLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        swaggerHubApiKeyField = new PasswordField();
        swaggerHubApiKeyField.setText(current.getSwaggerHubApiKey());
        styleTextField(swaggerHubApiKeyField);
        swaggerHubApiKeyField.textProperty().addListener((obs, oldV, newV) -> fireModified());

        Label keyHelpLabel = new Label("Paste API key here to be able to search among private specifications");
        keyHelpLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        swaggerHubForm.getChildren().addAll(
                addressLabel,
                swaggerHubAddressField,
                moreDetailsLink,
                keyLabel,
                swaggerHubApiKeyField,
                keyHelpLabel
        );

        // 3. Remote Specifications Section
        HBox remoteSpecsHeader = createSectionHeader("Remote Specifications");

        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(2, 0, 2, 0));

        addButton = new Button("+");
        addButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8;");
        addButton.setTooltip(new Tooltip("Add Remote Specification"));

        ContextMenu addMenu = new ContextMenu();
        MenuItem urlItem = new MenuItem("\uD83C\uDF10  URL");
        urlItem.setOnAction(e -> promptAddUrl());
        MenuItem swaggerItem = new MenuItem("\uD83D\uDFE2  Swagger Hub");
        swaggerItem.setOnAction(e -> promptAddSwaggerHub());
        addMenu.getItems().addAll(urlItem, swaggerItem);

        addButton.setOnAction(e -> addMenu.show(addButton, javafx.geometry.Side.BOTTOM, 0, 0));

        removeButton = new Button("-");
        removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8;");
        removeButton.setTooltip(new Tooltip("Remove Specification"));
        removeButton.setDisable(true);
        removeButton.setOnAction(e -> {
            int idx = tableView.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                tableItems.remove(idx);
                removeButton.setDisable(tableItems.isEmpty() || tableView.getSelectionModel().getSelectedIndex() < 0);
                fireModified();
            }
        });

        refreshButton = new Button("\uD83D\uDD04"); // 🔄
        refreshButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 8;");
        refreshButton.setTooltip(new Tooltip("Refresh specifications"));

        toolbar.getChildren().addAll(addButton, removeButton, refreshButton);

        tableView = new TableView<>();
        tableView.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-radius: 4; " +
                "-fx-font-size: 13px;"
        );
        tableView.setPrefHeight(200);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        tableItems = FXCollections.observableArrayList();
        for (RemoteOpenAPISpec spec : current.getRemoteSpecifications()) {
            tableItems.add(spec.copy());
        }
        tableView.setItems(tableItems);

        TableColumn<RemoteOpenAPISpec, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(220);

        TableColumn<RemoteOpenAPISpec, String> urlCol = new TableColumn<>("URL or Source");
        urlCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUrlOrSource()));
        urlCol.setPrefWidth(380);

        TableColumn<RemoteOpenAPISpec, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
        typeCol.setPrefWidth(120);

        tableView.getColumns().addAll(nameCol, urlCol, typeCol);

        // Empty state placeholder
        VBox emptyPlaceholder = new VBox(6);
        emptyPlaceholder.setAlignment(Pos.CENTER);

        HBox emptyLine = new HBox(4);
        emptyLine.setAlignment(Pos.CENTER);
        Label emptyPrefix = new Label("No remote specifications added. Add specification ");
        emptyPrefix.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");
        Hyperlink urlLink = new Hyperlink("URL");
        urlLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-underline: true; -fx-padding: 0;");
        urlLink.setOnAction(e -> promptAddUrl());
        Label orLabel = new Label(" or ");
        orLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 13px;");
        Hyperlink swaggerLink = new Hyperlink("search SwaggerHub");
        swaggerLink.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 13px; -fx-underline: true; -fx-padding: 0;");
        swaggerLink.setOnAction(e -> promptAddSwaggerHub());

        emptyLine.getChildren().addAll(emptyPrefix, urlLink, orLabel, swaggerLink);
        emptyPlaceholder.getChildren().add(emptyLine);
        tableView.setPlaceholder(emptyPlaceholder);

        tableView.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            removeButton.setDisable(newV == null || newV.intValue() < 0);
        });

        // 4. Subtext footer
        Label footerNote = new Label("The contents of added remote OpenAPI/Swagger specifications will be available in the Endpoints view and as options for URL completion");
        footerNote.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        getChildren().addAll(
                editorFeaturesHeader,
                gutterIconsCheckBox,
                swaggerHubHeader,
                swaggerHubForm,
                remoteSpecsHeader,
                toolbar,
                tableView,
                footerNote
        );
    }

    private void promptAddUrl() {
        TextInputDialog dialog = new TextInputDialog("https://");
        dialog.setTitle("Add OpenAPI Specification URL");
        dialog.setHeaderText("Enter remote OpenAPI / Swagger specification URL:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(url -> {
            if (!url.isBlank()) {
                String clean = url.trim();
                String name = clean.substring(clean.lastIndexOf('/') + 1);
                if (name.isEmpty() || name.equals("openapi.json") || name.equals("swagger.json")) {
                    name = "Specification";
                }
                tableItems.add(new RemoteOpenAPISpec(name, clean, "URL"));
                fireModified();
            }
        });
    }

    private void promptAddSwaggerHub() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Search SwaggerHub");
        dialog.setHeaderText("Enter specification name or API identifier on SwaggerHub:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(query -> {
            if (!query.isBlank()) {
                tableItems.add(new RemoteOpenAPISpec(query.trim(), "swaggerhub://" + query.trim(), "SwaggerHub"));
                fireModified();
            }
        });
    }

    private HBox createSectionHeader(String titleText) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(6, 0, 4, 0));

        Label label = new Label(titleText);
        label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #393B40;");
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(label, line);
        return box;
    }

    private void styleTextField(TextInputControl tf) {
        tf.setStyle(
                "-fx-background-color: #2B2D30; " +
                "-fx-text-fill: #DFE1E5; " +
                "-fx-border-color: #4E5157; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-font-size: 13px;"
        );
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    private void takeSnapshot() {
        this.initialSettings = getFormSettings();
    }

    private OpenAPISettings getFormSettings() {
        OpenAPISettings s = new OpenAPISettings();
        s.setGutterIconsForEdits(gutterIconsCheckBox.isSelected());
        s.setSwaggerHubAddress(swaggerHubAddressField.getText() != null ? swaggerHubAddressField.getText() : "");
        s.setSwaggerHubApiKey(swaggerHubApiKeyField.getText() != null ? swaggerHubApiKeyField.getText() : "");
        List<RemoteOpenAPISpec> specs = new ArrayList<>();
        for (RemoteOpenAPISpec spec : tableItems) {
            specs.add(spec.copy());
        }
        s.setRemoteSpecifications(specs);
        return s;
    }

    public boolean isModified() {
        return !Objects.equals(getFormSettings(), initialSettings);
    }

    public void apply() {
        manager.setSettings(getFormSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        if (initialSettings != null) {
            gutterIconsCheckBox.setSelected(initialSettings.isGutterIconsForEdits());
            swaggerHubAddressField.setText(initialSettings.getSwaggerHubAddress());
            swaggerHubApiKeyField.setText(initialSettings.getSwaggerHubApiKey());
            tableItems.clear();
            for (RemoteOpenAPISpec spec : initialSettings.getRemoteSpecifications()) {
                tableItems.add(spec.copy());
            }
            removeButton.setDisable(true);
        }
        fireModified();
    }

    public void revertChanges() {
        reset();
    }

    public CheckBox getGutterIconsCheckBox() {
        return gutterIconsCheckBox;
    }

    public TextField getSwaggerHubAddressField() {
        return swaggerHubAddressField;
    }

    public PasswordField getSwaggerHubApiKeyField() {
        return swaggerHubApiKeyField;
    }

    public TableView<RemoteOpenAPISpec> getTableView() {
        return tableView;
    }

    public ObservableList<RemoteOpenAPISpec> getTableItems() {
        return tableItems;
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public Button getRefreshButton() {
        return refreshButton;
    }
}
