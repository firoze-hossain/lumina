package dev.lumina.git;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceRemotesTest {

    @Test
    void testRemoteEntryGetDisplayUrl() {
        GitService.RemoteEntry e1 = new GitService.RemoteEntry("origin", "https://github.com/foo/bar.git", "https://github.com/foo/bar.git");
        assertEquals("origin", e1.name());
        assertEquals("https://github.com/foo/bar.git", e1.getDisplayUrl());

        GitService.RemoteEntry e2 = new GitService.RemoteEntry("upstream", null, "https://github.com/upstream/bar.git");
        assertEquals("https://github.com/upstream/bar.git", e2.getDisplayUrl());

        GitService.RemoteEntry e3 = new GitService.RemoteEntry("test", "", "");
        assertEquals("", e3.getDisplayUrl());
    }

    @Test
    void testRemoteEntriesOnCurrentRepo() {
        Path repo = Path.of(".");
        if (!GitService.isRepository(repo)) return;

        List<GitService.RemoteEntry> remotes = GitService.remoteEntries(repo);
        assertNotNull(remotes);
        assertFalse(remotes.isEmpty(), "Active repository should have at least 1 remote (origin)");
        GitService.RemoteEntry origin = remotes.stream()
                .filter(r -> "origin".equalsIgnoreCase(r.name()))
                .findFirst()
                .orElse(null);
        assertNotNull(origin);
        assertTrue(origin.getDisplayUrl().contains("lumina"));
    }

    @Test
    void testRemoteOperationsValidation() {
        Path dummy = Path.of("/nonexistent/path");

        GitService.Result r1 = GitService.addRemote(dummy, "", "");
        assertFalse(r1.ok());
        assertTrue(r1.output().contains("empty"));

        GitService.Result r2 = GitService.removeRemote(dummy, "");
        assertFalse(r2.ok());

        GitService.Result r3 = GitService.setRemoteUrl(dummy, "origin", "");
        assertFalse(r3.ok());

        GitService.Result r4 = GitService.renameRemote(dummy, "", "new-origin");
        assertFalse(r4.ok());

        GitService.Result r5 = GitService.cloneRepo(dummy, "", null, null, null);
        assertFalse(r5.ok());
    }

    @Test
    void testFileRevisionAndContentAtRevision() {
        Path repo = Path.of(".");
        if (!GitService.isRepository(repo)) return;

        List<GitService.FileRevision> revs = GitService.fileRevisions(repo, "pom.xml", 5);
        assertNotNull(revs);
        assertFalse(revs.isEmpty(), "pom.xml should have revisions in git log");

        GitService.FileRevision rev = revs.get(0);
        assertNotNull(rev.hash());
        assertFalse(rev.hash().isBlank());
        assertNotNull(rev.date());

        GitService.Result content = GitService.fileContentAtRevision(repo, rev.hash(), "pom.xml");
        assertTrue(content.ok());
        assertTrue(content.output().contains("<artifactId>lumina-ide</artifactId>"));
    }

    @Test
    void testTags() {
        Path repo = Path.of(".");
        if (!GitService.isRepository(repo)) return;

        List<String> tags = GitService.tags(repo);
        assertNotNull(tags);
    }
}
