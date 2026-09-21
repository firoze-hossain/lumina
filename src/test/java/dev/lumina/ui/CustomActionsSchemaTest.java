package dev.lumina.ui;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomActionsSchemaTest {

    private CustomActionsSchema schema;

    @BeforeEach
    void setUp() {
        schema = CustomActionsSchema.getInstance();
        schema.revertToDefaults();
    }

    @Test
    void testRootGroupsCountAndIntegrity() {
        List<CustomActionItem> rootGroups = schema.getRootGroups();
        assertEquals(27, rootGroups.size(), "Should have exactly 27 root menus and toolbars matching reference image");

        // Verify key root items from media_1789966874315.png
        assertEquals("Main Menu", rootGroups.get(0).getText());
        assertEquals("Main Toolbar", rootGroups.get(1).getText());
        assertEquals("Editor Popup Menu", rootGroups.get(2).getText());
        assertEquals("Editor Gutter Popup Menu", rootGroups.get(3).getText());
        assertEquals("Editor Tab Popup Menu", rootGroups.get(4).getText());
        assertEquals("Project View Popup Menu", rootGroups.get(5).getText());
        assertEquals("Scope View Popup Menu", rootGroups.get(6).getText());
        assertEquals("VCS Operations Popup", rootGroups.get(26).getText());
    }

    @Test
    void testMainMenuSubmenus() {
        CustomActionItem mainMenu = schema.getRootGroups().get(0);
        List<String> submenus = mainMenu.getChildren().stream().map(CustomActionItem::getText).toList();

        // Verify submenus from media_1789966874317.png
        List<String> expected = List.of("File", "Edit", "View", "Navigate", "Code", "Refactor", "Build", "Run", "Tools", "Git", "Window", "Help");
        assertEquals(expected, submenus);
    }

    @Test
    void testDeepFileAndNewHierarchy() {
        CustomActionItem mainMenu = schema.getRootGroups().get(0);
        CustomActionItem fileMenu = mainMenu.getChildren().get(0);
        assertEquals("File", fileMenu.getText());

        // Find File Open Actions
        CustomActionItem fileOpenActions = fileMenu.getChildren().stream()
                .filter(i -> "File Open Actions".equals(i.getText()))
                .findFirst()
                .orElse(null);
        assertNotNull(fileOpenActions);

        // Find Open Project Actions
        CustomActionItem openProjActions = fileOpenActions.getChildren().stream()
                .filter(i -> "Open Project Actions".equals(i.getText()))
                .findFirst()
                .orElse(null);
        assertNotNull(openProjActions);

        // Find New
        CustomActionItem newMenu = openProjActions.getChildren().stream()
                .filter(i -> "New".equals(i.getText()))
                .findFirst()
                .orElse(null);
        assertNotNull(newMenu);

        // Verify language and template items in New menu matching media_1789966874336.png
        List<String> newSubGroupNames = newMenu.getChildren().stream().map(CustomActionItem::getText).toList();
        assertTrue(newSubGroupNames.contains("GitHub Actions"));
        assertTrue(newSubGroupNames.contains("Web Page"));
        assertTrue(newSubGroupNames.contains("New Group (1)"));
        assertTrue(newSubGroupNames.contains("PhpNewGroup"));
        assertTrue(newSubGroupNames.contains("JavaFxCreateActions"));
        assertTrue(newSubGroupNames.contains("NewJavaSpecialFile"));

        // Check New Group (1) items
        CustomActionItem newGroup1 = newMenu.getChildren().stream()
                .filter(i -> "New Group (1)".equals(i.getText()))
                .findFirst()
                .orElse(null);
        assertNotNull(newGroup1);

        List<String> langActions = newGroup1.getChildren().stream().map(CustomActionItem::getText).toList();
        assertTrue(langActions.contains("Kotlin Class/File"));
        assertTrue(langActions.contains("Python File"));
        assertTrue(langActions.contains("Rust File"));
        assertTrue(langActions.contains("Go File"));
        assertTrue(langActions.contains("Scala Class/File"));
        assertTrue(langActions.contains("Play template"));
    }

    @Test
    void testStrictBrandIsolation() {
        // Guarantee no "JetBrains" or "IntelliJ" word in text, ID, or children
        for (CustomActionItem root : schema.getRootGroups()) {
            assertNoThirdPartyBranding(root);
        }
    }

    private void assertNoThirdPartyBranding(CustomActionItem item) {
        if (item.getText() != null) {
            assertFalse(item.getText().toLowerCase().contains("jetbrains"), "Must not contain 'jetbrains': " + item.getText());
            assertFalse(item.getText().toLowerCase().contains("intellij"), "Must not contain 'intellij': " + item.getText());
        }
        if (item.getId() != null) {
            assertFalse(item.getId().toLowerCase().contains("jetbrains"), "Must not contain 'jetbrains': " + item.getId());
            assertFalse(item.getId().toLowerCase().contains("intellij"), "Must not contain 'intellij': " + item.getId());
        }
        for (CustomActionItem child : item.getChildren()) {
            assertNoThirdPartyBranding(child);
        }
    }

    @Test
    void testDynamicModificationsAndRevert() {
        assertFalse(schema.isModified());

        CustomActionItem mainMenu = schema.getRootGroups().get(0);
        int initialSize = mainMenu.getChildren().size();

        // Add separator
        CustomActionItem sep = CustomActionItem.separator();
        mainMenu.addChild(sep);
        schema.setModified(true);

        assertTrue(schema.isModified());
        assertEquals(initialSize + 1, mainMenu.getChildren().size());

        // Revert to defaults
        schema.revertToDefaults();
        assertFalse(schema.isModified());
        assertEquals(initialSize, schema.getRootGroups().get(0).getChildren().size());
    }

    @Test
    void testAllAvailableActionsCatalog() {
        List<CustomActionItem> catalog = schema.getAllAvailableActions();
        assertNotNull(catalog);
        assertFalse(catalog.isEmpty());
        assertTrue(catalog.stream().anyMatch(a -> "Save All".equals(a.getText())));
        assertTrue(catalog.stream().anyMatch(a -> "Search Everywhere".equals(a.getText())));
        assertTrue(catalog.stream().anyMatch(a -> "Terminal Tool Window".equals(a.getText())));
    }
}
