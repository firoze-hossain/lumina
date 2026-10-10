package dev.lumina.ui;

import dev.lumina.tools.QodanaSettings;
import dev.lumina.tools.QodanaSettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Tools > Qodana settings page in Lumina IDE.
 * Matches screenshot 4 1:1 with brand isolation (Lumina IDE branding only).
 */
public class SettingsToolsQodanaPage extends VBox {

    private final QodanaSettingsManager manager;
    private QodanaSettings initialSettings;
    private Runnable onModifiedListener;
    private boolean updating = false;

    private Button loginButton;
    private TextField qodanaUrlField;
    private Hyperlink exploreLink;
    private Button watchOverviewButton;

    public SettingsToolsQodanaPage() {
        this.manager = QodanaSettingsManager.getInstance();
        buildUI();
        loadData();
    }

    private void buildUI() {
        setStyle("-fx-background-color: #1E1F22;");
        VBox.setVgrow(this, Priority.ALWAYS);

        VBox contentBox = new VBox(16);
        contentBox.setPadding(new Insets(16, 24, 24, 24));
        contentBox.setStyle("-fx-background-color: #1E1F22;");

        // Header: Log in to Qodana
        Label loginHeader = new Label("Log in to Qodana");
        loginHeader.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 14px; -fx-font-weight: bold;");

        loginButton = new Button("Log In");
        loginButton.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 16; -fx-cursor: hand;");

        // URL row
        Label urlLabel = new Label("Qodana URL:");
        urlLabel.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        qodanaUrlField = new TextField("qodana.cloud");
        qodanaUrlField.setPrefWidth(260);
        qodanaUrlField.setStyle("-fx-background-color: #2B2D30; -fx-text-fill: #DFE1E5; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4;");

        HBox urlRow = new HBox(12, urlLabel, qodanaUrlField);
        urlRow.setAlignment(Pos.CENTER_LEFT);

        // Info Card & Banner
        VBox bannerCard = new VBox(12);
        bannerCard.setPadding(new Insets(16));
        bannerCard.setStyle("-fx-background-color: #26282E; -fx-border-color: #393B40; -fx-border-radius: 8; -fx-background-radius: 8;");
        bannerCard.setMaxWidth(680);

        // Qodana Logo + Title Header
        HBox brandHeader = new HBox(10);
        brandHeader.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("QD");
        iconBadge.setStyle("-fx-background-color: linear-gradient(to bottom right, #FC5858, #9B34EB); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 6 8; -fx-background-radius: 6;");

        VBox titleBox = new VBox(2);
        Label qdTitle = new Label("Qodana");
        qdTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label qdSubtitle = new Label("by Lumina IDE");
        qdSubtitle.setStyle("-fx-text-fill: #7A7E85; -fx-font-size: 11px;");

        titleBox.getChildren().addAll(qdTitle, qdSubtitle);
        brandHeader.getChildren().addAll(iconBadge, titleBox);

        Label descPara1 = new Label("The code quality platform for your favorite CI tool.");
        descPara1.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px;");

        Label descPara2 = new Label("Evaluate the integrity of code you own, contract, or purchase. Enrich your CI/CD pipelines with all the smart features you love from Lumina IDE.");
        descPara2.setStyle("-fx-text-fill: #8C9099; -fx-font-size: 12px;");
        descPara2.setWrapText(true);

        exploreLink = new Hyperlink("Explore Qodana ↗");
        exploreLink.setStyle("-fx-text-fill: #3574F0; -fx-font-size: 13px; -fx-padding: 0; -fx-underline: false;");

        // Preview thumbnail with video play button
        StackPane thumbnailPane = new StackPane();
        thumbnailPane.setPrefSize(480, 220);
        thumbnailPane.setMaxSize(480, 220);
        thumbnailPane.setStyle("-fx-background-color: #1E1F22; -fx-border-color: #4E5157; -fx-border-radius: 6; -fx-background-radius: 6;");

        VBox mockDashboard = new VBox(6);
        mockDashboard.setPadding(new Insets(12));
        mockDashboard.setAlignment(Pos.TOP_LEFT);
        Label mockTitle = new Label("my_test_project");
        mockTitle.setStyle("-fx-text-fill: #7A7E85; -fx-font-size: 11px;");
        Label mockProblems = new Label("112 problems found");
        mockProblems.setStyle("-fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-font-weight: bold;");
        mockDashboard.getChildren().addAll(mockTitle, mockProblems);

        watchOverviewButton = new Button("▶  Watch Qodana Overview");
        watchOverviewButton.setStyle("-fx-background-color: rgba(30, 31, 34, 0.85); -fx-text-fill: #DFE1E5; -fx-font-size: 13px; -fx-border-color: #4E5157; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8 16; -fx-cursor: hand;");

        thumbnailPane.getChildren().addAll(mockDashboard, watchOverviewButton);

        bannerCard.getChildren().addAll(brandHeader, descPara1, descPara2, exploreLink, thumbnailPane);

        contentBox.getChildren().addAll(loginHeader, loginButton, urlRow, bannerCard);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1E1F22; -fx-background-color: #1E1F22; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().add(scrollPane);

        setupListeners();
    }

    private void setupListeners() {
        qodanaUrlField.textProperty().addListener((obs, oldVal, newVal) -> notifyModified());
        loginButton.setOnAction(e -> {
            boolean currentLoggedIn = initialSettings != null && initialSettings.isLoggedIn();
            if (!currentLoggedIn) {
                loginButton.setText("Log Out");
            } else {
                loginButton.setText("Log In");
            }
            notifyModified();
        });
    }

    private void loadData() {
        updating = true;
        try {
            initialSettings = manager.getSettings();
            applySettingsToUI(initialSettings);
        } finally {
            updating = false;
        }
    }

    private void applySettingsToUI(QodanaSettings s) {
        if (s == null) return;
        qodanaUrlField.setText(s.getQodanaUrl());
        loginButton.setText(s.isLoggedIn() ? "Log Out" : "Log In");
    }

    private QodanaSettings getCurrentSettingsFromUI() {
        QodanaSettings s = new QodanaSettings();
        s.setQodanaUrl(qodanaUrlField.getText());
        s.setLoggedIn("Log Out".equals(loginButton.getText()));
        if (initialSettings != null) {
            s.setAccountName(initialSettings.getAccountName());
        }
        return s;
    }

    public boolean isModified() {
        if (initialSettings == null) return false;
        QodanaSettings current = getCurrentSettingsFromUI();
        return !Objects.equals(initialSettings, current);
    }

    public void apply() {
        QodanaSettings current = getCurrentSettingsFromUI();
        manager.setSettings(current);
        initialSettings = current.clone();
        notifyModified();
    }

    public void revertChanges() {
        if (initialSettings != null) {
            updating = true;
            try {
                applySettingsToUI(initialSettings);
            } finally {
                updating = false;
            }
            notifyModified();
        }
    }

    public void reset() {
        revertChanges();
    }

    public void setOnModifiedListener(Runnable listener) {
        this.onModifiedListener = listener;
    }

    private void notifyModified() {
        if (!updating && onModifiedListener != null) {
            onModifiedListener.run();
        }
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public TextField getQodanaUrlField() {
        return qodanaUrlField;
    }

    public Hyperlink getExploreLink() {
        return exploreLink;
    }

    public Button getWatchOverviewButton() {
        return watchOverviewButton;
    }
}
