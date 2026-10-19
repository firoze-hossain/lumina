package dev.lumina.ui;

import dev.lumina.settings.SystemSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Undecorated dark-themed exit confirmation dialog matching IntelliJ IDEA.
 */
public final class ConfirmExitDialog extends Stage {

    private boolean confirmed = false;
    private double dragOffsetX;
    private double dragOffsetY;

    public static boolean showAndWait(Window owner) {
        ConfirmExitDialog dialog = new ConfirmExitDialog(owner);
        dialog.showAndWait();
        return dialog.confirmed;
    }

    private ConfirmExitDialog(Window owner) {
        if (owner != null) {
            initOwner(owner);
        }
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);

        VBox card = new VBox(14);
        card.setPrefWidth(370);
        card.setMaxWidth(390);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setStyle("""
            -fx-background-color: #2B2D30;
            -fx-background-radius: 8;
            -fx-border-color: #43454A;
            -fx-border-radius: 8;
            -fx-border-width: 1;
        """);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.65));
        shadow.setRadius(24);
        shadow.setOffsetY(6);
        card.setEffect(shadow);

        // Header: Blue Question Icon + Title + Spacer + Close Button
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // Circular blue question mark badge
        StackPane iconBadge = new StackPane();
        iconBadge.setMinSize(22, 22);
        iconBadge.setMaxSize(22, 22);
        iconBadge.setStyle("""
            -fx-background-color: #3574F0;
            -fx-background-radius: 11;
        """);
        Label qMark = new Label("?");
        qMark.setStyle("""
            -fx-text-fill: #FFFFFF;
            -fx-font-size: 13px;
            -fx-font-weight: bold;
            -fx-font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        """);
        iconBadge.getChildren().add(qMark);

        Label title = new Label("Confirm Exit");
        title.setStyle("""
            -fx-text-fill: #DFE1E5;
            -fx-font-size: 13px;
            -fx-font-weight: bold;
            -fx-font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        """);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #7A7E85;
            -fx-font-size: 11px;
            -fx-padding: 2 6;
            -fx-cursor: hand;
        """);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("""
            -fx-background-color: #393B40;
            -fx-text-fill: #FFFFFF;
            -fx-font-size: 11px;
            -fx-padding: 2 6;
            -fx-background-radius: 4;
            -fx-cursor: hand;
        """));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #7A7E85;
            -fx-font-size: 11px;
            -fx-padding: 2 6;
            -fx-cursor: hand;
        """));
        closeBtn.setOnAction(e -> close());

        header.getChildren().addAll(iconBadge, title, spacer, closeBtn);

        // Body message
        Label message = new Label("Are you sure you want to exit?");
        message.setStyle("""
            -fx-text-fill: #BCBEC4;
            -fx-font-size: 13px;
            -fx-padding: 2 0 0 32;
            -fx-font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        """);

        // Checkbox: "Don't ask again"
        CheckBox dontAskCheck = new CheckBox("Don't ask again");
        dontAskCheck.setStyle("""
            -fx-text-fill: #DFE1E5;
            -fx-font-size: 12px;
            -fx-padding: 4 0 0 32;
            -fx-cursor: hand;
        """);

        // Footer buttons: Exit (Primary Blue) + Cancel
        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setStyle("-fx-padding: 10 0 0 0;");

        Button exitBtn = new Button("Exit");
        exitBtn.setDefaultButton(true);
        exitBtn.setStyle("""
            -fx-background-color: #3574F0;
            -fx-text-fill: #FFFFFF;
            -fx-font-weight: bold;
            -fx-font-size: 12px;
            -fx-padding: 5 18;
            -fx-background-radius: 4;
            -fx-cursor: hand;
        """);
        exitBtn.setOnMouseEntered(e -> exitBtn.setStyle("""
            -fx-background-color: #3069DA;
            -fx-text-fill: #FFFFFF;
            -fx-font-weight: bold;
            -fx-font-size: 12px;
            -fx-padding: 5 18;
            -fx-background-radius: 4;
            -fx-cursor: hand;
        """));
        exitBtn.setOnMouseExited(e -> exitBtn.setStyle("""
            -fx-background-color: #3574F0;
            -fx-text-fill: #FFFFFF;
            -fx-font-weight: bold;
            -fx-font-size: 12px;
            -fx-padding: 5 18;
            -fx-background-radius: 4;
            -fx-cursor: hand;
        """));
        exitBtn.setOnAction(e -> {
            if (dontAskCheck.isSelected()) {
                SystemSettings.getInstance().setConfirmExit(false);
                SystemSettings.getInstance().save();
            }
            confirmed = true;
            close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("""
            -fx-background-color: #2B2D30;
            -fx-border-color: #4E5157;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-text-fill: #DFE1E5;
            -fx-font-size: 12px;
            -fx-padding: 5 18;
            -fx-cursor: hand;
        """);
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle("""
            -fx-background-color: #35373C;
            -fx-border-color: #5E626A;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-text-fill: #FFFFFF;
            -fx-font-size: 12px;
            -fx-padding: 5 18;
            -fx-cursor: hand;
        """));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle("""
            -fx-background-color: #2B2D30;
            -fx-border-color: #4E5157;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-text-fill: #DFE1E5;
            -fx-font-size: 12px;
            -fx-padding: 5 18;
            -fx-cursor: hand;
        """));
        cancelBtn.setOnAction(e -> close());

        buttonBox.getChildren().addAll(exitBtn, cancelBtn);

        card.getChildren().addAll(header, message, dontAskCheck, buttonBox);

        // Window drag handlers on header
        header.setOnMousePressed((MouseEvent event) -> {
            dragOffsetX = event.getSceneX();
            dragOffsetY = event.getSceneY();
        });
        header.setOnMouseDragged((MouseEvent event) -> {
            setX(event.getScreenX() - dragOffsetX);
            setY(event.getScreenY() - dragOffsetY);
        });

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            } else if (event.getCode() == KeyCode.ENTER) {
                exitBtn.fire();
            }
        });

        setScene(scene);
    }
}
