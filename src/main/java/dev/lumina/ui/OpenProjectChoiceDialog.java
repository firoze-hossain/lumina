package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * IntelliJ's "Open Project" prompt: shown only when a project is already
 * open in the current window and another one is about to be opened (a
 * newly generated project, an Open Folder pick, a git clone, or a Recent
 * Projects entry). When no project is open yet, callers skip this dialog
 * entirely and open directly in the current window \u2014 exactly like IntelliJ.
 */
public final class OpenProjectChoiceDialog {

    public enum Choice { CANCEL, NEW_WINDOW, THIS_WINDOW }

    private OpenProjectChoiceDialog() {
    }

    /**
     * Show the prompt. onChoice receives the pick and, if "Don't ask again"
     * was checked, whether to remember it for next time (callers persist
     * that via Settings.OPEN_PROJECT_MODE).
     */
    public static void show(Stage owner, String projectName,
                            java.util.function.BiConsumer<Choice, Boolean> onChoice) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Open Project");
        dialog.setResizable(false);

        Label icon = new Label("\u2753");
        icon.getStyleClass().add("open-project-icon");

        Label header = new Label("Open Project");
        header.getStyleClass().add("open-project-header");

        Label message = new Label("Where would you like to open the project '"
                + projectName + "'?");
        message.getStyleClass().add("open-project-message");
        message.setWrapText(true);

        VBox textBox = new VBox(4, header, message);

        HBox top = new HBox(12, icon, textBox);
        top.setAlignment(Pos.TOP_LEFT);

        CheckBox dontAskAgain = new CheckBox("Don't ask again");
        dontAskAgain.getStyleClass().add("open-project-checkbox");

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setCancelButton(true);

        Button newWindow = new Button("New Window");
        newWindow.getStyleClass().add("dialog-secondary");

        Button thisWindow = new Button("This Window");
        thisWindow.getStyleClass().add("dialog-primary");
        thisWindow.setDefaultButton(true);

        Consumer<Choice> pick = choice -> {
            dialog.close();
            onChoice.accept(choice, dontAskAgain.isSelected());
        };
        cancel.setOnAction(e -> pick.accept(Choice.CANCEL));
        newWindow.setOnAction(e -> pick.accept(Choice.NEW_WINDOW));
        thisWindow.setOnAction(e -> pick.accept(Choice.THIS_WINDOW));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10, cancel, newWindow, thisWindow);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(18, top, dontAskAgain, buttons);
        root.getStyleClass().addAll("app-root", "open-project-dialog");
        root.setPadding(new Insets(20, 22, 18, 22));

        BorderPane wrapper = new BorderPane(root);
        Scene scene = new Scene(wrapper, 420, 170);
        scene.getStylesheets().add(OpenProjectChoiceDialog.class
                .getResource("/css/lumina-dark.css").toExternalForm());
        dialog.setScene(scene);

        dialog.setOnCloseRequest(e -> onChoice.accept(Choice.CANCEL, false));
        dialog.showAndWait();
    }
}
