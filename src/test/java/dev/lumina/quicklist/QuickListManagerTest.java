package dev.lumina.quicklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class QuickListManagerTest {

    private QuickListManager manager;

    @BeforeEach
    void setUp() {
        manager = QuickListManager.getInstance();
        manager.revertToDefaults();
    }

    @Test
    void testDefaultDeploymentQuickListExists() {
        QuickList deployment = manager.getQuickList("Deployment");
        assertNotNull(deployment, "Default 'Deployment' quick list must exist");
        assertEquals("Deployment actions", deployment.getDescription());

        List<QuickListItem> items = deployment.getItems();
        assertEquals(13, items.size(), "Deployment quick list must contain exactly 13 items from screenshot");

        // Check key items from media_1790045975661.png
        assertEquals("Upload to Default Server", items.get(0).getText());
        assertEquals("📤", items.get(0).getIconGlyph());
        assertFalse(items.get(0).isSeparator());

        assertEquals("Upload To...", items.get(1).getText());
        assertEquals("Download from Default Server", items.get(2).getText());
        assertEquals("Download From...", items.get(3).getText());
        assertEquals("Compare Local File with Deployed Version", items.get(4).getText());
        assertEquals("Open in default browser", items.get(5).getText());

        // First separator
        assertTrue(items.get(6).isSeparator());

        assertEquals("Select in Remote Host", items.get(7).getText());
        assertEquals("Browse Remote Host", items.get(8).getText());
        assertEquals("🗄", items.get(8).getIconGlyph());

        assertEquals("Directory", items.get(9).getText());
        assertEquals("📁", items.get(9).getIconGlyph());

        assertEquals("Change Permissions...", items.get(10).getText());
        assertEquals("🔑", items.get(10).getIconGlyph());

        // Second separator
        assertTrue(items.get(11).isSeparator());

        assertEquals("Configuration...", items.get(12).getText());
    }

    @Test
    void testAddAndRemoveQuickList() {
        QuickList custom = new QuickList("VCS Operations", "Version control quick actions");
        custom.addItem(QuickListItem.action("Vcs.Commit", "Commit...", "💾"));
        custom.addItem(QuickListItem.action("Vcs.Push", "Push...", "⬆"));

        manager.addQuickList(custom);

        QuickList retrieved = manager.getQuickList("VCS Operations");
        assertNotNull(retrieved);
        assertEquals(2, retrieved.getItems().size());

        manager.removeQuickList(custom);
        assertNull(manager.getQuickList("VCS Operations"));
    }

    @Test
    void testReorderItemsInQuickList() {
        QuickList testList = new QuickList("TestList");
        QuickListItem itemA = QuickListItem.action("A", "Action A", "");
        QuickListItem itemB = QuickListItem.action("B", "Action B", "");
        QuickListItem sep = QuickListItem.separator();

        testList.addItem(itemA);
        testList.addItem(itemB);
        testList.addItem(sep);

        assertEquals("Action A", testList.getItems().get(0).getText());
        assertEquals("Action B", testList.getItems().get(1).getText());

        // Move B up
        testList.moveUp(1);
        assertEquals("Action B", testList.getItems().get(0).getText());
        assertEquals("Action A", testList.getItems().get(1).getText());

        // Move B down
        testList.moveDown(0);
        assertEquals("Action A", testList.getItems().get(0).getText());
        assertEquals("Action B", testList.getItems().get(1).getText());

        // Remove item
        testList.removeItem(1);
        assertEquals(2, testList.getItems().size());
        assertTrue(testList.getItems().get(1).isSeparator());
    }

    @Test
    void testQuickListItemSerialization() {
        QuickListItem act = QuickListItem.action("Sample.Action", "Sample Action", "✨");
        String serializedAct = act.serialize();
        QuickListItem deserializedAct = QuickListItem.deserialize(serializedAct);
        assertNotNull(deserializedAct);
        assertEquals("Sample.Action", deserializedAct.getActionId());
        assertEquals("Sample Action", deserializedAct.getText());
        assertEquals("✨", deserializedAct.getIconGlyph());
        assertFalse(deserializedAct.isSeparator());

        QuickListItem sep = QuickListItem.separator();
        String serializedSep = sep.serialize();
        QuickListItem deserializedSep = QuickListItem.deserialize(serializedSep);
        assertNotNull(deserializedSep);
        assertTrue(deserializedSep.isSeparator());
    }

    @Test
    void testRevertToDefaults() {
        manager.addQuickList(new QuickList("Temporary", "Temporary list"));
        assertNotNull(manager.getQuickList("Temporary"));

        manager.revertToDefaults();
        assertNull(manager.getQuickList("Temporary"));
        assertNotNull(manager.getQuickList("Deployment"));
    }

    @Test
    void testListenerNotification() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);
        manager.addListener(listener);

        QuickList temp = new QuickList("Temp");
        manager.addQuickList(temp);
        assertTrue(notified.get());

        notified.set(false);
        manager.removeQuickList(temp);
        assertTrue(notified.get());

        manager.removeListener(listener);
    }
}
