package dev.lumina.diff;

import java.util.*;

/**
 * Pure Java Myers/LCS diff algorithm computing line-level difference chunks.
 * Accurately determines inserted, deleted, and modified blocks for IntelliJ-styled side-by-side diffing.
 */
public final class DiffEngine {

    public enum DiffType {
        INSERTED,  // Added in right version
        DELETED,   // Removed in right version
        MODIFIED   // Changed between left and right
    }

    public record DiffChunk(
            int leftStart,   // 0-based start line index in left
            int leftCount,   // number of lines in left chunk
            int rightStart,  // 0-based start line index in right
            int rightCount,  // number of lines in right chunk
            DiffType type
    ) {
        public int leftEnd() { return leftStart + leftCount; }
        public int rightEnd() { return rightStart + rightCount; }
    }

    public record DiffResult(
            List<String> leftLines,
            List<String> rightLines,
            List<DiffChunk> chunks,
            int differenceCount
    ) {}

    private DiffEngine() {}

    /**
     * Computes the line differences between left and right text.
     *
     * @param leftText          the current working version
     * @param rightText         the stashed / baseline version
     * @param ignoreWhitespaces whether to normalize leading/trailing whitespace when comparing lines
     */
    public static DiffResult diff(String leftText, String rightText, boolean ignoreWhitespaces) {
        List<String> leftLines = splitLines(leftText);
        List<String> rightLines = splitLines(rightText);

        int n = leftLines.size();
        int m = rightLines.size();

        // 1. Trim common prefix
        int prefix = 0;
        while (prefix < n && prefix < m && linesEqual(leftLines.get(prefix), rightLines.get(prefix), ignoreWhitespaces)) {
            prefix++;
        }

        // 2. Trim common suffix
        int suffix = 0;
        while (suffix < (n - prefix) && suffix < (m - prefix)
                && linesEqual(leftLines.get(n - 1 - suffix), rightLines.get(m - 1 - suffix), ignoreWhitespaces)) {
            suffix++;
        }

        List<DiffChunk> chunks = new ArrayList<>();

        int leftMidLen = n - prefix - suffix;
        int rightMidLen = m - prefix - suffix;

        if (leftMidLen > 0 || rightMidLen > 0) {
            List<String> subLeft = leftLines.subList(prefix, n - suffix);
            List<String> subRight = rightLines.subList(prefix, m - suffix);

            List<DiffChunk> rawChunks = computeLcsChunks(subLeft, subRight, prefix, prefix, ignoreWhitespaces);
            chunks.addAll(rawChunks);
        }

        return new DiffResult(leftLines, rightLines, chunks, chunks.size());
    }

    private static boolean linesEqual(String a, String b, boolean ignoreWhitespaces) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (ignoreWhitespaces) {
            return a.trim().equals(b.trim());
        }
        return a.equals(b);
    }

    private static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) return new ArrayList<>();
        List<String> lines = new ArrayList<>();
        // Split on CR, LF, or CRLF
        Scanner scanner = new Scanner(text);
        while (scanner.hasNextLine()) {
            lines.add(scanner.nextLine());
        }
        if (text.endsWith("\n") || text.endsWith("\r")) {
            lines.add("");
        }
        return lines;
    }

    private static List<DiffChunk> computeLcsChunks(
            List<String> left, List<String> right,
            int leftOffset, int rightOffset,
            boolean ignoreWhitespaces) {

        int n = left.size();
        int m = right.size();

        // Optimization for simple insert/delete cases
        if (n == 0 && m > 0) {
            return List.of(new DiffChunk(leftOffset, 0, rightOffset, m, DiffType.INSERTED));
        }
        if (m == 0 && n > 0) {
            return List.of(new DiffChunk(leftOffset, n, rightOffset, 0, DiffType.DELETED));
        }

        // DP table for LCS
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (linesEqual(left.get(i), right.get(j), ignoreWhitespaces)) {
                    dp[i + 1][j + 1] = dp[i][j] + 1;
                } else {
                    dp[i + 1][j + 1] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }

        // Backtrack edit operations
        List<EditOp> ops = new ArrayList<>();
        int i = n, j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && linesEqual(left.get(i - 1), right.get(j - 1), ignoreWhitespaces)) {
                ops.add(new EditOp(OpType.EQUAL, i - 1, j - 1));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                ops.add(new EditOp(OpType.INSERT, -1, j - 1));
                j--;
            } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
                ops.add(new EditOp(OpType.DELETE, i - 1, -1));
                i--;
            }
        }
        Collections.reverse(ops);

        // Group operations into DiffChunks
        List<DiffChunk> chunks = new ArrayList<>();
        int curLeft = 0;
        int curRight = 0;
        int k = 0;

        while (k < ops.size()) {
            EditOp op = ops.get(k);
            if (op.type == OpType.EQUAL) {
                curLeft++;
                curRight++;
                k++;
                continue;
            }

            int chunkLeftStart = curLeft;
            int chunkRightStart = curRight;
            int leftCount = 0;
            int rightCount = 0;

            while (k < ops.size() && ops.get(k).type != OpType.EQUAL) {
                EditOp diffOp = ops.get(k);
                if (diffOp.type == OpType.DELETE) {
                    leftCount++;
                    curLeft++;
                } else if (diffOp.type == OpType.INSERT) {
                    rightCount++;
                    curRight++;
                }
                k++;
            }

            DiffType type = DiffType.MODIFIED;
            if (leftCount == 0 && rightCount > 0) {
                type = DiffType.INSERTED;
            } else if (leftCount > 0 && rightCount == 0) {
                type = DiffType.DELETED;
            }

            chunks.add(new DiffChunk(
                    leftOffset + chunkLeftStart, leftCount,
                    rightOffset + chunkRightStart, rightCount,
                    type
            ));
        }

        return chunks;
    }

    private enum OpType { EQUAL, INSERT, DELETE }
    private record EditOp(OpType type, int leftIdx, int rightIdx) {}
}
