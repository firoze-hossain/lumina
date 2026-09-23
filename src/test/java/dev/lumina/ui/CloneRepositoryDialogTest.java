package dev.lumina.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CloneRepositoryDialogTest {

    private static volatile boolean javaFxAvailable = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank() || java.awt.GraphicsEnvironment.isHeadless()) {
                return;
            }
            Platform.startup(() -> javaFxAvailable = true);
            javaFxAvailable = true;
        } catch (Throwable ignored) {}
    }

    @Test
    void testExtractRepoName() {
        assertEquals("lumina", CloneRepositoryDialog.extractRepoName("https://github.com/firoze-hossain/lumina.git"));
        assertEquals("NexaCommerce", CloneRepositoryDialog.extractRepoName("https://github.com/firoze-hossain/NexaCommerce.git/"));
        assertEquals("my-repo", CloneRepositoryDialog.extractRepoName("git@github.com:user/my-repo.git"));
        assertEquals("repo", CloneRepositoryDialog.extractRepoName("https://gitlab.com/group/repo"));
        assertEquals("", CloneRepositoryDialog.extractRepoName(""));
        assertEquals("", CloneRepositoryDialog.extractRepoName(null));
    }

    @Test
    void testCloneRepositoryDialogComponents() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<CloneRepositoryDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                CloneRepositoryDialog dialog = new CloneRepositoryDialog(
                        null,
                        Path.of("/tmp/projects"),
                        "Repository URL",
                        null,
                        dir -> {}
                );
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        CloneRepositoryDialog dialog = ref.get();
        assertNotNull(dialog);
        assertEquals("Clone Repository", dialog.getTitle());
        assertEquals("Repository URL", dialog.getSelectedTab());

        // Controls
        assertNotNull(dialog.getUrlField());
        assertNotNull(dialog.getDirField());
        assertNotNull(dialog.getShallowCheck());
        assertNotNull(dialog.getShallowSpinner());
        assertNotNull(dialog.getCloneButton());
        assertNotNull(dialog.getCancelButton());

        // Initial button states
        assertTrue(dialog.getCloneButton().isDisable());
        assertTrue(dialog.getShallowSpinner().isDisable());

        // Shallow clone binding
        Platform.runLater(() -> dialog.getShallowCheck().setSelected(true));
        CountDownLatch latch2 = new CountDownLatch(1);
        Platform.runLater(latch2::countDown);
        assertTrue(latch2.await(2, TimeUnit.SECONDS));
        assertFalse(dialog.getShallowSpinner().isDisable());

        // Typing URL updates directory and enables Clone button
        Platform.runLater(() -> dialog.getUrlField().setText("https://github.com/firoze-hossain/awesome-app.git"));
        CountDownLatch latch3 = new CountDownLatch(1);
        Platform.runLater(latch3::countDown);
        assertTrue(latch3.await(2, TimeUnit.SECONDS));

        assertTrue(dialog.getDirField().getText().endsWith("awesome-app"));
        assertFalse(dialog.getCloneButton().isDisable());
    }

    @Test
    void testGitHubTabRepoListAndFilter() throws Exception {
        if (!javaFxAvailable) return;

        AtomicReference<CloneRepositoryDialog> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                CloneRepositoryDialog dialog = new CloneRepositoryDialog(
                        null,
                        Path.of("/tmp/projects"),
                        "GitHub",
                        null,
                        dir -> {}
                );
                ref.set(dialog);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        CloneRepositoryDialog dialog = ref.get();
        assertNotNull(dialog);
        assertEquals("GitHub", dialog.getSelectedTab());

        var repoList = dialog.getActiveGitHubRepoList();
        assertNotNull(repoList);
        assertFalse(repoList.getItems().isEmpty(), "Repositories should be dynamically loaded into ListView");

        // Verify that repo list contains seeded repos like Banking-app
        boolean hasBanking = repoList.getItems().stream().anyMatch(r -> r.name().contains("Banking-app"));
        assertTrue(hasBanking, "Expected Banking-app repository in the list");

        // Test search filtering
        var searchField = dialog.getActiveSearchField();
        assertNotNull(searchField);

        Platform.runLater(() -> searchField.setText("Banking"));
        CountDownLatch filterLatch = new CountDownLatch(1);
        Platform.runLater(filterLatch::countDown);
        assertTrue(filterLatch.await(2, TimeUnit.SECONDS));

        assertTrue(repoList.getItems().size() >= 1);
        for (var item : repoList.getItems()) {
            assertTrue(item.name().toLowerCase().contains("banking"));
        }

        // Test repo selection
        Platform.runLater(() -> repoList.getSelectionModel().select(0));
        CountDownLatch selectLatch = new CountDownLatch(1);
        Platform.runLater(selectLatch::countDown);
        assertTrue(selectLatch.await(2, TimeUnit.SECONDS));

        assertFalse(dialog.getCloneButton().isDisable(), "Clone button should be enabled upon selecting a repo");
        assertTrue(dialog.getUrlField().getText().contains("Banking-app"));
        assertTrue(dialog.getDirField().getText().endsWith("Banking-app"));
    }
}
