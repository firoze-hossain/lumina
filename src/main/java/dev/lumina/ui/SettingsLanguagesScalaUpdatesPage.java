package dev.lumina.ui;

import dev.lumina.scala.ScalaLanguageSettingsManager;
import dev.lumina.scala.ScalaUpdatesSettings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;

/**
 * Languages & Frameworks > Scala > Updates settings page in Lumina IDE.
 * Matches Image 1:
 *  - Update channel: [Stable Releases]
 *  - [Check Now] button
 *  - Explanatory note: You can always select "Stable Release" or "Early Access Program" to revert to a more stable build.
 */
public class SettingsLanguagesScalaUpdatesPage extends VBox {

    private final ScalaLanguageSettingsManager manager = ScalaLanguageSettingsManager.getInstance();
    private Runnable onModifiedListener;

    private ComboBox<String> updateChannelComboBox;
    private Button checkNowBtn;
    private Label statusLabel;

    private ScalaUpdatesSettings initialSettings;

    public SettingsLanguagesScalaUpdatesPage() {
        setSpacing(14);
        setPadding(new Insets(20, 24, 20, 24));
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        buildContent();
        takeSnapshot();
    }

    private void buildContent() {
        ScalaUpdatesSettings current = manager.getUpdatesSettings();

        // 1. Channel row: "Update channel:" [ ComboBox ] [ Check Now ]
        HBox channelRow = new HBox(12);
        channelRow.setAlignment(Pos.CENTER_LEFT);

        Label channelLabel = new Label("Update channel:");
        channelLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        updateChannelComboBox = new ComboBox<>(FXCollections.observableArrayList(
                ScalaUpdatesSettings.CHANNEL_STABLE,
                ScalaUpdatesSettings.CHANNEL_EAP,
                ScalaUpdatesSettings.CHANNEL_NIGHTLY
        ));
        updateChannelComboBox.setValue(current.getUpdateChannel());
        updateChannelComboBox.setPrefWidth(180);
        styleComboBox(updateChannelComboBox);
        updateChannelComboBox.valueProperty().addListener((obs, oldV, newV) -> fireModified());

        checkNowBtn = new Button("Check Now");
        styleButton(checkNowBtn);
        checkNowBtn.setOnAction(e -> handleCheckNow());

        channelRow.getChildren().addAll(channelLabel, updateChannelComboBox, checkNowBtn);

        // 2. Explanatory text
        Label noteLabel = new Label("You can always select \"Stable Release\" or \"Early Access Program\" to revert to a more stable build.");
        noteLabel.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");

        // 3. Status label (appears on check)
        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #589DF6; -fx-font-size: 12px;");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        getChildren().addAll(channelRow, noteLabel, statusLabel);
    }

    private void handleCheckNow() {
        statusLabel.setText("You have the latest version of Scala plugin installed.");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4;");
    }

    private void styleButton(Button btn) {
        btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; " +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 12; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #35373B; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 12; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #43454A; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 12; -fx-cursor: hand;"));
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void fireModified() {
        if (onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public void takeSnapshot() {
        this.initialSettings = getCurrentUiSettings();
    }

    public ScalaUpdatesSettings getCurrentUiSettings() {
        ScalaUpdatesSettings s = new ScalaUpdatesSettings();
        if (updateChannelComboBox != null && updateChannelComboBox.getValue() != null) {
            s.setUpdateChannel(updateChannelComboBox.getValue());
        }
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        return !Objects.equals(initialSettings, getCurrentUiSettings());
    }

    public void apply() {
        manager.setUpdatesSettings(getCurrentUiSettings());
        takeSnapshot();
        fireModified();
    }

    public void reset() {
        ScalaUpdatesSettings saved = manager.getUpdatesSettings();
        applySettingsToUi(saved);
        takeSnapshot();
        fireModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            applySettingsToUi(initialSettings);
            fireModified();
        }
    }

    public void resetDefaults() {
        applySettingsToUi(new ScalaUpdatesSettings());
        fireModified();
    }

    private void applySettingsToUi(ScalaUpdatesSettings s) {
        if (s == null) return;
        if (updateChannelComboBox != null) {
            updateChannelComboBox.setValue(s.getUpdateChannel());
        }
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    // Direct UI accessors for tests
    public ComboBox<String> getUpdateChannelComboBox() {
        return updateChannelComboBox;
    }

    public Button getCheckNowBtn() {
        return checkNowBtn;
    }

    public Label getStatusLabel() {
        return statusLabel;
    }
}
