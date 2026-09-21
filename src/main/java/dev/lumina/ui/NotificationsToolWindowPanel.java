package dev.lumina.ui;

import dev.lumina.notification.Notification;
import dev.lumina.notification.NotificationAction;
import dev.lumina.notification.NotificationService;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

/**
 * IntelliJ IDEA-identical Notifications Tool Window content panel.
 * Displays the "Timeline", "Clear all" action, and notification cards with
 * severity icons, timestamps, message details, and interactive action hyperlinks.
 */
public class NotificationsToolWindowPanel extends BorderPane {

    private final VBox cardsContainer = new VBox();
    private final Label emptyLabel = new Label("No new notifications");

    public NotificationsToolWindowPanel() {
        getStyleClass().add("notifications-tool-window");
        setStyle("-fx-background-color: #1E1F22;");

        // Sub-header bar: Timeline | Clear all
        HBox subHeader = new HBox(8);
        subHeader.setAlignment(Pos.CENTER_LEFT);
        subHeader.setPadding(new Insets(8, 14, 8, 14));
        subHeader.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent #2B2D30 transparent; -fx-border-width: 1;");

        Label timelineLabel = new Label("Timeline");
        timelineLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearAllBtn = new Button("Clear all");
        clearAllBtn.getStyleClass().add("notification-clear-all");
        clearAllBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #589DF6; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0;");
        clearAllBtn.setOnAction(e -> NotificationService.getInstance().clearTimeline());

        subHeader.getChildren().addAll(timelineLabel, spacer, clearAllBtn);
        setTop(subHeader);

        // Scrollable content
        cardsContainer.setFillWidth(true);
        cardsContainer.setStyle("-fx-background-color: #1E1F22;");

        emptyLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 13px;");
        StackPane emptyPane = new StackPane(emptyLabel);
        emptyPane.setPadding(new Insets(40, 20, 40, 20));

        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: transparent;");

        setCenter(scroll);

        // Populate and listen to timeline
        rebuildCards();
        NotificationService.getInstance().getTimeline().addListener((ListChangeListener<Notification>) c -> rebuildCards());
    }

    private void rebuildCards() {
        cardsContainer.getChildren().clear();
        var timeline = NotificationService.getInstance().getTimeline();

        if (timeline.isEmpty()) {
            cardsContainer.getChildren().add(new StackPane(emptyLabel));
            return;
        }

        for (Notification notification : timeline) {
            cardsContainer.getChildren().add(createCard(notification));
        }
    }

    private Node createCard(Notification notification) {
        VBox card = new VBox(4);
        card.getStyleClass().add("notification-card");
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent #2B2D30 transparent; -fx-border-width: 1;");

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #26282D; -fx-border-color: transparent transparent #2B2D30 transparent; -fx-border-width: 1;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #1E1F22; -fx-border-color: transparent transparent #2B2D30 transparent; -fx-border-width: 1;"));

        // Row 1: Icon + Title + Spacer + Timestamp
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Node iconNode = notification.getType().createIcon(16);

        Label titleLabel = new Label(notification.getTitle());
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label timeLabel = new Label(notification.getFormattedTime());
        timeLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

        topRow.getChildren().addAll(iconNode, titleLabel, timeLabel);
        card.getChildren().add(topRow);

        // Row 2: Subtitle / Content (indented 24px to align with title text)
        if (!notification.getContent().isBlank()) {
            Label contentLabel = new Label(notification.getContent());
            contentLabel.setWrapText(true);
            contentLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-padding: 0 0 0 24;");
            card.getChildren().add(contentLabel);
        }

        // Row 3: Action Hyperlinks (indented 24px)
        if (!notification.getActions().isEmpty()) {
            HBox actionsBox = new HBox(12);
            actionsBox.setAlignment(Pos.CENTER_LEFT);
            actionsBox.setStyle("-fx-padding: 3 0 0 24;");

            for (NotificationAction action : notification.getActions()) {
                Hyperlink link = new Hyperlink(action.text());
                link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-padding: 0; -fx-border-color: transparent; -fx-underline: false;");
                link.setOnAction(e -> {
                    try {
                        action.action().run();
                    } catch (Exception ignored) {}
                });
                actionsBox.getChildren().add(link);
            }
            card.getChildren().add(actionsBox);
        }

        return card;
    }
}
