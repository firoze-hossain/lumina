package dev.lumina.ui;

import dev.lumina.util.Settings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * IntelliJ-style Invalidate Caches dialog.
 * Exactly as shown in the screenshot.
 */
public class InvalidateCachesDialog {

    private final Stage stage;
    private final Runnable onRestart;

    // Checkbox states
    private final CheckBox clearFileSystemCache = new CheckBox("Clear file system cache and Local History");
    private final CheckBox clearVcsCache = new CheckBox("Clear VCS Log caches and indexes");
    private final CheckBox markSharedIndexes = new CheckBox("Mark downloaded shared indexes as broken");
    private final CheckBox deleteBrowserCache = new CheckBox("Delete embedded browser engine cache and cookies");

    private final Label sharedIndexHint = new Label("Download fresh shared indexes if they are available. Otherwise, indexes will be re-built locally.");
    private final Label browserCacheHint = new Label("Affects components that use an embedded browser to render HTML-based content and web pages.");

    public InvalidateCachesDialog(Stage owner, Runnable onRestart) {
        this.stage = new Stage();
        this.onRestart = onRestart;

        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Invalidate Caches");

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("app-root", "invalidate-dialog");

        // ---- Content ----
        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 24, 16, 24));

        // Title
        Label title = new Label("Invalidate Caches");
        title.getStyleClass().add("invalidate-title");

        // Description
        Label description = new Label("Remove caches and indexes for all projects. New caches will be built when you reopen the projects.");
        description.getStyleClass().add("invalidate-description");
        description.setWrapText(true);

        // Optional section
        Label optionalLabel = new Label("Optional:");
        optionalLabel.getStyleClass().add("invalidate-optional");

        // Checkboxes
        clearFileSystemCache.getStyleClass().add("settings-check");
        clearVcsCache.getStyleClass().add("settings-check");
        markSharedIndexes.getStyleClass().add("settings-check");
        deleteBrowserCache.getStyleClass().add("settings-check");

        // Hints
        sharedIndexHint.getStyleClass().add("invalidate-hint");
        sharedIndexHint.setWrapText(true);
        sharedIndexHint.setPadding(new Insets(0, 0, 0, 28));

        browserCacheHint.getStyleClass().add("invalidate-hint");
        browserCacheHint.setWrapText(true);
        browserCacheHint.setPadding(new Insets(0, 0, 0, 28));

        // ---- Buttons ----
        HBox buttons = buildButtonBar();

        // ---- Assemble ----
        VBox checkboxes = new VBox(6,
                clearFileSystemCache,
                clearVcsCache,
                markSharedIndexes,
                sharedIndexHint,
                deleteBrowserCache,
                browserCacheHint
        );
        checkboxes.setPadding(new Insets(4, 0, 0, 0));

        content.getChildren().addAll(title, description, optionalLabel, checkboxes);

        root.setCenter(content);
        root.setBottom(buttons);

        Scene scene = new Scene(root, 580, 400);
        scene.getStylesheets().add(
                getClass().getResource("/css/lumina-dark.css").toExternalForm());
        stage.setScene(scene);

        // Set default checkbox states (all unchecked as shown in screenshot)
        clearFileSystemCache.setSelected(false);
        clearVcsCache.setSelected(false);
        markSharedIndexes.setSelected(false);
        deleteBrowserCache.setSelected(false);
    }

    public void show() {
        stage.showAndWait();
    }

    // --------------------------------------------------- button bar

    private HBox buildButtonBar() {
        Button justRestart = new Button("Just Restart");
        justRestart.getStyleClass().add("dialog-secondary");
        justRestart.setOnAction(e -> {
            performInvalidate(false);
            stage.close();
            if (onRestart != null) onRestart.run();
        });

        Button invalidateAndRestart = new Button("Invalidate and Restart");
        invalidateAndRestart.getStyleClass().add("dialog-primary");
        invalidateAndRestart.setDefaultButton(true);
        invalidateAndRestart.setOnAction(e -> {
            performInvalidate(true);
            stage.close();
            if (onRestart != null) onRestart.run();
        });

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("dialog-secondary");
        cancel.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, spacer, justRestart, invalidateAndRestart, cancel);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(12, 20, 14, 20));
        bar.getStyleClass().add("dialog-footer");
        return bar;
    }

    // --------------------------------------------------- invalidation logic

    private void performInvalidate(boolean fullInvalidate) {
        try {
            Path cacheDir = Path.of(System.getProperty("user.home"), ".lumina", "cache");

            if (Files.exists(cacheDir)) {
                // Delete cache directory if full invalidation or file system cache is checked
                if (fullInvalidate || clearFileSystemCache.isSelected()) {
                    deleteDirectory(cacheDir);
                }
            }

            // Clear VCS Log caches
            if (clearVcsCache.isSelected()) {
                Path vcsDir = Path.of(System.getProperty("user.home"), ".lumina", "vcs-cache");
                if (Files.exists(vcsDir)) {
                    deleteDirectory(vcsDir);
                }
            }

            // Mark shared indexes as broken - just a marker file
            if (markSharedIndexes.isSelected()) {
                Path marker = Path.of(System.getProperty("user.home"), ".lumina", "shared-indexes-broken");
                Files.createDirectories(marker.getParent());
                Files.writeString(marker, "Shared indexes marked as broken at: " + java.time.LocalDateTime.now());
            }

            // Delete browser cache
            if (deleteBrowserCache.isSelected()) {
                Path browserCache = Path.of(System.getProperty("user.home"), ".lumina", "browser-cache");
                if (Files.exists(browserCache)) {
                    deleteDirectory(browserCache);
                }
            }

            System.out.println("Invalidate caches completed. Full invalidate: " + fullInvalidate);

        } catch (IOException e) {
            System.err.println("Failed to invalidate caches: " + e.getMessage());
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var walk = Files.walk(path)) {
                walk.sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
        Files.deleteIfExists(path);
    }

    public boolean isClearFileSystemCache() {
        return clearFileSystemCache.isSelected();
    }

    public boolean isClearVcsCache() {
        return clearVcsCache.isSelected();
    }

    public boolean isMarkSharedIndexes() {
        return markSharedIndexes.isSelected();
    }

    public boolean isDeleteBrowserCache() {
        return deleteBrowserCache.isSelected();
    }
}