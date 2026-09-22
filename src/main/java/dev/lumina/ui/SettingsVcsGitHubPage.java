package dev.lumina.ui;

import dev.lumina.git.GitHubAccountManager;
import dev.lumina.git.GitHubAuth;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

/**
 * Version Control > GitHub settings page matching IntelliJ IDEA Images 1 & 2.
 */
public class SettingsVcsGitHubPage extends VBox {

    private final GitHubAccountManager manager = GitHubAccountManager.getInstance();

    private final Button addBtn = new Button();
    private final Button removeBtn = new Button();
    private final Button defaultBtn = new Button();

    private final ListView<GitHubAccountManager.GitHubAccount> accountsList = new ListView<>();

    private final CheckBox cloneUsingSshCheck = new CheckBox("Clone git repositories using ssh");
    private final CheckBox autoMarkFilesViewedCheck = new CheckBox("Automatically mark opened files as viewed");
    private final CheckBox enableUnreadMarkersCheck = new CheckBox("Enable unread markers on Pull Requests in the list");
    private final TextField timeoutField = new TextField("5");

    public SettingsVcsGitHubPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 20, 20));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Toolbar
        HBox toolbar = buildToolbar();

        // 2. Accounts List
        buildAccountsList();

        // 3. Bottom Options
        VBox bottomOptions = buildBottomOptions();

        getChildren().addAll(toolbar, accountsList, bottomOptions);
        VBox.setVgrow(accountsList, Priority.ALWAYS);

        manager.addListener(this::refreshAccounts);
        refreshAccounts();
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);

        // Plus with ContextMenu matching Image 2
        SVGPath plus = new SVGPath();
        plus.setContent("M 6 2 L 6 10 M 2 6 L 10 6");
        styleSvg(plus);
        addBtn.setGraphic(plus);
        styleToolBtn(addBtn, "Add (Alt+Insert)");

        ContextMenu addMenu = new ContextMenu();
        addMenu.setStyle("-fx-background-color: #2B2D30; -fx-border-color: #393B40; -fx-padding: 4 0 4 0;");

        MenuItem viaBrowserItem = createMenuItem("Log In via GitHub...");
        viaBrowserItem.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(GitHubAuth.tokenUrl()));
                }
            } catch (Exception ignored) {}
            onAddToken(false);
        });

        MenuItem viaTokenItem = createMenuItem("Log In with Token...");
        viaTokenItem.setOnAction(e -> onAddToken(false));

        MenuItem viaEnterpriseItem = createMenuItem("Log In to GitHub Enterprise...");
        viaEnterpriseItem.setOnAction(e -> onAddToken(true));

        addMenu.getItems().addAll(viaBrowserItem, viaTokenItem, viaEnterpriseItem);
        addBtn.setOnAction(e -> addMenu.show(addBtn, Side.BOTTOM, 0, 2));

        // Minus
        SVGPath minus = new SVGPath();
        minus.setContent("M 2 6 L 10 6");
        styleSvg(minus);
        removeBtn.setGraphic(minus);
        styleToolBtn(removeBtn, "Remove (Delete)");
        removeBtn.setOnAction(e -> onRemove());

        // Checkmark (set default)
        SVGPath check = new SVGPath();
        check.setContent("M 2 6 L 5 9 L 10 2");
        styleSvg(check);
        defaultBtn.setGraphic(check);
        styleToolBtn(defaultBtn, "Set default account");
        defaultBtn.setOnAction(e -> onSetDefault());

        bar.getChildren().addAll(addBtn, removeBtn, defaultBtn);
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

    private void buildAccountsList() {
        accountsList.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-width: 1px;"
        );

        accountsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GitHubAccountManager.GitHubAccount item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                // Avatar circular image or monogram
                StackPane avatarPane = new StackPane();
                Circle bg = new Circle(14);
                bg.setFill(Color.web("#393B40"));
                bg.setStroke(Color.web("#5A5D63"));
                bg.setStrokeWidth(1);

                String initial = (!item.getName().isBlank())
                        ? item.getName().substring(0, 1).toUpperCase()
                        : (!item.getUsername().isBlank() ? item.getUsername().substring(0, 1).toUpperCase() : "G");
                Label monogram = new Label(initial);
                monogram.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 11px; -fx-font-weight: bold;");
                avatarPane.getChildren().addAll(bg, monogram);

                // User details matching Image 1: Name, Username, Server
                Label nameLabel = new Label(item.getName().isBlank() ? item.getUsername() : item.getName());
                nameLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");

                Label userLabel = new Label(item.getUsername());
                userLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

                HBox nameRow = new HBox(8, nameLabel, userLabel);
                nameRow.setAlignment(Pos.CENTER_LEFT);

                Label serverLabel = new Label(item.getServer());
                serverLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

                VBox detailsBox = new VBox(2, nameRow, serverLabel);

                HBox card = new HBox(12, avatarPane, detailsBox);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setPadding(new Insets(6, 8, 6, 8));

                setGraphic(card);
                setText(null);

                if (isSelected()) {
                    setStyle("-fx-background-color: #2E436E;");
                } else {
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        accountsList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean hasSel = (newV != null);
            removeBtn.setDisable(!hasSel);
            defaultBtn.setDisable(!hasSel);
        });
        removeBtn.setDisable(true);
        defaultBtn.setDisable(true);
    }

    private VBox buildBottomOptions() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8, 0, 0, 0));

        initCheckBox(cloneUsingSshCheck, manager.isCloneUsingSsh(), e -> manager.setCloneUsingSsh(cloneUsingSshCheck.isSelected()));
        initCheckBox(autoMarkFilesViewedCheck, manager.isAutoMarkFilesAsViewed(), e -> manager.setAutoMarkFilesAsViewed(autoMarkFilesViewedCheck.isSelected()));
        initCheckBox(enableUnreadMarkersCheck, manager.isEnableUnreadMarkersOnPullRequests(), e -> manager.setEnableUnreadMarkersOnPullRequests(enableUnreadMarkersCheck.isSelected()));

        Label timeoutLabel = new Label("Connection timeout:");
        timeoutLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        timeoutField.setText(String.valueOf(manager.getConnectionTimeoutSeconds()));
        timeoutField.setPrefWidth(45);
        timeoutField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 6 3 6; -fx-font-size: 12px;");
        timeoutField.textProperty().addListener((obs, oldV, newV) -> {
            try {
                if (newV != null && !newV.isBlank()) {
                    manager.setConnectionTimeoutSeconds(Integer.parseInt(newV.trim()));
                }
            } catch (Exception ignored) {}
        });

        Label secondsLabel = new Label("seconds");
        secondsLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox timeoutRow = new HBox(8, timeoutLabel, timeoutField, secondsLabel);
        timeoutRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(
                cloneUsingSshCheck, autoMarkFilesViewedCheck, enableUnreadMarkersCheck, timeoutRow
        );
        return box;
    }

    private void initCheckBox(CheckBox cb, boolean initial, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        cb.setSelected(initial);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        cb.setOnAction(onAction);
    }

    private void refreshAccounts() {
        List<GitHubAccountManager.GitHubAccount> accounts = manager.getAccounts();
        accountsList.getItems().setAll(accounts);
        if (!accounts.isEmpty() && accountsList.getSelectionModel().getSelectedItem() == null) {
            accountsList.getSelectionModel().select(0);
        }
    }

    private void onAddToken(boolean enterprise) {
        Stage owner = (Stage) getScene().getWindow();
        new AddGitHubAccountDialog(owner, enterprise).showAndWait();
        refreshAccounts();
    }

    private void onRemove() {
        GitHubAccountManager.GitHubAccount sel = accountsList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            manager.removeAccount(sel);
            refreshAccounts();
        }
    }

    private void onSetDefault() {
        GitHubAccountManager.GitHubAccount sel = accountsList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            manager.setDefaultAccount(sel);
            refreshAccounts();
        }
    }
}
