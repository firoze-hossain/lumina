package dev.lumina.ui;

import dev.lumina.git.IssueNavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.regex.Pattern;

/**
 * Dialogs for configuring Issue Navigation Patterns matching IntelliJ IDEA Images 1 & 2:
 * 1. Add YouTrack Issue Navigation Pattern
 * 2. Add JIRA Issue Navigation Pattern
 * 3. Add / Edit Custom Issue Navigation Link
 */
public class IssueNavigationPatternDialog extends Stage {

    public enum DialogMode {
        YOUTRACK,
        JIRA,
        CUSTOM_ADD,
        CUSTOM_EDIT
    }

    private final DialogMode mode;
    private final IssueNavigationManager.IssueNavigationLink existingLink;
    private boolean confirmed = false;

    public IssueNavigationPatternDialog(Window owner, DialogMode mode, IssueNavigationManager.IssueNavigationLink existingLink) {
        this.mode = mode;
        this.existingLink = existingLink;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(20, 24, 20, 24));
        root.setStyle("-fx-background-color: #2B2D30; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        switch (mode) {
            case YOUTRACK -> buildPresetDialog(root, "Add YouTrack Issue Navigation Pattern", "Enter YouTrack installation URL:", "https://youtrack.jetbrains.com");
            case JIRA -> buildPresetDialog(root, "Add JIRA Issue Navigation Pattern", "Enter JIRA installation URL:", "https://jira.atlassian.com");
            case CUSTOM_ADD, CUSTOM_EDIT -> buildCustomDialog(root);
        }

        Scene scene = new Scene(root);
        setScene(scene);
    }

    private void buildPresetDialog(VBox root, String title, String promptText, String placeholder) {
        setTitle(title);

        // Icon + Prompt & input
        HBox contentBox = new HBox(16);
        contentBox.setAlignment(Pos.TOP_LEFT);

        // ? circular icon (blue matching Image 2)
        Label questionIcon = new Label("?");
        questionIcon.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-min-width: 28px; -fx-min-height: 28px; -fx-max-width: 28px; -fx-max-height: 28px; -fx-background-radius: 14; -fx-alignment: center;");

        VBox formBox = new VBox(8);
        HBox.setHgrow(formBox, Priority.ALWAYS);

        Label promptLabel = new Label(promptText);
        promptLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField urlField = new TextField();
        urlField.setPromptText(placeholder);
        urlField.setPrefWidth(380);
        urlField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10 6 10; -fx-font-size: 13px;");

        formBox.getChildren().addAll(promptLabel, urlField);
        contentBox.getChildren().addAll(questionIcon, formBox);

        // Buttons: OK (blue), Cancel
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 18 6 18; -fx-background-radius: 4; -fx-cursor: hand;");
        okBtn.setDefaultButton(true);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 16 6 16; -fx-font-size: 13px; -fx-cursor: hand;");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(e -> close());

        okBtn.setOnAction(e -> {
            String url = urlField.getText();
            if (url != null && !url.isBlank()) {
                if (mode == DialogMode.YOUTRACK) {
                    IssueNavigationManager.getInstance().addYouTrackPattern(url);
                } else if (mode == DialogMode.JIRA) {
                    IssueNavigationManager.getInstance().addJiraPattern(url);
                }
                confirmed = true;
                close();
            }
        });

        buttonBar.getChildren().addAll(okBtn, cancelBtn);
        root.getChildren().addAll(contentBox, buttonBar);
    }

    private void buildCustomDialog(VBox root) {
        boolean isEdit = (mode == DialogMode.CUSTOM_EDIT);
        setTitle(isEdit ? "Edit Issue Navigation Link" : "Add Issue Navigation Link");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        Label patternLabel = new Label("Issue pattern:");
        patternLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField patternField = new TextField(existingLink != null ? existingLink.getIssuePattern() : "[A-Z]+-\\d+");
        patternField.setPrefWidth(350);
        patternField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10 6 10; -fx-font-size: 13px;");

        Label linkLabel = new Label("Replacement expression:");
        linkLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        TextField linkField = new TextField(existingLink != null ? existingLink.getLinkUrl() : "https://tracker.example.com/browse/$0");
        linkField.setPrefWidth(350);
        linkField.setStyle("-fx-background-color: #1E1F22; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 10 6 10; -fx-font-size: 13px;");

        Label hintLabel = new Label("Use regular expression syntax for Issue pattern and $0, $1... in Replacement expression");
        hintLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ED5E62; -fx-font-size: 11px;");
        errorLabel.setVisible(false);

        grid.add(patternLabel, 0, 0);
        grid.add(patternField, 1, 0);
        grid.add(linkLabel, 0, 1);
        grid.add(linkField, 1, 1);
        grid.add(hintLabel, 1, 2);
        grid.add(errorLabel, 1, 3);

        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 18 6 18; -fx-background-radius: 4; -fx-cursor: hand;");
        okBtn.setDefaultButton(true);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 16 6 16; -fx-font-size: 13px; -fx-cursor: hand;");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(e -> close());

        okBtn.setOnAction(e -> {
            String pat = patternField.getText().trim();
            String link = linkField.getText().trim();

            if (pat.isEmpty()) {
                errorLabel.setText("Issue pattern cannot be empty");
                errorLabel.setVisible(true);
                return;
            }

            try {
                Pattern.compile(pat);
            } catch (Exception ex) {
                errorLabel.setText("Invalid regular expression: " + ex.getMessage());
                errorLabel.setVisible(true);
                return;
            }

            if (isEdit && existingLink != null) {
                existingLink.setIssuePattern(pat);
                existingLink.setLinkUrl(link);
                IssueNavigationManager.getInstance().setLinks(IssueNavigationManager.getInstance().getLinks());
            } else {
                IssueNavigationManager.getInstance().addLink(pat, link);
            }
            confirmed = true;
            close();
        });

        buttonBar.getChildren().addAll(okBtn, cancelBtn);
        root.getChildren().addAll(grid, buttonBar);
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
