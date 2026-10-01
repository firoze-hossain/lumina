package dev.lumina.ui;

import dev.lumina.plugin.PluginManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.Map;

/**
 * Dialog for managing trusted plugin signing certificates.
 */
public class PluginCertificatesDialog extends Stage {

    public PluginCertificatesDialog(Window owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Plugin Certificates");

        PluginManager manager = PluginManager.getInstance();
        ObservableList<Map<String, String>> certs = FXCollections.observableArrayList(manager.getCertificates());

        TableView<Map<String, String>> table = new TableView<>(certs);
        table.setStyle("-fx-background-color: #1E1F22; -fx-control-inner-background: #1E1F22; -fx-border-color: #393B40;");
        table.setPlaceholder(new Label("No trusted plugin root certificates configured"));

        TableColumn<Map<String, String>, String> nameCol = new TableColumn<>("Certificate Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("name")));
        nameCol.setPrefWidth(160);

        TableColumn<Map<String, String>, String> issuerCol = new TableColumn<>("Issuer");
        issuerCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("issuer")));
        issuerCol.setPrefWidth(160);

        TableColumn<Map<String, String>, String> fpCol = new TableColumn<>("SHA-256 Fingerprint");
        fpCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("fingerprint")));
        fpCol.setPrefWidth(220);

        table.getColumns().addAll(nameCol, issuerCol, fpCol);

        Button addBtn = new Button("Import Certificate...");
        addBtn.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #393B40;");
        addBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Certificate File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Certificates (*.crt, *.cer, *.pem)", "*.crt", "*.cer", "*.pem"));
            File file = chooser.showOpenDialog(this);
            if (file != null) {
                String name = file.getName();
                String fp = Integer.toHexString(file.hashCode()).toUpperCase();
                manager.addCertificate(name, "Lumina Trusted CA", fp);
                certs.setAll(manager.getCertificates());
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #3574F0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 16;");
        closeBtn.setOnAction(e -> close());

        HBox btnBar = new HBox(10, addBtn, new Region(), closeBtn);
        HBox.setHgrow(btnBar.getChildren().get(1), Priority.ALWAYS);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(12,
                new Label("Manage X.509 certificates used to verify signatures of enterprise plugins:"),
                table,
                btnBar
        );
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(table, Priority.ALWAYS);

        Scene scene = new Scene(root, 580, 380);
        setScene(scene);
    }
}
