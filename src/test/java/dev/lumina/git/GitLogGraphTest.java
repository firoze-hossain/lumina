package dev.lumina.git;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GitLogGraphTest {

    @Test
    void testLinearHistoryGraph() {
        GitLogCommit c1 = new GitLogCommit("hash1", "h1", List.of("hash2"), "author", "a@b.com", 100, "c1", "c1", List.of(), 0);
        GitLogCommit c2 = new GitLogCommit("hash2", "h2", List.of("hash3"), "author", "a@b.com", 90, "c2", "c2", List.of(), 0);
        GitLogCommit c3 = new GitLogCommit("hash3", "h3", List.of(), "author", "a@b.com", 80, "c3", "c3", List.of(), 0);

        Map<String, GitLogGraph.RowGraph> graph = GitLogGraph.compute(List.of(c1, c2, c3));
        assertNotNull(graph);
        assertEquals(3, graph.size());

        GitLogGraph.RowGraph rg1 = graph.get("hash1");
        assertNotNull(rg1);
        assertEquals(0, rg1.commitLane());

        GitLogGraph.RowGraph rg2 = graph.get("hash2");
        assertNotNull(rg2);
        assertEquals(0, rg2.commitLane());

        GitLogGraph.RowGraph rg3 = graph.get("hash3");
        assertNotNull(rg3);
        assertEquals(0, rg3.commitLane());
    }

    @Test
    void testMergeCommitLanes() {
        // c1 is a merge of c2 (first parent) and c3 (second parent)
        // c2 and c3 both branch from c4
        GitLogCommit c1 = new GitLogCommit("m1", "m1", List.of("c2", "c3"), "author", "a@b.com", 100, "merge", "merge", List.of(), 0);
        GitLogCommit c2 = new GitLogCommit("c2", "c2", List.of("c4"), "author", "a@b.com", 90, "c2", "c2", List.of(), 0);
        GitLogCommit c3 = new GitLogCommit("c3", "c3", List.of("c4"), "author", "a@b.com", 85, "c3", "c3", List.of(), 0);
        GitLogCommit c4 = new GitLogCommit("c4", "c4", List.of(), "author", "a@b.com", 80, "c4", "c4", List.of(), 0);

        Map<String, GitLogGraph.RowGraph> graph = GitLogGraph.compute(List.of(c1, c2, c3, c4));
        assertNotNull(graph);
        assertEquals(4, graph.size());

        GitLogGraph.RowGraph rg1 = graph.get("m1");
        assertNotNull(rg1);
        assertEquals(0, rg1.commitLane());
        // Merge commit should have down connections to parent lanes
        assertFalse(rg1.downConnections().isEmpty());
    }

    @Test
    void testLaneColors() {
        for (int i = 0; i < 10; i++) {
            String color = GitLogGraph.getLaneColor(i);
            assertNotNull(color);
            assertTrue(color.startsWith("#"));
        }
        assertEquals(GitLogGraph.getLaneColor(0), GitLogGraph.getLaneColor(GitLogGraph.LANE_COLORS.length));
    }
}
