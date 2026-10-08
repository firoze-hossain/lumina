package dev.lumina.ui;

import dev.lumina.debugger.DebuggerSettingsManager;
import dev.lumina.debugger.JavaTypeRenderer;
import dev.lumina.debugger.JavaTypeRendererSettings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Settings page for Build, Execution, Deployment > Debugger > Data Views > Java Type Renderers.
 * Accurately replicates the UI and behavior shown in Image 5.
 */
public class SettingsDebuggerDataViewsTypeRenderersPage extends VBox {

    private final DebuggerSettingsManager manager = DebuggerSettingsManager.getInstance();

    // Left pane list
    private final ObservableList<JavaTypeRenderer> rendererList = FXCollections.observableArrayList();
    private final ListView<JavaTypeRenderer> renderersListView = new ListView<>(rendererList);

    // Right pane form controls
    private final TextField nameField = new TextField();
    private final TextField targetClassField = new TextField();
    private final Button browseClassBtn = new Button("...");

    // When rendering a node
    private final CheckBox showTypeAndObjectIdCheck = new CheckBox("Show type and object id");
    private final ToggleGroup nodeRenderGroup = new ToggleGroup();
    private final RadioButton nodeDefaultRadio = new RadioButton("Use default renderer");
    private final RadioButton nodeExpressionRadio = new RadioButton("Use following expression:");
    private final TextField nodeExpressionField = new TextField();
    private final CheckBox nodeOnDemandCheck = new CheckBox("On-demand");

    // When expanding a node
    private final ToggleGroup expandRenderGroup = new ToggleGroup();
    private final RadioButton expandDefaultRadio = new RadioButton("Use default renderer");
    private final RadioButton expandExpressionRadio = new RadioButton("Use following expression:");
    private final TextField expandExpressionField = new TextField();
    private final Label testExpandLabel = new Label("Test if a node can be expanded (optional):");
    private final TextField testExpandField = new TextField();

    private final RadioButton expandListRadio = new RadioButton("Use list of expressions:");
    private final ObservableList<JavaTypeRenderer.ChildExpressionRule> childList = FXCollections.observableArrayList();
    private final TableView<JavaTypeRenderer.ChildExpressionRule> childTable = new TableView<>(childList);
    private final VBox childTableContainer = new VBox();

    private final CheckBox appendDefaultChildrenCheck = new CheckBox("Append default children");

    private final VBox rightPane = new VBox(10);

    private JavaTypeRendererSettings initialSettings;
    private JavaTypeRenderer activeSelectedRenderer = null;
    private Runnable onModifiedListener;
    private boolean suppressEvents = false;

    public SettingsDebuggerDataViewsTypeRenderersPage() {
        getStyleClass().add("settings-page");
        setStyle("-fx-background-color: #1E1F22;");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(12);

        buildUI();
        loadData();
    }

    private void buildUI() {
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: #1E1F22; -fx-box-border: transparent;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // --- Left Master Pane ---
        VBox leftPane = buildLeftPane();
        leftPane.setPrefWidth(220);
        leftPane.setMinWidth(180);
        leftPane.setMaxWidth(350);

        // --- Right Detail Pane ---
        buildRightPane();
        VBox.setVgrow(rightPane, Priority.ALWAYS);

        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.28);

        getChildren().add(splitPane);
    }

    private VBox buildLeftPane() {
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-padding: 3 6 3 6;");

        Button addBtn = createToolbarButton("+", "Add Renderer");
        Button removeBtn = createToolbarButton("—", "Remove Renderer");
        Button upBtn = createToolbarButton("↑", "Move Up");
        Button downBtn = createToolbarButton("↓", "Move Down");
        Button copyBtn = createToolbarButton("⧉", "Duplicate Renderer");
        Button importBtn = createToolbarButton("📥", "Import");
        Button exportBtn = createToolbarButton("📤", "Export");

        addBtn.setOnAction(e -> {
            commitCurrentToActive();
            JavaTypeRenderer r = new JavaTypeRenderer("unnamed", "java.lang.Object");
            rendererList.add(r);
            renderersListView.getSelectionModel().select(r);
            renderersListView.scrollTo(r);
            checkModified();
        });

        removeBtn.setOnAction(e -> {
            JavaTypeRenderer sel = renderersListView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                int idx = rendererList.indexOf(sel);
                rendererList.remove(sel);
                if (!rendererList.isEmpty()) {
                    int nextIdx = Math.min(idx, rendererList.size() - 1);
                    renderersListView.getSelectionModel().select(nextIdx);
                } else {
                    activeSelectedRenderer = null;
                    rightPane.setVisible(false);
                }
                checkModified();
            }
        });

        upBtn.setOnAction(e -> {
            int idx = renderersListView.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                JavaTypeRenderer item = rendererList.remove(idx);
                rendererList.add(idx - 1, item);
                renderersListView.getSelectionModel().select(idx - 1);
                checkModified();
            }
        });

        downBtn.setOnAction(e -> {
            int idx = renderersListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < rendererList.size() - 1) {
                JavaTypeRenderer item = rendererList.remove(idx);
                rendererList.add(idx + 1, item);
                renderersListView.getSelectionModel().select(idx + 1);
                checkModified();
            }
        });

        copyBtn.setOnAction(e -> {
            JavaTypeRenderer sel = renderersListView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                commitCurrentToActive();
                JavaTypeRenderer copy = sel.clone();
                copy.setId(UUID.randomUUID().toString());
                copy.setName(copy.getName() + " (copy)");
                int idx = renderersListView.getSelectionModel().getSelectedIndex();
                rendererList.add(idx + 1, copy);
                renderersListView.getSelectionModel().select(copy);
                checkModified();
            }
        });

        importBtn.setOnAction(e -> importRenderers());
        exportBtn.setOnAction(e -> exportRenderers());

        toolbar.getChildren().addAll(addBtn, removeBtn, upBtn, downBtn, copyBtn, importBtn, exportBtn);

        renderersListView.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1;");
        VBox.setVgrow(renderersListView, Priority.ALWAYS);

        renderersListView.setCellFactory(lv -> new ListCell<>() {
            private final CheckBox check = new CheckBox();
            private final Label label = new Label();
            private final HBox box = new HBox(8, check, label);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                check.setOnAction(e -> {
                    JavaTypeRenderer item = getItem();
                    if (item != null) {
                        item.setEnabled(check.isSelected());
                        checkModified();
                    }
                });
            }

            @Override
            protected void updateItem(JavaTypeRenderer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    check.setSelected(item.isEnabled());
                    label.setText(item.getName());
                    label.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
                    setGraphic(box);
                }
            }
        });

        renderersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            commitCurrentToActive();
            activeSelectedRenderer = newV;
            loadRendererIntoRightPane(newV);
        });

        VBox left = new VBox(toolbar, renderersListView);
        VBox.setVgrow(left, Priority.ALWAYS);
        return left;
    }

    private void buildRightPane() {
        rightPane.setPadding(new Insets(4, 16, 12, 16));
        rightPane.setStyle("-fx-background-color: #1E1F22;");

        // 1. Renderer Name Row
        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("Renderer name:");
        nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        nameField.setPrefWidth(260);
        styleTextField(nameField);
        nameField.textProperty().addListener((obs, o, n) -> {
            if (activeSelectedRenderer != null && !suppressEvents) {
                activeSelectedRenderer.setName(n);
                renderersListView.refresh();
                checkModified();
            }
        });
        nameRow.getChildren().addAll(nameLabel, nameField);

        // 2. Apply renderer to objects of type
        VBox targetTypeBox = new VBox(6);
        Label targetLabel = new Label("Apply renderer to objects of type (fully qualified name):");
        targetLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        HBox targetRow = new HBox(8);
        targetRow.setAlignment(Pos.CENTER_LEFT);
        styleTextField(targetClassField);
        HBox.setHgrow(targetClassField, Priority.ALWAYS);
        targetClassField.textProperty().addListener((obs, o, n) -> {
            if (activeSelectedRenderer != null && !suppressEvents) {
                activeSelectedRenderer.setTargetClassName(n);
                checkModified();
            }
        });

        browseClassBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-padding: 3 8 3 8;");
        browseClassBtn.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog(targetClassField.getText());
            tid.setTitle("Choose Class");
            tid.setHeaderText("Specify fully qualified class name or wildcard (e.g. java.util.*):");
            tid.showAndWait().ifPresent(cls -> {
                if (!cls.isBlank()) {
                    targetClassField.setText(cls.trim());
                }
            });
        });

        targetRow.getChildren().addAll(targetClassField, browseClassBtn);
        targetTypeBox.getChildren().addAll(targetLabel, targetRow);

        // 3. When rendering a node
        VBox renderNodeBox = new VBox(8);
        renderNodeBox.setPadding(new Insets(8, 0, 4, 0));
        Label renderNodeLabel = new Label("When rendering a node");
        renderNodeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        styleCheckBox(showTypeAndObjectIdCheck);
        showTypeAndObjectIdCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        nodeDefaultRadio.setToggleGroup(nodeRenderGroup);
        nodeExpressionRadio.setToggleGroup(nodeRenderGroup);
        styleRadioButton(nodeDefaultRadio);
        styleRadioButton(nodeExpressionRadio);

        styleTextField(nodeExpressionField);
        HBox.setHgrow(nodeExpressionField, Priority.ALWAYS);
        nodeExpressionField.textProperty().addListener((obs, o, n) -> checkModified());

        styleCheckBox(nodeOnDemandCheck);
        nodeOnDemandCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        HBox exprRow = new HBox(8, nodeExpressionRadio, nodeExpressionField);
        exprRow.setAlignment(Pos.CENTER_LEFT);

        nodeRenderGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            boolean isExpr = nodeExpressionRadio.isSelected();
            nodeExpressionField.setDisable(!isExpr);
            nodeOnDemandCheck.setDisable(!isExpr);
            checkModified();
        });

        VBox.setMargin(showTypeAndObjectIdCheck, new Insets(0, 0, 0, 16));
        VBox.setMargin(nodeDefaultRadio, new Insets(0, 0, 0, 16));
        VBox.setMargin(exprRow, new Insets(0, 0, 0, 16));
        VBox.setMargin(nodeOnDemandCheck, new Insets(0, 0, 0, 36));

        renderNodeBox.getChildren().addAll(renderNodeLabel, showTypeAndObjectIdCheck, nodeDefaultRadio, exprRow, nodeOnDemandCheck);

        // 4. When expanding a node
        VBox expandNodeBox = new VBox(8);
        expandNodeBox.setPadding(new Insets(8, 0, 4, 0));
        Label expandNodeLabel = new Label("When expanding a node");
        expandNodeLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        expandDefaultRadio.setToggleGroup(expandRenderGroup);
        expandExpressionRadio.setToggleGroup(expandRenderGroup);
        expandListRadio.setToggleGroup(expandRenderGroup);
        styleRadioButton(expandDefaultRadio);
        styleRadioButton(expandExpressionRadio);
        styleRadioButton(expandListRadio);

        styleTextField(expandExpressionField);
        HBox.setHgrow(expandExpressionField, Priority.ALWAYS);
        expandExpressionField.textProperty().addListener((obs, o, n) -> checkModified());

        testExpandLabel.setStyle("-fx-text-fill: #8C9199; -fx-font-size: 12px;");
        styleTextField(testExpandField);
        HBox.setHgrow(testExpandField, Priority.ALWAYS);
        testExpandField.textProperty().addListener((obs, o, n) -> checkModified());

        HBox expandExprRow = new HBox(8, expandExpressionRadio, expandExpressionField);
        expandExprRow.setAlignment(Pos.CENTER_LEFT);

        VBox testBox = new VBox(4, testExpandLabel, testExpandField);
        testBox.setPadding(new Insets(0, 0, 0, 20));

        buildChildTableUI();

        expandRenderGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            boolean isExpr = expandExpressionRadio.isSelected();
            boolean isList = expandListRadio.isSelected();

            expandExpressionField.setDisable(!isExpr);
            testExpandLabel.setDisable(!isExpr);
            testExpandField.setDisable(!isExpr);
            childTableContainer.setDisable(!isList);
            checkModified();
        });

        VBox.setMargin(expandDefaultRadio, new Insets(0, 0, 0, 16));
        VBox.setMargin(expandExprRow, new Insets(0, 0, 0, 16));
        VBox.setMargin(testBox, new Insets(0, 0, 0, 16));
        VBox.setMargin(expandListRadio, new Insets(0, 0, 0, 16));
        VBox.setMargin(childTableContainer, new Insets(0, 0, 0, 36));

        expandNodeBox.getChildren().addAll(
                expandNodeLabel,
                expandDefaultRadio,
                expandExprRow,
                testBox,
                expandListRadio,
                childTableContainer
        );

        // 5. Append default children
        styleCheckBox(appendDefaultChildrenCheck);
        appendDefaultChildrenCheck.selectedProperty().addListener((obs, o, n) -> checkModified());

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #1E1F22;");
        VBox innerScroll = new VBox(14, nameRow, targetTypeBox, renderNodeBox, expandNodeBox, appendDefaultChildrenCheck);
        innerScroll.setStyle("-fx-background-color: #1E1F22;");
        sp.setContent(innerScroll);
        VBox.setVgrow(sp, Priority.ALWAYS);

        rightPane.getChildren().add(sp);
    }

    private void buildChildTableUI() {
        HBox toolbar = new HBox(4);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-border-width: 1 1 0 1; -fx-padding: 2 6 2 6;");

        Button addBtn = createToolbarButton("+", "Add child expression");
        Button removeBtn = createToolbarButton("—", "Remove child expression");
        Button upBtn = createToolbarButton("↑", "Move Up");
        Button downBtn = createToolbarButton("↓", "Move Down");

        addBtn.setOnAction(e -> {
            JavaTypeRenderer.ChildExpressionRule rule = new JavaTypeRenderer.ChildExpressionRule("child", "this", false);
            childList.add(rule);
            childTable.getSelectionModel().select(rule);
            checkModified();
        });

        removeBtn.setOnAction(e -> {
            JavaTypeRenderer.ChildExpressionRule sel = childTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                childList.remove(sel);
                checkModified();
            }
        });

        upBtn.setOnAction(e -> {
            int idx = childTable.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                JavaTypeRenderer.ChildExpressionRule item = childList.remove(idx);
                childList.add(idx - 1, item);
                childTable.getSelectionModel().select(idx - 1);
                checkModified();
            }
        });

        downBtn.setOnAction(e -> {
            int idx = childTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < childList.size() - 1) {
                JavaTypeRenderer.ChildExpressionRule item = childList.remove(idx);
                childList.add(idx + 1, item);
                childTable.getSelectionModel().select(idx + 1);
                checkModified();
            }
        });

        toolbar.getChildren().addAll(addBtn, removeBtn, upBtn, downBtn);

        childTable.setEditable(true);
        childTable.setPrefHeight(130);
        childTable.setMaxHeight(150);
        childTable.setPlaceholder(new Label("Nothing to show"));
        childTable.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #393B40; -fx-border-width: 0 1 1 1;");

        TableColumn<JavaTypeRenderer.ChildExpressionRule, String> nameCol = new TableColumn<>("Name");
        nameCol.setPrefWidth(120);
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(event -> {
            event.getRowValue().setName(event.getNewValue());
            checkModified();
        });

        TableColumn<JavaTypeRenderer.ChildExpressionRule, String> exprCol = new TableColumn<>("Expression");
        exprCol.setPrefWidth(180);
        exprCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getExpression()));
        exprCol.setCellFactory(TextFieldTableCell.forTableColumn());
        exprCol.setOnEditCommit(event -> {
            event.getRowValue().setExpression(event.getNewValue());
            checkModified();
        });

        TableColumn<JavaTypeRenderer.ChildExpressionRule, Boolean> demandCol = new TableColumn<>("On-demand");
        demandCol.setPrefWidth(90);
        demandCol.setCellFactory(CheckBoxTableCell.forTableColumn(param -> {
            JavaTypeRenderer.ChildExpressionRule rule = childTable.getItems().get(param);
            SimpleBooleanProperty prop = new SimpleBooleanProperty(rule.isOnDemand());
            prop.addListener((obs, o, n) -> {
                rule.setOnDemand(n);
                checkModified();
            });
            return prop;
        }));

        childTable.getColumns().addAll(nameCol, exprCol, demandCol);

        childTableContainer.getChildren().addAll(toolbar, childTable);
    }

    private Button createToolbarButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373B; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 6 2 6;"));
        return btn;
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8 4 8;");
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void styleRadioButton(RadioButton rb) {
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
    }

    private void loadRendererIntoRightPane(JavaTypeRenderer r) {
        if (r == null) {
            rightPane.setVisible(false);
            return;
        }
        rightPane.setVisible(true);
        suppressEvents = true;

        nameField.setText(r.getName());
        targetClassField.setText(r.getTargetClassName());
        showTypeAndObjectIdCheck.setSelected(r.isShowTypeAndObjectId());

        if (r.getNodeRendererType() == JavaTypeRenderer.NodeRendererType.DEFAULT) {
            nodeDefaultRadio.setSelected(true);
            nodeExpressionField.setDisable(true);
            nodeOnDemandCheck.setDisable(true);
        } else {
            nodeExpressionRadio.setSelected(true);
            nodeExpressionField.setDisable(false);
            nodeOnDemandCheck.setDisable(false);
        }
        nodeExpressionField.setText(r.getNodeExpression());
        nodeOnDemandCheck.setSelected(r.isNodeOnDemand());

        if (r.getExpandRendererType() == JavaTypeRenderer.ExpandRendererType.DEFAULT) {
            expandDefaultRadio.setSelected(true);
            expandExpressionField.setDisable(true);
            testExpandField.setDisable(true);
            childTableContainer.setDisable(true);
        } else if (r.getExpandRendererType() == JavaTypeRenderer.ExpandRendererType.EXPRESSION) {
            expandExpressionRadio.setSelected(true);
            expandExpressionField.setDisable(false);
            testExpandField.setDisable(false);
            childTableContainer.setDisable(true);
        } else {
            expandListRadio.setSelected(true);
            expandExpressionField.setDisable(true);
            testExpandField.setDisable(true);
            childTableContainer.setDisable(false);
        }
        expandExpressionField.setText(r.getExpandExpression());
        testExpandField.setText(r.getTestCanExpandExpression());

        childList.clear();
        for (JavaTypeRenderer.ChildExpressionRule rule : r.getChildExpressions()) {
            childList.add(rule.clone());
        }

        appendDefaultChildrenCheck.setSelected(r.isAppendDefaultChildren());

        suppressEvents = false;
    }

    private void commitCurrentToActive() {
        if (activeSelectedRenderer == null) return;
        activeSelectedRenderer.setName(nameField.getText());
        activeSelectedRenderer.setTargetClassName(targetClassField.getText());
        activeSelectedRenderer.setShowTypeAndObjectId(showTypeAndObjectIdCheck.isSelected());

        activeSelectedRenderer.setNodeRendererType(nodeDefaultRadio.isSelected() ?
                JavaTypeRenderer.NodeRendererType.DEFAULT :
                JavaTypeRenderer.NodeRendererType.EXPRESSION);
        activeSelectedRenderer.setNodeExpression(nodeExpressionField.getText());
        activeSelectedRenderer.setNodeOnDemand(nodeOnDemandCheck.isSelected());

        if (expandDefaultRadio.isSelected()) {
            activeSelectedRenderer.setExpandRendererType(JavaTypeRenderer.ExpandRendererType.DEFAULT);
        } else if (expandExpressionRadio.isSelected()) {
            activeSelectedRenderer.setExpandRendererType(JavaTypeRenderer.ExpandRendererType.EXPRESSION);
        } else {
            activeSelectedRenderer.setExpandRendererType(JavaTypeRenderer.ExpandRendererType.EXPRESSION_LIST);
        }
        activeSelectedRenderer.setExpandExpression(expandExpressionField.getText());
        activeSelectedRenderer.setTestCanExpandExpression(testExpandField.getText());

        List<JavaTypeRenderer.ChildExpressionRule> rules = new ArrayList<>();
        for (JavaTypeRenderer.ChildExpressionRule rule : childList) {
            rules.add(rule.clone());
        }
        activeSelectedRenderer.setChildExpressions(rules);
        activeSelectedRenderer.setAppendDefaultChildren(appendDefaultChildrenCheck.isSelected());
    }

    private void importRenderers() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Java Type Renderers");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        Stage stage = (Stage) getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            try {
                String json = Files.readString(file.toPath());
                boolean ok = manager.importTypeRenderersFromJson(json);
                if (ok) {
                    loadData();
                    checkModified();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void exportRenderers() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Java Type Renderers");
        fc.setInitialFileName("type_renderers.json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        Stage stage = (Stage) getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try {
                commitCurrentToActive();
                String json = manager.exportTypeRenderersToJson();
                Files.writeString(file.toPath(), json);
            } catch (Exception ignored) {
            }
        }
    }

    public void loadData() {
        suppressEvents = true;
        initialSettings = manager.getJavaTypeRendererSettings();

        rendererList.clear();
        for (JavaTypeRenderer r : initialSettings.getRenderers()) {
            rendererList.add(r.clone());
        }

        if (!rendererList.isEmpty()) {
            renderersListView.getSelectionModel().select(0);
            activeSelectedRenderer = rendererList.get(0);
            loadRendererIntoRightPane(activeSelectedRenderer);
        } else {
            activeSelectedRenderer = null;
            rightPane.setVisible(false);
        }

        suppressEvents = false;
        checkModified();
    }

    public JavaTypeRendererSettings getCurrentSettings() {
        commitCurrentToActive();
        JavaTypeRendererSettings s = new JavaTypeRendererSettings();
        List<JavaTypeRenderer> list = new ArrayList<>();
        for (JavaTypeRenderer r : rendererList) {
            list.add(r.clone());
        }
        s.setRenderers(list);
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentSettings());
    }

    public void apply() {
        if (isModified()) {
            initialSettings = getCurrentSettings();
            manager.setJavaTypeRendererSettings(initialSettings);
            checkModified();
        }
    }

    public void reset() {
        loadData();
    }

    public void revert() {
        reset();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void checkModified() {
        if (suppressEvents) return;
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }
}
