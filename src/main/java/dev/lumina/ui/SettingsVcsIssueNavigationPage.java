package dev.lumina.ui;

import dev.lumina.git.IssueNavigationManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.util.List;

/**
 * Version Control > Issue Navigation settings page matching IntelliJ IDEA Image 1.
 */
public class SettingsVcsIssueNavigationPage extends VBox {

    private final IssueNavigationManager manager = IssueNavigationManager.getInstance();

    private final TableView<IssueNavigationManager.IssueNavigationLink> table = new TableView<>();
    private final TableColumn<IssueNavigationManager.IssueNavigationLink, String> issueCol = new TableColumn<>("Issue");
    private final TableColumn<IssueNavigationManager.IssueNavigationLink, String> linkCol = new TableColumn<>("Link");

    private final Button addBtn = new Button();
    private final Button editBtn = new Button();
    private final Button removeBtn = new Button();

    public SettingsVcsIssueNavigationPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 14, 20));
        setSpacing(10);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Description label matching Image 1
        Label desc = new Label("IntelliJ IDEA will search for the specified patterns in check-in comments and link them to issues in your issue tracker");
        desc.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");
        desc.setWrapText(true);

        // 2. Toolbar
        HBox toolbar = buildToolbar();

        // 3. TableView
        buildTable();

        getChildren().addAll(desc, toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        manager.addListener(this::refreshTable);
        refreshTable();
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);

        // Plus with context menu
        SVGPath plus = new SVGPath();
        plus.setContent("M 6 2 L 6 10 M 2 6 L 10 6");
        styleSvg(plus);
        addBtn.setGraphic(plus);
        styleToolBtn(addBtn, "Add (Alt+Insert)");

        ContextMenu addMenu = new ContextMenu();
        addMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-padding: 4 0 4 0;");

        MenuItem youTrackItem = createMenuItem("Add YouTrack Pattern");
        youTrackItem.setOnAction(e -> onAddYouTrack());

        MenuItem jiraItem = createMenuItem("Add JIRA Pattern");
        jiraItem.setOnAction(e -> onAddJira());

        MenuItem customItem = createMenuItem("Add Issue Navigation Link");
        customItem.setOnAction(e -> onAddCustom());

        addMenu.getItems().addAll(youTrackItem, jiraItem, customItem);
        addBtn.setOnAction(e -> addMenu.show(addBtn, Side.BOTTOM, 0, 2));

        // Edit pencil
        SVGPath edit = new SVGPath();
        edit.setContent("M 2 8.5 L 2 10 L 3.5 10 L 9 4.5 L 7.5 3 Z M 8 2 L 9.5 3.5");
        styleSvg(edit);
        editBtn.setGraphic(edit);
        styleToolBtn(editBtn, "Edit (Enter)");
        editBtn.setOnAction(e -> onEdit());

        // Minus
        SVGPath minus = new SVGPath();
        minus.setContent("M 2 6 L 10 6");
        styleSvg(minus);
        removeBtn.setGraphic(minus);
        styleToolBtn(removeBtn, "Remove (Delete)");
        removeBtn.setOnAction(e -> onRemove());

        bar.getChildren().addAll(addBtn, editBtn, removeBtn);
        return bar;
    }

    private MenuItem createMenuItem(String text) {
        MenuItem item = new MenuItem(text);
        item.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-padding: 6 16 6 16;");
        return item;
    }

    private void styleSvg(SVGPath p) {
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web("#848BA3"));
        p.setStrokeWidth(1.2);
    }

    private void styleToolBtn(Button btn, String tooltip) {
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 4 6 4 6; -fx-cursor: hand; -fx-background-radius: 4;");
        btn.setTooltip(new Tooltip(tooltip));
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2B2D30; -fx-padding: 4 6 4 6; -fx-cursor: hand; -fx-background-radius: 4;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-padding: 4 6 4 6; -fx-cursor: hand; -fx-background-radius: 4;"));
    }

    private void buildTable() {
        table.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-table-cell-border-color: transparent; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-width: 1px;"
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        issueCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getIssuePattern()));
        issueCol.setPrefWidth(220);
        issueCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                }
            }
        });

        linkCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLinkUrl()));
        linkCol.setPrefWidth(400);
        linkCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 12px; -fx-padding: 4 8 4 8;");
                }
            }
        });

        table.getColumns().addAll(issueCol, linkCol);

        // Placeholder matching Image 1 & 2: "No patterns configured"
        Label placeholder = new Label("No patterns configured");
        placeholder.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");
        table.setPlaceholder(placeholder);

        table.setRowFactory(tv -> {
            TableRow<IssueNavigationManager.IssueNavigationLink> row = new TableRow<>() {
                @Override
                protected void updateItem(IssueNavigationManager.IssueNavigationLink item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("-fx-background-color: transparent;");
                    } else if (isSelected()) {
                        setStyle("-fx-background-color: #2E436E; -fx-text-fill: #FFFFFF;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-text-fill: #DFE1E5;");
                    }
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && (!row.isEmpty())) {
                    onEdit();
                }
            });
            return row;
        });

        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                onRemove();
            } else if (e.getCode() == KeyCode.ENTER) {
                onEdit();
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean hasSel = (newV != null);
            editBtn.setDisable(!hasSel);
            removeBtn.setDisable(!hasSel);
        });

        editBtn.setDisable(true);
        removeBtn.setDisable(true);
    }

    private void refreshTable() {
        List<IssueNavigationManager.IssueNavigationLink> links = manager.getLinks();
        table.getItems().setAll(links);
        boolean hasSel = table.getSelectionModel().getSelectedItem() != null;
        editBtn.setDisable(!hasSel);
        removeBtn.setDisable(!hasSel);
    }

    private void onAddYouTrack() {
        Stage owner = (Stage) getScene().getWindow();
        IssueNavigationPatternDialog dlg = new IssueNavigationPatternDialog(owner, IssueNavigationPatternDialog.DialogMode.YOUTRACK, null);
        dlg.showAndWait();
        refreshTable();
    }

    private void onAddJira() {
        Stage owner = (Stage) getScene().getWindow();
        IssueNavigationPatternDialog dlg = new IssueNavigationPatternDialog(owner, IssueNavigationPatternDialog.DialogMode.JIRA, null);
        dlg.showAndWait();
        refreshTable();
    }

    private void onAddCustom() {
        Stage owner = (Stage) getScene().getWindow();
        IssueNavigationPatternDialog dlg = new IssueNavigationPatternDialog(owner, IssueNavigationPatternDialog.DialogMode.CUSTOM_ADD, null);
        dlg.showAndWait();
        refreshTable();
    }

    private void onEdit() {
        IssueNavigationManager.IssueNavigationLink sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Stage owner = (Stage) getScene().getWindow();
        IssueNavigationPatternDialog dlg = new IssueNavigationPatternDialog(owner, IssueNavigationPatternDialog.DialogMode.CUSTOM_EDIT, sel);
        dlg.showAndWait();
        refreshTable();
    }

    private void onRemove() {
        IssueNavigationManager.IssueNavigationLink sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        manager.removeLink(sel);
        refreshTable();
    }
}
