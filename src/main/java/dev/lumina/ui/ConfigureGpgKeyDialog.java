package dev.lumina.ui;

import dev.lumina.git.GitSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

/**
 * Modal dialog matching IntelliJ IDEA Image 4: "Configure GPG Key".
 */
public class ConfigureGpgKeyDialog extends Stage {

    private final GitSettingsManager manager = GitSettingsManager.getInstance();

    private final CheckBox signCommitsCheck = new CheckBox("Sign commits with GPG key:");
    private final ComboBox<String> keyCombo = new ComboBox<>();
    private final Label noKeyWarning = new Label("Cannot find suitable private key");
    private final Hyperlink setupGuideLink = new Hyperlink("See GPG setup guide");
    private final Label syncedLabel = new Label("Synced with gitconfig");

    private final Button okBtn = new Button("OK");
    private final Button cancelBtn = new Button("Cancel");

    public ConfigureGpgKeyDialog(Window owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Configure GPG Key");
        setResizable(false);

        VBox root = new VBox(12);
        root.setPadding(new Insets(18, 20, 18, 20));
        root.setStyle("-fx-background-color: #2B2D30; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. Sign commits checkbox
        signCommitsCheck.setSelected(manager.isSignCommitsWithGpg());
        signCommitsCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-cursor: hand;");

        // 2. ComboBox for key
        keyCombo.setPrefWidth(320);
        keyCombo.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        List<String> keys = manager.detectGpgSecretKeys();
        if (!keys.isEmpty()) {
            keyCombo.getItems().addAll(keys);
            String savedKey = manager.getGpgKeyId();
            if (!savedKey.isEmpty() && keys.contains(savedKey)) {
                keyCombo.setValue(savedKey);
            } else {
                keyCombo.setValue(keys.get(0));
            }
            noKeyWarning.setVisible(false);
            noKeyWarning.setManaged(false);
        } else {
            noKeyWarning.setVisible(true);
            noKeyWarning.setManaged(true);
        }

        keyCombo.disableProperty().bind(signCommitsCheck.selectedProperty().not());

        // 3. Warning label & Guide link
        noKeyWarning.setStyle("-fx-text-fill: #ED5E62; -fx-font-size: 12px;");

        setupGuideLink.setStyle("-fx-text-fill: #56A8F5; -fx-font-size: 12px; -fx-padding: 0; -fx-underline: false;");
        setupGuideLink.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create("https://docs.github.com/en/authentication/managing-commit-signature-verification/generating-a-new-gpg-key"));
                }
            } catch (Exception ignored) {}
        });

        // 4. Synced label
        syncedLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

        // 5. Buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        okBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 18 6 18; -fx-background-radius: 4; -fx-cursor: hand;");
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(e -> {
            manager.setSignCommitsWithGpg(signCommitsCheck.isSelected());
            if (keyCombo.getValue() != null) {
                manager.setGpgKeyId(keyCombo.getValue());
            }
            close();
        });

        cancelBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #5A5D63; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6 16 6 16; -fx-font-size: 13px; -fx-cursor: hand;");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(e -> close());

        buttonBar.getChildren().addAll(okBtn, cancelBtn);

        root.getChildren().addAll(signCommitsCheck, keyCombo, noKeyWarning, setupGuideLink, syncedLabel, buttonBar);

        Scene scene = new Scene(root, 360, 230);
        setScene(scene);
    }
}
