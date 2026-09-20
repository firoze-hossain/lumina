package dev.lumina.ui;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MavenPanelTest {

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
    void testMavenPanelStructureAndExecution() {
        if (!javaFxAvailable) return;

        AtomicReference<String> executedGoal = new AtomicReference<>();
        AtomicReference<Path> openedFile = new AtomicReference<>();

        MavenPanel panel = new MavenPanel(executedGoal::set, openedFile::set);
        Path root = Path.of("/home/firoze/projects/others/lumina");

        // 1. Download menu verification (Image 1)
        assertNotNull(panel.getDownloadBtn());
        assertEquals(3, panel.getDownloadBtn().getItems().size());
        assertEquals("Download Sources", panel.getDownloadBtn().getItems().get(0).getText());
        assertEquals("Download Documentation", panel.getDownloadBtn().getItems().get(1).getText());
        assertEquals("Download Sources and Documentation", panel.getDownloadBtn().getItems().get(2).getText());

        // 2. Play button starts disabled (Image 1 & 2)
        assertTrue(panel.getRunGoalBtn().isDisabled());

        // 3. Unlink button starts disabled before project loaded
        assertTrue(panel.getRemoveProjectBtn().isDisabled());

        // Load project
        panel.setProject(root);

        // Unlink button should now be enabled (Image 2)
        assertFalse(panel.getRemoveProjectBtn().isDisabled());
        assertEquals("Unlink Maven Project", panel.getRemoveProjectBtn().getTooltip().getText());

        // Verify goal execution
        panel.executeGoal("compile");
        assertEquals("compile", executedGoal.get());

        panel.executeGoal("clean package");
        assertEquals("clean package", executedGoal.get());

        // 4. Test selecting a goal enables the play button with green styling (Image 3)
        // Find lifecycle 'clean' node in tree
        TreeItem<MavenPanel.MavenNodeData> rootItem = panel.getTree().getRoot();
        assertNotNull(rootItem);
        assertFalse(rootItem.getChildren().isEmpty());
        TreeItem<MavenPanel.MavenNodeData> projectItem = rootItem.getChildren().get(0);
        assertFalse(projectItem.getChildren().isEmpty());

        TreeItem<MavenPanel.MavenNodeData> lifecycleItem = projectItem.getChildren().get(0);
        assertFalse(lifecycleItem.getChildren().isEmpty());

        TreeItem<MavenPanel.MavenNodeData> cleanGoalItem = lifecycleItem.getChildren().get(0);
        assertEquals("clean", cleanGoalItem.getValue().goalToRun());

        // Select the clean goal
        panel.getTree().getSelectionModel().select(cleanGoalItem);
        assertFalse(panel.getRunGoalBtn().isDisabled(), "Run button should be enabled when a goal is selected");
        assertTrue(panel.getRunGoalBtn().getStyle().contains("#59A869"), "Run button should show bright green signal");
        assertEquals("Run 'clean'", panel.getRunGoalBtn().getTooltip().getText());

        // Fire run button action
        panel.getRunGoalBtn().fire();
        assertEquals("clean", executedGoal.get());

        // 5. Select non-goal item (e.g. lifecycle folder) -> run button should disable
        panel.getTree().getSelectionModel().select(lifecycleItem);
        assertTrue(panel.getRunGoalBtn().isDisabled(), "Run button should disable for non-goal items");

        // 6. Test Expand All (Image 1)
        assertNotNull(panel.getExpandAllBtn());
        assertEquals("Expand All  Ctrl+NumPad +", panel.getExpandAllBtn().getTooltip().getText());
        panel.onExpandAll();
        assertTrue(projectItem.isExpanded());
        assertTrue(lifecycleItem.isExpanded());

        // 7. Test Collapse All (Image 2)
        assertNotNull(panel.getCollapseAllBtn());
        assertEquals("Collapse All  Ctrl+NumPad -", panel.getCollapseAllBtn().getTooltip().getText());
        panel.onCollapseAll();
        assertFalse(projectItem.isExpanded(), "Root project node should be collapsed");
        assertEquals(projectItem, panel.getTree().getSelectionModel().getSelectedItem());

        // 8. Test Settings button (Image 3)
        assertNotNull(panel.getSettingsBtn());
        assertEquals("Maven Settings", panel.getSettingsBtn().getTooltip().getText());

        // 9. Test Unlink Project removes project and disables remove button
        panel.getRemoveProjectBtn().fire();
        assertTrue(panel.getRemoveProjectBtn().isDisabled());
    }

    @Test
    void testSetProjectNull() {
        if (!javaFxAvailable) return;

        MavenPanel panel = new MavenPanel(g -> {});
        panel.setProject(null);
        assertTrue(panel.getRemoveProjectBtn().isDisabled());
        assertTrue(panel.getRunGoalBtn().isDisabled());
    }
}
