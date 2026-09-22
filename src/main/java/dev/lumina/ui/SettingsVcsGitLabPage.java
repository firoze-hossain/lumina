package dev.lumina.ui;

import dev.lumina.git.GitLabAccountManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.util.List;

/**
 * Version Control > GitLab settings page matching IntelliJ IDEA Image 3.
 */
public class SettingsVcsGitLabPage extends VBox {

    private final GitLabAccountManager manager = GitLabAccountManager.getInstance();

    private final Button addBtn = new Button();
    private final Button removeBtn = new Button();
    private final Button defaultBtn = new Button();

    private final ListView<GitLabAccountManager.GitLabAccount> accountsList = new ListView<>();
    private final VBox emptyStateBox = new VBox(6);

    private final CheckBox autoMarkFilesViewedCheck = new CheckBox("Automatically mark opened files as viewed");
    private final CheckBox cloneUsingSshCheck = new CheckBox("Clone using SSH");

    public SettingsVcsGitLabPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 20, 20));
        setSpacing(12);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Toolbar
        HBox toolbar = buildToolbar();

        // 2. Center Content (List + Empty State)
        StackPane centerStack = buildCenterContent();

        // 3. Bottom Options
        VBox bottomOptions = new VBox(8);
        bottomOptions.setPadding(new Insets(8, 0, 0, 0));
        initCheckBox(autoMarkFilesViewedCheck, manager.isAutoMarkFilesAsViewed(), e -> manager.setAutoMarkFilesAsViewed(autoMarkFilesViewedCheck.isSelected()));
        initCheckBox(cloneUsingSshCheck, manager.isCloneUsingSsh(), e -> manager.setCloneUsingSsh(cloneUsingSshCheck.isSelected()));
        bottomOptions.getChildren().addAll(autoMarkFilesViewedCheck, cloneUsingSshCheck);

        getChildren().addAll(toolbar, centerStack, bottomOptions);
        VBox.setVgrow(centerStack, Priority.ALWAYS);

        manager.addListener(this::refreshAccounts);
        refreshAccounts();
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);

        // Plus
        SVGPath plus = new SVGPath();
        plus.setContent("M 6 2 L 6 10 M 2 6 L 10 6");
        styleSvg(plus);
        addBtn.setGraphic(plus);
        styleToolBtn(addBtn, "Add (Alt+Insert)");
        addBtn.setOnAction(e -> onAddAccount());

        // Minus
        SVGPath minus = new SVGPath();
        minus.setContent("M 2 6 L 10 6");
        styleSvg(minus);
        removeBtn.setGraphic(minus);
        styleToolBtn(removeBtn, "Remove (Delete)");
        removeBtn.setOnAction(e -> onRemove());

        // Checkmark
        SVGPath check = new SVGPath();
        check.setContent("M 2 6 L 5 9 L 10 2");
        styleSvg(check);
        defaultBtn.setGraphic(check);
        styleToolBtn(defaultBtn, "Set default account");
        defaultBtn.setOnAction(e -> onSetDefault());

        bar.getChildren().addAll(addBtn, removeBtn, defaultBtn);
        return bar;
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

    private StackPane buildCenterContent() {
        accountsList.setStyle(
                "-fx-background-color: #1E1F22; " +
                "-fx-control-inner-background: #1E1F22; " +
                "-fx-border-color: #393B40; " +
                "-fx-border-width: 1px;"
        );

        accountsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GitLabAccountManager.GitLabAccount item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                StackPane avatarPane = new StackPane();
                Circle bg = new Circle(14);
                bg.setFill(Color.web("#E24329")); // GitLab orange/red
                String initial = (!item.getName().isBlank())
                        ? item.getName().substring(0, 1).toUpperCase()
                        : (!item.getUsername().isBlank() ? item.getUsername().substring(0, 1).toUpperCase() : "G");
                Label monogram = new Label(initial);
                monogram.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                avatarPane.getChildren().addAll(bg, monogram);

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

        // Empty state matching Image 3: "No accounts added" and "Add account... (Alt+Insert)"
        emptyStateBox.setAlignment(Pos.CENTER);
        Label emptyLabel = new Label("No accounts added");
        emptyLabel.setStyle("-fx-text-fill: #6F737A; -fx-font-size: 13px;");

        Hyperlink addLink = new Hyperlink("Add account...");
        addLink.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 13px; -fx-padding: 0; -fx-underline: false;");
        addLink.setOnAction(e -> onAddAccount());

        Label shortcutLabel = new Label("(Alt+Insert)");
        shortcutLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px;");

        HBox addRow = new HBox(4, addLink, shortcutLabel);
        addRow.setAlignment(Pos.CENTER);

        emptyStateBox.getChildren().addAll(emptyLabel, addRow);

        StackPane stack = new StackPane(accountsList, emptyStateBox);
        return stack;
    }

    private void initCheckBox(CheckBox cb, boolean initial, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        cb.setSelected(initial);
        cb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        cb.setOnAction(onAction);
    }

    private void refreshAccounts() {
        List<GitLabAccountManager.GitLabAccount> accounts = manager.getAccounts();
        accountsList.getItems().setAll(accounts);
        boolean empty = accounts.isEmpty();
        emptyStateBox.setVisible(empty);
        emptyStateBox.setManaged(empty);

        boolean hasSel = accountsList.getSelectionModel().getSelectedItem() != null;
        removeBtn.setDisable(!hasSel);
        defaultBtn.setDisable(!hasSel);
    }

    private void onAddAccount() {
        Stage owner = (Stage) getScene().getWindow();
        new AddGitLabAccountDialog(owner).showAndWait();
        refreshAccounts();
    }

    private void onRemove() {
        GitLabAccountManager.GitLabAccount sel = accountsList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            manager.removeAccount(sel);
            refreshAccounts();
        }
    }

    private void onSetDefault() {
        GitLabAccountManager.GitLabAccount sel = accountsList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            manager.setDefaultAccount(sel);
            refreshAccounts();
        }
    }
}
