package dev.lumina.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class IssueNavigationManagerTest {

    private IssueNavigationManager manager;

    @BeforeEach
    void setUp() {
        manager = IssueNavigationManager.getInstance();
        manager.clearLinks();
    }

    @Test
    void testYouTrackPatternGeneration() {
        manager.addYouTrackPattern("https://mycompany.myjetbrains.com/youtrack/");
        List<IssueNavigationManager.IssueNavigationLink> links = manager.getLinks();
        assertEquals(1, links.size());
        assertEquals("[A-Z]+-\\d+", links.get(0).getIssuePattern());
        assertEquals("https://mycompany.myjetbrains.com/youtrack/issue/$0", links.get(0).getLinkUrl());
    }

    @Test
    void testJiraPatternGeneration() {
        manager.addJiraPattern("jira.company.com");
        List<IssueNavigationManager.IssueNavigationLink> links = manager.getLinks();
        assertEquals(1, links.size());
        assertEquals("[A-Z]+-\\d+", links.get(0).getIssuePattern());
        assertEquals("https://jira.company.com/browse/$0", links.get(0).getLinkUrl());
    }

    @Test
    void testFindIssueMatches() {
        manager.addYouTrackPattern("https://youtrack.jetbrains.com");
        String message = "Fix IDEA-12345 and resolve issue in KT-999.";
        List<IssueNavigationManager.IssueMatch> matches = manager.findIssueMatches(message);

        assertEquals(2, matches.size());
        assertEquals("IDEA-12345", matches.get(0).issueKey());
        assertEquals("https://youtrack.jetbrains.com/issue/IDEA-12345", matches.get(0).targetUrl());
        assertEquals(4, matches.get(0).start());
        assertEquals(14, matches.get(0).end());

        assertEquals("KT-999", matches.get(1).issueKey());
        assertEquals("https://youtrack.jetbrains.com/issue/KT-999", matches.get(1).targetUrl());
    }

    @Test
    void testCustomGroupReplacement() {
        manager.addLink("#(\\d+)", "https://github.com/myorg/myrepo/issues/$1");
        String message = "Merged PR closes #456 today";
        List<IssueNavigationManager.IssueMatch> matches = manager.findIssueMatches(message);

        assertEquals(1, matches.size());
        assertEquals("#456", matches.get(0).issueKey());
        assertEquals("https://github.com/myorg/myrepo/issues/456", matches.get(0).targetUrl());
    }

    @Test
    void testAddUpdateRemoveAndListener() {
        AtomicBoolean called = new AtomicBoolean(false);
        Runnable listener = () -> called.set(true);
        manager.addListener(listener);

        manager.addLink("[A-Z]+-\\d+", "https://example.com/$0");
        assertTrue(called.get());
        assertEquals(1, manager.getLinks().size());

        called.set(false);
        manager.updateLink(0, "ABC-\\d+", "https://example.com/browse/$0");
        assertTrue(called.get());
        assertEquals("ABC-\\d+", manager.getLinks().get(0).getIssuePattern());

        called.set(false);
        manager.removeLink(0);
        assertTrue(called.get());
        assertTrue(manager.getLinks().isEmpty());

        manager.removeListener(listener);
    }
}
