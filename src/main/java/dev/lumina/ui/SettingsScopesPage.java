// SettingsScopesPage.java
package dev.lumina.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * IntelliJ-style Scopes settings page.
 */
public class SettingsScopesPage extends VBox {

    public SettingsScopesPage() {
        getStyleClass().add("settings-page");
        setPadding(new Insets(16, 20, 16, 20));
        setSpacing(14);

        // Add Scope section
        Label addLabel = new Label("Add Scope");
        addLabel.getStyleClass().add("settings-section");

        HBox scopeButtons = new HBox(8);
        scopeButtons.setAlignment(Pos.CENTER_LEFT);
        
        Button localScope = new Button("Local");
        localScope.getStyleClass().add("dialog-secondary");
        Button sharedScope = new Button("Shared");
        sharedScope.getStyleClass().add("dialog-secondary");
        
        scopeButtons.getChildren().addAll(localScope, sharedScope);

        // Empty state
        Label empty = new Label("No scopes added.");
        empty.getStyleClass().add("settings-placeholder");

        Label hint = new Label("Select a scope to view or edit its details here");
        hint.getStyleClass().add("settings-hint");

        VBox content = new VBox(10, addLabel, scopeButtons, empty, hint);
        getChildren().addAll(content);
    }
}