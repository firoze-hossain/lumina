package dev.lumina.diff;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiffEngineTest {

    @Test
    void testIdenticalTextsProduceZeroDifferences() {
        String code = "line 1\nline 2\nline 3";
        DiffEngine.DiffResult res = DiffEngine.diff(code, code, false);
        assertEquals(0, res.differenceCount());
        assertTrue(res.chunks().isEmpty());
        assertEquals(3, res.leftLines().size());
        assertEquals(3, res.rightLines().size());
    }

    @Test
    void testInsertedLines() {
        String left = "line 1\nline 3";
        String right = "line 1\nline 2\nline 3";
        DiffEngine.DiffResult res = DiffEngine.diff(left, right, false);
        assertEquals(1, res.differenceCount());
        DiffEngine.DiffChunk chunk = res.chunks().get(0);
        assertEquals(1, chunk.leftStart());
        assertEquals(0, chunk.leftCount());
        assertEquals(1, chunk.rightStart());
        assertEquals(1, chunk.rightCount());
        assertEquals(DiffEngine.DiffType.INSERTED, chunk.type());
    }

    @Test
    void testDeletedLines() {
        String left = "line 1\nline 2\nline 3";
        String right = "line 1\nline 3";
        DiffEngine.DiffResult res = DiffEngine.diff(left, right, false);
        assertEquals(1, res.differenceCount());
        DiffEngine.DiffChunk chunk = res.chunks().get(0);
        assertEquals(1, chunk.leftStart());
        assertEquals(1, chunk.leftCount());
        assertEquals(1, chunk.rightStart());
        assertEquals(0, chunk.rightCount());
        assertEquals(DiffEngine.DiffType.DELETED, chunk.type());
    }

    @Test
    void testModifiedLines() {
        String left = "int a = 1;\nint b = 2;";
        String right = "int a = 10;\nint b = 2;";
        DiffEngine.DiffResult res = DiffEngine.diff(left, right, false);
        assertEquals(1, res.differenceCount());
        DiffEngine.DiffChunk chunk = res.chunks().get(0);
        assertEquals(0, chunk.leftStart());
        assertEquals(1, chunk.leftCount());
        assertEquals(0, chunk.rightStart());
        assertEquals(1, chunk.rightCount());
        assertEquals(DiffEngine.DiffType.MODIFIED, chunk.type());
    }

    @Test
    void testWhitespaceNormalization() {
        String left = "int a = 1;  ";
        String right = "int a = 1;";
        // Without whitespace ignore
        DiffEngine.DiffResult res1 = DiffEngine.diff(left, right, false);
        assertEquals(1, res1.differenceCount());

        // With whitespace ignore
        DiffEngine.DiffResult res2 = DiffEngine.diff(left, right, true);
        assertEquals(0, res2.differenceCount());
    }
}
