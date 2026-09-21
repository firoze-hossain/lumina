package dev.lumina.ui;

import dev.lumina.notification.Notification;
import dev.lumina.notification.NotificationAction;
import dev.lumina.notification.NotificationDisplayType;
import dev.lumina.notification.NotificationGroup;
import dev.lumina.notification.NotificationService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Floating Balloon Notification Popup matching IntelliJ IDEA.
 * Renders in the bottom-right corner of the IDE window with smooth fade animations.
 */
public class NotificationBalloonToast extends VBox {

    private final FadeTransition fadeOut = new FadeTransition(Duration.millis(300), this);
    private final PauseTransition delay = new PauseTransition(Duration.seconds(6));

    public NotificationBalloonToast(Notification notification, Runnable onOpenToolWindow, Runnable onDismiss) {
        setPrefWidth(350);
        setMaxWidth(380);
        setSpacing(4);
        setPadding(new Insets(10, 12, 10, 12));

        setStyle("""
            -fx-background-color: #2B2D30;
            -fx-border-color: #43454A;
            -fx-border-width: 1;
            -fx-background-radius: 6;
            -fx-border-radius: 6;
        """);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.45));
        shadow.setRadius(10);
        shadow.setOffsetY(3);
        setEffect(shadow);

        // Header: Icon + Title + Spacer + Close Button
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Node icon = notification.getType().createIcon(16);

        Label title = new Label(notification.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 0 2 0 4;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 0 2 0 4;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #848BA3; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 0 2 0 4;"));
        closeBtn.setOnAction(e -> dismiss(onDismiss));

        topRow.getChildren().addAll(icon, title, closeBtn);
        getChildren().add(topRow);

        // Content
        if (!notification.getContent().isBlank()) {
            Label content = new Label(notification.getContent());
            content.setWrapText(true);
            content.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 12px; -fx-padding: 2 0 0 24;");
            getChildren().add(content);
        }

        // Action links
        if (!notification.getActions().isEmpty()) {
            HBox actionsBox = new HBox(12);
            actionsBox.setAlignment(Pos.CENTER_LEFT);
            actionsBox.setStyle("-fx-padding: 4 0 0 24;");

            for (NotificationAction action : notification.getActions()) {
                Hyperlink link = new Hyperlink(action.text());
                link.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 11px; -fx-padding: 0; -fx-underline: false;");
                link.setOnAction(e -> {
                    dismiss(onDismiss);
                    try {
                        action.action().run();
                    } catch (Exception ignored) {}
                });
                actionsBox.getChildren().add(link);
            }
            getChildren().add(actionsBox);
        }

        // Clicking card body opens tool window
        setOnMouseClicked(e -> {
            if (e.getTarget() != closeBtn && !(e.getTarget() instanceof Hyperlink)) {
                if (onOpenToolWindow != null) {
                    onOpenToolWindow.run();
                }
            }
        });

        // Determine if sticky
        NotificationGroup group = NotificationService.getInstance().getGroup(notification.getGroupId());
        boolean isSticky = group != null && group.getDisplayType() == NotificationDisplayType.STICKY_BALLOON;

        // Fade in
        setOpacity(0.0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        // Auto dismiss for non-sticky balloons
        if (!isSticky) {
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                if (onDismiss != null) onDismiss.run();
            });

            delay.setOnFinished(e -> fadeOut.play());
            delay.play();

            // Pause auto-dismiss on mouse hover
            setOnMouseEntered(e -> delay.pause());
            setOnMouseExited(e -> delay.play());
        }
    }

    private void dismiss(Runnable onDismiss) {
        delay.stop();
        fadeOut.stop();
        FadeTransition fastFade = new FadeTransition(Duration.millis(150), this);
        fastFade.setFromValue(getOpacity());
        fastFade.setToValue(0.0);
        fastFade.setOnFinished(e -> {
            if (onDismiss != null) onDismiss.run();
        });
        fastFade.play();
    }
}
