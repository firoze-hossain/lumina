package dev.lumina.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = NotificationService.getInstance();
    }

    @Test
    void testDefaultGroupsRegisteredAndSorted() {
        var groups = service.getObservableGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());

        // Check standard IntelliJ groups exist
        assertNotNull(service.getGroup("VCS messages"));
        assertNotNull(service.getGroup("Build finished"));
        assertNotNull(service.getGroup("AI Assistant installer"));
        assertNotNull(service.getGroup("Webpack configuration analysis failed"));

        // Check alphabetical sorting
        for (int i = 0; i < groups.size() - 1; i++) {
            String curr = groups.get(i).getTitle();
            String next = groups.get(i + 1).getTitle();
            assertTrue(curr.compareToIgnoreCase(next) <= 0,
                    "Expected '" + curr + "' <= '" + next + "'");
        }
    }

    @Test
    void testDynamicGroupRegistration() {
        String testGroupId = "custom.plugin.updates";
        String testGroupTitle = "Custom Plugin Updates";
        NotificationGroup group = new NotificationGroup(testGroupId, testGroupTitle,
                NotificationDisplayType.STICKY_BALLOON, true, true, false);

        service.registerGroup(group);

        NotificationGroup retrieved = service.getGroup(testGroupId);
        assertNotNull(retrieved);
        assertEquals(testGroupTitle, retrieved.getTitle());
        assertEquals(NotificationDisplayType.STICKY_BALLOON, retrieved.getDisplayType());
        assertTrue(retrieved.isShowInToolWindow());
        assertTrue(retrieved.isPlaySound());
        assertFalse(retrieved.isReadAloud());

        // Also retrievable by title
        NotificationGroup byTitle = service.getGroup(testGroupTitle);
        assertNotNull(byTitle);
        assertEquals(testGroupId, byTitle.getId());
    }

    @Test
    void testNotifyAddsToTimeline() {
        int initialSize = service.getTimeline().size();
        String title = "Unit Test Notification " + System.currentTimeMillis();
        String content = "Test Content";

        Notification notification = new Notification("vcs.messages", "VCS messages", title, content, NotificationType.INFORMATION);
        service.notify(notification);

        assertEquals(initialSize + 1, service.getTimeline().size());
        Notification first = service.getTimeline().getFirst();
        assertEquals(title, first.getTitle());
        assertEquals(content, first.getContent());
        assertEquals(NotificationType.INFORMATION, first.getType());
        assertNotNull(first.getFormattedTime());

        // Clean up
        service.removeNotification(first);
        assertEquals(initialSize, service.getTimeline().size());
    }

    @Test
    void testNotificationActions() {
        AtomicReference<Boolean> clicked = new AtomicReference<>(false);
        Notification notification = new Notification("vcs.messages", "VCS messages", "Commit", "", NotificationType.INFORMATION)
                .addAction("View commits", () -> clicked.set(true));

        assertEquals(1, notification.getActions().size());
        assertEquals("View commits", notification.getActions().getFirst().text());

        notification.getActions().getFirst().action().run();
        assertTrue(clicked.get());
    }

    @Test
    void testToastNotificationDelivery() {
        AtomicReference<Notification> received = new AtomicReference<>();
        java.util.function.Consumer<Notification> listener = received::set;
        service.addToastListener(listener);

        try {
            Notification n = new Notification("build.finished", "Build finished", "Build completed", "", NotificationType.INFORMATION);
            service.notify(n);

            assertNotNull(received.get());
            assertEquals("Build completed", received.get().getTitle());
        } finally {
            service.removeToastListener(listener);
        }
    }

    @Test
    void testNoPopupSuppression() {
        AtomicReference<Notification> received = new AtomicReference<>();
        java.util.function.Consumer<Notification> listener = received::set;
        service.addToastListener(listener);

        NotificationGroup silentGroup = new NotificationGroup("silent.group", "Silent Group",
                NotificationDisplayType.NONE, true, false, false);
        service.registerGroup(silentGroup);

        try {
            Notification n = new Notification("silent.group", "Silent Group", "Silent message", "", NotificationType.INFORMATION);
            service.notify(n);

            // Balloon toast should NOT fire because displayType is NONE
            assertNull(received.get());

            // But it SHOULD still be in the timeline
            assertTrue(service.getTimeline().stream().anyMatch(item -> item.getTitle().equals("Silent message")));
        } finally {
            service.removeToastListener(listener);
        }
    }

    @Test
    void testDontAskAgainList() {
        String testPrompt = "Confirm before deleting database " + System.currentTimeMillis();
        service.addDontAskAgain(testPrompt);
        assertTrue(service.getDontAskAgainList().contains(testPrompt));

        service.removeDontAskAgain(testPrompt);
        assertFalse(service.getDontAskAgainList().contains(testPrompt));
    }

    @Test
    void testNotificationDisplayTypeFromLabel() {
        assertEquals(NotificationDisplayType.NONE, NotificationDisplayType.fromLabel("No popup"));
        assertEquals(NotificationDisplayType.BALLOON, NotificationDisplayType.fromLabel("Balloon"));
        assertEquals(NotificationDisplayType.STICKY_BALLOON, NotificationDisplayType.fromLabel("Sticky balloon"));
        assertEquals(NotificationDisplayType.BALLOON, NotificationDisplayType.fromLabel("Unknown"));
    }
}
