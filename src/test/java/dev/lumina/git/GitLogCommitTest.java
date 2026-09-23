package dev.lumina.git;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitLogCommitTest {

    @Test
    void testCommitCreationAndFields() {
        GitLogCommit commit = new GitLogCommit(
                "c4b247304d6817da61c8bfa44a7909622def2820",
                "c4b2473",
                List.of("e8b2824fd93925b70c960c1a31728e352bc4f1f4"),
                "firoze-hossain",
                "firoze.hossain@outlook.com",
                1790127983L,
                "update project section added",
                "update project section added\n\nFull details here",
                List.of("HEAD -> master", "origin/master"),
                0
        );

        assertEquals("c4b247304d6817da61c8bfa44a7909622def2820", commit.hash());
        assertEquals("c4b2473", commit.shortHash());
        assertEquals(1, commit.parents().size());
        assertEquals("firoze-hossain", commit.authorName());
        assertEquals("firoze.hossain@outlook.com", commit.authorEmail());
        assertEquals("update project section added", commit.subject());
        assertEquals(2, commit.refs().size());
        assertEquals(0, commit.graphLane());

        GitLogCommit updated = commit.withGraphLane(2);
        assertEquals(2, updated.graphLane());
    }

    @Test
    void testDateFormattingTodayYesterdayAndPast() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        Instant now = today.atTime(12, 0).atZone(zone).toInstant();

        // 1. Today
        long todayTimestamp = today.atTime(7, 46).atZone(zone).toEpochSecond();
        String formattedToday = GitLogCommit.formatCommitTimestamp(todayTimestamp, now);
        assertTrue(formattedToday.startsWith("Today "), "Expected 'Today ...' but got: " + formattedToday);

        // 2. Yesterday
        long yesterdayTimestamp = today.minusDays(1).atTime(14, 51).atZone(zone).toEpochSecond();
        String formattedYesterday = GitLogCommit.formatCommitTimestamp(yesterdayTimestamp, now);
        assertTrue(formattedYesterday.startsWith("Yesterday "), "Expected 'Yesterday ...' but got: " + formattedYesterday);

        // 3. Past (e.g. 5 days ago)
        long pastTimestamp = today.minusDays(5).atTime(17, 55).atZone(zone).toEpochSecond();
        String formattedPast = GitLogCommit.formatCommitTimestamp(pastTimestamp, now);
        assertFalse(formattedPast.startsWith("Today"));
        assertFalse(formattedPast.startsWith("Yesterday"));
        assertTrue(formattedPast.contains(", ") || formattedPast.contains("/"));
    }

    @Test
    void testParseRefs() {
        String refStr = "HEAD -> master, origin/master, tag: v1.0.0, origin/HEAD";
        List<String> parsed = GitLogCommit.parseRefs(refStr);

        assertEquals(4, parsed.size());
        assertEquals("master", parsed.get(0));
        assertEquals("origin/master", parsed.get(1));
        assertEquals("v1.0.0", parsed.get(2));
        assertEquals("origin/HEAD", parsed.get(3));
    }

    @Test
    void testMatchingAndCaseSensitivity() {
        GitLogCommit commit = new GitLogCommit(
                "4dc22ca6abcdef123456",
                "4dc22ca6",
                List.of(),
                "Firoze-UHL",
                "firoze@uhlbd.com",
                1790000000L,
                "Validation added in additional",
                "Validation added in additional",
                List.of(),
                0
        );

        // Case insensitive
        assertTrue(commit.matches("validation", false));
        assertTrue(commit.matches("4dc22", false));
        assertTrue(commit.matches("firoze", false));
        assertFalse(commit.matches("nonexistent", false));

        // Case sensitive
        assertTrue(commit.matches("Validation", true));
        assertFalse(commit.matches("validation", true));
        assertTrue(commit.matches("Firoze", true));
        assertFalse(commit.matches("firoze", true));
    }
}
