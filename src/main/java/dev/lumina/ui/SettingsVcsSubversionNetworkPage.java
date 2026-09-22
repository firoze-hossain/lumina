package dev.lumina.ui;

import dev.lumina.git.SubversionSettingsManager;
import dev.lumina.git.SubversionSettingsManager.SslProtocol;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Version Control > Subversion > Network settings page matching IntelliJ IDEA Image 2.
 */
public class SettingsVcsSubversionNetworkPage extends VBox {

    private final SubversionSettingsManager manager = SubversionSettingsManager.getInstance();

    private final CheckBox useGeneralProxyCheck = new CheckBox("Use IDEA general proxy settings as default for Subversion");
    private final Hyperlink navigateProxyLink = new Hyperlink("Navigate to general proxy settings...");
    private final Label proxySubtextLabel = new Label("Only HTTP proxy can be used as default");

    private final Spinner<Integer> httpTimeoutSpinner = new Spinner<>(0, 86400, 0);
    private final Spinner<Integer> sshConnTimeoutSpinner = new Spinner<>(0, 86400, 30);
    private final Spinner<Integer> sshReadTimeoutSpinner = new Spinner<>(0, 86400, 30);

    private final RadioButton sslAllRadio = new RadioButton("All");
    private final RadioButton sslV3Radio = new RadioButton("SSLv3");
    private final RadioButton tlsV1Radio = new RadioButton("TLSv1");

    private final Button editNetworkOptionsBtn = new Button("Edit Network Options...");
    private final Label editServersDescriptionLabel = new Label("Edit 'servers' Subversion runtime configuration file");

    public SettingsVcsSubversionNetworkPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(14, 20, 24, 20));
        setSpacing(16);
        setStyle("-fx-background-color: #1E1F22; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");

        // 1. General Proxy Option & Link
        VBox proxyBox = buildProxyBox();

        // 2. Timeout Spinners
        VBox timeoutsBox = buildTimeoutsBox();

        // 3. SSL Protocols Radio Group
        HBox sslBox = buildSslBox();

        // 4. Edit Network Options Button
        HBox editOptionsBox = buildEditOptionsBox();

        getChildren().addAll(proxyBox, timeoutsBox, sslBox, editOptionsBox);

        manager.addListener(this::syncFromManager);
        syncFromManager();
    }

    private VBox buildProxyBox() {
        VBox box = new VBox(3);

        useGeneralProxyCheck.setSelected(manager.isUseGeneralProxySettings());
        useGeneralProxyCheck.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
        useGeneralProxyCheck.setOnAction(e -> manager.setUseGeneralProxySettings(useGeneralProxyCheck.isSelected()));

        navigateProxyLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 12px; -fx-underline: true; -fx-padding: 0;");
        navigateProxyLink.setOnAction(e -> {
            // Hyperlink navigation to general proxy settings
        });

        HBox topRow = new HBox(12, useGeneralProxyCheck, navigateProxyLink);
        topRow.setAlignment(Pos.CENTER_LEFT);

        proxySubtextLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");
        proxySubtextLabel.setPadding(new Insets(0, 0, 0, 24));

        box.getChildren().addAll(topRow, proxySubtextLabel);
        return box;
    }

    private VBox buildTimeoutsBox() {
        VBox box = new VBox(8);

        box.getChildren().add(createTimeoutRow("HTTP timeout:", httpTimeoutSpinner, manager.getHttpTimeoutSeconds(), val -> manager.setHttpTimeoutSeconds(val)));
        box.getChildren().add(createTimeoutRow("SSH connection timeout:", sshConnTimeoutSpinner, manager.getSshConnectionTimeoutSeconds(), val -> manager.setSshConnectionTimeoutSeconds(val)));
        box.getChildren().add(createTimeoutRow("SSH read timeout:", sshReadTimeoutSpinner, manager.getSshReadTimeoutSeconds(), val -> manager.setSshReadTimeoutSeconds(val)));

        return box;
    }

    private HBox createTimeoutRow(String labelText, Spinner<Integer> spinner, int initialVal, java.util.function.Consumer<Integer> onChange) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        lbl.setPrefWidth(160);

        spinner.getValueFactory().setValue(initialVal);
        spinner.setPrefWidth(70);
        spinner.setEditable(true);
        spinner.setStyle("-fx-background-color: #1E1F22; -fx-font-size: 12px;");
        spinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) onChange.accept(newV);
        });

        Label secLbl = new Label("seconds");
        secLbl.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");

        HBox row = new HBox(8, lbl, spinner, secLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildSslBox() {
        Label sslLabel = new Label("SSL protocols:");
        sslLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px;");
        sslLabel.setPrefWidth(160);

        ToggleGroup sslGroup = new ToggleGroup();
        sslAllRadio.setToggleGroup(sslGroup);
        sslV3Radio.setToggleGroup(sslGroup);
        tlsV1Radio.setToggleGroup(sslGroup);

        initRadio(sslAllRadio, manager.getSslProtocol() == SslProtocol.ALL);
        initRadio(sslV3Radio, manager.getSslProtocol() == SslProtocol.SSLV3);
        initRadio(tlsV1Radio, manager.getSslProtocol() == SslProtocol.TLSV1);

        sslGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == sslV3Radio) {
                manager.setSslProtocol(SslProtocol.SSLV3);
            } else if (newV == tlsV1Radio) {
                manager.setSslProtocol(SslProtocol.TLSV1);
            } else {
                manager.setSslProtocol(SslProtocol.ALL);
            }
        });

        HBox radioBox = new HBox(16, sslAllRadio, sslV3Radio, tlsV1Radio);
        radioBox.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(8, sslLabel, radioBox);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildEditOptionsBox() {
        editNetworkOptionsBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #3574F0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 14 5 14; -fx-font-size: 12px; -fx-cursor: hand;");
        editNetworkOptionsBtn.setOnAction(e -> {
            EditSubversionNetworkOptionsDialog dlg = new EditSubversionNetworkOptionsDialog(getScene().getWindow());
            dlg.showAndWait();
        });

        editServersDescriptionLabel.setStyle("-fx-text-fill: #848BA3; -fx-font-size: 11px;");

        HBox box = new HBox(12, editNetworkOptionsBtn, editServersDescriptionLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(16, 0, 0, 0));
        return box;
    }

    private void initRadio(RadioButton rb, boolean initial) {
        rb.setSelected(initial);
        rb.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    private void syncFromManager() {
        useGeneralProxyCheck.setSelected(manager.isUseGeneralProxySettings());
        httpTimeoutSpinner.getValueFactory().setValue(manager.getHttpTimeoutSeconds());
        sshConnTimeoutSpinner.getValueFactory().setValue(manager.getSshConnectionTimeoutSeconds());
        sshReadTimeoutSpinner.getValueFactory().setValue(manager.getSshReadTimeoutSeconds());

        switch (manager.getSslProtocol()) {
            case SSLV3 -> sslV3Radio.setSelected(true);
            case TLSV1 -> tlsV1Radio.setSelected(true);
            default -> sslAllRadio.setSelected(true);
        }
    }
}
